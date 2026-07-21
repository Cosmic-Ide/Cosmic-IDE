package org.cosmicide.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.project.Project

class ProjectViewModel(
    private val repository: ProjectRepository = FileSystemProjectRepository()
) : ViewModel() {

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
            val projectsList = repository.projects()

            withContext(Dispatchers.Main) {
                _projects.value = projectsList
                _isLoading.value = false
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(project)
            loadProjects()
        }
    }
}
