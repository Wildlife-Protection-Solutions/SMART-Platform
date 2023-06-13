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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.wcs.smart.asset.internal.Messages;
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

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tools for importing field sensor data from CSV File
 * 
 * @author Emily
 *
 */
public class AssetCsvImporter {

	private ConservationArea ca = null;
	
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
	
	
	public AssetCsvImporter(ConservationArea ca, Path file, char delimiter, boolean skipFirst, 
			Integer idField, Object typeField, HashMap<AssetAttribute, Integer> attributeMappings, 
			DateTimeFormatter formatter) {
		this.ca = ca;
		this.file = file;
		this.delimiter = delimiter;
		
		this.idField = idField;
		this.typeField = typeField;
		this.attributeMappings = attributeMappings;
		this.skipFirst = skipFirst;
		this.dateTimeFormat = formatter;
	}
	
	public boolean processFile() throws Exception {
		warnings = new ArrayList<>();
		assetIds = new HashSet<>();
		
		try(Session session = HibernateManager.openSession()){
			if (typeField instanceof Integer) {
				assetTypes = QueryFactory.buildQuery(session, AssetType.class, "conservationArea", ca).list(); //$NON-NLS-1$
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
			assetIds.addAll(session.createQuery("SELECT LOWER(id) FROM Asset WHERE conservationArea = :ca", String.class).setParameter("ca", ca).list()); //$NON-NLS-1$ //$NON-NLS-2$
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
			int[] r = new int[1];
			Display.getDefault().syncExec(()->{
				WarningDialog warn = new WarningDialog(Display.getDefault().getActiveShell(), Messages.AssetCsvImporter_ImportingTitle, Messages.AssetCsvImporter_WarningsMsg, warnings, 
					new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
				r[0] = warn.open();
			});
			
			if (r[0]!= 0) {
				return false;
			}
		}

		if (newAssets.isEmpty()) {
			Display.getDefault().syncExec(()->{
				MessageDialog.openWarning(Display.getDefault().getActiveShell(),Messages.AssetCsvImporter_ImportingTitle, Messages.AssetCsvImporter_NoDataMsg);
			});
			return true;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (Asset a : newAssets) {
					session.persist(a);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetCsvImporter_7 + ex.getMessage(), ex);
				return false;
			}
		}

		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(),  Messages.AssetCsvImporter_ImportingTitle, MessageFormat.format(Messages.AssetCsvImporter_SuccessMsg, newAssets.size()));
		});
		
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
				warnings.add(MessageFormat.format(Messages.AssetCsvImporter_TypeRequired, row));
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
				warnings.add(MessageFormat.format(Messages.AssetCsvImporter_TypeNotFound, value, row));
				return null;
			}
		}
		
		String id = data[idField].trim();
		if (id.isEmpty()) {
			warnings.add(MessageFormat.format(Messages.AssetCsvImporter_IdRequired, row));
			return  null;
		}
		if (assetIds.contains(id.toLowerCase())){
			warnings.add(MessageFormat.format(Messages.AssetCsvImporter_IdDuplicated, row));
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
				warnings.add(MessageFormat.format(Messages.AssetCsvImporter_BooleanParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(x ? 1.0 : 0);
			break;
			
		case DATE:
			LocalDate d = null;
			try {
				d = LocalDate.parse(data, dateTimeFormat);
			}catch (Exception e) {
				d = null;
			}
			if (d == null) {
				warnings.add(MessageFormat.format(Messages.AssetCsvImporter_dateParseError, data, attributeValue.getAttribute().getName(), rowIndex));
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
				warnings.add(MessageFormat.format(Messages.AssetCsvImporter_ListParseError, data, attributeValue.getAttribute().getName(), rowIndex));
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
				warnings.add(MessageFormat.format(Messages.AssetCsvImporter_NumericParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(v);
			break;
		case POSITION:
			//TODO: add support for position attributes
			warnings.add(MessageFormat.format(Messages.AssetCsvImporter_PositionNotSupported, rowIndex));
			return null;
		case TEXT:
			attributeValue.setStringValue(data);
			break;
		}
		return attributeValue;
	}
}
