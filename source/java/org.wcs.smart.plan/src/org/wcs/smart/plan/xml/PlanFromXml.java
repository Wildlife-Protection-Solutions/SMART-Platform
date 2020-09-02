/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.plan.xml;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;
import org.wcs.smart.plan.xml.model.ObjectFactory;
import org.wcs.smart.plan.xml.model.XmlName;
import org.wcs.smart.plan.xml.model.XmlPlan;
import org.wcs.smart.plan.xml.model.XmlPlanTarget;
import org.wcs.smart.plan.xml.model.XmlPlanTargetPoint;

/**
 * Converts plan from xml file into SMART Plan data model object
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class PlanFromXml {

	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	
	private List<String> warnings;	
	private Plan importedPlan = null;
	private List<Plan> tosave;
	private boolean isNew = false;
	
	private String allDuplicateMessage = Messages.PlanFromXml_PlansAlreadyExist;
	
	/**
	 * Sets the message to display when all plans already exists in the database
	 * 
	 * @param msg
	 */
	public void setAllDuplicateMessage(String msg) {
		allDuplicateMessage = msg;
	}
	
	/**
	 * Converts the xml plan file to a set of SMART plan objects
	 * but does not have it to the database.  Call doSave
	 * to save to the database.
	 * 
	 * @param xmlFile
	 * @return
	 * @throws Exception
	 */
	public boolean convertPlan(Path xmlFile) throws Exception {

		warnings = new ArrayList<>();
		XmlPlan xml = readXmlFile(xmlFile);
		
		Plan conversion = null;
		tosave = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
			conversion = convertPlanInternal(xml, session);
			
			Plan toSave = conversion;
			while(toSave.getParent() != null) toSave = toSave.getParent();
			tosave.add(toSave);

			if (conversion.getParent() == null && xml.getParentid() != null) {
				//lets looks for the parent
				Plan parent = QueryFactory.buildQuery(session, Plan.class, 
						new Object[] {"conservationArea", conversion.getConservationArea()}, //$NON-NLS-1$
						new Object[] {"id", xml.getParentid()}).uniqueResult(); //$NON-NLS-1$
				if (parent == null) {
					warnings.add(Messages.PlanFromXml_ParentNotFound);
				}else {
					conversion.setParent(parent);
				}
			}
			
			//check for duplicates based on ID only
			
			HashMap<Plan, Plan> xmlToDb = new HashMap<>();
			
			List<Plan> all = new ArrayList<>();
			List<Plan> toprocess = new ArrayList<>();
			toprocess.add(conversion);
			while(!toprocess.isEmpty()) {
				Plan t = toprocess.remove(0);
				all.add(t);
				toprocess.addAll(t.getChildren());
			}
			Plan t = conversion.getParent();
			while(t != null) {
				all.add(t);
				t = t.getParent();
			}
			
			for (Plan p : all) {
				Plan temp = QueryFactory.buildQuery(session, Plan.class, 
						new Object[] {"conservationArea", p.getConservationArea()}, //$NON-NLS-1$
						new Object[] {"id", p.getId()}).uniqueResult(); //$NON-NLS-1$
				if (temp != null) xmlToDb.put(p, temp);
			}
			
			importedPlan = conversion;
			if (!xmlToDb.isEmpty()) {
				if (xmlToDb.size() == all.size()) {
					//they all already exists
					//options are to import plans anyways and assign new ids OR skip
					
					final boolean[] r = new boolean[]{false};
					
					Display.getDefault().syncExec(()->{
						MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(), 
								Messages.PlanFromXml_ImportTitle, 
								null, 
								allDuplicateMessage, MessageDialog.QUESTION, 1, 
								new String[] {Messages.PlanFromXml_ImportButton, Messages.PlanFromXml_SkipButton});

						int x = md.open();
						if (x == 0) {
							r[0] = true;
						}					
					});
					if (!r[0]) return false;
					
					//generate new ids
					Set<String> other = new HashSet<>();
					for (Plan p : all) {
						p.setId( PlanHibernateManager.generatePlanId(p, session, other) );
						other.add(p.getId());
					}
					
				}else {
					//some already exist and some don't
					//options ares:
					//cancel
					//load all duplicating and assigning new ids
					//load only new plans updating references as required
					final int[] r = new int[1];
					Display.getDefault().syncExec(()->{
						r[0] = MessageDialog.open(MessageDialog.QUESTION, Display.getDefault().getActiveShell(), Messages.PlanFromXml_ImportTitle, 
							MessageFormat.format(Messages.PlanFromXml_SomePlansAlreadyExist,xmlToDb.size(),all.size()),
							SWT.NONE,
							Messages.PlanFromXml_ImportAllOp, Messages.PlanFromXml_ImportNewOp, Messages.PlanFromXml_CancelOp);
					});
					if (r[0] == 2) {
						return false;
					}else if (r[0] == 1) {
						//perform mapping
						tosave.clear();
						tosave.addAll(all);
						for (Plan p : all) {
							if (!xmlToDb.containsKey(p)) {
								if (p.getParent() != null && xmlToDb.containsKey(p.getParent())) {
									//update parent
									p.setParent(xmlToDb.get(p.getParent()));
									
								}
							}else {
								//remove this from the items to save
								if (p.getParent() != null) p.getParent().getChildren().remove(p);
								tosave.remove(p);
							}
						}
						if (xmlToDb.containsKey(conversion)) importedPlan = xmlToDb.get(conversion);
					}else {
						//import all with new ids
						//generate new ids
						Set<String> other = new HashSet<>();
						for (Plan p : all) {
							p.setId( PlanHibernateManager.generatePlanId(p, session, other) );
							other.add(p.getId());
						}
					}
							
				}
			}
		}
		
		if (!warnings.isEmpty()) {
			final int[] r = new int[1];
			Display.getDefault().syncExec(()->{
				WarningDialog warn = new WarningDialog(Display.getDefault().getActiveShell(), 
							Messages.PlanFromXml_ImportTitle, 
							Messages.PlanFromXml_WarningsMsg, 
							warnings,
							new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
				r[0]= warn.open();
			});
			if (r[0] != 0) return false;
		}

		return true;		
	}
	
	/**
	 * Saves the imported plan(s) to the 
	 * database.  Once complete call fireEvents
	 * to fire appropriate events.
	 * 
	 * @param session
	 */
	public void doSave(Session session) {
		isNew = importedPlan.getUuid() == null;

		for (Plan p : tosave) {
			session.saveOrUpdate(p);
		}
	}
	
	/**
	 * Fires appropriate events.  Should be
	 * called after save is complete.
	 * 
	 */
	public void fireEvents() {
		if (isNew) {
			PlanEventManager.getInstance().planAdded(importedPlan);
		}else {
			PlanEventManager.getInstance().planChanged(-1, importedPlan);
		}
	}
	
	/**
	 * If the import is successful this will return the plan
	 * added to the database or the existing plan referenced
	 * 
	 * @return
	 */
	public Plan getRootPlan() {
		return importedPlan;
	}
	
	private Plan convertPlanInternal(XmlPlan xml, Session session) throws Exception{
	
		Plan plan = new Plan();
		
		plan.setActiveEmployees(xml.getAvaliableEmployees());
		plan.setUnavailableEmployees(xml.getUnavaliableEmployees());
		plan.setChildren(new ArrayList<>());
		plan.setComment(xml.getComment());
		plan.setDescription(xml.getDescription());
		plan.setConservationArea(ca);
		plan.setCreator(SmartDB.getCurrentEmployee());
		if (xml.getEndDate() != null) plan.setEndDate(new Date(xml.getEndDate().toGregorianCalendar().getTime().getTime()));
		plan.setId(xml.getId());
		if (xml.getStartDate() != null) plan.setStartDate(new Date(xml.getStartDate().toGregorianCalendar().getTime().getTime()));
		
		try {
			plan.setStation(findStation(xml.getStation(), session));
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.PlanFromXml_StationNotFound, xml.getId(), ex.getMessage()));
		}
		try {
			plan.setTeam(findTeam(xml.getTeam(), session));
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.PlanFromXml_TeamNotFound, xml.getId(), ex.getMessage()));
		}
		
		for (XmlName n : xml.getNames()) {
			Language l = findLanguage(n.getLanguage(), session);
			if (l != null) plan.updateName(l, n.getValue());
		}
		if (plan.findNameNull(ca.getDefaultLanguage()) == null) {
			for (XmlName n : xml.getNames()) {
				if (n.isIsdeafult()) {
					plan.updateName(ca.getDefaultLanguage(), n.getValue());
					break;
				}
			}
		}
		
		plan.setTargets(new ArrayList<>());
		plan.setType(getType(xml.getType()));
		
		for (XmlPlanTarget xtarget : xml.getTargets()) {
			
			String type = xtarget.getType();
			if (type.equalsIgnoreCase(AdministrativePlanTarget.class.getName())) {
				AdministrativePlanTarget target = new AdministrativePlanTarget();
				target.setName(xtarget.getName());
				target.setTargetDesc(xtarget.getDescription());
				target.setStatus(xtarget.isCompleted());
				target.setPlan(plan);
				plan.getTargets().add(target);
			}else if (type.equalsIgnoreCase(SpatialPlanTarget.class.getName())) {
				SpatialPlanTarget target = new SpatialPlanTarget();
				target.setDescription(xtarget.getDescription());
				target.setDistanceForCompletion(xtarget.getSuccessDistance());
				target.setName(xtarget.getName());
				target.setPoints(new ArrayList<>());
				for (XmlPlanTargetPoint xpoint : xtarget.getPoints()) {
					SpatialPlanTargetPoint point = new SpatialPlanTargetPoint();
					point.setPlanTarget(target);
					point.setX(xpoint.getX());
					point.setY(xpoint.getY());
					target.getPoints().add(point);
				}
				
				target.setPlan(plan);
				plan.getTargets().add(target);
			}else if (type.toLowerCase(Locale.ROOT).startsWith(NumericPlanTarget.class.getName().toLowerCase(Locale.ROOT))) {
				String ntype = type.split(":")[1]; //$NON-NLS-1$
				NumericPlanTarget target = new NumericPlanTarget();
				try {
					target.setType( NumericPlanTarget.TargetType.valueOf(ntype) );
				}catch(Exception ex) {
					warnings.add(MessageFormat.format(Messages.PlanFromXml_InvalidNumericTargetType, xml.getId(), ntype));
					continue;
				}
				try {
					target.setOp( NumericPlanTarget.Operator.valueOf(xtarget.getOperator()));
				}catch(Exception ex) {
					warnings.add(MessageFormat.format(Messages.PlanFromXml_InvalidNumericOperator, xml.getId(), xtarget.getOperator()));
					continue;
				}	
				target.setDescription(xtarget.getDescription());
				target.setName(xtarget.getName());
				target.setValue(xtarget.getValue());
				
				target.setPlan(plan);
				plan.getTargets().add(target);
				
				
			}else {
				warnings.add(MessageFormat.format(Messages.PlanFromXml_InvalidTargetType, xml.getId(), type));
			}
			
		}
		
		if (xml.getParent() != null) {
			Plan parent = convertPlanInternal(xml.getParent(), session);
			plan.setParent(parent);
			parent.getChildren().add(plan);
		}
		for (XmlPlan xmlkid : xml.getChildren()) {
			Plan kid = convertPlanInternal(xmlkid, session);
			kid.setParent(plan);
			plan.getChildren().add(kid);
		}
		//parent
		return plan;
		
	}
	
	
	private Language findLanguage(String code, Session session) {
		List<Language> langs = QueryFactory.buildQuery(session, Language.class, new Object[] {"ca", ca} ).list(); //$NON-NLS-1$
		for (Language l : langs) {
			if (l.getCode().equalsIgnoreCase(code)) return l;
		}
		return null;
	}
	
	private Team findTeam(String team, Session session)  throws Exception{
		if (team == null) return null;
		
		List<Team>  items = QueryFactory.buildQuery(session, Team.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		
		for (Team s : items) {
			if (s.getName().equalsIgnoreCase(team)) return s;
		}
		for (Team s : items) {
			for (Label lbl : s.getNames()) {
				if (lbl.getValue().equalsIgnoreCase(team)) return s;
			}
		}
		throw new Exception(MessageFormat.format(Messages.PlanFromXml_TeamNameNotFound, team));
	}
	
	private Station findStation(String station, Session session)  throws Exception{
		if (station == null) return null;
		
		List<Station>  items = QueryFactory.buildQuery(session, Station.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		
		for (Station s : items) {
			if (s.getName().equalsIgnoreCase(station)) return s;
		}
		for (Station s : items) {
			for (Label lbl : s.getNames()) {
				if (lbl.getValue().equalsIgnoreCase(station)) return s;
			}
		}
		throw new Exception(MessageFormat.format(Messages.PlanFromXml_StationNameNotFound, station));
	}
	
	private Plan.PlanType getType(String key) throws Exception{
		try{
			return Plan.PlanType.valueOf(key);
		}catch (Exception ex) {
			throw new Exception(MessageFormat.format(Messages.PlanFromXml_InvalidPlanType, key));
		}
	}
	
	
	
	private XmlPlan readXmlFile(Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<XmlPlan> o = (JAXBElement<XmlPlan>) unmarshaller.unmarshal(xmlFile.toFile());
		return o.getValue();
	}
}
