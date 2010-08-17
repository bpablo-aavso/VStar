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
package org.aavso.tools.vstar.util.stats;

import org.aavso.tools.vstar.data.ValidObservation;

/**
 * The results associated with a single bin.
 */
public class Bin {

	private ValidObservation meanObservation;
	private double[] magnitudes;

	/**
	 * Constructor
	 * 
	 * @param meanObservation
	 *            The mean observation resulting from a binning activity.
	 * @param magnitudes
	 *            The corresponding raw binned magnitude data.
	 */
	public Bin(ValidObservation meanObservation, double[] magnitudeBin) {
		this.meanObservation = meanObservation;
		this.magnitudes = magnitudeBin;
	}

	/**
	 * @return the meanObservation
	 */
	public ValidObservation getMeanObservation() {
		return meanObservation;
	}

	/**
	 * @return the magnitudes
	 */
	public double[] getMagnitudes() {
		return magnitudes;
	}
}
