export type Course = {
  code: string;
  title: string;
  units: number;
  term: number;
  pre: string[];
  co?: string;
  category: 'Marine Science' | 'General Education' | 'Foundation';
  difficulty: number; // 1.0 to 10.0 scale
  isCapstone?: boolean;
};

// Historical difficulty rating mapping for curriculum course codes
const DIFFICULTY_RATINGS: Record<string, number> = {
  CHM032: 8.5,
  'CHM032.2': 8.5,
  MRB199: 9.0,
  MRB196: 8.5,
  MAT043: 8.2,
  CHM045: 8.0,
  'CHM045.1': 8.0,
  MRB198: 8.0,
  BIO121: 7.8,
  PHY101: 7.5,
  'PHY101.1': 7.2,
  PHY102: 7.6,
  'PHY102.1': 7.3,
  STT034: 7.2,
  MRB134: 7.6,
  MRB195: 7.5,
  OCN121: 7.4,
  OCN122: 7.3,
  MRB148: 7.5,
  MRB145: 7.2,
  MRB143: 7.0,
  MRB132: 7.0,
  MRB131: 6.8,
  MRB114: 6.5,
  BIO117: 7.0,
  BIO118: 7.0,
  BIO119: 6.8,
  BIO120: 6.6,
  MRB147: 6.7,
  MRB146: 6.9,
  MRB197: 6.5,
  CHM011: 6.8,
  'CHM011.1': 6.5,
  ERS100: 5.5,
  MRB125: 6.0,
  MRB117: 6.0,
  MRB122: 6.2,
};

function resolveDifficulty(code: string): number {
  if (DIFFICULTY_RATINGS[code]) return DIFFICULTY_RATINGS[code];
  if (code.startsWith('MRB') || code.startsWith('OCN')) return 6.8;
  if (code.startsWith('CHM') || code.startsWith('PHY') || code.startsWith('MAT')) return 7.2;
  if (code.startsWith('BIO') || code.startsWith('STT')) return 6.5;
  if (code.startsWith('PED') || code.startsWith('NST')) return 2.2;
  return 4.0; // General Education baseline
}

const rows: [string, string, number, number, string?, string?][] = [
  ['PHY101', 'Mechanics, Electromagnetism and Gravitational Theory', 2, 0, '', 'PHY101.1'],
  ['PHY101.1', 'Mechanics, Electromagnetism and Gravitational Theory (Lab.)', 1, 0, '', 'PHY101'],
  ['MAT043', 'Calculus (Integrative)', 4, 0],
  ['CHM011', 'Inorganic Chemistry', 2, 0, '', 'CHM011.1'],
  ['CHM011.1', 'Inorganic Chemistry (Lab.)', 1, 0, '', 'CHM011'],
  ['BIO117', 'Cell and Molecular Biology', 4, 0],
  ['GEC101', 'Understanding the Self', 3, 0],
  ['PED011', 'PATHFIT 1: Movement Competency Training', 2, 0],
  ['NST001', 'National Service Training Program 1', 3, 0],
  ['PHY102', 'Waves, Optics, and Thermodynamics', 2, 1, 'PHY101,PHY101.1', 'PHY102.1'],
  ['PHY102.1', 'Waves, Optics, and Thermodynamics (Lab.)', 1, 1, 'PHY101,PHY101.1', 'PHY102'],
  ['STT034', 'Inferential Statistics and Biometry', 3, 1],
  ['CHM032', 'Organic Chemistry', 2, 1, 'CHM011,CHM011.1', 'CHM032.2'],
  ['CHM032.2', 'Organic Chemistry (Lab.)', 2, 1, 'CHM011,CHM011.1', 'CHM032'],
  ['BIO118', 'General Physiology', 3, 1, 'BIO117'],
  ['GEC105', 'Readings in Philippine History', 3, 1],
  ['PED012', 'PATHFIT 2: Exercise-based Fitness Activities', 2, 1, 'PED011'],
  ['NST002', 'National Service Training Program 2', 3, 1, 'NST001'],
  ['OCN121', 'Geological and Physical Oceanography', 4, 2, 'PHY102,PHY102.1,MAT043'],
  ['CHM045', 'Biochemistry', 2, 2, 'CHM032,CHM032.2', 'CHM045.1'],
  ['CHM045.1', 'Biochemistry (Lab.)', 1, 2, 'CHM032,CHM032.2', 'CHM045'],
  ['BIO119', 'Developmental and Reproductive Biology', 3, 2, 'BIO118'],
  ['GEC102', 'Purposive Communication', 3, 2],
  ['GEC106', 'Art Appreciation', 3, 2],
  ['GEC103', 'The Contemporary World', 3, 2],
  ['GEC120', 'Wika at Kultura sa Mapayapang Lipunan', 3, 2],
  ['PED013', 'PATHFIT 3: Swimming', 2, 2, 'PED011'],
  ['OCN122', 'Biological and Chemical Oceanography', 4, 3, 'CHM011,CHM011.1'],
  ['ERS100', 'Earth Science', 3, 3],
  ['BIO120', 'Ecology', 3, 3],
  ['MRB147', 'Systematics, Biodiversity, and Evolution', 4, 3],
  ['GEC107', 'Ethics', 3, 3],
  ['FPE101', 'Fundamentals of Peace Education', 3, 3],
  ['PED014', 'PATHFIT 4: Team Sports', 2, 3, 'PED011'],
  ['MRB131', 'Marine Botany', 4, 4],
  ['MRB132', 'Marine Invertebrates', 4, 4, 'BIO117'],
  ['MRB114', 'Marine Plankton', 3, 4],
  ['BIO121', 'Genetics', 4, 4, 'STT034'],
  ['GEC130', 'Ekokritisismo at Pagpapahalaga sa Kalikasan', 3, 4],
  ['MRB125', 'Marine Geography and Geochemistry', 3, 4],
  ['MRB134', 'Marine Ecology', 4, 5, 'STT034,MRB131,MRB132'],
  ['MRB198', 'Research Methodology', 3, 5, 'STT034'],
  ['MRB117', 'Marine Protected Areas', 3, 5],
  ['GEC108', 'Science, Technology and Society', 3, 5],
  ['MRB146', 'Marine Microbiology & Fungi', 4, 5],
  ['MRB195', 'Fundamentals of Research', 2, 5, 'STT034'],
  ['GAS123', 'Gender and Society', 3, 5],
  ['GEC104', 'Mathematics in the Modern World', 3, 6],
  ['GEC110', 'History of Muslim Filipinos and Indigenous Peoples of MINSUPALA', 3, 6],
  ['GEC124', 'Entrepreneurial Mind', 3, 6],
  ['HAL101', 'Halal', 3, 6],
  ['MRB145', 'Coral Reefs', 3, 7, 'MRB134'],
  ['MRB143', 'Marine Vertebrates', 4, 7, 'MRB132'],
  ['MRB122', 'Remote Sensing and Geographic Information System', 3, 7],
  ['GEC109', 'Life and Works of Rizal', 3, 7],
  ['MRB148', 'Integrated Coastal/Marine Resources Management', 4, 7, 'MRB134'],
  ['MRB196', 'Experimental Research', 2, 7, 'MRB195'],
  ['MRB197', 'Practicum', 3, 8],
  ['MRB199', 'Undergraduate Thesis', 2, 8, 'MRB196'],
];

export const courses: Course[] = rows.map(([code, title, units, term, pre, co]) => {
  const category: Course['category'] =
    code.startsWith('MRB') || code.startsWith('OCN')
      ? 'Marine Science'
      : code.startsWith('GEC') || code.startsWith('GAS') || code.startsWith('FPE') || code.startsWith('HAL')
      ? 'General Education'
      : 'Foundation';

  return {
    code,
    title,
    units,
    term,
    pre: pre ? pre.split(',').map(s => s.trim()) : [],
    co,
    category,
    difficulty: resolveDifficulty(code),
    isCapstone: ['MRB199', 'MRB197', 'MRB148'].includes(code),
  };
});

export const terms = [
  'Year 1 · First semester',
  'Year 1 · Second semester',
  'Year 2 · First semester',
  'Year 2 · Second semester',
  'Year 3 · First semester',
  'Year 3 · Second semester',
  'Year 3 · Summer',
  'Year 4 · First semester',
  'Year 4 · Second semester',
];

export type StudentStanding =
  | '1st Year'
  | '2nd Year'
  | '3rd Year'
  | '4th Year'
  | 'Irregular';

export type DegreeProgram = {
  id: string;
  name: string;
  campus: string;
  catalogAY: string;
  totalUnits: number;
};

export type StudentProfile = {
  fullName: string;
  studentId: string;
  programId: string;
  academicYear: string;
  standing: StudentStanding;
  startingTerm: number;
  confidence: number;
  isConfigured: boolean;
};

export const ACADEMIC_YEARS = [
  'AY 2025–2026',
  'AY 2024–2025',
  'AY 2023–2024',
  'AY 2022–2023',
  'AY 2021–2022',
];

export const DEGREE_PROGRAMS: DegreeProgram[] = [
  {
    id: 'BS-MARSCI-MSUN',
    name: 'BS Marine Science',
    campus: 'MSU at Naawan',
    catalogAY: 'AY 2025–2026',
    totalUnits: 168,
  },
  {
    id: 'BS-BIO-MSUN',
    name: 'BS Biology (Marine Biology)',
    campus: 'MSU at Naawan',
    catalogAY: 'AY 2024–2025',
    totalUnits: 162,
  },
  {
    id: 'BS-FISH-MSUN',
    name: 'BS Fisheries',
    campus: 'MSU at Naawan',
    catalogAY: 'AY 2025–2026',
    totalUnits: 165,
  },
];

export const DEFAULT_PROFILE: StudentProfile = {
  fullName: 'Alex Santos',
  studentId: '2023-0145',
  programId: 'BS-MARSCI-MSUN',
  academicYear: 'AY 2025–2026',
  standing: '3rd Year',
  startingTerm: 4,
  confidence: 7,
  isConfigured: false,
};
