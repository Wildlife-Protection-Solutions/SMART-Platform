package org.wcs.smart.er.query.model;

import java.net.URL;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.query.ui.editor.SurveySimpleQueryResultEditor;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

public class SurveyObservationQueryType implements IQueryType {

	public static final String KEY = "surveyobservation"; //$NON-NLS-1$
	
	public SurveyObservationQueryType() {
	}

	@Override
	public Class<? extends Query> getHibernateClass() {
		return SurveyObservationQuery.class;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getGuiName() {
		return "Survey Observation Query";
	}

	@Override
	public String getEditorId() {
		return SurveySimpleQueryResultEditor.ID;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateQueryDefinition(Query query,
			List<IDefinitionPanel> components) {
		// TODO Auto-generated method stub

	}

	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getDescription() {
		return null;
	}
	
	/**
	 * Valid filter fields for query type
	 * @return
	 */
	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE};
	}

}
