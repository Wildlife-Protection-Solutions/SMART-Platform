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
package org.wcs.smart.ca.datamodel;

import java.util.Locale;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Data model value attribute aggregation options 
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.dm_aggregation")

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Aggregation {

	/**
	 * short name of aggregation used as key
	 */
	private String name = ""; //$NON-NLS-1$
	
	@Transient
	private String guiName = null;
	
	/**
	 * Creates new aggregation
	 */
	public Aggregation(){

	}
	
	/**
	 * 
	 * @return the name for this aggregation
	 */
	@Id
	public String getName() {
		return name;
	}

	/**
	 * Sets the name for the aggregation.  Aggregations
	 * name should never be changed.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	@Transient
	public synchronized String getGuiName(){
		if (guiName != null){
			return guiName;
		}
		
		Job j = new Job(""){ //$NON-NLS-1$
			public IStatus run(IProgressMonitor monitor){
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					AggregationLabel current = (AggregationLabel)s.get(AggregationLabel.class, new AggregationLabel.AggregationLabelPk(Aggregation.this.getName(),SmartUtils.localeToString(Locale.getDefault())));
					if (current == null){
						//look for just language match
						current = (AggregationLabel)s.get(AggregationLabel.class, new AggregationLabel.AggregationLabelPk(Aggregation.this.getName(),Locale.getDefault().getLanguage()));
					}
					if (current != null){
						guiName = current.getGuiName();
					}	
				}finally{
					if (s.getTransaction().isActive()){
						s.getTransaction().commit();
					}
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException ex) {
			SmartPlugIn.log("Error loading aggregation name", ex); //$NON-NLS-1$
		}
		
		if (guiName == null){
			guiName = name;
		}
		return guiName;
		
		
	}
	
	/**
	 * <p> Two Aggregation objects are considered the same
	 * object if they have the same name.
	 * </p>
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o){
		if (o == null ) return false;
		if (! (o instanceof Aggregation)) return false;
		if (name == null && ((Aggregation)o).name != null)return false;
		if (name != null && ((Aggregation)o).name == null)return false;
		return ((Aggregation)o).name.equals(name);
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode(){
		return name.hashCode();
	}
}
