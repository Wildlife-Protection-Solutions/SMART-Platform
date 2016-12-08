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
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.dialog.AssociatedImageInterceptor;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmDmAttributeSettings;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.external.IXmlCmExtraDataContribution;
import org.wcs.smart.dataentry.model.xml.generated.AttributeItemType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeOptionType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeType;
import org.wcs.smart.dataentry.model.xml.generated.CmDmAttributeSettingsType;
import org.wcs.smart.dataentry.model.xml.generated.CmDmAttributeSettingsTypeList;
import org.wcs.smart.dataentry.model.xml.generated.LanguageType;
import org.wcs.smart.dataentry.model.xml.generated.ListItemType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;
import org.wcs.smart.dataentry.model.xml.generated.NodeType;
import org.wcs.smart.dataentry.model.xml.generated.TreeNodeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.OptionSelectionDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartFileUtils;
import org.wcs.smart.util.ZipUtil;

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
	private Map<String, File> fileLookup;
	
	private List<String> warnings;
	private Map<String, UuidItem> dataMap;

	/**
	 * 
	 * @param xmlFile
	 * @param monitor
	 * @return null if error or monitor cancelled
	 * @throws Exception
	 */
	public ConfigurableModel importZip(File zipFile, IProgressMonitor monitor) throws Exception {
		File tempFolder = null;
		try {
			monitor.beginTask(Messages.CmXmlToSmartImporter_ExtractingZip, 1);
			tempFolder = SmartFileUtils.createTempDirectory("smart_cm_import"); //$NON-NLS-1$
			ZipUtil.unzipFolder(zipFile, tempFolder);
			File xmlFile = null;
			fileLookup = new HashMap<String, File>();
			for (File file : tempFolder.listFiles()) {
				fileLookup.put(file.getName(), file);
				if (file.getName().endsWith(".xml")) { //$NON-NLS-1$
					xmlFile = file;
				}
			}
			if (xmlFile != null) {
				return importXml(xmlFile, monitor);
			}
		} finally  {
			fileLookup = null;
			SmartFileUtils.deleteTempDirectory(tempFolder);
		}
		return null;
	}
	
	/**
	 * 
	 * @param xmlFile
	 * @param monitor
	 * @return null if error or monitor cancelled
	 * @throws Exception
	 */
	public ConfigurableModel importXml(File xmlFile, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.CmXmlToSmartImporter_ImportingFromXml, 3);
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlCm = null;
		
		try (FileInputStream in = new FileInputStream(xmlFile)){
			monitor.subTask(Messages.CmXmlToSmartImporter_Reading);
			xmlCm = CmXmlManager.readDataModel(in);
			monitor.worked(1);
		}
		if (monitor.isCanceled()) return null;
		if (xmlCm == null) {
			throw new Exception(Messages.CmXmlToSmartImporter_ReadFile_Error);
		}
		return convertAndSave(xmlCm, monitor);
	}

	private ConfigurableModel convertAndSave(org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlCm, IProgressMonitor monitor) {
		catLookup = new HashMap<String, Category>();
		attrLookup = new HashMap<String, Attribute>();
		listItemLookup = new HashMap<String, AttributeListItem>();
		treeNodeLookup = new HashMap<String, AttributeTreeNode>();
		warnings = new ArrayList<String>();
		dataMap = new HashMap<>();
		
		session = HibernateManager.openSession(new AssociatedImageInterceptor());
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
			cm.setDisplayMode(getDisplayMode(xmlCm.getDisplayMode()));
			
			cm.setNodes(processCmNodes(xmlCm.getNodes().getNode(), cm, null, monitor));
			
			if (monitor.isCanceled()) return null;
			
			monitor.subTask(Messages.CmXmlToSmartImporter_ImportingTreeNodes);
			if (xmlCm.getDefaultTrees() != null) {
				//tree mapping was introduced in 3.2.0, previous version were using different mapping
				cm.setDefaultTrees(processCmTreeNodes(cm, null, null, xmlCm.getDefaultTrees().getTreeNode(), monitor));
			} else if (xmlCm.getTreeNodes() != null) {
				//if we are here than we are importing data from version less than 3.2
				//need to perform additional data conversion
				List<CmAttributeTreeNode> treeNodes = processTreeNodes(xmlCm.getTreeNodes().getNode(), cm);
				cm.setDefaultTrees(CmDefaultTreesUtil.upgradeDefaultTrees(cm, treeNodes));
			}
			
			monitor.subTask(Messages.CmXmlToSmartImporter_ImportingListItems);
			if (xmlCm.getDefaultLists() != null) {
				//list mapping was introduced in 3.2.1, previous version were using different mapping
				cm.setDefaultLists(processCmListItems(cm, null, xmlCm.getDefaultLists().getListItem(), monitor));
			} else if (xmlCm.getListItems() != null) {
				//if we are here than we are importing data from version less than 3.2
				//need to perform additional data conversion
				List<CmAttributeListItem> listItems = processListItems(xmlCm.getListItems().getItem(), cm);
				cm.setDefaultLists(CmDefaultListsUtil.upgradeDefaultLists(cm, listItems));
			}

			if (monitor.isCanceled()) return null;
			monitor.subTask(Messages.CmXmlToSmartImporter_ImportAttributesSettings);
			processAttributeSettings(cm, xmlCm.getSetting());

			//converting extra data
			List<IConvertedCmExtraData> convertedExtraData = new ArrayList<IConvertedCmExtraData>();
			for (IXmlCmExtraDataContribution edc : XmlCmExtraDataContributionFactory.getContributions()) {
				IConvertedCmExtraData extraData = edc.fromXml(xmlCm.getExtraData(), dataMap, session);
				if (extraData != null) {
					if (extraData.getWarnings() != null) {
						warnings.addAll(extraData.getWarnings());
					}
					convertedExtraData.add(extraData);
				}
			}
			
			if (!warnings.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (String str: warnings){
					sb.append(str);
					sb.append(SharedUtils.LINE_SEPARATOR);
				}
				final String message = sb.toString();
				final boolean[] cont = new boolean[]{true}; 
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						ProblemInputDialog dialog = new ProblemInputDialog(
								Display.getDefault().getActiveShell(),
								Messages.CmXmlToSmartImporter_ProblemDialog_Title,
								Messages.CmXmlToSmartImporter_ProblemDialog_Message,
								message, null);
						if (dialog.open() != ProblemInputDialog.OK){
							cont[0] = false;
							
						}	
					}
				});
				if (!cont[0]) {
					return null;
				}
			}
			
			monitor.subTask(Messages.CmXmlToSmartImporter_Saving);
			session.save(cm);
			for (IConvertedCmExtraData extraData : convertedExtraData) {
				extraData.saveInTransaction(session, cm);
			}
			session.getTransaction().commit();
			return cm;
			
		} finally {
			langLookup = null;
			useAsDefault = null;
			catLookup = null;
			attrLookup = null;
			listItemLookup = null;
			treeNodeLookup = null;
			warnings = null;
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
			session = null;
		}
	}

	private List<CmAttributeTreeNode> processCmTreeNodes(ConfigurableModel cm, CmAttribute cmAttribute, CmAttributeTreeNode parent, List<TreeNodeType> xmlNodes, IProgressMonitor monitor) {
		if (monitor.isCanceled()) return null;
		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
		for (TreeNodeType xmlNode : xmlNodes) {
			CmAttributeTreeNode node = new CmAttributeTreeNode();
			node.setConfigurableModel(cm);
			updateNames(node, xmlNode.getName());
			node.setIsActive(xmlNode.isIsActive());
			if (cmAttribute == null) {
				//this is default mapping and it MUST be provided with datamodel attribute key
				Attribute dmAttribute = fetchAttribute(xmlNode.getAttributeKey());
				if (dmAttribute == null){
					//attribute doesn't exist; skip this tree node
					continue;
				}
				node.setDmAttribute(dmAttribute);
				node.setDmTreeNode(fetchAttributeTreeNode(xmlNode.getKeyRef(), xmlNode.getHkeyRef(), dmAttribute));
			} else {
				//this is custom mapping and datamodel attribute key MUST be null
				node.setAttribute(cmAttribute);
				node.setDmTreeNode(fetchAttributeTreeNode(xmlNode.getKeyRef(), xmlNode.getHkeyRef(), cmAttribute.getAttribute()));
			}
			node.setParent(parent);
			node.setNodeOrder(result.size());
			node.setDisplayMode(getDisplayMode(xmlNode.getDisplayMode()));
			node.setImageFile(findFile(xmlNode.getImageFile()));
			addToDataMap(xmlNode.getId(), node);
			node.setChildren(processCmTreeNodes(cm, cmAttribute, node, xmlNode.getChildren(), monitor));
			if (monitor.isCanceled()) return null;
			result.add(node);
		}
		return result;
	}

	private List<CmAttributeListItem> processCmListItems(ConfigurableModel cm, CmAttribute cmAttribute, List<ListItemType> xmlNodes, IProgressMonitor monitor) {
		if (monitor.isCanceled()) return null;
		List<CmAttributeListItem> result = new ArrayList<CmAttributeListItem>();
		for (ListItemType xmlNode : xmlNodes) {
			CmAttributeListItem item = new CmAttributeListItem();
			item.setConfigurableModel(cm);
			updateNames(item, xmlNode.getName());
			item.setIsActive(xmlNode.isIsActive());
			if (cmAttribute == null) {
				//this is default mapping and it MUST be provided with datamodel attribute key
				Attribute dmAttribute = fetchAttribute(xmlNode.getAttributeKey());
				if (dmAttribute == null){
					//attribute not found; do not add to list
					continue;
				}
				item.setDmAttribute(dmAttribute);
				item.setListItem(fetchAttributeListItem(xmlNode.getKeyRef(), dmAttribute));
			} else {
				//this is custom mapping and datamodel attribute key MUST be null
				item.setAttribute(cmAttribute);
				item.setListItem(fetchAttributeListItem(xmlNode.getKeyRef(), cmAttribute.getAttribute()));
			}
			item.setListOrder(result.size());
			item.setImageFile(findFile(xmlNode.getImageFile()));
			addToDataMap(xmlNode.getId(), item);
			if (monitor.isCanceled()) return null;
			result.add(item);
		}
		return result;
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
			//NOTE: hkey support was added in version 3.3.0, this code is conversion from version less than 3.2 (before configurable trees), this is why we pass hkey=null
			item.setDmTreeNode(fetchAttributeTreeNode(xmlItem.getRefKey(), null, fetchAttribute(xmlItem.getAttributeKey())));
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
			cmNode.setCategory(fetchCategory(xmlNode.getCategoryKey(), xmlNode.getCategoryHkey()));
			cmNode.setPhotoAllowed(xmlNode.isPhotoAllowed());
			cmNode.setPhotoRequired(xmlNode.isPhotoRequired());
			cmNode.setNodeOrder(i);
			cmNode.setParent(parent);
			cmNode.setChildren(processCmNodes(xmlNode.getNode(), cm, cmNode, monitor));
			cmNode.setCmAttributes(processAttributes(xmlNode.getAttribute(), cmNode, monitor));
			cmNode.setDisplayMode(getDisplayMode(xmlNode.getDisplayMode()));
			cmNode.setImageFile(findFile(xmlNode.getImageFile()));
			addToDataMap(xmlNode.getId(), cmNode);
			
			result.add(cmNode);
			if (monitor.isCanceled()) return null;
		}
		return result;
	}
	
	private List<CmAttribute> processAttributes(List<AttributeType> xmlAttrList, CmNode parent, IProgressMonitor monitor) {

		List<CmAttribute> result = new ArrayList<CmAttribute>();
		
		for (int i = 0; i < xmlAttrList.size(); i++) {
			AttributeType xmlAttr = xmlAttrList.get(i);
			
			CmAttribute cmAttr = new CmAttribute();
			cmAttr.setNode(parent);
			updateNames(cmAttr, xmlAttr.getName());
			Attribute dmAttribute = fetchAttribute(xmlAttr.getAttributeKey());
			if (dmAttribute == null){
				//no attribute; skip this item
				continue;
			}
			cmAttr.setAttribute(dmAttribute);
			cmAttr.setOrder(i);
			cmAttr.setCmAttributeOptions(processAttributeOptions(xmlAttr.getOption(), cmAttr));
			cmAttr.setTree(processCmTreeNodes(parent.getModel(), cmAttr, null, xmlAttr.getTreeNode(), monitor));
			cmAttr.setList(processCmListItems(parent.getModel(), cmAttr, xmlAttr.getListItem(), monitor));
			addToDataMap(xmlAttr.getId(), cmAttr);

			result.add(cmAttr);
			if (monitor.isCanceled()) return null;
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
					} else {
						break;
					}
					break;
				}
				case TREE:
				{
					AttributeTreeNode item = fetchAttributeTreeNode(xmlOption.getKeyRef(), xmlOption.getHkeyRef(), parent.getAttribute());
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

	private void processAttributeSettings(ConfigurableModel cm, CmDmAttributeSettingsTypeList setting) {
		if (setting != null) {
			for (CmDmAttributeSettingsType sXml : setting.getSetting()) {
				CmDmAttributeSettings s = new CmDmAttributeSettings();
				s.setModel(cm);
				Attribute dmAttribute = fetchAttribute(sXml.getAttributeKey());
				if (dmAttribute == null){
					//no data model attribute skip these settings
					continue;
				}
				s.setDmAttribute(dmAttribute);
				s.setDisplayMode(getDisplayMode(sXml.getDisplayMode()));
				cm.getAttributeSettings().put(s.getDmAttribute(), s);
			}
		}
	}
	
	private Category fetchCategory(String key, String hkey) {
		if (key == null || key.isEmpty())
			return null;
		String mapKey = hkey != null ? hkey : key;
		Category c = catLookup.get(mapKey);
		if (c == null) {
			Criteria query = session.createCriteria(Category.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.eq("keyId", key)); //$NON-NLS-1$
			if (hkey != null) {
				query = query.add(Restrictions.eq("hkey", hkey)); //$NON-NLS-1$
			}
			List<?> lst = query.list();
			if (lst.size() > 0) {
				c = (Category) lst.get(0);
				catLookup.put(mapKey, c);
			}
			if (lst.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (Object object : lst) {
					Category cat = (Category) object;
					if (sb.length() > 0) {
						sb.append("; "); //$NON-NLS-1$
					}
					sb.append(cat.getHkey());
				}
				warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_CategoryMultipleMatches, key, sb.toString()));
			}
		}
		if (c == null) {
			String warnMsg = hkey == null ?
					MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_Category, key) :
					MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_CategoryHkey, key, hkey);
			warnings.add(warnMsg);
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
		if (a == null) {
			warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_Attribute, key));
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
		if (a == null) {
			warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_ListItem, key, attribute.getKeyId()));
		}
		return a;
	}

	private AttributeTreeNode fetchAttributeTreeNode(String key, String hkey, Attribute attribute) {
		//NOTE: we need to be able to work with keyId for backward compatibility, as before 3.3.0 hkey was not exported
		if (key == null || key.isEmpty() || attribute == null)
			return null;
		String mapKey = hkey != null ? hkey : key;
		AttributeTreeNode a = treeNodeLookup.get(mapKey);
		if (a == null) {
			Criteria query = session.createCriteria(AttributeTreeNode.class)
					.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
					.add(Restrictions.eq("keyId", key)); //$NON-NLS-1$
			if (hkey != null) {
				query = query.add(Restrictions.eq("hkey", hkey)); //$NON-NLS-1$
			}
			List<?> lst = query.list();
			if (lst.size() > 0) {
				a = (AttributeTreeNode) lst.get(0);
				treeNodeLookup.put(mapKey, a);
			}
			if (lst.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (Object object : lst) {
					AttributeTreeNode n = (AttributeTreeNode) object;
					if (sb.length() > 0) {
						sb.append("; "); //$NON-NLS-1$
					}
					sb.append(n.getHkey());
				}
				warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_TreeNodeMultipleMatches, key, attribute.getKeyId(), sb.toString()));
			}
		}
		if (a == null) {
			String warnMsg = hkey == null ?
					MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_TreeNode, key, attribute.getKeyId()) :
					MessageFormat.format(Messages.CmXmlToSmartImporter_Problem_TreeNodeNotFoundHkey, key, hkey, attribute.getKeyId());
			warnings.add(warnMsg);
		}
		return a;
	}

	private File findFile(String name) {
		if (name == null) {
			return null;
		}
		return fileLookup != null ? fileLookup.get(name) : null;
	}
	
	private DisplayMode getDisplayMode(String mode) {
		if (mode == null) {
			return null;
		}
		return DisplayMode.valueOf(mode);
	}
	
	private void addToDataMap(String id, UuidItem item) {
		if (id != null) {
			dataMap.put(id, item);
		}
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
