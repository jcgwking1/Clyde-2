/**
 * Mamdani Fuzzy Inference System (FIS) for Academic Workload Stress Evaluation
 * Reference: CourseAdvisor System Design Specification (CourseAdvisor-DevBP.docx)
 *
 * Pipeline:
 * 1. Fuzzification (Triangular & Trapezoidal membership functions)
 * 2. Rule Evaluation (Mamdani Min operator for T-norm AND)
 * 3. Aggregation (Max operator for S-norm OR)
 * 4. Defuzzification (Discrete Centroid / Center of Gravity Method)
 */

export type FuzzyStressResult = {
  stressScore: number;          // Crisp Defuzzified value 0..100
  rating: 'Relaxed' | 'Manageable' | 'Demanding' | 'Critical';
  recommendedCredits: number;   // 12, 15, or 18
  description: string;
  linguisticScores: {
    relaxed: number;
    manageable: number;
    demanding: number;
    critical: number;
  };
  dominantState: string;
};

// Triangular Membership Function
export function trimf(x: number, a: number, b: number, c: number): number {
  if (x <= a || x >= c) return 0;
  if (x === b) return 1;
  if (x < b) return (x - a) / (b - a);
  return (c - x) / (c - b);
}

// Trapezoidal Membership Function
export function trapmf(x: number, a: number, b: number, c: number, d: number): number {
  if (x <= a || x >= d) return 0;
  if (x >= b && x <= c) return 1;
  if (x < b) return (x - a) / (b - a);
  return (d - x) / (d - c);
}

/**
 * Evaluates academic workload using a Mamdani Fuzzy Logic Inferencer.
 *
 * @param units Total enrolled/planned credit units (e.g. 12 to 24)
 * @param confidence Student academic confidence (1 to 10)
 * @param avgDifficulty Average course difficulty coefficient (1 to 10)
 */
export function evaluateMamdaniStress(
  units: number,
  confidence: number,
  avgDifficulty: number = 6.2
): FuzzyStressResult {
  // Clamp inputs to universe of discourse
  const c = Math.max(1, Math.min(10, confidence));
  const u = Math.max(3, Math.min(26, units));
  const d = Math.max(1, Math.min(10, avgDifficulty));

  // --- 1. FUZZIFICATION ---
  // Confidence Membership
  const confLow = trapmf(c, 1, 1, 3.5, 5.0);
  const confMed = trimf(c, 3.5, 6.0, 8.0);
  const confHigh = trapmf(c, 6.5, 8.5, 10, 10);

  // Unit Load Membership
  const loadLight = trapmf(u, 3, 3, 11, 14);
  const loadStd = trimf(u, 13, 17, 20);
  const loadHeavy = trapmf(u, 18, 21, 26, 26);

  // Difficulty Membership
  const diffEasy = trapmf(d, 1, 1, 3.5, 5.0);
  const diffMod = trimf(d, 4.0, 6.2, 8.0);
  const diffHard = trapmf(d, 6.5, 8.2, 10, 10);

  // --- 2. RULE EVALUATION (Mamdani Min operator) ---
  // Rule 1: IF Confidence IS Low AND Load IS Heavy THEN Stress IS Critical
  const r1 = Math.min(confLow, loadHeavy);

  // Rule 2: IF Confidence IS Low AND Difficulty IS Hard THEN Stress IS Critical
  const r2 = Math.min(confLow, diffHard);

  // Rule 3: IF Confidence IS Low AND Load IS Std AND Difficulty IS Mod THEN Stress IS Demanding
  const r3 = Math.min(confLow, loadStd, diffMod);

  // Rule 4: IF Confidence IS Low AND Load IS Light THEN Stress IS Manageable
  const r4 = Math.min(confLow, loadLight);

  // Rule 5: IF Confidence IS Med AND Load IS Heavy THEN Stress IS Demanding
  const r5 = Math.min(confMed, loadHeavy);

  // Rule 6: IF Confidence IS Med AND Load IS Std AND Difficulty IS Mod THEN Stress IS Manageable
  const r6 = Math.min(confMed, loadStd, diffMod);

  // Rule 7: IF Confidence IS Med AND Load IS Light THEN Stress IS Relaxed
  const r7 = Math.min(confMed, loadLight);

  // Rule 8: IF Confidence IS High AND Load IS Heavy AND Difficulty IS Hard THEN Stress IS Demanding
  const r8 = Math.min(confHigh, loadHeavy, diffHard);

  // Rule 9: IF Confidence IS High AND Load IS Heavy AND Difficulty IS Mod THEN Stress IS Manageable
  const r9 = Math.min(confHigh, loadHeavy, diffMod);

  // Rule 10: IF Confidence IS High AND Load IS Std THEN Stress IS Manageable
  const r10 = Math.min(confHigh, loadStd);

  // Rule 11: IF Confidence IS High AND Load IS Light THEN Stress IS Relaxed
  const r11 = Math.min(confHigh, loadLight);

  // Aggregation of Consequents
  const stressRelaxedFiring = Math.max(r7, r11);
  const stressManageableFiring = Math.max(r4, r6, r9, r10);
  const stressDemandingFiring = Math.max(r3, r5, r8);
  const stressCriticalFiring = Math.max(r1, r2);

  // --- 3. AGGREGATION & 4. CENTROID DEFUZZIFICATION ---
  // Output space: S in [0, 100]
  // Relaxed: trapmf(s, 0, 0, 20, 38)
  // Manageable: trimf(s, 25, 48, 68)
  // Demanding: trimf(s, 52, 74, 88)
  // Critical: trapmf(s, 76, 90, 100, 100)

  let numerator = 0;
  let denominator = 0;

  for (let s = 0; s <= 100; s += 2) {
    const muRelaxed = Math.min(stressRelaxedFiring, trapmf(s, 0, 0, 20, 38));
    const muManageable = Math.min(stressManageableFiring, trimf(s, 25, 48, 68));
    const muDemanding = Math.min(stressDemandingFiring, trimf(s, 52, 74, 88));
    const muCritical = Math.min(stressCriticalFiring, trapmf(s, 76, 90, 100, 100));

    // Aggregate with Max
    const muAggregate = Math.max(muRelaxed, muManageable, muDemanding, muCritical);

    numerator += s * muAggregate;
    denominator += muAggregate;
  }

  // Fallback if no rules activated significantly (e.g. at boundary)
  const crispScore = denominator > 0 ? Math.round(numerator / denominator) : 50;

  // Determine Rating & Description
  let rating: FuzzyStressResult['rating'] = 'Manageable';
  let description = 'Balanced workload well-aligned with your subject confidence.';
  let recommendedCredits = 15;

  if (crispScore <= 38) {
    rating = 'Relaxed';
    description = 'Gentle pace with plenty of breathing room for hands-on marine lab work.';
    recommendedCredits = 18;
  } else if (crispScore <= 64) {
    rating = 'Manageable';
    description = 'Harmonious academic balance. Sufficient study time across core modules.';
    recommendedCredits = confidence >= 7 ? 18 : 15;
  } else if (crispScore <= 82) {
    rating = 'Demanding';
    description = 'Demanding curriculum block. Solid time management and study routines required.';
    recommendedCredits = confidence >= 6 ? 15 : 12;
  } else {
    rating = 'Critical';
    description = 'High burnout potential. Consider deferring a non-critical elective to keep pace.';
    recommendedCredits = 12;
  }

  return {
    stressScore: Math.min(98, Math.max(12, crispScore)),
    rating,
    recommendedCredits,
    description,
    linguisticScores: {
      relaxed: Math.round(stressRelaxedFiring * 100) / 100,
      manageable: Math.round(stressManageableFiring * 100) / 100,
      demanding: Math.round(stressDemandingFiring * 100) / 100,
      critical: Math.round(stressCriticalFiring * 100) / 100,
    },
    dominantState: rating,
  };
}
