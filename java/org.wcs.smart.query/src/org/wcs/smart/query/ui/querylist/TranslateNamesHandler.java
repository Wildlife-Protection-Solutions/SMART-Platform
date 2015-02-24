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
package org.wcs.smart.query.ui.querylist;

import javax.inject.Named;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Extension of the handler for translating names; to fire events
 * 
 * @author egouge
 *
 */
public class TranslateNamesHandler extends org.wcs.smart.ui.TranslateNamesHandler {

	@Execute
	@Override
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection,
			@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) throws ExecutionException {
		
		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty()){
			return;
		}
		
		Object obj = ((IStructuredSelection)thisSelection).getFirstElement();
		final Object o = obj;
		
		NamedItem toUpdate = null;
		if (o instanceof QueryEditorInput){
			QueryEditorInput input = (QueryEditorInput)o;
			Session s = HibernateManager.openSession();
			try{
				s.getTransaction().begin();
				toUpdate = QueryHibernateManager.getInstance().findQuery(s, input.getUuid(), input.getType());
				s.getTransaction().commit();
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.TranslateNamesHandler_LoadQueryError + ex.getMessage(), ex);
			}finally{
				s.close();
			}
		}else if  (o instanceof NamedItem){
			toUpdate = (NamedItem)o;
		}
		
		if (toUpdate == null){
			return;
		}
		
		super.translateItem(toUpdate, activeShell);
		
		if (toUpdate instanceof Query){
			((QueryEditorInput)o).setQueryName(toUpdate.getName());
			QueryEventManager.getInstance().fireQueryNameModified((Query)toUpdate);
		}else if (toUpdate instanceof QueryFolder){
			QueryEventManager.getInstance().fireFolderRenamed((QueryFolder)toUpdate);
		}
		
		return;
	}
	
	public static class TranslateNamesHandlerWrapper extends DIHandler<TranslateNamesHandler>{
		public TranslateNamesHandlerWrapper(){
			super(TranslateNamesHandler.class);
		}
	}
}