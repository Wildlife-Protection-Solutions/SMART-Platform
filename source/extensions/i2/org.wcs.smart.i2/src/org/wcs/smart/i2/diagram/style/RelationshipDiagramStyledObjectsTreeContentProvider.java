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

import org.eclipse.jface.viewers.ITreeContentProvider;
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
			case ENTITY_TYPE:
				//TODO: ZZZZZZZZZZZZ
				break;
			case RELATIONSIP_TYPE:
				//TODO: ZZZZZZZZZZZZ
				break;

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
