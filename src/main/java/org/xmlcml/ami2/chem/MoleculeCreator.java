package org.xmlcml.ami2.chem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nu.xom.Element;

import org.apache.log4j.Logger;
import org.xmlcml.ami2.chem.Joinable.JoinPoint;
import org.xmlcml.ami2.chem.JoinableText.LargestFontFinderForJoinables;
import org.xmlcml.ami2.chem.svg.SVGContainerNew;
import org.xmlcml.cml.base.CMLAttribute;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.element.CMLAtom;
import org.xmlcml.cml.element.CMLBond;
import org.xmlcml.cml.element.CMLBondStereo;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLConditionList;
import org.xmlcml.cml.element.CMLLabel;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.CMLMolecule.HydrogenControl;
import org.xmlcml.cml.element.CMLMoleculeList;
import org.xmlcml.cml.element.CMLProduct;
import org.xmlcml.cml.element.CMLReactant;
import org.xmlcml.cml.element.CMLReaction;
import org.xmlcml.cml.element.CMLReactionList;
import org.xmlcml.cml.element.CMLScalar;
import org.xmlcml.cml.tools.MoleculeTool;
import org.xmlcml.cml.tools.SMILESTool;
import org.xmlcml.cml.tools.StereochemistryTool;
import org.xmlcml.euclid.EuclidRuntimeException;
import org.xmlcml.euclid.Line2;
import org.xmlcml.euclid.Line2AndReal2Calculator;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGRect;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.molutil.ChemicalElement;
import org.xmlcml.svg2xml.text.ScriptLine;
import org.xmlcml.svg2xml.text.ScriptWord;
import org.xmlcml.svg2xml.text.TextStructurer;
import org.xmlcml.xml.XMLUtil;

import blogspot.software_and_algorithms.stern_library.optimization.HungarianAlgorithm;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.UnionFind;

//import com.google.common.collect.Iterables; TODO is using Guava without adding it as a dependency a good idea?

/** 
 * Creates molecules and reactions from a ChemistryBuilder.
 * 
 * @author pm286
 */
public class MoleculeCreator {

	public class CMLPage extends CMLCml {

		private <T extends CMLElement> List<T> getDescendants(String name) {
			List<Element> elements = XMLUtil.getQueryElements(this, ".//cml:molecule", CMLCml.CML_XPATH);
			List<T> returnList = new ArrayList<T>();
			for (Element e : elements) {
				returnList.add((T) e);
			}
			return returnList;
		}

		public List<CMLReaction> getReactions() {
			return getDescendants("reaction");
		}
		
		public List<CMLMolecule> getMolecules() {
			return getDescendants("molecule");
		}
		
	}
	
	static class LineAndBoundingBoxCalculator {
		
		double maximumDistanceFromEnd = Double.MIN_VALUE;
		double minimumDistanceFromEnd = Double.MAX_VALUE;
		int offEnd1 = 0;
		int offEnd2 = 0;
		double minimumDistanceFromBoxToLine = Double.MAX_VALUE;
		
		public LineAndBoundingBoxCalculator(Line2 line, Real2Range box) {
			Real2[] corners = getCorners(box);
			Real2 point1 = line.getXY(0);
			Real2 point2 = line.getXY(1);
			for (int j = 0; j < 4; j++) {
				/*double dist = getMinimumDistanceOfPointFromLine(line, corners[j]);
				Real2 proj = line.getNearestPointOnLine(corners[j]);
				double dist1 = proj.getDistance(point1);
				double dist2 = proj.getDistance(point2);*/
				Line2AndReal2Calculator calc = new Line2AndReal2Calculator(line, corners[j]);
				if (calc.minimumDistance < minimumDistanceFromBoxToLine) {
					minimumDistanceFromBoxToLine = calc.minimumDistance;
				}
				if (calc.offEnd1) {
					if (calc.distanceOfProjectionFromEnd1 < minimumDistanceFromEnd) {
						minimumDistanceFromEnd = calc.distanceOfProjectionFromEnd1;
					}
					if (calc.distanceOfProjectionFromEnd1 > maximumDistanceFromEnd) {
						maximumDistanceFromEnd = calc.distanceOfProjectionFromEnd1;
					}
					offEnd1++;
				} else if (calc.offEnd2) {
					if (calc.distanceOfProjectionFromEnd2 < minimumDistanceFromEnd) {
						minimumDistanceFromEnd = calc.distanceOfProjectionFromEnd2;
					}
					if (calc.distanceOfProjectionFromEnd2 > maximumDistanceFromEnd) {
						maximumDistanceFromEnd = calc.distanceOfProjectionFromEnd2;
					}
					offEnd2++;
				}
			}
			for (Line2 edge : getEdges(box)) {
				try {
					Line2AndReal2Calculator calc1 = new Line2AndReal2Calculator(edge, point1);
					if (calc1.minimumDistance < minimumDistanceFromBoxToLine) {
						minimumDistanceFromBoxToLine = calc1.minimumDistance;
					}
				} catch (EuclidRuntimeException e) {
					
				}
				try {
					Line2AndReal2Calculator calc2 = new Line2AndReal2Calculator(edge, point2);
					if (calc2.minimumDistance < minimumDistanceFromBoxToLine) {
						minimumDistanceFromBoxToLine = calc2.minimumDistance;
					}
				} catch (EuclidRuntimeException e) {
					
				}
			}
		}

		public static Real2[] getCorners(Real2Range box) {
			Real2[] corners = new Real2[4];
			corners[0] = new Real2(box.getXMin(), box.getYMin());
			corners[1] = new Real2(box.getXMin(), box.getYMax());
			corners[2] = new Real2(box.getXMax(), box.getYMin());
			corners[3] = new Real2(box.getXMax(), box.getYMax());
			return corners;
		}

		public static Line2[] getEdges(Real2Range box) {
			Line2[] edges = new Line2[4];
			edges[0] = new Line2(new Real2(box.getXMin(), box.getYMin()), new Real2(box.getXMax(), box.getYMin()));
			edges[1] = new Line2(new Real2(box.getXMax(), box.getYMin()), new Real2(box.getXMax(), box.getYMax()));
			edges[2] = new Line2(new Real2(box.getXMax(), box.getYMax()), new Real2(box.getXMin(), box.getYMax()));
			edges[3] = new Line2(new Real2(box.getXMin(), box.getYMax()), new Real2(box.getXMin(), box.getYMin()));
			return edges;
		}
		
	}
	
	public static class DuplicateBondException extends RuntimeException {

		private static final long serialVersionUID = -1790043820664125603L;
		
		public DuplicateBondException(Exception e) {
			initCause(e);
		}
		
	}
	
	public static class CircularBondException extends RuntimeException {

		private static final long serialVersionUID = 2340236802937419130L;

		public CircularBondException(Exception e) {
			initCause(e);
		}
		
	}

	private final static Logger LOG = Logger.getLogger(MoleculeCreator.class);
	
	private static final String CLICKABLE_HTML_SUFFIX = ".clickable";
	private static final String ANNOTATED_SVG_SUFFIX = ".annotated";
	
	private MoleculeCreatorParameters parameters;

	//private Map<Junction, CMLAtom> junctionToAtomMap = new HashMap<Junction, CMLAtom>();
	//private Map<JoinPoint, Junction> joinPointToJunctionMap = new HashMap<JoinPoint, Junction>();
	private Map<JoinPoint, CMLAtom> joinPointToAtomMap = new LinkedHashMap<JoinPoint, CMLAtom>();
	private Set<Joinable> joinableSet = new LinkedHashSet<Joinable>();
	private HashMap<CMLReaction, Real2Range> positionsOfReactantsOfReactions = new LinkedHashMap<CMLReaction, Real2Range>();
	private HashMap<CMLReaction, Real2Range> positionsOfProductsOfReactions = new LinkedHashMap<CMLReaction, Real2Range>();
	
	double leftmostPoint = Double.MAX_VALUE;
	double rightmostPoint = Double.MIN_VALUE;

	private ChemistryBuilder chemistryBuilder;
	
	//public static HashMap<String, String> groupsDictionary = new HashMap<String, String>();
	private static GroupList groupList;
	
	private int i;

	Map<Real2Range, List<Joinable>> labelLocations;
	LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations;
	Map<Real2Range, Set<Junction>> arrowLocations;

	private List<CMLReaction> reactions;

	private SVGContainerNew inputCopy;
	
	/*static {
		BufferedReader dict = new BufferedReader(new InputStreamReader(MoleculeCreator.class.getResourceAsStream("/org/xmlcml/xhtml2stm/visitor/chem/groupsdictionary.tab")));
		String l = null;
		try {
			while ((l = dict.readLine()) != null) {
				String[] splitByTab = l.split("\t");
				String[] splitByComma = splitByTab[0].split(",");
				for (String group : splitByComma) {
					groupsDictionary.put(group, splitByTab[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
	
//	public MoleculeCreator(SVGContainer container, long timeout, MoleculeCreatorParameters parameters) {
//		chemistryBuilder = new ChemistryBuilder(container, timeout, parameters);
//		inputCopy = new SVGContainer(container.getFile(), (Element) container.getElement().copy());
//		this.parameters = parameters;
//	}
//	
//	public MoleculeCreator(SVGContainer container, MoleculeCreatorParameters parameters) {
//		chemistryBuilder = new ChemistryBuilder(container, parameters);
//		inputCopy = new SVGContainer(container.getFile(), (Element) container.getElement().copy());
//		this.parameters = parameters;
//	}
//	
//	public MoleculeCreator(SVGContainer container, long timeout) {
//		parameters = new MoleculeCreatorParameters();
//		chemistryBuilder = new ChemistryBuilder(container, timeout, parameters);
//		inputCopy = new SVGContainer(container.getFile(), (Element) container.getElement().copy());
//	}
//	
//	public MoleculeCreator(SVGContainer container) {
//		parameters = new MoleculeCreatorParameters();
//		chemistryBuilder = new ChemistryBuilder(container, parameters);
//		inputCopy = new SVGContainer(container.getFile(), (Element) container.getElement().copy());
//	}
	
	public MoleculeCreator(SVGContainerNew container, long timeout, MoleculeCreatorParameters parameters) {
		chemistryBuilder = new ChemistryBuilder(container, timeout, parameters);
		inputCopy = new SVGContainerNew(container.getFile(), (SVGElement) container.getElement().copy());
		this.parameters = parameters;
	}
	
	public MoleculeCreator(SVGContainerNew container, MoleculeCreatorParameters parameters) {
		chemistryBuilder = new ChemistryBuilder(container, parameters);
		inputCopy = new SVGContainerNew(container.getFile(), (SVGElement) container.getElement().copy());
		this.parameters = parameters;
	}
	
	public MoleculeCreator(SVGContainerNew container, long timeout) {
		parameters = new MoleculeCreatorParameters();
		chemistryBuilder = new ChemistryBuilder(container, timeout, parameters);
		inputCopy = new SVGContainerNew(container.getFile(), (SVGElement) container.getElement().copy());
	}
	
	public MoleculeCreator(SVGContainerNew container) {
		parameters = new MoleculeCreatorParameters();
		chemistryBuilder = new ChemistryBuilder(container, parameters);
		inputCopy = new SVGContainerNew(container.getFile(), (SVGElement) container.getElement().copy());
	}
	
	public MoleculeCreator(ChemistryBuilder builder) {
		chemistryBuilder = builder;
		parameters = new MoleculeCreatorParameters();
	}
	
	static {
		try {
			groupList = new GroupList(MoleculeCreator.class.getResource("groups.cml").openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ChemistryBuilder getChemistryBuilder() {
		return chemistryBuilder;
	}
	
	/**
	 * @return
	 * @deprecated Use getMolecules()
	 */
	@Deprecated
	public CMLMolecule createMolecule() {
		/*List<Word> wordList = geometryBuilder.getWordList();
		for (Word word : wordList) {
			LOG.debug(word.getXY().format(2)+"; "+word.getValue());
		}*/
		chemistryBuilder.createHigherPrimitives();
		arrowLocations = new LinkedHashMap<Real2Range, Set<Junction>>();
		labelLocations = new LinkedHashMap<Real2Range, List<Joinable>>();
		moleculeLocations = LinkedHashBasedTable.create();
		CMLMolecule molecule = createMolecule(chemistryBuilder.getHigherPrimitives().getJunctionList(), true);
		Real2Range location = new Real2Range();
		for (Junction junction : chemistryBuilder.getHigherPrimitives().getJunctionList()) {
			List<Joinable> joinables = junction.getJoinables();
			location.plusEquals(getBoundingBox(joinables, true));
		}
		moleculeLocations.put(location, location, molecule);
		molecule.addAttribute(new CMLAttribute("x2", Double.toString(location.getXRange().getMidPoint())));
		molecule.addAttribute(new CMLAttribute("y2", Double.toString(location.getYRange().getMidPoint())));
		attachLabelsToAtoms(moleculeLocations, chemistryBuilder.getAtomLabels());
		debugMolecule(location, molecule);
		return molecule;
	}
	
	/**
	 * @return
	 * @deprecated Use getMolecules()
	 */
	@Deprecated
	public Collection<CMLMolecule> createMolecules() {
		LOG.trace("Looking for molecules");
		chemistryBuilder.createHigherPrimitives();
		UnionFind<Junction> bits = findConnectedBits(chemistryBuilder.getHigherPrimitives().getJunctionList());
		arrowLocations = new LinkedHashMap<Real2Range, Set<Junction>>();
		labelLocations = new LinkedHashMap<Real2Range, List<Joinable>>();
		moleculeLocations = LinkedHashBasedTable.create();
		findLocationsOfBits(bits, arrowLocations, labelLocations, moleculeLocations);
		attachLabelsToAtoms(moleculeLocations, chemistryBuilder.getAtomLabels());
		stitchLabelsTogether(labelLocations);
		addLabelsToMolecules(labelLocations, moleculeLocations);
		for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> molecule : moleculeLocations.columnMap().entrySet()) {
			getMolecule(molecule).addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
			getMolecule(molecule).addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
		}
		debugMolecules(moleculeLocations);
		return moleculeLocations.values();
	}

	private UnionFind<Junction> findConnectedBits(Collection<Junction> junctions) {
		UnionFind<Junction> bits = UnionFind.create(junctions);
		for (Junction j : junctions) {
			for (JoinPoint point : j.getJoinPoints()) {
				for (Junction k : junctions) {
					if (k.getJoinables().contains(point.getJoinable())) {
						bits.union(j, k);
					}
				}
			}
		}
		return bits;
	}
	
	public Collection<CMLReaction> getReactions() {
		if (reactions == null) {
			getReactionsAndMolecules();
		}
		for (CMLReaction reaction : reactions) {
			if (reaction.getParent() != null) {
				reaction.getParent().removeChild(reaction);
			}
		}
		return reactions;
	}
	
	public Collection<CMLMolecule> getMolecules() {
		if (moleculeLocations == null) {
			getReactionsAndMolecules();
		}
		for (CMLMolecule molecule : moleculeLocations.values()) {
			if (molecule.getParent() != null) {
				molecule.getParent().removeChild(molecule);
			}
		}
		return moleculeLocations.values();
	}

	public CMLPage getReactionsAndMolecules() {
		LOG.trace("Looking for reactions and molecules");
		chemistryBuilder.createHigherPrimitives();
		UnionFind<Junction> bits = findConnectedBits(chemistryBuilder.getHigherPrimitives().getJunctionList());
		arrowLocations = new LinkedHashMap<Real2Range, Set<Junction>>();
		labelLocations = new LinkedHashMap<Real2Range, List<Joinable>>();
		moleculeLocations = LinkedHashBasedTable.create();
		findLocationsOfBits(bits, arrowLocations, labelLocations, moleculeLocations);
		if (arrowLocations.size() == 0) {
			LOG.debug("No reactions found");
		}
		parameters.setStandardMoleculeSizeFromMolecules(moleculeLocations.columnKeySet());
		attachLabelsToAtoms(moleculeLocations, chemistryBuilder.getAtomLabels());
		stitchLabelsTogether(labelLocations);
		Map<Real2Range, List<Joinable>> labelsLeft = addLabelsToMolecules(labelLocations, moleculeLocations);
		findMoleculeGroups(moleculeLocations);
		reactions = createReactionsAndAddMolecules(arrowLocations, moleculeLocations);
		labelsLeft = addLabelsToReactions(reactions, labelsLeft, arrowLocations);
		debugMolecules(moleculeLocations);
		int molecules = moleculeLocations.rowKeySet().size();
		LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> newMoleculeLocations = createEmptyMoleculesFromLabels(labelsLeft);
		addMoleculesToReactions(arrowLocations, newMoleculeLocations, reactions, molecules);
		debugMolecules(newMoleculeLocations);
		moleculeLocations.putAll(newMoleculeLocations);
		return makeCollection();
	}

	private CMLPage makeCollection() {
		CMLPage page = new CMLPage();
		CMLMoleculeList moleculeList = new CMLMoleculeList();
		for (CMLMolecule molecule : getMolecules()) {
			moleculeList.addMolecule(molecule);
		}
		page.appendChild(moleculeList);
		CMLReactionList reactionList = new CMLReactionList();
		for (CMLReaction reaction : getReactions()) {
			reactionList.addReaction(reaction);
		}
		page.appendChild(reactionList);
		return page;
	}
	
	/**
	 * @return
	 * @deprecated Use getReactions()
	 */
	@Deprecated
	public Collection<CMLReaction> createReactions() {
		LOG.trace("Looking for reactions");
		chemistryBuilder.createHigherPrimitives();
		UnionFind<Junction> bits = findConnectedBits(chemistryBuilder.getHigherPrimitives().getJunctionList());
		arrowLocations = new LinkedHashMap<Real2Range, Set<Junction>>();
		labelLocations = new LinkedHashMap<Real2Range, List<Joinable>>();
		moleculeLocations = LinkedHashBasedTable.create();
		findLocationsOfBits(bits, arrowLocations, labelLocations, moleculeLocations);
		if (arrowLocations.size() == 0) {
			return new ArrayList<CMLReaction>();
		}
		attachLabelsToAtoms(moleculeLocations, chemistryBuilder.getAtomLabels());
		stitchLabelsTogether(labelLocations);
		Map<Real2Range, List<Joinable>> labelsLeft = addLabelsToMolecules(labelLocations, moleculeLocations);
		List<CMLReaction> results = createReactionsAndAddMolecules(arrowLocations, moleculeLocations);
		labelsLeft = addLabelsToReactions(results, labelsLeft, arrowLocations);
		debugMolecules(moleculeLocations);
		int molecules = moleculeLocations.size();
		LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> newMoleculeLocations = createEmptyMoleculesFromLabels(labelsLeft);
		addMoleculesToReactions(arrowLocations, newMoleculeLocations, results, molecules);
		debugMolecules(newMoleculeLocations);
		moleculeLocations.putAll(newMoleculeLocations);
		reactions = results;
		return results;
	}

	private void debugMolecules(LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations) {
		for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol : moleculeLocations.columnMap().entrySet()){
			Real2Range location = mol.getKey();
			debugMolecule(location, getMolecule(mol));
		}
	}

	private void debugMolecule(Real2Range location, CMLMolecule molecule) {
		LOG.trace("Molecule " + location + " " + molecule.getAtomCount() + " " + (molecule.getAtomCount() == 0 ? "" : new SMILESTool((CMLMolecule) molecule.copy()).write()));
	}

	private void attachLabelsToAtoms(LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations, Map<Real2Range, Integer> atomLabels) {
		if (atomLabels.size() == 0) {
			return;
		}
		
		List<CMLAtom> atoms = new ArrayList<CMLAtom>();
		for (CMLMolecule mol : moleculeLocations.values()) {
			for (CMLAtom a : mol.getAtoms()) {
				if (!"H".equals(a.getElementType())) {
					atoms.add(a);
				}
			}
		}
		double[][] distances = new double[atomLabels.size()][atoms.size()];
		for (int a = 0; a < atoms.size(); a++) {
			CMLAtom atom = atoms.get(a);
			int l = 0;
			for (Real2Range label : atomLabels.keySet()) {
				distances[l++][a] = label.distanceOutside(new Real2(atom.getXY2().x / parameters.getBondLengthScale(), -atom.getXY2().y / parameters.getBondLengthScale())).getLength();
			}
		}
		HungarianAlgorithm h = new HungarianAlgorithm(distances);
		int[] results = h.execute();
		int i = 0;
		for (Integer number : atomLabels.values()) {
			CMLLabel label = new CMLLabel();
			label.setCMLValue(number.toString());
			if (results[i] != -1) {
				//System.out.println(i + atoms.get(results[i]).getElementType());
				atoms.get(results[i++]).addLabel(label);
			}
		}
	}

	private void stitchLabelsTogether(Map<Real2Range, List<Joinable>> labelLocations) {
		UnionFind<Entry<Real2Range, List<Joinable>>> newLabels = UnionFind.create(labelLocations.entrySet());
		for (Entry<Real2Range, List<Joinable>> label1 : labelLocations.entrySet()) {
			for (Entry<Real2Range, List<Joinable>> label2 : labelLocations.entrySet()) {
				if (label1 != label2) {
					Real2Range label1Box = getBoundingBox(label1.getValue(), false);
					Real2Range label2Box = getBoundingBox(label2.getValue(), false);
					if (!label2Box.isValid() || !label1Box.isValid()) {
						continue;
					}
					double meanSize = (label1.getValue().get(0).getSVGElement().getFontSize() + label2.getValue().get(0).getSVGElement().getFontSize()) / 2;
					boolean xConstraint = label1.getKey().getXRange().getRangeExtendedBy(parameters.getMaximumTabDistance() / 2, parameters.getMaximumTabDistance() / 2).intersectsWith(label2.getKey().getXRange().getRangeExtendedBy(parameters.getMaximumTabDistance() / 2, parameters.getMaximumTabDistance() / 2));
					boolean horizontal = xConstraint && (((label2Box.getYMin() - label1Box.getYMin()) / meanSize > -parameters.getTextCoordinateTolerance() && (label1Box.getYMax() - label2Box.getYMax()) / meanSize > -parameters.getTextCoordinateTolerance()) || ((label1Box.getYMin() - label2Box.getYMin()) / meanSize > -parameters.getTextCoordinateTolerance() && (label2Box.getYMax() - label1Box.getYMax()) / meanSize > -parameters.getTextCoordinateTolerance()));
					if (horizontal) {
						newLabels.union(label1, label2);
					}
				}
			}
		}
		labelLocations.clear();
		convertArrowsGroupedByUnionFindIntoArrowGroups(labelLocations, newLabels);
		newLabels = UnionFind.create(labelLocations.entrySet());
		for (Entry<Real2Range, List<Joinable>> label1 : labelLocations.entrySet()) {
			for (Entry<Real2Range, List<Joinable>> label2 : labelLocations.entrySet()) {
				if (label1 != label2) {
					Real2Range label1Box = getBoundingBox(label1.getValue(), false);
					Real2Range label2Box = getBoundingBox(label2.getValue(), false);
					if (!label2Box.isValid() || !label1Box.isValid()) {
						continue;
					}
					boolean xConstraint = Math.abs(label1.getKey().getXRange().getMidPoint() - label2.getKey().getXRange().getMidPoint()) < parameters.getLabelJoiningMaximumXJitter() || Math.abs(label1.getKey().getXRange().getMin() - label2.getKey().getXRange().getMin()) < parameters.getLabelJoiningMaximumXJitter();
					boolean vertical = xConstraint && ((label1.getKey().getYMin() - label2.getKey().getYMax() < parameters.getMaximumSpacingBetweenLabelLines() && label1.getKey().getYMin() - label2.getKey().getYMax() > -parameters.getMaximumOverlapBetweenLabelLines()) || (label2.getKey().getYMin() - label1.getKey().getYMax() < parameters.getMaximumSpacingBetweenLabelLines() && label2.getKey().getYMin() - label1.getKey().getYMax() > -parameters.getMaximumOverlapBetweenLabelLines()));
					if (vertical) {
						newLabels.union(label1, label2);
					}
				}
			}
		}
		labelLocations.clear();
		convertArrowsGroupedByUnionFindIntoArrowGroups(labelLocations,newLabels);
	}

	private void convertArrowsGroupedByUnionFindIntoArrowGroups(Map<Real2Range, List<Joinable>> labelLocations, UnionFind<Entry<Real2Range, List<Joinable>>> newLabels) {
		for (Set<Entry<Real2Range, List<Joinable>>> group : newLabels.snapshot()) {
			List<Entry<Real2Range, List<Joinable>>> groupList = new ArrayList<Entry<Real2Range, List<Joinable>>>(group);
			Collections.sort(groupList, new Comparator<Entry<Real2Range, List<Joinable>>>(){
				public int compare(Entry<Real2Range, List<Joinable>> o1, Entry<Real2Range, List<Joinable>> o2) {
					return (o2.getKey().getYMin() > o1.getKey().getYMin() ? -1 : 1);
				}}
			);
			//String text = "";
			List<Joinable> newList = new ArrayList<Joinable>();
			Real2Range boundingBox = new Real2Range();
			for (Entry<Real2Range, List<Joinable>> line : groupList) {
				//text += (text.equals("") ? "" : System.lineSeparator()) + line.getValue();
				newList.addAll(line.getValue());
				boundingBox.add(line.getKey().getCorners()[0]);
				boundingBox.add(line.getKey().getCorners()[1]);
			}
			labelLocations.put(boundingBox, newList);
		}
	}

	private void findLocationsOfBits(UnionFind<Junction> bits, Map<Real2Range, Set<Junction>> arrowLocations, Map<Real2Range, List<Joinable>> labelLocations, LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations) {
		for (Set<Junction> bit : bits.snapshot()) {
			Real2Range location = new Real2Range();
			for (Junction junction : bit) {
				List<Joinable> joinables = junction.getJoinables();
				location.plusEquals(getBoundingBox(joinables, true));
			}
			if (bit.size() == 1) {
				List<Joinable> joinables = Iterables.getOnlyElement(bit).getJoinables();
				boolean allTexts = true;
				for (Joinable j : joinables) {
					if (!(j instanceof JoinableText)) {
						allTexts = false;
						break;
					}
				}
				if (allTexts) {
					labelLocations.put(location, joinables);
					//labels.add(bit);
					//CMLMolecule mol = createMolecule(bit, false);
					//if (mol == null) {
						//if (Iterables.getOnlyElement(bit).getText() != null) {
							//labelLocations.put(location, Iterables.getOnlyElement(bit).getText());
						//}
					//} else {
						//moleculeLocations.put(location, mol);
					//}
				} else {
					CMLMolecule mol = createMolecule(bit, true);
					if (mol != null) {
						moleculeLocations.put(location, location, mol);
					}
				}
			} else if (bit.size() == 3) {
				Set<Joinable> joinablesSet = new LinkedHashSet<Joinable>();
				for (Junction j : bit) {
					joinablesSet.addAll(j.getJoinables());
				}
				List<Joinable> joinablesList = new ArrayList<Joinable>(joinablesSet);
				if (joinablesList.size() == 2 && ((joinablesList.get(0) instanceof SingleBond && joinablesList.get(1) instanceof WedgeBond) || (joinablesList.get(0) instanceof WedgeBond && joinablesList.get(1) instanceof SingleBond))) {
					//arrows.add(bit);
					arrowLocations.put(location, bit);
				} else {
					CMLMolecule mol = createMolecule(bit, true);
					if (mol != null) {
						moleculeLocations.put(location, location, mol);
					}
				}
			} else if (bit.size() == 5) {
				Set<Joinable> joinablesSet = new LinkedHashSet<Joinable>();
				Junction middle = null;
				for (Junction j : bit) {
					joinablesSet.addAll(j.getJoinables());
					try {
						if (j.getJoinables().get(0) instanceof WedgeBond && j.getJoinables().get(1) instanceof WedgeBond) {
							middle = j;
						}
					} catch (IndexOutOfBoundsException e) {
						
					}
				}
				int singleBonds = 0;
				int wedgeBonds = 0;
				for (Joinable joinable : joinablesSet) {
					if (joinable instanceof SingleBond) {
						singleBonds++;
					} else if (joinable instanceof WedgeBond) {
						wedgeBonds++;
					}
				}
				List<Joinable> joinablesList = new ArrayList<Joinable>(joinablesSet);
				if (joinablesList.size() == 4 && singleBonds == 2 && wedgeBonds == 2 && middle != null) {
					Joinable arrow1 = middle.getJoinables().get(0);
					Joinable arrow2 = middle.getJoinables().get(1);
					Junction j1 = new Junction(Sets.intersection(new HashSet<JoinPoint>(middle.getJoinPoints()), new HashSet<JoinPoint>(arrow1.getJoinPoints())));
					Junction j2 = new Junction(Sets.intersection(new HashSet<JoinPoint>(middle.getJoinPoints()), new HashSet<JoinPoint>(arrow2.getJoinPoints())));
					Set<Junction> newJunctions = new LinkedHashSet<Junction>(bit);
					newJunctions.remove(middle);
					newJunctions.add(j1);
					newJunctions.add(j2);
					UnionFind<Junction> found = findConnectedBits(newJunctions);
					for (Set<Junction> arrow : found.snapshot()) {
						location = new Real2Range();
						for (Junction junction : arrow) {
							List<Joinable> joinables = junction.getJoinables();
							location.plusEquals(getBoundingBox(joinables, true));
						}
						arrowLocations.put(location, arrow);
					}
				} else {
					CMLMolecule mol = createMolecule(bit, true);
					if (mol != null) {
						moleculeLocations.put(location, location, mol);
					}
				}
			} else {
				//molecules.add(bit);
				CMLMolecule mol = createMolecule(bit, true);
				if (mol != null) {
					moleculeLocations.put(location, location, mol);
				}
			}
		}
	}

	private Real2Range getBoundingBox(List<Joinable> joinables, boolean includeSuperscriptAndSubscriptJoinableTexts) {
		Real2Range location = new Real2Range();
		LargestFontFinderForJoinables finder = null;
		if (!includeSuperscriptAndSubscriptJoinableTexts) {
			finder = new LargestFontFinderForJoinables(joinables, false, parameters);
		}
		for (Joinable joinable : joinables) {
			if (joinable.getJoinPoints().size() == 2) {
				location.add(joinable.getJoinPoints().get(0).getPoint());
				location.add(joinable.getJoinPoints().get(1).getPoint());
			} else if (includeSuperscriptAndSubscriptJoinableTexts || !(joinable instanceof JoinableText) || ((JoinableText) joinable).getSVGElement().getFontSize() / finder.largestFontSize > parameters.getAllowedFontSizeVariation()) {
				location.add(joinable.getJoinPoints().get(0).getPoint().plus(new Real2(joinable.getJoinPoints().get(0).getRadius(), joinable.getJoinPoints().get(0).getRadius())));
				location.add(joinable.getJoinPoints().get(0).getPoint().subtract(new Real2(joinable.getJoinPoints().get(0).getRadius(), joinable.getJoinPoints().get(0).getRadius())));
			}
		}
		return location;
	}
	
	private Map<Real2Range, List<Joinable>> addLabelsToReactions(List<CMLReaction> results, Map<Real2Range, List<Joinable>> labelsLeftAtFirst, Map<Real2Range, Set<Junction>> arrowLocations) {
		Map<Real2Range, List<Joinable>> labelsLeft = new LinkedHashMap<Real2Range, List<Joinable>>();
		labelsLeft.putAll(labelsLeftAtFirst);
		for (Entry<Real2Range, List<Joinable>> label : labelsLeftAtFirst.entrySet()) {
			int index = 0;
			for (Entry<Real2Range, Set<Junction>> arrow : arrowLocations.entrySet()) {
				for (Junction j : arrow.getValue()) {
					List<Joinable> joinables = j.getJoinables();//arrow.getValue().iterator().next().getJoinables();
					if (joinables.size() == 2) {
						SingleBond arrowBond = (SingleBond) (joinables.get(0) instanceof SingleBond ? joinables.get(0) : joinables.get(1));
						Real2 point1 = arrowBond.getJoinPoints().get(0).getPoint();
						Real2 point2 = arrowBond.getJoinPoints().get(1).getPoint();
						Line2 arrowLine = new Line2(point1, point2);
						
						LineAndBoundingBoxCalculator calc = new LineAndBoundingBoxCalculator(arrowLine, label.getKey());
						
						//System.out.println(label.getValue() + " " + arrow.getKey().getXMin() + " " + calc.offEnd1 + " " + calc.offEnd2);
						//if (((Math.abs(label.getKey().getYMin() - arrow.getKey().getYMax()) < labelAndArrowGap) || (Math.abs(arrow.getKey().getYMin() - label.getKey().getYMax()) < labelAndArrowGap)) && Math.abs(label.getKey().getXMax() - arrow.getKey().getXMax() + label.getKey().getXMin() - arrow.getKey().getXMin()) < maximumReactionCaptionAndArrowJitter){
						boolean placementCheck = (calc.maximumDistanceFromEnd - calc.minimumDistanceFromEnd < parameters.getMaximumReactionCaptionAndArrowJitter() || calc.minimumDistanceFromEnd <= parameters.getMaximumForNoReactionCaptionJitterTest());
						if (calc.offEnd1 <= 2 && calc.offEnd2 <= 2 && calc.minimumDistanceFromBoxToLine < parameters.getLabelAndArrowGap() && placementCheck) {
							CMLConditionList conditions = (results.get(index).getConditionListElements().size() > 0 ? results.get(index).getConditionListElements().get(0) : new CMLConditionList());
							String[] lines = JoinableText.getMultiLineTextFromJoinableTexts(label.getValue(), parameters).split(System.getProperty("line.separator"));
							for (String line : lines) {
								CMLScalar scalar = new CMLScalar(line);
								conditions.appendChild(scalar);
								scalar.addAttribute(new CMLAttribute("x2", Double.toString(label.getKey().getXRange().getMidPoint())));
								scalar.addAttribute(new CMLAttribute("y2", Double.toString(label.getKey().getYRange().getMidPoint())));
								scalar.setDictRef("cml:enzyme");
							}
							results.get(index).addConditionList(conditions);
							labelsLeft.remove(label.getKey());
						}
						break;
					}
				}
				index++;
			}
		}
		return labelsLeft;
	}

	private List<CMLReaction> createReactionsAndAddMolecules(Map<Real2Range, Set<Junction>> arrowLocations, LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations) {
		List<CMLReaction> results = new ArrayList<CMLReaction>();
		Map<Real2Range, Set<Junction>> newArrowLocations = new LinkedHashMap<Real2Range, Set<Junction>>();
		
		UnionFind<Entry<Real2Range, Set<Junction>>> arrows = UnionFind.create(arrowLocations.entrySet());
		for (Entry<Real2Range, Set<Junction>> arrow1 : arrowLocations.entrySet()) {
			for (Entry<Real2Range, Set<Junction>> arrow2 : arrowLocations.entrySet()) {
				if (arrow1.getKey().getCentroid().getDistance(arrow2.getKey().getCentroid()) < parameters.getMaximumMultiArrowSpacing()) {
					arrows.union(arrow1, arrow2);
				}
			}
		}
		for (Set<Entry<Real2Range, Set<Junction>>> group : arrows.snapshot()) {
			Real2Range newRange = new Real2Range();
			Set<Junction> newJunctionSet = new LinkedHashSet<Junction>();
			for (Entry<Real2Range, Set<Junction>> arrow : group) {
				newRange.plusEquals(arrow.getKey());
				newJunctionSet.addAll(arrow.getValue());
			}
			newArrowLocations.put(newRange, newJunctionSet);
		}
		
		for (Entry<Real2Range, Set<Junction>> arrow : newArrowLocations.entrySet()) {
			CMLReaction reaction = new CMLReaction();
			reaction.addAttribute(new CMLAttribute("x2", Double.toString(arrow.getKey().getXRange().getMidPoint())));
			reaction.addAttribute(new CMLAttribute("y2", Double.toString(arrow.getKey().getYRange().getMidPoint())));
			results.add(reaction);
			if (arrow.getValue().size() > 3) {
				CMLLabel label = new CMLLabel();
				label.setCMLValue("Multi-step");
				reaction.addLabel(label);
			}
		}
		arrowLocations.clear();
		arrowLocations.putAll(newArrowLocations);
		addMoleculesToReactions(arrowLocations, moleculeLocations, results, 0);
		return results;
	}

	private void findMoleculeGroups(LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations) {
		Map<Real2Range, CMLMolecule> plusSigns = new LinkedHashMap<Real2Range, CMLMolecule>();
		Map<Real2Range, CMLMolecule> otherMolecules = new LinkedHashMap<Real2Range, CMLMolecule>();
		for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol : moleculeLocations.columnMap().entrySet()) {
			CMLMolecule molecule = getMolecule(mol);
			if (molecule.getAtomCount() == 4 && molecule.getAtom(0).getFormalCharge() == 1) {
				if ("C".equals(molecule.getAtom(0).getElementType()) && "H".equals(molecule.getAtom(1).getElementType()) && "H".equals(molecule.getAtom(2).getElementType()) && "H".equals(molecule.getAtom(3).getElementType())) {
					plusSigns.put(mol.getKey(), molecule);
				}
			} else {
				otherMolecules.put(mol.getKey(), molecule);
			}
		}
		UnionFind<Real2Range> unionFind = UnionFind.create(otherMolecules.keySet());
		for (Entry<Real2Range, CMLMolecule> plusSign : plusSigns.entrySet()) {
			Real2 plusPosition = plusSign.getKey().getCentroid();
			double smallestLeft = Double.MAX_VALUE;
			double smallestRight = Double.MAX_VALUE;
			Real2Range left = null;
			Real2Range right = null;
			for (Entry<Real2Range, CMLMolecule> molecule : otherMolecules.entrySet()) {
				Real2 corner1 = molecule.getKey().getCorners()[0];
				Real2 corner2 = molecule.getKey().getCorners()[1];
				if (corner1.getY() < plusPosition.getY() && corner2.getY() > plusPosition.getY()) {
					if (corner1.getX() < plusPosition.getX() && corner2.getX() < plusPosition.getX()) {
						double diff = plusPosition.getX() - corner2.getX();
						if (diff < smallestLeft) {
							smallestLeft = diff;
							left = molecule.getKey();
						}
					} else if (corner1.getX() > plusPosition.getX() && corner2.getX() > plusPosition.getX()) {
						double diff = corner1.getX() - plusPosition.getX();
						if (diff < smallestRight) {
							smallestRight = corner1.getX() - plusPosition.getX();
							right = molecule.getKey();
						}
					}
				}
			}
			if (left != null && right != null) {
				unionFind.union(left, right);
			}
		}
		LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> groups = LinkedHashBasedTable.create();
		for (Set<Real2Range> group : unionFind.snapshot()) {
			Real2Range overallRange = new Real2Range();
			for (Real2Range range : group) {
				overallRange.plusEquals(range);
				groups.put(overallRange, range, otherMolecules.get(range));
			}
		}
		moleculeLocations.clear();
		moleculeLocations.putAll(groups);
	}

	static CMLMolecule getMolecule(Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol) {
		return Iterables.getOnlyElement(mol.getValue().values());
	}

	private void addMoleculesToReactions(Map<Real2Range, Set<Junction>> arrowLocations, LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations, List<CMLReaction> reactions, int startID) {
		int count = 0;
		for (Entry<Real2Range, Set<Junction>> arrow : arrowLocations.entrySet()) {
			CMLReaction reaction = reactions.get(count++);
			int moleculeID = startID;
			for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol : moleculeLocations.rowMap().entrySet()) {
				if (mol.getKey().getXMin() < leftmostPoint) {
					leftmostPoint = mol.getKey().getXMin();
				}
				if (mol.getKey().getXMax() > rightmostPoint) {
					rightmostPoint = mol.getKey().getXMax();
				}
				//Junction junction = null;// = arrow.getValue().iterator().next();
				//List<Joinable> joinables = junction.getJoinables();
				//Set<Joinable> joinablesSet = new HashSet<Joinable>();
				for (Junction j : arrow.getValue()) {
					List<Joinable> joinables = j.getJoinables();//new ArrayList<Joinable>(joinablesSet);
					if (joinables.size() == 2) {
						//junction = j;
						//joinablesSet.addAll(j.getJoinables());
						SingleBond arrowBond = (SingleBond) (joinables.get(0) instanceof SingleBond ? joinables.get(0) : joinables.get(1));
						JoinPoint headPoint = (j.getJoinPoints().contains(arrowBond.getJoinPoints().get(0)) ? arrowBond.getJoinPoints().get(0) : arrowBond.getJoinPoints().get(1));
						JoinPoint shaftPoint = (j.getJoinPoints().contains(arrowBond.getJoinPoints().get(0)) ? arrowBond.getJoinPoints().get(1) : arrowBond.getJoinPoints().get(0));
						Line2 arrowLine = new Line2(shaftPoint.getPoint(), headPoint.getPoint());
						
						Real2Range range = mol.getKey();
						LineAndBoundingBoxCalculator distanceCalc = new LineAndBoundingBoxCalculator(arrowLine, range);
						Line2[] edges = LineAndBoundingBoxCalculator.getEdges(range);
						double lambda1 = (edges[0].getLength() > 0 ? edges[0].getLambda(edges[0].getIntersection(arrowLine)) : -1);
						double lambda2 = (edges[1].getLength() > 0 ? edges[1].getLambda(edges[1].getIntersection(arrowLine)) : -1);
						double lambda3 = (edges[2].getLength() > 0 ? edges[2].getLambda(edges[2].getIntersection(arrowLine)) : -1);
						double lambda4 = (edges[3].getLength() > 0 ? edges[3].getLambda(edges[3].getIntersection(arrowLine)) : -1);
						//TODO molecules covering both ends of an arrow should probably be ignored
						if ((lambda1 >= 0 && lambda1 <= 1) || (lambda2 >= 0 && lambda2 <= 1) || (lambda3 >= 0 && lambda3 <= 1) || (lambda4 >= 0 && lambda4 <= 1)) {
							double distToHead = headPoint.getPoint().getDistance(range.getCentroid());
							double distToShaft = shaftPoint.getPoint().getDistance(range.getCentroid());
							if (positionsOfReactantsOfReactions.get(reaction) == null && distToShaft < distToHead && distanceCalc.minimumDistanceFromBoxToLine < parameters.getArrowAndMoleculeGapOrOverlap()) {
								int subID = 0;
								for (Entry<Real2Range, CMLMolecule> molecule : mol.getValue().entrySet()) {
									CMLMolecule reactant = (CMLMolecule) molecule.getValue().copy();
									reactant.addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
									reactant.addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
									reactant.setId("m" + moleculeID + "_" + subID++);
									reaction.addReactant(reactant);
								}
								positionsOfReactantsOfReactions.put(reaction, mol.getKey());
								break;
							} else if (positionsOfProductsOfReactions.get(reaction) == null && distToHead < distToShaft && distanceCalc.minimumDistanceFromBoxToLine < parameters.getArrowAndMoleculeGapOrOverlap()) {
								int subID = 0;
								for (Entry<Real2Range, CMLMolecule> molecule : mol.getValue().entrySet()) {
									CMLMolecule product = (CMLMolecule) molecule.getValue().copy();
									product.addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
									product.addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
									product.setId("m" + moleculeID + "_" + subID++);
									reaction.addProduct(product);
								}
								positionsOfProductsOfReactions.put(reaction, mol.getKey());
								break;
							}
						}
					}
				}
				
				moleculeID++;
				
				/*//if (Math.abs(molecule.getKey().getXMax() - arrow.getKey().getXMin()) < arrowAndMoleculeGapOrOverlap && molecule.getKey().getYMax() > arrow.getKey().getYMax() && molecule.getKey().getYMin() < arrow.getKey().getYMin()) {
				if (calc.minDistanceToLine < arrowAndMoleculeGapOrOverlap && calc.offEnd1 > 2 && calc.offEnd2 == 0) {
					CMLMolecule reactant = (CMLMolecule) molecule.getValue().copy();
					reactant.addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
					reactant.addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
					reaction.addReactant(reactant);
					positionsOfReactantsOfReactions.put(reaction, molecule.getKey());
				//} else if (Math.abs(molecule.getKey().getXMin() - arrow.getKey().getXMax()) < arrowAndMoleculeGapOrOverlap && molecule.getKey().getYMax() > arrow.getKey().getYMax() && molecule.getKey().getYMin() < arrow.getKey().getYMin()) {
				} else if (calc.minDistanceToLine < arrowAndMoleculeGapOrOverlap && calc.offEnd1 == 0 && calc.offEnd2 > 2) {
					CMLMolecule product = (CMLMolecule) molecule.getValue().copy();
					product.addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
					product.addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
					reaction.addProduct(product);
					positionsOfProductsOfReactions.put(reaction, molecule.getKey());
				}*/
			}
		}
		handleMultiLineReactions(arrowLocations, moleculeLocations, reactions, leftmostPoint, rightmostPoint);
	}

	private void handleMultiLineReactions(Map<Real2Range, Set<Junction>> arrowLocations, LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> newMoleculeLocations, List<CMLReaction> reactions, double leftmostPoint, double rightmostPoint) {
		int i = 0;
		arrow: for (Set<Junction> arrow : arrowLocations.values()) {
			CMLReaction reaction = reactions.get(i++);
			for (Junction j : arrow) {
				List<Joinable> joinables = j.getJoinables();//new ArrayList<Joinable>(joinablesSet);
				if (joinables.size() == 2) {
					//junction = j;
					//joinablesSet.addAll(j.getJoinables());
					SingleBond arrowBond = (SingleBond) (joinables.get(0) instanceof SingleBond ? joinables.get(0) : joinables.get(1));
					JoinPoint headPoint = (j.getJoinPoints().contains(arrowBond.getJoinPoints().get(0)) ? arrowBond.getJoinPoints().get(0) : arrowBond.getJoinPoints().get(1));
					JoinPoint shaftPoint = (j.getJoinPoints().contains(arrowBond.getJoinPoints().get(0)) ? arrowBond.getJoinPoints().get(1) : arrowBond.getJoinPoints().get(0));
					Line2 arrowLine = new Line2(shaftPoint.getPoint(), headPoint.getPoint());
					if (!new SVGLine(arrowLine).isHorizontal(parameters.getFlatLineEpsilon()) || headPoint.getPoint().getX() < shaftPoint.getPoint().getX()) {
						continue arrow;
					}
				}
			}
			if (reaction.getReactantList() != null && reaction.getProductList() == null) {
				CMLMolecule product = null;
				int moleculeID = 0;
				for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol : newMoleculeLocations.rowMap().entrySet()) {
					if ((product == null || mol.getKey().getYRange().getMidPoint() < Double.parseDouble(product.getAttribute("y2").getValue())) && mol.getKey().getXMin() - leftmostPoint < parameters.getMaximumDistanceFromEdgeForReactionLine() && mol.getKey().getYMin() - positionsOfReactantsOfReactions.get(reaction).getYMax() > -parameters.getAllowedReactionLineOverlap()) {
						int subID = 0;
						for (Entry<Real2Range, CMLMolecule> molecule : mol.getValue().entrySet()) {
							product = (CMLMolecule) molecule.getValue().copy();
							product.addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
							product.addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
							product.setId("m" + moleculeID + "_" + subID++);
						}
					}
				}
				moleculeID++;
				if (product != null) {
					reaction.addProduct(product);
				}
			}
			if (reaction.getProductList() != null && reaction.getReactantList() == null) {
				CMLMolecule reactant = null;
				int moleculeID = 0;
				for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol : newMoleculeLocations.rowMap().entrySet()) {
					if ((reactant == null || mol.getKey().getYRange().getMidPoint() > Double.parseDouble(reactant.getAttribute("y2").getValue())) && rightmostPoint - mol.getKey().getXMax() < parameters.getMaximumDistanceFromEdgeForReactionLine() && mol.getKey().getYMax() < positionsOfProductsOfReactions.get(reaction).getYMin()) {
						int subID = 0;
						for (Entry<Real2Range, CMLMolecule> molecule : mol.getValue().entrySet()) {
							reactant = (CMLMolecule) molecule.getValue().copy();
							reactant.addAttribute(new CMLAttribute("x2", Double.toString(molecule.getKey().getXRange().getMidPoint())));
							reactant.addAttribute(new CMLAttribute("y2", Double.toString(molecule.getKey().getYRange().getMidPoint())));
							reactant.setId("m" + moleculeID + "_" + subID++);
						}
					}
				}
				moleculeID++;
				if (reactant != null) {
					reaction.addReactant(reactant);
				}
			}
		}
	}

	private Map<Real2Range, List<Joinable>> addLabelsToMolecules(Map<Real2Range, List<Joinable>> labelLocations, LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> moleculeLocations) {
		Map<Real2Range, List<Joinable>> labelsLeft = new LinkedHashMap<Real2Range, List<Joinable>>();
		labelsLeft.putAll(labelLocations);
		LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> newMoleculeLocations = LinkedHashBasedTable.create();
		for (Entry<Real2Range, Map<Real2Range, CMLMolecule>> mol : moleculeLocations.columnMap().entrySet()) {
			CMLMolecule molecule = getMolecule(mol);
			RealRange smallRange = getSmallerRangeFromLowerParts(mol.getKey(), molecule);
			double nearestLabelDistance = Double.MAX_VALUE;
			Entry<Real2Range, List<Joinable>> nearestLabel = null;
			for (Entry<Real2Range, List<Joinable>> label : labelLocations.entrySet()) {
				double gap = label.getKey().getYMin() - mol.getKey().getYMax();
				double jitter1 = Math.abs(label.getKey().getXMax() - mol.getKey().getXMax() + label.getKey().getXMin() - mol.getKey().getXMin());
				double jitter2 = Math.abs(label.getKey().getXMax() - smallRange.getMax() + label.getKey().getXMin() - smallRange.getMin());
				double jitter3 = Math.abs(label.getKey().getXMax() - (smallRange.getMax() + mol.getKey().getXMax()) / 2 + label.getKey().getXMin() - (smallRange.getMin() + mol.getKey().getXMin()) / 2);
				if (gap > -parameters.getMaximumMoleculeLabelOverlap() && gap < parameters.getLabelAndMoleculeGap() && gap < nearestLabelDistance && (jitter1 < parameters.getMaximumMoleculeCaptionAndMoleculeJitter() || jitter2 < parameters.getMaximumMoleculeCaptionAndMoleculeJitter() || jitter3 < parameters.getMaximumMoleculeCaptionAndMoleculeJitter())) {
					nearestLabelDistance = gap;
					nearestLabel = label;
				}		
			}
			if (nearestLabel != null) {
				Map<String, Map<String, String>> rDetails = getRGroupSubstitutionDetails(nearestLabel.getValue());
				if (rDetails != null) {
					for (Entry<String, Map<String, String>> rMolecule : rDetails.entrySet()) {
						Real2Range range = new Real2Range(mol.getKey());
						CMLMolecule newMolecule = (CMLMolecule) molecule.copy();
						new MoleculeBuilder(groupList).buildOnto(newMolecule, rMolecule.getValue());
						newMoleculeLocations.put(range, range, newMolecule);
						CMLLabel l = new CMLLabel();
						l.setCMLValue(rMolecule.getKey());
						l.addAttribute(new CMLAttribute("x2", Double.toString(nearestLabel.getKey().getXRange().getMidPoint())));
						l.addAttribute(new CMLAttribute("y2", Double.toString(nearestLabel.getKey().getYRange().getMidPoint())));
						newMolecule.addLabel(l);
					}
					labelsLeft.remove(nearestLabel.getKey());
				} else {
					Real2Range newRange = mol.getKey().plus(nearestLabel.getKey());
					newMoleculeLocations.put(newRange, newRange, molecule);
					labelsLeft.remove(nearestLabel.getKey());
					CMLLabel l = new CMLLabel();
					l.setCMLValue(JoinableText.getMultiLineTextFromJoinableTexts(nearestLabel.getValue(), parameters).replace(System.lineSeparator(), " "));
					l.addAttribute(new CMLAttribute("x2", Double.toString(nearestLabel.getKey().getXRange().getMidPoint())));
					l.addAttribute(new CMLAttribute("y2", Double.toString(nearestLabel.getKey().getYRange().getMidPoint())));
					molecule.addLabel(l);
				}
			} else {
				newMoleculeLocations.put(mol.getKey(), mol.getKey(), molecule);
			}
		}
		moleculeLocations.clear();
		moleculeLocations.putAll(newMoleculeLocations);
		return labelsLeft;
	}

	private RealRange getSmallerRangeFromLowerParts(Real2Range range, CMLMolecule molecule) {
		double minY = Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		for (CMLBond bond : molecule.getBonds()) {
			if (bond.getAtom(0).getElementType().equals("H") || bond.getAtom(1).getElementType().equals("H")) {
				continue;
			}
			double y1 = bond.getAtom(0).get2DPoint3().getArray()[1];
			double y2 = bond.getAtom(1).get2DPoint3().getArray()[1];
			if (y1 < minY) {
				minY = y1;
			}
			if (y1 > maxY) {
				maxY = y1;
			}
			if (y2 < minY) {
				minY = y2;
			}
			if (y2 > maxY) {
				maxY = y2;
			}
		}
		double minXLower = Double.MAX_VALUE;
		double maxXLower = Double.MIN_VALUE;
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		for (CMLBond bond : molecule.getBonds()) {
			if (bond.getAtom(0).getElementType().equals("H") || bond.getAtom(1).getElementType().equals("H")) {
				continue;
			}
			double y1 = bond.getAtom(0).get2DPoint3().getArray()[1];
			double y2 = bond.getAtom(1).get2DPoint3().getArray()[1];
			double x1 = bond.getAtom(0).get2DPoint3().getArray()[0];
			double x2 = bond.getAtom(1).get2DPoint3().getArray()[0];
			if (x1 < minX) {
				minX = x1;
			}
			if (x1 > maxX) {
				maxX = x1;
			}
			if (x2 < minX) {
				minX = x2;
			}
			if (x2 > maxX) {
				maxX = x2;
			}
			double threshold = minY + (maxY - minY) / 2;
			if (y1 < threshold && y2 < threshold) {
				if (x1 < minXLower) {
					minXLower = x1;
				}
				if (x1 > maxXLower) {
					maxXLower = x1;
				}
				if (x2 < minXLower) {
					minXLower = x2;
				}
				if (x2 > maxXLower) {
					maxXLower = x2;
				}
			}
		}
		return new RealRange(range.getXMin() + ((minXLower - minX) / (maxX - minX)) * range.getXRange().getRange(), range.getXMin() + ((maxXLower - minX) / (maxX - minX)) * range.getXRange().getRange());
	}

	private Map<String, Map<String, String>> getRGroupSubstitutionDetails(List<Joinable> value) {
		List<SVGText> texts = new ArrayList<SVGText>();
		for (Joinable t : value) {
			if (t instanceof JoinableText) {
				texts.add(((JoinableText) t).getSVGElement());
			}
		}
		/*ColumnMaps columns = new ColumnMaps(new TextStructurer(texts));
		columns.getTabs();
		columns.createSingleTabList();
		(new TextStructurer(texts)).getScriptedLineList().get(0).getScriptWordList();*/
		List<ScriptWord> words = new ArrayList<ScriptWord>();
		TextStructurer structurer = new TextStructurer(texts);
		List<ScriptLine> rows = structurer.getScriptedLineList();
		List<List<ScriptWord>> rowsAsWordLists = new ArrayList<List<ScriptWord>>();
		for (ScriptLine line : rows) {
			List<ScriptWord> wordList = line.getScriptWordList();
			rowsAsWordLists.add(wordList);
			words.addAll(wordList);
		}
		Collections.sort(words, new Comparator<ScriptWord>(){

			public int compare(ScriptWord o1, ScriptWord o2) {
				return Double.compare(o1.getLeftMargin(), o2.getLeftMargin());
			}
			
		});
		List<List<ScriptWord>> columns = new ArrayList<List<ScriptWord>>();
		List<ScriptWord> currentColumn = new ArrayList<ScriptWord>();
		columns.add(currentColumn);
		double lastXMin = words.get(0).getLeftMargin();
		for (ScriptWord word : words) {
			if (word.getLeftMargin() - lastXMin < parameters.getColumnFindingMaximumXJitter()) {
				currentColumn.add(word);
			} else {
				currentColumn = new ArrayList<ScriptWord>();
				columns.add(currentColumn);
				currentColumn.add(word);
				lastXMin = word.getLeftMargin();
			}
		}
		ScriptWord[][] table = new ScriptWord[rows.size()][columns.size()];
		int empty = 0;
		for (int rowIndex = 0; rowIndex < rowsAsWordLists.size(); rowIndex++) {
			int i = 0;
			List<ScriptWord> row = rowsAsWordLists.get(rowIndex);
			for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
				List<ScriptWord> column = columns.get(columnIndex);
				if (row.size() > i && column.contains(row.get(i))) {
					table[rowIndex][columnIndex] = row.get(i++);
				} else {
					empty++;
				}
			}
		}
		if (empty != 1 || table[0][0] != null) {
			Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();
			for (List<ScriptWord> row : rowsAsWordLists) {
				Map<String, String> processed = processRGroupSubstitutionString(row.subList(1, row.size()));
				if (processed.size() == 0) {
					return null;
				}
				result.put(row.get(0).toUnderscoreAndCaretString(), processed);
			}
			return result;
		}
		
		List<ScriptWord> rGroupIdentifiers = rowsAsWordLists.get(0);
		List<ScriptWord> moleculeIdentifiers = columns.get(0);
		
		Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();

		int i = 0;
		for (ScriptWord molecule : moleculeIdentifiers) {
			i++;
			Map<String, String> mapForMolecule = new LinkedHashMap<String, String>();
			result.put(molecule.toUnderscoreAndCaretString(), mapForMolecule);
			int j = 0;
			for (ScriptWord rGroup : rGroupIdentifiers) {
				j++;
				mapForMolecule.put(rGroup.toUnderscoreAndCaretString(), table[i][j].toUnderscoreAndCaretString());
			}
		}
		
		/*Map<String, String> rMol1 = new HashMap<String, String>();
		Map<String, String> rMol2 = new HashMap<String, String>();
		rMol1.put("R_1_", "CH_3_");
		rMol1.put("R_2_", "H");
		rMol2.put("R_1_", "H");
		rMol2.put("R_2_", "OH");
		table.put("1", rMol1);
		table.put("2", rMol2);*/
		
		return result;
	}

	private Map<String, String> processRGroupSubstitutionString(List<ScriptWord> string) {
		String currentString = "";
		List<String> rGroupNames = new ArrayList<String>();
		Map<String, String> result = new HashMap<String, String>();
		for (ScriptWord word : string) {
			String text = word.toUnderscoreAndCaretString();
			for (char c : text.toCharArray()) {
				if (c == '=') {
					rGroupNames.add(currentString);
					currentString = "";
				} else if (c == '(') {

				} else if (c == ')') {
					
				} else if (c == ',') {
					for (String s : rGroupNames) {
						result.put(s, currentString);
					}
					currentString = "";
					rGroupNames.clear();
				} else {
					currentString += c;
				}
			}
		}
		if (currentString != "") {
			for (String s : rGroupNames) {
				result.put(s, currentString);
			}
		}
		return result;
	}

	private LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> createEmptyMoleculesFromLabels(Map<Real2Range, List<Joinable>> labelsLeft) {
		LinkedHashBasedTable<Real2Range, Real2Range, CMLMolecule> newMolecules = LinkedHashBasedTable.create();
		for (Entry<Real2Range, List<Joinable>> label : labelsLeft.entrySet()) {
			CMLMolecule newMolecule = new CMLMolecule();
			CMLLabel l = new CMLLabel();
			l.setCMLValue(JoinableText.getMultiLineTextFromJoinableTexts(label.getValue(), parameters).replace(System.getProperty("line.separator"), " "));
			l.addAttribute(new CMLAttribute("x2", Double.toString(label.getKey().getXRange().getMidPoint())));
			l.addAttribute(new CMLAttribute("y2", Double.toString(label.getKey().getYRange().getMidPoint())));
			newMolecule.addLabel(l);
			newMolecules.put(label.getKey(), label.getKey(), newMolecule);
		}
		return newMolecules;
	}
	
	private CMLMolecule createMolecule(Collection<Junction> junctions, boolean detectRGroups) {
		try {
			CMLMolecule molecule = new CMLMolecule();
			joinableSet = new LinkedHashSet<Joinable>();
			joinPointToAtomMap = new LinkedHashMap<JoinPoint, CMLAtom>();
			createAndAddJunctionAtoms(junctions, molecule, detectRGroups);
			if (molecule.getAtomCount() == 0) {
				return null;
			}
			
			double shortestSingleBond = Double.MAX_VALUE;
			double longestSingleBond = Double.MIN_VALUE;
			for (Joinable joinable : joinableSet) {
				LOG.trace(joinable);
				if (joinable instanceof SingleBond) {
					CMLAtom[] atoms = getAtomsOfBondByJoinable(molecule, joinable);
					double length = atoms[0].getXY2().getDistance(atoms[1].getXY2());
					if (length > longestSingleBond) {
						longestSingleBond = length;
					}
					if (length < shortestSingleBond) {
						shortestSingleBond = length;
					}
				}
				try {
					addBond(molecule, joinable);
				} catch (DuplicateBondException e) {
					return null;
				} catch (CircularBondException e) {
					if (!(joinable instanceof SingleBond) || ((SVGLine) joinable.getSVGElement()).getLength() > parameters.getMaximumAbsoluteSeparation()) {
						return null;
					}
				}
			}
			
			if (longestSingleBond / shortestSingleBond > parameters.getMaximumRatioOfExtremeSingleBondLengths()) {
				return null;
			}

			new MoleculeBuilder(groupList).buildOnto(molecule);
			MoleculeTool tool = MoleculeTool.getOrCreateTool(molecule);
			tool.adjustHydrogenCountsToValency(HydrogenControl.ADD_TO_HYDROGEN_COUNT);
			StereochemistryTool stereoTool = new StereochemistryTool(molecule);
			/*for (CMLAtom a : molecule.getAtomArray().getAtomElements()) {
				if (a.getXY2() == null) {*/
					//a.setXY2(a.getLigandAtoms().get(0).getXY2().plus(new Real2(1, 1)));
					/*boolean hasHatch = false;
					boolean hasWedge = false;
					//boolean hasDouble = false;
					for (CMLBond bond : a.getLigandAtoms().get(0).getLigandBonds()) {
						if (bond.getBondStereo() != null && bond.getBondStereo().getValue().equals(CMLBondStereo.WEDGE)) {
							hasWedge = true;
						} else if (bond.getBondStereo() != null && bond.getBondStereo().getValue().equals(CMLBondStereo.HATCH)) {
							hasHatch = true;
						} else if (bond.getOrder().equals(CMLBond.DOUBLE_D)) {
							hasDouble = true;
						}
					}*/
					/*CMLAtom hatchAtom = null;
					CMLAtom wedgeAtom = null;
					List<CMLAtom> otherAtoms = new ArrayList<CMLAtom>();
					for (CMLBond bond : a.getLigandAtoms().get(0).getLigandBonds()) {
						Set<CMLAtom> end = new HashSet<CMLAtom>(bond.getAtoms());
						end.remove(a.getLigandAtoms().get(0));
						if (bond.getBondStereo() != null && bond.getBondStereo().getValue().equals(CMLBondStereo.WEDGE) && bond.getAtom(0) == a.getLigandAtoms().get(0)) {
							wedgeAtom = Iterables.getOnlyElement(end);
						} else if (bond.getBondStereo() != null && bond.getBondStereo().getValue().equals(CMLBondStereo.HATCH) && bond.getAtom(0) == a.getLigandAtoms().get(0)) {
							hatchAtom = Iterables.getOnlyElement(end);
						} else {
							otherAtoms.add(Iterables.getOnlyElement(end));
						}
					}*/
					/*int numHydrogens = 0;
					for (CMLAtom atom : a.getLigandAtoms().get(0).getLigandAtoms()) {
						if (atom.getElementType().equals("H")) {
							numHydrogens++;
						}
					}*/
					/*if ((hatchAtom == null || wedgeAtom == null) && StereochemistryTool.isChiralCentre(a.getLigandAtoms().get(0))) {//!hasDouble && numHydrogens < 2) {
						CMLBondStereo s = new CMLBondStereo();
						if (hatchAtom != null) {
							s.setXMLContent(CMLBondStereo.WEDGE);
							a.getLigandBonds().get(0).setBondStereo(s);
							//a.setXY2(hatchAtom.getXY2().getMidPoint(otherAtoms.get(0).getXY2()));
						} else if (wedgeAtom != null) {
							s.setXMLContent(CMLBondStereo.HATCH);
							a.getLigandBonds().get(0).setBondStereo(s);
							//a.setXY2(wedgeAtom.getXY2().getMidPoint(otherAtoms.get(0).getXY2()));
						} else {
							s.setXMLContent(CMLBondStereo.NONE);
							a.getLigandBonds().get(0).setBondStereo(s);
							//a.setXY2(wedgeAtom.getXY2().getMidPoint(otherAtoms.get(0).getXY2()));
						}
					} else {
						//a.setXY2(wedgeAtom.getXY2().getMidPoint(otherAtoms.get(0).getXY2()));
					}
				}
			}*/
			decideDirectionOfDifficultHatchBonds(stereoTool);
			stereoTool.addCalculatedAtomParityForPointyAtoms();
			for (CMLBond bond : molecule.getBonds()) {
				bond.clearBondStereo();
			}
			
			/*Joinable[] joinableArray = joinableSet.toArray(new Joinable[0]);
			for (Joinable joinable : joinableArray) {
				LOG.trace(joinable);
				List<CMLAtom> atomList = new ArrayList<CMLAtom>();
				for (Junction junction : joinable.getJunctionList()) {
					CMLAtom atom = junctionToAtomMap.get(junction.getID());
					atomList.add(atom);
					LOG.trace(atom.getId());
				}
				if (atomList.size() >= 2) {
					addBond(molecule, joinable, atomList);
				} else if (atomList.size() == 1) {
					//String atomId = "a" + (i + 1);
					//LOG.trace(atomId);
					//CMLAtom atom = new CMLAtom(atomId, ChemicalElement.getChemicalElement("C"));
					//Real2 junctionCoords = joinable.getJunctionList().get(0).getCoordinates();
					//Set<JoinPoint> joinPoints = new HashSet<JoinPoint>(joinable.getJoinPointList().getJoinPoints());
					//joinPoints.remove(joinable.getJunctionList().get(0).getJoinPoint());
					//Real2 coords = Iterables.getOnlyElement(joinPoints).getPoint();
					//coords = coords.multiplyBy(bondLengthScale);
					//coords.format(3);
					//atom.setXY2(coords);
					//molecule.addAtom(atom);
					//atomList.add(atom);
					//addBond(molecule, joinable, atomList);
					//i++;
				}
			}*/
			
			return molecule;
		} catch (Throwable t) {
			LOG.error("Problem creating molecule (" + t.getMessage() + ")");
			return null;
		}
	}

	private void decideDirectionOfDifficultHatchBonds(StereochemistryTool tool) {
		List<CMLAtom> centres = tool.getChiralAtoms();
		if (tool.getMolecule().getBondArray() != null) {
			for (CMLBond bond : tool.getMolecule().getBondArray().getBondElements()) {
				if (bond.getBondStereo() != null && bond.getBondStereo().getXMLContent().equals(CMLBondStereo.HATCH)) {
					if (!centres.contains(bond.getAtom(0)) && centres.contains(bond.getAtom(1))) {
						tool.getMolecule().removeChild(bond);
						tool.getMolecule().addBond(new CMLBond(bond.getAtom(1), bond.getAtom(0)));
					}
				}
			}
		}
	}

	private void addBond(CMLMolecule molecule, Joinable joinable) {
		CMLAtom[] atoms = getAtomsOfBondByJoinable(molecule, joinable);
		CMLBond bond = null;
		try {
			if (joinable instanceof DoubleBond) {
				bond = new CMLBond(atoms[0], atoms[1]);
				String bondOrder = CMLBond.DOUBLE_D;
				bond.setOrder(bondOrder);
			} else if (joinable instanceof TripleBond) {
				bond = new CMLBond(atoms[0], atoms[1]);
				String bondOrder = CMLBond.TRIPLE_T;
				bond.setOrder(bondOrder);
			} else if (joinable instanceof WedgeBond) {
				bond = new CMLBond(atoms[0], atoms[1]);
				String bondOrder = CMLBond.SINGLE_S;
				bond.setOrder(bondOrder);
				CMLBondStereo s = new CMLBondStereo();
				s.setXMLContent(CMLBondStereo.WEDGE);
				bond.setBondStereo(s);
				//bondOrder = CMLBond.WEDGE;
			} else if (joinable instanceof HatchedBond) {
				bond = new CMLBond(atoms[0], atoms[1]);
				String bondOrder = CMLBond.SINGLE_S;
				bond.setOrder(bondOrder);
				CMLBondStereo s = new CMLBondStereo();
				s.setXMLContent(CMLBondStereo.HATCH);
				bond.setBondStereo(s);
				//bondOrder = CMLBond.HATCH;
			} else if (joinable instanceof WigglyBond) {
				bond = new CMLBond(atoms[0], atoms[1]);
				String bondOrder = CMLBond.SINGLE_S;
				bond.setOrder(bondOrder);
				CMLBondStereo s = new CMLBondStereo();
				s.setXMLContent(CMLBondStereo.NONE);
				bond.setBondStereo(s);
			} else {
				bond = new CMLBond(atoms[0], atoms[1]);
				String bondOrder = CMLBond.SINGLE_S;
				bond.setOrder(bondOrder);
			}
		} catch (RuntimeException e) {
			LOG.error("Failed to add bond: "+e);
			throw new CircularBondException(e);
		}
		try {
			molecule.addBond(bond);
		} catch (RuntimeException e) {
			LOG.error("Failed to add bond: "+e);
			throw new DuplicateBondException(e);
		}
	}

	private CMLAtom[] getAtomsOfBondByJoinable(CMLMolecule molecule, Joinable joinable) {
		CMLAtom[] atoms = new CMLAtom[2];
		atoms[0] = joinPointToAtomMap.get(joinable.getJoinPoints().get(0));
		atoms[1] = joinPointToAtomMap.get(joinable.getJoinPoints().get(1));
		if (atoms[0] == null) {
			atoms[0] = createAtom(molecule, ChemicalElement.getChemicalElement("C"), joinable.getJoinPoints().get(0).getPoint());
			joinPointToAtomMap.put(joinable.getJoinPoints().get(0), atoms[0]);
		}
		if (atoms[1] == null) {
			atoms[1] = createAtom(molecule, ChemicalElement.getChemicalElement("C"), joinable.getJoinPoints().get(1).getPoint());
			joinPointToAtomMap.put(joinable.getJoinPoints().get(1), atoms[1]);
		}
		return atoms;
	}

	public void createAndAddJunctionAtoms(Collection<Junction> junctionList, CMLMolecule molecule, boolean detectRGroups) {
		for (Junction junction : junctionList) {
			LOG.trace("junctionId: " + junction.getID());
			String text = JoinableText.getSingleLineTextFromJoinableTexts(junction.getJoinables(), parameters);
			if (text == null) {
				text = "C";
			}
			//if (smiles == null) {
			ChemicalElement chemicalElement = ChemicalElement.getChemicalElement(text);
			if (chemicalElement == null && !detectRGroups) {
				return;
			} else if (chemicalElement == null) {
				chemicalElement = ChemicalElement.getChemicalElement("R");
			} 
			CMLAtom atom = createAtom(molecule, chemicalElement, junction.getCoordinates());
			if (junction.getCharge() != null) {
				atom.setFormalCharge(junction.getCharge());
			}
			if (chemicalElement == ChemicalElement.getChemicalElement("R")) {
				CMLLabel label = new CMLLabel();
			    CMLAttribute att = new CMLAttribute("name", MoleculeBuilder.JOIN);//.attributeFactory.createCMLAttribute("name", label);
			    //att.setCMLValue("join");
			    label.addAttribute(att);
			    label.setStringContent(text);//.replace(" ", ""));//TODO work out why the replacing was being done
			    atom.appendChild(label);
			}
			
			for (JoinPoint j : junction.getJoinPoints()) {
				//joinable.addJunction(junction);
				if (j.getJoinable().getJoinPoints().size() > 1) {
					joinableSet.add(j.getJoinable());
				}
				joinPointToAtomMap.put(j, atom);
			}
			//junctionToAtomMap.put(junction, atom);
			//} else {
				//Create group, embed CML or add all atoms and bonds to CML
			//}
		}
	}

	private CMLAtom createAtom(CMLMolecule molecule, ChemicalElement chemicalElement, Real2 coords) {
		String atomId = "a" + ++i;
		LOG.trace(atomId);
		CMLAtom atom = new CMLAtom(atomId, chemicalElement);
		coords = coords.multiplyBy(parameters.getBondLengthScale());
		coords.format(3);
		atom.setXY2(coords.getTransformed(new Transform2(new double[]{1, 0, 0, 0, -1, 0, 0, 0, 0})));
		molecule.addAtom(atom);
		return atom;
	}
	
	public void createAnnotatedVersionOfOutput(File outputDirectory) {
		getReactionsAndMolecules();
		if (reactions.size() == 0) {
			drawMolecules(outputDirectory.getAbsolutePath());
		} else {
			drawReactions(outputDirectory.getAbsolutePath());
		}
		ChemAnnotator.copyImageFilesFromDirectoryToDirectory(inputCopy.getFile().getParentFile(), outputDirectory);
	}
	
	public void createAnnotatedVersionOfInput(File outputDirectory) throws FileNotFoundException {
		ChemAnnotator.createClickableHTML(new File(outputDirectory, getOutputFileNameForInputAnnotation()), this);
		ChemAnnotator.copyImageFilesFromDirectoryToDirectory(inputCopy.getFile().getParentFile(), outputDirectory);
	}
	
	SVGContainerNew getInputCopy() {
		return inputCopy;
	}
	
	public void drawMolecules() {
		drawMolecules(new File("target/chem/andy.svg"));
	}
	
	public void drawMolecules(String outputDirectory) {
		drawMolecules(new File(outputDirectory, getOutputFileNameForOutputAnnotation()));
	}

	private String getOutputFileNameForOutputAnnotation() {
		return ((inputCopy != null ? inputCopy.getFile().getName() : "") + ANNOTATED_SVG_SUFFIX + ".svg");
	}
	
	private String getOutputFileNameForInputAnnotation() {
		return ((inputCopy != null ? inputCopy.getFile().getName() : "") + CLICKABLE_HTML_SUFFIX + ".html");
	}

	public void drawMolecules(File file) {
		SVGG svg = chemistryBuilder.drawPrimitivesJoinPointsAndJunctions();
		SVGG rects = new SVGG();
		svg.appendChild(rects);
		SVGG lines = new SVGG();
		svg.appendChild(lines);
		for (CMLMolecule molecule : moleculeLocations.values()) {
			try {
				drawLabelLines(lines, (CMLMolecule) molecule);
			} catch (NullPointerException e1) {

			}
		}
		if (moleculeLocations != null) {
			for (Real2Range location : moleculeLocations.columnKeySet()) {
				SVGRect rect = SVGRect.createFromReal2Range(location);
				rects.appendChild(rect);
			}
		}
		if (labelLocations != null) {
			for (Real2Range location : labelLocations.keySet()) {
				SVGRect rect = SVGRect.createFromReal2Range(location);
				rect.setStroke("blue");
				rects.appendChild(rect);
			}
		}
		SVGSVG.wrapAndWriteAsSVG(svg, file);
	}

	private void drawLabelLines(SVGG lines, CMLMolecule molecule) {
		Real2 moleculePoint = new Real2(Double.parseDouble(molecule.getAttribute("x2").getValue()), Double.parseDouble(molecule.getAttribute("y2").getValue()));
		for (CMLLabel label : molecule.getLabelElements()) {
			Real2 labelPoint = new Real2(Double.parseDouble(label.getAttribute("x2").getValue()), Double.parseDouble(label.getAttribute("y2").getValue()));
			SVGLine line = new SVGLine(moleculePoint, labelPoint);
			line.setStroke("blue");
			lines.appendChild(line);
		}
	}
	
	public void drawReactions() {
		drawReactions(new File("target/chem/andy.svg"));
	}
	
	public void drawReactions(String outputDirectory) {
		drawReactions(new File(outputDirectory, getOutputFileNameForOutputAnnotation()));
	}

	public void drawReactions(File file) {
		SVGG svg = chemistryBuilder.drawPrimitivesJoinPointsAndJunctions();
		SVGG rects = new SVGG();
		svg.appendChild(rects);
		SVGG lines = new SVGG();
		svg.appendChild(lines);
		for (CMLReaction reaction : reactions) {
			Real2 arrowPoint = new Real2(Double.parseDouble(reaction.getAttribute("x2").getValue()), Double.parseDouble(reaction.getAttribute("y2").getValue()));
			try {
				for (CMLReactant reactant : reaction.getReactantList().getReactantElements().getList()) {
					Real2 moleculePoint = new Real2(Double.parseDouble(reactant.getMolecule().getAttribute("x2").getValue()), Double.parseDouble(reactant.getMolecule().getAttribute("y2").getValue()));
					SVGLine line = new SVGLine(arrowPoint, moleculePoint);
					line.setStroke("red");
					lines.appendChild(line);
					for (CMLLabel label : reactant.getMolecule().getLabelElements()) {
						Real2 labelPoint = new Real2(Double.parseDouble(label.getAttribute("x2").getValue()), Double.parseDouble(label.getAttribute("y2").getValue()));
						line = new SVGLine((moleculePoint == null ? arrowPoint : moleculePoint), labelPoint);
						line.setStroke("blue");
						lines.appendChild(line);
					}
				}
			} catch (NullPointerException e) {
				
			}
			try {
				for (CMLProduct product : reaction.getProductList().getProductElements().getList()) {
					Real2 moleculePoint = new Real2(Double.parseDouble(product.getMolecule().getAttribute("x2").getValue()), Double.parseDouble(product.getMolecule().getAttribute("y2").getValue()));
					SVGLine line = new SVGLine(arrowPoint, moleculePoint);
					line.setStroke("green");
					lines.appendChild(line);
					for (CMLLabel label : product.getMolecule().getLabelElements()) {
						Real2 labelPoint = new Real2(Double.parseDouble(label.getAttribute("x2").getValue()), Double.parseDouble(label.getAttribute("y2").getValue()));
						line = new SVGLine((moleculePoint == null ? arrowPoint : moleculePoint), labelPoint);
						line.setStroke("blue");
						lines.appendChild(line);
					}
				}
			} catch (NullPointerException e) {
				
			}
			try {
				for (CMLScalar condition : reaction.getConditionListElements().get(0).getScalarElements()) {
					Real2 labelPoint = new Real2(Double.parseDouble(condition.getAttribute("x2").getValue()), Double.parseDouble(condition.getAttribute("y2").getValue()));
					SVGLine line = new SVGLine(arrowPoint, labelPoint);
					line.setStroke("blue");
					lines.appendChild(line);
				}
			} catch (NullPointerException e) {
				
			}
		}
		if (moleculeLocations != null) {
			for (Real2Range location : moleculeLocations.columnKeySet()) {
				SVGRect rect = SVGRect.createFromReal2Range(location);
				rects.appendChild(rect);
			}
		}
		if (labelLocations != null) {
			for (Real2Range location : labelLocations.keySet()) {
				SVGRect rect = SVGRect.createFromReal2Range(location);
				rect.setStroke("blue");
				rects.appendChild(rect);
			}
		}
		if (arrowLocations != null) {
			for (Real2Range location : arrowLocations.keySet()) {
				SVGRect rect = SVGRect.createFromReal2Range(location.getReal2RangeExtendedInX(0.1, 0.1).getReal2RangeExtendedInY(0.1, 0.1));
				rect.setStroke("yellow");
				rects.appendChild(rect);
			}
		}
		SVGSVG.wrapAndWriteAsSVG(svg, file);
	}
	
}