# Course Advisor System — Kotlin Edition 🌊

This is the official Kotlin implementation of the **Course Advisor System** for the **Bachelor of Science in Marine Science** program at **Mindanao State University at Naawan** (Curriculum AY 2025–2026, Ref: `MSUN-CFMS-POS-002-2025-REV00`).

---

## 🚀 Quick Start

You can run the application in two ways:

### Option 1: Standalone Single-File (`CourseAdvisor.kt`)

No build system or dependencies required. Open [CourseAdvisor.kt](file:///c:/Users/user/Downloads/Course%20Advisor%20System/CourseAdvisor.kt) in:
- **IntelliJ IDEA** or **Android Studio**: Right-click anywhere in the file and select **Run 'CourseAdvisorKt'**.
- **Command Line** (if `kotlinc` is installed):
  ```bash
  kotlinc CourseAdvisor.kt -include-runtime -d CourseAdvisor.jar
  java -jar CourseAdvisor.jar
  ```

---

### Option 2: Idiomatic Gradle Project (`kotlin/`)

A standard modular Gradle project with clean package structure and automated unit tests.

#### Project Structure:
```
kotlin/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── src/
    ├── main/kotlin/com/courseadvisor/
    │   ├── Main.kt                    # Application entrypoint
    │   ├── model/
    │   │   ├── Course.kt              # Course & WorkloadEvaluation data classes
    │   │   ├── Category.kt            # CourseCategory enum & derivation rules
    │   │   └── StudentProfile.kt      # Student profile & progress state
    │   ├── data/
    │   │   └── CurriculumData.kt      # Complete 60-course curriculum dataset
    │   ├── service/
    │   │   └── CourseAdvisorEngine.kt # Prerequisite graph, standing checks, plan generator
    │   └── cli/
    │       ├── TerminalStyle.kt       # ANSI styling, colors & ASCII progress bars
    │       └── CourseAdvisorCli.kt    # Full interactive terminal menu application
    └── test/kotlin/com/courseadvisor/
        └── CourseAdvisorEngineTest.kt # Automated unit tests for business logic
```

#### Opening in IntelliJ IDEA or Android Studio:
1. Open IntelliJ IDEA or Android Studio.
2. Select **File → Open...** and choose the `kotlin` directory.
3. IntelliJ will automatically detect Gradle, download the Kotlin standard library, and index the project.
4. Open `src/main/kotlin/com/courseadvisor/Main.kt` and click the green ▶ Run button.

---

## ✨ Implemented Features

| Feature | Description |
|---|---|
| **Complete 60-Course Dataset** | Includes all official subjects from Year 1 Sem 1 through Year 4 Sem 2, unit counts, categories, and codes. |
| **Prerequisite DAG Engine** | Direct and transitive prerequisite calculation, dependency trees, and "unlocked by" analysis. |
| **Third-Year Standing Rule** | Strictly enforces that `MRB195` and `MRB198` require completion of all Year 1 & Year 2 courses (terms 0..3). |
| **Co-requisite Bundling** | Automatically pairs Lecture and Lab courses together (e.g. `PHY101` + `PHY101.1`, `CHM011` + `CHM011.1`). |
| **Automated Plan Generator** | Recommends a balanced course load based on student confidence level (12, 15, or 18 units max cap). |
| **Workload Stress Index** | Evaluates semester stress index `[15..95]` using the curriculum formula `round(units * 2.1 + (10 - confidence) * 3)`. |
| **Prerequisite Pathway Visualizer** | Interactive traces for Marine Ecology (`BIO117 → MRB132 → MRB134 → MRB145`), Research (`STT034 → MRB195 → MRB196 → MRB199`), and Oceanography tracks. |
| **Interactive Terminal UI** | Clean ANSI color badges, progress bars, search, filtering, and student profile management. |
| **Plan Export** | Exports the finalized semester plan to `marine-science-plan.txt`. |
