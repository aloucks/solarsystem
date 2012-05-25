package net.cofront.jme3;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.Trigger;

/**
 * Callback based ActionListener and AnalogListener.
 * 
 * @author Aaron Loucks
 * @version $Id: UserListener.java 131 2011-05-31 17:12:54Z aloucks $
 */
public class UserListener implements ActionListener, AnalogListener {

	private final static Logger log = Logger.getLogger(UserListener.class.getName());
	/**
	 * Callback class for {@link #onAction(String, boolean, float)} and {@link #onAnalog(String, float, float)}. 
	 */
	public static abstract class UserAction {		
		public abstract String getName();
		public abstract Trigger[] getTriggers();
		public void onAnalog(float value, float tpf) {

		}
		public void onAction(boolean isPressed, float tpf) {
			
		}
	}
	
	private boolean bind = false;
	private HashMap<String,UserAction> actions = new HashMap<String,UserAction>();
	
	/**
	 * Returns whether or not these actions have been submitted to the inputManager.
	 * @return
	 */
	public boolean isBound() {
		return bind;
	}
	
	/**
	 * Returns a shallow copy of the {@link UserAction}s.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String,UserAction> getUserActions() {
		return (HashMap<String,UserAction>)actions.clone();
	}
	
	/**
	 * Sets the UserActions.
	 * @param actions
	 */
	public void setUserActions(HashMap<String,UserAction> actions) {
		if (bind)
			log.log(Level.WARNING, "UserActions have already been bound to an InputManager.");
		this.actions = actions;
	}
	
	/**
	 * Registers a UserAction. The UserListener must be bound after all actions are registered.
	 * Registering an action after {@link #bind(InputManager)} will not add the mappings to an input manager.
	 * @param action
	 */
	public void register(UserAction action) {
		if (bind)
			log.log(Level.WARNING, "UserActions have already been bound to an InputManager.");
		actions.put(action.getName(), action);
	}
	
	/**
	 * Unregisters a UserAction.
	 * @param action
	 */
	public void unregister(UserAction action) {
		if (bind)
			log.log(Level.WARNING, "UserActions have already been bound to an InputManager.");
		actions.remove(action.getName());
	}
	
	/**
	 * Removes all UserActions. Note: This will not remove mappings from the input manager.
	 * Call {@link #unbind(InputManager)} <b>before</b> {@link #unregisterAll()}.
	 */
	public void unregisterAll() {
		actions.clear();
	}
	
	/**
	 * Add all mappings and start listening.
	 * @param inputManager
	 */
	public void bind(InputManager inputManager) {
		for(UserAction a : actions.values()) {
			inputManager.addMapping(a.getName(), a.getTriggers());
		}
		String[] names = actions.keySet().toArray(new String[actions.size()]);
		inputManager.addListener(this, names);
		bind = true;
	}
	
	/**
	 * Remove all mappings and stop listening.
	 * @param inputManager
	 */
	public void unbind(InputManager inputManager) {
		for(UserAction a : actions.values()) {
			inputManager.deleteMapping(a.getName());
		}
		inputManager.removeListener(this);
		bind = false;
	}
	
	@Override
	public void onAnalog(String name, float value, float tpf) {
		UserAction a = actions.get(name);
		if (a != null) {
			a.onAnalog(value, tpf);
		}
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		UserAction a = actions.get(name);
		if (a != null) {
			a.onAction(isPressed, tpf);
		}
	}
}
