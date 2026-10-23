# CloudStream Extensions

Personal [CloudStream](https://github.com/recloudstream/cloudstream) extension
repo, based on the official [plugin template](https://github.com/recloudstream/TestPlugins)
and the [getting-started docs](https://recloudstream.github.io/csdocs/devs/gettingstarted/).

## Extensions in this repo

| Folder | Provider | What it does |
|--------|----------|--------------|
| `ExampleProvider/` | Example provider | The official starter template, kept as a copy-me reference. |
| `MediaServerProvider/` | **Plex** | Streams from your own Plex Media Server (Direct Play). Configure your server URL + token in the extension's settings. |

## Build

- **CI (recommended):** push to GitHub; `.github/workflows/build.yml` publishes
  `.cs3` files + `plugins.json` to the `builds` branch. See
  [`docs/SETUP_GITHUB.md`](docs/SETUP_GITHUB.md).
- **Locally** (JDK 17 + Android SDK):
  ```
  ./gradlew MediaServerProvider:make
  ./gradlew make makePluginsJson
  ```

## Learn how it works

New to CloudStream plugins? Read
[`docs/HOW_CLOUDSTREAM_EXTENSIONS_WORK.md`](docs/HOW_CLOUDSTREAM_EXTENSIONS_WORK.md)
— a plain-language walkthrough of providers, the four core methods, and the Plex
provider line by line.

## Note

These extensions are for legitimate use: the Example template, and a provider
for **your own** Plex server. This repo intentionally contains no scrapers for
copyrighted content on third-party streaming sites.
