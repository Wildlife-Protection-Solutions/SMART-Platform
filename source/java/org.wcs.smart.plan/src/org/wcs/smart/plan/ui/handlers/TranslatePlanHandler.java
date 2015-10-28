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
package org.wcs.smart.plan.ui.handlers;

import java.util.UUID;

import javax.inject.Named;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.ui.TranslateNamesHandler;

/**
 * Handler that brings up the plan translate dialog.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class TranslatePlanHandler extends TranslateNamesHandler {

	@Override
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, Shell activeShell) throws ExecutionException {

		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((StructuredSelection)thisSelection).isEmpty() ) {
			return ;
		}
		
		Object obj = ((IStructuredSelection)thisSelection).getFirstElement();

		Plan plan = null;
		if (obj instanceof PlanEditorInput) {
			UUID uuid = ((PlanEditorInput) obj).getUuid();
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				plan = (Plan) session.load(Plan.class, uuid);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		} else if (obj instanceof Plan) {
			plan = (Plan) obj;
		}
		
		if (plan != null) {
			translateItem(plan, activeShell);
			PlanEventManager.getInstance().planChanged(0, plan);
		}
	}
	
	public static class TranslatePlanHandlerWrapper extends DIHandler<TranslatePlanHandler>{
		public TranslatePlanHandlerWrapper(){
			super(TranslatePlanHandler.class);
		}
	}
	
}
