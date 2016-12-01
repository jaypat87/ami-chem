package org.xmlcml.ami2.chem;

import java.awt.Graphics2D;

import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import nu.xom.Nodes;

import org.apache.commons.math.complex.Complex;
import org.apache.log4j.Logger;
import org.xmlcml.ami2.chem.Joinable.JoinPoint;
import org.xmlcml.ami2.chem.JoinableText.AreInSameStringDetector;
import org.xmlcml.ami2.chem.svg.SVGContainerNew;
import org.xmlcml.diagrams.OCRManager;
import org.xmlcml.euclid.Angle;
import org.xmlcml.euclid.Angle.Units;
import org.xmlcml.euclid.Line2;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Array;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.graphics.svg.SVGCircle;
import org.xmlcml.graphics.svg.SVGConstants;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGImage;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGPolygon;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGTSpan;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.svgbuilder.geom.SimpleBuilder;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.UnionFind;
import com.google.common.util.concurrent.UncheckedTimeoutException;
//import net.sourceforge.tess4j.Tesseract;
//import net.sourceforge.tess4j.TesseractException;

/**
 * Builds higher-level primitives from SVGPaths, SVGLines, etc. to create SVG objects 
 * such as TramLine and (later) Arrow.
 * 
 * <p>SimpleBuilder's main function is to:
 * <ul>
 * <li>Read a raw SVG object and make lists of SVGPath and SVGText (and possibly higher levels ones
 * if present).</li>
 * <li>Turn SVGPaths into SVGLines , etc..</li>
 * <li>Identify Junctions (line-line, line-text, and probably more).</li>
 * <li>Join lines where they meet into higher level objects (TramLines, SVGRect, crosses, arrows, etc.).</li>
 * <li>Create topologies (e.g. connection of lines and Junctions).</li>
 * </ul>
 * 
 * SimpleBuilder uses the services of the org.xmlcml.graphics.svg.path package and may later use
 * org.xmlcml.graphics.svg.symbol.
 * </p>
 * 
 * <p>Input may either be explicit SVG primitives (e.g. &lt;svg:rect&gt;, &lt;svg:line&gt;) or 
 * implicit ones (&lt;svg:path&gt;) that can be interpreted as the above. The input may be either or
 * both - we can't control it. The implicit get converted to explicit and then merged with the 
 * explicit:
 * <pre>
 *    paths-> implicitLineList + rawLinelist -> explicitLineList 
 * </pre>
 * </p>
 * 
 * <h3>Strategy</h3>
 * <p>createHigherLevelPrimitives() carries out the complete chain from svgRoot to the final
 * primitives. Each step tests to see whether the result of the previous is null.
 * If so it creates a non-null list and fills it if possible.</p>
 * 
 * @author pm286
 */
public class ChemistryBuilder extends SimpleBuilder {
	
	private final static Logger LOG = Logger.getLogger(ChemistryBuilder.class);
	
	private ChemistryBuilderParameters parameters = new ChemistryBuilderParameters();

	protected HigherPrimitives higherPrimitives;

	protected SVGContainerNew input;

	private List<JoinableText> atomLabelTexts;
	private Map<Real2Range, Integer> atomLabelPositionsAndNumbers;
	
	List<WedgeBond> wedgeBonds = new ArrayList<WedgeBond>();
	
	private double scale = 1;

	static class MutuallyExclusiveShortLineTriple {
	
		HatchedBond hatchedBond;
		Charge minus;

		SVGLine line;
		SingleBond singleBond;
		
		public MutuallyExclusiveShortLineTriple(HatchedBond hatchedBond, Charge minus, SVGLine line) {
			this.hatchedBond = hatchedBond;
			this.minus = minus;
			this.line = line;
		}
	
	}

	static class MutuallyExclusiveShortLinePairTriple {
		
		HatchedBond hatchedBond;

		SVGLine line1;
		SVGLine line2;
		DoubleBond doubleBond;
		SingleBond singleBond1;
		SingleBond singleBond2;
		
		public MutuallyExclusiveShortLinePairTriple(HatchedBond hatchedBond, SVGLine line1, SVGLine line2) {
			this.hatchedBond = hatchedBond;
			this.line1 = line1;
			this.line2 = line2;
		}
		
	}
	
	static class MutuallyExclusiveLinePairPair {

		SVGLine line1;
		SVGLine line2;
		DoubleBond doubleBond;
		SingleBond singleBond1;
		SingleBond singleBond2;
		
		public MutuallyExclusiveLinePairPair(DoubleBond doubleBond) {
			this.line1 = doubleBond.getLine(0);
			this.line2 = doubleBond.getLine(1);
			this.doubleBond = doubleBond;
		}
		
	}

	List<MutuallyExclusiveShortLineTriple> mutuallyExclusiveShortLineTriples;
	List<MutuallyExclusiveShortLinePairTriple> mutuallyExclusiveShortLinePairTriples;
	List<MutuallyExclusiveLinePairPair> mutuallyExclusiveLinePairPairs;

	public ChemistryBuilder(SVGContainerNew svgRoot, long timeout, ChemistryBuilderParameters parameters) {
		super((SVGElement) svgRoot.getElement(), timeout);
		input = svgRoot;
		this.parameters = parameters;
	}

	public ChemistryBuilder(SVGContainerNew svgRoot, ChemistryBuilderParameters parameters) {
		super((SVGElement) svgRoot.getElement());
		input = svgRoot;
		this.parameters = parameters;
	}
	
	public ChemistryBuilder(SVGElement svgRoot, long timeout, ChemistryBuilderParameters parameters) {
		super(svgRoot, timeout);
		this.parameters = parameters;
	}

	public ChemistryBuilder(SVGElement svgRoot, ChemistryBuilderParameters parameters) {
		super(svgRoot);
		this.parameters = parameters;
	}
	
	public ChemistryBuilder(SVGContainerNew svgRoot, long timeout) {
		super((SVGElement) svgRoot.getElement(), timeout);
		input = svgRoot;
		parameters = new ChemistryBuilderParameters();
	}

	public ChemistryBuilder(SVGContainerNew svgRoot) {
		super((SVGElement) svgRoot.getElement());
		input = svgRoot;
		parameters = new ChemistryBuilderParameters();
	}
	
	public ChemistryBuilder(SVGElement svgRoot, long timeout) {
		super(svgRoot, timeout);
		parameters = new ChemistryBuilderParameters();
	}

	public ChemistryBuilder(SVGElement svgRoot) {
		super(svgRoot);
		parameters = new ChemistryBuilderParameters();
	}
	
	public ChemistryBuilderParameters getParameters() {
		return parameters;
	}

	public SVGContainerNew getInputContainer() {
		return input;
	}
	
	/**
	 * Complete processing chain for low-level SVG into high-level SVG and non-SVG primitives such as double bonds.
	 * <p>
	 * Creates junctions.
	 * <p>
	 * Runs createDerivedPrimitives().
	 * 
	 * @throws TimeoutException 
	 */
	public void createHigherPrimitives() {
		if (higherPrimitives == null) {
			startTiming();
			createDerivedPrimitives();
			replaceTextImagesWithText();
			splitMultiCharacterTexts();
			higherPrimitives = new HigherPrimitives();
			higherPrimitives.addSingleLines(derivedPrimitives.getLineList());
			handleShortLines();
			createUnsaturatedBondLists();
			//createWords();
			createJunctions();
		}
	}

	@Override
	protected void removeNearDuplicateAndObscuredPrimitives() {
		double scale = parameters.setStandardBondLengthFromSVG(derivedPrimitives.getLineList());
		nearDuplicateLineRemovalDistance *= (scale / this.scale);
		nearDuplicatePolygonRemovalDistance *= (scale / this.scale);
		minimumCutObjectGap *= (scale / this.scale);
		maximumCutObjectGap *= (scale / this.scale);
		this.scale = scale;
		super.removeNearDuplicateAndObscuredPrimitives();
	}
	
	private void splitMultiCharacterTexts() {
		Iterator<SVGText> it = derivedPrimitives.getTextList().iterator();
		List<SVGText> newTexts = new ArrayList<SVGText>();
		while (it.hasNext()) {
			SVGText text = it.next();
			String string = text.getText();
			List<SVGTSpan> spanList = new ArrayList<SVGTSpan>();
			double totalWidth = 0;
			if (string == null) {
				Nodes spans = text.query("svg:tspan", SVGSVG.SVG_XPATH);
				for (int i = 0; i < spans.size(); i++) {
					SVGTSpan span = (SVGTSpan) spans.get(i);
					spanList.add(span);
					GlyphVector v = span.getGlyphVector();
					totalWidth += v.getLogicalBounds().getWidth();
					if (span.getAttributeValue("dx") != null) {
						totalWidth += Double.parseDouble(span.getAttributeValue("dx"));
					}
				}
			} else {
				spanList.add(new SVGTSpan(text));
				totalWidth = text.getGlyphVector().getLogicalBounds().getWidth();
			}
			it.remove();
			double previousX = text.getX() - ("end".equals(text.getAttributeValue("text-anchor")) ? totalWidth : 0);
			double previousY = text.getY();
			for (SVGTSpan span : spanList) {
				if (span.getX() != 0.0) {
					previousX = span.getX() - ("end".equals(text.getAttributeValue("text-anchor")) ? totalWidth : 0);
				}
				if (span.getY() != 0.0) {
					previousY = span.getY();
				}
				GlyphVector glyphVector = span.getGlyphVector();
				String spanText = span.getText();
				for (int i = 0; i < spanText.length(); i++) {
					String substring = spanText.substring(i, i + 1);
					SVGText newText = new SVGText(new Real2(0, 0), substring);
					newTexts.add(newText);
					newText.copyAttributesFrom(span);
					double dX = 0;
					if (span.getAttributeValue("dx") != null) {
						dX = Double.parseDouble(span.getAttributeValue("dx"));
						newText.removeAttribute(newText.getAttribute("dx"));
					}
					double dY = 0;
					if (span.getAttributeValue("dy") != null) {
						dY = Double.parseDouble(span.getAttributeValue("dy"));
						newText.removeAttribute(newText.getAttribute("dy"));
					}
					newText.setX(previousX + dX + glyphVector.getGlyphPosition(i).getX());
					newText.setY(previousY + dY + glyphVector.getGlyphPosition(i).getY());
				}
				previousX += glyphVector.getGlyphPosition(glyphVector.getNumGlyphs()).getX();
				if (span.getAttributeValue("dy") != null) {
					previousY += Double.parseDouble(span.getAttributeValue("dy"));
				}
			}
		}
		derivedPrimitives.getTextList().addAll(newTexts);
	}

	public BufferedImage flipHorizontally(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(w, h, img.getColorModel().getTransparency());
		Graphics2D g = dimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
		g.dispose();
		return dimg;
	}

	private File getImageFileFromSVGImage(SVGImage image) {
		String filename = image.getAttributeValue("href", SVGConstants.XLINK_NS);
		File testFile = new File(filename);
		return (testFile.isAbsolute() ? testFile : new File(input.getFile().getParentFile().getAbsolutePath() + "/" + filename));
	}
	
	private void replaceTextImagesWithText() {
		if (rawPrimitives.getImageList().size() == 0) {
			return;
		}
		Set<Complex> done = new HashSet<Complex>();
		OCRManager manager = new OCRManager();
		
		for (SVGImage image : rawPrimitives.getImageList()) {
			try {
				checkTime("Took too long to convert images to text");
				image.applyTransformAttributeAndRemove();
				if (image.getWidth() > parameters.getMaximumImageElementWidthForOCR()) {
					continue;
				}
				File file = getImageFileFromSVGImage(image);
				BufferedImage bufferedImage = flipHorizontally(ImageIO.read(file));
				/*Tesseract tess = Tesseract.getInstance();
				try {
					s = tess.doOCR(im);
				} catch (TesseractException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/
				SVGText text = manager.scan(bufferedImage, new Real2Range(new RealRange(image.getX(), image.getX() + image.getWidth()), new RealRange(Math.min(image.getY(), image.getY() + image.getHeight()), Math.max(image.getY(), image.getY() + image.getHeight()))), parameters.getBlackThreshold(), parameters.getMaximumOCRError());
				if (text != null) {
					image.getParent().replaceChild(image, text);
					//text.copyAttributesFrom(image);
					derivedPrimitives.getTextList().add(text);
					derivedPrimitives.getImageList().remove(image);
				}
				
				if (!done.add(new Complex(image.getX(), image.getY())) || bufferedImage.getWidth() < parameters.getMimimumImageWidthForOCR()) {
					derivedPrimitives.getImageList().remove(image);
					continue;
				}
			} catch (IOException e) {
				System.err.println("Error handling image within SVG file - it's probably embedded in base 64, but it should be linked to and stored separately");
				e.printStackTrace();
			} catch (Exception e) {
				//TODO handle other images
			}

		}
		
		manager.handleAmbiguousTexts(parameters.getTextCoordinateTolerance(), parameters.getAllowedFontSizeVariation());
	}
	
	/*private void convertImagesOfTextToText() {
		for (SVGImage image : rawPrimitives.getImageList()) {
			String path = image.getAttributeValue("href", "xlink");
		}
	}*/

	/*private void createWords() {
		TextStructurer t = new TextStructurer(derivedPrimitives.getTextList());
		//List<RawWords> lines = t.createRawWordsList();
		List<ScriptLine> lines = t.getScriptedLineList();
		//j.get(0).getScriptWordList().get(0).createSuscriptTextLineList();
		List<JoinableScriptWord> words = new ArrayList<JoinableScriptWord>();
		higherPrimitives.setWordsList(words);
		for (ScriptLine line : lines) {
			for (ScriptWord word : line.getScriptWordList()) {
				words.add(new JoinableScriptWord(word));
			}
		}
	}*/

	private void handleShortLines() {
		List<HatchedBond> hatchList = new ArrayList<HatchedBond>();
		higherPrimitives.setHatchedBondList(hatchList);
		List<SVGLine> smallLines = new ArrayList<SVGLine>();
		for (SVGLine l : derivedPrimitives.getLineList()) {
			if (l.getXY(0).getDistance(l.getXY(1)) < parameters.getHatchLineMaximumLength() && l.getXY(0).getDistance(l.getXY(1)) > 0) {//TODO l.getLength() < hatchLineMaximumLength) {
				smallLines.add(l);
			}
		}
		higherPrimitives.setChargeList(new ArrayList<Charge>());
		if (smallLines.size() == 0) {
			mutuallyExclusiveShortLineTriples = new ArrayList<MutuallyExclusiveShortLineTriple>();
			mutuallyExclusiveShortLinePairTriples = new ArrayList<MutuallyExclusiveShortLinePairTriple>();
			return;
		}
		UnionFind<SVGLine> hatchedBonds = UnionFind.create(smallLines);
		for (int i = 0; i < smallLines.size(); i++) {
			SVGLine firstLine = smallLines.get(i);
			for (int j = i + 1; j < smallLines.size(); j++) {
				checkTime("Took too long to handle short lines");
				SVGLine secondLine = smallLines.get(j);
				Double dist = firstLine.calculateUnsignedDistanceBetweenLines(secondLine, new Angle((firstLine.getLength() < parameters.getTinyHatchLineMaximumLength() || secondLine.getLength() < parameters.getTinyHatchLineMaximumLength() ? parameters.getMaximumAngleForParallelIfOneLineIsTiny() : parameters.getMaximumAngleForParallel()), Units.RADIANS));
				if (dist != null && dist < parameters.getHatchLinesMaximumSpacing() && dist > parameters.getHatchLinesMinimumSpacing() && (firstLine.overlapsWithLine(secondLine, parameters.getLineOverlapEpsilon()) || secondLine.overlapsWithLine(firstLine, parameters.getLineOverlapEpsilon()))) {
					try {
						hatchedBonds.union(firstLine, secondLine);
					} catch (IllegalArgumentException e) {
						
					}
				}
				if ((firstLine.isHorizontal(parameters.getFlatLineEpsilon()) || secondLine.isHorizontal(parameters.getFlatLineEpsilon())) && firstLine.overlapsWithLine(secondLine, parameters.getLineOverlapEpsilon()) && secondLine.overlapsWithLine(firstLine, parameters.getLineOverlapEpsilon()) && firstLine.getEuclidLine().isPerpendicularTo(secondLine.getEuclidLine(), new Angle(parameters.getPlusChargeAngleTolerance(), Units.DEGREES))) {
					hatchedBonds.remove(firstLine);
					hatchedBonds.remove(secondLine);
					higherPrimitives.getLineList().remove(firstLine);
					higherPrimitives.getLineList().remove(secondLine);
					higherPrimitives.getLineChargeList().add(new Charge(parameters, firstLine, secondLine));
				}
			}
		}
		handleShortLines(hatchedBonds);
	}

	private void handleShortLines(UnionFind<SVGLine> disjointSets) {
		final double threshold = parameters.getThresholdForOrderingCheckForHatchedBonds();
		mutuallyExclusiveShortLineTriples = new ArrayList<MutuallyExclusiveShortLineTriple>();
		mutuallyExclusiveShortLinePairTriples = new ArrayList<MutuallyExclusiveShortLinePairTriple>();
		List<HatchedBond> hatchList = higherPrimitives.getHatchedBondList();
		set: for (Set<SVGLine> set : disjointSets.snapshot()) {
			ArrayList<SVGLine> lines1 = new ArrayList<SVGLine>(set);
			ArrayList<SVGLine> lines2 = new ArrayList<SVGLine>(set);
			Collections.sort(lines1, new Comparator<SVGLine>(){
				public int compare(SVGLine i, SVGLine j) {
					Real2 firstOfI = (i.getXY(0).getX() < i.getXY(1).getX() ? i.getXY(0) : i.getXY(1));
					Real2 firstOfJ = (j.getXY(0).getX() < j.getXY(1).getX() ? j.getXY(0) : j.getXY(1));
					return (Real.isEqual(firstOfI.getX(), firstOfJ.getX(), threshold) ? Double.compare(firstOfI.getY(), firstOfJ.getY()) : Double.compare(firstOfI.getX(), firstOfJ.getX()));
				}});
			Collections.sort(lines2, new Comparator<SVGLine>(){
				public int compare(SVGLine i, SVGLine j) {
					Real2 firstOfI = (i.getXY(0).getY() < i.getXY(1).getY() ? i.getXY(0) : i.getXY(1));
					Real2 firstOfJ = (j.getXY(0).getY() < j.getXY(1).getY() ? j.getXY(0) : j.getXY(1));
					return (Real.isEqual(firstOfI.getY(), firstOfJ.getY(), threshold) ? Double.compare(firstOfI.getX(), firstOfJ.getX()) : Double.compare(firstOfI.getY(), firstOfJ.getY()));
				}});
			ArrayList<SVGLine> lines3 = (ArrayList<SVGLine>) lines1.clone();
			Collections.reverse(lines3);
			ArrayList<SVGLine> lines4 = (ArrayList<SVGLine>) lines1.clone();
			ArrayList<SVGLine> lines5 = (ArrayList<SVGLine>) lines1.clone();
			Collections.sort(lines4, new Comparator<SVGLine>(){
				public int compare(SVGLine i, SVGLine j) {
					Real2 secondOfI = (i.getXY(0).getX() >= i.getXY(1).getX() ? i.getXY(0) : i.getXY(1));
					Real2 secondOfJ = (j.getXY(0).getX() >= j.getXY(1).getX() ? j.getXY(0) : j.getXY(1));
					return (Real.isEqual(secondOfI.getX(), secondOfJ.getX(), threshold) ? Double.compare(secondOfI.getY(), secondOfJ.getY()) : Double.compare(secondOfI.getX(), secondOfJ.getX()));
				}});
			Collections.sort(lines5, new Comparator<SVGLine>(){
				public int compare(SVGLine i, SVGLine j) {
					Real2 secondOfI = (i.getXY(0).getY() >= i.getXY(1).getY() ? i.getXY(0) : i.getXY(1));
					Real2 secondOfJ = (j.getXY(0).getY() >= j.getXY(1).getY() ? j.getXY(0) : j.getXY(1));
					return (Real.isEqual(secondOfI.getY(), secondOfJ.getY(), threshold) ? Double.compare(secondOfI.getX(), secondOfJ.getX()) : Double.compare(secondOfI.getY(), secondOfJ.getY()));
				}});
			ArrayList<SVGLine> lines6 = (ArrayList<SVGLine>) lines4.clone();
			Collections.reverse(lines6);
			ArrayList<SVGLine> lines7 = (ArrayList<SVGLine>) lines1.clone();
			ArrayList<SVGLine> lines8 = (ArrayList<SVGLine>) lines1.clone();
			Collections.sort(lines7, new Comparator<SVGLine>(){
				public int compare(SVGLine i, SVGLine j) {
					return (Real.isEqual(i.getMidPoint().getX(), j.getMidPoint().getX(), threshold) ? Double.compare(i.getMidPoint().getY(), j.getMidPoint().getY()) : Double.compare(i.getMidPoint().getX(), j.getMidPoint().getX()));
				}});
			Collections.sort(lines8, new Comparator<SVGLine>(){
				public int compare(SVGLine i, SVGLine j) {
					return (Real.isEqual(i.getMidPoint().getY(), j.getMidPoint().getY(), threshold) ? Double.compare(i.getMidPoint().getX(), j.getMidPoint().getX()) : Double.compare(i.getMidPoint().getY(), j.getMidPoint().getY()));
				}});
			ArrayList<SVGLine> lines9 = (ArrayList<SVGLine>) lines7.clone();
			Collections.reverse(lines9);
			boolean firstEndPointsAndSecondEndPointsOrdered = ((lines1.equals(lines2) || lines3.equals(lines2)) && (lines4.equals(lines5) || lines6.equals(lines5)));
			boolean firstEndPointsAndMidPointsOrdered = ((lines1.equals(lines2) || lines3.equals(lines2)) && (lines7.equals(lines8) || lines9.equals(lines8)));
			boolean secondEndPointsAndMidPointsOrdered = ((lines4.equals(lines5) || lines6.equals(lines5)) && (lines7.equals(lines8) || lines9.equals(lines8)));
			if (firstEndPointsAndSecondEndPointsOrdered || firstEndPointsAndMidPointsOrdered || secondEndPointsAndMidPointsOrdered) {
				ArrayList<SVGLine> lines;
				if (firstEndPointsAndSecondEndPointsOrdered || firstEndPointsAndMidPointsOrdered) {
					lines = lines1;
				} else {
					lines = lines4;
				}
				try {
					double change = lines.get(1).getLength() - lines.get(0).getLength();
					double direction = Math.signum(change);
					double firstLength = lines.get(0).getLength();
					for (int i = 2; i < lines.size() && change > parameters.getLengthTolerance(); i++) {
						if (Math.signum(lines.get(i).getLength() - firstLength) != direction) {
							continue set;
						}
					}
				} catch (IndexOutOfBoundsException e) {
					
				}
				HatchedBond hatchedBond = new HatchedBond(parameters, lines);
				hatchList.add(hatchedBond);
				if (lines.size() > 2) {
					higherPrimitives.getLineList().removeAll(lines);
				} else if (lines.size() == 2) {
					mutuallyExclusiveShortLinePairTriples.add(new MutuallyExclusiveShortLinePairTriple(hatchedBond, lines.get(0), lines.get(1)));
				} else {
					Charge charge = null;
					if (lines.get(0).isHorizontal(parameters.getFlatLineEpsilon()) && !lines.get(0).isVertical(parameters.getFlatLineEpsilon())) {
						charge = new Charge(parameters, lines);
						higherPrimitives.getLineChargeList().add(charge);
					}
					mutuallyExclusiveShortLineTriples.add(new MutuallyExclusiveShortLineTriple(hatchedBond, charge, lines.get(0)));
				}
			}
		}
	}
	
	private void createJunctions() {
		createJoinableList();
		List<Joinable> joinables = higherPrimitives.getJoinableList();

		List<JoinPoint> joinPoints = extractAtomLabelsAndGetRemainingJoinPoints(joinables);
		
		UnionFind<JoinPoint> joinPointsGroupedIntoJunctions = UnionFind.create(joinPoints);
		//int deleted = 0;
		attemptToJoinListOfJoinables(joinables, joinPointsGroupedIntoJunctions);
		
		try {
			handleAmbiguities(joinPointsGroupedIntoJunctions);
		} catch (IllegalArgumentException e) {
			joinPoints.clear();
			LOG.debug("Processing failed as the diagram was too complex");
		}
		
		List<Junction> junctions = new ArrayList<Junction>();
			
		if (joinPoints.size() != 0) {
			for (Set<JoinPoint> junctionJoinPoints : joinPointsGroupedIntoJunctions.snapshot()) {
				//if (junctionJoinPoints.size() != 1) {
					/*Set<Joinable> junctionJoinables = new HashSet<Joinable>();
					Set<JoinPoint> newJunctionJoinPoints = new HashSet <JoinPoint>();
					for (JoinPoint point : junctionJoinPoints) {
						if (junctionJoinables.add(point.getJoinable())) {
							newJunctionJoinPoints.add(point);
						} else {
							newJunctionJoinPoints.removeAll(point.getJoinable().getJoinPoints());
							removeJoinable(point.getJoinable());
							//junctionJoinables.remove(point.getJoinable());
						}
					}
					for (Joinable j1 : junctionJoinables) {
						int numberParallel = 0;
						for (Joinable j2 : junctionJoinables) {
							if (Joinable.areParallel(j1, j2)) {
								numberParallel++;
								if (numberParallel == 3) {
									for (JoinPoint p : newJunctionJoinPoints) {
										junctions.add(new Junction(p));
									}
									continue junction;
								}
							}
						}
					}
					junctions.add(new Junction(newJunctionJoinPoints));*/
				//}
				junctions.add(new Junction(junctionJoinPoints));
			}
		}
		higherPrimitives.setJunctionList(junctions);
				
				/*JoinPoint commonPoint = joinablei.getIntersectionPoint(joinablej);
				if (commonPoint != null) {
					Junction junction = new Junction(joinablei, joinablej, commonPoint);
					rawJunctionList.add(junction);
					String junctAttVal = "junct"+"."+rawJunctionList.size();
					junction.addAttribute(new Attribute(SVGElement.ID, junctAttVal));
					if (junction.getCoordinates() == null && commonPoint.getPoint() != null) {
						junction.setCoordinates(commonPoint.getPoint());
					}
					LOG.debug("junct: "+junction.getId()+" between " + joinablei.getClass() + " and " + joinablej.getClass() + " with coords "+junction.getCoordinates()+" "+commonPoint.getPoint());
				}
			}
		}*/
		
		/*createRawJunctionList();
		List<Junction> junctionList = new ArrayList<Junction>(higherPrimitives.getRawJunctionList());
		for (int i = junctionList.size() - 1; i > 0; i--) {
			Junction labile = junctionList.get(i);
			for (int j = 0; j < i; j++) {
				Junction fixed = junctionList.get(j);
				if (fixed.containsCommonPoints(labile)) {
					labile.transferDetailsTo(fixed);
					junctionList.remove(i);
					break;
				}
			}
		}
		higherPrimitives.setMergedJunctionList(junctionList);*/
	}

	private void attemptToJoinListOfJoinables(List<Joinable> joinables, UnionFind<JoinPoint> joinPointsGroupedIntoJunctions) {
		List<JoinableText> texts = new ArrayList<JoinableText>();
		for (int i = 0; i < joinables.size() - 1; i++) {
			Joinable joinableI = joinables.get(i);
			if (!(joinableI instanceof JoinableText)) {
				for (int j = i + 1; j < joinables.size(); j++) {
					Joinable joinableJ = joinables.get(j);
					if (!(joinableJ instanceof JoinableText)) {
						checkTime("Took too long to determine what is joined to what");
						
						//System.out.println(joinableI + "\n" + joinableJ);
						
						joinPointsGroupedIntoJunctions.unionAll(getListOfOverlappingJoinPointsForJoinables(joinPointsGroupedIntoJunctions, joinableI, joinableJ));
					}
				}
			} else {
				texts.add((JoinableText) joinableI);
			}
		}
		if (joinables.size() > 0) {
			if (joinables.get(joinables.size() - 1) instanceof JoinableText) {
				texts.add((JoinableText) joinables.get(joinables.size() - 1));
			}
			attemptToJoinTexts(texts, joinables, joinPointsGroupedIntoJunctions);
		}
	}

	private void attemptToJoinTexts(List<JoinableText> texts, List<Joinable> joinables, UnionFind<JoinPoint> joinPointsGroupedIntoJunctions) {
		for (int i = 0; i < texts.size() - 1; i++) {
			JoinableText textI = texts.get(i);
			for (int j = i + 1; j < texts.size(); j++) {
				JoinableText textJ = texts.get(j);
				checkTime("Took too long to determine what is joined to what");
				joinPointsGroupedIntoJunctions.unionAll(getListOfOverlappingJoinPointsForJoinables(joinPointsGroupedIntoJunctions, textI, textJ));
			}
		}
		for (JoinableText text : texts) {
			Set<JoinPoint> joinPoints = new HashSet<JoinPoint>();
			for (Joinable joinable : joinables) {
				if (!(joinable instanceof JoinableText)) {
					checkTime("Took too long to determine what is joined to what");
					List<JoinPoint> overlap = getListOfOverlappingJoinPointsForJoinables(joinPointsGroupedIntoJunctions, text, joinable);
					joinPoints.addAll(overlap);
					for (JoinPoint j : overlap) {
						if (!(j.getJoinable() instanceof JoinableText)) {
							joinPoints.addAll(joinPointsGroupedIntoJunctions.getObjectsInPartitionOf(j));
						}
					}
				}
			}
			List<JoinPoint> actualJoinPoints = new ArrayList<JoinPoint>();
			i: for (JoinPoint joinPointI : joinPoints) {
				for (JoinPoint joinPointJ : joinPoints) {
					if (joinPointI != joinPointJ && joinPointsGroupedIntoJunctions.getObjectsInPartitionOf(joinPointI).contains(joinPointJ)) {
						continue i;
					}
				}
				actualJoinPoints.add(joinPointI);
			}
			joinPointsGroupedIntoJunctions.unionAll(actualJoinPoints);
		}
	}

	private List<JoinPoint> getListOfOverlappingJoinPointsForJoinables(UnionFind<JoinPoint> joinPointsGroupedIntoJunctions, Joinable joinableI, Joinable joinableJ) {
		Set<JoinPoint> overlapSet = joinableI.overlapWith(joinableJ);
		if (overlapSet != null) {
			List<JoinPoint> overlapList = new ArrayList<JoinPoint>(overlapSet);
			if (!joinPointsGroupedIntoJunctions.contains(overlapList.get(0)) || !joinPointsGroupedIntoJunctions.contains(overlapList.get(1)) || (overlapList.size() > 2 && !joinPointsGroupedIntoJunctions.contains(overlapList.get(2))) || (overlapList.size() > 3 && !joinPointsGroupedIntoJunctions.contains(overlapList.get(3)))) {
				return new ArrayList<JoinPoint>();
			}
			if (mutuallyExclusive(joinableI, joinableJ)) {
				return new ArrayList<JoinPoint>();
			}
			if (joinableI instanceof JoinableText && joinableJ instanceof JoinableText) {
				if (JoinableText.doTextsJoin((JoinableText) joinableI, (JoinableText) joinableJ, parameters)) {
				//joinPointsGroupedIntoJunctions.union(overlap.get(0), overlap.get(1));
					return overlapList;
				}
			} else if ((joinableI instanceof JoinableText && joinableJ.getJoinPoints().size() == 2) || (joinableJ instanceof JoinableText && joinableI.getJoinPoints().size() == 2)) {
				Joinable lineJoinable = (joinableI instanceof JoinableText ? joinableJ : joinableI);
				JoinPoint lineJoinEnd = (overlapList.get(0).getJoinable() instanceof JoinableText ? overlapList.get(1) : overlapList.get(0));
				JoinPoint lineOtherEnd = (lineJoinable.getJoinPoints().get(0) == lineJoinEnd ? lineJoinable.getJoinPoints().get(1) : lineJoinable.getJoinPoints().get(0));
				Line2 line = new Line2(lineOtherEnd.getPoint(), lineJoinEnd.getPoint());
				JoinPoint text = (overlapList.get(0).getJoinable() instanceof JoinableText ? overlapList.get(0) : overlapList.get(1));
				Line2 testLine = new Line2(lineJoinEnd.getPoint(), text.getPoint());
				if (isNumber((SVGText) text.getJoinable().getSVGElement()) || line.isParallelTo(testLine, new Angle(parameters.getTightBondAndTextAngle(), Units.DEGREES))) {
					return overlapList;
				} else {
					text.setRadius(text.getRadius() * parameters.getSmallRadiusExpansion() / parameters.getLargeRadiusExpansion());
					Set<JoinPoint> overlapSet2 = joinableI.overlapWith(joinableJ);
					text.setRadius(text.getRadius() * parameters.getLargeRadiusExpansion() / parameters.getSmallRadiusExpansion());
					if (overlapSet2 != null && line.isParallelTo(testLine, new Angle(parameters.getLooseBondAndTextAngle(), Units.DEGREES))) {
						return overlapList;
					}
				}
			} else {
				return overlapList;
			}
			
			/*if (joinableI instanceof JoinableText && joinableJ instanceof JoinableText) {
				if (doTextsJoin(joinableI, joinableJ)) { 
					joinPointsGroupedIntoJunctions.union(overlap.get(0), overlap.get(1));
				}
			} else {
				joinPointsGroupedIntoJunctions.union(overlap.get(0), overlap.get(1));*/
				/*if (joinableI instanceof HatchedBond) {
					joinables.remove(whichLineIsWhichSingleBond.get(mutuallyExclusivePairs.get(joinableI)));
				}
				if (joinableJ instanceof HatchedBond) {
					joinables.remove(whichLineIsWhichSingleBond.get(mutuallyExclusivePairs.get(joinableJ)));
				}
				if (joinableI instanceof SingleBond) {
					joinables.remove(mutuallyExclusivePairs.inverse().get(joinableI));
				}
				if (joinableJ instanceof SingleBond) {
					joinables.remove(mutuallyExclusivePairs.inverse().get(joinableJ));
				}*/
			//}
			/*if (joinablei instanceof JoinableScriptWord && joinablej instanceof JoinableScriptWord) {
				if (!((JoinableScriptWord) joinablei).getScriptWord().toUnderscoreAndCaretString().equals("H") && !((JoinableScriptWord) joinablej).getScriptWord().toUnderscoreAndCaretString().equals("H")) {
					continue;
				}
			}*/
		}
		return new ArrayList<JoinPoint>();
	}

	private void removeJoinable(Joinable joinable) {
		higherPrimitives.getJoinableList().remove(joinable);
		higherPrimitives.getDoubleBondList().remove(joinable);
		higherPrimitives.getHatchedBondList().remove(joinable);
		higherPrimitives.getLineChargeList().remove(joinable);
	}

	private void handleAmbiguities(UnionFind<JoinPoint> joinPointsGroupedIntoJunctions) {
		for (MutuallyExclusiveShortLineTriple triple : mutuallyExclusiveShortLineTriples) {
			handleMutuallyExclusiveShortLineTriple(joinPointsGroupedIntoJunctions, triple);
		}
		
		for (MutuallyExclusiveShortLinePairTriple triple : mutuallyExclusiveShortLinePairTriples) {
			handleMutuallyExclusiveShortLinePairTriple(joinPointsGroupedIntoJunctions, triple);
		}

		Set<SingleBond> singleBonds = new HashSet<SingleBond>();
		for (MutuallyExclusiveLinePairPair pair : mutuallyExclusiveLinePairPairs) {
			singleBonds.add(pair.singleBond1);
			singleBonds.add(pair.singleBond2);
		}
		for (MutuallyExclusiveLinePairPair pair : mutuallyExclusiveLinePairPairs) {
			handleMutuallyExclusiveLinePairPair(joinPointsGroupedIntoJunctions, singleBonds, pair);
		}
	}

	private void handleMutuallyExclusiveShortLineTriple(UnionFind<JoinPoint> joinPointsGroupedIntoJunctions, MutuallyExclusiveShortLineTriple triple) {
		JoinPoint singleBondFirst = triple.singleBond.getJoinPoints().get(0);
		JoinPoint singleBondSecond = triple.singleBond.getJoinPoints().get(1);
		JoinPoint hatchedBondFirst = triple.hatchedBond.getJoinPoints().get(0);
		JoinPoint hatchedBondSecond = triple.hatchedBond.getJoinPoints().get(1);
		JoinPoint minus = (triple.minus == null ? null : triple.minus.getJoinPoints().get(0));
		if (joinPointsGroupedIntoJunctions.getSizeOfPartition(singleBondFirst) == 1 && joinPointsGroupedIntoJunctions.getSizeOfPartition(singleBondSecond) == 1 && joinPointsGroupedIntoJunctions.getSizeOfPartition(hatchedBondFirst) > 1 && joinPointsGroupedIntoJunctions.getSizeOfPartition(hatchedBondSecond) > 1) {
			undoDamageFromIncorrectMinus(joinPointsGroupedIntoJunctions, minus);
			joinPointsGroupedIntoJunctions.remove(singleBondFirst);
			joinPointsGroupedIntoJunctions.remove(singleBondSecond);
			joinPointsGroupedIntoJunctions.remove(minus);
			removeJoinable(triple.singleBond);
			higherPrimitives.getLineList().remove(triple.line);
			removeJoinable(triple.minus);
		} else if (joinPointsGroupedIntoJunctions.getSizeOfPartition(singleBondFirst) == 1 && joinPointsGroupedIntoJunctions.getSizeOfPartition(singleBondSecond) == 1 && (joinPointsGroupedIntoJunctions.getSizeOfPartition(hatchedBondFirst) == 1 || joinPointsGroupedIntoJunctions.getSizeOfPartition(hatchedBondSecond) == 1) && minus != null) {
			joinPointsGroupedIntoJunctions.remove(singleBondFirst);
			joinPointsGroupedIntoJunctions.remove(singleBondSecond);
			joinPointsGroupedIntoJunctions.remove(hatchedBondFirst);
			joinPointsGroupedIntoJunctions.remove(hatchedBondSecond);
			removeJoinable(triple.singleBond);
			higherPrimitives.getLineList().remove(triple.line);
			removeJoinable(triple.hatchedBond);
			removeJoinable(triple.hatchedBond);
		} else {
			undoDamageFromIncorrectMinus(joinPointsGroupedIntoJunctions, minus);
			joinPointsGroupedIntoJunctions.remove(hatchedBondFirst);
			joinPointsGroupedIntoJunctions.remove(hatchedBondSecond);
			joinPointsGroupedIntoJunctions.remove(minus);
			removeJoinable(triple.hatchedBond);
			removeJoinable(triple.minus);
			removeJoinable(triple.hatchedBond);
			removeJoinable(triple.minus);
		}
	}
	
	private void handleMutuallyExclusiveShortLinePairTriple(UnionFind<JoinPoint> joinPointsGroupedIntoJunctions, MutuallyExclusiveShortLinePairTriple triple) {
		joinPointsGroupedIntoJunctions.remove(triple.singleBond1.getJoinPoints().get(0));
		joinPointsGroupedIntoJunctions.remove(triple.singleBond1.getJoinPoints().get(1));
		joinPointsGroupedIntoJunctions.remove(triple.singleBond2.getJoinPoints().get(0));
		joinPointsGroupedIntoJunctions.remove(triple.singleBond2.getJoinPoints().get(1));
		removeJoinable(triple.singleBond1);
		removeJoinable(triple.singleBond2);
		higherPrimitives.getLineList().remove(triple.line1);
		higherPrimitives.getLineList().remove(triple.line2);
		if (triple.doubleBond == null) {
			return;
		}
		JoinPoint doubleBondFirst = triple.doubleBond.getJoinPoints().get(0);
		JoinPoint doubleBondSecond = triple.doubleBond.getJoinPoints().get(1);
		JoinPoint hatchedBondFirst = triple.hatchedBond.getJoinPoints().get(0);
		JoinPoint hatchedBondSecond = triple.hatchedBond.getJoinPoints().get(1);
		if ((joinPointsGroupedIntoJunctions.getSizeOfPartition(hatchedBondFirst) > 1 || joinPointsGroupedIntoJunctions.getSizeOfPartition(hatchedBondSecond) > 1) && joinPointsGroupedIntoJunctions.getSizeOfPartition(doubleBondFirst) == 1 && joinPointsGroupedIntoJunctions.getSizeOfPartition(doubleBondSecond) == 1) {
			joinPointsGroupedIntoJunctions.remove(doubleBondFirst);
			joinPointsGroupedIntoJunctions.remove(doubleBondSecond);
			removeJoinable(triple.doubleBond);
		} else {
			joinPointsGroupedIntoJunctions.remove(hatchedBondFirst);
			joinPointsGroupedIntoJunctions.remove(hatchedBondSecond);
			removeJoinable(triple.hatchedBond);
		}
	}

	private void handleMutuallyExclusiveLinePairPair(UnionFind<JoinPoint> joinPointsGroupedIntoJunctions, Set<SingleBond> singleBonds, MutuallyExclusiveLinePairPair pair) {
		JoinPoint doubleBondFirst = pair.doubleBond.getJoinPoints().get(0);
		JoinPoint doubleBondSecond = pair.doubleBond.getJoinPoints().get(1);
		boolean sewn = joinPointsGroupedIntoJunctions.get(doubleBondFirst).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond1.getJoinPoints().get(0)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondFirst).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond1.getJoinPoints().get(1)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondFirst).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond2.getJoinPoints().get(0)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondFirst).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond2.getJoinPoints().get(1)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondSecond).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond1.getJoinPoints().get(0)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondSecond).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond1.getJoinPoints().get(1)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondSecond).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond2.getJoinPoints().get(0)));
		sewn |= joinPointsGroupedIntoJunctions.get(doubleBondSecond).equals(joinPointsGroupedIntoJunctions.get(pair.singleBond2.getJoinPoints().get(1)));
		if (sewn) {
			Set<JoinPoint> points = joinPointsGroupedIntoJunctions.getObjectsInPartitionOf(doubleBondFirst);
			boolean foundParallel = false;
			for (JoinPoint p1 : points) {
				if (!(p1.getJoinable() instanceof DoubleBond) && !singleBonds.contains(p1.getJoinable()) && Joinable.areParallel(p1.getJoinable(), pair.doubleBond, new Angle(parameters.getToleranceForParallelJoinables(), Units.RADIANS))) {
					if (foundParallel) {
						joinPointsGroupedIntoJunctions.explode(points);
						joinPointsGroupedIntoJunctions.remove(doubleBondFirst);
						joinPointsGroupedIntoJunctions.remove(doubleBondSecond);
						removeJoinable(pair.doubleBond);
						Set<Joinable> joinables = new HashSet<Joinable>();
						for (JoinPoint p2 : points) {
							if (p2.getJoinable() != pair.doubleBond) {
								joinables.add(p2.getJoinable());
							}
						}
						attemptToJoinListOfJoinables(new ArrayList<Joinable>(joinables), joinPointsGroupedIntoJunctions);
						return;
					} else {
						foundParallel = true;
					}
				}
			}
			points = joinPointsGroupedIntoJunctions.getObjectsInPartitionOf(doubleBondSecond);
			foundParallel = false;
			for (JoinPoint p1 : points) {
				if (!(p1.getJoinable() instanceof DoubleBond) && !singleBonds.contains(p1.getJoinable()) && Joinable.areParallel(p1.getJoinable(), pair.doubleBond, new Angle(parameters.getToleranceForParallelJoinables(), Units.RADIANS))) {
					if (foundParallel) {
						joinPointsGroupedIntoJunctions.explode(points);
						joinPointsGroupedIntoJunctions.remove(doubleBondFirst);
						joinPointsGroupedIntoJunctions.remove(doubleBondSecond);
						removeJoinable(pair.doubleBond);
						Set<Joinable> joinables = new HashSet<Joinable>();
						for (JoinPoint p2 : points) {
							if (p2.getJoinable() != pair.doubleBond) {
								joinables.add(p2.getJoinable());
							}
						}
						attemptToJoinListOfJoinables(new ArrayList<Joinable>(joinables), joinPointsGroupedIntoJunctions);
						return;
					} else {
						foundParallel = true;
					}
				}
			}
			joinPointsGroupedIntoJunctions.remove(pair.singleBond1.getJoinPoints().get(0));
			joinPointsGroupedIntoJunctions.remove(pair.singleBond1.getJoinPoints().get(1));
			joinPointsGroupedIntoJunctions.remove(pair.singleBond2.getJoinPoints().get(0));
			joinPointsGroupedIntoJunctions.remove(pair.singleBond2.getJoinPoints().get(1));
			higherPrimitives.getLineList().remove(pair.line1);
			higherPrimitives.getLineList().remove(pair.line2);
			removeJoinable(pair.singleBond1);
			removeJoinable(pair.singleBond2);
		} else {
			joinPointsGroupedIntoJunctions.remove(pair.singleBond1.getJoinPoints().get(0));
			joinPointsGroupedIntoJunctions.remove(pair.singleBond1.getJoinPoints().get(1));
			joinPointsGroupedIntoJunctions.remove(pair.singleBond2.getJoinPoints().get(0));
			joinPointsGroupedIntoJunctions.remove(pair.singleBond2.getJoinPoints().get(1));
			higherPrimitives.getLineList().remove(pair.line1);
			higherPrimitives.getLineList().remove(pair.line2);
			removeJoinable(pair.singleBond1);
			removeJoinable(pair.singleBond2);
		}
	}
	
	private void undoDamageFromIncorrectMinus(UnionFind<JoinPoint> joinPointsGroupedIntoJunctions, JoinPoint minus) {
		if (minus != null) {
			Set<JoinPoint> points = joinPointsGroupedIntoJunctions.getObjectsInPartitionOf(minus);
			joinPointsGroupedIntoJunctions.explode(points);
			Set<Joinable> joinables = new HashSet<Joinable>();
			for (JoinPoint p : points) {
				if (p.getJoinable() != minus.getJoinable()) {
					joinables.add(p.getJoinable());
				}
			}
			attemptToJoinListOfJoinables(new ArrayList<Joinable>(joinables), joinPointsGroupedIntoJunctions);
		}
	}

	private boolean mutuallyExclusive(Joinable joinableI, Joinable joinableJ) {
		for (MutuallyExclusiveShortLineTriple triple : mutuallyExclusiveShortLineTriples) {
			if (joinableI == triple.hatchedBond && joinableJ == triple.minus || joinableJ == triple.hatchedBond && joinableI == triple.minus) {
				return true;
			}
			if (joinableI == triple.hatchedBond && joinableJ == triple.singleBond || joinableJ == triple.hatchedBond && joinableI == triple.singleBond) {
				return true;
			}
			if (joinableI == triple.singleBond && joinableJ == triple.minus || joinableJ == triple.singleBond && joinableI == triple.minus) {
				return true;
			}
		}
		
		for (MutuallyExclusiveShortLinePairTriple triple : mutuallyExclusiveShortLinePairTriples) {
			if (joinableI == triple.hatchedBond && joinableJ == triple.doubleBond || joinableJ == triple.hatchedBond && joinableI == triple.doubleBond) {
				return true;
			}
			if (joinableI == triple.hatchedBond && joinableJ == triple.singleBond1 || joinableJ == triple.hatchedBond && joinableI == triple.singleBond1) {
				return true;
			}
			if (joinableI == triple.singleBond1 && joinableJ == triple.doubleBond || joinableJ == triple.singleBond1 && joinableI == triple.doubleBond) {
				return true;
			}
			if (joinableI == triple.hatchedBond && joinableJ == triple.singleBond2 || joinableJ == triple.hatchedBond && joinableI == triple.singleBond2) {
				return true;
			}
			if (joinableI == triple.doubleBond && joinableJ == triple.singleBond2 || joinableJ == triple.doubleBond && joinableI == triple.singleBond2) {
				return true;
			}
			if (joinableI == triple.singleBond1 && joinableJ == triple.singleBond2 || joinableJ == triple.singleBond1 && joinableI == triple.singleBond2) {
				return true;
			}
		}
		
		/*for (MutuallyExclusiveLinePairPair pair : mutuallyExclusiveLinePairPairs) {
			if (joinableI == pair.singleBond2 && joinableJ == pair.doubleBond || joinableJ == pair.singleBond2 && joinableI == pair.doubleBond) {
				return true;
			}
			if (joinableI == pair.singleBond2 && joinableJ == pair.singleBond1 || joinableJ == pair.singleBond2 && joinableI == pair.singleBond1) {
				return true;
			}
			if (joinableI == pair.singleBond1 && joinableJ == pair.doubleBond || joinableJ == pair.singleBond1 && joinableI == pair.doubleBond) {
				return true;
			}
		}*/
		
		return false;
	}

	private List<JoinPoint> extractAtomLabelsAndGetRemainingJoinPoints(List<Joinable> joinables) {

		List<JoinPoint> remainingJoinPoints = new ArrayList<JoinPoint>();
		Map<Double, List<JoinableText>> listsOfTextsByFontSize = new LinkedHashMap<Double, List<JoinableText>>();
		for (Joinable j : joinables) {
			if (j instanceof JoinableText) {// && isLabel(((JoinableText) j).getSVGElement())) {// && !"1".equals(((JoinableText) j).getSVGElement().getText()) && !"2".equals(((JoinableText) j).getSVGElement().getText()) && !"3".equals(((JoinableText) j).getSVGElement().getText()) && !"4".equals(((JoinableText) j).getSVGElement().getText())) {
				List<JoinableText> joinablesForSize = listsOfTextsByFontSize.get(((JoinableText) j).getSVGElement().getFontSize());
				if (joinablesForSize == null) {
					joinablesForSize = new ArrayList<JoinableText>();
					listsOfTextsByFontSize.put(((JoinableText) j).getSVGElement().getFontSize(), joinablesForSize);
				}
				joinablesForSize.add((JoinableText) j);
			} else {
				remainingJoinPoints.addAll(j.getJoinPoints());
			}
		}
		
		double fontSizeOfLabels = Double.MAX_VALUE;
		list: for (Entry<Double, List<JoinableText>> list : listsOfTextsByFontSize.entrySet()) {
			AreInSameStringDetector sameString = new AreInSameStringDetector(list.getValue(), parameters, false, true);
			ImmutableCollection<Set<Joinable>> groups = sameString.texts.snapshot();
			//List<Integer> labelNumbers = new ArrayList<Integer>();
			Map<Real2Range, Integer> labelNumbers = new LinkedHashMap<Real2Range, Integer>();
			group: for (Set<Joinable> group : groups) {
				List<Joinable> potentialLabelTexts = new ArrayList<Joinable>(group);
				Joinable.sortJoinablesByX(potentialLabelTexts);
				String number = "";
				Real2Range bounds = new Real2Range();
				for (Joinable potentialLabelText : potentialLabelTexts) {
					if (!isLabel((SVGText) potentialLabelText.getSVGElement())) {
						for (Joinable t : group) {
							remainingJoinPoints.addAll(t.getJoinPoints());
						}
						continue group;
					}
					bounds.add(potentialLabelText.getJoinPoints().get(0).getPoint());
					if (isNumber((SVGText) potentialLabelText.getSVGElement())) {
						number += ((SVGText) potentialLabelText.getSVGElement()).getText();
					}
				}
				try {
					labelNumbers.put(bounds, Integer.parseInt(number));
				} catch (NumberFormatException e) {
					
				}
			}
			List<Integer> labelNumbersToBeSorted = new ArrayList<Integer>(labelNumbers.values());
			Collections.sort(labelNumbersToBeSorted);
			int previousPreviousLabel = 0;
			int previousLabel = 0;
			for (Integer i : labelNumbersToBeSorted) {
				if (i - previousLabel > labelNumbersToBeSorted.get(labelNumbersToBeSorted.size() - 1) * parameters.getMaximumLabelSequenceGap()) {
					for (JoinableText t : list.getValue()) {
						remainingJoinPoints.addAll(t.getJoinPoints());
					}
					continue list;
				}
				previousPreviousLabel = previousLabel;
				previousLabel = i;
			}
			if (list.getKey() < fontSizeOfLabels && labelNumbers.size() > 1) {
				if (atomLabelTexts != null) {
					for (JoinableText t : atomLabelTexts) {
						remainingJoinPoints.addAll(t.getJoinPoints());
					}
				}
				atomLabelTexts = list.getValue();
				atomLabelPositionsAndNumbers = labelNumbers;
				fontSizeOfLabels = list.getKey();
			} else {
				for (JoinableText t : list.getValue()) {
					remainingJoinPoints.addAll(t.getJoinPoints());
				}
			}
		}
		
		if (atomLabelTexts == null) {
			atomLabelTexts = new ArrayList<JoinableText>();
			atomLabelPositionsAndNumbers = new HashMap<Real2Range, Integer>();
		}

		return remainingJoinPoints;
	}
	
	private boolean isLetter(SVGText svgElement) {
		return (svgElement.getText() == null ? false : svgElement.getText().matches("[A-Za-z]"));
	}
	
	private boolean isNumber(SVGText svgElement) {
		return (svgElement.getText() == null ? false : svgElement.getText().matches("[0-9]"));
	}
	
	private boolean isLabel(SVGText svgElement) {
		return (svgElement.getText() == null ? false : svgElement.getText().matches("[0-9'a]"));
	}

	protected void createJoinableList() {
		List<Joinable> joinableList = createJoinableList(higherPrimitives.getLineList());
		joinableList.addAll(createJoinableList(derivedPrimitives.getPolygonList()));
		joinableList.addAll(createJoinableList(derivedPrimitives.getPathList()));
		joinableList.addAll(higherPrimitives.getDoubleBondList());
		joinableList.addAll(higherPrimitives.getHatchedBondList());
		joinableList.addAll(higherPrimitives.getLineChargeList());
		//joinableList.addAll(higherPrimitives.getWordList());
		joinableList.addAll(createJoinableList(derivedPrimitives.getTextList()));
		//joinableList.addAll(createJoinableList(derivedPrimitives.getImageList()));
		higherPrimitives.addJoinableList(joinableList);
	}
	
	public List<Joinable> createJoinableList(List<? extends SVGElement> elementList) {
		List<Joinable> joinableList = new ArrayList<Joinable>();
		for (SVGElement element : elementList) {
			Joinable joinable = createJoinable(element);
			if (joinable != null) {
				joinableList.add(joinable);
			}
		}
		return joinableList;
	}

	private Joinable createJoinable(SVGElement element) {
		Joinable joinable = null;
		if (element instanceof SVGLine) {
			joinable = new SingleBond(parameters, (SVGLine) element);
			for (MutuallyExclusiveShortLineTriple triple : mutuallyExclusiveShortLineTriples) {
				if (triple.line == element) {
					triple.singleBond = (SingleBond) joinable;
				}
			}
			for (MutuallyExclusiveShortLinePairTriple triple : mutuallyExclusiveShortLinePairTriples) {
				if (triple.line1 == element) {
					triple.singleBond1 = (SingleBond) joinable;
				}
				if (triple.line2 == element) {
					triple.singleBond2 = (SingleBond) joinable;
				}
			}
			for (MutuallyExclusiveLinePairPair pair : mutuallyExclusiveLinePairPairs) {
				if (pair.line1 == element) {
					pair.singleBond1 = (SingleBond) joinable;
				}
				if (pair.line2 == element) {
					pair.singleBond2 = (SingleBond) joinable;
				}
			}
		} else if (element instanceof SVGText) {
			if (("+".equals(((SVGText) element).getText()) || "-".equals(((SVGText) element).getText())) && !JoinableText.anyTextsInSameString((SVGText) element, derivedPrimitives.getTextList(), parameters, false, true) && !JoinableText.anyTextsToRightInSameString((SVGText) element, derivedPrimitives.getTextList(), parameters, true)) {
				joinable = new Charge(parameters, (SVGText) element);
			} else {
				joinable = new JoinableText(parameters, (SVGText) element);
			}
 		} else if (element instanceof SVGPolygon && ((SVGPolygon) element).createLineList(true).size() == 3) {
 			double shortest = Double.MAX_VALUE;
 			for (SVGLine line : ((SVGPolygon) element).getLineList()) {
 				if (line.getLength() < shortest) {
 					shortest = line.getLength();
 				}
 			}
 			if (shortest > 0.5) {
 				joinable = new WedgeBond(parameters, (SVGPolygon) element);
 				wedgeBonds.add((WedgeBond) joinable);
 			}
 		} else if (element instanceof SVGPolygon && ((SVGPolygon) element).createLineList(true).size() == 4) {
 			for (int i = 0; i < ((SVGPolygon) element).getReal2Array().size(); i++) {
 				Real2Array withoutPoint = new Real2Array(((SVGPolygon) element).getReal2Array());
 				Real2 deleted = withoutPoint.get(i);
 				withoutPoint.deleteElement(i);
 				SVGPolygon newPoly = new SVGPolygon(withoutPoint);
 				if (newPoly.containsPoint(deleted, 0)) {//withoutPoint.getRange2().includes(((SVGPolygon) element).getReal2Array().get(i))) {
 					((SVGPolygon) element).setReal2Array(withoutPoint);
 					joinable = new WedgeBond(parameters, (SVGPolygon) element);
 	 				wedgeBonds.add((WedgeBond) joinable);
 					break;
 				}
 			}
 		} else if (element instanceof SVGPath) {
 			try {
 				joinable = new WigglyBond(parameters, (SVGPath) element);
 			} catch (IllegalArgumentException e) {
 				
 			}
 		}
		if (joinable == null) {
 			LOG.debug("Unknown joinable: " + element);
 		}
		return joinable;
	}

	private void createUnsaturatedBondLists() {
		DoubleBondManager unsaturatedBondManager = new DoubleBondManager(parameters);
		try {
			unsaturatedBondManager.createBondLists(higherPrimitives.getLineList(), startTime + timeout - System.currentTimeMillis());
		} catch (TimeoutException e) {
			throw new UncheckedTimeoutException(e.getMessage());
		}
		//doubleBondManager.removeUsedDoubleBondPrimitives(higherPrimitives.getLineList());
		List<DoubleBond> doubleBondList = unsaturatedBondManager.getDoubleBondList();
		List<TripleBond> tripleBondList = unsaturatedBondManager.getTripleBondList();
		higherPrimitives.setDoubleBondList(doubleBondList);
		higherPrimitives.setTripleBondList(tripleBondList);
		mutuallyExclusiveLinePairPairs = new ArrayList<MutuallyExclusiveLinePairPair>();
		bond: for (DoubleBond bond : doubleBondList) {
			for (MutuallyExclusiveShortLinePairTriple pair : mutuallyExclusiveShortLinePairTriples) {
				if ((pair.line1 == bond.getLine(0) && pair.line2 == bond.getLine(1)) || (pair.line1 == bond.getLine(1) && pair.line2 == bond.getLine(0))) {
					pair.doubleBond = bond;
					continue bond;
				}
			}
			mutuallyExclusiveLinePairPairs.add(new MutuallyExclusiveLinePairPair(bond));
			//higherPrimitives.getLineList().add(bond.getLine(0));
			//higherPrimitives.getLineList().add(bond.getLine(1));
		}
	}

	/*private void createRawJunctionList() {
		createJoinableList();
		List<Joinable> joinableList = higherPrimitives.getJoinableList();
		List<Junction> rawJunctionList = new ArrayList<Junction>();
		for (int i = 0; i < joinableList.size() - 1; i++) {
			Joinable joinablei = joinableList.get(i);
			for (int j = i + 1; j < joinableList.size(); j++) {
				Joinable joinablej = joinableList.get(j);
				JoinPoint commonPoint = joinablei.getIntersectionPoint(joinablej);
				if (commonPoint != null) {
					Junction junction = new Junction(joinablei, joinablej, commonPoint);
					rawJunctionList.add(junction);
					String junctAttVal = "junct"+"."+rawJunctionList.size();
					junction.addAttribute(new Attribute(SVGElement.ID, junctAttVal));
					if (junction.getCoordinates() == null && commonPoint.getPoint() != null) {
						junction.setCoordinates(commonPoint.getPoint());
					}
					LOG.debug("junct: "+junction.getId()+" between " + joinablei.getClass() + " and " + joinablej.getClass() + " with coords "+junction.getCoordinates()+" "+commonPoint.getPoint());
				}
			}
		}
		higherPrimitives.setRawJunctionList(rawJunctionList);
	}*/

	public SVGElement getSVGRoot() {
		return svgRoot;
	}

	public HigherPrimitives getHigherPrimitives() {
		return higherPrimitives;
	}
	
	public Map<Real2Range, Integer> getAtomLabels() {
		return atomLabelPositionsAndNumbers;
	}

	void draw() {
		draw(new File("target/chem/andy.svg"));
	}
	
	public void draw(File file) {
		SVGG out = drawPrimitivesJoinPointsAndJunctions();
		SVGSVG.wrapAndWriteAsSVG(out, file);
	}

	SVGG drawPrimitivesJoinPointsAndJunctions() {
		SVGG out = new SVGG();
		SVGG circles = new SVGG();
		out.appendChild(circles);
		if (higherPrimitives.getJunctionList() != null) {
			for (Junction j : higherPrimitives.getJunctionList()) {
				Real2 coords = (j.getCoordinates() == null ? new Real2(0, 0) : j.getCoordinates());
				SVGCircle c = new SVGCircle(coords, 1.2);
				c.setFill("#555555");
				c.setOpacity(0.7);
				c.setStrokeWidth(0.0);
				circles.appendChild(c);
				SVGText t = new SVGText(coords.plus(new Real2(1.5, Math.random() * 6)), j.getID());
				circles.appendChild(t);
				for (JoinPoint point : j.getJoinPoints()) {
					SVGLine line = new SVGLine(coords, point.getPoint());
					line.setStrokeWidth(0.05);
					circles.appendChild(line);
				}
			}
		}
		for (SVGText t : getDerivedPrimitives().getTextList()) {
			SVGText o = (SVGText) t.copy();
			out.appendChild(o);
		}
		for (SVGLine l : getDerivedPrimitives().getLineList()) {
			SVGLine o = (SVGLine) l.copy();
			o.setStrokeWidth(0.4);
			out.appendChild(o);
		}
		for (SVGPolygon p : getDerivedPrimitives().getPolygonList()) {
			SVGPolygon o = (SVGPolygon) p.copy();
			o.setStrokeWidth(0.4);
			out.appendChild(o);
		}
		for (SVGPath p : getDerivedPrimitives().getPathList()) {
			SVGPath o = (SVGPath) p.copy();
			o.setStrokeWidth(0.4);
			out.appendChild(o);
		}
		for (Charge t : getHigherPrimitives().getLineChargeList()) {
			if (t.getSVGElement() != null) {
				SVGElement e = (SVGElement) t.getSVGElement().copy();
				out.appendChild(e);
			}
		}
		/*for (SVGImage t : simpleBuilder.getDerivedPrimitives().getImageList()) {
			SVGText e = new SVGText
			out.appendChild(e);
		}*/
		if (getHigherPrimitives().getJoinableList() != null) {
			for (Joinable j : getHigherPrimitives().getJoinableList()) {
				for (JoinPoint p : j.getJoinPoints()) {
					Real2 coords = (p.getPoint() == null ? new Real2(0, 0) : p.getPoint());
					SVGCircle c = new SVGCircle(coords, p.getRadius());
					if (j instanceof SingleBond) {
						c.setFill("#9999FF");
					} else if (j instanceof DoubleBond) {
						c.setFill("#99FF99");
					} else if (j instanceof TripleBond) {
						c.setFill("#CCCCCC");
					} else if (j instanceof HatchedBond) {
						c.setFill("#FF9999");
					} else if (j instanceof WedgeBond) {
						c.setFill("#99FFFF");
					} else if (j instanceof Charge) {
						c.setFill("#FFFF99");
					} else if (j instanceof JoinableText) {
						c.setFill("#FF99FF");
					} else if (j instanceof WigglyBond) {
						c.setFill("#999999");
					}
					c.setOpacity(0.7);
					c.setStrokeWidth(0.0);
					circles.appendChild(c);
					//SVGText t = new SVGText(coords.plus(new Real2(1.5, Math.random() * 6)), j.getId());
					//out.appendChild(t);
				}
			}
		}
		return out;
	}
	
}