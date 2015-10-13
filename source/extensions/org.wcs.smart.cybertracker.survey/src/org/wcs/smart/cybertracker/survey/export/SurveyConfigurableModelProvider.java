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
package org.wcs.smart.cybertracker.survey.export;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.export.IConfigurableModelProvider;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyConfigurableModelProvider implements IConfigurableModelProvider {
	
	private SurveyDesignEditorInput input;

	private DataModelWrapper dmWrapper;

	
	public SurveyConfigurableModelProvider(SurveyDesignEditorInput input) {
		if (input == null) {
			throw new IllegalArgumentException("Survey design cannot be null"); //$NON-NLS-1$
		}
		this.input = input;
	}

	@Override
	public ConfigurableModel getConfigurableModel(Session session, IProgressMonitor monitor) {
		SurveyDesign sd = SurveyHibernateManager.getInstance().getSurveyDesign(input.getSurveyDesignKey(), session);
		if (sd != null) {
			if (sd.getConfigurableModel() != null) {
				monitor.subTask(Messages.SurveyConfigurableModelProvider_Task_FetchConfigurableModel);
				return DataentryHibernateManager.getFullConfigurableModel(sd.getConfigurableModel().getUuid(), session);
			} else {
				if (dmWrapper == null) {
					dmWrapper = new DataModelWrapper();
				}
				return dmWrapper.buildConfigurableModel(session, monitor);
			}
		}
		return null;
	}

	@Override
	public Object getExportSource() {
		return input;
	}

}
