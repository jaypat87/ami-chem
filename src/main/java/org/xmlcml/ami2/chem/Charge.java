package org.xmlcml.ami2.chem;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Real2;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGLine;
import org.xmlcml.graphics.svg.SVGText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Charge extends Joinable {

	private final static Logger LOG = Logger.getLogger(Charge.class);

	private static final double CHARGE_PRIORITY = 0.2;

	private List<SVGLine> lines;
	private SVGText text;

	private Double radiusExpansion;

	public Charge(ChemistryBuilderParameters parameters, List<SVGLine> lines) {
		radiusExpansion = parameters.getChargeRadiusExpansion();
		if (lines.size() != 1 && lines.size() != 2) {
			throw new IllegalArgumentException();
		}
		this.lines = lines;
		addJoinPointFromLines();
	}

	public Charge(ChemistryBuilderParameters parameters, SVGLine i) {
		radiusExpansion = parameters.getChargeRadiusExpansion();
		lines = new ArrayList<SVGLine>();
		lines.add(i);
		addJoinPointFromLines();
	}

	public Charge(ChemistryBuilderParameters parameters, SVGLine i, SVGLine j) {
		radiusExpansion = parameters.getChargeRadiusExpansion();
		lines = new ArrayList<SVGLine>();
		lines.add(i);
		lines.add(j);
		addJoinPointFromLines();
	}
	
	public Charge(ChemistryBuilderParameters parameters, SVGText text) {
		radiusExpansion = parameters.getChargeRadiusExpansion();
		if ("+".equals(text.getText()) || "-".equals(text.getText())) {
			this.text = text;
		} else {
			throw new IllegalArgumentException();
		}
		addJoinPointFromText();
	}

	private void addJoinPointFromText() {
		JoinPoint joinPoint;
		joinPoint = new JoinPoint(text.getCentrePointOfFirstCharacter(), (text.getWidthOfFirstCharacter() / 2) * radiusExpansion);
		getJoinPoints().add(joinPoint);
	}

	private void addJoinPointFromLines() {
		Real2 coord = lines.get(0).getMidPoint();
		if (coord != null) {
			JoinPoint joinPoint = new JoinPoint(coord, (lines.get(0).getLength() / 2) * radiusExpansion);
			getJoinPoints().add(joinPoint);
		}
	}
	
	public SVGElement getSVGElement() {
		return text;
	}

	public double getPriority() {
		return CHARGE_PRIORITY;
	}
	
	public int getCharge() {
		return (text != null ? (text.getText().equals("+") ? 1 : -1) : (lines.size() == 1 ? -1 : 1));
	}
	
	public String toString() {
		return "Charge\n ... " + Arrays.toString(getJoinPoints().toArray());
	}
	
}