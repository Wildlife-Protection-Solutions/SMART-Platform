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
import java.util.HashMap;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

	private static final String SSH_CERTIFICATE_FILENAME = "certificate.crt"; //$NON-NLS-1$
	/*
	 * server connection options
	 */
	public enum Option{
		MAX_RETRY_DOWNLOAD (10),
		MAX_RETRY_UPLOAD(100),
		RETY_WAIT_TIME(500),
		MAX_PROCESSING_WAIT_TIME(5 * 60 * 1000l);
		
		Object defaultValue;
		
		private Option(Object defaultValue){
			this.defaultValue = defaultValue;
		}
		
		public String getDefaultValueAsString(){
			return this.defaultValue.toString();
		}
	}
	
	private ConservationArea ca;
	private String serverUrl;
	private String serverOptions;
	private HashMap<Option, String> options;
	private String certificate;
	
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

	@Column(name="options")
	public String getServerOptions(){
		return serverOptions;
	}

	public void setServerOptions(String options) {
		this.serverOptions = options;
	}

	@Column(name="certificate")
	public String getCertificateFileName(){
		return this.certificate;
	}
	public void setCertificateFileName(String certificate){
		this.certificate = certificate;
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
		
		Path copyToFile = Paths.get(getConservationArea().getFileDataStoreLocation(),
				ConnectSyncHistoryRecord.CONNECT_FILESTORE_DIR, SSH_CERTIFICATE_FILENAME);
		
		Path copyFrom = newFile;

		Files.createDirectories(copyToFile.getParent());
		if (Files.exists(copyToFile)){
			//old certificate; delete me
			Files.delete(copyToFile);
		}
		Files.copy(copyFrom, copyToFile);
		setCertificateFileName(Paths.get(getConservationArea().getFileDataStoreLocation()).relativize(copyToFile).toString());
	}
	@Transient
	public void setOption(Option op, String value){
		if (options == null) parseOptions();
		options.put(op, value);
		setServerOptions(encodeOptions());
	}
	
	@Transient
	public String getOption(Option op){
		if (options == null) parseOptions();
		return options.get(op);
	}
	@Transient
	private String encodeOptions(){
		StringBuilder str = new StringBuilder();
		for (Entry<Option,String> item : options.entrySet()){
			str.append(item.getKey().name() + "=" + item.getValue() + "|"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return str.toString();
	}
	
	@Transient
	private void parseOptions(){
		options = new HashMap<ConnectServer.Option, String>();
		if (getServerOptions() == null) return;
		String[] parts = getServerOptions().split("\\|"); //$NON-NLS-1$
		for(String part : parts){
			String[] bits = part.split("="); //$NON-NLS-1$
			for (Option o : Option.values()){
				if (o.name().equals(bits[0])){
					options.put(o, bits[1]);
					break;
				}
			}
		}
	}
	
	@Transient
	public void initalizeOptions(){
		for (Option p : Option.values()){
			setOption(p, p.defaultValue.toString());
		}
	}
	/**
	 * Number of times to retry downloading a file
	 * before failing. 
	 */
	@Transient
	public int getMaxRetryDownload(){
		String x = getOption(Option.MAX_RETRY_DOWNLOAD);
		if (x == null) return (Integer)Option.MAX_RETRY_DOWNLOAD.defaultValue;
		return Integer.valueOf(x);
	}
	/**
	 * Number of time to retry uploading a file
	 * before failing.
	 */
	@Transient
	public int getMaxRetryUpload(){
		String x = getOption(Option.MAX_RETRY_UPLOAD);
		if (x == null) return (Integer)Option.MAX_RETRY_UPLOAD.defaultValue;
		return Integer.valueOf(x);
	}
	/**
	 * Initial time to wait between retrys in milliseconds.
	 * 
	 * Second and third wait times should double previous value.
	 */
	@Transient
	public int getRetryWaitTime(){
		String x = getOption(Option.RETY_WAIT_TIME);
		if (x == null) return (Integer)Option.RETY_WAIT_TIME.defaultValue;
		return Integer.valueOf(x);
	}
	/**
	 * Maximum wait time for processing item in milliseconds.
	 */
	@Transient
	public long getWaitProcessingTime(){
		String x = getOption(Option.MAX_PROCESSING_WAIT_TIME);
		if (x == null) return (Long)Option.MAX_PROCESSING_WAIT_TIME.defaultValue;
		return Long.valueOf(x);
	}
}
