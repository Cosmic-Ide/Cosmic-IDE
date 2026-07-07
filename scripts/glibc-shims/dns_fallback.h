#ifndef COSMICIDE_DNS_FALLBACK_H
#define COSMICIDE_DNS_FALLBACK_H

#include <netdb.h>

/*
 * Exported by dns_fallback.c as an LD_PRELOAD interposer.
 */
int getaddrinfo(
    const char* node,
    const char* service,
    const struct addrinfo* hints,
    struct addrinfo** res
);

#endif
