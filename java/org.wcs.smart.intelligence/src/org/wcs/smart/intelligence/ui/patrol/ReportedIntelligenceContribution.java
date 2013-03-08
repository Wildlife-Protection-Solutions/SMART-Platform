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

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
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
import org.wcs.smart.intelligence.model.IntelligenceSourceType;
import org.wcs.smart.intelligence.ui.IntelligencePerspective;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditor;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;
import org.wcs.smart.intelligence.ui.wizard.NewIntelligenceWizard;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;

/**
 * Reported Intelligence contribution item for the patrol editor.
 * Lists all the intelligence that was reported by this patrol.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ReportedIntelligenceContribution implements IPatrolEditorContribution {

	private Patrol patrol;

	private Composite main;
	
	private Label label;

	private TableViewer tableViewer;
	private Button btnOpen;

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
	
	public ReportedIntelligenceContribution() {}

	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit) {
		main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(2, false));
		
		label = toolkit.createLabel(main, ""); //$NON-NLS-1$
		
		Hyperlink btnCreate = toolkit.createHyperlink(main, Messages.ReportedIntelligenceContribution_Create_Button, SWT.NONE);
		btnCreate.setToolTipText(Messages.ReportedIntelligenceContribution_Create_Button_Tooltip);
		btnCreate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnCreate.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				createIntelligence();
			}
		});
		
		Table reportedTable = toolkit.createTable(main, SWT.V_SCROLL | SWT.H_SCROLL);
		tableViewer = new TableViewer(reportedTable);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new IntelligenceLabelProvider());
		reportedTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnOpen.setEnabled(!tableViewer.getSelection().isEmpty());
			}
		});
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openCurrentItem();
			}
		});

		btnOpen = toolkit.createButton(main, Messages.ReportedIntelligenceContribution_Open_Button, SWT.PUSH);
		btnOpen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openCurrentItem();
			}
		});
		btnOpen.setEnabled(false);

		//listeners
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_ADDED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
		main.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_ADDED, intelligenceListener);
				IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
				IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
			}
		});
		
		return main;
	}

	@Override
	public String getName() {
		return Messages.ReportedIntelligenceContribution_Name;
	}

	@Override
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
		updateControls();
	}

	protected void updateControls() {
		List<Intelligence> data = IntelligenceHibernateManager.getReportedIntelligences(patrol);
		if (data.isEmpty()) {
			label.setText(Messages.ReportedIntelligenceContribution_NothingReported_Label);
			tableViewer.getControl().setVisible(false);
			btnOpen.setVisible(false);
			((GridData)tableViewer.getControl().getLayoutData()).heightHint = 0;
			((GridData)tableViewer.getControl().getLayoutData()).grabExcessVerticalSpace = false;
		} else {
			label.setText(Messages.ReportedIntelligenceContribution_IntelligenceReported_Label);
			tableViewer.getControl().setVisible(true);
			tableViewer.setInput(data.toArray());
			btnOpen.setVisible(true);
			((GridData)tableViewer.getControl().getLayoutData()).heightHint = 75;
			((GridData)tableViewer.getControl().getLayoutData()).grabExcessVerticalSpace = true;
		}
		main.getParent().getParent().layout(true, true);
	}

	private void createIntelligence() {
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			wb.showPerspective(IntelligencePerspective.ID, win);
			NewIntelligenceWizard wizard = new NewIntelligenceWizard();
			wizard.getIntelligence().setSource(IntelligenceSourceType.PATROL);
			wizard.getIntelligence().setPatrol(patrol);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		} catch (Throwable t) {
			IntelligencePlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}
	
	private void openCurrentItem() {
		IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
		Intelligence intelligence = (Intelligence) sel.getFirstElement();
		openEditor(intelligence);
	}
	
	private void openEditor(Intelligence intelligence) {
		Assert.isNotNull(intelligence);
		IntelligenceEditorInput input = new IntelligenceEditorInput(intelligence.getUuid(), intelligence.getName(), intelligence.getReceivedDate());
		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			wb.showPerspective(IntelligencePerspective.ID, win);
			IWorkbenchPage page = win.getActivePage();
			page.openEditor(input, IntelligenceEditor.ID);						
		} catch (Throwable t) {
			IntelligencePlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}
	
}
