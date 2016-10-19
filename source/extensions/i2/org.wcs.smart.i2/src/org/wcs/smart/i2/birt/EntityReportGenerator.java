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
package org.wcs.smart.i2.birt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.internal.ui.util.DataUtil;
import org.eclipse.birt.report.model.adapter.oda.ModelOdaAdapter;
import org.eclipse.birt.report.model.api.AutoTextHandle;
import org.eclipse.birt.report.model.api.ColumnHandle;
import org.eclipse.birt.report.model.api.DataItemHandle;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ElementFactory;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.GridHandle;
import org.eclipse.birt.report.model.api.ImageHandle;
import org.eclipse.birt.report.model.api.LabelHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSourceHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.ScalarParameterHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.SimpleMasterPageHandle;
import org.eclipse.birt.report.model.api.StructureFactory;
import org.eclipse.birt.report.model.api.StyleHandle;
import org.eclipse.birt.report.model.api.TableHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.ComputedColumn;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.interfaces.IStyleModel;
import org.eclipse.birt.report.model.elements.interfaces.ITableColumnModel;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DataSetParameters;
import org.eclipse.datatools.connectivity.oda.design.DataSourceDesign;
import org.eclipse.datatools.connectivity.oda.design.DesignFactory;
import org.eclipse.datatools.connectivity.oda.design.ParameterDefinition;
import org.eclipse.datatools.connectivity.oda.design.ResultSetColumns;
import org.eclipse.datatools.connectivity.oda.design.ResultSetDefinition;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.birt.datasource.IntelBirtDataSource;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDataset;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDataset;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDatasetResultSetMetadata;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.birt.map.item.SmartMapItem;

/**
 * Entity Type template generator.  Generates the default template for a given
 * entity type.
 * 
 * @author Emily
 *
 */
public enum EntityReportGenerator {
	INSTANCE;
	
	public void generateReport(IntelEntityType entityType, Path file) throws Exception{
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
		ReportDesignHandle rdh = session.createDesign(file.toFile().getAbsolutePath());
		ModelOdaAdapter modelAdapter = new ModelOdaAdapter();
		try {
			rdh.setTitle(entityType.getName());
		} catch (SemanticException e) {
			//lets just consume this 
		}
		
		//add parameters - entity types and dates
		ScalarParameterHandle shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.ENTITY_UUID.getName());
		shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
		shandler.setIsRequired(true);
		shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_STRING);
		shandler.setDistinct(true);
		rdh.getParameters().add(shandler);
		
		shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.START_DATE.getName());
		shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
		shandler.setIsRequired(false);
		shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_DATETIME);
		shandler.setDistinct(true);
		rdh.getParameters().add(shandler);
		
		shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.END_DATE.getName());
		shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
		shandler.setIsRequired(false);
		shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_DATETIME);
		shandler.setDistinct(true);
		rdh.getParameters().add(shandler);
		
		//add intelligence data source
		String dataSourceName = "Intelligence Data Source";
		OdaDataSourceHandle datasource = rdh.getElementFactory().newOdaDataSource(dataSourceName, IntelBirtDataSource.ODA_DATA_SOURCE_ID);
		rdh.getDataSources().add(datasource);
		DataSourceDesign dSource = modelAdapter.createDataSourceDesign(datasource);
		
		//add datasets
		String[] datasets = new String[]{
				 EntityDataset.DATASET_TYPE,
				 EntityRecordDataset.DATASET_TYPE,
				 EntityAttachmentDataset.DATASET_TYPE,
				 EntityLocationDataset.DATASET_TYPE,
				 EntityRelationDataset.DATASET_TYPE
		};
		
		HashMap<String, OdaDataSetHandle> datasetHandles = new HashMap<>();
		for (String d : datasets){
			DataSetDesign dsDesign = DesignFactory.eINSTANCE.createDataSetDesign();
			dsDesign.setDataSourceDesign(dSource);
			dsDesign.setName(IntelReportManager.INSTANCE.getName(entityType, d));
			dsDesign.setDisplayName(IntelReportManager.INSTANCE.getName(entityType, d));
			dsDesign.setQueryText(entityType.getKeyId());
			dsDesign.setOdaExtensionDataSetId(d);
			
			IntelBirtConnection connection = new IntelBirtConnection();
			connection.open(null);
			try{
				IQuery query = connection.newQuery(d);
				query.prepare(entityType.getKeyId());
				IResultSetMetaData md = query.getMetaData();
				ResultSetColumns columns = DesignSessionUtil.toResultSetColumnsDesign(md);
				//result set metadata
				ResultSetDefinition resultSetDefn = DesignFactory.eINSTANCE.createResultSetDefinition();
				resultSetDefn.setResultSetColumns(columns);
				dsDesign.setPrimaryResultSet(resultSetDefn);
				dsDesign.getResultSets().setDerivedMetaData(true);

				//parameter metadata						
				DataSetParameters paramDesign = DesignSessionUtil.toDataSetParametersDesign(
						query.getParameterMetaData(),
						DesignSessionUtil.toParameterModeDesign(IParameterMetaData.parameterModeIn));
				dsDesign.setParameters(paramDesign);
				if (paramDesign != null){
					paramDesign.setDerivedMetaData(true);
					if (paramDesign.getParameterDefinitions().size() > 0) {
						for (ParameterDefinition param : paramDesign.getParameterDefinitions()) {
							//will be linked automatically below
							param.setDefaultScalarValue("TODO: Link to Report Parameter");
						}
					}
				}
			}finally{
				connection.close();
			}
			OdaDataSetHandle dataset = modelAdapter.createDataSetHandle(dsDesign, rdh.getModuleHandle());
			dataset.setDataSource(dataSourceName);
			rdh.getDataSets().add(dataset);

			//link to report parameters
			PropertyHandle odaDataSetParameterProp = dataset.getPropertyHandle(OdaDataSetHandle.PARAMETERS_PROP);
			List<?> items = odaDataSetParameterProp.getItems();
			for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
				OdaDataSetParameter parameter = (OdaDataSetParameter) iterator.next();
				if (parameter.getName().equals(DataSourceParameter.ENTITY_UUID.getName())) {
					parameter.setDefaultValue(""); //$NON-NLS-1$
					parameter.setParamName(parameter.getName());
				}else if (parameter.getName().equals(DataSourceParameter.START_DATE.getName())) {
					parameter.setDefaultValue(""); //$NON-NLS-1$
					parameter.setParamName(parameter.getName());
				}else if (parameter.getName().equals(DataSourceParameter.END_DATE.getName())) {
					parameter.setDefaultValue(""); //$NON-NLS-1$
					parameter.setParamName(parameter.getName());
				}
			}
			
			try{
				ArrayList<?> hcolumns = (ArrayList<?>) dataset.getProperty("columnHints");  //$NON-NLS-1$
				for (Object col : hcolumns){
					((ColumnHint)col).setProperty("alias", ((ColumnHint)col).getProperty(dataset.getModule(), "displayName"));   //$NON-NLS-1$//$NON-NLS-2$
				}
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
			
			datasetHandles.put(d,  dataset);
		}
		
		initializeValue(rdh, datasetHandles, entityType);
		rdh.save();
		rdh.close();
	}
	
	//TODO i18n
	private void initializeValue(ReportDesignHandle rdh, HashMap<String,OdaDataSetHandle> datasetHandles, IntelEntityType type) throws Exception{
		ElementFactory factory = rdh.getElementFactory();
		
		StyleHandle headerStyle = factory.newStyle("HeaderStyle");
		headerStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_WEIGHT, DesignChoiceConstants.FONT_WEIGHT_BOLD);
		headerStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_FAMILY, "Verdana");
		headerStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_SIZE, "14pt");
		headerStyle.setProperty(IStyleModel.PADDING_BOTTOM_PROP, "3px");
		headerStyle.setProperty(IStyleModel.PADDING_TOP_PROP, "3px");
		headerStyle.setProperty(IStyleModel.PADDING_LEFT_PROP, "3px");
		headerStyle.setProperty(IStyleModel.PADDING_RIGHT_PROP, "3px");
		headerStyle.setProperty(IStyleModel.BORDER_BOTTOM_STYLE_PROP, DesignChoiceConstants.LINE_STYLE_SOLID);
		headerStyle.setProperty(IStyleModel.BORDER_BOTTOM_WIDTH_PROP, "1px");
		rdh.getStyles().add(headerStyle);
		
		StyleHandle sectionHeaderStyle = factory.newStyle("SectionHeaderStyle");
		sectionHeaderStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_WEIGHT, DesignChoiceConstants.FONT_WEIGHT_BOLD);
		sectionHeaderStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_FAMILY, "Verdana");
		sectionHeaderStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_SIZE, "10pt");
		sectionHeaderStyle.setProperty(IStyleModel.BACKGROUND_COLOR_PROP, "#E2E4E6");
		sectionHeaderStyle.setProperty(IStyleModel.PADDING_BOTTOM_PROP, "3px");
		sectionHeaderStyle.setProperty(IStyleModel.PADDING_TOP_PROP, "3px");
		sectionHeaderStyle.setProperty(IStyleModel.PADDING_LEFT_PROP, "3px");
		sectionHeaderStyle.setProperty(IStyleModel.PADDING_RIGHT_PROP, "3px");
		rdh.getStyles().add(sectionHeaderStyle);
		
		StyleHandle tableStyle = factory.newStyle("TableStyle");
		tableStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_FAMILY, "Verdana");
		tableStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_SIZE, "8pt");
		rdh.getStyles().add(tableStyle);
		
		StyleHandle footerStyle = factory.newStyle("FooterStyle");
		footerStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_FAMILY, "Verdana");
		footerStyle.setProperty(DesignChoiceConstants.CHOICE_FONT_SIZE, "6pt");
		rdh.getStyles().add(footerStyle);
		
		OdaDataSetHandle entityDataset = datasetHandles.get(EntityDataset.DATASET_TYPE);
		
		DataItemHandle headerId = factory.newDataItem(null);
		
		headerId.setDataSet(entityDataset);
		ComputedColumn cc = StructureFactory.createComputedColumn();
		cc.setProperty("name", "ID");
		cc.setDisplayName("ID");
		cc.setDataType("string");
		cc.setExpression("dataSetRow[\"ID\"]");
		headerId.getColumnBindings().addItem(cc);
		headerId.setResultSetColumn("ID");
		headerId.setStyleName(headerStyle.getName());
				
		rdh.getBody().add(headerId);
		
		LabelHandle ll = factory.newLabel(null);
		rdh.getBody().add(ll);
		
		GridHandle headerGrid = factory.newGridItem(null, 2, 1);
		rdh.getBody().add(headerGrid);
		headerGrid.setDataSet(entityDataset);
		
		List<ComputedColumn> entityColumns = DataUtil.generateComputedColumns(headerGrid);
		for (ComputedColumn c : entityColumns){
			headerGrid.getColumnBindings().addItem(c);
		}

		ImageHandle primaryImage = factory.newImage(null);
		primaryImage.setWidth("2in");
		primaryImage.setHeight("2in");
		primaryImage.setSource(DesignChoiceConstants.IMAGE_REF_TYPE_URL);
		primaryImage.setURL("row[\"Primary Image\"]");
		
		headerGrid.getCell(1, 1).getContent().add(primaryImage);
		((ColumnHandle)headerGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "2.2in");
		
		
		
		GridHandle infoGrid = factory.newGridItem(null, 2, 5);
		infoGrid.setStyleName(tableStyle.getName());
		((ColumnHandle)infoGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "1.3in");
		headerGrid.getCell(1, 2).getContent().add(infoGrid);
		
		int row = 1;
		LabelHandle l = factory.newLabel(null);
		l.setText("Entity Type:");
		infoGrid.getCell(row,1).getContent().add(l);
		
		DataItemHandle di = factory.newDataItem(null);
		di.setResultSetColumn("Entity Type");
		infoGrid.getCell(row,2).getContent().add(di);		
		row++;
		
		l = factory.newLabel(null);
		l.setText("Date Created:");
		infoGrid.getCell(row,1).getContent().add(l);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn("Date Created");
		infoGrid.getCell(row,2).getContent().add(di);
		row++;
		
		l = factory.newLabel(null);
		l.setText("Last Modified:");
		infoGrid.getCell(row,1).getContent().add(l);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn("Last Modified");
		infoGrid.getCell(row,2).getContent().add(di);
		row++;
		
		l = factory.newLabel(null);
		l.setText("Created By:");
		infoGrid.getCell(row,1).getContent().add(l);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn("Created By");
		infoGrid.getCell(row,2).getContent().add(di);
		row++;
		
		l = factory.newLabel(null);
		l.setText("Last Modified By:");
		infoGrid.getCell(row,1).getContent().add(l);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn("Last Modified By");
		infoGrid.getCell(row,2).getContent().add(di);
		
		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
		
		l = factory.newLabel(null);
		l.setText("Attributes");
		l.setStyleName(sectionHeaderStyle.getName());
		rdh.getBody().add(l);
		
		GridHandle attributeGrid = factory.newGridItem(null, 2, type.getAttributes().size() + 1);
		attributeGrid.setDataSet(entityDataset);
		attributeGrid.setStyleName(tableStyle.getName());
		rdh.getBody().add(attributeGrid);
		
		for (ComputedColumn c : entityColumns){
			attributeGrid.getColumnBindings().addItem(c);
		}
		((ColumnHandle)attributeGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "20%");
		
		l = factory.newLabel(null);
		l.setText("Entity Type:");
		attributeGrid.getCell(1,1).getContent().add(l);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn("Entity Type");
		attributeGrid.getCell(1,2).getContent().add(di);
		
		int rowcnt = 2;
		for (IntelEntityTypeAttribute a : type.getAttributes()){
			l = factory.newLabel(null);
			l.setText(a.getAttribute().getName() + ":");
			attributeGrid.getCell(rowcnt,1).getContent().add(l);
			
			
			di = factory.newDataItem(null);
			di.setResultSetColumn(a.getAttribute().getName());
			attributeGrid.getCell(rowcnt,2).getContent().add(di);
			
			rowcnt++;
		}
		
		
		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
		
		/* ----- Attachments Table ----- */
		l = factory.newLabel(null);
		l.setText("Attachments");
		l.setStyleName(sectionHeaderStyle.getName());
		rdh.getBody().add(l);
		
		TableHandle attachmentsTable = factory.newTableItem(null,2);
		attachmentsTable.setDataSet(datasetHandles.get(EntityAttachmentDataset.DATASET_TYPE));
		attachmentsTable.setStyleName(tableStyle.getName());
		rdh.getBody().add(attachmentsTable);
		
		List<ComputedColumn> attachmentsColumns = DataUtil.generateComputedColumns(attachmentsTable);
		for (ComputedColumn c : attachmentsColumns){
			attachmentsTable.getColumnBindings().addItem(c);
		}
		((ColumnHandle)attachmentsTable.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "1.2in");
		
		ImageHandle attachImage = factory.newImage(null);
		attachImage.setWidth("1in");
		attachImage.setHeight("1in");
		attachImage.setSource(DesignChoiceConstants.IMAGE_REF_TYPE_URL);
		attachImage.setURL("row[\"" + EntityAttachmentDatasetResultSetMetadata.Column.PATH.getColumnName() + "\"]");
		attachmentsTable.getCell(attachmentsTable.getDetail().getSlotID(), -1, 1, 1).getContent().add(attachImage);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn(EntityAttachmentDatasetResultSetMetadata.Column.FILE_NAME.getColumnName());
		attachmentsTable.getCell(attachmentsTable.getDetail().getSlotID(), -1, 1, 2).getContent().add(di);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn(EntityAttachmentDatasetResultSetMetadata.Column.DATE_CREATED.getColumnName());
		attachmentsTable.getCell(attachmentsTable.getDetail().getSlotID(), -1, 1, 2).getContent().add(di);
				
		
		/* ----- Relationship ----- */
		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
				
		l = factory.newLabel(null);
		l.setText("Relationships");
		l.setStyleName(sectionHeaderStyle.getName());
		rdh.getBody().add(l);
		
		
		List<String> names = new ArrayList<String>();
		
		List<String> toExclude = new ArrayList<String>();
		toExclude.add(EntityRelationDatasetResultSetMetadata.Column.ENTITY_UUID.getColumnName());
		toExclude.add(EntityRelationDatasetResultSetMetadata.Column.SOURCE_RELATION_UUID.getColumnName());
		toExclude.add(EntityRelationDatasetResultSetMetadata.Column.TARGET_RELATION_UUID.getColumnName());
		toExclude.add(EntityRelationDatasetResultSetMetadata.Column.GROUP_KEY.getColumnName());
		toExclude.add(EntityRelationDatasetResultSetMetadata.Column.GROUP_NAME.getColumnName());
		toExclude.add(EntityRelationDatasetResultSetMetadata.Column.RELATIONSHIP_TYPE_KEY.getColumnName());
		
		TableHandle relationsTable = factory.newTableItem(null,2);
		relationsTable.setDataSet(datasetHandles.get(EntityRelationDataset.DATASET_TYPE));
		
		List<ComputedColumn> relationsColumns = DataUtil.generateComputedColumns(relationsTable);

		for (ComputedColumn c : relationsColumns){
			if (!toExclude.contains(c.getName())){
				names.add(c.getName());
			}
		}
		
		relationsTable = factory.newTableItem(null,names.size());
		relationsTable.setDataSet(datasetHandles.get(EntityRelationDataset.DATASET_TYPE));
		relationsTable.setStyleName(tableStyle.getName());
		rdh.getBody().add(relationsTable);
		for (ComputedColumn c : relationsColumns){
			relationsTable.getColumnBindings().addItem(c);
		}
		relationsTable.getHeader().get(0).setProperty(DesignChoiceConstants.CHOICE_FONT_WEIGHT,DesignChoiceConstants.FONT_WEIGHT_BOLD);
		relationsTable.getHeader().get(0).setProperty(IStyleModel.BORDER_BOTTOM_STYLE_PROP, DesignChoiceConstants.LINE_STYLE_SOLID);
		relationsTable.getHeader().get(0).setProperty(IStyleModel.BORDER_BOTTOM_WIDTH_PROP, "1px");
		
		relationsTable.getDetail().get(0).setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_CENTER);
		
		int colindex = 1;
		for (String s : names){
			l = factory.newLabel(null);
			l.setText(s);
			relationsTable.getCell(relationsTable.getHeader().getSlotID(), -1, 1, colindex).getContent().add(l);
			
			di = factory.newDataItem(null);
			di.setResultSetColumn(s);
			relationsTable.getCell(relationsTable.getDetail().getSlotID(), -1, 1, colindex).getContent().add(di);
			
			colindex++;
		}
		
		/* ----- Records ----- */
		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
				
		l = factory.newLabel(null);
		l.setText("Records");
		l.setStyleName(sectionHeaderStyle.getName());
		rdh.getBody().add(l);
		
		
		names = new ArrayList<String>();
		toExclude = new ArrayList<String>();
		toExclude.add(EntityRecordDatasetResultSetMetadata.Column.ENTITY_UUID.getColumnName());
		toExclude.add(EntityRecordDatasetResultSetMetadata.Column.STATUS.getColumnName());
		
		TableHandle recordsTable = factory.newTableItem(null,2);
		recordsTable.setDataSet(datasetHandles.get(EntityRecordDataset.DATASET_TYPE));
		List<ComputedColumn> recordsColumns = DataUtil.generateComputedColumns(recordsTable);
		for (ComputedColumn c : recordsColumns){
			if (!toExclude.contains(c.getName())){
				names.add(c.getName());
			}
		}
		
		recordsTable = factory.newTableItem(null,names.size());
		recordsTable.setDataSet(datasetHandles.get(EntityRecordDataset.DATASET_TYPE));
		recordsTable.setStyleName(tableStyle.getName());
		rdh.getBody().add(recordsTable);
		for (ComputedColumn c : recordsColumns){
			recordsTable.getColumnBindings().addItem(c);
		}
		recordsTable.getHeader().get(0).setProperty(DesignChoiceConstants.CHOICE_FONT_WEIGHT,DesignChoiceConstants.FONT_WEIGHT_BOLD);
		recordsTable.getHeader().get(0).setProperty(IStyleModel.BORDER_BOTTOM_STYLE_PROP, DesignChoiceConstants.LINE_STYLE_SOLID);
		recordsTable.getHeader().get(0).setProperty(IStyleModel.BORDER_BOTTOM_WIDTH_PROP, "1px");
		recordsTable.getDetail().get(0).setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_CENTER);
		
		colindex = 1;
		for (String s : names){
			l = factory.newLabel(null);
			l.setText(s);
			recordsTable.getCell(relationsTable.getHeader().getSlotID(), -1, 1, colindex).getContent().add(l);
			
			di = factory.newDataItem(null);
			di.setResultSetColumn(s);
			recordsTable.getCell(relationsTable.getDetail().getSlotID(), -1, 1, colindex).getContent().add(di);
			
			if (s.equalsIgnoreCase(EntityRecordDatasetResultSetMetadata.Column.DATE_MODIFIED.getColumnName()) || 
					s.equalsIgnoreCase(EntityRecordDatasetResultSetMetadata.Column.DATE_RECIEVED.getColumnName())){
				((ColumnHandle)recordsTable.getColumns().get(colindex-1).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "9em");
			}else if (s.equalsIgnoreCase(EntityRecordDatasetResultSetMetadata.Column.TITLE.getColumnName())){
				((ColumnHandle)recordsTable.getColumns().get(colindex-1).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "13em");
				((ColumnHandle)recordsTable.getColumns().get(colindex-1).getElement().getHandle(rdh.getModule())).setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_LEFT);
			}else if (s.equalsIgnoreCase(EntityRecordDatasetResultSetMetadata.Column.DESCRIPTION.getColumnName())){
				((ColumnHandle)recordsTable.getColumns().get(colindex-1).getElement().getHandle(rdh.getModule())).setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_LEFT);
			}
			colindex++;
		}
		
		/* ----- Locations Map ----- */
		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);

		l = factory.newLabel(null);
		l.setText("Locations");
		l.setStyleName(sectionHeaderStyle.getName());
		l.setProperty(IStyleModel.PAGE_BREAK_BEFORE_PROP, DesignChoiceConstants.PAGE_BREAK_BEFORE_ALWAYS);
		rdh.getBody().add(l);
		
		ExtendedItemHandle map = factory.newExtendedItem(null, SmartMapItem.EXTENSION_NAME);
		map.setWidth("7in");
		map.setHeight("7in");
		map.setProperty( SmartMapItem.SMART_BASEMAP_PROP, SmartMapItem.DEFAULT_BASEMAP_KEY);
		rdh.getBody().add(map);
		
		DataSetHandle layersHandle = datasetHandles.get(EntityLocationDataset.DATASET_TYPE);
		ExtendedItemHandle pointLayer = factory.newExtendedItem(null, LayerItem.EXTENSION_NAME);
		pointLayer.setDataSet(layersHandle);
		pointLayer.setProperty(LayerItem.SMART_LAYERNAME_PROP, layersHandle.getDisplayName());
		pointLayer.setProperty(LayerItem.SMART_LAYERTYPE_PROP, MapLayerInfo.LayerType.POINT.toString());
		pointLayer.setProperty(LayerItem.SMART_GEOMCOLUMN_PROP, "location:geom");
		
		ExtendedItemHandle polyLayer = factory.newExtendedItem(null, LayerItem.EXTENSION_NAME);
		polyLayer.setDataSet(layersHandle);
		polyLayer.setProperty(LayerItem.SMART_LAYERNAME_PROP, layersHandle.getDisplayName());
		polyLayer.setProperty(LayerItem.SMART_LAYERTYPE_PROP, MapLayerInfo.LayerType.POLYGON.toString());
		polyLayer.setProperty(LayerItem.SMART_GEOMCOLUMN_PROP, "location:geom");
		
		PropertyHandle layershandle = map.getPropertyHandle(SmartMapItem.SMART_LAYER_PROP2);
		layershandle.add(pointLayer);
		layershandle.add(polyLayer);
		
		
		/* footer */
		SimpleMasterPageHandle masterHandle = factory.newSimpleMasterPage("MasterPage");
		rdh.getMasterPages().add(masterHandle);
		
		//entity id and date parameters
		GridHandle footerGrid1 = factory.newGridItem(null, 2, 1);
		footerGrid1.setStyleName(footerStyle.getName());
		di = factory.newDataItem(null);
		di.setDataSet(entityDataset);
		footerGrid1.setProperty(IStyleModel.BORDER_TOP_STYLE_PROP, DesignChoiceConstants.LINE_STYLE_SOLID);
		footerGrid1.setProperty(IStyleModel.BORDER_TOP_WIDTH_PROP, "1px");
		masterHandle.getPageFooter().add(footerGrid1);
		
		ComputedColumn c = StructureFactory.createComputedColumn();
		c.setProperty("name", "ID_Date_Time");
		c.setDisplayName("ID_Date_Time");
		c.setDataType("string");
		c.setExpression("dataSetRow[\"ID\"] + \"\\n\" + Formatter.format(params[\"Start Date\"].value, 'MMM dd, YYYY') + \" to \" + Formatter.format(params[\"End Date\"].value, 'MMM dd, YYYY')");
		di.getColumnBindings().addItem(c);
		di.setResultSetColumn("ID_Date_Time");
		footerGrid1.getCell(1, 1).getContent().add(di);
		
		
		GridHandle footerGrid2 = factory.newGridItem(null, 3, 1);
		di = factory.newDataItem(null);
		di.setDataSet(entityDataset);

		AutoTextHandle pageNumber = factory.newAutoText(null);
		pageNumber.setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_RIGHT);
		pageNumber.setAutoTextType(DesignChoiceConstants.AUTO_TEXT_PAGE_NUMBER);
		pageNumber.setProperty(IStyleModel.PADDING_TOP_PROP, "0pt");
		pageNumber.setProperty(IStyleModel.PADDING_BOTTOM_PROP, "0pt");
		pageNumber.setProperty(IStyleModel.PADDING_LEFT_PROP, "0pt");
		pageNumber.setProperty(IStyleModel.PADDING_RIGHT_PROP, "0pt");
		
		LabelHandle of = factory.newLabel(null);
		of.setText("of");
		of.setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_CENTER);
		of.setProperty(IStyleModel.PADDING_TOP_PROP, "0pt");
		of.setProperty(IStyleModel.PADDING_BOTTOM_PROP, "0pt");
		of.setProperty(IStyleModel.PADDING_LEFT_PROP, "0pt");
		of.setProperty(IStyleModel.PADDING_RIGHT_PROP, "0pt");
		
		AutoTextHandle totalPageNumber = factory.newAutoText(null);
		totalPageNumber.setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_LEFT);
		totalPageNumber.setAutoTextType(DesignChoiceConstants.AUTO_TEXT_TOTAL_PAGE);
		totalPageNumber.setProperty(IStyleModel.PADDING_TOP_PROP, "0pt");
		totalPageNumber.setProperty(IStyleModel.PADDING_BOTTOM_PROP, "0pt");
		totalPageNumber.setProperty(IStyleModel.PADDING_LEFT_PROP, "0pt");
		totalPageNumber.setProperty(IStyleModel.PADDING_RIGHT_PROP, "0pt");
		
		footerGrid2.getCell(1, 1).getContent().add(pageNumber);
		footerGrid2.getCell(1, 2).getContent().add(of);
		footerGrid2.getCell(1, 3).getContent().add(totalPageNumber);
		
		footerGrid2.getColumns().get(0).setProperty(ITableColumnModel.WIDTH_PROP, "2em");
		footerGrid2.getColumns().get(1).setProperty(ITableColumnModel.WIDTH_PROP, "2em");
		footerGrid2.getColumns().get(2).setProperty(ITableColumnModel.WIDTH_PROP, "2em");
		
		footerGrid1.getCell(1, 2).getContent().add(footerGrid2);
		footerGrid1.getCell(1, 2).setProperty(IStyleModel.TEXT_ALIGN_PROP, DesignChoiceConstants.TEXT_ALIGN_RIGHT);
	}
}
