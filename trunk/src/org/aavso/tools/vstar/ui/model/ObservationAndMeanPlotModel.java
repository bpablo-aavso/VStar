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
package org.aavso.tools.vstar.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.util.DescStats;
import org.aavso.tools.vstar.util.Notifier;

/**
 * This class is a model that represents a series of valid variable star
 * observations, e.g. for different bands (or from different sources).
 */
public class ObservationAndMeanPlotModel extends ObservationPlotModel {

	public static final String MEANS_SERIES_NAME = "Means";

	public static final int NO_MEANS_SERIES = -1;

	// The series number of the series that is the source of the
	// means series.
	private int meanSourceSeriesNum;

	// The series number of the means series.
	private int meansSeriesNum;

	// The number of days in a means series bin.
	private double daysInBin;

	// The observations that constitute the means series.
	private List<ValidObservation> meanObsList;

	private Notifier<List<ValidObservation>> meansChangeNotifier;

	/**
	 * Constructor
	 * 
	 * We add named observation source lists to unique series numbers. Then we
	 * add the initial mean-based series.
	 * 
	 * @param obsSourceListMap
	 *            A mapping from source name to lists of observation sources.
	 */
	public ObservationAndMeanPlotModel(
			Map<String, List<ValidObservation>> obsSourceListMap) {
		super(obsSourceListMap);
		this.meansSeriesNum = NO_MEANS_SERIES;
		this.daysInBin = DescStats.DEFAULT_BIN_DAYS; // TODO: or just define
		// this in this class?

		this.meansChangeNotifier = new Notifier<List<ValidObservation>>();

		this.meanSourceSeriesNum = determineMeanSeriesSource();

		this.setMeanSeries();
	}

	/**
	 * Set the mean-based series.
	 * 
	 * This method creates a new means series based upon the current
	 * mean source series index and days-in-bin. It then updates the
	 * view and any listeners.
	 */
	public void setMeanSeries() {

		meanObsList = DescStats.createdBinnedObservations(
				seriesNumToObSrcListMap.get(meanSourceSeriesNum), daysInBin);

		// As long as there were enough observations to create a means list
		// to make a "means" series, we do so.
		if (!meanObsList.isEmpty()) {
			boolean found = false;

			for (Map.Entry<Integer, String> entry : this.seriesNumToSrcNameMap
					.entrySet()) {
				if (MEANS_SERIES_NAME.equals(entry.getValue())) {
					int series = entry.getKey();
					this.seriesNumToObSrcListMap.put(series, meanObsList);
					this.fireDatasetChanged();
					found = true;
					break;
				}
			}

			// Is this the first time the means series has been added?
			if (!found) {
				this.meansSeriesNum = this.addObservationSeries(
						MEANS_SERIES_NAME, meanObsList);
				
				// Make sure it's rendered!
				this.getSeriesVisibilityMap().put(this.meansSeriesNum, true);
			}

			// Notify listeners.
			this.meansChangeNotifier.notifyListeners(meanObsList);

		} else {
			// TODO: remove empty check; should never happen because of way
			// binning is done
		}
	}

	public void changeMeansSeries(double daysInBin) {
		this.daysInBin = daysInBin;
		this.setMeanSeries();
	}

	/**
	 * @see org.aavso.tools.vstar.ui.model.ObservationPlotModel#changeSeriesVisibility(int,
	 *      boolean)
	 */
	public boolean changeSeriesVisibility(int seriesNum, boolean visibility) {
		// It doesn't make sense to remove the means series from a plot
		// whose purpose is to render a means series. :)
		if (seriesNum != meansSeriesNum) {
			return super.changeSeriesVisibility(seriesNum, visibility);
		} else {
			return false;
		}
	}

	/**
	 * Which series' elements should be joined visually (e.g. with lines)?
	 * 
	 * @return A collection of series numbers for series whose elements should be
	 *         joined visually.
	 */
	public Collection<Integer> getSeriesWhoseElementsShouldBeJoinedVisually() {
		List<Integer> seriesNumList = new ArrayList<Integer>();

		for (Map.Entry<Integer, String> entry : this.seriesNumToSrcNameMap
				.entrySet()) {
			if (MEANS_SERIES_NAME.equals(entry.getValue())) {
				seriesNumList.add(entry.getKey());
				break;
			}
		}

		return seriesNumList;
	}

	/**
	 * Return the error associated with the magnitude. We skip the series and
	 * item legality check to improve performance on the assumption that this
	 * has been checked already when calling getMagAsYCoord(). So this is a
	 * precondition of calling the current function.
	 * 
	 * @param series
	 *            The series number.
	 * @param item
	 *            The item number within the series.
	 * @return The error value associated with the mean.
	 */
	protected double getMagError(int series, int item) {
		if (series != this.meansSeriesNum) {
			// The series is something other than the means series
			// so just default to the superclass behaviour.
			return super.getMagError(series, item);
		} else {
			// For the means series, we store the mean magnitude error
			// value as the magnitude's uncertainty. TODO: change this?
			return this.seriesNumToObSrcListMap.get(series).get(item)
					.getMagnitude().getUncertainty();
		}
	}

	/**
	 * @return the meanSourceSeriesNum
	 */
	public int getMeanSourceSeriesNum() {
		return meanSourceSeriesNum;
	}

	/**
	 * @param meanSourceSeriesNum
	 *            the meanSourceSeriesNum to set
	 */
	public void setMeanSourceSeriesNum(int meanSourceSeriesNum) {
		this.meanSourceSeriesNum = meanSourceSeriesNum;
	}

	/**
	 * @return the means series number
	 */
	public int getMeansSeriesNum() {
		return meansSeriesNum;
	}

	/**
	 * @return the daysInBin
	 */
	public double getDaysInBin() {
		return daysInBin;
	}

	/**
	 * @param daysInBin
	 *            the daysInBin to set
	 */
	public void setDaysInBin(double daysInBin) {
		this.daysInBin = daysInBin;
	}

	/**
	 * @return the meanObsList
	 */
	public List<ValidObservation> getMeanObsList() {
		return meanObsList;
	}

	/**
	 * @return the meansChangeNotifier
	 */
	public Notifier<List<ValidObservation>> getMeansChangeNotifier() {
		return meansChangeNotifier;
	}

	/**
	 * Listen for valid observation change notification, e.g. an observation is
	 * marked as discrepant. Since a discrepant observation is ignored for
	 * statistical analysis purposes (see DescStats class), we need to
	 * re-calculate the means series.
	 */
	public void update(ValidObservation ob) {
		setMeanSeries();
	}

	// Helper methods.

	/**
	 * Determine which series will be the source of the mean series. Note that
	 * this may be changed subsequently. Visual bands are highest priority, and
	 * if not found, the first band will be chosen at random. TODO: should this
	 * be refined?
	 * 
	 * @return The series number on which to base the mean series.
	 */
	private int determineMeanSeriesSource() {
		int seriesNum = -1;
		
		// Look for Visual, then V.
		// TODO: what about Stromgren V, unfiltered with V zero point?

		for (String series : srcNameToSeriesNumMap.keySet()) {
			if (SeriesType.Visual.getName().equals(series)
					|| SeriesType.VISUAL.getName().equals(series)) {
				// Visual band
				seriesNum = srcNameToSeriesNumMap.get(series);
				break;
			}
		}

		if (seriesNum == -1) {
			for (String series : srcNameToSeriesNumMap.keySet()) {
				if (SeriesType.V.getName().equals(series)
						|| SeriesType.Johnson_V.getName().equals(series)) {
					// V band
					seriesNum = srcNameToSeriesNumMap.get(series);
					break;
				}
			}
		}

		// No match: choose some series other than "fainter than".
		if (seriesNum == -1) {
			for (String series : srcNameToSeriesNumMap.keySet()) {
				if (!SeriesType.FAINTER_THAN.getName().equalsIgnoreCase(series)) {
					seriesNum = srcNameToSeriesNumMap.get(series);
					break;
				}
			}
		}

		assert seriesNum != -1;
		
		return seriesNum;
	}
}
