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
package org.wcs.smart.cybertracker.export;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.filter.Categories;
import org.wcs.smart.cybertracker.model.filter.ElementFilters;
import org.wcs.smart.cybertracker.model.filter.Filter;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.SmartUtils;

/**
 * Util for creating patrol screens for CyberTracker.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolScreensUtil {
	
	private static final String GLOBAL_PATROL_TYPE = "GLOBAL_PATROL_TYPE"; //$NON-NLS-1$

	/**
	 * @param screens
	 * @param element
	 * @return root id
	 */
	public static CyberTrackerId addPatrolNodes(List<Node> nodes, Elements elements, CyberTrackerId dmRootId, Session session) {
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		CyberTrackerId id = addSimpleNextRadioNode(startId, nodes, elements, "SMART CyberTracker", "#PatrolStart", ElementsUtil.addCustomElements(elements, "Start New Patrol"));
		//patrol type & transport
		List<PatrolType> patrolTypes = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
		id = addTypeTransportNodes(id, nodes, elements, patrolTypes);
		//patrol armed
		List<String> labelValues = new ArrayList<String>();
		labelValues.add("Yes");
		labelValues.add("No");
		List<String> tag0Values = new ArrayList<String>();
		tag0Values.add("true"); //$NON-NLS-1$
		tag0Values.add("false"); //$NON-NLS-1$
		List<CyberTrackerId> armedIds = ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
		id = addSimpleNextRadioNode(id, nodes, elements, "Is Armed", "#Armed", armedIds);
		
		List<Team> teams = PatrolHibernateManager.getActiveTeams(ca, session);
		id = addSimpleNextRadioNode(id, nodes, elements, "Team", "#Team", toCyberTrackerIds(elements, teams));

		List<Station> stations = PatrolHibernateManager.getActiveStations(ca, session);
		id = addSimpleNextRadioNode(id, nodes, elements, "Station", "#Station", toCyberTrackerIds(elements, stations));

		List<PatrolMandate> mandates = PatrolHibernateManager.getActiveMandates(ca, session);
		id = addSimpleNextRadioNode(id, nodes, elements, "Mandate", "#Mandate", toCyberTrackerIds(elements, mandates));

		id = addNoteNextNode(id, nodes, elements, "Objective", "#Objective");
		id = addNoteNextNode(id, nodes, elements, "Comments", "#Comments");

		//getting all members names
		List<Employee> employees = PatrolHibernateManager.getActiveEmployees(ca, session);
		List<String> members = new ArrayList<String>();
		for (Employee i : employees) {
			members.add(i.getLabel());
		}
		String[] memberNames = members.toArray(new String[members.size()]);
		List<CyberTrackerId> memberIds = ElementsUtil.addCustomElements(elements, memberNames);
		String filter = buildMembersFilter(id.getNodeId(), memberIds, Arrays.asList(memberNames));
		if (filter != null) {
			filter = SmartUtils.encodeHex(filter.getBytes());
		}
		
		id = addMembersNode(id, nodes, memberIds);
		id = addSimpleNextRadioNode(id, nodes, elements, "Leader", "#Leader", memberIds, filter);
		Node leaderNode = nodes.get(nodes.size()-1);
		String pilotNodeId = id.getNodeId();
		id = addSimpleNextRadioNode(id, nodes, elements, "Pilot", "#Pilot", memberIds, filter);
		addNavigationFormula(leaderNode, builPilotFormula(patrolTypes), pilotNodeId, id.getNodeId());
		
		addTaskNode(id, nodes, elements, startId, dmRootId);
		return id;
	}

	/**
	 * @param name
	 * @param elements
	 * @return String id of newly created element
	 */
	private static String createResultElement(String name, Elements elements) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId());
		return resultId.getItemId();
	}

	private static CyberTrackerId toNextScreen(Node node) {
		CyberTrackerId nextId = new CyberTrackerId();
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		control2.setTranslateNextScreenId(nextId.getNodeId());
		return nextId;
	}

	private static void applyFilter(Node node, String filter) {
		if (filter != null) {
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setFilterEnabled("True"); //$NON-NLS-1$
			control7.setTranslateFilter(filter);
		}
	}
	
	private static CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, List<Node> nodes, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids) {
		String resultId = createResultElement(resultElName, elements);
		Node node = CyberTrackerUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		nodes.add(node);
		return toNextScreen(node);
	}

	private static CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, List<Node> nodes, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, String filter) {
		String resultId = createResultElement(resultElName, elements);
		Node node = CyberTrackerUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		applyFilter(node, filter);
		nodes.add(node);
		return toNextScreen(node);
	}
	
	private static CyberTrackerId addNoteNextNode(CyberTrackerId id, List<Node> nodes, Elements elements, String name, String resultElName) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ScreensObjectFactory.createNodeNote(id.getNodeId(), name,  resultId);
		nodes.add(node);
		return toNextScreen(node);
	}

	private static CyberTrackerId addTypeTransportNodes(CyberTrackerId id, List<Node> nodes, Elements elements, List<PatrolType> pTypes) {
		List<String> types = new ArrayList<String>();
		List<String> tag0Types = new ArrayList<String>();
		for (PatrolType patrolType : pTypes) {
			types.add(patrolType.getType().getGuiName());
			tag0Types.add(patrolType.getType().name());
		}
		List<CyberTrackerId> typeIds = ElementsUtil.addCustomElements(elements, types, tag0Types);
		String resultElemId = createResultElement("#PatrolType", elements);
		Node node = CyberTrackerUtil.createRadioNode(id.getNodeId(), "Patrol Type", typeIds, resultElemId, true);
		Control control7 = ScreensObjectFactory.getRadioMainControl(node);
		control7.setResultGlobalValue(GLOBAL_PATROL_TYPE);
		nodes.add(node);
		CyberTrackerId nextId = new CyberTrackerId();
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, "#PatrolTransport", resultId.getItemId()); //$NON-NLS-1$
		for (int i = 0; i < pTypes.size(); i++) {
			List<CyberTrackerId> trIds = toCyberTrackerIds(elements, pTypes.get(i).getTransportTypes());
			node = CyberTrackerUtil.createRadioNode(typeIds.get(i).getNodeId(), types.get(i), trIds, resultId.getItemId());
			nodes.add(node);
			Control control2 = ScreensObjectFactory.getNavigationControl(node);
			control2.setTranslateNextScreenId(nextId.getNodeId());
		}
		return nextId;
	}

	private static CyberTrackerId addMembersNode(CyberTrackerId id, List<Node> nodes, List<CyberTrackerId> memberIds) {
		List<String> values = CyberTrackerUtil.listItemIds(memberIds);
		String trElements = CyberTrackerUtil.translateElements(memberIds);
		String trLinks = CyberTrackerUtil.translateLinks(memberIds, false);
		Node node = ScreensObjectFactory.createNodeChecklist(id.getNodeId(), "Members", values, trElements, trLinks);
		nodes.add(node);
		return toNextScreen(node);
	}

	private static void addTaskNode(CyberTrackerId id, List<Node> nodes, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId) {
		CyberTrackerId confId = new CyberTrackerId();
		Node confirmNode = ScreensObjectFactory.createNodeMsgText(confId.getNodeId(), "Confirm", "Press \"Save\" to confirm ending patrol or use back button");
		//disable next button, enable save button,navigate on save to start point
		Control control2 = ScreensObjectFactory.getNavigationControl(confirmNode);
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setTranslateMajorScreenId(startId.getNodeId());
		
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, "Make Observation", "End Patrol");
		List<String> values = CyberTrackerUtil.listItemIds(ids);
		String trElements = CyberTrackerUtil.translateElements(ids);
		//custom translate links logic
		StringBuilder links = new StringBuilder();
		// "Make observations" leads to datamodel root
		links.append(ids.get(0).getItemTranslatedId()).append(dmRootId.getNodeTranslatedId());
		// "End Patrol" leads to confirmation screen
		links.append(ids.get(1).getItemTranslatedId()).append(confId.getNodeTranslatedId());
		Node node = ScreensObjectFactory.createNodeRadio(id.getNodeId(), "Next Task", values, trElements, links.toString(), null);
		nodes.add(node);
		nodes.add(confirmNode);
	}
	
	public static List<CyberTrackerId> toCyberTrackerIds(Elements elements, List<? extends SimpleListItem> items) {
		List<String> labelValues = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (SimpleListItem i : items) {
			labelValues.add(i.getName());
			tag0Values.add(SmartUtils.encodeHex(i.getUuid()));
		}
		return ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
	}

	private static String buildMembersFilter(String memberNodeId, List<CyberTrackerId> memberIds, List<String> memberNames) {
		Filter filter = new Filter();
		filter.setVersion(1);
		
		Categories categories = new Categories();
		filter.setCategories(categories);
		Categories.Items cItems = new Categories.Items();
		categories.setItems(cItems);
		Categories.Items.Item cIt = new Categories.Items.Item();
		cIt.setId(new CyberTrackerId().getNodeId());
		cIt.setName("Members"); //$NON-NLS-1$
		cIt.setCategoryId(memberNodeId);
		cIt.setFilterType(1); // 1 for "Any" filter type
		cItems.getItem().add(cIt);
		
		ElementFilters elFilter = new ElementFilters();
		filter.setElementFilters(elFilter);
		ElementFilters.Items eItems = new ElementFilters.Items();
		elFilter.setItems(eItems);
		for (int i = 0; i < memberIds.size(); i++) {
			CyberTrackerId id = memberIds.get(i);
			String name = (memberNames != null && memberNames.size() > i) ? memberNames.get(i) : null;
			ElementFilters.Items.Item eIt = new ElementFilters.Items.Item();
			eIt.setId(id.getItemId());
			eIt.setName(name);
			ElementFilters.Items.Item.CheckedElements chEl = new ElementFilters.Items.Item.CheckedElements();
			eIt.setCheckedElements(chEl);
			chEl.getValue().add(id.getItemId());
			eItems.getItem().add(eIt);
		}
		
		try {
			JAXBContext context = JAXBContext.newInstance(Filter.class);
			Marshaller marshaller = context.createMarshaller();
			final StringWriter stringWriter = new StringWriter();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(filter, stringWriter);
			String filterStr = stringWriter.toString();
			int index = filterStr.indexOf("<Filter>"); //$NON-NLS-1$
			return "<?xml version=\"1.0\"?>\r\n" + filterStr.substring(index); //$NON-NLS-1$ //this is REQUIRED as CyberTracker expects EXACTLY "<?xml version=\"1.0\"?>\r\n<Filter>" at the begining
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static String builPilotFormula(List<PatrolType> patrolTypes) {
		String result = ""; //$NON-NLS-1$
		for (int i = 0; i < patrolTypes.size(); i++) {
			patrolTypes.get(i).getType();
			switch (patrolTypes.get(i).getType()) {
			case AIR:
			case MARINE:
				if (!result.isEmpty())
					result += " || "; //$NON-NLS-1$
				result += GLOBAL_PATROL_TYPE+"=="+String.valueOf(i+1); //$NON-NLS-1$
				break;
			default:
				break;
			}
		}
		return result;
	}
	
	private static void addNavigationFormula(Node node, String formula, String successId, String failId) {
		Control formulaControl = ScreensObjectFactory.createFormulaControl12(formula, failId, successId);
		node.getData().getControls().getControl().add(formulaControl);
	}
	
}
