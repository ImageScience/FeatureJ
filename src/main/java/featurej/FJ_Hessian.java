package featurej;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

public class FJ_Hessian implements PlugIn, WindowListener {
	
	private static boolean largest = true;
	private static boolean middle = false;
	private static boolean smallest = true;
	
	private static boolean absolute = true;
	
	private static String scale = "1.0";
	
	private static Point position = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!FJ.check()) return;
		final ImagePlus input = FJ.imageplus();
		if (input == null) return;
		
		FJ.log(FJ.name()+" "+FJ.version()+": Hessian");
		
		GenericDialog gd = new GenericDialog(FJ.name()+": Hessian");
		gd.addCheckbox(" Largest eigenvalue of Hessian tensor     ",largest);
		gd.addCheckbox(" Middle eigenvalue of Hessian tensor     ",middle);
		gd.addCheckbox(" Smallest eigenvalue of Hessian tensor     ",smallest);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(5,0,0,0));
		gd.addCheckbox(" Absolute eigenvalue comparison     ",absolute);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(5,0,0,0));
		gd.addStringField("                Smoothing scale:",scale);
		
		if (position.x >= 0 && position.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(position);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		largest = gd.getNextBoolean();
		middle = gd.getNextBoolean();
		smallest = gd.getNextBoolean();
		absolute = gd.getNextBoolean();
		scale = gd.getNextString();
		
		if (largest || middle || smallest)
			(new FJHessian()).run(input,largest,middle,smallest,absolute,scale);
	}
	
	public void windowActivated(final WindowEvent e) { }
	
	public void windowClosed(final WindowEvent e) {
		
		position.x = e.getWindow().getX();
		position.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) { }
	
	public void windowDeactivated(final WindowEvent e) { }
	
	public void windowDeiconified(final WindowEvent e) { }
	
	public void windowIconified(final WindowEvent e) { }
	
	public void windowOpened(final WindowEvent e) { }
	
}

class FJHessian { // To avoid exceptions when ImageScience is not installed
	
	void run(final ImagePlus input, final boolean largest, final boolean middle, final boolean smallest, final boolean absolute, final String scale) {
		
		try {
			double scaleValue; try { scaleValue = Double.parseDouble(scale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }
			
			final Image image = Image.wrap(input);
			final Aspects aspects = image.aspects();
			if (!FJ_Options.isotropic) image.aspects(new Aspects());
			final Hessian hessian = new Hessian();
			hessian.messenger.log(FJ_Options.log);
			hessian.progressor.display(FJ_Options.progress);
			
			final Vector<Image> eigenimages = hessian.run(new FloatImage(image),scaleValue,absolute);
			
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
