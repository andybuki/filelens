# Play Store assets

The listing source-of-truth now lives at **`app/src/main/play/`**, the layout
expected by the Gradle Play Publisher plugin. Pushing changes there to `main`
triggers `.github/workflows/play-publish.yml`, which uploads everything to
Google Play Console via the Play Developer API — no Console UI editing
needed.

## Where things live

```
app/src/main/play/
├── default-language.txt              "en-US"
├── listings/
│   ├── en-US/                        ← source language (Play default)
│   │   ├── title.txt                       ≤ 30 chars
│   │   ├── short-description.txt           ≤ 80 chars
│   │   ├── full-description.txt            ≤ 4000 chars
│   │   └── graphics/
│   │       ├── feature-graphic/1.png       1024 × 500
│   │       ├── icon/1.png                  512 × 512
│   │       ├── phone-screenshots/1.png … 8.png    (add when captured)
│   │       └── tablet-screenshots/1.png … 4.png   (add when captured)
│   └── <14 locales>/                 es-ES · pt-BR · fr-FR · de-DE · it-IT
│                                     · ru-RU · uk · tr-TR · pl-PL
│                                     · ja-JP · ko-KR · zh-CN · hi-IN · ar
└── release-notes/
    └── <locale>/production.txt       ≤ 500 chars, one per locale
```

Per-locale folders only carry text — graphics fall back to en-US in
Play Console, which is what we want unless we localise screenshots.

## What's still under `store/`

- `listing/feature-graphic.svg` — the editable source for the rendered
  PNG that ships in `app/src/main/play/listings/en-US/graphics/`.
- `listing/screenshots-plan.md` — capture brief for the 8 phone + 4
  tablet screenshots (yours to shoot).
- `release-notes/v0.1.0-beta.txt` — kept for history.

When you capture screenshots, place them as numbered PNGs under
`app/src/main/play/listings/en-US/graphics/phone-screenshots/` and
`.../tablet-screenshots/` — names `1.png` … `8.png` control the order
Play Console displays them.

## Publishing flow

| Action | Command / trigger | What it pushes |
|---|---|---|
| Listing text + graphics | `./gradlew :app:publishListing` | titles, descriptions, feature graphic, icon, screenshots — all locales |
| AAB (signed bundle) | `./gradlew :app:publishBundle` | the build itself + release notes |
| All of the above | `./gradlew :app:publishApps` | one shot |

CI: any push to `main` that touches `app/src/main/play/**` runs
`publishListing` automatically. AAB upload is opt-in via
`workflow_dispatch` (the toggle in the workflow's "Run workflow" panel).

## One-time setup

1. **Service account.** Google Cloud Console → IAM → create service account →
   download JSON key.
2. **Play Console linkage.** Play Console → Setup → API access → link the
   GCP project → grant the new service account the **Release manager**
   role on this app.
3. **GitHub secret.** Settings → Secrets and variables → Actions → add
   `PLAY_SERVICE_ACCOUNT_JSON` with the JSON contents.

Local dev: drop the same JSON at `app/play-service-account.json`
(gitignored) to run the publish tasks from your machine.

## Safety rails baked into `app/build.gradle.kts`

- `track = "internal"` and `releaseStatus = DRAFT` — nothing reaches
  end-users until you change it.
- `defaultToAppBundles = true` — no APK fallback, AAB only.

Flip both once the listing renders correctly in the Console preview.
