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
package org.wcs.smart.patrol.query.ui.definition;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.GriddedQuery;
import org.wcs.smart.patrol.query.ui.definition.dropItems.AbstractValueDropItem;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.AbstractGridDefinitionPanel;
import org.wcs.smart.query.ui.definition.ListDefinitionPanel;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Gridded patrol query definition panel.
 * @author Emily
 *
 */
public class PatrolGriddedQueryDefinitionPanel extends
		AbstractGridDefinitionPanel {

	public static final String ID = "org.wcs.smart.patrol.query.PatrolGriddedQueryDefinitionPanel"; //$NON-NLS-1$
	
	public static final String VALUE_PANEL_ID = ID + ".values"; //$NON-NLS-1$
	
	public PatrolGriddedQueryDefinitionPanel() {
		super();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return Messages.GriddedQueryDefinitionComposite_GridValueDefinitionSectionHeader;
	}

	@Override
	public ListDefinitionPanel createValueListPanel() {
		return new ListDefinitionPanel(false) {
			@Override
			public String validate() {
				if (items.size() != 1){
					return Messages.PatrolGriddedQueryDefinitionPanel_ValueRequiredError;
				}
				return null;
			}
			@Override
			public String getId() {
				return VALUE_PANEL_ID;
			}
			
			@Override
			public String getGuiName() {
				return Messages.PatrolGriddedQueryDefinitionPanel_ValuePanelTitle;
			}
		};
	}

	@Override
	public void initItems(QueryProxy q) {
		super.initItems(q);
		
		isInitializing = true;
		try{
			GriddedQuery gridQuery = (GriddedQuery)q.getQuery();
			txtGridSize.setText(String.valueOf(gridQuery.getGridSize()));
			try {
				selectProjection(gridQuery.getCoordinateReferenceSystem());
			} catch (Exception e) {
				PatrolQueryPlugIn.log(e.getMessage(), e);
			}
		}finally{
			isInitializing = false;
		}
		
		
	}
	
	@Override
	public CoordinateReferenceSystem getDefaultProjection() {
		try {
			return ((GriddedQuery)currentQuery.getQuery()).getCoordinateReferenceSystem();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String validate() {
		//update the simple value rate filter panel
		((SimpleValueRateFilterPanel)currentQuery.getQueryDefinitionPanel().findQueryDefinitionPanel(SimpleValueRateFilterPanel.ID)).updateFilterPanel(hasRate());
		
		String error = lstValues.validate();
		if (error != null){
			return error;
		}
		try{
			double x = getGridSize();
			if (x <= 0){
				return Messages.PatrolGriddedQueryDefinitionPanel_GridSizeError1;
			}
		}catch (Exception ex){
			return Messages.PatrolGriddedQueryDefinitionPanel_GridSizeError2 + ex.getMessage();
		}
		if (getCrs() == null){
			return Messages.PatrolGriddedQueryDefinitionPanel_CRSError;
		}
		return null;
	}

	@Override
	public String getQueryPart() {
		return lstValues.getQueryPart() + "|" + getGridSize(); //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @return true if one of the values has an encounter rate
	 */
	public boolean hasRate(){
		for (DropItem it : lstValues.getItems()){
			if (it instanceof AbstractValueDropItem && ((AbstractValueDropItem)it).hasEncounterRatio()){
				return true;
			}
		}
		return false;
	}
}
