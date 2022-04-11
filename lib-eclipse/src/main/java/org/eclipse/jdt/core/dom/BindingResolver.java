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
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;

/**
 * A binding resolver is an internal mechanism for figuring out the binding
 * for a major declaration, type, or name reference. This also handles
 * the creation and mapping between annotations and the ast nodes that define them.
 * <p>
 * The default implementation serves as the default binding resolver
 * that does no resolving whatsoever. Internal subclasses do all the real work.
 * </p>
 *
 * @see AST#getBindingResolver
 */
class BindingResolver {

	/**
	 * Creates a binding resolver.
	 */
	BindingResolver() {
		// default implementation: do nothing
	}

	/**
	 * Finds the corresponding AST node from which the given binding originated.
	 * Returns <code>null</code> if the binding does not correspond to any node
	 * in the compilation unit.
	 * <p>
	 * The following table indicates the expected node type for the various
	 * different kinds of bindings:
	 * <ul>
	 * <li></li>
	 * <li>package - a <code>PackageDeclaration</code></li>
	 * <li>class or interface - a <code>TypeDeclaration</code> or a
	 *    <code>ClassInstanceCreation</code> (for anonymous classes) </li>
	 * <li>primitive type - none</li>
	 * <li>array type - none</li>
	 * <li>field - a <code>VariableDeclarationFragment</code> in a
	 *    <code>FieldDeclaration</code> </li>
	 * <li>local variable - a <code>SingleVariableDeclaration</code>, or
	 *    a <code>VariableDeclarationFragment</code> in a
	 *    <code>VariableDeclarationStatement</code> or
	 *    <code>VariableDeclarationExpression</code></li>
	 * <li>method - a <code>MethodDeclaration</code> </li>
	 * <li>constructor - a <code>MethodDeclaration</code> </li>
	 * <li>annotation type - an <code>AnnotationTypeDeclaration</code>
	 * <li>annotation type member - an <code>AnnotationTypeMemberDeclaration</code>
	 * </ul>
	 * </p>
	 * <p>
	 * The implementation of <code>CompilationUnit.findDeclaringNode</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param binding the binding
	 * @return the corresponding node where the bindings is declared,
	 *    or <code>null</code> if none
	 */
	ASTNode findDeclaringNode(IBinding binding) {
		return null;
	}

	/**
	 * Finds the corresponding AST node from which the given binding key originated.
	 *
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param bindingKey the binding key
	 * @return the corresponding node where the bindings is declared,
	 *    or <code>null</code> if none
	 */
	ASTNode findDeclaringNode(String bindingKey) {
		return null;
	}

	/**
	 * Finds the corresponding AST node from which the given annotation instance originated.
	 *
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param instance the dom annotation
	 * @return the corresponding node where the bindings is declared,
	 *    or <code>null</code> if none
	 */
	ASTNode findDeclaringNode(IAnnotationBinding instance) {
		return null;
	}

	/**
	 * Allows the user to get information about the given old/new pair of
	 * AST nodes.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param currentNode the new node
	 * @return org.eclipse.jdt.internal.compiler.ast.ASTNode
	 */
	org.eclipse.jdt.internal.compiler.ast.ASTNode getCorrespondingNode(ASTNode currentNode) {
		return null;
	}

	/**
	 * Returns the new method binding corresponding to the given old method binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param methodBinding the old method binding
	 * @return the new method binding
	 */
	IMethodBinding getMethodBinding(org.eclipse.jdt.internal.compiler.lookup.MethodBinding methodBinding) {
		return null;
	}

	/**
	 * Returns the new member value pair binding corresponding to the given old value pair binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param valuePair the old value pair binding
	 * @return the new member value pair binding
	 */
	IMemberValuePairBinding getMemberValuePairBinding(ElementValuePair valuePair) {
		return null;
	}

	/**
	 * Returns the new module binding corresponding to the given old module binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param moduleBinding the old module binding
	 * @return the new module binding
	 *
	 */
	IModuleBinding getModuleBinding(org.eclipse.jdt.internal.compiler.lookup.ModuleBinding moduleBinding) {
		return null;
	}

	/**
	 * Returns the new package binding corresponding to the given old package binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param packageBinding the old package binding
	 * @return the new package binding
	 */
	IPackageBinding getPackageBinding(org.eclipse.jdt.internal.compiler.lookup.PackageBinding packageBinding) {
		return null;
	}

	/**
	 * Returns the new type binding corresponding to the given old type binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param referenceBinding the old type binding
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(org.eclipse.jdt.internal.compiler.lookup.TypeBinding referenceBinding) {
		return null;
	}


	/**
	 * Returns the new type binding corresponding to the given variableDeclaration.
	 * This is used for recovered binding only.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param variableDeclaration the given variable declaration
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(VariableDeclaration variableDeclaration) {
		return null;
	}

	/**
	 * Returns the new type binding corresponding to the given type. This is used for recovered binding
	 * only.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the given type
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(Type type) {
		return null;
	}

	/**
	 * Returns the new type binding corresponding to the given recovered type binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param recoveredTypeBinding the recovered type binding
	 * @param dimensions the dimensions to add the to given type binding dimensions
	 * @return the new type binding
	 */
	ITypeBinding getTypeBinding(RecoveredTypeBinding recoveredTypeBinding, int dimensions) {
		return null;
	}

	/**
	 * Returns the new variable binding corresponding to the given old variable binding.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param binding the old variable binding
	 * @return the new variable binding
	 */
	IVariableBinding getVariableBinding(org.eclipse.jdt.internal.compiler.lookup.VariableBinding binding) {
		return null;
	}

	/**
	 * Return the working copy owner for the receiver.
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 * @return the working copy owner for the receiver
	 */
	public WorkingCopyOwner getWorkingCopyOwner() {
		return null;
	}

	/**
	 * Return the new annotation corresponding to the given old annotation
	 * <p>
	 * The default implementation of this method returns <code>null</code>
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param instance the old annotation
	 * @return the new DOM annotation
	 */
	IAnnotationBinding getAnnotationInstance(org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding instance) {
		return null;
	}

	boolean isResolvedTypeInferredFromExpectedType(MethodInvocation methodInvocation) {
		return false;
	}

	boolean isResolvedTypeInferredFromExpectedType(SuperMethodInvocation methodInvocation) {
		return false;
	}

	boolean isResolvedTypeInferredFromExpectedType(ClassInstanceCreation classInstanceCreation) {
		return false;
	}

	/**
	 * Returns the compiler lookup environment used by this binding resolver.
	 * Returns <code>null</code> if none.
	 *
	 * @return the lookup environment used by this resolver, or <code>null</code> if none.
	 */
	LookupEnvironment lookupEnvironment() {
		return null;
	}

	/**
	 * This method is used to record the scope and its corresponding node.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 * @param astNode
	 */
	void recordScope(ASTNode astNode, BlockScope blockScope) {
		// default implementation: do nothing
	}

	/**
	 * Returns whether this expression node is the site of a boxing
	 * conversion (JLS3 5.1.7). This information is available only
	 * when bindings are requested when the AST is being built.
	 *
	 * @return <code>true</code> if this expression is the site of a
	 * boxing conversion, or <code>false</code> if either no boxing conversion
	 * is involved or if bindings were not requested when the AST was created
	 * @since 3.1
	 */
	boolean resolveBoxing(Expression expression) {
		return false;
	}

	/**
	 * Returns whether this expression node is the site of an unboxing
	 * conversion (JLS3 5.1.8). This information is available only
	 * when bindings are requested when the AST is being built.
	 *
	 * @return <code>true</code> if this expression is the site of an
	 * unboxing conversion, or <code>false</code> if either no unboxing
	 * conversion is involved or if bindings were not requested when the
	 * AST was created
	 * @since 3.1
	 */
	boolean resolveUnboxing(Expression expression) {
		return false;
	}

	/**
	 * Resolves and returns the compile-time constant expression value as
	 * specified in JLS2 15.28, if this expression has one. Constant expression
	 * values are unavailable unless bindings are requested when the AST is
	 * being built. If the type of the value is a primitive type, the result
	 * is the boxed equivalent (i.e., int returned as an <code>Integer</code>);
	 * if the type of the value is <code>String</code>, the result is the string
	 * itself. If the expression does not have a compile-time constant expression
	 * value, the result is <code>null</code>.
	 * <p>
	 * Resolving constant expressions takes into account the value of simple
	 * and qualified names that refer to constant variables (JLS2 4.12.4).
	 * </p>
	 * <p>
	 * Note 1: enum constants are not considered constant expressions either.
	 * The result is always <code>null</code> for these.
	 * </p>
	 * <p>
	 * Note 2: Compile-time constant expressions cannot denote <code>null</code>.
	 * So technically {@link NullLiteral} nodes are not constant expressions.
	 * The result is <code>null</code> for these nonetheless.
	 * </p>
	 *
	 * @return the constant expression value, or <code>null</code> if this
	 * expression has no constant expression value or if bindings were not
	 * requested when the AST was created
	 * @since 3.1
	 */
	Object resolveConstantExpressionValue(Expression expression) {
		return null;
	}

	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>ClassInstanceCreation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveConstructor(ClassInstanceCreation expression) {
		return null;
	}

	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>ConstructorInvocation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveConstructor(ConstructorInvocation expression) {
		return null;
	}
	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>ConstructorInvocation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param enumConstantDeclaration the enum constant declaration of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveConstructor(EnumConstantDeclaration enumConstantDeclaration) {
		return null;
	}
	/**
	 * Resolves and returns the binding for the constructor being invoked.
	 * <p>
	 * The implementation of
	 * <code>SuperConstructorInvocation.resolveConstructor</code>
	 * forwards to this method. Which constructor is invoked is often a function
	 * of the context in which the expression node is embedded as well as
	 * the expression subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression of interest
	 * @return the binding for the constructor being invoked, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveConstructor(SuperConstructorInvocation expression) {
		return null;
	}
	/**
	 * Resolves the type of the given expression and returns the type binding
	 * for it.
	 * <p>
	 * The implementation of <code>Expression.resolveTypeBinding</code>
	 * forwards to this method. The result is often a function of the context
	 * in which the expression node is embedded as well as the expression
	 * subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param expression the expression whose type is of interest
	 * @return the binding for the type of the given expression, or
	 *    <code>null</code> if no binding is available
	 */
	ITypeBinding resolveExpressionType(Expression expression) {
		return null;
	}

	/**
	 * Resolves the given field access and returns the binding for it.
	 * <p>
	 * The implementation of <code>FieldAccess.resolveFieldBinding</code>
	 * forwards to this method. How the field resolves is often a function of
	 * the context in which the field access node is embedded as well as
	 * the field access subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param fieldAccess the field access of interest
	 * @return the binding for the given field access, or
	 *    <code>null</code> if no binding is available
	 */
	IVariableBinding resolveField(FieldAccess fieldAccess) {
		return null;
	}

	/**
	 * Resolves the given super field access and returns the binding for it.
	 * <p>
	 * The implementation of <code>SuperFieldAccess.resolveFieldBinding</code>
	 * forwards to this method. How the field resolves is often a function of
	 * the context in which the super field access node is embedded as well as
	 * the super field access subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param fieldAccess the super field access of interest
	 * @return the binding for the given field access, or
	 *    <code>null</code> if no binding is available
	 */
	IVariableBinding resolveField(SuperFieldAccess fieldAccess) {
		return null;
	}

	/**
	 * Resolves the given import declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>ImportDeclaration.resolveBinding</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param importDeclaration the import declaration of interest
	 * @return the binding for the given package declaration, or
	 *         the package binding (for on-demand imports) or type binding
	 *         (for single-type imports), or <code>null</code> if no binding is
	 *         available
	 */
	IBinding resolveImport(ImportDeclaration importDeclaration) {
		return null;
	}

	/**
	 * Resolves the given annotation type declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>AnnotationTypeMemberDeclaration.resolveBinding</code>
	 * forwards to this method. How the declaration resolves is often a
	 * function of the context in which the declaration node is embedded as well
	 * as the declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param member the annotation type member declaration of interest
	 * @return the binding for the given annotation type member declaration, or <code>null</code>
	 *    if no binding is available
	 * @since 3.0
	 */
	IMethodBinding resolveMember(AnnotationTypeMemberDeclaration member) {
		return null;
	}

	/**
	 * Resolves the given method declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>MethodDeclaration.resolveBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method declaration node is embedded as well as
	 * the method declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param method the method or constructor declaration of interest
	 * @return the binding for the given method declaration, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveMethod(MethodDeclaration method) {
		return null;
	}

	/**
	 * Resolves the given  method reference and returns the binding for it.
	 * <p>
	 * The implementation of <code>MethodReference.resolveMethodBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method reference node is embedded as well as
	 * the method reference subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param methodReference the  method reference of interest
	 * @return the binding for the given  method reference, or
	 *    <code>null</code> if no binding is available
	 * @since 3.10
	 */
	IMethodBinding resolveMethod(MethodReference methodReference) {
		return null;
	}

	/**
	 * Resolves the given Lambda Expression and returns the binding for it.
	 * <p>
	 * The implementation of <code>LambdaExpression.resolveMethodBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method declaration node is embedded as well as
	 * the method declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may re-implement.
	 * </p>
	 *
	 * @param lambda LambdaExpression of interest
	 * @return the binding for the given lambda expression, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveMethod(LambdaExpression lambda) {
		return null;
	}

	/**
	 * Resolves the given method invocation and returns the binding for it.
	 * <p>
	 * The implementation of <code>MethodInvocation.resolveMethodBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method invocation node is embedded as well as
	 * the method invocation subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param method the method invocation of interest
	 * @return the binding for the given method invocation, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveMethod(MethodInvocation method) {
		return null;
	}

	/**
	 * Resolves the given method invocation and returns the binding for it.
	 * <p>
	 * The implementation of <code>MethodInvocation.resolveMethodBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method invocation node is embedded as well as
	 * the method invocation subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param method the method invocation of interest
	 * @return the binding for the given method invocation, or
	 *    <code>null</code> if no binding is available
	 */
	IMethodBinding resolveMethod(SuperMethodInvocation method) {
		return null;
	}

	/**
	 * Resolves the given module declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>ModuleDeclaration.resolveBinding</code>
	 * forwards to this method. How the method resolves is often a function of
	 * the context in which the method declaration node is embedded as well as
	 * the method declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param module declaration of interest
	 * @return the binding for the given module declaration, or
	 *    <code>null</code> if no binding is available
	 *
	 * @since 3.14
	 */
	IModuleBinding resolveModule(ModuleDeclaration module) {
		return null;
	}

	/**
	 * Resolves the given name and returns the type binding for it.
	 * <p>
	 * The implementation of <code>Name.resolveBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param name the name of interest
	 * @return the binding for the name, or <code>null</code> if no binding is
	 *    available
	 */
	IBinding resolveName(Name name) {
		return null;
	}

	/**
	 * Resolves the given package declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>PackageDeclaration.resolveBinding</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param pkg the package declaration of interest
	 * @return the binding for the given package declaration, or
	 *    <code>null</code> if no binding is available
	 */
	IPackageBinding resolvePackage(PackageDeclaration pkg) {
		return null;
	}

	/**
	 * Resolves the given reference and returns the binding for it.
	 * <p>
	 * The implementation of <code>MemberRef.resolveBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param ref the reference of interest
	 * @return the binding for the reference, or <code>null</code> if no binding is
	 *    available
	 * @since 3.0
	 */
	IBinding resolveReference(MemberRef ref) {
		return null;
	}

	/**
	 * Resolves the given member value pair and returns the binding for it.
	 * <p>
	 * The implementation of <code>MemberValuePair.resolveMemberValuePairBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param memberValuePair the member value pair of interest
	 * @return the binding for the member value pair, or <code>null</code> if no binding is
	 *    available
	 * @since 3.2
	 */
	IMemberValuePairBinding resolveMemberValuePair(MemberValuePair memberValuePair) {
		return null;
	}

	/**
	 * Resolves the given reference and returns the binding for it.
	 * <p>
	 * The implementation of <code>MethodRef.resolveBinding</code> forwards to
	 * this method. How the name resolves is often a function of the context
	 * in which the name node is embedded as well as the name itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param ref the reference of interest
	 * @return the binding for the reference, or <code>null</code> if no binding is
	 *    available
	 * @since 3.0
	 */
	IBinding resolveReference(MethodRef ref) {
		return null;
	}

	/**
	 * Resolves the given annotation type declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>AnnotationTypeDeclaration.resolveBinding</code>
	 * forwards to this method. How the declaration resolves is often a
	 * function of the context in which the declaration node is embedded as well
	 * as the declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the annotation type declaration of interest
	 * @return the binding for the given annotation type declaration, or <code>null</code>
	 *    if no binding is available
	 * @since 3.0
	 */
	ITypeBinding resolveType(AnnotationTypeDeclaration type) {
		return null;
	}

	/**
	 * Resolves the given anonymous class declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>AnonymousClassDeclaration.resolveBinding</code>
	 * forwards to this method. How the declaration resolves is often a
	 * function of the context in which the declaration node is embedded as well
	 * as the declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the anonymous class declaration of interest
	 * @return the binding for the given class declaration, or <code>null</code>
	 *    if no binding is available
	 */
	ITypeBinding resolveType(AnonymousClassDeclaration type) {
		return null;
	}

	/**
	 * Resolves the given enum declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>EnumDeclaration.resolveBinding</code>
	 * forwards to this method. How the enum declaration resolves is often
	 * a function of the context in which the declaration node is embedded
	 * as well as the enum declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the enum declaration of interest
	 * @return the binding for the given enum declaration, or <code>null</code>
	 *    if no binding is available
	 * @since 3.0
	 */
	ITypeBinding resolveType(EnumDeclaration type) {
		return null;
	}

	/**
	 * Resolves the given record declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>RecordDeclaration.resolveBinding</code>
	 * forwards to this method. How the record declaration resolves is often
	 * a function of the context in which the declaration node is embedded
	 * as well as the record declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may re implement.
	 * </p>
	 *
	 * @param type the record declaration of interest
	 * @return the binding for the given record declaration, or <code>null</code>
	 *    if no binding is available
	 * @since 3.22
	 */
	ITypeBinding resolveType(RecordDeclaration type) {
		return null;
	}

	/**
	 * Resolves the given type and returns the type binding for it.
	 * <p>
	 * The implementation of <code>Type.resolveBinding</code>
	 * forwards to this method. How the type resolves is often a function
	 * of the context in which the type node is embedded as well as the type
	 * subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the type of interest
	 * @return the binding for the given type, or <code>null</code>
	 *    if no binding is available
	 */
	ITypeBinding resolveType(Type type) {
		return null;
	}

	/**
	 * Resolves the given class or interface declaration and returns the binding
	 * for it.
	 * <p>
	 * The implementation of <code>TypeDeclaration.resolveBinding</code>
	 * (and <code>TypeDeclarationStatement.resolveBinding</code>) forwards
	 * to this method. How the type declaration resolves is often a function of
	 * the context in which the type declaration node is embedded as well as the
	 * type declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param type the class or interface declaration of interest
	 * @return the binding for the given type declaration, or <code>null</code>
	 *    if no binding is available
	 */
	ITypeBinding resolveType(TypeDeclaration type) {
		return null;
	}

	/**
	 * Resolves the given type parameter and returns the type binding for the
	 * type parameter.
	 * <p>
	 * The implementation of <code>TypeParameter.resolveBinding</code>
	 * forwards to this method. How the declaration resolves is often a
	 * function of the context in which the declaration node is embedded as well
	 * as the declaration subtree itself.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param typeParameter the type paramter of interest
	 * @return the binding for the given type parameter, or <code>null</code>
	 *    if no binding is available
	 * @since 3.1
	 */
	ITypeBinding resolveTypeParameter(TypeParameter typeParameter) {
		return null;
	}

	/**
	 * Resolves the given enum constant declaration and returns the binding for
	 * the field.
	 * <p>
	 * The implementation of <code>EnumConstantDeclaration.resolveVariable</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param enumConstant the enum constant declaration of interest
	 * @return the field binding for the given enum constant declaration, or
	 *    <code>null</code> if no binding is available
	 * @since 3.0
	 */
	IVariableBinding resolveVariable(EnumConstantDeclaration enumConstant) {
		return null;
	}

	/**
	 * Resolves the given variable declaration and returns the binding for it.
	 * <p>
	 * The implementation of <code>VariableDeclaration.resolveBinding</code>
	 * forwards to this method. How the variable declaration resolves is often
	 * a function of the context in which the variable declaration node is
	 * embedded as well as the variable declaration subtree itself. VariableDeclaration
	 * declarations used as local variable, formal parameter and exception
	 * variables resolve to local variable bindings; variable declarations
	 * used to declare fields resolve to field bindings.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param variable the variable declaration of interest
	 * @return the binding for the given variable declaration, or
	 *    <code>null</code> if no binding is available
	 */
	IVariableBinding resolveVariable(VariableDeclaration variable) {
		return null;
	}

	/**
	 * Resolves the given well known type by name and returns the type binding
	 * for it.
	 * <p>
	 * The implementation of <code>AST.resolveWellKnownType</code>
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param name the name of a well known type
	 * @return the corresponding type binding, or <code>null<code> if the
	 *   named type is not considered well known or if no binding can be found
	 *   for it
	 */
	ITypeBinding resolveWellKnownType(String name) {
		return null;
	}

	/**
	 * Resolves the given annotation instance and returns the DOM representation for it.
	 * <p>
	 * The implementation of {@link Annotation#resolveAnnotationBinding()}
	 * forwards to this method.
	 * </p>
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param annotation the annotation ast node of interest
	 * @return the DOM annotation representation for the given ast node, or
	 *    <code>null</code> if none is available
	 */
	IAnnotationBinding resolveAnnotation(Annotation annotation) {
		return null;
	}

	/**
	 * Answer an array type binding with the given type binding and the given
	 * dimensions.
	 *
	 * <p>If the given type binding is an array binding, then the resulting dimensions is the given dimensions
	 * plus the existing dimensions of the array binding. Otherwise the resulting dimensions is the given
	 * dimensions.</p>
	 *
	 * <p>
	 * The default implementation of this method returns <code>null</code>.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param typeBinding the given type binding
	 * @param dimensions the given dimensions
	 * @return an array type binding with the given type binding and the given
	 * dimensions
	 * @throws IllegalArgumentException if the type binding represents the <code>void</code> type binding
	 */
	ITypeBinding resolveArrayType(ITypeBinding typeBinding, int dimensions) {
		return null;
	}

	/**
	 * Returns the compilation unit scope used by this binding resolver.
	 * Returns <code>null</code> if none.
	 *
	 * @return the compilation unit scope by this resolver, or <code>null</code> if none.
	 */
	public CompilationUnitScope scope() {
		return null;
	}

	/**
	 * Allows the user to store information about the given old/new pair of
	 * AST nodes.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param newNode the new AST node
	 * @param oldASTNode the old AST node
	 */
	void store(ASTNode newNode, org.eclipse.jdt.internal.compiler.ast.ASTNode oldASTNode) {
		// default implementation: do nothing
	}

	/**
	 * Allows the user to update information about the given old/new pair of
	 * AST nodes.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses may reimplement.
	 * </p>
	 *
	 * @param node the old AST node
	 * @param newNode the new AST node
	 */
	void updateKey(ASTNode node, ASTNode newNode) {
		// default implementation: do nothing
	}
}
