package net.cofront.solarsystem;

import java.util.Calendar;

import javax.vecmath.Vector3d;

import com.jme3.math.Vector3f;


/**
 * 
 * @author Aaron Loucks
 * @version $Id: OrbitalElements.java 139 2011-06-01 02:18:10Z aloucks $
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Solar_System">http://en.wikipedia.org/wiki/Solar_System</a>
 * @see <a href="http://en.wikipedia.org/wiki/Kepler%27s_laws_of_planetary_motion">http://en.wikipedia.org/wiki/Kepler's_laws_of_planetary_motion</a>
 * @see <a href="http://en.wikipedia.org/wiki/Orbital_elements#Keplerian_elements">http://en.wikipedia.org/wiki/Orbital_elements#Keplerian_elements</a>
 * @see <a href="http://en.wikipedia.org/wiki/List_of_gravitationally_rounded_objects_of_the_Solar_System">http://en.wikipedia.org/wiki/List_of_gravitationally_rounded_objects_of_the_Solar_System</a>
 * @see <a href="http://www.pgccphy.net/ref/celmech.pdf">http://www.pgccphy.net/ref/celmech.pdf</a>
 * @see <a href="http://www.stjarnhimlen.se/comp/tutorial.html">http://www.stjarnhimlen.se/comp/tutorial.html<a/>
 * @see <a href="http://www.davidcolarusso.com/astro/">http://www.davidcolarusso.com/astro/</a>
 * @see <a href="http://en.wikibooks.org/wiki/Astrodynamics/Orbit_Basics">http://en.wikibooks.org/wiki/Astrodynamics/Orbit_Basics</a>
 * @see <a href="http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html">http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html</a>
 * @see <a href="http://hpiers.obspm.fr/eop-pc/models/constants.html">http://hpiers.obspm.fr/eop-pc/models/constants.html</a>
 * @see <a href="http://nssdc.gsfc.nasa.gov/planetary/factsheet/planetfact_notes.html".http://nssdc.gsfc.nasa.gov/planetary/factsheet/planetfact_notes.html</a>
 * @see <a href="http://en.wikipedia.org/wiki/Rotation_period">http://en.wikipedia.org/wiki/Rotation_period</a>
 */
public class OrbitalElements {
	/** 2 * PI = {@value} */
	public final static double TWO_PI = 2 * Math.PI;
	/** PI / 180 = {@value} */
	public final static double DEG_TO_RAD = Math.PI / 180;
	/** 180 / PI = {@value} */
	public final static double RAD_TO_DEG = 180 / Math.PI;
	/** Default <code>accuracy</code> ({@value}) when calculating <code>E</code>. */
	public final static float ACCURACY = 0.0000001f;
	/** Default <code>max_iterations</code> ({@value}) when calculating <code>E</code>. */
	public final static int MAX_ITERATIONS = 5;
	/** Obliquity of the ecliptic = {@value} */
	public final static double EARTH_AXIAL_TILT_RAD = 23.439281061084554 * DEG_TO_RAD;
	public final static double SIN_EARTH_AXIAL_TILT_RAD = Math.sin(EARTH_AXIAL_TILT_RAD);
	public final static double COS_EARTH_AXIAL_TILT_RAD = Math.cos(EARTH_AXIAL_TILT_RAD);
	
	/*
	protected final double semimajor_axis;
	protected final double eccentricity;
	protected final double orbital_period;
	protected final double mean_anomaly_at_epoch;
	protected final double inclination;
	protected final double longitude_of_the_ascending_node;
	protected final double argument_of_periapsis;
	*/
	
	protected float accuracy = ACCURACY;
	protected int max_iterations = MAX_ITERATIONS;
	
	public final double a;		// = semimajor_axis;
	public final double e;		// = eccentricity;
	public final double i;		// = inclination * DEG_TO_RAD;
	public final double L;		// = longitude_of_the_ascending_node * DEG_TO_RAD;
	public final double w;		// = argument_of_periapsis * DEG_TO_RAD;
	public final double M0;		// = mean_anomaly_at_epoch * DEG_TO_RAD;
	public final double P;		// = orbital_period;
	public final float radius;
	public final float tilt;
	public final float rev;
	
	/**
	 * Creates a new set of orbital elements with the given known values. 
	 * @param semimajor_axis Semi-major axis in kilometers.
	 * @param eccentricity Eccentricity of orbit. <code>0 < e < 1</code> for elliptical orbits.
	 * @param orbital_period Number of julian earth days required for the planet to make one full rotation (360 degrees) around the sun.
	 * @param mean_anomaly_at_epoch Degrees (at epoch J2000)
	 * @param inclination Degrees (to to ecliptic)
	 * @param longitude_of_the_ascending_node Degrees
	 * @param argument_of_periapsis Degrees
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/Solar_System">http://en.wikipedia.org/wiki/Solar_System</a>
	 * @see <a href="http://en.wikipedia.org/wiki/Longitude_of_the_ascending_node">http://en.wikipedia.org/wiki/Longitude_of_the_ascending_node</a>
	 * @see <a href="http://en.wikipedia.org/wiki/Argument_of_periapsis">http://en.wikipedia.org/wiki/Argument_of_periapsis</a>
	 * 
	 */
	public OrbitalElements(
		double semimajor_axis,
		double eccentricity,
		double orbital_period,
		double mean_anomaly_at_epoch,
		double inclination,
		double longitude_of_the_ascending_node,
		double argument_of_periapsis,
		float radius,
		float tilt,
		float rev
	) {
		/*
		this.semimajor_axis = semimajor_axis;
		this.eccentricity = eccentricity;
		this.orbital_period = orbital_period;
		this.mean_anomaly_at_epoch = mean_anomaly_at_epoch;
		this.inclination = inclination;
		this.longitude_of_the_ascending_node = longitude_of_the_ascending_node;
		this.argument_of_periapsis = argument_of_periapsis;
		*/
	
		a = semimajor_axis;
		e = eccentricity;
		i = inclination * DEG_TO_RAD;
		L = longitude_of_the_ascending_node * DEG_TO_RAD;
		w = argument_of_periapsis * DEG_TO_RAD;
		M0 = mean_anomaly_at_epoch * DEG_TO_RAD;
		P = orbital_period;
		this.radius = radius;
		this.tilt = tilt;
		this.rev = rev;
	}
	
	/**
	 * All values are set to zero except for the radius.
	 */
	public final static OrbitalElements Sun = new OrbitalElements(
		0,		// semi-major axis
		0,		// eccentricity
		0,		// orbital period
		0,		// mean anomaly at epoch
		0,			// inclination
		0,			// longitude of the ascending node
		0,			// argument of periapsis
		6.955E5f,
		0,
		0
	);
	
	/**
	 * Mercury's orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Mercury_(planet)">http://en.wikipedia.org/wiki/Mercury_(planet)</a>
	 */
	public final static OrbitalElements Mercury = new OrbitalElements(
		57909100,		// semi-major axis
		0.205630,		// eccentricity
		87.9691,		// orbital period
		174.796,		// mean anomaly at epoch
		//3.38, // sun's equator
		7.005,			// inclination
		48.331,			// longitude of the ascending node
		29.12,			// argument of periapsis
		2439.7f,
		2 * (float)DEG_TO_RAD, // ?
		1 / 8f //58.646f
	);
	
	/**
	 * Venus' orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Earth">http://en.wikipedia.org/wiki/Venus</a>
	 */
	public final static OrbitalElements Venus = new OrbitalElements(
		108208930,		// semi-major axis
		0.0068,			// eccentricity
		224.70069,		// orbital period
		50.44675,		// mean anomaly at epoch
		//3.86, // sun's equator
		3.39471,		// inclination
		76.67069,		// longitude of the ascending node
		54.85229,		// argument of periapsis
		6051.8f,
		177.3f * (float) DEG_TO_RAD,
		1 / 20f //1 / -243.0187f
	);
	
	/**
	 * Earth's orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Earth">http://en.wikipedia.org/wiki/Earth</a>
	 */
	public final static OrbitalElements Earth = new OrbitalElements(
		149598261,		// semi-major axis
		0.01671123,		// eccentricity
		365.256363004,	// orbital period
		357.51716,		// mean anomaly at epoch
		//7.155, //  sun's equator
		0,				// inclination
		348.73936,		// longitude of the ascending node
		114.20783,		// argument of periapsis
		6371f,
		23.4f * (float)DEG_TO_RAD,
		1 / 0.99726968f
	);
	
	/**
	 * Mars' orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Mars">http://en.wikipedia.org/wiki/Mars</a>
	 */
	public final static OrbitalElements Mars = new OrbitalElements(
		227939100,		// semi-major axis
		0.093315,		// eccentricity
		686.971,		// orbital period
		19.3564,		// mean anomaly at epoch
		//5.65, // sun's equator
		1.850,			// inclination
		49.562,			// longitude of the ascending node
		286.537,			// argument of periapsis
		3396.2f,
		25.19f * (float) DEG_TO_RAD,
		1 / 1.02595675f
	);
	
	/**
	 * Jupiter's orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Jupiter">http://en.wikipedia.org/wiki/Jupiter</a>
	 */
	public final static OrbitalElements Jupiter = new OrbitalElements(
		778547200,		// semi-major axis
		0.048775,		// eccentricity
		4331.572,		// orbital period
		18.818,			// mean anomaly at epoch
		//6.09,// sun's equator
		1.305,			// inclination
		100.492,		// longitude of the ascending node
		275.066,			// argument of periapsis
		69911f,
		3.13f * (float) DEG_TO_RAD,
		1 / 0.41007f
	);
	
	/**
	 * Saturn's orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Saturn">http://en.wikipedia.org/wiki/Saturn</a>
	 */
	public final static OrbitalElements Saturn = new OrbitalElements(
		1433449370,		// semi-major axis
		0.055723219,	// eccentricity
		10759.22,		// orbital period
		320.346750,		// mean anomaly at epoch
		//5.51,// sun's equator
		2.485240,		// inclination
		113.642811,		// longitude of the ascending node
		336.013862,		// argument of periapsis
		60268f,
		26.73f * (float) DEG_TO_RAD,
		1 / 0.426f
	);
	
	/**
	 * Uranus' orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Uranus">http://en.wikipedia.org/wiki/Uranus</a>
	 */
	public final static OrbitalElements Uranus = new OrbitalElements(
		2876679082.0,	// semi-major axis
		0.044405586,	// eccentricity
		30799.095,		// orbital period
		142.955717,		// mean anomaly at epoch
		//6.48,// sun's equator
		0.772556,		// inclination
		73.989821,		// longitude of the ascending node
		96.541318,		// argument of periapsis
		25559f,
		97.77f * (float) DEG_TO_RAD,
		1 / -0.71833f
	);
	
	/**
	 * Neptune's orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Neptune">http://en.wikipedia.org/wiki/Neptune</a>
	 */
	public final static OrbitalElements Neptune = new OrbitalElements(
		4503443661.0,	// semi-major axis
		0.011214269,	// eccentricity
		60190,			// orbital period
		267.767281,		// mean anomaly at epoch
		//6.43,// sun's equator
		1.767975,		// inclination
		131.794310,		// longitude of the ascending node
		265.646853,		// argument of periapsis
		24764f,
		28.32f * (float) DEG_TO_RAD,
		1 / 0.67125f
	);
	
	/**
	 * Pluto's orbital elements.
	 * @see <a href="http://en.wikipedia.org/wiki/Pluto">http://en.wikipedia.org/wiki/Pluto</a>
	 */
	public final static OrbitalElements Pluto = new OrbitalElements(
		7311000000.0,	// semi-major axis
		0.24880766,		// eccentricity
		90613.305,		// orbital period
		14.86012204,	// mean anomaly at epoch
		//11.88,// sun's equator
		17.14175,		// inclination
		110.30347,		// longitude of the ascending node
		113.76329,		// argument of periapsis
		1153f,
		119.591f * (float) DEG_TO_RAD,
		1 / -6.38718f
	);
	
	/**
	 * The <code>accuracy</code> for calculating <code>E</code>.
	 * @return the accuracy
	 */
	public float getAccuracy() {
		return accuracy;
	}
	/**
	 * Set the accuracy of <code>E()</code>
	 * @param accuracy the accuracy to set
	 * @see #E(double E0, double M, double e)
	 */
	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}
	/**
	 * The <code>max_iterations</code> when calculating <code>E</code>.
	 * @return the max_iterations
	 */
	public int getMaxIterations() {
		return max_iterations;
	}
	/**
	 * Set the max iterations of <code>E()</code>
	 * @param max_iterations the max_iterations to set
	 * @see #E(double E0, double M, double e)
	 */
	public void setMaxIterations(int max_iterations) {
		this.max_iterations = max_iterations;
	}
	
	/**
	 * Calls <code>E</code> with the <code>accuracy</code> and <code>max_iterations</code> set in this instance.
	 * @param E0 first approximation (start with M).
	 * @param M mean anomaly.
	 * @param e eccentricity of orbit.
	 * @return eccentric anomaly.
	 * 
	 */
	public double E(double E0, double M, double e) {
		return E(E0, M, e, accuracy, max_iterations);
	}
	/**
	 * Calculates the <i>Eccentric anomaly</i> given the current
	 * mean anomaly and the eccentricity of orbit. 
	 * 
	 * Kepler's equation:
	 * <code>M = E - e * sin(E)</code>
	 * 
	 * The function is called recursively until the desired accuracy is achieved (<code>|E1 - E0| < accuracy</code>) or <code>max_iterations</code> have occurred.
	 * 
	 * <code>E1 = E0 + (M + e * sin(E0) - E0)/(1 - e * cos(E0)</code>
	 * 
	 * @param E0 first approximation (start with M).
	 * @param M mean anomaly.
	 * @param e eccentricity of orbit.
	 * @param accuracy desired accuracy.
	 * @param max_iterations the maximum number of attempts to reach the desired accuracy.
	 * @return eccentric anomaly.
	 * 
	 * @see <a href="http://answers.yahoo.com/question/index?qid=20090204154844AAbnVCp">http://answers.yahoo.com/question/index?qid=20090204154844AAbnVCp<a/>
	 * @see <a href="http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html">http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html<a/>
	 */
	public static double E(double E0, double M, double e, float accuracy, int max_iterations) {
		double E1 = E0 + (M + e * Math.sin(E0) - E0)/(1 - e * Math.cos(E0));
		if ( (Math.abs(E1 - E0)) < accuracy || max_iterations < 1) {
			return E1;
		}
		else {
			return E(E1, M, e, accuracy, --max_iterations);
		}
	}
	
	/**
	 * Returns the number of Julian days since (or before) epoch J2000.
	 * @param c Gregorian calendar date. This should be UTC.
	 * @return number of days (including fractional days).
	 * @see <a href="http://en.wikipedia.org/wiki/Epoch_%28astronomy%29#Julian_years_and_J2000">http://en.wikipedia.org/wiki/Epoch_(astronomy)#Julian_years_and_J2000</a>
	 * @see <a href="http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html">http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html</a>
	 * @see <a href="http://www.stjarnhimlen.se/comp/tutorial.html">http://www.stjarnhimlen.se/comp/tutorial.html</a>
	 */
	public static float getDaysJ2000(Calendar c) {
		float jd;
		int y = c.get(Calendar.YEAR);
		int m = c.get(Calendar.MONTH) + 1;
		int D = c.get(Calendar.DAY_OF_MONTH);
		int hh = c.get(Calendar.HOUR_OF_DAY);
		int mm = c.get(Calendar.MINUTE);
		int ss = c.get(Calendar.SECOND);
		jd = 367*y - 7 * ( y + (m+9)/12 ) / 4 + 275*m/9 + D - 730530;
		jd = jd + (hh/24 + mm/3600 + ss/86400);
		return jd;
	}

	/**
	 * Calcualtes the x, y, and z heliocentric coordinates on the specified date.
	 * Assumes the sun is located at (0,0,0).
	 * Note: Axes are flipped. 
	 * 
	 * @param c the date.
	 * @return vector heliocentric position.
	 */
	public Vector3d getHeliocentricPosition(Calendar c) {
		Vector3d store = new Vector3d();
		getHeliocentricPosition(c, store, true);
		return store;
	}
	/**
	 * Calculates the x, y, and z heliocentric coordinates on the specified date.
	 * Assumes the sun is located at (0,0,0). See the links class links for an
	 * explanation of how this is calculated.
	 * 
	 * @param c the date.
	 * @param store the vector to store the x, y, and z coordinates.
	 * @param flipAxes x=y, y=z, z=x
	 */
	public void getHeliocentricPosition(Calendar c, Vector3d store, boolean flipAxes) {
		/*
		double a = semimajor_axis;
		double e = eccentricity;
 		double i = inclination * DEG_TO_RAD;
		double L = longitude_of_the_ascending_node * DEG_TO_RAD;
		double w = argument_of_periapsis * DEG_TO_RAD;
		double M0 = mean_anomaly_at_epoch * DEG_TO_RAD;
		double P = orbital_period;
		*/
		
		// fractional days
		double t = getDaysJ2000(c);
		
		// T0 = 0
		// M = M0 + ( TWO_PI * ( t - T0 ) / P )
		double M = M0 + ( TWO_PI * ( t ) / P );
		
		// http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html
		// recommends normalizing the angle if it's not between 0 - 360 degrees
		M = M % TWO_PI; 
		if (M < 0) {
			M += TWO_PI;
		}
		
		// essentric anomaly
		double E = E(M, M, e);
		
		// "The true anomaly f is the true polar coordinate of the body, 
		//  measured from the pericenter to the body, in the plane of the orbit."
		double f = 2 * Math.atan( Math.sqrt((1+e)/(1-e)) * Math.tan(E/2) );
		
		// http://mysite.verizon.net/res148h4j/zenosamples/zs_planetorbits.html
		// recommends normalizing the angle if it's not between 0 - 360 degrees
		if (f < 0) {
			f += TWO_PI;
		}
		
		// radial distance r of the orbiting body from the central body
		double r = a * ( 1 - e * Math.cos(E) );
		
		// "The quantities r and f are the plane polar coordinates of the 
		//  orbiting body, with the central body at the origin. The remainder 
		//  of the calculations are essentially a set of coordinate 
		//  transformations to find the right ascension and declination of the body."
		
		// argument of latitude
		double u = w + f;	
		
		// heliocentric cartesian ecliptic coordinates (x,y,z) of the orbiting body
		double x = r * ( Math.cos(u) * Math.cos(L) - Math.sin(u) * Math.sin(L) * Math.cos(i) );
		double y = r * ( Math.cos(u) * Math.sin(L) + Math.sin(u) * Math.cos(L) * Math.cos(i) );
		double z = r * ( Math.sin(u) * Math.sin(i) );
		
	
		/*
		// this would be for using the sun's equitorial plane
		// instead of the ecliptic 
		double X = x;
		double Y = y * Math.cos(i) - z * Math.sin(i);
		double Z = y * Math.sin(i) + z * Math.cos(i);

		if (flipAxes) {
			store.x = Y;
			store.y = Z;
			store.z = X;
		}
		else {
			store.x = X;
			store.y = Y;
			store.z = Z;
		}
		*/
		
		/**/
		if (flipAxes) {
			store.x = y;//Y;
			store.y = z;//Z;
			store.z = x;//X;
		}
		else {
			store.x = x;//X;
			store.y = y;//Y;
			store.z = z;//Z;
		}
		/**/
		
		
	}
	/*
	 * This doesn't seem to work yet.
	 */
	public Vector3f getGeoentricPosition(Calendar c, int r, boolean flipAxes) {
		Vector3d pos = new Vector3d();
		this.getHeliocentricPosition(c, pos, flipAxes);
		Vector3d earthPos = new Vector3d();
		OrbitalElements.Earth.getHeliocentricPosition(c, earthPos, flipAxes);
		pos.add(earthPos);
		double e_lon = Math.atan( pos.y / pos.x);
		double e_lat  = pos.z / Math.sqrt( Math.pow(pos.x, 2) +  Math.pow(pos.y, 2) + Math.pow(pos.z, 2));
		double ras = Math.atan( ( Math.sin(e_lon) * COS_EARTH_AXIAL_TILT_RAD - Math.tan(e_lat) * SIN_EARTH_AXIAL_TILT_RAD ) / Math.cos(e_lon) );
		double dec = Math.asin( Math.sin(e_lat) * COS_EARTH_AXIAL_TILT_RAD + Math.cos(e_lat) * SIN_EARTH_AXIAL_TILT_RAD * Math.sin(e_lon) );
		//System.out.println("ras="+ras);
		//System.out.println("dec="+dec);
		
		// dec = acos(z/r)
		// ras = atan(y/x)
		
		Vector3f geo = new Vector3f();
		geo.x = (float)( r * Math.sin(dec) * Math.cos(ras) );
		geo.y = (float)( r * Math.sin(dec) * Math.sin(ras) );
		geo.z = (float)( r * Math.cos(dec) );
		
		System.out.println(geo);
		
		return geo;
	}
	/**
	 * Semi-major axis in Kilometers.
	 * @return the semimajor_axis
	 * @see <a href="http://en.wikipedia.org/wiki/Semi-major_axis">http://en.wikipedia.org/wiki/Semi-major_axis</a>
	 */
	public double getSemimajorAxis() {
		return a; //semimajor_axis;
	}
	/**
	 * Eccentricity of orbit.
	 * @return the eccentricity
	 * @see <a href="http://en.wikipedia.org/wiki/Orbital_eccentricity">http://en.wikipedia.org/wiki/Orbital_eccentricity<a/>
	 */
	public double getEccentricity() {
		return e; //eccentricity;
	}
	/**
	 * Sidereal orbital period in Julian Earth days.
	 * @see <a href="http://en.wikipedia.org/wiki/Orbital_period">http://en.wikipedia.org/wiki/Orbital_period<a/>
	 * @return the orbital_period
	 */
	public double getOrbitalPeriod() {
		return P; //orbital_period;
	}
	/**
	 * Mean anomaly at epoch J2000 in degrees.
	 * @return the mean_anomaly_at_epoch
	 * @see <a href="http://en.wikipedia.org/wiki/Mean_anomaly">http://en.wikipedia.org/wiki/Mean_anomaly<a/>
	 */
	public double getMeanAnomalyAtEpoch() {
		return M0 * RAD_TO_DEG; //mean_anomaly_at_epoch;
	}
	/**
	 * Inclination to the ecliptic in degrees.
	 * @return the inclination
	 * @see <a href="http://en.wikipedia.org/wiki/Ecliptic">http://en.wikipedia.org/wiki/Ecliptic</a>
	 */
	public double getInclination() {
		return i * RAD_TO_DEG; //inclination;
	}
	/**
	 * Returns longitude of the ascending node.
	 * @return the longitude_of_the_ascending_node
	 * @see <a href="http://en.wikipedia.org/wiki/Longitude_of_the_ascending_node">http://en.wikipedia.org/wiki/Longitude_of_the_ascending_node<a/>
	 */
	public double getLongitudeOfTheAscending_node() {
		return L * RAD_TO_DEG; //longitude_of_the_ascending_node;
	}
	/**
	 * Returns argument of periapsis (perihelion).
	 * @return the argument_of_periapsis
	 * @see <a href="http://en.wikipedia.org/wiki/Argument_of_periapsis">http://en.wikipedia.org/wiki/Argument_of_periapsis</a>
	 */
	public double getArgumentOfPeriapsis() {
		return w * RAD_TO_DEG; //argument_of_periapsis;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public float getTilt() {
		return tilt;
	}
}
