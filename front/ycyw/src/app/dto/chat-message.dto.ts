export type MessageType = 'CHAT' | 'JOIN' | 'LEAVE' | 'INFO' | 'CLOSE';

export interface ChatMessageDTO {
  id: number;
  content: string;
  timestamp: string;
  dialogId: number;
  sender: string;
  type: MessageType;
}
