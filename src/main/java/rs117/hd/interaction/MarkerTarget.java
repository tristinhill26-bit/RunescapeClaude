package rs117.hd.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.runelite.api.*;
import net.runelite.api.coords.*;

import static net.runelite.api.Perspective.*;

/**
 * Something which can be marked and lit: a tile, an object, an actor or a ground item.
 * Tracks the target's position and footprint, and whether it still exists.
 */
class MarkerTarget {
	enum Kind {
		TILE,
		OBJECT,
		ACTOR,
		GROUND_ITEM
	}

	final Kind kind;
	@Nullable
	final TileObject object;
	@Nullable
	final Actor actor;
	final int itemId;
	final String name;
	final int plane;

	/**
	 * The center of the target, in top-level scene local coordinates
	 */
	LocalPoint location;
	/**
	 * The south-west tile of the target's footprint
	 */
	LocalPoint minTile;
	int sizeX = 1;
	int sizeY = 1;
	/**
	 * Approximate height of the target's model, in local units
	 */
	int height;
	boolean gone;

	private MarkerTarget(Kind kind, LocalPoint location, int plane, @Nullable TileObject object, @Nullable Actor actor, int itemId, String name) {
		this.kind = kind;
		this.location = location;
		this.minTile = location;
		this.plane = plane;
		this.object = object;
		this.actor = actor;
		this.itemId = itemId;
		this.name = name == null ? "" : name;
	}

	static MarkerTarget tile(LocalPoint location, int plane) {
		return new MarkerTarget(Kind.TILE, location, plane, null, null, -1, "");
	}

	static MarkerTarget object(TileObject object, String name) {
		var target = new MarkerTarget(Kind.OBJECT, object.getLocalLocation(), object.getPlane(), object, null, -1, name);
		target.height = getModelHeight(object);
		if (object instanceof GameObject) {
			var gameObject = (GameObject) object;
			target.sizeX = gameObject.sizeX();
			target.sizeY = gameObject.sizeY();
		}
		target.updateMinTile();
		return target;
	}

	static MarkerTarget actor(Actor actor, int plane) {
		var target = new MarkerTarget(Kind.ACTOR, actor.getLocalLocation(), plane, null, actor, -1, actor.getName());
		if (actor instanceof NPC) {
			var composition = ((NPC) actor).getTransformedComposition();
			if (composition != null)
				target.sizeX = target.sizeY = composition.getSize();
		}
		target.height = actor.getLogicalHeight();
		target.updateMinTile();
		return target;
	}

	static MarkerTarget groundItem(LocalPoint location, int plane, int itemId, String name) {
		var target = new MarkerTarget(Kind.GROUND_ITEM, location, plane, null, null, itemId, name);
		target.height = 32;
		return target;
	}

	/**
	 * Whether this targets the same thing as the other target
	 */
	boolean isSameAs(@Nullable MarkerTarget other) {
		if (other == null || other.kind != kind)
			return false;
		switch (kind) {
			case OBJECT:
				return other.object == object;
			case ACTOR:
				return other.actor == actor;
			case GROUND_ITEM:
				return other.itemId == itemId && other.location.equals(location);
			default:
				return other.location.equals(location) && other.plane == plane;
		}
	}

	/**
	 * Refresh the target's position, and check whether it still exists.
	 *
	 * @return true if the target still exists
	 */
	boolean update(@Nonnull Client client) {
		if (gone)
			return false;

		var wv = client.getTopLevelWorldView();
		switch (kind) {
			case OBJECT:
				assert object != null;
				gone = !objectExists(wv, object);
				break;
			case ACTOR:
				assert actor != null;
				if (actor instanceof NPC) {
					var npc = (NPC) actor;
					gone = wv.npcs().byIndex(npc.getIndex()) != npc;
				} else if (actor instanceof Player) {
					var player = (Player) actor;
					gone = wv.players().byIndex(player.getId()) != player;
				}
				if (!gone) {
					var lp = actor.getLocalLocation();
					if (lp != null) {
						location = lp;
						updateMinTile();
					}
				}
				break;
			case GROUND_ITEM:
				gone = !groundItemExists(wv, location, plane, itemId);
				break;
			default:
				break;
		}
		return !gone;
	}

	/**
	 * Chebyshev distance in tiles from the given point to the nearest tile of the target's footprint
	 */
	int tileDistanceTo(LocalPoint point) {
		int x = point.getSceneX();
		int y = point.getSceneY();
		int minX = minTile.getSceneX();
		int minY = minTile.getSceneY();
		int dx = Math.max(0, Math.max(minX - x, x - (minX + sizeX - 1)));
		int dy = Math.max(0, Math.max(minY - y, y - (minY + sizeY - 1)));
		return Math.max(dx, dy);
	}

	private void updateMinTile() {
		minTile = new LocalPoint(
			location.getX() - (sizeX - 1) * LOCAL_HALF_TILE_SIZE,
			location.getY() - (sizeY - 1) * LOCAL_HALF_TILE_SIZE,
			location.getWorldView()
		);
	}

	static int getModelHeight(TileObject object) {
		Renderable renderable = null;
		if (object instanceof GameObject) {
			renderable = ((GameObject) object).getRenderable();
		} else if (object instanceof WallObject) {
			renderable = ((WallObject) object).getRenderable1();
		} else if (object instanceof DecorativeObject) {
			renderable = ((DecorativeObject) object).getRenderable();
		} else if (object instanceof GroundObject) {
			renderable = ((GroundObject) object).getRenderable();
		}

		int height = renderable == null ? 0 : renderable.getModelHeight();
		// Some renderables don't report a height, so fall back to roughly the height of a character
		return height > 0 ? height : 150;
	}

	@Nullable
	static Tile getTile(WorldView wv, LocalPoint location, int plane) {
		int x = location.getSceneX();
		int y = location.getSceneY();
		var tiles = wv.getScene().getTiles();
		if (plane < 0 || plane >= tiles.length || x < 0 || y < 0 || x >= tiles[plane].length || y >= tiles[plane][x].length)
			return null;
		return tiles[plane][x][y];
	}

	@Nullable
	static TileObject findObject(@Nullable Tile tile, int id) {
		if (tile == null)
			return null;
		for (var gameObject : tile.getGameObjects())
			if (gameObject != null && gameObject.getId() == id)
				return gameObject;
		var wall = tile.getWallObject();
		if (wall != null && wall.getId() == id)
			return wall;
		var decoration = tile.getDecorativeObject();
		if (decoration != null && decoration.getId() == id)
			return decoration;
		var ground = tile.getGroundObject();
		if (ground != null && ground.getId() == id)
			return ground;
		return null;
	}

	private static boolean objectExists(WorldView wv, TileObject object) {
		var lp = object.getLocalLocation();
		if (lp == null)
			return false;

		// Large objects are stored on the tile at their south-west corner
		if (object instanceof GameObject) {
			var min = ((GameObject) object).getSceneMinLocation();
			lp = LocalPoint.fromScene(min.getX(), min.getY(), wv);
		}

		var tile = getTile(wv, lp, object.getPlane());
		if (tile == null)
			return false;
		for (var gameObject : tile.getGameObjects())
			if (gameObject == object)
				return true;
		return tile.getWallObject() == object || tile.getDecorativeObject() == object || tile.getGroundObject() == object;
	}

	private static boolean groundItemExists(WorldView wv, LocalPoint location, int plane, int itemId) {
		var tile = getTile(wv, location, plane);
		if (tile == null || tile.getGroundItems() == null)
			return false;
		for (var item : tile.getGroundItems())
			if (item.getId() == itemId)
				return true;
		return false;
	}
}
