package com.ivy.wallet.core.utils

import androidx.compose.ui.graphics.toArgb
import com.ivy.wallet.R
import com.ivy.wallet.core.model.Stats
import com.ivy.wallet.domain.data.core.Category
import com.ivy.wallet.domain.data.core.ExchangeRate
import com.ivy.wallet.stringRes
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.RedLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.math.roundToInt

fun emptyExchangeRate() = ExchangeRate("", "", 0.0)

fun Double.roundTo2Digits(): Double {
    return try {
        (this * 100.0).roundToInt() / 100.0
    } catch (e: Exception) {
        0.0
    }
}

fun statsDummy() = Stats(
    0.0,
    0.0,
    0,
    0,
    0.0,
    0.0,
    0,
    0
)

val unSpecifiedCategory = Category(stringRes(R.string.unspecified), color = Gray.toArgb())
val accountTransfersCategory =
    Category(stringRes(R.string.account_transfers), RedLight.toArgb(), "transfer")

fun Category.isCategoryUnspecified() =
    this.id == unSpecifiedCategory.id || this.id == accountTransfersCategory.id

fun <E> MutableCollection<E>.addOrRemove(item: E) {
    val isItemPresent = this.contains(item)
    if (isItemPresent)
        this.remove(item)
    else
        this.add(item)
}

suspend fun <A, B> Iterable<A>.pmap(scope: CoroutineScope, f: suspend (A) -> B): List<B> =
    with(scope) {
        map { async { f(it) } }.awaitAll()
    }