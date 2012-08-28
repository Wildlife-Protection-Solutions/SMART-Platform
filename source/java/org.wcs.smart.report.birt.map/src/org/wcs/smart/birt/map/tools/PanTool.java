package org.wcs.smart.birt.map.tools;

import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;

public class PanTool extends net.refractions.udig.tools.internal.PanTool {

	public PanTool() {
		super();
	}

	
    /**
     * @see net.refractions.udig.project.ui.tool.AbstractTool#mouseReleased(net.refractions.udig.project.render.displayAdapter.MapMouseEvent)
     */
    public void mouseReleased( MapMouseEvent e ) {
     	super.mouseReleased(e);
     	context.getMap().getRenderManager().refresh(null);
    }
}
