package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Member

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    onBack: () -> Unit,
    onSave: (name: String, phone: String, email: String, notes: String) -> Unit,
    memberToEdit: Member? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(memberToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(memberToEdit?.phone ?: "") }
    var email by remember { mutableStateOf(memberToEdit?.email ?: "") }
    var notes by remember { mutableStateOf(memberToEdit?.notes ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (memberToEdit == null) "Add New Member" else "Edit Member",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            Text(
                text = "Enter person details to record and manage loans.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Full Name *") },
                placeholder = { Text("John Doe") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = "Person Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_name_input"),
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Phone Field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+1 (555) 019-2834") },
                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = "Phone Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_phone_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("johndoe@example.com") },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_email_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Relationship") },
                placeholder = { Text("Close friend, colleague, group member...") },
                leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = "Notes Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("member_notes_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name cannot be empty"
                    } else {
                        onSave(name.trim(), phone.trim(), email.trim(), notes.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_member_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (memberToEdit == null) "Create Member" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
