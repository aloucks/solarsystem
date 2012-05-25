package net.cofront.solarsystem;

import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.vecmath.Vector3d;

import net.cofront.jme3.FutureAssetLoaderState;
import net.cofront.jme3.UserListener;
import net.cofront.jme3.UserListener.UserAction;

import com.jme3.asset.AssetKey;
import com.jme3.bounding.BoundingSphere;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

/**
 * The solar system.
 * 
 * @author Aaron Loucks
 * @version $Id: Main.java 138 2011-06-01 02:12:32Z aloucks $
 */
public class Main extends AbstractLoadingApp {
	
	private final static Logger log = Logger.getLogger(Main.class.getName());

	static {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				String threadInfo = "null";
				if (t != null) {
					threadInfo = ("name="+t.getName() + ", id=" + t.getId() + ", group=" + 
						t.getThreadGroup().toString() + ", state=" + t.getState().toString());
				}
				log.log(Level.SEVERE, "Uncaught Exception in thread: " + threadInfo, e);
			}
		});
	}

	private Node sunNode = new Node();
	private Node planetsNode = new Node();
	private Node orbitsNode = new Node();
	private Node indicatorsNode = new Node();
	
	private Vector3d v3dtmp1 = new Vector3d();	// render thread
	private Vector3d v3dtmp2 = new Vector3d();  // not-render thread 
	
	private float sr_scale;	// sun radius scale
	private float pr_scale;	// planet raidus scale
	private float d_scale;	// distance scale

	private float camSpeed = 6500;
	
	private Calendar date;
	private boolean slow;
	
	private DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy");
	private Vector3f dateLoc = new Vector3f();
	
	private Future<BitmapFont> f_font;
	private Future<Material> f_indicator;
	
	private Spatial currentSky;
	private Spatial skyMilkyway;
	private Spatial skyClear;
	
	private BloomFilter bf;
	
	private BitmapText dateText;
	
	private float minBlur = 0f;
	private float maxBlur = 2.7f;
		
	public static void main(String[] args) {
		Logger.getLogger("").setLevel(Level.SEVERE);
		try {
			FileHandler fh = new FileHandler("error.log");
			fh.setFormatter(new SimpleFormatter());
			Logger.getLogger("").addHandler(fh);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Main app = new Main();
		AppSettings settings = new AppSettings(false);
		settings.setTitle(rb.getString("Solar_System"));
		settings.setIcons(AppIcons.getIcons());
		settings.setSettingsDialogImage("Textures/splash.jpg");
		app.setSettings(settings);
		app.start();
	}

	@Override
	public void simpleInitApp() {
		super.simpleInitApp();
		guiNode.detachAllChildren();
		loadFPSText();

		initNifty("Interface/ui.xml", "loading");
		initProgressBar("loading");
		
		setupInput();
				
		rootNode.attachChild(sunNode);
		rootNode.attachChild(planetsNode);
		rootNode.attachChild(orbitsNode);
		
		flyCam.setMoveSpeed(camSpeed);
		flyCam.setDragToRotate(true);
		
		cam.setFrustumFar(1905000);
		
		// Start out with the camera set back and pointed at the sun.
		cam.setLocation( new Vector3f(  -28378.906f, 4143.0205f, 9969.55f ) );
		cam.setRotation( new Quaternion( 0.035670176f, 0.80625373f, -0.04887279f, 0.5884676f ) );
		//cam.setDirection( new Vector3f( 0.94542176f, -0.120789215f, -0.30263484f ) );
		
		pr_scale = 0.0040f;
		sr_scale = 0.0040f;
		d_scale = 0.000070f;
		
		PointLight pLight = new PointLight();
		pLight.setColor(ColorRGBA.White);
		rootNode.addLight(pLight);
		
		float pRadius = 10000000000f * d_scale;
		pLight.setRadius(pRadius);
		
		// this is needed for the rings.
		AmbientLight aLight = new AmbientLight();
		rootNode.addLight(aLight);
	
		date = Calendar.getInstance();
		// Technically, the date should be UTC, but it doesn't really matter.
		//date.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		f_font = monitor(loader.loadFont("Interface/Fonts/Default.fnt"));
		f_indicator = monitor(loader.loadMaterial("Materials/Indicators.j3m"));
		
		initDateText();
		
		// setup the sky
		final Future<MaterialDef> f_clearSkyDef = loader.loadAsset(new AssetKey<MaterialDef>("Common/MatDefs/Misc/Sky.j3md"));
		final Future<Texture> f_clearSkyTex = loader.loadTexture("Textures/Sky/GenericStars/face.jpg");
		
		final Future<Texture> f_skyTex_west = loadTexture("Textures/Sky/Milkyway/med/west.jpg");
		final Future<Texture> f_skyTex_east = loadTexture("Textures/Sky/Milkyway/med/east.jpg");
		final Future<Texture> f_skyTex_north = loadTexture("Textures/Sky/Milkyway/med/north.jpg");
		final Future<Texture> f_skyTex_south = loadTexture("Textures/Sky/Milkyway/med/south.jpg");
		final Future<Texture> f_skyTex_up = loadTexture("Textures/Sky/Milkyway/med/up.jpg");
		final Future<Texture> f_skyTex_down = loadTexture("Textures/Sky/Milkyway/med/down.jpg");
		
		// attach the sky, indicators, and goto the main screen
		// after everything as loaded.
		progress.monitor(futures, progressBar, new Runnable() {
			public void run()  {
				try {
					Material skyMat = new Material(f_clearSkyDef.get());
					skyClear = makeSingleSidedSky(f_clearSkyTex.get(), skyMat);				
					skyMilkyway = SkyFactory.createSky(assetManager, 
						f_skyTex_west.get(), 
						f_skyTex_east.get(), 
						f_skyTex_north.get(), 
						f_skyTex_south.get(), 
						f_skyTex_up.get(), 
						f_skyTex_down.get(),
						Vector3f.UNIT_XYZ
					);
					currentSky = skyMilkyway;
					guiNode.attachChild(indicatorsNode);
					nifty.gotoScreen("main");
					currentSky.updateGeometricState();
					viewPort.attachScene(currentSky);
					
					// we're done with this.
					loader.getExecutor().shutdown();
					
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error while executing post-loading tasks.", e);
				}
			}
		});

		String[] planets = new String[] {
			"Mercury",
			"Venus",
			"Earth",
			"Mars",
			"Jupiter",
			"Saturn",
			"Uranus",
			"Neptune",
			"Pluto"
		};
		
		final HashMap<String,Float> rings = new HashMap<String,Float>();
		rings.put("Saturn", 2.5f * 120700f); // my rings aren't perfect. scale by 2.5 to fix it.
		rings.put("Uranus", 0.8f * 120700f);
		
		final List<String> clouds = new ArrayList<String>();
		clouds.add("Earth");
		
		// create the sun
		Sphere s = new Sphere(128, 128, OrbitalElements.Sun.radius * sr_scale);
		TangentBinormalGenerator.generate(s);
		s.setTextureMode(Sphere.TextureMode.Projected);
		Geometry sun = new Geometry("Sun", s) {
			Quaternion r = new Quaternion();
			@Override
			public synchronized void updateLogicalState(float tpf) {
				super.updateLogicalState(tpf);
				r.fromAngleNormalAxis(0.5f * FastMath.DEG_TO_RAD * tpf, Vector3f.UNIT_Z);
				rotate(r);
			}
		};
		sun.rotate(Planet.Z_ADJUSTMENT);
		loadMaterial("Materials/Sun.j3m", sun, sunNode);
		
		// blur filter for the sun
		FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
		bf = new BloomFilter(BloomFilter.GlowMode.Objects);
		
		bf.setExposurePower(18f);
		bf.setBloomIntensity(1.0f);
		bf.setBlurScale(maxBlur);
		//bf.setExposureCutOff(1f);
		bf.setDownSamplingFactor(2.2f); 
		fpp.addFilter(bf);
		viewPort.addProcessor(fpp);

		// borrow the executor
		ScheduledThreadPoolExecutor ex = loader.getExecutor();
		for (final String name : planets) {
			final Planet p = new Planet(name, getOrbitalElements(name), pr_scale);
			p.adjustLocation(date, d_scale, v3dtmp1);
			planetsNode.attachChild(p);
			
			// Load the planet material. Set the material to the geometry and attach the geo to the planet node
			// in the render thread.
			loadMaterial("Materials/" + name + ".j3m", p.getGeometry(), p);
			
			// Load the rings and attach it in the render thread after it loads.
			if (rings.containsKey(p.getName())) {
				monitor(loader.loadMaterial("Materials/" + p.getName() + "-Rings.j3m", new FutureAssetLoaderState.Callback<Material>() {
					@Override
					public void run() {
						enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								p.addRings(get(), rings.get(name).floatValue());
								return null;
							}
						});
					}
				}));
			}
			
			// Load the clouds and attach it in the render thread after it loads.
			if (clouds.contains(p.getName())) {
				monitor(loader.loadMaterial("Materials/" + p.getName() + "-Clouds.j3m", new FutureAssetLoaderState.Callback<Material>() {
					@Override
					public void run() {
						enqueue(new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								p.addClouds(get());
								return null;
							}
						});
					}
				}));
			}
			
			// Load the orbit and attach it in the render thread after it loads.
			monitor(loader.loadMaterial("Materials/" + p.getName() + "-Orbit.j3m", new FutureAssetLoaderState.Callback<Material>() {
				@Override
				public void run() {
					final Geometry orbit = p.createOrbit(date, v3dtmp2, d_scale);
					orbit.setCullHint(CullHint.Never);
					enqueue(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							orbit.setMaterial(get());
							orbitsNode.attachChild(orbit);
							return null;
						}
					});
				}
			}));
			
			// Add the indicator
			monitor(ex.submit(new Runnable() {
				@Override
				public void run() {
				    try {
				    	p.setIndicator(createIndicator(p.getName()));
				    } catch (Exception e) {
				    	String name = "Unknown";
				    	if (p != null) {
				    		name = p.getName();
				    	}
				    	log.log(Level.SEVERE, "Error while setting indicator for: " + name, e);
					}
				}
			}));			
		} // for(Planets)
	} // simpleInitApp() 
	
	public OrbitalElements getOrbitalElements(String name) {
		Class<OrbitalElements> oeClass = OrbitalElements.class;
		OrbitalElements oe = null;
		try {
			oe = (OrbitalElements)oeClass.getField(name).get(null);
		} catch (Exception e) {
			log.log(Level.SEVERE, "No elements for: " + name, e);
		}
		return oe;
	}
	
	/**
	 * Add all of the user actions.
	 */
	protected void setupInput() {
		UserListener userActions = new UserListener();
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "toggle-orbits";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_O) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					toggleOrbits();
				}
			}
			
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "toggle-indicators";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_I) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					toggleIndicators();
				}
			}
			
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "toggle-clouds";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_C) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					toggleClouds();
				}
			}
			
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "toggle-sky";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_Y) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					toggleSky();
				}
			}
			
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "toggle-slow";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_LSHIFT) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					slow = true;
				}
				else{
					slow = false;
				}
			}
			
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "toggle-mousecam";
			}
			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new MouseButtonTrigger(MouseInput.BUTTON_LEFT) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					
				}
				else{
					toggleMousecam();
				}
			}
			
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-mercury";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_1) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Mercury");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-venus";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_2) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Venus");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-earth";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_3) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Earth");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-mars";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_4) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Mars");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-jupiter";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_5) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Jupiter");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-saturn";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_6) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Saturn");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-neptune";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_7) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Neptune");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-uranus";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_8) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Uranus");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			@Override
			public String getName() {
				return "goto-pluto";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_9) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed == false) {
					Planet p = (Planet)planetsNode.getChild("Pluto");
					lookAtPlanet(p);
				}
			}
		});
		userActions.register(new UserAction() {
			private final float threshold = 0.25f;
			private final float step = 0.02f;
			private float time;
			private float stepTime;
			@Override
			public String getName() {
				return "date-increase";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_F) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					synchronized(date) {
						date.add(Calendar.DAY_OF_YEAR, 1);
						for(Spatial s : planetsNode.getChildren()) {
							Planet p = (Planet)s;
							p.adjustLocation(date, d_scale, v3dtmp1);
						}
					}
				}
				else {
					time = 0;
					stepTime = 0;
				}
			}
			@Override
			public void onAnalog(float value, float tpf) {
				time += tpf;
				stepTime += tpf;
				if (time > threshold && stepTime > step) {
					synchronized(date) {
						date.add(Calendar.DAY_OF_YEAR, 1);
						for(Spatial s : planetsNode.getChildren()) {
							Planet p = (Planet)s;
							p.adjustLocation(date, d_scale, v3dtmp1);
						}
					}
					stepTime = 0;
				}
			}
		});
		userActions.register(new UserAction() {
			private final float threshold = 0.25f;
			private final float step = 0.02f;
			private float time;
			private float stepTime;
			@Override
			public String getName() {
				return "date-decrease";
			}

			@Override
			public Trigger[] getTriggers() {
				return new Trigger[] { new KeyTrigger(KeyInput.KEY_R) };
			}
			@Override
			public void onAction(boolean isPressed, float tpf) {
				if (isPressed) {
					synchronized(date) {
						date.add(Calendar.DAY_OF_YEAR, -1);
						for(Spatial s : planetsNode.getChildren()) {
							Planet p = (Planet)s;
							p.adjustLocation(date, d_scale, v3dtmp1);
						}
					}
				}
				else {
					time = 0;
					stepTime = 0;
				}
			}
			@Override
			public void onAnalog(float value, float tpf) {
				time += tpf;
				stepTime += tpf;
				if (time > threshold && stepTime > step) {
					synchronized(date) {
						date.add(Calendar.DAY_OF_YEAR, -1);
						for(Spatial s : planetsNode.getChildren()) {
							Planet p = (Planet)s;
							p.adjustLocation(date, d_scale, v3dtmp1);
						}
					}
					stepTime = 0;
				}
			}
		});
		
		
		userActions.bind(inputManager);
	}
	
	public void lookAtPlanet(Planet p) {		
		Vector3f pLoc = p.getLocalTranslation().clone();
		Vector3f dir = pLoc.clone().normalizeLocal();
		float offset = p.getBoundingSphere().getRadius()/7f;
		pLoc.subtractLocal(dir.multLocal(offset));
		
		cam.setLocation(pLoc);
		cam.lookAt(p.getLocalTranslation(), Vector3f.UNIT_Y);
		
		pLoc.subtractLocal(cam.getLeft().clone().multLocal(-offset));
		cam.setLocation(pLoc);
		
		cam.lookAt(p.getLocalTranslation(), Vector3f.UNIT_Y);
	}
	
	@Override
	public void simpleUpdate(float tpf) {
		updateDateText();
		
		boolean outside = cam.contains(sunNode.getWorldBound()).equals( FrustumIntersect.Outside );
		
		if ( outside ) {
			bf.setEnabled(false);
		}
		else {
			// scale the blur around the edge of the sun
			float d = cam.getLocation().distance(Vector3f.ZERO);
			float ratio = FastMath.clamp(d / 415000, 0, 1);
			float blurRange = maxBlur - minBlur;
			float blur = maxBlur - (ratio * blurRange);
			bf.setBlurScale( blur );
			bf.setEnabled(true);
		}
		
		Vector3f camLoc = cam.getLocation();		
		float newSpeed = camSpeed;
		
		for(Spatial s : planetsNode.getChildren()) {
			if (s instanceof Planet) {
				// adjust cam speed
				Planet p = (Planet)s;
				BoundingSphere bs = p.getBoundingSphere();
				boolean bound = bs.contains(camLoc);
				if (bound) {
					float d = bs.distanceTo(camLoc);
					float dlogd = FastMath.pow(d, 2f) * FastMath.log(d);
					float rlogr = FastMath.pow(bs.getRadius(), 2f) * FastMath.log(bs.getRadius());
					newSpeed *= dlogd / rlogr;
				}
				
				// update indicators
				Node i = p.getIndicator();
				if (i != null) {
					Vector3f pLoc = p.getWorldTranslation();
					Vector3f iLoc = cam.getScreenCoordinates(pLoc);
					i.setLocalTranslation(iLoc);
					// close to the planet or it's behind the camera
					if (bound || iLoc.z > 1) {
						i.removeFromParent();
					}
					else if (! indicatorsNode.hasChild(i) ) {
						indicatorsNode.attachChild(i);
					}
					i.setLocalTranslation(iLoc);
				}
			}
		}
		if (slow) {
			newSpeed = 0.1f * newSpeed;
		}
		float minSpeed = camSpeed / 500;
		if (Float.isNaN(newSpeed)) {
			newSpeed = minSpeed;
		} else if (newSpeed < minSpeed) {
			newSpeed = minSpeed;
		}
		flyCam.setMoveSpeed(newSpeed);
	}
	
	/**
	 * Sets the display of the orbital paths on and off.
	 */
	public void toggleOrbits() {
		if (rootNode.hasChild(orbitsNode)) {
			orbitsNode.removeFromParent();
		}
		else {
			rootNode.attachChild(orbitsNode);
		}
	}
	
	/**
	 * Sets the display of the planet indicators on and off.
	 */
	public void toggleIndicators() {
		if (guiNode.hasChild(indicatorsNode)) {
			indicatorsNode.removeFromParent();
		}
		else {
			guiNode.attachChild(indicatorsNode);
		}
	}
	
	public void toggleMousecam() {
		if (flyCam.isDragToRotate()) {
			flyCam.setDragToRotate(false);
		}
		else {
			flyCam.setDragToRotate(true);
		}
	}
	
	public void toggleClouds() {
		for(Spatial s : planetsNode.getChildren()) {
			Planet p = (Planet)s;
			p.toggleClouds();
		}
	}
	
	/**
	 * Swaps between the milky way and a clear sky.
	 */
	public void toggleSky() {
		viewPort.detachScene(currentSky);
		if (currentSky == skyMilkyway) {
			currentSky = skyClear;
		}
		else {
			currentSky = skyMilkyway;
		}
		currentSky.updateGeometricState();
		viewPort.attachScene(currentSky);
	}
	
	/**
	 * Creates the gui indicator for a planet.
	 * 
	 * @param planetName
	 * @return
	 * @throws Exception
	 */
	protected Node createIndicator(String planetName) throws Exception {
		Material m = f_indicator.get();
		Texture t = m.getTextureParam("ColorMap").getTextureValue();
		int w = t.getImage().getWidth();
		int h = t.getImage().getHeight();
		Quad q = new Quad(w, h);
		Geometry g = new Geometry(planetName + "-Indicatotr", q);
		g.setMaterial(m);
		
		BitmapText bt = new BitmapText(f_font.get(), false);
		bt.setText(rb.getString(planetName));
	    bt.setLocalTranslation(-1 * bt.getLineWidth() / 2 , h/2 + bt.getLineHeight(), 0);
	    
	    final Node indicator = new Node(g.getName());
	    indicator.attachChild(bt);
	    indicator.attachChild(g.center());
	    
	    return indicator;
	}

	protected void initDateText() {
		try {
			dateText = new BitmapText(f_font.get(), false);
			guiNode.attachChild(dateText);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error initializing date text.", e);
		}
	}
	
	
	public void updateDateText() {
		if (date != null) {
			String s = df.format(date.getTime());
			dateText.setText(s);
			int w = viewPort.getCamera().getWidth();
			int dw = (int)dateText.getLineWidth();
			int dh = (int)dateText.getLineHeight();
			dateLoc.set(w - dw - 5, dh, 0);
			dateText.setLocalTranslation(dateLoc);
		}
	}
}
