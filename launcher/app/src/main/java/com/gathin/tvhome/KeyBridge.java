package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Remote buttons that only the stock launcher answered, without an accessibility service (key_bridge).
 *
 * A key-filtering accessibility service (HomeKeyService, InputKeyService) gets those buttons back, but while it is on
 * Android re-injects every key it sees without key repeats, and on TCL Google TV it never sees Volume at all: holding
 * Volume or an arrow moves one step only (examples/tcl-g08-google-tv). The bridge instead runs a small script as the
 * shell user, started through the TV's own network ADB (LocalAdb): `getevent` reads the remote's events in parallel,
 * grabbing nothing, and `am start` opens the app for each "scan:" entry of app_keys. Every other key, repeats
 * included, reaches Android untouched.
 *
 * It needs network debugging left on: the script dies when debugging is switched off and with a reboot, and the
 * launcher starts it again whenever Home starts. Standby keeps it running. The first start asks to allow this
 * launcher's ADB key on screen.
 */
final class KeyBridge {
    private KeyBridge() { }

    private static final String TAG = "KeyBridge";
    static final String SCRIPT = "/data/local/tmp/cleanyourtv-keys.sh";
    static final String PID = "/data/local/tmp/cleanyourtv-keys.pid";
    /** Target that opens the TV's Settings instead of an app: "scan:561:settings". */
    static final String SETTINGS = "settings";

    private static boolean started;

    /** Starts (or replaces, or stops) the script for this config, once per launcher process, off the main thread. */
    static synchronized void ensure(Context context, Config config) {
        if (started) return;
        started = true;
        Context app = context.getApplicationContext();
        Map<Integer, String> scancodes = RemoteKeys.parseAppScancodes(config.appKeys);
        boolean wanted = config.keyBridge && !scancodes.isEmpty();
        File key = new File(app.getFilesDir(), "adbkey");
        // Nothing to start and never set up: don't touch ADB at all (no prompt on TVs that don't use this).
        if (!wanted && !key.isFile()) return;
        new Thread(() -> {
            try {
                String command = wanted ? startCommand(script(commands(app, scancodes))) : stopCommand();
                String result = LocalAdb.shell(key, "cleanyourtv@" + android.os.Build.MODEL, command);
                Log.i(TAG, result.trim());
            } catch (Exception e) {
                // Network debugging off, prompt declined or timed out: the buttons stay as they are.
                Log.w(TAG, "not started: " + e);
            }
        }, TAG).start();
    }

    /** scancode -> shell command that opens its target, resolved now so the script needs no launcher at run time. */
    private static Map<Integer, String> commands(Context ctx, Map<Integer, String> scancodes) {
        Map<Integer, String> commands = new TreeMap<>();
        for (Map.Entry<Integer, String> e : scancodes.entrySet()) {
            String target = e.getValue();
            if (SETTINGS.equals(target)) {
                commands.put(e.getKey(), "am start -a android.settings.SETTINGS");
                continue;
            }
            Intent open = ctx.getPackageManager().getLeanbackLaunchIntentForPackage(target);
            if (open == null) open = ctx.getPackageManager().getLaunchIntentForPackage(target);
            ComponentName c = open != null ? open.getComponent() : null;
            if (c != null) commands.put(e.getKey(), "am start -n " + c.flattenToShortString());
        }
        return commands;
    }

    /**
     * The script: getevent on every input device (hotplugged remotes included), and on each key press (EV_KEY,
     * value 1) whose code is listed, the matching command. getevent prints the code as four hex digits.
     */
    static String script(Map<Integer, String> commands) {
        StringBuilder s = new StringBuilder();
        s.append("#!/system/bin/sh\n")
                .append("# Written by the Clean Your TV launcher (key_bridge). Remote buttons -> apps.\n")
                .append("echo $$ > ").append(PID).append('\n')
                .append("getevent | while read dev type code value; do\n")
                .append("  [ \"$type\" = 0001 ] && [ \"$value\" = 00000001 ] || continue\n")
                .append("  case \"$code\" in\n");
        for (Map.Entry<Integer, String> e : commands.entrySet()) {
            s.append(String.format(Locale.ROOT, "    %04x) %s >/dev/null 2>&1 ;;\n", e.getKey(), e.getValue()));
        }
        return s.append("  esac\ndone\n").toString();
    }

    /** Kills a running copy: the script's own shell and its getevent. */
    private static String stopRunning() {
        return "if [ -f " + PID + " ]; then p=$(cat " + PID + "); pkill -P $p; kill $p; rm -f " + PID + "; fi 2>/dev/null; ";
    }

    /** Leaves an identical running script alone; otherwise replaces it and starts it detached from adbd. */
    static String startCommand(String script) {
        String b64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
        String alive = "[ -f " + PID + " ] && grep -q cleanyourtv-keys /proc/$(cat " + PID + ")/cmdline 2>/dev/null";
        List<String> c = new ArrayList<>();
        c.add("echo " + b64 + " | base64 -d > " + SCRIPT + ".new");
        c.add("if cmp -s " + SCRIPT + ".new " + SCRIPT + " && " + alive + "; then rm -f " + SCRIPT + ".new; echo running; "
                + "else " + stopRunning() + "mv " + SCRIPT + ".new " + SCRIPT + "; chmod 755 " + SCRIPT + "; "
                + "(setsid nohup " + SCRIPT + " >/dev/null 2>&1 </dev/null &); "
                // adbd ends the session's processes when the connection closes: wait until the script has its
                // own session (it writes the pid file first thing) before answering.
                + "for i in 1 2 3 4 5 6 7 8 9 10; do " + alive + " && break; sleep 0.3; done; "
                + "if " + alive + "; then echo started; else echo failed; fi; fi");
        return String.join("; ", c);
    }

    static String stopCommand() {
        return stopRunning() + "rm -f " + SCRIPT + "; echo stopped";
    }
}
