package org.wcs.smart.er.json;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.er.json.MissionAttributeMetadata.ListOption;
import org.wcs.smart.er.json.MissionAttributeMetadata.Name;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

public class SurveyDesignMetadata {

	public static final String SD_ID_KEY = "id"; //$NON-NLS-1$
	public static final String SU_KEY = "samplingUnits"; //$NON-NLS-1$
	public static final String SURVEY_KEY = "surveys"; //$NON-NLS-1$
	public static final String MISSIONMETADATA_KEY = "missionMetadata"; //$NON-NLS-1$
	
	
	private String id;
	private List<Name> names;
	private String configurableModel;
	
	private List<MissionAttributeMetadata> missionMetadata;
	private List<MissionAttributeMetadata> waypointMetadata;
	private List<MissionAttributeMetadata> trackMetadata;
	
	private List<ListOption> samplingUnits;
	private List<ListOption> surveys;
	private List<ListOption> signatures;
	
	public SurveyDesignMetadata(String id) {
		this.id = id;
		this.names = new ArrayList<>();
		this.missionMetadata = new ArrayList<>();
		this.waypointMetadata = new ArrayList<>();
		this.trackMetadata = new ArrayList<>();
		
		samplingUnits = null;
		surveys = null;
		
	}
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public List<Name> getNames(){
		return this.names;
	}
	
	public void addName(Name name) {
		this.names.add(name);
	}
	
	public List<MissionAttributeMetadata> getMissionMetadata(){
		return this.missionMetadata;
	}
	
	public List<MissionAttributeMetadata> getTrackMetadata(){
		return this.trackMetadata;
	}
	public List<MissionAttributeMetadata> getWaypointMetadata(){
		return this.waypointMetadata;
	}
	public List<ListOption> getSamplingUnits(){
		return this.samplingUnits;
	}
	public List<ListOption> getSurveys(){
		return this.surveys;
	}
	public List<ListOption> getSignatureTypes(){
		return this.signatures;
	}
	
	public String getConfigurableModel() {
		return this.configurableModel;
	}
	public static List<SurveyDesignMetadata> getMetadata(ConservationArea ca, Session session){
		List<SurveyDesign> designs = 
				session.createQuery("FROM SurveyDesign WHERE conservationArea = :ca AND state = :state", SurveyDesign.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("state", SurveyDesign.State.ACTIVE) //$NON-NLS-1$
				.list();
		
		List<SurveyDesignMetadata> items = new ArrayList<>();
		for (SurveyDesign d : designs) {
			
			SurveyDesignMetadata md = new SurveyDesignMetadata(d.getKeyId());
			items.add(md);
			for (Label l : d.getNames()) {
				md.names.add(new Name(l.getValue(), l.getLanguage().getCode()));
			}
			
			if (d.getConfigurableModel() != null) {
				md.configurableModel = UuidUtils.uuidToString(d.getConfigurableModel().getUuid());
			}
			
			List<SamplingUnit> units =
					session.createQuery("FROM SamplingUnit WHERE surveyDesign = :design", SamplingUnit.class) //$NON-NLS-1$
					.setParameter("design", d)							 //$NON-NLS-1$
					.list();
			
			md.waypointMetadata.add(MissionAttributeMetadata.MissionWaypointMetadata.COMMENT.toMetadata(session, ca));
			if (!units.isEmpty()) {
				md.waypointMetadata.add(MissionAttributeMetadata.MissionWaypointMetadata.SAMPLING_UNIT.toMetadata(session, ca));
				md.trackMetadata.add(MissionAttributeMetadata.MissionTrackMetadata.SAMPLING_UNIT.toMetadata(session, ca));
				
				md.samplingUnits = new ArrayList<>();
				for (SamplingUnit unit: units) {
					ListOption op = new ListOption( UuidUtils.uuidToString(unit.getUuid()));
					op.addName(new Name(unit.getId(), null));
					md.samplingUnits.add(op);
				}
			}
			if (d.getTrackDistanceDirection()) {
				md.waypointMetadata.add(MissionAttributeMetadata.MissionWaypointMetadata.DISTANCE.toMetadata(session, ca));
				md.waypointMetadata.add(MissionAttributeMetadata.MissionWaypointMetadata.BEARING.toMetadata(session, ca));
			}
			if (d.getTrackObserver()) {
				md.waypointMetadata.add(MissionAttributeMetadata.MissionWaypointMetadata.OBSERVER.toMetadata(session, ca));
			}
			
			for (MissionAttributeMetadata.MissionMetadata mmd : MissionAttributeMetadata.MissionMetadata.values()) {
				md.missionMetadata.add(mmd.toMetadata(session, ca));
			}
			
			//custom mission attributes
			for (MissionProperty p : d.getMissionProperties()) {
				md.missionMetadata.add(MissionAttributeMetadata.toMetadata(p));
			}
			
			
			List<Survey> surveys = 
					session.createQuery("FROM Survey WHERE surveyDesign = :design", Survey.class) //$NON-NLS-1$
					.setParameter("design",d) //$NON-NLS-1$
					.list();
			if (!surveys.isEmpty()) {
				md.surveys = new ArrayList<>();
			
				for (Survey survey : surveys) {
					ListOption op = new ListOption( UuidUtils.uuidToString(survey.getUuid()));
					op.addName(new Name(survey.getId(), null));
					md.surveys.add(op);
				}
			}
			
			md.signatures = getSignatureMetadata(session, ca);
		}
		
		return items;
	}
	
	
	/**
	 * will return null if no signature types are configured for ca
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	private static List<ListOption> getSignatureMetadata(Session session, ConservationArea ca) {
		
		List<SignatureType> types = QueryFactory.buildQuery(session, SignatureType.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		if (types.isEmpty()) return null;
		List<ListOption> items = new ArrayList<>();
		for (SignatureType type : types) {
			ListOption op = new ListOption(type.getKeyId());
			for (Label l : type.getNames()) {
				op.addName(new Name(l.getValue(), l.getLanguage().getCode()));
			}
			items.add(op);
		}
		return items;
	}
}
