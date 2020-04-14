/**
 * Class to contain variables ascociated with an individual vehicle at a moment in time
 * probably a totally unnecessary class but feels more natural
 * @author Benjamin Chant
 */
public class CarInstance {
	private double position;
	private double velocity;
	private double acceleration;
	private double jerk;
	
	public CarInstance(double position, double velocity, double acceleration, double jerk) {
		this.position = position;
		this.velocity = velocity;
		this.acceleration = acceleration;
		this.jerk = jerk;
	}
	
	/**
	 * Getters and Setters
	 */
	public double getPosition() {
		return position;
	}
	public void setPosition(double position) {
		this.position = position;
	}
	
	public double getVelocity() {
		return velocity;
	}
	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}

	public double getAcceleration() {
		return acceleration;
	}
	public void setAcceleration(double acceleration) {
		this.acceleration = acceleration;
	}

	public double getJerk() {
		return jerk;
	}
	public void setJerk(double jerk) {
		this.jerk = jerk;
	}
}
