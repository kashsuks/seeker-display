# Git Conventions

## Commit Rules

- Ensure the code runs correctly before committing

## Commit Message Format

```
<type>[<scope>]: <subject>
```

A space follows the colon. Type values:

| type | Purpose |
|------|---------|
| Feat | New feature |
| Fix | Bug fix |
| Docs | Documentation or comments |
| Style | Code formatting (no runtime impact) |
| Refactor | Refactoring (not a new feature or bug fix) |
| Perf | Performance optimization |
| Test | Adding tests |
| Chore | Build process or tooling changes |

Additionally, ensure that the first letter of type, scope, and subject are capitalized.

## Squash Commits and Pull Requests

PR Message/Title Format

PR's MUST use squash commit formatting as follows

```
[Scope] <overview of changes for this PR>
```

Note that the first letter of the scope and commit message must be capitalized

For example:

```
[Shooter] Add different shooting angles
```

This allows us to squash multiple commits that were in the Pull Request into one general commit.

## Other

- You MUST also use squash commit formatting if your commit covers more than one changed issue. Refer to the [squash commit](#squash-commits-and-pull-requests)
- Keep commit names short and elaborate. They're meant to be used to quickly identify issues or to understand changes.
