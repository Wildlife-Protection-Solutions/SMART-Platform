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

import java.util.Collection;
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
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.er.query.ui.SurveyDesignDialog;
import org.wcs.smart.er.query.ui.dropitems.ISurveyDesignDropItem;
import org.wcs.smart.er.query.ui.dropitems.SamplingUnitAttributeDropItem;
import org.wcs.smart.er.query.ui.dropitems.SamplingUnitDropItem;
import org.wcs.smart.er.query.ui.editor.SurveyQueryEventManager;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
import org.wcs.smart.query.ui.itempanel.IQueryItemPanel;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * A definition panel for filtering survey queries.  Includes the ability to
 * change the survey design.
 * 
 * @author Emily
 * 
 */
public class FilterDefintionPanel extends BasicFilterDefintionPanel implements ISurveyPanel{

	public static final String ID = "org.wcs.smart.query.er.survey.definition.filter"; //$NON-NLS-1$

	private Link surveyDesignLabel;
	private SurveyDesign currentDesign;

	private boolean showSurveyDesignLabel = true;
	private boolean includeFilterTypeOp = true;
	
	private SurveyQueryEventManager.QuerySurveyDesignChangeListener listener;
	
	@Inject private DefinitionPanelManager pnlManager;
	
	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public FilterDefintionPanel() {
		this(true);
	}

	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public FilterDefintionPanel(boolean showSurveyDesign) {
		this(showSurveyDesign, true);
	}
	
	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public FilterDefintionPanel(boolean showSurveyDesign, boolean includeFilterTypeOp) {
		super();
		this.showSurveyDesignLabel = showSurveyDesign;
		this.includeFilterTypeOp = includeFilterTypeOp;
		listener = new DefinitionListener(this);
		SurveyQueryEventManager.getInstance().addSurveyDesignChangeListener(listener);
	}
	
	@Override
	public void dispose(){
		SurveyQueryEventManager.getInstance().removeSurveyDesignChangeListener(listener);
		super.dispose();
		
	}

	/**
	 * Return the unique identifier for the panel
	 */
	@Override
	public String getId() {
		return ID;
	}

	/**
	 * Clears all items from the query and hides the proxy.
	 * 
	 */
	@Override
	public void clear() {
		super.clear();
	}

	/**
	 * 
	 * @return the current survey design
	 */
	public SurveyDesign getSurveyDesign() {
		return this.currentDesign;
	}

	/**
	 * adds a drop item;setting the survey design if appropriate
	 */
	public void addItem(DropItem item) {
		configureSamplingUnitDropItem(item);
		super.addItem(item);
		if (item instanceof ISurveyDesignDropItem) {
			((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
		}
	}

	private void configureSamplingUnitDropItem(DropItem item){
		String key = currentQuery.getQueryType().getKey();
	
		if (item instanceof SamplingUnitDropItem){
			SamplingUnitDropItem suItem = (SamplingUnitDropItem) item;
			//these have fixed source; others are variable and defined 
			// elsewhere
			if (key.equals(MissionTrackQuery.KEY)){
				suItem.setSource(Source.TRACK);
			}else if (key.equals(SurveyWaypointQuery.KEY)){
				suItem.setSource(Source.OBSERVATION);
			}else if (key.equals(SurveyObservationQuery.KEY)){
				suItem.setSource(Source.OBSERVATION);
			}
		}else if (item instanceof SamplingUnitAttributeDropItem){
			SamplingUnitAttributeDropItem suItem = (SamplingUnitAttributeDropItem) item;
			
			//these have fixed source; others are variable and defined 
			// elsewhere
			if (key.equals(MissionTrackQuery.KEY)){
				suItem.setSource(Source.TRACK);
			}else if (key.equals(SurveyWaypointQuery.KEY)){
				suItem.setSource(Source.OBSERVATION);
			}else if (key.equals(SurveyObservationQuery.KEY)){
				suItem.setSource(Source.OBSERVATION);
			}
		}
	}
	
	/**
	 * adds a collection of drop items; sets the 
	 * survey designs if appropriate
	 */
	public void addItems(Collection<DropItem> items) {
		if (items != null){
			for (DropItem item : items){
				configureSamplingUnitDropItem(item);
			}
		}
		super.addItems(items);
		for (DropItem item : super.items) {
			if (item instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
			}
		}
	}

	@Override
	public void initItems(QueryProxy q) throws Exception {
		this.currentDesign = null;
		super.initItems(q);
		
		if (q.getQuery() instanceof ISurveyQuery){
			final ISurveyQuery sq = (ISurveyQuery) q.getQuery();
			
			//load and configure survey design
			Job j = new Job(
					Messages.SurveyObservationQuery_loadingDesignJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
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

					return Status.OK_STATUS;
				}
			};
			j.schedule();
			j.join();
		}
	}

	private void updateDesignLabel() {
		if (showSurveyDesignLabel){
			String text = Messages.SurveyFilterDefintionPanel_AllSurveyLabel;
			if (currentDesign != null) {
				text = currentDesign.getName();
			}
			surveyDesignLabel.setText("<a>" + text + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			surveyDesignLabel.getParent().getParent().layout(true);
		}
	}

	@Override
	protected void createFilterTypeComposite(Composite parent) {
		final Composite outer = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		outer.setLayout(gl);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		if (includeFilterTypeOp){
			super.createFilterTypeComposite(outer);
		}else{
			Label spacer = new Label(outer, SWT.NONE);
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		if (showSurveyDesignLabel){
			Composite right = new Composite(outer, SWT.NONE);
			gl = new GridLayout(2, false);
			gl.horizontalSpacing = 5;
			gl.verticalSpacing = 0;
			gl.marginWidth = 5;
			gl.marginHeight = 3;
		
			right.setLayout(gl);
			right.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

			Label l = new Label(right, SWT.NONE);
			l.setText(Messages.SurveyFilterDefintionPanel_SurveyDesignLabel);
			surveyDesignLabel = new Link(right, SWT.NONE);

			surveyDesignLabel.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					SurveyDesignDialog dialog = new SurveyDesignDialog(outer.getShell(), currentDesign);
					if (dialog.open() == SurveyDesignDialog.OK) {
						// update query
						SurveyDesign newDesign = dialog.getSelectedDesign();

						if ((currentDesign == null && newDesign != null)
								|| (currentDesign != null && !currentDesign.equals(newDesign))) {
							if (newDesign == null){
								((ISurveyQuery) currentQuery.getQuery()).setSurveyDesign(null);
							}else{
								((ISurveyQuery) currentQuery.getQuery()).setSurveyDesign(newDesign.getKeyId());
							}
							SurveyQueryEventManager.getInstance().fireQuerySurveyDesignChange((ISurveyQuery) currentQuery.getQuery(), newDesign);
							fireQueryChangedListeners();
						}
					}
				}
			});
			updateDesignLabel();
		}
	}

	@Override
	public void refreshPanel(SurveyDesign surveyDesign) {
		this.currentDesign = surveyDesign;
		updateDesignLabel();
		for (DropItem di : super.items) {
			if (di instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) di).setSurveyDesign(currentDesign);
			}
		}
		IQueryItemPanel pnl = pnlManager.getQueryItemPanel(getId(), currentQuery.getQueryType());
		if (pnl instanceof ISurveyPanel){
			((ISurveyPanel) pnl).refreshPanel(currentDesign);
		}
	}

	@Override
	public Query getQuery() {
		return currentQuery.getQuery();
	}

}
