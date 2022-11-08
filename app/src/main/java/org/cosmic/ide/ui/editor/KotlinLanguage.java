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
import org.cosmic.ide.project.Project;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.File;
import java.util.Collection;

public class KotlinLanguage extends TextMateLanguage {

    private final CodeEditor mEditor;
    private final Project mProject;
    private final KotlinEnvironment kotlinEnvironment;
    private final String fileName;
    private final String TAG = "KotlinLanguage";

    public KotlinLanguage(CodeEditor editor, Project project, File file, IThemeSource theme)
            throws Exception {
        super(
                GrammarRegistry.getInstance().findGrammar("source.kotlin"),
                GrammarRegistry.getInstance().findLanguageConfiguration("source.kotlin"),
                GrammarRegistry.getInstance(),
                ThemeRegistry.getInstance(),
                true);
        mEditor = editor;
        mProject = project;
        kotlinEnvironment = KotlinEnvironment.Companion.get(mProject);
        final var ktFile =
                kotlinEnvironment.updateKotlinFile(
                        file.getAbsolutePath(), mEditor.getText().toString());
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

        try {
            final var text = mEditor.getText().toString();
            final var ktFile = kotlinEnvironment.updateKotlinFile(fileName, text);
            Collection<CompletionItem> itemList =
                    kotlinEnvironment.complete(
                             ktFile, position.getLine(), position.getColumn());
            publisher.addItems(itemList);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to fetch code suggestions", e);
        }
        super.requireAutoComplete(content, position, publisher, extraArguments);
    }
}
