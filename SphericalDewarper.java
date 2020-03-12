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
import java.io.FileNotFoundException;
import java.io.PrintWriter;


public class SphericalDewarper {

  public static Integer horizontalResolution = 550;
  public static Integer verticalResolution = 400;
  public static Double vertAngleStart = 0.0;
  public static Double vertAngleEnd = 60.0;

  public static SampleImageConfigurator sampleImageDisplay = new SampleImageConfigurator();
  public static SampleOutputDisplay sampleOutput = new SampleOutputDisplay(horizontalResolution, verticalResolution);

  public static double[] perPixelAngles; // Stores the angle from horizontal at each vertical step
  public static int[][][] perPixelLookupTable; // Stores a map of each coordinate to a new coordinate (in the form [width][height][x,y])
  //public static int[][][][] perPixelSpreadLookupTable; // Stores a collection of coordinates for each pixel in the output from that in the input (in the form [width][height][xs][ys]) TODO.

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
      @Override
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

    // Add the save UI:
    configPanel.add(new JLabel("Save format and file name:"));
    JComboBox saveFormatComboBox = new JComboBox(new String[]{"Python", "Java"});
    JTextField saveFilename = new JTextField("output");
    JButton saveButton = new JButton("Save");
    JLabel saveState = new JLabel("");
    JPanel savePanel = new JPanel(new GridLayout(1,2));
    saveButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Find the line ending
        String fileEnding = saveFormatComboBox.getSelectedItem().toString();
        if(fileEnding.equals("Python"))
          fileEnding = "py";
        else if(fileEnding.equals("Java"))
          fileEnding = "java";

        // Actually write the file
        String filename = saveFilename.getText() + "." + fileEnding;
        System.out.print("Saving to " + filename + "... ");
        saveState.setText("Saving...");
        saveState.repaint();
        configPanel.repaint();
        try(PrintWriter file = new PrintWriter(filename))
        {
          //file.println(generateSaveCode(fileEnding));
          generateAndSaveCode(file, fileEnding);
          System.out.println("Saved!");
          saveState.setText("Saved!");
        }catch(FileNotFoundException ex){
          System.out.println("ERROR: File not found for saving - " + ex);
          saveState.setText("Error!");
        }catch(Exception ex){
          System.out.println("ERROR: Unexpected write error - " + ex);
          saveState.setText("Error!");
        }
      }
    });
    configPanel.add(saveFormatComboBox);
    configPanel.add(saveFilename);
    savePanel.add(saveButton);
    savePanel.add(saveState);
    configPanel.add(savePanel);

    configFrame.add(configPanel);
    configFrame.pack();
    configFrame.setLocationRelativeTo(null);
    configFrame.setVisible(true);

    // Render the initial run of the visualiser
    computeAndRenderVisualiser();
  }

  /**
   * Calculates sample bounds for the `perPixelSpreadLookupTable` variable.
   * Note: Incomplete. TODO.
   */
  public static void computeSampleBounds()
  {
    // Calculate the angles
    double[] pixelBoundaryAngles = new double[verticalResolution+1];// Stores the angles of each vertical pixel boundary
    double scale = (vertAngleEnd - vertAngleStart)/(verticalResolution);
    for(int i = 0; i<verticalResolution+1; i++)
    {
      ReflectionRegressor rr = new ReflectionRegressor(vertAngleStart + i*scale);
      pixelBoundaryAngles[i] = rr.regressAngle();
    }

    // Calculate pixel spread lookup table
  }
  public static void computeAndRenderVisualiser()
  {
    ReflectionRegressor.drawEnvironment();
    if(ReflectionRegressor.getCameraDistance() > ReflectionRegressor.reflectorCircle.getRadius())
    {
      // Calculate the angles
      double angles[] = new double[verticalResolution];// Stores the angle from vertical around the camera for each step
      perPixelAngles = new double[verticalResolution];// Stores the angle from the vertical around the reflector for use in export.
      double scale = (vertAngleEnd - vertAngleStart)/(verticalResolution);
      for(int i = 0; i<verticalResolution; i++)
      {
        ReflectionRegressor rr = new ReflectionRegressor(vertAngleStart + scale/2 + i*scale);
        angles[i] = rr.regressAngle();
        perPixelAngles[i] = rr.outputRayAngleFromDown;
        rr.drawLines();
      }

      // Also draw the start and end angles
      ReflectionRegressor rr = new ReflectionRegressor(vertAngleStart);
      rr.regressAngle();
      rr.drawLines(Color.BLACK, false);
      rr = new ReflectionRegressor(vertAngleEnd);
      rr.regressAngle();
      rr.drawLines(Color.BLACK, false);
      
      ////Use the calculated angles to sample the input image and draw to the output image
      // Calculate lookup table
      perPixelLookupTable = new int[horizontalResolution][verticalResolution][2];// Store a map of coordinates to new coordinates
      for(int x = 0; x<horizontalResolution; x++)
      {
        for(int y = 0; y<verticalResolution; y++)
        {
          Point coordinate = sampleImageDisplay.getCoordinateAt(x/(double)horizontalResolution, perPixelAngles[y]/perPixelAngles[0]);
          perPixelLookupTable[x][y][0] = (int)coordinate.getX();
          perPixelLookupTable[x][y][1] = (int)coordinate.getY();
        }
      }

      // Use lookup table to draw dewarped image
      // TODO: Probably shouldn't reference the images like this, instead should make getters and setters (and put the subclasses and ImageContainer instances back to being private)
      if (sampleImageDisplay.imageContainer.image != null)
        for(int x = 0; x<horizontalResolution; x++)
        {
          for(int y = 0; y<verticalResolution; y++)
          {
            //System.out.println("(" + x + ", " + y + ")  -  (" + perPixelLookupTable[x][y][0] + ", " + perPixelLookupTable[x][y][1] + ")");
            sampleOutput.imageContainer.image.setRGB(x, y, sampleImageDisplay.imageContainer.image.getRGB(perPixelLookupTable[x][y][0], perPixelLookupTable[x][y][1]));
          }
        }
      sampleOutput.imageContainer.repaint();
    }
  }

  /**
   * Generates the save code for the file.
   */
  public static void generateAndSaveCode(PrintWriter f, String filetype)
  {
    if(filetype.equals("py"))
      generateAndSavePythonCode(f);
    else if(filetype.equals("java"))
      generateAndSaveJavaCode(f);
    else
    {
      f.println("Error - somehow you've specified a nonexistant save type.");
      System.out.println("Error - somehow you've specified a nonexistant save type.");
    }
  }
  public static void generateAndSavePythonCode(PrintWriter f)
  {
    f.print("import numpy as np\ndewarpData = {}\ndewarpData['angles-degrees'] = np.asarray([");
    System.out.print("Saving per pixel angles (degrees)...");
    for(int i = 0; i<perPixelAngles.length; i++)
    {
      double angle = perPixelAngles[i]/Math.PI*180;
      f.print(angle + (i != perPixelAngles.length-1 ? "," : ""));
    }
    f.print("], dtype=np.float64)\ndewarpData['angles'] = np.asarray([");
    System.out.print("saved!\nSaving per pixel angles (radians)...");
    for(int i = 0; i<perPixelAngles.length; i++)
    {
      f.print( perPixelAngles[i] + (i != perPixelAngles.length-1 ? "," : ""));
    }
    f.print("], dtype=np.float64)\ndewarpData['lookup-table'] = np.asarray([");
    System.out.print("saved!\nSaving lookup table...");
    for(int i = 0; i<perPixelLookupTable.length; i++)
    {
      f.print("[");
      for(int o = 0; o<perPixelLookupTable[i].length; o++)
      {
        f.print("[" + perPixelLookupTable[i][o][0] + "," + perPixelLookupTable[i][o][1] + "]" + (o != perPixelLookupTable[i].length-1 ? "," : ""));
      }
      f.print("]" + (i != perPixelLookupTable.length-1 ? ",\n" : "\n"));
    }
    f.print("], dtype=np.int32)\n");
    System.out.println("saved!");
  }
  public static void generateAndSaveJavaCode(PrintWriter f)
  {
    f.println("This is not implemented yet.");
  }
}
