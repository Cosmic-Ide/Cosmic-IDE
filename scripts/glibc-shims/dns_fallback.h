#ifndef COSMICIDE_DNS_FALLBACK_H
#define COSMICIDE_DNS_FALLBACK_H

#include <netdb.h>

/*
 * Exported by dns_fallback.c as an LD_PRELOAD interposer.
 * Keep this in a separate translation unit so libpath_redirect.c remains
 * focused on filesystem/path redirection and HotSpot hsperfdata stat spoofing.
 */
int getaddrinfo(
    const char* node,
    const char* service,
    const struct addrinfo* hints,
    struct addrinfo** res
);

#endif /* COSMICIDE_DNS_FALLBACK_H */
