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

import java.nio.file.Path;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;

/**
 * R Script object 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.r_script")
public class RScript extends NamedItem {

	public static final String KEY = "rscript"; //$NON-NLS-1$
	
	public static final int MAX_DEFAULT_PARAM_SIZE = 32672;
	
	private String filename = null;
	private ConservationArea ca;
	private String defaultParameters = null;
	private Employee creator = null;
	private Path importfile;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
	public Employee getCreator() {
		return this.creator;
	}
	
	public void setCreator(Employee creator) {
		this.creator = creator;
	}
		
	@Column(name="filename")
	public String getFilename() {
		return this.filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	
	@Column(name="default_parameters")
	public String getDefaultParameters() {
		return this.defaultParameters;
	}
	
	public void setDefaultParameters(String defaultparameters) {
		this.defaultParameters = defaultparameters;
	}

	
	/**
	 * The import file for the script is the source script file
	 * the user provided for import. This should be null unless
	 * we are creating a new script
	 * 
	 * @return
	 */
	@Transient
	public Path getImportFile() {
		return this.importfile;
	}
	@Transient
	public void setImportFile(Path path) {
		this.importfile = path;
	}
}
