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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramEntityTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramRelationshipTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyleOptions;

/**
 * Factory for creating {@link RelationshipDiagramStyle}.
 *  
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramStyleFactory {

	public static RelationshipDiagramStyleOptions createDefaultStyleOptions() {
		RelationshipDiagramStyleOptions options = new RelationshipDiagramStyleOptions("{}"); //$NON-NLS-1$
		options.setDefaultNodeStyle(createDefaultNodeOptions());
		options.setDefaultEdgeStyle(createDefaultEdgeOptions());
		return options;
	}
	
	public static RelationshipDiagramNodeStyleOptions createDefaultNodeOptions() {
		RelationshipDiagramNodeStyleOptions options = new RelationshipDiagramNodeStyleOptions("{'imageSize':'SMALL','backgroundColor':'#FFFFFF','foregroundColor':'#000000'}"); //$NON-NLS-1$
		return options;
	}

	public static RelationshipDiagramEdgeStyleOptions createDefaultEdgeOptions() {
		RelationshipDiagramEdgeStyleOptions options = new RelationshipDiagramEdgeStyleOptions("{'color':'#FF0000','showLabel':true}"); //$NON-NLS-1$
		return options;
	}
	
	/**
	 * Creates a CyberTracker properties style with all parameters set to defaults.
	 */
	public static RelationshipDiagramStyle createUsingDefaults(String name) {
		RelationshipDiagramStyle style = new RelationshipDiagramStyle();
		style.setConservationArea(SmartDB.getCurrentConservationArea());
		style.setName(name);
		style.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		
		style.setStyleOptions(createDefaultStyleOptions());
		
		return style;
	}
	
	/**
	 * Creates a CyberTracker properties style using another configurable model as a template.
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility 
	 * to call done() on the given monitor
	 */
	public static RelationshipDiagramStyle createStyleClone(RelationshipDiagramStyle style, String name, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.RelationshipDiagramStyleFactory_CloneStyle_Task, 1);
		
		progress.subTask(Messages.RelationshipDiagramStyleFactory_CloneStyle_Task);
		RelationshipDiagramStyle clone = createUsingDefaults(name);
		try {
			//NOTE: we are not coping isDefault and names
			clone.setOptionValues(style.getOptionValues());
			
			for (RelationshipDiagramEntityTypeStyle etStyle : style.getEntityTypeStyles().values()) {
				RelationshipDiagramEntityTypeStyle ets = new RelationshipDiagramEntityTypeStyle();
				ets.setStyle(clone);
				ets.setEntityType(etStyle.getEntityType());
				ets.setOptionValues(etStyle.getOptionValues());
				clone.getEntityTypeStyles().put(ets.getEntityType(), ets);
				progress.checkCanceled();
			}

			for (RelationshipDiagramRelationshipTypeStyle rtStyle : style.getRelationshipTypeStyles().values()) {
				RelationshipDiagramRelationshipTypeStyle rts = new RelationshipDiagramRelationshipTypeStyle();
				rts.setStyle(clone);
				rts.setRelationshipType(rtStyle.getRelationshipType());
				rts.setOptionValues(rtStyle.getOptionValues());
				clone.getRelationshipTypeStyles().put(rts.getRelationshipType(), rts);
				progress.checkCanceled();
			}

		} catch ( OperationCanceledException ex) {
			return null;
		}
		return clone;
	}

}