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
package org.wcs.smart.query.common.model.udig;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IServiceInfo;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResourceInfo;
import org.locationtech.udig.catalog.rasterings.AbstractRasterService;
import org.locationtech.udig.catalog.rasterings.AbstractRasterServiceInfo;
import org.locationtech.udig.core.internal.CorePlugin;
import org.locationtech.udig.style.sld.SLDContent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.XMLMemento;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.geotools.geometry.Envelope2D;
import org.geotools.styling.Style;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.GridQueryResultMetadata;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.util.SmartUtils;

/**
 * This service maintains the raster file associated to the query result.
 * <p>
 * It is responsible to create the raster file, when the raster service is created, 
 * and delete it when the service is disposed. 
 * <p>
 * 
 * @author Mauricio Pazos
 * @author Emily
 *
 */
public class RasterService extends AbstractRasterService {

	public static final String GRIDDED_TYPE = "Gridded";  //$NON-NLS-1$
	
	/** <code>URL_PARAM</code> field */
    public final static String URL_PARAM = "URL"; //$NON-NLS-1$

	private static GeoTiffFormatFactorySpi factory = null;
	
	private String rasterFileName;
	private File rasterFile;
	private List<AbstractRasterGeoResource> geoResources = null;
	private GriddedQuery query = null;

	/**
	 * New instance of {@link RasterService}
	 * 
	 * @param queryResults
	 * @throws IOException 
	 * @throws RasterServiceException 
	 */
	public RasterService(final GriddedQuery query){
		super(buildUrl(query.getUuid()),"geotiff", getFactory()); //$NON-NLS-1$
		this.query = query;
		rasterFileName = query.getRasterFileName().getAbsolutePath();
	}
	
	/**
	 * Builds the service url
	 * @param queryId
	 * @return
	 */
	private static URL buildUrl(byte[] queryId){
		String url = "smart://smartdb/query/"; //$NON-NLS-1$
		if (queryId == null){
			url += System.nanoTime();
		}else{
			url += SmartUtils.encodeHex(queryId) ;
		}
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Exception ex){
			QueryPlugIn.log(Messages.RasterService_urlError + ex.getLocalizedMessage(), ex);
			return null;
		}
	}
	
	
	/**
     * Finds or creates a GeoTiffFormatFactorySpi.
     * 
     * @return Default GeoTiffFormatFactorySpi
     */
    public synchronized static GeoTiffFormatFactorySpi getFactory() {
        if (factory == null) {
            factory = new GeoTiffFormatFactorySpi();
        }
        return factory;
    }

    /**
     * Disposes of GeoTiffFormatFactorySpi
     */
    private synchronized static void disposeFactory(){
    	factory = null;
    }
    
	/**
	 * Creates a {@link File} for the raster associated to the query result.
	 * 
	 * @param queryResults
	 * 
	 * @return {@link File} which contains the generated raster
	 * @throws RasterServiceException 
	 * @throws IOException 
	 */
	private File createRaster() throws Exception {
		
		RasterBuilder rb = new RasterBuilder();
		rb.setFileName(rasterFileName);
		
		if (query.getDateFilter() == null){
			//no date filter; build an empty raster -> we need this for styling
			rb.setRasterDimensions(1,1);
				
			// sets the envelope based in the map bound
			rb.setEnvelope(new Envelope2D(SmartDB.DATABASE_CRS,0,0,1,1));
			GridResultItem gi = new GridResultItem();
			gi.setTileX(0);
			gi.setTileY(0);
			gi.setValue(0);
			
			GridQueryResultMetadata md = new GridQueryResultMetadata(0,0,0,0,0,0);
			rb.setTable(Collections.singletonList(gi),md);
			rb.setGridCellSize(1);
			rb.build();
			return rb.getResult();	
		}
		
		GridQueryResultMetadata metadata = query.getResultMetadata();
		if (metadata == null){
			//query has not been run
			return null;
		}
		long width = metadata.getMaxXTile() - metadata.getMinXTile() + 1;
		long height = metadata.getMaxYTile() - metadata.getMinYTile() + 1;
		if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE || width * height > Grid.MAX_GRID_CELLS){
			throw Grid.GRID_TO_BIG_EXCEPTION;
		}
		rb.setRasterDimensions((int)width, (int)height);
			
		// sets the envelope based in the map bound
		double gridCellSize = query.getGridSize();
		rb.setEnvelope(
				new Envelope2D(
					query.getCoordinateReferenceSystem(), 
					(metadata.getMinXTile()-1)* gridCellSize + 0.5* gridCellSize + query.getGridOrigin().x, 
					(metadata.getMinYTile()-1) * gridCellSize - 0.5*gridCellSize + query.getGridOrigin().y, 
					width * gridCellSize , height*gridCellSize)); 
		rb.setTable((Collection<GridResultItem>) query.getCachedResults(new NullProgressMonitor()), query.getResultMetadata());
		rb.setGridCellSize(gridCellSize);
		rb.build();
		
		return rb.getResult();
	}

	
	/**
	 * Returns the {@link IGeoResource} list associated to the generated raster.
	 *  
	 * @return list of {@link IGeoResource}  
	 * @throws RasterServiceException 
	 */
	private List<AbstractRasterGeoResource> getGeoResource() throws Exception {
		AbstractRasterGeoResource resource = null;
		resource = createGeoResource(this.rasterFileName);
		return Collections.singletonList(resource);
	}

	private AbstractRasterGeoResource createGeoResource(String fileName) throws IOException{
		return new AbstractRasterGeoResource(this, fileName) {
			
			private AbstractRasterGeoResourceInfo  info = null;
			
			
			@Override
			protected AbstractRasterGeoResourceInfo createInfo(
					IProgressMonitor monitor) throws IOException {
				if (info == null){
					info = new AbstractRasterGeoResourceInfo(this, Messages.RasterService_keyword1, Messages.RasterService_keywork2, Messages.RasterService_keyword3){
						 @Override
						 public String getTitle() {
							 return query.getName() + " [" + query.getId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
						 }
					};
				}
				return info;
			}

			 @Override
			 public Style style( IProgressMonitor monitor ) {
				double minValue = 0;
				double maxValue = 10;
				if (query.getResultMetadata() != null){
					 minValue = query.getResultMetadata().getMinResultValue();
					 maxValue = query.getResultMetadata().getMaxResultValue();
				}
				
				
				String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //$NON-NLS-1$
						+ "<styleEntry type=\"SLDStyle\" version=\"1.0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;" //$NON-NLS-1$
						+ "&lt;sld:StyledLayerDescriptor xmlns=&quot;http://www.opengis.net/sld&quot; xmlns:sld=&quot;http://www.opengis.net/sld&quot; xmlns:ogc=&quot;http://www.opengis.net/ogc&quot; xmlns:gml=&quot;http://www.opengis.net/gml&quot; version=&quot;1.0.0&quot;&gt;" //$NON-NLS-1$
						+ "&lt;sld:UserLayer&gt; " //$NON-NLS-1$
						+ "&lt;sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
						+ "&lt;sld:FeatureTypeConstraint/&gt; " //$NON-NLS-1$
						+ "&lt;/sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
						+ "&lt;sld:UserStyle&gt; " //$NON-NLS-1$
						+ "&lt;sld:Name&gt;000051&lt;/sld:Name&gt; " //$NON-NLS-1$
						+ "&lt;sld:Title/&gt; &lt;sld:FeatureTypeStyle&gt; " //$NON-NLS-1$
						+ "&lt;sld:Name&gt;name&lt;/sld:Name&gt; &lt;sld:Rule&gt; " //$NON-NLS-1$
						+ "&lt;sld:RasterSymbolizer&gt; &lt;sld:Geometry&gt; " //$NON-NLS-1$
						+ "&lt;ogc:PropertyName&gt;grid&lt;/ogc:PropertyName&gt; " //$NON-NLS-1$
						+ "&lt;/sld:Geometry&gt; &lt;sld:ColorMap&gt; " //$NON-NLS-1$
						+ "&lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot; label=&quot;-no data-&quot;/&gt; "; //$NON-NLS-1$
//				if (minValue == 0){
//						sld +=
//						  "&lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;1.0&quot; quantity=&quot;-9998&quot;/&gt; " //$NON-NLS-1$
//						+ "&lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;1.0&quot; quantity=&quot;0&quot;/&gt; " //$NON-NLS-1$
//						+ "&lt;sld:ColorMapEntry color=&quot;#FFECEC&quot; opacity=&quot;1.0&quot; quantity=&quot;0.0000001"; //$NON-NLS-1$
//				}else{
					sld +="&lt;sld:ColorMapEntry color=&quot;#FFECEC&quot; opacity=&quot;1.0&quot; quantity=&quot;" + minValue; //$NON-NLS-1$
//				}
				sld += "&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FF0000&quot; opacity=&quot;1.0&quot; quantity=&quot;" //$NON-NLS-1$
						+ maxValue
						+ "&quot;/&gt; &lt;/sld:ColorMap&gt; &lt;/sld:RasterSymbolizer&gt; &lt;/sld:Rule&gt; &lt;/sld:FeatureTypeStyle&gt; &lt;/sld:UserStyle&gt; &lt;/sld:UserLayer&gt;&lt;/sld:StyledLayerDescriptor&gt;</styleEntry>"; //$NON-NLS-1$
				try {
					XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
					SLDContent c = new SLDContent();
	 				Style style = (Style)c.load(memento);
					 return style;
				} catch (Exception ex) {
					QueryPlugIn.displayLog(Messages.RasterService_InvalidRasterStyle, ex);
					return null;
				}
 				
			 }

			 @Override
			 public <T> boolean canResolve(Class<T> adaptee) {
				if (adaptee == null) return false;
			
				return adaptee.isAssignableFrom(RasterService.class) || 
							super.canResolve(adaptee);
			}
			 
			 @Override
			 public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor) throws IOException {
			 	if (adaptee == null) return null;
			 	
			 	if (adaptee.isAssignableFrom(RasterService.class)){
			 		return adaptee.cast(RasterService.this);
			 	}
			 	return super.resolve(adaptee, monitor);
			 }
		};
	}
	
	/**
     * Finds or creates the Reader used to access this service. Upon any exception, the message
     * field is populated and null is returned.
     * 
     * @return Reader linked to this service.
     */
	@Override
    public synchronized AbstractGridCoverage2DReader getReader(IProgressMonitor monitor) {
		this.message = null;
        if (this.reader == null) {
        	// create the raster
        	try {
        		this.rasterFile = createRaster();
        	} catch (Exception ex) {
        		this.message = ex;
//        		String message = "Could create map raster layer. \n\n" + ex.getMessage();
//        		QueryPlugIn.displayLog(message, ex);
        		return null;
        	}
        	if (this.rasterFile == null){
        		this.message = new Exception(Messages.RasterService_QueryNotRunErr);
        		return null;
        	}
        	
            try {
                AbstractGridFormat frmt = (AbstractGridFormat) getFormat();
                this.reader = (AbstractGridCoverage2DReader) frmt.getReader(this.rasterFile);
                return this.reader;
	            
            } catch (Exception ex) {
                this.message = ex;
            }
        }
        return this.reader;
    }
	
	
	
	/**
	 * Refreshes 
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(IProgressMonitor monitor) throws Exception{
		//clean up old files
		cleanTemporaryDirectory();
		
		//remove any services
		ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
		catalog.remove(this);
		if (geoResources != null){
			for (AbstractRasterGeoResource r: geoResources){
				r.dispose(monitor);
			}
		}
		geoResources = null;
		this.info = null;
		if (this.reader != null){
			this.reader.dispose();
			this.reader = null;
		}
	}	
	
	
	/**
	 * @see org.locationtech.udig.catalog.rasterings.AbstractRasterService#resources(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized List<AbstractRasterGeoResource> resources(IProgressMonitor monitor) {
		if (this.geoResources != null) {
			return this.geoResources;
		}

		try {
			this.geoResources = getGeoResource();
		} catch (Exception e) {
			QueryPlugIn.log("Error loading resources", e); //$NON-NLS-1$
		}
		if (this.geoResources == null) {
			return Collections.emptyList();
		}
		return this.geoResources;
	}


	public void dispose(IProgressMonitor monitor) {
		if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
		super.dispose(monitor);
    	
		if (this.reader != null){
			this.reader.dispose();
			this.reader = null;
		}
    	
		//remove service from catalog		
		disposeFactory();
		CatalogPlugin.getDefault().getLocalCatalog().remove(this);
    	
    	if (geoResources != null){
    		int steps = (int) ((double) 99 / (double) geoResources.size());
    		for( IGeoResource resolve : this.geoResources ) {
    			try {
    				SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
    				resolve.dispose(subProgressMonitor);
    				subProgressMonitor.done();
    			} catch (Throwable e) {
    				QueryPlugIn.log("Could not dispose query service.", e); //$NON-NLS-1$
    			}
    		}
    		geoResources = null;
    	}
        
        cleanTemporaryDirectory();
	}

	/**
	 * Removes all rester files that were created in the temporal directory
	 */
	private void cleanTemporaryDirectory() {
		
		File tempDirectory = QueryPlugIn.getDefault().getQueryTempDirectory();
		if(tempDirectory.exists()){
			if(this.rasterFile != null && this.rasterFile.exists()){
				final String[] fullName = this.rasterFile.getName().split("[.]");				 //$NON-NLS-1$
				final String namePrefix = fullName[0];

				// remove the tiff file
				if( !this.rasterFile.delete()){
					//this is a serious problem as we won't be
					//able to regenerate this file
					QueryPlugIn.log("cannot delete the file " + this.rasterFile.toString() , null); //$NON-NLS-1$
				}

				String[] toDelete = tempDirectory.list(new FilenameFilter() {					
					@Override
					public boolean accept(File dir, String name) {
						int index = name.lastIndexOf('.');
						if (index < 0){
							return false;
						}
						String subpart = name.substring(0, index);
						return (subpart.equals(namePrefix));
					}
				});
				for (int i = 0; i < toDelete.length; i ++){
					File f = new File(tempDirectory, toDelete[i]);
					if( !f.delete()){
						QueryPlugIn.log("cannot delete the file.  Should delete on shutdown " + f.toString(), null); //$NON-NLS-1$
					}
				}
				
			}
		}
	}


	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			monitor.beginTask(Messages.RasterService_Progress_CreatingInfo, 2);
			monitor.worked(1);
			return new AbstractRasterServiceInfo(this, "geotiff", "tiff", "tif"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		} finally {
			monitor.done();
		}
		
	}

	@Override
	public Map<String, Serializable> getConnectionParams() {

		if (super.getID() != null) {
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(URL_PARAM, super.getID());
			return params;
		}
		return null;

	}

}
