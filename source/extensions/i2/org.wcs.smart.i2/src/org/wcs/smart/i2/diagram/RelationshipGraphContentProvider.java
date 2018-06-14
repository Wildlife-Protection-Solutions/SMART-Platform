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

import org.eclipse.gef.zest.fx.jface.IGraphContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.model.IntelEntityRelationship;

/**
 * Content provider for displaying relationship data as a graph.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class RelationshipGraphContentProvider implements IGraphContentProvider {
	
	private IRelationshipGraphData graphData;

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof IRelationshipGraphData) {
			graphData = (IRelationshipGraphData) newInput;
		} else if (newInput == null) {
			graphData = null;
		}
	}
	
	@Override
	public Object[] getNodes() {
		if (graphData != null ) {
			return graphData.getEntities();
		}
		return new Object[]{};
	}
	
	@Override
	public Object[] getAdjacentNodes(Object node) {
		if (graphData != null) {
			return graphData.getTargets(node);
		}
		return null;
	}

	@Override
	public boolean hasNestedGraph(Object node) {
		return false;
	}

	@Override
	public Object[] getNestedGraphNodes(Object node) {
		return null;
	}

	public boolean isRootNode(Object node) {
		if (graphData != null) {
			return graphData.isRootNode(node);
		}
		return false;
	}

	public IntelEntityRelationship getRelationship(Object source, Object target) {
		if (graphData != null) {
			return graphData.getRelationship(source, target);
		}
		return null;
	}
}
