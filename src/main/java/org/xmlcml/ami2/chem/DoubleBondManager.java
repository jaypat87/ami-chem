package org.xmlcml.ami2.chem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Angle.Units;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGLine;

public class DoubleBondManager {

	private static Logger LOG = Logger.getLogger(DoubleBondManager.class);

	private List<DoubleBond> doubleBondList;
	private List<TripleBond> tripleBondList;
	
	private ChemistryBuilderParameters parameters;
	
	private Set<SVGLine> usedLineSet;

	public DoubleBondManager(ChemistryBuilderParameters parameters) {
		ensureBondListsAndUsedLineSet();
		this.parameters = parameters;
	}

	private void ensureBondListsAndUsedLineSet() {
		if (doubleBondList == null) {
			doubleBondList = new ArrayList<DoubleBond>();
		}
		if (tripleBondList == null) {
			tripleBondList = new ArrayList<TripleBond>();
		}
 		if (usedLineSet == null) {
	 		usedLineSet = new HashSet<SVGLine>();
	 	}
	}
	
	public DoubleBond createDoubleBond(SVGLine lineI, SVGLine lineJ) {
		DoubleBond doubleBond = null;
		if (lineI == null || lineJ == null) {
			return doubleBond;
		}
		Double length1 = lineI.getXY(0).getDistance(lineI.getXY(1));//TODO getLength(); fix Euclid
		Double length2 = lineJ.getXY(0).getDistance(lineJ.getXY(1));//TODO getLength();
		Double longer = (length1 > length2 ? length1 : length2);
		Double shorter = (length1 > length2 ? length2 : length1);
		if (shorter / longer > parameters.getMinimumLengthRatio()) {
			if (lineI.isParallelOrAntiParallelTo(lineJ, new Angle(parameters.getParallelThresholdInRadians(), Units.RADIANS))) {
				Double dist1 = lineI.calculateUnsignedDistanceBetweenLines(lineJ, new Angle(360, Units.DEGREES));//TODO angleEps; fix Euclid
				SVGLine lineIAgain = (SVGLine) lineI.copy();
				lineIAgain.setXY(lineI.getXY(1), 0);
				lineIAgain.setXY(lineI.getXY(0), 1);
				Double dist2 = lineIAgain.calculateUnsignedDistanceBetweenLines(lineJ, new Angle(360, Units.DEGREES));//TODO angleEps;
				LOG.trace(dist1 + " " + dist2);
				if (dist1 < parameters.getMaximumAbsoluteSeparation() && dist2 < parameters.getMaximumAbsoluteSeparation() && dist1 < longer * parameters.getMaximumRelativeSeparation() && dist1 > longer * parameters.getMinimumRelativeSeparation() && dist2 < longer * parameters.getMaximumRelativeSeparation() && dist2 > longer * parameters.getMinimumRelativeSeparation()) {
					double reductionLambdaFirstPoint = 0.5 - parameters.getShrinkFactorForOverlapCheck() / 2;
					double reductionLambdaSecontPoint = 0.5 + parameters.getShrinkFactorForOverlapCheck() / 2;
					SVGLine lineI2 = new SVGLine(lineI.getEuclidLine().createPointOnLine(reductionLambdaFirstPoint * length1), lineI.getEuclidLine().createPointOnLine(reductionLambdaSecontPoint * length1));
					SVGLine lineJ2 = new SVGLine(lineJ.getEuclidLine().createPointOnLine(reductionLambdaFirstPoint * length2), lineJ.getEuclidLine().createPointOnLine(reductionLambdaSecontPoint * length2));
					if (lineI2.overlapsWithLine(lineJ2, parameters.getLineOverlapEpsilon()) || lineJ2.overlapsWithLine(lineI2, parameters.getLineOverlapEpsilon())) {
						doubleBond = new DoubleBond(parameters, lineI, lineJ);
					}
				}
			}
		}
		return doubleBond;
	}
	
	public TripleBond createTripleBond(DoubleBond doubleBond, SVGLine line) {
		TripleBond tripleBond = null;
		SVGLine lineI = doubleBond.getLine(0);
		SVGLine lineJ = doubleBond.getLine(1);
		Double length1 = lineI.getXY(0).getDistance(lineI.getXY(1));//TODO getLength();
		Double length2 = lineJ.getXY(0).getDistance(lineJ.getXY(1));//TODO getLength();
		Double length3 = line.getXY(0).getDistance(line.getXY(1));//TODO getLength();
		Double longest = (length1 > length2 ? (length1 > length3 ? length1 : length3) : (length2 > length3 ? length2 : length3));
		Double shortest = (length1 > length2 ? (length2 > length3 ? length3 : length2) : (length1 > length3 ? length3 : length1));
		if (shortest / longest > parameters.getMinimumLengthRatio()) {
			boolean check1 = line.isParallelOrAntiParallelTo(lineI, new Angle(parameters.getParallelThresholdInRadians(), Units.RADIANS));
			boolean check2 = line.isParallelOrAntiParallelTo(lineJ, new Angle(parameters.getParallelThresholdInRadians(), Units.RADIANS));
			if (check1 && check2) {
				Double dist1 = line.calculateUnsignedDistanceBetweenLines(lineI, new Angle(360, Units.DEGREES));//TODO angleEps;
				Double dist2 = line.calculateUnsignedDistanceBetweenLines(lineJ, new Angle(360, Units.DEGREES));//TODO angleEps;
				SVGLine lineAgain = (SVGLine) line.copy();
				lineAgain.setXY(line.getXY(1), 0);
				lineAgain.setXY(line.getXY(0), 1);
				Double dist3 = lineAgain.calculateUnsignedDistanceBetweenLines(lineI, new Angle(360, Units.DEGREES));//TODO angleEps;
				Double dist4 = lineAgain.calculateUnsignedDistanceBetweenLines(lineJ, new Angle(360, Units.DEGREES));//TODO angleEps;
				LOG.trace(dist1 + " " + dist2 + " " + dist3 + " " + dist4);
				boolean secondCheck1 = dist1 < parameters.getMaximumAbsoluteSeparation() && dist3 < parameters.getMaximumAbsoluteSeparation() && dist1 < longest * parameters.getMaximumRelativeSeparation() && dist1 > longest * parameters.getMinimumRelativeSeparation() && dist3 < longest * parameters.getMaximumRelativeSeparation() && dist3 > longest * parameters.getMinimumRelativeSeparation();
				boolean secondCheck2 = dist2 < parameters.getMaximumAbsoluteSeparation() && dist4 < parameters.getMaximumAbsoluteSeparation() && dist2 < longest * parameters.getMaximumRelativeSeparation() && dist2 > longest * parameters.getMinimumRelativeSeparation() && dist4 < longest * parameters.getMaximumRelativeSeparation() && dist4 > longest * parameters.getMinimumRelativeSeparation();
				if (secondCheck1 || secondCheck2) {
					double reductionLambdaFirstPoint = 0.5 - parameters.getShrinkFactorForOverlapCheck() / 2;
					double reductionLambaSecondPoint = 0.5 + parameters.getShrinkFactorForOverlapCheck() / 2;
					SVGLine lineI2 = new SVGLine(lineI.getEuclidLine().createPointOnLine(reductionLambdaFirstPoint * length1), lineI.getEuclidLine().createPointOnLine(reductionLambaSecondPoint * length1));
					SVGLine lineJ2 = new SVGLine(lineJ.getEuclidLine().createPointOnLine(reductionLambdaFirstPoint * length2), lineJ.getEuclidLine().createPointOnLine(reductionLambaSecondPoint * length2));
					SVGLine line2 = new SVGLine(line.getEuclidLine().createPointOnLine(reductionLambdaFirstPoint * length3), line.getEuclidLine().createPointOnLine(reductionLambaSecondPoint * length3));
					boolean thirdCheck1 = line2.overlapsWithLine(lineI2, parameters.getLineOverlapEpsilon()) || lineI2.overlapsWithLine(line2, parameters.getLineOverlapEpsilon());
					boolean thirdCheck2 = line2.overlapsWithLine(lineJ2, parameters.getLineOverlapEpsilon()) || lineJ2.overlapsWithLine(line2, parameters.getLineOverlapEpsilon());
					if (thirdCheck1 || thirdCheck2) {
						tripleBond = new TripleBond(parameters, lineI, lineJ, line);
					}
				}
			}
		}
		return tripleBond;
	}
	
	public TripleBond createTripleBondWithCut(DoubleBond doubleBond, SVGLine line) {
		TripleBond tripleBond = null;
		SVGLine lineI = doubleBond.getLine(0);
		SVGLine lineJ = doubleBond.getLine(1);
		Double length = lineI.getXY(0).getDistance(lineI.getXY(1));//TODO getLength();
		boolean check1 = line.isParallelOrAntiParallelTo(lineI, new Angle(parameters.getParallelThresholdInRadians(), Units.RADIANS));
		boolean check2 = line.isParallelOrAntiParallelTo(lineJ, new Angle(parameters.getParallelThresholdInRadians(), Units.RADIANS));
		if (check1 && check2) {
			Double dist1 = line.calculateUnsignedDistanceBetweenLines(lineI, new Angle(360, Units.DEGREES));//TODO angleEps;
			Double dist2 = line.calculateUnsignedDistanceBetweenLines(lineJ, new Angle(360, Units.DEGREES));//TODO angleEps;
			SVGLine lineAgain = (SVGLine) line.copy();
			lineAgain.setXY(line.getXY(1), 0);
			lineAgain.setXY(line.getXY(0), 1);
			Double dist3 = lineAgain.calculateUnsignedDistanceBetweenLines(lineI, new Angle(360, Units.DEGREES));//TODO angleEps;
			Double dist4 = lineAgain.calculateUnsignedDistanceBetweenLines(lineJ, new Angle(360, Units.DEGREES));//TODO angleEps;
			LOG.trace(dist1 + " " + dist2 + " " + dist3 + " " + dist4);
			boolean secondCheck1 = dist1 < parameters.getMaximumAbsoluteSeparation() / 2 && dist3 < parameters.getMaximumAbsoluteSeparation() / 2 && dist1 < length * parameters.getMaximumRelativeSeparation() && dist1 > length * parameters.getMinimumRelativeSeparation() && dist3 < length * parameters.getMaximumRelativeSeparation() && dist3 > length * parameters.getMinimumRelativeSeparation();
			boolean secondCheck2 = dist2 < parameters.getMaximumAbsoluteSeparation() / 2 && dist4 < parameters.getMaximumAbsoluteSeparation() / 2 && dist2 < length * parameters.getMaximumRelativeSeparation() && dist2 > length * parameters.getMinimumRelativeSeparation() && dist4 < length * parameters.getMaximumRelativeSeparation() && dist4 > length * parameters.getMinimumRelativeSeparation();
			if (secondCheck1 && secondCheck2) {
				SVGLine across1 = new SVGLine(lineI.getXY(0), lineJ.getXY(0));
				SVGLine across2 = new SVGLine(lineI.getXY(1), lineJ.getXY(1));
				SVGLine across3 = new SVGLine(lineI.getXY(0), lineJ.getXY(1));
				SVGLine across4 = new SVGLine(lineI.getXY(1), lineJ.getXY(0));
				if (across1.getXY(0).getDistance(across1.getXY(1)) > across3.getXY(0).getDistance(across3.getXY(1))) {//TODO getLength();
					across1 = across3;
					across2 = across4;
				}
				Real2 intersection1 = across1.getIntersection(line);
				Real2 intersection2 = across2.getIntersection(line);
				double lambda1 = line.getEuclidLine().getLambda(intersection1);
				double lambda2 = line.getEuclidLine().getLambda(intersection2);
				if (lambda1 > 0.5) {
					lambda1 = 1 - lambda1;
				}
				if (lambda2 > 0.5) {
					lambda2 = 1 - lambda2;
				}
				boolean thirdCheck1 = (Math.abs(lambda1) * length < parameters.getThresholdForOrderingCheckForHatchedBonds() && lambda2 > 0 && lambda2 < 1);
				boolean thirdCheck2 = (Math.abs(lambda2) * length < parameters.getThresholdForOrderingCheckForHatchedBonds() && lambda1 > 0 && lambda1 < 1);
				if (thirdCheck1 || thirdCheck2) {
					if (thirdCheck1) {
						Real2 temporaryIntersection = intersection1;
						intersection1 = intersection2;
						intersection2 = temporaryIntersection;
					}
					SVGLine extracted = new SVGLine(intersection1, intersection2);
					tripleBond = new TripleBond(parameters, lineI, lineJ, extracted);
					double distance1 = line.getXY(0).getDistance(intersection2);
					double distance2 = line.getXY(1).getDistance(intersection2);
					line.setXY((distance1 > distance2 ? line.getXY(0) : line.getXY(1)), 0);
					line.setXY(intersection1, 1);
				}
			}
		}
		return tripleBond;
	}
	
	public void createBondLists(List<SVGLine> lineList, long timeout) throws TimeoutException {
		createDoubleBondListWithoutReusingLines(lineList, timeout);
		createTripleBondListWithoutReusingLines(lineList, timeout);
	}
	
	private void createDoubleBondListWithoutReusingLines(List<SVGLine> lineList, long timeout) throws TimeoutException {
		long startTime = System.currentTimeMillis();
		ensureBondListsAndUsedLineSet();
		if (doubleBondList.size() == 0) {
			outer: for (int i = 0; i < lineList.size() - 1; i++) {
				SVGLine lineI = lineList.get(i);
				for (int j = i + 1; j < lineList.size(); j++) {
					if (System.currentTimeMillis() - startTime >= timeout) {
						throw new TimeoutException("Took too long to look for double bonds");
					}
					SVGLine lineJ = lineList.get(j);
					if (!usedLineSet.contains(lineI) && !usedLineSet.contains(lineJ)) {
						DoubleBond doubleBond = createDoubleBond(lineI, lineJ);
						if (doubleBond != null) {
							LOG.trace("Double bond " + lineI.getId() + " " + lineJ.getId());
		 					doubleBond.setID("doublebond." + lineI.getId() + "." + lineJ.getId());
		 					doubleBondList.add(doubleBond);
		 					usedLineSet.add(lineI);
		 					usedLineSet.add(lineJ);
		 					continue outer;
						}
					}
				}
			}
		}
	}
	
	private void createTripleBondListWithoutReusingLines(List<SVGLine> lineList, long timeout) throws TimeoutException {
		long startTime = System.currentTimeMillis();
		ensureBondListsAndUsedLineSet();
		if (tripleBondList.size() == 0) {
			Iterator<DoubleBond> iterator = doubleBondList.iterator();
			outer: for (DoubleBond doubleBond = (iterator.hasNext() ? (DoubleBond) iterator.next() : null); doubleBond != null ; doubleBond = (iterator.hasNext() ? (DoubleBond) iterator.next() : null)) {
				for (int i = 0; i < lineList.size(); i++) {
					if (System.currentTimeMillis() - startTime >= timeout) {
						throw new TimeoutException("Took too long to look for triple bonds");
					}
					SVGLine line = lineList.get(i);
					if (!usedLineSet.contains(line)) {
						TripleBond tripleBond = createTripleBond(doubleBond, line);
						if (tripleBond != null) {
							LOG.trace("Triple bond " + doubleBond.getID() + " " + line.getId());
							tripleBond.setID("triplebond." + doubleBond.getID() + "." + line.getId());
		 					tripleBondList.add(tripleBond);
		 					iterator.remove();
		 					usedLineSet.add(line);
		 					continue outer;
						} else {
							tripleBond = createTripleBondWithCut(doubleBond, line);
							if (tripleBond != null) {
								LOG.trace("Triple bond " + doubleBond.getID() + " " + line.getId());
								tripleBond.setID("triplebond." + doubleBond.getID() + "." + line.getId());
			 					tripleBondList.add(tripleBond);
			 					iterator.remove();
			 					continue outer;
							}
						}
					}
				}
			}
		}
	}

	public List<DoubleBond> getDoubleBondList() {
		return doubleBondList;
	}

	public List<TripleBond> getTripleBondList() {
		return tripleBondList;
	}

	public List<SVGLine> removeUsedDoubleBondPrimitives(List<SVGLine> lineList) {
 		for (int i = lineList.size() - 1; i >= 0; i--) {
 			if (usedLineSet.contains(lineList.get(i))) {
 				lineList.remove(i);
 			}
 		}
 		return lineList;
 	}
	
}