/**
 * Backward Chaining & Kahn's Topological Sorting Engine
 * Reference: CourseAdvisor System Design Specification (CourseAdvisor-DevBP.docx)
 *
 * Implements:
 * 1. Directed Acyclic Graph (DAG) construction
 * 2. Kahn's Algorithm for cycle detection and topological ordering
 * 3. Stratified depth tiers (Levelization)
 * 4. Backward Chaining goal-driven prerequisite traversal
 * 5. Transparent scheduling rationale generation
 */

import { Course } from './courses';

export type CourseNode = {
  course: Course;
  inDegree: number;
  tier: number;
  unlocks: string[];
};

export type BackwardChainingGoal = {
  goalCode: string;
  goalTitle: string;
  unfulfilledChain: string[];
  depthTiers: { tier: number; courses: Course[] }[];
  terminalSatisfied: string[];
};

export type CourseRationale = {
  code: string;
  urgency: 'high' | 'medium' | 'normal';
  headline: string;
  detail: string;
  unblocksCapstone?: string;
};

export class CourseGraphEngine {
  private courseMap: Map<string, Course>;
  private adjacency: Map<string, string[]>;     // course -> courses that depend on it
  private reverseAdjacency: Map<string, string[]>; // course -> its prerequisites
  private depthTiers: Map<string, number>;

  constructor(private courses: Course[]) {
    this.courseMap = new Map(courses.map(c => [c.code, c]));
    this.adjacency = new Map();
    this.reverseAdjacency = new Map();
    this.depthTiers = new Map();

    // Initialize graph
    for (const c of courses) {
      this.adjacency.set(c.code, []);
      this.reverseAdjacency.set(c.code, [...c.pre]);
    }

    // Populate forward edges (u -> v where u is prerequisite for v)
    for (const c of courses) {
      for (const pre of c.pre) {
        const dependents = this.adjacency.get(pre) || [];
        dependents.push(c.code);
        this.adjacency.set(pre, dependents);
      }
    }

    this.computeTiersAndValidateCycles();
  }

  /**
   * Kahn's Algorithm for Cycle Detection & Longest-Path Levelization.
   */
  private computeTiersAndValidateCycles(): { isValid: boolean; sorted: string[] } {
    const inDegree = new Map<string, number>();
    for (const c of this.courses) {
      inDegree.set(c.code, c.pre.length);
      this.depthTiers.set(c.code, 0);
    }

    const queue: string[] = [];
    for (const [code, deg] of inDegree.entries()) {
      if (deg === 0) {
        queue.push(code);
        this.depthTiers.set(code, 0); // Base tier
      }
    }

    const sorted: string[] = [];

    while (queue.length > 0) {
      const u = queue.shift()!;
      sorted.push(u);
      const currentTier = this.depthTiers.get(u) || 0;

      for (const v of this.adjacency.get(u) || []) {
        const nextDeg = (inDegree.get(v) || 0) - 1;
        inDegree.set(v, nextDeg);

        // Update longest path tier
        const existingNextTier = this.depthTiers.get(v) || 0;
        this.depthTiers.set(v, Math.max(existingNextTier, currentTier + 1));

        if (nextDeg === 0) {
          queue.push(v);
        }
      }
    }

    const isValid = sorted.length === this.courses.length;
    if (!isValid) {
      console.warn('[CourseGraphEngine] Prerequisite cycle detected in curriculum graph!');
    }

    return { isValid, sorted };
  }

  public getCourseTier(code: string): number {
    return this.depthTiers.get(code) || 0;
  }

  /**
   * Goal-Driven Backward Chaining Traversal.
   * Traverses backward from target capstone/milestone course until all prerequisites
   * hit terminal stopping conditions (courses already completed).
   */
  public backwardChainFromGoal(
    goalCode: string,
    completedCodes: Set<string>
  ): BackwardChainingGoal {
    const goalCourse = this.courseMap.get(goalCode);
    const goalTitle = goalCourse ? goalCourse.title : goalCode;

    const unfulfilledSet = new Set<string>();
    const terminalSatisfiedSet = new Set<string>();
    const visited = new Set<string>();

    const traverse = (code: string) => {
      if (visited.has(code)) return;
      visited.add(code);

      const course = this.courseMap.get(code);
      if (!course) return;

      for (const pre of course.pre) {
        if (completedCodes.has(pre)) {
          terminalSatisfiedSet.add(pre); // Terminal stopping condition
        } else {
          unfulfilledSet.add(pre);
          traverse(pre);
        }
      }
    };

    traverse(goalCode);

    // Group unfulfilled into tiers for structured term allocation
    const tierMap = new Map<number, Course[]>();
    for (const code of unfulfilledSet) {
      const c = this.courseMap.get(code);
      if (!c) continue;
      const tier = this.depthTiers.get(code) || 0;
      if (!tierMap.has(tier)) tierMap.set(tier, []);
      tierMap.get(tier)!.push(c);
    }

    const depthTiers = Array.from(tierMap.entries())
      .sort(([a], [b]) => a - b)
      .map(([tier, courses]) => ({ tier, courses }));

    return {
      goalCode,
      goalTitle,
      unfulfilledChain: Array.from(unfulfilledSet),
      depthTiers,
      terminalSatisfied: Array.from(terminalSatisfiedSet),
    };
  }

  /**
   * Generates transparent, human-readable scheduling explanations for why
   * a course is placed in the candidate semester.
   */
  public generateRationale(course: Course, completedCodes: Set<string>): CourseRationale {
    const unlocks = this.adjacency.get(course.code) || [];

    // Capstone target checks (MRB199 Thesis, MRB197 Practicum, MRB196)
    const capstones = ['MRB199', 'MRB197', 'MRB196', 'MRB198', 'MRB148'];
    const reachesCapstone = unlocks.find(u => capstones.includes(u));

    if (reachesCapstone) {
      const capstoneCourse = this.courseMap.get(reachesCapstone);
      return {
        code: course.code,
        urgency: 'high',
        headline: `Direct unlock for ${reachesCapstone}`,
        detail: `Critical gateway course required for senior milestone: ${capstoneCourse?.title || reachesCapstone}.`,
        unblocksCapstone: reachesCapstone,
      };
    }

    // Check if it unblocks multiple future courses
    if (unlocks.length >= 2) {
      return {
        code: course.code,
        urgency: 'high',
        headline: `Unlocks ${unlocks.length} downstream courses`,
        detail: `Prerequisite foundation for: ${unlocks.slice(0, 3).join(', ')}${unlocks.length > 3 ? '...' : ''}.`,
      };
    }

    // Co-requisite pairing rationale
    if (course.co) {
      return {
        code: course.code,
        urgency: 'medium',
        headline: 'Co-requisite companion module',
        detail: `Best scheduled concurrently with lab/lecture partner ${course.co}.`,
      };
    }

    // Regular foundation progression
    const tier = this.depthTiers.get(course.code) || 0;
    return {
      code: course.code,
      urgency: 'normal',
      headline: `Tier ${tier + 1} Curriculum Progression`,
      detail: `All prerequisites met. Steady milestone progression in ${course.category}.`,
    };
  }

  /**
   * Validates if moving a course to a given term violates any topological prerequisite constraints.
   */
  public validateMove(
    courseCode: string,
    targetTerm: number,
    termAllocations: Map<string, number>
  ): { valid: boolean; reason?: string } {
    const course = this.courseMap.get(courseCode);
    if (!course) return { valid: false, reason: 'Course not found in curriculum.' };

    for (const pre of course.pre) {
      const preTerm = termAllocations.get(pre);
      if (preTerm !== undefined && preTerm >= targetTerm) {
        const preCourse = this.courseMap.get(pre);
        return {
          valid: false,
          reason: `Violates prerequisite: ${pre} (${preCourse?.title || ''}) is scheduled in Term ${preTerm + 1}.`,
        };
      }
    }

    // Check dependents
    const dependents = this.adjacency.get(courseCode) || [];
    for (const dep of dependents) {
      const depTerm = termAllocations.get(dep);
      if (depTerm !== undefined && depTerm <= targetTerm) {
        const depCourse = this.courseMap.get(dep);
        return {
          valid: false,
          reason: `Violates dependent sequence: ${dep} (${depCourse?.title || ''}) requires this course prior to Term ${depTerm + 1}.`,
        };
      }
    }

    return { valid: true };
  }
}
