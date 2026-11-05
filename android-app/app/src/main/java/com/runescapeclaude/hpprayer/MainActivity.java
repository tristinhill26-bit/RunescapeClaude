package com.runescapeclaude.hpprayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;

public class MainActivity extends Activity implements StatsConnection.Listener, LootLog.Listener {
	static final String PREFS = "settings";
	static final String KEY_HOST = "host";
	static final String KEY_PORT = "port";
	static final int DEFAULT_PORT = 8765;
	private static final int NOTIFICATION_PERMISSION_REQUEST = 1;

	private static final int BACKGROUND = Color.rgb(18, 18, 22);
	private static final int HP_GREEN = Color.rgb(46, 160, 67);
	private static final int HP_ORANGE = Color.rgb(230, 140, 30);
	private static final int HP_RED = Color.rgb(211, 47, 47);
	private static final int PRAYER_ON = Color.rgb(41, 182, 246);
	private static final int PRAYER_OFF = Color.rgb(30, 110, 160);

	private SharedPreferences prefs;
	private volatile StatsConnection connection;

	private TextView statusView;
	private TextView nameView;
	private StatBarView hpBar;
	private StatBarView prayerBar;
	private LinearLayout settingsPanel;
	private EditText hostInput;
	private EditText portInput;
	private Switch statusBarSwitch;
	private TextView goadingView;
	// Whether a goading potion was active since connecting, so "ran out" only shows after you've used one
	private boolean goadingSeen;
	private TextView lootTotalView;
	private TextView lootSourcesView;
	private TextView lootItemsView;
	private TextView lootRecentView;
	private LootLog lootLog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
		lootLog = LootLog.get(this);
		// It's a display you glance at while playing, so don't let the screen go dark
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setStatusBarColor(BACKGROUND);
		getWindow().setNavigationBarColor(BACKGROUND);
		setContentView(buildUi());
		settingsPanel.setVisibility(savedHost().isEmpty() ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		lootLog.addListener(this);
		showLoot();
		connect();
		// The notification's Stop button can turn this off while the app is closed
		boolean statusBar = prefs.getBoolean(StatsService.KEY_STATUS_BAR, false);
		statusBarSwitch.setChecked(statusBar);
		if (statusBar) {
			startStatsService();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		lootLog.removeListener(this);
		disconnect();
	}

	private View buildUi() {
		float dp = getResources().getDisplayMetrics().density;
		int pad = (int) (20 * dp);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(pad, pad, pad, pad);
		root.setBackgroundColor(BACKGROUND);

		LinearLayout header = new LinearLayout(this);
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);

		LinearLayout titles = new LinearLayout(this);
		titles.setOrientation(LinearLayout.VERTICAL);
		nameView = text(22, Color.WHITE);
		nameView.setTypeface(Typeface.DEFAULT_BOLD);
		nameView.setText(R.string.app_name);
		statusView = text(14, Color.rgb(170, 170, 180));
		titles.addView(nameView);
		titles.addView(statusView);
		header.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

		Button settingsButton = new Button(this);
		settingsButton.setText("PC");
		settingsButton.setOnClickListener(v -> settingsPanel.setVisibility(
			settingsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
		header.addView(settingsButton);
		root.addView(header);

		settingsPanel = buildSettings(dp);
		root.addView(settingsPanel);

		hpBar = new StatBarView(this, "Hitpoints", HP_GREEN);
		prayerBar = new StatBarView(this, "Prayer", PRAYER_OFF);
		LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		barParams.topMargin = (int) (20 * dp);
		root.addView(prayerBar, barParams);
		root.addView(hpBar, new LinearLayout.LayoutParams(barParams));

		goadingView = text(17, Color.WHITE);
		goadingView.setTypeface(Typeface.DEFAULT_BOLD);
		goadingView.setGravity(Gravity.CENTER);
		goadingView.setPadding(0, (int) (12 * dp), 0, (int) (12 * dp));
		goadingView.setVisibility(View.GONE);
		root.addView(goadingView, new LinearLayout.LayoutParams(barParams));

		statusBarSwitch = new Switch(this);
		statusBarSwitch.setText("Keep running when the app is closed (status bar and alerts)");
		statusBarSwitch.setTextColor(Color.rgb(200, 200, 210));
		statusBarSwitch.setTextSize(15);
		statusBarSwitch.setOnClickListener(v -> setStatusBar(statusBarSwitch.isChecked()));
		root.addView(statusBarSwitch, new LinearLayout.LayoutParams(barParams));

		root.addView(prefSwitch("Prayer alerts at 20, 10 and 0", StatsService.KEY_PRAYER_ALERTS),
			new LinearLayout.LayoutParams(barParams));
		root.addView(prefSwitch("Alert when a goading potion runs out", StatsService.KEY_GOADING_ALERTS),
			new LinearLayout.LayoutParams(barParams));
		root.addView(prefSwitch("Drop alerts (set the value in the RuneLite plugin)", StatsService.KEY_DROP_ALERTS),
			new LinearLayout.LayoutParams(barParams));

		buildLoot(root, dp);

		ScrollView scroll = new ScrollView(this);
		scroll.setFillViewport(true);
		scroll.setBackgroundColor(BACKGROUND);
		scroll.addView(root);
		applySystemBarInsets(scroll);
		return scroll;
	}

	private LinearLayout buildSettings(float dp) {
		LinearLayout panel = new LinearLayout(this);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(0, (int) (16 * dp), 0, 0);

		TextView help = text(14, Color.rgb(200, 200, 210));
		help.setText("Enter your PC's address. RuneLite prints it in the chat box when you log in, "
			+ "e.g. 192.168.1.23. Your phone has to be on the same Wi-Fi as the PC.");
		panel.addView(help);

		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);

		hostInput = input("PC address");
		hostInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		hostInput.setText(savedHost());
		row.addView(hostInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));

		portInput = input("Port");
		portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		portInput.setText(String.valueOf(prefs.getInt(KEY_PORT, DEFAULT_PORT)));
		portInput.setImeOptions(EditorInfo.IME_ACTION_GO);
		portInput.setOnEditorActionListener((v, actionId, event) -> {
			saveAndConnect();
			return true;
		});
		row.addView(portInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

		Button connectButton = new Button(this);
		connectButton.setText("Connect");
		connectButton.setOnClickListener(v -> saveAndConnect());
		row.addView(connectButton);
		panel.addView(row);
		return panel;
	}

	private void saveAndConnect() {
		String host = hostInput.getText().toString().trim()
			.replaceFirst("^https?://", "")
			.replaceFirst("/.*$", "");
		int port = DEFAULT_PORT;
		// Accept "192.168.1.23:8765" typed into the address box too
		int colon = host.lastIndexOf(':');
		if (colon > 0 && host.indexOf(':') == colon) {
			try {
				port = Integer.parseInt(host.substring(colon + 1));
			} catch (NumberFormatException ignored) {
			}
			host = host.substring(0, colon);
		} else {
			try {
				port = Integer.parseInt(portInput.getText().toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		hostInput.setText(host);
		portInput.setText(String.valueOf(port));
		prefs.edit().putString(KEY_HOST, host).putInt(KEY_PORT, port).apply();

		InputMethodManager imm = getSystemService(InputMethodManager.class);
		if (imm != null) {
			imm.hideSoftInputFromWindow(hostInput.getWindowToken(), 0);
		}
		if (!host.isEmpty()) {
			settingsPanel.setVisibility(View.GONE);
		}
		disconnect();
		connect();
		if (prefs.getBoolean(StatsService.KEY_STATUS_BAR, false)) {
			// Reconnect the notification to the new address too
			stopService(new Intent(this, StatsService.class));
			startStatsService();
		}
	}

	private void setStatusBar(boolean on) {
		prefs.edit().putBoolean(StatsService.KEY_STATUS_BAR, on).apply();
		if (!on) {
			stopService(new Intent(this, StatsService.class));
			return;
		}
		if (Build.VERSION.SDK_INT >= 33
			&& checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS },
				NOTIFICATION_PERMISSION_REQUEST);
			return;
		}
		startStatsService();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode != NOTIFICATION_PERMISSION_REQUEST) {
			return;
		}
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			startStatsService();
		} else {
			statusBarSwitch.setChecked(false);
			prefs.edit().putBoolean(StatsService.KEY_STATUS_BAR, false).apply();
			statusView.setText("Allow notifications for HP & Prayer to show it in the status bar");
		}
	}

	private void startStatsService() {
		if (savedHost().isEmpty()) {
			return;
		}
		startForegroundService(new Intent(this, StatsService.class));
	}

	private void connect() {
		String host = savedHost();
		if (host.isEmpty()) {
			statusView.setText("Not set up yet");
			return;
		}
		connection = new StatsConnection(host, prefs.getInt(KEY_PORT, DEFAULT_PORT), this);
		connection.start();
	}

	private void disconnect() {
		if (connection != null) {
			connection.stop();
			connection = null;
		}
	}

	private String savedHost() {
		return prefs.getString(KEY_HOST, "");
	}

	// Callbacks from the connection thread. Each one checks it came from the current connection, so a late message
	// from one that was just replaced can't overwrite the screen.

	@Override
	public void onConnecting(StatsConnection source) {
		runOnUiThread(() -> {
			if (source == connection) {
				statusView.setText("Connecting to " + savedHost() + "…");
			}
		});
	}

	@Override
	public void onConnected(StatsConnection source) {
		runOnUiThread(() -> {
			if (source == connection) {
				statusView.setText("Connected to " + savedHost());
			}
		});
	}

	@Override
	public void onStats(StatsConnection source, Stats stats) {
		runOnUiThread(() -> {
			if (source != connection) {
				return;
			}
			if (!stats.loggedIn) {
				nameView.setText(R.string.app_name);
				statusView.setText("Connected. Waiting for you to log in");
				hpBar.clear();
				prayerBar.clear();
				prayerBar.setBadge("");
				goadingSeen = false;
				goadingView.setVisibility(View.GONE);
				return;
			}
			showGoading(stats.goading);
			nameView.setText(stats.name.isEmpty() ? getString(R.string.app_name) : stats.name);
			statusView.setText("Connected to " + savedHost());
			hpBar.setValues(stats.hp, stats.maxHp);
			hpBar.setColor(hpColor(stats.hp, stats.maxHp));
			prayerBar.setValues(stats.prayer, stats.maxPrayer);
			prayerBar.setColor(stats.prayerActive ? PRAYER_ON : PRAYER_OFF);
			prayerBar.setBadge(stats.prayerActive ? "ON" : "");
		});
	}

	@Override
	public void onDrop(StatsConnection source, String json) {
		// Drop notifications come from the background service, so they work with the app closed
	}

	@Override
	public void onLoot(StatsConnection source, String json) {
		lootLog.add(json);
	}

	@Override
	public void onDisconnected(StatsConnection source, String reason) {
		runOnUiThread(() -> {
			if (source == connection) {
				statusView.setText(reason + ". Retrying…");
				hpBar.clear();
				prayerBar.clear();
			}
		});
	}

	private void showGoading(int seconds) {
		if (seconds > 0) {
			goadingSeen = true;
			goadingView.setText("Goading potion: " + Stats.duration(seconds) + " left");
			goadingView.setBackgroundColor(Color.rgb(36, 80, 44));
			goadingView.setVisibility(View.VISIBLE);
		} else if (goadingSeen) {
			goadingView.setText("Goading potion ran out. Drink another dose!");
			goadingView.setBackgroundColor(Color.rgb(140, 70, 10));
			goadingView.setVisibility(View.VISIBLE);
		} else {
			goadingView.setVisibility(View.GONE);
		}
	}

	private static int hpColor(int hp, int maxHp) {
		if (maxHp <= 0) {
			return HP_GREEN;
		}
		float fraction = hp / (float) maxHp;
		if (fraction > 0.5f) {
			return HP_GREEN;
		}
		return fraction > 0.25f ? HP_ORANGE : HP_RED;
	}

	private void buildLoot(LinearLayout root, float dp) {
		LinearLayout header = new LinearLayout(this);
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = text(20, Color.WHITE);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setText("Loot");
		header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		Button reset = new Button(this);
		reset.setText("Reset");
		reset.setOnClickListener(v -> new AlertDialog.Builder(this)
			.setTitle("Reset loot?")
			.setMessage("This clears the totals and the drop list.")
			.setPositiveButton("Reset", (dialog, which) -> lootLog.clear())
			.setNegativeButton("Cancel", null)
			.show());
		header.addView(reset);
		LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		headerParams.topMargin = (int) (28 * dp);
		root.addView(header, headerParams);

		lootTotalView = text(16, Color.rgb(255, 214, 64));
		root.addView(lootTotalView);
		lootSourcesView = lootSection(root, "Best sources", dp);
		lootItemsView = lootSection(root, "Top items", dp);
		lootRecentView = lootSection(root, "Recent drops", dp);
	}

	private TextView lootSection(LinearLayout root, String heading, float dp) {
		TextView label = text(15, Color.rgb(150, 150, 165));
		label.setTypeface(Typeface.DEFAULT_BOLD);
		label.setText(heading);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.topMargin = (int) (14 * dp);
		root.addView(label, params);
		TextView body = text(15, Color.rgb(220, 220, 228));
		body.setLineSpacing(3 * dp, 1f);
		root.addView(body);
		return body;
	}

	@Override
	public void onLootChanged() {
		runOnUiThread(this::showLoot);
	}

	private void showLoot() {
		int count = lootLog.dropCount();
		if (count == 0) {
			lootTotalView.setText("No drops yet. Kill something!");
			lootSourcesView.setText("–");
			lootItemsView.setText("–");
			lootRecentView.setText("–");
			return;
		}
		java.text.DateFormat date = java.text.DateFormat.getDateTimeInstance(
			java.text.DateFormat.SHORT, java.text.DateFormat.SHORT);
		lootTotalView.setText(StatsService.coins(lootLog.totalValue()) + " from " + count
			+ (count == 1 ? " drop" : " drops") + " since " + date.format(new java.util.Date(lootLog.firstTime())));

		StringBuilder sources = new StringBuilder();
		List<LootLog.Tally> bySource = lootLog.bySource();
		for (int i = 0; i < Math.min(5, bySource.size()); i++) {
			LootLog.Tally tally = bySource.get(i);
			appendLine(sources, tally.name + "  ·  " + tally.count + (tally.count == 1 ? " drop" : " drops")
				+ "  ·  " + StatsService.coins(tally.value));
		}
		lootSourcesView.setText(sources);

		StringBuilder items = new StringBuilder();
		List<LootLog.Tally> byItem = lootLog.byItem();
		for (int i = 0; i < Math.min(10, byItem.size()); i++) {
			LootLog.Tally tally = byItem.get(i);
			appendLine(items, tally.name + (tally.count > 1 ? " x" + tally.count : "") + "  ·  "
				+ (tally.tradeable ? StatsService.coins(tally.value) : "untradeable"));
		}
		lootItemsView.setText(items);

		StringBuilder recent = new StringBuilder();
		java.text.DateFormat time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT);
		for (LootLog.Drop drop : lootLog.recent(10)) {
			StringBuilder names = new StringBuilder();
			for (LootLog.Item item : drop.items) {
				if (names.length() > 0) {
					names.append(", ");
				}
				names.append(item.name);
				if (item.quantity > 1) {
					names.append(" x").append(item.quantity);
				}
			}
			appendLine(recent, time.format(new java.util.Date(drop.time)) + "  " + drop.source + " (" +
				StatsService.coins(drop.total) + "): " + names);
		}
		lootRecentView.setText(recent);
	}

	private static void appendLine(StringBuilder sb, String line) {
		if (sb.length() > 0) {
			sb.append('\n');
		}
		sb.append(line);
	}

	/** A switch for an alert setting the background service reads. Alerts are on until turned off. */
	private Switch prefSwitch(String label, String key) {
		Switch view = new Switch(this);
		view.setText(label);
		view.setTextColor(Color.rgb(200, 200, 210));
		view.setTextSize(15);
		view.setChecked(prefs.getBoolean(key, true));
		view.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key, checked).apply());
		return view;
	}

	private TextView text(int sp, int color) {
		TextView view = new TextView(this);
		view.setTextSize(sp);
		view.setTextColor(color);
		return view;
	}

	private EditText input(String hint) {
		EditText input = new EditText(this);
		input.setHint(hint);
		input.setSingleLine(true);
		input.setTextColor(Color.WHITE);
		input.setHintTextColor(Color.rgb(120, 120, 130));
		return input;
	}

	/** Android 15 draws apps under the status and navigation bars, so pad the content to stay clear of them. */
	private static void applySystemBarInsets(View view) {
		view.setOnApplyWindowInsetsListener((v, insets) -> {
			int left;
			int top;
			int right;
			int bottom;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				android.graphics.Insets bars = insets.getInsets(
					WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
				left = bars.left;
				top = bars.top;
				right = bars.right;
				bottom = bars.bottom;
			} else {
				left = insets.getSystemWindowInsetLeft();
				top = insets.getSystemWindowInsetTop();
				right = insets.getSystemWindowInsetRight();
				bottom = insets.getSystemWindowInsetBottom();
			}
			v.setPadding(left, top, right, bottom);
			return insets;
		});
	}
}
