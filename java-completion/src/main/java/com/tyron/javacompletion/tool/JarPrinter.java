package com.tyron.javacompletion.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import com.tyron.javacompletion.parser.classfile.AttributeInfo;
import com.tyron.javacompletion.parser.classfile.ClassFileInfo;
import com.tyron.javacompletion.parser.classfile.ClassFileParser;
import com.tyron.javacompletion.parser.classfile.ClassInfoConverter;
import com.tyron.javacompletion.parser.classfile.ConstantPoolInfo;
import com.tyron.javacompletion.parser.classfile.FieldInfo;
import com.tyron.javacompletion.parser.classfile.MethodInfo;
import com.tyron.javacompletion.parser.classfile.ParsedClassFile;
import com.tyron.javacompletion.parser.classfile.ParsedClassFile.ParsedField;
import com.tyron.javacompletion.parser.classfile.ParsedClassFile.ParsedMethod;

/**
 * Print result of parsing a .jar file.
 *
 * <p>Usage:
 *
 * <pre>
 * bazel run //src/main/java/org/javacomp/tool:JarPrinter -- [<jar-file-name>] [-c <class-file-name>]
 * </pre>
 */
public class JarPrinter {
    private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
    private final ClassFileParser parser;
    private final ClassInfoConverter converter;

    public JarPrinter() {
        this.parser = new ClassFileParser();
        this.converter = new ClassInfoConverter();
    }

    public void parseAndPrint(String jarFilename, Optional<String> classFilename) {
        // JAR specific URI pattern.
        // See https://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
        FileSystem fs;
        try {
            Path jarPath = Paths.get(jarFilename);
            URI uri = new URI("jar", jarPath.toUri().toString(), null);
            fs = FileSystems.newFileSystem(uri, ImmutableMap.of());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Path jarRootPath = fs.getPath("/");
        try (Stream<Path> pathStream = Files.walk(jarRootPath)) {
            pathStream.forEach(
                    (Path path) -> {
                        if (!path.toString().endsWith(".class")) {
                            return;
                        }
                        if (!classFilename.isPresent() || path.toString().endsWith(classFilename.get())) {
                            printClassFile(path);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printClassFile(Path path) {
        if (Files.isRegularFile(path)) {
            System.out.println(".class file: " + path);
            try {
                ClassFileInfo classFileInfo = parser.parse(path);
                System.out.println("  Parsed: ");
                printClassFileInfo(classFileInfo);

                ParsedClassFile converted = converter.convert(classFileInfo);
                System.out.println("  Converted:");
                printParsedClassFile(converted);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void printClassFileInfo(ClassFileInfo classFileInfo) {
        System.out.println("    This class index: " + classFileInfo.getThisClassIndex());
        System.out.println("    Super class index: " + classFileInfo.getSuperClassIndex());

        System.out.println("    Constant Pool");
        int constantIndex = 0;
        for (ConstantPoolInfo constantPool : classFileInfo.getConstantPool()) {
            System.out.println("      " + constantIndex + ": " + constantPool.toString());
            constantIndex++;
        }

        System.out.println("    Interface indeces: " + classFileInfo.getInterfaceIndeces());

        System.out.println("    Fields");
        for (FieldInfo field : classFileInfo.getFields()) {
            System.out.println("      " + field);
        }

        System.out.println("    Methods");
        for (MethodInfo method : classFileInfo.getMethods()) {
            System.out.println("      " + method);
        }

        System.out.println("    Attributes");
        for (AttributeInfo attribute : classFileInfo.getAttributes()) {
            System.out.println("      " + attribute.toString());
        }
    }

    private void printParsedClassFile(ParsedClassFile parsedClassFile) {
        System.out.println("    Class binary name: " + parsedClassFile.getClassBinaryName());
        System.out.println("    Simple name: " + parsedClassFile.getSimpleName());
        System.out.println(
                "    Class qualifiers: " + QUALIFIER_JOINER.join(parsedClassFile.getClassQualifiers()));
        System.out.println("    Outer class binary name: " + parsedClassFile.getOuterClassBinaryName());
        System.out.println("    Class signature: " + parsedClassFile.getClassSignature());
        System.out.println("    Kind: " + parsedClassFile.getEntityKind());
        System.out.println("    Static? " + parsedClassFile.isStatic());

        System.out.println("    Methods:");
        for (ParsedMethod method : parsedClassFile.getMethods()) {
            System.out.println("      " + method);
        }

        System.out.println("    Fields:");
        for (ParsedField field : parsedClassFile.getFields()) {
            System.out.println("      " + field);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
        }
        boolean fixFileContent = false;
        Optional<String> jarFilename = Optional.empty();
        Optional<String> classFilename = Optional.empty();
        for (int i = 0; i < args.length; i++) {
            if ("-c".equals(args[i])) {
                if (i + 1 == args.length) {
                    System.out.println("Missing .class filename for option -c");
                    printHelp();
                }

                classFilename = Optional.of(args[i + 1]);
                i++;
            } else if (!jarFilename.isPresent()) {
                jarFilename = Optional.of(args[i]);
            } else {
                System.out.println("Unknown argument " + args[i]);
                printHelp();
            }
        }

        if (jarFilename.isPresent()) {
            new JarPrinter().parseAndPrint(jarFilename.get(), classFilename);
        } else if (classFilename.isPresent()) {
            new JarPrinter().printClassFile(Paths.get(classFilename.get()));
        } else {
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("Usage: JarPrinter [<jar-file>] [-c <class-file>]");
        System.out.println("  Prints the internal representation of parsed .class files.");
        System.out.println();
        System.out.println(
                "  If both <jar-file> and <class-file> exist, print the files in <jar-file>");
        System.out.println("  whose names end with <class-file>.");
        System.out.println();
        System.out.println("  If only <jar-file> exists, print all .class files in the jar file.");
        System.out.println();
        System.out.println("  If only <class-file> exists, print the .class file in the file system.");
        System.exit(1);
    }
}