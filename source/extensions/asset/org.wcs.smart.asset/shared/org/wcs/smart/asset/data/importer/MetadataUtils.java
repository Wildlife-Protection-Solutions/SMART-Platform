package org.wcs.smart.asset.data.importer;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.adobe.AdobeJpegDirectory;
import com.drew.metadata.bmp.BmpHeaderDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifImageDirectory;
import com.drew.metadata.exif.ExifInteropDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.PanasonicRawDistortionDirectory;
import com.drew.metadata.exif.PanasonicRawIFD0Directory;
import com.drew.metadata.exif.PanasonicRawWbInfo2Directory;
import com.drew.metadata.exif.PanasonicRawWbInfoDirectory;
import com.drew.metadata.exif.PrintIMDirectory;
import com.drew.metadata.exif.makernotes.AppleMakernoteDirectory;
import com.drew.metadata.exif.makernotes.CanonMakernoteDirectory;
import com.drew.metadata.exif.makernotes.CasioType1MakernoteDirectory;
import com.drew.metadata.exif.makernotes.CasioType2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.FujifilmMakernoteDirectory;
import com.drew.metadata.exif.makernotes.KodakMakernoteDirectory;
import com.drew.metadata.exif.makernotes.KyoceraMakernoteDirectory;
import com.drew.metadata.exif.makernotes.LeicaMakernoteDirectory;
import com.drew.metadata.exif.makernotes.LeicaType5MakernoteDirectory;
import com.drew.metadata.exif.makernotes.NikonType1MakernoteDirectory;
import com.drew.metadata.exif.makernotes.NikonType2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusCameraSettingsMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusEquipmentMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusFocusInfoMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusImageProcessingMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusRawDevelopment2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusRawDevelopmentMakernoteDirectory;
import com.drew.metadata.exif.makernotes.OlympusRawInfoMakernoteDirectory;
import com.drew.metadata.exif.makernotes.PanasonicMakernoteDirectory;
import com.drew.metadata.exif.makernotes.PentaxMakernoteDirectory;
import com.drew.metadata.exif.makernotes.ReconyxHyperFireMakernoteDirectory;
import com.drew.metadata.exif.makernotes.ReconyxUltraFireMakernoteDirectory;
import com.drew.metadata.exif.makernotes.RicohMakernoteDirectory;
import com.drew.metadata.exif.makernotes.SamsungType2MakernoteDirectory;
import com.drew.metadata.exif.makernotes.SanyoMakernoteDirectory;
import com.drew.metadata.exif.makernotes.SigmaMakernoteDirectory;
import com.drew.metadata.exif.makernotes.SonyType1MakernoteDirectory;
import com.drew.metadata.exif.makernotes.SonyType6MakernoteDirectory;
import com.drew.metadata.file.FileMetadataDirectory;
import com.drew.metadata.gif.GifAnimationDirectory;
import com.drew.metadata.gif.GifControlDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.gif.GifImageDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.ico.IcoDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.jfxx.JfxxDirectory;
import com.drew.metadata.jpeg.HuffmanTablesDirectory;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.pcx.PcxDirectory;
import com.drew.metadata.photoshop.DuckyDirectory;
import com.drew.metadata.photoshop.PhotoshopDirectory;
import com.drew.metadata.photoshop.PsdHeaderDirectory;
import com.drew.metadata.png.PngChromaticitiesDirectory;
import com.drew.metadata.webp.WebpDirectory;
import com.drew.metadata.xmp.XmpDirectory;

public class MetadataUtils {

	
	private static Directory[] supportedDirectories = new Directory[] {
		new AdobeJpegDirectory(),
		new AppleMakernoteDirectory(),
		new BmpHeaderDirectory(),
		new CanonMakernoteDirectory(),
		new CasioType1MakernoteDirectory(),
		new CasioType2MakernoteDirectory(),
		new DuckyDirectory(),
		new ExifIFD0Directory(),
		new ExifImageDirectory(),
		new ExifInteropDirectory(),
		new ExifThumbnailDirectory(),
		new GpsDirectory(),
		new FileMetadataDirectory(),
		new FujifilmMakernoteDirectory(),
		new GifAnimationDirectory(),
		new GifControlDirectory(),
		new GifHeaderDirectory(),
		new GifImageDirectory(),
		new HuffmanTablesDirectory(),
		new IccDirectory(),
		new IcoDirectory(),
		new IptcDirectory(),
		new JfifDirectory(),
		new JfxxDirectory(),
		new JpegCommentDirectory(),
		new JpegDirectory(),
		new KodakMakernoteDirectory(),
		new KyoceraMakernoteDirectory(),
		new LeicaMakernoteDirectory(),
		new LeicaType5MakernoteDirectory(),
		new NikonType1MakernoteDirectory(),
		new NikonType2MakernoteDirectory(),
		new OlympusCameraSettingsMakernoteDirectory(),
		new OlympusEquipmentMakernoteDirectory(),
		new OlympusFocusInfoMakernoteDirectory(),
		new OlympusImageProcessingMakernoteDirectory(),
		new OlympusMakernoteDirectory(),
		new OlympusRawDevelopmentMakernoteDirectory(),
		new OlympusRawDevelopment2MakernoteDirectory(),
		new OlympusRawInfoMakernoteDirectory(), 
		new PanasonicMakernoteDirectory(),
		new PanasonicRawDistortionDirectory(),
		new PanasonicRawIFD0Directory(),
		new PanasonicRawWbInfo2Directory(),
		new PanasonicRawWbInfoDirectory(),
		new PcxDirectory(),
		new PentaxMakernoteDirectory(),
		new PhotoshopDirectory(),
		new PngChromaticitiesDirectory(),
//		new PngDirectory(),
		new PrintIMDirectory(),
		new PsdHeaderDirectory(),
		new ReconyxHyperFireMakernoteDirectory(),
		new ReconyxUltraFireMakernoteDirectory(),
		new RicohMakernoteDirectory(),
		new SamsungType2MakernoteDirectory(),
		new SanyoMakernoteDirectory(),
		new SigmaMakernoteDirectory(), 
		new SonyType1MakernoteDirectory(),
		new SonyType6MakernoteDirectory(),
		new WebpDirectory(),
		new XmpDirectory(),
	};
	
	public static Tag findTagName(int tagType) {
		for (Directory d : supportedDirectories) {
			if (d.hasTagName(tagType)) {
				return new Tag(tagType, d);
			}
		}
		return null;
	}
}
