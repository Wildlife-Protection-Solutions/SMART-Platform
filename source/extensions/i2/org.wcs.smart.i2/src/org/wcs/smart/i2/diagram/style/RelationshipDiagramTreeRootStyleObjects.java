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

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;

/**
 * Root objects in styles configuration tree. 
 * 
 * @author elitvin
 * @since 6.0.0
 */
public enum RelationshipDiagramTreeRootStyleObjects {
	DEFAULT(Messages.RelationshipDiagramTreeRootStyleObjects_Default),
	ROOT(Messages.RelationshipDiagramTreeRootStyleObjects_RootNode),
	ENTITY_TYPE(Messages.RelationshipDiagramTreeRootStyleObjects_EntityType),
	RELATIONSIP_TYPE(Messages.RelationshipDiagramTreeRootStyleObjects_RelationshipType);

	private final String displayName;

	private RelationshipDiagramTreeRootStyleObjects(String displayName) {
		this.displayName = displayName;
	}

	public String getGuiName() {
		return this.displayName;
	}
	
	public Image getImage() {
		switch (this) {
		case DEFAULT:
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STYLE_ICON);
		case ENTITY_TYPE:
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
		case RELATIONSIP_TYPE:
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RELATIONSHIP);
		case ROOT:
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ROOTENTITY);
		}
		return null;
	}
}
