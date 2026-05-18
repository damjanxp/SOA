import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AuthInterceptor } from './auth/auth.interceptor';
import { PositionSimulatorComponent } from './simulator/position-simulator.component';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { UsersComponent } from './users/users.component';
import { NavbarComponent } from './navbar/navbar.component';
import { ProfileComponent } from './profile/profile.component';
import { BlogListComponent } from './blog/blog-list/blog-list.component';
import { BlogDetailComponent } from './blog/blog-detail/blog-detail.component';
import { BlogFormComponent } from './blog/blog-form/blog-form.component';
import { TourListComponent } from './tour/tour-list/tour-list.component';
import { TourFormComponent } from './tour/tour-form/tour-form.component';
import { TourDetailComponent } from './tour/tour-detail/tour-detail.component';
import { TourKeypointsComponent } from './tour/tour-keypoints/tour-keypoints.component';
import { TourReviewsComponent } from './tour/tour-reviews/tour-reviews.component';
import { RouterModule } from '@angular/router';
import { AllToursComponent } from './tour/all-tours/all-tours.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    UsersComponent,
    NavbarComponent,
    ProfileComponent,
    BlogListComponent,
    BlogDetailComponent,
    BlogFormComponent,
    TourListComponent,
    TourFormComponent,
    TourDetailComponent,
    TourKeypointsComponent,
    TourReviewsComponent,
    AllToursComponent,
    PositionSimulatorComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule 
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
