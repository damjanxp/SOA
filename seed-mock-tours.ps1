################################################################################
# seed-mock-tours.ps1
# Creates mock users (admin, guide, tourist) and tours in DRAFT, PUBLISHED,
# ARCHIVED statuses with keypoints and transport times.
#
# Usage: .\seed-mock-tours.ps1
# Requirements: Docker services must be running on localhost:8080
################################################################################

$BASE = "http://localhost:8080"

# ── Helper: decode JWT payload ─────────────────────────────────────────────────
function Get-JwtPayload($token) {
    $parts = $token.Split('.')
    $pad   = 4 - ($parts[1].Length % 4)
    $b64   = $parts[1] + ('=' * ($pad % 4))
    $json  = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
    return $json | ConvertFrom-Json
}

# ── Helper: make authenticated request ────────────────────────────────────────
function Invoke-Auth($method, $url, $token, $body = $null) {
    $headers = @{ Authorization = "Bearer $token" }
    if ($body) {
        return Invoke-RestMethod -Uri $url -Method $method -ContentType "application/json" `
                                 -Headers $headers -Body ($body | ConvertTo-Json -Depth 10) -ErrorAction Stop
    }
    return Invoke-RestMethod -Uri $url -Method $method -Headers $headers -ErrorAction Stop
}

# ══════════════════════════════════════════════════════════════════════════════
# STEP 1 — Register users
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "`n=== Registering mock users ===" -ForegroundColor Cyan

$mockUsers = @(
    @{ username = "admin_user";   email = "admin@tourism.com";   password = "Admin1234!"; role = "admin"   },
    @{ username = "guide_marko";  email = "marko@tourism.com";   password = "Guide1234!"; role = "guide"   },
    @{ username = "guide_ana";    email = "ana@tourism.com";     password = "Guide1234!"; role = "guide"   },
    @{ username = "tourist_petar";email = "petar@tourism.com";   password = "Tour1234!";  role = "tourist" },
    @{ username = "tourist_maja"; email = "maja@tourism.com";    password = "Tour1234!";  role = "tourist" }
)

foreach ($u in $mockUsers) {
    try {
        Invoke-RestMethod -Uri "$BASE/api/auth/register" -Method POST `
            -ContentType "application/json" -Body ($u | ConvertTo-Json) -ErrorAction Stop | Out-Null
        Write-Host "  [OK] Registered $($u.username)" -ForegroundColor Green
    } catch {
        Write-Host "  [--] $($u.username) already exists (skipping)" -ForegroundColor Yellow
    }
}

# ══════════════════════════════════════════════════════════════════════════════
# STEP 2 — Login and collect tokens
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "`n=== Logging in ===" -ForegroundColor Cyan

$tokens  = @{}
$userIds = @{}

foreach ($u in $mockUsers) {
    try {
        $body = @{ username = $u.username; password = $u.password } | ConvertTo-Json
        $resp = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST `
                -ContentType "application/json" -Body $body -ErrorAction Stop
        $tokens[$u.username]  = $resp.token
        $payload              = Get-JwtPayload $resp.token
        $userIds[$u.username] = $payload.userId
        Write-Host "  [OK] $($u.username) => userId: $($payload.userId)" -ForegroundColor Green
    } catch {
        Write-Host "  [!!] Login failed for $($u.username): $($_.Exception.Message)" -ForegroundColor Red
    }
}

$markoToken  = $tokens["guide_marko"]
$anaToken    = $tokens["guide_ana"]
$petarToken  = $tokens["tourist_petar"]

if (-not $markoToken) {
    Write-Host "`n[FATAL] Could not get token for guide_marko. Aborting." -ForegroundColor Red
    exit 1
}

# ══════════════════════════════════════════════════════════════════════════════
# STEP 3 — Create tours for guide_marko
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "`n=== Creating tours for guide_marko ===" -ForegroundColor Cyan

# Helper: create tour, keypoints, transport times, then publish/archive
function New-Tour($token, $tourData) {
    return Invoke-Auth "POST" "$BASE/api/tours" $token $tourData
}

function Add-Keypoint($token, $tourId, $kp) {
    return Invoke-Auth "POST" "$BASE/api/tours/$tourId/keypoints" $token $kp
}

function Add-TransportTime($token, $tourId, $tt) {
    return Invoke-Auth "POST" "$BASE/api/tours/$tourId/transport-times" $token $tt
}

function Publish-Tour($token, $tourId) {
    return Invoke-Auth "POST" "$BASE/api/tours/$tourId/publish" $token
}

function Archive-Tour($token, $tourId) {
    return Invoke-Auth "POST" "$BASE/api/tours/$tourId/archive" $token
}

# ─────────────────────────────────────────────────────────────
# TOUR 1: PUBLISHED — Stari Grad Sarajevo
# ─────────────────────────────────────────────────────────────
Write-Host "`n  [1/5] Kreiranje: Stari Grad Sarajevo (PUBLISHED)..." -ForegroundColor White
try {
    $t1 = New-Tour $markoToken @{
        name        = "Stari Grad Sarajevo"
        description = "Sетња кроз историjski centar Sarajeva - Baščaršija, Sebilj, Gazi Husrev-begova džamija."
        difficulty  = "EASY"
        tags        = @("history", "culture", "walking", "sarajevo")
        lengthKm    = 4.5
    }
    $id1 = $t1.id
    Write-Host "    Tura kreirana, id=$id1" -ForegroundColor DarkGray

    Add-Keypoint $markoToken $id1 @{ lat=43.8563; lon=18.4131; name="Sebilj - Baščaršija"; description="Čuvena fontana u srcu Baščaršije."; orderIndex=0 } | Out-Null
    Add-Keypoint $markoToken $id1 @{ lat=43.8584; lon=18.4312; name="Gazi Husrev-begova džamija"; description="Najpoznatija džamija u Bosni iz 16. vijeka."; orderIndex=1 } | Out-Null
    Add-Keypoint $markoToken $id1 @{ lat=43.8606; lon=18.4230; name="Vijećnica"; description="Austrougarska gradska vijećnica, simbol Sarajeva."; orderIndex=2 } | Out-Null
    Add-Keypoint $markoToken $id1 @{ lat=43.8617; lon=18.4200; name="Latinska ćuprija"; description="Most gdje je počeo Prvi svjetski rat."; orderIndex=3 } | Out-Null
    Write-Host "    Keypointi dodani (4)" -ForegroundColor DarkGray

    Add-TransportTime $markoToken $id1 @{ transportType="WALK"; durationMinutes=90 } | Out-Null
    Write-Host "    Transport dodan" -ForegroundColor DarkGray

    Publish-Tour $markoToken $id1 | Out-Null
    Write-Host "  [OK] PUBLISHED: Stari Grad Sarajevo (id=$id1)" -ForegroundColor Green
} catch {
    Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────
# TOUR 2: PUBLISHED — Trebević planina
# ─────────────────────────────────────────────────────────────
Write-Host "`n  [2/5] Kreiranje: Trebević planina (PUBLISHED)..." -ForegroundColor White
try {
    $t2 = New-Tour $markoToken @{
        name        = "Trebević planinska tura"
        description = "Pješacka tura na Trebević iznad Sarajeva sa panoramskim pogledom na grad. Prolaz kroz napuštenu bobslejsku stazu."
        difficulty  = "MEDIUM"
        tags        = @("mountain", "hiking", "nature", "panorama")
        lengthKm    = 12.0
    }
    $id2 = $t2.id
    Write-Host "    Tura kreirana, id=$id2" -ForegroundColor DarkGray

    Add-Keypoint $markoToken $id2 @{ lat=43.8414; lon=18.4189; name="Polazište - Bistrik"; description="Start ture u starom dijelu Sarajeva."; orderIndex=0 } | Out-Null
    Add-Keypoint $markoToken $id2 @{ lat=43.8165; lon=18.4270; name="Bobslejska staza"; description="Napuštena staza iz doba Olimpijade 1984."; orderIndex=1 } | Out-Null
    Add-Keypoint $markoToken $id2 @{ lat=43.8071; lon=18.4326; name="Vrh Trebevića (1629m)"; description="Panoramski vrh sa pogledom na cijelo Sarajevo."; orderIndex=2 } | Out-Null
    Write-Host "    Keypointi dodani (3)" -ForegroundColor DarkGray

    Add-TransportTime $markoToken $id2 @{ transportType="WALK"; durationMinutes=240 } | Out-Null
    Add-TransportTime $markoToken $id2 @{ transportType="BIKE"; durationMinutes=90 } | Out-Null
    Write-Host "    Transport dodan" -ForegroundColor DarkGray

    Publish-Tour $markoToken $id2 | Out-Null
    Write-Host "  [OK] PUBLISHED: Trebević planinska tura (id=$id2)" -ForegroundColor Green
} catch {
    Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────
# TOUR 3: ARCHIVED — Mostar stari most (arhivirana)
# ─────────────────────────────────────────────────────────────
Write-Host "`n  [3/5] Kreiranje: Mostar Stari Most (ARCHIVED)..." -ForegroundColor White
try {
    $t3 = New-Tour $markoToken @{
        name        = "Mostar - Stari Most i Staro Jezero"
        description = "Jednodnevna tura u Mostaru sa obilaskom Starog Mosta, Kujundziluk čaršije i skakanjem sa mosta."
        difficulty  = "EASY"
        tags        = @("mostar", "culture", "heritage", "unesco")
        lengthKm    = 5.0
    }
    $id3 = $t3.id
    Write-Host "    Tura kreirana, id=$id3" -ForegroundColor DarkGray

    Add-Keypoint $markoToken $id3 @{ lat=43.3361; lon=17.8151; name="Stari Most"; description="UNESCO zaštićeni kameni most iz 16. vijeka."; orderIndex=0 } | Out-Null
    Add-Keypoint $markoToken $id3 @{ lat=43.3373; lon=17.8157; name="Kujundžiluk čaršija"; description="Stara orijentalana čaršija sa zanatlijama."; orderIndex=1 } | Out-Null
    Add-Keypoint $markoToken $id3 @{ lat=43.3388; lon=17.8155; name="Koski Mehmed-pašina džamija"; description="Džamija sa tornjem sa kojeg je spektakularan pogled."; orderIndex=2 } | Out-Null
    Write-Host "    Keypointi dodani (3)" -ForegroundColor DarkGray

    Add-TransportTime $markoToken $id3 @{ transportType="WALK"; durationMinutes=120 } | Out-Null
    Write-Host "    Transport dodan" -ForegroundColor DarkGray

    Publish-Tour $markoToken $id3 | Out-Null
    Start-Sleep -Milliseconds 500
    Archive-Tour $markoToken $id3 | Out-Null
    Write-Host "  [OK] ARCHIVED: Mostar Stari Most (id=$id3)" -ForegroundColor Green
} catch {
    Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────
# TOUR 4: DRAFT — Sutjeska nacionalni park (u izradi)
# ─────────────────────────────────────────────────────────────
Write-Host "`n  [4/5] Kreiranje: Sutjeska NP (DRAFT)..." -ForegroundColor White
try {
    $t4 = New-Tour $markoToken @{
        name        = "Sutjeska - Proslost Perucica"
        description = "Trekking kroz prašumu Perucica i kanjon rijeke Sutjeske. Najzahtjevnija planinska tura u BiH."
        difficulty  = "HARD"
        tags        = @("trekking", "nature", "nationalpark", "sutjeska")
        lengthKm    = 18.5
    }
    $id4 = $t4.id
    Write-Host "  [OK] DRAFT: Sutjeska NP (id=$id4) - nema keypointa namjerno" -ForegroundColor Green
} catch {
    Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────
# TOUR 5: DRAFT sa keypointima — Vidikovac Igman (spreman za publish)
# ─────────────────────────────────────────────────────────────
Write-Host "`n  [5/5] Kreiranje: Igman vidikovac (DRAFT sa keypointima)..." -ForegroundColor White
try {
    $t5 = New-Tour $markoToken @{
        name        = "Igman - Vidikovac i Malo Polje"
        description = "Zimska/ljetna tura na planini Igman sa obilaskom olimpijskih objekata iz 1984. i panoramskog vidikovca."
        difficulty  = "MEDIUM"
        tags        = @("igman", "olympic", "mountain", "winter")
        lengthKm    = 9.0
    }
    $id5 = $t5.id
    Write-Host "    Tura kreirana, id=$id5" -ForegroundColor DarkGray

    Add-Keypoint $markoToken $id5 @{ lat=43.7421; lon=18.2874; name="Parkiranje Igman"; description="Polazna tačka ture, parking na platou Igmana."; orderIndex=0 } | Out-Null
    Add-Keypoint $markoToken $id5 @{ lat=43.7389; lon=18.2756; name="Malo polje"; description="Ravnica okružena šumom, idealna za odmor."; orderIndex=1 } | Out-Null
    Add-Keypoint $markoToken $id5 @{ lat=43.7301; lon=18.2680; name="Olimpijski vidikovac"; description="Pogled na Sarajevo i okolne planine."; orderIndex=2 } | Out-Null
    Add-TransportTime $markoToken $id5 @{ transportType="WALK"; durationMinutes=180 } | Out-Null
    Write-Host "  [OK] DRAFT (spreman za publish): Igman vidikovac (id=$id5)" -ForegroundColor Green
} catch {
    Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
}

# ══════════════════════════════════════════════════════════════════════════════
# STEP 4 — guide_ana kreira još 2 ture
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "`n=== Ture za guide_ana ===" -ForegroundColor Cyan

if ($anaToken) {
    # Ana - Tour 1: PUBLISHED
    Write-Host "`n  [1/2] Neretva rafting tura (PUBLISHED)..." -ForegroundColor White
    try {
        $ta1 = New-Tour $anaToken @{
            name        = "Neretva Rafting Avantura"
            description = "Uzbudljivi rafting po rijeci Neretvi kroz kanjon. Pogodno za početnike i iskusne raftere."
            difficulty  = "MEDIUM"
            tags        = @("rafting", "neretva", "adventure", "water")
            lengthKm    = 22.0
        }
        $ida1 = $ta1.id
        Add-Keypoint $anaToken $ida1 @{ lat=43.6901; lon=17.5891; name="Start rafting - Konjic"; description="Oprema i sigurnosni briefing."; orderIndex=0 } | Out-Null
        Add-Keypoint $anaToken $ida1 @{ lat=43.6012; lon=17.9134; name="Kanjon Neretve"; description="Najuzbudljiviji dio kanjonskog rafting prolaza."; orderIndex=1 } | Out-Null
        Add-Keypoint $anaToken $ida1 @{ lat=43.3361; lon=17.8151; name="Finish - Mostar"; description="Kraj ture u Mostaru, obilazak starog grada opciono."; orderIndex=2 } | Out-Null
        Add-TransportTime $anaToken $ida1 @{ transportType="BIKE"; durationMinutes=300 } | Out-Null
        Publish-Tour $anaToken $ida1 | Out-Null
        Write-Host "  [OK] PUBLISHED: Neretva Rafting (id=$ida1)" -ForegroundColor Green
    } catch {
        Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
    }

    # Ana - Tour 2: DRAFT
    Write-Host "`n  [2/2] Kravica vodopadi (DRAFT)..." -ForegroundColor White
    try {
        $ta2 = New-Tour $anaToken @{
            name        = "Kravica Vodopadi - Ljetni odmor"
            description = "Posjeta veličanstvenim Kravica vodopadima u blizini Ljubuškog. Kupanje i piknik u prirodi."
            difficulty  = "EASY"
            tags        = @("waterfall", "swimming", "nature", "kravica")
            lengthKm    = 3.0
        }
        $ida2 = $ta2.id
        Write-Host "  [OK] DRAFT: Kravica Vodopadi (id=$ida2)" -ForegroundColor Green
    } catch {
        Write-Host "  [!!] Greška: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# ══════════════════════════════════════════════════════════════════════════════
# STEP 5 — tourist_petar dodaje recenzije na published ture
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "`n=== Dodavanje recenzija (tourist_petar) ===" -ForegroundColor Cyan

if ($petarToken -and $id1 -and $id2) {
    $reviews = @(
        @{ tourId=$id1; rating=5; comment="Fantastična tura! Baščaršija je fenomenalna, vodič je bio odličan." },
        @{ tourId=$id2; rating=4; comment="Treking na Trebević je bio naporan ali vrijedan. Pogled je nevjerovatan!" }
    )

    foreach ($r in $reviews) {
        try {
            Invoke-Auth "POST" "$BASE/api/tours/$($r.tourId)/reviews" $petarToken @{
                rating    = $r.rating
                comment   = $r.comment
                visitDate = "2026-05-15"
                images    = @()
            } | Out-Null
            Write-Host "  [OK] Recenzija za turu $($r.tourId): $($r.rating)★" -ForegroundColor Green
        } catch {
            Write-Host "  [--] Recenzija: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# ══════════════════════════════════════════════════════════════════════════════
# SUMMARY
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "`n" + "="*60 -ForegroundColor Cyan
Write-Host "SEED ZAVRŠEN" -ForegroundColor Cyan
Write-Host "="*60 -ForegroundColor Cyan
Write-Host ""
Write-Host "KORISNICI:" -ForegroundColor Yellow
Write-Host "  admin_user    / Admin1234!  (role: admin)"
Write-Host "  guide_marko   / Guide1234!  (role: guide)"
Write-Host "  guide_ana     / Guide1234!  (role: guide)"
Write-Host "  tourist_petar / Tour1234!   (role: tourist)"
Write-Host "  tourist_maja  / Tour1234!   (role: tourist)"
Write-Host ""
Write-Host "TURE:" -ForegroundColor Yellow
Write-Host "  [PUBLISHED] Stari Grad Sarajevo       - guide_marko - EASY   - 4 keypointa"
Write-Host "  [PUBLISHED] Trebević planinska tura   - guide_marko - MEDIUM - 3 keypointa"
Write-Host "  [ARCHIVED]  Mostar - Stari Most       - guide_marko - EASY   - 3 keypointa"
Write-Host "  [DRAFT]     Sutjeska - Proslost       - guide_marko - HARD   - 0 keypointa (nije spremna)"
Write-Host "  [DRAFT]     Igman - Vidikovac         - guide_marko - MEDIUM - 3 keypointa (spremna za publish)"
Write-Host "  [PUBLISHED] Neretva Rafting           - guide_ana   - MEDIUM - 3 keypointa"
Write-Host "  [DRAFT]     Kravica Vodopadi          - guide_ana   - EASY   - 0 keypointa"
Write-Host ""
Write-Host "Login endpoint: POST $BASE/api/auth/login" -ForegroundColor DarkGray
Write-Host '  Body: { "username": "guide_marko", "password": "Guide1234!" }' -ForegroundColor DarkGray

