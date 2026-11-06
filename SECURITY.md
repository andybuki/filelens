# Security policy

## Reporting a vulnerability

Please **do not open a public issue** for security problems.
Use GitHub's private reporting: **Security → Report a vulnerability** on
<https://github.com/andybuki/filelens/security/advisories/new>.

Include what you found, how to reproduce it (a synthetic test file is ideal),
the Android version and device, and the impact. This is a one-person project,
so please allow a few days for an answer.

## Scope

- The FileLens Android app (`app/`), in particular:
  - parsing of untrusted files by the inspectors (crashes, hangs, memory blow-ups, path traversal when browsing or extracting archives);
  - metadata that Safe Share claims to remove but leaves in the shared copy;
  - storage of remote-server credentials and the hidden vault;
  - anything that sends file contents or metadata off the device without the user asking.
- The CI workflows in `.github/workflows`.

## Design notes relevant to security

- FileLens has no backend and no analytics. Network access is only used for
  FTP/SFTP/SMB servers the user adds.
- The trust score and risk cards are heuristics. FileLens is not an antivirus
  and does not claim to detect malware.
- Signing keys and the Play service account are never committed; CI reads them
  from repository secrets.

If you ever find a real key, token or personal data in this repository or its
history, report it the same way.
