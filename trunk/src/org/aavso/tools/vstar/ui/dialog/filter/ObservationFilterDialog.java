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
package org.aavso.tools.vstar.ui.dialog.filter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.data.filter.IObservationFieldMatcher;
import org.aavso.tools.vstar.data.filter.ObservationFilter;
import org.aavso.tools.vstar.ui.MainFrame;
import org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog;
import org.aavso.tools.vstar.ui.dialog.MessageBox;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.ui.mediator.message.FilteredObservationMessage;
import org.aavso.tools.vstar.ui.mediator.message.NewStarMessage;
import org.aavso.tools.vstar.ui.mediator.message.ObservationSelectionMessage;
import org.aavso.tools.vstar.util.notification.Listener;

/**
 * This dialog permits the user to specify a conjunctive filter, i.e. a set of
 * sub-filters each of which must be true (i.e. provide an match) for the
 * overall filter to be true (i.e. match). The result of applying a filter is
 * that some subset of the current observations will be captured in a collection
 * and a message sent.
 */
public class ObservationFilterDialog extends AbstractOkCancelDialog {

	private NewStarMessage newStarMessage;

	private ValidObservation selectedObservation;

	private ObservationFilter filter;

	private List<ObservationFilterPane> filterPanes;

	private JCheckBox useSelectedObservationCheckbox;

	/**
	 * Constructor.
	 */
	public ObservationFilterDialog() {
		super("Filter Observations");

		newStarMessage = null;

		selectedObservation = null;

		filter = new ObservationFilter();

		filterPanes = new LinkedList<ObservationFilterPane>();

		Container contentPane = this.getContentPane();

		JPanel topPane = new JPanel();
		topPane.setLayout(new BoxLayout(topPane, BoxLayout.PAGE_AXIS));
		topPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		topPane.add(createFiltersPane());
		topPane.add(createButtonPane());

		contentPane.add(topPane);

		this.pack();
	}

	@Override
	protected JPanel createButtonPane() {
		JPanel extraButtonPanel = new JPanel(new BorderLayout());

		useSelectedObservationCheckbox = new JCheckBox(
				"Use selected observation");
		useSelectedObservationCheckbox
				.addActionListener(createUseSelectedObservationCheckBoxHandler());
		useSelectedObservationCheckbox.setEnabled(false);
		extraButtonPanel.add(useSelectedObservationCheckbox,
				BorderLayout.LINE_START);

		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(createResetButtonListener());
		extraButtonPanel.add(resetButton, BorderLayout.LINE_END);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		panel.add(extraButtonPanel);
		panel.add(super.createButtonPane());

		return panel;
	}

	// Return a listener for the reset button.
	private ActionListener createResetButtonListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetFilters();
			}
		};
	}

	private JPanel createFiltersPane() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Create filter panes for each unique filter.
		for (int i = 0; i < ObservationFilter.MATCHERS.size(); i++) {
			ObservationFilterPane filterPane = new ObservationFilterPane();
			filterPanes.add(filterPane);
			panel.add(filterPane);
			panel.add(Box.createRigidArea(new Dimension(75, 10)));
		}

		return panel;
	}

	/**
	 * @return A new star listener for the filter dialog.
	 */
	public Listener<NewStarMessage> createNewStarListener() {
		return new Listener<NewStarMessage>() {

			@Override
			public void update(NewStarMessage info) {
				newStarMessage = info;
				resetFilters();
				useSelectedObservationCheckbox.setEnabled(false);
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	/**
	 * @return An observation selection listener for the filter dialog.
	 */
	public Listener<ObservationSelectionMessage> createObservationSelectionListener() {
		return new Listener<ObservationSelectionMessage>() {

			@Override
			public void update(ObservationSelectionMessage msg) {
				selectedObservation = msg.getObservation();
				useSelectedObservationCheckbox.setEnabled(true);

				// Pass the selected observation to each filter
				// pane if the checkbox was already selected.
				if (useSelectedObservationCheckbox.isSelected()) {
					for (ObservationFilterPane pane : filterPanes) {
						pane.useObservation(selectedObservation);
					}
				}
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	// Returns a use-selected-observation-checkbox listener.
	private ActionListener createUseSelectedObservationCheckBoxHandler() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ValidObservation ob = null;

				// If the checkbox is selected, retrieve the last selected
				// observation from the current view. This of course could be
				// null, except that this checkbox is only enabled when an
				// observation *has* been selected, so it really *can't* be null
				// at this point. :)
				if (useSelectedObservationCheckbox.isSelected()) {
					ob = selectedObservation;
				}

				// Pass the last selected observation or null to each filter
				// pane.
				for (ObservationFilterPane pane : filterPanes) {
					pane.useObservation(ob);
				}
			}
		};
	}

	/**
	 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#cancelAction()
	 */
	@Override
	protected void cancelAction() {
		// Nothing to be done.
	}

	/**
	 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#okAction()
	 */
	@Override
	protected void okAction() {
		// Add all non-null (valid) matchers to the filter.
		boolean filterError = false;

		for (ObservationFilterPane filterPane : filterPanes) {
			try {
				IObservationFieldMatcher matcher = filterPane.getFieldMatcher();
				if (matcher != null) {
					filter.addMatcher(matcher);
				}
			} catch (IllegalArgumentException e) {
				filterError = true;
				MessageBox.showErrorDialog(this, "Observation Filter", e
						.getMessage());
			}
		}

		if (!filterError) {
			if (filter.getMatchers().size() != 0) {

				setVisible(false);

				// MainFrame.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				// Apply the filter (and all its matchers) to the full set
				// of observations.
				List<ValidObservation> obs = newStarMessage.getObservations();

				Set<ValidObservation> filteredObs = filter
						.getFilteredObservations(obs);

				if (filteredObs.size() != 0) {
					// Send a message containing the observation subset.
					FilteredObservationMessage msg = new FilteredObservationMessage(
							this, filteredObs);

					Mediator.getInstance().getFilteredObservationNotifier()
							.notifyListeners(msg);
				} else {
					String msg = "No observations matched.";
					MessageBox.showWarningDialog(MainFrame.getInstance(),
							"Observation Filter", msg);
				}

				// MainFrame.getInstance().setCursor(null);
			} else {
				String msg = "No filter selected.";
				MessageBox.showWarningDialog(MainFrame.getInstance(),
						"Observation Filter", msg);
			}

			// Clear state for next use of this dialog.
			filter.reset();
		}
	}

	private void resetFilters() {
		for (ObservationFilterPane filterPane : filterPanes) {
			filterPane.resetFilter();
		}
	}
}
