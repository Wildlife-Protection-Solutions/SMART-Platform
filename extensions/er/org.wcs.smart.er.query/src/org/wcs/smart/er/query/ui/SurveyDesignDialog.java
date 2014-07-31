package org.wcs.smart.er.query.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.er.hibernate.FieldSurveyHibernateManager;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;

public class SurveyDesignDialog extends TitleAreaDialog{

	private Session session;
	
	private SurveyDesign sd;
	private ComboViewer cmbViewer;
	
	public SurveyDesignDialog(Shell parentShell) {
		super(parentShell);
	}
	
	
	@Override
	public boolean close(){
		session.close();
		return super.close();
	}
	
	@Override
	public void okPressed(){
		Object selection =  ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
		if (selection instanceof SurveyDesign){
			sd = (SurveyDesign)selection;
		}else{
			sd = null;
		}
		super.okPressed();
	}
	
	public SurveyDesign getSelectedDesign(){
		return this.sd;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		setTitle("Survey Design");
		getShell().setText("Survey Design");
		setMessage("Select the survey design you wish to query.");
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Survey Design:");
	
		session = HibernateManager.openSession();
		
		cmbViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		List<SurveyDesign> sds = FieldSurveyHibernateManager.getInstance().getActiveSurveys(session);
		List<Object> all = new ArrayList<Object>();
		all.addAll(sds);
		all.add("< All Survey Designs >");
		cmbViewer.setInput(all);
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (sds.size() > 0){
			cmbViewer.setSelection(new StructuredSelection(sds.get(0)));
		}
	
		return composite;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
