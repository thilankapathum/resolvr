// ── Enums ─────────────────────────────────────────────────────

export type UserRole =
  | 'TECHNICAL_OFFICER'
  | 'ENGINEER'
  | 'MANAGER'
  | 'HEAD'
  | 'ADMIN';

export type ComplaintStatus =
  | 'NOT_ASSIGNED'
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'ESCALATED_TO_ENGINEER'
  | 'RESOLVED'
  | 'CLOSED';

export type ComplaintPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type IssueCategory = 'DATA' | 'VOICE' | 'VOICE_AND_DATA' | 'OTHER';
export type DeviceType = 'PHONE' | 'DONGLE' | 'ROUTER' | 'IOT' | 'OTHER';
export type TechnologyType = 'GSM' | 'UMTS' | 'LTE' | 'NR';
export type AuditAction =
  | 'CREATED' | 'ASSIGNED' | 'STARTED'
  | 'ANALYSIS_ADDED' | 'ANALYSIS_EDITED'
  | 'SOLUTION_ADDED' | 'SOLUTION_EDITED'
  | 'ESCALATED_TO_ENGINEER' | 'CUSTOMER_FEEDBACK_TAKEN'
  | 'MARKED_RESOLVED' | 'CLOSED' | 'REOPENED' | 'REASSIGNED' | 'COMMENT_ADDED';

// ── Auth ──────────────────────────────────────────────────────

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
}

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  role: UserRole | null;
  active: boolean;
  emailVerified: boolean;
  regionId: number | null;
  regionName: string | null;
  districtIds: number[];
  districtNames: string[];
  createdAt: string;
}

// ── Region & District ────────────────────────────────────────

export interface DistrictResponse {
  id: number;
  name: string;
  code: string;
  regionId: number | null;
  regionName: string | null;
}

export interface RegionResponse {
  id: number;
  name: string;
  districts: DistrictResponse[];
}

// ── Complaint ─────────────────────────────────────────────────

export interface AttachmentResponse {
  id: number;
  entryType: 'ANALYSIS' | 'SOLUTION';
  entryId: number;
  originalName: string;
  mimeType: string;
  fileSizeBytes: number;
  createdAt: string;
  uploadedByName: string;
  /** Time-limited presigned MinIO URL — use directly in <img>, <video>, <a>. */
  url: string;
}

export interface AnalysisEntryResponse {
  id: number;
  content: string;
  edited: boolean;
  authorId: number;
  authorName: string;
  authorRole: string;
  createdAt: string;
  updatedAt: string;
  attachments: AttachmentResponse[];
}

export interface SolutionEntryResponse {
  id: number;
  content: string;
  solutionTargetDate: string | null;
  remarks: string | null;
  edited: boolean;
  authorId: number;
  authorName: string;
  authorRole: string;
  createdAt: string;
  updatedAt: string;
  attachments: AttachmentResponse[];
}

export interface AuditLogResponse {
  id: number;
  action: AuditAction;
  fromStatus: ComplaintStatus | null;
  toStatus: ComplaintStatus | null;
  notes: string | null;
  actorName: string;
  actorRole: string;
  createdAt: string;
}

export interface ComplaintResponse {
  id: number;
  refNumber: string;
  districtId: number;
  districtName: string;
  districtCode: string;
  regionId: number | null;
  regionName: string | null;
  status: ComplaintStatus;
  priority: ComplaintPriority;
  targetDate: string;
  createdById: number;
  createdByName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  assignedToRole: string | null;
  raisedBy: string;
  customerName: string;
  contactNumber: string;
  msisdns: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  issueCategory: IssueCategory;
  issueDescription: string;
  issueDuration: string | null;
  lastExperienced: string | null;
  technology: TechnologyType | null;
  additionalInfo: string | null;
  deviceType: DeviceType | null;
  signalBars: number | null;
  usingVpnApn: boolean | null;
  servingSitesCells: string | null;
  coverageQuality: string | null;
  customerFeedbackTaken: boolean;
  analysisEntries: AnalysisEntryResponse[];
  solutionEntries: SolutionEntryResponse[];
  auditLogs: AuditLogResponse[];
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
}

// ── Pagination ────────────────────────────────────────────────

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page (0-based)
  size: number;
  first: boolean;
  last: boolean;
}

// ── Error ─────────────────────────────────────────────────────

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  fieldErrors?: { [key: string]: string };
}

// ── Display helpers ───────────────────────────────────────────

export const STATUS_LABELS: Record<ComplaintStatus, string> = {
  NOT_ASSIGNED:          'Not Assigned',
  NOT_STARTED:           'Not Started',
  IN_PROGRESS:           'In Progress',
  ESCALATED_TO_ENGINEER: 'Escalated',
  RESOLVED:              'Resolved',
  CLOSED:                'Closed',
};

export const STATUS_BADGE_CLASS: Record<ComplaintStatus, string> = {
  NOT_ASSIGNED:          'badge-neutral badge-outline',
  NOT_STARTED:           'badge-accent badge-outline',
  IN_PROGRESS:           'badge-accent',
  ESCALATED_TO_ENGINEER: 'badge-accent',
  RESOLVED:              'badge-success',
  CLOSED:                'badge-info',
};

export const PRIORITY_LABELS: Record<ComplaintPriority, string> = {
  LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High',
};

export const PRIORITY_BADGE_CLASS: Record<ComplaintPriority, string> = {
  LOW:    'badge-neutral badge-outline',
  MEDIUM: 'badge-warning badge-outline',
  HIGH:   'badge-error badge-outline',
};

export const ROLE_LABELS: Record<UserRole, string> = {
  TECHNICAL_OFFICER: 'Technical Officer',
  ENGINEER:          'Engineer',
  MANAGER:           'Manager',
  HEAD:              'Head',
  ADMIN:             'Admin',
};

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  CREATED:               'Complaint Created',
  ASSIGNED:              'Assigned',
  STARTED:               'Started Analysis',
  ANALYSIS_ADDED:        'Analysis Added',
  ANALYSIS_EDITED:       'Analysis Edited',
  SOLUTION_ADDED:        'Solution Added',
  SOLUTION_EDITED:       'Solution Edited',
  ESCALATED_TO_ENGINEER: 'Escalated to Engineer',
  CUSTOMER_FEEDBACK_TAKEN: 'Customer Feedback Taken',
  MARKED_RESOLVED:       'Resolved',
  CLOSED:                'Closed',
  REOPENED:              'Reopened',
  REASSIGNED:            'Reassigned',
  COMMENT_ADDED:         'Comment Added',
};
