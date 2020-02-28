import blayzeTechUtils.env.*;
import blayzeTechUtils.math.*;
import blayzeTechUtils.env.nonpolyshapes.*;
import blayzeTechUtils.graphics.SimpleDisplay;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.*;
import java.awt.event.*;
import java.awt.GridLayout;


public class BallReflection {

  public static final double SCALE_MULT = 10;

	public static class ReflectionRegressor {
		public static SimpleDisplay d = new SimpleDisplay(400,600, "Reflection Regressor", true, true);
		public static Graphics2D g = d.getGraphics2D();
		public static final double RAY_LEN = 1000;
		public static final int MAX_ITERATIONS = 100;
		public static final int STEPS = 10;
		public static final double STEP_DOWNSCALE = 0.4;

		public static final Double DEFAULT_CAMERA_DISTANCE = 30.0;
		public static final Double DEFAULT_REFLECTOR_RADIUS = 10.0;
		public static NVector offset = new NVector(new double[]{100,100});
		
		// Store the environment:
		public static Environment env = new Environment();
		public static CircleBoundedEntity camCircle;
		public static CircleBoundedEntity reflectorCircle;


		private NVector targetSlope;
		private Hit rayHit = new Hit(false, camCircle.getX(), camCircle.getY());
		private NVector reflectedRay = new NVector(new double[]{0,0});

		static{
			// Man look at this strange static-class programming.
			// preconfig with arbitary positions:
			camCircle = new CircleBoundedEntity(offset.getElement(0), offset.getElement(1) + DEFAULT_CAMERA_DISTANCE, 4);
			reflectorCircle = new CircleBoundedEntity(offset.getElement(0), offset.getElement(1), DEFAULT_REFLECTOR_RADIUS * SCALE_MULT);
			setCameraDistance(DEFAULT_CAMERA_DISTANCE);
			env.entities.add(reflectorCircle);
			env.entities.add(camCircle);
		}
		public static void setCameraDistance(double distance)
		{
      distance *= SCALE_MULT;
			camCircle.setY(reflectorCircle.getY() + Math.max(0,distance));
		}
		public static double getCameraDistance()
		{
			return(Math.abs(camCircle.getY() - reflectorCircle.getY()));
		}
		public static void drawEnvironment()
		{
			d.fill(new Color(210,210,210));// TODO: Put colours somewhere else
			d.drawGrid((int)fromMm(10), new Color(180,180,180));
			g.setColor(Color.BLACK);
			env.draw(g);
			d.repaint();
		}

		public ReflectionRegressor(double targetAngle)
		{
			setTargetAngle(targetAngle);
		}
		public void drawLines()
		{
			NVector mirrorDisp = reflectedRay.scale(100);
			g.setColor(Color.ORANGE);
			g.drawLine((int)camCircle.getX(), (int)camCircle.getY(), (int)rayHit.getX(), (int)rayHit.getY());
			g.setColor(Color.RED);
			g.drawLine((int)rayHit.getX(), (int)rayHit.getY(), (int)(rayHit.getX()+mirrorDisp.getElement(0)), (int)(rayHit.getY()+mirrorDisp.getElement(1)));
			d.repaint();
		}

		public void setTargetAngle(double angleDegs)
		{
			double rads = angleDegs/180 * Math.PI;
			targetSlope = new NVector(new double[]{Math.cos(rads), Math.sin(rads)});
		}
		public double regressAngle()
		{
			NVector cameraV = new NVector(new double[]{camCircle.getX(), camCircle.getY()});
			double maximumRange = Math.asin(reflectorCircle.getRadius()/(camCircle.getY()-reflectorCircle.getY()));
			double angularStep = maximumRange/STEPS;

			int iterations = 0;
			double angle = 0.0;
			int lastSign = 1;
			do
			{
				NVector ray = new NVector(new double[]{Math.sin(angle), -Math.cos(angle)});
				Point endPoint = toPoint(ray.scale(RAY_LEN).add(cameraV));
				rayHit = env.hitScan(camCircle, endPoint, (AbstractEntity)camCircle);

				NVector normal = toNVector(rayHit).subtract(offset);
				NVector invertedRay = ray.scale(-1);
				reflectedRay = normal.scale(2 * ((invertedRay.dot(normal))/(normal.dot(normal)))).subtract(invertedRay);
				//NVector mirrorDisp = mirror.scale(100);

				// Update the rule based on the error
				if(!rayHit.madeContact())
				{
					angle -= angularStep; // Just step back if the end was reached, clearly the target is closer to the right
					angularStep *= STEP_DOWNSCALE;
					continue;
				}
				// TODO: Actually use the correct heuristic.
				double yError = reflectedRay.getElement(1) - targetSlope.getElement(1);
				double xError = reflectedRay.getElement(0) - targetSlope.getElement(0);
				//double error = targetSlope.crossProduct(reflectedRay);//xError + yError;
				//double error = reflectedRay.getElement(1)*targetSlope.getElement(0) - reflectedRay.getElement(0)*reflectedRay.getElement(1);
				double error = yError;
				//System.out.println("y error: " + yError);
				//System.out.println("error: " + error);
				int errorSign = (int)Math.signum(error); // Really, we only care about the vertical error.
				if(errorSign != lastSign)
				{
					angularStep *= -STEP_DOWNSCALE; // Flip direction and scale down the step
				}
				lastSign = errorSign;
				angle += angularStep;
				iterations++;
			}while(iterations < MAX_ITERATIONS);
			
			return angle;
		}
	}

  public static Integer horizontalResolution = 550;
  public static Integer verticalResolution = 400;
  public static Double vertAngleStart = 0.0;
  public static Double vertAngleEnd = 60.0;

	public static void main(String[] args) throws InterruptedException
	{
		// ReflectionRegressor.cameraDistance is not needed! (Plus probably ball radius, but you can create a static function that changes that object - maybe make the object private?)
		ReflectionRegressor.setCameraDistance(30);

		// Shove all the UI stuff in here:
		JFrame configFrame = new JFrame();
		JPanel configPanel = new JPanel();
		configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

		// Add the camera distance UI:
		configPanel.add(new JLabel("Camera Distance (mm):"));
		JTextField cameraDistanceSelector = new JTextField(ReflectionRegressor.DEFAULT_CAMERA_DISTANCE.toString());
		cameraDistanceSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ReflectionRegressor.setCameraDistance(Double.parseDouble(cameraDistanceSelector.getText()));
				computeAndRenderVisualiser();
			}
		});
		configPanel.add(cameraDistanceSelector);
		
		// Add the reflector radius UI:
		configPanel.add(new JLabel("Reflector Radius (mm):"));
		JTextField reflectorRadiusSelector = new JTextField(ReflectionRegressor.DEFAULT_REFLECTOR_RADIUS.toString());
		reflectorRadiusSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ReflectionRegressor.reflectorCircle.setRadius(SCALE_MULT * Math.max(0,Double.parseDouble(reflectorRadiusSelector.getText())));
				computeAndRenderVisualiser();
			}
		});
		configPanel.add(reflectorRadiusSelector);

    // Add the angle UI:
    configPanel.add(new JLabel("View angle (start, end):"));
    JPanel anglePanel = new JPanel(new GridLayout(1,2));
    JTextField startAngleSelector = new JTextField(vertAngleStart.toString());
    JTextField endAngleSelector = new JTextField(vertAngleEnd.toString());
    startAngleSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        vertAngleStart = Double.parseDouble(startAngleSelector.getText());
        computeAndRenderVisualiser();
      }
    });
    endAngleSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        vertAngleEnd = Math.min(88.48, Math.max(0, Double.parseDouble(endAngleSelector.getText())));
        computeAndRenderVisualiser();
      }
    });
    anglePanel.add(startAngleSelector);
    anglePanel.add(endAngleSelector);
    configPanel.add(anglePanel);

    // Add the resolution UI:
    configPanel.add(new JLabel("Output image resultion (pixels, WxH):"));
    JPanel resPanel = new JPanel(new GridLayout(1,2));
    JTextField hozResolutionSelector = new JTextField(horizontalResolution.toString());
    JTextField vertResolutionSelector = new JTextField(verticalResolution.toString());
    hozResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        horizontalResolution = Math.max(0, Integer.parseInt(hozResolutionSelector.getText()));
        computeAndRenderVisualiser();
      }
    });
    vertResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        verticalResolution = Math.max(0, Integer.parseInt(vertResolutionSelector.getText()));
        computeAndRenderVisualiser();
      }
    });
    resPanel.add(hozResolutionSelector);
    resPanel.add(vertResolutionSelector);
    configPanel.add(resPanel);

		configFrame.add(configPanel);
		configFrame.pack();
		configFrame.setLocationRelativeTo(null);
		configFrame.setVisible(true);

		computeAndRenderVisualiser();
	}
	public static void computeAndRenderVisualiser()
	{
		ReflectionRegressor.drawEnvironment();
		if(ReflectionRegressor.getCameraDistance() > ReflectionRegressor.reflectorCircle.getRadius())
    {
      double scale = (vertAngleEnd - vertAngleStart)/(verticalResolution+1.0);// Plus one to get each point to be in the center
			for(double i = 0; i<verticalResolution; i++)
			{
				ReflectionRegressor rr = new ReflectionRegressor(vertAngleStart + i*scale);
				rr.regressAngle();
				rr.drawLines();
			}
    }
	}
	public static Point toPoint(NVector v)
	{
		return new Point(v.getElement(0), v.getElement(1));
	}
	public static NVector toNVector(StaticPoint p)
	{
		return new NVector(new double[]{p.getX(), p.getY()});
	}
	public static double fromMm(double Mm)
	{
		return (Mm*SCALE_MULT);// TODO: Remove magic numbers
	}
	public static double toMm(double d)
	{
		return (d/SCALE_MULT);
	}
}
