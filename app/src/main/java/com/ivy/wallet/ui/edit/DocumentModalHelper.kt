package com.ivy.wallet.ui.edit

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.ivy.design.utils.showKeyboard
import com.ivy.wallet.ui.RootActivity
import com.ivy.wallet.ui.theme.modal.ProgressModal
import com.ivy.wallet.utils.getFileName
import java.io.File

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ShowDocumentModal(
    viewModel: EditTransactionViewModel,
    viewDocumentModalVisible: Boolean,
    onModalDismiss: () -> Unit
) {
    val context = LocalContext.current
    val documentState by viewModel.documentState.collectAsState()

    var fileUri: Uri? by remember {
        mutableStateOf(null)
    }

    val fileName by remember(fileUri) {
        val name = fileUri?.let {
            context.getFileName(it, " ")
        } ?: ""
        mutableStateOf(name)
    }

    var fileModalData: FileNameModalData? by remember {
        mutableStateOf(null)
    }

    ViewDocumentModal(
        documentList = documentState.documentList,
        visible = viewDocumentModalVisible,
        onDismiss = onModalDismiss,
        onDocumentAdd = {
            fileUri = it
            fileModalData = FileNameModalData(
                initialFileName = context.getFileName(it, defaultFileName = ""),
                visible = true,
                onDismiss = {
                    fileModalData = null
                },
                onFileRenamed = { fName ->
                    viewModel.addDocument(fName, fileUri, context)
                })
        },
        onDocumentClick = {
            val viewFileUri = FileProvider.getUriForFile(
                (context as RootActivity),
                context.getApplicationContext().packageName + ".provider",
                File(it.filePath)
            )

            context.shareDocument(
                fileUri = viewFileUri
            )
        },
        onDocumentRemove = {
            viewModel.deleteDocument(it)
        }
    )

    ProgressModal(
        title = "Copying Document",
        description = "Please Wait, Copying Document",
        visible = documentState.showProgress
    )

    ShowFileNameModal(fileNameModalData = fileModalData)
}

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ShowFileNameModal(
    fileNameModalData: FileNameModalData?
) {
    if (fileNameModalData != null) {
        showKeyboard()

        FileNameModal(
            initialFileName = fileNameModalData.initialFileName,
            visible = fileNameModalData.visible,
            onDismiss = fileNameModalData.onDismiss,
            onFileRenamed = fileNameModalData.onFileRenamed
        )
    }
}

data class FileNameModalData(
    val initialFileName: String,
    val visible: Boolean,
    val onDismiss: () -> Unit,
    val onFileRenamed: (String) -> Unit
)

