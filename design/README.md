# Home screen mockups

HTML mockups made while redesigning the launcher's home screen. Open them at 1920x1080 (they scale to the
window) and drive them with the arrow keys, Enter and Esc, like the remote.

| File | What it is |
|---|---|
| `home.html` | **Penumbra**, the chosen design, as built in the `penumbra-launcher` branch (see `DESIGN.md` there) |
| `escenario.html`, `panel.html`, `lista.html` | Three alternative layouts that were tried and set aside |
| `common.css`, `common.js` | Shared pieces of the three alternatives |

The tiles show app banners from `banners/*.jpg`, which are not in the repository: they were crops of each app's
own artwork taken from a TV's screen, and that artwork belongs to the apps. To see the mockups with pictures,
put 640x360 JPEGs named `tizentube`, `jellyfin`, `netflix`, `prime`, `appletv`, `disney`, `atresplayer`, `rtve`,
`playstore` and `settings` in `design/banners/`; without them the tiles stay plain.

Serve the folder rather than opening the file, so the fonts and scripts load: `python3 -m http.server -d design`.
