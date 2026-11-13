package com.kamarajlawoffice.mcopcalculator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToLong

enum class ClaimMode(val label: String, val shortLabel: String) {
    FATAL("Fatal Accident", "Fatal"),
    PERMANENT("Permanent Disability (Multiplier)", "Perm %"),
    PARTIAL("Partial Disability (Per % Point)", "Partial %"),
    TEMPORARY("Simple / Temporary Injury", "Temp")
}

data class HeadBifurcation(
    val headName: String,
    val amount: Double
)

data class McopCalculationResult(
    val netAnnualIncome: Double,
    val multiplier: Int,
    val netAwardAmount: Double,
    val interestAmount: Double,
    val totalPayableWithInterest: Double,
    val compromiseOfferAmount: Double,
    val compromisePercentageOfTotal: Double,
    val npvMaxCompromiseCeiling: Double,
    val heads: List<HeadBifurcation>
)

data class SavedMcopCase(
    val id: String,
    val mcopNumber: String,
    val claimantName: String,
    val claimModeName: String,
    val ageStr: String,
    val monthlyIncomeStr: String,
    val disabilityStr: String,
    val ratePerPctStr: String,
    val dependentsStr: String,
    val pranaySethiSteps: Int,
    val tempInjuryCompStr: String,
    val lossOfIncomeMonthsStr: String,
    val medicalStr: String,
    val futureMedicalStr: String,
    val painSufferingStr: String,
    val transportStr: String,
    val nourishmentStr: String,
    val attendantStr: String,
    val clothingDamageStr: String,
    val amenitiesStr: String,
    val prospectsStr: String,
    val interestRateStr: String,
    val petitionDateLabel: String,
    val daysStr: String,
    val customOfferStr: String,
    val savedAt: String,
    val netAwardPreview: Long
)

val PrimaryNavy = Color(0xFF1B4F72)
val SecondaryBlue = Color(0xFF2874A6)
val LightBlueBg = Color(0xFFEBF5FB)
val SurfaceBg = Color(0xFFF4F6F7)
val AccentGreen = Color(0xFF1E8449)
val AccentPurple = Color(0xFF6C3483)
val AccentRed = Color(0xFFB03A2E)
val GoldAccent = Color(0xFFD4AC0D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = PrimaryNavy.toArgb()
                    window.navigationBarColor = PrimaryNavy.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                }
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PrimaryNavy,
                    onPrimary = Color.White,
                    primaryContainer = LightBlueBg,
                    onPrimaryContainer = PrimaryNavy,
                    secondary = SecondaryBlue,
                    background = SurfaceBg,
                    surface = Color.White
                )
            ) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1300)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!showSplash) {
                        McopCalculatorApp()
                    }
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        SplashScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Balance,
                    contentDescription = "App Logo",
                    tint = GoldAccent,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "MCOP Compensation\nCalculator",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Motor Accident Claims & Compromise Portal",
                color = Color(0xFFD4E6F1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = GoldAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount.roundToLong())
}

fun getMultiplier(age: Int): Int {
    return when {
        age <= 25 -> 18
        age in 26..30 -> 17
        age in 31..35 -> 16
        age in 36..40 -> 15
        age in 41..45 -> 14
        age in 46..50 -> 13
        age in 51..55 -> 11
        age in 56..60 -> 9
        age in 61..65 -> 7
        else -> 5
    }
}

fun calculateCompensation(
    mode: ClaimMode,
    monthlyIncome: Double,
    age: Int,
    disabilityPercent: Double,
    ratePerPercentage: Double,
    dependentsCount: Int,
    pranaySethiSteps: Int,
    tempInjuryComp: Double,
    lossOfIncomeMonths: Double,
    medicalExpenses: Double,
    futureMedicalExpenses: Double,
    painAndSuffering: Double,
    transportationCharges: Double,
    extraNourishment: Double,
    attendantCharges: Double,
    clothingDamage: Double,
    lossOfAmenities: Double,
    futureProspectsPercent: Double,
    interestRatePercent: Double,
    daysOfAccidentToNow: Long,
    customCompromiseOffer: Double?
): McopCalculationResult {
    val annualIncome = monthlyIncome * 12.0
    val multiplier = getMultiplier(age)
    val prospectsFactor = 1.0 + (futureProspectsPercent / 100.0)

    val heads = mutableListOf<HeadBifurcation>()

    when (mode) {
        ClaimMode.FATAL -> {
            val deductionFraction = when {
                dependentsCount <= 1 -> 0.50
                dependentsCount in 2..3 -> 1.0 / 3.0
                dependentsCount in 4..6 -> 0.25
                else -> 0.20
            }
            val effectiveAnnualIncome = annualIncome * prospectsFactor * (1.0 - deductionFraction)
            val lossOfDependency = effectiveAnnualIncome * multiplier

            // Pranay Sethi 10% escalation every 3 years since 2017
            val escalationFactor = (1.10).pow(pranaySethiSteps.coerceIn(0, 5))
            val perConsortium = (40000.0 * escalationFactor).roundToLong().toDouble()
            val estateAndFuneral = (15000.0 * escalationFactor).roundToLong().toDouble()
            val consortiumTotal = (dependentsCount.coerceAtLeast(1)) * perConsortium

            val stepLabel = if (pranaySethiSteps > 0) " [+${pranaySethiSteps}x10%]" else ""

            heads.add(HeadBifurcation("Loss of Dependency (Mult: $multiplier)", lossOfDependency))
            heads.add(HeadBifurcation("Loss of Consortium ($dependentsCount × ₹${perConsortium.toInt()}$stepLabel)", consortiumTotal))
            heads.add(HeadBifurcation("Loss of Estate$stepLabel", estateAndFuneral))
            heads.add(HeadBifurcation("Funeral Expenses$stepLabel", estateAndFuneral))
            if (transportationCharges > 0) {
                heads.add(HeadBifurcation("Transportation of Body / Ambulance", transportationCharges))
            }
            if (clothingDamage > 0) {
                heads.add(HeadBifurcation("Damage to Clothing & Articles", clothingDamage))
            }
            if (medicalExpenses > 0) {
                heads.add(HeadBifurcation("Medical Expenses (Pre-Death)", medicalExpenses))
            }
        }
        ClaimMode.PERMANENT -> {
            val lossOfEarningCapacity = annualIncome * prospectsFactor * (disabilityPercent / 100.0) * multiplier
            heads.add(HeadBifurcation("Loss of Earning Capacity (${disabilityPercent.toInt()}\% • Mult: $multiplier)", lossOfEarningCapacity))
            if (lossOfIncomeMonths > 0 && monthlyIncome > 0) {
                heads.add(HeadBifurcation("Loss of Income During Treatment (${lossOfIncomeMonths.toInt()} Mos)", monthlyIncome * lossOfIncomeMonths))
            }
            if (painAndSuffering > 0) heads.add(HeadBifurcation("Pain & Suffering", painAndSuffering))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Medical Bills & Inpatient Treatment", medicalExpenses))
            if (futureMedicalExpenses > 0) heads.add(HeadBifurcation("Future Medical Expenses", futureMedicalExpenses))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transportation to Hospital", transportationCharges))
            if (extraNourishment > 0) heads.add(HeadBifurcation("Extra Nourishment", extraNourishment))
            if (attendantCharges > 0) heads.add(HeadBifurcation("Attendant Charges", attendantCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Damage to Clothing & Articles", clothingDamage))
            if (lossOfAmenities > 0) heads.add(HeadBifurcation("Loss of Amenities / Marriage Prospects", lossOfAmenities))
        }
        ClaimMode.PARTIAL -> {
            val disabilityAward = disabilityPercent * ratePerPercentage
            heads.add(HeadBifurcation("Permanent Disability (${disabilityPercent.toInt()}\% × ₹${ratePerPercentage.toInt()})", disabilityAward))
            if (lossOfIncomeMonths > 0 && monthlyIncome > 0) {
                heads.add(HeadBifurcation("Loss of Income During Treatment (${lossOfIncomeMonths.toInt()} Mos)", monthlyIncome * lossOfIncomeMonths))
            }
            if (painAndSuffering > 0) heads.add(HeadBifurcation("Pain & Suffering", painAndSuffering))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Medical Bills & Treatment", medicalExpenses))
            if (futureMedicalExpenses > 0) heads.add(HeadBifurcation("Future Medical / Implant Removal", futureMedicalExpenses))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transportation Charges", transportationCharges))
            if (extraNourishment > 0) heads.add(HeadBifurcation("Extra Nourishment", extraNourishment))
            if (attendantCharges > 0) heads.add(HeadBifurcation("Attendant Charges", attendantCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Damage to Clothing & Articles", clothingDamage))
            if (lossOfAmenities > 0) heads.add(HeadBifurcation("Loss of Amenities", lossOfAmenities))
        }
        ClaimMode.TEMPORARY -> {
            if (tempInjuryComp > 0) {
                heads.add(HeadBifurcation("Compensation for Injuries Sustained", tempInjuryComp))
            }
            if (lossOfIncomeMonths > 0 && monthlyIncome > 0) {
                heads.add(HeadBifurcation("Loss of Income (${lossOfIncomeMonths.toInt()} Months)", monthlyIncome * lossOfIncomeMonths))
            }
            if (painAndSuffering > 0) heads.add(HeadBifurcation("Pain & Suffering", painAndSuffering))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Medical Bills & Treatment", medicalExpenses))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transportation Charges", transportationCharges))
            if (extraNourishment > 0) heads.add(HeadBifurcation("Extra Nourishment", extraNourishment))
            if (attendantCharges > 0) heads.add(HeadBifurcation("Attendant Charges", attendantCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Damage to Clothing & Articles", clothingDamage))
        }
    }

    val netAward = heads.sumOf { it.amount }
    val years = (daysOfAccidentToNow.coerceAtLeast(0L)) / 365.25
    val interestAmount = netAward * (interestRatePercent / 100.0) * years
    val totalPayable = netAward + interestAmount

    val defaultCompromise = totalPayable * 0.85
    val compromiseOffer = if (customCompromiseOffer != null && customCompromiseOffer > 0.0) {
        customCompromiseOffer
    } else {
        defaultCompromise
    }

    val compromisePct = if (totalPayable > 0.0) (compromiseOffer / totalPayable) * 100.0 else 0.0
    val npvCeiling = netAward + (interestAmount * 0.65)

    return McopCalculationResult(
        netAnnualIncome = annualIncome,
        multiplier = multiplier,
        netAwardAmount = netAward,
        interestAmount = interestAmount,
        totalPayableWithInterest = totalPayable,
        compromiseOfferAmount = compromiseOffer,
        compromisePercentageOfTotal = compromisePct,
        npvMaxCompromiseCeiling = npvCeiling,
        heads = heads
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McopCalculatorApp() {
    val context = LocalContext.current

    var mcopNumber by remember { mutableStateOf("20/2026") }
    var claimantName by remember { mutableStateOf("Jeeva") }
    var ageStr by remember { mutableStateOf("34") }
    var monthlyIncomeStr by remember { mutableStateOf("15000") }
    var disabilityStr by remember { mutableStateOf("20") }
    var ratePerPctStr by remember { mutableStateOf("7000") }
    var dependentsStr by remember { mutableStateOf("3") }
    var pranaySethiSteps by remember { mutableStateOf(2) }

    // Injury & Conventional Heads
    var tempInjuryCompStr by remember { mutableStateOf("35000") }
    var lossOfIncomeMonthsStr by remember { mutableStateOf("3") }
    var medicalStr by remember { mutableStateOf("25000") }
    var futureMedicalStr by remember { mutableStateOf("0") }
    var painSufferingStr by remember { mutableStateOf("40000") }
    var transportStr by remember { mutableStateOf("10000") }
    var nourishmentStr by remember { mutableStateOf("15000") }
    var attendantStr by remember { mutableStateOf("15000") }
    var clothingDamageStr by remember { mutableStateOf("3000") }
    var amenitiesStr by remember { mutableStateOf("25000") }

    var prospectsStr by remember { mutableStateOf("40") }
    var interestRateStr by remember { mutableStateOf("7.5") }
    var petitionDateLabel by remember { mutableStateOf("09-05-2024") }
    var daysStr by remember { mutableStateOf("914") }
    var customOfferStr by remember { mutableStateOf("") }

    var selectedMode by remember { mutableStateOf(ClaimMode.PARTIAL) }
    var includeInterestInExport by remember { mutableStateOf(true) }
    var includeLokAdalatInExport by remember { mutableStateOf(true) }

    // Saved Cases Vault State
    var showSavedCasesDialog by remember { mutableStateOf(false) }
    var savedCasesList by remember { mutableStateOf(loadAllSavedCases(context)) }
    var searchQuery by remember { mutableStateOf("") }

    val age = ageStr.toIntOrNull() ?: 34
    val monthlyIncome = monthlyIncomeStr.toDoubleOrNull() ?: 15000.0
    val disability = disabilityStr.toDoubleOrNull() ?: 20.0
    val ratePerPct = ratePerPctStr.toDoubleOrNull() ?: 7000.0
    val dependents = dependentsStr.toIntOrNull() ?: 3
    val tempInjuryComp = tempInjuryCompStr.toDoubleOrNull() ?: 0.0
    val lossOfIncomeMonths = lossOfIncomeMonthsStr.toDoubleOrNull() ?: 0.0
    val medical = medicalStr.toDoubleOrNull() ?: 0.0
    val futureMedical = futureMedicalStr.toDoubleOrNull() ?: 0.0
    val painSuffering = painSufferingStr.toDoubleOrNull() ?: 0.0
    val transport = transportStr.toDoubleOrNull() ?: 0.0
    val nourishment = nourishmentStr.toDoubleOrNull() ?: 0.0
    val attendant = attendantStr.toDoubleOrNull() ?: 0.0
    val clothingDamage = clothingDamageStr.toDoubleOrNull() ?: 0.0
    val amenities = amenitiesStr.toDoubleOrNull() ?: 0.0
    val prospects = prospectsStr.toDoubleOrNull() ?: 40.0
    val interestRate = interestRateStr.toDoubleOrNull() ?: 7.5
    val days = daysStr.toLongOrNull() ?: 914L
    val customOffer = customOfferStr.toDoubleOrNull()

    val result = remember(
        selectedMode, monthlyIncome, age, disability, ratePerPct,
        dependents, pranaySethiSteps, tempInjuryComp, lossOfIncomeMonths,
        medical, futureMedical, painSuffering, transport, nourishment,
        attendant, clothingDamage, amenities, prospects, interestRate, days, customOffer
    ) {
        calculateCompensation(
            selectedMode, monthlyIncome, age, disability, ratePerPct,
            dependents, pranaySethiSteps, tempInjuryComp, lossOfIncomeMonths,
            medical, futureMedical, painSuffering, transport, nourishment,
            attendant, clothingDamage, amenities, prospects, interestRate, days, customOffer
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Balance,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MCOP Compensation Calculator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Motor Accident Claims Portal • Saved: ${savedCasesList.size}",
                                fontSize = 11.sp,
                                color = Color(0xFFD4E6F1)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryNavy,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                        val record = SavedMcopCase(
                            id = mcopNumber.trim().ifEmpty { UUID.randomUUID().toString() },
                            mcopNumber = mcopNumber,
                            claimantName = claimantName,
                            claimModeName = selectedMode.name,
                            ageStr = ageStr,
                            monthlyIncomeStr = monthlyIncomeStr,
                            disabilityStr = disabilityStr,
                            ratePerPctStr = ratePerPctStr,
                            dependentsStr = dependentsStr,
                            pranaySethiSteps = pranaySethiSteps,
                            tempInjuryCompStr = tempInjuryCompStr,
                            lossOfIncomeMonthsStr = lossOfIncomeMonthsStr,
                            medicalStr = medicalStr,
                            futureMedicalStr = futureMedicalStr,
                            painSufferingStr = painSufferingStr,
                            transportStr = transportStr,
                            nourishmentStr = nourishmentStr,
                            attendantStr = attendantStr,
                            clothingDamageStr = clothingDamageStr,
                            amenitiesStr = amenitiesStr,
                            prospectsStr = prospectsStr,
                            interestRateStr = interestRateStr,
                            petitionDateLabel = petitionDateLabel,
                            daysStr = daysStr,
                            customOfferStr = customOfferStr,
                            savedAt = timestamp,
                            netAwardPreview = result.netAwardAmount.roundToLong()
                        )
                        savedCasesList = saveCaseToPrefs(context, record)
                        Toast.makeText(context, "Saved MCOP $mcopNumber ($claimantName)", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Case")
                    }
                    IconButton(onClick = {
                        savedCasesList = loadAllSavedCases(context)
                        showSavedCasesDialog = true
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Find Saved Case")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceBg)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Case Vault Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CASE VAULT & ARCHIVE",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${savedCasesList.size} saved case(s) stored on device",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                savedCasesList = loadAllSavedCases(context)
                                showSavedCasesDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Find Case", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                                val record = SavedMcopCase(
                                    id = mcopNumber.trim().ifEmpty { UUID.randomUUID().toString() },
                                    mcopNumber = mcopNumber,
                                    claimantName = claimantName,
                                    claimModeName = selectedMode.name,
                                    ageStr = ageStr,
                                    monthlyIncomeStr = monthlyIncomeStr,
                                    disabilityStr = disabilityStr,
                                    ratePerPctStr = ratePerPctStr,
                                    dependentsStr = dependentsStr,
                                    pranaySethiSteps = pranaySethiSteps,
                                    tempInjuryCompStr = tempInjuryCompStr,
                                    lossOfIncomeMonthsStr = lossOfIncomeMonthsStr,
                                    medicalStr = medicalStr,
                                    futureMedicalStr = futureMedicalStr,
                                    painSufferingStr = painSufferingStr,
                                    transportStr = transportStr,
                                    nourishmentStr = nourishmentStr,
                                    attendantStr = attendantStr,
                                    clothingDamageStr = clothingDamageStr,
                                    amenitiesStr = amenitiesStr,
                                    prospectsStr = prospectsStr,
                                    interestRateStr = interestRateStr,
                                    petitionDateLabel = petitionDateLabel,
                                    daysStr = daysStr,
                                    customOfferStr = customOfferStr,
                                    savedAt = timestamp,
                                    netAwardPreview = result.netAwardAmount.roundToLong()
                                )
                                savedCasesList = saveCaseToPrefs(context, record)
                                Toast.makeText(context, "Case Saved: $mcopNumber", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Mode Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CLAIM CATEGORY",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ClaimMode.values().forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ClaimMode.values().size
                                ),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = PrimaryNavy,
                                    activeContentColor = Color.White,
                                    inactiveContainerColor = Color.White,
                                    inactiveContentColor = PrimaryNavy
                                )
                            ) {
                                Text(mode.shortLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 1. Case & Income Parameters Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. CASE & CLAIMANT PARAMETERS",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        fontSize = 13.sp
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = mcopNumber,
                            onValueChange = { mcopNumber = it },
                            label = { Text("MCOP No.") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = claimantName,
                            onValueChange = { claimantName = it },
                            label = { Text("Claimant Name") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = { ageStr = it },
                            label = { Text("Age (Yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = monthlyIncomeStr,
                            onValueChange = { monthlyIncomeStr = it },
                            label = { Text("Monthly Income (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }

                    when (selectedMode) {
                        ClaimMode.FATAL -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = dependentsStr
