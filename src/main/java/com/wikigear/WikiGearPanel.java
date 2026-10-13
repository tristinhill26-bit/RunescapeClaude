package com.wikigear;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.LinkBrowser;

/**
 * Side panel: search for a monster or activity, pick it, tick the setups to generate, then create the bank tags.
 */
class WikiGearPanel extends PluginPanel {
	private static final int ICON_SIZE = 32;

	private final WikiClient wikiClient;
	private final WikiGearPlugin plugin;

	private final IconTextField searchBar = new IconTextField();
	private final JPanel resultsPanel = new JPanel();
	private final JPanel monsterPanel = new JPanel();
	private final JLabel monsterLabel = new JLabel();
	private final JLabel statusLabel = new JLabel();
	private final JPanel setupsPanel = new JPanel();
	private final JButton createButton = new JButton("Create bank tags");
	private final JTextArea summary = new JTextArea();

	private final List<JCheckBox> setupBoxes = new ArrayList<>();
	private List<GearSetup> setups = new ArrayList<>();
	private WikiClient.Target target;
	private String strategyPage;
	/**
	 * Increments with each search or selection, so stale responses are ignored
	 */
	private int requestId;

	@Inject
	private WikiGearPanel(WikiClient wikiClient, WikiGearPlugin plugin) {
		this.wikiClient = wikiClient;
		this.plugin = plugin;

		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout(0, 8));

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addActionListener(e -> search());
		searchBar.addClearListener(() -> {
			requestId++;
			resultsPanel.removeAll();
			refresh();
		});
		add(searchBar, BorderLayout.NORTH);

		var content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
		resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsPanel.setAlignmentX(LEFT_ALIGNMENT);
		content.add(resultsPanel);

		buildMonsterPanel();
		content.add(monsterPanel);

		var hint = new JLabel("<html>Type a monster or activity, like Vorkath, Barrows or Chambers of Xeric, and press Enter.</html>");
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hint.setAlignmentX(LEFT_ALIGNMENT);
		resultsPanel.add(hint);

		add(content, BorderLayout.CENTER);
	}

	private void buildMonsterPanel() {
		monsterPanel.setLayout(new BoxLayout(monsterPanel, BoxLayout.Y_AXIS));
		monsterPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		monsterPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
		monsterPanel.setAlignmentX(LEFT_ALIGNMENT);
		monsterPanel.setVisible(false);

		monsterLabel.setForeground(Color.WHITE);
		monsterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		monsterLabel.setToolTipText("Open the strategy page on the wiki");
		monsterLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (strategyPage != null)
					LinkBrowser.browse(WikiClient.pageUrl(strategyPage));
			}
		});
		monsterLabel.setAlignmentX(LEFT_ALIGNMENT);
		monsterPanel.add(monsterLabel);
		monsterPanel.add(Box.createVerticalStrut(4));

		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setAlignmentX(LEFT_ALIGNMENT);
		monsterPanel.add(statusLabel);
		monsterPanel.add(Box.createVerticalStrut(4));

		setupsPanel.setLayout(new BoxLayout(setupsPanel, BoxLayout.Y_AXIS));
		setupsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setupsPanel.setAlignmentX(LEFT_ALIGNMENT);
		monsterPanel.add(setupsPanel);
		monsterPanel.add(Box.createVerticalStrut(6));

		createButton.setAlignmentX(LEFT_ALIGNMENT);
		createButton.setEnabled(false);
		createButton.addActionListener(e -> createTags());
		monsterPanel.add(createButton);
		monsterPanel.add(Box.createVerticalStrut(6));

		summary.setEditable(false);
		summary.setLineWrap(true);
		summary.setWrapStyleWord(true);
		summary.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summary.setBorder(BorderFactory.createEmptyBorder());
		summary.setAlignmentX(LEFT_ALIGNMENT);
		monsterPanel.add(summary);
	}

	private void search() {
		String query = searchBar.getText().trim();
		if (query.isEmpty())
			return;

		int id = ++requestId;
		searchBar.setIcon(IconTextField.Icon.LOADING_DARKER);
		wikiClient.search(query,
			targets -> SwingUtilities.invokeLater(() -> {
				if (id != requestId)
					return;
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				showResults(targets);
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (id != requestId)
					return;
				searchBar.setIcon(IconTextField.Icon.ERROR);
				resultsPanel.removeAll();
				resultsPanel.add(message(error));
				refresh();
			}));
	}

	private void showResults(List<WikiClient.Target> targets) {
		resultsPanel.removeAll();
		if (targets.isEmpty())
			resultsPanel.add(message("Nothing found."));

		for (var result : targets) {
			var row = new JPanel(new BorderLayout(8, 0));
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(new EmptyBorder(4, 4, 4, 4));
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ICON_SIZE + 8));
			row.setAlignmentX(LEFT_ALIGNMENT);
			row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			var icon = new JLabel();
			icon.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
			icon.setHorizontalAlignment(JLabel.CENTER);
			row.add(icon, BorderLayout.WEST);
			String thumbnailUrl = result.thumbnailUrl;
			if (thumbnailUrl != null) {
				wikiClient.fetchImage(thumbnailUrl, image -> SwingUtilities.invokeLater(() -> {
					double scale = Math.min((double) ICON_SIZE / image.getWidth(), (double) ICON_SIZE / image.getHeight());
					int w = Math.max(1, (int) (image.getWidth() * scale));
					int h = Math.max(1, (int) (image.getHeight() * scale));
					icon.setIcon(new ImageIcon(image.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
				}));
			}

			var name = new JLabel(result.isGuide()
				? "<html>" + result.name + "<br><font color='#a5a5a5'>Has recommended gear</font></html>"
				: result.name);
			name.setForeground(Color.WHITE);
			row.add(name, BorderLayout.CENTER);

			row.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					select(result);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					row.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				}
			});

			resultsPanel.add(row);
			resultsPanel.add(Box.createVerticalStrut(2));
		}
		refresh();
	}

	private void select(WikiClient.Target selected) {
		int id = ++requestId;
		target = selected;
		strategyPage = null;
		setups = new ArrayList<>();
		setupBoxes.clear();
		setupsPanel.removeAll();
		summary.setText("");
		createButton.setEnabled(false);
		monsterLabel.setText(selected.name);
		statusLabel.setText("Loading recommended equipment...");
		monsterPanel.setVisible(true);
		resultsPanel.removeAll();
		refresh();

		wikiClient.fetchStrategies(selected,
			page -> SwingUtilities.invokeLater(() -> {
				if (id != requestId)
					return;
				showSetups(page);
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (id != requestId)
					return;
				statusLabel.setText("<html>" + error + "</html>");
				refresh();
			}));
	}

	private void showSetups(WikiClient.StrategyPage page) {
		strategyPage = page.title;
		setups = page.setups;
		monsterLabel.setText("<html><u>" + target.name + "</u></html>");

		if (setups.isEmpty()) {
			statusLabel.setText("<html>The wiki has no recommended equipment tables for this.</html>");
			refresh();
			return;
		}

		statusLabel.setText("<html>Choose the styles to create tags for:</html>");
		for (int i = 0; i < setups.size(); i++) {
			var box = new JCheckBox(setups.get(i).getLabel(), i == 0);
			box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			box.setForeground(Color.WHITE);
			box.setAlignmentX(LEFT_ALIGNMENT);
			box.addActionListener(e -> updateCreateButton());
			setupBoxes.add(box);
			setupsPanel.add(box);
		}
		updateCreateButton();
		refresh();
	}

	private void updateCreateButton() {
		createButton.setEnabled(setupBoxes.stream().anyMatch(JCheckBox::isSelected));
	}

	private void createTags() {
		var selected = new ArrayList<GearSetup>();
		for (int i = 0; i < setupBoxes.size(); i++)
			if (setupBoxes.get(i).isSelected())
				selected.add(setups.get(i));
		if (selected.isEmpty())
			return;

		int id = requestId;
		createButton.setEnabled(false);
		summary.setText("Creating tags...");
		plugin.createLoadouts(target.name, selected,
			results -> SwingUtilities.invokeLater(() -> {
				if (id != requestId)
					return;
				summary.setText(describe(results));
				updateCreateButton();
				refresh();
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (id != requestId)
					return;
				summary.setText(error);
				updateCreateButton();
				refresh();
			}));
	}

	private static String describe(List<WikiGearPlugin.LoadoutResult> results) {
		var text = new StringBuilder();
		for (var loadout : results) {
			if (text.length() > 0)
				text.append("\n\n");
			text.append(loadout.setup.getLabel()).append(" -> tag '").append(loadout.tag).append("'\n");
			var picks = loadout.result.picks;
			for (var slot : GearSlot.values()) {
				var pick = picks.get(slot);
				if (pick == null && loadout.setup.getRanks(slot).isEmpty())
					continue;
				text.append(slot.getDisplayName()).append(": ");
				if (pick == null) {
					text.append("nothing owned");
				} else {
					text.append(pick.name);
					switch (pick.source) {
						case WIKI:
							if (pick.rank > 1)
								text.append(" (#").append(pick.rank).append(" of ").append(pick.rankCount).append(')');
							break;
						case SET:
							text.append(" (completes set)");
							break;
						case FILLED:
							text.append(" (your best, not on the wiki's list)");
							break;
					}
				}
				text.append('\n');
			}
			for (var note : loadout.result.notes)
				text.append(note).append('\n');
		}
		text.append("\nTip: search 'tag:<name>' in your bank, or add it as a tab, to use a tag later.");
		return text.toString();
	}

	private static JLabel message(String text) {
		var label = new JLabel("<html>" + text + "</html>");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private void refresh() {
		revalidate();
		repaint();
	}
}
