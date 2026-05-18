import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { UsersComponent } from './users/users.component';
import { AuthGuard } from './auth/auth.guard';
import { ProfileComponent } from './profile/profile.component';
import { BlogListComponent } from './blog/blog-list/blog-list.component';
import { BlogDetailComponent } from './blog/blog-detail/blog-detail.component';
import { BlogFormComponent } from './blog/blog-form/blog-form.component';
import { TourListComponent } from './tour/tour-list/tour-list.component';
import { TourFormComponent } from './tour/tour-form/tour-form.component';
import { TourDetailComponent } from './tour/tour-detail/tour-detail.component';
import { TourKeypointsComponent } from './tour/tour-keypoints/tour-keypoints.component';
import { TourReviewsComponent } from './tour/tour-reviews/tour-reviews.component';
import { AllToursComponent } from './tour/all-tours/all-tours.component';
import { PositionSimulatorComponent } from './simulator/position-simulator.component';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'users', component: UsersComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent },
  { path: 'simulator', component: PositionSimulatorComponent, canActivate: [AuthGuard] },

  // Blog routes
  { path: 'blogs', component: BlogListComponent, canActivate: [AuthGuard] },
  { path: 'blogs/new', component: BlogFormComponent, canActivate: [AuthGuard] },
  { path: 'blogs/:id', component: BlogDetailComponent, canActivate: [AuthGuard] },
  
  // Tour routes
  { path: 'tours', component: TourListComponent, canActivate: [AuthGuard] },
  { path: 'tours/new', component: TourFormComponent, canActivate: [AuthGuard] },
  { path: 'tours/:id', component: TourDetailComponent, canActivate: [AuthGuard] },
  { path: 'tours/:id/edit', component: TourFormComponent, canActivate: [AuthGuard] },
  { path: 'tours/:id/keypoints', component: TourKeypointsComponent, canActivate: [AuthGuard] },
  { path: 'tours/:id/reviews', component: TourReviewsComponent, canActivate: [AuthGuard] },
  { path: 'all-tours', component: AllToursComponent, canActivate: [AuthGuard] },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
