package org.cosmic.ide.activity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmic.ide.R
import org.cosmic.ide.activity.model.FileViewModel
import org.cosmic.ide.activity.model.GitViewModel
import org.cosmic.ide.activity.model.MainViewModel
import org.cosmic.ide.android.task.jar.JarTask
import org.cosmic.ide.code.decompiler.FernFlowerDecompiler
import org.cosmic.ide.code.disassembler.JavapDisassembler
import org.cosmic.ide.code.formatter.GoogleJavaFormatter
import org.cosmic.ide.code.formatter.ktfmtFormatter
import org.cosmic.ide.common.util.CoroutineUtil.execute
import org.cosmic.ide.common.util.CoroutineUtil.inParallel
import org.cosmic.ide.compiler.CompileTask
import org.cosmic.ide.compiler.CompileTask.CompilerListeners
import org.cosmic.ide.databinding.ActivityMainBinding
import org.cosmic.ide.fragment.CodeEditorFragment
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.ui.editor.adapter.PageAdapter
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.Constants
import org.cosmic.ide.util.EditorUtil.getColorScheme
import org.cosmic.ide.util.EditorUtil.javaLanguage
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.ClassDef
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var compileTask: CompileTask
    private lateinit var temp: String
    private lateinit var loadingDialog: BottomSheetDialog
    lateinit var project: JavaProject
    private lateinit var mainViewModel: MainViewModel
    private val gitViewModel: GitViewModel = GitViewModel()
    private lateinit var tabsAdapter: PageAdapter
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val fileViewModel = ViewModelProvider(this)[FileViewModel::class.java]
        val gitViewModel = ViewModelProvider(this)[GitViewModel::class.java]
        tabsAdapter = PageAdapter(supportFragmentManager, lifecycle)
        project = JavaProject(File(intent.getStringExtra(Constants.PROJECT_PATH).toString()))
        binding.appBar.addSystemWindowInsetToPadding(top = true)
        if (binding.root is DrawerLayout) {
            val drawer = binding.root as DrawerLayout
            binding.toolbar.setNavigationOnClickListener {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    mainViewModel.setDrawerState(false)
                } else if (!drawer.isDrawerOpen(GravityCompat.START)) {
                    mainViewModel.setDrawerState(true)
                }
            }
            drawer.addDrawerListener(
                object : DrawerLayout.SimpleDrawerListener() {
                    override fun onDrawerOpened(p1: View) {
                        mainViewModel.setDrawerState(true)
                    }

                    override fun onDrawerClosed(p1: View) {
                        mainViewModel.setDrawerState(false)
                    }
                })
        }
        buildLoadingDialog()
        fileViewModel.refreshNode(project.rootFile)
        mainViewModel.setFiles(project.indexer.getList())
        supportActionBar?.setTitle(
            project.projectName
        )
        binding.viewPager.adapter = tabsAdapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(
            object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    mainViewModel.setCurrentPosition(position)
                }
            })
        binding.tabLayout.addOnTabSelectedListener(
            object : OnTabSelectedListener {
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {
                    val popup = PopupMenu(this@MainActivity, tab.view)
                    popup.menu.add(0, 0, 1, getString(R.string.menu_close_file))
                    popup.menu.add(0, 1, 2, getString(R.string.menu_close_others))
                    popup.menu.add(0, 2, 3, getString(R.string.menu_close_all))
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            0 -> mainViewModel.removeFile(
                                mainViewModel.currentFile
                            )
                            1 -> mainViewModel.removeOthers(
                                mainViewModel.currentFile
                            )
                            2 -> mainViewModel.clear()
                        }
                        true
                    }
                    popup.show()
                }

                override fun onTabSelected(p1: TabLayout.Tab) {
                    updateTab(p1, p1.position)
                }
            })
        TabLayoutMediator(
            binding.tabLayout, binding.viewPager, true, false
        ) { tab, pos ->
            updateTab(
                tab,
                pos
            )
        }
            .attach()
        gitViewModel.setPath(project.projectDirPath)
        gitViewModel.postCheckout = {
            fileViewModel.refreshNode(project.rootFile)
        }
        gitViewModel.onSave = {
            mainViewModel.clear()
        }
        mainViewModel
            .files
            .observe(
                this
            ) { files ->
                tabsAdapter.submitList(files)
                if (files.isEmpty()) {
                    binding.viewPager.visibility = View.GONE
                    binding.tabLayout.removeAllTabs()
                    binding.tabLayout.visibility = View.GONE
                    binding.emptyContainer.visibility = View.VISIBLE
                    mainViewModel.setCurrentPosition(-1)
                } else {
                    binding.tabLayout.visibility = View.VISIBLE
                    binding.emptyContainer.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                }
            }
        mainViewModel
            .currentPosition
            .observe(
                this
            ) { position ->
                if (position == -1) {
                    return@observe
                }
                binding.viewPager.currentItem = position
            }
        if (binding.root is DrawerLayout) {
            mainViewModel
                .drawerState
                .observe(
                    this
                ) { isOpen: Boolean ->
                    if (isOpen) {
                        (binding.root as DrawerLayout).open()
                    } else {
                        (binding.root as DrawerLayout).close()
                    }
                }
        }
        savedInstanceState?.let { restoreViewState(it) }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        if (binding.root is DrawerLayout) {
            outState.putBoolean(
                Constants.DRAWER_STATE,
                (binding.root as DrawerLayout).isDrawerOpen(GravityCompat.START)
            )
        }
        saveOpenedFiles()
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        if (File(project.rootFile, ".git").exists()) {
            menu.findItem(R.id.action_git).isVisible = true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val tag = "f" + tabsAdapter.getItemId(binding.viewPager.currentItem)
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        val id = item.itemId
        if (id == R.id.action_format && fragment is CodeEditorFragment) {
            execute {
                val current = mainViewModel.currentFile
                if (current?.extension.equals("java")) {
                    val formatter = GoogleJavaFormatter(
                        fragment
                            .getEditor()
                            .text
                            .toString()
                    )
                    temp = formatter.format()
                } else if (current?.extension.equals("kt") || current?.extension.equals("kts")) {
                    ktfmtFormatter(current.toString()).format()
                    try {
                        temp =
                            current?.readText()!!
                    } catch (e: IOException) {
                        Log.d(
                            TAG,
                            getString(R.string.error_file_open),
                            e
                        )
                    }
                } else {
                    temp = fragment
                        .getEditor()
                        .text
                        .toString()
                }
            }
            fragment.getEditor().setText(temp)
        } else if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else if (id == R.id.action_run) {
            compile(execute = true, blockMainThread = false)
        } else if (id == R.id.action_smali) {
            smali()
        } else if (id == R.id.action_disassemble) {
            disassemble()
        } else if (id == R.id.action_class2java) {
            decompile()
        } else if (id == R.id.action_undo) {
            if (fragment is CodeEditorFragment) {
                fragment.undo()
            }
        } else if (id == R.id.action_redo) {
            if (fragment is CodeEditorFragment) {
                fragment.redo()
            }
        } else if (id == R.id.action_git) {
            startActivity(Intent(this, GitActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun restoreViewState(state: Bundle) {
        if (binding.root is DrawerLayout) {
            val b = state.getBoolean(Constants.DRAWER_STATE, false)
            mainViewModel.setDrawerState(b)
        }
    }

    private fun saveOpenedFiles() {
        try {
            project
                .indexer
                .put(
                    "lastOpenedFiles", mainViewModel.files.value!!
                )
                .flush()
        } catch (e: JSONException) {
            Log.e(TAG, "Cannot save opened files", e)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        saveOpenedFiles()
    }

    private fun updateTab(tab: TabLayout.Tab, pos: Int) {
        val currentFile = mainViewModel.files.value!!.get(pos)
        tab.text = if (currentFile != null) currentFile.name else "Unknown"
    }

    private fun buildLoadingDialog() {
        loadingDialog = BottomSheetDialog(this)
        loadingDialog.setContentView(R.layout.dialog_compile_running)
        loadingDialog.setCancelable(false)
        loadingDialog.setCanceledOnTouchOutside(false)
    }

    /* So, this method is also triggered from another thread (CompileTask.java)
     * We need to make sure that this code is executed on main thread */
    private fun changeLoadingDialogBuildStage(currentStage: String) {
        if (loadingDialog.isShowing) {
            runOnUiThread {
                val stage =
                    loadingDialog.findViewById<TextView>(R.id.stage_txt)!!
                stage.text = currentStage
            }
        }
    }

    private fun compile(execute: Boolean, blockMainThread: Boolean) {
        val id = 201
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notifChannel = NotificationChannel(
            BUILD_STATUS, "Compiler", NotificationManager.IMPORTANCE_DEFAULT
        )
        notifChannel.description = "Foreground notification for the compiler status"
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.createNotificationChannel(notifChannel)
        val notifBuilder = Notification.Builder(this, BUILD_STATUS)
            .setContentTitle(project.projectName)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        loadingDialog.show()
        compileTask = CompileTask(
            this,
            object : CompilerListeners {
                private var compileSuccess = true
                override fun onCurrentBuildStageChanged(stage: String) {
                    changeLoadingDialogBuildStage(stage)
                }

                override fun onSuccess() {
                    notifBuilder.setContentText(
                        getString(R.string.compilation_result_success)
                    )
                    notifManager.notify(id, notifBuilder.build())
                    if (loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }
                }

                override fun onFailed(errorMessage: String) {
                    compileSuccess = false
                    notifBuilder.setContentText(
                        getString(R.string.compilation_result_failed)
                    )
                    notifManager.notify(id, notifBuilder.build())
                    if (loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }
                    Handler(Looper.getMainLooper())
                        .post {
                            AndroidUtilities.showSimpleAlert(
                                this@MainActivity,
                                getString(
                                    R.string.compilation_result_failed
                                ),
                                errorMessage,
                                getString(R.string.dialog_close),
                                getString(R.string.copy_stacktrace)
                            ) { _, which ->
                                if (which
                                    == DialogInterface.BUTTON_NEGATIVE
                                ) {
                                    AndroidUtilities
                                        .copyToClipboard(
                                            errorMessage
                                        )
                                }
                            }
                        }
                }

                override fun isSuccessTillNow(): Boolean {
                    return compileSuccess
                }
            })

        compileTask.setExecution(execute)
        if (!blockMainThread) {
            inParallel(compileTask)
        } else {
            execute(compileTask)
        }
    }

    private fun smali() {
        val classes = classesFromDex ?: return
        listDialog(
            getString(R.string.select_smali_class),
            classes
        ) { _, pos ->
            val claz = classes.get(pos)
            val smaliFile = File(
                project.binDirPath,
                "smali" + "/" + claz.replace(".", "/") + ".smali"
            )
            execute {
                try {
                    val dexFile: DexBackedDexFile = DexFileFactory.loadDexFile(
                        File(
                            project.binDirPath,
                            "classes.dex"
                        ),
                        Opcodes.forApi(32)
                    )
                    val options = BaksmaliOptions()
                    options.apiLevel = 32
                    Baksmali.disassembleDexFile(
                        dexFile,
                        File(project.binDirPath, "smali"),
                        1,
                        options
                    )
                } catch (e: Throwable) {
                    AndroidUtilities.showSimpleAlert(
                        this,
                        getString(R.string.error_file_extract_smali),
                        e.localizedMessage,
                        getString(R.string.dialog_close),
                        getString(R.string.copy_stacktrace)
                    ) { _, which ->
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            AndroidUtilities.copyToClipboard(
                                e.localizedMessage
                            )
                        }
                    }
                }
            }
            mainViewModel.addFile(smaliFile)
        }
    }

    private fun decompile() {
        val classes = classesFromDex ?: return
        listDialog(
            getString(R.string.select_class_decompile),
            classes
        ) { _, pos ->
            val claz = classes.get(pos).replace(".", "/")
            execute {
                try {
                    JarTask().doFullTask(project)
                    temp = FernFlowerDecompiler()
                        .decompile(
                            claz,
                            File(
                                project.binDirPath
                                        + "classes.jar"
                            )
                        )
                } catch (e: Exception) {
                    AndroidUtilities.showSimpleAlert(
                        this,
                        getString(R.string.error_class_decompile),
                        e.localizedMessage,
                        getString(R.string.dialog_close),
                        getString(R.string.copy_stacktrace),
                        (DialogInterface.OnClickListener { _, which ->
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                AndroidUtilities.copyToClipboard(
                                    e.localizedMessage
                                )
                            }
                        })
                    )
                }
            }
            val edi = CodeEditor(this)
            edi.typefaceText = ResourcesCompat.getFont(
                this,
                R.font.jetbrains_mono_light
            )
            edi.colorScheme = getColorScheme(this)
            edi.setTextSize(12f)
            edi.setEditorLanguage(javaLanguage)
            edi.setText(temp)
            val dialog =
                AlertDialog.Builder(this).setView(edi).create()
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()
        }
    }

    private fun disassemble() {
        val classes = classesFromDex ?: return
        listDialog(
            getString(R.string.select_class_disassemble),
            classes
        ) { _, pos ->
            val claz = classes.get(pos).replace(".", "/")
            var disassembled = ""
            try {
                disassembled = JavapDisassembler(
                    project.binDirPath
                            + "classes"
                            + "/"
                            + claz
                            + ".class"
                )
                    .disassemble()
            } catch (e: Throwable) {
                AndroidUtilities.showSimpleAlert(
                    this,
                    getString(R.string.error_class_disassemble),
                    e.localizedMessage,
                    getString(R.string.dialog_close),
                    getString(R.string.copy_stacktrace)
                ) { _, which ->
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        AndroidUtilities.copyToClipboard(e.localizedMessage)
                    }
                }
            }
            val edi = CodeEditor(this)
            edi.typefaceText = ResourcesCompat.getFont(
                this,
                R.font.jetbrains_mono_light
            )
            edi.colorScheme = getColorScheme(this)
            edi.setTextSize(12f)
            edi.setEditorLanguage(javaLanguage)
            edi.setText(disassembled)
            val dialog =
                AlertDialog.Builder(this).setView(edi).create()
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()
        }
    }

    fun listDialog(
        title: String?,
        items: Array<String>,
        listener: DialogInterface.OnClickListener
    ) {
        runOnUiThread {
            if (items.size == 1) {
                listener.onClick(null, 0)
                return@runOnUiThread
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setItems(items, listener)
                .create()
                .show()
        }
    }// convert class name to standard form/* If the project doesn't seem to have the dex file, just recompile it */

    /* Used to find all the compiled classes from the output dex file */
    val classesFromDex: Array<String>?
        get() = try {
            val dex = File(project.binDirPath + "classes.dex")
            /* If the project doesn't seem to have the dex file, just recompile it */if (!dex.exists()) {
                compile(execute = false, blockMainThread = true)
            }
            val classes = ArrayList<String>()
            val dexfile = DexFileFactory.loadDexFile(dex.absolutePath, Opcodes.forApi(32))
            for (f in dexfile.classes.toTypedArray<ClassDef>()) {
                val name = f.type.replace("/", ".") // convert class name to standard form
                classes.add(name.substring(1, name.length - 1))
            }
            classes.toTypedArray()
        } catch (e: Exception) {
            AndroidUtilities.showSimpleAlert(
                this,
                getString(R.string.error_classes_get_dex),
                e.localizedMessage,
                getString(R.string.dialog_close),
                getString(R.string.copy_stacktrace)
            ) { _, which ->
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    AndroidUtilities.copyToClipboard(e.localizedMessage)
                }
            }
            null
        }

    companion object {
        const val BUILD_STATUS = "BUILD_STATUS"
        const val TAG = "MainActivity"
    }
}