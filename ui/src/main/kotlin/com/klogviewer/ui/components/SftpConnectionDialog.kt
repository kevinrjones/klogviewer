package com.klogviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.klogviewer.domain.model.*

@Composable
fun SftpConnectionDialog(
    savedConnections: List<SftpConfig> = emptyList(),
    onConnect: (name: String, host: String, port: Int, user: String, auth: SftpAuth, path: String) -> Unit,
    onSave: (SftpConfig) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
    dialogProvider: DialogProvider = AwtDialogProvider()
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var keyPath by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("/var/log/syslog") }
    var authType by remember { mutableStateOf(0) } // 0 for Password, 1 for KeyPair

    var expanded by remember { mutableStateOf(false) }

    val firstFieldFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        firstFieldFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to SFTP") },
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
                                    host = config.host.value
                                    port = config.port.value.toString()
                                    user = config.username.value
                                    path = config.logFilePath
                                    when (val auth = config.auth) {
                                        is SftpAuth.Password -> {
                                            authType = 0
                                            password = auth.password
                                        }
                                        is SftpAuth.KeyPair -> {
                                            authType = 1
                                            keyPath = auth.privateKeyPath
                                            passphrase = auth.passphrase ?: ""
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
                    placeholder = { Text("e.g. Production Web Server") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Authentication", style = MaterialTheme.typography.subtitle2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = authType == 0, onClick = { authType = 0 })
                    Text("Password", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = authType == 1, onClick = { authType = 1 })
                    Text("Key Pair", modifier = Modifier.padding(start = 8.dp))
                }
                
                if (authType == 0) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = keyPath,
                                onValueChange = { keyPath = it },
                                label = { Text("Key Path") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val file = dialogProvider.showOpenFileDialog("Select SSH Key")
                                    if (file != null) {
                                        keyPath = file.absolutePath
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Browse")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("Passphrase (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Log File Path") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        },
        confirmButton = {
            val focusManager = LocalFocusManager.current
            Row {
                Button(
                    onClick = {
                        val auth = if (authType == 0) {
                            SftpAuth.Password(password)
                        } else {
                            SftpAuth.KeyPair(keyPath, passphrase.takeIf { it.isNotBlank() })
                        }
                        onSave(SftpConfig(name, Host(host), Port(port.toIntOrNull() ?: 22), Username(user), auth, path))
                    },
                    enabled = name.isNotBlank() && host.isNotBlank() && user.isNotBlank(),
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
                        val auth = if (authType == 0) {
                            SftpAuth.Password(password)
                        } else {
                            SftpAuth.KeyPair(keyPath, passphrase.takeIf { it.isNotBlank() })
                        }
                        onConnect(name, host, port.toIntOrNull() ?: 22, user, auth, path)
                    },
                    enabled = name.isNotBlank() && host.isNotBlank() && user.isNotBlank() && path.isNotBlank(),
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
