import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import { WebsocketDialogboxComponent } from './websocket-dialogbox.component';
import { DialogService } from '../../services/dialog.service';
import { WebsocketService } from '../../services/websocket.service';
import { UserService } from '../../services/user.service';
import { of, Subject, BehaviorSubject, Subscription } from 'rxjs';
import { DialogDTO, ChatMessageDTO, UserProfileDTO } from '../../dto';
import { SimpleChange } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { MatListModule } from '@angular/material/list';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

describe('WebsocketDialogboxComponent', () => {
  let component: WebsocketDialogboxComponent;
  let fixture: ComponentFixture<WebsocketDialogboxComponent>;

  // Service mocks
  let dialogServiceMock: jasmine.SpyObj<DialogService>;
  let websocketServiceMock: jasmine.SpyObj<WebsocketService>;
  let userServiceMock: jasmine.SpyObj<UserService>;

  // Subject for mocking observables
  let connectionStatusSubject: BehaviorSubject<boolean>;
  let userSubject: BehaviorSubject<UserProfileDTO | null>;
  let dialogMessages: Subject<ChatMessageDTO>;

  // Test data
  const mockUser: UserProfileDTO = {
    id: 1,
    firstName: 'Support',
    lastName: 'Agent',
    company: '',
    type: 'SUPPORT',
  };

  const mockDialog: DialogDTO = {
    id: 1,
    topic: 'Assistance.@20230515_15:15:00',
    createdAt: '2023-05-15T10:00:00',
    closedAt: '',
    status: 'PENDING',
    lastActivityAt: '2023-05-15T10:00:00',
    participants: [
      mockUser,
      {
        id: 2,
        firstName: 'Support',
        lastName: 'Agent',
        company: '',
        type: 'INDIVIDUAL',
      },
    ],
    messages: [
      {
        id: 1,
        dialogId: 1,
        content: 'Bonjour',
        sender: '1',
        timestamp: '2023-05-15T10:00:00',
        isRead: true,
        type: 'CHAT',
      },
      {
        id: 2,
        dialogId: 1,
        content: 'Comment puis-je vous aider ?',
        sender: '2',
        timestamp: '2023-05-15T10:01:00',
        isRead: true,
        type: 'CHAT',
      },
    ],
  };

  beforeEach(async () => {
    // Create spy objects for services
    dialogServiceMock = jasmine.createSpyObj('DialogService', [
      'getDialogById',
      'triggerDialogRefresh',
    ]);
    websocketServiceMock = jasmine.createSpyObj('WebsocketService', [
      'initWebsocket',
      'disconnect',
      'subscribeToDialog',
      'subscribeToDialogCreated',
      'subscribeToUpdateChannel',
      'sendMessage',
      'sendDisconnectMessage',
    ]);
    userServiceMock = jasmine.createSpyObj('UserService', [], {
      user$: of(mockUser),
    });

    // Set up subjects for mocking observable behavior
    connectionStatusSubject = new BehaviorSubject<boolean>(true);
    userSubject = new BehaviorSubject<UserProfileDTO | null>(mockUser);
    dialogMessages = new Subject<ChatMessageDTO>();

    // Configure spy returns
    dialogServiceMock.getDialogById.and.returnValue(of(mockDialog));
    websocketServiceMock.connectionStatus$ =
      connectionStatusSubject.asObservable();
    userServiceMock.user$ = userSubject.asObservable();
    websocketServiceMock.subscribeToDialog.and.callFake(
      (dialogId, callback) => {
        return dialogMessages.subscribe(callback);
      }
    );
    websocketServiceMock.subscribeToUpdateChannel.and.callFake((callback) => {
      callback();
      return new Subscription();
    });
    websocketServiceMock.subscribeToDialogCreated.and.callFake((callback) => {
      return new Subscription();
    });

    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        FormsModule,
        MatListModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatProgressSpinnerModule,
        WebsocketDialogboxComponent,
      ],
      providers: [
        { provide: DialogService, useValue: dialogServiceMock },
        { provide: WebsocketService, useValue: websocketServiceMock },
        { provide: UserService, useValue: userServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WebsocketDialogboxComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize WebSocket connection on init', () => {
      fixture.detectChanges();
      expect(websocketServiceMock.initWebsocket).toHaveBeenCalled();
    });

    it('should subscribe to user profile on init', () => {
      fixture.detectChanges();
      expect(component.currentUser).toEqual(mockUser);
    });

    it('should subscribe to connection status on init', () => {
      connectionStatusSubject.next(true);
      fixture.detectChanges();
      expect(component.isConnected).toBeTrue();
      expect(component.connectionError).toBeNull();
    });
  });

  describe('Dialog handling', () => {
    it('should load dialog info when dialogId changes', () => {
      component.dialogId = null;
      fixture.detectChanges();

      dialogServiceMock.getDialogById.and.returnValue(of(mockDialog));
      component.dialogId = 1;
      component.ngOnChanges({
        dialogId: new SimpleChange(null, 1, true),
      });

      expect(dialogServiceMock.getDialogById).toHaveBeenCalledWith(1);
      expect(component.currentDialog).toEqual(mockDialog);
      expect(component.messages.length).toBe(3);
    });

    it('should reset when dialogId becomes null', () => {
      component.dialogId = 1;
      component.currentDialog = mockDialog;
      component.messages = [...(mockDialog.messages as ChatMessageDTO[])];
      fixture.detectChanges();

      component.dialogId = null;
      component.ngOnChanges({
        dialogId: new SimpleChange(1, null, false),
      });

      expect(component.currentDialog).toBeNull();
    });

    it('should sort messages by timestamp', fakeAsync(() => {
      component.dialogId = 1;
      fixture.detectChanges();

      const newMessage: ChatMessageDTO = {
        id: 3,
        dialogId: 1,
        content: 'Message intercalé',
        sender: '1',
        timestamp: '2023-05-15T10:00:30',
        isRead: false,
        type: 'CHAT',
      };

      dialogMessages.next(newMessage);
      tick();

      expect(component.messages[1].content).toBe('Message intercalé');
    }));
  });

  describe('Message sending', () => {
    it('should not send empty messages', () => {
      component.dialogId = 1;
      component.newMessage = '   ';
      fixture.detectChanges();

      component.sendMessage();

      expect(websocketServiceMock.sendMessage).not.toHaveBeenCalled();
      expect(component.newMessage).toBe('   ');
    });

    it('should send non-empty messages', () => {
      component.dialogId = 1;
      component.newMessage = 'Hello';
      component.currentUser = mockUser;
      fixture.detectChanges();

      component.sendMessage();

      expect(websocketServiceMock.sendMessage).toHaveBeenCalledWith({
        dialogId: 1,
        content: 'Hello',
        sender: '1',
        isRead: false,
        type: 'CHAT',
      });
      expect(component.newMessage).toBe('');
    });
  });

  describe('Disconnect handling', () => {
    it('should send disconnect message when disconnecting from dialog', () => {
      component.dialogId = 1;
      component.isConnected = true;
      component.currentUser = mockUser;
      fixture.detectChanges();

      component.disconnectFromDialog();

      expect(websocketServiceMock.sendDisconnectMessage).toHaveBeenCalledWith({
        dialogId: 1,
        content: "L'utilisateur s'est déconnecté",
        sender: '1',
        type: 'LEAVE',
      });
      expect(component.dialogId).toBeNull();
      expect(component.currentDialog).toBeNull();
      expect(component.messages.length).toBe(0);
      expect(dialogServiceMock.triggerDialogRefresh).toHaveBeenCalled();
    });

    it('should clean up subscriptions on destroy', () => {
      const unsubscribeSpy = spyOn(Subscription.prototype, 'unsubscribe');

      fixture.detectChanges();
      fixture.destroy();

      expect(websocketServiceMock.disconnect).toHaveBeenCalled();
      expect(unsubscribeSpy).toHaveBeenCalled();
    });
  });

  describe('UI helper methods', () => {
    beforeEach(() => {
      component.currentDialog = mockDialog;
      component.currentUser = mockUser;
    });

    it('should extract dialog title', () => {
      expect(component.dialogTitle).toBe('Assistance');
    });

    it('should extract dialog date', () => {
      expect(component.dialogDate).toBe('15/05/2023 à 15h15');
    });

    it('should format message timestamp', () => {
      const timestamp = '2023-05-15T14:30:00';
      expect(component.formatMessageTimestamp(timestamp)).toBeDefined();
    });

    const senderTestCases = [
      { senderId: '1', expected: 'Support' },
      { senderId: '88', expected: 'Utilisateur 88' },
      { senderId: undefined, expected: 'Inconnu' },
    ];

    for (const testCase of senderTestCases) {
      it(`should return sender name "${testCase.expected}" for ID ${testCase.senderId}`, () => {
        expect(component.getSenderName(testCase.senderId)).toBe(
          testCase.expected
        );
      });
    }

    const messageClassTestCases = [
      {
        message: { sender: '1', type: 'CHAT' } as ChatMessageDTO,
        expected: 'my-message',
      },
      {
        message: { sender: '2', type: 'CHAT' } as ChatMessageDTO,
        expected: 'other-message',
      },
      {
        message: { type: 'INFO' } as ChatMessageDTO,
        expected: 'system-message',
      },
    ];

    for (const testCase of messageClassTestCases) {
      it(`should return correct CSS class for message type`, () => {
        expect(component.getMessageCssClass(testCase.message)).toContain(
          testCase.expected
        );
      });
    }
  });

  const connectionStates = [true, false];

  for (const isConnected of connectionStates) {
    describe(`when connection state is ${isConnected}`, () => {
      beforeEach(() => {
        connectionStatusSubject.next(isConnected);
        fixture.detectChanges();
      });

      it(`should reflect connection status as ${isConnected}`, () => {
        expect(component.isConnected).toBe(isConnected);
      });

      it(`should ${
        isConnected ? '' : 'not'
      } load dialog when ID changes`, () => {
        dialogServiceMock.getDialogById.calls.reset();
        component.dialogId = 1;

        component.ngOnChanges({
          dialogId: new SimpleChange(null, 1, true),
        });

        if (isConnected) {
          expect(dialogServiceMock.getDialogById).toHaveBeenCalled();
        } else {
          expect(dialogServiceMock.getDialogById).not.toHaveBeenCalled();
        }
      });
    });
  }

  // Tests paramétrés pour la gestion des différents types de messages
  const messageTypes = ['CHAT', 'SYSTEM', 'JOIN', 'LEAVE'];

  for (const type of messageTypes) {
    it(`should handle ${type} message correctly`, fakeAsync(() => {
      component.dialogId = 1;
      fixture.detectChanges();

      const testMessage: ChatMessageDTO = {
        id: 100,
        dialogId: 1,
        content: `Test ${type} message`,
        sender: '1',
        timestamp: '2023-05-15T12:00:00',
        isRead: false,
        type: type as any,
      };

      dialogMessages.next(testMessage);
      tick();

      expect(component.messages).toContain(
        jasmine.objectContaining({
          content: `Test ${type} message`,
          type: type,
        })
      );
    }));
  }

  // Tests paramétrés pour les différents cas d'erreur
  describe('Error handling', () => {
    const errorScenarios = [
      {
        desc: 'dialog loading fails',
        setup: () => {
          dialogServiceMock.getDialogById.and.returnValue(
            new Subject<DialogDTO>().asObservable()
          );
          spyOn(console, 'error');
        },
      },
      {
        desc: 'connection drops',
        setup: () => {
          connectionStatusSubject.next(false);
        },
      },
    ];

    for (const scenario of errorScenarios) {
      it(`should handle when ${scenario.desc}`, fakeAsync(() => {
        scenario.setup();
        component.dialogId = 1;
        fixture.detectChanges();

        tick();

        if (scenario.desc === 'dialog loading fails') {
          expect(component.isDialogLoading).toBeTrue();
        } else if (scenario.desc === 'connection drops') {
          expect(component.isConnected).toBeFalse();
        }
      }));
    }
  });
});
