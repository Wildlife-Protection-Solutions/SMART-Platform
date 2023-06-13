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
package org.wcs.smart.query.ui;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.OpenQueryHandler;
import org.wcs.smart.ui.SmartStyledInputDialog;

/**
 * Handler for creating a new SMART report from existing report
 * 
 * @author egouge
 * @since 7.0.0
 */
@SuppressWarnings("restriction")
public class SaveAsQueryHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, final Shell activeShell){
		if (thisSelection == null) return;
		QueryEditorInput tocopy = null;
		if (thisSelection instanceof QueryEditorInput) {
			tocopy = (QueryEditorInput)thisSelection;
		}else if (thisSelection instanceof Query) {
			tocopy = new QueryEditorInput((Query)thisSelection);
		}else if (thisSelection instanceof IStructuredSelection) {
			Object x = ((IStructuredSelection) thisSelection).getFirstElement();
			if (x instanceof QueryEditorInput) tocopy = (QueryEditorInput)x;
			if (x instanceof Query) tocopy = new QueryEditorInput((Query)x);
		}
		
		if (tocopy == null) return;
		
		InputDialog id = new SmartStyledInputDialog(activeShell, Messages.SaveAsQueryHandler_DialogTitle, Messages.SaveAsQueryHandler_DialogMsg, tocopy.getName() + Messages.SaveAsQueryHandler_CopyName, new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText.strip().isEmpty()) return Messages.SaveAsQueryHandler_NameRequired;
				return null;
			}
			
		});
		
		if (id.open() != Window.OK) return;
		
		Query clone;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Query c = QueryHibernateManager.getInstance().findQuery(session, tocopy.getUuid(), tocopy.getType());
				if (c == null) return;
				
				clone = c.clone(c.getOwner());
				clone.getNames().clear();
				clone.setName(id.getValue());
				clone.updateName(SmartDB.getCurrentLanguage(), id.getValue());
				clone.setIsShared(c.getIsShared());
				clone.setFolder(c.getFolder());
				clone.setId(QueryHibernateManager.getInstance().generateQueryId(session));
				
				QueryEventManager.getInstance().fireBeforeSave(clone, session);
				
				session.persist(clone);
				session.getTransaction().commit();
			}catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.SaveAsQueryHandler_CopyError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return;
			}
		}
	
		QueryEventManager.getInstance().fireQueryAdded(clone);
		(new OpenQueryHandler()).execute(new StructuredSelection(new QueryEditorInput(clone)));	
	}

	
	public static class SaveAsQueryHandlerWrapper extends DIHandler<SaveAsQueryHandler>{
		public SaveAsQueryHandlerWrapper(){
			super(SaveAsQueryHandler.class);
		}
	}
}
