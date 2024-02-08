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
package org.wcs.smart.i2.udig.record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.hibernate.Session;
import org.opengis.feature.type.Name;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class IntelRecordDataSource extends ContentDataStore{

	private UUID recordUuid;
	private IntelRecord record;
	
	private List<Attribute> attributeMap;
	
	public IntelRecordDataSource(UUID recordUuid){
		this.recordUuid = recordUuid;
	}

	public IntelRecordDataSource(IntelRecord record){
		this(record.getUuid());
		this.record = record;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		if (record != null) {
			return new IntelRecordFeatureSource(entry, record);
		}else {
			return new IntelRecordFeatureSource(entry,recordUuid);
		}
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<Name>();
		for (LocationLayerType layertype : LocationLayerType.values()){
			names.add(generateTypeName(layertype, recordUuid));
		}
		
		this.attributeMap = new ArrayList<>();
		try (Session session = HibernateManager.openSession()){		
			//find all geometry based data model
			List<Attribute> attributes = DataModelManager.INSTANCE.getGeometryAttributes(session);
			for (Attribute attribute : attributes) {
				Name name = generateTypeName(attribute);
				names.add(name); 
				attributeMap.add(attribute);
			}	
		}
		
		return names;
	}
		
	public Collection<Attribute> getAttributes(){
		return this.attributeMap;
	}
	
	public static Name generateTypeName(LocationLayerType type, UUID recordUuid){
		return new NameImpl("id_record_location", UuidUtils.uuidToString(recordUuid) + ";" + type.name()); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static Name generateTypeName(Attribute attribute) {
		String type = attribute.getType().name() + ";" + attribute.getKeyId(); //$NON-NLS-1$
		return new NameImpl("dm_attribute", type); //$NON-NLS-1$
	}
	
	public static String getAttributeKeyFromType(Name type) {
		return type.getLocalPart().split(";")[1]; //$NON-NLS-1$
	}
	
	public static Attribute.AttributeType getAttributeTypeFromType(Name type) {
		return Attribute.AttributeType.valueOf( type.getLocalPart().split(";")[0]); //$NON-NLS-1$
	}
	
	public static boolean isGeometryAttribute(Name name) {
		if (name.getNamespaceURI().equalsIgnoreCase("dm_attribute")) return true; //$NON-NLS-1$
		return false;
		
	}
}
