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
package com.tyron.javacompletion.parser.classfile;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;

/** A lexer for the content of {@link AttributeInfo.Signature}. */
public class SignatureLexer {
    public static final char END_OF_SIGNATURE = '\0';
    private static final ImmutableSet<Character> NON_IDENTIFIER_CHARS =
            ImmutableSet.of('.', ';', '[', '/', '<', '>', ':');
    private final String content;
    private int pos;
    private final int identifierEndPos = -1;

    public SignatureLexer(String content) {
        this.content = content;
        this.pos = 0;
    }

    /** Read the next character without consuming it. */
    public char peekChar() {
        // You can peek a character at the end of the signature as long as you don't skip it.
        if (pos >= content.length()) {
            return END_OF_SIGNATURE;
        }
        return content.charAt(pos);
    }

    /** Read the next character and consume it. */
    public char nextChar() {
        return content.charAt(pos++);
    }

    /** Skip the next character. */
    public void skipChar() {
        if (pos >= content.length()) {
            throw new IndexOutOfBoundsException(
                    "Cannot skip the next character. Already reach the end of the signature.");
        }
        pos++;
    }

    /** Read the next identifier and consume it. */
    public String nextIdentifier() {
        int startPos = pos;
        while (pos < content.length() && !NON_IDENTIFIER_CHARS.contains(content.charAt(pos))) {
            pos++;
        }
        checkState(startPos < pos, "No identifier at current point: " + remainingContent());

        return content.substring(startPos, pos);
    }

    public boolean hasRemainingContent() {
        return pos < content.length();
    }

    public String remainingContent() {
        return hasRemainingContent() ? content.substring(pos) : "";
    }
}