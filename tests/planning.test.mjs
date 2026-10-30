import assert from 'node:assert/strict';
import { after, test } from 'node:test';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

// Compile the production TypeScript using the project's existing compiler; no test dependency is needed.
const source = resolve(dirname(fileURLToPath(import.meta.url)), '../src');
const output = mkdtempSync(join(tmpdir(), 'course-advisor-planning-'));
after(() => rmSync(output, { recursive: true, force: true }));
const program = ts.createProgram([join(source, 'planning.ts')], {
  target: ts.ScriptTarget.ES2020,
  module: ts.ModuleKind.CommonJS,
  moduleResolution: ts.ModuleResolutionKind.Node10,
  rootDir: source,
  outDir: output,
  strict: true,
  skipLibCheck: true,
  types: [],
});
const diagnostics = ts.getPreEmitDiagnostics(program);
assert.equal(diagnostics.length, 0, ts.formatDiagnosticsWithColorAndContext(diagnostics, {
  getCanonicalFileName: value => value, getCurrentDirectory: () => source, getNewLine: () => '\n',
}));
program.emit();
writeFileSync(join(output, 'package.json'), '{"type":"commonjs"}');
const require = createRequire(import.meta.url);
const { buildSuggestedPlan, missingPrerequisites } = require(join(output, 'planning.js'));
const { courses } = require(join(output, 'courses.js'));
const { evaluateMamdaniStress } = require(join(output, 'fuzzyEngine.js'));
const course = code => courses.find(item => item.code === code);
const foundation = courses.filter(item => item.term < 4).map(item => item.code);

function assertValidPlan(result, completed, confidence) {
  const planned = result.codes.map(course);
  assert.equal(new Set(result.codes).size, result.codes.length, 'No duplicate courses');
  assert.equal(result.units, planned.reduce((sum, item) => sum + item.units, 0));
  const average = planned.length ? planned.reduce((sum, item) => sum + item.difficulty, 0) / planned.length : 6.2;
  assert.equal(result.cap, evaluateMamdaniStress(result.units, confidence, average).recommendedCredits);
  assert.ok(result.units <= result.cap, `${result.units} units exceed final ${result.cap}-unit recommendation`);
  assert.ok(result.units <= evaluateMamdaniStress(0, confidence, 6.2).recommendedCredits);
  for (const item of planned) {
    assert.ok(!completed.includes(item.code), 'Completed courses must not be planned again');
    assert.deepEqual(missingPrerequisites(item, completed), [], `${item.code} has missing requirements`);
    if (item.co) assert.ok(completed.includes(item.co) || result.codes.includes(item.co), `${item.code} needs its companion`);
  }
}

for (const confidence of [1, 2, 7, 10]) {
  test(`actual workload respects its recommendation at confidence ${confidence}/10`, () => {
    for (const completed of [[], foundation, foundation.filter(code => code !== 'CHM011.1')]) {
      for (const targetGoal of ['MRB199', 'MRB197', 'MRB148', 'MRB145', 'OCN121']) {
        for (const term of [0, 4, 8]) {
          const result = buildSuggestedPlan({ completed, targetGoal, term, confidence });
          assert.ok(result.codes.length > 0);
          assertValidPlan(result, completed, confidence);
        }
      }
    }
  });
}

test('a new student starts with actual foundation courses and no assumed completions', () => {
  const completed = [];
  const result = buildSuggestedPlan({ completed, targetGoal: 'MRB199', term: 0, confidence: 7 });
  assert.deepEqual(completed, []);
  assert.ok(result.codes.includes('PHY101'));
  assert.ok(!result.codes.includes('MRB195'));
  assert.ok(!result.codes.includes('MRB196'));
  assert.ok(!result.codes.includes('MRB199'));
});

test('third-year research requires every Year 1 and Year 2 course', () => {
  const completed = foundation.filter(code => code !== 'GEC101');
  assert.deepEqual(missingPrerequisites(course('MRB195'), completed), ['GEC101']);
  assert.deepEqual(missingPrerequisites(course('MRB198'), completed), ['GEC101']);
  assert.deepEqual(missingPrerequisites(course('MRB195'), foundation), []);
  const result = buildSuggestedPlan({ completed, targetGoal: 'MRB199', term: 5, confidence: 7 });
  assert.equal(result.codes[0], 'GEC101', 'Outstanding standing requirement should be prioritized for thesis');
  assert.ok(!result.codes.includes('MRB195'));
  assert.ok(!result.codes.includes('MRB198'));
});

test('a goal prerequisite takes priority over courses in the selected semester', () => {
  const result = buildSuggestedPlan({ completed: foundation, targetGoal: 'MRB199', term: 4, confidence: 7 });
  assert.equal(result.codes[0], 'MRB195');
  assert.ok(result.codes.indexOf('MRB131') < result.codes.indexOf('MRB132'), 'Equal priorities preserve curriculum order');
});

test('changing the goal changes the first recommended course', () => {
  const thesis = buildSuggestedPlan({ completed: foundation, targetGoal: 'MRB199', term: 4, confidence: 7 });
  const practicum = buildSuggestedPlan({ completed: foundation, targetGoal: 'MRB197', term: 4, confidence: 7 });
  assert.equal(thesis.codes[0], 'MRB195');
  assert.equal(practicum.codes[0], 'MRB197');
});

test('lecture and laboratory are bundled, but a completed companion is not repeated', () => {
  const paired = buildSuggestedPlan({ completed: [], targetGoal: 'OCN121', term: 0, confidence: 7 });
  assert.ok(paired.codes.includes('PHY101') && paired.codes.includes('PHY101.1'));
  const completed = ['PHY101.1'];
  const partial = buildSuggestedPlan({ completed, targetGoal: 'OCN121', term: 0, confidence: 7 });
  assert.ok(partial.codes.includes('PHY101'));
  assert.ok(!partial.codes.includes('PHY101.1'));
  assertValidPlan(partial, completed, 7);
});

test('a completed curriculum returns an empty plan and the corresponding displayed cap', () => {
  const completed = courses.map(item => item.code);
  const result = buildSuggestedPlan({ completed, targetGoal: 'MRB199', term: 8, confidence: 2 });
  assert.deepEqual(result.codes, []);
  assert.equal(result.units, 0);
  assertValidPlan(result, completed, 2);
});
