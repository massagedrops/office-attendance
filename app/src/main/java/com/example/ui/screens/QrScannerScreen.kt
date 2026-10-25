package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.Employee
import com.example.data.repository.ScanResult
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseBg
import com.example.ui.theme.RoseDanger
import com.example.util.Localization
import com.example.util.TimeUtil
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    employees: List<Employee>,
    scanResult: ScanResult?,
    isArabic: Boolean,
    bossWhatsAppNumber: String = "",
    onScanCode: (String) -> Unit,
    onConfirmAttendance: (ScanResult.PendingConfirmation) -> Unit = {},
    onDismissScanResult: () -> Unit,
    onRequestAdminOverride: () -> Unit,
    onNotifyBossWhatsApp: (employeeName: String, time: String, date: String, isCheckIn: Boolean) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isModalOpen = scanResult != null
    val isModalOpenState = rememberUpdatedState(isModalOpen)

    var lastScannedCodeInMemory by remember { mutableStateOf("") }
    var lastScannedTimeInMemory by remember { mutableStateOf(0L) }

    // When modal closes or is dismissed, reset cooldown to now so the camera does not immediately re-scan the same code
    LaunchedEffect(scanResult) {
        if (scanResult == null) {
            lastScannedTimeInMemory = System.currentTimeMillis()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qr_scanner_screen")
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // CameraX Viewfinder with ZXing ImageAnalysis
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val qrReader = MultiFormatReader()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                if (isModalOpenState.value) {
                                    // A confirmation/result dialog is open; pause camera frame parsing
                                    return@setAnalyzer
                                }

                                val buffer = imageProxy.planes[0].buffer
                                val data = toByteArray(buffer)
                                val width = imageProxy.width
                                val height = imageProxy.height

                                val source = PlanarYUVLuminanceSource(
                                    data, width, height, 0, 0, width, height, false
                                )
                                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                val result = qrReader.decodeWithState(binaryBitmap)

                                if (result != null && result.text.isNotBlank()) {
                                    val text = result.text.trim()
                                    val now = System.currentTimeMillis()
                                    if (text != lastScannedCodeInMemory || (now - lastScannedTimeInMemory) > 2500L) {
                                        lastScannedCodeInMemory = text
                                        lastScannedTimeInMemory = now
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onScanCode(text)
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // No barcode found in this frame
                            } finally {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) {
                            // Camera binding fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        } else {
            // Permission request overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = Localization.get("camera_permission_required", isArabic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(Localization.get("grant_permission", isArabic))
                    }
                }
            }
        }

        // Scanner Reticle Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(3.dp, PrimaryBlue, RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = Localization.get("scan_instruction", isArabic),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Bottom Test / Simulate Scan Bar (Essential for office phone testing & emulator use!)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Navy900.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get("select_employee_to_scan", isArabic),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Asia/Riyadh",
                        color = PrimaryBlue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(employees) { emp ->
                        val displayName = if (isArabic && emp.nameAr.isNotBlank()) emp.nameAr else emp.name
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                            modifier = Modifier
                                .clickable { onScanCode(emp.qrCode) }
                                .testTag("simulate_scan_${emp.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = emp.id,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = displayName.split(" ").firstOrNull() ?: displayName,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scan Result Modal / Dialog
        scanResult?.let { result ->
            ScanResultDialog(
                result = result,
                isArabic = isArabic,
                bossWhatsAppNumber = bossWhatsAppNumber,
                onConfirmAttendance = onConfirmAttendance,
                onDismiss = onDismissScanResult,
                onRequestOverride = onRequestAdminOverride,
                onNotifyBoss = { res ->
                    val empName = when (res) {
                        is ScanResult.PendingConfirmation -> if (isArabic && res.employee.nameAr.isNotBlank()) res.employee.nameAr else res.employee.name
                        is ScanResult.SuccessCheckIn -> if (isArabic && res.employee.nameAr.isNotBlank()) res.employee.nameAr else res.employee.name
                        is ScanResult.SuccessCheckOut -> if (isArabic && res.employee.nameAr.isNotBlank()) res.employee.nameAr else res.employee.name
                        else -> ""
                    }
                    val time = when (res) {
                        is ScanResult.PendingConfirmation -> res.timeFormatted
                        is ScanResult.SuccessCheckIn -> res.record.checkInFormatted ?: ""
                        is ScanResult.SuccessCheckOut -> res.record.checkOutFormatted ?: ""
                        else -> ""
                    }
                    val date = when (res) {
                        is ScanResult.PendingConfirmation -> res.date
                        is ScanResult.SuccessCheckIn -> res.record.attendanceDate
                        is ScanResult.SuccessCheckOut -> res.record.attendanceDate
                        else -> TimeUtil.todayDateString()
                    }
                    val isCheckIn = when (res) {
                        is ScanResult.PendingConfirmation -> res.isCheckIn
                        is ScanResult.SuccessCheckIn -> true
                        else -> false
                    }
                    onNotifyBossWhatsApp(empName, time, date, isCheckIn)
                }
            )
        }
    }
}

@Composable
fun ScanResultDialog(
    result: ScanResult,
    isArabic: Boolean,
    bossWhatsAppNumber: String = "",
    onConfirmAttendance: (ScanResult.PendingConfirmation) -> Unit = {},
    onDismiss: () -> Unit,
    onRequestOverride: () -> Unit,
    onNotifyBoss: (ScanResult) -> Unit = {}
) {
    var selectedIsCheckIn by remember(result) {
        mutableStateOf(if (result is ScanResult.PendingConfirmation) result.isCheckIn else true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("scan_result_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val icon = when (result) {
                    is ScanResult.PendingConfirmation -> if (selectedIsCheckIn) Icons.Default.Login else Icons.Default.Logout
                    is ScanResult.SuccessCheckIn -> Icons.Default.Done
                    is ScanResult.SuccessCheckOut -> Icons.Default.Done
                    is ScanResult.DuplicateWarning -> Icons.Default.WarningAmber
                    is ScanResult.InactiveEmployee -> Icons.Default.WarningAmber
                    is ScanResult.NotFound, is ScanResult.Error -> Icons.Default.Close
                }
                val iconColor = when (result) {
                    is ScanResult.PendingConfirmation -> if (selectedIsCheckIn) EmeraldSuccess else PrimaryBlue
                    is ScanResult.SuccessCheckIn -> EmeraldSuccess
                    is ScanResult.SuccessCheckOut -> PrimaryBlue
                    is ScanResult.DuplicateWarning -> AmberWarning
                    is ScanResult.InactiveEmployee -> RoseDanger
                    is ScanResult.NotFound, is ScanResult.Error -> RoseDanger
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor)
                }

                val titleText = when (result) {
                    is ScanResult.PendingConfirmation -> if (selectedIsCheckIn) Localization.get("title_confirm_in", isArabic) else Localization.get("title_confirm_out", isArabic)
                    is ScanResult.SuccessCheckIn -> Localization.get("check_in_confirmed_success", isArabic)
                    is ScanResult.SuccessCheckOut -> Localization.get("check_out_confirmed_success", isArabic)
                    is ScanResult.DuplicateWarning -> Localization.get("duplicate_scan_warning", isArabic)
                    is ScanResult.InactiveEmployee -> Localization.get("employee_inactive_warning", isArabic)
                    is ScanResult.NotFound -> Localization.get("employee_not_found", isArabic)
                    is ScanResult.Error -> result.message
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (result) {
                    is ScanResult.PendingConfirmation -> {
                        val empName = if (isArabic && result.employee.nameAr.isNotBlank()) result.employee.nameAr else result.employee.name
                        Text(
                            text = empName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${Localization.get("employee_id", isArabic)}: ${result.employee.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Mode Switcher Chips (IN / OUT)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { selectedIsCheckIn = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedIsCheckIn) EmeraldSuccess else EmeraldBg,
                                border = BorderStroke(1.5.dp, if (selectedIsCheckIn) EmeraldSuccess else EmeraldSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        tint = if (selectedIsCheckIn) Color.White else EmeraldSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Localization.get("badge_in", isArabic),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedIsCheckIn) Color.White else EmeraldSuccess
                                    )
                                }
                            }

                            Surface(
                                onClick = { selectedIsCheckIn = false },
                                shape = RoundedCornerShape(10.dp),
                                color = if (!selectedIsCheckIn) PrimaryBlue else Color(0xFFEFF6FF),
                                border = BorderStroke(1.5.dp, if (!selectedIsCheckIn) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        tint = if (!selectedIsCheckIn) Color.White else PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Localization.get("badge_out", isArabic),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!selectedIsCheckIn) Color.White else PrimaryBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Action Banner: IN or OUT
                        val actionBg = if (selectedIsCheckIn) EmeraldBg else Color(0xFFEFF6FF)
                        val actionBorder = if (selectedIsCheckIn) EmeraldSuccess else PrimaryBlue
                        val actionColor = if (selectedIsCheckIn) EmeraldSuccess else PrimaryBlue
                        val actionText = if (selectedIsCheckIn) Localization.get("action_check_in", isArabic) else Localization.get("action_check_out", isArabic)
                        val actionIcon = if (selectedIsCheckIn) Icons.Default.Login else Icons.Default.Logout

                        Card(
                            colors = CardDefaults.cardColors(containerColor = actionBg),
                            border = BorderStroke(1.5.dp, actionBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = actionIcon,
                                        contentDescription = null,
                                        tint = actionColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "${Localization.get("attendance_action", isArabic)}: $actionText",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = actionColor,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${Localization.get("date_and_time", isArabic)}: ${result.date} • ${result.timeFormatted}",
                                    fontSize = 13.sp,
                                    color = actionColor.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Localization.get("assigned_shift", isArabic)}: ${result.shiftName}",
                                    fontSize = 12.sp,
                                    color = actionColor.copy(alpha = 0.8f)
                                )
                                if (selectedIsCheckIn && result.lateMinutes > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${Localization.get("late", isArabic)}: ${result.lateMinutes} min",
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    is ScanResult.SuccessCheckIn -> {
                        val empName = if (isArabic && result.employee.nameAr.isNotBlank()) result.employee.nameAr else result.employee.name
                        Text(
                            text = empName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${result.employee.id} • ${result.shiftName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldBg),
                            border = BorderStroke(1.5.dp, EmeraldSuccess),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "${Localization.get("check_in", isArabic)} (IN): ${result.record.checkInFormatted}",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "${TimeUtil.todayDateString()} (Asia/Riyadh)",
                                    fontSize = 12.sp,
                                    color = EmeraldSuccess.copy(alpha = 0.8f)
                                )
                                if (result.lateMinutes > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${Localization.get("late", isArabic)}: ${result.lateMinutes} min",
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Localization.get("ontime", isArabic),
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onNotifyBoss(result) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notify_boss_whatsapp_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${Localization.get("notify_boss_whatsapp", isArabic)} (\"$empName coming\")",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (bossWhatsAppNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${Localization.get("boss_number_saved_hint", isArabic)}: $bossWhatsAppNumber",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is ScanResult.SuccessCheckOut -> {
                        val empName = if (isArabic && result.employee.nameAr.isNotBlank()) result.employee.nameAr else result.employee.name
                        Text(
                            text = empName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${result.employee.id} • ${result.shiftName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.5.dp, PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "${Localization.get("check_out", isArabic)} (OUT): ${result.record.checkOutFormatted}",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "${Localization.get("working_hours", isArabic)}: ${TimeUtil.formatCompletedWorkingDuration(result.totalHours, isArabic)}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "${TimeUtil.todayDateString()} (Asia/Riyadh)",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onNotifyBoss(result) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notify_boss_whatsapp_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${Localization.get("notify_boss_whatsapp", isArabic)} (\"$empName out\")",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (bossWhatsAppNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${Localization.get("boss_number_saved_hint", isArabic)}: $bossWhatsAppNumber",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is ScanResult.DuplicateWarning -> {
                        Text(
                            text = result.employeeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Localization.get("duplicate_scan_warning", isArabic),
                            color = AmberWarning,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    is ScanResult.InactiveEmployee -> {
                        Text(
                            text = result.employeeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Localization.get("employee_inactive_warning", isArabic),
                            color = RoseDanger,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is ScanResult.NotFound -> {
                        Text(
                            text = "Code: ${result.scannedCode}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Localization.get("employee_not_found", isArabic),
                            color = RoseDanger,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is ScanResult.Error -> {
                        Text(text = result.message, color = RoseDanger)
                    }
                }
            }
        },
        confirmButton = {
            if (result is ScanResult.PendingConfirmation) {
                val btnColor = if (selectedIsCheckIn) EmeraldSuccess else PrimaryBlue
                val btnText = if (selectedIsCheckIn) Localization.get("confirm_check_in", isArabic) else Localization.get("confirm_check_out", isArabic)
                Button(
                    onClick = { onConfirmAttendance(result.copy(isCheckIn = selectedIsCheckIn)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    modifier = Modifier.testTag("confirm_attendance_btn")
                ) {
                    Text(btnText, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(Localization.get("done", isArabic), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result is ScanResult.PendingConfirmation) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Localization.get("cancel", isArabic))
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRequestOverride()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("admin_override_btn")
                ) {
                    Text(Localization.get("admin_correction", isArabic))
                }
            }
        }
    )
}

private fun toByteArray(buffer: ByteBuffer): ByteArray {
    buffer.rewind()
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    return data
}
