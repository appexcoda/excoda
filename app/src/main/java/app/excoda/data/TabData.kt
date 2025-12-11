package app.excoda.data

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val id: String,
    val fileName: String,
    val uri: String
)

@Serializable
data class TabData(
    val id: String,
    val name: String,
    val files: List<FileItem> = emptyList()
)

@Serializable
data class AppState(
    val tabs: List<TabData> = listOf(
        TabData(id = "tab_1", name = "New list", files = emptyList())
    ),
    val activeTabId: String = "tab_1"
)
