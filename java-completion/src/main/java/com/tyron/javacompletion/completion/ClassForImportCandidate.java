package com.tyron.javacompletion.completion;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveAction;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveActionParams;
import com.tyron.javacompletion.protocol.textdocument.CompletionItem.ResolveAddImportTextEditsParams;
import com.tyron.javacompletion.typesolver.EntityShadowingListBuilder.ForImportEntity;

/** A candidate with import class edit actions. */
class ClassForImportCandidate extends EntityBasedCompletionCandidate {
    private final Path filePath;

    ClassForImportCandidate(ClassEntity classEntity, String filePath) {
        super(new ForImportEntity(classEntity));
        this.filePath = Paths.get(filePath);
    }

    @Override
    public String getName() {
        return getEntity().getSimpleName();
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public Optional<String> getDetail() {
        return Optional.of(getEntity().getQualifiedName());
    }

    @Override
    public SortCategory getSortCategory() {
        return SortCategory.TO_IMPORT;
    }

    @Override
    public Map<ResolveAction, ResolveActionParams> getResolveActions() {
        ImmutableMap.Builder<ResolveAction, ResolveActionParams> builder = new ImmutableMap.Builder<>();
        ResolveAddImportTextEditsParams params = new ResolveAddImportTextEditsParams();
        params.uri = filePath.toUri();
        params.classFullName = getEntity().getQualifiedName();
        builder.put(ResolveAction.ADD_IMPORT_TEXT_EDIT, params);
        builder.putAll(super.getResolveActions());
        return builder.build();
    }
}