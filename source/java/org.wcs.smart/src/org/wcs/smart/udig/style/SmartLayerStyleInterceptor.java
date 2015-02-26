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
package org.wcs.smart.udig.style;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.interceptor.LayerInterceptor;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * A layer interceptor to initiate SMART Saved Styles.  This interceptor
 * adds a layer listener to each layer, the runs only the first time the layer
 * style blackboard is set.  When setting the layer style blackboard if the SMART
 * saved style id exists on the blackboard, then the style is loaded from the database
 * and applied to the layer.
 * 
 * @author Emily
 *
 */
public class SmartLayerStyleInterceptor implements LayerInterceptor {

	final static ILayerListener ll = new ILayerListener() {
		@Override
		public void refresh(LayerEvent event) {
			if (event.getType() == EventType.STYLE){
				//remove the listener; we only want to run this once
				event.getSource().removeListener(ll);
				loadStyle(event.getSource());
			}
		}
	};
	
	@Override
	public void run(Layer layer) {
		layer.addListener(ll);
	}
	
	private static void loadStyle(ILayer layer){
		//check for SMART saved style
		final byte[] styleUuid = (byte[]) layer.getStyleBlackboard().get(SmartLayerStyle.STYLE_ID);
		if (styleUuid != null) {
			//load from database
			final String[] styleString = {null};
			
			Job loadStyle = new Job("load style"){ //$NON-NLS-1$
				//in job so we don't interfere with open session (namely reports)
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try {
						//we want to make sure we are loading from the current conservation area
						//it would be possible to export a query, import into a different ca, but this
						//value would still reference the old conservation area
						@SuppressWarnings("unchecked")
						List<SmartStyle> list = s.createCriteria(SmartStyle.class)
									.add(Restrictions.eq("uuid", styleUuid)) //$NON-NLS-1$
									.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
									.list();
						if (list.size() > 0){
							SmartStyle ss = list.get(0);
							styleString[0] = ss.getStyleString();
						}
					} finally {
						s.close();
					}
					return Status.OK_STATUS;
				}
				
			};
			loadStyle.setSystem(true);
			loadStyle.schedule();
			
			
			try {
				loadStyle.join();
				
				if (styleString[0] != null) {
					StyleBlackboard sb = StyleManager.INSTANCE.fromString(styleString[0]);
					layer.getStyleBlackboard().clear();
					layer.getStyleBlackboard().addAll(sb);
					layer.getStyleBlackboard().put(SmartLayerStyle.STYLE_ID, styleUuid);
				}
			} catch (Exception ex) {
				SmartPlugIn.log(Messages.SmartLayerStyleInterceptor_StyleError + ex.getMessage(), ex);
			}
		}
		
	}

}
