package org.wcs.smart.er.query.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
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

public class MissionQueryType implements IQueryType {

	public static final String KEY = "surveymission"; //$NON-NLS-1$
	
	protected String surveyDesignKey;
	protected SurveyDesign surveyDesign;
	
	public MissionQueryType() {
	}

	@Override
	public Class<? extends Query> getHibernateClass() {
		return MissionQuery.class;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getGuiName() {
		return "Mission Query";
	}

	@Override
	public String getEditorId() {
		return SurveySimpleQueryResultEditor.ID;
	}

	@Override
	public Image getImage() {
		return ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.MISSION_ICON);
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
		MissionQuery squery = (MissionQuery)query;
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
		IPath path = new Path("src/org/wcs/smart/er/query/model/types/mission.html"); //$NON-NLS-1$
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
