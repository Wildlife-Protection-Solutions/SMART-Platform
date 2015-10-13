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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerExportDialog;
import org.wcs.smart.cybertracker.export.IConfigurableModelProvider;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog for exporting Survey Designs to CyberTracker application.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyCTExportDialog extends CyberTrackerExportDialog {

	private CyberTrackerConfExporter exporter = new SurveyCTExporter();

	private ComboViewer designsViewer;
	private Object selectedDesign;

    public SurveyCTExportDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected IConfigurableModelProvider getConfigurableModelProvider() {
		SurveyDesignEditorInput designInput = (SurveyDesignEditorInput) selectedDesign;
		return new SurveyConfigurableModelProvider(designInput);
	}

	@Override
	protected CyberTrackerConfExporter getExporter() {
		return exporter;
	}
	
	@Override
	protected void addModelSourceControl(Composite parent) {
		Label modelLabel = new Label(parent, SWT.NONE);
		modelLabel.setText(Messages.SurveyCTExportDialog_SurveyDesign);
		designsViewer = new ComboViewer(parent, SWT.READ_ONLY);
		designsViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)designsViewer.getControl().getLayoutData()).widthHint = 100;
		designsViewer.setContentProvider(ArrayContentProvider.getInstance());
		designsViewer.setLabelProvider(new SurveyDesignLabelProvider());
		designsViewer.setInput(getDesignsList().toArray());
		designsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedDesign = ((IStructuredSelection)designsViewer.getSelection()).getFirstElement();
				updateExportButtonState();
			}
		});
	}
	
	private List<?> getDesignsList() {
		List<Object> modelList = new ArrayList<Object>();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			;
			modelList.addAll(SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(s, null));
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.SurveyCTExportDialog_LoadSurveyDesigns_Error, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
		return modelList;
	}

	@Override
	protected boolean isValidExportSource() {
		return selectedDesign != null;
	}

}
