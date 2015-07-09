package org.xmlcml.ami2.chem;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.RealArray;
import org.xmlcml.euclid.Univariate;
import org.xmlcml.graphics.svg.SVGLine;

import com.google.common.collect.UnionFind;

public class ChemistryBuilderParameters {

	/**
	 * Copyright 2011 Max Rohde
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 *
	 * An array list implemention depending on object identity (==) rather than
	 * equality (.equals) to identify elements.<br/>
	 * <br/>
	 * See: <a href=
	 * "http://nexnet.wordpress.com/2011/03/09/java-list-equality-and-object-identity/"
	 * >KnowledgeNetworks: Java List Equality and Object Identity</a>
	 *
	 * @author <a href="http://www.mxro.de/">Max Rohde</a>
	 * @param <E>
	 */
	public static class IdentityArrayList<E> extends ArrayList<E> {
	
		private static final long serialVersionUID = 1L;
	
		@Override
		public boolean remove(final Object o) {
			return super.remove(indexOf(o)) != null;
		}
	
		@Override
		public boolean contains(final Object o) {
			return indexOf(o) >= 0;
		}
	
		@Override
		public int indexOf(final Object o) {
			for (int i = 0; i < size(); i++) {
				if (o == get(i)) {
					return i;
				}
			}
			return -1;
		}
	
		@Override
		public int lastIndexOf(final Object o) {
			for (int i = size() - 1; i >= 0; i--) {
				if (o == get(i)) {
					return i;
				}
			}
			return -1;
		}
	
		public IdentityArrayList() {
			super();
		}
	
		public IdentityArrayList(final Collection<? extends E> c) {
			super(c);
		}
	
		public IdentityArrayList(final int initialCapacity) {
			super(initialCapacity);
		}

	}
	
	private final static Logger LOG = Logger.getLogger(ChemistryBuilderParameters.class);
	
	private static final double DEFAULT_STANDARD_BOND_LENGTH = 12.69;//13.1;
	private static final double DEFAULT_BOND_LENGTH_QUANTILE_AFTER_SPLITTING = 0.5;//0.74 then 0.43
	private static final double DEFAULT_BIG_JUMP_THRESHOLD = 0.6;
	private static final double DEFAULT_JUMP_WIDTH_THRESHOLD = 0.5;
	private static final int DEFAULT_EXPANSION_DELAY = 2;
	private static final double DEFAULT_RELATIVE_THRESHOLD_FOR_DETECTING_SMALL_HATCHED_BOND_TO_SINGLE_BOND_JUMP = 0.4;
	private static final double DEFAULT_MINIMUM_PROPORTION_OF_SINGLE_BONDS = 0.09;
	
	private double standardBondLength = DEFAULT_STANDARD_BOND_LENGTH;
	private double quantile = DEFAULT_BOND_LENGTH_QUANTILE_AFTER_SPLITTING;
	private double bigJumpThreshold = DEFAULT_BIG_JUMP_THRESHOLD;
	private double jumpWidthThreshold = DEFAULT_JUMP_WIDTH_THRESHOLD;
	private int expansionDelay = DEFAULT_EXPANSION_DELAY;
	private double relativeThresholdForDetectingSmallFirstJump = DEFAULT_RELATIVE_THRESHOLD_FOR_DETECTING_SMALL_HATCHED_BOND_TO_SINGLE_BOND_JUMP;
	private double minimumProportionOfSingleBonds = DEFAULT_MINIMUM_PROPORTION_OF_SINGLE_BONDS;
	
	private static final double HATCH_DETECTION_DEFAULT_MAXIMUM_LINE_LENGTH = 5 / DEFAULT_STANDARD_BOND_LENGTH;
	private static final double HATCH_DETECTION_DEFAULT_MAXIMUM_SPACING = 4.5 / DEFAULT_STANDARD_BOND_LENGTH;//5.5 then 4
	private static final double HATCH_DETECTION_DEFAULT_MINIMUM_SPACING = 0.5 / DEFAULT_STANDARD_BOND_LENGTH;
	private static final double HATCH_DETECTION_DEFAULT_MAXIMUM_TINY_LINE_LENGTH = 0.8 / DEFAULT_STANDARD_BOND_LENGTH;
	private static final double DEFAULT_FLAT_LINE_EPSILON = 0.5 / DEFAULT_STANDARD_BOND_LENGTH;
	private static final double WIGGLY_BOND_DETECTION_DEFAULT_INCORRECT_DIRECTION_TOLERANCE = 0.15 / DEFAULT_STANDARD_BOND_LENGTH;
	private static final double HATCH_DETECTION_DEFAULT_MAXIMUM_LENGTH_CHANGE_FOR_QUADRILATERAL_HATCHES = 0.01 / DEFAULT_STANDARD_BOND_LENGTH;
	private static final double DEFAULT_WIGGLY_BOND_JOINT_POINT_RADIUS = 1.5 / DEFAULT_STANDARD_BOND_LENGTH;
	
	private static final double DOUBLE_BOND_DETECTION_DEFAULT_MAXIMUM_ABSOLUTE_SEPARATION = 4.3 / DEFAULT_STANDARD_BOND_LENGTH;
	
	private static final double DEFAULT_JOIN_POINT_RADIUS = 1.05 / DEFAULT_STANDARD_BOND_LENGTH;
	
	private static final double HATCH_DETECTION_DEFAULT_THRESHOLD_FOR_ORDERING_CHECK = 0.1 / DEFAULT_STANDARD_BOND_LENGTH;
	
	private static final double HATCH_DETECTION_DEFAULT_MAXIMUM_ANGLE_FOR_PARALLEL = 0.15;
	private static final double DEFAULT_LINE_OVERLAP_EPSILON = 1e-8;
	private static final double WORD_DETECTION_DEFAULT_RELATIVE_TEXT_Y_COORDINATE_TOLERANCE = 0.033;
	private static final double WORD_DETECTION_DEFAULT_MAXIMUM_CHARACTER_X_RANGE_OVERLAP_WHEN_ADJACENT = 0.1;//0.045; TODO the larger value is needed for some subscripts (a subscript can be placed very close to the character to its left)
	private static final double HATCH_DETECTION_DEFAULT_MAXIMUM_ANGLE_FOR_PARALLEL_IF_ONE_LINE_IS_TINY = 0.9;
	private static final double DEFAULT_MINIMUM_SPACE_WIDTH_IN_EN_SPACES = 0.3;
	private static final double DEFAULT_MAXIMUM_SPACE_WIDTH_IN_EN_SPACES = 1.2;//0.6;
	private static final double DEFAULT_PLUS_CHARGE_ANGLE_TOLERANCE = 0.1;
	private static final double WORD_DETECTION_DEFAULT_MINIMUM_Y_RANGE_OVERLAP_FOR_SUPERSCRIPTS_AND_SUBSCRIPTS = 3;
	private static final double ATOM_LABEL_DETECTION_DEFAULT_MAXIMUM_SEQUENCE_GAP_RELATIVE_TO_LARGEST = 0.4;
	private static final double WIGGLY_BOND_DETECTION_DEFAULT_RELATIVE_GAP_TOLERANCE = 0.2;
	private static final int OCR_DEFAULT_BLACK_THRESHOLD = 128;
	private static final int OCR_MINIMUM_IMAGE_WIDTH = 14;
	private static final double OCR_DEFAULT_MAXIMUM_IMAGE_ELEMENT_WIDTH = 12;
	private static final double DEFAULT_RELATIVE_FONT_SIZE_TOLERANCE = 0.9;
	private static final int DEFAULT_MAXIMUM_OCR_ERROR = 135;//15;
	
	private static final double DOUBLE_BOND_DETECTION_PARALLEL_THRESHOLD_IN_RADIANS = 0.3;
	private static final double DOUBLE_BOND_DETECTION_DEFAULT_MAXIMUM_RELATIVE_SEPARATION = 0.5;//0.4;//0.35;//This is tricky for very short tramlines
	private static final double DOUBLE_BOND_DETECTION_DEFAULT_MINIMUM_RELATIVE_SEPARATION = 0.08;
	private static final double DOUBLE_BOND_DETECTION_DEFAULT_MINIMUM_LENGTH_RATIO = 0.5;
	private static final double DOUBLE_BOND_DETECTION_DEFAULT_SHRINK_FACTOR_FOR_OVERLAP_CHECK = 0.5;

	private static final double DEFAULT_CHARGE_RADIUS_EXPANSION = 3.2;
	
	private static final double DEFAULT_RELATIVE_DISTANCE_FROM_SINGLE_HATCH_LINE = 1;
	
	private static final double DEFAULT_TOLERANCE_FOR_PARALLEL_JOINABLES = 0.1;
	
	private static final double DEFAULT_Y_AXIS_SHIFT_FOR_TEXT_CENTRE = 0.125;

	private static final double DEFAULT_SMALL_TEXT_RADIUS_EXPANSION = 0.85;//0.9;//.05;
	private static final double DEFAULT_LARGE_TEXT_RADIUS_EXPANSION = 1.05;
	private static final double DEFAULT_TIGHT_BOND_AND_TEXT_ANGLE = 15;
	private static final double DEFAULT_LOOSE_BOND_AND_TEXT_ANGLE = 40;

	private double hatchLineMaximumLength = HATCH_DETECTION_DEFAULT_MAXIMUM_LINE_LENGTH;
	private double hatchLinesMaximumSpacing = HATCH_DETECTION_DEFAULT_MAXIMUM_SPACING;
	private double hatchLinesMinimumSpacing = HATCH_DETECTION_DEFAULT_MINIMUM_SPACING;
	private double maximumAngleForParallel = HATCH_DETECTION_DEFAULT_MAXIMUM_ANGLE_FOR_PARALLEL;
	private double lineOverlapEpsilon = DEFAULT_LINE_OVERLAP_EPSILON;
	private double textCoordinateTolerance = WORD_DETECTION_DEFAULT_RELATIVE_TEXT_Y_COORDINATE_TOLERANCE;
	private double maximumCharacterXRangeOverlapWhenAdjacent = WORD_DETECTION_DEFAULT_MAXIMUM_CHARACTER_X_RANGE_OVERLAP_WHEN_ADJACENT;
	private double tinyHatchLineMaximumLength = HATCH_DETECTION_DEFAULT_MAXIMUM_TINY_LINE_LENGTH;
	private double maximumAngleForParallelIfOneLineIsTiny = HATCH_DETECTION_DEFAULT_MAXIMUM_ANGLE_FOR_PARALLEL_IF_ONE_LINE_IS_TINY;
	private double minimumSpaceWidthInEnSpaces = DEFAULT_MINIMUM_SPACE_WIDTH_IN_EN_SPACES;
	private double maximumSpaceWidthInEnSpaces = DEFAULT_MAXIMUM_SPACE_WIDTH_IN_EN_SPACES;
	private double plusChargeAngleTolerance = DEFAULT_PLUS_CHARGE_ANGLE_TOLERANCE;
	private double flatLineEpsilon = DEFAULT_FLAT_LINE_EPSILON;
	private double lengthTolerance = HATCH_DETECTION_DEFAULT_MAXIMUM_LENGTH_CHANGE_FOR_QUADRILATERAL_HATCHES;
	private double superscriptAndSubscriptOverlap = WORD_DETECTION_DEFAULT_MINIMUM_Y_RANGE_OVERLAP_FOR_SUPERSCRIPTS_AND_SUBSCRIPTS;
	private double wigglyBondIncorrectDirectionTolerance = WIGGLY_BOND_DETECTION_DEFAULT_INCORRECT_DIRECTION_TOLERANCE;
	private double maximumLabelSequenceGap = ATOM_LABEL_DETECTION_DEFAULT_MAXIMUM_SEQUENCE_GAP_RELATIVE_TO_LARGEST;
	private double wigglyBondGapTolerance = WIGGLY_BOND_DETECTION_DEFAULT_RELATIVE_GAP_TOLERANCE;
	private double wigglyBondJoinPointRadius = DEFAULT_WIGGLY_BOND_JOINT_POINT_RADIUS;
	private int blackThreshold = OCR_DEFAULT_BLACK_THRESHOLD;
	private int mimimumImageWidthForOCR = OCR_MINIMUM_IMAGE_WIDTH;
	private double maximumImageElementWidthForOCR = OCR_DEFAULT_MAXIMUM_IMAGE_ELEMENT_WIDTH;
	private double allowedFontSizeVariation = DEFAULT_RELATIVE_FONT_SIZE_TOLERANCE;
	private int maximumOCRError = DEFAULT_MAXIMUM_OCR_ERROR;
	
	private double parallelThresholdInRadians = DOUBLE_BOND_DETECTION_PARALLEL_THRESHOLD_IN_RADIANS;
	private double maximumRelativeSeparation = DOUBLE_BOND_DETECTION_DEFAULT_MAXIMUM_RELATIVE_SEPARATION;
	private double minimumRelativeSeparation = DOUBLE_BOND_DETECTION_DEFAULT_MINIMUM_RELATIVE_SEPARATION;
	private double minimumLengthRatio = DOUBLE_BOND_DETECTION_DEFAULT_MINIMUM_LENGTH_RATIO;

	private double maximumAbsoluteSeparation = DOUBLE_BOND_DETECTION_DEFAULT_MAXIMUM_ABSOLUTE_SEPARATION;
	private double shrinkFactorForOverlapCheck = DOUBLE_BOND_DETECTION_DEFAULT_SHRINK_FACTOR_FOR_OVERLAP_CHECK;

	private double chargeRadiusExpansion = DEFAULT_CHARGE_RADIUS_EXPANSION;

	private double relativeDistanceFromSingleLine = DEFAULT_RELATIVE_DISTANCE_FROM_SINGLE_HATCH_LINE;

	private double toleranceForParallelJoinables = DEFAULT_TOLERANCE_FOR_PARALLEL_JOINABLES;

	private double joinPointRadius = DEFAULT_JOIN_POINT_RADIUS;
	
	private double thresholdForOrderingCheckForHatchedBonds = HATCH_DETECTION_DEFAULT_THRESHOLD_FOR_ORDERING_CHECK;

	private double yAxisShiftForCentre = DEFAULT_Y_AXIS_SHIFT_FOR_TEXT_CENTRE;

	private double largeRadiusExpansion = DEFAULT_LARGE_TEXT_RADIUS_EXPANSION;
	private double smallRadiusExpansion = DEFAULT_SMALL_TEXT_RADIUS_EXPANSION;
	private double tightBondAndTextAngle = DEFAULT_TIGHT_BOND_AND_TEXT_ANGLE;
	private double looseBondAndTextAngle = DEFAULT_LOOSE_BOND_AND_TEXT_ANGLE;
	
	public double getHatchLineMaximumLength() {
		return hatchLineMaximumLength * standardBondLength;
	}

	public double getHatchLinesMaximumSpacing() {
		return hatchLinesMaximumSpacing * standardBondLength;
	}

	public double getHatchLinesMinimumSpacing() {
		return hatchLinesMinimumSpacing * standardBondLength;
	}

	public double getTinyHatchLineMaximumLength() {
		return tinyHatchLineMaximumLength * standardBondLength;
	}

	public double getFlatLineEpsilon() {
		return flatLineEpsilon * standardBondLength;
	}

	public double getWigglyBondIncorrectDirectionTolerance() {
		return wigglyBondIncorrectDirectionTolerance * standardBondLength;
	}

	public double getLengthTolerance() {
		return lengthTolerance * standardBondLength;
	}

	public double getWigglyBondJoinPointRadius() {
		return wigglyBondJoinPointRadius * standardBondLength;
	}

	public double getMaximumAbsoluteSeparation() {
		return maximumAbsoluteSeparation * standardBondLength;
	}
	
	public double getJoinPointRadius() {
		return joinPointRadius * standardBondLength;
	}
	
	public double getThresholdForOrderingCheckForHatchedBonds() {
		return thresholdForOrderingCheckForHatchedBonds * standardBondLength;
	}

	public double getMaximumAngleForParallel() {
		return maximumAngleForParallel;
	}

	public double getLineOverlapEpsilon() {
		return lineOverlapEpsilon;
	}

	public double getTextCoordinateTolerance() {
		return textCoordinateTolerance;
	}

	public double getMaximumCharacterXRangeOverlapWhenAdjacent() {
		return maximumCharacterXRangeOverlapWhenAdjacent;
	}

	public double getMaximumAngleForParallelIfOneLineIsTiny() {
		return maximumAngleForParallelIfOneLineIsTiny;
	}

	public double getMinimumSpaceWidthInEnSpaces() {
		return minimumSpaceWidthInEnSpaces;
	}

	public double getMaximumSpaceWidthInEnSpaces() {
		return maximumSpaceWidthInEnSpaces;
	}

	public double getPlusChargeAngleTolerance() {
		return plusChargeAngleTolerance;
	}

	public double getSuperscriptAndSubscriptOverlap() {
		return superscriptAndSubscriptOverlap;
	}

	public double getMaximumLabelSequenceGap() {
		return maximumLabelSequenceGap;
	}

	public double getWigglyBondGapTolerance() {
		return wigglyBondGapTolerance;
	}

	public int getBlackThreshold() {
		return blackThreshold;
	}

	public int getMimimumImageWidthForOCR() {
		return mimimumImageWidthForOCR;
	}

	public double getMaximumImageElementWidthForOCR() {
		return maximumImageElementWidthForOCR;
	}

	public double getAllowedFontSizeVariation() {
		return allowedFontSizeVariation;
	}

	public int getMaximumOCRError() {
		return maximumOCRError;
	}

	public double getParallelThresholdInRadians() {
		return parallelThresholdInRadians;
	}

	public double getMaximumRelativeSeparation() {
		return maximumRelativeSeparation;
	}

	public double getMinimumRelativeSeparation() {
		return minimumRelativeSeparation;
	}

	public double getMinimumLengthRatio() {
		return minimumLengthRatio;
	}

	public double getShrinkFactorForOverlapCheck() {
		return shrinkFactorForOverlapCheck;
	}

	public double getChargeRadiusExpansion() {
		return chargeRadiusExpansion;
	}

	public double getRelativeDistanceFromSingleLine() {
		return relativeDistanceFromSingleLine;
	}

	public double getToleranceForParallelJoinables() {
		return toleranceForParallelJoinables;
	}

	public double getyAxisShiftForCentre() {
		return yAxisShiftForCentre;
	}

	public double getLargeRadiusExpansion() {
		return largeRadiusExpansion;
	}

	public double getSmallRadiusExpansion() {
		return smallRadiusExpansion;
	}

	public double getTightBondAndTextAngle() {
		return tightBondAndTextAngle;
	}

	public double getLooseBondAndTextAngle() {
		return looseBondAndTextAngle;
	}
	
	double setStandardBondLengthFromSVG(List<SVGLine> lines) {
		/*if (0 == 0) {
			standardBondLength = 29.497618971706853;
			return standardBondLength / DEFAULT_STANDARD_BOND_LENGTH;
		}*/
		if(lines.size() == 0) {
			LOG.debug("Standard bond length: " + standardBondLength + " (normally " + DEFAULT_STANDARD_BOND_LENGTH + ")");
			return 1;
		}

		List<Double> lengths = getLengths(lines);
		List<Double> differences = getDifferences(lengths);
		int[] groups = groupDifferences(differences);
		
		int max = 0;
		List<Double> jumps = new IdentityArrayList<Double>();
		Map<Double, Double> jumpEnds = new IdentityHashMap<Double, Double>();
		Map<Double, Double> jumpStarts = new IdentityHashMap<Double, Double>();
		Map<Integer, Double> jumpWidths = new IdentityHashMap<Integer, Double>();
		Double runningSum = 0.0;
		int previousGroup = groups[0];
		int index = 0;
		int start = 0;
		for (int num : groups) {
			if (num > max) {
				max = num;
			}
			if (num != previousGroup) {
				jumps.add(runningSum);
				jumpStarts.put(runningSum, lengths.get(start));
				jumpEnds.put(runningSum, lengths.get(index));
				jumpWidths.put(new Integer(index - start), runningSum);
				runningSum = differences.get(index);
				previousGroup = num;
				start = index;
			} else {
				runningSum += differences.get(index);
			}
			index++;
		}
		jumpStarts.put(runningSum, lengths.get(start));
		jumpEnds.put(runningSum, lengths.get(lengths.size() - 1));
		jumpWidths.put(new Integer(lengths.size() - 1 - start), runningSum);
		jumps.add(runningSum);
		
		//System.out.println(max);
		
		List<Double> bigJumps = getBigJumps(jumps);
		List<Double> bigJumpsInOrder = getBigJumpsInOrder(jumps, bigJumps);
		
		double lowerPosition = -1;
		double upperPosition = -1;
		if (bigJumps.size() == 1) {
			if (jumps.size() > 1) {
				List<Integer> jumpWidthList = new IdentityArrayList<Integer>(jumpWidths.keySet());
				Collections.sort(jumpWidthList);
				Collections.reverse(jumpWidthList);
				if (jumpWidths.get(jumpWidthList.get(0)) != bigJumps.get(0)) {
					if (jumps.indexOf(jumpWidths.get(jumpWidthList.get(0))) < jumps.indexOf(bigJumps.get(0))) {
						lowerPosition = jumpEnds.get(jumpWidths.get(jumpWidthList.get(0)));
						upperPosition = jumpStarts.get(bigJumps.get(0));
					}
					if (jumps.indexOf(jumpWidths.get(jumpWidthList.get(1))) < jumps.indexOf(jumpWidths.get(jumpWidthList.get(0))) && jumps.indexOf(jumpWidths.get(jumpWidthList.get(1))) < jumps.indexOf(bigJumps.get(0))) {
						lowerPosition = jumpEnds.get(jumpWidths.get(jumpWidthList.get(1)));
						upperPosition = jumpStarts.get(bigJumps.get(0));
					}
				} else if (jumpWidthList.get(1) / ((double) jumpWidthList.get(0)) >= jumpWidthThreshold) {
					if (jumps.indexOf(jumpWidths.get(jumpWidthList.get(1))) < jumps.indexOf(bigJumps.get(0)) && jumpWidths.get(jumpWidthList.get(1)) * relativeThresholdForDetectingSmallFirstJump > jumps.get(jumps.indexOf(jumpWidths.get(jumpWidthList.get(1))) + 1)) {
						lowerPosition = jumpEnds.get(jumpWidths.get(jumpWidthList.get(1)));
						upperPosition = jumpStarts.get(bigJumps.get(0));
					}				
				}
			}
		} else {
			lowerPosition = jumpEnds.get(bigJumpsInOrder.get(0));
			upperPosition = jumpStarts.get(bigJumpsInOrder.get(1));
		}
		
		if (lowerPosition == -1) {
			int singleBonds = filterLinesAndApplyQuantile(lines, jumpEnds.get(bigJumpsInOrder.get(0)), Double.MAX_VALUE);
			if (singleBonds / ((double) lines.size()) < minimumProportionOfSingleBonds) {
				filterLinesAndApplyQuantile(lines, jumpStarts.get(bigJumpsInOrder.get(0)), Double.MAX_VALUE);
			}
		} else {
			filterLinesAndApplyQuantile(lines, lowerPosition, upperPosition);
		}
		
		/*UnionFind<Double> u = UnionFind.create(differences);
		for (int i = 0; i < differences.size(); i++) {
			for (int j = 0; j <= i; j++) {
				Double difference = sortedDifferences.get(j);
				int indexOfDifference = differences.indexOf(difference);
				//if ((indexOfDifference + 1 >= differences.size() || (indexOfDifference - 1 >= 0 && differences.get(indexOfDifference - 1) > differences.get(indexOfDifference + 1)))) {
				try {	
					u.union(difference, differences.get(indexOfDifference - 1));
				} catch (IndexOutOfBoundsException e) {
					
				}
				//} else {
				try {
					u.union(difference, differences.get(indexOfDifference + 1));
				} catch (IndexOutOfBoundsException e) {
					
				}
				//}
			}
		}*/
		
		/*PrintWriter p = null;
		try {
			p = new PrintWriter("C:/workspace/" + b.getInputContainer().getFile().getParentFile().getName() + "_" + b.getInputContainer().getFile().getName());
		} catch (FileNotFoundException e1) {
			
		}
		List<SVGLine> lines = SVGLine.extractSelfAndDescendantLines(b.getSVGRoot());
		List<Double> lengths = new ArrayList<Double>();
		RealArray lengthsForUni = new RealArray();
		lengths.add(0.0);
		for (SVGLine line : lines) {
			double length = line.getXY(0).getDistance(line.getXY(1));
			lengths.add(length);
			lengthsForUni.addElement(length);
		}
		Univariate uni = new Univariate(lengthsForUni);
		Collections.sort(lengths);
		for (double d : lengths) {
			p.println(d);
		}
		p.close();
		double biggestDifference = 0;
		double secondBiggestDifference = 0;
		double thirdBiggestDifference = 0;
		double positionOfBiggestDifference = -1;
		double positionOfSecondBiggestDifference = -1;
		double positionOfThirdBiggestDifference = -1;
		for (int i = 0; i < lengths.size() - 1; i++) {
			double test = lengths.get(i + 1) - lengths.get(i);
			if (test > biggestDifference) {
				thirdBiggestDifference = secondBiggestDifference;
				positionOfThirdBiggestDifference = positionOfSecondBiggestDifference;
				secondBiggestDifference = biggestDifference;
				positionOfSecondBiggestDifference = positionOfBiggestDifference;
				biggestDifference = test;
				positionOfBiggestDifference = (lengths.get(i + 1) + lengths.get(i)) / 2;
			} else if (test > secondBiggestDifference) {
				thirdBiggestDifference = secondBiggestDifference;
				positionOfThirdBiggestDifference = positionOfSecondBiggestDifference;
				secondBiggestDifference = test;
				positionOfSecondBiggestDifference = (lengths.get(i + 1) + lengths.get(i)) / 2;
			} else if (test > thirdBiggestDifference) {
				thirdBiggestDifference = test;
				positionOfThirdBiggestDifference = (lengths.get(i + 1) + lengths.get(i)) / 2;
			}
		}
		double positionBelowMedian = -1;
		double positionAboveMedian = -1;
		if (positionOfThirdBiggestDifference < uni.getMedian()) {
			positionBelowMedian = positionOfThirdBiggestDifference;
		} else {
			positionAboveMedian = positionOfThirdBiggestDifference;
		}
		if (positionOfSecondBiggestDifference < uni.getMedian()) {
			positionBelowMedian = positionOfSecondBiggestDifference;
		} else {
			positionAboveMedian = positionOfSecondBiggestDifference;
		}
		if (positionOfBiggestDifference < uni.getMedian()) {
			positionBelowMedian = positionOfBiggestDifference;
		} else {
			positionAboveMedian = positionOfBiggestDifference;
		}
		if (positionBelowMedian != -1 && positionAboveMedian != -1) {
			RealArray middlingLengths = new RealArray();
			for (SVGLine line : lines) {
				double length = line.getXY(0).getDistance(line.getXY(1));
				if (length > positionBelowMedian && length < positionAboveMedian) {
					middlingLengths.addElement(length);
				}
			}
			Univariate newUni = new Univariate(middlingLengths);
			try {
				standardBondLength = newUni.getQuantile(STANDARD_BOND_LENGTH_QUANTILE_AFTER_SPLITTING);
			} catch (ArrayIndexOutOfBoundsException e) {
				
			}
		} else {
			RealArray longerLengths = new RealArray();
			for (SVGLine line : lines) {
				double length = line.getXY(0).getDistance(line.getXY(1));
				if (length > positionOfBiggestDifference) {
					longerLengths.addElement(length);
				}
			}
			Univariate newUni = new Univariate(longerLengths);
			try {
				standardBondLength = newUni.getQuantile(STANDARD_BOND_LENGTH_QUANTILE_AFTER_SPLITTING);
			} catch (ArrayIndexOutOfBoundsException e) {
				
			}
			//standardBondLength = uni.getMedian();
			//for (double d : lengths) {
				//if (d > positionOfBiggestDifference) {
					//standardBondLength = d;
					//break;
				//}
			//}
			//standardBondLength = (positionOfBiggestDifference > positionOfSecondBiggestDifference ? positionOfBiggestDifference : positionOfSecondBiggestDifference);
			//for (double d1 : lengths) {
				//for (double d2 : lengths) {
					
				//}
			//}
		}*/
		
		//b.maximumCutObjectGap *= standardBondLength / DEFAULT_STANDARD_BOND_LENGTH;
		//b.minimumCutObjectGap *= standardBondLength / DEFAULT_STANDARD_BOND_LENGTH;
		LOG.debug("Standard bond length: " + standardBondLength + " (normally " + DEFAULT_STANDARD_BOND_LENGTH + ")");
		return standardBondLength / DEFAULT_STANDARD_BOND_LENGTH;
	}

	private List<Double> getBigJumps(List<Double> jumps) {
		List<Double> sortedJumps = new IdentityArrayList<Double>(jumps);
		Collections.sort(sortedJumps);
		Collections.reverse(sortedJumps);
		Double previous = sortedJumps.get(0);
		List<Double> bigJumps = new ArrayList<Double>();
		bigJumps.add(previous);
		for (int i = 1; i < jumps.size(); i++) {
			if (sortedJumps.get(i) / previous < bigJumpThreshold) {
				break;
			} else {
				bigJumps.add(sortedJumps.get(i));
				previous = sortedJumps.get(i);
			}
		}
		return bigJumps;
	}

	private List<Double> getLengths(List<SVGLine> lines) {
		List<Double> lengths = new IdentityArrayList<Double>();
		lengths.add(0.0);
		for (SVGLine line : lines) {
			double length = line.getXY(0).getDistance(line.getXY(1));
			lengths.add(length);
		}
		Collections.sort(lengths);
		return lengths;
	}

	private List<Double> getDifferences(List<Double> lengths2) {
		List<Double> differences = new IdentityArrayList<Double>();
		for (int i = 0; i < lengths2.size() - 1; i++) {
			differences.add(lengths2.get(i + 1) - lengths2.get(i));
		}
		return differences;
	}

	private List<Double> getBigJumpsInOrder(List<Double> jumps, List<Double> bigJumps) {
		List<Double> bigJumpsInOrder = new IdentityArrayList<Double>();
		for (Double jump : jumps) {
			if (bigJumps.contains(jump)) {
				bigJumpsInOrder.add(jump);
			}
		}
		return bigJumpsInOrder;
	}

	private int filterLinesAndApplyQuantile(List<SVGLine> lines2, double lowerPosition, double upperPosition) {
		RealArray singleBonds = new RealArray();
		for (SVGLine line : lines2) {
			double length = line.getXY(0).getDistance(line.getXY(1));
			if (length >= lowerPosition && length <= upperPosition) {
				singleBonds.addElement(length);
			}
		}
		Univariate newUni = new Univariate(singleBonds);
		try {
			standardBondLength = newUni.getQuantile(quantile);
		} catch (ArrayIndexOutOfBoundsException e) {
			standardBondLength = singleBonds.get(0);
		}
		return singleBonds.size();
	}

	private int[] groupDifferences(List<Double> differences) {
		List<Double> sortedDifferences = new IdentityArrayList<Double>(differences);
		Collections.sort(sortedDifferences);
		Collections.reverse(sortedDifferences);
		
		int[] groups = new int[differences.size()];
		for (int i = 0; i < differences.size(); i++) {
			groups[i] = -1;
		}
		
		for (int i = 0; i < differences.size() * expansionDelay; i++) {
			for (int j = 0; j <= i / expansionDelay; j++) {
				Double difference = sortedDifferences.get(j);
				int indexOfDifference = differences.indexOf(difference);
				int groupOfDifference = groups[indexOfDifference];
				if (groupOfDifference == -1) {
					groupOfDifference = groups[indexOfDifference] = j;
				}
				
				for (int k = indexOfDifference; k >= 0; k--) {
					if (groups[k] != groupOfDifference) {
						if (groups[k] == -1 && differences.get(k) < difference) {
							groups[k] = groupOfDifference;
						}
						break;
					}
				}
				
				for (int k = indexOfDifference; k < differences.size(); k++) {
					if (groups[k] != groupOfDifference) {
						if (groups[k] == -1 && differences.get(k) < difference) {
							groups[k] = groupOfDifference;
						}
						break;
					}
				}
			}
		}
		return groups;
	}
	
	public double getStandardBondLength() {
		return standardBondLength;
	}
	
}