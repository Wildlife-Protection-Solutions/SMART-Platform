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

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.map.SurveyQueryDataSource;
import org.wcs.smart.er.query.map.SurveyQueryService;
import org.wcs.smart.er.query.map.style.MissionWaypointQueryDefaultStyle;
import org.wcs.smart.er.query.map.style.MissionWaypointTrackQueryDefaultStyle;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.editor.SurveySimpleQueryResultEditor;
import org.wcs.smart.er.query.ui.panels.definition.FilterDefintionPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.WaypointGeometryQueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.filter.date.WaypointLastModifiedDateField;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.IQueryDefinitionPanel;
import org.wcs.smart.query.ui.model.IQueryDropItemFactory;

/**
 * Survey incident/waypoint query type.
 * 
 * @author Emily
 *
 */
public class SurveyWaypointQueryType implements IMappableQueryType {

	private static final HashMap<String, String> styleMappings = new HashMap<>();
	static{
		styleMappings.put( WaypointGeometryQueryColumn.KEY, MissionWaypointQueryDefaultStyle.KEY);
		styleMappings.put( SurveyQueryDataSource.MISSION_TRACK.getLocalPart(), MissionWaypointTrackQueryDefaultStyle.KEY);
	}
	
	public SurveyWaypointQueryType() {
	}

	@Override
	public Class<? extends Query> getHibernateClass() {
		return SurveyWaypointQuery.class;
	}

	@Override
	public String getKey() {
		return SurveyWaypointQuery.KEY;
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
	public boolean supportsImageResult() {
		return true;
	}

	@Override
	public IQueryDropItemFactory getDropItemFactory() {
		return SurveyDropItemFactory.INSTANCE;		
	}

	@Override
	public void updateQueryDefinition(Query query,
			List<IQueryDefinitionPanel> components) {
		SurveyWaypointQuery squery = (SurveyWaypointQuery)query;
		for (IQueryDefinitionPanel panel : components){
			if (panel.getId().equals(FilterDefintionPanel.ID)){
				squery.setSurveyDesign(  ((FilterDefintionPanel)panel).getSurveyDesign()  );
				squery.setQueryFilter(  ((FilterDefintionPanel)panel).getQueryPart()  );
			}else if (panel.getId().equals(ConservationAreaFilterPanel.ID)){
				squery.setConservationAreaFilter(  ((ConservationAreaFilterPanel)panel).getCaFilter().asString() );
			}
		}

	}

	@Override
	public String validateQuery(List<IQueryDefinitionPanel> components) {
		String filter = ""; //$NON-NLS-1$
		for (IQueryDefinitionPanel panel : components){
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
		
		try(Reader is = new StringReader(queryString)){
			Parser parser = new Parser(is);
			parser.QueryFilter();
		}catch (Throwable ex){
			return ex.getMessage();
		}
		return null;
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/er/query/model/types/surveywaypoint.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, ERQueryPlugIn.getDefault().getBundle());
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getDateFilterOptions()
	 */
	@Override
	public IDateFieldFilter[] getDateFilterOptions() {
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE, 
				MissionStartDateField.INSTANCE,
				MissionEndDateField.INSTANCE,
				WaypointLastModifiedDateField.INSTANCE};
	}

	@Override
	public IQueryResultInfoProvider[] getResultProviders(){
		return new IQueryResultInfoProvider[]{
				new SurveyResultInfoProvider(),
				new SurveyZoomToResultProvider()
		};
	}
	
	/**
	 * @see org.wcs.smart.query.model.IMappableQueryType#createQueryService(org.wcs.smart.query.model.Query)
	 */
	@Override
	public IQueryService createQueryService(Query query, IProjectionProvider prjProvider){
		return new SurveyQueryService(query, prjProvider);
	}
	
	/**
	 * 
	 * @return true if query type is supported in reports;
	 * false otherwise
	 */
	public boolean supportsReports(){
		return true;
	}
	
	/**
	 * 
	 * @return a map that links a layer georesource id to the default style key
	 * 
	 */
	@Override
	public Map<String, String> getDefaultStyleMappings(){
		return styleMappings;
	}
}
