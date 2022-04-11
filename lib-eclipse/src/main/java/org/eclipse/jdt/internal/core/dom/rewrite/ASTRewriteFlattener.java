/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;

@SuppressWarnings("rawtypes")
public class ASTRewriteFlattener extends ASTVisitor {

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY = ArrayType.COMPONENT_TYPE_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_FIELD_MODIFIERS_PROPERTY = FieldDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_INITIALIZER_MODIFIERS_PROPERTY = Initializer.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_METHOD_MODIFIERS_PROPERTY = MethodDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_METHOD_RETURN_TYPE_PROPERTY = MethodDeclaration.RETURN_TYPE_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_METHOD_EXTRA_DIMENSIONS_PROPERTY = MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildListPropertyDescriptor INTERNAL_METHOD_THROWN_EXCEPTIONS_PROPERTY = MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_TYPE_MODIFIERS_PROPERTY = TypeDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_TYPE_SUPERCLASS_PROPERTY = TypeDeclaration.SUPERCLASS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildListPropertyDescriptor INTERNAL_TYPE_SUPER_INTERFACES_PROPERTY = TypeDeclaration.SUPER_INTERFACES_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_CIC_NAME_PROPERTY = ClassInstanceCreation.NAME_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_FRAGMENT_EXTRA_DIMENSIONS_PROPERTY = VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_TDS_TYPE_DECLARATION_PROPERTY = TypeDeclarationStatement.TYPE_DECLARATION_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VARIABLE_MODIFIERS_PROPERTY = SingleVariableDeclaration.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VARIABLE_EXTRA_DIMENSIONS_PROPERTY = SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VDE_MODIFIERS_PROPERTY = VariableDeclarationExpression.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final SimplePropertyDescriptor INTERNAL_VDS_MODIFIERS_PROPERTY = VariableDeclarationStatement.MODIFIERS_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildListPropertyDescriptor INTERNAL_TRY_STATEMENT_RESOURCES_PROPERTY = TryStatement.RESOURCES_PROPERTY;

	/** @deprecated using deprecated code */
	private static final ChildPropertyDescriptor INTERNAL_SWITCH_EXPRESSION_PROPERTY = SwitchCase.EXPRESSION_PROPERTY;

	/** @deprecated using deprecated code */
	private static final int JLS2_INTERNAL = AST.JLS2;

	/** @deprecated using deprecated code */
	private static final int JLS3_INTERNAL = AST.JLS3;

	/** @deprecated using deprecated code */
	private static final int JLS4_INTERNAL = AST.JLS4;

	/** @deprecated using deprecated code */
	private static final int JLS8_INTERNAL = AST.JLS8;

	/** @deprecated using deprecated code */
	private static final int JLS9_INTERNAL = AST.JLS9;

	/** @deprecated using deprecated code */
	private static final int JLS14_INTERNAL = AST.JLS14;

	public static String asString(ASTNode node, RewriteEventStore store) {
		ASTRewriteFlattener flattener= new ASTRewriteFlattener(store);
		node.accept(flattener);
		return flattener.getResult();
	}

	protected StringBuffer result;
	private RewriteEventStore store;

	public ASTRewriteFlattener(RewriteEventStore store) {
		this.store= store;
		this.result= new StringBuffer();
	}

	/**
	 * Returns the string accumulated in the visit.
	 *
	 * @return the serialized
	 */
	public String getResult() {
		return this.result.toString();
	}

	/**
	 * Resets this printer so that it can be used again.
	 */
	public void reset() {
		this.result.setLength(0);
	}

	/**
	 * Appends the text representation of the given modifier flags, followed by a single space.
	 *
	 * @param modifiers the modifiers
	 * @param buf The <code>StringBuffer</code> to write the result to.
	 */
	public static void printModifiers(int modifiers, StringBuffer buf) {
		if (Modifier.isPublic(modifiers)) {
			buf.append("public "); //$NON-NLS-1$
		}
		if (Modifier.isProtected(modifiers)) {
			buf.append("protected "); //$NON-NLS-1$
		}
		if (Modifier.isPrivate(modifiers)) {
			buf.append("private "); //$NON-NLS-1$
		}
		if (Modifier.isStatic(modifiers)) {
			buf.append("static "); //$NON-NLS-1$
		}
		if (Modifier.isAbstract(modifiers)) {
			buf.append("abstract "); //$NON-NLS-1$
		}
		if (Modifier.isFinal(modifiers)) {
			buf.append("final "); //$NON-NLS-1$
		}
		if (Modifier.isSynchronized(modifiers)) {
			buf.append("synchronized "); //$NON-NLS-1$
		}
		if (Modifier.isVolatile(modifiers)) {
			buf.append("volatile "); //$NON-NLS-1$
		}
		if (Modifier.isNative(modifiers)) {
			buf.append("native "); //$NON-NLS-1$
		}
		if (Modifier.isStrictfp(modifiers)) {
			buf.append("strictfp "); //$NON-NLS-1$
		}
		if (Modifier.isTransient(modifiers)) {
			buf.append("transient "); //$NON-NLS-1$
		}
		if (Modifier.isSealed(modifiers)) {
			buf.append("sealed ");//$NON-NLS-1$
		}
		if (Modifier.isNonSealed(modifiers)) {
			buf.append("non-sealed ");//$NON-NLS-1$
		}
	}


	protected List getChildList(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return (List) getAttribute(parent, childProperty);
	}

	protected ASTNode getChildNode(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return (ASTNode) getAttribute(parent, childProperty);
	}

	protected int getIntAttribute(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return ((Integer) getAttribute(parent, childProperty)).intValue();
	}

	protected boolean getBooleanAttribute(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return ((Boolean) getAttribute(parent, childProperty)).booleanValue();
	}

	protected Object getAttribute(ASTNode parent, StructuralPropertyDescriptor childProperty) {
		return this.store.getNewValue(parent, childProperty);
	}

	protected void visitList(ASTNode parent, StructuralPropertyDescriptor childProperty, String separator) {
		List list= getChildList(parent, childProperty);
		for (int i= 0; i < list.size(); i++) {
			if (separator != null && i > 0) {
				this.result.append(separator);
			}
			((ASTNode) list.get(i)).accept(this);
		}
	}

	protected void visitList(ASTNode parent, StructuralPropertyDescriptor childProperty, String separator, String lead, String post) {
		List list= getChildList(parent, childProperty);
		if (!list.isEmpty()) {
			this.result.append(lead);
			for (int i= 0; i < list.size(); i++) {
				if (separator != null && i > 0) {
					this.result.append(separator);
				}
				((ASTNode) list.get(i)).accept(this);
			}
			this.result.append(post);
		}
	}

	private void visitExtraDimensions(ASTNode node, SimplePropertyDescriptor dimensions, ChildListPropertyDescriptor dimensionsInfo) {
		if (node.getAST().apiLevel() < JLS8_INTERNAL) {
			int extraDimensions= getIntAttribute(node, dimensions);
			for (int i = 0; i < extraDimensions; i++) {
				this.result.append("[]"); //$NON-NLS-1$
			}
		} else {
			visitList(node, dimensionsInfo, String.valueOf(' '), String.valueOf(' '), Util.EMPTY_STRING);
		}
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		this.result.append('{');
		visitList(node, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(ArrayAccess node) {
		getChildNode(node, ArrayAccess.ARRAY_PROPERTY).accept(this);
		this.result.append('[');
		getChildNode(node, ArrayAccess.INDEX_PROPERTY).accept(this);
		this.result.append(']');
		return false;
	}

	@Override
	public boolean visit(ArrayCreation node) {
		this.result.append("new "); //$NON-NLS-1$
		ArrayType arrayType= (ArrayType) getChildNode(node, ArrayCreation.TYPE_PROPERTY);

		// get the element type and count dimensions
		Type elementType;
		int dimensions;
		boolean astLevelGTE8 = node.getAST().apiLevel() >= JLS8_INTERNAL;
		if (astLevelGTE8) {
			elementType = (Type) getChildNode(arrayType, ArrayType.ELEMENT_TYPE_PROPERTY);
			dimensions = getChildList(arrayType, ArrayType.DIMENSIONS_PROPERTY).size();
		} else {
			elementType = (Type) getChildNode(arrayType, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
			dimensions = 1; // always include this array type
			while (elementType.isArrayType()) {
				dimensions++;
				elementType = (Type) getChildNode(elementType, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY);
			}
		}

		elementType.accept(this);

		// add "<annotations> [ <dimension> ]" for each dimension expression
		List list= getChildList(node, ArrayCreation.DIMENSIONS_PROPERTY);
		int size = list.size();
		for (int i= 0; i < size; i++) {
			internalVisitDimensionAnnotations(arrayType, i, astLevelGTE8);
			this.result.append('[');
			((ASTNode) list.get(i)).accept(this);
			this.result.append(']');
		}

		// add "<annotations> []" for each extra array dimension
		for (int i= list.size(); i < dimensions; i++) {
			internalVisitDimensionAnnotations(arrayType, i, astLevelGTE8);
			this.result.append("[]"); //$NON-NLS-1$
		}

		ASTNode initializer= getChildNode(node, ArrayCreation.INITIALIZER_PROPERTY);
		if (initializer != null) {
			getChildNode(node, ArrayCreation.INITIALIZER_PROPERTY).accept(this);
		}
		return false;
	}

	private void internalVisitDimensionAnnotations(ArrayType arrayType, int index, boolean astLevelGTE8) {
		if (astLevelGTE8) {
			Dimension dimension = (Dimension) arrayType.dimensions().get(index);
			visitList(dimension, Dimension.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		this.result.append('{');
		visitList(node, ArrayInitializer.EXPRESSIONS_PROPERTY, String.valueOf(','));
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(ArrayType node) {
		if (node.getAST().apiLevel() < JLS8_INTERNAL) {
			getChildNode(node, INTERNAL_ARRAY_COMPONENT_TYPE_PROPERTY).accept(this);
			this.result.append("[]"); //$NON-NLS-1$
		} else {
			getChildNode(node, ArrayType.ELEMENT_TYPE_PROPERTY).accept(this);
			visitList(node, ArrayType.DIMENSIONS_PROPERTY, Util.EMPTY_STRING, Util.EMPTY_STRING, Util.EMPTY_STRING);
		}
		return false;
	}

	@Override
	public boolean visit(AssertStatement node) {
		this.result.append("assert "); //$NON-NLS-1$
		getChildNode(node, AssertStatement.EXPRESSION_PROPERTY).accept(this);

		ASTNode message= getChildNode(node, AssertStatement.MESSAGE_PROPERTY);
		if (message != null) {
			this.result.append(':');
			message.accept(this);
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(Assignment node) {
		getChildNode(node, Assignment.LEFT_HAND_SIDE_PROPERTY).accept(this);
		this.result.append(getAttribute(node, Assignment.OPERATOR_PROPERTY).toString());
		getChildNode(node, Assignment.RIGHT_HAND_SIDE_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(Block node) {
		this.result.append('{');
		visitList(node, Block.STATEMENTS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		if (node.booleanValue() == true) {
			this.result.append("true"); //$NON-NLS-1$
		} else {
			this.result.append("false"); //$NON-NLS-1$
		}
		return false;
	}

	@Override
	public boolean visit(BreakStatement node) {
		this.result.append("break"); //$NON-NLS-1$
		ASTNode label= getChildNode(node, BreakStatement.LABEL_PROPERTY);
		if (label != null) {
			this.result.append(' ');
			label.accept(this);
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(CaseDefaultExpression node) {
		if (DOMASTUtil.isPatternSupported(node.getAST())) {
			this.result.append("default");//$NON-NLS-1$}
		}
		return false;
	}

	@Override
	public boolean visit(CastExpression node) {
		this.result.append('(');
		getChildNode(node, CastExpression.TYPE_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, CastExpression.EXPRESSION_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(CatchClause node) {
		this.result.append("catch ("); //$NON-NLS-1$
		getChildNode(node, CatchClause.EXCEPTION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, CatchClause.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		this.result.append(getAttribute(node, CharacterLiteral.ESCAPED_VALUE_PROPERTY));
		return false;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		ASTNode expression= getChildNode(node, ClassInstanceCreation.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
			this.result.append('.');
		}
		this.result.append("new ");//$NON-NLS-1$
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			getChildNode(node, INTERNAL_CIC_NAME_PROPERTY).accept(this);
		} else {
			visitList(node, ClassInstanceCreation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
			getChildNode(node, ClassInstanceCreation.TYPE_PROPERTY).accept(this);
		}

		this.result.append('(');
		visitList(node, ClassInstanceCreation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		ASTNode decl= getChildNode(node, ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY);
		if (decl != null) {
			decl.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(CompilationUnit node) {
		if (node.getAST().apiLevel() >= JLS9_INTERNAL) {
			ASTNode module= getChildNode(node, CompilationUnit.MODULE_PROPERTY);
			if (module != null) {
				module.accept(this);
			}
		}

		ASTNode pack= getChildNode(node, CompilationUnit.PACKAGE_PROPERTY);
		if (pack != null) {
			pack.accept(this);
		}
		visitList(node, CompilationUnit.IMPORTS_PROPERTY, null);
		visitList(node, CompilationUnit.TYPES_PROPERTY, null);
		return false;
	}

	@Override
	public boolean visit(ConditionalExpression node) {
		getChildNode(node, ConditionalExpression.EXPRESSION_PROPERTY).accept(this);
		this.result.append('?');
		getChildNode(node, ConditionalExpression.THEN_EXPRESSION_PROPERTY).accept(this);
		this.result.append(':');
		getChildNode(node, ConditionalExpression.ELSE_EXPRESSION_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			visitList(node, ConstructorInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}
		this.result.append("this("); //$NON-NLS-1$
		visitList(node, ConstructorInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(");"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(ContinueStatement node) {
		this.result.append("continue"); //$NON-NLS-1$
		ASTNode label= getChildNode(node, ContinueStatement.LABEL_PROPERTY);
		if (label != null) {
			this.result.append(' ');
			label.accept(this);
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(CreationReference node) {
		getChildNode(node, CreationReference.TYPE_PROPERTY).accept(this);
		this.result.append("::"); //$NON-NLS-1$
		visitList(node, CreationReference.TYPE_ARGUMENTS_PROPERTY, Util.EMPTY_STRING, String.valueOf('<'), String.valueOf('>'));
		this.result.append("new"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(Dimension node) {
		visitList(node, Dimension.ANNOTATIONS_PROPERTY, String.valueOf(' '), String.valueOf(' '), String.valueOf(' '));
		this.result.append("[]"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(DoStatement node) {
		this.result.append("do "); //$NON-NLS-1$
		getChildNode(node, DoStatement.BODY_PROPERTY).accept(this);
		this.result.append(" while ("); //$NON-NLS-1$
		getChildNode(node, DoStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(");"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(EmptyStatement node) {
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(ExportsDirective node) {
		this.result.append("exports "); //$NON-NLS-1$
		getChildNode(node, ExportsDirective.NAME_PROPERTY).accept(this);
		List<Name> modules = node.modules();
		if (modules.size() > 0) {
			this.result.append(" to "); //$NON-NLS-1$
			visitList(node, ExportsDirective.MODULES_PROPERTY, Util.COMMA_SEPARATOR, Util.EMPTY_STRING, Util.EMPTY_STRING);
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(ExpressionStatement node) {
		getChildNode(node, ExpressionStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(FieldAccess node) {
		getChildNode(node, FieldAccess.EXPRESSION_PROPERTY).accept(this);
		this.result.append('.');
		getChildNode(node, FieldAccess.NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		ASTNode javadoc= getChildNode(node, FieldDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_FIELD_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, FieldDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, FieldDeclaration.TYPE_PROPERTY).accept(this);
		this.result.append(' ');
		visitList(node, FieldDeclaration.FRAGMENTS_PROPERTY, String.valueOf(','));
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(ForStatement node) {
		this.result.append("for ("); //$NON-NLS-1$
		visitList(node, ForStatement.INITIALIZERS_PROPERTY, String.valueOf(','));
		this.result.append(';');
		ASTNode expression= getChildNode(node, ForStatement.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
		}
		this.result.append(';');
		visitList(node, ForStatement.UPDATERS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		getChildNode(node, ForStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(GuardedPattern node) {
		if (DOMASTUtil.isPatternSupported(node.getAST())) {
			node.getPattern().accept(this);
			this.result.append(" && ");//$NON-NLS-1$
			node.getExpression().accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(IfStatement node) {
		this.result.append("if ("); //$NON-NLS-1$
		getChildNode(node, IfStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, IfStatement.THEN_STATEMENT_PROPERTY).accept(this);
		ASTNode elseStatement= getChildNode(node, IfStatement.ELSE_STATEMENT_PROPERTY);
		if (elseStatement != null) {
			this.result.append(" else "); //$NON-NLS-1$
			elseStatement.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		this.result.append("import "); //$NON-NLS-1$
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			if (getBooleanAttribute(node, ImportDeclaration.STATIC_PROPERTY)) {
				this.result.append("static ");//$NON-NLS-1$
			}
		}
		getChildNode(node, ImportDeclaration.NAME_PROPERTY).accept(this);
		if (getBooleanAttribute(node, ImportDeclaration.ON_DEMAND_PROPERTY)) {
			this.result.append(".*"); //$NON-NLS-1$
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(InfixExpression node) {
		getChildNode(node, InfixExpression.LEFT_OPERAND_PROPERTY).accept(this);
		this.result.append(' ');
		String operator= getAttribute(node, InfixExpression.OPERATOR_PROPERTY).toString();

		this.result.append(operator);
		this.result.append(' ');
		getChildNode(node, InfixExpression.RIGHT_OPERAND_PROPERTY).accept(this);

		String separator= ' ' + operator + ' ';
		visitList(node, InfixExpression.EXTENDED_OPERANDS_PROPERTY, separator, separator, Util.EMPTY_STRING);
		return false;
	}

	@Override
	public boolean visit(Initializer node) {
		ASTNode javadoc= getChildNode(node, Initializer.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_INITIALIZER_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, Initializer.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, Initializer.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		getChildNode(node, InstanceofExpression.LEFT_OPERAND_PROPERTY).accept(this);
		this.result.append(" instanceof "); //$NON-NLS-1$
		getChildNode(node, InstanceofExpression.RIGHT_OPERAND_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(PatternInstanceofExpression node) {
		getChildNode(node, PatternInstanceofExpression.LEFT_OPERAND_PROPERTY).accept(this);
		this.result.append(" instanceof "); //$NON-NLS-1$
		getChildNode(node, PatternInstanceofExpression.RIGHT_OPERAND_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(IntersectionType node) {
		visitList(node, IntersectionType.TYPES_PROPERTY, " & ", Util.EMPTY_STRING, Util.EMPTY_STRING); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(Javadoc node) {
		this.result.append("/**"); //$NON-NLS-1$
		List list= getChildList(node, Javadoc.TAGS_PROPERTY);
		for (int i= 0; i < list.size(); i++) {
			this.result.append("\n * "); //$NON-NLS-1$
			((ASTNode) list.get(i)).accept(this);
		}
		this.result.append("\n */"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(LabeledStatement node) {
		getChildNode(node, LabeledStatement.LABEL_PROPERTY).accept(this);
		this.result.append(": "); //$NON-NLS-1$
		getChildNode(node, LabeledStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(LambdaExpression node) {
		boolean hasParentheses = getBooleanAttribute(node, LambdaExpression.PARENTHESES_PROPERTY);
		if (!hasParentheses) {
			List parameters = getChildList(node, LambdaExpression.PARAMETERS_PROPERTY);
			hasParentheses = !(parameters.size() == 1 && parameters.get(0) instanceof VariableDeclarationFragment);
		}

		if (hasParentheses)
			this.result.append('(');
		visitList(node, LambdaExpression.PARAMETERS_PROPERTY, String.valueOf(','));
		if (hasParentheses)
			this.result.append(')');
		this.result.append("->"); //$NON-NLS-1$
		getChildNode(node, LambdaExpression.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		ASTNode javadoc= getChildNode(node, MethodDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_METHOD_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, MethodDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
			visitList(node, MethodDeclaration.TYPE_PARAMETERS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}

		if (!getBooleanAttribute(node, MethodDeclaration.CONSTRUCTOR_PROPERTY)) {
			if (node.getAST().apiLevel() == JLS2_INTERNAL) {
				getChildNode(node, INTERNAL_METHOD_RETURN_TYPE_PROPERTY).accept(this);
			} else {
				ASTNode returnType = getChildNode(node, MethodDeclaration.RETURN_TYPE2_PROPERTY);
				if (returnType != null) {
					returnType.accept(this);
				} else {
					// methods really ought to have a return type
					this.result.append("void");//$NON-NLS-1$
				}
			}
			this.result.append(' ');
		}
		getChildNode(node, MethodDeclaration.NAME_PROPERTY).accept(this);
		if (!(DOMASTUtil.isRecordDeclarationSupported(node.getAST()) && getBooleanAttribute(node, MethodDeclaration.COMPACT_CONSTRUCTOR_PROPERTY))) {
			this.result.append('(');
			// receiver parameter
			if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
				ASTNode receiverType = getChildNode(node, MethodDeclaration.RECEIVER_TYPE_PROPERTY);
				if (receiverType != null) {
					receiverType.accept(this);
					this.result.append(' ');
					ASTNode qualifier = getChildNode(node, MethodDeclaration.RECEIVER_QUALIFIER_PROPERTY);
					if (qualifier != null) {
						qualifier.accept(this);
						this.result.append('.');
					}
					this.result.append("this"); //$NON-NLS-1$
					if (getChildList(node, MethodDeclaration.PARAMETERS_PROPERTY).size() > 0) {
						this.result.append(',');
					}
				}
			}

			visitList(node, MethodDeclaration.PARAMETERS_PROPERTY, String.valueOf(','));
			this.result.append(')');
		}
		visitExtraDimensions(node, INTERNAL_METHOD_EXTRA_DIMENSIONS_PROPERTY, MethodDeclaration.EXTRA_DIMENSIONS2_PROPERTY);

		ChildListPropertyDescriptor exceptionsProperty = node.getAST().apiLevel() <	JLS8_INTERNAL ?
				INTERNAL_METHOD_THROWN_EXCEPTIONS_PROPERTY : MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY;
		visitList(node, exceptionsProperty, String.valueOf(','), " throws ", Util.EMPTY_STRING); //$NON-NLS-1$
		ASTNode body= getChildNode(node, MethodDeclaration.BODY_PROPERTY);
		if (body == null) {
			this.result.append(';');
		} else {
			body.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(ModuleDeclaration node) {
		ASTNode javadoc= getChildNode(node, ModuleDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		visitList(node, ModuleDeclaration.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		if (getBooleanAttribute(node, ModuleDeclaration.OPEN_PROPERTY)) {
			this.result.append("open ");//$NON-NLS-1$
		}
		this.result.append("module "); //$NON-NLS-1$
		getChildNode(node, ModuleDeclaration.NAME_PROPERTY).accept(this);
		this.result.append('{');
		visitList(node, ModuleDeclaration.MODULE_DIRECTIVES_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		ASTNode expression= getChildNode(node, MethodInvocation.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
			this.result.append('.');
		}
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			visitList(node, MethodInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}

		getChildNode(node, MethodInvocation.NAME_PROPERTY).accept(this);
		this.result.append('(');
		visitList(node, MethodInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		return false;
	}

	@Override
	public boolean visit(NullLiteral node) {
		this.result.append("null"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(NullPattern node) {
		this.result.append("null");//$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(NumberLiteral node) {
		this.result.append(getAttribute(node, NumberLiteral.TOKEN_PROPERTY).toString());
		return false;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			ASTNode javadoc = getChildNode(node, PackageDeclaration.JAVADOC_PROPERTY);
			if (javadoc != null) {
				javadoc.accept(this);
			}
			visitList(node, PackageDeclaration.ANNOTATIONS_PROPERTY, String.valueOf(' '));
		}
		this.result.append("package "); //$NON-NLS-1$
		getChildNode(node, PackageDeclaration.NAME_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		this.result.append('(');
		getChildNode(node, ParenthesizedExpression.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		return false;
	}

	@Override
	public boolean visit(PostfixExpression node) {
		getChildNode(node, PostfixExpression.OPERAND_PROPERTY).accept(this);
		this.result.append(getAttribute(node, PostfixExpression.OPERATOR_PROPERTY).toString());
		return false;
	}

	@Override
	public boolean visit(PrefixExpression node) {
		this.result.append(getAttribute(node, PrefixExpression.OPERATOR_PROPERTY).toString());
		getChildNode(node, PrefixExpression.OPERAND_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(ProvidesDirective node) {
		this.result.append("provides "); //$NON-NLS-1$
		getChildNode(node, ProvidesDirective.NAME_PROPERTY).accept(this);
		this.result.append(" with "); //$NON-NLS-1$
		visitList(node, ProvidesDirective.IMPLEMENTATIONS_PROPERTY, Util.EMPTY_STRING, Util.COMMA_SEPARATOR, Util.EMPTY_STRING);
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(PrimitiveType node) {
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			visitList(node, PrimitiveType.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		this.result.append(getAttribute(node, PrimitiveType.PRIMITIVE_TYPE_CODE_PROPERTY).toString());
		return false;
	}

	@Override
	public boolean visit(QualifiedName node) {
		getChildNode(node, QualifiedName.QUALIFIER_PROPERTY).accept(this);
		this.result.append('.');
		getChildNode(node, QualifiedName.NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(RecordDeclaration node) {
		ASTNode javadoc= getChildNode(node, RecordDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		visitList(node, RecordDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		this.result.append("record ");//$NON-NLS-1$
		getChildNode(node, RecordDeclaration.NAME_PROPERTY).accept(this);
		this.result.append(' ');

		visitList(node, RecordDeclaration.TYPE_PARAMETERS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));

		this.result.append('(');
		visitList(node, RecordDeclaration.RECORD_COMPONENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');

		this.result.append(' ');
		visitList(node, RecordDeclaration.SUPER_INTERFACE_TYPES_PROPERTY, String.valueOf(','), "implements ", Util.EMPTY_STRING); //$NON-NLS-1$

		this.result.append('{');
		visitList(node, RecordDeclaration.BODY_DECLARATIONS_PROPERTY, Util.EMPTY_STRING, String.valueOf(';'), Util.EMPTY_STRING);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(RequiresDirective node) {
		this.result.append("requires "); //$NON-NLS-1$
		visitList(node, RequiresDirective.MODIFIERS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		getChildNode(node, RequiresDirective.NAME_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(ReturnStatement node) {
		this.result.append("return"); //$NON-NLS-1$
		ASTNode expression= getChildNode(node, ReturnStatement.EXPRESSION_PROPERTY);
		if (expression != null) {
			this.result.append(' ');
			expression.accept(this);
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(SimpleName node) {
		this.result.append(getAttribute(node, SimpleName.IDENTIFIER_PROPERTY));
		return false;
	}

	@Override
	public boolean visit(SimpleType node) {
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			visitList(node, SimpleType.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		node.getName().accept(this);
		return false;
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_VARIABLE_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, SingleVariableDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, SingleVariableDeclaration.TYPE_PROPERTY).accept(this);
		if (node.getAST().apiLevel() >= JLS8_INTERNAL  && node.isVarargs()) {
			visitList(node, SingleVariableDeclaration.VARARGS_ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			if (getBooleanAttribute(node, SingleVariableDeclaration.VARARGS_PROPERTY)) {
				this.result.append("...");//$NON-NLS-1$
			}
		}
		this.result.append(' ');
		getChildNode(node, SingleVariableDeclaration.NAME_PROPERTY).accept(this);
		visitExtraDimensions(node, INTERNAL_VARIABLE_EXTRA_DIMENSIONS_PROPERTY, SingleVariableDeclaration.EXTRA_DIMENSIONS2_PROPERTY);
		ASTNode initializer= getChildNode(node, SingleVariableDeclaration.INITIALIZER_PROPERTY);
		if (initializer != null) {
			this.result.append('=');
			initializer.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(StringLiteral node) {
		this.result.append(getAttribute(node, StringLiteral.ESCAPED_VALUE_PROPERTY));
		return false;
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		ASTNode expression= getChildNode(node, SuperConstructorInvocation.EXPRESSION_PROPERTY);
		if (expression != null) {
			expression.accept(this);
			this.result.append('.');
		}
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			visitList(node, SuperConstructorInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}
		this.result.append("super("); //$NON-NLS-1$
		visitList(node, SuperConstructorInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(");"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		ASTNode qualifier= getChildNode(node, SuperFieldAccess.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("super."); //$NON-NLS-1$
		getChildNode(node, SuperFieldAccess.NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		ASTNode qualifier= getChildNode(node, SuperMethodInvocation.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("super."); //$NON-NLS-1$
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			visitList(node, SuperMethodInvocation.TYPE_ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}
		getChildNode(node, SuperMethodInvocation.NAME_PROPERTY).accept(this);
		this.result.append('(');
		visitList(node, SuperMethodInvocation.ARGUMENTS_PROPERTY, String.valueOf(','));
		this.result.append(')');
		return false;
	}

	@Override
	public boolean visit(SwitchCase node) {
		if ((node.getAST().apiLevel() >= JLS14_INTERNAL)) {
			if (node.isDefault()) {
				this.result.append("default");//$NON-NLS-1$
				this.result.append(getBooleanAttribute(node, SwitchCase.SWITCH_LABELED_RULE_PROPERTY) ? " ->" : ":");//$NON-NLS-1$ //$NON-NLS-2$
			} else {
				this.result.append("case ");//$NON-NLS-1$
				for (Iterator it = node.expressions().iterator(); it.hasNext(); ) {
					Expression t = (Expression) it.next();
						t.accept(this);
						this.result.append(it.hasNext() ? ", " : ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				this.result.append(getBooleanAttribute(node, SwitchCase.SWITCH_LABELED_RULE_PROPERTY) ? " ->" : ":");//$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			ASTNode expression= getChildNode(node, INTERNAL_SWITCH_EXPRESSION_PROPERTY);
			if (expression == null) {
				this.result.append("default"); //$NON-NLS-1$
			} else {
				this.result.append("case "); //$NON-NLS-1$
				expression.accept(this);
			}
			this.result.append(':');
		}
		return false;
	}

	@Override
	public boolean visit(SwitchExpression node) {
		visitSwitchNode(node);
		return false;
	}

	private void visitSwitchNode(ASTNode node) {
		this.result.append("switch ("); //$NON-NLS-1$
		if (node instanceof SwitchExpression) {
			getChildNode(node, SwitchExpression.EXPRESSION_PROPERTY).accept(this);
		}
		else if (node instanceof SwitchStatement) {
			getChildNode(node, SwitchStatement.EXPRESSION_PROPERTY).accept(this);
		}
		this.result.append(')');
		this.result.append('{');
		if (node instanceof SwitchExpression) {
			visitList(node, SwitchExpression.STATEMENTS_PROPERTY, null);
		}
		else if (node instanceof SwitchStatement) {
			visitList(node, SwitchStatement.STATEMENTS_PROPERTY, null);
		}

		this.result.append('}');
	}

	@Override
	public boolean visit(SwitchStatement node) {
		visitSwitchNode(node);
		return false;
	}

	@Override
	public boolean visit(SynchronizedStatement node) {
		this.result.append("synchronized ("); //$NON-NLS-1$
		getChildNode(node, SynchronizedStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, SynchronizedStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(ThisExpression node) {
		ASTNode qualifier= getChildNode(node, ThisExpression.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("this"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		this.result.append("throw "); //$NON-NLS-1$
		getChildNode(node, ThrowStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(TryStatement node) {
		this.result.append("try "); //$NON-NLS-1$
		int level = node.getAST().apiLevel();
		if (level >= JLS4_INTERNAL) {
			StructuralPropertyDescriptor desc = level < JLS9_INTERNAL ? INTERNAL_TRY_STATEMENT_RESOURCES_PROPERTY : TryStatement.RESOURCES2_PROPERTY;
			visitList(node, desc, String.valueOf(';'), String.valueOf('('), String.valueOf(')'));
		}
		getChildNode(node, TryStatement.BODY_PROPERTY).accept(this);
		this.result.append(' ');
		visitList(node, TryStatement.CATCH_CLAUSES_PROPERTY, null);
		ASTNode finallyClause= getChildNode(node, TryStatement.FINALLY_PROPERTY);
		if (finallyClause != null) {
			this.result.append(" finally "); //$NON-NLS-1$
			finallyClause.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		int apiLevel= node.getAST().apiLevel();

		ASTNode javadoc= getChildNode(node, TypeDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}

		if (apiLevel == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_TYPE_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, TypeDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}

		boolean isInterface= getBooleanAttribute(node, TypeDeclaration.INTERFACE_PROPERTY);
		this.result.append(isInterface ? "interface " : "class "); //$NON-NLS-1$ //$NON-NLS-2$
		getChildNode(node, TypeDeclaration.NAME_PROPERTY).accept(this);
		if (apiLevel >= JLS3_INTERNAL) {
			visitList(node, TypeDeclaration.TYPE_PARAMETERS_PROPERTY, String.valueOf(','), String.valueOf('<'), String.valueOf('>'));
		}

		this.result.append(' ');

		ChildPropertyDescriptor superClassProperty= (apiLevel == JLS2_INTERNAL) ? INTERNAL_TYPE_SUPERCLASS_PROPERTY : TypeDeclaration.SUPERCLASS_TYPE_PROPERTY;
		ASTNode superclass= getChildNode(node, superClassProperty);
		if (superclass != null) {
			this.result.append("extends "); //$NON-NLS-1$
			superclass.accept(this);
			this.result.append(' ');
		}

		ChildListPropertyDescriptor superInterfaceProperty= (apiLevel == JLS2_INTERNAL) ? INTERNAL_TYPE_SUPER_INTERFACES_PROPERTY : TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY;
		String lead= isInterface ? "extends " : "implements ";  //$NON-NLS-1$//$NON-NLS-2$
		visitList(node, superInterfaceProperty, String.valueOf(','), lead, Util.EMPTY_STRING);


		if (DOMASTUtil.isFeatureSupportedinAST(node.getAST(), Modifier.SEALED) && !node.permittedTypes().isEmpty()) {
				visitList(node, TypeDeclaration.PERMITS_TYPES_PROPERTY, String.valueOf(','), lead, Util.EMPTY_STRING);
		}
		this.result.append('{');
		visitList(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY, null);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			getChildNode(node, INTERNAL_TDS_TYPE_DECLARATION_PROPERTY).accept(this);
		} else {
			getChildNode(node, TypeDeclarationStatement.DECLARATION_PROPERTY).accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		getChildNode(node, TypeLiteral.TYPE_PROPERTY).accept(this);
		this.result.append(".class"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(UnionType node) {
		visitList(node, UnionType.TYPES_PROPERTY, " | ", Util.EMPTY_STRING, Util.EMPTY_STRING); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean visit(UsesDirective node) {
		this.result.append("uses "); //$NON-NLS-1$
		getChildNode(node, UsesDirective.NAME_PROPERTY).accept(this);
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_VDE_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, VariableDeclarationExpression.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, VariableDeclarationExpression.TYPE_PROPERTY).accept(this);
		this.result.append(' ');
		visitList(node, VariableDeclarationExpression.FRAGMENTS_PROPERTY, String.valueOf(','));
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		getChildNode(node, VariableDeclarationFragment.NAME_PROPERTY).accept(this);
		visitExtraDimensions(node, INTERNAL_FRAGMENT_EXTRA_DIMENSIONS_PROPERTY, VariableDeclarationFragment.EXTRA_DIMENSIONS2_PROPERTY);
		ASTNode initializer= getChildNode(node, VariableDeclarationFragment.INITIALIZER_PROPERTY);
		if (initializer != null) {
			this.result.append('=');
			initializer.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (node.getAST().apiLevel() == JLS2_INTERNAL) {
			printModifiers(getIntAttribute(node, INTERNAL_VDS_MODIFIERS_PROPERTY), this.result);
		} else {
			visitList(node, VariableDeclarationStatement.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, VariableDeclarationStatement.TYPE_PROPERTY).accept(this);
		this.result.append(' ');
		visitList(node, VariableDeclarationStatement.FRAGMENTS_PROPERTY, String.valueOf(','));
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(WhileStatement node) {
		this.result.append("while ("); //$NON-NLS-1$
		getChildNode(node, WhileStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, WhileStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(BlockComment node) {
		return false; // cant flatten, needs source
	}

	@Override
	public boolean visit(LineComment node) {
		return false; // cant flatten, needs source
	}

	@Override
	public boolean visit(MemberRef node) {
		ASTNode qualifier= getChildNode(node, MemberRef.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
		}
		this.result.append('#');
		getChildNode(node, MemberRef.NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(MethodRef node) {
		ASTNode qualifier= getChildNode(node, MethodRef.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
		}
		this.result.append('#');
		getChildNode(node, MethodRef.NAME_PROPERTY).accept(this);
		this.result.append('(');
		visitList(node, MethodRef.PARAMETERS_PROPERTY, ","); //$NON-NLS-1$
		this.result.append(')');
		return false;
	}

	@Override
	public boolean visit(MethodRefParameter node) {
		getChildNode(node, MethodRefParameter.TYPE_PROPERTY).accept(this);
		if (node.getAST().apiLevel() >= JLS3_INTERNAL) {
			if (getBooleanAttribute(node, MethodRefParameter.VARARGS_PROPERTY)) {
				this.result.append("..."); //$NON-NLS-1$
			}
		}
		ASTNode name= getChildNode(node, MethodRefParameter.NAME_PROPERTY);
		if (name != null) {
			this.result.append(' ');
			name.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(TagElement node) {
		Object tagName= getAttribute(node, TagElement.TAG_NAME_PROPERTY);
		if (tagName != null) {
			this.result.append((String) tagName);
		}
		List list= getChildList(node, TagElement.FRAGMENTS_PROPERTY);
		for (int i= 0; i < list.size(); i++) {
			if (i > 0 || tagName != null) {
				this.result.append(' ');
			}
			ASTNode curr= (ASTNode) list.get(i);
			if (curr instanceof TagElement) {
				this.result.append('{');
				curr.accept(this);
				this.result.append('}');
			} else {
				curr.accept(this);
			}
		}
		return false;
	}

	@Override
	public boolean visit(TextBlock node) {
		this.result.append(getAttribute(node, TextBlock.ESCAPED_VALUE_PROPERTY));
		return false;
	}

	@Override
	public boolean visit(TextElement node) {
		this.result.append(getAttribute(node, TextElement.TEXT_PROPERTY));
		return false;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		ASTNode javadoc= getChildNode(node, AnnotationTypeDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		visitList(node, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		this.result.append("@interface ");//$NON-NLS-1$
		getChildNode(node, AnnotationTypeDeclaration.NAME_PROPERTY).accept(this);
		this.result.append('{');
		visitList(node, AnnotationTypeDeclaration.BODY_DECLARATIONS_PROPERTY, Util.EMPTY_STRING);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		ASTNode javadoc= getChildNode(node, AnnotationTypeMemberDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		visitList(node, AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		getChildNode(node, AnnotationTypeMemberDeclaration.TYPE_PROPERTY).accept(this);
		this.result.append(' ');
		getChildNode(node, AnnotationTypeMemberDeclaration.NAME_PROPERTY).accept(this);
		this.result.append("()");//$NON-NLS-1$
		ASTNode def= getChildNode(node, AnnotationTypeMemberDeclaration.DEFAULT_PROPERTY);
		if (def != null) {
			this.result.append(" default ");//$NON-NLS-1$
			def.accept(this);
		}
		this.result.append(';');
		return false;
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		this.result.append("for (");//$NON-NLS-1$
		getChildNode(node, EnhancedForStatement.PARAMETER_PROPERTY).accept(this);
		this.result.append(':');
		getChildNode(node, EnhancedForStatement.EXPRESSION_PROPERTY).accept(this);
		this.result.append(')');
		getChildNode(node, EnhancedForStatement.BODY_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		ASTNode javadoc= getChildNode(node, EnumConstantDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		visitList(node, EnumConstantDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		getChildNode(node, EnumConstantDeclaration.NAME_PROPERTY).accept(this);
		visitList(node, EnumConstantDeclaration.ARGUMENTS_PROPERTY, String.valueOf(','), String.valueOf('('), String.valueOf(')'));
		ASTNode classDecl= getChildNode(node, EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY);
		if (classDecl != null) {
			classDecl.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		ASTNode javadoc= getChildNode(node, EnumDeclaration.JAVADOC_PROPERTY);
		if (javadoc != null) {
			javadoc.accept(this);
		}
		visitList(node, EnumDeclaration.MODIFIERS2_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		this.result.append("enum ");//$NON-NLS-1$
		getChildNode(node, EnumDeclaration.NAME_PROPERTY).accept(this);
		this.result.append(' ');
		visitList(node, EnumDeclaration.SUPER_INTERFACE_TYPES_PROPERTY, String.valueOf(','), "implements ", Util.EMPTY_STRING); //$NON-NLS-1$

		this.result.append('{');
		visitList(node, EnumDeclaration.ENUM_CONSTANTS_PROPERTY, String.valueOf(','), Util.EMPTY_STRING, Util.EMPTY_STRING);
		visitList(node, EnumDeclaration.BODY_DECLARATIONS_PROPERTY, Util.EMPTY_STRING, String.valueOf(';'), Util.EMPTY_STRING);
		this.result.append('}');
		return false;
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
		getChildNode(node, ExpressionMethodReference.EXPRESSION_PROPERTY).accept(this);
		this.result.append("::"); //$NON-NLS-1$
		visitList(node, ExpressionMethodReference.TYPE_ARGUMENTS_PROPERTY, Util.EMPTY_STRING, String.valueOf('<'), String.valueOf('>'));
		node.getName().accept(this);
		return false;
	}

	@Override
	public boolean visit(MarkerAnnotation node) {
		this.result.append('@');
		getChildNode(node, MarkerAnnotation.TYPE_NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(MemberValuePair node) {
		getChildNode(node, MemberValuePair.NAME_PROPERTY).accept(this);
		this.result.append('=');
		getChildNode(node, MemberValuePair.VALUE_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(Modifier node) {
		this.result.append(getAttribute(node, Modifier.KEYWORD_PROPERTY).toString());
		return false;
	}

	@Override
	/*
	 * @see ASTVisitor#visit(ModuleModifier)
	 * @since 3.14
	 */
	public boolean visit(ModuleModifier node) {
		this.result.append(getAttribute(node, ModuleModifier.KEYWORD_PROPERTY).toString());
		return false;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		this.result.append('@');
		getChildNode(node, NormalAnnotation.TYPE_NAME_PROPERTY).accept(this);
		this.result.append('(');
		visitList(node, NormalAnnotation.VALUES_PROPERTY, ", "); //$NON-NLS-1$
		this.result.append(')');
		return false;
	}

	@Override
	public boolean visit(NameQualifiedType node) {
		getChildNode(node, NameQualifiedType.QUALIFIER_PROPERTY).accept(this);
		this.result.append('.');
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			visitList(node, NameQualifiedType.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, NameQualifiedType.NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(ParameterizedType node) {
		getChildNode(node, ParameterizedType.TYPE_PROPERTY).accept(this);
		this.result.append('<');
		visitList(node, ParameterizedType.TYPE_ARGUMENTS_PROPERTY, ", "); //$NON-NLS-1$
		this.result.append('>');
		return false;
	}

	@Override
	public boolean visit(QualifiedType node) {
		getChildNode(node, QualifiedType.QUALIFIER_PROPERTY).accept(this);
		this.result.append('.');
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			visitList(node, QualifiedType.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, QualifiedType.NAME_PROPERTY).accept(this);
		return false;
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		this.result.append('@');
		getChildNode(node, SingleMemberAnnotation.TYPE_NAME_PROPERTY).accept(this);
		this.result.append('(');
		getChildNode(node, SingleMemberAnnotation.VALUE_PROPERTY).accept(this);
		this.result.append(')');
		return false;
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		ASTNode qualifier = getChildNode(node, SuperMethodReference.QUALIFIER_PROPERTY);
		if (qualifier != null) {
			qualifier.accept(this);
			this.result.append('.');
		}
		this.result.append("super ::"); //$NON-NLS-1$
		visitList(node, SuperMethodReference.TYPE_ARGUMENTS_PROPERTY, Util.EMPTY_STRING, String.valueOf('<'), String.valueOf('>'));
		node.getName().accept(this);
		return false;
	}

	@Override
	public boolean visit(TypeMethodReference node) {
		getChildNode(node, TypeMethodReference.TYPE_PROPERTY).accept(this);
		this.result.append("::"); //$NON-NLS-1$
		visitList(node, TypeMethodReference.TYPE_ARGUMENTS_PROPERTY, Util.EMPTY_STRING, String.valueOf('<'), String.valueOf('>'));
		node.getName().accept(this);
		return false;
	}

	@Override
	public boolean visit(TypeParameter node) {
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			visitList(node, TypeParameter.MODIFIERS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		getChildNode(node, TypeParameter.NAME_PROPERTY).accept(this);
		visitList(node, TypeParameter.TYPE_BOUNDS_PROPERTY, " & ", " extends ", Util.EMPTY_STRING); //$NON-NLS-1$ //$NON-NLS-2$
		return false;
	}

	@Override
	public boolean visit(TypePattern node) {
		if (DOMASTUtil.isPatternSupported(node.getAST())) {
			node.getPatternVariable().accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(WildcardType node) {
		if (node.getAST().apiLevel() >= JLS8_INTERNAL) {
			visitList(node, WildcardType.ANNOTATIONS_PROPERTY, String.valueOf(' '), Util.EMPTY_STRING, String.valueOf(' '));
		}
		this.result.append('?');
		ASTNode bound = getChildNode(node, WildcardType.BOUND_PROPERTY);
		if (bound != null) {
			if (getBooleanAttribute(node, WildcardType.UPPER_BOUND_PROPERTY)) {
				this.result.append(" extends ");//$NON-NLS-1$
			} else {
				this.result.append(" super ");//$NON-NLS-1$
			}
			bound.accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(YieldStatement node) {
		if (node.getAST().apiLevel() >= JLS14_INTERNAL && node.isImplicit()  && node.getExpression() == null) {
			return false;
		}

		this.result.append("yield"); //$NON-NLS-1$

		ASTNode expression = getChildNode(node, YieldStatement.EXPRESSION_PROPERTY);
		if (expression != null ) {
			this.result.append(' ');
			expression.accept(this);
		}
		this.result.append(';');
		return false;
	}
}
