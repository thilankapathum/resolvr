import {Component, computed, HostListener, inject, input, OnInit, signal} from '@angular/core';
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
  initialAttachments = input<AttachmentResponse[]>([]);
  canUpload          = input<boolean>(false);

  private svc   = inject(AttachmentService);
  private toast = inject(ToastService);

  attachments  = signal<AttachmentResponse[]>([]);
  uploading    = signal(false);

  // ── Gallery state ──────────────────────────────────────────
  galleryOpen  = signal(false);
  galleryIndex = signal(0);                       // current index in mediaItems

  /** Only images and videos — the items navigable in the gallery. */
  mediaItems = computed(() =>
    this.attachments().filter(a => this.isImage(a) || this.isVideo(a))
  );

  /** Non-media files shown separately as download chips. */
  docItems = computed(() =>
    this.attachments().filter(a => !this.isImage(a) && !this.isVideo(a))
  );

  currentItem = computed(() => this.mediaItems()[this.galleryIndex()] ?? null);

  ngOnInit() {
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

  // ── Gallery navigation ─────────────────────────────────────

  openGallery(att: AttachmentResponse) {
    const idx = this.mediaItems().indexOf(att);
    this.galleryIndex.set(idx >= 0 ? idx : 0);
    this.galleryOpen.set(true);
  }

  closeGallery() { this.galleryOpen.set(false); }

  prev() {
    const len = this.mediaItems().length;
    this.galleryIndex.update(i => (i - 1 + len) % len);
  }

  next() {
    const len = this.mediaItems().length;
    this.galleryIndex.update(i => (i + 1) % len);
  }

  goTo(idx: number) { this.galleryIndex.set(idx); }

  @HostListener('document:keydown', ['$event'])
  onKey(e: KeyboardEvent) {
    if (!this.galleryOpen()) return;
    if (e.key === 'ArrowLeft')  { e.preventDefault(); this.prev(); }
    if (e.key === 'ArrowRight') { e.preventDefault(); this.next(); }
    if (e.key === 'Escape')     { e.preventDefault(); this.closeGallery(); }
  }

  // ── Upload ─────────────────────────────────────────────────

  onFilePicked(event: Event) {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;
    input.value = '';
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

  deleteAttachment(att: AttachmentResponse, event: MouseEvent) {
    event.stopPropagation();
    if (!confirm(`Delete "${att.originalName}"? This cannot be undone.`)) return;
    this.svc.delete(att.id).subscribe({
      next: () => {
        // If we delete the currently viewed item, close or shift index
        const wasMedia = this.isImage(att) || this.isVideo(att);
        if (wasMedia && this.galleryOpen()) {
          const newMedia = this.mediaItems().filter(a => a.id !== att.id);
          if (newMedia.length === 0) { this.closeGallery(); }
          else { this.galleryIndex.set(Math.min(this.galleryIndex(), newMedia.length - 1)); }
        }
        this.attachments.update(list => list.filter(a => a.id !== att.id));
        this.toast.success('Attachment deleted.');
      },
      error: e => this.toast.error(e.error?.message ?? 'Delete failed.'),
    });
  }

  // ── Helpers ────────────────────────────────────────────────

  isImage(att: AttachmentResponse): boolean { return this.svc.isImage(att.mimeType); }
  isVideo(att: AttachmentResponse): boolean { return this.svc.isVideo(att.mimeType); }
  isPdf  (att: AttachmentResponse): boolean { return this.svc.isPdf(att.mimeType);   }
  formatBytes(b: number):           string  { return this.svc.formatBytes(b); }

  docIcon(att: AttachmentResponse): 'pdf' | 'doc' {
    return this.isPdf(att) ? 'pdf' : 'doc';
  }
}
