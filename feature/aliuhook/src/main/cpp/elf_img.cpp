//
// From https://github.com/ganyao114/SandHook/blob/master/hooklib/src/main/cpp/includes/elf_util.h
// Original work Copyright (c) Swift Gan (github user ganyao114)
// Modified work Copyright (c) canyie (github user canyie)
// Modified work Copyright (c) Aliucord (github.com/Aliucord) - Remove pine includes, const char* -> std::string_view
// License: Anti 996 License Version 1.0
// Created by Swift Gan on 2019/3/14.
//

#include "elf_img.h"

#include <malloc.h>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>
#include <string_view>
#include <string>
#include "log.h"
#include <fcntl.h>
#include "xz.h"
// Pine changed: namespace
using namespace pine;

inline bool CanRead(const char *file) {
    return access(file, R_OK) == 0;
}

void ElfImg::Init(const char *elf, jint android_version) {
    this->elf = elf;
    this->android_version = android_version;

    if (elf[0] == '/') {
        Open(elf, true);
    } else {
        // Relative path
        RelativeOpen(elf, true);
    }
}

void ElfImg::Open(const char *path, bool warn_if_symtab_not_found) {
    //load elf
    int fd = open(path, O_RDONLY | O_CLOEXEC); // Pine changed: add O_CLOEXEC to flags
    if (fd == -1) {
        LOGE("failed to open %s", path);
        return;
    }

    size = lseek(fd, 0, SEEK_END);
    if (size <= 0) {
        LOGE("lseek() failed for %s: errno %d (%s)", path, errno, strerror(errno));
    }

    header = reinterpret_cast<Elf_Ehdr *>(mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0));

    close(fd);
    parse(header, path, warn_if_symtab_not_found);
    if (debugdata_offset != 0 && debugdata_size != 0) {
        if (xzdecompress()) {
            header_debugdata = reinterpret_cast<Elf_Ehdr *>(elf_debugdata.data());
            parse(header_debugdata, path, warn_if_symtab_not_found);
        }
    }
    //load module base
    base = GetModuleBase(path);
}

void ElfImg::parse(Elf_Ehdr *hdr, const char *path, bool warn_if_symtab_not_found) {
    // Pine changed: Use uintptr_t instead of size_t

    // section_header = reinterpret_cast<Elf_Shdr *>(((size_t) header) + header->e_shoff);
    section_header = reinterpret_cast<Elf_Shdr *>(((uintptr_t) hdr) + hdr->e_shoff);

    // size_t shoff = reinterpret_cast<size_t>(section_header);
    auto shoff = reinterpret_cast<uintptr_t>(section_header);
    char *section_str = reinterpret_cast<char *>(section_header[hdr->e_shstrndx].sh_offset +
                                                 // ((size_t) header)
                                                 ((uintptr_t) hdr));

    for (int i = 0; i < hdr->e_shnum; i++, shoff += hdr->e_shentsize) {
        auto *section_h = (Elf_Shdr *) shoff;
        char *sname = section_h->sh_name + section_str;
        Elf_Off entsize = section_h->sh_entsize;
        switch (section_h->sh_type) {
            case SHT_DYNSYM:
                if (bias == -4396) {
                    dynsym = section_h;
                    dynsym_offset = section_h->sh_offset;
                    dynsym_size = section_h->sh_size;
                    dynsym_count = dynsym_size / entsize;
                    // dynsym_start = reinterpret_cast<Elf_Sym *>(((size_t) header) + dynsym_offset);
                    dynsym_start = reinterpret_cast<Elf_Sym *>(((uintptr_t) hdr) +
                                                               dynsym_offset);
                    LOGD("dynsym header {:#x} size {}", section_h->sh_offset, section_h->sh_size);
                }
                break;
            case SHT_SYMTAB:
                if (strcmp(sname, ".symtab") == 0) {
                    symtab = section_h;
                    symtab_offset = section_h->sh_offset;
                    symtab_size = section_h->sh_size;
                    symtab_count = symtab_size / entsize;
                    // symtab_start = reinterpret_cast<Elf_Sym *>(((size_t) header) + symtab_offset);
                    symtab_start = reinterpret_cast<Elf_Sym *>(((uintptr_t) hdr) +
                                                               symtab_offset);
                    LOGD("symtab header {:#x} size {} found in {}", section_h->sh_offset,
                         section_h->sh_size, debugdata_offset != 0 ? "gnu_debugdata" : "orgin elf");
                }
                break;
            case SHT_STRTAB:
                if (bias == -4396) {
                    strtab = section_h;
                    symstr_offset = section_h->sh_offset;
                    // strtab_start = reinterpret_cast<Elf_Sym *>(((size_t) header) + symstr_offset);
                    strtab_start = reinterpret_cast<Elf_Sym *>(((uintptr_t) hdr) +
                                                               symstr_offset);
                    LOGD("strtab header {:#x} size {}", section_h->sh_offset, section_h->sh_size);
                }
                if (strcmp(sname, ".strtab") == 0) {
                    symstr_offset_for_symtab = section_h->sh_offset;
                }
                break;
            case SHT_PROGBITS:
                if (strcmp(sname, ".gnu_debugdata") == 0) {
                    debugdata_offset = section_h->sh_offset;
                    debugdata_size = section_h->sh_size;
                    LOGD("gnu_debugdata header {:#x} size {}", section_h->sh_offset,
                         section_h->sh_size);
                }
                if (strtab == nullptr || dynsym == nullptr) break;
                if (bias == -4396) {
                    bias = (off_t) section_h->sh_addr - (off_t) section_h->sh_offset;
                }
                break;
        }
    }

    if (!symtab_offset && warn_if_symtab_not_found) {
        // Pine changed: print log with filename
        // LOGW("can't find symtab from sections\n");
        LOGW("can't find symtab from sections in %s\n", path);
    } else {
        LOGW("found symtab %s\n", path);
    }
}

bool ElfImg::xzdecompress() {
    struct xz_buf str_xz_buf;
    struct xz_dec *str_xz_dec;
    enum xz_ret ret = XZ_OK;
    bool bError = true;

#define BUFSIZE 1024*1024

    xz_crc32_init();
#ifdef XZ_USE_CRC64
    xz_crc64_init();
#endif
    str_xz_dec = xz_dec_init(XZ_DYNALLOC, 1 << 26);
    if (str_xz_dec == NULL) {
        LOGE("xz_dec_init memory allocation failed");
        return false;
    }

    uint8_t *sBuffOut = (uint8_t *) malloc(BUFSIZE);
    if (sBuffOut == NULL) {
        LOGE("allocation for debugdata_header failed");
        return false;
    }

    int iSzOut = BUFSIZE;

    str_xz_buf.in = ((uint8_t *) header) + debugdata_offset;
    str_xz_buf.in_pos = 0;
    str_xz_buf.in_size = debugdata_size;
    str_xz_buf.out = sBuffOut;
    str_xz_buf.out_pos = 0;
    str_xz_buf.out_size = BUFSIZE;

    uint8_t iSkip = 0;

    while (true) {
        ret = xz_dec_run(str_xz_dec, &str_xz_buf);

        if (str_xz_buf.out_pos == BUFSIZE) {
            str_xz_buf.out_pos = 0;
            iSkip++;
        } else {
            iSzOut -= (BUFSIZE - str_xz_buf.out_pos);
        }

        if (ret == XZ_OK) {
            iSzOut += BUFSIZE;
            sBuffOut = (uint8_t *) realloc(sBuffOut, iSzOut);
            str_xz_buf.out = sBuffOut + (iSkip * BUFSIZE);
            continue;
        }

#ifdef XZ_DEC_ANY_CHECK
        if (ret == XZ_UNSUPPORTED_CHECK) {
            LOGW("Unsupported check; not verifying file integrity");
            continue;
        }
#endif
        break;
    } // end while true

    switch (ret) {
        case XZ_STREAM_END:
            bError = false;
            break;

        case XZ_MEM_ERROR:
            LOGE("Memory allocation failed");
            break;

        case XZ_MEMLIMIT_ERROR:
            LOGE("Memory usage limit reached");
            break;

        case XZ_FORMAT_ERROR:
            LOGE("Not a .xz file");
            break;

        case XZ_OPTIONS_ERROR:
            LOGE("Unsupported options in the .xz headers");
            break;

        case XZ_DATA_ERROR:
        case XZ_BUF_ERROR:
            LOGE("File is corrupt");
            break;

        default:
            LOGE("xz_dec_run return a wrong value!");
            break;
    }
    xz_dec_end(str_xz_dec);
    if (bError) {
        return false;
    }
    if (sBuffOut[0] != 0x7F && sBuffOut[1] != 0x45 && sBuffOut[2] != 0x4C && sBuffOut[3] != 0x46) {
        LOGE("not ELF header in gnu_debugdata");
        return false;
    }
    elf_debugdata = std::string((char *) sBuffOut, iSzOut);
    free(sBuffOut);
    return true;
}

void ElfImg::RelativeOpen(const char *elf, bool warn_if_symtab_not_found) {
    char buffer[64] = {0}; // We assume that the path length doesn't exceed 64 bytes.
    if (android_version >= 29) {
        // Android R: com.android.art
        strcpy(buffer, kApexArtLibDir);
        strcat(buffer, elf);
        if (CanRead(buffer)) {
            Open(buffer, warn_if_symtab_not_found);
            return;
        }

        memset(buffer, 0, sizeof(buffer));

        // Android Q: com.android.runtime
        strcpy(buffer, kApexRuntimeLibDir);
        strcat(buffer, elf);
        if (CanRead(buffer)) {
            Open(buffer, warn_if_symtab_not_found);
            return;
        }

        memset(buffer, 0, sizeof(buffer));
    }
    strcpy(buffer, kSystemLibDir);
    strcat(buffer, elf);
    Open(buffer, warn_if_symtab_not_found);
}


ElfImg::~ElfImg() {
    //open elf file local
    if (buffer) {
        free(buffer);
        buffer = nullptr;
    }
    //use mmap
    if (header) {
        munmap(header, size);
    }
}

Elf_Addr
ElfImg::GetSymbolOffset(std::string_view name, bool warn_if_missing, bool match_prefix) const {
    Elf_Addr _offset = 0;

    //search dynmtab
    if (dynsym_start != nullptr && strtab_start != nullptr) {
        Elf_Sym *sym = dynsym_start;
        char *strings = (char *) strtab_start;
        int k;
        for (k = 0; k < dynsym_count; k++, sym++) {
            auto s = std::string_view(strings + sym->st_name);
            if (name.compare(s) == 0 || (match_prefix && s.starts_with(name))) {
                _offset = sym->st_value;
                // BEGIN Pine changed: Remove log
                // LOGD("find %s: %x\n", elf, _offset);
                // END Pine changed: Remove log
                return _offset;
            }
        }
    }

    //search symtab
    if (symtab_start != nullptr && symstr_offset_for_symtab != 0) {
        auto hdr = header_debugdata != nullptr ? header_debugdata : header;
        for (int i = 0; i < symtab_count; i++) {
            unsigned int st_type = ELF_ST_TYPE(symtab_start[i].st_info);
            // char *st_name = reinterpret_cast<char *>(((size_t) header) + symstr_offset_for_symtab +
            char *st_name = reinterpret_cast<char *>(((uintptr_t) hdr) +
                                                     symstr_offset_for_symtab +
                                                     symtab_start[i].st_name);
            if (st_type == STT_FUNC && symtab_start[i].st_size) {
                auto s = std::string_view(st_name);
                if (name.compare(s) == 0 || (match_prefix && s.starts_with(name))) {
                    _offset = symtab_start[i].st_value;
                    // BEGIN Pine changed: Remove log
                    // LOGD("find %s: %x\n", elf, _offset);
                    // END Pine changed: Remove log
                    return _offset;
                }
            }
        }
    }
    if (warn_if_missing) LOGE("Symbol %s not found in elf %s", std::string(name).c_str(), elf);
    return 0;
}

void *
ElfImg::GetSymbolAddress(std::string_view name, bool warn_if_missing, bool match_prefix) const {
    Elf_Addr offset = GetSymbolOffset(name, warn_if_missing, match_prefix);
    if (offset > 0 && base != nullptr) {
        // Pine changed: Use uintptr_t instead of size_t
        // return reinterpret_cast<void *>((size_t) base + offset - bias);
        return reinterpret_cast<void *>((uintptr_t) base + offset - bias);
    } else {
        return nullptr;
    }
}

void *ElfImg::GetModuleBase(const char *name) {
    FILE *maps;
    char buff[256];
    off_t load_addr;
    // Pine changed: Use bool to instead of int
    bool found = false;
    // Pine changed: add "e" to mode
    maps = fopen("/proc/self/maps", "re");
    while (fgets(buff, sizeof(buff), maps)) {
        if (strstr(buff, name) && (strstr(buff, "r-xp") || strstr(buff, "r--p"))) {
            found = true;
            break;
        }
    }

    if (!found) {
        LOGE("failed to read load address for %s", name);
        fclose(maps);
        return nullptr;
    }

    if (sscanf(buff, "%lx", &load_addr) != 1)
        LOGE("failed to read load address for %s", name);

    fclose(maps);

    return reinterpret_cast<void *>(load_addr);
}
