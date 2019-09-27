/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.UuidItem;

/**
 * A cybertracker navigation layer.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.ct_navigation_layer")
public class NavigationLayer extends UuidItem {

	private static final String JSON_FEATURES_KEY = "features"; //$NON-NLS-1$
	
	protected ConservationArea ca;
	protected String name;
	protected byte[] targets; // geojson string

	private LocalDate createdDate;
	private LocalDate lastModifiedDate;
	private Employee lastModifiedBy;

	private List<NavigationTarget> ntargets = null;
	
	@Column(name = "name")
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Transient
	public List<NavigationTarget> getTargetsAsJson() {
		if (ntargets != null) return ntargets;
		
		byte[] trgs = getTargets();
		if (trgs == null) return new ArrayList<>();
		
		ntargets = new ArrayList<>();
		String v = new String(trgs, StandardCharsets.UTF_8);
		JSONObject features = null;
		try {
			features = (JSONObject) (new JSONParser()).parse(v);
		}catch (ParseException ex) {
			//TODO;
		}
		if (features == null) return ntargets;
		
		JSONArray items = (JSONArray) features.get(JSON_FEATURES_KEY); 
		for(int i = 0; i < items.size(); i ++) {
			NavigationTarget t = NavigationTarget.parse((JSONObject)items.get(i));
			if (t != null) ntargets.add(t);
		}
		return Collections.unmodifiableList( ntargets );
	}

	@SuppressWarnings("unchecked")
	public void setTargets(List<NavigationTarget> targets) {
		this.ntargets = targets;
		
		JSONObject json = new JSONObject();
		json.put("type", "FeatureCollection"); //$NON-NLS-1$ //$NON-NLS-2$
		JSONArray features = new JSONArray();
		for (NavigationTarget t : targets) {
			features.add(t.toJson());
		}
		json.put(JSON_FEATURES_KEY, features);
		setTargets(json.toJSONString().getBytes(StandardCharsets.UTF_8));
	}
	
	
	@Column(name = "targets")
	public byte[] getTargets() {
		return this.targets;
	}

	public void setTargets(byte[] targets) {
		this.targets = targets;
	}

	/**
	 * 
	 * @return last_modified_by
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ca_uuid", referencedColumnName = "uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}

	/**
	 * Sets the Conservation Area.
	 * 
	 * @param ca
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	/**
	 * 
	 * @return the created date
	 */
	@Column(name = "created_date")
	public LocalDate getCreatedDate() {
		return createdDate;
	}

	/**
	 * @param the created date time
	 */
	public void setCreatedDate(LocalDate createdDate) {
		this.createdDate = createdDate;
	}

	@Column(name = "last_modified_date")
	public LocalDate getLastModifiedDate() {
		return lastModifiedDate;
	}

	/**
	 * @param the last modified date time
	 */
	public void setLastModifiedDate(LocalDate modifiedDateTime) {
		this.lastModifiedDate = modifiedDateTime;
	}

	/**
	 * Get the last modified by.
	 * 
	 * @return last_modified_by
	 */

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modified_by", referencedColumnName = "uuid")
	public Employee getLastModifiedBy() {
		return this.lastModifiedBy;
	}

	/**
	 * Set the created_by.
	 * 
	 * @param createdBy created_by
	 */
	public void setLastModifiedBy(Employee lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

}
