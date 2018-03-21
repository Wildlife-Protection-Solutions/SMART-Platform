package org.wcs.smart.event.i2;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.ui.model.IActionParameterCollector;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.ui.properties.DialogConstants;

public class CreateRecordParameterCollector implements IActionParameterCollector {

	private Text txtTitle;
	private ComboViewer cmbSource;
	
	private String defaultKey = null;
	private List<IntelRecordSource> sources = null;
	
	public CreateRecordParameterCollector() {
	}

	@Override
	public void initParameters(EAction action) {

		EActionParameterValue sourceParam = action.findParameter(SourceParameter.INSTANCE.getKey());
		if (sourceParam != null) {
			if(sources != null) {
				for (IntelRecordSource r : sources) {
					if (r.getKeyId().equals(sourceParam.getParameterValue())) {
						cmbSource.setSelection(new StructuredSelection(r));
						break;
					}
				}
			}else {
				defaultKey = sourceParam.getParameterValue();
			}
		}
		
		EActionParameterValue titleParam = action.findParameter(TitleParameter.INSTANCE.getKey());
		if (titleParam != null) {
			txtTitle.setText(sourceParam.getParameterValue());
		}
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
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Record Source:");
		
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
		l = new Label(main, SWT.NONE);
		l.setText("Record Title:");
		
		txtTitle = new Text(main, SWT.BORDER);
		txtTitle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		loadSources.schedule();
		return main;
	}

	@Override
	public void addModifyListener(Listener listener) {
	
	}

	private Job loadSources = new Job("loading record sources") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> srcs = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				srcs.addAll(QueryFactory.buildQuery(session, IntelRecordSource.class, "conservationArea", SmartDB.getCurrentConservationArea()).list());
			}
			
			srcs.add(0, "");
			Display.getDefault().syncExec(()->{
				if (cmbSource.getControl().isDisposed()) return;
				cmbSource.setInput(srcs);
				if (defaultKey != null) {
					for (IntelRecordSource r : sources) {
						if (r.getKeyId().equals(defaultKey)) {
							cmbSource.setSelection(new StructuredSelection(r));
							break;
						}
					}
				}
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
