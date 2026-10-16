package com.courseadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.model.Course
import com.courseadvisor.service.CourseAdvisorEngine
import com.courseadvisor.ui.theme.*

@Composable
fun PrerequisitesScreen(
    engine: CourseAdvisorEngine,
    completedCodes: Set<String>,
    onSelectCourse: (Course) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column {
                Text("Prerequisite Pathways", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("See how today’s foundational courses unlock advanced marine research.", fontSize = 12.sp, color = TextMuted)
            }
        }

        // Track 1: Marine Ecology Pathway
        item {
            PathwayCard(
                title = "Marine Ecology Pathway",
                subtitle = "Cell Biology → Marine Invertebrates → Marine Ecology → Coral Reefs",
                steps = listOf("BIO117", "MRB132", "MRB134", "MRB145"),
                engine = engine,
                completedCodes = completedCodes,
                onSelectCourse = onSelectCourse
            )
        }

        // Track 2: Research Pathway
        item {
            PathwayCard(
                title = "Research & Thesis Pathway",
                subtitle = "Statistics → Fundamentals of Research → Experimental Research → Thesis",
                steps = listOf("STT034", "MRB195", "MRB196", "MRB199"),
                engine = engine,
                completedCodes = completedCodes,
                onSelectCourse = onSelectCourse
            )
        }

        // Track 3: Oceanography & Chemistry Pathway
        item {
            PathwayCard(
                title = "Oceanography Pathway",
                subtitle = "Physics & Calculus → Waves & Optics → Geological Oceanography",
                steps = listOf("PHY101", "PHY102", "OCN121"),
                engine = engine,
                completedCodes = completedCodes,
                onSelectCourse = onSelectCourse
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SageLight)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Curriculum rule: MRB195 and MRB198 require third-year standing (completion of all 1st and 2nd year subjects).",
                        fontSize = 12.sp,
                        color = TextDark,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PathwayCard(
    title: String,
    subtitle: String,
    steps: List<String>,
    engine: CourseAdvisorEngine,
    completedCodes: Set<String>,
    onSelectCourse: (Course) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(14.dp))

            steps.forEachIndexed { index, code ->
                val course = engine.findCourse(code)
                if (course != null) {
                    val isDone = completedCodes.contains(code)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDone) SuccessGreenBg.copy(alpha = 0.4f) else BackgroundMint)
                            .border(
                                width = 1.dp,
                                color = if (isDone) SuccessGreen.copy(alpha = 0.3f) else BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectCourse(course) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDone) SuccessGreen else SageLight
                        ) {
                            Text(
                                text = course.code,
                                color = if (isDone) SurfaceWhite else EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                            Text(
                                text = "${course.units} units · Term ${course.term + 1}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        if (isDone) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
