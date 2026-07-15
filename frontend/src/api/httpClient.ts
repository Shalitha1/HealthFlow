import type { ApiErrorBody } from '../types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8084'
const TOKEN_KEY = 'healthflow_access_token'

export class ApiRequestError extends Error {
  readonly status: number
  readonly errorCode: string
  readonly fieldErrors: Record<string, string>

  constructor(body: Partial<ApiErrorBody>, status: number) {
    super(body.message ?? 'The request could not be completed.')
    this.name = 'ApiRequestError'
    this.status = status
    this.errorCode = body.errorCode ?? 'REQUEST_FAILED'
    this.fieldErrors = body.fieldErrors ?? {}
  }
}

export function getAccessToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setAccessToken(token: string) {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearAccessToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getAccessToken()
  const headers = new Headers(options.headers)

  headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  } catch {
    throw new ApiRequestError(
      { message: 'HealthFlow could not reach the API Gateway.' },
      0,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const body = contentType.includes('application/json')
    ? await response.json()
    : undefined

  if (!response.ok) {
    if (
      response.status === 401 &&
      path !== '/auth/login' &&
      path !== '/auth/register'
    ) {
      clearAccessToken()
      window.dispatchEvent(new Event('healthflow:unauthorized'))
    }
    throw new ApiRequestError(body ?? {}, response.status)
  }

  return body as T
}
