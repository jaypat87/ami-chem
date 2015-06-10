package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;

import java.util.Arrays;

public class SingleBond extends Joinable {
	
	private final static Logger LOG = Logger.getLogger(SingleBond.class);

	private static final double SINGLE_BOND_PRIORITY = 5.0;
	
	ChemistryBuilderParameters parameters;

	private SVGLine svgLine;

	public SingleBond(ChemistryBuilderParameters parameters, SVGLine svgLine) {
		this.parameters = parameters;
		this.svgLine = svgLine;
		addJoinPoints();
	}

	public double getPriority() {
		return SINGLE_BOND_PRIORITY;
	}
	
	public void addJoinPoints() {
		//if (svgLine.getLength() < minimumLengthForJoinPointsToBeAtEnds)
		JoinPoint j1 = new JoinPoint(svgLine.getXY(0), parameters.getJoinPointRadius());
		JoinPoint j2 = new JoinPoint(svgLine.getXY(1), parameters.getJoinPointRadius());
		getJoinPoints().add(j1);
		getJoinPoints().add(j2);
		j1.setRadius(Math.min(j1.getRadius(), svgLine.getLength() / 2));
		j2.setRadius(Math.min(j2.getRadius(), svgLine.getLength() / 2));
	}
	
	public String getID() {
		return svgLine.getId();
	}

	public SVGElement getSVGElement() {
		return svgLine;
	}

	public String toString() {
		return svgLine.toXML() + "\n ... " + Arrays.toString(getJoinPoints().toArray());
	}
	
}