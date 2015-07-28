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
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.MetaExportResult.IdNamePair;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.filter.Categories;
import org.wcs.smart.cybertracker.model.filter.ElementFilters;
import org.wcs.smart.cybertracker.model.filter.Filter;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.util.SmartUtils;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author elitvin
 * @since 4.0.0
 */
public class ScreensUtil {

	private ScreensObjectFactory screensFactory;
	private CyberTrackerUtil ctUtil;
	
	protected ScreensUtil(CyberTrackerUtil ctUtil) {
		this.ctUtil = ctUtil;
		this.screensFactory = ctUtil.getScreensFactory();
	}
	
	/**
	 * @param name
	 * @param elements
	 * @return String id of newly created element
	 */
	protected String createResultElement(String name, Elements elements) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId());
		return resultId.getItemId();
	}

	protected String createDefaultResultElement(String name, Elements elements, String defaultValue) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId(), null, null, defaultValue);
		return resultId.getItemId();
	}
	
	protected CyberTrackerId toNextScreen(Node node) {
		return toNextScreen(node, false);
	}
	
	protected CyberTrackerId toNextScreen(Node node, boolean canSkip) {
		CyberTrackerId nextId = new CyberTrackerId();
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		control2.setTranslateNextScreenId(nextId.getNodeId());
		if (canSkip) {
			//we should be here only for radio nodes if we want to allow user press "Next" without selecting anything
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
		}
		return nextId;
	}

	protected void applyFilter(Node node, String filter) {
		if (filter != null) {
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setFilterEnabled("True"); //$NON-NLS-1$
			control7.setTranslateFilter(filter);
		}
	}
	
	protected CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, boolean canSkip) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ctUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node, canSkip);
	}

	protected CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, String filter) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ctUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		applyFilter(node, filter);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node);
	}
	
	protected CyberTrackerId addNoteNextNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName, int maxLength) {
		String resultId = createResultElement(resultElName, elements);
		Node node = screensFactory.createNodeNote(id.getNodeId(), name,  resultId);
		
		Control textControl = ScreensObjectFactory.getNoteMainControl(node);
		textControl.setMaxLength(maxLength);
		
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node);
	}
	
	protected CyberTrackerId addMembersNode(CyberTrackerId id, MetaExportResult container, List<CyberTrackerId> memberIds) {
		List<String> values = ctUtil.listItemIds(memberIds);
		String trElements = ctUtil.translateElements(memberIds);
		String trLinks = ctUtil.translateLinks(memberIds, false);
		Node node = screensFactory.createNodeMultiList(id.getNodeId(), Messages.PatrolScreens_Members, values, trElements, trLinks, 1, false);
		container.screenNodes.add(node);
		return toNextScreen(node);
	}
	
	public List<CyberTrackerId> toCyberTrackerIds(Elements elements, List<? extends NamedItem> items) {
		List<String> labelValues = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (NamedItem i : items) {
			labelValues.add(ctUtil.getName(i));
			tag0Values.add(SmartUtils.encodeHex(i.getUuid()));
		}
		return ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
	}

	protected String buildMembersFilter(String memberNodeId, List<CyberTrackerId> memberIds, List<String> memberNames) {
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
			CyberTrackerPlugIn.log(e.getMessage(), e);
		}
		return null;
	}
	
	protected void addNavigationFormula(Node node, String formula, String successId, String failId) {
		Control formulaControl = screensFactory.createNavFormulaControl12(formula, failId, successId);
		node.getData().getControls().getControl().add(formulaControl);
	}

	protected void addGpsConfiguration(Node node, CyberTrackerProperties props) {
		addGpsConfiguration(node, props, null);
	}
	
	protected void addGpsConfiguration(Node node, CyberTrackerProperties props, Integer timerOverride) {
		Control gpsConf = screensFactory.createConfigureGPSControl13(props);
		if (timerOverride != null) {
			gpsConf.setWaypointTimer(timerOverride);
		}
		node.getData().getControls().getControl().add(gpsConf);
	}
	
	protected void addUniqueAttrubute(Node node, String resultElementId) {
		Control uniqueAttr = screensFactory.createAttrubuteControl14(resultElementId, true, null);
		ScreensObjectFactory.addControlToNode(node, uniqueAttr);
	}

	protected void addStartTimeAttrubute(Node node, String resultDateId, String resultTimeId) {
		Control dtAttr = screensFactory.createSnapDateTimeControl15(resultDateId, resultTimeId);
		ScreensObjectFactory.addControlToNode(node, dtAttr);
	}
	
	protected void addGPSControl(Node node) {
		Control gpsControl = screensFactory.createGPSControl16();
		ScreensObjectFactory.addControlToNode(node, gpsControl);
	}

	protected void addGPSRequiredWarning(Node node) {
		Control msgControl = screensFactory.createBottomMemoControl17(Messages.PatrolScreens_Begin_GPSRequiredMessage);
		ScreensObjectFactory.addControlToNode(node, msgControl);
	}

}
