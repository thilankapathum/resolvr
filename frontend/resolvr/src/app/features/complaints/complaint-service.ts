import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {environment} from '../../../../environments/environment';
import {ComplaintResponse, Page} from '../../core/models/models';

@Injectable({
  providedIn: 'root',
})
export class ComplaintService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/complaints`;

  getComplaints(page = 0, size = 20, status = '', search = '') {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');

    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);

    return this.http.get<Page<ComplaintResponse>>(this.api, { params });
  }

  // getComplaint(id: number) {
  //   return this.http.get<ComplaintResponse>(`${this.api}/${id}`);
  // }

  getComplaintByRef(refNumber: string) {
    return this.http.get<ComplaintResponse>(`${this.api}/${refNumber}`);
  }

  createComplaint(payload: any) {
    return this.http.post<ComplaintResponse>(this.api, payload);
  }

  getRaiserSuggestions(q: string) {
    return this.http.get<string[]>(`${this.api}/raisers`, { params: { q } });
  }

  assignComplaint(id: number, assignedToId: number) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/assign`, { assignedToId });
  }

  startComplaint(id: number) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/start`, {});
  }

  addAnalysis(id: number, payload: { content: string; servingSitesCells?: string; coverageQuality?: string }) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/analysis`, payload);
  }

  editAnalysis(id: number, entryId: number, payload: any) {
    return this.http.put<ComplaintResponse>(`${this.api}/${id}/analysis/${entryId}`, payload);
  }

  addSolution(id: number, payload: { content: string; solutionTargetDate?: string; remarks?: string }) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/solution`, payload);
  }

  editSolution(id: number, entryId: number, payload: any) {
    return this.http.put<ComplaintResponse>(`${this.api}/${id}/solution/${entryId}`, payload);
  }

  escalateToEngineer(id: number, engineerId: number, notes?: string) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/escalate`, { engineerId, notes });
  }

  markResolved(id: number, customerFeedbackTaken: boolean, notes?: string) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/resolve`, { customerFeedbackTaken, notes });
  }

  closeComplaint(id: number, notes?: string) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/close`, { notes });
  }

  reopenComplaint(id: number, assignedToId: number, notes: string) {
    return this.http.post<ComplaintResponse>(`${this.api}/${id}/reopen`, { assignedToId, notes });
  }

  getMyQueue(page = 0, size = 10) {
    const params = new HttpParams()
      .set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<ComplaintResponse>>(`${this.api}/my-queue`, { params });
  }

}
