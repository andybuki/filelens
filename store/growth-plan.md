# FileLens growth plan: 50 → 200 installs in 30 days

**Is it realistic?** Yes. +150 installs in 30 days is about 5 a day. A small
niche app gets that from two sources together:

- a store listing that converts, and
- a few community posts that actually reach people.

One good Reddit or XDA post alone can bring 50–150 installs in a day.

What blocks growth today:

1. The screenshots show Settings and an empty form instead of what makes FileLens unique.
2. The title competes for "file manager" against apps with 100M+ installs.
3. The Reddit posts were removed by spam filters, so nobody saw them.

Items 1 and 2 are done in the repo (see "Done for you"). The rest needs your
accounts and your phone.

---

## Done for you (in this branch)

| What | Where | Goes live |
|---|---|---|
| New title in all 15 languages, aimed at searches a small app can win ("EXIF", "remove GPS", "metadata") | `app/src/main/play/listings/*/title.txt` | When merged to `main` (play-publish workflow) |
| New short description in all 15 languages: problem first, then "no ads, no tracking" | `.../short-description.txt` | Same |
| English full description now opens with the problem and the reasons people leave competitors (ads, trackers, paid dark theme) | `listings/en-US/full-description.txt` | Same |
| Captioned 1080×1920 screenshots from real app captures | `store/screenshots/out/` | You upload them (step 2) |
| Screenshot generator: edit `captions.json`, run `node store/screenshots/render.js` | `store/screenshots/` | n/a |

---

## Your checklist

### Day 1: measure where you start (15 min)

Play Console → **Statistics**, period "Last 30 days". Write these numbers in the tracking table at the bottom:

- **Store listing visitors** and **store listing acquisitions** (Grow users → Store performance → Store listing acquisition). Conversion = acquisitions ÷ visitors.
- **Search terms** (same report, "Traffic source: Google Play search" → view search terms). These are the words people actually type to find you.
- **Top countries.**

Export the CSVs (download icon on each report) and send them to Claude for analysis.

### Days 1–3: capture 4 screens on your phone

The generator needs these real captures. Save them as PNG into
`store/screenshots/raw/` with exactly these names:

1. `gps-risk.png`: take a photo outdoors with location on. Open it in FileLens → Inspector → **Risks** tab, with the GPS danger card visible.
2. `safe-share.png`: same photo → **Share cleaned copy**, the screen that shows what was removed.
3. `fake-extension.png`: copy any `.apk` and rename it to `holiday.jpg`. Open it in FileLens so the type-mismatch warning shows.
4. `archive.png`: open any `.apk` or `.zip` as a folder (AndroidManifest.xml, classes.dex, META-INF/ visible).

Tips: use real-looking file names, the default font size, and full battery and signal in the status bar.
Then run `node store/screenshots/render.js` (or ask Claude to). You get 9 images in `out/`, with the GPS ones first.

### Day 3: A/B test the screenshots, don't just swap them

Play Console → **Grow users → Store listing experiments → Create experiment**:

- Type: Default graphics. Target: 50% of visitors.
- Variant A = current screenshots. Variant B = `store/screenshots/out/1.png … 8.png` (Play allows max 8).
- Run it until Play shows a result, usually 7–14 days at your traffic. Apply the winner.

With fewer than ~100 visitors a week the test can stay "inconclusive".
In that case simply apply the new set after 7 days, since it's clearly better than Settings screens.

### Day 3: merge the listing text

Merge this branch to `main`. The `play-publish` workflow uploads the new titles and descriptions.
Check the listing in Play Console afterwards. Google reviews listing changes, which takes a few hours to 2 days.

### Week 1: set up the free directories (1 hour total)

- **AlternativeTo** (alternativeto.net): add FileLens and list it as an alternative to *File Manager+, ExifTool, Scrambled Exif, Solid Explorer, Material Files*. It brings steady, free search traffic.
- **XDA Forums**: create a thread in "Android Apps and Games" with the XDA text below.
- **Reddit karma**: for 7–10 days, write 1–2 helpful comments a day in r/Android, r/androidapps, r/privacy and r/degoogle. Don't link the app yet. New accounts posting store links get auto-removed, which is what happened to your post.

### Weeks 2–4: posts (one every 3–4 days, never the same text twice)

| Day | Where | What |
|---|---|---|
| 10 | r/androidapps | Dev post, exact format below |
| 13 | r/privacy (no link in the post) | Finding post: "I checked my Downloads for GPS data" |
| 16 | XDA thread bump with a changelog | Release notes |
| 18 | Hacker News, "Show HN" | Short technical post |
| 20 | r/degoogle or r/fossdroid* | Privacy angle |
| 22–28 | Answer old threads | Search Google for `reddit remove exif android`, `reddit how to check if apk is safe`, and reply where FileLens actually answers the question |

\* r/fossdroid only if you open-source the app; otherwise skip it.

### Week 2: email 15–20 small reviewers

Look for Android YouTubers and bloggers with 2k–50k followers who review "useful apps", "privacy apps" or "hidden Android features".
Small creators answer and often do a quick mention. Use the email template below.

### Every day: reply to every Play review within 24 hours

Ratings matter more than anything at under 10 reviews. The in-app review prompt is already built in (`ui/util/InAppReview.kt`).
It asks after a few inspections or Safe Shares, never on first launch. Ask friends and colleagues who really use the app to leave an honest rating.

---

## Ready-to-paste texts

### r/androidapps (read the sidebar rules first; many subs want the "[DEV]" tag)

**Title:** [DEV] FileLens: see what's hidden inside a file before you open or share it (free, no ads, no tracking)

> Hi r/androidapps. I built FileLens because I wanted to know what a file
> contains before opening it, like photos that still carry GPS, Word files
> with tracked changes, or a ".jpg" that is really an APK.
>
> **What it does**
> - Inspector for any file: real type from magic bytes, EXIF/GPS, PDF JavaScript, Office macros and hidden sheets, APK permissions and trackers
> - Safe Share: strips location and author data before sharing
> - Full file manager: dual pane on tablets and DeX, recycle bin, vault, FTP/SFTP/SMB, free dark theme
>
> **What it doesn't do:** no ads, no accounts, no analytics. Play's Data safety says "no data collected".
>
> Free: https://play.google.com/store/apps/details?id=app.com.filelens
>
> I'm the only developer, so feedback and bug reports are very welcome.
> What file type should the inspector understand next?

### r/privacy: a finding post, not an ad

**Title:** I checked 200 photos in my Downloads folder. 38% still had exact GPS location.

> (Use your own real numbers.) Explain how you checked, when location survives
> (files sent "as document" in messengers, email attachments, cloud uploads), and how
> to remove it on Android: the built-in option when sharing in Google Photos,
> Scrambled Exif, or ExifTool on PC. Mention FileLens last, as "the tool I
> built for this", and only if the sub's rules allow it. Otherwise answer in
> the comments when someone asks.

### XDA thread

**Title:** [APP][8.0+] FileLens: file manager that inspects files before you open them

> Body: the same as the Reddit post, plus a changelog section and
> screenshots 1–4. Update it with every release, because XDA threads rise again
> when you post.

### Show HN

**Title:** Show HN: FileLens – an Android file manager that tells you what's inside a file

> 3–4 sentences: the problem, the technical part (magic-byte detection,
> OOXML/PDF parsing, on-device trust score, no network), what's next,
> and a link. Stay around in the first 2 hours to answer comments.

### Email to reviewers

**Subject:** Free privacy app for your audience: shows hidden GPS in photos before sharing

> Hi <name>, I liked your video on <specific video>. I'm a solo developer
> and made FileLens, a free Android app with no ads that shows what's hidden
> inside files (GPS in photos, macros in Office files, fake extensions) and
> removes it before sharing. I think it would fit your <topic> videos.
> Play link: … Happy to answer questions or add a feature your viewers ask
> for. Thanks, <your name>

---

## Tracking table (fill in every Monday)

| Week | Listing visitors | Installs | Conversion | Ratings (count / avg) | Top search term | What I did |
|---|---|---|---|---|---|---|
| 0 (start, 21 Oct) | ? | 55 installed audience (37 on 25 Sept, +0.7/day net) | ? | 0 | ? | Baseline from Play export |
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |
| 4 | | | | | | |

**Targets for day 30:** conversion ≥ 25%, ≥ 10 ratings, 200 total installs.

**How to read the numbers:**

- **Conversion below 20% with many visitors:** the listing is the problem. Iterate the screenshots and the first line of the description.
- **Conversion fine but few visitors:** visibility is the problem. Post more and add more directory listings.

## Don't (yet)

- **Don't pay for ads** until conversion is above 25%, or you'll buy visitors who don't install.
- **Don't add big new features this month.** The app already does more than the listing shows.
- **Don't post the same text to many subreddits on the same day.** It gets flagged as spam everywhere.
