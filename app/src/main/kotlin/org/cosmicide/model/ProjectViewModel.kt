package org.cosmicide.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.util.FileUtil
import java.io.File

class ProjectViewModel : ViewModel() {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
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
                _isLoading.value = false
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            project.delete()
            loadProjects()
        }
    }
}
