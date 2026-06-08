import {Component, inject, input, OnInit, signal} from '@angular/core';
import {AttachmentResponse, AttachmentService} from '../../features/complaints/attachment-service';
import {ToastService} from '../toast-service';

@Component({
  selector: 'app-attachment-panel-component',
  imports: [],
  templateUrl: './attachment-panel-component.html',
  styleUrl: './attachment-panel-component.css',
})
export class AttachmentPanelComponent implements OnInit{
  entryType          = input.required<'ANALYSIS' | 'SOLUTION'>();
  complaintId        = input.required<number>();
  entryId            = input.required<number>();
  /** Pre-populated from the parent complaint response — avoids an extra HTTP round-trip. */
  initialAttachments = input<AttachmentResponse[]>([]);
  canUpload          = input<boolean>(false);

  private svc   = inject(AttachmentService);
  private toast = inject(ToastService);

  attachments  = signal<AttachmentResponse[]>([]);
  uploading    = signal(false);
  lightboxItem = signal<AttachmentResponse | null>(null);

  ngOnInit() {
    // Use pre-populated list; fall back to a fresh fetch if parent didn't supply them.
    const initial = this.initialAttachments();
    if (initial && initial.length > 0) {
      this.attachments.set(initial);
    } else {
      this.refresh();
    }
  }

  refresh() {
    const obs = this.entryType() === 'ANALYSIS'
      ? this.svc.listForAnalysis(this.complaintId(), this.entryId())
      : this.svc.listForSolution(this.complaintId(), this.entryId());
    obs.subscribe({ next: list => this.attachments.set(list) });
  }

  // ── Upload ─────────────────────────────────────────────────

  onFilePicked(event: Event) {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;
    input.value = ''; // reset so the same file can be re-selected

    this.uploading.set(true);

    this.maybeCompressImage(file).then(toUpload => {
      const obs = this.entryType() === 'ANALYSIS'
        ? this.svc.uploadForAnalysis(this.complaintId(), this.entryId(), toUpload)
        : this.svc.uploadForSolution(this.complaintId(), this.entryId(), toUpload);

      obs.subscribe({
        next: att => {
          this.attachments.update(list => [...list, att]);
          this.uploading.set(false);
          this.toast.success('File attached successfully.');
        },
        error: e => {
          this.uploading.set(false);
          this.toast.error(e.error?.message ?? 'Upload failed. Please try again.');
        },
      });
    });
  }

  /**
   * Client-side pre-compression using Canvas API.
   * Reduces upload bandwidth for large phone photos before the server
   * applies its own Thumbnailator compression pass.
   * - Max 1600px long edge
   * - 82% JPEG quality
   * - Non-images and GIFs returned unchanged
   */
  private maybeCompressImage(file: File): Promise<File> {
    if (!file.type.startsWith('image/') || file.type === 'image/gif') {
      return Promise.resolve(file);
    }
    return new Promise(resolve => {
      const img = new Image();
      const url = URL.createObjectURL(file);
      img.onload = () => {
        URL.revokeObjectURL(url);
        const MAX = 1600;
        let { width, height } = img;
        if (width > MAX || height > MAX) {
          if (width > height) { height = Math.round(height * MAX / width);  width = MAX; }
          else                { width  = Math.round(width  * MAX / height); height = MAX; }
        }
        const canvas = document.createElement('canvas');
        canvas.width = width; canvas.height = height;
        canvas.getContext('2d')!.drawImage(img, 0, 0, width, height);
        canvas.toBlob(blob => {
          if (!blob) { resolve(file); return; }
          resolve(new File([blob], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' }));
        }, 'image/jpeg', 0.82);
      };
      img.onerror = () => { URL.revokeObjectURL(url); resolve(file); };
      img.src = url;
    });
  }

  // ── Delete ─────────────────────────────────────────────────

  deleteAttachment(att: AttachmentResponse) {
    if (!confirm(`Delete "${att.originalName}"? This cannot be undone.`)) return;
    this.svc.delete(att.id).subscribe({
      next: () => {
        this.attachments.update(list => list.filter(a => a.id !== att.id));
        this.toast.success('Attachment deleted.');
      },
      error: e => this.toast.error(e.error?.message ?? 'Delete failed.'),
    });
  }

  // ── Lightbox ───────────────────────────────────────────────

  openLightbox(att: AttachmentResponse) { this.lightboxItem.set(att); }
  closeLightbox()                        { this.lightboxItem.set(null); }

  // ── Helpers ────────────────────────────────────────────────

  isImage(att: AttachmentResponse): boolean { return this.svc.isImage(att.mimeType); }
  isVideo(att: AttachmentResponse): boolean { return this.svc.isVideo(att.mimeType); }
  isPdf  (att: AttachmentResponse): boolean { return this.svc.isPdf(att.mimeType);   }
  formatBytes(b: number):           string  { return this.svc.formatBytes(b); }

}
