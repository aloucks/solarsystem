package net.cofront.jme3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.cofront.jme3.ui.nifty.ProgressBarControl;

import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

/**
 * Monitors lists of {@link Future}s and updates {@link ProgressBarControl}s to show
 * the percentage of completed tasks.
 * 
 * @author Aaron Loucks
 * @version $Id: ProgressMonitorState.java 86 2011-05-19 23:56:28Z aloucks $
 *
 */
public class ProgressMonitorState extends AbstractAppState {
	
	private final static Logger log = Logger.getLogger(ProgressMonitorState.class.getName());
	
	private class FuturesControl {
		private List<Future<?>> futures;
		private ProgressBarControl progressBar;
		private Runnable callback;
		private FuturesControl(List<Future<?>>futures, ProgressBarControl progressBar, Runnable callback) {
			this.futures = futures;
			this.progressBar = progressBar;
			this.callback = callback;
		}
	}
	
	private List<FuturesControl> loadingList = Collections.synchronizedList(new ArrayList<FuturesControl>());
	private List<FuturesControl> pruneList = new ArrayList<FuturesControl>();
	private AppStateManager sm;
	private boolean autoAttaching = true;
	
	/**
	 * Adds a list of futures to monitor and updates the progress bar accordingly. 
	 * Note: if the list is updated outside of the render thread, it needs to be 
	 * backed by {@link Collections#synchronizedList()}.
	 * @param futures
	 * @param progressBar
	 */
	public void monitor(List<Future<?>> futures, ProgressBarControl progressBar) {
		monitor(futures, progressBar, null);
	}
	
	/**
	 * Adds a list of futures to monitor and updates the progress bar accordingly. 
	 * Note: if the list is updated outside of the render thread, it needs to be 
	 * backed by {@link Collections#synchronizedList()}.
	 * 
	 * @param futures
	 * @param progressBar
	 * @param callback Execute a callback when all futures are done or cancelled. 
	 */
	public void monitor(List<Future<?>> futures, ProgressBarControl progressBar, Runnable callback) {
		loadingList.add(new FuturesControl(futures, progressBar, callback));
		// re-attach to the statemanager if needed
		if (autoAttaching && sm != null && !sm.hasState(this)) {
			sm.attach(this);
		}
	}

	/* (non-Javadoc)
	 * @see com.jme3.app.state.AbstractAppState#stateAttached(com.jme3.app.state.AppStateManager)
	 */
	@Override
	public void stateAttached(AppStateManager stateManager) {
		super.stateAttached(stateManager);
		sm = stateManager;
	}

	/* (non-Javadoc)
	 * @see com.jme3.app.state.AbstractAppState#stateDetached(com.jme3.app.state.AppStateManager)
	 */
	@Override
	public void stateDetached(AppStateManager stateManager) {
		super.stateDetached(stateManager);
		sm = stateManager;
	}
	
	/* (non-Javadoc)
	 * @see com.jme3.app.state.AbstractAppState#update(float)
	 */
	@Override
	public synchronized void update(float tpf) {
		boolean detach = true;
		// run the callbacks for any batches that are 100% complete and
		// remove these from the controls we're monitoring.
		if (pruneList.size() > 0) {
			for(FuturesControl fc : pruneList) {
				try {
					if (fc.callback != null) {
						fc.callback.run();
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error while running callback.", e);
				}
				loadingList.remove(fc);
			}
			pruneList.clear();
			detach = false;
		}
		
		
		// update the progressbar controls.
		if (loadingList.size() > 0) {
			synchronized (loadingList) { 
				for(FuturesControl fc : loadingList) {
					int jobCount = fc.futures.size();
					if (jobCount > 0) {
						int jobsDone = 0;
						synchronized(fc.futures) {
							for (Future<?> f : fc.futures) {
								jobsDone += f.isDone() || f.isCancelled() ? 1 : 0;
							}
						}
						float progress = (float)jobsDone / (float)jobCount;
						if (progress > 1) {
							progress = 1;
						}
						fc.progressBar.setProgress(progress);
						// add the completed future/callback to the prune list
						// and execute it's callback on the next call to update().
						// This will allow the render thread to update the progress to 100%
						// before the callback is executed.
						if (progress == 1) {
							pruneList.add(fc);
						}
					}
				}
			}
			detach = false;
		}
		
		// detach if all all futures are complete and all callbacks have been executed.
		if (autoAttaching && detach) {
			sm.detach(this);
		}
	}

	/**
	 * Returns whether or not this will automatically detach/re-attach when there's 
	 * futures to monitor.
	 * 
	 * @return 
	 */
	public boolean isAutoAttaching() {
		return autoAttaching;
	}

	/**
	 * Set the auto detach/re-attach option. Default is true.
	 * @param autoAttaching
	 */
	public void setAutoAttaching(boolean autoAttaching) {
		this.autoAttaching = autoAttaching;
	}
}
