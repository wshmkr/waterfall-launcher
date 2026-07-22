## Branches
- Never work on `main`. Use worktrees in `.claude/worktrees/`.
- Branch name: `<type>/<summary>` (e.g. `feat/config-menu`).

## Commits
- Title: brief and descriptive.
- Body optional. If present, highlight the big changes only.

## Pull requests
- Title format: `<type>: <description>` (Conventional Commits — enforced by `.github/workflows/pr-title.yml`).
- Body: fill out `.github/pull_request_template.md`.
- Feature-oriented, brief; implementation notes only for meaningful tradeoffs.
- Professional tone. No local paths or private files.

## Code style
- Self-documenting names. If a name needs a comment to explain what it does, rename it.
- Comments: brief — one line where possible, no over-explaining.

## Build
- Android / Kotlin, Gradle wrapper.
- On WSL, build with `cmd.exe /c gradlew.bat` (the Linux `gradlew` can't reach the Windows Android SDK). Fresh worktrees need `local.properties` copied from the primary checkout.
