/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

#include "hidden_api.h"

#include "log.h"
#include "aliuhook.h"

bool disable_hidden_api(JNIEnv *env) {
    // Hidden api introduced in sdk 29
    if (AliuHook::android_version < 29) {
        return true;
    }

    void *addr = AliuHook::elf_img.GetSymbolAddress(
            "_ZN3artL32VMRuntime_setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
            true,
            /* match_prefix: OneUI appends a random set of numbers at the end */
            true);
    if (!addr) {
        LOGE("HiddenAPI: Didn't find setHiddenApiExemptions");
        return false;
    }

    jclass stringClass = env->FindClass("java/lang/String");
    // L is basically wildcard for everything
    jobjectArray args = env->NewObjectArray(1, stringClass, env->NewStringUTF("L"));

    auto func = reinterpret_cast<void (*)(JNIEnv *, jclass, jobjectArray)>(addr);
    // jclass arg is not used so pass string class for the memes
    func(env, stringClass, args);

    return true;
}