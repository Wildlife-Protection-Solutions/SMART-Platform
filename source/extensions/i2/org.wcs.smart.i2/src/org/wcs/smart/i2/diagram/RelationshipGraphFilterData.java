/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.diagram;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;

/**
 * Data for filtering relationship diagram content.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphFilterData {
	
	private static final int DEFAULT_GRAPH_DEPTH = 2;
	
	private int depth = DEFAULT_GRAPH_DEPTH;
	private List<IntelEntityType> entityTypes = new ArrayList<>();
	private List<IntelRelationshipType> relationshipTypes = new ArrayList<>();
	
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public List<IntelEntityType> getEntityTypes() {
		return entityTypes;
	}
	public void setEntityTypes(List<IntelEntityType> entityTypes) {
		this.entityTypes = entityTypes;
	}
	
	public List<IntelRelationshipType> getRelationshipTypes() {
		return relationshipTypes;
	}
	public void setRelationshipTypes(List<IntelRelationshipType> relationshipTypes) {
		this.relationshipTypes = relationshipTypes;
	}
	
}
