# One-time GitHub setup for automatic `.cs3` builds

The CI in `.github/workflows/build.yml` builds your extensions and publishes
them to a `builds` branch every time you push. A few one-time steps are needed.

## 1. Create the repo and push

```
# from inside CloudstreamExtensions/
git init
git add <files>            # stage intentionally; this workspace has stray 0-byte files
git commit -m "Initial CloudStream extensions"
gh repo create CloudstreamExtensions --public --source . --push
```
(You're signed in to `gh` as **saywhy2me**, so the repo lands under that account.)

## 2. Create the empty `builds` branch

The workflow checks out a branch named `builds` and pushes the compiled files
there. Create it once as an orphan (empty) branch:

```
git checkout --orphan builds
git rm -rf . >/dev/null 2>&1
git commit --allow-empty -m "init builds"
git push origin builds
git checkout main        # or master, whichever you used
```

## 3. Allow Actions to write

On GitHub: **Settings → Actions → General**
- *Actions permissions*: **Allow all actions and reusable workflows**
- *Workflow permissions*: **Read and write permissions**

(The official docs require both before the auto-build works.)

## 4. Add the repo inside CloudStream

After the first successful build, your `plugins.json` lives at:

```
https://raw.githubusercontent.com/saywhy2me/CloudstreamExtensions/builds/plugins.json
```

In the app: **Settings → Extensions → Add repository** → paste that URL. Your
providers (Example + Plex) will appear to install. Open the Plex one's settings
(gear icon) and enter your server URL + token.

> Tip: CloudStream also accepts a short `recloudstream`-style link, but the raw
> `plugins.json` URL above always works.
