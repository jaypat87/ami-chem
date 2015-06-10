package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Angle.Units;
import org.xmlcml.euclid.Real2;
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
public class DoubleBond extends Joinable {

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
		Angle eps = new Angle(0.1, Units.RADIANS);//We already know they are aligned
		SVGLine line0 = lineList.get(0);
		SVGLine line1 = lineList.get(1);
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