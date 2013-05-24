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
package org.wcs.smart.patrol.internal.ui.importwp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.patrol.gpx.WptType;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Wizard page to select the waypoints or track points to import.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportWpSelectWizardPage extends WizardPage { 
	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportWpSelectWizardPage_PageName;
	
	
	private CheckboxTableViewer tblWaypoint;
	/**
	 * @param pageName
	 */
	protected ImportWpSelectWizardPage( ImportGpsDataWizard wizard ) {
		super(PAGE_NAME);
		wizard.addPage(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		tblWaypoint = CheckboxTableViewer.newCheckList(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		tblWaypoint.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof WptType) {
					WptType wp = (WptType) element;
					StringBuilder value = new StringBuilder(wp.getName());
					
					if (wp.getCmt() != null && !wp.getCmt().toLowerCase().equals("null")){ //$NON-NLS-1$
						value.append (" (" + wp.getCmt() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					if (wp.getTime() != null){
						value.append(" [ " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM).format(wp.getTime().toGregorianCalendar().getTime()) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return value.toString();
				}
				return super.getText(element);
			}
		});

		tblWaypoint.getTable().addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == ' '){
					boolean value = tblWaypoint.getChecked(   ((IStructuredSelection)tblWaypoint.getSelection()).getFirstElement() );
					for (Iterator<?> iterator = ((IStructuredSelection)tblWaypoint.getSelection()).iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						tblWaypoint.setChecked(tp, !value);
						
					}
					e.doit = false;
					
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		
		
		tblWaypoint.setContentProvider(ArrayContentProvider.getInstance());
		tblWaypoint.setInput(   ((ImportGpsDataWizard)getWizard()).getWaypoints().toArray() );
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = Math.min(tblWaypoint.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y, 400);
		tblWaypoint.getTable().setLayoutData(gd);

		Composite links = new Composite(comp, SWT.NONE);
		links.setLayout(new GridLayout(2, false));
		links.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Link lnkSelectAll = new Link(links, SWT.NONE);
		lnkSelectAll.setText("<a>" + Messages.ImportWpSelectWizardPage_SelectAll + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkSelectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				tblWaypoint.setAllChecked(true);
			}
		});
		
		lnkSelectAll = new Link(links, SWT.NONE);
		lnkSelectAll.setText("<a>" + Messages.ImportWpSelectWizardPage_DeSelectAll + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkSelectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				tblWaypoint.setAllChecked(false);
			}
		});
		
		((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		super.setTitle(Messages.ImportWpSelectWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(((ImportGpsDataWizard)getWizard()).getType().importDesc );
		super.setControl(comp);
	}
	
	/**
	 * 
	 * @return the list of selected waypoints 
	 */
	public List<WptType> getSelectedWaypoints(){
		List<WptType> wps = new ArrayList<WptType>();
		Object[] checked = tblWaypoint.getCheckedElements();
		for (int i = 0; i < checked.length; i ++){
			wps.add((WptType)checked[i]);
		}
		return wps;
	}

	/**
	 * Sets the list of waypoints to choose from 
	 * @param waypoints
	 */
	public void setWaypoints(List<WptType> waypoints){
		if (tblWaypoint != null){
			tblWaypoint.setInput(   waypoints.toArray() );
		}
		((ImportGpsDataWizard)getWizard()).setCanFinish(true);
	}
	
	@Override
    public IWizardPage getNextPage() {
		return null;
    }
}