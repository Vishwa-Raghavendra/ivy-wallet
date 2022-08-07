package com.ivy.wallet.domain.action.edit

import android.content.Context
import android.net.Uri
import com.ivy.wallet.domain.data.core.Document
import com.ivy.wallet.io.persistence.dao.DocumentDao
import com.ivy.wallet.utils.getFileName
import com.ivy.wallet.utils.ioThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import javax.inject.Inject

class DocumentsLogic @Inject constructor(
    private val documentsDao: DocumentDao
) {
    companion object {
        const val DOCUMENT_FOLDER_NAME = "documents"
        private const val MODE_READ = "r"
    }

    suspend fun findByTransactionId(transactionId: UUID): List<Document> {
        return ioThread {
            documentsDao.findByTransactionId(transactionId).map {
                it.toDomain()
            }
        }
    }

    suspend fun findById(id: UUID): Document {
        return ioThread {
            documentsDao.findById(id).toDomain()
        }
    }

    suspend fun deleteDocument(document: Document): Document {
        return ioThread {
            documentsDao.deleteById(document.id)
            deleteDocumentFromStorage(document)
            document
        }
    }

    private suspend fun deleteDocumentFromStorage(document: Document) {
        val file = File(document.filePath)
        if (file.exists())
            file.delete()
    }

    suspend fun addDocument(
        transactionId: UUID,
        documentURI: Uri,
        context: Context,
        onProgressStart: suspend () -> Unit,
        onProgressEnd: suspend () -> Unit
    ): Document {
        return ioThread {
            onProgressStart()

            val documentFileName = getFilename(
                context = context,
                documentURI = documentURI,
                defaultFileName = "file${System.currentTimeMillis()}"
            )

            val documentDestinationFolder = File(context.filesDir, DOCUMENT_FOLDER_NAME)
            val documentDestinationFilePath = getDestinationFolderPath(
                context = context,
                documentURI = documentURI,
                documentDestinationFolder = documentDestinationFolder,
            )


            if (!documentDestinationFolder.exists()) {
                documentDestinationFolder.mkdirs()
            }

            copyFileToDestination(
                context = context,
                documentURI = documentURI,
                documentDestinationFilePath = documentDestinationFilePath,
            )

            val document = addDocumentToDb(
                transactionId = transactionId,
                documentFileName = documentFileName,
                documentDestinationFilePath = documentDestinationFilePath
            )

            onProgressEnd()

            document
        }
    }

    private suspend fun addDocumentToDb(
        transactionId: UUID,
        documentFileName: String,
        documentDestinationFilePath: String
    ): Document {
        return ioThread {
            val document =
                Document(
                    transactionId = transactionId,
                    filePath = documentDestinationFilePath,
                    fileName = documentFileName
                )

            documentsDao.save(document.toEntity())

            return@ioThread document
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun copyFileToDestination(
        context: Context,
        documentURI: Uri,
        documentDestinationFilePath: String
    ) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openFileDescriptor(documentURI, MODE_READ).use { descriptor ->
                descriptor?.fileDescriptor?.let {
                    BufferedInputStream(FileInputStream(it)).use { inStream ->
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

    private fun getDestinationFolderPath(
        context: Context,
        documentURI: Uri,
        documentDestinationFolder: File
    ): String {
        val documentFileName =
            getFilename(
                context = context,
                documentURI = documentURI,
                defaultFileName = "file${System.currentTimeMillis()}"
            )

        val documentDestinationFilePath = documentDestinationFolder.absolutePath.let {
            if (!it.endsWith(File.separator)) "$it${File.separator}"
            else it
        } + applyDuplicateFilenameFix(documentFileName)

        return documentDestinationFilePath
    }

    private fun getFilename(
        context: Context,
        documentURI: Uri,
        defaultFileName: String
    ): String {
        return context.getFileName(
            uri = documentURI,
            defaultFileName = defaultFileName
        )
    }

    /**
     * Added "_file${System.currentTimeMillis()}" suffix to destination folder path to accommodate
     * duplicate files
     */
    private fun applyDuplicateFilenameFix(documentFileName: String): String {
        val index = documentFileName.lastIndexOf(".")
        val suffix = "_file${System.currentTimeMillis()}"

        return if (index == -1)
            return documentFileName + suffix
        else
            documentFileName.substring(0, index) + suffix + documentFileName.substring(index)
    }
}