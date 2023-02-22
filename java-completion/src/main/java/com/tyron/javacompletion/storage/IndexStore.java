/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.javacompletion.storage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import androidx.annotation.Nullable;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.EntityWithContext;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.MethodEntity;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.model.PrimitiveEntity;
import com.tyron.javacompletion.model.SolvedTypeParameters;
import com.tyron.javacompletion.model.TypeArgument;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;
import com.tyron.javacompletion.model.VariableEntity;
import com.tyron.javacompletion.model.WildcardTypeArgument;
import com.tyron.javacompletion.typesolver.TypeSolver;

/** Storing and loading indexed Java modules from storage. */
public class IndexStore {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final String QUALIFIER_SEPARATOR = "\\.";
    private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
    private static final Range<Integer> EMPTY_RANGE = Range.closedOpen(0, 0);
    private static final ImmutableList<String> EMPTY_QUALIFIERS = ImmutableList.of();

    private final Gson gson = new GsonBuilder().create();
    private final TypeSolver typeSolver = new TypeSolver();

    private final Map<Entity, Entity> visitedEntities = new HashMap<>();

    private Module module;

    public void writeModuleToFile(Module module, Path filePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, UTF_8)) {
            gson.toJson(serializeModule(module), writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Module readModuleFromFile(Path filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath), UTF_8);
        return deserializeModule(gson.fromJson(content, SerializedModule.class));
    }

    public Module readModule(Reader reader) {
        return deserializeModule(gson.fromJson(reader, SerializedModule.class));
    }

    @VisibleForTesting
    SerializedModule serializeModule(Module module) {
        this.module = module;
        SerializedModule ret = new SerializedModule();
        ret.files =
                module.getAllFiles().stream()
                        .collect(
                                Collectors.groupingBy(
                                        fileScope -> QUALIFIER_JOINER.join(fileScope.getPackageQualifiers())))
                        .entrySet()
                        .stream()
                        .map(entry -> serializeFileScopes(entry.getKey(), entry.getValue()))
                        .sorted()
                        .collect(Collectors.toList());
        this.module = null;
        return ret;
    }

    @VisibleForTesting
    Module deserializeModule(SerializedModule serializedModule) {
        checkNotNull(serializedModule.files, "serializedModule.files");
        Module module = new Module();
        for (SerializedFileScope file : serializedModule.files) {
            module.addOrReplaceFileScope(deserializeFileScope(file));
        }
        return module;
    }

    private SerializedFileScope serializeFileScopes(String packageName, List<FileScope> fileScopes) {
        SerializedFileScope ret = new SerializedFileScope();
        ret.packageName = packageName;
        ret.entities =
                fileScopes.stream()
                        .flatMap(fileScope -> fileScope.getMemberEntities().values().stream())
                        .map(this::serializeEntity)
                        .sorted()
                        .collect(Collectors.toList());
        return ret;
    }

    private FileScope deserializeFileScope(SerializedFileScope serializedFileScope) {
        checkNotNull(serializedFileScope.packageName, "serializedFileScope.packageName");
        checkNotNull(serializedFileScope.entities, "serializedFileScope.entities");
        List<String> packageQualifiers =
                ImmutableList.copyOf(serializedFileScope.packageName.split(QUALIFIER_SEPARATOR));
        FileScope fileScope = FileScope.createFromTypeIndex(packageQualifiers);
        for (SerializedEntity childEntity : serializedFileScope.entities) {
            fileScope.addEntity(deserializeEntity(childEntity, packageQualifiers, fileScope));
        }
        return fileScope;
    }

    private SerializedEntity serializeEntity(Entity entity) {
        SerializedEntity ret;
        if (entity instanceof ClassEntity) {
            ret = serializeClassEntity((ClassEntity) entity);
        } else if (entity instanceof MethodEntity) {
            ret = serializeMethodEntity((MethodEntity) entity);
        } else if (entity instanceof VariableEntity) {
            ret = serializeVariableEntity((VariableEntity) entity);
        } else {
            logger.warning("Unknown Entity: %s", entity);
            ret = new SerializedEntity();
        }
        ret.kind = entity.getKind().name();
        ret.simpleName = entity.getSimpleName();
        ret.isStatic = entity.isStatic();
        ret.javadoc = entity.getJavadoc().orElse(null);
        return ret;
    }

    private SerializedEntity serializeClassEntity(ClassEntity entity) {
        SerializedEntity ret = new SerializedEntity();
        ret.members =
                entity.getConstructors().stream()
                        .map(constructor -> serializeEntity(constructor))
                        .collect(Collectors.toList());
        ret.members.addAll(
                entity.getMemberEntities().values().stream()
                        .map(
                                childEntity -> {
                                    if (visitedEntities.containsKey(childEntity)) {
                                        throw new RuntimeException(
                                                "Entity "
                                                        + childEntity
                                                        + "Has already been added by "
                                                        + visitedEntities.get(childEntity)
                                                        + ", it's being added by "
                                                        + entity
                                                        + " again");
                                    }
                                    visitedEntities.put(childEntity, entity);
                                    return serializeEntity(childEntity);
                                })
                        .sorted()
                        .collect(Collectors.toList()));
        if (entity.getSuperClass().isPresent()) {
            ret.superClass =
                    serializeTypeReference(entity.getSuperClass().get(), entity.getParentScope().get());
        }
        ret.interfaces =
                entity.getInterfaces().stream()
                        .map(t -> serializeTypeReference(t, entity.getParentScope().get()))
                        .sorted()
                        .collect(Collectors.toList());
        if (!entity.getTypeParameters().isEmpty()) {
            ret.typeParameters =
                    entity.getTypeParameters().stream()
                            .map(t -> serializeTypeParameter(t, entity))
                            .collect(Collectors.toList());
        }
        return ret;
    }

    private SerializedEntity serializeMethodEntity(MethodEntity entity) {
        SerializedEntity ret = new SerializedEntity();
        ret.parameters =
                entity.getParameters().stream()
                        .map(param -> serializeEntity(param))
                        .collect(Collectors.toList());
        if (!entity.getSimpleName().equals("<init>")) {
            ret.type = serializeTypeReference(entity.getReturnType(), entity.getScope());
        }
        if (!entity.getTypeParameters().isEmpty()) {
            ret.typeParameters =
                    entity.getTypeParameters().stream()
                            .map(t -> serializeTypeParameter(t, entity))
                            .collect(Collectors.toList());
        }
        return ret;
    }

    private SerializedEntity serializeVariableEntity(VariableEntity entity) {
        SerializedEntity ret = new SerializedEntity();
        ret.type = serializeTypeReference(entity.getType(), entity.getParentScope().get());
        return ret;
    }

    private Entity deserializeEntity(
            SerializedEntity serializedEntity, List<String> qualifiers, EntityScope parentScope) {
        checkNotNull(serializedEntity.kind, "serializedEntity.kind is null");
        checkNotNull(serializedEntity.simpleName, "serializedEntity.simpleName is null");

        Entity.Kind entityKind = Entity.Kind.valueOf(serializedEntity.kind);
        if (entityKind == null) {
            throw new IllegalArgumentException("Unknown entity kind: " + serializedEntity.kind);
        }

        if (VariableEntity.ALLOWED_KINDS.contains(entityKind)) {
            return deserializeVariableEntity(serializedEntity, entityKind, qualifiers, parentScope);
        } else if (ClassEntity.ALLOWED_KINDS.contains(entityKind)) {
            return deserializeClassEntity(serializedEntity, entityKind, qualifiers, parentScope);
        } else if (entityKind == Entity.Kind.METHOD) {
            checkArgument(
                    parentScope instanceof ClassEntity, "parentScope must be ClassEntity for methods.");
            return deserializeMethodEntity(
                    serializedEntity, entityKind, qualifiers, (ClassEntity) parentScope);
        } else {
            throw new UnsupportedOperationException("Unsupported entity kind " + entityKind);
        }
    }

    private ClassEntity deserializeClassEntity(
            SerializedEntity serializedEntity,
            Entity.Kind entityKind,
            List<String> qualifiers,
            EntityScope parentScope) {
        Optional<TypeReference> superClass =
                serializedEntity.superClass == null
                        ? Optional.empty()
                        : Optional.of(deserializeTypeReference(serializedEntity.superClass));
        ImmutableList<TypeReference> interfaces =
                deserializeTypeReferences(serializedEntity.interfaces);
        ImmutableList<TypeParameter> typeParameters =
                deserializeTypeParameters(serializedEntity.typeParameters);
        ClassEntity classEntity =
                new ClassEntity(
                        serializedEntity.simpleName,
                        entityKind,
                        qualifiers,
                        serializedEntity.isStatic,
                        parentScope,
                        superClass,
                        interfaces,
                        typeParameters,
                        Optional.ofNullable(serializedEntity.javadoc),
                        EMPTY_RANGE,
                        EMPTY_RANGE);
        if (serializedEntity.members != null) {
            List<String> childQualifiers =
                    new ImmutableList.Builder<String>()
                            .addAll(qualifiers)
                            .add(serializedEntity.simpleName)
                            .build();
            for (SerializedEntity childEntity : serializedEntity.members) {
                classEntity.addEntity(deserializeEntity(childEntity, childQualifiers, classEntity));
            }
        }
        return classEntity;
    }

    private MethodEntity deserializeMethodEntity(
            SerializedEntity serializedEntity,
            Entity.Kind entityKind,
            List<String> qualifiers,
            ClassEntity classEntity) {
        TypeReference returnType =
                serializedEntity.type == null
                        ? TypeReference.EMPTY_TYPE
                        : deserializeTypeReference(serializedEntity.type);
        List<VariableEntity> parameters;
        if (serializedEntity.parameters == null) {
            parameters = ImmutableList.of();
        } else {
            parameters =
                    serializedEntity.parameters.stream()
                            .map(
                                    p ->
                                            deserializeVariableEntity(
                                                    p, Entity.Kind.VARIABLE, EMPTY_QUALIFIERS, classEntity))
                            .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        }
        ImmutableList<TypeParameter> typeParameters =
                deserializeTypeParameters(serializedEntity.typeParameters);
        return new MethodEntity(
                serializedEntity.simpleName,
                qualifiers,
                serializedEntity.isStatic,
                returnType,
                parameters,
                typeParameters,
                classEntity,
                Optional.ofNullable(serializedEntity.javadoc),
                EMPTY_RANGE,
                EMPTY_RANGE);
    }

    private VariableEntity deserializeVariableEntity(
            SerializedEntity serializedEntity,
            Entity.Kind entityKind,
            List<String> qualifiers,
            EntityScope parentScope) {
        TypeReference type =
                serializedEntity.type == null
                        ? TypeReference.EMPTY_TYPE
                        : deserializeTypeReference(serializedEntity.type);
        return new VariableEntity(
                serializedEntity.simpleName,
                entityKind,
                qualifiers,
                serializedEntity.isStatic,
                type,
                parentScope,
                Optional.ofNullable(serializedEntity.javadoc),
                EMPTY_RANGE,
                EMPTY_RANGE);
    }

    private SerializedType serializeTypeReference(TypeReference type, EntityScope baseScope) {
        SerializedType ret = new SerializedType();
        Optional<EntityWithContext> optionalEntityWithContext;
        TypeReference typeToSolve = type;
        if (!type.getTypeArguments().isEmpty()) {
            // Use raw type to solve the full name, since the type arguments will be
            // solved by serializeTypeArgument().
            typeToSolve = type.toBuilder().setTypeArguments(ImmutableList.of()).build();
        }
        try {
            SolvedTypeParameters typeParametersFromScope =
                    typeSolver.solveTypeParametersFromScope(baseScope, module);
            if (type.getFullName().size() == 1
                    && typeParametersFromScope.getTypeParameter(type.getSimpleName()).isPresent()) {
                // The type name is a type variable defined by a type parameter in the
                // enclosing scopes. It should not be solved here.
                optionalEntityWithContext = Optional.empty();
            } else {
                optionalEntityWithContext =
                        typeSolver
                                .solve(type, typeParametersFromScope, baseScope, module)
                                .map(solvedType -> EntityWithContext.from(solvedType).build());
            }
        } catch (Throwable t) {
            logger.warning(t, "Error on solving type %s in %s", type, baseScope);
            optionalEntityWithContext = Optional.empty();
        }
        if (optionalEntityWithContext.isPresent()
                && Objects.equals(
                optionalEntityWithContext.get().getEntity().getSimpleName(),
                typeToSolve.getSimpleName())) {
            EntityWithContext entityWithContext = optionalEntityWithContext.get();
            boolean isArray = entityWithContext.getArrayLevel() > 0;
            ret.fullName = entityWithContext.getEntity().getQualifiedName();
        } else {
            ret.fullName = QUALIFIER_JOINER.join(type.getFullName());
        }
        ret.isArray = type.isArray();

        if (!type.getTypeArguments().isEmpty()) {
            ret.typeArguments =
                    type.getTypeArguments().stream()
                            .map(typeArgument -> serializeTypeArgument(typeArgument, baseScope))
                            .collect(Collectors.toList());
        }
        return ret;
    }

    private ImmutableList<TypeReference> deserializeTypeReferences(
            @Nullable List<SerializedType> types) {
        if (types == null) {
            return ImmutableList.of();
        }
        return types.stream()
                .map(t -> deserializeTypeReference(t))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    private TypeReference deserializeTypeReference(SerializedType type) {
        String fullName = type.fullName != null ? type.fullName : "";
        List<TypeArgument> typeArguments = ImmutableList.of();

        if (type.typeArguments != null && !type.typeArguments.isEmpty()) {
            try {
                typeArguments =
                        type.typeArguments.stream()
                                .map(this::deserializeTypeArgument)
                                .collect(Collectors.toList());
            } catch (Exception e) {
                logger.severe(
                        "Failed to deserialize type arguments %s for %s",
                        Collections.singletonList(type.typeArguments), type.fullName);
            }
        }

        TypeReference ret =
                TypeReference.builder()
                        .setFullName(type.fullName.split(QUALIFIER_SEPARATOR))
                        .setPrimitive(PrimitiveEntity.isPrimitive(type.fullName))
                        .setArray(type.isArray)
                        .setTypeArguments(typeArguments)
                        .build();
        return ret;
    }

    private SerializedTypeArgument serializeTypeArgument(
            TypeArgument typeArgument, EntityScope baseScope) {
        SerializedTypeArgument ret = new SerializedTypeArgument();
        if (typeArgument instanceof TypeReference) {
            ret.kind = SerializedTypeArgumentKind.EXPLICIT;
            ret.explicitType = serializeTypeReference((TypeReference) typeArgument, baseScope);
        } else if (typeArgument instanceof WildcardTypeArgument) {
            WildcardTypeArgument wildcardTypeArgument = (WildcardTypeArgument) typeArgument;
            if (wildcardTypeArgument.getBound().isPresent()) {
                WildcardTypeArgument.Bound.Kind boundKind = wildcardTypeArgument.getBound().get().getKind();
                switch (boundKind) {
                    case SUPER:
                        ret.kind = SerializedTypeArgumentKind.WILDCARD_SUPER;
                        break;
                    case EXTENDS:
                        ret.kind = SerializedTypeArgumentKind.WILDCARD_EXTENDS;
                        break;
                }
                ret.bound =
                        serializeTypeReference(
                                wildcardTypeArgument.getBound().get().getTypeReference(), baseScope);
            } else {
                ret.kind = SerializedTypeArgumentKind.WILDCARD_UNBOUNDED;
            }
        } else {
            throw new RuntimeException("Unknown type argument " + typeArgument);
        }
        return ret;
    }

    private TypeArgument deserializeTypeArgument(SerializedTypeArgument typeArgument) {
        switch (typeArgument.kind) {
            case EXPLICIT:
                checkNotNull(
                        typeArgument.explicitType,
                        "Type Argument with kind %s should have explicit type set",
                        typeArgument.kind);
                return deserializeTypeReference(typeArgument.explicitType);
            case WILDCARD_UNBOUNDED:
                return WildcardTypeArgument.create(Optional.empty());
            case WILDCARD_SUPER:
            case WILDCARD_EXTENDS:
                checkNotNull(
                        typeArgument.bound,
                        "Type Argument with kind %s should have bound set",
                        typeArgument.kind);
            {
                WildcardTypeArgument.Bound.Kind boundKind =
                        typeArgument.kind == SerializedTypeArgumentKind.WILDCARD_SUPER
                                ? WildcardTypeArgument.Bound.Kind.SUPER
                                : WildcardTypeArgument.Bound.Kind.EXTENDS;
                WildcardTypeArgument.Bound bound =
                        WildcardTypeArgument.Bound.create(
                                boundKind, deserializeTypeReference(typeArgument.bound));
                return WildcardTypeArgument.create(Optional.of(bound));
            }
            default:
                throw new RuntimeException("Unknown type argument " + typeArgument);
        }
    }

    private SerializedTypeParameter serializeTypeParameter(
            TypeParameter typeParameter, EntityScope entityScope) {
        SerializedTypeParameter ret = new SerializedTypeParameter();
        ret.name = typeParameter.getName();
        if (!typeParameter.getExtendBounds().isEmpty()) {
            ret.bounds =
                    typeParameter.getExtendBounds().stream()
                            .map(bound -> serializeTypeReference(bound, entityScope))
                            .collect(Collectors.toList());
        }
        return ret;
    }

    private ImmutableList<TypeParameter> deserializeTypeParameters(
            @Nullable List<SerializedTypeParameter> typeParameters) {
        if (typeParameters == null) {
            return ImmutableList.of();
        }
        return typeParameters.stream()
                .map(tp -> deserializeTypeParameter(tp))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    private TypeParameter deserializeTypeParameter(SerializedTypeParameter typeParameter) {
        return TypeParameter.create(
                typeParameter.name, deserializeTypeReferences(typeParameter.bounds));
    }

    private static <E extends Comparable<E>> int compareLists(
            @Nullable List<E> lhs, @Nullable List<E> rhs) {
        if (lhs == rhs) {
            return 0;
        }
        if (lhs == null && rhs != null) {
            return -1;
        }
        if (rhs == null) {
            return 1;
        }

        int ret = Integer.compare(lhs.size(), rhs.size());
        if (ret != 0) {
            return ret;
        }

        for (int i = 0; i < lhs.size(); i++) {
            ret = Objects.compare(lhs.get(i), rhs.get(i), Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }
        }

        return 0;
    }

    @VisibleForTesting
    static class SerializedModule {
        private List<SerializedFileScope> files;
    }

    private static class SerializedFileScope implements Comparable<SerializedFileScope> {
        private String packageName;
        private List<SerializedEntity> entities;

        @Override
        public int compareTo(SerializedFileScope other) {
            if (this == other) {
                return 0;
            }

            int ret = Objects.compare(this.packageName, other.packageName, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            return compareLists(this.entities, other.entities);
        }
    }

    private static class SerializedEntity implements Comparable<SerializedEntity> {
        private String kind;
        private String simpleName;
        private boolean isStatic;
        private List<SerializedEntity> members;
        private List<SerializedEntity> parameters;
        private SerializedType type;
        private SerializedType superClass;
        private List<SerializedType> interfaces;
        private List<SerializedTypeParameter> typeParameters;
        private String javadoc;

        @Override
        public int compareTo(SerializedEntity other) {
            if (this == other) {
                return 0;
            }
            int ret = Objects.compare(this.simpleName, other.simpleName, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            ret = Objects.compare(this.kind, other.kind, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            ret = compareLists(this.members, other.members);
            if (ret != 0) {
                return ret;
            }

            ret = compareLists(this.parameters, other.parameters);
            if (ret != 0) {
                return ret;
            }

            ret = Objects.compare(this.type, other.type, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            ret =
                    Objects.compare(
                            this.javadoc, other.javadoc, Comparator.nullsLast(Comparator.naturalOrder()));
            if (ret != 0) {
                return ret;
            }

            ret = Objects.compare(this.superClass, other.superClass, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            ret = compareLists(this.interfaces, other.interfaces);
            if (ret != 0) {
                return ret;
            }

            return compareLists(this.typeParameters, other.typeParameters);
        }
    }

    private static class SerializedType implements Comparable<SerializedType> {
        private String fullName;
        private boolean isArray;
        private List<SerializedTypeArgument> typeArguments;

        @Override
        public int compareTo(SerializedType other) {
            if (this == other) {
                return 0;
            }
            int ret = Objects.compare(this.fullName, other.fullName, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            ret = Boolean.compare(this.isArray, other.isArray);
            if (ret != 0) {
                return ret;
            }

            return compareLists(this.typeArguments, other.typeArguments);
        }
    }

    private static class SerializedTypeArgument implements Comparable<SerializedTypeArgument> {
        private SerializedTypeArgumentKind kind;
        private SerializedType explicitType;
        private SerializedType bound;

        @Override
        public String toString() {
            return "kind: " + kind + ", explicitType: " + explicitType + ", bound: " + bound;
        }

        @Override
        public int compareTo(SerializedTypeArgument other) {
            if (this == other) {
                return 0;
            }

            int ret = Objects.compare(this.kind, other.kind, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            ret = Objects.compare(this.explicitType, other.explicitType, Comparator.naturalOrder());
            if (ret != 0) {
                return ret;
            }

            return Objects.compare(this.bound, other.bound, Comparator.naturalOrder());
        }
    }

    private enum SerializedTypeArgumentKind {
        EXPLICIT,
        WILDCARD_UNBOUNDED,
        WILDCARD_SUPER,
        WILDCARD_EXTENDS,
    }

    private static class SerializedTypeParameter implements Comparable<SerializedTypeParameter> {
        private String name;
        private List<SerializedType> bounds;

        @Override
        public int compareTo(SerializedTypeParameter other) {
            int ret = Objects.compare(this.name, other.name, Comparator.naturalOrder());
            if (ret == 0) {
                return ret;
            }

            return compareLists(this.bounds, other.bounds);
        }
    }
}
