package com.courseadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.model.CourseCategory
import com.courseadvisor.service.CourseAdvisorEngine
import com.courseadvisor.ui.components.CourseRowItem
import com.courseadvisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    engine: CourseAdvisorEngine,
    completedCodes: Set<String>,
    onSelectCourse: (Course) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CourseCategory?>(null) }

    val categories = listOf(
        null to "All Subjects",
        CourseCategory.MARINE_SCIENCE to "Marine Science",
        CourseCategory.FOUNDATION to "Foundation",
        CourseCategory.GENERAL_EDUCATION to "General Ed"
    )

    val filteredCourses = CurriculumData.ALL_COURSES.filter { course ->
        val matchesCategory = selectedCategory == null || course.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                course.code.contains(searchQuery.trim(), ignoreCase = true) ||
                course.title.contains(searchQuery.trim(), ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search course code or title...", fontSize = 14.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = BorderSubtle
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (category, label) ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = SurfaceWhite,
                        containerColor = SurfaceWhite,
                        labelColor = TextDark
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${filteredCourses.size} subjects · Official 2025–2026 Prospectus",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Course List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredCourses) { course ->
                CourseRowItem(
                    course = course,
                    isCompleted = completedCodes.contains(course.code),
                    isEligible = engine.isEligible(course, completedCodes),
                    onClick = { onSelectCourse(course) }
                )
            }
        }
    }
}
