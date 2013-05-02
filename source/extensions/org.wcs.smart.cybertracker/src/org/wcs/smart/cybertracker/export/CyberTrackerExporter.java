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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Exporter to CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerExporter {

	public static File export(File file, IProgressMonitor monitor) throws Exception {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			return performExport(file, monitor, session);
		} finally{
			session.getTransaction().rollback();
			session.close();
		}
	}
		
	private static File performExport(File file, IProgressMonitor monitor, Session session) throws Exception {
		DataModel dataModel = getDataModel(session);
		Category root = CyberTrackerUtil.buildRoot(dataModel);
		Map<Category, CyberTrackerId> keyMap = CyberTrackerUtil.buildMap(root);
		
		List<Node> screenNodes = buildScreenNodes(root, keyMap);
		Screens screens = ScreensObjectFactory.createScreens(screenNodes);
		BufferedOutputStream outS = new BufferedOutputStream(new FileOutputStream("c:/dev/CyberTracker/out/Screens.xml")); //$NON-NLS-1$
		try {
			writeDataModel(screens, outS, Screens.class);
		} finally {
			outS.close();
		}
		
		Elements elements = buildEmptyElements();
		addElements(elements, root, keyMap);
		BufferedOutputStream outE = new BufferedOutputStream(new FileOutputStream("c:/dev/CyberTracker/out/Elements.xml")); //$NON-NLS-1$
		try {
			writeDataModel(elements, outE, Elements.class);
		} finally {
			outE.close();
		}
		
		return file;
	}

	private static Elements buildEmptyElements() {
		Elements elements = new Elements();
		Elements.List list = new Elements.List();
		elements.setList(list);
		Elements.List.Items items = new Elements.List.Items();
		list.setItems(items);
		return elements;
	}

	private static List<Node> buildScreenNodes(Category category, Map<Category, CyberTrackerId> keyMap) {
		List<Node> result = new ArrayList<Node>();
		if (category == null)
			return result;
		//result.add(CyberTrackerUtil.createRadioNode(category, keyMap));
		
		if (category.getChildren() == null || category.getChildren().isEmpty()) {
			result.addAll(buildAttributeNodes(category, keyMap));
			return result;
		}
		result.add(CyberTrackerUtil.createRadioNode(category, keyMap));
		
		for (Category child : category.getChildren()) {
			result.addAll(buildScreenNodes(child, keyMap));
		}		
		return result;
	}
	
	private static List<Node> buildAttributeNodes(Category category, Map<Category, CyberTrackerId> keyMap) {
		List<Attribute> attrList = new ArrayList<Attribute>();
		category.getAllAttribute(attrList, true);
		List<Node> result = new ArrayList<Node>();
		CyberTrackerId id = keyMap.get(category);
		for (Attribute attribute : attrList) {
			switch (attribute.getType()) {
			case NUMERIC:
				result.add(ScreensObjectFactory.createNodeNumber(id.getNodeId(), attribute.getName()));
				id = new CyberTrackerId();
				break;
			case TEXT:
				result.add(ScreensObjectFactory.createNodeNote(id.getNodeId(), attribute.getName()));
				id = new CyberTrackerId();
				break;
			case LIST:
				break;
			case TREE:
				break;
			case BOOLEAN:
				//TODO: implement!!!!
				//result.add(ScreensObjectFactory.createNodeRadio(id, name, values, trElements, trLinks);
				break;
			}
		}
		return result;
	}

	private static void addElements(Elements elements, Category category, Map<Category, CyberTrackerId> keyMap) {
		Elements.List.Items.Item item = new Elements.List.Items.Item();
		item.setName(category.getName());
		item.setId(keyMap.get(category).getItemId());
		elements.getList().getItems().getItem().add(item);
		if (category.getChildren() != null) {
			for (Category child : category.getChildren()) {
				addElements(elements, child, keyMap);
			}
		}
	}

	private static void writeDataModel(Object obj, OutputStream file, Class<?> clazz) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(clazz);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(obj, file);
	}

	private static DataModel getDataModel(Session session) {
		DataModel dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		//load into memory; no-lazy loading here.
		for (Category cat: dataModel.getCategories()){
			visitCategory(cat);
		}
		for (Attribute att: dataModel.getAttributes()){
			att.getAggregations().size();
		}
		return dataModel;
	}
	
	private static void visitCategory(Category cat){
		for (Category child : cat.getActiveChildren()){
			visitCategory(child);
			child.getName();
		}
		for (CategoryAttribute ca: cat.getAttributes()){
			ca.getAttribute().getName();
		}	
	}	
}
