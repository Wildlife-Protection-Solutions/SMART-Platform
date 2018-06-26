package org.wcs.smart.event.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.event.ActionTypeManager;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Imports trigger items from xml file and merges them with the existing items in the database
 * based on id.  Will add new items and update items, but does not delete items that do not exist in the
 * xml file.
 * 
 * @author Emily
 *
 */
public class EventsFromXml {

	/**
	 * Reads a event xml file into a Configuration objects
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static org.wcs.smart.event.xml.model.Configuration readEventXml(Path intputFile ) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(EventsToXml.METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		JAXBElement<org.wcs.smart.event.xml.model.Configuration> o = (JAXBElement<org.wcs.smart.event.xml.model.Configuration>) un.unmarshal(intputFile.toFile());
		org.wcs.smart.event.xml.model.Configuration x = o.getValue();
		return x;
		
	}
	
	private ConservationArea ca;
	
	private List<String> warnings = new ArrayList<>();
	public EventsFromXml(ConservationArea ca) {
		this.ca = ca;
	}
	
	public void importAndMerge(Path inputFile, IProgressMonitor monitor) throws Exception {
		warnings = new ArrayList<>();
		SubMonitor progress = SubMonitor.convert(monitor);
		
		progress.beginTask(Messages.EventsFromXml_progress1,  3);
		org.wcs.smart.event.xml.model.Configuration xmlConfig = readEventXml(inputFile);
		progress.worked(1);
		
		//convert xml to smart model items
		SmartModelItems items = convertToSmart(xmlConfig, progress.split(1));
		
		//merge with SMART
		mergeWithDatabase(items, progress.split(1));
		
		progress.done();
	}
	
	private void mergeWithDatabase(SmartModelItems items, SubMonitor monitor) throws Exception {
		List<EActionEvent> actionEventsToSaveOrUpdate = new ArrayList<>();
		List<EFilter> filtersToSaveOrUpdate = new ArrayList<>();
		List<EAction> toSaveOrUpdate = new ArrayList<>();
		
		monitor.beginTask(Messages.EventsFromXml_progress2, 4);
		try(Session session = HibernateManager.openSession()){
			
			//merge actions
			List<EAction> dbActions = QueryFactory.buildQuery(session, EAction.class, new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			
			
			for (EAction newAction : items.actions) {
				//find an action with the same type and id
				EAction dbActionToMerge = null;
				for (EAction dbAction : dbActions) {
					if (dbAction.getActionTypeKey().equalsIgnoreCase(newAction.getActionTypeKey()) &&
							dbAction.getId().equals(newAction.getId())){
						dbActionToMerge = dbAction;
						break;
					}
				}
				
				if (dbActionToMerge == null) {
					//we want to save new Action to the database
					toSaveOrUpdate.add(newAction);
				}else {
					toSaveOrUpdate.add(dbActionToMerge);
					//update any new action events to map to this action
					for (EActionEvent ae : items.events) {
						if (ae.getAction().equals(newAction)) {
							ae.setAction(dbActionToMerge);
						}
					}
					
					//update dbAction and do not save newAction to database
					for (EActionParameterValue newActionParam : newAction.getParameters()) {
						EActionParameterValue toUpdate = null;
						for (EActionParameterValue pv : dbActionToMerge.getParameters()) {
							if (pv.getId().getParameterKey().equalsIgnoreCase(newActionParam.getId().getParameterKey())) {
								toUpdate = pv;
								break;
							}
						}
						if (toUpdate == null) {
							EActionParameterValue toadd = new EActionParameterValue();
							toadd.setParameterValue(newActionParam.getParameterValue());
							toadd.getId().setAction(dbActionToMerge);
							toadd.getId().setParameterKey(newActionParam.getId().getParameterKey());
							dbActionToMerge.getParameters().add(toadd);
						}else {
							toUpdate.setParameterValue(newActionParam.getParameterValue());
						}
					}
				}
			}
			monitor.worked(1);
			
			//merge filters
			List<EFilter> dbFilters = QueryFactory.buildQuery(session, EFilter.class, new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			for (EFilter newFilter : items.filters) {
				//find an action with the same type and id
				EFilter dbFilterToMerge = null;
				for (EFilter dbFilter: dbFilters) {
					if (dbFilter.getId().equals(newFilter.getId())){
						dbFilterToMerge = dbFilter;
						break;
					}
				}
				
				if (dbFilterToMerge == null) {
					//we want to save new Action to the database
					filtersToSaveOrUpdate.add(newFilter);
				}else {
					filtersToSaveOrUpdate.add(dbFilterToMerge);
					dbFilterToMerge.setFilterString(newFilter.getFilterString());
				
					//update any new filters events to map to this action
					for (EActionEvent ae : items.events) {
						if (ae.getFilter().equals(newFilter)) {
							ae.setFilter(dbFilterToMerge);
						}
					}
				}
			}
			monitor.worked(1);
			
			//merge action/events
			List<EActionEvent> dbActionEvents = QueryFactory.buildQuery(session, EActionEvent.class, new Object[] {"action.conservationArea", ca}).list(); //$NON-NLS-1$
			for (EActionEvent newActionEvent : items.events) {
				//find an action with the same type and id
				boolean save = true;
				for (EActionEvent dbActionEvent: dbActionEvents) {
					if (newActionEvent.getAction().equals(dbActionEvent.getAction()) &&
							newActionEvent.getFilter().equals(dbActionEvent.getFilter())) {
						save = false;
						break;
					}
				}
				if (save) actionEventsToSaveOrUpdate.add(newActionEvent);
			}
			monitor.worked(1);
		}
		
		if (!warnings.isEmpty()) {
			WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), 
					Messages.EventsFromXml_WaringsTitle, Messages.EventsFromXml_WarningsMsg, 
					warnings,
					new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL},
					1);
			if(wd.open() != 0) return;
		}
		
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				toSaveOrUpdate.forEach(e->session.saveOrUpdate(e));
				filtersToSaveOrUpdate.forEach(f->session.saveOrUpdate(f));
				actionEventsToSaveOrUpdate.forEach(ae->session.saveOrUpdate(ae));
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw new Exception(Messages.EventsFromXml_MergeError + ex.getMessage(), ex);
			}
		}
		monitor.worked(1);
		monitor.done();
	}
	
	private SmartModelItems convertToSmart(org.wcs.smart.event.xml.model.Configuration xmlConfig, SubMonitor monitor) {
		SmartModelItems items = new SmartModelItems();
		
		monitor.beginTask(Messages.EventsFromXml_progress3, 3);
		//import filters
		HashMap<String, EFilter> newFilters = new HashMap<>();
		for (org.wcs.smart.event.xml.model.EFilter xmlFilter : xmlConfig.getFilters()) {
			EFilter filter = new EFilter();
			filter.setConservationArea(ca);
			filter.setFilterString(xmlFilter.getFilterString()); //TODO Validate filter (may be invalid if data model not the same)
			filter.setId(xmlFilter.getId());
			
			items.filters.add(filter);
			newFilters.put(xmlFilter.getUuid(), filter);
		}
		monitor.worked(1);
		
		//import actions
		HashMap<String, EAction> newActions = new HashMap<>();
		for (org.wcs.smart.event.xml.model.EAction xmlAction : xmlConfig.getActions()) {
			
			IActionType type = ActionTypeManager.INSTANCE.getActionType(xmlAction.getActionTypeKey());
			if (type == null) {
				warnings.add(MessageFormat.format(Messages.EventsFromXml_ActionTypeNotFound, xmlAction.getActionTypeKey()));
				continue;
			}
			
			
			EAction action = new EAction();
			action.setActionTypeKey(type.getKey());
			action.setConservationArea(ca);
			action.setId(xmlAction.getId());
			action.setParameters(new ArrayList<>());
			
			for (org.wcs.smart.event.xml.model.EActionParameter xmlParam : xmlAction.getParameters()) {
				IActionParameter actionParameter = null;
				for (IActionParameter pp : type.getActionParameters()) {
					if (pp.getKey().equalsIgnoreCase(xmlParam.getKey())) {
						actionParameter = pp;
						break;
					}
				}
				if (actionParameter == null) {
					warnings.add(MessageFormat.format(Messages.EventsFromXml_ParameterKeyNotFoun, xmlParam.getKey(), xmlAction.getActionTypeKey()));
					continue;
				}
				
				EActionParameterValue parameter = new EActionParameterValue();
				parameter.getId().setAction(action);
				parameter.getId().setParameterKey(actionParameter.getKey());
				parameter.setParameterValue(xmlParam.getValue());
				
				action.getParameters().add(parameter);
				
			}
					
			items.actions.add(action);
			newActions.put(xmlAction.getUuid(), action);
		}
		monitor.worked(1);
		
		//import action events
		for (org.wcs.smart.event.xml.model.EActionEvent xmlEventAction : xmlConfig.getActionEvents()) {
			EAction action = newActions.get(xmlEventAction.getEActionUuid());
			EFilter filter = newFilters.get(xmlEventAction.getEFilterUuid());
			if (action == null || filter == null) continue;
			
			EActionEvent eventAction = new EActionEvent();
			eventAction.setEnabled(true);
			if (xmlEventAction.isEnabled() != null) {
				eventAction.setEnabled(xmlEventAction.isEnabled());
			}
			eventAction.setAction(action);
			eventAction.setFilter(filter);
			items.events.add(eventAction);
		}
		monitor.worked(1);
		
		monitor.done();
		return items;
		
	}
	
	
	private class SmartModelItems{
		private List<EAction> actions = new ArrayList<>();
		private List<EFilter> filters = new ArrayList<>();
		private List<EActionEvent> events = new ArrayList<>();
		
		
		
	}
}
