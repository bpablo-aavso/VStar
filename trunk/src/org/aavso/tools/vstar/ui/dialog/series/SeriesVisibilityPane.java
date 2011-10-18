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
package org.aavso.tools.vstar.ui.dialog.series;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.ui.mediator.AnalysisType;
import org.aavso.tools.vstar.ui.model.plot.ObservationAndMeanPlotModel;
import org.aavso.tools.vstar.util.notification.Listener;
import org.aavso.tools.vstar.util.stats.BinningResult;

/**
 * This class represents a pane with checkboxes showing those series that are
 * rendered. The series to be displayed can be changed.
 */
public class SeriesVisibilityPane extends JPanel {

	private ObservationAndMeanPlotModel obsPlotModel;
	private AnalysisType analysisType;

	private Map<Integer, Boolean> visibilityDeltaMap;

	private List<JCheckBox> checkBoxes;

	private JCheckBox discrepantCheckBox;
	private JCheckBox excludedCheckBox;

	private JCheckBox meanCheckBox;
	private JCheckBox filteredCheckBox;
	private JCheckBox modelCheckBox;
	private JCheckBox residualsCheckBox;

	private int discrepantCount;
	private int excludedCount;

	/**
	 * Constructor.
	 */
	public SeriesVisibilityPane(ObservationAndMeanPlotModel obsPlotModel,
			AnalysisType analysisType) {
		super();

		this.analysisType = analysisType;

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBorder(BorderFactory.createTitledBorder("Visibility"));
		this
				.setToolTipText("Select or deselect series for desired visibility.");

		this.obsPlotModel = obsPlotModel;
		this.visibilityDeltaMap = new HashMap<Integer, Boolean>();

		this.checkBoxes = new ArrayList<JCheckBox>();

		filteredCheckBox = null;
		modelCheckBox = null;

		addSeriesCheckBoxes();

		// Get initial discrepant and excluded counts, if any.

		Integer discrepantSeriesNum = obsPlotModel.getSrcTypeToSeriesNumMap()
				.get(SeriesType.DISCREPANT);
		discrepantCount = obsPlotModel.getSeriesNumToObSrcListMap().get(
				discrepantSeriesNum).size();

		Integer excludedSeriesNum = obsPlotModel.getSrcTypeToSeriesNumMap()
				.get(SeriesType.Excluded);
		excludedCount = obsPlotModel.getSeriesNumToObSrcListMap().get(
				excludedSeriesNum).size();

		obsPlotModel.getMeansChangeNotifier().addListener(
				createMeanObsChangeListener());

		addButtons();
	}

	// Create a checkbox for each series, grouped by type.
	private void addSeriesCheckBoxes() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(createDataSeriesCheckboxes());
		panel.add(createOtherSeriesCheckboxes());
		this.add(panel);
	}

	private JPanel createDataSeriesCheckboxes() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("Data"));

		// Ensure the panel is always wide enough.
		this.add(Box.createRigidArea(new Dimension(75, 1)));

		// We treat derived series separately.
		for (SeriesType series : this.obsPlotModel.getSeriesKeys()) {
			if (!series.isSynthetic()) {
				String seriesName = series.getDescription();
				JCheckBox checkBox = new JCheckBox(seriesName);

				checkBox
						.addActionListener(createSeriesVisibilityCheckBoxListener());

				// Enable/disable the series.
				boolean vis = obsPlotModel.getSeriesVisibilityMap().get(series);
				checkBox.setSelected(vis);

				panel.add(checkBox);
				panel.add(Box.createRigidArea(new Dimension(3, 3)));

				checkBoxes.add(checkBox);

				Integer seriesNum = obsPlotModel.getSrcTypeToSeriesNumMap()
						.get(series);

				// Listeners need access to discrepant and excluded checkboxes.
				// We also set the initial state for these checkboxes conditionally,
				// depending upon whether any observations are present in these series.
				if (series == SeriesType.DISCREPANT) {
					discrepantCheckBox = checkBox;
					if (obsPlotModel.getSeriesNumToObSrcListMap()
							.get(seriesNum).isEmpty()) {
						setInitialCheckBoxState(series, discrepantCheckBox);
					}
				} else if (series == SeriesType.Excluded) {
					excludedCheckBox = checkBox;
					if (obsPlotModel.getSeriesNumToObSrcListMap()
							.get(seriesNum).isEmpty()) {
						setInitialCheckBoxState(series, excludedCheckBox);
					}
				}
			}
		}

		return panel;
	}

	private JPanel createOtherSeriesCheckboxes() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("Analysis"));

		// Mean series.
		meanCheckBox = new JCheckBox(SeriesType.MEANS.getDescription());
		meanCheckBox
				.addActionListener(createSeriesVisibilityCheckBoxListener());
		setInitialCheckBoxState(SeriesType.MEANS, meanCheckBox);
		panel.add(meanCheckBox);
		panel.add(Box.createRigidArea(new Dimension(3, 3)));
		checkBoxes.add(meanCheckBox);

		// Filtered series.
		filteredCheckBox = new JCheckBox(SeriesType.Filtered.getDescription());
		filteredCheckBox
				.addActionListener(createSeriesVisibilityCheckBoxListener());
		setInitialCheckBoxState(SeriesType.Filtered, filteredCheckBox);
		panel.add(filteredCheckBox);
		panel.add(Box.createRigidArea(new Dimension(3, 3)));
		checkBoxes.add(filteredCheckBox);

		JPanel subPanel = new JPanel();
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
		subPanel.setBorder(BorderFactory.createTitledBorder("Model"));
		subPanel.add(Box.createRigidArea(new Dimension(75, 1)));

		// Model series.
		modelCheckBox = new JCheckBox(SeriesType.Model
				.getDescription());
		modelCheckBox
				.addActionListener(createSeriesVisibilityCheckBoxListener());
		setInitialCheckBoxState(SeriesType.Model, modelCheckBox);
		subPanel.add(modelCheckBox);
		subPanel.add(Box.createRigidArea(new Dimension(3, 3)));
		checkBoxes.add(modelCheckBox);

		// Residuals series.
		residualsCheckBox = new JCheckBox(SeriesType.Residuals.getDescription());
		residualsCheckBox
				.addActionListener(createSeriesVisibilityCheckBoxListener());
		setInitialCheckBoxState(SeriesType.Residuals, residualsCheckBox);
		subPanel.add(residualsCheckBox);
		checkBoxes.add(residualsCheckBox);

		panel.add(subPanel);

		return panel;
	}

	/**
	 * Set the enabled and selected states of the checkbox corresponding to the
	 * specified series type according to the plot model's visibility map.
	 * 
	 * @param seriesType
	 *            The series type in question.
	 * @param checkbox
	 *            The checkbox whose state is to be set.
	 */
	protected void setInitialCheckBoxState(SeriesType seriesType,
			JCheckBox checkbox) {
		Integer seriesNum = obsPlotModel.getSrcTypeToSeriesNumMap().get(
				seriesType);

		// This series exists and has obs (or not), so allow it to be selected
		// (or not).
		boolean hasObs = obsPlotModel.getSeriesNumToObSrcListMap().containsKey(
				seriesNum)
				&& !obsPlotModel.getSeriesNumToObSrcListMap().get(seriesNum)
						.isEmpty();
		checkbox.setEnabled(hasObs);

		// The series is (or is not) marked as being visible so also select
		// (or don't) the checkbox. A series visibility may not have been set in
		// the map yet, unless it has been selected by default (e.g. visual
		// bands) or by the user.
		boolean visible = obsPlotModel.getSeriesVisibilityMap().containsKey(
				seriesType)
				&& obsPlotModel.getSeriesVisibilityMap().get(seriesType);
		checkbox.setSelected(visible);
	}

	// Return a listener for the series visibility checkboxes.
	private ActionListener createSeriesVisibilityCheckBoxListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox checkBox = (JCheckBox) e.getSource();
				updateSeriesVisibilityMap(checkBox);
				seriesVisibilityChange(getVisibilityDeltaMap());
			}
		};
	}

	// Create buttons for en-masse selection/deselection
	// of visibility checkboxes and an apply button.
	private void addButtons() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createEtchedBorder());

		JButton selectAllButton = new JButton("Select All");
		selectAllButton
				.addActionListener(createEnMasseSelectionButtonListener(true));
		panel.add(selectAllButton, BorderLayout.LINE_START);

		JButton deSelectAllButton = new JButton("Deselect All");
		deSelectAllButton
				.addActionListener(createEnMasseSelectionButtonListener(false));
		panel.add(deSelectAllButton, BorderLayout.LINE_END);

		this.add(panel, BorderLayout.CENTER);
	}

	/**
	 * Was there a change in the series visibility? Some callers may want to
	 * invoke this only for its side effects, while others may also want to know
	 * the result.
	 * 
	 * @param deltaMap
	 *            A mapping from series number to whether or not each series'
	 *            visibility was changed.
	 * 
	 * @return Was there a change in the visibility of any series?
	 */
	protected boolean seriesVisibilityChange(Map<Integer, Boolean> deltaMap) {
		boolean delta = false;

		for (int seriesNum : deltaMap.keySet()) {
			boolean visibility = deltaMap.get(seriesNum);
			delta |= obsPlotModel.changeSeriesVisibility(seriesNum, visibility);
		}

		return delta;
	}

	/**
	 * Return a listener for the "select/deselect all" checkbox.
	 * 
	 * @param target
	 *            The target check-button state.
	 * @return The button listener.
	 */
	private ActionListener createEnMasseSelectionButtonListener(
			final boolean target) {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (JCheckBox checkBox : checkBoxes) {
					if (checkBox.isEnabled()) {
						checkBox.setSelected(target);
						updateSeriesVisibilityMap(checkBox);
					}
				}

				seriesVisibilityChange(getVisibilityDeltaMap());
			}
		};
	}

	/**
	 * Update the series visibility map according to the state of the
	 * checkboxes.
	 * 
	 * @param checkBox
	 *            The checkbox whose state we want to update from.
	 */
	private void updateSeriesVisibilityMap(JCheckBox checkBox) {
		String seriesName = checkBox.getText();
		int seriesNum = obsPlotModel.getSrcTypeToSeriesNumMap().get(
				SeriesType.getSeriesFromDescription(seriesName));
		visibilityDeltaMap.put(seriesNum, checkBox.isSelected());
	}

	/**
	 * @return the visibilityDeltaMap
	 */
	public Map<Integer, Boolean> getVisibilityDeltaMap() {
		return visibilityDeltaMap;
	}

	// Return a mean observation change listener to ensure that the
	// mean series checkbox is selected if a new binning operation takes
	// place that also set the mean series as visible, assuming it was not
	// already.
	private Listener<BinningResult> createMeanObsChangeListener() {
		return new Listener<BinningResult>() {
			@Override
			public void update(BinningResult info) {
				// Check that the series was actually marked as visible in the
				// model!
				boolean meanSeriesVisible = obsPlotModel
						.getSeriesVisibilityMap().get(SeriesType.MEANS);
				if (meanSeriesVisible) {
					meanCheckBox.setSelected(true);
				}
			}

			@Override
			public boolean canBeRemoved() {
				return true;
			}
		};
	}
}
