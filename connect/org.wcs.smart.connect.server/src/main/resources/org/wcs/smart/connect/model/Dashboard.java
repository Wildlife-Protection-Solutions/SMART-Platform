/*
 * Copyright (C) 2015 Wildlife Conservation Society
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


import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A dashboard entity. A dashboard has two associated reports and default parameters for each of them. 
 *
 * @Author Jeff
 */
@Entity
@Table(name = "connect.dashboards")
public class Dashboard extends ConnectUuidItem{
	private String label;
	private UUID reportUuid1;
	private UUID reportUuid2;
	private int dateRange1;
	private int dateRange2;
	private String customDate1From;
	private String customDate1To;
	private String customDate2From;
	private String customDate2To;
	private String parameterList1; 
	private String parameterList2;
	
	@Column(name="label")
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	@Column(name="report_uuid_1")
	public UUID getReportUuid1() {
		return reportUuid1;
	}
	public void setReportUuid1(UUID reportUuid1) {
		this.reportUuid1 = reportUuid1;
	}
	
	
	@Column(name="report_uuid_2")
	public UUID getReportUuid2() {
		return reportUuid2;
	}
	public void setReportUuid2(UUID reportUuid2) {
		this.reportUuid2 = reportUuid2;
	}
	
	
	@Column(name="date_range1")
	public int getDateRange1() {
		return dateRange1;
	}
	public void setDateRange1(int dateRange1) {
		this.dateRange1 = dateRange1;
	}
	
	@Column(name="date_range2")
	public int getDateRange2() {
		return dateRange2;
	}
	public void setDateRange2(int dateRange2) {
		this.dateRange2 = dateRange2;
	}
	
	@Column(name="custom_date1_from")
	public String getCustomDate1From() {
		return customDate1From;
	}
	public void setCustomDate1From(String customDate1From) {
		this.customDate1From = customDate1From;
	}
	
	@Column(name="custom_date1_to")
	public String getCustomDate1To() {
		return customDate1To;
	}
	public void setCustomDate1To(String customDate1To) {
		this.customDate1To = customDate1To;
	}
	
	@Column(name="custom_date2_from")
	public String getCustomDate2From() {
		return customDate2From;
	}
	public void setCustomDate2From(String customDate2From) {
		this.customDate2From = customDate2From;
	}
	
	@Column(name="custom_date2_to")
	public String getCustomDate2To() {
		return customDate2To;
	}
	public void setCustomDate2To(String customDate2To) {
		this.customDate2To = customDate2To;
	}
	
	@Column(name="report_parameterlist_1")
	public String getParameterList1() {
		return parameterList1;
	}
	public void setParameterList1(String parameterList1) {
		this.parameterList1 = parameterList1;
	}
	

	@Column(name="report_parameterlist_2")
	public String getParameterList2() {
		return parameterList2;
	}
	public void setParameterList2(String parameterList2) {
		this.parameterList2 = parameterList2;
	}
	
}