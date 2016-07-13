/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.cybertracker.export.alert;

import org.hibernate.Session;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Interface that must be implemented if a plugin wants to provide alert configuration
 * of data target configuration for CyberTracker.
 * 
 * Each CT export will create a single new instance of this class.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public interface ICtConfigurationExtension {
	
	/**
	 * Gets the alert provider for the given configurable model. May return null
	 * if not applicable
	 * 
	 * @param model
	 * @return
	 */
	public IAlertProvider getAlertProvider(ConfigurableModel model, Session session);

	/**
	 * Gets the target provider for the given configurable model.  May return null
	 * if not applicable.
	 * 
	 * @param model
	 * @return
	 * @throws Exception
	 */
	public IDataTargetProvider getDataTargetProvider(ConfigurableModel model, Session session) throws Exception;
}
