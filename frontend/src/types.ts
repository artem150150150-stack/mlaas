export type UserRole = 'USER' | 'ADMIN'
export type TaskType = 'CLASSIFICATION' | 'REGRESSION'
export type TaskStatus =
  | 'PENDING'
  | 'QUEUED'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'
export type ModelKind =
  | 'RANDOM_FOREST'
  | 'XGBOOST'
  | 'LOGISTIC_REGRESSION'
  | 'LINEAR_REGRESSION'

export interface UserDto {
  id: string
  email: string
  role: UserRole
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  user: UserDto
}

export interface ProjectDto {
  id: string
  name: string
  description: string | null
  ownerId: string
  createdAt: string
}

export interface DatasetDto {
  id: string
  projectId: string
  originalFilename: string
  taskType: TaskType
  targetColumn: string
  rowCount: number | null
  createdAt: string
}

export interface TrainingMetricsDto {
  accuracy: number | null
  precisionMacro: number | null
  recallMacro: number | null
  f1Macro: number | null
  rmse: number | null
  confusionMatrix: number[][] | null
  trainScore: number | null
  valScore: number | null
  overfittingEstimate: number | null
}

export interface TrainingTaskDto {
  id: string
  projectId: string
  datasetId: string
  mlModelId: string | null
  modelKind: ModelKind
  taskType: TaskType
  status: TaskStatus
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
  metrics: TrainingMetricsDto | null
}

export interface RecommendationDto {
  id: string
  code: string
  severity: string
  message: string
  detailsJson: string | null
  createdAt: string
}
