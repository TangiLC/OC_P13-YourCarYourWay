import { ChatMessageDTO } from './chat-message.dto';
import { UserProfileDTO } from './user-profile.dto';

export type DialogStatus = 'OPEN' | 'PENDING' | 'CLOSED';

export interface DialogDTO {
  id: number;
  topic: string;
  status: DialogStatus;
  createdAt: string;
  closedAt: string;
  lastActivityAt: string;
  participants: UserProfileDTO[];
  messages: ChatMessageDTO[];
}
