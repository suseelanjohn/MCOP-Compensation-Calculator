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
            heads.add(HeadBifurcation("Loss of Earning Capacity (${disabilityPercent.toInt()}% • Mult: $multiplier)", lossOfEarningCapacity))
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
            heads.add(HeadBifurcation("Permanent Disability (${disabilityPercent.toInt()}% × ₹${ratePerPercentage.toInt()})", disabilityAward))
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
                                    value = dependentsStr,
                                    onValueChange = { dependentsStr = it },
                                    label = { Text("No. of Dependents") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = prospectsStr,
                                    onValueChange = { prospectsStr = it },
                                    label = { Text("Future Prospects %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pranay Sethi 10% Escalation (Every 3 Yrs on Consortium/Estate/Funeral)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryNavy
                            )
                            val stepOptions = listOf("Base (₹40k)", "+10% (₹44k)", "+21% (₹48.4k)", "+33% (₹53.2k)")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                stepOptions.forEachIndexed { idx, label ->
                                    SegmentedButton(
                                        selected = pranaySethiSteps == idx,
                                        onClick = { pranaySethiSteps = idx },
                                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = stepOptions.size)
                                    ) {
                                        Text(label, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        ClaimMode.PERMANENT -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = disabilityStr,
                                    onValueChange = { disabilityStr = it },
                                    label = { Text("Functional Disab. %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = prospectsStr,
                                    onValueChange = { prospectsStr = it },
                                    label = { Text("Future Prospects %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        ClaimMode.PARTIAL -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = disabilityStr,
                                    onValueChange = { disabilityStr = it },
                                    label = { Text("Disability %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = ratePerPctStr,
                                    onValueChange = { ratePerPctStr = it },
                                    label = { Text("Rate per % (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        ClaimMode.TEMPORARY -> {
                            OutlinedTextField(
                                value = tempInjuryCompStr,
                                onValueChange = { tempInjuryCompStr = it },
                                label = { Text("Injury Compensation (For Injuries Sustained ₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // 2. All Conventional & Pecuniary Heads Card
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
                        text = if (selectedMode == ClaimMode.FATAL) "2. FATAL PECUNIARY HEADS & INTEREST" else "2. ALL INJURY CONVENTIONAL HEADS & INTEREST",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        fontSize = 13.sp
                    )

                    if (selectedMode == ClaimMode.FATAL) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = transportStr,
                                onValueChange = { transportStr = it },
                                label = { Text("Transport/Ambulance (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = medicalStr,
                                onValueChange = { medicalStr = it },
                                label = { Text("Pre-Death Medical (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = clothingDamageStr,
                            onValueChange = { clothingDamageStr = it },
                            label = { Text("Damage to Clothing & Articles (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        // Complete set of Injury Heads
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = lossOfIncomeMonthsStr,
                                onValueChange = { lossOfIncomeMonthsStr = it },
                                label = { Text("Treatment Loss (Months)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = painSufferingStr,
                                onValueChange = { painSufferingStr = it },
                                label = { Text("Pain & Suffering (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = medicalStr,
                                onValueChange = { medicalStr = it },
                                label = { Text("Medical Bills (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            if (selectedMode != ClaimMode.TEMPORARY) {
                                OutlinedTextField(
                                    value = futureMedicalStr,
                                    onValueChange = { futureMedicalStr = it },
                                    label = { Text("Future Medical (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            } else {
                                OutlinedTextField(
                                    value = transportStr,
                                    onValueChange = { transportStr = it },
                                    label = { Text("Transportation (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        if (selectedMode != ClaimMode.TEMPORARY) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = transportStr,
                                    onValueChange = { transportStr = it },
                                    label = { Text("Transportation (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = nourishmentStr,
                                    onValueChange = { nourishmentStr = it },
                                    label = { Text("Extra Nourishment (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = nourishmentStr,
                                    onValueChange = { nourishmentStr = it },
                                    label = { Text("Extra Nourishment (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = attendantStr,
                                    onValueChange = { attendantStr = it },
                                    label = { Text("Attendant Charges (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (selectedMode != ClaimMode.TEMPORARY) {
                                OutlinedTextField(
                                    value = attendantStr,
                                    onValueChange = { attendantStr = it },
                                    label = { Text("Attendant Charges (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = clothingDamageStr,
                                onValueChange = { clothingDamageStr = it },
                                label = { Text("Damage to Clothing (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        if (selectedMode != ClaimMode.TEMPORARY) {
                            OutlinedTextField(
                                value = amenitiesStr,
                                onValueChange = { amenitiesStr = it },
                                label = { Text("Loss of Amenities / Marriage Prospects (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = petitionDateLabel,
                            onValueChange = { petitionDateLabel = it },
                            label = { Text("Petition Date") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = daysStr,
                            onValueChange = { daysStr = it },
                            label = { Text("Days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = interestRateStr,
                            onValueChange = { interestRateStr = it },
                            label = { Text("Int %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = customOfferStr,
                        onValueChange = { customOfferStr = it },
                        label = { Text("Custom Lok Adalat Offer ₹ (Leave blank for 85% auto)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Head-Wise Bifurcation Live Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                elevation = CardDefaults.cardElevation(3.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HEAD-WISE BIFURCATION",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = PrimaryNavy,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Age: $age | Mult: ${result.multiplier}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = PrimaryNavy.copy(alpha = 0.2f))

                    result.heads.forEachIndexed { idx, head ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${idx + 1}. ${head.headName}",
                                fontSize = 13.sp,
                                color = Color(0xFF1C2833),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatInr(head.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C2833)
                            )
                        }
                    }

                    HorizontalDivider(color = PrimaryNavy.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NET PRINCIPAL AWARD:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                        Text(formatInr(result.netAwardAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                    }

                    if (includeInterestInExport) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Interest (${interestRate}% from $petitionDateLabel • $days days):",
                                fontSize = 12.sp,
                                color = AccentGreen
                            )
                            Text(
                                text = "+ ${formatInr(result.interestAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL PAYABLE TODAY:", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AccentRed)
                            Text(formatInr(result.totalPayableWithInterest), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AccentRed)
                        }
                    }

                    if (includeLokAdalatInExport) {
                        HorizontalDivider(color = PrimaryNavy.copy(alpha = 0.2f))
                        Text(
                            text = "LOK ADALAT COMPROMISE ANALYSIS (NO SEPARATE INTEREST)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AccentPurple
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• Consolidated Lump Sum Offer (${String.format(Locale.US, "%.1f", result.compromisePercentageOfTotal)}%):",
                                fontSize = 13.sp,
                                color = AccentPurple
                            )
                            Text(
                                text = formatInr(result.compromiseOfferAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AccentPurple
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• Max NPV Settlement Ceiling:", fontSize = 13.sp, color = AccentPurple)
                            Text(formatInr(result.npvMaxCompromiseCeiling), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentPurple)
                        }
                    }
                }
            }

            // 3. Export & Share Controls Card
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
                        text = "3. EXPORT & SHARE CONTROLS",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Interest Projection", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Turn off to export Principal Award or Lok Adalat only", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = includeInterestInExport,
                            onCheckedChange = { includeInterestInExport = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Lok Adalat Compromise Section", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Lump-sum compromise without interest breakdown", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = includeLokAdalatInExport,
                            onCheckedChange = { includeLokAdalatInExport = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val text = formatExportText(
                                    mcopNumber, claimantName, age, petitionDateLabel, days,
                                    interestRate, selectedMode, result,
                                    includeInterestInExport, includeLokAdalatInExport
                                )
                                shareText(context, text)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                exportPdfMemo(
                                    context, mcopNumber, claimantName, age, petitionDateLabel, days,
                                    interestRate, selectedMode, result,
                                    includeInterestInExport, includeLokAdalatInExport
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Memo", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                exportCsvForSheets(
                                    context, mcopNumber, claimantName, age, selectedMode,
                                    result, includeInterestInExport
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Saved Cases Search & Load Modal Dialog
    if (showSavedCasesDialog) {
        val filteredList = remember(savedCasesList, searchQuery) {
            if (searchQuery.isBlank()) {
                savedCasesList
            } else {
                val q = searchQuery.trim().lowercase()
                savedCasesList.filter {
                    it.mcopNumber.lowercase().contains(q) ||
                        it.claimantName.lowercase().contains(q) ||
                        it.claimModeName.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showSavedCasesDialog = false },
            title = {
                Column {
                    Text("Saved MCOP Cases Vault", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search MCOP No. or Claimant Name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            text = {
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching saved cases found.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mcopNumber = item.mcopNumber
                                        claimantName = item.claimantName
                                        selectedMode = runCatching { ClaimMode.valueOf(item.claimModeName) }.getOrDefault(ClaimMode.PARTIAL)
                                        ageStr = item.ageStr
                                        monthlyIncomeStr = item.monthlyIncomeStr
                                        disabilityStr = item.disabilityStr
                                        ratePerPctStr = item.ratePerPctStr
                                        dependentsStr = item.dependentsStr
                                        pranaySethiSteps = item.pranaySethiSteps
                                        tempInjuryCompStr = item.tempInjuryCompStr
                                        lossOfIncomeMonthsStr = item.lossOfIncomeMonthsStr
                                        medicalStr = item.medicalStr
                                        futureMedicalStr = item.futureMedicalStr
                                        painSufferingStr = item.painSufferingStr
                                        transportStr = item.transportStr
                                        nourishmentStr = item.nourishmentStr
                                        attendantStr = item.attendantStr
                                        clothingDamageStr = item.clothingDamageStr
                                        amenitiesStr = item.amenitiesStr
                                        prospectsStr = item.prospectsStr
                                        interestRateStr = item.interestRateStr
                                        petitionDateLabel = item.petitionDateLabel
                                        daysStr = item.daysStr
                                        customOfferStr = item.customOfferStr
                                        showSavedCasesDialog = false
                                        Toast.makeText(context, "Loaded MCOP ${item.mcopNumber}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "MCOP ${item.mcopNumber} • ${item.claimantName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryNavy
                                        )
                                        Text(
                                            text = "${item.claimModeName} | Net Award: ${formatInr(item.netAwardPreview.toDouble())}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentGreen
                                        )
                                        Text(
                                            text = "Saved: ${item.savedAt}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(onClick = {
                                        savedCasesList = deleteCaseFromPrefs(context, item.id)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedCasesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private const val PREFS_NAME = "mcop_saved_cases_prefs"
private const val KEY_CASES_JSON = "saved_cases_json"

fun saveCaseToPrefs(context: Context, newCase: SavedMcopCase): List<SavedMcopCase> {
    val existing = loadAllSavedCases(context).toMutableList()
    val index = existing.indexOfFirst { it.id.equals(newCase.id, ignoreCase = true) }
    if (index >= 0) {
        existing[index] = newCase
    } else {
        existing.add(0, newCase)
    }
    persistCasesList(context, existing)
    return existing
}

fun deleteCaseFromPrefs(context: Context, id: String): List<SavedMcopCase> {
    val existing = loadAllSavedCases(context).filterNot { it.id.equals(id, ignoreCase = true) }
    persistCasesList(context, existing)
    return existing
}

private fun persistCasesList(context: Context, list: List<SavedMcopCase>) {
    val arr = JSONArray()
    list.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("mcopNumber", item.mcopNumber)
            put("claimantName", item.claimantName)
            put("claimModeName", item.claimModeName)
            put("ageStr", item.ageStr)
            put("monthlyIncomeStr", item.monthlyIncomeStr)
            put("disabilityStr", item.disabilityStr)
            put("ratePerPctStr", item.ratePerPctStr)
            put("dependentsStr", item.dependentsStr)
            put("pranaySethiSteps", item.pranaySethiSteps)
            put("tempInjuryCompStr", item.tempInjuryCompStr)
            put("lossOfIncomeMonthsStr", item.lossOfIncomeMonthsStr)
            put("medicalStr", item.medicalStr)
            put("futureMedicalStr", item.futureMedicalStr)
            put("painSufferingStr", item.painSufferingStr)
            put("transportStr", item.transportStr)
            put("nourishmentStr", item.nourishmentStr)
            put("attendantStr", item.attendantStr)
            put("clothingDamageStr", item.clothingDamageStr)
            put("amenitiesStr", item.amenitiesStr)
            put("prospectsStr", item.prospectsStr)
            put("interestRateStr", item.interestRateStr)
            put("petitionDateLabel", item.petitionDateLabel)
            put("daysStr", item.daysStr)
            put("customOfferStr", item.customOfferStr)
            put("savedAt", item.savedAt)
            put("netAwardPreview", item.netAwardPreview)
        }
        arr.put(obj)
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CASES_JSON, arr.toString())
        .apply()
}

fun loadAllSavedCases(context: Context): List<SavedMcopCase> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CASES_JSON, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<SavedMcopCase>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SavedMcopCase(
                    id = obj.optString("id"),
                    mcopNumber = obj.optString("mcopNumber"),
                    claimantName = obj.optString("claimantName"),
                    claimModeName = obj.optString("claimModeName", ClaimMode.PARTIAL.name),
                    ageStr = obj.optString("ageStr", "34"),
                    monthlyIncomeStr = obj.optString("monthlyIncomeStr", "15000"),
                    disabilityStr = obj.optString("disabilityStr", "20"),
                    ratePerPctStr = obj.optString("ratePerPctStr", "7000"),
                    dependentsStr = obj.optString("dependentsStr", "3"),
                    pranaySethiSteps = obj.optInt("pranaySethiSteps", 2),
                    tempInjuryCompStr = obj.optString("tempInjuryCompStr", "35000"),
                    lossOfIncomeMonthsStr = obj.optString("lossOfIncomeMonthsStr", "3"),
                    medicalStr = obj.optString("medicalStr", "25000"),
                    futureMedicalStr = obj.optString("futureMedicalStr", "0"),
                    painSufferingStr = obj.optString("painSufferingStr", "40000"),
                    transportStr = obj.optString("transportStr", "10000"),
                    nourishmentStr = obj.optString("nourishmentStr", "15000"),
                    attendantStr = obj.optString("attendantStr", "15000"),
                    clothingDamageStr = obj.optString("clothingDamageStr", "3000"),
                    amenitiesStr = obj.optString("amenitiesStr", "25000"),
                    prospectsStr = obj.optString("prospectsStr", "40"),
                    interestRateStr = obj.optString("interestRateStr", "7.5"),
                    petitionDateLabel = obj.optString("petitionDateLabel", "09-05-2024"),
                    daysStr = obj.optString("daysStr", "914"),
                    customOfferStr = obj.optString("customOfferStr", ""),
                    savedAt = obj.optString("savedAt", ""),
                    netAwardPreview = obj.optLong("netAwardPreview", 0L)
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun formatExportText(
    mcopNo: String,
    name: String,
    age: Int,
    petitionDate: String,
    days: Long,
    interestRate: Double,
    mode: ClaimMode,
    result: McopCalculationResult,
    includeInterest: Boolean,
    includeLokAdalat: Boolean
): String {
    val sb = StringBuilder()
    sb.append("*MCOP CALCULATION & COMPROMISE MEMO (${mode.shortLabel.uppercase()})*\n")
    sb.append("----------------------------------------\n")
    sb.append("MCOP No: *$mcopNo*\n")
    sb.append("Claimant: *$name*\n")
    sb.append("Age / Multiplier: $age Yrs (Mult: ${result.multiplier})\n")
    sb.append("Net Annual Income: ${formatInr(result.netAnnualIncome)}\n\n")

    sb.append("*HEAD-WISE BIFURCATION:*\n")
    result.heads.forEachIndexed { index, head ->
        sb.append("${index + 1}. ${head.headName}: *${formatInr(head.amount)}*\n")
    }
    sb.append("----------------------------------------\n")
    sb.append("*NET PRINCIPAL AWARD: ${formatInr(result.netAwardAmount)}*\n")

    if (includeInterest) {
        sb.append("Interest (${interestRate}% from $petitionDate - $days days): +${formatInr(result.interestAmount)}\n")
        sb.append("*TOTAL PAYABLE TODAY: ${formatInr(result.totalPayableWithInterest)}*\n")
    }

    if (includeLokAdalat) {
        sb.append("========================================\n")
        sb.append("*LOK ADALAT COMPROMISE (CONSOLIDATED)*\n")
        sb.append("• Proposed Consolidated Offer: *${formatInr(result.compromiseOfferAmount)}* (${String.format(Locale.US, "%.1f", result.compromisePercentageOfTotal)}%)\n")
        sb.append("• Max NPV Ceiling: *${formatInr(result.npvMaxCompromiseCeiling)}*\n")
        sb.append("_(Full & final consolidated settlement without separate interest)_")
    }

    return sb.toString()
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share MCOP Memo"))
}

fun exportPdfMemo(
    context: Context,
    mcopNo: String,
    name: String,
    age: Int,
    petitionDate: String,
    days: Long,
    interestRate: Double,
    mode: ClaimMode,
    result: McopCalculationResult,
    includeInterest: Boolean,
    includeLokAdalat: Boolean
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = android.graphics.Color.rgb(27, 79, 114)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = android.graphics.Color.DKGRAY
        }
        val amountBoldPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 42f
        canvas.drawText("BEFORE THE MOTOR ACCIDENTS CLAIMS TRIBUNAL / LOK ADALAT", 297.5f, y, titlePaint)
        y += 18f
        canvas.drawText("CALCULATION & BIFURCATION MEMO IN M.C.O.P. No. $mcopNo", 297.5f, y, titlePaint)
        y += 14f
        canvas.drawLine(45f, y, 550f, y, linePaint)

        y += 20f
        canvas.drawText("Claimant Name: $name", 45f, y, subHeaderPaint)
        canvas.drawText("Category: ${mode.label}", 310f, y, bodyPaint)
        y += 16f
        canvas.drawText("Age / Multiplier: $age Yrs (Multiplier: ${result.multiplier})", 45f, y, bodyPaint)
        canvas.drawText("Annual Income: ${formatInr(result.netAnnualIncome)}", 310f, y, bodyPaint)
        y += 14f
        canvas.drawLine(45f, y, 550f, y, linePaint)

        y += 20f
        canvas.drawText("S.No.", 45f, y, subHeaderPaint)
        canvas.drawText("HEAD OF COMPENSATION", 90f, y, subHeaderPaint)
        canvas.drawText("AMOUNT (INR)", 545f, y, amountBoldPaint)
        y += 10f
        canvas.drawLine(45f, y, 550f, y, linePaint)

        result.heads.forEachIndexed { index, head ->
            y += 18f
            canvas.drawText("${index + 1}.", 45f, y, bodyPaint)
            canvas.drawText(head.headName, 90f, y, bodyPaint)
            canvas.drawText(formatInr(head.amount), 545f, y, amountBoldPaint)
        }

        y += 12f
        canvas.drawLine(45f, y, 550f, y, linePaint)

        y += 20f
        canvas.drawText("NET PRINCIPAL AWARD AMOUNT", 45f, y, subHeaderPaint)
        canvas.drawText(formatInr(result.netAwardAmount), 545f, y, amountBoldPaint)

        if (includeInterest) {
            y += 18f
            canvas.drawText("Interest @ ${interestRate}% p.a. (from $petitionDate for $days days)", 45f, y, bodyPaint)
            canvas.drawText("+ ${formatInr(result.interestAmount)}", 545f, y, amountBoldPaint)

            y += 12f
            canvas.drawLine(45f, y, 550f, y, linePaint)
            y += 20f
            canvas.drawText("TOTAL PAYABLE TODAY (WITH INTEREST)", 45f, y, subHeaderPaint)
            canvas.drawText(formatInr(result.totalPayableWithInterest), 545f, y, amountBoldPaint)
        }

        if (includeLokAdalat) {
            y += 24f
            canvas.drawLine(45f, y, 550f, y, linePaint)
            y += 20f
            canvas.drawText("LOK ADALAT COMPROMISE PROPOSAL (WITHOUT SEPARATE INTEREST)", 45f, y, subHeaderPaint)
            y += 18f
            canvas.drawText(
                "• Proposed Consolidated Settlement Offer (${String.format(Locale.US, "%.1f", result.compromisePercentageOfTotal)}%)",
                55f,
                y,
                bodyPaint
            )
            canvas.drawText(formatInr(result.compromiseOfferAmount), 545f, y, amountBoldPaint)

            y += 16f
            canvas.drawText("• Maximum Actuarial NPV Ceiling", 55f, y, bodyPaint)
            canvas.drawText(formatInr(result.npvMaxCompromiseCeiling), 545f, y, amountBoldPaint)
        }

        y = 795f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 15f
        canvas.drawText("Generated via MCOP Compensation Calculator", 45f, y, bodyPaint)

        pdfDocument.finishPage(page)

        val safeNo = mcopNo.replace("/", "_").replace(" ", "_")
        val file = File(context.cacheDir, "MCOP_Memo_$safeNo.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Calculation Memo"))
    } catch (e: Exception) {
        Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun exportCsvForSheets(
    context: Context,
    mcopNo: String,
    name: String,
    age: Int,
    mode: ClaimMode,
    result: McopCalculationResult,
    includeInterest: Boolean
) {
    try {
        val headsSummary = result.heads.joinToString(" | ") { "${it.headName}: ${it.amount.roundToLong()}" }
        val csvHeader = "MCOP_No,Claimant,Age,Mode,Net_Annual_Income,Multiplier,Head_Wise_Breakdown,Net_Principal_Award,Interest_Amount,Total_Payable,Lok_Adalat_Offer,NPV_Ceiling\n"
        val interestVal = if (includeInterest) result.interestAmount.roundToLong() else 0L
        val totalVal = if (includeInterest) result.totalPayableWithInterest.roundToLong() else result.netAwardAmount.roundToLong()
        val csvRow = listOf(
            mcopNo,
            name.replace(",", " "),
            age.toString(),
            mode.shortLabel,
            result.netAnnualIncome.roundToLong().toString(),
            result.multiplier.toString(),
            "\"$headsSummary\"",
            result.netAwardAmount.roundToLong().toString(),
            interestVal.toString(),
            totalVal.toString(),
            result.compromiseOfferAmount.roundToLong().toString(),
            result.npvMaxCompromiseCeiling.roundToLong().toString()
        ).joinToString(",") + "\n"

        val safeNo = mcopNo.replace("/", "_").replace(" ", "_")
        val file = File(context.cacheDir, "MCOP_$safeNo.csv")
        FileOutputStream(file).use { it.write((csvHeader + csvRow).toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV for Google Sheets"))
    } catch (e: Exception) {
        Toast.makeText(context, "CSV Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
