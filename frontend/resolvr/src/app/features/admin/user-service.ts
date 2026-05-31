import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {environment} from '../../../../environments/environment';
import {Page, UserResponse} from '../../core/models/models';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users`;

  getAssigners(districtId: number) {
    const params = new HttpParams().set('districtId', districtId);
    return this.http.get<UserResponse[]>(`${this.api}/assigners`, {params});
  }
}
