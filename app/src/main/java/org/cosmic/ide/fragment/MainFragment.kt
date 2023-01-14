package org.cosmic.ide.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.pedrovgs.lynx.LynxActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.R
import org.cosmic.ide.databinding.FragmentMainBinding
import org.cosmic.ide.databinding.DialogNewProjectBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.project.KotlinProject
import org.cosmic.ide.ui.adapter.MainActionsListAdapter
import org.cosmic.ide.ui.model.MainScreenAction
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.Constants.PROJECT_PATH
import org.cosmic.ide.util.runOnUiThread
import java.io.File

interface OnProjectCreatedListener {
    fun openProject(root: File)
}

class MainFragment : Fragment(), OnProjectCreatedListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private var onProjectCreatedListener: OnProjectCreatedListener? = null
    private val newProjectBinding by lazy {
        val layoutInflater = LayoutInflater.from(requireContext())
        DialogNewProjectBinding.inflate(layoutInflater)
    }
    private val newProjectDialog: AlertDialog by lazy {
        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(
            requireActivity(), AndroidUtilities.dialogFullWidthButtonsThemeOverlay)
        dialog.apply {
            setTitle(context.getString(R.string.create_project))
            setView(newProjectBinding.root)
            setPositiveButton(context.getString(R.string.create), null)
            setNegativeButton(context.getString(android.R.string.cancel), null)
            setOnDismissListener {
                newProjectBinding.text1.setText("")
            }
        }.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        val createProject =
            MainScreenAction(R.string.create_project, R.drawable.ic_add) { showNewProject() }
        val openProject =
            MainScreenAction(R.string.open_project, R.drawable.ic_folder) { gotoProjects() }
        val openSettings =
            MainScreenAction(R.string.settings, R.drawable.ic_settings) { gotoSettings() }
        val openLogcat =
            MainScreenAction(R.string.logcat, R.drawable.ic_list_alt) { gotoLynxActivity() }

        binding.getStartedActions.adapter =
            MainActionsListAdapter(
                listOf(
                    createProject,
                    openProject,
                    openSettings,
                    openLogcat
                )
            )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onProjectCreatedListener = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun gotoSettings() {
        findNavController().navigate(MainFragmentDirections.actionShowSettingsFragment())
    }

    private fun gotoLynxActivity() {
        val activity = requireActivity()
        activity.startActivity(LynxActivity.getIntent(activity))
    }

    private fun gotoProjects() {
        findNavController().navigate(MainFragmentDirections.actionShowProjectsFragment())
    }

    override fun openProject(root: File) {
        findNavController().navigate(MainFragmentDirections.actionShowHomeFragment(root.absolutePath))
    }

    private fun showNewProject() {
        val createButton = newProjectDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        createButton?.setOnClickListener {
            val projectName =
                newProjectBinding.text1.text.toString().trim().replace("..", "")
            val kotlinChecked =
                newProjectBinding.useKotlinTemplate.isChecked

            if (projectName.isNullOrBlank()) {
                return@setOnClickListener
            }

            val project = if (kotlinChecked) {
                    KotlinProject.newProject(projectName)
                } else {
                    JavaProject.newProject(projectName)
                }

            runOnUiThread {
                onProjectCreatedListener?.openProject(project.rootFile)

                if (newProjectDialog.isShowing) {
                    newProjectDialog.dismiss()
                }
            }
        }

        if (!newProjectDialog.isShowing) {
            newProjectDialog.show()
        }
    }
}