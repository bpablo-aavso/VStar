/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2012  AAVSO (http://www.aavso.org/)
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
package org.aavso.tools.vstar.ui.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

/**
 * This class implements a dialog to obtain one or more string values from text
 * boxes.
 */
@SuppressWarnings("serial")
public class TextDialog extends AbstractOkCancelDialog {

	private List<TextField> textFields;

	/**
	 * Constructor.
	 * 
	 * @param title
	 *            The title to be used for the dialog.
	 * @param fields
	 *            An array of fields.
	 */
	public TextDialog(String title, TextField[] fields) {
		super(title);
		this.setModal(true);

		Container contentPane = this.getContentPane();

		JPanel topPane = new JPanel();
		topPane.setLayout(new BoxLayout(topPane, BoxLayout.PAGE_AXIS));
		topPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		textFields = new ArrayList<TextField>();

		for (TextField field : fields) {
			textFields.add(field);
			topPane.add(createTextFieldPane(field));
			topPane.add(Box.createRigidArea(new Dimension(75, 10)));
		}

		// OK, Cancel
		topPane.add(createButtonPane());

		contentPane.add(topPane);

		this.pack();
	}

	/**
	 * Get a list of strings from the text fields.
	 * 
	 * @return a list of strings.
	 */
	public List<String> getTextStrings() {
		List<String> strings = new ArrayList<String>();

		for (TextField field : textFields) {
			strings.add(field.getValue());
		}

		return strings;
	}

	private JPanel createTextFieldPane(TextField field) {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

		JTextComponent textField = field.getTextField();
		textField.setEditable(!field.isReadOnly());
		if (textField instanceof JTextArea) {
			// Make text areas scrollable since we don't know how much content
			// one will have.
			panel.add(new JScrollPane(textField));
		} else {
			panel.add(textField);
		}

		return panel;
	}

	/**
	 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#cancelAction()
	 */
	protected void cancelAction() {
		// Nothing to do
	}

	/**
	 * @see org.aavso.tools.vstar.ui.dialog.AbstractOkCancelDialog#okAction()
	 */
	protected void okAction() {
		// If there is a field that cannot be empty, but is, we cannot dismiss
		// the dialog.
		for (TextField field : textFields) {
			if (!field.canBeEmpty() && field.getValue().trim().length() == 0) {
				return;
			}
		}

		cancelled = false;
		setVisible(false);
		dispose();
	}
}
