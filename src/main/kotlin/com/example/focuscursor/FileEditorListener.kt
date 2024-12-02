package com.example.focuscursor

import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.vfs.VirtualFile

class FileEditorListener : FileEditorManagerListener {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    data class ProjectInfo(
        val project: String,
        val file: String,
        val line: Int
    )

    private fun sendProjectInfo(project: Project, file: VirtualFile, line: Int) {
        val projectPath = project.projectFilePath?.replace("/.idea/misc.xml", "") ?: return
        val projectName = projectPath.split("/").last()

        val projectInfo = ProjectInfo(
            project = projectName,
            file = file.path,
            line = line
        )

        val requestBody = RequestBody.create(
            mediaType,
            gson.toJson(projectInfo)
        )

        val request = Request.Builder()
            .url("http://127.0.0.1:8989/focus")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 处理错误
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    // 处理响应
                }
            }
        })
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val project = event.manager.project
        val newFile = event.newFile ?: return
        val editor = (event.newEditor as? TextEditor)?.editor ?: return
        
        // 添加光标监听器
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val currentLine = event.editor.caretModel.primaryCaret.logicalPosition.line + 1
                sendProjectInfo(project, newFile, currentLine)
            }
        })

        // 初始发送当前位置
        val currentLine = editor.caretModel.primaryCaret.logicalPosition.line + 1
        sendProjectInfo(project, newFile, currentLine)
    }
} 