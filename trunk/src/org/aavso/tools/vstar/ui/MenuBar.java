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
package org.aavso.tools.vstar.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.aavso.tools.vstar.data.InvalidObservation;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.exception.ObservationReadError;
import org.aavso.tools.vstar.input.ObservationRetrieverBase;
import org.aavso.tools.vstar.input.SimpleTextFormatReader;
import org.aavso.tools.vstar.ui.model.InvalidObservationTableModel;
import org.aavso.tools.vstar.ui.model.ObservationPlotModel;
import org.aavso.tools.vstar.ui.model.ValidObservationTableModel;

/**
 * VStar's menu bar.
 */
public class MenuBar extends JMenuBar {

	private JFileChooser fileOpenDialog;

	// The parent window.
	private MainFrame parent;

	// Menu items.
	JMenuItem fileNewStarFromDatabaseItem;
	JMenuItem fileNewStarFromFileItem;
	JMenuItem fileSaveItem;
	JMenuItem filePrintItem;
	JMenuItem fileQuitItem;

	JMenuItem analysisRawDataItem;
	JMenuItem analysisPhasePlotItem;
	JMenuItem analysisPeriodSearchItem;

	JMenuItem helpContentsItem;
	JMenuItem helpAboutItem;

	/**
	 * Constructor
	 */
	public MenuBar(MainFrame parent) {
		super();

		this.parent = parent;

		List<String> extensions = new ArrayList<String>();
		extensions.add("sim");
		extensions.add("simple");
		this.fileOpenDialog = new JFileChooser();
		this.fileOpenDialog.setFileFilter(new FileExtensionFilter(extensions));

		createFileMenu();
		createAnalysisMenu();
		createHelpMenu();
	}

	private void createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		fileNewStarFromDatabaseItem = new JMenuItem(
				"New Star from AAVSO Database...");
		fileNewStarFromDatabaseItem
				.addActionListener(createNewStarFromDatabaseListener());
		fileNewStarFromDatabaseItem.setEnabled(false);
		fileMenu.add(fileNewStarFromDatabaseItem);

		fileNewStarFromFileItem = new JMenuItem("New Star from File...");
		fileNewStarFromFileItem
				.addActionListener(createNewStarFromFileListener());
		fileMenu.add(fileNewStarFromFileItem);

		fileSaveItem = new JMenuItem("Save...");
		fileSaveItem.addActionListener(this.createSaveLightCurveListener());
		fileSaveItem.setEnabled(false);
		fileMenu.add(fileSaveItem);

		filePrintItem = new JMenuItem("Print...");
		filePrintItem.addActionListener(this.createPrintLightCurveListener());
		filePrintItem.setEnabled(false);
		fileMenu.add(filePrintItem);

		fileMenu.addSeparator();

		fileQuitItem = new JMenuItem("Quit", KeyEvent.VK_Q);
		fileQuitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
				ActionEvent.META_MASK));
		fileQuitItem.addActionListener(createQuitListener());
		fileMenu.add(fileQuitItem);

		this.add(fileMenu);
	}

	private void createAnalysisMenu() {
		JMenu analysisMenu = new JMenu("Analysis");

		analysisRawDataItem = new JMenuItem("Raw Data");
		// TODO: select card in conjunction with mode radio group
		analysisMenu.add(analysisRawDataItem);

		analysisPhasePlotItem = new JMenuItem("Phase Plot...");
		analysisMenu.add(analysisPhasePlotItem);

		analysisPeriodSearchItem = new JMenuItem("Period Search...");
		analysisPeriodSearchItem.setEnabled(false);
		analysisMenu.add(analysisPeriodSearchItem);

		this.add(analysisMenu);
	}

	private void createHelpMenu() {
		JMenu helpMenu = new JMenu("Help");

		helpContentsItem = new JMenuItem("Help Contents...", KeyEvent.VK_H);
		helpContentsItem.setEnabled(false);
		helpMenu.add(helpContentsItem);

		helpMenu.addSeparator();

		helpAboutItem = new JMenuItem("About...", KeyEvent.VK_A);
		helpAboutItem.addActionListener(createAboutListener());
		helpMenu.add(helpAboutItem);

		this.add(helpMenu);
	}

	/**
	 * Returns the action listener to be invoked for File->New Star from AAVSO
	 * Database...
	 */
	private ActionListener createNewStarFromDatabaseListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO
			}
		};
	}

	/**
	 * Returns the action listener to be invoked for File->New Star from File...
	 * 
	 * The action is to open a file dialog to allow the user to select a single
	 * file.
	 */
	private ActionListener createNewStarFromFileListener() {
		final JFileChooser fileOpenDialog = this.fileOpenDialog;
		final MainFrame parent = this.parent;
		final MenuBar self = this;

		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fileOpenDialog.showOpenDialog(parent);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File f = fileOpenDialog.getSelectedFile();

					try {
						List<ValidObservation> validObs = self
								.createObservationTab(f);

						self.createLightCurve(f.getName(), validObs);
					} catch (Exception ex) {
						MessageBox.showErrorDialog(parent,
								"New Star from File", ex.getMessage());
					}
				}
			}
		};
	}

	/**
	 * Returns the action listener to be invoked for File->Save Light Curve...
	 */
	private ActionListener createSaveLightCurveListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					parent.getLightCurveChartPane().doSaveAs();
				} catch (IOException ex) {
					MessageBox.showErrorDialog(parent, "Light Curve Save", ex
							.getMessage());
				}
			}
		};
	}

	/**
	 * Returns the action listener to be invoked for File->Print Light Curve...
	 */
	private ActionListener createPrintLightCurveListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.getLightCurveChartPane().createChartPrintJob();
			}
		};
	}

	/**
	 * Returns the action listener to be invoked for File->Quit
	 */
	private ActionListener createQuitListener() {
		return new ActionListener() {
			// TODO: do other cleanup, e.g. if file needs saving;
			// need a document model including undo for this
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
	}

	/**
	 * Returns the action listener to be invoked for Help->About...
	 */
	private ActionListener createAboutListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				StringBuffer strBuf = new StringBuffer();
				strBuf.append("VStar (alpha)\n\n");
				strBuf
						.append("A variable star observation data analysis tool\n");
				strBuf.append("developed for:\n\n");
				strBuf.append("  The American Association of Variable Star\n");
				strBuf.append("  Observers: http://www.aavso.org/\n\n");
				strBuf.append("  and\n\n");
				strBuf
						.append("  The CitizenSky Project: http://www.citizensky.org/\n\n");
				strBuf.append("Code By: David Benn, Sara Beck, Kate Davis\n");
				strBuf.append("Contact: aavso@aavso.org\n");
				strBuf.append("License: GNU Affero General Public License\n\n");
				strBuf
						.append("Thanks to the following AAVSO staff for support:\n");
				strBuf
						.append("Sara Beck, Arne Henden, Doc Kinne, Aaron Price,");
				strBuf
						.append("Matt Templeton, Rebecca Turner, and Elizabeth Waagen.");

				MessageBox
						.showMessageDialog(parent, "VStar", strBuf.toString());
			}
		};
	}

	/**
	 * Create a table in a new tab for the observations in the file.
	 * 
	 * @param obsFile
	 *            The file from which to read observations.
	 * @throws IOException
	 * @throws ObservationReadError
	 * @return The list of valid observations.
	 */
	private List<ValidObservation> createObservationTab(File obsFile)
			throws IOException, ObservationReadError {
		FileReader fileReader = new FileReader(obsFile.getPath());

		// TODO: Use a factory method to determine observation
		// retriever class to use given the file type.
		ObservationRetrieverBase simpleTextFormatReader = new SimpleTextFormatReader(
				new LineNumberReader(fileReader));

		simpleTextFormatReader.retrieveObservations();

		List<ValidObservation> validObs = simpleTextFormatReader
				.getValidObservations();

		List<InvalidObservation> invalidObs = simpleTextFormatReader
				.getInvalidObservations();

		// Add a new tab with the observation data.
		ValidObservationTableModel validObsModel = null;
		InvalidObservationTableModel invalidObsModel = null;

		if (!validObs.isEmpty()) {
			validObsModel = new ValidObservationTableModel(validObs);
		}

		if (!invalidObs.isEmpty()) {
			invalidObsModel = new InvalidObservationTableModel(invalidObs);
		}

		this.parent.getTabs().insertTab(obsFile.getName(),
				null, // TODO: icon, close box
				new SimpleTextFormatObservationPane(validObsModel,
						invalidObsModel), obsFile.getPath(), 0);

		this.parent.getTabs().setSelectedIndex(0);

		return validObs;
	}

	/**
	 * Create the light curve for a list of valid observations.
	 * 
	 * @param obsName
	 *            The name of the observation list.
	 * @param validObs
	 *            The observation list.
	 */
	private void createLightCurve(String obsName,
			List<ValidObservation> validObs) {
		if (parent.getObsModel() == null) {
			ObservationPlotModel model = new ObservationPlotModel(obsName,
					validObs);
			parent.setObsModel(model);
			Dimension bounds = new Dimension((int) (parent.getWidth() * 0.75),
					(int) (parent.getHeight() * 0.75));
			// TODO: make title more meaningful
			LightCurvePane lightCurvePane = new LightCurvePane(
					"JD vs Magnitude", model, bounds);

			for (int i = 0; i < parent.getTabs().getTabCount(); i++) {
				String tabName = parent.getTabs().getTitleAt(i);
				if (MainFrame.LIGHT_CURVE.equals(tabName)) {
					parent.getTabs().setComponentAt(i, lightCurvePane);
					parent.setLightCurveChartPane(lightCurvePane);
					break;
				}
			}

			this.enableOutputMenuItems();
		}
		// } else {
		// // Add to the existing plot model.
		// parent.getObsModel().addObservationSeries(obsName, validObs);
		// }
	}

	private void enableOutputMenuItems() {
		this.fileSaveItem.setEnabled(true);
		this.filePrintItem.setEnabled(true);
	}
}
