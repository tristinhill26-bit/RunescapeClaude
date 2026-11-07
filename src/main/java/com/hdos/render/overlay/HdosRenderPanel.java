package com.hdos.render.overlay;

import com.hdos.render.layout.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class HdosRenderPanel extends PluginPanel {

    private final HdosRenderPlugin plugin;
    private final Client client;

    private JComboBox<String> skillSelector;
    private JSpinner targetXpSpinner;
    private JButton trackButton;
    private JLabel statusLabel;
    private JLabel progressLabel;

    @Inject
    public HdosRenderPanel(HdosRenderPlugin plugin, Client client) {
        this.plugin = plugin;
        this.client = client;
        buildPanel();
    }

    private void buildPanel() {
        setLayout(new VerticalLayout(8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("HDOS Render Engine");
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
        title.setForeground(Color.WHITE);
        add(title);

        JPanel skillPanel = new JPanel(new BorderLayout(5, 0));
        skillPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        skillPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));

        JLabel skillLabel = new JLabel("Skill:");
        skillLabel.setForeground(Color.WHITE);
        skillPanel.add(skillLabel, BorderLayout.WEST);

        String[] skillNames = new String[Skill.values().length - 1];
        for (int i = 0; i < skillNames.length; i++) {
            skillNames[i] = Skill.values()[i].getName();
        }
        skillSelector = new JComboBox<>(skillNames);
        skillPanel.add(skillSelector, BorderLayout.CENTER);
        add(skillPanel);

        JPanel targetPanel = new JPanel(new BorderLayout(5, 0));
        targetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        targetPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));

        JLabel targetLabel = new JLabel("Target XP:");
        targetLabel.setForeground(Color.WHITE);
        targetPanel.add(targetLabel, BorderLayout.WEST);

        targetXpSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 200_000_000, 1000));
        targetPanel.add(targetXpSpinner, BorderLayout.CENTER);
        add(targetPanel);

        trackButton = new JButton("Start Tracking");
        trackButton.addActionListener(e -> toggleTracking());
        add(trackButton);

        statusLabel = new JLabel("Not tracking");
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        add(statusLabel);

        progressLabel = new JLabel("");
        progressLabel.setForeground(new Color(0, 180, 0));
        progressLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        add(progressLabel);
    }

    private void toggleTracking() {
        if (plugin.isTracking()) {
            plugin.stopTracking();
            trackButton.setText("Start Tracking");
            statusLabel.setText("Not tracking");
            progressLabel.setText("");
        } else {
            int skillIndex = skillSelector.getSelectedIndex();
            int targetXp = (int) targetXpSpinner.getValue();
            if (targetXp <= 0) {
                statusLabel.setText("Set a target XP value");
                return;
            }
            plugin.startTracking(skillIndex, targetXp);
            trackButton.setText("Stop Tracking");
            statusLabel.setText("Tracking " + skillSelector.getSelectedItem());
        }
    }

    public void updateStatus() {
        if (plugin.isTracking()) {
            statusLabel.setText(plugin.getStatusText());
            progressLabel.setText(plugin.getProgressPercent() + "%");
        }
    }
}
