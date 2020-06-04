package net.osmand.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

@RunWith(Parameterized.class)
public class SearchUICoreTest {

	private static final String SEARCH_RESOURCES_PATH = "src/test/resources/search/";
	private static boolean TEST_EXTRA_RESULTS = true;
	
	private File testFile;

    public SearchUICoreTest(String name, File file) {
        this.testFile = file;
    }
	

	@BeforeClass
	public static void setUp() {
		defaultSetup();
	}


	static void defaultSetup() {
		MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
		MapPoiTypes poiTypes = MapPoiTypes.getDefault();
		Map<String, String> enPhrases = new HashMap<>();
		Map<String, String> phrases = new HashMap<>();
		try {
			enPhrases = Algorithms.parseStringsXml(new File("src/test/resources/phrases/en/phrases.xml"));
			//phrases = Algorithms.parseStringsXml(new File("src/test/resources/phrases/ru/phrases.xml"));
			phrases = enPhrases;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		poiTypes.setPoiTranslator(new TestSearchTranslator(phrases, enPhrases));
	}
	
	
	@Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
    	final File[] files = new File(SEARCH_RESOURCES_PATH).listFiles();
    	ArrayList<Object[]> arrayList = new ArrayList<>();
    	if (files != null) {
			for (File file : files) {
				String fileName = file.getName();
				if(fileName.endsWith(".json")) {
					String name = fileName.substring(0, fileName.length() - ".json".length());
					arrayList.add(new Object[] {name, file});
				}
			}
		}
    	return arrayList;
    }

    @Test
	public void testSearch() throws IOException, JSONException {
		File jsonFile = testFile;
		String sourceJsonText = Algorithms.getFileAsString(jsonFile);
		Assert.assertNotNull(sourceJsonText);
		Assert.assertTrue(sourceJsonText.length() > 0);

		BinaryMapIndexReaderTest reader = new BinaryMapIndexReaderTest();
		JSONObject sourceJson = new JSONObject(sourceJsonText);
		JSONArray phrasesJson = sourceJson.optJSONArray("phrases");
		String singlePhrase = sourceJson.optString("phrase", null);
		List<String> phrases = new ArrayList<>();
		if (singlePhrase != null) {
			phrases.add(singlePhrase);
		}
		if (phrasesJson != null) {
			for (int i = 0; i < phrasesJson.length(); i++) {
				String phrase = phrasesJson.optString(i);
				if (phrase != null) {
					phrases.add(phrase);
				}
			}
		}
		JSONObject settingsJson = sourceJson.getJSONObject("settings");
		if (sourceJson.has("amenities")) {
			JSONArray amenitiesArr = sourceJson.getJSONArray("amenities");
			List<Amenity> amenities = new ArrayList<>();
			for (int i = 0; i < amenitiesArr.length(); i++) {
				JSONObject amenityObj = amenitiesArr.getJSONObject(i);
				amenities.add(Amenity.parseJSON(amenityObj));
			}
			reader.amenities = amenities;
		}
		if (sourceJson.has("cities")) {
			JSONArray citiesArr = sourceJson.getJSONArray("cities");
			List<City> cities = new ArrayList<>();
			List<City> initCities = new ArrayList<>();
			List<City> matchedCities = new ArrayList<>();
			List<City> streetCities = new ArrayList<>();
			for (int i = 0; i < citiesArr.length(); i++) {
				JSONObject cityObj = citiesArr.getJSONObject(i);
				final City city = City.parseJSON(cityObj);
				cities.add(city);
				if (cityObj.has("init")) {
					initCities.add(city);
				}
				if (cityObj.has("matchCity")) {
					matchedCities.add(city);
				}
				if (cityObj.has("matchStreet")) {
					streetCities.add(city);
				}
			}
			reader.cities = cities;
			reader.initCities = initCities;
			reader.matchedCities = matchedCities;
			reader.streetCities = streetCities;
		}
		List<List<String>> results = new ArrayList<>();
		for (int i = 0; i < phrases.size(); i++) {
			results.add(new ArrayList<String>());
		}
		if (sourceJson.has("results")) {
			parseResults(sourceJson, "results", results);
		}
		if (TEST_EXTRA_RESULTS && sourceJson.has("extra-results")) {
			parseResults(sourceJson, "extra-results", results);
		}

		Assert.assertEquals(phrases.size(), results.size());
		if (phrases.size() != results.size()) {
			return;
		}

		SearchSettings s = SearchSettings.parseJSON(settingsJson);
		s.setOfflineIndexes(Collections.singletonList(reader));

		final SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
		core.init();

		ResultMatcher<SearchResult> rm = new ResultMatcher<SearchResult>() {
			@Override
			public boolean publish(SearchResult object) {
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};

		boolean simpleTest = true;
		SearchPhrase emptyPhrase = SearchPhrase.emptyPhrase(s);
		for (int k = 0; k < phrases.size(); k++) {
			String text = phrases.get(k);
			List<String> result = results.get(k);
			SearchPhrase phrase = emptyPhrase.generateNewPhrase(text, s);
			SearchResultMatcher matcher = new SearchResultMatcher(rm, phrase, 1, new AtomicInteger(1), -1);
			core.searchInternal(phrase, matcher);

			SearchResultCollection collection = new SearchResultCollection(phrase);
			collection.addSearchResults(matcher.getRequestResults(), true, true);
			List<SearchResult> searchResults = collection.getCurrentSearchResults();
			int i = 0;
			for (SearchResult res : searchResults) {
				String expected = result.get(i++);
				if (simpleTest && expected.indexOf('[') != -1) {
					expected = expected.substring(0, expected.indexOf('[')).trim();
				}
//				String present = result.toString();
				String present = formatResult(simpleTest, res, phrase);
				if (!Algorithms.stringsEqual(expected, present)) {
					System.out.println(String.format("Phrase: %s", phrase));
					System.out.println(String.format("Mismatch for '%s' != '%s'. Result: ", expected, present));
					for (SearchResult r : searchResults) {
						System.out.println(String.format("\t\"%s\",", formatResult(false, r, phrase)));
					}
				}
				Assert.assertEquals(expected, present);
				if (i >= result.size()) {
					break;
				}
			}
		}
	}

	private void parseResults(JSONObject sourceJson, String tag, List<List<String>> results) {
		List<String> result = results.get(0);
		JSONArray resultsArr = sourceJson.getJSONArray(tag);
		boolean hasInnerArray = resultsArr.length() > 0 && resultsArr.optJSONArray(0) != null;
		for (int i = 0; i < resultsArr.length(); i++) {
			if (hasInnerArray) {
				JSONArray innerArray = resultsArr.optJSONArray(i);
				if (innerArray != null && results.size() > i) {
					result = results.get(i);
					for (int k = 0; k < innerArray.length(); k++) {
						result.add(innerArray.getString(k));
					}
				}
			} else {
				result.add(resultsArr.getString(i));
			}
		}
	}

	private String formatResult(boolean simpleTest, SearchResult r, SearchPhrase phrase) {
		if (simpleTest) {
			return r.toString().trim();
		}
		double dist = 0;
		if(r.location != null) {
			dist = MapUtils.getDistance(r.location, phrase.getLastTokenLocation());
		}
		return String.format("%s [[%d, %s, %.3f, %.2f km]]", r.toString(), 
				r.getFoundWordCount(), r.objectType.toString(),
				r.getUnknownPhraseMatchWeight(),
				dist / 1000
				);
	}

	static class TestSearchTranslator implements MapPoiTypes.PoiTranslator {

		private Map<String, String> enPhrases;
		private Map<String, String> phrases;
		public TestSearchTranslator(Map<String, String> phrases, Map<String, String> enPhrases) {
			this.phrases = phrases;
			this.enPhrases = enPhrases;
		}

		@Override
		public String getTranslation(AbstractPoiType type) {
			AbstractPoiType baseLangType = type.getBaseLangType();
			if (baseLangType != null) {
				return getTranslation(baseLangType) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getTranslation(type.getIconKeyName());
		}

		@Override
		public String getTranslation(String keyName) {
			String val = phrases.get("poi_" + keyName);
			if (val != null) {
				int ind = val.indexOf(';');
				if (ind > 0) {
					return val.substring(0, ind);
				}
			}
			return val;
		}

		@Override
		public String getSynonyms(AbstractPoiType type) {
			AbstractPoiType baseLangType = type.getBaseLangType();
			if (baseLangType != null) {
				return getSynonyms(baseLangType);
			}
			return getSynonyms(type.getIconKeyName());
		}


		@Override
		public String getSynonyms(String keyName) {
			String val = phrases.get("poi_" + keyName);
			if (val != null) {
				int ind = val.indexOf(';');
				if (ind > 0) {
					return val.substring(ind + 1);
				}
				return "";
			}
			return null;
		}

		@Override
		public String getEnTranslation(AbstractPoiType type) {
			AbstractPoiType baseLangType = type.getBaseLangType();
			if (baseLangType != null) {
				return getEnTranslation(baseLangType) + " (" + type.getLang().toLowerCase() + ")";
			}
			return getEnTranslation(type.getIconKeyName());
		}

		@Override
		public String getEnTranslation(String keyName) {
			if (enPhrases.isEmpty()) {
				return Algorithms.capitalizeFirstLetter(keyName.replace('_', ' '));
			}
			String val = enPhrases.get("poi_" + keyName);
			if (val != null) {
				int ind = val.indexOf(';');
				if (ind > 0) {
					return val.substring(0, ind);
				}
			}
			return val;
		}
	};
	
	private static class BinaryMapIndexReaderTest extends BinaryMapIndexReader {

		List<Amenity> amenities = Collections.emptyList();
		List<City> cities = Collections.emptyList();
		List<City> initCities = Collections.emptyList();
		List<City> matchedCities = Collections.emptyList();
		List<City> streetCities = Collections.emptyList();

		BinaryMapIndexReaderTest() throws IOException {
			super(null, null, false);
		}

		@Override
		public List<Amenity> searchPoiByName(SearchRequest<Amenity> req) throws IOException {
			for (Amenity amenity : amenities) {
				req.publish(amenity);
			}
			return req.getSearchResults();
		}

		@Override
		public List<Amenity> searchPoi(SearchRequest<Amenity> req) throws IOException {
			for (Amenity amenity : amenities) {
				req.publish(amenity);
			}
			return req.getSearchResults();
		}

		@Override
		public List<City> getCities(SearchRequest<City> resultMatcher, int cityType) throws IOException {
			for (City city : initCities) {
				if (resultMatcher != null) {
					resultMatcher.publish(city);
				}
			}
			return initCities;
		}

		@Override
		public int preloadStreets(City c, SearchRequest<Street> resultMatcher) throws IOException {
			return 0;
		}

		@Override
		public void preloadBuildings(Street s, SearchRequest<Building> resultMatcher) throws IOException {
			// cities must be filled with streets and buildings
		}

		@Override
		public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req) throws IOException {
			for (City city : streetCities) {
				for (Street street : city.getStreets()) {
					req.publish(street);
				}
			}
			for (City city : matchedCities) {
				req.publish(city);
			}
			return req.getSearchResults();
		}

		@Override
		public String getRegionName() {
			return "Test region";
		}

		@Override
		public boolean containsPoiData(int left31x, int top31y, int right31x, int bottom31y) {
			return true;
		}

		@Override
		public boolean containsMapData() {
			return true;
		}

		@Override
		public boolean containsPoiData() {
			return true;
		}

		@Override
		public boolean containsRouteData() {
			return true;
		}

		@Override
		public boolean containsRouteData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
			return true;
		}

		@Override
		public boolean containsAddressData(int left31x, int top31y, int right31x, int bottom31y) {
			return true;
		}

		@Override
		public boolean containsMapData(int tile31x, int tile31y, int zoom) {
			return true;
		}

		@Override
		public boolean containsMapData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
			return true;
		}

		@Override
		public boolean containsAddressData() {
			return true;
		}
	}
}
