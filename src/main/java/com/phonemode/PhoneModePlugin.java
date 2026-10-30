package com.phonemode;

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Phone Mode",
	description = "Makes RuneLite easier to play on a phone, starting with keeping the window filling the whole screen",
	tags = { "phone", "mobile", "android", "termux", "fullscreen", "touch" }
)
public class PhoneModePlugin extends Plugin {
	@Inject
	private Client client;

	private Timer fillScreenTimer;

	@Override
	protected void startUp() {
		// Checked every second rather than once, because RuneLite restores its saved window size after it starts, and
		// the screen changes size when the phone rotates
		SwingUtilities.invokeLater(() -> {
			fillScreenTimer = new Timer(1000, e -> fillScreen());
			fillScreenTimer.setInitialDelay(0);
			fillScreenTimer.start();
		});
	}

	@Override
	protected void shutDown() {
		SwingUtilities.invokeLater(() -> {
			if (fillScreenTimer != null) {
				fillScreenTimer.stop();
				fillScreenTimer = null;
			}
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
}
