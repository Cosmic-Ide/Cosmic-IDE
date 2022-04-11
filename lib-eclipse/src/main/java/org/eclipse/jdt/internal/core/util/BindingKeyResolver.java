/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *     							Bug 425183 - [1.8][inference] make CaptureBinding18 safe
 *								Bug 466308 - [hovering] Javadoc header for parameter is wrong with annotation-based null analysis
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import java.util.ArrayList;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CaptureBinding;
import org.eclipse.jdt.internal.compiler.lookup.CaptureBinding18;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.PlainPackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.PolymorphicMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.RawTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.WildcardBinding;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BindingKeyResolver extends BindingKeyParser {

	/** Synthetic bindings for local variables (method arguments) restored from a binding key. */
	private static final class SyntheticLocalVariableBinding extends LocalVariableBinding {

		private final MethodBinding enclosingMethod;
		private int paramPosition;
		private char[] key;

		SyntheticLocalVariableBinding(char[] name, TypeBinding type, MethodBinding enclosingMethod, int paramPosition) {
			super(name, type, 0, true);
			this.enclosingMethod = enclosingMethod;
			this.paramPosition = paramPosition;
		}

		@Override
		public char[] computeUniqueKey() {
			if (this.key == null) {
				// have no scope to find the enclosing method, so use the captured method:
				StringBuilder buf = new StringBuilder().append(this.enclosingMethod.computeUniqueKey());
				buf.append('#');
				buf.append(this.name);
				buf.append("#0#"); //$NON-NLS-1$
				buf.append(this.paramPosition);
				int length = buf.length();
				this.key = new char[length];
				buf.getChars(0, length, this.key, 0);
			}
			return this.key;
		}

		@Override
		public MethodBinding getEnclosingMethod() {
			return this.enclosingMethod;
		}

		@Override
		public int hashCode() {
			return CharOperation.hashCode(computeUniqueKey());
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SyntheticLocalVariableBinding))
				return false;
			return CharOperation.equals(computeUniqueKey(), ((SyntheticLocalVariableBinding) obj).computeUniqueKey());
		}
	}

	Compiler compiler;
	Binding compilerBinding;

	char[][] compoundName;
	char[] moduleName;
	int dimension;
	LookupEnvironment environment;
	ReferenceBinding genericType;
	MethodBinding methodBinding;
	AnnotationBinding annotationBinding;

	char[] secondarySimpleName;
	CompilationUnitDeclaration parsedUnit;
	BlockScope scope;
	TypeBinding typeBinding;
	TypeDeclaration typeDeclaration;
	ArrayList types = new ArrayList();

	int wildcardRank;

	CompilationUnitDeclaration outerMostParsedUnit;

	/*
	 * A hash set of the file names of already resolved units
	 */
	HashtableOfObject resolvedUnits;

	private BindingKeyResolver(BindingKeyParser parser, Compiler compiler, LookupEnvironment environment, CompilationUnitDeclaration outerMostParsedUnit, HashtableOfObject parsedUnits) {
		super(parser);
		this.compiler = compiler;
		this.environment = environment;
		this.outerMostParsedUnit = outerMostParsedUnit;
		this.resolvedUnits = parsedUnits;
	}

	public BindingKeyResolver(String key, Compiler compiler, LookupEnvironment environment) {
		super(key);
		this.compiler = compiler;
		this.environment = environment;
		this.resolvedUnits = new HashtableOfObject();
	}

	/*
	 * If not already cached, computes and cache the compound name (pkg name + top level name) of this key.
	 * Returns the package name if key is a pkg key.
	 * Returns an empty array if malformed.
	 * This key's scanner should be positioned on the package or type token.
	 */
	public char[][] compoundName() {
		return this.compoundName;
	}

	public char[] moduleName() {
		return this.moduleName;
	}

	@Override
	public void consumeAnnotation() {
		int size = this.types.size();
		if (size == 0) return;
		Binding annotationType = ((BindingKeyResolver) this.types.get(size-1)).compilerBinding;
		AnnotationBinding[] annotationBindings;
		if (this.compilerBinding == null && this.typeBinding instanceof ReferenceBinding) {
			annotationBindings = ((ReferenceBinding) this.typeBinding).getAnnotations();
		} else if (this.compilerBinding instanceof MethodBinding) {
			annotationBindings = ((MethodBinding) this.compilerBinding).getAnnotations();
		} else if (this.compilerBinding instanceof VariableBinding) {
			annotationBindings = ((VariableBinding) this.compilerBinding).getAnnotations();
		} else {
			return;
		}
		for (int i = 0, length = annotationBindings.length; i < length; i++) {
			AnnotationBinding binding = annotationBindings[i];
			if (binding.getAnnotationType() == annotationType) {
				this.annotationBinding = binding;
				break;
			}
		}
	}

	@Override
	public void consumeArrayDimension(char[] brakets) {
		this.dimension = brakets.length;
	}

	@Override
	public void consumeBaseType(char[] baseTypeSig) {
		this.compoundName = new char[][] {getKey().toCharArray()};
		TypeBinding baseTypeBinding = getBaseTypeBinding(baseTypeSig);
		if (baseTypeBinding != null) {
			this.typeBinding = baseTypeBinding;
		}
	}

	@Override
	public void consumeCapture18ID(int id, int position) {
		consumeAnyCapture(id, position);
	}

	@Override
	public void consumeCapture(final int position) {
		consumeAnyCapture(-1, position);
	}
	public void consumeAnyCapture(final int capture18id, final int position) {
		CompilationUnitDeclaration outerParsedUnit = this.outerMostParsedUnit == null ? this.parsedUnit : this.outerMostParsedUnit;
		if (outerParsedUnit == null) return;
		final Binding wildcardBinding = this.types.size() > 0  // 0 may happen for CaptureBinding18
				? ((BindingKeyResolver) this.types.get(0)).compilerBinding : null;
		class CaptureFinder extends ASTVisitor {
			CaptureBinding capture;
			boolean checkType(TypeBinding binding) {
				if (binding == null)
					return false;
				switch (binding.kind()) {
					case Binding.PARAMETERIZED_TYPE:
						TypeBinding[] arguments = ((ParameterizedTypeBinding) binding).arguments;
						if (arguments == null) return false;
						for (int i = 0, length = arguments.length; i < length; i++) {
							if (checkType(arguments[i]))
								return true;
						}
						break;
					case Binding.WILDCARD_TYPE:
						return checkType(((WildcardBinding) binding).bound);
					case Binding.INTERSECTION_TYPE:
						if (checkType(((WildcardBinding) binding).bound))
							return true;
						TypeBinding[] otherBounds = ((WildcardBinding) binding).otherBounds;
						// per construction, otherBounds is never null
						for (int i = 0, length = otherBounds.length; i < length; i++) {
							if (checkType(otherBounds[i]))
								return true;
						}
						break;
					case Binding.ARRAY_TYPE:
						return checkType(((ArrayBinding) binding).leafComponentType);
					case Binding.TYPE_PARAMETER:
						if (binding.isCapture()) {
							CaptureBinding captureBinding = (CaptureBinding) binding;
							if (captureBinding.end == position && captureBinding.wildcard == wildcardBinding) {
								if (captureBinding instanceof CaptureBinding18) {
									if (((CaptureBinding18)captureBinding).captureID != capture18id)
										return false;
								}
								this.capture = captureBinding;
								return true;
							}
						}
						break;
				}
				return false;
			}
			@Override
			public boolean visit(SingleNameReference singleNameReference, BlockScope blockScope) {
				if (checkType(singleNameReference.resolvedType))
					return false;
				return super.visit(singleNameReference, blockScope);
			}
			@Override
			public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope blockScope) {
				if (checkType(qualifiedNameReference.resolvedType))
					return false;
				return super.visit(qualifiedNameReference, blockScope);
			}
			@Override
			public boolean visit(MessageSend messageSend, BlockScope blockScope) {
				if (checkType(messageSend.resolvedType))
					return false;
				return super.visit(messageSend, blockScope);
			}
			@Override
			public boolean visit(FieldReference fieldReference, BlockScope blockScope) {
				if (checkType(fieldReference.resolvedType))
					return false;
				return super.visit(fieldReference, blockScope);
			}
			@Override
			public boolean visit(ConditionalExpression conditionalExpression, BlockScope blockScope) {
				if (checkType(conditionalExpression.resolvedType))
					return false;
				return super.visit(conditionalExpression, blockScope);
			}
			@Override
			public boolean visit(CastExpression castExpression, BlockScope blockScope) {
				if (checkType(castExpression.resolvedType))
					return false;
				return super.visit(castExpression, blockScope);
			}
			@Override
			public boolean visit(Assignment assignment, BlockScope blockScope) {
				if (checkType(assignment.resolvedType))
					return false;
				return super.visit(assignment, blockScope);
			}
			@Override
			public boolean visit(ArrayReference arrayReference, BlockScope blockScope) {
				if (checkType(arrayReference.resolvedType))
					return false;
				return super.visit(arrayReference, blockScope);
			}
		}
		CaptureFinder captureFinder = new CaptureFinder();
		outerParsedUnit.traverse(captureFinder, outerParsedUnit.scope);
		this.typeBinding = captureFinder.capture;
	}

	@Override
	public void consumeException() {
		this.types = new ArrayList();
	}

	@Override
	public void consumeField(char[] fieldName) {
		if (this.typeBinding == null)
			return;
		FieldBinding[] fields = ((ReferenceBinding) this.typeBinding).availableFields(); // resilience
	 	for (int i = 0, length = fields.length; i < length; i++) {
			FieldBinding field = fields[i];
			if (CharOperation.equals(fieldName, field.name)) {
				this.typeBinding = null;
				this.compilerBinding = field;
				return;
			}
		}
	}

	@Override
	public void consumeParameterizedGenericMethod() {
		if (this.methodBinding == null)
			return;
		TypeBinding[] arguments = getTypeBindingArguments();
		if (arguments == null) {
			this.methodBinding = null;
			this.compilerBinding = null;
			return;
		}
		if (arguments.length != this.methodBinding.typeVariables().length)
			this.methodBinding = this.environment.createParameterizedGenericMethod(this.methodBinding, (RawTypeBinding) null);
		else
	 		this.methodBinding = this.environment.createParameterizedGenericMethod(this.methodBinding, arguments);
		this.compilerBinding = this.methodBinding;
	}

	@Override
	public void consumeLocalType(char[] uniqueKey) {
		if (this.parsedUnit == null) {
			this.typeBinding = null;
			return;
		}
 		for (LocalTypeBinding localTypeBinding : this.parsedUnit.localTypes.values()) {
 			if (CharOperation.equals(uniqueKey, localTypeBinding.computeUniqueKey(false/*not a leaf*/))) {
 				this.typeBinding = localTypeBinding;
 				return;
 			}
 		}
	}

	@Override
	public void consumeLocalVar(char[] varName, int occurrenceCount, int argumentPosition) {
		if (this.scope == null) {
			if (this.methodBinding == null)
				return;
			AbstractMethodDeclaration sourceMethod = this.methodBinding.sourceMethod();
			if (sourceMethod != null) {
				this.scope = sourceMethod.scope;
			} else {
				char[][] parameterNames = this.methodBinding.parameterNames;
				int paramPosition = -1;
				if (parameterNames.length == 0) {
					paramPosition = argumentPosition;
				} else {
					for (int i = 0; i < parameterNames.length; i++) {
						if (CharOperation.equals(parameterNames[i], varName)) {
							paramPosition = i;
							break;
						}
					}
				}
				if (paramPosition != -1) {
					// we don't have a compiler binding for this argument, but we can craft one:
					this.compilerBinding = new SyntheticLocalVariableBinding(varName, this.methodBinding.parameters[paramPosition], this.methodBinding, paramPosition);
					this.methodBinding = null;
					return;
				}
			}
		}
		if (this.scope != null) {
		 	for (int i = 0; i < this.scope.localIndex; i++) {
				LocalVariableBinding local = this.scope.locals[i];
				if (CharOperation.equals(local.name, varName)
						&& occurrenceCount-- == 0) {
					this.methodBinding = null;
					this.compilerBinding = local;
					return;
				}
			}
		}
	}

	@Override
	public void consumeMethod(char[] selector, char[] signature) {
		if (this.typeBinding == null)
			return;
		MethodBinding[] methods = ((ReferenceBinding) this.typeBinding).availableMethods(); // resilience
	 	for (int i = 0, methodLength = methods.length; i < methodLength; i++) {
			MethodBinding method = methods[i];
			if (CharOperation.equals(selector, method.selector) || (selector.length == 0 && method.isConstructor())) {
				char[] methodSignature = method.genericSignature();
				if (methodSignature == null)
					methodSignature = method.signature();
				if (CharOperation.equals(signature, methodSignature)) {
					this.typeBinding = null;
					this.methodBinding = method;
					this.compilerBinding = this.methodBinding;
					return;
				} else if ((method.tagBits & TagBits.AnnotationPolymorphicSignature) != 0) {
					this.typeBinding = null;
					char[][] typeParameters = Signature.getParameterTypes(signature);
					int length = typeParameters.length;
					TypeBinding[] parameterTypes = new TypeBinding[length];
					for (int j = 0; j < length; j++) {
						parameterTypes[j] = getType(typeParameters[j]);
					}
					PolymorphicMethodBinding polymorphicMethod = this.environment.createPolymorphicMethod(method, parameterTypes, this.scope);
					this.methodBinding = polymorphicMethod;
					this.methodBinding = this.environment.updatePolymorphicMethodReturnType(
							polymorphicMethod,
							getType(Signature.getReturnType(signature)));
					this.compilerBinding = this.methodBinding;
					return;
				}
			}
		}
	}

	private TypeBinding getType(char[] type) {
		TypeBinding binding = null;
		int length = type.length;
		switch(length) {
			case 1 :
				switch (type[0]) {
					case 'I' :
						binding = TypeBinding.INT;
						break;
					case 'Z' :
						binding = TypeBinding.BOOLEAN;
						break;
					case 'V' :
						binding = TypeBinding.VOID;
						break;
					case 'C' :
						binding = TypeBinding.CHAR;
						break;
					case 'D' :
						binding = TypeBinding.DOUBLE;
						break;
					case 'B' :
						binding = TypeBinding.BYTE;
						break;
					case 'F' :
						binding = TypeBinding.FLOAT;
						break;
					case 'J' :
						binding = TypeBinding.LONG;
						break;
					case 'S' :
						binding = TypeBinding.SHORT;
						break;
				}
				break;
			default:
				int dimensions = 0;
				int start = 0;
				while (type[start] == '[') {
					start++;
					dimensions++;
				}
				binding = this.environment.getType(CharOperation.splitOn('/', type, start + 1, length - 1));
				if (dimensions != 0) {
					binding = this.environment.createArrayType(binding, dimensions);
				}
		}
		return binding;
	}
	@Override
	public void consumeMemberType(char[] simpleTypeName) {
		this.typeBinding = getTypeBinding(simpleTypeName);
	}

	@Override
	public void consumePackage(char[] pkgName) {
		this.compoundName = CharOperation.splitOn('/', pkgName);
		this.compilerBinding = new PlainPackageBinding(this.compoundName, null, this.environment, this.environment.module); //TODO(SHMOD) enclosingModule
	}

	@Override
	public void consumeParameterizedType(char[] simpleTypeName, boolean isRaw) {
		if (this.typeBinding == null)
			return;
		TypeBinding[] arguments = getTypeBindingArguments();
		if (arguments == null) {
			this.typeBinding = null;
			this.genericType = null;
			return;
		}
		if (simpleTypeName != null) {
			if (this.genericType == null) {
				// parameterized member type with raw enclosing type
				this.genericType = ((ReferenceBinding) this.typeBinding).getMemberType(simpleTypeName);
			} else {
				// parameterized member type with parameterized enclosing type
				this.genericType = this.genericType.getMemberType(simpleTypeName);
			}
			if (!isRaw)
				this.typeBinding = this.environment.createParameterizedType(this.genericType, arguments, (ReferenceBinding) this.typeBinding);
			else
				// raw type
				this.typeBinding = this.environment.createRawType(this.genericType, (ReferenceBinding) this.typeBinding);
		} else {
			// parameterized top level type or parameterized member type with raw enclosing type
			this.genericType = (ReferenceBinding) this.typeBinding;
			ReferenceBinding enclosing = this.genericType.enclosingType();
			if (enclosing != null) enclosing = (ReferenceBinding) this.environment.convertToRawType(enclosing, false /*do not force conversion of enclosing types*/);
			this.typeBinding = this.environment.createParameterizedType(this.genericType, arguments, enclosing);
		}
	}


	@Override
	public void consumeParser(BindingKeyParser parser) {
		this.types.add(parser);
	}

	@Override
	public void consumeScope(int scopeNumber) {
		if (this.scope == null) {
			if (this.methodBinding == null)
				return;
			this.scope = this.methodBinding.sourceMethod().scope;
		}
		if (scopeNumber >= this.scope.subscopeCount)
			return; // malformed key
		this.scope = (BlockScope) this.scope.subscopes[scopeNumber];
	}

	@Override
	public void consumeRawType() {
		if (this.typeBinding == null) return;
		this.typeBinding = this.environment.convertToRawType(this.typeBinding, false /*do not force conversion of enclosing types*/);
	}
	@Override
	public void consumeSecondaryType(char[] simpleTypeName) {
		this.secondarySimpleName = simpleTypeName;
	}

	@Override
	public void consumeFullyQualifiedName(char[] fullyQualifiedName) {
		this.compoundName = CharOperation.splitOn('/', fullyQualifiedName);
	}

	@Override
	public void consumeTopLevelType() {
		char[] fileName;
		this.parsedUnit = getCompilationUnitDeclaration();
		if (this.parsedUnit != null && this.compiler != null && !this.resolvedUnits.containsKey(fileName = this.parsedUnit.getFileName())) {
			this.compiler.process(this.parsedUnit, this.compiler.totalUnits+1); // unit is resolved only once thanks to the resolvedUnits protection
			this.resolvedUnits.put(fileName, fileName);
		}
		if (this.parsedUnit == null) {
			this.typeBinding = getBinaryBinding();
		} else {
			char[] typeName = this.secondarySimpleName == null ? this.compoundName[this.compoundName.length-1] : this.secondarySimpleName;
			this.typeBinding = getTypeBinding(typeName);
		}
	}

	@Override
	public void consumeKey() {
		if (this.typeBinding != null) {
			this.typeBinding = getArrayBinding(this.dimension, this.typeBinding);
			this.compilerBinding = this.typeBinding;
		}
	}

	@Override
	public void consumeTypeVariable(char[] position, char[] typeVariableName) {
		if (position.length > 0) {
			if (this.typeBinding == null)
				return;
			int pos = Integer.parseInt(new String(position));
			MethodBinding[] methods = ((ReferenceBinding) this.typeBinding).availableMethods(); // resilience
			if (methods != null && pos < methods.length) {
				this.methodBinding = methods[pos];
			}
		}
	 	TypeVariableBinding[] typeVariableBindings;
	 	if (this.methodBinding != null) {
	 		typeVariableBindings = this.methodBinding.typeVariables();
	 	} else if (this.typeBinding != null) {
	 		typeVariableBindings = this.typeBinding.typeVariables();
	 	} else {
	 		return;
	 	}
	 	for (int i = 0, length = typeVariableBindings.length; i < length; i++) {
			TypeVariableBinding typeVariableBinding = typeVariableBindings[i];
			if (CharOperation.equals(typeVariableName, typeVariableBinding.sourceName())) {
				this.typeBinding = typeVariableBinding;
				return;
			}
		}
	}

	@Override
	public void consumeTypeWithCapture() {
		BindingKeyResolver resolver = (BindingKeyResolver) this.types.get(0);
		this.typeBinding =(TypeBinding) resolver.compilerBinding;
	}

	@Override
	public void consumeWildcardRank(int aRank) {
		this.wildcardRank = aRank;
	}

	@Override
	public void consumeWildCard(int kind) {
		switch (kind) {
			case Wildcard.EXTENDS:
			case Wildcard.SUPER:
				BindingKeyResolver boundResolver = (BindingKeyResolver) this.types.get(0);
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157847, do not allow creation of
				// internally inconsistent wildcards of the form '? super <null>' or '? extends <null>'
				final Binding boundBinding = boundResolver.compilerBinding;
				if (boundBinding instanceof TypeBinding) {
					this.typeBinding = this.environment.createWildcard((ReferenceBinding) this.typeBinding, this.wildcardRank, (TypeBinding) boundBinding, null /*no extra bound*/, kind);
				} else {
					this.typeBinding = null;
				}
				break;
			case Wildcard.UNBOUND:
				this.typeBinding = this.environment.createWildcard((ReferenceBinding) this.typeBinding, this.wildcardRank, null/*no bound*/, null /*no extra bound*/, kind);
				break;
		}
	}

	@Override
	public void consumeModule(char[] aModuleName) {
		this.moduleName = aModuleName;
		this.compilerBinding = this.environment.getModule(aModuleName);
	}

	public AnnotationBinding getAnnotationBinding() {
		return this.annotationBinding;
	}

	/*
	 * If the given dimension is greater than 0 returns an array binding for the given type binding.
	 * Otherwise return the given type binding.
	 * Returns null if the given type binding is null.
	 */
	private TypeBinding getArrayBinding(int dim, TypeBinding binding) {
		if (binding == null) return null;
		if (dim == 0) return binding;
		return this.environment.createArrayType(binding, dim);
	}

	private TypeBinding getBaseTypeBinding(char[] signature) {
		switch (signature[0]) {
			case 'I' :
				return TypeBinding.INT;
			case 'Z' :
				return TypeBinding.BOOLEAN;
			case 'V' :
				return TypeBinding.VOID;
			case 'C' :
				return TypeBinding.CHAR;
			case 'D' :
				return TypeBinding.DOUBLE;
			case 'B' :
				return TypeBinding.BYTE;
			case 'F' :
				return TypeBinding.FLOAT;
			case 'J' :
				return TypeBinding.LONG;
			case 'S' :
				return TypeBinding.SHORT;
			case 'N':
				return TypeBinding.NULL;
			default :
				return null;
		}
	}

	/*
	 * Returns a binary binding corresonding to this key's compound name.
	 * Returns null if not found.
	 */
	private TypeBinding getBinaryBinding() {
		if (this.compoundName.length == 0) return null;
		return this.environment.getType(this.compoundName);
	}

	/*
	 * Finds the compilation unit declaration corresponding to the key in the given lookup environment.
	 * Returns null if no compilation unit declaration could be found.
	 * This key's scanner should be positioned on the package token.
	 */
	public CompilationUnitDeclaration getCompilationUnitDeclaration() {
		char[][] name = this.compoundName;
		if (name.length == 0) return null;
		if (this.environment == null) return null;
		ReferenceBinding binding = this.environment.getType(name);
		if (!(binding instanceof SourceTypeBinding)) {
			if (this.secondarySimpleName == null)
				return null;
			// case of a secondary type with no primary type (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=177115)
			int length = name.length;
			System.arraycopy(name, 0, name = new char[length][], 0, length-1);
			name[length-1] = this.secondarySimpleName;
			binding = this.environment.getType(name);
			if (!(binding instanceof SourceTypeBinding))
				return null;
		}
		SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) binding;
		if (sourceTypeBinding.scope == null)
			return null;
		return sourceTypeBinding.scope.compilationUnitScope().referenceContext;
	}

	/*
	 * Returns the compiler binding corresponding to this key.
	 * Returns null is malformed.
	 * This key's scanner should be positioned on the package token.
	 */
	public Binding getCompilerBinding() {
		try {
			parse();
			return this.compilerBinding;
		} catch (RuntimeException e) {
			Util.log(e, "Could not create binding from binding key: " + getKey()); //$NON-NLS-1$
			return null;
		}
	}

	private TypeBinding getTypeBinding(char[] simpleTypeName) {
		if (this.typeBinding instanceof ReferenceBinding) {
			return ((ReferenceBinding) this.typeBinding).getMemberType(simpleTypeName);
		}
		TypeDeclaration[] typeDeclarations =
			this.typeDeclaration == null ?
				(this.parsedUnit == null ? null : this.parsedUnit.types) :
				this.typeDeclaration.memberTypes;
		if (typeDeclarations == null) return null;
		for (int i = 0, length = typeDeclarations.length; i < length; i++) {
			TypeDeclaration declaration = typeDeclarations[i];
			if (CharOperation.equals(simpleTypeName, declaration.name)) {
				this.typeDeclaration = declaration;
				return declaration.binding;
			}
		}
		return null;
	}

	private TypeBinding[] getTypeBindingArguments() {
		int size = this.types.size();
		TypeBinding[] arguments = new TypeBinding[size];
		for (int i = 0; i < size; i++) {
			BindingKeyResolver resolver = (BindingKeyResolver) this.types.get(i);
			TypeBinding compilerBinding2 = (TypeBinding) resolver.compilerBinding;
			if (compilerBinding2 == null) {
				this.types = new ArrayList();
				return null;
			}
			arguments[i] = compilerBinding2;
		}
		this.types = new ArrayList();
		return arguments;
	}

	@Override
	public void malformedKey() {
		this.compoundName = CharOperation.NO_CHAR_CHAR;
	}

	@Override
	public BindingKeyParser newParser() {
		return new BindingKeyResolver(this, this.compiler, this.environment, this.outerMostParsedUnit == null ? this.parsedUnit : this.outerMostParsedUnit, this.resolvedUnits);
	}

	@Override
	public String toString() {
		return getKey();
	}

}
