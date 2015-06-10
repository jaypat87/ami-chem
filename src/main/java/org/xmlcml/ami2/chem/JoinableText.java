package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.svg2xml.text.ScriptLine;
import org.xmlcml.svg2xml.text.ScriptWord;
import org.xmlcml.svg2xml.text.TextStructurer;

import com.google.common.collect.UnionFind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JoinableText extends Joinable {

	private final static Logger LOG = Logger.getLogger(JoinableText.class);

	private static final double TEXT_PRIORITY = 6.0;

	private SVGText svgText;

	public JoinableText(ChemistryBuilderParameters parameters, SVGText svgText) {
		this.svgText = svgText;
		addJoinPoints(parameters);
	}

	private void addJoinPoints(ChemistryBuilderParameters parameters) {
		JoinPoint joinPoint;
		svgText.applyTransformAttributeAndRemove();
		Real2 coord = svgText.getCentrePointOfFirstCharacter();
		joinPoint = new JoinPoint(coord.plus(new Real2(0, svgText.getHeightOfFirstCharacter() * parameters.getyAxisShiftForCentre())), svgText.getRadiusOfFirstCharacter() * parameters.getLargeRadiusExpansion());
		getJoinPoints().add(joinPoint);
	}
	
	public SVGText getSVGElement() {
		return svgText;
	}
	
	public double getPriority() {
		return TEXT_PRIORITY;
	}
	
	public String toString() {
		return svgText.toXML() + "\n ... " + Arrays.toString(getJoinPoints().toArray());
	}
	
	/*boolean joinableTextOnSameLine(List<Joinable> joinables, double textCoordinateTolerance, boolean returnTrueIfNoOthers) {
		SVGText text = getSVGElement();
		int otherJoinableTexts = 0;
		for (Joinable joinable : joinables) {
			if (joinable instanceof JoinableText && joinable != this) {
				otherJoinableTexts++;
				if (Math.abs(((SVGText) joinable.getSVGElement()).getY() - text.getY()) < textCoordinateTolerance) {
					return true;
				}
			}
		}
		return (otherJoinableTexts == 0 && returnTrueIfNoOthers);
	}*/
	
	static boolean anyTextsInSameString(SVGText text, List<SVGText> others, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts, boolean includeSpaces) {
		for (SVGText other : others) {
			if (other != text) {
				if (areAdjacentInSameString(text, other, parameters, checkSuperscriptsAndSubscripts, includeSpaces)) {
					return true;
				}
			}
		}
		return false;
	}
	
	static boolean anyTextsToRightInSameString(SVGText text, List<SVGText> others, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts) {
		for (SVGText other : others) {
			if (other != text) {
				if (secondAdjacentToRightOfFirstInSameString(text, other, parameters, checkSuperscriptsAndSubscripts)) {
					return true;
				}
			}
		}
		return false;
	}
	
	static boolean secondAdjacentToRightOfFirstInSameString(SVGText text, SVGText otherText, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts) {
		double maximumSpaceWidthInEnSpaces = parameters.getMaximumSpaceWidthInEnSpaces();
		double maximumOverlapInEnSpaces = parameters.getMaximumCharacterXRangeOverlapWhenAdjacent();
		return (text.getEnSpaceCount(otherText) != null && text.getEnSpaceCount(otherText) < maximumSpaceWidthInEnSpaces && text.getEnSpaceCount(otherText) > -maximumOverlapInEnSpaces && textsOnSameLine(text, otherText, parameters, checkSuperscriptsAndSubscripts));
	}
	
	static boolean areAdjacentInSameString(SVGText text, SVGText otherText, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts, boolean includeSpaces) {
		return (haveAlignedVerticals(text, otherText, parameters, includeSpaces) && textsOnSameLine(text, otherText, parameters, checkSuperscriptsAndSubscripts));
	}
	
	static boolean areAdjacentInSameString(JoinableText j, JoinableText otherJ, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts, boolean includeSpaces) {	
		return areAdjacentInSameString(j.getSVGElement(), otherJ.getSVGElement(), parameters, checkSuperscriptsAndSubscripts, includeSpaces);
	}
	
	static boolean haveAlignedVerticals(SVGText text, SVGText otherText, ChemistryBuilderParameters parameters, boolean includeSpaces) {
		double spaceWidthInEnSpaces = (includeSpaces ? parameters.getMaximumSpaceWidthInEnSpaces() : parameters.getMinimumSpaceWidthInEnSpaces());
		return (otherText.getEnSpaceCount(text) != null && otherText.getEnSpaceCount(text) < spaceWidthInEnSpaces && text.getEnSpaceCount(otherText) != null && text.getEnSpaceCount(otherText) < spaceWidthInEnSpaces);
	}

	static class AreInSameStringDetector {
		
		UnionFind<Joinable> texts;
		
		public AreInSameStringDetector(List<? extends Joinable> joinables, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts, boolean includeSpaces) {
			texts = UnionFind.create(joinables);
			for (Joinable j1 : joinables) {
				if (j1 instanceof JoinableText) {
					for (Joinable j2 : joinables) {
						if (j2 instanceof JoinableText && j1 != j2 && areAdjacentInSameString((JoinableText) j1, (JoinableText) j2, parameters, checkSuperscriptsAndSubscripts, includeSpaces)) {
							texts.union(j1, j2);
						}
					}
				}
			}
		}
		
		boolean areInSameString(JoinableText j, JoinableText otherJ) {
			return (texts.get(j).equals(texts.get(otherJ)));
		}
		
	}

	static boolean anyTextsOnSameLine(SVGText text, List<SVGText> others, boolean returnTrueIfNoOthers, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts) {
		int otherJoinableTexts = 0;
		for (SVGText other : others) {
			if (other != text) {
				otherJoinableTexts++;
				if (textsOnSameLine(text, other, parameters, checkSuperscriptsAndSubscripts)) {
					return true;
				}
			}
		}
		return (otherJoinableTexts == 0 && returnTrueIfNoOthers);
	}
	
	static boolean anyTextsOnSameLine(JoinableText text, List<? extends Joinable> others, boolean returnTrueIfNoOthers, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts) {
		int otherJoinableTexts = 0;
		for (Joinable other : others) {
			if (other != text && other instanceof JoinableText) {
				otherJoinableTexts++;
				if (textsOnSameLine(text.getSVGElement(), (SVGText) other.getSVGElement(), parameters, checkSuperscriptsAndSubscripts)) {
					return true;
				}
			}
		}
		return (otherJoinableTexts == 0 && returnTrueIfNoOthers);
	}

	static boolean textsOnSameLine(SVGText text, SVGText other, ChemistryBuilderParameters parameters, boolean checkSuperscriptsAndSubscripts) {
		double textCoordinateTolerance = parameters.getTextCoordinateTolerance();
		double subscriptAndSuperscriptOverlap = parameters.getSuperscriptAndSubscriptOverlap();
		double meanSize = (text.getFontSize() + other.getFontSize()) / 2;
		return (checkSuperscriptsAndSubscripts && !text.getFontSize().equals(other.getFontSize()) ? (text.getY() - text.getHeightOfFirstCharacter() - other.getY() < -subscriptAndSuperscriptOverlap && other.getY() - other.getHeightOfFirstCharacter() - text.getY() < -subscriptAndSuperscriptOverlap) : (Math.abs(other.getY() - text.getY()) / meanSize  < textCoordinateTolerance));
	}
	
	/*static class LargestFontFinder {
		double largestFontSize = 0;
		double yOfLargestFontSize = 0;
		List<JoinableText> joinableTextsLookedAt = new ArrayList<JoinableText>();
		List<SVGText> svgTextsLookedAt = new ArrayList<SVGText>();
		
		public LargestFontFinder(List<Joinable> list, boolean ignoreLoneHydrogens) {
			for (Joinable j : list) {
				if (j instanceof JoinableText) {
					SVGText text = (SVGText) j.getSVGElement();
					if (text.getText() != null) {
						if (text.getText().equals("H") && ignoreLoneHydrogens) {
							if (JoinableText.anyTextsOnSameLine((JoinableText) j, list, textCoordinateTolerance, true)) {
								joinableTextsLookedAt.add((JoinableText) j);
								svgTextsLookedAt.add(((JoinableText) j).getSVGElement());
								if (text.getFontSize() > largestFontSize) { 
									largestFontSize = text.getFontSize();
									yOfLargestFontSize = text.getY();
								}
							} 
						} else {
							joinableTextsLookedAt.add((JoinableText) j);
							svgTextsLookedAt.add(((JoinableText) j).getSVGElement());
							if (text.getFontSize() > largestFontSize && !text.getText().replace((char) 160, (char) 32).replace(" ", "").equals("")) { 
								largestFontSize = text.getFontSize();
								yOfLargestFontSize = text.getY();
							}
						}
					}
				}
			}
		}
	}*/
	
	private static abstract class LargestFontFinder<T, U extends T> {
		
		ChemistryBuilderParameters parameters;
		
		double largestFontSize = 0;
		double yOfLargestFontSize = 0;
		
		List<U> textsLookedAt = new ArrayList<U>();
		List<SVGText> svgTextsLookedAt = new ArrayList<SVGText>(); 
		
		public LargestFontFinder(List<T> list, boolean ignoreLoneHydrogens, ChemistryBuilderParameters parameters) {
			this.parameters = parameters;
			for (T j : list) {
				try {
					@SuppressWarnings("unchecked")
					U u = (U) j;
					SVGText text = getText(u);
					if (text.getText() != null) {
						if (text.getText().equals("H") && ignoreLoneHydrogens) {
							if (anyTextsOnSameLine(u, list)) {
								textsLookedAt.add(u);
								svgTextsLookedAt.add(getText(u));
								if (text.getFontSize() > largestFontSize) { 
									largestFontSize = text.getFontSize();
									yOfLargestFontSize = text.getY();
								}
							} 
						} else {
							textsLookedAt.add(u);
							svgTextsLookedAt.add(getText(u));
							if (text.getFontSize() > largestFontSize && !text.getText().replace((char) 160, (char) 32).replace(" ", "").equals("")) { 
								largestFontSize = text.getFontSize();
								yOfLargestFontSize = text.getY();
							}
						}
					}
				} catch (ClassCastException e) {
					continue;
				}
			}
		}

		protected abstract boolean anyTextsOnSameLine(U j, List<T> list);

		protected abstract SVGText getText(U j);
		
	}
	
	static class LargestFontFinderForJoinables extends LargestFontFinder<Joinable, JoinableText> {

		public LargestFontFinderForJoinables(List<Joinable> list, boolean ignoreLoneHydrogens, ChemistryBuilderParameters parameters) {
			super(list, ignoreLoneHydrogens, parameters);
		}

		@Override
		protected boolean anyTextsOnSameLine(JoinableText j, List<Joinable> list) {
			return JoinableText.anyTextsOnSameLine(j, list, true, parameters, false);
		}

		@Override
		protected SVGText getText(JoinableText j) {
			return j.getSVGElement();
		}
		
	}
	
	static class LargestFontFinderForSVGTexts extends LargestFontFinder<SVGText, SVGText> {

		public LargestFontFinderForSVGTexts(List<SVGText> list, boolean ignoreLoneHydrogens, ChemistryBuilderParameters parameters) {
			super(list, ignoreLoneHydrogens, parameters);
		}

		@Override
		protected boolean anyTextsOnSameLine(SVGText j, List<SVGText> list) {
			return JoinableText.anyTextsOnSameLine(j, list, true, parameters, false);
		}

		@Override
		protected SVGText getText(SVGText j) {
			return j;
		}
		
	}
	
	public static String getSingleLineTextFromJoinableTexts(List<Joinable> list, ChemistryBuilderParameters parameters) {
		double allowedFontSizeVariation = parameters.getAllowedFontSizeVariation();
		LargestFontFinderForJoinables finder = new LargestFontFinderForJoinables(list, true, parameters);
		Joinable.sortJoinablesByX(finder.textsLookedAt);
		List<JoinableText> texts = finder.textsLookedAt;
		String text = "";
		SVGText previous = null;
		for (JoinableText joinableText : texts) {
			String character = "";
			SVGText svgText = joinableText.getSVGElement();
			if (svgText.getFontSize() / finder.largestFontSize <= allowedFontSizeVariation) {
				//TODO unify with ScriptWord.toUnderscoreAndCaretString()
				if (svgText.getY() < finder.yOfLargestFontSize) {
					character = "^";
				} else {
					character = "_";
				}
			}
			//text += (previous != null && previous.getEnSpaceCount(svgText) >= minimumSpaceWidthInEnSpaces ? " " : "") + character + svgText.getText().replace((char) 160, (char) 32) + character;
			text += (previous != null && !JoinableText.haveAlignedVerticals(previous, svgText, parameters, false) ? " " : "") + character + svgText.getText().replace((char) 160, (char) 32) + character;
			previous = joinableText.getSVGElement();
		}
		return (text == "" ? null : text);
		//JoinableScriptWord word = getWord();
		//return (word == null ? null : word.getScriptWord().toUnderscoreAndCaretString());
	}

	public static String getMultiLineTextFromJoinableTexts(List<Joinable> list, ChemistryBuilderParameters parameters) {
		LargestFontFinderForJoinables finder = new LargestFontFinderForJoinables(list, false, parameters);
		
		TextStructurer t = new TextStructurer(finder.svgTextsLookedAt);
		//List<RawWords> lines = t.createRawWordsList();
		List<ScriptLine> lines = t.getScriptedLineList();
		//j.get(0).getScriptWordList().get(0).createSuscriptTextLineList();
		/*List<JoinableScriptWord> words = new ArrayList<JoinableScriptWord>();
		higherPrimitives.setWordsList(words);
		for (ScriptLine line : lines) {
			for (ScriptWord word : line.getScriptWordList()) {
				words.add(new JoinableScriptWord(word));
			}
		}*/
		String result = "";
		boolean firstLine = true;
		for (ScriptLine line : lines) {
			if (!firstLine) {
				result += System.getProperty("line.separator");
			}
			boolean firstWord = true;
			for (ScriptWord word : line.getScriptWordList()) {
				result += (firstWord ? "" : " ") + word.toUnderscoreAndCaretString();
				firstWord = false;
			}
			firstLine = false;
		}
		
		return result;//"one\r\ntwo";
	}
	
}