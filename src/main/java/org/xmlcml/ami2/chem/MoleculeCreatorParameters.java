package org.xmlcml.ami2.chem;

import java.util.Map;
import java.util.Set;

import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealArray;
import org.xmlcml.euclid.Univariate;

public class MoleculeCreatorParameters extends ChemistryBuilderParameters {
	
	private static final double DEFAULT_STANDARD_MOLECULE_SIZE = 63.2;//79;
	private static final double STANDARD_MOLECULE_SIZE_QUANTILE = 0.63;//0.67;
	
	private double standardMoleculeSize = DEFAULT_STANDARD_MOLECULE_SIZE;
	
	private static final double DEFAULT_ARROW_AND_MOLECULE_GAP_OR_OVERLAP = 45 / DEFAULT_STANDARD_MOLECULE_SIZE;//35 then 40;
	private static final double DEFAULT_LABEL_AND_MOLECULE_GAP = 20 / DEFAULT_STANDARD_MOLECULE_SIZE;//40 then 25
	private static final double DEFAULT_LABEL_AND_ARROW_GAP = 15 / DEFAULT_STANDARD_MOLECULE_SIZE;//25 then 20
	private static final double DEFAULT_TEXT_LINE_MAXIMUM_SPACING = 5 / DEFAULT_STANDARD_MOLECULE_SIZE;//2.3;
	private static final double DEFAULT_TEXT_LINE_MAXIMUM_OVERLAP = 0.6 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_DISTANCE_FROM_LEFT_HAND_SIDE_FOR_NEW_REACTION_LINE = 35 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_REACTION_CAPTION_AND_ARROW_JITTER = 10 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_MOLECULE_CAPTION_AND_MOLECULE_JITTER = 30 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_MULTI_ARROW_SPACING = 15 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MOLECULE_LABEL_OVERLAP = 12.5 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_TAB_WIDTH = 12 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double TABLE_DETECTION_DEFAULT_MAXIMUM_COLUMN_JITTER = 2 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_TEXT_LINE_MAXIMUM_X_JITTER = 8 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_OVERLAP_BETWEEN_REACTION_LINES = 20 / DEFAULT_STANDARD_MOLECULE_SIZE;
	private static final double DEFAULT_MAXIMUM_DISTANCE_FOR_REACTION_CAPTION_LINED_UP_WITH_AN_END = 7 / DEFAULT_STANDARD_MOLECULE_SIZE;
	
	private static final double DEFAULT_BOND_LENGTH_SCALE = 0.1;
	
	private static final double DEFAULT_MAXIMUM_RATIO_OF_EXTREME_SINGLE_BOND_LENGTHS = 4.2;//45 before being made relative

	private double arrowAndMoleculeGapOrOverlap = DEFAULT_ARROW_AND_MOLECULE_GAP_OR_OVERLAP;
	private double labelAndMoleculeGap = DEFAULT_LABEL_AND_MOLECULE_GAP;
	private double labelAndArrowGap = DEFAULT_LABEL_AND_ARROW_GAP;
	private double maximumSpacingBetweenLabelLines = DEFAULT_TEXT_LINE_MAXIMUM_SPACING;
	private double maximumOverlapBetweenLabelLines = DEFAULT_TEXT_LINE_MAXIMUM_OVERLAP;
	private double maximumDistanceFromEdgeForReactionLine = DEFAULT_MAXIMUM_DISTANCE_FROM_LEFT_HAND_SIDE_FOR_NEW_REACTION_LINE;
	private double maximumReactionCaptionAndArrowJitter = DEFAULT_MAXIMUM_REACTION_CAPTION_AND_ARROW_JITTER;
	private double maximumMoleculeCaptionAndMoleculeJitter = DEFAULT_MAXIMUM_MOLECULE_CAPTION_AND_MOLECULE_JITTER;
	private double maximumMultiArrowSpacing = DEFAULT_MAXIMUM_MULTI_ARROW_SPACING;
	private double maximumMoleculeLabelOverlap = DEFAULT_MOLECULE_LABEL_OVERLAP;
	private double maximumTabDistance = DEFAULT_MAXIMUM_TAB_WIDTH;
	private double columnFindingMaximumXJitter = TABLE_DETECTION_DEFAULT_MAXIMUM_COLUMN_JITTER;
	//private double labelJoiningMaximumYJitter = ChemistryBuilder.WORD_DETECTION_DEFAULT_TEXT_Y_COORDINATE_TOLERANCE;
	private double labelJoiningMaximumXJitter = DEFAULT_TEXT_LINE_MAXIMUM_X_JITTER;
	private double allowedReactionLineOverlap = DEFAULT_MAXIMUM_OVERLAP_BETWEEN_REACTION_LINES;
	//private double allowedFontSizeVariation = ChemistryBuilder.DEFAULT_FONT_SIZE_TOLERANCE;
	//private double flatLineTolerance = ChemistryBuilder.DEFAULT_FLAT_LINE_EPSILON;
	private double maximumForNoReactionCaptionJitterTest = DEFAULT_MAXIMUM_DISTANCE_FOR_REACTION_CAPTION_LINED_UP_WITH_AN_END;
	
	private double bondLengthScale = DEFAULT_BOND_LENGTH_SCALE;

	private double maximumRatioOfExtremeSingleBondLengths = DEFAULT_MAXIMUM_RATIO_OF_EXTREME_SINGLE_BOND_LENGTHS;
	
	public double getArrowAndMoleculeGapOrOverlap() {
		return arrowAndMoleculeGapOrOverlap * standardMoleculeSize;
	}
	
	public double getLabelAndMoleculeGap() {
		return labelAndMoleculeGap * standardMoleculeSize;
	}
	
	public double getLabelAndArrowGap() {
		return labelAndArrowGap * standardMoleculeSize;
	}
	
	public double getMaximumSpacingBetweenLabelLines() {
		return maximumSpacingBetweenLabelLines * standardMoleculeSize;
	}
	
	public double getMaximumOverlapBetweenLabelLines() {
		return maximumOverlapBetweenLabelLines * standardMoleculeSize;
	}
	
	public double getMaximumDistanceFromEdgeForReactionLine() {
		return maximumDistanceFromEdgeForReactionLine * standardMoleculeSize;
	}
	
	public double getMaximumReactionCaptionAndArrowJitter() {
		return maximumReactionCaptionAndArrowJitter * standardMoleculeSize;
	}
	
	public double getMaximumMoleculeCaptionAndMoleculeJitter() {
		return maximumMoleculeCaptionAndMoleculeJitter * standardMoleculeSize;
	}
	
	public double getMaximumMultiArrowSpacing() {
		return maximumMultiArrowSpacing * standardMoleculeSize;
	}
	
	public double getMaximumMoleculeLabelOverlap() {
		return maximumMoleculeLabelOverlap * standardMoleculeSize;
	}
	
	public double getMaximumTabDistance() {
		return maximumTabDistance * standardMoleculeSize;
	}
	
	public double getColumnFindingMaximumXJitter() {
		return columnFindingMaximumXJitter * standardMoleculeSize;
	}
	
	public double getLabelJoiningMaximumXJitter() {
		return labelJoiningMaximumXJitter * standardMoleculeSize;
	}
	
	public double getAllowedReactionLineOverlap() {
		return allowedReactionLineOverlap * standardMoleculeSize;
	}
	
	public double getMaximumForNoReactionCaptionJitterTest() {
		return maximumForNoReactionCaptionJitterTest * standardMoleculeSize;
	}
	
	public double getBondLengthScale() {
		return bondLengthScale;
	}
	
	public double getMaximumRatioOfExtremeSingleBondLengths() {
		return maximumRatioOfExtremeSingleBondLengths;
	}
	
	void setStandardMoleculeSizeFromMolecules(Set<Real2Range> set) {
		RealArray sizes = new RealArray();
		for (Real2Range range : set) {
			//sizes.addElement(range.getXRange().getRange());
			//sizes.addElement(range.getYRange().getRange());
			if (range.getXRange().getRange() < range.getYRange().getRange()) {
				sizes.addElement(range.getXRange().getRange());
			} else {
				sizes.addElement(range.getYRange().getRange());
			}
		}
		Univariate uni = new Univariate(sizes);
		try {
			standardMoleculeSize = uni.getQuantile(STANDARD_MOLECULE_SIZE_QUANTILE);
		} catch (ArrayIndexOutOfBoundsException e1) {
			if (sizes.size() == 1) {
				standardMoleculeSize = sizes.get(0);
			}
		}
		System.out.println(standardMoleculeSize + " (normally " + DEFAULT_STANDARD_MOLECULE_SIZE + ")");
	}
	
}