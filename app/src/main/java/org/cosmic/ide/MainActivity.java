package org.cosmic.ide;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.snackbar.Snackbar;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;

import org.cosmic.ide.android.code.decompiler.FernFlowerDecompiler;
import org.cosmic.ide.android.code.disassembler.*;
import org.cosmic.ide.android.code.formatter.*;
import org.cosmic.ide.common.Indexer;
import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.common.util.ZipUtil;
import org.cosmic.ide.compiler.CompileTask;
import org.cosmic.ide.databinding.ActivityMainBinding;
import org.cosmic.ide.editor.completion.CustomCompletionItemAdapter;
import org.cosmic.ide.editor.completion.CustomCompletionLayout;
import org.cosmic.ide.project.JavaProject;
import org.cosmic.ide.ui.utils.UiUtilsKt;
import org.eclipse.tm4e.core.internal.theme.reader.ThemeReader;
import org.eclipse.tm4e.core.theme.IRawTheme;
import org.jf.baksmali.Baksmali;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int LANGUAGE_JAVA = 0;
    private static final int LANGUAGE_KOTLIN = 1;

    private AlertDialog loadingDialog;

    public JavaProject javaProject;

    // It's a variable that stores an object temporarily, for e.g. if you want to access a local
    // variable in a lambda expression, etc.
    private String temp;

    public String currentWorkingFilePath;
    public Indexer indexer;
    public static String BUILD_STATUS = "BUILD_STATUS";
    public ActivityMainBinding binding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        javaProject = new JavaProject(new File(getIntent().getStringExtra("project_path")));

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setTitle(getProject().getProjectName());

        if (binding.root instanceof DrawerLayout) {
            DrawerLayout drawer = (DrawerLayout) binding.root;
            if (drawer != null) {
                var toggle =
                        new ActionBarDrawerToggle(
                                this,
                                drawer,
                                binding.toolbar,
                                R.string.app_name,
                                R.string.app_name);
                drawer.addDrawerListener(toggle);
                toggle.syncState();
            }
            tintAppBarLayout(SurfaceColors.SURFACE_2.getColor(this));
            UiUtilsKt.addSystemWindowInsetToPadding(binding.appbar, false, true, false, false);
        } else {
            binding.toolbar.setNavigationIcon(null);
        }

        binding.editor.setTypefaceText(
                ResourcesCompat.getFont(this, R.font.jetbrains_mono_regular));
        binding.editor.setTextSize(12);
        binding.editor.setEdgeEffectColor(Color.TRANSPARENT);
        binding.editor.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        binding.editor.setInputType(
                EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                        | EditorInfo.TYPE_CLASS_TEXT
                        | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                        | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        binding.editor
                .getComponent(EditorAutoCompletion.class)
                .setLayout(new CustomCompletionLayout());
        binding.editor
                .getComponent(EditorAutoCompletion.class)
                .setAdapter(new CustomCompletionItemAdapter());

        var props = binding.editor.getProps();
        props.overScrollEnabled = false;
        props.allowFullscreen = false;
        props.deleteEmptyLineFast = false;

        try {
            indexer = new Indexer(getProject().getProjectName(), getProject().getCacheDirPath());
            if (indexer.notHas("currentFile")) {
                indexer.put("currentFile", getProject().getSrcDirPath() + "Main.kt");
                indexer.flush();
            }
            currentWorkingFilePath = indexer.getString("currentFile");
            getSupportActionBar().setSubtitle(new File(currentWorkingFilePath).getName());
        } catch (Exception e) {
            dialog("Exception", e.getMessage(), true);
        }
        if (currentWorkingFilePath.endsWith(".kt")) {
            setEditorLanguage(LANGUAGE_KOTLIN);
        } else if (currentWorkingFilePath.endsWith(".java")
                || currentWorkingFilePath.endsWith(".jav")) {
            setEditorLanguage(LANGUAGE_JAVA);
        }

        final var file = new File(currentWorkingFilePath);
        if (file.exists()) {
            try {
                binding.editor.setText(FileUtil.readFile(file));
            } catch (IOException e) {
                dialog("Failed to read file", getString(e), true);
            }
        }

        if (!new File(FileUtil.getClasspathDir(), "android.jar").exists()) {
            ZipUtil.unzipFromAssets(this, "android.jar.zip", FileUtil.getClasspathDir());
        }
        final var stdlib = new File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.7.10.jar");
        if (!stdlib.exists()) {
            try {
                FileUtil.writeFile(
                        getAssets().open("kotlin-stdlib-1.7.20-Beta.jar"),
                        stdlib.getAbsolutePath());
            } catch (Exception e) {
                showErr(getString(e));
            }
        }
        if (!new File(FileUtil.getDataDir(), "compiler-modules").exists()) {
            ZipUtil.unzipFromAssets(this, "compiler-modules.zip", FileUtil.getDataDir());
        }
        var output = new File(FileUtil.getClasspathDir() + "/core-lambda-stubs.jar");
        if (!output.exists()) {
            try {
                FileUtil.writeFile(
                        getAssets().open("core-lambda-stubs.jar"), output.getAbsolutePath());
            } catch (Exception e) {
                showErr(getString(e));
            }
        }

        /* Create Loading Dialog */
        buildLoadingDialog();

        binding.btnDisassemble.setOnClickListener(v -> disassemble());
        binding.btnSmali2java.setOnClickListener(v -> decompile());
        binding.btnSmali.setOnClickListener(v -> smali());

        binding.editor
                .getText()
                .addContentListener(
                        new ProblemMarker(binding.editor, currentWorkingFilePath, getProject()));
        UiUtilsKt.addSystemWindowInsetToPadding(binding.bottomButtons, false, false, false, true);
    }

    /* Build Loading Dialog - This dialog shows on code compilation */
    void buildLoadingDialog() {
        var builder = new MaterialAlertDialogBuilder(this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView =
                getLayoutInflater().inflate(R.layout.compile_loading_dialog, viewGroup, false);
        builder.setView(dialogView);
        loadingDialog = builder.create();
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
    }

    /* To change user Stage TextView Text to compiling stage in CompileTask.java */
    void changeLoadingDialogBuildStage(String stage) {
        if (loadingDialog.isShowing()) {
            /* So, this method is also triggered from another thread (Compile.java)
             * We need to make sure that this code is executed on main thread */
            runOnUiThread(
                    () -> {
                        TextView stage_txt = loadingDialog.findViewById(R.id.stage_txt);
                        stage_txt.setText(stage);
                    });
        }
    }

    /* Loads a file from a path to the editor */
    public void loadFileToEditor(String path) throws IOException, JSONException {
        if (!indexer.getString("currentFile").equals(path)) {
            indexer.put("currentFile", path);
            indexer.flush();
            final String code =
                    binding.editor
                            .getText()
                            .toString()
                            .replace("System.exit(", "System.out.println(\"Exit code \" + ");
            FileUtil.writeFile(currentWorkingFilePath, code);
        }
        var newWorkingFile = new File(path);
        binding.editor.setText(FileUtil.readFile(newWorkingFile));

        if (path.endsWith(".kt")) {
            setEditorLanguage(LANGUAGE_KOTLIN);
        } else if (path.endsWith(".java") || path.endsWith(".jav")) {
            setEditorLanguage(LANGUAGE_JAVA);
        }

        binding.editor
                .getText()
                .addContentListener(new ProblemMarker(binding.editor, path, getProject()));
        currentWorkingFilePath = path;
        getSupportActionBar().setSubtitle(new File(path).getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.format_menu_button:
                CoroutineUtil.execute(
                        () -> {
                            if (compiler_settings
                                    .getString("formatter", javaFormatters[0])
                                    .equals(javaFormatters[0])) {
                                var formatter =
                                        new GoogleJavaFormatter(
                                                binding.editor.getText().toString());
                                temp = formatter.format();
                            } else {
                                var formatter =
                                        new EclipseJavaFormatter(
                                                binding.editor.getText().toString());
                                temp = formatter.format();
                            }
                        });
                binding.editor.setText(temp);
                break;
            case R.id.settings_menu_button:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            case R.id.run_menu_button:
                compile(true, false);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            final String code =
                    binding.editor
                            .getText()
                            .toString()
                            .replace("System.exit(", "System.out.println(\"Exit code \" + ");
            FileUtil.writeFile(currentWorkingFilePath, code);
        } catch (IOException e) {
            Log.e(TAG, e + "while saving a file");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.editor.release();
    }

    private void tintAppBarLayout(@ColorInt int targetColor) {
        int appBarColor = getAppBarLayoutColor();
        if (appBarColor == targetColor) {
            return;
        }
        var valueAnimator = ValueAnimator.ofArgb(appBarColor, targetColor);
        valueAnimator.addUpdateListener(
                animation ->
                        binding.appbar.setBackgroundColor((int) valueAnimator.getAnimatedValue()));
        valueAnimator.setDuration(200).start();
    }

    private int getAppBarLayoutColor() {
        var background = binding.appbar.getBackground();
        if (background == null || background.getClass() != ColorDrawable.class) {
            binding.appbar.setBackgroundColor(getColorAttr(this, android.R.attr.colorBackground));
        }
        return ((ColorDrawable) binding.appbar.getBackground()).getColor();
    }

    /* Shows a snackbar indicating that there were problems during compilation */
    public void showErr(final String e) {
        Snackbar.make(binding.bottomButtons, "An error occurred", Snackbar.LENGTH_INDEFINITE)
                .setAction("Show error", v -> dialog("Failed...", e, true))
                .show();
    }

    private void compile(boolean execute, boolean blockMainThread) {
        final int id = 1;
        final var intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final var pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        final var channel =
                new NotificationChannel(
                        BUILD_STATUS, "Build Status", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Shows the current build status.");

        final var manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        final var mBuilder =
                new Notification.Builder(this, BUILD_STATUS)
                        .setContentTitle("Build Status")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        loadingDialog.show(); // Show Loading Dialog
        final var compilationThread =
                new CompileTask(
                        this,
                        execute,
                        new CompileTask.CompilerListeners() {
                            @Override
                            public void onCurrentBuildStageChanged(String stage) {
                                mBuilder.setContentText(stage);
                                manager.notify(id, mBuilder.build());
                                changeLoadingDialogBuildStage(stage);
                            }

                            @Override
                            public void onSuccess() {
                                loadingDialog.dismiss();
                                mBuilder.setContentText("Success!");
                                manager.notify(id, mBuilder.build());
                            }

                            @Override
                            public void onFailed(String errorMessage) {
                                mBuilder.setContentText("Failure");
                                manager.notify(id, mBuilder.build());
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                showErr(errorMessage);
                            }
                        });
        if (!blockMainThread) {
            compilationThread.start();
        } else {
            CoroutineUtil.execute(compilationThread);
        }
    }

    private void setEditorLanguage(int lang) {
        if (lang == LANGUAGE_JAVA) {
            binding.editor.setColorScheme(getColorScheme());
            binding.editor.setEditorLanguage(getJavaLanguage());
        } else if (lang == LANGUAGE_KOTLIN) {
            binding.editor.setColorScheme(getColorScheme());
            binding.editor.setEditorLanguage(getKotlinLanguage());
        } else {
            binding.editor.setColorScheme(getColorScheme());
            binding.editor.setEditorLanguage(new EmptyLanguage());
        }
    }

    private TextMateColorScheme getColorScheme() {
        try {
            IRawTheme rawTheme;
            if (isDarkMode()) {
                rawTheme =
                        ThemeReader.readThemeSync(
                                "darcula.json", getAssets().open("textmate/darcula.json"));
            } else {
                rawTheme =
                        ThemeReader.readThemeSync(
                                "light.tmTheme", getAssets().open("textmate/light.tmTheme"));
            }
            return TextMateColorScheme.create(rawTheme);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private Language getJavaLanguage() {
        try {
            return TextMateLanguage.create(
                    "java.tmLanguage.json",
                    getAssets().open("textmate/java/syntaxes/java.tmLanguage.json"),
                    new InputStreamReader(
                            getAssets().open("textmate/java/language-configuration.json")),
                    getColorScheme().getRawTheme());
        } catch (IOException e) {
            Log.e(TAG, e + " while loading kotlin language");
            return new EmptyLanguage();
        }
    }

    private Language getKotlinLanguage() {
        try {
            return TextMateLanguage.create(
                    "kotlin.tmLanguage",
                    getAssets().open("textmate/kotlin/syntaxes/kotlin.tmLanguage"),
                    new InputStreamReader(
                            getAssets().open("textmate/kotlin/language-configuration.json")),
                    getColorScheme().getRawTheme());
        } catch (IOException e) {
            Log.e(TAG, e + " while loading kotlin language");
            return new EmptyLanguage();
        }
    }

    private Language getSmaliLanguage() {
        try {
            return TextMateLanguage.create(
                    "smali.tmLanguage.json",
                    getAssets().open("textmate/smali/syntaxes/smali.tmLanguage.json"),
                    new InputStreamReader(
                            getAssets().open("textmate/smali/language-configuration.json")),
                    getColorScheme().getRawTheme());
        } catch (IOException e) {
            Log.e(TAG, e + " while loading smali language");
            return new EmptyLanguage();
        }
    }

    public void smali() {
        try {
            final var classes = getClassesFromDex();
            if (classes == null) return;
            listDialog(
                    "Select a class to extract source",
                    classes,
                    (d, pos) -> {
                        final var claz = classes[pos];
                        final var smaliFile =
                                new File(
                                        getProject().getBinDirPath(),
                                        "smali" + "/" + claz.replace(".", "/") + ".smali");
                        try {
                            final var opcodes = Opcodes.forApi(32);
                            final var options = new BaksmaliOptions();

                            final var dexFile =
                                    DexFileFactory.loadDexFile(
                                            new File(getProject().getBinDirPath(), "classes.dex"),
                                            opcodes);
                            options.apiLevel = 32;
                            CoroutineUtil.execute(
                                    () ->
                                            Baksmali.disassembleDexFile(
                                                    dexFile,
                                                    new File(getProject().getBinDirPath(), "smali"),
                                                    1,
                                                    options));
                        } catch (Exception e) {
                            dialog("Unable to load dex file", getString(e), true);
                            return;
                        }

                        var edi = new CodeEditor(this);
                        edi.setTypefaceText(
                                ResourcesCompat.getFont(this, R.font.jetbrains_mono_regular));
                        edi.setColorScheme(getColorScheme());
                        edi.setTextSize(12);
                        edi.setEditorLanguage(getSmaliLanguage());

                        try {
                            edi.setText(FileUtil.readFile(smaliFile));
                        } catch (IOException e) {
                            dialog("Failed to read file", getString(e), true);
                            return;
                        }

                        var dialog = new AlertDialog.Builder(this).setView(edi).create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                    });
        } catch (Throwable e) {
            dialog("Failed to extract smali source", getString(e), true);
        }
    }

    public void decompile() {
        final var classes = getClassesFromDex();
        if (classes == null) return;
        listDialog(
                "Select a class to extract source",
                classes,
                (d, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    var decompiled = "";
                    CoroutineUtil.execute(
                            () -> {
                                try {
                                    decompiled = new FernFlowerDecompiler().decompile(claz, new File(getProject().getBinDirPath() + "classes"));
                                } catch (Exception e) {
                                    dialog("Failed to decompile...", getString(e), true);
                                }
                            });

                    final var edi = new CodeEditor(this);
                    edi.setTypefaceText(
                            ResourcesCompat.getFont(this, R.font.jetbrains_mono_regular));
                    edi.setColorScheme(getColorScheme());
                    edi.setTextSize(12);
                    edi.setEditorLanguage(getJavaLanguage());

                    edi.setText(decompiled);

                    var dialog = new AlertDialog.Builder(this).setView(edi).create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                });
    }

    public void disassemble() {
        final var classes = getClassesFromDex();
        if (classes == null) return;
        listDialog(
                "Select a class to disassemble",
                classes,
                (d, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    var disassembled = "";
                    try {
                        if (compiler_settings.getString("disassembler", "Javap").equals("Javap")) {
                            disassembled =
                                    new JavapDisassembler(
                                                    getProject().getBinDirPath()
                                                            + "classes"
                                                            + "/"
                                                            + claz
                                                            + ".class")
                                            .disassemble();
                        } else {
                            disassembled =
                                    new EclipseDisassembler(
                                                    getProject().getBinDirPath()
                                                            + "classes"
                                                            + "/"
                                                            + claz
                                                            + ".class")
                                            .disassemble();
                        }
                    } catch (Throwable e) {
                        dialog("Failed to disassemble", getString(e), true);
                    }

                    var edi = new CodeEditor(this);
                    edi.setTypefaceText(
                            ResourcesCompat.getFont(this, R.font.jetbrains_mono_regular));
                    edi.setColorScheme(getColorScheme());
                    edi.setTextSize(12);
                    edi.setEditorLanguage(getJavaLanguage());

                    edi.setText(disassembled);

                    var dialog = new AlertDialog.Builder(this).setView(edi).create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                });
    }

    public void listDialog(String title, String[] items, DialogInterface.OnClickListener listener) {
        runOnUiThread(
                () -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(title)
                            .setItems(items, listener)
                            .create()
                            .show();
                });
    }

    public void dialog(String title, final String message, boolean copyButton) {
        var dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null);
        if (copyButton)
            dialog.setNeutralButton(
                    "Copy",
                    (dialogInterface, i) -> {
                        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
                                .setPrimaryClip(ClipData.newPlainText("", message));
                    });
        dialog.create().show();
    }

    /* Used to find all the compiled classes from the output dex file */
    public String[] getClassesFromDex() {
        try {
            var dex = new File(getProject().getBinDirPath().concat("classes.dex"));
            /* If the project doesn't seem to have the dex file, just recompile it */
            if (!dex.exists()) {
                compile(false, true);
            }
            var classes = new ArrayList<String>();
            var dexfile = DexFileFactory.loadDexFile(dex.getAbsolutePath(), Opcodes.forApi(32));
            for (var f : dexfile.getClasses().toArray(new ClassDef[0])) {
                var name = f.getType().replace("/", "."); // convert class name to standard form
                classes.add(name.substring(1, name.length() - 1));
            }
            return classes.toArray(new String[0]);
        } catch (Exception e) {
            dialog("Failed to get available classes in dex...", getString(e), true);
            return null;
        }
    }

    private String getString(Throwable e) {
        return Log.getStackTraceString(e);
    }

    public JavaProject getProject() {
        return javaProject;
    }
}
