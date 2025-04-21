export type ProfileType = 'INDIVIDUAL' | 'COMPANY' | 'SUPPORT' | 'AGENCY';

export interface UserProfileDTO {
  id: number;
  firstName: string;
  lastName: string;
  company: string;
  type: ProfileType;
}
