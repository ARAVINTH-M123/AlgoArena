# Project instructions
- This repo is AlgoArena (Next.js + Tailwind frontend, Spring Boot 4 / Java 21 backend, PostgreSQL).
- The Nation progression system is specified in docs/NATION_BLUEPRINT.md. Follow it. Progress is tracked in NATION_PROGRESS.md.
- Backend layering: Controller -> Service -> Repository -> DTO. Never expose entities. Keep gameplay numbers in the central nation configuration (NationProperties).
- Only add features; do not refactor or restyle existing code. Keep the dark black + green accent theme.
- Never hardcode secrets. Keep the repo compiling after every change. Run the build and tests before finishing a task.
