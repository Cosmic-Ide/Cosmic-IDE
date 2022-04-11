/*******************************************************************************
 * Copyright (c) 2016, 2017 GK Software AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.util.Messages;

/**
 * Used for superimposing external annotations (served by an {@link ITypeAnnotationWalker})
 * over signatures of a {@link SourceTypeBinding}.
 */
class ExternalAnnotationSuperimposer extends TypeBindingVisitor {

	public static void apply(SourceTypeBinding typeBinding, String externalAnnotationPath) {
		ZipFile zipFile = null;
		try {
			File annotationBase = new File(externalAnnotationPath);
			if (annotationBase.exists()) {
				String binaryTypeName = String.valueOf(typeBinding.constantPoolName());
				String relativeFileName = binaryTypeName.replace('.', '/')+ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX;

				InputStream input;
				if (annotationBase.isDirectory()) {
					input = new FileInputStream(externalAnnotationPath+'/'+relativeFileName);
				} else {
					zipFile = new ZipFile(externalAnnotationPath);
					ZipEntry zipEntry = zipFile.getEntry(relativeFileName);
					if (zipEntry == null)
						return;
					input = zipFile.getInputStream(zipEntry);
				}
				annotateType(typeBinding, new ExternalAnnotationProvider(input, binaryTypeName), typeBinding.environment);
			}
		} catch (FileNotFoundException e) {
			// file not found is expected
		} catch (IOException e) {
			typeBinding.scope.problemReporter().abortDueToInternalError(Messages.bind(Messages.abort_externaAnnotationFile,
						new String[] {String.valueOf(typeBinding.readableName()), externalAnnotationPath, e.getMessage()}));
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing
				}
		}
	}

	static void annotateType(SourceTypeBinding binding, ExternalAnnotationProvider provider, LookupEnvironment environment) {
		ITypeAnnotationWalker typeWalker = provider.forTypeHeader(environment);
		if (typeWalker != null && typeWalker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
			ExternalAnnotationSuperimposer visitor = new ExternalAnnotationSuperimposer(environment);
			TypeVariableBinding[] typeParameters = binding.typeVariables();
			for (int i = 0; i < typeParameters.length; i++) {
				if (visitor.go(typeWalker.toTypeParameter(true, i)))
					typeParameters[i] = visitor.superimpose(typeParameters[i], TypeVariableBinding.class);
			}
		}
		binding.externalAnnotationProvider = provider; // for superimposing method signatures
	}

	public static void annotateComponentBinding(RecordComponentBinding componentBinding, ExternalAnnotationProvider provider, LookupEnvironment environment) {
		char[] componentSignature = componentBinding.genericSignature();
		if (componentSignature == null && componentBinding.type != null)
			componentSignature = componentBinding.type.signature();
		// TODO: check - do we need a provider.forRecordComponent; won't the field be sufficient - SH?
		ITypeAnnotationWalker walker = provider.forField(componentBinding.name, componentSignature, environment);
		ExternalAnnotationSuperimposer visitor = new ExternalAnnotationSuperimposer(environment);
		if (visitor.go(walker))
			componentBinding.type = visitor.superimpose(componentBinding.type, TypeBinding.class);
	}

	public static void annotateFieldBinding(FieldBinding field, ExternalAnnotationProvider provider, LookupEnvironment environment) {
		char[] fieldSignature = field.genericSignature();
		if (fieldSignature == null && field.type != null)
			fieldSignature = field.type.signature();
		ITypeAnnotationWalker walker = provider.forField(field.name, fieldSignature, environment);
		ExternalAnnotationSuperimposer visitor = new ExternalAnnotationSuperimposer(environment);
		if (visitor.go(walker))
			field.type = visitor.superimpose(field.type, TypeBinding.class);
	}

	public static void annotateMethodBinding(MethodBinding method, Argument[] arguments, ExternalAnnotationProvider provider, LookupEnvironment environment) {
		char[] methodSignature = method.genericSignature();
		if (methodSignature == null)
			methodSignature = method.signature();
		ITypeAnnotationWalker walker = provider.forMethod(method.selector, methodSignature, environment);
		if (walker != null && walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
			ExternalAnnotationSuperimposer visitor = new ExternalAnnotationSuperimposer(environment);
			TypeVariableBinding[] typeParams = method.typeVariables;
			for (short i = 0; i < typeParams.length; i++) {
				if (visitor.go(walker.toTypeParameter(false, i)))
					typeParams[i] = visitor.superimpose(typeParams[i], TypeVariableBinding.class);
			}
			if (!method.isConstructor()) {
				if (visitor.go(walker.toMethodReturn()))
					method.returnType = visitor.superimpose(method.returnType, TypeBinding.class);
			}
			TypeBinding[] parameters = method.parameters;
			for (short i = 0; i < parameters.length; i++) {
				if (visitor.go(walker.toMethodParameter(i))) {
					parameters[i] = visitor.superimpose(parameters[i], TypeBinding.class);
					if (arguments != null && i < arguments.length)
						arguments[i].binding.type = parameters[i];
				}
			}
		}
	}

	private ITypeAnnotationWalker currentWalker;
	private TypeBinding typeReplacement;
	private LookupEnvironment environment;
	private boolean isReplacing;

	ExternalAnnotationSuperimposer(LookupEnvironment environment) {
		this.environment = environment;
	}

	/** for constructing a memento of the superimposer's current state. */
	private ExternalAnnotationSuperimposer(TypeBinding typeReplacement, boolean isReplacing, ITypeAnnotationWalker walker) {
		this.typeReplacement = typeReplacement;
		this.isReplacing = isReplacing;
		this.currentWalker = walker;
	}
	private ExternalAnnotationSuperimposer snapshot() {
		ExternalAnnotationSuperimposer memento = new ExternalAnnotationSuperimposer(this.typeReplacement, this.isReplacing, this.currentWalker);
		// soft reset:
		this.typeReplacement = null;
		this.isReplacing = false;
		return memento;
	}
	private void restore(ExternalAnnotationSuperimposer memento) {
		this.isReplacing = memento.isReplacing;
		this.currentWalker = memento.currentWalker;
	}

	boolean go(ITypeAnnotationWalker walker) {
		// hard reset:
		reset();
		this.typeReplacement = null;
		this.isReplacing = false;
		// and start anew:
		this.currentWalker = walker;
		return walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
	}

	<T extends TypeBinding> T superimpose(T type, Class<? extends T> cl) {
		TypeBindingVisitor.visit(this, type);
		if (cl.isInstance(this.typeReplacement))
			return cl.cast(this.typeReplacement);
		return type;
	}

	private TypeBinding goAndSuperimpose(ITypeAnnotationWalker walker, TypeBinding type) {
		// no reset here
		if (walker == ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER)
			return type;
		this.currentWalker = walker;

		TypeBindingVisitor.visit(this, type);

		if (this.typeReplacement == null)
			return type;
		this.isReplacing = true;
		TypeBinding answer = this.typeReplacement;
		this.typeReplacement = null;
		return answer;
	}

	@Override
	public boolean visit(ArrayBinding arrayBinding) {
		ExternalAnnotationSuperimposer memento = snapshot();
		try {
			int dims = arrayBinding.dimensions;
			AnnotationBinding[][] annotsOnDims = new AnnotationBinding[dims][];
			ITypeAnnotationWalker walker = this.currentWalker;
			for (int i = 0; i < dims; i++) {
				IBinaryAnnotation[] binaryAnnotations = walker.getAnnotationsAtCursor(arrayBinding.id, false);
				if (binaryAnnotations != ITypeAnnotationWalker.NO_ANNOTATIONS) {
					annotsOnDims[i] = BinaryTypeBinding.createAnnotations(binaryAnnotations, this.environment, null);
					this.isReplacing = true;
				} else {
					annotsOnDims[i] = Binding.NO_ANNOTATIONS;
				}
				walker = walker.toNextArrayDimension();
			}
			TypeBinding leafComponentType = goAndSuperimpose(walker, arrayBinding.leafComponentType());
			if (this.isReplacing) {
				this.typeReplacement = this.environment.createArrayType(leafComponentType, dims, AnnotatableTypeSystem.flattenedAnnotations(annotsOnDims));
			}
		} finally {
			restore(memento);
		}
		return false;
	}
	@Override
	public boolean visit(BaseTypeBinding baseTypeBinding) {
		return false; // no null annotations
	}
	@Override
	public boolean visit(IntersectionTypeBinding18 intersectionTypeBinding18) {
		return false; // shouldn't occur in declarations
	}
	@Override
	public boolean visit(ParameterizedTypeBinding parameterizedTypeBinding) {
		ExternalAnnotationSuperimposer memento = snapshot();
		try {
			IBinaryAnnotation[] binaryAnnotations = this.currentWalker.getAnnotationsAtCursor(parameterizedTypeBinding.id, false);
			AnnotationBinding[] annotations = Binding.NO_ANNOTATIONS;
			if (binaryAnnotations != ITypeAnnotationWalker.NO_ANNOTATIONS) {
				annotations = BinaryTypeBinding.createAnnotations(binaryAnnotations, this.environment, null);
				this.isReplacing = true;
			}

			TypeBinding[] typeArguments = parameterizedTypeBinding.typeArguments();
			TypeBinding[] newArguments = new TypeBinding[typeArguments.length];
			for (int i = 0; i < typeArguments.length; i++) {
				newArguments[i] = goAndSuperimpose(memento.currentWalker.toTypeArgument(i), typeArguments[i]);
			}
			if (this.isReplacing)
				this.typeReplacement = this.environment.createParameterizedType(parameterizedTypeBinding.genericType(), newArguments, parameterizedTypeBinding.enclosingType(), annotations);
			return false;
		} finally {
			restore(memento);
		}
	}
	@Override
	public boolean visit(RawTypeBinding rawTypeBinding) {
		return visit((ReferenceBinding)rawTypeBinding);
	}
	@Override
	public boolean visit(ReferenceBinding referenceBinding) {
		IBinaryAnnotation[] binaryAnnotations = this.currentWalker.getAnnotationsAtCursor(referenceBinding.id, false);
		if (binaryAnnotations != ITypeAnnotationWalker.NO_ANNOTATIONS)
			this.typeReplacement = this.environment.createAnnotatedType(referenceBinding, BinaryTypeBinding.createAnnotations(binaryAnnotations, this.environment, null));
		return false;
	}
	@Override
	public boolean visit(TypeVariableBinding typeVariable) {
		return visit((ReferenceBinding) typeVariable);
	}
	@Override
	public boolean visit(WildcardBinding wildcardBinding) {
		TypeBinding bound = wildcardBinding.bound;
		ExternalAnnotationSuperimposer memento = snapshot();
		try {
			if (bound != null) {
				bound = goAndSuperimpose(memento.currentWalker.toWildcardBound(), bound);
			}
			IBinaryAnnotation[] binaryAnnotations = memento.currentWalker.getAnnotationsAtCursor(-1, false);
			if (this.isReplacing || binaryAnnotations != ITypeAnnotationWalker.NO_ANNOTATIONS) {
				TypeBinding[] otherBounds = wildcardBinding.otherBounds;
				if (binaryAnnotations != ITypeAnnotationWalker.NO_ANNOTATIONS) {
					AnnotationBinding[] annotations = BinaryTypeBinding.createAnnotations(binaryAnnotations, this.environment, null);
					this.typeReplacement = this.environment.createWildcard(wildcardBinding.genericType, wildcardBinding.rank, bound, otherBounds, wildcardBinding.boundKind, annotations);
				} else {
					this.typeReplacement = this.environment.createWildcard(wildcardBinding.genericType, wildcardBinding.rank, bound, otherBounds, wildcardBinding.boundKind);
				}
			}
		} finally {
			restore(memento);
		}
		return false;
	}
}
