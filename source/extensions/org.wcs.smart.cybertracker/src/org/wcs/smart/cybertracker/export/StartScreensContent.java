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
package org.wcs.smart.cybertracker.export;

import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.elements.Elements.List.Items.Item;

/**
 * Class that is responsible for providing content for start screens.
 * @author elitvin
 * @since 4.0.0
 */
public class StartScreensContent {

	private CyberTrackerId startScreenItemId;
	private String beginScreenName;
	private CyberTrackerId beginScreenItemId;
	
	private StartScreensContent() {
	}
	
	public CyberTrackerId getStartScreenItemId() {
		return startScreenItemId;
	}
	public String getBeginScreenName() {
		return beginScreenName;
	}
	public CyberTrackerId getBeginScreenItemId() {
		return beginScreenItemId;
	}

	public static StartScreensContent create(Elements elements, String startScreenItemLabel, String beginScreenName, String beginScreenItemLabel, String dataType) {
		CyberTrackerId id = new CyberTrackerId();
		Item item = ElementsUtil.addElementsItem(elements, startScreenItemLabel, id.getItemId());
		item.setJsonId(dataType);

		CyberTrackerId id2 = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, beginScreenItemLabel, id2.getItemId());
		
		return create(id, beginScreenName, id2);
	}
	
	public static StartScreensContent create(Elements elements, String startScreenItemLabel, String beginScreenName, CyberTrackerId beginScreenItemId, String dataType) {
		CyberTrackerId id = new CyberTrackerId();
		Item item = ElementsUtil.addElementsItem(elements, startScreenItemLabel, id.getItemId());
		item.setJsonId(dataType);
		
		return create(id, beginScreenName, beginScreenItemId);
	}

	private static StartScreensContent create(CyberTrackerId startScreenItemId, String beginScreenName, CyberTrackerId beginScreenItemId) {
		StartScreensContent result = new StartScreensContent();
		result.startScreenItemId = startScreenItemId;
		result.beginScreenName = beginScreenName;
		result.beginScreenItemId = beginScreenItemId;
		return result;
		
	}
}
