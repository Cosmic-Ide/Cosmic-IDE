package org.cosmic.ide.fragment

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmic.ide.R
import org.cosmic.ide.android.task.jar.JarTask
import org.cosmic.ide.code.decompiler.FernFlowerDecompiler
import org.cosmic.ide.code.disassembler.JavapDisassembler
import org.cosmic.ide.code.formatter.GoogleJavaFormatter
import org.cosmic.ide.code.formatter.ktfmtFormatter
import org.cosmic.ide.common.util.CoroutineUtil.execute
import org.cosmic.ide.common.util.CoroutineUtil.inParallel
import org.cosmic.ide.compiler.CompileTask
import org.cosmic.ide.compiler.CompileTask.CompilerListeners
import org.cosmic.ide.databinding.FragmentHomeBinding
import org.cosmic.ide.databinding.DialogLibraryDownloaderBinding
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmic.ide.fragment.CodeEditorFragment
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.ui.adapter.EditorPageAdapter
import org.cosmic.ide.ui.model.FileViewModel
import org.cosmic.ide.ui.model.HomeViewModel
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.Constants
import org.cosmic.ide.util.EditorUtil.getColorScheme
import org.cosmic.ide.util.EditorUtil.javaLanguage
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.runOnUiThread
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.json.JSONException
import java.io.File
import java.io.IOException

class HomeFragment : Fragment() {
    private val args: HomeFragmentArgs by navArgs()
    private lateinit var temp: String
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    lateinit var project: JavaProject

    private var tabsAdapter: EditorPageAdapter? = null
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fileViewModel: FileViewModel
    private val loadingDialog by lazy {
        BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.dialog_compile_running)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
       }
    }
    private val libraryBinding by lazy {
        val layoutInflater = LayoutInflater.from(requireContext())
        DialogLibraryDownloaderBinding.inflate(layoutInflater)
    }
    private val libraryDialog: AlertDialog by lazy {
        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(
            context, AndroidUtilities.dialogFullWidthButtonsThemeOverlay
        ).apply {
            setTitle("Library Downloader")
            setView(libraryBinding.root)
            setPositiveButton(context.getString(R.string.create), null)
            setNegativeButton(context.getString(android.R.string.cancel), null)
        }
        dialog.create()
    }

    private val compileTask by lazy {
        CompileTask(
            this,
            object : CompilerListeners {
                private var compileSuccess = true
                private val context = requireContext()
                override fun onCurrentBuildStageChanged(stage: String) {
                    changeLoadingDialogBuildStage(stage)
                }

                override fun onSuccess() {
                    if (loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }
                }

                override fun onFailed(errorMessage: String) {
                    compileSuccess = false
                    Handler(Looper.getMainLooper())
                        .post {
                            if (loadingDialog.isShowing) {
                                loadingDialog.dismiss()
                            }
                            AndroidUtilities.showSimpleAlert(
                                context,
                                context.getString(R.string.compilation_result_failed),
                                errorMessage,
                                context.getString(R.string.dialog_close),
                                context.getString(R.string.copy_stacktrace)
                            ) { _, which ->
                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    AndroidUtilities
                                        .copyToClipboard(
                                            errorMessage
                                        )
                                }
                            }
                        }
                }

                override fun isSuccessTillNow(): Boolean = compileSuccess
            }
        )
    }

    companion object {
        const val TAG = "HomeFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        tabsAdapter = EditorPageAdapter(childFragmentManager, requireActivity().lifecycle)
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        fileViewModel = ViewModelProvider(requireActivity())[FileViewModel::class.java]
        project = JavaProject(File(args.projectPath))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.appBar.addSystemWindowInsetToPadding(top = true)

        binding.toolbar.subtitle = project.projectName
        binding.toolbar.setOnMenuItemClickListener {
            val tag = "f$tabsAdapter.getItemId(binding.viewPager.currentItem)"
            val fragment = childFragmentManager.findFragmentByTag(tag)
            val id = it.itemId
            if (id == R.id.action_format && fragment is CodeEditorFragment) {
                execute {
                    val current = homeViewModel.currentFile
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
                                requireContext().getString(R.string.error_file_open),
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
                findNavController().navigate(HomeFragmentDirections.actionShowSettingsFragment())
            } else if (id == R.id.action_run) {
                true.compile(blockMainThread = false)
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
            } else if (id == R.id.library_downloader) {
                showLibraryDialog()
            }
            true
        }

        initViewModelListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (binding.root is DrawerLayout) {
            val drawer = binding.root as DrawerLayout

            binding.toolbar.setNavigationOnClickListener {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    homeViewModel.setDrawerState(false)
                } else {
                    homeViewModel.setDrawerState(true)
                }
            }
            drawer.addDrawerListener(
                object : DrawerLayout.SimpleDrawerListener() {
                    override fun onDrawerOpened(p1: View) {
                        homeViewModel.setDrawerState(true)
                    }
                    override fun onDrawerClosed(p1: View) {
                        homeViewModel.setDrawerState(false)
                    }
                })
        }

        fileViewModel.refreshNode(project.rootFile)
        homeViewModel.clear()

        binding.viewPager.apply {
            adapter = tabsAdapter
            isUserInputEnabled = false
            registerOnPageChangeCallback(
                object : OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        homeViewModel.setCurrentPosition(position)
                    }
                })
        }

        binding.tabLayout.addOnTabSelectedListener(
            object : OnTabSelectedListener {
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {
                    val context = requireActivity()
                    val popup = PopupMenu(context, tab.view)
                    popup.menu.add(0, 0, 1, context.getString(R.string.menu_close_file))
                    popup.menu.add(0, 1, 2, context.getString(R.string.menu_close_others))
                    popup.menu.add(0, 2, 3, context.getString(R.string.menu_close_all))
                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            0 -> homeViewModel.removeFile(
                                homeViewModel.currentFile
                            )
                            1 -> homeViewModel.removeOthers(
                                homeViewModel.currentFile
                            )
                            2 -> homeViewModel.clear()
                        }
                        true
                    }
                    popup.show()
                }

                override fun onTabSelected(tab: TabLayout.Tab) {
                    updateTab(tab, tab.position)
                }
            })

        TabLayoutMediator(
            binding.tabLayout, binding.viewPager, true, false
        ) { tab, position ->
            updateTab(
                tab,
                position
            )
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        tabsAdapter = null
    }

    // private fun saveOpenedFiles() {
        // try {
            // project
                // .indexer
                // .putPathOpenFiles(homeViewModel.files.value!!)
                // .flush()
        // } catch (e: JSONException) {
            // Log.e(TAG, "Cannot save opened files", e)
        // }
    // }

    private fun initViewModelListeners() {
        homeViewModel
            .files
            .observe(
                viewLifecycleOwner
            ) { files ->
                tabsAdapter?.submitList(files)
                if (files.isEmpty()) {
                    binding.viewPager.visibility = View.GONE
                    binding.tabLayout.removeAllTabs()
                    binding.tabLayout.visibility = View.GONE
                    binding.emptyContainer.visibility = View.VISIBLE
                    homeViewModel.setCurrentPosition(-1)
                } else {
                    binding.tabLayout.visibility = View.VISIBLE
                    binding.emptyContainer.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                }
            }

        homeViewModel
            .currentPosition
            .observe(
                viewLifecycleOwner
            ) { position ->
                if (position == -1) {
                    return@observe
                }
                binding.viewPager.currentItem = position
            }

        if (binding.root is DrawerLayout) {
            homeViewModel
                .drawerState
                .observe(
                    viewLifecycleOwner
                ) { isOpen ->
                    if (isOpen) {
                        (binding.root as DrawerLayout).open()
                    } else {
                        (binding.root as DrawerLayout).close()
                    }
                }
        }
    }

    private fun updateTab(tab: TabLayout.Tab, position: Int) {
        val currentFile = homeViewModel.files.value!![position]
        tab.text = if (currentFile != null) currentFile.name else "Unknown"
    }

    private fun showLibraryDialog() {
        val createBtn: Button = libraryDialog.findViewById(android.R.id.button1)!!
        createBtn.setOnClickListener {
            val groupId = libraryBinding.groupId.text.toString()
            val artifactId = libraryBinding.artifactId.text.toString()
            val version = libraryBinding.version.text.toString()

            inParallel {
                val artifact = getArtifact(groupId, artifactId, version)

                if (artifact != null) {
                    artifact.downloadArtifact(File(project.libDirPath))
                    runOnUiThread {
                        AndroidUtilities.showToast("Library $artifactId downloaded.")
                        libraryDialog.dismiss()
                    }
                } else {
                    runOnUiThread {
                        AndroidUtilities.showToast("Library not available.")
                        libraryDialog.dismiss()
                    }
                }
            }
        }

        libraryBinding.groupId.setText("")
        libraryBinding.artifactId.setText("")
        libraryBinding.version.setText("")

        if (!libraryDialog.isShowing) {
            libraryDialog.show()
        }
    }

    /**
     * This method is also run from another thread
     * Need to make sure that the code is running on the main thread
     */
    private fun changeLoadingDialogBuildStage(currentStage: String) {
        if (loadingDialog.isShowing) {
            runOnUiThread {
                val stage: TextView =
                    loadingDialog.findViewById(R.id.stage_txt)!!
                stage.text = currentStage
            }
        }
    }

    private fun Boolean.compile(blockMainThread: Boolean) {
        compileTask.setExecution(this)
        loadingDialog.show()
        if (blockMainThread) {
            execute(compileTask)
            return
        }
        inParallel(compileTask)
    }

    private fun decompile() {
        val classes = classesFromDex ?: return
        val context = requireContext()
        val decompilerEditor = CodeEditor(context).apply {
            typefaceText = ResourcesCompat.getFont(
                context,
                R.font.jetbrains_mono_light
            )
            colorScheme = getColorScheme(context)
            setTextSize(12f)
            setEditorLanguage(javaLanguage)
        }

        listDialog(
            getString(R.string.select_class_decompile),
            classes
        ) { _, pos ->
            val className = classes[pos].replace(".", "/")

            execute {
                try {
                    JarTask().doFullTask(project)
                    decompilerEditor.setText(
                        FernFlowerDecompiler()
                            .decompile(
                                className,
                                File(
                                    project.binDirPath,
                                    "classes.jar"
                                )
                            )
                        )
                } catch (e: Exception) {
                    AndroidUtilities.showSimpleAlert(
                        context,
                        context.getString(R.string.error_class_decompile),
                        e.localizedMessage,
                        context.getString(R.string.dialog_close),
                        context.getString(R.string.copy_stacktrace),
                        null,
                        (
                            DialogInterface.OnClickListener { _, which ->
                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    AndroidUtilities.copyToClipboard(
                                        e.localizedMessage
                                    )
                                }
                            }
                        )
                    )
                }
            }

            AlertDialog.Builder(context)
                .setView(decompilerEditor)
                .create().also {
                    it.setCanceledOnTouchOutside(true)
                    it.show()
                }
        }
    }

    private fun disassemble() {
        val classes = classesFromDex ?: return
        val context = requireContext()
        val disassembleEditor = CodeEditor(context).apply {
            typefaceText = ResourcesCompat.getFont(
                context,
                R.font.jetbrains_mono_light
            )
            colorScheme = getColorScheme(context)
            setTextSize(12f)
            setEditorLanguage(javaLanguage)
        }

        listDialog(
            getString(R.string.select_class_disassemble),
            classes
        ) { _, pos ->
            val claz = classes[pos].replace(".", "/")

            try {
                disassembleEditor.setText(
                    JavapDisassembler(
                        project.binDirPath +
                            "classes" +
                            "/" +
                            claz +
                            ".class"
                    )
                    .disassemble()
                )
            } catch (e: Throwable) {
                AndroidUtilities.showSimpleAlert(
                    context,
                    context.getString(R.string.error_class_disassemble),
                    e.localizedMessage,
                    context.getString(R.string.dialog_close),
                    context.getString(R.string.copy_stacktrace)
                ) { _, which ->
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        AndroidUtilities.copyToClipboard(e.localizedMessage)
                    }
                }
            }

            AlertDialog.Builder(context)
                .setView(disassembleEditor)
                .create().also {
                    it.setCanceledOnTouchOutside(true)
                    it.show()
                }
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setItems(items, listener)
                .create()
                .show()
        }
    }

    fun showConsoleFragmentFromCompileTask(projectPath: String, compileClass: String) {
        findNavController().navigate(HomeFragmentDirections.actionShowConsoleFragment(projectPath, compileClass))
    }

    /**
      * Used to find compiled classes from dex file.
      */
    val classesFromDex: Array<String>?
        get() {
            val context = requireContext()
            return try {
                val classes = mutableListOf<String>()
                val dex = File(project.binDirPath + "classes.dex")
                if (!dex.exists()) {
                    AndroidUtilities.showToast(context.getString(R.string.project_not_compiled))
                }

                val dexFile = DexFileFactory.loadDexFile(dex.absolutePath, Opcodes.forApi(32))
                for (f in dexFile.classes.toTypedArray<ClassDef>()) {
                    val name = f.type.replace("/", ".")
                    classes.add(name.substring(1, name.length - 1))
                }

                classes.toTypedArray()
            } catch (e: Exception) {
                AndroidUtilities.showSimpleAlert(
                    context,
                    context.getString(R.string.error_classes_get_dex),
                    e.localizedMessage,
                    context.getString(R.string.dialog_close),
                    context.getString(R.string.copy_stacktrace)
                ) { _, which ->
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        AndroidUtilities.copyToClipboard(e.localizedMessage)
                    }
                }
                null
            }
    }
}