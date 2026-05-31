import {inject, Injectable} from '@angular/core';
import {DistrictResponse, Page, RegionResponse, UserResponse} from '../../core/models/models';
import {HttpClient, HttpParams} from '@angular/common/http';
import {environment} from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/admin`;

  // ── Users ─────────────────────────────────────────────────

  getUsers(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<UserResponse>>(`${this.api}/users`, { params });
  }

  getUserById(id: number) {
    return this.http.get<UserResponse>(`${this.api}/users/${id}`);
  }

  activateUser(id: number, role: string, regionId?: number) {
    return this.http.post<UserResponse>(`${this.api}/users/${id}/activate`, { role, regionId });
  }

  deactivateUser(id: number) {
    return this.http.post<UserResponse>(`${this.api}/users/${id}/deactivate`, {});
  }

  updateUserRole(id: number, role: string, regionId?: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/role`, { role, regionId });
  }

  adminResetPassword(id: number, newPassword: string) {
    return this.http.post<{ message: string }>(`${this.api}/users/${id}/reset-password`, { newPassword });
  }

  assignDistricts(id: number, districtIds: number[]) {
    return this.http.patch<UserResponse>(`${this.api}/users/${id}/districts`, { districtIds });
  }

  assignRegion(userId: number, regionId: number) {
    return this.http.patch<UserResponse>(`${this.api}/users/${userId}/region/${regionId}`, {});
  }

  deleteUser(id: number) {
    return this.http.delete<{ message: string }>(`${this.api}/users/${id}`);
  }

  // ── Regions ───────────────────────────────────────────────

  getRegions() {
    return this.http.get<RegionResponse[]>(`${environment.apiUrl}/regions`);
  }

  createRegion(name: string) {
    return this.http.post<RegionResponse>(`${this.api}/regions`, { name });
  }

  updateRegion(id: number, name: string) {
    return this.http.put<RegionResponse>(`${this.api}/regions/${id}`, { name });
  }

  deleteRegion(id: number) {
    return this.http.delete<{ message: string }>(`${this.api}/regions/${id}`);
  }

  assignDistrictsToRegion(regionId: number, districtIds: number[]) {
    return this.http.post<RegionResponse>(`${this.api}/regions/${regionId}/districts`, { districtIds });
  }

  // ── Districts ─────────────────────────────────────────────

  getDistricts() {
    return this.http.get<DistrictResponse[]>(`${environment.apiUrl}/districts`);
  }

  getUnassignedDistricts() {
    return this.http.get<DistrictResponse[]>(`${this.api}/districts/unassigned`);
  }

}
