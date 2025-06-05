/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

#include <jni.h>
#include <string>
#include <lsplant.hpp>
#include <dobby.h>
#include <sys/mman.h>
#include <bits/sysconf.h>
#include "elf_img.h"
#include "log.h"
#include "profile_saver.h"
#include "hidden_api.h"
#include <sys/system_properties.h>
#include <cstdlib>
#include <cerrno>
#include "aliuhook.h"
#include "invoke_constructor.h"

int AliuHook::android_version = -1;
pine::ElfImg AliuHook::elf_img; // NOLINT(cert-err58-cpp)

void AliuHook::init(int version) {
    elf_img.Init("libart.so", version);
    android_version = version;
}

static size_t page_size_;

// Macros to align addresses to page boundaries
#define ALIGN_DOWN(addr, page_size)         ((addr) & -(page_size))
#define ALIGN_UP(addr, page_size)           (((addr) + ((page_size) - 1)) & ~((page_size) - 1))

static bool Unprotect(void *addr) {
    auto addr_uint = reinterpret_cast<uintptr_t>(addr);
    auto page_aligned_prt = reinterpret_cast<void *>(ALIGN_DOWN(addr_uint, page_size_));
    size_t size = page_size_;
    if (ALIGN_UP(addr_uint + page_size_, page_size_) != ALIGN_UP(addr_uint, page_size_)) {
        size += page_size_;
    }

    int result = mprotect(page_aligned_prt, size, PROT_READ | PROT_WRITE | PROT_EXEC);
    if (result == -1) {
        LOGE("mprotect failed for %p: %s (%d)", addr, strerror(errno), errno);
        return false;
    }
    return true;
}

void *InlineHooker(void *address, void *replacement) {
    if (!Unprotect(address)) {
        return nullptr;
    }

    void *origin_call;
    if (DobbyHook(address, replacement, &origin_call) == RS_SUCCESS) {
        return origin_call;
    } else {
        return nullptr;
    }
}

bool InlineUnhooker(void *func) {
    return DobbyDestroy(func) == RT_SUCCESS;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_isHooked0(JNIEnv *env, jclass, jobject method) {
    return lsplant::IsHooked(env, method);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_de_robv_android_xposed_XposedBridge_hook0(JNIEnv *env, jclass, jobject context,
                                               jobject original,
                                               jobject callback) {
    return lsplant::Hook(env, original, context, callback);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_unhook0(JNIEnv *env, jclass, jobject target) {
    return lsplant::UnHook(env, target);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_deoptimize0(JNIEnv *env, jclass, jobject method) {
    return lsplant::Deoptimize(env, method);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_makeClassInheritable0(JNIEnv *env, jclass, jclass clazz) {
    return lsplant::MakeClassInheritable(env, clazz);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_disableProfileSaver(JNIEnv *, jclass) {
    return disable_profile_saver();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_disableHiddenApiRestrictions(JNIEnv *env, jclass) {
    return disable_hidden_api(env);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_de_robv_android_xposed_XposedBridge_allocateInstance0(JNIEnv *env, jclass, jclass clazz) {
    return env->AllocObject(clazz);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_de_robv_android_xposed_XposedBridge_invokeConstructor0(JNIEnv *env, jclass, jobject instance,
                                                            jobject constructor,
                                                            jobjectArray args) {
    jmethodID constructorMethodId = env->FromReflectedMethod(constructor);
    if (!constructorMethodId) return JNI_FALSE;

    if (!args) {
        env->CallVoidMethod(instance, constructorMethodId);
        return JNI_TRUE;
    } else {
        return InvokeConstructorWithArgs(env, instance, constructor, args);
    }
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    page_size_ = static_cast<const size_t>(sysconf(_SC_PAGESIZE));

    {
        int api_level = android_get_device_api_level();

        if (api_level <= 0) {
            LOGE("Invalid SDK int %i", api_level);
            return JNI_ERR;
        }

        AliuHook::init(static_cast<int>(api_level));
    }

    lsplant::InitInfo initInfo{
            .inline_hooker = InlineHooker,
            .inline_unhooker = InlineUnhooker,
            .art_symbol_resolver = [](std::string_view symbol) -> void * {
                return AliuHook::elf_img.GetSymbolAddress(symbol, false, false);
            },
            .art_symbol_prefix_resolver = [](std::string_view symbol) -> void * {
                return AliuHook::elf_img.GetSymbolAddress(symbol, false, true);
            }
    };

    bool res = lsplant::Init(env, initInfo);
    if (!res) {
        LOGE("lsplant init failed");
        return JNI_ERR;
    }

    LOGI("lsplant init finished");

    res = LoadInvokeConstructorCache(env, AliuHook::android_version);
    if (!res) {
        LOGE("invoke_constructor init failed");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_1);

    UnloadInvokeConstructorCache(env);
}
