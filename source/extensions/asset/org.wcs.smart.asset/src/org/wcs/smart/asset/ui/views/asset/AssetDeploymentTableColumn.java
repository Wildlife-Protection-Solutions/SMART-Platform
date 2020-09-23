/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.asset;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.engine.StatisticsEngine.Statistic;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Asset deployment table columns.
 * 
 * @author Emily
 *
 */
public class AssetDeploymentTableColumn extends ColumnLabelProvider{

	public enum FixedColumn{
		STATION(Messages.AssetDeploymentTableColumn_StationColumnName),
		LOCATION(Messages.AssetDeploymentTableColumn_LocationColumnName),
		START_DATE(Messages.AssetDeploymentTableColumn_StartDateColumnName),
		END_DATE(Messages.AssetDeploymentTableColumn_EndDateColumnName),
		ACTIVE_TIME(Messages.AssetDeploymentTableColumn_ActiveTimeColumnName),
		TOTAL_TIME(Messages.AssetDeploymentTableColumn_TimeColumnName),
		NUM_INCIDENTS(Messages.AssetDeploymentTableColumn_IncCntColumnName);
		
		String guiName;
		
		FixedColumn(String name) {
			this.guiName = name;
		}
	}
	

	private FixedColumn column;
	private AssetAttribute attribute;
	private CoordinateReferenceSystem crs;
	
	public AssetDeploymentTableColumn(FixedColumn col, CoordinateReferenceSystem crs) {
		this.column = col;
		this.crs = crs;
	}
	
	public AssetDeploymentTableColumn(AssetAttribute attribute, CoordinateReferenceSystem crs) {
		this.attribute = attribute;
		this.crs = crs;
	}
	
	public String getColumnName() {
		if (column != null) return column.guiName;
		if (attribute != null) return attribute.getName();
		return ""; //$NON-NLS-1$
		
	}
	
	@Override
	public String getText(Object element) {
		if (! (element instanceof AssetDeploymentWrapper)) return super.getText(element);
		AssetDeployment deployment = ((AssetDeploymentWrapper) element).getDeployment();
		if (column != null) {
			switch(column) {
			case END_DATE:
				if (deployment.getEndDate() == null) return Messages.AssetDeploymentTableColumn_CurrentLabel;
				return deployment.getEndDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
			case LOCATION:
				return deployment.getStationLocation().getId();
			case NUM_INCIDENTS:
				Object x = ((AssetDeploymentWrapper)element).getStatistic(Statistic.NUMBER_INCIDENTS);
				if (x == null) return DialogConstants.LOADING_TEXT;
				return x.toString();
			case START_DATE:
				return deployment.getStartDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
			case STATION:
				return deployment.getStationLocation().getStation().getId();
			case TOTAL_TIME:
				return AssetUtils.formatTime(deployment.getTimeOutInSeconds());
			case ACTIVE_TIME:
				return AssetUtils.formatTime(deployment.getActiveTimeOutInSeconds());
			}
		}else if (attribute != null ) {
			if (deployment.getAttributeValues() != null) {
				for (AssetDeploymentAttributeValue v : deployment.getAttributeValues()) {
					if (v.getAttribute().equals(attribute)) {
						return v.getAttributeValueAsString(Locale.getDefault(), crs);
					}
				}
			}
			return ""; //$NON-NLS-1$
		}
		//should never get here
		return ""; //$NON-NLS-1$
	}
		
	public static List<AssetDeploymentTableColumn> getTableColumns(Asset asset, CoordinateReferenceSystem crs, Session session){
		AssetType type = session.get(AssetType.class, asset.getAssetType().getUuid());
		if (type == null) return Collections.emptyList();
		
		 List<AssetDeploymentTableColumn> columns = new ArrayList<>();
		 for (FixedColumn c : FixedColumn.values()) {
			 columns.add(new AssetDeploymentTableColumn(c, crs));
		 }
		 
		 for(AssetTypeDeploymentAttribute attribute : type.getAssetDeploymentAttributes()) {
			 columns.add(new AssetDeploymentTableColumn(attribute.getAttribute(), crs));
		 }
		 return columns;
	}
}
