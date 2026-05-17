import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TourKeypointsComponent } from './tour-keypoints.component';

describe('TourKeypointsComponent', () => {
  let component: TourKeypointsComponent;
  let fixture: ComponentFixture<TourKeypointsComponent>;

  beforeEach(async) {
    await TestBed.configureTestingModule({
      declarations: [ TourKeypointsComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TourKeypointsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
