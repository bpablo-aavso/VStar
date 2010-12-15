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
package org.aavso.tools.vstar.ui.pane.plot;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import org.aavso.tools.vstar.ui.mediator.AnalysisType;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.ui.mediator.ViewModeType;
import org.aavso.tools.vstar.ui.mediator.message.FilteredObservationMessage;
import org.aavso.tools.vstar.ui.mediator.message.NewStarMessage;
import org.aavso.tools.vstar.ui.mediator.message.ObservationSelectionMessage;
import org.aavso.tools.vstar.ui.mediator.message.PanRequestMessage;
import org.aavso.tools.vstar.ui.mediator.message.PolynomialFitMessage;
import org.aavso.tools.vstar.ui.mediator.message.ZoomRequestMessage;
import org.aavso.tools.vstar.ui.model.plot.IVisibilityMapSource;
import org.aavso.tools.vstar.ui.model.plot.ObservationAndMeanPlotModel;
import org.aavso.tools.vstar.ui.model.plot.PhaseTimeElementEntity;
import org.aavso.tools.vstar.util.notification.Listener;
import org.aavso.tools.vstar.util.stats.BinningResult;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

/**
 * This class represents a chart pane containing a phase plot for a set of valid
 * observations along with mean-based data.
 */
public class PhaseAndMeanPlotPane extends ObservationAndMeanPlotPane implements
		IVisibilityMapSource {

	/**
	 * Constructor.
	 * 
	 * @param title
	 *            The title for the chart.
	 * @param subTitle
	 *            The sub-title for the chart.
	 * @param obsAndMeanModel
	 *            The data model to plot.
	 * @param bounds
	 *            The bounding box to which to set the chart's preferred size.
	 */
	public PhaseAndMeanPlotPane(String title, String subTitle,
			ObservationAndMeanPlotModel obsAndMeanModel, Dimension bounds) {

		super(title, subTitle, PHASE_TITLE, MAG_TITLE, obsAndMeanModel,
				new TimeElementsInBinSettingPane(
						"Phase Steps per Mean Series Bin", obsAndMeanModel,
						PhaseTimeElementEntity.instance), bounds);

		this.getChartControlPanel().add(new NewPhasePlotButtonPane(this));
	}

	// Return a mapping from series number to visibility status,
	// filtering out the means series. The means series is always
	// visible, so we are simply excluding this from consideration
	// (in the context of creating a phase plot, see NewPhasePlotButtonPane).
	public Map<Integer, Boolean> getVisibilityMap() {
		Map<Integer, Boolean> visibilityMap = obsModel.getSeriesVisibilityMap();
		Map<Integer, Boolean> visibilityMapWithoutMeans = new HashMap<Integer, Boolean>();
		for (Integer seriesNum : visibilityMap.keySet()) {
			if (seriesNum != obsModel.getMeanSourceSeriesNum()) {
				visibilityMapWithoutMeans.put(seriesNum, visibilityMap
						.get(seriesNum));
			}
		}

		return null;
	}

	// Returns an observation selection listener.
	protected Listener<ObservationSelectionMessage> createObservationSelectionListener() {
		return new Listener<ObservationSelectionMessage>() {

			public void update(ObservationSelectionMessage message) {
				// Move the cross hairs if we have phase information since
				// this plot's domain is phase.
				if (message.getSource() != this
						&& message.getObservation().getStandardPhase() != null) {
					chart.getXYPlot().setDomainCrosshairValue(
							message.getObservation().getStandardPhase());
					chart.getXYPlot().setRangeCrosshairValue(
							message.getObservation().getMag());
				}
			}

			public boolean canBeRemoved() {
				return true;
			}
		};
	}

	// Returns a zoom request listener.
	protected Listener<ZoomRequestMessage> createZoomRequestListener() {
		return new Listener<ZoomRequestMessage>() {
			public void update(ZoomRequestMessage info) {
				if (Mediator.getInstance().getAnalysisType() == AnalysisType.PHASE_PLOT
						&& Mediator.getInstance().getViewMode() == ViewModeType.PLOT_OBS_AND_MEANS_MODE) {
					doZoom(info.getZoomType());
				}
			}

			public boolean canBeRemoved() {
				return true;
			}
		};
	}

	// Returns a pan request listener.
	protected Listener<PanRequestMessage> createPanRequestListener() {
		return new Listener<PanRequestMessage>() {
			@Override
			public void update(PanRequestMessage msg) {
				final PlotRenderingInfo plotInfo = chartPanel
						.getChartRenderingInfo().getPlotInfo();

				final Point2D source = new Point2D.Double(0, 0);

				double percentage = 0.01;

				XYPlot plot = chart.getXYPlot();
				NewStarMessage newStarMsg = Mediator.getInstance()
						.getNewStarMessage();

				switch (msg.getPanType()) {
				case LEFT:
					if (plot.getDomainAxis().getLowerBound() >= -1) {
						plot.panDomainAxes(-percentage, plotInfo, source);
					}
					break;
				case RIGHT:
					if (plot.getDomainAxis().getUpperBound() <= 1) {
						plot.panDomainAxes(percentage, plotInfo, source);
					}
					break;
				case UP:
					if (newStarMsg.getMinMag() <= plot.getRangeAxis()
							.getLowerBound()) {
						plot.panRangeAxes(percentage, plotInfo, source);
					}
					break;
				case DOWN:
					if (newStarMsg.getMaxMag() >= plot.getRangeAxis()
							.getUpperBound()) {
						plot.panRangeAxes(-percentage, plotInfo, source);
					}
					break;
				}
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	@Override
	protected Listener<FilteredObservationMessage> createFilteredObservationListener() {
		return new Listener<FilteredObservationMessage>() {
			public void update(FilteredObservationMessage info) {
				// Do nothing for phase plots currently.
				// When we do eventually enable this,
				// this method should only do something
				// if a phase plot has been created, otherwise
				// we see assertion errors from PhaseCoordSource.getXCoord()
				// since phase values will be null.
			}

			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	// Returns a polynomial fit listener.
	protected Listener<PolynomialFitMessage> createPolynomialFitListener() {
		return new Listener<PolynomialFitMessage>() {

			@Override
			public void update(PolynomialFitMessage info) {
				// Do nothing for phase plots currently.
				// When we do eventually enable this,
				// this method should only do something
				// if a phase plot has been created, otherwise
				// we see assertion errors from PhaseCoordSource.getXCoord()
				// since phase values will be null.
			}

			@Override
			public boolean canBeRemoved() {
				return true;
			}
		};
	}

	// Returns a mean observation change (binning result) listener.
	protected Listener<BinningResult> createBinChangeListener() {
		return new Listener<BinningResult>() {
			public void update(BinningResult info) {
				// Do nothing. We may want to show ANOVA here, but I need to
				// understand whether this is useful for a phase plot by reading
				// more and talking with Grant et al.
			}

			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	protected void updateAnovaSubtitle(BinningResult binningResult) {
		// Do nothing. See createBinChangeListener().
	}
}
