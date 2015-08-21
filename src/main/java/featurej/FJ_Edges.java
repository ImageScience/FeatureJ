package featurej;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import imagescience.feature.Edges;
import imagescience.image.Aspects;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.segment.Thresholder;
import imagescience.utility.Progressor;

import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class FJ_Edges implements PlugIn, ItemListener, WindowListener {
	
	private static boolean compute = true;
	private static boolean suppress = false;
	
	private static String scale = "1.0";
	private static String lower = "";
	private static String higher = "";
	
	private Checkbox computebox, suppressbox;
	
	private static Point position = new Point(-1,-1);
	
	public void run(String arg) {
		
		if (!FJ.check()) return;
		final ImagePlus input = FJ.imageplus();
		if (input == null) return;
		
		FJ.log(FJ.name()+" "+FJ.version()+": Edges");
		
		GenericDialog gd = new GenericDialog(FJ.name()+": Edges");
		gd.addCheckbox(" Compute gradient-magnitude image     ",compute);
		gd.addStringField("                Smoothing scale:",scale);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		gd.addCheckbox(" Suppress non-maximum gradients     ",suppress);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(0,0,0,0));
		gd.addStringField("                Lower threshold value:",lower);
		gd.addStringField("                Higher threshold value:",higher);
		computebox = (Checkbox)gd.getCheckboxes().get(0); computebox.addItemListener(this);
		suppressbox = (Checkbox)gd.getCheckboxes().get(1); suppressbox.addItemListener(this);
		
		if (position.x >= 0 && position.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(position);
		} else gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		compute = gd.getNextBoolean();
		scale = gd.getNextString();
		suppress = gd.getNextBoolean();
		lower = gd.getNextString();
		higher = gd.getNextString();
		
		if (compute || suppress || !lower.isEmpty() || !higher.isEmpty())
			(new FJEdges()).run(input,compute,scale,suppress,lower,higher);
	}
	
	public void itemStateChanged(final ItemEvent e) {
		
		if (e.getSource() == computebox) {
			if (!computebox.getState()) suppressbox.setState(false);
		} else if (e.getSource() == suppressbox) {
			if (suppressbox.getState()) computebox.setState(true);
		}
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

class FJEdges { // To avoid exceptions when ImageScience is not installed
	
	void run(final ImagePlus input, final boolean compute, final String scale, final boolean suppress, final String lower, final String higher) {
		
		try {
			double scaleValue, lowValue=0, highValue=0;
			boolean lowThreshold = true, highThreshold = true;
			try { scaleValue = Double.parseDouble(scale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }
			try { if (lower.isEmpty()) lowThreshold = false; else lowValue = Double.parseDouble(lower); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid lower threshold value"); }
			try { if (higher.isEmpty()) highThreshold = false; else highValue = Double.parseDouble(higher); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid higher threshold value"); }
			final int thresholdMode = (lowThreshold ? 10 : 0) + (highThreshold ? 1 : 0);
			
			final Image image = Image.wrap(input);
			Image output = new FloatImage(image);
			
			double[] pls = {0, 1}; int pl = 0;
			if ((compute || suppress) && thresholdMode > 0)
				pls = new double[] {0, 0.9, 1};
			final Progressor progressor = new Progressor();
			progressor.display(FJ_Options.progress);
			
			if (compute || suppress) {
				progressor.range(pls[pl],pls[++pl]);
				final Aspects aspects = output.aspects();
				if (!FJ_Options.isotropic) output.aspects(new Aspects());
				final Edges edges = new Edges();
				edges.messenger.log(FJ_Options.log);
				edges.progressor.parent(progressor);
				output = edges.run(output,scaleValue,suppress);
				output.aspects(aspects);
			}
			
			if (thresholdMode > 0) {
				progressor.range(pls[pl],pls[++pl]);
				final Thresholder thresholder = new Thresholder();
				thresholder.messenger.log(FJ_Options.log);
				thresholder.progressor.parent(progressor);
				switch (thresholdMode) {
					case 1: { thresholder.hard(output,highValue); break; }
					case 10: { thresholder.hard(output,lowValue); break; }
					case 11: { thresholder.hysteresis(output,lowValue,highValue); break; }
				}
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
