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

package org.aavso.tools.vstar.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.aavso.tools.vstar.data.InvalidObservation;
import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.exception.ObservationReadError;

/**
 * This is the abstract base class for all observation retrieval classes,
 * irrespective of source (AAVSO standard file format, simple file format, VStar
 * database).
 */
public abstract class AbstractObservationRetriever {

	private double minMag;
	private double maxMag;
	
	/**
	 * The list of valid observations retrieved.
	 */
	protected List<ValidObservation> validObservations;

	/**
	 * The list of invalid observations retrieved.
	 */
	protected List<InvalidObservation> invalidObservations;

	/**
	 * A mapping from observation category (e.g. band, fainter-than) to list of
	 * valid observations.
	 */
	protected Map<SeriesType, List<ValidObservation>> validObservationCategoryMap;

	/**
	 * Constructor.
	 */
	public AbstractObservationRetriever() {
		// TODO: consider making lists LinkedLists to accommodate all
		// input types, and the possible need to add to head or tail for
		// time panning; this applies mostly to valid observations.
		this.validObservations = new ArrayList<ValidObservation>();
		this.invalidObservations = new ArrayList<InvalidObservation>();
		this.validObservationCategoryMap = new TreeMap<SeriesType, List<ValidObservation>>();
		this.minMag = Double.MAX_VALUE;
		this.maxMag = -Double.MAX_VALUE;
	}

	/**
	 * @return the minimum magnitude
	 */
	public double getMinMag() {
		return minMag;
	}

	/**
	 * @param minMag the minimum magnitude to set
	 */
	public void setMinMag(double minMag) {
		this.minMag = minMag;
	}

	/**
	 * @return the maximum magnitude
	 */
	public double getMaxMag() {
		return maxMag;
	}

	/**
	 * @param maxMag the maximum magnitude to set
	 */
	public void setMaxMag(double maxMag) {
		this.maxMag = maxMag;
	}

	/**
	 * Retrieve the set of observations from the specified source.
	 * 
	 * @throws throws ObservationReadError
	 */
	public abstract void retrieveObservations() throws ObservationReadError,
			InterruptedException;

	/**
	 * @return the validObservations
	 */
	public List<ValidObservation> getValidObservations() {
		return validObservations;
	}

	/**
	 * @return the invalidObservations
	 */
	public List<InvalidObservation> getInvalidObservations() {
		return invalidObservations;
	}

	/**
	 * @return the validObservationCategoryMap
	 */
	public Map<SeriesType, List<ValidObservation>> getValidObservationCategoryMap() {
		return validObservationCategoryMap;
	}

	/**
	 * <p>
	 * Add an observation to the list of valid observations.
	 * </p>
	 * 
	 * <p>
	 * This is a convenience method that adds an observation to the list of
	 * valid observations and categorises it by band. This method is
	 * particularly suitable for observation source plugins since it asks
	 * whether an observation satisfies the requirement that it has at least JD
	 * and magnitude values. The caller can either propagate this exception
	 * further or add to the invalid observation list, or do whatever else it
	 * considers to be appropriate.
	 * </p>
	 * 
	 * @param ob
	 *            The valid observation to be added to collections.
	 */
	protected void collectObservation(ValidObservation ob)
			throws ObservationReadError {
		if (ob.getDateInfo() == null) {
			throw new ObservationReadError("Observation #"
					+ ob.getRecordNumber() + " has no date.");
		}

		if (ob.getMagnitude() == null) {
			throw new ObservationReadError("Observation #"
					+ ob.getRecordNumber() + " has no magnitude.");
		}

		addValidObservation(ob);
		categoriseValidObservation(ob);
	}

	/**
	 * Here we categorise a valid observation in terms of whether it is a
	 * fainter-than or discrepant or belongs to a particular band, in that
	 * order.
	 * 
	 * @param validOb
	 *            A valid observation.
	 */
	protected void categoriseValidObservation(ValidObservation validOb) {
		SeriesType category = null;

		if (validOb.getMagnitude().isFainterThan()) {
			category = SeriesType.FAINTER_THAN;
		} else if (validOb.isDiscrepant()) {
			category = SeriesType.DISCREPANT;
		} else {
			category = validOb.getBand();
		}

		List<ValidObservation> validObsList = validObservationCategoryMap
				.get(category);

		if (validObsList == null) {
			validObsList = new ArrayList<ValidObservation>();
			validObservationCategoryMap.put(category, validObsList);
		}

		validObsList.add(validOb);
	}

	/**
	 * Adds an observation to the list of valid observations.
	 * Also, updates min/max magnitude values for the dataset.
	 * 
	 * @param ob
	 *            The valid observation to be added.
	 */
	protected void addValidObservation(ValidObservation ob) {
		validObservations.add(ob);
		
		double uncert = ob.getMagnitude().getUncertainty();
		// If uncertainty not given, get HQ uncertainty if present.
		if (uncert == 0.0 && ob.getHqUncertainty() != null) {
			uncert = ob.getHqUncertainty();
		}

		if (ob.getMag() - uncert < minMag) {
			minMag = ob.getMag() - uncert;
		}

		if (ob.getMag() + uncert > maxMag) {
			maxMag = ob.getMag() + uncert;
		}
	}

	/**
	 * Add an observation to the list of invalid observations.
	 * 
	 * @param ob
	 *            The invalid observation to be added.
	 */
	protected void addInvalidObservation(InvalidObservation ob) {
		invalidObservations.add(ob);
	}
}