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
			return Messages.IntelligenceLabelProviderImpl_RecordDatasetName;
		}else if (dataSetType.equals(RecordAttributeDataset.DATASET_TYPE)){
			return Messages.IntelligenceLabelProviderImpl_RecordAttributeDatasetName;
		}else if (dataSetType.equals(RecordEntityDataset.DATASET_TYPE)){
			return Messages.IntelligenceLabelProviderImpl_RecordEntitiesDatasetName;
		}else if (dataSetType.equals(RecordLocationDataset.DATASET_TYPE)){
			return Messages.IntelligenceLabelProviderImpl_RecordLocationsDatasetName;
		}else if (dataSetType.equals(RecordAttachmentDataset.DATASET_TYPE)){
			return Messages.IntelligenceLabelProviderImpl_RecordAttachmentsDatasetName;
		}
		return ""; //$NON-NLS-1$
	}
	
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof AttributeType){
			
			AttributeType type = (AttributeType) item;
			switch(type){
			case BOOLEAN:
				return Messages.IntelligenceLabelProviderImpl_BooleanAttributeName;
			case DATE:
				return Messages.IntelligenceLabelProviderImpl_DateAttributeName;
			case LIST:
				return Messages.IntelligenceLabelProviderImpl_ListAttributeName;
			case NUMERIC:
				return Messages.IntelligenceLabelProviderImpl_NumberAttributeName;
			case POSITION:
				return Messages.IntelligenceLabelProviderImpl_PositionAttributeName;
			case TEXT:
				return Messages.IntelligenceLabelProviderImpl_TextAttributeName;
			}
		}
		if (item == QUERY_COLUMN_CATEGORY_LABEL) return Messages.IntelligenceLabelProviderImpl_QueryCategoryLabel;
		if (item == OBS_COUNT_LABEL ) return Messages.IntelligenceLabelProviderImpl_QueryObservationCntLabel;
		if (item == IntelWorkingSetCategory.ENTITY) return Messages.IntelligenceLabelProviderImpl_EntitiesWorkingSetLabel;
		if (item == IntelWorkingSetCategory.RECORD) return Messages.IntelligenceLabelProviderImpl_RecordsWorkingSetLabel;
		if (item == IntelWorkingSetCategory.QUERIES) return Messages.IntelligenceLabelProviderImpl_QueriewWorkingSetLabel;
		
		if (item == FixedQueryColumn.Column.LOC_COMMENT) return Messages.IntelligenceLabelProviderImpl_CommentColumnLabel;
		if (item == FixedQueryColumn.Column.LOC_DATE) return Messages.IntelligenceLabelProviderImpl_DateColumnLabel;
		if (item == FixedQueryColumn.Column.LOC_TIME) return Messages.IntelligenceLabelProviderImpl_TimeColumnLabel;
		if (item == FixedQueryColumn.Column.LOC_GEOMTRY) return Messages.IntelligenceLabelProviderImpl_GeomColumnLabel;
		if (item == FixedQueryColumn.Column.LOC_ID) return Messages.IntelligenceLabelProviderImpl_IDColumnLabel;
		if (item == FixedQueryColumn.Column.RECORD_STATUS) return Messages.IntelligenceLabelProviderImpl_StatusColumnLabel;
		if (item == FixedQueryColumn.Column.RECORD_TITLE) return Messages.IntelligenceLabelProviderImpl_TitleColumnLabel;
						
		if (item == IntelRecord.Status.NEW) return Messages.IntelligenceLabelProviderImpl_RecordUnprocessedLabel;
		if (item == IntelRecord.Status.PROCESSING) return Messages.IntelligenceLabelProviderImpl_RecordInProgressLabel;
		if (item == IntelRecord.Status.COMPLETE) return Messages.IntelligenceLabelProviderImpl_RecordCompleteLabel;
		
		if (item instanceof Operator){
			switch((Operator)item){
				case AND: return Messages.IntelligenceLabelProviderImpl_AndLabel;
				case BETWEEN: return Messages.IntelligenceLabelProviderImpl_BetweenLabel;
				case BRACKETS: return "( )"; //$NON-NLS-1$
				case BRACKET_CLOSE: return ")"; //$NON-NLS-1$
				case BRACKET_OPEN: return "("; //$NON-NLS-1$
				case EQUALS: return "="; //$NON-NLS-1$
				case GREATERTHAN: return ">"; //$NON-NLS-1$
				case GREATERTHANEQUALS: return ">="; //$NON-NLS-1$
				case LESSTHAN: return "<"; //$NON-NLS-1$
				case LESSTHANEQUALS: return "<="; //$NON-NLS-1$
				case NOT: return Messages.IntelligenceLabelProviderImpl_NotLabel;
				case NOTEQUALS: return "!="; //$NON-NLS-1$
				case NOT_BETWEEN: return Messages.IntelligenceLabelProviderImpl_NotBetweenLabel;
				case OR: return Messages.IntelligenceLabelProviderImpl_OrLabel;
				case STR_CONTAINS: return Messages.IntelligenceLabelProviderImpl_ContainsLabel;
				case STR_EQUALS: return Messages.IntelligenceLabelProviderImpl_StrEqualsLabel;
				case STR_NOTCONTAINS: return Messages.IntelligenceLabelProviderImpl_NotContainsLabel;
			}
		}
		
		if (item instanceof CsvQueryExporter) return Messages.IntelligenceLabelProviderImpl_CSVLabel;
		if (item instanceof ShpQueryExporter) return Messages.IntelligenceLabelProviderImpl_ShapefileLabel;
		if (item == IntelQueryColumnProvider.ANY_ITEM) return Messages.IntelligenceLabelProviderImpl_AnyLabel;
		if (item == Boolean.TRUE) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
		if (item == Boolean.FALSE) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
		
		
		
		if (item == RecordLocationDatasetResultSetMetadata.Column.COMMENT) return Messages.IntelligenceLabelProviderImpl_RecordLocationRsCommentColumn;
		if (item == RecordLocationDatasetResultSetMetadata.Column.DATE) return Messages.IntelligenceLabelProviderImpl_RecordLocationRsDateColumn;
		if (item == RecordLocationDatasetResultSetMetadata.Column.GEOM) return Messages.IntelligenceLabelProviderImpl_RecordLocationRsGeometryColumn;
		if (item == RecordLocationDatasetResultSetMetadata.Column.ID) return Messages.IntelligenceLabelProviderImpl_RecordLocationRsIdColumn;
		if (item == RecordLocationDatasetResultSetMetadata.Column.OBSERVATION) return Messages.IntelligenceLabelProviderImpl_RecordLocationRsObsColumn;
		if (item == RecordLocationDatasetResultSetMetadata.Column.RECORD_UUID) return Messages.IntelligenceLabelProviderImpl_RecordLocationRsRecordUuidColumn;
		
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_ID) return Messages.IntelligenceLabelProviderImpl_RecordEntitiesRsEntityIdColumn;
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_IMAGE) return Messages.IntelligenceLabelProviderImpl_RecordEntitiesRsEntityImageColumn;
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_RecordEntitiesRsEntityUUIDColumn;
		if (item == RecordEntityDatasetResultSetMetadata.Column.UUID) return Messages.IntelligenceLabelProviderImpl_RecordEntitiesRsRecordUuidColumn;
		
		if (item == EntityDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_EntityRsEntityUUIDColumn;
		if (item == EntityDatasetResultSetMetadata.Column.ID) return Messages.IntelligenceLabelProviderImpl_EntityRsEntityIDColumn;
		if (item == EntityDatasetResultSetMetadata.Column.TYPE_KEY) return Messages.IntelligenceLabelProviderImpl_EntityRsEntityTypeKeyColumn;
		if (item == EntityDatasetResultSetMetadata.Column.TYPE) return Messages.IntelligenceLabelProviderImpl_EntityRsEntityTypeColumn;
		if (item == EntityDatasetResultSetMetadata.Column.DATE_CREATED) return Messages.IntelligenceLabelProviderImpl_EntityRsDateCreatedColumn;
		if (item == EntityDatasetResultSetMetadata.Column.DATE_MODIFIED) return Messages.IntelligenceLabelProviderImpl_EntityRsDateModifiedColumn;
		if (item == EntityDatasetResultSetMetadata.Column.CREATED_BY) return Messages.IntelligenceLabelProviderImpl_EntityRsCreatedByColumn;
		if (item == EntityDatasetResultSetMetadata.Column.MODIFIED_BY) return Messages.IntelligenceLabelProviderImpl_EntityRsLastModifiedByColumn;
		if (item == EntityDatasetResultSetMetadata.Column.PRIMARY_IMAGE) return Messages.IntelligenceLabelProviderImpl_EntityRsPrimaryKeyColumn;
		
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsEntityUUIDColumn;
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE_KEY) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsAttributeKeyColumn;
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE_NAME) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsAttributeNameColumn;
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ENTITY_UUID)  return Messages.IntelligenceLabelProviderImpl_EntityLocationRsAttributeGeomtryColumn;
			
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_EntityAttachmentRsEntityUuidColumn;
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.FILE_NAME) return  Messages.IntelligenceLabelProviderImpl_EntityAttachmentRsNameColumn;
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.DATE_CREATED) return  Messages.IntelligenceLabelProviderImpl_EntityAttachmentRsEntityDateCreatedColumn;
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.PATH) return  Messages.IntelligenceLabelProviderImpl_EntityAttachmentRsPathColumn;
				
		if (item == EntityLocationDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsEntityUuidColumn;
		if (item == EntityLocationDatasetResultSetMetadata.Column.ID) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsEntityIdColumn;
		if (item == EntityLocationDatasetResultSetMetadata.Column.GEOM) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsGeometryColumn;
		if (item == EntityLocationDatasetResultSetMetadata.Column.DATE) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsDateColumn;
		if (item == EntityLocationDatasetResultSetMetadata.Column.COMMENT) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsCommentColumn;
		if (item == EntityLocationDatasetResultSetMetadata.Column.OBSERVATION) return Messages.IntelligenceLabelProviderImpl_EntityLocationRsObservationColumn;
				

		if (item == EntityRecordDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_EntityRecordRsEntityUuidColumn;
		if (item == EntityRecordDatasetResultSetMetadata.Column.TITLE) return Messages.IntelligenceLabelProviderImpl_EntityRecordRsTitleColumn;
		if (item == EntityRecordDatasetResultSetMetadata.Column.STATUS) return Messages.IntelligenceLabelProviderImpl_EntityRecordRsStatusColumn;
		if (item == EntityRecordDatasetResultSetMetadata.Column.DATE_RECIEVED) return Messages.IntelligenceLabelProviderImpl_EntityRecordRsDateReceivedColumn;
		if (item == EntityRecordDatasetResultSetMetadata.Column.DATE_MODIFIED) return Messages.IntelligenceLabelProviderImpl_EntityRecordRsDateModifiedColumn;
		if (item == EntityRecordDatasetResultSetMetadata.Column.DESCRIPTION) return Messages.IntelligenceLabelProviderImpl_EntityRecordRsDescriptionColumn;

		if (item == EntityRelationDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsEntityUuidColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_UUID) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsSrcEntityUuidColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_ID) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsSrcEntityIdColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_UUID) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsTrgEntityUuidColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_ID) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsTrgEntityIdColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.GROUP_NAME) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsGroupColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.GROUP_KEY) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsKeyColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE) return  Messages.IntelligenceLabelProviderImpl_EntityRelationRsTypeColumn;
		if (item == EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE_KEY) return Messages.IntelligenceLabelProviderImpl_EntityRelationRsTypeKeyColumn;
		
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.RECORD_UUID) return Messages.IntelligenceLabelProviderImpl_RecordAttachmentRsRecordUuidColumn;
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.FILE_NAME) return Messages.IntelligenceLabelProviderImpl_RecordAttachmentRsRecordNameColumn;
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.DATE_CREATED) return Messages.IntelligenceLabelProviderImpl_RecordAttachmentRsDateCreatedColumn;
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.PATH) return Messages.IntelligenceLabelProviderImpl_RecordAttachmentRsPathColumn;
		
		if (item == RecordAttributeDatasetResultSetMetadata.Column.RECORD_UUID) return Messages.IntelligenceLabelProviderImpl_RecordAttributeRsColumnRecordUuid;
		if (item == RecordAttributeDatasetResultSetMetadata.Column.ATTRIBUTE) return Messages.IntelligenceLabelProviderImpl_RecordAttributeRsColumnAttributeName;
		if (item == RecordAttributeDatasetResultSetMetadata.Column.TEXT) return Messages.IntelligenceLabelProviderImpl_RecordAttributeRsColumnAttributeValue;
		if (item == RecordAttributeDatasetResultSetMetadata.Column.STRING_VALUE) return Messages.IntelligenceLabelProviderImpl_RecordAttributeRsColumnStringValue;
		if (item == RecordAttributeDatasetResultSetMetadata.Column.NUMBER_VALUE) return Messages.IntelligenceLabelProviderImpl_RecordAttributeRsColumnNumberValue;
		if (item == RecordAttributeDatasetResultSetMetadata.Column.DATE_VALUE) return Messages.IntelligenceLabelProviderImpl_RecordAttributeRsColumnDateValue;
		
		if (item == RecordDatasetResultSetMetadata.Column.UUID) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnUUID;
		if (item == RecordDatasetResultSetMetadata.Column.TITLE) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnTitle;
		if (item == RecordDatasetResultSetMetadata.Column.DESCRIPTION) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnDescription;
		if (item == RecordDatasetResultSetMetadata.Column.SCRATCHPAD) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnScratchpad;
		if (item == RecordDatasetResultSetMetadata.Column.CREATED_BY) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnCreatedBy;
		if (item == RecordDatasetResultSetMetadata.Column.LAST_MODIFIED_BY) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnLastModifiedBy;
		if (item == RecordDatasetResultSetMetadata.Column.CREATED) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnDateCreated;
		if (item == RecordDatasetResultSetMetadata.Column.LAST_MODIFIED) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnDateModified;
		if (item == RecordDatasetResultSetMetadata.Column.STATUS) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnStatusLabel;
		if (item == RecordDatasetResultSetMetadata.Column.STATUS_KEY) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnStatusKey;
		if (item == RecordDatasetResultSetMetadata.Column.SOURCE) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnSource;
		if (item == RecordDatasetResultSetMetadata.Column.SOURCE_ICON) return Messages.IntelligenceLabelProviderImpl_RecordRsColumnSourceImage;
		
		
		if (item.equals(EntityDatasetMetadata.class)) return Messages.IntelligenceLabelProviderImpl_EntityDatasetName;
		if (item.equals(EntityLocationAttributeDatasetMetadata.class)) return Messages.IntelligenceLabelProviderImpl_EntityLocaitonAttributeDatasetName;
		if (item.equals(EntityAttachmentDatasetMetadata.class)) return Messages.IntelligenceLabelProviderImpl_EntityAttachmentDsName;
		if (item.equals(EntityLocationDatasetMetadata.class)) return Messages.IntelligenceLabelProviderImpl_EntityLocationDsName;
		if (item.equals(EntityRecordDatasetMetadata.class)) return Messages.IntelligenceLabelProviderImpl_EntityRecordDsName;
		if (item.equals(EntityRelationDatasetMetadata.class)) return Messages.IntelligenceLabelProviderImpl_EntityRelationDsName;
		if (item.equals(RecordMetadata.class)) return Messages.IntelligenceLabelProviderImpl_DefaultRecordDatasetName;
	
		return ""; //$NON-NLS-1$
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
