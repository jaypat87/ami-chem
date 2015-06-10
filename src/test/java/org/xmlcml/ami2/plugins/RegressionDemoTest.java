package org.xmlcml.ami2.plugins;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.ami2.Fixtures;
import org.xmlcml.ami2.plugins.identifier.IdentifierPlugin;

/** collection of archetypal tests from each plugin.
 * 
 * These should always run and will use communal resources
 * 
 * @author pm286
 *
 */
public class RegressionDemoTest {

	
	private static final Logger LOG = Logger
			.getLogger(RegressionDemoTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	@Test
	@Ignore
	public void testIdentifiersArgProcessor() throws Exception {
		// SHOWCASE
		String cmd = "-q target/examples_16_1_1/ -i scholarly.html --context 25 40 "
				+ "--id.identifier --id.regex regex/identifiers.xml --id.type clin.nct clin.isrctn";
		Fixtures.runStandardTestHarness(
				new File("src/test/resources/org/xmlcml/ami2/regressiondemos/http_www.trialsjournal.com_content_16_1_1/"),
				new File("target/examples_16_1_1/"), 
				new IdentifierPlugin(),
				cmd,
				"identifier/clin.nct/", "identifier/clin.isrctn/");
	}


//	@Test
//	public void testWordHarness() throws IOException {
//		// SHOWCASE
//		String cmd = "-q target/word/16_1_1_test/ -i scholarly.html --context 25 40 --w.words wordLengths wordFrequencies --w.stopwords /org/xmlcml/ami2/plugins/word/stopwords.txt";
//		Fixtures.runStandardTestHarness(
//				new File("src/test/resources/org/xmlcml/ami2/regressiondemos/http_www.trialsjournal.com_content_16_1_1"), 
//				new File("target/word/16_1_1_test/"), 
//				new WordPlugin(),
//				cmd,
//				"word/lengths/", "word/frequencies/");
//		
//
//	}


}
