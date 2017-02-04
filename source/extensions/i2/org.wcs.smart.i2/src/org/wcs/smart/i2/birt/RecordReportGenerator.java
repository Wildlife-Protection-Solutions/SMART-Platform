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
package org.wcs.smart.i2.birt;

import java.nio.file.Path;
import java.text.MessageFormat;
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
import org.eclipse.birt.report.model.api.TextDataHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.ComputedColumn;
import org.eclipse.birt.report.model.api.elements.structures.HideRule;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.interfaces.IImageItemModel;
import org.eclipse.birt.report.model.elements.interfaces.IReportItemModel;
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
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.birt.datasource.IConnectionFactory;
import org.wcs.smart.i2.birt.datasource.IntelBirtDataSource;
import org.wcs.smart.i2.birt.record.RecordAttributeDataset;
import org.wcs.smart.i2.birt.record.RecordAttributeDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.RecordDataset;
import org.wcs.smart.i2.birt.record.RecordDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDataset;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDataset;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDatasetResultSetMetadata;
import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationDatasetResultSetMetadata;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.birt.map.item.SmartMapItem;

/**
 * Generates the default record birt export template.
 * 
 * @author Emily
 *
 */
public enum RecordReportGenerator {
	INSTANCE;
	
	@SuppressWarnings("unchecked")
	public void generateReport(Path file) throws Exception{
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
		ReportDesignHandle rdh = session.createDesign(file.toFile().getAbsolutePath());
		ModelOdaAdapter modelAdapter = new ModelOdaAdapter();
		try {
			rdh.setTitle("Record Template");
		} catch (SemanticException e) {
			//lets just consume this 
		}
		
		//add parameters - entity types and dates
		ScalarParameterHandle shandler = rdh.getElementFactory().newScalarParameter(DataSourceParameter.RECORD_UUID.getName());
		shandler.setValueType(DesignChoiceConstants.PARAM_VALUE_TYPE_STATIC);
		shandler.setIsRequired(true);
		shandler.setDataType(DesignChoiceConstants.PARAM_TYPE_STRING);
		shandler.setDistinct(true);
		rdh.getParameters().add(shandler);
		
		//add intelligence data source
		String dataSourceName = "Intelligence Data Source";
		OdaDataSourceHandle datasource = rdh.getElementFactory().newOdaDataSource(dataSourceName, IntelBirtDataSource.ODA_DATA_SOURCE_ID);
		rdh.getDataSources().add(datasource);
		DataSourceDesign dSource = modelAdapter.createDataSourceDesign(datasource);
		
		//add datasets
		String[] datasets = new String[]{
				 RecordDataset.DATASET_TYPE,
				 RecordAttributeDataset.DATASET_TYPE,
				 RecordEntityDataset.DATASET_TYPE,
				 RecordAttachmentDataset.DATASET_TYPE,
				 RecordLocationDataset.DATASET_TYPE
		};
		
		HashMap<String, OdaDataSetHandle> datasetHandles = new HashMap<>();
		for (String d : datasets){
			DataSetDesign dsDesign = DesignFactory.eINSTANCE.createDataSetDesign();
			dsDesign.setDataSourceDesign(dSource);
			
			String dsName = IntelReportManager.INSTANCE.getName(null, d);
			dsDesign.setName(dsName);
			dsDesign.setDisplayName(dsName);
			dsDesign.setQueryText("");
			dsDesign.setOdaExtensionDataSetId(d);
			
			AbstractIntelBirtConnection connection  = SmartContext.INSTANCE.getClass(IConnectionFactory.class).createConnection();
			connection.open(null);
			try{
				IQuery query = connection.newQuery(d);
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
				if (parameter.getName().equals(DataSourceParameter.RECORD_UUID.getName())) {
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
		

		OdaDataSetHandle recordDataset = datasetHandles.get(RecordDataset.DATASET_TYPE);
		
		/* Title */
		DataItemHandle headerId = factory.newDataItem(null);		
		headerId.setDataSet(recordDataset);
		ComputedColumn cc = StructureFactory.createComputedColumn();
		cc.setProperty("name", RecordDatasetResultSetMetadata.Column.TITLE.getColumnName());
		cc.setDisplayName(RecordDatasetResultSetMetadata.Column.TITLE.getColumnName());
		cc.setDataType("string");
		cc.setExpression("dataSetRow[\"" + RecordDatasetResultSetMetadata.Column.TITLE.getColumnName() +"\"]");
		headerId.getColumnBindings().addItem(cc);
		headerId.setResultSetColumn(RecordDatasetResultSetMetadata.Column.TITLE.getColumnName());
		headerId.setStyleName(headerStyle.getName());
		rdh.getBody().add(headerId);
		
		/* Details */
		GridHandle headerGrid = factory.newGridItem(null, 2, 4);
		rdh.getBody().add(headerGrid);
		headerGrid.setDataSet(recordDataset);
		headerGrid.setStyleName(tableStyle.getName());
		((ColumnHandle)headerGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "10em");
		((ColumnHandle)headerGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(IStyleModel.VERTICAL_ALIGN_PROP, DesignChoiceConstants.VERTICAL_ALIGN_MIDDLE);
		
		List<ComputedColumn> entityColumns = DataUtil.generateComputedColumns(headerGrid);
		for (ComputedColumn c : entityColumns){
			headerGrid.getColumnBindings().addItem(c);
		}
		
		cc = StructureFactory.createComputedColumn();
		cc.setProperty("name", "CreatedDetails");
		cc.setDisplayName("CreatedDetails");
		cc.setDataType("string");
		cc.setExpression("dataSetRow[\"" + RecordDatasetResultSetMetadata.Column.CREATED.getColumnName() + "\"] + \" (\" + dataSetRow[\"" + RecordDatasetResultSetMetadata.Column.CREATED_BY.getColumnName() + "\"] + \")\"");
		headerGrid.getColumnBindings().addItem(cc);
		
		cc = StructureFactory.createComputedColumn();
		cc.setProperty("name", "ModifiedDetails");
		cc.setDisplayName("ModifiedDetails");
		cc.setDataType("string");
		cc.setExpression("dataSetRow[\"" + RecordDatasetResultSetMetadata.Column.LAST_MODIFIED.getColumnName() + "\"] + \" (\" + dataSetRow[\"" + RecordDatasetResultSetMetadata.Column.LAST_MODIFIED_BY.getColumnName() + "\"] + \")\"");
		headerGrid.getColumnBindings().addItem(cc);
		
		//status
		int row = 1;
		LabelHandle l = factory.newLabel(null);
		l.setText(MessageFormat.format("{0}:",RecordDatasetResultSetMetadata.Column.STATUS.getColumnName()));
		headerGrid.getCell(row,1).getContent().add(l);
		
		
		DataItemHandle di = factory.newDataItem(null);
		di.setResultSetColumn(RecordDatasetResultSetMetadata.Column.STATUS.getColumnName());
		headerGrid.getCell(row,2).getContent().add(di);
		row++;
		
		//created details
		l = factory.newLabel(null);
		l.setText("Created:");
		headerGrid.getCell(row,1).getContent().add(l);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn("CreatedDetails");
		headerGrid.getCell(row,2).getContent().add(di);		
		row++;
		
		//modified details
		l = factory.newLabel(null);
		l.setText("Modified:");
		headerGrid.getCell(row,1).getContent().add(l);
				
		di = factory.newDataItem(null);
		di.setResultSetColumn("ModifiedDetails");
		headerGrid.getCell(row,2).getContent().add(di);
		row++;
		
		l = factory.newLabel(null);
		l.setText("Source:");
		headerGrid.getCell(row,1).getContent().add(l);
		
		GridHandle srcGrid = factory.newGridItem(null, 2, 1);
		srcGrid.setStyleName(tableStyle.getName());
		((ColumnHandle)srcGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "20px");
		
		
		ImageHandle mi = factory.newImage(null);
		mi.setProperty(IReportItemModel.HEIGHT_PROP, "16px");
		mi.setProperty(IReportItemModel.WIDTH_PROP, "16px");
		mi.setDataSet(recordDataset);
		mi.setProperty(IImageItemModel.SOURCE_PROP, "expr");
		mi.setProperty(IImageItemModel.VALUE_EXPR_PROP, "row[\"" + RecordDatasetResultSetMetadata.Column.SOURCE_ICON.getColumnName() + "\"]");
		srcGrid.getCell(1,1).getContent().add(mi);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordDatasetResultSetMetadata.Column.SOURCE.getColumnName());
		srcGrid.getCell(1,2).getContent().add(di);
		
		headerGrid.getCell(row,2).getContent().add(srcGrid);
		row++;
		
		
		/* Record Attributes */
		OdaDataSetHandle attributeDataset = datasetHandles.get(RecordAttributeDataset.DATASET_TYPE);
		TableHandle attributeTable = factory.newTableItem(null, 2);
		rdh.getBody().add(attributeTable);
		attributeTable.setDataSet(attributeDataset);
		attributeTable.setStyleName(tableStyle.getName());
		((ColumnHandle)attributeTable.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "10em");
		List<ComputedColumn> computedColumns = DataUtil.generateComputedColumns(attributeTable);
		for (ComputedColumn c : computedColumns){
			attributeTable.getColumnBindings().addItem(c);
		}
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordAttributeDatasetResultSetMetadata.Column.ATTRIBUTE.getColumnName());
		attributeTable.getCell(attributeTable.getDetail().getSlotID(), -1, 1, 1).getContent().add(di);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordAttributeDatasetResultSetMetadata.Column.TEXT.getColumnName());
		attributeTable.getCell(attributeTable.getDetail().getSlotID(), -1, 1, 2).getContent().add(di);
		
		/* Narrative Scratchpad */
		
		GridHandle narrativeGrid = factory.newGridItem(null, 2, 2);
		rdh.getBody().add(narrativeGrid);
		narrativeGrid.setDataSet(recordDataset);
		narrativeGrid.setStyleName(tableStyle.getName());
		((ColumnHandle)narrativeGrid.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, "10em");
		
		computedColumns = DataUtil.generateComputedColumns(narrativeGrid);
		for (ComputedColumn c : computedColumns){
			narrativeGrid.getColumnBindings().addItem(c);
		}
		
		row = 1;
		l = factory.newLabel(null);
		l.setText("Narrative:");
		narrativeGrid.getCell(row,1).getContent().add(l);
				
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordDatasetResultSetMetadata.Column.DESCRIPTION.getColumnName());
		narrativeGrid.getCell(row,2).getContent().add(di);	
		row++;
		
		//modified details
		l = factory.newLabel(null);
		l.setText("Scratchpad:");
		narrativeGrid.getCell(row,1).getContent().add(l);
				
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordDatasetResultSetMetadata.Column.SCRATCHPAD.getColumnName());
		narrativeGrid.getCell(row,2).getContent().add(di);	
		row++;
		
		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
	
		
		
		
		/* Entity Links */
		l = factory.newLabel(null);
		l.setText("Entities");
		l.setStyleName(sectionHeaderStyle.getName());
		rdh.getBody().add(l);
		
		OdaDataSetHandle entityDataset = datasetHandles.get(RecordEntityDataset.DATASET_TYPE);
		TableHandle entityTable = factory.newTableItem(null, 2);
		rdh.getBody().add(entityTable);
		entityTable.setDataSet(entityDataset);
		entityTable.setStyleName(tableStyle.getName());
		((ColumnHandle)entityTable.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, ".7in");
		
		computedColumns = DataUtil.generateComputedColumns(entityTable);
		for (ComputedColumn c : computedColumns){
			entityTable.getColumnBindings().addItem(c);
		}
		
		ImageHandle attachImage = factory.newImage(null);
		attachImage.setWidth(".5in");
		attachImage.setHeight(".5in");
		attachImage.setSource(DesignChoiceConstants.IMAGE_REF_TYPE_URL);
		attachImage.setProportionalScale(true);
		attachImage.setURL("row[\"" + RecordEntityDatasetResultSetMetadata.Column.ENTITY_IMAGE.getColumnName() + "\"]");
		/* hide if no image */
		HideRule visibility = StructureFactory.createHideRule();
		visibility.setFormat(DesignChoiceConstants.FORMAT_TYPE_ALL);
		visibility.setExpression("row[\""+ RecordEntityDatasetResultSetMetadata.Column.ENTITY_IMAGE.getColumnName() +"\"] == null");
		attachImage.getPropertyHandle(IReportItemModel.VISIBILITY_PROP).addItem(visibility);
		
		entityTable.getCell(entityTable.getDetail().getSlotID(), -1, 1, 1).getContent().add(attachImage);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordEntityDatasetResultSetMetadata.Column.ENTITY_ID.getColumnName());
		entityTable.getCell(entityTable.getDetail().getSlotID(), -1, 1, 2).getContent().add(di);
		

		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
		
		/* Attachments */
		l = factory.newLabel(null);
		l.setText("Attachments");
		l.setStyleName(sectionHeaderStyle.getName());
		rdh.getBody().add(l);
		
		OdaDataSetHandle attachmentDataset = datasetHandles.get(RecordAttachmentDataset.DATASET_TYPE);
		TableHandle attachmentTable = factory.newTableItem(null, 2);
		rdh.getBody().add(attachmentTable);
		attachmentTable.setDataSet(attachmentDataset);
		attachmentTable.setStyleName(tableStyle.getName());
		((ColumnHandle)attachmentTable.getColumns().get(0).getElement().getHandle(rdh.getModule())).setProperty(ITableColumnModel.WIDTH_PROP, ".7in");
		
		computedColumns = DataUtil.generateComputedColumns(attachmentTable);
		for (ComputedColumn c : computedColumns){
			attachmentTable.getColumnBindings().addItem(c);
		}
		
		attachImage = factory.newImage(null);
		attachImage.setWidth(".5in");
		attachImage.setHeight(".5in");
		attachImage.setSource(DesignChoiceConstants.IMAGE_REF_TYPE_URL);
		attachImage.setProportionalScale(true);
		attachImage.setURL("row[\"" + RecordAttachmentDatasetResultSetMetadata.Column.PATH.getColumnName() + "\"]");
		attachImage.getPropertyHandle(IReportItemModel.VISIBILITY_PROP).addItem(visibility);
		
		attachmentTable.getCell(entityTable.getDetail().getSlotID(), -1, 1, 1).getContent().add(attachImage);
		
		di = factory.newDataItem(null);
		di.setResultSetColumn(RecordAttachmentDatasetResultSetMetadata.Column.FILE_NAME.getColumnName());
		attachmentTable.getCell(entityTable.getDetail().getSlotID(), -1, 1, 2).getContent().add(di);
		

		//spacer
		l = factory.newLabel(null);
		rdh.getBody().add(l);
		
		/* ----- Locations Map ----- */
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
		
		DataSetHandle layersHandle = datasetHandles.get(RecordLocationDataset.DATASET_TYPE);
		ExtendedItemHandle pointLayer = factory.newExtendedItem(null, LayerItem.EXTENSION_NAME);
		pointLayer.setDataSet(layersHandle);
		pointLayer.setProperty(LayerItem.SMART_LAYERNAME_PROP, layersHandle.getDisplayName());
		pointLayer.setProperty(LayerItem.SMART_LAYERTYPE_PROP, MapLayerInfo.LayerType.POINT.toString());
		pointLayer.setProperty(LayerItem.SMART_GEOMCOLUMN_PROP, RecordLocationDatasetResultSetMetadata.Column.GEOM.getId());
		
		ExtendedItemHandle polyLayer = factory.newExtendedItem(null, LayerItem.EXTENSION_NAME);
		polyLayer.setDataSet(layersHandle);
		polyLayer.setProperty(LayerItem.SMART_LAYERNAME_PROP, layersHandle.getDisplayName());
		polyLayer.setProperty(LayerItem.SMART_LAYERTYPE_PROP, MapLayerInfo.LayerType.POLYGON.toString());
		polyLayer.setProperty(LayerItem.SMART_GEOMCOLUMN_PROP, RecordLocationDatasetResultSetMetadata.Column.GEOM.getId());
		
		PropertyHandle layershandle = map.getPropertyHandle(SmartMapItem.SMART_LAYER_PROP2);
		layershandle.add(pointLayer);
		layershandle.add(polyLayer);
		
		
		/* footer */
		SimpleMasterPageHandle masterHandle = factory.newSimpleMasterPage("MasterPage");
		rdh.getMasterPages().add(masterHandle);
		
		//entity id and date parameters
		GridHandle footerGrid1 = factory.newGridItem(null, 2, 1);
		footerGrid1.setStyleName(footerStyle.getName());
		footerGrid1.setProperty(IStyleModel.BORDER_TOP_STYLE_PROP, DesignChoiceConstants.LINE_STYLE_SOLID);
		footerGrid1.setProperty(IStyleModel.BORDER_TOP_WIDTH_PROP, "1px");
		masterHandle.getPageFooter().add(footerGrid1);
		
		di = factory.newDataItem(null);
		cc = StructureFactory.createComputedColumn();
		cc.setProperty("name", RecordDatasetResultSetMetadata.Column.TITLE.getColumnName());
		cc.setDisplayName(RecordDatasetResultSetMetadata.Column.TITLE.getColumnName());
		cc.setDataType("string");
		cc.setExpression("dataSetRow[\"" + RecordDatasetResultSetMetadata.Column.TITLE.getColumnName() +"\"]");
		di.getColumnBindings().addItem(cc);
		di.setDataSet(recordDataset);
		di.setResultSetColumn(RecordDatasetResultSetMetadata.Column.TITLE.getColumnName());
		footerGrid1.getCell(1, 1).getContent().add(di);
		
		TextDataHandle ti = factory.newTextData(null);
		ti.setValueExpr("(new Date()).toDateString() + \" \" + (new Date()).toLocaleTimeString()");
		footerGrid1.getCell(1, 1).getContent().add(ti);
		
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
		rdh.save();
		rdh.close();
	}
	
}
