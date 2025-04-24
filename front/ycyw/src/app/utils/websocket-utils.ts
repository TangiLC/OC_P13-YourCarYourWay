import { ChatMessageDTO, DialogDTO } from '../dto';

export function formatMessage(msg: ChatMessageDTO): string {
  switch (msg.type) {
    case 'JOIN':
      return `[CONNEXION] ${msg.sender}`;
    case 'LEAVE':
      return `[DÉCONNEXION] ${msg.sender}`;
    case 'CLOSE':
      return `[CLOSE] ${msg}`;
    case 'INFO':
      return `[INFO] ${msg}`;
    default:
      return `${msg.sender}: ${msg.content}`;
  }
}

export function getMessageCssClass(
  msg: ChatMessageDTO,
  currentUserId: any
): string {
  if (msg.type !== 'CHAT') {
    return 'system-message';
  }
  const senderId = msg.sender?.toString?.();
  const currentId = currentUserId?.toString();

  if (senderId && currentId && senderId === currentId) {
    return 'my-message';
  }
  return 'other-message';
}
