package com.kamarajlawoffice.mcopcalculator

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToLong

enum class ClaimMode(val label: String, val shortLabel: String) {
    FATAL("Fatal Accident", "Fatal"),
    PERMANENT("Permanent Disability (Multiplier)", "Perm %"),
    PARTIAL("Partial Disability (Per % Point)", "Partial %"),
    TEMPORARY("Simple / Temporary Injury", "Temp")
}

enum class AppScreen {
    CALCULATOR,
    SAVED_CASES
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

// Palette synchronized directly with the Royal Navy & Gold App Icon
val IconRoyalNavy = Color(0xFF154360)
val IconDeepNavy = Color(0xFF0E2F44)
val IconBadgeBlue = Color(0xFF2E86C1)
val IconGoldRing = Color(0xFFD4AC0D)
val IconGoldBright = Color(0xFFF4D03F)
val CreamBg = Color(0xFFF8F9FA)
val CardWarmBg = Color(0xFFFFFFFF)
val GoldTintBg = Color(0xFFFEF9E7)
val NavyTintBg = Color(0xFFEBF5FB)
val EmeraldGreen = Color(0xFF1E8449)
val CrimsonRed = Color(0xFFA93226)
val RoyalPurple = Color(0xFF6C3483)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = IconDeepNavy.toArgb()
                    window.navigationBarColor = IconDeepNavy.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                }
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = IconRoyalNavy,
                    onPrimary = Color.White,
                    primaryContainer = NavyTintBg,
                    onPrimaryContainer = IconRoyalNavy,
                    secondary = IconGoldRing,
                    onSecondary = IconDeepNavy,
                    tertiary = IconBadgeBlue,
                    background = CreamBg,
                    surface = CardWarmBg
                )
            ) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!showSplash) {
                        McopMainContainer()
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
            .background(IconRoyalNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .clip(CircleShape)
                    .border(3.dp, IconGoldRing, CircleShape)
                    .background(IconDeepNavy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Balance,
                    contentDescription = "App Logo",
                    tint = IconGoldBright,
                    modifier = Modifier.size(58.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
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
                text = "Motor Accident Claims & Actuarial Portal",
                color = IconGoldBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(30.dp))
            CircularProgressIndicator(
                color = IconGoldBright,
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

/**
 * Automatically parses dd-MM-yyyy or dd/MM/yyyy or yyyy-MM-dd and returns days up to today.
 */
fun computeDaysFromPetitionDate(dateStr: String): Long? {
    val cleaned = dateStr.trim()
    if (cleaned.length < 8) return null
    val patterns = listOf("dd-MM-yyyy", "dd/MM/yyyy", "d-M-yyyy", "d/M/yyyy", "yyyy-MM-dd")
    for (pattern in patterns) {
        try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
            val parsedDate = LocalDate.parse(cleaned, formatter)
            val today = LocalDate.now()
            val diff = ChronoUnit.DAYS.between(parsedDate, today)
            return diff.coerceAtLeast(0L)
        } catch (_: Exception) {
        }
    }
    return null
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

            val escalationFactor = (1.10).pow(pranaySethiSteps.coerceIn(0, 5))
            val perConsortium = (40000.0 * escalationFactor).roundToLong().toDouble()
            val estateAndFuneral = (15000.0 * escalationFactor).roundToLong().toDouble()
            val consortiumTotal = (dependentsCount.coerceAtLeast(1)) * perConsortium

            val stepLabel = if (pranaySethiSteps > 0) " [+${pranaySethiSteps}x10%]" else ""

            heads.add(HeadBifurcation("Dependency (M:$multiplier)", lossOfDependency))
            heads.add(HeadBifurcation("Consortium ($dependentsCount×₹${perConsortium.toInt()}$stepLabel)", consortiumTotal))
            heads.add(HeadBifurcation("Loss of Estate$stepLabel", estateAndFuneral))
            heads.add(HeadBifurcation("Funeral Exp.$stepLabel", estateAndFuneral))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transport / Ambulance", transportationCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Clothing & Articles", clothingDamage))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Pre-Death Medical", medicalExpenses))
        }
        ClaimMode.PERMANENT -> {
            val lossOfEarningCapacity = annualIncome * prospectsFactor * (disabilityPercent / 100.0) * multiplier
            heads.add(HeadBifurcation("Earning Loss (${disabilityPercent.toInt()}% • M:$multiplier)", lossOfEarningCapacity))
            if (lossOfIncomeMonths > 0 && monthlyIncome > 0) {
                heads.add(HeadBifurcation("Treatment Income Loss (${lossOfIncomeMonths.toInt()}m)", monthlyIncome * lossOfIncomeMonths))
            }
            if (painAndSuffering > 0) heads.add(HeadBifurcation("Pain & Suffering", painAndSuffering))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Medical Bills", medicalExpenses))
            if (futureMedicalExpenses > 0) heads.add(HeadBifurcation("Future Medical", futureMedicalExpenses))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transportation", transportationCharges))
            if (extraNourishment > 0) heads.add(HeadBifurcation("Extra Nourishment", extraNourishment))
            if (attendantCharges > 0) heads.add(HeadBifurcation("Attendant Charges", attendantCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Clothing Damage", clothingDamage))
            if (lossOfAmenities > 0) heads.add(HeadBifurcation("Loss of Amenities", lossOfAmenities))
        }
        ClaimMode.PARTIAL -> {
            val disabilityAward = disabilityPercent * ratePerPercentage
            heads.add(HeadBifurcation("Disability (${disabilityPercent.toInt()}% × ₹${ratePerPercentage.toInt()})", disabilityAward))
            if (lossOfIncomeMonths > 0 && monthlyIncome > 0) {
                heads.add(HeadBifurcation("Treatment Income Loss (${lossOfIncomeMonths.toInt()}m)", monthlyIncome * lossOfIncomeMonths))
            }
            if (painAndSuffering > 0) heads.add(HeadBifurcation("Pain & Suffering", painAndSuffering))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Medical Bills", medicalExpenses))
            if (futureMedicalExpenses > 0) heads.add(HeadBifurcation("Future Medical", futureMedicalExpenses))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transportation", transportationCharges))
            if (extraNourishment > 0) heads.add(HeadBifurcation("Extra Nourishment", extraNourishment))
            if (attendantCharges > 0) heads.add(HeadBifurcation("Attendant Charges", attendantCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Clothing Damage", clothingDamage))
            if (lossOfAmenities > 0) heads.add(HeadBifurcation("Loss of Amenities", lossOfAmenities))
        }
        ClaimMode.TEMPORARY -> {
            if (tempInjuryComp > 0) heads.add(HeadBifurcation("Injury Compensation", tempInjuryComp))
            if (lossOfIncomeMonths > 0 && monthlyIncome > 0) {
                heads.add(HeadBifurcation("Loss of Income (${lossOfIncomeMonths.toInt()}m)", monthlyIncome * lossOfIncomeMonths))
            }
            if (painAndSuffering > 0) heads.add(HeadBifurcation("Pain & Suffering", painAndSuffering))
            if (medicalExpenses > 0) heads.add(HeadBifurcation("Medical Bills", medicalExpenses))
            if (transportationCharges > 0) heads.add(HeadBifurcation("Transportation", transportationCharges))
            if (extraNourishment > 0) heads.add(HeadBifurcation("Extra Nourishment", extraNourishment))
            if (attendantCharges > 0) heads.add(HeadBifurcation("Attendant Charges", attendantCharges))
            if (clothingDamage > 0) heads.add(HeadBifurcation("Clothing Damage", clothingDamage))
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

/**
 * Compact, high-density single-line input field that automatically advances focus on Enter/Next.
 */
@Composable
fun CompactInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isNumber: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = IconRoyalNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(1.dp, IconRoyalNavy.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { newVal ->
                        if (newVal.contains('\n')) {
                            if (imeAction == ImeAction.Done) focusManager.clearFocus()
                            else focusManager.moveFocus(FocusDirection.Next)
                        } else {
                            onValueChange(newVal)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IconDeepNavy
                    ),
                    cursorBrush = SolidColor(IconRoyalNavy),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isNumber) KeyboardType.Decimal else KeyboardType.Text,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McopMainContainer() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.CALCULATOR) }

    // Core Case State
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
    var daysStr by remember {
        val initialDays = computeDaysFromPetitionDate("09-05-2024")?.toString() ?: "914"
        mutableStateOf(initialDays)
    }
    var customOfferStr by remember { mutableStateOf("") }

    var selectedMode by remember { mutableStateOf(ClaimMode.PARTIAL) }
    var activeInputTab by remember { mutableStateOf(0) } // 0 = Case & Primary, 1 = Conventional Heads, 2 = Interest & Compromise
    var includeInterestInExport by remember { mutableStateOf(true) }
    var includeLokAdalatInExport by remember { mutableStateOf(true) }

    var savedCasesList by remember { mutableStateOf(loadAllSavedCases(context)) }

    // Automatically recalculate days whenever petitionDateLabel changes
    LaunchedEffect(petitionDateLabel) {
        computeDaysFromPetitionDate(petitionDateLabel)?.let { calculatedDays ->
            daysStr = calculatedDays.toString()
        }
    }

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
    val days = daysStr.toLongOrNull() ?: 0L
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

    val saveCurrentCase: () -> Unit = {
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
    }

    if (currentScreen == AppScreen.SAVED_CASES) {
        BackHandler { currentScreen = AppScreen.CALCULATOR }
        SavedCasesPage(
            savedCasesList = savedCasesList,
            onBack = { currentScreen = AppScreen.CALCULATOR },
            onSelectCase = { item ->
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
                daysStr = computeDaysFromPetitionDate(item.petitionDateLabel)?.toString() ?: item.daysStr
                customOfferStr = item.customOfferStr
                currentScreen = AppScreen.CALCULATOR
                Toast.makeText(context, "Loaded MCOP ${item.mcopNumber}", Toast.LENGTH_SHORT).show()
            },
            onDeleteCase = { id ->
                savedCasesList = deleteCaseFromPrefs(context, id)
            }
        )
    } else {
        Scaffold(
            topBar = {
                Surface(
                    color = IconRoyalNavy,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, IconGoldRing, CircleShape)
                                    .background(IconDeepNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Balance,
                                    contentDescription = null,
                                    tint = IconGoldBright,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MCOP Compensation Calculator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Motor Accident Claims Portal • Today: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}",
                                    fontSize = 10.sp,
                                    color = IconGoldBright
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = IconDeepNavy,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, IconGoldRing),
                                modifier = Modifier.clickable { saveCurrentCase() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", tint = IconGoldBright, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = IconGoldRing,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    savedCasesList = loadAllSavedCases(context)
                                    currentScreen = AppScreen.SAVED_CASES
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Vault", tint = IconDeepNavy, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cases (${savedCasesList.size})", color = IconDeepNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            // Single-Viewport Non-Scrolling Dashboard Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(CreamBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Compact Claim Mode Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IconDeepNavy)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ClaimMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) IconGoldRing else Color.Transparent)
                                .clickable { selectedMode = mode }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.shortLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) IconDeepNavy else Color.White
                            )
                        }
                    }
                }

                // 2. Tabbed Input Workbench (Fixed Height, Zero Vertical Scrolling)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWarmBg),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, IconRoyalNavy.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Input Section Switcher Tabs
                        val tabTitles = listOf("1. Case & Primary", "2. Conventional Heads", "3. Interest & Lok Adalat")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(NavyTintBg)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tabTitles.forEachIndexed { idx, title ->
                                val active = activeInputTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (active) IconRoyalNavy else Color.Transparent)
                                        .clickable { activeInputTab = idx }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) IconGoldBright else IconRoyalNavy,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Tab 0: Case & Primary Inputs
                        if (activeInputTab == 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CompactInputBox(
                                        value = mcopNumber,
                                        onValueChange = { mcopNumber = it },
                                        label = "MCOP No.",
                                        isNumber = false,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    CompactInputBox(
                                        value = claimantName,
                                        onValueChange = { claimantName = it },
                                        label = "Claimant Name",
                                        isNumber = false,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    CompactInputBox(
                                        value = ageStr,
                                        onValueChange = { ageStr = it },
                                        label = "Age (M:${result.multiplier})",
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }

                                when (selectedMode) {
                                    ClaimMode.FATAL -> {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CompactInputBox(
                                                value = monthlyIncomeStr,
                                                onValueChange = { monthlyIncomeStr = it },
                                                label = "Monthly Income ₹",
                                                modifier = Modifier.weight(1.1f)
                                            )
                                            CompactInputBox(
                                                value = dependentsStr,
                                                onValueChange = { dependentsStr = it },
                                                label = "Dependents",
                                                modifier = Modifier.weight(0.8f)
                                            )
                                            CompactInputBox(
                                                value = prospectsStr,
                                                onValueChange = { prospectsStr = it },
                                                label = "Future Prosp. %",
                                                imeAction = ImeAction.Done,
                                                modifier = Modifier.weight(0.9f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Pranay 10% Step:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IconRoyalNavy)
                                            val steps = listOf("Base 40k", "+10% 44k", "+21% 48.4k", "+33% 53.2k")
                                            steps.forEachIndexed { idx, sLabel ->
                                                val sel = pranaySethiSteps == idx
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(if (sel) IconGoldRing else NavyTintBg)
                                                        .clickable { pranaySethiSteps = idx }
                                                        .padding(vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(sLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IconDeepNavy)
                                                }
                                            }
                                        }
                                    }
                                    ClaimMode.PERMANENT -> {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CompactInputBox(
                                                value = monthlyIncomeStr,
                                                onValueChange = { monthlyIncomeStr = it },
                                                label = "Monthly Income ₹",
                                                modifier = Modifier.weight(1f)
                                            )
                                            CompactInputBox(
                                                value = disabilityStr,
                                                onValueChange = { disabilityStr = it },
                                                label = "Functional Disab %",
                                                modifier = Modifier.weight(1f)
                                            )
                                            CompactInputBox(
                                                value = prospectsStr,
                                                onValueChange = { prospectsStr = it },
                                                label = "Future Prosp %",
                                                imeAction = ImeAction.Done,
                                                modifier = Modifier.weight(0.9f)
                                            )
                                        }
                                    }
                                    ClaimMode.PARTIAL -> {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CompactInputBox(
                                                value = monthlyIncomeStr,
                                                onValueChange = { monthlyIncomeStr = it },
                                                label = "Monthly Income ₹",
                                                modifier = Modifier.weight(1f)
                                            )
                                            CompactInputBox(
                                                value = disabilityStr,
                                                onValueChange = { disabilityStr = it },
                                                label = "Disability %",
                                                modifier = Modifier.weight(0.8f)
                                            )
                                            CompactInputBox(
                                                value = ratePerPctStr,
                                                onValueChange = { ratePerPctStr = it },
                                                label = "Rate per % (₹)",
                                                imeAction = ImeAction.Done,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    ClaimMode.TEMPORARY -> {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CompactInputBox(
                                                value = tempInjuryCompStr,
                                                onValueChange = { tempInjuryCompStr = it },
                                                label = "Injury Compensation ₹",
                                                modifier = Modifier.weight(1.2f)
                                            )
                                            CompactInputBox(
                                                value = monthlyIncomeStr,
                                                onValueChange = { monthlyIncomeStr = it },
                                                label = "Monthly Income ₹",
                                                modifier = Modifier.weight(1f)
                                            )
                                            CompactInputBox(
                                                value = lossOfIncomeMonthsStr,
                                                onValueChange = { lossOfIncomeMonthsStr = it },
                                                label = "Loss Months",
                                                imeAction = ImeAction.Done,
                                                modifier = Modifier.weight(0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tab 1: Conventional & Pecuniary Heads
                        if (activeInputTab == 1) {
                            if (selectedMode == ClaimMode.FATAL) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CompactInputBox(
                                        value = transportStr,
                                        onValueChange = { transportStr = it },
                                        label = "Transport / Ambulance ₹",
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactInputBox(
                                        value = medicalStr,
                                        onValueChange = { medicalStr = it },
                                        label = "Pre-Death Medical ₹",
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactInputBox(
                                        value = clothingDamageStr,
                                        onValueChange = { clothingDamageStr = it },
                                        label = "Clothing & Articles ₹",
                                        imeAction = ImeAction.Done,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CompactInputBox(
                                            value = painSufferingStr,
                                            onValueChange = { painSufferingStr = it },
                                            label = "Pain & Suffering ₹",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactInputBox(
                                            value = medicalStr,
                                            onValueChange = { medicalStr = it },
                                            label = "Medical Bills ₹",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactInputBox(
                                            value = futureMedicalStr,
                                            onValueChange = { futureMedicalStr = it },
                                            label = "Future Medical ₹",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactInputBox(
                                            value = lossOfIncomeMonthsStr,
                                            onValueChange = { lossOfIncomeMonthsStr = it },
                                            label = "Loss Months",
                                            modifier = Modifier.weight(0.75f)
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CompactInputBox(
                                            value = transportStr,
                                            onValueChange = { transportStr = it },
                                            label = "Transportation ₹",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactInputBox(
                                            value = nourishmentStr,
                                            onValueChange = { nourishmentStr = it },
                                            label = "Nourishment ₹",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactInputBox(
                                            value = attendantStr,
                                            onValueChange = { attendantStr = it },
                                            label = "Attendant ₹",
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactInputBox(
                                            value = amenitiesStr,
                                            onValueChange = { amenitiesStr = it },
                                            label = "Amenities ₹",
                                            imeAction = ImeAction.Done,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Tab 2: Automatic Petition Date -> Days & Lok Adalat Controls
                        if (activeInputTab == 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CompactInputBox(
                                        value = petitionDateLabel,
                                        onValueChange = {
                                            petitionDateLabel = it
                                            computeDaysFromPetitionDate(it)?.let { d -> daysStr = d.toString() }
                                        },
                                        label = "Petition Date (DD-MM-YYYY)",
                                        isNumber = false,
                                        modifier = Modifier.weight(1.3f),
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Pick Date",
                                                tint = IconRoyalNavy,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val cal = Calendar.getInstance()
                                                        DatePickerDialog(
                                                            context,
                                                            { _, y, m, d ->
                                                                val formatted = String.format(Locale.US, "%02d-%02d-%04d", d, m + 1, y)
                                                                petitionDateLabel = formatted
                                                                computeDaysFromPetitionDate(formatted)?.let { calc ->
                                                                    daysStr = calc.toString()
                                                                }
                                                            },
                                                            cal.get(Calendar.YEAR),
                                                            cal.get(Calendar.MONTH),
                                                            cal.get(Calendar.DAY_OF_MONTH)
                                                        ).show()
                                                    }
                                            )
                                        }
                                    )
                                    CompactInputBox(
                                        value = daysStr,
                                        onValueChange = { daysStr = it },
                                        label = "Auto Days (Till Today)",
                                        modifier = Modifier.weight(0.95f)
                                    )
                                    CompactInputBox(
                                        value = interestRateStr,
                                        onValueChange = { interestRateStr = it },
                                        label = "Interest %",
                                        modifier = Modifier.weight(0.75f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompactInputBox(
                                        value = customOfferStr,
                                        onValueChange = { customOfferStr = it },
                                        label = "Custom Lok Adalat Offer ₹ (Blank = 85%)",
                                        imeAction = ImeAction.Done,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NavyTintBg)
                                            .clickable { includeInterestInExport = !includeInterestInExport }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = includeInterestInExport,
                                            onCheckedChange = { includeInterestInExport = it },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Int.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IconDeepNavy)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldTintBg)
                                            .clickable { includeLokAdalatInExport = !includeLokAdalatInExport }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = includeLokAdalatInExport,
                                            onCheckedChange = { includeLokAdalatInExport = it },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Lok Adalat", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IconDeepNavy)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Executive Live Head-Wise Bifurcation & Summary Viewport (Fills Remaining Screen Space)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, IconGoldRing.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header Strip
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "HEAD-WISE BIFURCATION",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IconRoyalNavy,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${selectedMode.shortLabel} • $mcopNumber • $claimantName)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IconBadgeBlue
                                    )
                                }
                                Surface(
                                    color = IconRoyalNavy,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Ann. Inc: ${formatInr(result.netAnnualIncome)} | M:${result.multiplier}",
                                        color = IconGoldBright,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = IconRoyalNavy.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(4.dp))

                            // Two-Column Compact Grid for All Active Heads (Fits up to 10 Heads Without Scrolling)
                            val chunkedHeads = result.heads.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                chunkedHeads.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { head ->
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(NavyTintBg.copy(alpha = 0.6f))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = head.headName,
                                                    fontSize = 10.sp,
                                                    color = IconDeepNavy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = formatInr(head.amount),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IconRoyalNavy
                                                )
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Financial Metrics Strip (Principal, Interest, Total, Lok Adalat)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HorizontalDivider(color = IconRoyalNavy.copy(alpha = 0.15f))

                            // Row 1: Principal + Interest + Total Payable KPI Boxes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Net Principal Box
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NavyTintBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("NET PRINCIPAL AWARD", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = IconRoyalNavy)
                                    Text(formatInr(result.netAwardAmount), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = IconDeepNavy)
                                }

                                if (includeInterestInExport) {
                                    // Interest Box
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE8F8F5))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("INT (${interestRate}% • $days d)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                        Text("+ ${formatInr(result.interestAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    }

                                    // Total Payable Today Box
                                    Column(
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFDEDEC))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("TOTAL PAYABLE TODAY", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                                        Text(formatInr(result.totalPayableWithInterest), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = CrimsonRed)
                                    }
                                }
                            }

                            // Row 2: Lok Adalat Compromise Banner
                            if (includeLokAdalatInExport) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldTintBg)
                                        .border(1.dp, IconGoldRing.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "LOK ADALAT COMPROMISE (CONSOLIDATED • NO INT)",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = IconDeepNavy
                                        )
                                        Text(
                                            text = "Proposed Offer (${String.format(Locale.US, "%.1f", result.compromisePercentageOfTotal)}%): ${formatInr(result.compromiseOfferAmount)}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalPurple
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Max NPV Ceiling", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = IconRoyalNavy)
                                        Text(formatInr(result.npvMaxCompromiseCeiling), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = IconRoyalNavy)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Bottom Action Bar (Fixed at Bottom of Viewport)
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
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IconRoyalNavy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = IconGoldBright, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp Memo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            exportPdfMemo(
                                context, mcopNumber, claimantName, age, petitionDateLabel, days,
                                interestRate, selectedMode, result,
                                includeInterestInExport, includeLokAdalatInExport
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IconBadgeBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Memo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            exportCsvForSheets(
                                context, mcopNumber, claimantName, age, selectedMode,
                                result, includeInterestInExport
                            )
                        },
                        modifier = Modifier
                            .weight(0.75f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IconRoyalNavy),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = IconRoyalNavy, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = IconRoyalNavy)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Full-Screen Page to View, Search, Load, and Delete Saved MCOP Cases.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCasesPage(
    savedCasesList: List<SavedMcopCase>,
    onBack: () -> Unit,
    onSelectCase: (SavedMcopCase) -> Unit,
    onDeleteCase: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            Surface(color = IconRoyalNavy, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = IconGoldBright)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Saved MCOP Cases Directory",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${savedCasesList.size} Total Records Stored on Device",
                                fontSize = 11.sp,
                                color = IconGoldBright
                            )
                        }
                    }

                    Surface(
                        color = IconGoldRing,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onBack() }
                    ) {
                        Text(
                            text = "New / Calculator",
                            color = IconDeepNavy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CreamBg)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by MCOP Number, Claimant Name, or Category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = IconRoyalNavy) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No saved MCOP cases found.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        val autoDays = computeDaysFromPetitionDate(item.petitionDateLabel) ?: (item.daysStr.toLongOrNull() ?: 0L)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, IconRoyalNavy.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCase(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = IconRoyalNavy,
                                            shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text(
                                                text = "MCOP ${item.mcopNumber}",
                                                color = IconGoldBright,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.claimantName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = IconDeepNavy
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Category: ${item.claimModeName} • Age: ${item.ageStr} Yrs • Income: ₹${item.monthlyIncomeStr}/m",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Net Principal Award: ${formatInr(item.netAwardPreview.toDouble())} • Petition: ${item.petitionDateLabel} ($autoDays days)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                    Text(
                                        text = "Last Saved: ${item.savedAt}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onSelectCase(item) }) {
                                        Icon(Icons.Default.EditNote, contentDescription = "Open Case", tint = IconRoyalNavy)
                                    }
                                    IconButton(onClick = { onDeleteCase(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Case", tint = CrimsonRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            color = android.graphics.Color.rgb(21, 67, 96)
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
