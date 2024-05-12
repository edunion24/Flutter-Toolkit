package cn.neday.excavator.action.generation

import cn.neday.excavator.action.BaseGenerationAnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.io.File


class PartialBuildAction : BaseGenerationAnAction() {
    override val command = "dart run build_runner build"
    override val title = "Building"
    override val successMessage = "Complete!\nRunning build successfully."
    override val errorMessage = "Could not running build!"

    override fun actionPerformed(event: AnActionEvent) {
        // Default message
        val message = "Current directory does not seem to be a project directory."
        // Get current project
        val project = event.project ?: return showErrorMessage(message)
        val projectPath = project.basePath ?: return showErrorMessage(message)
        val projectURI = File(projectPath).toURI()
        // Get current element
        val element = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return showErrorMessage(message)
        val elementURI = File(element.path).toURI()
        // Get relative path
        val parentDirectory = projectURI.relativize(elementURI).path.substringBefore("/lib")
        val subPath = projectURI.relativize(elementURI).path.substringAfter("$parentDirectory/")
        val relativePath = subPath.let { path ->
            if (element.isDirectory) path.plus("**")
            else path
        }
        if (element.isDirectory) {
            val command = "cd $parentDirectory && $command --build-filter=\"${relativePath}\""
            execCommand(project, projectPath, command)
        } else {
            val file = File(element.path)
            val lines = file.readLines()

            val partFiles = mutableListOf<String>()

            lines.forEach { line ->
                if (line.startsWith("part")) {
                    val partFilePath = line.substringAfter("part").trim().removePrefix("'").removeSuffix("';")
                    partFiles.add(partFilePath)
                }
            }

            if (partFiles.isEmpty()) {
                return
            }

            var command = "cd $parentDirectory && $command "
            val lastIndex = relativePath.lastIndexOf('/')
            val folderPath = relativePath.substring(0, lastIndex)

            partFiles.forEachIndexed { index, partFile ->
                if (index > 0) {
                    command += " &&"
                }
                command += " --build-filter=\"${"$folderPath/$partFile"}\""
            }

            execCommand(project, projectPath, command)
        }
    }
}
