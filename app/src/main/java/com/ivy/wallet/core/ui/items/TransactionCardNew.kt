package com.ivy.wallet.core.ui.items

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.core.model.TransactionNew
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.core.Account
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.ui.theme.*
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import com.ivy.wallet.utils.format
import com.ivy.wallet.utils.formatNicely
import com.ivy.wallet.utils.timeNowUTC
import java.time.LocalDateTime

private data class AmountTypeStyle(
    @DrawableRes val icon: Int,
    val gradient: Gradient,
    val iconTint: Color,
    val textColor: Color
)


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.TransactionCardNew(
    transaction: TransactionNew,
    onTransactionClick: () -> Unit,
    onAccountClick: (Account?) -> Unit,
    onCategoryClick: (Category?) -> Unit,
    showPlannedPaymentsData: Boolean = true,
    onPayOrGet: (TransactionNew) -> Unit = {},
    onSkipTransaction: (TransactionNew) -> Unit = {}
) {
    val (isPlannedPaymentTransaction, reduceTrnsDescHeight) = remember(transaction.id.toString()) {
        Pair(
            transaction.dueDate != null && transaction.dateTime == null && showPlannedPaymentsData,
            !transaction.title.isNullOrEmpty()
        )
    }

    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.r4)
            .clickable {
                onTransactionClick()
//                if (baseData.accounts.find { it.id == transaction.accountId } != null) {
//                    onClick(transaction)
//                }
            }
            .background(UI.colors.medium, UI.shapes.r4)
            .testTag("transaction_card_new")
            .animateItemPlacement(animationSpec = tween(300))
    ) {
        Spacer(Modifier.height(20.dp))

        TransactionHeaderIcons(transaction, onAccountClick = onAccountClick, onCategoryClick)

        if (isPlannedPaymentTransaction) {
            PlannedPaymentsUpcomingOverDueText(transaction)
        }

        TransactionTime(transaction.dateTime)

        TransactionTitle(transaction.title, reduceHeight = isPlannedPaymentTransaction)

        TransactionDescription(transaction.description, reduceHeight = reduceTrnsDescHeight)

        Spacer(Modifier.height(if (isPlannedPaymentTransaction) 12.dp else 16.dp))

        TransactionAmount(transaction)

        if (showPlannedPaymentsData) {
            PlannedPaymentsButtons(
                transaction,
                isPlannedPaymentTransaction,
                onPayOrGet,
                onSkipTransaction
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PlannedPaymentsButtons(
    transaction: TransactionNew,
    plannedPaymentTransaction: Boolean,
    onPayOrGet: (TransactionNew) -> Unit,
    onSkipTransaction: (TransactionNew) -> Unit,
) {
    if (plannedPaymentTransaction) {
        val isExpense = remember(transaction.id) {
            transaction.type == TransactionType.EXPENSE
        }

        Spacer(Modifier.height(16.dp))

        Row {
            IvyButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
                text = stringResource(R.string.skip),
                wrapContentMode = false,
                backgroundGradient = Gradient.solid(UI.colors.pure),
                textStyle = UI.typo.b2.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.Bold
                )
            ) {
                onSkipTransaction(transaction)
            }

            Spacer(Modifier.width(8.dp))

            IvyButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp),
                text = if (isExpense) stringResource(R.string.pay) else stringResource(R.string.get),
                wrapContentMode = false,
                backgroundGradient = if (isExpense) gradientExpenses() else GradientGreen,
                textStyle = UI.typo.b2.style(
                    color = if (isExpense) UI.colors.pure else White,
                    fontWeight = FontWeight.Bold
                )
            ) {
                onPayOrGet(transaction)
            }
        }
    }
}

@Composable
private fun TransactionTime(dateTime: LocalDateTime?) {
    if (dateTime != null) {
        val formattedDateText = remember(dateTime) {
            dateTime.format(
                "hh:mm a"
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = formattedDateText,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = UI.typo.nC.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionAmount(transaction: TransactionNew) {
    val colors = UI.colors
    Row(
        modifier = Modifier.testTag("type_amount_currency_new"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val style = remember(transaction.id) {
            val today = LocalDateTime.now()
            when (transaction.type) {
                TransactionType.INCOME -> {
                    AmountTypeStyle(
                        icon = R.drawable.ic_income,
                        gradient = GradientGreen,
                        iconTint = White,
                        textColor = Green
                    )
                }
                TransactionType.EXPENSE -> {
                    when {
                        transaction.dueDate != null && transaction.dueDate.isAfter(today) -> {
                            //Upcoming Expense
                            AmountTypeStyle(
                                icon = R.drawable.ic_expense,
                                gradient = GradientOrangeRevert,
                                iconTint = White,
                                textColor = Orange
                            )
                        }
                        transaction.dueDate != null && transaction.dueDate.isBefore(
                            today.toLocalDate().atStartOfDay()
                        ) -> {
                            //Overdue Expense
                            AmountTypeStyle(
                                icon = R.drawable.ic_overdue,
                                gradient = GradientRed,
                                iconTint = White,
                                textColor = Red
                            )
                        }
                        else -> {
                            //Normal Expense
                            AmountTypeStyle(
                                icon = R.drawable.ic_expense,
                                gradient = Gradient.black(colors),
                                iconTint = White,
                                textColor = colors.pureInverse
                            )
                        }
                    }
                }
                TransactionType.TRANSFER -> {
                    //Transfer
                    AmountTypeStyle(
                        icon = R.drawable.ic_transfer,
                        gradient = GradientIvy,
                        iconTint = White,
                        textColor = Ivy
                    )
                }
            }
        }

        IvyIcon(
            modifier = Modifier.background(style.gradient.asHorizontalBrush(), CircleShape),
            icon = style.icon,
            tint = style.iconTint
        )

        Spacer(Modifier.width(12.dp))

        AmountCurrencyB1(
            amount = transaction.amount,
            currency = transaction.account.currencyOrBaseCurrency,
            textColor = style.textColor
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun TransactionTitle(title: String?, reduceHeight: Boolean = false) {
    if (!title.isNullOrEmpty()) {
        Spacer(Modifier.height(if (reduceHeight) 8.dp else 12.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = title,
            style = UI.typo.b1.style(
                fontWeight = FontWeight.ExtraBold,
                color = UI.colors.pureInverse
            )
        )

    }
}

@Composable
private fun TransactionDescription(description: String?, reduceHeight: Boolean = false) {
    if (!description.isNullOrEmpty()) {
        Spacer(Modifier.height(if (reduceHeight) 4.dp else 8.dp))

        Text(
            text = description,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = UI.typo.nC.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionHeaderIcons(
    transaction: TransactionNew,
    onAccountClick: (Account?) -> Unit,
    onCategoryClick: (Category?) -> Unit,
) {
    if (transaction.type == TransactionType.TRANSFER) {
        TransferHeaderIcon(transaction.account, transaction.toAccount, onAccountClick)
    } else {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(transaction.category, onCategoryClick)

            Spacer(Modifier.width(12.dp))

            AccountIcon(transaction.account, onAccountClick)
        }
    }
}

@Composable
private fun CategoryIcon(category: Category?, onCategoryClick: (Category?) -> Unit) {
    if (category != null) {
        BadgeNew(
            text = category.name,
            backgroundColor = category.color.toComposeColor(),
            icon = category.icon,
            defaultIcon = R.drawable.ic_custom_category_s
        ) {
            onCategoryClick(category)
        }
    }
}

@Composable
private fun AccountIcon(account: Account, onAccountClick: (Account?) -> Unit) {
    BadgeNew(
        text = account.name,
        backgroundColor = account.color.toComposeColor(),
        icon = account.icon,
        defaultIcon = R.drawable.ic_custom_account_s
    ) {
        onAccountClick(account)
    }
}

@Composable
private fun TransferHeaderIcon(
    fromAccount: Account?,
    toAccount: Account?,
    onAccountClick: (Account?) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .background(UI.colors.pure, UI.shapes.rFull),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))

        ItemIconSDefaultIcon(
            iconName = fromAccount?.icon,
            defaultIcon = R.drawable.ic_custom_account_s
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable {
                    onAccountClick(fromAccount)
                },
            // used toString() in case of null
            text = fromAccount?.name.toString(),
            style = UI.typo.c.style(
                fontWeight = FontWeight.ExtraBold,
                color = UI.colors.pureInverse
            )
        )

        Spacer(Modifier.width(12.dp))

        IvyIcon(icon = R.drawable.ic_arrow_right)

        Spacer(Modifier.width(12.dp))

        ItemIconSDefaultIcon(
            iconName = toAccount?.icon,
            defaultIcon = R.drawable.ic_custom_account_s
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable {
                    onAccountClick(toAccount)
                },
            // used toString() in case of null
            text = toAccount?.name.toString(),
            style = UI.typo.c.style(
                fontWeight = FontWeight.ExtraBold,
                color = UI.colors.pureInverse
            )
        )

        Spacer(Modifier.width(20.dp))
    }
}

@Composable
private fun PlannedPaymentsUpcomingOverDueText(transaction: TransactionNew) {
    if (transaction.dueDate != null) {
        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(
                R.string.due_on,
                transaction.dueDate.formatNicely()
            ).uppercase(),
            style = UI.typo.nC.style(
                color = if (transaction.dueDate.isAfter(timeNowUTC()))
                    Orange else UI.colors.gray,
                fontWeight = FontWeight.Bold
            )
        )
    }
}


