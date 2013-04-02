/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2010  AAVSO (http://www.aavso.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package org.aavso.tools.vstar.util.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.exception.AlgorithmError;
import org.aavso.tools.vstar.util.locale.LocaleProps;
import org.aavso.tools.vstar.util.period.IPeriodAnalysisAlgorithm;
import org.aavso.tools.vstar.util.prefs.NumericPrecisionPrefs;

/**
 * This class creates a multi-periodic fit model for the specified observations.
 */
public class PeriodAnalysisDerivedMultiPeriodicModel implements IModel {

	private List<Harmonic> harmonics;
	private IPeriodAnalysisAlgorithm algorithm;

	private List<ValidObservation> fit;
	private List<ValidObservation> residuals;

	// TODO: PeriodFitParameters could be a generic parameter per concrete
	// model since this will differ for each model type.
	private List<PeriodFitParameters> parameters;

	private String desc;

	private Map<String, String> functionStrMap;

	/**
	 * Constructor.
	 * 
	 * @param harmonics
	 *            A list of harmonics used as input to the fit algorithm.
	 * @param algorithm
	 *            The algorithm to be executed to generate the fit.
	 */
	public PeriodAnalysisDerivedMultiPeriodicModel(List<Harmonic> harmonics,
			IPeriodAnalysisAlgorithm algorithm) {
		this.harmonics = harmonics;
		this.algorithm = algorithm;

		this.fit = new ArrayList<ValidObservation>();
		this.residuals = new ArrayList<ValidObservation>();
		this.parameters = new ArrayList<PeriodFitParameters>();
		this.functionStrMap = new LinkedHashMap<String, String>();

		desc = null;
	}

	/**
	 * @see org.aavso.tools.vstar.util.model.IModel#getDescription()
	 */
	@Override
	public String getDescription() {
		if (desc == null) {
			desc = getKind() + " from periods: ";
			for (Harmonic harmonic : harmonics) {
				desc += String.format(NumericPrecisionPrefs
						.getOtherOutputFormat(), harmonic.getPeriod())
						+ " ";
			}
		}

		return desc;
	}

	/**
	 * @see org.aavso.tools.vstar.util.model.IModel#getKind()
	 */
	@Override
	public String getKind() {
		return "Fit";
	}

	/**
	 * @return the harmonics
	 */
	public List<Harmonic> getHarmonics() {
		return harmonics;
	}

	/**
	 * @see org.aavso.tools.vstar.util.model.IModel#getFit()
	 */
	@Override
	public List<ValidObservation> getFit() {
		return fit;
	}

	/**
	 * @see org.aavso.tools.vstar.util.model.IModel#getResiduals()
	 */
	@Override
	public List<ValidObservation> getResiduals() {
		return residuals;
	}

	/**
	 * @see org.aavso.tools.vstar.util.model.IModel#getParameters()
	 */
	@Override
	public List<PeriodFitParameters> getParameters() {
		return parameters;
	}

	/**
	 * @see org.aavso.tools.vstar.util.IAlgorithm#execute()
	 */
	@Override
	public void execute() throws AlgorithmError {

		try {
			algorithm.multiPeriodicFit(harmonics, this);

			functionStrMap.put(LocaleProps.get("MODEL_INFO_FUNCTION_TITLE"),
					toString());

			functionStrMap.put(LocaleProps.get("MODEL_INFO_EXCEL_TITLE"),
					toExcelString());

			functionStrMap.put(LocaleProps.get("MODEL_INFO_R_TITLE"),
					toRString());

		} catch (InterruptedException e) {
			// Do nothing; just return.
		}
	}

	@Override
	public boolean hasFuncDesc() {
		return true;
	}

	public String toString() {
		String strRepr = functionStrMap.get(LocaleProps
				.get("MODEL_INFO_FUNCTION_TITLE"));

		if (strRepr == null) {
			strRepr = "f(t) = ";

			String fmt = NumericPrecisionPrefs.getOtherOutputFormat();
			double constantCoefficient = parameters.get(0)
					.getConstantCoefficient();
			strRepr += String.format(fmt, constantCoefficient) + "\n";

			for (int i = 0; i < parameters.size(); i++) {
				PeriodFitParameters params = parameters.get(i);
				strRepr += params.toString() + "\n";
			}

			strRepr = strRepr.trim();
		}

		return strRepr;
	}

	private String toExcelString() { 
		String strRepr = functionStrMap.get(LocaleProps
				.get("MODEL_INFO_EXCEL_TITLE"));

		if (strRepr == null) {
			strRepr = "=SUM(";

			String fmt = NumericPrecisionPrefs.getOtherOutputFormat();
			double constantCoefficient = parameters.get(0)
					.getConstantCoefficient();
			strRepr += String.format(fmt, constantCoefficient);

			for (int i = 0; i < parameters.size(); i++) {
				PeriodFitParameters params = parameters.get(i);
				strRepr += params.toExcelString();
			}

			strRepr += ")";
		}

		return strRepr;
	}

	public String toRString() {
		String strRepr = functionStrMap.get(LocaleProps
				.get("MODEL_INFO_R_TITLE"));

		if (strRepr == null) {
			strRepr = "model <- function(t) ";

			String fmt = NumericPrecisionPrefs.getOtherOutputFormat();
			double constantCoefficient = parameters.get(0)
					.getConstantCoefficient();
			strRepr += String.format(fmt, constantCoefficient);

			for (int i = 0; i < parameters.size(); i++) {
				PeriodFitParameters params = parameters.get(i);
				strRepr += params.toRString();
			}

			strRepr = strRepr.trim();
		}

		return strRepr;
	}

	@Override
	public void interrupt() {
		algorithm.interrupt();
	}

	@Override
	public Map<String, String> getFunctionStrings() {
		return functionStrMap;
	}
}
