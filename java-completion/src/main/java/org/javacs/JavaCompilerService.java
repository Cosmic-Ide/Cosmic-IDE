/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * Java Language Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Language Server.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class JavaCompilerService implements CompilerProvider {
    private static final Pattern PACKAGE_EXTRACTOR = Pattern.compile("^([a-z][_a-zA-Z0-9]*\\.)*[a-z][_a-zA-Z0-9]*");
    private static final Pattern SIMPLE_EXTRACTOR = Pattern.compile("[A-Z][_a-zA-Z0-9]*$");
    private static final Cache<String, Boolean> cacheContainsWord = new Cache<>();
    private static final Cache<Void, List<String>> cacheContainsType = new Cache<>();
    private static final Logger LOG = Logger.getLogger("main");
    // Not modifiable! If you want to edit these, you need to create a new instance
    final Set<Path> classPath, docPath;
    final Set<String> addExports;
    final ReusableCompiler compiler = new ReusableCompiler();
    final Docs docs;
    final Set<String> jdkClasses = new HashSet<String>();
    final Set<String> classPathClasses;
    // Diagnostics from the last compilation task
    final List<Diagnostic<? extends JavaFileObject>> diags = Collections.synchronizedList(new ArrayList<Diagnostic<? extends JavaFileObject>>());
    // Use the same file manager for multiple tasks, so we don't repeatedly re-compile the same files
    // TODO intercept files that aren't in the batch and erase method bodies so compilation is faster
    final SourceFileManager fileManager;
    private final Map<JavaFileObject, Long> cachedModified = new HashMap<>();
    private final Cache<Void, List<String>> cacheFileImports = new Cache<>();
    private CompileBatch cachedCompile;

    public JavaCompilerService(Set<Path> classPath, Set<Path> docPath, Set<String> addExports) {
        this.classPath = classPath;
        this.docPath = docPath;
        this.addExports = addExports;
        this.docs = new Docs(docPath);
        this.classPathClasses = ScanClassPath.classPathTopLevelClasses(classPath);
        this.fileManager = new SourceFileManager();
    }

    private boolean needsCompile(Collection<? extends JavaFileObject> sources) {
        if (cachedModified.size() != sources.size()) {
            return true;
        }
        for (var f : sources) {
            if (!cachedModified.containsKey(f)) {
                return true;
            }
            if (f.getLastModified() != cachedModified.get(f)) {
                return true;
            }
        }
        return false;
    }

    private void loadCompile(Collection<? extends JavaFileObject> sources) {
        if (cachedCompile != null) {
            cachedCompile.borrow.close();
        }
        cachedCompile = doCompile(sources);
        cachedModified.clear();
        for (var f : sources) {
            cachedModified.put(f, f.getLastModified());
        }
    }

    private CompileBatch doCompile(Collection<? extends JavaFileObject> sources) {
        if (sources.isEmpty()) throw new RuntimeException("empty sources");
        var firstAttempt = new CompileBatch(this, sources);
        var addFiles = firstAttempt.needsAdditionalSources();
        if (addFiles.isEmpty()) return firstAttempt;
        // If the compiler needs additional source files that contain package-private files
        LOG.info("...need to recompile with " + addFiles);
        firstAttempt.close();
        firstAttempt.borrow.close();
        var moreSources = new ArrayList<JavaFileObject>(sources);
        for (var add : addFiles) {
            moreSources.add(new SourceFileObject(add));
        }
        return new CompileBatch(this, moreSources);
    }

    private CompileBatch compileBatch(Collection<? extends JavaFileObject> sources) {
        if (needsCompile(sources)) {
            loadCompile(sources);
        } else {
            LOG.info("...using cached compile");
        }
        return cachedCompile;
    }

    private String packageName(String className) {
        var m = PACKAGE_EXTRACTOR.matcher(className);
        if (m.find()) {
            return m.group();
        }
        return "";
    }

    private String simpleName(String className) {
        var m = SIMPLE_EXTRACTOR.matcher(className);
        if (m.find()) {
            return m.group();
        }
        return "";
    }

    private boolean containsWord(Path file, String word) {
        if (cacheContainsWord.needs(file, word)) {
            cacheContainsWord.load(file, word, StringSearch.containsWord(file, word));
        }
        return cacheContainsWord.get(file, word);
    }

    private boolean containsType(Path file, String className) {
        if (cacheContainsType.needs(file, null)) {
            var root = parse(file).root;
            var types = new ArrayList<String>();
            new FindTypeDeclarations().scan(root, types);
            cacheContainsType.load(file, null, types);
        }
        return cacheContainsType.get(file, null).contains(className);
    }

    private List<String> readImports(Path file) {
        if (cacheFileImports.needs(file, null)) {
            loadImports(file);
        }
        return cacheFileImports.get(file, null);
    }

    private void loadImports(Path file) {
        var list = new ArrayList<String>();
        var importClass = Pattern.compile("^import +([\\w\\.]+\\.\\w+);");
        var importStar = Pattern.compile("^import +([\\w\\.]+\\.\\*);");
        try (var lines = FileStore.lines(file)) {
            for (var line = lines.readLine(); line != null; line = lines.readLine()) {
                // If we reach a class declaration, stop looking for imports
                // TODO This could be a little more specific
                if (line.contains("class")) break;
                // import foo.bar.Doh;
                var matchesClass = importClass.matcher(line);
                if (matchesClass.matches()) {
                    list.add(matchesClass.group(1));
                }
                // import foo.bar.*
                var matchesStar = importStar.matcher(line);
                if (matchesStar.matches()) {
                    list.add(matchesStar.group(1));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cacheFileImports.load(file, null, list);
    }

    @Override
    public Set<String> imports() {
        var all = new HashSet<String>();
        for (var f : FileStore.all()) {
            all.addAll(readImports(f));
        }
        return all;
    }

    @Override
    public List<String> publicTopLevelTypes() {
        var all = new ArrayList<String>();
        for (var file : FileStore.all()) {
            var fileName = file.getFileName().toString();
            if (!fileName.endsWith(".java")) continue;
            var className = fileName.substring(0, fileName.length() - ".java".length());
            var packageName = FileStore.packageName(file);
            if (!packageName.isEmpty()) {
                className = packageName + "." + className;
            }
            all.add(className);
        }
        all.addAll(classPathClasses);
        all.addAll(jdkClasses);
        return all;
    }

    @Override
    public List<String> packagePrivateTopLevelTypes(String packageName) {
        return List.of("TODO");
    }

    public boolean containsImport(Path file, String className) {
        var packageName = packageName(className);
        if (FileStore.packageName(file).equals(packageName)) return true;
        var star = packageName + ".*";
        for (var i : readImports(file)) {
            if (i.equals(className) || i.equals(star)) return true;
        }
        return false;
    }

    @Override
    public Iterable<Path> search(String query) {
        Predicate<Path> test = f -> StringSearch.containsWordMatching(f, query);
        return () -> FileStore.all().stream().filter(test).iterator();
    }

    @Override
    public Optional<JavaFileObject> findAnywhere(String className) {
        var fromDocs = findPublicTypeDeclarationInDocPath(className);
        if (fromDocs.isPresent()) {
            return fromDocs;
        }
        var fromJdk = findPublicTypeDeclarationInJdk(className);
        if (fromJdk.isPresent()) {
            return fromJdk;
        }
        var fromSource = findTypeDeclaration(className);
        if (fromSource != NOT_FOUND) {
            return Optional.of(new SourceFileObject(fromSource));
        }
        return Optional.empty();
    }

    private Optional<JavaFileObject> findPublicTypeDeclarationInDocPath(String className) {
        try {
            var found =
                    docs.fileManager.getJavaFileForInput(
                            StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE);
            return Optional.ofNullable(found);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<JavaFileObject> findPublicTypeDeclarationInJdk(String className) {
        try {
            for (var module : ScanClassPath.JDK_MODULES) {
                var moduleLocation = docs.fileManager.getLocationForModule(StandardLocation.MODULE_SOURCE_PATH, module);
                if (moduleLocation == null) continue;
                var fromModuleSourcePath =
                        docs.fileManager.getJavaFileForInput(moduleLocation, className, JavaFileObject.Kind.SOURCE);
                if (fromModuleSourcePath != null) {
                    LOG.info(String.format("...found %s in module %s of jdk", fromModuleSourcePath.toUri(), module));
                    return Optional.of(fromModuleSourcePath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Path findTypeDeclaration(String className) {
        var fastFind = findPublicTypeDeclaration(className);
        if (fastFind != NOT_FOUND) return fastFind;
        // In principle, the slow path can be skipped in many cases.
        // If we're spending a lot of time in findTypeDeclaration, this would be a good optimization.
        var packageName = packageName(className);
        var simpleName = simpleName(className);
        for (var f : FileStore.list(packageName)) {
            if (containsWord(f, simpleName) && containsType(f, className)) {
                return f;
            }
        }
        return NOT_FOUND;
    }

    private Path findPublicTypeDeclaration(String className) {
        JavaFileObject source;
        try {
            source =
                    fileManager.getJavaFileForInput(
                            StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (source == null) return NOT_FOUND;
        if (!source.toUri().getScheme().equals("file")) return NOT_FOUND;
        var file = Paths.get(source.toUri());
        if (!containsType(file, className)) return NOT_FOUND;
        return file;
    }

    @Override
    public Path[] findTypeReferences(String className) {
        var packageName = packageName(className);
        var simpleName = simpleName(className);
        var candidates = new ArrayList<Path>();
        for (var f : FileStore.all()) {
            if (containsWord(f, packageName) && containsImport(f, className) && containsWord(f, simpleName)) {
                candidates.add(f);
            }
        }
        return candidates.toArray(Path[]::new);
    }

    @Override
    public Path[] findMemberReferences(String className, String memberName) {
        var candidates = new ArrayList<Path>();
        for (var f : FileStore.all()) {
            if (containsWord(f, memberName)) {
                candidates.add(f);
            }
        }
        return candidates.toArray(Path[]::new);
    }

    @Override
    public ParseTask parse(Path file) {
        var parser = Parser.parseFile(file);
        return new ParseTask(parser.task, parser.root);
    }

    @Override
    public ParseTask parse(JavaFileObject file) {
        var parser = Parser.parseJavaFileObject(file);
        return new ParseTask(parser.task, parser.root);
    }

    @Override
    public CompileTask compile(Path... files) {
        var sources = new ArrayList<JavaFileObject>();
        for (var f : files) {
            sources.add(new SourceFileObject(f));
        }
        return compile(sources);
    }

    @Override
    public CompileTask compile(Collection<? extends JavaFileObject> sources) {
        var compile = compileBatch(sources);
        return new CompileTask(compile.task, compile.roots, diags, compile::close);
    }
}
