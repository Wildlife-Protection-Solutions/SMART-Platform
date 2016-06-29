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
package org.wcs.smart.patrol.query.model.types;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.Area;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.patrol.query.ui.editor.PatrolSimpleQueryResultEditor;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
/**
 * Waypoint query type
 * @author Emily
 *
 */
public class PatrolWaypointQueryType implements IQueryType {
	
	private static IDropItemFactory dropItemFactory = null;
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return PatrolWaypointQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return PatrolWaypointQuery.KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.PatrolWaypointQueryType_WaypointQueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return PatrolSimpleQueryResultEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.WAYPOINT_QUERY_ICON);
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#supportsCrossCaQueries()
	 */
	@Override
	public boolean supportsCrossCaQueries() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#supportsSingleCaQueries()
	 */
	@Override
	public boolean supportsSingleCaQueries() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getDropItemFactory()
	 */
	@Override
	public IDropItemFactory getDropItemFactory() {
		if (dropItemFactory == null){
			dropItemFactory = new PatrolDropItemFactory(){
				@Override
				public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
					DropItem[] items = super.generateDropItem(source, queryItemPanelId);
					if (items != null){
						return items;
					}
					if (source instanceof Area){
						items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.WAYPOINT) };
					}
					return items;
					
				}
				
				
			};
		}
		return dropItemFactory;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#updateQueryDefinition(org.wcs.smart.query.model.Query, java.util.List)
	 */
	@Override
	public void updateQueryDefinition(Query query, List<IDefinitionPanel> components) {
		for (IDefinitionPanel p : components){
			if (p.getId().equals(BasicFilterDefintionPanel.ID)){
				((PatrolWaypointQuery)query).setQueryFilter(p.getQueryPart());
			}else if (p.getId().equals(ConservationAreaFilterPanel.ID)){
				query.setConservationAreaFilter(p.getQueryPart());
			}
		}
	}
	
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#validateQuery(java.util.List)
	 */
	public String validateQuery(List<IDefinitionPanel> components) {
		String filters= ""; //$NON-NLS-1$
		
		// validate each panel
		for (IDefinitionPanel p : components){
			String panelError = p.validate();
			if (panelError != null){
				return panelError;
			}
			
			if (p.getId().equals(BasicFilterDefintionPanel.ID)){
				filters = p.getQueryPart();
			}
			
		}
		
		//validate query
		String queryString = filters;
		if (queryString.isEmpty()) return null;
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			parser.QueryFilter();
		}catch (Throwable ex){
			return ex.getMessage();
		}
		return null;
	}

	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE,
				PatrolStartDateField.INSTANCE,
				PatrolEndDateField.INSTANCE};
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/patrol/query/model/types/patrolincident.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, PatrolQueryPlugIn.getDefault().getBundle());
	}
	@Override
	public IQueryResultInfoProvider[] getResultProviders(){
		return new IQueryResultInfoProvider[]{
				new PatrolResultInfoProvider()
		};
	}
}
