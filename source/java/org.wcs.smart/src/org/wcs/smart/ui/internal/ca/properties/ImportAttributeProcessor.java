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

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.hibernate.Session;
import org.wcs.smart.SmartApp;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSmartConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelImporter;

/**
 * Processor for importing attribute information from an xml file.
 * <p>This class attempts to find an xml file that matches the key of
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

//	private DataModel xmlDataModel = null;
	private Attribute attribute;
//	private AttributeType matchedAttribute;
	private Attribute matchedAttribute;
	
	private List<AttributeTreeNode> workingRoots = null;
	private String defaultLangCode = null;
	private String currentKey = null;
	private Session session = null;
	
	public ImportAttributeProcessor(String currentKey, Attribute attribute, 
			List<AttributeTreeNode> workingRootNodes, Session session){
		this.currentKey = currentKey;
		this.attribute = attribute;
		this.workingRoots = workingRootNodes;
		this.session = session;
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
				Path f = promptForFile();
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
		
		List<AttributeTreeNode> roots = dialog.getSelectedNodes();
		List<AttributeTreeNode> newNodes = new ArrayList<AttributeTreeNode>();
		for (AttributeTreeNode root : roots){
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
			SmartPlugIn.displayLog(MessageFormat.format(Messages.ImportAttributeProcessor_NoAttributeFound, new Object[]{currentKey}), null);
			return false;
		}
		
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
	public Attribute getMatchedAttribute(){
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
	public Path promptForFile(){
		
		FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell());
		fileDialog.setFilterExtensions(new String[]{"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fileDialog.setFilterNames(new String[]{Messages.ImportAttributeProcessor_XMLFileFilterName, Messages.ImportAttributeProcessor_AllFileFilterName});
		String selectedFile = fileDialog.open();
		if (selectedFile == null){
			return null;
		}
		Path f = Paths.get(selectedFile);
		if (!Files.exists(f)){
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
	private  AttributeTreeNode convertNode(AttributeTreeNode toClone,
			AttributeTreeNode parent) {
		
		HashMap<String, Language>langLookup = new HashMap<String, Language>();
		for (Language lang : attribute.getConservationArea().getLanguages()) {
			langLookup.put(lang.getCode(), lang);
		}
		
		AttributeTreeNode cloned = new AttributeTreeNode();
		cloned.setAttribute(attribute);
		cloned.setKeyId(toClone.getKeyId());
		for (Label nt : toClone.getNames()){
			String value = nt.getValue();
			Language l = langLookup.get(nt.getLanguage().getCode());
			if (l != null){
				cloned.updateName(l, value);
			}
			if (nt.getLanguage().getCode().equals(defaultLangCode)){
				cloned.updateName(attribute.getConservationArea().getDefaultLanguage(), value);
			}
		}
		
		cloned.setActiveChildren(new ArrayList<AttributeTreeNode>());
		cloned.setChildren(new ArrayList<AttributeTreeNode>());
		cloned.setIsActive(toClone.getIsActive());
		cloned.setParent(parent);
		
		return cloned;
	}
	
	/*
	 * Converts and xml tree node and it's children to a smart tree node.
	 */
	private AttributeTreeNode convertNodeCascade(AttributeTreeNode toClone, AttributeTreeNode parent){
		AttributeTreeNode cloned = convertNode(toClone, parent);
	
		int i = 1;
		for (AttributeTreeNode child : toClone.getChildren()){
			AttributeTreeNode clonedchild = convertNodeCascade(child, cloned);
			clonedchild.setNodeOrder(i++);
			clonedchild.setParent(cloned);
			
			cloned.getChildren().add(clonedchild);
			if (clonedchild.getIsActive()){
				cloned.getActiveChildren().add(clonedchild);
			}
			clonedchild.setIcon(child.getIcon());
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
		return "/" + SmartProperties.PROPERTIES_DIR + "/" + currentKey + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	/**
	 * Reads a smart data model from a file.
	 * 
	 * @param f the file to read
	 * @throws Exception
	 */
	public void readDataModel(Path f) throws Exception{
		importAttributeDataModel(f, null);
	}
	
	
	private void readDefaultFile() throws Exception{
		InputStream in = SmartApp.class.getClassLoader().getResourceAsStream(getAttributeFile());
		if (in == null){
			//no file found
			return ;
		}
		importAttributeDataModel(null, in);
	}
	
	/*
	 * read the xml file from an input stream.  Closes the input
	 * stream when finished reading.
	 */
	private void importAttributeDataModel(Path file, InputStream in) throws Exception{
		matchedAttribute = null;
		defaultLangCode = null;
		
		List<Icon> icons = IconManager.INSTANCE.getIcons(session, attribute.getConservationArea());
			icons.addAll(IconManager.INSTANCE.getSystemIcons(session, attribute.getConservationArea()));

		List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
					new Object[] {"conservationArea", attribute.getConservationArea()}) //$NON-NLS-1$
					.list();
		
		XmlDataModelImporter importer = new XmlDataModelImporter(icons, sets, Locale.getDefault(), null);
		if (file != null) {
			importer.processFile(file);
		}else if (in != null) {
			importer.processInputStream(in);
		}else {
			throw new Exception("No file to import"); //$NON-NLS-1$
		}
		
		if (importer.getImportedDataModel() == null) throw new Exception("Data model is null."); //$NON-NLS-1$
		
		for (Attribute a : importer.getImportedDataModel().getAttributes()) {
			if (a.getKeyId().equalsIgnoreCase(currentKey)) {
				matchedAttribute = a;
				break;
			}
		}

		SimpleDataModel dm = importer.getImportedDataModel();
		Set<String> lcodes = new HashSet<>();
		//collect all language codes used in data model
		dm.getCategories().forEach(category->{
			category.accept(e->{
				e.getNames().forEach(l->lcodes.add(l.getLanguage().getCode()));
				return true;
			});
		});
		dm.getAttributes().forEach(a->{
			a.getNames().forEach(l->lcodes.add(l.getLanguage().getCode()));
			if (a.getAttributeList() != null) {
				a.getAttributeList().forEach(li -> li.getNames().forEach(l->lcodes.add(l.getLanguage().getCode())));
			}
			if (a.getTree() != null) {
				for (AttributeTreeNode node : a.getTree()) {
					 node.accept(e->{
						e.getNames().forEach(l->lcodes.add(l.getLanguage().getCode()));
						return true;
					});
				}
			}
		});
		
		defaultLangCode = DataModelXmlToSmartConverter.checkLanguage(lcodes, attribute.getConservationArea());
	}
	
	
}
