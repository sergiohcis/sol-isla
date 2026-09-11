export type UserRole = 'ADMIN' | 'STAFF';

export interface CurrentUser {
  id: string;
  email: string;
  role: UserRole;
}
