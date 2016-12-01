package org.xmlcml.ami2.chem.svg;

import java.io.File;

import nu.xom.Element;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;


public class SVGContainerNew {

	private static final Logger LOG = Logger.getLogger(SVGContainerNew.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}


	private File file;
	private SVGElement svgElement;


	public SVGContainerNew(File file, SVGElement svgElement) {
		this.file = file;
		this.svgElement = svgElement;
	}

	public File getFile() {
		return file;
	}

	public SVGElement getElement() {
		return svgElement;
	}

}