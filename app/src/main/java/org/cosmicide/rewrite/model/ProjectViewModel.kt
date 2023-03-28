package org.cosmicide.rewrite.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import java.io.File

class ProjectViewModel : ViewModel() {

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val projectsList = FileUtil.projectDir.listFiles { file -> file.isDirectory }
                ?.sortedByDescending { it.lastModified() }
                ?.map {
                    if (File(it, "src/main/java").exists()) {
                        Project(it, Language.Java)
                    } else {
                        Project(it, Language.Kotlin)
                    }
                }
                ?: emptyList()

            withContext(Dispatchers.Main) {
                _projects.value = projectsList
                Log.d(TAG, "Projects: $projectsList")
            }
        }
    }

    companion object {
        private const val TAG = "ProjectViewModel"
    }
}