import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import Icon from './icons';
import Dialog from './components/Dialog';
import ProfileForm from './components/ProfileForm';
import AdvisingSlipModal from './components/AdvisingSlipModal';
import {
  DEGREE_PROGRAMS,
  getProgramById,
  DEFAULT_PROGRAM_ID,
  DEFAULT_PROFILE,
  type DegreeProgram,
  type Course,
  type StudentProfile,
} from './curricula';
import { evaluateMamdaniStress } from './fuzzyEngine';
import { CourseGraphEngine } from './graphEngine';

const cn = (...values: ClassValue[]) => twMerge(clsx(values));
const navigation = [
  { icon: 'grid', label: 'Overview' },
  { icon: 'calendar', label: 'My planner' },
  { icon: 'book', label: 'Course catalog' },
  { icon: 'graph', label: 'Prerequisite map' },
  { icon: 'chart', label: 'My progress' },
] as const;
type Page = (typeof navigation)[number]['label'] | 'Settings';

const blankProfile: StudentProfile = {
  ...DEFAULT_PROFILE,
  fullName: '',
  studentId: '',
  programId: DEFAULT_PROGRAM_ID,
  standing: '1st Year',
  startingTerm: 0,
  confidence: 7,
  isConfigured: false,
};

function sanitizeProfile(raw: any): StudentProfile {
  if (!raw || typeof raw !== 'object') return blankProfile;
  return {
    ...blankProfile,
    ...raw,
    fullName: typeof raw.fullName === 'string' ? raw.fullName : (typeof raw.name === 'string' ? raw.name : ''),
    studentId: typeof raw.studentId === 'string' ? raw.studentId : '',
    programId: typeof raw.programId === 'string' && raw.programId ? raw.programId : DEFAULT_PROGRAM_ID,
    academicYear: typeof raw.academicYear === 'string' && raw.academicYear ? raw.academicYear : 'AY 2025–2026',
    standing: ['1st Year', '2nd Year', '3rd Year', '4th Year', 'Irregular'].includes(raw.standing)
      ? raw.standing
      : '1st Year',
    startingTerm: typeof raw.startingTerm === 'number' ? raw.startingTerm : 0,
    confidence: typeof raw.confidence === 'number' ? Math.max(1, Math.min(10, raw.confidence)) : 7,
    isConfigured: Boolean(raw.isConfigured),
  };
}

function load<T>(key: string, fallback: T): T {
  try {
    return JSON.parse(localStorage.getItem(key) || 'null') ?? fallback;
  } catch {
    return fallback;
  }
}

export default function App() {
  const [page, setPage] = useState<Page>('Overview');
  const [profile, setProfile] = useState<StudentProfile>(() => sanitizeProfile(load('ca-profile', blankProfile)));
  const [completed, setCompleted] = useState<string[]>(() => load('ca-completed', []));
  const [plan, setPlan] = useState<string[]>(() => load('ca-plan', []));
  const [term, setTerm] = useState<number>(() => load('ca-term', load('ca-profile', blankProfile).startingTerm));
  const [targetGoal, setTargetGoal] = useState<string>(() => load('ca-goal', ''));
  const [demo, setDemo] = useState(() => load('ca-demo', false));
  const [modal, setModal] = useState(() => (load('ca-profile', blankProfile).isConfigured ? '' : 'onboarding'));
  const [selected, setSelected] = useState<Course | null>(null);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All categories');
  const [status, setStatus] = useState('All courses');
  const [catalogTerm, setCatalogTerm] = useState('all');
  const [notice, setNotice] = useState('');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [storageError, setStorageError] = useState(false);
  const [draftConfidence, setDraftConfidence] = useState(profile.confidence);
  const noticeTimer = useRef<number | undefined>(undefined);
  const pageTitle = useRef<HTMLHeadingElement>(null);
  const previousPage = useRef<Page>(page);

  // Dynamic active program & curriculum from student profile
  const program: DegreeProgram = useMemo(
    () => getProgramById(profile.programId || DEFAULT_PROGRAM_ID),
    [profile.programId]
  );
  const courses: Course[] = program.courses;
  const terms: string[] = program.terms;
  const goals: string[] = useMemo(() => program.milestones.map(m => m.code), [program]);
  const categories = useMemo(() => ['All categories', ...new Set(courses.map(c => c.category))], [courses]);

  const thirdYearTerm = program.standingTerm['3rd Year'] ?? 4;
  const standingCourses = courses.filter(c => c.term < thirdYearTerm);
  const needsStanding = (c: Course) =>
    ['MRB195', 'MRB198', 'ANS191', 'ANS194', 'AGM191', 'AGM194', 'MKT197', 'ELC101', 'ELC102'].includes(c.code);

  const courseByCode = (code: string): Course =>
    courses.find(c => c.code === code) ||
    courses.find(c => c.code.toLowerCase() === code.toLowerCase()) ||
    courses[0];

  const graph = useMemo(() => new CourseGraphEngine(courses), [courses]);

  useEffect(() => () => window.clearTimeout(noticeTimer.current), []);
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && sidebarOpen) {
        setSidebarOpen(false);
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [sidebarOpen]);
  useEffect(() => {
    if (previousPage.current !== page) {
      pageTitle.current?.focus();
      window.scrollTo({ top: 0 });
      previousPage.current = page;
    }
  }, [page]);

  const done = courses.filter(c => completed.includes(c.code));
  const earned = done.reduce((sum, c) => sum + c.units, 0);
  const total = courses.reduce((sum, c) => sum + c.units, 0);
  const progress = total > 0 ? Math.round((earned / total) * 100) : 0;
  const planned = courses.filter(c => plan.includes(c.code) && !completed.includes(c.code));
  const units = planned.reduce((sum, c) => sum + c.units, 0);
  const averageDifficulty = planned.length
    ? planned.reduce((sum, c) => sum + c.difficulty, 0) / planned.length
    : 6.2;
  const workload = evaluateMamdaniStress(units, profile.confidence, averageDifficulty);
  const previewWorkload = evaluateMamdaniStress(units, draftConfidence, averageDifficulty);

  const termLabel = terms[term] || terms[0] || 'Current Semester';
  const firstName = (profile.fullName || '').trim().split(/\s+/)[0] || 'there';
  const initials =
    (profile.fullName || '')
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .map(p => p[0])
      .filter((_, i, a) => i === 0 || i === a.length - 1)
      .join('')
      .toUpperCase() || 'CA';

  function missingPrerequisites(c: Course) {
    return [
      ...new Set([
        ...c.pre,
        ...(needsStanding(c) ? standingCourses.map(x => x.code) : []),
      ]),
    ].filter(code => !completed.includes(code));
  }

  const eligible = (c: Course) => missingPrerequisites(c).length === 0;
  const available = courses.filter(c => !completed.includes(c.code) && eligible(c));

  const planIssues = planned.flatMap(c => {
    const missing = missingPrerequisites(c);
    return [
      ...(missing.length ? [`${c.code}: complete ${missing.join(', ')} first.`] : []),
      ...(c.co && !completed.includes(c.co) && !plan.includes(c.co)
        ? [`${c.code}: add companion course ${c.co}.`]
        : []),
    ];
  });

  const activeGoalCode = courses.some(c => c.code === targetGoal)
    ? targetGoal
    : program.milestones[0]?.code || courses[0]?.code || '';
  const goalCourse = courseByCode(activeGoalCode);
  const goalAnalysis = graph.backwardChainFromGoal(goalCourse.code, new Set(completed));
  const goalRequirements = new Set(goalAnalysis.unfulfilledChain);

  if ([goalCourse, ...goalAnalysis.unfulfilledChain.map(courseByCode)].some(needsStanding)) {
    standingCourses.filter(c => !completed.includes(c.code)).forEach(c => goalRequirements.add(c.code));
  }

  const goalTierMap = new Map<number, Course[]>();
  for (const code of goalRequirements) {
    const tier = graph.getCourseTier(code);
    goalTierMap.set(tier, [...(goalTierMap.get(tier) || []), courseByCode(code)]);
  }
  const goalTiers = [...goalTierMap.entries()].sort(([a], [b]) => a - b);

  const filteredCourses = courses.filter(
    c =>
      `${c.code} ${c.title}`.toLowerCase().includes(query.trim().toLowerCase()) &&
      (category === 'All categories' || c.category === category) &&
      (catalogTerm === 'all' || c.term === Number(catalogTerm)) &&
      (status === 'All courses' ||
        (status === 'Completed'
          ? completed.includes(c.code)
          : status === 'Ready to take'
          ? !completed.includes(c.code) && eligible(c)
          : !completed.includes(c.code) && !eligible(c)))
  );

  function persist(key: string, value: unknown) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      setStorageError(true);
    }
  }

  function notify(message: string) {
    window.clearTimeout(noticeTimer.current);
    setNotice(message);
    noticeTimer.current = window.setTimeout(() => setNotice(''), 6000);
  }

  function navigate(next: Page) {
    setPage(next);
    setSidebarOpen(false);
  }

  function savePlan(next: string[]) {
    setPlan(next);
    persist('ca-plan', next);
  }

  function saveTerm(next: number) {
    setTerm(next);
    persist('ca-term', next);
  }

  function saveGoal(next: string) {
    setTargetGoal(next);
    persist('ca-goal', next);
  }

  function toggleCompleted(c: Course) {
    const next = completed.includes(c.code)
      ? completed.filter(code => code !== c.code)
      : [...completed, c.code];
    setCompleted(next);
    persist('ca-completed', next);
    notify(
      `${c.code} marked ${completed.includes(c.code) ? 'not completed' : 'completed'}. Progress updated.`
    );
  }

  function saveProfile(
    next: StudentProfile,
    options?: { autoFillPrior: boolean; goalCode: string }
  ) {
    const isProgramChange = next.programId !== profile.programId;
    const firstSetup = !profile.isConfigured || modal === 'new-profile' || modal === 'onboarding' || isProgramChange;
    const nextProgram = getProgramById(next.programId);
    const saved = { ...next, fullName: next.fullName.trim(), isConfigured: true };

    setProfile(saved);
    persist('ca-profile', saved);
    persist('ca-confidence', saved.confidence);

    const goalToSet = options?.goalCode || nextProgram.milestones[0]?.code || nextProgram.courses[0]?.code || '';
    saveGoal(goalToSet);

    if (firstSetup) {
      if (options?.autoFillPrior && saved.startingTerm > 0) {
        const priorCourses = nextProgram.courses
          .filter(c => c.term < saved.startingTerm)
          .map(c => c.code);
        setCompleted(priorCourses);
        persist('ca-completed', priorCourses);
      } else {
        setCompleted([]);
        persist('ca-completed', []);
      }
      savePlan([]);
      setDemo(false);
      persist('ca-demo', false);
      saveTerm(saved.startingTerm);
      navigate('Overview');
    }

    setModal('');
    notify(
      firstSetup
        ? `Welcome, ${saved.fullName}! ${nextProgram.code} curriculum loaded.`
        : 'Your profile has been updated.'
    );
  }

  function exploreDemo() {
    const demoProgram = getProgramById(profile.programId || DEFAULT_PROGRAM_ID);
    const startingTerm = demoProgram.standingTerm['3rd Year'] ?? 4;
    const saved: StudentProfile = {
      ...DEFAULT_PROFILE,
      fullName: 'Alex Santos',
      programId: demoProgram.id,
      academicYear: demoProgram.catalogAY,
      standing: '3rd Year',
      startingTerm,
      confidence: 7,
      isConfigured: true,
    };
    setProfile(saved);
    persist('ca-profile', saved);

    const sampleCompleted = demoProgram.courses
      .filter(c => c.term < startingTerm)
      .map(c => c.code);
    setCompleted(sampleCompleted);
    persist('ca-completed', sampleCompleted);

    const samplePlan = demoProgram.courses
      .filter(c => c.term === startingTerm)
      .slice(0, 4)
      .map(c => c.code);
    savePlan(samplePlan);
    saveTerm(startingTerm);
    saveGoal(demoProgram.milestones[0]?.code || demoProgram.courses[0]?.code || '');
    setDemo(true);
    persist('ca-demo', true);
    setModal('');
    notify(`Sample ${demoProgram.code} dashboard loaded. You can set up your own profile anytime.`);
  }

  function openPreferences(kind: string) {
    setDraftConfidence(profile.confidence);
    setModal(kind);
  }

  function proposedPlan() {
    const result: string[] = [];
    let count = 0;
    const priorities = available.slice().sort(
      (a, b) =>
        Number(goalRequirements.has(b.code) || b.code === goalCourse.code) -
          Number(goalRequirements.has(a.code) || a.code === goalCourse.code) ||
        Number(b.term === term) - Number(a.term === term) ||
        a.term - b.term
    );
    for (const c of priorities) {
      if (result.includes(c.code)) continue;
      const group = [
        c,
        ...(c.co && !completed.includes(c.co) && !result.includes(c.co) ? [courseByCode(c.co)] : []),
      ];
      if (!group.every(eligible)) continue;
      const groupUnits = group.reduce((sum, x) => sum + x.units, 0);
      if (count + groupUnits > previewWorkload.recommendedCredits) continue;
      result.push(...group.map(x => x.code));
      count += groupUnits;
    }
    return { codes: result, units: count };
  }

  const proposal = proposedPlan();

  function generate() {
    if (!proposal.codes.length) return;
    const next = { ...profile, confidence: draftConfidence };
    setProfile(next);
    persist('ca-profile', next);
    persist('ca-confidence', draftConfidence);
    savePlan(proposal.codes);
    setModal('');
    navigate('My planner');
    notify(`Plan created: ${proposal.codes.length} courses, ${proposal.units} units. Review it with your adviser.`);
  }

  function addCourse(c: Course) {
    const pair = c.co ? courseByCode(c.co) : undefined;
    if (!eligible(c) || (pair && !completed.includes(pair.code) && !eligible(pair))) return;
    savePlan([...new Set([...plan, c.code, ...(pair && !completed.includes(pair.code) ? [pair.code] : [])])]);
    notify(`${c.code}${pair && !completed.includes(pair.code) ? ` and ${pair.code}` : ''} added to your plan.`);
    setSelected(null);
  }

  function removeCourse(c: Course) {
    const removed = [c.code, ...(c.co ? [c.co] : [])];
    savePlan(plan.filter(code => !removed.includes(code)));
    notify(`${removed.filter(code => plan.includes(code)).join(' and ')} removed from your plan.`);
  }

  function exportPlan() {
    const text = [
      'J.A.K.E.V. Edu | Course Advisor',
      'ACADEMIC ADVISING DRAFT',
      `Student: ${profile.fullName}`,
      `Student ID: ${profile.studentId || 'Not provided'}`,
      `Program: ${program.name} | ${program.college} | ${program.campus}`,
      `Curriculum: ${program.catalogAY}`,
      `Semester: ${termLabel}`,
      `Milestone Goal: ${goalCourse.code} - ${goalCourse.title}`,
      `Total: ${planned.length} courses | ${units} units`,
      `Workload estimate: ${workload.rating} (${workload.stressScore}/100)`,
      `Confidence: ${profile.confidence}/10 | Suggested limit: ${workload.recommendedCredits} units`,
      '',
      ...planned.map(c => `${c.code} | ${c.title} | ${c.units} units${c.co ? ` | Companion: ${c.co}` : ''}`),
      '',
      'Curriculum checks:',
      ...(planIssues.length ? planIssues : ['No missing prerequisites or companion courses in the current plan.']),
      ...(units > workload.recommendedCredits
        ? [`Plan exceeds suggested limit by ${units - workload.recommendedCredits} units.`]
        : []),
      '',
      'Planning draft only. Confirm course availability, credited courses, and enrollment with your academic adviser.',
    ].join('\n');

    const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${profile.fullName.toLowerCase().replace(/[^a-z0-9]+/g, '-') || 'student'}-course-plan.txt`;
    anchor.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    notify('Your advising draft has been exported.');
  }

  function resetFilters() {
    setQuery('');
    setCategory('All categories');
    setStatus('All courses');
    setCatalogTerm('all');
  }

  function courseStatus(c: Course) {
    return completed.includes(c.code)
      ? 'Completed'
      : eligible(c)
      ? 'Ready to take'
      : 'Needs prerequisites';
  }

  const renderCourse = (c: Course) => (
    <button
      key={c.code}
      className="course-row"
      onClick={() => setSelected(c)}
      aria-label={`${c.code}: ${c.title}, ${c.units} units, ${courseStatus(c)}`}
    >
      <span className="course-symbol">
        <Icon
          name={
            c.category.includes('Marine') || c.category.includes('Fisheries')
              ? 'wave'
              : c.category.includes('Science') || c.category.includes('Agri')
              ? 'leaf'
              : 'book'
          }
          size={19}
        />
      </span>
      <span className="course-name">
        <strong>{c.title}</strong>
        <span>
          {c.code} <b>·</b> {c.category}
        </span>
      </span>
      <span className="course-units">
        {c.units}
        <small> units</small>
      </span>
      <span
        className={cn(
          'pill course-status',
          completed.includes(c.code) ? 'green' : eligible(c) ? 'neutral' : 'amber'
        )}
      >
        {courseStatus(c)}
      </span>
      <Icon name="chevron" size={16} className="course-arrow" />
    </button>
  );

  const title =
    page === 'Overview'
      ? `Welcome back, ${firstName}.`
      : page === 'My planner'
      ? 'A semester that works for you.'
      : page === 'Course catalog'
      ? 'Find your next course.'
      : page === 'Prerequisite map'
      ? 'See the path ahead.'
      : page === 'My progress'
      ? 'Every course moves you forward.'
      : 'Your profile & preferences.';

  const descriptions: Record<Page, string> = {
    Overview: `A little clarity for your next big step. Here’s where you stand in ${program.code}.`,
    'My planner': 'Build a thoughtful course load, one subject at a time.',
    'Course catalog': `Explore your ${program.name} curriculum, check requirements, and plan what comes next.`,
    'Prerequisite map': 'Start with a goal. See the courses that will help you get there.',
    'My progress': 'Keep your course history up to date for recommendations that fit you.',
    Settings: 'Make this workspace yours. Your information stays in this browser.',
  };

  const workloadCard = (
    <section className="panel workload" aria-labelledby="workload-title">
      <div className="panel-heading">
        <div>
          <h2 id="workload-title">Your semester, balanced</h2>
          <p>A little room to do your best work.</p>
        </div>
        <Icon name="leaf" size={21} />
      </div>
      <div
        className="gauge"
        role="img"
        aria-label={
          planned.length
            ? `Estimated workload ${workload.stressScore} out of 100, ${workload.rating}`
            : 'Add courses to see your workload estimate'
        }
      >
        <svg viewBox="0 0 240 140" aria-hidden="true">
          <path d="M25 118a95 95 0 0 1 190 0" fill="none" stroke="#e1e9e5" strokeWidth="15" strokeLinecap="round" />
          <path
            d="M25 118a95 95 0 0 1 190 0"
            fill="none"
            stroke={workload.stressScore > 64 ? '#b77924' : '#348574'}
            strokeWidth="15"
            strokeLinecap="round"
            strokeDasharray={`${planned.length ? workload.stressScore * 2.985 : 0} 299`}
          />
        </svg>
        <div className="workload-score">
          <strong>
            {planned.length ? workload.stressScore : '—'}
            <small>/100</small>
          </strong>
          <span>{planned.length ? workload.rating : 'Not estimated yet'}</span>
        </div>
      </div>
      <div className="gauge-labels">
        <span>Light</span>
        <span>Moderate</span>
        <span>Heavy</span>
      </div>
      <p className="workload-description">
        {!planned.length
          ? 'Add courses to your plan to see how your workload is shaping up.'
          : workload.stressScore > 64
          ? 'This plan may need more study time. Consider a lighter load or discuss it with your adviser.'
          : 'Your plan leaves space to focus. Keep your other commitments in mind as you review it.'}
      </p>
      <dl className="workload-facts">
        <div>
          <dt>Planned units</dt>
          <dd>
            {units} <span>/ {workload.recommendedCredits} suggested</span>
          </dd>
        </div>
        <div>
          <dt>Study confidence</dt>
          <dd>
            {profile.confidence}
            <span> / 10</span>
          </dd>
        </div>
      </dl>
      <button className="text-button" onClick={() => openPreferences('confidence')}>
        Adjust my study pace <Icon name="arrow" size={16} />
      </button>
      <small className="form-hint">An estimate based on units, course difficulty, and your confidence.</small>
    </section>
  );

  return (
    <div className="app">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      {sidebarOpen && (
        <div
          className="sidebar-backdrop"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}
      <aside className={cn('sidebar', { 'sidebar-open': sidebarOpen })}>
        <div className="sidebar-header">
          <a
            className="brand"
            href="#overview"
            onClick={e => {
              e.preventDefault();
              navigate('Overview');
            }}
          >
            <span className="brand-icon">
              <Icon name="book" size={23} />
            </span>
            <span className="brand-copy">
              <strong>
                J.A.K.E.V. <span>Edu</span>
              </strong>
              <small>Your academic compass</small>
            </span>
          </a>
          <button
            type="button"
            className="sidebar-close-btn"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close navigation menu"
          >
            <Icon name="close" size={20} />
          </button>
        </div>
        <div className="workspace-label">YOUR WORKSPACE</div>
        <nav className="main-nav" aria-label="Main navigation">
          {navigation.map(item => (
            <button
              key={item.label}
              className={cn({ active: page === item.label })}
              aria-current={page === item.label ? 'page' : undefined}
              onClick={() => navigate(item.label)}
            >
              <Icon name={item.icon} size={19} />
              <span>{item.label}</span>
              {item.label === 'My planner' && planned.length > 0 && (
                <span className="nav-count">{planned.length}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="advisor-tip">
            <Icon name="compass" size={25} />
            <strong>
              A goal. A plan.
              <br />A world of possibilities.
            </strong>
            <p>Your next chapter starts with one thoughtful step.</p>
            <button className="text-button" onClick={() => navigate('Prerequisite map')}>
              Explore my pathway <Icon name="arrow" size={15} />
            </button>
          </div>
          <button
            className={cn('settings-link', { active: page === 'Settings' })}
            aria-current={page === 'Settings' ? 'page' : undefined}
            onClick={() => navigate('Settings')}
          >
            <Icon name="settings" size={18} />
            Profile & settings
          </button>
          <div className="offline">
            <Icon name="shield" size={14} />
            <span>{storageError ? 'Changes not saved' : 'Saved on this device'}</span>
          </div>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="hamburger-btn"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open navigation menu"
            >
              <Icon name="menu" size={20} />
            </button>
            <div className="breadcrumb">
              Workspace <Icon name="chevron" size={13} />
              <strong>{page}</strong>
            </div>
          </div>
          <div className="header-right">
            <span className="academic-year">
              <span />
              {program.code} · {program.catalogAY}
            </span>
            <button className="icon-button" aria-label="How Course Advisor works" onClick={() => setModal('help')}>
              <Icon name="info" size={20} />
            </button>
            <button className="avatar" aria-label="Edit student profile" onClick={() => setModal('profile')}>
              {initials}
            </button>
          </div>
        </header>

        <main className="main-content" id="main-content">
          {storageError && (
            <div className="validation-note warning" role="alert">
              Your browser could not save these changes. Keep this tab open and export your plan before leaving.
            </div>
          )}
          {demo && (
            <div className="demo-banner">
              <span>
                <Icon name="info" size={16} />
                You’re exploring a sample {program.code} student profile.
              </span>
              <button className="text-button" onClick={() => setModal('new-profile')}>
                Set up my profile <Icon name="arrow" size={14} />
              </button>
            </div>
          )}
          <div className="page-heading">
            <div>
              <div className="eyebrow">{page === 'Overview' ? 'LET’S LOOK AHEAD' : page.toUpperCase()}</div>
              <h1 ref={pageTitle} tabIndex={-1}>
                {title}
              </h1>
              <p>{descriptions[page]}</p>
            </div>
            <div className="page-actions">
              {page === 'My planner' && (
                <div className="page-actions-group">
                  <button className="primary advising-slip-btn" disabled={!planned.length} onClick={() => setModal('advising-slip')}>
                    <Icon name="spark" size={16} />
                    <span>Official Advising Slip</span>
                  </button>
                  <button className="secondary" disabled={!planned.length} onClick={exportPlan} title="Download text draft">
                    <Icon name="download" size={16} />
                    <span>Export .txt</span>
                  </button>
                </div>
              )}
              {page !== 'Settings' && (
                <button className="primary build-plan-btn" onClick={() => openPreferences('generate')}>
                  <Icon name="spark" size={17} />
                  <span>{planned.length ? 'Build a new plan' : 'Build my plan'}</span>
                </button>
              )}
            </div>
          </div>

          {page === 'Overview' && (
            <>
              <section className="journey-banner">
                <div className="banner-content">
                  <span className="banner-tag">
                    <Icon name="wave" size={15} />
                    {program.name} · {program.college}
                  </span>
                  <h2>
                    Big ambitions.
                    <br />
                    One semester at a time.
                  </h2>
                  <p>
                    Your path to graduation is taking shape.
                    <br />
                    Let’s make your next step a confident one.
                  </p>
                  <div className="banner-actions">
                    <button
                      className="primary"
                      onClick={() => navigate(planned.length ? 'My planner' : 'My progress')}
                    >
                      {planned.length ? 'Continue to my planner' : 'Review my course history'}
                      <Icon name="arrow" size={16} />
                    </button>
                    <span>
                      {profile.standing} <b>·</b> {done.length} of {courses.length} courses completed
                    </span>
                  </div>
                </div>
                <div className="banner-art" aria-hidden="true">
                  <JourneyArt progress={progress} />
                </div>
              </section>

              <section className="stats" aria-label="Academic summary">
                <Stat icon="cap" label="Degree progress" value={`${progress}%`} detail={`${earned} of ${total} units completed`}>
                  <div className="small-progress">
                    <span style={{ width: `${progress}%` }} />
                  </div>
                </Stat>
                <Stat icon="check" label="Courses completed" value={String(done.length)} suffix={` / ${courses.length}`} detail="Every subject is a step forward." />
                <Stat icon="book" label="Ready to take" value={String(available.length)} suffix=" courses" detail="Your prerequisites are complete." />
                <Stat icon="calendar" label="Planned this semester" value={String(units)} suffix=" units" detail={`${planned.length} courses · ${workload.recommendedCredits}-unit suggested limit`} />
              </section>

              <div className="overview-columns">
                <section className="panel recommended">
                  <div className="panel-heading">
                    <div>
                      <h2>
                        Your next semester <span className="pill neutral">Draft plan</span>
                      </h2>
                      <p>{termLabel}</p>
                    </div>
                    <button className="text-button" onClick={() => navigate('My planner')}>
                      View planner <Icon name="arrow" size={15} />
                    </button>
                  </div>
                  {planned.length ? (
                    <>
                      <div className="table-label">
                        <span>COURSE</span>
                        <span>UNITS</span>
                        <span>STATUS</span>
                      </div>
                      <div className="course-list">{planned.slice(0, 4).map(renderCourse)}</div>
                      <div className="panel-footer">
                        <span>
                          <Icon name={planIssues.length ? 'info' : 'shield'} size={15} />
                          {planIssues.length
                            ? 'Some course requirements need a review.'
                            : 'Prerequisites checked against your course history.'}
                        </span>
                        <button className="text-button" onClick={() => navigate('My planner')}>
                          See all {planned.length} courses <Icon name="arrow" size={14} />
                        </button>
                      </div>
                    </>
                  ) : (
                    <EmptyState
                      icon="calendar"
                      title="Your next chapter starts here"
                      detail="Confirm your completed courses, then build a plan around your goal and study pace."
                      action="Build my plan"
                      onAction={() => openPreferences('generate')}
                    />
                  )}
                </section>
                {workloadCard}
              </div>

              <div className="bottom-grid">
                <section className="next-step">
                  <div className="next-step-main">
                    <span className="next-step-icon">
                      <Icon name="graph" size={24} />
                    </span>
                    <div>
                      <h3>Today’s courses. Tomorrow’s possibilities.</h3>
                      <p>See how your foundation connects to {goalCourse.title.toLowerCase()}.</p>
                    </div>
                  </div>
                  <button className="text-button" onClick={() => navigate('Prerequisite map')}>
                    Explore my pathway <Icon name="arrow" size={16} />
                  </button>
                </section>
              </div>
            </>
          )}

          {page === 'Course catalog' && (
            <section className="panel catalog">
              <div className="catalog-toolbar">
                <label className="search-field">
                  <Icon name="search" size={19} />
                  <input
                    type="search"
                    aria-label="Search courses"
                    placeholder="Search by course name or code…"
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                  />
                </label>
                <select aria-label="Filter by course category" value={category} onChange={e => setCategory(e.target.value)}>
                  {categories.map(x => (
                    <option key={x}>{x}</option>
                  ))}
                </select>
                <select aria-label="Filter by curriculum semester" value={catalogTerm} onChange={e => setCatalogTerm(e.target.value)}>
                  <option value="all">All semesters</option>
                  {terms.map((t, i) => (
                    <option key={t} value={i}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>
              <div className="filter-tabs" aria-label="Filter by course status">
                {['All courses', 'Ready to take', 'Completed', 'Needs prerequisites'].map(x => (
                  <button
                    key={x}
                    aria-pressed={status === x}
                    className={cn({ active: status === x })}
                    onClick={() => setStatus(x)}
                  >
                    {x}
                  </button>
                ))}
              </div>
              <div className="catalog-info" role="status">
                <span>
                  {filteredCourses.length} courses <b>·</b> {program.name} ({program.catalogAY})
                </span>
                {(query || category !== 'All categories' || status !== 'All courses' || catalogTerm !== 'all') && (
                  <button className="text-button" onClick={resetFilters}>
                    Clear filters
                  </button>
                )}
              </div>
              <div className="course-list">
                {filteredCourses.length ? (
                  filteredCourses.map(renderCourse)
                ) : (
                  <EmptyState
                    icon="search"
                    title="No courses found"
                    detail="Try a different course name, code, or filter."
                    action="Clear filters"
                    onAction={resetFilters}
                  />
                )}
              </div>
              <div className="panel-footer">
                <span>
                  <Icon name="info" size={16} />
                  Official MSU at Naawan Prospectus ({program.college}).
                </span>
              </div>
            </section>
          )}

          {page === 'My planner' && (
            <>
              <div className="planner-toolbar">
                <label className="semester-field">
                  <span>Planning for</span>
                  <select aria-label="Planning semester" value={term} onChange={e => saveTerm(Number(e.target.value))}>
                    {terms.map((t, i) => (
                      <option key={t} value={i}>
                        {t}
                      </option>
                    ))}
                  </select>
                </label>
                <span>
                  {planned.length} courses <b>·</b> {units} total units
                </span>
                <button
                  className="text-button"
                  onClick={() => {
                    resetFilters();
                    setStatus('Ready to take');
                    navigate('Course catalog');
                  }}
                >
                  <Icon name="plus" size={16} />
                  Add a course
                </button>
              </div>
              <div className="planner-layout">
                <section className="panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Your semester plan</h2>
                      <p>Selected courses stay in your draft when you change the planning semester.</p>
                    </div>
                    <span className="pill neutral">{planned.length} courses</span>
                  </div>
                  {planIssues.length > 0 && (
                    <div className="prereq-blocker-card" role="alert">
                      <div className="blocker-header">
                        <Icon name="alert" size={20} />
                        <div>
                          <strong>⚠️ Prerequisite Conflict Detected (Irregular Enrollment)</strong>
                          <p>The following planned course(s) cannot be taken this semester because their required foundation courses are not yet completed:</p>
                        </div>
                      </div>
                      <div className="blocker-list">
                        {planned.filter(c => missingPrerequisites(c).length > 0).map(c => {
                          const missing = missingPrerequisites(c);
                          return (
                            <div key={c.code} className="blocker-item">
                              <div className="blocker-item-title">
                                <strong>{c.code}</strong> — {c.title} ({c.units} units)
                              </div>
                              <div className="blocker-missing">
                                <span>Required:</span>
                                {missing.map(m => (
                                  <span key={m} className="missing-badge">{m}</span>
                                ))}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                  {units > workload.recommendedCredits && (
                    <div className="validation-note warning">
                      You’re {units - workload.recommendedCredits} units above your suggested limit. Consider removing a
                      course or adjusting your study pace.
                    </div>
                  )}
                  {planned.length ? (
                    <div className="course-list">
                      {planned.map(c => {
                        const rationale = graph.generateRationale(c, new Set(completed));
                        return (
                          <div className="planned-item" key={c.code}>
                            {renderCourse(c)}
                            <div className="plan-rationale">
                              <div className="rationale-content">
                                <span>
                                  <Icon name="info" size={14} />
                                  {eligible(c)
                                    ? goalRequirements.has(c.code)
                                      ? `A step toward ${goalCourse.title.toLowerCase()}`
                                      : rationale.headline
                                    : 'Prerequisites need attention'}
                                </span>
                                <p>
                                  {eligible(c)
                                    ? rationale.detail
                                    : `Complete ${missingPrerequisites(c).join(', ')} before taking this course.`}
                                  {c.co && ` Paired with companion ${c.co}.`}
                                </p>
                              </div>
                              <button
                                className="remove-plan-btn"
                                aria-label={`Remove ${c.code}${c.co ? ` and companion ${c.co}` : ''} from plan`}
                                onClick={() => removeCourse(c)}
                              >
                                Remove
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <EmptyState
                      icon="calendar"
                      title="Make room for what’s next"
                      detail="Build a suggested plan based on your completed courses and your academic goal."
                      action="Build my plan"
                      onAction={() => openPreferences('generate')}
                    />
                  )}
                </section>
                <div className="planner-summary">
                  {workloadCard}
                  <div className="panel adviser-note">
                    <Icon name="info" size={20} />
                    <h3>Ready for a conversation</h3>
                    <p>
                      Export your draft and review it with your adviser. They can confirm course offerings and enrollment
                      requirements.
                    </p>
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                      <button className="primary" onClick={() => setModal('advising-slip')} disabled={!planned.length}>
                        <Icon name="spark" size={16} />
                        Official Advising Slip (Print/PDF)
                      </button>
                      <button className="secondary" onClick={exportPlan} disabled={!planned.length}>
                        <Icon name="download" size={16} />
                        Download .txt
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}

          {page === 'Prerequisite map' && (
            <section className="panel map-panel">
              <div className="panel-heading">
                <div>
                  <h2>Where would you like to go?</h2>
                  <p>Choose a milestone to see the prerequisites that lead to it.</p>
                </div>
                <Icon name="compass" size={24} />
              </div>
              <div className="goal-chips">
                {goals.map(code => (
                  <button
                    key={code}
                    aria-pressed={activeGoalCode === code}
                    className={cn({ active: activeGoalCode === code })}
                    onClick={() => saveGoal(code)}
                  >
                    {courseByCode(code).title}
                  </button>
                ))}
              </div>
              <div className="pathway-summary">
                <div>
                  <span>YOUR MILESTONE</span>
                  <strong>{goalCourse.title}</strong>
                  <small>
                    {goalCourse.code} · {goalCourse.units} units
                  </small>
                </div>
                <div>
                  <span>BEFORE YOU GET THERE</span>
                  <strong>
                    {completed.includes(goalCourse.code)
                      ? 'Milestone completed'
                      : goalRequirements.size
                      ? `${goalRequirements.size} courses remaining`
                      : 'Prerequisites complete'}
                  </strong>
                  <small>Based on the course history you’ve recorded</small>
                </div>
              </div>
              {completed.includes(goalCourse.code) ? (
                <div className="validation-note">
                  <Icon name="check" size={20} />
                  You’ve marked this milestone as completed. Choose another goal to explore your next steps.
                </div>
              ) : (
                <>
                  <div className="map-legend">
                    <span>
                      <i />
                      Prerequisite course
                    </span>
                    <span>
                      <i className="done" />
                      Ready to take
                    </span>
                    <span>Open any course to see its requirements</span>
                  </div>
                  <div className="pathway-grid">
                    {goalTiers.map(([tier, list], index) => (
                      <section className="pathway-tier" key={tier}>
                        <div className="tier-header">
                          <span>Step {index + 1}</span>
                          <small>
                            {list.length} {list.length === 1 ? 'course' : 'courses'}
                          </small>
                        </div>
                        {list.map(c => (
                          <button
                            className={cn('pathway-course', { ready: eligible(c) })}
                            key={c.code}
                            onClick={() => setSelected(c)}
                          >
                            <span>
                              {c.code}
                              <Icon name={eligible(c) ? 'check' : 'chevron'} size={15} />
                            </span>
                            <strong>{c.title}</strong>
                            <small>
                              {c.units} units · {eligible(c) ? 'Ready to take' : 'Prerequisites first'}
                            </small>
                          </button>
                        ))}
                      </section>
                    ))}
                    <section className="pathway-tier goal-tier">
                      <div className="tier-header">
                        <span>Your milestone</span>
                        <Icon name="cap" size={17} />
                      </div>
                      <button className="pathway-course target" onClick={() => setSelected(goalCourse)}>
                        <span>
                          {goalCourse.code}
                          <Icon name="star" size={16} />
                        </span>
                        <strong>{goalCourse.title}</strong>
                        <small>
                          {goalCourse.units} units · {eligible(goalCourse) ? 'Ready to take' : 'Your destination'}
                        </small>
                      </button>
                      {!goalRequirements.size && (
                        <p className="form-hint">
                          No outstanding prerequisites in the curriculum. Check availability with your adviser.
                        </p>
                      )}
                    </section>
                  </div>
                </>
              )}
              <div className="panel-footer">
                <span>
                  <Icon name="info" size={16} />
                  Prerequisite sequence evaluated using Backward Chaining over {program.name}.
                </span>
                <button className="text-button" onClick={() => openPreferences('generate')}>
                  Plan toward this goal <Icon name="arrow" size={15} />
                </button>
              </div>
            </section>
          )}

          {page === 'My progress' && (
            <>
              <div className="progress-overview panel">
                <div className="progress-ring">
                  <strong>{progress}%</strong>
                  <span>complete</span>
                </div>
                <div>
                  <h2>Your journey so far</h2>
                  <p>
                    <strong>{done.length} courses</strong> completed · <strong>{earned} units</strong> earned ·{' '}
                    {total - earned} units to go
                  </p>
                  <div className="small-progress">
                    <span style={{ width: `${progress}%` }} />
                  </div>
                  <small className="form-hint">
                    Mark only courses you have passed or received credit for. Your plan updates as your history changes.
                  </small>
                </div>
                <button className="primary" onClick={() => openPreferences('generate')}>
                  Plan my next step <Icon name="arrow" size={16} />
                </button>
              </div>
              <section className="panel">
                <div className="panel-heading">
                  <div>
                    <h2>Your course history</h2>
                    <p>Open a semester to update your completed courses.</p>
                  </div>
                  <span className="pill neutral">{demo ? 'Sample history' : 'Self-reported history'}</span>
                </div>
                {terms.map((t, index) => {
                  const list = courses.filter(c => c.term === index);
                  const count = list.filter(c => completed.includes(c.code)).length;
                  return (
                    <details className="progress-term" key={t}>
                      <summary>
                        <span className="term-number">{String(index + 1).padStart(2, '0')}</span>
                        <span className="progress-term-heading">
                          <strong>{t}</strong>
                          <small>
                            {count} of {list.length} courses completed
                          </small>
                        </span>
                        <span className="small-progress">
                          <span style={{ width: `${list.length ? (count / list.length) * 100 : 0}%` }} />
                        </span>
                        <span className={cn('pill', count === list.length ? 'green' : 'neutral')}>
                          {count === list.length ? 'Complete' : `${count}/${list.length}`}
                        </span>
                        <Icon name="chevron" size={18} />
                      </summary>
                      <div className="progress-course-list">
                        {list.map(c => (
                          <div className="progress-course" key={c.code}>
                            <label className="completion-check">
                              <input
                                type="checkbox"
                                checked={completed.includes(c.code)}
                                onChange={() => toggleCompleted(c)}
                              />
                              <span>
                                <strong>{c.code}</strong> {c.title}
                              </span>
                              <small>{c.units} units</small>
                            </label>
                            <button
                              className="icon-button"
                              aria-label={`View requirements for ${c.code}`}
                              onClick={() => setSelected(c)}
                            >
                              <Icon name="info" size={17} />
                            </button>
                          </div>
                        ))}
                      </div>
                    </details>
                  );
                })}
              </section>
            </>
          )}

          {page === 'Settings' && (
            <div className="settings-grid">
              <section className="panel">
                <div className="panel-heading">
                  <div>
                    <h2>Student profile</h2>
                    <p>The starting point for your academic plan.</p>
                  </div>
                  <button className="text-button" onClick={() => setModal('profile')}>
                    Edit profile & degree <Icon name="arrow" size={15} />
                  </button>
                </div>
                <div className="profile-summary">
                  <span className="profile-avatar">{initials}</span>
                  <div>
                    <h3>{profile.fullName || 'Your student profile'}</h3>
                    <p>
                      {profile.studentId || 'No student ID added'} · {profile.standing}
                    </p>
                    {demo && <span className="pill amber">Sample profile</span>}
                  </div>
                </div>
                <dl className="profile-details">
                  <div>
                    <dt>Degree program</dt>
                    <dd>{program.name}</dd>
                  </div>
                  <div>
                    <dt>College</dt>
                    <dd>{program.college}</dd>
                  </div>
                  <div>
                    <dt>Campus</dt>
                    <dd>{program.campus}</dd>
                  </div>
                  <div>
                    <dt>Curriculum version</dt>
                    <dd>{program.catalogAY}</dd>
                  </div>
                  <div>
                    <dt>Total program units</dt>
                    <dd>{program.totalUnits} Units ({courses.length} Courses)</dd>
                  </div>
                  <div>
                    <dt>Study confidence pace</dt>
                    <dd>{profile.confidence} / 10</dd>
                  </div>
                </dl>
                {demo && (
                  <div className="form-actions">
                    <button className="primary" onClick={() => setModal('new-profile')}>
                      Set up my own profile
                    </button>
                  </div>
                )}
              </section>
              <section className="panel settings-data">
                <Icon name="shield" size={25} />
                <h2>Your data, on your device</h2>
                <p>
                  Your profile, course history, and current plan are saved in this browser. They don’t automatically sync
                  to other devices.
                </p>
                <button className="secondary" disabled={!planned.length} onClick={exportPlan}>
                  <Icon name="download" size={16} />
                  Export my plan
                </button>
                <hr />
                <h3>Start with a clean course history</h3>
                <p>
                  Clear completed courses and the current plan. Your profile and study preferences will stay saved.
                </p>
                <button className="text-button danger" onClick={() => setModal('reset')}>
                  Clear course history & plan
                </button>
              </section>
            </div>
          )}

          <footer className="footer">
            <span>
              <Icon name="wave" size={17} />
              Your path, a little clearer.
            </span>
            <span>
              {program.name} <b>·</b> {program.college} <b>·</b> {program.campus}
              <button className="text-button" onClick={() => setModal('help')}>
                About Course Advisor
              </button>
            </span>
          </footer>
        </main>
      </div>

      {notice && (
        <div className="notice" role="status">
          <Icon name="check" size={18} />
          <span>{notice}</span>
          <button className="icon-button" aria-label="Dismiss notification" onClick={() => setNotice('')}>
            <Icon name="close" size={16} />
          </button>
        </div>
      )}

      {/* Course Detail Modal Dialog */}
      <Dialog
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        title={selected?.title || 'Course details'}
        description={selected ? `${selected.code} · ${selected.units} units · ${selected.category}` : undefined}
      >
        {selected && (
          <>
            <div className="course-detail-meta">
              <span
                className={cn(
                  'pill',
                  completed.includes(selected.code) || eligible(selected) ? 'green' : 'amber'
                )}
              >
                {courseStatus(selected)}
              </span>
              <span>{terms[selected.term]}</span>
            </div>
            <div className="detail-box">
              <h3>Before you take this course</h3>
              {selected.pre.length ? (
                <ul className="requirements-list">
                  {selected.pre.map(code => (
                    <li className="requirement" key={code}>
                      <Icon name={completed.includes(code) ? 'check' : 'clock'} size={16} />
                      <span>
                        <strong>{code}</strong> · {courseByCode(code).title}
                        <small>{completed.includes(code) ? 'Completed' : 'Not yet completed'}</small>
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>No individual course prerequisites are listed.</p>
              )}
              {needsStanding(selected) && (
                <p>
                  <strong>Academic Standing required:</strong> complete all lower-level courses.{' '}
                  {standingCourses.filter(c => !completed.includes(c.code)).length} remaining.
                </p>
              )}
              {selected.co && (
                <p>
                  <strong>Take together:</strong> {selected.co} · {courseByCode(selected.co).title}. Companion courses are
                  added and removed together.
                </p>
              )}
            </div>
            {!completed.includes(selected.code) && !eligible(selected) && (
              <p className="field-error">Complete the missing requirements before adding this course to your plan.</p>
            )}
            {selected.co && !completed.includes(selected.co) && !eligible(courseByCode(selected.co)) && (
              <p className="field-error">The companion course {selected.co} also has unfinished prerequisites.</p>
            )}
            <div className="dialog-actions">
              {!completed.includes(selected.code) && (
                <button
                  className="primary"
                  disabled={
                    plan.includes(selected.code) ||
                    !eligible(selected) ||
                    Boolean(selected.co && !completed.includes(selected.co) && !eligible(courseByCode(selected.co)))
                  }
                  onClick={() => addCourse(selected)}
                >
                  <Icon name={plan.includes(selected.code) ? 'check' : 'plus'} size={16} />
                  {plan.includes(selected.code) ? 'Already in your plan' : 'Add to my plan'}
                </button>
              )}
              <button className="secondary" onClick={() => toggleCompleted(selected)}>
                <Icon name="check" size={16} />
                {completed.includes(selected.code) ? 'Mark as not completed' : 'Mark as completed'}
              </button>
            </div>
            <small className="form-hint">
              Mark completion only for courses you have already passed or received credit for.
            </small>
          </>
        )}
      </Dialog>

      {/* Profile & Onboarding Dialog (Mandatory First-Run Intake Gate) */}
      <Dialog
        open={['onboarding', 'profile', 'new-profile'].includes(modal)}
        onClose={() => {
          if (modal !== 'onboarding') setModal('');
        }}
        closable={modal !== 'onboarding'}
        title={
          modal === 'profile'
            ? 'Edit Student Profile & Program'
            : modal === 'new-profile'
            ? 'Create New Student Profile'
            : 'Welcome to J.A.K.E.V. Edu Course Advisor'
        }
        description={
          modal === 'onboarding'
            ? 'Select your degree program and academic standing. The system will load your exact prospectus, prerequisite tree, and workload stress gauge.'
            : 'A few details help us keep your advising recommendations accurate and personalized.'
        }
        wide
      >
        {['onboarding', 'profile', 'new-profile'].includes(modal) && (
          <ProfileForm
            key={modal}
            profile={modal === 'new-profile' ? blankProfile : profile}
            onboarding={modal !== 'profile'}
            initialGoal={activeGoalCode}
            onSave={saveProfile}
            onDemo={modal === 'onboarding' ? exploreDemo : undefined}
          />
        )}
      </Dialog>

      {/* Auto-Plan Generator Dialog */}
      <Dialog
        open={modal === 'generate' || modal === 'confidence'}
        onClose={() => setModal('')}
        title={modal === 'generate' ? 'Let’s build your next semester' : 'Find a pace that fits you'}
        description="Your course history, academic goal, and confidence guide the recommendation."
      >
        <div className="dialog-stepper">
          <span>
            <Icon name="check" size={14} />
            {done.length} courses completed
          </span>
          <span>
            <Icon name="calendar" size={14} />
            {termLabel}
          </span>
        </div>
        {modal === 'generate' && (
          <label className="field">
            <span>Your academic goal</span>
            <select value={activeGoalCode} onChange={e => saveGoal(e.target.value)}>
              {program.milestones.map(m => (
                <option key={m.code} value={m.code}>
                  {m.title} ({m.code})
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="field confidence-field">
          <span>
            How confident do you feel about your studies?
            <strong>{draftConfidence} / 10</strong>
          </span>
          <input
            type="range"
            min="1"
            max="10"
            step="1"
            value={draftConfidence}
            onChange={e => setDraftConfidence(Number(e.target.value))}
          />
          <span className="confidence-scale">
            <small>I need a gentler pace</small>
            <small>Ready for a challenge</small>
          </span>
        </label>
        <div className="detail-box plan-preview">
          <Icon name="leaf" size={24} />
          <div>
            <h3>
              {modal === 'generate'
                ? `${proposal.codes.length} courses · ${proposal.units} units`
                : `${previewWorkload.recommendedCredits}-unit suggested limit`}
            </h3>
            <p>
              {modal === 'generate'
                ? `Up to ${previewWorkload.recommendedCredits} units, with prerequisites and companion courses checked.`
                : 'Your workload estimate will update when you save your study pace.'}
            </p>
          </div>
        </div>
        {modal === 'generate' && (
          <>
            <p className="form-hint">
              Courses toward your goal are prioritized. Your adviser can confirm which courses are offered in the
              selected semester.
            </p>
            {!done.length && (
              <div className="validation-note">
                No completed courses recorded. If you’ve already passed courses,{' '}
                <button
                  className="text-button"
                  onClick={() => {
                    setModal('');
                    navigate('My progress');
                  }}
                >
                  update your course history
                </button>{' '}
                first.
              </div>
            )}
            {planned.length > 0 && (
              <p className="validation-note">Building a new plan will replace your current {planned.length}-course draft.</p>
            )}
            {!proposal.codes.length && (
              <p className="field-error">No eligible courses remain for a new plan. Review your course history or choose another goal.</p>
            )}
          </>
        )}
        <div className="dialog-actions">
          <button className="secondary" onClick={() => setModal('')}>
            Cancel
          </button>
          <button
            className="primary"
            disabled={modal === 'generate' && !proposal.codes.length}
            onClick={() => {
              if (modal === 'generate') generate();
              else {
                const next = { ...profile, confidence: draftConfidence };
                setProfile(next);
                persist('ca-profile', next);
                persist('ca-confidence', draftConfidence);
                setModal('');
                notify('Study pace updated. Your workload estimate has been refreshed.');
              }
            }}
          >
            <Icon name={modal === 'generate' ? 'spark' : 'check'} size={16} />
            {modal === 'generate' ? 'Create my plan' : 'Save study pace'}
          </button>
        </div>
      </Dialog>

      {/* Reset Confirmation Dialog */}
      <Dialog
        open={modal === 'reset'}
        onClose={() => setModal('')}
        title="Clear your course history and plan?"
        description="This removes all recorded course completions and your current semester draft from this browser. This cannot be undone."
        alert
      >
        <div className="dialog-actions">
          <button className="secondary" autoFocus onClick={() => setModal('')}>
            Keep my progress
          </button>
          <button
            className="primary danger"
            onClick={() => {
              setCompleted([]);
              persist('ca-completed', []);
              savePlan([]);
              setDemo(false);
              persist('ca-demo', false);
              setModal('');
              notify('Course history and plan cleared. Your profile is still saved.');
            }}
          >
            Clear history & plan
          </button>
        </div>
      </Dialog>

      {/* About Dialog */}
      <Dialog
        open={modal === 'help'}
        onClose={() => setModal('')}
        title="A clearer path through your curriculum"
        description="Course Advisor helps you prepare a thoughtful academic advising draft."
      >
        <ol className="help-steps">
          <li>
            <strong>Select your program.</strong> Choose from the 7 verified MSU at Naawan prospectuses.
          </li>
          <li>
            <strong>Record your progress.</strong> Mark the courses you have already passed.
          </li>
          <li>
            <strong>Choose a milestone.</strong> Explore the prerequisites for your thesis, practicum, or capstone.
          </li>
          <li>
            <strong>Find your pace.</strong> Set your confidence and review your estimated workload stress.
          </li>
          <li>
            <strong>Build and review.</strong> Adjust your plan and export it for advising consultations.
          </li>
        </ol>
        <div className="detail-box">
          <h3>{program.name}</h3>
          <p>
            Curriculum: {program.catalogAY} at {program.campus} ({program.college}). Total requirements: {program.totalUnits} units across {courses.length} courses.
          </p>
        </div>
        <p className="form-hint">Your data is stored in this browser. Clearing browser data will remove it.</p>
      </Dialog>
      <AdvisingSlipModal
        open={modal === 'advising-slip'}
        onClose={() => setModal('')}
        profile={profile}
        program={program}
        termLabel={termLabel}
        goalCourse={goalCourse}
        planned={planned}
        units={units}
        workload={workload}
        planIssues={planIssues}
        missingPrerequisites={missingPrerequisites}
        onDownloadTxt={exportPlan}
      />
    </div>
  );
}

function Stat({
  icon,
  label,
  value,
  suffix,
  detail,
  children,
}: {
  icon: string;
  label: string;
  value: string;
  suffix?: string;
  detail: string;
  children?: ReactNode;
}) {
  return (
    <article className="stat-card">
      <div className="stat-top">
        <span>{label}</span>
        <span className="stat-icon">
          <Icon name={icon} size={19} />
        </span>
      </div>
      <div className="stat-value">
        {value}
        <small>{suffix}</small>
      </div>
      <p className="stat-detail">{detail}</p>
      {children}
    </article>
  );
}

function EmptyState({
  icon,
  title,
  detail,
  action,
  onAction,
}: {
  icon: string;
  title: string;
  detail: string;
  action: string;
  onAction: () => void;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Icon name={icon} size={28} />
      </span>
      <h3>{title}</h3>
      <p>{detail}</p>
      <button className="secondary" onClick={onAction}>
        {action}
        <Icon name="arrow" size={16} />
      </button>
    </div>
  );
}

function JourneyArt({ progress }: { progress: number }) {
  return (
    <svg viewBox="0 0 360 230" fill="none">
      <circle cx="218" cy="107" r="94" stroke="#cbddd4" />
      <circle cx="218" cy="107" r="70" stroke="#cbddd4" strokeDasharray="3 7" />
      <path
        d="M16 186C61 186 45 82 110 82S150 186 211 167S247 54 320 54"
        stroke="#88b1a1"
        strokeWidth="2"
        strokeDasharray="5 6"
      />
      <rect x="32" y="123" width="153" height="68" rx="12" fill="white" stroke="#d3e3da" />
      <circle cx="58" cy="148" r="12" fill="#e4f0e9" />
      <path d="m53 148 3 3 6-7" stroke="#176b5b" strokeWidth="2" strokeLinecap="round" />
      <text x="80" y="151" fill="#3b6253" fontSize="11" fontFamily="inherit">
        YOUR JOURNEY
      </text>
      <text x="50" y="175" fill="#1d4939" fontSize="13" fontWeight="600" fontFamily="inherit">
        {progress}% closer to your goal
      </text>
      <rect x="193" y="34" width="142" height="72" rx="12" fill="#176b5b" />
      <path
        d="m210 56 14-6 14 6-14 6-14-6Zm5 4v7c6 4 12 4 18 0v-7m5-4v11"
        stroke="white"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <text x="209" y="91" fill="white" fontSize="12" fontWeight="500" fontFamily="inherit">
        Your next horizon
      </text>
      <circle cx="111" cy="82" r="7" fill="#176b5b" stroke="#eaf3ed" strokeWidth="5" />
      <circle cx="211" cy="167" r="6" fill="#fafcf9" stroke="#88b1a1" strokeWidth="2" />
      <path
        d="M248 197c9-9 15 9 24 0s15 9 24 0m-48 9c9-9 15 9 24 0s15 9 24 0"
        stroke="#8db5a3"
        strokeWidth="1.5"
      />
    </svg>
  );
}
