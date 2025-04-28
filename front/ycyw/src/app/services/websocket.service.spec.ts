import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, skip, Subject } from 'rxjs';
import { WebsocketService } from './websocket.service';
import { IMessage } from '@stomp/stompjs';
import { ChatMessageDTO } from '../dto';

// Mock RxStomp pour éviter d'avoir besoin de global
class MockRxStomp {
  activate = jasmine.createSpy('activate');
  deactivate = jasmine.createSpy('deactivate');
  publish = jasmine.createSpy('publish');
  connected$ = new BehaviorSubject<boolean>(false);
  stompErrors$ = new Subject<any>();
  webSocketErrors$ = new Subject<any>();
  watch = jasmine.createSpy('watch').and.returnValue({
    subscribe: (callback: any) => {
      return { unsubscribe: () => {} };
    },
  });
  configure = jasmine.createSpy('configure');
}

describe('WebsocketService', () => {
  let service: WebsocketService;
  let mockClient: MockRxStomp;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WebsocketService],
    });

    service = TestBed.inject(WebsocketService);

    // Remplacer le client RxStomp par notre mock
    mockClient = new MockRxStomp();
    (service as any).client = mockClient;
    (service as any).messageQueue = [];
  });

  describe('connectionStatus$', () => {
    it('doit émettre true lorsque connected$ émet true', (done) => {
      service.connectionStatus$.pipe(skip(1)).subscribe((status) => {
        expect(status).toBeTrue();
        done();
      });
      service.updateConnectionStatus(true);
    });

    it('doit émettre false initialement', (done) => {
      service.connectionStatus$.subscribe((status) => {
        expect(status).toBeFalse();
        done();
      });
    });
  });

  describe('initWebsocket', () => {
    it("doit activer le client et traiter les messages en file d'attente", () => {
      // Préparer la file d'attente avec un message
      (service as any).messageQueue = [
        { destination: '/app/test', body: 'message' },
      ];

      // Appeler initWebsocket
      service.initWebsocket();

      // Vérifier que le client a été activé
      expect(mockClient.activate).toHaveBeenCalled();

      // Simuler la connexion
      mockClient.connected$.next(true);

      // Vérifier que les messages en file d'attente ont été traités
      expect((service as any).messageQueue.length).toBe(0);
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/test',
        body: 'message',
      });
      expect((service as any).isConnected).toBeTrue();
    });
  });

  describe('sendMessage & sendDisconnectMessage', () => {
    interface Case {
      method: keyof WebsocketService;
      dest: string;
      payload: any;
      connected: boolean;
      desc: string;
    }
    const cases: Case[] = [
      {
        method: 'sendMessage',
        dest: '/app/chat.sendMessage',
        payload: { foo: 1 },
        connected: true,
        desc: 'connecté → publie immédiatement',
      },
      {
        method: 'sendMessage',
        dest: '/app/chat.sendMessage',
        payload: { foo: 2 },
        connected: false,
        desc: 'non connecté → en file',
      },
      {
        method: 'sendDisconnectMessage',
        dest: '/app/chat.disconnect',
        payload: { d: 3 },
        connected: true,
        desc: 'disconnect connecté → publie immédiatement',
      },
      {
        method: 'sendDisconnectMessage',
        dest: '/app/chat.disconnect',
        payload: { d: 4 },
        connected: false,
        desc: 'disconnect non connecté → en file',
      },
    ];

    cases.forEach(({ method, dest, payload, connected, desc }) => {
      it(desc, () => {
        // Définir l'état de connexion
        (service as any).isConnected = connected;

        // Appeler la méthode
        (service as any)[method](payload);

        const body = JSON.stringify(payload);
        if (connected) {
          expect(mockClient.publish).toHaveBeenCalledWith({
            destination: dest,
            body,
          });
          expect((service as any).messageQueue.length).toBe(0);
        } else {
          expect(mockClient.publish).not.toHaveBeenCalled();
          expect((service as any).messageQueue).toEqual([
            { destination: dest, body },
          ]);
        }
      });
    });
  });

  describe('subscribeToDialog', () => {
    it('doit appeler callback avec ChatMessageDTO valide', () => {
      const fakeMsg: ChatMessageDTO = {
        id: 1,
        sender: 'A',
        content: 'hi',
      } as any;

      mockClient.watch.and.returnValue({
        subscribe: (cb: (msg: IMessage) => void) => {
          cb({ body: JSON.stringify(fakeMsg) } as any);
          return { unsubscribe: () => {} } as any;
        },
      });

      const cb = jasmine.createSpy('cb');
      service.subscribeToDialog(42, cb);

      expect(mockClient.watch).toHaveBeenCalledWith('/topic/dialog/42');
      expect(cb).toHaveBeenCalledWith(fakeMsg);
    });

    it('doit log erreur sur JSON invalide', () => {
      spyOn(console, 'error');
      mockClient.watch.and.returnValue({
        subscribe: (cb: (msg: IMessage) => void) => {
          cb({ body: 'not-json' } as any);
          return { unsubscribe: () => {} } as any;
        },
      });

      const cb = jasmine.createSpy('cb');
      service.subscribeToDialog(1, cb);

      expect(console.error).toHaveBeenCalled();
      expect(cb).not.toHaveBeenCalled();
    });
  });

  describe('subscribeToUpdateChannel', () => {
    interface Case {
      body: string;
      shouldCallback: boolean;
      desc: string;
    }
    const cases: Case[] = [
      { body: 'NEW', shouldCallback: true, desc: 'status string valide' },
      { body: 'PENDING', shouldCallback: true, desc: 'status string valide' },
      { body: 'OPENED', shouldCallback: true, desc: 'status string valide' },
      { body: 'CLOSED', shouldCallback: true, desc: 'status string valide' },
      { body: 'UNKNOWN', shouldCallback: false, desc: 'status non valide' },
      {
        body: JSON.stringify({ event: 'NEW' }),
        shouldCallback: true,
        desc: 'payload JSON with valid event',
      },
      {
        body: JSON.stringify({ event: 'CLOSED' }),
        shouldCallback: true,
        desc: 'payload JSON with valid event',
      },
      {
        body: JSON.stringify({ event: 'XYZ' }),
        shouldCallback: false,
        desc: 'payload JSON with invalid event',
      },
      {
        body: '{badJson',
        shouldCallback: false,
        desc: 'JSON malformé → erreur',
      },
    ];

    cases.forEach(({ body, shouldCallback, desc }) => {
      it(desc, () => {
        spyOn(console, 'error');
        spyOn(console, 'log');
        const cb = jasmine.createSpy('cb');

        mockClient.watch.and.returnValue({
          subscribe: (cbObs: (msg: IMessage) => void) => {
            cbObs({ body } as any);
            return { unsubscribe: () => {} } as any;
          },
        });

        service.subscribeToUpdateChannel(cb);

        expect(mockClient.watch).toHaveBeenCalledWith('/topic/dialogs/update');

        if (shouldCallback) {
          expect(cb).toHaveBeenCalled();
          expect(console.log).toHaveBeenCalledWith('REFRESH LIST');
        } else {
          expect(cb).not.toHaveBeenCalled();
        }

        if (body.startsWith('{')) {
          try {
            JSON.parse(body);
          } catch (e) {
            expect(console.error).toHaveBeenCalled();
          }
        }
      });
    });
  });

  describe('createDialog & joinDialog', () => {
    it('createDialog should send or queue message', () => {
      spyOn(service as any, 'sendOrQueue');
      service.createDialog('test-topic');
      expect((service as any).sendOrQueue).toHaveBeenCalledWith(
        '/app/chat.createDialog',
        'test-topic'
      );
    });

    it('joinDialog should send or queue message', () => {
      spyOn(service as any, 'sendOrQueue');
      service.joinDialog(123);
      expect((service as any).sendOrQueue).toHaveBeenCalledWith(
        '/app/chat.addUser',
        JSON.stringify({
          dialogId: 123,
          content: 'A rejoint le dialogue',
        })
      );
    });
  });

  describe('disconnect', () => {
    it('should deactivate client and update connection status', () => {
      service.disconnect();
      expect(mockClient.deactivate).toHaveBeenCalled();
      expect((service as any).isConnected).toBeFalse();
      service.connectionStatus$.subscribe((status) => {
        expect(status).toBeFalse();
      });
    });
  });

  describe('subscribeToDialogCreated', () => {
    let subscribeCb: (msg: IMessage) => void;
    let fakeSubscription: { unsubscribe: jasmine.Spy };

    beforeEach(() => {
      // Ré-implémenter watch pour capturer le callback
      fakeSubscription = { unsubscribe: jasmine.createSpy('unsubscribe') };
      mockClient.watch.and.callFake((destination: string) => {
        expect(destination).toBe('/user/queue/dialog-created');
        return {
          subscribe: (cb: any) => {
            subscribeCb = cb;
            return fakeSubscription;
          },
        };
      });
    });

    it('doit parser et appeler le callback sur JSON valide', () => {
      const cb = jasmine.createSpy('cb');
      const sub = service.subscribeToDialogCreated(cb);
      const payload = { id: 123, name: 'TestDialog' };
      subscribeCb({ body: JSON.stringify(payload) } as IMessage);
      expect(cb).toHaveBeenCalledWith(payload);
      sub.unsubscribe();
      expect(fakeSubscription.unsubscribe).toHaveBeenCalled();
    });

    it('doit attraper l’erreur de parsing et ne pas appeler le callback', () => {
      spyOn(console, 'error');
      const cb = jasmine.createSpy('cb');
      service.subscribeToDialogCreated(cb);
      subscribeCb({ body: '« bad json »' } as IMessage);
      expect(console.error).toHaveBeenCalledWith(
        'Erreur parsing dialog-created:',
        jasmine.any(Error)
      );
      expect(cb).not.toHaveBeenCalled();
    });
  });
});
