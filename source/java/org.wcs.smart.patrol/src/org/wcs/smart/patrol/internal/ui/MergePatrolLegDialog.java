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

package org.wcs.smart.patrol.internal.ui;

import java.sql.Time;
import java.text.Collator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Dialog for merging patrol legs.
 * <p>Users can select the leg ID, transportation type, leader and pilot of the newly merged leg.
 * 
 * @author Jeff
 * @since 5.0.0
 */
public class MergePatrolLegDialog extends SmartStyledTitleDialog{
	private PatrolLeg newLeg; 
	private List<PatrolLeg> legsToMerge;
	
	private Text txtLegId;
	
	private ComboViewer groupLeader;
	private ComboViewer groupPilot;
	private ComboViewer cmbTransportTypes;
	private ComboViewer cmbMandates;
	
	private List<PatrolTransportType> typeOps;
	
	private boolean showPilot;
	private ArrayList<Employee> employeeList;
	private List<PatrolMandate> mandates;

	
	/**
	 * Creates a new merge leg dialog 
	 * 
	 * @param parentShell parent shell
	 * @param patrolLeg legs to merge
	 * @param typeOps transport type options 
	 */
	public MergePatrolLegDialog(Shell parentShell, ArrayList<PatrolLeg> patrolLegs,  
			List<PatrolTransportType> typeOps, List<PatrolMandate> mandates) {
		super(parentShell);
		this.legsToMerge = patrolLegs;
		this.typeOps = typeOps;
		this.mandates = mandates;

		showPilot = false;
		employeeList = new ArrayList<Employee>();
		for(PatrolLeg l : legsToMerge){
			if(l.getPatrol().hasPilot()){
				showPilot = true;
			}
			
			List<PatrolLegMember> members = l.getMembers();
			for(PatrolLegMember m : members){
				boolean duplicate = false;
				for(Employee existing : employeeList){
					if(existing.getId().equals(m.getMember().getId())) duplicate = true;
				}
				if(!duplicate){
					employeeList.add(m.getMember());
				}
			}

		}
		sortList(employeeList);
		
		
	}

	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	private void sortList(ArrayList<Employee> list){
		Collections.sort(list, new Comparator<Employee>(){
			@Override
			public int compare(Employee o1, Employee o2) {
				return Collator.getInstance().compare(
						SmartLabelProvider.getFullLabel(o1), SmartLabelProvider.getFullLabel(o2));
			}});
	}
	
	
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Label lbl;
		parent = (Composite) super.createDialogArea(parent);
		
		Composite patrolIdComp = new Composite(parent, SWT.NONE);
		patrolIdComp.setLayout(new GridLayout(2, false));
		patrolIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(patrolIdComp, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_LegId_Label );
		txtLegId = new Text(patrolIdComp, SWT.BORDER);
		txtLegId.setTextLimit(PatrolLeg.ID_MAX_SIZE);
		txtLegId.setText(legsToMerge.get(0).getId());
		txtLegId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		cmbTransportTypes = createTransportTypeComboViewer(patrolIdComp);		
		cmbMandates = createMandateComboViewer(patrolIdComp);

		
		groupLeader = createLeaderPilot(patrolIdComp, Messages.MergePatrolLegDialog_LeaderLabel, employeeList, legsToMerge.get(0).getLeader().getMember());
		if (showPilot){
			groupPilot = createLeaderPilot(patrolIdComp, Messages.MergePatrolLegDialog_Pilot, employeeList, null);
		}
		
		setMessage(Messages.MergePatrolLegDialog_DialogMessage);
		getShell().setText(Messages.MergePatrolLegDialog_ShellText);
		setTitle(Messages.MergePatrolLegDialog_DialogTitle);
		return parent;
	}
	
	/*
	 * Create a combo viewer for selecting patrol leader/pilot
	 */
	private ComboViewer createLeaderPilot(Composite parent, String name, List<Employee> employeeList, Employee defaultValue){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(name);
		ComboViewer cmb = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmb.setLabelProvider(new EmployeeLabelProvider());
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setInput(employeeList);
		cmb.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		if(defaultValue != null){
			cmb.setSelection( new StructuredSelection(defaultValue));
		}
		
		return cmb;
	}

	/*
	 * Transport type combo viewer
	 */
	private ComboViewer createTransportTypeComboViewer(Composite parent){
//		Composite ttype = new Composite(parent, SWT.NONE);
//		ttype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
//		ttype.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.EditPatrolLegDialog_TransportType_Label);
		ComboViewer cmbTransportType = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbTransportType.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				return ((PatrolTransportType)element).getName();
			}
		});
		cmbTransportType.setContentProvider(ArrayContentProvider.getInstance());
		cmbTransportType.setInput( typeOps );
		cmbTransportType.setSelection(new StructuredSelection(legsToMerge.get(0).getType()));
		cmbTransportType.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return cmbTransportType;
	}
	
	/*
	 * Mandate combo viewer
	 */
	private ComboViewer createMandateComboViewer(Composite parent){
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(Messages.MergePatrolLegDialog_MandateLabel);
		ComboViewer cmb = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmb.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				return ((PatrolMandate)element).getName();
			}
		});
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		if (!mandates.contains(legsToMerge.get(0).getMandate())) mandates.add(legsToMerge.get(0).getMandate());
		cmb.setInput( mandates); 
		cmb.setSelection(new StructuredSelection(legsToMerge.get(0).getMandate()));
		cmb.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return cmb;
	}
	
	/**
	 * Performs a validation then updates the patrol leg with the new values.
	 */
	@Override
	protected void okPressed() {
		//Make a new Leg to put everything into:
		newLeg = new PatrolLeg();
		newLeg.setId(txtLegId.getText());
		
		//get date from all legs to be merged
		Date startDate = null;
		Date endDate = null;
		ArrayList<PatrolLegMember> allMembers = new ArrayList<PatrolLegMember>();

		//gather start date and member type data from list of legs.
		for(PatrolLeg l : legsToMerge){
			if(startDate == null || l.getStartDate().before(startDate)) startDate = l.getStartDate();
			if(endDate == null || l.getEndDate().after(endDate)) endDate = l.getEndDate();

			for(PatrolLegMember m : l.getMembers()){
				//check if this employee is already in the list
				boolean duplicate = false;
				for(PatrolLegMember existing : allMembers){
					if(existing.getMember().equals(m.getMember())) {
						duplicate = true;
						break;
					}
				}
				if(!duplicate){
					PatrolLegMember plm = new PatrolLegMember();
					plm.setMember(m.getMember());
					plm.setPatrolLeg(newLeg);
					
					StructuredSelection selected = (StructuredSelection) groupLeader.getSelection(); 
					Employee e = (Employee) selected.getFirstElement();
					if(plm.getMember().equals( e ) ){
						plm.setIsLeader(true);
					}
					
					if(groupPilot != null){
						selected = (StructuredSelection) groupPilot.getSelection();
					}
					if(groupPilot != null && (plm.getMember().equals( e ) )){
						plm.setIsPilot(true);
					}
					allMembers.add(plm);
				}
			}
		}
			
		//set the start and end date to the earliest and latest of all legs we are merging
		newLeg.setStartDate(startDate);
		newLeg.setEndDate(endDate);
		newLeg.setMandate( (PatrolMandate)((StructuredSelection) cmbMandates.getSelection()).getFirstElement());
		newLeg.setMembers(allMembers);
		//newLeg.setPatrol(legsToMerge.get(0).getPatrol());
		StructuredSelection selected = (StructuredSelection) cmbTransportTypes.getSelection();
		newLeg.setType((PatrolTransportType)selected.getFirstElement());
		
		//Create a new list of merged PatrolLegDays
		
		//Put all the PatrolLegDays in a single list
		ArrayList<PatrolLegDay> pldsToMerge = new ArrayList<PatrolLegDay>();
		for(PatrolLeg l : legsToMerge) {
			List<PatrolLegDay> legDays = l.getPatrolLegDays();
			if(legDays != null){
				for(PatrolLegDay ld: legDays){
					pldsToMerge.add(ld);
				}
			}
		}
		// determine the range of days we need to create for the new list
		Date s = null;
		Date e = null;		
		for(PatrolLegDay  pld : pldsToMerge){
			if(s == null || pld.getDate().before(s))s = pld.getDate();
			if(e == null || pld.getDate().after(e))e = pld.getDate();
		}
		LocalDate curDay = LocalDate.ofInstant(s.toInstant(), ZoneId.systemDefault());
		LocalDate lastDay = LocalDate.ofInstant(e.toInstant(), ZoneId.systemDefault());
		
		ArrayList<PatrolLegDay> newDays = new ArrayList<PatrolLegDay>();
		for(;!curDay.isAfter(lastDay); curDay = curDay.plusDays(1)) {
			PatrolLegDay newpld = new PatrolLegDay();
			
			newpld.setDate(java.sql.Date.valueOf(curDay));
			newpld.setPatrolLeg(newLeg);

			Time st = null;
			Time et = null;
			ArrayList<PatrolLegDay> todaysPlds = new ArrayList<PatrolLegDay>();
			
			int totalRestTime = 0;
			for(PatrolLegDay  pld : pldsToMerge){
				// check if the pld is on the day we are currently doing
				LocalDate d = LocalDate.ofInstant(pld.getDate().toInstant(), ZoneId.systemDefault());
				if(curDay.equals(d)) {
					todaysPlds.add(pld);
					if(st == null || pld.getStartTime().before(st)) st = pld.getStartTime();
					if(et == null || pld.getEndTime().after(et)) et = pld.getEndTime();
					if(pld.getRestMinutes() != null) totalRestTime += pld.getRestMinutes();
				}
			}
			
			newpld.setStartTime(st);
			newpld.setEndTime(et);
			newpld.setRestMinutes(totalRestTime);			
			
			ArrayList<byte[]> allTracks = new ArrayList<byte[]>();
			ArrayList<PatrolWaypoint> allWaypoints = new ArrayList<PatrolWaypoint>();
			for(PatrolLegDay pld : todaysPlds){
				if(pld.getWaypoints() != null){
					for(PatrolWaypoint wp : pld.getWaypoints() ){
						PatrolWaypoint pw = new PatrolWaypoint();
						pw.setPatrolLegDay(newpld);
						pw.setWaypoint(wp.getWaypoint());
						allWaypoints.add(wp);
					}
				}
				
				if(pld.getTrack() != null){
					allTracks.add(pld.getTrack().getGeom());
				}
			}
			
			
			ArrayList<Track> tracks = new ArrayList<Track>();
			newpld.setTracks(tracks);
			
			Track t = rebuildTrackFromGeometries(allTracks);
			if (t != null) {
				t.setPatrolLegDay(newpld);
				tracks.add(t);
			}
			
			newpld.setWaypoints(allWaypoints);
			newpld.setPatrolLeg(newLeg);
			newDays.add(newpld);
		}
		
		newLeg.setPatrolLegDays(newDays);
		
		super.okPressed();
		
	}


	private Track rebuildTrackFromGeometries(ArrayList<byte[]> allTracks) {
		ArrayList<Coordinate> allPoints = new ArrayList<Coordinate>(); 
		for(byte[] geomByte : allTracks){
			WKBReader wkbReader = new WKBReader();
		    try {
		        Geometry geom = wkbReader.read(geomByte);
		        for(Coordinate point : geom.getCoordinates()){
		        	allPoints.add(point);
		        }
		    } catch (ParseException e) {
		        throw new RuntimeException(e);
		    }
		}
		Coordinate[] orderedPoints = new Coordinate[allPoints.size()];
		int x = 0;
		Coordinate earliest;
		while(!allPoints.isEmpty()){
			orderedPoints[x] = earliest = getEarliestPoint(allPoints);
			x++;
			allPoints.remove(earliest);
		}
		
		if (orderedPoints.length == 0) return null;
		
		GeometryFactory geomFact = new GeometryFactory();
		LineString ls = geomFact.createLineString(orderedPoints);
		
		Track newTrack = new Track();
		newTrack.setLineStrings(Arrays.asList(ls));		
		return newTrack;		
	}


	private Coordinate getEarliestPoint(ArrayList<Coordinate> allPoints) {
		Coordinate earliest = allPoints.get(0);
		for(Coordinate c : allPoints){
			if(c.getZ() < earliest.getZ())earliest = c;
		}
		return earliest;
	}


	public  PatrolLeg getNewLeg() {
		return newLeg;
	}
	
}