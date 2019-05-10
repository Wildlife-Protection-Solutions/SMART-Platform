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

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

@Entity
@Table(name="smart.paws_run")
public class PawsRun extends UuidItem{

	public enum Status{
		COMPILING_DATA,
		UPLOADING_DATA,
		RUNNING,
		DOWNLOADING_RESULTS,
		COMPLETE,
		ERROR
	}
	private PawsConfiguration config;
	private ConservationArea ca;
	
	private String id;
	private String runid;
	
	private LocalDateTime runDate;
	
	private LocalDate dataStartDate;
	private LocalDate dataEndDate;
	
	private String packagefile;
	private String resultslocation;
	
	private Status status;
	private String statusmessage;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public PawsConfiguration getConfiguration() {
		return this.config;
	}
	
	public void setConfiguration(PawsConfiguration config) {
		this.config = config;
	}
	
	@Column(name="run_date")
	public LocalDateTime getRunDate() {
		return this.runDate;
	}
	
	public void setRunDate(LocalDateTime runDate) {
		this.runDate = runDate;
	}
	
	@Column(name="data_start_date")
	public LocalDate getDataStartDate() {
		return this.dataStartDate;
	}
	
	public void setDataStartDate(LocalDate dataStartDate) {
		this.dataStartDate = dataStartDate;
	}
	
	@Column(name="data_end_date")
	public LocalDate getDataEndDate() {
		return this.dataEndDate;
	}
	
	public void setDataEndDate(LocalDate dataEndDate) {
		this.dataEndDate = dataEndDate;
	}
	
	@Column(name="id")
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	@Column(name="server_run_id")
	public String getRunId() {
		return this.runid;
	}
	
	public void setRunId(String runid) {
		this.runid = runid;
	}
	
	@Column(name="package_file")
	public String getPackageFile() {
		return this.packagefile;
	}
	
	public void setPackageFile(String packagefile) {
		this.packagefile = packagefile;
	}
	
	@Column(name="result_location")
	public String getResultLocation() {
		return this.resultslocation;
	}
	
	public void setResultLocation(String resultslocation) {
		this.resultslocation = resultslocation;
	}
	
	@Column(name="status_message")
	public String getStatusMessage() {
		return this.statusmessage;
	}
	
	public void setStatusMessage(String statusmessage) {
		this.statusmessage = statusmessage;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus() {
		return this.status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
}
