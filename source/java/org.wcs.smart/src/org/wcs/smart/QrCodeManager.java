/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

/**
 * For QR Code generation
 */
public enum QrCodeManager {

	INSTANCE;

	/**
	 * Generates qr code for URL
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public Image generateQRCode(String url) throws Exception {
				
		// Create the ByteMatrix for the QR-Code that encodes the given String
		Map<EncodeHintType, Object> hintMap = new HashMap<>();
		hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
		hintMap.put(EncodeHintType.MARGIN, Integer.valueOf(1));
		
	    QRCode code = Encoder.encode(url, ErrorCorrectionLevel.L, Collections.emptyMap());

	    int height = code.getMatrix().getHeight();
	    int width = code.getMatrix().getWidth();

		// Make the BufferedImage that are to hold the QRCode
		RGB white = new RGB(255, 255, 255);
		RGB black = new RGB(0, 0, 0);

		PaletteData bw = new PaletteData(white, black);
		ImageData data = new ImageData(width+2, height+2, 1, bw);

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (code.getMatrix().get(i, j) == 1) {
					data.setPixel(i+1, j+1, 1);
				} else {
					data.setPixel(i+1, j+1, 0);
				}
			}
		}

		return new Image(Display.getDefault(), data);
	}
}
