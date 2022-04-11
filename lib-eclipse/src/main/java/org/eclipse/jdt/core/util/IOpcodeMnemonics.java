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
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOpcodeMnemonics {

	int NOP = 0x00;
	int ACONST_NULL = 0x01;
	int ICONST_M1 = 0x02;
	int ICONST_0 = 0x03;
	int ICONST_1 = 0x04;
	int ICONST_2 = 0x05;
	int ICONST_3 = 0x06;
	int ICONST_4 = 0x07;
	int ICONST_5 = 0x08;
	int LCONST_0 = 0x09;
	int LCONST_1 = 0x0A;
	int FCONST_0 = 0x0B;
	int FCONST_1 = 0x0C;
	int FCONST_2 = 0x0D;
	int DCONST_0 = 0x0E;
	int DCONST_1 = 0x0F;
	int BIPUSH = 0x10;
	int SIPUSH = 0x11;
	int LDC = 0x12;
	int LDC_W = 0x13;
	int LDC2_W= 0x14;
	int ILOAD = 0x15;
	int LLOAD = 0x16;
	int FLOAD = 0x17;
	int DLOAD = 0x18;
	int ALOAD = 0x19;
	int ILOAD_0 = 0x1A;
	int ILOAD_1 = 0x1B;
	int ILOAD_2 = 0x1C;
	int ILOAD_3 = 0x1D;
	int LLOAD_0 = 0x1E;
	int LLOAD_1 = 0x1F;
	int LLOAD_2 = 0x20;
	int LLOAD_3 = 0x21;
	int FLOAD_0 = 0x22;
	int FLOAD_1 = 0x23;
	int FLOAD_2 = 0x24;
	int FLOAD_3 = 0x25;
	int DLOAD_0 = 0x26;
	int DLOAD_1 = 0x27;
	int DLOAD_2 = 0x28;
	int DLOAD_3 = 0x29;
	int ALOAD_0 = 0x2A;
	int ALOAD_1 = 0x2B;
	int ALOAD_2 = 0x2C;
	int ALOAD_3 = 0x2D;
	int IALOAD = 0x2E;
	int LALOAD = 0x2F;
	int FALOAD = 0x30;
	int DALOAD = 0x31;
	int AALOAD = 0x32;
	int BALOAD = 0x33;
	int CALOAD = 0x34;
	int SALOAD = 0x35;
	int ISTORE = 0x36;
	int LSTORE = 0x37;
	int FSTORE = 0x38;
	int DSTORE = 0x39;
	int ASTORE = 0x3A;
	int ISTORE_0 = 0x3B;
	int ISTORE_1 = 0x3C;
	int ISTORE_2 = 0x3D;
	int ISTORE_3 = 0x3E;
	int LSTORE_0 = 0x3F;
	int LSTORE_1 = 0x40;
	int LSTORE_2 = 0x41;
	int LSTORE_3 = 0x42;
	int FSTORE_0 = 0x43;
	int FSTORE_1 = 0x44;
	int FSTORE_2 = 0x45;
	int FSTORE_3 = 0x46;
	int DSTORE_0 = 0x47;
	int DSTORE_1 = 0x48;
	int DSTORE_2 = 0x49;
	int DSTORE_3 = 0x4A;
	int ASTORE_0 = 0x4B;
	int ASTORE_1 = 0x4C;
	int ASTORE_2 = 0x4D;
	int ASTORE_3 = 0x4E;
	int IASTORE = 0x4F;
	int LASTORE = 0x50;
	int FASTORE = 0x51;
	int DASTORE = 0x52;
	int AASTORE = 0x53;
	int BASTORE = 0x54;
	int CASTORE = 0x55;
	int SASTORE = 0x56;
	int POP = 0x57;
	int POP2 = 0x58;
	int DUP = 0x59;
	int DUP_X1 = 0x5A;
	int DUP_X2 = 0x5B;
	int DUP2 = 0x5C;
	int DUP2_X1 = 0x5D;
	int DUP2_X2 = 0x5E;
	int SWAP = 0x5F;
	int IADD = 0x60;
	int LADD = 0x61;
	int FADD = 0x62;
	int DADD = 0x63;
	int ISUB = 0x64;
	int LSUB = 0x65;
	int FSUB = 0x66;
	int DSUB = 0x67;
	int IMUL = 0x68;
	int LMUL = 0x69;
	int FMUL = 0x6A;
	int DMUL = 0x6B;
	int IDIV = 0x6C;
	int LDIV = 0x6D;
	int FDIV = 0x6E;
	int DDIV = 0x6F;
	int IREM = 0x70;
	int LREM = 0x71;
	int FREM = 0x72;
	int DREM = 0x73;
	int INEG = 0x74;
	int LNEG = 0x75;
	int FNEG = 0x76;
	int DNEG = 0x77;
	int ISHL = 0x78;
	int LSHL = 0x79;
	int ISHR = 0x7A;
	int LSHR = 0x7B;
	int IUSHR = 0x7C;
	int LUSHR = 0x7D;
	int IAND = 0x7E;
	int LAND = 0x7F;
	int IOR = 0x80;
	int LOR = 0x81;
	int IXOR = 0x82;
	int LXOR = 0x83;
	int IINC = 0x84;
	int I2L = 0x85;
	int I2F = 0x86;
	int I2D = 0x87;
	int L2I = 0x88;
	int L2F = 0x89;
	int L2D = 0x8A;
	int F2I = 0x8B;
	int F2L = 0x8C;
	int F2D = 0x8D;
	int D2I = 0x8E;
	int D2L = 0x8F;
	int D2F = 0x90;
	int I2B = 0x91;
	int I2C = 0x92;
	int I2S = 0x93;
	int LCMP = 0x94;
	int FCMPL = 0x95;
	int FCMPG = 0x96;
	int DCMPL = 0x97;
	int DCMPG = 0x98;
	int IFEQ = 0x99;
	int IFNE = 0x9A;
	int IFLT = 0x9B;
	int IFGE = 0x9C;
	int IFGT = 0x9D;
	int IFLE = 0x9E;
	int IF_ICMPEQ = 0x9F;
	int IF_ICMPNE = 0xA0;
	int IF_ICMPLT = 0xA1;
	int IF_ICMPGE = 0xA2;
	int IF_ICMPGT = 0xA3;
	int IF_ICMPLE = 0xA4;
	int IF_ACMPEQ = 0xA5;
	int IF_ACMPNE = 0xA6;
	int GOTO = 0xA7;
	int JSR = 0xA8;
	int RET = 0xA9;
	int TABLESWITCH = 0xAA;
	int LOOKUPSWITCH = 0xAB;
	int IRETURN = 0xAC;
	int LRETURN = 0xAD;
	int FRETURN = 0xAE;
	int DRETURN = 0xAF;
	int ARETURN = 0xB0;
	int RETURN = 0xB1;
	int GETSTATIC = 0xB2;
	int PUTSTATIC = 0xB3;
	int GETFIELD = 0xB4;
	int PUTFIELD = 0xB5;
	int INVOKEVIRTUAL = 0xB6;
	int INVOKESPECIAL = 0xB7;
	int INVOKESTATIC = 0xB8;
	int INVOKEINTERFACE = 0xB9;
	/**
	 * @since 3.6
	 */
	int INVOKEDYNAMIC = 0xBA;
	int NEW = 0xBB;
	int NEWARRAY = 0xBC;
	int ANEWARRAY = 0xBD;
	int ARRAYLENGTH = 0xBE;
	int ATHROW = 0xBF;
	int CHECKCAST = 0xC0;
	int INSTANCEOF = 0xC1;
	int MONITORENTER = 0xC2;
	int MONITOREXIT = 0xC3;
	int WIDE = 0xC4;
	int MULTIANEWARRAY = 0xC5;
	int IFNULL = 0xC6;
	int IFNONNULL = 0xC7;
	int GOTO_W = 0xC8;
	int JSR_W = 0xC9;

	int BREAKPOINT = 0xCA;
	int IMPDEP1 = 0xFE;
	int IMPDEP2 = 0xFF;
}
