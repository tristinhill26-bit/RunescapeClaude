package com.wikigear;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
	private static final int MAX_RESULTS = 20;
	private static final int THUMBNAIL_SIZE = 40;

	/**
	 * A monster or activity which may have recommended equipment, e.g. "Vorkath" or "Barrows"
	 */
	public static class Target
	{
		public final String name;
		/**
		 * A page known to hold recommended equipment, e.g. "Barrows/Strategy", or null to look for one
		 */
		@Nullable
		public final String page;
		@Nullable
		public volatile String thumbnailUrl;

		Target(String name, @Nullable String page, @Nullable String thumbnailUrl) {
			this.name = name;
			this.page = page;
			this.thumbnailUrl = thumbnailUrl;
		}

		/**
		 * Whether this was found through a page with recommended equipment, such as a strategy guide
		 */
		public boolean isGuide() {
			return page != null;
		}
	}

	public static class StrategyPage
	{
		/**
		 * The page the setups were found on, e.g. "Vorkath/Strategies"
		 */
		public final String title;
		public final List<GearSetup> setups;
		/**
		 * Whether this takes place in the Wilderness, so blighted items can be used
		 */
		public final boolean wilderness;

		public StrategyPage(String title, List<GearSetup> setups) {
			this(title, setups, false);
		}

		public StrategyPage(String title, List<GearSetup> setups, boolean wilderness) {
			this.title = title;
			this.setups = setups;
			this.wilderness = wilderness;
		}
	}

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	/**
	 * Search for monsters, and for activities like Barrows, raids and minigames whose pages have recommended equipment.
	 * Falls back to a search of all pages if neither finds anything.
	 */
	public void search(String query, Consumer<List<Target>> onResult, Consumer<String> onError) {
		String text = query.trim();
		// Match partial words, so "vork" finds Vorkath
		if (!text.endsWith("*") && !text.endsWith("\""))
			text += "*";
		String searchText = text;

		var guides = new AtomicReference<List<SearchHit>>(List.of());
		var monsters = new AtomicReference<List<SearchHit>>(List.of());
		var pending = new AtomicInteger(2);
		var failures = new AtomicInteger();
		var lastError = new AtomicReference<String>();

		Runnable onBothDone = () -> {
			if (pending.decrementAndGet() > 0)
				return;
			if (failures.get() == 2) {
				onError.accept(lastError.get());
				return;
			}

			var targets = combine(query, guides.get(), monsters.get());
			if (!targets.isEmpty()) {
				addThumbnails(targets, onResult);
				return;
			}

			searchPages(searchText, hits -> {
				var fallback = new ArrayList<Target>();
				for (var hit : hits)
					fallback.add(new Target(hit.title, null, hit.thumbnailUrl));
				onResult.accept(fallback);
			}, onError);
		};
		Consumer<String> onSearchError = error -> {
			failures.incrementAndGet();
			lastError.set(error);
			onBothDone.run();
		};

		searchPages(searchText + " insource:\"Recommended equipment\"", hits -> {
			guides.set(hits);
			onBothDone.run();
		}, onSearchError);
		searchPages(searchText + " incategory:Monsters", hits -> {
			monsters.set(hits);
			onBothDone.run();
		}, onSearchError);
	}

	/**
	 * Merge pages with recommended equipment and monster pages into one list, named after their main page,
	 * so "Barrows/Strategy" shows up as "Barrows". Names starting with the query come first.
	 */
	static List<Target> combine(String query, List<SearchHit> guides, List<SearchHit> monsters) {
		var targets = new LinkedHashMap<String, Target>();
		for (var hit : guides) {
			String name = mainPage(hit.title);
			// Subpages rarely have their own image, so the main page's image is looked up later
			String thumbnail = name.equals(hit.title) ? hit.thumbnailUrl : null;
			targets.putIfAbsent(name.toLowerCase(Locale.ROOT), new Target(name, hit.title, thumbnail));
		}
		for (var hit : monsters)
			targets.putIfAbsent(hit.title.toLowerCase(Locale.ROOT), new Target(hit.title, null, hit.thumbnailUrl));

		String prefix = query.trim().replace("*", "").toLowerCase(Locale.ROOT);
		var list = new ArrayList<>(targets.values());
		list.sort(Comparator.comparingInt(t -> t.name.toLowerCase(Locale.ROOT).startsWith(prefix) ? 0 : 1));
		if (list.size() > MAX_RESULTS)
			list.subList(MAX_RESULTS, list.size()).clear();
		return list;
	}

	static String mainPage(String title) {
		int slash = title.indexOf('/');
		return slash > 0 ? title.substring(0, slash) : title;
	}

	private void searchPages(String searchText, Consumer<List<SearchHit>> onResult, Consumer<String> onError) {
		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("generator", "search")
			.addQueryParameter("gsrsearch", searchText)
			.addQueryParameter("gsrnamespace", "0")
			.addQueryParameter("gsrlimit", String.valueOf(SEARCH_LIMIT))
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", String.valueOf(THUMBNAIL_SIZE))
			.addQueryParameter("redirects", "1")
			.build();

		getJson(url, json -> {
			var hits = new ArrayList<SearchHit>();
			for (JsonObject page : pages(json)) {
				int index = page.has("index") ? page.get("index").getAsInt() : Integer.MAX_VALUE;
				hits.add(new SearchHit(index, page.get("title").getAsString(), thumbnail(page)));
			}
			hits.sort(Comparator.comparingInt(hit -> hit.index));
			onResult.accept(hits);
		}, onError);
	}

	/**
	 * Look up pictures for targets which don't have one yet. Results are returned even if this fails.
	 */
	private void addThumbnails(List<Target> targets, Consumer<List<Target>> onResult) {
		var missing = new ArrayList<String>();
		for (var target : targets)
			if (target.thumbnailUrl == null)
				missing.add(target.name);
		if (missing.isEmpty()) {
			onResult.accept(targets);
			return;
		}

		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", String.valueOf(THUMBNAIL_SIZE))
			.addQueryParameter("titles", String.join("|", missing))
			.build();

		getJson(url, json -> {
			var thumbnails = new HashMap<String, String>();
			for (JsonObject page : pages(json)) {
				String thumbnail = thumbnail(page);
				if (thumbnail != null)
					thumbnails.put(page.get("title").getAsString(), thumbnail);
			}
			for (var target : targets)
				if (target.thumbnailUrl == null)
					target.thumbnailUrl = thumbnails.get(target.name);
			onResult.accept(targets);
		}, error -> onResult.accept(targets));
	}

	/**
	 * Load the recommended equipment for a target. Tries the page it was found on, then its strategy pages,
	 * and finally its main page.
	 */
	public void fetchStrategies(Target target, Consumer<StrategyPage> onResult, Consumer<String> onError) {
		var candidates = new LinkedHashSet<String>();
		if (target.page != null)
			candidates.add(target.page);
		candidates.add(target.name + "/Strategies");
		candidates.add(target.name + "/Strategy");
		candidates.add(target.name);
		fetchFirst(new ArrayList<>(candidates), 0, target.name, onResult, onError);
	}

	private void fetchFirst(List<String> titles, int index, String name, Consumer<StrategyPage> onResult, Consumer<String> onError) {
		if (index >= titles.size()) {
			onResult.accept(new StrategyPage(name, new ArrayList<>()));
			return;
		}
		fetchSetups(titles.get(index), page -> {
			if (page != null && !page.setups.isEmpty()) {
				onResult.accept(page);
			} else {
				fetchFirst(titles, index + 1, name, onResult, onError);
			}
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
			onResult.accept(new StrategyPage(pageTitle, RecommendedEquipmentParser.parse(wikitext),
				WildernessDetector.isWilderness(pageTitle, wikitext)));
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

	private static List<JsonObject> pages(JsonObject json) {
		var pages = new ArrayList<JsonObject>();
		JsonObject query = json.getAsJsonObject("query");
		if (query != null && query.has("pages"))
			for (JsonElement element : query.getAsJsonArray("pages"))
				pages.add(element.getAsJsonObject());
		return pages;
	}

	@Nullable
	private static String thumbnail(JsonObject page) {
		if (!page.has("thumbnail"))
			return null;
		return absoluteUrl(page.getAsJsonObject("thumbnail").get("source").getAsString());
	}

	@RequiredArgsConstructor
	static class SearchHit
	{
		final int index;
		final String title;
		@Nullable
		final String thumbnailUrl;
	}
}
