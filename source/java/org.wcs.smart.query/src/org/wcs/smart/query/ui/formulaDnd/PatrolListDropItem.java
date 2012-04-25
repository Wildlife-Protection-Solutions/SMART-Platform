/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.query.ui.formulaDnd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.query.parser.internal.PatrolFilter;
import org.wcs.smart.query.parser.internal.PatrolFilter.PatrolFilterOption;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol drop item for a patrol option 
 * that contains a list of options (for example
 * station list, team list etc).
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolListDropItem extends DropItem{

	private String keyPart;
	private String text;
	private PatrolFilterOption option;
	
	private ComboViewer listViewer;
	private Font smallerFont = null;
	
	private ListItem currentSelection = null;
	
	/*
	 * job for loading options
	 */
	private Job loadItemsJobs = new Job("Loading List Items"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session s = HibernateManager.openSession();
			try{
				ConservationArea ca = SmartDB.getCurrentConservationArea();
				final ArrayList<ListItem> items = new ArrayList<ListItem>();
				
				if (option == PatrolFilterOption.MANDATE){
					List<PatrolMandate> mandates = PatrolHibernateManager.getActiveMandates(ca, s);
					for (PatrolMandate m : mandates){
						items.add(new ListItem(m.getUuid(), m.getName()));
					}
				}else if (option == PatrolFilterOption.STATION){
					List<Station> stations = PatrolHibernateManager.getActiveStations(ca, s);
					for (Station m : stations){
						items.add(new ListItem(m.getUuid(), m.getName()));
					}
				}else if (option == PatrolFilterOption.TEAM){
					List<Team> teams = PatrolHibernateManager.getActiveTeams(ca, s);
					for (Team m : teams){
						items.add(new ListItem(m.getUuid(), m.getName()));
					}
				}else if (option == PatrolFilterOption.TRANSPORT){
					List<PatrolTransportType> transports = PatrolHibernateManager.getActivePatrolTransporationTypes(ca, s);
					for (PatrolTransportType m : transports){
						items.add(new ListItem(m.getUuid(), m.getName()));
					}
					
				}else if (option == PatrolFilterOption.PATROLTYPE){
					List<PatrolType> types = PatrolHibernateManager.getActivePatrolTypes(ca, s);
					for (PatrolType m : types){
						items.add(new ListItem(null, m.getType().getGuiName(), m.getType().name()));
					}
					
				}else if (option == PatrolFilterOption.EMPLOYEE ||
						option == PatrolFilterOption.LEADER ||
						option == PatrolFilterOption.PILOT
						){
					List<Employee> types = HibernateManager.getActiveEmployees(ca, s);
					for (Employee m : types){
						items.add(new ListItem(m.getUuid(), m.getGivenName() + " " + m.getFamilyName() + " [" + m.getId() + "]"));
					}
				}
			
				Display.getDefault().asyncExec(new Runnable(){

					@Override
					public void run() {
						listViewer.setInput(items.toArray(new ListItem[items.size()]));
						if (currentSelection != null){
							listViewer.setSelection(new StructuredSelection(currentSelection));
						}
					}});
				
			}finally{
				s.close();
			}
			return Status.OK_STATUS;
		}};
	private Label lbl;
		
		
	/**
	 * Creates a new patrol list drop item
	 *  
	 * @param parent parent
	 * @param target target item
	 * @param option patrol filter option
	 */
	public PatrolListDropItem(PatrolFilter.PatrolFilterOption option) {
		//super(parent, target);

		this.keyPart = "patrol:" + option.getKeyPart();
		this.text = option.getGuiName();
		this.option = option;
	}

	
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder(this.text);
		sb.append(" = ");
		if (listViewer.getSelection() != null){
			ListItem it = (ListItem)((IStructuredSelection)listViewer.getSelection()).getFirstElement();
			if (it != null){
				sb.append("\"");
				sb.append(it.getName());
				sb.append("\"");
			}
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder(this.keyPart);
		sb.append(" equals ");
		ListItem it = null;
		
		
		if (currentSelection != null){
			it = currentSelection;
		}else if (listViewer.getSelection() != null){ 
			it = (ListItem)((IStructuredSelection)listViewer.getSelection()).getFirstElement();
		}
		if (it != null){
			sb.append("\"");
			if (option == PatrolFilterOption.PATROLTYPE){
				sb.append(it.getKey().toUpperCase());
			}else{
				if (it.getUuid() != null){
					sb.append( SmartUtils.encodeHex(it.getUuid()));
				}
			}
			sb.append("\"");
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lbl = new Label(main, SWT.NONE);
		
		
		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ListItem newSelection =  (ListItem) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				if (currentSelection != null && newSelection.equals(currentSelection)){
					//no change
				}else{		
					PatrolListDropItem.this.queryChanged();
					currentSelection = newSelection;
				}
			}
		});
		
		listViewer.setInput(new ListItem[]{new ListItem(null, "Loading")});
		
		initDrag(main);
		initDrag(lbl);

		lbl.setText(this.text + " = ");	
		loadItemsJobs.schedule();
	}


	
	/**
	 * @param data a list item
	 */
	@Override
	public void initializeData(Object data) {
		currentSelection = (ListItem)data;
	}
}
