package com.ivy.wallet.ui.tags

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.core.common.AddNewTag
import com.ivy.wallet.core.model.Tag
import com.ivy.wallet.ui.theme.Blue2Dark
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.components.IvyBasicTextField
import com.ivy.wallet.ui.theme.components.IvyBorderButton
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalPositiveButton
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.utils.*
import java.util.*

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ShowTagModal(
    id: UUID = UUID.randomUUID(),
    visible: Boolean = false,
    onDismiss: () -> Unit,
    tagState: TagState,
    onTagAdd: (Tag) -> Unit,
    onTagEdit: (oldTag: Tag, newTag: Tag) -> Unit,
    onTagDelete: (Tag) -> Unit,
    onTagSelected: (Tag) -> Unit,
    onTagDeSelected: (Tag) -> Unit,
    onTagSearch: (String) -> Unit,
) {
    var showTagAddModal by remember {
        mutableStateOf(false)
    }

    var deleteTagModalVisible by remember {
        mutableStateOf(false)
    }

    var selectedTag by remember {
        mutableStateOf<Tag?>(null)
    }

    var selectedTagId by remember(selectedTag) {
        mutableStateOf(selectedTag?.id ?: UUID.randomUUID())
    }

    var searchQueryTextFieldValue by remember {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = onDismiss,
        PrimaryAction = {
            ModalPositiveButton(
                onClick = onDismiss,
                text = "Done",
                iconStart = R.drawable.ic_custom_document_s
            )
        },
        scrollState = null
    ) {
        HideKeyboard()

        Spacer(Modifier.height(32.dp))

        ModalTitle(text = "Tags")

        Spacer(Modifier.height(24.dp))

        SearchInput(
            searchQueryTextFieldValue = searchQueryTextFieldValue
        ) {
            searchQueryTextFieldValue = it
            onTagSearch(it.text)
        }

        Spacer(Modifier.height(24.dp))

        TagList(
            transactionTags = tagState.transactionTags,
            chunkedAllTags = tagState.chunkedAllTags,
            onAddNewTag = {
                showTagAddModal = true
            },
            onTagSelected = {
                onTagSelected(it)
            },
            onTagDeSelected = {
                onTagDeSelected(it)
            },
            onTagLongClick = {
                selectedTag = it
                showTagAddModal = true
            }
        )
    }

    val view = LocalView.current

    AddOrEditTagModal(
        id = selectedTagId,
        initialTag = selectedTag,
        visible = showTagAddModal,
        onDismiss = {
            showTagAddModal = false
            selectedTag = null
        },
        onTagAdd = {
            onTagAdd(it)
            selectedTag = null
            selectedTagId = UUID.randomUUID()
        },
        onTagDelete = {
            deleteTagModalVisible = true
            hideKeyboard(view)
        },
        onTagEdit = { oldTag, newTag ->
            onTagEdit(oldTag, newTag)
        }
    )

    DeleteModal(
        visible = deleteTagModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = "Are you sure you want to delete the following tag:\t'${selectedTag?.name}' ?",
        dismiss = { deleteTagModalVisible = false }
    ) {
        if (selectedTag != null) {
            deleteTagModalVisible = false
            onTagDelete(selectedTag!!)
            showTagAddModal = false
            selectedTag = null
        }
    }
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.TagList(
    chunkedAllTags: List<List<Any>> = emptyList(),
    transactionTags: Set<Tag>,
    onAddNewTag: () -> Unit,
    onTagSelected: (Tag) -> Unit = {},
    onTagDeSelected: (Tag) -> Unit = {},
    onTagLongClick: (Tag) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        items(chunkedAllTags) { item ->
            DisplayTagRow(
                transactionTags = transactionTags,
                item = item,
                onAddNewTag = onAddNewTag,
                onTagSelected = onTagSelected,
                onTagDeSelected = onTagDeSelected,
                onTagLongClick = onTagLongClick
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun DisplayTagRow(
    transactionTags: Set<Tag>,
    item: List<Any>,
    horizontalMarginBetweenItems: Dp = 12.dp,
    verticalMarginBetweenRows: Dp = 12.dp,
    onAddNewTag: () -> Unit,
    onTagSelected: (Tag) -> Unit = {},
    onTagDeSelected: (Tag) -> Unit = {},
    onTagLongClick: (Tag) -> Unit = {},
) {
    Spacer(modifier = Modifier.height(verticalMarginBetweenRows))

    Row(modifier = Modifier.padding(start = horizontalMarginBetweenItems, end = 24.dp)) {
        item.forEach {
            when (it) {
                is Tag -> {
                    ExistingTag(
                        tag = it,
                        selected = transactionTags.contains(it),
                        onClick = { onTagSelected(it) },
                        onLongClick = { onTagLongClick(it) },
                        onDeselect = {
                            onTagDeSelected(it)
                        }
                    )
                }
                is AddNewTag -> {
                    AddNewTagButton(onClick = onAddNewTag)
                }
            }
            Spacer(modifier = Modifier.width(horizontalMarginBetweenItems))
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ExistingTag(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
) {
    val tagColor = Blue2Dark

    Row(
        modifier = Modifier
            .thenIf(selected) {
                drawColoredShadow(tagColor)
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
                background(tagColor, UI.shapes.rFull)
            }
            .testTag("choose_category_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(if (selected) 12.dp else 8.dp))

//        ItemIconSDefaultIcon(
//            modifier = Modifier
//                .background(tagColor, CircleShape),
//            iconName = category.icon,
//            defaultIcon = R.drawable.ic_custom_category_s,
//            tint = findContrastTextColor(tagColor)
//        )

        Text(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(
                    start = if (selected) 12.dp else 12.dp,
                    end = if (selected) 20.dp else 24.dp
                )
                .weight(1f, fill = false),
            text = "#${tag.name}",
            style = UI.typo.b2.style(
                color = if (selected)
                    findContrastTextColor(tagColor) else UI.colors.pureInverse,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (selected) {
            val deselectBtnBackground = findContrastTextColor(tagColor)
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
}


@Composable
private fun HideKeyboard() {
    val view = LocalView.current
    onScreenStart {
        hideKeyboard(view)
    }
}

@Composable
private fun AddNewTagButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IvyBorderButton(
        modifier = modifier,
        text = stringResource(R.string.add_new),
        backgroundGradient = Gradient.solid(UI.colors.mediumInverse),
        iconStart = R.drawable.ic_plus,
        textStyle = UI.typo.b2.style(
            color = UI.colors.pureInverse,
            fontWeight = FontWeight.Bold
        ),
        iconTint = UI.colors.pureInverse,
        padding = 10.dp,
        onClick = onClick
    )
}

@Composable
private fun SearchInput(
    searchQueryTextFieldValue: TextFieldValue,
    onSetSearchQueryTextField: (TextFieldValue) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.rFull)
            .background(UI.colors.pure)
            .border(1.dp, Gray, UI.shapes.rFull),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(icon = R.drawable.ic_search)

        Spacer(Modifier.width(12.dp))

        val searchFocus = FocusRequester()
        IvyBasicTextField(
            modifier = Modifier
                .focusRequester(searchFocus)
                .padding(vertical = 12.dp),
            value = searchQueryTextFieldValue,
            hint = "Search Tags",
            onValueChanged = {
                onSetSearchQueryTextField(it)
            }
        )

        Spacer(Modifier.weight(1f).clickable
        {
            searchFocus.requestFocus()
        })

        IvyIcon(
            modifier = Modifier
                .clickable {
                    onSetSearchQueryTextField(selectEndTextFieldValue(""))
                }
                .padding(all = 12.dp), //enlarge click area
            icon = R.drawable.ic_outline_clear_24
        )

        Spacer(Modifier.width(8.dp))
    }
}