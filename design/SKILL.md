---
name: filelens-design
description: Use this skill to generate well-branded interfaces and assets for FileLens, the privacy-first Android smart file commander (browse files, peek inside, inspect metadata, and understand risk & trust before opening). Dark-first Darcula-flavored look with a violet accent and a risk-&-trust visualization signature. Contains design guidelines, color/type tokens, the lens brand mark, and an interactive Android UI kit for prototyping.
user-invocable: true
---

Read the `README.md` file within this skill, then explore the other files:

- `colors_and_type.css` — all design tokens (dark `:root`, light `[data-theme="light"]`). Always copy this out and use `var(--fl-*)` / `var(--cat-*)` names; never invent colors.
- `assets/` — `lens_mark.svg` (monochrome, `currentColor`), `app_icon.svg` (full-color launcher).
- `preview/` — concept cards (colors, type, spacing, components, brand).
- `ui_kits/android/` — interactive recreation of the app; copy and fork its components (`app.jsx`, `kit.css`, `icons.jsx`, `data.jsx`) to build new FileLens screens.

Core rules (full detail in README):
- **Dark-first.** Canvas `#16171A`, panels `#1E2024`, cards `#25272D`, hairline borders. Light theme is secondary.
- **One accent:** violet `#A855F7`, for interaction only — never decoration.
- **Color = meaning.** Risk (danger/warning/info/safe) and the trust ramp (Trusted/Low/Medium/High) and category seeds are the only other colors.
- **Type:** Roboto for UI, Roboto Mono for data (sizes, hashes, scores, code).
- **Signature:** the risk & trust layer — trust chips/rings, severity dots, and three-beat risk cards (*what found → why it matters → what to do*).
- **Voice:** calm, precise, protective; sentence case; honest about heuristic confidence; no emoji; Lucide icons (1.75 stroke).

If creating visual artifacts (mocks, prototypes, throwaway demos), copy the assets + tokens out and produce static/interactive HTML for the user to view. If working on production code, copy assets and use these rules to design natively. If the user invokes this skill without guidance, ask what they want to build, ask a few focused questions, then act as an expert FileLens designer outputting HTML or production code as appropriate.
