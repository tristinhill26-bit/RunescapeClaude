package com.toahelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class ToaInfoOverlay extends OverlayPanel
{
	private static final Color GOOD = new Color(0, 220, 0);
	private static final Color BAD = new Color(255, 70, 70);
	private static final Color HEADING = new Color(255, 200, 0);
	private static final int MAX_QUEUE = 4;
	private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.US);

	private final Client client;
	private final ToaHelperPlugin plugin;
	private final ToaHelperConfig config;

	@Inject
	ToaInfoOverlay(Client client, ToaHelperPlugin plugin, ToaHelperConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.inRaid())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("ToA Helper: " + plugin.getRoom().displayName)
			.color(HEADING)
			.build());

		if (config.showIncoming())
		{
			addPrayer();
		}
		addWarnings();
		if (config.showTarget())
		{
			addTarget();
		}
		if (config.showPoints())
		{
			addPoints();
		}

		return super.render(graphics);
	}

	private void addPrayer()
	{
		PrayerAdvisor.Advice advice = plugin.getPrayerAdvice();
		if (advice.getPray() != null)
		{
			boolean on = client.isPrayerActive(PrayerBookOverlay.prayer(advice.getPray()));
			String when = advice.getTicks() >= 0 ? " in " + advice.getTicks() : "";
			line("Pray " + advice.getPray().getDisplayName() + when, on ? "ON" : "SWITCH", on ? GOOD : BAD);
			if (advice.getTicks() < 0 && advice.getReason() != null)
			{
				line("  " + advice.getReason(), "", Color.LIGHT_GRAY);
			}
		}

		List<PrayerAdvisor.Incoming> queue = advice.getQueue();
		for (int i = 1; i < queue.size() && i < MAX_QUEUE; i++)
		{
			PrayerAdvisor.Incoming next = queue.get(i);
			line("  then " + next.getStyle().getDisplayName(), next.getTicks() + "t", Color.LIGHT_GRAY);
		}
	}

	private void addWarnings()
	{
		if (plugin.isZebakMeleeWarning())
		{
			line("Step out of Zebak's melee", "", BAD);
		}
		if (plugin.isDungStrikeWarning())
		{
			line("Dung strike on you: knockback + stun", "", BAD);
		}
		if (config.babaThresholds() && plugin.isBabaThresholdWarning())
		{
			line("Ba-Ba threshold soon: go to the top or sides", "", BAD);
		}

		if (config.akkhaSpecials() && plugin.getRoom() == ToaIds.Room.AKKHA)
		{
			if (plugin.getAkkhaSpecialAdvice() != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(plugin.getAkkhaSpecialAdvice())
					.leftColor(BAD)
					.build());
			}
			else
			{
				int left = ToaMechanics.akkhaAttacksUntilSpecial(plugin.getAkkhaAttacksSinceSpecial(), config.stayVigilant());
				line("Akkha special", left == 0 ? "NEXT" : "in " + left, left <= 1 ? BAD : Color.WHITE);
			}
		}

		int core = plugin.getCoreTicksLeft();
		if (config.coreTimer() && core >= 0)
		{
			line("Core out", core + "t", core <= 5 ? BAD : GOOD);
		}
	}

	private void addTarget()
	{
		NPC target = plugin.getTarget();
		if (target == null)
		{
			return;
		}

		TargetAdvisor.Advice advice = TargetAdvisor.advise(target.getId(), ToaHelperPlugin.overheadPrayers(target));
		line(target.getName() == null ? "Target" : target.getName(), "x" + advice.getPointsFactor() + " pts", HEADING);
		if (!advice.getBlocked().isEmpty())
		{
			line("  Blocks", names(advice.getBlocked()), BAD);
		}
		if (advice.getBest() != null)
		{
			line("  Best", advice.getBest().getDisplayName(), GOOD);
		}
		else if (!advice.getBlocked().isEmpty())
		{
			line("  Use", names(advice.getUse()), GOOD);
		}
		if (advice.getTip() != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(advice.getTip())
				.leftColor(Color.LIGHT_GRAY)
				.build());
		}
	}

	private void addPoints()
	{
		PointsCalculator points = plugin.getPoints();
		ToaIds.Room room = plugin.getRoom();
		line("Room points", NUMBER.format(points.getRoom()) + " / " + NUMBER.format(room.pointsCap), Color.WHITE);
		line("Raid points", NUMBER.format(points.lootPoints()), Color.WHITE);
		line("A death costs", NUMBER.format(points.deathPenalty()), BAD);
		if (plugin.getRaidLevel() > 0)
		{
			double chance = PointsCalculator.uniqueChance(plugin.getRaidLevel(), points.lootPoints());
			line("Purple (solo)", String.format(Locale.US, "%.2f%%", chance * 100), GOOD);
		}
	}

	private void line(String left, String right, Color color)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(color)
			.build());
	}

	private static String names(Set<AttackStyle> styles)
	{
		return styles.stream().map(AttackStyle::getDisplayName).collect(Collectors.joining(", "));
	}
}
