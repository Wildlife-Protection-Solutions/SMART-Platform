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
package org.wcs.smart.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.wcs.smart.SmartPlugIn;

/**
 * Class contains util method for file manipulations.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SmartFileUtils {

	public static Path createTempDirectory(String prefix) throws IOException {
		final Path temp;
		temp = Files.createTempFile(prefix, Long.toString(System.nanoTime()));

		Files.delete(temp);
		SmartUtils.createDirectory(temp);

		return temp;
	}

	public static void deleteTempDirectory(Path tempDir) {
		if (tempDir == null)
			return;
		try {
			SmartUtils.deleteDirectory(tempDir);
		} catch (IOException e) {
			// ignore
			SmartPlugIn.log(e.getMessage(), e);
		}
	}

	// https://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
	public static final void openFileBrowser(Path file) throws IOException {
		if (!Files.exists(file))
			Files.createDirectories(file);

		if (Desktop.isDesktopSupported()) {
			if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				Desktop.getDesktop().open(file.toAbsolutePath().toFile());
			}
		} else {
			String s = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$

			if (s.contains("mac")) { //$NON-NLS-1$
				if (runCommand("open", file)) //$NON-NLS-1$
					return;
			} else if (s.contains("linux")) { //$NON-NLS-1$
				if (runCommand("kde-open", file)) //$NON-NLS-1$
					return;
				if (runCommand("gnome-open", file)) //$NON-NLS-1$
					return;
				if (runCommand("xdg-open", file)) //$NON-NLS-1$
					return;
			}
		}
		// not supported
		return;
	}

	private static boolean runCommand(String command, Path file) {

		String[] parts = new String[] { command, file.toAbsolutePath().toString() };
		try {
			Process p = Runtime.getRuntime().exec(parts);
			if (p == null)
				return false;

			try {
				int retval = p.waitFor();
				if (retval == 0) {
					return true;
				}
				return false;
			} catch (Exception ex) {
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}
}
