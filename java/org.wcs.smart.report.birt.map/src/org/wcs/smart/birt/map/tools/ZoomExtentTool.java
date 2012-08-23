package org.wcs.smart.birt.map.tools;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;

import net.refractions.udig.project.IMap;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.ui.tool.AbstractActionTool;

public class ZoomExtentTool extends AbstractActionTool {

	public ZoomExtentTool() {
	}

	@Override
	public void run() {
		
		IMap map = super.context.getMap();
		ReferencedEnvelope bounds = map.getBounds(new NullProgressMonitor());
		map.sendCommandASync(new SetViewportBBoxCommand(bounds));
		map.getRenderManager().refresh(null);
	}

	@Override
	public void dispose() {

	}

}
