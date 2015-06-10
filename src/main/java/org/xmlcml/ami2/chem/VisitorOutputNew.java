package org.xmlcml.ami2.chem;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class VisitorOutputNew {

	
	private static final Logger LOG = Logger.getLogger(VisitorOutputNew.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	private File outputDirectoryFile;
	
	public File getOutputDirectoryFile() {
		return outputDirectoryFile;
	}
	
}
