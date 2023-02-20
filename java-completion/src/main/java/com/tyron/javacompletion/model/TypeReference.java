package com.tyron.javacompletion.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** A reference to a type for lazy resolution. */
@AutoValue
public abstract class TypeReference implements TypeArgument {
    public static final TypeReference EMPTY_TYPE =
            TypeReference.builder()
                    .setFullName()
                    .setPrimitive(false)
                    .setArray(false)
                    .setTypeArguments(ImmutableList.of())
                    .build();
    public static final TypeReference JAVA_LANG_OBJECT =
            TypeReference.builder()
                    .setFullName("java", "lang", "Object")
                    .setPrimitive(false)
                    .setArray(false)
                    .setTypeArguments(ImmutableList.of())
                    .build();
    public static final TypeReference JAVA_LANG_ENUM =
            TypeReference.builder()
                    .setFullName("java", "lang", "Enum")
                    .setPrimitive(false)
                    .setArray(false)
                    .setTypeArguments(ImmutableList.of())
                    .build();
    public static final TypeReference VOID_TYPE = primitiveType("void");
    public static final TypeReference BYTE_TYPE = primitiveType("byte");
    public static final TypeReference CHAR_TYPE = primitiveType("char");
    public static final TypeReference DOUBLE_TYPE = primitiveType("double");
    public static final TypeReference FLOAT_TYPE = primitiveType("float");
    public static final TypeReference INT_TYPE = primitiveType("int");
    public static final TypeReference LONG_TYPE = primitiveType("long");
    public static final TypeReference SHORT_TYPE = primitiveType("short");
    public static final TypeReference BOOLEAN_TYPE = primitiveType("boolean");

    private static final Joiner JOINER = Joiner.on(".");

    protected abstract ImmutableList<String> getUnformalizedFullName();

    protected abstract SimpleType getSimpleType();

    public abstract Optional<ImmutableList<String>> getPackageName();

    public abstract Optional<ImmutableList<SimpleType>> getEnclosingClasses();

    public abstract boolean isArray();

    /**
     * ["org", "package", "EnclosingClass&lt;TypeArg1&gt;", "SimpleName"]
     *
     * <p>Note: type arguments after simple name is not returned for historical reasons.
     */
    public ImmutableList<String> getFullName() {
        if (getPackageName().isPresent()) {
            ImmutableList.Builder<String> builder =
                    new ImmutableList.Builder<String>().addAll(getPackageName().get());

            for (SimpleType enclosingClass : getEnclosingClasses().get()) {
                builder.add(
                        enclosingClass.getSimpleName() + typeArgumentString(enclosingClass.getTypeArguments()));
            }
            return builder.add(getSimpleName()).build();
        } else {
            return getUnformalizedFullName();
        }
    }

    /** ["org", "package", "EnclosingClass"] */
    public ImmutableList<String> getQualifiers() {
        if (getPackageName().isPresent()) {
            ImmutableList.Builder<String> builder =
                    new ImmutableList.Builder<String>().addAll(getPackageName().get());

            for (SimpleType enclosingClass : getEnclosingClasses().get()) {
                builder.add(enclosingClass.getSimpleName());
            }
            return builder.build();
        } else {
            ImmutableList<String> unformalizedFullName = getUnformalizedFullName();
            return unformalizedFullName.subList(0, unformalizedFullName.size() - 1);
        }
    }

    public boolean isPrimitive() {
        return getSimpleType().isPrimitive();
    }

    public String getSimpleName() {
        return getSimpleType().getSimpleName();
    }

    public ImmutableList<TypeArgument> getTypeArguments() {
        return getSimpleType().getTypeArguments();
    }

    @Override
    public Optional<TypeReference> applyTypeParameters(SolvedTypeParameters solvedTypeParameters) {
        if (getFullName().size() == 1 && getTypeArguments().isEmpty()) {
            // Only simple name, can be replaced with type parameters.
            Optional<SolvedType> solved = solvedTypeParameters.getTypeParameter(getSimpleName());
            if (solved.isPresent()) {
                return Optional.of(solved.get().toTypeReference());
            }
        }
        boolean hasChange = false;
        List<TypeArgument> newTypeArguments = new ArrayList<>();
        for (TypeArgument typeArgument : getTypeArguments()) {
            Optional<? extends TypeArgument> applied =
                    typeArgument.applyTypeParameters(solvedTypeParameters);
            TypeArgument newTypeArgument;
            if (applied.isPresent()) {
                hasChange = true;
                newTypeArgument = applied.get();
            } else {
                newTypeArgument = typeArgument;
            }
            newTypeArguments.add(newTypeArgument);
        }
        if (!hasChange) {
            return Optional.empty();
        }
        // TODO: apply to type parameters of the enclosing classes.
        return Optional.of(toBuilder().setTypeArguments(newTypeArguments).build());
    }

    public static Builder builder() {
        return new AutoValue_TypeReference.Builder()
                .setPackageName(Optional.empty())
                .setEnclosingClasses(Optional.empty());
    }

    public static Builder formalizedBuilder() {
        return new AutoValue_TypeReference.Builder().setUnformalizedFullName(ImmutableList.of());
    }

    protected abstract Builder autoToBuilder();

    public Builder toBuilder() {
        return autoToBuilder()
                .setPrimitive(isPrimitive())
                .setSimpleName(getSimpleName())
                .setTypeArguments(getTypeArguments());
    }

    @Override
    public String toString() {
        StringBuilder sb =
                new StringBuilder().append("TypeReference<").append(JOINER.join(getFullName()));
        sb.append(typeArgumentString(getTypeArguments()));
        if (isArray()) {
            sb.append("[]");
        }
        sb.append(">");
        return sb.toString();
    }

    private static CharSequence typeArgumentString(ImmutableList<TypeArgument> typeArguments) {
        if (typeArguments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        boolean first = true;
        for (TypeArgument typeArgument : typeArguments) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(typeArgument.toString());
        }
        sb.append(">");
        return sb;
    }

    @Override
    public String toDisplayString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getSimpleName());
        if (!getTypeArguments().isEmpty()) {
            sb.append("<");
            boolean isFirst = true;
            for (TypeArgument typeArgument : getTypeArguments()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(", ");
                }
                sb.append(typeArgument.toDisplayString());
            }
            sb.append(">");
        }
        if (isArray()) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static TypeReference primitiveType(String name) {
        return TypeReference.builder()
                .setArray(false)
                .setPrimitive(true)
                .setFullName(name)
                .setTypeArguments(ImmutableList.of())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        private final SimpleType.Builder simpleTypeBuilder = SimpleType.builder();

        public abstract Builder setArray(boolean isArray);

        public abstract Builder setPackageName(ImmutableList<String> packageName);

        public abstract Builder setPackageName(Optional<ImmutableList<String>> packageName);

        public Builder setPackageName(String... packageNames) {
            return this.setPackageName(ImmutableList.copyOf(packageNames));
        }

        public Builder setPackageName(Collection<String> packageNames) {
            return this.setPackageName(ImmutableList.copyOf(packageNames));
        }

        public abstract Builder setEnclosingClasses(ImmutableList<SimpleType> enclosingClassName);

        public abstract Builder setEnclosingClasses(
                Optional<ImmutableList<SimpleType>> enclosingClassName);

        public Builder setEnclosingClasses(Collection<SimpleType> enclosingClassName) {
            return setEnclosingClasses(ImmutableList.copyOf(enclosingClassName));
        }

        public Builder setEnclosingClasses(SimpleType... enclosingClassNames) {
            return setEnclosingClasses(ImmutableList.copyOf(enclosingClassNames));
        }

        protected abstract Builder setUnformalizedFullName(ImmutableList<String> fullName);

        public abstract Builder setSimpleType(SimpleType simpleType);

        public Builder setPrimitive(boolean isPrimitive) {
            simpleTypeBuilder.setPrimitive(isPrimitive);
            return this;
        }

        public Builder setSimpleName(String simpleName) {
            simpleTypeBuilder.setSimpleName(simpleName);
            return this;
        }

        public Builder setTypeArguments(ImmutableList<TypeArgument> typeArguments) {
            simpleTypeBuilder.setTypeArguments(typeArguments);
            return this;
        }

        public Builder setTypeArguments(Collection<TypeArgument> typeArguments) {
            return setTypeArguments(ImmutableList.copyOf(typeArguments));
        }

        public Builder setTypeArguments(TypeArgument... typeArguments) {
            return setTypeArguments(ImmutableList.copyOf(typeArguments));
        }

        public Builder setFullName(String... fullName) {
            return setFullName(ImmutableList.copyOf(fullName));
        }

        public Builder setFullName(Collection<String> fullName) {
            return setFullName(ImmutableList.copyOf(fullName));
        }

        public Builder setFullName(ImmutableList<String> fullName) {
            setUnformalizedFullName(fullName);
            if (fullName.isEmpty()) {
                setSimpleName("");
            } else {
                setSimpleName(fullName.get(fullName.size() - 1));
            }
            return this;
        }

        protected abstract TypeReference autoBuild();

        public TypeReference build() {
            return setSimpleType(simpleTypeBuilder.build()).autoBuild();
        }
    }
}