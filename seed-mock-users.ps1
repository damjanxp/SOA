################################################################################
# seed-mock-users.ps1
# Creates 20 mock users via the gateway, then sets up follow relationships
# so that friends-of-friends recommendations can be tested.
#
# Usage: .\seed-mock-users.ps1
################################################################################

$BASE = "http://localhost:8080"

$users = @(
    @{ username = "alice";    email = "alice@test.com";    password = "Test1234!"; role = "tourist" },
    @{ username = "bob";      email = "bob@test.com";      password = "Test1234!"; role = "tourist" },
    @{ username = "charlie";  email = "charlie@test.com";  password = "Test1234!"; role = "tourist" },
    @{ username = "diana";    email = "diana@test.com";    password = "Test1234!"; role = "tourist" },
    @{ username = "eve";      email = "eve@test.com";      password = "Test1234!"; role = "tourist" },
    @{ username = "frank";    email = "frank@test.com";    password = "Test1234!"; role = "guide" },
    @{ username = "grace";    email = "grace@test.com";    password = "Test1234!"; role = "guide" },
    @{ username = "henry";    email = "henry@test.com";    password = "Test1234!"; role = "tourist" },
    @{ username = "iris";     email = "iris@test.com";     password = "Test1234!"; role = "tourist" },
    @{ username = "jack";     email = "jack@test.com";     password = "Test1234!"; role = "tourist" },
    @{ username = "kate";     email = "kate@test.com";     password = "Test1234!"; role = "guide" },
    @{ username = "liam";     email = "liam@test.com";     password = "Test1234!"; role = "tourist" },
    @{ username = "mia";      email = "mia@test.com";      password = "Test1234!"; role = "tourist" },
    @{ username = "noah";     email = "noah@test.com";     password = "Test1234!"; role = "tourist" },
    @{ username = "olivia";   email = "olivia@test.com";   password = "Test1234!"; role = "tourist" },
    @{ username = "peter";    email = "peter@test.com";    password = "Test1234!"; role = "tourist" },
    @{ username = "quinn";    email = "quinn@test.com";    password = "Test1234!"; role = "tourist" },
    @{ username = "rachel";   email = "rachel@test.com";   password = "Test1234!"; role = "tourist" },
    @{ username = "sam";      email = "sam@test.com";      password = "Test1234!"; role = "guide" },
    @{ username = "tina";     email = "tina@test.com";     password = "Test1234!"; role = "tourist" }
)

# ── Step 1: Register all users ────────────────────────────────────────────────
Write-Host "`n=== Registering users ===" -ForegroundColor Cyan

$tokens = @{}

foreach ($u in $users) {
    try {
        $body = $u | ConvertTo-Json
        $resp = Invoke-RestMethod -Uri "$BASE/api/auth/register" `
                                  -Method POST `
                                  -ContentType "application/json" `
                                  -Body $body `
                                  -ErrorAction Stop
        Write-Host "  [OK] Registered $($u.username)" -ForegroundColor Green
    } catch {
        $msg = $_.Exception.Message
        Write-Host "  [--] $($u.username) already exists or error: $msg" -ForegroundColor Yellow
    }
}

# ── Step 2: Login all users and collect tokens ────────────────────────────────
Write-Host "`n=== Logging in users ===" -ForegroundColor Cyan

foreach ($u in $users) {
    try {
        $body = @{ username = $u.username; password = $u.password } | ConvertTo-Json
        $resp = Invoke-RestMethod -Uri "$BASE/api/auth/login" `
                                  -Method POST `
                                  -ContentType "application/json" `
                                  -Body $body `
                                  -ErrorAction Stop
        $tokens[$u.username] = $resp.token
        Write-Host "  [OK] Logged in $($u.username)" -ForegroundColor Green
    } catch {
        Write-Host "  [!!] Failed login for $($u.username): $($_.Exception.Message)" -ForegroundColor Red
    }
}

# ── Step 3: Resolve user IDs from token JWT payload ───────────────────────────
Write-Host "`n=== Resolving user IDs ===" -ForegroundColor Cyan

$userIds = @{}

foreach ($u in $users) {
    $token = $tokens[$u.username]
    if (-not $token) { continue }

    try {
        $parts  = $token.Split('.')
        $pad    = 4 - ($parts[1].Length % 4)
        $b64    = $parts[1] + ('=' * ($pad % 4))
        $json   = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
        $payload = $json | ConvertFrom-Json
        $userIds[$u.username] = $payload.userId
        Write-Host "  $($u.username) => $($payload.userId)" -ForegroundColor DarkGray
    } catch {
        Write-Host "  [!!] Could not decode token for $($u.username)" -ForegroundColor Red
    }
}

# ── Step 4: Create follow relationships ──────────────────────────────────────
# Graph layout (who follows whom):
#
#  Cluster A: alice, bob, charlie, diana, eve
#    alice   -> bob, charlie, diana
#    bob     -> charlie, eve
#    charlie -> diana, alice
#    diana   -> eve, bob
#    eve     -> alice, charlie
#
#  Cluster B: frank, grace, henry, iris, jack
#    frank   -> grace, henry
#    grace   -> henry, iris, jack
#    henry   -> jack, frank
#    iris    -> frank, grace
#    jack    -> iris, henry
#
#  Cluster C: kate, liam, mia, noah, olivia
#    kate    -> liam, mia
#    liam    -> mia, noah, olivia
#    mia     -> noah, kate
#    noah    -> olivia, liam
#    olivia  -> kate, mia
#
#  Cross-cluster bridges (create friends-of-friends between clusters):
#    peter   -> alice, frank, kate        (bridge across all 3 clusters)
#    quinn   -> bob, grace, liam
#    rachel  -> charlie, henry, mia
#    sam     -> diana, iris, noah
#    tina    -> eve, jack, olivia
#    alice   -> peter
#    frank   -> quinn
#    kate    -> rachel
#
Write-Host "`n=== Creating follow relationships ===" -ForegroundColor Cyan

$follows = @(
    # Cluster A
    @("alice",   "bob"),
    @("alice",   "charlie"),
    @("alice",   "diana"),
    @("bob",     "charlie"),
    @("bob",     "eve"),
    @("charlie", "diana"),
    @("charlie", "alice"),
    @("diana",   "eve"),
    @("diana",   "bob"),
    @("eve",     "alice"),
    @("eve",     "charlie"),
    # Cluster B
    @("frank",   "grace"),
    @("frank",   "henry"),
    @("grace",   "henry"),
    @("grace",   "iris"),
    @("grace",   "jack"),
    @("henry",   "jack"),
    @("henry",   "frank"),
    @("iris",    "frank"),
    @("iris",    "grace"),
    @("jack",    "iris"),
    @("jack",    "henry"),
    # Cluster C
    @("kate",    "liam"),
    @("kate",    "mia"),
    @("liam",    "mia"),
    @("liam",    "noah"),
    @("liam",    "olivia"),
    @("mia",     "noah"),
    @("mia",     "kate"),
    @("noah",    "olivia"),
    @("noah",    "liam"),
    @("olivia",  "kate"),
    @("olivia",  "mia"),
    # Bridge users (peter, quinn, rachel, sam, tina)
    @("peter",   "alice"),
    @("peter",   "frank"),
    @("peter",   "kate"),
    @("quinn",   "bob"),
    @("quinn",   "grace"),
    @("quinn",   "liam"),
    @("rachel",  "charlie"),
    @("rachel",  "henry"),
    @("rachel",  "mia"),
    @("sam",     "diana"),
    @("sam",     "iris"),
    @("sam",     "noah"),
    @("tina",    "eve"),
    @("tina",    "jack"),
    @("tina",    "olivia"),
    # Back-links so bridges get followers too
    @("alice",   "peter"),
    @("frank",   "quinn"),
    @("kate",    "rachel")
)

$ok  = 0
$err = 0

foreach ($pair in $follows) {
    $follower = $pair[0]
    $target   = $pair[1]

    $token    = $tokens[$follower]
    $targetId = $userIds[$target]

    if (-not $token -or -not $targetId) {
        Write-Host "  [SKIP] $follower -> $target (missing token or id)" -ForegroundColor DarkYellow
        continue
    }

    try {
        Invoke-RestMethod -Uri "$BASE/api/followers/follow/$targetId" `
                          -Method POST `
                          -Headers @{ Authorization = "Bearer $token" } `
                          -ContentType "application/json" `
                          -Body "{}" `
                          -ErrorAction Stop | Out-Null
        Write-Host "  [OK] $follower -> $target" -ForegroundColor Green
        $ok++
    } catch {
        Write-Host "  [!!] $follower -> $target : $($_.Exception.Message)" -ForegroundColor Red
        $err++
    }
}

Write-Host "`n=== Done! $ok follows created, $err errors ===" -ForegroundColor Cyan
Write-Host "Log in as any user (password: Test1234) to test recommendations." -ForegroundColor White
Write-Host "Tip: Log in as 'peter' - he follows alice/frank/kate across all clusters" -ForegroundColor Yellow
Write-Host ""

