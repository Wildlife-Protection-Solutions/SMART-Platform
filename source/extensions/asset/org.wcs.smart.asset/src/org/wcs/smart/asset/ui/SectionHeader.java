package org.wcs.smart.asset.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class SectionHeader extends Composite{

	private static final String EVENT_KEY = "EVENT";
	private FormToolkit toolkit;
	private List<Hyperlink> links;
	
	private Font normalFont;
	private Font boldFont;
	
	public SectionHeader(Composite parent, int style, String[] headers, Listener[] actions, FormToolkit toolkit) {
		super(parent, style);
		this.toolkit = toolkit;
		createComponent(headers, actions);
	}
	
	private void createComponent(String[] headers, Listener[] actions) {
		
		setLayout(new GridLayout(headers.length, false));
		((GridLayout)getLayout()).marginWidth = 2;
		((GridLayout)getLayout()).marginHeight = 2;
		setBackground( toolkit.getColors().getColor(IFormColors.TB_BG) );
		
		FontData fd = getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(getDisplay(), fd);
		addListener(SWT.Dispose, e->boldFont.dispose());
		normalFont = getFont();
		
		links = new ArrayList<>();
		for (int i = 0; i < headers.length; i ++) {
			links.add(createHyperlink(headers[i], actions[i]));
		}
		
	}
	
	public void selectPanel(int index) {
		((IHyperlinkListener)links.get(index).getData(EVENT_KEY)).linkActivated(new HyperlinkEvent(links.get(index), null, "", 0));
	}

	private Hyperlink createHyperlink(String text, Listener action) {
		
		Hyperlink lnkEvents = toolkit.createHyperlink(this, text, SWT.NONE);
		lnkEvents.setBackground(getBackground());
		
		IHyperlinkListener listener = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				action.handleEvent(null);
				
				links.forEach(lnk->lnk.setFont(normalFont));
				lnkEvents.setFont(boldFont);
				layout();
			}
		};
		lnkEvents.addHyperlinkListener(listener);
		lnkEvents.setData(EVENT_KEY, listener);
		return lnkEvents;
	}
}
