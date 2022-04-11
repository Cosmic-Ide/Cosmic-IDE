/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of each opcode mnemonic according to the JVM specifications.
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpcodeStringValues implements IOpcodeMnemonics {

	public static final String[] BYTECODE_NAMES = new String[256];
	static {
		BYTECODE_NAMES[NOP] = "nop"; //$NON-NLS-1$
		BYTECODE_NAMES[ACONST_NULL] = "aconst_null"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_M1] = "iconst_m1"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_0] = "iconst_0"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_1] = "iconst_1"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_2] = "iconst_2"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_3] = "iconst_3"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_4] = "iconst_4"; //$NON-NLS-1$
		BYTECODE_NAMES[ICONST_5] = "iconst_5"; //$NON-NLS-1$
		BYTECODE_NAMES[LCONST_0] = "lconst_0"; //$NON-NLS-1$
		BYTECODE_NAMES[LCONST_1] = "lconst_1"; //$NON-NLS-1$
		BYTECODE_NAMES[FCONST_0] = "fconst_0"; //$NON-NLS-1$
		BYTECODE_NAMES[FCONST_1] = "fconst_1"; //$NON-NLS-1$
		BYTECODE_NAMES[FCONST_2] = "fconst_2"; //$NON-NLS-1$
		BYTECODE_NAMES[DCONST_0] = "dconst_0"; //$NON-NLS-1$
		BYTECODE_NAMES[DCONST_1] = "dconst_1"; //$NON-NLS-1$
		BYTECODE_NAMES[BIPUSH] = "bipush"; //$NON-NLS-1$
		BYTECODE_NAMES[SIPUSH] = "sipush"; //$NON-NLS-1$
		BYTECODE_NAMES[LDC] = "ldc"; //$NON-NLS-1$
		BYTECODE_NAMES[LDC_W] = "ldc_w"; //$NON-NLS-1$
		BYTECODE_NAMES[LDC2_W] = "ldc2_w"; //$NON-NLS-1$
		BYTECODE_NAMES[ILOAD] = "iload"; //$NON-NLS-1$
		BYTECODE_NAMES[LLOAD] = "lload"; //$NON-NLS-1$
		BYTECODE_NAMES[FLOAD] = "fload"; //$NON-NLS-1$
		BYTECODE_NAMES[DLOAD] = "dload"; //$NON-NLS-1$
		BYTECODE_NAMES[ALOAD] = "aload"; //$NON-NLS-1$
		BYTECODE_NAMES[ILOAD_0] = "iload_0"; //$NON-NLS-1$
		BYTECODE_NAMES[ILOAD_1] = "iload_1"; //$NON-NLS-1$
		BYTECODE_NAMES[ILOAD_2] = "iload_2"; //$NON-NLS-1$
		BYTECODE_NAMES[ILOAD_3] = "iload_3"; //$NON-NLS-1$
		BYTECODE_NAMES[LLOAD_0] = "lload_0"; //$NON-NLS-1$
		BYTECODE_NAMES[LLOAD_1] = "lload_1"; //$NON-NLS-1$
		BYTECODE_NAMES[LLOAD_2] = "lload_2"; //$NON-NLS-1$
		BYTECODE_NAMES[LLOAD_3] = "lload_3"; //$NON-NLS-1$
		BYTECODE_NAMES[FLOAD_0] = "fload_0"; //$NON-NLS-1$
		BYTECODE_NAMES[FLOAD_1] = "fload_1"; //$NON-NLS-1$
		BYTECODE_NAMES[FLOAD_2] = "fload_2"; //$NON-NLS-1$
		BYTECODE_NAMES[FLOAD_3] = "fload_3"; //$NON-NLS-1$
		BYTECODE_NAMES[DLOAD_0] = "dload_0"; //$NON-NLS-1$
		BYTECODE_NAMES[DLOAD_1] = "dload_1"; //$NON-NLS-1$
		BYTECODE_NAMES[DLOAD_2] = "dload_2"; //$NON-NLS-1$
		BYTECODE_NAMES[DLOAD_3] = "dload_3"; //$NON-NLS-1$
		BYTECODE_NAMES[ALOAD_0] = "aload_0"; //$NON-NLS-1$
		BYTECODE_NAMES[ALOAD_1] = "aload_1"; //$NON-NLS-1$
		BYTECODE_NAMES[ALOAD_2] = "aload_2"; //$NON-NLS-1$
		BYTECODE_NAMES[ALOAD_3] = "aload_3"; //$NON-NLS-1$
		BYTECODE_NAMES[IALOAD] = "iaload"; //$NON-NLS-1$
		BYTECODE_NAMES[LALOAD] = "laload"; //$NON-NLS-1$
		BYTECODE_NAMES[FALOAD] = "faload"; //$NON-NLS-1$
		BYTECODE_NAMES[DALOAD] = "daload"; //$NON-NLS-1$
		BYTECODE_NAMES[AALOAD] = "aaload"; //$NON-NLS-1$
		BYTECODE_NAMES[BALOAD] = "baload"; //$NON-NLS-1$
		BYTECODE_NAMES[CALOAD] = "caload"; //$NON-NLS-1$
		BYTECODE_NAMES[SALOAD] = "saload"; //$NON-NLS-1$
		BYTECODE_NAMES[ISTORE] = "istore"; //$NON-NLS-1$
		BYTECODE_NAMES[LSTORE] = "lstore"; //$NON-NLS-1$
		BYTECODE_NAMES[FSTORE] = "fstore"; //$NON-NLS-1$
		BYTECODE_NAMES[DSTORE] = "dstore"; //$NON-NLS-1$
		BYTECODE_NAMES[ASTORE] = "astore"; //$NON-NLS-1$
		BYTECODE_NAMES[ISTORE_0] = "istore_0"; //$NON-NLS-1$
		BYTECODE_NAMES[ISTORE_1] = "istore_1"; //$NON-NLS-1$
		BYTECODE_NAMES[ISTORE_2] = "istore_2"; //$NON-NLS-1$
		BYTECODE_NAMES[ISTORE_3] = "istore_3"; //$NON-NLS-1$
		BYTECODE_NAMES[LSTORE_0] = "lstore_0"; //$NON-NLS-1$
		BYTECODE_NAMES[LSTORE_1] = "lstore_1"; //$NON-NLS-1$
		BYTECODE_NAMES[LSTORE_2] = "lstore_2"; //$NON-NLS-1$
		BYTECODE_NAMES[LSTORE_3] = "lstore_3"; //$NON-NLS-1$
		BYTECODE_NAMES[FSTORE_0] = "fstore_0"; //$NON-NLS-1$
		BYTECODE_NAMES[FSTORE_1] = "fstore_1"; //$NON-NLS-1$
		BYTECODE_NAMES[FSTORE_2] = "fstore_2"; //$NON-NLS-1$
		BYTECODE_NAMES[FSTORE_3] = "fstore_3"; //$NON-NLS-1$
		BYTECODE_NAMES[DSTORE_0] = "dstore_0"; //$NON-NLS-1$
		BYTECODE_NAMES[DSTORE_1] = "dstore_1"; //$NON-NLS-1$
		BYTECODE_NAMES[DSTORE_2] = "dstore_2"; //$NON-NLS-1$
		BYTECODE_NAMES[DSTORE_3] = "dstore_3"; //$NON-NLS-1$
		BYTECODE_NAMES[ASTORE_0] = "astore_0"; //$NON-NLS-1$
		BYTECODE_NAMES[ASTORE_1] = "astore_1"; //$NON-NLS-1$
		BYTECODE_NAMES[ASTORE_2] = "astore_2"; //$NON-NLS-1$
		BYTECODE_NAMES[ASTORE_3] = "astore_3"; //$NON-NLS-1$
		BYTECODE_NAMES[IASTORE] = "iastore"; //$NON-NLS-1$
		BYTECODE_NAMES[LASTORE] = "lastore"; //$NON-NLS-1$
		BYTECODE_NAMES[FASTORE] = "fastore"; //$NON-NLS-1$
		BYTECODE_NAMES[DASTORE] = "dastore"; //$NON-NLS-1$
		BYTECODE_NAMES[AASTORE] = "aastore"; //$NON-NLS-1$
		BYTECODE_NAMES[BASTORE] = "bastore"; //$NON-NLS-1$
		BYTECODE_NAMES[CASTORE] = "castore"; //$NON-NLS-1$
		BYTECODE_NAMES[SASTORE] = "sastore"; //$NON-NLS-1$
		BYTECODE_NAMES[POP] = "pop"; //$NON-NLS-1$
		BYTECODE_NAMES[POP2] = "pop2"; //$NON-NLS-1$
		BYTECODE_NAMES[DUP] = "dup"; //$NON-NLS-1$
		BYTECODE_NAMES[DUP_X1] = "dup_x1"; //$NON-NLS-1$
		BYTECODE_NAMES[DUP_X2] = "dup_x2"; //$NON-NLS-1$
		BYTECODE_NAMES[DUP2] = "dup2"; //$NON-NLS-1$
		BYTECODE_NAMES[DUP2_X1] = "dup2_x1"; //$NON-NLS-1$
		BYTECODE_NAMES[DUP2_X2] = "dup2_x2"; //$NON-NLS-1$
		BYTECODE_NAMES[SWAP] = "swap"; //$NON-NLS-1$
		BYTECODE_NAMES[IADD] = "iadd"; //$NON-NLS-1$
		BYTECODE_NAMES[LADD] = "ladd"; //$NON-NLS-1$
		BYTECODE_NAMES[FADD] = "fadd"; //$NON-NLS-1$
		BYTECODE_NAMES[DADD] = "dadd"; //$NON-NLS-1$
		BYTECODE_NAMES[ISUB] = "isub"; //$NON-NLS-1$
		BYTECODE_NAMES[LSUB] = "lsub"; //$NON-NLS-1$
		BYTECODE_NAMES[FSUB] = "fsub"; //$NON-NLS-1$
		BYTECODE_NAMES[DSUB] = "dsub"; //$NON-NLS-1$
		BYTECODE_NAMES[IMUL] = "imul"; //$NON-NLS-1$
		BYTECODE_NAMES[LMUL] = "lmul"; //$NON-NLS-1$
		BYTECODE_NAMES[FMUL] = "fmul"; //$NON-NLS-1$
		BYTECODE_NAMES[DMUL] = "dmul"; //$NON-NLS-1$
		BYTECODE_NAMES[IDIV] = "idiv"; //$NON-NLS-1$
		BYTECODE_NAMES[LDIV] = "ldiv"; //$NON-NLS-1$
		BYTECODE_NAMES[FDIV] = "fdiv"; //$NON-NLS-1$
		BYTECODE_NAMES[DDIV] = "ddiv"; //$NON-NLS-1$
		BYTECODE_NAMES[IREM] = "irem"; //$NON-NLS-1$
		BYTECODE_NAMES[LREM] = "lrem"; //$NON-NLS-1$
		BYTECODE_NAMES[FREM] = "frem"; //$NON-NLS-1$
		BYTECODE_NAMES[DREM] = "drem"; //$NON-NLS-1$
		BYTECODE_NAMES[INEG] = "ineg"; //$NON-NLS-1$
		BYTECODE_NAMES[LNEG] = "lneg"; //$NON-NLS-1$
		BYTECODE_NAMES[FNEG] = "fneg"; //$NON-NLS-1$
		BYTECODE_NAMES[DNEG] = "dneg"; //$NON-NLS-1$
		BYTECODE_NAMES[ISHL] = "ishl"; //$NON-NLS-1$
		BYTECODE_NAMES[LSHL] = "lshl"; //$NON-NLS-1$
		BYTECODE_NAMES[ISHR] = "ishr"; //$NON-NLS-1$
		BYTECODE_NAMES[LSHR] = "lshr"; //$NON-NLS-1$
		BYTECODE_NAMES[IUSHR] = "iushr"; //$NON-NLS-1$
		BYTECODE_NAMES[LUSHR] = "lushr"; //$NON-NLS-1$
		BYTECODE_NAMES[IAND] = "iand"; //$NON-NLS-1$
		BYTECODE_NAMES[LAND] = "land"; //$NON-NLS-1$
		BYTECODE_NAMES[IOR] = "ior"; //$NON-NLS-1$
		BYTECODE_NAMES[LOR] = "lor"; //$NON-NLS-1$
		BYTECODE_NAMES[IXOR] = "ixor"; //$NON-NLS-1$
		BYTECODE_NAMES[LXOR] = "lxor"; //$NON-NLS-1$
		BYTECODE_NAMES[IINC] = "iinc"; //$NON-NLS-1$
		BYTECODE_NAMES[I2L] = "i2l"; //$NON-NLS-1$
		BYTECODE_NAMES[I2F] = "i2f"; //$NON-NLS-1$
		BYTECODE_NAMES[I2D] = "i2d"; //$NON-NLS-1$
		BYTECODE_NAMES[L2I] = "l2i"; //$NON-NLS-1$
		BYTECODE_NAMES[L2F] = "l2f"; //$NON-NLS-1$
		BYTECODE_NAMES[L2D] = "l2d"; //$NON-NLS-1$
		BYTECODE_NAMES[F2I] = "f2i"; //$NON-NLS-1$
		BYTECODE_NAMES[F2L] = "f2l"; //$NON-NLS-1$
		BYTECODE_NAMES[F2D] = "f2d"; //$NON-NLS-1$
		BYTECODE_NAMES[D2I] = "d2i"; //$NON-NLS-1$
		BYTECODE_NAMES[D2L] = "d2l"; //$NON-NLS-1$
		BYTECODE_NAMES[D2F] = "d2f"; //$NON-NLS-1$
		BYTECODE_NAMES[I2B] = "i2b"; //$NON-NLS-1$
		BYTECODE_NAMES[I2C] = "i2c"; //$NON-NLS-1$
		BYTECODE_NAMES[I2S] = "i2s"; //$NON-NLS-1$
		BYTECODE_NAMES[LCMP] = "lcmp"; //$NON-NLS-1$
		BYTECODE_NAMES[FCMPL] = "fcmpl"; //$NON-NLS-1$
		BYTECODE_NAMES[FCMPG] = "fcmpg"; //$NON-NLS-1$
		BYTECODE_NAMES[DCMPL] = "dcmpl"; //$NON-NLS-1$
		BYTECODE_NAMES[DCMPG] = "dcmpg"; //$NON-NLS-1$
		BYTECODE_NAMES[IFEQ] = "ifeq"; //$NON-NLS-1$
		BYTECODE_NAMES[IFNE] = "ifne"; //$NON-NLS-1$
		BYTECODE_NAMES[IFLT] = "iflt"; //$NON-NLS-1$
		BYTECODE_NAMES[IFGE] = "ifge"; //$NON-NLS-1$
		BYTECODE_NAMES[IFGT] = "ifgt"; //$NON-NLS-1$
		BYTECODE_NAMES[IFLE] = "ifle"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ICMPEQ] = "if_icmpeq"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ICMPNE] = "if_icmpne"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ICMPLT] = "if_icmplt"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ICMPGE] = "if_icmpge"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ICMPGT] = "if_icmpgt"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ICMPLE] = "if_icmple"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ACMPEQ] = "if_acmpeq"; //$NON-NLS-1$
		BYTECODE_NAMES[IF_ACMPNE] = "if_acmpne"; //$NON-NLS-1$
		BYTECODE_NAMES[GOTO] = "goto"; //$NON-NLS-1$
		BYTECODE_NAMES[JSR] = "jsr"; //$NON-NLS-1$
		BYTECODE_NAMES[RET] = "ret"; //$NON-NLS-1$
		BYTECODE_NAMES[TABLESWITCH] = "tableswitch"; //$NON-NLS-1$
		BYTECODE_NAMES[LOOKUPSWITCH] = "lookupswitch"; //$NON-NLS-1$
		BYTECODE_NAMES[IRETURN] = "ireturn"; //$NON-NLS-1$
		BYTECODE_NAMES[LRETURN] = "lreturn"; //$NON-NLS-1$
		BYTECODE_NAMES[FRETURN] = "freturn"; //$NON-NLS-1$
		BYTECODE_NAMES[DRETURN] = "dreturn"; //$NON-NLS-1$
		BYTECODE_NAMES[ARETURN] = "areturn"; //$NON-NLS-1$
		BYTECODE_NAMES[RETURN] = "return"; //$NON-NLS-1$
		BYTECODE_NAMES[GETSTATIC] = "getstatic"; //$NON-NLS-1$
		BYTECODE_NAMES[PUTSTATIC] = "putstatic"; //$NON-NLS-1$
		BYTECODE_NAMES[GETFIELD] = "getfield"; //$NON-NLS-1$
		BYTECODE_NAMES[PUTFIELD] = "putfield"; //$NON-NLS-1$
		BYTECODE_NAMES[INVOKEVIRTUAL] = "invokevirtual"; //$NON-NLS-1$
		BYTECODE_NAMES[INVOKESPECIAL] = "invokespecial"; //$NON-NLS-1$
		BYTECODE_NAMES[INVOKESTATIC] = "invokestatic"; //$NON-NLS-1$
		BYTECODE_NAMES[INVOKEINTERFACE] = "invokeinterface"; //$NON-NLS-1$
		BYTECODE_NAMES[INVOKEDYNAMIC] = "invokedynamic"; //$NON-NLS-1$
		BYTECODE_NAMES[NEW] = "new"; //$NON-NLS-1$
		BYTECODE_NAMES[NEWARRAY] = "newarray"; //$NON-NLS-1$
		BYTECODE_NAMES[ANEWARRAY] = "anewarray"; //$NON-NLS-1$
		BYTECODE_NAMES[ARRAYLENGTH] = "arraylength"; //$NON-NLS-1$
		BYTECODE_NAMES[ATHROW] = "athrow"; //$NON-NLS-1$
		BYTECODE_NAMES[CHECKCAST] = "checkcast"; //$NON-NLS-1$
		BYTECODE_NAMES[INSTANCEOF] = "instanceof"; //$NON-NLS-1$
		BYTECODE_NAMES[MONITORENTER] = "monitorenter"; //$NON-NLS-1$
		BYTECODE_NAMES[MONITOREXIT] = "monitorexit"; //$NON-NLS-1$
		BYTECODE_NAMES[WIDE] = "wide"; //$NON-NLS-1$
		BYTECODE_NAMES[MULTIANEWARRAY] = "multianewarray"; //$NON-NLS-1$
		BYTECODE_NAMES[IFNULL] = "ifnull"; //$NON-NLS-1$
		BYTECODE_NAMES[IFNONNULL] = "ifnonnull"; //$NON-NLS-1$
		BYTECODE_NAMES[GOTO_W] = "goto_w"; //$NON-NLS-1$
		BYTECODE_NAMES[JSR_W] = "jsr_w"; //$NON-NLS-1$
		BYTECODE_NAMES[BREAKPOINT] = "breakpoint"; //$NON-NLS-1$
		BYTECODE_NAMES[IMPDEP1] = "impdep1"; //$NON-NLS-1$
		BYTECODE_NAMES[IMPDEP2] = "impdep2"; //$NON-NLS-1$
	}
}
