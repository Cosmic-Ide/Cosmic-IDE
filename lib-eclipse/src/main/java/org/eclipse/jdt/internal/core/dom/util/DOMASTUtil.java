/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

package org.eclipse.jdt.internal.core.dom.util;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Modifier;

public class DOMASTUtil {

	/**
	 * Validates if the given <code>AST</code> supports the provided <code>nodeType</code>. This API checks for node
	 * types supported from JLS 14 onwards and will return <code>true></code> for nodes added before JLS14.
	 *
	 * @param ast
	 *            the AST to be evaluated
	 * @param nodeType
	 *            the node type constant indicating a node of type to be evaluated
	 * @return <code>true</code> if the given <code>AST</code> supports the provided <code>nodeType</code> else
	 *         <code>false</code>
	 * @see ASTNode#getNodeType()
	 * @since 3.22
	 */
	private static boolean isNodeTypeSupportedinAST(AST ast, int nodeType) {
		return isNodeTypeSupportedinAST(ast.apiLevel(), ast.isPreviewEnabledSet(), nodeType);
	}

	/**
	 * Validates if the given <code>apiLevel</code> and <code>previewEnabled</code> supports the provided
	 * <code>nodeType</code>. This API checks for node types supported from JLS 14 onwards and will return
	 * <code>true></code> for nodes added before JLS14.
	 *
	 * @param apiLevel
	 *            the level to be checked
	 * @param previewEnabled
	 *            the preview feature to be considered
	 * @param nodeType
	 *            the node type constant indicating a node of type to be evaluated
	 * @return <code>true</code> if the given <code>AST</code> supports the provided <code>nodeType</code> else
	 *         <code>false</code>
	 * @see ASTNode#getNodeType()
	 * @since 3.22
	 */
	@SuppressWarnings("deprecation")
	private static boolean isNodeTypeSupportedinAST(int apiLevel, boolean previewEnabled, int nodeType) {
		switch (nodeType) {
			case ASTNode.SWITCH_EXPRESSION:
			case ASTNode.YIELD_STATEMENT:
				return apiLevel >= AST.JLS14;
			case ASTNode.TEXT_BLOCK:
				return apiLevel >= AST.JLS15;
			case ASTNode.RECORD_DECLARATION:
			case ASTNode.PATTERN_INSTANCEOF_EXPRESSION:
				return apiLevel >= AST.JLS16;
			case ASTNode.TYPE_PATTERN:
				return apiLevel == AST.getJLSLatest() && previewEnabled;
		}
		return false;
	}

	/**
	 * Validates if the given <code>apiLevel</code> and <code>previewEnabled</code> supports the provided
	 * <code>nodeType</code>. This API checks for node types supported from JLS 14 onwards and will return
	 * <code>true></code> for nodes added before JLS14.
	 *
	 * @param ast
	 *            the AST to be evaluated
	 * @param featureName
	 *            the feature name constant indicating the feature to be evaluated
	 * @return <code>true</code> if the given <code>AST</code> supports the provided <code>nodeType</code> else
	 *         <code>false</code>
	 * @see ASTNode#getNodeType()
	 * @since 3.22
	 */
	public static boolean isFeatureSupportedinAST(AST ast, int featureName) {
		switch (featureName) {
			case Modifier.SEALED:
				return ast.apiLevel() >= AST.JLS17;
			case Modifier.NON_SEALED:
				return ast.apiLevel() >= AST.JLS17;
		}
		return false;
	}

	/**
	 * Validates if the given <code>apiLevel</code> and <code>previewEnabled</code> supports the provided
	 * <code>nodeType</code>. This API checks for node types supported from JLS 14 onwards and will return
	 * <code>true></code> for nodes added before JLS14.
	 *
	 * @param apiLevel
	 *            the level to be checked
	 * @param previewEnabled
	 *            the preview feature to be considered
	 * @param featureName
	 *            the feature name constant indicating the feature to be evaluated
	 * @return <code>true</code> if the given <code>AST</code> supports the provided <code>nodeType</code> else
	 *         <code>false</code>
	 * @see ASTNode#getNodeType()
	 * @since 3.22
	 */
	public static boolean isFeatureSupportedinAST(int apiLevel, boolean previewEnabled, int featureName) {
		switch (featureName) {
			case Modifier.SEALED:
				return apiLevel >= AST.JLS17;
			case Modifier.NON_SEALED:
				return apiLevel >= AST.JLS17;
		}
		return false;
	}

	/**
	 * Validates if the given <code>apiLevel</code> and <code>previewEnabled</code> supports the provided
	 * <code>nodeType</code>. This API checks for node types supported from JLS 14 onwards and will return
	 * <code>true></code> for nodes added before JLS14.
	 *
	 * @param apiLevel
	 *            the level to be checked
	 * @param featureName
	 *            the feature name constant indicating the feature to be evaluated
	 * @return <code>true</code> if the given <code>AST</code> supports the provided <code>nodeType</code> else
	 *         <code>false</code>
	 * @see ASTNode#getNodeType()
	 * @since 3.27
	 */
	public static boolean isFeatureSupportedinAST(int apiLevel, int featureName) {
		switch (featureName) {
			case Modifier.SEALED:
				return apiLevel >= AST.JLS17;
			case Modifier.NON_SEALED:
				return apiLevel >= AST.JLS17;
		}
		return false;
	}

	public static boolean isPreviewEnabled(int apiLevel, boolean previewEnabled) {
		return (apiLevel == AST.JLS17 && previewEnabled);
	}

	public static boolean isSwitchExpressionSupported(AST ast) {
		return isNodeTypeSupportedinAST(ast, ASTNode.SWITCH_EXPRESSION);
	}

	public static boolean isYieldStatementSupported(AST ast) {
		return isNodeTypeSupportedinAST(ast, ASTNode.YIELD_STATEMENT);
	}

	public static boolean isTextBlockSupported(AST ast) {
		return isNodeTypeSupportedinAST(ast, ASTNode.TEXT_BLOCK);
	}

	public static boolean isRecordDeclarationSupported(AST ast) {
		return isNodeTypeSupportedinAST(ast, ASTNode.RECORD_DECLARATION);
	}

	public static boolean isRecordDeclarationSupported(int apiLevel) {
		return isNodeTypeSupportedinAST(apiLevel, true, ASTNode.RECORD_DECLARATION);
	}

	public static boolean isPatternInstanceofExpressionSupported(AST ast) {
		return isNodeTypeSupportedinAST(ast, ASTNode.PATTERN_INSTANCEOF_EXPRESSION);
	}

	public static boolean isPatternSupported(AST ast) {
		return isNodeTypeSupportedinAST(ast, ASTNode.TYPE_PATTERN);
	}

	public static boolean isPatternSupported(int apiLevel, boolean previewEnabled) {
		return isNodeTypeSupportedinAST(apiLevel, previewEnabled, ASTNode.TYPE_PATTERN);
	}

	@SuppressWarnings("deprecation")
	public static void checkASTLevel(int level) {
		// Clients can use AST.getJLSLatest()
		if(level >=AST.JLS8 && level <= AST.getJLSLatest() )
			return;
		switch (level) {
	        case AST.JLS2 :
	        case AST.JLS3 :
	        case AST.JLS4 :
	        	return;
		}
		throw new IllegalArgumentException(Integer.toString(level));
	}

	private static final String[] AST_COMPLIANCE_MAP = {"-1","-1",JavaCore.VERSION_1_2, JavaCore.VERSION_1_3, JavaCore.VERSION_1_7, //$NON-NLS-1$ //$NON-NLS-2$
			JavaCore.VERSION_1_7, JavaCore.VERSION_1_7, JavaCore.VERSION_1_7, JavaCore.VERSION_1_8, JavaCore.VERSION_9, JavaCore.VERSION_10,
			JavaCore.VERSION_11, JavaCore.VERSION_12, JavaCore.VERSION_13, JavaCore.VERSION_14, JavaCore.VERSION_15, JavaCore.VERSION_16, JavaCore.VERSION_17};

	/**
	 * Calculates the JavaCore Option value string corresponding to the input ast level.
	 * AST Level 4 is used for Java versions 1.4 to 1.7 and is converted to compliance level 7
	 * if input ast level is out of boundary, latest compliance will be returned
	 * @param astLevel
	 * @return JavaCore Option value string corresponding to the ast level
	 */
	@SuppressWarnings("deprecation")
	public static String getCompliance(int astLevel) {
		if (astLevel < AST.JLS2 && astLevel > AST.getJLSLatest()) return JavaCore.latestSupportedJavaVersion();
		return AST_COMPLIANCE_MAP[astLevel];
	}

}
