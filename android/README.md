# J.A.K.E.V Edu — Native Android App (Jetpack Compose) 📱

**J.A.K.E.V Edu** is a native Android application for the **Course Advisor System** (Bachelor of Science in Marine Science at Mindanao State University at Naawan, Curriculum AY 2025–2026, Ref: `MSUN-CFMS-POS-002-2025-REV00`).

Built with **Kotlin 2.0**, **Jetpack Compose**, and **Material 3**, utilizing edge-to-edge layouts and an Emerald marine color theme.

---

## 🌟 Features & Screens

| Screen / Feature | Description |
|---|---|
| **Overview Screen** | Degree progress bar (52%), 4 KPI stat cards (Units Earned, Courses Done, Standing, Advisor Load), dynamic Workload Stress gauge, and one-tap recommended enrollment. |
| **Planner Screen** | Semester selector (Year 1 Sem 1 through Year 4 Sem 2), planned subjects list, unit counter, workload index indicator, and formatted plan export. |
| **Catalog Screen** | Instant live search across all 59 courses by code or title, category filter chips (*All Subjects*, *Marine Science*, *Foundation*, *General Education*), and full syllabus details. |
| **Pathways Screen** | Visual prerequisite dependency chains for *Marine Ecology Track*, *Research & Thesis Track*, and *Oceanography Track*. |
| **Settings & Profile** | Student profile editor (Name, ID), subject confidence slider (1..10) that dynamically adjusts the recommended unit capacity, and progress reset. |
| **Course Detail Dialog** | Tap any course to inspect prerequisites, co-requisite requirements, and toggle between *Completed* or *Add to Plan*. |
| **Automated Plan Generator** | Auto-schedules eligible courses adhering to prerequisite DAG checks, co-requisite bundling (Lecture + Lab pairs), and student confidence caps. |

---

## 🚀 How to Run the App in Android Studio

1. **Open the Project**:
   - Launch **Android Studio**.
   - Click **File ➔ Open...** (or click **Open** on the welcome dialog).
   - Navigate to and select the `android` folder:
     ```
     <repository-path>/android
     ```
   - Click **Trust Project**.

2. **Wait for Gradle Sync**:
   - Android Studio will automatically sync the project using **Gradle 8.10.2** and **Android Gradle Plugin 8.8.2**.
   - Ensure the Gradle JDK is set to **Embedded JDK 21** (JBR 21) in **Settings ➔ Build, Execution, Deployment ➔ Build Tools ➔ Gradle**.

3. **Select Device & Run**:
   - Select your connected emulator (e.g., `Medium_Phone`) or physical device from the top target dropdown.
   - Click the green **Run (▶)** button or press <kbd>Shift</kbd> + <kbd>F10</kbd>.
   - The app **J.A.K.E.V Edu** will launch directly on your screen!

---

## 💻 How to Build & Run from Command Line

Ensure `ANDROID_HOME` or `local.properties` points to your Android SDK.

### Build Debug APK:
```bash
# On Windows PowerShell
cd android
.\gradlew.bat assembleDebug

# On macOS / Linux
cd android
./gradlew assembleDebug
```
The compiled APK will be located at:
`android/app/build/outputs/apk/debug/app-debug.apk`

### Install and Launch on Connected Device / Emulator:
```bash
# Install APK
.\gradlew.bat installDebug

# Start MainActivity directly
adb shell am start -n com.courseadvisor/.MainActivity
```

---

## 🧪 How to Test

### Run Unit Tests:
```bash
cd android
.\gradlew.bat test
```

### Run Android Instrumented Tests (requires running emulator/device):
```bash
cd android
.\gradlew.bat connectedAndroidTest
```

---

## 📁 Source Code Structure

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   └── values/
│       │       ├── strings.xml             # App name: "J.A.K.E.V Edu"
│       │       └── themes.xml              # Material 3 theme definitions
│       └── java/com/courseadvisor/
│           ├── MainActivity.kt             # Single Activity entry point
│           ├── model/
│           │   ├── Course.kt               # Course & WorkloadEvaluation data classes
│           │   ├── Category.kt             # CourseCategory enum & badges
│           │   └── StudentProfile.kt       # Student profile & progress state
│           ├── data/
│           │   └── CurriculumData.kt       # Complete 59-course BS Marine Science curriculum
│           ├── service/
│           │   └── CourseAdvisorEngine.kt  # Prerequisite graph, standing checks, plan generator
│           └── ui/
│               ├── CourseAdvisorApp.kt     # Bottom navigation shell, top bar & dialogs
│               ├── theme/                  # Emerald Marine palette & Typography
│               ├── components/             # Reusable cards, badges & stress gauge
│               └── screens/                # Overview, Planner, Catalog, Pathways, Settings
├── build.gradle.kts                        # Root build configuration
├── settings.gradle.kts                     # Project module definitions
└── gradle.properties                       # Memory & JBR toolchain configuration
```
