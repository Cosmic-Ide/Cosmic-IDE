package org.cosmic.ide.activity;

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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.snackbar.Snackbar;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;

import org.cosmic.ide.R;
import org.cosmic.ide.ApplicationLoader;
import org.cosmic.ide.activity.model.MainViewModel;
import org.cosmic.ide.activity.model.FileViewModel;
import org.cosmic.ide.activity.editor.CodeEditorView;
import org.cosmic.ide.activity.editor.adapter.PageAdapter;
import org.cosmic.ide.android.code.decompiler.FernFlowerDecompiler;
import org.cosmic.ide.android.code.disassembler.*;
import org.cosmic.ide.android.code.formatter.*;
import org.cosmic.ide.android.task.jar.JarTask;
import org.cosmic.ide.common.Indexer;
import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.common.util.ZipUtil;
import org.cosmic.ide.common.util.UniqueNameBuilder;
import org.cosmic.ide.compiler.CompileTask;
import org.cosmic.ide.databinding.ActivityMainBinding;
import org.cosmic.ide.fragment.CodeEditorFragment;
import org.cosmic.ide.project.JavaProject;
import org.cosmic.ide.util.Constants;
import org.cosmic.ide.util.UiUtilsKt;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends BaseActivity {

    public static final String BUILD_STATUS = "BUILD_STATUS";
    public static final String TAG = MainActivity.class.getSimpleName();

    private String temp;
    private BottomSheetDialog loadingDialog;

    private JavaProject javaProject;
    private MainViewModel mainViewModel;
    private FileViewModel fileViewModel;
    private PageAdapter tabsAdapter;
    private Indexer indexer;

    public ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        fileViewModel = new ViewModelProvider(this).get(FileViewModel.class);
        tabsAdapter = new PageAdapter(getSupportFragmentManager(), getLifecycle());
        javaProject = new JavaProject(new File(getIntent().getStringExtra(Constants.PROJECT_PATH)));

        setSupportActionBar(binding.toolbar);
 
        UiUtilsKt.addSystemWindowInsetToPadding(binding.appbar, false, true, false, false);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.viewPager, false, true, false, false);
        
        if (binding.root instanceof DrawerLayout) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(false);
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
                binding.toolbar.setNavigationOnClickListener(v -> {
                    if (binding.root instanceof DrawerLayout) {
                        if (drawer.isDrawerOpen(GravityCompat.START)) {
                            mainViewModel.setDrawerState(false);
                        } else if (!drawer.isDrawerOpen(GravityCompat.START)) {
                            mainViewModel.setDrawerState(true);
                        }
                    }
                });
                drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                    @Override
                    public void onDrawerOpened(@NonNull View p1) {
                        mainViewModel.setDrawerState(true);
                    }
    
                    @Override
                    public void onDrawerClosed(@NonNull View p1) {
                        mainViewModel.setDrawerState(false);
                    }
                });
            }
        } else {
            binding.toolbar.setNavigationIcon(null);
        }

        unzipFiles();
        buildLoadingDialog();

        fileViewModel.refreshNode(getProject().getRootFile());

        mainViewModel.setFiles(new ArrayList<>());
        mainViewModel.getToolbarTitle().observe(this, getSupportActionBar()::setTitle);
        mainViewModel.setToolbarTitle(getProject().getProjectName());
        binding.viewPager.setAdapter(tabsAdapter);
        binding.viewPager.setUserInputEnabled(false);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mainViewModel.setCurrentPosition(position);
            }
        });

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabUnselected(TabLayout.Tab p1) {
                /* There is no need to save the file here
                 * as each time you enter text the file
                 * is automatically saved. */
            }

            @Override
            public void onTabReselected(TabLayout.Tab p1) {
                PopupMenu popup = new PopupMenu(MainActivity.this, p1.view);
                popup.getMenu().add(0, 0, 1, getString(R.string.close));
                popup.getMenu().add(0, 1, 2, getString(R.string.close_others));
                popup.getMenu().add(0, 2, 3, getString(R.string.close_all));
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 0:
                            mainViewModel.removeFile(mainViewModel.getCurrentFile());
                            break;
                        case 1:
                            mainViewModel.removeOthers(mainViewModel.getCurrentFile());
                            break;
                        case 2:
                            mainViewModel.clear();
                    }
                    return true;
                });
                popup.show();
            }

            @Override
            public void onTabSelected(TabLayout.Tab p1) {
                updateTab(p1, p1.getPosition());
            }
        });
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, true, false, this::updateTab).attach();

        mainViewModel.getFiles().observe(this, files -> {
            tabsAdapter.submitList(files);
            if (files.isEmpty()) {
                binding.tabLayout.removeAllTabs();
                binding.tabLayout.setVisibility(View.GONE);
                binding.viewPager.setVisibility(View.GONE);
                binding.emptyContainer.setVisibility(View.VISIBLE);
            } else {
                binding.tabLayout.setVisibility(View.VISIBLE);
                binding.viewPager.setVisibility(View.VISIBLE);
                binding.emptyContainer.setVisibility(View.GONE);
            }
        });
        mainViewModel.getCurrentPosition().observe(this, position -> {
            binding.viewPager.setCurrentItem(position);
        });
        if (binding.root instanceof DrawerLayout) {
            mainViewModel.getDrawerState().observe(this, isOpen -> {
                if (isOpen) {
                    ((DrawerLayout) binding.root).open();
                } else {
                    ((DrawerLayout) binding.root).close();
                }
            });
        }

        if (savedInstanceState != null) {
            restoreViewState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveAll();
        if (binding.root instanceof DrawerLayout) {
            outState.putBoolean(Constants.DRAWER_STATE,
                    ((DrawerLayout) binding.root).isDrawerOpen(GravityCompat.START));
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreViewState(@NonNull Bundle state) {
        if (binding.root instanceof DrawerLayout) {
            boolean b = state.getBoolean(Constants.DRAWER_STATE, false);
            mainViewModel.setDrawerState(b);
        }
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
                String tag = "f" + tabsAdapter.getItemId(binding.viewPager.getCurrentItem());
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment instanceof CodeEditorFragment) {
                    CoroutineUtil.execute(
                             () -> {
                                 if (settings.getString(
                                                "key_java_formatter",
                                                getString(R.string.google_java_formatter))
                                            .equals(getString(R.string.google_java_formatter))) {
                                     var formatter =
                                            new GoogleJavaFormatter(
                                                    ((CodeEditorFragment) fragment).getEditor().getText().toString());
                                     temp = formatter.format();
                                 } else {
                                     var formatter =
                                            new EclipseJavaFormatter(
                                                    ((CodeEditorFragment) fragment).getEditor().getText().toString());
                                     temp = formatter.format();
                                 }
                            });
                    ((CodeEditorFragment) fragment).getEditor().setText(temp);
                }
                break;
            case R.id.settings_menu_button:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            case R.id.run_menu_button:
                compile(true, false);
                break;
            case R.id.smali_menu_button:
                smali();
                break;
            case R.id.disassemble_menu_button:
                disassemble();
                break;
            case R.id.smali2java_menu_button:
                decompile();
                break;
            case R.id.action_undo:
                String _tag = "f" + tabsAdapter.getItemId(binding.viewPager.getCurrentItem());
                Fragment _fragment = getSupportFragmentManager().findFragmentByTag(_tag);
                if (_fragment instanceof CodeEditorFragment) {
                    ((CodeEditorFragment) _fragment).undo();
                }
                break;
            case R.id.action_redo:
                String __tag = "f" + tabsAdapter.getItemId(binding.viewPager.getCurrentItem());
                Fragment __fragment = getSupportFragmentManager().findFragmentByTag(__tag);
                if (__fragment instanceof CodeEditorFragment) {
                    ((CodeEditorFragment) __fragment).redo();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void unzipFiles() {
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
                showError(getString(e));
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
                showError(getString(e));
            }
        }
    }

    private void updateTab(TabLayout.Tab tab, int pos) {
        File currentFile = Objects.requireNonNull(mainViewModel.getFiles().getValue()).get(pos);
        tab.setText(currentFile != null ? getUniqueTabName(currentFile) : "Unknown");
    }

    private String getUniqueTabName(@NonNull File currentFile) {
        int sameFileNameCount = 0;
        UniqueNameBuilder<File> builder = new UniqueNameBuilder<>("", "/");

        for (File file : Objects.requireNonNull(mainViewModel.getFiles().getValue())) {
            if (file.getName().equals(currentFile.getName())) {
                sameFileNameCount++;
            }
            builder.addPath(file, file.getPath());
        }

        if (sameFileNameCount > 1) {
            return builder.getShortPath(currentFile);
        } else {
            return currentFile.getName();
        }
    }

    private void buildLoadingDialog() {
        loadingDialog = new BottomSheetDialog(this);
        loadingDialog.setContentView(R.layout.compile_loading_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
    }

    /* So, this method is also triggered from another thread (Compile.java)
     * We need to make sure that this code is executed on main thread */
    private void changeLoadingDialogBuildStage(String currentStage) {
        if (loadingDialog.isShowing()) {
            runOnUiThread(
                    () -> {
                        TextView stage = loadingDialog.findViewById(R.id.stage_txt);
                        stage.setText(currentStage);
                    });
        }
    }

    private void compile(boolean execute, boolean blockMainThread) {
        final int id = 201;
        final var intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final var pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        final var notifChannel =
                new NotificationChannel(
                        BUILD_STATUS, "Compiler", NotificationManager.IMPORTANCE_NONE);
        notifChannel.setDescription("Foreground notification for the compiler");

        final var notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifManager.createNotificationChannel(notifChannel);

        final var notifBuilder =
                new Notification.Builder(this, BUILD_STATUS)
                        .setContentTitle(getProject().getProjectName())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        loadingDialog.show();
        final var compilationThread =
                new CompileTask(
                        this,
                        execute,
                        new CompileTask.CompilerListeners() {
                            private boolean compileSuccess = true;
                            @Override
                            public void onCurrentBuildStageChanged(String stage) {
                                changeLoadingDialogBuildStage(stage);
                            }

                            @Override
                            public void onSuccess() {
                                notifBuilder.setContentText("Success");
                                notifManager.notify(id, notifBuilder.build());
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                            }

                            @Override
                            public void onFailed(String errorMessage) {
                                compileSuccess = false;
                                notifBuilder.setContentText("Failure");
                                notifManager.notify(id, notifBuilder.build());
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                showError(errorMessage);
                            }

                            @Override
                            public boolean isSuccessTillNow() {
                                return compileSuccess;
                            }
                        });
        if (!blockMainThread) {
            compilationThread.start();
        } else {
            CoroutineUtil.execute(compilationThread);
        }
    }

    private TextMateColorScheme getColorScheme() {
        try {
            IRawTheme rawTheme;
            if (ApplicationLoader.Companion.isDarkMode(this)) {
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
            return new EmptyLanguage();
        }
    }

    private void smali() {
        try {
            final var classes = getClassesFromDex();
            if (classes == null) return;
            listDialog(
                    "Select a class to show smali",
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
                            dialog("Failed to open file", getString(e), true);
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

    private void decompile() {
        final var classes = getClassesFromDex();
        if (classes == null) return;
        listDialog(
                "Select a class to decompile",
                classes,
                (d, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    CoroutineUtil.execute(
                            () -> {
                                try {
                                    new JarTask().doFullTask(getProject());
                                    temp =
                                            new FernFlowerDecompiler()
                                                    .decompile(
                                                            claz,
                                                            new File(
                                                                    getProject().getBinDirPath()
                                                                            + "classes.jar"));
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

                    edi.setText(temp);

                    var dialog = new AlertDialog.Builder(this).setView(edi).create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                });
    }

    private void disassemble() {
        final var classes = getClassesFromDex();
        if (classes == null) return;
        listDialog(
                "Select a class to disassemble",
                classes,
                (d, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    var disassembled = "";
                    try {
                        if (settings.getString("key_java_disassembler", getString(R.string.javap))
                                .equals(getString(R.string.javap))) {
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
        var dialog = new MaterialAlertDialogBuilder(this)
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

    /* Shows a snackbar indicating that there were problems during compilation */
    private void showError(String exception) {
        Snackbar.make(binding.snackbarContainer, "An error occurred", Snackbar.LENGTH_INDEFINITE)
                .setAction("Show error", v -> dialog("Failed...", exception.toString(), true))
                .show();
    }

    private String getString(Throwable e) {
        return Log.getStackTraceString(e);
    }

    public JavaProject getProject() {
        return javaProject;
    }

    public void saveAll() {
        for (int i = 0; i < tabsAdapter.getItemCount(); i++) {
            String tag = "f" + tabsAdapter.getItemId(i);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment instanceof CodeEditorFragment) {
                ((CodeEditorFragment) fragment).save();
            }
        }
    }
}