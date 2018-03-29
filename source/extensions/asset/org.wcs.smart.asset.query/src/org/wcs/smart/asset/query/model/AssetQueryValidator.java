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
package org.wcs.smart.asset.query.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.query.IDataModelManager;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.filter.QueryDefinitionValidator;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools for asset query validation.
 * 
 * @author Emily
 *
 */
public class AssetQueryValidator extends QueryDefinitionValidator {
	
	private HashMap<String, UuidItemType> uuidLookup;
	private ConservationArea importCa;

	/**
	 * @param langCode the language value of the query 
	 * @param uuidLookup a uuid lookup map that looks up uuid values
	 * @param session database session
	 * @param queryDmManager
	 * @param ConservationArea 
	 * 
	 */
	public AssetQueryValidator(HashMap<String, UuidItemType> uuidLookup, 
			Session session, IDataModelManager queryDmManager, ConservationArea ca ){
		super(session, queryDmManager, ca);
		
		this.importCa = ca;
		this.uuidLookup = uuidLookup;
	}

	
	/**
	 * Validates a filter item against the database.
	 * 
	 * @param filter the filter to validate
	 * 
	 * @throws Exception if filter cannot be validated
	 */
	@Override
	public List<String> validate(IFilter filter) throws Exception{
		List<String> warnings = super.validate(filter);
		
		FilterValidatorVisitor vv = new FilterValidatorVisitor();
		filter.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		warnings.addAll(vv.warnings);
		return warnings;
	}
		
	
	/**
	 * Validates a group by item.
	 * @param item
	 * @return
	 * @throws Exception
	 */
	public List<String> validate(IGroupBy item) throws Exception{
		List<String> warnings = super.validate(item);
		
		GroupByValidatorVisitor vv = new GroupByValidatorVisitor();
		item.visit(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		warnings.addAll(vv.warnings);
		return warnings;
	}
	
	
	/**
	 * Validates value items
	 */
	@Override
	public List<String> validate(IValueItem item) throws Exception{
		List<String> warnings = super.validate(item);
		return warnings;
	}
	
	class FilterValidatorVisitor implements IFilterVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IFilter filter) {
			if (ex != null) return;
			try{
				if (filter instanceof AssetFilter){
					AssetFilter assetFilter = (AssetFilter) filter;			
					UUID uuid = assetFilter.getValue();
					if (assetFilter.getAssetOption() == AssetFilterOption.ASSET) {
						Asset a = lookupOption(uuid, Asset.class);
						assetFilter.setValue(a.getUuid());
					}else if (assetFilter.getAssetOption() == AssetFilterOption.ASSETTYPE) {
						AssetType a = lookupAssetTypeOption(uuid);
						assetFilter.setValue(a.getUuid());
					}else if (assetFilter.getAssetOption() == AssetFilterOption.STATION) {
						AssetStation a = lookupOption(uuid, AssetStation.class);
						assetFilter.setValue(a.getUuid());
					}else if (assetFilter.getAssetOption() == AssetFilterOption.STATIONLOCATION) {
						AssetStationLocation a = lookupOption(uuid, AssetStationLocation.class);
						assetFilter.setValue(a.getUuid());
					}
				}
			}catch(Exception ex){
				this.ex = ex;
			}
		}
	}
	private AssetType lookupAssetTypeOption(UUID uuid) throws Exception{
		AssetType a = session.get(AssetType.class, uuid);
		if (a.getConservationArea().equals(importCa)) {
			//this is fine
		}else {
			//not this ca; lets look for something with the same id in this ca
			String id = uuidLookup.get(UuidUtils.uuidToString(uuid)).getId();
			a = (AssetType)QueryFactory.buildQuery(session, AssetType.class, new Object[] {"keyId", id}, new Object[]{"conservationArea", importCa}).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
			if (a == null) {
				throw new Exception(
						MessageFormat.format(Messages.AssetQueryValidator_AssetTypeNotFound, id)
				);
			}
		}
		return a;
	}
	
	private <T> T lookupOption(UUID uuid, Class<T> clazz) throws Exception {
		T a = session.get(clazz, uuid);
		ConservationArea ca = null;
		if (a instanceof Asset) {
			ca = ((Asset)a).getConservationArea();
		}else if (a instanceof AssetStation) {
			ca = ((AssetStation)a).getConservationArea();
		}else if (a instanceof AssetStationLocation) {
			ca = ((AssetStationLocation)a).getStation().getConservationArea();
		}else if (a instanceof AssetType) {
			ca = ((AssetType)a).getConservationArea();
		}
		if (ca == null) throw new IllegalStateException(MessageFormat.format("Conservation area field not found for class {0}", clazz.getName())); //$NON-NLS-1$
		if (ca.equals(importCa)) {
			//this is fine
			return a;
		}else {
			//not this ca; lets look for something with the same id in this ca
			String id = uuidLookup.get(UuidUtils.uuidToString(uuid)).getId();
			a = (T)QueryFactory.buildQuery(session, clazz, new Object[] {"id", id}, new Object[]{"conservationArea", importCa}).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
			if (a != null) return a;
			
			throw new Exception(
					MessageFormat.format(Messages.AssetQueryValidator_StationNotFound, id)
			);
		}
	}
	
	
	class GroupByValidatorVisitor implements IGroupByVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IGroupBy filter) {
			if (ex != null)
				return;
			try {
				if (filter instanceof AssetGroupBy) {
					AssetGroupBy groupBy = (AssetGroupBy) filter;
					if (groupBy.getItems() == null){
						//nothing to validate
						return;
					}
					AssetFilterOption op = groupBy.getOption();
					if (op == AssetFilterOption.ASSET) {
						for (int i = 0; i < groupBy.getItems().length; i ++){
							UUID uuid = UuidUtils.stringToUuid(groupBy.getItems()[i]);
							Asset a = lookupOption(uuid, Asset.class);
							groupBy.getItems()[i] = UuidUtils.uuidToString(a.getUuid());
						}
					}else if (op == AssetFilterOption.ASSETTYPE) {
						for (int i = 0; i < groupBy.getItems().length; i ++){
							UUID uuid = UuidUtils.stringToUuid(groupBy.getItems()[i]);
							AssetType a = lookupAssetTypeOption(uuid);
							groupBy.getItems()[i] = UuidUtils.uuidToString(a.getUuid());
						}
					}else if (op == AssetFilterOption.STATION) {
						for (int i = 0; i < groupBy.getItems().length; i ++){
							UUID uuid = UuidUtils.stringToUuid(groupBy.getItems()[i]);
							AssetStation a = lookupOption(uuid, AssetStation.class);
							groupBy.getItems()[i] = UuidUtils.uuidToString(a.getUuid());
						}
					}else if (op == AssetFilterOption.STATIONLOCATION) {
						for (int i = 0; i < groupBy.getItems().length; i ++){
							UUID uuid = UuidUtils.stringToUuid(groupBy.getItems()[i]);
							AssetStationLocation a = lookupOption(uuid, AssetStationLocation.class);
							groupBy.getItems()[i] = UuidUtils.uuidToString(a.getUuid());
						}
					}
				}
			} catch (Exception ex) {
				this.ex = ex;
			}
		}
	}
}
