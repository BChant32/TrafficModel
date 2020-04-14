import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to describe a vehicle's motion by containing two fields
 * one an ArrayList of CarInstances and the other is the ascociated times.
 * Contains method to write data out to text file.
 * @author Benjamin Chant
 */
public class CarMotion {
	private ArrayList<CarInstance> instances = new ArrayList<CarInstance>();
	private ArrayList<Long> times = new ArrayList<Long>();
	
	/**
	 * Will be used depending on the model to get previous values from a different time code
	 * @param time	should be in millis
	 * @return		the CarInstance for the specific time given
	 */
	public CarInstance getInstance(long time) {
		int index = times.indexOf(Long.valueOf(time));
		return instances.get(index);
	}
	
	/**
	 * Will be used in the methods describing the model to create the motion of both lead and following vehicles
	 * @param instance
	 * @param time
	 */
	public void appendInstance(CarInstance instance, long time) {
		if (times == null) {
			times = new ArrayList<Long>();
			instances = new ArrayList<CarInstance>();
		}
		
		times.add(Long.valueOf(time));
		instances.add(instance);
		return;
	}
	
	/**
	 * Takes each variable for each time and writes out to the given text file in the format
	 * time1 pos1 vel1 acc1 jerk1
	 * time2 pos2 vel2 acc2 jerk2
	 * ...
	 * @param file
	 */
	public void writeCarMotionToFile(File file) {
		try {
        	BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        	
        	for (long time : times) {
				CarInstance carInstance = getInstance(time);
        		writer.write(String.valueOf(time) + " ");
        		writer.write(String.valueOf(carInstance.getPosition() + " "));
        		writer.write(String.valueOf(carInstance.getVelocity() + " "));
        		writer.write(String.valueOf(carInstance.getAcceleration() + " "));
        		writer.write(String.valueOf(carInstance.getJerk()));
        		writer.newLine();
			}
        	
            writer.close();
        }
        catch(IOException ex) {
            System.out.println("Error writing to file");
        }
	}
}
