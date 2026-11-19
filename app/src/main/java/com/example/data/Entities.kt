package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["currentBalance"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val customerType: String, // "Residential", "Outlet", "Bank / Direct"
    val isBankDirectAccount: Boolean = false,
    val defaultPurifiedPrice: Double = 35.0,
    val defaultAlkalinePrice: Double = 45.0,
    val currentBalance: Double = 0.0, // Utang
    val storedCredit: Double = 0.0, // Sukli / Barya credit
    val pendingCommissionGallons: Double = 0.0,
    val lastDeliveredDate: String? = null
)

@Entity(
    tableName = "deliveries",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"]),
        Index(value = ["dateString"]),
        Index(value = ["isRemitted"])
    ]
)
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val customerAddress: String,
    val purifiedQty: Int = 0,
    val alkalineQty: Int = 0,
    val freePurifiedQty: Int = 0,
    val freeAlkalineQty: Int = 0,
    val purifiedPrice: Double = 35.0,
    val alkalinePrice: Double = 45.0,
    val customDiscount: Double = 0.0,
    val isFreeOrDamaged: Boolean = false,
    val subtotal: Double = 0.0,
    val utangApplied: Double = 0.0,
    val creditApplied: Double = 0.0,
    val totalDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentStatus: String = "Paid", // "Paid", "Utang", "May Sukli", "Partial", "Paid at Station", "Unpaid / Collect on Delivery", "Direct to Boss"
    val orderSource: String = "Route Delivery", // "Route Delivery", "Station Walk-In"
    val isBankDirect: Boolean = false,
    val commissionGallons: Double = 0.0,
    val pendingCommissionAdded: Double = 0.0,
    val utangCommissionGallons: Double = 0.0,
    val baryaGenerated: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val isRemitted: Boolean = false,
    val remittanceId: Long = 0L
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["isRemitted"]),
        Index(value = ["timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Gasolina", "Vulcanizing", "Other"
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val isRemitted: Boolean = false,
    val remittanceId: Long = 0L
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["isRemitted"]),
        Index(value = ["timestamp"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val paymentType: String, // "Utang Payment", "Utang Payment + Sukli", "Cash Sale", "Advance Payment"
    val previousBalance: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val sukliAdded: Double = 0.0,
    val commissionGallonsEarned: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val isRemitted: Boolean = false,
    val remittanceId: Long = 0L
)

@Entity(
    tableName = "advance_remits",
    indices = [
        Index(value = ["isRemitted"]),
        Index(value = ["timestamp"])
    ]
)
data class AdvanceRemitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val isRemitted: Boolean = false,
    val remittanceId: Long = 0L
)

@Entity(tableName = "remittances")
data class RemittanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val purifiedGallons: Int = 0,
    val alkalineGallons: Int = 0,
    val paidPurifiedGallons: Int = 0,
    val freePurifiedGallons: Int = 0,
    val paidAlkalineGallons: Int = 0,
    val freeAlkalineGallons: Int = 0,
    val cashPaidPurifiedGallons: Int = 0,
    val cashPaidAlkalineGallons: Int = 0,
    val cashDeliverySales: Double = 0.0,
    val bankDirectGallons: Int = 0,
    val bankDirectAmount: Double = 0.0,
    val totalCommissionGallons: Double = 0.0,
    val totalCashCollected: Double,
    val totalExpenses: Double,
    val totalAdvanceRemits: Double = 0.0,
    val netRemit: Double,
    val totalDayRemitted: Double = 0.0,
    val notes: String = "",
    val expenseDetails: String = "",
    val utangPaymentDetails: String = "",
    val bankDirectDetails: String = "",
    val newUtangDetails: String = "",
    val advanceRemitDetails: String = "",
    val totalNewUtang: Double = 0.0,
    val totalUtangCollected: Double = 0.0
)

@Entity(
    tableName = "vales",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["timestamp"])
    ]
)
data class ValeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val isArchived: Boolean = false
)

@Entity(tableName = "weekly_payouts")
data class WeeklyPayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekLabel: String,
    val paidGallons: Int,
    val freeGallons: Int,
    val commissionRate: Double,
    val grossCommission: Double,
    val totalVale: Double,
    val netTakeHomePay: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = ""
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val commissionPerGallon: Double = 5.0,
    val defaultPurifiedPrice: Double = 35.0,
    val defaultAlkalinePrice: Double = 45.0,
    val purifiedPreset1: Double = 25.0,
    val purifiedPreset2: Double = 35.0,
    val alkalinePreset1: Double = 30.0,
    val alkalinePreset2: Double = 45.0,
    val lastBackupTimestamp: Long = 0L
)
