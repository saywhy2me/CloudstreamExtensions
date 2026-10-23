# Installing the Plex extension locally (no GitHub, no repo)

CloudStream can load plugins straight from a folder on the device, so you build
the `.cs3` **once** and copy it to each device. Nothing checks the internet.

The built files live in `dist/` (and in each module's `build/` folder):
- **`MediaServerProvider.cs3`** ← the Plex provider (the one you want)
- `ExampleProvider.cs3` ← the empty template; optional, harmless

## Where the files go on a device

CloudStream scans this folder on the device's internal storage:

```
Internal storage/Cloudstream3/plugins/
```
(full path: `/storage/emulated/0/Cloudstream3/plugins/`)

Create the `plugins` folder if it isn't there, and drop the `.cs3` inside.

## Method A — file transfer (phones/tablets)

1. Connect the device by USB (or use any file manager) and copy
   `MediaServerProvider.cs3` into `Internal storage/Cloudstream3/plugins/`.
2. Grant CloudStream **All files access** (needed on Android 11+):
   Settings → Apps → CloudStream → Permissions → Files/All files access → Allow.
3. Force-close and reopen CloudStream. The provider appears in the provider list.
4. Open its settings (gear icon) → enter your **Plex server URL** and
   **X-Plex-Token** → Save.

## Method B — ADB push (TV boxes, Chromecast with Google TV, emulators)

With the device reachable over ADB (USB, or Wi-Fi e.g. `adb connect 10.0.0.27:5555`):

```
adb push dist/MediaServerProvider.cs3 /sdcard/Cloudstream3/plugins/
# grant all-files access (use the package for your build):
#   stable:     com.lagradost.cloudstream3
#   prerelease: com.lagradost.cloudstream3.prerelease
#   debug:      com.lagradost.cloudstream3.prerelease.debug
adb shell appops set --uid com.lagradost.cloudstream3 MANAGE_EXTERNAL_STORAGE allow
```
Then reopen CloudStream and configure the provider's settings as above.

> The developer shortcut `./gradlew MediaServerProvider:deployWithAdb` does the
> build + push in one step to a single connected device.

## Rebuilding after code changes

```
./gradlew MediaServerProvider:make
```
then re-copy the new `MediaServerProvider.cs3` (from `MediaServerProvider/build/`)
to each device's `Cloudstream3/plugins/` folder, overwriting the old one.

## Settings are per-device

The Plex server URL + token are stored by CloudStream on each device, so you
enter them once per device after installing. The `.cs3` itself carries no
credentials.
```
