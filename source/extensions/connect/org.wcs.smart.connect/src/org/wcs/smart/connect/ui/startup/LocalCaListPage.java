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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;

/**
 * Lists conservation area from a smart connect server.
 * 
 * @author Emily
 *
 */
public class LocalCaListPage extends WizardPage implements ISelectionChangedListener{

	public static final String NAME = "CALIST"; //$NON-NLS-1$
	
	private CheckboxTableViewer cmbList;
	private Text txtUsername;
	private Text txtPassword;
	
	private String initUsername = null;
	private String initPassword = null;
	
	public LocalCaListPage(){
		super(NAME);
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		Composite inner = new Composite(outer, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)inner.getLayout()).marginWidth = 0;
		
		Label l = new Label(inner, SWT.NONE);
		l.setText(Messages.LocalCaListPage_UsernameLabel);
		txtUsername = new Text(inner, SWT.BORDER);
		txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.LocalCaListPage_PasswordLabel);
		txtPassword = new Text(inner, SWT.PASSWORD | SWT.BORDER);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(outer, SWT.NONE);
		l.setText(Messages.LocalCaListPage_CaLabel);
	
		cmbList = CheckboxTableViewer.newCheckList(outer, SWT.BORDER | SWT.MULTI);
		cmbList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)cmbList.getControl().getLayoutData()).heightHint = 200;
		((GridData)cmbList.getControl().getLayoutData()).widthHint = 100;
		cmbList.setContentProvider(ArrayContentProvider.getInstance());
		cmbList.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof ConservationArea){
					return ((ConservationArea) element).getNameLabel();
				}
				return super.getText(element);
			}
		});
		cmbList.setInput(new String[]{Messages.LocalCaListPage_Loading});
		cmbList.addSelectionChangedListener(this);
		cmbList.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(cmbList));

		Composite links = new Composite(outer, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		links.setLayout(gl);
		
		Link selectAll = new Link(links, SWT.NONE);
		selectAll.setText("<a>" + Messages.LocalCaListPage_selectall + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cmbList.setAllChecked(true);
			}
		});
		 
		Link selectNone = new Link(links, SWT.NONE);
		selectNone.setText("<a>" + Messages.LocalCaListPage_deselectall + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		selectNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cmbList.setAllChecked(false);
			}
		});
		initList();
		
		if (initUsername != null) this.txtUsername.setText(initUsername);
		if (initPassword != null) this.txtPassword.setText(initPassword);
		
		setTitle(Messages.LocalCaListPage_Title);
		setMessage(Messages.LocalCaListPage_Message);
		setControl(outer);
	}
	
	public void setUsernamePassword(String username, String password) {
		this.initPassword = password;
		this.initUsername = username;
	}
	
	public String getUsername() {
		return txtUsername.getText();
	}

	public String getPassword() {
		return txtPassword.getText();
	}

	
	public void initList(){
		final List<ConservationArea> ca = new ArrayList<ConservationArea>();
		
		try(Session s = HibernateManager.openSession()){
			List<ConnectServer> servers = QueryFactory.buildQuery(s, ConnectServer.class)
					.list();
			for (Iterator<ConnectServer> iterator = servers.iterator(); iterator.hasNext();) {
				ConnectServer server = (ConnectServer) iterator.next();
				ca.add(server.getConservationArea());
			}
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
			setErrorMessage(Messages.LocalCaListPage_CaLoadError + ex.getMessage());
			return;
		}
		
		if (SmartDB.getCurrentConservationArea() != null) {
			//we are logged in so we can validate replication enabled state fo ca
			for (Iterator<ConservationArea> iterator = ca.iterator(); iterator.hasNext();) {
				ConservationArea conservationArea = (ConservationArea) iterator.next();
				try(Session session = HibernateManager.openSession()){
					if (!DerbyReplicationManager.INSTANCE.isReplicationEnabled(conservationArea.getUuid(), session)) {
						iterator.remove();
					}
				}
			}
		}
		
		Collections.sort(ca, new Comparator<ConservationArea>() {
			@Override
			public int compare(ConservationArea o1, ConservationArea o2) {
				return Collator.getInstance().compare(o1.getNameLabel(), o2.getNameLabel());
			}
		});
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				if (ca.isEmpty()){
					setErrorMessage(Messages.LocalCaListPage_NoCaFound);
				}
				cmbList.setInput(ca);
				cmbList.refresh();
				cmbList.setAllChecked(true);
			}
		});
	}
	
	public void clearList(){
		cmbList.setInput(new String[]{Messages.LocalCaListPage_Loading});
	}
	
	public boolean isPageComplete(){
		if (!super.isPageComplete()){
			return false;
		}
		return !getSelection().isEmpty();
	}
	
	public List<ConservationArea> getSelection(){
		List<ConservationArea> selected = new ArrayList<ConservationArea>();
		Object[] items = cmbList.getCheckedElements();
		for (Object item : items){
			if (item instanceof ConservationArea){
				selected.add((ConservationArea)item);
			}
		}
		return selected;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (getContainer() != null) getContainer().updateButtons();
	}

}
