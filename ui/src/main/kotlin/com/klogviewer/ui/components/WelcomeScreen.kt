package com.klogviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onOpenFile: () -> Unit,
    onOpenDirectory: () -> Unit,
    onConnectSftp: () -> Unit,
    onConnectS3: () -> Unit,
    onShowRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().testTag("welcome_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to KLogViewer",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a log source to get started",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            WelcomeCard(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                title = "Open Local File",
                description = "View a single log file on your computer",
                onClick = onOpenFile,
                testTag = "welcome_open_file"
            )
            WelcomeCard(
                icon = Icons.Default.Folder,
                title = "Open Directory",
                description = "Monitor all logs in a local folder",
                onClick = onOpenDirectory,
                testTag = "welcome_open_directory"
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            WelcomeCard(
                icon = Icons.Default.Cloud,
                title = "Connect SFTP",
                description = "Tail logs from a remote server via SSH",
                onClick = onConnectSftp,
                testTag = "welcome_connect_sftp"
            )
            WelcomeCard(
                icon = Icons.Default.CloudQueue,
                title = "Connect S3",
                description = "Read logs from an AWS S3 bucket",
                onClick = onConnectS3,
                testTag = "welcome_connect_s3"
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        WelcomeCard(
            icon = Icons.Default.History,
            title = "Recent Items",
            description = "Quickly reopen your previous logs",
            onClick = onShowRecent,
            modifier = Modifier.width(424.dp),
            testTag = "welcome_recent_items"
        )
    }
}

@Composable
private fun WelcomeCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(200.dp),
    testTag: String? = null
) {
    Card(
        modifier = (if (testTag != null) modifier.testTag(testTag) else modifier)
            .clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(40.dp), 
                tint = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title, 
                style = MaterialTheme.typography.subtitle1, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description, 
                style = MaterialTheme.typography.caption, 
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}
