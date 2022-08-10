package com.ivy.wallet.ui.documents

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.domain.data.core.Document
import com.ivy.wallet.ui.ivyWalletCtx
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Purple2Dark
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.components.*
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSave
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.utils.drawColoredShadow
import com.ivy.wallet.utils.hideKeyboard
import com.ivy.wallet.utils.onScreenStart
import com.ivy.wallet.utils.thenIf
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun AddDocument(
    existingDocumentList: List<Document>,
    onDocumentAdd: (uri: Uri?) -> Unit = {},
    onClick: () -> Unit
) {
    val ivyContext = ivyWalletCtx()
    if (existingDocumentList.isNotEmpty()) {
        ViewDocumentsButton(onClick = onClick)
    } else {
        IvyBorderButton(
            modifier = Modifier.padding(start = 24.dp),
            iconStart = R.drawable.ic_plus,
            iconTint = UI.colors.pureInverse,
            text = "Add Document",
            onClick = {
                ivyContext.openFile {
                    onDocumentAdd(it)
                }
            }
        )
    }
}


@Composable
private fun ViewDocumentsButton(
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
    onDocumentRemove: (Document) -> Unit,
    onDocumentLongClick: (Document) -> Unit = {},
) {
    IvyModal(
        id = id,
        visible = visible,
        dismiss = {
            onDismiss()
        },
        PrimaryAction = {
            ModalSave(onClick = onDismiss)
        }
    ) {
        ViewDocumentContents(
            documentList,
            onDocumentAdd = onDocumentAdd,
            onDocumentClick = onDocumentClick,
            onDocumentRemove = onDocumentRemove,
            onDocumentLongClick = onDocumentLongClick
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun ColumnScope.ViewDocumentContents(
    existingDocumentList: List<Document>,
    onDocumentAdd: (uri: Uri?) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onDocumentRemove: (Document) -> Unit,
    onDocumentLongClick: (Document) -> Unit,
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
        onDocumentRemove = onDocumentRemove,
        onDocumentLongClick = onDocumentLongClick
    )

    Spacer(Modifier.height(100.dp))
}

@ExperimentalFoundationApi
@Composable
private fun DocumentsListDisplay(
    dataItems: List<Document>,
    onDocumentAdd: (uri: Uri?) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onDocumentRemove: (Document) -> Unit,
    onDocumentLongClick: (Document) -> Unit
) {
    val data = mutableListOf<Any>()
    data.add(AddNewDocument())
    data.addAll(dataItems)

    WrapContentRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalMarginBetweenItems = 12.dp,
        verticalMarginBetweenRows = 8.dp,
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
                        onDocumentLongClick(it)
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
                )
                .weight(1f, fill = false),
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

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.FileNameModal(
    id: UUID = UUID.randomUUID(),
    shouldShowKeyboard: Boolean = true,
    visible: Boolean = false,
    initialFileName: String,
    onDismiss: () -> Unit,
    onFileRenamed: (String) -> Unit
) {

    var titleTextFieldValue by remember(initialFileName) {
        mutableStateOf(
            TextFieldValue(
                initialFileName
            )
        )
    }

    var filename by remember(initialFileName) {
        mutableStateOf(initialFileName)
    }

    val titleFocus = FocusRequester()

    IvyModal(
        id = id,
        visible = visible,
        dismiss = {
            onDismiss()
        },
        PrimaryAction = {
            ModalSave(
                enabled = initialFileName.isNotEmpty()
            ) {
                onFileRenamed(filename)
                onDismiss()
            }
        }
    ) {

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = "Filename"
        )

        Spacer(Modifier.height(24.dp))

        IvyTitleTextField(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .focusRequester(titleFocus),
            dividerModifier = Modifier
                .padding(horizontal = 24.dp),
            value = titleTextFieldValue,
            hint = "Enter Filename",
            keyboardOptions = KeyboardOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(
                onNext = {

                }
            )
        ) {
            titleTextFieldValue = it
            filename = it.text
        }

        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(titleFocus) {
            if (shouldShowKeyboard) {
                titleFocus.requestFocus()
                delay(100) // Make sure you have delay here
                keyboard?.show()
            }
        }
    }
}

private class AddNewDocument