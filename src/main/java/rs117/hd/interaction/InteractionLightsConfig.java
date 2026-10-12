package rs117.hd.interaction;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import static rs117.hd.interaction.InteractionLightsConfig.*;

@ConfigGroup(CONFIG_GROUP)
public interface InteractionLightsConfig extends Config
{
	String CONFIG_GROUP = "hdinteractionlights";

	/*====== Click marker ======*/

	@ConfigSection(
		name = "Click marker",
		description = "A light beam and tile marker on whatever you last clicked",
		position = 0
	)
	String clickMarkerSection = "clickMarkerSection";

	@ConfigItem(
		keyName = "clickMarker",
		name = "Click marker",
		description = "Mark the tile, object, NPC or item you last clicked with a beam of light.",
		position = 0,
		section = clickMarkerSection
	)
	default boolean clickMarker() {
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "markerColor",
		name = "Marker colour",
		description = "Colour of the beam, tile marker, path dots and the light they cast.",
		position = 1,
		section = clickMarkerSection
	)
	default Color markerColor() {
		return new Color(0xE6, 0xF5, 0x5A, 0xE6);
	}

	@Range(max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "markerLight",
		name = "Marker light",
		description = "How strongly the marker lights up its surroundings. 0% disables the light.",
		position = 2,
		section = clickMarkerSection
	)
	default int markerLight() {
		return 100;
	}

	@ConfigItem(
		keyName = "beam",
		name = "Light beam",
		description = "Draw a vertical beam of light above the marker.",
		position = 3,
		section = clickMarkerSection
	)
	default boolean beam() {
		return true;
	}

	@ConfigItem(
		keyName = "pathDots",
		name = "Path dots",
		description = "Draw dots along the path your character will walk to reach the marker.",
		position = 4,
		section = clickMarkerSection
	)
	default boolean pathDots() {
		return true;
	}

	@ConfigItem(
		keyName = "tileOutline",
		name = "Tile outline",
		description = "Outline the marked tile, or the tiles covered by the marked object.",
		position = 5,
		section = clickMarkerSection
	)
	default boolean tileOutline() {
		return true;
	}

	@Range(min = 1, max = 6)
	@ConfigItem(
		keyName = "outlineWidth",
		name = "Outline width",
		description = "Width of the tile outline in pixels.",
		position = 6,
		section = clickMarkerSection
	)
	default int outlineWidth() {
		return 2;
	}

	@Range(max = 8)
	@ConfigItem(
		keyName = "outlineFeather",
		name = "Outline feather",
		description = "How far the outline's soft glow extends, in pixels.",
		position = 7,
		section = clickMarkerSection
	)
	default int outlineFeather() {
		return 2;
	}

	@Range(max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "tileFill",
		name = "Tile fill",
		description = "Opacity of the fill inside the tile outline.",
		position = 8,
		section = clickMarkerSection
	)
	default int tileFill() {
		return 38;
	}

	@ConfigItem(
		keyName = "flashOnClick",
		name = "Flash on click",
		description = "Briefly flash the marker light when you click.",
		position = 9,
		section = clickMarkerSection
	)
	default boolean flashOnClick() {
		return true;
	}

	/*====== Mouse-over ======*/

	@ConfigSection(
		name = "Mouse-over",
		description = "Light up whatever is under your cursor",
		position = 1
	)
	String mouseOverSection = "mouseOverSection";

	@ConfigItem(
		keyName = "markOnMouseOver",
		name = "Mark on mouse-over",
		description = "Outline the tile, object, NPC or item under your cursor.",
		position = 0,
		section = mouseOverSection
	)
	default boolean markOnMouseOver() {
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "mouseOverColor",
		name = "Mouse-over colour",
		description = "Colour of the mouse-over outline and light.",
		position = 1,
		section = mouseOverSection
	)
	default Color mouseOverColor() {
		return new Color(0xB1, 0xA3, 0x8D, 0xA0);
	}

	@Range(max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "mouseOverLight",
		name = "Mouse-over light",
		description = "How strongly the thing under your cursor is lit. 0% disables the light.",
		position = 2,
		section = mouseOverSection
	)
	default int mouseOverLight() {
		return 35;
	}

	/*====== Chevron ======*/

	@ConfigSection(
		name = "Chevron",
		description = "An arrow floating above the marker",
		position = 2
	)
	String chevronSection = "chevronSection";

	@ConfigItem(
		keyName = "chevron",
		name = "Chevron",
		description = "Draw a downward-pointing chevron above the marker.",
		position = 0,
		section = chevronSection
	)
	default boolean chevron() {
		return false;
	}

	@ConfigItem(
		keyName = "chevronBounce",
		name = "Bounce",
		description = "Make the chevron bob up and down.",
		position = 1,
		section = chevronSection
	)
	default boolean chevronBounce() {
		return true;
	}

	@Range(min = 25, max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "chevronSize",
		name = "Size",
		description = "Size of the chevron.",
		position = 2,
		section = chevronSection
	)
	default int chevronSize() {
		return 100;
	}

	/*====== Long range ======*/

	@ConfigSection(
		name = "Long range",
		description = "Make far away markers and hint arrows easier to spot",
		position = 3
	)
	String longRangeSection = "longRangeSection";

	@ConfigItem(
		keyName = "hintArrowMode",
		name = "Game's hint arrow",
		description = "Mark the target of the game's own hint arrow, such as quest and clue targets.",
		position = 0,
		section = longRangeSection
	)
	default HintArrowMode hintArrowMode() {
		return HintArrowMode.BEAM_AND_LIGHT;
	}

	@Range(min = 1, max = 64)
	@Units(" tiles")
	@ConfigItem(
		keyName = "longRangeDistance",
		name = "Long range distance",
		description = "Markers further away than this get a taller beam and an arrow at the screen edge when off-screen.",
		position = 1,
		section = longRangeSection
	)
	default int longRangeDistance() {
		return 10;
	}

	@ConfigItem(
		keyName = "longRangeWhenWalking",
		name = "Long range when walking",
		description = "Also use the long range beam and edge arrow for plain 'Walk here' clicks.",
		position = 2,
		section = longRangeSection
	)
	default boolean longRangeWhenWalking() {
		return true;
	}

	/*====== Behaviour ======*/

	@ConfigSection(
		name = "Behaviour",
		description = "When the click marker goes away",
		position = 4
	)
	String behaviourSection = "behaviourSection";

	@ConfigItem(
		keyName = "clearOnArrival",
		name = "Clear on arrival",
		description = "Fade out 'Walk here' markers once you reach the tile.",
		position = 0,
		section = behaviourSection
	)
	default boolean clearOnArrival() {
		return true;
	}

	@Range(max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "forgetAfter",
		name = "Forget after",
		description = "Fade out the marker after this many seconds. 0 keeps it until something replaces it.",
		position = 1,
		section = behaviourSection
	)
	default int forgetAfter() {
		return 0;
	}

	@ConfigItem(
		keyName = "keepAfterTargetGone",
		name = "Keep after it is gone",
		description = "Keep marking the tile after the clicked object, NPC or item disappears, e.g. a tree that was chopped down.",
		position = 2,
		section = behaviourSection
	)
	default boolean keepAfterTargetGone() {
		return false;
	}

	/*====== Scan pulse ======*/

	@ConfigSection(
		name = "Scan pulse",
		description = "Press a key to send out a pulse of light which reveals useful things nearby",
		position = 5
	)
	String scanSection = "scanSection";

	@ConfigItem(
		keyName = "scanKey",
		name = "Scan key",
		description = "Key which sends out a scan pulse.",
		position = 0,
		section = scanSection
	)
	default Keybind scanKey() {
		return Keybind.NOT_SET;
	}

	@Range(min = 1, max = 40)
	@Units(" tiles")
	@ConfigItem(
		keyName = "scanRadius",
		name = "Scan radius",
		description = "How far the scan pulse travels.",
		position = 1,
		section = scanSection
	)
	default int scanRadius() {
		return 16;
	}

	@ConfigItem(
		keyName = "scanLights",
		name = "Scan lights",
		description = "Which things the scan pulse lights up. The categories below are always lit brightly.",
		position = 2,
		section = scanSection
	)
	default ScanLightMode scanLights() {
		return ScanLightMode.EVERYTHING_EXAMINABLE;
	}

	@Range(max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "scanLightStrength",
		name = "Scan light strength",
		description = "How brightly things revealed by the scan are lit.",
		position = 3,
		section = scanSection
	)
	default int scanLightStrength() {
		return 100;
	}

	@Range(min = 50, max = 5000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "scanSweepTime",
		name = "Scan sweep time",
		description = "How long the pulse takes to travel out to the scan radius.",
		position = 4,
		section = scanSection
	)
	default int scanSweepTime() {
		return 500;
	}

	@Range(min = 1, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "scanDuration",
		name = "Scan duration",
		description = "How long things stay lit after being revealed.",
		position = 5,
		section = scanSection
	)
	default int scanDuration() {
		return 8;
	}

	@ConfigItem(
		keyName = "scanAgilityShortcuts",
		name = "Agility shortcuts",
		description = "Reveal agility shortcuts you have the level for.",
		position = 6,
		section = scanSection
	)
	default boolean scanAgilityShortcuts() {
		return true;
	}

	@ConfigItem(
		keyName = "scanShortcutsAboveLevel",
		name = "Shortcuts above your level",
		description = "Also reveal agility shortcuts you don't have the level for, in red.",
		position = 7,
		section = scanSection
	)
	default boolean scanShortcutsAboveLevel() {
		return true;
	}

	@ConfigItem(
		keyName = "scanValuableItems",
		name = "Valuable ground items",
		description = "Reveal ground items worth at least the item value below.",
		position = 8,
		section = scanSection
	)
	default boolean scanValuableItems() {
		return true;
	}

	@Units(" gp")
	@ConfigItem(
		keyName = "scanItemValue",
		name = "Item value",
		description = "Minimum total value of a ground item stack for it to count as valuable.",
		position = 9,
		section = scanSection
	)
	default int scanItemValue() {
		return 0;
	}

	@ConfigItem(
		keyName = "scanGroundItemsHighlights",
		name = "Ground Items highlights",
		description = "Reveal items on the Ground Items plugin's highlighted list.",
		position = 10,
		section = scanSection
	)
	default boolean scanGroundItemsHighlights() {
		return true;
	}

	@ConfigItem(
		keyName = "scanObjectMarkers",
		name = "Object Markers",
		description = "Reveal objects marked with the Object Markers plugin.",
		position = 11,
		section = scanSection
	)
	default boolean scanObjectMarkers() {
		return true;
	}

	@ConfigItem(
		keyName = "scanOwnItems",
		name = "Your own items",
		description = "Reveal ground items that belong to you.",
		position = 12,
		section = scanSection
	)
	default boolean scanOwnItems() {
		return true;
	}

	@ConfigItem(
		keyName = "scanExits",
		name = "Ladders, stairs and exits",
		description = "Reveal ladders, staircases, doors to other areas and other exits.",
		position = 13,
		section = scanSection
	)
	default boolean scanExits() {
		return true;
	}

	@ConfigItem(
		keyName = "scanResourcesLikeTarget",
		name = "Resources like your target",
		description = "Reveal objects and NPCs with the same name as the last thing you interacted with, e.g. other trees of the same kind.",
		position = 14,
		section = scanSection
	)
	default boolean scanResourcesLikeTarget() {
		return true;
	}

	/*====== Shortcut glint ======*/

	@ConfigSection(
		name = "Shortcut glint",
		description = "Make agility shortcuts twinkle",
		position = 6
	)
	String glintSection = "glintSection";

	@ConfigItem(
		keyName = "shortcutGlint",
		name = "Shortcut glint",
		description = "Make nearby agility shortcuts you can use give off a faint, twinkling light.",
		position = 0,
		section = glintSection
	)
	default boolean shortcutGlint() {
		return true;
	}

	@Range(max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "shortcutGlintStrength",
		name = "Shortcut glint strength",
		description = "How bright the shortcut glint is.",
		position = 1,
		section = glintSection
	)
	default int shortcutGlintStrength() {
		return 40;
	}

	/*====== Scene lights ======*/

	@ConfigSection(
		name = "Scene lights",
		description = "Adjust 117 HD's regular lights, such as torches and fires",
		position = 7
	)
	String sceneLightsSection = "sceneLightsSection";

	@Range(max = 400)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "sceneLightStrength",
		name = "Light strength",
		description = "Brightness of torches, fires and other scene lights.",
		position = 0,
		section = sceneLightsSection
	)
	default int sceneLightStrength() {
		return 100;
	}

	@Range(min = 25, max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "sceneLightRadius",
		name = "Light radius",
		description = "How far torches, fires and other scene lights reach.",
		position = 1,
		section = sceneLightsSection
	)
	default int sceneLightRadius() {
		return 100;
	}
}
