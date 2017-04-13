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

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.ui.SurveyDesignDialog;
import org.wcs.smart.er.query.ui.dropitems.ISurveyDesignDropItem;
import org.wcs.smart.er.query.ui.editor.SurveyQueryEventManager;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.AbstractSummaryGroupByValuePanel;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
import org.wcs.smart.query.ui.definition.ListDefinitionPanel;
import org.wcs.smart.query.ui.itempanel.IQueryItemPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Summary patrol query definition panel. This panel allows
 * users to specify row, column group by and values to compute.
 * 
 * @author Emily
 *
 */
public class SummaryDefinitionPanel extends AbstractSummaryGroupByValuePanel
	implements ISurveyPanel {

	public static final String ID = "org.wcs.smart.query.er.survey.definition.summary"; //$NON-NLS-1$
	
	private Link surveyDesignLabel;
	private SurveyDesign currentDesign;
	
	private SurveyQueryEventManager.QuerySurveyDesignChangeListener listener;
	@Inject private DefinitionPanelManager pnlManager; 
	
	public SummaryDefinitionPanel() {
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
	public String getGuiName() {
		return Messages.SummaryDefinitionPanel_Name;
	}

	@Override
	public void addItem(DropItem item) {
		if (item instanceof ISurveyDesignDropItem) {
			((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
		}
		super.addItem(item);
	}

	@Override
	public String validate() {
		//update the simple value rate filter panel
		((SimpleValueRateFilterPanel)currentQuery.getQueryDefinitionPanel().findQueryDefinitionPanel(SimpleValueRateFilterPanel.ID)).updateFilterPanel(hasRate());
		
		String error = super.lstColumnGroupBy.validate();
		if (error != null) return error;
		
		
		error = super.lstRowGroupBy.validate();
		if (error != null) return error;
		
		error = super.lstValues.validate();
		if (error != null) return error;
		
		return null;
	}

	
	@Override
	public String getQueryPart() {
		return  lstValues.getQueryPart() + "|" + lstRowGroupBy.getQueryPart() + "|" + lstColumnGroupBy.getQueryPart(); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Override
	public ListDefinitionPanel createListDropTargetPanel(final ListTargetType type) {
		boolean unique = (type == ListTargetType.COLUMN || type == ListTargetType.ROW);
		
		return new ListDefinitionPanel(unique) {
			
			@Override
			public String validate() {
				if (type == ListTargetType.VALUE){
					if (super.items.size() == 0){
						return Messages.SummaryDefinitionPanel_ValueRequiredError;
					}
				}
				return null;
			}
						
			@Override
			public String getId() {
				return ID + "." + type.name(); //$NON-NLS-1$
			}
			
			@Override
			public String getGuiName() {
				return MessageFormat.format (Messages.SummaryDefinitionPanel_GroupByLabel, new Object[]{type.name()});
			}
		};
	}
	
	@Override
	protected void createValuesHeader(Composite rightInner){
		Composite c = new Composite(rightInner, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = gl.marginHeight = 0;
		c.setLayout(gl);
		
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		super.createValuesHeader(c);
		
		final Composite parent = new Composite(c, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = 0;
		parent.setLayout(gl);
		parent.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.SummaryDefinitionPanel_SurveyDesignLabel);
		lbl.setBackground(rightInner.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		surveyDesignLabel = new Link(parent, SWT.NONE);
		surveyDesignLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		surveyDesignLabel.setBackground(rightInner.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		surveyDesignLabel.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SurveyDesignDialog dialog = new SurveyDesignDialog(parent.getShell(), currentDesign);
				if (dialog.open() == SurveyDesignDialog.OK) {
					// update query
					SurveyDesign newDesign = dialog.getSelectedDesign();
					if ((currentDesign == null && newDesign != null) || 
						(currentDesign != null && !currentDesign.equals(newDesign))) {
						if (newDesign == null){
							((ISurveyQuery)currentQuery.getQuery()).setSurveyDesign(null);
						}else{
							((ISurveyQuery)currentQuery.getQuery()).setSurveyDesign(newDesign.getKeyId());
						}
						SurveyQueryEventManager.getInstance().fireQuerySurveyDesignChange((ISurveyQuery)currentQuery.getQuery(), newDesign);
						fireQueryChangedListeners();
					}
				}
			}
		});
		
		updateDesignLabel();
	}
	
	@Override
	public void initItems(QueryProxy q) {
		super.initItems(q);
		
		if (q.getQuery() instanceof ISurveyQuery){
			final ISurveyQuery sq = (ISurveyQuery) q.getQuery();
			
			
			//load and configure survey design
			Job j = new Job(
					Messages.SurveyObservationQuery_loadingDesignJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						List<?> results = s
							.createCriteria(SurveyDesign.class)
							.add(Restrictions.eq("keyId", sq.getSurveyDesign())) //$NON-NLS-1$
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.list(); 

						if (results.size() > 0) {
							final SurveyDesign sd = (SurveyDesign) results.get(0);
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									refreshPanel(sd);
								}
							});
						}
					}finally{
						s.close();
					}

					return Status.OK_STATUS;
				}
			};
			j.schedule();
		}
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

	private void updateDesignLabel() {
		String text = Messages.SummaryDefinitionPanel_AllLabels;
		if (currentDesign != null) {
			text = currentDesign.getName();
		}
		surveyDesignLabel.setText("<a>" + text + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		surveyDesignLabel.getParent().getParent().layout(true);
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
		for (DropItem di : super.lstColumnGroupBy.getItems()) {
			if (di instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) di).setSurveyDesign(currentDesign);
			}
		}
		for (DropItem di : super.lstRowGroupBy.getItems()) {
			if (di instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) di).setSurveyDesign(currentDesign);
			}
		}
		
		//update associated item panel
		IQueryItemPanel pnl = pnlManager.getQueryItemPanel(getId(), currentQuery.getQueryType());
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
