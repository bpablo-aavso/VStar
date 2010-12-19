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
package org.aavso.tools.vstar.util.stats.epoch;

import java.util.List;

import org.aavso.tools.vstar.data.ValidObservation;

/**
 * This interface must be implemented by all classes implementing an
 * epoch determination strategy.
 */
public interface IEpochStrategy {

	/**
	 * Determine the epoch for a given sequence of observations.
	 * 
	 * @param obs A sequence of valid observations.
	 * @return The epoch as a double JD value.
	 */
	public double determineEpoch(List<ValidObservation> obs);
	
	/**
	 * @return A human readable description of the epoch 
	 * determination strategy.
	 */
	public String getDescription();
}
