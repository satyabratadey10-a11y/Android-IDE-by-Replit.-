package com.androidide.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androidide.engine.BuildPipelineEngine
import com.androidide.engine.TerminalEngine
import com.androidide.project.AndroidProject
import com.androidide.project.ProjectManager
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val toolchain = ToolchainManager(app)
    val projectManager = ProjectManager(toolchain)

    // ---- Toolchain state ----
    private val _toolchainReady = MutableStateFlow(false)
    val toolchainReady: StateFlow<Boolean> = _toolchainReady.asStateFlow()

    private val _setupLog = MutableStateFlow("")
    val setupLog: StateFlow<String> = _setupLog.asStateFlow()

    // ---- Projects ----
    private val _projects = MutableStateFlow<List<AndroidProject>>(emptyList())
    val projects: StateFlow<List<AndroidProject>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<AndroidProject?>(null)
    val currentProject: StateFlow<AndroidProject?> = _currentProject.asStateFlow()

    // ---- Editor ----
    private val _currentFile = MutableStateFlow<android.net.Uri?>(null)
    val currentFile: StateFlow<android.net.Uri?> = _currentFile.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    // ---- Build ----
    private val _buildLog = MutableStateFlow<List<BuildPipelineEngine.BuildEvent>>(emptyList())
    val buildLog: StateFlow<List<BuildPipelineEngine.BuildEvent>> = _buildLog.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    private val _lastApkPath = MutableStateFlow<String?>(null)
    val lastApkPath: StateFlow<String?> = _lastApkPath.asStateFlow()

    // ---- Terminal ----
    private val _terminalOutput = MutableStateFlow<List<TerminalEngine.TerminalOutput>>(emptyList())
    val terminalOutput: StateFlow<List<TerminalEngine.TerminalOutput>> = _terminalOutput.asStateFlow()

    private val _terminalEngine = MutableStateFlow<TerminalEngine?>(null)

    // ---- Init ----
    init {
        setupToolchain()
    }

    private fun setupToolchain() {
        viewModelScope.launch(Dispatchers.IO) {
            toolchain.setup { progress ->
                _setupLog.value = progress
            }
            _toolchainReady.value = true
            _terminalEngine.value = TerminalEngine(toolchain)
            loadProjects()
        }
    }

    // ---- Project operations ----

    fun loadProjects() {
        viewModelScope.launch {
            _projects.value = projectManager.listProjects()
        }
    }

    fun openProject(project: AndroidProject) {
        _currentProject.value = project
        _buildLog.value = emptyList()
    }

    fun createProject(
        name: String,
        packageName: String,
        minSdk: Int = 24,
        useKotlin: Boolean = true,
        useNdk: Boolean = false
    ) {
        viewModelScope.launch {
            val project = projectManager.createProject(name, packageName, minSdk, 34, useKotlin, useNdk)
            loadProjects()
            openProject(project)
        }
    }

    fun deleteProject(project: AndroidProject) {
        viewModelScope.launch {
            projectManager.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
            }
            loadProjects()
        }
    }

    // ---- Editor operations ----

    fun openFile(file: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _editorContent.value = file.readText()
                _isDirty.value = false
            } catch (e: Exception) {
                _editorContent.value = "Error reading file: ${e.message}"
            }
        }
    }

    fun onEditorContentChange(content: String) {
        _editorContent.value = content
        _isDirty.value = true
    }

    fun saveCurrentFile(file: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.writeText(_editorContent.value)
                _isDirty.value = false
            } catch (e: Exception) {
                // Handle save error
            }
        }
    }

    // ---- Build operations ----

    fun buildDebug() {
        val project = _currentProject.value ?: return
        if (_isBuilding.value) return

        _isBuilding.value = true
        _buildLog.value = emptyList()

        viewModelScope.launch {
            val engine = BuildPipelineEngine(toolchain, project)
            engine.buildDebug()
                .catch { e ->
                    emit(BuildPipelineEngine.BuildEvent(
                        BuildPipelineEngine.BuildEvent.Type.BUILD_FAILED,
                        "Unexpected error: ${e.message}",
                        isError = true
                    ))
                }
                .collect { event ->
                    _buildLog.value = _buildLog.value + event
                    if (event.type == BuildPipelineEngine.BuildEvent.Type.BUILD_SUCCESS) {
                        _lastApkPath.value = project.outputApkPath
                    }
                    if (event.type == BuildPipelineEngine.BuildEvent.Type.BUILD_SUCCESS ||
                        event.type == BuildPipelineEngine.BuildEvent.Type.BUILD_FAILED) {
                        _isBuilding.value = false
                    }
                }
        }
    }

    fun installApk() {
        val apkPath = _lastApkPath.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val project = _currentProject.value ?: return@launch
            val engine = BuildPipelineEngine(toolchain, project)
            val result = engine.installToDevice(apkPath)
            _buildLog.value = _buildLog.value + BuildPipelineEngine.BuildEvent(
                BuildPipelineEngine.BuildEvent.Type.LOG,
                result
            )
        }
    }

    fun clearBuildLog() {
        _buildLog.value = emptyList()
    }

    // ---- Terminal operations ----

    fun executeCommand(commandLine: String) {
        val engine = _terminalEngine.value ?: return
        viewModelScope.launch {
            engine.execute(commandLine)
                .catch { e ->
                    emit(TerminalEngine.TerminalOutput("Error: ${e.message}", true))
                }
                .collect { output ->
                    _terminalOutput.value = _terminalOutput.value + output
                }
        }
    }

    fun clearTerminal() {
        _terminalOutput.value = emptyList()
    }
}
