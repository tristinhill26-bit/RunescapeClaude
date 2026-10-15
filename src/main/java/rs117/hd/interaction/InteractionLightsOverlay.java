package rs117.hd.interaction;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.*;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import static net.runelite.api.Perspective.*;

class InteractionLightsOverlay extends Overlay {
	private static final int BEAM_HEIGHT = 520;
	private static final int LONG_RANGE_BEAM_HEIGHT = 2600;
	private static final float SCAN_RING_FADE = .5f;
	private static final int SCAN_RING_SEGMENTS = 72;

	private final Client client;
	private final InteractionLightsPlugin plugin;
	private final InteractionLightsConfig config;

	@Inject
	private InteractionLightsOverlay(Client client, InteractionLightsPlugin plugin, InteractionLightsConfig config) {
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D g) {
		if (client.getGameState() != GameState.LOGGED_IN)
			return null;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double now = plugin.getNow();

		renderScanRing(g, now);

		var hover = plugin.getHover();
		if (hover != null && config.markOnMouseOver() && !hover.target.isSameAs(markerTarget()))
			renderTileOutline(g, hover.target, config.mouseOverColor(), hover.alpha, false);

		var hintArrow = plugin.getHintArrow();
		if (hintArrow != null && hintArrow.awtColor != null && config.hintArrowMode() != HintArrowMode.OFF) {
			boolean longRange = plugin.isLongRange(hintArrow, false);
			renderBeam(g, hintArrow.target, hintArrow.awtColor, hintArrow.alpha, longRange);
			if (longRange)
				renderOffscreenArrow(g, hintArrow.target, hintArrow.awtColor, hintArrow.alpha);
		}

		for (var marker : plugin.getFadingMarkers())
			renderMarker(g, marker, now, false);

		var marker = plugin.getClickMarker();
		if (marker != null) {
			if (config.pathDots())
				renderPath(g, marker, now);
			renderMarker(g, marker, now, true);
		}

		return null;
	}

	@Nullable
	private MarkerTarget markerTarget() {
		var marker = plugin.getClickMarker();
		return marker == null ? null : marker.target;
	}

	private void renderMarker(Graphics2D g, LitTarget marker, double now, boolean active) {
		Color color = config.markerColor();
		float alpha = marker.alpha;
		if (alpha <= 0)
			return;

		boolean longRange = plugin.isLongRange(marker, plugin.isClickMarkerIsWalk());
		if (config.tileOutline())
			renderTileOutline(g, marker.target, color, alpha, active && config.flashOnClick());
		if (config.beam())
			renderBeam(g, marker.target, color, alpha, longRange);
		if (config.chevron())
			renderChevron(g, marker.target, color, alpha, now);
		if (active && longRange)
			renderOffscreenArrow(g, marker.target, color, alpha);
	}

	@Nullable
	private Polygon getTargetPolygon(MarkerTarget target) {
		if (target.location == null)
			return null;
		if (target.kind == MarkerTarget.Kind.TILE || target.kind == MarkerTarget.Kind.GROUND_ITEM)
			return getCanvasTilePoly(client, target.location);
		return getCanvasTileAreaPoly(client, target.location, Math.max(target.sizeX, target.sizeY));
	}

	private void renderTileOutline(Graphics2D g, MarkerTarget target, Color color, float alpha, boolean flash) {
		var polygon = getTargetPolygon(target);
		if (polygon == null)
			return;

		float flashBoost = 0;
		if (flash) {
			float t = (float) (plugin.getNow() - plugin.getClickTime()) / .35f;
			if (t >= 0 && t < 1)
				flashBoost = (1 - t) * (1 - t);
		}

		int fill = config.tileFill();
		if (fill > 0) {
			g.setColor(withAlpha(color, alpha * fill / 100f * (1 + flashBoost)));
			g.fill(polygon);
		}

		int width = config.outlineWidth();
		int feather = config.outlineFeather() + Math.round(flashBoost * 4);
		for (int i = feather; i > 0; i--) {
			float falloff = 1 - i / (feather + 1f);
			g.setStroke(new BasicStroke(width + i * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(withAlpha(color, alpha * .3f * falloff * falloff));
			g.draw(polygon);
		}
		g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(withAlpha(color, alpha));
		g.draw(polygon);
	}

	private void renderBeam(Graphics2D g, MarkerTarget target, Color color, float alpha, boolean longRange) {
		var location = target.location;
		if (location == null || alpha <= 0)
			return;

		int height = longRange ? LONG_RANGE_BEAM_HEIGHT : Math.max(BEAM_HEIGHT, target.height + 300);
		Point base = localToCanvas(client, location, target.plane, 0);
		Point top = localToCanvas(client, location, target.plane, height);
		var tilePolygon = getCanvasTilePoly(client, location);
		if (base == null || top == null || tilePolygon == null)
			return;

		// Scale the beam's width with the on-screen size of a tile, so it shrinks with distance
		Rectangle bounds = tilePolygon.getBounds();
		float tileWidth = Math.max(4, Math.max(bounds.width, bounds.height));
		float width = Math.max(2, tileWidth * .14f);

		float dx = top.getX() - base.getX();
		float dy = top.getY() - base.getY();
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 1)
			return;
		// Perpendicular unit vector
		float px = -dy / length;
		float py = dx / length;

		// Ground glow
		float glowRadius = tileWidth * .6f;
		var glow = new RadialGradientPaint(
			new Point2D.Float(base.getX(), base.getY()),
			glowRadius,
			new float[] { 0, 1 },
			new Color[] { withAlpha(color, alpha * .55f), withAlpha(color, 0) },
			MultipleGradientPaint.CycleMethod.NO_CYCLE
		);
		g.setPaint(glow);
		g.fill(new Ellipse2D.Float(base.getX() - glowRadius, base.getY() - glowRadius * .45f, glowRadius * 2, glowRadius * .9f));

		// Soft outer glow, inner beam and a bright core
		float[][] layers = {
			{ 3.2f, .16f },
			{ 1.6f, .35f },
			{ .55f, .95f },
		};
		for (float[] layer : layers) {
			float halfBase = width * layer[0] / 2;
			float halfTop = halfBase * .35f;
			var shape = new GeneralPath();
			shape.moveTo(base.getX() + px * halfBase, base.getY() + py * halfBase);
			shape.lineTo(top.getX() + px * halfTop, top.getY() + py * halfTop);
			shape.lineTo(top.getX() - px * halfTop, top.getY() - py * halfTop);
			shape.lineTo(base.getX() - px * halfBase, base.getY() - py * halfBase);
			shape.closePath();
			g.setPaint(new GradientPaint(
				base.getX(), base.getY(), withAlpha(color, alpha * layer[1]),
				top.getX(), top.getY(), withAlpha(color, 0)
			));
			g.fill(shape);
		}
	}

	private void renderChevron(Graphics2D g, MarkerTarget target, Color color, float alpha, double now) {
		if (target.location == null)
			return;

		float bounce = config.chevronBounce() ? (float) Math.abs(Math.sin(now * 3.5)) : 0;
		int height = target.height + 140 + Math.round(bounce * 60);
		Point point = localToCanvas(client, target.location, target.plane, height);
		if (point == null)
			return;

		float size = 9 * config.chevronSize() / 100f;
		var shape = new GeneralPath();
		shape.moveTo(point.getX() - size, point.getY() - size * .6f);
		shape.lineTo(point.getX(), point.getY() + size * .6f);
		shape.lineTo(point.getX() + size, point.getY() - size * .6f);

		g.setStroke(new BasicStroke(size * .65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(withAlpha(Color.BLACK, alpha * .6f));
		g.draw(shape);
		g.setStroke(new BasicStroke(size * .35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(withAlpha(color, alpha));
		g.draw(shape);
	}

	private void renderPath(Graphics2D g, LitTarget marker, double now) {
		var path = plugin.getPath();
		if (path.size() < 2)
			return;

		var wv = client.getTopLevelWorldView();
		int plane = wv.getPlane();
		Color color = config.markerColor();
		// Skip the player's tile, and the destination tile if it's already marked
		int end = path.size();
		int last = path.get(end - 1);
		if (marker.target.tileDistanceTo(LocalPoint.fromScene(last >>> 16, last & 0xFFFF, wv)) == 0)
			end--;
		for (int i = 1; i < end; i++) {
			int packed = path.get(i);
			var location = LocalPoint.fromScene(packed >>> 16, packed & 0xFFFF, wv);
			Point point = localToCanvas(client, location, plane, 0);
			var tilePolygon = getCanvasTilePoly(client, location);
			if (point == null || tilePolygon == null)
				continue;

			Rectangle bounds = tilePolygon.getBounds();
			float radius = Math.max(1.5f, Math.max(bounds.width, bounds.height) * .05f);
			// A gentle wave travelling towards the destination
			float wave = .65f + .35f * (float) Math.sin(now * 6 - i * .8);
			g.setColor(withAlpha(color, marker.alpha * wave));
			g.fill(new Ellipse2D.Float(point.getX() - radius, point.getY() - radius * .6f, radius * 2, radius * 1.2f));
		}
	}

	private void renderOffscreenArrow(Graphics2D g, MarkerTarget target, Color color, float alpha) {
		if (target.location == null)
			return;

		int vx = client.getViewportXOffset();
		int vy = client.getViewportYOffset();
		int vw = client.getViewportWidth();
		int vh = client.getViewportHeight();
		float cx = vx + vw / 2f;
		float cy = vy + vh / 2f;
		float margin = 28;

		Point point = localToCanvas(client, target.location, target.plane, target.height / 2);
		float dirX, dirY;
		if (point == null) {
			// Behind the camera, so point downwards
			dirX = 0;
			dirY = 1;
		} else {
			if (point.getX() >= vx + margin && point.getX() <= vx + vw - margin &&
				point.getY() >= vy + margin && point.getY() <= vy + vh - margin)
				return; // On screen
			dirX = point.getX() - cx;
			dirY = point.getY() - cy;
		}

		float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
		if (length < 1)
			return;
		dirX /= length;
		dirY /= length;

		// Place the arrow where the direction meets the edge of the viewport
		float halfW = vw / 2f - margin;
		float halfH = vh / 2f - margin;
		float scale = Math.min(
			Math.abs(dirX) < 1e-4 ? Float.MAX_VALUE : halfW / Math.abs(dirX),
			Math.abs(dirY) < 1e-4 ? Float.MAX_VALUE : halfH / Math.abs(dirY)
		);
		float ax = cx + dirX * scale;
		float ay = cy + dirY * scale;

		float size = 14;
		float px = -dirY;
		float py = dirX;
		var arrow = new GeneralPath();
		arrow.moveTo(ax + dirX * size, ay + dirY * size);
		arrow.lineTo(ax - dirX * size * .6f + px * size * .75f, ay - dirY * size * .6f + py * size * .75f);
		arrow.lineTo(ax - dirX * size * .2f, ay - dirY * size * .2f);
		arrow.lineTo(ax - dirX * size * .6f - px * size * .75f, ay - dirY * size * .6f - py * size * .75f);
		arrow.closePath();

		float pulse = .75f + .25f * (float) Math.sin(plugin.getNow() * 4);
		g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(withAlpha(Color.BLACK, alpha * .5f));
		g.draw(arrow);
		g.setColor(withAlpha(color, alpha * pulse));
		g.fill(arrow);
	}

	private void renderScanRing(Graphics2D g, double now) {
		var center = plugin.getScanCenter();
		if (center == null)
			return;

		float sweepTime = config.scanSweepTime() / 1000f;
		float elapsed = (float) (now - plugin.getScanStartTime());
		if (elapsed < 0 || elapsed > sweepTime + SCAN_RING_FADE)
			return;

		float progress = Math.min(1, elapsed / sweepTime);
		float alpha = elapsed <= sweepTime ? 1 : 1 - (elapsed - sweepTime) / SCAN_RING_FADE;
		// Ease out, so the pulse slows down as it reaches the edge
		float radius = (1 - (1 - progress) * (1 - progress)) * config.scanRadius() * LOCAL_TILE_SIZE;
		if (radius < LOCAL_HALF_TILE_SIZE)
			return;

		var ring = buildGroundCircle(center, plugin.getScanPlane(), radius);
		Color color = config.markerColor();
		g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(withAlpha(color, alpha * .18f));
		g.draw(ring);
		g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(withAlpha(color, alpha * .8f));
		g.draw(ring);
	}

	/**
	 * Project a circle on the ground into a canvas shape, following the terrain height
	 */
	private Shape buildGroundCircle(LocalPoint center, int plane, float radius) {
		var shape = new GeneralPath();
		boolean penDown = false;
		for (int i = 0; i <= SCAN_RING_SEGMENTS; i++) {
			double angle = Math.PI * 2 * i / SCAN_RING_SEGMENTS;
			int x = center.getX() + (int) (Math.cos(angle) * radius);
			int y = center.getY() + (int) (Math.sin(angle) * radius);
			var location = new LocalPoint(x, y, center.getWorldView());
			Point point = location.isInScene() ? localToCanvas(client, location, plane, 0) : null;
			if (point == null) {
				penDown = false;
				continue;
			}
			if (penDown) {
				shape.lineTo(point.getX(), point.getY());
			} else {
				shape.moveTo(point.getX(), point.getY());
				penDown = true;
			}
		}
		return shape;
	}

	private static Color withAlpha(Color color, float alpha) {
		int a = Math.round(Math.max(0, Math.min(1, alpha * color.getAlpha() / 255f)) * 255);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
	}
}
