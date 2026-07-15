import type {
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from '../types/api'
import { apiRequest } from './httpClient'

export const authApi = {
  login: (request: LoginRequest) =>
    apiRequest<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(request),
    }),

  register: (request: RegisterRequest) =>
    apiRequest<LoginResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(request),
    }),

  me: () => apiRequest<CurrentUser>('/auth/me'),
}
