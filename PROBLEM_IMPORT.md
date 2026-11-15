# Automated Problem Import (Hugging Face dataset)

AlgoArena no longer depends on manual admin problem creation alone: the backend now
imports problems automatically from the public Hugging Face dataset
[`sigcp/hardtests_problems`](https://huggingface.co/datasets/sigcp/hardtests_problems).
Everything lives in the self-contained module `com.problemsolving.platform.importing`
and is completely separated from the normal problem-serving APIs (`/api/problems/**`).

## How it works

```
ProblemImportScheduler                 ProblemImportService
  ├─ startup: initial import  ───────►  1. atomic single-run lock (DB conditional update)
  └─ quarterly cron                      2. storage check (DatabaseStorageService)
                                         3. loop: fetch batch -> map -> dedupe
RestHuggingFaceProblemClient  ◄──────►     -> storage recheck -> save batch (1 tx)
  (datasets-server /rows API,             4. advance offset only after a saved batch
   max 100 rows per request)              5. COMPLETED / STORAGE_FULL / FAILED
```

- **Initial import** – the first `initial-import-count` (default 500) problems are imported
  once, when the application starts and the import system is initialized. A first import
  interrupted by a crash/restart resumes automatically on the next startup.
- **Quarterly import** – every 3 months (cron `0 0 3 1 1,4,7,10 *`, i.e. Jan/Apr/Jul/Oct 1st
  at 03:00) the next `import-count-per-run` (default 500) problems are imported.
- **Never the whole dataset locally** – problems are fetched through the Hugging Face
  `datasets-server` rows API in batches; nothing is downloaded or stored as a file.
- **Duplicate prevention** – every imported problem stores its dataset identity in
  `problems.external_id` (e.g. `hf:sigcp/hardtests_problems:atcoder_1202contest_a`,
  unique). A batch drops rows already stored, rows repeated inside the batch and rows
  that can never be served (no pid/title/content/test cases).
- **Progress tracking** – the single row in `problem_import_state` persists the dataset
  offset (`last_offset`) and total imported count. The offset is advanced only AFTER a
  batch was saved in its own transaction, so any interruption (error, storage full,
  restart) loses nothing and the next run resumes exactly at the last safe offset.
- **Single-run guarantee** – `tryAcquire` is an atomic `UPDATE ... WHERE status IS NULL
  OR status <> 'RUNNING'`; concurrent triggers (schedule, manual, restart) can never run
  two import jobs at the same time. A run left `RUNNING` by a crash is marked `FAILED`
  on startup (offset untouched) so the lock can never deadlock.

### Import statuses (`ProblemImportStatus`)

| Status         | Meaning |
|----------------|---------|
| `RUNNING`      | a run currently holds the import lock |
| `COMPLETED`    | run finished (target reached or dataset exhausted) |
| `STORAGE_FULL` | run stopped because the database storage threshold was reached |
| `FAILED`       | run stopped after an unexpected error (HF API failure, DB error, ...) |

`problem_import_state.status` is `null` until the very first run starts.

## Storage-full detection (configurable, provider-independent)

```yaml
app:
  problem-import:
    storage:
      max-limit-mb: 2048          # maximum DB storage for the platform
      threshold-percentage: 50    # stop importing at 50% of the limit (=> 1024 MB)
```

The threshold is `max-limit-mb * threshold-percentage / 100`. It is checked **before an
import starts** and **before every batch** (default 100 problems). Reaching it marks the
run `STORAGE_FULL` and stops immediately; a storage-limit error raised by the database
during an insert is classified and handled the same way.

All provider specifics stay behind one interface:

```java
public interface DatabaseStorageService {
    Double getCurrentUsageMb();                    // null = unknown -> do not block imports
    boolean isStorageLimitError(Throwable error);  // classify insert-time DB errors
}
```

`PlaceholderDatabaseStorageService` is active today: it returns `null` ("unknown") so
imports run normally, and detects storage-limit errors with a provider-agnostic keyword
scan of the exception chain. **For deployment**, replace/extend it with your provider's
metrics (e.g. Postgres `pg_database_size()` vs. disk/volume size, or a managed-cloud
instance's used-vs-max storage) and override `isStorageLimitError` with the provider's
real error codes. No storage logic exists anywhere else in the import service.

## Configuration (`application.yaml`)

```yaml
app:
  problem-import:
    enabled: true                     # master switch (startup, schedule, manual trigger)
    dataset: sigcp/hardtests_problems
    dataset-config: default
    split: train
    api-base-url: https://datasets-server.huggingface.co
    initial-import-count: 500         # first import, once, on initialization
    import-count-per-run: 500         # further problems per quarterly run
    batch-size: 100                   # rows per fetch+save batch; storage recheck each batch
    cron: "0 0 3 1 1,4,7,10 *"        # quarterly schedule (every 3 months)
    connect-timeout-seconds: 10
    read-timeout-seconds: 60
    storage:
      max-limit-mb: 2048
      threshold-percentage: 50
```

## Admin API (ADMIN role, under the existing `/api/admin/**` rule)

| Endpoint | Description |
|----------|-------------|
| `GET /api/admin/problem-import/status` | status, offset, imported count, message, storage threshold/usage, config |
| `POST /api/admin/problem-import/trigger` | run an import now (resumes from the last offset); `409` if already `RUNNING` |

## Dataset row -> Problem mapping

| Dataset field | Problem field |
|---------------|---------------|
| `pid` | `external_id` = `hf:<dataset>:<pid>` (dedupe key) |
| `question_title` | `title` (truncated to 200) |
| `question_content` | `description` |
| `difficulty_ratings[].level` | `difficulty` (`easy`/`hard` case-insensitive, anything else -> `MEDIUM`) |
| `tags[].content[]` | `tags` (flattened, comma-joined, truncated to 500; `imported` if empty) |
| `public_test_cases[0]` | `sample_io` as `Input:\n...\n\nOutput:\n...` (same format the admin UI produces) |
| `public_test_cases[]` | `hidden_test_cases` JSON `[{"input","output"}]` (exactly what the judge parses; cases without an expected output are dropped) |
| `time_limit`, `memory_limit` | `constraints` |
| — | `input_format` / `output_format` = standard stdin/stdout notes |

## Code map

```
backend/src/main/java/com/problemsolving/platform/importing/
├── ProblemImportService.java        # orchestration: lock, storage checks, batches, offsets
├── ProblemImportMapper.java         # dataset row -> Problem (platform-compatible formats)
├── ProblemImportScheduler.java      # startup initial import + quarterly cron
├── api/ProblemImportAdminController.java   # GET status / POST trigger (ADMIN)
├── config/ProblemImportProperties.java     # app.problem-import.* configuration
├── domain/ProblemImportState(.java + Repository)  # single-row progress + atomic lock
├── hf/                              # HuggingFaceProblemClient + REST impl + row records
└── storage/                         # DatabaseStorageService + placeholder implementation
```

Tests: `backend/src/test/java/com/problemsolving/platform/importing/` (mapper mapping,
batch/offset flow, storage-full before start and between batches, insert-time storage
error vs. other DB error, HF failure, duplicate skipping, resume, lock concurrency).
