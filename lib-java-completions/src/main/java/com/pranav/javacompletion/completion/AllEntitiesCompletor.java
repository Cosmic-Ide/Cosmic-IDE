package com.pranav.javacompletion.completion;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import com.pranav.javacompletion.model.ClassEntity;
import com.pranav.javacompletion.model.Entity;
import com.pranav.javacompletion.model.FileScope;
import com.pranav.javacompletion.model.Module;

/** Completor for all entities accessible from a module */
class AllEntitiesCompletor {
    ImmutableList<ClassEntity> getAllClasses(Module module, String prefix) {
        ImmutableList.Builder<ClassEntity> builder = new ImmutableList.Builder<>();
        HashSet<FileScope> visitedFiles = new HashSet<>();
        HashSet<Module> visitedModules = new HashSet<>();
        addClassesInModule(builder, module, prefix, visitedModules, visitedFiles);
        return builder.build();
    }

    private void addClassesInModule(
            ImmutableList.Builder<ClassEntity> builder,
            Module module,
            String prefix,
            HashSet<Module> visitedModules,
            HashSet<FileScope> visitedFiles) {
        visitedModules.add(module);

        for (FileScope fileScope : module.getAllFiles()) {
            if (visitedFiles.contains(fileScope)) {
                continue;
            }
            visitedFiles.add(fileScope);

            for (Entity entity : fileScope.getMemberEntities().values()) {
                if (!(entity instanceof ClassEntity)) {
                    continue;
                }

                ClassEntity classEntity = (ClassEntity) entity;
                addAllClasses(builder, classEntity, prefix);
            }
        }

        for (Module depModule : module.getDependingModules()) {
            if (!visitedModules.contains(depModule)) {
                addClassesInModule(builder, depModule, prefix, visitedModules, visitedFiles);
            }
        }
    }

    private void addAllClasses(
            ImmutableList.Builder<ClassEntity> builder, ClassEntity parentClass, String prefix) {
        LinkedList<ClassEntity> queue = new LinkedList<ClassEntity>();
        queue.addLast(parentClass);
        while (!queue.isEmpty()) {
            ClassEntity classEntity = queue.removeFirst();
            if (CompletionPrefixMatcher.matches(classEntity.getSimpleName(), prefix)) {
                builder.add(classEntity);
            }
            Collection<ClassEntity> innerClasses = classEntity.getInnerClasses().values();
            queue.addAll(innerClasses);
        }
    }
}