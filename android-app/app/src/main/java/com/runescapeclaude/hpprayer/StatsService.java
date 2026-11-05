package com.runescapeclaude.hpprayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import java.lang.reflect.Method;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Keeps an ongoing notification with your Prayer and Hitpoints while the app is closed, and sends prayer and drop
 * alerts. The status bar icon is your current Prayer drawn as a number (Hitpoints once Prayer is empty), and on
 * Android 16 the notification also asks to be a Live Update, which shows it in a chip at the top of the screen (and
 * in the Now Bar on Samsung phones).
 */
public class StatsService extends Service implements StatsConnection.Listener {
	static final String ACTION_STOP = "com.runescapeclaude.hpprayer.STOP";
	static final String KEY_STATUS_BAR = "statusBar";
	static final String KEY_PRAYER_ALERTS = "prayerAlerts";
	static final String KEY_DROP_ALERTS = "dropAlerts";
	static final String KEY_GOADING_ALERTS = "goadingAlerts";

	private static final String CHANNEL_ID = "live_stats";
	private static final String PRAYER_CHANNEL_ID = "prayer_alerts";
	private static final String DROP_CHANNEL_ID = "drop_alerts";
	private static final String GOADING_CHANNEL_ID = "goading_alerts";
	private static final int NOTIFICATION_ID = 1;
	private static final int PRAYER_NOTIFICATION_ID = 2;
	private static final int GOADING_NOTIFICATION_ID = 3;
	private static final int[] PRAYER_THRESHOLDS = { 20, 10, 0 };
	private static final int NOT_LOW = Integer.MAX_VALUE;
	private static final int UNKNOWN = -1;

	// The lowest prayer threshold you're currently at or below, so each one only alerts once on the way down
	private int prayerBracket = UNKNOWN;
	private int nextDropId = 100;
	private int lastGoading = -1;

	private NotificationManager notifications;
	private volatile StatsConnection connection;
	private Stats lastStats;
	private String lastStatus = "";

	static SharedPreferences prefs(android.content.Context context) {
		return context.getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notifications = getSystemService(NotificationManager.class);
		NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Live HP & Prayer",
			NotificationManager.IMPORTANCE_DEFAULT);
		channel.setDescription("Your Hitpoints and Prayer while the app is closed");
		channel.setSound(null, null);
		channel.enableVibration(false);
		channel.setShowBadge(false);
		notifications.createNotificationChannel(channel);

		NotificationChannel prayerChannel = new NotificationChannel(PRAYER_CHANNEL_ID, "Prayer alerts",
			NotificationManager.IMPORTANCE_HIGH);
		prayerChannel.setDescription("When your Prayer drops to 20, 10 and 0");
		prayerChannel.enableVibration(true);
		prayerChannel.setVibrationPattern(new long[] { 0, 250, 120, 250 });
		notifications.createNotificationChannel(prayerChannel);

		NotificationChannel goadingChannel = new NotificationChannel(GOADING_CHANNEL_ID, "Goading potion",
			NotificationManager.IMPORTANCE_HIGH);
		goadingChannel.setDescription("When your goading potion runs out");
		notifications.createNotificationChannel(goadingChannel);

		NotificationChannel dropChannel = new NotificationChannel(DROP_CHANNEL_ID, "Drop alerts",
			NotificationManager.IMPORTANCE_HIGH);
		dropChannel.setDescription("Valuable and untradeable drops, as set in the RuneLite plugin");
		notifications.createNotificationChannel(dropChannel);

		Notification first = build(null, "Connecting…");
		if (Build.VERSION.SDK_INT >= 34) {
			startForeground(NOTIFICATION_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
		} else {
			startForeground(NOTIFICATION_ID, first);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && ACTION_STOP.equals(intent.getAction())) {
			prefs(this).edit().putBoolean(KEY_STATUS_BAR, false).apply();
			stopSelf();
			return START_NOT_STICKY;
		}
		if (connection == null) {
			SharedPreferences prefs = prefs(this);
			String host = prefs.getString(MainActivity.KEY_HOST, "");
			if (host.isEmpty()) {
				stopSelf();
				return START_NOT_STICKY;
			}
			connection = new StatsConnection(host, prefs.getInt(MainActivity.KEY_PORT, MainActivity.DEFAULT_PORT),
				this);
			connection.start();
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		StatsConnection c = connection;
		connection = null;
		if (c != null) {
			c.stop();
		}
		stopForeground(STOP_FOREGROUND_REMOVE);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConnecting(StatsConnection source) {
		showStatus(source, "Connecting…");
	}

	@Override
	public void onConnected(StatsConnection source) {
		showStatus(source, "Connected");
	}

	@Override
	public void onDisconnected(StatsConnection source, String reason) {
		showStatus(source, reason);
	}

	@Override
	public synchronized void onStats(StatsConnection source, Stats stats) {
		if (source != connection) {
			return;
		}
		if (!stats.loggedIn) {
			prayerBracket = UNKNOWN;
			lastGoading = -1;
			showStatus(source, "Waiting for you to log in");
			return;
		}
		checkPrayer(stats);
		checkGoading(stats);
		if (lastStats != null && lastStats.sameAs(stats)) {
			return;
		}
		lastStats = stats;
		lastStatus = "";
		notifications.notify(NOTIFICATION_ID, build(stats, null));
	}

	private void checkPrayer(Stats stats) {
		// Levels read as 0 for a moment while logging in, so wait until they're loaded
		if (stats.maxPrayer <= 0) {
			return;
		}
		int bracket = NOT_LOW;
		for (int threshold : PRAYER_THRESHOLDS) {
			if (stats.prayer <= threshold) {
				bracket = threshold;
			}
		}
		// No alert for where you already were when the app connected, only for drops after that
		boolean dropped = prayerBracket != UNKNOWN && bracket < prayerBracket;
		prayerBracket = bracket;
		if (!dropped || !prefs(this).getBoolean(KEY_PRAYER_ALERTS, true)) {
			return;
		}

		String title = bracket == 0 ? "Out of prayer!" : "Prayer is low: " + stats.prayer;
		Notification alert = new Notification.Builder(this, PRAYER_CHANNEL_ID)
			.setSmallIcon(Icon.createWithBitmap(numberIcon(stats.prayer)))
			.setContentTitle(title)
			.setContentText("Prayer " + stats.prayer + "/" + stats.maxPrayer + "  ·  HP " + stats.hp + "/" + stats.maxHp)
			.setCategory(Notification.CATEGORY_ALARM)
			.setContentIntent(openAppIntent())
			.setAutoCancel(true)
			.setTimeoutAfter(60_000)
			.build();
		// One notification that each alert replaces, so the shade doesn't fill up
		notifications.notify(PRAYER_NOTIFICATION_ID, alert);
	}

	private void checkGoading(Stats stats) {
		boolean ranOut = lastGoading > 0 && stats.goading == 0;
		lastGoading = stats.goading;
		if (stats.goading > 0) {
			// Drank another dose, so the old alert no longer applies
			notifications.cancel(GOADING_NOTIFICATION_ID);
		}
		if (!ranOut || !prefs(this).getBoolean(KEY_GOADING_ALERTS, true)) {
			return;
		}
		Notification alert = new Notification.Builder(this, GOADING_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_stat_heart)
			.setContentTitle("Goading potion ran out")
			.setContentText("Drink another dose to keep monsters coming")
			.setCategory(Notification.CATEGORY_REMINDER)
			.setContentIntent(openAppIntent())
			.setAutoCancel(true)
			.build();
		notifications.notify(GOADING_NOTIFICATION_ID, alert);
	}

	@Override
	public void onLoot(StatsConnection source, String json) {
		LootLog.get(this).add(json);
	}

	@Override
	public void onDrop(StatsConnection source, String json) {
		if (source != connection || !prefs(this).getBoolean(KEY_DROP_ALERTS, true)) {
			return;
		}
		try {
			JSONObject drop = new JSONObject(json);
			JSONArray items = drop.getJSONArray("items");
			if (items.length() == 0) {
				return;
			}
			StringBuilder lines = new StringBuilder();
			for (int i = 0; i < items.length(); i++) {
				JSONObject item = items.getJSONObject(i);
				if (lines.length() > 0) {
					lines.append('\n');
				}
				lines.append(item.getString("name"));
				int quantity = item.optInt("quantity", 1);
				if (quantity > 1) {
					lines.append(" x").append(quantity);
				}
				if (!item.optBoolean("tradeable", true)) {
					lines.append(" (untradeable)");
				} else {
					lines.append(" (").append(coins(item.optLong("value"))).append(')');
				}
			}

			JSONObject first = items.getJSONObject(0);
			String title = items.length() == 1
				? "Drop: " + first.getString("name")
				: "Drop: " + items.length() + " items, " + coins(drop.optLong("total"));
			String text = "From " + drop.optString("source", "?") + "\n" + lines;
			Notification alert = new Notification.Builder(this, DROP_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_stat_heart)
				.setContentTitle(title)
				.setContentText(text.replace('\n', ' '))
				.setStyle(new Notification.BigTextStyle().bigText(text))
				.setCategory(Notification.CATEGORY_EVENT)
				.setContentIntent(openAppIntent())
				.setAutoCancel(true)
				.build();
			notifications.notify(nextDropId++, alert);
		} catch (JSONException ignored) {
			// A drop the app doesn't understand
		}
	}

	/** 950, 12.5K, 2.4M, 1.2B, like the game's stack numbers but with a decimal. */
	static String coins(long value) {
		if (value >= 1_000_000_000L) {
			return String.format(Locale.US, "%.1fB", value / 1e9);
		}
		if (value >= 1_000_000L) {
			return String.format(Locale.US, "%.1fM", value / 1e6);
		}
		if (value >= 1_000L) {
			return String.format(Locale.US, "%.1fK", value / 1e3);
		}
		return value + " gp";
	}

	private PendingIntent openAppIntent() {
		Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
	}

	private synchronized void showStatus(StatsConnection source, String status) {
		if (source != connection || status.equals(lastStatus)) {
			return;
		}
		lastStatus = status;
		lastStats = null;
		notifications.notify(NOTIFICATION_ID, build(null, status));
	}

	private Notification build(Stats stats, String status) {
		PendingIntent openIntent = openAppIntent();
		Intent stop = new Intent(this, StatsService.class).setAction(ACTION_STOP);
		PendingIntent stopIntent = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE);

		Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setShowWhen(false)
			.setCategory(Notification.CATEGORY_STATUS)
			.setContentIntent(openIntent)
			.addAction(new Notification.Action.Builder(null, "Stop", stopIntent).build());

		if (stats == null) {
			builder.setSmallIcon(R.drawable.ic_stat_heart)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(status);
			return builder.build();
		}

		String title = "Prayer " + stats.prayer + "/" + stats.maxPrayer + (stats.prayerActive ? " (on)" : "")
			+ "  ·  HP " + stats.hp + "/" + stats.maxHp;
		// Prayer first; once it's empty, Hitpoints is what matters
		boolean showPrayer = stats.prayer > 0;
		int shown = showPrayer ? stats.prayer : stats.hp;
		builder.setSmallIcon(Icon.createWithBitmap(numberIcon(shown)))
			.setContentTitle(title)
			.setContentText((stats.name.isEmpty() ? "Logged in" : stats.name)
				+ (stats.goading > 0 ? "  ·  Goading " + Stats.duration(stats.goading) : ""));
		requestLiveUpdate(builder, (showPrayer ? "Pray " : "HP ") + shown);
		return builder.build();
	}

	/**
	 * Android 16 can show an ongoing notification as a Live Update: a chip with a few characters of text in the status
	 * bar. Called through reflection so the app also builds and runs on older Android versions.
	 */
	private static void requestLiveUpdate(Notification.Builder builder, String chipText) {
		if (Build.VERSION.SDK_INT < 36) {
			return;
		}
		Bundle extras = new Bundle();
		extras.putBoolean("android.requestPromotedOngoing", true);
		builder.addExtras(extras);
		invoke(builder, "setRequestPromotedOngoing", boolean.class, true);
		invoke(builder, "setShortCriticalText", String.class, chipText);
	}

	private static void invoke(Notification.Builder builder, String name, Class<?> type, Object value) {
		try {
			Method method = Notification.Builder.class.getMethod(name, type);
			method.invoke(builder, value);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// Not on this Android version
		}
	}

	/** Draws a number as a white status bar icon, sized to fill as much of the icon as it can. */
	private static Bitmap numberIcon(int number) {
		int size = 96;
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.WHITE);
		paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		paint.setTextAlign(Paint.Align.CENTER);
		String text = String.valueOf(number);
		float textSize = 88;
		paint.setTextSize(textSize);
		while (paint.measureText(text) > size - 2 && textSize > 20) {
			textSize -= 2;
			paint.setTextSize(textSize);
		}
		float baseline = size / 2f - (paint.descent() + paint.ascent()) / 2f;
		canvas.drawText(text, size / 2f, baseline, paint);
		return bitmap;
	}
}
