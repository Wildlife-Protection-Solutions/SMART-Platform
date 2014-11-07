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
package org.wcs.smart.er.query.ui.panels.definition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.ui.SurveyDesignDialog;
import org.wcs.smart.er.query.ui.dropitems.ISurveyDesignDropItem;
import org.wcs.smart.er.query.ui.editor.SurveyQueryEventManager;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.BasicGridDefinitionPanel;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
import org.wcs.smart.query.ui.itempanel.IQueryItemPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Gridded patrol query definition panel.
 * @author Emily
 *
 */
public class GriddedDefinitionPanel extends
		BasicGridDefinitionPanel implements ISurveyPanel {

	public static final String ID = "org.wcs.smart.query.er.survey.definition.grid"; //$NON-NLS-1$
	
	public static final String VALUE_PANEL_ID = ID + ".values"; //$NON-NLS-1$
	
	
	private Link surveyDesignLabel;
	private SurveyDesign currentDesign;
	
	private SurveyQueryEventManager.SurveyDesignChangeListener listener;
	
	
	public GriddedDefinitionPanel() {
		super();
		
		listener = new DefinitionListener(this);
		SurveyQueryEventManager.getInstance().addSurveyDesignChangeListener(listener);
	}

	@Override
	public void dispose(){
		SurveyQueryEventManager.getInstance().removeSurveyDesignChangeListener(listener);
		super.dispose();
		
	}
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public String validate() {
		//update the simple value rate filter panel
		((SimpleValueRateFilterPanel)currentQuery.getQueryDefinitionPanel().findQueryDefinitionPanel(SimpleValueRateFilterPanel.ID)).updateFilterPanel(hasRate());
		return super.validate();
	}
	
	@Override
	public void initItems(QueryProxy q) {
		super.initItems(q);
		
		if (q.getQuery() instanceof SurveyGriddedQuery){
			SurveyGriddedQuery sq = (SurveyGriddedQuery) q.getQuery();
			refreshPanel(sq.getSurveyDesignAsObject());
		}
	}

	@Override
	protected void createGridDefinitionPanel(Composite outer){
		Composite leftMain = new Composite(outer, SWT.NONE);
		leftMain.setLayout(new GridLayout(3, false));
		leftMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		leftMain.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		createSurveyDef(leftMain);
		
		createProjection(leftMain);
		createGridSize(leftMain);
		createOrigin(leftMain);
	}
	
	protected void createSurveyDef(final Composite parent){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.GriddedDefinitionPanel_Label);
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		surveyDesignLabel = new Link(parent, SWT.NONE);
		surveyDesignLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		surveyDesignLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		surveyDesignLabel.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SurveyDesignDialog dialog = new SurveyDesignDialog(parent.getShell(), currentDesign);
				if (dialog.open() == SurveyDesignDialog.OK) {
					// update query
					SurveyDesign newDesign = dialog.getSelectedDesign();
					if ((currentDesign == null && newDesign != null) || 
						(currentDesign != null && !currentDesign.equals(newDesign))) {
						
						((ISurveyQuery)currentQuery.getQuery()).setSurveyDesign(newDesign);
						SurveyQueryEventManager.getInstance().fireSurveyDesignChange((ISurveyQuery)currentQuery.getQuery());
						fireQueryChangedListeners();
					}
				}
			}
		});
		
	}
	
	private void updateDesignLabel() {
		String text = Messages.GriddedDefinitionPanel_AllLabel;
		if (currentDesign != null) {
			text = currentDesign.getName();
		}
		surveyDesignLabel.setText("<a>" + text + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		surveyDesignLabel.getParent().getParent().layout(true);
	}

	
	/**
	 * 
	 * @return true if one of the values has an encounter rate
	 */
	public boolean hasRate(){
		for (DropItem it : lstValues.getItems()){
			if (it instanceof AbstractValueDropItem 
					&& ((AbstractValueDropItem)it).hasEncounterRatio()){
				return true;
			}
		}
		return false;
	}

	@Override
	public void refreshPanel(SurveyDesign newDesign) {
		this.currentDesign = newDesign;
		updateDesignLabel();
				
		for (DropItem di : super.lstValues.getItems()) {
			if (di instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) di).setSurveyDesign(currentDesign);
			}
		}
		
		//update associated item panel
		IQueryItemPanel pnl = DefinitionPanelManager.getInstance().getQueryItemPanel(getId(), currentQuery.getQuery().getType());
		if (pnl instanceof ISurveyPanel){
			((ISurveyPanel) pnl).refreshPanel(currentDesign);
		}
	}
	
	@Override
	public Query getQuery() {
		return currentQuery.getQuery();
	}
	
	@Override
	public SurveyDesign getSurveyDesign() {
		return currentDesign;
	}
}
