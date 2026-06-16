package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.HistoryItem
import com.example.data.HistoryRepository
import com.example.util.AdManager
import com.example.util.CalEngines
import com.example.util.HistoryExporter
import com.example.util.SoundManager
import android.util.Log
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CalProViewModel(application: Application) : AndroidViewModel(application) {

    // Room Database Init
    private val database by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "calpro_history_db"
        ).fallbackToDestructiveMigration().build()
    }
    
    private val repository by lazy {
        HistoryRepository(database.historyDao())
    }

    // History Flow observed by Compose UI
    val historyItemsStateList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Screen Navigation
    var currentScreen by mutableStateOf("splash") // splash, home, history
    
    // Theme option (true = dark, false = light)
    var isDarkTheme by mutableStateOf(false)

    // Current selected active tool index:
    // 0: Calculator, 1: EMI Calculator, 2: Age Calculator, 3: GST Calculator, 4: Unit Converter, 5: Currency Converter
    var activeToolIndex by mutableStateOf(0)

    // Search query & visibility
    var searchQuery by mutableStateOf("")
    var isSearchActive by mutableStateOf(false)

    // Standard Calculator State Variables
    var calcInputDisplay by mutableStateOf("")
    var calcResultDisplay by mutableStateOf("")

    // EMI Calculator Inputs
    var emiLoanAmount by mutableStateOf("100000")
    var emiInterestRate by mutableStateOf("8.5")
    var emiTenureYears by mutableStateOf("5")
    var emiResultState by mutableStateOf(CalEngines.EmiResult(0.0, 0.0, 0.0))

    // Age Calculator Inputs
    var ageDobDate by mutableStateOf(LocalDate.of(2000, 1, 1))
    var ageResultState by mutableStateOf(CalEngines.AgeResult(0, 0, 0))

    // GST Calculator Inputs
    var gstAmountStr by mutableStateOf("1000")
    var gstRateStr by mutableStateOf("18")
    var gstResultState by mutableStateOf(CalEngines.GstResult(0.0, 0.0))

    // Unit Converter State
    var unitCategory by mutableStateOf("Length") // Length, Weight, Area, Temperature
    var unitInputVal by mutableStateOf("1")
    var unitFromUnit by mutableStateOf("Meters")
    var unitToUnit by mutableStateOf("Kilometers")
    var unitResultVal by mutableStateOf(0.0)

    // Currency Converter State
    var currencyInputVal by mutableStateOf("100")
    var currencyFromCode by mutableStateOf("USD")
    var currencyToCode by mutableStateOf("EUR")
    var currencyResultVal by mutableStateOf(0.0)

    // Dynamic Exchange Rates & Status Message
    var currencyRatesMap by mutableStateOf(mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "INR" to 83.50,
        "GBP" to 0.79,
        "JPY" to 157.30,
        "CAD" to 1.37,
        "AUD" to 1.51,
        "CHF" to 0.90,
        "CNY" to 7.25,
        "SGD" to 1.35
    ))
    var currencyStatusMessage by mutableStateOf("Using Default Rates")
    var lastUpdatedTimeByStr by mutableStateOf("Never")

    // 1. Percentage Calculator State (Index 6)
    var pctInputVal by mutableStateOf("15")
    var pctTotalVal by mutableStateOf("250")
    var pctResultVal by mutableStateOf(37.5)

    // 2. Discount Calculator State (Index 7)
    var discountOrigPrice by mutableStateOf("100")
    var discountPct by mutableStateOf("20")
    var discountFinalPrice by mutableStateOf(80.0)
    var discountSavings by mutableStateOf(20.0)

    // 3. Loan Calculator State (Index 8)
    var loanAmount by mutableStateOf("250000")
    var loanInterestRate by mutableStateOf("9.0")
    var loanTenureYears by mutableStateOf("10")
    var loanEmi by mutableStateOf(0.0)
    var loanTotalInterest by mutableStateOf(0.0)
    var loanTotalPayment by mutableStateOf(0.0)

    // 4. Date Difference Calculator State (Index 9)
    var dateDiffStartDate by mutableStateOf(LocalDate.now())
    var dateDiffEndDate by mutableStateOf(LocalDate.now().plusDays(30))
    var dateDiffDays by mutableStateOf(30L)
    var dateDiffMonths by mutableStateOf(1)
    var dateDiffYears by mutableStateOf(0)

    val df = DecimalFormat("#,##0.00")
    val dfInput = DecimalFormat("#,###.##")

    init {
        // Run initial calculations
        updateEmi()
        updateAge()
        updateGst()
        updateUnits()
        fetchExchangeRates()
        updatePercentage()
        updateDiscount()
        updateLoan()
        updateDateDifference()
    }

    // Switch screen with instant transition
    fun navigateTo(screen: String) {
        currentScreen = screen
        searchQuery = ""
        isSearchActive = false
    }

    // Toggle day/night theme
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    // Search Engine Items matching: Disconnect home screen options and search hidden calculators
    val searchResults: List<Pair<String, Int>>
        get() {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) return emptyList()
            val res = mutableListOf<Pair<String, Int>>()
            
            // Percentage Calculator matching
            if ("percentage calculator".contains(q) || listOf("percentage", "percent", "%", "profit", "loss", "margin").any { it.contains(q) }) {
                res.add("Percentage Calculator" to 6)
            }
            // Discount Calculator matching
            if ("discount calculator".contains(q) || listOf("discount", "offer", "sale", "price off", "save money").any { it.contains(q) }) {
                res.add("Discount Calculator" to 7)
            }
            // Loan Calculator matching
            if ("loan calculator".contains(q) || listOf("loan", "interest", "finance", "emi loan", "borrow").any { it.contains(q) }) {
                res.add("Loan Calculator" to 8)
            }
            // Date Difference Calculator matching
            if ("date difference calculator".contains(q) || listOf("date", "days", "months", "years", "date difference", "duration").any { it.contains(q) }) {
                res.add("Date Difference Calculator" to 9)
            }
            return res
        }

    // Helper to log calculation records to database
    fun saveCalculationToHistory(toolName: String, input: String, output: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            
            repository.insert(
                HistoryItem(
                    toolName = toolName,
                    inputs = input,
                    outputs = output,
                    dateString = dateStr,
                    timeString = timeStr
                )
            )
            // Play save sound
            SoundManager.playSave()
            // Hook in Ad counter increment
            AdManager.incrementCalculation(getApplication(), viewModelScope)
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    // --- STANDARD CALCULATOR CONTROLS ---
    fun onCalcKeyPress(key: String) {
        if (key != "=") {
            SoundManager.playTap()
        }
        when (key) {
            "AC" -> {
                calcInputDisplay = ""
                calcResultDisplay = ""
            }
            "DEL" -> {
                if (calcInputDisplay.isNotEmpty()) {
                    calcInputDisplay = calcInputDisplay.dropLast(1)
                    evaluateCalcLive()
                }
            }
            "+/-" -> {
                if (calcInputDisplay.isNotEmpty()) {
                    // Toggle sign of last number or expression
                    if (calcInputDisplay.startsWith("-")) {
                        calcInputDisplay = calcInputDisplay.drop(1)
                    } else {
                        calcInputDisplay = "-$calcInputDisplay"
                    }
                    evaluateCalcLive()
                }
            }
            "=" -> {
                if (calcInputDisplay.isNotEmpty()) {
                    val finalResult = evaluateExpression(calcInputDisplay)
                    if (finalResult != null) {
                        val formattedOutput = formatNumber(finalResult)
                        calcResultDisplay = formattedOutput
                        
                        // Play success sound
                        SoundManager.playSuccess()
                        
                        // Save Standard Calc success to history
                        saveCalculationToHistory("Standard Calculator", calcInputDisplay, "= $formattedOutput")
                        
                        calcInputDisplay = formattedOutput
                    } else {
                        calcResultDisplay = "Error"
                        // Play error sound
                        SoundManager.playError()
                    }
                }
            }
            "%" -> {
                if (calcInputDisplay.isNotEmpty() && !calcInputDisplay.endsWith(" ") && !calcInputDisplay.endsWith(".")) {
                    calcInputDisplay += "%"
                    evaluateCalcLive()
                }
            }
            "+", "-", "×", "÷" -> {
                if (calcInputDisplay.isNotEmpty()) {
                    val lastChar = calcInputDisplay.last()
                    if (lastChar == ' ' && calcInputDisplay.length > 2) {
                        calcInputDisplay = calcInputDisplay.dropLast(3) + " $key "
                    } else if (lastChar != ' ' && lastChar != '.') {
                        calcInputDisplay += " $key "
                    }
                }
            }
            "." -> {
                val parts = calcInputDisplay.split(" ")
                val lastPart = parts.lastOrNull() ?: ""
                if (!lastPart.contains(".")) {
                    calcInputDisplay += if (lastPart.isEmpty()) "0." else "."
                }
            }
            else -> { // Digits
                val parts = calcInputDisplay.split(" ")
                val lastPart = parts.lastOrNull() ?: ""
                // Avoid leading double zeros
                if (!(lastPart == "0" && key == "0")) {
                    calcInputDisplay += key
                    evaluateCalcLive()
                }
            }
        }
    }

    private fun evaluateCalcLive() {
        if (calcInputDisplay.isNotEmpty()) {
            val res = evaluateExpression(calcInputDisplay)
            if (res != null) {
                calcResultDisplay = formatNumber(res)
            }
        } else {
            calcResultDisplay = ""
        }
    }

    private fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) {
            num.toLong().toString()
        } else {
            df.format(num)
        }
    }

    // Arithmetic parsing stack evaluator
    private fun evaluateExpression(expr: String): Double? {
        try {
            // Pre-process percentages: "value%" -> "value/100"
            var cleanExpr = expr.trim()
            if (cleanExpr.isEmpty()) return null
            
            // Basic operations split
            val tokens = ArrayList<String>()
            var currentNum = StringBuilder()
            var i = 0
            while (i < cleanExpr.length) {
                val c = cleanExpr[i]
                if (c == ' ') {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    // Skip multiple spaces, check next non-space char as operator
                    while (i + 1 < cleanExpr.length && cleanExpr[i + 1] == ' ') i++
                } else if (c == '×' || c == '÷' || c == '+' || c == '-') {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    tokens.add(c.toString())
                } else {
                    currentNum.append(c)
                }
                i++
            }
            if (currentNum.isNotEmpty()) {
                tokens.add(currentNum.toString())
            }

            // High precedence passes (percentage conversion)
            val updatedTokens = ArrayList<String>()
            for (token in tokens) {
                if (token.endsWith("%")) {
                    val rawValStr = token.dropLast(1)
                    val rawVal = rawValStr.toDoubleOrNull() ?: 0.0
                    updatedTokens.add((rawVal / 100.0).toString())
                } else {
                    updatedTokens.add(token)
                }
            }

            // Evaluate Multiplications and Divisions
            val firstPass = ArrayList<String>()
            var index = 0
            while (index < updatedTokens.size) {
                val token = updatedTokens[index]
                if (token == "×" || token == "÷") {
                    val leftVal = firstPass.removeAt(firstPass.size - 1).toDoubleOrNull() ?: 0.0
                    val rightToken = if (index + 1 < updatedTokens.size) updatedTokens[index + 1] else "0"
                    val rightVal = rightToken.toDoubleOrNull() ?: 1.0
                    val res = if (token == "×") leftVal * rightVal else {
                        if (rightVal == 0.0) return 0.0 // Div by zero precaution
                        leftVal / rightVal
                    }
                    firstPass.add(res.toString())
                    index += 2
                } else {
                    firstPass.add(token)
                    index++
                }
            }

            // Evaluate Additions and Subtractions
            if (firstPass.isEmpty()) return null
            var result = firstPass[0].toDoubleOrNull() ?: 0.0
            index = 1
            while (index < firstPass.size) {
                val op = firstPass[index]
                val rightValToken = if (index + 1 < firstPass.size) firstPass[index + 1] else "0"
                val rightVal = rightValToken.toDoubleOrNull() ?: 0.0
                if (op == "+") {
                    result += rightVal
                } else if (op == "-") {
                    result -= rightVal
                }
                index += 2
            }
            return result
        } catch (e: Exception) {
            return null
        }
    }

    // --- EMI CALCULATOR CONTROLS ---
    fun updateEmi() {
        val p = emiLoanAmount.toDoubleOrNull() ?: 0.0
        val r = emiInterestRate.toDoubleOrNull() ?: 0.0
        val t = emiTenureYears.toDoubleOrNull() ?: 0.0
        emiResultState = CalEngines.calculateEmi(p, r, t)
    }

    fun saveEmiClicked() {
        updateEmi()
        val textInput = "${dfInput.format(emiLoanAmount.toDoubleOrNull() ?: 0.0)} @ ${emiInterestRate}% for ${emiTenureYears} yr"
        val textOutput = "= ${df.format(emiResultState.monthlyEmi)} / mo"
        saveCalculationToHistory("EMI Calculator", textInput, textOutput)
    }

    // --- AGE CALCULATOR CONTROLS ---
    fun updateAge() {
        ageResultState = CalEngines.calculateAge(ageDobDate)
    }

    fun saveAgeClicked() {
        updateAge()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val textInput = "DOB: ${ageDobDate.format(formatter)}"
        val textOutput = "= ${ageResultState.years} Years, ${ageResultState.months} Months, ${ageResultState.days} Days"
        saveCalculationToHistory("Age Calculator", textInput, textOutput)
    }

    // --- GST CALCULATOR CONTROLS ---
    fun updateGst() {
        val amt = gstAmountStr.toDoubleOrNull() ?: 0.0
        val rate = gstRateStr.toDoubleOrNull() ?: 0.0
        gstResultState = CalEngines.calculateGst(amt, rate)
    }

    fun saveGstClicked() {
        updateGst()
        val textInput = "${dfInput.format(gstAmountStr.toDoubleOrNull() ?: 0.0)} + ${gstRateStr}% GST"
        val textOutput = "= GST Amt: ${df.format(gstResultState.gstAmount)} | Total: ${df.format(gstResultState.finalAmount)}"
        saveCalculationToHistory("GST Calculator", textInput, textOutput)
    }

    // --- UNIT CONVERTER CONTROLS ---
    fun changeUnitCategory(cat: String) {
        unitCategory = cat
        val unitList = when (cat) {
            "Length" -> CalEngines.Units.lengthFactors.keys.toList()
            "Weight" -> CalEngines.Units.weightFactors.keys.toList()
            "Area" -> CalEngines.Units.areaFactors.keys.toList()
            else -> listOf("Celsius", "Fahrenheit", "Kelvin")
        }
        unitFromUnit = unitList[0]
        unitToUnit = unitList[1]
        updateUnits()
    }

    fun updateUnits() {
        val v = unitInputVal.toDoubleOrNull() ?: 0.0
        unitResultVal = CalEngines.convertUnits(unitCategory, v, unitFromUnit, unitToUnit)
    }

    fun saveUnitsClicked() {
        updateUnits()
        val v = unitInputVal.toDoubleOrNull() ?: 0.0
        val textInput = "$v $unitFromUnit"
        val textOutput = "= ${df.format(unitResultVal)} $unitToUnit"
        saveCalculationToHistory("Unit Converter - $unitCategory", textInput, textOutput)
    }

    // --- CURRENCY CONVERTER CONTROLS ---
    fun updateCurrency() {
        val v = currencyInputVal.toDoubleOrNull() ?: 0.0
        val fromRate = currencyRatesMap[currencyFromCode] ?: 1.0
        val toRate = currencyRatesMap[currencyToCode] ?: 1.0
        val amountInUsd = v / fromRate
        currencyResultVal = amountInUsd * toRate
    }

    fun saveCurrencyClicked() {
        updateCurrency()
        val v = currencyInputVal.toDoubleOrNull() ?: 0.0
        val textInput = "$v $currencyFromCode to $currencyToCode"
        val textOutput = "= ${df.format(currencyResultVal)} $currencyToCode"
        saveCalculationToHistory("Currency Converter", textInput, textOutput)
    }

    fun fetchExchangeRates() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            var fetchedText = ""
            var providerStatus = ""
            
            // 1. Try Frankfurter API as primary provider
            try {
                val conn = java.net.URL("https://api.frankfurter.app/latest?from=USD").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                
                if (conn.responseCode == 200) {
                    fetchedText = conn.inputStream.bufferedReader().use { it.readText() }
                    providerStatus = "Live Rate"
                    success = true
                }
            } catch (e: Exception) {
                Log.e("CalProViewModel", "Frankfurter API primary fetch failed, attempting backup provider...", e)
            }
            
            // 2. Try ExchangeRate Host as secondary backup provider
            if (!success) {
                try {
                    val conn = java.net.URL("https://api.exchangerate.host/latest?base=USD").openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    
                    if (conn.responseCode == 200) {
                        fetchedText = conn.inputStream.bufferedReader().use { it.readText() }
                        providerStatus = "Backup Rate"
                        success = true
                    }
                } catch (e: Exception) {
                    Log.e("CalProViewModel", "Backup ExchangeRate Host fetch failed...", e)
                }
            }
            
            // 3. Process result or fallback
            if (success && fetchedText.isNotEmpty()) {
                try {
                    val json = org.json.JSONObject(fetchedText)
                    val ratesJson = json.getJSONObject("rates")
                    
                    val newRates = mutableMapOf<String, Double>("USD" to 1.0)
                    val keys = ratesJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        newRates[key] = ratesJson.getDouble(key)
                    }
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        currencyRatesMap = newRates
                        val sdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
                        val timeStr = sdf.format(Date())
                        lastUpdatedTimeByStr = timeStr
                        currencyStatusMessage = providerStatus
                        
                        // Play refresh sound
                        SoundManager.playRefresh()
                        
                        // Save to cache with timestamp
                        val prefs = getApplication<Application>().getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putString("rates_json", fetchedText)
                        editor.putString("last_update_time", timeStr)
                        editor.putLong("last_update_timestamp", System.currentTimeMillis())
                        editor.apply()
                        
                        updateCurrency()
                    }
                } catch (e: Exception) {
                    Log.e("CalProViewModel", "JSON parsing failed for fetched rates", e)
                    loadRatesFromCache()
                }
            } else {
                loadRatesFromCache()
            }
        }
    }

    private fun loadRatesFromCache() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val prefs = getApplication<Application>().getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("rates_json", null)
            val timeStr = prefs.getString("last_update_time", "Never") ?: "Never"
            val lastTimestamp = prefs.getLong("last_update_timestamp", 0L)
            
            val isExpired = lastTimestamp != 0L && (System.currentTimeMillis() - lastTimestamp >= 24 * 3600 * 1000)
            val status = if (isExpired) "Offline Cached Rate" else "Offline Cached Rate"
            
            if (jsonStr != null) {
                try {
                    val json = org.json.JSONObject(jsonStr)
                    val ratesJson = json.getJSONObject("rates")
                    val newRates = mutableMapOf<String, Double>("USD" to 1.0)
                    val keys = ratesJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        newRates[key] = ratesJson.getDouble(key)
                    }
                    currencyRatesMap = newRates
                    lastUpdatedTimeByStr = timeStr
                    currencyStatusMessage = status
                    
                    if (isExpired) {
                        // Expired: attempt reload in background
                        fetchExchangeRates()
                    } else {
                        updateCurrency()
                    }
                } catch (e: Exception) {
                    currencyStatusMessage = "No Internet Connection"
                }
            } else {
                currencyStatusMessage = "No Internet Connection"
            }
        }
    }

    // --- 1. PERCENTAGE CALCULATOR CONTROLS ---
    fun updatePercentage() {
        val p = pctInputVal.toDoubleOrNull() ?: 0.0
        val t = pctTotalVal.toDoubleOrNull() ?: 0.0
        pctResultVal = (p / 100.0) * t
    }

    fun savePercentageClicked() {
        updatePercentage()
        val textInput = "${dfInput.format(pctInputVal.toDoubleOrNull() ?: 0.0)}% of ${dfInput.format(pctTotalVal.toDoubleOrNull() ?: 0.0)}"
        val textOutput = "= ${df.format(pctResultVal)}"
        saveCalculationToHistory("Percentage Calculator", textInput, textOutput)
    }

    // --- 2. DISCOUNT CALCULATOR CONTROLS ---
    fun updateDiscount() {
        val orig = discountOrigPrice.toDoubleOrNull() ?: 0.0
        val pct = discountPct.toDoubleOrNull() ?: 0.0
        discountSavings = orig * (pct / 100.0)
        discountFinalPrice = orig - discountSavings
    }

    fun saveDiscountClicked() {
        updateDiscount()
        val orig = discountOrigPrice.toDoubleOrNull() ?: 0.0
        val textInput = "Orig: ${dfInput.format(orig)} | Less: ${discountPct}%"
        val textOutput = "= Final: ${df.format(discountFinalPrice)} (Saved: ${df.format(discountSavings)})"
        saveCalculationToHistory("Discount Calculator", textInput, textOutput)
    }

    // --- 3. LOAN CALCULATOR CONTROLS ---
    fun updateLoan() {
        val p = loanAmount.toDoubleOrNull() ?: 0.0
        val r = loanInterestRate.toDoubleOrNull() ?: 0.0
        val t = loanTenureYears.toDoubleOrNull() ?: 0.0
        val res = CalEngines.calculateEmi(p, r, t)
        loanEmi = res.monthlyEmi
        loanTotalInterest = res.totalInterest
        loanTotalPayment = res.totalPayment
    }

    fun saveLoanClicked() {
        updateLoan()
        val p = loanAmount.toDoubleOrNull() ?: 0.0
        val textInput = "${dfInput.format(p)} @ ${loanInterestRate}% for ${loanTenureYears} yr"
        val textOutput = "= EMI: ${df.format(loanEmi)}/mo | Total Int: ${df.format(loanTotalInterest)}"
        saveCalculationToHistory("Loan Calculator", textInput, textOutput)
    }

    // --- 4. DATE DIFFERENCE CALCULATOR CONTROLS ---
    fun updateDateDifference() {
        val start = dateDiffStartDate
        val end = dateDiffEndDate
        if (start.isAfter(end)) {
            dateDiffDays = 0L
            dateDiffMonths = 0
            dateDiffYears = 0
            return
        }
        val period = java.time.Period.between(start, end)
        dateDiffDays = java.time.temporal.ChronoUnit.DAYS.between(start, end)
        dateDiffMonths = period.months + (period.years * 12)
        dateDiffYears = period.years
    }

    fun saveDateDifferenceClicked() {
        updateDateDifference()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val textInput = "From: ${dateDiffStartDate.format(formatter)} | To: ${dateDiffEndDate.format(formatter)}"
        val textOutput = "= ${dateDiffDays} Days (${dateDiffMonths} Months, ${dateDiffYears} Years)"
        saveCalculationToHistory("Date Difference", textInput, textOutput)
    }

    // --- HISTORY EXPORTS WRAPPED IN REWARD CALLBACK ---
    fun triggerHistoryDownload(context: Context, format: String, onNotificationToast: (String) -> Unit) {
        // Must unlock via Watched Rewarded Ad
        AdManager.showRewarded(context) {
            viewModelScope.launch {
                val list = historyItemsStateList.value
                val file = if (format == "pdf") {
                    HistoryExporter.exportToPdf(context, list)
                } else {
                    HistoryExporter.exportToTxt(context, list)
                }
                
                if (file != null) {
                    onNotificationToast("Saved to downloads: ${file.name}")
                } else {
                    onNotificationToast("Failed to write history file.")
                }
            }
        }
    }
}
