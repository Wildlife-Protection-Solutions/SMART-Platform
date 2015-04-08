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
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.patrol.query.ui.definition.PatrolGriddedQueryDefinitionPanel;
import org.wcs.smart.patrol.query.ui.definition.SimpleValueRateFilterPanel;
import org.wcs.smart.patrol.query.ui.editor.PatrolGriddedEditor;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Patrol Query Type
 * @author Emily
 *
 */
public class PatrolGridQueryType implements IQueryType {

	private static IDropItemFactory dropItemFactory = null;
	
	public static final String KEY = "patrolgrid"; //$NON-NLS-1$
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return PatrolGriddedQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.PatrolGridQueryType_PatrolGriddedQueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return PatrolGriddedEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.GRIDDED_SUMMARY_QUERY_ICON);
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
					
					if (source instanceof Area){
						items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.TRACK) };				
					}

					if (items != null){
						for (int i = 0; i < items.length; i ++){
							if (items[i] instanceof AbstractValueDropItem){
								((AbstractValueDropItem)items[i]).setEncounterRateOptions(PatrolQueryOptions.GRID_ENCOUNTER_RATE_DROP_OPTIONS);
							}
						}
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
	
		PatrolGriddedQuery summary = (PatrolGriddedQuery)query;
		
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		for (IDefinitionPanel p : components){
			if (p.getId().equals(SimpleValueRateFilterPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(PatrolGriddedQueryDefinitionPanel.ID)){
				definition = p.getQueryPart();
				if (((PatrolGriddedQueryDefinitionPanel)p).getCrs() != null){
					summary.setCrsDefinition(((PatrolGriddedQueryDefinitionPanel)p).getCrs().toWKT());
				}
			}else if (p.getId().equals(ConservationAreaFilterPanel.ID)){
				query.setConservationAreaFilter(p.getQueryPart());
			}
		}
		summary.setQuery(definition + "|" + filters); //$NON-NLS-1$
	}
	
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#validateQuery(java.util.List)
	 */
	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		// validate each panel
		for (IDefinitionPanel p : components){
			String panelError = p.validate();
			if (panelError != null){
				return panelError;
			}
			
			if (p.getId().equals(SimpleValueRateFilterPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(PatrolGriddedQueryDefinitionPanel.ID)){
				definition = p.getQueryPart();
			}
			
		}
		
		//validate query
		String queryString = definition + "|" + filters; //$NON-NLS-1$
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			parser.GridQuery();
		}catch (Exception ex){
			return ex.getMessage();
		}
		return null;
	}


	/**
	 * Valid filter fields for query type
	 * @return
	 */
	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE,
				PatrolStartDateField.INSTANCE,
				PatrolEndDateField.INSTANCE};
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/patrol/query/model/types/patrolgrid.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, PatrolQueryPlugIn.getDefault().getBundle());
	}

}