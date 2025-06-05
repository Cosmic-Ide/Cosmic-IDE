/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

#include "profile_saver.h"
#include <dobby.h>
#include "aliuhook.h"

#include "log.h"

static bool replace_process_profiling_info() {
    LOGD("Ignoring profile saver request");
    return true;
}

static void *backup = nullptr;

bool disable_profile_saver() {
    if (backup) {
        LOGW("disableProfileSaver called multiple times - It is already disabled.");
        return true;
    }

    void *process_profiling_info;
    // MIUI moment, see https://github.com/canyie/pine/commit/ef0f5fb08e6aa42656065e431c65106b41f87799
    process_profiling_info = AliuHook::elf_img.GetSymbolAddress(
            "_ZN3art12ProfileSaver20ProcessProfilingInfoEbPtb", false);
    if (!process_profiling_info) {
        const char *symbol;
        if (AliuHook::android_version < 26) {
            // https://android.googlesource.com/platform/art/+/nougat-release/runtime/jit/profile_saver.cc#270
            symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEPt";
        } else if (AliuHook::android_version < 31) {
            // https://android.googlesource.com/platform/art/+/android11-release/runtime/jit/profile_saver.cc#514
            symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt";
        } else {
            // https://android.googlesource.com/platform/art/+/android12-release/runtime/jit/profile_saver.cc#823
            symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt";
        }
        process_profiling_info = AliuHook::elf_img.GetSymbolAddress(symbol);

        // https://android.googlesource.com/platform/art/+/android15-qpr1-release/runtime/jit/profile_saver.cc#767
        // Android 15 QPR1 changed back to the same symbol as API <31
        // ART is also an APEX Android Mainline component, which can be back-ported down to API 31 via a Google Play Update
        if (!process_profiling_info && AliuHook::android_version >= 31) {
            symbol = "_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt";
            process_profiling_info = AliuHook::elf_img.GetSymbolAddress(symbol);
        }
    }

    if (!process_profiling_info) {
        LOGE("Failed to disable ProfileSaver: ProfileSaver::ProcessProfilingInfo not found");
        return false;
    }

    backup = InlineHooker(process_profiling_info,
                          reinterpret_cast<void *>(replace_process_profiling_info));

    if (backup) {
        LOGI("Successfully disabled ProfileSaver");
        return true;
    } else {
        LOGE("Failed to disable ProfileSaver");
        return false;
    }
}
