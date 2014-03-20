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
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.generated.AttributeItemType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeOptionType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeType;
import org.wcs.smart.dataentry.model.xml.generated.LanguageType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;
import org.wcs.smart.dataentry.model.xml.generated.NodeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.OptionSelectionDialog;

/**
 * Converts a SMART XML configurable model to the database
 * configurable model.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CmXmlToSmartImporter {
	
	private Session session;
	private String useAsDefault;

	private Map<String, Language> langLookup;
	private Map<String, Category> catLookup;
	private Map<String, Attribute> attrLookup;
	private Map<String, AttributeListItem> listItemLookup;
	private Map<String, AttributeTreeNode> treeNodeLookup;

	public void importXml(File xmlFile, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.CmXmlToSmartImporter_ImportingFromXml, 3);
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlCm = null;
		FileInputStream in = new FileInputStream(xmlFile);
		try {
			monitor.subTask(Messages.CmXmlToSmartImporter_Reading);
			xmlCm = CmXmlManager.readDataModel(in);
			monitor.worked(1);
		} finally {
			in.close();
		}
		if (xmlCm == null) {
			throw new Exception(Messages.CmXmlToSmartImporter_ReadFile_Error);
		}
		convertAndSave(xmlCm, monitor);
	}

	private void convertAndSave(org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlCm, IProgressMonitor monitor) {
		catLookup = new HashMap<String, Category>();
		attrLookup = new HashMap<String, Attribute>();
		listItemLookup = new HashMap<String, AttributeListItem>();
		treeNodeLookup = new HashMap<String, AttributeTreeNode>();
		
		session = HibernateManager.openSession(new AttachmentInterceptor());
		session.beginTransaction();
		try {
			langLookup = new HashMap<String, Language>();
			for (Language lang : SmartDB.getCurrentConservationArea().getLanguages()) {
				langLookup.put(lang.getCode(), lang);
			}
			
			useAsDefault = checkLanguage(xmlCm.getLanguages().getLanguage(), SmartDB.getCurrentConservationArea());
			if (useAsDefault == null){
				throw new IllegalStateException(Messages.CmXmlToSmartImporter_DefaultLanguage_Error);
			}

			ConfigurableModel cm = new ConfigurableModel();
			cm.setConservationArea(SmartDB.getCurrentConservationArea());
			updateNames(cm, xmlCm.getName());
			
			cm.setNodes(processCmNodes(xmlCm.getNodes().getNode(), cm, null, monitor));
			
			monitor.subTask(Messages.CmXmlToSmartImporter_ImportingListItems);
			List<CmAttributeListItem> listItems = processListItems(xmlCm.getListItems().getItem(), cm);
			monitor.subTask(Messages.CmXmlToSmartImporter_ImportingTreeNodes);
			List<CmAttributeTreeNode> treeNodes = processTreeNodes(xmlCm.getTreeNodes().getNode(), cm);
			
			monitor.subTask(Messages.CmXmlToSmartImporter_Saving);
			session.save(cm);
			for (CmAttributeListItem item : listItems) {
				session.save(item);
			}
			for (CmAttributeTreeNode node : treeNodes) {
				session.save(node);
			}
			session.getTransaction().commit();
			
		} finally {
			langLookup = null;
			useAsDefault = null;
			catLookup = null;
			attrLookup = null;
			listItemLookup = null;
			treeNodeLookup = null;
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
			session = null;
		}
	}

	private List<CmAttributeListItem> processListItems(List<AttributeItemType> xmlItems, ConfigurableModel cm) {
		List<CmAttributeListItem> result = new ArrayList<CmAttributeListItem>();
		for (AttributeItemType xmlItem : xmlItems) {
			CmAttributeListItem item = new CmAttributeListItem();
			item.setConfigurableModel(cm);
			updateNames(item, xmlItem.getName());
			item.setIsActive(xmlItem.isIsActive());
			item.setListItem(fetchAttributeListItem(xmlItem.getRefKey(), fetchAttribute(xmlItem.getAttributeKey())));
			result.add(item);
		}
		return result;
	}

	private List<CmAttributeTreeNode> processTreeNodes(List<AttributeItemType> xmlItems, ConfigurableModel cm) {
		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
		for (AttributeItemType xmlItem : xmlItems) {
			CmAttributeTreeNode item = new CmAttributeTreeNode();
			item.setConfigurableModel(cm);
			updateNames(item, xmlItem.getName());
			item.setIsActive(xmlItem.isIsActive());
			item.setDmTreeNode(fetchAttributeTreeNode(xmlItem.getRefKey(), fetchAttribute(xmlItem.getAttributeKey())));
			result.add(item);
		}
		return result;
	}

	private List<CmNode> processCmNodes(List<NodeType> xmlNodes, ConfigurableModel cm, CmNode parent, IProgressMonitor monitor) {

		List<CmNode> result = new ArrayList<CmNode>();
		
		for (int i = 0; i < xmlNodes.size(); i++) {
			NodeType xmlNode = xmlNodes.get(i);
			
			CmNode cmNode = new CmNode();
			cmNode.setModel(cm);
			updateNames(cmNode, xmlNode.getName());
			monitor.subTask(MessageFormat.format(Messages.CmXmlToSmartImporter_ImportingNode, cmNode.findName(langLookup.get(useAsDefault))));
			cmNode.setCategory(fetchCategory(xmlNode.getCategoryKey()));
			cmNode.setPhotoAllowed(xmlNode.isPhotoAllowed());
			cmNode.setPhotoRequired(xmlNode.isPhotoRequired());
			cmNode.setNodeOrder(i);
			cmNode.setParent(parent);
			cmNode.setChildren(processCmNodes(xmlNode.getNode(), cm, cmNode, monitor));
			cmNode.setCmAttributes(processAttributes(xmlNode.getAttribute(), cmNode));
			
			result.add(cmNode);
		}
		return result;
	}
	
	private List<CmAttribute> processAttributes(List<AttributeType> xmlAttrList, CmNode parent) {

		List<CmAttribute> result = new ArrayList<CmAttribute>();
		
		for (int i = 0; i < xmlAttrList.size(); i++) {
			AttributeType xmlAttr = xmlAttrList.get(i);
			
			CmAttribute cmAttr = new CmAttribute();
			cmAttr.setNode(parent);
			updateNames(cmAttr, xmlAttr.getName());
			cmAttr.setAttribute(fetchAttribute(xmlAttr.getAttributeKey()));
			cmAttr.setOrder(i);
			cmAttr.setCmAttributeOptions(processAttributeOptions(xmlAttr.getOption(), cmAttr));

			result.add(cmAttr);
		}
		return result;
	}

	private Map<String, CmAttributeOption> processAttributeOptions(List<AttributeOptionType> xmlOptionList, CmAttribute parent) {

		Map<String, CmAttributeOption> result = new HashMap<String, CmAttributeOption>();
		for (AttributeOptionType xmlOption : xmlOptionList) {
			CmAttributeOption cmOption = new CmAttributeOption();
			cmOption.setCmAttribute(parent);
			cmOption.setOptionId(xmlOption.getId());
			cmOption.setStringValue(xmlOption.getStringValue());
			cmOption.setDoubleValue(xmlOption.getDoubleValue());
			if (xmlOption.getKeyRef() != null && parent.getAttribute() != null) {
				switch (parent.getAttribute().getType()) {
				case LIST:
				{
					AttributeListItem item = fetchAttributeListItem(xmlOption.getKeyRef(), parent.getAttribute());
					if (item != null) {
						cmOption.setUuidValue(item.getUuid());
					}
					break;
				}
				case TREE:
				{
					AttributeTreeNode item = fetchAttributeTreeNode(xmlOption.getKeyRef(), parent.getAttribute());
					if (item != null) {
						cmOption.setUuidValue(item.getUuid());
					}
					break;
				}
				default:
					break;
				}
			}

			result.put(cmOption.getOptionId(), cmOption);
		}
		return result;
	}

	private Category fetchCategory(String key) {
		if (key == null || key.isEmpty())
			return null;
		Category c = catLookup.get(key);
		if (c == null) {
			Criteria query = session.createCriteria(Category.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.eq("keyId", key)); //$NON-NLS-1$
			c = (Category) query.uniqueResult();
			catLookup.put(key, c);
		}
		return c;
	}

	private Attribute fetchAttribute(String key) {
		if (key == null || key.isEmpty())
			return null;
		Attribute a = attrLookup.get(key);
		if (a == null) {
			Criteria query = session.createCriteria(Attribute.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.eq("keyId", key)); //$NON-NLS-1$
			a = (Attribute) query.uniqueResult();
			attrLookup.put(key, a);
		}
		return a;
	}

	private AttributeListItem fetchAttributeListItem(String key, Attribute attribute) {
		if (key == null || key.isEmpty() || attribute == null)
			return null;
		AttributeListItem a = listItemLookup.get(key);
		if (a == null) {
			Criteria query = session.createCriteria(AttributeListItem.class)
					.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
					.add(Restrictions.eq("keyId", key)); //$NON-NLS-1$
			a = (AttributeListItem) query.uniqueResult();
			listItemLookup.put(key, a);
		}
		return a;
	}

	private AttributeTreeNode fetchAttributeTreeNode(String key, Attribute attribute) {
		if (key == null || key.isEmpty() || attribute == null)
			return null;
		AttributeTreeNode a = treeNodeLookup.get(key);
		if (a == null) {
			Criteria query = session.createCriteria(AttributeTreeNode.class)
					.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
					.add(Restrictions.eq("keyId", key)); //$NON-NLS-1$
			a = (AttributeTreeNode) query.uniqueResult();
			treeNodeLookup.put(key, a);
		}
		return a;
	}
	
	/**
	 * Returns the language code to use as the default language.
	 * <p>
	 * Any labels with the selected language code should be applied 
	 * to the default language of the conservation area.
	 * </p>
	 * @param xmlLanguages
	 * @param targetCa
	 * @return null if should not continue (no default language found); otherwise
	 * the language code to use as the default ca language
	 */
	private String checkLanguage(List<LanguageType> xmlLanguages, final ConservationArea targetCa){
		//here we check to ensure default ca lang
		for (LanguageType lt : xmlLanguages){
			if (lt.getCode().equals(targetCa.getDefaultLanguage().getCode())){
				return lt.getCode();
			}
		}
		
		final String[] values = new String[xmlLanguages.size()];
		for (int i = 0; i < values.length; i ++){
			values[i] = xmlLanguages.get(i).getCode();
		}
		final String[] selected = new String[1];
		selected[0] = null;
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				OptionSelectionDialog sd = new OptionSelectionDialog(Display.getDefault().getActiveShell(), values,
						Messages.CmXmlToSmartImporter_LanguageSelect_Title, 
						MessageFormat.format(Messages.CmXmlToSmartImporter_LanguageSelect_Message, targetCa.getDefaultLanguage().getCode()));
				if (sd.open() != IDialogConstants.OK_ID) {
					selected[0] = null;
				}else{
					selected[0] = (String)((StructuredSelection)sd.getSelection()).getFirstElement();
				}
				
			}});
		
		return selected[0];
	}

	/*
	 * updates the names associated with a data model object
	 */
	private void updateNames(NamedItem dmobject, List<NameType> names){
		for (NameType nameType : names) {
			String code = nameType.getLanguageCode();
			String value = nameType.getValue();
			
			Language lang = langLookup.get(code);
			//if language not found we ignore; this should be dealt with earlier
			if (lang != null){
				dmobject.updateName(lang, value);
			}
			
			if (useAsDefault != null && useAsDefault.equals(nameType.getLanguageCode())){
				dmobject.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), value);
				
			}
		}
		
	}
	
}
