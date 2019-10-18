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
package org.wcs.smart.r.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;

/**
 * R Query object
 * @author Emily
 *
 */
@Entity
@Table(name="smart.r_query")
public class RQuery extends NamedItem{

	private static final long serialVersionUID = 1L;
	
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

}
