/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.view;

import java.awt.Point;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class QaMapInfoToolProvider implements IInfoToolProvider {

	
	private ValidationResultsEditor editor;
	
	public QaMapInfoToolProvider(ValidationResultsEditor editor){
		this.editor = editor;
	}
	
	
	@Override
	public InfoPoint findFeature(int x, int y, IViewportModel vm) {
		if (editor.getResults() == null) return null;
		
		Coordinate world = vm.pixelToWorld(x, y);
		Coordinate db = null;
		try {
			db = ReprojectUtils.reproject(world.x, world.y, vm.getCRS(), SmartDB.DATABASE_CRS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		QaError nearest = null;
		com.vividsolutions.jts.geom.Point toTest = GeometryFactoryProvider.getFactory().createPoint(db);
		double distance = Double.MAX_VALUE;
		
		for (QaError result : editor.getResults()){
			if (result.getGeometryObject() != null){
				if ( (result.getGeometryObject() instanceof com.vividsolutions.jts.geom.Point) ||
						result.getGeometryObject().getEnvelopeInternal().contains(db)){
					double d = result.getGeometryObject().distance(toTest);
					if (d < distance){
						distance = d;
						nearest = result;
					}
				}
			}
		}
		
		if (nearest == null) return null;
		Geometry g = nearest.getGeometryObject();
		Coordinate[] c = DistanceOp.nearestPoints(g, toTest);
		if (c.length == 0) return null;

		Coordinate px;
		try {
			px = ReprojectUtils.reproject(c[0].x, c[0].y, SmartDB.DATABASE_CRS, vm.getCRS());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Point pnt = vm.worldToPixel(px);
		if (pnt.distance(x, y) > 5) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append(nearest.getErrorId());
		
		createMenu(editor.getMapViewer().getControl(), nearest);
		return new InfoPoint(pnt, nearest, sb.toString());
	}
	
	
	private void createMenu(Control control, QaError error){
		
		Menu existingMenu = control.getMenu();
		if(existingMenu != null && !existingMenu.isDisposed()){
			existingMenu.dispose();
		}

		Menu menuTable = new Menu(control);
		control.setMenu(menuTable);

		control.addListener(SWT.MouseMove, new Listener(){
			@Override
			public void handleEvent(Event event) {
				control.removeListener(SWT.MouseMove, this);
				menuTable.dispose();
				control.setMenu(null);
			}
			
		});

		MenuItem miTest = new MenuItem(menuTable, SWT.NONE);
		miTest.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
		miTest.setText("Show In Table");
		miTest.addListener(SWT.Selection, e->{
			editor.setSelection(error);
		});
		
		new QaActionMenu(menuTable, editor.getContext(), new ISelectionProvider() {
			
			@Override
			public void setSelection(ISelection selection) {
			}
			
			@Override
			public void removeSelectionChangedListener(
					ISelectionChangedListener listener) {
			}
			
			@Override
			public ISelection getSelection() {
				return new StructuredSelection(error);
			}
			
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
			}
		}){
			@Override
			public void refresh() {
				editor.refreshResults();
			}
			
		};
		
	}

}
