/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.er.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Managing patrol metadata
 * 
 * @author Emily
 *
 */
@JsonInclude(Include.NON_NULL)
public class MissionAttributeMetadata {
	
	private static final String SU_KEY = "samplingunit"; //$NON-NLS-1$
	private static final String SD_PROPERTIES_KEY = "properties"; //$NON-NLS-1$
	
	public enum MissionTrackMetadata{
		SAMPLING_UNIT(SU_KEY, Attribute.AttributeType.LIST);
		
		String key;
		Attribute.AttributeType type;
		
		MissionTrackMetadata(String key, Attribute.AttributeType type){
			this.key = key;
			this.type = type;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public Attribute.AttributeType getType(){
			return this.type;
		}
		
		public MissionAttributeMetadata toMetadata(Session session, ConservationArea ca) {
			MissionAttributeMetadata item = new MissionAttributeMetadata(key, type);
			item.setRequired(false);
			item.setLinkTo(MissionMetadata.SURVEYDESIGN.getKey() + "." + SD_PROPERTIES_KEY +"." + MissionWaypointMetadata.SAMPLING_UNIT.getKey()); //$NON-NLS-1$ //$NON-NLS-2$
			item.options = null;
			HashMap<Locale, String> names = SmartContext.INSTANCE.getClass(IErLabelProvider.class).getNames(this);
			for (Entry<Locale, String> name : names.entrySet()) {
				item.addName(item.new Name(name.getValue(), name.getKey().toString()));
			}
			return item;
		}
	}
	
	
	public enum MissionWaypointMetadata{
		DISTANCE(IJsonFeatureProcessor.WaypointMetadata.DISTANCE.getKey(), Attribute.AttributeType.NUMERIC), 
		BEARING(IJsonFeatureProcessor.WaypointMetadata.BEARING.getKey(), Attribute.AttributeType.NUMERIC),
		COMMENT(IJsonFeatureProcessor.WaypointMetadata.COMMENT.getKey(), Attribute.AttributeType.TEXT),
		SAMPLING_UNIT(SU_KEY, Attribute.AttributeType.LIST);
		
		String key;
		Attribute.AttributeType type;
		
		MissionWaypointMetadata(String key, Attribute.AttributeType type){
			this.key = key;
			this.type = type;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public Attribute.AttributeType getType(){
			return this.type;
		}
		
		public MissionAttributeMetadata toMetadata(Session session, ConservationArea ca) {
			MissionAttributeMetadata item = new MissionAttributeMetadata(key, type);
			item.setRequired(false);

			if (this == SAMPLING_UNIT) {
				item.setLinkTo(MissionMetadata.SURVEYDESIGN.getKey() + "." + SD_PROPERTIES_KEY +"." + MissionWaypointMetadata.SAMPLING_UNIT.getKey()); //$NON-NLS-1$ //$NON-NLS-2$
				item.options = null;
			}
			if (this == DISTANCE || this == BEARING) {
				item.setRequiredExpression(MissionMetadata.SURVEYDESIGN.getKey() + "." + SD_PROPERTIES_KEY +"." + getKey() + " = true");  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
			}
			HashMap<Locale, String> names = SmartContext.INSTANCE.getClass(IErLabelProvider.class).getNames(this);
			for (Entry<Locale, String> name : names.entrySet()) {
				item.addName(item.new Name(name.getValue(), name.getKey().toString()));
			}
			return item;
		}
	}
	
	public enum MissionMetadata{
		COMMENT("comment", Attribute.AttributeType.TEXT), //$NON-NLS-1$
		EMPLOYEES("members", Attribute.AttributeType.LIST), //$NON-NLS-1$
		LEADER("leader", Attribute.AttributeType.LIST), //$NON-NLS-1$
		SURVEY("survey", Attribute.AttributeType.LIST), //$NON-NLS-1$
		SURVEYDESIGN("surveydesign", Attribute.AttributeType.LIST), //$NON-NLS-1$
		MISSIONID("missionId", Attribute.AttributeType.TEXT); //$NON-NLS-1$
		
		String key;
		Attribute.AttributeType type;
		
		MissionMetadata(String key, Attribute.AttributeType type){
			this.key = key;
			this.type = type;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public Attribute.AttributeType getType(){
			return this.type;
		}
		
		public MissionAttributeMetadata toMetadata(Session session, ConservationArea ca) {
			MissionAttributeMetadata item = new MissionAttributeMetadata(key, type);
			
			item.setRequired(false);
			
			if (this == SURVEY || this == SURVEYDESIGN) {
				item.options = null;
			}
			if (this == EMPLOYEES || this == LEADER) {
				item.setRequired(true);
			}
			
			HashMap<Locale, String> names = SmartContext.INSTANCE.getClass(IErLabelProvider.class).getNames(this);
			for (Entry<Locale, String> name : names.entrySet()) {
				item.addName(item.new Name(name.getValue(), name.getKey().toString()));
			}
			
			if (this == EMPLOYEES) {
				List<Employee> employees = 
						session.createQuery("FROM Employee WHERE conservationArea = :ca and endEmploymentDate is null", Employee.class) //$NON-NLS-1$
						.setParameter("ca",ca) //$NON-NLS-1$
						.list();
				for (Employee e : employees) {
					MissionAttributeMetadata.ListOption op = item.new ListOption(UuidUtils.uuidToString(e.getUuid()));
					String name = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(e, Locale.getDefault());
					op.addName(item.new Name(name, null));
					item.addListOption(op);
				}			
			}
			
			if (this == SURVEYDESIGN) {
				item.setLinkTo(MissionMetadata.SURVEYDESIGN.getKey());
				item.setRequiredExpression(SURVEY.getKey() + " = null"); //$NON-NLS-1$
			}
			
			if (this == LEADER) {
				item.options = null;
				item.setLinkTo(MissionMetadata.EMPLOYEES.key);
			}
			
			if (this == SURVEY) {
				item.setLinkTo(MissionMetadata.SURVEYDESIGN.getKey() + "." + SD_PROPERTIES_KEY +"." + MissionMetadata.SURVEY.getKey()); //$NON-NLS-1$ //$NON-NLS-2$
				item.options = null;
			}
			
			return item;
		}
	}
	
	private String id;
	private List<Name> names;
	private Attribute.AttributeType type;
	private List<ListOption> options;
	private String requiredWhen;
	private String linkTo;
	
	private HashMap<String, Object> properties;
	
	public MissionAttributeMetadata(String id, Attribute.AttributeType type) {
		this.id = id;
		names = new ArrayList<>();
		this.type = type;
		if (type != null && type == Attribute.AttributeType.LIST) {
			options = new ArrayList<>();
		}
		requiredWhen = Boolean.TRUE.toString();		
	}
	
	public void setLinkTo(String optionLink) {
		this.linkTo = optionLink;
	}
	public String getLinkTo() {
		return this.linkTo;
	}
	public String getId() {
		return this.id;
	}
	
	public HashMap<String, Object> getProperties(){
		return this.properties;
	}
	
	public List<Name> getNames(){
		return this.names;
	}
	
	public void addName(Name name) {
		this.names.add(name);
	}
	
	public String getType(){
		if (type == null) return null;
		return this.type.name();
	}
	public String getRequiredWhen() {
		return this.requiredWhen;
	}
	public List<ListOption> getListOptions(){
		return this.options;
	}
	
	public void setRequired(boolean isRequired) {
		if (isRequired) {
			this.requiredWhen = Boolean.TRUE.toString();
		}else {
			this.requiredWhen = Boolean.FALSE.toString();
		}
	}
	public void setRequiredExpression(String requiredWhen) {
		this.requiredWhen = requiredWhen;
	}
	
	public void addListOption(ListOption op) {
		this.options.add(op);
	}
	
	public class ListOption{
		private String id;
		private List<Name> names;
		
		public ListOption(String id) {
			this.id = id;
			this.names = new ArrayList<>();
		}
		
		public void addName(Name name) {
			this.names.add(name);
		}
		
		public String getId() {
			return this.id;
		}
		public List<Name> getNames(){
			return this.names;
		}
	}
	
	@JsonInclude(Include.NON_NULL)
	public class Name{
		private String name;
		private String locale;
		
		public Name(String name, String locale) {
			this.name = name;
			this.locale = locale;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String getLocale() {
			return this.locale;
		}
	}
	
	public static MissionAttributeMetadata toMetadata(MissionAttribute attribute) {
		MissionAttributeMetadata item = new MissionAttributeMetadata(attribute.getKeyId(), attribute.getType());
		item.setRequired(false);
		for (Label l : attribute.getNames()) {
			item.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
		}
		
		if (attribute.getType() == Attribute.AttributeType.LIST) {
			
			for (NamedKeyItem kid: attribute.getAttributeList()) {
				MissionAttributeMetadata.ListOption op = item.new ListOption(kid.getKeyId());
				for (Label l : kid.getNames()) {
					op.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
				}
				item.addListOption(op);
			}
			
		}
		return item;
	}
	
	/**
	 * will return null if no signature types are configured for ca
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public static MissionAttributeMetadata getSignatureMetadata(Session session, ConservationArea ca) {
		MissionAttributeMetadata item = new MissionAttributeMetadata(IJsonFeatureProcessor.JSON_SIGNATURETYPE_KEY, null);
		item.names = null;
		item.requiredWhen = null;
		item.options = new ArrayList<>();
		List<SignatureType> types = QueryFactory.buildQuery(session, SignatureType.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		if (types.isEmpty()) return null;
		for (SignatureType type : types) {
			ListOption op = item.new ListOption(type.getKeyId());
			
			for (Label l : type.getNames()) {
				op.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
			}
			item.addListOption(op);
		}
		return item;
	}
	
	
	public static List<MissionAttributeMetadata> surveyDesignToMetadata(Session session, ConservationArea ca) {
		List<MissionAttributeMetadata> items = new ArrayList<>();
		List<SurveyDesign> designs = 
				session.createQuery("FROM SurveyDesign WHERE conservationArea = :ca AND state = :state", SurveyDesign.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("state", SurveyDesign.State.ACTIVE) //$NON-NLS-1$
				.list();
		
		for (SurveyDesign d : designs) {
			MissionAttributeMetadata item = new MissionAttributeMetadata(d.getKeyId(),null);
			items.add(item);
			item.requiredWhen = null;
			item.properties = new HashMap<>();
			
			for (Label l : d.getNames()) {
				item.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
			}
			
			item.properties.put(MissionWaypointMetadata.BEARING.getKey(), d.getTrackDistanceDirection());
			item.properties.put(MissionWaypointMetadata.DISTANCE.getKey(), d.getTrackDistanceDirection());
			
			MissionAttributeMetadata suMetadata = new MissionAttributeMetadata(MissionWaypointMetadata.SAMPLING_UNIT.getKey(), AttributeType.LIST);
			List<SamplingUnit> units =
					session.createQuery("FROM SamplingUnit WHERE surveyDesign = :design", SamplingUnit.class) //$NON-NLS-1$
					.setParameter("design", d)							 //$NON-NLS-1$
					.list();
			
			if (!units.isEmpty()) {
				suMetadata.names = null;
				suMetadata.requiredWhen = null;
				for (SamplingUnit unit : units) {
					MissionAttributeMetadata.ListOption sop = item.new ListOption(UuidUtils.uuidToString(unit.getUuid()));
					sop.addName(item.new Name(unit.getId(), null));
					suMetadata.addListOption(sop);
				}
				item.properties.put(suMetadata.getId(), suMetadata);
			}
			
			MissionAttributeMetadata sMetadata = new MissionAttributeMetadata(MissionMetadata.SURVEY.getKey(), AttributeType.LIST);
			List<Survey> surveys = 
					session.createQuery("FROM Survey WHERE surveyDesign = :design", Survey.class) //$NON-NLS-1$
					.setParameter("design",d) //$NON-NLS-1$
					.list();
			if (!surveys.isEmpty()) {
				sMetadata.names = null;
				sMetadata.requiredWhen = null;
				for (Survey s : surveys) {
					MissionAttributeMetadata.ListOption sop = item.new ListOption(UuidUtils.uuidToString(s.getUuid()));
					sop.addName(item.new Name(s.getId(), null));
					sMetadata.addListOption(sop);
				}
				item.properties.put(sMetadata.getId(), sMetadata);
			}
			
		}
		return items;
		
	}
	
	
}
