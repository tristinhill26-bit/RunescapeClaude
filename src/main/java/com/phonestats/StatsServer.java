package com.phonestats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * A tiny HTTP server for the phone app. It has no RuneLite code in it, so it can be tested on its own.
 *
 * <ul>
 * <li>{@code GET /events} keeps the connection open and sends every new snapshot as a Server-Sent Event, which is
 * what the phone app uses. Drops are sent on the same stream as {@code event: drop}.</li>
 * <li>{@code GET /} (or {@code /stats}) answers once with the latest snapshot, handy for checking the connection from
 * a phone's web browser.</li>
 * </ul>
 *
 * Snapshots are sent from a background thread, so a slow or vanished phone never holds up the game.
 */
@Slf4j
public class StatsServer
{
	private static final int HEARTBEAT_SECONDS = 5;
	private static final int REQUEST_TIMEOUT_MS = 5000;

	private final int port;
	private final List<Socket> listeners = new CopyOnWriteArrayList<>();
	private final ExecutorService sender = Executors.newSingleThreadExecutor(daemon("phone-stats-sender"));
	private final ScheduledExecutorService heartbeat =
		Executors.newSingleThreadScheduledExecutor(daemon("phone-stats-heartbeat"));

	private volatile String latest = "{\"loggedIn\":false}";
	private volatile boolean running;
	private ServerSocket serverSocket;

	public StatsServer(int port)
	{
		this.port = port;
	}

	/**
	 * Start listening on every network address of this PC, so phones on the same Wi-Fi can connect.
	 */
	public void start() throws IOException
	{
		serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(port));
		running = true;

		Thread acceptThread = daemon("phone-stats-accept").newThread(this::acceptLoop);
		acceptThread.start();
		// A comment line every few seconds lets both sides notice a dropped connection quickly
		heartbeat.scheduleAtFixedRate(() -> sendToAll(": ping\n\n"), HEARTBEAT_SECONDS, HEARTBEAT_SECONDS,
			TimeUnit.SECONDS);
	}

	public void stop()
	{
		running = false;
		heartbeat.shutdownNow();
		sender.shutdownNow();
		closeQuietly(serverSocket);
		for (Socket socket : listeners)
		{
			closeQuietly(socket);
		}
		listeners.clear();
	}

	public int getPort()
	{
		return serverSocket != null ? serverSocket.getLocalPort() : port;
	}

	public int getListenerCount()
	{
		return listeners.size();
	}

	/**
	 * Send a JSON snapshot to every connected phone, and remember it for phones that connect later.
	 */
	public void publish(String json)
	{
		latest = json;
		sendToAll(event(json));
	}

	/**
	 * Send a one-off named event, such as a drop, to every connected phone. Unlike {@link #publish}, it isn't kept for
	 * phones that connect later.
	 */
	public void publishEvent(String name, String json)
	{
		sendToAll("event: " + name + "\n" + event(json));
	}

	private void sendToAll(String message)
	{
		if (!running || listeners.isEmpty())
		{
			return;
		}

		byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
		try
		{
			sender.execute(() ->
			{
				for (Socket socket : listeners)
				{
					try
					{
						OutputStream out = socket.getOutputStream();
						out.write(bytes);
						out.flush();
					}
					catch (IOException e)
					{
						// The phone went away (screen off, Wi-Fi dropped, app closed)
						listeners.remove(socket);
						closeQuietly(socket);
					}
				}
			});
		}
		catch (java.util.concurrent.RejectedExecutionException ignored)
		{
			// Stopping
		}
	}

	private void acceptLoop()
	{
		while (running)
		{
			try
			{
				Socket socket = serverSocket.accept();
				daemon("phone-stats-request").newThread(() -> handle(socket)).start();
			}
			catch (IOException e)
			{
				if (running)
				{
					log.warn("Phone stats server stopped accepting connections", e);
				}
				return;
			}
		}
	}

	private void handle(Socket socket)
	{
		try
		{
			socket.setSoTimeout(REQUEST_TIMEOUT_MS);
			socket.setTcpNoDelay(true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),
				StandardCharsets.US_ASCII));
			String requestLine = in.readLine();
			if (requestLine == null)
			{
				closeQuietly(socket);
				return;
			}
			// Skip the headers; nothing in them matters here
			String header;
			do
			{
				header = in.readLine();
			}
			while (header != null && !header.isEmpty());

			String[] parts = requestLine.split(" ");
			String method = parts[0];
			String path = parts.length > 1 ? parts[1] : "/";
			int query = path.indexOf('?');
			if (query >= 0)
			{
				path = path.substring(0, query);
			}

			OutputStream out = socket.getOutputStream();
			if (!method.equals("GET"))
			{
				respond(out, "405 Method Not Allowed", "text/plain", "Only GET is supported\n");
				closeQuietly(socket);
			}
			else if (path.equals("/events"))
			{
				socket.setSoTimeout(0);
				String headers = "HTTP/1.1 200 OK\r\n" +
					"Content-Type: text/event-stream; charset=utf-8\r\n" +
					"Cache-Control: no-cache\r\n" +
					"Connection: keep-alive\r\n" +
					"Access-Control-Allow-Origin: *\r\n" +
					"\r\n";
				// Send the headers and the current stats on the sender thread, so they can't interleave with an
				// update that's being sent at the same moment, and the phone never misses one
				sender.execute(() ->
				{
					try
					{
						out.write((headers + event(latest)).getBytes(StandardCharsets.UTF_8));
						out.flush();
						listeners.add(socket);
					}
					catch (IOException e)
					{
						closeQuietly(socket);
					}
				});
			}
			else if (path.equals("/") || path.equals("/stats"))
			{
				respond(out, "200 OK", "application/json; charset=utf-8", latest + "\n");
				closeQuietly(socket);
			}
			else
			{
				respond(out, "404 Not Found", "text/plain", "Try /events or /stats\n");
				closeQuietly(socket);
			}
		}
		catch (SocketTimeoutException e)
		{
			closeQuietly(socket);
		}
		catch (Exception e)
		{
			log.debug("Phone stats request failed", e);
			closeQuietly(socket);
		}
	}

	private static void respond(OutputStream out, String status, String type, String body) throws IOException
	{
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		String headers = "HTTP/1.1 " + status + "\r\n" +
			"Content-Type: " + type + "\r\n" +
			"Content-Length: " + bytes.length + "\r\n" +
			"Cache-Control: no-cache\r\n" +
			"Access-Control-Allow-Origin: *\r\n" +
			"Connection: close\r\n" +
			"\r\n";
		out.write(headers.getBytes(StandardCharsets.US_ASCII));
		out.write(bytes);
		out.flush();
	}

	private static String event(String json)
	{
		return "data: " + json + "\n\n";
	}

	private static void closeQuietly(java.io.Closeable closeable)
	{
		if (closeable == null)
		{
			return;
		}
		try
		{
			closeable.close();
		}
		catch (IOException ignored)
		{
		}
	}

	private static java.util.concurrent.ThreadFactory daemon(String name)
	{
		return runnable ->
		{
			Thread thread = new Thread(runnable, name);
			thread.setDaemon(true);
			return thread;
		};
	}
}
