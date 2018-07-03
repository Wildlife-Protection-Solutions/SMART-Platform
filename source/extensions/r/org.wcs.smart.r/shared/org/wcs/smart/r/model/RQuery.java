package org.wcs.smart.r.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.r.engine.QueryConfiguration;
import org.wcs.smart.util.UuidUtils;

@Entity
@Table(name="smart.r_query")
public class RQuery extends NamedItem{

	/**
	 * JSON Keys for serializing queries parameters for rquery
	 */
	public static final String QUERY_JSONKEY = "q"; //$NON-NLS-1$
	public static final String QDATE_JSON_KEY = "d"; //$NON-NLS-1$
	public static final String QEXPORT_JSONKEY = "e"; //$NON-NLS-1$
	public static final String QUUID_JSONKEY = "u"; //$NON-NLS-1$
	public static final String QTYPE_JSONKEY = "t"; //$NON-NLS-1$
	public static final String PARAM_JSONKEY = "p"; //$NON-NLS-1$
	
	private RScript script;
	private String configuration;
	private ConservationArea ca;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="config")
	public String getConfiguration() {
		return this.configuration;
	}
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="script_uuid", referencedColumnName="uuid")
	public RScript getScript() {
		return this.script;
	}
	
	public void setScript(RScript script) {
		this.script = script;
	}

	
	/**
	 * Converts to configuration string to be stored with query
	 * 
	 * @param param
	 * @param queries
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Transient
	public static String toConfigurationString(String param, List<QueryConfiguration> queries) {
		JSONObject items = new JSONObject();
		items.put(RQuery.PARAM_JSONKEY, param);
		
		JSONArray array = new JSONArray();
		for (QueryConfiguration cc : queries) {
			JSONObject jquery = new JSONObject();
			
			jquery.put(QTYPE_JSONKEY, cc.getQuery().getTypeKey());
			jquery.put(QUUID_JSONKEY, UuidUtils.uuidToString(cc.getQuery().getUuid()));
			jquery.put(QEXPORT_JSONKEY, cc.getQueryExporter().getId());
			jquery.put(QDATE_JSON_KEY, cc.getDateFilter().asString());
			
			array.add(jquery);
		}
		items.put(QUERY_JSONKEY,array);
		return items.toJSONString();
	}
}
