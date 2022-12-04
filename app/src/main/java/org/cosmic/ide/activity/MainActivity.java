package org.cosmic.ide.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import io.github.rosemoe.sora.widget.CodeEditor;

import org.cosmic.ide.R;
import org.cosmic.ide.activity.model.FileViewModel;
import org.cosmic.ide.activity.model.GitViewModel;
import org.cosmic.ide.activity.model.MainViewModel;
import org.cosmic.ide.android.task.jar.JarTask;
import org.cosmic.ide.code.decompiler.FernFlowerDecompiler;
import org.cosmic.ide.code.disassembler.JavapDisassembler;
import org.cosmic.ide.code.formatter.*;
import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.common.util.ZipUtil;
import org.cosmic.ide.compiler.CompileTask;
import org.cosmic.ide.databinding.ActivityMainBinding;
import org.cosmic.ide.fragment.CodeEditorFragment;
import org.cosmic.ide.project.JavaProject;
import org.cosmic.ide.ui.editor.adapter.PageAdapter;
import org.cosmic.ide.util.AndroidUtilities;
import org.cosmic.ide.util.Constants;
import org.cosmic.ide.util.EditorUtil;
import org.cosmic.ide.util.UiUtilsKt;
import org.jf.baksmali.Baksmali;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import kotlin.Unit;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    public static final String BUILD_STATUS = "BUILD_STATUS";
    public static final String TAG = "MainActivity";
    private CompileTask compileTask = null;

    private String temp;
    private BottomSheetDialog loadingDialog;

    private JavaProject javaProject;
    private MainViewModel mainViewModel;
    private GitViewModel gitViewModel;
    private PageAdapter tabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityMainBinding.inflate(getLayoutInflater()));
        setSupportActionBar(binding.toolbar);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        var fileViewModel = new ViewModelProvider(this).get(FileViewModel.class);
        var gitViewModel = new ViewModelProvider(this).get(GitViewModel.class);
        tabsAdapter = new PageAdapter(getSupportFragmentManager(), getLifecycle());
        javaProject = new JavaProject(new File(getIntent().getStringExtra(Constants.PROJECT_PATH)));

        CoroutineUtil.inParallel(this::unzipFiles);

        UiUtilsKt.addSystemWindowInsetToPadding(binding.appBar, false, true, false, false);

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.viewPager,
                (vi, insets) -> {
                    boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                    int bottomInset;

                    if (imeVisible) {
                        bottomInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    } else {
                        bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                    }
                    binding.viewPager.setPadding(0, 0, 0, bottomInset);
                    return insets;
                });

        if (binding.getRoot() instanceof DrawerLayout) {
            var drawer = (DrawerLayout) binding.getRoot();
            binding.toolbar.setNavigationOnClickListener(
                    v -> {
                        if (drawer.isDrawerOpen(GravityCompat.START)) {
                            mainViewModel.setDrawerState(false);
                        } else if (!drawer.isDrawerOpen(GravityCompat.START)) {
                            mainViewModel.setDrawerState(true);
                        }
                    });
            drawer.addDrawerListener(
                    new DrawerLayout.SimpleDrawerListener() {
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

        buildLoadingDialog();

        fileViewModel.refreshNode(javaProject.getRootFile());
        mainViewModel.setFiles(javaProject.getIndexer().getList());

        Objects.requireNonNull(getSupportActionBar()).setTitle(javaProject.getProjectName());

        binding.viewPager.setAdapter(tabsAdapter);
        binding.viewPager.setUserInputEnabled(false);
        binding.viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        mainViewModel.setCurrentPosition(position);
                    }
                });

        binding.tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabUnselected(TabLayout.Tab p1) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab p1) {
                        PopupMenu popup = new PopupMenu(MainActivity.this, p1.view);
                        popup.getMenu().add(0, 0, 1, getString(R.string.menu_close_file));
                        popup.getMenu().add(0, 1, 2, getString(R.string.menu_close_others));
                        popup.getMenu().add(0, 2, 3, getString(R.string.menu_close_all));
                        popup.setOnMenuItemClickListener(
                                item -> {
                                    switch (item.getItemId()) {
                                        case 0:
                                            mainViewModel.removeFile(
                                                    mainViewModel.getCurrentFile());
                                            break;
                                        case 1:
                                            mainViewModel.removeOthers(
                                                    mainViewModel.getCurrentFile());
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
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, true, false, this::updateTab)
                .attach();

        gitViewModel.setPath(javaProject.getProjectDirPath());
        gitViewModel.setPostCheckout(() -> {
            fileViewModel.refreshNode(javaProject.getRootFile());
            return Unit.INSTANCE;
        });
        gitViewModel.setOnSave(() -> {
            mainViewModel.clear();
            return Unit.INSTANCE;
        });

        mainViewModel
                .getFiles()
                .observe(
                        this,
                        files -> {
                            tabsAdapter.submitList(files);
                            if (files.isEmpty()) {
                                binding.viewPager.setVisibility(View.GONE);
                                binding.tabLayout.removeAllTabs();
                                binding.tabLayout.setVisibility(View.GONE);
                                binding.emptyContainer.setVisibility(View.VISIBLE);
                                mainViewModel.setCurrentPosition(-1);
                            } else {
                                binding.tabLayout.setVisibility(View.VISIBLE);
                                binding.emptyContainer.setVisibility(View.GONE);
                                binding.viewPager.setVisibility(View.VISIBLE);
                            }
                        });
        mainViewModel
                .getCurrentPosition()
                .observe(
                        this,
                        position -> {
                            if (position == -1) {
                                return;
                            }
                            binding.viewPager.setCurrentItem(position);
                        });
        if (binding.getRoot() instanceof DrawerLayout) {
            mainViewModel
                    .getDrawerState()
                    .observe(
                            this,
                            isOpen -> {
                                if (isOpen) {
                                    ((DrawerLayout) binding.getRoot()).open();
                                } else {
                                    ((DrawerLayout) binding.getRoot()).close();
                                }
                            });
        }

        if (savedInstanceState != null) {
            restoreViewState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding.getRoot() instanceof DrawerLayout) {
            outState.putBoolean(
                    Constants.DRAWER_STATE,
                    ((DrawerLayout) binding.getRoot()).isDrawerOpen(GravityCompat.START));
        }
        saveOpenedFiles();
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (new File(javaProject.getRootFile(), ".git").exists()) {
            menu.findItem(R.id.action_git).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String tag = "f" + tabsAdapter.getItemId(binding.viewPager.getCurrentItem());
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        final var id = item.getItemId();

        if (id == R.id.action_format && fragment instanceof CodeEditorFragment) {
            CoroutineUtil.execute(
                    () -> {
                        String current = Objects.requireNonNull(mainViewModel.getCurrentFile()).getAbsolutePath();
                        if (current.endsWith(".java")) {
                            var formatter =
                                    new GoogleJavaFormatter(
                                            ((CodeEditorFragment) fragment)
                                                    .getEditor()
                                                    .getText()
                                                    .toString());
                            temp = formatter.format();
                        } else if (current.endsWith(".kt") || current.endsWith(".kts")) {
                            new ktfmtFormatter(current).format();
                            try {
                                temp = FileUtil.readFile(new File(current));
                            } catch (IOException e) {
                                Log.d(TAG, getString(R.string.error_file_open), e);
                            }
                        } else {
                            temp =
                                    ((CodeEditorFragment) fragment)
                                            .getEditor()
                                            .getText()
                                            .toString();
                        }
                    });
            ((CodeEditorFragment) fragment).getEditor().setText(temp);
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_run) {
            compile(true, false);
        } else if (id == R.id.action_smali) {
            smali();
        } else if (id == R.id.action_disassemble) {
            disassemble();
        } else if (id == R.id.action_class2java) {
            decompile();
        } else if (id == R.id.action_undo) {
            if (fragment instanceof CodeEditorFragment) {
                ((CodeEditorFragment) fragment).undo();
            }
        } else if (id == R.id.action_redo) {
            if (fragment instanceof CodeEditorFragment) {
                ((CodeEditorFragment) fragment).redo();
            }
        } else if (id == R.id.action_git) {
            startActivity(new Intent(this, GitActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void restoreViewState(@NonNull Bundle state) {
        if (binding.getRoot() instanceof DrawerLayout) {
            boolean b = state.getBoolean(Constants.DRAWER_STATE, false);
            mainViewModel.setDrawerState(b);
        }
    }

    private void saveOpenedFiles() {
        try {
            javaProject
                    .getIndexer()
                    .put("lastOpenedFiles", Objects.requireNonNull(mainViewModel.getFiles().getValue()))
                    .flush();
        } catch (JSONException e) {
            Log.e(TAG, "Cannot save opened files", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveOpenedFiles();
    }

    private void unzipFiles() {
        if (!new File(FileUtil.getClasspathDir(), "android.jar").exists()) {
            ZipUtil.unzipFromAssets(this, "android.jar.zip", FileUtil.getClasspathDir());
        }
        final var stdlib = new File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.7.20.jar");
        if (!stdlib.exists()) {
            try {
                FileUtil.writeFile(
                        getAssets().open("kotlin-stdlib-1.7.20.jar"), stdlib.getAbsolutePath());
            } catch (Exception e) {
                AndroidUtilities.showSimpleAlert(
                        this,
                        getString(R.string.error_file_unzip),
                        e.getLocalizedMessage(),
                        getString(R.string.dialog_close),
                        getString(R.string.copy_stacktrace),
                        ((dialog, which) -> {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                AndroidUtilities.copyToClipboard(e.getLocalizedMessage());
                            }
                        }));
            }
        }
        final var commonStdlib =
                new File(FileUtil.getClasspathDir(), "kotlin-stdlib-common-1.7.20.jar");
        if (!commonStdlib.exists()) {
            try {
                FileUtil.writeFile(
                        getAssets().open("kotlin-stdlib-common-1.7.20.jar"),
                        commonStdlib.getAbsolutePath());
            } catch (Exception e) {
                AndroidUtilities.showSimpleAlert(
                        this,
                        getString(R.string.error_file_unzip),
                        e.getLocalizedMessage(),
                        getString(R.string.dialog_close),
                        getString(R.string.copy_stacktrace),
                        ((dialog, which) -> {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                AndroidUtilities.copyToClipboard(e.getLocalizedMessage());
                            }
                        }));
            }
        }
        if (new File(FileUtil.getDataDir(), "compiler-modules").exists()) {
            FileUtil.deleteFile(FileUtil.getDataDir() + "compiler-modules");
        }
        var output = new File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar");
        if (!output.exists()) {
            try {
                FileUtil.writeFile(
                        getAssets().open("core-lambda-stubs.jar"), output.getAbsolutePath());
            } catch (Exception e) {
                AndroidUtilities.showSimpleAlert(
                        this,
                        getString(R.string.error_file_unzip),
                        e.getLocalizedMessage(),
                        getString(R.string.dialog_close),
                        getString(R.string.copy_stacktrace),
                        ((dialog, which) -> {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                AndroidUtilities.copyToClipboard(e.getLocalizedMessage());
                            }
                        }));
            }
        }
    }

    private void updateTab(TabLayout.Tab tab, int pos) {
        File currentFile = Objects.requireNonNull(mainViewModel.getFiles().getValue()).get(pos);
        tab.setText(currentFile != null ? currentFile.getName() : "Unknown");
    }

    private void buildLoadingDialog() {
        loadingDialog = new BottomSheetDialog(this);
        loadingDialog.setContentView(R.layout.dialog_compile_running);
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
    }

    /* So, this method is also triggered from another thread (CompileTask.java)
     * We need to make sure that this code is executed on main thread */
    private void changeLoadingDialogBuildStage(String currentStage) {
        if (loadingDialog.isShowing()) {
            runOnUiThread(
                    () -> {
                        TextView stage = loadingDialog.findViewById(R.id.stage_txt);
                        assert stage != null;
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
                        BUILD_STATUS, "Compiler", NotificationManager.IMPORTANCE_DEFAULT);
        notifChannel.setDescription("Foreground notification for the compiler status");

        final var notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifManager.createNotificationChannel(notifChannel);

        final var notifBuilder =
                new Notification.Builder(this, BUILD_STATUS)
                        .setContentTitle(javaProject.getProjectName())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        loadingDialog.show();
        if (compileTask == null) {
            compileTask =
                    new CompileTask(
                            this,
                            new CompileTask.CompilerListeners() {
                                private boolean compileSuccess = true;

                                @Override
                                public void onCurrentBuildStageChanged(String stage) {
                                    changeLoadingDialogBuildStage(stage);
                                }

                                @Override
                                public void onSuccess() {
                                    notifBuilder.setContentText(
                                            getString(R.string.compilation_result_success));
                                    notifManager.notify(id, notifBuilder.build());
                                    if (loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                }

                                @Override
                                public void onFailed(String errorMessage) {
                                    compileSuccess = false;
                                    notifBuilder.setContentText(
                                            getString(R.string.compilation_result_failed));
                                    notifManager.notify(id, notifBuilder.build());
                                    if (loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    new Handler(Looper.getMainLooper())
                                            .post(
                                                    () -> AndroidUtilities.showSimpleAlert(
                                                            MainActivity.this,
                                                            getString(
                                                                    R.string
                                                                            .compilation_result_failed),
                                                            errorMessage,
                                                            getString(R.string.dialog_close),
                                                            getString(R.string.copy_stacktrace),
                                                            ((dialog, which) -> {
                                                                if (which
                                                                        == DialogInterface
                                                                        .BUTTON_NEGATIVE) {
                                                                    AndroidUtilities
                                                                            .copyToClipboard(
                                                                                    errorMessage);
                                                                }
                                                            })));
                                }

                                @Override
                                public boolean isSuccessTillNow() {
                                    return compileSuccess;
                                }
                            });
        }
        compileTask.setExecution(execute);
        if (!blockMainThread) {
            CoroutineUtil.inParallel(compileTask);
        } else {
            CoroutineUtil.execute(compileTask);
        }
    }

    private void smali() {
        final var classes = getClassesFromDex();
        if (classes == null) return;
        listDialog(
                getString(R.string.select_smali_class),
                classes,
                (d, pos) -> {
                    final var claz = classes[pos];
                    final var smaliFile =
                            new File(
                                    javaProject.getBinDirPath(),
                                    "smali" + "/" + claz.replace(".", "/") + ".smali");

                    CoroutineUtil.execute(
                            () -> {
                                try {
                                    final var dexFile =
                                            DexFileFactory.loadDexFile(
                                                    new File(
                                                            javaProject.getBinDirPath(),
                                                            "classes.dex"),
                                                    Opcodes.forApi(32));
                                    final var options = new BaksmaliOptions();
                                    options.apiLevel = 32;
                                    Baksmali.disassembleDexFile(
                                            dexFile,
                                            new File(javaProject.getBinDirPath(), "smali"),
                                            1,
                                            options);
                                } catch (Throwable e) {
                                    AndroidUtilities.showSimpleAlert(
                                            this,
                                            getString(R.string.error_file_extract_smali),
                                            e.getLocalizedMessage(),
                                            getString(R.string.dialog_close),
                                            getString(R.string.copy_stacktrace),
                                            ((dialog, which) -> {
                                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                                    AndroidUtilities.copyToClipboard(
                                                            e.getLocalizedMessage());
                                                }
                                            }));
                                }
                            });

                    mainViewModel.addFile(smaliFile);
                });
    }

    private void decompile() {
        final var classes = getClassesFromDex();
        if (classes == null) return;

        listDialog(
                getString(R.string.select_class_decompile),
                classes,
                (d, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    CoroutineUtil.execute(
                            () -> {
                                try {
                                    new JarTask().doFullTask(javaProject);
                                    temp =
                                            new FernFlowerDecompiler()
                                                    .decompile(
                                                            claz,
                                                            new File(
                                                                    javaProject.getBinDirPath()
                                                                            + "classes.jar"));
                                } catch (Exception e) {
                                    AndroidUtilities.showSimpleAlert(
                                            this,
                                            getString(R.string.error_class_decompile),
                                            e.getLocalizedMessage(),
                                            getString(R.string.dialog_close),
                                            getString(R.string.copy_stacktrace),
                                            ((dialog, which) -> {
                                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                                    AndroidUtilities.copyToClipboard(
                                                            e.getLocalizedMessage());
                                                }
                                            }));
                                }
                            });

                    final var edi = new CodeEditor(this);
                    edi.setTypefaceText(ResourcesCompat.getFont(this, R.font.jetbrains_mono_light));
                    edi.setColorScheme(EditorUtil.INSTANCE.getColorScheme(this));
                    edi.setTextSize(12);
                    edi.setEditorLanguage(EditorUtil.INSTANCE.getJavaLanguage());

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
                getString(R.string.select_class_disassemble),
                classes,
                (d, pos) -> {
                    var claz = classes[pos].replace(".", "/");

                    var disassembled = "";
                    try {
                        disassembled =
                                new JavapDisassembler(
                                        javaProject.getBinDirPath()
                                                + "classes"
                                                + "/"
                                                + claz
                                                + ".class")
                                        .disassemble();
                    } catch (Throwable e) {
                        AndroidUtilities.showSimpleAlert(
                                this,
                                getString(R.string.error_class_disassemble),
                                e.getLocalizedMessage(),
                                getString(R.string.dialog_close),
                                getString(R.string.copy_stacktrace),
                                ((dialog, which) -> {
                                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                                        AndroidUtilities.copyToClipboard(e.getLocalizedMessage());
                                    }
                                }));
                    }

                    var edi = new CodeEditor(this);
                    edi.setTypefaceText(ResourcesCompat.getFont(this, R.font.jetbrains_mono_light));
                    edi.setColorScheme(EditorUtil.INSTANCE.getColorScheme(this));
                    edi.setTextSize(12);
                    edi.setEditorLanguage(EditorUtil.INSTANCE.getJavaLanguage());

                    edi.setText(disassembled);

                    var dialog = new AlertDialog.Builder(this).setView(edi).create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                });
    }

    public void listDialog(String title, String[] items, DialogInterface.OnClickListener listener) {
        runOnUiThread(
                () -> {
                    if (items.length == 1) {
                        listener.onClick(null, 0);
                        return;
                    }
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(title)
                            .setItems(items, listener)
                            .create()
                            .show();
                });
    }

    /* Used to find all the compiled classes from the output dex file */
    public String[] getClassesFromDex() {
        try {
            var dex = new File(javaProject.getBinDirPath() + "classes.dex");
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
            AndroidUtilities.showSimpleAlert(
                    this,
                    getString(R.string.error_classes_get_dex),
                    e.getLocalizedMessage(),
                    getString(R.string.dialog_close),
                    getString(R.string.copy_stacktrace),
                    ((dialog, which) -> {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            AndroidUtilities.copyToClipboard(e.getLocalizedMessage());
                        }
                    }));
            return null;
        }
    }

    public JavaProject getProject() {
        return javaProject;
    }
}
