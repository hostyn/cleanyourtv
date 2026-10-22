# TCL Smart TV Pro G08 (Google TV, Android 14)

Firmware `G08_4K_GB` (UTT2.250416.001 AU02, MediaTek `tcl9618`), used in Spain. Both halves work on this
set: the debloat list and the Clean Your TV launcher as Home. Tested on 21 October 2026, including a reboot.

| File | What it is |
|---|---|
| `debloat.txt` | The 33 packages that were disabled, grouped and commented, plus what each remote button does |
| `config.json` | Dock order, hidden apps, Live TV app, columns, remote buttons; push it with `tools/theme.sh` |

All commands run from the repo root, with the TV's IP in `TV_HOST`. USB debugging must be on while you
run them (Settings → System → Developer options), and `tools/venv` must exist
(`python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt`).

## Install, from scratch or again

If the Wi-Fi network was saved during first setup, forget it and join it again in Settings → Network &
Internet first: it belongs to the setup wizard (setupwraith) until then, and Android deletes it the moment
setupwraith is disabled. `disable.sh` checks this and skips setupwraith while it owns the network.

The order matters: setupwraith is disabled while Google TV's home still owns Home, then
`build-and-install.sh` starts the new launcher and only then disables launcherx.

```bash
TV_HOST=<tv-ip> tools/disable.sh examples/tcl-g08-google-tv/debloat.txt
TV_HOST=<tv-ip> launcher/build-and-install.sh
TV_HOST=<tv-ip> tools/theme.sh examples/tcl-g08-google-tv
TV_HOST=<tv-ip> tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'
```

The last line swaps the Ambient screensaver (`dreamx`, disabled by the list) for the launcher's Clock.
Building needs JDK 17 and the Android SDK (platform 34, build-tools 34.0.0). On Arch/CachyOS:
`paru -S jdk17-openjdk android-sdk-cmdline-tools-latest`, then `sdkmanager --sdk_root=$HOME/Android/Sdk
"platforms;android-34" "build-tools;34.0.0"` and `--licenses`.

## Undo everything

```bash
TV_HOST=<tv-ip> tools/restore.sh examples/tcl-g08-google-tv/debloat.txt com.google.android.apps.tv.launcherx
TV_HOST=<tv-ip> tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop'
```

The launcher stays installed but is no longer Home. Never re-enable just
`com.google.android.tungsten.setupwraith` on its own while the new launcher is Home; `restore.sh` brings
Google TV's home back first.

## Re-enable one app

```bash
TV_HOST=<tv-ip> tools/venv/bin/python -I tools/tv.py 'pm enable com.google.android.youtube.tv'
```

Comment its line out in `debloat.txt` so the next run leaves it alone. Without a computer: Settings →
Apps → See all apps → Show system apps → pick the app → Enable. Off by choice on this unit: official
YouTube (TizenTube is used instead), YouTube Music, the Google Assistant (`katniss`), Miracast, AirPlay,
Alexa and TCL Channel. Chromecast built-in, Netflix and Prime Video stay.

## Change the dock order or the wallpaper

Edit `dock_apps` in `config.json`: package names, left to right; an app that isn't installed is skipped.
`tools/tv.py 'pm list packages -3'` lists the installed ones. Push it again with the `theme.sh` line
above, no rebuild needed. For a wallpaper, put a dark 1920×1080 `wallpaper.jpg` (under about 300 KB)
next to `config.json`, add `"wallpaper": true` to it and push the same way; `tools/theme.sh --reset`
goes back to the stock look. All keys:
[`themes/README.md`](../../themes/README.md). `com.tcl.tv` and `com.tcl.suspension` are in `hidden_apps`
because the Inputs pill already covers Live TV and the inputs.

## The remote's app buttons

Netflix, YouTube, Prime Video, Settings and TV do nothing once Google TV's home is gone: TCL's system code
hands them to launcherx by name. `config.json` brings them back with `key_bridge`:

```json
"key_bridge": true,
"app_keys": ["scan:566:com.netflix.ninja", "scan:569:io.gh.reisxd.tizentube.cobalt",
             "scan:752:com.amazon.amazonvideo.livingroom", "scan:561:settings", "scan:365:com.tcl.tv"]
```

The launcher connects to the TV's own network ADB (127.0.0.1:5555) with its own key and starts a small
`getevent` script that opens the app for each button. The first time, the TV asks "Allow USB debugging?"
for `cleanyourtv@Smart TV Pro`: tick "Always allow". Nothing is intercepted, so holding Volume or an arrow
keeps repeating, which the accessibility services (`HOME_KEY_SERVICE=1`) break on this set.

The cost: **network debugging stays on**. The script dies when debugging is switched off, and after a
reboot Home starts it again only while debugging is on. Standby keeps it running. To stop it, remove
`key_bridge` from `config.json` and push the theme again (the launcher then stops the script), or switch
debugging off.

## Notes for this set

- DNS: this network's DHCP hands out a local AdGuard Home, which already filters the TV, so Private DNS
  was left alone. With a hostname in `private_dns_specifier` the TV would bypass it.
- Firmware updates come through Play Services and are untouched.
- Screenshots come out black while HDMI, Live TV or Netflix is on screen; press Home first.
- Debugging stays on for `key_bridge` (see above). Without the bridge, switch it off when you're done; the
  launcher keeps working without it.
