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
package org.aavso.tools.vstar.ui.dialog;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.aavso.tools.vstar.ui.MainFrame;
import org.aavso.tools.vstar.util.locale.NumberParser;

/**
 * This modal dialog class allows period analysis parameters to be entered.
 */
public class PeriodAnalysisParameterDialog extends AbstractOkCancelDialog {

	private double loFreq;
	private double hiFreq;
	private double resolution;

	private JTextField loFreqField;
	private JTextField hiFreqField;
	private JTextField resolutionField;

	public PeriodAnalysisParameterDialog(double loFreq, double hiFreq,
			double resolution) {
		super("Parameters");
		this.loFreq = loFreq;
		this.hiFreq = hiFreq;
		this.resolution = resolution;

		Container contentPane = this.getContentPane();

		JPanel topPane = new JPanel();
		topPane.setLayout(new BoxLayout(topPane, BoxLayout.PAGE_AXIS));
		topPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		topPane.add(createParameterPane());

		// OK, Cancel
		topPane.add(createButtonPane());

		contentPane.add(topPane);

		this.pack();
		setLocationRelativeTo(MainFrame.getInstance().getContentPane());
		this.setVisible(true);
	}

	private JPanel createParameterPane() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		loFreqField = new JTextField(String.format("%1.6f", loFreq));
		loFreqField
				.setBorder(BorderFactory.createTitledBorder("Low Frequency"));
		loFreqField.setToolTipText("Enter low frequency for scan");
		panel.add(loFreqField);
		
		panel.add(Box.createRigidArea(new Dimension(75, 10)));

		hiFreqField = new JTextField(String.format("%1.6f", hiFreq));
		hiFreqField.setBorder(BorderFactory
				.createTitledBorder("High Frequency"));
		hiFreqField.setToolTipText("Enter high frequency for scan");
		panel.add(hiFreqField);
		
		panel.add(Box.createRigidArea(new Dimension(75, 10)));

		resolutionField = new JTextField(String.format("%1.6f", resolution));
		resolutionField.setBorder(BorderFactory
				.createTitledBorder("Resolution"));
		resolutionField.setToolTipText("Enter resolution for scan");
		panel.add(resolutionField);

		panel.add(Box.createRigidArea(new Dimension(75, 10)));

		return panel;
	}

	/**
	 * @return the loFreq
	 */
	public double getLoFreq() {
		return loFreq;
	}

	/**
	 * @return the hiFreq
	 */
	public double getHiFreq() {
		return hiFreq;
	}

	/**
	 * @return the resolution
	 */
	public double getResolution() {
		return resolution;
	}

	/**
	 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#cancelAction()
	 */
	@Override
	protected void cancelAction() {
		// Nothing to do.
	}

	/**
	 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#okAction()
	 */
	@Override
	protected void okAction() {
		try {
			loFreq = NumberParser.parseDouble(loFreqField.getText());
			hiFreq = NumberParser.parseDouble(hiFreqField.getText());
			resolution = NumberParser.parseDouble(resolutionField.getText());

			// If we got to here without a parse error, we can dismiss the dialog.
			cancelled = false;
			setVisible(false);
			dispose();
		} catch (NumberFormatException e) {
			// Nothing to do. The dialog stays open.
		}
	}
}
