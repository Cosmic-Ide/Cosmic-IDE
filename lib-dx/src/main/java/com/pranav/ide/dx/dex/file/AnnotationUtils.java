/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pranav.ide.dx.dex.file;

import com.pranav.ide.dx.rop.annotation.Annotation;
import com.pranav.ide.dx.rop.annotation.AnnotationVisibility;
import com.pranav.ide.dx.rop.annotation.NameValuePair;
import com.pranav.ide.dx.rop.cst.Constant;
import com.pranav.ide.dx.rop.cst.CstAnnotation;
import com.pranav.ide.dx.rop.cst.CstArray;
import com.pranav.ide.dx.rop.cst.CstInteger;
import com.pranav.ide.dx.rop.cst.CstKnownNull;
import com.pranav.ide.dx.rop.cst.CstMethodRef;
import com.pranav.ide.dx.rop.cst.CstString;
import com.pranav.ide.dx.rop.cst.CstType;
import com.pranav.ide.dx.rop.type.Type;
import com.pranav.ide.dx.rop.type.TypeList;

import java.util.ArrayList;

/**
 * Utility class for dealing with annotations.
 */
public final class AnnotationUtils {

    /**
     * {@code non-null;} type for {@code AnnotationDefault} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType ANNOTATION_DEFAULT_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/AnnotationDefault;"));

    /**
     * {@code non-null;} type for {@code EnclosingClass} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType ENCLOSING_CLASS_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/EnclosingClass;"));

    /**
     * {@code non-null;} type for {@code EnclosingMethod} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType ENCLOSING_METHOD_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/EnclosingMethod;"));

    /**
     * {@code non-null;} type for {@code InnerClass} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType INNER_CLASS_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/InnerClass;"));

    /**
     * {@code non-null;} type for {@code MemberClasses} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType MEMBER_CLASSES_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/MemberClasses;"));

    /**
     * {@code non-null;} type for {@code Signature} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType SIGNATURE_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/Signature;"));

    /**
     * {@code non-null;} type for {@code SourceDebugExtension} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType SOURCE_DEBUG_EXTENSION_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(com.pranav.ide.dx.rop.type.Type.intern("Ldalvik/annotation/SourceDebugExtension;"));

    /**
     * {@code non-null;} type for {@code Throws} annotations
     */
    private static final com.pranav.ide.dx.rop.cst.CstType THROWS_TYPE =
            com.pranav.ide.dx.rop.cst.CstType.intern(Type.intern("Ldalvik/annotation/Throws;"));

    /**
     * {@code non-null;} the UTF-8 constant {@code "accessFlags"}
     */
    private static final com.pranav.ide.dx.rop.cst.CstString ACCESS_FLAGS_STRING = new com.pranav.ide.dx.rop.cst.CstString("accessFlags");

    /**
     * {@code non-null;} the UTF-8 constant {@code "name"}
     */
    private static final com.pranav.ide.dx.rop.cst.CstString NAME_STRING = new com.pranav.ide.dx.rop.cst.CstString("name");

    /**
     * {@code non-null;} the UTF-8 constant {@code "value"}
     */
    private static final com.pranav.ide.dx.rop.cst.CstString VALUE_STRING = new com.pranav.ide.dx.rop.cst.CstString("value");

    /**
     * This class is uninstantiable.
     */
    private AnnotationUtils() {
        // This space intentionally left blank.
    }

    /**
     * Constructs a standard {@code AnnotationDefault} annotation.
     *
     * @param defaults {@code non-null;} the defaults, itself as an annotation
     * @return {@code non-null;} the constructed annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeAnnotationDefault(com.pranav.ide.dx.rop.annotation.Annotation defaults) {
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(ANNOTATION_DEFAULT_TYPE, AnnotationVisibility.SYSTEM);

        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(VALUE_STRING, new CstAnnotation(defaults)));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code EnclosingClass} annotation.
     *
     * @param clazz {@code non-null;} the enclosing class
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeEnclosingClass(com.pranav.ide.dx.rop.cst.CstType clazz) {
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(ENCLOSING_CLASS_TYPE, AnnotationVisibility.SYSTEM);

        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(VALUE_STRING, clazz));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code EnclosingMethod} annotation.
     *
     * @param method {@code non-null;} the enclosing method
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeEnclosingMethod(CstMethodRef method) {
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(ENCLOSING_METHOD_TYPE, AnnotationVisibility.SYSTEM);

        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(VALUE_STRING, method));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code InnerClass} annotation.
     *
     * @param name        {@code null-ok;} the original name of the class, or
     *                    {@code null} to represent an anonymous class
     * @param accessFlags the original access flags
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeInnerClass(com.pranav.ide.dx.rop.cst.CstString name, int accessFlags) {
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(INNER_CLASS_TYPE, AnnotationVisibility.SYSTEM);
        Constant nameCst = (name != null) ? name : CstKnownNull.THE_ONE;

        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(NAME_STRING, nameCst));
        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(ACCESS_FLAGS_STRING,
                CstInteger.make(accessFlags)));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code MemberClasses} annotation.
     *
     * @param types {@code non-null;} the list of (the types of) the member classes
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeMemberClasses(com.pranav.ide.dx.rop.type.TypeList types) {
        com.pranav.ide.dx.rop.cst.CstArray array = makeCstArray(types);
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(MEMBER_CLASSES_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(VALUE_STRING, array));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code Signature} annotation.
     *
     * @param signature {@code non-null;} the signature string
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeSignature(com.pranav.ide.dx.rop.cst.CstString signature) {
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(SIGNATURE_TYPE, AnnotationVisibility.SYSTEM);

        /*
         * Split the string into pieces that are likely to be common
         * across many signatures and the rest of the file.
         */

        String raw = signature.getString();
        int rawLength = raw.length();
        ArrayList<String> pieces = new ArrayList<String>(20);

        for (int at = 0; at < rawLength; /*at*/) {
            char c = raw.charAt(at);
            int endAt = at + 1;
            if (c == 'L') {
                // Scan to ';' or '<'. Consume ';' but not '<'.
                while (endAt < rawLength) {
                    c = raw.charAt(endAt);
                    if (c == ';') {
                        endAt++;
                        break;
                    } else if (c == '<') {
                        break;
                    }
                    endAt++;
                }
            } else {
                // Scan to 'L' without consuming it.
                while (endAt < rawLength) {
                    c = raw.charAt(endAt);
                    if (c == 'L') {
                        break;
                    }
                    endAt++;
                }
            }

            pieces.add(raw.substring(at, endAt));
            at = endAt;
        }

        int size = pieces.size();
        com.pranav.ide.dx.rop.cst.CstArray.List list = new com.pranav.ide.dx.rop.cst.CstArray.List(size);

        for (int i = 0; i < size; i++) {
            list.set(i, new com.pranav.ide.dx.rop.cst.CstString(pieces.get(i)));
        }

        list.setImmutable();

        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(VALUE_STRING, new com.pranav.ide.dx.rop.cst.CstArray(list)));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code SourceDebugExtension} annotation.
     *
     * @param smapString {@code non-null;} the SMAP string associated with
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeSourceDebugExtension(CstString smapString) {
        com.pranav.ide.dx.rop.annotation.Annotation result = new com.pranav.ide.dx.rop.annotation.Annotation(SOURCE_DEBUG_EXTENSION_TYPE, AnnotationVisibility.SYSTEM);

        result.put(new com.pranav.ide.dx.rop.annotation.NameValuePair(VALUE_STRING, smapString));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code Throws} annotation.
     *
     * @param types {@code non-null;} the list of thrown types
     * @return {@code non-null;} the annotation
     */
    public static com.pranav.ide.dx.rop.annotation.Annotation makeThrows(com.pranav.ide.dx.rop.type.TypeList types) {
        com.pranav.ide.dx.rop.cst.CstArray array = makeCstArray(types);
        com.pranav.ide.dx.rop.annotation.Annotation result = new Annotation(THROWS_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(VALUE_STRING, array));
        result.setImmutable();
        return result;
    }

    /**
     * Converts a {@link com.pranav.ide.dx.rop.type.TypeList} to a {@link com.pranav.ide.dx.rop.cst.CstArray}.
     *
     * @param types {@code non-null;} the type list
     * @return {@code non-null;} the corresponding array constant
     */
    private static com.pranav.ide.dx.rop.cst.CstArray makeCstArray(TypeList types) {
        int size = types.size();
        com.pranav.ide.dx.rop.cst.CstArray.List list = new com.pranav.ide.dx.rop.cst.CstArray.List(size);

        for (int i = 0; i < size; i++) {
            list.set(i, CstType.intern(types.getType(i)));
        }

        list.setImmutable();
        return new CstArray(list);
    }
}
