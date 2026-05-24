package com.klogviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.*

@Composable
fun S3ConnectionDialog(
    savedConnections: List<S3Config> = emptyList(),
    onConnect: (config: S3Config) -> Unit,
    onSave: (S3Config) -> Unit,
    onDelete: (String) -> Unit,
    onBrowse: (config: S3Config) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bucket by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("us-east-1") }
    var prefix by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(0) } // 0: Default, 1: Profile, 2: Explicit
    var profileName by remember { mutableStateOf("") }
    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }

    val firstFieldFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        firstFieldFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to AWS S3") },
        text = {
            val focusManager = LocalFocusManager.current
            val handleTab = { event: KeyEvent ->
                if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                    focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                    true
                } else {
                    false
                }
            }

            Column(
                modifier = Modifier
                    .width(400.dp)
                    .padding(vertical = 8.dp)
                    .onPreviewKeyEvent(handleTab)
            ) {
                if (savedConnections.isNotEmpty()) {
                    Box {
                        OutlinedTextField(
                            value = if (name.isEmpty()) "Select saved connection..." else name,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = { Text("Saved Connections") },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, "Select saved connection")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded, 
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.width(400.dp)
                        ) {
                            savedConnections.forEach { config ->
                                DropdownMenuItem(onClick = {
                                    name = config.name
                                    bucket = config.bucket
                                    region = config.region ?: "us-east-1"
                                    prefix = config.prefix
                                    when (val auth = config.auth) {
                                        is S3Auth.DefaultChain -> {
                                            authType = 0
                                        }
                                        is S3Auth.Profile -> {
                                            authType = 1
                                            profileName = auth.profileName
                                        }
                                        is S3Auth.Explicit -> {
                                            authType = 2
                                            accessKey = auth.accessKey
                                            secretKey = auth.secretKey
                                            region = auth.region
                                        }
                                    }
                                    expanded = false
                                }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(config.name, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { 
                                            onDelete(config.name)
                                        }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Connection Name") },
                    modifier = Modifier.fillMaxWidth().focusRequester(firstFieldFocusRequester),
                    placeholder = { Text("e.g. Production Logs S3") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bucket,
                    onValueChange = { bucket = it },
                    label = { Text("Bucket Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Region") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Authentication", style = MaterialTheme.typography.subtitle2)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == 0, onClick = { authType = 0 })
                        Text("Default (Env/Web Identity)", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == 1, onClick = { authType = 1 })
                        Text("AWS Profile", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = authType == 2, onClick = { authType = 2 })
                        Text("Explicit Credentials", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (authType == 1) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )
                } else if (authType == 2) {
                    OutlinedTextField(
                        value = accessKey,
                        onValueChange = { accessKey = it },
                        label = { Text("Access Key ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("Secret Access Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("S3 Prefix / Object Key") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val auth = when(authType) {
                                1 -> S3Auth.Profile(profileName)
                                2 -> S3Auth.Explicit(accessKey, secretKey, region)
                                else -> S3Auth.DefaultChain
                            }
                            onBrowse(S3Config(name, bucket, region.takeIf { it.isNotBlank() }, auth, prefix))
                        },
                        enabled = bucket.isNotBlank(),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Browse")
                    }
                }
            }
        },
        confirmButton = {
            val focusManager = LocalFocusManager.current
            Row {
                Button(
                    onClick = {
                        val auth = when(authType) {
                            1 -> S3Auth.Profile(profileName)
                            2 -> S3Auth.Explicit(accessKey, secretKey, region)
                            else -> S3Auth.DefaultChain
                        }
                        onSave(S3Config(name, bucket, region.takeIf { it.isNotBlank() }, auth, prefix))
                    },
                    enabled = name.isNotBlank() && bucket.isNotBlank(),
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
                ) {
                    Text("Save")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val auth = when(authType) {
                            1 -> S3Auth.Profile(profileName)
                            2 -> S3Auth.Explicit(accessKey, secretKey, region)
                            else -> S3Auth.DefaultChain
                        }
                        onConnect(S3Config(name, bucket, region.takeIf { it.isNotBlank() }, auth, prefix))
                    },
                    enabled = name.isNotBlank() && bucket.isNotBlank() && prefix.isNotBlank(),
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
                ) {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            val focusManager = LocalFocusManager.current
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                        focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                        true
                    } else {
                        false
                    }
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
