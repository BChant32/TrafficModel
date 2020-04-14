import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.*;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Creates a window to display the resulting motion of the cars in the model as an animation
 * @author Benjamin Chant
 */
public class Video extends Application{
	private static final String title = "Traffic Model";

	private static CarMotion leadCarMotion;
	private static CarMotion firstCarMotion;
	private static CarMotion secondCarMotion;

	public static void main(String args[]) {
		// Use TrafficModel to perform all the computations and get the result
		CarMotion[] results = TrafficModel.run();
		
		// Save a local CarMotion copy so that don't need to read in from text file later
		leadCarMotion = results[0];
		firstCarMotion = results[1];
		secondCarMotion = results[2];
		
		// Hands over to existing library methods to create the application we continue in start() method
		Video.launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Set up some window parameters
		primaryStage.setTitle(title);
		primaryStage.initStyle(StageStyle.UNIFIED);
		Group root = new Group();
        Scene scene = new Scene(root, 1000, 400);
        primaryStage.setScene(scene);
        
        // Creates the "cars"
        Rectangle leadCar = new Rectangle(TrafficModel.VEHICLE_LENGTH, 5, Color.BLUE);
        Rectangle firstCar = new Rectangle(TrafficModel.VEHICLE_LENGTH, 5, Color.RED);
        Rectangle secondCar = new Rectangle(TrafficModel.VEHICLE_LENGTH, 5, Color.GREEN);
        leadCar.setY(200);
        firstCar.setY(200);
        secondCar.setY(200);
        root.getChildren().addAll(leadCar, firstCar, secondCar);
        
        // Calculates the furthest the lead car travels (since position is an increasing sequence)
        // 	to rescale the distances involved to remain onsceen
        double totalDist = leadCarMotion.getInstance(TrafficModel.RUN_TIME).getPosition();
        double scaleFactor = scene.getWidth() / totalDist;
        
        // States when and where each vehicle will be
        Timeline timeline = new Timeline();
        for (long time = 0; time < TrafficModel.RUN_TIME; time += TrafficModel.TIME_STEP) {
        	double leadPosition = leadCarMotion.getInstance(time).getPosition() * scaleFactor; 
        	double firstPosition = firstCarMotion.getInstance(time).getPosition() * scaleFactor;
        	double secondPosition = secondCarMotion.getInstance(time).getPosition() * scaleFactor; 	
        	KeyFrame keyFrame = new KeyFrame(Duration.millis((double)time / (double)10), new KeyValue(leadCar.translateXProperty(), leadPosition),
    				new KeyValue(firstCar.translateXProperty(), firstPosition),
    				new KeyValue(secondCar.translateXProperty(), secondPosition));
            timeline.getKeyFrames().add(keyFrame);
		}
        
        // Sets the animation to loop and starts showing it
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();        
        primaryStage.show();
	}
}
