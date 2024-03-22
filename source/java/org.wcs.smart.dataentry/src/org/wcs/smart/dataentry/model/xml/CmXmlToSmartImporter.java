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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.LocalAttachmentTagManager;
import org.wcs.smart.LocalSignatureTypeManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.dialog.AssociatedImageInterceptor;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.dataentry.model.xml.external.ICmXmlExtraDataImporter;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.external.XmlCmExtraDataImporterFactory;
import org.wcs.smart.dataentry.model.xml.generated.AttributeOptionType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeType;
import org.wcs.smart.dataentry.model.xml.generated.CmAttributeConfigType;
import org.wcs.smart.dataentry.model.xml.generated.CmDmAttributeSettingsType;
import org.wcs.smart.dataentry.model.xml.generated.LanguageType;
import org.wcs.smart.dataentry.model.xml.generated.ListItemType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;
import org.wcs.smart.dataentry.model.xml.generated.NodeType;
import org.wcs.smart.dataentry.model.xml.generated.TreeNodeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.OptionSelectionDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartFileUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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
	private Map<String, CmAttributeConfig> configLookup;
	private Map<String, Path> fileLookup;

	private Map<String, Integer> compatibilityConfigIndexes;

	private List<String> warnings;
	private Map<String, UuidItem> dataMap;

	private HashMap<String, SignatureType> signatures;
	private HashMap<String, AttachmentTag> attachmentTags;
	
	/**
	 * 
	 * @param xmlFile
	 * @param monitor
	 * @return null if error or monitor cancelled
	 * @throws Exception
	 */
	public ConfigurableModel importZip(Path zipFile, IProgressMonitor monitor) throws Exception {
		Path tempFolder = null;
		try {
			monitor.beginTask(Messages.CmXmlToSmartImporter_ExtractingZip, 1);
			tempFolder = SmartFileUtils.createTempDirectory("smart_cm_import"); //$NON-NLS-1$
			ZipUtil.unzipFolder(zipFile, tempFolder);
			Path[] xmlFile = {null};
			fileLookup = new HashMap<>();
			
			try(Stream<Path> stream = Files.list(tempFolder)){
				stream.forEach(file->{
					fileLookup.put(file.getFileName().toString(), file);
					if (file.getFileName().toString().endsWith(".xml")) { //$NON-NLS-1$
						xmlFile[0] = file;
					}
				});
			}
			if (xmlFile[0] != null) {
				return importXml(xmlFile[0], monitor);
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
	public ConfigurableModel importXml(Path xmlFile, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.CmXmlToSmartImporter_ImportingFromXml, 3);
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlCm = null;
		
		try (InputStream in = Files.newInputStream(xmlFile)){
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
		signatures = new HashMap<>();
		attachmentTags = new HashMap<>();
		
		compatibilityConfigIndexes = new HashMap<>();
		
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
			cm.setInstantGps(xmlCm.isInstantGps());
			cm.setPhotoFirst(xmlCm.isPhotoFirst());
			if (xmlCm.isUseEarthRanger() != null) {
				cm.setUseEarthRanger(xmlCm.isUseEarthRanger());
			}else {
				cm.setUseEarthRanger(Boolean.FALSE);
			}
			
			//signatures
			List<SignatureType> types = LocalSignatureTypeManager.INSTANCE.getTypes(session,  SmartDB.getCurrentConservationArea());
			
			if (xmlCm.getSignatures() != null) {
				for (org.wcs.smart.dataentry.model.xml.generated.SignatureType st : xmlCm.getSignatures().getSignatureType()) {
					String keyId = st.getKeyid();
					
					SignatureType existing = null;
					for (SignatureType t : types) {
						if (t.getKeyId().equalsIgnoreCase(keyId)) {
							existing = t;
							break;
						}
					}
					if (existing == null) {
						warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_SignatureTypeNotFound, keyId));
						existing = LocalSignatureTypeManager.INSTANCE.createType(SmartDB.getCurrentConservationArea());
						existing.setKeyId(keyId);
						updateNames(existing, st.getName());
						HibernateManager.saveOrMerge(session, existing);
					}
					signatures.put(st.getUuid(), existing);
				}
			}
			
			List<AttachmentTag> atags = LocalAttachmentTagManager.INSTANCE.getTags(session,  SmartDB.getCurrentConservationArea());
			
			if (xmlCm.getAttachmentTags() != null) {
				for (org.wcs.smart.dataentry.model.xml.generated.AttachmentTagType xtag : xmlCm.getAttachmentTags().getAttachmentTagType()){
					String keyId = xtag.getKeyid();
					
					AttachmentTag existing = null;
					for (AttachmentTag t : atags) {
						if (t.getKeyId().equalsIgnoreCase(keyId)) {
							existing = t;
							break;
						}
					}
					if (existing == null) {
						warnings.add(MessageFormat.format("An attachment tag with the key ''{0}'' could not found, a new attachment tag will be added to the Conservation Area", keyId));
						existing = LocalAttachmentTagManager.INSTANCE.createTag(SmartDB.getCurrentConservationArea(), keyId);
						existing.setKeyId(keyId);
						updateNames(existing, xtag.getName());
						session.persist(existing);
					}
					attachmentTags.put(xtag.getUuid(), existing);
				}
			
			}			
			//icon set
			if (xmlCm.getIconSet() != null && !xmlCm.getIconSet().isEmpty()) {
				IconSet is = QueryFactory.buildQuery(session, IconSet.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"keyId", xmlCm.getIconSet()}).uniqueResult(); //$NON-NLS-1$
				cm.setIconSet(is);
			}
			monitor.subTask(Messages.CmXmlToSmartImporter_ImportingConfigs);
			configLookup = loadAttributeConfigs(xmlCm.getAttributeConfig(), cm, monitor);

			Map<Attribute,CmAttributeConfig> items = new HashMap<>();
			for (CmAttributeConfig c : configLookup.values()) {
				if (!c.isDefault()) continue;
				if (items.containsKey(c.getAttribute())) {
					warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_InvalidXml, c.getAttribute().getName()));
					c.setDefault(false);
				}else {
					items.put(c.getAttribute(), c);
				}
				
			}
			cm.setDefaultConfigs(items);
			
			if (xmlCm.getDefaultTrees() != null || xmlCm.getDefaultLists() != null) {
				//backward compatibility logic to import configurable models of SMART versions after 3.2 and before 6.0.0
				Map<Attribute, CmAttributeConfig> defaultConfigs = createBackwardCompatibilityDefaultConfigs(cm, xmlCm, monitor);
				cm.setDefaultConfigs(defaultConfigs);
			}

			cm.setNodes(processCmNodes(xmlCm.getNodes().getNode(), cm, null, monitor));
			
			if (monitor.isCanceled()) return null;
			if (xmlCm.getTreeNodes() != null || xmlCm.getListItems() != null) {
				warnings.add(Messages.CmXmlToSmartImporter_Warn_VessionToOld);
			}
			
			//validation logic
			monitor.subTask(Messages.CmXmlToSmartImporter_Validating);
			validateConfigurableModel(cm);

			if (monitor.isCanceled()) return null;
			//converting extra data
			List<IConvertedCmExtraData> convertedExtraData = new ArrayList<IConvertedCmExtraData>();
			 
			List<ICmXmlExtraDataImporter> extensions = Collections.emptyList();
			try{
				extensions = XmlCmExtraDataImporterFactory.getImporters();
			}catch (Exception ex) {
				SmartPlugIn.log(Messages.XmlCmExtraDataContributionFactory_ErrorParseExtraData, ex);
			}
			 
			for (ICmXmlExtraDataImporter edc : extensions) {
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
			session.persist(cm);
			for (IConvertedCmExtraData extraData : convertedExtraData) {
				extraData.saveInTransaction(session, cm);
			}
			session.getTransaction().commit();
			return cm;
			
		} finally {
			langLookup = null;
			configLookup = null;
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

	
	private void validateConfigurableModel(ConfigurableModel cm) {
		for (CmNode cmNode : cm.getNodes()) {
			validateCmNode(cmNode);
		}
	}

	private void validateCmNode(CmNode cmNode) {
		
		for (CmAttribute cmAttr : cmNode.getCmAttributes()) {
			Attribute dmAttr = cmAttr.getAttribute();
			switch (dmAttr.getType()) {
			case LIST:
			case MLIST:
			case TREE: {
				ConfigurableModel cm = cmNode.getModel();
				if (cm.getDefaultConfigs().get(dmAttr) == null) {
					warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_Warn_DefaultConfigMissing, dmAttr.getName()));
					CmAttributeConfig config = dmAttr.getType().isList() ? CmDefaultListsUtil.buildDefaultListConfig(cm, dmAttr) : CmDefaultTreesUtil.buildDefaultTreeConfig(cm, dmAttr);
					cm.getDefaultConfigs().put(dmAttr, config);
				}
				if (cmAttr.getConfig() == null) {
					warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_Warn_NoConfigAssigned, dmAttr.getName()));
					cmAttr.setConfig(cm.getDefaultConfigs().get(dmAttr));
				}
				break;
			}
			default:
				break;
			}
		}
		
		if (cmNode.getCmAttributes() != null) {
			for (CmAttribute a : cmNode.getCmAttributes()) {
				if (a.getAttribute().getType().isList()) {
					//validate that all list items are part of the configuration
					List<AttributeListItem> allitems = new ArrayList<>(a.getAttribute().getAttributeList());
					
					for (CmAttributeListItem li : a.getConfig().getList()) {
						allitems.remove(li.getListItem());
					}
					for (AttributeListItem toadd : allitems) {
						CmAttributeListItem ali = new CmAttributeListItem();
						ali.setConfig(a.getConfig());
						ali.setIsActive(false);
						ali.setListItem(toadd);
						ali.setListOrder(a.getCurrentList().size() + 1);
						for (Label l : toadd.getNames()) ali.updateName(l.getLanguage(),l.getValue());
						a.getConfig().getList().add(ali);
					}
				}
			}
		}
		if (cmNode.getCategory() != null) {
			//validate that all attributes for this category are associated with
			//this node; if they aren't add them as disabled
			
			Category c = cmNode.getCategory();
			List<Attribute> dmAttributes = new ArrayList<>();
			List<Attribute> cmAttributes = new ArrayList<>();
			
			c.getAllAttribute(dmAttributes, true);
			for (CmAttribute ca : cmNode.getCmAttributes()) {
				cmAttributes.add(ca.getAttribute());
			}
			dmAttributes.removeAll(cmAttributes);
			
			for (Attribute a : dmAttributes) {
				CmAttribute newa = new CmAttribute();
				newa.setAttribute(a);
				
				CmAttributeConfig defaultc = configLookup.get(a.getKeyId());
				if (defaultc == null) {
					defaultc = new CmAttributeConfig();
					configLookup.put(a.getKeyId(), defaultc);
					defaultc.setAttribute(a);
					defaultc.setDisplayMode(DisplayMode.TEXT_IMAGE);
					defaultc.setDefault(true);
					for (Language l : langLookup.values()) defaultc.updateName(l, Messages.CmXmlToSmartImporter_DefaultConfigName);
					defaultc.setModel(cmNode.getModel());
					cmNode.getModel().getDefaultConfigs().put(a,  defaultc);
					if (a.getType().isList()) {
						validateListConfig(defaultc, a);
					}else if (a.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.TREE) {
						validateTreeConfig(defaultc, a);
					}
				}
				newa.setConfig( defaultc );
				for (Label l : a.getNames()) newa.updateName(l.getLanguage(),l.getValue());
				newa.setNode(cmNode);
				newa.setOrder(cmNode.getCmAttributes().size() + 1);
				
				HashMap<String, CmAttributeOption> ops = new HashMap<>();
				CmAttributeOption isvis = new CmAttributeOption();
				isvis.setCmAttribute(newa);
				isvis.setBooleanValue(false);
				isvis.setOptionId(CmAttributeOption.ID_IS_VISIBLE);
				ops.put(CmAttributeOption.ID_IS_VISIBLE, isvis);
				newa.setCmAttributeOptions(ops);
				
				cmNode.getCmAttributes().add(newa);
			}
		}
		
		
		for (CmNode node : cmNode.getChildren()) {
			validateCmNode(node);
		}
	}
	
	private void processTreeNode(AttributeTreeNode node, CmAttributeTreeNode parent, CmAttributeConfig config) {
		CmAttributeTreeNode kid = new CmAttributeTreeNode();
		kid.setChildren(new ArrayList<>());
		kid.setConfig(config);
		kid.setDisplayMode(DisplayMode.TEXT_IMAGE);
		kid.setDmTreeNode(node);
		kid.setIsActive(node.getIsActive());
		for (Label l : node.getNames()) kid.updateName(l.getLanguage(),l.getValue());
		kid.setNodeOrder(node.getNodeOrder());
		kid.setParent(parent);
		if (parent != null) {
			parent.getChildren().add(kid);
		}else {
			config.getTree().add(kid);
		}
		
		for (AttributeTreeNode c : node.getChildren()) {
			processTreeNode(c, kid, config);
		}
		
	}
	
	private void validateListConfig(CmAttributeConfig config, Attribute dmAttribute) {
		if (config.getList() == null) config.setList(new ArrayList<>());
		if (config.getList().isEmpty()) {
			warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_InvalidConfiguration, dmAttribute.getName()));
			//add list items
			for (AttributeListItem i : dmAttribute.getAttributeList()) {
				CmAttributeListItem ali = new CmAttributeListItem();
				ali.setConfig(config);
				ali.setIsActive(i.getIsActive());
				ali.setListItem(i);
				ali.setListOrder(i.getListOrder());
				for (Label l : i.getNames()) ali.updateName(l.getLanguage(),l.getValue());
				config.getList().add(ali);
			}
		}
	}
	
	private void validateTreeConfig(CmAttributeConfig config, Attribute dmAttribute) {
		if (config.getTree() == null) config.setTree(new ArrayList<>());
		if (config.getTree().isEmpty()) {
			warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_InvalidConfiguration, dmAttribute.getName()));
			//add default attributes
			for (AttributeTreeNode n : dmAttribute.getTree()) {
				processTreeNode(n, null, config);
			}
		}
	}
	
	private Map<String, CmAttributeConfig> loadAttributeConfigs(List<CmAttributeConfigType> xmlConfigList, ConfigurableModel cm, IProgressMonitor monitor) {
		Map<String, CmAttributeConfig> configMap = new HashMap<>();
		for (CmAttributeConfigType xmlConfig : xmlConfigList) {
			CmAttributeConfig config = new CmAttributeConfig();
			updateNames(config, xmlConfig.getName());
			config.setModel(cm);
			config.setDefault(xmlConfig.isIsDefault());
			config.setDisplayMode(getDisplayMode(xmlConfig.getDisplayMode()));
			Attribute dmAttribute = fetchAttribute(xmlConfig.getAttributeKey());
			if (dmAttribute != null) {
				config.setAttribute(dmAttribute);
				
				if (dmAttribute.getType().isList()) {
					config.setList(processCmListItems(config, dmAttribute, xmlConfig.getListItem(), monitor));
					validateListConfig(config, dmAttribute);
				}else if (dmAttribute.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.TREE) {
					config.setTree(processCmTreeNodes(config, dmAttribute, null, xmlConfig.getTreeNode(), monitor));
					validateTreeConfig(config, dmAttribute);
				}
				
				addToDataMap(xmlConfig.getId(), config);
				configMap.put(xmlConfig.getId(), config);
			}
		}
		return configMap;
	}

	private List<CmAttributeTreeNode> processCmTreeNodes(CmAttributeConfig cfg, Attribute dmAttribute, CmAttributeTreeNode parent, List<TreeNodeType> xmlNodes, IProgressMonitor monitor) {
		if (monitor.isCanceled()) return null;
		if (xmlNodes == null) return null;
		List<CmAttributeTreeNode> result = new ArrayList<CmAttributeTreeNode>();
		for (TreeNodeType xmlNode : xmlNodes) {
			CmAttributeTreeNode node = new CmAttributeTreeNode();
			updateNames(node, xmlNode.getName());
			node.setConfig(cfg);
			node.setIsActive(xmlNode.isIsActive());
			AttributeTreeNode dmnode = fetchAttributeTreeNode(xmlNode.getKeyRef(), xmlNode.getHkeyRef(), dmAttribute);
			if (dmnode == null) continue; 
			node.setDmTreeNode(dmnode);
			node.setParent(parent);
			node.setNodeOrder(result.size());
			node.setDisplayMode(getDisplayMode(xmlNode.getDisplayMode()));
			
			if ( (xmlNode.isIsCustomImage() == null || xmlNode.isIsCustomImage() == Boolean.TRUE) && xmlNode.getImageFile() != null) {
				node.setImageFile(findFile(xmlNode.getImageFile()));
			}
			
			addToDataMap(xmlNode.getId(), node);
			node.setChildren(processCmTreeNodes(cfg, dmAttribute, node, xmlNode.getChildren(), monitor));
			if (monitor.isCanceled()) return null;
			result.add(node);
		}
		return result;
	}

	private List<CmAttributeListItem> processCmListItems(CmAttributeConfig cfg, Attribute dmAttribute, List<ListItemType> xmlNodes, IProgressMonitor monitor) {
		if (monitor.isCanceled()) return null;
		if (xmlNodes == null) return null;
		List<CmAttributeListItem> result = new ArrayList<CmAttributeListItem>();
		for (ListItemType xmlNode : xmlNodes) {
			CmAttributeListItem item = new CmAttributeListItem();
			item.setConfig(cfg);
			updateNames(item, xmlNode.getName());
			item.setIsActive(xmlNode.isIsActive());
			AttributeListItem li = fetchAttributeListItem(xmlNode.getKeyRef(), dmAttribute);
			if (li == null) continue; //skip 
			item.setListItem(li);
			item.setListOrder(result.size());
			if ( (xmlNode.isIsCustomImage() == null || xmlNode.isIsCustomImage() == Boolean.TRUE) && xmlNode.getImageFile() != null) {
				item.setImageFile(findFile(xmlNode.getImageFile()));
			}
			addToDataMap(xmlNode.getId(), item);
			if (monitor.isCanceled()) return null;
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
			
			if (xmlNode.getCategoryKey() != null) {
				Category c = fetchCategory(xmlNode.getCategoryKey(), xmlNode.getCategoryHkey());
				if (c == null) {
					//don't import this node
					// we can't find the category to associated it with
					continue;
				}
				cmNode.setCategory(c);
				
				if (xmlNode.getSignature() != null) {
					Set<SignatureType> bits = new HashSet<>();
					for (org.wcs.smart.dataentry.model.xml.generated.SignatureType s : xmlNode.getSignature()) {
						bits.add(signatures.get(s.getUuid()));
					}
					cmNode.setSignatures(bits);
				}
				if (xmlNode.getAttachmentTags() != null) {
					Set<AttachmentTag> bits = new HashSet<>();
					for (org.wcs.smart.dataentry.model.xml.generated.AttachmentTagType s : xmlNode.getAttachmentTags()) {
						if (attachmentTags.containsKey(s.getUuid())) {
							bits.add(attachmentTags.get(s.getUuid()));
						}
					}
					cmNode.setAttachmentTags(bits);
				}
				if (xmlNode.getIntegrateIncidentType() != null) {
					cmNode.setIntegrateIncidentType(CmNode.IntegrateIncidentType.valueOf(xmlNode.getIntegrateIncidentType()));
				}
			}
			cmNode.setPhotoAllowed(xmlNode.isPhotoAllowed());
			cmNode.setPhotoRequired(xmlNode.isPhotoRequired());
			cmNode.setCollectMultipleObservations(xmlNode.isCollectMultipleObs());
			cmNode.setUseSingleGpsPoint(xmlNode.isUseSingleGpsPoint());
			cmNode.setNodeOrder(i);
			cmNode.setParent(parent);
			cmNode.setChildren(processCmNodes(xmlNode.getNode(), cm, cmNode, monitor));
			cmNode.setCmAttributes(processAttributes(xmlNode.getAttribute(), cmNode, monitor));
			cmNode.setDisplayMode(getDisplayMode(xmlNode.getDisplayMode()));
			
			if ( (xmlNode.isIsCustomImage() == null || xmlNode.isIsCustomImage() == Boolean.TRUE) && 
					xmlNode.getImageFile() != null) {
				cmNode.setImageFile(findFile(xmlNode.getImageFile()));
			}
			
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
			
			if (cmAttr.getHelpFormat() != null) {
				CmAttribute temp = new CmAttribute();
				temp.setUuid(UuidUtils.stringToUuid( xmlAttr.getId()) );
				String helpFileName = temp.getHelpImageFileRootName() + "." + cmAttr.getHelpFormat(); //$NON-NLS-1$
				if (fileLookup == null) {
					warnings.add(Messages.CmXmlToSmartImporter_NoHelpFiles);
				}else {
					Path f = fileLookup.get(helpFileName);
					if (f != null && Files.exists(f)) {
						cmAttr.setImportHelpFile(f);
					}else {
						warnings.add(MessageFormat.format(Messages.CmXmlToSmartImporter_HelpFileNotFound, helpFileName));
					}
				}
			}
			if ( (xmlAttr.isIsCustomImage() == null || xmlAttr.isIsCustomImage() == Boolean.TRUE) && xmlAttr.getImageFile() != null) {
				cmAttr.setImageFile(findFile(xmlAttr.getImageFile()));
			}
			
			if (xmlAttr.getConfigId() != null) {
				cmAttr.setConfig(configLookup.get(xmlAttr.getConfigId()));
			} else {
				switch (dmAttribute.getType()) {
				case LIST:
				case MLIST:
				case TREE: {
					//looks like we are importing model exported in SMART version lower than 6.0.0
					CmAttributeOption optionCC = cmAttr.getCmAttributeOptions().get("CUSTOM_CONFIG"); //$NON-NLS-1$
					if (optionCC != null && optionCC.getBooleanValue()) {
						CmAttributeOption optionDM = cmAttr.getCmAttributeOptions().get("DISPLAY_MODE"); //$NON-NLS-1$
						String displayMode = optionDM != null ? optionDM.getStringValue() : null;
						cmAttr.setConfig(createCompatibilityConfig(parent.getModel(), dmAttribute, false, displayMode, xmlAttr.getListItem(), xmlAttr.getTreeNode(), monitor));
					} else {
						cmAttr.setConfig(parent.getModel().getDefaultConfigs().get(dmAttribute));
					}
					cmAttr.getCmAttributeOptions().remove("DISPLAY_MODE"); //$NON-NLS-1$
					cmAttr.getCmAttributeOptions().remove("CUSTOM_CONFIG"); //$NON-NLS-1$
					
					break;
				}
				default:
					break;
				}
			}
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
				case MLIST:
					cmOption.setStringValue(xmlOption.getStringValue());
					break;
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

	private Map<Attribute, CmAttributeConfig> createBackwardCompatibilityDefaultConfigs(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlCm, IProgressMonitor monitor) {
		Map<Attribute, CmAttributeConfig> result = new HashMap<>();
		Map<String, String> attr2DisplayMode = xmlCm.getSetting() != null ? xmlCm.getSetting().getSetting().stream().collect(Collectors.toMap(CmDmAttributeSettingsType::getAttributeKey, CmDmAttributeSettingsType::getDisplayMode)) : Collections.emptyMap();

		monitor.subTask(Messages.CmXmlToSmartImporter_ImportingTreeNodes);
		if (xmlCm.getDefaultTrees() != null) {
			//tree mapping was introduced in 3.2.0, previous version were using different mapping
			Map<String, List<TreeNodeType>> attrMap = xmlCm.getDefaultTrees().getTreeNode().stream().collect(Collectors.groupingBy(TreeNodeType::getAttributeKey));
			for (String attrKey : attrMap.keySet()) {
				Attribute dmAttribute = fetchAttribute(attrKey);
				if (dmAttribute != null) {
					CmAttributeConfig config = createCompatibilityConfig(cm, dmAttribute, true, attr2DisplayMode.get(attrKey), null, attrMap.get(attrKey), monitor);
					result.put(config.getAttribute(), config);
				}
				
			}
		}

		monitor.subTask(Messages.CmXmlToSmartImporter_ImportingListItems);
		if (xmlCm.getDefaultLists() != null) {
			//list mapping was introduced in 3.2.1, previous version were using different mapping
			Map<String, List<ListItemType>> attrMap = xmlCm.getDefaultLists().getListItem().stream().collect(Collectors.groupingBy(ListItemType::getAttributeKey));
			for (String attrKey : attrMap.keySet()) {
				Attribute dmAttribute = fetchAttribute(attrKey);
				if (dmAttribute != null) {
					//attribute doesn't exist so we skip it here
					CmAttributeConfig config = createCompatibilityConfig(cm, dmAttribute, true, attr2DisplayMode.get(attrKey), attrMap.get(attrKey), null, monitor);
					result.put(config.getAttribute(), config);
				}
			}
		}
		return result;
	}

	private CmAttributeConfig createCompatibilityConfig(ConfigurableModel model, Attribute dmAttribute, boolean isDefault, String displayMode, List<ListItemType> listItem, List<TreeNodeType> treeNode, IProgressMonitor monitor) {
		CmAttributeConfig config = new CmAttributeConfig();
		
		String name = isDefault ? dmAttribute.getName() : Messages.CmAttributeConfig_Custom_Prefix + " " + dmAttribute.getName(); //$NON-NLS-1$
		if (!isDefault) {
			String attrKey = dmAttribute.getKeyId();
			Integer index = compatibilityConfigIndexes.get(attrKey);
			if (index == null) {
				index = 1;
				compatibilityConfigIndexes.put(attrKey, index);
			} else {
				index++;
			}
			name += " " + String.valueOf(index); //$NON-NLS-1$
		}
		config.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		config.setModel(model);
		config.setDefault(isDefault);
		config.setDisplayMode(getDisplayMode(displayMode));
		config.setAttribute(dmAttribute);
		config.setList(processCmListItems(config, dmAttribute, listItem, monitor));
		config.setTree(processCmTreeNodes(config, dmAttribute, null, treeNode, monitor));
		
		return config;
	}
	
	private Category fetchCategory(String key, String hkey) {
		if (key == null || key.isEmpty())
			return null;
		String mapKey = hkey != null ? hkey : key;
		Category c = catLookup.get(mapKey);
		if (c == null) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Category> cq = cb.createQuery(Category.class);
			Root<Category> from = cq.from(Category.class);
			Predicate[] filters = new Predicate[hkey != null ? 3 : 2];
			filters[0] = cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			filters[1] = cb.equal(from.get("keyId"), key); //$NON-NLS-1$
			if (hkey != null) {
				filters[2] = cb.equal(from.get("hkey"), hkey); //$NON-NLS-1$
			}
			cq.where(cb.and(filters));
			
			List<Category> lst = session.createQuery(cq).list();
			if (lst.size() > 0) {
				c = lst.get(0);
				catLookup.put(mapKey, c);
			}
			if (lst.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (Category cat : lst) {
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
			a = QueryFactory.buildQuery(session, Attribute.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId", key}).uniqueResult(); //$NON-NLS-1$
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
		AttributeListItem a = listItemLookup.get(attribute.getKeyId() + "." + key); //$NON-NLS-1$
		if (a == null) {
			a = QueryFactory.buildQuery(session, AttributeListItem.class,
					new Object[] {"attribute", attribute}, //$NON-NLS-1$
					new Object[] {"keyId", key}).uniqueResult(); //$NON-NLS-1$
			listItemLookup.put(attribute.getKeyId() + "." + key, a); //$NON-NLS-1$
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
		String mapKey = attribute.getKeyId() + "." + (hkey != null ? hkey : key); //$NON-NLS-1$
		AttributeTreeNode a = treeNodeLookup.get(mapKey);
		if (a == null) {
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<AttributeTreeNode> cq = cb.createQuery(AttributeTreeNode.class);
			Root<AttributeTreeNode> from = cq.from(AttributeTreeNode.class);
			Predicate[] filters = new Predicate[hkey != null ? 3 : 2];
			filters[0] = cb.equal(from.get("attribute"), attribute); //$NON-NLS-1$
			filters[1] = cb.equal(from.get("keyId"), key); //$NON-NLS-1$
			if (hkey != null) {
				filters[2] = cb.equal(from.get("hkey"), hkey); //$NON-NLS-1$
			}
			cq.where(cb.and(filters));
			
			List<AttributeTreeNode> lst = session.createQuery(cq).list();
			if (lst.size() > 0) {
				a = lst.get(0);
				treeNodeLookup.put(mapKey, a);
			}
			if (lst.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (AttributeTreeNode n : lst) {
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

	private Path findFile(String name) {
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
			//we don't want to import names whose source was the data model;
			//in this case we just want to use the datamodel names so we skip these
			if (nameType.getSource() != null && nameType.getSource().equals(CmXmlManager.NAME_SOURCE.DM.name())) continue;
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
