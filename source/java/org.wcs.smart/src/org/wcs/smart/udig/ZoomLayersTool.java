package org.wcs.smart.udig;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.tool.AbstractActionTool;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Zoom tool for map.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ZoomLayersTool extends AbstractActionTool {

	public ZoomLayersTool() {
	}

	@Override
	public void run() {
		Map map = (Map) ApplicationGIS.getActiveMap();
		ReferencedEnvelope bounds = map.getBounds(new NullProgressMonitor());
		map.sendCommandASync(new SetViewportBBoxCommand(bounds));
	}

	@Override
	public void dispose() {

	}

}
