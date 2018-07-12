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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.dataentry.model.xml.external.IXmlCmExtraDataContribution;
import org.wcs.smart.dataentry.model.xml.generated.AttributeOptionType;
import org.wcs.smart.dataentry.model.xml.generated.AttributeType;
import org.wcs.smart.dataentry.model.xml.generated.CmAttributeConfigType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;
import org.wcs.smart.dataentry.model.xml.generated.LanguageListType;
import org.wcs.smart.dataentry.model.xml.generated.LanguageType;
import org.wcs.smart.dataentry.model.xml.generated.ListItemType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;
import org.wcs.smart.dataentry.model.xml.generated.NodeType;
import org.wcs.smart.dataentry.model.xml.generated.NodeTypeList;
import org.wcs.smart.dataentry.model.xml.generated.TreeNodeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

/**
 * Converts a database configurable model to the xml representation 
 * of the configurable model.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CmSmartToXmlConverter {
	
	//EG: added support to export cm not in the database
	/**
	 * Configurable model can have a UUID or not.  If it has a uuid it will be reloaded from the database, otherwise
	 * the object provided will be used.  This is support exporting the "original data model" to cybertracker.
	 * @param cm
	 * @param monitor
	 * @return
	 */
	public static org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel convert(ConfigurableModel cm, IProgressMonitor monitor) {
		org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml = new org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel();

		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				if (cm.getUuid() != null) {
					cm = DataentryHibernateManager.getFullConfigurableModel(cm.getUuid(), session);
				}
				if (monitor.isCanceled()) return null;
				
				monitor.subTask(Messages.CmSmartToXmlConverter_ProcessLanguages);
				HashMap<String, Language> llookup = processLanguages(cm, xml);
				setNames(xml.getName(), cm.getNames(), null, llookup);
				if (cm.getDisplayMode() != null) {
					xml.setDisplayMode(cm.getDisplayMode().name());
				}
				xml.setInstantGps(cm.isInstantGps());
				xml.setPhotoFirst(cm.isPhotoFirst());
				monitor.worked(1);
				if (monitor.isCanceled()) return null;
				
				monitor.subTask(Messages.CmSmartToXmlConverter_ProcessCmNodes);
				processCmNodes(cm, xml, llookup, session, monitor);
				monitor.worked(1);
				if (monitor.isCanceled()) return null;
				
				monitor.subTask(Messages.CmSmartToXmlConverter_ProcessAttributeConfigs);
				processCmAttributeConfigs(cm, xml, llookup, session, monitor);
				monitor.worked(1);
				if (monitor.isCanceled()) return null;
				
				monitor.subTask(Messages.CmSmartToXmlConverter_ProcessExtraData);
				for (IXmlCmExtraDataContribution dc : XmlCmExtraDataContributionFactory.getContributions()) {
					List<CmExtraDataType> extraData = dc.exportData(cm, session);
					if (extraData != null) {
						xml.getExtraData().addAll(extraData);
					}
				}
				monitor.worked(1);
				if (monitor.isCanceled()) return null;
				
			} finally {
				session.getTransaction().rollback();
			}
		}
		return xml;
	}

	private static void processCmAttributeConfigs(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml,
			HashMap<String, Language> llookup, Session session, IProgressMonitor monitor) {
		if(cm.getUuid() == null) return;
		
		List<CmAttributeConfig> configs = QueryFactory.buildQuery(session, CmAttributeConfig.class, "model", cm).list(); //$NON-NLS-1$
		for (CmAttributeConfig config : configs) {
			CmAttributeConfigType xmlConfig = new CmAttributeConfigType();
			setNames(xmlConfig.getName(), config.getNames(), null, llookup);
			xmlConfig.setId(config.getUuid().toString());
			xmlConfig.setIsDefault(config.isDefault());
			if (config.getDisplayMode() != null) {
				xmlConfig.setDisplayMode(config.getDisplayMode().name());
			}
			xmlConfig.setAttributeKey(config.getAttribute().getKeyId());
			xmlConfig.setAttributeUuid(toString(config.getAttribute().getUuid()));
			
			processCmListItems(config.getList(), xmlConfig.getListItem(), llookup, monitor);
			processCmTreeNodes(config.getTree(), xmlConfig.getTreeNode(), llookup, monitor);
			
			xml.getAttributeConfig().add(xmlConfig);
		}
		
	}

	private static String toString(UUID uuid) {
		return UuidUtils.uuidToString(uuid);
	}
	
	private static void processCmTreeNodes(List<CmAttributeTreeNode> cmList,
			List<TreeNodeType> xmlList, HashMap<String, Language> llookup, IProgressMonitor monitor) {
		if(monitor.isCanceled()) return;
		for (CmAttributeTreeNode cmNode : cmList) {
			TreeNodeType xmlNode = new TreeNodeType();
			setNames(xmlNode.getName(), cmNode.getNames(), cmNode.getDmTreeNode() == null ? Collections.emptySet() :  cmNode.getDmTreeNode().getNames(), llookup);
			xmlNode.setIsActive(cmNode.getIsActive());
			if (cmNode.getDmTreeNode() == null){
				xmlNode.setKeyRef(null);
				xmlNode.setHkeyRef(null);
			}else{
				xmlNode.setKeyRef(cmNode.getDmTreeNode().getKeyId());
				xmlNode.setHkeyRef(cmNode.getDmTreeNode().getHkey());
				xmlNode.setDmUuid(toString(cmNode.getDmTreeNode().getUuid()));
			}
			if (cmNode.getDisplayMode() != null) {
				xmlNode.setDisplayMode(cmNode.getDisplayMode().name());
			}
			xmlNode.setImageFile(getImageFileRef(cmNode));
			xmlNode.setId(cmNode.getUuid().toString()); //this will allow to reference this item in extradata
			processCmTreeNodes(cmNode.getChildren(), xmlNode.getChildren(), llookup, monitor);
			xmlList.add(xmlNode);
		}
	}

	private static void processCmListItems(List<CmAttributeListItem> cmList,
			List<ListItemType> xmlList,	HashMap<String, Language> llookup, IProgressMonitor monitor) {

		if(monitor.isCanceled()) return;
		for (CmAttributeListItem cmNode : cmList) {
			ListItemType xmlNode = new ListItemType();
			setNames(xmlNode.getName(), cmNode.getNames(), cmNode.getListItem().getNames(), llookup);
			xmlNode.setIsActive(cmNode.getIsActive());
			if (cmNode.getListItem() == null){
				xmlNode.setKeyRef(null);
			}else{
				xmlNode.setKeyRef(cmNode.getListItem().getKeyId());
				xmlNode.setDmUuid(toString(cmNode.getListItem().getUuid()));
			}
			xmlNode.setImageFile(getImageFileRef(cmNode));
			xmlNode.setId(cmNode.getUuid().toString()); //this will allow to reference this item in extradata
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
		setNames(nt.getName(), node.getNames(), null, llookup);
		if (node.getCategory() != null) {
			nt.setCategoryKey(node.getCategory().getKeyId());
			nt.setCategoryHkey(node.getCategory().getHkey());
			nt.setDmUuid(toString(node.getCategory().getUuid()));
		}
		nt.setPhotoAllowed(node.isPhotoAllowed());
		nt.setPhotoRequired(node.isPhotoRequired());
		if (node.getDisplayMode() != null) {
			nt.setDisplayMode(node.getDisplayMode().name());
		}
		nt.setImageFile(getImageFileRef(node));
		if (node.getUuid() != null) {
			nt.setId(node.getUuid().toString()); //this will allow to reference this item in extradata
		}
		
		if (node.getCmAttributes() != null){
			for (CmAttribute ca : node.getCmAttributes()) {
				AttributeType at = new AttributeType();
				setNames(at.getName(), ca.getNames(), null, llookup);
				at.setAttributeKey(ca.getAttribute().getKeyId());
				at.setDmUuid(toString(ca.getAttribute().getUuid()));
				at.setRequired(ca.getAttribute().getIsRequired());
				
				for (CmAttributeOption option : ca.getCmAttributeOptions().values()) {
					AttributeOptionType aot = new AttributeOptionType();
					aot.setId(option.getOptionId());
					aot.setStringValue(option.getStringValue());
					aot.setDoubleValue(option.getDoubleValue());
					if (option.getUuidValue() != null) {
						switch (ca.getAttribute().getType()) {
						case LIST:
						{
							AttributeListItem item = session.get(AttributeListItem.class, option.getUuidValue());
							if (item != null) {
								aot.setKeyRef(item.getKeyId());
								aot.setHkeyRef(null); //not relevant for list items attributes
								aot.setDmUuid(toString(item.getUuid()));
							}
							break;
						}
						case TREE:
						{
							AttributeTreeNode item = session.get(AttributeTreeNode.class, option.getUuidValue());
							if (item != null) {
								aot.setKeyRef(item.getKeyId());
								aot.setHkeyRef(item.getHkey());
								aot.setDmUuid(toString(item.getUuid()));
							}
							break;
						}
						default:
							break;
						}
					}
					at.getOption().add(aot);
				}
				if (ca.getUuid() != null) {
					at.setId(ca.getUuid().toString()); //this will allow to reference this item in extradata
				}
				if (ca.getConfig() != null && ca.getConfig().getUuid() != null) {
					at.setConfigId(ca.getConfig() != null ? ca.getConfig().getUuid().toString() : null);
				}
				at.setType(ca.getAttribute().getType().name());
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

	private static HashMap<String, Language> processLanguages(ConfigurableModel cm, org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml) {
		HashMap<String, Language> lookup = new HashMap<String, Language>();
		LanguageListType llt = new LanguageListType();
		xml.setLanguages(llt);
		for (Language ll : cm.getConservationArea().getLanguages()) {
			LanguageType lt = new LanguageType();
			lt.setCode(ll.getCode());
			lt.setIsDefault(ll.equals(cm.getConservationArea().getDefaultLanguage()));
			llt.getLanguage().add(lt);
			lookup.put(UuidUtils.uuidToString(ll.getUuid()), ll);
		}	
		return lookup;
	}
	
	private static void setNames(List<NameType> list, Set<Label> elementNames, Set<Label> srcElementNames, HashMap<String, Language> llookup){
		Set<Language> allLangs = new HashSet<>();
		if (elementNames != null) {
			elementNames.forEach(l -> allLangs.add(l.getLanguage()));
		}
		if (srcElementNames != null) {
			srcElementNames.forEach(l -> allLangs.add(l.getLanguage()));
		}
		
		for (Language l : allLangs) {
			Label elementName = null;
			for (Label ll : elementNames) {
				if (ll.getLanguage().equals(l)) { elementName = ll; break; }
			}
			if (elementName != null) {
				NameType nt = new NameType();
				nt.setValue(elementName.getValue());
				nt.setLanguageCode(llookup.get(UuidUtils.uuidToString(elementName.getLanguage().getUuid())).getCode());
				nt.setSource(CmXmlManager.NAME_SOURCE.CM.name());
				list.add(nt);
				continue;
			}
			
			for (Label ll : srcElementNames) {
				if (ll.getLanguage().equals(l)) { elementName = ll; break; }
			}
			if (elementName != null) {
				NameType nt = new NameType();
				nt.setValue(elementName.getValue());
				nt.setLanguageCode(llookup.get(UuidUtils.uuidToString(elementName.getLanguage().getUuid())).getCode());
				nt.setSource(CmXmlManager.NAME_SOURCE.DM.name());
				list.add(nt);
			}
		}
//		for (Label lbl: names){
//			NameType nt = new NameType();
//			nt.setValue(lbl.getValue());
//			nt.setLanguageCode(llookup.get(UuidUtils.uuidToString(lbl.getLanguage().getUuid())).getCode());
//			list.add(nt);
//		}
	}

	private static String getImageFileRef(IImageAssociatedObject obj) {
		File file = obj.getImageFile();
		return (file != null && file.exists()) ? file.getName() : null;
	}
	
}
