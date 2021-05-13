package org.wcs.smart.dataentry.model.xml;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.dataentry.model.xml.external.ICmXmlExtraDataExporter;
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
public class CmSmartToXml {
	
	private Session session;
	
	private boolean forSmartMobile = false;
	
	private org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel;
	private HashMap<String, Language> llookup;
	private ConfigurableModel cmModel;
	private Map<String, Path> filesToInclude;
	private Set<UUID> signatures;

	
	public CmSmartToXml(Session session) {
		this(session, false);
	}
	
	public CmSmartToXml(Session session, boolean forSmartMobile) {
		this.session = session;
		this.forSmartMobile = forSmartMobile;
		filesToInclude = new HashMap<>();
	}
	
	public org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel getXmlModel(){
		return this.xmlModel;
	}
	
	public Map<String, Path> getReferencedFiles(){
		return this.filesToInclude;
	}
	
	/**
	 * 
	 * @param cm
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public void convert(ConfigurableModel cm, IProgressMonitor monitor) throws OperationCanceledException, IOException {
		
		monitor.beginTask("", 4); //$NON-NLS-1$
		
		xmlModel = new org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel();
		this.cmModel = cm;
		if (cm.getUuid() != null) {
			this.cmModel = session.get(ConfigurableModel.class, cm.getUuid());
		}

		if (monitor.isCanceled()) throw new OperationCanceledException();
		
		processLanguages();
		setNames(xmlModel.getName(), cmModel.getNames());
		
		if (cmModel.getDisplayMode() != null) {
			xmlModel.setDisplayMode(cmModel.getDisplayMode().name());
		}
				
		if (cmModel.getIconSet() != null) xmlModel.setIconSet(cmModel.getIconSet().getKeyId());
		xmlModel.setInstantGps(cmModel.isInstantGps());
		xmlModel.setPhotoFirst(cmModel.isPhotoFirst());
		monitor.worked(1);
		
		if (monitor.isCanceled()) throw new OperationCanceledException();
				
		addSignatureTypeMetadata(cmModel);
				
		//nodes
		processCmNodes(monitor);
		monitor.worked(1);
		if (monitor.isCanceled()) throw new OperationCanceledException();
				
		processCmAttributeConfigs(monitor);
		monitor.worked(1);
		if (monitor.isCanceled()) throw new OperationCanceledException();
		
		List<ICmXmlExtraDataExporter> extensions = Collections.emptyList();
		try{
			extensions = XmlCmExtraDataContributionFactory.getExporters();
		}catch (Exception ex) {
			//eat this exception
			Logger.getLogger(CmSmartToXml.class.getName()).log(Level.WARNING, "failed to load configurable model additions", ex); //$NON-NLS-1$
		}
		
		for (ICmXmlExtraDataExporter dc : extensions) {
			List<CmExtraDataType> extraData = dc.exportData(cmModel, session);
			if (extraData != null) {
				xmlModel.getExtraData().addAll(extraData);
			}
		}
		monitor.worked(1);
		if (monitor.isCanceled()) throw new OperationCanceledException();
	}

	private void addSignatureTypeMetadata(ConfigurableModel cm) {
		//find and write signatures
		Set<UUID> allsignatures = new HashSet<>();
		ArrayDeque<CmNode> nodes = new ArrayDeque<>();
		nodes.addAll(cm.getNodes());
		while(!nodes.isEmpty()) {
			CmNode node = nodes.remove();
			allsignatures.addAll(node.getSignatureUuids());
			nodes.addAll(node.getChildren());
		}
		signatures = new HashSet<>();
		if (!allsignatures.isEmpty()) {
		
			xmlModel.setSignatures(new SignatureTypeList());
			
			for (UUID sig : allsignatures) {
				SignatureType stype = session.get(SignatureType.class, sig);
				if (stype != null) {
					org.wcs.smart.dataentry.model.xml.generated.SignatureType xtype = new org.wcs.smart.dataentry.model.xml.generated.SignatureType();
					xtype.setUuid(UuidUtils.uuidToString(stype.getUuid()));
					setNames(xtype.getName(), stype.getNames());
					xtype.setKeyid(stype.getKeyId());
					xmlModel.getSignatures().getSignatureType().add(xtype);
					signatures.add(stype.getUuid());
				}
			}
		}
	}
	
	private void processCmAttributeConfigs(IProgressMonitor monitor) {
		Collection<CmAttributeConfig> configs = Collections.emptyList();
		
		if (cmModel.getUuid() == null) {
			//if there is no uuid; then we just use what is in the cm 
			configs = cmModel.getDefaultConfigs().values();
		}else {
			configs = QueryFactory.buildQuery(session, CmAttributeConfig.class, "model", cmModel).list(); //$NON-NLS-1$
		}
		for (CmAttributeConfig config : configs) {
			CmAttributeConfigType xmlConfig = new CmAttributeConfigType();
			setNames(xmlConfig.getName(), config.getNames());
			if (config.getUuid() != null) {
				xmlConfig.setId(config.getUuid().toString());
			}
			xmlConfig.setIsDefault(config.isDefault());
			if (config.getDisplayMode() != null) {
				xmlConfig.setDisplayMode(config.getDisplayMode().name());
			}
			xmlConfig.setAttributeKey(config.getAttribute().getKeyId());
			xmlConfig.setAttributeUuid(toString(config.getAttribute().getUuid()));
			
			processCmListItems(config.getList(), xmlConfig.getListItem(), monitor);
			processCmTreeNodes(config.getTree(), xmlConfig.getTreeNode(), monitor);
			
			xmlModel.getAttributeConfig().add(xmlConfig);
		}
		
	}

	private static String toString(UUID uuid) {
		return UuidUtils.uuidToString(uuid);
	}
	
	private void processCmTreeNodes(List<CmAttributeTreeNode> cmList, List<TreeNodeType> xmlList, IProgressMonitor monitor) {

		if(monitor.isCanceled()) return;
		
		for (CmAttributeTreeNode cmNode : cmList) {
			TreeNodeType xmlNode = new TreeNodeType();
			setNames(xmlNode.getName(), cmNode.getNames(), cmNode.getDmTreeNode() == null ? Collections.emptySet() :  cmNode.getDmTreeNode().getNames());
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
			xmlNode.setIsCustomImage(isCustomIcon(cmNode));
			if (cmNode.getUuid() != null) {
				xmlNode.setId(cmNode.getUuid().toString()); //this will allow to reference this item in extradata
			}
			processCmTreeNodes(cmNode.getChildren(), xmlNode.getChildren(), monitor);
			xmlList.add(xmlNode);
		}
	}

	private void processCmListItems(List<CmAttributeListItem> cmList, 
			List<ListItemType> xmlList,	 IProgressMonitor monitor) {

		if(monitor.isCanceled()) return;
		for (CmAttributeListItem cmNode : cmList) {
			ListItemType xmlNode = new ListItemType();
			setNames(xmlNode.getName(), cmNode.getNames(), cmNode.getListItem().getNames());
			xmlNode.setIsActive(cmNode.getIsActive());
			if (cmNode.getListItem() == null){
				xmlNode.setKeyRef(null);
			}else{
				xmlNode.setKeyRef(cmNode.getListItem().getKeyId());
				xmlNode.setDmUuid(toString(cmNode.getListItem().getUuid()));
			}
			xmlNode.setImageFile(getImageFileRef(cmNode));
			xmlNode.setIsCustomImage(isCustomIcon(cmNode));
			if (cmNode.getUuid() != null) {
				xmlNode.setId(cmNode.getUuid().toString()); //this will allow to reference this item in extradata
			}
			xmlList.add(xmlNode);
		}
	}

	private void processCmNodes(IProgressMonitor monitor) throws IOException  {
		NodeTypeList ntl = new NodeTypeList();
		xmlModel.setNodes(ntl);
		if (cmModel.getNodes() != null) {
			for (CmNode node : cmModel.getNodes()){
				processCmNode(node, ntl.getNode(), monitor);
			}
		}
	}

	private void processCmNode(CmNode node, List<NodeType> xmlNodes, IProgressMonitor monitor) throws IOException  {

		NodeType nt = new NodeType();
		setNames(nt.getName(), node.getNames());
		if (node.getCategory() != null) {
			nt.setCategoryKey(node.getCategory().getKeyId());
			nt.setCategoryHkey(node.getCategory().getHkey());
			nt.setDmUuid(toString(node.getCategory().getUuid()));
			
			//only some cateogories can collect signatures
			for (UUID uuid : node.getSignatureUuids()) {
				if (signatures.contains(uuid)) {
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
		
		nt.setImageFile(getImageFileRef(node));
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
				setNames(at.getName(), ca.getNames());
				at.setAttributeKey(ca.getAttribute().getKeyId());
				at.setDmUuid(toString(ca.getAttribute().getUuid()));
				at.setRequired(ca.getAttribute().getIsRequired());
				
				at.setIsCustomImage(isCustomIcon(ca));
				at.setImageFile(getImageFileRef(ca));
		
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
					
					if (forSmartMobile && option.getOptionId().equalsIgnoreCase(CmAttributeOption.ID_IS_VISIBLE)) {
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
				processCmNode(cn, nt.getNode(), monitor);
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
	private String convertVisibleWhenExpression(String expression, CmNode node) throws IOException{
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
	
	private CmAttribute findAttributeId(String attributeKey, CmNode node) throws IOException{
		for (CmAttribute cmattribute : node.getCmAttributes()) {
			if (cmattribute.getAttribute().getKeyId().equalsIgnoreCase(attributeKey)) {
				return cmattribute;
			}
		}	
		throw new IOException(MessageFormat.format("Visible-when expression is invalid for an attribute in the category {0}.  Attribute with key {1} not found.", node.getCategory().getName(), attributeKey)); //$NON-NLS-1$
	}
	
	private CmAttributeListItem findAttributeListItem(CmAttribute attribute, String listItemKey, CmNode node) throws IOException{
		for (CmAttributeListItem li : attribute.getConfig().getList()) {
			if (li.getListItem().getKeyId().equalsIgnoreCase(listItemKey)) {
				return li;
			}
		}
		//list node doesn't exist or is not configured for this attribute
		return null;
	}
	
	private CmAttributeTreeNode findAttributeTreeNode(CmAttribute attribute, String treehKey, CmNode node) throws IOException{
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
	
	private void processFilter(IFilter filter, StringBuilder sb, CmNode node) throws IOException{
		
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
	
	private void processLanguages() {
		llookup = new HashMap<String, Language>();
		LanguageListType llt = new LanguageListType();
		xmlModel.setLanguages(llt);
		for (Language ll : cmModel.getConservationArea().getLanguages()) {
			LanguageType lt = new LanguageType();
			lt.setCode(ll.getCode());
			lt.setIsDefault(ll.equals(cmModel.getConservationArea().getDefaultLanguage()));
			llt.getLanguage().add(lt);
			llookup.put(UuidUtils.uuidToString(ll.getUuid()), ll);
		}	
	}
	
	private void setNames(List<NameType> list, Set<Label> elementNames){
		setNames(list, elementNames, null);
	}
	
	private void setNames(List<NameType> list, Set<Label> elementNames, Set<Label> srcElementNames){
	
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

	private boolean isCustomIcon(IImageAssociatedObject obj) {
		if (obj instanceof CmNode) return ((CmNode) obj).hasCustomImage();
		if (obj instanceof CmAttribute) return ((CmAttribute) obj).hasCustomImage();
		if (obj instanceof CmAttributeListItem) return ((CmAttributeListItem) obj).hasCustomImage();
		if (obj instanceof CmAttributeTreeNode) return ((CmAttributeTreeNode) obj).hasCustomImage();
		return false;
	}
	
	private String getImageFileRef(IImageAssociatedObject obj) {
		
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
		if (file != null) {
			if (Files.exists(file)) {
				filesToInclude.put(file.getFileName().toString(), file);
				return file.getFileName().toString();
			}
		}else {
			if (obj.getUuid() == null) return null;
			file = Paths.get(UuidUtils.uuidToString(obj.getUuid()));
		}
		
		if (obj instanceof CmNode) {
			CmNode node = (CmNode) obj;
			if (node.getCategory() == null) return null;
			if (node.getCategory().getIcon() == null) return null;
			IconFile iconFile = node.getCategory().getIcon().getIconFile(cmModel.getIconSet());
			if (iconFile == null) return null;
			
			String fname = findFileName(file.getFileName().toString(), iconFile.getFilename());
			iconFile.computeFileLocation(session);
			filesToInclude.put(fname, iconFile.getAttachmentFile());
			return fname;
			
		}else if (obj instanceof CmAttribute) {
			CmAttribute node = (CmAttribute) obj;
			if (node.getAttribute() == null) return null;
			if (node.getAttribute().getIcon() == null) return null;
			IconFile iconFile = node.getAttribute().getIcon().getIconFile(cmModel.getIconSet());
			if (iconFile == null) return null;
			
			String fname = findFileName(file.getFileName().toString(), iconFile.getFilename());
			iconFile.computeFileLocation(session);
			filesToInclude.put(fname, iconFile.getAttachmentFile());
			return fname;
			
		}else if (obj instanceof CmAttributeListItem) {
			CmAttributeListItem node = (CmAttributeListItem) obj;
			if (node.getListItem() == null) return null;
			if (node.getListItem().getIcon() == null) return null;
			IconFile iconFile = node.getListItem().getIcon().getIconFile(cmModel.getIconSet());
			if (iconFile == null) return null;
			
			String fname = findFileName(file.getFileName().toString(), iconFile.getFilename());
			iconFile.computeFileLocation(session);
			filesToInclude.put(fname, iconFile.getAttachmentFile());
			return fname;

		}else if (obj instanceof CmAttributeTreeNode) {
			CmAttributeTreeNode node = (CmAttributeTreeNode) obj;
			if (node.getDmTreeNode() == null) return null;
			if (node.getDmTreeNode().getIcon() == null) return null;
			IconFile iconFile = node.getDmTreeNode().getIcon().getIconFile(cmModel.getIconSet());
			if (iconFile == null) return null;
			
			String fname = findFileName(file.getFileName().toString(), iconFile.getFilename());
			iconFile.computeFileLocation(session);
			filesToInclude.put(fname, iconFile.getAttachmentFile());
			return fname;

		}
		return null;
	}
	
	private String findFileName(String cmFileName, String dmIconFileName) {
		return SharedUtils.getFilenameWithoutExtension(cmFileName) + "." + SharedUtils.getFilenameExtension(dmIconFileName); //$NON-NLS-1$
	}
	
//	private void processFile(DmObject object, IImageAssociatedObject cmObject, 
//			Path tempDir, Session session) throws IOException {
//		
//		IconFile file = object.getIcon().getIconFile(cmModel.getIconSet());
//		if (file != null) {
//			file.computeFileLocation(session);
//			Path fromPath = file.getAttachmentFile();
//			String fileName = cmObject.getImageFile() == null? cmObject.getDefaultImageFileName() : cmObject.getImageFile().getFileName().toString();
//			if (cmObject.getUuid() == null) {
//				fileName = UuidUtils.uuidToString(object.getUuid());
//			}
//			Path toPath = tempDir.resolve(SharedUtils.getFilenameWithoutExtension(fileName) + "." + SharedUtils.getFilenameExtension(fromPath.getFileName().toString())); //$NON-NLS-1$
//			if (Files.exists(toPath)) return;
//			Files.copy(fromPath, toPath);
//			
//		}
//	}
}
