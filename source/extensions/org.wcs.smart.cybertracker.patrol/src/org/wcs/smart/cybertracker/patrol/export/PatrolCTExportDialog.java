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
package org.wcs.smart.cybertracker.patrol.export;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerExportDialog;
import org.wcs.smart.cybertracker.export.IConfigurableModelProvider;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Dialog for exporting Survey Designs to CyberTracker application.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class PatrolCTExportDialog extends CyberTrackerExportDialog {

	private CyberTrackerConfExporter exporter = new PatrolCTExporter();
	
	private Object selectedModel;
    private ComboViewer modelViewer;

    public PatrolCTExportDialog(Shell parentShell) {
		super(parentShell);
	}

    @Override
    protected IConfigurableModelProvider getConfigurableModelProvider() {
    	final Object src = selectedModel;
    	return new IConfigurableModelProvider() {
    		@Override
    		public ConfigurableModel getConfigurableModel(Session session, IProgressMonitor monitor) {
    			if (src instanceof ConfigurableModel) {
    				monitor.subTask("Fetching configurable model...");
    				ConfigurableModel model = (ConfigurableModel) src;
    				return DataentryHibernateManager.getFullConfigurableModel(model.getUuid(), session);
    			} else if (src instanceof DataModelWrapper) {
    				return ((DataModelWrapper) src).buildConfigurableModel(session, monitor);
    			}
    			return null;
    		}

			@Override
			public Object getExportSource() {
				return src;
			}
    	};
    }

	@Override
	protected CyberTrackerConfExporter getExporter() {
		return exporter;
	}

	@Override
	protected void addModelSourceControl(Composite parent) {
		Label modelLabel = new Label(parent, SWT.NONE);
		modelLabel.setText(Messages.PatrolCTExportDialog_ConfigurableModel);
		modelViewer = new ComboViewer(parent, SWT.READ_ONLY);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)modelViewer.getControl().getLayoutData()).widthHint = 100;
		modelViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelViewer.setLabelProvider(new ConfigurableModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DataModelWrapper) {
					return Messages.DataModelWrapper_Dropdown_Label;
				}
				return super.getText(element);
			}
		});
		modelViewer.setInput(getModelsList().toArray());
		modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedModel = ((IStructuredSelection)modelViewer.getSelection()).getFirstElement();
				updateExportButtonState();
			}
		});
	}
	
	private List<?> getModelsList() {
		List<Object> modelList = new ArrayList<Object>();
		DataModel dataModel = null;
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			modelList.addAll(DataentryHibernateManager.getConfigurableModels(s));
			dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.PatrolCTExportDialog_LoadConfModels_Error, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
		
		if (dataModel != null) {
			modelList.add(new DataModelWrapper());
		}
		return modelList;
	}

	@Override
	protected boolean isValidExportSource() {
		return selectedModel != null;
	}

}
