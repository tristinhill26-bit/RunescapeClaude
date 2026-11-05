package com.phonemode;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

@Slf4j
@PluginDescriptor(
	name = "Phone Mode",
	description = "Makes RuneLite play like the mobile game: fills the screen, and a long press opens the right-click menu",
	tags = { "phone", "mobile", "android", "termux", "fullscreen", "touch" }
)
public class PhoneModePlugin extends Plugin implements MouseListener {
	private static final String CONFIG_GROUP = "phonemode";
	private static final String SETUP_DONE_KEY = "stretchedModeSetUp";
	private static final int LONG_PRESS_MS = 400;
	// How far a finger can wander during a press and still count as a tap or long press, rather than a drag
	private static final int TOUCH_SLOP = 12;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private MouseManager mouseManager;

	private Timer fillScreenTimer;
	private Timer longPressTimer;

	// The left press being held back until we know whether it's a tap, a long press or a drag
	private MouseEvent heldPress;
	// Set while sending our own events to the game, so the listener lets them through
	private boolean replaying;
	// Set after a long press opened the menu, so the finger lifting doesn't also click
	private boolean swallowRelease;
	private boolean swallowClick;

	@Override
	protected void startUp() {
		mouseManager.registerMouseListener(0, this);
		// Checked every second rather than once, because RuneLite restores its saved window size after it starts, and
		// the screen changes size when the phone rotates
		SwingUtilities.invokeLater(() -> {
			fillScreenTimer = new Timer(1000, e -> {
				fillScreen();
				setUpStretchedMode();
			});
			fillScreenTimer.setInitialDelay(0);
			fillScreenTimer.start();
		});
	}

	@Override
	protected void shutDown() {
		mouseManager.unregisterMouseListener(this);
		SwingUtilities.invokeLater(() -> {
			if (fillScreenTimer != null) {
				fillScreenTimer.stop();
				fillScreenTimer = null;
			}
			cancelLongPress();
			heldPress = null;
			swallowRelease = false;
			swallowClick = false;
		});
	}

	private void fillScreen() {
		if (client.getCanvas() == null)
			return;
		Window window = SwingUtilities.getWindowAncestor(client.getCanvas());
		if (window == null || !window.isShowing() || window.getGraphicsConfiguration() == null)
			return;

		Rectangle screen = window.getGraphicsConfiguration().getBounds();
		if (window.getBounds().equals(screen))
			return;

		// Without a window manager, maximizing does nothing, so set the size directly too
		window.setBounds(screen);
		if (window instanceof Frame)
			((Frame) window).setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	/**
	 * Turn on RuneLite's Stretched Mode once, so the game scales up to fill the screen in both fixed and resizable
	 * mode, like the mobile game. Only done once, so it stays off if it's turned off later.
	 */
	private void setUpStretchedMode() {
		if (Boolean.parseBoolean(configManager.getConfiguration(CONFIG_GROUP, SETUP_DONE_KEY)))
			return;

		for (Plugin plugin : pluginManager.getPlugins()) {
			if (!plugin.getClass().getSimpleName().equals("StretchedModePlugin"))
				continue;

			configManager.setConfiguration("stretchedmode", "keepAspectRatio", true);
			configManager.setConfiguration("stretchedmode", "integerScaling", false);
			configManager.setConfiguration(CONFIG_GROUP, SETUP_DONE_KEY, true);
			if (!pluginManager.isPluginEnabled(plugin)) {
				pluginManager.setPluginEnabled(plugin, true);
				try {
					pluginManager.startPlugin(plugin);
				} catch (Exception e) {
					log.warn("Couldn't start Stretched Mode", e);
				}
			}
			return;
		}
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e) {
		if (replaying || e.getButton() != MouseEvent.BUTTON1)
			return e;

		// Hold the press back: the game acts as soon as the button goes down, so letting it through would walk or
		// attack before we know the finger is staying down to open the menu
		heldPress = e;
		swallowRelease = false;
		swallowClick = false;
		cancelLongPress();
		longPressTimer = new Timer(LONG_PRESS_MS, t -> onLongPress());
		longPressTimer.setRepeats(false);
		longPressTimer.start();
		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e) {
		if (replaying || heldPress == null)
			return e;

		// The finger moved, so it's a drag: let the press through late and carry on as a normal drag
		if (Math.abs(e.getXOnScreen() - heldPress.getXOnScreen()) > TOUCH_SLOP || Math.abs(e.getYOnScreen() - heldPress.getYOnScreen()) > TOUCH_SLOP) {
			cancelLongPress();
			MouseEvent press = heldPress;
			heldPress = null;
			replay(press, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
			return e;
		}

		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e) {
		if (replaying || e.getButton() != MouseEvent.BUTTON1)
			return e;

		if (swallowRelease) {
			swallowRelease = false;
			swallowClick = true;
			e.consume();
			return e;
		}

		if (heldPress != null) {
			// A quick tap: send the held press now, then let this release through as the click
			cancelLongPress();
			MouseEvent press = heldPress;
			heldPress = null;
			replay(press, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e) {
		if (!replaying && swallowClick) {
			swallowClick = false;
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e) {
		return e;
	}

	private void onLongPress() {
		longPressTimer = null;
		MouseEvent press = heldPress;
		if (press == null)
			return;

		// The finger stayed down: open the right-click menu where it is, and ignore the lift
		heldPress = null;
		swallowRelease = true;
		replay(press, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3);
		replay(press, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3);
	}

	private void cancelLongPress() {
		if (longPressTimer != null) {
			longPressTimer.stop();
			longPressTimer = null;
		}
	}

	/**
	 * Send a mouse event to the game at the same place on screen as the press it stands in for. Screen positions are
	 * used because Stretched Mode hands listeners like this one positions already scaled to the game, and it would
	 * scale them a second time on the way back in.
	 */
	private void replay(MouseEvent original, int id, int button) {
		Canvas canvas = client.getCanvas();
		if (canvas == null || !canvas.isShowing())
			return;

		Point canvasOnScreen = canvas.getLocationOnScreen();
		int x = original.getXOnScreen() - canvasOnScreen.x;
		int y = original.getYOnScreen() - canvasOnScreen.y;
		int modifiers = id == MouseEvent.MOUSE_RELEASED ? 0
			: button == MouseEvent.BUTTON3 ? MouseEvent.BUTTON3_DOWN_MASK : MouseEvent.BUTTON1_DOWN_MASK;
		MouseEvent event = new MouseEvent(canvas, id, System.currentTimeMillis(), modifiers, x, y,
			original.getXOnScreen(), original.getYOnScreen(), 1, button == MouseEvent.BUTTON3, button);
		replaying = true;
		try {
			canvas.dispatchEvent(event);
		} finally {
			replaying = false;
		}
	}
}
