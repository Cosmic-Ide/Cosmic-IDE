//
// Created by rushii on 2024-08-27.
//

#include "invoke_constructor.h"
#include "aliuhook.h"

// Based on https://github.com/toolfactory/narcissus/blob/c81c3d0a6f0fb5ee8ab444d170db21ff7fe8a7ad/src/main/c/narcissus.c

jclass Integer_class;
jclass int_class;
jmethodID Integer_intValue_methodID;

jclass Long_class;
jclass long_class;
jmethodID Long_longValue_methodID;

jclass Short_class;
jclass short_class;
jmethodID Short_shortValue_methodID;

jclass Character_class;
jclass char_class;
jmethodID Character_charValue_methodID;

jclass Boolean_class;
jclass boolean_class;
jmethodID Boolean_booleanValue_methodID;

jclass Byte_class;
jclass byte_class;
jmethodID Byte_byteValue_methodID;

jclass Float_class;
jclass float_class;
jmethodID Float_floatValue_methodID;

jclass Double_class;
jclass double_class;
jmethodID Double_doubleValue_methodID;

jclass Executable_class;
jmethodID Executable_getParameterTypes_methodID;

jclass AbstractMethod_class;
jmethodID AbstractMethod_getParameterTypes_methodID;

void throwIllegalArgumentException(JNIEnv *env, const char *message);

bool unboxArgs(JNIEnv *env, jobject method, jobjectArray args, jsize argsCount, jvalue *args_out);

bool LoadInvokeConstructorCache(JNIEnv *env, int android_version) {
    Integer_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Integer"));
    if (env->ExceptionOccurred()) return false;
    int_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Integer_class,
                                                                     env->GetStaticFieldID(
                                                                             Integer_class, "TYPE",
                                                                             "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Integer_intValue_methodID = env->GetMethodID(Integer_class, "intValue", "()I");
    if (env->ExceptionOccurred()) return false;

    Long_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Long"));
    if (env->ExceptionOccurred()) return false;
    long_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Long_class,
                                                                      env->GetStaticFieldID(
                                                                              Long_class, "TYPE",
                                                                              "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Long_longValue_methodID = env->GetMethodID(Long_class, "longValue", "()J");
    if (env->ExceptionOccurred()) return false;

    Short_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Short"));
    if (env->ExceptionOccurred()) return false;
    short_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Short_class,
                                                                       env->GetStaticFieldID(
                                                                               Short_class, "TYPE",
                                                                               "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Short_shortValue_methodID = env->GetMethodID(Short_class, "shortValue", "()S");
    if (env->ExceptionOccurred()) return false;

    Character_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Character"));
    if (env->ExceptionOccurred()) return false;
    char_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Character_class,
                                                                      env->GetStaticFieldID(
                                                                              Character_class,
                                                                              "TYPE",
                                                                              "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Character_charValue_methodID = env->GetMethodID(Character_class, "charValue", "()C");
    if (env->ExceptionOccurred()) return false;

    Boolean_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Boolean"));
    if (env->ExceptionOccurred()) return false;
    boolean_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Boolean_class,
                                                                         env->GetStaticFieldID(
                                                                                 Boolean_class,
                                                                                 "TYPE",
                                                                                 "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Boolean_booleanValue_methodID = env->GetMethodID(Boolean_class, "booleanValue", "()Z");
    if (env->ExceptionOccurred()) return false;

    Byte_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Byte"));
    if (env->ExceptionOccurred()) return false;
    byte_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Byte_class,
                                                                      env->GetStaticFieldID(
                                                                              Byte_class, "TYPE",
                                                                              "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Byte_byteValue_methodID = env->GetMethodID(Byte_class, "byteValue", "()B");
    if (env->ExceptionOccurred()) return false;

    Float_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Float"));
    if (env->ExceptionOccurred()) return false;
    float_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Float_class,
                                                                       env->GetStaticFieldID(
                                                                               Float_class, "TYPE",
                                                                               "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Float_floatValue_methodID = env->GetMethodID(Float_class, "floatValue", "()F");
    if (env->ExceptionOccurred()) return false;

    Double_class = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Double"));
    if (env->ExceptionOccurred()) return false;
    double_class = (jclass) env->NewGlobalRef(env->GetStaticObjectField(Double_class,
                                                                        env->GetStaticFieldID(
                                                                                Double_class,
                                                                                "TYPE",
                                                                                "Ljava/lang/Class;")));
    if (env->ExceptionOccurred()) return false;
    Double_doubleValue_methodID = env->GetMethodID(Double_class, "doubleValue", "()D");
    if (env->ExceptionOccurred()) return false;

    if (android_version >= 26) {
        // https://cs.android.com/android/_/android/platform/libcore/+/e1f193f0f7ccd8a3c0557ec93055140c68546aa9
        // https://cs.android.com/android/_/android/platform/libcore/+/refs/tags/android-8.0.0_r1:ojluni/src/main/java/java/lang/reflect/Executable.java;l=222;drc=e77a467884a8b323a1ac6a229f06f9a032b141b5
        Executable_class = env->FindClass("java/lang/reflect/Executable");
        if (env->ExceptionOccurred()) return false;
        Executable_getParameterTypes_methodID = env->GetMethodID(Executable_class,
                                                                 "getParameterTypes",
                                                                 "()[Ljava/lang/Class;");
        if (env->ExceptionOccurred()) return false;
    } else {
        // https://cs.android.com/android/_/android/platform/libcore/+/refs/tags/android-7.0.0_r1:libart/src/main/java/java/lang/reflect/AbstractMethod.java;l=160;drc=f04099d77872a0742db6f67263e7edc0828a8af6
        AbstractMethod_class = env->FindClass("java/lang/reflect/AbstractMethod");
        if (env->ExceptionOccurred()) return false;
        AbstractMethod_getParameterTypes_methodID = env->GetMethodID(AbstractMethod_class,
                                                                     "getParameterTypes",
                                                                     "()[Ljava/lang/Class;");
        if (env->ExceptionOccurred()) return false;
    }

    return true;
}

void UnloadInvokeConstructorCache(JNIEnv *env) {
    env->DeleteGlobalRef(Integer_class);
    env->DeleteGlobalRef(int_class);
    env->DeleteGlobalRef(Long_class);
    env->DeleteGlobalRef(long_class);
    env->DeleteGlobalRef(Short_class);
    env->DeleteGlobalRef(short_class);
    env->DeleteGlobalRef(Character_class);
    env->DeleteGlobalRef(char_class);
    env->DeleteGlobalRef(Boolean_class);
    env->DeleteGlobalRef(boolean_class);
    env->DeleteGlobalRef(Byte_class);
    env->DeleteGlobalRef(byte_class);
    env->DeleteGlobalRef(Float_class);
    env->DeleteGlobalRef(float_class);
    env->DeleteGlobalRef(Double_class);
    env->DeleteGlobalRef(double_class);
    env->DeleteGlobalRef(Executable_class);
    env->DeleteGlobalRef(AbstractMethod_class);
}

bool
InvokeConstructorWithArgs(JNIEnv *env, jobject instance, jobject constructor, jobjectArray args) {
    jmethodID constructorMethodId = env->FromReflectedMethod(constructor);
    if (env->ExceptionOccurred()) return false;

    jsize argsCount = env->GetArrayLength(args);
    if (env->ExceptionOccurred()) return false;

    auto *unboxedArgs = new jvalue[argsCount];
    if (!unboxArgs(env, constructor, args, argsCount, unboxedArgs)) {
        delete[] unboxedArgs;
        return false;
    }

    env->CallVoidMethodA(instance, constructorMethodId, unboxedArgs);
    delete[] unboxedArgs;
    return !env->ExceptionOccurred();
}

// Unbox a jobjectArray of method invocation args into a jvalue array.
bool unboxArgs(JNIEnv *env, jobject method, jobjectArray args, jsize argsCount, jvalue *args_out) {
    // Get parameter types
    auto parameterTypes = (jobjectArray) env->CallObjectMethod(
            method,
            AliuHook::android_version >= 26
            ? Executable_getParameterTypes_methodID
            : AbstractMethod_getParameterTypes_methodID);
    if (env->ExceptionOccurred()) return false;

    jsize parameterCount = env->GetArrayLength(parameterTypes);
    if (env->ExceptionOccurred()) return false;

    if (argsCount != parameterCount) {
        throwIllegalArgumentException(env, "Tried to invoke method with wrong number of arguments");
        return false;
    }

    // Unbox non-varargs args
    for (jsize i = 0; i < argsCount; i++) {
        auto parameterType = (jclass) env->GetObjectArrayElement(parameterTypes, i);
        if (env->ExceptionOccurred()) return false;

        jobject arg = env->GetObjectArrayElement(args, i);
        if (env->ExceptionOccurred()) return false;

        jclass arg_type = arg == nullptr ? nullptr : env->GetObjectClass(arg);
        if (env->ExceptionOccurred()) return false;

#define TRY_UNBOX_ARG(_prim_type, _Prim_type, _Boxed_type, _jvalue_field) \
        if (env->IsSameObject(parameterType, _prim_type ## _class)) { \
            if (arg == NULL) { \
                throwIllegalArgumentException(env, "Tried to unbox a null argument; expected " #_Boxed_type); \
            } else if (!env->IsSameObject(arg_type, _Boxed_type ## _class)) { \
                throwIllegalArgumentException(env, "Tried to unbox arg of wrong type; expected " #_Boxed_type); \
            } else { \
                args_out[i]._jvalue_field = env->Call ## _Prim_type ## Method(arg, _Boxed_type ## _ ## _prim_type ## Value_methodID); \
            } \
        }

        TRY_UNBOX_ARG(int, Int, Integer, i)
        else TRY_UNBOX_ARG(long, Long, Long, j)
        else TRY_UNBOX_ARG(short, Short, Short, s)
        else TRY_UNBOX_ARG(char, Char, Character, c)
        else TRY_UNBOX_ARG(boolean, Boolean, Boolean, z)
        else TRY_UNBOX_ARG(byte, Byte, Byte, b)
        else TRY_UNBOX_ARG(float, Float, Float, f)
        else TRY_UNBOX_ARG(double, Double, Double, d)
        else {
            // Parameter type is not primitive -- check if arg is assignable from the parameter type
            if (arg != nullptr && !env->IsAssignableFrom(arg_type, parameterType)) {
                throwIllegalArgumentException(env,
                                              "Tried to invoke function with arg of incompatible type");
            } else {
                args_out[i].l = arg;
            }
        }

        if (env->ExceptionOccurred()) return false;
    }

    return true;
}

void throwIllegalArgumentException(JNIEnv *env, const char *message) {
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (cls) {
        env->ThrowNew(cls, message);
    }
}
