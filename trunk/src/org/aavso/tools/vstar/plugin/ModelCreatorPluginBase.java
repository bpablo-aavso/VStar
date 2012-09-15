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
package org.aavso.tools.vstar.plugin;

import java.util.List;

import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.util.model.IModel;

/**
 * <p>
 * This is the abstract base class for all model creator plugin classes.
 * </p>
 * 
 * <p>
 * A model creator plugin will appear in VStar's Analysis menu when its jar file
 * is placed into the vstar_plugins directory.
 * </p>
 * 
 * @see org.aavso.tools.vstar.plugin.IPlugin
 */
abstract public class ModelCreatorPluginBase implements IPlugin {

	protected Mediator mediator = Mediator.getInstance();

	/**
	 * Returns the model object for this plugin whose execute() method can be
	 * invoked to create the model artifacts.
	 * 
	 * @param obs The list of observations to create a model of.
	 * @return The model object or null if no model was created.
	 */
	abstract public IModel getModel(List<ValidObservation> obs);

	/**
	 * @see org.aavso.tools.vstar.plugin.IPlugin#getDescription()
	 */
	@Override
	abstract public String getDescription();

	/**
	 * @see org.aavso.tools.vstar.plugin.IPlugin#getDisplayName()
	 */
	@Override
	abstract public String getDisplayName();

	/**
	 * @see org.aavso.tools.vstar.plugin.IPlugin#getGroup()
	 */
	@Override
	public String getGroup() {
		return "Model Creator";
	}
}
