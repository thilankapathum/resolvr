import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegionDistrictComponent } from './region-district-component';

describe('RegionDistrictComponent', () => {
  let component: RegionDistrictComponent;
  let fixture: ComponentFixture<RegionDistrictComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegionDistrictComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegionDistrictComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
