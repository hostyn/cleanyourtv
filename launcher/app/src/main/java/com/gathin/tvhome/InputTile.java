package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A TV input shown as a tile among the apps (config.xml or a theme's input_tiles): an HDMI port as the Inputs
 * menu names it ("HDMI 2"), a TvInputManager input id, or {@link #LIVE_TV} for the Live TV app. The tile has no
 * banner; the launcher draws its icon and label.
 */
final class InputTile {
    static final String LIVE_TV = "live_tv";
    /** dock_size bounds: fewer than 3 favourites is not a dock, more than 6 makes them smaller than grid tiles. */
    static final int MIN_DOCK = 3, MAX_DOCK = 6;
    /** dock_apps entries naming an input tile start with this, followed by the tile's input. */
    static final String DOCK_PREFIX = "input:";

    final String input;
    final String label;
    final String icon;

    private InputTile(String input, String label, String icon) {
        this.input = input;
        this.label = label;
        this.icon = icon;
    }

    /** null when the input is empty; the label defaults to the input, the icon to the generic input icon. */
    static InputTile of(String input, String label, String icon) {
        input = input == null ? "" : input.trim();
        if (input.isEmpty() || input.length() > 128) return null;
        label = label == null ? "" : label.trim();
        if (label.isEmpty()) label = LIVE_TV.equals(input) ? "TV" : input;
        if (label.length() > 24) label = label.substring(0, 24);
        return new InputTile(input, label, icon == null ? "" : icon.trim().toLowerCase(Locale.ROOT));
    }

    /** "input|label|icon"; label and icon are optional. */
    static InputTile parse(String entry) {
        if (entry == null) return null;
        String[] parts = entry.split("\\|", -1);
        return of(parts[0], parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : "");
    }

    static List<InputTile> parseAll(List<String> entries) {
        List<InputTile> out = new ArrayList<>();
        for (String e : entries) {
            InputTile t = parse(e);
            if (t != null) out.add(t);
        }
        return out;
    }

    /** The key dock_apps and the saved order use for this tile. */
    String key() {
        return DOCK_PREFIX + input;
    }

    /** The drawable for this tile's icon name. */
    int iconRes() {
        switch (icon) {
            case "gamepad": return R.drawable.ic_gamepad;
            case "tv": return R.drawable.ic_livetv;
            case "box": return R.drawable.ic_streambox;
            case "computer": return R.drawable.ic_computer;
            case "player": return R.drawable.ic_player;
            default: return LIVE_TV.equals(input) ? R.drawable.ic_livetv : R.drawable.ic_input;
        }
    }

    /** Whether this names an HDMI port by its menu name ("HDMI 2", "hdmi2"), and which number. 0 when not. */
    static int hdmiNumber(String input) {
        String s = input.toUpperCase(Locale.ROOT).replace(" ", "");
        if (!s.startsWith("HDMI")) return 0;
        try {
            return Integer.parseInt(s.substring(4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
