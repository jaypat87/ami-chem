package org.xmlcml.ami2.chem.html;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class HtmlVisitableNew {

	
	private static final Logger LOG = Logger.getLogger(HtmlVisitableNew.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	private List<HtmlContainerNew> htmlContainerList;

	public List<HtmlContainerNew> getHtmlContainerList() {
		return htmlContainerList;
	}

}
