package net.cofront.solarsystem;

import java.util.Calendar;

import javax.vecmath.Vector3d;

import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.util.TangentBinormalGenerator;

public class Planet extends Node {
	// the textures wrap around the z-axis
	public final static Quaternion Z_ADJUSTMENT = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
	
	// scale the rotation so that one earth day occurs every 30s
	public final static float DAY = 120f; //86400f;
	
	public final static float FIVE_PI = FastMath.PI * 5;
	
	private OrbitalElements oe;
	private String name;
	private Geometry g;
	private BoundingSphere bs;
	private Node indicator;
	private float r_scale;
	private Geometry clouds;
	private float time;
	
	public Planet(String name, final OrbitalElements oe, float r_scale) {
		this.name = name;
		this.oe = oe;
		this.r_scale = r_scale;
		this.time = 0;
		Sphere s = new Sphere(64, 64, r_scale * oe.radius);
		TangentBinormalGenerator.generate(s);
		s.setTextureMode(TextureMode.Projected);
		// this is used to slow down the camera as it approaches.
		bs = new BoundingSphere(oe.radius * r_scale * 20, getWorldTranslation());
		g = new Geometry(name, s) {
			private Quaternion r = new Quaternion();
			@Override
			public void updateLogicalState(float tpf) {
				super.updateLogicalState(tpf);
				// scale the rotation
				float rAngle = FastMath.TWO_PI * oe.rev * tpf / DAY;
				//System.out.println(name + " " + rAngle);
				r.fromAngleNormalAxis(rAngle, Vector3f.UNIT_Z);
				rotate(r);
				if (clouds != null) {
					// rotate with the planet
					r.fromAngleNormalAxis(rAngle, Vector3f.UNIT_Z);
					clouds.rotate(r);
					
					// next, have the clouds wobble around the poles.
					// this will create the illusion that they are moving, 
					// without having to rotate them faster than the planet is moving.
					
					// slow down the oscillation
					float tpfAdj = tpf/15f;
					
					time += (tpfAdj);
					if (Float.isNaN(time)) {
						time = 0;
					}
					
					// have them move in a circular pattern around the pole
					float sinTime = FastMath.sin(time);
					float cosTime = FastMath.cos(time);
					
					// limit the distance the clouds pole can move from the real pole
					float xAngle = (tpfAdj) * (cosTime) / FIVE_PI;
					float yAngle = (tpfAdj) * (sinTime) / FIVE_PI;
										
					r.fromAngleNormalAxis(xAngle, Vector3f.UNIT_X);
					clouds.rotate(r);
					
					r.fromAngleNormalAxis(yAngle, Vector3f.UNIT_Y);
					clouds.rotate(r);
					
				}
			}
		};
		g.rotate(Z_ADJUSTMENT);
		g.rotate(new Quaternion().fromAngleAxis(oe.tilt, Vector3f.UNIT_Y));
		g.setShadowMode(ShadowMode.CastAndReceive);
		//attachChild(g);
	}
	
	@Override
	public Spatial scale(float scale) {
		float r = bs.getRadius();
		bs = new BoundingSphere(r * scale, getWorldTranslation());
		return super.scale(scale);
	}
	
	public Geometry getGeometry() {
		return g;
	}
	
	public void setIndicator(Node indicator) {
		this.indicator = indicator;
	}
	
	public Node getIndicator() {
		return indicator;
	}

	public String getName() {
		return name;
	}
	
	public BoundingSphere getBoundingSphere() {
		return bs;
	}
	
	public OrbitalElements getOrbitalElements() {
		return oe;
	}
	
	public void adjustLocation(Calendar c, float d_scale, Vector3d tmp) {
		if (tmp == null) {
			tmp = new Vector3d();
		}
		oe.getHeliocentricPosition(c, tmp, true);
		tmp.scale(d_scale);
		Vector3f pos = getLocalTranslation();
		v3f(tmp, pos);
		if (pos.equals(Vector3f.NAN)) {
			pos.set(0, 0, 0);
		}
		setLocalTranslation(pos);
	}
	
	public void addRings(Material m, float ringMaxRadius) {
		float size = r_scale * ringMaxRadius;
		// Box q = new Box(size, size, size);
		Quad q = new Quad(size, size);
		Geometry g = new Geometry(name + "-Rings", q);
		g.setMaterial(m);
		g.rotate(Z_ADJUSTMENT);
		g.rotate(new Quaternion().fromAngleAxis(oe.tilt, Vector3f.UNIT_Y));
		g.setShadowMode(ShadowMode.CastAndReceive);
		g.setQueueBucket(Bucket.Transparent);
		attachChild(g.center());
	}
	
	public void addClouds(Material m) {
		Sphere s = new Sphere(64, 64, r_scale * oe.radius * 1.01f);
		Geometry g = new Geometry(name + "-Clouds", s);
		g.setMaterial(m);
		g.rotate(Z_ADJUSTMENT);
		// offset the cloud oscillation
		g.rotate(new Quaternion().fromAngleNormalAxis(-1 / FIVE_PI, Vector3f.UNIT_Y));
		// tilt
		g.rotate(new Quaternion().fromAngleAxis(oe.tilt, Vector3f.UNIT_Y));
		g.setShadowMode(ShadowMode.CastAndReceive);
		g.setQueueBucket(Bucket.Transparent);
		clouds = g;
		//attachChild(g.center());
		enableClouds();
		
	}
	
	public void disableClouds() {
		clouds.removeFromParent();
	}
	
	public void enableClouds() {
		if (clouds != null) {
			attachChild(clouds.center());
		}
	}
	
	public void toggleClouds() {
		if (this.hasChild(clouds)) {
			disableClouds();
		}
		else {
			enableClouds();
		}
	}
	
	/*
	public void addMoon(String moonName, Material mMat, MoonApproximations ma, float d_scale) {
		Sphere s = new Sphere(32, 32, r_scale * ma.radius);
		Geometry g = new Geometry(name + "-Moon-" + moonName, s);
		TangentBinormalGenerator.generate(s);
		s.setTextureMode(TextureMode.Projected);
		g.setMaterial(mMat);
		g.rotate(Z_ADJUSTMENT);
		g.setShadowMode(ShadowMode.CastAndReceive);
		attachChild(g.center());
		//g.setLocalTranslation(new Vector3f(ma.a * d_scale * (oe.radius * r_scale), 0, 0));
		g.setLocalTranslation(new Vector3f(ma.a * d_scale * (oe.radius * r_scale), 0, 0));
		
		
	}
	*/
	
	public Geometry createOrbit(Calendar c, Vector3d v3dtmp2, float d_scale) {
		Geometry g = null;
		synchronized(v3dtmp2) {
			c = (Calendar)c.clone();
			double lastStep = oe.getOrbitalPeriod();
			
			// 1-step per earth day.
			int step = (int)oe.getOrbitalPeriod() / 365;
			if (step < 1) step = 1;
			
			Spline spline = new Spline();
			oe.getHeliocentricPosition(c, v3dtmp2, true);
			v3dtmp2.scale(d_scale);
			Vector3f nowPos = v3f(v3dtmp2, null);
			spline.addControlPoint(nowPos);	
			
			for (int i=step;i<=lastStep;i+=step) {
				c.add(Calendar.DAY_OF_YEAR, step);
				oe.getHeliocentricPosition(c, v3dtmp2, true);
				v3dtmp2.scale(d_scale);
				Vector3f pos = v3f(v3dtmp2, null);
				spline.addControlPoint(pos);
			}
			spline.addControlPoint(nowPos);	
			Curve curve = new Curve(spline, 1);
			g = new Geometry(null, curve);
		}
		return g;
	}
	
	
	private Vector3f v3f(Vector3d v3d, Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		store.x = (float)v3d.x;
		store.y = (float)v3d.y;
		store.z = (float)v3d.z;
		return store;
	}

	/* (non-Javadoc)
	 * @see com.jme3.scene.Node#updateLogicalState(float)
	 */
	@Override
	public void updateLogicalState(float tpf) {
		super.updateLogicalState(tpf);
		bs.setCenter(getWorldTranslation());
		if (indicator != null) {
			
		}
	}
}
