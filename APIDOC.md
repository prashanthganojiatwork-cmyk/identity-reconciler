# API Documentation - Identity Reconciler

## Base URL

```
http://localhost:8080
```

Production: `http://<elastic-ip>:8080`

## Quick Links

| Page | URL |
|------|-----|
| Reconciliation Form | [/ui/input](http://localhost:8080/ui/input) |
| Sample Datasets | [/ui/datasets](http://localhost:8080/ui/datasets) |

---

## REST API

### POST /api/v1/reconcile

Reconcile two sets of person records and return scored match candidates.

#### Request

**Content-Type:** `application/json`  
**Max payload size:** 10 MB

```json
{
  "sourceA": [
    {
      "id": "1",
      "firstname": "John",
      "middlename": null,
      "lastname": "Smith",
      "alternateNames": null,
      "dateOfBirth": "1990-05-15",
      "phone": "2065551234",
      "email": "john.smith@gmail.com",
      "address": {
        "streetLine1": "100 Main St",
        "streetLine2": null,
        "city": "Seattle",
        "stateCode": "WA",
        "postalCode": "98101",
        "countryCode": null
      }
    }
  ],
  "sourceB": [
    {
      "id": "B1",
      "firstname": "Jon",
      "lastname": "Smith",
      "phone": "(206) 555-1234",
      "email": "john.smith@gmail.com",
      "dateOfBirth": "05/15/1990",
      "address": {
        "streetLine1": "100 Main Street",
        "city": "Seattle",
        "stateCode": "WA",
        "postalCode": "98101"
      }
    }
  ],
  "thresholds": {
    "matchThreshold": 0.7,
    "reviewBandLowerBound": 0.4
  }
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sourceA` | Array of PersonRecord | Yes | First dataset (1–10,000 records) |
| `sourceB` | Array of PersonRecord | Yes | Second dataset (1–10,000 records) |
| `thresholds` | ThresholdConfig | No | Override default thresholds |

**PersonRecord fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Unique identifier within the source |
| `firstname` | String | No | Given name |
| `middlename` | String | No | Middle name |
| `lastname` | String | No | Family name |
| `alternateNames` | Array of String | No | Other known names |
| `dateOfBirth` | String | No | DOB in any format (YYYY-MM-DD, MM/DD/YYYY, DD-MM-YYYY, M/D/YYYY, Month DD YYYY) |
| `phone` | String | No | Phone in any format (digits, dashes, parens, dots, +prefix) |
| `email` | String | No | Email address |
| `address` | Address | No | Structured address |

**Address fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `streetLine1` | String | No | Street address line 1 |
| `streetLine2` | String | No | Apt/Suite/Unit |
| `city` | String | No | City |
| `stateCode` | String | No | State code (2-letter) |
| `postalCode` | String | No | ZIP/postal code |
| `countryCode` | String | No | Country code |

**ThresholdConfig fields:**

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `matchThreshold` | Double | 0.7 | Score at or above = positive match |
| `reviewBandLowerBound` | Double | 0.4 | Score below this = excluded from results |

#### Successful Response (200 OK)

```json
{
  "jobId": "a2e8a803-0679-4c78-98f1-2777347e72de",
  "metadata": {
    "sourceACount": 4,
    "sourceBCount": 4,
    "matchesFound": 3,
    "flaggedForReview": 0,
    "processingDurationMs": 12
  },
  "matches": [
    {
      "sourceARecordId": "1",
      "sourceBRecordId": "B1",
      "confidenceScore": 0.98,
      "status": "match",
      "requiresReview": false,
      "explanation": {
        "summary": "Strong match: lastName, phone agree with high similarity.",
        "fieldBreakdowns": [
          {
            "fieldName": "firstName",
            "rawValueA": "John",
            "rawValueB": "Jon",
            "normalizationApplied": "Lowercase, whitespace collapse, nickname resolution",
            "similarityMethod": "Phonetic match (Soundex): john ↔ jon",
            "similarityResult": 0.85,
            "scoreContribution": 0.085,
            "present": true
          },
          {
            "fieldName": "lastName",
            "rawValueA": "Smith",
            "rawValueB": "Smith",
            "normalizationApplied": "Lowercase, whitespace collapse, retain hyphens/apostrophes",
            "similarityMethod": "Exact match",
            "similarityResult": 1.0,
            "scoreContribution": 0.15,
            "present": true
          }
        ],
        "totalScore": 0.98,
        "ambiguous": false,
        "ambiguityReasons": []
      }
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | String | UUID identifying this reconciliation run |
| `metadata.sourceACount` | Integer | Records received in Source A |
| `metadata.sourceBCount` | Integer | Records received in Source B |
| `metadata.matchesFound` | Integer | Total candidates returned |
| `metadata.flaggedForReview` | Integer | Candidates in review band |
| `metadata.processingDurationMs` | Long | Processing time in milliseconds |
| `matches` | Array | Match candidates sorted by score descending (max 100 per Source A record) |

**MatchCandidate fields:**

| Field | Type | Description |
|-------|------|-------------|
| `sourceARecordId` | String | ID from Source A record |
| `sourceBRecordId` | String | ID from Source B record |
| `confidenceScore` | Double | 0.0–1.0, rounded to 2 decimal places |
| `status` | String | `"match"` (≥ threshold), `"review"` (in review band), or `"ambiguous"` (review band + conflicting fields) |
| `requiresReview` | Boolean | True if score < matchThreshold |
| `explanation` | Object | Detailed breakdown |

**Match status logic:**

| Score Range | Conflicting Fields? | Status |
|-------------|-------------------|--------|
| ≥ matchThreshold (0.7) | N/A | `match` |
| reviewBand to threshold (0.4–0.7) | No | `review` |
| reviewBand to threshold (0.4–0.7) | Yes | `ambiguous` |
| < reviewBandLowerBound (0.4) | N/A | Excluded from response |

---

## Error Responses

All errors return a structured JSON body:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable description",
  "fieldErrors": null
}
```

| HTTP Status | Code | Trigger |
|-------------|------|---------|
| 400 | `EMPTY_DATASET` | sourceA or sourceB is null/empty |
| 400 | `VALIDATION_ERROR` | Source exceeds 10,000 records, or malformed JSON |
| 400 | `INVALID_THRESHOLD` | Threshold outside [0.0, 1.0], or reviewBand ≥ matchThreshold |
| 413 | `PAYLOAD_TOO_LARGE` | Request body exceeds 10 MB |
| 500 | `INTERNAL_ERROR` | Unexpected server error (no internal details exposed) |

---

## Web UI

### Pages

| Path | Method | Description |
|------|--------|-------------|
| `/ui/input` | GET | Input form - paste JSON for Source A and Source B, configure thresholds, submit |
| `/ui/datasets` | GET | Sample datasets - click to load pre-built test data into the input form |
| `/ui/reconcile` | POST | Form submission handler - processes input, redirects to results |
| `/ui/results/{jobId}` | GET | Results overview - table of match candidates with filtering |
| `/ui/results/{jobId}/match/{index}` | GET | Match detail - side-by-side comparison, per-field breakdown, score gauge |

### UI Flow

```
/ui/input → (submit form) → /ui/reconcile → (redirect) → /ui/results/{jobId}
                                                                ↓
                                                    /ui/results/{jobId}/match/{index}
```

### Sample Datasets Page

Navigate to `/ui/datasets` to load pre-built test data covering:

1. Typos, formatting, and nickname resolution
2. Asymmetric sources (different population sizes)
3. One-to-many and many-to-many matches
4. Life changes (marriage name change, address moves)
5. Conflict scenarios (same phone, different DOB)
6. Partial/missing data (weight redistribution)
7. Kitchen sink (all scenarios combined)
8. Duplicates and near-duplicates within/across sources

---

## Field Normalization Rules

The API normalizes fields before comparison:

| Field | Normalization Applied |
|-------|---------------------|
| Name | Lowercase, collapse whitespace, trim, remove periods, retain hyphens/apostrophes |
| Phone | Strip all non-digits, prepend "1" if 10 digits |
| Email | Lowercase, remove "+" and everything after it before "@" |
| DOB | Parse multiple formats → ISO 8601 (YYYY-MM-DD) |
| Address | Expand USPS abbreviations (St→Street, Ave→Avenue, etc.), lowercase, collapse whitespace |

**Supported DOB formats:** `YYYY-MM-DD`, `MM/DD/YYYY`, `DD-MM-YYYY`, `M/D/YYYY`, `YYYY/MM/DD`, `Month DD, YYYY`

---

## Scoring Weights

Default field weights (total = 1.0):

| Field | Weight | Category |
|-------|--------|----------|
| Phone | 0.25 | High-entropy |
| Email | 0.20 | High-entropy |
| DOB | 0.20 | High-entropy |
| Last Name | 0.15 | Low-entropy |
| First Name | 0.10 | Low-entropy |
| Address | 0.10 | Low-entropy |

High-entropy fields collectively contribute 65% of the score.

**Missing field handling:** If a field is absent in either record, it's excluded from scoring and its weight is redistributed proportionally among present fields.

**Conflict cap:** If exactly one high-entropy field ≥ 0.8 AND another field < 0.3, the score is capped at 0.6.

---

## cURL Examples

**Basic reconciliation:**

```bash
curl -X POST http://localhost:8080/api/v1/reconcile \
  -H "Content-Type: application/json" \
  -d '{
    "sourceA": [{"id":"1","firstname":"John","lastname":"Smith","phone":"2065551234","email":"john@example.com","dateOfBirth":"1990-05-15"}],
    "sourceB": [{"id":"B1","firstname":"Jon","lastname":"Smith","phone":"(206) 555-1234","email":"john@example.com","dateOfBirth":"05/15/1990"}]
  }'
```

**With custom thresholds:**

```bash
curl -X POST http://localhost:8080/api/v1/reconcile \
  -H "Content-Type: application/json" \
  -d '{
    "sourceA": [{"id":"1","firstname":"John","lastname":"Smith","phone":"2065551234"}],
    "sourceB": [{"id":"B1","firstname":"Jon","lastname":"Smith","phone":"2065551234"}],
    "thresholds": {"matchThreshold": 0.8, "reviewBandLowerBound": 0.5}
  }'
```

**Error case (empty source):**

```bash
curl -X POST http://localhost:8080/api/v1/reconcile \
  -H "Content-Type: application/json" \
  -d '{"sourceA": [], "sourceB": [{"id":"1","firstname":"John"}]}'
# → 400: {"code":"EMPTY_DATASET","message":"Source A contains zero records","fieldErrors":null}
```
