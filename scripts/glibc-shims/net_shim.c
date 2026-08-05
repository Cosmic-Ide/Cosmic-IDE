/*
 * net_shim.c - Intercepts getifaddrs to supply a mock loopback interface
 *              when restricted by Android security sandboxing (EACCES/EPERM).
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <ifaddrs.h>
#include <net/if.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

typedef int (*getifaddrs_func_t)(struct ifaddrs **ifap);
typedef void (*freeifaddrs_func_t)(struct ifaddrs *ifa);

int getifaddrs(struct ifaddrs **ifap) {
    if (ifap == NULL) {
        errno = EINVAL;
        return -1;
    }

    getifaddrs_func_t real_getifaddrs = (getifaddrs_func_t)dlsym(RTLD_NEXT, "getifaddrs");
    if (!real_getifaddrs) {
        errno = ENOSYS;
        return -1;
    }

    // Attempt to execute the system's real getifaddrs call
    int result = real_getifaddrs(ifap);
    if (result == 0) {
        return 0;
    }

    // If Android's sandbox blocks interface enumeration, safely fallback to a mock loopback interface
    if (errno == EACCES || errno == EPERM) {
        struct ifaddrs *ifa = (struct ifaddrs *)malloc(sizeof(struct ifaddrs));
        if (!ifa) {
            errno = ENOMEM;
            return -1;
        }
        memset(ifa, 0, sizeof(struct ifaddrs));

        ifa->ifa_name = strdup("lo");
        if (!ifa->ifa_name) {
            free(ifa);
            errno = ENOMEM;
            return -1;
        }
        ifa->ifa_flags = IFF_UP | IFF_LOOPBACK | IFF_RUNNING;

        struct sockaddr_in *sa = (struct sockaddr_in *)malloc(sizeof(struct sockaddr_in));
        if (!sa) {
            free(ifa->ifa_name);
            free(ifa);
            errno = ENOMEM;
            return -1;
        }
        memset(sa, 0, sizeof(struct sockaddr_in));
        sa->sin_family = AF_INET;
        sa->sin_addr.s_addr = htonl(INADDR_LOOPBACK); // 127.0.0.1
        ifa->ifa_addr = (struct sockaddr *)sa;

        struct sockaddr_in *netmask = (struct sockaddr_in *)malloc(sizeof(struct sockaddr_in));
        if (!netmask) {
            free(sa);
            free(ifa->ifa_name);
            free(ifa);
            errno = ENOMEM;
            return -1;
        }
        memset(netmask, 0, sizeof(struct sockaddr_in));
        netmask->sin_family = AF_INET;
        netmask->sin_addr.s_addr = htonl(0xFF000000); // 255.0.0.0
        ifa->ifa_netmask = (struct sockaddr *)netmask;

        *ifap = ifa;
        errno = 0;
        return 0;
    }

    return result;
}

void freeifaddrs(struct ifaddrs *ifa) {
    if (!ifa) return;

    // Free resources allocated for our mock loopback interface safely
    if (ifa->ifa_name && strcmp(ifa->ifa_name, "lo") == 0 && ifa->ifa_addr) {
        struct sockaddr_in *sa = (struct sockaddr_in *)ifa->ifa_addr;
        if (sa->sin_family == AF_INET && sa->sin_addr.s_addr == htonl(INADDR_LOOPBACK)) {
            free(ifa->ifa_name);
            free(ifa->ifa_addr);
            if (ifa->ifa_netmask) {
                free(ifa->ifa_netmask);
            }
            free(ifa);
            return;
        }
    }

    // Otherwise, delegate to the real system freeifaddrs implementation
    freeifaddrs_func_t real_freeifaddrs = (freeifaddrs_func_t)dlsym(RTLD_NEXT, "freeifaddrs");
    if (real_freeifaddrs) {
        real_freeifaddrs(ifa);
    }
}