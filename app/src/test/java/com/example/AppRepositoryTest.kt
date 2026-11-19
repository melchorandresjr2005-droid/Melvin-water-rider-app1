package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdvanceRemitEntity
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CustomerEntity
import androidx.compose.ui.graphics.Color
import com.example.data.ExpenseEntity
import com.example.data.ThemePreferences
import com.example.ui.customers.computeTotalRegularUtang
import com.example.ui.customers.filterAndSortCustomers
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.AppThemeId
import com.example.ui.theme.materialColorSchemeForTheme
import com.example.ui.theme.paletteForTheme
import com.example.ui.today.buildRemitReport
import com.example.util.MessengerUtils
import com.example.viewmodel.CustomerFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun partialUtangPayment_updatesRemainingBalanceAndLogsPayment() = runBlocking {
        val customerId = repository.insertCustomer(
            CustomerEntity(
                name = "Nena",
                address = "Purok 1",
                customerType = "Residential",
                currentBalance = 150.0,
                storedCredit = 0.0
            )
        )

        repository.recordUtangPayment(customerId, 100.0, "Pauna")

        val updated = repository.allCustomers.first().first { it.id == customerId }
        assertEquals(50.0, updated.currentBalance, 0.001)
        assertEquals(0.0, updated.storedCredit, 0.001)

        val payment = repository.allPayments.first().first { it.customerId == customerId }
        assertEquals(100.0, payment.amount, 0.001)
        assertEquals(150.0, payment.previousBalance, 0.001)
        assertEquals(50.0, payment.remainingBalance, 0.001)
        assertEquals(0.0, payment.sukliAdded, 0.001)
    }

    @Test
    fun overpaymentUtang_clearsDebtAndStoresExtraAsSukli() = runBlocking {
        val customerId = repository.insertCustomer(
            CustomerEntity(
                name = "Mario",
                address = "Purok 2",
                customerType = "Residential",
                currentBalance = 150.0,
                storedCredit = 0.0
            )
        )

        repository.recordUtangPayment(customerId, 200.0, "Sobrang bayad")

        val updated = repository.allCustomers.first().first { it.id == customerId }
        assertEquals(0.0, updated.currentBalance, 0.001)
        assertEquals(50.0, updated.storedCredit, 0.001)

        val payment = repository.allPayments.first().first { it.customerId == customerId }
        assertEquals(200.0, payment.amount, 0.001)
        assertEquals(150.0, payment.previousBalance, 0.001)
        assertEquals(0.0, payment.remainingBalance, 0.001)
        assertEquals(50.0, payment.sukliAdded, 0.001)
    }

    @Test
    fun regularUtang_defersCommissionUntilBayadUtang() = runBlocking {
        val customerId = repository.insertCustomer(
            CustomerEntity(
                name = "Aling Nena",
                address = "Purok 3",
                customerType = "Residential"
            )
        )

        // 3 Purified at 35 = 105 Utang -> 0 immediate commission, 3.0 pending gallons
        repository.recordDelivery(
            customerId = customerId,
            purifiedQty = 3,
            alkalineQty = 0,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 0.0,
            paymentStatus = "Utang",
            orderSource = "Route Delivery"
        )

        val delivery = repository.allDeliveries.first().first { it.customerId == customerId }
        assertEquals(0.0, delivery.commissionGallons, 0.001)
        assertEquals(3.0, delivery.pendingCommissionAdded, 0.001)

        val afterUtang = repository.allCustomers.first().first { it.id == customerId }
        assertEquals(105.0, afterUtang.currentBalance, 0.001)
        assertEquals(3.0, afterUtang.pendingCommissionGallons, 0.001)

        // Partial payment of 70 -> 0 commission gallons unlocked while balance remains (₱35.00 left)
        repository.recordUtangPayment(customerId, 70.0, "Hulog")
        val afterPartial = repository.allCustomers.first().first { it.id == customerId }
        assertEquals(35.0, afterPartial.currentBalance, 0.001)
        assertEquals(3.0, afterPartial.pendingCommissionGallons, 0.001)

        val firstPayment = repository.allPayments.first().first { it.notes == "Hulog" }
        assertEquals(0.0, firstPayment.commissionGallonsEarned, 0.001)

        // Final payment of 35 -> balance hits ₱0.00 -> unlocks all 3.0 commission gallons!
        repository.recordUtangPayment(customerId, 35.0, "Full")
        val afterFull = repository.allCustomers.first().first { it.id == customerId }
        assertEquals(0.0, afterFull.currentBalance, 0.001)
        assertEquals(0.0, afterFull.pendingCommissionGallons, 0.001)

        val secondPayment = repository.allPayments.first().first { it.notes == "Full" }
        assertEquals(3.0, secondPayment.commissionGallonsEarned, 0.001)
    }

    @Test
    fun bankDirectAccount_earnsImmediateCommissionEvenWhenLoggedAsUtang() = runBlocking {
        val bankId = repository.insertCustomer(
            CustomerEntity(
                name = "BDO Branch",
                address = "Main Ave",
                customerType = "Bank / Direct",
                isBankDirectAccount = true
            )
        )

        repository.recordDelivery(
            customerId = bankId,
            purifiedQty = 0,
            alkalineQty = 5,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 0.0,
            paymentStatus = "Utang",
            orderSource = "Route Delivery"
        )

        val delivery = repository.allDeliveries.first().first { it.customerId == bankId }
        assertTrue(delivery.isBankDirect)
        assertEquals("Direct to Boss", delivery.paymentStatus)
        assertEquals(0.0, delivery.amountPaid, 0.001)
        assertEquals(5.0, delivery.commissionGallons, 0.001)
        assertEquals(0.0, delivery.pendingCommissionAdded, 0.001)

        val bankCustomer = repository.allCustomers.first().first { it.id == bankId }
        assertEquals(0.0, bankCustomer.currentBalance, 0.001)
        assertEquals(0.0, bankCustomer.pendingCommissionGallons, 0.001)

        val log = repository.closeAfternoonShift("")
        assertNotNull(log)
        assertEquals(0.0, log!!.totalCashCollected, 0.001)
        assertEquals(0.0, log.netRemit, 0.001)
    }

    @Test
    fun multipleAdvanceRemits_deductFromAfternoonNetAndShowChronologicallyInReport() = runBlocking {
        val customerId = repository.insertCustomer(
            CustomerEntity(name = "Ana", address = "Highway", customerType = "Residential")
        )
        // 30 Purified * 35 = 1050 cash
        repository.recordDelivery(
            customerId = customerId,
            purifiedQty = 30,
            alkalineQty = 0,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 1050.0,
            paymentStatus = "Paid",
            orderSource = "Route Delivery"
        )
        repository.insertAdvanceRemit(
            AdvanceRemitEntity(
                amount = 400.0,
                notes = "Pangtanghali 1",
                timestamp = 1_700_000_000_000L,
                dateString = "2026-11-18"
            )
        )
        repository.insertAdvanceRemit(
            AdvanceRemitEntity(
                amount = 250.0,
                notes = "Pangtanghali 2",
                timestamp = 1_700_003_600_000L,
                dateString = "2026-11-18"
            )
        )

        val log = repository.closeAfternoonShift("")
        assertNotNull(log)
        assertEquals(1050.0, log!!.totalCashCollected, 0.001)
        assertEquals(650.0, log.totalAdvanceRemits, 0.001)
        assertEquals(400.0, log.netRemit, 0.001)
        assertEquals(1050.0, log.totalDayRemitted, 0.001)

        val report = buildRemitReport(log)
        assertTrue(report.contains("GALON NA MAY BAYAD (Ngayong Araw): 30 gal (30 Purified | 0 Alkaline)"))
        assertTrue(report.contains("Benta sa Cash Delivered: ₱1,050.00"))
        assertTrue(report.contains("ADVANCE REMIT:"))
        assertTrue(report.contains("• Pangtanghali 1 ("))
        assertTrue(report.contains("): -₱400.00"))
        assertTrue(report.contains("• Pangtanghali 2 ("))
        assertTrue(report.contains("): -₱250.00"))
        assertTrue(report.indexOf("Pangtanghali 1") < report.indexOf("Pangtanghali 2"))
        assertTrue(report.contains("TOTAL GALON (Para sa Commission): 30 gal"))
        assertTrue(report.contains("NATITIRANG CASH NA IRE-REMIT: ₱400.00"))
        assertTrue(report.contains("(Kabuuang Remit Buong Araw: ₱1,050.00)"))
        assertFalse(report.contains("BAYAD UTANG"))
        assertFalse(report.contains("GASTOS SA ROTA"))
    }

    @Test
    fun closeAfternoonShift_andTagalogRemitReport_excludesUnpaidUtangAndIncludesBayadUtangAndBankDirectGallons() = runBlocking {
        val cashCustomerId = repository.insertCustomer(
            CustomerEntity(
                name = "Ana",
                address = "Highway",
                customerType = "Residential"
            )
        )
        val bankId = repository.insertCustomer(
            CustomerEntity(
                name = "BDO Branch",
                address = "Town Center",
                customerType = "Bank / Direct",
                isBankDirectAccount = true
            )
        )
        val unpaidUtangCustomerId = repository.insertCustomer(
            CustomerEntity(
                name = "Pedro",
                address = "Purok 1",
                customerType = "Residential"
            )
        )
        val nenaId = repository.insertCustomer(
            CustomerEntity(
                name = "Nena",
                address = "Purok 2",
                customerType = "Residential",
                currentBalance = 70.0,
                pendingCommissionGallons = 2.0
            )
        )

        // 39 Purified (38 paid, 1 free) @ 32.5 + 20 Alkaline paid @ 45? Let's use exact example numbers:
        // 38 Purified paid @ 35 = 1,330; 20 Alkaline paid @ 40 = 800 -> 58 paid gal, ₱2,130.00 cash
        repository.recordDelivery(
            customerId = cashCustomerId,
            purifiedQty = 39,
            alkalineQty = 20,
            freePurifiedQty = 1,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 40.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 2130.0,
            paymentStatus = "Paid",
            orderSource = "Route Delivery"
        )
        // Bank direct: 5 Alkaline = 225.0 (0 cash handed to rider, still counts toward TOTAL GALON commission)
        repository.recordDelivery(
            customerId = bankId,
            purifiedQty = 0,
            alkalineQty = 5,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 0.0,
            paymentStatus = "Direct to Boss",
            orderSource = "Route Delivery"
        )
        // Unpaid regular Utang: Pedro 4 Purified = ₱0 commission today & EXCLUDED from remit report!
        repository.recordDelivery(
            customerId = unpaidUtangCustomerId,
            purifiedQty = 4,
            alkalineQty = 0,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 0.0,
            paymentStatus = "Utang",
            orderSource = "Route Delivery"
        )
        // Nena Bayad Utang: +₱70.00 (unlocks 2.0 gal)
        repository.recordUtangPayment(nenaId, 70.0, "")
        // Expense: Gasolina -₱30.00
        repository.insertExpense(
            ExpenseEntity(
                category = "Gasolina",
                amount = 30.0,
                notes = "",
                dateString = "2026-11-18"
            )
        )
        // Advance remit: Tanghali -₱1,000.00
        repository.insertAdvanceRemit(
            AdvanceRemitEntity(
                amount = 1000.0,
                notes = "Tanghali",
                timestamp = 1_700_000_000_000L,
                dateString = "2026-11-18"
            )
        )

        val log = repository.closeAfternoonShift("")
        assertNotNull(log)
        // Cash = 2130 + 70 = 2200; Expense = 30; Advance = 1000 -> Net = 1170; Day = 2170
        assertEquals(2200.0, log!!.totalCashCollected, 0.001)
        assertEquals(30.0, log.totalExpenses, 0.001)
        assertEquals(1000.0, log.totalAdvanceRemits, 0.001)
        assertEquals(1170.0, log.netRemit, 0.001)
        assertEquals(2170.0, log.totalDayRemitted, 0.001)
        // Paid cash (58) + Bank Direct (5) + Bayad Utang (2) = 65 gal (Pedro's 4 Utang & 1 free excluded!)
        assertEquals(65.0, log.totalCommissionGallons, 0.001)

        val report = buildRemitReport(log)
        assertTrue(report.contains("Melvin WaterRider - Remit Report"))
        assertTrue(report.contains("GALON NA MAY BAYAD (Ngayong Araw): 58 gal (38 Purified | 20 Alkaline)"))
        assertTrue(report.contains("Benta sa Cash Delivered: ₱2,130.00"))
        assertTrue(report.contains("Bank / Direct (Diretso kay Boss): 5 gal — ₱225.00"))
        assertTrue(report.contains("58 gal = Delivered (Paid)"))
        assertTrue(report.contains("+2 gal - Nena (Paid Pautang)"))
        assertTrue(report.contains("BAYAD UTANG (Dagdag sa Galon & Cash):"))
        assertTrue(report.contains("• Nena (2 gal - FULLY PAID): +₱70.00"))
        assertTrue(report.contains("GASTOS SA ROTA:"))
        assertTrue(report.contains("• Gasolina: -₱30.00"))
        assertTrue(report.contains("ADVANCE REMIT:"))
        assertTrue(report.contains("• Tanghali ("))
        assertTrue(report.contains(": -₱1,000.00"))
        assertTrue(report.contains("TOTAL GALON (Para sa Commission): 65 gal"))
        assertTrue(report.contains("NATITIRANG CASH NA IRE-REMIT: ₱1,170.00"))
        assertTrue(report.contains("(Kabuuang Remit Buong Araw: ₱2,170.00)"))
        // Ensure unpaid Utang (Pedro) is completely excluded
        assertFalse(report.contains("Pedro"))
        assertFalse(report.contains("BAGONG PAUTANG"))
    }

    @Test
    fun tagalogRemitReport_hidesEmptyOptionalSectionsWhenZero() = runBlocking {
        val cashCustomerId = repository.insertCustomer(
            CustomerEntity(
                name = "Ana",
                address = "Highway",
                customerType = "Residential"
            )
        )
        repository.recordDelivery(
            customerId = cashCustomerId,
            purifiedQty = 10,
            alkalineQty = 0,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 350.0,
            paymentStatus = "Paid",
            orderSource = "Route Delivery"
        )

        val log = repository.closeAfternoonShift("")
        assertNotNull(log)
        val report = buildRemitReport(log!!)
        assertTrue(report.contains("GALON NA MAY BAYAD (Ngayong Araw): 10 gal (10 Purified | 0 Alkaline)"))
        assertTrue(report.contains("Benta sa Cash Delivered: ₱350.00"))
        assertTrue(report.contains("TOTAL GALON (Para sa Commission): 10 gal"))
        assertTrue(report.contains("NATITIRANG CASH NA IRE-REMIT: ₱350.00"))
        assertTrue(report.contains("(Kabuuang Remit Buong Araw: ₱350.00)"))
        // Empty optional sections must be omitted cleanly
        assertFalse(report.contains("Bank / Direct"))
        assertFalse(report.contains("BAYAD UTANG"))
        assertFalse(report.contains("GASTOS SA ROTA"))
        assertFalse(report.contains("ADVANCE REMIT"))
        assertFalse(report.contains("Wala"))
    }

    @Test
    fun mayUtangSingilan_sortsHighestAndOldestFirstAndRemovesAtZero() = runBlocking {
        val nenaId = repository.insertCustomer(
            CustomerEntity(
                name = "Nena",
                address = "Purok 1",
                customerType = "Residential",
                currentBalance = 150.0,
                lastDeliveredDate = "2026-11-15"
            )
        )
        val marioId = repository.insertCustomer(
            CustomerEntity(
                name = "Mario",
                address = "Purok 2",
                customerType = "Residential",
                currentBalance = 300.0,
                lastDeliveredDate = "2026-11-17"
            )
        )
        val olderTieId = repository.insertCustomer(
            CustomerEntity(
                name = "Aling Rosa",
                address = "Purok 3",
                customerType = "Residential",
                currentBalance = 150.0,
                lastDeliveredDate = "2026-11-10"
            )
        )
        // Bank/Direct account excluded from regular customer Singilan total
        repository.insertCustomer(
            CustomerEntity(
                name = "BDO Branch",
                address = "Town",
                customerType = "Bank / Direct",
                isBankDirectAccount = true,
                currentBalance = 500.0
            )
        )

        var customers = repository.allCustomers.first()
        assertEquals(600.0, computeTotalRegularUtang(customers), 0.001)
        var singilan = filterAndSortCustomers(customers, "", CustomerFilter.WITH_UTANG, "2026-11-18")
        assertEquals(listOf(marioId, olderTieId, nenaId), singilan.map { it.id })

        // Partial payment (hulog) on Nena: ₱100 paid -> ₱50 remaining -> stays in Singilan list
        repository.recordUtangPayment(nenaId, 100.0, "Pauna")
        customers = repository.allCustomers.first()
        assertEquals(500.0, computeTotalRegularUtang(customers), 0.001)
        singilan = filterAndSortCustomers(customers, "", CustomerFilter.WITH_UTANG, "2026-11-18")
        val updatedNena = singilan.first { it.id == nenaId }
        assertEquals(50.0, updatedNena.currentBalance, 0.001)
        assertEquals(listOf(marioId, olderTieId, nenaId), singilan.map { it.id })

        // Pay remaining ₱50 -> balance hits ₱0.00 -> automatically removed from Singilan list
        repository.recordUtangPayment(nenaId, 50.0, "Full")
        customers = repository.allCustomers.first()
        assertEquals(450.0, computeTotalRegularUtang(customers), 0.001)
        singilan = filterAndSortCustomers(customers, "", CustomerFilter.WITH_UTANG, "2026-11-18")
        assertFalse(singilan.any { it.id == nenaId })
        assertEquals(listOf(marioId, olderTieId), singilan.map { it.id })
    }

    @Test
    fun partialBayadUtangHulog_givesZeroGallonsUntilFullyPaidInRemitReport() = runBlocking {
        val nenaId = repository.insertCustomer(
            CustomerEntity(
                name = "Nena",
                address = "Purok 1",
                customerType = "Residential",
                currentBalance = 105.0,
                pendingCommissionGallons = 3.0
            )
        )

        // Shift 1: Nena pays ₱70.00 partial ("Hulog") -> ₱35.00 remains -> 0 commission gallons, cash counted
        repository.recordUtangPayment(nenaId, 70.0, "Hulog")
        val shift1 = repository.closeAfternoonShift("")
        assertNotNull(shift1)
        assertEquals(70.0, shift1!!.totalCashCollected, 0.001)
        assertEquals(70.0, shift1.netRemit, 0.001)
        assertEquals(0.0, shift1.totalCommissionGallons, 0.001)

        val report1 = buildRemitReport(shift1)
        assertTrue(report1.contains("• Nena (Hulog lang | Balanse: ₱35.00): +₱70.00"))
        assertTrue(report1.contains("TOTAL GALON (Para sa Commission): 0 gal"))
        assertTrue(report1.contains("NATITIRANG CASH NA IRE-REMIT: ₱70.00"))

        // Shift 2: Nena pays final ₱35.00 -> ₱0.00 balance -> unlocks all 3.0 gallons!
        repository.recordUtangPayment(nenaId, 35.0, "Full")
        val shift2 = repository.closeAfternoonShift("")
        assertNotNull(shift2)
        assertEquals(35.0, shift2!!.totalCashCollected, 0.001)
        assertEquals(35.0, shift2.netRemit, 0.001)
        assertEquals(3.0, shift2.totalCommissionGallons, 0.001)

        val report2 = buildRemitReport(shift2)
        assertTrue(report2.contains("• Nena (3 gal - FULLY PAID): +₱35.00"))
        assertTrue(report2.contains("TOTAL GALON (Para sa Commission): 3 gal"))
        assertTrue(report2.contains("NATITIRANG CASH NA IRE-REMIT: ₱35.00"))
    }

    @Test
    fun themeSwitcher_persistsAllFiveThemesAndProvidesDistinctPalettes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ThemePreferences(context)

        assertEquals(5, AppThemeId.entries.size)
        for (theme in AppThemeId.entries) {
            prefs.setSelectedTheme(theme)
            assertEquals(theme, prefs.selectedThemeFlow.first())
            val palette = paletteForTheme(theme)
            assertTrue(palette.primaryAccent != palette.background)
            assertTrue(palette.textPrimary != palette.surface)
        }
        // Reset default theme
        prefs.setSelectedTheme(AppThemeId.ROYAL_DAYLIGHT)
        assertEquals(AppThemeId.ROYAL_DAYLIGHT, prefs.selectedThemeFlow.first())
    }

    @Test
    fun autoBackup_writesLocalFailSafeJsonAndPersistsBackupFolderConfig() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ThemePreferences(context)

        prefs.setBackupFolder("content://com.google.android.apps.docs.storage/tree/1234", "WaterRider Backups")
        val savedConfig = prefs.backupConfigFlow.first()
        assertTrue(savedConfig.isConnected)
        assertEquals("WaterRider Backups", savedConfig.folderName)

        val customerId = repository.insertCustomer(
            CustomerEntity(
                name = "Mang Juan",
                address = "Purok 5",
                customerType = "Residential",
                currentBalance = 0.0
            )
        )
        repository.recordDelivery(
            customerId = customerId,
            purifiedQty = 3,
            alkalineQty = 0,
            freePurifiedQty = 0,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 0.0,
            utangPaidAmount = 0.0,
            cashGiven = 105.0,
            paymentStatus = "Paid",
            orderSource = "Walk-in"
        )

        val result = repository.performAutoBackup(context, null)
        assertTrue(result.localSaved)
        assertFalse(result.driveSaved)
        assertNotNull(result.localPath)

        val backupFile = File(result.localPath!!)
        assertTrue(backupFile.exists())
        assertEquals(AppRepository.AUTO_BACKUP_FILE_NAME, backupFile.name)

        val restoreDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val restoreRepo = AppRepository(restoreDb)
        val restoredOk = backupFile.inputStream().use { restoreRepo.importFromJson(it) }
        assertTrue(restoredOk)
        val restoredCustomers = restoreRepo.allCustomers.first()
        assertEquals(1, restoredCustomers.size)
        assertEquals("Mang Juan", restoredCustomers.first().name)
        restoreDb.close()

        prefs.clearBackupFolder()
        assertNull(prefs.backupConfigFlow.first().folderUri)
    }

    @Test
    fun exportDeliveriesToCsv_formatsHeadersAndEscapesSpecialCharacters() = runBlocking {
        val customerId = repository.insertCustomer(
            CustomerEntity(
                name = "Aling Nena, \"Store\"",
                address = "Purok 1, Blk 2",
                customerType = "Residential"
            )
        )
        repository.recordDelivery(
            customerId = customerId,
            purifiedQty = 4,
            alkalineQty = 2,
            freePurifiedQty = 1,
            freeAlkalineQty = 0,
            purifiedPrice = 35.0,
            alkalinePrice = 45.0,
            customDiscount = 10.0,
            utangPaidAmount = 0.0,
            cashGiven = 185.0,
            paymentStatus = "Paid",
            orderSource = "Route Delivery"
        )

        val out = java.io.ByteArrayOutputStream()
        val exported = repository.exportDeliveriesToCsv(out)
        assertTrue(exported)

        val csv = out.toString(Charsets.UTF_8.name())
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("ID,Date,Timestamp,Customer ID,Customer Name,Customer Address"))
        assertTrue(lines[1].contains("\"Aling Nena, \"\"Store\"\"\""))
        assertTrue(lines[1].contains("\"Purok 1, Blk 2\""))
        assertTrue(lines[1].contains(",Route Delivery,Paid,No,4,1,35.00,2,0,45.00,10.00,"))
    }

    @Test
    fun defaultPricesAndBossContact_persistViaDataStoreAndMessengerHelpersWork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ThemePreferences(context)

        prefs.setDefaultPrices(28.0, 38.0)
        val prices = prefs.defaultPriceConfigFlow.first()
        assertEquals(28.0, prices.defaultPurifiedPrice, 0.001)
        assertEquals(38.0, prices.defaultAlkalinePrice, 0.001)

        prefs.setBossContact("https://m.me/boss.station/", "09171234567")
        val boss = prefs.bossContactFlow.first()
        assertTrue(boss.hasMessenger)
        assertEquals("https://m.me/boss.station/", boss.messengerUsername)
        assertEquals("09171234567", boss.phoneNumber)

        val uri = MessengerUtils.buildBossMessengerUri(boss.messengerUsername)
        assertNotNull(uri)
        assertEquals("https://m.me/boss.station", uri.toString())

        val reminder = MessengerUtils.buildUtangMessengerReminder("Aling Nena", 150.0)
        assertTrue(reminder.contains("Magandang araw po Aling Nena!"))
        assertTrue(reminder.contains("Paalala lang po sana sa natitirang balanse ninyo sa tubig na ₱150.00"))
    }

    @Test
    fun roomIndices_andHighContrastFiveThemes_areConfiguredCorrectly() {
        val sqlDb = db.openHelper.writableDatabase

        val customerIndices = mutableSetOf<String>()
        sqlDb.query("PRAGMA index_list(`customers`)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                customerIndices.add(cursor.getString(nameIdx))
            }
        }
        assertTrue(customerIndices.contains("index_customers_name"))
        assertTrue(customerIndices.contains("index_customers_currentBalance"))

        val deliveryIndices = mutableSetOf<String>()
        sqlDb.query("PRAGMA index_list(`deliveries`)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                deliveryIndices.add(cursor.getString(nameIdx))
            }
        }
        assertTrue(deliveryIndices.contains("index_deliveries_customerId"))
        assertTrue(deliveryIndices.contains("index_deliveries_timestamp"))
        assertTrue(deliveryIndices.contains("index_deliveries_dateString"))
        assertTrue(deliveryIndices.contains("index_deliveries_isRemitted"))

        // Verify strict outdoor contrast (#090D16 on light, #FFFFFF on dark) and singleton ColorSchemes
        for (theme in AppThemeId.entries) {
            val palette: AppThemeColors = paletteForTheme(theme)
            assertTrue(materialColorSchemeForTheme(theme) === materialColorSchemeForTheme(theme))
            if (palette.isDark) {
                assertEquals(Color(0xFFFFFFFF), palette.textPrimary)
            } else {
                assertEquals(Color(0xFF090D16), palette.textPrimary)
            }
        }
    }
}
