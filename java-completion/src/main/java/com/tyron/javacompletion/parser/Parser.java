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
