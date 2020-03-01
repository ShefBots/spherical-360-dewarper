import blayzeTechUtils.env.*;
import blayzeTechUtils.math.*;
import blayzeTechUtils.env.nonpolyshapes.*;
import blayzeTechUtils.graphics.SimpleDisplay;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.*;
import java.awt.event.*;
import java.awt.GridLayout;


public class SphericalDewarper {

  public static Integer horizontalResolution = 550;
  public static Integer verticalResolution = 400;
  public static Double vertAngleStart = 0.0;
  public static Double vertAngleEnd = 60.0;

	public static void main(String[] args) throws InterruptedException
	{
		// ReflectionRegressor.cameraDistance is not needed! (Plus probably ball radius, but you can create a static function that changes that object - maybe make the object private?)
		ReflectionRegressor.setCameraDistance(30);

		//// CONFIG UI
		JFrame configFrame = new JFrame();
		configFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    configFrame.setTitle("Config");
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
				ReflectionRegressor.reflectorCircle.setRadius(ReflectionRegressor.SCALE_MULT * Math.max(0,Double.parseDouble(reflectorRadiusSelector.getText())));
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

    //// LOADED IMAGE UI
    

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
}
