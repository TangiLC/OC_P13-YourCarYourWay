import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogService } from './dialog.service';
import { DialogDTO } from '../dto';
import { take } from 'rxjs/operators';

describe('DialogService', () => {
  let service: DialogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DialogService],
    });
    service = TestBed.inject(DialogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDialogsBySender', () => {
    interface Case {
      senderId: number;
      desc: string;
    }
    const cases: Case[] = [
      { senderId: 1, desc: 'ids normaux' },
      { senderId: 0, desc: 'senderId = 0 (edge)' },
      { senderId: -5, desc: 'senderId négatif (edge)' },
      { senderId: 999999, desc: 'senderId élevé' },
    ];

    cases.forEach(({ senderId, desc }) => {
      it(`GET /api/dialog/sender/${senderId} — ${desc}`, () => {
        const mock: DialogDTO[] = [
          { id: 1, name: 'A' } as any,
          { id: 2, name: 'B' } as any,
        ];
        service.getDialogsBySender(senderId).subscribe((res) => {
          expect(res).toEqual(mock);
        });
        const req = httpMock.expectOne(`/api/dialog/sender/${senderId}`);
        expect(req.request.method).toBe('GET');
        req.flush(mock);
      });
    });
  });

  describe('getDialogById', () => {
    interface Case {
      id: number;
      desc: string;
    }
    const cases: Case[] = [
      { id: 1, desc: 'id normal' },
      { id: 0, desc: 'id = 0 (edge)' },
      { id: -10, desc: 'id négatif (edge)' },
      { id: 123456, desc: 'id élevé' },
    ];

    cases.forEach(({ id, desc }) => {
      it(`GET /api/dialog/${id} — ${desc}`, () => {
        const mock: DialogDTO = { id, name: 'Test' } as any;
        service.getDialogById(id).subscribe((res) => {
          expect(res).toEqual(mock);
        });
        const req = httpMock.expectOne(`/api/dialog/${id}`);
        expect(req.request.method).toBe('GET');
        req.flush(mock);
      });
    });
  });

  describe('getDialogsByStatus', () => {
    interface Case {
      status: string;
      desc: string;
    }
    const cases: Case[] = [
      { status: 'OPEN', desc: 'status normal' },
      { status: '', desc: 'status vide' },
      { status: 'CLOSED', desc: 'status fermé' },
      { status: 'XYZ123', desc: 'status avec caractères spéciaux' },
    ];

    cases.forEach(({ status, desc }) => {
      it(`GET /api/dialog/status/${status} — ${desc}`, () => {
        const mock: DialogDTO[] = [
          { id: 1, name: 'S1', status } as any,
          { id: 2, name: 'S2', status } as any,
        ];
        service.getDialogsByStatus(status).subscribe((res) => {
          expect(res).toEqual(mock);
        });
        const req = httpMock.expectOne(`/api/dialog/status/${status}`);
        expect(req.request.method).toBe('GET');
        req.flush(mock);
      });
    });
  });

  describe('markDialogMessagesAsRead', () => {
    interface Case {
      dialogId: number;
      senderId: number;
      desc: string;
    }
    const cases: Case[] = [
      { dialogId: 1, senderId: 2, desc: 'ids normaux' },
      { dialogId: 0, senderId: 0, desc: 'zeros (edge)' },
      { dialogId: -1, senderId: -2, desc: 'négatifs (edge)' },
    ];

    cases.forEach(({ dialogId, senderId, desc }) => {
      it(`POST /api/dialog/${dialogId}/${senderId}/markasread — ${desc}`, () => {
        service
          .markDialogMessagesAsRead(dialogId, senderId)
          .subscribe((res) => {
            expect(res).toBeNull();
          });
        const req = httpMock.expectOne(
          `/api/dialog/${dialogId}/${senderId}/markasread`
        );
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({});
        req.flush(null);
      });
    });
  });

  describe('onDialogRefresh & triggerDialogRefresh', () => {
    it('devrait émettre quand on appelle triggerDialogRefresh()', (done) => {
      service
        .onDialogRefresh()
        .pipe(take(1))
        .subscribe((val) => {
          expect(val).toBeUndefined();
          done();
        });
      service.triggerDialogRefresh();
    });
  });
});
