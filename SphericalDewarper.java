import blayzeTechUtils.env.*;
import blayzeTechUtils.math.*;
import blayzeTechUtils.env.nonpolyshapes.*;
import blayzeTechUtils.graphics.SimpleDisplay;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.*;
import java.awt.event.*;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;


public class SphericalDewarper {

  public static Integer horizontalResolution = 550;
  public static Integer verticalResolution = 400;
  public static Double vertAngleStart = 0.0;
  public static Double vertAngleEnd = 60.0;

  public static SampleImageConfigurator sampleImageDisplay = new SampleImageConfigurator();
  public static SampleOutputDisplay sampleOutput = new SampleOutputDisplay(horizontalResolution, verticalResolution);

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
        sampleOutput.setNewImage(horizontalResolution, verticalResolution);
        computeAndRenderVisualiser();
      }
    });
    vertResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        verticalResolution = Math.max(0, Integer.parseInt(vertResolutionSelector.getText()));
        sampleOutput.setNewImage(horizontalResolution, verticalResolution);
        computeAndRenderVisualiser();
      }
    });
    resPanel.add(hozResolutionSelector);
    resPanel.add(vertResolutionSelector);
    configPanel.add(resPanel);

    // Add the file loader UI:
    final JFileChooser fileChooser = new JFileChooser();
    //fileChooser.addChoosableFileFilter(new ImageFilter());// TODO: Could filter the file selector to only be images.
    JButton loadFileBtn = new JButton("Load Sample Image");
    loadFileBtn.setSize(new Dimension(resPanel.getSize().width, loadFileBtn.getSize().height));
    loadFileBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int fileChooserReturn = fileChooser.showOpenDialog(configPanel);
        if(fileChooserReturn == JFileChooser.APPROVE_OPTION)
        {
          File imgFile = fileChooser.getSelectedFile();
          System.out.println("Opening file " + imgFile + "...");
          BufferedImage img = null;
          try {
                img = ImageIO.read(imgFile);
                sampleImageDisplay.setImage(img);
                computeAndRenderVisualiser();
          } catch (IOException ex) {
            System.out.println("Warning: Unable to read and load file. Please try another.\n\t" + ex);
          }
        }
      }
    });
    configPanel.add(loadFileBtn);


    configFrame.add(configPanel);
    configFrame.pack();
    configFrame.setLocationRelativeTo(null);
    configFrame.setVisible(true);


    // Render the initial run of the visualiser
    computeAndRenderVisualiser();
  }

  public static void computeAndRenderVisualiser()
  {
    ReflectionRegressor.drawEnvironment();
    if(ReflectionRegressor.getCameraDistance() > ReflectionRegressor.reflectorCircle.getRadius())
    {
      // Calculate the angles
      double[] angle = new double[verticalResolution];// Stores the angle from horizontal at each vertical step
      double scale = (vertAngleEnd - vertAngleStart)/(verticalResolution+1.0);// Plus one to get each point to be in the center
      for(int i = 0; i<verticalResolution; i++)
      {
        ReflectionRegressor rr = new ReflectionRegressor(vertAngleStart + i*scale);
        angle[i] = rr.regressAngle();
        rr.drawLines();
      }
      
      ////Use the calculated angles to sample the input image and draw to the output image
      // Calculate lookup table
      int[][][] lookupTable = new int[horizontalResolution][verticalResolution][2];// Store a map of coordinates to new coordinates
      for(int x = 0; x<horizontalResolution; x++)
      {
        for(int y = 0; y<verticalResolution; y++)
        {
          Point coordinate = sampleImageDisplay.getCoordinateAt(x/(double)horizontalResolution, angle[y]/angle[0]);
          lookupTable[x][y][0] = (int)coordinate.getX();
          lookupTable[x][y][1] = (int)coordinate.getY();
        }
      }

      // Use lookup table to draw dewarped image
      // TODO: Probably shouldn't reference the images like this, instead should make getters and setters (and put the subclasses and ImageContainer instances back to being private)
      if (sampleImageDisplay.imageContainer.image != null)
        for(int x = 0; x<horizontalResolution; x++)
        {
          for(int y = 0; y<verticalResolution; y++)
          {
            //System.out.println("(" + x + ", " + y + ")  -  (" + lookupTable[x][y][0] + ", " + lookupTable[x][y][1] + ")");
            sampleOutput.imageContainer.image.setRGB(x, y, sampleImageDisplay.imageContainer.image.getRGB(lookupTable[x][y][0], lookupTable[x][y][1]));
          }
        }
      sampleOutput.imageContainer.repaint();
    }

  }
}
