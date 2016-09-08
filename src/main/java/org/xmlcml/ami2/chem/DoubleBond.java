package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 
 * Two parallel lines with overlap.
 * <p>
 * Originally extended SVGG so it could be used in place of SVGLines when needed but no longer does
 * 
 * @author pm286
 */
public class DoubleBond extends UnsaturatedBond {

	@SuppressWarnings("unused")
	private final static Logger LOG = Logger.getLogger(DoubleBond.class);

	private static final double DOUBLE_BOND_PRORITY = 4.0;

	private List<SVGLine> lineList;
	
	public DoubleBond(ChemistryBuilderParameters parameters, SVGLine linei, SVGLine linej) {
		add(linei);
		add(linej);
		addJoinPoints(parameters);
	}

	private void addJoinPoints(ChemistryBuilderParameters parameters) {
		addJoinPoints(lineList.get(0), lineList.get(1), parameters);
	}
	
	public double getPriority() {
		return DOUBLE_BOND_PRORITY;
	}
	
	public void add(SVGLine line) {
		//this.appendChild(line);
		ensureLineList();
		lineList.add(line);
	}
	
	public SVGLine getLine(int i) {
		ensureLineList();
		return (i < 0 || i >= lineList.size()) ? null : lineList.get(i);
	}

	private void ensureLineList() {
		if (lineList == null) {
			lineList = new ArrayList<SVGLine>();
			//lineList = SVGLine.extractSelfAndDescendantLines(this);
		}
	}

	public SVGElement getSVGElement() {
		return null;
		//TODO change this if needed, likewise in HatchedBond etc.
		/*SVGG g = new SVGG();
		for (SVGLine l : lineList) {
			g.appendChild(l);
		}
		return g;*/
	}
	
	public String toString() {
		return "Double bond\n ... " + Arrays.toString(getJoinPoints().toArray());
	}

}