package org.wcs.smart.udig;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.tool.AbstractActionTool;

public class ZoomLayersTool extends AbstractActionTool {

	public ZoomLayersTool() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		Map map = (Map) ApplicationGIS.getActiveMap();
		ReferencedEnvelope bounds = map.getBounds(new NullProgressMonitor());
		map.sendCommandASync(new SetViewportBBoxCommand(bounds));
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}
