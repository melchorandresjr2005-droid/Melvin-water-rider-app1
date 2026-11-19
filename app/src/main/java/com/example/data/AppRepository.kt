package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoBackupResult(
    val localSaved: Boolean,
    val driveSaved: Boolean,
    val localPath: String? = null
)

data class BackupData(
    val customers: List<CustomerEntity>,
    val deliveries: List<DeliveryEntity>,
    val expenses: List<ExpenseEntity>,
    val payments: List<PaymentEntity>,
    val advanceRemits: List<AdvanceRemitEntity> = emptyList(),
    val remittances: List<RemittanceLogEntity>,
    val vales: List<ValeLogEntity>,
    val weeklyPayouts: List<WeeklyPayoutEntity> = emptyList(),
    val settings: SettingsEntity?
)

class AppRepository(private val db: AppDatabase) {
    private val customerDao = db.customerDao()
    private val deliveryDao = db.deliveryDao()
    private val expenseDao = db.expenseDao()
    private val paymentDao = db.paymentDao()
    private val advanceRemitDao = db.advanceRemitDao()
    private val remittanceDao = db.remittanceDao()
    private val valeDao = db.valeDao()
    private val weeklyPayoutDao = db.weeklyPayoutDao()
    private val settingsDao = db.settingsDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allDeliveries: Flow<List<DeliveryEntity>> = deliveryDao.getAllDeliveries().distinctUntilChanged().flowOn(Dispatchers.IO)
    val unremittedDeliveries: Flow<List<DeliveryEntity>> = deliveryDao.getUnremittedDeliveries().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses().distinctUntilChanged().flowOn(Dispatchers.IO)
    val unremittedExpenses: Flow<List<ExpenseEntity>> = expenseDao.getUnremittedExpenses().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments().distinctUntilChanged().flowOn(Dispatchers.IO)
    val unremittedPayments: Flow<List<PaymentEntity>> = paymentDao.getUnremittedPayments().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allAdvanceRemits: Flow<List<AdvanceRemitEntity>> = advanceRemitDao.getAllAdvanceRemits().distinctUntilChanged().flowOn(Dispatchers.IO)
    val unremittedAdvanceRemits: Flow<List<AdvanceRemitEntity>> = advanceRemitDao.getUnremittedAdvanceRemits().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allRemittances: Flow<List<RemittanceLogEntity>> = remittanceDao.getAllRemittances().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allVales: Flow<List<ValeLogEntity>> = valeDao.getAllVales().distinctUntilChanged().flowOn(Dispatchers.IO)
    val activeVales: Flow<List<ValeLogEntity>> = valeDao.getActiveVales().distinctUntilChanged().flowOn(Dispatchers.IO)
    val allWeeklyPayouts: Flow<List<WeeklyPayoutEntity>> = weeklyPayoutDao.getAllWeeklyPayouts().distinctUntilChanged().flowOn(Dispatchers.IO)
    val settings: Flow<SettingsEntity?> = settingsDao.getSettings().distinctUntilChanged().flowOn(Dispatchers.IO)

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> =
        customerDao.searchCustomers(query).distinctUntilChanged().flowOn(Dispatchers.IO)

    fun getDeliveriesForCustomer(customerId: Long): Flow<List<DeliveryEntity>> =
        deliveryDao.getDeliveriesForCustomer(customerId).distinctUntilChanged().flowOn(Dispatchers.IO)

    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntity>> =
        deliveryDao.getDeliveriesForDate(date).distinctUntilChanged().flowOn(Dispatchers.IO)

    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsForCustomer(customerId).distinctUntilChanged().flowOn(Dispatchers.IO)

    suspend fun getCustomerById(id: Long): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    suspend fun getDeliveryById(id: Long): DeliveryEntity? = withContext(Dispatchers.IO) {
        deliveryDao.getDeliveryById(id)
    }

    suspend fun getSettingsSync(): SettingsEntity? = withContext(Dispatchers.IO) {
        settingsDao.getSettingsSync()
    }

    suspend fun deletePayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        paymentDao.deletePayment(payment)
    }

    suspend fun insertCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun insertExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertAdvanceRemit(advanceRemit: AdvanceRemitEntity): Long = withContext(Dispatchers.IO) {
        advanceRemitDao.insertAdvanceRemit(advanceRemit)
    }

    suspend fun updateAdvanceRemit(advanceRemit: AdvanceRemitEntity) = withContext(Dispatchers.IO) {
        advanceRemitDao.updateAdvanceRemit(advanceRemit)
    }

    suspend fun deleteAdvanceRemit(advanceRemit: AdvanceRemitEntity) = withContext(Dispatchers.IO) {
        advanceRemitDao.deleteAdvanceRemit(advanceRemit)
    }

    suspend fun insertVale(vale: ValeLogEntity) = withContext(Dispatchers.IO) {
        valeDao.insertVale(vale)
    }

    suspend fun deleteVale(vale: ValeLogEntity) = withContext(Dispatchers.IO) {
        valeDao.deleteVale(vale)
    }

    suspend fun archiveWeeklyPayout(
        weekLabel: String,
        paidGallons: Int,
        freeGallons: Int,
        commissionRate: Double,
        grossCommission: Double,
        totalVale: Double,
        netTakeHomePay: Double
    ) = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        db.withTransaction {
            weeklyPayoutDao.insertWeeklyPayout(
                WeeklyPayoutEntity(
                    weekLabel = weekLabel,
                    paidGallons = paidGallons,
                    freeGallons = freeGallons,
                    commissionRate = commissionRate,
                    grossCommission = grossCommission,
                    totalVale = totalVale,
                    netTakeHomePay = netTakeHomePay,
                    dateString = todayStr
                )
            )
            valeDao.archiveAllVales()
        }
    }

    suspend fun updateSettings(settings: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun recordUtangPayment(customerId: Long, amount: Double, notes: String) = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext
        db.withTransaction {
            val customer = customerDao.getCustomerById(customerId) ?: return@withTransaction

            val previousUtang = customer.currentBalance
            val appliedToDebt = minOf(amount, previousUtang)
            val rawNewBalance = maxOf(0.0, previousUtang - appliedToDebt)
            val isFullyPaid = rawNewBalance <= 0.005
            val newBalance = if (isFullyPaid) 0.0 else rawNewBalance
            val extraSukli = maxOf(0.0, amount - previousUtang)
            val newStoredCredit = customer.storedCredit + extraSukli

            // Partial payments ("hulog") earn 0 commission; full 100% settlement unlocks all pending gallons
            val commissionEarned = if (isFullyPaid) customer.pendingCommissionGallons else 0.0
            val newPendingCommission = if (isFullyPaid) 0.0 else customer.pendingCommissionGallons

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())

            val payment = PaymentEntity(
                customerId = customerId,
                customerName = customer.name,
                amount = amount,
                paymentType = if (extraSukli > 0.0) "Utang Payment + Sukli" else "Utang Payment",
                previousBalance = previousUtang,
                remainingBalance = newBalance,
                sukliAdded = extraSukli,
                commissionGallonsEarned = commissionEarned,
                notes = notes,
                dateString = todayStr,
                isRemitted = false
            )
            val updatedCustomer = customer.copy(
                currentBalance = newBalance,
                storedCredit = newStoredCredit,
                pendingCommissionGallons = newPendingCommission
            )
            paymentDao.insertPaymentAndUpdateCustomer(payment, updatedCustomer)
        }
    }

    suspend fun recordDelivery(
        customerId: Long,
        purifiedQty: Int,
        alkalineQty: Int,
        freePurifiedQty: Int,
        freeAlkalineQty: Int,
        purifiedPrice: Double,
        alkalinePrice: Double,
        customDiscount: Double,
        utangPaidAmount: Double,
        cashGiven: Double,
        paymentStatus: String,
        orderSource: String
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val customer = customerDao.getCustomerById(customerId) ?: return@withTransaction

        val clampedFreePurified = freePurifiedQty.coerceIn(0, purifiedQty)
        val clampedFreeAlkaline = freeAlkalineQty.coerceIn(0, alkalineQty)
        val paidPurified = maxOf(0, purifiedQty - clampedFreePurified)
        val paidAlkaline = maxOf(0, alkalineQty - clampedFreeAlkaline)
        val totalPaidGallons = paidPurified + paidAlkaline

        val rawSubtotal = (paidPurified * purifiedPrice) + (paidAlkaline * alkalinePrice) - customDiscount
        val subtotal = maxOf(0.0, rawSubtotal)

        val effectiveBankDirect = customer.isBankDirectAccount ||
            customer.customerType == "Bank / Direct" ||
            paymentStatus == "Direct to Boss"

        var effectiveSubtotal = subtotal
        var newStoredCredit = customer.storedCredit
        val oldUtangBeforeOrder = customer.currentBalance
        val oldPendingBeforeOrder = customer.pendingCommissionGallons
        var newUtangBalance = customer.currentBalance
        var newPendingCommission = customer.pendingCommissionGallons
        var creditApplied = 0.0

        if (!effectiveBankDirect && newStoredCredit > 0.0) {
            if (newStoredCredit >= effectiveSubtotal) {
                creditApplied = effectiveSubtotal
                newStoredCredit -= effectiveSubtotal
                effectiveSubtotal = 0.0
            } else {
                creditApplied = newStoredCredit
                effectiveSubtotal -= newStoredCredit
                newStoredCredit = 0.0
            }
        }

        val isPayingNow = !effectiveBankDirect && paymentStatus !in listOf("Utang", "Unpaid / Collect on Delivery")
        var actualUtangPaid = 0.0
        if (isPayingNow && utangPaidAmount > 0.0 && newUtangBalance > 0.0) {
            actualUtangPaid = minOf(utangPaidAmount, newUtangBalance)
            newUtangBalance -= actualUtangPaid
        }

        val totalDue = effectiveSubtotal + actualUtangPaid

        var amountPaid = 0.0
        var baryaGenerated = 0.0
        var finalPaymentStatus = paymentStatus
        var immediateCommissionGallons = 0.0
        var pendingCommissionAdded = 0.0
        var utangCommissionGallons = 0.0

        if (effectiveBankDirect) {
            amountPaid = 0.0
            finalPaymentStatus = "Direct to Boss"
            immediateCommissionGallons = totalPaidGallons.toDouble()
        } else if (paymentStatus == "Paid" || paymentStatus == "May Sukli" || paymentStatus == "Paid at Station") {
            if (cashGiven > totalDue) {
                baryaGenerated = cashGiven - totalDue
                newStoredCredit += baryaGenerated
                amountPaid = cashGiven
                if (paymentStatus == "Paid" && baryaGenerated > 0.0) {
                    finalPaymentStatus = "May Sukli"
                }
                immediateCommissionGallons = totalPaidGallons.toDouble()
            } else if (cashGiven < totalDue) {
                val unpaid = totalDue - cashGiven
                newUtangBalance += unpaid
                amountPaid = cashGiven
                finalPaymentStatus = "Partial"
                val cashTowardsOrder = minOf(effectiveSubtotal, amountPaid)
                val orderUnpaid = maxOf(0.0, effectiveSubtotal - cashTowardsOrder)
                val unpaidFraction = if (effectiveSubtotal > 0.0) (orderUnpaid / effectiveSubtotal).coerceIn(0.0, 1.0) else 0.0
                immediateCommissionGallons = totalPaidGallons * (1.0 - unpaidFraction)
                pendingCommissionAdded = totalPaidGallons * unpaidFraction
                newPendingCommission += pendingCommissionAdded
            } else {
                amountPaid = totalDue
                immediateCommissionGallons = totalPaidGallons.toDouble()
            }

            if (actualUtangPaid > 0.0 && oldUtangBeforeOrder > 0.0) {
                val cashTowardsOldUtang = minOf(actualUtangPaid, maxOf(0.0, amountPaid - effectiveSubtotal))
                val oldUtangRemaining = maxOf(0.0, oldUtangBeforeOrder - cashTowardsOldUtang)
                if (oldUtangRemaining <= 0.005) {
                    utangCommissionGallons = oldPendingBeforeOrder
                    newPendingCommission = maxOf(0.0, newPendingCommission - utangCommissionGallons)
                }
            }
        } else if (paymentStatus == "Utang" || paymentStatus == "Unpaid / Collect on Delivery") {
            newUtangBalance += totalDue
            amountPaid = 0.0
            finalPaymentStatus = "Utang"
            immediateCommissionGallons = 0.0
            pendingCommissionAdded = totalPaidGallons.toDouble()
            newPendingCommission += pendingCommissionAdded
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val delivery = DeliveryEntity(
            customerId = customerId,
            customerName = customer.name,
            customerAddress = customer.address,
            purifiedQty = purifiedQty,
            alkalineQty = alkalineQty,
            freePurifiedQty = clampedFreePurified,
            freeAlkalineQty = clampedFreeAlkaline,
            purifiedPrice = purifiedPrice,
            alkalinePrice = alkalinePrice,
            customDiscount = customDiscount,
            isFreeOrDamaged = (clampedFreePurified > 0 || clampedFreeAlkaline > 0),
            subtotal = subtotal,
            utangApplied = actualUtangPaid,
            creditApplied = creditApplied,
            totalDue = totalDue,
            amountPaid = amountPaid,
            paymentStatus = finalPaymentStatus,
            orderSource = orderSource,
            isBankDirect = effectiveBankDirect,
            commissionGallons = immediateCommissionGallons,
            pendingCommissionAdded = pendingCommissionAdded,
            utangCommissionGallons = utangCommissionGallons,
            baryaGenerated = baryaGenerated,
            dateString = todayStr,
            isRemitted = false
        )

        val updatedCustomer = customer.copy(
            currentBalance = maxOf(0.0, newUtangBalance),
            storedCredit = maxOf(0.0, newStoredCredit),
            pendingCommissionGallons = maxOf(0.0, newPendingCommission),
            lastDeliveredDate = todayStr
        )
        deliveryDao.insertDeliveryAndUpdateCustomer(delivery, updatedCustomer)
        }
    }

    private suspend fun reverseDeliveryImpactOnCustomer(
        delivery: DeliveryEntity
    ): CustomerEntity? = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(delivery.customerId) ?: return@withContext null
        var bal = customer.currentBalance
        var cred = customer.storedCredit
        var pendingComm = customer.pendingCommissionGallons

        // Restore stored credit used in this order
        cred += delivery.creditApplied
        // Restore old utang paid in this order
        bal += delivery.utangApplied
        // Remove barya generated by this order
        if (delivery.baryaGenerated > 0.0) {
            cred = maxOf(0.0, cred - delivery.baryaGenerated)
        }
        // Remove any unpaid debt added by this order (never deduct for Direct to Boss)
        val wasDebtOrder = !delivery.isBankDirect && (
            delivery.paymentStatus == "Utang" ||
                delivery.paymentStatus == "Partial" ||
                delivery.paymentStatus == "Unpaid / Collect on Delivery"
            )
        val unpaidAdded = if (wasDebtOrder) maxOf(0.0, delivery.totalDue - delivery.amountPaid) else 0.0
        if (unpaidAdded > 0.0) {
            bal = maxOf(0.0, bal - unpaidAdded)
        }
        // Restore pending commission pool
        pendingComm = maxOf(0.0, pendingComm - delivery.pendingCommissionAdded + delivery.utangCommissionGallons)

        val restored = customer.copy(
            currentBalance = maxOf(0.0, bal),
            storedCredit = maxOf(0.0, cred),
            pendingCommissionGallons = pendingComm
        )
        customerDao.updateCustomer(restored)
        restored
    }

    suspend fun updateDelivery(
        deliveryId: Long,
        purifiedQty: Int,
        alkalineQty: Int,
        freePurifiedQty: Int,
        freeAlkalineQty: Int,
        purifiedPrice: Double,
        alkalinePrice: Double,
        customDiscount: Double,
        utangPaidAmount: Double,
        cashGiven: Double,
        paymentStatus: String,
        orderSource: String
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val oldDelivery = deliveryDao.getDeliveryById(deliveryId) ?: return@withTransaction
            val restoredCustomer = reverseDeliveryImpactOnCustomer(oldDelivery) ?: return@withTransaction

        val clampedFreePurified = freePurifiedQty.coerceIn(0, purifiedQty)
        val clampedFreeAlkaline = freeAlkalineQty.coerceIn(0, alkalineQty)
        val paidPurified = maxOf(0, purifiedQty - clampedFreePurified)
        val paidAlkaline = maxOf(0, alkalineQty - clampedFreeAlkaline)
        val totalPaidGallons = paidPurified + paidAlkaline

        val rawSubtotal = (paidPurified * purifiedPrice) + (paidAlkaline * alkalinePrice) - customDiscount
        val subtotal = maxOf(0.0, rawSubtotal)

        val effectiveBankDirect = restoredCustomer.isBankDirectAccount ||
            restoredCustomer.customerType == "Bank / Direct" ||
            paymentStatus == "Direct to Boss"

        var effectiveSubtotal = subtotal
        var newStoredCredit = restoredCustomer.storedCredit
        val oldUtangBeforeOrder = restoredCustomer.currentBalance
        val oldPendingBeforeOrder = restoredCustomer.pendingCommissionGallons
        var newUtangBalance = restoredCustomer.currentBalance
        var newPendingCommission = restoredCustomer.pendingCommissionGallons
        var creditApplied = 0.0

        if (!effectiveBankDirect && newStoredCredit > 0.0) {
            if (newStoredCredit >= effectiveSubtotal) {
                creditApplied = effectiveSubtotal
                newStoredCredit -= effectiveSubtotal
                effectiveSubtotal = 0.0
            } else {
                creditApplied = newStoredCredit
                effectiveSubtotal -= newStoredCredit
                newStoredCredit = 0.0
            }
        }

        val isPayingNow = !effectiveBankDirect && paymentStatus !in listOf("Utang", "Unpaid / Collect on Delivery")
        var actualUtangPaid = 0.0
        if (isPayingNow && utangPaidAmount > 0.0 && newUtangBalance > 0.0) {
            actualUtangPaid = minOf(utangPaidAmount, newUtangBalance)
            newUtangBalance -= actualUtangPaid
        }

        val totalDue = effectiveSubtotal + actualUtangPaid

        var amountPaid = 0.0
        var baryaGenerated = 0.0
        var finalPaymentStatus = paymentStatus
        var immediateCommissionGallons = 0.0
        var pendingCommissionAdded = 0.0
        var utangCommissionGallons = 0.0

        if (effectiveBankDirect) {
            amountPaid = 0.0
            finalPaymentStatus = "Direct to Boss"
            immediateCommissionGallons = totalPaidGallons.toDouble()
        } else if (paymentStatus == "Paid" || paymentStatus == "May Sukli" || paymentStatus == "Paid at Station") {
            if (cashGiven > totalDue) {
                baryaGenerated = cashGiven - totalDue
                newStoredCredit += baryaGenerated
                amountPaid = cashGiven
                if (paymentStatus == "Paid" && baryaGenerated > 0.0) {
                    finalPaymentStatus = "May Sukli"
                }
                immediateCommissionGallons = totalPaidGallons.toDouble()
            } else if (cashGiven < totalDue) {
                val unpaid = totalDue - cashGiven
                newUtangBalance += unpaid
                amountPaid = cashGiven
                finalPaymentStatus = "Partial"
                val cashTowardsOrder = minOf(effectiveSubtotal, amountPaid)
                val orderUnpaid = maxOf(0.0, effectiveSubtotal - cashTowardsOrder)
                val unpaidFraction = if (effectiveSubtotal > 0.0) (orderUnpaid / effectiveSubtotal).coerceIn(0.0, 1.0) else 0.0
                immediateCommissionGallons = totalPaidGallons * (1.0 - unpaidFraction)
                pendingCommissionAdded = totalPaidGallons * unpaidFraction
                newPendingCommission += pendingCommissionAdded
            } else {
                amountPaid = totalDue
                immediateCommissionGallons = totalPaidGallons.toDouble()
            }

            if (actualUtangPaid > 0.0 && oldUtangBeforeOrder > 0.0) {
                val cashTowardsOldUtang = minOf(actualUtangPaid, maxOf(0.0, amountPaid - effectiveSubtotal))
                val oldUtangRemaining = maxOf(0.0, oldUtangBeforeOrder - cashTowardsOldUtang)
                if (oldUtangRemaining <= 0.005) {
                    utangCommissionGallons = oldPendingBeforeOrder
                    newPendingCommission = maxOf(0.0, newPendingCommission - utangCommissionGallons)
                }
            }
        } else if (paymentStatus == "Utang" || paymentStatus == "Unpaid / Collect on Delivery") {
            newUtangBalance += totalDue
            amountPaid = 0.0
            finalPaymentStatus = "Utang"
            immediateCommissionGallons = 0.0
            pendingCommissionAdded = totalPaidGallons.toDouble()
            newPendingCommission += pendingCommissionAdded
        }

        val updatedDelivery = oldDelivery.copy(
            purifiedQty = purifiedQty,
            alkalineQty = alkalineQty,
            freePurifiedQty = clampedFreePurified,
            freeAlkalineQty = clampedFreeAlkaline,
            purifiedPrice = purifiedPrice,
            alkalinePrice = alkalinePrice,
            customDiscount = customDiscount,
            isFreeOrDamaged = (clampedFreePurified > 0 || clampedFreeAlkaline > 0),
            subtotal = subtotal,
            utangApplied = actualUtangPaid,
            creditApplied = creditApplied,
            totalDue = totalDue,
            amountPaid = amountPaid,
            paymentStatus = finalPaymentStatus,
            orderSource = orderSource,
            isBankDirect = effectiveBankDirect,
            commissionGallons = immediateCommissionGallons,
            pendingCommissionAdded = pendingCommissionAdded,
            utangCommissionGallons = utangCommissionGallons,
            baryaGenerated = baryaGenerated
        )

        deliveryDao.updateDeliveryAndCustomer(
            updatedDelivery,
            restoredCustomer.copy(
                currentBalance = maxOf(0.0, newUtangBalance),
                storedCredit = maxOf(0.0, newStoredCredit),
                pendingCommissionGallons = maxOf(0.0, newPendingCommission)
            )
        )
        }
    }

    suspend fun deleteDelivery(deliveryId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val delivery = deliveryDao.getDeliveryById(deliveryId) ?: return@withTransaction
            reverseDeliveryImpactOnCustomer(delivery)
            deliveryDao.deleteDelivery(delivery)
        }
    }

    suspend fun closeAfternoonShift(notes: String): RemittanceLogEntity? = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val unremittedDeliveriesList = deliveryDao.getUnremittedDeliveries().first()
        val unremittedPaymentsList = paymentDao.getUnremittedPayments().first()
        val unremittedExpList = expenseDao.getUnremittedExpenses().first()
        val unremittedAdvanceList = advanceRemitDao.getUnremittedAdvanceRemits().first().sortedBy { it.timestamp }

        val totalCashCollected = unremittedDeliveriesList.sumOf { it.amountPaid } + unremittedPaymentsList.sumOf { it.amount }
        val cashDeliverySales = unremittedDeliveriesList.sumOf { it.amountPaid }
        val totalExpenses = unremittedExpList.sumOf { it.amount }
        val totalAdvanceRemits = unremittedAdvanceList.sumOf { it.amount }
        val netRemit = maxOf(0.0, totalCashCollected - totalExpenses - totalAdvanceRemits)
        val totalDayRemitted = totalAdvanceRemits + netRemit

        if (totalCashCollected == 0.0 && totalExpenses == 0.0 && totalAdvanceRemits == 0.0 &&
            unremittedDeliveriesList.isEmpty() && unremittedPaymentsList.isEmpty() && unremittedAdvanceList.isEmpty()
        ) return@withContext null

        // Only count paid cash deliveries (exclude unpaid regular Utang & Bank/Direct from Cash Paid counts)
        val cashPaidDeliveries = unremittedDeliveriesList.filter {
            !it.isBankDirect &&
                it.paymentStatus != "Direct to Boss" &&
                it.paymentStatus != "Utang" &&
                it.paymentStatus != "Unpaid / Collect on Delivery"
        }

        val cashPaidPurifiedGallons = cashPaidDeliveries.sumOf { del ->
            val paidP = maxOf(0, del.purifiedQty - del.freePurifiedQty.coerceIn(0, del.purifiedQty))
            if (del.paymentStatus == "Partial") {
                val paidA = maxOf(0, del.alkalineQty - del.freeAlkalineQty.coerceIn(0, del.alkalineQty))
                val totalP = maxOf(1, paidP + paidA)
                kotlin.math.round(paidP * (del.commissionGallons / totalP.toDouble())).toInt()
            } else {
                paidP
            }
        }
        val cashPaidAlkalineGallons = cashPaidDeliveries.sumOf { del ->
            val paidA = maxOf(0, del.alkalineQty - del.freeAlkalineQty.coerceIn(0, del.alkalineQty))
            if (del.paymentStatus == "Partial") {
                val paidP = maxOf(0, del.purifiedQty - del.freePurifiedQty.coerceIn(0, del.purifiedQty))
                val totalP = maxOf(1, paidP + paidA)
                kotlin.math.round(paidA * (del.commissionGallons / totalP.toDouble())).toInt()
            } else {
                paidA
            }
        }

        val bankDirectDeliveries = unremittedDeliveriesList.filter { it.isBankDirect || it.paymentStatus == "Direct to Boss" }
        val bankDirectPurified = bankDirectDeliveries.sumOf { maxOf(0, it.purifiedQty - it.freePurifiedQty.coerceIn(0, it.purifiedQty)) }
        val bankDirectAlkaline = bankDirectDeliveries.sumOf { maxOf(0, it.alkalineQty - it.freeAlkalineQty.coerceIn(0, it.alkalineQty)) }
        val bankDirectGallons = bankDirectPurified + bankDirectAlkaline
        val bankDirectAmount = bankDirectDeliveries.sumOf { it.totalDue }
        val bankDirectDetails = bankDirectDeliveries.joinToString("\n") { del ->
            val paidP = maxOf(0, del.purifiedQty - del.freePurifiedQty.coerceIn(0, del.purifiedQty))
            val paidA = maxOf(0, del.alkalineQty - del.freeAlkalineQty.coerceIn(0, del.alkalineQty))
            val waterDesc = formatWaterSummary(paidP, paidA)
            String.format(
                Locale.US,
                "• %s (%s): ₱%,.2f",
                del.customerName,
                waterDesc,
                del.totalDue
            )
        }

        val purifiedGallons = cashPaidPurifiedGallons + bankDirectPurified
        val alkalineGallons = cashPaidAlkalineGallons + bankDirectAlkaline
        val freePurifiedGallons = unremittedDeliveriesList.sumOf { it.freePurifiedQty.coerceIn(0, it.purifiedQty) }
        val freeAlkalineGallons = unremittedDeliveriesList.sumOf { it.freeAlkalineQty.coerceIn(0, it.alkalineQty) }

        val totalCommissionGallons = unremittedDeliveriesList.sumOf { it.commissionGallons + it.utangCommissionGallons } +
            unremittedPaymentsList.sumOf { it.commissionGallonsEarned }

        val newUtangDeliveries = unremittedDeliveriesList.filter {
            !it.isBankDirect &&
                (it.paymentStatus == "Utang" || it.paymentStatus == "Partial" || it.paymentStatus == "Unpaid / Collect on Delivery") &&
                (it.totalDue - it.amountPaid) > 0.0
        }
        val totalNewUtang = newUtangDeliveries.sumOf { maxOf(0.0, it.totalDue - it.amountPaid) }
        val newUtangDetails = newUtangDeliveries.joinToString("\n") { del ->
            val unpaidAmt = maxOf(0.0, del.totalDue - del.amountPaid)
            val waterDesc = formatWaterSummary(del.purifiedQty, del.alkalineQty)
            String.format(
                Locale.US,
                "• %s (%s): -₱%,.2f",
                del.customerName,
                waterDesc,
                unpaidAmt
            )
        }

        val expenseDetails = unremittedExpList.joinToString("\n") { exp ->
            val label = if (exp.notes.isNotBlank()) "${exp.category} (${exp.notes.trim()})" else exp.category
            String.format(Locale.US, "• %s: -₱%,.2f", label, exp.amount)
        }

        val totalUtangCollected = unremittedPaymentsList.sumOf { it.amount }
        val utangPaymentDetails = unremittedPaymentsList.joinToString("\n") { pay ->
            val statusTag = if (pay.remainingBalance > 0.0) {
                String.format(Locale.US, "Hulog lang | Balanse: ₱%,.2f", pay.remainingBalance)
            } else if (pay.commissionGallonsEarned > 0.0) {
                "${formatGallonCount(pay.commissionGallonsEarned)} gal - FULLY PAID"
            } else {
                "FULLY PAID"
            }
            String.format(
                Locale.US,
                "• %s (%s): +₱%,.2f",
                pay.customerName,
                statusTag,
                pay.amount
            )
        }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val advanceRemitDetails = unremittedAdvanceList.joinToString("\n") { adv ->
            val timeStr = timeFormat.format(Date(adv.timestamp))
            val label = if (adv.notes.isNotBlank()) "${adv.notes.trim()} ($timeStr)" else timeStr
            String.format(Locale.US, "• %s: -₱%,.2f", label, adv.amount)
        }

        val remitLog = RemittanceLogEntity(
            dateString = todayStr,
            purifiedGallons = purifiedGallons,
            alkalineGallons = alkalineGallons,
            paidPurifiedGallons = cashPaidPurifiedGallons + bankDirectPurified,
            freePurifiedGallons = freePurifiedGallons,
            paidAlkalineGallons = cashPaidAlkalineGallons + bankDirectAlkaline,
            freeAlkalineGallons = freeAlkalineGallons,
            cashPaidPurifiedGallons = cashPaidPurifiedGallons,
            cashPaidAlkalineGallons = cashPaidAlkalineGallons,
            cashDeliverySales = cashDeliverySales,
            bankDirectGallons = bankDirectGallons,
            bankDirectAmount = bankDirectAmount,
            totalCommissionGallons = totalCommissionGallons,
            totalCashCollected = totalCashCollected,
            totalExpenses = totalExpenses,
            totalAdvanceRemits = totalAdvanceRemits,
            netRemit = netRemit,
            totalDayRemitted = totalDayRemitted,
            notes = notes,
            expenseDetails = expenseDetails,
            utangPaymentDetails = utangPaymentDetails,
            bankDirectDetails = bankDirectDetails,
            newUtangDetails = newUtangDetails,
            advanceRemitDetails = advanceRemitDetails,
            totalNewUtang = totalNewUtang,
            totalUtangCollected = totalUtangCollected
        )

        val insertedId = db.withTransaction {
            val id = remittanceDao.insertRemittance(remitLog)
            deliveryDao.markAllDeliveriesRemittedWithId(id)
            paymentDao.markAllPaymentsRemittedWithId(id)
            expenseDao.markAllExpensesRemittedWithId(id)
            advanceRemitDao.markAllAdvanceRemitsRemittedWithId(id)
            id
        }

        return@withContext remitLog.copy(id = insertedId)
    }

    private fun formatGallonCount(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded)
    }

    private fun formatWaterSummary(purifiedQty: Int, alkalineQty: Int): String {
        val parts = mutableListOf<String>()
        if (purifiedQty > 0) parts.add("$purifiedQty Purified")
        if (alkalineQty > 0) parts.add("$alkalineQty Alkaline")
        return parts.joinToString(", ").ifEmpty { "0 gal" }
    }

    suspend fun exportToJsonString(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val customers = customerDao.getAllCustomers().first()
            val deliveries = deliveryDao.getAllDeliveries().first()
            val expenses = expenseDao.getAllExpenses().first()
            val payments = paymentDao.getAllPayments().first()
            val advanceRemits = advanceRemitDao.getAllAdvanceRemits().first()
            val remittances = remittanceDao.getAllRemittances().first()
            val vales = valeDao.getAllVales().first()
            val weeklyPayouts = weeklyPayoutDao.getAllWeeklyPayouts().first()
            val settingsVal = settingsDao.getSettings().first()

            val backup = BackupData(
                customers = customers,
                deliveries = deliveries,
                expenses = expenses,
                payments = payments,
                advanceRemits = advanceRemits,
                remittances = remittances,
                vales = vales,
                weeklyPayouts = weeklyPayouts,
                settings = settingsVal
            )

            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(BackupData::class.java)
            adapter.toJson(backup)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportToJson(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = exportToJsonString() ?: return@withContext false
            outputStream.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performAutoBackup(
        context: Context,
        treeUriString: String?,
        backupFileName: String = AUTO_BACKUP_FILE_NAME
    ): AutoBackupResult = withContext(Dispatchers.IO) {
        val jsonString = exportToJsonString() ?: return@withContext AutoBackupResult(false, false, null)
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

        // 1. Always write fail-safe local backup copy in app Downloads / internal backups dir
        var localSaved = false
        var localPath: String? = null
        try {
            val localDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "backups")
            if (!localDir.exists()) {
                localDir.mkdirs()
            }
            val localFile = File(localDir, backupFileName)
            localFile.writeBytes(jsonBytes)
            localSaved = localFile.exists()
            localPath = localFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. If SAF / Google Drive folder URI is connected, write or overwrite the single backup file
        var driveSaved = false
        if (!treeUriString.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(treeUriString)
                val folderDoc = DocumentFile.fromTreeUri(context, treeUri)
                if (folderDoc != null && folderDoc.exists() && folderDoc.canWrite()) {
                    val existing = folderDoc.findFile(backupFileName)
                        ?: folderDoc.listFiles().firstOrNull { it.name == backupFileName }

                    if (existing != null && existing.exists()) {
                        val truncatedWriteOk = try {
                            context.contentResolver.openOutputStream(existing.uri, "wt")?.use { out ->
                                out.write(jsonBytes)
                                out.flush()
                            } != null
                        } catch (_: Exception) {
                            false
                        }
                        if (truncatedWriteOk) {
                            driveSaved = true
                        } else {
                            existing.delete()
                            val recreated = folderDoc.createFile("application/json", backupFileName)
                            if (recreated != null) {
                                driveSaved = context.contentResolver.openOutputStream(recreated.uri, "w")?.use { out ->
                                    out.write(jsonBytes)
                                    out.flush()
                                    true
                                } ?: false
                            }
                        }
                    } else {
                        val created = folderDoc.createFile("application/json", backupFileName)
                        if (created != null) {
                            driveSaved = context.contentResolver.openOutputStream(created.uri, "w")?.use { out ->
                                out.write(jsonBytes)
                                out.flush()
                                true
                            } ?: false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext AutoBackupResult(
            localSaved = localSaved,
            driveSaved = driveSaved,
            localPath = localPath
        )
    }

    suspend fun importFromJson(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonString) ?: return@withContext false

            db.withTransaction {
                for (c in backup.customers) customerDao.insertCustomer(c)
                for (d in backup.deliveries) deliveryDao.insertDelivery(d)
                for (e in backup.expenses) expenseDao.insertExpense(e)
                for (p in backup.payments) paymentDao.insertPayment(p)
                for (a in backup.advanceRemits) advanceRemitDao.insertAdvanceRemit(a)
                for (r in backup.remittances) remittanceDao.insertRemittance(r)
                for (v in backup.vales) valeDao.insertVale(v)
                for (w in backup.weeklyPayouts) weeklyPayoutDao.insertWeeklyPayout(w)
                if (backup.settings != null) settingsDao.insertOrUpdateSettings(backup.settings)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportDeliveriesToCsvString(): String = withContext(Dispatchers.IO) {
        val deliveries = deliveryDao.getAllDeliveries().first()
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val headers = listOf(
            "ID",
            "Date",
            "Timestamp",
            "Customer ID",
            "Customer Name",
            "Customer Address",
            "Order Source",
            "Payment Status",
            "Bank/Direct",
            "Purified Qty",
            "Free Purified Qty",
            "Purified Price",
            "Alkaline Qty",
            "Free Alkaline Qty",
            "Alkaline Price",
            "Custom Discount",
            "Subtotal",
            "Utang Applied",
            "Credit Applied",
            "Total Due",
            "Amount Paid",
            "Barya Generated",
            "Commission Gallons",
            "Pending Commission Gallons",
            "Utang Commission Gallons",
            "Is Remitted",
            "Remittance ID"
        )

        return@withContext buildString {
            appendLine(headers.joinToString(",") { escapeCsvField(it) })
            for (d in deliveries) {
                val row = listOf(
                    d.id.toString(),
                    d.dateString,
                    timestampFormat.format(Date(d.timestamp)),
                    d.customerId.toString(),
                    d.customerName,
                    d.customerAddress,
                    d.orderSource,
                    d.paymentStatus,
                    if (d.isBankDirect) "Yes" else "No",
                    d.purifiedQty.toString(),
                    d.freePurifiedQty.toString(),
                    String.format(Locale.US, "%.2f", d.purifiedPrice),
                    d.alkalineQty.toString(),
                    d.freeAlkalineQty.toString(),
                    String.format(Locale.US, "%.2f", d.alkalinePrice),
                    String.format(Locale.US, "%.2f", d.customDiscount),
                    String.format(Locale.US, "%.2f", d.subtotal),
                    String.format(Locale.US, "%.2f", d.utangApplied),
                    String.format(Locale.US, "%.2f", d.creditApplied),
                    String.format(Locale.US, "%.2f", d.totalDue),
                    String.format(Locale.US, "%.2f", d.amountPaid),
                    String.format(Locale.US, "%.2f", d.baryaGenerated),
                    String.format(Locale.US, "%.2f", d.commissionGallons),
                    String.format(Locale.US, "%.2f", d.pendingCommissionAdded),
                    String.format(Locale.US, "%.2f", d.utangCommissionGallons),
                    if (d.isRemitted) "Yes" else "No",
                    d.remittanceId.toString()
                )
                appendLine(row.joinToString(",") { escapeCsvField(it) })
            }
        }
    }

    suspend fun exportDeliveriesToCsv(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val csvContent = exportDeliveriesToCsvString()
            outputStream.use { stream ->
                stream.write(csvContent.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeCsvField(value: String): String {
        val sanitized = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        return if (sanitized.contains(',') || sanitized.contains('"')) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }

    companion object {
        const val AUTO_BACKUP_FILE_NAME = "MelvinWaterRider_Backup.json"
    }
}
