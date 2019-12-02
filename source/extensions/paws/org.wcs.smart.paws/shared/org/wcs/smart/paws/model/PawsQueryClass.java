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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.query.model.Query;

@Entity
@Table(name="smart.paws_query_class")
public class PawsQueryClass extends AbstractPawsClass{

	private String querytype;
	private UUID queryuuid;
	
	private Query cachedQuery;
	
	@Column(name="query_type")
	public String getQueryType() {
		return this.querytype;
	}
	
	public void setQueryType(String querytype) {
		this.querytype = querytype;
	}
	
	@Column(name="query_uuid")
	public UUID getQueryUuid() {
		return this.queryuuid;
	}
	
	public void setQueryUuid(UUID queryuuid) {
		this.queryuuid = queryuuid;
	}
	
	@Transient
	public Query getCachedQuery() {
		return this.cachedQuery;
	}
	@Transient
	public void setCachedQuery(Query cachedQuery) {
		this.cachedQuery = cachedQuery;
	}
}

