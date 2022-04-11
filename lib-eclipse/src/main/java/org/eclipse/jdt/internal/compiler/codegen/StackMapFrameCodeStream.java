/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.problem.AbortMethod;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class StackMapFrameCodeStream extends CodeStream {
	public static class ExceptionMarker implements Comparable {
		private TypeBinding binding;
		public int pc;

		public ExceptionMarker(int pc, TypeBinding typeBinding) {
			this.pc = pc;
			this.binding = typeBinding;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof ExceptionMarker) {
				return this.pc - ((ExceptionMarker) o).pc;
			}
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ExceptionMarker) {
				ExceptionMarker marker = (ExceptionMarker) obj;
				return this.pc == marker.pc && this.binding.equals(marker.binding);
			}
			return false;
		}

		public TypeBinding getBinding() {
			return this.binding;
		}

		@Override
		public int hashCode() {
			return this.pc + CharOperation.hashCode(this.binding.constantPoolName());
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append('(').append(this.pc).append(',').append(this.binding.constantPoolName()).append(')');
			return String.valueOf(buffer);
		}
	}

	static class FramePosition {
		int counter;
	}

	public int[] stateIndexes;
	public int stateIndexesCounter;
	private HashMap framePositions;
	public Set exceptionMarkers;
	public ArrayList stackDepthMarkers;
	public ArrayList stackMarkers;

	public StackMapFrameCodeStream(ClassFile givenClassFile) {
		super(givenClassFile);
		this.generateAttributes |= ClassFileConstants.ATTR_STACK_MAP;
	}

	@Override
	public void addDefinitelyAssignedVariables(Scope scope, int initStateIndex) {
		// Required to fix 1PR0XVS: LFRE:WINNT - Compiler: variable table for method appears incorrect
		for (int i = 0; i < this.visibleLocalsCount; i++) {
			LocalVariableBinding localBinding = this.visibleLocals[i];
			if (localBinding != null) {
				// Check if the local is definitely assigned
				boolean isDefinitelyAssigned = isDefinitelyAssigned(scope, initStateIndex, localBinding);
				if (!isDefinitelyAssigned) {
					continue;
				} else {
					if ((localBinding.initializationCount == 0)
							|| (localBinding.initializationPCs[((localBinding.initializationCount - 1) << 1)
									+ 1] != -1)) {
						/*
						 * There are two cases: 1) there is no initialization interval opened ==> add an opened interval
						 * 2) there is already some initialization intervals but the last one is closed ==> add an
						 * opened interval An opened interval means that the value at
						 * localBinding.initializationPCs[localBinding.initializationCount - 1][1] is equals to -1.
						 * initializationPCs is a collection of pairs of int: first value is the startPC and second
						 * value is the endPC. -1 one for the last value means that the interval is not closed yet.
						 */
						localBinding.recordInitializationStartPC(this.position);
					}
				}
			}
		}
	}

	public void addExceptionMarker(int pc, TypeBinding typeBinding) {
		if (this.exceptionMarkers == null) {
			this.exceptionMarkers = new HashSet();
		}

		this.exceptionMarkers.add(new ExceptionMarker(pc, typeBinding));
	}

	public void addFramePosition(int pc) {
		Integer newEntry = Integer.valueOf(pc);
		FramePosition value;
		if ((value = (FramePosition) this.framePositions.get(newEntry)) != null) {
			value.counter++;
		} else {
			this.framePositions.put(newEntry, new FramePosition());
		}
	}

	@Override
	public void optimizeBranch(int oldPosition, BranchLabel lbl) {
		super.optimizeBranch(oldPosition, lbl);
		removeFramePosition(oldPosition);
	}

	public void removeFramePosition(int pc) {
		Integer entry = Integer.valueOf(pc);
		FramePosition value;
		if ((value = (FramePosition) this.framePositions.get(entry)) != null) {
			value.counter--;
			if (value.counter <= 0) {
				this.framePositions.remove(entry);
			}
		}
	}

	@Override
	public void addVariable(LocalVariableBinding localBinding) {
		if (localBinding.initializationPCs == null) {
			record(localBinding);
		}
		localBinding.recordInitializationStartPC(this.position);
	}

	@Override
	public void recordExpressionType(TypeBinding typeBinding, int delta, boolean adjustStackDepth) {
		if (adjustStackDepth) {
			// optimized goto
			// the break label already adjusted the stack depth (-1 or -2 depending on the return type)
			// we need to adjust back to what it was
			switch (typeBinding.id) {
				case TypeIds.T_long:
				case TypeIds.T_double:
					this.stackDepth += 2;
					break;
				case TypeIds.T_void:
					break;
				default:
					this.stackDepth++;
					break;
			}
		}
	}

	/**
	 * Macro for building a class descriptor object
	 */
	@Override
	public void generateClassLiteralAccessForType(Scope scope, TypeBinding accessedType,
			FieldBinding syntheticFieldBinding) {
		if (accessedType.isBaseType() && accessedType != TypeBinding.NULL) {
			getTYPE(accessedType.id);
			return;
		}

		if (this.targetLevel >= ClassFileConstants.JDK1_5) {
			// generation using the new ldc_w bytecode
			this.ldc(accessedType);
		} else {
			// use in CLDC mode
			BranchLabel endLabel = new BranchLabel(this);
			if (syntheticFieldBinding != null) { // non interface case
				fieldAccess(Opcodes.OPC_getstatic, syntheticFieldBinding, null /* default declaringClass */);
				dup();
				ifnonnull(endLabel);
				pop();
			}

			/*
			 * Macro for building a class descriptor object... using or not a field cache to store it into... this
			 * sequence is responsible for building the actual class descriptor.
			 *
			 * If the fieldCache is set, then it is supposed to be the body of a synthetic access method factoring the
			 * actual descriptor creation out of the invocation site (saving space). If the fieldCache is nil, then we
			 * are dumping the bytecode on the invocation site, since we have no way to get a hand on the field cache to
			 * do better.
			 */

			// Wrap the code in an exception handler to convert a ClassNotFoundException into a NoClassDefError

			ExceptionLabel classNotFoundExceptionHandler = new ExceptionLabel(this,
					TypeBinding.NULL /* represents ClassNotFoundException */);
			classNotFoundExceptionHandler.placeStart();
			this.ldc(accessedType == TypeBinding.NULL ? "java.lang.Object" //$NON-NLS-1$
					: String.valueOf(accessedType.constantPoolName()).replace('/', '.'));
			invokeClassForName();

			/*
			 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=37565 if (accessedType == BaseTypes.NullBinding) {
			 * this.ldc("java.lang.Object"); //$NON-NLS-1$ } else if (accessedType.isArrayType()) {
			 * this.ldc(String.valueOf(accessedType.constantPoolName()).replace('/', '.')); } else { // we make it an
			 * array type (to avoid class initialization) this.ldc("[L" +
			 * String.valueOf(accessedType.constantPoolName()).replace('/', '.') + ";"); //$NON-NLS-1$//$NON-NLS-2$ }
			 * this.invokeClassForName(); if (!accessedType.isArrayType()) { // extract the component type, which
			 * doesn't initialize the class this.invokeJavaLangClassGetComponentType(); }
			 */
			/*
			 * We need to protect the runtime code from binary inconsistencies in case the accessedType is missing, the
			 * ClassNotFoundException has to be converted into a NoClassDefError(old ex message), we thus need to build
			 * an exception handler for this one.
			 */
			classNotFoundExceptionHandler.placeEnd();

			if (syntheticFieldBinding != null) { // non interface case
				dup();
				fieldAccess(Opcodes.OPC_putstatic, syntheticFieldBinding, null /* default declaringClass */);
			}
			goto_(endLabel);
			int savedStackDepth = this.stackDepth;
			// Generate the body of the exception handler
			/*
			 * ClassNotFoundException on stack -- the class literal could be doing more things on the stack, which means
			 * that the stack may not be empty at this point in the above code gen. So we save its state and restart it
			 * from 1.
			 */

			pushExceptionOnStack(scope.getJavaLangClassNotFoundException());
			classNotFoundExceptionHandler.place();

			// Transform the current exception, and repush and throw a
			// NoClassDefFoundError(ClassNotFound.getMessage())

			newNoClassDefFoundError();
			dup_x1();
			swap();

			// Retrieve the message from the old exception
			invokeThrowableGetMessage();

			// Send the constructor taking a message string as an argument
			invokeNoClassDefFoundErrorStringConstructor();
			athrow();
			endLabel.place();
			this.stackDepth = savedStackDepth;
		}
	}

	@Override
	public void generateOuterAccess(Object[] mappingSequence, ASTNode invocationSite, Binding target, Scope scope) {
		int currentPosition = this.position;
		super.generateOuterAccess(mappingSequence, invocationSite, target, scope);
		if (currentPosition == this.position) {
			// no code has been generate during outer access => no enclosing instance is available
			throw new AbortMethod(scope.referenceCompilationUnit().compilationResult, null);
		}
	}

	public ExceptionMarker[] getExceptionMarkers() {
		Set exceptionMarkerSet = this.exceptionMarkers;
		if (this.exceptionMarkers == null)
			return null;
		int size = exceptionMarkerSet.size();
		ExceptionMarker[] markers = new ExceptionMarker[size];
		int n = 0;
		for (Iterator iterator = exceptionMarkerSet.iterator(); iterator.hasNext();) {
			markers[n++] = (ExceptionMarker) iterator.next();
		}
		Arrays.sort(markers);
//  System.out.print('[');
//  for (int n = 0; n < size; n++) {
//  	if (n != 0) System.out.print(',');
//  	System.out.print(positions[n]);
//  }
//  System.out.println(']');
		return markers;
	}

	public int[] getFramePositions() {
		Set set = this.framePositions.keySet();
		int size = set.size();
		int[] positions = new int[size];
		int n = 0;
		for (Iterator iterator = set.iterator(); iterator.hasNext();) {
			positions[n++] = ((Integer) iterator.next()).intValue();
		}
		Arrays.sort(positions);
//  System.out.print('[');
//  for (int n = 0; n < size; n++) {
//  	if (n != 0) System.out.print(',');
//  	System.out.print(positions[n]);
//  }
//  System.out.println(']');
		return positions;
	}

	public boolean hasFramePositions() {
		return this.framePositions.size() != 0;
	}

	@Override
	public void init(ClassFile targetClassFile) {
		super.init(targetClassFile);
		this.stateIndexesCounter = 0;
		if (this.framePositions != null) {
			this.framePositions.clear();
		}
		if (this.exceptionMarkers != null) {
			this.exceptionMarkers.clear();
		}
		if (this.stackDepthMarkers != null) {
			this.stackDepthMarkers.clear();
		}
		if (this.stackMarkers != null) {
			this.stackMarkers.clear();
		}
	}

	@Override
	public void initializeMaxLocals(MethodBinding methodBinding) {
		super.initializeMaxLocals(methodBinding);
		if (this.framePositions == null) {
			this.framePositions = new HashMap();
		} else {
			this.framePositions.clear();
		}
	}

	public void popStateIndex() {
		this.stateIndexesCounter--;
	}

	public void pushStateIndex(int naturalExitMergeInitStateIndex) {
		if (this.stateIndexes == null) {
			this.stateIndexes = new int[3];
		}
		int length = this.stateIndexes.length;
		if (length == this.stateIndexesCounter) {
			// resize
			System.arraycopy(this.stateIndexes, 0, (this.stateIndexes = new int[length * 2]), 0, length);
		}
		this.stateIndexes[this.stateIndexesCounter++] = naturalExitMergeInitStateIndex;
	}

	@Override
	public void removeNotDefinitelyAssignedVariables(Scope scope, int initStateIndex) {
		int index = this.visibleLocalsCount;
		loop: for (int i = 0; i < index; i++) {
			LocalVariableBinding localBinding = this.visibleLocals[i];
			if (localBinding != null && localBinding.initializationCount > 0) {
				boolean isDefinitelyAssigned = isDefinitelyAssigned(scope, initStateIndex, localBinding);
				if (!isDefinitelyAssigned) {
					if (this.stateIndexes != null) {
						for (int j = 0, max = this.stateIndexesCounter; j < max; j++) {
							if (isDefinitelyAssigned(scope, this.stateIndexes[j], localBinding)) {
								continue loop;
							}
						}
					}
					localBinding.recordInitializationEndPC(this.position);
				}
			}
		}
	}

	@Override
	public void reset(ClassFile givenClassFile) {
		super.reset(givenClassFile);
		this.stateIndexesCounter = 0;
		if (this.framePositions != null) {
			this.framePositions.clear();
		}
		if (this.exceptionMarkers != null) {
			this.exceptionMarkers.clear();
		}
		if (this.stackDepthMarkers != null) {
			this.stackDepthMarkers.clear();
		}
		if (this.stackMarkers != null) {
			this.stackMarkers.clear();
		}
	}

	@Override
	protected void writePosition(BranchLabel label) {
		super.writePosition(label);
		addFramePosition(label.position);
	}

	@Override
	protected void writePosition(BranchLabel label, int forwardReference) {
		super.writePosition(label, forwardReference);
		addFramePosition(label.position);
	}

	@Override
	protected void writeSignedWord(int pos, int value) {
		super.writeSignedWord(pos, value);
		addFramePosition(this.position);
	}

	@Override
	protected void writeWidePosition(BranchLabel label) {
		super.writeWidePosition(label);
		addFramePosition(label.position);
	}

	@Override
	public void areturn() {
		super.areturn();
		addFramePosition(this.position);
	}

	@Override
	public void ireturn() {
		super.ireturn();
		addFramePosition(this.position);
	}

	@Override
	public void lreturn() {
		super.lreturn();
		addFramePosition(this.position);
	}

	@Override
	public void freturn() {
		super.freturn();
		addFramePosition(this.position);
	}

	@Override
	public void dreturn() {
		super.dreturn();
		addFramePosition(this.position);
	}

	@Override
	public void return_() {
		super.return_();
		addFramePosition(this.position);
	}

	@Override
	public void athrow() {
		super.athrow();
		addFramePosition(this.position);
	}

	@Override
	public void pushExceptionOnStack(TypeBinding binding) {
		super.pushExceptionOnStack(binding);
		addExceptionMarker(this.position, binding);
	}

	@Override
	public void goto_(BranchLabel label) {
		super.goto_(label);
		addFramePosition(this.position);
	}

	@Override
	public void goto_w(BranchLabel label) {
		super.goto_w(label);
		addFramePosition(this.position);
	}

	@Override
	public void resetInWideMode() {
		this.resetSecretLocals();
		super.resetInWideMode();
	}

	@Override
	public void resetForCodeGenUnusedLocals() {
		this.resetSecretLocals();
		super.resetForCodeGenUnusedLocals();
	}

	public void resetSecretLocals() {
		for (int i = 0, max = this.locals.length; i < max; i++) {
			LocalVariableBinding localVariableBinding = this.locals[i];
			if (localVariableBinding != null && localVariableBinding.isSecret()) {
				// all other locals are reinitialized inside the computation of their resolved positions
				localVariableBinding.resetInitializations();
			}
		}
	}
}
