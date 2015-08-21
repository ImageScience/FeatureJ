package featurej;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import imagescience.feature.Differentiator;
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

public class FJ_Derivatives implements PlugIn, WindowListener {
	
	private static int xorder = 0;
	private static int yorder = 0;
	private static int zorder = 0;
	
	private static String scale = "1.0";
	
	private static Point position = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!FJ.check()) return;
		final ImagePlus input = FJ.imageplus();
		if (input == null) return;
		
		FJ.log(FJ.name()+" "+FJ.version()+": Derivatives");
		
		final String[] orders = new String[11];
		for (int i=0; i<11; ++i) orders[i] = String.valueOf(i);
		
		GenericDialog gd = new GenericDialog(FJ.name()+": Derivatives");
		gd.setInsets(0,0,0);
		gd.addMessage("Differentiation orders:");
		gd.addChoice("x-Order:",orders,orders[xorder]);
		gd.addChoice("y-Order:",orders,orders[yorder]);
		gd.addChoice("z-Order:",orders,orders[zorder]);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		gd.addStringField("Smoothing scale:",scale);
		
		if (position.x >= 0 && position.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(position);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		xorder = gd.getNextChoiceIndex();
		yorder = gd.getNextChoiceIndex();
		zorder = gd.getNextChoiceIndex();
		scale = gd.getNextString();
		
		(new FJDerivatives()).run(input,xorder,yorder,zorder,scale);
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

class FJDerivatives { // To avoid exceptions when ImageScience is not installed
	
	void run(final ImagePlus input, final int xorder, final int yorder, final int zorder, final String scale) {
		
		try {
			final Image image = Image.wrap(input);
			final Aspects aspects = image.aspects();
			if (!FJ_Options.isotropic) image.aspects(new Aspects());
			double scaleValue; try { scaleValue = Double.parseDouble(scale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }
			final Image output = new FloatImage(image);
			final Differentiator differentiator = new Differentiator();
			differentiator.messenger.log(FJ_Options.log);
			differentiator.progressor.display(FJ_Options.progress);
			differentiator.run(output,scaleValue,xorder,yorder,zorder);
			output.aspects(aspects);
			FJ.show(output,input);
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
