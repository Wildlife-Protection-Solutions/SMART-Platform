/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.udig;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.ParameterGroup;
import org.geotools.styling.ColorMap;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.util.factory.GeoTools;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResourceInfo;
import org.locationtech.udig.catalog.rasterings.AbstractRasterService;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.wcs.smart.SmartContext;
import org.wcs.smart.paws.model.PawsResultFile;
import org.wcs.smart.util.SharedUtils;


/**
 * Service for displaying PAWS results
 */
public class PawsTiffGeoResource extends AbstractRasterGeoResource {

	private AbstractGridCoverage2DReader reader;
	private Path file;

	private String name;
	/**
	 * Construct <code>GeoTiffGeoResourceImpl</code>.
	 */
	public PawsTiffGeoResource(AbstractRasterService service, PawsResultFile rfile, Path layerfile) {
		super(service, parsePath(layerfile));
		layerfile = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).relativize(layerfile);
		this.file = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(layerfile);
		this.name= SharedUtils.getFilenameWithoutExtension( rfile.getResultsFile().getFileName().toString() )
				+ ":" + SharedUtils.getFilenameWithoutExtension( layerfile.getFileName().toString() ); //$NON-NLS-1$

	}

	private static String parsePath(Path file) {
		file = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).relativize(file);
		
		String part = file.toString();
		part = part.replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		if (part.startsWith(".")) return part.substring(1); //$NON-NLS-1$
		return part;
	}
	/**
	 * Template method called by findResource that is responsible for loading the
	 * coverage. By default it loads a very small coverage for getting info from but
	 * not using as a real datasource
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	protected GridCoverage loadCoverage() throws IOException {
		AbstractGridCoverage2DReader reader = getReader();
		ParameterGroup pvg = getReadParameters();
		List<GeneralParameterValue> list = pvg.values();
		GeneralParameterValue[] values = list.toArray(new GeneralParameterValue[0]);
		GridCoverage gridCoverage = reader.read(values);
		return gridCoverage;
	}

	@Override
	protected AbstractRasterGeoResourceInfo createInfo(IProgressMonitor monitor) throws IOException {
		return info;
	}
	
	@Override
	public void dispose(IProgressMonitor monitor) {
		super.dispose(monitor);
		if (reader != null) reader.dispose();
	}

	public synchronized AbstractGridCoverage2DReader getReader() throws FileNotFoundException {
		if (this.reader == null) {
			AbstractGridFormat frmt = (AbstractGridFormat) ((PawsService) service).getFormat();
			if (Files.exists(file)) {
				this.reader = (AbstractGridCoverage2DReader) frmt.getReader(file.toFile());
			}else {
				throw new FileNotFoundException(file.toString());
			}

		}
		return this.reader;
	}
	
	@Override
	public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor) throws IOException {
		
		if (adaptee.isAssignableFrom(AbstractGridCoverage2DReader.class)) {
			AbstractGridCoverage2DReader reader = getReader();
			return adaptee.cast(reader);
		}
		return super.resolve(adaptee, monitor);
	}
				
	public Style style(IProgressMonitor monitor) {
		
		
		StyleFactory sf = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
		StyleBuilder sb = new StyleBuilder(sf);
		
		String[] labels = new String[] {"-no data-","","","","","","","","","","","",""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$
		double[] values = new double[] {-9999,0,0.000001,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8, 0.9,1.0};
		Color[] colors = new Color[] {
				new Color(255,255,255,0),
				new Color(255,255,255,255),
				new Color(255,247,236,255),
				new Color(254,237,212,255),
				new Color(253,225,186,255),
				new Color(253,212,158,255),
				new Color(253,195,140,255),
				new Color(253,171,117,255),
				new Color(252,141,89,255),
				new Color(243,114,77,255),
				new Color(231,83,58,255),
				new Color(215,48,31,255),
				new Color(190,15,10,255),
		};
		ColorMap cm = sb.createColorMap(labels, values, colors, ColorMap.TYPE_RAMP);
		
		RasterSymbolizer sym = sb.createRasterSymbolizer(cm, 1);
		
		return sb.createStyle(sym);

	}
	
	AbstractRasterGeoResourceInfo info = new AbstractRasterGeoResourceInfo(this, "PAWS") { //$NON-NLS-1$
		@Override
		public String getTitle() {
			return PawsTiffGeoResource.this.name;
		}
		
		 @Override
		    public synchronized ReferencedEnvelope getBounds() {
			 try {
				 Envelope env = getReader().getOriginalEnvelope();
				 return new ReferencedEnvelope(env.getMinimum(0), env.getMaximum(0), env.getMinimum(1), env.getMaximum(1), getReader().getCoordinateReferenceSystem());
			 }catch (Exception ex) {
				 ex.printStackTrace();
			 }
			 return new ReferencedEnvelope();
			 
		 }
	};
}
