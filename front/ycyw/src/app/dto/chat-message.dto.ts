export type MessageType = 'CHAT' | 'JOIN' | 'LEAVE';

export interface ChatMessageDTO {
  id: number;
  content: string;
  timestamp: string;
  dialogId: number;
  sender: string;
  type: MessageType;
}
