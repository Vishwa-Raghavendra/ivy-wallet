package com.ivy.wallet.ui.edit

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.domain.data.core.Document
import com.ivy.wallet.ui.IvyWalletPreview
import com.ivy.wallet.ui.ivyWalletCtx
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Purple2Dark
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.components.*
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.utils.drawColoredShadow
import com.ivy.wallet.utils.hideKeyboard
import com.ivy.wallet.utils.onScreenStart
import com.ivy.wallet.utils.thenIf
import java.util.*

@Composable
fun AddDocument(
    existingDocumentList: List<Document> = emptyList(),
    onClick: () -> Unit = {}
) {
    if (existingDocumentList.isNotEmpty()) {
        ViewDocuments(onClick = onClick)
    } else {
        IvyBorderButton(
            modifier = Modifier.padding(start = 24.dp),
            iconStart = R.drawable.ic_plus,
            iconTint = UI.colors.pureInverse,
            text = "Add Document",
            onClick = onClick
        )
    }
}


@Composable
private fun ViewDocuments(
    onClick: () -> Unit,
) {
    val contrastColor = findContrastTextColor(Purple2Dark)
    IvyButton(
        modifier = Modifier.padding(start = 24.dp),
        text = "View Documents",
        backgroundGradient = Gradient.solid(Purple2Dark),
        textStyle = UI.typo.b2.style(
            color = contrastColor,
            fontWeight = FontWeight.Bold
        ),
        iconTint = contrastColor,
        hasGlow = false,
        iconEnd = R.drawable.ic_onboarding_next_arrow,
        wrapContentMode = true,
        onClick = onClick
    )
}

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ViewDocumentModal(
    id: UUID = UUID.randomUUID(),
    documentList: List<Document>,
    visible: Boolean = false,
    onDismiss: () -> Unit,
    onDocumentAdd: (uri: Uri?) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onDocumentRemove: (Document) -> Unit
) {
    IvyModal(
        id = id,
        visible = visible,
        dismiss = {
            onDismiss()
        },
        PrimaryAction = {

        }
    ) {
        ViewDocumentContents(
            documentList,
            onDocumentAdd = onDocumentAdd,
            onDocumentClick = onDocumentClick,
            onDocumentRemove = onDocumentRemove
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun ColumnScope.ViewDocumentContents(
    existingDocumentList: List<Document>,
    onDocumentAdd: (uri: Uri?) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onDocumentRemove: (Document) -> Unit
) {
    val view = LocalView.current
    onScreenStart {
        hideKeyboard(view)
    }

    Spacer(Modifier.height(32.dp))

    ModalTitle(
        text = "Documents"
    )

    Spacer(Modifier.height(24.dp))

    DocumentsListDisplay(
        dataItems = existingDocumentList,
        onDocumentAdd = onDocumentAdd,
        onDocumentClick = onDocumentClick,
        onDocumentRemove = onDocumentRemove
    )

    Spacer(Modifier.height(100.dp))
}

@ExperimentalFoundationApi
@Composable
private fun DocumentsListDisplay(
    dataItems: List<Document>,
    onDocumentAdd: (uri: Uri?) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onDocumentRemove: (Document) -> Unit
) {
    val data = mutableListOf<Any>()
    data.add(AddNewDocument())
    data.addAll(dataItems)

    WrapContentRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalMarginBetweenItems = 12.dp,
        verticalMarginBetweenRows = 12.dp,
        items = data
    ) {
        when (it) {
            is Document -> {
                DocumentDisplayItem(
                    item = it.fileName,
                    onClick = {
                        onDocumentClick(it)
                    },
                    onLongClick = {
                        //Do Nothing
                    },
                    onDeselect = {
                        onDocumentRemove(it)
                    }
                )
            }
            is AddNewDocument -> {
                AddNewDocumentButton(onDocumentAdd = onDocumentAdd)
            }
        }
    }
}

@Composable
private fun AddNewDocumentButton(onDocumentAdd: (uri: Uri?) -> Unit) {
    val ivyContext = ivyWalletCtx()
    IvyBorderButton(
        modifier = Modifier.padding(end = 16.dp),
        iconStart = R.drawable.ic_plus,
        iconTint = UI.colors.pureInverse,
        text = "Add Document"
    ) {
        ivyContext.openFile {
            onDocumentAdd(it)
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun DocumentDisplayItem(
    item: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
) {
    val categoryColor = Red

    Row(
        modifier = Modifier
            .thenIf(selected) {
                drawColoredShadow(categoryColor)
            }
            .clip(UI.shapes.rFull)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 2.dp,
                color = if (selected) UI.colors.pureInverse else UI.colors.medium,
                shape = UI.shapes.rFull
            )
            .thenIf(selected) {
                background(categoryColor, UI.shapes.rFull)
            }
            .testTag("choose_document_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(if (selected) 12.dp else 8.dp))

        ItemIconSDefaultIcon(
            modifier = Modifier
                .background(categoryColor, CircleShape),
            iconName = null,
            defaultIcon = R.drawable.ic_custom_document_s,
            tint = findContrastTextColor(categoryColor)
        )

        Text(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(
                    start = if (selected) 12.dp else 12.dp,
                    end = if (selected) 20.dp else 24.dp
                ),
            text = item,
            style = UI.typo.b2.style(
                color = if (selected)
                    findContrastTextColor(categoryColor) else UI.colors.pureInverse,
                fontWeight = FontWeight.SemiBold
            )
        )

        val deselectBtnBackground = findContrastTextColor(categoryColor)
        IvyCircleButton(
            modifier = Modifier
                .size(32.dp),
            icon = R.drawable.ic_remove,
            backgroundGradient = Gradient.solid(deselectBtnBackground),
            tint = findContrastTextColor(deselectBtnBackground)
        ) {
            onDeselect()
        }

        Spacer(Modifier.width(8.dp))
    }
}

private class AddNewDocument

@Preview
@Composable
fun PreviewAddDocument() {
    IvyWalletPreview {
        AddDocument()
    }
}