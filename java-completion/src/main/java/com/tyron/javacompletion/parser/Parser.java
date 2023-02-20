package com.tyron.javacompletion.parser;

import com.tyron.javacompletion.file.FileManager;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.options.IndexOptions;
import com.sun.source.tree.LineMap;

import java.nio.file.Path;
import java.util.Optional;

/** Parser that converts source file to {@link FileScope}. */
public class Parser {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private final ParserContext parserContext = new ParserContext();
    private final FileContentFixer fileContentFixer = new FileContentFixer(parserContext);
    private final FileManager fileManager;
    private final IndexOptions indexOptions;

    public Parser(FileManager fileManager, IndexOptions indexOptions) {
        this.fileManager = fileManager;
        this.indexOptions = indexOptions;
    }

    public Optional<FileScope> parseSourceFile(Path path, boolean fixContentForParsing) {
        parserContext.setupLoggingSource(path.toString());
        Optional<CharSequence> optionalContent = fileManager.getFileContent(path);
        if (!optionalContent.isPresent()) {
            logger.info("Didn't parse %s because it's not found.", path);
            return Optional.empty();
        }
        CharSequence content = optionalContent.get();
        LineMap adjustedLineMap = null;

        if (fixContentForParsing) {
            FileContentFixer.FixedContent fixedContent = fileContentFixer.fixFileContent(content);
            content = fixedContent.getContent();
            adjustedLineMap = fixedContent.getAdjustedLineMap();
        }
        FileScope fileScope =
                new AstScanner(indexOptions)
                        .startScan(parserContext.parse(path.toString(), content), path.toString(), content);
        if (adjustedLineMap != null) {
            fileScope.setAdjustedLineMap(adjustedLineMap);
        }
        return Optional.of(fileScope);
    }
}
