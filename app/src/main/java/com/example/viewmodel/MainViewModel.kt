package com.example.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.File
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdvanceRemitEntity
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AutoBackupResult
import com.example.data.BackupFolderConfig
import com.example.data.BossContactConfig
import com.example.data.CustomerEntity
import com.example.data.DefaultPriceConfig
import com.example.data.DeliveryEntity
import com.example.data.ExpenseEntity
import com.example.data.PaymentEntity
import com.example.data.RemittanceLogEntity
import com.example.data.SettingsEntity
import com.example.data.ThemePreferences
import com.example.data.ValeLogEntity
import com.example.data.WeeklyPayoutEntity
import com.example.ui.theme.AppThemeId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

enum class CustomerFilter {
    ALL, WITH_UTANG, WITH_SUKLI, NOT_DELIVERED_TODAY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database)
    private val themePreferences = ThemePreferences(application)

    private val _selectedTheme = MutableStateFlow(AppThemeId.ROYAL_DAYLIGHT)
    val selectedTheme: StateFlow<AppThemeId> = _selectedTheme.asStateFlow()

    private val _backupConfig = MutableStateFlow(BackupFolderConfig())
    val backupConfig: StateFlow<BackupFolderConfig> = _backupConfig.asStateFlow()

    private val _defaultPriceConfig = MutableStateFlow(DefaultPriceConfig())
    val defaultPriceConfig: StateFlow<DefaultPriceConfig> = _defaultPriceConfig.asStateFlow()

    private val _bossContactConfig = MutableStateFlow(BossContactConfig())
    val bossContactConfig: StateFlow<BossContactConfig> = _bossContactConfig.asStateFlow()

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveries: StateFlow<List<DeliveryEntity>> = repository.allDeliveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unremittedDeliveries: StateFlow<List<DeliveryEntity>> = repository.unremittedDeliveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unremittedExpenses: StateFlow<List<ExpenseEntity>> = repository.unremittedExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unremittedPayments: StateFlow<List<PaymentEntity>> = repository.unremittedPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val advanceRemits: StateFlow<List<AdvanceRemitEntity>> = repository.allAdvanceRemits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unremittedAdvanceRemits: StateFlow<List<AdvanceRemitEntity>> = repository.unremittedAdvanceRemits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remittances: StateFlow<List<RemittanceLogEntity>> = repository.allRemittances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vales: StateFlow<List<ValeLogEntity>> = repository.allVales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVales: StateFlow<List<ValeLogEntity>> = repository.activeVales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyPayouts: StateFlow<List<WeeklyPayoutEntity>> = repository.allWeeklyPayouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _customerFilter = MutableStateFlow(CustomerFilter.ALL)
    val customerFilter: StateFlow<CustomerFilter> = _customerFilter.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _showAddDeliveryDialog = MutableStateFlow(false)
    val showAddDeliveryDialog: StateFlow<Boolean> = _showAddDeliveryDialog.asStateFlow()

    private val _backupReminderDismissed = MutableStateFlow(false)
    val backupReminderDismissed: StateFlow<Boolean> = _backupReminderDismissed.asStateFlow()

    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus.asStateFlow()

    private val _updateDownloadUrl = MutableStateFlow<String?>(null)
    val updateDownloadUrl: StateFlow<String?> = _updateDownloadUrl.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (repository.getSettingsSync() == null) {
                    repository.updateSettings(
                        SettingsEntity(
                            id = 1,
                            commissionPerGallon = 5.0,
                            defaultPurifiedPrice = 35.0,
                            defaultAlkalinePrice = 45.0,
                            purifiedPreset1 = 25.0,
                            purifiedPreset2 = 35.0,
                            alkalinePreset1 = 30.0,
                            alkalinePreset2 = 45.0
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            themePreferences.selectedThemeFlow.collect { savedTheme ->
                _selectedTheme.value = savedTheme
            }
        }
        viewModelScope.launch {
            themePreferences.backupConfigFlow.collect { config ->
                _backupConfig.value = config
            }
        }
        viewModelScope.launch {
            themePreferences.defaultPriceConfigFlow.collect { priceConfig ->
                _defaultPriceConfig.value = priceConfig
            }
        }
        viewModelScope.launch {
            themePreferences.bossContactFlow.collect { contact ->
                _bossContactConfig.value = contact
            }
        }
    }

    fun updateDefaultPriceList(purifiedPrice: Double, alkalinePrice: Double) {
        val updated = DefaultPriceConfig(purifiedPrice, alkalinePrice)
        _defaultPriceConfig.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            themePreferences.setDefaultPrices(purifiedPrice, alkalinePrice)
            val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
            repository.updateSettings(
                current.copy(
                    defaultPurifiedPrice = purifiedPrice,
                    defaultAlkalinePrice = alkalinePrice
                )
            )
        }
    }

    fun updateBossContact(messengerUsername: String, phoneNumber: String) {
        val updated = BossContactConfig(messengerUsername.trim(), phoneNumber.trim())
        _bossContactConfig.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            themePreferences.setBossContact(updated.messengerUsername, updated.phoneNumber)
        }
    }

    fun selectTheme(theme: AppThemeId) {
        _selectedTheme.value = theme
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                themePreferences.setSelectedTheme(theme)
            }
        }
    }

    fun saveBackupFolderSelection(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedName = try {
                DocumentFile.fromTreeUri(appContext, uri)?.name?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            } ?: uri.lastPathSegment
                ?.substringAfterLast(':')
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: "Google Drive Folder"

            themePreferences.setBackupFolder(uri.toString(), resolvedName)
        }
    }

    fun clearBackupFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            themePreferences.clearBackupFolder()
        }
    }

    fun triggerAutoBackup(context: Context, onComplete: ((AutoBackupResult) -> Unit)? = null) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val folderUri = _backupConfig.value.folderUri
                ?: themePreferences.backupConfigFlow.first().folderUri
            val result = repository.performAutoBackup(appContext, folderUri)
            if (result.localSaved || result.driveSaved) {
                val now = System.currentTimeMillis()
                themePreferences.setLastAutoBackupTimestamp(now)
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(current.copy(lastBackupTimestamp = now))
                _backupReminderDismissed.value = true
            }
            if (onComplete != null) {
                withContext(Dispatchers.Main) {
                    onComplete(result)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCustomerFilter(filter: CustomerFilter) {
        _customerFilter.value = filter
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun openAddDeliveryWizard() {
        _showAddDeliveryDialog.value = true
    }

    fun closeAddDeliveryWizard() {
        _showAddDeliveryDialog.value = false
    }

    fun dismissBackupReminder() {
        _backupReminderDismissed.value = true
    }

    fun addCustomer(
        name: String,
        address: String,
        type: String,
        purifiedPrice: Double,
        alkalinePrice: Double,
        isBankDirectAccount: Boolean = (type == "Bank / Direct")
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCustomer(
                CustomerEntity(
                    name = name,
                    address = address,
                    customerType = type,
                    isBankDirectAccount = isBankDirectAccount,
                    defaultPurifiedPrice = purifiedPrice,
                    defaultAlkalinePrice = alkalinePrice
                )
            )
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customer)
        }
    }

    fun recordDelivery(
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
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordDelivery(
                customerId = customerId,
                purifiedQty = purifiedQty,
                alkalineQty = alkalineQty,
                freePurifiedQty = freePurifiedQty,
                freeAlkalineQty = freeAlkalineQty,
                purifiedPrice = purifiedPrice,
                alkalinePrice = alkalinePrice,
                customDiscount = customDiscount,
                utangPaidAmount = utangPaidAmount,
                cashGiven = cashGiven,
                paymentStatus = paymentStatus,
                orderSource = orderSource
            )
        }
    }

    fun recordDeliveryByName(
        customerName: String,
        address: String,
        customerType: String,
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
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allCusts = repository.allCustomers.first()
                val existing = allCusts.find { it.name.equals(customerName, ignoreCase = true) }
                val customerId = if (existing != null) {
                    existing.id
                } else {
                    val newC = CustomerEntity(
                        name = customerName,
                        address = address.ifBlank { "Default Route" },
                        customerType = customerType,
                        isBankDirectAccount = (customerType == "Bank / Direct" || paymentStatus == "Direct to Boss"),
                        defaultPurifiedPrice = purifiedPrice,
                        defaultAlkalinePrice = alkalinePrice
                    )
                    repository.insertCustomer(newC)
                }

                repository.recordDelivery(
                    customerId = customerId,
                    purifiedQty = purifiedQty,
                    alkalineQty = alkalineQty,
                    freePurifiedQty = freePurifiedQty,
                    freeAlkalineQty = freeAlkalineQty,
                    purifiedPrice = purifiedPrice,
                    alkalinePrice = alkalinePrice,
                    customDiscount = customDiscount,
                    utangPaidAmount = utangPaidAmount,
                    cashGiven = cashGiven,
                    paymentStatus = paymentStatus,
                    orderSource = orderSource
                )
            }
        }
    }

    fun updateDelivery(
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
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDelivery(
                deliveryId = deliveryId,
                purifiedQty = purifiedQty,
                alkalineQty = alkalineQty,
                freePurifiedQty = freePurifiedQty,
                freeAlkalineQty = freeAlkalineQty,
                purifiedPrice = purifiedPrice,
                alkalinePrice = alkalinePrice,
                customDiscount = customDiscount,
                utangPaidAmount = utangPaidAmount,
                cashGiven = cashGiven,
                paymentStatus = paymentStatus,
                orderSource = orderSource
            )
        }
    }

    fun recordUtangPayment(customerId: Long, amount: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordUtangPayment(customerId, amount, notes)
        }
    }

    fun deleteDelivery(deliveryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDelivery(deliveryId)
        }
    }

    fun addExpense(category: String, amount: Double, notes: String, dateString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExpense(
                ExpenseEntity(
                    category = category,
                    amount = amount,
                    notes = notes,
                    dateString = dateString
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun addAdvanceRemit(amount: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertAdvanceRemit(
                AdvanceRemitEntity(
                    amount = amount,
                    notes = notes,
                    timestamp = System.currentTimeMillis(),
                    dateString = todayStr
                )
            )
        }
    }

    fun updateAdvanceRemit(advanceRemit: AdvanceRemitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAdvanceRemit(advanceRemit)
        }
    }

    fun deleteAdvanceRemit(advanceRemit: AdvanceRemitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAdvanceRemit(advanceRemit)
        }
    }

    fun addVale(amount: Double, notes: String, dateString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertVale(
                ValeLogEntity(
                    amount = amount,
                    notes = notes,
                    dateString = dateString
                )
            )
        }
    }

    fun deleteVale(vale: ValeLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVale(vale)
        }
    }

    fun archiveWeeklyPayout(
        weekLabel: String,
        paidGallons: Int,
        freeGallons: Int,
        commissionRate: Double,
        grossCommission: Double,
        totalVale: Double,
        netTakeHomePay: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.archiveWeeklyPayout(
                weekLabel = weekLabel,
                paidGallons = paidGallons,
                freeGallons = freeGallons,
                commissionRate = commissionRate,
                grossCommission = grossCommission,
                totalVale = totalVale,
                netTakeHomePay = netTakeHomePay
            )
        }
    }

    fun closeAfternoonShift(notes: String, onResult: (RemittanceLogEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.closeAfternoonShift(notes)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun updateCommissionRate(rate: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(current.copy(commissionPerGallon = rate))
            }
        }
    }

    fun updateDefaultPurifiedPrice(price: Double) {
        _defaultPriceConfig.value = _defaultPriceConfig.value.copy(defaultPurifiedPrice = price)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                themePreferences.setDefaultPrices(price, _defaultPriceConfig.value.defaultAlkalinePrice)
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(
                    current.copy(defaultPurifiedPrice = price, purifiedPreset2 = price)
                )
            }
        }
    }

    fun updateDefaultAlkalinePrice(price: Double) {
        _defaultPriceConfig.value = _defaultPriceConfig.value.copy(defaultAlkalinePrice = price)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                themePreferences.setDefaultPrices(_defaultPriceConfig.value.defaultPurifiedPrice, price)
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(
                    current.copy(defaultAlkalinePrice = price, alkalinePreset2 = price)
                )
            }
        }
    }

    fun updatePurifiedPreset1(price: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(current.copy(purifiedPreset1 = price))
            }
        }
    }

    fun updatePurifiedPreset2(price: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(
                    current.copy(purifiedPreset2 = price, defaultPurifiedPrice = price)
                )
            }
        }
    }

    fun updateAlkalinePreset1(price: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(current.copy(alkalinePreset1 = price))
            }
        }
    }

    fun updateAlkalinePreset2(price: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                repository.updateSettings(
                    current.copy(alkalinePreset2 = price, defaultAlkalinePrice = price)
                )
            }
        }
    }

    fun exportDatabase(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val exported = repository.exportToJson(outputStream)
                        if (exported) {
                            val now = System.currentTimeMillis()
                            val current = repository.getSettingsSync() ?: settings.value ?: SettingsEntity()
                            repository.updateSettings(current.copy(lastBackupTimestamp = now))
                            themePreferences.setLastAutoBackupTimestamp(now)
                            _backupReminderDismissed.value = true
                        }
                        exported
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun importDatabase(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val success = repository.importFromJson(inputStream)
                    withContext(Dispatchers.Main) {
                        onComplete(success)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun exportDeliveriesCsv(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val outputStream = appContext.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        repository.exportDeliveriesToCsv(outputStream)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun checkForAppUpdates() {
        _updateStatus.value = "Checking for updates..."
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.github.com/repos/melchorandresp2005-droid/Melvin-water-rider-app/releases/latest")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "MelvinWaterRider-Android")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _updateStatus.value = "Failed to check updates: ${e.localizedMessage}"
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val tagName = json.optString("tag_name", "v1.0")
                        val cleanTag = tagName.removePrefix("v").trim()
                        val currentVersion = BuildConfig.VERSION_NAME.trim()

                        val assets = json.optJSONArray("assets")
                        var downloadUrl = ""
                        if (assets != null && assets.length() > 0) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val url = asset.optString("browser_download_url", "")
                                if (url.endsWith(".apk", ignoreCase = true)) {
                                    downloadUrl = url
                                    break
                                }
                            }
                            if (downloadUrl.isEmpty()) {
                                downloadUrl = assets.getJSONObject(0).optString("browser_download_url", "")
                            }
                        }

                        if (downloadUrl.isEmpty()) {
                            _updateDownloadUrl.value = null
                            _updateStatus.value = "Release $tagName found, but no .apk asset attached in release assets."
                        } else {
                            _updateDownloadUrl.value = downloadUrl
                            if (isNewerRelease(tagName, currentVersion)) {
                                _updateStatus.value = "New release $tagName available! (Current: $currentVersion)"
                            } else {
                                _updateStatus.value = "Latest release: $tagName (You are on current version $currentVersion)"
                            }
                        }
                    } catch (e: Exception) {
                        _updateStatus.value = "Error parsing release info: ${e.localizedMessage}"
                    }
                } else {
                    _updateStatus.value = "No published GitHub releases found yet (HTTP ${response.code})."
                }
            }
        })
    }

    private fun isNewerRelease(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanLocal = currentVersion.removePrefix("v").trim()
        if (cleanRemote == cleanLocal) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return cleanRemote != cleanLocal
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        try {
            val appContext = context.applicationContext
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pm = appContext.packageManager
                if (!pm.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${appContext.packageName}"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(intent)
                    Toast.makeText(appContext, "Please allow 'Install unknown apps' permission to update.", Toast.LENGTH_LONG).show()
                    _updateStatus.value = "Grant 'Install unknown apps' permission in Settings."
                    return
                }
            }

            val destDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(destDir, "MelvinWaterRider_latest.apk")
            if (file.exists()) {
                file.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("MelvinWaterRider Update")
                .setDescription("Downloading latest APK...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, "MelvinWaterRider_latest.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        try {
                            appContext.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        installDownloadedApk(appContext)
                    }
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )

            _updateStatus.value = "Downloading update to app downloads directory..."
            Toast.makeText(appContext, "Downloading update...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            _updateStatus.value = "Download failed: ${e.localizedMessage}"
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun installDownloadedApk(context: Context) {
        try {
            val appContext = context.applicationContext
            val destDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(destDir, "MelvinWaterRider_latest.apk")
            if (!file.exists()) {
                Toast.makeText(appContext, "Downloaded APK not found in Downloads folder.", Toast.LENGTH_SHORT).show()
                _updateStatus.value = "Downloaded APK file not found."
                return
            }
            val apkUri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            _updateStatus.value = "Launching package installer..."
        } catch (e: Exception) {
            _updateStatus.value = "Install failed: ${e.localizedMessage}"
            Toast.makeText(context, "Install failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
