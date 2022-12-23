package org.cosmic.ide.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.cosmic.ide.R
import org.cosmic.ide.activity.model.GitViewModel
import org.cosmic.ide.databinding.ActivityGitBinding
import org.cosmic.ide.databinding.LayoutEditTextBinding
import org.cosmic.ide.git.model.Author
import org.cosmic.ide.git.model.Failure
import org.cosmic.ide.git.model.Success
import org.cosmic.ide.ui.logger.Logger
import org.cosmic.ide.util.*

// TODO: Implement RecyclerView for merging and deleting
// TODO: Automatically update project files after deleting file in a topic branch and checking out
// TODO: Let select commits for reverting, restore and reset

class GitActivity :
    BaseActivity(),
    AdapterView.OnItemSelectedListener {

    lateinit var binding: ActivityGitBinding

    val gitViewModel = GitViewModel.INSTANCE

    private var arrayAdapter: ArrayAdapter<String>? = null
    private val logger by lazy {
        Logger()
    }

    private val person by lazy {
        Author(settings.gitUserName, settings.gitUserEmail)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGitBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        initListeners(this)
        binding.root.addSystemWindowInsetToPadding(
            top = true,
            bottom = true
        )
        logger.attach(binding.recyclerView)

        gitViewModel.apply {
            hasRepo.observe(this@GitActivity) { isRepo ->
                Log.d(TAG, "hasRepo: $isRepo")
                if (!isRepo) {
                    createGitRepo(person)
                }
            }
            gitLog.observe(this@GitActivity) { log ->
                Log.d(TAG, "gitLog: $log")
                logger.message(log)
            }

            branchList.observe(this@GitActivity) { list ->
                Log.d(TAG, "branchList: $list")
                arrayAdapter?.listOf(list)
            }

            getBranchList()
        }
    }

    private fun initListeners(listener: AdapterView.OnItemSelectedListener) {
        arrayAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf("")
        )
        arrayAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        with(binding) {
            spinnerBranch.adapter = arrayAdapter
            spinnerBranch.onItemSelectedListener = listener

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

    override fun onDestroy() {
        super.onDestroy()
        gitViewModel.dispose()
    }

    override fun onItemSelected(
        parent: AdapterView<*>,
        v: View?,
        position: Int,
        id: Long
    ) {
        gitViewModel.checkout(position)
    }
    override fun onNothingSelected(parent: AdapterView<*>) {}

    companion object {
        const val TAG = "GitActivity"
    }
}

fun GitActivity.commitWith(commiter: Author) {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.hint = getString(R.string.git_commit_message)

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_commit))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.git_commit)) { _, _ ->
            gitViewModel.commiting(commiter, binding.editText.text.toString())
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
        .show()
}

fun GitActivity.createBranch() {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.hint = getString(R.string.git_name_branch)

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_new_branch))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.create)) { _, _ ->
            val text = binding.editText.text.toString()

            when (gitViewModel.createBranch(text)) {
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
    binding.editLayout.hint = getString(R.string.git_merge_branch_message)

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_merge_branch))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.git_merge)) { _, _ ->
            val text = binding.editText.text.toString()

            when (gitViewModel.mergeBranch(text)) {
                is Success -> showSnackbar("Branch '$text' merged")
                is Failure -> AndroidUtilities.showSimpleAlert(
                    this,
                    getString(R.string.error),
                    "Branch '$text' not in repository"
                )

                else -> showSnackbar("Unknown error")
            }
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
        .show()
}

fun GitActivity.deleteBranch() {
    val binding = LayoutEditTextBinding.inflate(layoutInflater)
    binding.editLayout.hint = getString(R.string.git_delete_branch_message)

    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.git_delete_branch))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.delete)) { _, _ ->
            val text = binding.editText.text.toString()

            when (gitViewModel.deleteBranch(text)) {
                is Success -> showSnackbar("Branch '$text' deleted")
                is Failure -> AndroidUtilities.showSimpleAlert(
                    this,
                    getString(R.string.error),
                    "Branch '$text' must not be the current branch to delete."
                )

                else -> showSnackbar("Unknown error")
            }
        }
        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
        .show()
}

fun GitActivity.showSnackbar(message: String) =
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

fun <I> ArrayAdapter<I>.listOf(items: List<I>) {
    clear()
    addAll(items)
}
