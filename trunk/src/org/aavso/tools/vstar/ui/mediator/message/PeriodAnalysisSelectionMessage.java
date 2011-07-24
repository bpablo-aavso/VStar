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
package org.aavso.tools.vstar.ui.mediator.message;

import org.aavso.tools.vstar.util.period.dcdft.PeriodAnalysisDataPoint;

/**
 * Instances of this message class can be sent when a period analysis chart
 * data point selection occurs.
 */
public class PeriodAnalysisSelectionMessage extends MessageBase {

	private PeriodAnalysisDataPoint dataPoint;

	/**
	 * Constructor
	 * 
	 * @param source
	 *            The source of this message.
	 * @param dataPoint
	 *            The selected period analysis data point containing period,
	 *            frequency, power, amplitude.
	 */
	public PeriodAnalysisSelectionMessage(Object source,
			PeriodAnalysisDataPoint dataPoint) {
		super(source);
		this.dataPoint = dataPoint;
	}

	/**
	 * @return the selected period analysis data point
	 */
	public PeriodAnalysisDataPoint getDataPoint() {
		return dataPoint;
	}
}
