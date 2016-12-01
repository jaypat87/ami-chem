package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Angle.Units;
import org.xmlcml.euclid.Line2;
import org.xmlcml.euclid.Line2AndReal2Calculator;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 
 * Three parallel lines with overlap.
 * 
 * @author pm286
 */
public class TripleBond extends UnsaturatedBond {

	@SuppressWarnings("unused")
	private final static Logger LOG = Logger.getLogger(TripleBond.class);

	private static final double PRIORITY_TRIPLE_BOND = 4.0;

	private List<SVGLine> lineList;
	
	public TripleBond(ChemistryBuilderParameters parameters, SVGLine linei, SVGLine linej, SVGLine linek) {
		add(linei);
		add(linej);
		add(linek);
		addJoinPoints(parameters);
	}

	private void addJoinPoints(ChemistryBuilderParameters parameters) {
		SVGLine line0 = lineList.get(0);
		SVGLine line1 = lineList.get(1);
		SVGLine line2 = lineList.get(2);
		Real2 midpoint0 = line0.getMidPoint();
		Real2 midpoint1 = line1.getMidPoint();
		Real2 midpoint2 = line2.getMidPoint();
		Line2 lineNearMiddle = new Line2(midpoint0, midpoint1);
		Line2AndReal2Calculator calculator = new Line2AndReal2Calculator(lineNearMiddle, midpoint2);
		SVGLine side0;
		SVGLine side1;
		if (calculator.offEnd1) {
			side0 = line1;
			side1 = line2;
		} else if (calculator.offEnd2) {
			side0 = line0;
			side1 = line2;
		} else {
			side0 = line0;
			side1 = line1;
		}
		super.addJoinPoints(side0, side1, parameters);
	}
	
	public double getPriority() {
		return PRIORITY_TRIPLE_BOND;
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
		return "Triple bond\n ... " + Arrays.toString(getJoinPoints().toArray());
	}

}