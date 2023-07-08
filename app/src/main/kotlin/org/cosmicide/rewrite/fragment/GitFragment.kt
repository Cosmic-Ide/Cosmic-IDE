/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.pranav.jgit.api.Author
import dev.pranav.jgit.tasks.Credentials
import dev.pranav.jgit.tasks.Repository
import dev.pranav.jgit.tasks.createRepository
import dev.pranav.jgit.tasks.toRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.GitAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.FragmentGitBinding
import org.cosmicide.rewrite.util.ProjectHandler
import org.eclipse.jgit.transport.URIish
import java.io.OutputStreamWriter

class GitFragment : BaseBindingFragment<FragmentGitBinding>() {

    private lateinit var repository: Repository

    override fun getViewBinding() = FragmentGitBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view.context)
    }

    private fun setupUI(context: Context) {
        val root = ProjectHandler.getProject()!!.root
        val git = root.resolve(".git")
        if (git.exists()) {
            repository = git.toRepository()
        } else {
            MaterialAlertDialogBuilder(context).setTitle("Git repository not found")
                .setMessage("Do you want to initialize a new repository?").setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository = root.createRepository()
                        repository.addAll()
                        repository.commit(
                            getAuthor(),
                            "Initial commit"
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            (binding.recyclerview.adapter as GitAdapter).updateCommits(repository.getCommitList())
                            Snackbar.make(binding.root, "Committed", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }.setNegativeButton("No") { _, _ ->
                    parentFragmentManager.popBackStack()
                }.show()
        }

        if (Prefs.gitUsername.isEmpty() || Prefs.gitEmail.isEmpty()) {
            MaterialAlertDialogBuilder(context).setTitle("Git username or email not set")
                .setMessage("Do you want to set it now?").setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    parentFragmentManager.popBackStack()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment()).addToBackStack(null)
                        .commit()
                }.setNegativeButton("No") { _, _ ->
                    parentFragmentManager.popBackStack()
                }.show()
            return
        }

        binding.recyclerview.apply {
            adapter = GitAdapter()
            layoutManager = LinearLayoutManager(context)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (!::repository.isInitialized) {
                return@launch
            }
            val commits = repository.getCommitList()
            lifecycleScope.launch(Dispatchers.Main) {
                (binding.recyclerview.adapter as GitAdapter).updateCommits(commits)
            }
        }

        if (::repository.isInitialized) {
            val remotes = repository.git.remoteList()
            if (remotes.isNotEmpty() && remotes[0].pushURIs.isNotEmpty()) {
                binding.remote.setText(remotes[0].pushURIs[0].toString())
            }
        }

        binding.remote.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val remote = binding.remote.text.toString()
                if (remote.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.git.remoteAdd {
                            setName("origin")
                            setUri(URIish(remote))
                        }
                    }
                }
            }
        }

        binding.pull.setOnClickListener {
            repository.pull(OutputStreamWriter(System.out), binding.rebase.isChecked)
        }

        binding.commit.setOnClickListener {
            if (repository.isClean()) {
                Snackbar.make(binding.root, "Nothing to commit", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            repository.addAll()
            lifecycleScope.launch(Dispatchers.IO) {
                repository.commit(
                    getAuthor(),
                    binding.commitMessage.text.toString()
                )
                lifecycleScope.launch(Dispatchers.Main) {
                    (binding.recyclerview.adapter as GitAdapter).updateCommits(repository.getCommitList())
                    Snackbar.make(binding.root, "Committed", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.push.setOnClickListener {
            binding.remote.text.toString().let { remote ->
                repository.git.remoteAdd {
                    setName("origin")
                    setUri(URIish(remote))
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.push(
                        OutputStreamWriter(System.out),
                        Credentials(Prefs.gitUsername, Prefs.gitApiKey)
                    )
                    lifecycleScope.launch(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Pushed", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getAuthor() = Author(Prefs.gitUsername, Prefs.gitEmail)
}
