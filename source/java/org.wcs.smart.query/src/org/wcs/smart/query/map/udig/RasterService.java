package org.wcs.smart.query.map.udig;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IServiceInfo;
import net.refractions.udig.catalog.rasterings.AbstractRasterGeoResource;
import net.refractions.udig.catalog.rasterings.AbstractRasterGeoResourceInfo;
import net.refractions.udig.catalog.rasterings.AbstractRasterService;
import net.refractions.udig.catalog.rasterings.AbstractRasterServiceInfo;
import net.refractions.udig.core.internal.CorePlugin;
import net.refractions.udig.style.sld.SLDContent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.XMLMemento;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.geotools.geometry.Envelope2D;
import org.geotools.styling.Style;
import org.wcs.smart.ca.Area;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GridQueryResultMetadata;
import org.wcs.smart.query.model.GriddedQuery;
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
    /** <code>URL_PARAM</code> field */
    public final static String URL_PARAM = "URL"; //$NON-NLS-1$

	private static GeoTiffFormatFactorySpi factory = null;
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
		super(buildUrl(query.getUuid()),"geotiff", getFactory());
		this.query = query;		
		
		//ensure query dir exists
		File dir = QueryPlugIn.getDefault().getQueryTempDirectory();
		if(!dir.exists()){
			dir.mkdir();
		}
		
		//create raster file name
		String fName = null;
		if (query.getUuid() == null){
			fName = String.valueOf( System.nanoTime() );
		}else{
			//ensure filename is unique for each raster service created
			fName = SmartUtils.encodeHex(query.getUuid()) + "_" + String.valueOf( System.nanoTime() );
		}

		StringBuilder pathBuilder = new StringBuilder(50);
		pathBuilder
				.append(dir.getAbsolutePath())
				.append(File.separator)
				.append(fName)
				.append(".tiff");
		this.rasterFile = new File(pathBuilder.toString());
		
	}
	
	/**
	 * Builds the service url
	 * @param queryId
	 * @return
	 */
	private static URL buildUrl(byte[] queryId){
		String url = "smart://smartdb/query/";
		if (queryId == null){
			url += System.nanoTime();
		}else{
			url += SmartUtils.encodeHex(queryId) ;
		}
		try{
			return new URL(null, url, CorePlugin.RELAXED_HANDLER);
		}catch (Exception ex){
			QueryPlugIn.log("Error creating url: " + ex.getMessage(), ex);
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

		rb.setFileName(rasterFile.getCanonicalPath());
		
		GridQueryResultMetadata metadata = query.getResultMetadata();
		
		int width = metadata.getMaxXTile() - metadata.getMinXTile() + 1;
		int height = metadata.getMaxYTile() - metadata.getMinYTile() + 1;
		
		rb.setRasterDimensions(width, height);
			
		// sets the envelope based in the map bound
		double gridCellSize = query.getGridSize();
		rb.setEnvelope(
				new Envelope2D(
					Area.AREA_CRS,//TODO: fix this 
					(metadata.getMinXTile()-1)* gridCellSize + 0.5* gridCellSize + query.getGridOrigin().x, 
					(metadata.getMinYTile()-1) * gridCellSize - 0.5*gridCellSize + query.getGridOrigin().y, 
					width * gridCellSize , height*gridCellSize)); 
		rb.setTable(query.getLastResults(), query.getResultMetadata());
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
	
		final AbstractRasterGeoResource resource = new AbstractRasterGeoResource(
				this, rasterFile.getCanonicalPath()) {
			
			
			private AbstractRasterGeoResourceInfo  info = null;
			
			
			public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor)
					 throws IOException {
							T ret = super.resolve(adaptee, monitor);
							return ret;
			}
			@Override
			protected AbstractRasterGeoResourceInfo createInfo(
					IProgressMonitor monitor) throws IOException {
				if (info == null){
					info = new AbstractRasterGeoResourceInfo(this, "grid analysis", "tif", "tiff"){
						 @Override
						 public String getTitle() {
							 return query.getName() + " [" + query.getId() + "]";
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
				
				String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleEntry type=\"SLDStyle\" version=\"1.0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&lt;sld:StyledLayerDescriptor xmlns=&quot;http://www.opengis.net/sld&quot; xmlns:sld=&quot;http://www.opengis.net/sld&quot; xmlns:ogc=&quot;http://www.opengis.net/ogc&quot; xmlns:gml=&quot;http://www.opengis.net/gml&quot; version=&quot;1.0.0&quot;&gt; &lt;sld:UserLayer&gt; &lt;sld:LayerFeatureConstraints&gt; &lt;sld:FeatureTypeConstraint/&gt; &lt;/sld:LayerFeatureConstraints&gt; &lt;sld:UserStyle&gt; &lt;sld:Name&gt;000051&lt;/sld:Name&gt; &lt;sld:Title/&gt; &lt;sld:FeatureTypeStyle&gt; &lt;sld:Name&gt;name&lt;/sld:Name&gt; &lt;sld:Rule&gt; &lt;sld:RasterSymbolizer&gt; &lt;sld:Geometry&gt; &lt;ogc:PropertyName&gt;grid&lt;/ogc:PropertyName&gt; &lt;/sld:Geometry&gt; &lt;sld:ColorMap&gt; &lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FFECEC&quot; opacity=&quot;1.0&quot; quantity=&quot;"
						+ minValue
						+ "&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FF0000&quot; opacity=&quot;1.0&quot; quantity=&quot;"
						+ maxValue
						+ "&quot;/&gt; &lt;/sld:ColorMap&gt; &lt;/sld:RasterSymbolizer&gt; &lt;/sld:Rule&gt; &lt;/sld:FeatureTypeStyle&gt; &lt;/sld:UserStyle&gt; &lt;/sld:UserLayer&gt;&lt;/sld:StyledLayerDescriptor&gt;</styleEntry>";
				try {
					XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
					SLDContent c = new SLDContent();
	 				Style style = (Style)c.load(memento);
					 return style;
				} catch (Exception ex) {
					QueryPlugIn.displayLog("Could not parse raster style", ex);
					return null;
				}
 				
			 }
		};

		List<AbstractRasterGeoResource> geoResources = new ArrayList<AbstractRasterGeoResource>();
		geoResources.add(resource);
		return geoResources;
	}

	
	/**
     * Finds or creates the Reader used to access this service. Apon any exception, the message
     * field is populated and null is returned.
     * 
     * @return Reader linked to this service.
     */
	@Override
    public synchronized AbstractGridCoverage2DReader getReader(IProgressMonitor monitor) {
        if (this.reader == null) {
        	
        	// create the raster
        	try {
        		this.rasterFile = createRaster();
        	} catch (Exception ex) {
        		QueryPlugIn.displayLog("Error creating gridded query.", ex);
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
		cleanTemporaDirectory();
		
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
		
		this.rasterFile = createRaster();

	}	
	
	
	/**
	 * @see net.refractions.udig.catalog.rasterings.AbstractRasterService#resources(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized List<AbstractRasterGeoResource> resources(IProgressMonitor monitor) {
		if (this.geoResources != null) {
			return this.geoResources;
		}

		try {
			this.geoResources = getGeoResource();
		} catch (Exception e) {
			// TODO:
			e.printStackTrace();
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
    	
		//remove service from catalog
		
		factory = null;
		CatalogPlugin.getDefault().getLocalCatalog().remove(this);
    	
    	if (geoResources != null){
    		int steps = (int) ((double) 99 / (double) geoResources.size());
    		for( IGeoResource resolve : this.geoResources ) {
    			try {
    				SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, steps);
    				resolve.dispose(subProgressMonitor);
    				subProgressMonitor.done();
    			} catch (Throwable e) {
    				QueryPlugIn.log("Could not dispose query Service", e);
    			}
    		}
    		geoResources = null;
    	}
        
        cleanTemporaDirectory();
	}

	/**
	 * Removes all rester files that were created in the temporal directory
	 */
	private void cleanTemporaDirectory() {
		
		File tempDirectory = QueryPlugIn.getDefault().getQueryTempDirectory();
		if(tempDirectory.exists()){
			if(this.rasterFile != null && this.rasterFile.exists()){
				final String[] fullName = this.rasterFile.getName().split("[.]");				
				final String namePrefix = fullName[0];

				// remove the tiff file
				if( !this.rasterFile.delete()){
					//TODO: this is a serious problem as we won't be
					//able to regenerate this file
					QueryPlugIn.log("cannot delete the file " + this.rasterFile.toString() , null);
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
						QueryPlugIn.log("cannot delete the file.  Should delete on shutdown " + f.toString(), null);
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
			monitor.beginTask("Creating Service Info", 2);
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
