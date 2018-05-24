package org.wcs.smart.r.model;

import java.nio.file.Path;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;

@Entity
@Table(name="smart.r_script")
public class RScript extends NamedItem {

	public static final String KEY = "rscript"; //$NON-NLS-1$
	
	public static final int MAX_DEFAULT_PARAM_SIZE = 32672;
	
	private List<RScriptParameter> parameters;
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
	
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="script", cascade={CascadeType.ALL}, orphanRemoval=true)
	public List<RScriptParameter> getParameters(){
		return this.parameters;
	}
	
	public void setParameters(List<RScriptParameter> parameters) {
		this.parameters = parameters;
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
