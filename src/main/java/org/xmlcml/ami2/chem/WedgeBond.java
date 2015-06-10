package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGPolygon;

import java.util.Arrays;

public class WedgeBond extends Joinable {
	
	private final static Logger LOG = Logger.getLogger(WedgeBond.class);

	private static final double TRIANGLE_PRORITY = 3.0;

	ChemistryBuilderParameters parameters;
	
	private SVGPolygon svgPolygon;

	public WedgeBond(ChemistryBuilderParameters parameters, SVGPolygon svgPolygon) {
		if (svgPolygon.createLineList(true).size() != 3) {
			throw new IllegalArgumentException();
		}
		this.parameters = parameters;
		this.svgPolygon = svgPolygon;
		addJoinPoints();
	}

	public double getPriority() {
		return TRIANGLE_PRORITY;
	}
	
	public void addJoinPoints() {
		double shortestLineLength = Double.MAX_VALUE;
		SVGLine shortestLine = null;
		Real2 point = null;
		/*List<Real2> allPoints = new ArrayList<Real2>();
		for (SVGMarker l : svgPolygon.getPointList()) {
			allPoints.add(((SVGCircle) l.getChild(0)).getXY());
		}*/
		for (SVGLine l : svgPolygon.getLineList()) {
			if (l.getLength() < shortestLineLength) {
				shortestLineLength = l.getLength();
				shortestLine = l;
				for (Real2 p : svgPolygon.getReal2Array()) {
					if (!p.isEqualTo(l.getXY(0), 1e-10) && !p.isEqualTo(l.getXY(1), 1e-10)) {
						point = p;
					}
				}
			}
		}
		getJoinPoints().add(new JoinPoint(point, parameters.getJoinPointRadius()));
		getJoinPoints().add(new JoinPoint(Real2.getCentroid(Arrays.asList(shortestLine.getXY(0), shortestLine.getXY(1))), parameters.getJoinPointRadius()));
	}

	public String getID() {
		return svgPolygon.getId();
	}

	public SVGPolygon getSVGElement() {
		return svgPolygon;
	}
	
	public String toString() {
		return svgPolygon.toXML() + "\n ... " + Arrays.toString(getJoinPoints().toArray());
	}
	
}