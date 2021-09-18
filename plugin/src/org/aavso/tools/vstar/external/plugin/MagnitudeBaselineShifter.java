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
package org.aavso.tools.vstar.external.plugin;

import java.util.Set;

import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.plugin.ObservationTransformerPluginBase;
import org.aavso.tools.vstar.ui.dialog.DoubleField;
import org.aavso.tools.vstar.ui.dialog.MultiEntryComponentDialog;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.ui.mediator.message.NewStarMessage;
import org.aavso.tools.vstar.ui.mediator.message.UndoableActionType;
import org.aavso.tools.vstar.ui.model.plot.ISeriesInfoProvider;
import org.aavso.tools.vstar.ui.undo.IUndoableAction;
import org.aavso.tools.vstar.util.notification.Listener;

/**
 * This plugin allows the magnitude baseline of a set of observations to be
 * reversibly shifted up or down by a specified amount.
 */
public class MagnitudeBaselineShifter extends ObservationTransformerPluginBase {

	private double shift;
	private boolean firstInvocation;

	public MagnitudeBaselineShifter() {
		super();
		shift = 0;
		firstInvocation = true;
	}

	@Override
	public String getDisplayName() {
		return "Magnitude Baseline Shifter";
	}

	@Override
	public String getDescription() {
		return "Magnitude Baseline Shifter";
	}

	@Override
	public IUndoableAction createAction(ISeriesInfoProvider seriesInfo,
			Set<SeriesType> series) {

		if (firstInvocation) {
			Mediator.getInstance().getNewStarNotifier()
					.addListener(getNewStarListener());

			firstInvocation = false;
		}

		return new IUndoableAction() {
			@Override
			public String getDisplayString() {
				return "shifted magnitude baseline";
			}

			@Override
			public boolean execute(UndoableActionType type) {
				boolean ok = true;
				
				switch (type) {
				case DO:
					// For a do operation, invoke the dialog when the action is
					// executed.
					ok = invokeDialog();
					break;
					
				case UNDO:
				case REDO:
					// For an undo or a redo operation, negate the shift.
					shift = -shift;
					break;
				}

				if (shift != 0) {
					for (SeriesType seriesType : series) {
						for (ValidObservation ob : seriesInfo
								.getObservations(seriesType)) {
							ob.setMag(ob.getMag() + shift);
						}
					}
				}
				
				return ok;
			}
		};
	}

	/**
	 * Get the new star listener for this plugin.
	 */
	protected Listener<NewStarMessage> getNewStarListener() {
		return new Listener<NewStarMessage>() {
			public void update(NewStarMessage info) {
				shift = 0;
			}

			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	/**
	 * Invoke dialog to request magnitude shift value.
	 * 
	 * @return Whether the dialog was dismissed but not cancelled.
	 */
	private boolean invokeDialog() {
		boolean ok = true;

		DoubleField shiftField = new DoubleField("Shift", null, null, shift);
		MultiEntryComponentDialog dialog = new MultiEntryComponentDialog(
				"Magnitude Shift", shiftField);

		ok = !dialog.isCancelled();

		if (ok) {
			shift = shiftField.getValue();
		} else {
			shift = 0;
		}

		return ok;
	}
}
