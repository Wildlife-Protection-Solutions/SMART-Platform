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
package org.wcs.smart.query.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.newwizard.NewQueryWizard;
import org.wcs.smart.query.ui.querylist.OpenQueryHandler;

/**
 * Query handler that prompt the user for the type of query they
 * want to create before creating it.
 * @author egouge
 *
 */
public class CreateUnknownQueryHandler extends AbstractHandler {

	private static final String QUERY_TYPE_KEY = "org.wcs.smart.query.type"; //$NON-NLS-1$
	
	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		showQueryPerspective(event);
		String qType = event.getParameter(QUERY_TYPE_KEY);
		if (qType != null){
			IQueryType type = QueryTypeManager.getInstance().findQueryType(qType);
			if (type != null){
				createQuery(type);
				return null;
			}
		}
		createUnknownQuery(event);
		
		return null;
	}

	private void showQueryPerspective(final ExecutionEvent event){
		try {
			String activeId = HandlerUtil.getActivePart(event).getSite().getPage().getPerspective().getId();
			
			String perspectiveId = QueryPlugIn.getActivePerspectiveId();
			if (!activeId.equals(perspectiveId)){
				//show query perspective
				HandlerUtil
				.getActiveWorkbenchWindow(event)
				.getWorkbench()
				.showPerspective(perspectiveId,
						HandlerUtil.getActiveWorkbenchWindow(event));	
			}
			
		} catch (WorkbenchException e) {
			QueryPlugIn
					.displayLog(Messages.CreateUnknownQueryHandler_ErrorLoadingQueryView, e);
		}
	}
	private void createQuery(IQueryType qtype){
		/* open editor for query type */
		OpenQueryHandler.openQuery(new QueryEditorInput(qtype));
	}

	
	private void createUnknownQuery(final ExecutionEvent event){
		NewQueryWizard w = new NewQueryWizard();
		WizardDialog d = new WizardDialog(HandlerUtil.getActiveShell(event), w);
		d.setMinimumPageSize(650,200);
		
		if (d.open() != IDialogConstants.OK_ID){
			return;
		}
		IQueryType type = w.getSelectedQueryType();
		if (type == null){
			return ;
		}
		createQuery(type);
	}
}

