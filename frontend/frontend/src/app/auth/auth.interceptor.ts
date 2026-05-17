// import { Injectable } from '@angular/core';
// // import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';
// // import { AuthService } from './auth.service';

// // @Injectable()
// // export class AuthInterceptor implements HttpInterceptor {
// //   constructor(private authService: AuthService) {}

// //   intercept(req: HttpRequest<any>, next: HttpHandler) {
// //   if (!req.url.includes('localhost:8083')) {
// //     return next.handle(req);
// //   }

// //   const token = this.authService.getToken();
// //   if (token) {
// //     const cloned = req.clone({
// //       setHeaders: { 'Authorization': `Bearer ${token}` }
// //     });
// //     return next.handle(cloned);
// //   }

// //   return next.handle(req);
// // }
// // }
// // =======
// import {
//   HttpEvent,
//   HttpHandler,
//   HttpInterceptor,
//   HttpRequest,
// } from '@angular/common/http';
// import { Observable } from 'rxjs';

// @Injectable()
// export class AuthInterceptor implements HttpInterceptor {
//   intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
//     const token = localStorage.getItem('token');
//     if (token) {
//       const cloned = req.clone({
//         setHeaders: {
//           Authorization: `Bearer ${token}`,
//         },
//       });
//       return next.handle(cloned);
//     }
//     return next.handle(req);
//   }
// }
// // >>>>>>> main
import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    // 1. opcionalno: preskoči neke URL-ove (kao HEAD verzija)
    if (req.url.includes('assets') || req.url.includes('public')) {
      return next.handle(req);
    }

    // 2. uzmi token (kombinacija oba pristupa)
    const token = this.authService.getToken() || localStorage.getItem('token');

    // 3. ako postoji token -> dodaj ga
    if (token) {
      const cloned = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
      return next.handle(cloned);
    }

    return next.handle(req);
  }
}