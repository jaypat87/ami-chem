package org.xmlcml.ami2.plugins.chemtext;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nu.xom.Document;
import nu.xom.Nodes;

import org.antlr.v4.runtime.tree.Tree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
		String sentence = FileUtils.readFileToString(new File(Fixtures.TEST_FULLPARSE_DIR, "paragraph1.txt"), "UTF-8");
		LOG.debug("TEXT "+sentence);
		String expectedTags = ""
				+ "DT A NN-CHEMENTITY solution IN-OF of CD-ALPHANUM 124C -LRB- ( CD 7.0 NN-MASS g COMMA ,"
				+ " CD 32.4 NN-AMOUNT mmol -RRB- ) IN-IN in JJ-CHEM concentrate OSCAR-CM H2SO4 -LRB- ("
				+ " CD 9.5 NN-VOL mL -RRB- ) VBD was VB-ADD added TO to DT a NN-CHEMENTITY solution IN-OF"
				+ " of JJ-CHEM concentrate OSCAR-CM H2SO4 -LRB- ( CD 9.5 NN-VOL mL -RRB- ) CC and JJ fuming"
				+ " OSCAR-CM HNO3 -LRB- ( CD 13 NN-VOL mL -RRB- ) CC and DT-THE the NN-CHEMENTITY mixture"
				+ " VBD was VB-HEAT heated IN at CD 60 NN-TEMP °C IN-FOR for CD 30 NN-TIME min STOP ."
				+ " IN-AFTER After VB-COOL cooling TO to NN-TEMP room NN-TEMP temperature COMMA , DT-THE"
				+ " the JJ-CHEM reaction NN-CHEMENTITY mixture VBD was VB-ADD added TO to JJ iced CD 6"
				+ " NN-MOLAR M NN-CHEMENTITY solution IN-OF of OSCAR-CM NaOH -LRB- ( CD 150 NN-VOL mL -RRB- )"
				+ " CC and VBN neutralized TO to NN-PH pH CD 6 IN-WITH with CD 1 NN-MOLAR N OSCAR-CM NaOH"
				+ " NN-CHEMENTITY solution STOP . DT-THE The JJ-CHEM reaction NN-CHEMENTITY mixture VBD was"
				+ " VB-EXTRACT extracted IN-WITH with OSCAR-CM dichloromethane -LRB- ( CD 4x100 NN-VOL mL -RRB-"
				+ " ) STOP . DT-THE The JJ-CHEM combined JJ-CHEM organic NN-CHEMENTITY phases VBD were VB-DRY"
				+ " dried IN-OVER over OSCAR-CM Na2SO4 COMMA , VB-FILTER filtered CC and VB-CONCENTRATE"
				+ " concentrated TO to VB-YIELD give CD-ALPHANUM 124D IN-AS as DT a NN-STATE solid STOP .";
		ChemistryPOSTagger posTagger = ChemistryPOSTagger.getDefaultInstance();
		POSContainer posContainer = posTagger.runTaggers(sentence, true);
		Assert.assertEquals("Spectra List size", 0, posContainer.getSpectrumElementList().getChildCount());
		Assert.assertEquals("Tagging Output",expectedTags, posContainer.getTokenTagTupleAsString());
		ChemistrySentenceParser chemistrySentenceParser = new ChemistrySentenceParser(posContainer);
		chemistrySentenceParser.parseTags();
		Tree t = chemistrySentenceParser.getParseTree();
		Document doc = chemistrySentenceParser.makeXMLDocument();
//		LOG.debug("DOC "+doc.toXML());
		Nodes sentenceNodes = doc.query("//Sentence");
		Nodes actionNodes = doc.query("//ActionPhrase");
		Nodes moleculeNodes = doc.query("//MOLECULE");
		Assert.assertEquals("Sentence node size",4, sentenceNodes.size());
		Assert.assertEquals("Action node size",10, actionNodes.size());
		Assert.assertEquals("Molecule node size",7, moleculeNodes.size());
//		Assert.assertEquals("Input string is equal to output content", text.replace(" ", "").toLowerCase(), doc.getValue().toLowerCase());
		CMDir cmDir = new CMDir(Fixtures.TEST_CHEMTEXT_ASPERGILLUS);
		File normaTemp = new File("target/chem/aspergillus/");
	}
	
	
	
}
