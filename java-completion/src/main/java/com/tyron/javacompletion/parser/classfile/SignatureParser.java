package com.tyron.javacompletion.parser.classfile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.tyron.javacompletion.model.SimpleType;
import com.tyron.javacompletion.model.TypeArgument;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;
import com.tyron.javacompletion.model.WildcardTypeArgument;

/**
 * A parser for the content of {@link AttributeInfo.Signature}.
 *
 * <p>Naming convension of this class:
 *
 * <ul>
 *   <li>{@code parseXxxContent} parses the Xxx structure without the leading character or leading
 *       identifier.
 *   <li>{@code parseYyy} parses the Yyy structure with the leading character.
 * </ul>
 */
public class SignatureParser {
    private static final ImmutableMap<Character, TypeReference> BASE_TYPE_MAP =
            new ImmutableMap.Builder<Character, TypeReference>()
                    .put('B', TypeReference.BYTE_TYPE)
                    .put('C', TypeReference.CHAR_TYPE)
                    .put('D', TypeReference.DOUBLE_TYPE)
                    .put('F', TypeReference.FLOAT_TYPE)
                    .put('I', TypeReference.INT_TYPE)
                    .put('J', TypeReference.LONG_TYPE)
                    .put('S', TypeReference.SHORT_TYPE)
                    .put('Z', TypeReference.BOOLEAN_TYPE)
                    .build();

    private final SignatureLexer lexer;
    private final ImmutableMap<String, InnerClassEntry> innerClassMap;

    public SignatureParser(String content, Map<String, InnerClassEntry> innerClassMap) {
        this.lexer = new SignatureLexer(content);
        this.innerClassMap = ImmutableMap.copyOf(innerClassMap);
    }

    /** [TypeParameters] SuperclassSignature {SuperinterfaceSignature} */
    public ClassSignature parseClassSignature() {
        ClassSignature.Builder builder = ClassSignature.builder();
        char ch = lexer.peekChar();
        if (ch == '<') {
            builder.setTypeParameters(parseTypeParameters());
        }
        builder.setSuperClass(parseClassTypeSignature());
        while (lexer.hasRemainingContent()) {
            builder.addInterface(parseClassTypeSignature());
        }
        return builder.build();
    }

    /** [TypeParameters] ( {JavaTypeSignature} ) Result {ThrowsSignature} */
    public MethodSignature parseMethodSignature() {
        MethodSignature.Builder builder = MethodSignature.builder();
        char ch = lexer.peekChar();

        // Parse type parameters
        if (ch == '<') {
            builder.setTypeParameters(parseTypeParameters());
            ch = lexer.peekChar();
        } else {
            builder.setTypeParameters(ImmutableList.of());
        }

        // Parse parameters
        checkState(ch == '(', "Method parameters do not start with '(': %s", lexer.remainingContent());
        lexer.skipChar();
        ch = lexer.peekChar();
        while (ch != ')') {
            builder.addParameter(parseJavaTypeSignature());
            ch = lexer.peekChar();
        }
        lexer.skipChar();

        // Parse result
        ch = lexer.peekChar();
        if (ch == 'V') {
            builder.setResult(TypeReference.VOID_TYPE);
            lexer.skipChar();
        } else {
            builder.setResult(parseJavaTypeSignature());
        }

        // Parse throws signatures
        ch = lexer.peekChar();
        while (ch == '^') {
            lexer.skipChar();
            ch = lexer.nextChar();
            switch (ch) {
                case 'L':
                    builder.addThrowsSignature(parseClassTypeSignatureContent(false /* endsWithEos */));
                    break;
                case 'T':
                    builder.addThrowsSignature(parseTypeVariableSignatureContent());
                    break;
                default:
                    throw new IllegalStateException(
                            "Unknown leading character for method throws signature: "
                                    + ch
                                    + lexer.remainingContent());
            }
            ch = lexer.peekChar();
        }
        return builder.build();
    }

    public TypeReference parseFieldReference() {
        return parseReferenceTypeSignature();
    }

    /** &lt; TypeParameter {TypeParameter} &gt; */
    @VisibleForTesting
    ImmutableList<TypeParameter> parseTypeParameters() {
        char ch = lexer.nextChar();
        checkState(
                ch == '<', "TypeParameters do not start with '<': %s%s", ch, lexer.remainingContent());

        ImmutableList.Builder<TypeParameter> builder = new ImmutableList.Builder<>();
        while (true) {
            builder.add(parseTypeParameter());
            ch = lexer.peekChar();
            if (ch == '>') {
                lexer.skipChar();
                break;
            }
        }
        return builder.build();
    }

    /**
     * Identifier ClassBound {InterfaceBound}
     *
     * <ul>
     *   <li>ClassBound: : [ReferenceTypeSignature]
     *   <li>InterfaceBound: : ReferenceTypeSignature
     * </ul>
     */
    private TypeParameter parseTypeParameter() {
        String identifier = lexer.nextIdentifier();
        ImmutableList.Builder<TypeReference> boundsBuilder = new ImmutableList.Builder<>();

        // ClassBound
        char ch = lexer.nextChar();
        checkState(ch == ':', "ClassBound does not start with ':': %s", lexer.remainingContent());
        ch = lexer.peekChar();
        if (isReferenceTypeSignatureLeadingChar(ch)) {
            boundsBuilder.add(parseReferenceTypeSignature());
        }

        // InterfaceBounds
        ch = lexer.peekChar();
        while (ch == ':') {
            lexer.skipChar();
            boundsBuilder.add(parseReferenceTypeSignature());
            ch = lexer.peekChar();
        }
        return TypeParameter.create(identifier, boundsBuilder.build());
    }

    /** L [PackageSpecifier] SimpleClassTypeSignature {ClassTypeSignatureSuffix} ; */
    @VisibleForTesting
    TypeReference parseClassTypeSignature() {
        char ch = lexer.nextChar();
        checkState(
                ch == 'L',
                "ClassTypeSignature does not start with 'L': %s%s",
                ch,
                lexer.remainingContent());
        return parseClassTypeSignatureContent(false /* endsWithEos */);
    }

    /**
     * @param endsWithEos whether the signature should end with END_OF_SIGNATURE. If it's false, it
     *     should end with a semi-colon.
     */
    private TypeReference parseClassTypeSignatureContent(boolean endsWithEos) {
        ImmutableList.Builder<String> packageNameBuilder = new ImmutableList.Builder<>();
        Map<String, ImmutableList<TypeArgument>> binaryNameToTypeArguments = new HashMap<>();

        StringBuilder binaryNameBuilder = new StringBuilder();
        String identifier = lexer.nextIdentifier();
        while (lexer.peekChar() == '/') {
            lexer.skipChar();
            packageNameBuilder.add(identifier);
            binaryNameBuilder.append(identifier).append('/');
            identifier = lexer.nextIdentifier();
        }

        SimpleType simpleClassTypeSignature = parseSimpleClassTypeSignatureContent(identifier);
        while (lexer.peekChar() == '.') {
            lexer.nextChar();
            // All . should be converted to $ in the binary name.
            binaryNameBuilder.append(simpleClassTypeSignature.getSimpleName());
            if (!simpleClassTypeSignature.getTypeArguments().isEmpty()) {
                binaryNameToTypeArguments.put(
                        binaryNameBuilder.toString(), simpleClassTypeSignature.getTypeArguments());
            }
            binaryNameBuilder.append('$');
            simpleClassTypeSignature = parseSimpleClassTypeSignature();
        }
        binaryNameBuilder.append(simpleClassTypeSignature.getSimpleName());
        if (endsWithEos) {
            checkState(lexer.peekChar() == SignatureLexer.END_OF_SIGNATURE);
        } else {
            char ch = lexer.nextChar();
            checkState(
                    ch == ';',
                    "ClassTypeSignature does not end with ';': %s%s",
                    ch,
                    lexer.remainingContent());
        }

        // When the binary name is in the form of package/to/Foo$Bar, we don't know if there is
        // an inner class Bar defined in the class Foo, or there is a class named Foo$Bar. We need
        // to use the innerClassMap to get the actual simple names of the enclosing classes.
        String binaryName = binaryNameBuilder.toString();
        ImmutableList.Builder<SimpleType> enclosingClassesBuilder = new ImmutableList.Builder<>();
        boolean isLast = true;
        boolean hasOuterClass = true;
        SimpleType lastSimpleClassType = null;
        while (hasOuterClass) {
            String originalBinaryName = binaryName;
            String innerName;
            if (innerClassMap.containsKey(originalBinaryName)) {
                InnerClassEntry innerClassEntry = innerClassMap.get(originalBinaryName);
                binaryName = innerClassEntry.getOuterClassName();
                innerName = innerClassEntry.getInnerName();
            } else {
                hasOuterClass = false;
                int packagePos = originalBinaryName.lastIndexOf('/');
                if (packagePos >= 0) {
                    innerName = originalBinaryName.substring(packagePos + 1);
                } else {
                    innerName = originalBinaryName;
                }
            }
            if (isLast) {
                isLast = false;
                lastSimpleClassType =
                        SimpleType.builder()
                                .setSimpleName(innerName)
                                .setTypeArguments(simpleClassTypeSignature.getTypeArguments())
                                .setPrimitive(false)
                                .build();
            } else {
                ImmutableList<TypeArgument> typeArguments;
                if (binaryNameToTypeArguments.containsKey(originalBinaryName)) {
                    typeArguments = binaryNameToTypeArguments.get(originalBinaryName);
                } else {
                    typeArguments = ImmutableList.of();
                }
                enclosingClassesBuilder.add(
                        SimpleType.builder()
                                .setSimpleName(innerName)
                                .setTypeArguments(typeArguments)
                                .setPrimitive(false)
                                .build());
            }
        }

        checkNotNull(
                lastSimpleClassType, "No lastSimpleClassType for %s", binaryNameBuilder.toString());

        return TypeReference.formalizedBuilder()
                .setPackageName(packageNameBuilder.build())
                .setEnclosingClasses(Lists.reverse(enclosingClassesBuilder.build()))
                .setSimpleName(lastSimpleClassType.getSimpleName())
                .setTypeArguments(lastSimpleClassType.getTypeArguments())
                .setPrimitive(false)
                .setArray(false)
                .build();
    }

    /**
     * Parses class and interface binary names.
     *
     * <p>See JVMS 4.2.1: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2.1
     */
    public TypeReference parseClassBinaryName() {
        // The binary name is a subset of the class type signature, with the exception that it doesn't
        // have the leading "L" and ends without a semicolon.
        return parseClassTypeSignatureContent(true /* endsWithEos */);
    }

    private TypeReference parseTypeVariableSignatureContent() {
        String identifier = lexer.nextIdentifier();
        char ch = lexer.nextChar();
        checkState(ch == ';', "TypeVariable does not end with ';'", ch, lexer.remainingContent());
        return TypeReference.builder()
                .setArray(false)
                .setFullName(identifier)
                .setPrimitive(false)
                .setTypeArguments(ImmutableList.of())
                .build();
    }

    /** Identifier [TypeArguments] */
    private SimpleType parseSimpleClassTypeSignature() {
        return parseSimpleClassTypeSignatureContent(lexer.nextIdentifier());
    }

    private SimpleType parseSimpleClassTypeSignatureContent(String identifier) {
        SimpleType.Builder builder = SimpleType.builder().setPrimitive(false).setSimpleName(identifier);
        char ch = lexer.peekChar();
        if (ch == '<') {
            builder.setTypeArguments(parseTypeArguments());
        } else {
            builder.setTypeArguments(ImmutableList.of());
        }
        return builder.build();
    }

    /** &lt; TypeArgument {TypeArgument} &gt; */
    private ImmutableList<TypeArgument> parseTypeArguments() {
        char ch = lexer.nextChar();
        checkState(ch == '<', "TypeArguments do no start with '<': %s%s", ch, lexer.remainingContent());
        ImmutableList.Builder<TypeArgument> builder = new ImmutableList.Builder<>();
        while (true) {
            builder.add(parseTypeArgument());
            ch = lexer.peekChar();
            if (ch == '>') {
                lexer.skipChar();
                break;
            }
        }
        return builder.build();
    }

    /**
     *
     *
     * <ul>
     *   <li>[WildcardIndicator] ReferenceTypeSignature
     *   <li>*
     * </ul>
     */
    private TypeArgument parseTypeArgument() {
        char ch = lexer.peekChar();
        switch (ch) {
            case '*':
                lexer.skipChar();
                return WildcardTypeArgument.create(Optional.empty() /* bound */);
            case '+':
                lexer.skipChar();
                return WildcardTypeArgument.create(
                        Optional.of(
                                WildcardTypeArgument.Bound.create(
                                        WildcardTypeArgument.Bound.Kind.EXTENDS, parseReferenceTypeSignature())));
            case '-':
                lexer.skipChar();
                return WildcardTypeArgument.create(
                        Optional.of(
                                WildcardTypeArgument.Bound.create(
                                        WildcardTypeArgument.Bound.Kind.SUPER, parseReferenceTypeSignature())));
            default:
                return parseReferenceTypeSignature();
        }
    }

    /**
     *
     *
     * <ul>
     *   <li>ClassTypeSignature
     *   <li>TypeVariableSignature
     *   <li>ArrayTypeSignature
     * </ul>
     *
     * <p>TypeVariableSignature: T Identifier ;
     *
     * <p>ArrayTypeSignature: [ JavaTypeSignature
     */
    @VisibleForTesting
    TypeReference parseReferenceTypeSignature() {
        char ch = lexer.nextChar();
        switch (ch) {
            case 'L':
                return parseClassTypeSignatureContent(false /* endsWithEos */);
            case 'T':
                return parseTypeVariableSignatureContent();
            case '[':
                // TODO: support multi-dimensional array.
                return parseJavaTypeSignature().toBuilder().setArray(true).build();
            default:
                throw new IllegalStateException(
                        "Invalid referenceTypeSignature: " + ch + lexer.remainingContent());
        }
    }

    private boolean isReferenceTypeSignatureLeadingChar(char ch) {
        return ch == 'L' || ch == 'T' || ch == '[';
    }

    /**
     *
     *
     * <ul>
     *   <li>ReferenceTypeSignature
     *   <li>BaseType
     * </ul>
     */
    @VisibleForTesting
    TypeReference parseJavaTypeSignature() {
        char ch = lexer.peekChar();
        if (BASE_TYPE_MAP.containsKey(ch)) {
            lexer.skipChar();
            return BASE_TYPE_MAP.get(ch);
        }

        return parseReferenceTypeSignature();
    }
}