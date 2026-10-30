import { useId, useState, type FormEvent } from 'react';
import {
  DEGREE_PROGRAMS,
  getProgramById,
  type StudentProfile,
  type StudentStanding,
} from '../curricula';
import Icon from '../icons';
import InfoTooltip from './InfoTooltip';

export type ProfileFormOptions = {
  autoFillPrior: boolean;
  goalCode: string;
};

export type ProfileFormProps = {
  profile: StudentProfile;
  onboarding?: boolean;
  initialGoal?: string;
  onSave: (profile: StudentProfile, options: ProfileFormOptions) => void;
  onDemo?: () => void;
};

const standings: StudentStanding[] = ['1st Year', '2nd Year', '3rd Year', '4th Year', 'Irregular'];

export default function ProfileForm({
  profile,
  onboarding = false,
  initialGoal,
  onSave,
  onDemo,
}: ProfileFormProps) {
  const id = useId();
  const [draft, setDraft] = useState<StudentProfile>(() => {
    const prog = getProgramById(profile.programId || 'BSMS');
    const defaultTerm = prog.standingTerm[profile.standing] ?? profile.startingTerm;
    return {
      ...profile,
      fullName: onboarding && !profile.isConfigured ? '' : profile.fullName,
      studentId: onboarding && !profile.isConfigured ? '' : profile.studentId,
      programId: prog.id,
      academicYear: prog.catalogAY,
      startingTerm: defaultTerm,
    };
  });

  const currentProgram = getProgramById(draft.programId);
  const [autoFillPrior, setAutoFillPrior] = useState(true);
  const [goalCode, setGoalCode] = useState(
    initialGoal || currentProgram.milestones[0]?.code || ''
  );
  const [error, setError] = useState('');

  function handleProgramChange(newProgramId: string) {
    const prog = getProgramById(newProgramId);
    const nextTerm = prog.standingTerm[draft.standing] ?? 0;
    const nextGoal = prog.milestones[0]?.code ?? '';
    setDraft({
      ...draft,
      programId: prog.id,
      academicYear: prog.catalogAY,
      startingTerm: nextTerm,
    });
    setGoalCode(nextGoal);
  }

  function handleStandingChange(newStanding: StudentStanding) {
    const nextTerm = currentProgram.standingTerm[newStanding] ?? draft.startingTerm;
    setDraft({
      ...draft,
      standing: newStanding,
      startingTerm: nextTerm,
    });
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft.fullName.trim()) {
      setError('Please enter your full name to proceed.');
      return;
    }
    setError('');
    onSave(
      {
        ...draft,
        fullName: draft.fullName.trim(),
        studentId: draft.studentId.trim(),
        isConfigured: true,
      },
      {
        autoFillPrior: onboarding && draft.standing !== '1st Year' ? autoFillPrior : false,
        goalCode: goalCode || currentProgram.milestones[0]?.code || '',
      }
    );
  }

  const paceLabel =
    draft.confidence <= 3
      ? '🌿 Gentle Pace · Suggests ~12 units (Lightens workload stress)'
      : draft.confidence <= 7
      ? '⚖️ Balanced Pace · Suggests ~15 units (Standard curriculum load)'
      : '🚀 Intensive Pace · Suggests ~18 units (Max capacity with labs)';

  return (
    <form className="profile-setup" onSubmit={submit}>
      {onboarding && (
        <ol className="setup-steps" aria-label="Getting started">
          <li aria-current="step">
            <span>1</span> Student Profile
          </li>
          <li>
            <span>2</span> Course History
          </li>
          <li>
            <span>3</span> Semester Plan
          </li>
        </ol>
      )}

      <div className="form-grid profile-name-grid">
        <div className="field">
          <label htmlFor={`${id}-name`}>
            Student Full Name <span aria-hidden="true">*</span>
          </label>
          <input
            id={`${id}-name`}
            value={draft.fullName}
            onChange={event => {
              setDraft({ ...draft, fullName: event.target.value });
              if (error) setError('');
            }}
            placeholder="e.g. Maria Clara Santos"
            autoComplete="name"
            maxLength={100}
            required
            autoFocus
            aria-invalid={!!error}
            aria-describedby={error ? `${id}-name-error` : undefined}
          />
          {error && (
            <span className="field-error" id={`${id}-name-error`} role="alert">
              {error}
            </span>
          )}
        </div>
        <div className="field">
          <label htmlFor={`${id}-student-id`}>
            Student ID <span className="profile-optional">(optional)</span>
          </label>
          <input
            id={`${id}-student-id`}
            value={draft.studentId}
            onChange={event => setDraft({ ...draft, studentId: event.target.value })}
            placeholder="e.g. 2023-0145"
            maxLength={50}
          />
        </div>
      </div>

      <div className="field">
        <label htmlFor={`${id}-program`}>
          Select Degree Program <span aria-hidden="true">*</span>
          <InfoTooltip content="Course prerequisites and recommended sequences are strictly validated from this official MSU at Naawan prospectus." />
        </label>
        <select
          id={`${id}-program`}
          value={draft.programId}
          onChange={e => handleProgramChange(e.target.value)}
        >
          {DEGREE_PROGRAMS.map(prog => (
            <option key={prog.id} value={prog.id}>
              {prog.name} ({prog.college})
            </option>
          ))}
        </select>
      </div>

      <div className="profile-curriculum">
        <span className="profile-curriculum-icon">
          <Icon name="cap" size={23} />
        </span>
        <div>
          <strong>{currentProgram.name}</strong>
          <span>
            {currentProgram.campus} · {currentProgram.college} · {currentProgram.catalogAY}
          </span>
        </div>
        <span className="pill">
          {currentProgram.totalUnits} Units · {currentProgram.courses.length} Courses
        </span>
      </div>

      <fieldset className="profile-standing">
        <legend>
          Current Year Level
          <InfoTooltip content="Sets your starting semester. Eligibility and prerequisite checks reference your completed subjects." />
        </legend>
        <div className="profile-standing-options">
          {standings.map(standing => (
            <label key={standing}>
              <input
                type="radio"
                name={`${id}-standing`}
                value={standing}
                checked={draft.standing === standing}
                onChange={() => handleStandingChange(standing)}
              />
              <span>{standing === 'Irregular' ? 'Irregular / Transferee' : standing}</span>
            </label>
          ))}
        </div>
      </fieldset>

      {onboarding && draft.standing !== '1st Year' && (
        <div className="profile-autofill">
          <input
            type="checkbox"
            id={`${id}-autofill`}
            checked={autoFillPrior}
            onChange={e => setAutoFillPrior(e.target.checked)}
          />
          <div className="profile-autofill-content">
            <label htmlFor={`${id}-autofill`} className="profile-autofill-title">
              Auto-mark prior years' courses as completed (Terms 1 to {draft.startingTerm})
              <InfoTooltip content="Recommended: Automatically satisfies foundational prerequisite trees for earlier semesters so upcoming subjects are unlocked right away." />
            </label>
          </div>
        </div>
      )}

      {draft.standing === 'Irregular' && (
        <div className="field">
          <label htmlFor={`${id}-term`}>
            Starting Semester
            <InfoTooltip content="The specific semester you plan to enroll in for advising." />
          </label>
          <select
            id={`${id}-term`}
            value={draft.startingTerm}
            onChange={event => setDraft({ ...draft, startingTerm: Number(event.target.value) })}
          >
            {currentProgram.terms.map((term, index) => (
              <option key={term} value={index}>
                {term}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="field">
        <label htmlFor={`${id}-goal`}>
          Primary Academic Milestone Goal
          <InfoTooltip content="The Backward Chaining engine prioritizes your prerequisite roadmap to reach this capstone milestone without delay." />
        </label>
        <select
          id={`${id}-goal`}
          value={goalCode}
          onChange={e => setGoalCode(e.target.value)}
        >
          {currentProgram.milestones.map(g => (
            <option key={g.code} value={g.code}>
              {g.code}: {g.title}
            </option>
          ))}
        </select>
      </div>

      <div className="profile-confidence">
        <div className="profile-confidence-heading">
          <label htmlFor={`${id}-confidence`}>
            Study Confidence & Target Pace
            <InfoTooltip content="Feeds the Mamdani Fuzzy Stress engine to prevent semester overload. You can adjust this anytime." />
          </label>
          <output htmlFor={`${id}-confidence`}>
            {draft.confidence}
            <span> / 10</span>
          </output>
        </div>
        <input
          id={`${id}-confidence`}
          type="range"
          min={1}
          max={10}
          step={1}
          value={draft.confidence}
          onChange={event => setDraft({ ...draft, confidence: Number(event.target.value) })}
        />
        <div className="profile-range-labels">
          <span>Gentler load</span>
          <span>Maximum load</span>
        </div>
        <div className="profile-pace-badge">
          <span>{paceLabel}</span>
        </div>
      </div>

      <div className="profile-privacy">
        <Icon name="shield" size={16} />
        <span>Your student preferences and course history remain 100% private in this device.</span>
      </div>

      <div className="form-actions profile-form-actions">
        {onboarding && onDemo && (
          <button type="button" className="text-button" onClick={onDemo}>
            Explore with sample student (Demo Mode)
          </button>
        )}
        <button type="submit" className="primary">
          {onboarding ? 'Start My Academic Plan' : 'Save Profile Changes'}
          <Icon name={onboarding ? 'arrow' : 'check'} size={17} />
        </button>
      </div>
    </form>
  );
}
