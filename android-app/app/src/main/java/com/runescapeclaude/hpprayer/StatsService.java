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

/**
 * Keeps an ongoing notification with your Hitpoints and Prayer while the app is closed. The status bar icon is your
 * current Hitpoints drawn as a number, and on Android 16 the notification also asks to be a Live Update, which shows
 * both numbers in a chip at the top of the screen (and in the Now Bar on Samsung phones).
 */
public class StatsService extends Service implements StatsConnection.Listener {
	static final String ACTION_STOP = "com.runescapeclaude.hpprayer.STOP";
	static final String KEY_STATUS_BAR = "statusBar";

	private static final String CHANNEL_ID = "live_stats";
	private static final int NOTIFICATION_ID = 1;

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
			showStatus(source, "Waiting for you to log in");
			return;
		}
		if (lastStats != null && lastStats.sameAs(stats)) {
			return;
		}
		lastStats = stats;
		lastStatus = "";
		notifications.notify(NOTIFICATION_ID, build(stats, null));
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
		Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent openIntent = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
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

		String title = "HP " + stats.hp + "/" + stats.maxHp + "  ·  Prayer " + stats.prayer + "/" + stats.maxPrayer
			+ (stats.prayerActive ? " (on)" : "");
		builder.setSmallIcon(Icon.createWithBitmap(numberIcon(stats.hp)))
			.setContentTitle(title)
			.setContentText(stats.name.isEmpty() ? "Logged in" : stats.name);
		requestLiveUpdate(builder, stats.hp + " · " + stats.prayer);
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
