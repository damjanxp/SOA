# Tour Service

Spring Boot microservice za upravljanje turама, ključnim tačkama i recenzijама.

## Karakteristike

- **Tour Management**: Kreiranje, ažuriranje, brisanje i objavljivanje tura
- **Keypoints**: Upravljanje ključnim tačkama sa GPS koordinatama
- **Reviews**: Sistem recenzija sa ocenama (1-5) i slikama
- **JWT Authentication**: Zaštita preko X-User-Id headera
- **PostgreSQL**: Perzistencija podataka
- **Docker**: Multi-stage build i kontejnerizacija

## Tehnologije

- Java 21
- Spring Boot 3.1.5
- Spring Data JPA
- PostgreSQL 16
- Maven
- Docker

## API Endpoints

### Tours

- `POST /api/tours` - Kreiraj novu turu
- `GET /api/tours/my` - Lista mojih tura
- `GET /api/tours/{id}` - Preuzmi turu
- `PUT /api/tours/{id}` - Ažuriraj turu
- `DELETE /api/tours/{id}` - Obriši turu
- `POST /api/tours/{id}/publish` - Objavi turu

### Keypoints

- `POST /api/tours/{tourId}/keypoints` - Dodaj ključnu tačku
- `GET /api/tours/{tourId}/keypoints` - Lista ključnih tačaka
- `GET /api/tours/{tourId}/keypoints/{kpId}` - Preuzmi ključnu tačku
- `PUT /api/tours/{tourId}/keypoints/{kpId}` - Ažuriraj ključnu tačku
- `DELETE /api/tours/{tourId}/keypoints/{kpId}` - Obriši ključnu tačku

### Reviews

- `POST /api/tours/{tourId}/reviews` - Ostavi recenziju
- `GET /api/tours/{tourId}/reviews` - Lista recenzija
- `GET /api/tours/{tourId}/reviews/{reviewId}` - Preuzmi recenziju
- `DELETE /api/tours/{tourId}/reviews/{reviewId}` - Obriši recenziju

## Pokretanje

### Lokalno

```bash
mvn clean install
mvn spring-boot:run
```

### Docker

```bash
docker-compose up tour-service postgres-tours
```

## Baza podataka

Automatski se kreiraju tabele:
- `tours` - Ture
- `tour_tags` - Tagovi za ture
- `keypoints` - Ključne tačke
- `reviews` - Recenzije
- `review_images` - Slike u recenzijama

## Konfiguracija

Promenljive okruženja (.env):

```
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-tours:5432/tours_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres123
SERVER_PORT=8083
```

## Autentifikacija

Servis očekuje `X-User-Id` header u zahtevima:

```
X-User-Id: 1
```

## Struktura projekta

```
tour-service/
├── src/main/java/com/soa/
│   ├── TourServiceApplication.java
│   ├── models/          - JPA entiteti
│   ├── controllers/      - REST kontroleri
│   ├── services/         - Poslovna logika
│   ├── repositories/     - Data access
│   ├── dtos/            - Transfer objekti
│   ├── middleware/       - JWT filter
│   ├── exceptions/       - Exception handling
│   └── config/          - Spring konfiguracija
├── src/main/resources/
│   └── application.yml   - Spring Boot konfiguracija
├── pom.xml
└── Dockerfile           - Multi-stage build
```

## Validacija

- Tour ime: Obavezno
- Difficulty: EASY, MEDIUM, HARD
- Status: DRAFT, PUBLISHED, ARCHIVED
- Rating: 1-5
- Latitude/Longitude: Obavezno za keypoints
