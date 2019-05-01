/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.i2;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.ui.model.IActionParameterCollector;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Parameter collector for create record action type parameters
 * 
 * @author Emily
 *
 */
public class CreateRecordParameterCollector implements IActionParameterCollector {

	private Text txtTitle;
	private ComboViewer cmbSource;
	
	private String initSourceKey = null;
	private List<Listener> modifyListeners;
	
	public CreateRecordParameterCollector() {
		modifyListeners = new ArrayList<>();
	}

	@Override
	public void initParameters(EAction action) {

		EActionParameterValue sourceParam = action.findParameter(SourceParameter.INSTANCE.getKey());
		if (sourceParam != null) {
			initSourceKey = sourceParam.getParameterValue();
			initSourceCombo(sourceParam.getParameterValue());
			
		}
		
		EActionParameterValue titleParam = action.findParameter(TitleParameter.INSTANCE.getKey());
		if (titleParam != null) {
			txtTitle.setText(titleParam.getParameterValue());
		}
		
		
		txtTitle.addListener(SWT.Modify, evt->fireListeners());
	}

	
	@Override
	public void updateParameters(EAction action) {
		EActionParameterValue sourceParam = action.findParameter(SourceParameter.INSTANCE.getKey());
		Object x = cmbSource.getStructuredSelection().getFirstElement();
		if (x instanceof IntelRecordSource) {
			if (sourceParam == null) {
				sourceParam = new EActionParameterValue();
				sourceParam.getId().setAction(action);
				sourceParam.getId().setParameterKey(SourceParameter.INSTANCE.getKey());
				action.getParameters().add(sourceParam);
			}
			sourceParam.setParameterValue(((IntelRecordSource)x).getKeyId());
		}else {
			if (sourceParam != null) {
				action.getParameters().remove(sourceParam);
			}
		}
		
		EActionParameterValue titleParam = action.findParameter(TitleParameter.INSTANCE.getKey());
		if (txtTitle.getText().trim().isEmpty()) {
			if (titleParam != null) {
				action.getParameters().remove(titleParam);
			}
		}else {
			if (titleParam == null) {
				titleParam = new EActionParameterValue();
				titleParam.getId().setAction(action);
				titleParam.getId().setParameterKey(TitleParameter.INSTANCE.getKey());
				action.getParameters().add(titleParam);
			}
			titleParam.setParameterValue(txtTitle.getText().trim());
		}
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.CreateRecordParameterCollector_SourceLabel);
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbSource = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSource.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbSource.setContentProvider(ArrayContentProvider.getInstance());
		cmbSource.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelRecordSource) {
					return ((IntelRecordSource) element).getName();
				}
				return super.getText(element);
			}
		});
		cmbSource.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbSource.getControl().setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.CreateRecordParameterCollector_TitleLabel);
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		txtTitle = new Text(main, SWT.BORDER);
		txtTitle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtTitle.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		loadSources.schedule();
		return main;
	}

	@Override
	public void addModifyListener(Listener listener) {
		modifyListeners.add(listener);
	}
	
	private void fireListeners() {
		modifyListeners.forEach(e->e.handleEvent(new Event()));
	}

	private void initSourceCombo(String key) {
		if (key == null) return;
		
		Object x = cmbSource.getInput();
		if (!(x instanceof List)) return;
		
		try {
			List<?> items = (List<?>)x;
			for (Object item : items) {
				if ((item instanceof IntelRecordSource) && (((IntelRecordSource)item).getKeyId()).equals(key)){
					cmbSource.setSelection(new StructuredSelection(item));
					return;
				}
			}
		}finally {
			cmbSource.addSelectionChangedListener(evt->fireListeners());
		}
	}
	private Job loadSources = new Job(Messages.CreateRecordParameterCollector_JobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> srcs = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				srcs.addAll(QueryFactory.buildQuery(session, IntelRecordSource.class, 
						"conservationArea", SmartDB.getCurrentConservationArea()).list()); //$NON-NLS-1$
				
			}
			srcs.sort((a,b)-> Collator.getInstance().compare(((IntelRecordSource)a).getName(), ((IntelRecordSource)b).getName()));
			srcs.add(0, ""); //$NON-NLS-1$
			
			Display.getDefault().syncExec(()->{
				if (cmbSource.getControl().isDisposed()) return;
				cmbSource.setInput(srcs);
				initSourceCombo(initSourceKey);
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
