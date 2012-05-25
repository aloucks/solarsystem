package net.cofront.solarsystem;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class AppIcons {
	public final static Logger log = Logger.getLogger(AppIcons.class.getName());
	public static BufferedImage[] getIcons() {
		BufferedImage[] icons = new BufferedImage[2];
		try {
			icons[0] = ImageIO.read( ClassLoader.getSystemResourceAsStream("Interface/Icons/default_32x32.png") );
			icons[1] = ImageIO.read( ClassLoader.getSystemResourceAsStream("Interface/Icons/default_16x16.png") );
		} catch (IOException e) {
			log.log(Level.SEVERE, "Unable to load icon(s).", e);
		}
		return icons;
	}
}
