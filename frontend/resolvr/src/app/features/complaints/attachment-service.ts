import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../../environments/environment';

export interface AttachmentResponse {
  id: number;
  entryType: 'ANALYSIS' | 'SOLUTION';
  entryId: number;
  originalName: string;
  mimeType: string;
  fileSizeBytes: number;
  createdAt: string;
  uploadedByName: string;
  /** Time-limited presigned MinIO URL — use directly in <img>, <video>, <a>. */
  url: string;
}

@Injectable({
  providedIn: 'root',
})
export class AttachmentService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}`;

  uploadForAnalysis(complaintId: number, entryId: number, file: File) {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<AttachmentResponse>(
      `${this.api}/complaints/${complaintId}/analysis/${entryId}/attachments`, fd
    );
  }

  uploadForSolution(complaintId: number, entryId: number, file: File) {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<AttachmentResponse>(
      `${this.api}/complaints/${complaintId}/solution/${entryId}/attachments`, fd
    );
  }

  listForAnalysis(complaintId: number, entryId: number) {
    return this.http.get<AttachmentResponse[]>(
      `${this.api}/complaints/${complaintId}/analysis/${entryId}/attachments`
    );
  }

  listForSolution(complaintId: number, entryId: number) {
    return this.http.get<AttachmentResponse[]>(
      `${this.api}/complaints/${complaintId}/solution/${entryId}/attachments`
    );
  }

  delete(attachmentId: number) {
    return this.http.delete<void>(`${this.api}/attachments/${attachmentId}`);
  }

  isImage(mimeType: string): boolean { return mimeType.startsWith('image/'); }
  isVideo(mimeType: string): boolean { return mimeType.startsWith('video/'); }
  isPdf  (mimeType: string): boolean { return mimeType === 'application/pdf'; }

  formatBytes(bytes: number): string {
    if (bytes < 1024)        return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

}
