import React from 'react';
import { Course, DegreeProgram, StudentProfile } from '../curricula';
import { WorkloadEvaluation } from '../fuzzyEngine';
import Icon from '../icons';

interface AdvisingSlipModalProps {
  open: boolean;
  onClose: () => void;
  profile: StudentProfile;
  program: DegreeProgram;
  termLabel: string;
  goalCourse: Course;
  planned: Course[];
  units: number;
  workload: WorkloadEvaluation;
  planIssues: string[];
  missingPrerequisites: (c: Course) => string[];
  onDownloadTxt: () => void;
}

export default function AdvisingSlipModal({
  open,
  onClose,
  profile,
  program,
  termLabel,
  goalCourse,
  planned,
  units,
  workload,
  planIssues,
  missingPrerequisites,
  onDownloadTxt,
}: AdvisingSlipModalProps) {
  if (!open) return null;

  const today = new Date().toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  });

  const handlePrint = () => {
    window.print();
  };

  const isOverload = units > workload.recommendedCredits;

  return (
    <div className="slip-modal-backdrop" onClick={onClose} role="dialog" aria-modal="true">
      <div className="slip-modal-container" onClick={e => e.stopPropagation()}>
        {/* Modal Toolbar (hidden when printing) */}
        <div className="slip-modal-toolbar no-print">
          <div className="slip-toolbar-info">
            <Icon name="check" size={18} />
            <span>Official Academic Advising Slip Preview</span>
          </div>
          <div className="slip-toolbar-actions">
            <button className="primary print-btn" onClick={handlePrint} title="Print or save as PDF">
              <Icon name="spark" size={16} />
              Print / Save as PDF
            </button>
            <button className="secondary" onClick={onDownloadTxt} title="Download plain text draft">
              <Icon name="download" size={16} />
              Download .TXT
            </button>
            <button className="close-btn" onClick={onClose} aria-label="Close dialog">
              ✕
            </button>
          </div>
        </div>

        {/* The Printable Advising Slip Document */}
        <div className="advising-slip-document" id="printable-advising-slip">
          {/* Header */}
          <div className="slip-header">
            <div className="slip-header-emblem">
              <div className="slip-crest">MSUN</div>
            </div>
            <div className="slip-header-text">
              <span className="slip-univ-name">MINDANAO STATE UNIVERSITY AT NAAWAN</span>
              <span className="slip-sub-office">Office of the University Registrar & Academic Advising Unit</span>
              <span className="slip-address">9023 Naawan, Misamis Oriental, Philippines · www.msunaawan.edu.ph</span>
              <h2 className="slip-title">OFFICIAL ACADEMIC ADVISING SLIP & STUDY PLAN</h2>
              <span className="slip-curriculum-badge">{program.catalogAY}</span>
            </div>
          </div>

          <div className="slip-divider-double" />

          {/* Student Profile & Program Details */}
          <div className="slip-section-grid">
            <div className="slip-grid-item">
              <span className="slip-meta-label">STUDENT NAME:</span>
              <span className="slip-meta-value slip-highlight">{profile.fullName || 'Alex Santos'}</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">STUDENT ID:</span>
              <span className="slip-meta-value">{profile.studentId || '2022-0042-NAA'}</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">DEGREE PROGRAM:</span>
              <span className="slip-meta-value">{program.name} ({program.code})</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">COLLEGE / CAMPUS:</span>
              <span className="slip-meta-value">{program.college} · {program.campus}</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">ACADEMIC STANDING:</span>
              <span className="slip-meta-value">{profile.standing}</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">TARGET SEMESTER:</span>
              <span className="slip-meta-value">{termLabel}</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">MILESTONE TRACK:</span>
              <span className="slip-meta-value">{goalCourse.code} — {goalCourse.title}</span>
            </div>
            <div className="slip-grid-item">
              <span className="slip-meta-label">DATE OF EVALUATION:</span>
              <span className="slip-meta-value">{today}</span>
            </div>
          </div>

          {/* Mamdani Fuzzy Stress Assessment Card */}
          <div className={`slip-stress-card ${isOverload ? 'overload' : 'balanced'}`}>
            <div className="slip-stress-header">
              <span className="slip-stress-title">MAMDANI FUZZY LOGIC & WORKLOAD AUDIT</span>
              <span className={`slip-stress-badge ${isOverload ? 'badge-danger' : 'badge-success'}`}>
                {isOverload ? 'OVERLOAD WARNING' : 'OPTIMAL LOAD CERTIFIED'}
              </span>
            </div>
            <div className="slip-stress-body">
              <div className="slip-stress-stat">
                <span className="stat-label">Total Enrolled Units</span>
                <span className="stat-val">{units} Units</span>
                <span className="stat-sub">{planned.length} scheduled courses</span>
              </div>
              <div className="slip-stress-stat">
                <span className="stat-label">Mamdani Stress Index</span>
                <span className="stat-val">{workload.stressScore} / 100</span>
                <span className="stat-sub">{workload.rating} Rating</span>
              </div>
              <div className="slip-stress-stat">
                <span className="stat-label">Recommended Credit Limit</span>
                <span className="stat-val">{workload.recommendedCredits} Units</span>
                <span className="stat-sub">Based on pace {profile.confidence}/10</span>
              </div>
              <div className="slip-stress-stat">
                <span className="stat-label">DAG Topological Status</span>
                <span className="stat-val" style={{ color: planIssues.length ? '#b91c1c' : '#15803d' }}>
                  {planIssues.length ? `${planIssues.length} Issues` : '0 Prerequisite Cycles'}
                </span>
                <span className="stat-sub">{planIssues.length ? 'Requires Review' : 'Verified Cycle-Free'}</span>
              </div>
            </div>
          </div>

          {/* Course Plan Table */}
          <div className="slip-table-container">
            <h3 className="slip-section-heading">RECOMMENDED SEMESTER COURSE ENROLLMENT</h3>
            <table className="slip-table">
              <thead>
                <tr>
                  <th style={{ width: '14%' }}>Course Code</th>
                  <th style={{ width: '42%' }}>Descriptive Title</th>
                  <th style={{ width: '10%' }} className="text-center">Units</th>
                  <th style={{ width: '22%' }}>Prerequisite(s)</th>
                  <th style={{ width: '12%' }} className="text-center">Status</th>
                </tr>
              </thead>
              <tbody>
                {planned.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center empty-cell">
                      No courses currently selected for this semester.
                    </td>
                  </tr>
                ) : (
                  planned.map(c => {
                    const missing = missingPrerequisites(c);
                    const hasConflict = missing.length > 0;
                    return (
                      <tr key={c.code} className={hasConflict ? 'row-conflict' : ''}>
                        <td className="code-cell">
                          <strong>{c.code}</strong>
                          {c.co && <div className="co-tag">Paired: {c.co}</div>}
                        </td>
                        <td className="title-cell">
                          {c.title}
                          {c.isCapstone && <span className="capstone-pill">Capstone</span>}
                        </td>
                        <td className="text-center units-cell">{c.units}</td>
                        <td className="prereq-cell">
                          {c.pre.length ? c.pre.join(', ') : <span className="none-muted">None</span>}
                          {hasConflict && (
                            <div className="missing-alert">
                              Missing: {missing.join(', ')}
                            </div>
                          )}
                        </td>
                        <td className="text-center status-cell">
                          {hasConflict ? (
                            <span className="badge-slip-danger">Blocked</span>
                          ) : (
                            <span className="badge-slip-success">Eligible</span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
              <tfoot>
                <tr className="slip-footer-row">
                  <td colSpan={2} className="footer-label">
                    <strong>TOTAL RECOMMENDED ENROLLMENT:</strong>
                  </td>
                  <td className="text-center footer-units">
                    <strong>{units}</strong>
                  </td>
                  <td colSpan={2} className="footer-notes">
                    Max Suggested Limit: {workload.recommendedCredits} Units
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Curriculum Integrity Notes */}
          <div className="slip-notes-block">
            <strong>ADVISING & ENROLLMENT GUIDELINES:</strong>
            <ol>
              <li>
                This advising slip is dynamically generated using the official <strong>{program.catalogAY}</strong> curriculum for <strong>{program.name}</strong>.
              </li>
              <li>
                Prerequisite chains are enforced using a backward chaining Directed Acyclic Graph (DAG) solver. All prerequisites must be validated with official grades prior to enrollment.
              </li>
              <li>
                Workload ratings are computed via a Mamdani Fuzzy Inference Engine balancing student academic confidence against course rigor.
              </li>
              {planIssues.length > 0 && (
                <li style={{ color: '#b91c1c', fontWeight: 'bold' }}>
                  Attention: {planIssues.join(' | ')}
                </li>
              )}
            </ol>
          </div>

          {/* Official Signatures Grid */}
          <div className="slip-signatures-grid">
            <div className="slip-sign-box">
              <div className="slip-sign-line" />
              <span className="slip-sign-name">{profile.fullName || 'Student Signature'}</span>
              <span className="slip-sign-role">Student (Signature over Printed Name)</span>
              <span className="slip-sign-date">Date: ________________________</span>
            </div>

            <div className="slip-sign-box">
              <div className="slip-sign-line" />
              <span className="slip-sign-name">Academic Program Adviser</span>
              <span className="slip-sign-role">{program.college}</span>
              <span className="slip-sign-date">Date: ________________________</span>
            </div>

            <div className="slip-sign-box">
              <div className="slip-sign-line" />
              <span className="slip-sign-name">Department Chairperson / Dean</span>
              <span className="slip-sign-role">College of {program.college.replace(/^College of\s+/i, '')}</span>
              <span className="slip-sign-date">Date: ________________________</span>
            </div>
          </div>

          {/* Footer watermark */}
          <div className="slip-doc-footer">
            <span>Doc Ref: MSUN-CA-{program.code}-{new Date().getFullYear()}-REV1</span>
            <span>Generated via J.A.K.E.V. Edu Course Advisor Engine</span>
          </div>
        </div>
      </div>
    </div>
  );
}
