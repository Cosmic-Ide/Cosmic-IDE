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
package com.tyron.javacompletion.project;

import java.nio.file.Path;
import java.util.Optional;

import com.tyron.javacompletion.model.Module;

/** Interface for classes that creates {@link Module} instance and manages their dependencies. */
public interface ModuleManager {
    /**
     * Initialize the module manager. The module manager can start index files and building modules.
     * The module manager may also choose to defer indexing files to optimize for large repository.
     */
    void initialize();

    /**
     * Gets information for parsed file of {@code path}.
     *
     * <p>Note that the manager implementation may choose not index all files under the project root
     * directory due to too many files in the project, or the indexing is delayed. To ensure a file
     * and its dependency is indexed, call {@link #addOrUpdateFile}.
     */
    Optional<FileItem> getFileItem(Path path);

    /**
     * Explicitly index a file with the content in filesystem and add it to modules. If the file
     * already exist in modules, replace it with the new one.
     *
     * <p>Manager implementation may choose not index all files under the project root directory due
     * to too many files in the project, or the indexing is delayed. This method ensures {@code path}
     * is indexed if it exists in the filesystem.
     */
    void addOrUpdateFile(Path path, boolean fixContentForParsing);

    /** Remove a file from modules. */
    void removeFile(Path path);

    /** Add a module that all modules loaded by the module manager depends on. */
    void addDependingModule(Module module);
}