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
package org.wcs.smart.er.query.ui.editor;

import net.refractions.udig.project.internal.Map;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.SurveyGridQueryType;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.ui.GriddedEditor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Jeff
 * @author Emily
 * @since 1.0.0
 */
public class SurveyGriddedEditor extends GriddedEditor  {

	public static final String ID = "org.wcs.smart.er.query.ui.GriddedEditor";  //$NON-NLS-1$

	private SurveyQueryEventManager.SurveyDesignChangeListener updateTable = new SurveyQueryEventManager.SurveyDesignChangeListener(){
		@Override
		public void surveyDesignChange(ISurveyQuery query) {
			if (!getQuery().equals(query)) return;
			
			getQueryResultsTable().clearColumns();
			getQueryResultsTable().initQuery(getQueryInternal());
		
			addSuLayer.schedule();
		}
	};
	
	private AddSamplingUnitLayersJob addSuLayer = null;

	/**
	 * Creates a new results editor
	 */
	public SurveyGriddedEditor(){
		super();
		SurveyQueryEventManager.getInstance().addSurveyDesignChangeListener(updateTable);	
	}
	
	
	/**
	 * Disposes editor
	 */
	@Override
	public void dispose(){
		super.dispose();
		SurveyQueryEventManager.getInstance().removeSurveyDesignChangeListener(updateTable);
		addSuLayer.dispose();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		
		addSuLayer = new AddSamplingUnitLayersJob(){
			@Override
			public Query getQuery() {
				return SurveyGriddedEditor.this.getQuery();
			}
			@Override
			public Map getMap() {
				return SurveyGriddedEditor.this.getMap();
			}};
		addSuLayer.schedule();
	}
	
	/**
	 * Loads the query for the editor
	 * @param session
	 * @param uuid
	 * @return
	 */
	@Override
	public GriddedQuery createQuery(){
		return SurveyQueryFactory.createGriddedQuery();
	}
	
	@Override
	protected IDateFieldFilter[] getDateFilterOptions(){
		return SurveyGridQueryType.validDateFields();
	}
}

	
