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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.zest.fx.jface.IGraphContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.diagram.RelationshipGraphLoadDataJob.GraphData;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRelationship;

/**
 * Content provider for displaying relationship data as a graph.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class RelationshipGraphContentProvider implements IGraphContentProvider {
	
	private Set<IntelEntityRelationship> relationships = Collections.emptySet();
	private Set<IntelEntity> entities = Collections.emptySet();

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof IntelEntity[]) {
			IntelEntity[] roots = (IntelEntity[]) newInput;
			loadGraphData(roots);
		} else if (newInput instanceof IntelEntity) {
			IntelEntity root = (IntelEntity) newInput;
			loadGraphData(root);
		}
	}

	private void loadGraphData(IntelEntity... root) {
		RelationshipGraphLoadDataJob loadJob = new RelationshipGraphLoadDataJob(2, root);
		loadJob.schedule();
		try {
			loadJob.join();
		} catch (InterruptedException e) {
			SmartPlugIn.displayError(Messages.RelationshipGraphContentProvider_LoadDataError, e);
		}
		GraphData loadedData = loadJob.getLoadedData();
		relationships = loadedData.getRelationships();
		entities = loadedData.getEntities();


	}

	@Override
	public Object[] getNodes() {
		return entities.toArray();
	}
	
	@Override
	public Object[] getAdjacentNodes(Object node) {
		if (node instanceof IntelEntity) {
			IntelEntity e = (IntelEntity) node;
			Set<IntelEntityRelationship> rels = relationships;
			Set<IntelEntity> ents = new HashSet<>();
			for (IntelEntityRelationship r : rels) {
				if (e.equals(r.getSourceEntity())) {
					ents.add(r.getTargetEntity());
				}
			}
			return ents.toArray();
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

	public IntelEntityRelationship getRelationship(Object source, Object target) {
		Set<IntelEntityRelationship> rels = relationships;
		for (IntelEntityRelationship r : rels) {
			if (source.equals(r.getSourceEntity()) && target.equals(r.getTargetEntity())) {
				return r;
			}
		}
		return null;
	}

}
