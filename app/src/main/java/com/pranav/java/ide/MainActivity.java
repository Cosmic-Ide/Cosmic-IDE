package com.pranav.java.ide;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.pranav.ProblemMarker;
import com.pranav.android.code.disassembler.*;
import com.pranav.android.code.formatter.*;
import com.pranav.common.Indexer;
import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.FileUtil;
import com.pranav.common.util.ZipUtil;
import com.pranav.java.ide.compiler.CompileTask;
import com.pranav.java.ide.ui.TreeViewDrawer;
import com.pranav.java.ide.ui.utils.UiUtilsKt;
import com.pranav.project.mode.JavaProject;
import com.pranav.project.mode.JavaTemplate;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;

import org.benf.cfr.reader.Main;
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

public final class MainActivity extends AppCompatActivity {

    public CodeEditor editor;
    public SharedPreferences prefs;
    public DrawerLayout drawer;

    private AlertDialog loadingDialog;

    private Bundle projectDatas;
    public JavaProject javaProject;

    // It's a variable that stores an object temporarily, for e.g. if you want to access a local
    // variable in a lambda expression, etc.
    private String temp;

    public String currentWorkingFilePath;
    public Indexer indexer;
    public static String BUILD_STATUS = "BUILD_STATUS";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        prefs = getSharedPreferences("compiler_settings", MODE_PRIVATE);
        projectDatas = getIntent().getExtras();

        var projectPath = projectDatas.getString("project_path"); 

        javaProject = new JavaProject(new File(projectPath));

        var toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setTitle(getProject().getProjectName());

        var appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        UiUtilsKt.addSystemWindowInsetToPadding(appBarLayout, false, true, false, false);

        drawer = findViewById(R.id.mDrawerLayout);
        var toggle =
                new ActionBarDrawerToggle(
                        this, drawer, toolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        editor = findViewById(R.id.editor);
        editor.setTypefaceText(Typeface.MONOSPACE);
        editor.setColorScheme(getColorScheme());
        final var language = getTextMateLanguageForJava();
        language.setEnableJavaCompletions(true);
        editor.setEditorLanguage(language);
        editor.setTextSize(12);
        editor.setPinLineNumber(true);

        try {
            indexer = new Indexer(getProject().getProjectName(), getProject().getCacheDirPath());
            if (indexer.notHas("currentFile")) {
                indexer.put("currentFile", getProject().getSrcDirPath() + "Main.java");
                indexer.flush();
            }
            currentWorkingFilePath = indexer.getString("currentFile");
        } catch (JSONException e) {
            dialog("JsonException", e.getMessage(), true);
        }

        final var file = new File(currentWorkingFilePath);
        if (file.exists()) {
            try {
                editor.setText(FileUtil.readFile(file));
            } catch (IOException e) {
                dialog("Cannot read file", getString(e), true);
            }
        } else {
            try {
                FileUtil.writeFileFromString(
                        getProject().getSrcDirPath() + 
                        "Main.java", JavaTemplate.getClassTemplate(null, "Main", true));
                editor.setText(JavaTemplate.getClassTemplate(null, "Main", true));
            } catch (Exception e) {
                dialog("Cannot create file", getString(e), true);
            }
        }

        if (!new File(FileUtil.getClasspathDir(), "android.jar").exists()) {
            ZipUtil.unzipFromAssets(
                    MainActivity.this, "android.jar.zip", FileUtil.getClasspathDir());
        }
        if (!new File(FileUtil.getDataDir(), "compiler-modules").exists()) {
            ZipUtil.unzipFromAssets(
                    MainActivity.this, "compiler-modules.zip", FileUtil.getDataDir());
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

        findViewById(R.id.btn_disassemble).setOnClickListener(v -> disassemble());
        findViewById(R.id.btn_smali2java).setOnClickListener(v -> decompile());
        findViewById(R.id.btn_smali).setOnClickListener(v -> smali());

        editor.getText().addContentListener(new ProblemMarker(editor, currentWorkingFilePath, getProject()));
        var scrollView = (HorizontalScrollView) findViewById(R.id.scrollview);
        UiUtilsKt.addSystemWindowInsetToPadding(scrollView, false, false, false, true);
    }

    /* Build Loading Dialog - This dialog shows on code compilation */
    void buildLoadingDialog() {
        var builder = new MaterialAlertDialogBuilder(MainActivity.this);
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
        var newWorkingFile = new File(path);
        editor.setText(FileUtil.readFile(newWorkingFile));
        editor.getText().addContentListener(new ProblemMarker(editor, path, getProject()));
        indexer.put("currentFile", path);
        indexer.flush();
        currentWorkingFilePath = path;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.format_menu_button) {
            ConcurrentUtil.execute(
                    () -> {
                        if (prefs.getString("formatter", "Google Java Formatter")
                                .equals("Google Java Formatter")) {
                            var formatter = new GoogleJavaFormatter(editor.getText().toString());
                            temp = formatter.format();
                        } else {
                            var formatter = new EclipseJavaFormatter(editor.getText().toString());
                            temp = formatter.format();
                        }
                    });
            editor.setText(temp);
        } else if (id == R.id.settings_menu_button) {
            var intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        } else if (id == R.id.run_menu_button) {
            compile(true, false);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        editor.release();
    }

    /* Shows a snackbar indicating that there were problems during compilation */
    public void showErr(final String e) {
        Snackbar.make(
                        (LinearLayout) findViewById(R.id.container),
                        "An error occurred",
                        Snackbar.LENGTH_INDEFINITE)
                .setAction("Show error", v -> dialog("Failed...", e, true))
                .show();
    }

    private void compile(boolean execute, boolean blockMainThread) {
        final int id = 1;
        final var intent = new Intent(MainActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final var pendingIntent =
                PendingIntent.getActivity(
                        MainActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        final var channel =
                new NotificationChannel(
                        BUILD_STATUS, "Build Status", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Shows the current build status.");

        final var manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        final var mBuilder =
                new Notification.Builder(MainActivity.this, BUILD_STATUS)
                        .setContentTitle("Build Status")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        loadingDialog.show(); // Show Loading Dialog
        final var compilationThread =
                new CompileTask(
                        MainActivity.this,
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
                                manager.cancel(id);
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
            ConcurrentUtil.execute(compilationRunnable);
        }
    }

    private TextMateColorScheme getColorScheme() {
        return new TextMateColorScheme(getDarculaTheme());
    }

    private IRawTheme getDarculaTheme() {
        try {
            var rawTheme =
                    ThemeReader.readThemeSync(
                            "darcula.json", getAssets().open("textmate/darcula.json"));
            return rawTheme;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TextMateLanguage getTextMateLanguageForJava() {
        try {
            var language =
                    TextMateLanguage.create(
                            "java.tmLanguage.json",
                            getAssets().open("textmate/java/syntaxes/java.tmLanguage.json"),
                            new InputStreamReader(
                                    getAssets().open("textmate/java/language-configuration.json")),
                            getDarculaTheme());
            return language;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TextMateLanguage getTextMateLanguageForSmali() {
        try {
            var language =
                    TextMateLanguage.create(
                            "smali.tmLanguage.json",
                            getAssets().open("textmate/smali/syntaxes/smali.tmLanguage.json"),
                            new InputStreamReader(
                                    getAssets().open("textmate/smali/language-configuration.json")),
                            getDarculaTheme());
            return language;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void smali() {
        try {
            var classes = getClassesFromDex();
            if (classes == null) return;
            listDialog(
                    "Select a class to extract source",
                    classes,
                    (d, pos) -> {
                        var claz = classes[pos];
                        var smaliFile =
                                new File(
                                        getProject().getBinDirPath(),
                                        "smali" + "/" + claz.replace(".", "/") + ".smali");
                        try {
                            var opcodes = Opcodes.forApi(32);
                            var options = new BaksmaliOptions();

                            var dexFile =
                                    DexFileFactory.loadDexFile(
                                            new File(getProject().getBinDirPath(), "classes.dex"), opcodes);
                            options.apiLevel = 26;
                            ConcurrentUtil.execute(
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

                        var edi = new CodeEditor(MainActivity.this);
                        edi.setTypefaceText(Typeface.MONOSPACE);
                        edi.setColorScheme(getColorScheme());
                        edi.setEditorLanguage(getTextMateLanguageForSmali());
                        edi.setTextSize(12);

                        try {
                            edi.setText(FileUtil.readFile(smaliFile));
                        } catch (IOException e) {
                            dialog("Cannot read file", getString(e), true);
                            return;
                        }

                        var dialog =
                                new AlertDialog.Builder(MainActivity.this).setView(edi).create();
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
                (dialog, pos) -> {
                    var claz = classes[pos].replace(".", "/");
                    var args =
                            new String[] {
                                getProject().getBinDirPath()
                                        + "classes"
                                        + "/"
                                        + claz
                                        + // full class name
                                        ".class",
                                "--extraclasspath",
                                FileUtil.getClasspathDir() + "android.jar",
                                "--outputdir",
                                getProject().getBinDirPath() + "cfr/"
                            };

                    ConcurrentUtil.execute(
                            () -> {
                                try {
                                    Main.main(args);
                                } catch (Exception e) {
                                    dialog("Failed to decompile...", getString(e), true);
                                }
                            });

                    var edi = new CodeEditor(MainActivity.this);
                    edi.setTypefaceText(Typeface.MONOSPACE);
                    edi.setColorScheme(getColorScheme());
                    edi.setEditorLanguage(getTextMateLanguageForJava());
                    edi.setTextSize(12);

                    var decompiledFile = new File(getProject().getBinDirPath() + "cfr" + "/" + claz + ".java");

                    try {
                        edi.setText(FileUtil.readFile(decompiledFile));
                    } catch (IOException e) {
                        dialog("Cannot read file", getString(e), true);
                    }

                    var d = new AlertDialog.Builder(MainActivity.this).setView(edi).create();
                    d.setCanceledOnTouchOutside(true);
                    d.show();
                });
    }

    public void disassemble() {
        final var classes = getClassesFromDex();
        if (classes == null) return;
        listDialog(
                "Select a class to disassemble",
                classes,
                (dialog, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    var edi = new CodeEditor(MainActivity.this);
                    edi.setTypefaceText(Typeface.MONOSPACE);
                    edi.setColorScheme(getColorScheme());
                    edi.setEditorLanguage(getTextMateLanguageForJava());
                    edi.setTextSize(12);

                    try {
                        var disassembled = "";
                        if (prefs.getString("disassembler", "Javap").equals("Javap")) {
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
                                                            +"/"
                                                            + claz
                                                            + ".class")
                                            .disassemble();
                        }
                        edi.setText(disassembled);

                    } catch (Throwable e) {
                        dialog("Failed to disassemble", getString(e), true);
                    }
                    var d = new AlertDialog.Builder(MainActivity.this).setView(edi).create();
                    d.setCanceledOnTouchOutside(true);
                    d.show();
                });
    }

    public void listDialog(String title, String[] items, DialogInterface.OnClickListener listener) {
        runOnUiThread(
                () -> {
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle(title)
                            .setItems(items, listener)
                            .create()
                            .show();
                });
    }

    public void dialog(String title, final String message, boolean copyButton) {
        var dialog =
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null);
        if (copyButton)
            dialog.setNeutralButton(
                    "COPY",
                    (dialogInterface, i) -> {
                        ((ClipboardManager)
                                        getSystemService(getApplicationContext().CLIPBOARD_SERVICE))
                                .setPrimaryClip(ClipData.newPlainText("clipboard", message));
                    });
        dialog.create().show();
    }

    /* Used to find all the compiled classes from the output dex file */
    public String[] getClassesFromDex() {
        try {
            var dex = new File(getProject().getBinDirPath().concat("classes.dex"));
            /* If the project doesn't seem to have been compiled yet, compile it */
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

    private String getString(final Throwable e) {
        return Log.getStackTraceString(e);
    }
    
    public JavaProject getProject() {
        return javaProject;
    }
}
