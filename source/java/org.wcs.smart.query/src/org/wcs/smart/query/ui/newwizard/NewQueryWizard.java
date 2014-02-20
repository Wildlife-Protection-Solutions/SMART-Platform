package org.wcs.smart.query.ui.newwizard;

import java.net.URL;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryTypeGroup;
import org.wcs.smart.query.ui.QueryTypeLabelProvider;

public class NewQueryWizard extends Wizard implements IPageChangingListener {

	private ListHelpWizardPage queryGroupPage;
	private ListHelpWizardPage queryTypePage;
	
	private IQueryType queryType = null;
	
	@Override
	public boolean performFinish() {
		Object x = queryTypePage.getSelection();
		if (x == null || ! (x instanceof IQueryType)){
			MessageDialog.openError(getShell(), "Error", "A valid query type must be selected.");
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
		queryGroupPage = new ListHelpWizardPage("QUERYGROUP") {

			@Override
			public void updateHelpPage() {
				QueryTypeGroup group = (QueryTypeGroup) getSelection();
				if (group == null){
					helpPage.setText("");
				}else{
					helpPage.setText(group.getHtmlDescription());
				}
			}
		};
		super.addPage(queryGroupPage);
		
		
		
		queryTypePage = new ListHelpWizardPage("QUERYTYPE") {
			@Override
			public void updateHelpPage() {
				IQueryType group = (IQueryType) getSelection();
				if (group == null){
					helpPage.setText("");
				}else{
					URL url = group.getDescription();
					if (url == null){
						helpPage.setText("");
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
		 queryGroupPage.setOptions(QueryTypeManager.getInstance().getQueryGroups(),new LabelProvider(){
				public String getText(Object element){
					return ((QueryTypeGroup)element).getName();
				}
			});
		 
		 queryGroupPage.setMessage("Select the general type of data you are interesting in querying");
		queryTypePage.setMessage("Select type of query you wish to perform");
	 }
	
	
	
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		
		if (event.getCurrentPage() == queryGroupPage ) {
			QueryTypeGroup group = (QueryTypeGroup) queryGroupPage.getSelection();
			if (group == null){
				event.doit = false;
				return;
			}
			queryTypePage.setOptions(group.getTypes(), new QueryTypeLabelProvider());
		}
		
		
	}
}
