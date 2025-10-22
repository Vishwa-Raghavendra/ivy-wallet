package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.wallet.R
import com.ivy.wallet.io.SmsMessage
import com.ivy.wallet.ui.theme.components.IvyButton

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.SmsImportModal(
    visible: Boolean,
    smsMessages: List<SmsMessage>,
    onSmsSelected: (SmsMessage) -> Unit,
    dismiss: () -> Unit
) {
    var selectedMessage by remember { mutableStateOf<SmsMessage?>(null) }

    IvyModal(id = null, visible = visible, dismiss = dismiss, PrimaryAction = {
        IvyButton(
            text = "Import",
            onClick = {
                selectedMessage?.let {
                    onSmsSelected(it)
                    dismiss()
                }
            },
            enabled = selectedMessage != null
        )
    }) {

        Spacer(modifier = Modifier.height(32.dp))
        ModalTitle(text = "Select an SMS to import")
        Spacer(modifier = Modifier.height(16.dp))

        for (message in smsMessages) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedMessage = message
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMessage == message,
                    onClick = { selectedMessage = message }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = message.body)
            }
        }
    }
}