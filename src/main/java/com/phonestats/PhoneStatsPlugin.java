package com.phonestats;

import com.google.inject.Provides;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Phone HP & Prayer",
	description = "Sends your current and max Hitpoints and Prayer to the HP & Prayer app on your phone over Wi-Fi",
	tags = { "phone", "android", "mobile", "companion", "hitpoints", "hp", "prayer", "status", "orbs" }
)
public class PhoneStatsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PhoneStatsConfig config;

	private volatile StatsServer server;
	private StatsSnapshot lastSent;
	private boolean addressShown;

	@Provides
	PhoneStatsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PhoneStatsConfig.class);
	}

	@Override
	protected void startUp()
	{
		startServer();
	}

	@Override
	protected void shutDown()
	{
		stopServer();
		addressShown = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(PhoneStatsConfig.CONFIG_GROUP) && event.getKey().equals("port"))
		{
			stopServer();
			startServer();
			addressShown = false;
			clientThread.invokeLater(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					showAddress();
				}
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN && !addressShown && config.showAddressOnLogin())
		{
			showAddress();
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST)
		{
			send(StatsSnapshot.LOGGED_OUT);
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		// Hitpoints and Prayer only change on game ticks, so checking every tick (0.6 s) catches every change
		send(snapshot());
	}

	private StatsSnapshot snapshot()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return StatsSnapshot.LOGGED_OUT;
		}

		Player player = client.getLocalPlayer();
		String name = player != null && player.getName() != null ? player.getName() : "";
		return new StatsSnapshot(
			true,
			name,
			client.getBoostedSkillLevel(Skill.HITPOINTS),
			client.getRealSkillLevel(Skill.HITPOINTS),
			client.getBoostedSkillLevel(Skill.PRAYER),
			client.getRealSkillLevel(Skill.PRAYER),
			isAnyPrayerActive());
	}

	private boolean isAnyPrayerActive()
	{
		for (Prayer prayer : Prayer.values())
		{
			try
			{
				if (client.isPrayerActive(prayer))
				{
					return true;
				}
			}
			catch (RuntimeException e)
			{
				// A prayer this server's game cache doesn't have
			}
		}
		return false;
	}

	private void send(StatsSnapshot snapshot)
	{
		StatsServer current = server;
		if (current == null || snapshot.equals(lastSent))
		{
			return;
		}
		lastSent = snapshot;
		current.publish(snapshot.toJson());
	}

	private void startServer()
	{
		StatsServer newServer = new StatsServer(config.port());
		try
		{
			newServer.start();
			server = newServer;
			lastSent = null;
			log.info("Phone HP & Prayer is listening on port {}", config.port());
		}
		catch (IOException e)
		{
			log.warn("Phone HP & Prayer couldn't use port {}", config.port(), e);
			server = null;
		}
	}

	private void stopServer()
	{
		if (server != null)
		{
			server.stop();
			server = null;
		}
	}

	private void showAddress()
	{
		addressShown = true;
		StatsServer current = server;
		String message;
		if (current == null)
		{
			message = "Phone HP & Prayer: port " + config.port() + " is already in use. " +
				"Pick another port in the plugin's settings.";
		}
		else
		{
			List<String> addresses = lanAddresses();
			String where = addresses.isEmpty() ? "this PC's IP address" : String.join(" or ", addresses);
			message = "Phone HP & Prayer: in the phone app, connect to " + where + ", port " + current.getPort() + ".";
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	/**
	 * The addresses a phone on the same Wi-Fi can reach this PC at, e.g. 192.168.1.23.
	 */
	static List<String> lanAddresses()
	{
		List<String> addresses = new ArrayList<>();
		try
		{
			for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces()))
			{
				if (!nic.isUp() || nic.isLoopback() || nic.isVirtual())
				{
					continue;
				}
				String nicName = (nic.getDisplayName() + " " + nic.getName()).toLowerCase();
				// Skip adapters made by VM and VPN software, which phones can't reach
				if (nicName.contains("virtual") || nicName.contains("vmware") || nicName.contains("vbox")
					|| nicName.contains("hyper-v") || nicName.contains("wsl") || nicName.contains("docker"))
				{
					continue;
				}
				for (InetAddress address : Collections.list(nic.getInetAddresses()))
				{
					if (address instanceof Inet4Address && address.isSiteLocalAddress())
					{
						addresses.add(address.getHostAddress());
					}
				}
			}
		}
		catch (SocketException e)
		{
			log.debug("Couldn't list network addresses", e);
		}
		return addresses;
	}
}
