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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
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
import org.wcs.smart.i2.birt.entity.search.EntitySearchDataset;
import org.wcs.smart.i2.birt.entity.search.EntitySearchDatasetResultSetMetadata;
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
import org.wcs.smart.i2.query.export.CsvEntitySummaryQueryExporter;
import org.wcs.smart.i2.query.export.CsvRecordQueryExporter;
import org.wcs.smart.i2.query.export.ShpRecordQueryExporter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.ValuePart;
import org.wcs.smart.i2.search.AdvancedEntitySearch;

/**
 * Label provider for the profile module
 * 
 * @author Emily
 *
 */
public class AdvancedLabelProviderImpl implements
		IIntelligenceLabelProvider {

	public String getDataSourceProductName(String dataSetType, Locale l){
		if (dataSetType.equals(RecordDataset.DATASET_TYPE)) return Messages.getString("AdvancedLabelProviderImpl.DatasetTypeRecordDetails", l); //$NON-NLS-1$
		if (dataSetType.equals(RecordAttributeDataset.DATASET_TYPE)) return Messages.getString("AdvancedLabelProviderImpl.DatasetTypeRecordAttributes", l); //$NON-NLS-1$
		if (dataSetType.equals(RecordEntityDataset.DATASET_TYPE)) return Messages.getString("AdvancedLabelProviderImpl.DatasetTypeRecordEntities", l); //$NON-NLS-1$
		if (dataSetType.equals(RecordLocationDataset.DATASET_TYPE)) return Messages.getString("AdvancedLabelProviderImpl.DatasetTypeRecordLocations", l); //$NON-NLS-1$
		if (dataSetType.equals(RecordAttachmentDataset.DATASET_TYPE)) return Messages.getString("AdvancedLabelProviderImpl.DatasetTypeRecordAttachments", l); //$NON-NLS-1$
		
		return ""; //$NON-NLS-1$
	}
	
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof AttributeType){
			
			AttributeType type = (AttributeType) item;
			switch(type){
			case BOOLEAN: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypeBoolean", l); //$NON-NLS-1$
			case DATE: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypeDate", l); //$NON-NLS-1$
			case LIST: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypeList", l); //$NON-NLS-1$
			case NUMERIC: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypeNumeric", l); //$NON-NLS-1$
			case POSITION: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypePosition", l); //$NON-NLS-1$
			case TEXT: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypeText", l); //$NON-NLS-1$
			case EMPLOYEE: return Messages.getString("AdvancedLabelProviderImpl.AttributeTypeEmployee", l); //$NON-NLS-1$
			}
		}
		if (item == DM_SOURCE_LABEL) return Messages.getString("AdvancedLabelProviderImpl.DmObservation", l); //$NON-NLS-1$
		if (item == PROFILE_SOURCE_LABEL) return Messages.getString("AdvancedLabelProviderImpl.ProfileObservation", l); //$NON-NLS-1$
		if (item == QUERY_COLUMN_CATEGORY_LABEL) return Messages.getString("AdvancedLabelProviderImpl.CategoryColumnLabel", l); //$NON-NLS-1$
		if (item == OBS_COUNT_LABEL ) return Messages.getString("AdvancedLabelProviderImpl.ObservationColumnLabel", l); //$NON-NLS-1$
		if (item == INSUFFICIENT_PRIVILEGES_LABEL ) return Messages.getString("AdvancedLabelProviderImpl.InsufficientPrivileges", l); //$NON-NLS-1$
		if (item == IntelWorkingSetCategory.ENTITY) return Messages.getString("AdvancedLabelProviderImpl.WsEnitiesLabel", l); //$NON-NLS-1$
		if (item == IntelWorkingSetCategory.RECORD) return Messages.getString("AdvancedLabelProviderImpl.WsRecordsLabel", l); //$NON-NLS-1$
		if (item == IntelWorkingSetCategory.QUERIES) return Messages.getString("AdvancedLabelProviderImpl.WsQueriesLabel", l); //$NON-NLS-1$
		
		if (item == FixedQueryColumn.Column.LOC_COMMENT) return  Messages.getString("AdvancedLabelProviderImpl.QueryColComment", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.LOC_DATE) return Messages.getString("AdvancedLabelProviderImpl.QueryColDate", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.LOC_TIME) return Messages.getString("AdvancedLabelProviderImpl.QueryColTime", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.LOC_GEOMTRY) return Messages.getString("AdvancedLabelProviderImpl.QueryColGeom", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.LOC_ID) return Messages.getString("AdvancedLabelProviderImpl.QueryColId", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.RECORD_STATUS) return Messages.getString("AdvancedLabelProviderImpl.QueryColRecordStatus", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.RECORD_TITLE) return Messages.getString("AdvancedLabelProviderImpl.QueryColRecordTital", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.RECORD_SOURCE) return Messages.getString("AdvancedLabelProviderImpl.QueryColRecordSource", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.RECORD_DATE) return Messages.getString("AdvancedLabelProviderImpl.RecordDateColName", l); //$NON-NLS-1$

		if (item == FixedQueryColumn.Column.ENTITY_ID) return Messages.getString("AdvancedLabelProviderImpl.QueryColEntityId", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.ENTITY_TYPE) return Messages.getString("AdvancedLabelProviderImpl.QueryColEntityTable", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.ENTITY_PROFILE) return Messages.getString("AdvancedLabelProviderImpl.QueryColEntityProfile", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.RECORD_PROFILE) return Messages.getString("AdvancedLabelProviderImpl.QueryColRecordProfile", l); //$NON-NLS-1$
		
		
		if (item == FixedQueryColumn.Column.CA_ID) return Messages.getString("AdvancedLabelProviderImpl.CaIdColumn", l); //$NON-NLS-1$
		if (item == FixedQueryColumn.Column.CA_NAME) return Messages.getString("AdvancedLabelProviderImpl.CaNameColumn", l); //$NON-NLS-1$

		if (item == IntelRecord.Status.NEW) return Messages.getString("AdvancedLabelProviderImpl.IntelRecordStatusUnprocessed", l); //$NON-NLS-1$
		if (item == IntelRecord.Status.PROCESSING) return Messages.getString("AdvancedLabelProviderImpl.IntelRecordStatusInProgress", l); //$NON-NLS-1$
		if (item == IntelRecord.Status.COMPLETE) return Messages.getString("AdvancedLabelProviderImpl.IntelRecordStatusComplete", l); //$NON-NLS-1$
		
		if (item instanceof Operator){
			switch((Operator)item){
				case EXACT: return Messages.getString("AdvancedLabelProviderImpl.ExactOp", l); //$NON-NLS-1$
				case AND: return Messages.getString("AdvancedLabelProviderImpl.AndOp", l); //$NON-NLS-1$
				case BETWEEN: return Messages.getString("AdvancedLabelProviderImpl.BetweenOp", l); //$NON-NLS-1$
				case BRACKETS: return "( )"; //$NON-NLS-1$
				case BRACKET_CLOSE: return ")"; //$NON-NLS-1$
				case BRACKET_OPEN: return "("; //$NON-NLS-1$
				case EQUALS: return "="; //$NON-NLS-1$
				case GREATERTHAN: return ">"; //$NON-NLS-1$
				case GREATERTHANEQUALS: return ">="; //$NON-NLS-1$
				case LESSTHAN: return "<"; //$NON-NLS-1$
				case LESSTHANEQUALS: return "<="; //$NON-NLS-1$
				case NOT: return Messages.getString("AdvancedLabelProviderImpl.NotOp", l); //$NON-NLS-1$
				case NOTEQUALS: return "!="; //$NON-NLS-1$
				case NOT_BETWEEN: return Messages.getString("AdvancedLabelProviderImpl.NotBetweenOp", l); //$NON-NLS-1$
				case OR: return Messages.getString("AdvancedLabelProviderImpl.OrOp", l); //$NON-NLS-1$
				case STR_CONTAINS: return Messages.getString("AdvancedLabelProviderImpl.ContainsOp", l); //$NON-NLS-1$
				case STR_EQUALS: return Messages.getString("AdvancedLabelProviderImpl.EqualsOp", l); //$NON-NLS-1$
				case STR_NOTCONTAINS: return Messages.getString("AdvancedLabelProviderImpl.NotEqualsOp", l); //$NON-NLS-1$
			}
		}
		
		if (item instanceof CsvRecordQueryExporter) return Messages.getString("AdvancedLabelProviderImpl.CsvExporter", l); //$NON-NLS-1$
		if (item instanceof ShpRecordQueryExporter) return Messages.getString("AdvancedLabelProviderImpl.ShpExporter", l); //$NON-NLS-1$
		if (item instanceof CsvEntitySummaryQueryExporter) return Messages.getString("AdvancedLabelProviderImpl.CsvExporter", l); //$NON-NLS-1$
		
		if (item == IntelQueryColumnProvider.ANY_ITEM) return Messages.getString("AdvancedLabelProviderImpl.AnyLabel", l); //$NON-NLS-1$
		if (item == Boolean.TRUE) return  Messages.getString("SmartLabelProvider.BooleanYesOp",l); //$NON-NLS-1$
		if (item == Boolean.FALSE) return Messages.getString("SmartLabelProvider.BooleanNoOp",l); //$NON-NLS-1$
		
		
		if (item == RecordLocationDatasetResultSetMetadata.Column.COMMENT) return Messages.getString("AdvancedLabelProviderImpl.RecordLocationDatasetColComment", l); //$NON-NLS-1$
		if (item == RecordLocationDatasetResultSetMetadata.Column.DATE) return Messages.getString("AdvancedLabelProviderImpl.RecordLocationDatasetColDate", l); //$NON-NLS-1$
		if (item == RecordLocationDatasetResultSetMetadata.Column.GEOM) return Messages.getString("AdvancedLabelProviderImpl.RecordLocationDatasetColGeom", l); //$NON-NLS-1$
		if (item == RecordLocationDatasetResultSetMetadata.Column.ID) return Messages.getString("AdvancedLabelProviderImpl.RecordLocationDatasetColId", l); //$NON-NLS-1$
		if (item == RecordLocationDatasetResultSetMetadata.Column.OBSERVATION) return Messages.getString("AdvancedLabelProviderImpl.RecordLocationDatasetColObs", l); //$NON-NLS-1$
		if (item == RecordLocationDatasetResultSetMetadata.Column.RECORD_UUID) return Messages.getString("AdvancedLabelProviderImpl.RecordLocationDatasetColRecorduuid", l); //$NON-NLS-1$
		
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_ID) return Messages.getString("AdvancedLabelProviderImpl.RecordEntityDatasetColEntityId", l); //$NON-NLS-1$
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_IMAGE) return Messages.getString("AdvancedLabelProviderImpl.RecordEntityDatasetColImage", l); //$NON-NLS-1$
		if (item == RecordEntityDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.RecordEntityDatasetColEntityUuid", l); //$NON-NLS-1$
		if (item == RecordEntityDatasetResultSetMetadata.Column.UUID) return Messages.getString("AdvancedLabelProviderImpl.RecordEntityDatasetColRecordUuid", l); //$NON-NLS-1$
		
		if (item == EntityDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColEntityUuid", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.ID) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColId", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.TYPE_KEY) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColEntityTypeKey", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.TYPE) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColEntityType", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.DATE_CREATED) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColDateCreated", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.DATE_MODIFIED) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColDateMod", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.CREATED_BY) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColCreatedBy", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.MODIFIED_BY) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColModBy", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.PRIMARY_IMAGE) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColImage", l); //$NON-NLS-1$
		if (item == EntityDatasetResultSetMetadata.Column.PROFILE) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColProfile", l); //$NON-NLS-1$

		if (item == EntitySearchDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColEntityUuid", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.ID) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColId", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.TYPE_KEY) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColEntityTypeKey", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.TYPE) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColEntityType", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.DATE_CREATED) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColDateCreated", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.DATE_MODIFIED) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColDateMod", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.CREATED_BY) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColCreatedBy", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.MODIFIED_BY) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColModBy", l); //$NON-NLS-1$
		if (item == EntitySearchDatasetResultSetMetadata.Column.PRIMARY_IMAGE) return Messages.getString("AdvancedLabelProviderImpl.EntityDatasetColImage", l); //$NON-NLS-1$
		
		if (item == EntitySearchDataset.NOT_FOUND_KEY) return Messages.getString("AdvancedLabelProviderImpl.SearchNotFound", l); //$NON-NLS-1$

		
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationAttributeDatasetColEntity", l); //$NON-NLS-1$
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE_KEY) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationAttributeDatasetColAttributeKey", l); //$NON-NLS-1$
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.ATTRIBUTE_NAME) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationAttributeDatasetColAttributeName", l); //$NON-NLS-1$
		if (item == EntityLocationAttributeDatasetResultSetMetadata.Column.GEOMETRY)  return Messages.getString("AdvancedLabelProviderImpl.EntityLocationAttributeDatasetColGeom", l); //$NON-NLS-1$
			
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColEntity", l); //$NON-NLS-1$
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.FILE_NAME) return  Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColFileName", l); //$NON-NLS-1$
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.DATE_CREATED) return  Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColDateCreated", l); //$NON-NLS-1$
		if (item == EntityAttachmentDatasetResultSetMetadata.Column.PATH) return  Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColFile", l); //$NON-NLS-1$
				
		if (item == EntityLocationDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationDatasetColEntity", l); //$NON-NLS-1$
		if (item == EntityLocationDatasetResultSetMetadata.Column.ID) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationDatasetColId", l); //$NON-NLS-1$
		if (item == EntityLocationDatasetResultSetMetadata.Column.GEOM) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationDatasetColGeom", l); //$NON-NLS-1$
		if (item == EntityLocationDatasetResultSetMetadata.Column.DATE) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationDatasetColDate", l); //$NON-NLS-1$
		if (item == EntityLocationDatasetResultSetMetadata.Column.OBSERVATION) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationDatasetColObservation", l); //$NON-NLS-1$
		if (item == EntityLocationDatasetResultSetMetadata.Column.SOURCELINK) return Messages.getString("AdvancedLabelProviderImpl.SourceLinkColumnName",l); //$NON-NLS-1$
		if (item == EntityLocationDatasetResultSetMetadata.Column.SOURCE) return Messages.getString("AdvancedLabelProviderImpl.SourceColumnName",l); //$NON-NLS-1$
				
		if (item == EntityRecordDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityRecordDatasetColEntity", l); //$NON-NLS-1$
		if (item == EntityRecordDatasetResultSetMetadata.Column.TITLE) return Messages.getString("AdvancedLabelProviderImpl.EntityRecordDatasetColTitle", l); //$NON-NLS-1$
		if (item == EntityRecordDatasetResultSetMetadata.Column.STATUS) return Messages.getString("AdvancedLabelProviderImpl.EntityRecordDatasetColStatus", l); //$NON-NLS-1$
		if (item == EntityRecordDatasetResultSetMetadata.Column.DATE_RECIEVED) return Messages.getString("AdvancedLabelProviderImpl.EntityRecordDatasetColDateRec", l); //$NON-NLS-1$
		if (item == EntityRecordDatasetResultSetMetadata.Column.DATE_MODIFIED) return Messages.getString("AdvancedLabelProviderImpl.EntityRecordDatasetColDateMod", l); //$NON-NLS-1$
		if (item == EntityRecordDatasetResultSetMetadata.Column.DESCRIPTION) return Messages.getString("AdvancedLabelProviderImpl.EntityRecordDatasetColDescription", l); //$NON-NLS-1$

		if (item == EntityRelationDatasetResultSetMetadata.Column.ENTITY_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColEntity", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColSrcRelationUUID", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_ID) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColSrcRelation", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColTrgRelationUuid", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_ID) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColTrgRelation", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.GROUP_NAME) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColGroup", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.GROUP_KEY) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColGroupKey", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE) return  Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColRtype", l); //$NON-NLS-1$
		if (item == EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE_KEY) return Messages.getString("AdvancedLabelProviderImpl.EntityRelationDatasetColRtypeKey", l); //$NON-NLS-1$
		
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.RECORD_UUID) return Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColRecord", l); //$NON-NLS-1$
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.FILE_NAME) return Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColName", l); //$NON-NLS-1$
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.DATE_CREATED) return Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColDateCreated", l); //$NON-NLS-1$
		if (item == RecordAttachmentDatasetResultSetMetadata.Column.PATH) return Messages.getString("AdvancedLabelProviderImpl.EntityAttachmentDatasetColPath", l); //$NON-NLS-1$
		
		if (item == RecordAttributeDatasetResultSetMetadata.Column.RECORD_UUID) return Messages.getString("AdvancedLabelProviderImpl.RecordAttributeDatasetColRecordUuid", l); //$NON-NLS-1$
		if (item == RecordAttributeDatasetResultSetMetadata.Column.ATTRIBUTE) return Messages.getString("AdvancedLabelProviderImpl.RecordAttributeDatasetColName", l); //$NON-NLS-1$
		if (item == RecordAttributeDatasetResultSetMetadata.Column.TEXT) return Messages.getString("AdvancedLabelProviderImpl.RecordAttributeDatasetColValue", l); //$NON-NLS-1$
		if (item == RecordAttributeDatasetResultSetMetadata.Column.STRING_VALUE) return Messages.getString("AdvancedLabelProviderImpl.RecordAttributeDatasetStrValue", l); //$NON-NLS-1$
		if (item == RecordAttributeDatasetResultSetMetadata.Column.NUMBER_VALUE) return Messages.getString("AdvancedLabelProviderImpl.RecordAttributeDatasetColNumberValue", l); //$NON-NLS-1$
		if (item == RecordAttributeDatasetResultSetMetadata.Column.DATE_VALUE) return Messages.getString("AdvancedLabelProviderImpl.RecordAttributeDatasetColDateValue", l); //$NON-NLS-1$
		
		if (item == RecordDatasetResultSetMetadata.Column.UUID) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColuuid", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.TITLE) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColTitle", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.DESCRIPTION) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColDescription", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.SCRATCHPAD) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColScratchPad", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.CREATED_BY) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColCreatedBy", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.LAST_MODIFIED_BY) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetModBy", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.CREATED) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColCreated", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.LAST_MODIFIED) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColMod", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.STATUS) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColStatus", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.STATUS_KEY) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColStatusKey", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.SOURCE) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColSrc", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.SOURCE_ICON) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColSrcImg", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.PROFILE) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColProfile", l); //$NON-NLS-1$
		if (item == RecordDatasetResultSetMetadata.Column.PRIMARY_DATE) return Messages.getString("AdvancedLabelProviderImpl.RecordDatasetColRecordDate", l); //$NON-NLS-1$

		
		if (item.equals(EntityDatasetMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.EntityDataset", l); //$NON-NLS-1$
		if (item.equals(EntityLocationAttributeDatasetMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.EntityLocationAttributes", l); //$NON-NLS-1$
		if (item.equals(EntityAttachmentDatasetMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.EntityAttachment", l); //$NON-NLS-1$
		if (item.equals(EntityLocationDatasetMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.EntityLocations", l); //$NON-NLS-1$
		if (item.equals(EntityRecordDatasetMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.EntityRecords", l); //$NON-NLS-1$
		if (item.equals(EntityRelationDatasetMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.EntityRelation", l); //$NON-NLS-1$
		if (item.equals(RecordMetadata.class)) return Messages.getString("AdvancedLabelProviderImpl.RecordMetadata", l); //$NON-NLS-1$
	
		if (item.equals(AdvancedEntitySearch.Error.ATTRIBUTE_TYPE_NOT_SUPPORTED)) return Messages.getString("AdvancedLabelProviderImpl.AdvSearchParseError", l); //$NON-NLS-1$
		if (item.equals(AdvancedEntitySearch.Error.PARSE_ERROR)) return Messages.getString("AdvancedLabelProviderImpl.AdvSearchRunError", l); //$NON-NLS-1$
		if (item.equals(AdvancedEntitySearch.Error.RUN_ERROR)) return Messages.getString("AdvancedLabelProviderImpl.AdvSearchAttributeTypeNotSupported", l); //$NON-NLS-1$
		if (item.equals(AdvancedEntitySearch.Error.TOKEN_NOT_SUPPORTED)) return Messages.getString("AdvancedLabelProviderImpl.AdvSearchTokenNotSupported", l); //$NON-NLS-1$
		
		if (item == ValuePart.ValueOption.NUMBER_ENTITIES) return Messages.getString("AdvancedLabelProviderImpl.NumberOfEntitiesValue", l); //$NON-NLS-1$
		if (item == ValuePart.ValueOption.NUMBER_RECORDS) return Messages.getString("AdvancedLabelProviderImpl.NumberOfRecordsValue", l); //$NON-NLS-1$
		
		if (item instanceof SystemAttributeFilter.SystemAttribute) {
			switch ((SystemAttributeFilter.SystemAttribute)item) {
			case RECORD_DATE_CREATED:
			case ENTITY_DATE_CREATED: return Messages.getString("AdvancedLabelProviderImpl.SysAttDateCreated", l); //$NON-NLS-1$
			case RECORD_DATE_MODIFIED:
			case ENTITY_DATE_MODIFIED: return Messages.getString("AdvancedLabelProviderImpl.SysAttDateModified", l); //$NON-NLS-1$
			case RECORD_DATE: return Messages.getString("AdvancedLabelProviderImpl.SysAttRecordDate", l); //$NON-NLS-1$
			case RECORD_SOURCE: return Messages.getString("AdvancedLabelProviderImpl.SysAttRecordSource", l); //$NON-NLS-1$
			case RECORD_STATUS: return Messages.getString("AdvancedLabelProviderImpl.SysAttRecordStatus", l); //$NON-NLS-1$
			}
		}
		
		if (item.equals("MOTIVATEDBY")) return "Motivated By Profile Record";

		return ""; //$NON-NLS-1$
	}

}
