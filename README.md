# J.A.K.E.V Edu — Course Advisor System 🌊🎓

An academic advising and course progression tracking system designed for the **Bachelor of Science in Marine Science** program at **Mindanao State University at Naawan** (Curriculum AY 2025–2026, Ref: `MSUN-CFMS-POS-002-2025-REV00`).

This repository provides a multi-platform suite:
1. 📱 **Native Android Application (`android/`)** — Built with Kotlin 2.0, Jetpack Compose, and Material 3.
2. 💻 **Kotlin Engine & Interactive CLI (`kotlin/` & `CourseAdvisor.kt`)** — Standalone JVM application, automated JUnit tests, and CLI terminal tool.
3. 🌐 **Modern Web Dashboard (`src/`)** — Built with React, TypeScript, Vite, and Tailwind CSS.

---

## 🌟 Key Features & Academic Logic

- **Complete Curriculum Dataset**: Includes all 59 official courses across Year 1 Sem 1 through Year 4 Sem 2 (168 total units).
- **Prerequisite DAG Engine**: Enforces direct, transitive, and prerequisite trees.
- **Third-Year Standing Rule**: Strictly ensures courses like `MRB195` (Marine Science Seminar) and `MRB198` (Practicum) require completion of all Year 1 & 2 courses.
- **Co-requisite Bundling**: Automatically links theory and laboratory pairings (e.g. `PHY101` + `PHY101.1`, `CHM011` + `CHM011.1`).
- **Dynamic Workload Stress Gauge**: Calculates semester stress index `[15..95]` using:
  $$\text{Stress Index} = \text{round}(\text{units} \times 2.1 + (10 - \text{confidence}) \times 3)$$
- **Automated Plan Generator**: Recommends an optimal semester schedule customized to the student's confidence rating (12, 15, or 18 units max cap).
- **Visual Prerequisite Pathways**: Visualizes milestone chains for:
  - *Marine Ecology Track* (`BIO117 → MRB132 → MRB134 → MRB145`)
  - *Research & Thesis Track* (`STT034 → MRB195 → MRB196 → MRB199`)
  - *Oceanography Track* (`PHY101/CHM011 → MRB121 → MRB122 → MRB123`)

---

## 🚀 How to Run and Test

### 1. Native Android App (`android/`)

#### A. Running in Android Studio (Recommended):
1. Open **Android Studio**.
2. Click **File ➔ Open...** and select the **`android`** folder inside this repository:
   ```
   <repository-folder>/android
   ```
3. Allow Gradle to sync. (The project is configured for **Gradle 8.10.2** and uses Android Studio's **Embedded JDK 21 / JBR**).
4. Select your emulator (e.g. `Medium_Phone`) or physical device in the device dropdown.
5. Click the green **Run (▶)** button or press <kbd>Shift</kbd> + <kbd>F10</kbd>.
6. The app **J.A.K.E.V Edu** will launch directly on the device!

#### B. Building & Running via Command Line:
```bash
# Navigate to the android directory
cd android

# Build the debug APK
# On Windows:
.\gradlew.bat assembleDebug
# On Linux/macOS:
./gradlew assembleDebug

# Install on a connected emulator/device
.\gradlew.bat installDebug

# Launch the app directly
adb shell am start -n com.courseadvisor/.MainActivity
```
The compiled APK will be created at:
`android/app/build/outputs/apk/debug/app-debug.apk`

#### C. Testing the Android App:
```bash
cd android

# Run Unit Tests
.\gradlew.bat test

# Run Android Instrumented Device Tests
.\gradlew.bat connectedAndroidTest
```

---

### 2. Kotlin CLI & Standalone Engine (`kotlin/`)

#### A. Running the Interactive Terminal CLI:
```bash
cd kotlin

# On Windows:
.\gradlew.bat run -q --console=plain

# On Linux/macOS:
./gradlew run -q --console=plain
```

#### B. Running Automated Unit Tests:
```bash
cd kotlin

# On Windows:
.\gradlew.bat test

# On Linux/macOS:
./gradlew test
```
Test results and HTML reports will be generated in `kotlin/build/reports/tests/test/index.html`.

#### C. Running the Single-File Standalone Script (`CourseAdvisor.kt`):
```bash
# Can be opened and run directly in IntelliJ IDEA or compiled with kotlinc:
kotlinc CourseAdvisor.kt -include-runtime -d CourseAdvisor.jar
java -jar CourseAdvisor.jar
```

---

### 3. Web Application Dashboard

#### A. Running Locally:
```bash
# Install dependencies
npm install

# Start Vite dev server
npm run dev
```
Open your browser and navigate to `http://localhost:5173` (or the port specified in terminal output).

#### B. Building for Production:
```bash
npm run build
```

---

## 📁 Repository Directory Structure

```
├── README.md                               # Primary documentation & quickstart guide
├── CourseAdvisor.kt                        # Standalone runnable Kotlin CLI file
├── android/                                # Native Android Jetpack Compose Project
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── res/values/strings.xml      # App name: J.A.K.E.V Edu
│   │       └── java/com/courseadvisor/
│   │           ├── MainActivity.kt         # Edge-to-edge Compose activity
│   │           ├── data/                   # Curriculum data definitions
│   │           ├── model/                  # Domain entities (Course, Profile, etc.)
│   │           ├── service/                # Prerequisite DAG & plan evaluation engine
│   │           └── ui/                     # Compose UI (Overview, Planner, Catalog, etc.)
│   ├── build.gradle.kts
│   ├── gradle.properties
│   └── settings.gradle.kts
├── kotlin/                                 # Modular JVM Gradle Project with Tests
│   ├── src/main/kotlin/com/courseadvisor/  # CLI menu & engine source code
│   ├── src/test/kotlin/com/courseadvisor/  # Automated JUnit test suite
│   └── build.gradle.kts
├── src/                                    # React + Vite + Tailwind CSS Web Application
│   ├── App.tsx                             # Main dashboard component
│   ├── courses.ts                          # TypeScript curriculum dataset
│   └── index.css                           # Styling and theme definitions
├── public/                                 # Static assets & icons
├── package.json                            # Node.js project manifest
├── tsconfig.json                           # TypeScript compiler configuration
└── vite.config.ts                          # Vite build tool configuration
```

---

## 📄 License & Attribution

- **Program**: Bachelor of Science in Marine Science (BSMS)
- **Institution**: Mindanao State University at Naawan (MSU at Naawan)
- **Curriculum Reference**: `MSUN-CFMS-POS-002-2025-REV00` (Academic Year 2025–2026)
