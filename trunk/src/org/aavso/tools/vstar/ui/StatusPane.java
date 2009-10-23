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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.BevelBorder;

import org.aavso.tools.vstar.ui.controller.ModelManager;
import org.aavso.tools.vstar.ui.controller.NewStarMessage;
import org.aavso.tools.vstar.ui.model.NewStarType;
import org.aavso.tools.vstar.ui.model.ProgressInfo;
import org.aavso.tools.vstar.util.notification.Listener;

/**
 * A status panel containing a text status message component and a
 * status bar.
 * 
 * The intention is that this should be added to the bottom of the GUI.
 * 
 * This class will also listen to various events.
 */
public class StatusPane extends JPanel {

	private ModelManager modelMgr = ModelManager.getInstance();

	private JLabel statusLabel;
	private JProgressBar progressBar;

	/**
	 * Constructor.
	 * 
	 * @param firstMsg
	 *            The first message to be displayed in the status pane.
	 */
	public StatusPane(String firstMsg) {
		super(true);
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

		statusLabel = new JLabel();
		statusLabel.setHorizontalAlignment(JLabel.LEFT);
		statusLabel.setText(firstMsg);
		this.add(statusLabel);

		this.add(Box.createHorizontalGlue());

		this.progressBar = new JProgressBar();
		this.add(progressBar);

		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		modelMgr.getNewStarNotifier().addListener(createNewStarListener());
		modelMgr.getProgressNotifier().addListener(createProgressListener());
	}

	/**
	 * Set the status message to be displayed.
	 * 
	 * @param msg
	 *            The message to be displayed.
	 */
	public void setMessage(String msg) {
		this.statusLabel.setText(" " + msg);
	}

	/**
	 * Set the minimum progress bar value.
	 * 
	 * @param n
	 *            The minimum value to set.
	 */
	public void setMinProgressValue(int n) {
		this.progressBar.setMinimum(n);
		assert (this.progressBar.getMinimum() <= this.progressBar.getMaximum());
	}

	/**
	 * Set the maximum progress bar value.
	 * 
	 * @param n
	 *            The maximum value to set.
	 */
	public void setMaxProgressValue(int n) {
		this.progressBar.setMaximum(n);
		assert (this.progressBar.getMaximum() >= this.progressBar.getMinimum());
	}

	/**
	 * Reset the progress bar to its minimum value.
	 */
	public void resetProgressBar() {
		this.progressBar.setValue(this.progressBar.getMinimum());
	}

	/**
	 * Increment the progress bar by N.
	 */
	public void incrementProgressBar(int n) {
		if (this.progressBar.getValue() + n <= this.progressBar.getMaximum()) {
			this.progressBar.setValue(this.progressBar.getValue() + n);
		}
	}

	/**
	 * Set the progress bar to its maximum value, i.e. complete the progress
	 * bar.
	 */
	public void completeProgressBar() {
		this.progressBar.setValue(this.progressBar.getMaximum());
	}

	/**
	 * Return a new star creation listener.
	 * TODO: instead of this, we could just write the status at end of new 
	 * star task processing in model manager.
	 */
	private Listener<NewStarMessage> createNewStarListener() {
		return new Listener<NewStarMessage>() {
			// Update the status bar to say something about the fact that a
			// new star has just been loaded.

			public void update(NewStarMessage msg) {
				if (msg.getNewStarType() == NewStarType.NEW_STAR_FROM_SIMPLE_FILE
						|| msg.getNewStarType() == NewStarType.NEW_STAR_FROM_DOWNLOAD_FILE) {
					StringBuffer strBuf = new StringBuffer();
					strBuf.append("'");
					strBuf.append(msg.getNewStarName());
					strBuf.append("' loaded.");
					setMessage(strBuf.toString());
				} else if (msg.getNewStarType() == NewStarType.NEW_STAR_FROM_DATABASE) {
					StringBuffer strBuf = new StringBuffer();
					strBuf.append("'");
					strBuf.append(msg.getNewStarName());
					strBuf.append("' loaded from database.");
					setMessage(strBuf.toString());
				}
			}
		};
	}

	/**
	 * Return a progress listener.
	 */
	private Listener<ProgressInfo> createProgressListener() {
		final StatusPane self = this;
		return new Listener<ProgressInfo>() {
			public void update(ProgressInfo info) {
				switch (info.getType()) {
				case MIN_PROGRESS:
					self.setMinProgressValue(info.getNum());
					break;
				case MAX_PROGRESS:
					self.setMaxProgressValue(info.getNum());
					break;
				case RESET_PROGRESS:
					// Ensure the main window now has focus so we see
					// the progress bar and busy cursor as enabled.
					//self.requestFocusInWindow();
					self.resetProgressBar();
					self.setMessage("");
					break;
				case COMPLETE_PROGRESS:
					self.completeProgressBar();
					break;
				case INCREMENT_PROGRESS:
					// Ensure the main window now has focus so we see
					// the progress bar and busy cursor as enabled.
					// Except that it does not work here or above.
					//self.requestFocusInWindow();
					self.incrementProgressBar(info.getNum());
					break;
				}
			}
		};
	}
}