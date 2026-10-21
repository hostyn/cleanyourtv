package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Spaces the favourites (dock) row from the grid and draws the "Apps" heading in the gap (DESIGN.md, Layout).
 * No band behind the dock: in Penumbra the favourites stand on the ground like every other tile.
 */
public class DockDecoration extends RecyclerView.ItemDecoration {

    /** Offset below the dock row: room for the heading and the gap under it. */
    static final float BELOW_DP = 44;

    private final int dockSize;
    private final float density;
    private final boolean rtl;
    private final String heading;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    DockDecoration(Context ctx, int dockSize, float density, boolean rtl) {
        this.dockSize = dockSize;
        this.density = density;
        this.rtl = rtl;
        this.heading = ctx.getString(R.string.apps_heading);
        text.setColor(0xFF9EA3AA);
        text.setTextSize(14 * ctx.getResources().getDisplayMetrics().scaledDensity);
        text.setTypeface(ctx.getResources().getFont(R.font.penumbra_medium));
        text.setTextAlign(rtl ? Paint.Align.RIGHT : Paint.Align.LEFT);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) (BELOW_DP * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        RecyclerView.Adapter<?> adapter = parent.getAdapter();
        if (adapter == null || adapter.getItemCount() <= dockSize) return;   // nothing below the favourites
        // The heading lines up with the first grid column's tile edge, under the dock row.
        float bottom = -1, edge = -1;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0) continue;
            if (pos < dockSize) bottom = Math.max(bottom, child.getBottom() + child.getTranslationY());
            if (pos == dockSize) {
                RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
                View card = vh instanceof AppAdapter.Holder ? ((AppAdapter.Holder) vh).card : null;
                if (card != null) edge = rtl ? child.getLeft() + card.getRight() : child.getLeft() + card.getLeft();
            }
        }
        if (bottom < 0 || edge < 0) return;
        c.drawText(heading, edge, bottom + 26 * density, text);
    }
}
