/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.data.inout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tools for importing asset data from CSV File
 * @author Emily
 *
 */
public class AssetCsvImporter {

	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	
	private Path file;
	private char delimiter;
	
	private Integer idField;
	private Object typeField;
	private boolean skipFirst;
	private HashMap<AssetAttribute, Integer> attributeMappings;
	private DateTimeFormatter dateTimeFormat;
	
	private List<AssetType> assetTypes = null;
	private List<String> warnings = null;
	private Set<String> assetIds = null;
	
	@Inject
	IEventBroker broker;
	
	
	public AssetCsvImporter(Path file, char delimiter, boolean skipFirst, Integer idField, Object typeField, HashMap<AssetAttribute, Integer> attributeMappings, String dateTimeFormat) {
		this.file = file;
		this.delimiter = delimiter;
		
		this.idField = idField;
		this.typeField = typeField;
		this.attributeMappings = attributeMappings;
		this.skipFirst = skipFirst;
		this.dateTimeFormat = DateTimeFormatter.ofPattern(dateTimeFormat);
	}
	
	@SuppressWarnings("unchecked")
	public boolean processFile() throws Exception {
		warnings = new ArrayList<>();
		assetIds = new HashSet<>();
		
		try(Session session = HibernateManager.openSession()){
			if (typeField instanceof Integer) {
				assetTypes = QueryFactory.buildQuery(session, AssetType.class, "conservationArea", ca).list();
				assetTypes.forEach(a->{
					a.getNames().size();
					a.getAssetAttributes().forEach(aa->{
						aa.getAttribute().getKeyId();
						aa.getAttribute().getNames().size();
						aa.getAttribute().getAttributeList().forEach(l->{
							l.getKeyId();
							l.getNames().size();
						});
					});
				});
			}else if (typeField instanceof AssetType) {
				AssetType t = session.get(AssetType.class, ((AssetType) typeField).getUuid());
				t.getAssetAttributes().forEach(aa->{
					aa.getAttribute().getKeyId();
					aa.getAttribute().getNames().size();
					aa.getAttribute().getAttributeList().forEach(l->{
						l.getKeyId();
						l.getNames().size();
					});
				});
				typeField = t;	
			}
			assetIds.addAll(session.createQuery("SELECT LOWER(id) FROM Asset WHERE conservationArea = :ca").setParameter("ca", ca).list());
		}
		
		//read and parse asset data
		List<Asset> newAssets = new ArrayList<>();
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(file), delimiter)) {
			int row = 0;
			if (skipFirst) {
				reader.readNext();
				row ++;
			}
			
			String[] data = null;
			while((data = reader.readNext()) != null) {
				row ++;
				Asset a = processRow(row, data);
				if (a != null) newAssets.add(a);
			}
		}
		
		//process warnings from user
		if (!warnings.isEmpty()) {
			WarningDialog warn = new WarningDialog(Display.getDefault().getActiveShell(), "Importing Assets", "The following warnings were generated while processing asset data.  Do you want to continue?", warnings, 
					new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
			if (warn.open() != 0) {
				return false;
			}
		}
		if (newAssets.isEmpty()) {
			MessageDialog.openWarning(Display.getDefault().getActiveShell(),"Importing Assets", "Nothing to import - no assets found.");
			return true;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (Asset a : newAssets) {
					session.save(a);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to save imported assets to database: " + ex.getMessage(), ex);
				return false;
			}
		}
		
		MessageDialog.openInformation(Display.getDefault().getActiveShell(),  "Importing Assets", MessageFormat.format("{0} assets successfully imported", newAssets.size()));
		broker.post(AssetEvents.ASSET_NEW, newAssets);
		
		return true;
	}
	
	
	private Asset processRow(int row, String[] data) {
		AssetType type = null;
		if (typeField instanceof AssetType) {
			type = (AssetType)typeField;
		}else {
			String value = data[(Integer)typeField].trim().toLowerCase();
			if (value.isEmpty()) {
				warnings.add(MessageFormat.format("ROW NOT IMPORTED - Could not determine asset type at row {0}. Asset Type field is blank", row));
				return null;
			}
			for (AssetType t : assetTypes) {
				if (t.getKeyId().equalsIgnoreCase(value)) {
					type = t;
					break;
				}
				for (Label l : t.getNames()) {
					if (l.getValue().equalsIgnoreCase(value)) {
						type = t;
						break;
					}
				}
			}
			if (type == null) {
				warnings.add(MessageFormat.format("ROW NOT IMPORTED - Could not match the value ''{0}'' to an asset type at row {1}", value, row));
				return null;
			}
		}
		
		String id = data[idField].trim();
		if (id.isEmpty()) {
			warnings.add(MessageFormat.format("ROW NOT IMPORTED - Asset id cannot be empty at row {0}.", row));
			return  null;
		}
		if (assetIds.contains(id.toLowerCase())){
			warnings.add(MessageFormat.format("ROW NOT IMPORTED - Asset id already exists at row {0}. Cannot duplicate asset ids.", row));
			return null;
		}
		
		Asset newAsset = new Asset();
		newAsset.setAssetType(type);
		newAsset.setConservationArea(ca);
		newAsset.setId(id);
		newAsset.setIsRetired(false);
		newAsset.setAttributeValues(new ArrayList<AssetAttributeValue>());
		
		//parse attribute values
		for (AssetTypeAttribute assetAttribute : type.getAssetAttributes() ) {
			Integer columnIndex = attributeMappings.get(assetAttribute.getAttribute());
			if (columnIndex == null) continue; //this attribute is not mapped
			
			String value = data[columnIndex].trim();
			if (value.isEmpty()) continue; //not value 
			
			
			//we need to convert value to the correct type based on the attribute type
			AssetAttributeValue attributeValue = new AssetAttributeValue();
			attributeValue.setAsset(newAsset);
			attributeValue.setAttribute(assetAttribute.getAttribute());
			attributeValue = convertAttributeValue(attributeValue, value, row);
			if (attributeValue != null) {
				newAsset.getAttributeValues().add(attributeValue);
			}
		}
		
		assetIds.add(id.toLowerCase());
		return newAsset;
	}
	
	private AssetAttributeValue convertAttributeValue(AssetAttributeValue attributeValue, String data, int rowIndex) {
		data = data.trim();
		switch (attributeValue.getAttribute().getType()) {
		case BOOLEAN:
			Boolean x = null;
			try {
				x = Boolean.valueOf(data);
				
			}catch (Exception ex) {
				x = null;
			}
			if (x == null) {
				warnings.add(MessageFormat.format("Could not parse boolean value from value ''{0}'' for attribute {1} at row {2}.  Asset attribute value will not be imported.", data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(x ? 1.0 : 0);
			break;
			
		case DATE:
			Date d = null;
			try {
				d = Date.from(LocalDate.parse(data, dateTimeFormat).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
			}catch (Exception e) {
				d = null;
			}
			if (d == null) {
				warnings.add(MessageFormat.format("Could not parse date from value ''{0}'' for attribute {1} at row {2}.  Asset attribute value will not be imported.", data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setDateValue(d);
			break;
		case LIST:
			AssetAttributeListItem av = null;
			for (AssetAttributeListItem i : attributeValue.getAttribute().getAttributeList()) {
				if (i.getKeyId().equalsIgnoreCase(data)) {
					av = i;
					break;
				}
				for (Label l : i.getNames()) {
					if (l.getValue().equalsIgnoreCase(data)) {
						av = i;
						break;
					}
				}
			}
			if (av == null) {
				warnings.add(MessageFormat.format("Could not parse list value from value ''{0}'' for attribute {1} at row {2}.  Asset attribute value will not be imported.", data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setAttributeListItem(av);
			break;
			
		case NUMERIC:
			Double v = null;
			try {
				v = Double.parseDouble(data);
			}catch (Exception ex) {
				v = null;
			}
			if (v == null) {
				warnings.add(MessageFormat.format("Could not parse boolean value from value ''{0}'' for attribute {1} at row {2}.  Asset attribute value will not be imported.", data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(v);
			break;
		case POSITION:
			//TODO: add support for position attributes
			warnings.add(MessageFormat.format("Positiong attributes are not supported in csv importer at row {0}.  Asset attribute value will not be imported.", rowIndex));
			return null;
		case TEXT:
			attributeValue.setStringValue(data);
			break;
		}
		return attributeValue;
	}
}
