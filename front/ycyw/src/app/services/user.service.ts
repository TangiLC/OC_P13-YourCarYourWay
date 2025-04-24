import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { UserProfileDTO } from '../dto';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private userSubject = new BehaviorSubject<UserProfileDTO | null>(null);
  public user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) {}

  fetchAndStoreCurrentUser(): Observable<UserProfileDTO> {
    return this.http
      .get<UserProfileDTO>('/api/profile/me')
      .pipe(tap((user) => this.userSubject.next(user)));
  }

  getCurrentUser(): UserProfileDTO | null {
    return this.userSubject.value;
  }
}
