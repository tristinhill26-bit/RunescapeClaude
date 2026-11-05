package com.runescapeclaude.hpprayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** A big rounded bar with "current / max" written across it, like a stat orb stretched out. */
final class StatBarView extends View {
	private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF rect = new RectF();
	private final float density;

	private String label;
	private String badge = "";
	private int current;
	private int max;
	private boolean known;

	StatBarView(Context context, String label, int color) {
		super(context);
		this.label = label;
		density = context.getResources().getDisplayMetrics().density;
		trackPaint.setColor(Color.argb(255, 44, 44, 50));
		fillPaint.setColor(color);
		valuePaint.setColor(Color.WHITE);
		valuePaint.setTextAlign(Paint.Align.CENTER);
		valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		valuePaint.setShadowLayer(4 * density, 0, density, Color.BLACK);
		labelPaint.setColor(Color.argb(220, 255, 255, 255));
		labelPaint.setTextSize(16 * density);
		labelPaint.setShadowLayer(3 * density, 0, density, Color.BLACK);
	}

	void setValues(int current, int max) {
		this.current = current;
		this.max = max;
		this.known = true;
		invalidate();
	}

	void clear() {
		known = false;
		invalidate();
	}

	void setColor(int color) {
		if (fillPaint.getColor() != color) {
			fillPaint.setColor(color);
			invalidate();
		}
	}

	void setBadge(String badge) {
		if (!this.badge.equals(badge)) {
			this.badge = badge;
			invalidate();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = (int) (110 * density);
		setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float w = getWidth();
		float h = getHeight();
		float radius = 18 * density;
		rect.set(0, 0, w, h);
		canvas.drawRoundRect(rect, radius, radius, trackPaint);

		if (known && max > 0) {
			// Boosted levels can go above the max (e.g. a Saradomin brew), so cap the bar at full
			float fraction = Math.max(0f, Math.min(1f, current / (float) max));
			if (fraction > 0) {
				canvas.save();
				canvas.clipRect(0, 0, w * fraction, h);
				canvas.drawRoundRect(rect, radius, radius, fillPaint);
				canvas.restore();
			}
		}

		float pad = 14 * density;
		canvas.drawText(label, pad, pad + labelPaint.getTextSize(), labelPaint);
		if (!badge.isEmpty()) {
			labelPaint.setTextAlign(Paint.Align.RIGHT);
			canvas.drawText(badge, w - pad, pad + labelPaint.getTextSize(), labelPaint);
			labelPaint.setTextAlign(Paint.Align.LEFT);
		}

		String value = known ? current + " / " + max : "–";
		valuePaint.setTextSize(Math.min(h * 0.45f, 48 * density));
		// Centre the number in the space below the label
		float baseline = h * 0.6f - (valuePaint.descent() + valuePaint.ascent()) / 2f;
		canvas.drawText(value, w / 2f, baseline, valuePaint);
	}
}
