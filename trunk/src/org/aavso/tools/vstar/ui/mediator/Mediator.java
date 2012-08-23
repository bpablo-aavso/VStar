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
package org.aavso.tools.vstar.ui.mediator;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.SwingWorker;
import javax.swing.JTable.PrintMode;

import org.aavso.tools.vstar.data.InvalidObservation;
import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.exception.AuthenticationError;
import org.aavso.tools.vstar.exception.CancellationException;
import org.aavso.tools.vstar.exception.ConnectionException;
import org.aavso.tools.vstar.exception.ObservationReadError;
import org.aavso.tools.vstar.input.database.Authenticator;
import org.aavso.tools.vstar.input.text.ObservationSourceAnalyser;
import org.aavso.tools.vstar.plugin.CustomFilterPluginBase;
import org.aavso.tools.vstar.plugin.ObservationSourcePluginBase;
import org.aavso.tools.vstar.plugin.ObservationToolPluginBase;
import org.aavso.tools.vstar.plugin.period.PeriodAnalysisPluginBase;
import org.aavso.tools.vstar.ui.MainFrame;
import org.aavso.tools.vstar.ui.MenuBar;
import org.aavso.tools.vstar.ui.NamedComponent;
import org.aavso.tools.vstar.ui.TabbedDataPane;
import org.aavso.tools.vstar.ui.dialog.DelimitedFieldFileSaveAsChooser;
import org.aavso.tools.vstar.ui.dialog.DiscrepantReportDialog;
import org.aavso.tools.vstar.ui.dialog.MessageBox;
import org.aavso.tools.vstar.ui.dialog.ObservationDetailsDialog;
import org.aavso.tools.vstar.ui.dialog.PhaseDialog;
import org.aavso.tools.vstar.ui.dialog.PhaseParameterDialog;
import org.aavso.tools.vstar.ui.dialog.PlotControlDialog;
import org.aavso.tools.vstar.ui.dialog.PolynomialDegreeDialog;
import org.aavso.tools.vstar.ui.dialog.filter.ObservationFilterDialog;
import org.aavso.tools.vstar.ui.dialog.model.ModelDialog;
import org.aavso.tools.vstar.ui.dialog.series.SingleSeriesSelectionDialog;
import org.aavso.tools.vstar.ui.mediator.message.AnalysisTypeChangeMessage;
import org.aavso.tools.vstar.ui.mediator.message.DiscrepantObservationMessage;
import org.aavso.tools.vstar.ui.mediator.message.ExcludedObservationMessage;
import org.aavso.tools.vstar.ui.mediator.message.FilteredObservationMessage;
import org.aavso.tools.vstar.ui.mediator.message.HarmonicSearchResultMessage;
import org.aavso.tools.vstar.ui.mediator.message.MeanSourceSeriesChangeMessage;
import org.aavso.tools.vstar.ui.mediator.message.ModelCreationMessage;
import org.aavso.tools.vstar.ui.mediator.message.ModelSelectionMessage;
import org.aavso.tools.vstar.ui.mediator.message.MultipleObservationSelectionMessage;
import org.aavso.tools.vstar.ui.mediator.message.NewStarMessage;
import org.aavso.tools.vstar.ui.mediator.message.ObservationSelectionMessage;
import org.aavso.tools.vstar.ui.mediator.message.PanRequestMessage;
import org.aavso.tools.vstar.ui.mediator.message.PeriodAnalysisRefinementMessage;
import org.aavso.tools.vstar.ui.mediator.message.PeriodAnalysisSelectionMessage;
import org.aavso.tools.vstar.ui.mediator.message.PeriodChangeMessage;
import org.aavso.tools.vstar.ui.mediator.message.PhaseChangeMessage;
import org.aavso.tools.vstar.ui.mediator.message.PhaseSelectionMessage;
import org.aavso.tools.vstar.ui.mediator.message.ProgressInfo;
import org.aavso.tools.vstar.ui.mediator.message.ProgressType;
import org.aavso.tools.vstar.ui.mediator.message.SeriesVisibilityChangeMessage;
import org.aavso.tools.vstar.ui.mediator.message.StopRequestMessage;
import org.aavso.tools.vstar.ui.mediator.message.UndoActionMessage;
import org.aavso.tools.vstar.ui.mediator.message.ZoomRequestMessage;
import org.aavso.tools.vstar.ui.model.list.AbstractMeanObservationTableModel;
import org.aavso.tools.vstar.ui.model.list.AbstractModelObservationTableModel;
import org.aavso.tools.vstar.ui.model.list.InvalidObservationTableModel;
import org.aavso.tools.vstar.ui.model.list.PhasePlotMeanObservationTableModel;
import org.aavso.tools.vstar.ui.model.list.RawDataMeanObservationTableModel;
import org.aavso.tools.vstar.ui.model.list.ValidObservationTableModel;
import org.aavso.tools.vstar.ui.model.plot.JDCoordSource;
import org.aavso.tools.vstar.ui.model.plot.JDTimeElementEntity;
import org.aavso.tools.vstar.ui.model.plot.ObservationAndMeanPlotModel;
import org.aavso.tools.vstar.ui.model.plot.PhaseTimeElementEntity;
import org.aavso.tools.vstar.ui.model.plot.PreviousCyclePhaseCoordSource;
import org.aavso.tools.vstar.ui.model.plot.StandardPhaseCoordSource;
import org.aavso.tools.vstar.ui.pane.list.ObservationListPane;
import org.aavso.tools.vstar.ui.pane.list.SyntheticObservationListPane;
import org.aavso.tools.vstar.ui.pane.plot.ObservationAndMeanPlotPane;
import org.aavso.tools.vstar.ui.pane.plot.PhaseAndMeanPlotPane;
import org.aavso.tools.vstar.ui.pane.plot.TimeElementsInBinSettingPane;
import org.aavso.tools.vstar.ui.resources.ResourceAccessor;
import org.aavso.tools.vstar.ui.task.ModellingTask;
import org.aavso.tools.vstar.ui.task.NewStarFromDatabaseTask;
import org.aavso.tools.vstar.ui.task.NewStarFromFileTask;
import org.aavso.tools.vstar.ui.task.NewStarFromObSourcePluginTask;
import org.aavso.tools.vstar.ui.task.ObsListFileSaveTask;
import org.aavso.tools.vstar.ui.task.PeriodAnalysisTask;
import org.aavso.tools.vstar.ui.task.PhasePlotTask;
import org.aavso.tools.vstar.ui.undo.UndoableActionManager;
import org.aavso.tools.vstar.util.comparator.JDComparator;
import org.aavso.tools.vstar.util.comparator.PreviousCyclePhaseComparator;
import org.aavso.tools.vstar.util.comparator.StandardPhaseComparator;
import org.aavso.tools.vstar.util.discrepant.DiscrepantReport;
import org.aavso.tools.vstar.util.discrepant.IDiscrepantReporter;
import org.aavso.tools.vstar.util.discrepant.ZapperLogger;
import org.aavso.tools.vstar.util.locale.LocaleProps;
import org.aavso.tools.vstar.util.model.IModel;
import org.aavso.tools.vstar.util.model.IPolynomialFitter;
import org.aavso.tools.vstar.util.model.TSPolynomialFitter;
import org.aavso.tools.vstar.util.notification.Listener;
import org.aavso.tools.vstar.util.notification.Notifier;
import org.aavso.tools.vstar.util.prefs.NumericPrecisionPrefs;
import org.aavso.tools.vstar.util.stats.BinningResult;
import org.aavso.tools.vstar.util.stats.PhaseCalcs;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;

/**
 * This class manages the creation of models and views and sends notifications
 * for changes to mode and analysis types.
 * 
 * This is a Singleton since only one mediator per application instance should
 * exist.
 * 
 * TODO: This is really 2 classes: a task manager and a message broker...
 */
public class Mediator {

	public static final String NOT_IMPLEMENTED_YET = "This feature is not implemented yet.";

	// Valid and invalid observation lists and series category map.
	private List<ValidObservation> validObsList;
	private List<InvalidObservation> invalidObsList;
	private Map<SeriesType, List<ValidObservation>> validObservationCategoryMap;
	private Map<SeriesType, List<ValidObservation>> phasedValidObservationCategoryMap;

	// Current observation and mean plot model.
	// Period search needs access to this to determine
	// the current mean source band.
	private ObservationAndMeanPlotModel obsAndMeanPlotModel;

	// Current view viewMode.
	private ViewModeType viewMode;

	// Current analysis type.
	private AnalysisType analysisType;

	// The latest new star message created and sent to listeners.
	private NewStarMessage newStarMessage;

	// The latest model selection message created and sent to listeners.
	private ModelSelectionMessage modelSelectionMessage;

	// Mapping from analysis type to the latest analysis change
	// messages created and sent to listeners.
	private Map<AnalysisType, AnalysisTypeChangeMessage> analysisTypeMap;

	// A file dialog for saving any kind of observation list.
	private DelimitedFieldFileSaveAsChooser obsListFileSaveDialog;

	// Persistent phase parameter dialog.
	private PhaseParameterDialog phaseParameterDialog;

	// Persistent observation filter dialog.
	private ObservationFilterDialog obsFilterDialog;

	// Model dialog.
	private ModelDialog modelDialog;

	// A dialog to manage phase plots.
	private PhaseDialog phaseDialog;

	// Notifiers.
	private Notifier<AnalysisTypeChangeMessage> analysisTypeChangeNotifier;
	private Notifier<NewStarMessage> newStarNotifier;
	private Notifier<ProgressInfo> progressNotifier;
	// TODO: This next notifier could be used to mark the "document"
	// (the current star's dataset) associated with the valid obs
	// as being in need of saving (optional for now). See DocumentManager
	private Notifier<DiscrepantObservationMessage> discrepantObservationNotifier;
	private Notifier<ExcludedObservationMessage> excludedObservationNotifier;
	private Notifier<ObservationSelectionMessage> observationSelectionNotifier;
	private Notifier<MultipleObservationSelectionMessage> multipleObservationSelectionNotifier;
	private Notifier<PeriodAnalysisSelectionMessage> periodAnalysisSelectionNotifier;
	private Notifier<PeriodChangeMessage> periodChangeNotifier;
	private Notifier<PhaseChangeMessage> phaseChangeNotifier;
	private Notifier<PhaseSelectionMessage> phaseSelectionNotifier;
	private Notifier<PeriodAnalysisRefinementMessage> periodAnalysisRefinementNotifier;
	private Notifier<MeanSourceSeriesChangeMessage> meanSourceSeriesChangeNotifier;
	private Notifier<ZoomRequestMessage> zoomRequestNotifier;
	private Notifier<FilteredObservationMessage> filteredObservationNotifier;
	private Notifier<ModelSelectionMessage> modelSelectionNofitier;
	private Notifier<ModelCreationMessage> modelCreationNotifier;
	private Notifier<PanRequestMessage> panRequestNotifier;
	private Notifier<UndoActionMessage> undoActionNotifier;
	private Notifier<StopRequestMessage> stopRequestNotifier;
	private Notifier<SeriesVisibilityChangeMessage> seriesVisibilityChangeNotifier;
	private Notifier<HarmonicSearchResultMessage> harmonicSearchNotifier;

	private DocumentManager documentManager;

	private UndoableActionManager undoableActionManager;

	// Currently active task.
	private SwingWorker currTask;

	// Singleton fields, constructor, getter.

	private static Mediator mediator = new Mediator();

	/**
	 * Private constructor.
	 */
	private Mediator() {
		this.analysisTypeChangeNotifier = new Notifier<AnalysisTypeChangeMessage>();
		this.newStarNotifier = new Notifier<NewStarMessage>();
		this.progressNotifier = new Notifier<ProgressInfo>();
		this.discrepantObservationNotifier = new Notifier<DiscrepantObservationMessage>();
		this.excludedObservationNotifier = new Notifier<ExcludedObservationMessage>();
		this.observationSelectionNotifier = new Notifier<ObservationSelectionMessage>();
		this.multipleObservationSelectionNotifier = new Notifier<MultipleObservationSelectionMessage>();
		this.periodAnalysisSelectionNotifier = new Notifier<PeriodAnalysisSelectionMessage>();
		this.periodChangeNotifier = new Notifier<PeriodChangeMessage>();
		this.phaseChangeNotifier = new Notifier<PhaseChangeMessage>();
		this.phaseSelectionNotifier = new Notifier<PhaseSelectionMessage>();
		this.periodAnalysisRefinementNotifier = new Notifier<PeriodAnalysisRefinementMessage>();
		this.meanSourceSeriesChangeNotifier = new Notifier<MeanSourceSeriesChangeMessage>();
		this.zoomRequestNotifier = new Notifier<ZoomRequestMessage>();
		this.filteredObservationNotifier = new Notifier<FilteredObservationMessage>();
		this.modelSelectionNofitier = new Notifier<ModelSelectionMessage>();
		this.modelCreationNotifier = new Notifier<ModelCreationMessage>();
		this.panRequestNotifier = new Notifier<PanRequestMessage>();
		this.undoActionNotifier = new Notifier<UndoActionMessage>();
		this.stopRequestNotifier = new Notifier<StopRequestMessage>();
		this.seriesVisibilityChangeNotifier = new Notifier<SeriesVisibilityChangeMessage>();
		this.harmonicSearchNotifier = new Notifier<HarmonicSearchResultMessage>();

		this.obsListFileSaveDialog = new DelimitedFieldFileSaveAsChooser();

		// These (among other things) are created for each new star.
		this.validObsList = null;
		this.invalidObsList = null;
		this.validObservationCategoryMap = null;
		this.phasedValidObservationCategoryMap = null;
		this.obsAndMeanPlotModel = null;

		this.analysisTypeMap = new HashMap<AnalysisType, AnalysisTypeChangeMessage>();

		this.viewMode = ViewModeType.PLOT_OBS_MODE;
		this.analysisType = AnalysisType.RAW_DATA;
		this.newStarMessage = null;
		this.modelSelectionMessage = null;

		this.periodChangeNotifier.addListener(createPeriodChangeListener());

		this.phaseSelectionNotifier.addListener(createPhaseSelectionListener());

		this.modelSelectionNofitier.addListener(createModelSelectionListener());
		this.filteredObservationNotifier
				.addListener(createFilteredObservationListener());

		this.phaseParameterDialog = new PhaseParameterDialog();
		this.newStarNotifier.addListener(this.phaseParameterDialog);

		this.obsFilterDialog = new ObservationFilterDialog();
		this.newStarNotifier.addListener(this.obsFilterDialog
				.createNewStarListener());
		this.observationSelectionNotifier.addListener(this.obsFilterDialog
				.createObservationSelectionListener());

		this.modelDialog = new ModelDialog();
		this.newStarNotifier.addListener(this.modelDialog
				.createNewStarListener());
		this.modelCreationNotifier.addListener(this.modelDialog
				.createModelCreationListener());

		this.phaseDialog = new PhaseDialog();
		this.newStarNotifier.addListener(this.phaseDialog
				.createNewStarListener());
		this.phaseChangeNotifier.addListener(this.phaseDialog
				.createPhaseChangeListener());

		// Document manager creation and listener setup.
		this.documentManager = new DocumentManager();
		this.phaseChangeNotifier.addListener(this.documentManager
				.createPhaseChangeListener());
		this.newStarNotifier.addListener(this.documentManager
				.createNewStarListener());

		// Undoable action manager creation and listener setup.
		this.undoableActionManager = new UndoableActionManager();
		this.newStarNotifier.addListener(this.undoableActionManager
				.createNewStarListener());
		this.observationSelectionNotifier
				.addListener(this.undoableActionManager
						.createObservationSelectionListener());
		this.multipleObservationSelectionNotifier
				.addListener(this.undoableActionManager
						.createMultipleObservationSelectionListener());
	}

	/**
	 * Return the Singleton instance.
	 */
	public static Mediator getInstance() {
		return mediator;
	}

	/**
	 * @return the newStarMessage
	 */
	public NewStarMessage getNewStarMessage() {
		return newStarMessage;
	}

	/**
	 * @return the analysisTypeChangeNotifier
	 */
	public Notifier<AnalysisTypeChangeMessage> getAnalysisTypeChangeNotifier() {
		return analysisTypeChangeNotifier;
	}

	/**
	 * @param analysisType
	 *            the analysisType to set
	 */
	public void setAnalysisType(AnalysisType analysisType) {
		this.analysisType = analysisType;
	}

	/**
	 * @return the newStarNotifier
	 */
	public Notifier<NewStarMessage> getNewStarNotifier() {
		return newStarNotifier;
	}

	/**
	 * @return the progressNotifier
	 */
	public Notifier<ProgressInfo> getProgressNotifier() {
		return progressNotifier;
	}

	/**
	 * @return the discrepantObservationNotifier
	 */
	public Notifier<DiscrepantObservationMessage> getDiscrepantObservationNotifier() {
		return discrepantObservationNotifier;
	}

	/**
	 * @return the excludedObservationNotifier
	 */
	public Notifier<ExcludedObservationMessage> getExcludedObservationNotifier() {
		return excludedObservationNotifier;
	}

	/**
	 * @return the observationSelectionNotifier
	 */
	public Notifier<ObservationSelectionMessage> getObservationSelectionNotifier() {
		return observationSelectionNotifier;
	}

	/**
	 * @return the multipleObservationSelectionNotifier
	 */
	public Notifier<MultipleObservationSelectionMessage> getMultipleObservationSelectionNotifier() {
		return multipleObservationSelectionNotifier;
	}

	/**
	 * @return the periodAnalysisSelectionNotifier
	 */
	public Notifier<PeriodAnalysisSelectionMessage> getPeriodAnalysisSelectionNotifier() {
		return periodAnalysisSelectionNotifier;
	}

	/**
	 * @return the periodChangeNotifier
	 */
	public Notifier<PeriodChangeMessage> getPeriodChangeNotifier() {
		return periodChangeNotifier;
	}

	/**
	 * @return the phaseChangeNotifier
	 */
	public Notifier<PhaseChangeMessage> getPhaseChangeNotifier() {
		return phaseChangeNotifier;
	}

	/**
	 * @return the phaseSelectionNotifier
	 */
	public Notifier<PhaseSelectionMessage> getPhaseSelectionNotifier() {
		return phaseSelectionNotifier;
	}

	/**
	 * @return the periodAnalysisRefinementNotifier
	 */
	public Notifier<PeriodAnalysisRefinementMessage> getPeriodAnalysisRefinementNotifier() {
		return periodAnalysisRefinementNotifier;
	}

	/**
	 * @return the meanSourceSeriesChangeNotifier
	 */
	public Notifier<MeanSourceSeriesChangeMessage> getMeanSourceSeriesChangeNotifier() {
		return meanSourceSeriesChangeNotifier;
	}

	/**
	 * @return the zoomRequestNotifier
	 */
	public Notifier<ZoomRequestMessage> getZoomRequestNotifier() {
		return zoomRequestNotifier;
	}

	/**
	 * @return the filteredObservationNotifier
	 */
	public Notifier<FilteredObservationMessage> getFilteredObservationNotifier() {
		return filteredObservationNotifier;
	}

	/**
	 * @return the modelSelectionNofitier
	 */
	public Notifier<ModelSelectionMessage> getModelSelectionNofitier() {
		return modelSelectionNofitier;
	}

	/**
	 * @return the modelCreationNotifier
	 */
	public Notifier<ModelCreationMessage> getModelCreationNotifier() {
		return modelCreationNotifier;
	}

	/**
	 * @return the panRequestNotifier
	 */
	public Notifier<PanRequestMessage> getPanRequestNotifier() {
		return panRequestNotifier;
	}

	/**
	 * @return the undoActionNotifier
	 */
	public Notifier<UndoActionMessage> getUndoActionNotifier() {
		return undoActionNotifier;
	}

	/**
	 * @return the stopRequestNotifier
	 */
	public Notifier<StopRequestMessage> getStopRequestNotifier() {
		return stopRequestNotifier;
	}

	/**
	 * @return the seriesVisibilityChangeNotifier
	 */
	public Notifier<SeriesVisibilityChangeMessage> getSeriesVisibilityChangeNotifier() {
		return seriesVisibilityChangeNotifier;
	}

	/**
	 * @return the harmonicSearchNotifier
	 */
	public Notifier<HarmonicSearchResultMessage> getHarmonicSearchNotifier() {
		return harmonicSearchNotifier;
	}

	/**
	 * @return the undoableActionManager
	 */
	public UndoableActionManager getUndoableActionManager() {
		return undoableActionManager;
	}

	/**
	 * @return the documentManager
	 */
	public DocumentManager getDocumentManager() {
		return documentManager;
	}

	/**
	 * Create a mean observation change listener and return it. Whenever the
	 * mean series source changes, listeners may want to perform a new period
	 * analysis or change the max time increments for means binning.
	 */
	private Listener<BinningResult> createMeanObsChangeListener(
			int initialSeriesNum) {
		final int initialSeriesNumFinal = initialSeriesNum;

		return new Listener<BinningResult>() {
			private int meanSourceSeriesNum = initialSeriesNumFinal;

			public boolean canBeRemoved() {
				return false;
			}

			public void update(BinningResult info) {
				// TODO: would removing this guard permit listeners
				// to do other things, e.g. compare old and new binning results,
				// e.g. for change to days-in-bin?
				if (this.meanSourceSeriesNum != obsAndMeanPlotModel
						.getMeanSourceSeriesNum()) {

					this.meanSourceSeriesNum = obsAndMeanPlotModel
							.getMeanSourceSeriesNum();

					SeriesType meanSourceSeriesType = obsAndMeanPlotModel
							.getSeriesNumToSrcTypeMap().get(
									this.meanSourceSeriesNum);

					meanSourceSeriesChangeNotifier
							.notifyListeners(new MeanSourceSeriesChangeMessage(
									this, meanSourceSeriesType));
				}
			}
		};
	}

	// When the period changes, create a new phase plot passing the pre-existing
	// series visibility map if a previous phase plot was created.
	//
	// TODO: actually, it should only be necessary to a. set the phases with the
	// new period and epoch (need to include the epoch in the message), and b.
	// update the plot and table models.
	private Listener<PeriodChangeMessage> createPeriodChangeListener() {
		return new Listener<PeriodChangeMessage>() {
			public void update(PeriodChangeMessage info) {
				PhaseParameterDialog phaseDialog = getPhaseParameterDialog();
				phaseDialog.setPeriodField(info.getPeriod());
				phaseDialog.showDialog();

				if (!phaseDialog.isCancelled()) {
					double period = phaseDialog.getPeriod();
					double epoch = phaseDialog.getEpoch();

					AnalysisTypeChangeMessage lastPhasePlotMsg = analysisTypeMap
							.get(AnalysisType.PHASE_PLOT);

					Map<SeriesType, Boolean> seriesVisibilityMap = null;

					if (lastPhasePlotMsg != null) {
						// Use the last phase plot's series visibility map.
						seriesVisibilityMap = lastPhasePlotMsg
								.getObsAndMeanChartPane().getObsModel()
								.getSeriesVisibilityMap();
					} else {
						// There has been no phase plot yet, so use the
						// light curve's series visibility map.
						AnalysisTypeChangeMessage lightCurveMsg = analysisTypeMap
								.get(AnalysisType.RAW_DATA);
						seriesVisibilityMap = lightCurveMsg
								.getObsAndMeanChartPane().getObsModel()
								.getSeriesVisibilityMap();
					}

					performPhasePlot(period, epoch, seriesVisibilityMap);
				}
			}

			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	// Returns a phase selection message listener, the purpose of which is to
	// recreate a previous phase plot.
	protected Listener<PhaseSelectionMessage> createPhaseSelectionListener() {
		final Mediator me = this;
		return new Listener<PhaseSelectionMessage>() {
			@Override
			public void update(PhaseSelectionMessage info) {
				if (info.getSource() != me) {
					performPhasePlot(info.getPeriod(), info.getEpoch(), info
							.getSeriesVisibilityMap());
				}
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	/**
	 * Create a phase plot, first asking for period and epoch.
	 * 
	 * The series visibility map for the phase plot is taken from the currently
	 * visible plot (raw data or phase plot).
	 */
	public void createPhasePlot() {
		PhaseParameterDialog phaseDialog = getPhaseParameterDialog();
		phaseDialog.showDialog();
		if (!phaseDialog.isCancelled()) {
			double period = phaseDialog.getPeriod();
			double epoch = phaseDialog.getEpoch();

			Map<SeriesType, Boolean> seriesVisibilityMap = analysisTypeMap.get(
					analysisType).getObsAndMeanChartPane().getObsModel()
					.getSeriesVisibilityMap();

			performPhasePlot(period, epoch, seriesVisibilityMap);
		}
	}

	/**
	 * Create a phase plot, given the period and epoch.
	 * 
	 * The series visibility map for the phase plot is taken from the currently
	 * visible plot (raw data or phase plot).
	 * 
	 * @param period
	 *            The requested period of the phase plot.
	 * @param epoch
	 *            The epoch (first Julian Date) for the phase plot.
	 */
	public void createPhasePlot(double period, double epoch) {
		Map<SeriesType, Boolean> seriesVisibilityMap = analysisTypeMap.get(
				analysisType).getObsAndMeanChartPane().getObsModel()
				.getSeriesVisibilityMap();

		performPhasePlot(period, epoch, seriesVisibilityMap);
	}

	/**
	 * Common phase plot handler.
	 * 
	 * @param period
	 *            The requested period of the phase plot.
	 * @param epoch
	 *            The epoch (first Julian Date) for the phase plot.
	 * @param seriesVisibilityMap
	 *            A mapping from series number to visibility status.
	 */
	public void performPhasePlot(double period, double epoch,
			Map<SeriesType, Boolean> seriesVisibilityMap) {

		PhasePlotTask task = new PhasePlotTask(period, epoch,
				seriesVisibilityMap);

		try {
			currTask = task;
			task.execute();
		} catch (Exception e) {
			MainFrame.getInstance().setCursor(null);
			MessageBox.showErrorDialog(MainFrame.getInstance(),
					"New Phase Plot", e);
		}
	}

	// Returns a model selection listener that updates the observation
	// category map with model and residuals series.
	private Listener<ModelSelectionMessage> createModelSelectionListener() {
		return new Listener<ModelSelectionMessage>() {
			@Override
			public void update(ModelSelectionMessage info) {
				validObservationCategoryMap.put(SeriesType.Model, info
						.getModel().getFit());

				validObservationCategoryMap.put(SeriesType.Residuals, info
						.getModel().getResiduals());

				modelSelectionMessage = info;
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	// Returns a filtered observation listener that updates the observation
	// category map with the filtered series.
	protected Listener<FilteredObservationMessage> createFilteredObservationListener() {
		return new Listener<FilteredObservationMessage>() {
			@Override
			public void update(FilteredObservationMessage info) {
				if (info == FilteredObservationMessage.NO_FILTER) {
					validObservationCategoryMap.remove(SeriesType.Filtered);
				} else {
					// First, copy the set of filtered observations to a list.
					List<ValidObservation> obs = new ArrayList<ValidObservation>();
					for (ValidObservation ob : info.getFilteredObs()) {
						obs.add(ob);
					}
					validObservationCategoryMap.put(SeriesType.Filtered, obs);
				}
			}

			@Override
			public boolean canBeRemoved() {
				return false;
			}
		};
	}

	/**
	 * Change the mode of VStar's focus (i.e what is to be presented to the
	 * user).
	 * 
	 * @param viewMode
	 *            The mode to change to.
	 */
	public void changeViewMode(ViewModeType viewMode) {
		if (viewMode != this.viewMode) {
			this.viewMode = viewMode;
		}
	}

	/**
	 * @return the viewMode
	 */
	public ViewModeType getViewMode() {
		return viewMode;
	}

	/**
	 * @return the phaseParameterDialog
	 */
	public PhaseParameterDialog getPhaseParameterDialog() {
		return phaseParameterDialog;
	}

	/**
	 * @return the obsFilterDialog
	 */
	public ObservationFilterDialog getObsFilterDialog() {
		return obsFilterDialog;
	}

	/**
	 * Change the analysis type. If the old and new types are the same, there
	 * will be no effect.
	 * 
	 * @param analysisType
	 *            The analysis type to change to.
	 */
	public AnalysisType changeAnalysisType(AnalysisType analysisType) {
		if (this.analysisType != analysisType) {
			try {
				AnalysisTypeChangeMessage msg;

				switch (analysisType) {
				case RAW_DATA:
					// Create or retrieve raw plots and data tables.
					// There has to be observations loaded already in order
					// to be able to switch to raw data analysis mode.
					msg = this.analysisTypeMap.get(AnalysisType.RAW_DATA);

					if (msg != null) {
						this.analysisType = analysisType;
						this.analysisTypeChangeNotifier.notifyListeners(msg);
						String statusMsg = "Raw data mode ("
								+ this.newStarMessage.getStarInfo()
										.getDesignation() + ")";
						MainFrame.getInstance().getStatusPane().setMessage(
								statusMsg);
					}
					break;

				case PHASE_PLOT:
					// Create or retrieve phase plots and data tables passing
					// the light curve's series visibility map for the first
					// phase plot.
					msg = this.analysisTypeMap.get(AnalysisType.PHASE_PLOT);

					if (msg == null) {
						createPhasePlot();
					} else {
						// Change to the existing phase plot.
						this.analysisType = analysisType;
						this.analysisTypeChangeNotifier.notifyListeners(msg);
						setPhasePlotStatusMessage();
					}
					break;
				}
			} catch (Exception e) {
				MessageBox.showErrorDialog(MainFrame.getInstance(),
						"Analysis Type Change", e);
			}
		}

		return this.analysisType;
	}

	/**
	 * Set the status bar to display phase plot information.
	 */
	public void setPhasePlotStatusMessage() {
		String statusMsg = "Phase plot mode ("
				+ this.newStarMessage.getStarInfo().getDesignation() + ")";
		MainFrame.getInstance().getStatusPane().setMessage(statusMsg);
	}

	/**
	 * @return the analysisType
	 */
	public AnalysisType getAnalysisType() {
		return analysisType;
	}

	/**
	 * Remove all listeners that are willing, from all notifiers, to ensure that
	 * no old, unnecessary listeners remain from one new-star load to another.
	 * Such listeners could receive notifications that make no sense (e.g.
	 * location of an observation within a dataset) and guard against memory
	 * leaks.
	 */
	private void freeListeners() {
		analysisTypeChangeNotifier.removeAllWillingListeners();
		newStarNotifier.removeAllWillingListeners();
		progressNotifier.removeAllWillingListeners();
		discrepantObservationNotifier.removeAllWillingListeners();
		excludedObservationNotifier.removeAllWillingListeners();
		observationSelectionNotifier.removeAllWillingListeners();
		multipleObservationSelectionNotifier.removeAllWillingListeners();
		periodAnalysisSelectionNotifier.removeAllWillingListeners();
		periodChangeNotifier.removeAllWillingListeners();
		phaseChangeNotifier.removeAllWillingListeners();
		phaseSelectionNotifier.removeAllWillingListeners();
		periodAnalysisRefinementNotifier.removeAllWillingListeners();
		meanSourceSeriesChangeNotifier.removeAllWillingListeners();
		zoomRequestNotifier.removeAllWillingListeners();
		filteredObservationNotifier.removeAllWillingListeners();
		modelSelectionNofitier.removeAllWillingListeners();
		modelCreationNotifier.removeAllWillingListeners();
		panRequestNotifier.removeAllWillingListeners();
		undoActionNotifier.removeAllWillingListeners();
		stopRequestNotifier.removeAllWillingListeners();
		seriesVisibilityChangeNotifier.removeAllWillingListeners();
		harmonicSearchNotifier.removeAllWillingListeners();
		observationSelectionNotifier.removeAllWillingListeners();
	}

	/**
	 * Creates and executes a background task to handle new-star-from-file.
	 * 
	 * @param obsFile
	 *            The file from which to load the star observations.
	 * @param parent
	 *            The GUI component that can be used to display.
	 */
	public void createObservationArtefactsFromFile(File obsFile)
			throws IOException, ObservationReadError {

		this.getProgressNotifier().notifyListeners(ProgressInfo.START_PROGRESS);

		// Analyse the observation file.
		ObservationSourceAnalyser analyser = new ObservationSourceAnalyser(
				new LineNumberReader(new FileReader(obsFile)), obsFile
						.getName());
		analyser.analyse();

		// Task begins: Number of lines in file and a portion for the light
		// curve plot.
		int plotPortion = (int) (analyser.getLineCount() * 0.2);

		this.getProgressNotifier().notifyListeners(
				new ProgressInfo(ProgressType.MAX_PROGRESS, analyser
						.getLineCount()
						+ plotPortion));

		NewStarFromFileTask task = new NewStarFromFileTask(obsFile, analyser,
				plotPortion);
		this.currTask = task;
		task.execute();
	}

	/**
	 * Creates and executes a background task to handle new-star-from-database.
	 * 
	 * @param starName
	 *            The name of the star.
	 * @param auid
	 *            AAVSO unique ID for the star.
	 * @param minJD
	 *            The minimum Julian Day of the requested range.
	 * @param maxJD
	 *            The maximum Julian Day of the requested range.
	 */
	public void createObservationArtefactsFromDatabase(String starName,
			String auid, double minJD, double maxJD) {

		try {
			this.getProgressNotifier().notifyListeners(
					ProgressInfo.START_PROGRESS);

			this.getProgressNotifier().notifyListeners(
					new ProgressInfo(ProgressType.MAX_PROGRESS, 10));

			NewStarFromDatabaseTask task = new NewStarFromDatabaseTask(
					starName, auid, minJD, maxJD);
			this.currTask = task;
			task.execute();
		} catch (Exception ex) {
			MessageBox.showErrorDialog(MainFrame.getInstance(),
					MenuBar.NEW_STAR_FROM_DATABASE, ex);
			MainFrame.getInstance().getStatusPane().setMessage("");
		}
	}

	/**
	 * Creates and executes a background task to handle
	 * new-star-from-external-source-plugin.
	 * 
	 * @param obSourcePlugin
	 *            The plugin that will be used to obtain observations.
	 */
	public void createObservationArtefactsFromObSourcePlugin(
			ObservationSourcePluginBase obSourcePlugin) {

		this.getProgressNotifier().notifyListeners(ProgressInfo.START_PROGRESS);
		this.getProgressNotifier().notifyListeners(ProgressInfo.BUSY_PROGRESS);

		NewStarFromObSourcePluginTask task = new NewStarFromObSourcePluginTask(
				obSourcePlugin);
		this.currTask = task;
		task.execute();
	}

	/**
	 * Create observation artefacts (models, GUI elements) on the assumption
	 * that a valid observation list and category map have already been created.
	 * 
	 * @param newStarType
	 *            The new star enum type.
	 * @param starInfo
	 *            Information about the star, e.g. name (designation), AUID (for
	 *            AID), period, epoch, including observation retriever.
	 * @param obsArtefactProgressAmount
	 *            The amount the progress bar should be incremented by, a value
	 *            corresponding to a portion of the overall task of which this
	 *            is just a part.
	 * @param addObs
	 *            Should the observations be added to the existing loaded
	 *            dataset?
	 */
	public void createNewStarObservationArtefacts(NewStarType newStarType,
			StarInfo starInfo, int obsArtefactProgressAmount, boolean addObs) {

		// Given raw valid and invalid observation data, create observation
		// table and plot models, along with corresponding GUI components.

		List<ValidObservation> validObsList = starInfo.getRetriever()
				.getValidObservations();

		List<InvalidObservation> invalidObsList = starInfo.getRetriever()
				.getInvalidObservations();

		Map<SeriesType, List<ValidObservation>> validObservationCategoryMap = starInfo
				.getRetriever().getValidObservationCategoryMap();

		// Table models.
		ValidObservationTableModel validObsTableModel = null;
		InvalidObservationTableModel invalidObsTableModel = null;
		RawDataMeanObservationTableModel meanObsTableModel = null;

		// Plot models.
		obsAndMeanPlotModel = null;

		// GUI table and chart components.
		ObservationListPane obsListPane = null;
		SyntheticObservationListPane<AbstractMeanObservationTableModel> meansListPane = null;
		ObservationAndMeanPlotPane obsAndMeanChartPane = null;

		if (!validObsList.isEmpty()) {

			freeListeners();

			// This is a specific fix for tracker 3007948.
			this.discrepantObservationNotifier = new Notifier<DiscrepantObservationMessage>();

			// Observation table and plot.
			validObsTableModel = new ValidObservationTableModel(validObsList,
					newStarType.getRawDataTableColumnInfoSource());

			// Observation-and-mean table and plot.
			obsAndMeanPlotModel = new ObservationAndMeanPlotModel(
					validObservationCategoryMap, JDCoordSource.instance,
					JDComparator.instance, JDTimeElementEntity.instance, null);

			// Record initial ANOVA information and make the document manager
			// listen to changes to ANOVA via new binning results.
			documentManager.updateAnovaInfo(obsAndMeanPlotModel
					.getBinningResult());

			obsAndMeanPlotModel.getMeansChangeNotifier().addListener(
					documentManager.createBinChangeListener());

			documentManager
					.addStatsInfo("Confidence Interval",
							"Mean error bars denote 95% Confidence Interval (twice Standard Error)");

			obsAndMeanChartPane = createObservationAndMeanPlotPane(LocaleProps
					.get("LIGHT_CURVE")
					+ " "
					+ LocaleProps.get("FOR")
					+ " "
					+ starInfo.getDesignation(), null, obsAndMeanPlotModel);

			obsAndMeanPlotModel.getMeansChangeNotifier().addListener(
					createMeanObsChangeListener(obsAndMeanPlotModel
							.getMeanSourceSeriesNum()));

			// The mean observation table model must listen to the plot
			// model to know when the means data has changed. We also pass
			// the initial means data obtained from the plot model to
			// the mean observation table model.
			meanObsTableModel = new RawDataMeanObservationTableModel(
					obsAndMeanPlotModel.getMeanObsList());

			obsAndMeanPlotModel.getMeansChangeNotifier().addListener(
					meanObsTableModel);

			if (obsArtefactProgressAmount > 0) {
				// Update progress.
				getProgressNotifier().notifyListeners(
						new ProgressInfo(ProgressType.INCREMENT_PROGRESS,
								obsArtefactProgressAmount));
			}
		}

		if (!invalidObsList.isEmpty()) {
			invalidObsTableModel = new InvalidObservationTableModel(
					invalidObsList);
		}

		// The observation table pane contains valid and potentially
		// invalid data components. Tell the valid data table to have
		// a horizontal scrollbar if there will be too many columns.

		boolean enableColumnAutoResize = newStarType == NewStarType.NEW_STAR_FROM_SIMPLE_FILE
				|| newStarType == NewStarType.NEW_STAR_FROM_EXTERNAL_SOURCE;

		obsListPane = new ObservationListPane(starInfo.getDesignation(),
				validObsTableModel, invalidObsTableModel,
				enableColumnAutoResize, obsAndMeanPlotModel.getVisibleSeries(),
				AnalysisType.RAW_DATA);

		// We also create the means list pane.
		meansListPane = new SyntheticObservationListPane<AbstractMeanObservationTableModel>(
				meanObsTableModel, null);

		// Create a message to notify whoever is listening that a new star
		// has been loaded.
		newStarMessage = new NewStarMessage(newStarType, starInfo,
				validObsList, validObservationCategoryMap, starInfo
						.getRetriever().getMinMag(), starInfo.getRetriever()
						.getMaxMag(), starInfo.getRetriever().getSourceName());

		// Create a message to notify whoever is listening that the analysis
		// type has changed (we could have been viewing a phase plot for a
		// different star before now) passing GUI components in the message.
		analysisType = AnalysisType.RAW_DATA;

		AnalysisTypeChangeMessage analysisTypeMsg = new AnalysisTypeChangeMessage(
				analysisType, obsAndMeanChartPane, obsListPane, meansListPane,
				ViewModeType.PLOT_OBS_MODE);

		// Commit to using the new observation lists and category map,
		// first making old values available for garbage collection.
		// TODO: It would be worth considering doing this at the start
		// of this method, not at the end, so more memory is free.

		if (this.validObsList != null) {
			this.validObsList.clear();
		}

		if (this.invalidObsList != null) {
			this.invalidObsList.clear();
		}

		if (this.validObservationCategoryMap != null) {
			this.validObservationCategoryMap.clear();
		}

		if (this.phasedValidObservationCategoryMap != null) {
			// In case we did a phase plot, free this up.
			this.phasedValidObservationCategoryMap.clear();
			this.phasedValidObservationCategoryMap = null;
		}

		// Throw away old artefacts from raw and phase plot,
		// if there was (at least) one.
		analysisTypeMap.clear();
		analysisTypeMap.put(analysisType, analysisTypeMsg);

		// Suggest garbage collection.
		System.gc();

		// Store new data.
		this.validObsList = validObsList;
		this.invalidObsList = invalidObsList;
		this.validObservationCategoryMap = validObservationCategoryMap;

		// Notify listeners of new star and analysis type.
		newStarNotifier.notifyListeners(newStarMessage);
		analysisTypeChangeNotifier.notifyListeners(analysisTypeMsg);
	}

	/**
	 * Create phase plot artefacts, adding them to the analysis type map and
	 * returning this message.
	 * 
	 * @param period
	 *            The requested period of the phase plot.
	 * @param epoch
	 *            The epoch (first Julian Date) for the phase plot.
	 * @param seriesVisibilityMap
	 *            A mapping from series number to visibility status.
	 * @return An analysis type message consisting of phase plot artefacts.
	 */
	public AnalysisTypeChangeMessage createPhasePlotArtefacts(double period,
			double epoch, Map<SeriesType, Boolean> seriesVisibilityMap)
			throws Exception {
		String objName = newStarMessage.getStarInfo().getDesignation();

		String subTitle = "";
		String periodAndEpochStr = String.format(LocaleProps.get("PERIOD")
				+ ": " + NumericPrecisionPrefs.getOtherOutputFormat() + ", "
				+ LocaleProps.get("EPOCH") + ": "
				+ NumericPrecisionPrefs.getTimeOutputFormat(), period, epoch);

		if (this.newStarMessage.getNewStarType() == NewStarType.NEW_STAR_FROM_DATABASE) {
			Date now = Calendar.getInstance().getTime();
			String formattedDate = DateFormat.getDateInstance().format(now);
			subTitle = formattedDate + " (" + LocaleProps.get("DATABASE")
					+ "), " + periodAndEpochStr;
		} else {
			subTitle = periodAndEpochStr;
		}

		// Here we modify the underlying ValidObservation objects which will
		// affect both validObsList and validObservationCategoryMap. Some
		// series are not in the main observation list, only in the map
		// (e.g. model, residuals, filtered obs), so we handle those separately.
		PhaseCalcs.setPhases(validObsList, epoch, period);
		setPhasesForSeries(SeriesType.Model, epoch, period);
		setPhasesForSeries(SeriesType.Residuals, epoch, period);
		setPhasesForSeries(SeriesType.Filtered, epoch, period);

		// We duplicate the valid observation category map
		// so that it can vary from the main plot's over time.
		Map<SeriesType, List<ValidObservation>> phasedValidObservationCategoryMap = new TreeMap<SeriesType, List<ValidObservation>>();

		for (SeriesType series : validObservationCategoryMap.keySet()) {
			List<ValidObservation> obs = validObservationCategoryMap
					.get(series);

			// Note: only duplicate and sort list if mean series
			// since that is the only one that can be joined!
			// this will reduce the memory footprint of a phase plot!
			List<ValidObservation> phasedObs;

			if (series == SeriesType.MEANS) {
				phasedObs = new ArrayList<ValidObservation>(obs);
				Collections.sort(phasedObs, StandardPhaseComparator.instance);
			} else {
				phasedObs = obs;
			}

			phasedValidObservationCategoryMap.put(series, phasedObs);
		}

		// TODO:
		// o fix occurrences of obs doubling and just copy and sort
		// o indeed: is this needed now anyway? see plot model/pane code

		// Table and plot models.
		ValidObservationTableModel validObsTableModel = new ValidObservationTableModel(
				validObsList, newStarMessage.getNewStarType()
						.getPhasePlotTableColumnInfoSource());

		// Observation-and-mean table and plot.
		ObservationAndMeanPlotModel obsAndMeanPlotModel1 = new ObservationAndMeanPlotModel(
				phasedValidObservationCategoryMap,
				PreviousCyclePhaseCoordSource.instance,
				PreviousCyclePhaseComparator.instance,
				PhaseTimeElementEntity.instance, seriesVisibilityMap);

		ObservationAndMeanPlotModel obsAndMeanPlotModel2 = new ObservationAndMeanPlotModel(
				phasedValidObservationCategoryMap,
				StandardPhaseCoordSource.instance,
				StandardPhaseComparator.instance,
				PhaseTimeElementEntity.instance, seriesVisibilityMap);

		// Select an arbitrary model for mean
		obsAndMeanPlotModel = obsAndMeanPlotModel1;

		// The mean observation table model must listen to the plot
		// model to know when the means data has changed. We also pass
		// the initial means data obtained from the plot model to
		// the mean observation table model.
		PhasePlotMeanObservationTableModel meanObsTableModel = new PhasePlotMeanObservationTableModel(
				obsAndMeanPlotModel1.getMeanObsList());

		obsAndMeanPlotModel1.getMeansChangeNotifier().addListener(
				meanObsTableModel);

		obsAndMeanPlotModel2.getMeansChangeNotifier().addListener(
				meanObsTableModel);

		PhaseAndMeanPlotPane obsAndMeanChartPane = createPhaseAndMeanPlotPane(
				LocaleProps.get("PHASE_PLOT") + " " + LocaleProps.get("FOR")
						+ " " + objName, subTitle, obsAndMeanPlotModel1,
				obsAndMeanPlotModel2, epoch, period);

		// The observation table pane contains valid and potentially
		// invalid data components but for phase plot purposes, we only
		// display valid data, as opposed to the raw data view in which
		// both are shown. Tell the valid data table to have a horizontal
		// scrollbar if there will be too many columns.
		boolean enableColumnAutoResize = newStarMessage.getNewStarType() == NewStarType.NEW_STAR_FROM_SIMPLE_FILE
				|| newStarMessage.getNewStarType() == NewStarType.NEW_STAR_FROM_EXTERNAL_SOURCE;

		ObservationListPane obsListPane = new ObservationListPane(objName,
				validObsTableModel, null, enableColumnAutoResize,
				obsAndMeanPlotModel1.getVisibleSeries(),
				AnalysisType.PHASE_PLOT);

		SyntheticObservationListPane<AbstractMeanObservationTableModel> meansListPane = new SyntheticObservationListPane<AbstractMeanObservationTableModel>(
				meanObsTableModel, null);

		// Create a phase change message so that existing plot and tables can
		// update their GUI components and/or models accordingly. Also,
		// recording the series visibility map permits the existence of a phase
		// change creation listener that collects phase change messages for the
		// purpose of later being able to re-create the same phase plot.
		PhaseChangeMessage phaseChangeMessage = new PhaseChangeMessage(this,
				period, epoch, seriesVisibilityMap);
		phaseChangeNotifier.notifyListeners(phaseChangeMessage);

		// Observation-and-mean table and plot.
		AnalysisTypeChangeMessage phasePlotMsg = new AnalysisTypeChangeMessage(
				AnalysisType.PHASE_PLOT, obsAndMeanChartPane, obsListPane,
				meansListPane, ViewModeType.PLOT_OBS_MODE);

		analysisTypeMap.put(AnalysisType.PHASE_PLOT, phasePlotMsg);

		analysisTypeChangeNotifier.notifyListeners(phasePlotMsg);

		return phasePlotMsg;
	}

	/**
	 * Set the phases for a particular series in the observation category map.
	 * 
	 * @param type
	 *            The series type of the observations whose phases are to be
	 *            set.
	 * @param epoch
	 *            The epoch to use for the phase calculation.
	 * @param period
	 *            The period to use for the phase calculation.
	 */
	public void setPhasesForSeries(SeriesType type, double epoch, double period) {
		if (validObservationCategoryMap.containsKey(type)) {
			List<ValidObservation> obs = validObservationCategoryMap.get(type);
			PhaseCalcs.setPhases(obs, epoch, period);
		}
	}

	/**
	 * Block, waiting for a job to complete. We only want to block if there is a
	 * concurrent task in progress.
	 */
	public void waitForJobCompletion() {
		if (currTask != null && !currTask.isDone()) {
			try {
				currTask.get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
		}
	}

	/**
	 * Attempt to stop the current task.
	 */
	public void stopCurrentTask() {
		if (this.currTask != null) {
			this.currTask.cancel(true);
		}
	}

	/**
	 * Clear the current task if not already cleared.
	 */
	public void clearCurrentTask() {
		if (this.currTask != null) {
			this.currTask = null;
		}
	}

	/**
	 * Create the observation-and-mean plot pane for the current list of valid
	 * observations.
	 */
	private ObservationAndMeanPlotPane createObservationAndMeanPlotPane(
			String plotName, String subTitle,
			ObservationAndMeanPlotModel obsAndMeanPlotModel) {

		Dimension bounds = new Dimension((int) (TabbedDataPane.WIDTH * 0.9),
				(int) (TabbedDataPane.HEIGHT * 0.9));

		return new ObservationAndMeanPlotPane(plotName, subTitle,
				obsAndMeanPlotModel, bounds);
	}

	/**
	 * Create the observation-and-mean phase plot pane for the current list of
	 * valid observations.
	 */
	private PhaseAndMeanPlotPane createPhaseAndMeanPlotPane(String plotName,
			String subTitle, ObservationAndMeanPlotModel obsAndMeanPlotModel1,
			ObservationAndMeanPlotModel obsAndMeanPlotModel2, double epoch,
			double period) {

		Dimension bounds = new Dimension((int) (TabbedDataPane.WIDTH * 0.9),
				(int) (TabbedDataPane.HEIGHT * 0.9));

		return new PhaseAndMeanPlotPane(plotName, subTitle, bounds, epoch,
				period, obsAndMeanPlotModel1, obsAndMeanPlotModel2);
	}

	/**
	 * Create a period analysis dialog after the analysis is done. It only makes
	 * sense to apply the observations to a single band as per this Q & A
	 * between Matt Templeton and I:<br/>
	 * DB: Like mean curve creation in VStar, should we only apply DC DFT to a
	 * single band, e.g. visual? MT: Yes, because of two things: 1) The
	 * different bands will have different mean values, and 2) The different
	 * bands will have different amplitudes or frequencies depending on what is
	 * physically causing the variation. Variability caused by temperature
	 * changes can have wildly different amplitudes in U or B versus Rc or Ic.
	 */
	public void performPeriodAnalysis(PeriodAnalysisPluginBase plugin) {
		try {
			if (this.newStarMessage != null && this.validObsList != null) {
				SingleSeriesSelectionDialog dialog = new SingleSeriesSelectionDialog(
						obsAndMeanPlotModel);

				if (!dialog.isCancelled()) {
					SeriesType type = dialog.getSeries();

					// Note that this will work so long as we have a single
					// retriever. For additive loads we will either have to
					// use an aggregated retriever or add file-loaded obs
					// to the AID retriever.
					//
					// Question: do we want to do the same thing for polynomial
					// fit? Or do we need to allow a polynomial fit of a phase
					// plot?
					List<ValidObservation> obs = newStarMessage.getStarInfo()
							.getRetriever().getValidObservationCategoryMap()
							.get(type);
					
					this.getProgressNotifier().notifyListeners(
							ProgressInfo.START_PROGRESS);
					this.getProgressNotifier().notifyListeners(
							ProgressInfo.BUSY_PROGRESS);

					PeriodAnalysisTask task = new PeriodAnalysisTask(plugin,
							type, obs);

					this.currTask = task;
					task.execute();
				}
			}
		} catch (Exception e) {
			MessageBox.showErrorDialog(MainFrame.getInstance(), LocaleProps
					.get("PERIOD_ANALYSIS"), e);

			this.getProgressNotifier().notifyListeners(
					ProgressInfo.START_PROGRESS);

			MainFrame.getInstance().getStatusPane().setMessage("");
		}
	}

	/**
	 * Open the plot control dialog relevant to the current analysis mode. TODO:
	 * move to DocumentManager
	 */
	public void showPlotControlDialog() {
		String title = null;
		ObservationAndMeanPlotPane plotPane = analysisTypeMap.get(analysisType)
				.getObsAndMeanChartPane();
		TimeElementsInBinSettingPane binSettingPane = null;
		NamedComponent extra = null;

		if (analysisType == AnalysisType.RAW_DATA) {
			title = LocaleProps.get("LIGHT_CURVE_CONTROL_DLG_TITLE");
			binSettingPane = new TimeElementsInBinSettingPane(LocaleProps
					.get("DAYS_PER_MEAN_SERIES_BIN"), plotPane,
					JDTimeElementEntity.instance);
		} else if (analysisType == AnalysisType.PHASE_PLOT) {
			title = LocaleProps.get("PHASE_PLOT_CONTROL_DLG_TITLE");
			binSettingPane = new TimeElementsInBinSettingPane(LocaleProps
					.get("PHASE_STEPS_PER_MEAN_SERIES_BIN"), plotPane,
					PhaseTimeElementEntity.instance);
		}

		PlotControlDialog dialog = new PlotControlDialog(title, plotPane,
				binSettingPane, extra, analysisType);
		dialog.setVisible(true);
	}

	/**
	 * Open the model dialog.
	 */
	public void showModelDialog() {
		modelDialog.showDialog();
	}

	/**
	 * Open the phase plots dialog.
	 */
	public void showPhaseDialog() {
		phaseDialog.showDialog();
	}

	/**
	 * Perform a polynomial fit operation.
	 */
	public void performPolynomialFit() {
		try {
			if (this.newStarMessage != null && this.validObsList != null) {
				SingleSeriesSelectionDialog seriesDialog = new SingleSeriesSelectionDialog(
						obsAndMeanPlotModel);

				if (!seriesDialog.isCancelled()) {
					SeriesType type = seriesDialog.getSeries();

					int num = obsAndMeanPlotModel.getSrcTypeToSeriesNumMap()
							.get(type);

					List<ValidObservation> obs = obsAndMeanPlotModel
							.getSeriesNumToObSrcListMap().get(num);

					IPolynomialFitter polynomialFitter = new TSPolynomialFitter(
							obs);

					int minDegree = polynomialFitter.getMinDegree();
					int maxDegree = polynomialFitter.getMaxDegree();

					PolynomialDegreeDialog dialog = new PolynomialDegreeDialog(
							minDegree, maxDegree);

					if (!dialog.isCancelled()) {
						polynomialFitter.setDegree(dialog.getDegree());

						ModellingTask task = new ModellingTask(polynomialFitter);

						this.currTask = task;

						this.getProgressNotifier().notifyListeners(
								ProgressInfo.START_PROGRESS);
						this.getProgressNotifier().notifyListeners(
								ProgressInfo.BUSY_PROGRESS);

						task.execute();
					}
				}
			}
		} catch (Exception e) {
			MessageBox.showErrorDialog(MainFrame.getInstance(),
					"Polynomial Fit Error", e);

			this.getProgressNotifier().notifyListeners(
					ProgressInfo.START_PROGRESS);

			MainFrame.getInstance().getStatusPane().setMessage("");
		}
	}

	/**
	 * Perform a modelling operation (other than polynomial fit).
	 */
	public void performModellingOperation(IModel model) {
		try {
			ModellingTask task = new ModellingTask(model);

			this.currTask = task;

			this.getProgressNotifier().notifyListeners(
					ProgressInfo.START_PROGRESS);
			this.getProgressNotifier().notifyListeners(
					ProgressInfo.BUSY_PROGRESS);

			task.execute();
		} catch (Exception e) {
			MessageBox.showErrorDialog(MainFrame.getInstance(),
					"Modelling Error", e);

			this.getProgressNotifier().notifyListeners(
					ProgressInfo.START_PROGRESS);

			MainFrame.getInstance().getStatusPane().setMessage("");
		}
	}

	/**
	 * Invokes a tool plugin with the currently loaded observation set.
	 * 
	 * @param plugin
	 *            The tool plugin to be invoked.
	 */
	public void invokeTool(ObservationToolPluginBase plugin) {
		if (validObsList != null) {
			try {
				plugin.invoke(validObsList);
			} catch (Throwable t) {
				MessageBox.showErrorDialog("Tool Error", t);
			}
		} else {
			MessageBox.showMessageDialog(MainFrame.getInstance(), "Tool Error",
					"There are no observations loaded.");
		}
	}

	/**
	 * Applies the custom filter plugin to the currently loaded observation set.
	 * 
	 * @param plugin
	 *            The tool plugin to be invoked.
	 */
	public void applyCustomFilterToCurrentObservations(
			CustomFilterPluginBase plugin) {
		if (validObsList != null) {
			try {
				plugin.apply(validObsList);
			} catch (Throwable t) {
				MessageBox.showErrorDialog("Custom Filter Error", t);
			}
		} else {
			MessageBox.showMessageDialog(MainFrame.getInstance(),
					"Custom Filter", "There are no observations loaded.");
		}
	}

	/**
	 * Save the artefact corresponding to the current viewMode.
	 * 
	 * @param parent
	 *            The parent component to be used in dialogs.
	 */
	public void saveCurrentMode(Component parent) {
		List<ValidObservation> obs = null;

		switch (viewMode) {
		case PLOT_OBS_MODE:
			try {
				analysisTypeMap.get(analysisType).getObsAndMeanChartPane()
						.getChartPanel().doSaveAs();
			} catch (IOException ex) {
				MessageBox.showErrorDialog(parent,
						"Save Observation and Means Plot", ex.getMessage());
			}
			break;
		case LIST_OBS_MODE:
			saveObsListToFile(parent);
			break;
		case LIST_MEANS_MODE:
			obs = analysisTypeMap.get(analysisType).getMeansListPane()
					.getObsTableModel().getObs();
			saveSyntheticObsListToFile(parent, obs);
			break;
		case MODEL_MODE:
			if (modelSelectionMessage != null) {
				obs = modelSelectionMessage.getModel().getFit();
				saveSyntheticObsListToFile(parent, obs);
			}
			break;
		case RESIDUALS_MODE:
			if (modelSelectionMessage != null) {
				obs = modelSelectionMessage.getModel().getResiduals();
				saveSyntheticObsListToFile(parent, obs);
			}
			break;
		}
	}

	/**
	 * Save the current plot (as a PNG) to the specified file.
	 * 
	 * @param path
	 *            The file to write the PNG image to.
	 * @param width
	 *            The desired width of the image.
	 * @param height
	 *            The desired height of the image.
	 */
	public void saveCurrentPlotToFile(File file, int width, int height) {
		ChartPanel chart = analysisTypeMap.get(analysisType)
				.getObsAndMeanChartPane().getChartPanel();

		try {
			ChartUtilities
					.saveChartAsPNG(file, chart.getChart(), width, height);
		} catch (IOException e) {
			MessageBox.showErrorDialog("Save plot to file",
					"Cannot save plot to " + "'" + file.getPath() + "'.");
		}
	}

	/**
	 * Save observation list to a file in a separate thread. Note that we want
	 * to save just those observations that are in view in the observation list
	 * currently.
	 * 
	 * @param parent
	 *            The parent component to be used in dialogs.
	 * @param path
	 *            The path of the file to save to.
	 * @param delimiter
	 *            The delimiter between data items.
	 */
	public void saveObsListToFile(Component parent, File path, String delimiter) {
		if (analysisType == AnalysisType.RAW_DATA) {
			List<ValidObservation> obs = this.analysisTypeMap.get(analysisType)
					.getObsListPane().getObservationsInView();

			if (!obs.isEmpty()) {
				this.getProgressNotifier().notifyListeners(
						ProgressInfo.START_PROGRESS);

				this.getProgressNotifier()
						.notifyListeners(
								new ProgressInfo(ProgressType.MAX_PROGRESS, obs
										.size()));

				ObsListFileSaveTask task = new ObsListFileSaveTask(obs, path,
						this.newStarMessage.getNewStarType(), delimiter);

				this.currTask = task;
				task.execute();
			} else {
				MessageBox.showMessageDialog(parent, "Save Observations",
						"There are no visible observations to save.");
			}
		} else {
			MessageBox.showMessageDialog(parent, "Save Observations",
					"Observation data can only be saved in raw mode.");
		}
	}

	/**
	 * Save observation list to a file in a separate thread. Note that we want
	 * to save just those observations that are in view in the observation list
	 * currently. The file is requested from the user via a dialog.
	 * 
	 * @param parent
	 *            The parent component to be used in dialogs.
	 */
	private void saveObsListToFile(Component parent) {
		if (analysisType == AnalysisType.RAW_DATA) {
			List<ValidObservation> obs = this.analysisTypeMap.get(analysisType)
					.getObsListPane().getObservationsInView();

			if (!obs.isEmpty()) {
				if (obsListFileSaveDialog.showDialog(parent)) {
					File outFile = obsListFileSaveDialog.getSelectedFile();

					this.getProgressNotifier().notifyListeners(
							ProgressInfo.START_PROGRESS);

					this.getProgressNotifier().notifyListeners(
							new ProgressInfo(ProgressType.MAX_PROGRESS, obs
									.size()));

					ObsListFileSaveTask task = new ObsListFileSaveTask(obs,
							outFile, this.newStarMessage.getNewStarType(),
							obsListFileSaveDialog.getDelimiter());

					this.currTask = task;
					task.execute();
				}
			} else {
				MessageBox.showMessageDialog(parent, "Save Observations",
						"There are no visible observations to save.");
			}
		} else {
			MessageBox.showMessageDialog(parent, "Save Observations",
					"Observation data can only be saved in raw mode.");
		}
	}

	/**
	 * Save synthetic observation list (means, model, residuals) to a file in a
	 * separate thread.
	 * 
	 * @param parent
	 *            The parent component to be used in dialogs.
	 * @param obs
	 *            The list of observations to be saved.
	 */
	private void saveSyntheticObsListToFile(Component parent,
			List<ValidObservation> obs) {
		if (analysisType == AnalysisType.RAW_DATA) {

			if (!obs.isEmpty()) {
				if (obsListFileSaveDialog.showDialog(parent)) {
					File outFile = obsListFileSaveDialog.getSelectedFile();

					this.getProgressNotifier().notifyListeners(
							ProgressInfo.START_PROGRESS);

					this.getProgressNotifier().notifyListeners(
							new ProgressInfo(ProgressType.MAX_PROGRESS, obs
									.size()));

					// We re-use the same observation list file save task as
					// above but specify simple file type to match the fact that
					// we are only going to save JD, magnitude, and uncertainty
					// (for
					// means).
					ObsListFileSaveTask task = new ObsListFileSaveTask(obs,
							outFile, NewStarType.NEW_STAR_FROM_SIMPLE_FILE,
							obsListFileSaveDialog.getDelimiter());

					this.currTask = task;
					task.execute();
				}
			} else {
				MessageBox.showMessageDialog(parent, "Save Observations",
						"There are no visible observations to save.");
			}
		} else {
			MessageBox.showMessageDialog(parent, "Save Observations",
					"Observation data can only be saved in raw mode.");
		}
	}

	/**
	 * Print the artefact corresponding to the current mode.
	 * 
	 * @param parent
	 *            The parent component to be used by an error dialog.
	 */
	public void printCurrentMode(Component parent) {
		switch (viewMode) {
		case PLOT_OBS_MODE:
			this.analysisTypeMap.get(analysisType).getObsAndMeanChartPane()
					.getChartPanel().createChartPrintJob();
			break;

		case LIST_OBS_MODE:
			try {
				ObservationListPane obsListPane = this.analysisTypeMap.get(
						analysisType).getObsListPane();

				obsListPane.getValidDataTable().print(PrintMode.FIT_WIDTH);

				if (obsListPane.getInvalidDataTable() != null) {
					obsListPane.getInvalidDataTable()
							.print(PrintMode.FIT_WIDTH);
				}
			} catch (PrinterException e) {
				MessageBox.showErrorDialog(parent, "Print Observations", e
						.getMessage());
			}
			break;

		case LIST_MEANS_MODE:
			try {
				SyntheticObservationListPane<AbstractMeanObservationTableModel> meanObsListPane = this.analysisTypeMap
						.get(analysisType).getMeansListPane();

				meanObsListPane.getObsTable().print(PrintMode.FIT_WIDTH);
			} catch (PrinterException e) {
				MessageBox.showErrorDialog(parent, "Print Mean Values", e
						.getMessage());
			}
			break;
		case MODEL_MODE:
			if (modelSelectionMessage != null) {
				try {
					SyntheticObservationListPane<AbstractModelObservationTableModel> modelListPane = documentManager
							.getModelListPane(analysisType,
									modelSelectionMessage.getModel());

					modelListPane.getObsTable().print(PrintMode.FIT_WIDTH);
				} catch (PrinterException e) {
					MessageBox.showErrorDialog(parent, "Print Model Values", e
							.getMessage());
				}
			}
			break;
		case RESIDUALS_MODE:
			if (modelSelectionMessage != null) {
				try {
					SyntheticObservationListPane<AbstractModelObservationTableModel> residualsListPane = documentManager
							.getResidualsListPane(analysisType,
									modelSelectionMessage.getModel());

					residualsListPane.getObsTable().print(PrintMode.FIT_WIDTH);
				} catch (PrinterException e) {
					MessageBox.showErrorDialog(parent, "Print Residual Values",
							e.getMessage());
				}
			}
			break;
		}
	}

	/**
	 * Show the details of the currently selected observation in the current
	 * view mode (plot or table).
	 */
	public void showObservationDetails() {
		ValidObservation ob = null;

		switch (viewMode) {
		case PLOT_OBS_MODE:
			ob = this.analysisTypeMap.get(analysisType)
					.getObsAndMeanChartPane().getLastObSelected();
			break;
		case LIST_OBS_MODE:
			ob = this.analysisTypeMap.get(analysisType).getObsListPane()
					.getLastObSelected();
			break;
		case LIST_MEANS_MODE:
			ob = this.analysisTypeMap.get(analysisType).getMeansListPane()
					.getLastObSelected();
			break;
		case MODEL_MODE:
			ob = documentManager.getModelListPane(analysisType,
					modelSelectionMessage.getModel()).getLastObSelected();
			break;
		case RESIDUALS_MODE:
			ob = documentManager.getResidualsListPane(analysisType,
					modelSelectionMessage.getModel()).getLastObSelected();
			break;
		}

		if (ob != null) {
			new ObservationDetailsDialog(ob);
		} else {
			MessageBox.showWarningDialog("Observation Details",
					"No observation selected");
		}
	}

	/**
	 * Report a discrepant observation to AAVSO (if the dataset was
	 * AID-downloaded).
	 * 
	 * @param ob
	 *            The observation to be reported.
	 * @param dialog
	 *            A parent dialog to set non-visible and dispose. May be null.
	 */
	public void reportDiscrepantObservation(ValidObservation ob, JDialog dialog) {
		// If the dataset was loaded from AID and the change was
		// to mark this observation as discrepant, we ask the user
		// whether to report this to AAVSO.
		NewStarMessage newStarMessage = Mediator.getInstance()
				.getNewStarMessage();

		if (ob.isDiscrepant()
				&& newStarMessage.getNewStarType() == NewStarType.NEW_STAR_FROM_DATABASE) {

			String auid = newStarMessage.getStarInfo().getAuid();
			String name = ob.getName();
			int uniqueId = ob.getRecordNumber();

			DiscrepantReportDialog reportDialog = new DiscrepantReportDialog(
					auid, ob);

			if (!reportDialog.isCancelled()) {
				try {
					MainFrame.getInstance().setCursor(
							Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					Authenticator.getInstance().authenticate();

					String userName = ResourceAccessor.getLoginInfo()
							.getUserName();

					String editor = "vstar:" + userName;

					if (dialog != null) {
						dialog.setVisible(false);
					}

					// Create and submit the discrepant report.
					DiscrepantReport report = new DiscrepantReport(auid, name,
							uniqueId, editor, reportDialog.getComments());

					IDiscrepantReporter reporter = ZapperLogger.getInstance();

					reporter.lodge(report);

					MainFrame.getInstance().setCursor(null);

					if (dialog != null) {
						dialog.dispose();
					}
				} catch (CancellationException ex) {
					// Nothing to do; dialog cancelled.
				} catch (ConnectionException ex) {
					MessageBox.showErrorDialog("Authentication Source Error",
							ex);
				} catch (AuthenticationError ex) {
					MessageBox.showErrorDialog("Authentication Error", ex);
				} catch (Exception ex) {
					MessageBox
							.showErrorDialog("Discrepant Reporting Error", ex);
				} finally {
					MainFrame.getInstance().setCursor(null);
				}
			}
		}
	}

	/**
	 * Exit VStar.
	 */
	public void quit() {
		// TODO: do other cleanup, e.g. if file needs saving;
		// need a document model including undo for this;
		// defer to Mediator.
		System.exit(0);
	}
}
