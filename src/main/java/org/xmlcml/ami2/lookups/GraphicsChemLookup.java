package org.xmlcml.ami2.lookups;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.cmine.lookup.AbstractLookup;
import org.xmlcml.euclid.IntArray;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class GraphicsChemLookup extends AbstractLookup {

	private static final String ITEMS = "items";
	private static final String WIKIDATA_GETIDS = "http://wdq.wmflabs.org/api?q=string[";
	private static final String ESC_QUOTE = "%22";
	private static final String ESC_SPACE = "%20";
	private static final String FORMAT_XML = "&format=xml";
	private static final String WIKIDATA_GET_ENTITIES = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=";
	private static final String WIKIDATA_SPECIES = "225";
	
	private static final Logger LOG = Logger.getLogger(GraphicsChemLookup.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	public GraphicsChemLookup() {
	}

	public String lookup(String molecule) throws IOException {
		if (molecule != null) {
//			IntArray wikidataIntArray = getWikidataIDsAsIntArray(molecule);
//			String result = wikidataIntArray.toString();
//			// remove all brackets
//			result = result == null ? null : result.replaceAll("[\\(\\)]", "");
//			return (result == null || result.trim().equals("")) ? null : result; 
			return null;
		} else {
			LOG.error("null species");
			return null;
		}
	}
	
//	/** creates URL to retrieve data for an id.
//	 * 
//	 * @param wikidataId
//	 * @return
//	 * @throws MalformedURLException
//	 */
//	public static URL createWikidataXMLURL(String wikidataId) throws MalformedURLException {
//		String urlString = WIKIDATA_GET_ENTITIES;
//		urlString += wikidataId;
//		urlString += FORMAT_XML;
//		return new URL(urlString);
//	}
//    
//	private JsonElement getWikidataSpeciesJSONElement(String speciesName) throws IOException {
//		URL url = createWikidataSpeciesLookupURL(speciesName);
//		String json = this.getString(url);
//	    JsonParser parser = new JsonParser();
//	    return parser.parse(json);
//	}
//
//	/** create URL to lookup a species.
//	 * 
//	 * @param name
//	 * @return
//	 * @throws MalformedURLException
//	 */
//	private static URL createWikidataSpeciesLookupURL(String name) {
//		URL url =  createWikidataLookupURL(WIKIDATA_SPECIES, name);
//		return url;
//	}
//    
//	/** creates a search URL from a Wikipedia property and a name.
//	 * 
//	 * @param property (e.g. 225 for species)
//	 * @param name
//	 * @return
//	 * @throws MalformedURLException
//	 */
//	public static URL createWikidataLookupURL(String property, String name) {
//		name = name.replaceAll(" ", ESC_SPACE);
//		String urlString = WIKIDATA_GETIDS+property+":"+ESC_QUOTE+name+ESC_QUOTE+"]";
//		URL url = null;
//		try {
//			url = new URL(urlString);
//		} catch (MalformedURLException e) {
//			throw new RuntimeException("Bad url for wikidata: +url", e);
//		}
//		return url;
//	}
	
}
