package com.ivy.wallet.ui.documents

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ivy.design.utils.showKeyboard
import com.ivy.wallet.domain.data.core.Document
import com.ivy.wallet.ui.theme.modal.ProgressModal
import com.ivy.wallet.utils.getFileName

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ShowDocumentModal(
    documentState: DocumentState,
    viewDocumentModalVisible: Boolean,
    onDocumentAdd: (Uri?, String) -> Unit,
    onDocumentRename: (Document, String) -> Unit,
    onDocumentDelete: (Document) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onModalDismiss: () -> Unit
) {
    val context = LocalContext.current

    var fileUri: Uri? by remember {
        mutableStateOf(null)
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
                onFileNameSet = { fName ->
                    onDocumentAdd(it, fName)
                })
        },
        onDocumentClick = onDocumentClick,
        onDocumentRemove = onDocumentDelete,
        onDocumentLongClick = {
            fileModalData = FileNameModalData(
                initialFileName = it.fileName,
                visible = true,
                onDismiss = {
                    fileModalData = null
                },
                onFileNameSet = { fName ->
                    onDocumentRename(it, fName)
                })
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
    if (fileNameModalData == null)
        return

    showKeyboard()

    FileNameModal(
        initialFileName = fileNameModalData.initialFileName,
        visible = fileNameModalData.visible,
        onDismiss = fileNameModalData.onDismiss,
        onFileRenamed = fileNameModalData.onFileNameSet
    )
}

data class FileNameModalData(
    val initialFileName: String,
    val visible: Boolean,
    val onDismiss: () -> Unit,
    val onFileNameSet: (String) -> Unit
)

