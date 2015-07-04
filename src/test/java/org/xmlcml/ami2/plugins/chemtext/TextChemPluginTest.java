package org.xmlcml.ami2.plugins.chemtext;

import java.io.File;
import java.io.IOException;

import nu.xom.Document;
import nu.xom.Nodes;

import org.antlr.v4.runtime.tree.Tree;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.xmlcml.ami2.chemtext.Fixtures;
import org.xmlcml.cmine.files.CMDir;

import uk.ac.cam.ch.wwmm.chemicaltagger.ChemistryPOSTagger;
import uk.ac.cam.ch.wwmm.chemicaltagger.ChemistrySentenceParser;
import uk.ac.cam.ch.wwmm.chemicaltagger.POSContainer;
import uk.ac.cam.ch.wwmm.chemicaltagger.Utils;

/** NYI
 * 
 * @author pm286
 *
 */
public class TextChemPluginTest {

	
	private static final Logger LOG = Logger.getLogger(TextChemPluginTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	@Test
	public void testSimpleTextChem() throws IOException {
		File file = new File(Fixtures.TEST_CHEMTEXT_DIR, "fullParseTest/paragraph1.txt");
		Assert.assertTrue(file.exists());
		LOG.debug("file: "+FileUtils.readFileToString(file));
		String text = Utils.readSentence(file.toString());
		LOG.debug("TEXT "+text);
		String expectedTags = Utils.readSentence("uk/ac/cam/ch/wwmm/chemicaltagger/fullParseTest/ref1.txt");
		ChemistryPOSTagger posTagger = ChemistryPOSTagger.getDefaultInstance();
		POSContainer posContainer = posTagger.runTaggers(text, true);
		Assert.assertEquals("Spectra List size", 0, posContainer.getSpectrumElementList().getChildCount());
		Assert.assertEquals("Tagging Output",expectedTags, posContainer.getTokenTagTupleAsString());
		ChemistrySentenceParser chemistrySentenceParser = new ChemistrySentenceParser(posContainer);
		chemistrySentenceParser.parseTags();
		Tree t = chemistrySentenceParser.getParseTree();
		Document doc = chemistrySentenceParser.makeXMLDocument();
		LOG.debug("DOC "+doc.toXML());
		Nodes sentenceNodes = doc.query("//Sentence");
		Nodes actionNodes = doc.query("//ActionPhrase");
		Nodes moleculeNodes = doc.query("//MOLECULE");
		Assert.assertEquals("Sentence node size",4, sentenceNodes.size());
		Assert.assertEquals("Action node size",10, actionNodes.size());
		Assert.assertEquals("Molecule node size",7, moleculeNodes.size());
		Assert.assertEquals("Input string is equal to output content", text.replace(" ", "").toLowerCase(), doc.getValue().toLowerCase());
		CMDir cmDir = new CMDir(Fixtures.TEST_CHEMTEXT_ASPERGILLUS);
		File normaTemp = new File("target/chem/aspergillus/");
	}
	
	
	
}
