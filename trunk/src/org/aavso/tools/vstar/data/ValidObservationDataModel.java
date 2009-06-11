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
package org.aavso.tools.vstar.data;

import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * A table model for valid observations.
 */
public class ValidObservationDataModel extends AbstractTableModel {

	private final static int COLUMNS = 3;

	/**
	 * The list of valid observations retrieved.
	 */
	protected List<ValidObservation> validObservations;

	/**
	 * Constructor
	 * 
	 * @param validObservations
	 */
	public ValidObservationDataModel(List<ValidObservation> validObservations) {
		this.validObservations = validObservations;
	}

	/**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return COLUMNS;
	}

	/**
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return this.validObservations.size();
	}

	/**
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		assert columnIndex < COLUMNS;
		
		Object value = null;
		ValidObservation validOb = this.validObservations.get(rowIndex);
		switch(columnIndex) {
		case 0:
			value = validOb.getDateInfo().toString();
			break;
		case 1:
			value = validOb.getMagnitude().toString();
			break;
		case 2:
			value = validOb.getObsCode();
			break;
		}
		return value;
	}
}
