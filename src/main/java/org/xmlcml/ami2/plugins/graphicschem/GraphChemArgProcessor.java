package org.xmlcml.ami2.plugins.graphicschem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.ami2.chem.ChemistryBuilder;
import org.xmlcml.ami2.chem.MoleculeCreator;
import org.xmlcml.ami2.chem.svg.SVGContainerNew;
import org.xmlcml.ami2.plugins.AMIArgProcessor;
import org.xmlcml.cmine.args.ArgIterator;
import org.xmlcml.cmine.args.ArgumentOption;
import org.xmlcml.cmine.files.CTree;
import org.xmlcml.cmine.files.CTreeFiles;
import org.xmlcml.cmine.files.ResultElement;
import org.xmlcml.cmine.files.ResultsElement;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGUtil;

/** 
 * Processes command-line arguments.
 * 
 * @author pm286
 */
public class GraphChemArgProcessor extends AMIArgProcessor {
	
	public static final Logger LOG = Logger.getLogger(GraphChemArgProcessor.class);
	private List<String> params;
	private SVGElement inputSvg;
	private ArrayList<CMLMolecule> molecules = new ArrayList<CMLMolecule>();
	
	static {
		LOG.setLevel(Level.DEBUG);
	}

	//Shouldn't be required; fails to be inherited on Jenkins
	private static String WHITESPACE = "\\s+";
	
	public GraphChemArgProcessor() {
		super();
	}

	public GraphChemArgProcessor(String[] args) {
		this();
		parseArgs(args);
	}

	public GraphChemArgProcessor(String argString) {
		this(argString.split(WHITESPACE));
	}

	//=============== METHODS ==============

	public void parseChem(ArgumentOption option, ArgIterator argIterator) {
		params = argIterator.createTokenListUpToNextNonDigitMinus(option);
		LOG.debug("After parsing, arguments: " + params);
	}
	
	public void runChem(ArgumentOption option) {
		ensureSectionElements();
		CTreeFiles files = getCTree().extractCTreeFiles("**.svg");
		for (File file : files) {
			LOG.trace("SVG file: " + file);
			inputSvg = null;
			try {
				inputSvg = SVGUtil.parseToSVGElement(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Cannot read SVG file: " + file, e);
			}
			ChemistryBuilder geometryBuilder = new ChemistryBuilder(inputSvg);
			MoleculeCreator moleculeCreator = new MoleculeCreator(geometryBuilder);
			ResultsElement resultsElement = new ResultsElement();
			for (CMLMolecule molecule : moleculeCreator.getMolecules()) {
				ResultElement resultElement = new ResultElement();
				resultElement.appendChild(molecule);
				resultsElement.appendChild(resultElement);
				resultsElement.setTitle(file.getName());
			}
			getCurrentCTree().getOrCreateContentProcessor().addResultsElement(resultsElement);
		}
	}

	public void outputChem(ArgumentOption option) {
		getCurrentCTree().getOrCreateContentProcessor().createResultsDirectoriesAndOutputResultsElement("graphicalchemistry");
	}
	
	//=============================

	@Override
	/** 
	 * Parse arguments and resolve their dependencies.
	 * 
	 * (Don't run any argument actions.)
	 */
	public void parseArgs(String[] args) {
		super.parseArgs(args);
	}
	
}