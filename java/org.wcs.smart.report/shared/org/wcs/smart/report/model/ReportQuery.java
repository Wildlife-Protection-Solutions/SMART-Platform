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
package org.wcs.smart.report.model;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Mapping between reports and queries for tracking
 * which queries are used in which reports.
 * 
 * @author egouge
 * @since 1.0.0
 */
@Entity
@Table(name="smart.report_query")
@AssociationOverrides({
	@AssociationOverride(name = "id.report", 
		joinColumns = @JoinColumn(name = "report_uuid")),
	@AssociationOverride(name = "id.queryUuid", 
		joinColumns = @JoinColumn(name = "query_uuid")) })
public class ReportQuery {

	private ReportQueryPk id;
	
	/**
	 * Creates a new report
	 * query link
	 */
	public ReportQuery(){
		
	}
	
	public ReportQuery(Report report, UUID quuid){
		setId(new ReportQueryPk(report, quuid));
	}
	
	@EmbeddedId
	public ReportQueryPk getId(){
		return this.id;
	}
	
	public void setId(ReportQueryPk id){
		this.id = id;
	}
	
	@Transient
	public Report getReport(){
		return id.getReport();
	}
	
	@Transient
	public UUID getQueryUuid(){
		return id.getQueryUuid();
	}
	/**
	 * Primary key object for report/query association 
	 * 
	 */
	@Embeddable
	private static class ReportQueryPk implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private Report report;
		private UUID queryUuid;
		
		public ReportQueryPk(){
			
		}
		
		public ReportQueryPk(Report report, UUID queryUuid){
			setReport(report);
			setQueryUuid(queryUuid);
		}
		
		/**
		 * @return the report
		 */
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="report_uuid", referencedColumnName="uuid")
		public Report getReport() {
			return report;
		}
		
		/**
		 * @param report the report to set
		 */
		public void setReport(Report report) {
			this.report = report;
		}
		
		/**
		 * @return the queryUuid
		 */
		@Column(name="query_uuid")
		public UUID getQueryUuid() {
			return queryUuid;
		}
		
		/**
		 * @param queryUuid the queryUuid to set
		 */
		public void setQueryUuid(UUID queryUuid) {
			this.queryUuid = queryUuid;
		}
		
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getQueryUuid().hashCode();
			result = prime * result + ((report == null) ? 0 : report.hashCode());
			return result;
		}
		
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReportQueryPk other = (ReportQueryPk) obj;
			if (!getQueryUuid().equals(other.getQueryUuid()))
				return false;
			if (report == null) {
				if (other.report != null)
					return false;
			} else if (!report.equals(other.report))
				return false;
			return true;
		}
		
		
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ReportQuery && ((ReportQuery) obj).getId() != null){
			return this.id.equals(((ReportQuery) obj).getId());
		}
		return false;
	}
	
	
	
}
