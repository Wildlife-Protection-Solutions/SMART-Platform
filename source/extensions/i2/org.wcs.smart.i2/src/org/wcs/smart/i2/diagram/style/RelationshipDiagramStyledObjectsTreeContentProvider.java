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
package org.wcs.smart.i2.diagram.style;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.diagram.LoadEntityTypeJob;
import org.wcs.smart.i2.diagram.LoadRelationshipTypeJob;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Tree Content provider for objects that can have style configuration in tree diagram.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramStyledObjectsTreeContentProvider implements ITreeContentProvider {

	private IntelEntityType[] entityTypes;
	private IntelRelationshipType[] relationshipTypes;
	
	private LoadEntityTypeJob entityTypeJob = new LoadEntityTypeJob(false) {
		@Override
		protected void processData(List<IntelEntityType> types) {
			entityTypes = types.toArray(new IntelEntityType[]{});
		}
	};
	
	private LoadRelationshipTypeJob relationshipTypeJob = new LoadRelationshipTypeJob(false) {
		@Override
		protected void processData(List<IntelRelationshipType> types) {
			relationshipTypes = types.toArray(new IntelRelationshipType[]{});;
		}
	};
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof RelationshipDiagramStyle) {
			return RelationshipDiagramTreeRootStyleObjects.values();
		}
		return new Object[]{};
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RelationshipDiagramTreeRootStyleObjects) {
			switch ((RelationshipDiagramTreeRootStyleObjects)parentElement) {
			case ENTITY_TYPE: {
				if (entityTypes == null) {
					entityTypeJob.schedule();
					try{
						entityTypeJob.join();
					}catch(InterruptedException ex){
						Intelligence2PlugIn.log(ex.getLocalizedMessage(), ex);
					}
				}
				return entityTypes;
			}
			case RELATIONSIP_TYPE: {
				if (relationshipTypes == null) {
					relationshipTypeJob.schedule();
					try{
						relationshipTypeJob.join();
					}catch(InterruptedException ex){
						Intelligence2PlugIn.log(ex.getLocalizedMessage(), ex);
					}
				}
				return relationshipTypes;
				
			}
			default:
				return new Object[]{};
			}
		}
		return new Object[]{};
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IntelEntityType) {
			return RelationshipDiagramTreeRootStyleObjects.ENTITY_TYPE;
		}
		if (element instanceof IntelRelationshipType) {
			return RelationshipDiagramTreeRootStyleObjects.RELATIONSIP_TYPE;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return RelationshipDiagramTreeRootStyleObjects.ENTITY_TYPE.equals(element) || RelationshipDiagramTreeRootStyleObjects.RELATIONSIP_TYPE.equals(element);
	}

}
