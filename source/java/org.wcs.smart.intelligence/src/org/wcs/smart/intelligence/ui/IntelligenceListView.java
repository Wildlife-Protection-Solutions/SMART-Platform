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
package org.wcs.smart.intelligence.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditor;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;

/**
 * A viewer where users can view all intelligence items.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceListView extends ViewPart {

	public static final String ID = "org.wcs.smart.intelligence.IntelligenceListView"; //$NON-NLS-1$

	private TableViewer intelligenceListViewer;
	private Job updateJob = new UpdateIntelligenceListIdJob();
	/**
	 * Default constructor
	 */
	public IntelligenceListView() {}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		
		intelligenceListViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		Table list = intelligenceListViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		intelligenceListViewer.setLabelProvider(new IntelligenceLabelProvider());
		intelligenceListViewer.setContentProvider(ArrayContentProvider.getInstance());
		intelligenceListViewer.setInput(new Object[]{"Loading..."});
		intelligenceListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();

		//TODO: handle new element added
//		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, patrolListener);
//		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolListener);
//		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolListener);
		
		intelligenceListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Intelligence intelligence = (Intelligence)((IStructuredSelection)intelligenceListViewer.getSelection()).getFirstElement();
				if (intelligence != null){
					IWorkbenchPage page = null;
					try {
						page = getSite().getPage();
						//TODO: open editor
						IntelligenceEditorInput input = new IntelligenceEditorInput(intelligence.getUuid(), intelligence.getShortName());
						page.openEditor(input, IntelligenceEditor.ID);						
					} catch (Throwable t) {
						IntelligencePlugIn.displayLog(t.getLocalizedMessage(), t);
					}
				}
				
			}
		});
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(intelligenceListViewer.getControl());
		intelligenceListViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager,  intelligenceListViewer);
		getSite().setSelectionProvider(intelligenceListViewer);
	}

	/**
	 * Refreshes the intelligence list
	 */
	public void updateContent(){
		updateJob.cancel();
		updateJob.schedule();		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		intelligenceListViewer.getControl().setFocus();

	}

    /**
     * Label provider for intelligence objects.
     * 
     * @author elitvin
     *
     */
	private class IntelligenceLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Intelligence){
				return ((Intelligence)element).getShortName();
			}
			return super.getText(element);
		}
		
	}

    /**
     * Job is used to fill list viewer with data
     * 
     * @author elitvin
     *
     */
    private class UpdateIntelligenceListIdJob extends Job {

    	public UpdateIntelligenceListIdJob() {
			super("Update Intelligence List");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Loading Intelligences", 1);
			final List<Intelligence> result = IntelligenceHibernateManager.getIntelligences();
			monitor.internalWorked(0.8);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					intelligenceListViewer.setInput(result.toArray());
					intelligenceListViewer.refresh();
				}
			});
			return Status.OK_STATUS;
		}
   	
    }
 	
}
