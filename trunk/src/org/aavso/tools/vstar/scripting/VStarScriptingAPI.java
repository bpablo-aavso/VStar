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
package org.aavso.tools.vstar.scripting;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.exception.AlgorithmError;
import org.aavso.tools.vstar.exception.ObservationReadError;
import org.aavso.tools.vstar.input.database.VSXWebServiceStarInfoSource;
import org.aavso.tools.vstar.plugin.InputType;
import org.aavso.tools.vstar.plugin.ObservationSourcePluginBase;
import org.aavso.tools.vstar.plugin.ob.src.impl.VSXWebServiceAIDObservationSourcePlugin;
import org.aavso.tools.vstar.ui.MenuBar;
import org.aavso.tools.vstar.ui.dialog.MessageBox;
import org.aavso.tools.vstar.ui.mediator.AnalysisType;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.ui.mediator.StarInfo;
import org.aavso.tools.vstar.ui.mediator.ViewModeType;
import org.aavso.tools.vstar.ui.mediator.message.AnalysisTypeChangeMessage;
import org.aavso.tools.vstar.ui.model.plot.ObservationAndMeanPlotModel;
import org.aavso.tools.vstar.ui.resources.PluginLoader;
import org.aavso.tools.vstar.util.notification.Listener;
import org.aavso.tools.vstar.util.period.PeriodAnalysisCoordinateType;
import org.aavso.tools.vstar.util.period.dcdft.DcDftAnalysisType;
import org.aavso.tools.vstar.util.period.dcdft.TSDcDft;
import org.aavso.tools.vstar.util.period.wwz.WWZCoordinateType;
import org.aavso.tools.vstar.util.period.wwz.WWZStatistic;
import org.aavso.tools.vstar.util.period.wwz.WeightedWaveletZTransform;

/**
 * This is VStar's scripting Application Programming Interface. An instance of
 * this class will be passed to scripts.
 * 
 * All methods are synchronised to ensure that only one API method is being
 * called at a time.
 */
public class VStarScriptingAPI {

	private Mediator mediator = Mediator.getInstance();

	private final static VStarScriptingAPI instance = new VStarScriptingAPI();

	private AnalysisTypeChangeMessage analysisTypeMsg;

	/**
	 * Constructor
	 */
	private VStarScriptingAPI() {
		mediator = Mediator.getInstance();
		analysisTypeMsg = null;
		mediator.getAnalysisTypeChangeNotifier().addListener(
				createAnalysisTypeChangeListener());
	}

	/**
	 * Return Singleton.
	 */
	public static VStarScriptingAPI getInstance() {
		return instance;
	}

	/**
	 * Return an analysis type change listener.
	 */
	private Listener<AnalysisTypeChangeMessage> createAnalysisTypeChangeListener() {
		return new Listener<AnalysisTypeChangeMessage>() {
			@Override
			public void update(AnalysisTypeChangeMessage info) {
				analysisTypeMsg = info;
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	// ***************************************
	// ** VStar scripting API methods start **
	// ***************************************

	/**
	 * Load a dataset from the specified path. This is equivalent to
	 * "File -> New Star from File..."
	 * 
	 * @param path
	 *            The path to the file.
	 */
	public synchronized void loadFromFile(final String path) {
		commonLoadFromFile(path, false);
	}

	/**
	 * Load a dataset from the specified path, adding it to the existing
	 * dataset. This is equivalent to "File -> New Star from File..." with the
	 * additive checkbox selected.
	 * 
	 * @param path
	 *            The path to the file.
	 */
	public synchronized void additiveLoadFromFile(final String path) {
		commonLoadFromFile(path, true);
	}

	/**
	 * Load a dataset from the specified path. This is equivalent to
	 * "File -> New Star from File..." with URL requested.
	 * 
	 * @param url
	 *            The URL of the file.
	 */
	public synchronized void loadFromURL(final String path) {
		commonLoadFromURL(path, false);
	}

	/**
	 * Load a dataset from the specified path, adding it to the existing
	 * dataset. This is equivalent to "File -> New Star from File..." with the
	 * additive checkbox selected and URL requested.
	 * 
	 * @param url
	 *            The URL of the file.
	 */
	public synchronized void additiveLoadFromURL(final String path) {
		commonLoadFromURL(path, true);
	}

	/**
	 * Load a dataset from the specified path using the (possibly partial)
	 * plug-in name to identify the observation source plug-in to use. This is
	 * equivalent to "File -> New Star from <obs-source-type>..."
	 * 
	 * @param pluginName
	 *            The sub-string with which to match the plug-in name.
	 * @param path
	 *            The path to the file.
	 */
	public synchronized void loadFromFile(final String pluginName,
			final String path) {
		commonLoadFromFileOrURLViaPlugin(pluginName, InputType.FILE, path,
				false);
	}

	/**
	 * Load a dataset from the specified path, adding it to the existing dataset
	 * using the (possibly partial) plug-in name to identify the observation
	 * source plug-in to use. This is equivalent to
	 * "File -> New Star from <obs-source-type>..." with the additive checkbox
	 * selected.
	 * 
	 * @param pluginName
	 *            The sub-string with which to match the plug-in name.
	 * @param path
	 *            The path to the file.
	 */
	public synchronized void additiveLoadFromFile(final String pluginName,
			final String path) {
		commonLoadFromFileOrURLViaPlugin(pluginName, InputType.FILE, path, true);
	}

	/**
	 * Load a dataset from the specified URL using the (possibly partial)
	 * plug-in name to identify the observation source plug-in to use.
	 * 
	 * @param pluginName
	 *            The sub-string with which to match the plug-in name.
	 * @param url
	 *            The URL.
	 */
	public synchronized void loadFromURL(final String pluginName,
			final String url) {
		commonLoadFromFileOrURLViaPlugin(pluginName, InputType.URL, url, false);
	}

	/**
	 * Load a dataset from the specified URL, adding it to the existing dataset,
	 * using the (possibly partial) plug-in name to identify the observation
	 * source plug-in to use.
	 * 
	 * @param pluginName
	 *            The sub-string with which to match the plug-in name.
	 * @param url
	 *            The URL.
	 */
	public synchronized void additiveLoadFromURL(final String pluginName,
			final String url) {
		commonLoadFromFileOrURLViaPlugin(pluginName, InputType.URL, url, true);
	}

	/**
	 * Load a dataset from the AAVSO international database.
	 * 
	 * @param name
	 *            The name (not AUID) of the object.
	 * @param minJD
	 *            The minimum JD of the range to be loaded.
	 * @param maxJD
	 *            The maximum JD of the range to be loaded.
	 */
	public synchronized void loadFromAID(final String name, double minJD,
			double maxJD) {

		commonLoadFromAID(name, minJD, maxJD, false);
	}

	/**
	 * Load a dataset from the AAVSO international database, adding it to the
	 * existing dataset.
	 * 
	 * @param name
	 *            The name (not AUID) of the object.
	 * @param minJD
	 *            The minimum JD of the range to be loaded.
	 * @param maxJD
	 *            The maximum JD of the range to be loaded.
	 */
	public synchronized void additiveLoadFromAID(final String name,
			double minJD, double maxJD) {

		commonLoadFromAID(name, minJD, maxJD, true);
	}

	// TODO: add loadFromAID(name) => all

	/**
	 * Return a StarInfo object for named object.
	 * 
	 * @param name
	 *            The name (not AUID) of the object.
	 * @return The StarInfo object.
	 */
	public synchronized StarInfo getStarInfo(String name) {
		init();
		VSXWebServiceStarInfoSource infoSrc = new VSXWebServiceStarInfoSource();
		return infoSrc.getStarByName(null, name);
	}

	/**
	 * Save the raw or phase plot dataset (according to current mode) to a file
	 * of rows of values separated by the specified delimiter.
	 * 
	 * @param path
	 *            The path to the file to save to (as a string).
	 * @param delimiter
	 *            The delimiter between data items.
	 */
	public synchronized void saveObsList(final String path, String delimiter) {
		init();
		mediator.saveObsListToFile(Mediator.getUI().getComponent(), new File(
				path), delimiter);
		mediator.waitForJobCompletion();
	}

	/**
	 * Save the raw or phase plot mean list (according to current mode) to a
	 * file of rows of values separated by the specified delimiter.
	 * 
	 * @param path
	 *            The path to the file to save to (as a string).
	 * @param delimiter
	 *            The delimiter between data items.
	 */
	public synchronized void saveMeanList(final String path, String delimiter) {
		init();
		mediator.saveSyntheticObsListToFile(Mediator.getUI().getComponent(),
				ViewModeType.LIST_MEANS_MODE, new File(path), delimiter);
		mediator.waitForJobCompletion();
	}

	/**
	 * Save the raw or phase plot model list (according to current mode) to a
	 * file of rows of values separated by the specified delimiter.
	 * 
	 * @param path
	 *            The path to the file to save to (as a string).
	 * @param delimiter
	 *            The delimiter between data items.
	 */
	public synchronized void saveModelList(final String path, String delimiter) {
		init();
		mediator.saveSyntheticObsListToFile(Mediator.getUI().getComponent(),
				ViewModeType.MODEL_MODE, new File(path), delimiter);
		mediator.waitForJobCompletion();
	}

	/**
	 * Save the raw or phase plot residual list (according to current mode) to a
	 * file of rows of values separated by the specified delimiter.
	 * 
	 * @param path
	 *            The path to the file to save to (as a string).
	 * @param delimiter
	 *            The delimiter between data items.
	 */
	public synchronized void saveResidualList(final String path,
			String delimiter) {
		init();
		mediator.saveSyntheticObsListToFile(Mediator.getUI().getComponent(),
				ViewModeType.RESIDUALS_MODE, new File(path), delimiter);
		mediator.waitForJobCompletion();
	}

	/**
	 * Save the light curve for the current view mode (raw or phase plot) to a
	 * PNG image file.
	 * 
	 * @param path
	 *            The path to the file to save to (as a string).
	 * @param width
	 *            The desired width of the image.
	 * @param height
	 *            The desired height of the image.
	 */
	public synchronized void saveLightCurve(final String path, int width,
			int height) {
		init();
		mediator.saveCurrentPlotToFile(new File(path), width, height);
	}

	/**
	 * Switch to phase plot mode. If no phase plot has been created yet, this
	 * will open the phase parameter dialog.
	 */
	public synchronized void phasePlotMode() {
		init();
		mediator.changeAnalysisType(AnalysisType.PHASE_PLOT);
		mediator.waitForJobCompletion();
	}

	/**
	 * Switch to phase raw (light curve) mode.
	 */
	public synchronized void lightCurveMode() {
		init();
		mediator.changeAnalysisType(AnalysisType.RAW_DATA);
		mediator.waitForJobCompletion();
	}

	/**
	 * Create a phase plot given period and epoch.
	 * 
	 * @param period
	 *            The period on which to fold the data.
	 * @param epoch
	 *            The epoch (first Julian Date) for the phase plot.
	 */
	public synchronized void phasePlot(double period, double epoch) {
		init();
		mediator.createPhasePlot(period, epoch);
		mediator.waitForJobCompletion();
	}

	/**
	 * Perform DCDFT period analysis with period range.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @param lowPeriod
	 *            The low value of the period range to search in.
	 * @param highPeriod
	 *            The high value of the period range to search in.
	 * @param resolution
	 *            The resolution of the search over the range.
	 * @return An array of top-hits periods.
	 */
	public synchronized Double[] dcdftPeriod(String seriesName,
			double lowPeriod, double highPeriod, double resolution) {

		return dcdftCommon(seriesName, DcDftAnalysisType.PERIOD_RANGE,
				lowPeriod, highPeriod, resolution);
	}

	/**
	 * Perform DCDFT period analysis with frequency range.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @param lowFrequency
	 *            The low value of the frequency range to search in.
	 * @param highPeriod
	 *            The high value of the frequency range to search in.
	 * @param resolution
	 *            The resolution of the search over the range.
	 * @return An array of top-hits frequencies.
	 */
	public synchronized Double[] dcdftFrequency(String seriesName,
			double lowFrequency, double highFrequency, double resolution) {

		return dcdftCommon(seriesName, DcDftAnalysisType.FREQUENCY_RANGE,
				lowFrequency, highFrequency, resolution);
	}

	/**
	 * Perform DCDFT period analysis with standard scan.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @return An array of top-hits frequencies.
	 */
	public synchronized Double[] dcdftStandardScan(String seriesName) {

		return dcdftCommon(seriesName, DcDftAnalysisType.STANDARD_SCAN, 0, 0, 0);
	}

	/**
	 * Perform WWZ time-frequency analysis with period range.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @param minPeriod
	 *            The low value of the period range to search in.
	 * @param maxPeriod
	 *            The high value of the period range to search in.
	 * @param periodStep
	 *            The resolution of the search over the range.
	 * @param decay
	 *            The wavelet decay constant to use.
	 * @param timeDivisions
	 *            The number of time divisions to use.
	 * @return An array of top-hits periods; may be empty.
	 */
	public synchronized Double[][] wwzPeriod(String seriesName,
			double minPeriod, double maxPeriod, double periodStep,
			double decay, double timeDivisions) {

		List<ValidObservation> obs = getObsForSeries(seriesName);

		Double[][] results = {};

		if (obs.size() > 0) {
			WeightedWaveletZTransform wwz = new WeightedWaveletZTransform(obs,
					decay, timeDivisions);

			wwz.make_freqs_from_period_range(Math.min(minPeriod, maxPeriod),
					Math.max(minPeriod, maxPeriod), periodStep);

			results = wwzCommon(wwz, WWZCoordinateType.PERIOD);
		}

		return results;
	}

	/**
	 * Perform WWZ time-frequency analysis with period range.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @param minFreq
	 *            The low value of the frequency range to search in.
	 * @param maxFreq
	 *            The high value of the frequency range to search in.
	 * @param freqStep
	 *            The resolution of the search over the range.
	 * @param decay
	 *            The wavelet decay constant to use.
	 * @param timeDivisions
	 *            The number of time divisions to use.
	 * @return An array of top-hits frequencies; may be empty.
	 */
	public synchronized Double[][] wwzFrequency(String seriesName,
			double minFreq, double maxFreq, double freqStep, double decay,
			double timeDivisions) {

		List<ValidObservation> obs = getObsForSeries(seriesName);

		Double[][] results = {};

		if (obs.size() > 0) {
			WeightedWaveletZTransform wwz = new WeightedWaveletZTransform(obs,
					decay, timeDivisions);

			wwz.make_freqs_from_freq_range(Math.min(minFreq, maxFreq),
					Math.max(minFreq, maxFreq), freqStep);

			results = wwzCommon(wwz, WWZCoordinateType.FREQUENCY);
		}

		return results;
	}

	/**
	 * Return the last error.
	 * 
	 * @return The error string; may be null.
	 */
	public synchronized String getError() {
		return ScriptRunner.getInstance().getError();
	}

	/**
	 * Return the last warning.
	 * 
	 * @return The warning string; may be null.
	 */
	public synchronized String getWarning() {
		return ScriptRunner.getInstance().getWarning();
	}

	/**
	 * Returns a comma-separated string of series names for the current dataset,
	 * including bands and synthetic series such as means, model, residuals.
	 * 
	 * @return a comma-separated series names
	 */
	public synchronized String getSeries() {
		init();
		ObservationAndMeanPlotModel model = analysisTypeMsg
				.getObsAndMeanChartPane().getObsModel();

		String nameStr = "";

		for (SeriesType type : model.getSeriesKeys()) {
			nameStr += type.getShortName() + ",";
		}

		return nameStr.substring(0, nameStr.lastIndexOf(",") - 1);
	}

	/**
	 * Makes the specified series in the current dataset visible. Calling this
	 * method more than one consecutive time with the same visibility value has
	 * no effect on the current visibility status of a series.
	 * 
	 * @param seriesName
	 *            The long name (e.g. "Johnson V" not "V") of the series to make
	 *            visible.
	 */
	public synchronized void makeVisible(final String seriesName) {
		init();
		ObservationAndMeanPlotModel obsPlotModel = analysisTypeMsg
				.getObsAndMeanChartPane().getObsModel();

		if (SeriesType.exists(seriesName)) {
			SeriesType series = SeriesType.getSeriesFromDescription(seriesName);

			if (obsPlotModel.getSeriesKeys().contains(series)) {
				int seriesNum = obsPlotModel.getSrcTypeToSeriesNumMap().get(
						series);
				obsPlotModel.changeSeriesVisibility(seriesNum, true);
			} else {
				ScriptRunner.getInstance().setError(
						"Series does not exist in loaded dataset: "
								+ seriesName);
			}
		} else {
			ScriptRunner.getInstance().setError(
					"Unknown series type: " + seriesName);
		}

		mediator.waitForJobCompletion();
	}

	// TODO:
	// - makeInvisible()
	// - allow AoV, other period search plugins: see
	// commonLoadFromFileOrURLViaPlugin() re: pattern;
	// API may need to change to accommodate this, e.g.
	// a generic way to get results as a collection

	/**
	 * Pause for the specified number of milliseconds.
	 * 
	 * @param millis
	 *            The time to pause for.
	 */
	public synchronized void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Exit VStar
	 */
	public synchronized void exit() {
		mediator.quit();
	}

	// *************************************
	// ** VStar scripting API methods end **
	// *************************************

	// Common methods

	/**
	 * Common dataset file load method.
	 * 
	 * @param path
	 *            The path to the file or URL.
	 * @param isAdditive
	 *            Is this load additive?
	 */
	private void commonLoadFromFile(final String path, boolean isAdditive) {
		init();

		commonLoadFromFileOrURLViaPlugin(MenuBar.NEW_STAR_FROM_FILE,
				InputType.FILE, path, isAdditive);
	}

	/**
	 * Common dataset URL load method.
	 * 
	 * @param url
	 *            The URL of the file.
	 * @param isAdditive
	 *            Is this load additive?
	 */
	private void commonLoadFromURL(final String url, boolean isAdditive) {
		init();

		commonLoadFromFileOrURLViaPlugin(MenuBar.NEW_STAR_FROM_FILE,
				InputType.URL, url, isAdditive);
	}

	/**
	 * Common dataset plug-in load method.
	 * 
	 * @param pluginName
	 *            The sub-string with which to match the plug-in name.
	 * @param inputType
	 *            The input type (e.g. file, URL).
	 * @param location
	 *            The path or URL to the file.
	 * @param isAdditive
	 *            Is this load additive?
	 */
	private void commonLoadFromFileOrURLViaPlugin(final String pluginName,
			InputType inputType, final String location, boolean isAdditive) {
		init();

		ObservationSourcePluginBase obSourcePlugin = null;

		for (ObservationSourcePluginBase plugin : PluginLoader
				.getObservationSourcePlugins()) {
			if (plugin.getDisplayName().contains(pluginName)
					&& (plugin.getInputType() == inputType || plugin
							.getInputType() == InputType.FILE_OR_URL)) {
				obSourcePlugin = plugin;
				break;
			}
		}

		if (obSourcePlugin != null) {
			try {
				if (inputType == InputType.FILE) {
					mediator.createObservationArtefactsFromObSourcePlugin(
							obSourcePlugin, new File(location), isAdditive);
				} else if (inputType == InputType.URL) {
					mediator.createObservationArtefactsFromObSourcePlugin(
							obSourcePlugin, new URL(location), isAdditive);

				}
			} catch (IOException e) {
				MessageBox.showErrorDialog("Load File", "Cannot load file: "
						+ location);
			} catch (ObservationReadError e) {
				MessageBox.showErrorDialog("Load File",
						"Error reading observations from file: " + location
								+ " (reason: " + e.getLocalizedMessage() + ")");
			}
		} else {
			MessageBox
					.showErrorDialog("Load File",
							"No matching observation plugin found '"
									+ pluginName + "'");
		}

		mediator.waitForJobCompletion();
	}

	/**
	 * Common AID dataset load method.
	 * 
	 * @param name
	 *            The target name (not AUID).
	 * @param minJD
	 *            The minimum JD of the range to be loaded.
	 * @param maxJD
	 *            The maximum JD of the range to be loaded.
	 * @param isAdditive
	 *            Is this load additive?
	 */
	private void commonLoadFromAID(final String name, double minJD,
			double maxJD, boolean isAdditive) {
		init();

		ObservationSourcePluginBase obSourcePlugin = null;

		for (ObservationSourcePluginBase plugin : PluginLoader
				.getObservationSourcePlugins()) {
			if (plugin.getDisplayName()
					.contains(MenuBar.NEW_STAR_FROM_DATABASE)) {
				obSourcePlugin = plugin;
				break;
			}
		}

		if (obSourcePlugin != null) {
			try {
				VSXWebServiceStarInfoSource infoSrc = new VSXWebServiceStarInfoSource();
				StarInfo info = infoSrc.getStarByName(null, name);

				String url = VSXWebServiceAIDObservationSourcePlugin
						.createAIDUrlForAUID(info.getAuid(), minJD, maxJD);

				VSXWebServiceAIDObservationSourcePlugin aidPlugin = (VSXWebServiceAIDObservationSourcePlugin) obSourcePlugin;
				aidPlugin.setUrl(url);
				aidPlugin.setInfo(info);

				mediator.createObservationArtefactsFromObSourcePlugin(
						aidPlugin, (URL) null, isAdditive);
			} catch (IOException e) {
				MessageBox.showErrorDialog("Load from AID",
						"Cannot load from AID:  " + name);
			} catch (ObservationReadError e) {
				MessageBox.showErrorDialog("Load from AID",
						"Error reading observations from AID: " + name
								+ " (reason: " + e.getLocalizedMessage() + ")");
			}
		} else {
			MessageBox.showErrorDialog("Load from AID",
					"Error initialising load from AID plug-in");
		}

		mediator.waitForJobCompletion();
	}

	/**
	 * Perform DCDFT period analysis.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @param analysisType
	 *            Period range, frequency range, standard scan?
	 * @param lowPeriod
	 *            The low value of the period range to search in.
	 * @param highPeriod
	 *            The high value of the period range to search in.
	 * @param resolution
	 *            The resolution of the search over the range.
	 * @return An array of top-hits periods or frequencies.
	 */
	private synchronized Double[] dcdftCommon(String seriesName,
			DcDftAnalysisType analysisType, double low, double high,
			double resolution) {

		init();

		Double[] topHitPeriods = null;

		List<ValidObservation> obs = getObsForSeries(seriesName);

		if (obs.size() > 0) {
			TSDcDft dcdft = null;

			switch (analysisType) {
			case PERIOD_RANGE:
				dcdft = new TSDcDft(obs, DcDftAnalysisType.PERIOD_RANGE);
				dcdft.setLoPeriodValue(low);
				dcdft.setHiPeriodValue(high);
				dcdft.setResolutionValue(resolution);
				break;

			case FREQUENCY_RANGE:
				dcdft = new TSDcDft(obs, low, high, resolution);
				break;

			case STANDARD_SCAN:
				dcdft = new TSDcDft(obs);
				break;
			}

			try {
				dcdft.execute();

				Map<PeriodAnalysisCoordinateType, List<Double>> topHits = dcdft
						.getTopHits();

				PeriodAnalysisCoordinateType coordType = null;

				if (analysisType == DcDftAnalysisType.PERIOD_RANGE) {
					coordType = PeriodAnalysisCoordinateType.PERIOD;
				} else {
					coordType = PeriodAnalysisCoordinateType.FREQUENCY;
				}

				// Get array of top-hit periods or frequencies.
				topHitPeriods = topHits.get(coordType).toArray(new Double[0]);

			} catch (AlgorithmError e) {
				ScriptRunner.getInstance().setError(e.getMessage());
			}
		} else {
			ScriptRunner.getInstance().setError(
					"No observations in series " + seriesName);
		}

		return topHitPeriods;
	}

	/**
	 * Perform WWZ period analysis.
	 * 
	 * @param wwz
	 *            The initialised (WWZ transform object.
	 * @param coordType
	 *            The coordinate type: period or frequency.
	 * @return An array of top-hits times and periods or frequencies.
	 */
	private synchronized Double[][] wwzCommon(WeightedWaveletZTransform wwz,
			WWZCoordinateType coordType) {

		init();

		Double[][] maximalStats = null;

		try {
			wwz.execute();

			List<WWZStatistic> maximalStatsList = wwz.getMaximalStats();

			maximalStats = new Double[maximalStatsList.size()][2];

			int i = 0;
			for (WWZStatistic stat : maximalStatsList) {
				Double[] pair = new Double[2];
				pair[0] = stat.getTau();

				switch (coordType) {
				case FREQUENCY:
					pair[1] = stat.getFrequency();
					break;
				case PERIOD:
					pair[1] = stat.getPeriod();
					break;
				default:
					throw new IllegalArgumentException(
							"WWZ: only period or frequency allowed in output");
				}

				maximalStats[i++] = pair;
			}
		} catch (AlgorithmError e) {
			ScriptRunner.getInstance().setError(e.getMessage());
		}

		return maximalStats;
	}

	/**
	 * Given a series name, return a list of observations for the series.
	 * 
	 * @param seriesName
	 *            The short or long form of the series name, e.g. V or Johnson
	 *            V.
	 * @return A list of valid observations for the series; may be empty.
	 */
	private List<ValidObservation> getObsForSeries(String seriesName) {

		List<ValidObservation> obs = Collections.emptyList();

		// Find the requested series...
		SeriesType series = SeriesType.getSeriesFromShortName(seriesName);

		if (series == SeriesType.getDefault()) {
			series = SeriesType.getSeriesFromDescription(seriesName);
		}

		// ...if the user wasn't really asking for the default series name but
		// we got it anyway, we treat this as an error.
		if (series == SeriesType.getDefault()
				&& !SeriesType.getDefault().getDescription()
						.equals(seriesName.toLowerCase())) {
			ScriptRunner.getInstance().setError("Unknown series " + seriesName);
		} else {
			obs = Mediator.getInstance().getLatestNewStarMessage()
					.getObsCategoryMap().get(series);
			if (obs.size() == 0) {
				ScriptRunner.getInstance().setError(
						"No observations in series " + seriesName);
			}
		}

		return obs;
	}

	// Helpers

	private void init() {
		clearError();
	}

	private void clearError() {
		ScriptRunner.getInstance().setError(null);
	}
}
