package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Member
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    member: Member?,
    onBack: () -> Unit,
    onSave: (
        principal: Double,
        interestRate: Double,
        interestType: String,
        purpose: String,
        notes: String,
        startDate: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    if (member == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var principalText by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("") }
    var interestType by remember { mutableStateOf("MONTHLY_SIMPLE") } // "FIXED", "MONTHLY_SIMPLE", "YEARLY_SIMPLE"
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var principalError by remember { mutableStateOf<String?>(null) }
    var interestRateError by remember { mutableStateOf<String?>(null) }

    val interestTypesList = listOf(
        Triple("MONTHLY_SIMPLE", "Monthly %", "Accrues simple monthly interest (e.g., 5% monthly)"),
        Triple("YEARLY_SIMPLE", "Yearly %", "Accrues simple yearly interest (e.g., 8% yearly)"),
        Triple("FIXED", "Fixed Amount", "Adds a flat fixed interest fee (e.g., $100 flat)")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue New Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Member Info Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Borrower",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Principal Amount Field
            OutlinedTextField(
                value = principalText,
                onValueChange = {
                    principalText = it
                    if (it.toDoubleOrNull() != null) principalError = null
                },
                label = { Text("Loan Principal Amount (₹) *") },
                placeholder = { Text("0.00") },
                leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, contentDescription = "Money Icon") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_principal_input"),
                isError = principalError != null,
                supportingText = principalError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Interest Selection Header
            Text(
                text = "Interest Plan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Segmented/Visual Interest Selector
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                interestTypesList.forEach { (type, label, desc) ->
                    val isSelected = interestType == type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { interestType = type }
                            .testTag("interest_type_$type"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { interestType = type }
                            )
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Interest Rate Field
            val rateLabel = when (interestType) {
                "FIXED" -> "Fixed Interest Amount (₹)"
                "MONTHLY_SIMPLE" -> "Monthly Interest Rate (%)"
                "YEARLY_SIMPLE" -> "Yearly Interest Rate (%)"
                else -> "Interest Rate"
            }
            val ratePlaceholder = when (interestType) {
                "FIXED" -> "e.g., 100.00"
                else -> "e.g., 5.0"
            }
            OutlinedTextField(
                value = interestRateText,
                onValueChange = {
                    interestRateText = it
                    if (it.toDoubleOrNull() != null || it.isEmpty()) interestRateError = null
                },
                label = { Text(rateLabel) },
                placeholder = { Text(ratePlaceholder) },
                leadingIcon = {
                    Icon(
                        imageVector = if (interestType == "FIXED") Icons.Rounded.CurrencyRupee else Icons.Rounded.Percent,
                        contentDescription = "Rate Icon"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_interest_input"),
                isError = interestRateError != null,
                supportingText = interestRateError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Purpose Field
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Purpose of Loan") },
                placeholder = { Text("Business expansion, medical bill, travel, etc.") },
                leadingIcon = { Icon(Icons.Rounded.Label, contentDescription = "Label Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_purpose_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes") },
                placeholder = { Text("Terms, agreement details, payment cycles...") },
                leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = "Notes Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("loan_notes_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Quick Date Selector (Today, Yesterday, 1 week ago)
            Text(
                text = "Loan Date: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(selectedDate))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val now = System.currentTimeMillis()
                val oneDay = 24L * 60 * 60 * 1000
                DateOptionButton(
                    label = "Today",
                    isSelected = selectedDate in (now - 10000)..(now + 10000),
                    onClick = { selectedDate = now },
                    modifier = Modifier.weight(1f)
                )
                DateOptionButton(
                    label = "Yesterday",
                    isSelected = selectedDate in (now - oneDay - 10000)..(now - oneDay + 10000),
                    onClick = { selectedDate = now - oneDay },
                    modifier = Modifier.weight(1f)
                )
                DateOptionButton(
                    label = "30 Days Ago",
                    isSelected = selectedDate in (now - (30 * oneDay) - 10000)..(now - (30 * oneDay) + 10000),
                    onClick = { selectedDate = now - (30 * oneDay) },
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val principal = principalText.toDoubleOrNull()
                    val interestRate = interestRateText.toDoubleOrNull() ?: 0.0

                    if (principal == null || principal <= 0) {
                        principalError = "Please enter a valid positive principal amount"
                    } else if (interestRateText.isNotEmpty() && interestRateText.toDoubleOrNull() == null) {
                        interestRateError = "Please enter a valid interest rate"
                    } else {
                        onSave(principal, interestRate, interestType, purpose.trim(), notes.trim(), selectedDate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_loan_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Issue Loan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun DateOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
        )
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
