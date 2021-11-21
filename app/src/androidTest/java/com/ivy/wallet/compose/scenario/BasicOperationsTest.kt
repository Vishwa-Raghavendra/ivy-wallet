package com.ivy.wallet.compose.scenario

import androidx.compose.ui.test.*
import com.ivy.wallet.compose.IvyComposeTest
import com.ivy.wallet.compose.helpers.*
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class BasicOperationsTest : IvyComposeTest() {

    private val onboarding = OnboardingFlow(composeTestRule)
    private val amountInput = AmountInput(composeTestRule)
    private val accountModal = AccountModal(composeTestRule)
    private val mainBottomBar = MainBottomBar(composeTestRule)
    private val transactionFlow = TransactionFlow(composeTestRule)
    private val homeTab = HomeTab(composeTestRule)


    @Test
    fun contextLoads() {
    }

    @Test
    fun OnboardAndAdjustBalance() {
        onboarding.quickOnboarding()

        composeTestRule.onNode(hasText("To accounts"))
            .performClick()

        composeTestRule.onNode(hasText("Cash"))
            .performClick()

        composeTestRule
            .onNode(hasText("Edit"))
            .performClick()

        accountModal.clickBalance()

        amountInput.enterNumber("1,025.98")

        accountModal.clickSave()

        composeTestRule.onNodeWithTag("balance")
            .assertTextEquals("USD", "1,025", ".98")

        composeTestRule.onNodeWithTag("toolbar_close")
            .performClick()

        mainBottomBar.clickHome()

        composeTestRule.onNodeWithTag("home_balance")
            .assertTextEquals("USD", "1,025", ".98")

        homeTab.assertBalance(
            amount = "1,025",
            amountDecimalPart = ".98"
        )
    }

    @Test
    fun CreateIncome() {
        onboarding.quickOnboarding()

        transactionFlow.addIncome(
            amount = 5000.0,
            title = "Salary",
            category = "Investments"
        )

        composeTestRule.onNodeWithTag("transaction_card")
            .assertIsDisplayed()
    }

    @Test
    fun AddSeveralTransactions() {
        onboarding.quickOnboarding()

        transactionFlow.addIncome(
            amount = 1000.0,
            title = null,
            category = null
        )

        transactionFlow.addExpense(
            amount = 249.75,
            title = "Food",
            category = "Groceries"
        )

        transactionFlow.addExpense(
            amount = 300.25,
            title = null,
            category = null
        )

        homeTab.assertBalance(
            amount = "450",
            amountDecimalPart = ".00"
        )
    }
}