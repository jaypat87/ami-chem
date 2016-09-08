package org.xmlcml.ami2.chem;

import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Angle.Units;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGLine;

public abstract class UnsaturatedBond extends Joinable {

	protected void addJoinPoints(SVGLine line0, SVGLine line1, ChemistryBuilderParameters parameters) {
		Angle eps = new Angle(0.1, Units.RADIANS);//We already know they are aligned
		Real2 point00 = line0.getXY(0);
		Real2 point01 = line0.getXY(1);
		Real2 point10 = line1.getXY(0);
		Real2 point11 = line1.getXY(1);
		Real2 join0 = null;
		Real2 join1 = null;
		if (line0.isAntiParallelTo(line1, eps)) {
			join0 = point00.getMidPoint(point11);
			join1 = point01.getMidPoint(point10);
		} else {
			join0 = point00.getMidPoint(point10);
			join1 = point01.getMidPoint(point11);
		}
		JoinPoint point0 = new JoinPoint(join0, parameters.getJoinPointRadius());
		JoinPoint point1 = new JoinPoint(join1, parameters.getJoinPointRadius());
		point0.setRadius(Math.max(point0.getRadius(), line0.getMidPoint().getDistance(line1.getMidPoint()) / 2));
		point1.setRadius(Math.max(point1.getRadius(), line0.getMidPoint().getDistance(line1.getMidPoint()) / 2));
		getJoinPoints().add(point0);
		getJoinPoints().add(point1);
	}

}