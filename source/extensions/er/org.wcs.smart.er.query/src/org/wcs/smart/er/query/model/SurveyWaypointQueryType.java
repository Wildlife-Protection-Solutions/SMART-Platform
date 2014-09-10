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
package org.wcs.smart.er.query.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.editor.SurveySimpleQueryResultEditor;
import org.wcs.smart.er.query.ui.panels.definition.FilterDefintionPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

/**
 * Survey incident/waypoint query type.
 * 
 * @author Emily
 *
 */
public class SurveyWaypointQueryType implements IQueryType {

	public static final String KEY = "surveywaypoint"; //$NON-NLS-1$
	
	public SurveyWaypointQueryType() {
	}

	@Override
	public Class<? extends Query> getHibernateClass() {
		return SurveyWaypointQuery.class;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getGuiName() {
		return Messages.SurveyWaypointQueryType_IncidentQuery;
	}

	@Override
	public String getEditorId() {
		return SurveySimpleQueryResultEditor.ID;
	}

	@Override
	public Image getImage() {
		return ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.WAYPOINT_ICON);
	}

	@Override
	public boolean supportsCrossCaQueries() {
		return false;
	}

	@Override
	public boolean supportsSingleCaQueries() {
		return true;
	}

	@Override
	public IDropItemFactory getDropItemFactory() {
		return SurveyDropItemFactory.INSTANCE;		
	}

	@Override
	public void updateQueryDefinition(Query query,
			List<IDefinitionPanel> components) {
		SurveyWaypointQuery squery = (SurveyWaypointQuery)query;
		for (IDefinitionPanel panel : components){
			if (panel.getId().equals(FilterDefintionPanel.ID)){
				squery.setSurveyDesign(  ((FilterDefintionPanel)panel).getSurveyDesign()  );
				squery.setQueryFilter(  ((FilterDefintionPanel)panel).getQueryPart()  );
			}else if (panel.getId().equals(ConservationAreaFilterPanel.ID)){
				squery.setConservationAreaFilter(  ((ConservationAreaFilterPanel)panel).getCaFilter() );
			}
		}

	}

	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		String filter = ""; //$NON-NLS-1$
		for (IDefinitionPanel panel : components){
			String msg = panel.validate();
			if (msg != null){
				return msg;
			}
			
			if (panel.getId().equals(FilterDefintionPanel.ID)){
				filter = panel.getQueryPart();
			}	
		}
		
		//validate query
		String queryString = filter;
		if (queryString.isEmpty()) return null;
		InputStream is = new ByteArrayInputStream(queryString.getBytes());
		try{
			Parser parser = new Parser(is);
			parser.QueryFilter();
		}catch (Throwable ex){
			return ex.getMessage();
		}finally{
			try {
				is.close();
			} catch (IOException e) {
				//eatme
			}
		}
		return null;
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/er/query/model/types/surveyincident.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, ERQueryPlugIn.getDefault().getBundle());
	}
	
	/**
	 * Valid filter fields for query type
	 * @return
	 */
	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE, MissionStartDateField.INSTANCE, MissionEndDateField.INSTANCE};
	}

}
