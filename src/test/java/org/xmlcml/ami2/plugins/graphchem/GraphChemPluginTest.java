package org.xmlcml.ami2.plugins.graphchem;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.ami2.Fixtures;
import org.xmlcml.ami2.plugins.graphicschem.GraphChemPlugin;
import org.xmlcml.cmine.args.DefaultArgProcessor;
import org.xmlcml.cmine.files.CMDir;

public class GraphChemPluginTest {

	
	private static final Logger LOG = Logger.getLogger(GraphChemPluginTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	@Test
	public void testSimpleGraphChem() throws IOException {
		CMDir cmDir = new CMDir(Fixtures.TEST_GRAPHCHEM_ASPERGILLUS);
		File normaTemp = new File("target/chem/aspergillus/");
		cmDir.copyTo(normaTemp, true);
		String[] args = {
				"-q", normaTemp.toString(),
				"-i", "svg/image.g.2.10.svg",
				"-o", "results/graphchem/results.xml",
				"--gc.graphchem", "water"
		};
		GraphChemPlugin chemPlugin = new GraphChemPlugin(args);
		DefaultArgProcessor argProcessor = (DefaultArgProcessor) chemPlugin.getArgProcessor();
		Assert.assertNotNull(argProcessor);
		LOG.debug(argProcessor.getInputList());
		argProcessor.runAndOutput();
		CMDir cmDirTemp = new CMDir(normaTemp);
//		Assert.assertTrue("has results.xml", cmDirTemp.hasResultsDir());
	}
	
	
	
}
