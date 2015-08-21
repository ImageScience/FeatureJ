package featurej;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import imagescience.feature.Laplacian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.segment.ZeroCrosser;
import imagescience.utility.Progressor;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class FJ_Laplacian implements PlugIn, WindowListener {
	
	private static boolean compute = true;
	private static boolean zerocross = false;
	
	private static String scale = "1.0";
	
	private static Point position = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!FJ.check()) return;
		final ImagePlus input = FJ.imageplus();
		if (input == null) return;
		
		FJ.log(FJ.name()+" "+FJ.version()+": Laplacian");
		
		GenericDialog gd = new GenericDialog(FJ.name()+": Laplacian");
		gd.addCheckbox(" Compute Laplacian image    ",compute);
		gd.addStringField("                Smoothing scale:",scale);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(1,0,0,0));
		gd.addCheckbox(" Detect zero-crossings    ",zerocross);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		
		if (position.x >= 0 && position.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(position);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		compute = gd.getNextBoolean();
		scale = gd.getNextString();
		zerocross = gd.getNextBoolean();
		
		if (compute || zerocross)
			(new FJLaplacian()).run(input,compute,scale,zerocross);
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

class FJLaplacian { // To avoid exceptions when ImageScience is not installed
	
	void run(final ImagePlus input, final boolean compute, final String scale, final boolean zerocross) {
		
		try {
			double scaleValue; try { scaleValue = Double.parseDouble(scale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }
			
			final Image image = Image.wrap(input);
			Image output = new FloatImage(image);
			
			double[] pls = {0, 1}; int pl = 0;
			if (compute && zerocross)
				pls = new double[] {0, 0.95, 1};
			final Progressor progressor = new Progressor();
			progressor.display(FJ_Options.progress);
			
			if (compute) {
				progressor.range(pls[pl],pls[++pl]);
				final Aspects aspects = output.aspects();
				if (!FJ_Options.isotropic) output.aspects(new Aspects());
				final Laplacian laplace = new Laplacian();
				laplace.messenger.log(FJ_Options.log);
				laplace.progressor.parent(progressor);
				output = laplace.run(output,scaleValue);
				output.aspects(aspects);
			}
			
			if (zerocross) {
				progressor.range(pls[pl],pls[++pl]);
				final ZeroCrosser zerocrosser = new ZeroCrosser();
				zerocrosser.messenger.log(FJ_Options.log);
				zerocrosser.progressor.parent(progressor);
				zerocrosser.run(output);
			}
			
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
