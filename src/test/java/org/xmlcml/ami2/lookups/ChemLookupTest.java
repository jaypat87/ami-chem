package org.xmlcml.ami2.lookups;

import java.net.URL;

import nu.xom.Element;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.euclid.IntArray;
import org.xmlcml.xml.XMLUtil;

public class ChemLookupTest {

	
	private static final Logger LOG = Logger.getLogger(ChemLookupTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
//	@Test
//	@Ignore // LOOKUP 
//	public void getWikidataIdForSpecies() throws Exception {
//		GraphicsChemLookup wikipediaLookup = new GraphicsChemLookup();
//		IntArray intArray = wikipediaLookup.getWikidataIDsAsIntArray("Mus musculus");
//		Assert.assertEquals("mouse", "(83310)", intArray.toString());
//	}
//	
//	@Test
//	@Ignore // LOOKUP 
//	public void getWikidataXMLForID() throws Exception {
//		GraphicsChemLookup defaultLookup = new GraphicsChemLookup();
//		URL url = defaultLookup.createWikidataXMLURL("Q83310");
//		Element element = defaultLookup.getXML(url);
//		XMLUtil.debug(element, "Mus");
//		Assert.assertEquals("Q83310", 
//				"<api success=\"1\"><entities><entity pageid=\"85709\" ns=\"0\" title=\"Q83310\" lastrevid=\"194466801\" modifi",
//				element.toXML().substring(0, 100));
//	}
	
	
}
