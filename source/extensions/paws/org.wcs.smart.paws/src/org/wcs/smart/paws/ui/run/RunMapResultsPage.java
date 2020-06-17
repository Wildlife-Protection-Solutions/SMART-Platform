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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.part.EditorPart;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.LayerFactory;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsResultFile;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.udig.PawsService;
import org.wcs.smart.paws.udig.PawsServiceExtension;
import org.wcs.smart.paws.udig.PawsTiffGeoResource;
import org.wcs.smart.udig.catalog.smart.ui.DesktopSessionProvider;
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
	private Label lblTimeFrame;
	private Composite temp1;
	private Scale scEffort;
	
	private AddJob addlayerjob;
	
	public RunMapResultsPage(RunEditor parent) {
		this.editor = parent;
		resultslayers = new ArrayList<>();
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		temp1 = new Composite(part, SWT.NONE);
		temp1.setLayout(new GridLayout(2, false));
		((GridLayout)temp1.getLayout()).marginWidth = 0;
    	((GridLayout)temp1.getLayout()).marginHeight = 0;
    	
		lblTimeFrame = new Label(temp1, SWT.NONE);
		lblTimeFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (editor.getResultFile() != null) {
			lblTimeFrame.setText(editor.getResultFile().getTimeFrameString());
		}
		Composite temp2 = new Composite(part, SWT.NONE);
		temp2.setLayout(new GridLayout());
		((GridLayout)temp2.getLayout()).marginWidth = 0;
    	((GridLayout)temp2.getLayout()).marginHeight = 0;
    	
    	scEffort = new Scale(temp2, SWT.HORIZONTAL);
    	scEffort.setMinimum(1);
    	scEffort.setMaximum(10);
    	scEffort.setIncrement(1);
    	scEffort.setPageIncrement(1);
    	scEffort.setSelection(5);
    	scEffort.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	((GridData)scEffort.getLayoutData()).heightHint = 23;
    	scEffort.addListener(SWT.Selection, e->{
    		if (resultslayers == null || resultslayers.isEmpty()) return;
    		resultslayers.forEach(l->l.setVisible(false));
    		Layer l = resultslayers.get(scEffort.getSelection()-1);
    		l.setVisible(true);
    	});

    	Composite b= new Composite(temp2, SWT.NONE);
    	b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	b.setLayout(new GridLayout(3, false));
    	((GridLayout)b.getLayout()).marginWidth = 0;
    	((GridLayout)b.getLayout()).marginHeight = 0;
    	
    	Label low = new Label(b, SWT.NONE);
    	low.setText(Messages.RunMapResultsPage_LowEffort);
    	low.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
    	FontData fd = low.getFont().getFontData()[0];
    	fd.setHeight(fd.getHeight() - 2);
    	Font smaller = new Font(b.getDisplay(), fd);
    	low.addListener(SWT.Dispose, e->smaller.dispose());
    	low.setFont(smaller);
    	
    	Label l = new Label(b, SWT.NONE);
    	l.setText(Messages.RunMapResultsPage_PatrolEffort);
    	l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    	
    	Label high = new Label(b, SWT.NONE);
    	high.setText(Messages.RunMapResultsPage_HighEffort);
    	high.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));		
    	((GridData)high.getLayoutData()).horizontalIndent = 20;
    	high.setFont(smaller);	
		
		Composite part2 = new Composite(part, SWT.NONE);
		super.createPartControl(part2);
		part.addListener(SWT.Resize, e->{
			part2.setBounds(part.getBounds());
			
			Point pnt = temp1.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			temp1.setBounds(1, 1, pnt.x,pnt.y);
			int y = pnt.y;
			pnt = temp2.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			temp2.setBounds(1, y, pnt.x,pnt.y);
			
		});
		
		addInitialZoomFunction();
		(new LoadDefaultLayersJob(getMap(), false)).schedule();
	}  
	
	private PawsResultFile lastSelection = null;
	
	public void refresh(PawsResultManager mngr) {
		
		if (editor.getResultFile() != null && editor.getResultFile() == lastSelection) return;
		
		//delete existing layers
		if (!resultslayers.isEmpty()) {
			DeleteLayersCommand dcmd = new DeleteLayersCommand(resultslayers.toArray(new Layer[resultslayers.size()]));
			getMap().sendCommandASync(dcmd);
			resultslayers.clear();
		}
		
		//only add layers if status is complete
		if (mngr.getRun().getStatus() != PawsRun.Status.COMPLETE) {
			return;
		}
		
		if (editor.getResultFile() == null) return;
		
		lblTimeFrame.setText(editor.getResultFile().getTimeFrameString());

		Map<String, Serializable> params = new HashMap<>();
		params.put(PawsServiceExtension.RUN_UUID_KEY, mngr.getRun().getUuid());
		PawsService service = new PawsService(params, new DesktopSessionProvider());
		
		List<PawsTiffGeoResource> toadd = new ArrayList<>();
		try {
			scEffort.setMinimum(1);
			scEffort.setMaximum(editor.getResultFile().getRasterFiles().size());
			scEffort.setIncrement(1);
			scEffort.setPageIncrement(1);
			scEffort.setSelection(editor.getResultFile().getRasterFiles().size() / 2);
			
			lastSelection = editor.getResultFile();
			
			for (Path p : editor.getResultFile().getRasterFiles()) {
				PawsTiffGeoResource r = new PawsTiffGeoResource(service, editor.getResultFile(), p);
				toadd.add(r);
			}	
		}catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		final int selectionindex=scEffort.getSelection()-1;
		if (addlayerjob == null) addlayerjob = new AddJob(super.getMap());
		addlayerjob.setData(toadd, selectionindex);
	}
	
	@Override
	public void setFocus() {
		part.setFocus();
	}

	@Override
	public EditorPart getParentEditor() {
		return editor;
	}

	private class AddJob extends Job {

		private int selectionindex = 0;
		private org.locationtech.udig.project.internal.Map map;

		public AddJob(org.locationtech.udig.project.internal.Map map) {
			super("add layers"); //$NON-NLS-1$
			this.map = map;
		}

		private List<PawsTiffGeoResource> data;

		public void setData(List<PawsTiffGeoResource> data, int selectionindex) {
			this.data = data;
			this.selectionindex = selectionindex;
			this.cancel();
			this.schedule();
		}

		@Override
		protected IStatus run(IProgressMonitor progressMonitor) {
			ArrayList<Layer> layers = new ArrayList<>();
			LayerFactory layerFactory = map.getLayerFactory();

			for (PawsTiffGeoResource item : data) {
				try {
					Layer layer = layerFactory.createLayer(item);
					if (layer != null) {
						layer.setVisible(false);
						layer.setName(item.getInfo(progressMonitor).getTitle());
						layers.add(layer);
					}
				} catch (Throwable t) {
					ProjectPlugin.log("Unable to add " + item, t); //$NON-NLS-1$
				}
			}
			if(progressMonitor.isCanceled()) return Status.CANCEL_STATUS;
			if (resultslayers.isEmpty()) {
				DeleteLayersCommand dcmd = new DeleteLayersCommand(
						resultslayers.toArray(new Layer[resultslayers.size()]));
				getMap().sendCommandSync(dcmd);
				resultslayers.clear();
			}
			if (!layers.isEmpty()) {
				resultslayers.addAll(layers);
				map.getLayersInternal().addAll(map.getLayersInternal().size(), layers);
				resultslayers.get(selectionindex).setVisible(true);
			}
			return Status.OK_STATUS;
		}

	}
}
