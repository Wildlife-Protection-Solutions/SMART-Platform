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
package org.wcs.smart.query.ui.newwizard;

import java.net.URL;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryCategory;
import org.wcs.smart.query.ui.QueryTypeLabelProvider;
/**
 * New query wizard
 * 
 * @author Emily
 *
 */
public class NewQueryWizard extends Wizard implements IPageChangingListener {

	private ListHelpWizardPage queryGroupPage;
	private ListHelpWizardPage queryTypePage;
	
	private IQueryType queryType = null;
	
	public NewQueryWizard(){
		super();
		super.setWindowTitle(Messages.NewQueryWizard_NewQueryWizardTitle);
	}
	
    /*
     * (non-Javadoc) Method declared on IWizard.
     */
    public boolean canFinish() {
    	if (getContainer().getCurrentPage() == queryTypePage){
    		if (queryTypePage.getSelection() != null){
    			return true;
    		}
    	}
    	return false;
    }
    
	@Override
	public boolean performFinish() {
		Object x = queryTypePage.getSelection();
		if (x == null || ! (x instanceof IQueryType)){
			MessageDialog.openError(getShell(), Messages.NewQueryWizard_ErrorDialogTitle, Messages.NewQueryWizard_ErrorMessage);
		}
		queryType = (IQueryType) x;
		return true;
	}

	public IQueryType getSelectedQueryType(){
		return this.queryType;
	}

	@Override
	public void addPages() {
		((WizardDialog) getContainer()).addPageChangingListener(this);
		queryGroupPage = new ListHelpWizardPage("QUERYGROUP") { //$NON-NLS-1$

			@Override
			public void updateHelpPage() {
				QueryCategory group = (QueryCategory) getSelection();
				if (group == null){
					helpPage.setText(""); //$NON-NLS-1$
				}else{
					helpPage.setText(group.getHtmlDescription());
				}
			}
		};
		super.addPage(queryGroupPage);
		
		
		
		queryTypePage = new ListHelpWizardPage("QUERYTYPE") { //$NON-NLS-1$
			@Override
			public void updateHelpPage() {
				IQueryType group = (IQueryType) getSelection();
				if (group == null){
					helpPage.setText(""); //$NON-NLS-1$
				}else{
					URL url = group.getDescription();
					if (url == null){
						helpPage.setText(""); //$NON-NLS-1$
					}else{
						helpPage.setUrl(url.toString());
					}
				}
			}

		};
		super.addPage(queryTypePage);

	}
	
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		
		List<QueryCategory> c = QueryTypeManager.INSTANCE.getQueryGroups();
		Collections.sort(c, new Comparator<QueryCategory>() {
			@Override
			public int compare(QueryCategory c0, QueryCategory c1) {
				return Collator.getInstance().compare(c0.getName(), c1.getName());
			}
		});
		
		queryGroupPage.setOptions(c, new LabelProvider() {
			public String getText(Object element) {
				return ((QueryCategory) element).getName();
			}
		});

		queryGroupPage.setMessage(Messages.NewQueryWizard_DataQuery);
		queryTypePage.setMessage(Messages.NewQueryWizard_QueryType);
	}
	
	
	
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == queryGroupPage ) {
			QueryCategory group = (QueryCategory) queryGroupPage.getSelection();
			if (group == null){
				event.doit = false;
				return;
			}
			queryTypePage.setOptions(group.getTypes(), new QueryTypeLabelProvider());
		}
	}
}
