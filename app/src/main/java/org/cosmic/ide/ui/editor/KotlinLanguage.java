package org.cosmic.ide.ui.editor;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.tyron.kotlin.completion.KotlinEnvironment;

import io.github.rosemoe.sora.lang.completion.*;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.CodeEditor;

import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.project.KotlinProject;
import org.cosmic.ide.project.Project;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.File;
import java.util.Collection;

public class KotlinLanguage extends TextMateLanguage {

    private final CodeEditor mEditor;
    private final KotlinProject mProject;
    private final File mCurrentFile;
    private final KotlinEnvironment kotlinEnvironment;
    private final String fileName;

    public KotlinLanguage(CodeEditor editor, Project project, File file, IThemeSource theme)
            throws Exception {
        super(
                GrammarRegistry.getInstance().findGrammar("source.kotlin"),
                GrammarRegistry.getInstance().findLanguageConfiguration("source.kotlin"),
                GrammarRegistry.getInstance(),
                ThemeRegistry.getInstance(),
                true);
        mEditor = editor;
        mCurrentFile = file;
        if (project instanceof KotlinProject) {
            mProject = (KotlinProject) project;
        } else {
            mProject = new KotlinProject(project.getRootFile());
        }
        kotlinEnvironment = KotlinEnvironment.Companion.get(mProject);
        final var ktFile =
                kotlinEnvironment.updateKotlinFile(
                        mCurrentFile.getAbsolutePath(), mEditor.getText().toString());
        fileName = ktFile.getName();
    }

    @Override
    @WorkerThread
    public void requireAutoComplete(
            ContentReference content,
            CharPosition position,
            CompletionPublisher publisher,
            Bundle extraArguments)
            throws CompletionCancelledException {
        String prefix = CompletionHelper.computePrefix(content, position, this::isAutoCompleteChar);
        final var text = mEditor.getText().toString();

        CoroutineUtil.execute(
                () -> {
                    try {
                        final var ktFile = kotlinEnvironment.updateKotlinFile(fileName, text);
                        Collection<CompletionItem> itemList =
                                kotlinEnvironment.complete(
                                        ktFile, position.getLine(), position.getColumn());
                        publisher.addItems(itemList);
                    } catch (Throwable e) {
                        Log.e("CodeCompletion", "Failed to fetch code suggestions", e);
                    }
                });
        super.requireAutoComplete(content, position, publisher, extraArguments);
    }

    public boolean isAutoCompleteChar(char p1) {
        return true;
    }
}
