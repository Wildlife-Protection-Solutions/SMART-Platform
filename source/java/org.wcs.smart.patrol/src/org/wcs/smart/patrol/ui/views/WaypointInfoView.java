package org.wcs.smart.patrol.ui.views;

import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

public class WaypointInfoView extends ViewPart implements ISelectionListener {

	public static final String ID = "org.wcs.smart.patrol.waypointInfo"; //$NON-NLS-1$
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Label lblWaypointId;
	private Label lblDateTime;
	
	private Font boldFont = null;
	private ScrolledForm infoSection = null;

	private Waypoint selectedWaypoint;
	private Job updateUiJob = new Job("update info indow"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (infoSection.isDisposed()) return Status.OK_STATUS;
			final Waypoint wp = selectedWaypoint;
			final HashMap<String, List<List<String[]>>> displayData = new HashMap<String, List<List<String[]>>>();
			
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			Date wpDate = null; 
			try{
				s.update(wp);
				wpDate = wp.getPatrolLegDay().getDate();
				HashMap<Category, List<List<String[]>>> data = new HashMap<Category, List<List<String[]>>>();
				for (WaypointObservation wo : wp.getObservations()){
					List<List<String[]>> ops = data.get(wo.getCategory());
					
					if (ops == null){
						ops = new ArrayList<List<String[]>>();
						data.put(wo.getCategory(), ops);
					}
					ArrayList<String[]> attributeValues = new ArrayList<String[]>();
					for (WaypointObservationAttribute woa : wo.getAttributes()){
						String[] info = new String[]{woa.getAttribute().getName(), woa.getAttributeValueAsString()};
						attributeValues.add(info);
					}
					ops.add(attributeValues);
				}
				
				for (Entry<Category, List<List<String[]>>> cat : data.entrySet()){
					Category c = (Category) s.merge(cat.getKey());
					displayData.put(c.getFullCategoryName(), cat.getValue());
				}
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			final Date wpDate2 = wpDate;
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (lblWaypointId.isDisposed()) return;
					lblWaypointId.setText(String.valueOf(wp.getId()));
					lblDateTime.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(wpDate2) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(wp.getTime())); //$NON-NLS-1$
					
					for (Entry<String, List<List<String[]>>> cat : displayData.entrySet()){
						Label lbl = toolkit.createLabel(infoSection.getBody(), SmartUtils.formatStringForLabel(cat.getKey()), SWT.WRAP);
						lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
						((GridData)lbl.getLayoutData()).widthHint = 50;
						lbl.setFont(boldFont);
						 
						Composite attributeComp = toolkit.createComposite(infoSection.getBody());
						attributeComp.setLayout(new GridLayout(2, false));
						((GridLayout)attributeComp.getLayout()).marginLeft = 5;
						attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						
						
						for(List<String[]> obs : cat.getValue()){
							Collections.sort(obs, new Comparator<String[]>() {
								@Override
								public int compare(String[] o1, String[] o2) {
									return Collator.getInstance().compare(o1[0], o2[0]);
								}});
							for (String[] att : obs){
								toolkit.createLabel(attributeComp, SmartUtils.formatStringForLabel(att[0] + ":")); //$NON-NLS-1$
								toolkit.createLabel(attributeComp, SmartUtils.formatStringForLabel(att[1]));
							}
							Label l = toolkit.createLabel(attributeComp, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
							l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
							
						}
					}
					infoSection.getBody().layout();
					infoSection.reflow(true);
					lblWaypointId.getParent().layout();
				}
				
			});
			
			return Status.OK_STATUS;
		}};
	
	public WaypointInfoView() {
	}

	@Override
	public void dispose(){
		super.dispose();
		getSite().getPage().removeSelectionListener(this);
		if (boldFont != null && !boldFont.isDisposed()){
			boldFont.dispose();
			boldFont = null;
		}
	}
	@Override
	public void createPartControl(Composite parent) {
		getSite().getPage().addSelectionListener(this);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = 0;
		parent.setLayout(gl);
		
		
		Composite main = toolkit.createComposite(parent);
		gl = new GridLayout(1, false);
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(main);
		
		Label l = toolkit.createLabel(main, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		//infoSection = toolkit.createComposite(main);
		infoSection = toolkit.createScrolledForm(main);
		infoSection.getBody().setLayout(new GridLayout(1, false));
		infoSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
	}
	
	
	private void createHeader(Composite parent) {
		Composite header = toolkit.createComposite(parent);
		header.setLayout(new GridLayout(4, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label l = toolkit.createLabel(header, "Waypiont Id:");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(getViewSite().getShell().getDisplay(), fd);
		//l.setFont(boldFont);
		
		lblWaypointId = toolkit.createLabel(header, ""); //$NON-NLS-1$
		lblWaypointId.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(header,  "DateTime:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		//l.setFont(boldFont);
		
		lblDateTime = toolkit.createLabel(header, "");
		lblDateTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
	}

	private void updateContents(final Waypoint wp){
		for (Control c : infoSection.getBody().getChildren()){
			c.dispose();
		}
		selectedWaypoint = wp;
		updateUiJob.schedule();
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection.isEmpty()) {
			return;
		}
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		Object o = ss.getFirstElement();
		if (o instanceof Waypoint) {
			updateContents((Waypoint) o);
		}

	}

}
