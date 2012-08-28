package org.wcs.smart.birt.map.tools;

import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.tools.internal.Zoom;

public class ZoomTool extends Zoom {

	public static final String ID = "org.wcs.smart.birt.map.tools.Zoom";
	@Override
	  public void mouseReleased(MapMouseEvent e) {
		  super.mouseReleased(e);
		  context.getMap().getRenderManager().refresh(null);
	  }
}
