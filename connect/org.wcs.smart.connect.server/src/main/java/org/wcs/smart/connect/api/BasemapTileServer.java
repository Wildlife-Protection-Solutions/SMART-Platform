/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.connect.api;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.impl.MapImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.ConnectStartupContextListener;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.BasemapBounds;
import org.wcs.smart.connect.model.BasemapTile;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.report.birt.map.BirtMapFactory;
import org.wcs.smart.report.birt.map.RestoreMapSettings;
import org.wcs.smart.report.birt.map.execute.MapCreator;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Service for providing desktop Conservation Area basemaps
 * as tiles for connect map.
 * 
 * @author Emily
 *
 */

@Path(ConnectRESTApplication.PATH_SEPERATOR)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class BasemapTileServer {

	private static final int TILESIZE = 256;
	private static final int TILE_TO_RENDER_BUFFER = 10;


	private final Logger logger = Logger.getLogger(BasemapTileServer.class.getName());

	@Context
	private HttpServletRequest request;

	@Context
	private ServletContext context;
	

	/*
	 * Tile generator
	 */
	private static volatile TileMaker Tiler = null;
	/*
	 * Lock object of synchronizing
	 */
	private static Object TILER_LOCK = new Object();
	private static Object TILE_CREATION_LOCK = new Object();
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("/basemap")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Lists all SMART desktop basemap layers for Conservation Areas the current user can access.")
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(array = @ArraySchema(schema = @Schema(implementation=AlertType.class)))})
  	public JSONArray getLayers() {
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();

		JSONArray obj = new JSONArray();
		try {
			List<Object[]> layers = getBasemapLayers(s, request.getUserPrincipal().getName());
			
			
			for (Object[] item : layers) {
				
				JSONObject o = new JSONObject();
				o.put("name", (String)item[0]); //$NON-NLS-1$
				o.put("url", (String)item[1]); //$NON-NLS-1$
				o.put("uuid", (String)item[2]); //$NON-NLS-1$
				o.put("visible", (Boolean)item[3]); //$NON-NLS-1$
				obj.add(o);
			}

		} finally {
			s.getTransaction().rollback();
		}
		return obj;
	}

	@PUT
	@Path("/basemap")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Updates the set of basemaps to be visible by default for the current user")
	@ApiResponse(responseCode = "200", description = "OK")
  	public void updateLayerSet(List<String> visible) {
		List<String> items = new ArrayList<>();
		Session s = HibernateManager.getSession(context);
		try {
			s.beginTransaction();
			for (String layer : visible) {
				try {
					UUID uuid = UuidUtils.stringToUuid(layer);
					//ensure valid basemap
					BasemapDefinition bm = s.get(BasemapDefinition.class, uuid);
					if(bm != null) {
						items.add(UuidUtils.uuidToString(bm.getUuid()));
					}
				}catch (Exception ex) {
					//skip this one
				}
			}
			
			String layers = String.join(",", items); //$NON-NLS-1$
			
			SmartUser toUpdate = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			if (toUpdate == null) return;
			toUpdate.setDefaultBasemaps(layers);
			
			s.getTransaction().commit();
			
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	/**
	 * Gets the specified tile for the given tileset. All images 
	 * are returned as png images.
	 * 
	 * @param tileset
	 * @param z
	 * @param x
	 * @param y
	 * @return
	 */
	@GET
	@Path("/basemap/{tileset}/{z}/{x}/{y}")
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	@ApiResponse(responseCode = "200", description = "OK")
	public Response getTile(@Parameter(description = "The tileset") @PathParam("tileset") String tileset,
			@Parameter(description = "z") @PathParam("z") String z,
			@Parameter(description = "x") @PathParam("x") String x,
			@Parameter(description = "y") @PathParam("y") String y) {

		UUID layer = null;
		try {
			layer = UuidUtils.stringToUuid(tileset);
		} catch (Exception ex) {
			throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("BasemapTileServer.InvalidTileSet", request.getLocale()), layer)); //$NON-NLS-1$
		}

		int tx = -1;
		int ty = -1;
		int tz = -1;
		try {
			tx = Integer.parseInt(x);
			ty = Integer.parseInt(y);
			tz = Integer.parseInt(z);
		} catch (Exception ex) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("BasemapTileServer.InvalidRequest", request.getLocale())); //$NON-NLS-1$
		}

		BasemapDefinition def = null;
		byte[] data = null;
		boolean needsprocessing = true;
		
		TileJobItem tile = null;
		try{
			try (Session s = HibernateManager.getSession(context)){
				s.beginTransaction();
				try {
					def = s.get(BasemapDefinition.class, layer);
					if (def == null)
						throw new SmartConnectException(Status.NOT_FOUND, MessageFormat.format(Messages.getString("BasemapTileServer.NotFound", request.getLocale()), layer)); //$NON-NLS-1$
		
					if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.VIEWCA_KEY,
							def.getConservationArea().getUuid())) {
						throw new SmartConnectException(Response.Status.UNAUTHORIZED);
					}
					data = findTile(s, def, tx, ty, tz);
	
					if (data == null) {
						needsprocessing = needsGeneration(s, def, tz, tx, ty);
					}
				}finally {
					s.getTransaction().commit();
				}
			}
	
			if (needsprocessing) {
				tile = createTiles(def, tx, ty, tz);
			}
			
			if (tile != null) {
				TileMaker tiler = getTiler();
				TileJobItem t = tiler.findItem(tile);
				if (t != null) {
					String key = createKey(tx, ty);
					while(tiler.layersProcessing.contains(t) && !t.created.contains(key)) {
						synchronized (tiler) {
							tiler.wait(300000); //wait max of 5 minutes
						}
					}
				}
				//check if tile
				try(Session s = HibernateManager.getSession(context)){
					s.beginTransaction();
					data = findTile(s, def, tx, ty, tz);
					s.getTransaction().commit();
				}
			}
			String fileName = z + "_" + x + "_" + y + ".png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return Response.ok(data, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.entity(data).build();

		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}

	/**
	 * 
	 * @param session
	 * @param username
	 * @return list of array of basemaps the given user can access (name, url, uuid, onbydefault)
	 */
	public static List<Object[]> getBasemapLayers(Session session, String username) {
		List<Object[]> layers = new ArrayList<>();
		
		Set<UUID> onByDefault = new HashSet<>();
		SmartUser user = HibernateManager.getUser(session, username);
		if (user != null) {
			String bms = user.getDefaultBasemaps();
			if (bms != null) {
				for (String bm : bms.split(",")) { //$NON-NLS-1$
					try {
						onByDefault.add(UuidUtils.stringToUuid(bm));
					}catch (Exception ex) {
						
					}
				}
			}
		}
		List<BasemapDefinition> items = session.createQuery("FROM BasemapDefinition", BasemapDefinition.class).list(); //$NON-NLS-1$
		for (BasemapDefinition item : items) {
			if (!SecurityManager.INSTANCE.canAccess(session, username, 
					CaAction.VIEWCA_KEY,
					item.getConservationArea().getUuid()))
				continue;

			String uuid = UuidUtils.uuidToString(item.getUuid());
			String name = item.getConservationArea().getId() + ": " + item.getName(); //$NON-NLS-1$
			String url = ConnectRESTApplication.APP_PATH +
					ConnectRESTApplication.PATH_SEPERATOR + 
					"basemap" +  //$NON-NLS-1$
					ConnectRESTApplication.PATH_SEPERATOR +
					uuid
					+ "/{z}/{x}/{y}"; //$NON-NLS-1$
			
			boolean on = false;
			if (onByDefault.contains(item.getUuid())) on = true;
			layers.add(new Object[] {name, url, uuid, on});
		}
		layers.sort((a,b) -> Collator.getInstance().compare(a[0], b[0]) );
		return layers;
	}

	
	/**
	 * Gets the tile maker, creating new one if necessary
	 * 
	 * @return
	 */
	private TileMaker getTiler() {
		if (Tiler != null) return Tiler;
		
		synchronized (TILER_LOCK) {
			if (Tiler == null) {
				Tiler = new TileMaker( HibernateManager.getSessionFactory(context) );
			}
			return Tiler;
		}
	}
	
	/**
	 * if there is at least one tile for a given zoom level we don't generate tiles
	 * @param session
	 * @param layer
	 * @param z
	 * @return
	 */
	private static boolean needsGeneration(Session session, BasemapDefinition layer, int z, int x, int y) {
		BasemapBounds bnds = session.get(BasemapBounds.class, layer.getUuid());
		if (bnds == null) return true;
			
		Envelope env1 = new Envelope(bnds.getXMin(), bnds.getXMax(), bnds.getYMin(), bnds.getYMax());
		Envelope env2 = tileToBoundsLatLong(x, y, z);
			
		if (env1.intersects(env2)) return true;
		return false;
		
	}
//	@GET
//	@Path("/basemap2/{z}/{x}/{y}")
//	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
//	public Response getTile2(@Parameter(description = "The tileset") @PathParam("tileset") String tileset,
//			@Parameter(description = "z") @PathParam("z") String z,
//			@Parameter(description = "x") @PathParam("x") String x,
//			@Parameter(description = "y") @PathParam("y") String y) {
//
//		Session s = HibernateManager.getSession(context);
//		s.beginTransaction();
//		try {
//
//			BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
//			Graphics2D g = img.createGraphics();
//
//			g.setComposite(AlphaComposite.Clear);
//			g.fillRect(0, 0, 256, 256);
//
//			g.setComposite(AlphaComposite.SrcOver);
//
//			g.setColor(Color.RED);
//			g.drawRect(0, 0, 255, 255);
//			g.drawLine(0, 0, 256, 256);
//			String tile = z + ": (" + x + " " + y + ")";
//
//			Rectangle2D bnds = g.getFontMetrics().getStringBounds(tile, g);
//			g.drawString(tile, (int) ((256 / 2.0) - (bnds.getWidth() / 2.0)),
//					(int) ((256 / 2.0) - (bnds.getHeight() / 2.0)));
//
//			String fileName = z + "_" + x + "_" + y + ".png";
//
//			StreamingOutput stream = new StreamingOutput() {
//				@Override
//				public void write(OutputStream output) throws IOException {
//					ImageIO.write(img, "png", output);
//
//				}
//			};
//
//			return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
//					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"") //$NON-NLS-1$ //$NON-NLS-2$
//					// .header(HttpHeaders.CONTENT_LENGTH, fileSize)
////					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
//					.build();
//
//		} catch (Exception ex) {
//			logger.log(Level.SEVERE, ex.getMessage(), ex);
//			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
//		} finally {
//			s.getTransaction().rollback();
//		}
//	}


	
	private TileJobItem createTiles(BasemapDefinition layer, int x, int y, int z) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
		
		TileMaker tiler = getTiler();
		synchronized (tiler.layersProcessing) {
			for (TileJobItem i : tiler.layersProcessing) {
				if (i.def.equals(layer) && i.containsSubTile(x, y, z)) {
					return i;
				}
			}
		}
		
		ReferencedEnvelope re = null;
		synchronized (TILE_CREATION_LOCK) {
			try (Session session = HibernateManager.getSession(context)){
				session.beginTransaction();
				try {
					BasemapBounds bnds = session.get(BasemapBounds.class, layer.getUuid());
					if (bnds != null) {
						re = new ReferencedEnvelope(bnds.getXMin(), bnds.getXMax(), bnds.getYMin(), bnds.getYMax(), GeometryUtils.SMART_CRS);
					}else {
						Map thisMap = BirtMapFactory.createMap();
						((MapImpl) thisMap).getContextModel().eSetDeliver(false);
						((MapImpl) thisMap).eSetDeliver(false);
				
						(new RestoreMapSettings()).applyTo((Map) thisMap, layer, layer.getConservationArea(),
								new IDatabaseConnectionProvider() {
									private static final long serialVersionUID = 1L;
				
									@Override
									public Session openSession() {
										return session;
									}
				
									@Override
									public Locale getLocale() {
										return Locale.getDefault();
									}
				
									@Override
									public void finishSession(Session session) {
									}
								});
						CoordinateReferenceSystem sphericalMercator = CRS.decode("EPSG:3857", true); //$NON-NLS-1$
						thisMap.sendCommandSync(new ChangeCRSCommand(sphericalMercator));
			
						// determine bounds of map
						re = thisMap.getBounds(new NullProgressMonitor());
						
						re = re.transform(GeometryUtils.SMART_CRS, false);
		
						BasemapBounds bnd = new BasemapBounds();
						bnd.setBasemapUuid(layer.getUuid());
						bnd.setXMax(re.getMaxX());
						bnd.setYMin(re.getMinY());
						bnd.setXMin(re.getMinX());
						bnd.setYMax(re.getMaxY());
						session.persist(bnd);
						
						session.getTransaction().commit();
					}
				}catch (Exception ex) {
						session.getTransaction().rollback();
						throw ex;
				}				
			}
			
			ReferencedEnvelope ll = re.transform(GeometryUtils.SMART_CRS, false);
	
			int[] minTile = toTile(ll.getMinX(), ll.getMinY(), z);
			int[] maxTile = toTile(ll.getMaxX(), ll.getMaxY(), z);
	
			int txmin = Math.min(minTile[0], maxTile[0]);
			int tymin = Math.min(minTile[1], maxTile[1]);
			int txmax = Math.max(minTile[0], maxTile[0]);
			int tymax = Math.max(minTile[1], maxTile[1]);
	
			// process images TILE_TO_RENDER_BUFFER x TILE_TO_RENDER_BUFFER at a time			
			TileJobItem thisTile = null;
			for (int tx = txmin; tx <= txmax; tx += TILE_TO_RENDER_BUFFER) {
				for (int ty = tymin; ty <= tymax; ty += TILE_TO_RENDER_BUFFER) {
					// generate tile for tx,ty
					int startx = tx;
					int starty = ty;
					int endx = Math.min(txmax, tx + TILE_TO_RENDER_BUFFER - 1);
					int endy = Math.min(tymax, ty + TILE_TO_RENDER_BUFFER - 1);
					
					if (startx <= x && starty <= y && endx >= x && endy >= y) {
						thisTile = new TileJobItem(layer, startx, starty, endx, endy, z);
						break;
					}
				}
			}
			if (thisTile == null) return null;
			TileJobItem temp = tiler.findItem(thisTile);
			if (temp == null) temp = thisTile;
			tiler.addLayer(temp);
			return temp;
		}
	}
	
	/**
	 * finds the given tile in the db or returns null
	 * 
	 * @param session
	 * @param layer
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private byte[] findTile(Session session, BasemapDefinition layer, int x, int y, int z) {
		byte[] data = (byte[]) session
				.createNativeQuery("SELECT connect.find_tile(:layer, :z, :x, :y) ", byte[].class) //$NON-NLS-1$
				.setParameter("layer", layer.getUuid()) //$NON-NLS-1$
				.setParameter("z", z) //$NON-NLS-1$
				.setParameter("x", x) //$NON-NLS-1$
				.setParameter("y", y) //$NON-NLS-1$
				.uniqueResult();
		return data;
	}

	

	//convert lat/long to tile x,y
	private static int[] toTile(double x, double y, int zoom) {
		double lon = x;
		double lat = y;

		int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
		int ytile = (int) Math
				.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2
						* (1 << zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);
		return new int[] { xtile, ytile };
	}

	//converts tile x,y to to bounds in lat/lon
	private  static Envelope tileToBoundsLatLong(int tilex, int tiley, int zoom) {
		double north = Math.toDegrees(Math.atan(Math.sinh((Math.PI - (2.0 * Math.PI * tiley) / Math.pow(2.0, zoom)))));
		double south = Math
				.toDegrees(Math.atan(Math.sinh((Math.PI - (2.0 * Math.PI * (tiley + 1)) / Math.pow(2.0, zoom)))));
		double west = tilex / Math.pow(2.0, zoom) * 360.0 - 180;
		double east = (tilex + 1) / Math.pow(2.0, zoom) * 360.0 - 180;

		return new Envelope(west, east, south, north);
	}
	
	private static String createKey(int x, int y) {
		return x + "_" + y; //$NON-NLS-1$
	}

	//converts lat/lon to mercator projection
	private Coordinate toMercator(double x, double y) {
		x = 6378137.0 * Math.toRadians(x);
		if (y <= -90) {
			y = Double.NEGATIVE_INFINITY;
		} else if (y >= 90) {
			y = Double.POSITIVE_INFINITY;
		} else {
			y = 6378137.0 * Math.log(Math.tan((Math.PI * 0.25) + (0.5 * Math.toRadians(y))));
		}
		return new Coordinate(x, y);
	}
	
	private BufferedImage renderMap(Session session, TileJobItem item) throws NoSuchAuthorityCodeException, FactoryException {
		Map thisMap = BirtMapFactory.createMap();
		((MapImpl) thisMap).getContextModel().eSetDeliver(false);
		((MapImpl) thisMap).eSetDeliver(false);

		(new RestoreMapSettings()).applyTo((Map) thisMap, item.def, item.def.getConservationArea(),
				new IDatabaseConnectionProvider() {
					private static final long serialVersionUID = 1L;

					@Override
					public Session openSession() {
						return session;
					}

					@Override
					public Locale getLocale() {
						return Locale.getDefault();
					}

					@Override
					public void finishSession(Session session) {
					}
				});
		CoordinateReferenceSystem sphericalMercator = CRS.decode("EPSG:3857", true); //$NON-NLS-1$
		thisMap.sendCommandSync(new ChangeCRSCommand(sphericalMercator));

		int startx = item.xmin;
		int starty = item.ymin;
		int endx = item.xmax;
		int endy = item.ymax;
		
		Envelope emin = tileToBoundsLatLong(startx, starty, item.z);
		Envelope emax = tileToBoundsLatLong(endx, endy, item.z);

		Coordinate c0 = toMercator(emin.getMinX(), emin.getMinY());
		Coordinate c1 = toMercator(emin.getMaxX(), emin.getMaxY());
		Coordinate c2 = toMercator(emax.getMinX(), emax.getMinY());
		Coordinate c3 = toMercator(emax.getMaxX(), emax.getMaxY());

		Envelope r1 = new Envelope(c0, c1);
		r1.expandToInclude(new Envelope(c2, c3));

		int width = (endx - startx + 1) * TILESIZE;
		int height = (endy - starty + 1) * TILESIZE;

		// create a single image for entire area
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gg = img.createGraphics();
		gg.setComposite(AlphaComposite.Clear);
		gg.fillRect(0, 0, width, height);
		gg.setComposite(AlphaComposite.Src);

		ReferencedEnvelope rr = new ReferencedEnvelope(r1, sphericalMercator);
		try {
			MapCreator.INSTANCE.drawMap(gg, new Dimension(img.getWidth(), img.getHeight()), thisMap, 92, rr);
		} catch (Throwable tt) {
			logger.log(Level.WARNING, tt.getMessage(), tt);
			return null;
		}
		
//				try(ByteArrayOutputStream bout = new ByteArrayOutputStream()){
//					ImageIO.write(img, "png", new File("C:\\temp\\tiles\\all_" + z + "_" + x +"_"+ y + ".png"));					
//				}
		gg.dispose();
		return img;
	}
	
	/**
	 * For generating tiles for zoom levels
	 * @author Emily
	 *
	 */
	private class TileMaker implements Runnable{
		
		private List<TileJobItem> layersProcessing = Collections.synchronizedList(new ArrayList<>());	
		private SessionFactory factory;
		
		public TileMaker(SessionFactory factory) {
			this.factory = factory;
		}
		
		public TileJobItem findItem(TileJobItem search) {
			synchronized (layersProcessing) {
				for (TileJobItem item : layersProcessing) if (item.equals(search)) return item;
			}
			return null;
		}
		
		@Override
		public void run() {
			while(!layersProcessing.isEmpty()) {
				processLayer();
				synchronized (this) {
					this.notifyAll();	
				}
			}
			
			return;
		}
		
		public void processLayer() {
			if (layersProcessing.isEmpty()) return;
			
			TileJobItem toProcess = null;
			synchronized (layersProcessing) {
				for (TileJobItem it : layersProcessing) {
					if (!it.inprogress.get()) {
						toProcess = it;
						it.inprogress.set(true);
						break;
					}
				}
			}
			if (toProcess == null) return;
			try(Session session = factory.openSession()){
				generateTiles(session, toProcess);
				layersProcessing.remove(toProcess);
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		public void addLayer(TileJobItem item) {
			synchronized (layersProcessing) {
				if (!layersProcessing.contains(item)) {
					layersProcessing.add(0, item);	
					ExecutorService executor = (ExecutorService) context.getAttribute(ConnectStartupContextListener.EXECUTOR_KEY);
					executor.execute(this);	
				}
			}
			
			synchronized (this) {
				this.notifyAll();	
			}
		}

		
		/*
		 * generates the tiles for the given layer and zoom level and returns the
		 * specific x,y tile
		 */
		private void generateTiles(Session session, TileJobItem item)
				throws NoSuchAuthorityCodeException, FactoryException, IOException, TransformException {

			session.beginTransaction();
			try {
				if (findTile(session, item.def, item.xmin, item.ymin, item.z ) != null) {
					return;
				}
			}finally {
				session.getTransaction().commit();
			}
			
			int startx = item.xmin;
			int starty = item.ymin;
			int endx = item.xmax;
			int endy = item.ymax;

			BufferedImage mapImage = renderMap(session, item);
			
			BufferedImage tileimage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gc = tileimage.createGraphics();

			for (int i = 0; i < TILE_TO_RENDER_BUFFER; i++) {
				for (int j = 0; j < TILE_TO_RENDER_BUFFER; j++) {
					if (startx + i > endx || starty + j > endy)
						continue;

					int tilex = startx + i;
					int tiley = starty + j;
					
					gc.setComposite(AlphaComposite.Clear);
					gc.fillRect(0, 0, tileimage.getWidth(), tileimage.getHeight());
					gc.setComposite(AlphaComposite.Src);
					gc.drawImage(mapImage, 0, 0, 256, 256, i * 256, j * 256, (i + 1) * 256, (j + 1) * 256, null);
					gc.setColor(Color.RED);
//						String id = z + " " + tilex + ":" + tiley;
//						gc.drawString(id, 100, 100);
					writeTile(session, item.def, tilex, tiley, item.z, tileimage);
					item.created.add(createKey(tilex, tiley));
					synchronized (this) {
						this.notifyAll();
					}
					
				}
			}
			gc.dispose();
		}

		//saves tile data to database
		private BasemapTile writeTile(Session session, BasemapDefinition layer, int x, int y, int z, BufferedImage image)
				throws IOException {
			BasemapTile tile = new BasemapTile();
			tile.setBasemapUuid(layer.getUuid());
			tile.setLastAccessed(LocalDateTime.now());
			tile.setX(x);
			tile.setY(y);
			tile.setZ(z);

			try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
//				ImageIO.write(image, "png", new File("C:\\temp\\tiles\\abc_" + z + "_" + x +"_"+ y + ".png"));
				ImageIO.write(image, "png", bout); //$NON-NLS-1$
				tile.setData(bout.toByteArray());
			}
			
			session.beginTransaction();
			try {
				session.persist(tile);
				session.getTransaction().commit();
			}catch (Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
			}
			
			return tile;
		}
	};
	
	/**
	 * Job item for computing tiles for a given zoom level
	 * 
	 * @author Emily
	 *
	 */
	private class TileJobItem{
		//basemap definition
		BasemapDefinition def;
		AtomicBoolean inprogress = new AtomicBoolean(false);
		int xmin, xmax, ymin, ymax, z;
		Set<String> created = Collections.synchronizedSet(new HashSet<>());
		
		public TileJobItem(BasemapDefinition def, int xmin, int ymin, int xmax, int ymax, int z) {
			this.def = def;
			this.xmin = xmin;
			this.xmax = xmax;
			this.ymin = ymin;
			this.ymax = ymax;
			this.z = z;
		}
		
		public boolean containsSubTile(int x, int y, int z) {
			if (this.z != z) return false;
			if (xmin <= x && ymin <= y && xmax > x && ymax > y) return true;
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(def.getUuid(),  xmin, xmax, ymin, ymax, z);
		}
		
		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (other== this) return true;
			if (!other.getClass().equals(getClass())) return false;
			
			TileJobItem item = ((TileJobItem)other);
			return Objects.equals(def, item.def) &&
					Objects.equals(this.xmin, item.xmin) &&
					Objects.equals(this.ymin, item.ymin) &&
					Objects.equals(this.ymax, item.ymax) &&
					Objects.equals(this.xmax, item.xmax) &&
					Objects.equals(this.z, item.z);
			
		}
	}
	

}

