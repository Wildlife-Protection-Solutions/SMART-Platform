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
package org.wcs.smart.patrol.json;

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
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
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
public class PatrolAttributeMetadata {
	
	public enum FixedPatrolMetadata{
		TRANSPORT_TYPE("transportType", Attribute.AttributeType.LIST), //$NON-NLS-1$
		ARMED("isArmed", Attribute.AttributeType.BOOLEAN), //$NON-NLS-1$
		TEAM("team", Attribute.AttributeType.LIST), //$NON-NLS-1$
		STATION("station", Attribute.AttributeType.LIST), //$NON-NLS-1$
		MANDATE("mandate", Attribute.AttributeType.LIST), //$NON-NLS-1$
		OBJECTIVE("objective", Attribute.AttributeType.TEXT), //$NON-NLS-1$
		COMMENT("comment", Attribute.AttributeType.TEXT), //$NON-NLS-1$
		EMPLOYEES("members", Attribute.AttributeType.LIST), //$NON-NLS-1$
		LEADER("leader", Attribute.AttributeType.LIST), //$NON-NLS-1$
		PILOT("pilot", Attribute.AttributeType.LIST), //$NON-NLS-1$
		PATROLID("patrolId", Attribute.AttributeType.TEXT); //$NON-NLS-1$
		
		String key;
		Attribute.AttributeType type;
		
		FixedPatrolMetadata(String key, Attribute.AttributeType type){
			this.key = key;
			this.type = type;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public Attribute.AttributeType getType(){
			return this.type;
		}
		
		public PatrolAttributeMetadata toMetadata(Session session, ConservationArea ca) {
			PatrolAttributeMetadata item = new PatrolAttributeMetadata(key, type);
			if (this == TRANSPORT_TYPE || this == MANDATE ||
				this == EMPLOYEES || this == LEADER) {
				item.setRequired(true);
			}else {
				item.setRequired(false);
			}

			HashMap<Locale, String> names = SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getNames(this);
			for (Entry<Locale, String> name : names.entrySet()) {
				item.addName(item.new Name(name.getValue(), name.getKey().toString()));
			}
			
			List<? extends NamedKeyItem> kids = null;
			if (this == TRANSPORT_TYPE) {
				kids = QueryFactory.buildQuery(session, PatrolTransportType.class, new Object[] {"conservationArea",ca}).list(); //$NON-NLS-1$
			}else if (this == TEAM) {
				kids = QueryFactory.buildQuery(session, Team.class, new Object[] {"conservationArea",ca}).list(); //$NON-NLS-1$
			}else if (this == MANDATE) {
				kids = QueryFactory.buildQuery(session, PatrolMandate.class, new Object[] {"conservationArea",ca}).list(); //$NON-NLS-1$
			}
			
			if (kids != null) {
				for (NamedKeyItem kid: kids) {
					PatrolAttributeMetadata.ListOption op = item.new ListOption(kid.getKeyId());
					for (Label l : kid.getNames()) {
						op.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
					}
					item.addListOption(op);
				}
			}
			
			if (this == STATION) {
				List<Station> stations = QueryFactory.buildQuery(session, Station.class, new Object[] {"conservationArea",ca}).list(); //$NON-NLS-1$
				for (Station station: stations) {
					PatrolAttributeMetadata.ListOption op = item.new ListOption(UuidUtils.uuidToString(station.getUuid()));
					for (Label l : station.getNames()) {
						op.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
					}
					item.addListOption(op);
				}
			}
			
			if (this == EMPLOYEES) {
				List<Employee> employees = 
						session.createQuery("FROM Employee WHERE conservationArea = :ca and endEmploymentDate is null", Employee.class) //$NON-NLS-1$
						.setParameter("ca",ca) //$NON-NLS-1$
						.list();
				for (Employee e : employees) {
					PatrolAttributeMetadata.ListOption op = item.new ListOption(UuidUtils.uuidToString(e.getUuid()));
					String name = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(e, Locale.getDefault());
					op.addName(item.new Name(name, null));
					item.addListOption(op);
				}
						
			}
			
			if (this == PILOT || this == LEADER) {
				item.options = null;
				item.setLinkTo(FixedPatrolMetadata.EMPLOYEES.key);
			}
			
			if (this == PILOT) {
				
				List<PatrolTransportType> types = QueryFactory.buildQuery(session, PatrolTransportType.class, new Object[] {"conservationArea",ca}).list(); //$NON-NLS-1$
				StringBuilder sb = new StringBuilder();
				for (PatrolTransportType type : types) {
					if (type.getPatrolType().requiresPilot()) {
						if (sb.length() > 0) sb.append(" OR "); //$NON-NLS-1$
						sb.append(FixedPatrolMetadata.TRANSPORT_TYPE.key);
						sb.append(" = '"); //$NON-NLS-1$
						sb.append(type.getKeyId());
						sb.append("'"); //$NON-NLS-1$
					}
				}
				item.setRequiredExpression(sb.toString());
			}
			
			return item;
		}
	}
	
	public enum PatrolWaypointMetadata{
		DISTANCE(IJsonFeatureProcessor.WaypointMetadata.DISTANCE.getKey(), Attribute.AttributeType.NUMERIC), 
		BEARING(IJsonFeatureProcessor.WaypointMetadata.BEARING.getKey(), Attribute.AttributeType.NUMERIC),
		COMMENT(IJsonFeatureProcessor.WaypointMetadata.COMMENT.getKey(), Attribute.AttributeType.TEXT);
		
		String key;
		Attribute.AttributeType type;
		
		PatrolWaypointMetadata(String key, Attribute.AttributeType type){
			this.key = key;
			this.type = type;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public Attribute.AttributeType getType(){
			return this.type;
		}
		
		/**
		 * May return null if the metadata option is not valid for
		 * the current Conservation Area settings.
		 * 
		 * @param session
		 * @param ca
		 * @return
		 */
		public PatrolAttributeMetadata toMetadata(Session session, ConservationArea ca) {
			
			if (this == BEARING || this == DISTANCE) {
				//see what the field data settings are
				ObservationOptions op = session.get(ObservationOptions.class, ca.getUuid());
				if (op == null || !op.getTrackDistanceDirection()) return null;
			}
			
			PatrolAttributeMetadata item = new PatrolAttributeMetadata(key, type);
			item.setRequired(false);

			HashMap<Locale, String> names = SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getNames(this);
			for (Entry<Locale, String> name : names.entrySet()) {
				item.addName(item.new Name(name.getValue(), name.getKey().toString()));
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
	
	public PatrolAttributeMetadata(String id, Attribute.AttributeType type) {
		this.id = id;
		names = new ArrayList<>();
		this.type = type;
		if (type == Attribute.AttributeType.LIST) {
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
	
	public static PatrolAttributeMetadata toMetadata(PatrolAttribute attribute) {
		PatrolAttributeMetadata item = new PatrolAttributeMetadata(attribute.getKeyId(), attribute.getType());
		item.setRequired(false);
		for (Label l : attribute.getNames()) {
			item.addName(item.new Name(l.getValue(), l.getLanguage().getCode()));
		}
		
		if (attribute.getType() == Attribute.AttributeType.LIST) {
			
			for (NamedKeyItem kid: attribute.getAttributeList()) {
				PatrolAttributeMetadata.ListOption op = item.new ListOption(kid.getKeyId());
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
	public static PatrolAttributeMetadata getSignatureMetadata(Session session, ConservationArea ca) {
		PatrolAttributeMetadata item = new PatrolAttributeMetadata(IJsonFeatureProcessor.JSON_SIGNATURETYPE_KEY, null);
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
}
