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
package org.wcs.smart.udig.legend;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.geotools.styling.Rule;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Utility methods to create common ImageDescriptors using AWTGraphics only.
 * Modelled after the uDig Glyph class
 * 
 */
public class GlyphAWT {

    private final static int DEFAULT_WIDTH = 16;
    private final static int DEFAULT_HEIGHT = 16;
    private static final Color DEFAULT_BORDER = new Color(0,0,0);
    private static final Color DEFAULT_FILL = new Color(27,158,119, 255);
    
    /** Utility class for working with Images, Features and Styles */
    private static DrawingAWT d = new DrawingAWT();    
    

    public static ImageData extractImageDataAndDispose( BufferedImage image ) {
    	return LegendGraphicWriter.convertToSWT(image);
    }
    
    /**
     * Render a icon based on the current style.
     * <p>
     * Simple render of point in the center of the screen.
     * </p>
     * @param style
     * @return Icon representing style applyed to an image
     */
    public static ImageDescriptor point( final Rule rule ) {
        return new ImageDescriptor(){
            public ImageData getImageData() {
                try {
                	BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                	d.drawDirect( bimage, d.feature(d.point(7,7)), rule );                     
                	return extractImageDataAndDispose( bimage );
                } catch(RuntimeException ex) {
                    throw ex;
                }
            }
        };
    }
    /**
     * Icon for point data in the provided color
     * <p>
     * XXX: Suggest point( SLD style ) at a later time.
     * </p>
     * @return ImageDescriptor
     */    
    public static ImageDescriptor point() {
    	return point(DEFAULT_BORDER, DEFAULT_FILL);
    }

    /**
     * Icon for point data in the provided color
     * <p>
     * XXX: Suggest point( SLD style ) at a later time.
     * </p>
     * @param color
     * @param fill
     * @return ImageDescriptor
     */    
    public static ImageDescriptor point( final Color color, final Color fill ) {
        return new ImageDescriptor(){
            public ImageData getImageData() {
               
                try {
                BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                Graphics2D gc = bimage.createGraphics();
                configureGc(gc);
                try{
                	gc.setStroke(new BasicStroke(1));
                	
	                Color c = color == null ? Color.BLACK :  color;
	                Color f = fill == null ? Color.LIGHT_GRAY : fill;
	                gc.setColor( f );
	                gc.fillRect( 8,7, 5, 5 );
	                gc.setColor( c );
	                gc.drawRect( 8,7, 5, 5 );
                }finally{
                	gc.dispose();
                }
                return extractImageDataAndDispose(bimage);
               
                } catch(RuntimeException ex) {
                    throw ex;
                }
            }
        };
    } 
    /**
     * Complex render of Geometry allowing presentation of point, line and polygon styles.
     * <p>
     * Layout:<pre><code>
     *    1 2 3 4 5 6 7 8 9101112131415
     *   0
     *  1          LL                 L  
     *  2          L L                L
     *  3         L  L               L                   
     *  4        L    L             L  
     *  5        L     L            L  
     *  6       L      L           L   
     *  7      L        L         L    
     *  8      L         L        L    
     *  9     L          L       L     
     * 10    L            L     L      
     * 11    L             L    L      
     * 12   L              L   L       
     * 13  L                L L        
     * 14  L                 LL            
     * 15
     * </code><pre>
     * </p>
     */
    public static ImageDescriptor line() {
    	return line(DEFAULT_BORDER,1);
    }

    /**
     * Complex render of Geometry allowing presentation of point, line and polygon styles.
     * <p>
     * Layout:<pre><code>
     *    1 2 3 4 5 6 7 8 9101112131415
     *   0
     *  1          LL                 L  
     *  2          L L                L
     *  3         L  L               L                   
     *  4        L    L             L  
     *  5        L     L            L  
     *  6       L      L           L   
     *  7      L        L         L    
     *  8      L         L        L    
     *  9     L          L       L     
     * 10    L            L     L      
     * 11    L             L    L      
     * 12   L              L   L       
     * 13  L                L L        
     * 14  L                 LL            
     * 15
     * </code><pre>
     * </p>
     * @param style 
     * @return Icon representing geometry style
     */
    public static ImageDescriptor line( final Rule rule ) {
        final SimpleFeature feature=d.feature(d.line(new int[]{1,14, 6,0, 11,14, 15,1}));
        return new ImageDescriptor(){
            public ImageData getImageData() {
            	
                try {
                	BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                	d.drawDirect( bimage, feature, rule );                     
                	return extractImageDataAndDispose( bimage );            
                } catch(RuntimeException ex) {
                    throw ex;
                }
            }
        };       
    }
    /**
     * Icon for linestring in the provided color and width.
     * <p>
     * XXX: Suggest line( SLD style ) at a later time.
     * </p>
     * @param black
     * @return Icon
     */
    public static ImageDescriptor line( Color color, int width ) {
        Color color2 = color;
        int width2 = width;
        if (color2 == null) {
            color2 = Color.BLACK;
        }
        
        if (width2 <= 0) {
            width2 = 1;
        }
        
        final int finalWidth = width2;
        final Color finalColor = color2;
                
        return new ImageDescriptor(){
            public ImageData getImageData() {
            	 try {
                     BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                     Graphics2D gc = bimage.createGraphics();
                     configureGc(gc);
                     try{
                     	gc.setStroke(new BasicStroke(finalWidth));
                     	
                     	gc.setColor( finalColor );
                     	gc.drawLine(1, 13, 6, 2);
                        gc.drawLine(6, 2, 9, 13);
                        gc.drawLine(9, 13, 14, 2);
                       
                     }finally{
                     	gc.dispose();
                     }
                     return extractImageDataAndDispose(bimage);
                    
                     } catch(RuntimeException ex) {
                         throw ex;
                     }
            }
        };
    }

    /**
     * Complex render of Geometry allowing presentation of point, line and polygon styles.
     * <p>
     * Layout:<pre><code>
     *    1 2 3 4 5 6 7 8 9101112131415
     *   0
     *  1 
     *  2
     *  3           L                 L                  
     *  4       p  L L           PPPPPP
     *  5         L   L     PPPPP   L p
     *  6        L     LPPPP       L  p
     *  7       L    PPPL         L   p
     *  8      L   PP    L       L    p
     *  9     L   P       L     L     P
     * 10    L   P         L   L      P
     * 11   L   P           L L       P
     * 12  L   P             L        P
     * 13      p                      P
     * 14      PPPPPPPPPPPPPPPPPPPPPPPP    
     * 15
     * </code><pre>
     * </p>
     * @param style 
     * @return Icon representing geometry style
     */
    public static ImageDescriptor geometry( final Rule rule ) {
        return new ImageDescriptor(){
            public ImageData getImageData() {
            	 try {
                 	BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                 	 d.drawDirect(bimage, d.feature(d.line(new int[]{0, 12, 6, 3, 11, 12, 15, 3})), rule);
                     d.drawDirect(bimage, d.feature(d.point(4, 4)), rule);               
                 	return extractImageDataAndDispose( bimage );            
                 } catch(RuntimeException ex) {
                     throw ex;
                 }
            }
        };       
    }
    /**
     * Icon for generic Geometry or Geometry Collection.
     * @param color 
     * @param fill 
     * 
     * @return Icon
     */
    public static ImageDescriptor geometry( final Color color, final Color fill ) {
        return new ImageDescriptor(){
            public ImageData getImageData() {
                
            	try {
                    BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                    Graphics2D gc = bimage.createGraphics();
                    configureGc(gc);
                    try{
                    	gc.setStroke(new BasicStroke(1));
                    	Color c = color == null ? Color.BLACK :  color;
     	                Color f = fill == null ? Color.LIGHT_GRAY : fill;
                        gc.setColor( f );
                        gc.fillRoundRect( 2,1, 13, 13, 2, 2 );
                        gc.setColor( c  );
                        gc.drawRoundRect( 2,1, 13, 13, 2, 2 );
                    }finally{
                    	gc.dispose();
                    }
                    return extractImageDataAndDispose(bimage);
                   
                    } catch(RuntimeException ex) {
                        throw ex;
                    }
            	
             
            }
        };
    }     

    /**
     * Render of a polygon allowing style.
     * <p>
     * Layout:<pre><code>
     *    1 2 3 4 5 6 7 8 9101112131415
     *   0
     *  1             
     *  2                      PPPPPPPP
     *  3                PPPPPP       P                  
     *  4           PPPPPP            P
     *  5        PPP                  p
     *  6      PP                     p
     *  7     P                       p
     *  8    P                        p
     *  9   P                         P
     * 10   P                         P
     * 11  P                          P
     * 12  P                          P
     * 13  P                          P
     * 14  PPPPPPPPPPPPPPPPPPPPPPPPPPPP    
     * 15
     * </code><pre>
     * </p>
     * @param style 
     * @return Icon representing geometry style
     */
    public static ImageDescriptor polygon( final Rule rule ) {
    	return new ImageDescriptor(){
            public ImageData getImageData() {
	            try{	
	               	BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
	                d.drawDirect( bimage, d.feature(d.polygon(new int[]{1,14, 3,9, 4,6,  6,4,  9,3, 14,1, 14,14})), rule );
	                return extractImageDataAndDispose( bimage );            
	            } catch(RuntimeException ex) {
	                throw ex;
	            }
            }
        };
    }
  
    /**
     * Icon for polygon in default border, fill and width
     */
    public static ImageDescriptor polygon() {
    	return polygon(DEFAULT_BORDER, DEFAULT_FILL,1);
    }

    	/**
     * Icon for polygon in provided border, fill and width
     * 
     * @param black
     * @param gray
     * @param i
     * @return
     */
    public static ImageDescriptor polygon( final Color color, final Color fill, final int width ) {        
        return new ImageDescriptor(){
            public ImageData getImageData() {
            	try {
                    BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                    Graphics2D gc = bimage.createGraphics();
                    configureGc(gc);
                    try{
                    	int w = width > 0 ? width : 1;
                    	 gc.setStroke(new BasicStroke(w));
                    	 Color c = color == null ? Color.BLACK : color;
                         Color f = fill == null ? Color.LIGHT_GRAY : color;

                         int[] x= { 1,3,4,6, 9, 14, 14 };
                         int[] y= { 14, 9, 6,  4,  3, 1, 14 };
                         gc.setColor(f);
                         gc.fillPolygon(x,y,x.length);
                         gc.setColor(c);
                         gc.drawPolygon(x,y,x.length);
                    }finally{
                    	gc.dispose();
                    }
                    return extractImageDataAndDispose(bimage);
                   
                    } catch(RuntimeException ex) {
                        throw ex;
                    }
            	
            }
        };
    }

    private static void configureGc(Graphics2D gc){
    	gc.setRenderingHint(
     	        RenderingHints.KEY_TEXT_ANTIALIASING,
     	        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
     	gc.setRenderingHint(
     	        RenderingHints.KEY_ANTIALIASING,
     	        RenderingHints.VALUE_ANTIALIAS_ON);
    }
    /**
     * Icon for grid data, small grid made up of provided colors.
     * <p>
     * Layout:<pre><code>
     *    0 1 2 3 4 5 6 7 8 9 101112131415
     *  0  
     *  1   AAAAAAAAAAAAABBBBBBBBBBBBBB           
     *  2   AAAAAAAAAAAAABBBBBBBBBBBBBB
     *  3   AAAAAAAAAAAAABBBBBBBBBBBBBB                  
     *  4   AAAAAAAAAAAAABBBBBBBBBBBBBB
     *  5   AAAAAAAAAAAAABBBBBBBBBBBBBB
     *  6   AAAAAAAAAAAAABBBBBBBBBBBBBB
     *  7   AAAAAAAAAAAAABBBBBBBBBBBBBB
     *  8   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     *  9   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     * 10   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     * 11   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     * 12   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     * 13   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     * 14   CCCCCCCCCCCCCDDDDDDDDDDDDDD
     * 15
     * </code><pre>
     * </p>
     * @param a
     * @param b
     * @param c
     * @param d1
     * @return Icon representing a grid
     * 
     */
    public static ImageDescriptor grid( Color a, Color b, Color c, Color d1) {
        if (a == null) {
            a = Color.BLACK;
        }        
        if (b == null) {
            b = Color.DARK_GRAY;
        }
        
        if (c == null) {
            c = Color.LIGHT_GRAY;
        }
        
        if (d1 == null) {
            d1 = Color.WHITE;
        }        
        final Color finalA = a;
        final Color finalB = b;
        final Color finalC = c;
        final Color finalD = d1;
        
        return new ImageDescriptor(){
            public ImageData getImageData() {
                              
            	 BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                 Graphics2D gc = bimage.createGraphics();
                 configureGc(gc);
                 try{
                 	 gc.setColor( finalA );
                     gc.fillRect( 0, 0, 7, 7);
                     
                     gc.setColor( finalB );
                     gc.fillRect( 7, 0, 15, 7 ); 
                     
                     gc.setColor( finalC );
                     gc.fillRect( 0, 7, 7, 15 );
                     
                     gc.setColor( finalD );
                     gc.fillRect( 7, 7, 15, 15 );                
                     
                     gc.setColor( Color.BLACK );
                     gc.drawRect( 0, 0, 7, 7 );
                     gc.drawRect( 0, 0, 15, 7 );
                     gc.drawRect( 0, 0, 7, 15 );
                     gc.drawRect( 0, 0, 15, 15 );
                    
                 }finally{
                 	gc.dispose();
                 }
                 return extractImageDataAndDispose(bimage);
            }
        };
    }

    /**
     * Render of a color swatch allowing style.
     * <p>
     * Layout:<pre><code>
     *    0 1 2 3 4 5 6 7 8 9 101112131415
     *  0  
     *  1  dddddddddddddddddddddddddddd           
     *  2 dCCCCCCCCCCCCCCCCCCCCCcCCCCCCd
     *  3 dCCCCCCCCCCCCCCCCCCCCCCcCCCCCd                  
     *  4 dCCCCCCCCCCCCCCCCCCCCCCCcCCCCd
     *  5 dCCCCCCCCCCCCCCCCCCCCCCCCcCCCd
     *  6 dCCCCCCCCCCCCCCCCCCCCCCCCCcCCd
     *  7 dCCCCCCCCCCCCCCCCCCCCCCCCCCcCd
     *  8 dCcCCCCCCCCCCCCCCCCCCCCCCCCCCd
     *  9 dCCcCCCCCCCCCCCCCCCCCCCCCCCCCd
     * 10 dCCCcCCCCCCCCCCCCCCCCCCCCCCCCd
     * 11 dCCCCcCCCCCCCCCCCCCCCCCCCCCCCd
     * 12 dCCCCCcCCCCCCCCCCCCCCCCCCCCCCd
     * 13 ddCCCCCcCCCCCCCCCCCCCCCCCCCCdd
     * 14  ddddddddddddddddddddddddddd
     * 15    
     * </code><pre>
     * </p>
     * @param style 
     * @return Icon representing geometry style
     */
    public static ImageDescriptor swatch( Color c ) {
        Color c2=c;
        if( c==null ){
            c2=Color.GRAY;
        }else{
            c2=c;
        }
        
        final Color color = c2;
        
        int saturation = color.getRed() + color.getGreen() + color.getBlue();               
        final Color contrast = saturation < 384 ? color.brighter() : color.darker();        
        return new ImageDescriptor(){
            public ImageData getImageData() {
            	BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                Graphics2D gc = bimage.createGraphics();
                configureGc(gc);
                try{
                	gc.setColor( color );
  	                gc.fillRoundRect( 0, 0, 14, 14, 2, 2);
                  
                  	gc.setColor( contrast );
                  	gc.drawRoundRect( 0, 0, 14, 14, 2, 2 );    
                   
                }finally{
                	gc.dispose();
                }
                return extractImageDataAndDispose(bimage);
                
               
            }
        };     
    }  
    /**
     * Icon for grid data, small grid made up of provided colors.
     * Layout:<pre><code>
     *    0 1 2 3 4 5 6 7 8 9 101112131415
     *  0  
     *  1 AABBCDEEFfGgHhIiJjKkllmmnnoopp           
     *  2 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     *  3 AABBCDEEFfGgHhIiJjKkllmmnnoopp                 
     *  4 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     *  5 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     *  6 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     *  7 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     *  8 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     *  9 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     * 10 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     * 11 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     * 12 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     * 14 AABBCDEEFfGgHhIiJjKkllmmnnoopp
     * 15
     * </code><pre>
     * </p>
     * @param c palette of colors
     * @return Icon representing a palette
     * 
     */
    public static ImageDescriptor palette( Color c[]) {
    	final Color[] colors = new Color[16];
    	Color color = Color.GRAY;
    	if( c == null ){
    		for( int i=0; i<16; i++) color = Color.GRAY;
    	}
    	else {
    		for( int i=0; i<16; i++) {
    			int lookup = (i*c.length)/16;
    			if( c[ lookup ] != null ) color = c[ lookup ];
    			colors[i] = color;    			
    		}
    	}
        return new ImageDescriptor(){
            public ImageData getImageData() {
            	BufferedImage bimage = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT,BufferedImage.TYPE_4BYTE_ABGR );
                Graphics2D gc = bimage.createGraphics();
                configureGc(gc);
                try{
                	for( int i=0; i<16;i++){
                    	gc.setColor( colors[i]);
                    	gc.drawLine(i,0,i,15);
                    }
                    gc.setColor( Color.GRAY );                	
                    gc.drawRoundRect( 0, 0, 14, 14, 2, 2 );
                    
                }finally{
                	gc.dispose();
                }
                return extractImageDataAndDispose(bimage);
                
            }
        };
    }
    public static ImageDescriptor icon( SimpleFeatureType ft ) {
        if( ft==null || ft.getGeometryDescriptor()==null )
            return null;
        
        Class<?> geomType = ft.getGeometryDescriptor().getType().getBinding();
        return icon(geomType);
    }
    public static ImageDescriptor icon(Class<?> geomType) {
		if( Point.class.isAssignableFrom(geomType) 
                || MultiPoint.class.isAssignableFrom(geomType) ){
            return point(DEFAULT_BORDER, DEFAULT_FILL);
        }
        
        if( LineString.class.isAssignableFrom(geomType) 
                || MultiLineString.class.isAssignableFrom(geomType) 
                || LinearRing.class.isAssignableFrom(geomType)){
            return line(DEFAULT_BORDER, 1);
        }
        
        if( Polygon.class.isAssignableFrom(geomType) 
                || MultiPolygon.class.isAssignableFrom(geomType) ){
            return polygon(DEFAULT_BORDER, DEFAULT_FILL, 1);
        }
        
        return geometry(DEFAULT_BORDER, DEFAULT_FILL);
    }    
}
