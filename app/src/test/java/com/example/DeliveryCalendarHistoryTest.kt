package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.DeliveryEntity
import com.example.data.PaymentEntity
import com.example.ui.settings.DeliveryCalendarHistoryCard
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeliveryCalendarHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendarHistory_showsDailyBreakdownAndEmptyStateOnDateSelection() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val date1 = String.format(Locale.US, "%04d-%02d-05", year, month)
        val date2 = String.format(Locale.US, "%04d-%02d-10", year, month)

        val deliveries = listOf(
            DeliveryEntity(
                id = 1L,
                customerId = 1L,
                customerName = "Nena Store",
                customerAddress = "Purok 1",
                purifiedQty = 5,
                alkalineQty = 2,
                freePurifiedQty = 1,
                totalDue = 230.0,
                amountPaid = 230.0,
                paymentStatus = "Paid",
                commissionGallons = 6.0,
                dateString = date1
            )
        )
        val payments = listOf(
            PaymentEntity(
                id = 1L,
                customerId = 2L,
                customerName = "Mario",
                amount = 70.0,
                paymentType = "Utang Payment",
                commissionGallonsEarned = 2.0,
                remainingBalance = 0.0,
                dateString = date1
            )
        )

        composeRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DeliveryCalendarHistoryCard(
                        deliveries = deliveries,
                        payments = payments,
                        commissionPerGallon = 5.0
                    )
                }
            }
        }

        // Tap day 5 -> shows deliveries & Bayad Utang totals
        composeRule.onNodeWithTag("calendar_day_$date1").performScrollTo().performClick()
        composeRule.onNodeWithTag("daily_history_breakdown_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nena Store").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mario").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("5 Pur • 2 Alk").performScrollTo().assertIsDisplayed()

        // Tap day 10 (no activity) -> shows Tagalog empty state
        composeRule.onNodeWithTag("calendar_day_$date2").performScrollTo().performClick()
        composeRule.onNodeWithText("Walang delivery record sa petsang ito").performScrollTo().assertIsDisplayed()

        // Tap Previous Month arrow -> updates displayed month title
        val prevCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val expectedPrevMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(prevCal.time)
        composeRule.onNodeWithTag("calendar_prev_month").performScrollTo().performClick()
        composeRule.onNodeWithText(expectedPrevMonth).performScrollTo().assertIsDisplayed()
    }
}
