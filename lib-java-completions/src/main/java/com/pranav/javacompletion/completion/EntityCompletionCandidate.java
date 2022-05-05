package com.pranav.javacompletion.completion;

import java.util.List;
import java.util.Optional;
import com.pranav.javacompletion.model.ClassEntity;
import com.pranav.javacompletion.model.Entity;
import com.pranav.javacompletion.model.MethodEntity;
import com.pranav.javacompletion.model.TypeParameter;
import com.pranav.javacompletion.model.TypeReference;
import com.pranav.javacompletion.model.VariableEntity;

/** A {@link CompletionCandidate} backed by {@link Entity}. */
class EntityCompletionCandidate extends EntityBasedCompletionCandidate {
    private final SortCategory sortCategory;

    EntityCompletionCandidate(Entity entity, SortCategory sortCategory) {
        super(entity);
        this.sortCategory = sortCategory;
    }

    @Override
    public String getName() {
        return getEntity().getSimpleName();
    }

    @Override
    public Kind getKind() {
        return toCandidateKind(getEntity().getKind());
    }

    @Override
    public Optional<String> getInsertSnippet(TextEditOptions textEditOptions) {
        switch (getEntity().getKind()) {
            case METHOD:
            {
                if (textEditOptions.getAppendMethodArgumentSnippets()) {
                    MethodEntity method = (MethodEntity) getEntity();
                    StringBuilder sb = new StringBuilder(getName());
                    sb.append("(");
                    boolean firstParam = true;
                    int nParam = 0;
                    for (VariableEntity param : method.getParameters()) {
                        if (!firstParam) {
                            sb.append(", ");
                        } else {
                            firstParam = false;
                        }
                        nParam++;
                        sb.append(String.format("${%d:%s}", nParam, param.getSimpleName()));
                    }
                    sb.append(")");
                    return Optional.of(sb.toString());
                } else {
                    return Optional.empty();
                }
            }
            default:
                return Optional.empty();
        }
    }

    @Override
    public Optional<String> getDetail() {
        Entity entity = getEntity();
        switch (entity.getKind()) {
            case METHOD:
            {
                StringBuilder sb = new StringBuilder();
                MethodEntity method = (MethodEntity) entity;
                if (!method.getTypeParameters().isEmpty()) {
                    appendTypeParameters(sb, method.getTypeParameters());
                    sb.append(" ");
                }
                sb.append("(");
                boolean firstParam = true;
                for (VariableEntity param : method.getParameters()) {
                    if (firstParam) {
                        firstParam = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(param.getType().toDisplayString());
                    sb.append(" ");
                    sb.append(param.getSimpleName());
                }
                sb.append("): ");
                sb.append(method.getReturnType().toDisplayString());
                return Optional.of(sb.toString());
            }
            case CLASS:
            case INTERFACE:
            {
                ClassEntity classEntity = (ClassEntity) entity;
                if (classEntity.getTypeParameters().isEmpty()
                        && !classEntity.getSuperClass().isPresent()
                        && classEntity.getInterfaces().isEmpty()) {
                    return Optional.empty();
                }
                StringBuilder sb = new StringBuilder();
                if (!classEntity.getTypeParameters().isEmpty()) {
                    appendTypeParameters(sb, classEntity.getTypeParameters());
                }
                TypeReference superClassOrOnlyInterface = null;
                if (classEntity.getSuperClass().isPresent()) {
                    superClassOrOnlyInterface = classEntity.getSuperClass().get();
                } else if (classEntity.getInterfaces().size() == 1) {
                    superClassOrOnlyInterface = classEntity.getInterfaces().get(0);
                }

                if (superClassOrOnlyInterface != null) {
                    sb.append(": ");
                    sb.append(superClassOrOnlyInterface.getSimpleName());
                }
                return Optional.of(sb.toString());
            }
            case VARIABLE:
            case FIELD:
            {
                VariableEntity variable = (VariableEntity) entity;
                return Optional.of(variable.getType().toDisplayString());
            }
            default:
                return Optional.empty();
        }
    }

    @Override
    public SortCategory getSortCategory() {
        return sortCategory;
    }

    public static Kind toCandidateKind(Entity.Kind entityKind) {
        switch (entityKind) {
            case CLASS:
                return CompletionCandidate.Kind.CLASS;
            case ANNOTATION:
            case INTERFACE:
                return CompletionCandidate.Kind.INTERFACE;
            case ENUM:
                return CompletionCandidate.Kind.ENUM;
            case METHOD:
                return CompletionCandidate.Kind.METHOD;
            case VARIABLE:
            case PRIMITIVE:
                return CompletionCandidate.Kind.VARIABLE;
            case FIELD:
                return CompletionCandidate.Kind.FIELD;
            case QUALIFIER:
                return CompletionCandidate.Kind.PACKAGE;
            case REFERENCE:
            default:
                return CompletionCandidate.Kind.UNKNOWN;
        }
    }

    private static void appendTypeParameters(StringBuilder sb, List<TypeParameter> typeParameters) {
        sb.append("<");
        boolean firstParam = true;
        for (TypeParameter typeParameter : typeParameters) {
            if (firstParam) {
                firstParam = false;
            } else {
                sb.append(", ");
            }
            sb.append(typeParameter.toDisplayString());
        }
        sb.append(">");
    }

    @Override
    public String toString() {
        return getKind().name() + " " + getName();
    }
}