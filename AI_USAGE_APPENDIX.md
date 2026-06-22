# AI Usage Appendix

## AI Usage and Context

### Where AI Was NOT Used

- **System design and architecture decisions.** The interface-driven component architecture, the pipeline structure (normalize → block → compare → score → explain), the choice of Spring Boot, the component boundary design — all designed independently before any AI involvement. These decisions come from experience with clean architecture principles.

- **Requirements analysis and specification.** The requirements document was written based on my understanding of the problem domain, stakeholder needs, and the POC/scale scoping decision.

- **Technology selection.** Choosing Java/Spring Boot, Gradle, ECS deployment, GitHub Actions CI/CD — all made based on the constraints (free tier, existing stack familiarity, time budget).

### Where AI Was Used

#### 1. Algorithm Research: Blocking and Matching Strategies

Blocking key generation and field similarity algorithms were new territory for me. I used AI to explore:

- **Which blocking key strategies are industry-standard** — learned about Soundex, Metaphone, sorted neighborhood, LSH, canopy clustering. Used this to select our four generators (phonetic name, phone suffix, DOB year, initial+month).
- **Similarity algorithms for name matching** — explored Jaro-Winkler, Levenshtein, Soundex, Metaphone, and how to combine them in a priority cascade (exact → nickname → phonetic → JW → initial → partial).
- **How conflict cap works in entity resolution** — the pattern of capping scores when high-entropy fields disagree is documented in Fellegi-Sunter literature, but I needed AI to help me understand the implementation mechanics.

*Notable prompt:* "What are the industry standard blocking keys used in entity resolution?"

**My judgment applied:** I chose *which* algorithms to use and *how to weight them* based on our specific data characteristics (messy names, varied phone formats, missing fields). The AI provided the menu; I picked from it.

#### 2. Extension Design: Exploring Scale Approaches

For the scale architecture (HLD.md), I used AI to explore alternate approaches:

- **S3 vs Kinesis vs Hybrid for ingestion** — AI helped articulate tradeoffs (cost, latency, retry semantics). The decision to go batch-first (S3) was mine based on the use case being inherently batch-oriented.
- **LSH vs DynamoDB GSI vs Sorted Neighborhood for distributed blocking** — AI helped me understand how OpenSearch k-NN works under the hood (it's not literally LSH, it's HNSW-based approximate nearest neighbor). This corrected my initial design doc language.
- **SQS worker fleet vs EMR vs Lambda fan-out** — explored operational characteristics of each. Chose SQS workers for zero-idle-cost and simplicity.

**My judgment applied:** All final architecture decisions (which approach to propose, which to list as alternatives) were mine. AI provided deeper technical context I used to validate and refine my choices.

#### 3. Design-to-Task Breakdown

I used AI to convert the design document into an actionable task list. The design was already complete — the AI's role was mechanical: decompose each component's design into implementable subtasks with clear acceptance criteria and requirement traceability.

I reviewed and adjusted the task ordering, dependencies, and scope of each task. Several tasks were restructured to follow an interface-first approach (define interface → implement → test) rather than the bottom-up ordering AI initially suggested.

#### 4. Implementation (Coding)

AI was used as a coding accelerator for:

- **Boilerplate generation** — record/DTO classes, Spring configuration, controller scaffolding
- **Algorithm implementation** — translating the Jaro-Winkler, Soundex, and Jaccard algorithms into Java code based on my design specifications
- **Template/UI work** — Thymeleaf templates, CSS styling, JavaScript for the sample datasets page
- **Test data generation** — creating realistic sample datasets covering all matching scenarios

**My judgment applied throughout:** I reviewed all generated code for correctness, caught and fixed a significant scoring bug (missing fields were being penalized as 0.0 similarity instead of excluded), validated the blocking logic against hand-traced examples, and directed architectural decisions that the AI couldn't make (e.g., how to handle one-sided missing fields per the requirements).

#### 5. Documentation Touch-up

After the core system was complete, AI helped with:

- Formatting and structuring the markdown docs
- Expanding the README with deployment instructions
- Generating cURL examples for the API documentation

## What tasks were Deferred

| Item | Reason | Priority for Production |
|------|--------|----------------------|
| Property-based tests (jqwik) | Time constraint — focused on core logic + integration | High — catches edge cases manual tests miss |
| ArchUnit architecture tests | Would enforce package boundaries in CI | Medium |
| Swagger/OpenAPI auto-generation | Manual APIDOC.md covers current needs | Low |
| Learned scoring weights | Requires labeled training data we don't have yet | High for production |
| Enhanced blocking (compound keys) | POC scale doesn't need it | High at scale |
| Rate limiting / auth | POC is internal-only | Required before production exposure |
| Result persistence | In-memory store clears on restart | High |

I would have complete these deferred items given extra time.

##  Summary
AI accelerated my implementation significantly, what might have taken 1 week of coding was compressed into 1 day. But the intellectual work (design, architecture, algorithm selection, trade-off decisions, bug identification) remained mine. I used AI the way I'd use a very fast junior engineer: great for execution velocity, but needs direction and review on every non-trivial decision.

The areas where AI provided the most value were algorithm research (blocking/matching strategies I hadn't implemented before) and mechanical code generation (DTOs, templates, test data). The areas where it required the most correction were scoring edge cases and architectural decisions that require understanding the full system context.
