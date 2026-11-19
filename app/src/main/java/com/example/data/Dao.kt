package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries ORDER BY timestamp DESC")
    fun getAllDeliveries(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :id")
    suspend fun getDeliveryById(id: Long): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getDeliveriesForCustomer(customerId: Long): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE dateString = :date ORDER BY timestamp DESC")
    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE isRemitted = 0 ORDER BY timestamp DESC")
    fun getUnremittedDeliveries(): Flow<List<DeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity): Long

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    @Update
    suspend fun updateCustomerInTx(customer: CustomerEntity)

    @Delete
    suspend fun deleteDelivery(delivery: DeliveryEntity)

    @Transaction
    suspend fun insertDeliveryAndUpdateCustomer(
        delivery: DeliveryEntity,
        updatedCustomer: CustomerEntity
    ): Long {
        val id = insertDelivery(delivery)
        updateCustomerInTx(updatedCustomer)
        return id
    }

    @Transaction
    suspend fun updateDeliveryAndCustomer(
        delivery: DeliveryEntity,
        updatedCustomer: CustomerEntity
    ) {
        updateDelivery(delivery)
        updateCustomerInTx(updatedCustomer)
    }

    @Transaction
    suspend fun deleteDeliveryAndUpdateCustomer(
        delivery: DeliveryEntity,
        restoredCustomer: CustomerEntity
    ) {
        updateCustomerInTx(restoredCustomer)
        deleteDelivery(delivery)
    }

    @Query("UPDATE deliveries SET isRemitted = 1 WHERE isRemitted = 0")
    suspend fun markAllDeliveriesRemitted()

    @Query("UPDATE deliveries SET isRemitted = 1, remittanceId = :remittanceId WHERE isRemitted = 0")
    suspend fun markAllDeliveriesRemittedWithId(remittanceId: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isRemitted = 0 ORDER BY timestamp DESC")
    fun getUnremittedExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("UPDATE expenses SET isRemitted = 1 WHERE isRemitted = 0")
    suspend fun markAllExpensesRemitted()

    @Query("UPDATE expenses SET isRemitted = 1, remittanceId = :remittanceId WHERE isRemitted = 0")
    suspend fun markAllExpensesRemittedWithId(remittanceId: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE isRemitted = 0 ORDER BY timestamp DESC")
    fun getUnremittedPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updateCustomerInTx(customer: CustomerEntity)

    @Transaction
    suspend fun insertPaymentAndUpdateCustomer(
        payment: PaymentEntity,
        updatedCustomer: CustomerEntity
    ): Long {
        val id = insertPayment(payment)
        updateCustomerInTx(updatedCustomer)
        return id
    }

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET isRemitted = 1 WHERE isRemitted = 0")
    suspend fun markAllPaymentsRemitted()

    @Query("UPDATE payments SET isRemitted = 1, remittanceId = :remittanceId WHERE isRemitted = 0")
    suspend fun markAllPaymentsRemittedWithId(remittanceId: Long)
}

@Dao
interface AdvanceRemitDao {
    @Query("SELECT * FROM advance_remits ORDER BY timestamp ASC")
    fun getAllAdvanceRemits(): Flow<List<AdvanceRemitEntity>>

    @Query("SELECT * FROM advance_remits WHERE isRemitted = 0 ORDER BY timestamp ASC")
    fun getUnremittedAdvanceRemits(): Flow<List<AdvanceRemitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvanceRemit(advanceRemit: AdvanceRemitEntity): Long

    @Update
    suspend fun updateAdvanceRemit(advanceRemit: AdvanceRemitEntity)

    @Delete
    suspend fun deleteAdvanceRemit(advanceRemit: AdvanceRemitEntity)

    @Query("UPDATE advance_remits SET isRemitted = 1, remittanceId = :remittanceId WHERE isRemitted = 0")
    suspend fun markAllAdvanceRemitsRemittedWithId(remittanceId: Long)
}

@Dao
interface RemittanceDao {
    @Query("SELECT * FROM remittances ORDER BY timestamp DESC")
    fun getAllRemittances(): Flow<List<RemittanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemittance(remittance: RemittanceLogEntity): Long
}

@Dao
interface ValeDao {
    @Query("SELECT * FROM vales ORDER BY timestamp DESC")
    fun getAllVales(): Flow<List<ValeLogEntity>>

    @Query("SELECT * FROM vales WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getActiveVales(): Flow<List<ValeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVale(vale: ValeLogEntity): Long

    @Delete
    suspend fun deleteVale(vale: ValeLogEntity)

    @Query("UPDATE vales SET isArchived = 1 WHERE isArchived = 0")
    suspend fun archiveAllVales()
}

@Dao
interface WeeklyPayoutDao {
    @Query("SELECT * FROM weekly_payouts ORDER BY timestamp DESC")
    fun getAllWeeklyPayouts(): Flow<List<WeeklyPayoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyPayout(payout: WeeklyPayoutEntity): Long
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)
}
