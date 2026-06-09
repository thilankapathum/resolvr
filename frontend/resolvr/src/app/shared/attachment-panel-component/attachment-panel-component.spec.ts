import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AttachmentPanelComponent } from './attachment-panel-component';

describe('AttachmentPanelComponent', () => {
  let component: AttachmentPanelComponent;
  let fixture: ComponentFixture<AttachmentPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AttachmentPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AttachmentPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
