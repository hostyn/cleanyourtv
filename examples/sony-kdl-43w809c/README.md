# Sony BRAVIA KDL-43W809C (2015, Android TV 7.0)

EU firmware `SVPDTV15_EU` (NRD91N.S34 5.433, MediaTek, 1.6 GB RAM, 32-bit armeabi-v7a), used in Spain.
Both halves work on this set: the debloat list and the Clean Your TV launcher as Home, which needs the
launcher's Android 7 support (minSdk 24). Tested on 21 October 2026, including a reboot.

| File | What it is |
|---|---|
| `debloat.txt` | The 37 packages that were disabled, grouped and commented |
| `config.json` | Dock order, hidden Sony tiles, Live TV app, columns; push it with `tools/theme.sh` |

All commands run from the repo root, with the TV's IP in `TV_HOST`. USB debugging must be on while you
run them. This TV's adbd accepts one network connection at a time, so run `adb disconnect <tv-ip>:5555`
first if you used the stock `adb` client; otherwise `tools/tv.py` times out after 10 seconds.

## Install, from scratch or again

```bash
TV_HOST=<tv-ip> tools/disable.sh examples/sony-kdl-43w809c/debloat.txt
TV_HOST=<tv-ip> HOME_KEY_SERVICE=1 launcher/build-and-install.sh
TV_HOST=<tv-ip> tools/theme.sh examples/sony-kdl-43w809c
TV_HOST=<tv-ip> tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'
```

The stock launcher is `com.google.android.leanbacklauncher`; no setup wizard claims Home here, so nothing
else has to be disabled. TizenTube (YouTube without ads) is the first dock app: install the 32-bit
`cobalt-arm.apk` from [reisxd/TizenTubeCobalt](https://github.com/reisxd/TizenTubeCobalt/releases) (it needs
Android 7, which this set has) with `tools/b64push.py` and `pm install -r`, or with Downloader on the TV.

## Undo everything

```bash
TV_HOST=<tv-ip> tools/restore.sh examples/sony-kdl-43w809c/debloat.txt com.google.android.leanbacklauncher
```

## Re-enable one app

```bash
TV_HOST=<tv-ip> tools/venv/bin/python -I tools/tv.py 'pm enable com.google.android.apps.mediashell'
```

Comment its line out in `debloat.txt` so the next run leaves it alone. Without a computer: Settings →
Apps → System apps → pick the app → Enable. Off by choice on this unit: official YouTube (TizenTube is used
instead), voice search, Chromecast built-in, screen mirroring (Miracast / Wi-Fi Direct) and Sony's firmware
updates. Netflix and Prime Video stay.

## Change the dock order or the wallpaper

Edit `dock_apps` in `config.json`: package names, left to right; an app that isn't installed is skipped
and the next grid app fills its slot. `tools/tv.py 'pm list packages -3'` lists the installed ones. Push it
again with the `theme.sh` line above, no rebuild needed. For a wallpaper, put a dark 1920×1080
`wallpaper.jpg` (under about 300 KB) next to `config.json`, add `"wallpaper": true` and push the same way;
`tools/theme.sh --reset` goes back to the stock look. All keys: [`themes/README.md`](../../themes/README.md).

`hidden_apps` keeps Sony's own tiles out of the grid (TV, Programme Guide and Recorded Title List from
`com.sony.dtv.tvx`, and the Album, Music and Video players): the Inputs pill already opens Live TV and every
HDMI and AV input. Remove a package from that list to bring its tile back.

## TizenTube as "YouTube"

TizenTube's own banner did not say what it was, so the family kept opening the official YouTube with ads.
`config.json` gives its tile `banner_youtube_noads` (`app_banners`), YouTube's TV banner with a small
"TizenTube - No ads" under the wordmark, and the name "YouTube" (`app_labels`). The banner is bundled with
the launcher (`res/drawable-nodpi`), so it needs a launcher build that includes it.

## The Fire TV as a favourite

With the Halo launcher, `config.json` puts the Fire TV on HDMI 1 at the end of the favourites (`dock_size` 6,
after YouTube, Jellyfin, TiviMate, Netflix and Prime Video) as an input tile. It names the port by its input id, `com.sony.dtv.tvinput.external/.ExternalTvInputService/HW2`
(hardware 2, `hdmi_port=1` in `dumpsys tv_input`), not as `"HDMI 1"`: Sony calls its ports "HDMI 1/MHL" and
"HDMI 4/ARC", which the launcher does not read as port numbers. The id of the port stays the same when the
Fire TV reconnects; the CEC device's own id (`HDMI100004`) might not.

## Colour buttons open apps

`app_keys` in `config.json` turns the remote's red, green and yellow buttons into app buttons: red (keycode 183,
`KEYCODE_PROG_RED`) opens TizenTube, green (184) Jellyfin and yellow (185) TiviMate. Each colour has its own
keycode, so no scancode entries are needed. The launcher's `HomeKeyService` answers them, which is why the
install line above passes `HOME_KEY_SERVICE=1`: Android switches the service off whenever the launcher is
reinstalled. `InputKeyService` would work too, but it also takes the Input button and shows the launcher's
inputs menu instead of Sony's. With the service on, held arrows and Volume still repeat (checked with the real
remote, 2026-10-23). Blue (186) is still free. To change one, edit its line and push the theme again.

## Notes for this set

- The remote's Netflix, YouTube, Google Play, Input and Action Menu buttons keep working without the stock
  launcher. Only the colour buttons above need the accessibility service.
- Screenshots come out black while HDMI, Live TV or Netflix is on screen; press Home first.
- Switch USB debugging off when you're done; the launcher keeps working without it.
