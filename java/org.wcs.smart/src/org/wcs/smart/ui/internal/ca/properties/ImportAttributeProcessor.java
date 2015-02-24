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
package org.wcs.smart.ui.internal.ca.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.wcs.smart.SmartApp;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSmartConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;
import org.wcs.smart.ui.internal.ca.properties.ImportAttributeDialog.ParentTreeNodeType;

/**
 * Processor for importing attribute information from an xml file.
 * <p>This class attemps to find an xml file that matches the key of
 * the provided attribute and displays a dialog to users where they can select
 * items to import.
 * </p><p>Currently only works for 
 * tree attribute types but could be extended to other
 * attribute types in the future.</p>
 *  
 * 
 * @author Emily
 *
 */
public class ImportAttributeProcessor {

	private DataModel xmlDataModel = null;
	private Attribute attribute;
	private AttributeType matchedAttribute;
	private List<AttributeTreeNode> workingRoots = null;
	private String defaultLangCode = null;
	
	public ImportAttributeProcessor(Attribute attribute, List<AttributeTreeNode> workingRootNodes){
		this.attribute = attribute;
		this.workingRoots = workingRootNodes;
	}
	
	
	/**
	 * Runs the process for importing attributes
	 * from given xml file.
	 */
	public void importAttribute(){
		try {
			if (!hasPredefinedFile()){
				//prompt for file to import data from
				MessageDialog.openInformation(Display.getDefault().getActiveShell(),
						Messages.ImportAttributeProcessor_MessageDialogTitle, 
						MessageFormat.format(Messages.ImportAttributeProcessor_NoAttributeFileMessage, new Object[]{attribute.getName() }));
				
				//read the selected file
				File f = promptForFile();
				if (f == null){
					return;
				}
				readDataModel(f);
			}else{
				readDefaultFile();
			}
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.ImportAttributeProcessor_ErrorReadingXml + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return;
		}
		
		if (!validateInputFile()){
			return;
		}

			
		ImportAttributeDialog dialog = new ImportAttributeDialog(Display.getDefault().getActiveShell(), this);
			
		if (dialog.open() != ImportAttributeDialog.OK){
			return;
		}
		
		List<TreeNodeType> roots = dialog.getSelectedNodes();
		List<AttributeTreeNode> newNodes = new ArrayList<AttributeTreeNode>();
		for (TreeNodeType root : roots){
			newNodes.add(convertNodeCascade(root, null));
		}
		
		if (workingRoots == null){
			workingRoots = new ArrayList<AttributeTreeNode>();
		}
		//merge
		for(AttributeTreeNode newNode : newNodes){
			boolean found = false;

			for (AttributeTreeNode existing : workingRoots){
				if (existing.getKeyId().equals(newNode.getKeyId())){
					found = true;
					mergeNodes(existing, newNode);
					break;
				}
			}
			if (!found){
				newNode.updateHkey();
				workingRoots.add(newNode);
				newNode.setNodeOrder(workingRoots.size());
			}
		}
		
	}

	/**
	 * Validates input file.  Ensures the attribute
	 * exists and the default language is setup properly.
	 * 
	 * @return <code>true</code> if validations successful, <code>false</code> if should not continue
	 */
	public boolean validateInputFile(){
		if (matchedAttribute == null){
			SmartPlugIn.displayLog(MessageFormat.format(Messages.ImportAttributeProcessor_NoAttributeFound, new Object[]{attribute.getKeyId()}), null);
			return false;
		}
		
		defaultLangCode = DataModelXmlToSmartConverter.checkLanguage(xmlDataModel.getLanguages().getLanguages(), attribute.getConservationArea());
		if (defaultLangCode == null){
			return false;
		}
		return true;
	}
	

	/**
	 * 
	 * @return the data model xml attribute that
	 * matches the current smart attribute
	 */
	public AttributeType getMatchedAttribute(){
		return this.matchedAttribute;
	}
	
	/**
	 * 
	 * @return the current smart attribute being processed
	 */
	public Attribute getAttribute(){
		return this.attribute;
	}
	
	/**
	 * 
	 * @return the default language code selected by the user
	 */
	public String getDefaultLangCode(){
		return this.defaultLangCode;
	}
	
	/**
	 * Prompts the user for a data model file.
	 * @return
	 */
	public File promptForFile(){
		
		FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell());
		fileDialog.setFilterExtensions(new String[]{"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fileDialog.setFilterNames(new String[]{Messages.ImportAttributeProcessor_XMLFileFilterName, Messages.ImportAttributeProcessor_AllFileFilterName});
		String selectedFile = fileDialog.open();
		if (selectedFile == null){
			return null;
		}
		File f = new File(selectedFile);
		if (!f.exists()){
			SmartPlugIn.displayLog(Messages.ImportAttributeProcessor_FileNotFoundError, new Exception("File: " + f.toString() + " does not exist."));  //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}
		return f;
	}
	
	
	/**
	 * Merges two sets of tree nodes based on the keyids
	 * 
	 * @param existingNode
	 * @param newNode
	 */
	private  void mergeNodes(AttributeTreeNode existingNode, AttributeTreeNode newNode){
		for (AttributeTreeNode kid : newNode.getChildren()){
			boolean found = false;
			for (AttributeTreeNode existingkid : existingNode.getChildren()){
				if (existingkid.getKeyId().equals(kid.getKeyId())){
					found = true;
					mergeNodes(existingkid, kid);
				}
				
			}
			if (!found){
				//we need to add tomerge to existing
				kid.setParent(existingNode);
				existingNode.getChildren().add(kid);
				kid.updateHkey();
				kid.setNodeOrder(existingNode.getChildren().size());
			}
		}
	}
	
	/*
	 * Converts a xml tree node to a smart tree node.
	 */
	private  AttributeTreeNode convertNode(TreeNodeType toClone,
			AttributeTreeNode parent) {
		
		HashMap<String, Language>langLookup = new HashMap<String, Language>();
		for (Language lang : attribute.getConservationArea().getLanguages()) {
			langLookup.put(lang.getCode(), lang);
		}
		
		AttributeTreeNode cloned = new AttributeTreeNode();
		cloned.setAttribute(attribute);
		cloned.setKeyId(toClone.getKey());
		for (NameType nt : toClone.getNames()){
			String value = nt.getValue();
			Language l = langLookup.get(nt.getLanguageCode());
			if (l != null){
				cloned.updateName(l, value);
			}
			if (nt.getLanguageCode().equals(defaultLangCode)){
				cloned.updateName(attribute.getConservationArea().getDefaultLanguage(), value);
			}
		}
		
		cloned.setActiveChildren(new ArrayList<AttributeTreeNode>());
		cloned.setChildren(new ArrayList<AttributeTreeNode>());
		cloned.setIsActive(toClone.isIsactive());
		cloned.setParent(parent);
		
		return cloned;
	}
	
	/*
	 * Converts and xml tree node and it's children to a smart tree node.
	 */
	private AttributeTreeNode convertNodeCascade(TreeNodeType toClone, AttributeTreeNode parent){
		AttributeTreeNode cloned = convertNode(toClone, parent);
		
		int i = 1;
		for (TreeNodeType child : toClone.getChildrens()){
			AttributeTreeNode clonedchild = convertNodeCascade((ParentTreeNodeType)child, cloned);
			clonedchild.setNodeOrder(i++);
			clonedchild.setParent(cloned);
			
			cloned.getChildren().add(clonedchild);
			if (clonedchild.getIsActive()){
				cloned.getActiveChildren().add(clonedchild);
			}
			
		}
		return cloned;
	}

	/**
	 * 
	 * @return <code>true</code> if there exists a predefined file
	 * for the given attribute
	 */
	private boolean hasPredefinedFile(){
		URL url = SmartProperties.class.getResource(getAttributeFile());
		return url != null;
	}
	
	/**
	 * The path of the "predefined" xml file.
	 * @return
	 */
	private String getAttributeFile(){
		return "/" + SmartProperties.PROPERTIES_DIR + "/" + attribute.getKeyId() + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	/**
	 * Reads a smart data model from a file.
	 * 
	 * @param f the file to read
	 * @throws Exception
	 */
	public void readDataModel(File f) throws Exception{
		FileInputStream in = new FileInputStream(f);
		importAttributeDataModel(in);
		
	}
	
	
	private void readDefaultFile() throws Exception{
		InputStream in = SmartApp.class.getClassLoader().getResourceAsStream(getAttributeFile());
		if (in == null){
			//no file found
			return ;
		}
		importAttributeDataModel(in);
	}
	
	/*
	 * read the xml file from an input stream.  Closes the input
	 * stream when finished reading.
	 */
	private void importAttributeDataModel(InputStream in) throws Exception{
		xmlDataModel = null;
		matchedAttribute = null;
		defaultLangCode = null;
		
		try{
			xmlDataModel = XmlSmartDataModelManager.readDataModel(in);
		}finally{
			in.close();
		}
		if (xmlDataModel == null){
			throw new Exception("Data model is null."); //$NON-NLS-1$
		}
		for (AttributeType type : xmlDataModel.getAttributes().getAttributes()){
			if (type.getKey().equals(attribute.getKeyId())){
				matchedAttribute = type;
				break;
			}
		}	
	}
	
	
}
