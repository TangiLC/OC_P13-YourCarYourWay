import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DialogDTO } from '../dto';

@Injectable({ providedIn: 'root' })
export class DialogService {
  private dialogRefresh$ = new Subject<void>();

  constructor(private http: HttpClient) {}

  getDialogsBySender(senderId: number): Observable<DialogDTO[]> {
    return this.http.get<DialogDTO[]>(`/api/dialog/sender/${senderId}`);
  }

  getDialogById(id: number): Observable<DialogDTO> {
    return this.http.get<DialogDTO>(`/api/dialog/${id}`);
  }

  triggerDialogRefresh() {
    this.dialogRefresh$.next();
  }

  onDialogRefresh(): Observable<void> {
    return this.dialogRefresh$.asObservable();
  }
}
