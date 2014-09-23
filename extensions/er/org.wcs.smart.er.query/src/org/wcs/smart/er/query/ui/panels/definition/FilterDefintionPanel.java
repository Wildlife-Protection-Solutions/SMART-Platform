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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.ui.SurveyDesignDialog;
import org.wcs.smart.er.query.ui.dropitems.ISurveyDesignDropItem;
import org.wcs.smart.er.query.ui.editor.SurveyQueryEventManager;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
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
	
	private SurveyQueryEventManager.SurveyDesignChangeListener listener;
	
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
		super();
		this.showSurveyDesignLabel = showSurveyDesign;
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
		super.addItem(item);
		if (item instanceof ISurveyDesignDropItem) {
			((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
		}
	}

	/**
	 * adds a collection of drop items; sets the 
	 * survey designs if appropriate
	 */
	public void addItems(Collection<DropItem> items) {
		super.addItems(items);
		for (DropItem item : super.items) {
			if (item instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) item).setSurveyDesign(currentDesign);
			}
		}
	}

	@Override
	public void initItems(QueryProxy q) {
		super.initItems(q);
		
		if (q.getQuery() instanceof ISurveyQuery){
			ISurveyQuery sq = (ISurveyQuery) q.getQuery();
			refreshPanel(sq.getSurveyDesignAsObject());
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

		super.createFilterTypeComposite(outer);

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
					SurveyDesignDialog dialog = new SurveyDesignDialog(outer
							.getShell());
					if (dialog.open() == SurveyDesignDialog.OK) {
						// update query
						SurveyDesign newDesign = dialog.getSelectedDesign();

						if ((currentDesign == null && newDesign != null)
								|| (currentDesign != null 
								&& !currentDesign.equals(newDesign))) {
							((ISurveyQuery) currentQuery.getQuery()).setSurveyDesign(newDesign);
							SurveyQueryEventManager.getInstance().fireSurveyDesignChange((ISurveyQuery) currentQuery.getQuery());
							fireQueryChangedListeners();
						}
					}
				}
			});
			updateDesignLabel();
		}
	}



	@Override
	public void refreshPanel(SurveyDesign newDesign) {
		this.currentDesign = newDesign;
		
		updateDesignLabel();
		
		for (DropItem di : super.items) {
			if (di instanceof ISurveyDesignDropItem) {
				((ISurveyDesignDropItem) di).setSurveyDesign(currentDesign);
			}
		}
		
		IQueryItemPanel pnl = DefinitionPanelManager.getInstance().getQueryItemPanel(getId(), currentQuery.getQuery().getType());
		if (pnl instanceof ISurveyPanel){
			((ISurveyPanel) pnl).refreshPanel(currentDesign);
		}
	}

	@Override
	public Query getQuery() {
		return currentQuery.getQuery();
	}

}
