package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

/**
 * Halo Sans on Android 7 (API 24 and 25). Before Android 8, TextView ignores android:fontFamily="@font/..." and
 * Resources#getFont does not exist, so every label would come out in Roboto. On those versions this installs a
 * LayoutInflater factory that gives each TextView asking for a font resource the bundled Halo Sans, loaded once
 * with ResourcesCompat. The weights in res/font are dropped, as Android 8 already drops them. From Android 8
 * on, nothing here runs and the platform reads the layouts itself.
 */
final class LegacyFonts {
    private LegacyFonts() { }

    private static final boolean NEEDED = Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
    /** Where PhoneLayoutInflater looks for a bare tag such as TextView. */
    private static final String[] PREFIXES = {"android.widget.", "android.webkit.", "android.app.", "android.view."};
    private static final int[] FONT_FAMILY = {android.R.attr.fontFamily};

    private static Typeface halo;

    /** A font resource as a Typeface, on every API level. */
    static Typeface get(Context context, int fontRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return context.getResources().getFont(fontRes);
        if (halo == null) halo = ResourcesCompat.getFont(context, R.font.halo_sans);
        return halo;
    }

    /** Call before the first inflate (setContentView) with this inflater; dialogs' clones inherit it. */
    static void install(LayoutInflater inflater) {
        if (!NEEDED || inflater.getFactory2() != null) return;
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                View view = create(inflater, name, attrs);
                if (view instanceof TextView) apply(context, (TextView) view, attrs);
                return view;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                return onCreateView(null, name, context, attrs);
            }
        });
    }

    /** The view the inflater would have made, or null to let it make the view itself. */
    private static View create(LayoutInflater inflater, String name, AttributeSet attrs) {
        if (name.indexOf('.') >= 0) {
            try {
                return inflater.createView(name, null, attrs);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        for (String prefix : PREFIXES) {
            try {
                return inflater.createView(name, prefix, attrs);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    @SuppressLint("ResourceType")   // a one-attribute int[], not a generated styleable
    private static void apply(Context context, TextView view, AttributeSet attrs) {
        // Reads the attribute from the tag or its style="", the two places the layouts set it.
        TypedArray a = context.obtainStyledAttributes(attrs, FONT_FAMILY);
        int id = a.getResourceId(0, 0);
        a.recycle();
        if (id != 0 && "font".equals(context.getResources().getResourceTypeName(id))) {
            view.setTypeface(get(context, id));
        }
    }
}
