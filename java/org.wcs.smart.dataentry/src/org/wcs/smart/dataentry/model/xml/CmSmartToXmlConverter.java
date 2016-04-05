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
package org.wcs.smart.dataentry.model.xml;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmDmAttributeSettings;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.dataentry.model.xml.generated.AttributeCmListItemTypeList;
import org.wcs.smart.dataentry.model.xml.generated.AttributeCmTreeNodeTypeList;
import org.wcs.smart.dataentry.model.xml.generated.AttributeOptionType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeType;
import org.wcs.smart.dataentry.model.xml.generated.CmDmAttributeSettingsType;
import org.wcs.smart.dataentry.model.xml.generated.CmDmAttributeSettingsTypeList;
import org.wcs.smart.dataentry.model.xml.generated.LanguageListType;
import org.wcs.smart.dataentry.model.xml.generated.LanguageType;
import org.wcs.smart.dataentry.model.xml.generated.ListItemType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;
import org.wcs.smart.dataentry.model.xml.generated.NodeType;
import org.wcs.smart.dataentry.model.xml.generated.NodeTypeList;
import org.wcs.smart.dataentry.model.xml.generated.TreeNodeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Converts a database configurable model to the xml representation 
 * of the configurable model.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CmSmartToXmlConverter {

	public static org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel convert(ConfigurableModel cm, IProgressMonitor monitor) {
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml = new org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel();

		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			cm = DataentryHibernateManager.getFullConfigurableModel(cm.getUuid(), session);
			if (monitor.isCanceled()) return null;
			
			monitor.subTask(Messages.CmSmartToXmlConverter_ProcessLanguages);
			HashMap<String, Language> llookup = processLanguages(cm, xml);
			setNames(xml.getName(), cm.getNames(), llookup);
			if (cm.getDisplayMode() != null) {
				xml.setDisplayMode(cm.getDisplayMode().name());
			}
			monitor.worked(1);
			if (monitor.isCanceled()) return null;
			
			monitor.subTask(Messages.CmSmartToXmlConverter_ProcessCmNodes);
			processCmNodes(cm, xml, llookup, session, monitor);
			monitor.worked(1);
			if (monitor.isCanceled()) return null;
			
			monitor.subTask(Messages.CmSmartToXmlConverter_ProcessListItems);
			processDefaultListItems(cm, xml, llookup, monitor);
			monitor.worked(1);
			if (monitor.isCanceled()) return null;

			monitor.subTask(Messages.CmSmartToXmlConverter_ProcessTreeNodes);
			processDefaultTreeNodes(cm, xml, llookup, monitor);
			monitor.worked(1);
			if (monitor.isCanceled()) return null;

			monitor.subTask(Messages.CmSmartToXmlConverter_ProcessAttributeSettings);
			processCmDmAttributeSettings(cm, xml, llookup, monitor);
			monitor.worked(1);
			if (monitor.isCanceled()) return null;
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		return xml;
	}

	private static void processDefaultTreeNodes(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml,
			HashMap<String, Language> llookup, IProgressMonitor monitor) {

		AttributeCmTreeNodeTypeList defTree = new AttributeCmTreeNodeTypeList();
		xml.setDefaultTrees(defTree);
		processCmTreeNodes(cm.getDefaultTrees(), defTree.getTreeNode(), llookup, monitor);
	}

	private static void processCmTreeNodes(List<CmAttributeTreeNode> cmList,
			List<TreeNodeType> xmlList, HashMap<String, Language> llookup, IProgressMonitor monitor) {
		if(monitor.isCanceled()) return;
		for (CmAttributeTreeNode cmNode : cmList) {
			TreeNodeType xmlNode = new TreeNodeType();
			setNames(xmlNode.getName(), cmNode.getNames(), llookup);
			xmlNode.setIsActive(cmNode.getIsActive());
			if (cmNode.getDmTreeNode() == null){
				xmlNode.setKeyRef(null);
				xmlNode.setHkeyRef(null);
			}else{
				xmlNode.setKeyRef(cmNode.getDmTreeNode().getKeyId());
				xmlNode.setHkeyRef(cmNode.getDmTreeNode().getHkey());
			}
			Attribute dmAttribute = cmNode.getDmAttribute();
			if (dmAttribute != null) {
				xmlNode.setAttributeKey(dmAttribute.getKeyId());
			}
			if (cmNode.getDisplayMode() != null) {
				xmlNode.setDisplayMode(cmNode.getDisplayMode().name());
			}
			xmlNode.setImageFile(getImageFileRef(cmNode));
			processCmTreeNodes(cmNode.getChildren(), xmlNode.getChildren(), llookup, monitor);
			xmlList.add(xmlNode);
		}
	}

	private static void processDefaultListItems(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml,
			HashMap<String, Language> llookup, IProgressMonitor monitor) {

		AttributeCmListItemTypeList defList = new AttributeCmListItemTypeList();
		xml.setDefaultLists(defList);
		processCmListItems(cm.getDefaultLists(), defList.getListItem(), llookup, monitor);
	}
	
	private static void processCmListItems(List<CmAttributeListItem> cmList,
			List<ListItemType> xmlList,	HashMap<String, Language> llookup, IProgressMonitor monitor) {

		if(monitor.isCanceled()) return;
		for (CmAttributeListItem cmNode : cmList) {
			ListItemType xmlNode = new ListItemType();
			setNames(xmlNode.getName(), cmNode.getNames(), llookup);
			xmlNode.setIsActive(cmNode.getIsActive());
			if (cmNode.getListItem() == null){
				xmlNode.setKeyRef(null);
			}else{
				xmlNode.setKeyRef(cmNode.getListItem().getKeyId());
			}
			Attribute dmAttribute = cmNode.getDmAttribute();
			if (dmAttribute != null) {
				xmlNode.setAttributeKey(dmAttribute.getKeyId());
			}
			xmlNode.setImageFile(getImageFileRef(cmNode));
			xmlList.add(xmlNode);
		}
	}

	private static void processCmNodes(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml,
			HashMap<String, Language> llookup, Session session, IProgressMonitor monitor) {

		NodeTypeList ntl = new NodeTypeList();
		xml.setNodes(ntl);
		if (cm.getNodes() != null) {
			for (CmNode node : cm.getNodes()){
				processCmNode(node, ntl.getNode(), llookup, session, monitor);
			}
		}
	}

	private static void processCmNode(CmNode node, List<NodeType> xmlNodes,
			HashMap<String, Language> llookup, Session session, IProgressMonitor monitor) {

		monitor.subTask(MessageFormat.format(Messages.CmSmartToXmlConverter_ProcessingNode, node.getName()));
		NodeType nt = new NodeType();
		setNames(nt.getName(), node.getNames(), llookup);
		if (node.getCategory() != null) {
			nt.setCategoryKey(node.getCategory().getKeyId());
			nt.setCategoryHkey(node.getCategory().getHkey());
		}
		nt.setPhotoAllowed(node.isPhotoAllowed());
		nt.setPhotoRequired(node.isPhotoRequired());
		if (node.getDisplayMode() != null) {
			nt.setDisplayMode(node.getDisplayMode().name());
		}
		nt.setImageFile(getImageFileRef(node));
		
		if (node.getCmAttributes() != null){
			for (CmAttribute ca : node.getCmAttributes()) {
				AttributeType at = new AttributeType();
				setNames(at.getName(), ca.getNames(), llookup);
				at.setAttributeKey(ca.getAttribute().getKeyId());
				for (CmAttributeOption option : ca.getCmAttributeOptions().values()) {
					AttributeOptionType aot = new AttributeOptionType();
					aot.setId(option.getOptionId());
					aot.setStringValue(option.getStringValue());
					aot.setDoubleValue(option.getDoubleValue());
					if (option.getUuidValue() != null) {
						switch (ca.getAttribute().getType()) {
						case LIST:
						{
							Criteria query = session.createCriteria(AttributeListItem.class).add(Restrictions.eq("uuid", option.getUuidValue())); //$NON-NLS-1$
							AttributeListItem item = (AttributeListItem) query.uniqueResult();
							if (item != null) {
								aot.setKeyRef(item.getKeyId());
								aot.setHkeyRef(null); //not relevant for list items attributes
							}
							break;
						}
						case TREE:
						{
							Criteria query = session.createCriteria(AttributeTreeNode.class).add(Restrictions.eq("uuid", option.getUuidValue())); //$NON-NLS-1$
							AttributeTreeNode item = (AttributeTreeNode) query.uniqueResult();
							if (item != null) {
								aot.setKeyRef(item.getKeyId());
								aot.setHkeyRef(item.getHkey());
							}
							break;
						}
						default:
							break;
						}
					}
					at.getOption().add(aot);
				}
				processCmListItems(ca.getList(), at.getListItem(), llookup, monitor);
				processCmTreeNodes(ca.getTree(), at.getTreeNode(), llookup, monitor);
				nt.getAttribute().add(at);
			}
		}
		
		if (node.getChildren() != null) {
			for (CmNode cn : node.getChildren()) {
				processCmNode(cn, nt.getNode(), llookup, session, monitor);
				if (monitor.isCanceled()) return;
			}
		}
		xmlNodes.add(nt);
	}

	private static void processCmDmAttributeSettings(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml,
			HashMap<String, Language> llookup, IProgressMonitor monitor) {
		if (!cm.getAttributeSettings().isEmpty()) {
			CmDmAttributeSettingsTypeList xmlList = new CmDmAttributeSettingsTypeList();
			for (CmDmAttributeSettings s : cm.getAttributeSettings().values()) {
				CmDmAttributeSettingsType sXml = new CmDmAttributeSettingsType();
				sXml.setAttributeKey(s.getDmAttribute().getKeyId());
				if (s.getDisplayMode() != null) {
					sXml.setDisplayMode(s.getDisplayMode().name());
				}
				xmlList.getSetting().add(sXml);
			}
			xml.setSetting(xmlList);
		}
	}

	
	private static HashMap<String, Language> processLanguages(ConfigurableModel cm, org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml) {
		HashMap<String, Language> lookup = new HashMap<String, Language>();
		LanguageListType llt = new LanguageListType();
		xml.setLanguages(llt);
		for (Language ll : cm.getConservationArea().getLanguages()) {
			LanguageType lt = new LanguageType();
			lt.setCode(ll.getCode());
			llt.getLanguage().add(lt);
			lookup.put(UuidUtils.uuidToString(ll.getUuid()), ll);
		}	
		return lookup;
	}
	
	private static void setNames(List<NameType> list, Set<Label> names, HashMap<String, Language> llookup){
		if (names == null){
			return;
		}
		for (Label lbl: names){
			NameType nt = new NameType();
			nt.setValue(lbl.getValue());
			nt.setLanguageCode(llookup.get(UuidUtils.uuidToString(lbl.getLanguage().getUuid())).getCode());
			list.add(nt);
		}
	}

	private static String getImageFileRef(IImageAssociatedObject obj) {
		File file = obj.getImageFile();
		return (file != null && file.exists()) ? file.getName() : null;
	}
	
}
