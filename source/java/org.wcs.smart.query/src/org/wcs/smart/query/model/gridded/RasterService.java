package org.wcs.smart.query.model.gridded;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.ID;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.core.internal.CorePlugin;
import net.refractions.udig.project.IMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryResultItem;



/**
 * This service maintains the raster file associated to the query result.
 * <p>
 * It is responsible to create the raster file, when the raster service is created, 
 * and delete it when the service is disposed. 
 * <p>
 * 
 * @author Mauricio Pazos
 *
 */
public class RasterService {

	private static final String TMP_DIRECTORY = "tmp";
	private final File raster;
	private File tempDirectory;
	private List<IGeoResource>  geoResources = null;
	private IMap map;
	private List<IService> services;

	/**
	 * New instance of {@link RasterService}
	 * 
	 * @param queryResults
	 * @throws IOException 
	 * @throws RasterServiceException 
	 */
	public RasterService(final IMap map, final String rasterFileName, final List<QueryResultItem> queryResults) throws RasterServiceException, IOException {
		
		assert map != null;
		assert rasterFileName != null;
		assert queryResults != null;
		
		this.map = map;
		this.raster = createRaster(rasterFileName, queryResults);
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
	private File createRaster(final String rasterFileName, final List<QueryResultItem> queryResults) throws RasterServiceException, IOException {

		RasterBuilder rb = new RasterBuilder();

		this.tempDirectory = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), TMP_DIRECTORY);
		if(!tempDirectory.exists()){
				tempDirectory.mkdir();
		}
		StringBuilder pathBuilder = new StringBuilder(50);
		pathBuilder
				.append(tempDirectory.getCanonicalPath())
				.append(File.separator)
				.append(rasterFileName)
				.append(".tiff");

		rb.setFileName(pathBuilder.toString());

		ReferencedEnvelope bounds = this.map.getBounds(null);

			// gets the raster dimensions from bound's CRS 
		final int width = (int) Math.round(bounds.getWidth() +1);
		final int height = (int) Math.round(bounds.getHeight() +1);
		rb.setRasterDimensions(width, height);
			
			// sets the envelope based in the map bound
		rb.setEnvelope(
					new Envelope2D(
							bounds.getCoordinateReferenceSystem(), 
							bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())); 

			// set the query result in the raster builder
		List<Map<String, Object>> table = new LinkedList<Map<String, Object>>(); 
		Point gridSize = new Point(1, 1); // 1 degree TODO should be taken from GriddedValuePanel class or its model
		for (QueryResultItem item : queryResults) {

			Map<String, Object> row = new HashMap<String, Object>(3);

			// computes the raster x,y coord based on the top left bounds' coordenates (MinX, MaxY)
			Point rasterCoords = RasterCoordsCalculator.compute((float)item.getTileX(), (float)item.getTileY(), bounds);

			int tileX = rasterCoords.x * gridSize.x;
			row.put("x", tileX);

			int tileY = rasterCoords.y * gridSize.y;
			row.put("y", tileY);

			row.put("value", item.getValue());
			table.add(row);
		}
			
		rb.setTable(table);

		rb.build();

		return rb.getResult();

	}

	/**
	 * Returns the {@link IGeoResource} list associated to the generated raster.
	 *  
	 * @return list of {@link IGeoResource}  
	 * @throws RasterServiceException 
	 */
	public List<IGeoResource> getGeoResource() throws RasterServiceException {

	    	NullProgressMonitor monitor = new NullProgressMonitor();
	    	
	    	URI uri = this.raster.toURI();
	        
	    	try {
	    		// removes the fragment from the URL
	    		URL url  = new URL(null, uri.toString(), CorePlugin.RELAXED_HANDLER);

	        	this.geoResources = null;
	        	ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
				List<IResolve> resolveList = catalog.find(url, monitor);
				// it doesn't exist a service for the url then create one and try again
				if(resolveList.isEmpty()){
					// requires url without fragment
					URL urlWithoutFragment = new URL(url.getProtocol(), url.getHost(),url.getPort(), url.getFile(), CorePlugin.RELAXED_HANDLER );
					this.services = catalog.constructServices(urlWithoutFragment, monitor);
					assert this.services.size() == 1; // TODO Improvement: if this assertion is OK it is not necessary to maintain a list of services
					
					for (IService service : services) {
			            catalog.add(service);
					}
					resolveList = catalog.find(new ID(uri), monitor);
					assert !resolveList.isEmpty();
				} 
				// the service for the url exist then gets the resource
		     	for( IResolve resolve : resolveList ) {

	            	geoResources = new ArrayList<IGeoResource>();
	            	List<IResolve> members = resolve.members(monitor);
	                if (members.size() < 1 && resolve.canResolve(IGeoResource.class)) {
	                	geoResources.add(resolve.resolve(IGeoResource.class, monitor));

	                } else if (members.get(0).canResolve(IGeoResource.class)) {
	                	
	                    for( IResolve tmp : members ) {
	                        IGeoResource finalResolve = tmp.resolve(IGeoResource.class, monitor);
	                        geoResources.add(finalResolve);
	                    }
	                }
		        }
		        return geoResources;
	    	} catch (Exception e){
	    		throw new RasterServiceException(e.getMessage(), e);
	    	}
	}

	/**
	 * Refreshes 
	 * 
	 * @param monitor
	 * @throws IOException
	 */
	public void refresh(IProgressMonitor monitor) throws IOException{
		// TODO implement ??
	}	
	
	public List<IGeoResource> resources(IProgressMonitor monitor) {
		
		if(this.geoResources != null) return this.geoResources;
		else return Collections.emptyList();
	}

	public IService getService() {
		
		if(this.services == null || this.services.isEmpty()) return null;
		
		return this.services.get(0);
	}

	public void dispose(IProgressMonitor monitor) {
		
        if (this.geoResources == null)
            return;
        if (monitor == null){
        	monitor = new NullProgressMonitor();
        }
    	IService service = getService();
    	if(service != null){
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
    	}
        
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
        
        cleanTemporaDirectory();
	}

	/**
	 * Removes all rester files that were created in the temporal directory
	 */
	private void cleanTemporaDirectory() {
		
		File tempDirectory = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), TMP_DIRECTORY);
		if(tempDirectory.exists()){
			if(this.raster != null && this.raster.exists()){
				final String[] fullName = this.raster.getName().split("[.]");
				
				final String name = fullName[0];
				final String path = this.tempDirectory.getPath();
				
				// remove the tiff file
				if( !this.raster.delete()){
					QueryPlugIn.log("cannot delete the file " + name , null);
				}
				
				// remove the world file
				deleteTemporalFile(path, name, ".tfw");
				
				// remove projection file
				deleteTemporalFile(path, name, ".prj");
			}
		}
	}

	/**
	 * Deletes the file
	 * 
	 * @param path
	 * @param fileName
	 * @param fileExtension
	 */
	private void deleteTemporalFile(String path, String fileName, String fileExtension) {
		
		StringBuilder fileToDelete = new StringBuilder();
		fileToDelete
			.append(path)
			.append(File.separator)
			.append(fileName)
			.append(fileExtension);
		
		File file = new File(fileToDelete.toString());
		if( !file.delete()){
			QueryPlugIn.log("cannot delete the file " + fileToDelete.toString(), null);
		}
	}

}
