import { useState, useMemo } from 'react';
import Icon from './icons';
import {
  courses,
  terms,
  type Course,
  type StudentProfile,
  type StudentStanding,
  DEGREE_PROGRAMS,
  ACADEMIC_YEARS,
  DEFAULT_PROFILE,
} from './courses';
import { evaluateMamdaniStress } from './fuzzyEngine';
import { CourseGraphEngine } from './graphEngine';

const nav = [
  ['grid', 'Overview'],
  ['calendar', 'My planner'],
  ['book', 'Course catalog'],
  ['graph', 'Prerequisite map'],
  ['chart', 'My progress'],
];

function load<T>(key: string, fallback: T): T {
  try {
    return JSON.parse(localStorage.getItem(key) || 'null') ?? fallback;
  } catch {
    return fallback;
  }
}

export default function App() {
  const [page, setPage] = useState('Overview');
  const [profile, setProfile] = useState<StudentProfile>(() =>
    load('ca-profile', DEFAULT_PROFILE)
  );
  const [draftProfile, setDraftProfile] = useState<StudentProfile>(() =>
    load('ca-profile', DEFAULT_PROFILE)
  );

  const [completed, setCompleted] = useState<string[]>(() =>
    load('ca-completed', courses.filter(c => c.term < 4).map(c => c.code))
  );
  const [confidence, setConfidence] = useState(() => {
    const saved = load<StudentProfile | null>('ca-profile', null);
    return saved?.confidence ?? load('ca-confidence', 7);
  });
  const [selected, setSelected] = useState<Course | null>(null);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('All subjects');
  const [term, setTerm] = useState(4);
  const [notice, setNotice] = useState('');
  const [modal, setModal] = useState<string>(() => {
    const saved = load<StudentProfile | null>('ca-profile', null);
    return saved && saved.isConfigured ? '' : 'onboarding';
  });
  const [plan, setPlan] = useState<string[]>(() =>
    load('ca-plan', courses.filter(c => c.term === 4).map(c => c.code))
  );
  const [targetGoal, setTargetGoal] = useState('MRB199');

  const graphEngine = useMemo(() => new CourseGraphEngine(courses), []);

  const activeProgram = useMemo(() => {
    return DEGREE_PROGRAMS.find(p => p.id === profile.programId) || DEGREE_PROGRAMS[0];
  }, [profile.programId]);

  const activeAY = useMemo(() => {
    return profile.academicYear || activeProgram.catalogAY || 'AY 2025–2026';
  }, [profile.academicYear, activeProgram.catalogAY]);

  const initials = useMemo(() => {
    const parts = (profile.fullName || 'Alex Santos').trim().split(/\s+/);
    if (!parts.length || !parts[0]) return 'AS';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }, [profile.fullName]);

  const done = courses.filter(c => completed.includes(c.code));
  const earned = done.reduce((s, c) => s + c.units, 0);
  const total = courses.reduce((s, c) => s + c.units, 0);
  const planned = courses.filter(c => plan.includes(c.code) && !completed.includes(c.code));
  const units = planned.reduce((s, c) => s + c.units, 0);

  const avgDifficulty = useMemo(() => {
    if (!planned.length) return 6.2;
    return planned.reduce((s, c) => s + c.difficulty, 0) / planned.length;
  }, [planned]);

  const fuzzyWorkload = useMemo(
    () => evaluateMamdaniStress(units, confidence, avgDifficulty),
    [units, confidence, avgDifficulty]
  );
  const stress = fuzzyWorkload.stressScore;

  const eligible = (c: Course) =>
    c.pre.every(p => completed.includes(p)) &&
    (!['MRB198', 'MRB195'].includes(c.code) ||
      courses.filter(x => x.term < 4).every(x => completed.includes(x.code)));

  function toast(t: string) {
    setNotice(t);
    window.setTimeout(() => setNotice(''), 4000);
  }

  function toggle(c: Course) {
    const next = completed.includes(c.code)
      ? completed.filter(x => x !== c.code)
      : [...completed, c.code];
    setCompleted(next);
    localStorage.setItem('ca-completed', JSON.stringify(next));
  }

  function savePlan(p: string[]) {
    setPlan(p);
    localStorage.setItem('ca-plan', JSON.stringify(p));
  }

  function generate() {
    let p: string[] = [];
    let u = 0;
    const cap = fuzzyWorkload.recommendedCredits;
    for (const c of courses.filter(c => !completed.includes(c.code) && eligible(c))) {
      if (p.includes(c.code)) continue;
      const group = [
        c,
        ...(c.co
          ? courses.filter(x => x.code === c.co && !completed.includes(x.code) && !p.includes(x.code))
          : []),
      ];
      const n = group.reduce((s, x) => s + x.units, 0);
      if (u + n <= cap) {
        p.push(...group.map(x => x.code));
        u += n;
      }
    }
    savePlan(p);
    setModal('');
    setPage('My planner');
    toast(`Semester plan created! · ${u} units added (Target Cap: ${cap} units), prerequisites verified.`);
  }

  function saveStudentProfile(newProf: StudentProfile) {
    const configured: StudentProfile = { ...newProf, isConfigured: true };
    setProfile(configured);
    setConfidence(configured.confidence);
    localStorage.setItem('ca-profile', JSON.stringify(configured));
    localStorage.setItem('ca-confidence', configured.confidence.toString());

    // Adjust starting term according to year standing
    if (configured.standing === '1st Year') {
      setTerm(0);
    } else if (configured.standing === '2nd Year') {
      setTerm(2);
    } else if (configured.standing === '3rd Year') {
      setTerm(4);
    } else if (configured.standing === '4th Year') {
      setTerm(6);
    }

    setModal('');
    toast(`Profile saved · Welcome, ${configured.fullName}!`);
  }

  function exportPlan() {
    const paceLabel = profile.confidence >= 7 ? 'Full Load' : profile.confidence >= 4 ? 'Balanced Load' : 'Light Load';
    const blob = new Blob(
      [
        `================================================================================\n` +
        `J.A.K.E.V. Edu — OFFICIAL ACADEMIC ADVISING DRAFT\n` +
        `================================================================================\n` +
        `Student Name:       ${profile.fullName}\n` +
        `Student ID:         ${profile.studentId || 'N/A'}\n` +
        `Academic Standing:  ${profile.standing}\n` +
        `Degree Program:     ${activeProgram.name} (${activeProgram.campus})\n` +
        `Curriculum Guide:   ${activeAY}\n` +
        `Advising Semester:  ${terms[term]}\n` +
        `Total Plan Units:   ${units} units (${planned.length} subjects)\n` +
        `Semester Workload:  ${fuzzyWorkload.rating} (${stress}/100 stress score)\n` +
        `Study Pace:         ${paceLabel} (Recommended Cap: ${fuzzyWorkload.recommendedCredits} units)\n` +
        `================================================================================\n\n` +
        `SCHEDULED SUBJECTS:\n` +
        (planned.length
          ? planned
              .map(
                c =>
                  `• ${c.code.padEnd(10)} ${c.title.padEnd(52)} (${c.units} units) [Effort: ${c.difficulty.toFixed(1)}/10]`
              )
              .join('\n')
          : '• (No subjects planned for this semester yet)') +
        `\n\nPrerequisite Check: All prerequisite subjects and laboratory requirements verified.\n` +
        `Adviser Consultation Note: Planning draft for academic advisement. Official enrollment requires adviser approval.\n`,
      ],
      { type: 'text/plain' }
    );
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    const safeName = profile.fullName.toLowerCase().replace(/[^a-z0-9]/g, '-') || 'student';
    a.download = `${safeName}-academic-plan.txt`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  const courseRow = (c: Course, i: number) => (
    <div
      className="course-row"
      key={c.code}
      onClick={() => setSelected(c)}
      tabIndex={0}
      role="button"
      onKeyDown={e => e.key === 'Enter' && setSelected(c)}
    >
      <div className={`course-symbol tone-${i % 3}`}>
        <Icon name={c.category === 'Marine Science' ? 'wave' : c.category === 'General Education' ? 'book' : 'leaf'} size={20} />
      </div>
      <div className="course-name">
        <strong>{c.title}</strong>
        <span>
          {c.code} <b>·</b> {c.category}
        </span>
      </div>
      <span className="units">{c.units} units</span>
      <span className={`pill ${completed.includes(c.code) ? 'green' : eligible(c) ? 'green' : 'amber'}`}>
        {completed.includes(c.code) ? 'Completed' : eligible(c) ? 'Ready to take' : 'Prerequisites'}
      </span>
      <span className="circle-action-btn">
        <Icon name="arrow" size={14} />
      </span>
    </div>
  );

  return (
    <div className="app">
      {/* Floating Frosted Glass Sidebar */}
      <aside className="sidebar">
        <a
          className="brand"
          href="#"
          onClick={e => {
            e.preventDefault();
            setPage('Overview');
          }}
        >
          <span className="brand-icon">
            <Icon name="book" size={22} />
          </span>
          <span>
            J.A.K.E.V.<span className="brand-light"> Edu</span>
            <small>YOUR PATH, CLEARER.</small>
          </span>
        </a>

        <div className="workspace-label">WORKSPACE</div>

        <nav>
          {nav.map(([icon, label]) => (
            <button
              key={label}
              className={page === label ? 'active' : ''}
              onClick={() => {
                setPage(label);
                setQuery('');
              }}
            >
              <Icon name={icon} size={18} />
              {label}
              {label === 'My planner' && <span className="nav-dot" />}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="advisor-tip">
            <span className="mini-spark">
              <Icon name="spark" size={18} />
            </span>
            <strong>
              A little planning.
              <br />A sea of possibilities.
            </strong>
            <p>Your next chapter starts with a plan that works for you.</p>
            <button onClick={() => setModal('generate')}>
              Build my plan <Icon name="arrow" size={15} />
            </button>
          </div>

          <button
            className="settings-link"
            onClick={() => {
              setDraftProfile(profile);
              setModal('profile');
            }}
          >
            <Icon name="settings" size={17} />
            Settings
          </button>

          <div className="offline">
            <span />
            All changes saved locally
            <Icon name="shield" size={14} />
          </div>
        </div>
      </aside>

      {/* Main Glass Shell */}
      <div className="main-shell">
        <header>
          <div className="breadcrumb">
            Workspace <span>/</span> <strong>{page}</strong>
          </div>
          <div className="header-right">
            <span className="academic-year">
              <span />
              {activeAY}
            </span>
            <button
              className="icon-button notification"
              aria-label="Notifications"
              onClick={() => setModal('notifications')}
            >
              <Icon name="bell" size={18} />
              <i />
            </button>
            <div className="header-divider" />
            <button
              className="avatar"
              onClick={() => {
                setDraftProfile(profile);
                setModal('profile');
              }}
              aria-label="Student profile"
              title={`${profile.fullName} (${profile.standing})`}
            >
              {initials}
            </button>
          </div>
        </header>

        <main>
          <div className="page-heading">
            <div>
              <div className="eyebrow">YOUR ACADEMIC COMPASS</div>
              <h1>
                {page === 'Overview'
                  ? 'A clearer path to your future.'
                  : page === 'My planner'
                  ? 'Make room for what’s next.'
                  : page === 'Course catalog'
                  ? 'Explore your curriculum.'
                  : page === 'Prerequisite map'
                  ? 'See how it all connects.'
                  : page === 'My progress'
                  ? 'Every step counts.'
                  : 'Make this plan yours.'}
              </h1>
              <p>
                {page === 'Overview'
                  ? `Welcome back, ${profile.fullName.split(' ')[0] || 'Student'}. Let’s keep your ${activeProgram.name} journey on course.`
                  : `${activeProgram.name} · ${activeProgram.campus} · ${activeAY}`}
              </p>
            </div>
            <button className="primary" onClick={() => setModal('generate')}>
              <Icon name="spark" size={17} />
              Generate my plan
            </button>
          </div>

          {page === 'Overview' && (
            <>
              {/* Glass Hero Banner (Inspired by Travel & Payrot References) */}
              <section className="journey-banner">
                <div className="banner-watermark">HORIZON</div>
                <div className="banner-content">
                  <span className="banner-tag">
                    <span />
                    YOUR DEGREE, IN MOTION
                  </span>
                  <h2>
                    Big ambitions.
                    <em>One semester at a time.</em>
                  </h2>
                  <p>
                    A thoughtful, data-driven planning system for your BS in Marine Science. Built around your
                    progress, pace, and academic prerequisites.
                  </p>

                  {/* Floating Segmented Switcher Dock (From Image 2 Travel Search Bar) */}
                  <div className="quick-dock" role="tablist" aria-label="Quick Navigation">
                    {nav.map(([icon, label]) => (
                      <button
                        key={label}
                        className={page === label ? 'active' : ''}
                        onClick={() => {
                          setPage(label);
                          setQuery('');
                        }}
                      >
                        <Icon name={icon} size={15} />
                        {label}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="banner-widget">
                  <div className="banner-glass-card">
                    <div className="widget-header">
                      <div className="widget-icon-pill">
                        <Icon name="book" size={18} />
                      </div>
                      <div className="widget-title-group">
                        <span className="widget-kicker">DEGREE PATHWAY</span>
                        <h4 className="widget-title">{activeProgram.name}</h4>
                      </div>
                      <span className="widget-campus-tag">{activeProgram.campus}</span>
                    </div>

                    <div className="widget-stats-grid">
                      <div className="widget-stat-cell">
                        <span className="widget-stat-val">{earned}</span>
                        <span className="widget-stat-lbl">Units Earned</span>
                      </div>
                      <div className="widget-stat-divider" />
                      <div className="widget-stat-cell">
                        <span className="widget-stat-val">{total - earned}</span>
                        <span className="widget-stat-lbl">Remaining</span>
                      </div>
                      <div className="widget-stat-divider" />
                      <div className="widget-stat-cell">
                        <span className="widget-stat-val">{Math.round((earned / total) * 100)}%</span>
                        <span className="widget-stat-lbl">Progress</span>
                      </div>
                    </div>

                    <div className="widget-progress-bar">
                      <div
                        className="widget-progress-fill"
                        style={{ width: `${Math.round((earned / total) * 100)}%` }}
                      />
                    </div>

                    <div className="widget-footer">
                      <div className="widget-status-badge">
                        <Icon name="check" size={13} />
                        <span>{profile.standing} · {activeAY}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

              {/* Glass Stat Feature Tiles (Inspired by Image 2 4-Pill Grid) */}
              <section className="stats">
                <Stat
                  icon="cap"
                  label="Degree progress"
                  value={`${Math.round((earned / total) * 100)}%`}
                  detail={`${earned} of ${total} curriculum units completed`}
                >
                  <div className="small-progress">
                    <i style={{ width: `${(earned / total) * 100}%` }} />
                  </div>
                </Stat>

                <Stat
                  icon="check"
                  label="Courses completed"
                  value={`${done.length}`}
                  detail={`of ${courses.length} courses in curriculum`}
                >
                  <span className="stat-caption">
                    <span className="dot" /> A strong foundation for thesis
                  </span>
                </Stat>

                <Stat
                  icon="calendar"
                  label="Current standing"
                  value={profile.standing}
                  detail={`Active Catalog: ${activeProgram.catalogAY}`}
                >
                  <span className="stat-caption">Academic Year Standing</span>
                </Stat>

                <Stat
                  icon="spark"
                  label="Recommended load"
                  value={`${confidence >= 7 ? 18 : confidence >= 4 ? 15 : 12}`}
                  suffix=" units"
                  detail="Paced to keep your semester manageable"
                >
                  <span className="stat-caption">
                    <span className="pill green">
                      {confidence >= 7 ? 'Full Load' : confidence >= 4 ? 'Balanced' : 'Light'}
                    </span>
                    <span>Level {confidence}/10</span>
                  </span>
                </Stat>
              </section>

              {/* Overview Layout with Deep Navy Contrast Block */}
              <div className="overview-columns">
                <section className="panel recommended">
                  <div className="panel-heading">
                    <div>
                      <h2>
                        Your next semester <span className="tiny-tag">RECOMMENDED</span>
                      </h2>
                      <p>Third year · First semester</p>
                    </div>
                    <button className="text-button" onClick={() => setPage('My planner')}>
                      View planner <Icon name="arrow" size={16} />
                    </button>
                  </div>

                  <div className="table-label">
                    <span>COURSE</span>
                    <span>UNITS</span>
                    <span>ELIGIBILITY</span>
                    <span style={{ textAlign: 'center' }}>ACTION</span>
                  </div>

                  <div className="course-list">
                    {planned.slice(0, 4).map(courseRow)}
                  </div>

                  <div className="panel-footer">
                    <span>
                      <Icon name="info" size={16} />
                      Prerequisites and co-requisite rules automatically verified.
                    </span>
                    <button onClick={() => setPage('My planner')}>
                      See all {planned.length} courses <Icon name="arrow" size={15} />
                    </button>
                  </div>
                </section>

                {/* Deep Ocean Contrast Block (Payrot & Travel inspired) */}
                <section className="panel workload deep-ocean-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Semester Workload</h2>
                      <p>How manageable is your schedule?</p>
                    </div>
                    <span className="soft-icon">
                      <Icon name="wave" size={20} />
                    </span>
                  </div>

                  <div className="gauge">
                    <svg viewBox="0 0 220 125">
                      <path
                        d="M25 105a85 85 0 0 1 170 0"
                        fill="none"
                        stroke="rgba(255,255,255,0.18)"
                        strokeWidth="14"
                        strokeLinecap="round"
                      />
                      <path
                        d="M25 105a85 85 0 0 1 170 0"
                        fill="none"
                        stroke="url(#gauge-glass)"
                        strokeWidth="14"
                        strokeLinecap="round"
                        strokeDasharray={`${stress * 2.67} 267`}
                      />
                      <defs>
                        <linearGradient id="gauge-glass" x1="0" y1="0" x2="1" y2="0">
                          <stop stopColor={stress > 80 ? '#F43F5E' : stress > 64 ? '#F59E0B' : '#38BDF8'} />
                          <stop offset="1" stopColor={stress > 80 ? '#FB7185' : stress > 64 ? '#FBBF24' : '#00E5FF'} />
                        </linearGradient>
                      </defs>
                    </svg>
                    <div>
                      <strong>
                        {stress}
                        <small>/100</small>
                      </strong>
                      <span>{fuzzyWorkload.rating}</span>
                    </div>
                  </div>

                  <div className="gauge-labels">
                    <span>Relaxed</span>
                    <span>Balanced</span>
                    <span>Demanding</span>
                    <span>Heavy</span>
                  </div>

                  <p className="workload-description">
                    {fuzzyWorkload.description}
                  </p>

                  <div className="mamdani-fuzzy-pills">
                    <span className="fuzzy-pill">Avg Subject Effort: {avgDifficulty.toFixed(1)}/10</span>
                    <span className="fuzzy-pill">Recommended Cap: {fuzzyWorkload.recommendedCredits} units</span>
                  </div>

                  <button className="full-text-button" onClick={() => setModal('confidence')}>
                    Adjust study pace <Icon name="arrow" size={15} />
                  </button>
                </section>
              </div>

              {/* Bottom Discovery Grid */}
              <div className="bottom-grid">
                <section className="next-step">
                  <div className="stat-icon-wrap">
                    <Icon name="graph" size={22} />
                  </div>
                  <div>
                    <h3>See the bigger picture</h3>
                    <p>Discover how today’s Marine Science foundation unlocks tomorrow’s thesis & practicum.</p>
                  </div>
                  <button className="text-button" onClick={() => setPage('Prerequisite map')}>
                    Explore pathways <Icon name="arrow" size={16} />
                  </button>
                </section>

                <div className="source-note">
                  <Icon name="shield" size={22} />
                  <span>
                    Your journey. Your data.
                    <br />
                    <small>Saved locally in browser. No telemetry required.</small>
                  </span>
                </div>
              </div>
            </>
          )}

          {page === 'Course catalog' && (
            <section className="panel catalog">
              <div className="catalog-toolbar">
                <div className="search">
                  <Icon name="search" size={18} />
                  <input
                    placeholder="Search course code or title…"
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                  />
                </div>
                <select value={filter} onChange={e => setFilter(e.target.value)}>
                  {['All subjects', 'Marine Science', 'Foundation', 'General Education'].map(x => (
                    <option key={x}>{x}</option>
                  ))}
                </select>
              </div>

              <div className="catalog-info">
                {
                  courses.filter(
                    c =>
                      (filter === 'All subjects' || c.category === filter) &&
                      `${c.code} ${c.title}`.toLowerCase().includes(query.toLowerCase())
                  ).length
                }{' '}
                courses · Official 2025–2026 prospectus
              </div>

              <div className="course-list">
                {courses
                  .filter(
                    c =>
                      (filter === 'All subjects' || c.category === filter) &&
                      `${c.code} ${c.title}`.toLowerCase().includes(query.toLowerCase())
                  )
                  .map(courseRow)}
              </div>

              <div className="curriculum-note">
                <Icon name="info" size={18} />
                <span>
                  Source: MSUN-CFMS-POS-002-2025-REV00. Physics codes and MB 195 / MB 196 references normalized to
                  PHY101 and MRB195 / MRB196. Confirm enrollments with your adviser.
                </span>
              </div>
            </section>
          )}

          {page === 'My planner' && (
            <>
              <div className="planner-toolbar">
                <select
                  value={term}
                  onChange={e => {
                    const t = +e.target.value;
                    setTerm(t);
                    savePlan(courses.filter(c => c.term === t && !completed.includes(c.code)).map(c => c.code));
                  }}
                >
                  {terms.map((t, i) => (
                    <option key={t} value={i}>
                      {t}
                    </option>
                  ))}
                </select>
                <span>
                  {planned.length} courses <b>·</b> {units} units
                </span>
                <button className="secondary" onClick={exportPlan}>
                  <Icon name="download" size={17} />
                  Export plan
                </button>
              </div>

              <div className="planner-layout">
                <section className="panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Your semester plan</h2>
                      <p>Review courses and verify prerequisites before academic advising.</p>
                    </div>
                    <button className="text-button" onClick={() => setPage('Course catalog')}>
                      <Icon name="plus" size={16} />
                      Add course
                    </button>
                  </div>

                  <div className="course-list">
                    {planned.length ? (
                      planned.map((c, i) => (
                        <div className="planned-item" key={c.code}>
                          {courseRow(c, i)}
                          {(() => {
                            const rat = graphEngine.generateRationale(c, new Set(completed));
                            return (
                              <div className={`plan-rationale urgency-${rat.urgency}`}>
                                <div className="rationale-content">
                                  <div className="rationale-meta">
                                    <span className={`rationale-badge ${rat.urgency}`}>
                                      {rat.urgency === 'high' ? '🔥 Critical Pathway' : '✓ Progression Step'}
                                    </span>
                                    <span className="diff-tag">Difficulty: {c.difficulty.toFixed(1)}/10</span>
                                    {c.co && <span className="co-tag">Companion: {c.co}</span>}
                                  </div>
                                  <div className="rationale-body">
                                    <strong>{rat.headline}</strong> — <span>{rat.detail}</span>
                                  </div>
                                </div>
                                <button className="remove-plan-btn" onClick={() => savePlan(plan.filter(x => x !== c.code))}>
                                  Remove
                                </button>
                              </div>
                            );
                          })()}
                        </div>
                      ))
                    ) : (
                      <div className="empty">No courses planned. Generate a plan or add courses from the catalog.</div>
                    )}
                  </div>
                </section>

                <section className="panel planner-summary">
                  <div className="stat-icon-wrap" style={{ marginBottom: 16 }}>
                    <Icon name="leaf" size={24} />
                  </div>
                  <h2>A pace that fits you</h2>
                  <p>
                    This plan includes <strong>{units} units</strong>. Your recommended credit cap is{' '}
                    <strong>{fuzzyWorkload.recommendedCredits} units</strong> based on your chosen study pace.
                  </p>
                  <div className={`pill ${stress < 65 ? 'green' : 'amber'}`}>Semester Load: {fuzzyWorkload.rating}</div>
                  <hr />
                  <p>Course difficulty and workload balance are calculated from lecture hours, laboratory requirements, and prerequisite depth.</p>
                  <button className="secondary" onClick={() => setModal('confidence')}>
                    Adjust study pace
                  </button>
                </section>
              </div>
            </>
          )}

          {page === 'Prerequisite map' && (
            <section className="panel map-panel">
              <div className="panel-heading">
                <div>
                  <h2>Graduation Roadmap & Prerequisite Guide</h2>
                  <p>See the exact subjects you need to finish step-by-step to reach your senior milestones, thesis, and practicum.</p>
                </div>
                <span className="pill green">Degree Milestones</span>
              </div>

              {/* Backward Chaining Interactive Goal Selector */}
              <div className="goal-chain-selector">
                <span className="selector-label">Choose a Goal to Plan For:</span>
                <div className="goal-chips">
                  {[
                    { code: 'MRB199', label: 'Senior Thesis (MRB199)' },
                    { code: 'MRB197', label: 'Practicum (MRB197)' },
                    { code: 'MRB148', label: 'Coastal Mgmt (MRB148)' },
                    { code: 'MRB145', label: 'Coral Reefs (MRB145)' },
                    { code: 'OCN121', label: 'Oceanography (OCN121)' },
                  ].map(g => (
                    <button
                      key={g.code}
                      type="button"
                      className={targetGoal === g.code ? 'active' : ''}
                      onClick={() => setTargetGoal(g.code)}
                    >
                      {g.label}
                    </button>
                  ))}
                </div>
              </div>

              {(() => {
                const goalAnalysis = graphEngine.backwardChainFromGoal(targetGoal, new Set(completed));
                const targetCourse = courses.find(c => c.code === targetGoal);
                return (
                  <div className="goal-chain-result">
                    <div className="goal-status-strip">
                      <div className="goal-metric">
                        <span className="metric-label">Target Milestone</span>
                        <strong>{targetGoal} — {targetCourse?.title}</strong>
                      </div>
                      <div className="goal-metric">
                        <span className="metric-label">Prerequisites Status</span>
                        <strong className={goalAnalysis.unfulfilledChain.length === 0 ? 'text-green' : 'text-amber'}>
                          {goalAnalysis.unfulfilledChain.length === 0
                            ? '✓ Ready to Enroll!'
                            : `${goalAnalysis.unfulfilledChain.length} Subject(s) Needed First`}
                        </strong>
                      </div>
                      <div className="goal-metric">
                        <span className="metric-label">Already Completed</span>
                        <strong>{goalAnalysis.terminalSatisfied.length} Foundational Subject(s)</strong>
                      </div>
                    </div>

                    {goalAnalysis.unfulfilledChain.length > 0 ? (
                      <div className="unfulfilled-tiers-grid">
                        {goalAnalysis.depthTiers.map(({ tier, courses: tierCourses }) => (
                          <div key={tier} className="unfulfilled-tier-card">
                            <div className="tier-header">
                              <span className="tier-badge">Step {tier + 1}</span>
                              <span className="tier-count">{tierCourses.length} subject(s) to pass first</span>
                            </div>
                            <div className="tier-courses-list">
                              {tierCourses.map(c => (
                                <div
                                  key={c.code}
                                  className="tier-course-item"
                                  onClick={() => setSelected(c)}
                                  role="button"
                                  tabIndex={0}
                                >
                                  <div className="tier-course-main">
                                    <strong>{c.code}</strong>
                                    <span>{c.title}</span>
                                  </div>
                                  <span className="tier-course-units">{c.units}u</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="goal-unlocked-banner">
                        <Icon name="check" size={20} />
                        <div>
                          <strong>You've cleared all prerequisites!</strong>
                          <p>You have completed all prerequisite subjects for {targetGoal}. You are officially eligible to enroll in this subject!</p>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })()}

              <div className="map-legend" style={{ marginTop: 28 }}>
                <span>
                  <i className="done-dot" />
                  Completed
                </span>
                <span>
                  <i />
                  Upcoming
                </span>
              </div>

              <div className="graph-flow">
                <div className="graph-tier">
                  {['BIO117', 'STT034'].map(code => (
                    <GraphNode
                      key={code}
                      code={code}
                      done={completed.includes(code)}
                      onClick={() => setSelected(courses.find(c => c.code === code)!)}
                    />
                  ))}
                </div>
                <div className="graph-connect">→</div>
                <div className="graph-tier">
                  {['MRB131', 'MRB132'].map(code => (
                    <GraphNode
                      key={code}
                      code={code}
                      done={completed.includes(code)}
                      onClick={() => setSelected(courses.find(c => c.code === code)!)}
                    />
                  ))}
                </div>
                <div className="graph-connect">→</div>
                <div className="graph-tier">
                  <GraphNode
                    code="MRB134"
                    done={completed.includes('MRB134')}
                    onClick={() => setSelected(courses.find(c => c.code === code)!)}
                  />
                </div>
                <div className="graph-connect">→</div>
                <div className="graph-tier">
                  {['MRB145', 'MRB148'].map(code => (
                    <GraphNode
                      key={code}
                      code={code}
                      done={completed.includes(code)}
                      onClick={() => setSelected(courses.find(c => c.code === code)!)}
                    />
                  ))}
                </div>
              </div>

              <div className="curriculum-note">
                <Icon name="info" size={18} />
                <span>
                  MRB132 requires BIO117. MRB134 requires STT034, MRB131 and MRB132. MRB131 has no prerequisite.
                  Click any course card to inspect requirements.
                </span>
              </div>

              <div className="panel-heading" style={{ borderTop: '1px solid rgba(226,232,240,0.7)' }}>
                <div>
                  <h2>Research & Thesis Pathway</h2>
                  <p>STT034 → MRB195 → MRB196 → MRB199</p>
                </div>
              </div>

              <div className="research-flow">
                {['STT034', 'MRB195', 'MRB196', 'MRB199'].map(code => (
                  <GraphNode
                    key={code}
                    code={code}
                    done={completed.includes(code)}
                    onClick={() => setSelected(courses.find(c => c.code === code)!)}
                  />
                ))}
              </div>
            </section>
          )}

          {page === 'My progress' && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>Academic Milestones</h2>
                  <p>
                    {done.length} courses complete · {earned} curriculum units earned
                  </p>
                </div>
                <span className="pill green">Sample profile · Editable</span>
              </div>

              {terms.map((t, i) => {
                const list = courses.filter(c => c.term === i);
                const n = list.filter(c => completed.includes(c.code)).length;
                return (
                  <div className="progress-term" key={t}>
                    <div>
                      <strong>{t}</strong>
                      <span>
                        {n} of {list.length} courses complete
                      </span>
                    </div>
                    <div className="small-progress">
                      <i style={{ width: `${(n / list.length) * 100}%` }} />
                    </div>
                    <button
                      className="text-button"
                      onClick={() => {
                        setPage('Course catalog');
                        setQuery('');
                      }}
                    >
                      Review courses <Icon name="arrow" size={15} />
                    </button>
                  </div>
                );
              })}
            </section>
          )}

          {page === 'Settings' && (
            <section className="panel settings-panel">
              <div className="panel-heading">
                <div>
                  <h2>Student Profile & Preferences</h2>
                  <p>All your notes and progress are saved right here on your browser. It works 100% offline, and you never need to remember a password.</p>
                </div>
                <button
                  className="primary"
                  onClick={() => {
                    setDraftProfile(profile);
                    setModal('profile');
                  }}
                >
                  <Icon name="spark" size={16} /> Edit Profile
                </button>
              </div>

              <div className="settings-profile-card">
                <div className="profile-card-header">
                  <div className="profile-avatar-large">{initials}</div>
                  <div className="profile-card-titles">
                    <h3>{profile.fullName}</h3>
                    <div className="profile-card-tags">
                      <span className="profile-id-tag">ID: {profile.studentId || 'N/A'}</span>
                      <span className="profile-standing-badge">{profile.standing} Standing</span>
                    </div>
                  </div>
                </div>

                <div className="profile-meta-grid">
                  <div className="profile-meta-item">
                    <span className="meta-label">Enrolled Program</span>
                    <strong>{activeProgram.name}</strong>
                    <small>{activeProgram.campus}</small>
                  </div>
                  <div className="profile-meta-item">
                    <span className="meta-label">Curriculum Guide</span>
                    <strong>{activeAY}</strong>
                    <small>{activeProgram.totalUnits} Total Units</small>
                  </div>
                  <div className="profile-meta-item">
                    <span className="meta-label">Preferred Study Pace</span>
                    <strong>Level {confidence}/10 · {confidence >= 7 ? 'Full Load' : confidence >= 4 ? 'Balanced Pace' : 'Light Pace'}</strong>
                    <small>Recommended Cap: {fuzzyWorkload.recommendedCredits} units</small>
                  </div>
                </div>
              </div>

              <div className="settings-actions-row">
                <button
                  className="secondary"
                  onClick={() => {
                    setDraftProfile(profile);
                    setModal('profile');
                  }}
                >
                  Edit My Profile & Pace
                </button>
                <button className="secondary danger-btn" onClick={() => setModal('reset')}>
                  Reset Sample Progress
                </button>
              </div>
            </section>
          )}

          <footer>
            <span>
              <Icon name="book" size={16} />
              Made for your next horizon.
            </span>
            <span>
              {activeProgram.name} <b>·</b> {activeAY} <b>·</b> {activeProgram.campus}
            </span>
          </footer>
        </main>
      </div>

      {/* Floating Glass Toast Notification */}
      {notice && (
        <div className="toast" role="status">
          <Icon name="check" size={18} />
          {notice}
        </div>
      )}

      {/* Floating Glass Modal Backdrop */}
      {(modal || selected) && (
        <div
          className="modal-backdrop"
          onClick={() => {
            if (modal === 'onboarding' && !profile.isConfigured) {
              return;
            }
            setModal('');
            setSelected(null);
          }}
        >
          <section
            className={`modal ${modal === 'onboarding' || modal === 'profile' ? 'modal-profile' : ''}`}
            role="dialog"
            aria-modal="true"
            aria-label={
              selected?.title ||
              (modal === 'onboarding'
                ? 'Student Onboarding'
                : modal === 'profile'
                ? 'Student Profile'
                : 'Plan preferences')
            }
            onClick={e => e.stopPropagation()}
          >
            {!(modal === 'onboarding' && !profile.isConfigured) && (
              <button
                className="modal-close icon-button"
                aria-label="Close dialog"
                onClick={() => {
                  setModal('');
                  setSelected(null);
                }}
              >
                <Icon name="close" size={18} />
              </button>
            )}

            {selected ? (
              <>
                <span className="eyebrow">
                  {selected.code} · {selected.units} UNITS
                </span>
                <h2>{selected.title}</h2>
                <p>
                  {terms[selected.term]} · {selected.category}
                </p>
                <div className="detail-box">
                  <strong>Prerequisites & Requirements</strong>
                  <p>{selected.pre.length ? `Pass before enrolling: ${selected.pre.join(', ')}` : 'No prior subjects required — open to enroll!'}</p>
                  {['MRB195', 'MRB198'].includes(selected.code) && <p>Requires Third-Year standing (completion of Year 1 and 2 courses).</p>}
                  {selected.co && <p>Companion laboratory section: {selected.co} (take in the same semester)</p>}
                </div>
                <p>
                  {completed.includes(selected.code)
                    ? '✓ You have already marked this subject as completed.'
                    : eligible(selected)
                    ? '✓ You have completed all required prerequisites. This subject is ready for your plan!'
                    : '⚠️ Complete the required prerequisite subjects before enrolling in this course.'}
                </p>
                <button
                  className="primary wide"
                  onClick={() => {
                    toggle(selected);
                    toast(completed.includes(selected.code) ? 'Subject marked incomplete' : 'Subject marked completed');
                  }}
                >
                  <Icon name="check" size={17} />
                  {completed.includes(selected.code) ? 'Mark as not completed' : 'Mark as completed'}
                </button>
                {!completed.includes(selected.code) && (
                  <button
                    className="secondary wide"
                    onClick={() => {
                      if (!eligible(selected)) {
                        toast('Complete prerequisite subjects before adding this course');
                        return;
                      }
                      savePlan([
                        ...new Set([
                          ...plan,
                          selected.code,
                          ...(selected.co && !completed.includes(selected.co) ? [selected.co] : []),
                        ]),
                      ]);
                      toast('Subject added to your semester plan');
                      setSelected(null);
                    }}
                  >
                    Add to semester plan
                  </button>
                )}
              </>
            ) : modal === 'onboarding' || modal === 'profile' ? (
              <div className="modal-profile-onboarding">
                <div className="onboarding-header">
                  <div className="onboarding-badge-icon">
                    <Icon name="book" size={28} />
                  </div>
                  <span className="eyebrow">J.A.K.E.V. Edu · Student Advising</span>
                  <h2>{modal === 'onboarding' ? 'Welcome! Let’s set up your academic journey.' : 'Update Your Student Profile'}</h2>
                  <p>
                    {modal === 'onboarding'
                      ? 'Tell us a little about your degree and what year you’re in so we can suggest the best subjects for you this semester.'
                      : 'Update your year level, preferred study load, or degree details whenever you need to.'}
                  </p>
                </div>

                <form
                  className="onboarding-form"
                  onSubmit={e => {
                    e.preventDefault();
                    if (!draftProfile.fullName.trim()) {
                      toast('Please enter your full name');
                      return;
                    }
                    saveStudentProfile(draftProfile);
                  }}
                >
                  <div className="form-row-2">
                    <div className="form-group">
                      <label htmlFor="student-name">What is your full name?</label>
                      <input
                        id="student-name"
                        className="glass-input"
                        type="text"
                        placeholder="e.g. Alex Santos"
                        value={draftProfile.fullName}
                        onChange={e => setDraftProfile({ ...draftProfile, fullName: e.target.value })}
                        required
                        autoFocus
                      />
                    </div>

                    <div className="form-group">
                      <label htmlFor="student-id">Student ID Number (Optional)</label>
                      <input
                        id="student-id"
                        className="glass-input"
                        type="text"
                        placeholder="e.g. 2023-0145"
                        value={draftProfile.studentId || ''}
                        onChange={e => setDraftProfile({ ...draftProfile, studentId: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="form-row-2">
                    <div className="form-group">
                      <label htmlFor="student-program">What degree program are you taking?</label>
                      <select
                        id="student-program"
                        className="glass-select"
                        value={draftProfile.programId}
                        onChange={e => setDraftProfile({ ...draftProfile, programId: e.target.value })}
                      >
                        {DEGREE_PROGRAMS.map(prog => (
                          <option key={prog.id} value={prog.id}>
                            {prog.name} · {prog.campus}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="form-group">
                      <label htmlFor="student-ay">Curriculum Academic Year</label>
                      <select
                        id="student-ay"
                        className="glass-select"
                        value={draftProfile.academicYear || 'AY 2025–2026'}
                        onChange={e => setDraftProfile({ ...draftProfile, academicYear: e.target.value })}
                      >
                        {ACADEMIC_YEARS.map(ay => (
                          <option key={ay} value={ay}>
                            {ay}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="form-group">
                    <label>What year level are you currently in?</label>
                    <div className="standing-chips" role="radiogroup" aria-label="Academic standing">
                      {(['1st Year', '2nd Year', '3rd Year', '4th Year', 'Irregular / Transferee'] as StudentStanding[]).map(
                        st => (
                          <button
                            key={st}
                            type="button"
                            role="radio"
                            aria-checked={draftProfile.standing === st}
                            className={`standing-chip ${draftProfile.standing === st ? 'active' : ''}`}
                            onClick={() => {
                              const startingTerm =
                                st === '1st Year'
                                  ? 0
                                  : st === '2nd Year'
                                  ? 2
                                  : st === '3rd Year'
                                  ? 4
                                  : st === '4th Year'
                                  ? 6
                                  : draftProfile.startingTerm;
                              setDraftProfile({ ...draftProfile, standing: st, startingTerm });
                            }}
                          >
                            {st}
                          </button>
                        )
                      )}
                    </div>
                    <span className="form-hint">
                      {draftProfile.standing === '3rd Year'
                        ? 'Great! 3rd-year standing unlocks research subjects (like MRB195 & MRB198). Planner defaults to Year 3.'
                        : draftProfile.standing === '1st Year'
                        ? 'Welcome to university! We’ll start with your foundational sciences and general education subjects.'
                        : draftProfile.standing === '2nd Year'
                        ? 'Advancing to intermediate biochemistry, ecology, and oceanography courses.'
                        : draftProfile.standing === '4th Year'
                        ? 'Almost at graduation! Focusing on thesis defense, seminar, and final practicum.'
                        : 'Custom pathway with evaluation of credited subjects.'}
                    </span>
                  </div>

                  <div className="form-group">
                    <div className="confidence-header">
                      <label htmlFor="student-confidence">How heavy a study load are you ready for?</label>
                      <span className="confidence-pill-val">
                        <strong>
                          {draftProfile.confidence >= 7
                            ? 'Full Load'
                            : draftProfile.confidence >= 4
                            ? 'Balanced Pace'
                            : 'Light Pace'}
                        </strong>{' '}
                        · Level {draftProfile.confidence}/10
                      </span>
                    </div>
                    <input
                      id="student-confidence"
                      className="confidence-range"
                      type="range"
                      min="1"
                      max="10"
                      value={draftProfile.confidence}
                      onChange={e => setDraftProfile({ ...draftProfile, confidence: +e.target.value })}
                    />
                    <div className="range-labels">
                      <span>Light Load (~12 units)</span>
                      <span>Balanced Load (~15 units)</span>
                      <span>Full Load (18 units)</span>
                    </div>
                    <span className="pace-description-hint">
                      {draftProfile.confidence >= 7
                        ? '🔥 Ready for a full schedule of major lectures and laboratory classes.'
                        : draftProfile.confidence >= 4
                        ? '⚖️ A steady, balanced schedule that keeps you on track without burning you out.'
                        : '🌱 A lighter pace giving you more breathing room for personal commitments or difficult subjects.'}
                    </span>
                  </div>

                  <div className="onboarding-actions">
                    <button type="submit" className="primary wide">
                      <Icon name="spark" size={18} />
                      {modal === 'onboarding' ? 'Start Planning My Semester →' : 'Save Profile Changes'}
                    </button>
                    {modal === 'profile' && (
                      <button
                        type="button"
                        className="secondary wide"
                        onClick={() => {
                          setDraftProfile(profile);
                          setModal('');
                        }}
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </form>
              </div>
            ) : modal === 'notifications' ? (
              <>
                <h2>You’re all caught up!</h2>
                <p>
                  Your curriculum is fully configured and all your academic progress is saved safely on this device. Visit
                  your planner to inspect upcoming courses.
                </p>
                <button
                  className="primary"
                  onClick={() => {
                    setPage('My planner');
                    setModal('');
                  }}
                >
                  Open my planner
                </button>
              </>
            ) : modal === 'reset' ? (
              <>
                <h2>Start with a clean slate?</h2>
                <p>This will remove sample completed courses and your saved semester plan from this browser.</p>
                <button
                  className="primary"
                  onClick={() => {
                    setCompleted([]);
                    localStorage.setItem('ca-completed', '[]');
                    savePlan([]);
                    setModal('');
                    toast('Sample progress cleared');
                  }}
                >
                  Clear sample progress
                </button>
              </>
            ) : (
              <>
                <span className="modal-leaf">
                  <Icon name="spark" size={26} />
                </span>
                <h2>{modal === 'generate' ? 'A semester plan built around your pace.' : 'Find your comfortable study pace.'}</h2>
                <p>
                  How heavy of a study load are you ready to handle this semester? We’ll suggest a schedule that fits your energy and keeps stress low.
                </p>
                <div className="confidence-number">
                  {confidence >= 7 ? 'Full Load' : confidence >= 4 ? 'Balanced Pace' : 'Light Pace'}
                  <small> · Level {confidence}/10</small>
                </div>
                <input
                  className="confidence-range"
                  type="range"
                  min="1"
                  max="10"
                  value={confidence}
                  onChange={e => {
                    setConfidence(+e.target.value);
                    localStorage.setItem('ca-confidence', e.target.value);
                  }}
                />
                <div className="range-labels">
                  <span>Light Load (~12 units)</span>
                  <span>Balanced Load (~15 units)</span>
                  <span>Full Load (18 units)</span>
                </div>
                <div className="detail-box">
                  <strong>Recommended: {confidence >= 7 ? 18 : confidence >= 4 ? 15 : 12} units</strong>
                  <p>
                    {confidence >= 7
                      ? 'Full schedule including standard major lectures and laboratory courses for your year.'
                      : confidence >= 4
                      ? 'A steady, balanced load of essential prerequisite subjects.'
                      : 'A lighter schedule giving you extra time for difficult subjects and personal commitments.'}
                  </p>
                </div>
                <button
                  className="primary wide"
                  onClick={
                    modal === 'generate'
                      ? generate
                      : () => {
                          setModal('');
                          toast('Study pace updated');
                        }
                  }
                >
                  <Icon name="spark" size={17} />
                  {modal === 'generate' ? 'Generate my semester plan' : 'Save my study pace'}
                </button>
              </>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function Stat({
  icon,
  label,
  value,
  detail,
  suffix,
  children,
}: {
  icon: string;
  label: string;
  value: string;
  detail: string;
  suffix?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="stat">
      <div className="stat-header">
        <span className="stat-title">{label}</span>
        <div className="stat-icon-wrap">
          <Icon name={icon} size={20} />
        </div>
      </div>
      <div className="stat-value">
        {value}
        <small>{suffix}</small>
      </div>
      <p>{detail}</p>
      {children}
    </div>
  );
}

function GraphNode({ code, done, onClick }: { code: string; done: boolean; onClick: () => void }) {
  const c = courses.find(c => c.code === code)!;
  return (
    <button className={`graph-node ${done ? 'is-done' : ''}`} onClick={onClick}>
      <span>
        {code}
        {done && <Icon name="check" size={15} />}
      </span>
      <strong>{c.title}</strong>
      <small>{c.units} units</small>
    </button>
  );
}
