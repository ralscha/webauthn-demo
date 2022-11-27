import {Injectable} from '@angular/core';
import {Observable, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {catchError, map, tap} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private loggedIn = false;

  constructor(private readonly httpClient: HttpClient) {
  }

  isAuthenticated(): Observable<boolean> {
    return this.httpClient.get<void>('authenticate', {
      withCredentials: true
    }).pipe(
      tap(() => this.loggedIn = true),
      map(() => true),
      catchError(() => {
        this.loggedIn = false;
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    return this.httpClient.get<void>('logout', {
      withCredentials: true
    }).pipe(tap(() => this.loggedIn = false));
  }

  isLoggedIn(): boolean {
    return this.loggedIn;
  }

}



