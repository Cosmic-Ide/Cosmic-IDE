package org.cosmic.ide.activity.editor;

import android.content.res.AssetManager;
import android.os.Bundle;
import com.tyron.kotlin_completion.util.PsiUtils;
import com.tyron.kotlin.completion.KotlinCompletionUtils;
import org.cosmic.ide.project.Project;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.langs.textmate.theme.TextMateColorScheme;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.util.MyCharacter;

import java.io.IOException;
import java.io.File;
import java.util.Collection;

public class KotlinLanguage extends TextMateLanguage {

    private final CodeEditor mEditor;
    private final Project mProject;
    private final File mCurrentFile;

    public KotlinLanguage(CodeEditor editor, Project project, File file) throws IOException {
        mEditor = editor;
        mProject = project;
        mCurrentFile = file;
        super.create(
                IGrammarSource.fromInputStream(
                    editor.getContext().getAssets().open("textmate/kotlin/syntaxes/kotlin.tmLanguage"),
                    "kotlin.tmLanguage",
                    null
                ),
                new InputStreamReader(
                        editor.getContext().getAssets().open("textmate/kotlin/language-configuration.json")),
                getColorScheme().getThemeSource());
    }

    @Override
    public void requireAutoComplete(ContentReference content,
                                    CharPosition position,
                                    CompletionPublisher publisher,
                                    Bundle extraArguments) throws CompletionCancelledException {
        char c = content.charAt(position.getIndex() - 1);
        if (!isAutoCompleteChar(c)) {
            return;
        }
        String prefix = CompletionHelper.computePrefix(content, position, this::isAutoCompleteChar);
        PsiElement psiElement = KotlinCompletionUtils.INSTANCE
                .getPsiElement(mCurrentFile, mProject, mEditor, mEditor.getCaret().getStart());
        KtSimpleNameExpression parent =
                PsiUtils.findParent(psiElement, KtSimpleNameExpression.class);

        Collection<DeclarationDescriptor> referenceVariants = KotlinCompletionUtils.INSTANCE
                .getReferenceVariants(parent, name -> true, mCurrentFile, prefix);
        referenceVariants.stream().forEach(it -> {
            publisher.addItem(new SimpleCompletionItem(prefix.length(), it.getName().toString()));
        });
    }

    public boolean isAutoCompleteChar(char p1) {
        return p1 == '.' || MyCharacter.isJavaIdentifierPart(p1);
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return handlers;
    }

    private final NewlineHandler[] handlers = new NewlineHandler[]{new BraceHandler()};

    class BraceHandler implements NewlineHandler {

        @Override
        public boolean matchesRequirement(String beforeText, String afterText) {
            return beforeText.endsWith("{") && afterText.startsWith("}");
        }

        @Override
        public NewlineHandleResult handleNewline(String beforeText, String afterText, int tabSize) {
            int count = TextUtils.countLeadingSpaceCount(beforeText, tabSize);
            int advanceBefore = getIndentAdvance(beforeText);
            int advanceAfter = getIndentAdvance(afterText);
            String text;
            StringBuilder sb = new StringBuilder("\n").append(
                    TextUtils.createIndent(count + advanceBefore, tabSize, useTab()))
                    .append('\n')
                    .append(text = TextUtils.createIndent(count + advanceAfter, tabSize, useTab()));
            int shiftLeft = text.length() + 1;
            return new NewlineHandleResult(sb, shiftLeft);
        }
    }
}