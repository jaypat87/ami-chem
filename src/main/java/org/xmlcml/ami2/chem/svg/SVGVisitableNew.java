package org.xmlcml.ami2.chem.svg;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SVGVisitableNew {

	
	private static final Logger LOG = Logger.getLogger(SVGVisitableNew.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	private List<SVGContainerNew> svgContainerList;
	
	public List<SVGContainerNew> getSVGContainerList() {
		return svgContainerList;
	}

}
