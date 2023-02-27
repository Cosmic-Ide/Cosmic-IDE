package org.cosmic.ide.activity.model

import android.util.Log
import androidx.lifecycle.*
import org.cosmic.ide.git.model.*
import org.cosmic.ide.git.usecases.*

class GitViewModel : ViewModel() {

    private val TAG = "GitViewModel"
    var postCheckout: () -> Unit = {}
    var onSave: () -> Unit = {}

    private val _projectPath = MutableLiveData<String>()
    val projectPath: LiveData<String> = _projectPath

    private val _hasRepo = MutableLiveData<Boolean>()
    val hasRepo: LiveData<Boolean> = _hasRepo

    private val _gitLog = MutableLiveData<String>()
    val gitLog: LiveData<String> = _gitLog

    private val _branchList = MutableLiveData<List<String>>()
    val branchList = _branchList

    lateinit var git: Gitter

    companion object {
        val INSTANCE: GitViewModel by lazy { GitViewModel() }
    }

    fun setPath(newPath: String) {
        _projectPath.value = newPath
        if (isGitRepoAt(newPath)) {
            try {
                git = openGitAt(newPath)
                getLog()
                getBranchList()
                _hasRepo.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            _hasRepo.value = false
        }
    }

    fun getLog() {
        _gitLog.value = git.getLog()
    }

    fun getBranchList() {
        _branchList.value = git.getBranchList()
    }

    fun createGitRepo(commiter: Author) {
        _projectPath.value!!.createGitRepoWith(commiter, "Initial commit")
        getLog()
        _hasRepo.value = true
        postCheckout()
    }

    fun commiting(commiter: Author, msg: String) {
        git.commiting(commiter, msg)
        getLog()
    }

    fun createBranch(branch: String): Result {
        git.createBranch(branch)
        getLog()
        getBranchList()
        return Success
    }

    fun mergeBranch(branch: String): Result {
        if (branch in git.getBranchList()) {
            onSave()
            git.mergeBranch(branch)
            getLog()
            getBranchList()
            postCheckout()
            return Success
        }
        return Failure
    }

    fun getBranch(): String = git.getBranch()

    fun deleteBranch(branch: String): Result {
        if (branch !in getBranch()) {
            git.deleteBranch(branch)
            getLog()
            getBranchList()
            return Success
        }
        return Failure
    }

    fun checkout(position: Int) {
        if (::git.isInitialized) {
            _branchList.value?.let {
                if (it.isNotEmpty()) {
                    val branch = it[position]
                    if (branch.isNotBlank()) {
                        onSave()
                        git.checkout(branch)
                        postCheckout()
                        getLog()
                        getBranchList()
                    }
                }
            }
        }
    }

    fun dispose() {
        postCheckout = {}
        onSave = {}
        if (::git.isInitialized) git.dispose()
        _branchList.value = listOf("")
        _gitLog.value = "Logs not available."
    }
}
