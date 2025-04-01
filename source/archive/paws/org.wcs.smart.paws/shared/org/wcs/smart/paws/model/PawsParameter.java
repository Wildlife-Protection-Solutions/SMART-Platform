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
package org.wcs.smart.paws.model;

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="paws_parameter", schema="smart")
public class PawsParameter extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	public static final String AREA_PREFIX = "area:"; //$NON-NLS-1$
	public static final String FILE_PREFIX = "file:"; //$NON-NLS-1$
	
	public static enum FixedParameter{
		LYR_BOUNDARY,
		LYR_ELEVATION,
		LYR_LANDCOVER,
		LYR_OTHER,
		GRID_SIZE,
		TRAINING_RES,
		CLASSIFIER_MODEL,
		PTRANSPORT_FILTER,
		PMANDATE_FILTER
	}
	
	public static enum ClassifierModel{
		DECISION_TREE("decision_tree");//, //$NON-NLS-1$
		//RANDOM_FOREST("random_forest"), //$NON-NLS-1$
		//GAUSSIAN_PROCESS("gaussian_process"); //slow; not supported //$NON-NLS-1$
		
		public String key;
		
		private ClassifierModel(String key) {
			this.key = key;
		}
	}
	
	
	private PawsConfiguration config;
	
	private String key;
	private String value;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="config_uuid")
	public PawsConfiguration getConfiguration() {
		return this.config;
	}
	
	public void setConfiguration(PawsConfiguration config) {
		this.config = config;
	}
	
	@Column(name="keyid")
	public String getKey() {
		return this.key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	@Column(name="value")
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
