package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.euclid.Angle.Units;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;

import java.util.Arrays;
import java.util.List;

public class HatchedBond extends Joinable {
	
	private final static Logger LOG = Logger.getLogger(HatchedBond.class);

	private static final double HATCHED_BOND_PRORITY = 2.0;

	private List<SVGLine> lines;

	private boolean unspecifiedDirection;

	public HatchedBond(ChemistryBuilderParameters parameters, List<SVGLine> lines) {
		this.lines = lines;
		addJoinPoints(parameters);
	}

	public double getPriority() {
		return HATCHED_BOND_PRORITY;
	}
	
	public boolean isUnspecifiedDirection() {
		return unspecifiedDirection;
	}
	
	public void addJoinPoints(ChemistryBuilderParameters parameters) {
		SVGLine lastLine = lines.get(lines.size() - 1);
		SVGLine shortestLine;
		SVGLine longestLine;
		if (lines.get(0).getLength() > lastLine.getLength()) {
			longestLine = lines.get(0);
			shortestLine = lastLine;
		} else {
			longestLine = lastLine;
			shortestLine = lines.get(0);
		}
		try {
			double scaleFactor = (longestLine.getMidPoint().getDistance(shortestLine.getMidPoint()) + lines.get(0).getMidPoint().getDistance(lines.get(1).getMidPoint())) / longestLine.getMidPoint().getDistance(shortestLine.getMidPoint());
			Real2 newPoint1 = new Real2(longestLine.getMidPoint().getX() + (shortestLine.getMidPoint().getX() - longestLine.getMidPoint().getX()) * scaleFactor, longestLine.getMidPoint().getY() + (shortestLine.getMidPoint().getY() - longestLine.getMidPoint().getY()) * scaleFactor);
			scaleFactor = (longestLine.getMidPoint().getDistance(shortestLine.getMidPoint()) + lines.get(0).getMidPoint().getDistance(lines.get(1).getMidPoint()) / 2) / longestLine.getMidPoint().getDistance(shortestLine.getMidPoint());
			Real2 newPoint2 = new Real2(shortestLine.getMidPoint().getX() + (longestLine.getMidPoint().getX() - shortestLine.getMidPoint().getX()) * scaleFactor, shortestLine.getMidPoint().getY() + (longestLine.getMidPoint().getY() - shortestLine.getMidPoint().getY()) * scaleFactor);
			getJoinPoints().add(new JoinPoint(newPoint1, parameters.getJoinPointRadius()));
			getJoinPoints().add(new JoinPoint(newPoint2, parameters.getJoinPointRadius()));
			unspecifiedDirection = false;
		} catch (IndexOutOfBoundsException e) {
			Real2 perpendicularVector = longestLine.getEuclidLine().getVector().getTransformed(new Transform2(new Angle(90, Units.DEGREES))).getUnitVector();
			getJoinPoints().add(new JoinPoint(longestLine.getMidPoint().plus(perpendicularVector.multiplyBy(longestLine.getLength() * parameters.getRelativeDistanceFromSingleLine())), parameters.getJoinPointRadius()));
			getJoinPoints().add(new JoinPoint(longestLine.getMidPoint().subtract(perpendicularVector.multiplyBy(longestLine.getLength() * parameters.getRelativeDistanceFromSingleLine())), parameters.getJoinPointRadius()));
			unspecifiedDirection = true;
		}
		//Vector2 perp = new Vector2(-smallestLine.getEuclidLine().getVector().getUnitVector().y, smallestLine.getEuclidLine().getVector().getUnitVector().x);
		//perp = (Vector2) perp.multiplyBy(lines.get(0).getMidPoint().getDistance(lines.get(1).getMidPoint()));
		//new SVGLine(longestLine.getMidPoint(), shortestLine.getMidPoint());
	}

	public SVGElement getSVGElement() {
		return null;
		/*SVGG g = new SVGG();
		for (SVGLine l : lineList) {
			g.appendChild(l);
		}
		return g;*/
	}
	
	public String toString() {
		return "Hatched bond\n ... " + Arrays.toString(getJoinPoints().toArray());
	}
	
}