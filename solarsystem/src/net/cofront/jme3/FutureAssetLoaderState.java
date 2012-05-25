package net.cofront.jme3;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderKey;
import com.jme3.texture.Texture;

/**
 * A threaded wrapper for the {@link AssetManager}. Assets are loaded in a separate thread(s)
 * and {@link Future}s are returned. In addition, a {@link Callback} can be used to process the 
 * result of {@link Future#get()}. The callbacks are automatically executed in the application's 
 * render thread.
 * 
 * @author Aaron Loucks
 * @version $Id: FutureAssetLoaderState.java 86 2011-05-19 23:56:28Z aloucks $
 *
 */
public class FutureAssetLoaderState extends AbstractAppState {
	
	private final static Logger log = Logger.getLogger(FutureAssetLoaderState.class.getName());
	
	/**
	 * Extend this class to submit callbacks to execute after the future is done. The callback
	 * will be excuted in the application's render thread.
	 * 
	 * @author Aaron Loucks
	 *
	 * @param <T>
	 */
	public static abstract class Callback<T> implements Runnable {
		private T object;
		/**
		 * The result of {@link Future#get()}. This is called automatically before the callback is executed.
		 * @param object The result of {@link Future#get()}. This is called automatically
		 * 
		 */
		public final void set(T object) {
			this.object = object;
		}
		/**
		 * The result of {@link Future#get()}.
		 * @return The result of {@link Future#get()}.
		 */
		public final T get() {
			return object;
		}
	}
	
	// structure to hold the future and the callback to execute
	// when the future is done.
	private class FutureCallback<T> {
		private Future<T> f;
		private Callback<T> cb;
		private FutureCallback(Future<T> f, Callback<T> cb) {
			this.f = f;
			this.cb = cb;
		}
		private boolean isDone() {
			return f.isDone();
		}
		private boolean isCancelled() {
			return f.isCancelled();
		}
		public void callback() {
			try {
				cb.set(f.get());
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error loading asset.", e);
			} 
			try {
				cb.run();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error executing callback.", e);
			}
		}
	}

	private AssetManager am;
	private AppStateManager sm;
	private ScheduledThreadPoolExecutor executor;
	private ArrayList<FutureCallback<?>> queue = new ArrayList<FutureCallback<?>>();
	private ArrayList<FutureCallback<?>> prune = new ArrayList<FutureCallback<?>>();
	private boolean autoAttaching = true;
	
	@SuppressWarnings("unused")
	private FutureAssetLoaderState() {}
	
	/**
	 * Creates an asset loader with an executor pool of 1 daemon thread.
	 * @param am The {@link AssetManager} to use.
 	 */
	public FutureAssetLoaderState(AssetManager am) {
		this(am, 1);
	}
	
	/**
	 * Creates an asset loader with an executor pool the specified number of daemon threads.
	 * @param am The {@link AssetManager} to use.
	 * @param threadCount The number of daemon threads.
	 */
	public FutureAssetLoaderState(AssetManager am, int threadCount) {
		ThreadFactory daemonFactory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		};
		this.am = am;
		this.executor = new ScheduledThreadPoolExecutor(threadCount);
		this.executor.setThreadFactory(daemonFactory);
	}
	
	/**
	 * Creates an asset loader with the given executor.
	 * @param am The {@link AssetManager} to use.
	 * @param executor The {@link ScheduledThreadPoolExecutor} to use.
	 */
	public FutureAssetLoaderState(AssetManager am, ScheduledThreadPoolExecutor executor) {
		this.am = am;
		this.executor = executor;
	}
	
	/* (non-Javadoc)
	 * @see com.jme3.app.state.AbstractAppState#update(float)
	 */
	@Override
	public void update(float tpf) {
		boolean detach = true;
		if (queue.size() > 0) {
			synchronized(queue) {
				for(FutureCallback<?> fc : queue) {
					if (fc.isDone()) {
						fc.callback();
						prune.add(fc);
					}
					else if (fc.isCancelled()) {
						prune.add(fc);
					}
				}
			}
			detach = false;
		}
		if (prune.size() > 0) {
			synchronized(queue) {
				for(FutureCallback<?> fc : prune) {
					queue.remove(fc);
				}
			}
			prune.clear();
			detach = false;
		}
		if (autoAttaching && detach) {
			sm.detach(this);
		}
	}
	
	/**
	 * Returns the current executor.
	 * @return The current {@link ScheduledThreadPoolExecutor}.
	 */
	public ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}
	
	/**
	 * Sets the executor.
	 * @param executor
	 */
	public void setExecutor(ScheduledThreadPoolExecutor executor) {
		this.executor = executor;
	}

	/**
	 * Queues the callback to be run in the application's render thread via <code>update()</code>. 
	 * The state must be attached in order for callbacks to execute.
	 * 
	 * @param f The {@link Future} to monitor.
	 * @param cb The {@link Callback} to process the results of <code>f.get()</code>.
	 */
	protected <T> void queue(final Future<T> f, final Callback<T> cb) {
		if (cb == null) {
			return;
		}
		synchronized(queue) {
			queue.add(new FutureCallback<T>(f, cb));
		}
		// re-attach to the state manager if needed.
		if (autoAttaching && sm != null && !sm.hasState(this)) {
			sm.attach(this);
		}
	}
	
	//@Override
	/**
	 * Locate an asset.
	 * @see com.jme3.asset.AssetManager#locateAsset
	 */
	public Future<AssetInfo> locateAsset(final AssetKey<?> key) {
		return locateAsset(key, null);
	}
	
	/**
	 * Locate an asset.
	 * @param key
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return
	 * @see com.jme3.asset.AssetManager#locateAsset
	 */
	public Future<AssetInfo> locateAsset(final AssetKey<?> key, Callback<AssetInfo> cb) {
		final Future<AssetInfo> f = executor.submit(new Callable<AssetInfo>() {
			public AssetInfo call() throws Exception {
				return am.locateAsset(key);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load an asset.
	 * @see com.jme3.asset.AssetManager#loadAsset(AssetKey)
	 */
	public <T> Future<T> loadAsset(final AssetKey<T> key) {
		return loadAsset(key, null);
	}
	
	/**
	 * Load an asset.
	 * @param key
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return
	 * @see com.jme3.asset.AssetManager#loadAsset(AssetKey)
	 */
	public <T> Future<T> loadAsset(final AssetKey<T> key, Callback<T> cb) {
		final Future<T> f = executor.submit(new Callable<T>() {
			public T call() throws Exception {
				return am.loadAsset(key);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * @see com.jme3.asset.AssetManager#loadAsset(String)
	 */
	public Future<Object> loadAsset(final String name) {
		return loadAsset(name, null);
	}
	
	/**
	 * Load an asset.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return
	 * @see com.jme3.asset.AssetManager#loadAsset(String)
	 */
	public Future<Object> loadAsset(final String name, Callback<Object> cb) {
		final Future<Object> f = executor.submit(new Callable<Object>() {
			public Object call() throws Exception {
				return am.loadAsset(name);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load a texture.
	 * @see com.jme3.asset.AssetManager#loadTexture(TextureKey)
	 */
	public Future<Texture> loadTexture(final TextureKey key) {
		return loadTexture(key, null);
	}

	/**
	 * Load a texture.
	 * @param key
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadTexture(TextureKey)
	 */
	public Future<Texture> loadTexture(final TextureKey key, Callback<Texture> cb) {
		final Future<Texture> f = executor.submit(new Callable<Texture>() {
			public Texture call() throws Exception {
				return am.loadTexture(key);
			}
		});
		queue(f, cb);
		return f;
	}
	
	//@Override
	/**
	 * @see com.jme3.asset.AssetManager#loadTexture(String)
	 */
	public Future<Texture> loadTexture(final String name) {
		return loadTexture(name, null);
	}
	
	/**
	 * Load a texture.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadTexture(String)
	 */
	public Future<Texture> loadTexture(final String name, Callback<Texture> cb) {
		final Future<Texture> f = executor.submit(new Callable<Texture>() {
			public Texture call() throws Exception {
				return am.loadTexture(name);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load audio.
	 * @see com.jme3.asset.AssetManager#loadAudio(AudioKey)
	 */
	public Future<AudioData> loadAudio(final AudioKey key) {
		return loadAudio(key, null);
	}
	
	/**
	 * Load audio.
	 * @param key
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadAudio(AudioKey)
	 */
	public Future<AudioData> loadAudio(final AudioKey key, Callback<AudioData> cb) {
		final Future<AudioData> f = executor.submit(new Callable<AudioData>() {
			public AudioData call() throws Exception {
				return am.loadAudio(key);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * @see com.jme3.asset.AssetManager#loadAudio(String)
	 */
	public Future<AudioData> loadAudio(final String name) {
		return loadAudio(name, null);
	}
	
	/**
	 * Load audio.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadAudio(String)
	 */
	public Future<AudioData> loadAudio(final String name, Callback<AudioData> cb) {
		final Future<AudioData> f = executor.submit(new Callable<AudioData>() {
			public AudioData call() throws Exception {
				return am.loadAudio(name);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load a model.
	 * @see com.jme3.asset.AssetManager#loadModel(ModelKey)
	 */
	public Future<Spatial> loadModel(final ModelKey key) {
		return loadModel(key, null);
	}
	
	/**
	 * Load a model.
	 * @param key
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadModel(ModelKey)
	 */
	public Future<Spatial> loadModel(final ModelKey key, Callback<Spatial> cb) {
		final Future<Spatial> f = executor.submit(new Callable<Spatial>() {
			public Spatial call() throws Exception {
				return am.loadModel(key);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load a model.
	 * @see com.jme3.asset.AssetManager#loadModel(String)
	 */
	public Future<Spatial> loadModel(final String name) {
		return loadModel(name, null);
	}
	
	/**
	 * Load a model.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadModel(String)
	 */
	public Future<Spatial> loadModel(final String name, Callback<Spatial> cb) {
		final Future<Spatial> f = executor.submit(new Callable<Spatial>() {
			public Spatial call() throws Exception {
				return am.loadModel(name);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load a material.
	 * @see com.jme3.asset.AssetManager#loadMaterial(String)
	 */
	public Future<Material> loadMaterial(final String name) {
		return loadMaterial(name, null);
	}
	
	/**
	 * Load a material.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadMaterial(String)
	 */
	public Future<Material> loadMaterial(final String name, Callback<Material> cb) {
		final Future<Material> f = executor.submit(new Callable<Material>() {
			public Material call() throws Exception {
				return am.loadMaterial(name);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load a shader.
	 * @see com.jme3.asset.AssetManager#loadShader(ShaderKey)
	 */
	public Future<Shader> loadShader(final ShaderKey key) {
		return loadShader(key, null);
	}
	
	/**
	 * Load a shader.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return A {@link Future} that can be used to determine if the asset has finished loading.
	 * @see com.jme3.asset.AssetManager#loadShader(ShaderKey)
	 */
	public Future<Shader> loadShader(final ShaderKey key, Callback<Shader> cb) {
		final Future<Shader> f = executor.submit(new Callable<Shader>() {
			public Shader call() throws Exception {
				return am.loadShader(key);
			}
		});
		queue(f, cb);
		return f;
	}

	//@Override
	/**
	 * Load a bitmap font.
	 * @see com.jme3.asset.AssetManager#loadFont(String)
	 */
	public Future<BitmapFont> loadFont(final String name) {
		return loadFont(name, null);
	}
	
	/**
	 * Load a bitmap font.
	 * @param name
	 * @param cb The callback to automatically execute in the render thread when the asset is loaded.
	 * @return
	 * @see com.jme3.asset.AssetManager#loadFont(String)
	 */
	public Future<BitmapFont> loadFont(final String name, Callback<BitmapFont> cb) {
		final Future<BitmapFont> f = executor.submit(new Callable<BitmapFont>() {
			public BitmapFont call() throws Exception {
				return am.loadFont(name);
			}
		});
		queue(f, cb);
		return f;
	}

	/* (non-Javadoc)
	 * @see com.jme3.app.state.AbstractAppState#stateAttached(com.jme3.app.state.AppStateManager)
	 */
	@Override
	public void stateAttached(AppStateManager stateManager) {
		super.stateAttached(stateManager);
		this.sm = stateManager;
	}

	/* (non-Javadoc)
	 * @see com.jme3.app.state.AbstractAppState#stateDetached(com.jme3.app.state.AppStateManager)
	 */
	@Override
	public void stateDetached(AppStateManager stateManager) {
		super.stateDetached(stateManager);
		this.sm = stateManager;
	}

	/**
	 * Returns whether or not this will automatically detach/re-attach when there's assets to load. 
	 * The state will automatically detach by default when all current assets are loaded and callbacks
	 * have run. If additional assets are loaded, the state will re-attach to the last state manager 
	 * passed to {@link #stateDetach(AppStateManager)}.
	 * 
	 * @return autoAttaching Default: <b>true</b>.
	 */
	public boolean isAutoAttaching() {
		return autoAttaching;
	}

	/**
	 * Sets whether or not the state should automatically detach/attach from the 
	 * {@link AppStateManager} when needed.
	 * @param autoAttaching
	 */
	public void setAutoAttaching(boolean autoAttaching) {
		this.autoAttaching = autoAttaching;
	}
}
