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
package org.wcs.smart.intelligence.ui.patrol;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceEventManager.EventType;
import org.wcs.smart.intelligence.IntelligenceEventManager.IIntelligenceEventListener;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.IntelligencePerspective;
import org.wcs.smart.intelligence.ui.handlers.OpenIntelligenceHandler;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Motivation Intelligence contribution item for the patrol editor.
 * Lists all the intelligence that was motivated this patrol.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class MotivationIntelligenceContribution implements IPatrolEditorContribution {

	private Patrol patrol;

	private Composite main;
	private Label label;
	private TableViewer tableViewer;

	private List<Intelligence> intelligenceList;
	
	private Job updateJob = new Job("Update Motivation Intelligence "){ //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			intelligenceList = IntelligenceHibernateManager.getMotivatedIntelligences(patrol);
			Collections.sort(intelligenceList, new IntelligenceNameComparator());
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (tableViewer.getControl().isDisposed()){
						return;
					}
					if (intelligenceList.isEmpty()) {
						label.setText(Messages.MotivationIntelligenceContribution_NotMotivated_Label);
						tableViewer.getControl().setVisible(false);
						((GridData)tableViewer.getControl().getLayoutData()).heightHint = 0;
						((GridData)tableViewer.getControl().getLayoutData()).grabExcessVerticalSpace = false;
					} else {
						label.setText(Messages.MotivationIntelligenceContribution_Motivated_Label);
						tableViewer.getControl().setVisible(true);
						tableViewer.setInput(intelligenceList.toArray());
						((GridData)tableViewer.getControl().getLayoutData()).heightHint = 75;
						((GridData)tableViewer.getControl().getLayoutData()).grabExcessVerticalSpace = true;
					}
					main.getParent().getParent().layout(true,true);
				}});
			return Status.OK_STATUS;
		}};

	/**
	 * listener for intelligence change events.
	 */
	private IIntelligenceEventListener intelligenceListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateControls();
				}
			});
		}
	};
	
	public MotivationIntelligenceContribution() {}

	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit) {
		main = toolkit.createComposite(parent, SWT.NONE);
		main.setLayout(new GridLayout((canEdit?2:1), false));
		
		label = toolkit.createLabel(main, ""); //$NON-NLS-1$
		if (canEdit){
			Hyperlink lnk = createEditLink(toolkit, main);
			lnk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,1,2));
		}
		
		Table reportedTable = toolkit.createTable(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		tableViewer = new TableViewer(reportedTable);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new IntelligenceLabelProvider());
		reportedTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openCurrentItem();
			}
		});
		tableViewer.setInput(new Object[]{Messages.MotivationIntelligenceContribution_LoadingText});

		//listeners
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
		main.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
				IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
			}
		});
		
		return main;
	}

	@Override
	public String getName() {
		return Messages.MotivationIntelligenceContribution_Name;
	}

	@Override
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
		updateControls();
	}

	protected void updateControls() {
		updateJob.setSystem(true);
		updateJob.schedule();
	}
	
	/**
	 * Creates an edit hyperlink button
	 * @param toolkit toolkit
	 * @param parent parent composite
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit toolkit, Composite parent) {
		Hyperlink editLink = toolkit.createHyperlink(parent, DialogConstants.EDIT_LINK_TEXT, SWT.WRAP);
		editLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog();
			}
		});
		return editLink;
	}


	/**
	 * Displays and edit dialog 
	 */
	private void showEditDialog() {
		final EditPatrolMotivationDialog editDialog = new EditPatrolMotivationDialog(Display.getDefault().getActiveShell(), patrol, intelligenceList);
		editDialog.open();
		if (editDialog.isSavePerformed()) {
			updateControls();
		}
	}
	
	private void openCurrentItem() {
		IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
		Intelligence intelligence = (Intelligence) sel.getFirstElement();
		openEditor(intelligence);
	}
	
	private void openEditor(Intelligence intelligence) {
		Assert.isNotNull(intelligence);
		try {
			PlatformUI.getWorkbench().showPerspective(IntelligencePerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		} catch (Throwable t) {
			IntelligencePlugIn.displayLog(t.getLocalizedMessage(), t);
		}
		(new OpenIntelligenceHandler()).openIntelligence(intelligence.getUuid());

	}
	
}
