package featurej;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.LUT;

import imagescience.ImageScience;
import imagescience.image.Image;
import imagescience.utility.I5DResource;

public final class FJ {
	
	public static String name() {
		
		return "FeatureJ";
	}
	
	public static String version() {
		
		final String version = FJ.class.getPackage().getImplementationVersion();
		
		return (version == null) ? "DEV" : version;
	}
	
	private static final String MINIMUM_IMAGEJ_VERSION = "1.50a";
	
	private static final String MINIMUM_IMAGESCIENCE_VERSION = "3.0.0";
	
	static boolean check() {
		
		if (IJ.getVersion().compareTo(MINIMUM_IMAGEJ_VERSION) < 0) {
			error("This plugin requires ImageJ version "+MINIMUM_IMAGEJ_VERSION+" or higher");
			return false;
		}
		
		try { // Also works if ImageScience is not installed
			if (ImageScience.version().compareTo(MINIMUM_IMAGESCIENCE_VERSION) < 0)
				throw new IllegalStateException();
		} catch (Throwable e) {
			error("This plugin requires ImageScience version "+MINIMUM_IMAGESCIENCE_VERSION+" or higher");
			return false;
		}
		
		return true;
	}
	
	static ImagePlus imageplus() {
		
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)  {
			error("There are no images open");
			return null;
		}
		
		final int type = imp.getType();
		if (type != ImagePlus.GRAY8 && type != ImagePlus.GRAY16 && type != ImagePlus.GRAY32) {
			error("The image is not a gray-scale image");
			return null;
		}
		
		return imp;
	}
	
	static void show(final Image img, final ImagePlus imp) {
		
		ImagePlus output = img.imageplus();
		output.setCalibration(imp.getCalibration());
		final double[] minmax = img.extrema();
		final double min = minmax[0], max = minmax[1];
		output.setDisplayRange(min,max);
		
		switch (type(imp)) {
			
			case IMAGE5D: {
				output = I5DResource.convert(output,true);
				I5DResource.transfer(imp,output);
				I5DResource.minmax(output,min,max);
				I5DResource.mode(output,I5DResource.GRAY);
				break;
			}
			case COMPOSITEIMAGE: {
				final CompositeImage composite = new CompositeImage(output);
				composite.copyLuts(imp);
				composite.setMode(CompositeImage.GRAYSCALE);
				final int nc = composite.getNChannels();
				for (int c=1; c<=nc; ++c) {
					final LUT lut = composite.getChannelLut(c);
					lut.min = min; lut.max = max;
				}
				output = composite;
				break;
			}
			case HYPERSTACK: {
				output.setOpenAsHyperStack(true);
				break;
			}
		}
		
		output.changes = FJ_Options.save;
		
		log("Showing result image");
		output.show();
	}
	
	static void close(final ImagePlus imp) {
		
		if (FJ_Options.close) {
			log("Closing input image");
			imp.close();
		}
	}
	
	static final int SINGLEIMAGE=1, IMAGESTACK=2, HYPERSTACK=3, COMPOSITEIMAGE=4, IMAGE5D=5;
	
	static int type(final ImagePlus imp) {
		
		int type = SINGLEIMAGE;
		boolean i5dexist = false;
		try { Class.forName("i5d.Image5D"); i5dexist = true; } catch (Throwable e) { }
		if (i5dexist && I5DResource.instance(imp)) type = IMAGE5D;
		else if (imp.isComposite()) type = COMPOSITEIMAGE;
		else if (imp.isHyperStack()) type = HYPERSTACK;
		else if (imp.getImageStackSize() > 1) type = IMAGESTACK;
		return type;
	}
	
	static void error(final String message) {
		
		IJ.showMessage(name()+": Error",message+".");
		IJ.showProgress(1);
		IJ.showStatus("");
	}
	
	static void log(final String message) {
		
		if (FJ_Options.log) IJ.log(message);
	}
	
}
