package rs117.hd;

import com.fairyringlabels.FairyRingLabelsPlugin;
import com.madangel.MadAngelPlugin;
import com.phonemode.PhoneModePlugin;
import com.shadowtiles.ShadowTilesPlugin;
import com.toahelper.ToaHelperPlugin;
import com.wikigear.WikiGearPlugin;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.externalplugins.ExternalPluginManager;
import rs117.hd.interaction.InteractionLightsPlugin;
import rs117.hd.utils.Props;

@Slf4j
@SuppressWarnings("unchecked")
public class HdPluginTest
{
	public static void main(String[] args) throws Exception
	{
		log.warn(
			"If this is your first time using the HdPluginTest run configuration, " +
			"this window will filter on WARN-level logging by default"
		);

		Props.DEVELOPMENT = true;
		Props.set("rlhd.resource-path", "src/main/resources");
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
		useLatestPluginHub();
		if (Boolean.getBoolean("rlhd.lite"))
		{
			// For devices without desktop OpenGL (e.g. a phone running Linux in Termux): leave out the plugins that
			// need the 117 HD renderer, and keep the ones that only draw on top of the game.
			ExternalPluginManager.loadBuiltin(PhoneModePlugin.class, FairyRingLabelsPlugin.class, WikiGearPlugin.class,
				ShadowTilesPlugin.class, MadAngelPlugin.class,
				ToaHelperPlugin.class);
		}
		else
		{
			ExternalPluginManager.loadBuiltin(HdPlugin.class, InteractionLightsPlugin.class, FairyRingLabelsPlugin.class, WikiGearPlugin.class,
				ShadowTilesPlugin.class, MadAngelPlugin.class,
				ToaHelperPlugin.class);
		}
		RuneLite.main(args);
	}

	private static void useLatestPluginHub()
	{
		if (System.getProperty("runelite.pluginhub.version") == null)
		{
			try
			{
				Properties props = new Properties();
				try (InputStream in = RuneLiteProperties.class.getResourceAsStream("runelite.properties"))
				{
					props.load(in);
				}

				String version = props.getProperty("runelite.pluginhub.version");
				String[] parts = version.split("[.-]");
				if (parts.length > 3 && parts[3].equals("SNAPSHOT"))
				{
					int patch = Integer.parseInt(parts[2]) - 1;
					version = parts[0] + "." + parts[1] + "." + patch;
					log.info("Detected SNAPSHOT version with no manually specified plugin-hub version. " +
							"Setting runelite.pluginhub.version to {}", version);
					System.setProperty("runelite.pluginhub.version", version);
				}
			}
			catch (Exception ex)
			{
				log.error("Failed to automatically use latest plugin-hub version", ex);
			}
		}
	}
}
