package org.wcs.smart.connect.ui.startup;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.hibernate.HibernateManager;

public class UsernamePasswordWizardPage extends WizardPage {

	private Text txtUsername;
	private Text txtPassword;
	
	public UsernamePasswordWizardPage(){
		super("USERPASS");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		Composite inner = new Composite(outer, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(inner, SWT.NONE);
		l.setText("User Name:");
		txtUsername = new Text(inner, SWT.BORDER);
		txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
		l = new Label(inner, SWT.NONE);
		l.setText("Password:");
		txtPassword = new Text(inner, SWT.PASSWORD | SWT.BORDER);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		setTitle("User name and password");
		setMessage("Enter your smart desktop user name and password.");
		setControl(outer);
	}
		
	public String getUsername(){
		return txtUsername.getText();
	}
	public String getPassword(){
		return txtPassword.getText();
	}
	
}
