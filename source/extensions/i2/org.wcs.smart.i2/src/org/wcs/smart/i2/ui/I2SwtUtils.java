package org.wcs.smart.i2.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;


public class I2SwtUtils {

	public static void cascadeAdd(Control parent, Listener listener, int... eventTypes){
		List<Control> controls = new ArrayList<Control>();
		controls.add(parent);
		while(!controls.isEmpty()){
			Control c = controls.remove(0);
			
			for (int e: eventTypes){
				c.addListener(e, listener);
			}
			
			if (c instanceof Composite){
				for (Control kid : ((Composite)c).getChildren()){
					controls.add(kid);
				}
			}
		}
		
	}
}
