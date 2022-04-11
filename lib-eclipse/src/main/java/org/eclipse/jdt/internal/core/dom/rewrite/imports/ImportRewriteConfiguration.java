/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;

/**
 * Used as a constructor parameter to ImportRewriteAnalyzer to configure its behavior.
 * <p>
 * The starting points are the two static factory methods of {@link Builder}.
 */
public final class ImportRewriteConfiguration {
	public enum OriginalImportHandling {
		/**
		 * Specifies to discard original imports and totally sort all new imports, as in the case of
		 * the "Organize Imports" operation.
		 */
		DISCARD {
			@Override
			boolean shouldRemoveOriginalImports() {
				return true;
			}

			@Override
			boolean shouldFixAllLineDelimiters() {
				return true;
			}

			@Override
			ImportAdder createImportAdder(Comparator<ImportName> importComparator) {
				return new ReorderingImportAdder(importComparator);
			}
		},
		/**
		 * Specifies to keep original imports in their original order, placing each newly added
		 * import adjacent to the original import that it most closely matches.
		 */
		PRESERVE_IN_ORDER {
			@Override
			boolean shouldRemoveOriginalImports() {
				return false;
			}

			@Override
			boolean shouldFixAllLineDelimiters() {
				return false;
			}

			@Override
			ImportAdder createImportAdder(Comparator<ImportName> importComparator) {
				return new OrderPreservingImportAdder(importComparator);
			}
		},
		;

		/**
		 * If true, ImportRewriteAnalyzer will, during its initialization, mark all original imports
		 * for removal.
		 */
		abstract boolean shouldRemoveOriginalImports();

		/**
		 * If true, line delimiters will be standardized between every pair of adjacent imports.
		 * Otherwise, line delimiters will be corrected only between pairs of adjacent imports that
		 * were not adjacent originally.
		 */
		abstract boolean shouldFixAllLineDelimiters();

		/**
		 * Creates the {@link ImportAdder} which will combine and order new and existing imports
		 * together.
		 */
		abstract ImportAdder createImportAdder(Comparator<ImportName> importComparator);
	}

	/**
	 * Specifies how to sort import declarations by their packages and/or containing types.
	 */
	public enum ImportContainerSorting {
		/**
		 * Sorts imports by each import's package and any containing types, in lexicographic order.
		 * For example (assuming that all of the imports belong to the same import group):
		 * <pre>
		 * import java.net.Socket;
		 * import java.util.Map;
		 * import java.util.Set;
		 * import java.util.Map.Entry;
		 * </pre>
		 */
		BY_PACKAGE_AND_CONTAINING_TYPE {
			@Override
			Comparator<ImportName> createContainerComparator(JavaProject javaProject) {
				return new PackageAndContainingTypeImportComparator();
			}
		},

		/**
		 * Sorts imports by each import's package, in lexicographic order. For example (assuming all
		 * of the imports belong to the same import group):
		 * <pre>
		 * import java.net.Socket;
		 * import java.util.Map;
		 * import java.util.Map.Entry;
		 * import java.util.Set;
		 * </pre>
		 */
		BY_PACKAGE {
			@Override
			Comparator<ImportName> createContainerComparator(JavaProject javaProject) {
				return new PackageImportComparator(javaProject);
			}
		},
		;

		abstract Comparator<ImportName> createContainerComparator(JavaProject javaProject);
	}

	/**
	 * Specifies which types are considered to be implicitly imported.
	 * <p>
	 * An import declaration of such a type will not be added to the compilation unit unless it is
	 * needed to resolve a conflict with an on-demand imports, or the type's simple name has been
	 * specified with {@link ImportRewriteAnalyzer#requireExplicitImport}.
	 * <p>
	 * Also, implicitly imported types will be considered for conflicts when deciding which types
	 * from other packages can be reduced into on-demand imports. E.g. if java.lang.Integer were
	 * considered to be implicitly imported, that would prevent an import of com.example.Integer
	 * from being reduced into an on-demand import of com.example.*.
	 */
	public enum ImplicitImportIdentification {
		/**
		 * Specifies that types from the following packages are considered to be implicitly
		 * imported:
		 * <ul>
		 * <li>java.lang</li>
		 * <li>the package of the compilation unit being rewritten</li>
		 * </ul>
		 */
		JAVA_LANG_AND_CU_PACKAGE {
			@Override
			Set<String> determineImplicitImportContainers(ICompilationUnit compilationUnit) {
				Set<String> implicitImportContainerNames = new HashSet<String>();

				implicitImportContainerNames.add("java.lang"); //$NON-NLS-1$

				IJavaElement packageFragment = compilationUnit.getParent();
				String compilationUnitPackageName = packageFragment.getElementName();
				if (compilationUnitPackageName.isEmpty() && !packageFragment.exists() && compilationUnit.exists()) {
					/*
					 * For a file outside of the build path, JavaCore#create(IFile) creates an
					 * ICompilationUnit with the file's parent folder as package fragment root, and a default package.
					 * That "wrong" package is problematic for the ImportRewrite, since it doesn't get filtered
					 * and eventually leads to unused import statements.
					 */
					try {
						IPackageDeclaration[] packageDeclarations = compilationUnit.getPackageDeclarations();
						if (packageDeclarations.length > 0) {
							implicitImportContainerNames.add(packageDeclarations[0].getElementName());
							return implicitImportContainerNames;
						}
					} catch (JavaModelException e) {
						// continue
					}
				}
				implicitImportContainerNames.add(compilationUnitPackageName);

				return implicitImportContainerNames;
			}
		},
		/**
		 * Specifies that no types are considered to be implicitly imported.
		 */
		NONE {
			@Override
			Set<String> determineImplicitImportContainers(ICompilationUnit compilationUnit) {
				return Collections.emptySet();
			}
		},
		;

		abstract Set<String> determineImplicitImportContainers(ICompilationUnit compilationUnit);
	}

	public static class Builder {
		public static Builder discardingOriginalImports() {
			return new Builder(OriginalImportHandling.DISCARD);
		}

		public static Builder preservingOriginalImports() {
			return new Builder(OriginalImportHandling.PRESERVE_IN_ORDER);
		}

		final OriginalImportHandling originalImportHandling;
		ImportContainerSorting typeContainerSorting;
		ImportContainerSorting staticContainerSorting;
		ImplicitImportIdentification implicitImportIdentification;
		List<String> importOrder;
		Integer typeOnDemandThreshold;
		Integer staticOnDemandThreshold;

		private Builder(OriginalImportHandling originalImportHandling) {
			this.originalImportHandling = originalImportHandling;
			this.typeContainerSorting = ImportContainerSorting.BY_PACKAGE;
			this.staticContainerSorting = ImportContainerSorting.BY_PACKAGE_AND_CONTAINING_TYPE;
			this.implicitImportIdentification = ImplicitImportIdentification.JAVA_LANG_AND_CU_PACKAGE;
			this.importOrder = Collections.emptyList();
			this.typeOnDemandThreshold = null;
			this.staticOnDemandThreshold = null;
		}

		public Builder setTypeContainerSorting(ImportContainerSorting typeContainerSorting) {
			this.typeContainerSorting = typeContainerSorting;
			return this;
		}

		public Builder setStaticContainerSorting(ImportContainerSorting staticContainerSorting) {
			this.staticContainerSorting = staticContainerSorting;
			return this;
		}

		public Builder setImplicitImportIdentification(ImplicitImportIdentification implicitImportIdentification) {
			this.implicitImportIdentification = implicitImportIdentification;
			return this;
		}

		public Builder setImportOrder(List<String> importOrder) {
			this.importOrder = Collections.unmodifiableList(new ArrayList<String>(importOrder));
			return this;
		}

		public Builder setTypeOnDemandThreshold(int typeOnDemandThreshold) {
			this.typeOnDemandThreshold = typeOnDemandThreshold;
			return this;
		}

		public Builder setStaticOnDemandThreshold(int staticOnDemandThreshold) {
			this.staticOnDemandThreshold = staticOnDemandThreshold;
			return this;
		}

		public ImportRewriteConfiguration build() {
			return new ImportRewriteConfiguration(this);
		}
	}

	final OriginalImportHandling originalImportHandling;
	final ImportContainerSorting typeContainerSorting;
	final ImportContainerSorting staticContainerSorting;
	final ImplicitImportIdentification implicitImportIdentification;
	final List<String> importOrder;
	final int typeOnDemandThreshold;
	final int staticOnDemandThreshold;

	ImportRewriteConfiguration(Builder builder) {
		this.originalImportHandling = builder.originalImportHandling;
		this.typeContainerSorting = builder.typeContainerSorting;
		this.staticContainerSorting = builder.staticContainerSorting;
		this.implicitImportIdentification = builder.implicitImportIdentification;
		this.importOrder = builder.importOrder;
		this.typeOnDemandThreshold = builder.typeOnDemandThreshold;
		this.staticOnDemandThreshold = builder.staticOnDemandThreshold;
	}
}
