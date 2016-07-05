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
package org.wcs.smart.query.compound.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

/**
 * Query item used for displaying display results 
 * in the UI
 * @author Emily
 *
 */
public class QueryItem {

	/**
	 * status of query results
	 * @author Emily
	 *
	 */
	public enum Status{
		UNKNOWN,
		PROCESSING,
		DONE,
		ERROR
	}
	
	private Status status = Status.UNKNOWN;
	
	private CompoundMapQueryLayer layer;
	private Query query;
	private IQueryType type;
	
	private ProgressBar pbar;
	private Label totalWidget;
	private TableEditor tableEditor;

	private Integer totalCnt = null;
	private String errorMessage;
	
	private List<ILayer> layers;
	
	public QueryItem(CompoundMapQueryLayer layer, Query query, IQueryType type){
		this.layer = layer;
		this.query = query;
		this.type = type;
		this.layers = new ArrayList<ILayer>();
	}
	
	public IQueryType getQueryType(){
		return this.type;
	}
	public Query getQuery(){
		return this.query;
	}

	public CompoundMapQueryLayer getCompoundMapQueryLayer(){
		return layer;
	}
	
	public String getQueryName(){
		return query.getName() + " [" + query.getId() + "]";
	}

	public void addLayer(ILayer layer){
		layers.add(layer);
	}
	public void dispose(){
		if (layers != null && !layers.isEmpty()){
			try {
				IService service = layers.get(0).getGeoResource().resolve(IService.class, null);
				CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			DeleteLayersCommand cmd = new DeleteLayersCommand(layers.toArray(new ILayer[layers.size()]));
			layers.get(0).getMap().executeSyncWithoutUndo(cmd);
		}
		if (pbar != null){
			pbar.dispose();
			pbar = null;
		}
		if (totalWidget != null){
			totalWidget.dispose();
			totalWidget = null;		
		}
		if (tableEditor != null){
			tableEditor.getEditor().dispose();
			tableEditor.dispose();
			tableEditor = null;
		}
	}
	
	public void setProgressBar(ProgressBar pbar){
		this.pbar = pbar;
	}
	
	public void setTableEditor(TableEditor tableEditor){
		this.tableEditor = tableEditor;
	}
	public TableEditor getTableEditor(){
		return this.tableEditor;
	}
	public ProgressBar getProgressBar(){
		return this.pbar;
	}
	
	public void setTotalCount(int totalCnt){
		this.totalCnt = totalCnt;
	}
	
	public Integer getTotalCount(){
		return this.totalCnt;
	}
	public Label getTotalWidget() {
		return totalWidget;
	}

	public void setTotalWidget(Label totalWidget) {
		this.totalWidget = totalWidget;
	}
	
	public Status getStatus(){
		return this.status;
	}
	
	public void setStatus(Status status){
		this.status = status;
	}
	
	public void setErrorMessage(String message){
		this.errorMessage = message;
	}
	public String getErrorMessage(){
		return this.errorMessage;
	}
}
