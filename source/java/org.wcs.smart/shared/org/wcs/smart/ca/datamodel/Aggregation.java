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

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.wcs.smart.util.I18nUtil;


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
	
	/**
	 * Finds the gui name for a given aggregation.  Session
	 * must be provided.
	 * 
	 * @param agg
	 * @param session
	 * @param l
	 * @return
	 */
	public static synchronized String getGuiName(Aggregation agg, Session session, Locale l) {
		AggregationLabel current = (AggregationLabel) session.get(
				AggregationLabel.class,
				new AggregationLabel.AggregationLabelPk(agg
						.getName(),
						I18nUtil.localeToString(l)));
		if (current == null) {
			// look for just language match
			current = (AggregationLabel) session.get(
					AggregationLabel.class,
					new AggregationLabel.AggregationLabelPk(agg
							.getName(), l.getLanguage()));
		}
		String lbl = null;
		if (current == null) {
			lbl = agg.getName();
		}else{
			lbl = current.getGuiName();
		}
		return lbl;
	}
}
