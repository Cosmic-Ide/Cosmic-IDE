package org.cosmicide.rewrite.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.project.Java
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.util.Arrays

class ProjectViewModel : ViewModel() {

    private val _projects = MutableLiveData<List<Project>>()
    val projects: LiveData<List<Project>> = _projects

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val projectsList = mutableListOf<Project>()
            val projectDir = FileUtil.projectDir
            val directories = projectDir.listFiles { file -> file.isDirectory }
            if (directories != null) {
                Arrays.sort(directories, Comparator.comparingLong(File::lastModified).reversed())
                for (directory in directories) {
                    projectsList.add(Project(directory, Java))
                }
            }
            withContext(Dispatchers.Main) {
                _projects.value = projectsList
                Log.d("ProjectViewModel", "Projects: $projectsList")
            }
        }
    }
}