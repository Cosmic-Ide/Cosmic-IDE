/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *								Bug 342671 - ClassCastException: org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding cannot be cast to org.eclipse.jdt.internal.compiler.lookup.ArrayBinding
 *								Bug 429813 - [1.8][dom ast] IMethodBinding#getJavaElement() should return IMethod for lambda
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.MethodBinding.LambdaMethod;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.jdt.internal.compiler.ast.JavadocModuleReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Literal;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Receiver;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedGenericMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemPackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.RecordComponentBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VoidTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Internal class for resolving bindings using old ASTs.
 * <p>
 * IMPORTANT: The methods on this class are synchronized. This is required
 * because there may be multiple clients in separate threads concurrently
 * reading an AST and asking for bindings for its nodes. These requests all
 * end up invoking instance methods on this class. There are various internal
 * tables and caches which are built and maintained in the course of looking
 * up bindings. To ensure that they remain coherent in the presence of multiple
 * threads, the methods are synchronized on the DefaultBindingResolver instance.
 * </p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class DefaultBindingResolver extends BindingResolver {

	/*
	 * Holds on binding tables that can be shared by several ASTs.
	 */
	static class BindingTables {

		/**
		 * This map is used to get a binding from its binding key.
		 */
		Map bindingKeysToBindings;
		/**
		 * This map is used to keep the correspondence between new bindings and the
		 * compiler bindings to their internal counterpart.
		 * This is an identity map. We should only create one object for one binding.
		 */
		Map compilerBindingsToASTBindings;

		/**
		 * This map is used to keep the correspondence between new annotation instances to their internal counterpart.
		 * This is an identity map. We should only create one object for one annotation.
		 */
		Map compilerAnnotationBindingsToASTBindings;

		BindingTables() {
			this.compilerBindingsToASTBindings = new ConcurrentHashMap();
			this.compilerAnnotationBindingsToASTBindings = new ConcurrentHashMap();
			this.bindingKeysToBindings = new ConcurrentHashMap();
		}

	}
	/**
	 * This map is used to retrieve the corresponding block scope for a ast node
	 */
	Map astNodesToBlockScope;

	/**
	 * This map is used to get an ast node from its binding (new binding) or DOM
	 */
	Map bindingsToAstNodes;

	/*
	 * The shared binding tables accros ASTs.
	 */
	BindingTables bindingTables;

	/**
	 * This map is used to retrieve an old ast node using the new ast node. This is not an
	 * identity map, as several nested DOM nodes may be associated with the same "larger"
	 * compiler AST node.
	 * E.g., an ArrayAllocationExpression "new MyType[1]" will appear as the right-hand value
	 * for the SimpleType "MyType", the ArrayType "MyType[1]", and the ArrayCreation "new MyType[1]".
	 */
	Map newAstToOldAst;

	/**
	 * Compilation unit scope
	 */
	private CompilationUnitScope scope;

	/**
	 * The working copy owner that defines the context in which this resolver is creating the bindings.
	 */
	WorkingCopyOwner workingCopyOwner;

	/**
	 * Toggle controlling whether DOM bindings should be created when missing internal compiler bindings..
	 */
	boolean isRecoveringBindings;

	/**
	 * Set to <code>true</code> if initialized from a java project
	 */
	boolean fromJavaProject;

	/**
	 * Constructor for DefaultBindingResolver.
	 */
	DefaultBindingResolver(CompilationUnitScope scope, WorkingCopyOwner workingCopyOwner, BindingTables bindingTables, boolean isRecoveringBindings, boolean fromJavaProject) {
		this.newAstToOldAst = new HashMap();
		this.astNodesToBlockScope = new HashMap();
		this.bindingsToAstNodes = new HashMap();
		this.bindingTables = bindingTables;
		this.scope = scope;
		this.workingCopyOwner = workingCopyOwner;
		this.isRecoveringBindings = isRecoveringBindings;
		this.fromJavaProject = fromJavaProject;
	}

	DefaultBindingResolver(LookupEnvironment lookupEnvironment, WorkingCopyOwner workingCopyOwner, BindingTables bindingTables, boolean isRecoveringBindings, boolean fromJavaProject) {
		this.newAstToOldAst = new HashMap();
		this.astNodesToBlockScope = new HashMap();
		this.bindingsToAstNodes = new HashMap();
		this.bindingTables = bindingTables;
		this.scope = new CompilationUnitScope(new CompilationUnitDeclaration(null, null, -1), lookupEnvironment);
		this.workingCopyOwner = workingCopyOwner;
		this.isRecoveringBindings = isRecoveringBindings;
		this.fromJavaProject = fromJavaProject;
	}

	@Override
	synchronized ASTNode findDeclaringNode(IBinding binding) {
		if (binding == null) {
			return null;
		}
		if (binding instanceof IMethodBinding) {
			IMethodBinding methodBinding = (IMethodBinding) binding;
			return (ASTNode) this.bindingsToAstNodes.get(methodBinding.getMethodDeclaration());
		} else if (binding instanceof ITypeBinding) {
			ITypeBinding typeBinding = (ITypeBinding) binding;
			return (ASTNode) this.bindingsToAstNodes.get(typeBinding.getTypeDeclaration());
		} else if (binding instanceof IVariableBinding) {
			IVariableBinding variableBinding = (IVariableBinding) binding;
			return (ASTNode) this.bindingsToAstNodes.get(variableBinding.getVariableDeclaration());
		}
		return (ASTNode) this.bindingsToAstNodes.get(binding);
	}

	@Override
	synchronized ASTNode findDeclaringNode(String bindingKey) {
		if (bindingKey == null) {
			return null;
		}
		Object binding = this.bindingTables.bindingKeysToBindings.get(bindingKey);
		if (binding == null)
			return null;
		return (ASTNode) this.bindingsToAstNodes.get(binding);
	}

	IBinding getBinding(org.eclipse.jdt.internal.compiler.lookup.Binding binding) {
		switch (binding.kind()) {
			case Binding.PACKAGE:
				return getPackageBinding((org.eclipse.jdt.internal.compiler.lookup.PackageBinding) binding);
			case Binding.TYPE:
			case Binding.BASE_TYPE:
			case Binding.GENERIC_TYPE:
			case Binding.PARAMETERIZED_TYPE:
			case Binding.RAW_TYPE:
				return getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding) binding);
			case Binding.ARRAY_TYPE:
			case Binding.TYPE_PARAMETER:
				return new TypeBinding(this, (org.eclipse.jdt.internal.compiler.lookup.TypeBinding) binding);
			case Binding.METHOD:
				return getMethodBinding((org.eclipse.jdt.internal.compiler.lookup.MethodBinding) binding);
			case Binding.MODULE:
				return getModuleBinding((org.eclipse.jdt.internal.compiler.lookup.ModuleBinding) binding);
			case Binding.FIELD:
			case Binding.LOCAL:
			case Binding.RECORD_COMPONENT:
				return getVariableBinding((org.eclipse.jdt.internal.compiler.lookup.VariableBinding) binding);
		}
		return null;
	}

	Util.BindingsToNodesMap getBindingsToNodesMap() {
		return new Util.BindingsToNodesMap() {
			@Override
			public org.eclipse.jdt.internal.compiler.ast.ASTNode get(Binding binding) {
				return (org.eclipse.jdt.internal.compiler.ast.ASTNode)
					DefaultBindingResolver.this.newAstToOldAst.get(DefaultBindingResolver.this.bindingsToAstNodes.get(binding));
			}
		};
	}

	@Override
	synchronized org.eclipse.jdt.internal.compiler.ast.ASTNode getCorrespondingNode(ASTNode currentNode) {
		return (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(currentNode);
	}

	@Override
	synchronized IMethodBinding getMethodBinding(org.eclipse.jdt.internal.compiler.lookup.MethodBinding methodBinding) {
		return getMethodOrLambdaBinding(methodBinding, null, null);
	}

	private synchronized IMethodBinding getMethodOrLambdaBinding(org.eclipse.jdt.internal.compiler.lookup.MethodBinding methodBinding,
													org.eclipse.jdt.internal.compiler.lookup.MethodBinding descriptor,
													IBinding enclosingBinding)
	{
 		if (methodBinding != null && !methodBinding.isValidBinding()) {
			org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding problemMethodBinding =
				(org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding) methodBinding;
			methodBinding = problemMethodBinding.closestMatch;
 		}

		if (methodBinding != null) {
			if (!this.isRecoveringBindings && ((methodBinding.tagBits & TagBits.HasMissingType) != 0)) {
				return null;
			}
			IMethodBinding binding = (IMethodBinding) this.bindingTables.compilerBindingsToASTBindings.get(methodBinding);
			if (binding != null) {
				return binding;
			}
			if (descriptor != null && enclosingBinding != null) {
				binding = new MethodBinding.LambdaMethod(this, descriptor, methodBinding, enclosingBinding);
			} else {
				binding = new MethodBinding(this, methodBinding);
			}
			this.bindingTables.compilerBindingsToASTBindings.put(methodBinding, binding);
			return binding;
		}
		return null;
	}

	@Override
	synchronized IMemberValuePairBinding getMemberValuePairBinding(ElementValuePair valuePair) {
		if (valuePair == null || valuePair.binding == null) return null;
		IMemberValuePairBinding binding =
			(IMemberValuePairBinding) this.bindingTables.compilerBindingsToASTBindings.get(valuePair);
		if (binding != null)
			return binding;
		binding = new MemberValuePairBinding(valuePair, this);
		this.bindingTables.compilerBindingsToASTBindings.put(valuePair, binding);
		return binding;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.BindingResolver#getModuleBinding(org.eclipse.jdt.internal.compiler.lookup.ModuleBinding)
	 */
	@Override
	synchronized IModuleBinding getModuleBinding(org.eclipse.jdt.internal.compiler.lookup.ModuleBinding moduleBinding) {
		if (moduleBinding != null) {
			IModuleBinding binding = (IModuleBinding) this.bindingTables.compilerBindingsToASTBindings.get(moduleBinding);
			if (binding == null) {
				binding = new ModuleBinding(this, moduleBinding);
				this.bindingTables.compilerBindingsToASTBindings.put(moduleBinding, binding);
			}
			return binding;
		}
		return null;
	}

	@Override
	synchronized IPackageBinding getPackageBinding(org.eclipse.jdt.internal.compiler.lookup.PackageBinding packageBinding) {
		if (packageBinding == null || packageBinding instanceof ProblemPackageBinding) {
			return null;
		}
		IPackageBinding binding = (IPackageBinding) this.bindingTables.compilerBindingsToASTBindings.get(packageBinding);
		if (binding != null) {
			return binding;
		}
		binding = packageBinding instanceof ProblemPackageBinding ? new RecoveredPackageBinding(packageBinding, this) :
				new PackageBinding(packageBinding, this);
		this.bindingTables.compilerBindingsToASTBindings.put(packageBinding, binding);
		return binding;
	}
	private int getTypeCount(ParameterizedQualifiedTypeReference typeReference) {
		TypeReference[][] typeArguments = typeReference.typeArguments;
		int value = 0;
		org.eclipse.jdt.internal.compiler.ast.Annotation[][] typeAnnotations = typeReference.annotations;
		int length = typeReference.tokens.length;
		for (int i = 0; i < length; ++i) {
			if (value != 0 || (typeArguments != null && typeArguments[i] != null) ||
				(typeAnnotations != null && typeAnnotations[i] != null )) {
				value++;
			}
		}
		return value;
	}

	@Override
	synchronized ITypeBinding getTypeBinding(VariableDeclaration variableDeclaration) {
		ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(variableDeclaration);
		if (binding != null) {
			return binding;
		}
		binding = new RecoveredTypeBinding(this, variableDeclaration);
		this.bindingTables.compilerBindingsToASTBindings.put(variableDeclaration, binding);
		return binding;
	}

	@Override
	synchronized ITypeBinding getTypeBinding(Type type) {
		ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(type);
		if (binding != null) {
			return binding;
		}
		binding = new RecoveredTypeBinding(this, type);
		this.bindingTables.compilerBindingsToASTBindings.put(type, binding);
		return binding;
	}

	@Override
	synchronized ITypeBinding getTypeBinding(org.eclipse.jdt.internal.compiler.lookup.TypeBinding referenceBinding) {
		return internalGetTypeBinding(referenceBinding, null);
	}

	private synchronized ITypeBinding internalGetTypeBinding(org.eclipse.jdt.internal.compiler.lookup.TypeBinding referenceBinding, IBinding declaringMember) {
		// may also create an TypeBinding.AnonymousTypeBinding
		if (referenceBinding == null) {
			return null;
		} else if (!referenceBinding.isValidBinding()) {
			switch(referenceBinding.problemId()) {
				case ProblemReasons.NotVisible :
				case ProblemReasons.NonStaticReferenceInStaticContext :
					if (referenceBinding instanceof ProblemReferenceBinding) {
						ProblemReferenceBinding problemReferenceBinding = (ProblemReferenceBinding) referenceBinding;
						org.eclipse.jdt.internal.compiler.lookup.TypeBinding binding2 = problemReferenceBinding.closestMatch();
						ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(binding2);
						if (binding != null) {
							return binding;
						}
						binding = TypeBinding.createTypeBinding(this, binding2, declaringMember);
						this.bindingTables.compilerBindingsToASTBindings.put(binding2, binding);
						return binding;
					}
					break;
				case ProblemReasons.NotFound :
					if (!this.isRecoveringBindings) {
						return null;
					}
					ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(referenceBinding);
					if (binding != null) {
						return binding;
					}
					if ((referenceBinding.tagBits & TagBits.HasMissingType) != 0) {
						binding = TypeBinding.createTypeBinding(this, referenceBinding, declaringMember);
					} else {
						binding = new RecoveredTypeBinding(this, referenceBinding);
					}
					this.bindingTables.compilerBindingsToASTBindings.put(referenceBinding, binding);
					return binding;
			}
			return null;
		} else {
			if ((referenceBinding.tagBits & TagBits.HasMissingType) != 0 && !this.isRecoveringBindings) {
				return null;
			}
			ITypeBinding binding = (ITypeBinding) this.bindingTables.compilerBindingsToASTBindings.get(referenceBinding);
			if (binding != null) {
				return binding;
			}
			binding = TypeBinding.createTypeBinding(this, referenceBinding, declaringMember);
			this.bindingTables.compilerBindingsToASTBindings.put(referenceBinding, binding);
			return binding;
		}
	}

	@Override
	synchronized ITypeBinding getTypeBinding(RecoveredTypeBinding recoveredTypeBinding, int dimensions) {
		if (recoveredTypeBinding== null) {
			return null;
		}
		return new RecoveredTypeBinding(this, recoveredTypeBinding, dimensions);
	}

	synchronized IVariableBinding getVariableBinding(org.eclipse.jdt.internal.compiler.lookup.VariableBinding variableBinding, VariableDeclaration variableDeclaration) {
		if (this.isRecoveringBindings) {
			if (variableBinding != null) {
				if (variableBinding.isValidBinding()) {
					IVariableBinding binding = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(variableBinding);
					if (binding != null) {
						return binding;
					}
					if (variableBinding.type != null) {
						binding = new VariableBinding(this, variableBinding);
					} else {
						binding = new RecoveredVariableBinding(this, variableDeclaration);
					}
					this.bindingTables.compilerBindingsToASTBindings.put(variableBinding, binding);
					return binding;
				} else {
					/*
					 * http://dev.eclipse.org/bugs/show_bug.cgi?id=24449
					 */
					if (variableBinding instanceof ProblemFieldBinding) {
						ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) variableBinding;
						switch(problemFieldBinding.problemId()) {
							case ProblemReasons.NotVisible :
							case ProblemReasons.NonStaticReferenceInStaticContext :
							case ProblemReasons.NonStaticReferenceInConstructorInvocation :
								ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
								FieldBinding exactBinding = declaringClass.getField(problemFieldBinding.name, true /*resolve*/);
								if (exactBinding != null) {
									IVariableBinding variableBinding2 = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
									if (variableBinding2 != null) {
										return variableBinding2;
									}
									variableBinding2 = new VariableBinding(this, exactBinding);
									this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding2);
									return variableBinding2;
								}
								break;
						}
					}
				}
			}
			return null;
		}
		return this.getVariableBinding(variableBinding);
	}

	@Override
	public WorkingCopyOwner getWorkingCopyOwner() {
		return this.workingCopyOwner;
	}

	@Override
	synchronized IVariableBinding getVariableBinding(org.eclipse.jdt.internal.compiler.lookup.VariableBinding variableBinding) {
		if (variableBinding != null) {
			if (variableBinding.isValidBinding()) {
				org.eclipse.jdt.internal.compiler.lookup.TypeBinding variableType = variableBinding.type;
				if (variableType != null) {
					if (!this.isRecoveringBindings && ((variableType.tagBits & TagBits.HasMissingType) != 0)) {
						return null;
					}
					IVariableBinding binding = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(variableBinding);
					if (binding != null) {
						return binding;
					}
					binding = new VariableBinding(this, variableBinding);
					this.bindingTables.compilerBindingsToASTBindings.put(variableBinding, binding);
					return binding;
				}
			} else {
				/*
				 * http://dev.eclipse.org/bugs/show_bug.cgi?id=24449
				 */
				if (variableBinding instanceof ProblemFieldBinding) {
					ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) variableBinding;
					switch(problemFieldBinding.problemId()) {
						case ProblemReasons.NotVisible :
						case ProblemReasons.NonStaticReferenceInStaticContext :
						case ProblemReasons.NonStaticReferenceInConstructorInvocation :
							ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
							FieldBinding exactBinding = declaringClass.getField(problemFieldBinding.name, true /*resolve*/);
							if (exactBinding != null) {
								IVariableBinding variableBinding2 = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
								if (variableBinding2 != null) {
									return variableBinding2;
								}
								variableBinding2 = new VariableBinding(this, exactBinding);
								this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding2);
								return variableBinding2;
							}
							break;
					}
				}
			}
		}
		return null;
	}

	static class AnnotationIdentityBinding {
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding internalInstance;
		AnnotationIdentityBinding(org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding internalInstance) {
			this.internalInstance = internalInstance;
		}
		@Override
		public boolean equals(Object o) {
			return o instanceof AnnotationIdentityBinding && this.internalInstance == ((AnnotationIdentityBinding)o).internalInstance;
		}
		@Override
		public int hashCode() {
			return this.internalInstance.hashCode();
		}
	}

	@Override
	synchronized IAnnotationBinding getAnnotationInstance(org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding internalInstance) {
		if (internalInstance == null) return null;
		ReferenceBinding annotationType = internalInstance.getAnnotationType();
		if (!this.isRecoveringBindings) {
			if (annotationType == null || ((annotationType.tagBits & TagBits.HasMissingType) != 0)) {
				return null;
			}
		}
		Object key =  new AnnotationIdentityBinding(internalInstance);
		IAnnotationBinding newDomInstance = new AnnotationBinding(internalInstance, this);
		IAnnotationBinding domInstance = (IAnnotationBinding) ((ConcurrentHashMap)this.bindingTables.compilerAnnotationBindingsToASTBindings).putIfAbsent(key, newDomInstance);
		return domInstance != null ? domInstance : newDomInstance;
	}

	@Override
	boolean isResolvedTypeInferredFromExpectedType(MethodInvocation methodInvocation) {
		Object oldNode = this.newAstToOldAst.get(methodInvocation);
		if (oldNode instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) oldNode;
			org.eclipse.jdt.internal.compiler.lookup.MethodBinding methodBinding = messageSend.binding;
			if (methodBinding instanceof ParameterizedGenericMethodBinding) {
				ParameterizedGenericMethodBinding genericMethodBinding = (ParameterizedGenericMethodBinding) methodBinding;
				return genericMethodBinding.inferredReturnType;
			}
		}
		return false;
	}

	@Override
	boolean isResolvedTypeInferredFromExpectedType(SuperMethodInvocation superMethodInvocation) {
		Object oldNode = this.newAstToOldAst.get(superMethodInvocation);
		if (oldNode instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) oldNode;
			org.eclipse.jdt.internal.compiler.lookup.MethodBinding methodBinding = messageSend.binding;
			if (methodBinding instanceof ParameterizedGenericMethodBinding) {
				ParameterizedGenericMethodBinding genericMethodBinding = (ParameterizedGenericMethodBinding) methodBinding;
				return genericMethodBinding.inferredReturnType;
			}
		}
		return false;
	}

	@Override
	boolean isResolvedTypeInferredFromExpectedType(ClassInstanceCreation classInstanceCreation) {
		Object oldNode = this.newAstToOldAst.get(classInstanceCreation);
		if (oldNode instanceof AllocationExpression) {
			AllocationExpression allocationExpression = (AllocationExpression) oldNode;
			return allocationExpression.inferredReturnType;
		}
		return false;
	}

	@Override
	LookupEnvironment lookupEnvironment() {
		return this.scope.environment();
	}

	/**
	 * @see org.eclipse.jdt.core.dom.BindingResolver#recordScope(ASTNode, BlockScope)
	 */
	@Override
	synchronized void recordScope(ASTNode astNode, BlockScope blockScope) {
		this.astNodesToBlockScope.put(astNode, blockScope);
	}

	@Override
	boolean resolveBoxing(Expression expression) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.Expression &&
				((org.eclipse.jdt.internal.compiler.ast.Expression) node).isTrulyExpression()) {
			org.eclipse.jdt.internal.compiler.ast.Expression compilerExpression = (org.eclipse.jdt.internal.compiler.ast.Expression) node;
			return (compilerExpression.implicitConversion & TypeIds.BOXING) != 0;
		}
		return false;
	}

	@Override
	boolean resolveUnboxing(Expression expression) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.Expression) {
			org.eclipse.jdt.internal.compiler.ast.Expression compilerExpression = (org.eclipse.jdt.internal.compiler.ast.Expression) node;
			return (compilerExpression.implicitConversion & TypeIds.UNBOXING) != 0;
		}
		return false;
	}

	@Override
	Object resolveConstantExpressionValue(Expression expression) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if(node instanceof org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) {
				node = ((org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) node).initialization;
		}
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.Expression &&
				((org.eclipse.jdt.internal.compiler.ast.Expression) node).isTrulyExpression()) {
			org.eclipse.jdt.internal.compiler.ast.Expression compilerExpression = (org.eclipse.jdt.internal.compiler.ast.Expression) node;
			Constant constant = compilerExpression.constant;
			if (constant != null && constant != Constant.NotAConstant) {
				switch (constant.typeID()) {
					case TypeIds.T_int : return Integer.valueOf(constant.intValue());
					case TypeIds.T_byte : return Byte.valueOf(constant.byteValue());
					case TypeIds.T_short : return Short.valueOf(constant.shortValue());
					case TypeIds.T_char : return Character.valueOf(constant.charValue());
					case TypeIds.T_float : return Float.valueOf(constant.floatValue());
					case TypeIds.T_double : return Double.valueOf(constant.doubleValue());
					case TypeIds.T_boolean : return constant.booleanValue() ? Boolean.TRUE : Boolean.FALSE;
					case TypeIds.T_long : return Long.valueOf(constant.longValue());
					case TypeIds.T_JavaLangString : return constant.stringValue();
				}
				return null;
			}
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveConstructor(ClassInstanceCreation expression) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node != null && (node.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.IsAnonymousType) != 0) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration anonymousLocalTypeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			return getMethodBinding(anonymousLocalTypeDeclaration.allocation.binding);
		} else if (node instanceof AllocationExpression) {
			return getMethodBinding(((AllocationExpression)node).binding);
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveConstructor(ConstructorInvocation expression) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node instanceof ExplicitConstructorCall) {
			ExplicitConstructorCall explicitConstructorCall = (ExplicitConstructorCall) node;
			return getMethodBinding(explicitConstructorCall.binding);
		}
		return null;
	}

	@Override
	IMethodBinding resolveConstructor(EnumConstantDeclaration enumConstantDeclaration) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(enumConstantDeclaration);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) node;
			if (fieldDeclaration.getKind() == AbstractVariableDeclaration.ENUM_CONSTANT && fieldDeclaration.initialization != null) {
				AllocationExpression allocationExpression = (AllocationExpression) fieldDeclaration.initialization;
				return getMethodBinding(allocationExpression.binding);
			}
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveConstructor(SuperConstructorInvocation expression) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
		if (node instanceof ExplicitConstructorCall) {
			ExplicitConstructorCall explicitConstructorCall = (ExplicitConstructorCall) node;
			return getMethodBinding(explicitConstructorCall.binding);
		}
		return null;
	}

	@Override
	synchronized ITypeBinding resolveExpressionType(Expression expression) {
		try {
			switch(expression.getNodeType()) {
				case ASTNode.CLASS_INSTANCE_CREATION :
					org.eclipse.jdt.internal.compiler.ast.ASTNode astNode = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(expression);
					if (astNode instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
						// anonymous type case
						org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) astNode;
						ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
						if (typeBinding != null) {
							return typeBinding;
						}
					} else if (astNode instanceof AllocationExpression) {
						// should be an AllocationExpression
						AllocationExpression allocationExpression = (AllocationExpression) astNode;
						return this.getTypeBinding(allocationExpression.resolvedType);
					}
					break;
				case ASTNode.SIMPLE_NAME :
				case ASTNode.QUALIFIED_NAME :
					return resolveTypeBindingForName((Name) expression);
				case ASTNode.ARRAY_INITIALIZER :
				case ASTNode.ARRAY_CREATION :
				case ASTNode.ASSIGNMENT :
				case ASTNode.POSTFIX_EXPRESSION :
				case ASTNode.PREFIX_EXPRESSION :
				case ASTNode.CAST_EXPRESSION :
				case ASTNode.TYPE_LITERAL :
				case ASTNode.INFIX_EXPRESSION :
				case ASTNode.INSTANCEOF_EXPRESSION :
				case ASTNode.LAMBDA_EXPRESSION:
				case ASTNode.CREATION_REFERENCE:
				case ASTNode.EXPRESSION_METHOD_REFERENCE:
				case ASTNode.TYPE_METHOD_REFERENCE:
				case ASTNode.SUPER_METHOD_REFERENCE :
				case ASTNode.FIELD_ACCESS :
				case ASTNode.SUPER_FIELD_ACCESS :
				case ASTNode.ARRAY_ACCESS :
				case ASTNode.METHOD_INVOCATION :
				case ASTNode.SUPER_METHOD_INVOCATION :
				case ASTNode.CONDITIONAL_EXPRESSION :
				case ASTNode.SWITCH_EXPRESSION :
				case ASTNode.MARKER_ANNOTATION :
				case ASTNode.NORMAL_ANNOTATION :
				case ASTNode.SINGLE_MEMBER_ANNOTATION :
				case ASTNode.GUARDED_PATTERN :
				case ASTNode.TYPE_PATTERN :
					org.eclipse.jdt.internal.compiler.ast.Expression compilerExpression = (org.eclipse.jdt.internal.compiler.ast.Expression) this.newAstToOldAst.get(expression);
					if (compilerExpression != null) {
						return this.getTypeBinding(compilerExpression.resolvedType);
					}
					break;
				case ASTNode.TEXT_BLOCK :
				case ASTNode.STRING_LITERAL :
					if (this.scope != null) {
						return this.getTypeBinding(this.scope.getJavaLangString());
					}
					break;
				case ASTNode.NULL_PATTERN :
					return null;
				case ASTNode.BOOLEAN_LITERAL :
				case ASTNode.NULL_LITERAL :
				case ASTNode.CHARACTER_LITERAL :
				case ASTNode.NUMBER_LITERAL :
					Literal literal = (Literal) this.newAstToOldAst.get(expression);
					if (literal != null) {
						return this.getTypeBinding(literal.literalType(null));
					}
					break;
				case ASTNode.THIS_EXPRESSION :
					ThisReference thisReference = (ThisReference) this.newAstToOldAst.get(expression);
					BlockScope blockScope = (BlockScope) this.astNodesToBlockScope.get(expression);
					if (blockScope != null) {
						return this.getTypeBinding(thisReference.resolveType(blockScope));
					}
					break;
				case ASTNode.PARENTHESIZED_EXPRESSION :
					ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
					return resolveExpressionType(parenthesizedExpression.getExpression());
				case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
					VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) expression;
					Type type = variableDeclarationExpression.getType();
					if (type != null) {
						return type.resolveBinding();
					}
					break;
			}
		} catch (AbortCompilation e) {
			// handle missing types
		}
		return null;
	}

	@Override
	synchronized IVariableBinding resolveField(FieldAccess fieldAccess) {
		Object oldNode = this.newAstToOldAst.get(fieldAccess);
		if (oldNode instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) oldNode;
			return this.getVariableBinding(fieldReference.binding);
		}
		return null;
	}

	@Override
	synchronized IVariableBinding resolveField(SuperFieldAccess fieldAccess) {
		Object oldNode = this.newAstToOldAst.get(fieldAccess);
		if (oldNode instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) oldNode;
			return this.getVariableBinding(fieldReference.binding);
		}
		return null;
	}

	@Override
	synchronized IBinding resolveImport(ImportDeclaration importDeclaration) {
		if (this.scope == null) return null;
		try {
			org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(importDeclaration);
			if (node instanceof ImportReference) {
				ImportReference importReference = (ImportReference) node;
				final boolean isStatic = importReference.isStatic();
				if ((importReference.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.OnDemand) != 0) {
					Binding binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, importReference.tokens.length), true, isStatic);
					if (binding != null) {
						if (isStatic) {
							if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
								ITypeBinding typeBinding = this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding) binding);
								return typeBinding == null ? null : typeBinding;
							}
						} else {
							if ((binding.kind() & Binding.PACKAGE) != 0) {
								IPackageBinding packageBinding = getPackageBinding((org.eclipse.jdt.internal.compiler.lookup.PackageBinding) binding);
								if (packageBinding == null) {
									return null;
								}
								return packageBinding;
							} else {
								// if it is not a package, it has to be a type
								ITypeBinding typeBinding = this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding) binding);
								if (typeBinding == null) {
									return null;
								}
								return typeBinding;
							}
						}
					}
				} else {
					Binding binding = this.scope.getImport(importReference.tokens, false, isStatic);
					if (binding != null) {
						if (isStatic) {
							if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
								ITypeBinding typeBinding = this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding) binding);
								return typeBinding == null ? null : typeBinding;
							} else if (binding instanceof FieldBinding) {
								IVariableBinding variableBinding = this.getVariableBinding((FieldBinding) binding);
								return variableBinding == null ? null : variableBinding;
							} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.MethodBinding) {
								// it is a type
								return getMethodBinding((org.eclipse.jdt.internal.compiler.lookup.MethodBinding)binding);
							} else if (binding instanceof RecordComponentBinding) {
								IVariableBinding variableBinding = this.getVariableBinding((RecordComponentBinding) binding);
								return variableBinding == null ? null : variableBinding;
							}
						} else {
							if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
								ITypeBinding typeBinding = this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding) binding);
								return typeBinding == null ? null : typeBinding;
							}
						}
					}
				}
			}
		} catch(AbortCompilation e) {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
		}
		return null;
	}

	@Override
	IMethodBinding resolveMember(AnnotationTypeMemberDeclaration declaration) {
		Object oldNode = this.newAstToOldAst.get(declaration);
		if (oldNode instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) oldNode;
			IMethodBinding methodBinding = getMethodBinding(methodDeclaration.binding);
			if (methodBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(methodBinding, declaration);
			String key = methodBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, methodBinding);
			}
			return methodBinding;
		}
		return null;
	}

	private IVariableBinding[] getSyntheticOuterLocalVariables(org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression) {
		IVariableBinding[] syntheticOuterLocals = new IVariableBinding[lambdaExpression.outerLocalVariables.length];
		int i  = 0;
		for (SyntheticArgumentBinding sab : lambdaExpression.outerLocalVariables) {
			syntheticOuterLocals[i++] = getVariableBinding(sab);
		}
		return syntheticOuterLocals;
	}
	@Override
	synchronized IMethodBinding resolveMethod(LambdaExpression lambda) {
		Object oldNode = this.newAstToOldAst.get(lambda);
		if (oldNode instanceof org.eclipse.jdt.internal.compiler.ast.LambdaExpression) {
			org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression = (org.eclipse.jdt.internal.compiler.ast.LambdaExpression) oldNode;
			IMethodBinding methodBinding = null;
			if (lambdaExpression.descriptor != null) {
				IBinding declaringMember = getDeclaringMember(lambdaExpression, lambdaExpression.enclosingScope);
				if (declaringMember != null)
					methodBinding = getMethodOrLambdaBinding(lambdaExpression.getMethodBinding(), lambdaExpression.descriptor, declaringMember);
			}
			if (methodBinding == null) {
				return null;
			}
			if (methodBinding instanceof LambdaMethod) {
				((LambdaMethod) methodBinding).setSyntheticOuterLocals(getSyntheticOuterLocalVariables(lambdaExpression));
			}
			this.bindingsToAstNodes.put(methodBinding, lambda);
			String key = methodBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, methodBinding);
			}
			return methodBinding;
		}
		return null;
	}

	private IBinding getDeclaringMember(org.eclipse.jdt.internal.compiler.ast.ASTNode node, Scope currentScope) {
		MethodScope methodScope = currentScope != null ? currentScope.methodScope() : null;
		if (methodScope != null) {
			if (methodScope.isInsideInitializer()) {
				org.eclipse.jdt.internal.compiler.ast.TypeDeclaration enclosingType = methodScope.referenceType();
				if (enclosingType.fields != null) {
					for (int i = 0; i < enclosingType.fields.length; i++) {
						FieldDeclaration field = enclosingType.fields[i];
						if (field.declarationSourceStart <= node.sourceStart && node.sourceEnd <= field.declarationSourceEnd) {
							if (field instanceof org.eclipse.jdt.internal.compiler.ast.Initializer)
								return getMethodBinding(((org.eclipse.jdt.internal.compiler.ast.Initializer) field).getMethodBinding());
							else
								return getVariableBinding(field.binding);
						}
					}
				}
			} else {
				if (methodScope.isLambdaScope()) {
					org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression = (org.eclipse.jdt.internal.compiler.ast.LambdaExpression) methodScope.referenceContext;
					IMethodBinding methodBinding = null;
					if (lambdaExpression.descriptor != null) {
						IBinding declaringMember = getDeclaringMember(lambdaExpression, lambdaExpression.enclosingScope);
						if (declaringMember != null)
							methodBinding = getMethodOrLambdaBinding(lambdaExpression.getMethodBinding(), lambdaExpression.descriptor, declaringMember);
					}
					if (methodBinding == null) {
						return null;
					}
					String key = methodBinding.getKey();
					if (key != null) {
						this.bindingTables.bindingKeysToBindings.put(key, methodBinding);
					}
					return methodBinding;
				} else {
					return getMethodBinding(methodScope.referenceMethodBinding());
				}
			}
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveMethod(MethodDeclaration method) {
		Object oldNode = this.newAstToOldAst.get(method);
		if (oldNode instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) oldNode;
			IMethodBinding methodBinding = getMethodBinding(methodDeclaration.binding);
			if (methodBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(methodBinding, method);
			String key = methodBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, methodBinding);
			}
			return methodBinding;
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveMethod(MethodInvocation method) {
		Object oldNode = this.newAstToOldAst.get(method);
		if (oldNode instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) oldNode;
			return getMethodBinding(messageSend.binding);
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveMethod(MethodReference methodReference) {
		Object oldNode = this.newAstToOldAst.get(methodReference);
		if (oldNode instanceof org.eclipse.jdt.internal.compiler.ast.ReferenceExpression) {
			org.eclipse.jdt.internal.compiler.ast.ReferenceExpression referenceExpression = (org.eclipse.jdt.internal.compiler.ast.ReferenceExpression) oldNode;
			if (referenceExpression.receiverType != null && referenceExpression.receiverType.isArrayType())
				return null;
			IMethodBinding methodBinding = getMethodBinding(referenceExpression.getMethodBinding());
			if (methodBinding == null) {
				return null;
			}
			return methodBinding;
		}
		return null;
	}

	@Override
	synchronized IMethodBinding resolveMethod(SuperMethodInvocation method) {
		Object oldNode = this.newAstToOldAst.get(method);
		if (oldNode instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) oldNode;
			return getMethodBinding(messageSend.binding);
		}
		return null;
	}

	synchronized ITypeBinding resolveTypeBindingForName(Name name) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(name);
		int index = name.index;
		if (node instanceof QualifiedNameReference) {
			QualifiedNameReference qualifiedNameReference = (QualifiedNameReference) node;
			final char[][] tokens = qualifiedNameReference.tokens;
			if (tokens.length == index) {
				return this.getTypeBinding(qualifiedNameReference.resolvedType);
			}
			int indexOfFirstFieldBinding = qualifiedNameReference.indexOfFirstFieldBinding; // one-based
			if (index < indexOfFirstFieldBinding) {
				// an extra lookup is required
				BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
				Binding binding = null;
				try {
					if (internalScope == null) {
						if (this.scope == null) return null;
						binding = this.scope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					} else {
						binding = internalScope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					}
				} catch (AbortCompilation e) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
				}
				if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.PackageBinding) {
					return null;
				} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
					// it is a type
					return this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding)binding);
				}
			} else if (index == indexOfFirstFieldBinding) {
				if (qualifiedNameReference.isTypeReference()) {
					return this.getTypeBinding(qualifiedNameReference.resolvedType);
				} else {
					// in this case we want to get the next field declaring's class
					if (qualifiedNameReference.otherBindings == null) {
						return null;
					}
					FieldBinding fieldBinding = qualifiedNameReference.otherBindings[0];
					if (fieldBinding == null) return null;
					org.eclipse.jdt.internal.compiler.lookup.TypeBinding type = fieldBinding.declaringClass;
					if (type == null) { // array length scenario
						// use type from first binding (no capture needed for array type)
						switch (qualifiedNameReference.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.RestrictiveFlagMASK) {
							case Binding.FIELD:
								type = ((FieldBinding) qualifiedNameReference.binding).type;
								break;
							case Binding.LOCAL:
								type = ((LocalVariableBinding) qualifiedNameReference.binding).type;
								break;
							case Binding.RECORD_COMPONENT:
								type = ((RecordComponentBinding) qualifiedNameReference.binding).type;
								break;
						}
					}
					return this.getTypeBinding(type);
				}
			} else {
				/* This is the case for a name which is part of a qualified name that
				 * cannot be resolved. See PR 13063.
				 */
				if (qualifiedNameReference.otherBindings == null) return null;
				final int otherBindingsLength = qualifiedNameReference.otherBindings.length;
				if (otherBindingsLength == (index - indexOfFirstFieldBinding)) {
					return this.getTypeBinding(qualifiedNameReference.resolvedType);
				}
				FieldBinding fieldBinding = qualifiedNameReference.otherBindings[index - indexOfFirstFieldBinding];
				if (fieldBinding == null) return null;
				org.eclipse.jdt.internal.compiler.lookup.TypeBinding type = fieldBinding.declaringClass;
				if (type == null) { // array length scenario
					// use type from previous binding (no capture needed for array type)
					fieldBinding = qualifiedNameReference.otherBindings[index - indexOfFirstFieldBinding - 1];
					if (fieldBinding == null) return null;
					type = fieldBinding.type;
				}
				return this.getTypeBinding(type);
			}
		} else if (node instanceof QualifiedTypeReference) {
			QualifiedTypeReference qualifiedTypeReference = (QualifiedTypeReference) node;
			if (qualifiedTypeReference.resolvedType == null) {
				return null;
			}
			if (index == qualifiedTypeReference.tokens.length) {
				if (!qualifiedTypeReference.resolvedType.isValidBinding() && qualifiedTypeReference instanceof JavadocQualifiedTypeReference) {
					JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) node;
					if (typeRef.packageBinding != null) {
						return null;
					}
				}
				return this.getTypeBinding(qualifiedTypeReference.resolvedType.leafComponentType());
			} else {
				if (index >= 0) {
					BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
					Binding binding = null;
					try {
						if (internalScope == null) {
							if (this.scope == null) return null;
							binding = this.scope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						} else {
							binding = internalScope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						}
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
					if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.PackageBinding) {
						return null;
					} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding)binding);
					} else {
						return null;
					}
				}
			}
		} else if (node instanceof ImportReference) {
			if ((node.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.inModule) != 0)
				return null;
			ImportReference importReference = (ImportReference) node;
			int importReferenceLength = importReference.tokens.length;
			if (index >= 0) {
				Binding binding = null;
				if (this.scope == null) return null;
				if (importReferenceLength == index) {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), (importReference.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.OnDemand) != 0, importReference.isStatic());
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				} else {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), true, importReference.isStatic());
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				}
				if (binding != null) {
					if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding)binding);
					}
					return null;
				}
			}
		} else if (node instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) node;
			IMethodBinding method = getMethodBinding(methodDeclaration.binding);
			if (method == null) return null;
			return method.getReturnType();
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding != null) {
				return typeBinding;
			}
		} if (node instanceof JavadocSingleNameReference) {
			JavadocSingleNameReference singleNameReference = (JavadocSingleNameReference) node;
			org.eclipse.jdt.internal.compiler.lookup.VariableBinding localVariable = (org.eclipse.jdt.internal.compiler.lookup.VariableBinding)singleNameReference.binding;
			if (localVariable != null) {
				return this.getTypeBinding(localVariable.type);
			}
		} if (node instanceof SingleNameReference) {
			SingleNameReference singleNameReference = (SingleNameReference) node;
			return this.getTypeBinding(singleNameReference.resolvedType);
		} else if (node instanceof QualifiedSuperReference) {
			QualifiedSuperReference qualifiedSuperReference = (QualifiedSuperReference) node;
			return this.getTypeBinding(qualifiedSuperReference.qualification.resolvedType);
		} else if (node instanceof Receiver) {
			org.eclipse.jdt.internal.compiler.lookup.TypeBinding receiver = ((Receiver) node).type.resolvedType;
			return this.getTypeBinding(receiver);
		} else if (node instanceof LocalDeclaration) {
			IVariableBinding variable = this.getVariableBinding(((LocalDeclaration)node).binding);
			if (variable == null) return null;
			return variable.getType();
		} else if (node instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) node;
			if (fieldRef.methodBinding != null) {
				return getMethodBinding(fieldRef.methodBinding).getReturnType();
			}
			return getTypeBinding(fieldRef.resolvedType);
		} else if (node instanceof FieldReference) {
			return getTypeBinding(((FieldReference) node).resolvedType);
		} else if (node instanceof SingleTypeReference) {
			SingleTypeReference singleTypeReference = (SingleTypeReference) node;
			org.eclipse.jdt.internal.compiler.lookup.TypeBinding binding = singleTypeReference.resolvedType;
			if (binding != null) {
				return this.getTypeBinding(binding.leafComponentType());
			}
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) node;
			IVariableBinding field = this.getVariableBinding(fieldDeclaration.binding);
			if (field == null) return null;
			return field.getType();
		} else if (node instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) node;
			IMethodBinding method = getMethodBinding(messageSend.binding);
			if (method == null) return null;
			return method.getReturnType();
		} else if (node instanceof AllocationExpression) {
			AllocationExpression allocation = (AllocationExpression) node;
			return getTypeBinding(allocation.resolvedType);
		} else if (node instanceof JavadocImplicitTypeReference) {
			JavadocImplicitTypeReference implicitRef = (JavadocImplicitTypeReference) node;
			return getTypeBinding(implicitRef.resolvedType);
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeParameter) {
			org.eclipse.jdt.internal.compiler.ast.TypeParameter typeParameter = (org.eclipse.jdt.internal.compiler.ast.TypeParameter) node;
			return this.getTypeBinding(typeParameter.binding);
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.MemberValuePair) {
			org.eclipse.jdt.internal.compiler.ast.MemberValuePair memberValuePair = (org.eclipse.jdt.internal.compiler.ast.MemberValuePair) node;
			IMethodBinding method = getMethodBinding(memberValuePair.binding);
			if (method == null) return null;
			return method.getReturnType();
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.ReferenceExpression) {
			org.eclipse.jdt.internal.compiler.ast.ReferenceExpression referenceExpression = (org.eclipse.jdt.internal.compiler.ast.ReferenceExpression) node;
			IMethodBinding method = getMethodBinding(referenceExpression.getMethodBinding());
			if (method == null) return null;
			return method.getReturnType();
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.RecordComponent) {
			org.eclipse.jdt.internal.compiler.ast.RecordComponent recordComponent = (org.eclipse.jdt.internal.compiler.ast.RecordComponent) node;
			org.eclipse.jdt.internal.compiler.lookup.TypeBinding recordComponentType = recordComponent.type.resolvedType;
			return this.getTypeBinding(recordComponentType);
		}
		return null;
	}

	@Override
	synchronized IBinding resolveName(Name name) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(name);
		int index = name.index;
		if (node instanceof QualifiedNameReference) {
			QualifiedNameReference qualifiedNameReference = (QualifiedNameReference) node;
			final char[][] tokens = qualifiedNameReference.tokens;
			int indexOfFirstFieldBinding = qualifiedNameReference.indexOfFirstFieldBinding; // one-based
			if (index < indexOfFirstFieldBinding) {
				// an extra lookup is required
				BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
				Binding binding = null;
				try {
					if (internalScope == null) {
						if (this.scope == null) return null;
						binding = this.scope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					} else {
						binding = internalScope.getTypeOrPackage(CharOperation.subarray(tokens, 0, index));
					}
				} catch (AbortCompilation e) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
				}
				if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.PackageBinding) {
					return getPackageBinding((org.eclipse.jdt.internal.compiler.lookup.PackageBinding)binding);
				} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
					// it is a type
					return this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding)binding);
				}
			} else if (index == indexOfFirstFieldBinding) {
				if (qualifiedNameReference.isTypeReference()) {
					return this.getTypeBinding(qualifiedNameReference.resolvedType);
				} else {
					Binding binding = qualifiedNameReference.binding;
					if (binding != null) {
						if (binding.isValidBinding()) {
							return this.getVariableBinding((org.eclipse.jdt.internal.compiler.lookup.VariableBinding) binding);
						} else  if (binding instanceof ProblemFieldBinding) {
							ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) binding;
							switch(problemFieldBinding.problemId()) {
								case ProblemReasons.NotVisible :
								case ProblemReasons.NonStaticReferenceInStaticContext :
									ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
									if (declaringClass != null) {
										FieldBinding exactBinding = declaringClass.getField(tokens[tokens.length - 1], true /*resolve*/);
										if (exactBinding != null) {
											if (exactBinding.type != null) {
												IVariableBinding variableBinding = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
												if (variableBinding != null) {
													return variableBinding;
												}
												variableBinding = new VariableBinding(this, exactBinding);
												this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding);
												return variableBinding;
											}
										}
									}
									break;
							}
						}
					}
				}
			} else {
				/* This is the case for a name which is part of a qualified name that
				 * cannot be resolved. See PR 13063.
				 */
				if (qualifiedNameReference.otherBindings == null || (index - indexOfFirstFieldBinding - 1) < 0) {
					return null;
				} else {
					return this.getVariableBinding(qualifiedNameReference.otherBindings[index - indexOfFirstFieldBinding - 1]);
				}
			}
		} else if (node instanceof JavadocModuleReference)  {
			JavadocModuleReference modRef = (JavadocModuleReference) node;
			if (modRef.typeReference == null) {
				ModuleReference moduleReference = modRef.moduleReference;
				IModuleBinding moduleBinding = getModuleBinding(moduleReference.binding);
				if (moduleBinding != null) {
					return moduleBinding;
				}
			} else {
				if (name instanceof ModuleQualifiedName) {
					return resolveName(((ModuleQualifiedName)name).getName());
				}
			}
		} else if (node instanceof QualifiedTypeReference) {
			QualifiedTypeReference qualifiedTypeReference = (QualifiedTypeReference) node;
			if (qualifiedTypeReference.resolvedType == null) {
				return null;
			}
			if (index == qualifiedTypeReference.tokens.length) {
				if (!qualifiedTypeReference.resolvedType.isValidBinding() && qualifiedTypeReference instanceof JavadocQualifiedTypeReference) {
					JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) node;
					if (typeRef.packageBinding != null) {
						return getPackageBinding(typeRef.packageBinding);
					}
				}
				return this.getTypeBinding(qualifiedTypeReference.resolvedType.leafComponentType());
			} else {
				if (index >= 0) {
					BlockScope internalScope = (BlockScope) this.astNodesToBlockScope.get(name);
					Binding binding = null;
					try {
						if (internalScope == null) {
							if (this.scope == null) return null;
							binding = this.scope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						} else {
							binding = internalScope.getTypeOrPackage(CharOperation.subarray(qualifiedTypeReference.tokens, 0, index));
						}
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
					if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.PackageBinding) {
						return getPackageBinding((org.eclipse.jdt.internal.compiler.lookup.PackageBinding)binding);
					} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding)binding);
					} else {
						return null;
					}
				}
			}
		} else if (node instanceof ImportReference) {
			ImportReference importReference = (ImportReference) node;
			int importReferenceLength = importReference.tokens.length;
			boolean inModule = (importReference.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.inModule) != 0;
			if (index >= 0) {
				Binding binding = null;
				if (this.scope == null) return null;
				if (importReferenceLength == index && !inModule) {
					try {
						binding = this.scope.getImport(CharOperation.subarray(importReference.tokens, 0, index), (importReference.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.OnDemand) != 0, importReference.isStatic());
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				} else {
					try {
						binding = this.scope.getImport(inModule ? importReference.tokens : CharOperation.subarray(importReference.tokens, 0, index), true, importReference.isStatic());
					} catch (AbortCompilation e) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
					}
				}
				if (binding != null) {
					if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.PackageBinding) {
						return getPackageBinding((org.eclipse.jdt.internal.compiler.lookup.PackageBinding)binding);
					} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
						// it is a type
						return this.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding)binding);
					} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.FieldBinding) {
						// it is a type
						return this.getVariableBinding((org.eclipse.jdt.internal.compiler.lookup.FieldBinding)binding);
					} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.MethodBinding) {
						// it is a type
						return getMethodBinding((org.eclipse.jdt.internal.compiler.lookup.MethodBinding)binding);
					} else if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.RecordComponentBinding) {
						// it is a type
						return this.getVariableBinding((org.eclipse.jdt.internal.compiler.lookup.RecordComponentBinding)binding);
					} else {
						return null;
					}
				}
			}
		} else if (node instanceof CompilationUnitDeclaration) {
			CompilationUnitDeclaration compilationUnitDeclaration = (CompilationUnitDeclaration) node;
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
			if (types == null || types.length == 0) {
				return null;
			}
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration type = types[0];
			if (type != null) {
				ITypeBinding typeBinding = this.getTypeBinding(type.binding);
				if (typeBinding != null) {
					return typeBinding.getPackage();
				}
			}
		} else if (node instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) node;
			IMethodBinding methodBinding = getMethodBinding(methodDeclaration.binding);
			if (methodBinding != null) {
				return methodBinding;
			}
		} else if (node instanceof ModuleReference) {
			ModuleReference moduleReference = (ModuleReference) node;
			IModuleBinding moduleBinding = getModuleBinding(moduleReference.binding);
			if (moduleBinding != null) {
				return moduleBinding;
			}
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration moduleDeclaration = (org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration) node;
			IModuleBinding moduleBinding = getModuleBinding(moduleDeclaration.binding);
			if (moduleBinding != null) {
				return moduleBinding;
			}
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding != null) {
				return typeBinding;
			}
		} if (node instanceof SingleNameReference) {
			SingleNameReference singleNameReference = (SingleNameReference) node;
			if (singleNameReference.isTypeReference()) {
				return this.getTypeBinding(singleNameReference.resolvedType);
			} else {
				// this is a variable or a field
				Binding binding = singleNameReference.binding;
				if (binding != null) {
					if (binding.isValidBinding()) {
						return this.getVariableBinding((org.eclipse.jdt.internal.compiler.lookup.VariableBinding) binding);
					} else {
						/*
						 * http://dev.eclipse.org/bugs/show_bug.cgi?id=24449
						 */
						if (binding instanceof ProblemFieldBinding) {
							ProblemFieldBinding problemFieldBinding = (ProblemFieldBinding) binding;
							switch(problemFieldBinding.problemId()) {
								case ProblemReasons.NotVisible :
								case ProblemReasons.NonStaticReferenceInStaticContext :
								case ProblemReasons.NonStaticReferenceInConstructorInvocation :
									ReferenceBinding declaringClass = problemFieldBinding.declaringClass;
									FieldBinding exactBinding = declaringClass.getField(problemFieldBinding.name, true /*resolve*/);
									if (exactBinding != null) {
										if (exactBinding.type != null) {
											IVariableBinding variableBinding2 = (IVariableBinding) this.bindingTables.compilerBindingsToASTBindings.get(exactBinding);
											if (variableBinding2 != null) {
												return variableBinding2;
											}
											variableBinding2 = new VariableBinding(this, exactBinding);
											this.bindingTables.compilerBindingsToASTBindings.put(exactBinding, variableBinding2);
											return variableBinding2;
										}
									}
									break;
							}
						}
					}
				}
			}
		} else if (node instanceof QualifiedSuperReference) {
			QualifiedSuperReference qualifiedSuperReference = (QualifiedSuperReference) node;
			return this.getTypeBinding(qualifiedSuperReference.qualification.resolvedType);
		} else if (node instanceof LocalDeclaration) {
			return name.getAST().apiLevel() >= AST.JLS10_INTERNAL && name instanceof SimpleName && ((SimpleName) name).isVar()  ?
					resolveTypeBindingForName(name) :
					this.getVariableBinding(((LocalDeclaration)node).binding);
		} else if (node instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) node;
			if (fieldRef.methodBinding != null) {
				return getMethodBinding(fieldRef.methodBinding);
			}
			return getVariableBinding(fieldRef.binding);
		} else if (node instanceof FieldReference) {
			return getVariableBinding(((FieldReference) node).binding);
		} else if (node instanceof SingleTypeReference) {
			if (node instanceof JavadocSingleTypeReference) {
				JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) node;
				if (typeRef.packageBinding != null) {
					return getPackageBinding(typeRef.packageBinding);
				}
			}
			SingleTypeReference singleTypeReference = (SingleTypeReference) node;
			org.eclipse.jdt.internal.compiler.lookup.TypeBinding binding = singleTypeReference.resolvedType;
			if (binding == null) {
				return null;
			}
			return this.getTypeBinding(binding.leafComponentType());
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) node;
			return this.getVariableBinding(fieldDeclaration.binding);
		} else if (node instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) node;
			return getMethodBinding(messageSend.binding);
		} else if (node instanceof AllocationExpression) {
			AllocationExpression allocation = (AllocationExpression) node;
			return getMethodBinding(allocation.binding);
		} else if (node instanceof JavadocImplicitTypeReference) {
			JavadocImplicitTypeReference implicitRef = (JavadocImplicitTypeReference) node;
			return getTypeBinding(implicitRef.resolvedType);
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeParameter) {
			org.eclipse.jdt.internal.compiler.ast.TypeParameter typeParameter = (org.eclipse.jdt.internal.compiler.ast.TypeParameter) node;
			return this.getTypeBinding(typeParameter.binding);
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.MemberValuePair) {
			org.eclipse.jdt.internal.compiler.ast.MemberValuePair memberValuePair = (org.eclipse.jdt.internal.compiler.ast.MemberValuePair) node;
			return getMethodBinding(memberValuePair.binding);
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.ReferenceExpression) {
			org.eclipse.jdt.internal.compiler.ast.ReferenceExpression referenceExpression = (org.eclipse.jdt.internal.compiler.ast.ReferenceExpression) node;
			return getMethodBinding(referenceExpression.getMethodBinding());
		} else if (node instanceof org.eclipse.jdt.internal.compiler.ast.RecordComponent) {
			org.eclipse.jdt.internal.compiler.ast.RecordComponent recordComponent = (org.eclipse.jdt.internal.compiler.ast.RecordComponent) node;
			return this.getVariableBinding(recordComponent.binding);
		}
		return null;
	}

	@Override
	synchronized IPackageBinding resolvePackage(PackageDeclaration pkg) {
		if (this.scope == null) return null;
		try {
			org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(pkg);
			if (node instanceof ImportReference) {
				ImportReference importReference = (ImportReference) node;
				Binding binding = this.scope.getOnlyPackage(CharOperation.subarray(importReference.tokens, 0, importReference.tokens.length));
				if ((binding != null) && (binding.isValidBinding())) {
					if (binding instanceof ReferenceBinding) {
						// this only happens if a type name has the same name as its package
						ReferenceBinding referenceBinding = (ReferenceBinding) binding;
						binding = referenceBinding.fPackage;
					}
					if (binding instanceof org.eclipse.jdt.internal.compiler.lookup.PackageBinding) {
						IPackageBinding packageBinding = getPackageBinding((org.eclipse.jdt.internal.compiler.lookup.PackageBinding) binding);
						if (packageBinding == null) {
							return null;
						}
						this.bindingsToAstNodes.put(packageBinding, pkg);
						String key = packageBinding.getKey();
						if (key != null) {
							this.bindingTables.bindingKeysToBindings.put(key, packageBinding);
						}
						return packageBinding;
					}
				}
			}
		} catch (AbortCompilation e) {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53357
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
		}
		return null;
	}

	@Override
	synchronized IBinding resolveReference(MemberRef ref) {
		org.eclipse.jdt.internal.compiler.ast.Expression expression = (org.eclipse.jdt.internal.compiler.ast.Expression) this.newAstToOldAst.get(ref);
		if (expression instanceof TypeReference) {
			return getTypeBinding(expression.resolvedType);
		} else if (expression instanceof JavadocFieldReference) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) expression;
			if (fieldRef.methodBinding != null) {
				return getMethodBinding(fieldRef.methodBinding);
			}
			return getVariableBinding(fieldRef.binding);
		}
		return null;
	}

	@Override
	synchronized IMemberValuePairBinding resolveMemberValuePair(org.eclipse.jdt.core.dom.MemberValuePair memberValuePair) {
		MemberValuePair valuePair = (MemberValuePair) this.newAstToOldAst.get(memberValuePair);
		if (valuePair != null) {
			return getMemberValuePairBinding(valuePair.compilerElementPair);
		}
		return null;
	}

	/**
	 * @see BindingResolver#resolveModule(ModuleDeclaration)
	 * @since 3.14
	 */
	@Override
	IModuleBinding resolveModule(ModuleDeclaration module) {
		Object oldNode = this.newAstToOldAst.get(module);
		if (oldNode instanceof org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration moduleDeclaration = (org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration) oldNode;
			IModuleBinding moduleBinding = getModuleBinding(moduleDeclaration.binding);
			if (moduleBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(moduleBinding, module);
			String key = moduleBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, moduleBinding);
			}
			return moduleBinding;
		}

		return null;
	}

	@Override
	synchronized IBinding resolveReference(MethodRef ref) {
		org.eclipse.jdt.internal.compiler.ast.Expression expression = (org.eclipse.jdt.internal.compiler.ast.Expression) this.newAstToOldAst.get(ref);
		if (expression instanceof JavadocMessageSend) {
			return getMethodBinding(((JavadocMessageSend)expression).binding);
		}
		else if (expression instanceof JavadocAllocationExpression) {
			return getMethodBinding(((JavadocAllocationExpression)expression).binding);
		}
		return null;
	}

	@Override
	ITypeBinding resolveType(AnnotationTypeDeclaration type) {
		final Object node = this.newAstToOldAst.get(type);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	@Override
	synchronized ITypeBinding resolveType(AnonymousClassDeclaration type) {
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(type);
		if (node != null && (node.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.IsAnonymousType) != 0) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration anonymousLocalTypeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			IBinding declaringMember = getDeclaringMember(anonymousLocalTypeDeclaration, anonymousLocalTypeDeclaration.scope);
			ITypeBinding typeBinding = internalGetTypeBinding(anonymousLocalTypeDeclaration.binding, declaringMember);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	@Override
	ITypeBinding resolveType(EnumDeclaration type) {
		final Object node = this.newAstToOldAst.get(type);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	@Override
	ITypeBinding resolveType(RecordDeclaration type) {
		final Object node = this.newAstToOldAst.get(type);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeDeclaration.binding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}



	@Override
	synchronized ITypeBinding resolveType(Type type) {
		// retrieve the old ast node
		org.eclipse.jdt.internal.compiler.ast.ASTNode node = (org.eclipse.jdt.internal.compiler.ast.ASTNode) this.newAstToOldAst.get(type);
		org.eclipse.jdt.internal.compiler.lookup.TypeBinding binding = null;
		if (type.getAST().apiLevel() >= AST.JLS10_INTERNAL && type.isVar()) {
			return resolveTypeBindingForName(((SimpleType) type).getName());
		}
		if (node != null) {
			if (node instanceof Receiver) {
				node = ((Receiver) node).type;
			}
			if (node instanceof ParameterizedQualifiedTypeReference) {
 				ParameterizedQualifiedTypeReference typeReference = (ParameterizedQualifiedTypeReference) node;
				org.eclipse.jdt.internal.compiler.lookup.TypeBinding typeBinding = typeReference.resolvedType;
				// This unlikely case is possible when for some reason binding resolution has been stopped, like duplicate type declaration (bug 376440)
				if (typeBinding == null) return null;
				if (type.isArrayType()) {
					if (this.scope == null) {
						return null;
					}
					ArrayType arrayType = (ArrayType) type;
					ArrayBinding arrayBinding = (ArrayBinding) typeBinding;
					int dimensions = arrayType.getDimensions();
					boolean isVarargs = typeReference.isVarargs();
					if (dimensions == arrayBinding.dimensions)
						return getTypeBinding(arrayBinding); // reuse.
					return getTypeBinding(this.scope.createArrayType(arrayBinding.leafComponentType, dimensions, getTypeAnnotations(dimensions, arrayBinding, isVarargs)));
				}
				if (typeBinding.isArrayType()) {
					// 'typeBinding' can still be an array type because 'node' may be "larger" than 'type' (see comment of newAstToOldAst).
					typeBinding = ((ArrayBinding) typeBinding).leafComponentType;
				}
				int index;
				if (type.isQualifiedType()) {
					index = ((QualifiedType) type).index;
				} else if (type.isParameterizedType()) {
					index = ((ParameterizedType) type).index;
				} else {
					index = 1;
				}
				final int numberOfTypeArgumentsNotNull = getTypeCount(typeReference);
				if (index != numberOfTypeArgumentsNotNull) {
					int  i = numberOfTypeArgumentsNotNull;
					while (i != index) {
						typeBinding = typeBinding.enclosingType();
						i --;
					}
					binding = typeBinding;
				} else {
					binding = typeBinding;
				}
			} else if (node instanceof TypeReference) {
				if (type instanceof SimpleType && node instanceof QualifiedTypeReference) {
					return resolveTypeBindingForName(((SimpleType)type).getName());
				} else if (type instanceof QualifiedType) {
					return resolveTypeBindingForName(((QualifiedType)type).getName());
				} else if (type instanceof NameQualifiedType){
					return resolveTypeBindingForName(((NameQualifiedType)type).getName());
				}
				TypeReference typeReference = (TypeReference) node;
				binding = typeReference.resolvedType;
			} else if (node instanceof SingleNameReference && ((SingleNameReference)node).isTypeReference()) {
				binding = (((SingleNameReference)node).resolvedType);
			} else if (node instanceof QualifiedNameReference && ((QualifiedNameReference)node).isTypeReference()) {
				binding = (((QualifiedNameReference)node).resolvedType);
			} else if (node instanceof ArrayAllocationExpression) {
				binding = ((ArrayAllocationExpression) node).resolvedType;
			}
			if (binding != null) {
				if (type.isArrayType()) {
					ArrayType arrayType = (ArrayType) type;
					if (this.scope == null) {
						return null;
					}
					ArrayBinding arrayBinding = (ArrayBinding) binding;
					int dimensions = arrayType.getDimensions();
					boolean isVarargs = node instanceof TypeReference && ((TypeReference) node).isVarargs();
					if (dimensions == arrayBinding.dimensions)
						return getTypeBinding(arrayBinding); // reuse
					return getTypeBinding(this.scope.createArrayType(arrayBinding.leafComponentType, dimensions, getTypeAnnotations(dimensions, arrayBinding, isVarargs)));
				} else if (binding.isArrayType()) {
					// 'binding' can still be an array type because 'node' may be "larger" than 'type' (see comment of newAstToOldAst).
					ArrayBinding arrayBinding = (ArrayBinding) binding;
					return getTypeBinding(arrayBinding.leafComponentType);
				}
				return getTypeBinding(binding);
			}
		} else if (type.isPrimitiveType()) {
			/* Handle the void primitive type returned by getReturnType for a method declaration
			 * that is a constructor declaration. It prevents null from being returned
			 */
			if (((PrimitiveType) type).getPrimitiveTypeCode() == PrimitiveType.VOID) {
				return this.getTypeBinding(org.eclipse.jdt.internal.compiler.lookup.TypeBinding.VOID);
			}
		}
		return null;
	}

	private org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] getTypeAnnotations(int dimensions, ArrayBinding arrayBinding, boolean isVarargs) {
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding [] oldies = arrayBinding.getTypeAnnotations();
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] newbies = Binding.NO_ANNOTATIONS;
		// Skip past extended dimensions encoded ahead of base dimensions. Dimension for variable argument array comes after the base dimensions.
		int extendedDimensions = arrayBinding.dimensions - dimensions - (isVarargs ? 1 : 0);
		int i, length;
		for (i = 0, length = oldies == null ? 0 : oldies.length; i < length && extendedDimensions > 0 ; i++) {
			if (oldies[i] == null)
				extendedDimensions--;
		}
		int cells = 0;
		for (int j = i; j < length && dimensions > 0 ; j++) {
			if (oldies[j] == null)
				dimensions--;
			cells ++;
		}
		if (cells > 0)
			System.arraycopy(oldies, i, newbies = new org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[cells], 0, cells);
		return newbies;
	}

	@Override
	synchronized ITypeBinding resolveType(TypeDeclaration type) {
		final Object node = this.newAstToOldAst.get(type);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.TypeDeclaration typeDeclaration = (org.eclipse.jdt.internal.compiler.ast.TypeDeclaration) node;
			IBinding declaringMember = getDeclaringMember(typeDeclaration, typeDeclaration.scope);
			ITypeBinding typeBinding = internalGetTypeBinding(typeDeclaration.binding, declaringMember);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, type);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	@Override
	synchronized ITypeBinding resolveTypeParameter(TypeParameter typeParameter) {
		final Object node = this.newAstToOldAst.get(typeParameter);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.TypeParameter) {
			org.eclipse.jdt.internal.compiler.ast.TypeParameter typeParameter2 = (org.eclipse.jdt.internal.compiler.ast.TypeParameter) node;
			ITypeBinding typeBinding = this.getTypeBinding(typeParameter2.binding);
			if (typeBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(typeBinding, typeParameter);
			String key = typeBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, typeBinding);
			}
			return typeBinding;
		}
		return null;
	}

	@Override
	synchronized IVariableBinding resolveVariable(EnumConstantDeclaration enumConstant) {
		final Object node = this.newAstToOldAst.get(enumConstant);
		if (node instanceof org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) {
			org.eclipse.jdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) node;
			IVariableBinding variableBinding = this.getVariableBinding(fieldDeclaration.binding);
			if (variableBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(variableBinding, enumConstant);
			String key = variableBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, variableBinding);
			}
			return variableBinding;
		}
		return null;
	}

	@Override
	synchronized IVariableBinding resolveVariable(VariableDeclaration variable) {
		final Object node = this.newAstToOldAst.get(variable);
		if (node instanceof AbstractVariableDeclaration) {
			AbstractVariableDeclaration abstractVariableDeclaration = (AbstractVariableDeclaration) node;
			IVariableBinding variableBinding = null;
			if (abstractVariableDeclaration instanceof org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) {
				org.eclipse.jdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.jdt.internal.compiler.ast.FieldDeclaration) abstractVariableDeclaration;
				variableBinding = this.getVariableBinding(fieldDeclaration.binding, variable);
			} else if (abstractVariableDeclaration instanceof org.eclipse.jdt.internal.compiler.ast.RecordComponent) {
				org.eclipse.jdt.internal.compiler.ast.RecordComponent recordComponent = (org.eclipse.jdt.internal.compiler.ast.RecordComponent) abstractVariableDeclaration;
				variableBinding = this.getVariableBinding(recordComponent.binding, variable);
			} else {
				variableBinding = this.getVariableBinding(((LocalDeclaration) abstractVariableDeclaration).binding, variable);
			}
			if (variableBinding == null) {
				return null;
			}
			this.bindingsToAstNodes.put(variableBinding, variable);
			String key = variableBinding.getKey();
			if (key != null) {
				this.bindingTables.bindingKeysToBindings.put(key, variableBinding);
			}
			return variableBinding;
		}
		return null;
	}

	@Override
	synchronized ITypeBinding resolveWellKnownType(String name) {
		if (this.scope == null) return null;
		ITypeBinding typeBinding = null;
		try {
			if (("boolean".equals(name))//$NON-NLS-1$
				|| ("char".equals(name))//$NON-NLS-1$
				|| ("byte".equals(name))//$NON-NLS-1$
				|| ("short".equals(name))//$NON-NLS-1$
				|| ("int".equals(name))//$NON-NLS-1$
				|| ("long".equals(name))//$NON-NLS-1$
				|| ("float".equals(name))//$NON-NLS-1$
				|| ("double".equals(name))//$NON-NLS-1$
				|| ("void".equals(name))) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(Scope.getBaseType(name.toCharArray()));
			} else if ("java.lang.Object".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getJavaLangObject());
			} else if ("java.lang.String".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getJavaLangString());
			} else if ("java.lang.StringBuffer".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_STRINGBUFFER, 3));
			} else if ("java.lang.Throwable".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getJavaLangThrowable());
			} else if ("java.lang.Exception".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_EXCEPTION, 3));
			} else if ("java.lang.RuntimeException".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION, 3));
			} else if ("java.lang.Error".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_ERROR, 3));
			} else if ("java.lang.Class".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getJavaLangClass());
			} else if ("java.lang.Cloneable".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getJavaLangCloneable());
			} else if ("java.io.Serializable".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getJavaIoSerializable());
			} else if ("java.lang.Boolean".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_BOOLEAN, 3));
			} else if ("java.lang.Byte".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_BYTE, 3));
			} else if ("java.lang.Character".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_CHARACTER, 3));
			} else if ("java.lang.Double".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_DOUBLE, 3));
			} else if ("java.lang.Float".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_FLOAT, 3));
			} else if ("java.lang.Integer".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_INTEGER, 3));
			} else if ("java.lang.Long".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_LONG, 3));
			} else if ("java.lang.Short".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_SHORT, 3));
			} else if ("java.lang.Void".equals(name)) {//$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_VOID, 3));
			} else if ("java.lang.AssertionError".equals(name)) { //$NON-NLS-1$
				typeBinding = this.getTypeBinding(this.scope.getType(TypeConstants.JAVA_LANG_ASSERTIONERROR, 3));
			}
		} catch (AbortCompilation e) {
			// ignore missing types
		}
		if (typeBinding != null && !typeBinding.isRecovered()) {
			return typeBinding;
		}
		return null;
	}

	@Override
	synchronized IAnnotationBinding resolveAnnotation(final Annotation domASTNode) {
		Object oldNode = this.newAstToOldAst.get(domASTNode);
		if (oldNode instanceof org.eclipse.jdt.internal.compiler.ast.Annotation) {
			org.eclipse.jdt.internal.compiler.ast.Annotation internalAstNode =
				(org.eclipse.jdt.internal.compiler.ast.Annotation) oldNode;

			IAnnotationBinding domAnnotation = getAnnotationInstance(internalAstNode.getCompilerAnnotation());
			if (domAnnotation == null)
				return null;
			this.bindingsToAstNodes.put(domAnnotation, domASTNode);
			return domAnnotation;
		}
		return null;
	}

	@Override
	public CompilationUnitScope scope() {
		return this.scope;
	}

	@Override
	synchronized void store(ASTNode node, org.eclipse.jdt.internal.compiler.ast.ASTNode oldASTNode) {
		this.newAstToOldAst.put(node, oldASTNode);
	}

	@Override
	synchronized void updateKey(ASTNode node, ASTNode newNode) {
		Object astNode = this.newAstToOldAst.remove(node);
		if (astNode != null) {
			this.newAstToOldAst.put(newNode, astNode);
		}
	}

	@Override
	ITypeBinding resolveArrayType(ITypeBinding typeBinding, int dimensions) {
		if (typeBinding instanceof RecoveredTypeBinding) throw new IllegalArgumentException("Cannot be called on a recovered type binding"); //$NON-NLS-1$
		ITypeBinding leafComponentType = typeBinding;
		int actualDimensions = dimensions;
		if (typeBinding.isArray()) {
			leafComponentType = typeBinding.getElementType();
			actualDimensions += typeBinding.getDimensions();
		}
		if (!(leafComponentType instanceof TypeBinding)) return null;
		org.eclipse.jdt.internal.compiler.lookup.TypeBinding leafTypeBinding =
											((TypeBinding) leafComponentType).binding;
		if (leafTypeBinding instanceof VoidTypeBinding) {
			throw new IllegalArgumentException();
		}
		if (typeBinding.isArray()) {
			return this.getTypeBinding(lookupEnvironment().createArrayType(
											leafTypeBinding,
											actualDimensions,
											insertAnnotations((((TypeBinding) typeBinding).binding).getTypeAnnotations(), dimensions)));
		} else {
			return this.getTypeBinding(lookupEnvironment().createArrayType(
											leafTypeBinding,
											actualDimensions));
		}
	}

	private org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] insertAnnotations(
							org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] annots, int dimensions) {
		if (dimensions == 0 || annots == null || annots.length == 0) {
			return annots;
		}
		int index = 0;
		if (dimensions < 0) {
			for (int i = 0; i < annots.length; i++) {
				index++;
				if (annots[i] == null) {
					if(++dimensions == 0) break;
				}
			}
			if (dimensions < 0) dimensions = 0; // Just means there were no annotations
		}
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] newAnnots =
				new org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[annots.length - index + dimensions];

		System.arraycopy(annots, index, newAnnots, dimensions, annots.length - index);
		return newAnnots;
	}
}
