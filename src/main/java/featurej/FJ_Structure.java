package featurej;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import imagescience.feature.Structure;
import imagescience.image.Aspects;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.Progressor;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

public class FJ_Structure implements PlugIn, WindowListener {
	
	private static boolean largest = true;
	private static boolean middle = false;
	private static boolean smallest = true;
	
	private static String sscale = "1.0";
	private static String iscale = "3.0";
	
	private static Point pos = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!FJ.check()) return;
		final ImagePlus imp = FJ.imageplus();
		if (imp == null) return;
		
		FJ.log(FJ.name()+" "+FJ.version()+": Structure");
		
		GenericDialog gd = new GenericDialog(FJ.name()+": Structure");
		gd.addCheckbox(" Largest eigenvalue of structure tensor    ",largest);
		gd.addCheckbox(" Middle eigenvalue of structure tensor    ",middle);
		gd.addCheckbox(" Smallest eigenvalue of structure tensor    ",smallest);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(5,0,0,0));
		gd.addStringField("                Smoothing scale:",sscale);
		gd.addStringField("                Integration scale:",iscale);
		
		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		largest = gd.getNextBoolean();
		middle = gd.getNextBoolean();
		smallest = gd.getNextBoolean();
		sscale = gd.getNextString();
		iscale = gd.getNextString();
		
		if (largest || middle || smallest)
			(new FJStructure()).run(imp,largest,middle,smallest,sscale,iscale);
	}
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) {
		
		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) { }
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}

class FJStructure { // To avoid exceptions when ImageScience is not installed
	
	void run(final ImagePlus input, final boolean largest, final boolean middle, final boolean smallest, final String sscale, final String iscale) {
		
		try {
			double sscaleval, iscaleval;
			try { sscaleval = Double.parseDouble(sscale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }
			try { iscaleval = Double.parseDouble(iscale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid integration scale value"); }
			
			final Image img = Image.wrap(input);
			final Aspects aspects = img.aspects();
			if (!FJ_Options.isotropic) img.aspects(new Aspects());
			final Structure structure = new Structure();
			structure.messenger.log(FJ_Options.log);
			structure.progressor.display(FJ_Options.progress);
			
			final Vector<Image> eigenimages = structure.run(new FloatImage(img),sscaleval,iscaleval);
			
			final int size = eigenimages.size();
			for (int i=0; i<size; ++i)
				eigenimages.get(i).aspects(aspects);
			if (size == 2) {
				if (largest) FJ.show(eigenimages.get(0),input);
				if (smallest) FJ.show(eigenimages.get(1),input);
			} else if (size == 3) {
				if (largest) FJ.show(eigenimages.get(0),input);
				if (middle) FJ.show(eigenimages.get(1),input);
				if (smallest) FJ.show(eigenimages.get(2),input);
			}
			
			FJ.close(input);
			
		} catch (OutOfMemoryError e) {
			FJ.error("Not enough memory for this operation");
			
		} catch (IllegalArgumentException e) {
			FJ.error(e.getMessage());
			
		} catch (IllegalStateException e) {
			FJ.error(e.getMessage());
			
		} catch (Throwable e) {
			FJ.error("An unidentified error occurred while running the plugin");
			
		}
	}
	
}
