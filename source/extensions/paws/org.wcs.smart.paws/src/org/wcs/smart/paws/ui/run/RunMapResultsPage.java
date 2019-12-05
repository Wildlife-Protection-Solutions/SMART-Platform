/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.run;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.EditorPart;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.udig.PawsService;
import org.wcs.smart.paws.udig.PawsServiceExtension;
import org.wcs.smart.paws.udig.PawsTiffGeoResource;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;

/**
 * PAWS Editor results page
 * 
 * @author Emily
 *
 */
public class RunMapResultsPage extends SmartMapEditorPart{
	
	private Composite part;
	
	private RunEditor editor;
	
	private List<Layer> resultslayers;
	
	public RunMapResultsPage(RunEditor parent) {
		this.editor = parent;
		resultslayers = new ArrayList<>();
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		(new LoadDefaultLayersJob(getMap(), false)).schedule();
		addInitialZoomFunction();
	}  
	
	public void refresh(PawsResultManager run) {
		//delete existing layers
		if (!resultslayers.isEmpty()) {
			DeleteLayersCommand dcmd = new DeleteLayersCommand(resultslayers.toArray(new Layer[resultslayers.size()]));
			getMap().sendCommandASync(dcmd);
			resultslayers.clear();
		}
		
		//only add layers if status is complete
		if (run.getRun().getStatus() != PawsRun.Status.COMPLETE) {
			return;
		}
		Map<String, Serializable> params = new HashMap<>();
		params.put(PawsServiceExtension.CA_UUID_KEY, run.getRun().getConservationArea().getUuid());
		PawsService service = new PawsService(params);
		
		List<IGeoResource> toadd = new ArrayList<>();
		for (Path p : run.getRasterFiles()) {
			PawsTiffGeoResource r = new PawsTiffGeoResource(service, p);
			toadd.add(r);
		}
		AddLayersCommand cmd = new AddLayersCommand(toadd) {
		    public void run( IProgressMonitor monitor ) throws Exception {
		    	super.run(monitor);
		    	resultslayers.addAll(getLayers());
		    }
		};
		getMap().sendCommandASync(cmd);
	}
	
	@Override
	public void setFocus() {
		part.setFocus();
	}

	@Override
	public EditorPart getParentEditor() {
		return editor;
	}

}
