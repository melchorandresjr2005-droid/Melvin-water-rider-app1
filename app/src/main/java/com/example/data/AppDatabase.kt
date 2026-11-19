package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(
    entities = [
        CustomerEntity::class,
        DeliveryEntity::class,
        ExpenseEntity::class,
        PaymentEntity::class,
        AdvanceRemitEntity::class,
        RemittanceLogEntity::class,
        ValeLogEntity::class,
        WeeklyPayoutEntity::class,
        SettingsEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun advanceRemitDao(): AdvanceRemitDao
    abstract fun remittanceDao(): RemittanceDao
    abstract fun valeDao(): ValeDao
    abstract fun weeklyPayoutDao(): WeeklyPayoutDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_name` ON `customers` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_currentBalance` ON `customers` (`currentBalance`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deliveries_customerId` ON `deliveries` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deliveries_timestamp` ON `deliveries` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deliveries_dateString` ON `deliveries` (`dateString`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deliveries_isRemitted` ON `deliveries` (`isRemitted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_isRemitted` ON `expenses` (`isRemitted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_timestamp` ON `expenses` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_customerId` ON `payments` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_isRemitted` ON `payments` (`isRemitted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_timestamp` ON `payments` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_advance_remits_isRemitted` ON `advance_remits` (`isRemitted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_advance_remits_timestamp` ON `advance_remits` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vales_isArchived` ON `vales` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vales_timestamp` ON `vales` (`timestamp`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "melvin_water_rider_db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_10_11)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
