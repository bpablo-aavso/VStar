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
package org.aavso.tools.vstar.ui.pane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.ui.dialog.ObservationDetailsDialog;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.ui.mediator.MessageBase;
import org.aavso.tools.vstar.ui.mediator.ObservationSelectionMessage;
import org.aavso.tools.vstar.ui.model.plot.ObservationPlotModel;
import org.aavso.tools.vstar.util.notification.Listener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;

/**
 * This class is the base class for chart panes containing a plot of a set of
 * valid observations. It is genericised on observation model.
 */
abstract public class AbstractObservationPlotPane<T extends ObservationPlotModel>
		extends JPanel implements ChartMouseListener, DatasetChangeListener {

	protected T obsModel;

	protected JFreeChart chart;

	protected ChartPanel chartPanel;

	protected JPanel chartControlPanel;

	protected JTextArea obsInfo;

	protected XYErrorRenderer renderer;

	// Show error bars?
	protected boolean showErrorBars;

	// Show cross-hairs?
	protected boolean showCrossHairs;

	protected JButton visibilityButton;

	// Axis titles.
	public static String JD_TITLE = "Time (JD)";
	public static String PHASE_TITLE = "Phase";
	public static String MAG_TITLE = "Brightness (magnitude)";

	/**
	 * Constructor
	 * 
	 * @param title
	 *            The title for the chart.
	 * @param subTitle
	 *            The sub-title for the chart.
	 * @param domainTitle
	 *            The domain title (e.g. Julian Date, phase).
	 * @param rangeTitle
	 *            The range title (e.g. magnitude).
	 * @param obsModel
	 *            The data model to plot.
	 * @param bounds
	 *            The bounding box to which to set the chart's preferred size.
	 */
	public AbstractObservationPlotPane(String title, String subTitle,
			String domainTitle, String rangeTitle, T obsModel, Dimension bounds) {
		super();

		this.obsModel = obsModel;

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		this.showErrorBars = true;
		this.showCrossHairs = true;

		// Create a chart with legend, tooltips, and URLs showing
		// and add it to the panel.
		this.chartPanel = new ChartPanel(ChartFactory.createScatterPlot(title,
				domainTitle, rangeTitle, obsModel, PlotOrientation.VERTICAL,
				true, true, true));

		this.chartPanel.setPreferredSize(bounds);

		this.chart = chartPanel.getChart();

		this.chart.addSubtitle(new TextTitle(subTitle));

		this.renderer = new VStarPlotDataRenderer();
		this.renderer.setDrawYError(this.showErrorBars);

		// Should reduce number of Java2D draw operations.
		// this.renderer.setDrawSeriesLineAsPath(true);

		// Tell renderer which series elements should be rendered
		// as visually joined with lines.
		// TODO: change return type of getter below to be Set<Integer>
		for (int series : obsModel
				.getSeriesWhoseElementsShouldBeJoinedVisually()) {
			this.renderer.setSeriesLinesVisible(series, true);
		}

		// Tell renderer which series' elements should initially be
		// rendered, i.e. visible.
		setSeriesVisibility();

		/*
		 * The motivation for this is that when a means series is added, it will
		 * be last in sequence and we want it to be rendered last. We could just
		 * do this in subclasses dealing with such a means series, but then this
		 * would make all other series renderings look different compared with
		 * other plots without means series. So, in short, we're doing this for
		 * consistency and with the knowledge of the use cases for plot pane
		 * classes.
		 */
		this.chart.getXYPlot().setSeriesRenderingOrder(
				SeriesRenderingOrder.FORWARD);
		// this.chart.getXYPlot().setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);

		chart.getXYPlot().setRenderer(renderer);

		// TODO: Hmm. A white background with no grids looks a bit barren.
		// this.chart.getXYPlot().setBackgroundPaint(Color.WHITE);

		setupCrossHairs();

		setSeriesColors();

		SeriesType.getSeriesColorChangeNotifier().addListener(
				createSeriesColorChangeListener());

		// We want the magnitude scale to go from high to low as we ascend the
		// Y axis since as magnitude values get smaller, brightness increases.
		NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
		rangeAxis.setInverted(true);

		setMagScale();

		obsModel.addChangeListener(this);

		this.add(chartPanel);
		this.add(Box.createRigidArea(new Dimension(0, 10)));

		// Create a panel that can be used to add chart control widgets.
		chartControlPanel = createChartControlPanel();
		this.add(chartControlPanel);

		Mediator.getInstance().getObservationSelectionNotifier().addListener(
				createObservationSelectionListener());
	}

	/**
	 * @return the chartPanel
	 */
	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	/**
	 * @return the chartControlPanel
	 */
	public JPanel getChartControlPanel() {
		return chartControlPanel;
	}

	/**
	 * @return the obsInfo
	 */
	public JTextArea getObsInfo() {
		return obsInfo;
	}

	/**
	 * @return the renderer
	 */
	public XYErrorRenderer getRenderer() {
		return renderer;
	}

	// Populate a panel that can be used to add chart control widgets.
	protected JPanel createChartControlPanel() {
		chartControlPanel = new JPanel();
		chartControlPanel.setLayout(new BoxLayout(chartControlPanel,
				BoxLayout.LINE_AXIS));

		chartControlPanel.setBorder(BorderFactory
				.createTitledBorder("Plot Control"));

		// A button to change series visibility.
		JButton visibilityButton = new JButton("Change Series");
		visibilityButton.addActionListener(createSeriesChangeButtonListener());
		chartControlPanel.add(visibilityButton);

		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setBorder(BorderFactory.createTitledBorder("Show"));
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel,
				BoxLayout.PAGE_AXIS));

		// A checkbox to show/hide error bars.
		JCheckBox errorBarCheckBox = new JCheckBox("Error bars?");
		errorBarCheckBox.setSelected(this.showErrorBars);
		errorBarCheckBox.addActionListener(createErrorBarCheckBoxListener());
		checkBoxPanel.add(errorBarCheckBox);

		// A checkbox to show/hide cross hairs.
		JCheckBox crossHairCheckBox = new JCheckBox("Cross-hairs?");
		crossHairCheckBox.setSelected(this.showCrossHairs);
		crossHairCheckBox.addActionListener(createCrossHairCheckBoxListener());
		checkBoxPanel.add(crossHairCheckBox);

		chartControlPanel.add(checkBoxPanel);

		return chartControlPanel;
	}

	/**
	 * Returns a listener for the error bar visibility checkbox.
	 */
	private ActionListener createErrorBarCheckBoxListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleErrorBars();
			}
		};
	}

	/**
	 * Returns a listener for the cross-hair visibility checkbox.
	 */
	private ActionListener createCrossHairCheckBoxListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleCrossHairs();
			}
		};
	}

	/**
	 * Returns a series color change listener.
	 */
	private Listener<Map<SeriesType, Color>> createSeriesColorChangeListener() {
		return new Listener<Map<SeriesType, Color>>() {
			// Update the series colors.
			public void update(Map<SeriesType, Color> info) {
				setSeriesColors();
			}

			public boolean canBeRemoved() {
				return true;
			}
		};
	}

	/**
	 * Show/hide the error bars.
	 */
	private void toggleErrorBars() {
		this.showErrorBars = !this.showErrorBars;
		this.renderer.setDrawYError(this.showErrorBars);
	}

	/**
	 * Show/hide the cross hairs.
	 */
	private void toggleCrossHairs() {
		this.showCrossHairs = !this.showCrossHairs;
		chart.getXYPlot().setDomainCrosshairVisible(this.showCrossHairs);
		chart.getXYPlot().setRangeCrosshairVisible(this.showCrossHairs);
	}

	/**
	 * Return a listener for the "change series visibility" button.
	 */
	abstract protected ActionListener createSeriesChangeButtonListener();

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
			delta |= obsModel.changeSeriesVisibility(seriesNum, visibility);
		}

		return delta;
	}

	/**
	 * Set the visibility of each series.
	 */
	private void setSeriesVisibility() {
		Map<Integer, Boolean> seriesVisibilityMap = obsModel
				.getSeriesVisibilityMap();

		for (int seriesNum : seriesVisibilityMap.keySet()) {
			renderer.setSeriesVisible(seriesNum, seriesVisibilityMap
					.get(seriesNum));
		}
	}

	/**
	 * Set the color of each series.
	 */
	private void setSeriesColors() {
		Map<Integer, SeriesType> seriesNumToTypeMap = obsModel
				.getSeriesNumToSrcTypeMap();

		for (int seriesNum : seriesNumToTypeMap.keySet()) {
			Color color = SeriesType.getColorFromSeries(seriesNumToTypeMap
					.get(seriesNum));
			renderer.setSeriesPaint(seriesNum, color);
		}
	}

	// Cross hair handling methods.

	private void setupCrossHairs() {
		chart.getXYPlot().setDomainCrosshairValue(0);
		chart.getXYPlot().setRangeCrosshairValue(0);

		chart.getXYPlot().setDomainCrosshairVisible(true);
		chart.getXYPlot().setRangeCrosshairVisible(true);

		chartPanel.addChartMouseListener(this);
	}

	// From ChartMouseListener.
	// If the user clicks on a plot point, send a selection message,
	// open an information dialog.
	public void chartMouseClicked(ChartMouseEvent event) {
		if (event.getEntity() instanceof XYItemEntity) {
			XYItemEntity entity = (XYItemEntity) event.getEntity();
			int series = entity.getSeriesIndex();
			int item = entity.getItem();
			ValidObservation ob = obsModel.getValidObservation(series, item);

			new ObservationDetailsDialog(ob);

			ObservationSelectionMessage message = new ObservationSelectionMessage(
					ob, this);
			Mediator.getInstance().getObservationSelectionNotifier()
					.notifyListeners(message);
		}
	}

	// Returns an observation selection listener specific to the concrete plot
	// pane.
	abstract protected Listener<ObservationSelectionMessage> createObservationSelectionListener();

	// From ChartMouseListener
	public void chartMouseMoved(ChartMouseEvent arg0) {
		// Nothing to do here.
		// TODO: Zoom?
	}

	/**
	 * From DatasetChangeListener.
	 * 
	 * When the dataset changes, e.g. series visibility, we want to set the
	 * appropriate magnitude value range, ignoring any series that is not
	 * visible. We also make sure that the loaded series colors are all set.
	 */
	public void datasetChanged(DatasetChangeEvent event) {
		setSeriesVisibility();
		setSeriesColors();
		setMagScale();
	}

	// Helpers

	/**
	 * Set the appropriate magnitude value scale, ignoring any series that is
	 * not visible.
	 * 
	 * Note: for large datasets, this could be very expensive! Should maintain
	 * last min and max and only check observations for bands that have changed.
	 */
	private void setMagScale() {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		Map<Integer, List<ValidObservation>> seriesNumToObsMap = obsModel
				.getSeriesNumToObSrcListMap();

		Map<Integer, Boolean> seriesVisibilityMap = obsModel
				.getSeriesVisibilityMap();

		for (int series : seriesNumToObsMap.keySet()) {
			if (seriesVisibilityMap.get(series)) {

				List<ValidObservation> obs = seriesNumToObsMap.get(series);
				for (ValidObservation ob : obs) {
					double mag = ob.getMagnitude().getMagValue();
					double uncert = ob.getMagnitude().getUncertainty();
					//here if getUncertainty is null then get HQ
					if(uncert == 0.0){
						uncert = ob.getHqUncertainty();
					}

					if (mag-uncert < min) {
						min = mag-uncert;
					}

					if (mag+uncert > max) {
						max = mag+uncert;
					}
				}
			}
		}

		boolean obsToPlot = min <= max;

		if (obsToPlot) {
			if (min == max) {
				// For just one observation we will simply have one point at the
				// centre of the range.
				double mag = min;
				min = mag - 1;
				max = mag + 1;
			}

			// Add a small (1%) margin around min/max.
			double margin = (max - min) / 100;
			min -= margin;
			max += margin;

			NumberAxis magAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
			magAxis.setRange(new Range(min, max));
		}
	}
}
