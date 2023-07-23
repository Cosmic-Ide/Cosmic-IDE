/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.pranav.jgit.api.Author
import dev.pranav.jgit.tasks.Credentials
import dev.pranav.jgit.tasks.Repository
import dev.pranav.jgit.tasks.createRepository
import dev.pranav.jgit.tasks.execGit
import dev.pranav.jgit.tasks.toRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.GitAdapter
import org.cosmicide.rewrite.adapter.StagingAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.FragmentGitBinding
import org.cosmicide.rewrite.databinding.GitCommandBinding
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream

class GitFragment : BaseBindingFragment<FragmentGitBinding>() {

    private lateinit var repository: Repository

    override fun getViewBinding() = FragmentGitBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        val root = ProjectHandler.getProject()!!.root

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.recyclerview.apply {
            adapter = GitAdapter()
            layoutManager = LinearLayoutManager(context)
        }

        val git = root.resolve(".git")
        if (git.exists()) {
            repository = git.toRepository()
            setup()
        } else {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Git repository not found")
                .setMessage("Do you want to initialize a new repository?").setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository =
                            root.createRepository(Author(Prefs.gitUsername, Prefs.gitEmail))
                        repository.git.repository.config.apply {
                            setString("remote", "origin", "password", Prefs.gitApiKey)
                            save()
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                            (binding.recyclerview.adapter as GitAdapter).updateCommits(repository.getCommitList())
                            Snackbar.make(binding.root, "Committed", Snackbar.LENGTH_SHORT).show()
                            setup()
                        }
                    }
                }.setNegativeButton("No") { _, _ ->
                    parentFragmentManager.popBackStack()
                }.show()
        }
    }

    fun setup() {
        if (Prefs.gitUsername.isEmpty() || Prefs.gitEmail.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Git username or email not set")
                .setMessage("Do you want to set it now?").setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    parentFragmentManager.popBackStack()
                    parentFragmentManager.commit {
                        replace(R.id.fragment_container, SettingsFragment()).addToBackStack(null)
                        setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    }
                }.setNegativeButton("No") { _, _ ->
                    parentFragmentManager.popBackStack()
                }.show()
            return
        }

        binding.staging.apply {
            adapter = StagingAdapter(ProjectHandler.getProject()!!.root.absolutePath)
            layoutManager = LinearLayoutManager(context)
        }

        catchException {
            val commits = repository.getCommitList()
            withContext(Dispatchers.Main) {
                (binding.recyclerview.adapter as GitAdapter).updateCommits(commits)
            }
        }

        if (repository.isClean()) {
            binding.commit.isEnabled = false
        } else {
            (binding.staging.adapter as StagingAdapter).updateStatus(repository.git.status())
        }

        catchException {
            repository.git.repository.config.getString("remote", "origin", "url")?.let {
                withContext(Dispatchers.Main) {
                    binding.remote.setText(it)
                }
            }
        }

        binding.addAll.setOnClickListener {
            catchException {
                for (file in (binding.staging.adapter as StagingAdapter).files) {
                    repository.add(file.path)
                }
                (binding.staging.adapter as StagingAdapter).updateStatus(repository.git.status())
            }
        }

        binding.remote.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val remote = binding.remote.text.toString()
                if (remote.isNotEmpty()) {
                    catchException {
                        repository.git.repository.config.apply {
                            setString("remote", "origin", "url", remote)
                            save()
                        }
                    }
                }
            }
        }

        binding.pull.setOnClickListener {
            catchException {
                repository.pull(
                    OutputStreamWriter(System.out),
                    binding.rebase.isChecked,
                    Credentials(Prefs.gitUsername, Prefs.gitApiKey)
                )
                withContext(Dispatchers.Main) {
                    (binding.recyclerview.adapter as GitAdapter).updateCommits(repository.getCommitList())
                    Snackbar.make(binding.root, "Pulled", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.commit.setOnClickListener {
            if (repository.git.status().hasUncommittedChanges().not()) {
                Snackbar.make(binding.root, "Nothing to commit", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            catchException {
                repository.commit(
                    getAuthor(), binding.commitMessage.text.toString()
                )
                withContext(Dispatchers.Main) {
                    (binding.recyclerview.adapter as GitAdapter).updateCommits(repository.getCommitList())
                    Snackbar.make(binding.root, "Committed", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.push.setOnClickListener {
            catchException {
                repository.push(
                    OutputStreamWriter(System.out), Credentials(Prefs.gitUsername, Prefs.gitApiKey)
                )
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Pushed", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.custom_command) {
                val binding = GitCommandBinding.inflate(layoutInflater)
                BottomSheetDialog(requireContext()).apply {
                    setContentView(binding.root)
                    binding.execute.setOnClickListener {
                        binding.gitOutput.visibility = View.VISIBLE
                        binding.gitOutput.text = ""
                        lifecycleScope.launch(Dispatchers.IO) {
                            ProjectHandler.getProject()!!.root.execGit(
                                binding.gitCommand.editText?.text.toString().split(" ").toMutableList(),
                                PrintStream(object : OutputStream() {
                                    override fun write(b: Int) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            binding.gitOutput.append(b.toChar().toString())
                                        }
                                    }
                                })
                            )
                        }
                    }
                    show()
                }
            }
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::repository.isInitialized) repository.git.close()
    }

    fun catchException(code: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                code()
            } catch (e: Exception) {
                Log.e("GitFragment", e.message, e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getAuthor() = Author(Prefs.gitUsername, Prefs.gitEmail)
}
