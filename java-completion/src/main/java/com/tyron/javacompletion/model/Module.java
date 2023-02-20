package com.tyron.javacompletion.model;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.logging.JLogger;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A scope containing a set of classes and the packages defined under the root (unnamed) package.
 *
 * <p>A Module may be created from a set of Java files, index cache files, or JAR archives.
 */
public class Module {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    // Map of filename -> FileScope.
    private final Map<String, FileScope> fileScopeMap;
    private final PackageScope rootPackage;
    private final List<Module> dependingModules;

    public Module() {
        this.fileScopeMap = new HashMap<>();
        this.rootPackage = new PackageScope();
        this.dependingModules = new ArrayList<>();
    }

    public synchronized void addOrReplaceFileScope(FileScope fileScope) {
        logger.fine("Adding file: %s: %s", fileScope.getFilename(), fileScope.getMemberEntities());
        FileScope existingFileScope = fileScopeMap.get(fileScope.getFilename());
        // Add the new file scope to the package first, so that we don't GC the pacakge if
        // the new file and old file are in the same pacakge and is the only file in the package.
        addFileToPackage(fileScope);

        if (existingFileScope != null) {
            removeFileFromPacakge(existingFileScope);
        }
        fileScopeMap.put(fileScope.getFilename(), fileScope);
    }

    public synchronized void removeFile(Path filePath) {
        FileScope existingFileScope = fileScopeMap.get(filePath.toString());
        if (existingFileScope != null) {
            removeFileFromPacakge(existingFileScope);
        }
    }

    public synchronized Optional<FileScope> getFileScope(String filename) {
        return Optional.ofNullable(fileScopeMap.get(filename));
    }

    public synchronized PackageScope getRootPackage() {
        return rootPackage;
    }

    public synchronized PackageScope getPackageForFile(FileScope fileScope) {
        return getOrCreatePackage(fileScope.getPackageQualifiers());
    }

    public synchronized PackageScope getOrCreatePackage(List<String> packageQualifiers) {
        List<String> currentQualifiers = new ArrayList<>();
        PackageScope currentPackage = rootPackage;
        for (String qualifier : packageQualifiers) {
            Optional<PackageEntity> packageEntity = getPackageEntity(qualifier, currentPackage);
            if (packageEntity.isPresent()) {
                currentPackage = packageEntity.get().getScope();
            } else {
                PackageScope packageScope = new PackageScope();
                currentPackage.addEntity(new PackageEntity(qualifier, currentQualifiers, packageScope));
                currentPackage = packageScope;
            }
            currentQualifiers.add(qualifier);
        }
        return currentPackage;
    }

    public synchronized List<FileScope> getAllFiles() {
        return ImmutableList.copyOf(fileScopeMap.values());
    }

    private void addFileToPackage(FileScope fileScope) {
        getPackageForFile(fileScope).addFile(fileScope);
    }

    private void removeFileFromPacakge(FileScope fileScope) {
        Deque<PackageEntity> stack = new ArrayDeque<>();
        PackageScope currentPackage = rootPackage;
        for (String qualifier : fileScope.getPackageQualifiers()) {
            Optional<PackageEntity> optionalPackageEntity = getPackageEntity(qualifier, currentPackage);
            if (!optionalPackageEntity.isPresent()) {
                throw new RuntimeException("Package " + qualifier + " not found");
            }
            PackageEntity packageEntity = optionalPackageEntity.get();
            stack.addFirst(packageEntity);
            currentPackage = packageEntity.getScope();
        }
        currentPackage.removeFile(fileScope);
        while (!currentPackage.hasChildren() && !stack.isEmpty()) {
            PackageEntity packageEntity = stack.removeFirst();
            currentPackage = stack.isEmpty() ? rootPackage : stack.peekFirst().getScope();
            currentPackage.removePackage(packageEntity);
        }
    }

    private Optional<PackageEntity> getPackageEntity(String name, PackageScope packageScope) {
        for (Entity entity : packageScope.getMemberEntities().get(name)) {
            if (entity instanceof PackageEntity) {
                return Optional.of((PackageEntity) entity);
            }
        }
        return Optional.empty();
    }

    public void addDependingModule(Module dependingModule) {
        dependingModules.add(dependingModule);
    }

    public List<Module> getDependingModules() {
        return ImmutableList.copyOf(dependingModules);
    }
}