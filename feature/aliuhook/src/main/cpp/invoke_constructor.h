//
// Created by rushii on 2024-08-27.
//

#ifndef ALIUHOOK_INVOKE_CONSTRUCTOR_H
#define ALIUHOOK_INVOKE_CONSTRUCTOR_H

#include "jni.h"

bool LoadInvokeConstructorCache(JNIEnv *en, int android_version);

void UnloadInvokeConstructorCache(JNIEnv *);

bool
InvokeConstructorWithArgs(JNIEnv *env, jobject instance, jobject constructor, jobjectArray args);

#endif //ALIUHOOK_INVOKE_CONSTRUCTOR_H
