package com.kamarajlawoffice.mcopcalculator

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1B4F72),
                    secondary = Color(0xFF2874A6),
                    tertiary = Color(0xFF117864),
                    surfaceVariant = Color(0xFFEBF5FB)
                )
            ) {
                McopCalculatorScreen()
            }
        }
    }
}

enum class AppTab(val title: String) {
    CALCULATION_SHEET("1. MCOP Sheet"),
    LOK_ADALAT_COMPROMISE("2. Lok Adalat Compromise")
}

enum class ClaimMode(val label: String) {
    TEMPORARY("Temp"),
    PARTIAL_PERCENT("Partial %"),
    PERMANENT_MULTIPLIER("Permanent"),
    FATAL_DEATH("Fatal")
}

enum class OccupationType(val label: String) {
    SELF_EMPLOYED("Self-Employed / Fixed"),
    SALARIED("Salaried (Permanent)")
}

enum class MaritalDeductionCategory(val label: String) {
    MARRIED_FAMILY("Married / Family"),
    BACHELOR_HALF("Bachelor (1/2 Ded.)"),
    BACHELOR_ONE_THIRD("Bachelor (1/3 Ded.)")
}

data class McopInputState(
    val mcopNumber: String = "412/2025",
    val claimantName: String = "",
    val age: String = "34",
    val monthlyIncome: String = "15000",
    val annualIncomeTax: String = "0",
    val occupationType: OccupationType = OccupationType.SELF_EMPLOYED,
    val claimMode: ClaimMode = ClaimMode.FATAL_DEATH,
    val maritalCategory: MaritalDeductionCategory = MaritalDeductionCategory.MARRIED_FAMILY,
    val numberOfDependents: String = "3",
    val consortiumDependentsCount: String = "3",
    val consortiumRatePerHead: String = "48000",
    val lossOfEstate: String = "18000",
    val funeralExpenses: String = "18000",
    val medicalBoardPercentage: String = "40",
    val ratePerPercentage: String = "5000",
    val functionalDisabilityPercentage: String = "40",
    val includeFutureProspects: Boolean = true,
    val treatmentRestMonths: String = "4",
    val medicalBills: String = "0",
    val painAndSuffering: String = "50000",
    val transportAndNourishment: String = "15000",
    val attenderCharges: String = "10000",
    val lossOfAmenities: String = "40000",
    val damageToClothing: String = "2000",
    val futureMedicalExpenses: String = "0",
    val lossOfMarriageProspects: String = "0",
    val contributoryNegligencePercent: String = "0",
    val negligenceNote: String = "No Helmet / Contributory",
    val petitionDate: LocalDate = LocalDate.now().minusYears(2).minusMonths(6),
    val calculationDate: LocalDate = LocalDate.now(),
    val excludedDelayDays: String = "0",
    val interestRatePercent: String = "7.5",
    val excludeFutureMedicalFromInterest: Boolean = true,
    val compromiseOfferInput: String = "2200000",
    val expectedTrialYears: String = "5.0",
    val trialAssessmentHaircutPercent: String = "10",
    val discountOrFdRatePercent: String = "7.0",
    val futureLitigationCost: String = "35000"
)

data class McopCalculationResult(
    val netAnnualIncome: Double,
    val multiplier: Int,
    val futureProspectsPercent: Double,
    val annualIncomeWithFutureProspects: Double,
    val deductionFractionLabel: String,
    val personalLivingDeductionAmount: Double,
    val annualMultiplicand: Double,
    val lossOfDependencyTotal: Double,
    val totalConsortiumAmount: Double,
    val totalConventionalHeads: Double,
    val disabilityOrFutureEarningLoss: Double,
    val lossOfIncomeDuringTreatment: Double,
    val grossCompensation: Double,
    val negligenceDeductionAmount: Double,
    val netAwardAmount: Double,
    val totalInterestDays: Long,
    val formattedDuration: String,
    val interestBearingPrincipal: Double,
    val interestAmount: Double,
    val totalPayableWithInterest: Double,
    val compromiseOfferAmount: Double,
    val immediateDiscountSaved: Double,
    val settlementRatioToTodayTotalPercent: Double,
    val settlementRatioToPrincipalPercent: Double,
    val contestedPrincipalAfterHaircut: Double,
    val totalContestedInterestDays: Long,
    val projectedContestedInterestAmount: Double,
    val projectedContestedTotalOutflow: Double,
    val npvMaxCompromiseCeiling: Double,
    val npvNetSavingsVsContested: Double,
    val nominalFutureSavings: Double,
    val claimantOfferFutureFdValue: Double,
    val claimantFdAdvantageVsTrial: Double,
    val isOfferFavorableForSettlement: Boolean
)

object McopJudicialEngine {
    fun getSarlaVermaMultiplier(age: Int): Int = when {
        age <= 15 -> 15
        age in 16..25 -> 18
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

    fun getFutureProspectsPercentage(age: Int, occupation: OccupationType): Double = when (occupation) {
        OccupationType.SALARIED -> when {
            age < 40 -> 50.0
            age in 40..50 -> 30.0
            age in 51..60 -> 15.0
            else -> 0.0
        }
        OccupationType.SELF_EMPLOYED -> when {
            age < 40 -> 40.0
            age in 40..50 -> 25.0
            age in 51..60 -> 10.0
            else -> 0.0
        }
    }

    fun getPersonalDeductionRatio(category: MaritalDeductionCategory, dependents: Int): Pair<Double, String> = when (category) {
        MaritalDeductionCategory.BACHELOR_HALF -> Pair(0.50, "1/2 (50% - Bachelor)")
        MaritalDeductionCategory.BACHELOR_ONE_THIRD -> Pair(1.0 / 3.0, "1/3 (Large Family Bachelor)")
        MaritalDeductionCategory.MARRIED_FAMILY -> when {
            dependents <= 3 -> Pair(1.0 / 3.0, "1/3 (2 to 3 Dependents)")
            dependents in 4..6 -> Pair(0.25, "1/4 (4 to 6 Dependents)")
            else -> Pair(0.20, "1/5 (>6 Dependents)")
        }
    }

    fun calculate(state: McopInputState): McopCalculationResult {
        val age = state.age.toIntOrNull() ?: 30
        val monthlyIncome = state.monthlyIncome.toDoubleOrNull() ?: 0.0
        val annualTax = state.annualIncomeTax.toDoubleOrNull() ?: 0.0
        val restMonths = state.treatmentRestMonths.toDoubleOrNull() ?: 0.0

        val netAnnualIncome = max(0.0, (monthlyIncome * 12.0) - annualTax)
        val multiplier = getSarlaVermaMultiplier(age)

        val fpEnabled = state.claimMode == ClaimMode.FATAL_DEATH || state.includeFutureProspects
        val fpPercent = if (fpEnabled) getFutureProspectsPercentage(age, state.occupationType) else 0.0

        val annualWithFp = netAnnualIncome * (1.0 + (fpPercent / 100.0))

        val dependentsCount = state.numberOfDependents.toIntOrNull() ?: 3
        val (deductionRatio, deductionLabel) = getPersonalDeductionRatio(state.maritalCategory, dependentsCount)
        val personalDeductionAmt = if (state.claimMode == ClaimMode.FATAL_DEATH) annualWithFp * deductionRatio else 0.0
        val annualMultiplicand = annualWithFp - personalDeductionAmt
        val lossOfDependency = if (state.claimMode == ClaimMode.FATAL_DEATH) annualMultiplicand * multiplier else 0.0

        val consortiumHeads = state.consortiumDependentsCount.toIntOrNull() ?: 0
        val consortiumRate = state.consortiumRatePerHead.toDoubleOrNull() ?: 40000.0
        val totalConsortium = if (state.claimMode == ClaimMode.FATAL_DEATH) consortiumHeads * consortiumRate else 0.0

        val estate = if (state.claimMode == ClaimMode.FATAL_DEATH) (state.lossOfEstate.toDoubleOrNull() ?: 0.0) else 0.0
        val funeral = if (state.claimMode == ClaimMode.FATAL_DEATH) (state.funeralExpenses.toDoubleOrNull() ?: 0.0) else 0.0
        val totalConventional = totalConsortium + estate + funeral

        val lossDuringTreatment = if (state.claimMode == ClaimMode.FATAL_DEATH) 0.0 else (monthlyIncome * restMonths)
        val disabilityLoss = when (state.claimMode) {
            ClaimMode.TEMPORARY, ClaimMode.FATAL_DEATH -> 0.0
            ClaimMode.PARTIAL_PERCENT -> {
                val medPct = state.medicalBoardPercentage.toDoubleOrNull() ?: 0.0
                val rate = state.ratePerPercentage.toDoubleOrNull() ?: 0.0
                medPct * rate
            }
            ClaimMode.PERMANENT_MULTIPLIER -> {
                val funcPct = state.functionalDisabilityPercentage.toDoubleOrNull() ?: 0.0
                annualWithFp * (funcPct / 100.0) * multiplier
            }
        }

        val medical = state.medicalBills.toDoubleOrNull() ?: 0.0
        val transport = state.transportAndNourishment.toDoubleOrNull() ?: 0.0
        val clothing = state.damageToClothing.toDoubleOrNull() ?: 0.0
        val pain = if (state.claimMode == ClaimMode.FATAL_DEATH) 0.0 else (state.painAndSuffering.toDoubleOrNull() ?: 0.0)
        val attender = if (state.claimMode == ClaimMode.FATAL_DEATH) 0.0 else (state.attenderCharges.toDoubleOrNull() ?: 0.0)
        val amenities = if (state.claimMode == ClaimMode.PARTIAL_PERCENT || state.claimMode == ClaimMode.PERMANENT_MULTIPLIER) state.lossOfAmenities.toDoubleOrNull() ?: 0.0 else 0.0
        val futureMed = if (state.claimMode == ClaimMode.PERMANENT_MULTIPLIER) state.futureMedicalExpenses.toDoubleOrNull() ?: 0.0 else 0.0
        val marriage = if (state.claimMode == ClaimMode.PERMANENT_MULTIPLIER) state.lossOfMarriageProspects.toDoubleOrNull() ?: 0.0 else 0.0

        val gross = if (state.claimMode == ClaimMode.FATAL_DEATH) {
            lossOfDependency + totalConventional + medical + transport + clothing
        } else {
            disabilityLoss + lossDuringTreatment + medical + pain + transport + attender + amenities + clothing + futureMed + marriage
        }

        val negPct = (state.contributoryNegligencePercent.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
        val negDeduction = gross * (negPct / 100.0)
        val netAward = gross - negDeduction

        val rawDays = ChronoUnit.DAYS.between(state.petitionDate, state.calculationDate)
        val excludedDays = state.excludedDelayDays.toLongOrNull() ?: 0L
        val netInterestDays = max(0L, rawDays - excludedDays)

        val period = if (!state.calculationDate.isBefore(state.petitionDate)) Period.between(state.petitionDate, state.calculationDate) else Period.ZERO
        val durationStr = "${period.years} Yrs, ${period.months} Mos, ${period.days} Days"

        val effectiveFutureMedInNet = futureMed * (1.0 - (negPct / 100.0))
        val interestPrincipal = if (state.claimMode == ClaimMode.PERMANENT_MULTIPLIER && state.excludeFutureMedicalFromInterest) max(0.0, netAward - effectiveFutureMedInNet) else netAward

        val interestRate = state.interestRatePercent.toDoubleOrNull() ?: 7.5
        val interestAmount = interestPrincipal * (interestRate / 100.0) * (netInterestDays / 365.0)
        val totalPayableToday = netAward + interestAmount

        val offerAmount = state.compromiseOfferInput.toDoubleOrNull() ?: 0.0
        val immediateSaved = totalPayableToday - offerAmount
        val ratioToTodayTotal = if (totalPayableToday > 0.0) (offerAmount / totalPayableToday) * 100.0 else 0.0
        val ratioToPrincipal = if (netAward > 0.0) (offerAmount / netAward) * 100.0 else 0.0

        val trialYears = (state.expectedTrialYears.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0)
        val haircutPct = (state.trialAssessmentHaircutPercent.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
        val discountRate = (state.discountOrFdRatePercent.toDoubleOrNull() ?: 7.0).coerceAtLeast(0.0)
        val legalCosts = state.futureLitigationCost.toDoubleOrNull() ?: 0.0

        val haircutMultiplier = 1.0 - (haircutPct / 100.0)
        val contestedPrincipal = netAward * haircutMultiplier
        val contestedInterestPrincipal = interestPrincipal * haircutMultiplier

        val futureTrialDays = (trialYears * 365.0).roundToLong()
        val totalContestedDays = netInterestDays + futureTrialDays
        val projectedContestedInterest = contestedInterestPrincipal * (interestRate / 100.0) * (totalContestedDays / 365.0)
        val projectedContestedTotalOutflow = contestedPrincipal + projectedContestedInterest + legalCosts

        val compoundFactor = (1.0 + (discountRate / 100.0)).pow(trialYears)
        val npvMaxCeiling = if (compoundFactor > 0.0) projectedContestedTotalOutflow / compoundFactor else projectedContestedTotalOutflow
        val npvNetSavings = npvMaxCeiling - offerAmount
        val nominalFutureSavings = projectedContestedTotalOutflow - offerAmount
        val claimantOfferFutureFdValue = offerAmount * compoundFactor
        val claimantFdAdvantage = claimantOfferFutureFdValue - (contestedPrincipal + projectedContestedInterest)
        val isFavorable = offerAmount <= npvMaxCeiling

        return McopCalculationResult(
            netAnnualIncome = netAnnualIncome,
            multiplier = multiplier,
            futureProspectsPercent = fpPercent,
            annualIncomeWithFutureProspects = annualWithFp,
            deductionFractionLabel = deductionLabel,
            personalLivingDeductionAmount = personalDeductionAmt,
            annualMultiplicand = annualMultiplicand,
            lossOfDependencyTotal = lossOfDependency,
            totalConsortiumAmount = totalConsortium,
            totalConventionalHeads = totalConventional,
            disabilityOrFutureEarningLoss = disabilityLoss,
            lossOfIncomeDuringTreatment = lossDuringTreatment,
            grossCompensation = gross,
            negligenceDeductionAmount = negDeduction,
            netAwardAmount = netAward,
            totalInterestDays = netInterestDays,
            formattedDuration = durationStr,
            interestBearingPrincipal = interestPrincipal,
            interestAmount = interestAmount,
            totalPayableWithInterest = totalPayableToday,
            compromiseOfferAmount = offerAmount,
            immediateDiscountSaved = immediateSaved,
            settlementRatioToTodayTotalPercent = ratioToTodayTotal,
            settlementRatioToPrincipalPercent = ratioToPrincipal,
            contestedPrincipalAfterHaircut = contestedPrincipal,
            totalContestedInterestDays = totalContestedDays,
            projectedContestedInterestAmount = projectedContestedInterest,
            projectedContestedTotalOutflow = projectedContestedTotalOutflow,
            npvMaxCompromiseCeiling = npvMaxCeiling,
            npvNetSavingsVsContested = npvNetSavings,
            nominalFutureSavings = nominalFutureSavings,
            claimantOfferFutureFdValue = claimantOfferFutureFdValue,
            claimantFdAdvantageVsTrial = claimantFdAdvantage,
            isOfferFavorableForSettlement = isFavorable
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McopCalculatorScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(McopInputState()) }
    var activeTab by remember { mutableStateOf(AppTab.CALCULATION_SHEET) }
    val result = remember(state) { McopJudicialEngine.calculate(state) }

    var showPetitionDatePicker by remember { mutableStateOf(false) }
    var showCalcDatePicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd-MM-yyyy") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("MCOP & Lok Adalat Calculator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Fatal • Disability • Actuarial Compromise Engine", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    },
                    actions = {
                        IconButton(onClick = { state = McopInputState() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    AppTab.entries.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = { Text(tab.title, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTab == AppTab.CALCULATION_SHEET) {
                        Column {
                            Text("Net Award: ${formatInr(result.netAwardAmount)}", fontSize = 12.sp, color = Color.Gray)
                            Text("Total: ${formatInr(result.totalPayableWithInterest)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Column {
                            Text("Max NPV Ceiling: ${formatInr(result.npvMaxCompromiseCeiling)}", fontSize = 12.sp, color = Color.Gray)
                            Text("Offer: ${formatInr(result.compromiseOfferAmount)} (${result.settlementRatioToTodayTotalPercent.roundToLong()}%)", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (result.isOfferFavorableForSettlement) Color(0xFF117864) else Color(0xFFB03A2E))
                        }
                    }
                    Button(onClick = { showExportSheet = true }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Memo")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeTab == AppTab.CALCULATION_SHEET) {
                McopSheetTabContent(
                    state = state,
                    result = result,
                    dateFormatter = dateFormatter,
                    onStateChange = { state = it },
                    onShowPetitionPicker = { showPetitionDatePicker = true },
                    onShowCalcPicker = { showCalcDatePicker = true },
                    onJumpToCompromiseTab = {
                        state = state.copy(compromiseOfferInput = (result.totalPayableWithInterest * 0.85).roundToLong().toString())
                        activeTab = AppTab.LOK_ADALAT_COMPROMISE
                    }
                )
            } else {
                LokAdalatCompromiseTabContent(state = state, result = result, onStateChange = { state = it })
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPetitionDatePicker) {
        McopDatePickerModal(initialDate = state.petitionDate, onDateSelected = { state = state.copy(petitionDate = it) }, onDismiss = { showPetitionDatePicker = false })
    }
    if (showCalcDatePicker) {
        McopDatePickerModal(initialDate = state.calculationDate, onDateSelected = { state = state.copy(calculationDate = it) }, onDismiss = { showCalcDatePicker = false })
    }
    if (showExportSheet) {
        ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export & Share Calculation Memo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("MCOP No: ${state.mcopNumber} • Claim: ${formatInr(result.totalPayableWithInterest)} • Offer: ${formatInr(result.compromiseOfferAmount)}", fontSize = 12.5.sp, color = Color.Gray)
                Button(onClick = { showExportSheet = false; McopExportManager.exportPdfCourtMemo(context, state, result) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Download & Share A4 PDF Memo (Includes Compromise)")
                }
                OutlinedButton(onClick = { showExportSheet = false; McopExportManager.exportCsvForSheets(context, state, result) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export CSV Row (Excel / Google Sheets)")
                }
                OutlinedButton(onClick = { showExportSheet = false; McopExportManager.shareWhatsAppSummary(context, state, result) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Share Summary via WhatsApp / Text")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McopSheetTabContent(
    state: McopInputState,
    result: McopCalculationResult,
    dateFormatter: DateTimeFormatter,
    onStateChange: (McopInputState) -> Unit,
    onShowPetitionPicker: () -> Unit,
    onShowCalcPicker: () -> Unit,
    onJumpToCompromiseTab: () -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ClaimMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ClaimMode.entries.size),
                onClick = { onStateChange(state.copy(claimMode = mode)) },
                selected = state.claimMode == mode
            ) {
                Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1. CASE & INCOME PARAMETERS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.mcopNumber, onValueChange = { onStateChange(state.copy(mcopNumber = it)) }, label = { Text("MCOP No.") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = state.claimantName, onValueChange = { onStateChange(state.copy(claimantName = it)) }, label = { Text(if (state.claimMode == ClaimMode.FATAL_DEATH) "Deceased Name" else "Claimant Name") }, modifier = Modifier.weight(1.3f), singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(value = state.age, onValueChange = { onStateChange(state.copy(age = it)) }, label = "Age (Yrs)", modifier = Modifier.weight(0.8f))
                NumberField(value = state.monthlyIncome, onValueChange = { onStateChange(state.copy(monthlyIncome = it)) }, label = "Monthly Income (₹)", modifier = Modifier.weight(1.2f))
                NumberField(value = state.annualIncomeTax, onValueChange = { onStateChange(state.copy(annualIncomeTax = it)) }, label = "Annual Tax (₹)", modifier = Modifier.weight(1f))
            }
            Text("Occupation Category (Pranay Sethi):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OccupationType.entries.forEach { type ->
                    FilterChip(selected = state.occupationType == type, onClick = { onStateChange(state.copy(occupationType = type)) }, label = { Text(type.label, fontSize = 11.sp) })
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("AUTO-DETECTED JUDICIAL PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("• Net Annual Income: ${formatInr(result.netAnnualIncome)} | Sarla Verma Multiplier: ${result.multiplier}", fontSize = 12.sp)
                    Text("• Future Prospects (+${result.futureProspectsPercent.toInt()}%): ${formatInr(result.annualIncomeWithFutureProspects)} / yr", fontSize = 12.sp)
                    if (state.claimMode == ClaimMode.FATAL_DEATH) {
                        Text("• Personal Deduction (${result.deductionFractionLabel}): -${formatInr(result.personalLivingDeductionAmount)} / yr", fontSize = 12.sp)
                        Text("• Annual Multiplicand: ${formatInr(result.annualMultiplicand)} × ${result.multiplier} = ${formatInr(result.lossOfDependencyTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (state.claimMode) {
                ClaimMode.FATAL_DEATH -> {
                    Text("2. FATAL DEPENDENCY & PRANAY SETHI HEADS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Marital Status & Deduction Category (Sarla Verma):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MaritalDeductionCategory.entries.forEach { cat ->
                            FilterChip(selected = state.maritalCategory == cat, onClick = { onStateChange(state.copy(maritalCategory = cat)) }, label = { Text(cat.label, fontSize = 11.sp) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.maritalCategory == MaritalDeductionCategory.MARRIED_FAMILY) {
                            NumberField(value = state.numberOfDependents, onValueChange = { onStateChange(state.copy(numberOfDependents = it, consortiumDependentsCount = it)) }, label = "Total Dependents", modifier = Modifier.weight(1f))
                        }
                        NumberField(value = state.consortiumDependentsCount, onValueChange = { onStateChange(state.copy(consortiumDependentsCount = it)) }, label = "Consortium Claimants", modifier = Modifier.weight(1f))
                        NumberField(value = state.consortiumRatePerHead, onValueChange = { onStateChange(state.copy(consortiumRatePerHead = it)) }, label = "Consortium / Head (₹)", modifier = Modifier.weight(1.2f))
                    }
                    Text("Pranay Sethi 3-Year Escalation Presets:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Triple("Base (2017)", "40000", "15000"), Triple("+10% (2020)", "44000", "16500"), Triple("+20% (2023)", "48000", "18000"), Triple("+30% (2026)", "52000", "19500")).forEach { (label, consRate, estateFuneralRate) ->
                            AssistChip(onClick = { onStateChange(state.copy(consortiumRatePerHead = consRate, lossOfEstate = estateFuneralRate, funeralExpenses = estateFuneralRate)) }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(value = state.lossOfEstate, onValueChange = { onStateChange(state.copy(lossOfEstate = it)) }, label = "Loss of Estate (₹)", modifier = Modifier.weight(1f))
                        NumberField(value = state.funeralExpenses, onValueChange = { onStateChange(state.copy(funeralExpenses = it)) }, label = "Funeral Expenses (₹)", modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider()
                    Text("Loss of Dependency: ${formatInr(result.lossOfDependencyTotal)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("Total Consortium (${state.consortiumDependentsCount} × ₹${state.consortiumRatePerHead}): ${formatInr(result.totalConsortiumAmount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                ClaimMode.TEMPORARY -> {
                    Text("2. TEMPORARY INJURY (NO PERMANENT DISABILITY)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TreatmentIncomeLossRow(state, result, onStateChange)
                }
                ClaimMode.PARTIAL_PERCENT -> {
                    Text("2. PARTIAL DISABILITY (PER-PERCENTAGE METHOD)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(value = state.medicalBoardPercentage, onValueChange = { onStateChange(state.copy(medicalBoardPercentage = it)) }, label = "Med. Board %", modifier = Modifier.weight(1f))
                        NumberField(value = state.ratePerPercentage, onValueChange = { onStateChange(state.copy(ratePerPercentage = it)) }, label = "Rate per % (₹)", modifier = Modifier.weight(1.3f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Quick Rate:", fontSize = 12.sp)
                        listOf("4000", "5000", "7000", "9000").forEach { preset ->
                            AssistChip(onClick = { onStateChange(state.copy(ratePerPercentage = preset)) }, label = { Text("₹$preset", fontSize = 11.sp) })
                        }
                    }
                    Text("Disability Award (${state.medicalBoardPercentage}% × ₹${state.ratePerPercentage}): ${formatInr(result.disabilityOrFutureEarningLoss)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    HorizontalDivider()
                    TreatmentIncomeLossRow(state, result, onStateChange)
                }
                ClaimMode.PERMANENT_MULTIPLIER -> {
                    Text("2. PERMANENT FUNCTIONAL DISABILITY (MULTIPLIER)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(value = state.medicalBoardPercentage, onValueChange = { onStateChange(state.copy(medicalBoardPercentage = it)) }, label = "Medical %", modifier = Modifier.weight(1f))
                        NumberField(value = state.functionalDisabilityPercentage, onValueChange = { onStateChange(state.copy(functionalDisabilityPercentage = it)) }, label = "Functional Loss %", modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.includeFutureProspects, onCheckedChange = { onStateChange(state.copy(includeFutureProspects = it)) })
                        Text("Add Future Prospects (+${result.futureProspectsPercent.toInt()}%) - Pappu Deo Yadav", fontSize = 13.sp)
                    }
                    Text("Future Earning Loss (${formatInr(result.annualIncomeWithFutureProspects)} × ${state.functionalDisabilityPercentage}% × ${result.multiplier}): ${formatInr(result.disabilityOrFutureEarningLoss)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    HorizontalDivider()
                    TreatmentIncomeLossRow(state, result, onStateChange)
                }
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = if (state.claimMode == ClaimMode.FATAL_DEATH) "3. PRE-DEATH MEDICAL & TRANSPORT HEADS (₹)" else "3. PECUNIARY & NON-PECUNIARY HEADS (₹)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (state.claimMode == ClaimMode.FATAL_DEATH) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(state.medicalBills, { onStateChange(state.copy(medicalBills = it)) }, "Pre-Death Medical Bills", Modifier.weight(1f))
                    NumberField(state.transportAndNourishment, { onStateChange(state.copy(transportAndNourishment = it)) }, "Ambulance / Transport", Modifier.weight(1f))
                }
                NumberField(state.damageToClothing, { onStateChange(state.copy(damageToClothing = it)) }, "Damage to Clothing / Vehicle", Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(state.medicalBills, { onStateChange(state.copy(medicalBills = it)) }, "Medical Bills", Modifier.weight(1f))
                    NumberField(state.painAndSuffering, { onStateChange(state.copy(painAndSuffering = it)) }, "Pain & Suffering", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(state.transportAndNourishment, { onStateChange(state.copy(transportAndNourishment = it)) }, "Transport & Nourish", Modifier.weight(1f))
                    NumberField(state.attenderCharges, { onStateChange(state.copy(attenderCharges = it)) }, "Attender Charges", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.claimMode != ClaimMode.TEMPORARY) {
                        NumberField(state.lossOfAmenities, { onStateChange(state.copy(lossOfAmenities = it)) }, "Loss of Amenities", Modifier.weight(1f))
                    }
                    NumberField(state.damageToClothing, { onStateChange(state.copy(damageToClothing = it)) }, "Damage to Clothing", Modifier.weight(1f))
                }
                if (state.claimMode == ClaimMode.PERMANENT_MULTIPLIER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(state.futureMedicalExpenses, { onStateChange(state.copy(futureMedicalExpenses = it)) }, "Future Medical / Limb", Modifier.weight(1f))
                        NumberField(state.lossOfMarriageProspects, { onStateChange(state.copy(lossOfMarriageProspects = it)) }, "Marriage Prospects", Modifier.weight(1f))
                    }
                }
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("4. NEGLIGENCE & PETITION-DATE INTEREST", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(value = state.contributoryNegligencePercent, onValueChange = { onStateChange(state.copy(contributoryNegligencePercent = it)) }, label = "Negligence Deduction %", modifier = Modifier.weight(1f))
                OutlinedTextField(value = state.negligenceNote, onValueChange = { onStateChange(state.copy(negligenceNote = it)) }, label = { Text("Deduction Reason") }, modifier = Modifier.weight(1.4f), singleLine = true)
            }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.petitionDate.format(dateFormatter), onValueChange = {}, readOnly = true, label = { Text("Date of Petition") }, trailingIcon = { IconButton(onClick = onShowPetitionPicker) { Icon(Icons.Default.DateRange, contentDescription = "Select Petition Date") } }, modifier = Modifier.weight(1f).clickable { onShowPetitionPicker() })
                OutlinedTextField(value = state.calculationDate.format(dateFormatter), onValueChange = {}, readOnly = true, label = { Text("Date of Calculation") }, trailingIcon = { IconButton(onClick = onShowCalcPicker) { Icon(Icons.Default.DateRange, contentDescription = "Select Calc Date") } }, modifier = Modifier.weight(1f).clickable { onShowCalcPicker() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(value = state.interestRatePercent, onValueChange = { onStateChange(state.copy(interestRatePercent = it)) }, label = "Interest Rate (% p.a.)", modifier = Modifier.weight(1f))
                NumberField(value = state.excludedDelayDays, onValueChange = { onStateChange(state.copy(excludedDelayDays = it)) }, label = "Less Excluded Delay Days", modifier = Modifier.weight(1f))
            }
            if (state.claimMode == ClaimMode.PERMANENT_MULTIPLIER) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.excludeFutureMedicalFromInterest, onCheckedChange = { onStateChange(state.copy(excludeFutureMedicalFromInterest = it)) })
                    Text("Exclude Future Medical Expenses from retroactive interest", fontSize = 12.sp)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.claimMode == ClaimMode.FATAL_DEATH) {
                        SummaryRow("Loss of Dependency:", formatInr(result.lossOfDependencyTotal))
                        SummaryRow("Conventional Heads (Consortium/Estate/Funeral):", formatInr(result.totalConventionalHeads))
                    } else {
                        SummaryRow("Disability / Future Earning Loss:", formatInr(result.disabilityOrFutureEarningLoss))
                        SummaryRow("Loss of Income (Treatment):", formatInr(result.lossOfIncomeDuringTreatment))
                    }
                    SummaryRow("GROSS COMPENSATION:", formatInr(result.grossCompensation), isBold = true)
                    if (result.negligenceDeductionAmount > 0) {
                        SummaryRow("Less Contributory Negligence (${state.contributoryNegligencePercent}%):", "- ${formatInr(result.negligenceDeductionAmount)}")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow("NET PRINCIPAL AWARD:", formatInr(result.netAwardAmount), isBold = true)
                    SummaryRow("Interest @ ${state.interestRatePercent}% (${result.totalInterestDays} Days / ${result.formattedDuration}):", "+ ${formatInr(result.interestAmount)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SummaryRow("TOTAL PAYABLE WITH INTEREST:", formatInr(result.totalPayableWithInterest), isBold = true)
                }
            }
            OutlinedButton(onClick = onJumpToCompromiseTab, modifier = Modifier.fillMaxWidth()) {
                Text("Analyze in Lok Adalat Compromise Calculator →", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun LokAdalatCompromiseTabContent(
    state: McopInputState,
    result: McopCalculationResult,
    onStateChange: (McopInputState) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. CURRENT CLAIM BASELINE (${state.mcopNumber} • ${state.claimMode.label.uppercase()})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            SummaryRow("Net Principal Award Today:", formatInr(result.netAwardAmount))
            SummaryRow("Accrued Interest Till Today (${result.totalInterestDays} Days @ ${state.interestRatePercent}%):", "+ ${formatInr(result.interestAmount)}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            SummaryRow("TOTAL CLAIM PAYABLE TODAY:", formatInr(result.totalPayableWithInterest), isBold = true)
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("2. LOK ADALAT LUMP-SUM COMPROMISE OFFER", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            NumberField(value = state.compromiseOfferInput, onValueChange = { onStateChange(state.copy(compromiseOfferInput = it)) }, label = "Proposed Lump-Sum Compromise Offer (₹)", modifier = Modifier.fillMaxWidth())
            Text("Quick Interest-Waiver Presets:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = { onStateChange(state.copy(compromiseOfferInput = result.netAwardAmount.roundToLong().toString())) }, label = { Text("100% Principal (0% Interest)", fontSize = 11.sp) })
                AssistChip(onClick = {
                    val halfInt = result.netAwardAmount + (result.interestAmount * 0.5)
                    onStateChange(state.copy(compromiseOfferInput = halfInt.roundToLong().toString()))
                }, label = { Text("Principal + 50% Interest", fontSize = 11.sp) })
            }
            Text("Quick Percentage of Today's Total Payable:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(70, 75, 80, 85, 90).forEach { pct ->
                    AssistChip(onClick = {
                        val amt = result.totalPayableWithInterest * (pct / 100.0)
                        onStateChange(state.copy(compromiseOfferInput = amt.roundToLong().toString()))
                    }, label = { Text("$pct% (${formatInr(result.totalPayableWithInterest * (pct / 100.0))})", fontSize = 11.sp) })
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SummaryRow("Settlement Ratio (vs Today's Total):", "${String.format(Locale.US, "%.1f", result.settlementRatioToTodayTotalPercent)}%", isBold = true)
                    SummaryRow("Settlement Ratio (vs Net Principal):", "${String.format(Locale.US, "%.1f", result.settlementRatioToPrincipalPercent)}%")
                    SummaryRow("Immediate Haircut / Waiver Today:", if (result.immediateDiscountSaved >= 0) "${formatInr(result.immediateDiscountSaved)} Saved" else "${formatInr(-result.immediateDiscountSaved)} Excess")
                }
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. CONTESTED TRIBUNAL TRIAL PROJECTION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(value = state.expectedTrialYears, onValueChange = { onStateChange(state.copy(expectedTrialYears = it)) }, label = "Addl. Trial Duration (Yrs)", modifier = Modifier.weight(1f))
                NumberField(value = state.trialAssessmentHaircutPercent, onValueChange = { onStateChange(state.copy(trialAssessmentHaircutPercent = it)) }, label = "Trial Evidence Haircut %", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Trial Horizon:", fontSize = 12.sp)
                listOf("1.0", "2.0", "3.0", "5.0").forEach { yrs ->
                    AssistChip(onClick = { onStateChange(state.copy(expectedTrialYears = yrs)) }, label = { Text("$yrs Yrs", fontSize = 11.sp) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(value = state.discountOrFdRatePercent, onValueChange = { onStateChange(state.copy(discountOrFdRatePercent = it)) }, label = "Discount / FD Rate (% p.a.)", modifier = Modifier.weight(1f))
                NumberField(value = state.futureLitigationCost, onValueChange = { onStateChange(state.copy(futureLitigationCost = it)) }, label = "Future Legal / Appeal Cost (₹)", modifier = Modifier.weight(1f))
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("4. ACTUARIAL COMPROMISE VS. TRIAL VERDICT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            val bannerBg = if (result.isOfferFavorableForSettlement) Color(0xFFD4EFDF) else Color(0xFFFADBD8)
            val bannerTextCol = if (result.isOfferFavorableForSettlement) Color(0xFF0E6251) else Color(0xFF78281F)
            Box(modifier = Modifier.fillMaxWidth().background(bannerBg, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = if (result.isOfferFavorableForSettlement) "FAVORABLE COMPROMISE SETTLEMENT (BELOW NPV CEILING)" else "OFFER EXCEEDS ACTUARIAL NPV CEILING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bannerTextCol)
                    Text(text = "Max Recommended Compromise Ceiling (NPV): ${formatInr(result.npvMaxCompromiseCeiling)}", fontSize = 12.sp, color = bannerTextCol)
                }
            }
            Text("A. Contested Trial Projection (${state.expectedTrialYears} Yrs Later):", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            SummaryRow("Contested Principal (After ${state.trialAssessmentHaircutPercent}% Trial Haircut):", formatInr(result.contestedPrincipalAfterHaircut))
            SummaryRow("Total Accrued Interest (${result.totalContestedInterestDays} Days @ ${state.interestRatePercent}%):", "+ ${formatInr(result.projectedContestedInterestAmount)}")
            SummaryRow("Future Legal, Witness & Appeal Costs:", "+ ${formatInr(state.futureLitigationCost.toDoubleOrNull() ?: 0.0)}")
            SummaryRow("PROJECTED FUTURE TRIBUNAL OUTFLOW:", formatInr(result.projectedContestedTotalOutflow), isBold = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("B. Insurer / TP Claims Officer Actuarial Metrics:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            SummaryRow("Nominal Future Payout Saved:", formatInr(result.nominalFutureSavings))
            SummaryRow("Present Value (NPV @ ${state.discountOrFdRatePercent}% p.a.) of Trial:", formatInr(result.npvMaxCompromiseCeiling), isBold = true)
            SummaryRow("Proposed Lok Adalat Lump-Sum Offer:", formatInr(result.compromiseOfferAmount), isBold = true)
            SummaryRow("Net Present Value (NPV) Capital Gain / Loss:", if (result.npvNetSavingsVsContested >= 0) "+ ${formatInr(result.npvNetSavingsVsContested)} (Gain)" else "- ${formatInr(-result.npvNetSavingsVsContested)} (Loss)", isBold = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("C. Claimant / Petitioner FD Investment View:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            SummaryRow("Lump-Sum Invested in FD (@ ${state.discountOrFdRatePercent}% for ${state.expectedTrialYears} Yrs):", formatInr(result.claimantOfferFutureFdValue), isBold = true)
            SummaryRow("Expected Contested Award in ${state.expectedTrialYears} Yrs (Excl. Costs):", formatInr(result.contestedPrincipalAfterHaircut + result.projectedContestedInterestAmount))
            SummaryRow("Claimant FD Maturity vs Waiting for Trial:", if (result.claimantFdAdvantageVsTrial >= 0) "+ ${formatInr(result.claimantFdAdvantageVsTrial)} Better Now" else "${formatInr(result.claimantFdAdvantageVsTrial)} vs Trial")
        }
    }
}

@Composable
fun TreatmentIncomeLossRow(state: McopInputState, result: McopCalculationResult, onStateChange: (McopInputState) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(value = state.treatmentRestMonths, onValueChange = { onStateChange(state.copy(treatmentRestMonths = it)) }, label = "Bed Rest / Treatment (Months)", modifier = Modifier.weight(1f))
        Column(modifier = Modifier.weight(1f)) {
            Text("Loss of Income (Treatment)", fontSize = 11.sp, color = Color.Gray)
            Text(formatInr(result.lossOfIncomeDuringTreatment), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.all { it.isDigit() || it == '.' }) onValueChange(input) },
        label = { Text(label, fontSize = 11.5.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.2.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1.4f))
        Text(value, fontSize = 12.2.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McopDatePickerModal(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val initialMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                }
                onDismiss()
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = datePickerState) }
}

fun formatInr(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(amount.roundToLong())
}

object McopExportManager {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun shareWhatsAppSummary(context: Context, state: McopInputState, result: McopCalculationResult) {
        val text = buildString {
            appendLine("*MCOP CALCULATION & COMPROMISE MEMO (${state.claimMode.label.uppercase()})*")
            appendLine("--------------------------------")
            appendLine("*MCOP No:* ${state.mcopNumber}")
            if (state.claimantName.isNotBlank()) {
                appendLine("*${if (state.claimMode == ClaimMode.FATAL_DEATH) "Deceased" else "Claimant"}:* ${state.claimantName}")
            }
            appendLine("*Age / Multiplier:* ${state.age} Yrs (Mult: ${result.multiplier})")
            appendLine("*Net Annual Income:* ${formatInr(result.netAnnualIncome)}")
            appendLine("--------------------------------")
            appendLine("*NET PRINCIPAL AWARD:* *${formatInr(result.netAwardAmount)}*")
            appendLine("*Interest (${state.interestRatePercent}% from ${state.petitionDate.format(dateFormatter)} - ${result.totalInterestDays} days):* +${formatInr(result.interestAmount)}")
            appendLine("*TOTAL PAYABLE TODAY:* *${formatInr(result.totalPayableWithInterest)}*")
            if (result.compromiseOfferAmount > 0) {
                appendLine("================================")
                appendLine("*LOK ADALAT COMPROMISE ANALYSIS*")
                appendLine("• *Proposed Offer:* *${formatInr(result.compromiseOfferAmount)}* (${String.format(Locale.US, "%.1f", result.settlementRatioToTodayTotalPercent)}%)")
                appendLine("• *Max NPV Ceiling:* *${formatInr(result.npvMaxCompromiseCeiling)}*")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Summary"))
    }

    fun exportCsvForSheets(context: Context, state: McopInputState, result: McopCalculationResult) {
        try {
            val csvHeader = "MCOP_No,Name,Age,Mode,Net_Annual_Income,Multiplier,Net_Award,Total_Payable_Today,Lok_Adalat_Offer,NPV_Ceiling
"
            val csvRow = listOf(
                state.mcopNumber, state.claimantName.replace(",", " "), state.age, state.claimMode.label,
                result.netAnnualIncome.roundToLong().toString(), result.multiplier.toString(),
                result.netAwardAmount.roundToLong().toString(), result.totalPayableWithInterest.roundToLong().toString(),
                result.compromiseOfferAmount.roundToLong().toString(), result.npvMaxCompromiseCeiling.roundToLong().toString()
            ).joinToString(",")
            val file = File(context.cacheDir, "MCOP_${state.mcopNumber.replace("/", "_")}.csv")
            FileOutputStream(file).use { it.write((csvHeader + csvRow).toByteArray()) }
            shareFile(context, file, "text/csv", "Export CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "CSV Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportPdfCourtMemo(context: Context, state: McopInputState, result: McopCalculationResult) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 13.5f; color = AndroidColor.BLACK; textAlign = Paint.Align.CENTER }
            val boldPaint = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f; color = AndroidColor.BLACK }
            val normalPaint = Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textSize = 9.5f; color = AndroidColor.BLACK }
            val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
            val rightNormalPaint = Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT }
            val linePaint = Paint().apply { color = AndroidColor.DKGRAY; strokeWidth = 1f }

            var y = 40f
            canvas.drawText("BEFORE THE MOTOR ACCIDENTS CLAIMS TRIBUNAL / LOK ADALAT", 297f, y, titlePaint)
            y += 16f
            canvas.drawText("CALCULATION MEMO IN M.C.O.P. No. ${state.mcopNumber}", 297f, y, titlePaint)
            y += 12f
            canvas.drawLine(40f, y, 555f, y, linePaint)

            y += 16f
            canvas.drawText("Net Principal Award: ${formatInr(result.netAwardAmount)}", 45f, y, normalPaint)
            canvas.drawText("Total Payable: ${formatInr(result.totalPayableWithInterest)}", 330f, y, normalPaint)
            y += 16f
            canvas.drawText("Lok Adalat Offer: ${formatInr(result.compromiseOfferAmount)}", 45f, y, normalPaint)
            canvas.drawText("NPV Ceiling: ${formatInr(result.npvMaxCompromiseCeiling)}", 330f, y, normalPaint)

            pdfDocument.finishPage(page)
            val file = File(context.cacheDir, "MCOP_Memo_${state.mcopNumber.replace("/", "_")}.pdf")
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            shareFile(context, file, "application/pdf", "Share PDF")
        } catch (e: Exception) {
            Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
