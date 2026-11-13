export interface AuthStatus {
  authenticated: boolean;
  username: string | null;
}

export interface LoginPayload {
  username: string;
  password: string;
}
