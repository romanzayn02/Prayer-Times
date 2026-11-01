package com.example.prayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.model.PrayerSettingsRepository
import com.example.prayer.model.UserLocation
import com.example.prayer.util.DateUtils
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

@Composable
fun LocationPickerDialog(
    currentLocation: UserLocation,
    isRefreshingGps: Boolean,
    onRefreshGps: () -> Unit,
    onSelectLocation: (UserLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val isBangla = isBanglaLanguage()
    val themeColors = currentPrayerThemeColors()
    var showCustomInput by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf(currentLocation.locationName) }
    var customLatText by remember { mutableStateOf(String.format(Locale.US, "%.4f", currentLocation.latitude)) }
    var customLngText by remember { mutableStateOf(String.format(Locale.US, "%.4f", currentLocation.longitude)) }
    var customInputError by remember { mutableStateOf<String?>(null) }

    val resolvedZone = remember(currentLocation) {
        PrayerCalculator.resolveZoneId(currentLocation.latitude, currentLocation.longitude)
    }
    val resolvedOffsetHours = remember(currentLocation) {
        PrayerCalculator.resolveTimeZoneOffset(LocalDate.now(), currentLocation.latitude, currentLocation.longitude)
    }
    val offsetFormatted = remember(resolvedOffsetHours, isBangla) {
        val sign = if (resolvedOffsetHours >= 0) "+" else "-"
        val totalMinutes = (abs(resolvedOffsetHours) * 60.0).toInt()
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        val raw = String.format(Locale.US, "UTC%s%02d:%02d", sign, hrs, mins)
        DateUtils.localizeDigits(raw, isBangla)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppleLiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("location_picker_dialog"),
            shape = RoundedCornerShape(26.dp),
            tintColor = Color(0xFF38BDF8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            Color(0xFF10B981).copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(1.dp, themeColors.skyAccent.copy(alpha = 0.70f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = themeColors.skyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "অবস্থান ও সৌর স্থানাঙ্ক নির্ধারণ" else "Location & Solar Calibration",
                                color = themeColors.primaryText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isBangla) "জিপিএস অটো-ডিটেক্ট, শহর তালিকা ও কাস্টম স্থানাঙ্ক" else "GPS auto-detect, city presets & custom coordinates",
                                color = themeColors.secondaryText,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isBangla) "বন্ধ করুন" else "Close",
                            tint = themeColors.primaryText.copy(alpha = 0.80f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Location Telemetry Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(themeColors.insetPodSurface)
                        .border(1.dp, themeColors.skyAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = DateUtils.localizeLocationName(currentLocation.locationName, isBangla),
                                color = themeColors.primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (currentLocation.isGps) Color(0xFF10B981).copy(alpha = 0.22f)
                                        else Color(0xFF38BDF8).copy(alpha = 0.22f)
                                    )
                                    .border(
                                        0.8.dp,
                                        if (currentLocation.isGps) themeColors.emeraldAccent else themeColors.skyAccent,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (currentLocation.isGps) {
                                        if (isBangla) "লাইভ জিপিএস" else "LIVE GPS"
                                    } else {
                                        if (isBangla) "নির্ধারিত" else "CALIBRATED"
                                    },
                                    color = if (currentLocation.isGps) themeColors.emeraldAccent else themeColors.skyAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val coordStr = DateUtils.localizeDigits(
                            String.format(
                                Locale.US,
                                "%.4f° N, %.4f° E",
                                currentLocation.latitude,
                                currentLocation.longitude
                            ),
                            isBangla
                        )
                        Text(
                            text = if (isBangla) "স্থানাঙ্ক: $coordStr" else "Coordinates: $coordStr",
                            color = themeColors.emeraldAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBangla) "টাইমজোন: ${resolvedZone.id} ($offsetFormatted)" else "Timezone: ${resolvedZone.id} ($offsetFormatted)",
                            color = themeColors.secondaryText,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row: Live GPS Refresh + Custom Coordinates Toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onRefreshGps() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detect_gps_button")
                    ) {
                        if (isRefreshingGps) {
                            CircularProgressIndicator(
                                color = Color(0xFF022C22),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFF022C22),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshingGps) {
                                if (isBangla) "খোঁজা হচ্ছে..." else "Syncing..."
                            } else {
                                if (isBangla) "লাইভ জিপিএস" else "Use Live GPS"
                            },
                            color = Color(0xFF022C22),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showCustomInput = !showCustomInput },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_coordinates_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocationAlt,
                            contentDescription = null,
                            tint = themeColors.skyAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showCustomInput) {
                                if (isBangla) "বন্ধ করুন" else "Hide Custom"
                            } else {
                                if (isBangla) "কাস্টম স্থানাঙ্ক" else "Custom Lat/Lng"
                            },
                            color = themeColors.skyAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                if (showCustomInput) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(themeColors.subtleSurface)
                            .border(1.dp, themeColors.subtleBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isBangla) "ম্যানুয়াল স্থানাঙ্ক নির্ধারণ" else "MANUAL COORDINATE CALIBRATION",
                                color = themeColors.skyAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = {
                                    Text(
                                        if (isBangla) "স্থানের নাম (যেমন: স্থানীয় মসজিদ)" else "Location Name (e.g. My Mosque)",
                                        fontSize = 11.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = themeColors.primaryText,
                                    unfocusedTextColor = themeColors.primaryText,
                                    focusedBorderColor = themeColors.skyAccent,
                                    unfocusedBorderColor = themeColors.subtleBorder,
                                    focusedLabelColor = themeColors.skyAccent,
                                    unfocusedLabelColor = themeColors.secondaryText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_location_name_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = customLatText,
                                    onValueChange = { customLatText = it },
                                    label = {
                                        Text(
                                            if (isBangla) "অক্ষাংশ (-90..90)" else "Latitude (-90 to 90)",
                                            fontSize = 11.sp
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = themeColors.primaryText,
                                        unfocusedTextColor = themeColors.primaryText,
                                        focusedBorderColor = themeColors.skyAccent,
                                        unfocusedBorderColor = themeColors.subtleBorder,
                                        focusedLabelColor = themeColors.skyAccent,
                                        unfocusedLabelColor = themeColors.secondaryText
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_lat_input")
                                )

                                OutlinedTextField(
                                    value = customLngText,
                                    onValueChange = { customLngText = it },
                                    label = {
                                        Text(
                                            if (isBangla) "দ্রাঘিমাংশ (-180..180)" else "Longitude (-180 to 180)",
                                            fontSize = 11.sp
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = themeColors.primaryText,
                                        unfocusedTextColor = themeColors.primaryText,
                                        focusedBorderColor = themeColors.skyAccent,
                                        unfocusedBorderColor = themeColors.subtleBorder,
                                        focusedLabelColor = themeColors.skyAccent,
                                        unfocusedLabelColor = themeColors.secondaryText
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_lng_input")
                                )
                            }

                            if (customInputError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = customInputError!!,
                                    color = themeColors.crimsonAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val lat = customLatText.trim().toDoubleOrNull()
                                    val lng = customLngText.trim().toDoubleOrNull()
                                    if (lat == null || lat !in -90.0..90.0) {
                                        customInputError = if (isBangla) {
                                            "অনুগ্রহ করে -৯০ থেকে ৯০ এর মধ্যে সঠিক অক্ষাংশ লিখুন।"
                                        } else {
                                            "Please enter a valid latitude between -90 and 90."
                                        }
                                    } else if (lng == null || lng !in -180.0..180.0) {
                                        customInputError = if (isBangla) {
                                            "অনুগ্রহ করে -১৮০ থেকে ১৮০ এর মধ্যে সঠিক দ্রাঘিমাংশ লিখুন।"
                                        } else {
                                            "Please enter a valid longitude between -180 and 180."
                                        }
                                    } else {
                                        customInputError = null
                                        val label = customName.trim().ifEmpty {
                                            String.format(Locale.US, "%.4f°N, %.4f°E", lat, lng)
                                        }
                                        onSelectLocation(
                                            UserLocation(
                                                latitude = lat,
                                                longitude = lng,
                                                locationName = label,
                                                isGps = false
                                            )
                                        )
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_custom_coordinates_button")
                            ) {
                                Text(
                                    text = if (isBangla) "কাস্টম স্থানাঙ্ক সংরক্ষণ করুন" else "Save Custom Coordinates",
                                    color = Color(0xFF082F49),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preset Cities List
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = themeColors.skyAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "বাংলাদেশ ও বিশ্বের প্রধান শহরসমূহ" else "BANGLADESH & WORLDWIDE CITY PRESETS",
                        color = themeColors.skyAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PrayerSettingsRepository.presetCities.forEach { preset ->
                        val isSelected = !currentLocation.isGps &&
                            abs(currentLocation.latitude - preset.latitude) < 0.01 &&
                            abs(currentLocation.longitude - preset.longitude) < 0.01

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF10B981).copy(alpha = 0.22f)
                                    else themeColors.subtleSurface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColors.emeraldStrong else themeColors.subtleBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectLocation(
                                        UserLocation(
                                            latitude = preset.latitude,
                                            longitude = preset.longitude,
                                            locationName = preset.name,
                                            isGps = false
                                        )
                                    )
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .testTag("city_preset_${preset.name.substringBefore(",").lowercase().replace(" ", "_")}")
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.localizedName(isBangla),
                                    color = themeColors.primaryText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val latLngText = DateUtils.localizeDigits(
                                    String.format(Locale.US, "%.2f°, %.2f°", preset.latitude, preset.longitude),
                                    isBangla
                                )
                                Text(
                                    text = "${preset.localizedRegion(isBangla)} • $latLngText",
                                    color = themeColors.secondaryText,
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = if (isBangla) "নির্বাচিত" else "Selected",
                                    tint = themeColors.emeraldStrong,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
