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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;

/**
 * Util for creating patrol screens for CyberTracker.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolScreensUtil {

	/**
	 * @param screens
	 * @param element
	 * @return root id
	 */
	public static CyberTrackerId addPatrolNodes(List<Node> nodes, Elements elements, CyberTrackerId dmRootId, Session session) {
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		CyberTrackerId id = addSimpleNextRadioNode(startId, nodes, elements, "SMART CyberTracker", "#PatrolStart", "Start New Patrol");
		//patrol type & transport
		id = addTypeTransportNodes(id, nodes, elements, session);
		//patrol armed
		id = addSimpleNextRadioNode(id, nodes, elements, "Is Armed", "#Armed", "Yes", "No");
		
		List<Team> teams = PatrolHibernateManager.getActiveTeams(ca, session);
		id = addSimpleNextRadioNode(id, nodes, elements, "Team", "#Team", toNamesArray(teams));

		List<Station> stations = PatrolHibernateManager.getActiveStations(ca, session);
		id = addSimpleNextRadioNode(id, nodes, elements, "Station", "#Station", toNamesArray(stations));

		List<PatrolMandate> mandates = PatrolHibernateManager.getActiveMandates(ca, session);
		id = addSimpleNextRadioNode(id, nodes, elements, "Mandate", "#Mandate", toNamesArray(mandates));

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
		
		id = addMembersNode(id, nodes, memberIds);
		id = addSimpleNextRadioNode(id, nodes, elements, "Leader", "#Leader", memberIds);
		id = addSimpleNextRadioNode(id, nodes, elements, "Pilot", "#Pilot", memberIds);
		
//		addSimpleNextRadioNode(id, nodes, elements, "Next Task", "#Task", "Make Observation", "End Patrol");
//		Control control2 = nodes.get(nodes.size()-1).getData().getControls().getControl().get(0);
//		control2.setTranslateNextScreenId(dmRootId.getNodeId());
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
		Control control2 = node.getData().getControls().getControl().get(0);
		control2.setTranslateNextScreenId(nextId.getNodeId());
		return nextId;
	}
	
	private static CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, List<Node> nodes, Elements elements, String name, String resultElName,  String... options) {
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, options);
		return addSimpleNextRadioNode(id, nodes, elements, name, resultElName, ids);
	}

	private static CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, List<Node> nodes, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids) {
		String resultId = createResultElement(resultElName, elements);
		Node node = CyberTrackerUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		nodes.add(node);
		return toNextScreen(node);
	}
	
	private static CyberTrackerId addNoteNextNode(CyberTrackerId id, List<Node> nodes, Elements elements, String name, String resultElName) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ScreensObjectFactory.createNodeNote(id.getNodeId(), name,  resultId);
		nodes.add(node);
		return toNextScreen(node);
	}

	private static CyberTrackerId addTypeTransportNodes(CyberTrackerId id, List<Node> nodes, Elements elements, Session session) {
		List<PatrolType> pTypes = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
		List<String> types = new ArrayList<String>();
		for (PatrolType patrolType : pTypes) {
			types.add(patrolType.getType().getGuiName());
		}
		List<CyberTrackerId> typeIds = ElementsUtil.addCustomElements(elements, types.toArray(new String[types.size()]));
		String resultElemId = createResultElement("#PatrolType", elements);
		Node node = CyberTrackerUtil.createRadioNode(id.getNodeId(), "Patrol Type", typeIds, resultElemId, true);
		nodes.add(node);
		CyberTrackerId nextId = new CyberTrackerId();
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, "#PatrolTransport", resultId.getItemId()); //$NON-NLS-1$
		for (int i = 0; i < pTypes.size(); i++) {
			List<CyberTrackerId> trIds = ElementsUtil.addCustomElements(elements, toNamesArray(pTypes.get(i).getTransportTypes()));
			node = CyberTrackerUtil.createRadioNode(typeIds.get(i).getNodeId(), types.get(i), trIds, resultId.getItemId());
			nodes.add(node);
			Control control2 = node.getData().getControls().getControl().get(0);
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
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, "Make Observation", "End Patrol");
		List<String> values = CyberTrackerUtil.listItemIds(ids);
		String trElements = CyberTrackerUtil.translateElements(ids);
		//custom translate links logic
		StringBuilder links = new StringBuilder();
		// "Make observations" leads to datamodel root
		links.append(ids.get(0).getItemTranslatedId()).append(dmRootId.getNodeTranslatedId());
		// "End Patrol" leads to starting point
		links.append(ids.get(1).getItemTranslatedId()).append(startId.getNodeTranslatedId());
		Node node = ScreensObjectFactory.createNodeRadio(id.getNodeId(), "Next Task", values, trElements, links.toString(), null);
		nodes.add(node);
	}
	
	public static String[] toNamesArray(List<? extends SimpleListItem> items) {
		List<String> result = new ArrayList<String>();
		for (SimpleListItem i : items) {
			result.add(i.getName());
		}
		return result.toArray(new String[result.size()]);
	}
	
}
