# Identity Reconciler - POC

## Overview
The Identity Reconciler is a record linkage service that identifies matching person records between two data sources containing messy, inconsistent identity data. It produces confidence-scored match candidates with human-readable explanations.

The POC is a single-process, in-memory Java Spring Boot service deployed on AWS ECS free tier, handling datasets up to 10k records per source. Internal components have clean interfaces and module boundaries, making each component extractable as an independent service.

## System Architecture
[![](https://mermaid.ink/img/pako:eNqNlE1vozAQhv-K5XM--WrCodI2zUo9dBslPRX24MWzwVpjI2Oqtkn--w44KSRbRcsBPK_9zAwzDDuaaQ40plvDypw836eK4LWQApRN3IOMybfVA1loVdUFmJ9kOLzdr542z2TMSjF-nY4NZFplQsK-OZmsl7iHx63RUiLgfLp7Vf9yodYnxmzAvIoMSPLAMZqw7709MiQbobYSyMroDKrq6Ky5mqQwFfJkshwqa5jVJjmhglmh1dleD-3LrY8f2hRMig8wSbe8BtxJnf3BzJZqKxQkJ5M4-xr5XYDkC12UzGXc2qQTrrGbTJsu6NH6j5jLt1Iy1ZbkrhaS42v2JHLUjh5A8X7HunqQ4Qg7_2nzptaG7y9q4ahzzZELprjgzMKKCVPtLyvhwAvRkT1RVFqtoaqlRQ9n9XD8meToRgL-yGyW778oheP-1R3cYp-Ztx94qugAB0ZwGltTw4DiVBSsMemucZZSm0MBKY1xqaDGRsiUpuqAWMnUi9bFiTS63uYnoy6bEPeC4YR0J7AdYBa6VpbGQeS3Lmi8o2809r1g5HnTcBbMgjCcz6YD-k7jaBR48yj0vHkYeVGAy8OAfrRBJ6O5799EXjiZ3USBP_HQHXCBdX50vwEcnt9iSw9_AQz8Xuw?type=png)](https://mermaid.ai/live/edit#pako:eNqNlE1vozAQhv-K5XM--WrCodI2zUo9dBslPRX24MWzwVpjI2Oqtkn--w44KSRbRcsBPK_9zAwzDDuaaQ40plvDypw836eK4LWQApRN3IOMybfVA1loVdUFmJ9kOLzdr542z2TMSjF-nY4NZFplQsK-OZmsl7iHx63RUiLgfLp7Vf9yodYnxmzAvIoMSPLAMZqw7709MiQbobYSyMroDKrq6Ky5mqQwFfJkshwqa5jVJjmhglmh1dleD-3LrY8f2hRMig8wSbe8BtxJnf3BzJZqKxQkJ5M4-xr5XYDkC12UzGXc2qQTrrGbTJsu6NH6j5jLt1Iy1ZbkrhaS42v2JHLUjh5A8X7HunqQ4Qg7_2nzptaG7y9q4ahzzZELprjgzMKKCVPtLyvhwAvRkT1RVFqtoaqlRQ9n9XD8meToRgL-yGyW778oheP-1R3cYp-Ztx94qugAB0ZwGltTw4DiVBSsMemucZZSm0MBKY1xqaDGRsiUpuqAWMnUi9bFiTS63uYnoy6bEPeC4YR0J7AdYBa6VpbGQeS3Lmi8o2809r1g5HnTcBbMgjCcz6YD-k7jaBR48yj0vHkYeVGAy8OAfrRBJ6O5799EXjiZ3USBP_HQHXCBdX50vwEcnt9iSw9_AQz8Xuw)

## Component Responsibilities

### Component Responsibilities

| Component | Responsibility | Input | Output |
|-----------|---------------|-------|--------|
| REST Controller | Request validation, error responses | HTTP Request | HTTP Response |
| Orchestrator | Pipeline coordination | ReconciliationRequest | ReconciliationResult |
| Normalizer | Field canonicalization | PersonRecord | NormalizedRecord |
| Blocking Engine | Candidate pair reduction | NormalizedRecord[] × 2 | CandidatePair[] |
| Field Comparator | Per-field similarity | NormalizedRecord × 2 | FieldComparisonResult |
| Scoring Engine | Weighted aggregation | FieldComparisonResult | ScoredMatch |
| Explanation Builder | Human-readable output | ScoredMatch | MatchExplanation |