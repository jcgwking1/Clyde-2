package com.courseadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.model.Course
import com.courseadvisor.model.CourseCategory
import com.courseadvisor.ui.theme.*

/**
 * Modifier to draw minimalist bioluminescent jellyfish & fluid ocean currents on inner cards
 */
fun Modifier.subtleOceanContour(): Modifier = this.drawBehind {
    val strokeColor = OceanPrimary.copy(alpha = 0.065f)
    val fillColor = OceanPrimary.copy(alpha = 0.025f)
    val strokeWidth = 1.3f.dp.toPx()
    val width = size.width
    val height = size.height

    // 1. Minimalist Jellyfish Dome (Bell) anchored towards the right
    val jx = width * 0.78f
    val jy = height * 0.35f
    val jw = width * 0.16f
    val jh = height * 0.22f

    val bellPath = Path().apply {
        moveTo(jx, jy + jh)
        cubicTo(jx, jy, jx + jw, jy, jx + jw, jy + jh)
        cubicTo(jx + jw * 0.75f, jy + jh * 0.95f, jx + jw * 0.25f, jy + jh * 0.95f, jx, jy + jh)
        close()
    }
    drawPath(bellPath, color = fillColor)
    drawPath(bellPath, color = strokeColor, style = Stroke(width = strokeWidth))

    // Inner rim accent line
    val innerBell = Path().apply {
        moveTo(jx + jw * 0.15f, jy + jh * 0.9f)
        cubicTo(jx + jw * 0.25f, jy + jh * 0.5f, jx + jw * 0.75f, jy + jh * 0.5f, jx + jw * 0.85f, jy + jh * 0.9f)
    }
    drawPath(innerBell, color = strokeColor.copy(alpha = 0.04f), style = Stroke(width = strokeWidth * 0.8f))

    // 2. Flowing S-curve ribbon tentacles
    val t1 = Path().apply {
        moveTo(jx + jw * 0.2f, jy + jh)
        cubicTo(jx + jw * 0.1f, jy + jh * 1.6f, jx + jw * 0.4f, jy + jh * 2.2f, jx + jw * 0.15f, height)
    }
    val t2 = Path().apply {
        moveTo(jx + jw * 0.45f, jy + jh)
        cubicTo(jx + jw * 0.35f, jy + jh * 1.5f, jx + jw * 0.6f, jy + jh * 2.1f, jx + jw * 0.4f, height)
    }
    val t3 = Path().apply {
        moveTo(jx + jw * 0.7f, jy + jh)
        cubicTo(jx + jw * 0.85f, jy + jh * 1.6f, jx + jw * 0.55f, jy + jh * 2.3f, jx + jw * 0.8f, height)
    }
    val t4 = Path().apply {
        moveTo(jx + jw * 0.85f, jy + jh)
        cubicTo(jx + jw * 1.1f, jy + jh * 1.7f, jx + jw * 0.8f, jy + jh * 2.4f, jx + jw * 1.05f, height)
    }

    drawPath(t1, color = strokeColor, style = Stroke(width = strokeWidth))
    drawPath(t2, color = strokeColor, style = Stroke(width = strokeWidth))
    drawPath(t3, color = strokeColor, style = Stroke(width = strokeWidth))
    drawPath(t4, color = strokeColor.copy(alpha = 0.045f), style = Stroke(width = strokeWidth * 0.8f))

    // 3. Fluid Ambient Ribbon Streamlines across the card base
    val s1 = Path().apply {
        moveTo(width * 0.15f, height * 0.90f)
        cubicTo(width * 0.35f, height * 0.65f, width * 0.6f, height * 0.95f, width, height * 0.70f)
    }
    drawPath(s1, color = strokeColor.copy(alpha = 0.04f), style = Stroke(width = strokeWidth * 0.8f))

    // 4. Micro Bioluminescent Floating Bubbles
    drawCircle(color = OceanPrimary.copy(alpha = 0.06f), radius = 2.5f.dp.toPx(), center = Offset(jx - jw * 0.2f, jy + jh * 1.2f))
    drawCircle(color = OceanPrimary.copy(alpha = 0.05f), radius = 3.5f.dp.toPx(), center = Offset(jx + jw * 1.25f, jy + jh * 0.8f))
    drawCircle(color = OceanPrimary.copy(alpha = 0.05f), radius = 2f.dp.toPx(), center = Offset(jx - jw * 0.5f, jy + jh * 2f))
    drawCircle(color = OceanPrimary.copy(alpha = 0.06f), radius = 1.8f.dp.toPx(), center = Offset(jx + jw * 0.9f, jy + jh * 1.8f))
}

@Composable
fun CourseRowItem(
    course: Course,
    isCompleted: Boolean,
    isEligible: Boolean,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceWhite, OceanSurface)))
                .subtleOceanContour()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (course.category) {
                CourseCategory.MARINE_SCIENCE -> Icons.Default.Water
                CourseCategory.GENERAL_EDUCATION -> Icons.AutoMirrored.Filled.MenuBook
                CourseCategory.FOUNDATION -> Icons.Default.Spa
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OceanLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OceanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${course.code} · ${course.units} units · ${course.category.displayName}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val (statusText, statusBg, statusColor) = when {
                isCompleted -> Triple("Completed", OceanLight, OceanPrimary)
                isEligible -> Triple("Ready", OceanLight, OceanSecondary)
                else -> Triple("Prereqs", WarningAmberBg, WarningAmber)
            }

            Surface(
                color = statusBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (trailingAction != null) {
                trailingAction()
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(SurfaceWhite, OceanSurface)))
                .subtleOceanContour()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OceanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = OceanPrimary,
                    trackColor = OceanLight
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = detail,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
