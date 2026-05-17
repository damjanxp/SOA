# Odbrana projekta — SOA Turistička platforma

---

## 1. Šta je ovaj projekat?

Turistička platforma bazirana na **SOA (Service-Oriented Architecture)** — arhitektura gdje je sistem podijeljen na više manjih, nezavisnih servisa. Svaki servis radi svoju stvar i ne zna za internals drugog servisa.

**Servisi:**
| Servis | Jezik | Baza | Port |
|--------|-------|------|------|
| stakeholders-service | Go | PostgreSQL | 8081 |
| blog-service | TypeScript/Node.js | MongoDB | 8082 |
| tour-service | Go | - | 8083 |
| follower-service | Go | - | 8084 |
| Frontend | Angular | - | 4200 |

**Zašto SOA a ne monolitna aplikacija?**
- Svaki servis se može razvijati, deployovati i skalirati nezavisno
- Ako jedan servis padne, ostali rade dalje
- Različiti servisi mogu koristiti različite tehnologije i baze podataka

---

## 2. Docker — osnove (najvažnije za odbranu)

### Šta je Docker?
Docker omogućava da aplikacija radi **identično na svakom računaru**. Pakuje kod zajedno sa svime što mu treba (Node.js, Go, biblioteke, OS) u izolovanu jedinicu.

### Slika vs Kontejner
- **Slika (Image)** = recept. Statična, ne izvršava se. Gradi se iz Dockerfile-a.
- **Kontejner (Container)** = pokrenuta slika. Živi proces koji izvršava aplikaciju.

Iz jedne slike možeš pokrenuti više identičnih kontejnera.

### Multi-stage build — zašto?
Koristi se u svim servisima. Primjer: blog-service
- **Faza 1 (builder)**: instalira sve alate, prevodi TypeScript → JavaScript
- **Faza 2 (produkcija)**: uzima samo finalni kod, bez kompajlera i temp fajlova

**Rezultat**: produkcijska slika je ~10x manja. Manje prostora, brži deploy, manji napadna površina.

---

## 3. Dockerfile — svaka instrukcija

```dockerfile
FROM node:22-alpine       # Osnova: Linux + Node.js. Alpine = minimalni Linux (~5MB)
WORKDIR /app              # Radni folder unutar containera, kao "cd /app"
COPY package.json ./      # Kopira fajlove sa tvog računara u container
RUN npm ci                # Izvršava komandu TOKOM BUILDA (instalacija paketa)
COPY . .                  # Kopira cijeli kod
RUN npx tsc               # Prevodi TypeScript u JavaScript (output: /app/dist/)
EXPOSE 8082               # Dokumentacija: "koristim port 8082" (ne otvara port!)
CMD ["node","dist/app.js"]# Komanda koja se pokrene kada se CONTAINER STARTUJE
```

**Razlika RUN vs CMD:**
- `RUN` = izvršava se jednom, pri pravljenju slike
- `CMD` = izvršava se svaki put kad pokreneš container

**Zašto se dependency fajlovi kopiraju odvojeno od koda?**
Docker cachuje svaki korak. Ako `package.json` nije promijenjen, Docker preskače `npm ci` iz cachea. Ubrzava svaki naredni build.

---

## 4. docker-compose.yml — objašnjenje

docker-compose koordinira pokretanje **svih servisa zajedno**.

```yaml
services:
  stakeholders-service:
    build: ./stakeholders-service    # Gradi sliku iz Dockerfile-a u tom folderu
    ports:
      - "8081:8081"                  # HOST:CONTAINER mapiranje porta
    env_file:
      - ./stakeholders-service/.env  # Učitava tajne podatke (JWT, DB lozinka)
    depends_on:
      postgres-stakeholders:
        condition: service_healthy   # Ne pokreći servis dok baza nije zdrava
```

### depends_on i healthcheck — zašto?
Baza treba nekoliko sekundi da se inicijalizuje. Bez `depends_on`, servis bi pokušao da se konektuje na bazu koja još nije spremna i pao bi.

`healthcheck` — Docker svakih 5 sekundi testira da li je baza stvarno spremna (ne samo pokrenuta):
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres"]  # Testna komanda
  interval: 5s   # Koliko često testira
  timeout: 5s    # Koliko čeka odgovor
  retries: 5     # Koliko pokušaja prije "unhealthy"
```

### Ports — mapiranje
`"5433:5432"` znači: port `5433` na tvom računaru → port `5432` unutar containera.  
Zašto različiti? Da ne bi bio konflikt ako imaš lokalnu PostgreSQL instalaciju na 5432.

### env_file
`.env` fajl sadrži tajne podatke (JWT secret, lozinke baze). Ne commituje se na git. Servis čita ove varijable pri pokretanju.

---

## 5. Docker Network

```yaml
networks:
  default:
    name: tourism-network
```

Svi servisi su na istoj virtualnoj mreži. Zato `blog-service` može komunicirati sa `mongo` koristeći samo ime `mongo` (Docker interni DNS) umjesto IP adrese.

Primjer iz koda:
```
MONGO_URI=mongodb://mongo:27017/blog_db
```
`mongo` ovdje nije localhost — to je **ime servisa** iz docker-compose.

---

## 6. Autentifikacija — JWT

### Kako radi?
1. Korisnik se loguje → server mu vraća **JWT token**
2. Token sadrži: `userId`, `username`, `role` (kriptovano)
3. Svaki naredni zahtjev šalje token u headeru: `Authorization: Bearer TOKEN`
4. Server dekodira token i zna ko je korisnik bez upita u bazu

### Šta je u tokenu?
```go
type Claims struct {
    UserID   string  // ID korisnika
    Username string  // Korisničko ime
    Role     string  // "tourist", "guide", "admin"
}
```

### Zaštićene rute
```go
protected := r.Group("/api")
protected.Use(middleware.AuthMiddleware())  // Middleware provjerava token
```
Svaki zahtjev na `/api/users`, `/api/profile` itd. mora imati validan token.

### Zašto ne možeš registrovati admina?
```go
if input.Role == models.RoleAdmin {
    c.JSON(400, gin.H{"error": "Ne mozete se registrovati kao admin"})
    return
}
```
Admin se mora ručno ubaciti u bazu (direktno SQL INSERT).

---

## 7. Baze podataka

### PostgreSQL — stakeholders-service
- Relaciona baza, koristi tabele
- Čuva: korisnike, profile
- Go ORM: **GORM** — automatski kreira tabele (`AutoMigrate`)

### MongoDB — blog-service
- NoSQL baza, čuva dokumente (JSON format)
- Čuva: blogove, lajkove
- Node.js biblioteka: **Mongoose**

---

## 8. CORS
```go
c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS")
```
Browser blokira zahtjeve na drugi port/domen bez CORS headera. Angular (port 4200) šalje zahtjeve na Go (port 8081) — bez CORS servisi ne bi radili.

---

## 9. Korisne Docker komande

```bash
docker compose up -d --build   # Pokreni sve (rebuild slika, u pozadini)
docker compose down            # Ugasi sve i obriši mrežu
docker compose ps              # Lista pokrenutih kontejnera i njihov status
docker compose logs <servis>   # Logovi određenog servisa (za debug)
docker exec -it <container> sh # Uđi u container (terminal unutar containera)
```

---

## 10. Moguća pitanja na odbrani

**P: Zašto Docker a ne direktno pokretanje?**  
O: Izolacija okruženja — radi identično na svakom računaru. Nema problema "radi na mom računaru, ne radi na serveru".

**P: Šta je razlika između image i container?**  
O: Image je statični recept, container je pokrenuta instanca tog recepta.

**P: Zašto multi-stage build?**  
O: Da produkcijska slika bude manja — ne treba joj kompajler, samo kompajlirani kod.

**P: Kako servisi komuniciraju?**  
O: Kroz Docker internu mrežu (`tourism-network`). Koriste ime servisa kao hostname (npr. `mongo`, `postgres-stakeholders`).

**P: Zašto depends_on?**  
O: Servis ne smije startovati dok baza nije potpuno inicijalizovana.

**P: Kako radi JWT autentifikacija?**  
O: Korisnik se loguje, dobija token sa svojim podacima (ID, role). Token šalje uz svaki zahtjev, server ga validira bez upita u bazu.

**P: Zašto PostgreSQL za stakeholders a MongoDB za blog?**  
O: Korisnici/profili su strukturirani podaci sa relacijama → relaciona baza. Blogovi su fleksibilni dokumenti bez fiksne strukture → NoSQL.
