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
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/

package org.eclipse.jdt.core.dom.rewrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.internal.core.dom.rewrite.imports.ImportRewriteAnalyzer;
import org.eclipse.jdt.internal.core.dom.rewrite.imports.ImportRewriteConfiguration;
import org.eclipse.jdt.internal.core.dom.rewrite.imports.ImportRewriteConfiguration.ImplicitImportIdentification;
import org.eclipse.jdt.internal.core.dom.rewrite.imports.ImportRewriteConfiguration.ImportContainerSorting;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;


/**
 * The {@link ImportRewrite} helps updating imports following a import order and on-demand imports threshold as configured by a project.
 * <p>
 * The import rewrite is created on a compilation unit and collects references to types that are added or removed. When adding imports, e.g. using
 * {@link #addImport(String)}, the import rewrite evaluates if the type can be imported and returns the a reference to the type that can be used in code.
 * This reference is either unqualified if the import could be added, or fully qualified if the import failed due to a conflict with another element of the same name.
 * </p>
 * <p>
 * On {@link #rewriteImports(IProgressMonitor)} the rewrite translates these descriptions into
 * text edits that can then be applied to the original source. The rewrite infrastructure tries to generate minimal text changes and only
 * works on the import statements. It is possible to combine the result of an import rewrite with the result of a {@link org.eclipse.jdt.core.dom.rewrite.ASTRewrite}
 * as long as no import statements are modified by the AST rewrite.
 * </p>
 * <p>The options controlling the import order and on-demand thresholds are:
 * <ul><li>{@link #setImportOrder(String[])} specifies the import groups and their preferred order</li>
 * <li>{@link #setOnDemandImportThreshold(int)} specifies the number of imports in a group needed for a on-demand import statement (star import)</li>
 * <li>{@link #setStaticOnDemandImportThreshold(int)} specifies the number of static imports in a group needed for a on-demand import statement (star import)</li>
 * </ul>
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 * @since 3.2
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ImportRewrite {
	/**
	 * Used to determine how a type will be used, so that unwanted annotations can be filtered,
	 * which is in particular relevant for avoiding redundant null annotations in the scope of {@code @NonNullByDefault}.
	 * This enum is a superset of org.eclipse.jdt.annotation.DefaultLocation, and roughly corresponds
	 * to the classification of type locations as introduced by JSR 308.
	 *
	 * @since 3.13
	 */
	public enum TypeLocation {
		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.PARAMETER
		 *
		 * @since 3.13
		 */
		PARAMETER,

		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE
		 *
		 * @since 3.13
		 */
		RETURN_TYPE,

		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.FIELD
		 *
		 * @since 3.13
		 */
		FIELD,

		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.TYPE_PARAMETER
		 *
		 * @since 3.13
		 */
		TYPE_PARAMETER,

		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND
		 *
		 * @since 3.13
		 */
		TYPE_BOUND,

		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT
		 *
		 * @since 3.13
		 */
		TYPE_ARGUMENT,

		/**
		 * see org.eclipse.jdt.annotation.DefaultLocation.ARRAY_CONTENTS
		 *
		 * @since 3.13
		 */
		ARRAY_CONTENTS,

		/**
		 * The special value {@link #LOCAL_VARIABLE} is used for local variables: their nullness is determines by flow analysis,
		 * so top level nullness annotations are usually not needed for local variables (unless their type is a free
		 * type variable). Does not correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		LOCAL_VARIABLE,

		/**
		 * The special value {@link #CAST} is used for casts.
		 * Casts are never affected by {@code @NonNullByDefault}
		 * Does not correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		CAST,

		/**
		 * The special value {@link #INSTANCEOF} is used for {@code instanceof} expressions.
		 * Null annotations are not supported in this location.
		 * Does not correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		INSTANCEOF,

		/**
		 * The special value {@link #NEW} is used for {@code new} expressions (object allocations).
		 * Null annotations are not supported in this location.
		 * Does not correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		NEW,

		/**
		 * The special value {@link #RECEIVER} is used for the receiver type in a method declaration or method reference.
		 * Null annotations are not supported in this location.
		 * Does not correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		RECEIVER,

		/**
		 * The special value {@link #EXCEPTION} is used for exception types in catch and throws declarations, which are
		 * implicitly non-null. Does not correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		EXCEPTION,

		/**
		 * The special value {@link #OTHER} is used for locations where type annotations are illegal, like type literals
		 * (X.class), annotations, or as scope for static field accesses. Does not correspond to a value in
		 * org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		OTHER,

		/**
		 * The special value {@link #UNKNOWN} is used for invocations that don't specify the intended type usage. Does not
		 * correspond to a value in org.eclipse.jdt.annotation.DefaultLocation.
		 *
		 * @since 3.13
		 */
		UNKNOWN,
	}

	/**
	 * A {@link ImportRewrite.ImportRewriteContext} can optionally be used in e.g. {@link ImportRewrite#addImport(String, ImportRewrite.ImportRewriteContext)} to
	 * give more information about the types visible in the scope. These types can be for example inherited inner types where it is
	 * unnecessary to add import statements for.
	 *
	 * <p>
	 * This class can be implemented by clients.
	 * </p>
	 */
	public static abstract class ImportRewriteContext {

		/**
		 * Result constant signaling that the given element is know in the context.
		 */
		public final static int RES_NAME_FOUND= 1;

		/**
		 * Result constant signaling that the given element is not know in the context.
		 */
		public final static int RES_NAME_UNKNOWN= 2;

		/**
		 * Result constant signaling that the given element is conflicting with an other element in the context.
		 */
		public final static int RES_NAME_CONFLICT= 3;

		/**
		 * Result constant signaling that the given element must be imported explicitly (and must not be folded into
		 * an on-demand import or filtered as an implicit import).
		 *
		 * @since 3.11
		 */
		public final static int RES_NAME_UNKNOWN_NEEDS_EXPLICIT_IMPORT= 4;

		/**
		 * Kind constant specifying that the element is a type import.
		 */
		public final static int KIND_TYPE= 1;

		/**
		 * Kind constant specifying that the element is a static field import.
		 */
		public final static int KIND_STATIC_FIELD= 2;

		/**
		 * Kind constant specifying that the element is a static method import.
		 */
		public final static int KIND_STATIC_METHOD= 3;

		/**
		 * Searches for the given element in the context and reports if the element is known ({@link #RES_NAME_FOUND}),
		 * unknown ({@link #RES_NAME_UNKNOWN}), unknown in the context but known to require an explicit import
		 * ({@link #RES_NAME_UNKNOWN_NEEDS_EXPLICIT_IMPORT}), or if its name conflicts ({@link #RES_NAME_CONFLICT})
		 * with an other element.
		 *
		 * @param qualifier The qualifier of the element, can be package or the qualified name of a type
		 * @param name The simple name of the element; either a type, method or field name or * for on-demand imports.
		 * @param kind The kind of the element. Can be either {@link #KIND_TYPE}, {@link #KIND_STATIC_FIELD} or
		 * {@link #KIND_STATIC_METHOD}. Implementors should be prepared for new, currently unspecified kinds and return
		 * {@link #RES_NAME_UNKNOWN} by default.
		 * @return Returns the result of the lookup. Can be either {@link #RES_NAME_FOUND}, {@link #RES_NAME_UNKNOWN},
		 * {@link #RES_NAME_CONFLICT}, or {@link #RES_NAME_UNKNOWN_NEEDS_EXPLICIT_IMPORT}.
		 */
		public abstract int findInContext(String qualifier, String name, int kind);

		/**
		 * This method can be overridden in subclasses to remove annotations that are redundant, e.g. because of NonNullByDefault.
		 * @param annotations The array of type annotations to be filtered.
		 * @param location Specifies how the type being annotated will be used.
		 * @param type the type being annotated
		 * @return Returns the annotations that should be added to the type.
		 * @since 3.13
		 */
		public IAnnotationBinding[] removeRedundantTypeAnnotations(IAnnotationBinding[] annotations, TypeLocation location, ITypeBinding type) {
			return annotations;
		}
	}

	private static final char STATIC_PREFIX= 's';
	private static final char NORMAL_PREFIX= 'n';

	/** @deprecated using deprecated code */
	private static final int JLS8_INTERNAL = AST.JLS8;

	private final ImportRewriteContext defaultContext;

	private final ICompilationUnit compilationUnit;
	private final CompilationUnit astRoot;

	private final boolean restoreExistingImports;
	private final List existingImports;
	private final Map importsKindMap;

	private String[] importOrder;
	private int importOnDemandThreshold;
	private int staticImportOnDemandThreshold;

	private List<String> addedImports;
	private List<String> removedImports;

	/**
	 * Simple names of non-static imports which must not be reduced into on-demand imports
	 * or filtered out as implicit.
	 */
	private Set<String> typeExplicitSimpleNames;

	/**
	 * Simple names of static imports which must not be reduced into on-demand imports
	 * or filtered out as implicit.
	 */
	private Set<String> staticExplicitSimpleNames;

	private String[] createdImports;
	private String[] createdStaticImports;

	private boolean filterImplicitImports;
	private boolean useContextToFilterImplicitImports;

	/**
	 * Creates an {@link ImportRewrite} from an {@link ICompilationUnit}. If <code>restoreExistingImports</code>
	 * is <code>true</code>, all existing imports are kept, and new imports will be inserted at best matching locations. If
	 * <code>restoreExistingImports</code> is <code>false</code>, the existing imports will be removed and only the
	 * newly added imports will be created.
	 * <p>
	 * Note that {@link #create(CompilationUnit, boolean)} is more efficient than this method if an AST for
	 * the compilation unit is already available.
	 * </p>
	 * @param cu the compilation unit to create the imports for
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the created import rewriter.
	 * @throws JavaModelException thrown when the compilation unit could not be accessed.
	 */
	public static ImportRewrite create(ICompilationUnit cu, boolean restoreExistingImports) throws JavaModelException {
		if (cu == null) {
			throw new IllegalArgumentException("Compilation unit must not be null"); //$NON-NLS-1$
		}
		List existingImport= null;
		if (restoreExistingImports) {
			existingImport= new ArrayList();
			IImportDeclaration[] imports= cu.getImports();
			for (int i= 0; i < imports.length; i++) {
				IImportDeclaration curr= imports[i];
				char prefix= Flags.isStatic(curr.getFlags()) ? STATIC_PREFIX : NORMAL_PREFIX;
				existingImport.add(prefix + curr.getElementName());
			}
		}
		return new ImportRewrite(cu, null, existingImport);
	}

	/**
	 * Creates an {@link ImportRewrite} from an AST ({@link CompilationUnit}). The AST has to be created from an
	 * {@link ICompilationUnit}, that means {@link ASTParser#setSource(ICompilationUnit)} has been used when creating the
	 * AST. If <code>restoreExistingImports</code> is <code>true</code>, all existing imports are kept, and new imports
	 * will be inserted at best matching locations. If <code>restoreExistingImports</code> is <code>false</code>, the
	 * existing imports will be removed and only the newly added imports will be created.
	 * <p>
	 * Note that this method is more efficient than using {@link #create(ICompilationUnit, boolean)} if an AST is already available.
	 * </p>
	 * @param astRoot the AST root node to create the imports for
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the created import rewriter.
	 * @throws IllegalArgumentException thrown when the passed AST is null or was not created from a compilation unit.
	 */
	public static ImportRewrite create(CompilationUnit astRoot, boolean restoreExistingImports) {
		if (astRoot == null) {
			throw new IllegalArgumentException("AST must not be null"); //$NON-NLS-1$
		}
		ITypeRoot typeRoot = astRoot.getTypeRoot();
		if (!(typeRoot instanceof ICompilationUnit)) {
			throw new IllegalArgumentException("AST must have been constructed from a Java element"); //$NON-NLS-1$
		}
		List existingImport= null;
		if (restoreExistingImports) {
			existingImport= new ArrayList();
			List imports= astRoot.imports();
			for (int i= 0; i < imports.size(); i++) {
				ImportDeclaration curr= (ImportDeclaration) imports.get(i);
				StringBuilder buf= new StringBuilder();
				buf.append(curr.isStatic() ? STATIC_PREFIX : NORMAL_PREFIX).append(curr.getName().getFullyQualifiedName());
				if (curr.isOnDemand()) {
					if (buf.length() > 1)
						buf.append('.');
					buf.append('*');
				}
				existingImport.add(buf.toString());
			}
		}
		return new ImportRewrite((ICompilationUnit) typeRoot, astRoot, existingImport);
	}

	private ImportRewrite(ICompilationUnit cu, CompilationUnit astRoot, List existingImports) {
		this.compilationUnit= cu;
		this.astRoot= astRoot; // might be null
		if (existingImports != null) {
			this.existingImports= existingImports;
			this.restoreExistingImports= !existingImports.isEmpty();
		} else {
			this.existingImports= new ArrayList();
			this.restoreExistingImports= false;
		}
		this.filterImplicitImports= true;
		// consider that no contexts are used
		this.useContextToFilterImplicitImports = false;

		this.defaultContext= new ImportRewriteContext() {
			@Override
			public int findInContext(String qualifier, String name, int kind) {
				return findInImports(qualifier, name, kind);
			}
		};
		this.addedImports= new ArrayList<>();
		this.removedImports= new ArrayList<>();
		this.typeExplicitSimpleNames = new HashSet<>();
		this.staticExplicitSimpleNames = new HashSet<>();
		this.createdImports= null;
		this.createdStaticImports= null;

		this.importOrder= CharOperation.NO_STRINGS;
		this.importOnDemandThreshold= 99;
		this.staticImportOnDemandThreshold= 99;

		this.importsKindMap = new HashMap();
	}


	 /**
	 * Defines the import groups and order to be used by the {@link ImportRewrite}.
	 * Imports are added to the group matching their qualified name most. The empty group name groups all imports not matching
	 * any other group. Static imports are managed in separate groups. Static import group names are prefixed with a '#' character.
	 * @param order A list of strings defining the import groups. A group name must be a valid package name or empty. If can be
	 * prefixed by the '#' character for static import groups
	 */
	public void setImportOrder(String[] order) {
		if (order == null)
			throw new IllegalArgumentException("Order must not be null"); //$NON-NLS-1$
		this.importOrder= order;
	}

	 /**
	 *	Sets the on-demand import threshold for normal (non-static) imports.
	 *	This threshold defines the number of imports that need to be in a group to use
	 * a on-demand (star) import declaration instead.
	 *
	 * @param threshold a positive number defining the on-demand import threshold
	 * for normal (non-static) imports.
	 * @throws IllegalArgumentException a {@link IllegalArgumentException} is thrown
	 * if the number is not positive.
     */
	public void setOnDemandImportThreshold(int threshold) {
		if (threshold <= 0)
			throw new IllegalArgumentException("Threshold must be positive."); //$NON-NLS-1$
		this.importOnDemandThreshold= threshold;
	}

	 /**
	 *	Sets the on-demand import threshold for static imports.
	 *	This threshold defines the number of imports that need to be in a group to use
	 * a on-demand (star) import declaration instead.
	 *
	 * @param threshold a positive number defining the on-demand import threshold
	 * for normal (non-static) imports.
	 * @throws IllegalArgumentException a {@link IllegalArgumentException} is thrown
	 * if the number is not positive.
     */
	public void setStaticOnDemandImportThreshold(int threshold) {
		if (threshold <= 0)
			throw new IllegalArgumentException("Threshold must be positive."); //$NON-NLS-1$
		this.staticImportOnDemandThreshold= threshold;
	}

	/**
	 * The compilation unit for which this import rewrite was created for.
	 * @return the compilation unit for which this import rewrite was created for.
	 */
	public ICompilationUnit getCompilationUnit() {
		return this.compilationUnit;
	}

	/**
	 * Returns the default rewrite context that only knows about the imported types. Clients
	 * can write their own context and use the default context for the default behavior.
	 * @return the default import rewrite context.
	 */
	public ImportRewriteContext getDefaultImportRewriteContext() {
		return this.defaultContext;
	}

	/**
	 * Specifies that implicit imports (for types in <code>java.lang</code>, types in the same package as the rewrite
	 * compilation unit, and types in the compilation unit's main type) should not be created, except if necessary to
	 * resolve an on-demand import conflict.
	 * <p>
	 * The filter is enabled by default.
	 * </p>
	 * <p>
	 * Note: {@link #setUseContextToFilterImplicitImports(boolean)} can be used to filter implicit imports
	 * when a context is used.
	 * </p>
	 *
	 * @param filterImplicitImports
	 *            if <code>true</code>, implicit imports will be filtered
	 *
	 * @see #setUseContextToFilterImplicitImports(boolean)
	 */
	public void setFilterImplicitImports(boolean filterImplicitImports) {
		this.filterImplicitImports= filterImplicitImports;
	}

	/**
	* Sets whether a context should be used to properly filter implicit imports.
	* <p>
	* By default, the option is disabled to preserve pre-3.6 behavior.
	* </p>
	* <p>
	* When this option is set, the context passed to the <code>addImport*(...)</code> methods is used to determine
	* whether an import can be filtered because the type is implicitly visible. Note that too many imports
	* may be kept if this option is set and <code>addImport*(...)</code> methods are called without a context.
	* </p>
	*
	* @param useContextToFilterImplicitImports the given setting
	*
	* @see #setFilterImplicitImports(boolean)
	* @since 3.6
	*/
	public void setUseContextToFilterImplicitImports(boolean useContextToFilterImplicitImports) {
		this.useContextToFilterImplicitImports = useContextToFilterImplicitImports;
	}

	private static int compareImport(char prefix, String qualifier, String name, String curr) {
		if (curr.charAt(0) != prefix || !curr.endsWith(name)) {
			return ImportRewriteContext.RES_NAME_UNKNOWN;
		}

		curr= curr.substring(1); // remove the prefix

		if (curr.length() == name.length()) {
			if (qualifier.length() == 0) {
				return ImportRewriteContext.RES_NAME_FOUND;
			}
			return ImportRewriteContext.RES_NAME_CONFLICT;
		}
		// at this place: curr.length > name.length

		int dotPos= curr.length() - name.length() - 1;
		if (curr.charAt(dotPos) != '.') {
			return ImportRewriteContext.RES_NAME_UNKNOWN;
		}
		if (qualifier.length() != dotPos || !curr.startsWith(qualifier)) {
			return ImportRewriteContext.RES_NAME_CONFLICT;
		}
		return ImportRewriteContext.RES_NAME_FOUND;
	}

	/**
	 * Not API, package visibility as accessed from an anonymous type
	 */
	/* package */ final int findInImports(String qualifier, String name, int kind) {
		boolean allowAmbiguity=  (kind == ImportRewriteContext.KIND_STATIC_METHOD) || (name.length() == 1 && name.charAt(0) == '*');
		List imports= this.existingImports;
		char prefix= (kind == ImportRewriteContext.KIND_TYPE) ? NORMAL_PREFIX : STATIC_PREFIX;

		for (int i= imports.size() - 1; i >= 0 ; i--) {
			String curr= (String) imports.get(i);
			int res= compareImport(prefix, qualifier, name, curr);
			if (res != ImportRewriteContext.RES_NAME_UNKNOWN) {
				if (!allowAmbiguity || res == ImportRewriteContext.RES_NAME_FOUND) {
					if (prefix != STATIC_PREFIX) {
						return res;
					}
					Object currKind = this.importsKindMap.get(curr.substring(1));
					if (currKind != null && currKind.equals(this.importsKindMap.get(qualifier + '.' + name))) {
						return res;
					}
				}
			}
		}

		String packageName= this.compilationUnit.getParent().getElementName();
		if (kind == ImportRewriteContext.KIND_TYPE) {
			if (this.filterImplicitImports && this.useContextToFilterImplicitImports) {
				String mainTypeSimpleName= JavaCore.removeJavaLikeExtension(this.compilationUnit.getElementName());
				String mainTypeName= Util.concatenateName(packageName, mainTypeSimpleName, '.');
				if (qualifier.equals(packageName)
						|| mainTypeName.equals(Util.concatenateName(qualifier, name, '.'))) {
					return ImportRewriteContext.RES_NAME_FOUND;
				}

				if (this.astRoot != null) {
					List<AbstractTypeDeclaration> types = this.astRoot.types();
					int nTypes = types.size();
					for (int i = 0; i < nTypes; i++) {
						AbstractTypeDeclaration type = types.get(i);
						SimpleName simpleName = type.getName();
						if (simpleName.getIdentifier().equals(name)) {
							return qualifier.equals(packageName)
									? ImportRewriteContext.RES_NAME_FOUND
									: ImportRewriteContext.RES_NAME_CONFLICT;
						}
					}
				} else {
					try {
						IType[] types = this.compilationUnit.getTypes();
						int nTypes = types.length;
						for (int i = 0; i < nTypes; i++) {
							IType type = types[i];
							String typeName = type.getElementName();
							if (typeName.equals(name)) {
								return qualifier.equals(packageName)
										? ImportRewriteContext.RES_NAME_FOUND
										: ImportRewriteContext.RES_NAME_CONFLICT;
							}
						}
					} catch (JavaModelException e) {
						// don't want to throw an exception here
					}
				}
			}
		}

		return ImportRewriteContext.RES_NAME_UNKNOWN;
	}

	/**
	 * Adds the necessary imports for the given annotation binding to the rewriter's record
	 * and returns an {@link Annotation} that can be used in the code.
	 * <p>
	 * No imports are added for types that are already known. If an import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records newly added imports.
	 * </p>
	 * @param annotation the annotation to be added
	 * @param ast the AST to create the returned annotation for
	 * @param context an optional context that knows about types visible in the current scope or <code>null</code>
	 * to use the default context (only using the available imports)
	 * @return an annotation node. The returned annotation contains unqualified type names where
	 * an import could be added or was already known. Type names are fully qualified if an import conflict prevented an import.
	 *
	 * @since 3.10
	 */
	public Annotation addAnnotation(IAnnotationBinding annotation, AST ast, ImportRewriteContext context) {
		Type type = addImport(annotation.getAnnotationType(), ast, context, TypeLocation.OTHER);
		Name name;
		if (type instanceof SimpleType) {
			SimpleType simpleType = (SimpleType) type;
			name = simpleType.getName();
			// cut 'name' loose from its parent, so that it can be reused
			simpleType.setName(ast.newName("a")); //$NON-NLS-1$
		} else {
			name = ast.newName("invalid"); //$NON-NLS-1$
		}

		IMemberValuePairBinding[] mvps= annotation.getDeclaredMemberValuePairs();
		if (mvps.length == 0) {
			MarkerAnnotation result = ast.newMarkerAnnotation();
			result.setTypeName(name);
			return result;
		} else if (mvps.length == 1 && "value".equals(mvps[0].getName())) { //$NON-NLS-1$
			SingleMemberAnnotation result= ast.newSingleMemberAnnotation();
			result.setTypeName(name);
			Object value = mvps[0].getValue();
			if (value != null)
				result.setValue(addAnnotation(ast, value, context));
			return result;
		} else {
			NormalAnnotation result = ast.newNormalAnnotation();
			result.setTypeName(name);
			for (int i= 0; i < mvps.length; i++) {
				IMemberValuePairBinding mvp = mvps[i];
				MemberValuePair mvpNode = ast.newMemberValuePair();
				mvpNode.setName(ast.newSimpleName(mvp.getName()));
				Object value = mvp.getValue();
				if (value != null)
					mvpNode.setValue(addAnnotation(ast, value, context));
				result.values().add(mvpNode);
			}
			return result;
		}
	}

	/**
	 * Adds a new import to the rewriter's record and returns a {@link Type} node that can be used
	 * in the code as a reference to the type. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param typeSig the signature of the type to be added.
	 * @param ast the AST to create the returned type for.
	 * @return a type node for the given type signature. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 */
	public Type addImportFromSignature(String typeSig, AST ast) {
		return addImportFromSignature(typeSig, ast, this.defaultContext);
	}

	/**
	 * Adds a new import to the rewriter's record and returns a {@link Type} node that can be used
	 * in the code as a reference to the type. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param typeSig the signature of the type to be added.
	 * @param ast the AST to create the returned type for.
	 * @param context an optional context that knows about types visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @return a type node for the given type signature. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 */
	public Type addImportFromSignature(String typeSig, AST ast, ImportRewriteContext context) {
		if (typeSig == null || typeSig.length() == 0) {
			throw new IllegalArgumentException("Invalid type signature: empty or null"); //$NON-NLS-1$
		}
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				return ast.newPrimitiveType(PrimitiveType.toCode(Signature.toString(typeSig)));
			case Signature.ARRAY_TYPE_SIGNATURE:
				Type elementType= addImportFromSignature(Signature.getElementType(typeSig), ast, context);
				return ast.newArrayType(elementType, Signature.getArrayCount(typeSig));
			case Signature.CLASS_TYPE_SIGNATURE:
				String erasureSig= Signature.getTypeErasure(typeSig);

				String erasureName= Signature.toString(erasureSig);
				if (erasureSig.charAt(0) == Signature.C_RESOLVED) {
					erasureName= internalAddImport(erasureName, context, false);
				}
				Type baseType= ast.newSimpleType(ast.newName(erasureName));
				String[] typeArguments= Signature.getTypeArguments(typeSig);
				if (typeArguments.length > 0) {
					ParameterizedType type= ast.newParameterizedType(baseType);
					List argNodes= type.typeArguments();
					for (int i= 0; i < typeArguments.length; i++) {
						String curr= typeArguments[i];
						if (containsNestedCapture(curr)) { // see bug 103044
							argNodes.add(ast.newWildcardType());
						} else {
							argNodes.add(addImportFromSignature(curr, ast, context));
						}
					}
					return type;
				}
				return baseType;
			case Signature.TYPE_VARIABLE_SIGNATURE:
				return ast.newSimpleType(ast.newSimpleName(Signature.toString(typeSig)));
			case Signature.WILDCARD_TYPE_SIGNATURE:
				WildcardType wildcardType= ast.newWildcardType();
				char ch= typeSig.charAt(0);
				if (ch != Signature.C_STAR) {
					Type bound= addImportFromSignature(typeSig.substring(1), ast, context);
					wildcardType.setBound(bound, ch == Signature.C_EXTENDS);
				}
				return wildcardType;
			case Signature.CAPTURE_TYPE_SIGNATURE:
				return addImportFromSignature(typeSig.substring(1), ast, context);
			default:
				throw new IllegalArgumentException("Unknown type signature kind: " + typeSig); //$NON-NLS-1$
		}
	}



	/**
	 * Adds a new import to the rewriter's record and returns a type reference that can be used
	 * in the code. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored. Type annotations are ignored.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding the signature of the type to be added.
	 * @return a type reference for the given type binding. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 */
	public String addImport(ITypeBinding binding) {
		return addImport(binding, this.defaultContext);
	}

	/**
	 * Adds a new import to the rewriter's record and returns a type reference that can be used
	 * in the code. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored. Type annotations are ignored.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding the signature of the type to be added.
	 * @param context an optional context that knows about types visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @return a type reference for the given type binding. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 */
	public String addImport(ITypeBinding binding, ImportRewriteContext context) {
		if (binding.isPrimitive() || binding.isTypeVariable() || binding.isRecovered()) {
			return binding.getName();
		}

		ITypeBinding normalizedBinding= normalizeTypeBinding(binding);
		if (normalizedBinding == null) {
			return "invalid"; //$NON-NLS-1$
		}
		if (normalizedBinding.isWildcardType()) {
			StringBuilder res= new StringBuilder("?"); //$NON-NLS-1$
			ITypeBinding bound= normalizedBinding.getBound();
			if (bound != null && !bound.isWildcardType() && !bound.isCapture()) { // bug 95942
				if (normalizedBinding.isUpperbound()) {
					res.append(" extends "); //$NON-NLS-1$
				} else {
					res.append(" super "); //$NON-NLS-1$
				}
				res.append(addImport(bound, context));
			}
			return res.toString();
		}

		if (normalizedBinding.isArray()) {
			StringBuilder res= new StringBuilder(addImport(normalizedBinding.getElementType(), context));
			for (int i= normalizedBinding.getDimensions(); i > 0; i--) {
				res.append("[]"); //$NON-NLS-1$
			}
			return res.toString();
		}

		String qualifiedName= getRawQualifiedName(normalizedBinding);
		if (qualifiedName.length() > 0) {
			String str= internalAddImport(qualifiedName, context, isTypeInUnnamedPackage(normalizedBinding));

			ITypeBinding[] typeArguments= normalizedBinding.getTypeArguments();
			if (typeArguments.length > 0) {
				StringBuilder res= new StringBuilder(str);
				res.append('<');
				for (int i= 0; i < typeArguments.length; i++) {
					if (i > 0) {
						res.append(',');
					}
					ITypeBinding curr= typeArguments[i];
					if (containsNestedCapture(curr, false)) { // see bug 103044
						res.append('?');
					} else {
						res.append(addImport(curr, context));
					}
				}
				res.append('>');
				return res.toString();
			}
			return str;
		}
		return getRawName(normalizedBinding);
	}

	private boolean containsNestedCapture(ITypeBinding binding, boolean isNested) {
		if (binding == null || binding.isPrimitive() || binding.isTypeVariable()) {
			return false;
		}
		if (binding.isCapture()) {
			if (isNested) {
				return true;
			}
			return containsNestedCapture(binding.getWildcard(), true);
		}
		if (binding.isWildcardType()) {
			return containsNestedCapture(binding.getBound(), true);
		}
		if (binding.isArray()) {
			return containsNestedCapture(binding.getElementType(), true);
		}
		ITypeBinding[] typeArguments= binding.getTypeArguments();
		for (int i= 0; i < typeArguments.length; i++) {
			if (containsNestedCapture(typeArguments[i], true)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsNestedCapture(String signature) {
		return signature.length() > 1 && signature.indexOf(Signature.C_CAPTURE, 1) != -1;
	}

	private static ITypeBinding normalizeTypeBinding(ITypeBinding binding) {
		if (binding != null && !binding.isNullType() && !"void".equals(binding.getName())) { //$NON-NLS-1$
			if (binding.isAnonymous()) {
				ITypeBinding[] baseBindings= binding.getInterfaces();
				if (baseBindings.length > 0) {
					return baseBindings[0];
				}
				return binding.getSuperclass();
			}
			if (binding.isCapture()) {
				return binding.getWildcard();
			}
			return binding;
		}
		return null;
	}

	/**
	 * Adds a new import to the rewriter's record and returns a {@link Type} that can be used
	 * in the code. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored. If type annotations or type arguments are present at any point, the import is added up to that point and
	 * the type is retained from that point with type annotations and type arguments.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding the signature of the type to be added.
	 * @param ast the AST to create the returned type for.
	 * @return a type node for the given type binding. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 */
	public Type addImport(ITypeBinding binding, AST ast) {
		return addImport(binding, ast, this.defaultContext, TypeLocation.UNKNOWN);
	}

	/**
	 * Adds a new import to the rewriter's record and returns a {@link Type} that can be used
	 * in the code. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored. If type annotations or type arguments are present at any point, the import is added up to that point and
	 * the type is retained from that point with type annotations and type arguments
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding the signature of the type to be added.
	 * @param ast the AST to create the returned type for.
	 * @param context an optional context that knows about types visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @return a type node for the given type binding. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 */
	public Type addImport(ITypeBinding binding, AST ast, ImportRewriteContext context) {
		return addImport(binding, ast, context, TypeLocation.UNKNOWN);
	}

	/**
	 * Adds a new import to the rewriter's record and returns a {@link Type} that can be used
	 * in the code. The type binding can be an array binding, type variable or wildcard.
	 * If the binding is a generic type, the type parameters are ignored. For parameterized types, also the type
	 * arguments are processed and imports added if necessary. Anonymous types inside type arguments are normalized to their base type, wildcard
	 * of wildcards are ignored. If type annotations or type arguments are present at any point, the import is added up to that point and
	 * the type is retained from that point with type annotations and type arguments
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding the signature of the type to be added.
	 * @param ast the AST to create the returned type for.
	 * @param context an optional context that knows about types visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @param location where the type will be used. This information is needed to filter unwanted (nullness) type annotations.
	 * @return a type node for the given type binding. Type names are simple names if an import could be used,
	 * or else qualified names if an import conflict prevented an import.
	 * @since 3.13
	 */
	public Type addImport(ITypeBinding binding, AST ast, ImportRewriteContext context, TypeLocation location) {
		ITypeBinding bindingPoint = checkAnnotationAndGenerics(binding);
		Type type = internalAddImport(bindingPoint == null ? binding : bindingPoint, ast, context, null, /* getBase */ true, bindingPoint != null && !bindingPoint.equals(binding) ? TypeLocation.OTHER : location);
		if (bindingPoint != null && !bindingPoint.equals(binding)) {
			type = buildType(binding, bindingPoint, ast, context, type, location);
		}
		return type;
	}

	/**
	 * Adds a new import to the rewriter's record and returns a type reference that can be used
	 * in the code. The type binding can only be an array or non-generic type.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param qualifiedTypeName the qualified type name of the type to be added
	 * @param context an optional context that knows about types visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @return a type reference for the given qualified type name. The type name is a simple name if an import could be used,
	 * or else a qualified name if an import conflict prevented an import.
	 */
	public String addImport(String qualifiedTypeName, ImportRewriteContext context) {
		int angleBracketOffset= qualifiedTypeName.indexOf('<');
		if (angleBracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, angleBracketOffset), context, false) + qualifiedTypeName.substring(angleBracketOffset);
		}
		int bracketOffset= qualifiedTypeName.indexOf('[');
		if (bracketOffset != -1) {
			return internalAddImport(qualifiedTypeName.substring(0, bracketOffset), context, false) + qualifiedTypeName.substring(bracketOffset);
		}
		return internalAddImport(qualifiedTypeName, context, false);
	}

	/**
	 * Adds a new import to the rewriter's record and returns a type reference that can be used
	 * in the code. The type binding can only be an array or non-generic type.
	 * 	<p>
 	 * No imports are added for types that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param qualifiedTypeName the qualified type name of the type to be added
	 * @return a type reference for the given qualified type name. The type name is a simple name if an import could be used,
	 * or else a qualified name if an import conflict prevented an import.
	 */
	public String addImport(String qualifiedTypeName) {
		return addImport(qualifiedTypeName, this.defaultContext);
	}

	/**
	 * Adds a new static import to the rewriter's record and returns a name - single member name if
	 * import is successful, else qualified name.
	 * 	<p>
 	 * No imports are added for members that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding The binding of the static field or method to be added.
	 * @return either the simple member name if the import was successful or else the qualified name if
	 * an import conflict prevented the import
	 * @throws IllegalArgumentException an {@link IllegalArgumentException} is thrown if the binding is not a static field
	 * or method.
	 */
	public String addStaticImport(IBinding binding) {
		return addStaticImport(binding, this.defaultContext);
	}

	/**
	 * Adds a new static import to the rewriter's record and returns a name - single member name if
	 * import is successful, else qualified name.
	 * 	<p>
 	 * No imports are added for members that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param binding The binding of the static field or method to be added.
	 * @param context an optional context that knows about members visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @return either the simple member name if the import was successful or else the qualified name if
	 * an import conflict prevented the import
	 * @throws IllegalArgumentException an {@link IllegalArgumentException} is thrown if the binding is not a static field
	 * or method.
	 */
	public String addStaticImport(IBinding binding, ImportRewriteContext context) {
		if (Modifier.isStatic(binding.getModifiers())) {
			if (binding instanceof IVariableBinding) {
				IVariableBinding variableBinding= (IVariableBinding) binding;
				if (variableBinding.isField()) {
					ITypeBinding declaringType= variableBinding.getDeclaringClass();
					return addStaticImport(getRawQualifiedName(declaringType), binding.getName(), true, context);
				}
			} else if (binding instanceof IMethodBinding) {
				ITypeBinding declaringType= ((IMethodBinding) binding).getDeclaringClass();
				return addStaticImport(getRawQualifiedName(declaringType), binding.getName(), false, context);
			}
		}
		throw new IllegalArgumentException("Binding must be a static field or method."); //$NON-NLS-1$
	}

	/**
	 * Adds a new static import to the rewriter's record and returns a name - single member name if
	 * import is successful, else qualified name.
	 * 	<p>
 	 * No imports are added for members that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param declaringTypeName The qualified name of the static's member declaring type
	 * @param simpleName the simple name of the member; either a field or a method name.
	 * @param isField <code>true</code> specifies that the member is a field, <code>false</code> if it is a
	 * method.
	 * @return either the simple member name if the import was successful or else the qualified name if
	 * an import conflict prevented the import
	 */
	public String addStaticImport(String declaringTypeName, String simpleName, boolean isField) {
		return addStaticImport(declaringTypeName, simpleName, isField, this.defaultContext);
	}

	/**
	 * Adds a new static import to the rewriter's record and returns a name - single member name if
	 * import is successful, else qualified name.
	 * 	<p>
 	 * No imports are added for members that are already known. If a import for a type is recorded to be removed, this record is discarded instead.
	 * </p>
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been added.
	 * </p>
	 * @param declaringTypeName The qualified name of the static's member declaring type
	 * @param simpleName the simple name of the member; either a field or a method name.
	 * @param isField <code>true</code> specifies that the member is a field, <code>false</code> if it is a
	 * method.
	 * @param context an optional context that knows about members visible in the current scope or <code>null</code>
	 * to use the default context only using the available imports.
	 * @return either the simple member name if the import was successful or else the qualified name if
	 * an import conflict prevented the import
	 */
	public String addStaticImport(String declaringTypeName, String simpleName, boolean isField, ImportRewriteContext context) {
		String key = declaringTypeName + '.' + simpleName;
		if (declaringTypeName.indexOf('.') == -1) {
			return key;
		}
		if (context == null) {
			context= this.defaultContext;
		}
		int kind= isField ? ImportRewriteContext.KIND_STATIC_FIELD : ImportRewriteContext.KIND_STATIC_METHOD;
		this.importsKindMap.put(key, Integer.valueOf(kind));
		int res= context.findInContext(declaringTypeName, simpleName, kind);
		if (res == ImportRewriteContext.RES_NAME_CONFLICT) {
			return key;
		}
		if (res == ImportRewriteContext.RES_NAME_UNKNOWN) {
			addEntry(STATIC_PREFIX + key);
		}
		if (res == ImportRewriteContext.RES_NAME_UNKNOWN_NEEDS_EXPLICIT_IMPORT) {
			addEntry(STATIC_PREFIX + key);
			this.staticExplicitSimpleNames.add(simpleName);
		}
		return simpleName;
	}

	private String internalAddImport(String fullTypeName, ImportRewriteContext context, boolean isTypeInUnnamedPackage) {
		int idx= fullTypeName.lastIndexOf('.');
		String typeContainerName, typeName;
		if (idx != -1) {
			typeContainerName= fullTypeName.substring(0, idx);
			typeName= fullTypeName.substring(idx + 1);
		} else {
			typeContainerName= ""; //$NON-NLS-1$
			typeName= fullTypeName;
		}

		if (typeContainerName.length() == 0 && PrimitiveType.toCode(typeName) != null) {
			return fullTypeName;
		}

		if (context == null)
			context= this.defaultContext;

		int res= context.findInContext(typeContainerName, typeName, ImportRewriteContext.KIND_TYPE);
		if (res == ImportRewriteContext.RES_NAME_CONFLICT
				|| isTypeInUnnamedPackage) {
			return fullTypeName;
		}
		if (res == ImportRewriteContext.RES_NAME_UNKNOWN) {
			addEntry(NORMAL_PREFIX + fullTypeName);
		}
		if (res == ImportRewriteContext.RES_NAME_UNKNOWN_NEEDS_EXPLICIT_IMPORT) {
			addEntry(NORMAL_PREFIX + fullTypeName);
			this.typeExplicitSimpleNames.add(typeName);
		}
		return typeName;
	}

	private void addEntry(String entry) {
		this.existingImports.add(entry);

		if (this.removedImports.remove(entry)) {
			return;
		}

		this.addedImports.add(entry);
	}

	private boolean removeEntry(String entry) {
		if (this.existingImports.remove(entry)) {
			if (this.addedImports.remove(entry)) {
				return true;
			}

			this.removedImports.add(entry);

			return true;
		}
		return false;
	}

	/**
	 * Records to remove a import. No remove is recorded if no such import exists or if such an import is recorded
	 * to be added. In that case the record of the addition is discarded.
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that an import has been removed.
	 * </p>
	 * @param qualifiedName The import name to remove.
	 * @return <code>true</code> is returned of an import of the given name could be found.
	 */
	public boolean removeImport(String qualifiedName) {
		return removeEntry(NORMAL_PREFIX + qualifiedName);
	}

	/**
	 * Records to remove a static import. No remove is recorded if no such import exists or if such an import is recorded
	 * to be added. In that case the record of the addition is discarded.
	 * <p>
	 * The content of the compilation unit itself is actually not modified
	 * in any way by this method; rather, the rewriter just records that a new import has been removed.
	 * </p>
	 * @param qualifiedName The import name to remove.
	 * @return <code>true</code> is returned of an import of the given name could be found.
	 */
	public boolean removeStaticImport(String qualifiedName) {
		return removeEntry(STATIC_PREFIX + qualifiedName);
	}

	private static String getRawName(ITypeBinding normalizedBinding) {
		return normalizedBinding.getTypeDeclaration().getName();
	}

	private static String getRawQualifiedName(ITypeBinding normalizedBinding) {
		return normalizedBinding.getTypeDeclaration().getQualifiedName();
	}


	/**
	 * Converts all modifications recorded by this rewriter into an object representing the corresponding text
	 * edits to the source code of the rewrite's compilation unit. The compilation unit itself is not modified.
	 * <p>
	 * Calling this methods does not discard the modifications on record. Subsequence modifications are added
	 * to the ones already on record. If this method is called again later, the resulting text edit object will accurately
	 * reflect the net cumulative effect of all those changes.
	 * </p>
	 * @param monitor the progress monitor or <code>null</code>
	 * @return text edit object describing the changes to the document corresponding to the changes
	 * recorded by this rewriter
	 * @throws CoreException the exception is thrown if the rewrite fails.
	 */
	public final TextEdit rewriteImports(IProgressMonitor monitor) throws CoreException {

		SubMonitor subMonitor = SubMonitor.convert(monitor,
				Messages.bind(Messages.importRewrite_processDescription), 2);
		if (!hasRecordedChanges()) {
			this.createdImports= CharOperation.NO_STRINGS;
			this.createdStaticImports= CharOperation.NO_STRINGS;
			return new MultiTextEdit();
		}

		CompilationUnit usedAstRoot= this.astRoot;
		if (usedAstRoot == null) {
			ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
			parser.setSource(this.compilationUnit);
			parser.setFocalPosition(0); // reduced AST
			parser.setResolveBindings(false);
			usedAstRoot= (CompilationUnit) parser.createAST(subMonitor.split(1));
		}

		ImportRewriteConfiguration config= buildImportRewriteConfiguration();

		ImportRewriteAnalyzer computer=
			new ImportRewriteAnalyzer(this.compilationUnit, usedAstRoot, config);

		for (String addedImport : this.addedImports) {
			boolean isStatic = STATIC_PREFIX == addedImport.charAt(0);
			String qualifiedName = addedImport.substring(1);
			computer.addImport(isStatic, qualifiedName);
		}

		for (String removedImport : this.removedImports) {
			boolean isStatic = STATIC_PREFIX == removedImport.charAt(0);
			String qualifiedName = removedImport.substring(1);
			computer.removeImport(isStatic, qualifiedName);
		}

		for (String typeExplicitSimpleName : this.typeExplicitSimpleNames) {
			computer.requireExplicitImport(false, typeExplicitSimpleName);
		}

		for (String staticExplicitSimpleName : this.staticExplicitSimpleNames) {
			computer.requireExplicitImport(true, staticExplicitSimpleName);
		}

		ImportRewriteAnalyzer.RewriteResult result= computer.analyzeRewrite(subMonitor.split(1));

		this.createdImports= result.getCreatedImports();
		this.createdStaticImports= result.getCreatedStaticImports();

		return result.getTextEdit();
	}

	private ImportRewriteConfiguration buildImportRewriteConfiguration() {
		ImportRewriteConfiguration.Builder configBuilder;

		if (this.restoreExistingImports) {
			configBuilder= ImportRewriteConfiguration.Builder.preservingOriginalImports();
		} else {
			configBuilder= ImportRewriteConfiguration.Builder.discardingOriginalImports();
		}

		configBuilder.setImportOrder(Arrays.asList(this.importOrder));
		configBuilder.setTypeOnDemandThreshold(this.importOnDemandThreshold);
		configBuilder.setStaticOnDemandThreshold(this.staticImportOnDemandThreshold);

		configBuilder.setTypeContainerSorting(this.useContextToFilterImplicitImports ?
				ImportContainerSorting.BY_PACKAGE : ImportContainerSorting.BY_PACKAGE_AND_CONTAINING_TYPE);

		configBuilder.setStaticContainerSorting(ImportContainerSorting.BY_PACKAGE_AND_CONTAINING_TYPE);

		configBuilder.setImplicitImportIdentification(this.filterImplicitImports ?
				ImplicitImportIdentification.JAVA_LANG_AND_CU_PACKAGE : ImplicitImportIdentification.NONE);

		return configBuilder.build();
	}

	/**
	 * Returns all new non-static imports created by the last invocation of {@link #rewriteImports(IProgressMonitor)}
	 * or <code>null</code> if these methods have not been called yet.
	 * <p>
	 * 	Note that this list doesn't need to be the same as the added imports (see {@link #getAddedImports()}) as
	 * implicit imports are not created and some imports are represented by on-demand imports instead.
	 * </p>
	 * @return the created imports
	 */
	public String[] getCreatedImports() {
		return this.createdImports;
	}

	/**
	 * Returns all new static imports created by the last invocation of {@link #rewriteImports(IProgressMonitor)}
	 * or <code>null</code> if these methods have not been called yet.
	 * <p>
	 * Note that this list doesn't need to be the same as the added static imports ({@link #getAddedStaticImports()}) as
	 * implicit imports are not created and some imports are represented by on-demand imports instead.
	 * </p
	 * @return the created imports
	 */
	public String[] getCreatedStaticImports() {
		return this.createdStaticImports;
	}

	/**
	 * Returns all non-static imports that are recorded to be added.
	 *
	 * @return the imports recorded to be added.
	 */
	public String[] getAddedImports() {
		return filterFromList(this.addedImports, NORMAL_PREFIX);
	}

	/**
	 * Returns all static imports that are recorded to be added.
	 *
	 * @return the static imports recorded to be added.
	 */
	public String[] getAddedStaticImports() {
		return filterFromList(this.addedImports, STATIC_PREFIX);
	}

	/**
	 * Returns all non-static imports that are recorded to be removed.
	 *
	 * @return the imports recorded to be removed.
	 */
	public String[] getRemovedImports() {
		return filterFromList(this.removedImports, NORMAL_PREFIX);
	}

	/**
	 * Returns all static imports that are recorded to be removed.
	 *
	 * @return the static imports recorded to be removed.
	 */
	public String[] getRemovedStaticImports() {
		return filterFromList(this.removedImports, STATIC_PREFIX);
	}

	/**
	 * Returns <code>true</code> if imports have been recorded to be added or removed.
	 * @return boolean returns if any changes to imports have been recorded.
	 */
	public boolean hasRecordedChanges() {
		return !this.restoreExistingImports
				|| !this.addedImports.isEmpty()
				|| !this.removedImports.isEmpty();
	}


	private static String[] filterFromList(List<String> imports, char prefix) {
		if (imports == null) {
			return CharOperation.NO_STRINGS;
		}
		List<String> res= new ArrayList<>();
		for (String curr : imports) {
			if (prefix == curr.charAt(0)) {
				res.add(curr.substring(1));
			}
		}
		return res.toArray(new String[res.size()]);
	}

	private void annotateList(List annotations, IAnnotationBinding [] annotationBindings, AST ast,
						ImportRewriteContext context, TypeLocation location, ITypeBinding type) {
		if (context == null)
			context= this.defaultContext;
		annotationBindings = context.removeRedundantTypeAnnotations(annotationBindings, location, type);
		for (int i = 0; i< annotationBindings.length; i++) {
			Annotation annotation = addAnnotation(annotationBindings[i], ast, context);
			if (annotation != null) annotations.add(annotation);
		}
	}

	private Type annotateType(ITypeBinding binding, AST ast, ImportRewriteContext context, Type type, TypeLocation location) {
		IAnnotationBinding [] annotationBindings = binding.getTypeAnnotations();
		if (annotationBindings != null && annotationBindings.length > 0 && type instanceof AnnotatableType) {
			annotateList(((AnnotatableType) type).annotations(), annotationBindings, ast, context, location, binding);
		}
		return type;
	}

	private Type buildType(ITypeBinding binding, ITypeBinding bindingPoint, AST ast, ImportRewriteContext context, Type qualifier, TypeLocation location) {
		if (binding.equals(bindingPoint)) {
			return qualifier;
		}
		// build the type recursively from left to right
		Type type = binding.isMember() ? buildType(binding.getDeclaringClass(), bindingPoint, ast, context, qualifier, TypeLocation.OTHER) : null;
		type = internalAddImport(binding, ast, context, type, false, location);
		return type;
	}

	private ITypeBinding checkAnnotationAndGenerics(ITypeBinding binding) {
		ITypeBinding bindingPoint = null;
		while (binding != null) {
			IAnnotationBinding annotationBinding [] = binding.getTypeAnnotations();
			ITypeBinding []  typeArguments = binding.getTypeArguments();
			if ((annotationBinding != null && annotationBinding.length > 0) ||
					(typeArguments != null && typeArguments.length > 0)) {
				bindingPoint = binding;
			}
			if (binding.isMember()) {
				binding = binding.getDeclaringClass();
			} else {
				break;
			}
		}
		return bindingPoint;
	}

	private Type createBaseType(AST ast, ImportRewriteContext context, ITypeBinding normalizedBinding, TypeLocation location) {
		Type type;
		IAnnotationBinding annotationBinding [] = normalizedBinding.getTypeAnnotations();
		boolean annotsPresent = annotationBinding != null && annotationBinding.length > 0;

		String qualifiedName= getRawQualifiedName(normalizedBinding);
		String res = qualifiedName.length() > 0 ? internalAddImport(qualifiedName, context, isTypeInUnnamedPackage(normalizedBinding)) : getRawName(normalizedBinding);

		if (annotsPresent) {
			int dotIndex = res != null ? res.lastIndexOf('.') : -1;
			if (dotIndex > 0) {
				Name nameQualifier = ast.newName(res.substring(0, dotIndex));
				SimpleName simpleName = ast.newSimpleName(res.substring(dotIndex + 1));
				type = ast.newNameQualifiedType(nameQualifier, simpleName);
			} else {
				type = ast.newSimpleType(ast.newName(res));
			}
			annotateList(((AnnotatableType) type).annotations(), annotationBinding, ast, context, location, normalizedBinding);
		} else {
			type = ast.newSimpleType(ast.newName(res));
		}
		return type;
	}

	private Type getArrayType(Type elementType, AST ast, ImportRewriteContext context, ITypeBinding normalizedBinding, TypeLocation location) {
		int noDimensions = normalizedBinding.getDimensions();
		ArrayType arrayType = ast.newArrayType(elementType, noDimensions);
		if (ast.apiLevel() >= JLS8_INTERNAL) {
			for (int i = 0; i < noDimensions; i++) {
				IAnnotationBinding[] typeAnnotations = normalizedBinding.getTypeAnnotations();
				if (typeAnnotations.length > 0) {
					Dimension dimension = (Dimension) arrayType.dimensions().get(i);
					annotateList(dimension.annotations(), typeAnnotations, ast, context,
							i == 0 ? location : TypeLocation.ARRAY_CONTENTS, normalizedBinding);
				}
				normalizedBinding = normalizedBinding.getComponentType();
			}
		}
		return arrayType;
	}

	private Type internalAddImport(ITypeBinding binding, AST ast, ImportRewriteContext context, Type currentType, boolean getBase, TypeLocation location) {
		Type type = null;
		ITypeBinding normalizedBinding = null;

		if (binding.isPrimitive()) {
			type = ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
			normalizedBinding= binding;
		} else {
			normalizedBinding= normalizeTypeBinding(binding);
			if (normalizedBinding == null) {
				 return ast.newSimpleType(ast.newSimpleName("invalid")); //$NON-NLS-1$
			} else if (normalizedBinding.isTypeVariable()) {
					// no import
				type = ast.newSimpleType(ast.newSimpleName(binding.getName()));
			} else if (normalizedBinding.isWildcardType()) {
				WildcardType wcType= ast.newWildcardType();
				ITypeBinding bound= normalizedBinding.getBound();
				if (bound != null && !bound.isWildcardType() && !bound.isCapture()) { // bug 96942
					Type boundType= addImport(bound, ast, context, TypeLocation.TYPE_BOUND);
					wcType.setBound(boundType, normalizedBinding.isUpperbound());
				}
				type = wcType;
			} else if (normalizedBinding.isArray()) {
				Type elementType= addImport(normalizedBinding.getElementType(), ast, context, TypeLocation.ARRAY_CONTENTS);
				type = getArrayType(elementType, ast, context, normalizedBinding, location);
			}
		}

		if (type != null) {
			return annotateType(normalizedBinding, ast, context, type, location);
		}

		if (getBase) {
			type = createBaseType(ast, context, normalizedBinding, location);
		} else  {
			type = currentType != null ? (Type) ast.newQualifiedType(currentType, ast.newSimpleName(getRawName(normalizedBinding))) :
				ast.newSimpleType(ast.newName(getRawName(normalizedBinding)));
			type = annotateType(normalizedBinding, ast, context, type, location);
		}

		ITypeBinding[] typeArguments = normalizedBinding.getTypeArguments();
		if (typeArguments.length > 0) {
			ParameterizedType paramType = ast.newParameterizedType(type);
			List arguments = paramType.typeArguments();
			for (int i = 0; i < typeArguments.length; i++) {
				ITypeBinding curr = typeArguments[i];
				if (containsNestedCapture(curr, false)) { // see bug 103044
					arguments.add(ast.newWildcardType());
				} else {
					arguments.add(addImport(curr, ast, context, TypeLocation.TYPE_ARGUMENT));
				}
			}
			type = paramType;
		}
		return type;
	}

	private Expression addAnnotation(AST ast, Object value, ImportRewriteContext context) {
		if (value instanceof Boolean) {
			return ast.newBooleanLiteral(((Boolean) value).booleanValue());
		} else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
				|| value instanceof Float || value instanceof Double) {
			return ast.newNumberLiteral(value.toString());
		} else if (value instanceof Character) {
			CharacterLiteral result = ast.newCharacterLiteral();
			result.setCharValue(((Character) value).charValue());
			return result;
		} else if (value instanceof ITypeBinding) {
			TypeLiteral result = ast.newTypeLiteral();
			result.setType(addImport((ITypeBinding) value, ast, context, TypeLocation.OTHER));
			return result;
		} else if (value instanceof String) {
			StringLiteral result = ast.newStringLiteral();
			result.setLiteralValue((String) value);
			return result;
		} else if (value instanceof IVariableBinding) {
			IVariableBinding variable = (IVariableBinding) value;

			FieldAccess result = ast.newFieldAccess();
			result.setName(ast.newSimpleName(variable.getName()));
			Type type = addImport(variable.getType(), ast, context, TypeLocation.OTHER); // ignore annotations, type will discarded
			Name name;
			if (type instanceof SimpleType) {
				SimpleType simpleType = (SimpleType) type;
				name = simpleType.getName();
				// cut 'name' loose from its parent, so that it can be reused
				simpleType.setName(ast.newSimpleName("a")); //$NON-NLS-1$
			} else {
				name = ast.newName("invalid"); //$NON-NLS-1$
			}
			result.setExpression(name);
			return result;
		} else if (value instanceof IAnnotationBinding) {
			return addAnnotation((IAnnotationBinding) value, ast, context);
		} else if (value instanceof Object[]) {
			Object[] values = (Object[]) value;
			if (values.length == 1)
				return addAnnotation(ast, values[0], context);

			ArrayInitializer initializer = ast.newArrayInitializer();
			List expressions = initializer.expressions();
			int size = values.length;
			for (int i = 0; i < size; i++)
				expressions.add(addAnnotation(ast, values[i], context));
			return initializer;
		} else {
			return null;
		}
	}

	private static boolean isTypeInUnnamedPackage(ITypeBinding binding) {
		boolean isInUnnamedPackage= false;
		if (binding != null) {
			IPackageBinding pBinding= binding.getPackage();
			if (pBinding != null) {
				isInUnnamedPackage= pBinding.isUnnamed();
			}
		}
		return isInUnnamedPackage;
	}
}
