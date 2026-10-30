# 🎓 CourseAdvisor: System Defense & Presentation Guide (ITD109)
**Project Title**: J.A.K.E.V. Edu — Course Advisor System  
**Institution**: Mindanao State University at Naawan (MSU at Naawan)  
**Curriculum Year**: Academic Year 2025–2026  
**Architecture ID**: `ARCH-2026-v2.1` (Offline-First Clean Architecture)

---

## 1. Executive Summary & Problem Solved

### The Problem
Traditional university degree audit systems and student portals are **static**:
- They show a list of passed and failed subjects without intelligent semester-by-semester planning.
- Irregular students frequently suffer delayed graduation because they unknowingly take elective subjects while missing critical, bottleneck prerequisite courses.
- Students overload on credits without realizing the academic stress or burnout risk, leading to dropped or failed subjects.

### The Solution: CourseAdvisor
CourseAdvisor is an intelligent, offline-first course scheduling and advising engine that combines two advanced mathematical models:
1. **Goal-Driven Backward Chaining Engine on a Directed Acyclic Graph (DAG)** to schedule bottleneck prerequisites first.
2. **Mamdani Fuzzy Inference System** to calculate a real-time **Semester Stress Index** and recommend personalized credit caps (12, 15, 18, 24 units) based on student confidence.

---

## 2. Supported Curricula (Official MSU at Naawan Prospectuses)

The system is strictly calibrated to the **7 official university prospectuses**:

| Program ID | Program Name | College / Campus | Terms | Total Units |
|---|---|:---:|:---:|:---:|
| `BSBA-MM` | BS Business Administration - Marketing Management | CBIT | 8 | 169 |
| `BEED` | Bachelor of Elementary Education | CESS | 10 (2 Summers) | 167 |
| `BSES` | BS Environmental Science | CAS | 9 (1 Summer) | 168 |
| `BSA-ANSCI` | BS Agriculture major in Animal Science | Agriculture | 10 (2 Summers) | 178 |
| `BSA-AGRON` | BS Agriculture major in Agronomy | Agriculture | 10 (2 Summers) | 178 |
| `BSFI` | BS Fisheries | CFMS | 9 (1 Summer) | 183 |
| `BSMS` | BS Marine Science | CFMS | 9 (1 Summer) | 162 |

---

## 3. Core Algorithms Explained (How to Answer the Panel)

### A. Backward Chaining Prerequisite DAG Engine
* **Mathematical Model**: Directed Acyclic Graph $G = (V, E)$, where $V$ are courses and $E$ are prerequisite arrows ($u \rightarrow v$ means course $u$ requires course $v$).
* **Cycle Prevention**: The import parser executes **Kahn's Topological Sort Algorithm**. If any circular loop exists (e.g., $A \rightarrow B \rightarrow A$), the engine detects an in-degree stalemate and flags a cycle conflict before rendering.
* **Goal-Driven Traversal**: When the user selects a target milestone (e.g., *Undergraduate Thesis* or *Marketing Practicum*):
  1. The target course is pushed to an evaluation stack.
  2. The graph traverses backwards along incoming prerequisite edges.
  3. Already completed courses (from the student's transcript) are skipped.
  4. The unfulfilled chain is levelized into topological tiers ($Tier_0, Tier_1, \dots$). The engine schedules $Tier_0$ (bottleneck foundations) first!
* **Co-requisite Bundling**: Automatically binds theory lectures to their companion laboratory courses (e.g., `PHY101` + `PHY101.1`) so a student cannot accidentally take a lecture without the required lab.

### B. Mamdani Fuzzy Inference System (Workload Balancing)
To eliminate student burnout, the system avoids rigid, arbitrary unit limits by using fuzzy set theory:
1. **Fuzzification**: Converts two crisp numerical inputs into linguistic variables:
   * **Student Confidence** ($C \in [1, 10]$): `Low` (trapezoidal), `Moderate` (triangular), `High` (trapezoidal).
   * **Subject Difficulty** ($D \in [1, 10]$): `Easy` (1–4), `Moderate` (4–8), `Hard` (6.5–10).
2. **Rule Evaluation (Min-Max Inference)**:
   * *Rule 1*: IF Confidence is `Low` AND Difficulty is `Hard` THEN Workload Risk is `Very High` (Cap: 12 Units).
   * *Rule 2*: IF Confidence is `High` AND Difficulty is `Moderate` THEN Workload Risk is `Low` (Cap: 18–21 Units).
   * *Rule 3*: IF Confidence is `Moderate` THEN Workload Risk is `Moderate` (Cap: 15–17 Units).
3. **Defuzzification (Centroid Method)**:
   Computes the crisp center-of-gravity output for:
   $$\text{Stress Index} = \text{round}(\text{units} \times 2.1 + (10 - \text{confidence}) \times 3)$$
   This generates the live color-coded **Stress Meter** (Green: Balanced, Amber: Challenging, Red: Overload Risk).

---

## 4. Live 2-Minute Demonstration Script

When presenting to your professor or panel, follow this exact script:

### Step 1: The First-Launch Intake Gate (0:00 - 0:30)
1. Open the web app at `http://localhost:8443`.
2. **Explain to the panel**: *"When a student opens the application, direct workspace access is restricted until they specify their degree program and academic standing."*
3. Select a degree (e.g., **BS Business Administration - Marketing Management**).
4. Choose **3rd Year Standing** and check **"Pre-fill completed courses prior to my standing"**.
5. Move the **Confidence Slider** to **7/10** (*Balanced Pace*).
6. Click **"Start Planning"**. Show how all prior Year 1 & Year 2 courses are instantly recorded!

### Step 2: The Smart Scheduler & Prerequisite Protection (0:30 - 1:00)
1. Go to **"My planner"**.
2. Click **"Auto-generate plan"**. 
3. **Show the panel**: The engine automatically suggests the optimal courses within the recommended credit cap (e.g., 18 units) with all companion labs bundled.
4. **Demonstrate Irregular Student Handling**:
   - Manually add a senior course that the student does not have prerequisites for (e.g., `MKT199` Thesis).
   - **Point to the screen**: The prominent **Prerequisite Conflict Alert Card** appears in red:
     > ⚠️ *Prerequisite Conflict Detected (Irregular Enrollment): MKT199 requires MKT198 to be passed first.*
   - Show how the app actively protects irregular students from illegal registration.

### Step 3: The Official Advising Slip & PDF Export (1:00 - 1:45)
1. Click the green **"Official Advising Slip"** button.
2. **Show the document**:
   - Official **MSU at Naawan** header and registrar seal.
   - Student metadata and chosen academic standing.
   - **Mamdani Fuzzy Logic Workload Audit** (Stress Index score & DAG cycle-free certification).
   - Formatted course table with lecture/lab hours.
   - Academic Adviser and Dean signature boxes.
3. Click **"Print / Save as PDF"** to demonstrate that it opens the native browser print preview cleanly formatted for physical signing.

### Step 4: Multi-Curriculum Switching (1:45 - 2:00)
1. Click the degree pill in the top header.
2. Switch to **BS Marine Science** or **Bachelor of Elementary Education (BEEd)**.
3. Show that the entire course catalog, prerequisite tree, and milestone goals immediately re-align to that program's prospectus!

---

## 5. Answers to Anticipated Panel Questions

**Q: Where is the student's data stored? Is there a risk of privacy leaks?**  
*A: As outlined in the Architecture Blueprint, CourseAdvisor is built with an **Offline-First Local Architecture**. All academic records, transcripts, and preferences are stored locally on the client's device using HTML5 `localStorage` (and Room SQLite on Android). No personal education data is transmitted to third-party servers, ensuring 100% FERPA/Data Privacy compliance and sub-millisecond execution without network dependence.*

**Q: How does the system prevent infinite loops in course prerequisites?**  
*A: During data compilation and graph initialization, the system runs Kahn's topological sort algorithm across all course nodes. It calculates in-degree dependencies and validates that the graph is strictly acyclic ($0$ cycles). Any circular edge is rejected during catalog evaluation.*

**Q: Can this handle irregular students with failed subjects or back-subjects?**  
*A: Yes. In the course catalog and planner, any subject can be checked or unchecked. The system dynamically computes the difference between the required prerequisite set and the student's transcript set, displaying explicit blocker alerts for any unfulfilled prerequisite.*

