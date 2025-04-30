import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import { DialogHistoryComponent } from './dialog-history.component';
import { DialogService } from '../../services/dialog.service';
import { WebsocketService } from '../../services/websocket.service';
import { UserService } from '../../services/user.service';
import { DialogDTO, ChatMessageDTO, UserProfileDTO } from '../../dto';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';

describe('DialogHistoryComponent', () => {
  let component: DialogHistoryComponent;
  let fixture: ComponentFixture<DialogHistoryComponent>;
  let dialogService: jasmine.SpyObj<DialogService>;
  let websocketService: jasmine.SpyObj<WebsocketService>;
  let userService: jasmine.SpyObj<UserService>;
  let loader: HarnessLoader;

  // Mock des observables
  const mockUserSubject = new BehaviorSubject<UserProfileDTO | null>(null);
  const mockDialogRefreshSubject = new BehaviorSubject<void>(undefined);
  const mockConnectionSubject = new BehaviorSubject<boolean>(false);
  let mockWebsocketSubscription: any;

  beforeEach(async () => {
    // Créer les mocks pour les services
    dialogService = jasmine.createSpyObj('DialogService', [
      'getDialogsBySender',
      'getDialogsByStatus',
      'getDialogById',
      'markDialogMessagesAsRead',
      'onDialogRefresh',
    ]);
    websocketService = jasmine.createSpyObj('WebsocketService', [
      'subscribeToDialog',
    ]);
    userService = jasmine.createSpyObj('UserService', [], {
      user$: mockUserSubject.asObservable(),
    });

    // Configurer les retours des méthodes mockées
    dialogService.onDialogRefresh.and.returnValue(
      mockDialogRefreshSubject.asObservable()
    );
    websocketService.connectionStatus$ = mockConnectionSubject.asObservable();
    mockWebsocketSubscription = {
      unsubscribe: jasmine.createSpy('unsubscribe'),
    };
    websocketService.subscribeToDialog.and.returnValue(
      mockWebsocketSubscription
    );

    await TestBed.configureTestingModule({
      imports: [DialogHistoryComponent, NoopAnimationsModule, FormsModule],
      providers: [
        { provide: DialogService, useValue: dialogService },
        { provide: WebsocketService, useValue: websocketService },
        { provide: UserService, useValue: userService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DialogHistoryComponent);
    component = fixture.componentInstance;
    component.senderId = 1;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  describe('initialisation', () => {
    it('devrait afficher un avertissement quand senderId est manquant', () => {
      spyOn(console, 'warn');
      component.senderId = undefined as any;
      component.ngOnInit();
      expect(console.warn).toHaveBeenCalledWith(
        'senderId manquant dans DialogHistoryComponent'
      );
    });

    it("devrait souscrire aux services lors de l'initialisation", () => {
      const mockUser = {
        id: 1,
        type: 'INDIVIDUAL',
        firstName: 'Mock',
        lastName: 'Test',
        company: '',
      } as UserProfileDTO;
      mockUserSubject.next(mockUser);

      dialogService.getDialogsBySender.and.returnValue(of([]));

      component.ngOnInit();

      expect(dialogService.onDialogRefresh).toHaveBeenCalled();
      expect(websocketService.subscribeToDialog).toHaveBeenCalledWith(
        1,
        jasmine.any(Function)
      );
    });
  });

  /*describe('loadDialogs', () => {
    beforeEach(() => {
      component.senderId = 1;
      component.ngOnInit();
      dialogService.getDialogsBySender.calls.reset();
      dialogService.getDialogsByStatus.calls.reset();
    });

    interface Case {
      userType: string;
      expectedCalls: {
        mine: boolean;
        pending: boolean;
      };
      desc: string;
    }

    const cases: Case[] = [
      {
        userType: 'USER',
        expectedCalls: { mine: true, pending: false },
        desc: 'utilisateur standard - devrait charger uniquement ses propres dialogues',
      },
      {
        userType: 'SUPPORT',
        expectedCalls: { mine: true, pending: true },
        desc: 'utilisateur support - devrait charger ses dialogues + ceux en attente',
      },
    ];

    cases.forEach(({ userType, expectedCalls, desc }) => {
      it(desc, () => {
        const mockUser = { id: 1, type: userType } as UserProfileDTO;
        mockUserSubject.next(mockUser);

        dialogService.getDialogsBySender.and.returnValue(of([]));
        dialogService.getDialogsByStatus.and.returnValue(of([]));

        component.loadDialogs();

        expect(dialogService.getDialogsBySender).toHaveBeenCalledWith(1);

        if (expectedCalls.pending) {
          expect(dialogService.getDialogsByStatus).toHaveBeenCalledWith(
            'PENDING'
          );
        } else if (userType === 'SUPPORT') {
          expect(dialogService.getDialogsByStatus).not.toHaveBeenCalled();
        }
      });
    });

    it('ne devrait pas charger les dialogues quand currentUser est null', () => {
      mockUserSubject.next(null);
      component.loadDialogs();
      expect(dialogService.getDialogsBySender).not.toHaveBeenCalled();
    });

    it('devrait fusionner correctement les dialogues pour les utilisateurs de support', () => {
      const mockUser = { id: 1, type: 'SUPPORT' } as UserProfileDTO;
      mockUserSubject.next(mockUser);

      const myDialogs: DialogDTO[] = [
        { id: 1, status: 'OPEN' } as DialogDTO,
        { id: 2, status: 'CLOSED' } as DialogDTO,
      ];

      const pendingDialogs: DialogDTO[] = [
        { id: 2, status: 'CLOSED' } as DialogDTO,
        { id: 3, status: 'PENDING' } as DialogDTO,
      ];

      dialogService.getDialogsBySender.and.returnValue(of(myDialogs));
      dialogService.getDialogsByStatus.and.returnValue(of(pendingDialogs));

      component.loadDialogs();
      fixture.detectChanges();

      component.dialogs$.subscribe((dialogs) => {
        expect(dialogs.length).toBe(3);
        expect(dialogs.find((d) => d.id === 1)).toBeTruthy();
        expect(dialogs.find((d) => d.id === 2)).toBeTruthy();
        expect(dialogs.find((d) => d.id === 3)).toBeTruthy();
      });
    });
  });*/

  describe('createNewDialog', () => {
    interface Case {
      inputText: string;
      expectEmit: boolean;
      expectError: boolean;
      desc: string;
    }

    const cases: Case[] = [
      {
        inputText: 'Nouveau dialogue',
        expectEmit: true,
        expectError: false,
        desc: 'texte valide - devrait créer un dialogue',
      },
      {
        inputText: '   ',
        expectEmit: false,
        expectError: true,
        desc: 'texte avec espaces uniquement - ne devrait pas créer de dialogue',
      },
      {
        inputText: '',
        expectEmit: false,
        expectError: true,
        desc: 'texte vide - ne devrait pas créer de dialogue',
      },
      {
        inputText: 'a'.repeat(500),
        expectEmit: true,
        expectError: false,
        desc: 'texte très long - devrait créer un dialogue',
      },
    ];

    cases.forEach(({ inputText, expectEmit, expectError, desc }) => {
      it(
        desc,
        fakeAsync(async () => {
          spyOn(component.dialogCreated, 'emit');

          component.topicInput = inputText;
          component.createNewDialog();
          tick();

          if (expectEmit) {
            expect(component.dialogCreated.emit).toHaveBeenCalledWith(
              inputText.trim()
            );
            expect(component.topicInput).toBe('');
          } else {
            expect(component.dialogCreated.emit).not.toHaveBeenCalled();
          }

          expect(component.showError).toBe(expectError);
        })
      );
    });
  });

  describe('utilisation des utilitaires de dialogue', () => {
    it('getDialogTitle devrait formater correctement le titre', () => {
      const testCases = [
        { input: 'Problème.@10/04/2024', expected: 'Problème' },
        { input: null, expected: 'No Title' },
        { input: undefined, expected: 'No Title' },
        { input: '', expected: '' },
        { input: '  ', expected: '  ' },
        { input: 'Titre sans date', expected: 'Titre sans date' },
      ];

      testCases.forEach(({ input, expected }) => {
        expect(component.getDialogTitle(input)).toBe(expected);
      });
    });

    it('getDialogDate devrait extraire correctement la date', () => {
      const testCases = [
        {
          input: 'Problème.@20240410_12:12:00',
          expected: '10/04/2024 à 12h12',
        },
        { input: null, expected: '' },
        { input: undefined, expected: '' },
        { input: '', expected: '' },
        { input: 'Titre sans date', expected: '' },
      ];

      testCases.forEach(({ input, expected }) => {
        expect(component.getDialogDate(input)).toBe(expected);
      });
    });
  });

  describe('cycle de vie', () => {
    it('devrait annuler tous les abonnements lors de la destruction', () => {
      const mockUser = {
        id: 1,
        type: 'INDIVIDUAL',
        firstName: 'Mock',
        lastName: 'Test',
        company: '',
      } as UserProfileDTO;
      mockUserSubject.next(mockUser);

      dialogService.getDialogsBySender.and.returnValue(of([]));

      component.ngOnInit();

      // Remplacer les propriétés d'abonnement par des espions
      const spies = [
        'refreshSub',
        'connectionSub',
        'wsDialogSub',
        'intervalSub',
        'userSubscription',
      ];

      spies.forEach((propName) => {
        const sub = { unsubscribe: jasmine.createSpy('unsubscribe') };
        (component as any)[propName] = sub;
      });

      // Détruire le composant
      component.ngOnDestroy();

      // Vérifier que tous les abonnements ont été annulés
      spies.forEach((propName) => {
        expect((component as any)[propName].unsubscribe).toHaveBeenCalled();
      });
    });
  });
});

// Fonction d'aide pour créer des observables qui échouent
function throwError(message: string): Observable<never> {
  return new Observable((observer) => {
    observer.error(new Error(message));
  });
}
