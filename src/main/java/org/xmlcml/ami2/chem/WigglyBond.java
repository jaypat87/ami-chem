package org.xmlcml.ami2.chem;

import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGPathPrimitive;
import org.xmlcml.graphics.svg.path.ClosePrimitive;
import org.xmlcml.graphics.svg.path.CubicPrimitive;

public class WigglyBond extends Joinable {

	private static final double WIGGLY_BOND_PRORITY = 2.5;
	
	SVGPath path;

	public WigglyBond(ChemistryBuilderParameters parameters, SVGPath path) {
		this.path = path;
		addJoinPoints(parameters);
	}

	private void addJoinPoints(ChemistryBuilderParameters parameters) {
		Real2 end1 = path.parseDString().get(0).getFirstCoord();
		Real2 end2 = null;
		double maxDistance = 0;
		for (SVGPathPrimitive p : path.parseDString().getPrimitiveList()) {
			if (!(p instanceof ClosePrimitive)) {
				double distance = p.getFirstCoord().getDistance(end1);
				if (distance > maxDistance) {
					maxDistance = distance;
					end2 = p.getFirstCoord();
				}
			}
		}
		double currentDirection = 1.0;
		double previousDistanceToFirstEnd = 0;
		boolean fail = false;
		for (SVGPathPrimitive p : path.parseDString().getPrimitiveList()) {
			if (!(p instanceof ClosePrimitive)) {
				double distanceToFirstEnd = (p instanceof CubicPrimitive ? p.getLastCoord() : p.getFirstCoord()).getDistance(end1);
				if (Math.abs(distanceToFirstEnd - previousDistanceToFirstEnd) > maxDistance * parameters.getWigglyBondGapTolerance()) {
					fail = true;
					break;
				}
				if (Math.signum(distanceToFirstEnd - previousDistanceToFirstEnd) != currentDirection && Math.abs(distanceToFirstEnd - previousDistanceToFirstEnd) > parameters.getWigglyBondIncorrectDirectionTolerance()) {
					if (currentDirection == 1.0) {
						currentDirection = -1.0;
					} else {
						fail = true;
	 					break;
					}
				}
				previousDistanceToFirstEnd = distanceToFirstEnd;
			}
		}
		if (fail) {
			throw new IllegalArgumentException();
		}
		getJoinPoints().add(new JoinPoint(end1, parameters.getWigglyBondJoinPointRadius()));
		getJoinPoints().add(new JoinPoint(end2, parameters.getWigglyBondJoinPointRadius()));
	}
	
	@Override
	SVGPath getSVGElement() {
		return path;
	}

	@Override
	public double getPriority() {
		return WIGGLY_BOND_PRORITY;
	}

}