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
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.FilterDefinitionPanel;
/**
 * Simple filter panel for patrol, observation and incident queries.
 * @author Emily
 *
 */
public class SimplePatrolFilterPanel extends FilterDefinitionPanel {
	
	public static String ID  = "org.wcs.smart.patrol.query.SimplePatrolFilterPanel"; //$NON-NLS-1$
	
	private String id = ID;
	
	public SimplePatrolFilterPanel(){
		
	}
	
	public SimplePatrolFilterPanel(String customId){
		this.id = customId;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getGuiName() {
		return Messages.SimplePatrolFilterPanel_PanelTitle;
	}

	@Override
	public String validate() {
		return null;
	}
	

	@Override
	public void initItems(QueryProxy q){
		super.initItems(q);
		
		if (q.getQuery() instanceof SimpleQuery){
			setFilterType( ((SimpleQuery)q.getQuery()).getFilter().getFilterType() );
		}
	}
}
