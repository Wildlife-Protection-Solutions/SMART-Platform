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
package org.wcs.smart.i2.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramStyleOptions {
	
	private static final String NODE_DEFAULT = "nodeDefault"; //$NON-NLS-1$
	private static final String NODE_ROOT = "nodeRoot"; //$NON-NLS-1$
	private static final String EDGE_DEFAULT = "edgeDefault"; //$NON-NLS-1$
	
	private JsonObject json;
	
	public RelationshipDiagramStyleOptions(String options) {
		json = new Gson().fromJson(options, JsonObject.class);
	}

	protected JsonObject getJson() {
		return json;
	}

	public RelationshipDiagramNodeStyleOptions getDefaultNodeStyle() {
		JsonObject defaultNode = json.getAsJsonObject(NODE_DEFAULT);
		return defaultNode != null ? new RelationshipDiagramNodeStyleOptions(defaultNode) : null;
	}
	public void setDefaultNodeStyle(RelationshipDiagramNodeStyleOptions nodeOptions) {
		json.add(NODE_DEFAULT, nodeOptions.getJson());
	}

	public RelationshipDiagramNodeStyleOptions getRootNodeStyle() {
		JsonObject rootNode = json.getAsJsonObject(NODE_ROOT);
		return rootNode != null ? new RelationshipDiagramNodeStyleOptions(rootNode) : null;
	}
	public void setRootNodeStyle(RelationshipDiagramNodeStyleOptions nodeOptions) {
		if (nodeOptions != null) {
			json.add(NODE_ROOT, nodeOptions.getJson());
		} else {
			json.remove(NODE_ROOT);
		}
	}

	public RelationshipDiagramEdgeStyleOptions getDefaultEdgeStyle() {
		JsonObject defaultEdge = json.getAsJsonObject(EDGE_DEFAULT);
		return defaultEdge != null ? new RelationshipDiagramEdgeStyleOptions(defaultEdge) : null;
	}
	public void setDefaultEdgeStyle(RelationshipDiagramEdgeStyleOptions edgeOptions) {
		if (edgeOptions != null) {
			json.add(EDGE_DEFAULT, edgeOptions.getJson());
		} else {
			json.remove(EDGE_DEFAULT);
		}
	}
	
}
