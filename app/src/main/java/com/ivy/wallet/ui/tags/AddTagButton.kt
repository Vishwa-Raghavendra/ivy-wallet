package com.ivy.wallet.ui.tags

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.core.model.Tag
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Orange3
import com.ivy.wallet.ui.theme.components.IvyBorderButton
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.findContrastTextColor


@Composable
fun AddTagButton(
    transactionTags: Set<Tag>,
    onClick: () -> Unit
) {
    if (transactionTags.isNotEmpty()) {
        ViewTagsButton(transactionTags = transactionTags, onClick = onClick)
    } else {
        AddTagsButton(onClick = onClick)
    }
}

@Composable
private fun ViewTagsButton(
    transactionTags: Set<Tag>,
    onClick: () -> Unit,
) {
    val contrastColor = findContrastTextColor(Orange3)
    IvyButton(
        modifier = Modifier.padding(start = 24.dp),
        text = if (transactionTags.size <= 1) "${transactionTags.size}\t Tag" else "${transactionTags.size}\t Tags",
        backgroundGradient = Gradient.solid(Orange3),
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

@Composable
private fun AddTagsButton(
    onClick: () -> Unit,
) {
    IvyBorderButton(
        modifier = Modifier.padding(start = 24.dp),
        iconStart = R.drawable.ic_plus,
        iconTint = UI.colors.pureInverse,
        text = "Add Tags",
        onClick = onClick
    )
}
