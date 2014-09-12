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
package org.wcs.smart.observation.common.importwp;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Wizard page to select the waypoints or track points to import.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportWpSelectWizardPage extends WizardPage implements IImportWizardPage { 
	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportWpSelectWizardPage_PageName;
	
	
	private CheckboxTableViewer tblWaypoint;
	private List<Waypoint> initialWaypoints = null;
	
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
				if (element instanceof Waypoint) {
					Waypoint wp = (Waypoint) element;
					StringBuilder value = new StringBuilder( );
					value.append(wp.getId());
					if (wp.getComment() != null && !wp.getComment().toLowerCase().equals("null")){ //$NON-NLS-1$
						value.append (" (" + wp.getComment() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					value.append(" ["); //$NON-NLS-1$
					if (wp.getDateTime() != null){
						value.append(" "); //$NON-NLS-1$
						String dateFormatted = DateFormat.getDateTimeInstance().format(wp.getDateTime());
						value.append(dateFormatted); 
					}
					value.append("]"); //$NON-NLS-1$
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
		tblWaypoint.setInput(initialWaypoints);

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
	
	@Override
    public IWizardPage getNextPage() {
		return null;
    }

	/*
	 * Sets initial waypoints
	 */
	private void setWaypointsFromObjects(List<Waypoint> allWaypoints) {
		this.initialWaypoints = allWaypoints;
		if (tblWaypoint != null){
			tblWaypoint.setInput(initialWaypoints);
			tblWaypoint.refresh();
		}
		if (initialWaypoints == null || initialWaypoints.size() == 0){
			String type = ((ImportGpsDataWizard)getWizard()).getType().guiName;
			setErrorMessage(MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new Object[]{type, type}));
			((ImportGpsDataWizard)getWizard()).setCanFinish(false);
		}else{
			setErrorMessage(null);
			((ImportGpsDataWizard)getWizard()).setCanFinish(true);
		}
		
	}
	
	/**
	 * 
	 * @return the list of selected waypoints 
	 */
	private List<Waypoint> getSelectedWaypointsObj(){
		List<Waypoint> wps = new ArrayList<Waypoint>();
		Object[] checked = tblWaypoint.getCheckedElements();
		for (int i = 0; i < checked.length; i ++){
			wps.add((Waypoint)checked[i]);
		}
		return wps;
	}

	@Override
	public boolean beforeMoveNext(WizardPage nextPage) {
		ImportGpsDataWizard wizard = ((ImportGpsDataWizard)getWizard());
		wizard.setImportedData(getSelectedWaypointsObj());
		return true;
	}

	@Override
	public boolean init() {
		final boolean[] error = new boolean[]{false};
		final ImportGpsDataWizard wizard = ((ImportGpsDataWizard)getWizard());
		
		try {
			getWizard().getContainer().run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException{
					monitor.beginTask(Messages.ImportWpSelectWizardPage_ProgressReadingWaypoints, 2);
					try{
						List<Waypoint> points = wizard.getImportEngine().getWaypoints(wizard.getImportOption(), wizard.getType(), null, monitor);
						monitor.worked(1);
						setWaypointsFromObjects(points);
					}catch(Exception ex){
						error[0] = true;
						ObservationPlugIn.displayLog(ex.getMessage(), ex);
					}finally{
						monitor.done();
					}
				
				}
			});
		} catch (Exception e) {
			ObservationPlugIn.displayLog(e.getMessage(), e);
			return false;
		}
		return !error[0];
	}
}