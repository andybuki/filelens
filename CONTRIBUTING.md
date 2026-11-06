# Contributing

Thanks for your interest in FileLens! Please read this first.

## License of contributions

FileLens is licensed under the [GNU GPL v3.0](LICENSE). By submitting a pull
request you confirm that you wrote the contribution (or have the right to
submit it) and that you license it under GPL-3.0-or-later. Please also grant
the maintainer permission to distribute your contribution in the Google Play
build of FileLens under the same terms.

## Ways to help

- **Bug reports**: use the issue template. Describe the file type instead of
  attaching a private file; never upload real photos, documents or keys.
- **Translations**: strings live in `app/src/main/res/values-*/strings.xml`,
  the Play listing in `app/src/main/play/listings/<locale>/`.
- **New inspectors or file types**: open an issue first so we can agree on
  the scope.

## Ground rules

- Keep the principles: on-device, no analytics or advertising SDKs, no new
  network calls except for features the user turns on explicitly.
- Risk texts follow the three-beat style: what was found, why it matters,
  what to do. Never claim certainty a heuristic doesn't have.
- Add or update unit tests and run the checks before opening a PR:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

- Small, focused pull requests are easier to review.
- All changes go through pull requests and need maintainer approval.
