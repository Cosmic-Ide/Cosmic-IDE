// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0
// license that can be found in the LICENSE file.
package com.intellij.ide.plugins;

import com.itsaky.androidide.zipfs2.ZipFileSystemProvider;

import gnu.trove.THashMap;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * I have modified this class to use {@link com.itsaky.androidide.zipfs.ZipFileSystem} because
 * android doesn't provide a ZipFileSystem
 */

// parentContext is null only for CoreApplicationEnvironment - it is not valid otherwise because in
// this case XML is not interned.
final class DescriptorLoadingContext implements AutoCloseable {
    private final Map<Path, FileSystem> openedFiles = new THashMap<>();
    final DescriptorListLoadingContext parentContext;
    final boolean isBundled;
    final boolean isEssential;

    final PathBasedJdomXIncluder.PathResolver<?> pathResolver;

    DescriptorLoadingContext(
            DescriptorListLoadingContext parentContext,
            boolean isBundled,
            boolean isEssential,
            PathBasedJdomXIncluder.PathResolver<?> pathResolver) {
        this.parentContext = parentContext;
        this.isBundled = isBundled;
        this.isEssential = isEssential;
        this.pathResolver = pathResolver;
    }

    FileSystem open(Path file) throws IOException {
        FileSystem result = openedFiles.get(file);
        if (result == null) {
            result = new ZipFileSystemProvider().newFileSystem(file, Collections.emptyMap());
            openedFiles.put(file, result);
        }
        return result;
    }

    @Override
    public void close() {
        for (FileSystem file : openedFiles.values()) {
            try {
                file.close();
            } catch (IOException ignore) {
            }
        }
    }

    public DescriptorLoadingContext copy(boolean isEssential) {
        return new DescriptorLoadingContext(parentContext, isBundled, isEssential, pathResolver);
    }
}
