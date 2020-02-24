import classes.env.*;
import classes.math.*;
import classes.env.nonpolyshapes.*;
import classes.graphics.SimpleDisplay;
import java.awt.Color;
import java.awt.Graphics2D;


public class BallReflection {

	public static class ReflectionRegressor {
		public static SimpleDisplay d = new SimpleDisplay(400,600, "Reflection Regressor", true, true);
		public static Graphics2D g = d.getGraphics2D();
		public static final double RAY_LEN = 1000;
		public static final int MAX_ITERATIONS = 100;
		public static final int STEPS = 10;
		public static final double STEP_DOWNSCALE = 0.4;

		public static double ballRadius = 100;
		public static double cameraDistance = 300;
		public static NVector offset = new NVector(new double[]{100,100});
		
		// Store the environment:
		public static Environment env = new Environment();
		public static CircleBoundedEntity camCircle = new CircleBoundedEntity(offset.getElement(0), offset.getElement(1) + cameraDistance, 4);
		public static CircleBoundedEntity reflectorCircle;


		private NVector targetSlope;
		private Hit rayHit = new Hit(false, camCircle.getX(), camCircle.getY());
		private NVector reflectedRay = new NVector(new double[]{0,0});

		private double angularStep = Math.asin(ballRadius/cameraDistance)/STEPS;

		static{
			// Man look at this strange static-class programming.
			env.entities.add(new CircleBoundedEntity(offset.getElement(0), offset.getElement(1), ballRadius));
			env.entities.add(camCircle);
		}
		public static void drawEnvironment()
		{
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
				double yError = reflectedRay.getElement(1) - targetSlope.getElement(1);
				//System.out.println("y error: " + yError);
				int errorSign = (int)Math.signum(yError); // Really, we only care about the vertical error.
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

	public static void main(String[] args) throws InterruptedException
	{
		// ReflectionRegressor.cameraDistance is not needed! (Plus probably ball radius, but you can create a static function that changes that object - maybe make the object private?)
		ReflectionRegressor.camCircle.setY(ReflectionRegressor.camCircle.getY()+300);
		ReflectionRegressor.drawEnvironment();

		for(double i = -70; i<90; i+= 1)
		{
			ReflectionRegressor rr = new ReflectionRegressor(i);
			rr.regressAngle();
			rr.drawLines();
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
}
