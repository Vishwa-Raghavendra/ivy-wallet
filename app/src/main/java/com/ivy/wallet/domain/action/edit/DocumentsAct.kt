package com.ivy.wallet.domain.action.edit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ivy.wallet.utils.ioThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import javax.inject.Inject

class DocumentsAct @Inject constructor() {
    companion object {
        const val DOCUMENT_FOLDER_NAME = "documents"
        private const val MODE_READ = "r"
    }

    suspend fun addDocument(
        documentURI: Uri?,
        context: Context,
        onProgressStart: suspend () -> Unit,
        onProgressEnd: suspend () -> Unit
    ) {
        ioThread {
            if (documentURI == null)
                return@ioThread

            onProgressStart()

            val documentDestinationFolder = File(context.filesDir, DOCUMENT_FOLDER_NAME)


            if (!documentDestinationFolder.exists()) {
                documentDestinationFolder.mkdirs()
            }

            copyFileToDestination(
                context = context,
                documentURI = documentURI,
                documentDestinationFolder = documentDestinationFolder,
            )

            onProgressEnd()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun copyFileToDestination(
        context: Context,
        documentURI: Uri,
        documentDestinationFolder: File
    ) {
        val documentFileName = context.getFileName(
            uri = documentURI,
            defaultFileName = "file${System.currentTimeMillis()}"
        )

        withContext(Dispatchers.IO) {
            context.contentResolver.openFileDescriptor(documentURI, MODE_READ).use { descriptor ->
                descriptor?.fileDescriptor?.let {
                    BufferedInputStream(FileInputStream(it)).use { inStream ->

                        val documentDestinationFilePath =
                            getFilePath(documentDestinationFolder, documentFileName)

                        withContext(Dispatchers.IO) {
                            BufferedOutputStream(FileOutputStream(documentDestinationFilePath)).use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getFilePath(documentFolder: File, documentFileName: String): String {
        return documentFolder.absolutePath.let {
            if (!it.endsWith(File.separator)) "$it${File.separator}"
            else it
        } + documentFileName
    }


    private fun Context.getFileName(uri: Uri, defaultFileName: String): String =
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> getContentFileName(uri, defaultFileName)
            else -> uri.path?.let(::File)?.name ?: defaultFileName
        }

    private fun Context.getContentFileName(uri: Uri, defaultFileName: String): String =
        runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    .let(cursor::getString) ?: defaultFileName
            }
        }.getOrElse {
            defaultFileName
        }!!
}