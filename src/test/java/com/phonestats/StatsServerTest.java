package com.phonestats;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatsServerTest
{
	private StatsServer server;

	@Before
	public void setUp() throws Exception
	{
		server = new StatsServer(0);
		server.start();
	}

	@After
	public void tearDown()
	{
		server.stop();
	}

	@Test
	public void snapshotJsonEscapesNames()
	{
		StatsSnapshot snapshot = new StatsSnapshot(true, "Zezima \"the\" \\best", 45, 99, 12, 77, true, 90);
		assertEquals("{\"loggedIn\":true,\"name\":\"Zezima \\\"the\\\" \\\\best\",\"hp\":45,\"maxHp\":99," +
			"\"prayer\":12,\"maxPrayer\":77,\"prayerActive\":true,\"goading\":90}", snapshot.toJson());
	}

	@Test
	public void statsAnswersWithLatestSnapshot() throws Exception
	{
		server.publish("{\"hp\":10}");
		try (Socket socket = connect("/stats"))
		{
			BufferedReader in = reader(socket);
			assertEquals("HTTP/1.1 200 OK", in.readLine());
			skipHeaders(in);
			assertEquals("{\"hp\":10}", in.readLine());
		}
	}

	@Test
	public void eventsStreamsEveryUpdate() throws Exception
	{
		server.publish("{\"hp\":1}");
		try (Socket socket = connect("/events"))
		{
			BufferedReader in = reader(socket);
			assertEquals("HTTP/1.1 200 OK", in.readLine());
			skipHeaders(in);
			assertEquals("data: {\"hp\":1}", in.readLine());
			assertEquals("", in.readLine());

			waitForListener();
			server.publish("{\"hp\":2}");
			assertEquals("data: {\"hp\":2}", nextData(in));
			server.publish("{\"hp\":3}");
			assertEquals("data: {\"hp\":3}", nextData(in));
		}
	}

	@Test
	public void namedEventsAreSentButNotKept() throws Exception
	{
		server.publish("{\"hp\":1}");
		try (Socket socket = connect("/events"))
		{
			BufferedReader in = reader(socket);
			skipHeaders(in);
			assertEquals("data: {\"hp\":1}", in.readLine());
			waitForListener();
			server.publishEvent("drop", "{\"total\":5}");
			String line;
			do
			{
				line = in.readLine();
			}
			while (line.startsWith(":") || line.isEmpty());
			assertEquals("event: drop", line);
			assertEquals("data: {\"total\":5}", in.readLine());
		}
		try (Socket socket = connect("/stats"))
		{
			BufferedReader in = reader(socket);
			skipHeaders(in);
			assertEquals("{\"hp\":1}", in.readLine());
		}
	}

	@Test
	public void keptEventsAreReplayedToNewPhones() throws Exception
	{
		server.publish("{\"hp\":1}");
		server.publishEvent("loot", "{\"id\":\"a\"}", true);
		server.publishEvent("drop", "{\"total\":5}");
		try (Socket socket = connect("/events"))
		{
			BufferedReader in = reader(socket);
			skipHeaders(in);
			assertEquals("data: {\"hp\":1}", in.readLine());
			assertEquals("", in.readLine());
			assertEquals("event: loot", in.readLine());
			assertEquals("data: {\"id\":\"a\"}", in.readLine());
		}
	}

	@Test
	public void lootJsonHasIdAndTime()
	{
		DropMessage loot = new DropMessage("s-1", 1234, "Goblin");
		loot.add("Bones", 1, 100, true);
		assertEquals("{\"id\":\"s-1\",\"time\":1234,\"source\":\"Goblin\",\"total\":100,\"items\":[" +
			"{\"name\":\"Bones\",\"quantity\":1,\"value\":100,\"tradeable\":true}]}", loot.toJson());
	}

	@Test
	public void dropJsonListsItems()
	{
		DropMessage drop = new DropMessage("Vorkath \"boss\"");
		drop.add("Vorkath's head", 1, 0, false);
		drop.add("Dragon bones", 2, 10000, true);
		assertEquals("{\"source\":\"Vorkath \\\"boss\\\"\",\"total\":10000,\"items\":[" +
			"{\"name\":\"Vorkath's head\",\"quantity\":1,\"value\":0,\"tradeable\":false}," +
			"{\"name\":\"Dragon bones\",\"quantity\":2,\"value\":10000,\"tradeable\":true}]}", drop.toJson());
	}

	@Test
	public void unknownPathIs404() throws Exception
	{
		try (Socket socket = connect("/nope"))
		{
			assertTrue(reader(socket).readLine().startsWith("HTTP/1.1 404"));
		}
	}

	private Socket connect(String path) throws Exception
	{
		Socket socket = new Socket("127.0.0.1", server.getPort());
		socket.setSoTimeout(5000);
		OutputStream out = socket.getOutputStream();
		out.write(("GET " + path + " HTTP/1.1\r\nHost: test\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
		out.flush();
		return socket;
	}

	private void waitForListener() throws InterruptedException
	{
		for (int i = 0; i < 100 && server.getListenerCount() == 0; i++)
		{
			Thread.sleep(10);
		}
		assertEquals(1, server.getListenerCount());
	}

	private static BufferedReader reader(Socket socket) throws Exception
	{
		return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
	}

	private static void skipHeaders(BufferedReader in) throws Exception
	{
		String line;
		do
		{
			line = in.readLine();
		}
		while (line != null && !line.isEmpty());
	}

	private static String nextData(BufferedReader in) throws Exception
	{
		String line;
		while ((line = in.readLine()) != null)
		{
			if (line.startsWith("data:"))
			{
				return line;
			}
		}
		return null;
	}
}
