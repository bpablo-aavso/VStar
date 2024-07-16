/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2009  AAVSO (http://www.aavso.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed inputStream the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

// PMAK 2021-12-28

package org.aavso.tools.vstar.external.plugin;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.aavso.tools.vstar.data.DateInfo;
import org.aavso.tools.vstar.data.Magnitude;
import org.aavso.tools.vstar.data.SeriesType;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.data.ValidObservation.JDflavour;
import org.aavso.tools.vstar.exception.ObservationReadError;
import org.aavso.tools.vstar.input.AbstractObservationRetriever;
import org.aavso.tools.vstar.plugin.InputType;
import org.aavso.tools.vstar.plugin.ObservationSourcePluginBase;
import org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog;
import org.aavso.tools.vstar.ui.dialog.DoubleField;
import org.aavso.tools.vstar.ui.dialog.IntegerField;
import org.aavso.tools.vstar.ui.dialog.MessageBox;
import org.aavso.tools.vstar.ui.dialog.NumberFieldBase;
import org.aavso.tools.vstar.ui.dialog.TextField;
import org.aavso.tools.vstar.ui.mediator.StarInfo;
import org.aavso.tools.vstar.util.Pair;
import org.aavso.tools.vstar.util.help.Help;
import org.aavso.tools.vstar.util.locale.LocaleProps;
import org.aavso.tools.vstar.util.prefs.NumericPrecisionPrefs;
import org.aavso.tools.vstar.vela.FunctionExecutor;
import org.aavso.tools.vstar.vela.Operand;
import org.aavso.tools.vstar.vela.Type;
import org.aavso.tools.vstar.vela.VeLaInterpreter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.aavso.tools.vstar.ui.mediator.Mediator;

/**
 * The plugin generates series from VeLa expression  
 */
public class VeLaObSource extends ObservationSourcePluginBase {
	
	private static final String FUNC_NAME = "F";
	private static final String OBJ_NAME = "VeLa model";
	
	private final NameAndDescription DEFAULT_SERIES_NAME_AND_DESCRIPTION = new NameAndDescription(OBJ_NAME, OBJ_NAME); 
	
	private NameAndDescription seriesNameAndDescription = DEFAULT_SERIES_NAME_AND_DESCRIPTION;
	
	private double minJD = 0.0;
	private double maxJD = 0.0;
	private int points = 0;
	private JDflavour jDflavour = JDflavour.UNKNOWN;	
	private String veLaCode = null;
	private String titleX = null;
	private String titleY = null;
	private String objectName = null;
	
	private static ParameterDialog paramDialog;
	
	public VeLaObSource() {
		super();
	}
	
	@Override
	public String getCurrentStarName() {
		return getInputName();
	}

	@Override
	public InputType getInputType() {
		return InputType.NONE;
	}
	
	@Override
	public String getDescription() {
		return OBJ_NAME + " as an observation source";
	}

	@Override
	public String getDisplayName() {
		return "New Star from " + OBJ_NAME + "...";
	}

	/**
	 * @see org.aavso.tools.vstar.plugin.IPlugin#getDocName()
	 */
	@Override
	public String getDocName() {
		return "VeLa Model Source Plug-In.pdf";
	}

	@Override
	public AbstractObservationRetriever getObservationRetriever() {
		if (paramDialog == null)
			paramDialog = new ParameterDialog();
		paramDialog.showDialog();
		if (paramDialog.isCancelled()) {
			return null;
		}
		
		minJD = paramDialog.getMinJD();
		maxJD = paramDialog.getMaxJD();;
		points = paramDialog.getPoints();
		jDflavour = paramDialog.getJDflavour();		
		veLaCode = paramDialog.getCode();
		titleX = paramDialog.getTitleX();
		titleY = paramDialog.getTitleY();
		objectName = paramDialog.getObjectName();
		
		isAdditive = paramDialog.isAdditive();
				
		if (objectName == null || "".equals(objectName.trim()))
			inputName = OBJ_NAME;
		else
			inputName = objectName;
		
		seriesNameAndDescription = getSeriesNameAndDescription();
		
		return new VeLaModelObsRetriever();
	}
	
	// Create unique series name and description
	private NameAndDescription getSeriesNameAndDescription() {
		
		if (!isAdditive)
			return DEFAULT_SERIES_NAME_AND_DESCRIPTION;
		
		Map<SeriesType, List<ValidObservation>> validObservationCategoryMap = Mediator.getInstance().getValidObservationCategoryMap();
		if (validObservationCategoryMap == null)
			return DEFAULT_SERIES_NAME_AND_DESCRIPTION;

		int n = 0;
		String tempDesc = getStringWithNumber(DEFAULT_SERIES_NAME_AND_DESCRIPTION.desc , n);
		while (true) {
			boolean found = false;
			for (Map.Entry<SeriesType, List<ValidObservation>> series : validObservationCategoryMap.entrySet()) {
				String desc = series.getKey().getDescription();
				if (desc != null) desc = desc.trim();
				if (tempDesc.equals(desc)) {
					found = true;
					n++;
					tempDesc = getStringWithNumber(DEFAULT_SERIES_NAME_AND_DESCRIPTION.desc, n);
					break;
				}
			}
			if (!found)
				break;
		}
		
		n = 0;
		String tempName = getStringWithNumber(tempDesc, n);
		while (true) {
			boolean found = false;
			for (Map.Entry<SeriesType, List<ValidObservation>> series : validObservationCategoryMap.entrySet()) {
				String name = series.getKey().getShortName();
				if (name != null) name = name.trim();
				if (tempName.equals(name)) {
					found = true;
					n++;
					tempName = getStringWithNumber(tempDesc, n);
					break;
				}
			}
			if (!found)
				break;
		}
		
		return new NameAndDescription(tempName, tempDesc);
	}
	
	private String getStringWithNumber(String s, int i) {
		if (i > 1)
			return s + " (" + i + ")";
		else
			return s;
	}
	
	private VeLaInterpreter createVeLaInterpreter(String veLaCode) throws ObservationReadError {
		// Create a VeLa interpreter instance.
		VeLaInterpreter vela = new VeLaInterpreter();

		// Evaluate the VeLa model code.
		vela.program(veLaCode);

		// Check if a model function "FUNC_NAME(T:REAL):REAL" is defined 		
		Optional<List<FunctionExecutor>> functions = vela.lookupFunctions(FUNC_NAME); 
		if (functions.isPresent()) {
			List<FunctionExecutor> funcExecutors = functions.get();
			if (funcExecutors.size() == 1) {
				FunctionExecutor executor = funcExecutors.get(0);
				List<Type> paramTypes = executor.getParameterTypes();
				if (paramTypes != null && paramTypes.size() == 1 && paramTypes.get(0) == Type.REAL) {
					Optional<Type> returnType = executor.getReturnType();
					if (returnType.isPresent() && returnType.get() == Type.REAL)
						return vela;
				}
			}
		}
		throw new ObservationReadError("A (non-overloaded) model function " + FUNC_NAME + "(T:REAL):REAL must be defined");
	}
	
	class VeLaModelObsRetriever extends AbstractObservationRetriever {
		
		public VeLaModelObsRetriever() {
			//
		}
		
		@Override
		public void retrieveObservations() throws ObservationReadError {
			try {
				retrieveVeLaModelObservations();
			} catch (Exception e) {
				throw new ObservationReadError(e.getLocalizedMessage());
			}
		}			

		private void retrieveVeLaModelObservations() throws ObservationReadError {
		
			setJDflavour(jDflavour);
			
			VeLaInterpreter vela = createVeLaInterpreter(veLaCode);

			Double magVal = null;
			
			double step = (maxJD - minJD) / (points - 1);
			
			for (int i = 0; i < points && !wasInterrupted(); i++) {
				
				double time = minJD + i * step;
				
				String funCall = FUNC_NAME + "(" + NumericPrecisionPrefs.formatTime(time) + ")";
				Optional<Operand> result = vela.program(funCall);
				if (result.isPresent() && result.get().getType() == Type.REAL) {
					magVal = result.get().doubleVal();
				} else {
					throw new ObservationReadError("VeLa expression: Expected a Real value");
				}

				if (magVal != null) {
					double uncertainty = 0;

					ValidObservation ob = new ValidObservation();
					
					String name = getStarInfo().getDesignation();
					ob.setName(name);
					
					ob.setDateInfo(new DateInfo(time));
					ob.setMagnitude(new Magnitude(magVal, uncertainty));
					ob.setBand(SeriesType.create(seriesNameAndDescription.desc, seriesNameAndDescription.name, Color.RED, false, false));
					ob.setRecordNumber(i);
					collectObservation(ob);
				}
				
				incrementProgress();
			}
		}

		@Override
		public String getSourceName() {
			return getInputName();
		}

		@Override
		public String getSourceType() {
			return OBJ_NAME + " observation source";
		}
		
		@Override
		public StarInfo getStarInfo() {
			String name = getSourceName();
			return new StarInfo(this, name);
		}

		@Override
		public Integer getNumberOfRecords() throws ObservationReadError {
			return points;
		}
		
		@Override
		public String getDomainTitle() {
			return titleX;
		}

		@Override
		public String getRangeTitle() {
			return titleY;
		}

	}

	@SuppressWarnings("serial")
	private class ParameterDialog extends AbstractOkCancelDialog {
		
		private ActionListener cancelListener;

		private DoubleField minJD;
		private DoubleField maxJD;
		private IntegerField points;
		private JComboBox<String> jDflavour;
		private JTextArea codeArea;
		private JCheckBox addToCurrent;
		private TextField titleXfield;
		private TextField titleYfield;
		private TextField objectNameField;
		private JButton clearButton;
		private JButton testButton;
		private JButton checkButton;
		private JButton loadButton;
		private JButton saveButton;
		
		private UndoManager undo;
		
		/**
		 * Constructor
		 */
		public ParameterDialog() {
			super("Function Code [model: f(t)]");
			
			cancelListener = createCancelButtonListener();
			getRootPane().registerKeyboardAction(cancelListener,
					KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			Container contentPane = this.getContentPane();

			JPanel topPane = new JPanel();
			topPane.setLayout((LayoutManager) new BoxLayout(topPane, BoxLayout.PAGE_AXIS));
			topPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			topPane.add(createControlPane());
			topPane.add(createControlPane2());

			topPane.add(createCodePane());
			//topPane.add(createFileControlPane());
			
			// OK, Cancel
			topPane.add(createButtonPane());

			contentPane.add(topPane);
			
			addCodeAreaUndoManager();
			
			this.pack();
			//this.setResizable(false);
			// Singleton mode! Use showDialog()!
			//setLocationRelativeTo(Mediator.getUI().getContentPane());
			//this.setVisible(true);
			
			clearInput();
		}
		
		private void addCodeAreaUndoManager() {
			
			int keyMask;
			try {
				keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			} catch (HeadlessException ex) {
				return;
			}
			
			undo = new UndoManager();
			javax.swing.text.Document doc = codeArea.getDocument();
			doc.addUndoableEditListener(new UndoableEditListener() {
				public void undoableEditHappened(UndoableEditEvent e) {
					undo.addEdit(e.getEdit());
				}
			});
			
			InputMap im = codeArea.getInputMap(JComponent.WHEN_FOCUSED);
			ActionMap am = codeArea.getActionMap();

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, keyMask), "Undo");
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, keyMask), "Redo");

			am.put("Undo", new AbstractAction() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
		            if (undo.canUndo())
		            	try {
			                undo.undo();
		            	} catch (CannotUndoException ex) {
		            		//ex.printStackTrace();
		            	}
			    }
			});
			
			am.put("Redo", new AbstractAction() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
		            if (undo.canRedo())
		            	try {
			                undo.redo();
		            	} catch (CannotUndoException ex) {
		            		//ex.printStackTrace();
		            	}
			    }
			});			
		}
		
		public boolean isAdditive() {
			return addToCurrent.isSelected();
		}
		
		public Double getMinJD() {
			return minJD.getValue();
		}

		public Double getMaxJD() {
			return maxJD.getValue();
		}

		public Integer getPoints() {
			return points.getValue();
		}
		
		public String getCode() {
			return codeArea.getText();
		}
		
		public String getTitleX() {
			return titleXfield.getValue();
		}

		public String getTitleY() {
			return titleYfield.getValue();
		}

		public String getObjectName() {
			return objectNameField.getValue();
		}

		public JDflavour getJDflavour() {
			switch (jDflavour.getSelectedIndex()) {
				case 0: return JDflavour.JD;
				case 1: return JDflavour.HJD;
				case 2: return JDflavour.BJD;
				default: return JDflavour.UNKNOWN;
			}
		}
		
		private int dateTypeToSelectedIndex(String dateType) {
			if (dateType == null)
				dateType = "";
			switch (dateType) {
				case "JD": 
					return 0;
				case "HJD":
					return 1;
				case "BJD":
					return 2;
				default:
					return 1; // HJD
			}
		}
		
		private String jDflavourToString(JDflavour jDflavour) {
			switch (jDflavour) {
				case JD: 
					return "JD";
				case HJD:
					return "HJD";
				case BJD:
					return "BJD";
				default:
					return "UNKNOWN";
			}
		}
		
		private JPanel createControlPane() {
			JPanel panel = new JPanel(new FlowLayout());
			
			minJD = new DoubleField("Minimum JD", null, null, null);
			((JTextField)(minJD.getUIComponent())).setColumns(12);
			panel.add(minJD.getUIComponent());			

			maxJD = new DoubleField("Maximum JD", null, null, null);
			((JTextField)(maxJD.getUIComponent())).setColumns(12);			
			panel.add(maxJD.getUIComponent());

			points = new IntegerField("Points", 2, null, null);
			((JTextField)(points.getUIComponent())).setColumns(12);			
			panel.add(points.getUIComponent());
			
			String jdFlavourFieldName = "Date Type";
			jDflavour = new JComboBox<String>();
			//jDflavour.setBorder(BorderFactory.createTitledBorder(jdFlavourFieldName));
			jDflavour.setToolTipText("Select " + jdFlavourFieldName);
			
			jDflavour.addItem(JDflavour.JD.toString());
			jDflavour.addItem(JDflavour.HJD.toString());
			jDflavour.addItem(JDflavour.BJD.toString());
			jDflavour.setSelectedIndex(dateTypeToSelectedIndex("HJD")); // HJD
			panel.add(jDflavour);
			
			panel.add(Box.createRigidArea(new Dimension(25, 0)));
			
			addToCurrent = new JCheckBox("Add to current?");
			addToCurrent.setSelected(true);
			panel.add(addToCurrent);
			
			return panel;
		}
		
		private JPanel createControlPane2() {
			JPanel panel = new JPanel(new FlowLayout());
			
			titleXfield = new TextField("Domain Axis Title");
			((JTextField)(titleXfield.getUIComponent())).setColumns(30);
			panel.add(titleXfield.getUIComponent());

			titleYfield = new TextField("Range Axis Title");
			((JTextField)(titleYfield.getUIComponent())).setColumns(30);
			panel.add(titleYfield.getUIComponent());

			objectNameField = new TextField("Object Name");
			((JTextField)(objectNameField.getUIComponent())).setColumns(30);
			panel.add(objectNameField.getUIComponent());
			
			return panel;
		}

		private JPanel createCodePane() {
			JPanel panel = new JPanel();
			panel.setLayout((LayoutManager) new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			codeArea = new JTextArea(24, 80);
			Font font = codeArea.getFont();
			codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, font.getSize()));
			JScrollPane scrollPane = new JScrollPane(codeArea);
			panel.add(scrollPane);
			return panel;
		}
		
		private JPanel createFileControlPane() {
			JPanel panel = new JPanel(new FlowLayout());
			
			clearButton = new JButton(LocaleProps.get("CLEAR_BUTTON"));
			panel.add(clearButton);
			clearButton.addActionListener(createClearButtonActionListener());

			// to-do: localize
			testButton = new JButton("Test");
			panel.add(testButton);
			testButton.addActionListener(createTestButtonActionListener());

			checkButton = new JButton("Check");
			panel.add(checkButton);
			checkButton.addActionListener(createCheckButtonActionListener());
			
			loadButton = new JButton(LocaleProps.get("LOAD_BUTTON"));
			panel.add(loadButton);
			loadButton.addActionListener(createLoadButtonActionListener());

			saveButton = new JButton(LocaleProps.get("SAVE_BUTTON"));
			panel.add(saveButton);
			saveButton.addActionListener(createSaveButtonActionListener());
			
			return panel;
		}
		
		@Override
		protected JPanel createButtonPane() {
			JPanel panel = new JPanel(new FlowLayout());

			JButton helpButton = new JButton(LocaleProps.get("HELP_MENU"));
			helpButton.addActionListener(createHelpButtonListener());
			panel.add(helpButton);
			
			JButton cancelButton = new JButton(LocaleProps.get("CANCEL_BUTTON"));
			cancelButton.addActionListener(cancelListener);
			panel.add(cancelButton);

			JPanel fileControls = createFileControlPane();
			panel.add(fileControls);
			
			okButton = new JButton(LocaleProps.get("OK_BUTTON"));
			okButton.addActionListener(createOKButtonListener());
			panel.add(okButton);

			this.getRootPane().setDefaultButton(okButton);
			
			return panel;
		}
		
		ActionListener createClearButtonActionListener() {
			return new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					clearInput();
				}
			};
		}

		ActionListener createTestButtonActionListener() {
			return new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					testInput();
				}
			};
		}

		ActionListener createCheckButtonActionListener() {
			return new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					checkInput();
				}
			};
		}
		
		private ActionListener createLoadButtonActionListener() {
			return new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					readVelaXML();
				}
			};
		}
		
		private ActionListener createSaveButtonActionListener() {
			return new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					writeVelaXML();
				}
			};
		}
		
		private void clearInput() {
			minJD.setValue(null);
			maxJD.setValue(null);
			points.setValue(null);
			jDflavour.setSelectedIndex(dateTypeToSelectedIndex("HJD"));
			codeArea.setText("");
			titleXfield.setValue("");
			titleYfield.setValue("");
			objectNameField.setValue("");
		}
		
		private void testInput() {
			minJD.setValue(2457504.8);
			maxJD.setValue(2457505.0);
			points.setValue(501);
			jDflavour.setSelectedIndex(dateTypeToSelectedIndex("HJD"));
			codeArea.setText(
					"# test model\n\n" +
					"zeroPoint is 2457504.93 # time zero point\n" +
					"magZeroPoint is 13.69\n" +
					"period is 0.0850674\n\n" +
					"f(t: real): real {\n" +
					"   magZeroPoint\n" +
					"   + 0.091481685488957 * cos(2*PI*(1/period)*(t-zeroPoint)) + 0.114900355450183 * sin(2*PI*(1/period)*(t-zeroPoint))\n" +
					"   - 0.031986371275697 * cos(2*PI*(2/period)*(t-zeroPoint)) - 0.029782272061918 * sin(2*PI*(2/period)*(t-zeroPoint))\n" +
					"   - 0.005402185898561 * cos(2*PI*(3/period)*(t-zeroPoint)) + 0.001484256405225 * sin(2*PI*(3/period)*(t-zeroPoint))\n" +
					"   + 0.006091217702922 * cos(2*PI*(4/period)*(t-zeroPoint)) + 0.001654399074451 * sin(2*PI*(4/period)*(t-zeroPoint))\n" +
					"   - 0.004698206584795 * cos(2*PI*(5/period)*(t-zeroPoint)) - 0.000039671630067 * sin(2*PI*(4/period)*(t-zeroPoint))\n" +
					"   + 0.003549883073703 * cos(2*PI*(6/period)*(t-zeroPoint)) + 0.000022578051393 * sin(2*PI*(6/period)*(t-zeroPoint))\n" +
					"}\n");
			titleXfield.setValue("");
			titleYfield.setValue("");
			objectNameField.setValue("");
		}
		
		private boolean checkInput() {

			Double minJDval = getMinJD();
			Double maxJDval = getMaxJD();
			Integer pointsval = getPoints();
			
			if (minJDval == null) {
				valueError(minJD);
				return false;
			}
			
			if (maxJDval == null) {
				valueError(maxJD);
				return false;
			}
			
			if (pointsval == null) {
				valueError(points);
				return false;
			}

			if (pointsval < 2) {
				MessageBox.showErrorDialog(this, getTitle(), "Number of points must be >1");
				return false;
			}
			
			double step = (maxJDval - minJDval) / (pointsval - 1);
			if (step <= 0) {
				MessageBox.showErrorDialog(this, "Error", "Maximum JD must be greater than Minimum JD");
				return false;
			}
			
			if ("".equals(getCode().trim())) {
				MessageBox.showErrorDialog(this, getTitle(), "VeLa model must be defined.");
				return false;
			}
			
			if (getJDflavour() == JDflavour.UNKNOWN) {
				MessageBox.showErrorDialog(this, getTitle(), "Undefined DATE type");
				return false;
			}
			
			String veLaCode = getCode();
			if (veLaCode == null || "".equals(veLaCode.trim())) {
				MessageBox.showErrorDialog(this, getTitle(), "VeLa model is not defined");
				return false;
			}
			
			try {
				createVeLaInterpreter(veLaCode);
			} catch (Exception ex) {
				MessageBox.showErrorDialog(this, getTitle(), ex.getLocalizedMessage());
				return false;
			}
			
			return true;

		}
		
		private void readVelaXML() {
			try {
				Pair<byte[], String> content = Mediator.getInstance().getVelaXMLloadDialog().readFileAsBytes(ParameterDialog.this);
				if (content != null) {
					clearInput();
					try {
						processXMLbytes(content.first);
					} catch (Exception ex) {
						if ("vlx".equalsIgnoreCase(content.second)) {
							// We assume that file should be VeLa XML if it had 'vlx' extension.
							// In this case we are displaying the warning.
							MessageBox.showWarningDialog(ParameterDialog.this, getTitle(),
									"Cannot parse XML content. The data will be loaded as plain text.\n\n" +
									"Message:\n" + ex.getLocalizedMessage());
						}
						clearInput();
						codeArea.setText(new String(content.first, Charset.defaultCharset()));
						codeArea.setCaretPosition(0);
					}
				}
			} catch (Exception ex) {
				MessageBox.showErrorDialog(ParameterDialog.this, getTitle(), ex);
			}
		}

		private void processXMLbytes(byte[] xml) throws Exception {
			// https://mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
			
			// Instantiate the Factory
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();			
			// process XML securely, avoid attacks like XML External Entities (XXE)
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// parse XML file
	        DocumentBuilder db = dbf.newDocumentBuilder(); 
	        Document doc = db.parse(new InputSource(new ByteArrayInputStream(xml)));

	        // optional, but recommended
	        // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	        doc.getDocumentElement().normalize();

	        Node root = doc.getDocumentElement();

	        if ("VELA_MODEL".equals(root.getNodeName())) {
		        Element element = (Element)root;
	        	codeArea.setText(getNodeTextContent(element, "code"));
	        	codeArea.setCaretPosition(0);
	        	((JTextField)(minJD.getUIComponent())).setText(getNodeTextContent(element, "minJD"));
	        	((JTextField)(maxJD.getUIComponent())).setText(getNodeTextContent(element, "maxJD"));
	        	((JTextField)(points.getUIComponent())).setText(getNodeTextContent(element, "points"));
       			jDflavour.setSelectedIndex(dateTypeToSelectedIndex(getNodeTextContent(element, "datetype")));
       			((JTextField)(titleXfield.getUIComponent())).setText(getNodeTextContent(element, "domainTitle"));
       			((JTextField)(titleYfield.getUIComponent())).setText(getNodeTextContent(element, "rangeTitle"));
       			((JTextField)(objectNameField.getUIComponent())).setText(getNodeTextContent(element, "objectName"));
	        } else {
	        	throw new Exception("The document root is not VELA_MODEL");
	        }
		}
		
		private String getNodeTextContent(Element element, String name) {
			String s = null;
			NodeList list = element.getElementsByTagName(name);
			if (list.getLength() == 1) {
				Node node = list.item(0);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					s = node.getTextContent();
				}
			}
			return s;			
        }
		
		private void writeVelaXML() {
			try	{
				String content = getVelaXMLstring();
				Mediator.getInstance().getVelaXMLsaveDialog().writeStringToFile(ParameterDialog.this, content, StandardCharsets.UTF_8);
			} catch (Exception ex) {
				MessageBox.showErrorDialog(ParameterDialog.this, getTitle(), ex);
			}
		}
		
		private String getVelaXMLstring() throws ParserConfigurationException, TransformerException {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(getVelaXML()), new StreamResult(writer));
			return writer.getBuffer().toString();
		}
		
		private Document getVelaXML() throws ParserConfigurationException {
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();			
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			Document doc = documentBuilder.newDocument();
			
			// root element
            Element root = doc.createElement("VELA_MODEL");
            doc.appendChild(root);			
            
            Element element;
            
            element = doc.createElement("minJD");
            element.appendChild(doc.createTextNode(minJD.getStringValue()));
            root.appendChild(element);
            
            element = doc.createElement("maxJD");
            element.appendChild(doc.createTextNode(maxJD.getStringValue()));
            root.appendChild(element);

            element = doc.createElement("points");
            element.appendChild(doc.createTextNode(points.getStringValue()));
            root.appendChild(element);
          
            element = doc.createElement("datetype");
            element.appendChild(doc.createTextNode(jDflavourToString(getJDflavour())));
            root.appendChild(element);
            
            element = doc.createElement("domainTitle");
            element.appendChild(doc.createTextNode(titleXfield.getStringValue()));
            root.appendChild(element);

            element = doc.createElement("rangeTitle");
            element.appendChild(doc.createTextNode(titleYfield.getStringValue()));
            root.appendChild(element);

            element = doc.createElement("objectName");
            element.appendChild(doc.createTextNode(objectNameField.getStringValue()));
            root.appendChild(element);
            
            element = doc.createElement("code");
            element.appendChild(doc.createTextNode(codeArea.getText()));
            root.appendChild(element);
            
            return doc;
		}

		/**
		 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#helpAction()
		 */
		@Override
		protected void helpAction() {
			Help.openPluginHelp(getDocName());
		}

		/**
		 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#cancelAction()
		 */
		@Override
		protected void cancelAction() {
			// Nothing to do.
		}

		/**
		 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#okAction()
		 */
		@Override
		protected void okAction() {
			if (checkInput()) {
				cancelled = false;
				setVisible(false);
				dispose();
			}
		}
		
		
		private void valueError(NumberFieldBase<?> field) {
			MessageBox.showErrorDialog(this, "Error", field.getName() + ": Invalid value!\n" +
					numberFieldInfo(field));				
			(field.getUIComponent()).requestFocus();
			((JTextField)(field.getUIComponent())).selectAll();
			
		}
		
		private String numberFieldInfo(NumberFieldBase<?> field) {
			String s = "";
			if (field.getMin() == null && field.getMax() != null)
				s = "Only values <= " + field.getMax() + " allowed.";
			else if (field.getMin() != null && field.getMax() == null)
				s = "Only values >= " + field.getMin() + " allowed.";
			else if (field.getMin() != null && field.getMax() != null)
				s = "Only values between " + field.getMin() + " and " + field.getMax() + " allowed.";
			return s; 
		}
		
	}
	
	private class NameAndDescription {
		public String name;
		public String desc;
		NameAndDescription(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}
	}

}
