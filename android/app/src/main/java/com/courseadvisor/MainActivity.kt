package com.courseadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.courseadvisor.ui.CourseAdvisorApp
import com.courseadvisor.ui.theme.CourseAdvisorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourseAdvisorTheme {
                CourseAdvisorApp()
            }
        }
    }
}
