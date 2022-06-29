/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.langs.textmate;

import android.os.Bundle;

import java.io.InputStream;
import java.io.Reader;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.CharPosition;
import org.eclipse.tm4e.core.theme.IRawTheme;

@Experimental
public class TextM  ateLanguage extends EmptyLanguage {

    private TextMateAnalyzer textMateAnalyzer;
    private int tabSize = 4;
    private boolean javaCompeletions = false;
    private final IdentifierAutoComplete autoComplete;

    private TextMateLanguage(String grammarName, InputStream grammarIns, Reader languageConfiguration, IRawTheme theme) {
        autoComplete = new IdentifierAutoComplete(javaKeywords);
        try {
            textMateAnalyzer = new TextMateAnalyzer(this,grammarName, grammarIns,languageConfiguration, theme);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TextMateLanguage create(String grammarName, InputStream grammarIns,Reader languageConfiguration, IRawTheme theme) {
        return new TextMateLanguage(grammarName, grammarIns,languageConfiguration, theme);
    }

    public static TextMateLanguage create(String grammarName, InputStream grammarIns, IRawTheme theme) {
        return new TextMateLanguage(grammarName, grammarIns,null, theme);
    }
    /**
     * When you update the {@link TextMateColorScheme} for editor, you need to synchronize the updates here
     *
     * @param theme IRawTheme creates from file
     */

    public void updateTheme(IRawTheme theme) {
        if (textMateAnalyzer != null) {
            textMateAnalyzer.updateTheme(theme);
        }
    }

    @Override
    public AnalyzeManager getAnalyzeManager() {
        if (textMateAnalyzer != null) {
            return textMateAnalyzer;
        }
        return EmptyAnalyzeManager.INSTANCE;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Set tab size. The tab size is used to compute code blocks.
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    public int getTabSize() {
        return tabSize;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    public void setEnableJavaCompletions(boolean condition) {
        javaCompeletions = condition;
    }

    @Override
    public void requireAutoComplete(ContentReference content, CharPosition position, CompletionPublisher publisher, Bundle extraArguments) {
        if (javaCompeletions) {
            var prefix = CompletionHelper.computePrefix(content, position, MyCharacter::isJavaIdentifierPart);
            autoComplete.requireAutoComplete(prefix, publisher, null);
        }
    }

    private final String[] javaKeywords = {
        "assert", "abstract", "boolean", "byte", "char", "class", "do",
        "double", "final", "float", "for", "if", "int", "long", "new",
        "public", "private", "protected", "package", "return", "static",
        "short", "super", "switch", "else", "volatile", "synchronized", "strictfp",
        "goto", "continue", "break", "transient", "void", "try", "catch",
        "finally", "while", "case", "default", "const", "enum", "extends",
        "implements", "import", "instanceof", "interface", "native",
        "this", "throw", "throws", "true", "false", "null", "var", "sealed", "permits"
    };
}
