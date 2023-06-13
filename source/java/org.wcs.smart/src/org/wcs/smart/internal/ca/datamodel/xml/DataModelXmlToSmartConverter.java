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
package org.wcs.smart.internal.ca.datamodel.xml;

import java.io.InputStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.LanguageSelectionDialog;

import jakarta.xml.bind.JAXBException;

/**
 * Converts a SMART XML data model to the database
 * data model.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DataModelXmlToSmartConverter {

	private HashMap<String, Language> langLookup;	
	private List<Aggregation> aggs;
	private String useAsDefault = null;
	
	private ConservationArea targetCa = null;
	
	/**
	 * Converts an xml data model file represented by the input stream 
	 * into a SMART datamodel file. The XML input stream must be the latest
	 * version of the data model xml.
	 * 
	 * @param is input stream representing xm
	 * @param targetCa target conservation area
	 * @param icons existing icons in the conservation area
	 * @param sets existing icon sets in the conservation area
	 * @param syncLanguages see comment below
	 */
	public DataModel convert(InputStream is, ConservationArea targetCa, 
			Collection<Icon> icons,
			Collection<IconSet> sets,
			boolean syncLanguages, Path workingDirectory) throws Exception {
		this.targetCa = targetCa;
		useAsDefault = null;	

		//this converts without a conservation area
		XmlDataModelImporter cmimporter = new XmlDataModelImporter(icons, sets, Locale.getDefault(), workingDirectory);
		cmimporter.processInputStream(is);
		SimpleDataModel sdm = cmimporter.getImportedDataModel();
		
		return processDataModelInternal(syncLanguages, sdm);
	}
	
	/**
	 * Converts xml data model into smart database data model
	 * 
	 * 
	 * Requires that ca.getlanguages is called or an open hibernate session
	 * with ca attached is available to lazily load languages
	 * <p>
	 * If synchronizing then there must be an active display.
	 * </p>
	 * <p>
	 * Synchronizing data model languages has two affects:
	 * </p>
	 * <p>
	 * 1. If the target conservation area default language is
	 * not in the data model it prompts the user to
	 * select which language to use in the data model as the same as the default language.
	 * This will cause the labels from that language to be copied into the
	 * default language labels (for the conservation area).
	 * </p>
	 * <p>
	 * 2. If a language in the data model does not exist in the conservation area the user
	 * is prompted to add the language to the target conservation area.
	 * </p> 
	 * @param is input stream of xml data model
	 * @param targetCa conservation area
	 * @param icons set of icons to map xml items to can be null 
	 * @param syncLanguages if the languages of the data model and conservation should be synchronized
	 * @return data model generated from xml data model
	 * 
	 *  
	 * @throws JAXBException
	 * @throws ParseException
	 */
	public DataModel convert(Path file, ConservationArea targetCa, 
			Collection<Icon> icons,
			Collection<IconSet> sets,
			boolean syncLanguages,
			Path workingDirectory) throws Exception {
		this.targetCa = targetCa;
		useAsDefault = null;	

		//this converts without a conservation area
		XmlDataModelImporter cmimporter = new XmlDataModelImporter(icons, sets, Locale.getDefault(), workingDirectory);
		cmimporter.processFile(file);
		SimpleDataModel sdm = cmimporter.getImportedDataModel();
		return processDataModelInternal(syncLanguages, sdm);
	}

	private DataModel processDataModelInternal(boolean syncLanguages, SimpleDataModel sdm)
			throws ParseException {
		//get aggregations
		aggs = DataModel.getAggregations();

		//configure languages for dm objects
		Set<String> languageCodes = findLanguageCodes(sdm);
		getLanguages();  
		if (syncLanguages){
			//here we check to ensure default ca lang
			useAsDefault = checkLanguage(languageCodes, targetCa);
			if (useAsDefault == null){
				return null;
			}
		}
		
		//create data model
		DataModel dm = new DataModel(targetCa, Collections.<Category> emptyList(), Collections.<Attribute> emptyList());
		//add categories
		dm.getCategories().addAll(sdm.getCategories());
		//add attributes
		dm.getAttributes().addAll(sdm.getAttributes());
		
		//update aggregations and labels for attributes
		for (Attribute a : dm.getAttributes()) {
			a.setConservationArea(targetCa);
			
			List<Aggregation> newAggregations = new ArrayList<>();
			for (Aggregation c : newAggregations) {
				Aggregation newAgg = lookUpAggregation(c.getName());
				if (newAgg != null) newAggregations.add(newAgg);
			}
			a.getAggregations().clear();
			a.getAggregations().addAll(newAggregations);
			
			updateNamesAndIcons(a);
			if (a.getAttributeList() != null) {
				a.getAttributeList().forEach(item->updateNamesAndIcons(item));
			}
			
			if (a.getTree() != null) {
				DataModel.processAttributeTree(a, node->updateNamesAndIcons(node));
			}
		}
		
		//update labels for categories
		DataModel.processCategories(dm, c->{
			c.setConservationArea(targetCa);
			updateNamesAndIcons(c);
		});
		
		return dm;
	}
	
	private Set<String> findLanguageCodes(SimpleDataModel sdm){
		Set<String> languageCodes = new HashSet<String>();
		for (Attribute a : sdm.getAttributes()) {
			a.getNames().forEach(l->languageCodes.add(l.getLanguage().getCode()));
			if (a.getAttributeList() != null) {
				a.getAttributeList().forEach(item->item.getNames().forEach(l->languageCodes.add(l.getLanguage().getCode())));
			}
			
			if (a.getTree() != null) {
				DataModel.processAttributeTree(a, 
						node->node.getNames().forEach(l->languageCodes.add(l.getLanguage().getCode())));
			}
		}
		DataModel.processCategories(sdm, c->c.getNames().forEach(l->languageCodes.add(l.getLanguage().getCode())));
		return languageCodes;
		
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
	public static String checkLanguage(Set<String> xmlLanguages, final ConservationArea targetCa){
		//here we check to ensure default ca lang
		for (String lt : xmlLanguages){
			if (lt.equals(targetCa.getDefaultLanguage().getCode())){
				return lt;
			}
		}
		
		final String[] values = new String[xmlLanguages.size()];
		int i = 0;
		for (String v : xmlLanguages) {
			values[i++] = v;
		}
		final String[] selected = new String[1];
		selected[0] = null;
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				LanguageSelectionDialog sd = new LanguageSelectionDialog(Display.getDefault().getActiveShell(), targetCa, values);
				if (sd.open() != IDialogConstants.OK_ID){
					selected[0] = null;
				}else{
					selected[0] = (String)((StructuredSelection)sd.getSelection()).getFirstElement();
				}
				
			}});
		
		
		return selected[0];
	}
	
	
	
	/*
	 * Initializes the language lookup list from the conservation area.
	 */
	private void getLanguages(){
		langLookup = new HashMap<String, Language>();
		for (Language lang : this.targetCa.getLanguages()) {
			langLookup.put(lang.getCode(), lang);
		}
	}

	
	/*
	 * Looks up aggregation from list
	 */
	private Aggregation lookUpAggregation(String name) throws ParseException{
		for (Aggregation agg : aggs){
			if (agg.getName().equals(name)){
				return agg;
			}
		}
		throw new ParseException(Messages.DataModelXmlToSmartConverter_Error_AggregationNotFound + name, 0);
	}
	
	
	/*
	 * updates the names associated with a data model object
	 */
	private void updateNamesAndIcons(DmObject dmobject){
		updateNamedKeyItem(dmobject);
		if (dmobject.getIcon() != null && dmobject.getIcon().getUuid() == null) {
			updateNamedKeyItem(dmobject.getIcon());
		}
		if (dmobject.getIcon() != null) dmobject.getIcon().setConservationArea(targetCa);
	}
	
	private void updateNamedKeyItem(NamedKeyItem nameditem){
		List<Label> newLabels = new ArrayList<Label>();
		String labelValue = null;
		for (Label label : nameditem.getNames()) {
			Language lang = langLookup.get(label.getLanguage().getCode());
			labelValue = label.getValue();
			if (lang != null) {
				Label l = new Label();
				l.setElement(nameditem);
				l.setLanguage(lang);
				l.setValue(label.getValue());
				newLabels.add(l);
			}
			
			if (useAsDefault != null && useAsDefault.equals(label.getLanguage().getCode())){
				Label l = new Label();
				l.setElement(nameditem);
				l.setLanguage(targetCa.getDefaultLanguage());
				l.setValue(label.getValue());
				
			}
		}
		nameditem.getNames().clear();
		for (Label l : newLabels) {
			nameditem.updateName(l.getLanguage(), l.getValue());
		}
		
		if (nameditem.findNameNull(targetCa.getDefaultLanguage()) == null){
			if (labelValue != null){
				//no default name provided; lets use any of the names
				nameditem.updateName(targetCa.getDefaultLanguage(), labelValue);
			}else{
				nameditem.updateName(targetCa.getDefaultLanguage(), nameditem.getKeyId());
			}			
		}
	}
}
