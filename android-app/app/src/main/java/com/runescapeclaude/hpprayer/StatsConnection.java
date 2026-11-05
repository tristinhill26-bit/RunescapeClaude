package com.runescapeclaude.hpprayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;

/**
 * Keeps a connection open to the plugin's /events stream, and reconnects by itself whenever it drops (PC asleep,
 * RuneLite closed, phone switched Wi-Fi). Callbacks arrive on the background thread.
 */
final class StatsConnection {
	interface Listener {
		void onConnecting(StatsConnection source);

		void onConnected(StatsConnection source);

		void onStats(StatsConnection source, Stats stats);

		/** A drop the plugin thought worth a ping, as the plugin's JSON. */
		void onDrop(StatsConnection source, String json);

		void onDisconnected(StatsConnection source, String reason);
	}

	private static final int CONNECT_TIMEOUT_MS = 3000;
	// The plugin sends a heartbeat every 5 seconds, so 12 seconds of silence means the connection is dead
	private static final int READ_TIMEOUT_MS = 12000;
	private static final int RETRY_DELAY_MS = 2000;

	private final String url;
	private final Listener listener;
	private volatile boolean running;
	private volatile HttpURLConnection connection;
	private Thread thread;

	StatsConnection(String host, int port, Listener listener) {
		this.url = "http://" + host + ":" + port + "/events";
		this.listener = listener;
	}

	void start() {
		running = true;
		thread = new Thread(this::run, "stats-connection");
		thread.setDaemon(true);
		thread.start();
	}

	void stop() {
		running = false;
		HttpURLConnection c = connection;
		if (c != null) {
			// Unblocks the read on the background thread
			new Thread(c::disconnect).start();
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

	private void run() {
		while (running) {
			listener.onConnecting(this);
			String reason;
			try {
				readStream();
				reason = "RuneLite closed the connection";
			} catch (IOException e) {
				reason = describe(e);
			}
			if (!running) {
				return;
			}
			listener.onDisconnected(this, reason);
			try {
				Thread.sleep(RETRY_DELAY_MS);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	private void readStream() throws IOException {
		HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		connection = c;
		try {
			c.setConnectTimeout(CONNECT_TIMEOUT_MS);
			c.setReadTimeout(READ_TIMEOUT_MS);
			c.setUseCaches(false);
			c.setRequestProperty("Accept", "text/event-stream");
			int code = c.getResponseCode();
			if (code != 200) {
				throw new IOException("Unexpected answer from the PC (HTTP " + code + ")");
			}
			listener.onConnected(this);

			BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
			String line;
			// Server-Sent Events name an event with an "event:" line before its data; unnamed ones are stats
			String event = "";
			while (running && (line = in.readLine()) != null) {
				if (line.isEmpty()) {
					event = "";
				} else if (line.startsWith("event:")) {
					event = line.substring(6).trim();
				} else if (line.startsWith("data:")) {
					String data = line.substring(5).trim();
					if (event.equals("drop")) {
						listener.onDrop(this, data);
					} else if (event.isEmpty()) {
						try {
							listener.onStats(this, Stats.parse(data));
						} catch (JSONException e) {
							// Ignore a garbled update; the next one arrives within a game tick
						}
					}
				}
			}
		} finally {
			c.disconnect();
			connection = null;
		}
	}

	private static String describe(IOException e) {
		String type = e.getClass().getSimpleName();
		if (type.contains("Timeout")) {
			return "No answer. Is RuneLite running and the PC on the same Wi-Fi?";
		}
		if (type.contains("ConnectException") || String.valueOf(e.getMessage()).contains("ECONNREFUSED")) {
			return "The PC refused the connection. Is the Phone HP & Prayer plugin on?";
		}
		if (type.contains("UnknownHost")) {
			return "Couldn't find that address";
		}
		return e.getMessage() != null ? e.getMessage() : type;
	}
}
