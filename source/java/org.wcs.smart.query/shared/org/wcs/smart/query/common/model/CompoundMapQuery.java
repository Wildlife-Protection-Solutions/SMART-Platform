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
package org.wcs.smart.query.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
@Entity
@Table(name="smart.compound_query")
public class CompoundMapQuery extends Query{

	public static final String TYPE_KEY = "compound"; //$NON-NLS-1$
	
	private DateFilter dateFilter;
	private List<CompoundMapQueryLayer> queries;
	
	@OneToMany(cascade = {CascadeType.ALL}, mappedBy="mapQuery")
	public List<CompoundMapQueryLayer> getLayers(){
		return queries;
	}
	
	public void setLayers(List<CompoundMapQueryLayer> layers){
		this.queries = layers;
	}
	
	@Transient
	@Override
	public String getTypeKey() {
		return TYPE_KEY;
	}

	@Transient
	@Override
	public boolean isDefinitionEqual(Query other) {
		if (other instanceof CompoundMapQuery){
			CompoundMapQuery mq = (CompoundMapQuery)other;
			if (queries.size() == mq.getLayers().size()){
				for (CompoundMapQueryLayer layer : queries){
					boolean found = false;
					for (CompoundMapQueryLayer layer2 : mq.getLayers()){
						if (layer.areequals(layer2)){
							found = true;
							break;
						}
					}
					if (!found){
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	@Transient
	@Override
	public void copyQuery(Query copy) {
		assert copy instanceof CompoundMapQuery;
		
		CompoundMapQuery q = (CompoundMapQuery)copy;
		if (q.getLayers() == null) q.setLayers(new ArrayList<CompoundMapQueryLayer>());
		
		for (CompoundMapQueryLayer l : getLayers()){
			CompoundMapQueryLayer newLayer = new CompoundMapQueryLayer();
			newLayer.setMapQuery(q);
			newLayer.setQueryStyle(l.getQueryStyle());
			newLayer.setQueryUuid(l.getQueryUuid());
			newLayer.setQueryType(l.getQueryType());
			q.getLayers().add(newLayer);
		}
		setConservationAreaFilter(q.getConservationAreaFilter());
	}

	@Transient
	@Override
	public Query clone(Employee newOwner) {
		CompoundMapQuery clone = new CompoundMapQuery();
		clone.setConservationArea(getConservationArea());
		clone.setConservationAreaFilter(getConservationAreaFilter());
		clone.setDateFilter(dateFilter);
		clone.setFolder(getFolder());
		clone.setIsShared(getIsShared());
		clone.setOwner(newOwner);
		clone.setName(getName());
		if (getLayers() != null){
			clone.setLayers(new ArrayList<CompoundMapQueryLayer>());
			for (CompoundMapQueryLayer oldlayer : getLayers()){
				CompoundMapQueryLayer newlayer = new CompoundMapQueryLayer();
				newlayer.setMapQuery(clone);
				newlayer.setQueryUuid(oldlayer.getQueryUuid());
				newlayer.setQueryType(oldlayer.getQueryType());
				newlayer.setQueryStyle(oldlayer.getQueryStyle());
				clone.getLayers().add(newlayer);
			}
		}
		return clone;
	}

	@Transient
	@Override
	public void setDateFilter(DateFilter filter) {
		this.dateFilter = filter;
	}

}
