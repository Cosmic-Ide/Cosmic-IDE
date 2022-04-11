/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
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
 *								Bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.codeassist.complete.CompletionNodeDetector;
import org.eclipse.jdt.internal.codeassist.complete.CompletionParser;
import org.eclipse.jdt.internal.codeassist.impl.AssistCompilationUnit;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ImportBinding;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SignatureWrapper;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;
import org.eclipse.jdt.internal.core.CompilationUnitElementInfo;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.eclipse.jdt.internal.core.util.Util;

@SuppressWarnings({"rawtypes"})
public class InternalExtendedCompletionContext {
	private static Util.BindingsToNodesMap EmptyNodeMap = new Util.BindingsToNodesMap() {
		@Override
		public ASTNode get(Binding binding) {
			return null;
		}
	};

	private InternalCompletionContext completionContext;

	// static data
	private ITypeRoot typeRoot;
	private CompilationUnitDeclaration compilationUnitDeclaration;
	private LookupEnvironment lookupEnvironment;
	private Scope assistScope;
	private ASTNode assistNode;
	private ASTNode assistNodeParent;
	private WorkingCopyOwner owner;

	private CompletionParser parser;

	// computed data
	private boolean hasComputedVisibleElementBindings;
	private ObjectVector visibleLocalVariables;
	private ObjectVector visibleFields;
	private ObjectVector visibleMethods;

	private boolean hasComputedEnclosingJavaElements;
	private Map<Binding, JavaElement> bindingsToHandles;
	private Map<ASTNode, JavaElement> nodesWithProblemsToHandles;
	private ICompilationUnit compilationUnit;

	public InternalExtendedCompletionContext(
			InternalCompletionContext completionContext,
			ITypeRoot typeRoot,
			CompilationUnitDeclaration compilationUnitDeclaration,
			LookupEnvironment lookupEnvironment,
			Scope assistScope,
			ASTNode assistNode,
			ASTNode assistNodeParent,
			WorkingCopyOwner owner,
			CompletionParser parser) {
		this.completionContext = completionContext;
		this.typeRoot = typeRoot;
		this.compilationUnitDeclaration = compilationUnitDeclaration;
		this.lookupEnvironment = lookupEnvironment;
		this.assistScope = assistScope;
		this.assistNode = assistNode;
		this.assistNodeParent = assistNodeParent;
		this.owner = owner;
		this.parser = parser;
	}

	private void computeEnclosingJavaElements() {
		this.hasComputedEnclosingJavaElements = true;

		if (this.typeRoot == null) return;

		if (this.typeRoot.getElementType() == IJavaElement.COMPILATION_UNIT) {
	 		ICompilationUnit original = (org.eclipse.jdt.core.ICompilationUnit)this.typeRoot;

			HashMap<JavaElement, Binding> handleToBinding = new HashMap<>();
			HashMap<Binding, JavaElement> bindingToHandle = new HashMap<>();
			HashMap<ASTNode, JavaElement> nodeWithProblemToHandle = new HashMap<>();
			HashMap<ICompilationUnit, CompilationUnitElementInfo> handleToInfo = new HashMap<ICompilationUnit, CompilationUnitElementInfo>();

			org.eclipse.jdt.core.ICompilationUnit handle = new AssistCompilationUnit(original, this.owner, handleToBinding, handleToInfo);
			CompilationUnitElementInfo info = new CompilationUnitElementInfo();

			handleToInfo.put(handle, info);

			CompletionUnitStructureRequestor structureRequestor =
				new CompletionUnitStructureRequestor(
						handle,
						info,
						this.parser,
						this.assistNode,
						handleToBinding,
						bindingToHandle,
						nodeWithProblemToHandle,
						handleToInfo);

			CompletionElementNotifier notifier =
				new CompletionElementNotifier(
						structureRequestor,
						true,
						this.assistNode);

			notifier.notifySourceElementRequestor(
					this.compilationUnitDeclaration,
					this.compilationUnitDeclaration.sourceStart,
					this.compilationUnitDeclaration.sourceEnd,
					false,
					this.parser.sourceEnds,
					new HashMap());

			this.bindingsToHandles = bindingToHandle;
			this.nodesWithProblemsToHandles = nodeWithProblemToHandle;
			this.compilationUnit = handle;
		}
	}

	private void computeVisibleElementBindings() {
		CompilationUnitDeclaration previousUnitBeingCompleted = this.lookupEnvironment.unitBeingCompleted;
		this.lookupEnvironment.unitBeingCompleted = this.compilationUnitDeclaration;
		try {
			this.hasComputedVisibleElementBindings = true;

			Scope scope = this.assistScope;
			ASTNode astNode = this.assistNode;
			boolean notInJavadoc = this.completionContext.javadoc == 0;

			this.visibleLocalVariables = new ObjectVector();
			this.visibleFields = new ObjectVector();
			this.visibleMethods = new ObjectVector();

			ReferenceContext referenceContext = scope.referenceContext();
			if (referenceContext instanceof AbstractMethodDeclaration || referenceContext instanceof LambdaExpression) {
				// completion is inside a method body
				searchVisibleVariablesAndMethods(scope, this.visibleLocalVariables, this.visibleFields, this.visibleMethods, notInJavadoc);
			} else if (referenceContext instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;
				FieldDeclaration[] fields = typeDeclaration.fields;
				if (fields != null) {
					done : for (int i = 0; i < fields.length; i++) {
						if (fields[i] instanceof Initializer) {
							Initializer initializer = (Initializer) fields[i];
							if (initializer.block.sourceStart <= astNode.sourceStart &&
									astNode.sourceStart < initializer.bodyEnd) {
								// completion is inside an initializer
								searchVisibleVariablesAndMethods(scope, this.visibleLocalVariables, this.visibleFields, this.visibleMethods, notInJavadoc);
								break done;
							}
						} else {
							FieldDeclaration fieldDeclaration = fields[i];
							if (fieldDeclaration.initialization != null && fieldDeclaration.binding != null) {
								boolean isInsideInitializer = false;
								if (fieldDeclaration.initialization.sourceEnd > 0) {
									if (fieldDeclaration.initialization.sourceStart <= astNode.sourceStart &&
											astNode.sourceEnd <= fieldDeclaration.initialization.sourceEnd) {
										// completion is inside a field initializer
										isInsideInitializer = true;
									}
								} else { // The sourceEnd may not yet be set
									CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, fieldDeclaration.initialization);
									if (detector.containsCompletionNode()) {
										// completion is inside a field initializer
										isInsideInitializer = true;
									}
								}
								if (isInsideInitializer) {
									searchVisibleVariablesAndMethods(scope, this.visibleLocalVariables, this.visibleFields, this.visibleMethods, notInJavadoc);
									// remove this field from visibleFields list because completion is being asked in its
									// intialization and so this has not yet been declared successfully.
									if (this.visibleFields.size > 0 && this.visibleFields.contains(fieldDeclaration.binding)) {
										this.visibleFields.remove(fieldDeclaration.binding);
									}
									int count = 0;
									while (count < this.visibleFields.size) {
										FieldBinding visibleField = (FieldBinding)this.visibleFields.elementAt(count);
										if (visibleField.id > fieldDeclaration.binding.id) {
											this.visibleFields.remove(visibleField);
											continue;
										}
										count++;
									}
									break done;
								}
							}
						}
					}
				}
			}
		} finally {
			this.lookupEnvironment.unitBeingCompleted = previousUnitBeingCompleted;
		}
	}

	public IJavaElement getEnclosingElement() {
		try {
			if (!this.hasComputedEnclosingJavaElements) {
				computeEnclosingJavaElements();
			}
			if (this.compilationUnit == null) return null;
			IJavaElement enclosingElement = this.compilationUnit.getElementAt(this.completionContext.offset);
			return enclosingElement == null ? this.compilationUnit : enclosingElement;
		} catch (JavaModelException e) {
			Util.log(e, "Cannot compute enclosing element"); //$NON-NLS-1$
			return null;
		}
	}

	private JavaElement getJavaElement(LocalVariableBinding binding) {
		LocalDeclaration local = binding.declaration;

		JavaElement parent = null;
		ReferenceContext referenceContext = binding.declaringScope.isLambdaSubscope() ? binding.declaringScope.namedMethodScope().referenceContext() : binding.declaringScope.referenceContext();
		if (referenceContext instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) referenceContext;
			parent = this.getJavaElementOfCompilationUnit(methodDeclaration, methodDeclaration.binding);
		} else if (referenceContext instanceof TypeDeclaration){
			// Local variable is declared inside an initializer
			TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;

			JavaElement type = this.getJavaElementOfCompilationUnit(typeDeclaration, typeDeclaration.binding);
			parent = Util.getUnresolvedJavaElement(local.sourceStart, local.sourceEnd, type);
		}
		if (parent == null) return null;

		return new LocalVariable(
				parent,
				new String(local.name),
				local.declarationSourceStart,
				local.declarationSourceEnd,
				local.sourceStart,
				local.sourceEnd,
				local.type == null ? Signature.createTypeSignature(binding.type.signableName(), true) : Util.typeSignature(local.type),
				binding.declaration.annotations,
				local.modifiers,
				local.getKind() == AbstractVariableDeclaration.PARAMETER);
	}

	private JavaElement getJavaElementOfCompilationUnit(Binding binding) {
		if (!this.hasComputedEnclosingJavaElements) {
			computeEnclosingJavaElements();
		}
		if (this.bindingsToHandles == null) return null;
		return this.bindingsToHandles.get(binding);
	}

	private JavaElement getJavaElementOfCompilationUnit(ASTNode node, Binding binding) {
		if (!this.hasComputedEnclosingJavaElements) {
			computeEnclosingJavaElements();
		}
		if (binding != null) {
			if (this.bindingsToHandles == null) return null;
			return this.bindingsToHandles.get(binding);
		} else {
			if (this.nodesWithProblemsToHandles == null) return null;
			return this.nodesWithProblemsToHandles.get(node);
		}
	}

	private TypeBinding getTypeFromSignature(String typeSignature, Scope scope) {
		TypeBinding assignableTypeBinding = null;

		TypeVariableBinding[] typeVariables = Binding.NO_TYPE_VARIABLES;
		ReferenceContext referenceContext = scope.referenceContext();
		if (referenceContext instanceof AbstractMethodDeclaration) {
			AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) referenceContext;
			TypeParameter[] typeParameters = methodDeclaration.typeParameters();
			if (typeParameters != null && typeParameters.length > 0) {
				int length = typeParameters.length;
				int count = 0;
				typeVariables = new TypeVariableBinding[length];
				for (int i = 0; i < length; i++) {
					if (typeParameters[i].binding != null) {
						typeVariables[count++] = typeParameters[i].binding;
					}
				}

				if (count != length) {
					System.arraycopy(typeVariables, 0, typeVariables = new TypeVariableBinding[count], 0, count);
				}
			}
		}

		CompilationUnitDeclaration previousUnitBeingCompleted = this.lookupEnvironment.unitBeingCompleted;
		this.lookupEnvironment.unitBeingCompleted = this.compilationUnitDeclaration;
		try {

			SignatureWrapper wrapper = new SignatureWrapper(replacePackagesDot(typeSignature.toCharArray()));
			// FIXME(stephan): do we interpret type annotations here?
			assignableTypeBinding = this.lookupEnvironment.getTypeFromTypeSignature(wrapper, typeVariables, this.assistScope.enclosingClassScope().referenceContext.binding, null, ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER);
			assignableTypeBinding = BinaryTypeBinding.resolveType(assignableTypeBinding, this.lookupEnvironment, true);
		} catch (AbortCompilation e) {
			assignableTypeBinding = null;
		} finally {
			this.lookupEnvironment.unitBeingCompleted = previousUnitBeingCompleted;
		}
		return assignableTypeBinding;
	}

	private char[] replacePackagesDot(char[] signature) {
		boolean replace = true;
		int length = signature.length;
		for (int i = 0; i < length; i++) {
			switch (signature[i]) {
				case '.':
					if (replace) signature[i] = '/';
					break;
				case '<':
					replace = true;
					break;
				case '>':
					replace = false;
					break;
			}
		}
		return signature;
	}

	public IJavaElement[] getVisibleElements(String typeSignature) {
		if (this.assistScope == null) return new IJavaElement[0];

		if (!this.hasComputedVisibleElementBindings) {
			computeVisibleElementBindings();
		}

		TypeBinding assignableTypeBinding = null;
		if (typeSignature != null) {
			assignableTypeBinding = getTypeFromSignature(typeSignature, this.assistScope);
			if (assignableTypeBinding == null) return new IJavaElement[0];
		}

		int length = this.visibleLocalVariables.size() + this.visibleFields.size() + this.visibleMethods.size();
		if (length == 0) return new IJavaElement[0];

		IJavaElement[] result = new IJavaElement[length];

		int elementCount = 0;

		int size = this.visibleLocalVariables.size();
		if (size > 0) {
			next : for (int i = 0; i < size; i++) {
				try {
					LocalVariableBinding binding = (LocalVariableBinding) this.visibleLocalVariables.elementAt(i);
					if (binding.type == null || (assignableTypeBinding != null && !binding.type.isCompatibleWith(assignableTypeBinding))) continue next;
					JavaElement localVariable = getJavaElement(binding);
					if (localVariable != null) result[elementCount++] = localVariable;
				} catch(AbortCompilation e) {
					// log the exception and proceed
					Util.logRepeatedMessage(e.getKey(), e);
				}
			}

		}
		size = this.visibleFields.size();
		if (size > 0) {
			next : for (int i = 0; i < size; i++) {
				try {
					FieldBinding binding = (FieldBinding) this.visibleFields.elementAt(i);
					if (assignableTypeBinding != null && !binding.type.isCompatibleWith(assignableTypeBinding)) continue next;
					if (this.assistScope.isDefinedInSameUnit(binding.declaringClass)) {
						JavaElement field = getJavaElementOfCompilationUnit(binding);
						if (field != null) result[elementCount++] = field;
					} else {
						JavaElement field = Util.getUnresolvedJavaElement(binding, this.owner, EmptyNodeMap);
						if (field != null) result[elementCount++] = field.resolved(binding);
					}
				} catch(AbortCompilation e) {
					// log the exception and proceed
					Util.logRepeatedMessage(e.getKey(), e);
				}
			}

		}
		size = this.visibleMethods.size();
		if (size > 0) {
			next : for (int i = 0; i < size; i++) {
				try {
					MethodBinding binding = (MethodBinding) this.visibleMethods.elementAt(i);
					if (assignableTypeBinding != null && !binding.returnType.isCompatibleWith(assignableTypeBinding)) continue next;
					if (this.assistScope.isDefinedInSameUnit(binding.declaringClass)) {
						JavaElement method = getJavaElementOfCompilationUnit(binding);
						if (method != null) result[elementCount++] = method;
					} else {
						JavaElement method = Util.getUnresolvedJavaElement(binding, this.owner, EmptyNodeMap);
						if (method != null) result[elementCount++] = method.resolved(binding);
					}
				} catch(AbortCompilation e) {
					// log the exception and proceed
					Util.logRepeatedMessage(e.getKey(), e);
				}
			}
		}

		if (elementCount != result.length) {
			System.arraycopy(result, 0, result = new IJavaElement[elementCount], 0, elementCount);
		}

		return result;
	}

	private void searchVisibleFields(
			FieldBinding[] fields,
			ReferenceBinding receiverType,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean onlyStaticFields,
			ObjectVector localsFound,
			ObjectVector fieldsFound) {
		ObjectVector newFieldsFound = new ObjectVector();
		// Inherited fields which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite

		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			if (field.isSynthetic()) continue next;

			if (onlyStaticFields && !field.isStatic()) continue next;

			if (!field.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			for (int i = fieldsFound.size; --i >= 0;) {
				FieldBinding otherField = (FieldBinding) fieldsFound.elementAt(i);
				if (CharOperation.equals(field.name, otherField.name, true)) {
					continue next;
				}
			}

			for (int l = localsFound.size; --l >= 0;) {
				LocalVariableBinding local = (LocalVariableBinding) localsFound.elementAt(l);

				if (CharOperation.equals(field.name, local.name, true)) {
					continue next;
				}
			}

			newFieldsFound.add(field);
		}

		fieldsFound.addAll(newFieldsFound);
	}

	private void searchVisibleFields(
			ReferenceBinding receiverType,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean onlyStaticFields,
			boolean notInJavadoc,
			ObjectVector localsFound,
			ObjectVector fieldsFound) {

		ReferenceBinding currentType = receiverType;
		ReferenceBinding[] interfacesToVisit = null;
		int nextPosition = 0;
		do {
			ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
			if (notInJavadoc && itsInterfaces != Binding.NO_SUPERINTERFACES) {
				if (interfacesToVisit == null) {
					interfacesToVisit = itsInterfaces;
					nextPosition = interfacesToVisit.length;
				} else {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}

			FieldBinding[] fields = currentType.availableFields();
			if(fields != null && fields.length > 0) {

				searchVisibleFields(
						fields,
						receiverType,
						scope,
						invocationSite,
						invocationScope,
						onlyStaticFields,
						localsFound,
						fieldsFound);
			}
			currentType = currentType.superclass();
		} while (notInJavadoc && currentType != null);

		if (notInJavadoc && interfacesToVisit != null) {
			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding anInterface = interfacesToVisit[i];
				FieldBinding[] fields = anInterface.availableFields();
				if(fields !=  null) {
					searchVisibleFields(
							fields,
							receiverType,
							scope,
							invocationSite,
							invocationScope,
							onlyStaticFields,
							localsFound,
							fieldsFound);
				}

				ReferenceBinding[] itsInterfaces = anInterface.superInterfaces();
				if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	private void searchVisibleInterfaceMethods(
			ReferenceBinding[] itsInterfaces,
			ReferenceBinding receiverType,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean onlyStaticMethods,
			ObjectVector methodsFound) {
		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			ReferenceBinding[] interfacesToVisit = itsInterfaces;
			int nextPosition = interfacesToVisit.length;

			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding currentType = interfacesToVisit[i];
				MethodBinding[] methods = currentType.availableMethods();
				if(methods != null) {
					searchVisibleLocalMethods(
							methods,
							receiverType,
							scope,
							invocationSite,
							invocationScope,
							onlyStaticMethods,
							methodsFound);
				}

				itsInterfaces = currentType.superInterfaces();
				if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
					int itsLength = itsInterfaces.length;
					if (nextPosition + itsLength >= interfacesToVisit.length)
						System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
					nextInterface : for (int a = 0; a < itsLength; a++) {
						ReferenceBinding next = itsInterfaces[a];
						for (int b = 0; b < nextPosition; b++)
							if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
						interfacesToVisit[nextPosition++] = next;
					}
				}
			}
		}
	}

	private void searchVisibleLocalMethods(
			MethodBinding[] methods,
			ReferenceBinding receiverType,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean onlyStaticMethods,
			ObjectVector methodsFound) {
		ObjectVector newMethodsFound =  new ObjectVector();
		// Inherited methods which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite

		next : for (int f = methods.length; --f >= 0;) {
			MethodBinding method = methods[f];

			if (method.isSynthetic()) continue next;

			if (method.isDefaultAbstract())	continue next;

			if (method.isConstructor()) continue next;

			if (onlyStaticMethods && !method.isStatic()) continue next;

			if (!method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			for (int i = methodsFound.size; --i >= 0;) {
				MethodBinding otherMethod = (MethodBinding) methodsFound.elementAt(i);
				if (method == otherMethod)
					continue next;

				if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
					if (this.lookupEnvironment.methodVerifier().isMethodSubsignature(otherMethod, method)) {
						continue next;
					}
				}
			}

			newMethodsFound.add(method);
		}

		methodsFound.addAll(newMethodsFound);
	}

	private void searchVisibleMethods(
			ReferenceBinding receiverType,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			boolean onlyStaticMethods,
			boolean notInJavadoc,
			ObjectVector methodsFound) {
		ReferenceBinding currentType = receiverType;
		if (notInJavadoc) {
			if (receiverType.isInterface()) {
				searchVisibleInterfaceMethods(
						new ReferenceBinding[]{currentType},
						receiverType,
						scope,
						invocationSite,
						invocationScope,
						onlyStaticMethods,
						methodsFound);

				currentType = scope.getJavaLangObject();
			}
		}
		boolean hasPotentialDefaultAbstractMethods = true;
		while (currentType != null) {

			MethodBinding[] methods = currentType.availableMethods();
			if (methods != null) {
				searchVisibleLocalMethods(
						methods,
						receiverType,
						scope,
						invocationSite,
						invocationScope,
						onlyStaticMethods,
						methodsFound);
			}

			if (notInJavadoc &&
					hasPotentialDefaultAbstractMethods &&
					(currentType.isAbstract() ||
							currentType.isTypeVariable() ||
							currentType.isIntersectionType() ||
							currentType.isEnum())){

				ReferenceBinding[] superInterfaces = currentType.superInterfaces();
				if (superInterfaces != null && currentType.isIntersectionType()) {
					for (int i = 0; i < superInterfaces.length; i++) {
						superInterfaces[i] = (ReferenceBinding)superInterfaces[i].capture(invocationScope, invocationSite.sourceStart(), invocationSite.sourceEnd());
					}
				}

				searchVisibleInterfaceMethods(
						superInterfaces,
						receiverType,
						scope,
						invocationSite,
						invocationScope,
						onlyStaticMethods,
						methodsFound);
			} else {
				hasPotentialDefaultAbstractMethods = false;
			}
			if(currentType.isParameterizedType()) {
				currentType = ((ParameterizedTypeBinding)currentType).genericType().superclass();
			} else {
				currentType = currentType.superclass();
			}
		}
	}
	private void searchVisibleVariablesAndMethods(
			Scope scope,
			ObjectVector localsFound,
			ObjectVector fieldsFound,
			ObjectVector methodsFound,
			boolean notInJavadoc) {

		InvocationSite invocationSite = CompletionEngine.FakeInvocationSite;

		boolean staticsOnly = false;
		// need to know if we're in a static context (or inside a constructor)

		Scope currentScope = scope;

		done1 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
					//$FALL-THROUGH$
				case Scope.BLOCK_SCOPE :
					BlockScope blockScope = (BlockScope) currentScope;

					next : for (int i = 0, length = blockScope.locals.length; i < length; i++) {
						LocalVariableBinding local = blockScope.locals[i];

						if (local == null)
							break next;

						if (local.isSecret())
							continue next;
						// If the local variable declaration's initialization statement itself has the completion,
						// then don't propose the local variable
						if (local.declaration.initialization != null) {
							/*(use this if-else block if it is found that local.declaration.initialization != null is not sufficient to
							  guarantee that proposal is being asked inside a local variable declaration's initializer)
							 if(local.declaration.initialization.sourceEnd > 0) {
								if (this.assistNode.sourceEnd <= local.declaration.initialization.sourceEnd
										&& this.assistNode.sourceStart >= local.declaration.initialization.sourceStart) {
									continue next;
								}
							} else {
								CompletionNodeDetector detector = new CompletionNodeDetector(
										this.assistNode,
										local.declaration.initialization);
								if (detector.containsCompletionNode()) {
									continue next;
								}
							}*/
							continue next;
						}
						for (int f = 0; f < localsFound.size; f++) {
							LocalVariableBinding otherLocal =
								(LocalVariableBinding) localsFound.elementAt(f);
							if (CharOperation.equals(otherLocal.name, local.name, true))
								continue next;
						}

						localsFound.add(local);
					}
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					break done1;
			}
			currentScope = currentScope.parent;
		}

		staticsOnly = false;
		currentScope = scope;

		done2 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {
				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
					break;
				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					SourceTypeBinding enclosingType = classScope.referenceContext.binding;

					searchVisibleFields(
							enclosingType,
							classScope,
							invocationSite,
							scope,
							staticsOnly,
							notInJavadoc,
							localsFound,
							fieldsFound);

					searchVisibleMethods(
							enclosingType,
							classScope,
							invocationSite,
							scope,
							staticsOnly,
							notInJavadoc,
							methodsFound);

					staticsOnly |= enclosingType.isStatic();
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					break done2;
			}
			currentScope = currentScope.parent;
		}

		// search in static import
		ImportBinding[] importBindings = scope.compilationUnitScope().imports;
		for (int i = 0; i < importBindings.length; i++) {
			ImportBinding importBinding = importBindings[i];
			if(importBinding.isValidBinding() && importBinding.isStatic()) {
				Binding binding = importBinding.resolvedImport;
				if(binding != null && binding.isValidBinding()) {
					if(importBinding.onDemand) {
						if((binding.kind() & Binding.TYPE) != 0) {
							searchVisibleFields(
									(ReferenceBinding)binding,
									scope,
									invocationSite,
									scope,
									staticsOnly,
									notInJavadoc,
									localsFound,
									fieldsFound);

							searchVisibleMethods(
									(ReferenceBinding)binding,
									scope,
									invocationSite,
									scope,
									staticsOnly,
									notInJavadoc,
									methodsFound);
						}
					} else {
						if ((binding.kind() & Binding.FIELD) != 0) {
							searchVisibleFields(
									new FieldBinding[]{(FieldBinding)binding},
									((FieldBinding)binding).declaringClass,
									scope,
									invocationSite,
									scope,
									staticsOnly,
									localsFound,
									fieldsFound);
						} else if ((binding.kind() & Binding.METHOD) != 0) {
							MethodBinding methodBinding = (MethodBinding)binding;

							searchVisibleLocalMethods(
									methodBinding.declaringClass.getMethods(methodBinding.selector),
									methodBinding.declaringClass,
									scope,
									invocationSite,
									scope,
									true,
									methodsFound);
						}
					}
				}
			}
		}
	}

	public boolean canUseDiamond(String[] parameterTypes, char[] fullyQualifiedTypeName) {
		TypeBinding guessedType = null;
		char[][] cn = CharOperation.splitOn('.', fullyQualifiedTypeName);
		Scope scope = this.assistScope;
		if (scope.compilerOptions().sourceLevel < ClassFileConstants.JDK1_7) return false;
		// If no LHS or return type expected, then we can safely use diamond
		char[][] expectedTypekeys= this.completionContext.getExpectedTypesKeys();
		if (expectedTypekeys == null || expectedTypekeys.length == 0)
			return true;
		// Next, find out whether any of the constructor parameters are the same as one of the
		// class type variables. If yes, diamond cannot be used.
		TypeReference ref;
		if (cn.length == 1) {
			ref = new SingleTypeReference(cn[0], 0);
		} else {
			ref = new QualifiedTypeReference(cn,new long[cn.length]);
		}
		switch (scope.kind) {
			case Scope.METHOD_SCOPE :
			case Scope.BLOCK_SCOPE :
				guessedType = ref.resolveType((BlockScope)scope);
				break;
			case Scope.CLASS_SCOPE :
				guessedType = ref.resolveType((ClassScope)scope);
				break;
		}
		if (guessedType != null && guessedType.isValidBinding()) {
			// the erasure must be used because guessedType can be a RawTypeBinding
			guessedType = guessedType.erasure();
			TypeVariableBinding[] typeVars = guessedType.typeVariables();
			for (int i = 0; i < parameterTypes.length; i++) {
				for (int j = 0; j < typeVars.length; j++) {
					if (CharOperation.equals(parameterTypes[i].toCharArray(), typeVars[j].sourceName))
						return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * @see InternalCompletionContext#getCompletionNode()
	 */
	public ASTNode getCompletionNode() {
		return this.assistNode;
	}

	/**
	 * @see InternalCompletionContext#getCompletionNodeParent()
	 */
	public ASTNode getCompletionNodeParent() {
		// TODO Auto-generated method stub
		return this.assistNodeParent;
	}

	public ObjectVector getVisibleLocalVariables() {
		if (!this.hasComputedVisibleElementBindings) {
			computeVisibleElementBindings();
		}
		return this.visibleLocalVariables;
	}

	public ObjectVector getVisibleFields() {
		if (!this.hasComputedVisibleElementBindings) {
			computeVisibleElementBindings();
		}
		return this.visibleFields;
	}

	public ObjectVector getVisibleMethods() {
		if (!this.hasComputedVisibleElementBindings) {
			computeVisibleElementBindings();
		}
		return this.visibleMethods;
	}
}
