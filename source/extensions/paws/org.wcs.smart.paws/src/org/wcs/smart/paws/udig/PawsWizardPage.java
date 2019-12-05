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
package org.wcs.smart.paws.udig;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.text.Collator;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ui.AbstractUDIGImportPage;
import org.locationtech.udig.catalog.ui.UDIGConnectionPage;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.ui.properties.DialogConstants;

public class PawsWizardPage extends AbstractUDIGImportPage implements UDIGConnectionPage {

	private CheckboxTreeViewer tblResults;
	private PawsService service;

	public PawsWizardPage() {
		super("PAWS Results Layers");
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		setControl(main);
		
		main.setLayout(new GridLayout());
		if (SmartDB.isMultipleAnalysis()){
			
			Label lbl = new Label(main, SWT.NONE);
			lbl.setText("PAWS Results are not available for cross Conservation Area analysis."); 
			
		}else{
			tblResults = new CheckboxTreeViewer(main);
			tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			tblResults.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof PawsRun) return MessageFormat.format("{0} [{1}]",((PawsRun)element).getId(),  DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(((PawsRun)element).getRunDate()));
					if (element instanceof Path) return ((Path)element).getFileName().toString();
					return super.getText(element);
				}
			});
			
			tblResults.setContentProvider(new ITreeContentProvider() {
				private List<PawsRun> runs = null;

				@SuppressWarnings({ "unchecked", "rawtypes" })
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					runs = null;
					if (newInput instanceof List) runs = (List) newInput;
				}
				 
				@Override
				public boolean hasChildren(Object element) {
					return (element instanceof PawsRun);
				}
				
				@Override
				public Object getParent(Object element) {
					return null;
				}
				
				@Override
				public Object[] getElements(Object inputElement) {
					if (runs == null) return new String[] {DialogConstants.LOADING_TEXT};
					return runs.toArray(new PawsRun[runs.size()]);
				}
				
				@Override
				public Object[] getChildren(Object parentElement) {
					if (parentElement instanceof PawsRun) {
						PawsRun r = (PawsRun)parentElement;
						PawsResultManager mng = new PawsResultManager(r);
						List<Path> files = mng.getRasterFiles();
						return files.toArray( new Path[files.size()] );
					}
					return null;
				}
			});
			tblResults.setInput(new String[] {DialogConstants.LOADING_TEXT});
			tblResults.addSelectionChangedListener(e->{
				getWizard().getContainer().updateButtons();
			});
			loadRuns.schedule();
		}
	}
	
    public Collection<URL> getResourceIDs() {
    	getService();
    	List<URL> urls = new ArrayList<>();
    	if (service == null) return Collections.emptyList();
    	for (Object x : tblResults.getCheckedElements()) {
    		if (x instanceof Path) {
    			PawsTiffGeoResource r = new PawsTiffGeoResource(service, (Path)x);
    			urls.add(r.getIdentifier());
    		}
    	}
        return urls;
    }
    
    public Collection<IService> getServices() {
   		return Collections.singletonList(service);
    }
	
    private synchronized PawsService getService()  {
    	if (service != null) return service;

    	HashMap<String, Serializable> params = new HashMap<>();
    	params.put(PawsServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
    	PawsService temp = new PawsService(params);
    	
    	service = (PawsService) CatalogPlugin.getDefault().getLocalCatalog().add(temp);
    	return service;
    }
    
    @Override
    public Map<String, Serializable> getParams() {
    	HashMap<String, Serializable> map = new HashMap<String, Serializable>();
    	map.put(SmartServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
    	return map;
    }
    
	@Override
	public boolean isPageComplete() {
		if (SmartDB.isMultipleAnalysis()){
			return false;
		}else {
			for (Object x : tblResults.getCheckedElements()) {
				if (x instanceof Path) return true;
			}
			return false;
		}
	}
	
	private Job loadRuns = new Job("loading PAWS results") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				List<PawsRun> runs = QueryFactory.buildQuery(session,  PawsRun.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"status", PawsRun.Status.COMPLETE}).list();
				runs.forEach(r->r.getConservationArea().getFileDataStoreLocation());
				runs.sort((a,b)->{
					if (a.getRunDate().equals(b.getRunDate())) return Collator.getInstance().compare(a.getId(),  b.getId());
					return a.getRunDate().compareTo(b.getRunDate());
				});
				Display.getDefault().asyncExec(()->PawsWizardPage.this.tblResults.setInput(runs));
			}
			return Status.OK_STATUS;
		}
		
	};

}
