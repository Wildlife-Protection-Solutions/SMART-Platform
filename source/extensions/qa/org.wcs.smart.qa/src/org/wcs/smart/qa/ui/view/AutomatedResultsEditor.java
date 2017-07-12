/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.view;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaErrorCleaner;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.internal.Messages;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Editor part of displaying the results of automated validation.
 * 
 * @author Emily
 *
 */
public class AutomatedResultsEditor extends TableMapQaErrorComposite {
	
	public static final String ID = "org.wcs.smart.qa.data.validatation.automated"; //$NON-NLS-1$

	private FormToolkit toolkit;
	private boolean isModified = false;
	
	public static IEditorInput AUTO_VALIDATION_INPUT =  new IEditorInput() {
		
		@Override
		public Object getAdapter(Class adapter) {
			return null;
		}
		
		@Override
		public String getToolTipText() {
			return null;
		}
		
		@Override
		public IPersistableElement getPersistable() {
			return null;
		}
		
		@Override
		public String getName() {
			return null;
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}
		
		@Override
		public boolean exists() {
			return false;
		}
	};
		
	
	private IPartListener2 partListener = new IPartListener2() {
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partOpened(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partHidden(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) { }
		
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			if (isModified){
				if (MessageDialog.openQuestion(getSite().getShell(), Messages.AutomatedResultsEditor_CleanTitle, MessageFormat.format(Messages.AutomatedResultsEditor_CleanMsg, QaError.Status.NEW.getGuiName(Locale.getDefault())))){
					InternalExtensionManager.INSTANCE.cleanAutoResults();
				}
				
			}
		}
		

	};
	
	public AutomatedResultsEditor(){
		tableColumns = new ResultTableColumn[]{
				ResultTableColumn.STATUS,
				ResultTableColumn.DATE,
				ResultTableColumn.DATA_TYPE,
				ResultTableColumn.ROUTINE,
				ResultTableColumn.OBJECT_ID,
				ResultTableColumn.DESC,
				ResultTableColumn.FIX
		};
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		getSite().getPage().addPartListener(partListener);
	}

	@Override
	public void saveErrorItems(List<QaError> items){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			for (QaError i : items){
				for (QaError link : i.getLinks()){
					s.saveOrUpdate(link);
				}
				s.saveOrUpdate(i);
			}
			s.getTransaction().commit();
			isModified = true;
		}catch (Exception ex){
			QaPlugIn.displayLog(MessageFormat.format(Messages.AutomatedResultsEditor_SaveError, ex.getMessage()), ex);
		}finally{
			s.close();
		}		
	} 
	
	@Override
	protected void createHeaderToolbar(Composite parent){
		ToolBar tb = new ToolBar(parent,  SWT.NONE);
		
		ToolItem btnClean = new ToolItem(tb, SWT.PUSH);
		btnClean.setToolTipText(Messages.AutomatedResultsEditor_RemoveTooltip);
		btnClean.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnClean.addListener(SWT.Selection, e->{
			if (MessageDialog.openQuestion(getSite().getShell(), Messages.AutomatedResultsEditor_ConfirmDelete, MessageFormat.format(Messages.AutomatedResultsEditor_DeleteMessage,QaError.Status.NEW.getGuiName(Locale.getDefault())))){
				Session session = HibernateManager.openSession();
				try{
					session.beginTransaction();
					QaErrorCleaner.INSTANCE.cleanItems(SmartDB.getCurrentConservationArea(), session);
					session.getTransaction().commit();
				}catch (Exception ex){
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
					QaPlugIn.displayLog(ex.getMessage(), ex);
				}finally{
					session.close();
				}
				
				clearResults();
				loadData();
			}
			
		});
		
		ToolItem btnRefresh = new ToolItem(tb, SWT.PUSH);
		btnRefresh.setToolTipText(Messages.AutomatedResultsEditor_RelaodTooltip);
		btnRefresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		btnRefresh.addListener(SWT.Selection, e->{
			clearResults();
			loadData();
		});
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit =  new FormToolkit(parent.getDisplay());
		
		Form form = toolkit.createForm(parent);
		form.setText(Messages.AutomatedResultsEditor_FormTitle);
		form.getBody().setLayout(new GridLayout());
		
		super.createPartControl(form.getBody());
		loadData();
	}

	@Override
    public void dispose() {
		super.dispose();
		getSite().getPage().removePartListener(partListener);
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}

	@Override
	public EditorPart getParentEditor() {
		return this;
	}

	/*
	 * Loads all possible record sources from db and populates 
	 * provided combo
	 * @param cmbSource
	 */
	private void loadData(){
		tblResults.setInput(new String[]{DialogConstants.LOADING_TEXT});
		j.setSystem(true);
		j.cancel();
		j.schedule();
	}
	
	Job j = new Job(Messages.AutomatedResultsEditor_LoadResultsJobName){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QaError> errors = new ArrayList<>();
			Session s = HibernateManager.openSession();
			try{
				List<QaError> allErrors = s.createCriteria(QaError.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.list();
				//configure links
				for (QaError e : allErrors){
					for (QaError link : errors){
						if (e.getSourceId().equals(link.getSourceId())){
							e.addLink(link);
							link.addLink(e);
						}
					}
					errors.add(e);
					e.getQaRoutine().getName();
					e.getDataProvider().getName(Locale.getDefault());
				}
			}finally{
				s.close();
			}
			
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			Display.getDefault().asyncExec(()->{
				if (monitor.isCanceled()) return;
				setResults(errors);
			});
			return Status.OK_STATUS;
		}
		
	};
	
}