package org.wcs.smart.i2.birt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.birt.ui.IReportEditorManager;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;

public class IntelReportEditorManager implements IReportEditorManager{ 


	private RCPMultiPageReportEditor editor;
	
	public IntelReportEditorManager() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
//		ModuleHandle initialModel = editor.getModel();
//		Session s = HibernateManager.openSession();
//		try{
//			s.beginTransaction();
//			if (editor.getModel() instanceof ReportDesignHandle){
//				ReportManager.updateReportQueries(s, (ReportDesignHandle)editor.getModel(), getEditorInputLocal().getReport());
//			}
//			editor.doSaveParent(monitor);
//			s.getTransaction().commit();
//		}catch (Exception ex){
//			s.getTransaction().rollback();
//			ReportPlugIn.displayLog(Messages.RCPMultiPageReportEditor_Error_SavingReport + ex.getLocalizedMessage(), ex);
//		}finally{
//			s.close();
//		}
		try {
			//on the xml page saving changes the model so we need to reconfigure
			//the listeners
//			if (!editor.getModel().equals(initialModel)){
//				initialModel.removeListener(nameChangeListener);
//				editor.getModel().addListener(nameChangeListener);
//			}
			editor.refreshMarkers(editor.getEditorInput());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		editor.doSaveParent(monitor);

	}

	@Override
	public void doSaveAs() {
		//not supported
		
	}
//
//
//	/*
//	 * Fires the required notifications.  This is copied from
//	 *  org.eclipse.birt.report.designer.internal.ui.util.UIUtil.doFinishSave
//	 * 
//	 */
//	private void doFinishSave() {
//		ModuleHandle model = editor.getModel();
//		Object[] resolves = ElementAdapterManager.getAdapters(model,
//				IRelatedFileChangeResolve.class);
//		if (resolves == null) {
//			return;
//		}
//
//		for (int i = 0; i < resolves.length; i++) {
//			IRelatedFileChangeResolve find = (IRelatedFileChangeResolve) resolves[i];
//			find.notifySaveFile(model);
//		}
//	}
	
	@Override
	public void dispose() {
		editor = null;
	}

	@Override
	public void addPages() {	
		if (editor.getEditorInput() instanceof IntelEntityEditorInput){
			IntelEntityEditorInput in = getEditorInputLocal();
			//if we are editing a SMART report update the name; otherwise leave it alone 
			editor.setPartName(in.getEntityType().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void init(RCPMultiPageReportEditor editor) {
		this.editor = editor;
	}

	private IntelEntityEditorInput getEditorInputLocal(){
		if (editor == null){
			return null;
		}
		return (IntelEntityEditorInput)editor.getEditorInput();
	}

}
