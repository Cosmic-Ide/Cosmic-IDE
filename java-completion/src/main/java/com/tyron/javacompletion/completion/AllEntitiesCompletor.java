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
package com.tyron.javacompletion.completion;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import com.tyron.javacompletion.model.ClassEntity;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;

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
                if (entity instanceof ClassEntity) {
                    addAllClasses(builder, (ClassEntity) entity, prefix);
                } else {
                    continue;
                }
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