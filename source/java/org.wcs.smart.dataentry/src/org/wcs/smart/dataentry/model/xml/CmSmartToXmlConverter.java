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

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Collection;
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
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.IconFile;
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
import org.wcs.smart.dataentry.model.xml.generated.SignatureTypeList;
import org.wcs.smart.dataentry.model.xml.generated.TreeNodeType;
import org.wcs.smart.dataentry.visiblewhen.filter.Parser;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.BooleanFilter;
import org.wcs.smart.filter.BracketFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.NotFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.SharedUtils;
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
	 * This function should be used to export the configurable model to an xml
	 * file without including referenced data model icons.  (for exporting from one
	 * ca into another ca).  
	 * 
	 * @param cm
	 * @param monitor
	 * @return
	 * @throws Exception 
	 */
	public static org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel convertToXml(ConfigurableModel cm, IProgressMonitor monitor) throws Exception {
		return convert(cm, false, monitor);
	}
	
	/**
	 * Configurable Model can have a UUID or not.  If it has a UUID the model will be reloaded from the database, otherwise
	 * the object provided will be used.  This was done to support exporting the "original data model" to cybertracker.
	 * 
	 * This function should be used when converting for SMART Mobile/Cybertracker.  This will include
	 * data model icons and convert the visible when strings to SMART Mobile format. 
	 * 
	 * @param cm
	 * @param monitor
	 * @return
	 * @throws Exception 
	 */
	public static org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel convertToSmartMobileXML(ConfigurableModel cm, IProgressMonitor monitor) throws Exception {
		return convert(cm, true, monitor);
	}
	
	private static org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel convert(ConfigurableModel cm, 
			boolean forCybertracker, IProgressMonitor monitor) throws Exception {
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
				
				if (cm.getIconSet() != null) xml.setIconSet(cm.getIconSet().getKeyId());
				xml.setInstantGps(cm.isInstantGps());
				xml.setPhotoFirst(cm.isPhotoFirst());
				monitor.worked(1);
				if (monitor.isCanceled()) return null;
				
				
				//find and write signatures
				Set<UUID> signatures = new HashSet<>();
				ArrayDeque<CmNode> nodes = new ArrayDeque<>();
				nodes.addAll(cm.getNodes());
				while(!nodes.isEmpty()) {
					CmNode node = nodes.remove();
					signatures.addAll(node.getSignatureUuids());
					nodes.addAll(node.getChildren());
				}
				Set<UUID> validsignatures = new HashSet<>();
				if (!signatures.isEmpty()) {
					xml.setSignatures(new SignatureTypeList());
					for (UUID sig : signatures) {
						SignatureType stype = session.get(SignatureType.class, sig);
						if (stype != null) {
							org.wcs.smart.dataentry.model.xml.generated.SignatureType xtype = new org.wcs.smart.dataentry.model.xml.generated.SignatureType();
							xtype.setUuid(UuidUtils.uuidToString(stype.getUuid()));
							setNames(xtype.getName(), stype.getNames(), null, llookup);
							xtype.setKeyid(stype.getKeyId());
							xml.getSignatures().getSignatureType().add(xtype);
							validsignatures.add(stype.getUuid());
						}
					}
				}
				
				//nodes
				monitor.subTask(Messages.CmSmartToXmlConverter_ProcessCmNodes);
				processCmNodes(cm, xml, llookup, session, forCybertracker, validsignatures, monitor);
				monitor.worked(1);
				if (monitor.isCanceled()) return null;
				
				monitor.subTask(Messages.CmSmartToXmlConverter_ProcessAttributeConfigs);
				processCmAttributeConfigs(cm, xml, llookup, session, forCybertracker, monitor);
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
			HashMap<String, Language> llookup, Session session, boolean forCybertracker, IProgressMonitor monitor) {
		Collection<CmAttributeConfig> configs = Collections.emptyList();
		if (cm.getUuid() == null) {
			//if there is no uuid; then we just use what is in the cm 
			configs = cm.getDefaultConfigs().values();
		}else {
			configs = QueryFactory.buildQuery(session, CmAttributeConfig.class, "model", cm).list(); //$NON-NLS-1$
		}
		for (CmAttributeConfig config : configs) {
			CmAttributeConfigType xmlConfig = new CmAttributeConfigType();
			setNames(xmlConfig.getName(), config.getNames(), null, llookup);
			if (config.getUuid() != null) {
				xmlConfig.setId(config.getUuid().toString());
			}
			xmlConfig.setIsDefault(config.isDefault());
			if (config.getDisplayMode() != null) {
				xmlConfig.setDisplayMode(config.getDisplayMode().name());
			}
			xmlConfig.setAttributeKey(config.getAttribute().getKeyId());
			xmlConfig.setAttributeUuid(toString(config.getAttribute().getUuid()));
			
			processCmListItems(config.getList(), config.getModel(), xmlConfig.getListItem(), llookup, forCybertracker, monitor);
			processCmTreeNodes(config.getTree(), config.getModel(), xmlConfig.getTreeNode(), llookup, forCybertracker, monitor);
			
			xml.getAttributeConfig().add(xmlConfig);
		}
		
	}

	private static String toString(UUID uuid) {
		return UuidUtils.uuidToString(uuid);
	}
	
	private static void processCmTreeNodes(List<CmAttributeTreeNode> cmList, ConfigurableModel cm,
			List<TreeNodeType> xmlList, HashMap<String, Language> llookup, boolean forCybertracker, IProgressMonitor monitor) {
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
			xmlNode.setImageFile(getImageFileRef(cmNode, cm, forCybertracker));
			xmlNode.setIsCustomImage(isCustomIcon(cmNode));
			if (cmNode.getUuid() != null) {
				xmlNode.setId(cmNode.getUuid().toString()); //this will allow to reference this item in extradata
			}
			processCmTreeNodes(cmNode.getChildren(), cm, xmlNode.getChildren(), llookup, forCybertracker, monitor);
			xmlList.add(xmlNode);
		}
	}

	private static void processCmListItems(List<CmAttributeListItem> cmList, ConfigurableModel cm,
			List<ListItemType> xmlList,	HashMap<String, Language> llookup, boolean forCybertracker, IProgressMonitor monitor) {

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
			xmlNode.setImageFile(getImageFileRef(cmNode, cm, forCybertracker));
			xmlNode.setIsCustomImage(isCustomIcon(cmNode));
			if (cmNode.getUuid() != null) {
				xmlNode.setId(cmNode.getUuid().toString()); //this will allow to reference this item in extradata
			}
			xmlList.add(xmlNode);
		}
	}

	private static void processCmNodes(ConfigurableModel cm,
			org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml,
			HashMap<String, Language> llookup, Session session, boolean forCybertracker, 
			Set<UUID> validsignatures, IProgressMonitor monitor) throws Exception {

		NodeTypeList ntl = new NodeTypeList();
		xml.setNodes(ntl);
		if (cm.getNodes() != null) {
			for (CmNode node : cm.getNodes()){
				processCmNode(node, ntl.getNode(), llookup, session, forCybertracker, validsignatures, monitor);
			}
		}
	}

	private static void processCmNode(CmNode node, List<NodeType> xmlNodes,
			HashMap<String, Language> llookup, Session session, boolean forCybertracker, 
			Set<UUID> validsignatures,
			IProgressMonitor monitor) throws Exception {

		monitor.subTask(MessageFormat.format(Messages.CmSmartToXmlConverter_ProcessingNode, node.getName()));
		NodeType nt = new NodeType();
		setNames(nt.getName(), node.getNames(), null, llookup);
		if (node.getCategory() != null) {
			nt.setCategoryKey(node.getCategory().getKeyId());
			nt.setCategoryHkey(node.getCategory().getHkey());
			nt.setDmUuid(toString(node.getCategory().getUuid()));
			
			for (UUID uuid : node.getSignatureUuids()) {
				if (validsignatures.contains(uuid)) {
					org.wcs.smart.dataentry.model.xml.generated.SignatureType xs = new org.wcs.smart.dataentry.model.xml.generated.SignatureType();
					xs.setUuid(UuidUtils.uuidToString(uuid));
					nt.getSignatureType().add(xs);
				}
			}
		}
		
		nt.setPhotoAllowed(node.isPhotoAllowed());
		nt.setPhotoRequired(node.isPhotoRequired());
		if (node.getDisplayMode() != null) {
			nt.setDisplayMode(node.getDisplayMode().name());
		}
		
		nt.setImageFile(getImageFileRef(node, node.getModel(), forCybertracker));
		nt.setIsCustomImage(isCustomIcon(node));
		
		if (node.getUuid() != null) {
			nt.setId(node.getUuid().toString()); //this will allow to reference this item in extradata
		}
		nt.setCollectMultipleObs(node.isCollectMultipleObservations());
		nt.setUseSingleGpsPoint(node.isUseSingleGpsPoint());
		
		node.isUseSingleGpsPoint(); //only include if valid
		if (node.getCmAttributes() != null){
			for (CmAttribute ca : node.getCmAttributes()) {
				AttributeType at = new AttributeType();
				setNames(at.getName(), ca.getNames(), null, llookup);
				at.setAttributeKey(ca.getAttribute().getKeyId());
				at.setDmUuid(toString(ca.getAttribute().getUuid()));
				at.setRequired(ca.getAttribute().getIsRequired());
				
				at.setIsCustomImage(isCustomIcon(ca));
				at.setImageFile(getImageFileRef(ca, node.getModel(), forCybertracker));
		
				if (ca.getAttribute().getMinValue() != null) {
					at.setMinValue(ca.getAttribute().getMinValue());
				}
				if (ca.getAttribute().getMaxValue() != null) {
					at.setMaxValue(ca.getAttribute().getMaxValue());
				}
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
					
					if (forCybertracker && option.getOptionId().equalsIgnoreCase(CmAttributeOption.ID_IS_VISIBLE)) {
						if (option.getDoubleValue().intValue() == CmAttributeOption.VisibleWhen.CUSTOM.getValue()) {
							aot.setStringValue(convertVisibleWhenExpression(option.getStringValue(), node));
						}
					}
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
				processCmNode(cn, nt.getNode(), llookup, session, forCybertracker, validsignatures, monitor);
				if (monitor.isCanceled()) return;
			}
		}
		xmlNodes.add(nt);
	}

	/**
	 * Convert the visible when expression into the xforms (odk)
	 * https://docs.getodk.org/form-operators-functions/
	 * -> this is required for SMART Mobile packages
	 * 
	 * @param expression
	 * @return
	 * @throws Exception
	 */
	private static String convertVisibleWhenExpression(String expression, CmNode node) throws IOException{
		Parser parser = new Parser(new StringReader(expression));
		try {
			IFilter filter = parser.ParseQuery();
			StringBuilder sb = new StringBuilder();
			processFilter(filter,  sb, node);
			return sb.toString();
		}catch (Exception ex) {
			throw new IOException(ex);
		}
	}
	
	private static CmAttribute findAttributeId(String attributeKey, CmNode node) throws IOException{
		for (CmAttribute cmattribute : node.getCmAttributes()) {
			if (cmattribute.getAttribute().getKeyId().equalsIgnoreCase(attributeKey)) {
				return cmattribute;
			}
		}	
		throw new IOException(MessageFormat.format(Messages.CmSmartToXmlConverter_invalidVisibleWhenExpression, node.getCategory().getName(), attributeKey));
	}
	
	private static CmAttributeListItem findAttributeListItem(CmAttribute attribute, String listItemKey, CmNode node) throws IOException{
		for (CmAttributeListItem li : attribute.getConfig().getList()) {
			if (li.getListItem().getKeyId().equalsIgnoreCase(listItemKey)) {
				return li;
			}
		}
		//list node doesn't exist or is not configured for this attribute
		return null;
	}
	
	private static CmAttributeTreeNode findAttributeTreeNode(CmAttribute attribute, String treehKey, CmNode node) throws IOException{
		ArrayDeque<CmAttributeTreeNode> search = new ArrayDeque<>();
		search.addAll(attribute.getConfig().getTree());
		while(!search.isEmpty()) {
			CmAttributeTreeNode item = search.remove();
			if (item.getDmTreeNode().getHkey().equalsIgnoreCase(treehKey)){
				return item;
			}
			search.addAll(item.getChildren());
		}
		//tree node doesn't exist or is not configured for this attribute
		return null;
	}
	
	private static void processFilter(IFilter filter, StringBuilder sb, CmNode node) throws IOException{
		
		if (filter instanceof BooleanFilter) {
			BooleanFilter bfilter = (BooleanFilter)filter;
			processFilter(bfilter.getFilter1(), sb, node);
			sb.append(" "); //$NON-NLS-1$
			sb.append(bfilter.getOperator().asSmartValue().toLowerCase());
			sb.append(" "); //$NON-NLS-1$
			processFilter(bfilter.getFilter2(), sb, node);
		}else if (filter instanceof BracketFilter) {
			BracketFilter bfilter = (BracketFilter)filter;
			sb.append(" ( "); //$NON-NLS-1$
			processFilter(bfilter.getFilter(), sb, node);
			sb.append(" ) "); //$NON-NLS-1$
		}else if (filter instanceof NotFilter) {
			NotFilter bfilter = (NotFilter)filter;
			sb.append(" not( "); //$NON-NLS-1$
			processFilter(bfilter.getFilter(), sb, node);
			sb.append(" ) "); //$NON-NLS-1$
		}else if (filter instanceof AttributeFilter) {
			AttributeFilter afilter = (AttributeFilter)filter;
			sb.append(" "); //$NON-NLS-1$
			
			CmAttribute cattribute = findAttributeId(afilter.getAttributeKey(), node);
			String attributeKey = cattribute.getUuid().toString();
			switch(afilter.getAttributeType()) {
			case BOOLEAN:
				sb.append(" ${"); //$NON-NLS-1$
				sb.append(attributeKey);
				sb.append("}"); //$NON-NLS-1$
				sb.append(" "); //$NON-NLS-1$
				break;
			case DATE:
				if (afilter.getOperator() ==  Operator.NOT_BETWEEN) {
					sb.append(" not ("); //$NON-NLS-1$
				}else {
					sb.append(" ("); //$NON-NLS-1$
				}
				sb.append(" ${"); //$NON-NLS-1$
				sb.append(attributeKey);
				sb.append("} >= "); //$NON-NLS-1$
				sb.append(afilter.getValue().toString());
				sb.append(" "); //$NON-NLS-1$
				sb.append(" and "); //$NON-NLS-1$
				sb.append(" ${"); //$NON-NLS-1$
				sb.append(attributeKey);
				sb.append("} <= "); //$NON-NLS-1$
				sb.append(afilter.getValue().toString());
				sb.append(" "); //$NON-NLS-1$
				
				sb.append(" )"); //$NON-NLS-1$
				break;
			case LIST:
				if (afilter.getValue().toString().equalsIgnoreCase(AttributeFilter.ANY_OPTION_KEY)) {
					sb.append(" ${"); //$NON-NLS-1$
					sb.append(attributeKey);
					sb.append("} != ''"); //$NON-NLS-1$
				}else {
					CmAttributeListItem li = findAttributeListItem(cattribute, afilter.getValue().toString(), node);
					if (li == null) {
						//always false as list item not configured for this attribute
						sb.append(" boolean(0) "); //$NON-NLS-1$
					}else {
						sb.append(" ${"); //$NON-NLS-1$
						sb.append(afilter.getAttributeKey());
						sb.append("} = '"); //$NON-NLS-1$
						sb.append(li.getUuid().toString());
						sb.append("' "); //$NON-NLS-1$
					}
				}
				break;
			case MLIST:
				StringBuilder temp = new StringBuilder();
				temp.append(" checklist_match ( "); //$NON-NLS-1$
				temp.append("${"); //$NON-NLS-1$
				temp.append(attributeKey);
				temp.append("}, "); //$NON-NLS-1$
				
				if (afilter.getOperator() == Operator.OR) {
					temp.append(" 'or' "); //$NON-NLS-1$
				}else if (afilter.getOperator() == Operator.AND) {
					temp.append(" 'and' "); //$NON-NLS-1$
				}else if (afilter.getOperator() == Operator.EXACT) {
					temp.append(" 'exact' "); //$NON-NLS-1$
				}
				
				temp.append(" [ "); //$NON-NLS-1$
				for (String key : afilter.getValue().toString().split(AttributeFilter.MLIST_SEPERATOR)) {
					
					CmAttributeListItem li = findAttributeListItem(cattribute, key, node);
					if (li != null) {
						temp.append("'"); //$NON-NLS-1$
						temp.append(li.getUuid().toString());
						temp.append("', "); //$NON-NLS-1$
					}else {
						//list item not configured for this attribute
						if (afilter.getOperator() == Operator.OR) {
							//skip item; or might be true if one of others
						}else {
							//and/exact can never be true so assume false
							sb.append(" boolean(0)"); //$NON-NLS-1$
							temp = null;
							break;
						}
					}
				}
				if (temp != null) {
					temp.deleteCharAt(temp.length() - 1);
					temp.deleteCharAt(temp.length() - 1);
					temp.append("] ) "); //$NON-NLS-1$
					sb.append( temp );
				}
				break;
			case NUMERIC:
				sb.append(" ${"); //$NON-NLS-1$
				sb.append(attributeKey);
				sb.append("} "); //$NON-NLS-1$
				sb.append(afilter.getOperator().asSmartValue() );
				sb.append(" "); //$NON-NLS-1$
				sb.append(afilter.getValue().toString());
				sb.append(" "); //$NON-NLS-1$
				break;
			case TEXT:
				
				if (afilter.getOperator() == Operator.EQUALS) {
					sb.append(" ${"); //$NON-NLS-1$
					sb.append(attributeKey);
					sb.append("} = '"); //$NON-NLS-1$
					sb.append(afilter.getValue().toString());
					sb.append("' "); //$NON-NLS-1$
				}else if (afilter.getOperator() == Operator.STR_CONTAINS) {
					sb.append(" contains(${"); //$NON-NLS-1$
					sb.append(attributeKey);
					sb.append("}, '"); //$NON-NLS-1$
					sb.append(afilter.getValue().toString());
					sb.append("')"); //$NON-NLS-1$
				}else if (afilter.getOperator() == Operator.STR_NOTCONTAINS) {
					sb.append(" not(contains(${"); //$NON-NLS-1$
					sb.append(attributeKey);
					sb.append("}, '"); //$NON-NLS-1$
					sb.append(afilter.getValue().toString());
					sb.append("'))"); //$NON-NLS-1$
				}
				break;
			case TREE:
				CmAttributeTreeNode tnode = findAttributeTreeNode(cattribute,afilter.getValue().toString(), node);
				if (tnode == null) {
					//always false as node doesn't exist for this configuration
					sb.append(" boolean(0) "); //$NON-NLS-1$
				}else {
					sb.append(" ${"); //$NON-NLS-1$
					sb.append(attributeKey);
					sb.append("} = '"); //$NON-NLS-1$
					sb.append(tnode.getUuid().toString());
					sb.append("' "); //$NON-NLS-1$
				}
				break;
			default:
				break;
			
			}
			
			
		}
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
	}

	private static boolean isCustomIcon(IImageAssociatedObject obj) {
		if (obj instanceof CmNode) return ((CmNode) obj).hasCustomImage();
		if (obj instanceof CmAttribute) return ((CmAttribute) obj).hasCustomImage();
		if (obj instanceof CmAttributeListItem) return ((CmAttributeListItem) obj).hasCustomImage();
		if (obj instanceof CmAttributeTreeNode) return ((CmAttributeTreeNode) obj).hasCustomImage();
		return false;
	}
	private static String getImageFileRef(IImageAssociatedObject obj, ConfigurableModel cm, boolean forCybertracker) {
		Path file = obj.getImageFile();
		if (obj.getUuid() == null) {
			DmObject dm = null;
			if (obj instanceof CmNode) {
				if (((CmNode)obj).getCategory() == null) return null;
				dm = ((CmNode)obj).getCategory();
			}else if (obj instanceof CmAttribute) {
				if (((CmAttribute)obj).getAttribute() == null) return null;
				dm = ((CmAttribute)obj).getAttribute();
			}else if (obj instanceof CmAttributeListItem) {
				if (((CmAttributeListItem)obj).getListItem() == null) return null;
				dm = ((CmAttributeListItem)obj).getListItem();
			}else if (obj instanceof CmAttributeTreeNode) {
				if (((CmAttributeTreeNode)obj).getDmTreeNode() == null) return null;
				dm = ((CmAttributeTreeNode)obj).getDmTreeNode();
			}
			if (dm == null) return null;
			file = Paths.get( UuidUtils.uuidToString( dm.getUuid()) );
		}
		if (file == null) return null;
		if (Files.exists(file)) return file.getFileName().toString();
		
		if (obj instanceof CmNode) {
			CmNode node = (CmNode) obj;
			if (node.getCategory() == null) return null;
			if (node.getCategory().getIcon() == null) return null;
			IconFile iconFile = node.getCategory().getIcon().getIconFile(cm.getIconSet());
			if (iconFile == null) return null;
			if (iconFile != null) return findFileName(file.getFileName().toString(), iconFile.getFilename());
		}else if (obj instanceof CmAttribute) {
			CmAttribute node = (CmAttribute) obj;
			if (node.getAttribute() == null) return null;
			if (node.getAttribute().getIcon() == null) return null;
			IconFile iconFile = node.getAttribute().getIcon().getIconFile(cm.getIconSet());
			if (iconFile == null) return null;
			if (iconFile != null) return findFileName(file.getFileName().toString(), iconFile.getFilename());
		}else if (obj instanceof CmAttributeListItem) {
			CmAttributeListItem node = (CmAttributeListItem) obj;
			if (node.getListItem() == null) return null;
			if (node.getListItem().getIcon() == null) return null;
			IconFile iconFile = node.getListItem().getIcon().getIconFile(cm.getIconSet());
			if (iconFile == null) return null;
			if (iconFile != null) return findFileName(file.getFileName().toString(), iconFile.getFilename());
		}else if (obj instanceof CmAttributeTreeNode) {
			CmAttributeTreeNode node = (CmAttributeTreeNode) obj;
			if (node.getDmTreeNode() == null) return null;
			if (node.getDmTreeNode().getIcon() == null) return null;
			IconFile iconFile = node.getDmTreeNode().getIcon().getIconFile(cm.getIconSet());
			if (iconFile == null) return null;
			if (iconFile != null) return findFileName(file.getFileName().toString(), iconFile.getFilename());
		}
		return null;
	}
	
	private static String findFileName(String cmFileName, String dmIconFileName) {
		return SharedUtils.getFilenameWithoutExtension(cmFileName) + "." + SharedUtils.getFilenameExtension(dmIconFileName); //$NON-NLS-1$
	}
}
