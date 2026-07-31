package org.cosmicide.ui.editor

internal data class EditorToolWindowSessionState(
    val selectedTabId: String = SyncToolWindowTabId,
    val buildSessions: List<EditorBuildSession> = emptyList(),
    val nextBuildSessionId: Int = 0,
    val projectSyncRunId: Int = 0,
    val projectSyncStatus: String = "Running"
) {
    val isProjectSyncInProgress: Boolean
        get() = projectSyncStatus == "Running" || projectSyncStatus == "Stopping"

    fun openTerminal(
        title: String,
        command: String,
        arguments: List<String>?
    ): EditorToolWindowSessionState {
        val session = EditorBuildSession(
            id = nextBuildSessionId + 1,
            task = title,
            command = command,
            arguments = arguments
        )
        return copy(
            selectedTabId = session.tabId,
            buildSessions = buildSessions + session,
            nextBuildSessionId = session.id
        )
    }

    fun rerunProjectSync(): EditorToolWindowSessionState = copy(
        selectedTabId = SyncToolWindowTabId,
        projectSyncRunId = projectSyncRunId + 1,
        projectSyncStatus = "Running"
    )

    fun stopProjectSync(): EditorToolWindowSessionState = copy(
        projectSyncStatus = "Stopping"
    )

    fun selectTab(tabId: String): EditorToolWindowSessionState = copy(selectedTabId = tabId)

    fun closeBuild(sessionId: Int): EditorToolWindowSessionState {
        val closedTabId = buildSessions.firstOrNull { it.id == sessionId }?.tabId
            ?: return this
        val remaining = buildSessions.filterNot { it.id == sessionId }
        return copy(
            buildSessions = remaining,
            selectedTabId = if (selectedTabId == closedTabId) {
                remaining.lastOrNull()?.tabId ?: SyncToolWindowTabId
            } else {
                selectedTabId
            }
        )
    }

    fun rerunBuild(sessionId: Int): EditorToolWindowSessionState = copy(
        buildSessions = buildSessions.map { session ->
            if (session.id == sessionId) {
                session.copy(runId = session.runId + 1, status = "Running")
            } else {
                session
            }
        }
    )

    fun stopBuild(sessionId: Int): EditorToolWindowSessionState = copy(
        buildSessions = buildSessions.map { session ->
            if (session.id == sessionId) session.copy(status = "Stopping") else session
        }
    )

    fun updateBuildStatus(sessionId: Int, status: String): EditorToolWindowSessionState = copy(
        buildSessions = buildSessions.map { session ->
            if (session.id == sessionId) session.copy(status = status) else session
        }
    )

    fun updateProjectSyncStatus(status: String): EditorToolWindowSessionState = copy(
        projectSyncStatus = status
    )
}
