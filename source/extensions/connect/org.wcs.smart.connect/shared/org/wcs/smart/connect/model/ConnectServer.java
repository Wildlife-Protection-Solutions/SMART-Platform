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
package org.wcs.smart.connect.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Smart connect server information
 * @author Emily
 *
 */
@Entity
@Table(name="smart.connect_server")
public class ConnectServer extends UuidItem{

	/**
	 * The certificate file path for any connect server if it is defined.
	 * @param ca the conservation area
	 * @return certificate file path
	 */
	public static final Path getDefaultCertificateFileName(ConservationArea ca){
		return Paths.get(ca.getFileDataStoreLocation(),
				ConnectSyncHistoryRecord.CONNECT_FILESTORE_DIR, SSH_CERTIFICATE_FILENAME);
	}
	
	private static final String SSH_CERTIFICATE_FILENAME = "certificate.crt"; //$NON-NLS-1$
	
	private ConservationArea ca;
	private String serverUrl;
	private String certificate;
	private Map<String, ConnectServerOption> options;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	@Column(name="url")
	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Column(name="certificate")
	public String getCertificateFileName(){
		return this.certificate;
	}
	/**
	 * relative file location of the certificate file in the filestore
	 * @param certificate
	 */
	public void setCertificateFileName(String certificate){
		this.certificate = certificate;
	}
	
	
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy="id.server")
	@MapKey(name="id.optionKey")
	public Map<String, ConnectServerOption> getOptions(){
		return this.options;
	}
	
	public void setOptions(Map<String, ConnectServerOption> options){
		this.options = options;
	}
	
	/**
	 * Copies the certificate file into the filestore, replacing any
	 * existing certificate files; then updates the files name
	 * 
	 * @param newFile the new file location or null if we should delete
	 * the existing certificate
	 */
	@Transient
	public void setCertificateFile(Path newFile) throws Exception{
		if (getCertificateFileName() != null){
			//delete existing file
			Path p = Paths.get(getConservationArea().getFileDataStoreLocation(), getCertificateFileName());
			Files.deleteIfExists(p);
		}
		
		if (newFile == null){
			setCertificateFileName(null);
			return;
		}
		
		Path copyToFile =getDefaultCertificateFileName(getConservationArea());		
		Path copyFrom = newFile;

		Files.createDirectories(copyToFile.getParent());
		if (Files.exists(copyToFile)){
			//old certificate; delete me
			Files.delete(copyToFile);
		}
		Files.copy(copyFrom, copyToFile);
		setCertificateFileName(Paths.get(getConservationArea().getFileDataStoreLocation()).relativize(copyToFile).toString());
	}
	
	/**
	 * 
	 * @return the full absolute path to the certificate file in 
	 * the local filestore
	 */
	@Transient
	public Path getLocalCertificateFile(){
		return Paths.get(ca.getFileDataStoreLocation(),
				getCertificateFileName());
	}	
	
	/**
	 * Gets the option represented by the option key as a 
	 * integer value.  Will return null if option not found.
	 * @param optionKey
	 * @return
	 */
	@Transient
	public Integer getOptionAsInt(String optionKey){
		if (options == null || options.get(optionKey) == null 
				|| options.get(optionKey).getValue() == null){
			return null;
		}
		String x = options.get(optionKey).getValue();
		return Integer.valueOf(x);
	}
	
	
	/**
	 * Gets the option represented by the option key as a 
	 * boolean value.  Will return null if option not found.
	 * @param optionKey
	 * @return
	 */
	@Transient
	public Boolean getOptionAsBoolean(String optionKey){
		if (options == null || options.get(optionKey) == null 
				|| options.get(optionKey).getValue() == null){
			return null;	
		}
		String x = options.get(optionKey).getValue();
		return Boolean.valueOf(x);
	}
	
	@Transient
	public void setOption(String optionKey, String value){
		ConnectServerOption op = options.get(optionKey);
		if (op == null){
			op = new ConnectServerOption();
			op.setServer(this);
			op.setOptionKey(optionKey);
			options.put(optionKey, op);	
		}
		op.setValue(value);
		
	}
	
}
