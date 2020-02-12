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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
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
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsResultFile;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.udig.catalog.smart.ui.DesktopSessionProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * PAWS Wizard page for showing results and allows users to add them to any map.
 * @author Emily
 *
 */
public class PawsWizardPage extends AbstractUDIGImportPage implements UDIGConnectionPage {

	private CheckboxTreeViewer tblResults;
	
	private HashMap<PawsRun, PawsService> services = new HashMap<>();

	public PawsWizardPage() {
		super(Messages.PawsWizardPage_PageName);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		setControl(main);
		
		main.setLayout(new GridLayout());
		if (SmartDB.isMultipleAnalysis()){
			
			Label lbl = new Label(main, SWT.NONE);
			lbl.setText(Messages.PawsWizardPage_CCAAError); 
			
		}else{
			tblResults = new CheckboxTreeViewer(main);
			tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			tblResults.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof PawsRun) return MessageFormat.format("{0} [{1}]",((PawsRun)element).getId(),  DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(((PawsRun)element).getRunDate())); //$NON-NLS-1$
					if (element instanceof PawsFile) return ((PawsFile)element).file.getFileName().toString();
					if (element instanceof PawsResultFile) return ((PawsResultFile)element).getResultsFile().getFileName().toString();
					return super.getText(element);
				}
			});
			
			tblResults.addCheckStateListener(new ICheckStateListener() {				
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					Object[] x = ((ITreeContentProvider)tblResults.getContentProvider()).getChildren( event.getElement() );
					if (x != null) {
						for (Object i : x) tblResults.setChecked(i, event.getChecked());
					}
					getWizard().getContainer().updateButtons();
					services.clear();

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
					return (element instanceof PawsRun) ||
							(element instanceof PawsResultFile);
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
						try {
							PawsResultManager mng = new PawsResultManager(r);
							return mng.getResults().toArray();
						}catch (Exception ex) {
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
						return null;
					}else if (parentElement instanceof PawsResultFile) {
						PawsResultFile f = (PawsResultFile)parentElement;
						try {
							PawsFile[] results = new PawsFile[f.getRasterFiles().size()];
							for (int i = 0; i < f.getRasterFiles().size(); i ++) {
								results[i] = new PawsFile(f.getRasterFiles().get(i), f);
							}
							return results;
						}catch (Exception ex) {
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
					}
					return null;
				}
			});
			tblResults.setInput(new String[] {DialogConstants.LOADING_TEXT});
//			tblResults.addSelectionChangedListener(e->{
//				getWizard().getContainer().updateButtons();
//			});
			loadRuns.schedule();
		}
	}
	
	@Override
    public Collection<URL> getResourceIDs() {
    	
    	List<URL> urls = new ArrayList<>();
    	
    	
    	for (Object x : tblResults.getCheckedElements()) {
    		if (x instanceof PawsFile) {
    			PawsFile t = (PawsFile)x;
    			
    			PawsService ps = services.get(t.parent.getRun());
    			if (ps == null) {
    				ps = getService(t.parent.getRun());
    				services.put(t.parent.getRun(), ps);
    			}
    			
    			PawsTiffGeoResource r = new PawsTiffGeoResource(ps, t.parent, t.file);
    			urls.add(r.getIdentifier());
    		}
    	}
        return urls;
    }
    
    @Override
    public Collection<IService> getServices(){
    	if (services.isEmpty()) getResourceIDs();
    	List<IService> items = new ArrayList<>();
    	services.values().forEach(e->items.add(e));
    	return items;
    }
    
    private synchronized PawsService getService(PawsRun run)  {
    	HashMap<String, Serializable> params = new HashMap<>();
    	params.put(PawsServiceExtension.RUN_UUID_KEY, run.getUuid());
    	PawsService temp = new PawsService(params, new DesktopSessionProvider());
    	
    	temp = (PawsService) CatalogPlugin.getDefault().getLocalCatalog().add(temp);
    	return temp;
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
				if (x instanceof PawsFile) return true;
			}
			return false;
		}
	}
	
	private Job loadRuns = new Job(Messages.PawsWizardPage_loadingJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				List<PawsRun> runs = QueryFactory.buildQuery(session,  PawsRun.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"status", PawsRun.Status.COMPLETE}).list(); //$NON-NLS-1$
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

	private class PawsFile {
		Path file;
		PawsResultFile parent;
		
		public PawsFile(Path file, PawsResultFile parent) {
			this.file = file;
			this.parent = parent;
		}
	}
}
