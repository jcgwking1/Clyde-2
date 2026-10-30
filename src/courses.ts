/**
 * Re-exports from curricula.ts with backward compatibility for default curriculum
 */
export * from './curricula';

import {
  DEGREE_PROGRAMS,
  getProgramById,
  DEFAULT_PROGRAM_ID,
} from './curricula';

export const defaultProgram = getProgramById(DEFAULT_PROGRAM_ID);
export const courses = defaultProgram.courses;
export const terms = defaultProgram.terms;
