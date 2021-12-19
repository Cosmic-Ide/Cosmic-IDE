/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.pranav.ide.dx.dex.cf;

import com.pranav.ide.dex.util.ExceptionWithContext;
import com.pranav.ide.dx.cf.code.BootstrapMethodsList;
import com.pranav.ide.dx.cf.code.ConcreteMethod;
import com.pranav.ide.dx.cf.code.Ropper;
import com.pranav.ide.dx.cf.direct.DirectClassFile;
import com.pranav.ide.dx.cf.iface.Field;
import com.pranav.ide.dx.cf.iface.FieldList;
import com.pranav.ide.dx.cf.iface.Method;
import com.pranav.ide.dx.cf.iface.MethodList;
import com.pranav.ide.dx.command.dexer.DxContext;
import com.pranav.ide.dx.dex.DexOptions;
import com.pranav.ide.dx.dex.code.DalvCode;
import com.pranav.ide.dx.dex.code.PositionList;
import com.pranav.ide.dx.dex.code.RopTranslator;
import com.pranav.ide.dx.dex.file.CallSiteIdsSection;
import com.pranav.ide.dx.dex.file.ClassDefItem;
import com.pranav.ide.dx.dex.file.DexFile;
import com.pranav.ide.dx.dex.file.EncodedField;
import com.pranav.ide.dx.dex.file.EncodedMethod;
import com.pranav.ide.dx.dex.file.FieldIdsSection;
import com.pranav.ide.dx.dex.file.MethodHandlesSection;
import com.pranav.ide.dx.dex.file.MethodIdsSection;
import com.pranav.ide.dx.rop.annotation.Annotations;
import com.pranav.ide.dx.rop.annotation.AnnotationsList;
import com.pranav.ide.dx.rop.code.AccessFlags;
import com.pranav.ide.dx.rop.code.DexTranslationAdvice;
import com.pranav.ide.dx.rop.code.LocalVariableExtractor;
import com.pranav.ide.dx.rop.code.LocalVariableInfo;
import com.pranav.ide.dx.rop.code.RopMethod;
import com.pranav.ide.dx.rop.code.TranslationAdvice;
import com.pranav.ide.dx.rop.cst.Constant;
import com.pranav.ide.dx.rop.cst.ConstantPool;
import com.pranav.ide.dx.rop.cst.CstBaseMethodRef;
import com.pranav.ide.dx.rop.cst.CstBoolean;
import com.pranav.ide.dx.rop.cst.CstByte;
import com.pranav.ide.dx.rop.cst.CstCallSite;
import com.pranav.ide.dx.rop.cst.CstCallSiteRef;
import com.pranav.ide.dx.rop.cst.CstChar;
import com.pranav.ide.dx.rop.cst.CstEnumRef;
import com.pranav.ide.dx.rop.cst.CstFieldRef;
import com.pranav.ide.dx.rop.cst.CstInteger;
import com.pranav.ide.dx.rop.cst.CstInterfaceMethodRef;
import com.pranav.ide.dx.rop.cst.CstInvokeDynamic;
import com.pranav.ide.dx.rop.cst.CstMethodHandle;
import com.pranav.ide.dx.rop.cst.CstMethodRef;
import com.pranav.ide.dx.rop.cst.CstShort;
import com.pranav.ide.dx.rop.cst.CstString;
import com.pranav.ide.dx.rop.cst.CstType;
import com.pranav.ide.dx.rop.cst.TypedConstant;
import com.pranav.ide.dx.rop.type.Type;
import com.pranav.ide.dx.rop.type.TypeList;
import com.pranav.ide.dx.ssa.Optimizer;

/**
 * Static method that turns {@code byte[]}s containing Java
 * classfiles into {@link com.pranav.ide.dx.dex.file.ClassDefItem} instances.
 */
public class CfTranslator {
    /**
     * set to {@code true} to enable development-time debugging code
     */
    private static final boolean DEBUG = false;

    /**
     * This class is uninstantiable.
     */
    private CfTranslator() {
        // This space intentionally left blank.
    }

    /**
     * Takes a {@code byte[]}, interprets it as a Java classfile, and
     * translates it into a {@link com.pranav.ide.dx.dex.file.ClassDefItem}.
     *
     * @param context    {@code non-null;} the state global to this invocation.
     * @param cf         {@code non-null;} the class file
     * @param bytes      {@code non-null;} contents of the file
     * @param cfOptions  options for class translation
     * @param dexOptions options for dex output
     * @param dexFile    {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    public static com.pranav.ide.dx.dex.file.ClassDefItem translate(com.pranav.ide.dx.command.dexer.DxContext context, com.pranav.ide.dx.cf.direct.DirectClassFile cf, byte[] bytes,
                                                                    com.pranav.ide.dx.dex.cf.CfOptions cfOptions, com.pranav.ide.dx.dex.DexOptions dexOptions, DexFile dexFile) {
        try {
            return translate0(context, cf, bytes, cfOptions, dexOptions, dexFile);
        } catch (RuntimeException ex) {
            String msg = "...while processing " + cf.getFilePath();
            throw ExceptionWithContext.withContext(ex, msg);
        }
    }

    /**
     * Performs the main act of translation. This method is separated
     * from {@link #translate} just to keep things a bit simpler in
     * terms of exception handling.
     *
     * @param context    {@code non-null;} the state global to this invocation.
     * @param cf         {@code non-null;} the class file
     * @param bytes      {@code non-null;} contents of the file
     * @param cfOptions  options for class translation
     * @param dexOptions options for dex output
     * @param dexFile    {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    private static com.pranav.ide.dx.dex.file.ClassDefItem translate0(com.pranav.ide.dx.command.dexer.DxContext context, com.pranav.ide.dx.cf.direct.DirectClassFile cf, byte[] bytes,
                                                                      com.pranav.ide.dx.dex.cf.CfOptions cfOptions, com.pranav.ide.dx.dex.DexOptions dexOptions, DexFile dexFile) {

        context.optimizerOptions.loadOptimizeLists(cfOptions.optimizeListFile,
                cfOptions.dontOptimizeListFile);

        // Build up a class to output.

        com.pranav.ide.dx.rop.cst.CstType thisClass = cf.getThisClass();
        int classAccessFlags = cf.getAccessFlags() & ~com.pranav.ide.dx.rop.code.AccessFlags.ACC_SUPER;
        CstString sourceFile = (cfOptions.positionInfo == PositionList.NONE) ? null :
                cf.getSourceFile();
        com.pranav.ide.dx.dex.file.ClassDefItem out =
                new com.pranav.ide.dx.dex.file.ClassDefItem(thisClass, classAccessFlags,
                        cf.getSuperclass(), cf.getInterfaces(), sourceFile);

        com.pranav.ide.dx.rop.annotation.Annotations classAnnotations =
                com.pranav.ide.dx.dex.cf.AttributeTranslator.getClassAnnotations(cf, cfOptions);
        if (classAnnotations.size() != 0) {
            out.setClassAnnotations(classAnnotations, dexFile);
        }

        FieldIdsSection fieldIdsSection = dexFile.getFieldIds();
        MethodIdsSection methodIdsSection = dexFile.getMethodIds();
        MethodHandlesSection methodHandlesSection = dexFile.getMethodHandles();
        CallSiteIdsSection callSiteIds = dexFile.getCallSiteIds();
        processFields(cf, out, dexFile);
        processMethods(context, cf, cfOptions, dexOptions, out, dexFile);

        // intern constant pool method, field and type references
        ConstantPool constantPool = cf.getConstantPool();
        int constantPoolSize = constantPool.size();

        for (int i = 0; i < constantPoolSize; i++) {
            com.pranav.ide.dx.rop.cst.Constant constant = constantPool.getOrNull(i);
            if (constant instanceof com.pranav.ide.dx.rop.cst.CstMethodRef) {
                methodIdsSection.intern((CstBaseMethodRef) constant);
            } else if (constant instanceof com.pranav.ide.dx.rop.cst.CstInterfaceMethodRef) {
                methodIdsSection.intern(((CstInterfaceMethodRef) constant).toMethodRef());
            } else if (constant instanceof com.pranav.ide.dx.rop.cst.CstFieldRef) {
                fieldIdsSection.intern((com.pranav.ide.dx.rop.cst.CstFieldRef) constant);
            } else if (constant instanceof com.pranav.ide.dx.rop.cst.CstEnumRef) {
                fieldIdsSection.intern(((CstEnumRef) constant).getFieldRef());
            } else if (constant instanceof com.pranav.ide.dx.rop.cst.CstMethodHandle) {
                methodHandlesSection.intern((CstMethodHandle) constant);
            } else if (constant instanceof com.pranav.ide.dx.rop.cst.CstInvokeDynamic) {
                com.pranav.ide.dx.rop.cst.CstInvokeDynamic cstInvokeDynamic = (CstInvokeDynamic) constant;
                int index = cstInvokeDynamic.getBootstrapMethodIndex();
                BootstrapMethodsList.Item bootstrapMethod = cf.getBootstrapMethods().get(index);
                com.pranav.ide.dx.rop.cst.CstCallSite callSite =
                        CstCallSite.make(bootstrapMethod.getBootstrapMethodHandle(),
                                cstInvokeDynamic.getNat(),
                                bootstrapMethod.getBootstrapMethodArguments());
                cstInvokeDynamic.setDeclaringClass(cf.getThisClass());
                cstInvokeDynamic.setCallSite(callSite);
                for (CstCallSiteRef ref : cstInvokeDynamic.getReferences()) {
                    callSiteIds.intern(ref);
                }
            }
        }

        return out;
    }

    /**
     * Processes the fields of the given class.
     *
     * @param cf      {@code non-null;} class being translated
     * @param out     {@code non-null;} output class
     * @param dexFile {@code non-null;} dex output
     */
    private static void processFields(
            com.pranav.ide.dx.cf.direct.DirectClassFile cf, com.pranav.ide.dx.dex.file.ClassDefItem out, DexFile dexFile) {
        com.pranav.ide.dx.rop.cst.CstType thisClass = cf.getThisClass();
        FieldList fields = cf.getFields();
        int sz = fields.size();

        for (int i = 0; i < sz; i++) {
            Field one = fields.get(i);
            try {
                com.pranav.ide.dx.rop.cst.CstFieldRef field = new CstFieldRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();

                if (com.pranav.ide.dx.rop.code.AccessFlags.isStatic(accessFlags)) {
                    com.pranav.ide.dx.rop.cst.TypedConstant constVal = one.getConstantValue();
                    EncodedField fi = new EncodedField(field, accessFlags);
                    if (constVal != null) {
                        constVal = coerceConstant(constVal, field.getType());
                    }
                    out.addStaticField(fi, constVal);
                } else {
                    EncodedField fi = new EncodedField(field, accessFlags);
                    out.addInstanceField(fi);
                }

                com.pranav.ide.dx.rop.annotation.Annotations annotations =
                        com.pranav.ide.dx.dex.cf.AttributeTranslator.getAnnotations(one.getAttributes());
                if (annotations.size() != 0) {
                    out.addFieldAnnotations(field, annotations, dexFile);
                }
                dexFile.getFieldIds().intern(field);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                        " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper for {@link #processFields}, which translates constants into
     * more specific types if necessary.
     *
     * @param constant {@code non-null;} the constant in question
     * @param type     {@code non-null;} the desired type
     */
    private static com.pranav.ide.dx.rop.cst.TypedConstant coerceConstant(TypedConstant constant,
                                                                          com.pranav.ide.dx.rop.type.Type type) {
        com.pranav.ide.dx.rop.type.Type constantType = constant.getType();

        if (constantType.equals(type)) {
            return constant;
        }

        switch (type.getBasicType()) {
            case com.pranav.ide.dx.rop.type.Type.BT_BOOLEAN: {
                return CstBoolean.make(((com.pranav.ide.dx.rop.cst.CstInteger) constant).getValue());
            }
            case com.pranav.ide.dx.rop.type.Type.BT_BYTE: {
                return CstByte.make(((com.pranav.ide.dx.rop.cst.CstInteger) constant).getValue());
            }
            case com.pranav.ide.dx.rop.type.Type.BT_CHAR: {
                return CstChar.make(((com.pranav.ide.dx.rop.cst.CstInteger) constant).getValue());
            }
            case Type.BT_SHORT: {
                return CstShort.make(((CstInteger) constant).getValue());
            }
            default: {
                throw new UnsupportedOperationException("can't coerce " +
                        constant + " to " + type);
            }
        }
    }

    /**
     * Processes the methods of the given class.
     *
     * @param context    {@code non-null;} the state global to this invocation.
     * @param cf         {@code non-null;} class being translated
     * @param cfOptions  {@code non-null;} options for class translation
     * @param dexOptions {@code non-null;} options for dex output
     * @param out        {@code non-null;} output class
     * @param dexFile    {@code non-null;} dex output
     */
    private static void processMethods(com.pranav.ide.dx.command.dexer.DxContext context, DirectClassFile cf, com.pranav.ide.dx.dex.cf.CfOptions cfOptions,
                                       com.pranav.ide.dx.dex.DexOptions dexOptions, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();

        for (int i = 0; i < sz; i++) {
            Method one = methods.get(i);
            try {
                com.pranav.ide.dx.rop.cst.CstMethodRef meth = new CstMethodRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();
                boolean isStatic = com.pranav.ide.dx.rop.code.AccessFlags.isStatic(accessFlags);
                boolean isPrivate = com.pranav.ide.dx.rop.code.AccessFlags.isPrivate(accessFlags);
                boolean isNative = com.pranav.ide.dx.rop.code.AccessFlags.isNative(accessFlags);
                boolean isAbstract = com.pranav.ide.dx.rop.code.AccessFlags.isAbstract(accessFlags);
                boolean isConstructor = meth.isInstanceInit() ||
                        meth.isClassInit();
                DalvCode code;

                if (isNative || isAbstract) {
                    // There's no code for native or abstract methods.
                    code = null;
                } else {
                    com.pranav.ide.dx.cf.code.ConcreteMethod concrete =
                            new ConcreteMethod(one, cf,
                                    (cfOptions.positionInfo != PositionList.NONE),
                                    cfOptions.localInfo);

                    TranslationAdvice advice;

                    advice = DexTranslationAdvice.THE_ONE;

                    com.pranav.ide.dx.rop.code.RopMethod rmeth = Ropper.convert(concrete, advice, methods, dexOptions);
                    com.pranav.ide.dx.rop.code.RopMethod nonOptRmeth = null;
                    int paramSize;

                    paramSize = meth.getParameterWordCount(isStatic);

                    String canonicalName
                            = thisClass.getClassType().getDescriptor()
                            + "." + one.getName().getString();

                    if (cfOptions.optimize &&
                            context.optimizerOptions.shouldOptimize(canonicalName)) {
                        if (DEBUG) {
                            System.err.println("Optimizing " + canonicalName);
                        }

                        nonOptRmeth = rmeth;
                        rmeth = Optimizer.optimize(rmeth,
                                paramSize, isStatic, cfOptions.localInfo, advice);

                        if (DEBUG) {
                            context.optimizerOptions.compareOptimizerStep(nonOptRmeth,
                                    paramSize, isStatic, cfOptions, advice, rmeth);
                        }

                        if (cfOptions.statistics) {
                            context.codeStatistics.updateRopStatistics(
                                    nonOptRmeth, rmeth);
                        }
                    }

                    com.pranav.ide.dx.rop.code.LocalVariableInfo locals = null;

                    if (cfOptions.localInfo) {
                        locals = LocalVariableExtractor.extract(rmeth);
                    }

                    code = RopTranslator.translate(rmeth, cfOptions.positionInfo,
                            locals, paramSize, dexOptions);

                    if (cfOptions.statistics && nonOptRmeth != null) {
                        updateDexStatistics(context, cfOptions, dexOptions, rmeth, nonOptRmeth, locals,
                                paramSize, concrete.getCode().size());
                    }
                }

                // Preserve the synchronized flag as its "declared" variant...
                if (com.pranav.ide.dx.rop.code.AccessFlags.isSynchronized(accessFlags)) {
                    accessFlags |= com.pranav.ide.dx.rop.code.AccessFlags.ACC_DECLARED_SYNCHRONIZED;

                    /*
                     * ...but only native methods are actually allowed to be
                     * synchronized.
                     */
                    if (!isNative) {
                        accessFlags &= ~com.pranav.ide.dx.rop.code.AccessFlags.ACC_SYNCHRONIZED;
                    }
                }

                if (isConstructor) {
                    accessFlags |= AccessFlags.ACC_CONSTRUCTOR;
                }

                TypeList exceptions = com.pranav.ide.dx.dex.cf.AttributeTranslator.getExceptions(one);
                EncodedMethod mi =
                        new EncodedMethod(meth, accessFlags, code, exceptions);

                if (meth.isInstanceInit() || meth.isClassInit() ||
                        isStatic || isPrivate) {
                    out.addDirectMethod(mi);
                } else {
                    out.addVirtualMethod(mi);
                }

                Annotations annotations =
                        com.pranav.ide.dx.dex.cf.AttributeTranslator.getMethodAnnotations(one);
                if (annotations.size() != 0) {
                    out.addMethodAnnotations(meth, annotations, dexFile);
                }

                AnnotationsList list =
                        com.pranav.ide.dx.dex.cf.AttributeTranslator.getParameterAnnotations(one);
                if (list.size() != 0) {
                    out.addParameterAnnotations(meth, list, dexFile);
                }
                dexFile.getMethodIds().intern(meth);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                        " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper that updates the dex statistics.
     */
    private static void updateDexStatistics(DxContext context, CfOptions cfOptions, DexOptions dexOptions,
                                            com.pranav.ide.dx.rop.code.RopMethod optRmeth, RopMethod nonOptRmeth,
                                            LocalVariableInfo locals, int paramSize, int originalByteCount) {
        /*
         * Run rop->dex again on optimized vs. non-optimized method to
         * collect statistics. We have to totally convert both ways,
         * since converting the "real" method getting added to the
         * file would corrupt it (by messing with its constant pool
         * indices).
         */

        DalvCode optCode = RopTranslator.translate(optRmeth,
                cfOptions.positionInfo, locals, paramSize, dexOptions);
        DalvCode nonOptCode = RopTranslator.translate(nonOptRmeth,
                cfOptions.positionInfo, locals, paramSize, dexOptions);

        /*
         * Fake out the indices, so code.getInsns() can work well enough
         * for the current purpose.
         */

        DalvCode.AssignIndicesCallback callback =
                new DalvCode.AssignIndicesCallback() {
                    @Override
                    public int getIndex(Constant cst) {
                        // Everything is at index 0!
                        return 0;
                    }
                };

        optCode.assignIndices(callback);
        nonOptCode.assignIndices(callback);

        context.codeStatistics.updateDexStatistics(nonOptCode, optCode);
        context.codeStatistics.updateOriginalByteCount(originalByteCount);
    }
}
