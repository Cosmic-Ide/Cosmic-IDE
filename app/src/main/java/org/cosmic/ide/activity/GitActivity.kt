package org.cosmic.ide.activity

import android.os.Bundle
import android.content.Context
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.core.os.*
import androidx.core.view.ViewCompat
import androidx.lifecycle.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.cosmic.ide.R
import org.cosmic.ide.databinding.ActivityGitBinding
import org.cosmic.ide.databinding.LayoutEditTextBinding
import org.cosmic.ide.ui.logger.Logger
import org.cosmic.ide.util.*
import org.cosmic.ide.git.model.Author
import org.cosmic.ide.git.model.Result
import org.cosmic.ide.git.model.Success
import org.cosmic.ide.git.model.Failure
import org.cosmic.ide.project.Project
import org.cosmic.ide.activity.model.GitViewModel
import java.io.File

//TODO: implement ListView for merging and deleting
//TODO: fix wrong filetreeview after deleting file in a topic branch and checking out
//TODO: Let select commits for reverting, restore and reset

class GitActivity :
    BaseActivity<ActivityGitBinding>(),
    AdapterView.OnItemSelectedListener {

    private val TAG = "GitActivity"

    val mGitViewModel: GitViewModel by lazy {
        ViewModelProvider(this).get(GitViewModel::class.java)
    }

    private var arrayAdapter: ArrayAdapter<String>? = null
    private var preCheckout: () -> Unit = {}
    private val logger: Logger by lazy {
        Logger()
    }

    private lateinit var person: Author

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityGitBinding.inflate(layoutInflater))
        initListeners(this)
        binding.root.addSystemWindowInsetToPadding(false, true, false, true)
        logger.attach(binding.recyclerView)

        mGitViewModel.hasRepo.observe(this) { isRepo ->
            Log.d(TAG, "hasRepo=$isRepo") 
            if (!isRepo) {
                mGitViewModel.createGitRepo(person)
            }
        }

        mGitViewModel.gitLog.observe(this) { log ->
            Log.d(TAG, "gitLog=$log")
            logger.message(log)
        }

        mGitViewModel.branchList.observe(this) { list ->
            Log.d(TAG, "branchList=$list")
            arrayAdapter?.listOf(list)
        }
        mGitViewModel.getBranchList()
    }

    private fun initListeners(listener: AdapterView.OnItemSelectedListener) {
        arrayAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf("")
        )
        arrayAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        with(binding) {
            spinnerBranch.setAdapter(arrayAdapter)
            spinnerBranch.setOnItemSelectedListener(listener)

            buttonCreate.setOnClickListener {
                mGitViewModel.createGitRepo(person)
                Log.d(TAG, "Create Git Repository complete")
            }

            buttonCommit.setOnClickListener {
                commitWith(person)
                Log.d(TAG, "Commit complete")
            }

            buttonCreateBranch.setOnClickListener {
                createBranch()
                Log.d(TAG, "Create Branch complete")
            }

            buttonMergeBranch.setOnClickListener {
                mergeBranch()
                Log.d(TAG, "Merge branch complete")
            }

            buttonDeleteBranch.setOnClickListener {
                deleteBranch()
                Log.d(TAG, "Delete branch complete")
            }
        }
    }

    private fun switchButtons(hasRepo: Boolean) {
        with(binding) {
            buttonCommit.setEnabled(hasRepo)
            buttonCreateBranch.setEnabled(hasRepo)
            buttonMergeBranch.setEnabled(hasRepo)
            buttonDeleteBranch.setEnabled(hasRepo)
            spinnerBranch.setEnabled(hasRepo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mGitViewModel.dispose()
    }

    override fun onItemSelected(
        parent: AdapterView<*>,
        v: View?,
        position: Int,
        id: Long
    ) {
        mGitViewModel.checkout(position)
    }
    override fun onNothingSelected(parent: AdapterView<*>) {}

    companion object {
        const val ARG_PATH_ID = "pathId"
    }
}

fun GitActivity.commitWith(commiter: Author) {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.setHint(getString(R.string.git_commit_message))

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_commit))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.git_commit)) { _, _ ->
            mGitViewModel.commiting(commiter, binding.editText.getText().toString())
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
        .show()
}

fun GitActivity.createBranch() {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.setHint(getString(R.string.git_name_branch))

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_new_branch))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.create)) {_, _ ->
            val text = binding.editText.getText().toString()
            val result = mGitViewModel.createBranch(text)

            when (result) {
                is Success -> showSnackbar("Branch '$text' created")
                is Failure -> showSnackbar("Branch '$text' could not created")
                else -> showSnackbar("Unknown error")
            }
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
        .show()
}

fun GitActivity.mergeBranch() {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.setHint(getString(R.string.git_merge_branch_message))

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_merge_branch))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.git_merge)) { _, _ ->
            val text = binding.editText.getText().toString()
            val result = mGitViewModel.mergeBranch(text)

            when (result) {
                is Success -> showSnackbar("Branch '$text' merged")
                is Failure -> AndroidUtilities.showSimpleAlert(this, getString(R.string.error), "Branch '$text' not in repository", getString(android.R.string.ok), getString(R.string.dialog_close))
                else -> showSnackbar("Unknown error")
            }
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
        .show()
}

fun GitActivity.deleteBranch() {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.setHint(getString(R.string.git_delete_branch_message))

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_delete_branch))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.delete)) { _, _ ->
            val text = binding.editText.getText().toString()
            val result = mGitViewModel.deleteBranch(text)

            when (result) {
                is Success -> showSnackbar("Branch '$text' deleted")
                is Failure -> AndroidUtilities.showSimpleAlert(this, getString(R.string.error), "Branch '$text' must not be the current branch to delete.", getString(android.R.string.ok), getString(R.string.dialog_close))
                else -> showSnackbar("Unknown error")
            }
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->}    
        .show()
}

fun GitActivity.showSnackbar(message: String) =
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

fun toContent(file: File?) = file?.readText() ?: ""

fun <I> ArrayAdapter<I>.listOf(items: List<I>) {
    clear()
    addAll(items)
}