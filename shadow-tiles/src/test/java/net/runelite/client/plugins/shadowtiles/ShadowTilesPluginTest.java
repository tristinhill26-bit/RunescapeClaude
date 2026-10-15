package net.runelite.client.plugins.shadowtiles;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShadowTilesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ShadowTilesPlugin.class);
		RuneLite.main(args);
	}
}
