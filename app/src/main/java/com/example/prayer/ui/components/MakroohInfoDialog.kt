package com.example.prayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.prayer.model.PrayerSchedule
import com.example.prayer.util.DateUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MakroohInfoDialog(
    schedule: PrayerSchedule,
    onDismiss: () -> Unit
) {
    val isBangla = isBanglaLanguage()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH) }
    fun fmt(time: java.time.LocalTime): String = DateUtils.localizeTime(time.format(timeFormatter), isBangla)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFF87171).copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBangla) "মাকরূহ ও নিষিদ্ধ সময়সূচি" else "Makruh & Forbidden Times",
                            color = Color.White,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isBangla) "বন্ধ করুন" else "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isBangla) {
                        "হানাফী ফিকহ অনুযায়ী যে সময়গুলোতে নামাজ পড়া সম্পূর্ণ নিষিদ্ধ (মাকরূহ তাহরীমী) অথবা বিনা কারণে বিলম্ব করা মাকরূহ। অ্যাপের নামাজের সময়সূচিতে এই নিষিদ্ধ সময়গুলো স্বয়ংক্রিয়ভাবে বাদ দেওয়া হয়েছে।"
                    } else {
                        "Times when Salah is strictly prohibited (Makruh Tahrimi) or disliked to delay in Hanafi Fiqh. Displayed prayer ranges automatically exclude these windows."
                    },
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Sunrise
                LiquidGlassForbiddenCard(
                    title = if (isBangla) "১. সূর্যোদয়ের সময় (তুলুয়ে আফতাব)" else "1. Sunrise (Tulu-e-Aftab)",
                    timeRange = "${fmt(schedule.sunriseMakroohStart)} – ${fmt(schedule.sunriseMakroohEnd)}",
                    rule = if (isBangla) {
                        "সূর্যোদয়ের পর ১৫ মিনিট। এই সময়ে সকল ফরজ, সুন্নত ও নফল নামাজ এবং তিলাওয়াতে সিজদা সম্পূর্ণ নিষিদ্ধ। এরপর ইশরাকের ওয়াক্ত শুরু হয়।"
                    } else {
                        "15 minutes after sunrise. All Salah and Sajdah are strictly prohibited. Once passed, Ishraq begins."
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Zawal
                LiquidGlassForbiddenCard(
                    title = if (isBangla) "২. দ্বিপ্রহরের সময় (জাওয়াল)" else "2. Midday Zenith (Zawal)",
                    timeRange = "${fmt(schedule.zawalMakroohStart)} – ${fmt(schedule.zawalMakroohEnd)}",
                    rule = if (isBangla) {
                        "যোহরের ওয়াক্ত শুরুর পূর্বের ১০ মিনিট (সূর্য ঠিক মাথার উপরে থাকার সময়)। এ সময় সকল প্রকার নামাজ নিষিদ্ধ।"
                    } else {
                        "10 minutes before Duhr begins (exact solar zenith). All Salah are strictly prohibited."
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Pre-Sunset
                LiquidGlassForbiddenCard(
                    title = if (isBangla) "৩. সূর্যাস্তের পূর্ব মুহূর্ত (গুরুবে আফতাব)" else "3. Pre-Sunset (Ghuroob-e-Aftab)",
                    timeRange = "${fmt(schedule.sunsetMakroohStart)} – ${fmt(schedule.sunsetMakroohEnd)}",
                    rule = if (isBangla) {
                        "মাগরিবের পূর্বের ১৫ মিনিট। আসরের বৈধ (মাকরূহমুক্ত) সময় ${fmt(schedule.asrEnd)}-এ শেষ হয়।"
                    } else {
                        "15 minutes before Maghrib starts. Permitted Asr time ends at ${fmt(schedule.asrEnd)} before this window begins."
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Post-Midnight Isha Delay (Nisf al-Layl)
                LiquidGlassForbiddenCard(
                    title = if (isBangla) "৪. মধ্যরাতের পর ইশা বিলম্ব (নিসফুল লাইল)" else "4. Post-Midnight Isha Delay (Nisf al-Layl)",
                    timeRange = "${fmt(schedule.midnight)} – ${fmt(schedule.ishaMaxEnd)}",
                    rule = if (isBangla) {
                        "মাগরিব ও ফজরের মধ্যবর্তী সময় হলো শারঈ মধ্যরাত (${fmt(schedule.midnight)})। বিনা ওজরে ইশার ফরজ নামাজ মধ্যরাতের পর বিলম্ব করা মাকরূহ, তবে তাহাজ্জুদ ও নফল নামাজের জন্য এটি সর্বোত্তম সময়।"
                    } else {
                        "Islamic Midnight is the midpoint between Maghrib and Fazr (${fmt(schedule.midnight)}). Delaying Isha Farz past Midnight without excuse is Makruh, though Tahajjud/Nafl are virtuous."
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Hadith Reference
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isBangla) {
                            "\"রাসূলুল্লাহ (সা.) তিনটি সময়ে আমাদের নামাজ পড়তে ও মৃতদের দাফন করতে নিষেধ করেছেন: সূর্য উদিত হওয়ার সময় যতক্ষণ না তা পূর্ণরূপে উপরে ওঠে, ঠিক দ্বিপ্রহরে সূর্য মাথার উপরে থাকার সময় যতক্ষণ না তা ঢলে পড়ে, এবং সূর্য অস্ত যাওয়ার সময় যতক্ষণ না তা সম্পূর্ণ ডুবে যায়; আর ইশার সময় হলো অর্ধরাত্রি পর্যন্ত।\" (সহীহ মুসলিম ৬১২, ৮৩১)"
                        } else {
                            "\"There are three times at which the Messenger of Allah forbade us to pray or to bury our dead: when the sun has clearly started to rise until it is fully risen, when it is directly overhead at midday until it has passed its meridian, and when the sun starts to set until it has completely set, and the time of Isha is until the middle of the night.\" (Sahih Muslim 612, 831)"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassForbiddenCard(
    title: String,
    timeRange: String,
    rule: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.70f))
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeRange,
                color = Color(0xFFF87171),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = rule,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                lineHeight = 14.5.sp
            )
        }
    }
}
