package com.ivy.wallet.domain.action.edit

import android.content.Context
import android.net.Uri
import com.ivy.wallet.domain.data.core.Document
import com.ivy.wallet.io.persistence.dao.DocumentDao
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

    suspend fun addDocument(
        documentFileName: String,
        associatedId: UUID,
        documentURI: Uri,
        context: Context,
        onProgressStart: suspend () -> Unit,
        onProgressEnd: suspend () -> Unit
    ): Document {
        return ioThread {
            onProgressStart()

            val documentDestinationFolder = File(context.filesDir, DOCUMENT_FOLDER_NAME)
            val documentDestinationFilePath = getDestinationFolderPath(
                documentFileName = documentFileName,
                documentDestinationFolder = documentDestinationFolder,
            )

            if (!documentDestinationFolder.exists()) {
                documentDestinationFolder.mkdirs()
            }

            copyDocumentToDestination(
                context = context,
                documentURI = documentURI,
                documentDestinationFilePath = documentDestinationFilePath,
            )

            val document = addDocumentToDb(
                associatedId = associatedId,
                documentFileName = documentFileName,
                documentDestinationFilePath = documentDestinationFilePath
            )

            onProgressEnd()

            document
        }
    }

    suspend fun renameDocument(
        context: Context,
        document: Document,
        newFileName: String
    ) {
        ioThread {
            val documentsFolderPath = File(context.filesDir, DOCUMENT_FOLDER_NAME)

            val originalFile = File(document.filePath)
            val renamedFile = File(documentsFolderPath, applyDuplicateFilenameFix(newFileName))
            renameFile(originalFile, renamedFile)

            saveDocumentDetailsToDB(
                document.copy(
                    filePath = renamedFile.absolutePath,
                    fileName = newFileName
                )
            )
        }
    }

    suspend fun deleteDocument(document: Document): Document {
        return ioThread {
            deleteDocumentFromDB(document)
            deleteDocumentFromStorage(document)
            document
        }
    }


    private fun renameFile(from: File, to: File): Boolean {
        return from.exists() && from.renameTo(to)
    }

    private fun deleteDocumentFromStorage(document: Document) {
        val file = File(document.filePath)
        if (file.exists())
            file.delete()
    }


    private suspend fun addDocumentToDb(
        associatedId: UUID,
        documentFileName: String,
        documentDestinationFilePath: String
    ): Document {
        return ioThread {
            val document =
                Document(
                    associatedId = associatedId,
                    filePath = documentDestinationFilePath,
                    fileName = documentFileName
                )

            saveDocumentDetailsToDB(document)

            return@ioThread document
        }
    }

    //Nested withContext(Dispatchers.IO) is necessary to fix BlockingMethodInNonBlockingContext error
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun copyDocumentToDestination(
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
        documentFileName: String,
        documentDestinationFolder: File
    ): String {
        val documentDestinationFilePath = documentDestinationFolder.absolutePath.let {
            if (!it.endsWith(File.separator)) "$it${File.separator}"
            else it
        } + applyDuplicateFilenameFix(documentFileName)

        return documentDestinationFilePath
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


    //----------------------  DB Operations  ----------------------------

    suspend fun findByAssociatedId(transactionId: UUID): List<Document> {
        return ioThread {
            documentsDao.findByAssociatedId(transactionId).map {
                it.toDomain()
            }
        }
    }

    suspend fun findByDocumentId(documentId: UUID): Document {
        return ioThread {
            documentsDao.findById(documentId).toDomain()
        }
    }

    private suspend fun saveDocumentDetailsToDB(document: Document) {
        ioThread {
            documentsDao.save(document.toEntity())
        }
    }

    private suspend fun deleteDocumentFromDB(document: Document){
        documentsDao.deleteById(document.id)
    }
}