package org.wcs.smart.incident.ui.newwizard;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.map.location.ISmartPoint;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

public class MapDialog extends Dialog {

	private TmpPoint point;
	private LocationSelectComposite<TmpPoint> locationComp;
	protected MapDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected void okPressed(){
		point = null;
		List<TmpPoint> pnts = locationComp.getPoints();
		if (pnts.size() > 0){
			point = pnts.get(0);
		}
		super.okPressed();
	}
	
	public TmpPoint getPoint(){
		return point;
	}
	@Override
	protected Point getInitialSize() {
		return new Point(450, 400);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		locationComp = new LocationSelectComposite<TmpPoint>(parent, SWT.SINGLE, getLayerStyle()) {
			@Override
			protected TmpPoint createNewPoint() {
				return new TmpPoint();
			}
		};

		
		getShell().setText("Select Point");
		return parent;
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private String getLayerStyle(){
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
				"<styleEntry version=\"1.0\" type=\"SLDStyle\">"+ //$NON-NLS-1$
				"&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"+ //$NON-NLS-1$
				"	&lt;sld:UserStyle xmlns=\"http://www.opengis.net/sld\""+ //$NON-NLS-1$
				"		xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\""+ //$NON-NLS-1$
				"		xmlns:gml=\"http://www.opengis.net/gml\"&gt;"+ //$NON-NLS-1$
				"		&lt;sld:Name&gt;Default Styler&lt;/sld:Name&gt;"+ //$NON-NLS-1$
				"		&lt;sld:Title /&gt;"+ //$NON-NLS-1$
				"		&lt;sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
				"			&lt;sld:Name&gt;simple&lt;/sld:Name&gt;"+ //$NON-NLS-1$
				"			&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;"+ //$NON-NLS-1$
				"			&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
				"			&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$

				//rule for not selected points (same as default)
				"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
				"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
				"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:Literal&gt;false&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
				"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
				"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
				"							&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;" + //$NON-NLS-1$
				"							&lt;sld:Fill&gt;" + //$NON-NLS-1$
		    	"							&lt;sld:CssParameter name=\"fill\"&gt;#FF0000&lt;/sld:CssParameter&gt;" + //$NON-NLS-1$
		    	"							&lt;/sld:Fill&gt;" + //$NON-NLS-1$
				"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Size&gt;10.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
				"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
				"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
				
				//rule for selected points
				"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
				"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
				"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:Literal&gt;true&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
				"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
				"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
				"							&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;" + //$NON-NLS-1$
				"							&lt;sld:Fill&gt;" + //$NON-NLS-1$
		    	"							&lt;sld:CssParameter name=\"fill\"&gt;#FFFF00&lt;/sld:CssParameter&gt;" + //$NON-NLS-1$
		    	"							&lt;/sld:Fill&gt;" + //$NON-NLS-1$
				"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Size&gt;10.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
				"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
				"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
				
				
				"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
				"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
				"</styleEntry>"; //$NON-NLS-1$
	}
	
	class TmpPoint implements ISmartPoint{

		private double x;
		private double y;
		
		@Override
		public double getX() {
			return x;
		}

		@Override
		public void setX(double x) {
			this.x = x;
		}

		@Override
		public double getY() {
			return this.y;
		}

		@Override
		public void setY(double y) {
			this.y = y;
		}
		
	}

}
