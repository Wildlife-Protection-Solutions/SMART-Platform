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
package org.wcs.smart.er.query.ui.filter;

import java.util.Collection;

import javax.persistence.Transient;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.ui.SurveyDesignDialog;
import org.wcs.smart.er.query.ui.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.dropitems.ISurveyDesignDropItem;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Simple filter definition panel.
 * @author Emily
 *
 */
public class SurveyFilterDefintionPanel extends BasicFilterDefintionPanel {

	public static final String ID = "org.wcs.smart.query.er.survey.definition.filter"; //$NON-NLS-1$
	
	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public SurveyFilterDefintionPanel(){
	}
	
	/**
	 * Return the unique identifier for the panel
	 */
	@Override
	public String getId(){
		return ID;
	}
	
	/**
	 * Clears all items from the query and hides the 
	 * proxy.
	 * 
	 */
	@Override
	public void clear(){
		super.clear();		
	}
	
	/**
	 * Converts the items that make up the query to 
	 * a query string.
	 * 
	 * @return the query string represented by the items in the query panel
	 */
	@Override
	public String getQueryPart(){
//		StringBuilder query = new StringBuilder();
//		
//		if (items.size() > 0){
//			//if non-empty filter then include filter type
//			if (btnWaypoint.getSelection()){
//				query.append(IFilter.FilterType.WAYPOINT.getKey());
//			}else{
//				query.append(IFilter.FilterType.OBSERVATION.getKey());
//			}
//			query.append("|"); //$NON-NLS-1$
//		}
//		
//		for (Object item : items){
//			if (item instanceof DropItem){
//				DropItem it = (DropItem)item;
//				query.append(it.asQueryPart());
//				query.append(" "); //$NON-NLS-1$
//			}
//		}
//		return query.toString().trim();
		return "";
	}
	
	
	private Link surveyDesignLabel;
	private IQueryType currentType;
	private SurveyDesign currentDesign;
	
	public SurveyDesign getSurveyDesign(){
		return this.currentDesign;
	}
	
	public void addItem(DropItem item){
		super.addItem(item);
		if (item instanceof ISurveyDesignDropItem){
			((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
		}
	}
	
	public void addItems(Collection<DropItem> items) {
		super.addItems(items);
		for (DropItem item : super.items){
			if (item instanceof ISurveyDesignDropItem){
				((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
			}	
		}
	}
	
	@Override
	public void initItems(QueryProxy q){
		this.currentType = q.getQuery().getType();
		
		SurveyObservationQuery sq = (SurveyObservationQuery) q.getQuery();
		currentDesign = sq.getSurveyDesignAsObject();
		
		updateDesignLabel();

		super.initItems(q);
		
		surveyDesignModified();
	}
	
	private void updateDesignLabel(){
		String text = "All";
		if (currentDesign != null){
			text = currentDesign.getName();
		}
		surveyDesignLabel.setText("<a>" + text + "</a>");
		surveyDesignLabel.getParent().getParent().layout(true);
	}
	
	@Override
	protected void createFilterTypeComposite(Composite parent){
		final Composite outer = new Composite(parent, SWT.BORDER);
		outer.setLayout(new GridLayout(2, false));
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		super.createFilterTypeComposite(outer);
		
		Composite right = new Composite(outer, SWT.BORDER);
		right.setLayout(new GridLayout(2, false));
		right.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		Label l = new Label(right, SWT.NONE);
		l.setText("Survey Design:");
		
		surveyDesignLabel = new Link(right, SWT.NONE);
		surveyDesignLabel.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				SurveyDesignDialog dialog = new SurveyDesignDialog(outer.getShell());
				if (dialog.open() == SurveyDesignDialog.OK){
					//update query
					currentDesign = dialog.getSelectedDesign();
					updateDesignLabel();
					surveyDesignModified();
				}
			}
		});
	}
	
	/*
	 * Called when the survey design associated with the query
	 * has been modified.
	 * This needs to refresh the survey filter item panel 
	 */
	private void surveyDesignModified(){
		((SurveyFilterItemPanel)DefinitionPanelManager.getInstance().getQueryItemPanel(getId(), currentType)).refreshPanel(currentDesign);
		
		for (DropItem di : super.items){
			if (di instanceof ISurveyDesignDropItem){
				((ISurveyDesignDropItem) di).setSurveyDesign(currentDesign);
			}
		}
	}
	
}
