package net.cofront.solarsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.cofront.jme3.FutureAssetLoaderState;
import net.cofront.jme3.ProgressMonitorState;
import net.cofront.jme3.ui.nifty.ProgressBarControl;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetKey;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;

/**
 * Sets up some convenience methods and the loading bar.
 * 
 * @author Aaron Loucks
 * @version $Id: AbstractLoadingApp.java 132 2011-05-31 18:16:34Z aloucks $
 */
public abstract class AbstractLoadingApp extends SimpleApplication {

	protected List<Future<?>> futures = Collections.synchronizedList( new ArrayList<Future<?>>() );;
	protected FutureAssetLoaderState loader;
	protected ProgressMonitorState progress;
	protected NiftyJmeDisplay njd;
	protected Nifty nifty;
	protected ProgressBarControl progressBar;
	protected static ResourceBundle rb = ResourceBundle.getBundle("Locals/i18n");
	
	// add to the list of futures that the progress bar will monitor
	protected <T> Future<T> monitor(Future<T> f) {
		futures.add(f);
		return f;
	}
	
	protected Future<Material> loadMaterial(String name) {
		return loadMaterial(name, null, null);
	}
	
	protected Future<Material> loadMaterial(String name, final Callable<Void> enqueued) {
		Future<Material> f = loader.loadMaterial(name, new FutureAssetLoaderState.Callback<Material>() {
			public void run() {
				enqueue(enqueued);
			}
		});
		monitor(f);
		return f;
	}
	
	// convenience method
	protected Future<Material> loadMaterial(String name, final Spatial spatial, final Node node) {
		FutureAssetLoaderState.Callback<Material> callback = new FutureAssetLoaderState.Callback<Material>() {
			public void run() {
				Material m = get();
				if (spatial != null) {
					spatial.setMaterial(m);
					if (node != null) {
						node.attachChild(spatial);
					}
				}
			}
		};
		Future<Material> f = loader.loadMaterial(name, callback);
		monitor(f);
		return f;
	}
	
	protected Future<Texture> loadTexture(String name) {
		Future<Texture> f = loader.loadTexture(name);
		monitor(f);
		return f;
	}
	
	protected Future<MaterialDef> loadMaterialDef(String name) {
		Future<MaterialDef> f = loader.loadAsset(new AssetKey<MaterialDef>(name));
		monitor(f);
		return f;
	}

	/* (non-Javadoc)
	 * @see com.jme3.app.SimpleApplication#simpleInitApp()
	 */
	@Override
	public void simpleInitApp() {
		loader = new FutureAssetLoaderState(assetManager, 3);
		progress = new ProgressMonitorState();
		stateManager.attach(loader);
		stateManager.attach(progress);
	}
	
	public void initNifty(String xmlPath, String startScreen) {
		njd = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
		guiViewPort.addProcessor(njd);
		nifty = njd.getNifty();
		if (startScreen == null)
			nifty.fromXmlWithoutStartScreen(xmlPath);
		else
			nifty.fromXml(xmlPath, startScreen);
	}
	
	public void initProgressBar(String screenName) {
		Screen screen = nifty.getScreen(screenName);
		progressBar = screen.findControl("progressBar", ProgressBarControl.class);
	}
	
	protected Geometry makeSingleSidedSky(Texture sidesTexture, Material skyMat) {
		Geometry sky = new Geometry("Sky", new Sphere(10, 10, 10, false, true));
		sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);
        // all sides will be the same
        Image sides = sidesTexture.getImage();
        Image cubeImage = new Image(sides.getFormat(), sides.getWidth(), sides.getHeight(), null);
        for (int i=0;i<6;i++) {
        	cubeImage.addData(sides.getData(0));
        }
        TextureCubeMap cubeMap = new TextureCubeMap(cubeImage);
        cubeMap.setAnisotropicFilter(0);
        cubeMap.setMagFilter(Texture.MagFilter.Bilinear);
        cubeMap.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        cubeMap.setWrap(Texture.WrapMode.EdgeClamp);
        //Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");
        skyMat.setTexture("Texture", cubeMap);
        skyMat.setVector3("NormalScale", Vector3f.UNIT_XYZ);
        sky.setMaterial(skyMat);
        sky.updateGeometricState();
        return sky;
	}

}
