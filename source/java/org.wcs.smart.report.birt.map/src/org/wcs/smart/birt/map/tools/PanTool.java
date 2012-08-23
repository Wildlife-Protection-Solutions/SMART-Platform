package org.wcs.smart.birt.map.tools;

import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;

public class PanTool extends net.refractions.udig.tools.internal.PanTool {

	public PanTool() {
		// TODO Auto-generated constructor stub
	}

	
    /**
     * @see net.refractions.udig.project.ui.tool.AbstractTool#mouseReleased(net.refractions.udig.project.render.displayAdapter.MapMouseEvent)
     */
    public void mouseReleased( MapMouseEvent e ) {
     	super.mouseReleased(e);
     	context.getMap().getRenderManager().refresh(context.getMap().getViewportModel().getBounds());
    }
}
