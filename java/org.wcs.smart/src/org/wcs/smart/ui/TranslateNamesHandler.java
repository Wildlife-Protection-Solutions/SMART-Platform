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
package org.wcs.smart.ui;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Handler for translating names
 * 
 * @author egouge
 *
 */
public class TranslateNamesHandler {

	@Inject ESelectionService selection;
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, Shell activeShell) throws ExecutionException {
		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty()){
			return;
		}
		
		Object obj = ((IStructuredSelection)thisSelection).getFirstElement();
		final Object o = obj;
		if (o instanceof NamedItem ){
			translateItem((NamedItem) o, activeShell);
		}
	}
	
	protected void translateItem(NamedItem toUpdate, Shell activeShell){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(toUpdate);
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(activeShell, toUpdate);
			if (dialog.open() == TranslateSimpleListItemDialog.OK){
				s.getTransaction().commit();
			}else{
				s.getTransaction().rollback();
			}
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.TranslateNamesHandler_Error_TranslatingName, ex);
		}finally{
			s.close();
		}
	}
	
	// E3
	public static class TranslateNamesHandlerWrapper extends DIHandler<TranslateNamesHandler> {
		public TranslateNamesHandlerWrapper() {
			super(TranslateNamesHandler.class);
		}
	}
}