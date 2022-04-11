/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.problem.ProblemSeverities.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.ModuleScope;
import org.eclipse.jdt.internal.compiler.lookup.PlainPackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.AbortMethod;
import org.eclipse.jdt.internal.compiler.problem.AbortType;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;

public class ModuleDeclaration extends ASTNode implements ReferenceContext {

	public ExportsStatement[] exports;
	public RequiresStatement[] requires;
	public UsesStatement[] uses;
	public ProvidesStatement[] services;
	public OpensStatement[] opens;
	public Annotation[] annotations;
	public int exportsCount;
	public int requiresCount;
	public int usesCount;
	public int servicesCount;
	public int opensCount;
	public SourceModuleBinding binding;
	public int declarationSourceStart;
	public int declarationSourceEnd;
	public int bodyStart;
	public int bodyEnd; // doesn't include the trailing comment if any.
	public int modifiersSourceStart;
	public ModuleScope scope;
	public char[][] tokens;
	public char[] moduleName;
	public long[] sourcePositions;
	public int modifiers = ClassFileConstants.AccDefault;
	boolean ignoreFurtherInvestigation;
	boolean hasResolvedModuleDirectives;
	boolean hasResolvedPackageDirectives;
	boolean hasResolvedTypeDirectives;
	CompilationResult compilationResult;

	public ModuleDeclaration(CompilationResult compilationResult, char[][] tokens, long[] positions) {
		this.compilationResult = compilationResult;
		this.exportsCount = 0;
		this.requiresCount = 0;
		this.tokens = tokens;
		this.moduleName = CharOperation.concatWith(tokens, '.');
		this.sourcePositions = positions;
		this.sourceEnd = (int) (positions[positions.length-1] & 0x00000000FFFFFFFF);
		this.sourceStart = (int) (positions[0] >>> 32);
	}

	public ModuleBinding setBinding(SourceModuleBinding sourceModuleBinding) {
		this.binding = sourceModuleBinding;
		return sourceModuleBinding;
	}

	public void checkAndSetModifiers() {
		int realModifiers = this.modifiers & ExtraCompilerModifiers.AccJustFlag;
		int expectedModifiers = ClassFileConstants.ACC_OPEN | ClassFileConstants.ACC_SYNTHETIC;
		if ((realModifiers & ~(expectedModifiers)) != 0) {
			this.scope.problemReporter().illegalModifierForModule(this);
			realModifiers &= expectedModifiers;
		}
		int effectiveModifiers = ClassFileConstants.AccModule | realModifiers;
		this.modifiers = this.binding.modifiers = effectiveModifiers;
	}

	public boolean isOpen() {
		return (this.modifiers & ClassFileConstants.ACC_OPEN) != 0;
	}

	public void createScope(final Scope parentScope) {
		this.scope = new ModuleScope(parentScope, this);
	}

	public void generateCode() {
		if ((this.bits & ASTNode.HasBeenGenerated) != 0)
			return;
		this.bits |= ASTNode.HasBeenGenerated;
		if (this.ignoreFurtherInvestigation) {
			return;
		}
		try {
			// create the result for a compiled type
			LookupEnvironment env = this.scope.environment();
			ClassFile classFile = env.classFilePool.acquireForModule(this.binding, env.globalOptions);
			classFile.initializeForModule(this.binding);

			// finalize the compiled type result
			classFile.addModuleAttributes(this.binding, this.annotations, this.scope.referenceCompilationUnit());
			this.scope.referenceCompilationUnit().compilationResult.record(
				this.binding.moduleName,
				classFile);
		} catch (AbortType e) {
			if (this.binding == null)
				return;
		}
	}

	/** Resolve those module directives that relate to modules (requires). */
	public void resolveModuleDirectives(CompilationUnitScope cuScope) {
		if (this.binding == null) {
			this.ignoreFurtherInvestigation = true;
			return;
		}
		if (this.hasResolvedModuleDirectives)
			return;

		this.hasResolvedModuleDirectives = true;

		Set<ModuleBinding> requiredModules = new HashSet<ModuleBinding>();
		Set<ModuleBinding> requiredTransitiveModules = new HashSet<ModuleBinding>();
		for(int i = 0; i < this.requiresCount; i++) {
			RequiresStatement ref = this.requires[i];
			if (ref != null && ref.resolve(cuScope) != null) {
				if (!requiredModules.add(ref.resolvedBinding)) {
					cuScope.problemReporter().duplicateModuleReference(IProblem.DuplicateRequires, ref.module);
				}
				if (ref.isTransitive())
					requiredTransitiveModules.add(ref.resolvedBinding);
				Collection<ModuleBinding> deps = ref.resolvedBinding.dependencyGraphCollector().get();
				if (deps.contains(this.binding)) {
					cuScope.problemReporter().cyclicModuleDependency(this.binding, ref.module);
					requiredModules.remove(ref.module.binding);
				}
			}
		}
		this.binding.setRequires(requiredModules.toArray(new ModuleBinding[requiredModules.size()]),
								 requiredTransitiveModules.toArray(new ModuleBinding[requiredTransitiveModules.size()]));

		// also resolve module references inside package statements ("to"):
		if (this.exports != null) {
			for (ExportsStatement exportsStatement : this.exports) {
				if (exportsStatement.isQualified()) {
					for (ModuleReference moduleReference : exportsStatement.targets)
						moduleReference.resolve(cuScope);
				}
			}
		}
		if (this.opens != null) {
			for (OpensStatement opensStatement : this.opens) {
				if (opensStatement.isQualified()) {
					for (ModuleReference moduleReference : opensStatement.targets)
						moduleReference.resolve(cuScope);
				}
			}
		}
	}

	/** Resolve those module directives that relate to packages (exports, opens). */
	public void resolvePackageDirectives(CompilationUnitScope cuScope) {
		if (this.binding == null) {
			this.ignoreFurtherInvestigation = true;
			return;
		}
		if (this.hasResolvedPackageDirectives)
			return;

		this.hasResolvedPackageDirectives = true;

		Set<PlainPackageBinding> exportedPkgs = new HashSet<>();
		for (int i = 0; i < this.exportsCount; i++) {
			ExportsStatement ref = this.exports[i];
 			if (ref != null && ref.resolve(cuScope)) {
				if (!exportedPkgs.add(ref.resolvedPackage)) {
					cuScope.problemReporter().invalidPackageReference(IProblem.DuplicateExports, ref);
				}
				char[][] targets = null;
				if (ref.targets != null) {
					targets = new char[ref.targets.length][];
					for (int j = 0; j < targets.length; j++)
						targets[j] = ref.targets[j].moduleName;
				}
				this.binding.addResolvedExport(ref.resolvedPackage, targets);
			}
		}

		HashtableOfObject openedPkgs = new HashtableOfObject();
		for (int i = 0; i < this.opensCount; i++) {
			OpensStatement ref = this.opens[i];
			if (isOpen()) {
				cuScope.problemReporter().invalidOpensStatement(ref, this);
			} else {
				if (openedPkgs.containsKey(ref.pkgName)) {
					cuScope.problemReporter().invalidPackageReference(IProblem.DuplicateOpens, ref);
				} else {
					openedPkgs.put(ref.pkgName, ref);
					ref.resolve(cuScope);
				}
				char[][] targets = null;
				if (ref.targets != null) {
					targets = new char[ref.targets.length][];
					for (int j = 0; j < targets.length; j++)
						targets[j] = ref.targets[j].moduleName;
				}
				this.binding.addResolvedOpens(ref.resolvedPackage, targets);
			}
		}
	}

	/** Resolve those module directives that relate to types (provides / uses). */
	public void resolveTypeDirectives(CompilationUnitScope cuScope) {
		if (this.binding == null) {
			this.ignoreFurtherInvestigation = true;
			return;
		}
		if (this.hasResolvedTypeDirectives)
			return;

		this.hasResolvedTypeDirectives = true;
		ASTNode.resolveAnnotations(this.scope, this.annotations, this.binding);

		Set<TypeBinding> allTypes = new HashSet<TypeBinding>();
		for(int i = 0; i < this.usesCount; i++) {
			TypeBinding serviceBinding = this.uses[i].serviceInterface.resolveType(this.scope);
			if (serviceBinding != null && serviceBinding.isValidBinding()) {
				if (!(serviceBinding.isClass() || serviceBinding.isInterface() || serviceBinding.isAnnotationType())) {
					cuScope.problemReporter().invalidServiceRef(IProblem.InvalidServiceIntfType, this.uses[i].serviceInterface);
				}
				if (!allTypes.add(this.uses[i].serviceInterface.resolvedType)) {
					cuScope.problemReporter().duplicateTypeReference(IProblem.DuplicateUses, this.uses[i].serviceInterface);
				}
			}
		}
		this.binding.setUses(allTypes.toArray(new TypeBinding[allTypes.size()]));

		Set<TypeBinding> interfaces = new HashSet<>();
		for(int i = 0; i < this.servicesCount; i++) {
			this.services[i].resolve(this.scope);
			TypeBinding infBinding = this.services[i].serviceInterface.resolvedType;
			if (infBinding != null && infBinding.isValidBinding()) {
				if (!interfaces.add(this.services[i].serviceInterface.resolvedType)) {
					cuScope.problemReporter().duplicateTypeReference(IProblem.DuplicateServices,
							this.services[i].serviceInterface);
				}
				this.binding.setImplementations(infBinding, this.services[i].getResolvedImplementations());
			}
		}
		this.binding.setServices(interfaces.toArray(new TypeBinding[interfaces.size()]));
	}

	public void analyseCode(CompilationUnitScope skope) {
		analyseModuleGraph(skope);
		analyseReferencedPackages(skope);
	}

	private void analyseReferencedPackages(CompilationUnitScope skope) {
		if (this.exports != null) {
			analyseSomeReferencedPackages(this.exports, skope);
		}
		if (this.opens != null) {
			analyseSomeReferencedPackages(this.opens, skope);
		}
	}

	private void analyseSomeReferencedPackages(PackageVisibilityStatement[] stats, CompilationUnitScope skope) {
		for (PackageVisibilityStatement stat : stats) {
			PlainPackageBinding pb = stat.resolvedPackage;
			if (pb == null)
				continue;
			if (pb.hasCompilationUnit(true))
				continue;
			for (ModuleBinding req : this.binding.getAllRequiredModules()) {
				for (PlainPackageBinding exported : req.getExports()) {
					if (CharOperation.equals(pb.compoundName, exported.compoundName)) {
						skope.problemReporter().exportingForeignPackage(stat, req);
						return;
					}
				}
			}
			skope.problemReporter().invalidPackageReference(IProblem.PackageDoesNotExistOrIsEmpty, stat);
		}
	}

	public void analyseModuleGraph(CompilationUnitScope skope) {
		if (this.requires != null) {
			// collect transitively:
			Map<String, Set<ModuleBinding>> pack2mods = new HashMap<>();
			for (ModuleBinding requiredModule : this.binding.getAllRequiredModules()) {
				for (PlainPackageBinding exportedPackage : requiredModule.getExports()) {
					if (this.binding.canAccess(exportedPackage)) {
						String packName = String.valueOf(exportedPackage.readableName());
						Set<ModuleBinding> mods = pack2mods.get(packName);
						if (mods == null)
							pack2mods.put(packName, mods = new HashSet<>());
						mods.add(requiredModule);
					}
				}
			}
			// report against the causing requires directives:
			for (RequiresStatement requiresStat : this.requires) {
				ModuleBinding requiredModule = requiresStat.resolvedBinding;
				if (requiredModule != null) {
					if (requiredModule.isDeprecated())
						skope.problemReporter().deprecatedModule(requiresStat.module, requiredModule);
					analyseOneDependency(requiresStat, requiredModule, skope, pack2mods);
					if (requiresStat.isTransitive()) {
						for (ModuleBinding secondLevelModule : requiredModule.getAllRequiredModules())
							analyseOneDependency(requiresStat, secondLevelModule, skope, pack2mods);
					}
				}
			}
		}
	}

	private void analyseOneDependency(RequiresStatement requiresStat, ModuleBinding requiredModule, CompilationUnitScope skope,
			Map<String, Set<ModuleBinding>> pack2mods)
	{
		for (PlainPackageBinding pack : requiredModule.getExports()) {
			Set<ModuleBinding> mods = pack2mods.get(String.valueOf(pack.readableName()));
			if (mods != null && mods.size() > 1) {
				CompilerOptions compilerOptions = skope.compilerOptions();
				boolean inJdtDebugCompileMode = compilerOptions.enableJdtDebugCompileMode;
				if (!inJdtDebugCompileMode) {
					skope.problemReporter().conflictingPackagesFromModules(pack, mods, requiresStat.sourceStart, requiresStat.sourceEnd);
				}
			}
		}
	}

	public void traverse(ASTVisitor visitor, CompilationUnitScope unitScope) {
		visitor.visit(this, unitScope);
	}

	public StringBuffer printHeader(int indent, StringBuffer output) {
		if (this.annotations != null) {
			for (int i = 0; i < this.annotations.length; i++) {
				this.annotations[i].print(indent, output);
				if (i != this.annotations.length - 1)
					output.append(" "); //$NON-NLS-1$
			}
			output.append('\n');
		}
		if (isOpen()) {
			output.append("open "); //$NON-NLS-1$
		}
		output.append("module "); //$NON-NLS-1$
		output.append(CharOperation.charToString(this.moduleName));
		return output;
	}
	public StringBuffer printBody(int indent, StringBuffer output) {
		output.append(" {"); //$NON-NLS-1$
		if (this.requires != null) {
			for(int i = 0; i < this.requiresCount; i++) {
				output.append('\n');
				printIndent(indent + 1, output);
				this.requires[i].print(0, output);
			}
		}
		if (this.exports != null) {
			for(int i = 0; i < this.exportsCount; i++) {
				output.append('\n');
				this.exports[i].print(indent + 1, output);
			}
		}
		if (this.opens != null) {
			for(int i = 0; i < this.opensCount; i++) {
				output.append('\n');
				this.opens[i].print(indent + 1, output);
			}
		}
		if (this.uses != null) {
			for(int i = 0; i < this.usesCount; i++) {
				output.append('\n');
				this.uses[i].print(indent + 1, output);
			}
		}
		if (this.servicesCount != 0) {
			for(int i = 0; i < this.servicesCount; i++) {
				output.append('\n');
				this.services[i].print(indent + 1, output);
			}
		}
		output.append('\n');
		return printIndent(indent, output).append('}');
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		//
		printIndent(indent, output);
		printHeader(0, output);
		return printBody(indent, output);
	}

	@Override
	public void abort(int abortLevel, CategorizedProblem problem) {
		switch (abortLevel) {
			case AbortCompilation :
				throw new AbortCompilation(this.compilationResult, problem);
			case AbortCompilationUnit :
				throw new AbortCompilationUnit(this.compilationResult, problem);
			case AbortMethod :
				throw new AbortMethod(this.compilationResult, problem);
			default :
				throw new AbortType(this.compilationResult, problem);
		}
	}

	@Override
	public CompilationResult compilationResult() {
		return this.compilationResult;
	}

	@Override
	public CompilationUnitDeclaration getCompilationUnitDeclaration() {
		return this.scope.referenceCompilationUnit();
	}

	@Override
	public boolean hasErrors() {
		return this.ignoreFurtherInvestigation;
	}

	@Override
	public void tagAsHavingErrors() {
		this.ignoreFurtherInvestigation = true;
	}

	@Override
	public void tagAsHavingIgnoredMandatoryErrors(int problemId) {
		// Nothing to do for this context;
	}

	public String getModuleVersion() {
		if (this.scope != null) {
			LookupEnvironment env = this.scope.environment().root;
			return env.moduleVersion;
		}
		return null;
	}
}
