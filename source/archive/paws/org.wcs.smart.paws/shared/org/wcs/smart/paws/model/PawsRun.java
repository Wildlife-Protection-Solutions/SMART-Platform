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
import java.time.Month;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="paws_run", schema="smart")
public class PawsRun extends UuidItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public enum Status{
		COMPILING_DATA,
		UPLOADING_DATA,
		RUNNING,
		DOWNLOADING_RESULTS,
		COMPLETE,
		ERROR,
		AUTH_TIMEOUT;
		
		public boolean isRunning() {
			return this == COMPILING_DATA ||
					this == UPLOADING_DATA ||
					this == RUNNING ||
					this == DOWNLOADING_RESULTS;
		}
	}
	private PawsConfiguration config;
	private ConservationArea ca;
	
	private String id;
	private String runid;
	
	private String taskId;
	
	private LocalDateTime runDate;
	
	private String packagefile;
	private String resultslocation;
	
	private Status status;
	private String statusmessage;
	private String serverjson;
	
	private int trainstartyear;
	private int trainendyear;
	private int forecaststartyear;
	private int forecastendyear;

	private String containerName;
	
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
	
	@Column(name="container")
	public String getContainerName() {
		return this.containerName;
	}
	
	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}
	
	
	@Column(name="run_date")
	public LocalDateTime getRunDate() {
		return this.runDate;
	}
	
	public void setRunDate(LocalDateTime runDate) {
		this.runDate = runDate;
	}
	
	@Column(name="train_start_year")
	public int getTrainStartYear() {
		return this.trainstartyear;
	}
	
	public void setTrainStartYear(int start) {
		this.trainstartyear = start;
	}
	
	@Column(name="train_end_year")
	public int getTrainEndYear() {
		return this.trainendyear;
	}
	
	public void setTrainEndYear(int end) {
		this.trainendyear = end;
	}
	
	@Column(name="forecast_start_year")
	public int getForecastStartYear() {
		return this.forecaststartyear;
	}
	
	public void setForecastStartYear(int start) {
		this.forecaststartyear = start;
	}
	
	@Column(name="forecast_end_year")
	public int getForecastEndYear() {
		return this.forecastendyear;
	}
	
	public void setForecastEndYear(int end) {
		this.forecastendyear = end;
	}
	
	@Transient
	public LocalDate getDataStartDate() {
		return LocalDate.of(getTrainStartYear(), Month.JANUARY, 1);
	}
	@Transient
	public LocalDate getDataEndDate() {
		return LocalDate.of(getTrainEndYear(), Month.DECEMBER, 31);
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
	
	@Column(name="server_status_json")
	public String getServerStatusJson() {
		return this.serverjson;
	}
	
	public void setServerStatusJson(String serverjson) {
		this.serverjson = serverjson;
	}
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	public Status getStatus() {
		return this.status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	@Column(name="paws_task_id")
	public String getTaskId() {
		return this.taskId;
	}
	public void setTaskId(String id) {
		this.taskId = id;
	}
}
