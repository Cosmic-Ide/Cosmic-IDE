#define _GNU_SOURCE

#include "dns_fallback.h"

#include <arpa/inet.h>
#include <ctype.h>
#include <dlfcn.h>
#include <errno.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdint.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

/*
 * dns_fallback.c
 *
 * A pragmatic getaddrinfo(3) fallback for app-private glibc runtimes on
 * Android, where glibc's own NSS/resolver path may fail because its compiled
 * sysconfdir and NSS setup do not match the app sandbox.
 *
 * Behavior:
 *   1. Always call real glibc getaddrinfo() first.
 *   2. If real getaddrinfo() succeeds, return its result unchanged.
 *   3. If it fails for a hostname, emulate the important hosts/dns parts of
 *      glibc resolution using app-provided config files:
 *        - HOSTS_PATH, or default /etc/hosts
 *        - NSSWITCH_CONF_PATH, or default /etc/nsswitch.conf
 *        - RESOLV_CONF_PATH, or default /etc/resolv.conf
 *   4. Support nsswitch order for "hosts: files dns" / "hosts: dns files".
 *   5. Support resolv.conf nameserver, search/domain, options ndots/timeout/
 *      attempts/rotate.
 *   6. Support A, AAAA, CNAME chasing, UDP, and TCP retry for truncated replies.
 *
 * Deliberately not a full libc resolver replacement:
 *   - no DNSSEC validation
 *   - no mDNS/LLMNR/systemd-resolved/NIS/LDAP plugins
 *   - no full gai.conf sorting policy
 *   - no /etc/services database fallback for textual service names
 */

#define DNS_MAX_PACKET_UDP 512
#define DNS_MAX_PACKET_TCP 4096
#define DNS_MAX_NAME 256
#define DNS_MAX_SERVERS 4
#define DNS_MAX_SEARCH 6
#define DNS_MAX_ADDRS 32
#define DNS_MAX_CNAME_DEPTH 8
#define DNS_PORT 53
#define DNS_CLASS_IN 1
#define DNS_TYPE_A 1
#define DNS_TYPE_CNAME 5
#define DNS_TYPE_AAAA 28

#ifndef AI_ALL
#define AI_ALL 0x0010
#endif
#ifndef AI_V4MAPPED
#define AI_V4MAPPED 0x0008
#endif

typedef enum {
    NSS_SRC_FILES = 1,
    NSS_SRC_DNS = 2
} nss_source_t;

typedef struct {
    nss_source_t sources[4];
    size_t count;
} nss_hosts_order_t;

typedef struct {
    struct sockaddr_storage addr;
    socklen_t addr_len;
} dns_server_t;

typedef struct {
    dns_server_t servers[DNS_MAX_SERVERS];
    size_t server_count;

    char search[DNS_MAX_SEARCH][DNS_MAX_NAME];
    size_t search_count;

    int ndots;
    int timeout_sec;
    int attempts;
    int rotate;
} resolver_config_t;

typedef struct {
    struct sockaddr_storage addr;
    socklen_t addr_len;
} addr_result_t;

typedef struct {
    addr_result_t items[DNS_MAX_ADDRS];
    size_t count;
    char canonname[DNS_MAX_NAME];
} addr_result_list_t;

typedef enum {
    DNS_QUERY_NOANSWER = 0,
    DNS_QUERY_SUCCESS = 1,
    DNS_QUERY_NXDOMAIN = 2,
    DNS_QUERY_TEMPFAIL = 3
} dns_query_status_t;

typedef struct {
    dns_query_status_t status;
    int truncated;
    char cname[DNS_MAX_NAME];
} dns_parse_result_t;

static pthread_once_t real_getaddrinfo_once = PTHREAD_ONCE_INIT;
static int (*real_getaddrinfo_fn)(const char*, const char*, const struct addrinfo*, struct addrinfo**) = NULL;

static void init_real_getaddrinfo(void) {
    dlerror();
    real_getaddrinfo_fn = (int (*)(const char*, const char*, const struct addrinfo*, struct addrinfo**))
        dlsym(RTLD_NEXT, "getaddrinfo");
}

static int dns_debug_enabled(void) {
    const char* v = getenv("DNS_TRACE");
    return v && *v && strcmp(v, "0") != 0;
}

static int path_debug_enabled(void) {
    const char* v = getenv("PATH_REDIRECT_DEBUG");
    return v && *v && strcmp(v, "0") != 0;
}

static void dns_debug(const char* fmt, ...) {
    if (!dns_debug_enabled() && !path_debug_enabled()) return;

    va_list ap;
    va_start(ap, fmt);
    fprintf(stderr, "path_redirect: ");
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);
}

static uint16_t read_be16(const unsigned char* p) {
    return (uint16_t)(((uint16_t)p[0] << 8) | p[1]);
}

static uint32_t read_be32(const unsigned char* p) {
    return ((uint32_t)p[0] << 24) |
           ((uint32_t)p[1] << 16) |
           ((uint32_t)p[2] << 8) |
           ((uint32_t)p[3]);
}

static void write_be16(unsigned char* p, uint16_t v) {
    p[0] = (unsigned char)(v >> 8);
    p[1] = (unsigned char)(v & 0xff);
}

static void trim_ascii(char* s) {
    if (!s) return;

    char* start = s;
    while (*start && isspace((unsigned char)*start)) start++;
    if (start != s) memmove(s, start, strlen(start) + 1);

    size_t len = strlen(s);
    while (len > 0 && isspace((unsigned char)s[len - 1])) {
        s[--len] = '\0';
    }
}

static void strip_comment(char* s) {
    if (!s) return;
    char* hash = strchr(s, '#');
    if (hash) *hash = '\0';
}

static int str_eq_ci(const char* a, const char* b) {
    if (!a || !b) return 0;
    while (*a && *b) {
        if (tolower((unsigned char)*a) != tolower((unsigned char)*b)) return 0;
        a++;
        b++;
    }
    return *a == '\0' && *b == '\0';
}

static int has_trailing_dot(const char* name) {
    size_t len = name ? strlen(name) : 0;
    return len > 0 && name[len - 1] == '.';
}

static int count_dots(const char* name) {
    int dots = 0;
    if (!name) return 0;
    for (const char* p = name; *p; p++) {
        if (*p == '.') dots++;
    }
    return dots;
}

static void strip_trailing_dot_copy(const char* in, char* out, size_t out_size) {
    if (!out || out_size == 0) return;
    if (!in) {
        out[0] = '\0';
        return;
    }

    snprintf(out, out_size, "%s", in);
    size_t len = strlen(out);
    if (len > 1 && out[len - 1] == '.') out[len - 1] = '\0';
}

static int is_numeric_address_literal(const char* node) {
    if (!node || !*node) return 0;

    char copy[DNS_MAX_NAME];
    strip_trailing_dot_copy(node, copy, sizeof(copy));

    struct in_addr a4;
    struct in6_addr a6;
    return inet_pton(AF_INET, copy, &a4) == 1 || inet_pton(AF_INET6, copy, &a6) == 1;
}

static int parse_numeric_service(const char* service, int* out_port) {
    if (!out_port) return 0;
    *out_port = 0;

    if (!service || !*service) return 1;

    char* end = NULL;
    long value = strtol(service, &end, 10);
    if (end && *end == '\0' && value >= 0 && value <= 65535) {
        *out_port = (int)value;
        return 1;
    }

    /* Keep this fallback strict. Real getaddrinfo already had a chance to
     * resolve service names through /etc/services. */
    return 0;
}

static void addr_list_init(addr_result_list_t* out) {
    if (out) memset(out, 0, sizeof(*out));
}

static int addr_equal(const struct sockaddr_storage* a, const struct sockaddr_storage* b) {
    if (!a || !b || a->ss_family != b->ss_family) return 0;

    if (a->ss_family == AF_INET) {
        const struct sockaddr_in* x = (const struct sockaddr_in*)a;
        const struct sockaddr_in* y = (const struct sockaddr_in*)b;
        return memcmp(&x->sin_addr, &y->sin_addr, sizeof(x->sin_addr)) == 0;
    }

    if (a->ss_family == AF_INET6) {
        const struct sockaddr_in6* x = (const struct sockaddr_in6*)a;
        const struct sockaddr_in6* y = (const struct sockaddr_in6*)b;
        return memcmp(&x->sin6_addr, &y->sin6_addr, sizeof(x->sin6_addr)) == 0;
    }

    return 0;
}

static int addr_list_add_sockaddr(addr_result_list_t* out, const struct sockaddr_storage* addr, socklen_t len) {
    if (!out || !addr || out->count >= DNS_MAX_ADDRS) return 0;

    for (size_t i = 0; i < out->count; i++) {
        if (addr_equal(&out->items[i].addr, addr)) return 1;
    }

    out->items[out->count].addr = *addr;
    out->items[out->count].addr_len = len;
    out->count++;
    return 1;
}

static int addr_list_add_raw(addr_result_list_t* out, int family, const unsigned char* rdata, uint16_t rdlen) {
    if (!out || !rdata) return 0;

    struct sockaddr_storage ss;
    memset(&ss, 0, sizeof(ss));

    if (family == AF_INET && rdlen == 4) {
        struct sockaddr_in* a4 = (struct sockaddr_in*)&ss;
        a4->sin_family = AF_INET;
        memcpy(&a4->sin_addr, rdata, 4);
        return addr_list_add_sockaddr(out, &ss, sizeof(*a4));
    }

    if (family == AF_INET6 && rdlen == 16) {
        struct sockaddr_in6* a6 = (struct sockaddr_in6*)&ss;
        a6->sin6_family = AF_INET6;
        memcpy(&a6->sin6_addr, rdata, 16);
        return addr_list_add_sockaddr(out, &ss, sizeof(*a6));
    }

    return 0;
}

static int addr_list_add_v4mapped(addr_result_list_t* out, const struct sockaddr_storage* v4) {
    if (!out || !v4 || v4->ss_family != AF_INET) return 0;

    const struct sockaddr_in* a4 = (const struct sockaddr_in*)v4;
    struct sockaddr_storage ss;
    memset(&ss, 0, sizeof(ss));

    struct sockaddr_in6* a6 = (struct sockaddr_in6*)&ss;
    a6->sin6_family = AF_INET6;
    a6->sin6_addr.s6_addr[10] = 0xff;
    a6->sin6_addr.s6_addr[11] = 0xff;
    memcpy(&a6->sin6_addr.s6_addr[12], &a4->sin_addr, 4);

    return addr_list_add_sockaddr(out, &ss, sizeof(*a6));
}

static void set_port_on_results(addr_result_list_t* out, int port) {
    if (!out || port < 0) return;
    for (size_t i = 0; i < out->count; i++) {
        if (out->items[i].addr.ss_family == AF_INET) {
            ((struct sockaddr_in*)&out->items[i].addr)->sin_port = htons((uint16_t)port);
        } else if (out->items[i].addr.ss_family == AF_INET6) {
            ((struct sockaddr_in6*)&out->items[i].addr)->sin6_port = htons((uint16_t)port);
        }
    }
}

static int add_dns_server(resolver_config_t* cfg, const char* ip) {
    if (!cfg || !ip || !*ip || cfg->server_count >= DNS_MAX_SERVERS) return 0;

    struct sockaddr_in a4;
    memset(&a4, 0, sizeof(a4));
    a4.sin_family = AF_INET;
    a4.sin_port = htons(DNS_PORT);
    if (inet_pton(AF_INET, ip, &a4.sin_addr) == 1) {
        memcpy(&cfg->servers[cfg->server_count].addr, &a4, sizeof(a4));
        cfg->servers[cfg->server_count].addr_len = sizeof(a4);
        cfg->server_count++;
        return 1;
    }

    struct sockaddr_in6 a6;
    memset(&a6, 0, sizeof(a6));
    a6.sin6_family = AF_INET6;
    a6.sin6_port = htons(DNS_PORT);
    if (inet_pton(AF_INET6, ip, &a6.sin6_addr) == 1) {
        memcpy(&cfg->servers[cfg->server_count].addr, &a6, sizeof(a6));
        cfg->servers[cfg->server_count].addr_len = sizeof(a6);
        cfg->server_count++;
        return 1;
    }

    return 0;
}

static void add_search_domain(resolver_config_t* cfg, const char* domain) {
    if (!cfg || !domain || !*domain || cfg->search_count >= DNS_MAX_SEARCH) return;
    size_t j = 0;
    while (domain[j] && j + 1 < sizeof(cfg->search[cfg->search_count])) {
        cfg->search[cfg->search_count][j] = domain[j];
        j++;
    }
    cfg->search[cfg->search_count][j] = '\0';
    trim_ascii(cfg->search[cfg->search_count]);
    if (cfg->search[cfg->search_count][0]) cfg->search_count++;
}

static void parse_resolv_options(resolver_config_t* cfg, char* p) {
    if (!cfg || !p) return;

    char* save = NULL;
    for (char* tok = strtok_r(p, " \t\r\n", &save); tok; tok = strtok_r(NULL, " \t\r\n", &save)) {
        if (strncmp(tok, "ndots:", 6) == 0) {
            int v = atoi(tok + 6);
            if (v >= 0 && v <= 15) cfg->ndots = v;
        } else if (strncmp(tok, "timeout:", 8) == 0) {
            int v = atoi(tok + 8);
            if (v >= 1 && v <= 30) cfg->timeout_sec = v;
        } else if (strncmp(tok, "attempts:", 9) == 0) {
            int v = atoi(tok + 9);
            if (v >= 1 && v <= 10) cfg->attempts = v;
        } else if (strcmp(tok, "rotate") == 0) {
            cfg->rotate = 1;
        }
    }
}

static void load_resolver_config(resolver_config_t* cfg) {
    memset(cfg, 0, sizeof(*cfg));
    cfg->ndots = 1;
    cfg->timeout_sec = 2;
    cfg->attempts = 2;

    const char* path = getenv("RESOLV_CONF_PATH");
    if (!path || !*path) path = "/etc/resolv.conf";

    FILE* fp = fopen(path, "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            strip_comment(line);
            trim_ascii(line);
            if (!line[0]) continue;

            if (strncmp(line, "nameserver", 10) == 0 && isspace((unsigned char)line[10])) {
                char* p = line + 10;
                trim_ascii(p);
                char ip[128] = {0};
                sscanf(p, "%127s", ip);
                if (add_dns_server(cfg, ip)) dns_debug("resolver nameserver from %s: %s", path, ip);
            } else if (strncmp(line, "search", 6) == 0 && isspace((unsigned char)line[6])) {
                cfg->search_count = 0;
                char* p = line + 6;
                char* save = NULL;
                for (char* tok = strtok_r(p, " \t\r\n", &save); tok; tok = strtok_r(NULL, " \t\r\n", &save)) {
                    add_search_domain(cfg, tok);
                }
            } else if (strncmp(line, "domain", 6) == 0 && isspace((unsigned char)line[6])) {
                if (cfg->search_count == 0) {
                    char* p = line + 6;
                    trim_ascii(p);
                    add_search_domain(cfg, p);
                }
            } else if (strncmp(line, "options", 7) == 0 && isspace((unsigned char)line[7])) {
                parse_resolv_options(cfg, line + 7);
            }
        }
        fclose(fp);
    } else {
        dns_debug("failed to open resolver config %s errno=%d", path, errno);
    }

    if (cfg->server_count == 0) {
        add_dns_server(cfg, "1.1.1.1");
        add_dns_server(cfg, "8.8.8.8");
        dns_debug("resolver fallback nameservers: 1.1.1.1, 8.8.8.8");
    }
}

static void load_nss_hosts_order(nss_hosts_order_t* order) {
    memset(order, 0, sizeof(*order));
    order->sources[order->count++] = NSS_SRC_FILES;
    order->sources[order->count++] = NSS_SRC_DNS;

    const char* path = getenv("NSSWITCH_CONF_PATH");
    if (!path || !*path) path = "/etc/nsswitch.conf";

    FILE* fp = fopen(path, "r");
    if (!fp) return;

    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        strip_comment(line);
        trim_ascii(line);
        if (strncmp(line, "hosts:", 6) != 0) continue;

        order->count = 0;
        char* p = line + 6;
        char* save = NULL;
        for (char* tok = strtok_r(p, " \t\r\n", &save); tok && order->count < 4; tok = strtok_r(NULL, " \t\r\n", &save)) {
            if (tok[0] == '[') continue;
            if (strcmp(tok, "files") == 0) order->sources[order->count++] = NSS_SRC_FILES;
            else if (strcmp(tok, "dns") == 0) order->sources[order->count++] = NSS_SRC_DNS;
        }

        if (order->count == 0) {
            order->sources[order->count++] = NSS_SRC_FILES;
            order->sources[order->count++] = NSS_SRC_DNS;
        }
        break;
    }

    fclose(fp);
}

static int hosts_name_matches(const char* token, const char* node) {
    if (!token || !node) return 0;

    char a[DNS_MAX_NAME];
    char b[DNS_MAX_NAME];
    strip_trailing_dot_copy(token, a, sizeof(a));
    strip_trailing_dot_copy(node, b, sizeof(b));
    return str_eq_ci(a, b);
}

static int lookup_hosts_file(const char* node, int family, addr_result_list_t* out) {
    const char* path = getenv("HOSTS_PATH");
    if (!path || !*path) path = "/etc/hosts";

    FILE* fp = fopen(path, "r");
    if (!fp) return EAI_NONAME;

    char line[1024];
    while (fgets(line, sizeof(line), fp) && out->count < DNS_MAX_ADDRS) {
        strip_comment(line);
        trim_ascii(line);
        if (!line[0]) continue;

        char* save = NULL;
        char* ip = strtok_r(line, " \t\r\n", &save);
        if (!ip) continue;

        struct sockaddr_storage ss;
        socklen_t ss_len = 0;
        memset(&ss, 0, sizeof(ss));

        struct sockaddr_in* a4 = (struct sockaddr_in*)&ss;
        struct sockaddr_in6* a6 = (struct sockaddr_in6*)&ss;

        if ((family == AF_UNSPEC || family == AF_INET) && inet_pton(AF_INET, ip, &a4->sin_addr) == 1) {
            a4->sin_family = AF_INET;
            ss_len = sizeof(*a4);
        } else if ((family == AF_UNSPEC || family == AF_INET6) && inet_pton(AF_INET6, ip, &a6->sin6_addr) == 1) {
            a6->sin6_family = AF_INET6;
            ss_len = sizeof(*a6);
        } else {
            continue;
        }

        for (char* name = strtok_r(NULL, " \t\r\n", &save); name; name = strtok_r(NULL, " \t\r\n", &save)) {
            if (hosts_name_matches(name, node)) {
                addr_list_add_sockaddr(out, &ss, ss_len);
                if (!out->canonname[0]) snprintf(out->canonname, sizeof(out->canonname), "%s", name);
                break;
            }
        }
    }

    fclose(fp);
    return out->count > 0 ? 0 : EAI_NONAME;
}

static int encode_dns_name(const char* name, unsigned char* packet, size_t* offset, size_t capacity) {
    if (!name || !*name || !packet || !offset) return 0;

    char clean[DNS_MAX_NAME];
    strip_trailing_dot_copy(name, clean, sizeof(clean));

    const char* p = clean;
    while (*p) {
        const char* dot = strchr(p, '.');
        size_t label_len = dot ? (size_t)(dot - p) : strlen(p);

        if (label_len == 0) {
            p++;
            continue;
        }
        if (label_len > 63 || *offset + 1 + label_len >= capacity) return 0;

        packet[(*offset)++] = (unsigned char)label_len;
        memcpy(packet + *offset, p, label_len);
        *offset += label_len;

        if (!dot) break;
        p = dot + 1;
    }

    if (*offset >= capacity) return 0;
    packet[(*offset)++] = 0;
    return 1;
}

static ssize_t skip_dns_name(const unsigned char* packet, size_t packet_len, size_t offset) {
    size_t jumps = 0;

    while (offset < packet_len) {
        unsigned char len = packet[offset];
        if (len == 0) return (ssize_t)(offset + 1);

        if ((len & 0xC0) == 0xC0) {
            if (offset + 1 >= packet_len) return -1;
            return (ssize_t)(offset + 2);
        }

        if ((len & 0xC0) != 0) return -1;
        offset += 1 + len;
        if (++jumps > 128) return -1;
    }

    return -1;
}

static int decode_dns_name_at(
    const unsigned char* packet,
    size_t packet_len,
    size_t offset,
    char* out,
    size_t out_size,
    size_t* next_offset
) {
    if (!packet || !out || out_size == 0) return 0;

    size_t original = offset;
    size_t out_pos = 0;
    size_t jumps = 0;
    int jumped = 0;

    out[0] = '\0';

    while (offset < packet_len) {
        unsigned char len = packet[offset];

        if (len == 0) {
            if (!jumped && next_offset) *next_offset = offset + 1;
            if (out_pos == 0) snprintf(out, out_size, ".");
            return 1;
        }

        if ((len & 0xC0) == 0xC0) {
            if (offset + 1 >= packet_len) return 0;
            uint16_t ptr = (uint16_t)(((len & 0x3f) << 8) | packet[offset + 1]);
            if (ptr >= packet_len) return 0;
            if (!jumped && next_offset) *next_offset = offset + 2;
            offset = ptr;
            jumped = 1;
            if (++jumps > 128) return 0;
            continue;
        }

        if ((len & 0xC0) != 0 || len > 63) return 0;
        offset++;
        if (offset + len > packet_len) return 0;

        if (out_pos != 0) {
            if (out_pos + 1 >= out_size) return 0;
            out[out_pos++] = '.';
        }
        if (out_pos + len >= out_size) return 0;
        memcpy(out + out_pos, packet + offset, len);
        out_pos += len;
        out[out_pos] = '\0';
        offset += len;
    }

    (void)original;
    return 0;
}

static int build_dns_query(const char* name, int qtype, uint16_t id, unsigned char* packet, size_t* packet_len, size_t capacity) {
    if (!packet || !packet_len || capacity < 12) return 0;
    memset(packet, 0, capacity);

    write_be16(packet + 0, id);
    write_be16(packet + 2, 0x0100); /* recursion desired */
    write_be16(packet + 4, 1);      /* qdcount */

    size_t offset = 12;
    if (!encode_dns_name(name, packet, &offset, capacity)) return 0;
    if (offset + 4 > capacity) return 0;

    write_be16(packet + offset, (uint16_t)qtype);
    offset += 2;
    write_be16(packet + offset, DNS_CLASS_IN);
    offset += 2;

    *packet_len = offset;
    return 1;
}

static dns_parse_result_t parse_dns_response(
    const unsigned char* packet,
    size_t packet_len,
    uint16_t expected_id,
    int qtype,
    addr_result_list_t* out
) {
    dns_parse_result_t result;
    memset(&result, 0, sizeof(result));
    result.status = DNS_QUERY_NOANSWER;

    if (!packet || packet_len < 12) {
        result.status = DNS_QUERY_TEMPFAIL;
        return result;
    }

    if (read_be16(packet + 0) != expected_id) return result;

    uint16_t flags = read_be16(packet + 2);
    uint16_t qdcount = read_be16(packet + 4);
    uint16_t ancount = read_be16(packet + 6);
    int rcode = flags & 0x000f;

    if ((flags & 0x8000) == 0) return result;
    if (flags & 0x0200) result.truncated = 1;

    if (rcode == 3) {
        result.status = DNS_QUERY_NXDOMAIN;
        return result;
    }
    if (rcode != 0) {
        result.status = DNS_QUERY_TEMPFAIL;
        return result;
    }

    size_t offset = 12;
    for (uint16_t i = 0; i < qdcount; i++) {
        ssize_t next = skip_dns_name(packet, packet_len, offset);
        if (next < 0) {
            result.status = DNS_QUERY_TEMPFAIL;
            return result;
        }
        offset = (size_t)next;
        if (offset + 4 > packet_len) {
            result.status = DNS_QUERY_TEMPFAIL;
            return result;
        }
        offset += 4;
    }

    size_t before = out ? out->count : 0;

    for (uint16_t i = 0; i < ancount && offset < packet_len; i++) {
        size_t rr_next = 0;
        char owner[DNS_MAX_NAME];
        if (!decode_dns_name_at(packet, packet_len, offset, owner, sizeof(owner), &rr_next)) {
            result.status = DNS_QUERY_TEMPFAIL;
            return result;
        }
        offset = rr_next;

        if (offset + 10 > packet_len) {
            result.status = DNS_QUERY_TEMPFAIL;
            return result;
        }

        uint16_t type = read_be16(packet + offset);
        offset += 2;
        uint16_t klass = read_be16(packet + offset);
        offset += 2;
        (void)read_be32(packet + offset); /* TTL */
        offset += 4;
        uint16_t rdlen = read_be16(packet + offset);
        offset += 2;

        if (offset + rdlen > packet_len) {
            result.status = DNS_QUERY_TEMPFAIL;
            return result;
        }

        if (klass == DNS_CLASS_IN) {
            if (type == qtype) {
                if (qtype == DNS_TYPE_A) {
                    addr_list_add_raw(out, AF_INET, packet + offset, rdlen);
                } else if (qtype == DNS_TYPE_AAAA) {
                    addr_list_add_raw(out, AF_INET6, packet + offset, rdlen);
                }
            } else if (type == DNS_TYPE_CNAME && !result.cname[0]) {
                char cname[DNS_MAX_NAME];
                if (decode_dns_name_at(packet, packet_len, offset, cname, sizeof(cname), NULL)) {
                    snprintf(result.cname, sizeof(result.cname), "%s", cname);
                    if (out && !out->canonname[0]) snprintf(out->canonname, sizeof(out->canonname), "%s", cname);
                }
            }
        }

        offset += rdlen;
    }

    if (out && out->count > before) result.status = DNS_QUERY_SUCCESS;
    else result.status = result.cname[0] ? DNS_QUERY_NOANSWER : DNS_QUERY_NOANSWER;

    return result;
}

static int wait_readable(int fd, int timeout_sec) {
    fd_set readfds;
    FD_ZERO(&readfds);
    FD_SET(fd, &readfds);

    struct timeval tv;
    tv.tv_sec = timeout_sec;
    tv.tv_usec = 0;

    return select(fd + 1, &readfds, NULL, NULL, &tv);
}

static dns_parse_result_t dns_query_udp(
    const dns_server_t* server,
    const char* name,
    int qtype,
    int timeout_sec,
    uint16_t id,
    addr_result_list_t* out
) {
    dns_parse_result_t parsed;
    memset(&parsed, 0, sizeof(parsed));
    parsed.status = DNS_QUERY_TEMPFAIL;

    int family = server->addr.ss_family;
    int fd = socket(family, SOCK_DGRAM, 0);
    if (fd < 0) return parsed;

    unsigned char query[DNS_MAX_PACKET_UDP];
    unsigned char response[DNS_MAX_PACKET_UDP];
    size_t query_len = 0;

    if (!build_dns_query(name, qtype, id, query, &query_len, sizeof(query))) {
        close(fd);
        return parsed;
    }

    if (dns_debug_enabled() || path_debug_enabled()) {
        char ip[INET6_ADDRSTRLEN] = {0};
        if (server->addr.ss_family == AF_INET) {
            inet_ntop(AF_INET, &((const struct sockaddr_in*)&server->addr)->sin_addr, ip, sizeof(ip));
        } else if (server->addr.ss_family == AF_INET6) {
            inet_ntop(AF_INET6, &((const struct sockaddr_in6*)&server->addr)->sin6_addr, ip, sizeof(ip));
        }
        dns_debug("manual DNS UDP query %s type %d -> %s:53", name, qtype, ip[0] ? ip : "?");
    }

    ssize_t sent = sendto(fd, query, query_len, 0, (const struct sockaddr*)&server->addr, server->addr_len);
    if (sent < 0 || (size_t)sent != query_len) {
        dns_debug("manual DNS UDP sendto failed errno=%d", errno);
        close(fd);
        return parsed;
    }

    int ready = wait_readable(fd, timeout_sec);
    if (ready <= 0) {
        dns_debug("manual DNS UDP timeout/error ready=%d errno=%d", ready, errno);
        close(fd);
        return parsed;
    }

    ssize_t n = recvfrom(fd, response, sizeof(response), 0, NULL, NULL);
    close(fd);

    if (n <= 0) return parsed;
    return parse_dns_response(response, (size_t)n, id, qtype, out);
}

static dns_parse_result_t dns_query_tcp(
    const dns_server_t* server,
    const char* name,
    int qtype,
    int timeout_sec,
    uint16_t id,
    addr_result_list_t* out
) {
    dns_parse_result_t parsed;
    memset(&parsed, 0, sizeof(parsed));
    parsed.status = DNS_QUERY_TEMPFAIL;

    int family = server->addr.ss_family;
    int fd = socket(family, SOCK_STREAM, 0);
    if (fd < 0) return parsed;

    struct timeval tv;
    tv.tv_sec = timeout_sec;
    tv.tv_usec = 0;
    setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));

    if (connect(fd, (const struct sockaddr*)&server->addr, server->addr_len) != 0) {
        dns_debug("manual DNS TCP connect failed errno=%d", errno);
        close(fd);
        return parsed;
    }

    unsigned char query[DNS_MAX_PACKET_UDP];
    unsigned char framed[DNS_MAX_PACKET_UDP + 2];
    unsigned char response[DNS_MAX_PACKET_TCP];
    size_t query_len = 0;

    if (!build_dns_query(name, qtype, id, query, &query_len, sizeof(query))) {
        close(fd);
        return parsed;
    }

    write_be16(framed, (uint16_t)query_len);
    memcpy(framed + 2, query, query_len);

    if (write(fd, framed, query_len + 2) != (ssize_t)(query_len + 2)) {
        close(fd);
        return parsed;
    }

    unsigned char lenbuf[2];
    ssize_t got = read(fd, lenbuf, 2);
    if (got != 2) {
        close(fd);
        return parsed;
    }

    uint16_t resp_len = read_be16(lenbuf);
    if (resp_len == 0 || resp_len > sizeof(response)) {
        close(fd);
        return parsed;
    }

    size_t off = 0;
    while (off < resp_len) {
        ssize_t n = read(fd, response + off, resp_len - off);
        if (n <= 0) {
            close(fd);
            return parsed;
        }
        off += (size_t)n;
    }

    close(fd);
    dns_debug("manual DNS TCP retry %s type %d", name, qtype);
    return parse_dns_response(response, resp_len, id, qtype, out);
}

static dns_query_status_t query_server_once(
    const dns_server_t* server,
    const char* name,
    int qtype,
    int timeout_sec,
    addr_result_list_t* out,
    char* cname,
    size_t cname_size
) {
    uint16_t id = (uint16_t)(((uintptr_t)out) ^ (uintptr_t)time(NULL) ^ (uintptr_t)getpid() ^ (uintptr_t)qtype ^ (uintptr_t)name);

    dns_parse_result_t parsed = dns_query_udp(server, name, qtype, timeout_sec, id, out);
    if (parsed.truncated) {
        parsed = dns_query_tcp(server, name, qtype, timeout_sec, id, out);
    }

    if (parsed.cname[0] && cname && cname_size) snprintf(cname, cname_size, "%s", parsed.cname);
    return parsed.status;
}

static dns_query_status_t resolve_dns_name_once(
    const resolver_config_t* cfg,
    const char* name,
    int qtype,
    addr_result_list_t* out,
    char* cname,
    size_t cname_size
) {
    dns_query_status_t last = DNS_QUERY_NOANSWER;
    if (!cfg || cfg->server_count == 0) return DNS_QUERY_TEMPFAIL;

    size_t start = 0;
    if (cfg->rotate && cfg->server_count > 1) {
        start = (size_t)(time(NULL) % (time_t)cfg->server_count);
    }

    for (int attempt = 0; attempt < cfg->attempts; attempt++) {
        for (size_t s = 0; s < cfg->server_count; s++) {
            size_t idx = (start + s) % cfg->server_count;
            dns_query_status_t st = query_server_once(&cfg->servers[idx], name, qtype, cfg->timeout_sec, out, cname, cname_size);
            if (st == DNS_QUERY_SUCCESS) return st;
            if (st == DNS_QUERY_NXDOMAIN) return st;
            if (st == DNS_QUERY_TEMPFAIL) last = st;
        }
    }

    return last;
}

static int make_query_candidates(const resolver_config_t* cfg, const char* node, char out[][DNS_MAX_NAME], size_t max_out) {
    if (!cfg || !node || !out || max_out == 0) return 0;

    char base[DNS_MAX_NAME];
    strip_trailing_dot_copy(node, base, sizeof(base));
    int absolute = has_trailing_dot(node);
    int dots = count_dots(base);
    size_t count = 0;

    if (absolute) {
        snprintf(out[count++], DNS_MAX_NAME, "%s", base);
        return (int)count;
    }

    int try_absolute_first = dots >= cfg->ndots;

    if (try_absolute_first && count < max_out) {
        snprintf(out[count++], DNS_MAX_NAME, "%s", base);
    }

    if (cfg->search_count > 0) {
        for (size_t i = 0; i < cfg->search_count && count < max_out; i++) {
            snprintf(out[count], DNS_MAX_NAME, "%s.%s", base, cfg->search[i]);
            count++;
        }
    }

    if (!try_absolute_first && count < max_out) {
        snprintf(out[count++], DNS_MAX_NAME, "%s", base);
    }

    return (int)count;
}

static int dns_lookup_qtype_with_cname(
    const resolver_config_t* cfg,
    const char* qname,
    int qtype,
    addr_result_list_t* out
) {
    char current[DNS_MAX_NAME];
    snprintf(current, sizeof(current), "%s", qname);

    for (int depth = 0; depth < DNS_MAX_CNAME_DEPTH; depth++) {
        char cname[DNS_MAX_NAME] = {0};
        size_t before = out->count;
        dns_query_status_t st = resolve_dns_name_once(cfg, current, qtype, out, cname, sizeof(cname));

        if (out->count > before || st == DNS_QUERY_SUCCESS) {
            if (cname[0] && !out->canonname[0]) snprintf(out->canonname, sizeof(out->canonname), "%s", cname);
            return 0;
        }

        if (st == DNS_QUERY_NXDOMAIN) return EAI_NONAME;
        if (cname[0]) {
            dns_debug("manual DNS CNAME %s -> %s", current, cname);
            snprintf(current, sizeof(current), "%s", cname);
            continue;
        }

        if (st == DNS_QUERY_TEMPFAIL) return EAI_AGAIN;
        return EAI_NONAME;
    }

    return EAI_AGAIN;
}

static int lookup_dns(const char* node, int family, const struct addrinfo* hints, addr_result_list_t* out) {
    resolver_config_t cfg;
    load_resolver_config(&cfg);

    char candidates[DNS_MAX_SEARCH + 2][DNS_MAX_NAME];
    int candidate_count = make_query_candidates(&cfg, node, candidates, DNS_MAX_SEARCH + 2);
    if (candidate_count <= 0) return EAI_NONAME;

    int want_v4 = family == AF_UNSPEC || family == AF_INET;
    int want_v6 = family == AF_UNSPEC || family == AF_INET6;
    int v4mapped = hints && (hints->ai_flags & AI_V4MAPPED);
    int all = hints && (hints->ai_flags & AI_ALL);

    int last_rc = EAI_NONAME;

    for (int i = 0; i < candidate_count && out->count == 0; i++) {
        const char* qname = candidates[i];
        dns_debug("manual DNS trying candidate %s", qname);

        if (want_v4) {
            int rc = dns_lookup_qtype_with_cname(&cfg, qname, DNS_TYPE_A, out);
            if (rc != EAI_NONAME) last_rc = rc;
        }

        if (want_v6) {
            size_t before_v6 = out->count;
            int rc = dns_lookup_qtype_with_cname(&cfg, qname, DNS_TYPE_AAAA, out);
            if (rc != EAI_NONAME) last_rc = rc;

            if (family == AF_INET6 && v4mapped && (all || out->count == before_v6)) {
                addr_result_list_t v4tmp;
                addr_list_init(&v4tmp);
                if (dns_lookup_qtype_with_cname(&cfg, qname, DNS_TYPE_A, &v4tmp) == 0) {
                    for (size_t j = 0; j < v4tmp.count; j++) {
                        addr_list_add_v4mapped(out, &v4tmp.items[j].addr);
                    }
                }
            }
        }
    }

    return out->count > 0 ? 0 : last_rc;
}

static int resolve_by_nss_order(
    const char* node,
    int family,
    const struct addrinfo* hints,
    addr_result_list_t* out
) {
    nss_hosts_order_t order;
    load_nss_hosts_order(&order);

    int last_rc = EAI_NONAME;

    for (size_t i = 0; i < order.count && out->count == 0; i++) {
        if (order.sources[i] == NSS_SRC_FILES) {
            int rc = lookup_hosts_file(node, family, out);
            if (rc == 0) {
                dns_debug("hosts file resolved %s with %zu address(es)", node, out->count);
                return 0;
            }
            last_rc = rc;
        } else if (order.sources[i] == NSS_SRC_DNS) {
            int rc = lookup_dns(node, family, hints, out);
            if (rc == 0) return 0;
            last_rc = rc;
        }
    }

    return out->count > 0 ? 0 : last_rc;
}

static int synthesize_addrinfo(
    const char* node,
    const char* service,
    const struct addrinfo* hints,
    addr_result_list_t* results,
    struct addrinfo** res
) {
    if (!results || results->count == 0 || !res) return EAI_NONAME;

    int port = 0;
    if (!parse_numeric_service(service, &port)) return EAI_SERVICE;
    set_port_on_results(results, port);

    int base_socktype = hints ? hints->ai_socktype : 0;
    int base_protocol = hints ? hints->ai_protocol : 0;
    int want_canon = hints && (hints->ai_flags & AI_CANONNAME);

    int socktypes[2];
    int protocols[2];
    size_t combo_count = 0;

    if (base_socktype != 0 || base_protocol != 0) {
        socktypes[combo_count] = base_socktype;
        protocols[combo_count] = base_protocol;
        combo_count++;
    } else {
        socktypes[combo_count] = SOCK_STREAM;
        protocols[combo_count] = IPPROTO_TCP;
        combo_count++;
        socktypes[combo_count] = SOCK_DGRAM;
        protocols[combo_count] = IPPROTO_UDP;
        combo_count++;
    }

    struct addrinfo* head = NULL;
    struct addrinfo* tail = NULL;

    for (size_t c = 0; c < combo_count; c++) {
        for (size_t i = 0; i < results->count; i++) {
            struct addrinfo* ai = (struct addrinfo*)calloc(1, sizeof(*ai));
            if (!ai) goto oom;

            ai->ai_family = results->items[i].addr.ss_family;
            ai->ai_socktype = socktypes[c];
            ai->ai_protocol = protocols[c];
            ai->ai_addrlen = results->items[i].addr_len;
            ai->ai_addr = (struct sockaddr*)malloc(ai->ai_addrlen);
            if (!ai->ai_addr) {
                free(ai);
                goto oom;
            }
            memcpy(ai->ai_addr, &results->items[i].addr, ai->ai_addrlen);

            if (want_canon && !head) {
                const char* canon = results->canonname[0] ? results->canonname : node;
                ai->ai_canonname = strdup(canon);
                if (!ai->ai_canonname) {
                    free(ai->ai_addr);
                    free(ai);
                    goto oom;
                }
            }

            if (!head) head = ai;
            else tail->ai_next = ai;
            tail = ai;
        }
    }

    *res = head;
    return 0;

oom:
    while (head) {
        struct addrinfo* next = head->ai_next;
        free(head->ai_canonname);
        free(head->ai_addr);
        free(head);
        head = next;
    }
    return EAI_MEMORY;
}

int getaddrinfo(const char* node, const char* service, const struct addrinfo* hints, struct addrinfo** res) {
    pthread_once(&real_getaddrinfo_once, init_real_getaddrinfo);

    if (!real_getaddrinfo_fn) return EAI_SYSTEM;
    if (res) *res = NULL;

    int rc = real_getaddrinfo_fn(node, service, hints, res);
    if (rc == 0) return 0;

    if (!node || !*node || is_numeric_address_literal(node)) return rc;
    if (hints && (hints->ai_flags & AI_NUMERICHOST)) return rc;

    int family = hints ? hints->ai_family : AF_UNSPEC;
    if (family != AF_UNSPEC && family != AF_INET && family != AF_INET6) return rc;

    if (!parse_numeric_service(service, &(int){0})) return rc;

    dns_debug("real getaddrinfo failed for %s rc=%d; trying app DNS fallback", node, rc);

    addr_result_list_t results;
    addr_list_init(&results);

    int fallback_rc = resolve_by_nss_order(node, family, hints, &results);
    if (fallback_rc != 0 || results.count == 0) {
        dns_debug("app DNS fallback failed for %s rc=%d", node, fallback_rc);
        return rc;
    }

    struct addrinfo* synthesized = NULL;
    int synth_rc = synthesize_addrinfo(node, service, hints, &results, &synthesized);
    if (synth_rc != 0) return rc;

    *res = synthesized;
    dns_debug("app DNS fallback succeeded for %s with %zu address(es)", node, results.count);
    return 0;
}
