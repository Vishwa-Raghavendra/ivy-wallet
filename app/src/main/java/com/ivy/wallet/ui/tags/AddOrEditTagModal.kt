package com.ivy.wallet.ui.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ivy.wallet.R
import com.ivy.wallet.core.model.Tag
import com.ivy.wallet.ui.theme.components.DeleteButton
import com.ivy.wallet.ui.theme.components.IvyTitleTextField
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalPositiveButton
import com.ivy.wallet.ui.theme.modal.ModalTitle
import java.util.*


@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.AddOrEditTagModal(
    id: UUID,
    visible: Boolean = false,
    initialTag: Tag? = null,
    onTagAdd: (Tag) -> Unit = {},
    onTagEdit: (oldTag: Tag, newTag: Tag) -> Unit = { _, _ -> },
    onTagDelete: (Tag) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val titleFocus = FocusRequester()

    var titleTextFieldValue by remember(id) {
        mutableStateOf(
            TextFieldValue(
                initialTag?.name ?: "",
                selection = TextRange(initialTag?.name?.length ?: 0)
            )
        )
    }

    var filename by remember(id) {
        mutableStateOf(initialTag?.name ?: "")
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = onDismiss,
        PrimaryAction = {
            ModalPositiveButton(
                onClick = {
                    if (initialTag != null) {
                        onTagEdit(initialTag, initialTag.copy(name = filename))
                    } else {
                        onTagAdd(Tag(filename))
                    }

                    onDismiss()
                },
                text = "Done",
                iconStart = R.drawable.ic_custom_document_s,
                enabled = filename.isNotEmpty()
            )
        }
    )
    {
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .padding(start = 0.dp, end = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalTitle(text = if (initialTag == null) "Tag Name" else "Edit Tag Name")

            Spacer(modifier = Modifier.weight(1f))

            if (initialTag != null) {
                DeleteButton(
                    hasShadow = false,
                    onClick = {
                        onTagDelete(initialTag)
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        IvyTitleTextField(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .focusRequester(titleFocus),
            dividerModifier = Modifier
                .padding(horizontal = 24.dp),
            value = titleTextFieldValue,
            hint = "Enter TagName",
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

        LaunchedEffect(titleFocus) {
            titleFocus.requestFocus()
        }
    }
}