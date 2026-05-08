# Commit Convention

When code changes are complete, use Conventional Commits for the Git commit message.

Choose the type based on the main change:

- `feat`: a new feature
- `fix`: a bug fix
- `docs`: documentation changes only
- `style`: formatting or UI-only changes with no logic change
- `refactor`: code restructuring without new behavior or bug fixes
- `perf`: performance improvements
- `test`: adding or updating tests only
- `chore`: build, tooling, or dependency changes with no source-code impact
- `revert`: reverting a previous commit

Keep the subject short, specific, and in imperative form. Use a scope when it helps clarify the area changed, for example `feat(buyer-profile): add address card`.

If a change spans multiple categories, prefer the type that matches the primary user impact. If no single type is clear, ask before committing.