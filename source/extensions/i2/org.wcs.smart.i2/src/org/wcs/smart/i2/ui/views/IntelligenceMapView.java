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
package org.wcs.smart.i2.ui.views;

import javax.annotation.PostConstruct;

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.ui.map.MapView;

/*
 * implementing as an editor; but we only ever want one 
 * 
 */
public class IntelligenceMapView extends MapView  {

	public static final String ID = "org.wcs.smart.i2.view.map";
	
	  @PostConstruct
	  public void createPartControl( Composite parent ) {
		 super.createPartControl(parent);
		 
		 getMap().setName("Intelligence Map");
	  }

	  
	  public static class IntelligenceMapViewWrapper extends DIViewPart<IntelligenceMapView> implements MapPart{

			public IntelligenceMapViewWrapper() {
				super(IntelligenceMapView.class);
			}

			@SuppressWarnings({ "rawtypes" })
			public Object getAdapter(Class adaptee) {
				Object x = getComponent().getAdapter(adaptee);
				if (x != null){
					return x;
				}
				return super.getAdapter(adaptee);
			}

			@Override
			public Map getMap() {
				return getComponent().getMap();
			}

			@Override
			public void openContextMenu() {
				getComponent().openContextMenu();
			}

			@Override
			public void setFont(Control textArea) {
				getComponent().setFont(textArea);
			}

			@Override
			public void setSelectionProvider(
					IMapEditorSelectionProvider selectionProvider) {
				getComponent().setSelectionProvider(selectionProvider);
				
			}

			@Override
			public IStatusLineManager getStatusLineManager() {
				return getComponent().getStatusLineManager();
			}
		}
}
