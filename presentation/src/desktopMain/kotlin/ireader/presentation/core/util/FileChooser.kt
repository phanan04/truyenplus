package ireader.presentation.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * This code is derived from the terrific PACMC project:
 * https://github.com/jakobkmar/pacmc
 */
internal object FileChooser {
    private enum class CallType {
        FILE,
        DIRECTORY
    }

    suspend fun chooseFile(
        initialDirectory: String = System.getProperty("user.dir"),
        fileExtensions: String = ""
    ): String? {
        return chooseFile(CallType.FILE, initialDirectory, fileExtensions)
    }

    suspend fun chooseDirectory(
        initialDirectory: String = System.getProperty("user.dir"),
    ): String? {
        return chooseFile(CallType.DIRECTORY, initialDirectory)
    }

    suspend fun saveFile(
        initialDirectory: String = System.getProperty("user.dir"),
        defaultFileName: String = "",
        fileExtensions: String = ""
    ): String? {
        return saveFileSwing(initialDirectory, defaultFileName, fileExtensions)
    }

    private suspend fun saveFileSwing(
        initialDirectory: String,
        defaultFileName: String,
        fileExtension: String
    ) = withContext(Dispatchers.Main) {
        kotlin.runCatching {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }

        val chooser = JFileChooser(initialDirectory).apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            selectedFile = java.io.File(initialDirectory, defaultFileName)
            if (fileExtension.isNotEmpty()) {
                addChoosableFileFilter(FileNameExtensionFilter(fileExtension, fileExtension))
            }
        }

        when (val code = chooser.showSaveDialog(null)) {
            JFileChooser.APPROVE_OPTION -> chooser.selectedFile.absolutePath
            JFileChooser.CANCEL_OPTION -> null
            else -> null
        }
    }

    private suspend fun chooseFile(
        type: CallType,
        initialDirectory: String,
        fileExtensions: String = ""
    ): String? {
        return chooseFileSwing(type, initialDirectory, fileExtensions)
    }

    private suspend fun chooseFileSwing(
        type: CallType,
        initialDirectory: String,
        fileExtension: String
    ) = withContext(Dispatchers.Main) {
        kotlin.runCatching {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }

        val chooser = when (type) {
            CallType.FILE -> {
                JFileChooser(initialDirectory).apply {
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    if (fileExtension.isNotEmpty()) {
                        addChoosableFileFilter(FileNameExtensionFilter(fileExtension, fileExtension))
                    }
                }
            }

            CallType.DIRECTORY -> {
                JFileChooser(initialDirectory).apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
            }
        }

        when (val code = chooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> chooser.selectedFile.absolutePath
            JFileChooser.CANCEL_OPTION -> null
            else -> null
        }
    }
}