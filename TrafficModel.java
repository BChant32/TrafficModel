import java.io.File;

/**
 * Going to be the place where the model is formulated, built and run
 * @author Benjamin Chant
 */
public class TrafficModel {
	// Global parameters of the model which are all fixed during runtime
	public static final double VEHICLE_LENGTH = 5.0; // meter
	public static final long TIME_STEP = 10; // millis // Should probably divide RXN_TIME
	public static final long RUN_TIME = 100000; // millis // Change this to 0 for no same model with no reaction factor
	private static final long RXN_TIME = 2000; // millis
	private static final double MAX_DECEL = -2.0; // meter/(second^2)
	private static final double MAX_ACCEL = 2.0; // meter/(second^2)
	private static final double MAX_JERK = 1.0; // meter/(second^3)
	private static final double LAMBDA = 0.1; // Constant of proportionality used by some models
	private static final Case CURRENT_CASE = Case.QUICK_ACCELERATION; // Change this for different lead car motions
	private static enum Case {
		CONSTANT_VELOCITY,
		QUICK_ACCELERATION,
		SMOOTH_ACCELERATION,
		OSCILLATORY
	}
	private static final Model CHOSEN_MODEL = Model.LINEAR_CAR_FOLLOWING; // Change this for different modelling approaches
	private static enum Model{
		SAFE_DISTANCE,			// Uses parameter MEX_DECEL
		PROPORTIONAL_ACCELERATION,		// Uses parameter LAMBDA (and MAX_DECEL for safe distance)
		LINEAR_CAR_FOLLOWING,	// Uses parameter LAMBDA
		KEEPING_UP,				// Uses parameters MAX_DECEL and MAX_ACCEL
		LINEAR_PREDICION,		// Uses parameters MAX_DECEL and MAX_ACCEL
		FIRST_ORDER				// Uses parameters MAX_DECEL, MAX_ACCEL and MAX_JERK
	}
	
	/**
	 * Would be the main() method if I didn't wish to use javefx to create an animation outside this class
	 * @return		the array contains the 3 CarMotions after the model has ran
	 */
	public static CarMotion[] run() {
		CarMotion leadCar = generateLeadCar(); // Predetermined motion which doesn't use the model
		CarMotion firstCar = generateFollowingCar(leadCar, 20.0); // First use of the model to create another car's motion, dependent on "leadCar"
		CarMotion secondCar = generateFollowingCar(firstCar, 0.0); // Then uses the first generated car's motion to create a second car motion

		// Each car's motion is saved out to separate plain text files
		File leadCarFile = new File("lead.txt");
		File firstCarFile = new File("first.txt");
		File secondCarFile = new File("second.txt");
		leadCar.writeCarMotionToFile(leadCarFile);
		firstCar.writeCarMotionToFile(firstCarFile);
		secondCar.writeCarMotionToFile(secondCarFile);
		
		// Easier to return the CarMotion objects than to read out from text file when making the animation
		return new CarMotion[] {leadCar, firstCar, secondCar};
	}
	
	/**
	 * Used to create the motion of the "leader car" which is necessary as the model requires a car "up the road" to take data from
	 * Motion of the returned "vehicle" is not dependent on any others
	 * i.e. this car just does it's own path
	 * Will contain different cases of interest in order to test the model under in different scenarios
	 * @return		the motion of the lead vehicle, dependant on CURRENT_CASE
	 */
	private static CarMotion generateLeadCar() {
		// Generates initial conditions
		CarMotion leadCar = new CarMotion();
		long time = 0;
		double position = 50.0;
		double velocity = 10.0;
		double acceleration = 0.0;
		double jerk = 0.0;
		leadCar.appendInstance(new CarInstance(position, velocity, acceleration, jerk), time);
		
		double timeStep = (double)TIME_STEP / 1000.0; // Convert to seconds for ease
		while (time < RUN_TIME) {

			// Use the CURRENT_CASE to change which motion is used for the lead vehicle
			switch (CURRENT_CASE) {
			case CONSTANT_VELOCITY:
				acceleration = 0.0;
				jerk = 0.0;
				break;
			case QUICK_ACCELERATION:
				if (time < 5000) {
					acceleration = 0;
				} else if (time < 8000) {
					acceleration = 2.3;
				} else if (time < 10000) {
					acceleration = -0.5;
				} else {
					acceleration = 0.0;
				}
				jerk = 0.0;
				break;
			case SMOOTH_ACCELERATION:
				if (time < 1000 || time > 5000) {
					jerk = 0.1;
				} else if (time < 3000) {
					jerk = acceleration * ((double)1 - acceleration);
				} else {
					jerk = - acceleration * ((double)1 - acceleration);
				}
				acceleration += jerk * timeStep;
				break;
			case OSCILLATORY:
				double amplitude = 3.0;
				double period = 10000.0; // millis
				acceleration = amplitude * Math.cos(2.0 * Math.PI * time / period);
				break;
			default:
				break;
			}
			
			// Update variables according to new acceleration and save. nb. order matters
			velocity += acceleration * timeStep;
			position += velocity * timeStep;
			time += TIME_STEP;
			leadCar.appendInstance(new CarInstance(position, velocity, acceleration, jerk), time);
		}

		return leadCar;
	}
	
	/*
	 * This is the method containing the traffic model that we are interested in
	 */
	private static CarMotion generateFollowingCar(CarMotion leadCar, double offset) {
		// Generates initial conditions
		CarMotion followingCar = new CarMotion();	
		long time = 0;
		double position = offset;
		double velocity = 10.0;
		double acceleration = 0.0;
		double jerk = 0.0;
		followingCar.appendInstance(new CarInstance(position, velocity, acceleration, jerk), time);
		
		double timeStep = (double)TIME_STEP / 1000.0; // Convert to seconds for ease
		while (time < RUN_TIME) {
			// For positive reaction times cannot say anything about that first time interval so have everything remain the same
			if (time < RXN_TIME) {
				acceleration += jerk * timeStep;
				velocity += acceleration * timeStep;
				position += velocity * timeStep;
				time += TIME_STEP;
				followingCar.appendInstance(new CarInstance(position, velocity, acceleration, jerk), time);
				continue;
			}
			
			// All variables describing the lead car (RXN_TIME) ago
			CarInstance oldLeadCarInstance = leadCar.getInstance(time - RXN_TIME);
			double oldLeadPosition = oldLeadCarInstance.getPosition();
			double oldLeadVelocity = oldLeadCarInstance.getVelocity();
			double oldLeadAcceleration = oldLeadCarInstance.getAcceleration();
			double oldLeadJerk = oldLeadCarInstance.getJerk();
			
			// All variables describing the following car (RXN_TIME) ago
			CarInstance oldFollowingCarInstance = followingCar.getInstance(time - RXN_TIME);
			double oldPosition = oldFollowingCarInstance.getPosition();
			double oldVelocity = oldFollowingCarInstance.getVelocity();
			double oldAcceleration = oldFollowingCarInstance.getAcceleration();
			// No old jerk needed :)
			
			// Most models use the safe distance variable so it's extracted out here
			double safeDistance = safeDistance(oldLeadVelocity);
			double safePosition = oldLeadPosition - safeDistance;
			
			// Use the CHOSEN_MODEL to change which model is being used for the run
			switch (CHOSEN_MODEL) {
			/*
			 * Safe Distance model fixes the position and speed of each following vehicle according to the speed of the lead
			 * such that in theory there would be time to break to a stop if the lead vehicle immediately came to rest
			 */
			case SAFE_DISTANCE:
				// No acceleration or jerk terms as they are not relavent to discussion yet
				double currentLeadPosition = leadCar.getInstance(time).getPosition();
				double currentLeadVelocity = leadCar.getInstance(time).getVelocity();
				velocity = currentLeadVelocity;
				position = currentLeadPosition - safeDistance(velocity) - velocity * timeStep; // Position later updated
				break;
			
			/*
			 * Uses the proportionality constant to describe the acceleration of the
			 * following vehicle as proportional to the safe distance behind the lead vehicle
			 */
			case PROPORTIONAL_ACCELERATION:
				// Not going to bother defining jerk as it's not involved yet
				acceleration = LAMBDA * (safePosition - oldPosition);
				if (acceleration > MAX_ACCEL) {
					acceleration = MAX_ACCEL;
				} else if (acceleration < MAX_DECEL) {
					acceleration = MAX_DECEL;
				}
				
				break;
			
			/*
			 * Uses the proportionality constant to describe the acceleration of the
			 * following vehicle as proportional to the relative velocity to the lead vehicle
			 */
			case LINEAR_CAR_FOLLOWING:
				// Not going to bother defining jerk as it's not involved yet
				acceleration = LAMBDA * (oldLeadVelocity - oldVelocity);
				break;
				
			/*
			 * Decides which side of the safe distance measurement the vehicle is on and
			 * accelerates in the appropriate direction immeadiately
			 */
			case KEEPING_UP:
				// Not going to bother defining jerk as it would be poorly defined
				if (safePosition > oldPosition) {
					acceleration = MAX_ACCEL;
				} else if (safePosition < oldPosition) {
					acceleration = MAX_DECEL;
				} else {
					acceleration = 0;
				}
				
				break;
			
			/*
			 * Similar to KEEPING_UP but uses a prediction for the distance required to match speed
			 * in the aim of not overshooting
			 */
			case LINEAR_PREDICION:
				// Not going to bother defining jerk as it would be poorly defined
				if (oldVelocity > oldLeadVelocity) {
					// Require that following vehicle is going faster so that inputting the acceleration as MAX_DECEL makes sense
					double brakingDist = linearAccelerationDist(oldVelocity, oldLeadVelocity, MAX_DECEL);
					if (oldPosition >= safePosition - brakingDist) {
						acceleration = MAX_DECEL;
					}
				} else if (oldVelocity < oldLeadVelocity) {
					if (oldPosition < safePosition) {
						acceleration = MAX_ACCEL;
					}
				} else {
					acceleration = 0;
				}
				
				break;
			
			/**
			 * Similar to LINEAR_PREDICTION but uses a constant jerk equation for the predictions
			 */
			case FIRST_ORDER:
				// Assume constant jerk in lead vehicle from time (RXN_TIME) ago,
				// then what velocity will they have at current time?
				double rxnTime = (double)RXN_TIME / 1000.0; // Convert to seconds for ease
				double predictedLeadVelocity = oldLeadJerk * rxnTime*rxnTime / 2.0 + oldLeadAcceleration * rxnTime + oldLeadVelocity;
				double expectedSafeDist = safeDistance(predictedLeadVelocity);
				
				if (expectedSafeDist >= oldLeadPosition - oldPosition){
					if (acceleration == MAX_DECEL) {
						jerk = 0;
					} else if (acceleration + jerk * timeStep < MAX_DECEL){
						jerk = (MAX_DECEL - acceleration) / timeStep;
						acceleration = MAX_DECEL;
					} else {
						jerk = - MAX_JERK;
						acceleration += jerk * timeStep;
					}
				} else if (oldLeadVelocity < oldVelocity) {
					double expectedBrakingDist = firstOrderDist(oldVelocity, oldLeadVelocity, oldAcceleration, -MAX_JERK);
					if (expectedSafeDist + expectedBrakingDist >= oldLeadPosition - oldPosition) {
						if (acceleration == MAX_DECEL) {
							jerk = 0;
						} else if (acceleration + jerk * timeStep < MAX_DECEL){
							jerk = (MAX_DECEL - acceleration) / timeStep;
							acceleration = MAX_DECEL;
						} else {
							jerk = - MAX_JERK;
							acceleration += jerk * timeStep;
						} 
					} 
				} else {
					if (acceleration == MAX_ACCEL) {
						jerk = 0;
					} else if (acceleration + jerk * timeStep > MAX_ACCEL){
						jerk = (MAX_ACCEL - acceleration) / timeStep;
						acceleration = MAX_ACCEL; 
					} else {
						jerk = MAX_JERK;
						acceleration += jerk * timeStep;
					}
				}
				break;
				
			default:
				break;
			}

			velocity += acceleration * timeStep;
			position += velocity * timeStep;
			time += TIME_STEP;
			followingCar.appendInstance(new CarInstance(position, velocity, acceleration, jerk), time);
		}

		return followingCar;
	}
	
	/**
	 * Works out the safe distance to follow behind another vehicle
	 * Assumes both vehicles will be travelling at the same velocity
	 * @param	v	the assumed equalibrium velocity of both vehicles, should be meters/second
	 * @return		safe distance for the second vehicle to follow at, given in meters
	 */
	private static double safeDistance(double v) {
		return (double)VEHICLE_LENGTH + v * (double)RXN_TIME / 1000.0 - v*v /(2.0 * MAX_DECEL);
	}
	
	/**
	 * Works out distance reqiured to change speed assuming a constant acceleration or linear change in velocity
	 * uses the SUVAT/Kinematic equation V^2 - U^2 = 2 * A * S
	 * with V-endSpeed, U-startSpeed, A-acceleration, S-distance
	 * @param	startSpeed	initial velocity of the vehicle, should be meters/second
	 * @param	endSpeed	final, target velocity of the vehicle, should be meters/second
	 * @param	acceleration	the constant acceleration, should be meters/(second^2)
	 * @return		distance covered when using a constant acceleration to change speed, given in meters
	 */
	private static double linearAccelerationDist(double startSpeed, double endSpeed, double acceleration) {
		return (endSpeed*endSpeed - startSpeed*startSpeed) / ((double)2 *acceleration);
	}
	
	/**
	 * Works out distance required to change speed assuming constant jerk
	 * @param	u	start speed, should be meters/second
	 * @param	v	end speed, should be meters/second
	 * @param	a	start acceleration, should be meters/(second^2)
	 * @return		distance covered when using constant jerk to change speed to desired speed, given in meters
	 */
	private static double firstOrderDist(double u, double v, double a, double jerk) {
		double t = (-a - Math.sqrt(a*a - (double)2 * jerk * (u - v)) ) / jerk;
		return jerk * Math.pow(t, 3) / (double)6 + a * t*t / (double)2 + u * t;
	}
}
