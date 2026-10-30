import { courses, type Course } from './courses';
import { evaluateMamdaniStress } from './fuzzyEngine';
import { CourseGraphEngine } from './graphEngine';

const courseMap = new Map(courses.map(course => [course.code, course]));
const curriculumOrder = new Map(courses.map((course, index) => [course.code, index]));
const standingCourses = courses.filter(course => course.term < 4).map(course => course.code);
const graph = new CourseGraphEngine(courses);
const requiresThirdYearStanding = (course: Course) => ['MRB195', 'MRB198'].includes(course.code);

/** Requirements are satisfied by recorded completions, never by another course in the draft. */
export function missingPrerequisites(course: Course, completed: string[]): string[] {
  const requirements = [
    ...course.pre,
    ...(requiresThirdYearStanding(course) ? standingCourses : []),
  ];
  const completedSet = new Set(completed);
  return [...new Set(requirements)].filter(code => !completedSet.has(code));
}

export type SuggestedPlanInput = {
  completed: string[];
  targetGoal: string;
  term: number;
  confidence: number;
};

export type SuggestedPlan = { codes: string[]; units: number; cap: number };

/** Build a draft whose actual workload stays within the same cap shown after generation. */
export function buildSuggestedPlan({ completed, targetGoal, term, confidence }: SuggestedPlanInput): SuggestedPlan {
  const completedSet = new Set(completed);
  const goal = courseMap.get(targetGoal);
  const goalRequirements = new Set<string>();
  if (goal && !completedSet.has(goal.code)) {
    goalRequirements.add(goal.code);
    const chain = graph.backwardChainFromGoal(goal.code, completedSet);
    chain.unfulfilledChain.forEach(code => goalRequirements.add(code));
    if ([...goalRequirements].some(code => requiresThirdYearStanding(courseMap.get(code)!))) {
      standingCourses.filter(code => !completedSet.has(code)).forEach(code => goalRequirements.add(code));
    }
  }

  const eligible = (course: Course) => missingPrerequisites(course, completed).length === 0;
  const candidates = courses.filter(course => !completedSet.has(course.code) && eligible(course));
  candidates.sort((a, b) =>
    Number(goalRequirements.has(b.code)) - Number(goalRequirements.has(a.code)) ||
    Number(b.term === term) - Number(a.term === term) ||
    curriculumOrder.get(a.code)! - curriculumOrder.get(b.code)!
  );

  const initialCap = evaluateMamdaniStress(0, confidence, 6.2).recommendedCredits;
  const planned: Course[] = [];
  const plannedCodes = new Set<string>();
  let units = 0;
  let difficultyTotal = 0;
  for (const course of candidates) {
    if (plannedCodes.has(course.code)) continue;
    const group = [course];
    if (course.co && !completedSet.has(course.co) && !plannedCodes.has(course.co)) {
      const companion = courseMap.get(course.co);
      if (!companion || !eligible(companion)) continue;
      group.push(companion);
    }
    const candidateUnits = units + group.reduce((sum, item) => sum + item.units, 0);
    const candidateDifficulty = difficultyTotal + group.reduce((sum, item) => sum + item.difficulty, 0);
    const candidateCap = evaluateMamdaniStress(
      candidateUnits, confidence, candidateDifficulty / (planned.length + group.length)
    ).recommendedCredits;
    if (candidateUnits > Math.min(initialCap, candidateCap)) continue;
    planned.push(...group);
    group.forEach(item => plannedCodes.add(item.code));
    units = candidateUnits;
    difficultyTotal = candidateDifficulty;
  }

  const cap = evaluateMamdaniStress(units, confidence, planned.length ? difficultyTotal / planned.length : 6.2).recommendedCredits;
  return { codes: planned.map(course => course.code), units, cap };
}

export default buildSuggestedPlan;
