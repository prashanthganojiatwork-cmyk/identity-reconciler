# Extension Design: Identity Reconciler — Scale Architecture (HLD)

## Overview

This document describes how the POC Identity Reconciler scales to handle 200GB+ datasets and streaming workloads. Each section presents multiple approaches with tradeoffs, diagrams, and a recommended path.

This is a **design-only deliverable** — no code is built for this scope.

> For POC design (LLD), see [LLD.md](./LLD.md).

---

## POC → Extension Component Mapping

| POC Component | Extension Service | Key Change |
|---------------|------------------|------------|
| REST Controller (sync) | Job API + Query API | Async job submission |
| Normalizer (in-memory) | Normalizer Worker Fleet (ECS) | Reads from S3, writes to DynamoDB |
| Blocking Engine (HashMap) | LSH + OpenSearch | Distributed blocking index |
| Field Comparator (in-process loop) | Comparator Workers (ECS + SQS) | Pull pairs from queue |
| Scoring Engine (in-memory) | Embedded in Comparator Workers | Same interface, same logic |
| Explanation Builder (in-memory) | Embedded in Comparator Workers | Same interface, same logic |

---

## 1. Data Ingestion

**Problem:** The POC accepts records via a single synchronous REST call (max 10K records). At scale, we need to ingest 200GB+ datasets efficiently.

### Approach A: Batch File Upload (S3 + Trigger) ✓ PROPOSED

```mermaid
graph LR
    Client[Client] -->|Upload CSV/JSON| S3[S3 Bucket]
    S3 -->|S3 Event| Lambda[Lambda Trigger]
    Lambda -->|Start Job| StepFn[Step Functions]
    StepFn -->|Chunk & Distribute| NormWorkers[Normalizer Workers]
```

### Approach B: Streaming Ingestion (Kinesis)

```mermaid
graph LR
    Producer[Data Producer] -->|Put Records| Kinesis[Kinesis Data Stream]
    Kinesis -->|Shard 1| Consumer1[Normalizer 1]
    Kinesis -->|Shard 2| Consumer2[Normalizer 2]
    Kinesis -->|Shard N| ConsumerN[Normalizer N]
```

### Approach C: Hybrid (Batch + Streaming)

```mermaid
graph LR
    subgraph Batch Path
        S3[S3] -->|Large files| ChunkLambda[Chunk Lambda]
        ChunkLambda --> Kinesis
    end
    subgraph Streaming Path
        RealTime[Real-time API] -->|Single records| Kinesis[Kinesis]
    end
    Kinesis --> Workers[Normalizer Workers]
```

### Tradeoffs

| Criteria | A: Batch File (S3) | B: Streaming (Kinesis) | C: Hybrid |
|----------|-------------------|----------------------|-----------|
| Complexity | Low | Medium | High |
| Latency | Minutes (batch) | Seconds (per-record) | Both |
| Cost (200GB) | Low (S3 + Lambda) | Medium (Kinesis shards 24/7) | High |
| Backpressure | Natural (file-based) | Requires shard tuning | Mixed |
| Retry/replay | Easy (re-process file) | Kinesis retention window | Mixed |
| POC interface change | Minimal (add upload endpoint) | New producer SDK needed | Both |

**Recommendation: Approach A.** Simplest path from POC, matches the use case (reconcile two datasets = two files), low cost, easy retry. Streaming added later if real-time matching is needed.

---

## 2. Distributed Blocking

**Problem:** The POC uses in-memory HashMaps for blocking. At 200GB+, blocking keys and record groups don't fit in a single process.

### Approach A: Locality-Sensitive Hashing (LSH) + OpenSearch ✓ PROPOSED

```mermaid
graph TD
    Records[Normalized Records] --> LSH[LSH Hash Generator]
    LSH -->|MinHash signatures| OS[OpenSearch Index]
    OS -->|Similar band matches| PairGen[Pair Generator]
    PairGen --> Pairs[Candidate Pairs Queue]
```

### Approach B: Sorted Neighborhood on Partitioned Data

```mermaid
graph TD
    Records[Normalized Records] --> Sort[Sort by blocking key]
    Sort --> Partition1[Partition 1: A-F]
    Sort --> Partition2[Partition 2: G-M]
    Sort --> PartitionN[Partition N: N-Z]
    Partition1 --> Window1[Sliding Window Compare]
    Partition2 --> Window2[Sliding Window Compare]
    PartitionN --> WindowN[Sliding Window Compare]
```

### Approach C: Database-Backed Blocking (DynamoDB GSI)

```mermaid
graph TD
    Records[Normalized Records] --> DDB[DynamoDB Table]
    DDB -->|GSI: phonetic_key| Query1[Query Block 1]
    DDB -->|GSI: phone_suffix| Query2[Query Block 2]
    DDB -->|GSI: dob_year| Query3[Query Block 3]
    Query1 --> Merge[Merge Candidate Pairs]
    Query2 --> Merge
    Query3 --> Merge
```

### Tradeoffs

| Criteria | A: LSH + OpenSearch | B: Sorted Neighborhood | C: DynamoDB GSI |
|----------|--------------------|-----------------------|-----------------|
| Recall (true match rate) | High (tunable bands/rows) | Medium (fixed window) | Medium (exact key only) |
| Scalability | Excellent (horizontal) | Good (embarrassingly parallel) | Good (auto-scales) |
| Complexity | Medium (LSH tuning) | Low (sort + window) | Low (GSI queries) |
| Fuzzy matching | Native | Limited (key ordering) | None (exact keys) |
| Cost | Medium (OpenSearch cluster) | Low (compute only) | Medium (RCU/WCU) |
| Trestle stack fit | Strong (already using OpenSearch) | N/A | Partial |

**Recommendation: Approach A.** LSH provides tunable precision/recall tradeoff and supports fuzzy blocking — important for messy identity data. OpenSearch is already in Trestle's stack.

---

## 3. Parallel Matching

**Problem:** The POC compares pairs sequentially. At scale, millions of candidate pairs need parallel comparison.

### Approach A: SQS Worker Fleet ✓ PROPOSED

```mermaid
graph TD
    PairGen[Pair Generator] -->|Batch of pairs| SQS[SQS FIFO Queue]
    SQS --> W1[Comparator Worker 1]
    SQS --> W2[Comparator Worker 2]
    SQS --> WN[Comparator Worker N]
    W1 --> Results[Results Store]
    W2 --> Results
    WN --> Results
    
    subgraph Auto Scaling
        ASG[Auto Scaling Group] -.->|Scale on queue depth| W1
        ASG -.-> W2
        ASG -.-> WN
    end
```

### Approach B: EMR / MapReduce

```mermaid
graph TD
    Pairs[Candidate Pairs in S3] --> EMR[EMR Cluster]
    
    subgraph EMR Cluster
        Map[Map: Compare Pairs] --> Shuffle[Shuffle: Group by Source_A ID]
        Shuffle --> Reduce[Reduce: Score & Rank]
    end
    
    EMR --> Output[Results in S3]
```

### Approach C: Step Functions + Lambda Fan-Out

```mermaid
graph TD
    Orchestrator[Step Functions] -->|Distribute chunks| Map[Map State]
    Map --> L1[Lambda: Compare Chunk 1]
    Map --> L2[Lambda: Compare Chunk 2]
    Map --> LN[Lambda: Compare Chunk N]
    L1 --> Collect[Collect Results]
    L2 --> Collect
    LN --> Collect
    Collect --> Aggregate[Aggregate & Sort]
```

### Tradeoffs

| Criteria | A: SQS Workers | B: EMR / MapReduce | C: Step Functions + Lambda |
|----------|---------------|-------------------|---------------------------|
| Latency | Low (continuous) | High (cluster startup) | Medium (cold starts) |
| Scalability | Excellent (auto-scaling) | Excellent (add nodes) | Good (1000 concurrent) |
| Cost model | Pay per message + compute | Per-hour cluster | Pay per invocation |
| Complexity | Low (standard pattern) | Medium (EMR config) | Low (serverless) |
| Failure handling | Built-in DLQ | Task-level retry | Built-in retry + catch |
| Idle cost | Zero (scale to 0) | Non-zero (min cluster) | Zero |

**Recommendation: Approach A.** Proven pattern, auto-scales on queue depth, zero idle cost, built-in DLQ. Workers run the same `FieldComparator` + `ScoringEngine` interfaces from the POC.

---

## 4. Storage & State

**Problem:** The POC holds everything in memory. At scale, we need persistent storage for normalized records, blocking indices, and results.

### Approach A: DynamoDB ✓ PROPOSED

```mermaid
graph TD
    subgraph DynamoDB Tables
        NormTable[Normalized Records<br/>PK: source_id + record_id]
        BlockTable[Blocking Index<br/>PK: blocking_key, SK: record_id]
        ResultTable[Match Results<br/>PK: job_id, SK: source_a_id]
    end
    
    NormService[Normalizer] --> NormTable
    BlockService[Blocking Service] --> BlockTable
    ScoreService[Scoring Service] --> ResultTable
    QueryAPI[Query API] --> ResultTable
```

### Approach B: RDS (PostgreSQL)

```mermaid
graph TD
    subgraph RDS PostgreSQL
        NormSchema[normalized_records<br/>+ GIN indexes]
        BlockSchema[blocking_keys<br/>+ composite indexes]
        ResultSchema[match_results<br/>+ score indexes]
    end
    
    Services[All Services] --> RDS[RDS Read Replicas]
```

### Approach C: Redis Cluster (Hot) + S3 (Cold)

```mermaid
graph TD
    subgraph Hot [Redis Cluster - Active Jobs]
        Cache[Records + Blocking Keys + Scores]
    end
    subgraph Cold [S3 - Completed Jobs]
        Final[Final Results JSON]
    end
    Workers[Workers] --> Cache
    Cache -->|Job complete| Final
```

### Tradeoffs

| Criteria | A: DynamoDB | B: RDS (PostgreSQL) | C: Redis + S3 |
|----------|------------|--------------------|--------------| 
| Scalability | Excellent (auto) | Limited (vertical) | Excellent (sharding) |
| Query flexibility | Limited (key-based) | High (SQL, joins) | Limited (key-value) |
| Cost at 200GB | Medium (on-demand) | High (always-on) | High (RAM-priced) |
| Operational burden | Low (serverless) | Medium (patching) | Medium (cluster mgmt) |
| Latency | Single-digit ms | Low ms | Sub-ms |
| Durability | High (replicated) | High (Multi-AZ) | Low (volatile) |

**Recommendation: Approach A.** Serverless, scales without intervention, pay-per-use fits batch workloads. Key-access pattern matches our use case (lookup by blocking key, lookup by job ID).

---

## 5. Result Delivery

**Problem:** The POC returns results synchronously. At scale, processing takes minutes-to-hours.

### Approach A: Async Job + Polling API ✓ PROPOSED

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Job API
    participant Pipeline as Processing Pipeline
    participant Store as Result Store

    C->>API: POST /jobs (upload reference)
    API-->>C: 202 Accepted {jobId, status: "processing"}
    
    API->>Pipeline: Enqueue job
    Pipeline->>Store: Write results incrementally
    
    C->>API: GET /jobs/{jobId}/status
    API-->>C: {status: "processing", progress: "65%"}
    
    C->>API: GET /jobs/{jobId}/results?page=1
    API-->>C: {matches: [...], nextPage: 2}
```

### Approach B: Webhook Callback

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Job API
    participant Pipeline as Processing
    participant Hook as Client Webhook

    C->>API: POST /jobs {callbackUrl: "https://..."}
    API-->>C: 202 Accepted {jobId}
    Pipeline->>Hook: POST {jobId, status: "completed"}
    C->>API: GET /jobs/{jobId}/results
```

### Approach C: S3 Result File + SNS Notification

```mermaid
graph LR
    Pipeline[Pipeline] -->|Write| S3[S3: results/jobId.json]
    Pipeline -->|Notify| SNS[SNS Topic]
    SNS --> Client[Client Subscription]
    Client -->|Download| S3
```

### Tradeoffs

| Criteria | A: Polling API | B: Webhook | C: S3 + SNS |
|----------|---------------|-----------|-------------|
| Client complexity | Low (GET calls) | Medium (host endpoint) | Medium (SNS sub) |
| Real-time push | No (interval) | Yes | Yes |
| Reliability | High (client retries) | Medium (delivery issues) | High (S3 durable) |
| Large results | Good (paginated) | Poor (payload limits) | Good (file download) |
| Firewall friendly | Excellent (outbound) | Poor (inbound) | Good |

**Recommendation: Approach A.** Simplest for clients, paginated results handle large output, progress tracking built-in. Webhook can be added later as optional notification layer.

---

## Composed Extension Architecture

```mermaid
graph TD
    subgraph Ingestion [1. Batch Ingestion - S3]
        Client[Client] -->|Upload files| S3[S3 Bucket]
        S3 -->|Event| Lambda[Chunk Lambda]
        Lambda --> NormWorkers[Normalizer Workers]
    end
    
    subgraph Blocking [2. Distributed Blocking - LSH + OpenSearch]
        NormWorkers -->|Normalized records| DDB_Norm[DynamoDB]
        NormWorkers -->|LSH signatures| OS[OpenSearch]
        OS -->|Candidate pairs| PairGen[Pair Generator]
    end
    
    subgraph Matching [3. Parallel Matching - SQS Workers]
        PairGen --> SQS[SQS Queue]
        SQS --> CW1[Worker 1]
        SQS --> CW2[Worker 2]
        SQS --> CWN[Worker N]
    end
    
    subgraph Results [4. Storage & Delivery - DynamoDB + Polling]
        CW1 --> DDB_R[DynamoDB Results]
        CW2 --> DDB_R
        CWN --> DDB_R
        DDB_R --> QueryAPI[Query API]
        QueryAPI --> ClientPoll[Client Polls]
    end
```

---

## Operational Concerns

| Concern | Approach |
|---------|----------|
| Service discovery | AWS ECS Service Connect (or ALB for HTTP) |
| Retry policy | SQS visibility timeout + DLQ after 3 retries |
| Circuit breaker | Resilience4j on OpenSearch calls |
| Observability | X-Ray distributed tracing + CloudWatch metrics |
| Backpressure | SQS queue depth triggers auto-scaling; S3 upload rate naturally bounded |
| Cost optimization | DynamoDB on-demand (pay per request), ECS Fargate Spot for workers |
