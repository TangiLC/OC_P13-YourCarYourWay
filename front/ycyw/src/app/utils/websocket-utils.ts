import { ChatMessageDTO } from '../dto';

export function formatMessage(msg: ChatMessageDTO): string {
  switch (msg.type) {
    case 'JOIN':
      return `[CONNEXION] ${msg.sender}`;
    case 'LEAVE':
      return `[DÉCONNEXION] ${msg.sender}`;
    case 'CLOSE':
      return `[CLOSE] ${msg.content}`;
    case 'INFO':
      return `[INFO] ${msg.content}`;
    default:
      return `${msg.sender}: ${msg.content}`;
  }
}

export function getSenderName(
  senderId: string | undefined,
  participants: Array<{ id: number; firstName: string }>
): string {
  if (!senderId || !participants) return 'Inconnu';
  const id = parseInt(senderId, 10);
  const p = participants.find((x) => x.id === id);
  return p ? p.firstName : 'Utilisateur ' + senderId;
}

export function getMessageCssClass(
  msg: ChatMessageDTO,
  currentUserId: string | undefined
): string {
  if (msg.type !== 'CHAT') return 'system-message';
  return msg.sender === currentUserId ? 'my-message' : 'other-message';
}

export function sortMessages(msgs: ChatMessageDTO[]): ChatMessageDTO[] {
  return msgs.slice().sort((a, b) => {
    const tA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
    const tB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
    return tA - tB;
  });
}
