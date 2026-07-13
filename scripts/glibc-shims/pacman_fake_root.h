#ifndef PACMAN_FAKE_ROOT_H
#define PACMAN_FAKE_ROOT_H

#include <stdbool.h>
#include <sys/types.h>

bool pacman_fake_root_is_pacman(void);

uid_t getuid(void);
uid_t geteuid(void);
gid_t getgid(void);
gid_t getegid(void);

int getresuid(uid_t *ruid, uid_t *euid, uid_t *suid);
int getresgid(gid_t *rgid, gid_t *egid, gid_t *sgid);

#endif