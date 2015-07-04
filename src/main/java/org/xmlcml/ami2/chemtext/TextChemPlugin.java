package org.xmlcml.ami2.chemtext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.ami2.plugins.AMIPlugin;

/** test plugin.
 * 
 * Very simple tasks for testing and tutorials.
 * 
 * @author pm286
 *
 */
public class TextChemPlugin extends AMIPlugin {

	private static final Logger LOG = Logger.getLogger(TextChemPlugin.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

//	private SimpleArgProcessor argProcessor;
	
	public TextChemPlugin(String[] args) {
		super();
		this.argProcessor = new TextChemArgProcessor(args);
	}
	
	public static void main(String[] args) {
		TextChemArgProcessor argProcessor = new TextChemArgProcessor(args);
		argProcessor.runAndOutput();
	}

	public TextChemPlugin(String args) {
		super();
		this.argProcessor = new TextChemArgProcessor(args);
	}

	

}
