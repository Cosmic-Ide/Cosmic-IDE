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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IStackMapFrame;
import org.eclipse.jdt.core.util.IVerificationTypeInfo;

/**
 * Default implementation of IStackMapFrame
 */
public class StackMapFrame extends ClassFileStruct implements IStackMapFrame {
	private static final IVerificationTypeInfo[] EMPTY_LOCALS_OR_STACK_ITEMS = new IVerificationTypeInfo[0];

	private int readOffset;
	private int frameType;
	private int numberOfLocals;
	private int numberOfStackItems;
	private IVerificationTypeInfo[] locals;
	private IVerificationTypeInfo[] stackItems;
	private int offsetDelta;

	/**
	 * Constructor for StackMapFrame.
	 *
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public StackMapFrame(
			byte[] classFileBytes,
			IConstantPool constantPool,
			int offset) throws ClassFormatException {

		final int type = u1At(classFileBytes, 0, offset);
		this.frameType = type;
		switch(type) {
			case 247 : // SAME_LOCALS_1_STACK_ITEM_EXTENDED
				this.offsetDelta = u2At(classFileBytes, 1, offset);
				this.numberOfStackItems = 1;
				this.stackItems = new VerificationInfo[1];
				this.readOffset = 3;
				VerificationInfo info = new VerificationInfo(classFileBytes, constantPool, offset + this.readOffset);
				this.stackItems[0] = info;
				this.readOffset += info.sizeInBytes();
				this.locals = EMPTY_LOCALS_OR_STACK_ITEMS;
				this.numberOfLocals = 0;
				break;
			case 248 :
			case 249 :
			case 250:
				// CHOP
				this.offsetDelta = u2At(classFileBytes, 1, offset);
				this.numberOfStackItems = 0;
				this.stackItems = EMPTY_LOCALS_OR_STACK_ITEMS;
				this.readOffset = 3;
				this.locals = EMPTY_LOCALS_OR_STACK_ITEMS;
				this.numberOfLocals = 0;
				break;
			case 251 :
				// SAME_FRAME_EXTENDED
				this.offsetDelta = u2At(classFileBytes, 1, offset);
				this.numberOfStackItems = 0;
				this.stackItems = EMPTY_LOCALS_OR_STACK_ITEMS;
				this.readOffset = 3;
				this.locals = EMPTY_LOCALS_OR_STACK_ITEMS;
				this.numberOfLocals = 0;
				break;
			case 252 :
			case 253 :
			case 254 :
				// APPEND
				this.offsetDelta = u2At(classFileBytes, 1, offset);
				this.numberOfStackItems = 0;
				this.stackItems = EMPTY_LOCALS_OR_STACK_ITEMS;
				this.readOffset = 3;
				int diffLocals = type - 251;
				this.numberOfLocals = diffLocals;
				this.locals = new IVerificationTypeInfo[diffLocals];
				for (int i = 0; i < diffLocals; i++) {
					VerificationInfo verificationInfo = new VerificationInfo(classFileBytes, constantPool, offset + this.readOffset);
					this.locals[i] = verificationInfo;
					this.readOffset += verificationInfo.sizeInBytes();
				}
				break;
			case 255 :
				// FULL_FRAME
				this.offsetDelta = u2At(classFileBytes, 1, offset);
				int tempLocals = u2At(classFileBytes, 3, offset);
				this.numberOfLocals = tempLocals;
				this.readOffset = 5;
				if (tempLocals != 0) {
					this.locals = new IVerificationTypeInfo[tempLocals];
					for (int i = 0; i < tempLocals; i++) {
						VerificationInfo verificationInfo = new VerificationInfo(classFileBytes, constantPool, offset + this.readOffset);
						this.locals[i] = verificationInfo;
						this.readOffset += verificationInfo.sizeInBytes();
					}
				} else {
					this.locals = EMPTY_LOCALS_OR_STACK_ITEMS;
				}
				int tempStackItems = u2At(classFileBytes, this.readOffset, offset);
				this.readOffset += 2;
				this.numberOfStackItems = tempStackItems;
				if (tempStackItems != 0) {
					this.stackItems = new IVerificationTypeInfo[tempStackItems];
					for (int i = 0; i < tempStackItems; i++) {
						VerificationInfo verificationInfo = new VerificationInfo(classFileBytes, constantPool, offset + this.readOffset);
						this.stackItems[i] = verificationInfo;
						this.readOffset += verificationInfo.sizeInBytes();
					}
				} else {
					this.stackItems = EMPTY_LOCALS_OR_STACK_ITEMS;
				}
				break;
			default:
				if (type <= 63) {
					// SAME_FRAME
					this.offsetDelta = type;
					this.numberOfStackItems = 0;
					this.stackItems = EMPTY_LOCALS_OR_STACK_ITEMS;
					this.locals = EMPTY_LOCALS_OR_STACK_ITEMS;
					this.numberOfLocals = 0;
					this.readOffset = 1;
				} else if (type <= 127) {
					// SAME_LOCALS_1_STACK_ITEM
					this.offsetDelta = type - 64;
					this.numberOfStackItems = 1;
					this.stackItems = new VerificationInfo[1];
					this.readOffset = 1;
					info = new VerificationInfo(classFileBytes, constantPool, offset + this.readOffset);
					this.stackItems[0] = info;
					this.readOffset += info.sizeInBytes();
					this.locals = EMPTY_LOCALS_OR_STACK_ITEMS;
					this.numberOfLocals = 0;
				}
		}
	}
	int sizeInBytes() {
		return this.readOffset;
	}
	@Override
	public int getFrameType() {
		return this.frameType;
	}
	@Override
	public IVerificationTypeInfo[] getLocals() {
		return this.locals;
	}
	@Override
	public int getNumberOfLocals() {
		return this.numberOfLocals;
	}
	@Override
	public int getNumberOfStackItems() {
		return this.numberOfStackItems;
	}
	@Override
	public int getOffsetDelta() {
		return this.offsetDelta;
	}
	@Override
	public IVerificationTypeInfo[] getStackItems() {
		return this.stackItems;
	}
}
