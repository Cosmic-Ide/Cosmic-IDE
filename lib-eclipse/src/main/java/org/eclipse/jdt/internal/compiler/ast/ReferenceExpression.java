/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Jesper S Moller - Contributions for
 *							bug 382701 - [1.8][compiler] Implement semantic analysis of Lambda expressions & Reference expression
 *                          Bug 384687 - [1.8] Wildcard type arguments should be rejected for lambda and reference expressions
 *							Bug 416885 - [1.8][compiler]IncompatibleClassChange error (edit)
 *	   Stephan Herrmann - Contribution for
 *							bug 402028 - [1.8][compiler] null analysis for reference expressions
 *							bug 404649 - [1.8][compiler] detect illegal reference to indirect or redundant super via I.super.m() syntax
 *							Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *							Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *							Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *							Bug 423504 - [1.8] Implement "18.5.3 Functional Interface Parameterization Inference"
 *							Bug 424637 - [1.8][compiler][null] AIOOB in ReferenceExpression.resolveType with a method reference to Files::walk
 *							Bug 424415 - [1.8][compiler] Eventual resolution of ReferenceExpression is not seen to be happening.
 *							Bug 424403 - [1.8][compiler] Generic method call with method reference argument fails to resolve properly.
 *							Bug 427196 - [1.8][compiler] Compiler error for method reference to overloaded method
 *							Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *							Bug 428264 - [1.8] method reference of generic class causes problems (wrong inference result or NPE)
 *							Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *							Bug 426537 - [1.8][inference] Eclipse compiler thinks I<? super J> is compatible with I<J<?>> - raw type J involved
 *							Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *							Bug 435689 - [1.8][inference] Type inference not occurring with lambda expression and method reference
 *							Bug 438383 - [1.8][null] Bogus warning: Null type safety at method return type
 *							Bug 434483 - [1.8][compiler][inference] Type inference not picked up with method reference
 *							Bug 441734 - [1.8][inference] Generic method with nested parameterized type argument fails on method reference
 *							Bug 438945 - [1.8] NullPointerException InferenceContext18.checkExpression in java 8 with generics, primitives, and overloading
 *							Bug 452788 - [1.8][compiler] Type not correctly inferred in lambda expression
 *							Bug 448709 - [1.8][null] ensure we don't infer types that violate null constraints on a type parameter's bound
 *							Bug 459967 - [null] compiler should know about nullness of special methods like MyEnum.valueOf()
 *							Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *							Bug 470542 - NullPointerException in ReferenceExpression.isPotentiallyCompatibleWith (962)
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contribution for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/

package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.INVOCATION_CONTEXT;

import java.util.HashMap;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationPosition;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.ConstantPool;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.flow.FieldInitsFakingFlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.IrritantSet;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.ImplicitNullAnnotationVerifier;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedGenericMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.PolyTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.Scanner;

public class ReferenceExpression extends FunctionalExpression implements IPolyExpression, InvocationSite {
	// secret variable name
	private static final String SecretReceiverVariableName = " rec_"; //$NON-NLS-1$
	private static final char[] ImplicitArgName = " arg".toCharArray(); //$NON-NLS-1$
	// secret variable for codegen
	public LocalVariableBinding receiverVariable;
	public Expression lhs;
	public TypeReference [] typeArguments;
	public char [] selector;

	public int nameSourceStart;

	public TypeBinding receiverType;
	public boolean haveReceiver;
	public TypeBinding[] resolvedTypeArguments;
	private boolean typeArgumentsHaveErrors;

	MethodBinding syntheticAccessor;	// synthetic accessor for inner-emulation
	private int depth;
	private MethodBinding exactMethodBinding; // != null ==> exact method reference.
	private boolean receiverPrecedesParameters = false;
	private TypeBinding[] freeParameters; // descriptor parameters as used for method lookup - may or may not include the receiver
	private boolean checkingPotentialCompatibility;
	private MethodBinding[] potentialMethods = Binding.NO_METHODS;
	protected ReferenceExpression original;
	private HashMap<TypeBinding, ReferenceExpression> copiesPerTargetType;
	public char[] text; // source representation of the expression.
	private HashMap<ParameterizedGenericMethodBinding, InferenceContext18> inferenceContexts;

	// the scanner used when creating this expression, may be a RecoveryScanner (with proper RecoveryScannerData),
	// need to keep it so copy() can parse in the same mode (normal/recovery):
	private Scanner scanner;

	public ReferenceExpression(Scanner scanner) {
		super();
		this.original = this;
		this.scanner = scanner;
	}

	public void initialize(CompilationResult result, Expression expression, TypeReference [] optionalTypeArguments, char [] identifierOrNew, int sourceEndPosition) {
		super.setCompilationResult(result);
		this.lhs = expression;
		this.typeArguments = optionalTypeArguments;
		this.selector = identifierOrNew;
		this.sourceStart = expression.sourceStart;
		this.sourceEnd = sourceEndPosition;
	}

	private ReferenceExpression copy() {
		final Parser parser = new Parser(this.enclosingScope.problemReporter(), false);
		final ICompilationUnit compilationUnit = this.compilationResult.getCompilationUnit();
		final char[] source = compilationUnit != null ? compilationUnit.getContents() : this.text;
		parser.scanner = this.scanner;
		ReferenceExpression copy =  (ReferenceExpression) parser.parseExpression(source, compilationUnit != null ? this.sourceStart : 0, this.sourceEnd - this.sourceStart + 1,
										this.enclosingScope.referenceCompilationUnit(), false /* record line separators */);
		copy.original = this;
		copy.sourceStart = this.sourceStart;
		copy.sourceEnd = this.sourceEnd;
		return copy;
	}

	private boolean shouldGenerateSecretReceiverVariable() {
		if (isMethodReference() && this.haveReceiver) {
			if (this.lhs instanceof Invocation)
				return true;
			else {
				return new ASTVisitor() {
					boolean accessesnonFinalOuterLocals;

					@Override
					public boolean visit(SingleNameReference name, BlockScope skope) {
						Binding local = skope.getBinding(name.getName(), ReferenceExpression.this);
						if (local instanceof LocalVariableBinding) {
							LocalVariableBinding localBinding = (LocalVariableBinding) local;
							if (!localBinding.isFinal() && !localBinding.isEffectivelyFinal()) {
								this.accessesnonFinalOuterLocals = true;
							}
						}
						return false;
					}

					public boolean accessesnonFinalOuterLocals() {
						ReferenceExpression.this.lhs.traverse(this, ReferenceExpression.this.enclosingScope);
						return this.accessesnonFinalOuterLocals;
					}
				}.accessesnonFinalOuterLocals();
			}
		}
		return false;
	}
	public void generateImplicitLambda(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {

		ReferenceExpression copy = copy();

		int argc = this.descriptor.parameters.length;

		LambdaExpression implicitLambda = new LambdaExpression(this.compilationResult, false, (this.binding.modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0);
		Argument [] arguments = new Argument[argc];
		for (int i = 0; i < argc; i++)
			arguments[i] = new Argument(CharOperation.append(ImplicitArgName, Integer.toString(i).toCharArray()), 0, null, 0, true);
		implicitLambda.setArguments(arguments);
		implicitLambda.setExpressionContext(this.expressionContext);
		implicitLambda.setExpectedType(this.expectedType);

		int parameterShift = this.receiverPrecedesParameters ? 1 : 0;
		Expression [] argv = new SingleNameReference[argc - parameterShift];
		for (int i = 0, length = argv.length; i < length; i++) {
			char[] name = CharOperation.append(ImplicitArgName, Integer.toString((i + parameterShift)).toCharArray());
			argv[i] = new SingleNameReference(name, 0);
		}
		boolean generateSecretReceiverVariable = shouldGenerateSecretReceiverVariable();
		if (isMethodReference()) {
			if (generateSecretReceiverVariable) {
				this.lhs.generateCode(currentScope, codeStream, true);
				codeStream.store(this.receiverVariable, false);
				codeStream.addVariable(this.receiverVariable);
			}
			MessageSend message = new MessageSend();
			message.selector = this.selector;
			Expression receiver = generateSecretReceiverVariable ? new SingleNameReference(this.receiverVariable.name, 0) : copy.lhs;
			message.receiver = this.receiverPrecedesParameters ?
					new SingleNameReference(CharOperation.append(ImplicitArgName, Integer.toString(0).toCharArray()), 0) : receiver;
			message.typeArguments = copy.typeArguments;
			message.arguments = argv;
			implicitLambda.setBody(message);
		} else if (isArrayConstructorReference()) {
			// We don't care for annotations, source positions etc. They are immaterial, just drop.
			ArrayAllocationExpression arrayAllocationExpression = new ArrayAllocationExpression();
			arrayAllocationExpression.dimensions = new Expression[] { argv[0] };
			if (this.lhs instanceof ArrayTypeReference) {
				ArrayTypeReference arrayTypeReference = (ArrayTypeReference) this.lhs;
				arrayAllocationExpression.type = arrayTypeReference.dimensions == 1 ? new SingleTypeReference(arrayTypeReference.token, 0L) :
																new ArrayTypeReference(arrayTypeReference.token, arrayTypeReference.dimensions - 1, 0L);
			} else {
				ArrayQualifiedTypeReference arrayQualifiedTypeReference = (ArrayQualifiedTypeReference) this.lhs;
				arrayAllocationExpression.type = arrayQualifiedTypeReference.dimensions == 1 ? new QualifiedTypeReference(arrayQualifiedTypeReference.tokens, arrayQualifiedTypeReference.sourcePositions)
																: new ArrayQualifiedTypeReference(arrayQualifiedTypeReference.tokens, arrayQualifiedTypeReference.dimensions - 1,
																		arrayQualifiedTypeReference.sourcePositions);
			}
			implicitLambda.setBody(arrayAllocationExpression);
		} else {
			AllocationExpression allocation = new AllocationExpression();
			if (this.lhs instanceof TypeReference) {
				allocation.type = (TypeReference) this.lhs;
			} else if (this.lhs instanceof SingleNameReference) {
				allocation.type = new SingleTypeReference(((SingleNameReference) this.lhs).token, 0);
			} else if (this.lhs instanceof QualifiedNameReference) {
				allocation.type = new QualifiedTypeReference(((QualifiedNameReference) this.lhs).tokens, new long [((QualifiedNameReference) this.lhs).tokens.length]);
			} else {
				throw new IllegalStateException("Unexpected node type"); //$NON-NLS-1$
			}
			allocation.typeArguments = copy.typeArguments;
			allocation.arguments = argv;
			implicitLambda.setBody(allocation);
		}

		// Process the lambda, taking care not to double report diagnostics. Don't expect any from resolve, Any from code generation should surface, but not those from flow analysis.
		BlockScope lambdaScope = this.receiverVariable != null ? this.receiverVariable.declaringScope : currentScope;
		IErrorHandlingPolicy oldPolicy = lambdaScope.problemReporter().switchErrorHandlingPolicy(silentErrorHandlingPolicy);
		try {
			implicitLambda.resolveType(lambdaScope, true);
			implicitLambda.analyseCode(lambdaScope,
					new FieldInitsFakingFlowContext(null, this, Binding.NO_EXCEPTIONS, null, lambdaScope, FlowInfo.DEAD_END),
					UnconditionalFlowInfo.fakeInitializedFlowInfo(lambdaScope.outerMostMethodScope().analysisIndex, lambdaScope.referenceType().maxFieldCount));
		} finally {
			lambdaScope.problemReporter().switchErrorHandlingPolicy(oldPolicy);
		}
		SyntheticArgumentBinding[] outerLocals = this.receiverType.syntheticOuterLocalVariables();
		for (int i = 0, length = outerLocals == null ? 0 : outerLocals.length; i < length; i++)
			implicitLambda.addSyntheticArgument(outerLocals[i].actualOuterLocalVariable);

		implicitLambda.generateCode(lambdaScope, codeStream, valueRequired);
		if (generateSecretReceiverVariable) {
			codeStream.removeVariable(this.receiverVariable);
		}
	}

	private boolean shouldGenerateImplicitLambda(BlockScope currentScope) {
		// these cases are either too complicated, impossible to handle or result in significant code duplication
		return (this.binding.isVarargs() ||
				(isConstructorReference() && this.receiverType.syntheticOuterLocalVariables() != null && this.shouldCaptureInstance) ||
				this.requiresBridges() || // bridges.
				!isDirectCodeGenPossible());
		// To fix: We should opt for direct code generation wherever possible.
	}
	private boolean isDirectCodeGenPossible() {
		if (this.binding != null) {
			if (isMethodReference() && this.syntheticAccessor == null) {
				if (TypeBinding.notEquals(this.binding.declaringClass, this.lhs.resolvedType.erasure())) {
					// reference to a method declared by an inaccessible type accessed via a
					// subtype - normally a bridge method would be present to facilitate
					// this access, unless the method is final/static/default, in which case, direct access to
					// the method is not possible, an implicit lambda is needed
					if (!this.binding.declaringClass.canBeSeenBy(this.enclosingScope)) {
						if (this.binding.isDefaultMethod())
							return false; // workaround for bug in MethodHandle lookup, see https://bugs.openjdk.java.net/browse/JDK-8068253
						return !(this.binding.isFinal() || this.binding.isStatic());
					}
				}
			}
			TypeBinding[] descriptorParams = this.descriptor.parameters;
			TypeBinding[] origParams = this.binding.original().parameters;
			TypeBinding[] origDescParams = this.descriptor.original().parameters;
			int offset = this.receiverPrecedesParameters ? 1 : 0;
			for (int i = 0; i < descriptorParams.length - offset; i++) {
				TypeBinding descType = descriptorParams[i + offset];
				TypeBinding origDescType = origDescParams[i + offset];
				if (descType.isIntersectionType18() ||
						(descType.isTypeVariable() && ((TypeVariableBinding) descType).boundsCount() > 1)) {
					return CharOperation.equals(origDescType.signature(), origParams[i].signature());
				}
			}
		}
		return true;
	}
	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
		this.actualMethodBinding = this.binding; // grab before synthetics come into play.
		// Handle some special cases up front and transform them into implicit lambdas.
		if (shouldGenerateImplicitLambda(currentScope)) {
			generateImplicitLambda(currentScope, codeStream, valueRequired);
			return;
		}
		SourceTypeBinding sourceType = currentScope.enclosingSourceType();
		if (this.receiverType.isArrayType()) {
			char [] lambdaName = CharOperation.concat(TypeConstants.ANONYMOUS_METHOD, Integer.toString(this.ordinal).toCharArray());
			if (isConstructorReference()) {
				this.actualMethodBinding = this.binding = sourceType.addSyntheticArrayMethod((ArrayBinding) this.receiverType, SyntheticMethodBinding.ArrayConstructor, lambdaName);
			} else if (CharOperation.equals(this.selector, TypeConstants.CLONE)) {
				this.actualMethodBinding = this.binding = sourceType.addSyntheticArrayMethod((ArrayBinding) this.receiverType, SyntheticMethodBinding.ArrayClone, lambdaName);
			}
		} else if (this.syntheticAccessor != null) {
			if (this.lhs.isSuper() || isMethodReference())
				this.binding = this.syntheticAccessor;
		} else { // cf. MessageSend.generateCode()'s call to CodeStream.getConstantPoolDeclaringClass. We have extracted the relevant portions sans side effect here.
			if (this.binding != null && isMethodReference()) {
				if (TypeBinding.notEquals(this.binding.declaringClass, this.lhs.resolvedType.erasure())) {
					if (!this.binding.declaringClass.canBeSeenBy(currentScope)) {
						this.binding = new MethodBinding(this.binding.original(), (ReferenceBinding) this.lhs.resolvedType.erasure());
					}
				}
			}
		}
		int pc = codeStream.position;
		StringBuilder buffer = new StringBuilder();
		int argumentsSize = 0;
		buffer.append('(');
		if (this.haveReceiver) {
			this.lhs.generateCode(currentScope, codeStream, true);
			if (isMethodReference() && !this.lhs.isThis() && !this.lhs.isSuper()) {
				MethodBinding mb = currentScope.getJavaLangObject().getExactMethod(TypeConstants.GETCLASS,
						Binding.NO_PARAMETERS, currentScope.compilationUnitScope());
				codeStream.dup();
				codeStream.invoke(Opcodes.OPC_invokevirtual, mb, mb.declaringClass);
				codeStream.pop();
			}
			if (this.lhs.isSuper() && !this.actualMethodBinding.isPrivate()) {
				if (this.lhs instanceof QualifiedSuperReference) {
					QualifiedSuperReference qualifiedSuperReference = (QualifiedSuperReference) this.lhs;
					TypeReference qualification = qualifiedSuperReference.qualification;
					if (qualification.resolvedType.isInterface()) {
						buffer.append(sourceType.signature());
					} else {
						buffer.append(((QualifiedSuperReference) this.lhs).currentCompatibleType.signature());
					}
				} else {
					buffer.append(sourceType.signature());
				}
			} else {
				buffer.append(this.receiverType.signature());
			}
			argumentsSize = 1;
		} else {
			if (this.isConstructorReference()) {
				ReferenceBinding[] enclosingInstances = Binding.UNINITIALIZED_REFERENCE_TYPES;
				if (this.receiverType.isNestedType()) {
					ReferenceBinding nestedType = (ReferenceBinding) this.receiverType;
					if ((enclosingInstances = nestedType.syntheticEnclosingInstanceTypes()) != null) {
						int length = enclosingInstances.length;
						argumentsSize = length;
						for (int i = 0 ; i < length; i++) {
							ReferenceBinding syntheticArgumentType = enclosingInstances[i];
							buffer.append(syntheticArgumentType.signature());
							Object[] emulationPath = currentScope.getEmulationPath(
									syntheticArgumentType,
									false /* allow compatible match */,
									true /* disallow instance reference in explicit constructor call */);
							codeStream.generateOuterAccess(emulationPath, this, syntheticArgumentType, currentScope);
						}
					} else {
						enclosingInstances = Binding.NO_REFERENCE_TYPES;
					}
				}
				if (this.syntheticAccessor != null) {
					char [] lambdaName = CharOperation.concat(TypeConstants.ANONYMOUS_METHOD, Integer.toString(this.ordinal).toCharArray());
					this.binding = sourceType.addSyntheticFactoryMethod(this.binding, this.syntheticAccessor, enclosingInstances, lambdaName);
					this.syntheticAccessor = null; // add only once
				}
			}
		}
		buffer.append(')');
		buffer.append('L');
		if (this.resolvedType.isIntersectionType18()) {
			buffer.append(this.descriptor.declaringClass.constantPoolName());
		} else {
			buffer.append(this.resolvedType.constantPoolName());
		}
		buffer.append(';');
		if (this.isSerializable) {
			sourceType.addSyntheticMethod(this);
		}
		int invokeDynamicNumber = codeStream.classFile.recordBootstrapMethod(this);
		codeStream.invokeDynamic(invokeDynamicNumber, argumentsSize, 1, this.descriptor.selector, buffer.toString().toCharArray(),
				this.isConstructorReference(), (this.lhs instanceof TypeReference? (TypeReference) this.lhs : null), this.typeArguments,
				this.resolvedType.id, this.resolvedType);
		if (!valueRequired)
			codeStream.pop();
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	@Override
	public void cleanUp() {
		// no more rescanning needed beyond this point, so free the memory:
		if (this.copiesPerTargetType != null) {
			for (ReferenceExpression copy : this.copiesPerTargetType.values())
				copy.scanner = null;
		}
		if (this.original != null && this.original != this) {
			this.original.cleanUp();
		}
		this.scanner = null;
		this.receiverVariable = null;
	}

	public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {

		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0 || this.binding == null || !this.binding.isValidBinding())
			return;

		MethodBinding codegenBinding = this.binding.original();
		if (codegenBinding.isVarargs())
			return; // completely managed by transforming into implicit lambda expression.

		SourceTypeBinding enclosingSourceType = currentScope.enclosingSourceType();

		if (this.isConstructorReference()) {
			ReferenceBinding allocatedType = codegenBinding.declaringClass;
			if (codegenBinding.isPrivate() &&
					TypeBinding.notEquals(enclosingSourceType, (allocatedType = codegenBinding.declaringClass))) {
				if ((allocatedType.tagBits & TagBits.IsLocalType) != 0) {
					codegenBinding.tagBits |= TagBits.ClearPrivateModifier;
				} else {
					if (currentScope.enclosingSourceType().isNestmateOf(this.binding.declaringClass)) {
						this.syntheticAccessor = codegenBinding;
						return;
					}
					this.syntheticAccessor = ((SourceTypeBinding) allocatedType).addSyntheticMethod(codegenBinding, false);
					currentScope.problemReporter().needToEmulateMethodAccess(codegenBinding, this);
				}
			}
			return;
		}

		// -----------------------------------   Only method references from now on -----------
		if (this.binding.isPrivate()) {
			if (TypeBinding.notEquals(enclosingSourceType, codegenBinding.declaringClass)){
				this.syntheticAccessor = ((SourceTypeBinding)codegenBinding.declaringClass).addSyntheticMethod(codegenBinding, false /* not super access */);
				currentScope.problemReporter().needToEmulateMethodAccess(codegenBinding, this);
			}
			return;
		}

		if (this.lhs.isSuper()) {
			SourceTypeBinding destinationType = enclosingSourceType;
			if (this.lhs instanceof QualifiedSuperReference) { 	// qualified super
				QualifiedSuperReference qualifiedSuperReference = (QualifiedSuperReference) this.lhs;
				TypeReference qualification = qualifiedSuperReference.qualification;
				if (!qualification.resolvedType.isInterface()) // we can't drop the bridge in I, it may not even be a source type.
					destinationType = (SourceTypeBinding) (qualifiedSuperReference.currentCompatibleType);
			}

			this.syntheticAccessor = destinationType.addSyntheticMethod(codegenBinding, true);
			currentScope.problemReporter().needToEmulateMethodAccess(codegenBinding, this);
			return;
		}

		if (this.binding.isProtected() && (this.bits & ASTNode.DepthMASK) != 0 && codegenBinding.declaringClass.getPackage() != enclosingSourceType.getPackage()) {
			SourceTypeBinding currentCompatibleType = (SourceTypeBinding) enclosingSourceType.enclosingTypeAt((this.bits & ASTNode.DepthMASK) >> ASTNode.DepthSHIFT);
			this.syntheticAccessor = currentCompatibleType.addSyntheticMethod(codegenBinding, isSuperAccess());
			currentScope.problemReporter().needToEmulateMethodAccess(codegenBinding, this);
			return;
		}
	}

	@Override
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		// static methods with receiver value never get here
		if (this.haveReceiver) {
			this.lhs.analyseCode(currentScope, flowContext, flowInfo, true);
			this.lhs.checkNPE(currentScope, flowContext, flowInfo);
		} else if (isConstructorReference()) {
			TypeBinding type = this.receiverType.leafComponentType();
			if (type.isNestedType() &&
				type instanceof ReferenceBinding && !((ReferenceBinding)type).isStatic()) {
				currentScope.tagAsAccessingEnclosingInstanceStateOf((ReferenceBinding)type, false);
				this.shouldCaptureInstance = true;
				ReferenceBinding allocatedTypeErasure = (ReferenceBinding) type.erasure();
				if (allocatedTypeErasure.isLocalType()) {
					((LocalTypeBinding) allocatedTypeErasure).addInnerEmulationDependent(currentScope, false);
					// request cascade of accesses
				}
			}
		}
		if (currentScope.compilerOptions().isAnyEnabled(IrritantSet.UNLIKELY_ARGUMENT_TYPE) && this.binding.isValidBinding()
				&& this.binding != null && this.binding.parameters != null) {
			if (this.binding.parameters.length == 1
					&& this.descriptor.parameters.length == (this.receiverPrecedesParameters ? 2 : 1)
					&& !this.binding.isStatic()) {
				final TypeBinding argumentType = this.descriptor.parameters[this.receiverPrecedesParameters ? 1 : 0];
				final TypeBinding actualReceiverType = this.receiverPrecedesParameters ? this.descriptor.parameters[0] : this.binding.declaringClass;
				UnlikelyArgumentCheck argumentCheck = UnlikelyArgumentCheck
						.determineCheckForNonStaticSingleArgumentMethod(argumentType, currentScope, this.selector,
								actualReceiverType, this.binding.parameters);
				if (argumentCheck != null && argumentCheck.isDangerous(currentScope)) {
					currentScope.problemReporter().unlikelyArgumentType(this, this.binding, argumentType,
							argumentCheck.typeToReport, argumentCheck.dangerousMethod);
				}
			} else if (this.binding.parameters.length == 2 && this.descriptor.parameters.length == 2 && this.binding.isStatic()) {
				final TypeBinding argumentType1 = this.descriptor.parameters[0];
				final TypeBinding argumentType2 = this.descriptor.parameters[1];
				UnlikelyArgumentCheck argumentCheck = UnlikelyArgumentCheck
						.determineCheckForStaticTwoArgumentMethod(argumentType2, currentScope, this.selector,
								argumentType1, this.binding.parameters, this.receiverType);
				if (argumentCheck != null && argumentCheck.isDangerous(currentScope)) {
					currentScope.problemReporter().unlikelyArgumentType(this, this.binding, argumentType2,
							argumentCheck.typeToReport, argumentCheck.dangerousMethod);
				}
			}
		}

		if (currentScope.compilerOptions().analyseResourceLeaks) {
			if (this.haveReceiver && CharOperation.equals(this.selector, TypeConstants.CLOSE)) {
				FakedTrackingVariable trackingVariable = FakedTrackingVariable.getCloseTrackingVariable(this.lhs, flowInfo, flowContext);
				if (trackingVariable != null) { // null happens if target is not a local variable or not an AutoCloseable
					trackingVariable.markClosedInNestedMethod(); // there is a close()-call, but we don't know if it will be invoked
				}
			}
		}
		manageSyntheticAccessIfNecessary(currentScope, flowInfo);
		return flowInfo;
	}

	@Override
	public boolean checkingPotentialCompatibility() {
		return this.checkingPotentialCompatibility;
	}

	@Override
	public void acceptPotentiallyCompatibleMethods(MethodBinding[] methods) {
		if (this.checkingPotentialCompatibility)
			this.potentialMethods = methods;
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {

		final CompilerOptions compilerOptions = scope.compilerOptions();
		TypeBinding lhsType;
    	if (this.constant != Constant.NotAConstant) {
    		this.constant = Constant.NotAConstant;
    		this.enclosingScope = scope;
    		if (this.original == this)
    			this.ordinal = recordFunctionalType(scope);

    		this.lhs.bits |= ASTNode.IgnoreRawTypeCheck;
    		lhsType = this.lhs.resolveType(scope);
    		this.lhs.computeConversion(scope, lhsType, lhsType);
    		if (this.typeArguments != null) {
    			int length = this.typeArguments.length;
    			this.typeArgumentsHaveErrors = compilerOptions.sourceLevel < ClassFileConstants.JDK1_5;
    			this.resolvedTypeArguments = new TypeBinding[length];
    			for (int i = 0; i < length; i++) {
    				TypeReference typeReference = this.typeArguments[i];
    				if ((this.resolvedTypeArguments[i] = typeReference.resolveType(scope, true /* check bounds*/)) == null) {
    					this.typeArgumentsHaveErrors = true;
    				}
    				if (this.typeArgumentsHaveErrors && typeReference instanceof Wildcard) { // resolveType on wildcard always return null above, resolveTypeArgument is the real workhorse.
    					scope.problemReporter().illegalUsageOfWildcard(typeReference);
    				}
    			}
    			if (this.typeArgumentsHaveErrors || lhsType == null)
    				return this.resolvedType = null;
    			if (isConstructorReference() && lhsType.isRawType()) {
    				scope.problemReporter().rawConstructorReferenceNotWithExplicitTypeArguments(this.typeArguments);
    				return this.resolvedType = null;
    			}
    		}
    		if (this.typeArgumentsHaveErrors || lhsType == null)
				return this.resolvedType = null;

    		if (lhsType.problemId() == ProblemReasons.AttemptToBypassDirectSuper)
    			lhsType = lhsType.closestMatch();	// improve resolving experience
        	if (lhsType == null || !lhsType.isValidBinding())
    			return this.resolvedType = null;	// nope, no useful type found

    		this.receiverType = lhsType;
			this.haveReceiver = true;
			if (this.lhs instanceof NameReference) {
				if ((this.lhs.bits & ASTNode.RestrictiveFlagMASK) == Binding.TYPE) {
					this.haveReceiver = false;
				} else if (isConstructorReference()) {
					scope.problemReporter().invalidType(
							this.lhs,
							new ProblemReferenceBinding(((NameReference) this.lhs).getName(), null,
									ProblemReasons.NotFound));
					return this.resolvedType = null;
				}
			} else if (this.lhs instanceof TypeReference) {
				this.haveReceiver = false;
			}
			if (!this.haveReceiver && !this.lhs.isSuper() && !this.isArrayConstructorReference())
				this.receiverType = lhsType.capture(scope, this.sourceStart, this.sourceEnd);

			if (!lhsType.isRawType()) // RawType::m and RawType::new are not exact method references
	    		this.binding = this.exactMethodBinding = isMethodReference() ? scope.getExactMethod(lhsType, this.selector, this) : scope.getExactConstructor(lhsType, this);

    		if (isConstructorReference() && !lhsType.canBeInstantiated()) {
    			scope.problemReporter().cannotInstantiate(this.lhs, lhsType);
    			return this.resolvedType = null;
    		}

    		if (this.lhs instanceof TypeReference && ((TypeReference)this.lhs).hasNullTypeAnnotation(AnnotationPosition.ANY)) {
    			scope.problemReporter().nullAnnotationUnsupportedLocation((TypeReference) this.lhs);
    		}

    		if (isConstructorReference() && lhsType.isArrayType()) {
	        	final TypeBinding leafComponentType = lhsType.leafComponentType();
				if (!leafComponentType.isReifiable()) {
	        		scope.problemReporter().illegalGenericArray(leafComponentType, this);
	        		return this.resolvedType = null;
	        	}
				if (this.typeArguments != null) {
	                scope.problemReporter().invalidTypeArguments(this.typeArguments);
	                return this.resolvedType = null;
	            }
	        	this.binding = this.exactMethodBinding = scope.getExactConstructor(lhsType, this);
	        }
			if (isMethodReference() && this.haveReceiver && (this.original == this)) {
				this.receiverVariable = new LocalVariableBinding(
						(SecretReceiverVariableName + this.nameSourceStart).toCharArray(), this.lhs.resolvedType,
						ClassFileConstants.AccDefault, false);
				scope.addLocalVariable(this.receiverVariable);
				this.receiverVariable.setConstant(Constant.NotAConstant); // not inlinable
				this.receiverVariable.useFlag = LocalVariableBinding.USED;
			}

	    	if (this.expectedType == null && this.expressionContext == INVOCATION_CONTEXT) {
	    		if (compilerOptions.isAnnotationBasedNullAnalysisEnabled && this.binding != null) {
	    			ImplicitNullAnnotationVerifier.ensureNullnessIsKnown(this.binding, scope);
	    		}
	    		return new PolyTypeBinding(this);
			}

    	} else {
    		lhsType = this.lhs.resolvedType;
    		if (this.typeArgumentsHaveErrors || lhsType == null)
				return this.resolvedType = null;
    	}

    	super.resolveType(scope);

		/* For Reference expressions unlike other call sites, we always have a receiver _type_ since LHS of :: cannot be empty.
		   LHS's resolved type == actual receiver type. All code below only when a valid descriptor is available.
		*/
    	if (this.descriptor == null || !this.descriptor.isValidBinding())
    		return this.resolvedType =  null;

    	// Convert parameters into argument expressions for look up.
		TypeBinding[] descriptorParameters = descriptorParametersAsArgumentExpressions();

		if (lhsType.isBaseType()) {
			scope.problemReporter().errorNoMethodFor(this.lhs, lhsType, this.selector, descriptorParameters);
			return this.resolvedType = null;
		}

		/* 15.13: "If a method reference expression has the form super :: [TypeArguments] Identifier or TypeName . super :: [TypeArguments] Identifier,
		   it is a compile-time error if the expression occurs in a static context. ": This is nop since the primary when it resolves
		   itself will complain automatically.

		   15.13: "The immediately enclosing instance of an inner class instance (15.9.2) must be provided for a constructor reference by a lexically
		   enclosing instance of this (8.1.3)", we will actually implement this check in code generation. Emulation path computation will fail if there
		   is no suitable enclosing instance. While this could be pulled up to here, leaving it to code generation is more consistent with Java 5,6,7
		   modus operandi.
		*/

		// handle the special case of array construction first.
		final int parametersLength = descriptorParameters.length;
        if (isConstructorReference() && lhsType.isArrayType()) {
        	if (parametersLength != 1 || scope.parameterCompatibilityLevel(descriptorParameters[0], TypeBinding.INT) == Scope.NOT_COMPATIBLE) {
        		scope.problemReporter().invalidArrayConstructorReference(this, lhsType, descriptorParameters);
        		return this.resolvedType = null;
        	}
        	if (this.descriptor.returnType.isProperType(true) && !lhsType.isCompatibleWith(this.descriptor.returnType) && this.descriptor.returnType.id != TypeIds.T_void) {
        		scope.problemReporter().constructedArrayIncompatible(this, lhsType, this.descriptor.returnType);
        		return this.resolvedType = null;
        	}
            checkNullAnnotations(scope);
        	return this.resolvedType;
        }

        // 15.13.1
        final boolean isMethodReference = isMethodReference();
        this.depth = 0;
        this.freeParameters = descriptorParameters;
        MethodBinding someMethod = null;
        if (isMethodReference) {
        	someMethod = scope.getMethod(this.receiverType, this.selector, descriptorParameters, this);
        } else {
        	if (argumentsTypeElided() && this.receiverType.isRawType()) {
        		boolean[] inferredReturnType = new boolean[1];
	        	someMethod = AllocationExpression.inferDiamondConstructor(scope, this, this.receiverType, this.descriptor.parameters, inferredReturnType);
        	}
        	if (someMethod == null)
        		someMethod = scope.getConstructor((ReferenceBinding) this.receiverType, descriptorParameters, this);
        }
        int someMethodDepth = this.depth, anotherMethodDepth = 0;
    	if (someMethod != null && someMethod.isValidBinding()) {
    		if (someMethod.isStatic() && (this.haveReceiver || this.receiverType.isParameterizedTypeWithActualArguments())) {
    			scope.problemReporter().methodMustBeAccessedStatically(this, someMethod);
    			return this.resolvedType = null;
    		}
        }

    	if (this.lhs.isSuper() && this.lhs.resolvedType.isInterface()) {
    		scope.checkAppropriateMethodAgainstSupers(this.selector, someMethod, this.descriptor.parameters, this);
    	}

        MethodBinding anotherMethod = null;
        this.receiverPrecedesParameters = false;
        if (!this.haveReceiver && isMethodReference && parametersLength > 0) {
        	final TypeBinding potentialReceiver = descriptorParameters[0];
        	if (potentialReceiver.isCompatibleWith(this.receiverType, scope)) {
        		TypeBinding typeToSearch = this.receiverType;
        		if (this.receiverType.isRawType()) {
        			TypeBinding superType = potentialReceiver.findSuperTypeOriginatingFrom(this.receiverType);
        			if (superType != null)
        				typeToSearch = superType.capture(scope, this.sourceStart, this.sourceEnd);
        		}
        		TypeBinding [] parameters = Binding.NO_PARAMETERS;
        		if (parametersLength > 1) {
        			parameters = new TypeBinding[parametersLength - 1];
        			System.arraycopy(descriptorParameters, 1, parameters, 0, parametersLength - 1);
        		}
        		this.depth = 0;
        		this.freeParameters = parameters;
        		anotherMethod = scope.getMethod(typeToSearch, this.selector, parameters, this);
        		anotherMethodDepth = this.depth;
        		this.depth = 0;
        	}
        }

        if (someMethod != null && someMethod.isValidBinding() && someMethod.isStatic() && anotherMethod != null && anotherMethod.isValidBinding() && !anotherMethod.isStatic()) {
        	scope.problemReporter().methodReferenceSwingsBothWays(this, anotherMethod, someMethod);
        	return this.resolvedType = null;
        }

        if (someMethod != null && someMethod.isValidBinding() && (anotherMethod == null || !anotherMethod.isValidBinding() || anotherMethod.isStatic())) {
        	this.binding = someMethod;
        	this.bits &= ~ASTNode.DepthMASK;
        	if (someMethodDepth > 0) {
        		this.bits |= (someMethodDepth & 0xFF) << ASTNode.DepthSHIFT;
        	}
        	if (!this.haveReceiver) {
        		if (!someMethod.isStatic() && !someMethod.isConstructor()) {
        			scope.problemReporter().methodMustBeAccessedWithInstance(this, someMethod);
        			return this.resolvedType = null;
        		}
        	}
        } else if (anotherMethod != null && anotherMethod.isValidBinding() && (someMethod == null || !someMethod.isValidBinding() || !someMethod.isStatic())) {
        	this.binding = anotherMethod;
        	this.receiverPrecedesParameters = true; // 0 is receiver, real parameters start at 1
        	this.bits &= ~ASTNode.DepthMASK;
        	if (anotherMethodDepth > 0) {
        		this.bits |= (anotherMethodDepth & 0xFF) << ASTNode.DepthSHIFT;
        	}
        	if (anotherMethod.isStatic()) {
        		scope.problemReporter().methodMustBeAccessedStatically(this, anotherMethod);
        		return this.resolvedType = null;
        	}
        } else {
        	this.binding = null;
        	this.bits &= ~ASTNode.DepthMASK;
        }

        if (this.binding == null) {
        	char [] visibleName = isConstructorReference() ? this.receiverType.sourceName() : this.selector;
        	scope.problemReporter().danglingReference(this, this.receiverType, visibleName, descriptorParameters);
			return this.resolvedType = null;
        }

        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=382350#c2, I.super::abstractMethod will be handled there.

        if (this.binding.isAbstract() && this.lhs.isSuper())
        	scope.problemReporter().cannotDireclyInvokeAbstractMethod(this, this.binding);

        if (this.binding.isStatic()) {
        	if (TypeBinding.notEquals(this.binding.declaringClass, this.receiverType))
        		scope.problemReporter().indirectAccessToStaticMethod(this, this.binding);
        } else {
        	AbstractMethodDeclaration srcMethod = this.binding.sourceMethod();
        	if (srcMethod != null && srcMethod.isMethod())
        		srcMethod.bits &= ~ASTNode.CanBeStatic;
        }

    	if (isMethodUseDeprecated(this.binding, scope, true, this))
    		scope.problemReporter().deprecatedMethod(this.binding, this);

    	if (this.typeArguments != null && this.binding.original().typeVariables == Binding.NO_TYPE_VARIABLES)
    		scope.problemReporter().unnecessaryTypeArgumentsForMethodInvocation(this.binding, this.resolvedTypeArguments, this.typeArguments);

    	if ((this.binding.tagBits & TagBits.HasMissingType) != 0)
    		scope.problemReporter().missingTypeInMethod(this, this.binding);


        // OK, we have a compile time declaration, see if it passes muster.
        TypeBinding [] methodExceptions = this.binding.thrownExceptions;
        TypeBinding [] kosherExceptions = this.descriptor.thrownExceptions;
        next: for (int i = 0, iMax = methodExceptions.length; i < iMax; i++) {
        	if (methodExceptions[i].isUncheckedException(false)) {
        		continue next;
    		}
        	for (int j = 0, jMax = kosherExceptions.length; j < jMax; j++) {
        		if (methodExceptions[i].isCompatibleWith(kosherExceptions[j], scope))
        			continue next;
        	}
        	scope.problemReporter().unhandledException(methodExceptions[i], this);
        }
        checkNullAnnotations(scope);
        this.freeParameters = null; // not used after method lookup

    	if (checkInvocationArguments(scope, null, this.receiverType, this.binding, null, descriptorParameters, false, this))
    		this.bits |= ASTNode.Unchecked;

    	if (this.descriptor.returnType.id != TypeIds.T_void) {
    		TypeBinding returnType = null;
    		if (this.binding.isConstructor()) {
    			returnType = this.receiverType;
    		} else {
    			if ((this.bits & ASTNode.Unchecked) != 0 && this.resolvedTypeArguments == null) {
    				returnType = this.binding.returnType;
    				if (returnType != null) {
    					returnType = scope.environment().convertToRawType(returnType.erasure(), true);
    				}
    			} else {
    				returnType = this.binding.returnType;
    				if (returnType != null) {
    					returnType = returnType.capture(scope, this.sourceStart, this.sourceEnd);
    				}
    			}
    		}
    		if (this.descriptor.returnType.isProperType(true) // otherwise we cannot yet check compatibility
    				&& !returnType.isCompatibleWith(this.descriptor.returnType, scope)
    				&& !isBoxingCompatible(returnType, this.descriptor.returnType, this, scope))
    		{
    			scope.problemReporter().incompatibleReturnType(this, this.binding, this.descriptor.returnType);
    			this.binding = null;
    			this.resolvedType = null;
    		}
    	}

    	return this.resolvedType; // Phew !
	}

	protected void checkNullAnnotations(BlockScope scope) {
		CompilerOptions compilerOptions = scope.compilerOptions();
		if (compilerOptions.isAnnotationBasedNullAnalysisEnabled) {
        	if (this.expectedType == null || !NullAnnotationMatching.hasContradictions(this.expectedType)) { // otherwise assume it has been reported and we can do nothing here
        		ImplicitNullAnnotationVerifier.ensureNullnessIsKnown(this.binding, scope);
	        	// TODO: simplify by using this.freeParameters?
	        	int len;
	        	int expectedlen = this.binding.parameters.length;
	        	int providedLen = this.descriptor.parameters.length;
	        	if (this.receiverPrecedesParameters) {
	        		providedLen--; // one parameter is 'consumed' as the receiver

	        		TypeBinding descriptorParameter = this.descriptor.parameters[0];
	    			if((descriptorParameter.tagBits & TagBits.AnnotationNullable) != 0) { // Note: normal dereferencing of 'unchecked' values is not reported, either
		    			final TypeBinding receiver = scope.environment().createAnnotatedType(this.binding.declaringClass,
								new AnnotationBinding[] { scope.environment().getNonNullAnnotation() });
    					scope.problemReporter().referenceExpressionArgumentNullityMismatch(this, receiver, descriptorParameter, this.descriptor, -1, NullAnnotationMatching.NULL_ANNOTATIONS_MISMATCH);
	    			}
	        	}
	        	boolean isVarArgs = false;
	        	if (this.binding.isVarargs()) {
	        		isVarArgs = (providedLen == expectedlen)
						? !this.descriptor.parameters[expectedlen-1].isCompatibleWith(this.binding.parameters[expectedlen-1])
						: true;
	        		len = providedLen; // binding parameters will be padded from InferenceContext18.getParameter()
	        	} else {
	        		len = Math.min(expectedlen, providedLen);
	        	}
	    		for (int i = 0; i < len; i++) {
	    			TypeBinding descriptorParameter = this.descriptor.parameters[i + (this.receiverPrecedesParameters ? 1 : 0)];
	    			TypeBinding bindingParameter = InferenceContext18.getParameter(this.binding.parameters, i, isVarArgs);
					TypeBinding bindingParameterToCheck;
					if (bindingParameter.isPrimitiveType() && !descriptorParameter.isPrimitiveType()) {
						// replace primitive types by boxed equivalent for checking, e.g. int -> @NonNull Integer
						bindingParameterToCheck = scope.environment().createAnnotatedType(scope.boxing(bindingParameter),
								new AnnotationBinding[] { scope.environment().getNonNullAnnotation() });
					} else {
						bindingParameterToCheck = bindingParameter;
					}
	    			NullAnnotationMatching annotationStatus = NullAnnotationMatching.analyse(bindingParameterToCheck, descriptorParameter, FlowInfo.UNKNOWN);
	    			if (annotationStatus.isAnyMismatch()) {
	    				// immediate reporting:
	    				scope.problemReporter().referenceExpressionArgumentNullityMismatch(this, bindingParameter, descriptorParameter, this.descriptor, i, annotationStatus);
	    			}
	    		}
	    		TypeBinding returnType = this.binding.returnType;
	    		if(!returnType.isPrimitiveType()) {
		    		if (this.binding.isConstructor()) {
		    			returnType = scope.environment().createAnnotatedType(this.receiverType, new AnnotationBinding[]{ scope.environment().getNonNullAnnotation() });
		    		}
		    		NullAnnotationMatching annotationStatus = NullAnnotationMatching.analyse(this.descriptor.returnType, returnType, FlowInfo.UNKNOWN);
		        	if (annotationStatus.isAnyMismatch()) {
	        			scope.problemReporter().illegalReturnRedefinition(this, this.descriptor, annotationStatus.isUnchecked(), returnType);
		        	}
	    		}
        	}
        }
	}

	private TypeBinding[] descriptorParametersAsArgumentExpressions() {

		if (this.descriptor == null || this.descriptor.parameters == null || this.descriptor.parameters.length == 0)
			return Binding.NO_PARAMETERS;

		/* 15.13.1, " ... method reference is treated as if it were an invocation with argument expressions of types P1, ..., Pn;"
		   This implies/requires wildcard capture. This creates interesting complications, we can't just take the descriptor parameters
		   and apply captures - where a single wildcard type got "fanned out" and propagated into multiple locations through type variable
		   substitutions, we will end up creating distinct captures defeating the very idea of capture. We need to first capture and then
		   fan out. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=432759.
		*/
		if (this.expectedType.isParameterizedType()) {
			ParameterizedTypeBinding type = (ParameterizedTypeBinding) this.expectedType;
			MethodBinding method = type.getSingleAbstractMethod(this.enclosingScope, true, this.sourceStart, this.sourceEnd);
			return method.parameters;
		}
		return this.descriptor.parameters;
	}

	private boolean contextHasSyntaxError() {
		ReferenceContext referenceContext = this.enclosingScope.referenceContext();
		if (referenceContext instanceof AbstractMethodDeclaration) {
			if ((((AbstractMethodDeclaration) referenceContext).bits & ASTNode.HasSyntaxErrors) != 0)
				return true;
		}
		return false;
	}

	// Cache resolved copies against various target types, so repeat overload resolution and possibly type inference could be avoided.
	private ReferenceExpression cachedResolvedCopy(TypeBinding targetType) {

		ReferenceExpression copy = this.copiesPerTargetType != null ? this.copiesPerTargetType.get(targetType) : null;
		if (copy != null)
			return copy;

		if (contextHasSyntaxError())
			return null;

		IErrorHandlingPolicy oldPolicy = this.enclosingScope.problemReporter().switchErrorHandlingPolicy(silentErrorHandlingPolicy);
		try {
			copy = copy();
			if (copy == null) { // should never happen even for code assist.
				return null;
			}
			copy.setExpressionContext(this.expressionContext);
			copy.setExpectedType(targetType);
			copy.resolveType(this.enclosingScope);

			if (this.copiesPerTargetType == null)
				this.copiesPerTargetType = new HashMap<TypeBinding, ReferenceExpression>();
			this.copiesPerTargetType.put(targetType, copy);

			return copy;
		} finally {
			this.enclosingScope.problemReporter().switchErrorHandlingPolicy(oldPolicy);
		}
	}

	public void registerInferenceContext(ParameterizedGenericMethodBinding method, InferenceContext18 context) {
		if (this.inferenceContexts == null)
			this.inferenceContexts = new HashMap<ParameterizedGenericMethodBinding, InferenceContext18>();
		this.inferenceContexts.put(method, context);
	}

	public InferenceContext18 getInferenceContext(ParameterizedMethodBinding method) {
		if (this.inferenceContexts == null)
			return null;
		return this.inferenceContexts.get(method);
	}

	@Override
	public ReferenceExpression resolveExpressionExpecting(TypeBinding targetType, Scope scope, InferenceContext18 inferenceContext) {
		if (this.exactMethodBinding != null) { // We may see inference variables in target type.
			MethodBinding functionType = targetType.getSingleAbstractMethod(scope, true);
			if (functionType == null || functionType.problemId() == ProblemReasons.NoSuchSingleAbstractMethod)
				return null;
			int n = functionType.parameters.length;
			int k = this.exactMethodBinding.parameters.length;

			if (!this.haveReceiver && this.isMethodReference() && !this.exactMethodBinding.isStatic()) {
				k++;
			}
			return (n == k) ? this : null;
		}
		// descriptors parameters should be free of inference variables.
		ReferenceExpression copy = cachedResolvedCopy(targetType);
		return copy != null && copy.resolvedType != null && copy.resolvedType.isValidBinding() && copy.binding != null && copy.binding.isValidBinding() ? copy : null;
	}

	public boolean isConstructorReference() {
		return CharOperation.equals(this.selector,  ConstantPool.Init);
	}

	@Override
	public boolean isExactMethodReference() {
		return this.exactMethodBinding != null;
	}

	public MethodBinding getExactMethod() {
		return this.exactMethodBinding;
	}

	public boolean isMethodReference() {
		return !CharOperation.equals(this.selector,  ConstantPool.Init);
	}

	@Override
	public boolean isPertinentToApplicability(TypeBinding targetType, MethodBinding method) {
		if (!this.isExactMethodReference()) {
			return false;
		}
		return super.isPertinentToApplicability(targetType, method);
	}

	@Override
	public TypeBinding[] genericTypeArguments() {
		return this.resolvedTypeArguments;
	}

	@Override
	public InferenceContext18 freshInferenceContext(Scope scope) {
		if (this.expressionContext != ExpressionContext.VANILLA_CONTEXT) {
			Expression[] arguments = createPseudoExpressions(this.freeParameters);
			return new InferenceContext18(scope, arguments, this, null);
		}
		return null; // shouldn't happen, actually
	}

	@Override
	public boolean isSuperAccess() {
		return this.lhs.isSuper();
	}

	@Override
	public boolean isTypeAccess() {
		return !this.haveReceiver;
	}

	@Override
	public void setActualReceiverType(ReferenceBinding receiverType) {
		return;
	}

	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
	public void setFieldIndex(int depth) {
		return;
	}

	@Override
	public StringBuffer printExpression(int tab, StringBuffer output) {

		this.lhs.print(0, output);
		output.append("::"); //$NON-NLS-1$
		if (this.typeArguments != null) {
			output.append('<');
			int max = this.typeArguments.length - 1;
			for (int j = 0; j < max; j++) {
				this.typeArguments[j].print(0, output);
				output.append(", ");//$NON-NLS-1$
			}
			this.typeArguments[max].print(0, output);
			output.append('>');
		}
		if (isConstructorReference())
			output.append("new"); //$NON-NLS-1$
		else
			output.append(this.selector);

		return output;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {

			this.lhs.traverse(visitor, blockScope);

			int length = this.typeArguments == null ? 0 : this.typeArguments.length;
			for (int i = 0; i < length; i++) {
				this.typeArguments[i].traverse(visitor, blockScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}

	public Expression[] createPseudoExpressions(TypeBinding[] p) {
		// from 15.13.1:
		// ... the reference is treated as if it were an invocation with argument expressions of types P1..Pn
		// ... the reference is treated as if it were an invocation with argument expressions of types P2..Pn
		// (the different sets of types are passed from our resolveType to scope.getMethod(..), see someMethod, anotherMethod)
		Expression[] expressions = new Expression[p.length];
		long pos = (((long)this.sourceStart)<<32)+this.sourceEnd;
		for (int i = 0; i < p.length; i++) {
			expressions[i] = new SingleNameReference(("fakeArg"+i).toCharArray(), pos); //$NON-NLS-1$
			expressions[i].resolvedType = p[i];
		}
		return expressions;
	}

	@Override
	public boolean isPotentiallyCompatibleWith(TypeBinding targetType, Scope scope) {

        final boolean isConstructorRef = isConstructorReference();
		if (isConstructorRef) {
			if (this.receiverType == null)
				return false;
			if (this.receiverType.isArrayType()) {
				final TypeBinding leafComponentType = this.receiverType.leafComponentType();
				if (!leafComponentType.isReifiable()) {
					return false;
				}
			}
		}

		// We get here only when the reference expression is NOT pertinent to applicability.
		if (!super.isPertinentToApplicability(targetType, null))
			return true;
		final MethodBinding sam = targetType.getSingleAbstractMethod(this.enclosingScope, true);
		if (sam == null || !sam.isValidBinding())
			return false;
		if (this.typeArgumentsHaveErrors || this.receiverType == null || !this.receiverType.isValidBinding())
			return false;

		int parametersLength = sam.parameters.length;
		TypeBinding[] descriptorParameters = new TypeBinding[parametersLength];
		for (int i = 0; i < parametersLength; i++) {
			descriptorParameters[i] = new ReferenceBinding() {
				{
					this.compoundName = CharOperation.NO_CHAR_CHAR;
				}
				@Override
				public boolean isCompatibleWith(TypeBinding otherType, Scope captureScope) {
					return true;
				}
				@Override
				public TypeBinding findSuperTypeOriginatingFrom(TypeBinding otherType) {
					return otherType;
				}
				@Override
				public String toString() {
					return "(wildcard)"; //$NON-NLS-1$
				}
			};
		}

		// 15.13.1
        this.freeParameters = descriptorParameters;
        this.checkingPotentialCompatibility = true;
        try {
			MethodBinding compileTimeDeclaration = getCompileTimeDeclaration(scope, isConstructorRef, descriptorParameters);

        	if (compileTimeDeclaration != null && compileTimeDeclaration.isValidBinding()) // we have the mSMB.
        		this.potentialMethods = new MethodBinding [] { compileTimeDeclaration };
        	else {
        		/* We EITHER have potential methods that are input to Scope.mSMb already captured in this.potentialMethods
        	       OR there is no potentially compatible compile time declaration ...
        		 */
        	}

        	/* 15.12.2.1: A method reference expression (§15.13) is potentially compatible with a functional interface type if, where the type's function type arity is n,
		       there exists at least one potentially applicable method for the method reference expression with arity n (§15.13.1), and one of the following is true:
                   – The method reference expression has the form ReferenceType ::[TypeArguments] Identifier and at least one potentially applicable method is
                        i) static and supports arity n, or ii) not static and supports arity n-1.
                   – The method reference expression has some other form and at least one potentially applicable method is not static.
        	*/

        	for (int i = 0, length = this.potentialMethods.length; i < length; i++) {
        		if (this.potentialMethods[i].isStatic() || this.potentialMethods[i].isConstructor()) {
        			if (!this.haveReceiver) // form ReferenceType ::[TypeArguments] Identifier
        				return true;
        		} else {
        			if (this.haveReceiver) // some other form.
        				return true;
        		}
        	}

        	if (this.haveReceiver || parametersLength == 0)
        		return false;

        	System.arraycopy(descriptorParameters, 1, descriptorParameters = new TypeBinding[parametersLength - 1], 0, parametersLength - 1);
        	this.freeParameters = descriptorParameters;
        	this.potentialMethods = Binding.NO_METHODS;
        	compileTimeDeclaration = getCompileTimeDeclaration(scope, false, descriptorParameters);

        	if (compileTimeDeclaration != null && compileTimeDeclaration.isValidBinding()) // we have the mSMB.
        		this.potentialMethods = new MethodBinding [] { compileTimeDeclaration };
        	else {
        		/* We EITHER have potential methods that are input to Scope.mSMb already captured in this.potentialMethods
              	   OR there is no potentially compatible compile time declaration ...
        		*/
        	}
        	for (int i = 0, length = this.potentialMethods.length; i < length; i++) {
			if (!this.potentialMethods[i].isStatic() && !this.potentialMethods[i].isConstructor()) {
        			return true;
        		}
        	}
        } finally {
        	this.checkingPotentialCompatibility = false;
        	this.potentialMethods = Binding.NO_METHODS;
        	this.freeParameters = null; // not used after method lookup
        }
        return false;
	}

	MethodBinding getCompileTimeDeclaration(Scope scope, boolean isConstructorRef, TypeBinding[] parameters) {
		if (this.exactMethodBinding != null)
			return this.exactMethodBinding;
		else if (this.receiverType.isArrayType())
			return scope.findMethodForArray((ArrayBinding) this.receiverType, this.selector, Binding.NO_PARAMETERS, this);
		else if (isConstructorRef)
			return scope.getConstructor((ReferenceBinding) this.receiverType, parameters, this);
		else
			return scope.getMethod(this.receiverType, this.selector, parameters, this);
	}

	@Override
	public boolean isCompatibleWith(TypeBinding targetType, Scope scope) {
		ReferenceExpression copy = cachedResolvedCopy(targetType);
		if (copy == null) {
			return contextHasSyntaxError(); // in case of syntax error avoid secondary errors
		} else if (copy.resolvedType != null && copy.resolvedType.isValidBinding() && copy.binding != null && copy.binding.isValidBinding()) {
			return true;
		} else {
			boolean notPertinentToApplicability = targetType instanceof ParameterizedTypeBinding && !isPertinentToApplicability(targetType, null); // not mentioned in JLS (see prior art in LE.internalIsCompatibleWith()
			return notPertinentToApplicability;
		}
	}

	@Override
	public boolean sIsMoreSpecific(TypeBinding s, TypeBinding t, Scope scope) {

		if (super.sIsMoreSpecific(s, t, scope))
			return true;

		if (this.exactMethodBinding == null || t.findSuperTypeOriginatingFrom(s) != null)
			return false;

		s = s.capture(this.enclosingScope, this.sourceStart, this.sourceEnd);
		MethodBinding sSam = s.getSingleAbstractMethod(this.enclosingScope, true);
		if (sSam == null || !sSam.isValidBinding())
			return false;
		TypeBinding r1 = sSam.returnType;

		MethodBinding tSam = t.getSingleAbstractMethod(this.enclosingScope, true);
		if (tSam == null || !tSam.isValidBinding())
			return false;
		TypeBinding r2 = tSam.returnType;

		TypeBinding[] sParams = sSam.parameters;
		TypeBinding[] tParams = tSam.parameters;
		// Both must have the same number of parameters if we got this far
		for (int i = 0; i < sParams.length; i++) {
			if (TypeBinding.notEquals(sParams[i], tParams[i]))
				return false;
		}
		if (r2.id == TypeIds.T_void)
			return true;

		if (r1.id == TypeIds.T_void)
			return false;

		// r1 <: r2
		if (r1.isCompatibleWith(r2, scope))
			return true;

		return r1.isBaseType() != r2.isBaseType() && r1.isBaseType() == this.exactMethodBinding.returnType.isBaseType();
	}

	@Override
	public org.eclipse.jdt.internal.compiler.lookup.MethodBinding getMethodBinding() {
		if (this.actualMethodBinding == null)  // array new/clone, no real binding.
			this.actualMethodBinding = this.binding;
		return this.actualMethodBinding;
	}

	public boolean isArrayConstructorReference() {
		return isConstructorReference() && this.lhs.resolvedType != null && this.lhs.resolvedType.isArrayType();
	}
}
