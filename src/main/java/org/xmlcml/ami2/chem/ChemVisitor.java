package org.xmlcml.ami2.chem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xmlcml.ami2.chem.html.HtmlContainerNew;
import org.xmlcml.ami2.chem.html.HtmlVisitableNew;
import org.xmlcml.ami2.chem.svg.SVGContainerNew;
import org.xmlcml.ami2.chem.svg.SVGVisitableNew;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.CMLReaction;
import org.xmlcml.html.HtmlSub;

import com.google.common.util.concurrent.UncheckedTimeoutException;

public class ChemVisitor /*extends AbstractVisitor*/ {

	private static final String CHEM_SUB_DIRECTORY = "/"; // no subdirectory at present

	private final static Logger LOG = Logger.getLogger(ChemVisitor.class);
	
	private static final int TIMEOUT = 350000000;
	
	private List<CMLReaction> reactions = new ArrayList<CMLReaction>();
	private List<CMLMolecule> molecules = new ArrayList<CMLMolecule>();
	
	File outputDirectory;

	private VisitorOutputNew visitorOutput;
	
	public List<CMLMolecule> getMolecules() {
		if (molecules.size() > 0) {
			return molecules;
		}
		List<CMLMolecule> molecules = new ArrayList<CMLMolecule>();
		for (CMLReaction r : reactions) {
			if (r.getReactantList() != null) {
				molecules.addAll(r.getReactantList().getMolecules());
			}
			if (r.getProductList() != null) {
				molecules.addAll(r.getProductList().getMolecules());
			}
		}
		return molecules;
	}
	
	public List<CMLReaction> getReactions() {
		return reactions;
	}
	
	// ===================Called on Visitables===================

	public void visit(HtmlVisitableNew textVisitable) {
		// switch for different analyzers (OSCAR, OPSIN, ChemicalTagger
		LOG.trace("ChemVisitor: now visiting an HtmlVisitable");
		//TODO this should really be part of AbstractVisitor
		outputDirectory = new File(getOrCreateVisitorOutput().getOutputDirectoryFile(), CHEM_SUB_DIRECTORY);
		outputDirectory.mkdir();
		for (HtmlContainerNew htmlContainer : textVisitable.getHtmlContainerList()) {
			searchContainer(htmlContainer);
		}
	}
	
	private VisitorOutputNew getOrCreateVisitorOutput() {
		return visitorOutput;
	}

	protected void searchContainer(HtmlContainerNew htmlContainer) {
		searchForSubscripts(htmlContainer);
	}

	public void visit(SVGVisitableNew svgVisitable) {
		LOG.trace("ChemVisitor: now visiting an SVGVisitable");
		outputDirectory = new File(getOrCreateVisitorOutput().getOutputDirectoryFile(), CHEM_SUB_DIRECTORY);
		outputDirectory.mkdir();
		createCML(svgVisitable);
	}

	// =======================Called by Visitables===============

	private void createCML(SVGVisitableNew svgVisitable) {
		List<SVGContainerNew> svgContainerList = svgVisitable.getSVGContainerList();
		for (int i = 0; i < svgContainerList.size(); i++) {
//			LOG.trace("SVGContainer name: " + svgContainerList.get(i).getName());
			try {
				createAndSaveCML(svgContainerList.get(i));
			} catch (UncheckedTimeoutException e) {
				LOG.warn(e.getMessage());
			}
		}
	}

	private void createAndSaveCML(SVGContainerNew svgContainer) {
//		LOG.info("Working with svgContainer: "+ svgContainer.getName());
		
		MoleculeCreator cmlCreator = new MoleculeCreator(svgContainer, TIMEOUT);
		cmlCreator.getReactionsAndMolecules();
		try {
			cmlCreator.createAnnotatedVersionOfOutput(outputDirectory);
		} catch (Throwable t) {
			
		}
		try {
			cmlCreator.createAnnotatedVersionOfInput(outputDirectory);
		} catch (Throwable t) {
			
		}
	}
	
	/** 
	 * This is just a test at present, especially as spaces are not correct yet.
	 * <p>
	 * HCO <sub>2</sub> H
	 */
	private void searchForSubscripts(HtmlContainerNew htmlContainer) {
		List<HtmlSub> subList = HtmlSub.extractSelfAndDescendantLines(htmlContainer.getHtmlElement());
		LOG.debug("subscripts: "+subList.size());
	}
	
	public String getDescription() {
		return "Extracts chemical reactions.";
	}

	// =======================Called on Visitables===============
	
	public static void main(String[] args) throws Exception {
		new ChemVisitor().processArgs(args);
	}

	private void processArgs(String[] args) {
		throw new RuntimeException("NYI");
	}
	
//	protected void usage() {
//		System.err.println("Chem: ");
//		super.usage();
//	}

}