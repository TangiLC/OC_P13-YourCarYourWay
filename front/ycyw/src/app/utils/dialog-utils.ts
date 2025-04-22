import { ChatMessageDTO, DialogDTO } from "../dto";

export function extractDialogTitle(topic: string | undefined | null): string {
  return topic?.split('.@')[0] ?? 'No Title';
}

export function extractDialogDate(topic: string | undefined | null): string {
  const raw = topic?.split('.@')[1] ?? '';
  if (!raw) return '';

  const [dateStr, timeStr] = raw.split('_');
  const year = dateStr.slice(0, 4);
  const month = dateStr.slice(4, 6);
  const day = dateStr.slice(6, 8);
  const hour = timeStr?.slice(0, 2);
  const minute = timeStr?.slice(3, 5);

  return `${day}/${month}/${year} à ${hour}h${minute}`;
}

export function groupByStatus(dialogs: DialogDTO[]): Map<string, DialogDTO[]> {
  return dialogs.reduce((map, dlg) => {
    const list = map.get(dlg.status) ?? [];
    list.push(dlg);
    map.set(dlg.status, list);
    return map;
  }, new Map<string, DialogDTO[]>());
}

/**
 * Compte les messages non lus (isRead=false && sender != currentUser).
 */
export function countUnread(dialog: DialogDTO, currentUserId: number): number {
  return (
    dialog.messages
      ?.filter((msg: ChatMessageDTO) => !msg.isRead && msg.sender !== currentUserId.toString())
      .length || 0
  );
}
