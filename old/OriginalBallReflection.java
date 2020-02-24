import classes.env.*;
import classes.math.*;
import classes.env.nonpolyshapes.*;
import classes.graphics.SimpleDisplay;
import java.awt.Color;
import java.awt.Graphics2D;


public class BallReflection {
	public static double rayLen = 1000;
	public static void main(String[] args) throws InterruptedException
	{
		SimpleDisplay d = new SimpleDisplay(550,600,true, true);
		Graphics2D g = d.getGraphics2D();

		double radius = 60;
		double height = 300;
		NVector offsetV = new NVector(new double[]{200,100});
		NVector cameraV = offsetV.add(new NVector(new double[]{0,height}));

		Point offset = toPoint(offsetV);
		Point camera = toPoint(cameraV);
		
		Environment env = new Environment();
		env.entities.add(new CircleBoundedEntity(offset.getX(), offset.getY(), radius));
		CircleBoundedEntity camCircle = new CircleBoundedEntity(camera.getX(), camera.getY(), 4);
		env.entities.add(camCircle);
		
		g.setColor(Color.BLACK);
		env.draw(g);
		d.repaint();
		g.setColor(Color.RED);

		// SCREW THE MATHS PART. WE'RE GOING 'PROXIMA ON THIS.
		double targetAngle = 45;//-70; // Degrees from the top
		double rads = targetAngle/180 * Math.PI;
		NVector	targetSlope = new NVector(new double[]{Math.cos(rads), Math.sin(rads)});
		NVector targetSlopeDisp = targetSlope.scale(75);
		int STEPS = 10;
		int MAX_ITERATIONS = 100;
		double angularStep = Math.asin(radius/height)/STEPS;
		double stepDownscale = 0.5;

		int iterations = 0;
		double angle = 0.0;
		int lastSign = 1;
		do
		{
			NVector ray = new NVector(new double[]{Math.sin(angle), -Math.cos(angle)});
			Point endPoint = toPoint(ray.scale(rayLen).add(cameraV));
			Hit out = env.hitScan(camera, endPoint, (AbstractEntity)camCircle);
			//System.out.println(out);

			NVector normal = toNVector(out).subtract(offsetV);
			NVector invertedRay = ray.scale(-1);//.normalize();
			NVector mirror = normal.scale(2 * ((invertedRay.dot(normal))/(normal.dot(normal)))).subtract(invertedRay);
			NVector mirrorDisp = mirror.scale(100);

			// The drawing
			d.fill(Color.WHITE);
			g.setColor(Color.BLACK);
			env.draw(g);
			g.setColor(Color.RED);
			g.drawLine((int)camera.getX(), (int)camera.getY(), (int)out.getX(), (int)out.getY());
			g.setColor(Color.BLUE);
			g.drawLine((int)out.getX(), (int)out.getY(), (int)(out.getX()+normal.getElement(0)), (int)(out.getY()+normal.getElement(1)));
			g.setColor(Color.GREEN);
			g.drawLine((int)out.getX(), (int)out.getY(), (int)(out.getX()+mirrorDisp.getElement(0)), (int)(out.getY()+mirrorDisp.getElement(1)));
			g.setColor(Color.ORANGE);
			g.drawLine((int)out.getX(), (int)out.getY(), (int)(out.getX()+targetSlopeDisp.getElement(0)), (int)(out.getY()+targetSlopeDisp.getElement(1)));
			d.repaint();
			Thread.sleep(100);

			// Update the rule based on the error
			if(!out.madeContact())
			{
				angle -= angularStep; // Just step back if the end was reached, clearly the target is closer to the right
				angularStep *= stepDownscale;
				continue;
			}
			double yError = mirror.getElement(1) - targetSlope.getElement(1);
			System.out.println("y error: " + yError);
			int errorSign = (int)Math.signum(yError); // Really, we only care about the vertical error.
			if(errorSign != lastSign)
			{
				angularStep *= -stepDownscale; // Flip direction and scale down the step
			}
			lastSign = errorSign;
			angle += angularStep;
		}while(iterations < MAX_ITERATIONS);
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
