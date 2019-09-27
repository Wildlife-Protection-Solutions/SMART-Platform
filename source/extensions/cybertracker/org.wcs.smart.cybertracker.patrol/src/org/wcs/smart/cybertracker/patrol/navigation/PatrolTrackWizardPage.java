/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.navigation;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.TrackPart;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Wizard page for selecting sampling units from a survey design.
 * 
 * @author Emily
 *
 */
public class PatrolTrackWizardPage extends WizardPage {

	public static final String PAGE_NAME ="PatrolTrack"; //$NON-NLS-1$
	
	private static final String SELECTPATROL = Messages.PatrolTrackWizardPage_SelectPatrol;
	
	private CheckboxTableViewer tblViewer;
	private ComboViewer cmbViewer;
	
	
	private AtomicBoolean isLoaded = new AtomicBoolean(false);
	
	private List<TrackPart> selectedItems = new ArrayList<>();
	
	protected PatrolTrackWizardPage() {
		super(PAGE_NAME);
	}

	@Override
	public void setVisible(boolean visible) {
		if (!isLoaded.getAndSet(true)) {
			loadValues.schedule();
		}
		super.setVisible(visible);
	}
	
	public List<TrackPart> getTracks(){
		return selectedItems;
	}
	
	@Override
	public boolean isPageComplete() {
		if (!selectedItems.isEmpty()) return true;
		return false;
	}
	
	@Override
	public IWizardPage getNextPage() {
		return null;
	}

	@Override
	public IWizardPage getPreviousPage() {
		return getWizard().getPages()[0];
	}
	
	@Override
	public void createControl(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main,SWT.NONE);
		l.setText(Messages.PatrolTrackWizardPage_PatrolLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		cmbViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Patrol) return ((Patrol)element).getId();
				return super.getText(element);
			}
		});
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cmbViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		l = new Label(main,SWT.NONE);
		l.setText(Messages.PatrolTrackWizardPage_TracksLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayout(new TableColumnLayout());
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblViewer = CheckboxTableViewer.newCheckList(wrapper, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblViewer));
		
		TableViewerColumn tc = new TableViewerColumn(tblViewer, SWT.CHECK);
		((TableColumnLayout)wrapper.getLayout()).setColumnData(tc.getColumn(), new ColumnWeightData(100));
		tc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof TrackPart) {
					TrackPart pt = (TrackPart)element;
					StringBuilder sb = new StringBuilder();
					sb.append(  DateFormat.getDateInstance().format(pt.getTrack().getPatrolLegDay().getDate()));
					try {
						if (pt.getTrack().getTrackParts().size() > 1)
							sb.append( MessageFormat.format(" ({0}) ", + pt.getIndex())); //$NON-NLS-1$
					}catch (Exception ex) {
						CyberTrackerPlugIn.log(ex.getMessage(), ex);
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		tblViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				selectedItems.clear();
				for (Object o : tblViewer.getCheckedElements()) {
					if (o instanceof TrackPart) selectedItems.add((TrackPart)o);
				}
				getWizard().getContainer().updateButtons();
			}
		});
		cmbViewer.addSelectionChangedListener(e->{
			selectedItems.clear();
			getWizard().getContainer().updateButtons();
			loadTracks.schedule();
		});
		setControl(main);
		
		setTitle(Messages.PatrolTrackWizardPage_Title);
		setMessage(Messages.PatrolTrackWizardPage_Message);
	}

	private Job loadTracks = new Job("loading tracks") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Object[] selection = new Object[] {null};
			Display.getDefault().syncExec(()->{
				tblViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
				selection[0] = cmbViewer.getStructuredSelection().getFirstElement();
			});
			
			
			List<TrackPart> items = new ArrayList<>();
			
			if (selection[0] != null && selection[0] instanceof Patrol) {
				Patrol p = (Patrol)selection[0];
				
				try(Session session = HibernateManager.openSession()){
					p = session.get(Patrol.class, p.getUuid());
					for (PatrolLeg pl : p.getLegs()) {
						for (PatrolLegDay pld : pl.getPatrolLegDays()) {
							for (Track t : pld.getTracks()) {
								try {
									for (TrackPart tp : t.getTrackParts()) {
										items.add(tp);
									}
								}catch (Exception ex) {
									CyberTrackerPlugIn.log(ex.getMessage(), ex);
								}
							}
						}
					}
				}
			}
			Display.getDefault().syncExec(()->{
				selectedItems.clear();
				tblViewer.setInput(items);
				tblViewer.setAllChecked(true);
				selectedItems.addAll(items);
				getWizard().getContainer().updateButtons();
			});
			return Status.OK_STATUS;
		}
		
	};
	private Job loadValues = new Job("loading patrols") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Patrol> items;
			try(Session session = HibernateManager.openSession()){
				items = QueryFactory.buildQuery(session, Patrol.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				items.sort((a,b)->-1*a.getStartDate().compareTo(b.getStartDate()));
			}
			Display.getDefault().syncExec(()->{
				List<Object> all = new ArrayList<>();
				all.add(SELECTPATROL);
				all.addAll(items);
				cmbViewer.setInput(all);
				cmbViewer.refresh();
				cmbViewer.setSelection(new StructuredSelection(SELECTPATROL));
			});
			return Status.OK_STATUS;
		}
		
	};
}
