# How CloudStream extensions work (plain-language guide)

This repo builds **extensions** (also called plugins or providers) for the
CloudStream app. This doc explains the whole picture before you touch code, so
nothing in the project feels like magic.

---

## 1. The big picture

CloudStream itself is just a **player shell**. Out of the box it knows how to
show a home screen, a search box, a details page, and how to play a video — but
it has **no idea where any content comes from**. That is 100% the job of
extensions.

An extension is a small compiled file (`.cs3`) that you load into the app. Each
extension contains one or more **providers**. A provider is a Kotlin class that
answers four questions for the app:

| The app asks...                      | The provider method that answers |
|--------------------------------------|----------------------------------|
| "What's on the home screen?"         | `getMainPage()`                  |
| "The user searched X, what matches?" | `search()`                       |
| "They tapped a title — details?"     | `load()`                         |
| "They hit play — what's the video?"  | `loadLinks()`                    |

That's the entire contract. Everything else is detail.

```
┌────────────┐   getMainPage / search / load / loadLinks   ┌──────────────┐
│ CloudStream │ ──────────────────────────────────────────▶ │ Your provider │
│   (app)     │ ◀────────── SearchResponse / LoadResponse ── │  (MainAPI)    │
└────────────┘            / ExtractorLink                    └──────────────┘
```

---

## 2. How this project is laid out

This is a **Gradle multi-project build**. The root sets up shared config; each
sub-folder with a `build.gradle.kts` is one independent extension.

```
CloudstreamExtensions/
├─ build.gradle.kts        ← shared config for ALL extensions (SDK, deps, repo)
├─ settings.gradle.kts     ← auto-includes every folder that has a build.gradle.kts
├─ gradlew / gradlew.bat   ← the Gradle wrapper (so you don't need Gradle installed)
├─ .github/workflows/      ← CI that builds .cs3 files automatically on push
│
├─ ExampleProvider/        ← the official starter template, kept as a reference
│   └─ src/main/kotlin/com/example/
│       ├─ ExamplePlugin.kt     ← @CloudstreamPlugin entry point
│       └─ ExampleProvider.kt   ← an almost-empty MainAPI to copy from
│
└─ MediaServerProvider/    ← a REAL, working provider for your own Plex server
    └─ src/main/kotlin/com/example/plex/
        ├─ PlexPlugin.kt            ← entry point, registers the provider + settings
        ├─ PlexProvider.kt         ← the actual logic (the 4 methods above)
        └─ PlexSettingsFragment.kt ← the settings screen (server URL + token)
```

### Plugin vs Provider — what's the difference?

- A **Plugin** (`@CloudstreamPlugin class ... : Plugin()`) is the *entry point*.
  The app calls its `load()` once, and inside it you call `registerMainAPI(...)`
  for each provider you want to turn on. You can also wire up a settings screen
  here (`openSettings = { ... }`).
- A **Provider** (`class ... : MainAPI()`) is the thing that actually fetches
  content. One plugin can register several providers.

---

## 3. The Plex provider, walked through

`MediaServerProvider` connects CloudStream to **your own Plex Media Server**. It
never scrapes the public internet — every request goes to the address you type
into the settings screen, signed with your personal Plex token.

Here's how each method maps to Plex's API:

1. **Settings** (`PlexSettingsFragment`) — you enter your server URL
   (e.g. `http://192.168.1.50:32400`) and your `X-Plex-Token`. They're saved
   with `CloudStreamApp.setKey(...)`, which is CloudStream's little key/value
   store that survives app restarts.

2. **`getMainPage()`** — calls `GET /library/sections` to list your libraries
   (Movies, TV Shows...), then for each one calls
   `GET /library/sections/{id}/all?sort=addedAt:desc` to get recently-added
   items. Each becomes a row on the home screen.

3. **`search()`** — calls `GET /search?query=...` and turns every movie/show
   hit into a search result card.

4. **`load()`** — calls `GET /library/metadata/{id}` for the title's details.
   - If it's a **movie**, we're done: we hand back a `MovieLoadResponse`.
   - If it's a **show**, we also call `/library/metadata/{id}/allLeaves`, which
     returns *every episode in one request*, and build the season/episode list.

5. **`loadLinks()`** — given a movie or episode id, calls
   `GET /library/metadata/{id}` to find the file's `Part.key`, then builds the
   direct-play URL `{server}{Part.key}?X-Plex-Token=...` and hands it to the
   player. This is **Direct Play** (the original file, no transcoding).

### How Plex authentication works here

Plex uses a single secret string called an **X-Plex-Token**. We send it two ways:
- as an HTTP header (`X-Plex-Token`) for JSON API calls, and
- as a `?X-Plex-Token=` query parameter on image and video URLs, because the
  image loader and the video player don't send our custom headers.

To find your token: open any item in **Plex Web** → **⋯ → Get Info → View XML**,
then copy the `X-Plex-Token=...` value out of the browser's address bar.

---

## 4. Building a `.cs3` file

Two ways:

**A) Automatically with GitHub Actions (recommended, already set up).**
Push this repo to GitHub. On every push, `.github/workflows/build.yml` compiles
each provider and force-pushes the resulting `.cs3` files plus a `plugins.json`
to a branch called `builds`. You then add that `plugins.json` URL as a
*repository* inside CloudStream (Settings → Extensions → Add repository). See
`SETUP_GITHUB.md` in this folder for the one-time repo setup.

**B) Locally (needs JDK 17 + Android SDK).**
```
./gradlew MediaServerProvider:make          # produces the .cs3
./gradlew MediaServerProvider:deployWithAdb # build + push to a connected device
./gradlew make makePluginsJson              # build everything + the repo json
```

---

## 5. Where to go next

- Copy `ExampleProvider` to a new folder to start a brand-new provider — the
  `settings.gradle.kts` auto-includes any folder with a `build.gradle.kts`.
- The four methods are documented in the official stubs; the real reference is
  the app source under `../cloudstream/library/.../MainAPI.kt`.
- Official docs: https://recloudstream.github.io/csdocs/devs/gettingstarted/
