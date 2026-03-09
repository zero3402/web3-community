import api from './index'
import type { ApiResponse, LoginRequest, LoginResponse, RegisterRequest } from '@/types'

export const authApi = {
  login(data: LoginRequest) {
    return api.post<ApiResponse<LoginResponse>>('/auth/login', data)
  },

  register(data: RegisterRequest) {
    return api.post<ApiResponse<LoginResponse>>('/auth/register', data)
  },

  refresh(refreshToken: string) {
    return api.post<ApiResponse<LoginResponse>>('/auth/refresh', { refreshToken })
  },

  logout() {
    return api.post<ApiResponse<null>>('/auth/logout')
  },

  changePassword(currentPassword: string, newPassword: string) {
    return api.post<ApiResponse<null>>('/auth/password/change', { currentPassword, newPassword })
  },
}
