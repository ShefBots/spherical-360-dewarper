import blayzeTechUtils.env.*;
import blayzeTechUtils.math.*;
import blayzeTechUtils.env.nonpolyshapes.*;
import blayzeTechUtils.graphics.SimpleDisplay;
import java.awt.Color;
import java.awt.Graphics2D;

public class ReflectionRegressor {

  public static SimpleDisplay d = new SimpleDisplay(400,600, "Reflection Regressor", true, true);
  public static Graphics2D g = d.getGraphics2D();
  public static final double RAY_LEN = 1000;
  public static final int MAX_ITERATIONS = 100;
  public static final int STEPS = 10;
  public static final double STEP_DOWNSCALE = 0.4;
  public static final double SCALE_MULT = 10;

  public static final Double DEFAULT_CAMERA_DISTANCE = 30.0;
  public static final Double DEFAULT_REFLECTOR_RADIUS = 10.0;
  public static NVector offset = new NVector(new double[]{100,100});
  
  // Store the environment:
  public static Environment env = new Environment();
  public static CircleBoundedEntity camCircle;
  public static CircleBoundedEntity reflectorCircle;
  public static NVector DOWN = new NVector(new double[]{0,1});


  private NVector targetSlope;
  private Hit rayHit = new Hit(false, camCircle.getX(), camCircle.getY());
  private NVector reflectedRay = new NVector(new double[]{0,0});
  public double angle = 0;// Gets camera angle set by the regressor (stored here in case it gets needed somewhere else)
  public double outputRayAngleFromDown = 0; // Stores the angle from the down vector to the output ray

  static{
    // Man look at this strange static-class programming.
    // preconfig with arbitary positions:
    camCircle = new CircleBoundedEntity(offset.getElement(0), offset.getElement(1) + DEFAULT_CAMERA_DISTANCE, 4);
    reflectorCircle = new CircleBoundedEntity(offset.getElement(0), offset.getElement(1), DEFAULT_REFLECTOR_RADIUS * SCALE_MULT);
    setCameraDistance(DEFAULT_CAMERA_DISTANCE);
    env.entities.add(reflectorCircle);
    env.entities.add(camCircle);
  }
  /* Set the camera distance from the reflector (in millimeters) */
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
    d.drawGrid((int)(10 * SCALE_MULT), new Color(180,180,180));
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
    drawLines(Color.RED, true);
  }
  public void drawLines(Color c, boolean drawCamRay)
  {
    NVector mirrorDisp = reflectedRay.scale(100);
    if(drawCamRay)
    {
      g.setColor(Color.ORANGE);
      g.drawLine((int)camCircle.getX(), (int)camCircle.getY(), (int)rayHit.getX(), (int)rayHit.getY());
    }
    g.setColor(c);
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
      Point endPoint = new Point(ray.scale(RAY_LEN).add(cameraV));
      rayHit = env.hitScan(camCircle, endPoint, (AbstractEntity)camCircle);

      NVector normal = rayHit.toNVector().subtract(offset);
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
    
    this.angle = angle;
    this.outputRayAngleFromDown = Math.acos(reflectedRay.dot(DOWN));
    return angle;
  }
}
