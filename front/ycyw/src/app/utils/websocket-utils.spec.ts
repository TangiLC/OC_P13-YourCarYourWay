import { formatMessage, getMessageCssClass } from './websocket-utils';
import { ChatMessageDTO } from '../dto';

describe('websocket-utils', () => {

  describe('formatMessage', () => {
    interface Case {
      msg: Partial<ChatMessageDTO>;
      expected: string;
      desc: string;
    }
    const cases: Case[] = [
      {
        desc: 'JOIN → préfixe [CONNEXION]',
        msg: { type: 'JOIN', sender: 'Alice', content: 'ignored' },
        expected: '[CONNEXION] Alice',
      },
      {
        desc: 'LEAVE → préfixe [DÉCONNEXION]',
        msg: { type: 'LEAVE', sender: 'Bob', content: 'ignored' },
        expected: '[DÉCONNEXION] Bob',
      },
      {
        desc: 'CLOSE → utilise l’objet entier dans l’interpolation',
        msg: { type: 'CLOSE', sender: 'X', content: 'ignored' },
        expected: `[CLOSE] ${String({ type: 'CLOSE', sender: 'X', content: 'ignored' })}`,
      },
      {
        desc: 'INFO → utilise l’objet entier dans l’interpolation',
        msg: { type: 'INFO', sender: 'Y', content: 'ignored' },
        expected: `[INFO] ${String({ type: 'INFO', sender: 'Y', content: 'ignored' })}`,
      },
      {
        desc: 'CHAT → format “sender: content”',
        msg: { type: 'CHAT', sender: 'Eve', content: 'Hello!' },
        expected: 'Eve: Hello!',
      },{
        desc: 'type undefined → comportement default',
        msg: { sender: 'X', content: 'ciao' },
        expected: 'X: ciao',
      },
      {
        desc: 'sender undefined → "[TYPE] undefined"',
        msg: { type: 'JOIN', content: 'ignored' },
        expected: '[CONNEXION] undefined',
      },
      {
        desc: 'content undefined en CHAT → "sender: undefined"',
        msg: { type: 'CHAT', sender: 'Y' },
        expected: 'Y: undefined',
      },
      {
        desc: 'msg null → lève une erreur ou "undefined: undefined"',
        msg: {},
        expected: 'undefined: undefined',
      },
      {
        desc: 'sender objet sans toString → JSON.stringify fallback',
        msg: { type: 'CHAT', sender: '', content: 'hey' },
        expected: ': hey',
      },
    ];

    cases.forEach(({ msg, expected, desc }) => {
      it(desc, () => {
        expect(formatMessage(msg as ChatMessageDTO)).toBe(expected);
      });
    });
  });

  describe('getMessageCssClass', () => {
    interface Case {
      msg: Partial<ChatMessageDTO>;
      currentUserId: any;
      expected: string;
      desc: string;
    }
    const cases: Case[] = [
      {
        desc: 'type ≠ CHAT → system-message',
        msg: { type: 'JOIN', sender: 'Alice', content: '...' },
        currentUserId: 'Alice',
        expected: 'system-message',
      },
      {
        desc: 'CHAT & senderId === currentUserId (string) → my-message',
        msg: { type: 'CHAT', sender: 'Alice', content: 'hey' },
        currentUserId: 'Alice',
        expected: 'my-message',
      },
      {
        desc: 'CHAT & senderId ≠ currentUserId → other-message',
        msg: { type: 'CHAT', sender: 'Alice', content: 'hey' },
        currentUserId: 'Bob',
        expected: 'other-message',
      },
      {
        desc: 'CHAT & sender undefined → other-message',
        msg: { type: 'CHAT', sender: undefined, content: '' },
        currentUserId: 'Alice',
        expected: 'other-message',
      },
      {
        desc: 'CHAT & currentUserId null → other-message',
        msg: { type: 'CHAT', sender: 'Alice', content: '' },
        currentUserId: null,
        expected: 'other-message',
      },
      {
        desc: 'msg null → system-message',
        msg: {},
        currentUserId: 'X',
        expected: 'system-message',
      },

      {
        desc: 'currentUserId undefined → other-message',
        msg: { type: 'CHAT', sender: 'A', content: '' },
        currentUserId: undefined,
        expected: 'other-message',
      },
    ];

    cases.forEach(({ msg, currentUserId, expected, desc }) => {
      it(desc, () => {
        expect(getMessageCssClass(msg as ChatMessageDTO, currentUserId)).toBe(expected);
      });
    });
  });

});
