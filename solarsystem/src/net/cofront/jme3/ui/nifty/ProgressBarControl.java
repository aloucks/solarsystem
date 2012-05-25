package net.cofront.jme3.ui.nifty;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.xml.xpp3.Attributes;

/**
 * 
 * @author Aaron
 * @see <a href="http://sourceforge.net/apps/mediawiki/nifty-gui/index.php?title=Resizable_Images_%28ImageMode%3Dresize%29_explained">http://sourceforge.net/apps/mediawiki/nifty-gui/index.php?title=Resizable_Images_%28ImageMode%3Dresize%29_explained</a>
 * @see <a href="http://sourceforge.net/apps/mediawiki/nifty-gui/index.php?title=Create_your_own_Control_%28A_Nifty_Progressbar%29">http://sourceforge.net/apps/mediawiki/nifty-gui/index.php?title=Create_your_own_Control_%28A_Nifty_Progressbar%29</a>
 */
public class ProgressBarControl implements Controller {

	private static Logger log = Logger.getLogger(ProgressBarControl.class.getName());
	
	private Element bar;
	private Element parent;
	
	private int minWidth;
	private float progress;
	
	@Override
	public void bind(Nifty nifty, Screen screen, Element element, Properties props,
			Attributes attr) {
		bar = screen.findElementByName("#bar");
		parent = bar.getParent();
		minWidth = bar.getWidth();			
		if (parent.getHeight() < bar.getHeight()) {
			log.log(Level.INFO, "ProgressBarControl: '"+parent.getId() + "' has a height ("+parent.getHeight()+"px) less than the bar's minimum value (" + bar.getHeight() + "px) for this style.");
		}
		if (parent.getWidth() < bar.getWidth()) {
			log.log(Level.INFO, "ProgressBarControl: '"+parent.getId() + "' has a width ("+parent.getWidth()+"px) less than the bar's minimum value (" + bar.getWidth() + "px) for this style.");
		}
		bar.setConstraintHeight(parent.getConstraintHeight());
		setProgress(0);
	}
	
	/**
	 * Sets the progress bar to the given percentage.
	 * @param progress <code>0 -> 1</code>
	 */
	public void setProgress(float progress) {
		this.progress = progress;
		if (progress > 0 && parent.isVisible()) {
	    	bar.show();
	    }
	    else {
	    	bar.hide();
	    	return;
	    }
		
	    int pixelWidth = (int)(parent.getWidth() * progress);
	    
	    if (pixelWidth < 0) 
	    	pixelWidth = 0;
	    
	    if (pixelWidth > parent.getWidth()) 
	    	pixelWidth = parent.getWidth();
	    
	    if (pixelWidth < minWidth) 
	    	pixelWidth = minWidth;
	    
	    bar.setConstraintWidth(new SizeValue(pixelWidth + "px"));
	    parent.layoutElements();
	}
	
	/**
	 * Hide the progress bar.
	 */
	public void hide() {
		bar.hide();
		parent.hide();
	}
	
	/**
	 * Display the progress bar.
	 */
	public void show() {
		parent.show();
		setProgress(progress);
	}

	@Override
	public void init(Properties props, Attributes attr) {	
	}

	@Override
	public boolean inputEvent(NiftyInputEvent inputEvent) {
		return false;
	}

	@Override
	public void onFocus(boolean hasFocus) {
	}

	@Override
	public void onStartScreen() {
	}
}
