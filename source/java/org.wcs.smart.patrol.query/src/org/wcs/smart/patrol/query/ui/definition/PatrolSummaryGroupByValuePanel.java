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

import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.query.ui.definition.AbstractSummaryGroupByValuePanel;
import org.wcs.smart.query.ui.definition.ListDefinitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Summary patrol query definition panel. This panel allows
 * users to specify row, column group by and values to compute.
 * 
 * @author Emily
 *
 */
public class PatrolSummaryGroupByValuePanel extends AbstractSummaryGroupByValuePanel{

	public static final String ID = "org.wcs.smart.patrol.query.PatrolSummaryGroupByValuePanel"; //$NON-NLS-1$
	
	public PatrolSummaryGroupByValuePanel(){
		super();
	}
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return Messages.SummaryValueGroupByPanel_GroupByValuePanelTitle;
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
						return Messages.PatrolSummaryGroupByValuePanel_ValueError;
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
				return Messages.PatrolSummaryGroupByValuePanel_GroupByPanelTitle + type.name();
			}
		};
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
