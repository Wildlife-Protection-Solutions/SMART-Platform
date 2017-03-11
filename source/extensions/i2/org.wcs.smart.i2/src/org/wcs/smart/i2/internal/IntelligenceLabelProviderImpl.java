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
package org.wcs.smart.i2.internal;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.entity.EntityDatasetMetadata;
import org.wcs.smart.i2.birt.entity.EntityDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDatasetMetadata;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDatasetMetadata;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDatasetMetadata;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDatasetMetadata;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDatasetMetadata;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.RecordAttributeDataset;
import org.wcs.smart.i2.birt.record.RecordAttributeDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.RecordDataset;
import org.wcs.smart.i2.birt.record.RecordDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.RecordMetadata;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDataset;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDataset;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationDatasetResultSetMetadata;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.export.CsvQueryExporter;
import org.wcs.smart.i2.query.export.ShpQueryExporter;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Desktop label provider for intelligence module.
 * 
 * @author Emily
 *
 */
public class IntelligenceLabelProviderImpl implements
		IIntelligenceLabelProvider {

	public String getDataSourceProductName(String dataSetType, Locale l){
		if (dataSetType.equals(RecordDataset.DATASET_TYPE)){
			return "Record Details";
		}else if (dataSetType.equals(RecordAttributeDataset.DATASET_TYPE)){
			return "Record Attributes";
		}else if (dataSetType.equals(RecordEntityDataset.DATASET_TYPE)){
			return "Record Entities";
		}else if (dataSetType.equals(RecordLocationDataset.DATASET_TYPE)){
			return "Record Locations";
		}else if (dataSetType.equals(RecordAttachmentDataset.DATASET_TYPE)){
			return "Record Attachments";
		}
		return "";
	}
	
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof AttributeType){
			
			AttributeType type = (AttributeType) item;
			switch(type){
			case BOOLEAN:
				return "BOOLEAN";
			case DATE:
				return "DATE";
			case LIST:
				return "LIST";
			case NUMERIC:
				return "NUMERIC";
			case POSITION:
				return "POSITION";
			case TEXT:
				return "TEXT";
			}
		}
		if (item == QUERY_COLUMN_CATEGORY_LABEL) return "Category {0}";
		if (item == OBS_COUNT_LABEL ) return "{0} Observations";
		if (item == IntelWorkingSetCategory.ENTITY) return "Entities";
		if (item == IntelWorkingSetCategory.RECORD) return "Records";
		if (item == IntelWorkingSetCategory.QUERIES) return "Queries";
		
		if (item == FixedQueryColumn.Column.LOC_COMMENT) return "Comment";
		if (item == FixedQueryColumn.Column.LOC_DATE) return "Date";
		if (item == FixedQueryColumn.Column.LOC_TIME) return "Time";
		if (item == FixedQueryColumn.Column.LOC_GEOMTRY) return "Geometry";
		if (item == FixedQueryColumn.Column.LOC_ID) return "ID";
		if (item == FixedQueryColumn.Column.RECORD_STATUS) return "Record Status";
		if (item == FixedQueryColumn.Column.RECORD_TITLE) return "Record Title";
						
		if (item == IntelRecord.Status.NEW) return "Unprocessed";
		if (item == IntelRecord.Status.PROCESSING) return "In Progress";
		if (item == IntelRecord.Status.COMPLETE) return "Complete";
		
		if (item instanceof Operator){
			switch((Operator)item){
				case AND: return "And";
				case BETWEEN: return "Between";
				case BRACKETS: return "( )";
				case BRACKET_CLOSE: return ")";
				case BRACKET_OPEN: return "(";
				case EQUALS: return "=";
				case GREATERTHAN: return ">";
				case GREATERTHANEQUALS: return ">=";
				case LESSTHAN: return "<";
				case LESSTHANEQUALS: return "<=";
				case NOT: return "Not";
				case NOTEQUALS: return "!=";
				case NOT_BETWEEN: return "Not Between";
				case OR: return "Or";
				case STR_CONTAINS: return "Contains";
				case STR_EQUALS: return "Equals";
				case STR_NOTCONTAINS: return "Not Contains";
			}
		}
		
		if (item instanceof CsvQueryExporter) return "Comma Separated Values";
		if (item instanceof ShpQueryExporter) return "Shapefile";
		if (item == IntelQueryColumnProvider.ANY_ITEM) return "<Any>";
		if (item == Boolean.TRUE) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
		if (item == Boolean.FALSE) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
		
		
		
		if (item == RecordLocationDatasetResultSetMetadata.Column.COMMENT) return "Comment";
		if (item == RecordLocationDatasetResultSetMetadata.Column.DATE) return "Date";
		if (item == RecordLocationDatasetResultSetMetadata.Column.GEOM) return "Geometry";
		if (item == RecordLocationDatasetResultSetMetadata.Column.ID) return "ID";
		if (item == RecordLocationDatasetResultSetMetadata.Column.OBSERVATION) return "Observation";
		if (item == RecordLocationDatasetResultSetMetadata.Column.RECORD_UUID) return "Record UUID";
		
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_ID) return "Entity ID";
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_IMAGE) return "Primary Image";
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == RecordEntityDatasetResultSetMetadata.Column.UUID) return "Record UUID";
		
		if (item == EntityDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == EntityDatasetResultSetMetadata.Column.ID) return "ID";
		if (item == EntityDatasetResultSetMetadata.Column.TYPE_KEY) return "Entity Type Key";
		if (item == EntityDatasetResultSetMetadata.Column.TYPE) return "Entity Type";
		if (item == EntityDatasetResultSetMetadata.Column.DATE_CREATED) return "Date Created";
		if (item == EntityDatasetResultSetMetadata.Column.DATE_MODIFIED) return "Date Modified";
		if (item == EntityDatasetResultSetMetadata.Column.CREATED_BY) return "Created By";
		if (item == EntityDatasetResultSetMetadata.Column.MODIFIED_BY) return "Last Modified By";
		if (item == EntityDatasetResultSetMetadata.Column.PRIMARY_IMAGE) return "Primay Image";
		
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE_KEY) return "Attribute Key";
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE_NAME) return "Attribute Name";
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ENTITY_UUID)  return "Geometry";
			
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.FILE_NAME) return  "Name";
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.DATE_CREATED) return  "Date Created";
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.PATH) return  "Path";
				
		if (item == EntityLocationDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == EntityLocationDatasetResultSetMetadata.Column.ID) return "ID";
		if (item == EntityLocationDatasetResultSetMetadata.Column.GEOM) return "Geometry";
		if (item == EntityLocationDatasetResultSetMetadata.Column.DATE) return "Date";
		if (item == EntityLocationDatasetResultSetMetadata.Column.COMMENT) return "Comment";
		if (item == EntityLocationDatasetResultSetMetadata.Column.OBSERVATION) return "Observation";
				

		if (item == EntityRecordDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == EntityRecordDatasetResultSetMetadata.Column.TITLE) return "Title";
		if (item == EntityRecordDatasetResultSetMetadata.Column.STATUS) return "Status";
		if (item == EntityRecordDatasetResultSetMetadata.Column.DATE_RECIEVED) return "Date Received";
		if (item == EntityRecordDatasetResultSetMetadata.Column.DATE_MODIFIED) return "Date Modified";
		if (item == EntityRecordDatasetResultSetMetadata.Column.DESCRIPTION) return "Description";

		if (item == EntityRelationDatasetResultSetMetadata.Column.ENTITY_UUID) return "Entity UUID";
		if (item == EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_UUID) return "Source Relation UUID";
		if (item == EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_ID) return "Source Relation";
		if (item == EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_UUID) return "Target Relation UUID";
		if (item == EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_ID) return "Target Relation";
		if (item == EntityRelationDatasetResultSetMetadata.Column.GROUP_NAME) return "Group";
		if (item == EntityRelationDatasetResultSetMetadata.Column.GROUP_KEY) return "Group Key";
		if (item == EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE) return  "Relationship Type";
		if (item == EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE_KEY) return "Relationship Type Key";
		
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.RECORD_UUID) return "Record UUID";
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.FILE_NAME) return "Name";
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.DATE_CREATED) return "Date Created";
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.PATH) return "Path";
		
		if (item == RecordAttributeDatasetResultSetMetadata.Column.RECORD_UUID) return "Record UUID";
		if (item == RecordAttributeDatasetResultSetMetadata.Column.ATTRIBUTE) return "Attribute Name";
		if (item == RecordAttributeDatasetResultSetMetadata.Column.TEXT) return "Attribute Value";
		if (item == RecordAttributeDatasetResultSetMetadata.Column.STRING_VALUE) return "String Value";
		if (item == RecordAttributeDatasetResultSetMetadata.Column.NUMBER_VALUE) return "Number Value";
		if (item == RecordAttributeDatasetResultSetMetadata.Column.DATE_VALUE) return "Date Value";
		
		if (item == RecordDatasetResultSetMetadata.Column.UUID) return "UUID";
		if (item == RecordDatasetResultSetMetadata.Column.TITLE) return "Title";
		if (item == RecordDatasetResultSetMetadata.Column.DESCRIPTION) return "Description";
		if (item == RecordDatasetResultSetMetadata.Column.SCRATCHPAD) return "Scatchpad";
		if (item == RecordDatasetResultSetMetadata.Column.CREATED_BY) return "Created By";
		if (item == RecordDatasetResultSetMetadata.Column.LAST_MODIFIED_BY) return "Last Modified By";
		if (item == RecordDatasetResultSetMetadata.Column.CREATED) return "Date Created";
		if (item == RecordDatasetResultSetMetadata.Column.LAST_MODIFIED) return "Date Last Modified";
		if (item == RecordDatasetResultSetMetadata.Column.STATUS) return "Status";
		if (item == RecordDatasetResultSetMetadata.Column.STATUS_KEY) return "Status Key";
		if (item == RecordDatasetResultSetMetadata.Column.SOURCE) return "Record Source";
		if (item == RecordDatasetResultSetMetadata.Column.SOURCE_ICON) return "Record Source Image";
		
		
		if (item.equals(EntityDatasetMetadata.class)) return "Intelligence Entity Types";
		if (item.equals(EntityLocationAttributeDatasetMetadata.class)) return "Intelligence Entity Location Attributes";
		if (item.equals(EntityAttachmentDatasetMetadata.class)) return "Intelligence Entity Types";
		if (item.equals(EntityLocationDatasetMetadata.class)) return "Intelligence Entity Locations";
		if (item.equals(EntityRecordDatasetMetadata.class)) return "Intelligence Entity Records";
		if (item.equals(EntityRelationDatasetMetadata.class)) return "Intelligence Entity Records";
		if (item.equals(RecordMetadata.class)) return "Intelligence Records";
	
		return "";
	}
	
	public Image getImage(Object item){

		if (item == IntelWorkingSetCategory.RECORD){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
		}else if (item == IntelWorkingSetCategory.ENTITY){
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
		}
		return null;
	}

}
