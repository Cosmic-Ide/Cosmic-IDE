/*******************************************************************************
 * Copyright (c) 2014, 2017 GK Software AG.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.SignatureWrapper;
import org.eclipse.jdt.internal.compiler.util.Util;

public class ExternalAnnotationProvider {

	public static final String ANNOTATION_FILE_EXTENSION= "eea"; //$NON-NLS-1$
	public static final String CLASS_PREFIX = "class "; //$NON-NLS-1$
	public static final String SUPER_PREFIX = "super "; //$NON-NLS-1$

	/** Representation of a 'nullable' annotation, independent of the concrete annotation name used in Java sources. */
	public static final char NULLABLE = '0';

	/** Representation of a 'nonnull' annotation, independent of the concrete annotation name used in Java sources. */
	public static final char NONNULL = '1';

	/**
	 * Represents absence of a null annotation. Useful for removing an existing null annotation.
	 * This character is used only internally, it is not part of the Eclipse External Annotation file format.
	 */
	public static final char NO_ANNOTATION = '@';

	public static final String ANNOTATION_FILE_SUFFIX = ".eea"; //$NON-NLS-1$

	private static final String TYPE_PARAMETER_PREFIX = " <"; //$NON-NLS-1$


	private String typeName;
	String typeParametersAnnotationSource;
	Map<String,String> supertypeAnnotationSources;
	private Map<String,String> methodAnnotationSources;
	private Map<String,String> fieldAnnotationSources;

	/**
	 * Create and initialize.
	 * @param input open input stream to read the annotations from, will be closed by the constructor.
	 * @param typeName slash-separated qualified name of a type
	 * @throws IOException various issues when accessing the annotation file
	 */
	public ExternalAnnotationProvider(InputStream input, String typeName) throws IOException {
		this.typeName = typeName;
		initialize(input);
	}

	private void initialize(InputStream input) throws IOException {
		try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(input))) {
			assertClassHeader(reader.readLine(), this.typeName);

			String line;
			if ((line = reader.readLine()) == null) {
				return;
			}
			if (line.startsWith(TYPE_PARAMETER_PREFIX)) {
				if ((line = reader.readLine()) == null) // skip first line, second line may contain type parameter annotations
					return;
				if (line.startsWith(TYPE_PARAMETER_PREFIX)) {
					this.typeParametersAnnotationSource = line.substring(TYPE_PARAMETER_PREFIX.length());
					if ((line = reader.readLine()) == null)
						return;
				}
			}
			String pendingLine;
			do {
				pendingLine = null;
				line = line.trim();
				if (line.isEmpty()) continue;
				String rawSig = null, annotSig = null;
				// selector:
				String selector = line;
				boolean isSuper = selector.startsWith(SUPER_PREFIX);
				if (isSuper)
					selector = selector.substring(SUPER_PREFIX.length());
				int errLine = -1;
				try {
					// raw signature:
					line = reader.readLine();
					if (line != null && !line.isEmpty() && line.charAt(0) == ' ') // first signature line is mandatory
						rawSig = line.substring(1);
					else
						errLine = reader.getLineNumber();
					// annotated signature:
					line = reader.readLine();
					if (line == null || line.isEmpty())
						continue; // skip since optional line with annotations is missing
					if (line.charAt(0) != ' ') {
						pendingLine = line; // push back what appears to be the next selector, not a signature
						continue;
					}
					annotSig = line.substring(1);
				} catch (Exception ex) {
					// continue to escalate below
				}
				if (rawSig == null || annotSig == null) {
					if (errLine == -1) errLine = reader.getLineNumber();
					throw new IOException("Illegal format in annotation file for "+this.typeName+" at line "+errLine); //$NON-NLS-1$ //$NON-NLS-2$
				}
				// discard optional meta data (separated by whitespace):
				annotSig = trimTail(annotSig);
				if (isSuper) {
					if (this.supertypeAnnotationSources == null)
						this.supertypeAnnotationSources = new HashMap<>();
					this.supertypeAnnotationSources.put('L'+selector+rawSig+';', annotSig);
				} else if (rawSig.contains("(")) { //$NON-NLS-1$
					if (this.methodAnnotationSources == null)
						this.methodAnnotationSources = new HashMap<>();
					this.methodAnnotationSources.put(selector+rawSig, annotSig);
				} else {
					if (this.fieldAnnotationSources == null)
						this.fieldAnnotationSources = new HashMap<>();
					this.fieldAnnotationSources.put(selector+':'+rawSig, annotSig);
				}
			} while (((line = pendingLine) != null) || (line = reader.readLine()) != null);
		}
	}

	/**
	 * Assert that the given line is a class header for 'typeName' (slash-separated qualified name).
	 */
	public static void assertClassHeader(String line, String typeName) throws IOException {
		if (line != null && line.startsWith(CLASS_PREFIX)) {
			line = line.substring(CLASS_PREFIX.length());
		} else {
			throw new IOException("missing class header in annotation file for "+typeName); //$NON-NLS-1$
		}
		if (!trimTail(line).equals(typeName)) {
			throw new IOException("mismatching class name in annotation file, expected "+typeName+", but header said "+line); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Extract the signature from a line of an external annotation file.
	 * Answers null if line is not in the expected format.
	 */
	public static String extractSignature(String line) {
		if (line == null || line.isEmpty() || line.charAt(0) != ' ')
			return null;
		return trimTail(line.substring(1));
	}

	/** Lines may contain arbitrary trailing data, separated by white space. */
	protected static String trimTail(String line) {
		int tail = line.indexOf(' ');
		if (tail == -1)
			tail = line.indexOf('\t');
		if (tail != -1)
			return line.substring(0, tail);
		return line;
	}

	public ITypeAnnotationWalker forTypeHeader(LookupEnvironment environment) {
		if (this.typeParametersAnnotationSource != null || this.supertypeAnnotationSources != null)
			return new DispatchingAnnotationWalker(environment);
		return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
	}

	public ITypeAnnotationWalker forMethod(char[] selector, char[] signature, LookupEnvironment environment) {
		Map<String, String> sources = this.methodAnnotationSources;
		if (sources != null) {
			String source = sources.get(String.valueOf(CharOperation.concat(selector, signature)));
			if (source != null)
				return new MethodAnnotationWalker(source.toCharArray(), 0, environment);
		}
		return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
	}

	public ITypeAnnotationWalker forField(char[] selector, char[] signature, LookupEnvironment environment) {
		if (this.fieldAnnotationSources != null) {
			String source = this.fieldAnnotationSources.get(String.valueOf(CharOperation.concat(selector, signature, ':')));
			if (source != null)
				return new FieldAnnotationWalker(source.toCharArray(), 0, environment);
		}
		return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("External Annotations for ").append(this.typeName).append('\n'); //$NON-NLS-1$
		sb.append("Methods:\n"); //$NON-NLS-1$
		if (this.methodAnnotationSources != null)
			for (Entry<String,String> e : this.methodAnnotationSources.entrySet())
				sb.append('\t').append(e.getKey()).append('\n');
		return sb.toString();
	}

	abstract static class SingleMarkerAnnotation implements IBinaryAnnotation {
		@Override
		public IBinaryElementValuePair[] getElementValuePairs() {
			return ElementValuePairInfo.NoMembers;
		}
		@Override
		public boolean isExternalAnnotation() {
			return true;
		}
		protected char[] getBinaryTypeName(char[][] name) {
			return CharOperation.concat('L', CharOperation.concatWith(name, '/'), ';');
		}
	}

	SingleMarkerAnnotation NULLABLE_ANNOTATION, NONNULL_ANNOTATION;

	void initAnnotations(final LookupEnvironment environment) {
		if (this.NULLABLE_ANNOTATION == null) {
			this.NULLABLE_ANNOTATION = new SingleMarkerAnnotation() {
				@Override public char[] getTypeName() { return getBinaryTypeName(environment.getNullableAnnotationName()); }
			};
		}
		if (this.NONNULL_ANNOTATION == null) {
			this.NONNULL_ANNOTATION = new SingleMarkerAnnotation() {
				@Override public char[] getTypeName() { return getBinaryTypeName(environment.getNonNullAnnotationName()); }
			};
		}
	}

	/**
	 * Walker for top-level elements of a type (type parameters & super types),
	 * which dispatches to specialized walkers for those details.
	 */
	class DispatchingAnnotationWalker implements ITypeAnnotationWalker {

		private LookupEnvironment environment;
		private TypeParametersAnnotationWalker typeParametersWalker;

		public DispatchingAnnotationWalker(LookupEnvironment environment) {
			this.environment = environment;
		}
		@Override
		public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) {
			String source = ExternalAnnotationProvider.this.typeParametersAnnotationSource;
			if (source != null) {
				if (this.typeParametersWalker == null)
					this.typeParametersWalker = new TypeParametersAnnotationWalker(source.toCharArray(), 0, 0, null, this.environment);
				return this.typeParametersWalker.toTypeParameter(isClassTypeParameter, rank);
			}
			return this;
		}
		@Override
		public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) {
			if (this.typeParametersWalker != null)
				return this.typeParametersWalker.toTypeParameterBounds(isClassTypeParameter, parameterRank);
			return this;
		}
		@Override
		public ITypeAnnotationWalker toSupertype(short index, char[] superTypeSignature) {
			Map<String, String> sources = ExternalAnnotationProvider.this.supertypeAnnotationSources;
			if (sources != null) {
				String source = sources.get(String.valueOf(superTypeSignature));
				if (source != null)
					return new SuperTypesAnnotationWalker(source.toCharArray(), this.environment);
			}
			return this;
		}
		// the rest is borrowed from EMPTY_ANNOTATION_WALKER:
		@Override
		public ITypeAnnotationWalker toField() { return this; }
		@Override
		public ITypeAnnotationWalker toThrows(int rank) { return this; }
		@Override
		public ITypeAnnotationWalker toTypeArgument(int rank) { return this; }
		@Override
		public ITypeAnnotationWalker toMethodParameter(short index) { return this; }
		@Override
		public ITypeAnnotationWalker toTypeBound(short boundIndex) { return this; }
		@Override
		public ITypeAnnotationWalker toMethodReturn() { return this; }
		@Override
		public ITypeAnnotationWalker toReceiver() { return this; }
		@Override
		public ITypeAnnotationWalker toWildcardBound() { return this; }
		@Override
		public ITypeAnnotationWalker toNextArrayDimension() { return this; }
		@Override
		public ITypeAnnotationWalker toNextNestedType() { return this; }
		@Override
		public IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness) { return NO_ANNOTATIONS; }
	}

	abstract class BasicAnnotationWalker implements ITypeAnnotationWalker {

		char[] source;
		SignatureWrapper wrapper;
		int pos;
		int prevTypeArgStart;
		int currentTypeBound;
		LookupEnvironment environment;

		BasicAnnotationWalker(char[] source, int pos, LookupEnvironment environment) {
			this.source = source;
			this.pos = pos;
			this.environment = environment;
			initAnnotations(environment);
		}

		SignatureWrapper wrapperWithStart(int start) {
			if (this.wrapper == null)
				this.wrapper = new SignatureWrapper(this.source);
			this.wrapper.start = start;
			this.wrapper.bracket = -1;
			return this.wrapper;
		}

		@Override
		public ITypeAnnotationWalker toReceiver() {
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) {
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) {
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toTypeBound(short boundIndex) {
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toSupertype(short index, char[] superTypeSignature) {
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toTypeArgument(int rank) {
			if (rank == 0) {
				int start = CharOperation.indexOf('<', this.source, this.pos) + 1;
				this.prevTypeArgStart = start;
				return new MethodAnnotationWalker(this.source, start, this.environment);
			}
			int next = this.prevTypeArgStart;
			switch (this.source[next]) {
				case '*':
					next = skipNullAnnotation(next+1);
					break;
				case '-':
				case '+':
					next = skipNullAnnotation(next+1);
					//$FALL-THROUGH$
				default:
					next = wrapperWithStart(next).computeEnd();
					next++;
			}
			this.prevTypeArgStart = next;
		    return new MethodAnnotationWalker(this.source, next,	this.environment);
		}

		@Override
		public ITypeAnnotationWalker toWildcardBound() {
			switch (this.source[this.pos]) {
				case '-':
				case '+':
					int newPos = skipNullAnnotation(this.pos+1);
					return new MethodAnnotationWalker(this.source, newPos, this.environment);
				default: // includes unbounded '*'
					return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
			}
		}

		@Override
		public ITypeAnnotationWalker toNextArrayDimension() {
			if (this.source[this.pos] == '[') {
				int newPos = skipNullAnnotation(this.pos+1);
				return new MethodAnnotationWalker(this.source, newPos, this.environment);
			}
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toNextNestedType() {
			return this; // FIXME(stephan)
		}

		@Override
		public IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness) {
			if (this.pos != -1 && this.pos < this.source.length-2) {
				switch (this.source[this.pos]) {
					case 'T':
					case 'L':
					case '[':
					case '*':
					case '+':
					case '-':
						switch (this.source[this.pos+1]) {
							case NULLABLE:
								return new IBinaryAnnotation[]{ ExternalAnnotationProvider.this.NULLABLE_ANNOTATION };
							case NONNULL:
								return new IBinaryAnnotation[]{ ExternalAnnotationProvider.this.NONNULL_ANNOTATION };
						}
				}
			}
			return NO_ANNOTATIONS;
		}
		int skipNullAnnotation(int cur) {
			if (cur >= this.source.length)
				return cur;
			switch (this.source[cur]) {
				case NONNULL:
				case NULLABLE:
					return cur+1;
				default:
					return cur;
			}
		}
	}

	/**
	 * Walker that may serve the annotations on type parameters of the current class or method.
	 */
	public class TypeParametersAnnotationWalker extends BasicAnnotationWalker {

		int[] rankStarts; // indices of start positions for type parameters per rank
		int currentRank;

		TypeParametersAnnotationWalker(char[] source, int pos, int rank, int[] rankStarts, LookupEnvironment environment) {
			super(source, pos, environment);
			this.currentRank = rank;
			if (rankStarts != null) {
				this.rankStarts = rankStarts;
			} else {
				// eagerly scan all type parameters:
				int length = source.length;
				rankStarts = new int[length];
				int curRank = 0;
				// next block cf. BinaryTypeBinding.createTypeVariables():
				int depth = 0;
				boolean pendingVariable = true;
				scanVariables: {
					for (int i = pos; i < length; i++) {
						switch(this.source[i]) {
							case Util.C_GENERIC_START :
								depth++;
								break;
							case Util.C_GENERIC_END :
								if (--depth < 0)
									break scanVariables;
								break;
							case Util.C_NAME_END :
								if ((depth == 0) && (i +1 < length) && (this.source[i+1] != Util.C_COLON))
									pendingVariable = true;
								break;
							case Util.C_COLON :
								if (depth == 0)
									pendingVariable = true; // end of variable name
								// skip optional bound ReferenceTypeSignature
								i++; // peek next
								while (i < length && this.source[i] == Util.C_ARRAY)
									i++;
								if (i < length && this.source[i] == Util.C_RESOLVED) {
									int currentdepth = depth;
									while (i < length && (currentdepth != depth || this.source[i] != Util.C_NAME_END)) {
										if(this.source[i] == Util.C_GENERIC_START)
											currentdepth++;
										if(this.source[i] == Util.C_GENERIC_END)
											currentdepth--;
										i++;
									}
								}
								i--; // unget
								break;
							default:
								if (pendingVariable) {
									pendingVariable = false;
									rankStarts[curRank++] = i;
								}
						}
					}
				}
				System.arraycopy(rankStarts, 0, this.rankStarts = new int[curRank], 0, curRank);
			}
		}

		@Override
		public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) {
			if (rank == this.currentRank)
				return this;
			if (rank < this.rankStarts.length)
				return new TypeParametersAnnotationWalker(this.source, this.rankStarts[rank], rank, this.rankStarts, this.environment);
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) {
			return new TypeParametersAnnotationWalker(this.source, this.rankStarts[parameterRank], parameterRank, this.rankStarts, this.environment);
		}

		@Override
		public ITypeAnnotationWalker toTypeBound(short boundIndex) {
			// assume we are positioned either at the start of the bounded type parameter
			// or at the start of a previous type bound
			int p = this.pos;
			int i = this.currentTypeBound;
			while(true) {
				// each bound is prefixed with ':'
				int colon = CharOperation.indexOf(Util.C_COLON, this.source, p);
				if (colon != -1)
					p = colon + 1;
				if (++i > boundIndex) break;
				// skip next type:
				p = wrapperWithStart(p).computeEnd()+1;
			}
			this.pos = p;
			this.currentTypeBound = boundIndex;
			return this;
		}

		@Override
		public ITypeAnnotationWalker toField() {
			throw new UnsupportedOperationException("Cannot navigate to fields"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toMethodReturn() {
			throw new UnsupportedOperationException("Cannot navigate to method return"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toMethodParameter(short index) {
			throw new UnsupportedOperationException("Cannot navigate to method parameter"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toThrows(int index) {
			throw new UnsupportedOperationException("Cannot navigate to throws"); //$NON-NLS-1$
		}

		@Override
		public IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness) {
			if (this.pos != -1 && this.pos < this.source.length-1) {
				switch (this.source[this.pos]) {
					case NULLABLE:
						return new IBinaryAnnotation[]{ ExternalAnnotationProvider.this.NULLABLE_ANNOTATION };
					case NONNULL:
						return new IBinaryAnnotation[]{ ExternalAnnotationProvider.this.NONNULL_ANNOTATION };
				}
			}
			return super.getAnnotationsAtCursor(currentTypeId, mayApplyArrayContentsDefaultNullness);
		}
	}

	/** Walker serving type annotations on a type's supertypes. */
	class SuperTypesAnnotationWalker extends BasicAnnotationWalker {

		SuperTypesAnnotationWalker(char[] source, LookupEnvironment environment) {
			super(source, 0, environment);
		}

		// actual implementation is inherited, main entries: toTypeArgument & getAnnotationsAtCursor

		@Override
		public ITypeAnnotationWalker toField() {
			throw new UnsupportedOperationException("Supertype has no field annotations"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toMethodReturn() {
			throw new UnsupportedOperationException("Supertype has no method return"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toMethodParameter(short index) {
			throw new UnsupportedOperationException("Supertype has no method parameter"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toThrows(int index) {
			throw new UnsupportedOperationException("Supertype has no throws"); //$NON-NLS-1$
		}
	}

	public interface IMethodAnnotationWalker extends ITypeAnnotationWalker {
		int getParameterCount();
	}
	class MethodAnnotationWalker extends BasicAnnotationWalker implements IMethodAnnotationWalker {

		int prevParamStart;
		TypeParametersAnnotationWalker typeParametersWalker;

		MethodAnnotationWalker(char[] source, int pos, LookupEnvironment environment) {
			super(source, pos, environment);
		}

		int typeEnd(int start) {
			while (this.source[start] == '[') {
				start++;
				start = skipNullAnnotation(start);
			}
			SignatureWrapper wrapper1 = wrapperWithStart(start);
			int end = wrapper1.skipAngleContents(wrapper1.computeEnd());
			return end;
		}

		@Override
		public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) {
			if (this.source[0] == '<') {
				if (this.typeParametersWalker == null)
					return this.typeParametersWalker = new TypeParametersAnnotationWalker(this.source, this.pos+1, rank, null, this.environment);
				return this.typeParametersWalker.toTypeParameter(isClassTypeParameter, rank);
			}
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) {
			if (this.typeParametersWalker != null)
				return this.typeParametersWalker.toTypeParameterBounds(isClassTypeParameter, parameterRank);
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toMethodReturn() {
			int close = CharOperation.indexOf(')', this.source);
			if (close != -1) {
				// optimization, see toMethodParameter.
				this.pos = close+1;
				return this;
			}
			return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
		}

		@Override
		public ITypeAnnotationWalker toMethodParameter(short index) {
			if (index == 0) {
				int start = CharOperation.indexOf('(', this.source) + 1;
				this.prevParamStart = start;
				// optimization: normally we should create a new walker with pos=start,
				// but since we know the order how BTB/LE call us, we can safely use one walker for all parameters:
				this.pos = start;
				return this;
			}
			int end = typeEnd(this.prevParamStart); // leverage the fact that all parameters are evaluated in order
			end++;
		    this.prevParamStart = end;
		    // optimization, see above.
		    this.pos = end;
		    return this;
		}

		@Override
		public ITypeAnnotationWalker toThrows(int index) {
			return this;
		}

		@Override
		public ITypeAnnotationWalker toField() {
			throw new UnsupportedOperationException("Methods have no fields"); //$NON-NLS-1$
		}

		@Override
		public int getParameterCount() {
			int count = 0;
			int start = CharOperation.indexOf('(', this.source) + 1;
			while (start < this.source.length && this.source[start] != ')') {
				start = typeEnd(start) + 1;
				count++;
			}
			return count;
		}
	}

	class FieldAnnotationWalker extends BasicAnnotationWalker {
		public FieldAnnotationWalker(char[] source, int pos, LookupEnvironment environment) {
			super(source, pos, environment);
		}

		@Override
		public ITypeAnnotationWalker toField() {
			return this;
		}

		@Override
		public ITypeAnnotationWalker toMethodReturn() {
			throw new UnsupportedOperationException("Field has no method return"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toMethodParameter(short index) {
			throw new UnsupportedOperationException("Field has no method parameter"); //$NON-NLS-1$
		}

		@Override
		public ITypeAnnotationWalker toThrows(int index) {
			throw new UnsupportedOperationException("Field has no throws"); //$NON-NLS-1$
		}
	}
}
