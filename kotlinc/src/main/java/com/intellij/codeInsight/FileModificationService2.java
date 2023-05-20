/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.intellij.codeInsight;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public abstract class FileModificationService2 {
    public static FileModificationService2 getInstance() {
        return ApplicationManager.getApplication().getService(FileModificationService2.class);
    }

    public abstract boolean preparePsiElementsForWrite(
            @NotNull Collection<? extends PsiElement> elements);

    public abstract boolean prepareFileForWrite(@Nullable final PsiFile psiFile);

    public boolean preparePsiElementForWrite(@Nullable PsiElement element) {
        PsiFile file = element == null ? null : element.getContainingFile();
        return prepareFileForWrite(file);
    }

    public boolean preparePsiElementsForWrite(@NotNull PsiElement... elements) {
        return preparePsiElementsForWrite(Arrays.asList(elements));
    }

    public abstract boolean prepareVirtualFilesForWrite(
            @NotNull Project project, @NotNull Collection<? extends VirtualFile> files);
}
