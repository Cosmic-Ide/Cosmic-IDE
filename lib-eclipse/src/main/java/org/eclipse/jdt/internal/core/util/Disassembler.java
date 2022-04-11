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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *       Jesper Steen Moeller - Contributions for:
 *                          Bug 406973 - [compiler] Parse MethodParameters attribute
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.*;
import org.eclipse.jdt.internal.compiler.codegen.AttributeNamesConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;

/**
 * Disassembler of .class files. It generates an output in the Writer that looks close to
 * the javap output.
 */
public class Disassembler extends ClassFileBytesDisassembler {

	private static final char[] ANY_EXCEPTION = Messages.classfileformat_anyexceptionhandler.toCharArray();
	private static final String VERSION_UNKNOWN = Messages.classfileformat_versionUnknown;

	private boolean appendModifier(StringBuffer buffer, int accessFlags, int modifierConstant, String modifier, boolean firstModifier) {
		if ((accessFlags & modifierConstant) != 0) {
			if (!firstModifier) {
				buffer.append(Messages.disassembler_space);
			}
			if (firstModifier) {
				firstModifier = false;
			}
			buffer.append(modifier);
		}
		return firstModifier;
	}

	private void decodeModifiers(StringBuffer buffer, int accessFlags, int[] checkBits) {
		decodeModifiers(buffer, accessFlags, false, false, checkBits);
	}

	private void decodeModifiers(StringBuffer buffer, int accessFlags, boolean printDefault, boolean asBridge, int[] checkBits) {
		if (checkBits == null) return;
		boolean firstModifier = true;
		for (int i = 0, max = checkBits.length; i < max; i++) {
			switch(checkBits[i]) {
				case IModifierConstants.ACC_PUBLIC :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_PUBLIC, "public", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_PROTECTED :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_PROTECTED, "protected", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_PRIVATE :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_PRIVATE, "private", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_ABSTRACT :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_ABSTRACT, "abstract", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_STATIC :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_STATIC, "static", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_FINAL :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_FINAL, "final", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_SYNCHRONIZED :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_SYNCHRONIZED, "synchronized", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_NATIVE :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_NATIVE, "native", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_STRICT :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_STRICT, "strictfp", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_TRANSIENT :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_TRANSIENT, "transient", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_VOLATILE :
				// case IModifierConstants.ACC_BRIDGE :
					if (asBridge) {
						firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_BRIDGE, "bridge", firstModifier); //$NON-NLS-1$
					} else {
						firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_VOLATILE, "volatile", firstModifier); //$NON-NLS-1$
					}
					break;
				case IModifierConstants.ACC_ENUM :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_ENUM, "enum", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_SYNTHETIC :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_SYNTHETIC, "synthetic", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_MANDATED :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_MANDATED, "mandated", firstModifier); //$NON-NLS-1$
					break;
			}
		}
		if (!firstModifier) {
			if (!printDefault) buffer.append(Messages.disassembler_space);
		} else if (printDefault) {
			// no modifier: package default visibility
			buffer.append("default"); //$NON-NLS-1$
		}
	}

	private void decodeModifiersForField(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, new int[] {
				IModifierConstants.ACC_PUBLIC,
				IModifierConstants.ACC_PROTECTED,
				IModifierConstants.ACC_PRIVATE,
				IModifierConstants.ACC_STATIC,
				IModifierConstants.ACC_FINAL,
				IModifierConstants.ACC_TRANSIENT,
				IModifierConstants.ACC_VOLATILE,
				IModifierConstants.ACC_ENUM
		});
	}

	private void decodeModifiersForFieldForWorkingCopy(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, new int[] {
				IModifierConstants.ACC_PUBLIC,
				IModifierConstants.ACC_PROTECTED,
				IModifierConstants.ACC_PRIVATE,
				IModifierConstants.ACC_STATIC,
				IModifierConstants.ACC_FINAL,
				IModifierConstants.ACC_TRANSIENT,
				IModifierConstants.ACC_VOLATILE,
		});
	}

	private final void decodeModifiersForInnerClasses(StringBuffer buffer, int accessFlags, boolean printDefault) {
		decodeModifiers(buffer, accessFlags, printDefault, false, new int[] {
				IModifierConstants.ACC_PUBLIC,
				IModifierConstants.ACC_PROTECTED,
				IModifierConstants.ACC_PRIVATE,
				IModifierConstants.ACC_ABSTRACT,
				IModifierConstants.ACC_STATIC,
				IModifierConstants.ACC_FINAL,
		});
	}

	private final void decodeModifiersForMethod(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, false, true, new int[] {
				IModifierConstants.ACC_PUBLIC,
				IModifierConstants.ACC_PROTECTED,
				IModifierConstants.ACC_PRIVATE,
				IModifierConstants.ACC_ABSTRACT,
				IModifierConstants.ACC_STATIC,
				IModifierConstants.ACC_FINAL,
				IModifierConstants.ACC_SYNCHRONIZED,
				IModifierConstants.ACC_NATIVE,
				IModifierConstants.ACC_STRICT,
				IModifierConstants.ACC_BRIDGE,
		});
	}

	private final void decodeModifiersForMethodParameters(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, false, true, new int[] {
				IModifierConstants.ACC_FINAL,
				IModifierConstants.ACC_MANDATED,
				IModifierConstants.ACC_SYNTHETIC,
		});
	}

	private final void decodeModifiersForType(StringBuffer buffer, int accessFlags) {
		decodeModifiers(buffer, accessFlags, new int[] {
				IModifierConstants.ACC_PUBLIC,
				IModifierConstants.ACC_ABSTRACT,
				IModifierConstants.ACC_FINAL,
		});
	}
	private final void decodeModifiersForModuleRequires(StringBuffer buffer, int accessFlags) {
		int[] checkBits = new int[] {
				IModifierConstants.ACC_TRANSITIVE,
				IModifierConstants.ACC_STATIC_PHASE,
		};
		boolean firstModifier = true;
		for (int i = 0, max = checkBits.length; i < max; i++) {
			switch(checkBits[i]) {
				case IModifierConstants.ACC_TRANSITIVE :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_TRANSITIVE, "transitive", firstModifier); //$NON-NLS-1$
					break;
				case IModifierConstants.ACC_STATIC_PHASE :
					firstModifier = appendModifier(buffer, accessFlags, IModifierConstants.ACC_STATIC_PHASE, "protected", firstModifier); //$NON-NLS-1$
					break;
			}
		}
		if (!firstModifier) {
			buffer.append(Messages.disassembler_space);
		}
	}
	private final void decodeModifiersForModule(StringBuffer buffer, int accessFlags) {
		appendModifier(buffer, accessFlags, IModifierConstants.ACC_OPEN, "open", true); //$NON-NLS-1$
		buffer.append(Messages.disassembler_space);
	}
	public static String escapeString(String s) {
		return decodeStringValue(s);
	}

	static String decodeStringValue(char[] chars) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = chars.length; i < max; i++) {
			char c = chars[i];
			org.eclipse.jdt.internal.compiler.util.Util.appendEscapedChar(buffer, c, true);
		}
		return buffer.toString();
	}

	private static void escapeChar(StringBuffer buffer, char c) {
		org.eclipse.jdt.internal.compiler.util.Util.appendEscapedChar(buffer, c, false);
	}

	static String decodeStringValue(String s) {
		return decodeStringValue(s.toCharArray());
	}

	/**
	 * @see org.eclipse.jdt.core.util.ClassFileBytesDisassembler#disassemble(byte[], java.lang.String)
	 */
	@Override
	public String disassemble(byte[] classFileBytes, String lineSeparator) throws ClassFormatException {
		try {
			return disassemble(new ClassFileReader(classFileBytes, IClassFileReader.ALL), lineSeparator, ClassFileBytesDisassembler.DEFAULT);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ClassFormatException(e.getMessage(), e);
		}
	}

	/**
	 * @see org.eclipse.jdt.core.util.ClassFileBytesDisassembler#disassemble(byte[], java.lang.String, int)
	 */
	@Override
	public String disassemble(byte[] classFileBytes, String lineSeparator, int mode) throws ClassFormatException {
		try {
			return disassemble(new ClassFileReader(classFileBytes, IClassFileReader.ALL), lineSeparator, mode);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ClassFormatException(e.getMessage(), e);
		}
	}

	private void disassemble(IAnnotation annotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		final int typeIndex = annotation.getTypeIndex();
		final char[] typeName = CharOperation.replaceOnCopy(annotation.getTypeName(), '/', '.');
		buffer.append(
			Messages.bind(Messages.disassembler_annotationentrystart, new String[] {
				Integer.toString(typeIndex),
				new String(returnClassName(Signature.toCharArray(typeName), '.', mode))
			}));
		final IAnnotationComponent[] components = annotation.getComponents();
		for (int i = 0, max = components.length; i < max; i++) {
			disassemble(components[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_annotationentryend);
	}

	private void disassemble(IExtendedAnnotation extendedAnnotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		final int typeIndex = extendedAnnotation.getTypeIndex();
		final char[] typeName = CharOperation.replaceOnCopy(extendedAnnotation.getTypeName(), '/', '.');
		buffer.append(
			Messages.bind(Messages.disassembler_extendedannotationentrystart, new String[] {
				Integer.toString(typeIndex),
				new String(returnClassName(Signature.toCharArray(typeName), '.', mode))
			}));
		final IAnnotationComponent[] components = extendedAnnotation.getComponents();
		for (int i = 0, max = components.length; i < max; i++) {
			disassemble(components[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
		writeNewLine(buffer, lineSeparator, tabNumber + 2);
		int targetType = extendedAnnotation.getTargetType();
		buffer.append(
				Messages.bind(Messages.disassembler_extendedannotation_targetType, new String[] {
					Integer.toHexString(targetType),
					getTargetType(targetType),
				}));
		switch(targetType) {
			case IExtendedAnnotationConstants.METHOD_RECEIVER :
			case IExtendedAnnotationConstants.METHOD_RETURN:
			case IExtendedAnnotationConstants.FIELD :
				break;
			default:
				writeNewLine(buffer, lineSeparator, tabNumber + 2);
				disassembleTargetTypeContents(false, targetType, extendedAnnotation, buffer, lineSeparator, tabNumber, mode);
		}
		disassembleTypePathContents(targetType, extendedAnnotation, buffer, lineSeparator, tabNumber, mode);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_extendedannotationentryend);
	}

	private void disassembleTypePathContents(int targetType, IExtendedAnnotation extendedAnnotation,StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		int[][] typepath = extendedAnnotation.getTypePath();
		if (typepath.length != 0) {
			writeNewLine(buffer, lineSeparator, tabNumber + 2);
			buffer.append(
				Messages.bind(Messages.disassembler_extendedannotation_typepath, new String[] {
						toTypePathString(typepath),
				}));
		}
	}
	private void disassembleTargetTypeContents(boolean insideWildcard, int targetType, IExtendedAnnotation extendedAnnotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		switch(targetType) {
			case IExtendedAnnotationConstants.CLASS_TYPE_PARAMETER :
			case IExtendedAnnotationConstants.METHOD_TYPE_PARAMETER :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_type_parameter, new String[] {
							Integer.toString(extendedAnnotation.getTypeParameterIndex()),
						}));
				break;
			case IExtendedAnnotationConstants.CLASS_EXTENDS :
				buffer.append(
					Messages.bind(Messages.disassembler_extendedannotation_classextendsimplements, new String[] {
						Integer.toString(extendedAnnotation.getAnnotationTypeIndex()),
					}));
				break;

			case IExtendedAnnotationConstants.CLASS_TYPE_PARAMETER_BOUND :
			case IExtendedAnnotationConstants.METHOD_TYPE_PARAMETER_BOUND :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_type_parameter_with_bound, new String[] {
							Integer.toString(extendedAnnotation.getTypeParameterIndex()),
							Integer.toString(extendedAnnotation.getTypeParameterBoundIndex()),
						}));
				break;
			case IExtendedAnnotationConstants.FIELD :
			case IExtendedAnnotationConstants.METHOD_RETURN :
			case IExtendedAnnotationConstants.METHOD_RECEIVER :
				break;
			case IExtendedAnnotationConstants.METHOD_FORMAL_PARAMETER :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_method_parameter, new String[] {
							Integer.toString(extendedAnnotation.getParameterIndex()),
						}));
				break;
			case IExtendedAnnotationConstants.THROWS :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_throws, new String[] {
							Integer.toString(extendedAnnotation.getAnnotationTypeIndex()),
						}));
				break;

			case IExtendedAnnotationConstants.LOCAL_VARIABLE :
			case IExtendedAnnotationConstants.RESOURCE_VARIABLE :
				buffer.append(Messages.disassembler_localvariabletargetheader);
				writeNewLine(buffer, lineSeparator, tabNumber + 3);
				int localVariableTableSize = extendedAnnotation.getLocalVariableRefenceInfoLength();
				ILocalVariableReferenceInfo[] localVariableTable = extendedAnnotation.getLocalVariableTable();
				for (int i = 0; i < localVariableTableSize; i++) {
					if (i != 0) {
						writeNewLine(buffer, lineSeparator, tabNumber + 3);
					}
					ILocalVariableReferenceInfo info = localVariableTable[i];
					int index= info.getIndex();
					int startPC = info.getStartPC();
					int length  = info.getLength();
					buffer.append(Messages.bind(Messages.classfileformat_localvariablereferenceinfoentry,
						new String[] {
							Integer.toString(startPC),
							Integer.toString(startPC + length),
							Integer.toString(index),
						}));
				}
				break;
			case IExtendedAnnotationConstants.EXCEPTION_PARAMETER :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_exception_table_index, new String[] {
							Integer.toString(extendedAnnotation.getExceptionTableIndex()),
						}));
				break;

			case IExtendedAnnotationConstants.INSTANCEOF :
			case IExtendedAnnotationConstants.NEW :
			case IExtendedAnnotationConstants.CONSTRUCTOR_REFERENCE :
			case IExtendedAnnotationConstants.METHOD_REFERENCE :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_offset, new String[] {
							Integer.toString(extendedAnnotation.getOffset()),
						}));
				break;
			case IExtendedAnnotationConstants.CAST :
			case IExtendedAnnotationConstants.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT :
			case IExtendedAnnotationConstants.METHOD_INVOCATION_TYPE_ARGUMENT :
			case IExtendedAnnotationConstants.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT :
			case IExtendedAnnotationConstants.METHOD_REFERENCE_TYPE_ARGUMENT :
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_offset, new String[] {
							Integer.toString(extendedAnnotation.getOffset()),
						}));
				writeNewLine(buffer, lineSeparator, tabNumber + 2);
				buffer.append(
						Messages.bind(Messages.disassembler_extendedannotation_type_argument, new String[] {
							Integer.toString(extendedAnnotation.getAnnotationTypeIndex()),
						}));
				break;
		}
	}
	private String getTargetType(int targetType) {
		switch(targetType) {
			case IExtendedAnnotationConstants.CLASS_TYPE_PARAMETER :
				return "CLASS_TYPE_PARAMETER"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_TYPE_PARAMETER :
				return "METHOD_TYPE_PARAMETER"; //$NON-NLS-1$

			case IExtendedAnnotationConstants.CLASS_EXTENDS :
				return "CLASS_EXTENDS"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.CLASS_TYPE_PARAMETER_BOUND :
				return "CLASS_TYPE_PARAMETER_BOUND"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_TYPE_PARAMETER_BOUND :
				return "METHOD_TYPE_PARAMETER_BOUND"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.FIELD :
				return "FIELD"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_RETURN :
				return "METHOD_RETURN"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_RECEIVER :
				return "METHOD_RECEIVER"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_FORMAL_PARAMETER :
				return "METHOD_FORMAL_PARAMETER"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.THROWS :
				return "THROWS"; //$NON-NLS-1$

			case IExtendedAnnotationConstants.LOCAL_VARIABLE :
				return "LOCAL_VARIABLE"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.RESOURCE_VARIABLE :
				return "RESOURCE_VARIABLE"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.EXCEPTION_PARAMETER :
				return "EXCEPTION_PARAMETER"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.INSTANCEOF :
				return "INSTANCEOF"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.NEW :
				return "NEW"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.CONSTRUCTOR_REFERENCE :
				return "CONSTRUCTOR_REFERENCE"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_REFERENCE :
				return "METHOD_REFERENCE"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.CAST :
				return "CAST"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT :
				return "CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_INVOCATION_TYPE_ARGUMENT :
				return "METHOD_INVOCATION_TYPE_ARGUMENT"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT :
				return "CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT"; //$NON-NLS-1$
			case IExtendedAnnotationConstants.METHOD_REFERENCE_TYPE_ARGUMENT :
				return "METHOD_REFERENCE_TYPE_ARGUMENT"; //$NON-NLS-1$
			default:
				return "UNKNOWN"; //$NON-NLS-1$
		}
	}


	private void disassemble(IAnnotationComponent annotationComponent, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(
			Messages.bind(Messages.disassembler_annotationcomponent,
				new String[] {
					Integer.toString(annotationComponent.getComponentNameIndex()),
					new String(annotationComponent.getComponentName())
				}));
		disassemble(annotationComponent.getComponentValue(), buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassemble(IAnnotationComponentValue annotationComponentValue, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		switch(annotationComponentValue.getTag()) {
			case IAnnotationComponentValue.BYTE_TAG:
			case IAnnotationComponentValue.CHAR_TAG:
			case IAnnotationComponentValue.DOUBLE_TAG:
			case IAnnotationComponentValue.FLOAT_TAG:
			case IAnnotationComponentValue.INTEGER_TAG:
			case IAnnotationComponentValue.LONG_TAG:
			case IAnnotationComponentValue.SHORT_TAG:
			case IAnnotationComponentValue.BOOLEAN_TAG:
			case IAnnotationComponentValue.STRING_TAG:
				IConstantPoolEntry constantPoolEntry = annotationComponentValue.getConstantValue();
				String value = null;
				switch(constantPoolEntry.getKind()) {
					case IConstantPoolConstant.CONSTANT_Long :
						value = constantPoolEntry.getLongValue() + "L"; //$NON-NLS-1$
						break;
					case IConstantPoolConstant.CONSTANT_Float :
						value = constantPoolEntry.getFloatValue() + "f"; //$NON-NLS-1$
						break;
					case IConstantPoolConstant.CONSTANT_Double :
						value = Double.toString(constantPoolEntry.getDoubleValue());
						break;
					case IConstantPoolConstant.CONSTANT_Integer:
						StringBuffer temp = new StringBuffer();
						switch(annotationComponentValue.getTag()) {
							case IAnnotationComponentValue.CHAR_TAG :
								temp.append('\'');
								escapeChar(temp, (char) constantPoolEntry.getIntegerValue());
								temp.append('\'');
								break;
							case IAnnotationComponentValue.BOOLEAN_TAG :
								temp.append(constantPoolEntry.getIntegerValue() == 1 ? "true" : "false");//$NON-NLS-1$//$NON-NLS-2$
								break;
							case IAnnotationComponentValue.BYTE_TAG :
								temp.append("(byte) ").append(constantPoolEntry.getIntegerValue()); //$NON-NLS-1$
								break;
							case IAnnotationComponentValue.SHORT_TAG :
								temp.append("(short) ").append(constantPoolEntry.getIntegerValue()); //$NON-NLS-1$
								break;
							case IAnnotationComponentValue.INTEGER_TAG :
								temp.append("(int) ").append(constantPoolEntry.getIntegerValue()); //$NON-NLS-1$
						}
						value = String.valueOf(temp);
						break;
					case IConstantPoolConstant.CONSTANT_Utf8:
						value = "\"" + decodeStringValue(constantPoolEntry.getUtf8Value()) + "\"";//$NON-NLS-1$//$NON-NLS-2$
				}
				buffer.append(Messages.bind(Messages.disassembler_annotationdefaultvalue, value));
				break;
			case IAnnotationComponentValue.ENUM_TAG:
				final int enumConstantTypeNameIndex = annotationComponentValue.getEnumConstantTypeNameIndex();
				final char[] typeName = CharOperation.replaceOnCopy(annotationComponentValue.getEnumConstantTypeName(), '/', '.');
				final int enumConstantNameIndex = annotationComponentValue.getEnumConstantNameIndex();
				final char[] constantName = annotationComponentValue.getEnumConstantName();
				buffer.append(Messages.bind(Messages.disassembler_annotationenumvalue,
					new String[] {
						Integer.toString(enumConstantTypeNameIndex),
						Integer.toString(enumConstantNameIndex),
						new String(returnClassName(Signature.toCharArray(typeName), '.', mode)),
						new String(constantName)
					}));
				break;
			case IAnnotationComponentValue.CLASS_TAG:
				final int classIndex = annotationComponentValue.getClassInfoIndex();
				constantPoolEntry = annotationComponentValue.getClassInfo();
				final char[] className = CharOperation.replaceOnCopy(constantPoolEntry.getUtf8Value(), '/', '.');
				buffer.append(Messages.bind(Messages.disassembler_annotationclassvalue,
					new String[] {
						Integer.toString(classIndex),
						new String(returnClassName(Signature.toCharArray(className), '.', mode))
					}));
				break;
			case IAnnotationComponentValue.ANNOTATION_TAG:
				buffer.append(Messages.disassembler_annotationannotationvalue);
				IAnnotation annotation = annotationComponentValue.getAnnotationValue();
				disassemble(annotation, buffer, lineSeparator, tabNumber + 1, mode);
				break;
			case IAnnotationComponentValue.ARRAY_TAG:
				buffer.append(Messages.disassembler_annotationarrayvaluestart);
				final IAnnotationComponentValue[] annotationComponentValues = annotationComponentValue.getAnnotationComponentValues();
				for (int i = 0, max = annotationComponentValues.length; i < max; i++) {
					writeNewLine(buffer, lineSeparator, tabNumber + 1);
					disassemble(annotationComponentValues[i], buffer, lineSeparator, tabNumber + 1, mode);
				}
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
				buffer.append(Messages.disassembler_annotationarrayvalueend);
		}
	}

	private void disassemble(IAnnotationDefaultAttribute annotationDefaultAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_annotationdefaultheader);
		IAnnotationComponentValue componentValue = annotationDefaultAttribute.getMemberValue();
		writeNewLine(buffer, lineSeparator, tabNumber + 2);
		disassemble(componentValue, buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassemble(IClassFileAttribute classFileAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.bind(Messages.disassembler_genericattributeheader,
			new String[] {
				new String(classFileAttribute.getAttributeName()),
				Long.toString(classFileAttribute.getAttributeLength())
			}));
	}

	private void disassemble(IMethodParametersAttribute methodParametersAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		tabNumber += 2;
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_methodparametersheader);
		for (int i = 0, length = methodParametersAttribute.getMethodParameterLength(); i < length; ++i) {
			writeNewLine(buffer, lineSeparator, tabNumber + 1);
			short accessFlags = methodParametersAttribute.getAccessFlags(i);
			decodeModifiersForMethodParameters(buffer, accessFlags);
			char [] parameterName = methodParametersAttribute.getParameterName(i);
			if (parameterName == null)
				parameterName = Messages.disassembler_anonymousparametername.toCharArray();
			buffer.append(parameterName);
		}
	}

	private void disassembleEnumConstructor(IClassFileReader classFileReader, char[] className, IMethodInfo methodInfo, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final ICodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		IMethodParametersAttribute methodParametersAttribute = (IMethodParametersAttribute) Util.getAttribute(methodInfo, IAttributeNamesConstants.METHOD_PARAMETERS);
		char[] methodDescriptor = methodInfo.getDescriptor();
		final IClassFileAttribute runtimeVisibleAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		// disassemble compact version of annotations
		if (runtimeInvisibleAnnotationsAttribute != null) {
			disassembleAsModifier((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			writeNewLine(buffer, lineSeparator, tabNumber);
		}
		if (runtimeVisibleAnnotationsAttribute != null) {
			disassembleAsModifier((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			writeNewLine(buffer, lineSeparator, tabNumber);
		}
		final int accessFlags = methodInfo.getAccessFlags();
		decodeModifiersForMethod(buffer, accessFlags & IModifierConstants.ACC_PRIVATE);
		CharOperation.replace(methodDescriptor, '/', '.');
		final boolean isVarArgs = (accessFlags & IModifierConstants.ACC_VARARGS) != 0;
		final char[] signature = Signature.toCharArray(methodDescriptor, returnClassName(className, '.', COMPACT), getParameterNames(methodDescriptor, codeAttribute, methodParametersAttribute, accessFlags) , !checkMode(mode, COMPACT), false, isVarArgs);
		int index = CharOperation.indexOf(',', signature);
		index = CharOperation.indexOf(',', signature, index + 1);
		buffer.append(signature, 0, CharOperation.indexOf('(', signature) + 1);
		buffer.append(signature, index + 2, signature.length - index - 2);
		IExceptionAttribute exceptionAttribute = methodInfo.getExceptionAttribute();
		if (exceptionAttribute != null) {
			buffer.append(" throws "); //$NON-NLS-1$
			char[][] exceptionNames = exceptionAttribute.getExceptionNames();
			int length = exceptionNames.length;
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					buffer
    					.append(Messages.disassembler_comma)
    					.append(Messages.disassembler_space);
				}
				char[] exceptionName = exceptionNames[i];
				CharOperation.replace(exceptionName, '/', '.');
				buffer.append(returnClassName(exceptionName, '.', mode));
			}
		}
		if (((accessFlags & IModifierConstants.ACC_NATIVE) == 0)
				&& ((accessFlags & IModifierConstants.ACC_ABSTRACT) == 0)) {
			buffer.append(" {"); //$NON-NLS-1$
			final char[] returnType = Signature.getReturnType(methodDescriptor);
			if (returnType.length == 1) {
				switch(returnType[0]) {
					case 'V' :
						writeNewLine(buffer, lineSeparator, tabNumber);
						break;
					case 'I' :
					case 'B' :
					case 'J' :
					case 'D' :
					case 'F' :
					case 'S' :
					case 'C' :
						writeNewLine(buffer, lineSeparator, tabNumber + 1);
						buffer.append("return 0;"); //$NON-NLS-1$
						writeNewLine(buffer, lineSeparator, tabNumber);
						break;
					default :
						// boolean
						writeNewLine(buffer, lineSeparator, tabNumber + 1);
						buffer.append("return false;"); //$NON-NLS-1$
						writeNewLine(buffer, lineSeparator, tabNumber);
				}
			} else {
				// object
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
				buffer.append("return null;"); //$NON-NLS-1$
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			buffer.append('}');
		} else {
			buffer.append(';');
		}
	}

	/**
	 * Disassemble a method info header
	 */
	private void disassemble(IClassFileReader classFileReader, char[] className, IMethodInfo methodInfo, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final ICodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		final char[] methodDescriptor = methodInfo.getDescriptor();
		final ISignatureAttribute signatureAttribute = (ISignatureAttribute) Util.getAttribute(methodInfo, IAttributeNamesConstants.SIGNATURE);
		final IClassFileAttribute runtimeVisibleAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeVisibleTypeAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleTypeAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
		final IClassFileAttribute runtimeVisibleParameterAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleParameterAnnotationsAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS);
		final IClassFileAttribute methodParametersAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.METHOD_PARAMETERS);
		final IClassFileAttribute annotationDefaultAttribute = Util.getAttribute(methodInfo, IAttributeNamesConstants.ANNOTATION_DEFAULT);
		if (checkMode(mode, SYSTEM | DETAILED)) {
			buffer.append(Messages.bind(Messages.classfileformat_methoddescriptor,
				new String[] {
					Integer.toString(methodInfo.getDescriptorIndex()),
					new String(methodDescriptor)
				}));
			if (methodInfo.isDeprecated()) {
				buffer.append(Messages.disassembler_deprecated);
			}
			writeNewLine(buffer, lineSeparator, tabNumber);
			if (signatureAttribute != null) {
				buffer.append(Messages.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (codeAttribute != null) {
				buffer.append(Messages.bind(Messages.classfileformat_stacksAndLocals,
					new String[] {
						Integer.toString(codeAttribute.getMaxStack()),
						Integer.toString(codeAttribute.getMaxLocals())
					}));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		if (checkMode(mode, DETAILED)) {
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		final int accessFlags = methodInfo.getAccessFlags();
		decodeModifiersForMethod(buffer, accessFlags);
		if (methodInfo.isSynthetic() && !checkMode(mode, WORKING_COPY)) {
			buffer.append("synthetic"); //$NON-NLS-1$
			buffer.append(Messages.disassembler_space);
		}
		CharOperation.replace(methodDescriptor, '/', '.');
		final boolean isVarArgs = isVarArgs(methodInfo);
		char[] methodHeader = null;
		char[][] parameterNames = null;
		if (!methodInfo.isClinit()) {
			parameterNames = getParameterNames(methodDescriptor, codeAttribute, (IMethodParametersAttribute)methodParametersAttribute, accessFlags);
		}
		if (methodInfo.isConstructor()) {
			if (checkMode(mode, WORKING_COPY) && signatureAttribute != null) {
				final char[] signature = signatureAttribute.getSignature();
				CharOperation.replace(signature, '/', '.');
				disassembleGenericSignature(mode, buffer, signature);
				buffer.append(' ');
				methodHeader = Signature.toCharArray(signature, returnClassName(className, '.', COMPACT), parameterNames, !checkMode(mode, COMPACT), false, isVarArgs);
			} else {
				methodHeader = Signature.toCharArray(methodDescriptor, returnClassName(className, '.', COMPACT), parameterNames, !checkMode(mode, COMPACT), false, isVarArgs);
			}
		} else if (methodInfo.isClinit()) {
			methodHeader = Messages.bind(Messages.classfileformat_clinitname).toCharArray();
		} else {
			if (checkMode(mode, WORKING_COPY) && signatureAttribute != null) {
				final char[] signature = signatureAttribute.getSignature();
				CharOperation.replace(signature, '/', '.');
				disassembleGenericSignature(mode, buffer, signature);
				buffer.append(' ');
				methodHeader = Signature.toCharArray(signature, methodInfo.getName(), parameterNames, !checkMode(mode, COMPACT), true, isVarArgs);
			} else {
				methodHeader = Signature.toCharArray(methodDescriptor, methodInfo.getName(), parameterNames, !checkMode(mode, COMPACT), true, isVarArgs);
			}
		}
		if (checkMode(mode, DETAILED) && (runtimeInvisibleParameterAnnotationsAttribute != null || runtimeVisibleParameterAnnotationsAttribute != null)) {
			IParameterAnnotation[] invisibleParameterAnnotations = null;
			IParameterAnnotation[] visibleParameterAnnotations = null;
			int length = -1;
			if (runtimeInvisibleParameterAnnotationsAttribute != null) {
				IRuntimeInvisibleParameterAnnotationsAttribute attribute = (IRuntimeInvisibleParameterAnnotationsAttribute) runtimeInvisibleParameterAnnotationsAttribute;
				invisibleParameterAnnotations = attribute.getParameterAnnotations();
				length = invisibleParameterAnnotations.length;
				if (length > 0) {
					int parameterNamesLength = parameterNames.length;
					if (length < parameterNamesLength) {
						int startExplicitParams = parameterNamesLength - length;
						System.arraycopy(invisibleParameterAnnotations, 0, (invisibleParameterAnnotations = new IParameterAnnotation[parameterNamesLength]), startExplicitParams, length);
						length = parameterNamesLength;
					}
				}
			}
			if (runtimeVisibleParameterAnnotationsAttribute != null) {
				IRuntimeVisibleParameterAnnotationsAttribute attribute = (IRuntimeVisibleParameterAnnotationsAttribute) runtimeVisibleParameterAnnotationsAttribute;
				visibleParameterAnnotations = attribute.getParameterAnnotations();
				length = visibleParameterAnnotations.length;
				if (length > 0) {
					int parameterNamesLength = parameterNames.length;
					if (length < parameterNamesLength) {
						int startExplicitParams = parameterNamesLength - length;
						System.arraycopy(visibleParameterAnnotations, 0, (visibleParameterAnnotations = new IParameterAnnotation[parameterNamesLength]), startExplicitParams, length);
						length = parameterNamesLength;
					}
				}
			}
			int insertionPosition = CharOperation.indexOf('(', methodHeader) + 1;
			int start = 0;
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(methodHeader, 0, insertionPosition);
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					stringBuffer.append(' ');
				}
				int stringBufferSize = stringBuffer.length();
				if (visibleParameterAnnotations != null) {
					disassembleAsModifier(visibleParameterAnnotations, stringBuffer, i, lineSeparator, tabNumber, mode);
				}
				if (invisibleParameterAnnotations != null) {
					if (stringBuffer.length() != stringBufferSize) {
						stringBuffer.append(' ');
						stringBufferSize = stringBuffer.length();
					}
					disassembleAsModifier(invisibleParameterAnnotations, stringBuffer, i, lineSeparator, tabNumber, mode);
				}
				if (i == 0 && stringBuffer.length() != stringBufferSize) {
					stringBuffer.append(' ');
				}
				start = insertionPosition;
				insertionPosition = CharOperation.indexOf(',', methodHeader, start + 1) + 1;
				if (insertionPosition == 0) {
					stringBuffer.append(methodHeader, start, methodHeader.length - start);
				} else {
					stringBuffer.append(methodHeader, start, insertionPosition - start);
				}
			}
			buffer.append(stringBuffer);
		} else {
			buffer.append(methodHeader);
		}
		IExceptionAttribute exceptionAttribute = methodInfo.getExceptionAttribute();
		if (exceptionAttribute != null) {
			buffer.append(" throws "); //$NON-NLS-1$
			char[][] exceptionNames = exceptionAttribute.getExceptionNames();
			int length = exceptionNames.length;
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					buffer
						.append(Messages.disassembler_comma)
						.append(Messages.disassembler_space);
				}
				char[] exceptionName = exceptionNames[i];
				CharOperation.replace(exceptionName, '/', '.');
				buffer.append(returnClassName(exceptionName, '.', mode));
			}
		}
		if (checkMode(mode, DETAILED)) {
			if (annotationDefaultAttribute != null) {
				buffer.append(" default "); //$NON-NLS-1$
				disassembleAsModifier((IAnnotationDefaultAttribute) annotationDefaultAttribute, buffer, lineSeparator, tabNumber, mode);
			}
		}
		if (checkMode(mode, WORKING_COPY)) {
			// put the annotation default attribute if needed
			if (annotationDefaultAttribute != null) {
				buffer.append(" default "); //$NON-NLS-1$
				disassembleAsModifier((IAnnotationDefaultAttribute) annotationDefaultAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (((accessFlags & IModifierConstants.ACC_NATIVE) == 0)
					&& ((accessFlags & IModifierConstants.ACC_ABSTRACT) == 0)) {
				buffer.append(" {"); //$NON-NLS-1$
				final char[] returnType = Signature.getReturnType(methodDescriptor);
				if (returnType.length == 1) {
					switch(returnType[0]) {
						case 'V' :
							writeNewLine(buffer, lineSeparator, tabNumber);
							break;
						case 'I' :
						case 'B' :
						case 'J' :
						case 'D' :
						case 'F' :
						case 'S' :
						case 'C' :
							writeNewLine(buffer, lineSeparator, tabNumber + 1);
							buffer.append("return 0;"); //$NON-NLS-1$
							writeNewLine(buffer, lineSeparator, tabNumber);
							break;
						default :
							// boolean
							writeNewLine(buffer, lineSeparator, tabNumber + 1);
							buffer.append("return false;"); //$NON-NLS-1$
							writeNewLine(buffer, lineSeparator, tabNumber);
					}
				} else {
					// object
					writeNewLine(buffer, lineSeparator, tabNumber + 1);
					buffer.append("return null;"); //$NON-NLS-1$
					writeNewLine(buffer, lineSeparator, tabNumber);
				}
				buffer.append('}');
			} else {
				buffer.append(';');
			}
		} else {
			buffer.append(Messages.disassembler_endofmethodheader);
		}

		if (checkMode(mode, SYSTEM | DETAILED)) {
			if (codeAttribute != null) {
				disassemble(codeAttribute, parameterNames, methodDescriptor, (accessFlags & IModifierConstants.ACC_STATIC) != 0, buffer, lineSeparator, tabNumber, mode);
			}
		}
		if (checkMode(mode, SYSTEM | DETAILED)) {
			if (methodParametersAttribute != null) {
				disassemble((IMethodParametersAttribute)methodParametersAttribute, buffer, lineSeparator, tabNumber, mode);
			}
		}
		if (checkMode(mode, SYSTEM)) {
			IClassFileAttribute[] attributes = methodInfo.getAttributes();
			int length = attributes.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					IClassFileAttribute attribute = attributes[i];
					if (attribute != codeAttribute
							&& attribute != exceptionAttribute
							&& attribute != signatureAttribute
							&& attribute != annotationDefaultAttribute
							&& attribute != runtimeInvisibleAnnotationsAttribute
							&& attribute != runtimeVisibleAnnotationsAttribute
							&& attribute != runtimeInvisibleTypeAnnotationsAttribute
							&& attribute != runtimeVisibleTypeAnnotationsAttribute
							&& attribute != runtimeInvisibleParameterAnnotationsAttribute
							&& attribute != runtimeVisibleParameterAnnotationsAttribute
							&& attribute != methodParametersAttribute
							&& !CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.DEPRECATED)
							&& !CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.SYNTHETIC)) {
						disassemble(attribute, buffer, lineSeparator, tabNumber, mode);
						writeNewLine(buffer, lineSeparator, tabNumber);
					}
				}
			}
			if (annotationDefaultAttribute != null) {
				disassemble((IAnnotationDefaultAttribute) annotationDefaultAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleParameterAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleParameterAnnotationsAttribute) runtimeVisibleParameterAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleParameterAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleParameterAnnotationsAttribute) runtimeInvisibleParameterAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleTypeAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleTypeAnnotationsAttribute) runtimeVisibleTypeAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleTypeAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleTypeAnnotationsAttribute) runtimeInvisibleTypeAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
 			}
		}
	}

	/**
	 * @see #disassemble(org.eclipse.jdt.core.util.IClassFileReader, java.lang.String, int)
	 */
	public String disassemble(IClassFileReader classFileReader, String lineSeparator) {
		return disassemble(classFileReader, lineSeparator, ClassFileBytesDisassembler.DEFAULT);
	}

	/**
	 * Answers back the disassembled string of the IClassFileReader according to the
	 * mode.
	 * This is an output quite similar to the javap tool.
	 *
	 * @param classFileReader The classFileReader to be disassembled
	 * @param lineSeparator the line separator to use.
	 * @param mode the mode used to disassemble the IClassFileReader
	 *
	 * @return the disassembled string of the IClassFileReader according to the mode
	 */
	public String disassemble(IClassFileReader classFileReader, String lineSeparator, int mode) {
		if (classFileReader == null) return org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING;
		char[] className = classFileReader.getClassName();
		if (className == null) {
			// incomplete initialization. We cannot go further.
			return org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING;
		}
		className= CharOperation.replaceOnCopy(className, '/', '.');
		final int classNameLength = className.length;
		final int accessFlags = classFileReader.getAccessFlags();
		final boolean isEnum = (accessFlags & IModifierConstants.ACC_ENUM) != 0;

		StringBuffer buffer = new StringBuffer();
		ISourceAttribute sourceAttribute = classFileReader.getSourceFileAttribute();
		IClassFileAttribute classFileAttribute = Util.getAttribute(classFileReader, IAttributeNamesConstants.SIGNATURE);
		ISignatureAttribute signatureAttribute = (ISignatureAttribute) classFileAttribute;
		if (checkMode(mode, SYSTEM | DETAILED)) {
			int minorVersion = classFileReader.getMinorVersion();
			int majorVersion = classFileReader.getMajorVersion();
			buffer.append(Messages.disassembler_begincommentline);
			if (sourceAttribute != null) {
				buffer.append(Messages.disassembler_sourceattributeheader);
				buffer.append(sourceAttribute.getSourceFileName());
			}
			String versionNumber = VERSION_UNKNOWN;
			if (minorVersion == 3 && majorVersion == 45) {
				versionNumber = JavaCore.VERSION_1_1;
			} else {
				versionNumber = CompilerOptions.versionFromJdkLevel((majorVersion << 16) + minorVersion);
				if (versionNumber.length() == 0)
					versionNumber = VERSION_UNKNOWN;
			}
			buffer.append(
				Messages.bind(Messages.classfileformat_versiondetails,
				new String[] {
					versionNumber,
					Integer.toString(majorVersion),
					Integer.toString(minorVersion),
					((accessFlags & IModifierConstants.ACC_SUPER) != 0
							? Messages.classfileformat_superflagisset
							: Messages.classfileformat_superflagisnotset)
					+ (isDeprecated(classFileReader) ? ", deprecated" : org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING)//$NON-NLS-1$
				}));
			writeNewLine(buffer, lineSeparator, 0);
			if (signatureAttribute != null) {
				buffer.append(Messages.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, 0);
			}
		}
		final int lastDotIndexInClassName = CharOperation.lastIndexOf('.', className);

		if (checkMode(mode, WORKING_COPY) && lastDotIndexInClassName != -1) {
			// we print a package declaration
			buffer.append("package ");//$NON-NLS-1$
			buffer.append(className, 0, lastDotIndexInClassName);
			buffer.append(';');
			writeNewLine(buffer, lineSeparator, 0);
		}

		INestMembersAttribute nestMembersAttribute = classFileReader.getNestMembersAttribute();
		IPermittedSubclassesAttribute permittedSubclassesAttribute = classFileReader.getPermittedSubclassesAttribute();
		IInnerClassesAttribute innerClassesAttribute = classFileReader.getInnerClassesAttribute();
		IClassFileAttribute runtimeVisibleAnnotationsAttribute = Util.getAttribute(classFileReader, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		IClassFileAttribute runtimeInvisibleAnnotationsAttribute = Util.getAttribute(classFileReader, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		IClassFileAttribute runtimeVisibleTypeAnnotationsAttribute = Util.getAttribute(classFileReader, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
		IClassFileAttribute runtimeInvisibleTypeAnnotationsAttribute = Util.getAttribute(classFileReader, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);

		IClassFileAttribute bootstrapMethods = Util.getAttribute(classFileReader, IAttributeNamesConstants.BOOTSTRAP_METHODS);
		IModuleAttribute moduleAttribute = (IModuleAttribute) Util.getAttribute(classFileReader, IAttributeNamesConstants.MODULE);
		IRecordAttribute recordAttribute = classFileReader.getRecordAttribute();

		if (checkMode(mode, DETAILED)) {
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				writeNewLine(buffer, lineSeparator, 0);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				writeNewLine(buffer, lineSeparator, 0);
			}
		}
		boolean decoded = false;
		if (isEnum && checkMode(mode, WORKING_COPY)) {
			decodeModifiersForType(buffer, accessFlags & IModifierConstants.ACC_PUBLIC);
		} else {
			if (innerClassesAttribute != null) {
				// search the right entry
				IInnerClassesAttributeEntry[] entries = innerClassesAttribute.getInnerClassAttributesEntries();
				for (int i = 0, max = entries.length; i < max ; i++) {
					IInnerClassesAttributeEntry entry = entries[i];
					char[] innerClassName = entry.getInnerClassName();
					if (innerClassName != null) {
						if (CharOperation.equals(classFileReader.getClassName(), innerClassName)) {
							decodeModifiersForInnerClasses(buffer, entry.getAccessFlags(), false);
							decoded = true;
						}
					}
				}
			}
			if (!decoded) {
				decodeModifiersForType(buffer, accessFlags);
				if (isSynthetic(classFileReader)) {
					buffer.append("synthetic"); //$NON-NLS-1$
					buffer.append(Messages.disassembler_space);
				}
			}
		}

		final boolean isAnnotation = (accessFlags & IModifierConstants.ACC_ANNOTATION) != 0;
		final boolean isModule = (accessFlags & IModifierConstants.ACC_MODULE) != 0;
		boolean isInterface = false;
		if (isEnum) {
			buffer.append("enum "); //$NON-NLS-1$
		} else if (isModule) {
			// skip - process under module attribute
		} else if (classFileReader.isClass()) {
			if (CharOperation.equals(classFileReader.getSuperclassName(), TypeConstants.CharArray_JAVA_LANG_RECORD_SLASH)) {
				buffer.append("record "); //$NON-NLS-1$
			}
			else {
				buffer.append("class "); //$NON-NLS-1$
			}
		} else {
			if (isAnnotation) {
				buffer.append("@"); //$NON-NLS-1$
			}
			buffer.append("interface "); //$NON-NLS-1$
			isInterface = true;
		}

		if (checkMode(mode, WORKING_COPY)) {
			// we print the simple class name
			final int start = lastDotIndexInClassName + 1;
			buffer.append(className, start, classNameLength - start);
			className = CharOperation.subarray(className, start, classNameLength);
			if (signatureAttribute != null) {
				disassembleGenericSignature(mode, buffer, signatureAttribute.getSignature());
			}
		} else if (!isModule) {
			buffer.append(className);
		}

		char[] superclassName = classFileReader.getSuperclassName();
		if (superclassName != null) {
			CharOperation.replace(superclassName, '/', '.');
			if (!isJavaLangObject(superclassName) && !isEnum) {
				buffer.append(" extends "); //$NON-NLS-1$
				buffer.append(returnClassName(superclassName, '.', mode));
			}
		}
		if (!isAnnotation || !checkMode(mode, WORKING_COPY)) {
			char[][] superclassInterfaces = classFileReader.getInterfaceNames();
			int length = superclassInterfaces.length;
			if (length != 0) {
				if (isInterface) {
					buffer.append(" extends "); //$NON-NLS-1$
				} else {
					buffer.append(" implements "); //$NON-NLS-1$
				}
				for (int i = 0; i < length; i++) {
					if (i != 0) {
						buffer
							.append(Messages.disassembler_comma)
							.append(Messages.disassembler_space);
					}
					char[] superinterface = superclassInterfaces[i];
					CharOperation.replace(superinterface, '/', '.');
					buffer
						.append(returnClassName(superinterface, '.', mode));
				}
			}
		}
		if (!isModule)
			buffer.append(Messages.bind(Messages.disassembler_opentypedeclaration));
		if (checkMode(mode, SYSTEM)) {
			disassemble(classFileReader.getConstantPool(), buffer, lineSeparator, 1);
		}
		if (isModule && moduleAttribute != null) { // print attributes - module package and main class only if the mandatory module attribute non-null
			decodeModifiersForModule(buffer, accessFlags);
			buffer.append("module"); //$NON-NLS-1$
			buffer.append(Messages.disassembler_space);
			buffer.append(moduleAttribute.getModuleName());
			buffer.append(Messages.disassembler_space);
			buffer.append(Messages.bind(Messages.disassembler_opentypedeclaration));
			disassembleModule(moduleAttribute, buffer, lineSeparator, 1);
			IModulePackagesAttribute modulePackagesAttribute = (IModulePackagesAttribute) Util.getAttribute(classFileReader, IAttributeNamesConstants.MODULE_PACKAGES);
			disassembleModule(modulePackagesAttribute, buffer, lineSeparator, 1);
			IModuleMainClassAttribute mainClassAttribute = (IModuleMainClassAttribute) Util.getAttribute(classFileReader, IAttributeNamesConstants.MODULE_MAIN_CLASS);
			disassembleModule(mainClassAttribute, buffer, lineSeparator, 1);
		}
		disassembleTypeMembers(classFileReader, className, buffer, lineSeparator, 1, mode, isEnum);
		if (checkMode(mode, SYSTEM | DETAILED)) {
			IClassFileAttribute[] attributes = classFileReader.getAttributes();
			int length = attributes.length;
			IEnclosingMethodAttribute enclosingMethodAttribute = (IEnclosingMethodAttribute) Util.getAttribute(classFileReader, IAttributeNamesConstants.ENCLOSING_METHOD);
			INestHostAttribute nestHostAttribute = (INestHostAttribute) Util.getAttribute(classFileReader, IAttributeNamesConstants.NEST_HOST);
			int remainingAttributesLength = length;
			if (innerClassesAttribute != null) {
				remainingAttributesLength--;
			}
			if (enclosingMethodAttribute != null) {
				remainingAttributesLength--;
			}
			if (sourceAttribute != null) {
				remainingAttributesLength--;
			}
			if (signatureAttribute != null) {
				remainingAttributesLength--;
			}
			if (bootstrapMethods != null) {
				remainingAttributesLength--;
			}
			if (moduleAttribute != null) {
				remainingAttributesLength--;
			}
			if (nestHostAttribute != null) {
				remainingAttributesLength--;
			}
			if (nestMembersAttribute != null) {
				remainingAttributesLength--;
			}
			if (permittedSubclassesAttribute != null) {
				remainingAttributesLength--;
			}
			if (innerClassesAttribute != null
					|| enclosingMethodAttribute != null
					|| nestHostAttribute != null
					|| nestMembersAttribute != null
					|| bootstrapMethods != null
					|| moduleAttribute != null
					|| recordAttribute != null
					|| permittedSubclassesAttribute != null
					|| remainingAttributesLength != 0) {
				// this test is to ensure we don't insert more than one line separator
				if (buffer.lastIndexOf(lineSeparator) != buffer.length() - lineSeparator.length()) {
					writeNewLine(buffer, lineSeparator, 0);
				}
			}
			if (innerClassesAttribute != null) {
				disassemble(innerClassesAttribute, buffer, lineSeparator, 1);
			}
			if (enclosingMethodAttribute != null) {
				disassemble(enclosingMethodAttribute, buffer, lineSeparator, 0);
			}
			if (nestHostAttribute != null) {
				disassemble(nestHostAttribute, buffer, lineSeparator, 0);
			}
			if (nestMembersAttribute != null) {
				disassemble(nestMembersAttribute, buffer, lineSeparator, 0);
			}
			if (recordAttribute != null) {
				disassemble(recordAttribute, buffer, lineSeparator, 0, mode);
			}
			if (bootstrapMethods != null) {
				disassemble((IBootstrapMethodsAttribute) bootstrapMethods, buffer, lineSeparator, 0, classFileReader.getConstantPool());
			}
			if (permittedSubclassesAttribute != null) {
				disassemble(permittedSubclassesAttribute, buffer, lineSeparator, 0);
			}
			if (checkMode(mode, SYSTEM)) {
				if (runtimeVisibleAnnotationsAttribute != null) {
					disassemble((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				}
				if (runtimeInvisibleAnnotationsAttribute != null) {
					disassemble((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				}
				if (runtimeVisibleTypeAnnotationsAttribute != null) {
					disassemble((IRuntimeVisibleTypeAnnotationsAttribute) runtimeVisibleTypeAnnotationsAttribute, buffer, lineSeparator, 0, mode);
				}
				if (runtimeInvisibleTypeAnnotationsAttribute != null) {
					disassemble((IRuntimeInvisibleTypeAnnotationsAttribute) runtimeInvisibleTypeAnnotationsAttribute, buffer, lineSeparator, 0, mode);
	 			}
				if (length != 0) {
					for (int i = 0; i < length; i++) {
						IClassFileAttribute attribute = attributes[i];
						if (attribute != innerClassesAttribute
								&& attribute != nestHostAttribute
								&& attribute != nestMembersAttribute
								&& attribute != recordAttribute
								&& attribute != permittedSubclassesAttribute
								&& attribute != sourceAttribute
								&& attribute != signatureAttribute
								&& attribute != enclosingMethodAttribute
								&& attribute != runtimeInvisibleAnnotationsAttribute
								&& attribute != runtimeVisibleAnnotationsAttribute
								&& attribute != runtimeInvisibleTypeAnnotationsAttribute
								&& attribute != runtimeVisibleTypeAnnotationsAttribute
								&& !CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.DEPRECATED)
								&& !CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.SYNTHETIC)
								&& attribute != bootstrapMethods
								&& attribute != moduleAttribute
								) {
							disassemble(attribute, buffer, lineSeparator, 0, mode);
						}
					}
				}
			}
		}
		writeNewLine(buffer, lineSeparator, 0);
		buffer.append(Messages.disassembler_closetypedeclaration);
		return buffer.toString();
	}

	private void disassembleModule(IModuleAttribute moduleAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		char[] moduleVersion = moduleAttribute.getModuleVersionValue();
		if (moduleVersion == null) {
			moduleVersion = Messages.disassembler_module_version_none.toCharArray();
		}
		buffer.append(Messages.bind(Messages.disassembler_module_version, new String(moduleVersion)));
		IRequiresInfo[] requiresInfo = moduleAttribute.getRequiresInfo();
		if (requiresInfo.length > 0) {
			writeNewLine(buffer, lineSeparator, 0);
			for (int i = 0, max = requiresInfo.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(requiresInfo[i], buffer, lineSeparator, tabNumber);
			}
		}
		IPackageVisibilityInfo[] exportInfos = moduleAttribute.getExportsInfo();
		if (exportInfos.length > 0) {
			writeNewLine(buffer, lineSeparator, 0);
			for (int i = 0, max = exportInfos.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(exportInfos[i], buffer, lineSeparator, tabNumber, true);
			}
		}
		IPackageVisibilityInfo[] opensInfos = moduleAttribute.getOpensInfo();
		if (opensInfos.length > 0) {
			writeNewLine(buffer, lineSeparator, 0);
			for (int i = 0, max = opensInfos.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(opensInfos[i], buffer, lineSeparator, tabNumber, false);
			}
		}
		char[][] usesNames = moduleAttribute.getUsesClassNames();
		if (usesNames.length > 0) {
			writeNewLine(buffer, lineSeparator, 0);
			for (int i = 0, max = usesNames.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				buffer.append("uses " + CharOperation.charToString(CharOperation.replaceOnCopy(usesNames[i], '/','.'))); //$NON-NLS-1$
			}
		}
		IProvidesInfo[] providesInfos = moduleAttribute.getProvidesInfo();
		if (providesInfos.length > 0) {
			writeNewLine(buffer, lineSeparator, 0);
			for (int i = 0, max = providesInfos.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(providesInfos[i], buffer, lineSeparator, tabNumber);
			}
		}
	}
	private void convertModuleNames(StringBuffer buffer, char[] name) {
		buffer.append(CharOperation.replaceOnCopy(CharOperation.replaceOnCopy(name, '$','.'), '/','.'));
	}

	private void disassembleModule(IModulePackagesAttribute modulePackagesAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		if (modulePackagesAttribute == null) return;
		writeNewLine(buffer, lineSeparator, tabNumber);
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_modulepackagesattributeheader);
		char[][] names = modulePackagesAttribute.getPackageNames();
		for (int i = 0, l = modulePackagesAttribute.getPackagesCount(); i < l; ++i) {
			writeNewLine(buffer, lineSeparator, tabNumber + 1);
			convertModuleNames(buffer, names[i]);
		}
		writeNewLine(buffer, lineSeparator, 0);
	}

	private void disassembleModule(IModuleMainClassAttribute moduleMainClassAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		if (moduleMainClassAttribute == null) return;
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_modulemainclassattributeheader);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		convertModuleNames(buffer, moduleMainClassAttribute.getMainClassName());
		writeNewLine(buffer, lineSeparator, 0);
	}

	private void disassemble(IProvidesInfo iProvidesInfo, StringBuffer buffer, String lineSeparator, int tabNumber) {
		buffer.append("provides"); //$NON-NLS-1$
		buffer.append(Messages.disassembler_space);
		convertModuleNames(buffer, iProvidesInfo.getServiceName());
		buffer.append(Messages.disassembler_space);
		char[][] implementations = iProvidesInfo.getImplementationNames();
		if (implementations.length > 0) {
			buffer.append( "with"); //$NON-NLS-1$
			buffer.append(Messages.disassembler_space);
			for (int i = 0, l = implementations.length; i < l; ++i) {
				if (i != 0) {
					buffer
						.append(Messages.disassembler_comma)
						.append(Messages.disassembler_space);
				}
				convertModuleNames(buffer, implementations[i]);
			}
		}
		buffer.append(';');
	}

	private void disassemble(INestHostAttribute nestHostAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		writeNewLine(buffer, lineSeparator, tabNumber); // additional line
		buffer.append(Messages.disassembler_nesthost);
		buffer
			.append(Messages.disassembler_constantpoolindex)
			.append(nestHostAttribute.getNestHostIndex())
			.append(" ")//$NON-NLS-1$
			.append(nestHostAttribute.getNestHostName());
	}
	private void disassemble(IRecordAttribute recordAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		writeNewLine(buffer, lineSeparator, tabNumber); // additional line
		buffer.append(Messages.disassembler_record);
		buffer
			.append(Messages.disassembler_constantpoolindex)
			.append(recordAttribute.getAttributeName());
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_components);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		IComponentInfo[] entries = recordAttribute.getComponentInfos();
		for (IComponentInfo e : entries) {
			disassemble(e, buffer, lineSeparator, tabNumber, mode);
		}
	}
	private void disassemble(IComponentInfo componentInfo, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final char[] descriptor = componentInfo.getDescriptor();
		final ISignatureAttribute signatureAttribute = (ISignatureAttribute) Util.getAttribute(componentInfo, IAttributeNamesConstants.SIGNATURE);
		if (checkMode(mode, SYSTEM | DETAILED)) {
			buffer.append(Messages.bind(Messages.classfileformat_componentdescriptor,
				new String[] {
					Integer.toString(componentInfo.getDescriptorIndex()),
					new String(descriptor)
				}));
			writeNewLine(buffer, lineSeparator, tabNumber);
			if (signatureAttribute != null) {
				buffer.append(Messages.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		final IClassFileAttribute runtimeVisibleAnnotationsAttribute = Util.getAttribute(componentInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleAnnotationsAttribute = Util.getAttribute(componentInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeVisibleTypeAnnotationsAttribute = Util.getAttribute(componentInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleTypeAnnotationsAttribute = Util.getAttribute(componentInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
		if (checkMode(mode, DETAILED)) {
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		if (checkMode(mode, WORKING_COPY)) {
			if (signatureAttribute != null) {
				buffer.append(returnClassName(getSignatureForComponent(signatureAttribute.getSignature()), '.', mode));
			} else {
				buffer.append(returnClassName(getSignatureForComponent(descriptor), '.', mode));
			}
		} else {
			buffer.append(returnClassName(getSignatureForComponent(descriptor), '.', mode));
		}
		buffer.append(' ');
		buffer.append(new String(componentInfo.getName()));

		buffer.append(Messages.disassembler_endofcomponent);
		if (checkMode(mode, SYSTEM)) {
			IClassFileAttribute[] attributes = componentInfo.getAttributes();
			int length = attributes.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					IClassFileAttribute attribute = attributes[i];
					if (attribute != signatureAttribute
						&& attribute != runtimeInvisibleAnnotationsAttribute
						&& attribute != runtimeVisibleAnnotationsAttribute
						&& attribute != runtimeInvisibleTypeAnnotationsAttribute
						&& attribute != runtimeVisibleTypeAnnotationsAttribute) {
						disassemble(attribute, buffer, lineSeparator, tabNumber, mode);
					}
				}
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleTypeAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleTypeAnnotationsAttribute) runtimeVisibleTypeAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleTypeAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleTypeAnnotationsAttribute) runtimeInvisibleTypeAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
 			}
		}
	}

	private void disassemble(INestMembersAttribute nestMembersAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		writeNewLine(buffer, lineSeparator, tabNumber); // additional line
		buffer.append(Messages.disassembler_nestmembers);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		INestMemberAttributeEntry[] entries = nestMembersAttribute.getNestMemberAttributesEntries();
		int length = entries.length;
		int nestMemberIndex;
		INestMemberAttributeEntry entry;
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				buffer.append(Messages.disassembler_comma);
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
			}
			entry = entries[i];
			nestMemberIndex = entry.getNestMemberIndex();
			buffer
				.append(Messages.disassembler_constantpoolindex)
				.append(nestMemberIndex);
			if (nestMemberIndex != 0) {
				buffer
					.append(Messages.disassembler_space)
					.append(entry.getNestMemberName());
			}
		}
	}
	private void disassemble(IPermittedSubclassesAttribute permittedSubclassesAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		writeNewLine(buffer, lineSeparator, tabNumber); // additional line
		buffer.append(Messages.disassembler_permittedsubclasses);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		IPermittedSubclassesAttributeEntry[] entries = permittedSubclassesAttribute.getPermittedSubclassAttributesEntries();
		int length = entries.length;
		int permittedSubclassesIndex;
		IPermittedSubclassesAttributeEntry entry;
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				buffer.append(Messages.disassembler_comma);
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
			}
			entry = entries[i];
			permittedSubclassesIndex = entry.gePermittedSubclassIndex();
			buffer
				.append(Messages.disassembler_constantpoolindex)
				.append(permittedSubclassesIndex);
			if (permittedSubclassesIndex != 0) {
				buffer
					.append(Messages.disassembler_space)
					.append(entry.getPermittedSubclassName());
			}
		}
	}
	private void disassemble(IPackageVisibilityInfo iPackageVisibilityInfo, StringBuffer buffer, String lineSeparator,
			int tabNumber, boolean isExports) {
		buffer.append(isExports ? "exports" : "opens"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(Messages.disassembler_space);
		convertModuleNames(buffer, iPackageVisibilityInfo.getPackageName());
		char[][] targets = iPackageVisibilityInfo.getTargetModuleNames();
		if (targets.length > 0) {
			buffer.append(Messages.disassembler_space);
			buffer.append( "to"); //$NON-NLS-1$
			buffer.append(Messages.disassembler_space);
			for (int i = 0, l = targets.length; i < l; ++i) {
				if (i != 0) {
					buffer
						.append(Messages.disassembler_comma)
						.append(Messages.disassembler_space);
				}
				buffer.append(targets[i]);
			}
		}
		buffer.append(';');
	}

	private void disassemble(IRequiresInfo iRequiresInfo, StringBuffer buffer, String lineSeparator, int tabNumber) {
		buffer.append("requires "); //$NON-NLS-1$
		decodeModifiersForModuleRequires(buffer, iRequiresInfo.getRequiresFlags());
		buffer.append(iRequiresInfo.getRequiresModuleName());
		buffer.append(';');
	}

	private void disassembleGenericSignature(int mode, StringBuffer buffer, final char[] signature) {
		CharOperation.replace(signature, '/', '.');
		final char[][] typeParameters = Signature.getTypeParameters(signature);
		final int typeParametersLength = typeParameters.length;
		if (typeParametersLength != 0) {
			buffer.append('<');
			for (int i = 0; i < typeParametersLength; i++) {
				if (i != 0) {
					buffer.append(Messages.disassembler_comma);
				}
				// extract the name
				buffer.append(typeParameters[i], 0, CharOperation.indexOf(':', typeParameters[i]));
				final char[][] bounds = Signature.getTypeParameterBounds(typeParameters[i]);
				final int boundsLength = bounds.length;
				if (boundsLength != 0) {
					if (boundsLength == 1) {
						final char[] bound = bounds[0];
						// check if this is java.lang.Object
						if (!isJavaLangObject(Signature.toCharArray(bound))) {
							buffer.append(" extends "); //$NON-NLS-1$
							buffer.append(returnClassName(Signature.toCharArray(bound), '.', mode));
						}
					} else {
						buffer.append(" extends "); //$NON-NLS-1$
						for (int j= 0; j < boundsLength; j++) {
							if (j != 0) {
								buffer.append(" & "); //$NON-NLS-1$
							}
							buffer.append(returnClassName(Signature.toCharArray(bounds[j]), '.', mode));
						}
					}
				}
			}
			buffer.append('>');
		}
	}

	private boolean isJavaLangObject(final char[] className) {
		return CharOperation.equals(TypeConstants.JAVA_LANG_OBJECT, CharOperation.splitOn('.', className));
	}


	private boolean isVarArgs(IMethodInfo methodInfo) {
		int accessFlags = methodInfo.getAccessFlags();
		if ((accessFlags & IModifierConstants.ACC_VARARGS) != 0) return true;
		// check the presence of the unspecified Varargs attribute
		return Util.getAttribute(methodInfo, AttributeNamesConstants.VarargsName) != null;
	}
	private void disassemble(ICodeAttribute codeAttribute, char[][] parameterNames, char[] methodDescriptor, boolean isStatic, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber - 1);
		DefaultBytecodeVisitor visitor = new DefaultBytecodeVisitor(codeAttribute, parameterNames, methodDescriptor, isStatic, buffer, lineSeparator, tabNumber, mode);
		try {
			codeAttribute.traverse(visitor);
		} catch(ClassFormatException e) {
			dumpTab(tabNumber + 3, buffer);
			buffer.append(Messages.classformat_classformatexception);
			writeNewLine(buffer, lineSeparator, tabNumber - 1);
		}
		final int exceptionTableLength = codeAttribute.getExceptionTableLength();
		boolean isFirstAttribute = true;
		if (exceptionTableLength != 0) {
			final int tabNumberForExceptionAttribute = tabNumber + 2;
			isFirstAttribute = false;
			dumpTab(tabNumberForExceptionAttribute, buffer);
			final IExceptionTableEntry[] exceptionTableEntries = codeAttribute.getExceptionTable();
			buffer.append(Messages.disassembler_exceptiontableheader);
			writeNewLine(buffer, lineSeparator, tabNumberForExceptionAttribute + 1);
			for (int i = 0; i < exceptionTableLength; i++) {
				if (i != 0) {
					writeNewLine(buffer, lineSeparator, tabNumberForExceptionAttribute + 1);
				}
				IExceptionTableEntry exceptionTableEntry = exceptionTableEntries[i];
				char[] catchType;
				if (exceptionTableEntry.getCatchTypeIndex() != 0) {
					catchType = exceptionTableEntry.getCatchType();
					CharOperation.replace(catchType, '/', '.');
					catchType = returnClassName(catchType, '.', mode);
				} else {
					catchType = ANY_EXCEPTION;
				}
				buffer.append(Messages.bind(Messages.classfileformat_exceptiontableentry,
					new String[] {
						Integer.toString(exceptionTableEntry.getStartPC()),
						Integer.toString(exceptionTableEntry.getEndPC()),
						Integer.toString(exceptionTableEntry.getHandlerPC()),
						new String(catchType),
					}));
			}
		}
		final ILineNumberAttribute lineNumberAttribute = codeAttribute.getLineNumberAttribute();
		final int lineAttributeLength = lineNumberAttribute == null ? 0 : lineNumberAttribute.getLineNumberTableLength();
		if (lineAttributeLength != 0) {
			int tabNumberForLineAttribute = tabNumber + 2;
			if (!isFirstAttribute) {
				writeNewLine(buffer, lineSeparator, tabNumberForLineAttribute);
			} else {
				dumpTab(tabNumberForLineAttribute, buffer);
				isFirstAttribute = false;
			}
			buffer.append(Messages.disassembler_linenumberattributeheader);
			writeNewLine(buffer, lineSeparator, tabNumberForLineAttribute + 1);
			int[][] lineattributesEntries = lineNumberAttribute.getLineNumberTable();
			for (int i = 0; i < lineAttributeLength; i++) {
				if (i != 0) {
					writeNewLine(buffer, lineSeparator, tabNumberForLineAttribute + 1);
				}
				buffer.append(Messages.bind(Messages.classfileformat_linenumbertableentry,
					new String[] {
						Integer.toString(lineattributesEntries[i][0]),
						Integer.toString(lineattributesEntries[i][1])
					}));
			}
		}
		final ILocalVariableAttribute localVariableAttribute = codeAttribute.getLocalVariableAttribute();
		final int localVariableAttributeLength = localVariableAttribute == null ? 0 : localVariableAttribute.getLocalVariableTableLength();
		if (localVariableAttributeLength != 0) {
			int tabNumberForLocalVariableAttribute = tabNumber + 2;
			if (!isFirstAttribute) {
				writeNewLine(buffer, lineSeparator, tabNumberForLocalVariableAttribute);
			} else {
				isFirstAttribute = false;
				dumpTab(tabNumberForLocalVariableAttribute, buffer);
			}
			buffer.append(Messages.disassembler_localvariabletableattributeheader);
			writeNewLine(buffer, lineSeparator, tabNumberForLocalVariableAttribute + 1);
			ILocalVariableTableEntry[] localVariableTableEntries = localVariableAttribute.getLocalVariableTable();
			for (int i = 0; i < localVariableAttributeLength; i++) {
				if (i != 0) {
					writeNewLine(buffer, lineSeparator, tabNumberForLocalVariableAttribute + 1);
				}
				ILocalVariableTableEntry localVariableTableEntry = localVariableTableEntries[i];
				int index= localVariableTableEntry.getIndex();
				int startPC = localVariableTableEntry.getStartPC();
				int length  = localVariableTableEntry.getLength();
				final char[] typeName = Signature.toCharArray(localVariableTableEntry.getDescriptor());
				CharOperation.replace(typeName, '/', '.');
				buffer.append(Messages.bind(Messages.classfileformat_localvariabletableentry,
					new String[] {
						Integer.toString(startPC),
						Integer.toString(startPC + length),
						new String(localVariableTableEntry.getName()),
						Integer.toString(index),
						new String(returnClassName(typeName, '.', mode))
					}));
			}
		}
		final ILocalVariableTypeTableAttribute localVariableTypeAttribute= (ILocalVariableTypeTableAttribute) Util.getAttribute(codeAttribute, IAttributeNamesConstants.LOCAL_VARIABLE_TYPE_TABLE);
		final int localVariableTypeTableLength = localVariableTypeAttribute == null ? 0 : localVariableTypeAttribute.getLocalVariableTypeTableLength();
		if (localVariableTypeTableLength != 0) {
			int tabNumberForLocalVariableAttribute = tabNumber + 2;
			if (!isFirstAttribute) {
				writeNewLine(buffer, lineSeparator, tabNumberForLocalVariableAttribute);
			} else {
				isFirstAttribute = false;
				dumpTab(tabNumberForLocalVariableAttribute, buffer);
			}
			buffer.append(Messages.disassembler_localvariabletypetableattributeheader);
			writeNewLine(buffer, lineSeparator, tabNumberForLocalVariableAttribute + 1);
			ILocalVariableTypeTableEntry[] localVariableTypeTableEntries = localVariableTypeAttribute.getLocalVariableTypeTable();
			for (int i = 0; i < localVariableTypeTableLength; i++) {
				if (i != 0) {
					writeNewLine(buffer, lineSeparator, tabNumberForLocalVariableAttribute + 1);
				}
				ILocalVariableTypeTableEntry localVariableTypeTableEntry = localVariableTypeTableEntries[i];
				int index= localVariableTypeTableEntry.getIndex();
				int startPC = localVariableTypeTableEntry.getStartPC();
				int length  = localVariableTypeTableEntry.getLength();
				final char[] typeName = Signature.toCharArray(localVariableTypeTableEntry.getSignature());
				CharOperation.replace(typeName, '/', '.');
				buffer.append(Messages.bind(Messages.classfileformat_localvariabletableentry,
					new String[] {
						Integer.toString(startPC),
						Integer.toString(startPC + length),
						new String(localVariableTypeTableEntry.getName()),
						Integer.toString(index),
						new String(returnClassName(typeName, '.', mode))
					}));
			}
		}
		final int length = codeAttribute.getAttributesCount();
		if (length != 0) {
			IClassFileAttribute[] attributes = codeAttribute.getAttributes();
			for (int i = 0; i < length; i++) {
				IClassFileAttribute attribute = attributes[i];
				if (CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.STACK_MAP_TABLE)) {
					IStackMapTableAttribute stackMapTableAttribute = (IStackMapTableAttribute) attribute;
					if (!isFirstAttribute) {
						writeNewLine(buffer, lineSeparator, tabNumber + 2);
					} else {
						isFirstAttribute = false;
						dumpTab(tabNumber + 1, buffer);
					}
					int numberOfEntries = stackMapTableAttribute.getNumberOfEntries();
					buffer.append(Messages.bind(Messages.disassembler_stackmaptableattributeheader, Integer.toString(numberOfEntries)));
					if (numberOfEntries != 0) {
						disassemble(stackMapTableAttribute, buffer, lineSeparator, tabNumber, mode);
					}
				} else if (CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.STACK_MAP)) {
					IStackMapAttribute stackMapAttribute = (IStackMapAttribute) attribute;
					if (!isFirstAttribute) {
						writeNewLine(buffer, lineSeparator, tabNumber + 2);
					} else {
						isFirstAttribute = false;
						dumpTab(tabNumber + 1, buffer);
					}
					int numberOfEntries = stackMapAttribute.getNumberOfEntries();
					buffer.append(Messages.bind(Messages.disassembler_stackmapattributeheader, Integer.toString(numberOfEntries)));
					if (numberOfEntries != 0) {
						disassemble(stackMapAttribute, buffer, lineSeparator, tabNumber, mode);
					}
				} else if (CharOperation.equals(attribute.getAttributeName(),IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)) {
					disassemble((IRuntimeVisibleTypeAnnotationsAttribute) attribute, buffer, lineSeparator, tabNumber, mode);
				} else if (CharOperation.equals(attribute.getAttributeName(),IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)) {
					disassemble((IRuntimeInvisibleTypeAnnotationsAttribute) attribute, buffer, lineSeparator, tabNumber, mode);
				} else if (attribute != lineNumberAttribute
						&& attribute != localVariableAttribute
						&& attribute != localVariableTypeAttribute) {
					if (!isFirstAttribute) {
						writeNewLine(buffer, lineSeparator, tabNumber + 2);
					} else {
						isFirstAttribute = false;
						dumpTab(tabNumber + 1, buffer);
					}
					buffer.append(Messages.bind(Messages.disassembler_genericattributeheader,
						new String[] {
							new String(attribute.getAttributeName()),
							Long.toString(attribute.getAttributeLength())
						}));
				}
			}
		}
	}

	private void disassemble(IStackMapTableAttribute attribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 3);
		int numberOfEntries = attribute.getNumberOfEntries();
		final IStackMapFrame[] stackMapFrames = attribute.getStackMapFrame();
		int absolutePC = -1;
		for (int j = 0; j < numberOfEntries; j++) {
			if (j > 0) {
				writeNewLine(buffer, lineSeparator, tabNumber + 3);
			}
			final IStackMapFrame frame = stackMapFrames[j];
			// disassemble each frame
			int type = frame.getFrameType();
			int offsetDelta = frame.getOffsetDelta();
			if (absolutePC == -1) {
				absolutePC = offsetDelta;
			} else {
				absolutePC += (offsetDelta + 1);
			}
			switch(type) {
				case 247 : // SAME_LOCALS_1_STACK_ITEM_EXTENDED
					buffer.append(
						Messages.bind(
							Messages.disassembler_frame_same_locals_1_stack_item_extended,
							Integer.toString(absolutePC),
							disassemble(frame.getStackItems(), mode)));
					break;
				case 248 :
				case 249 :
				case 250:
					// CHOP
					buffer.append(
							Messages.bind(
								Messages.disassembler_frame_chop,
								Integer.toString(absolutePC),
								Integer.toString(251 - type)));
					break;
				case 251 :
					// SAME_FRAME_EXTENDED
					buffer.append(
							Messages.bind(
								Messages.disassembler_frame_same_frame_extended,
								Integer.toString(absolutePC)));
					break;
				case 252 :
				case 253 :
				case 254 :
					// APPEND
					buffer.append(
							Messages.bind(
								Messages.disassembler_frame_append,
								Integer.toString(absolutePC),
								disassemble(frame.getLocals(), mode)));
					break;
				case 255 :
					// FULL_FRAME
					buffer.append(
							Messages.bind(
								Messages.disassembler_frame_full_frame,
								new String[] {
									Integer.toString(absolutePC),
									Integer.toString(frame.getNumberOfLocals()),
									disassemble(frame.getLocals(), mode),
									Integer.toString(frame.getNumberOfStackItems()),
									disassemble(frame.getStackItems(), mode),
									dumpNewLineWithTabs(lineSeparator, tabNumber + 5)
								}));
					break;
				default:
					if (type <= 63) {
						// SAME_FRAME
						offsetDelta = type;
						buffer.append(
								Messages.bind(
									Messages.disassembler_frame_same_frame,
									Integer.toString(absolutePC)));
					} else if (type <= 127) {
						// SAME_LOCALS_1_STACK_ITEM
						offsetDelta = type - 64;
						buffer.append(
								Messages.bind(
									Messages.disassembler_frame_same_locals_1_stack_item,
									Integer.toString(absolutePC),
									disassemble(frame.getStackItems(), mode)));
					}
			}
		}
	}

	private void disassemble(IStackMapAttribute attribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 3);
		int numberOfEntries = attribute.getNumberOfEntries();
		final IStackMapFrame[] stackMapFrames = attribute.getStackMapFrame();
		for (int j = 0; j < numberOfEntries; j++) {
			if (j > 0) {
				writeNewLine(buffer, lineSeparator, tabNumber + 3);
			}
			final IStackMapFrame frame = stackMapFrames[j];
			// disassemble each frame
			buffer.append(
					Messages.bind(
						Messages.disassembler_frame_full_frame,
						new String[] {
							Integer.toString(frame.getOffsetDelta()),
							Integer.toString(frame.getNumberOfLocals()),
							disassemble(frame.getLocals(), mode),
							Integer.toString(frame.getNumberOfStackItems()),
							disassemble(frame.getStackItems(), mode),
							dumpNewLineWithTabs(lineSeparator, tabNumber + 5)
						}));
		}
	}

	private String bootstrapMethodDescription(IBootstrapMethodsEntry entry, IConstantPool constantPool) {
		// http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
		// The BootstrapMethods attribute records bootstrap method specifiers referenced by invokedynamic instructions.
		// The value of the bootstrap_method_ref item must be a valid index into the constant_pool table. The constant_pool entry at that index must be a CONSTANT_MethodHandle_info structure (§4.4.8).
		// http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.8
		// constantpoolentry.getKind() = IConstantPoolConstant.CONSTANT_MethodHandle

		ConstantPoolEntry2 constantPoolEntry2 =  (ConstantPoolEntry2) constantPool.decodeEntry(entry.getBootstrapMethodReference());

		// The reference_kind item of the CONSTANT_MethodHandle_info structure should have the value 6 (REF_invokeStatic) or 8 (REF_newInvokeSpecial)
		// (§5.4.3.5) or else invocation of the bootstrap method handle during call site specifier resolution for an invokedynamic instruction will complete abruptly.
		// If the value of the reference_kind item is 5 (REF_invokeVirtual), 6 (REF_invokeStatic), 7 (REF_invokeSpecial),
		// or 9 (REF_invokeInterface), the name of the method represented by a CONSTANT_Methodref_info structure must not be <init> or <clinit>.

		if (constantPoolEntry2.getReferenceKind() != 6)
			return null;
		ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) constantPool.decodeEntry(constantPoolEntry2.getReferenceIndex());
		StringBuilder builder = new StringBuilder();
		//String[] methodMsg = methodDescription(constantPoolEntry);
		builder.append(Messages.bind("invokestatic {0}.{1}:{2}", methodDescription(constantPoolEntry))); //$NON-NLS-1$
		return builder.toString();
	}

	private String[] bootstrapArgumentsDescription(IBootstrapMethodsEntry entry, IConstantPool constantPool) {
		// http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.21
		// bootstrap_arguments
		// 	    Each entry in the bootstrap_arguments array must be a valid index into the constant_pool table.
		//      The constant_pool entry at that index must be a CONSTANT_String_info, CONSTANT_Class_info, CONSTANT_Integer_info
		//      CONSTANT_Long_info, CONSTANT_Float_info, CONSTANT_Double_info, CONSTANT_MethodHandle_info, or
		//      CONSTANT_MethodType_info structure (§4.4.3, §4.4.1, §4.4.4, §4.4.5), §4.4.8, §4.4.9).
		if (entry.getBootstrapArguments().length == 0)
			return null;
		int[] bootstrapArguments = entry.getBootstrapArguments();
		String[] arguments = new String[bootstrapArguments.length];
		for (int i = 0, length = bootstrapArguments.length; i < length; i++) {
			ConstantPoolEntry constantPoolEntry =  (ConstantPoolEntry) constantPool.decodeEntry(bootstrapArguments[i]);
			switch(constantPoolEntry.getKind()) {
				case IConstantPoolConstant.CONSTANT_Integer:
					arguments[i] = ((Integer) constantPoolEntry.getIntegerValue()).toString();
					break;
				case IConstantPoolConstant.CONSTANT_MethodHandle:
					// http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.8
					// If the value of the reference_kind item is 5 (REF_invokeVirtual), 6 (REF_invokeStatic),
					// 7 (REF_invokeSpecial), or 8 (REF_newInvokeSpecial), then the constant_pool entry at that
					// index must be a CONSTANT_Methodref_info structure (§4.4.2) representing a class's method or
					// constructor (§2.9) for which a method handle is to be created.
					ConstantPoolEntry2 constantPoolEntry2 = (ConstantPoolEntry2) constantPoolEntry;
					StringBuilder builder = new StringBuilder(10);
					switch(constantPoolEntry2.getReferenceKind()) {
						case IConstantPoolConstant.METHOD_TYPE_REF_GetField:
							builder.append("REF_getField "); //$NON-NLS-1$
							constantPoolEntry = (ConstantPoolEntry) constantPool.decodeEntry(constantPoolEntry2.getReferenceIndex());
							builder.append(Messages.bind("{0}:{1}", fieldDescription(constantPoolEntry))); //$NON-NLS-1$
							arguments[i] =  builder.toString();
							break;
						case IConstantPoolConstant.METHOD_TYPE_REF_InvokeStatic:
							builder.append("invokestatic "); //$NON-NLS-1$
							//$FALL-THROUGH$
						case IConstantPoolConstant.METHOD_TYPE_REF_InvokeVirtual:
						case IConstantPoolConstant.METHOD_TYPE_REF_NewInvokeSpecial:
							constantPoolEntry = (ConstantPoolEntry) constantPool.decodeEntry(constantPoolEntry2.getReferenceIndex());
							builder.append(Messages.bind("{0}.{1}:{2}", methodDescription(constantPoolEntry))); //$NON-NLS-1$
							arguments[i] =  builder.toString();
							break;
					}
					break;
				case IConstantPoolConstant.CONSTANT_MethodType:
					arguments[i] = new String(((ConstantPoolEntry2) constantPoolEntry).getMethodDescriptor());
					break;
				case IConstantPoolConstant.CONSTANT_Class:
					arguments[i] = new String(constantPoolEntry.getClassInfoName());
					break;
				case IConstantPoolConstant.CONSTANT_String:
					arguments[i] = constantPoolEntry.getStringValue();
					break;
			}
		}
		return arguments;
	}

	private String[] fieldDescription(IConstantPoolEntry constantPoolEntry) {
		return new String[] { new String(constantPoolEntry.getFieldName()),
				new String(constantPoolEntry.getFieldDescriptor())};
	}

	private String[] methodDescription(IConstantPoolEntry constantPoolEntry) {
		return new String[] { new String(constantPoolEntry.getClassName()),
				new String(constantPoolEntry.getMethodName()),
				new String(constantPoolEntry.getMethodDescriptor())};
	}

	private void disassemble(IConstantPool constantPool, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		int length = constantPool.getConstantPoolCount();
		buffer.append(Messages.disassembler_constantpoolheader);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		for (int i = 1; i < length; i++) {
			if (i != 1) {
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
			}
			IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(i);
			String[] methodDescription;
			int kind = constantPool.getEntryKind(i);
			switch (kind) {
				case IConstantPoolConstant.CONSTANT_Class :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_class,
							new String[] {
								Integer.toString(i),
								Integer.toString(constantPoolEntry.getClassInfoNameIndex()),
								new String(constantPoolEntry.getClassInfoName())}));
					break;
				case IConstantPoolConstant.CONSTANT_Double :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_double,
							new String[] {
								Integer.toString(i),
								Double.toString(constantPoolEntry.getDoubleValue())}));
					break;
				case IConstantPoolConstant.CONSTANT_Fieldref :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_fieldref,
							new String[] {
								Integer.toString(i),
								Integer.toString(constantPoolEntry.getClassIndex()),
								Integer.toString(constantPoolEntry.getNameAndTypeIndex()),
								new String(constantPoolEntry.getClassName()),
								new String(constantPoolEntry.getFieldName()),
								new String(constantPoolEntry.getFieldDescriptor())
							}));
					break;
				case IConstantPoolConstant.CONSTANT_Float :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_float,
						new String[] {
							Integer.toString(i),
							Float.toString(constantPoolEntry.getFloatValue())}));
					break;
				case IConstantPoolConstant.CONSTANT_Integer :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_integer,
							new String[] {
								Integer.toString(i),
								Integer.toString(constantPoolEntry.getIntegerValue())}));
					break;
				case IConstantPoolConstant.CONSTANT_InterfaceMethodref :
					methodDescription = methodDescription(constantPoolEntry);
					buffer.append(
							Messages.bind(Messages.disassembler_constantpool_interfacemethodref,
								new String[] {
									Integer.toString(i),
									Integer.toString(constantPoolEntry.getClassIndex()),
									Integer.toString(constantPoolEntry.getNameAndTypeIndex()),
									methodDescription[0], methodDescription[1], methodDescription[2]}));
					break;
				case IConstantPoolConstant.CONSTANT_Long :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_long,
							new String[] {
								Integer.toString(i),
								Long.toString(constantPoolEntry.getLongValue())}));
					break;
				case IConstantPoolConstant.CONSTANT_Methodref :
					methodDescription = methodDescription(constantPoolEntry);
					buffer.append(
							Messages.bind(Messages.disassembler_constantpool_methodref,
								new String[] {
									Integer.toString(i),
									Integer.toString(constantPoolEntry.getClassIndex()),
									Integer.toString(constantPoolEntry.getNameAndTypeIndex()),
									methodDescription[0], methodDescription[1], methodDescription[2]}));
					break;
				case IConstantPoolConstant.CONSTANT_NameAndType :
					int nameIndex = constantPoolEntry.getNameAndTypeInfoNameIndex();
					int typeIndex = constantPoolEntry.getNameAndTypeInfoDescriptorIndex();
					IConstantPoolEntry entry = constantPool.decodeEntry(nameIndex);
					char[] nameValue = entry.getUtf8Value();
					entry = constantPool.decodeEntry(typeIndex);
					char[] typeValue = entry.getUtf8Value();
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_name_and_type,
							new String[] {
								Integer.toString(i),
								Integer.toString(nameIndex),
								Integer.toString(typeIndex),
								String.valueOf(nameValue),
								String.valueOf(typeValue)}));
					break;
				case IConstantPoolConstant.CONSTANT_String :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_string,
							new String[] {
								Integer.toString(i),
								Integer.toString(constantPoolEntry.getStringIndex()),
								decodeStringValue(constantPoolEntry.getStringValue())}));
					break;
				case IConstantPoolConstant.CONSTANT_Utf8 :
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_utf8,
							new String[] {
								Integer.toString(i),
								decodeStringValue(new String(constantPoolEntry.getUtf8Value()))}));
					break;
				case IConstantPoolConstant.CONSTANT_MethodHandle :
					IConstantPoolEntry2 entry2 = (IConstantPoolEntry2) constantPoolEntry;
					buffer.append(
							Messages.bind(Messages.disassembler_constantpool_methodhandle,
								new String[] {
									Integer.toString(i),
									getReferenceKind(entry2.getReferenceKind()),
									Integer.toString(entry2.getReferenceIndex()),
								}));
					break;
				case IConstantPoolConstant.CONSTANT_MethodType :
					entry2 = (IConstantPoolEntry2) constantPoolEntry;
					buffer.append(
							Messages.bind(Messages.disassembler_constantpool_methodtype,
								new String[] {
									Integer.toString(i),
									Integer.toString(entry2.getDescriptorIndex()),
									String.valueOf(entry2.getMethodDescriptor()),
								}));
					break;
				case IConstantPoolConstant.CONSTANT_InvokeDynamic :
					entry2 = (IConstantPoolEntry2) constantPoolEntry;
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_invokedynamic,
							new String[] {
								Integer.toString(i),
								Integer.toString(entry2.getBootstrapMethodAttributeIndex()),
								Integer.toString(entry2.getNameAndTypeIndex()),
								new String(constantPoolEntry.getMethodName()),
								new String(constantPoolEntry.getMethodDescriptor())
							}));
					break;
				case IConstantPoolConstant.CONSTANT_Dynamic :
					entry2 = (IConstantPoolEntry2) constantPoolEntry;
					buffer.append(
						Messages.bind(Messages.disassembler_constantpool_dynamic,
							new String[] {
								Integer.toString(i),
								Integer.toString(entry2.getBootstrapMethodAttributeIndex()),
								Integer.toString(entry2.getNameAndTypeIndex()),
								new String(constantPoolEntry.getFieldName()),
								new String(constantPoolEntry.getFieldDescriptor())
							}));
					break;
			}
		}
	}

	private String getReferenceKind(int referenceKind) {
		String message = null;
		switch(referenceKind) {
			case IConstantPoolConstant.METHOD_TYPE_REF_GetField :
				message = Messages.disassembler_method_type_ref_getfield;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_GetStatic :
				message = Messages.disassembler_method_type_ref_getstatic;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_PutField :
				message = Messages.disassembler_method_type_ref_putfield;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_PutStatic :
				message = Messages.disassembler_method_type_ref_putstatic;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_InvokeInterface :
				message = Messages.disassembler_method_type_ref_invokeinterface;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_InvokeSpecial :
				message = Messages.disassembler_method_type_ref_invokespecial;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_InvokeStatic :
				message = Messages.disassembler_method_type_ref_invokestatic;
				break;
			case IConstantPoolConstant.METHOD_TYPE_REF_InvokeVirtual :
				message = Messages.disassembler_method_type_ref_invokevirtual;
				break;
			default :
				message = Messages.disassembler_method_type_ref_newinvokespecial;
		}
		return Messages.bind(message, new String[] { Integer.toString(referenceKind) });
	}

	private void disassemble(IEnclosingMethodAttribute enclosingMethodAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_enclosingmethodheader);
		buffer
			.append(Messages.disassembler_constantpoolindex)
			.append(enclosingMethodAttribute.getEnclosingClassIndex())
			.append(" ")//$NON-NLS-1$
			.append(Messages.disassembler_constantpoolindex)
			.append(enclosingMethodAttribute.getMethodNameAndTypeIndex())
			.append(" ")//$NON-NLS-1$
			.append(enclosingMethodAttribute.getEnclosingClass());
		if (enclosingMethodAttribute.getMethodNameAndTypeIndex() != 0) {
			buffer
				.append(".")//$NON-NLS-1$
				.append(enclosingMethodAttribute.getMethodName())
				.append(enclosingMethodAttribute.getMethodDescriptor());
		}
	}

	private void disassembleEnumConstants(IFieldInfo fieldInfo, StringBuffer buffer, String lineSeparator, int tabNumber, char[][] argumentTypes, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final IClassFileAttribute runtimeVisibleAnnotationsAttribute = Util.getAttribute(fieldInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleAnnotationsAttribute = Util.getAttribute(fieldInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		// disassemble compact version of annotations
		if (runtimeInvisibleAnnotationsAttribute != null) {
			disassembleAsModifier((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			writeNewLine(buffer, lineSeparator, tabNumber);
		}
		if (runtimeVisibleAnnotationsAttribute != null) {
			disassembleAsModifier((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			writeNewLine(buffer, lineSeparator, tabNumber);
		}
		buffer.append(new String(fieldInfo.getName()));
		buffer.append('(');
		final int length = argumentTypes.length;
		if (length != 0) {
			// insert default value for corresponding argument types
			for (int i = 0; i < length; i++) {
				if (i != 0) {
					buffer.append(Messages.disassembler_comma);
				}
				final char[] type = argumentTypes[i];
				switch(type.length) {
					case 1 :
						switch(type[0]) {
							case 'B' :
							case 'I' :
							case 'J' :
							case 'D' :
							case 'F' :
							case 'S' :
								buffer.append('0');
								break;
							case 'Z' :
								buffer.append("false"); //$NON-NLS-1$
								break;
							case 'C' :
								buffer.append("\' \'"); //$NON-NLS-1$
								break;
						}
						break;
					default :
						buffer.append("null"); //$NON-NLS-1$
				}
			}
		}
		buffer.append(')').append(Messages.disassembler_comma);
	}

	/**
	 * Disassemble a field info
	 */
	private void disassemble(IFieldInfo fieldInfo, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		final char[] fieldDescriptor = fieldInfo.getDescriptor();
		final ISignatureAttribute signatureAttribute = (ISignatureAttribute) Util.getAttribute(fieldInfo, IAttributeNamesConstants.SIGNATURE);
		if (checkMode(mode, SYSTEM | DETAILED)) {
			buffer.append(Messages.bind(Messages.classfileformat_fieldddescriptor,
				new String[] {
					Integer.toString(fieldInfo.getDescriptorIndex()),
					new String(fieldDescriptor)
				}));
			if (fieldInfo.isDeprecated()) {
				buffer.append(Messages.disassembler_deprecated);
			}
			writeNewLine(buffer, lineSeparator, tabNumber);
			if (signatureAttribute != null) {
				buffer.append(Messages.bind(Messages.disassembler_signatureattributeheader, new String(signatureAttribute.getSignature())));
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		final IClassFileAttribute runtimeVisibleAnnotationsAttribute = Util.getAttribute(fieldInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleAnnotationsAttribute = Util.getAttribute(fieldInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS);
		final IClassFileAttribute runtimeVisibleTypeAnnotationsAttribute = Util.getAttribute(fieldInfo, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
		final IClassFileAttribute runtimeInvisibleTypeAnnotationsAttribute = Util.getAttribute(fieldInfo, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
		if (checkMode(mode, DETAILED)) {
			// disassemble compact version of annotations
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassembleAsModifier((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
		}
		if (checkMode(mode, WORKING_COPY)) {
			decodeModifiersForFieldForWorkingCopy(buffer, fieldInfo.getAccessFlags());
			if (signatureAttribute != null) {
				buffer.append(returnClassName(getSignatureForField(signatureAttribute.getSignature()), '.', mode));
			} else {
				buffer.append(returnClassName(getSignatureForField(fieldDescriptor), '.', mode));
			}
		} else {
			decodeModifiersForField(buffer, fieldInfo.getAccessFlags());
			if (fieldInfo.isSynthetic()) {
				buffer.append("synthetic"); //$NON-NLS-1$
				buffer.append(Messages.disassembler_space);
			}
			buffer.append(returnClassName(getSignatureForField(fieldDescriptor), '.', mode));
		}
		buffer.append(' ');
		buffer.append(new String(fieldInfo.getName()));
		IConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
		if (constantValueAttribute != null) {
			buffer.append(Messages.disassembler_fieldhasconstant);
			IConstantPoolEntry constantPoolEntry = constantValueAttribute.getConstantValue();
			switch(constantPoolEntry.getKind()) {
				case IConstantPoolConstant.CONSTANT_Long :
					buffer.append(constantPoolEntry.getLongValue() + "L"); //$NON-NLS-1$
					break;
				case IConstantPoolConstant.CONSTANT_Float :
					buffer.append(constantPoolEntry.getFloatValue() + "f"); //$NON-NLS-1$
					break;
				case IConstantPoolConstant.CONSTANT_Double :
					final double doubleValue = constantPoolEntry.getDoubleValue();
					if (checkMode(mode, ClassFileBytesDisassembler.WORKING_COPY)) {
						if (doubleValue == Double.POSITIVE_INFINITY) {
							buffer.append("1.0 / 0.0"); //$NON-NLS-1$
						} else if (doubleValue == Double.NEGATIVE_INFINITY) {
							buffer.append("-1.0 / 0.0"); //$NON-NLS-1$
						} else {
							buffer.append(constantPoolEntry.getDoubleValue());
						}
					} else {
						buffer.append(constantPoolEntry.getDoubleValue());
					}
					break;
				case IConstantPoolConstant.CONSTANT_Integer:
					switch(fieldDescriptor[0]) {
						case 'C' :
							buffer.append("'" + (char) constantPoolEntry.getIntegerValue() + "'"); //$NON-NLS-1$//$NON-NLS-2$
							break;
						case 'Z' :
							buffer.append(constantPoolEntry.getIntegerValue() == 1 ? "true" : "false");//$NON-NLS-1$//$NON-NLS-2$
							break;
						case 'B' :
							buffer.append(constantPoolEntry.getIntegerValue());
							break;
						case 'S' :
							buffer.append(constantPoolEntry.getIntegerValue());
							break;
						case 'I' :
							buffer.append(constantPoolEntry.getIntegerValue());
					}
					break;
				case IConstantPoolConstant.CONSTANT_String:
					buffer.append("\"" + decodeStringValue(constantPoolEntry.getStringValue()) + "\"" );//$NON-NLS-1$//$NON-NLS-2$
			}
		}
		buffer.append(Messages.disassembler_endoffieldheader);
		if (checkMode(mode, SYSTEM)) {
			IClassFileAttribute[] attributes = fieldInfo.getAttributes();
			int length = attributes.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					IClassFileAttribute attribute = attributes[i];
					if (attribute != constantValueAttribute
						&& attribute != signatureAttribute
						&& attribute != runtimeInvisibleAnnotationsAttribute
						&& attribute != runtimeVisibleAnnotationsAttribute
						&& attribute != runtimeInvisibleTypeAnnotationsAttribute
						&& attribute != runtimeVisibleTypeAnnotationsAttribute
						&& !CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.DEPRECATED)
						&& !CharOperation.equals(attribute.getAttributeName(), IAttributeNamesConstants.SYNTHETIC)) {
						disassemble(attribute, buffer, lineSeparator, tabNumber, mode);
					}
				}
			}
			if (runtimeVisibleAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleAnnotationsAttribute) runtimeVisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleAnnotationsAttribute) runtimeInvisibleAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeVisibleTypeAnnotationsAttribute != null) {
				disassemble((IRuntimeVisibleTypeAnnotationsAttribute) runtimeVisibleTypeAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
			}
			if (runtimeInvisibleTypeAnnotationsAttribute != null) {
				disassemble((IRuntimeInvisibleTypeAnnotationsAttribute) runtimeInvisibleTypeAnnotationsAttribute, buffer, lineSeparator, tabNumber, mode);
 			}
		}
	}

	private void disassemble(IInnerClassesAttribute innerClassesAttribute, StringBuffer buffer, String lineSeparator, int tabNumber) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_innerattributesheader);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		IInnerClassesAttributeEntry[] innerClassesAttributeEntries = innerClassesAttribute.getInnerClassAttributesEntries();
		int length = innerClassesAttributeEntries.length;
		int innerClassNameIndex, outerClassNameIndex, innerNameIndex, accessFlags;
		IInnerClassesAttributeEntry innerClassesAttributeEntry;
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				buffer.append(Messages.disassembler_comma);
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
			}
			innerClassesAttributeEntry = innerClassesAttributeEntries[i];
			innerClassNameIndex = innerClassesAttributeEntry.getInnerClassNameIndex();
			outerClassNameIndex = innerClassesAttributeEntry.getOuterClassNameIndex();
			innerNameIndex = innerClassesAttributeEntry.getInnerNameIndex();
			accessFlags = innerClassesAttributeEntry.getAccessFlags();
			buffer
				.append(Messages.disassembler_openinnerclassentry)
				.append(Messages.disassembler_inner_class_info_name)
				.append(Messages.disassembler_constantpoolindex)
				.append(innerClassNameIndex);
			if (innerClassNameIndex != 0) {
				buffer
					.append(Messages.disassembler_space)
					.append(innerClassesAttributeEntry.getInnerClassName());
			}
			buffer
				.append(Messages.disassembler_comma)
				.append(Messages.disassembler_space)
				.append(Messages.disassembler_outer_class_info_name)
				.append(Messages.disassembler_constantpoolindex)
				.append(outerClassNameIndex);
			if (outerClassNameIndex != 0) {
				buffer
					.append(Messages.disassembler_space)
					.append(innerClassesAttributeEntry.getOuterClassName());
			}
			writeNewLine(buffer, lineSeparator, tabNumber);
			dumpTab(tabNumber, buffer);
			buffer.append(Messages.disassembler_space);
			buffer
				.append(Messages.disassembler_inner_name)
				.append(Messages.disassembler_constantpoolindex)
				.append(innerNameIndex);
			if (innerNameIndex != 0) {
				buffer
					.append(Messages.disassembler_space)
					.append(innerClassesAttributeEntry.getInnerName());
			}
			buffer
				.append(Messages.disassembler_comma)
				.append(Messages.disassembler_space)
				.append(Messages.disassembler_inner_accessflags)
				.append(accessFlags)
				.append(Messages.disassembler_space);
			decodeModifiersForInnerClasses(buffer, accessFlags, true);
			buffer
				.append(Messages.disassembler_closeinnerclassentry);
		}
	}

	private void disassemble(IBootstrapMethodsAttribute bootstrapMethodsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, IConstantPool constantPool) {
		writeNewLine(buffer, lineSeparator, tabNumber);
		buffer.append(Messages.disassembler_bootstrapmethodattributesheader);
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		IBootstrapMethodsEntry[] entries = bootstrapMethodsAttribute.getBootstrapMethods();
		int length = entries.length;
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				buffer.append(Messages.disassembler_comma);
				writeNewLine(buffer, lineSeparator, tabNumber + 1);
			}
			IBootstrapMethodsEntry entry = entries[i];
			String[] argumentsName = bootstrapArgumentsDescription(entry, constantPool);

			buffer.append(
				Messages.bind(
					Messages.disassembler_bootstrapmethodentry,
					new String[] {
						Integer.toString(i),
						Integer.toString(entry.getBootstrapMethodReference()),
						bootstrapMethodDescription(entry, constantPool),
						getArguments(entry.getBootstrapArguments(), argumentsName)
					}));
		}
	}

	private String getArguments(int[] arguments, String[] argumentsName) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = arguments.length; i < max; i++) {
			buffer.append(
				Messages.bind(
					Messages.disassembler_bootstrapmethodentry_argument,
					new String[] {
						Integer.toString(arguments[i]),
						argumentsName[i]
					}));
			if (i != arguments.length - 1)
				buffer.append("\n\t\t"); //$NON-NLS-1$
		}
		return String.valueOf(buffer);
	}
	private void disassemble(int index, IParameterAnnotation parameterAnnotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		IAnnotation[] annotations = parameterAnnotation.getAnnotations();
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(
			Messages.bind(Messages.disassembler_parameterannotationentrystart, new String[] {Integer.toString(index), Integer.toString(annotations.length)}));
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassemble(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(IRuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimeinvisibleannotationsattributeheader);
		IAnnotation[] annotations = runtimeInvisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassemble(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(IRuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimeinvisibleparameterannotationsattributeheader);
		IParameterAnnotation[] parameterAnnotations = runtimeInvisibleParameterAnnotationsAttribute.getParameterAnnotations();
		for (int i = 0, max = parameterAnnotations.length; i < max; i++) {
			disassemble(i, parameterAnnotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(IRuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimeinvisibletypeannotationsattributeheader);
		IExtendedAnnotation[] extendedAnnotations = runtimeInvisibleTypeAnnotationsAttribute.getExtendedAnnotations();
		for (int i = 0, max = extendedAnnotations.length; i < max; i++) {
			disassemble(extendedAnnotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(IRuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimevisibleannotationsattributeheader);
		IAnnotation[] annotations = runtimeVisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassemble(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(IRuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimevisibleparameterannotationsattributeheader);
		IParameterAnnotation[] parameterAnnotations = runtimeVisibleParameterAnnotationsAttribute.getParameterAnnotations();
		for (int i = 0, max = parameterAnnotations.length; i < max; i++) {
			disassemble(i, parameterAnnotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassemble(IRuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		writeNewLine(buffer, lineSeparator, tabNumber + 1);
		buffer.append(Messages.disassembler_runtimevisibletypeannotationsattributeheader);
		IExtendedAnnotation[] extendedAnnotations = runtimeVisibleTypeAnnotationsAttribute.getExtendedAnnotations();
		for (int i = 0, max = extendedAnnotations.length; i < max; i++) {
			disassemble(extendedAnnotations[i], buffer, lineSeparator, tabNumber + 1, mode);
 		}
 	}

	private String disassemble(IVerificationTypeInfo[] infos, int mode) {
		StringBuffer buffer = new StringBuffer();
		buffer.append('{');
		for (int i = 0, max = infos.length; i < max; i++) {
			if(i != 0) {
				buffer
						.append(Messages.disassembler_comma)
						.append(Messages.disassembler_space);
			}
			switch(infos[i].getTag()) {
				case IVerificationTypeInfo.ITEM_DOUBLE :
					buffer.append("double"); //$NON-NLS-1$
					break;
				case IVerificationTypeInfo.ITEM_FLOAT :
					buffer.append("float"); //$NON-NLS-1$
					break;
				case IVerificationTypeInfo.ITEM_INTEGER :
					buffer.append("int"); //$NON-NLS-1$
					break;
				case IVerificationTypeInfo.ITEM_LONG :
					buffer.append("long"); //$NON-NLS-1$
					break;
				case IVerificationTypeInfo.ITEM_NULL :
					buffer.append("null"); //$NON-NLS-1$
					break;
				case IVerificationTypeInfo.ITEM_OBJECT :
					char[] classTypeName = infos[i].getClassTypeName();
					CharOperation.replace(classTypeName, '/', '.');
					if (classTypeName.length > 0 && classTypeName[0] == '[') { // length check for resilience
						classTypeName = Signature.toCharArray(classTypeName);
					}
					buffer.append(returnClassName(classTypeName, '.', mode));
					break;
				case IVerificationTypeInfo.ITEM_TOP :
					buffer.append("_"); //$NON-NLS-1$
					break;
				case IVerificationTypeInfo.ITEM_UNINITIALIZED :
					buffer.append("uninitialized("); //$NON-NLS-1$
					buffer.append(infos[i].getOffset());
					buffer.append(')');
					break;
				case IVerificationTypeInfo.ITEM_UNINITIALIZED_THIS :
					buffer.append("uninitialized_this"); //$NON-NLS-1$
			}
		}
		buffer.append('}');
		return String.valueOf(buffer);
	}

	private void disassembleAsModifier(IAnnotation annotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		final char[] typeName = CharOperation.replaceOnCopy(annotation.getTypeName(), '/', '.');
		buffer.append('@').append(returnClassName(Signature.toCharArray(typeName), '.', mode));
		final IAnnotationComponent[] components = annotation.getComponents();
		final int length = components.length;
		if (length != 0) {
			buffer.append('(');
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					buffer.append(',');
					writeNewLine(buffer, lineSeparator, tabNumber);
				}
				disassembleAsModifier(components[i], buffer, lineSeparator, tabNumber + 1, mode);
			}
			buffer.append(')');
		}
	}

	private void disassembleAsModifier(IAnnotationComponent annotationComponent, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		buffer.append(annotationComponent.getComponentName()).append('=');
		disassembleAsModifier(annotationComponent.getComponentValue(), buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassembleAsModifier(IAnnotationComponentValue annotationComponentValue, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		switch(annotationComponentValue.getTag()) {
			case IAnnotationComponentValue.BYTE_TAG:
			case IAnnotationComponentValue.CHAR_TAG:
			case IAnnotationComponentValue.DOUBLE_TAG:
			case IAnnotationComponentValue.FLOAT_TAG:
			case IAnnotationComponentValue.INTEGER_TAG:
			case IAnnotationComponentValue.LONG_TAG:
			case IAnnotationComponentValue.SHORT_TAG:
			case IAnnotationComponentValue.BOOLEAN_TAG:
			case IAnnotationComponentValue.STRING_TAG:
				IConstantPoolEntry constantPoolEntry = annotationComponentValue.getConstantValue();
				String value = null;
				switch(constantPoolEntry.getKind()) {
					case IConstantPoolConstant.CONSTANT_Long :
						value = constantPoolEntry.getLongValue() + "L"; //$NON-NLS-1$
						break;
					case IConstantPoolConstant.CONSTANT_Float :
						value = constantPoolEntry.getFloatValue() + "f"; //$NON-NLS-1$
						break;
					case IConstantPoolConstant.CONSTANT_Double :
						value = Double.toString(constantPoolEntry.getDoubleValue());
						break;
					case IConstantPoolConstant.CONSTANT_Integer:
						StringBuffer temp = new StringBuffer();
						switch(annotationComponentValue.getTag()) {
							case IAnnotationComponentValue.CHAR_TAG :
								temp.append('\'');
								escapeChar(temp, (char) constantPoolEntry.getIntegerValue());
								temp.append('\'');
								break;
							case IAnnotationComponentValue.BOOLEAN_TAG :
								temp.append(constantPoolEntry.getIntegerValue() == 1 ? "true" : "false");//$NON-NLS-1$//$NON-NLS-2$
								break;
							case IAnnotationComponentValue.BYTE_TAG :
								temp.append("(byte) ").append(constantPoolEntry.getIntegerValue()); //$NON-NLS-1$
								break;
							case IAnnotationComponentValue.SHORT_TAG :
								temp.append("(short) ").append(constantPoolEntry.getIntegerValue()); //$NON-NLS-1$
								break;
							case IAnnotationComponentValue.INTEGER_TAG :
								temp.append("(int) ").append(constantPoolEntry.getIntegerValue()); //$NON-NLS-1$
						}
						value = String.valueOf(temp);
						break;
					case IConstantPoolConstant.CONSTANT_Utf8:
						value = "\"" + decodeStringValue(constantPoolEntry.getUtf8Value()) + "\"";//$NON-NLS-1$//$NON-NLS-2$
				}
				buffer.append(value);
				break;
			case IAnnotationComponentValue.ENUM_TAG:
				final char[] typeName = CharOperation.replaceOnCopy(annotationComponentValue.getEnumConstantTypeName(), '/', '.');
				final char[] constantName = annotationComponentValue.getEnumConstantName();
				buffer.append(returnClassName(Signature.toCharArray(typeName), '.', mode)).append('.').append(constantName);
				break;
			case IAnnotationComponentValue.CLASS_TAG:
				constantPoolEntry = annotationComponentValue.getClassInfo();
				final char[] className = CharOperation.replaceOnCopy(constantPoolEntry.getUtf8Value(), '/', '.');
				buffer.append(returnClassName(Signature.toCharArray(className), '.', mode));
				break;
			case IAnnotationComponentValue.ANNOTATION_TAG:
				IAnnotation annotation = annotationComponentValue.getAnnotationValue();
				disassembleAsModifier(annotation, buffer, lineSeparator, tabNumber + 1, mode);
				break;
			case IAnnotationComponentValue.ARRAY_TAG:
				final IAnnotationComponentValue[] annotationComponentValues = annotationComponentValue.getAnnotationComponentValues();
				buffer.append('{');
				for (int i = 0, max = annotationComponentValues.length; i < max; i++) {
					if (i > 0) {
						buffer.append(',');
					}
					disassembleAsModifier(annotationComponentValues[i], buffer, lineSeparator, tabNumber + 1, mode);
				}
				buffer.append('}');
		}
	}

	private void disassembleAsModifier(IAnnotationDefaultAttribute annotationDefaultAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		IAnnotationComponentValue componentValue = annotationDefaultAttribute.getMemberValue();
		disassembleAsModifier(componentValue, buffer, lineSeparator, tabNumber + 1, mode);
	}

	private void disassembleAsModifier(IRuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		IAnnotation[] annotations = runtimeInvisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			disassembleAsModifier(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(IParameterAnnotation[] parameterAnnotations, StringBuffer buffer, int index, String lineSeparator, int tabNumber, int mode) {
		if (parameterAnnotations.length > index) {
			disassembleAsModifier(parameterAnnotations[index], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(IParameterAnnotation parameterAnnotation, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		if (parameterAnnotation == null) return;
		IAnnotation[] annotations = parameterAnnotation.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			if (i > 0) {
				buffer.append(' ');
			}
			disassembleAsModifier(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleAsModifier(IRuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		IAnnotation[] annotations = runtimeVisibleAnnotationsAttribute.getAnnotations();
		for (int i = 0, max = annotations.length; i < max; i++) {
			if (i > 0) {
				writeNewLine(buffer, lineSeparator, tabNumber);
			}
			disassembleAsModifier(annotations[i], buffer, lineSeparator, tabNumber + 1, mode);
		}
	}

	private void disassembleTypeMembers(IClassFileReader classFileReader, char[] className, StringBuffer buffer, String lineSeparator, int tabNumber, int mode, boolean isEnum) {
		IFieldInfo[] fields = classFileReader.getFieldInfos();
		if (isEnum && checkMode(mode, WORKING_COPY)) {
			int index = 0;
			final int fieldsLength = fields.length;
			IMethodInfo[] methods = classFileReader.getMethodInfos();
			char[][] constructorArguments = getConstructorArgumentsForEnum(methods);
			enumConstantLoop: for (; index < fieldsLength; index++) {
				final IFieldInfo fieldInfo = fields[index];
				final int accessFlags = fieldInfo.getAccessFlags();
				if ((accessFlags & IModifierConstants.ACC_ENUM) != 0) {
					writeNewLine(buffer, lineSeparator, tabNumber);
					disassembleEnumConstants(fields[index], buffer, lineSeparator, tabNumber, constructorArguments, mode);
				} else {
					break enumConstantLoop;
				}
			}
			buffer.append(';');
			boolean foundSyntheticField = false;
			fieldLoop: for (; index < fieldsLength; index++) {
				if (!foundSyntheticField && CharOperation.equals(TypeConstants.SYNTHETIC_ENUM_VALUES, fields[index].getName())) {
					foundSyntheticField = true;
					continue fieldLoop;
				}
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(fields[index], buffer, lineSeparator, tabNumber, mode);
			}
			methodLoop: for (int i = 0, max = methods.length; i < max; i++) {
				final IMethodInfo methodInfo = methods[i];
				if (CharOperation.equals(methodInfo.getName(), TypeConstants.VALUES)) {
					final char[] descriptor = methodInfo.getDescriptor();
					CharOperation.replace(descriptor, '/', '.');
					if (Signature.getParameterCount(descriptor) == 0) {
						if (CharOperation.equals(returnClassName(Signature.getReturnType(descriptor), '.', mode),
								CharOperation.concat(new char[] {'[', 'L'}, className, new char[] {';'}))) {
							continue methodLoop;
						}
					}
				} else if (CharOperation.equals(methodInfo.getName(), TypeConstants.VALUEOF)) {
					final char[] descriptor = methodInfo.getDescriptor();
					CharOperation.replace(descriptor, '/', '.');
					final char[][] parameterTypes = Signature.getParameterTypes(descriptor);
					if (parameterTypes.length == 1
							&& CharOperation.equals(parameterTypes[0], "Ljava.lang.String;".toCharArray())) { //$NON-NLS-1$
						if (CharOperation.equals(returnClassName(Signature.getReturnType(descriptor), '.', mode),
								CharOperation.concat('L', className, ';'))) {
							continue methodLoop;
						}
					}
				} else if (methodInfo.isClinit() || methodInfo.isSynthetic()) {
					continue methodLoop;
				} else if (methodInfo.isConstructor()) {
					writeNewLine(buffer, lineSeparator, tabNumber);
					disassembleEnumConstructor(classFileReader, className, methodInfo, buffer, lineSeparator, tabNumber, mode);
				} else {
					writeNewLine(buffer, lineSeparator, tabNumber);
					disassemble(classFileReader, className, methodInfo, buffer, lineSeparator, tabNumber, mode);
				}
			}
		} else {
			for (int i = 0, max = fields.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(fields[i], buffer, lineSeparator, tabNumber, mode);
			}
			IMethodInfo[] methods = classFileReader.getMethodInfos();
			for (int i = 0, max = methods.length; i < max; i++) {
				writeNewLine(buffer, lineSeparator, tabNumber);
				disassemble(classFileReader, className, methods[i], buffer, lineSeparator, tabNumber, mode);
			}
		}
	}

	private char[][] getConstructorArgumentsForEnum(final IMethodInfo[] methods) {
		loop: for (int i = 0, max = methods.length; i < max; i++) {
			IMethodInfo methodInfo = methods[i];
			if (methodInfo.isConstructor()) {
				char[][] parameterTypes = Signature.getParameterTypes(methodInfo.getDescriptor());
				final int length = parameterTypes.length;
				if (length >= 2) {
					return CharOperation.subarray(parameterTypes, 2, length);
				}
			} else {
				continue loop;
			}
		}
		return null;
	}

	private final void dumpTab(int tabNumber, StringBuffer buffer) {
		for (int i = 0; i < tabNumber; i++) {
			buffer.append(Messages.disassembler_indentation);
		}
	}

	private final String dumpNewLineWithTabs(String lineSeparator, int tabNumber) {
		StringBuffer buffer = new StringBuffer();
		writeNewLine(buffer, lineSeparator, tabNumber);
		return String.valueOf(buffer);
	}

	/**
	 * @see org.eclipse.jdt.core.util.ClassFileBytesDisassembler#getDescription()
	 */
	@Override
	public String getDescription() {
		return Messages.disassembler_description;
	}

	private char[][] getParameterNames(char[] methodDescriptor, ICodeAttribute codeAttribute, IMethodParametersAttribute parametersAttribute, int accessFlags) {
		int paramCount = Signature.getParameterCount(methodDescriptor);
		char[][] parameterNames = new char[paramCount][];
		// check if the code attribute has debug info for this method
		if (parametersAttribute != null) {
			int parameterCount = parametersAttribute.getMethodParameterLength();
			for (int i = 0; i < paramCount; i++) {
				if (i < parameterCount && parametersAttribute.getParameterName(i) != null) {
					parameterNames[i] = parametersAttribute.getParameterName(i);
				} else {
					parameterNames[i] = Messages.disassembler_anonymousparametername.toCharArray();
				}
			}
		} else if (codeAttribute != null) {
				ILocalVariableAttribute localVariableAttribute = codeAttribute.getLocalVariableAttribute();
				if (localVariableAttribute != null) {
					ILocalVariableTableEntry[] entries = localVariableAttribute.getLocalVariableTable();
					final int startingIndex = (accessFlags & IModifierConstants.ACC_STATIC) != 0 ? 0 : 1;
					for (int i = 0; i < paramCount; i++) {
						ILocalVariableTableEntry searchedEntry = getEntryFor(getLocalIndex(startingIndex, i, methodDescriptor), entries);
						if (searchedEntry != null) {
							parameterNames[i] = searchedEntry.getName();
						} else {
							parameterNames[i] = CharOperation.concat(Messages.disassembler_parametername.toCharArray(), Integer.toString(i).toCharArray());
						}
					}
				} else {
					for (int i = 0; i < paramCount; i++) {
						parameterNames[i] = CharOperation.concat(Messages.disassembler_parametername.toCharArray(), Integer.toString(i).toCharArray());
					}
				}
		} else {
			for (int i = 0; i < paramCount; i++) {
				parameterNames[i] = CharOperation.concat(Messages.disassembler_parametername.toCharArray(), Integer.toString(i).toCharArray());
			}
		}
		return parameterNames;
	}

	private int getLocalIndex(final int startingSlot, final int index, final char[] methodDescriptor) {
		int slot = startingSlot;
		final char[][] types = Signature.getParameterTypes(methodDescriptor);
		for (int i = 0; i < index; i++) {
			final char[] type = types[i];
			switch(type.length) {
				case 1 :
					switch(type[0]) {
						case 'D' :
						case 'J' :
							slot += 2;
							break;
						default :
							slot++;
					}
					break;
				default :
					slot++;
			}
		}
		return slot;
	}

	private ILocalVariableTableEntry getEntryFor(final int index, final ILocalVariableTableEntry[] entries) {
		for (int i = 0, max = entries.length; i < max; i++) {
			ILocalVariableTableEntry entry = entries[i];
			if (index == entry.getIndex()) {
				return entry;
			}
		}
		return null;
	}

	private char[] getSignatureForField(char[] fieldDescriptor) {
		char[] newFieldDescriptor = CharOperation.replaceOnCopy(fieldDescriptor, '/', '.');
		newFieldDescriptor = CharOperation.replaceOnCopy(newFieldDescriptor, '$', '%');
		char[] fieldDescriptorSignature = Signature.toCharArray(newFieldDescriptor);
		CharOperation.replace(fieldDescriptorSignature, '%', '$');
		return fieldDescriptorSignature;
	}
	private char[] getSignatureForComponent(char[] componentDescriptor) {
		char[] newComponentDescriptor = CharOperation.replaceOnCopy(componentDescriptor, '/', '.');
		newComponentDescriptor = CharOperation.replaceOnCopy(newComponentDescriptor, '$', '%');
		char[] componentDescriptorSignature = Signature.toCharArray(newComponentDescriptor);
		CharOperation.replace(componentDescriptorSignature, '%', '$');
		return componentDescriptorSignature;
	}

	private boolean isDeprecated(IClassFileReader classFileReader) {
		IClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), IAttributeNamesConstants.DEPRECATED)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSynthetic(IClassFileReader classFileReader) {
		int flags = classFileReader.getAccessFlags();
		if ((flags & IModifierConstants.ACC_SYNTHETIC) != 0) {
			return true;
		}
		IClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (CharOperation.equals(attributes[i].getAttributeName(), IAttributeNamesConstants.SYNTHETIC)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkMode(int mode, int flag) {
		return (mode & flag) != 0;
	}

	private boolean isCompact(int mode) {
		return (mode & ClassFileBytesDisassembler.COMPACT) != 0;
	}

	private char[] returnClassName(char[] classInfoName, char separator, int mode) {
		if (classInfoName.length == 0) {
			return CharOperation.NO_CHAR;
		} else if (isCompact(mode)) {
			int lastIndexOfSlash = CharOperation.lastIndexOf(separator, classInfoName);
			if (lastIndexOfSlash != -1) {
				return CharOperation.subarray(classInfoName, lastIndexOfSlash + 1, classInfoName.length);
			}
		}
		return classInfoName;
	}

	private void writeNewLine(StringBuffer buffer, String lineSeparator, int tabNumber) {
		buffer.append(lineSeparator);
		dumpTab(tabNumber, buffer);
	}

	private String toTypePathString(int[][] typepath) {
		StringBuffer buffer = new StringBuffer();
		buffer.append('[');
		for (int i = 0, max = typepath.length; i < max; i++) {
			int[] typepathElement = typepath[i];
			if (i > 0) {
				buffer.append(',').append(' ');
			}
			switch (typepathElement[0]) {
				case IExtendedAnnotationConstants.TYPE_PATH_DEEPER_IN_ARRAY:
					buffer.append(Messages.disassembler_extendedannotation_typepath_array);
					break;
				case IExtendedAnnotationConstants.TYPE_PATH_DEEPER_IN_INNER_TYPE:
					buffer.append(Messages.disassembler_extendedannotation_typepath_innertype);
					break;
				case IExtendedAnnotationConstants.TYPE_PATH_ANNOTATION_ON_WILDCARD_BOUND:
					buffer.append(Messages.disassembler_extendedannotation_typepath_wildcard);
					break;
				case IExtendedAnnotationConstants.TYPE_PATH_TYPE_ARGUMENT_INDEX:
					buffer.append(
							Messages.bind(Messages.disassembler_extendedannotation_typepath_typeargument,
								new String[] {
									Integer.toString(typepathElement[1])
								}));
					break;
				default:
					throw new IllegalStateException("Unrecognized type_path_kind: "+typepathElement[0]); //$NON-NLS-1$
			}
		}
		buffer.append(']');
		return String.valueOf(buffer);
	}

}
