# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Primary: the owner of one debloated TCL Google TV (Android 14), using the home screen from the sofa at
about three metres with an IR remote (D-pad, OK, Back, Home). The job is short and frequent: land on Home,
pick one of a handful of apps or an HDMI input, get back to watching. Secondary (possible fork or PR):
anyone who has run CleanYourTV on their own Android TV and installed its launcher.

## Product Purpose
CleanYourTV strips ads and bloat from Android TV / Google TV over ADB and replaces the stock, ad-filled home
screen with a small native launcher. The launcher's job is to be a calm, fast, ad-free Home: the apps you
chose, your inputs, the time, and nothing that sells you anything. Success: the screen feels as composed as a
premium set-top box, and getting to an app is one or two presses.

## Positioning
No ads, no recommendation rows, no network polling, no account: a ~400 KB native launcher the owner builds
and installs themself, configured by a JSON theme. Anything that looks like a content feed or a storefront is
off-brand.

## Operating Context
Living-room TV, 1920x1080 panel, viewed at distance, often in a dim room. Navigation is D-pad only, so focus
must always be obvious and movement deterministic. The launcher sits between apps (Netflix, Prime Video,
Jellyfin, TizenTube, Disney+, Apple TV, RTVE, Atresplayer, Twitch) and inputs (HDMI 1-4, Live TV).

## Capabilities and Constraints
- Native Android Views (Java, RecyclerView), minSdk 26; must run on TVs with 1-2 GB RAM.
- No blur, no video, no network polling. Gradients, shadows, opacity, scale and translate animation are fine.
- App artwork comes from each app's own leanback banner (16:9 bitmap); the launcher cannot restyle it.
- Elements that exist today and stay functional: dock of chosen apps, grid of the rest, focus highlight,
  focused app name, Inputs menu (HDMI, AV, Live TV), Wi-Fi/network, Settings, clock and date, greeting.
  Their visual form is open: the owner asked for a full redesign.
- Themes (config.json) can change dock order, hidden apps, columns and colours; keep that hook working.

## Brand Commitments
- Name: Clean Your TV. Apple TV-like calm is the stated aspiration of the project.
- The owner's verdict on the current look (Outfit type, capsule pills with borders, blue/purple glow
  background, oversized app name and empty space, very large tiles): "cutre, como de niño pequeño".
  Wanted instead: modern and minimalist, but not excessively flat.

## Evidence on Hand
- Real app banners are on the TV; mockups use stand-ins and must say so.
- docs/new-home.jpg and docs/stock-vs-new.jpg show the current launcher.

## Product Principles
1. Nothing on Home competes with the content you are about to watch.
2. Focus is the interface: at three metres the focused thing must be unmistakable, everything else quiet.
3. Fast and light beats rich: every effect must be cheap on a 1 GB TV.
4. Adult, composed, finished: no toy-like rounding, no decorative colour for its own sake.

## Accessibility & Inclusion
Ten-foot UI: legible type at distance (body no smaller than ~24px at 1080p), strong focus contrast, no
information carried by colour alone.
