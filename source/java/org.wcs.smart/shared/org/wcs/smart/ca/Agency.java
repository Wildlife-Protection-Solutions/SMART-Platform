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
package org.wcs.smart.ca;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A class representing an employee agency
 * 
 * @author Emily
 *
 */
@Entity
@Table(name ="smart.agency")
public class Agency extends NamedItem{
	
	public static final Integer MAX_AGENCY_LENGTH = 128;
	
	private ConservationArea ca;
	private List<Rank> ranks;
	
	/**
	 * Creates a new agency
	 */
	public Agency(){
		super();
	}
	
	/**
	 * 
	 * @return the conservation area associated with agency
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	/**
	 * 
	 * sets the conservation area
	 */
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return the ranks associated with the given agency
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="agency", cascade={CascadeType.ALL}, orphanRemoval=true)
	public List<Rank> getRanks(){
		if (this.ranks == null){
			this.ranks = new ArrayList<Rank>();
		}
		return this.ranks;
	}
	/**
	 * 
	 * @param ranks
	 */
	public void setRanks(List<Rank> ranks){
		this.ranks = ranks;
	}
}
