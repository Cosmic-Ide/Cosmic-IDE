//
// Created by ven on 24/03/2022.
//

#ifndef ALIUHOOK_ALIUHOOK_H
#define ALIUHOOK_ALIUHOOK_H

#include "elf_img.h"

void *InlineHooker(void *, void *);

bool InlineUnhooker(void *);

class AliuHook {
public:
    static pine::ElfImg elf_img;
    static int android_version;

    static void init(int version);
};

#endif //ALIUHOOK_ALIUHOOK_H