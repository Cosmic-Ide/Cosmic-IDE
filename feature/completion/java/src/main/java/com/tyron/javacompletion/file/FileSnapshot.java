/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

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
package com.tyron.javacompletion.file;

import java.io.IOException;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * Snapshot of the content of a file.
 */
public class FileSnapshot extends SimpleJavaFileObject {
    /**
     * Maps line number to the position of the start of the line in the content string.
     */
    private StringBuilder content;

    private FileSnapshot(URI fileUri, String content) {
        super(fileUri, Kind.SOURCE);
        this.content = new StringBuilder(content);
    }

    /**
     * Loads the content of a file from {@code filename} and creates a {@link FileSnapshot} with the
     * content.
     */
    public static FileSnapshot create(URI fileUri, String content) throws IOException {
        return new FileSnapshot(fileUri, content);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }

    public String getContent() {
        return content.toString();
    }

    public void setContent(String newText) {
        this.content = new StringBuilder(newText);
    }
}