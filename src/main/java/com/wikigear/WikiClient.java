package com.wikigear;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the OSRS Wiki's MediaWiki API. Callbacks run on OkHttp's threads.
 */
@Slf4j
@Singleton
public class WikiClient
{
	private static final String WIKI = "https://oldschool.runescape.wiki";
	private static final HttpUrl API = HttpUrl.get(WIKI + "/api.php");
	private static final int SEARCH_LIMIT = 15;
	private static final int THUMBNAIL_SIZE = 40;

	@RequiredArgsConstructor
	public static class Monster
	{
		public final String title;
		@Nullable
		public final String thumbnailUrl;
	}

	@RequiredArgsConstructor
	public static class StrategyPage
	{
		/**
		 * The page the setups were found on, e.g. "Vorkath/Strategies"
		 */
		public final String title;
		public final List<GearSetup> setups;
	}

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	/**
	 * Search for monster pages. Falls back to a search of all pages if no monsters match.
	 */
	public void searchMonsters(String query, Consumer<List<Monster>> onResult, Consumer<String> onError) {
		search(query, true, results -> {
			if (results.isEmpty()) {
				search(query, false, onResult, onError);
			} else {
				onResult.accept(results);
			}
		}, onError);
	}

	private void search(String query, boolean monstersOnly, Consumer<List<Monster>> onResult, Consumer<String> onError) {
		String text = query.trim();
		// Match partial words, so "vork" finds Vorkath
		if (!text.endsWith("*") && !text.endsWith("\""))
			text += "*";
		if (monstersOnly)
			text += " incategory:Monsters";

		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("generator", "search")
			.addQueryParameter("gsrsearch", text)
			.addQueryParameter("gsrnamespace", "0")
			.addQueryParameter("gsrlimit", String.valueOf(SEARCH_LIMIT))
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", String.valueOf(THUMBNAIL_SIZE))
			.addQueryParameter("redirects", "1")
			.build();

		getJson(url, json -> {
			var results = new ArrayList<SearchResult>();
			JsonObject queryResult = json.getAsJsonObject("query");
			if (queryResult != null && queryResult.has("pages")) {
				for (JsonElement element : queryResult.getAsJsonArray("pages")) {
					JsonObject page = element.getAsJsonObject();
					String thumbnail = null;
					if (page.has("thumbnail"))
						thumbnail = absoluteUrl(page.getAsJsonObject("thumbnail").get("source").getAsString());
					int index = page.has("index") ? page.get("index").getAsInt() : Integer.MAX_VALUE;
					results.add(new SearchResult(index, new Monster(page.get("title").getAsString(), thumbnail)));
				}
			}
			results.sort(Comparator.comparingInt(r -> r.index));
			var monsters = new ArrayList<Monster>();
			for (var result : results)
				monsters.add(result.monster);
			onResult.accept(monsters);
		}, onError);
	}

	/**
	 * Load the recommended equipment for a monster, from its strategy page if it has one, or else the monster's own page.
	 */
	public void fetchStrategies(String monsterTitle, Consumer<StrategyPage> onResult, Consumer<String> onError) {
		fetchSetups(monsterTitle + "/Strategies", page -> {
			if (page != null && !page.setups.isEmpty()) {
				onResult.accept(page);
				return;
			}
			fetchSetups(monsterTitle, fallback -> {
				if (fallback != null && !fallback.setups.isEmpty()) {
					onResult.accept(fallback);
				} else {
					onResult.accept(new StrategyPage(monsterTitle, new ArrayList<>()));
				}
			}, onError);
		}, onError);
	}

	private void fetchSetups(String title, Consumer<StrategyPage> onResult, Consumer<String> onError) {
		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("prop", "wikitext")
			.addQueryParameter("redirects", "1")
			.addQueryParameter("page", title)
			.build();

		getJson(url, json -> {
			JsonObject parse = json.getAsJsonObject("parse");
			if (parse == null || !parse.has("wikitext")) {
				// Most likely a missing page
				onResult.accept(null);
				return;
			}
			String wikitext = parse.get("wikitext").getAsString();
			String pageTitle = parse.has("title") ? parse.get("title").getAsString() : title;
			onResult.accept(new StrategyPage(pageTitle, RecommendedEquipmentParser.parse(wikitext)));
		}, onError);
	}

	public void fetchImage(String url, Consumer<BufferedImage> onResult) {
		Request request = new Request.Builder().url(url).build();
		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException ex) {
				log.debug("Failed to load wiki image {}", url, ex);
			}

			@Override
			public void onResponse(Call call, Response response) {
				try (response; ResponseBody body = response.body()) {
					if (!response.isSuccessful() || body == null)
						return;
					BufferedImage image;
					try (InputStream in = body.byteStream()) {
						synchronized (ImageIO.class) {
							image = ImageIO.read(in);
						}
					}
					if (image != null)
						onResult.accept(image);
				} catch (IOException ex) {
					log.debug("Failed to read wiki image {}", url, ex);
				}
			}
		});
	}

	public static String pageUrl(String title) {
		return WIKI + "/w/" + title.replace(' ', '_');
	}

	private void getJson(HttpUrl url, Consumer<JsonObject> onResult, Consumer<String> onError) {
		Request request = new Request.Builder().url(url).build();
		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException ex) {
				log.debug("Wiki request failed: {}", url, ex);
				onError.accept("Couldn't reach the wiki: " + ex.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) {
				try (response; ResponseBody body = response.body()) {
					if (!response.isSuccessful() || body == null) {
						onError.accept("The wiki responded with an error (" + response.code() + ")");
						return;
					}
					onResult.accept(gson.fromJson(body.charStream(), JsonObject.class));
				} catch (Exception ex) {
					log.warn("Error handling wiki response for {}", url, ex);
					onError.accept("Couldn't read the wiki's response");
				}
			}
		});
	}

	private static String absoluteUrl(String url) {
		if (url.startsWith("//"))
			return "https:" + url;
		if (url.startsWith("/"))
			return WIKI + url;
		return url;
	}

	@RequiredArgsConstructor
	private static class SearchResult
	{
		final int index;
		final Monster monster;
	}
}
