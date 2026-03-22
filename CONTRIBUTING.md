# Contributing to Wahon

Thank you for your interest in contributing.

Wahon is currently in beta and the architecture is still evolving. To keep changes maintainable, please coordinate larger feature work before implementation.

## Before you start

1. Check open issues and plans to avoid duplicate work.
2. For non-trivial features, open an issue/discussion first and describe:
   - problem statement
   - proposed approach
   - migration/risk impact
3. Keep pull requests focused. Separate unrelated changes into separate PRs.

## Development workflow

1. Fork the repository and create a feature branch from `main`.
2. Implement changes with tests or verification notes.
3. Run local checks/builds before opening a PR.
4. Open a PR with:
   - concise summary
   - screenshots/logs for UI/runtime changes
   - testing steps
   - migration notes (if any)

## Required agreement (CLA)

By contributing, you confirm agreement with the project [Contributor License Agreement](CLA.md).
PRs may be declined if CLA terms are not accepted.

## Licensing model

- Main app/source code: GNU GPL v3.0.
- Translations/localization assets: Apache 2.0.

This split is documented in [README.md](README.md) and license files in `licenses/`.

## Quality expectations

- Keep code and docs in English.
- Follow existing architecture boundaries (domain/data/ui separation).
- Avoid source-specific hardcoding in shared runtime flows.
- Include migration-safe behavior for data persistence changes.

## Code of conduct

Be respectful and constructive in discussions and reviews.
