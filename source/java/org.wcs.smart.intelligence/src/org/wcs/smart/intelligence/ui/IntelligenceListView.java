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
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceEventManager.EventType;
import org.wcs.smart.intelligence.IntelligenceEventManager.IIntelligenceEventListener;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
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

//	private IPartListener2 partListener = new IntelligencePartListener();

	/**
	 * listener for intelligence change events.
	 */
	private IIntelligenceEventListener intelligenceListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			updateContent();
		}
	};
	
	/**
	 * Default constructor
	 */
	public IntelligenceListView() {
//		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
	}

	public void dispose() {		
//		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_ADDED, intelligenceListener);
//		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
		super.dispose();
	}
	
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
		intelligenceListViewer.setInput(new Object[]{Messages.IntelligenceListView_Loading_Label});
		intelligenceListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();

		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_ADDED, intelligenceListener);
//		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
		
		intelligenceListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Intelligence intelligence = (Intelligence)((IStructuredSelection)intelligenceListViewer.getSelection()).getFirstElement();
				if (intelligence != null){
					IWorkbenchPage page = null;
					try {
						page = getSite().getPage();
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
			super(Messages.IntelligenceListView_UpdateJob_Title);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.IntelligenceListView_UpdateJob_LoadTask_Name, 1);
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
 	
//    private class IntelligencePartListener implements IPartListener2 {
//		@Override
//		public void partVisible(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partOpened(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partInputChanged(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partHidden(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partDeactivated(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partClosed(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partBroughtToTop(IWorkbenchPartReference partRef) {}
//		
//		@Override
//		public void partActivated(IWorkbenchPartReference partRef) {
//			if (partRef.getId().equals(IntelligenceEditor.ID)){
//				IWorkbenchPart part = partRef.getPart(false);
//				if (part instanceof IntelligenceEditor){
//					intelligenceListViewer.setSelection(new StructuredSelection( ((IntelligenceEditor) part).getEditorInput()));
//				}
//			}
//		}
//    }
    
}
