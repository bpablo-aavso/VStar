/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2009  AAVSO (http://www.aavso.org/)
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
package org.aavso.tools.vstar.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.ui.model.ObservationAndMeanPlotModel;

/**
 * This component permits the days-in-bin value to be changed which in turn
 * modifies the means series in the observations and means plot.
 */
public class DaysInBinSettingPane extends JPanel {

	private ObservationAndMeanPlotModel obsAndMeanModel;

	private JSpinner daysInBinSpinner;
	private SpinnerNumberModel daysInBinSpinnerModel;

	/**
	 * Constructor
	 * 
	 * @param obsAndMeanModel
	 */
	public DaysInBinSettingPane(ObservationAndMeanPlotModel obsAndMeanModel) {
		super();

		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.obsAndMeanModel = obsAndMeanModel;

		this.setBorder(BorderFactory.createTitledBorder("Days in Means Bin"));

		// Slider for days-in-bin.
		// Note that a slider can only handle integer values.
		List<ValidObservation> meanAndObsList = obsAndMeanModel
				.getMeanObsList();

		int max = (int) (meanAndObsList.get(meanAndObsList.size() - 1).getJD() - meanAndObsList
				.get(0).getJD());

		this.add(Box.createHorizontalGlue());

		// Spinner for days-in-bin with the specified current, min, and max
		// values, and step size (1 day).
		daysInBinSpinnerModel = new SpinnerNumberModel(obsAndMeanModel
				.getDaysInBin(), 0, max, 1);
		daysInBinSpinner = new JSpinner(daysInBinSpinnerModel);
		this.add(daysInBinSpinner);

		this.add(Box.createHorizontalGlue());

		// Update button for days-in-bin.
		JButton updateButton = new JButton("Update");
		updateButton.addActionListener(createUpdateMeansButtonListener());
		this.add(updateButton);
		
		this.add(Box.createHorizontalGlue());
	}

	// Return a listener for the "update means" button.
	private ActionListener createUpdateMeansButtonListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get the value and change the means series.
				// double daysInBin =
				// Double.parseDouble(daysInBinField.getText());
				double daysInBin = (Double) daysInBinSpinnerModel.getNumber();
				obsAndMeanModel.changeMeansSeries(daysInBin);
			}
		};
	}
}
