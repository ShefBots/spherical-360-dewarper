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

  private static abstract class ImageLoader implements ActionListener {
    public static final JFileChooser fileChooser = new JFileChooser();

    public void actionPerformed(ActionEvent e) {
      int fileChooserReturn = fileChooser.showOpenDialog(configFrame);
      if(fileChooserReturn == JFileChooser.APPROVE_OPTION)
      {
        File imgFile = fileChooser.getSelectedFile();
        System.out.println("Opening file " + imgFile + "...");
        BufferedImage img = null;
        try {
              img = ImageIO.read(imgFile);
              doSomethingWithImage(img);// Do something class-specific after loading the image
              computeAndRenderVisualiser();
        } catch (IOException ex) {
          System.out.println("Warning: Unable to read and load file. Please try another.\n\t" + ex);
        }
      }
    }

    public abstract void doSomethingWithImage(BufferedImage img);
  }

  public static String VERSION = "1.3.0";

  public static Integer horizontalPanoramicResolution = 550;
  public static Integer verticalPanoramicResolution = 400;
  public static Integer horizontalTopDownResolution = 400;
  public static Integer verticalTopDownResolution = 400;
  public static Double topDownMmPerPixel = 1.0;
  public static Double vertAngleStart = 0.0;
  public static Double vertAngleEnd = 60.0;
  //public static Double heightFromGround = 150; // How high the ball bearing is above the ground

  public static SampleImageConfigurator sampleImageDisplay = new SampleImageConfigurator();
  public static SampleOutputDisplay samplePanoramicOutput = new SampleOutputDisplay(horizontalPanoramicResolution, verticalPanoramicResolution, "Panoramic");
  public static SampleOutputDisplay sampleTopDownOutput = new SampleOutputDisplay(horizontalTopDownResolution, verticalTopDownResolution, "Top-Down");
  public static JFrame configFrame = new JFrame();

  public static double[] perPixelAngles; // Stores the angle from horizontal at each vertical step
  public static int[][][] perPixelLookupTable; // Stores a map of each coordinate to a new coordinate (in the form [width][height][x,y])
  public static int[][][] topDownLookupTable; // Stores a map of each coordinate to a new coordinate, top-down mode.
  //public static int[][][][] perPixelSpreadLookupTable; // Stores a collection of coordinates for each pixel in the output from that in the input (in the form [width][height][xs][ys]) TODO.

  public static void main(String[] args) throws InterruptedException
  {
    // ReflectionRegressor.cameraDistance is not needed! (Plus probably ball radius, but you can create a static function that changes that object - maybe make the object private?)
    ReflectionRegressor.setCameraDistance(30);

    //// Config UI
    //JFrame configFrame = new JFrame();
    configFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    configFrame.setTitle("Dewarper Config");

    // Store config panels in a tabbed pane
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel primaryConfigPanel = createPrimaryConfigPanel(tabbedPane);
    JPanel masksConfigPanel = createMasksConfigPanel();

    tabbedPane.addTab("General", primaryConfigPanel);
    tabbedPane.addTab("Masks", masksConfigPanel);
    tabbedPane.setEnabledAt(1, false); // Disable the masks tab until an image is loaded

    // Finalize and pack everything
    configFrame.add(tabbedPane);
    configFrame.pack();
    configFrame.setLocationRelativeTo(null);
    configFrame.setVisible(true);

    // Render the initial run of the visualiser
    computeAndRenderVisualiser();
  }

  public static JPanel createMasksConfigPanel()
  {
    JPanel configPanel = new JPanel();
    configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

    //// Closest-pixel (nearest neighbour) mask panel
    JPanel NNmaskPanel = new JPanel();
    NNmaskPanel.setBorder(BorderFactory.createTitledBorder("Nearest Neighbour Mask"));
    NNmaskPanel.setLayout(new GridLayout(0,1));
    JCheckBox enableNNmaskBtn = new JCheckBox("Enable");
    enableNNmaskBtn.setEnabled(false);
    enableNNmaskBtn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e)
      {
        sampleImageDisplay.imageContainer.setNNmaskImage(enableNNmaskBtn.isSelected());
        computeAndRenderVisualiser();
      }
    });
    
    // Load Mask Option
    JButton loadNNmaskBtn = new JButton("Load Mask");
    loadNNmaskBtn.addActionListener(new ImageLoader(){
      @Override
      public void doSomethingWithImage(BufferedImage img)
      {
        sampleImageDisplay.imageContainer.setNNmaskImage(img);
        enableNNmaskBtn.setEnabled(true);
        enableNNmaskBtn.setSelected(true);
      }
    });
    NNmaskPanel.add(loadNNmaskBtn);
    NNmaskPanel.add(enableNNmaskBtn);
    configPanel.add(NNmaskPanel);

    //// Direct-pixel mask panel
    JPanel DPmaskPanel = new JPanel();
    DPmaskPanel.setBorder(BorderFactory.createTitledBorder("Static Mask"));
    DPmaskPanel.setLayout(new GridLayout(0,1));
    JCheckBox enableDPmaskBtn = new JCheckBox("Enable");
    enableDPmaskBtn.setEnabled(false);
    enableDPmaskBtn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e)
      {
        sampleImageDisplay.imageContainer.setDPmaskImage(enableDPmaskBtn.isSelected());
        computeAndRenderVisualiser();
      }
    });
    JPanel coordinatePanel = new JPanel(new GridLayout(1,2));
    JTextField xCoordinateSelector = new JTextField("0");
    xCoordinateSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sampleImageDisplay.imageContainer.directLinkReticule.setX(Double.parseDouble(xCoordinateSelector.getText()));
        sampleImageDisplay.imageContainer.repaint();
        computeAndRenderVisualiser();
      }
    });
    JTextField yCoordinateSelector = new JTextField("0");
    yCoordinateSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sampleImageDisplay.imageContainer.directLinkReticule.setY(Double.parseDouble(yCoordinateSelector.getText()));
        sampleImageDisplay.imageContainer.repaint();
        computeAndRenderVisualiser();
      }
    });
    xCoordinateSelector.setEnabled(false);
    yCoordinateSelector.setEnabled(false);
    JButton loadDPmaskBtn = new JButton("Load Mask");
    loadDPmaskBtn.addActionListener(new ImageLoader(){
      @Override
      public void doSomethingWithImage(BufferedImage img)
      {
        // TODO: Add the mask image, configure JText field to update the mask link on click
        sampleImageDisplay.imageContainer.setDPmaskImage(img);
        enableDPmaskBtn.setEnabled(true);
        enableDPmaskBtn.setSelected(true);
        xCoordinateSelector.setEnabled(true);
        yCoordinateSelector.setEnabled(true);
      }
    });
    DPmaskPanel.add(loadDPmaskBtn);
    DPmaskPanel.add(new JLabel("Mask pixel location (x,y):"));
    coordinatePanel.add(xCoordinateSelector);
    coordinatePanel.add(yCoordinateSelector);
    DPmaskPanel.add(coordinatePanel);
    DPmaskPanel.add(enableDPmaskBtn);
    configPanel.add(DPmaskPanel);

    return configPanel;
  }

  public static JPanel createPrimaryConfigPanel(JTabbedPane tabbedPane)
  {
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

    // Add the height from ground UI
    configPanel.add(new JLabel("Reflector center above ground (mm):"));
    JTextField heightFromGroundSelector = new JTextField(ReflectionRegressor.DEFAULT_HEIGHT_TO_GROUND.toString());
    heightFromGroundSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ReflectionRegressor.ballHeightAboveGround = Double.parseDouble(heightFromGroundSelector.getText());
        System.out.println("Reflector height above ground set to " + ReflectionRegressor.ballHeightAboveGround + "mm");
        computeAndRenderVisualiser();
      }
    });
    configPanel.add(heightFromGroundSelector);

    configPanel.add(new JLabel("Topdown mm/pixel:"));
    JTextField mmPerPixelSelector = new JTextField(topDownMmPerPixel.toString());
    mmPerPixelSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        topDownMmPerPixel = Double.parseDouble(mmPerPixelSelector.getText());
        computeAndRenderVisualiser();
      }
    });
    configPanel.add(mmPerPixelSelector);

    // Add the panoramic resolution UI:
    configPanel.add(new JLabel("Panoramic image resolution (pixels, WxH):"));
    JPanel resPanel = new JPanel(new GridLayout(1,2));
    JTextField hozResolutionSelector = new JTextField(horizontalPanoramicResolution.toString());
    JTextField vertResolutionSelector = new JTextField(verticalPanoramicResolution.toString());
    hozResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        horizontalPanoramicResolution = Math.max(0, Integer.parseInt(hozResolutionSelector.getText()));
        samplePanoramicOutput.setNewImage(horizontalPanoramicResolution, verticalPanoramicResolution);
        computeAndRenderVisualiser();
      }
    });
    vertResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        verticalPanoramicResolution = Math.max(0, Integer.parseInt(vertResolutionSelector.getText()));
        samplePanoramicOutput.setNewImage(horizontalPanoramicResolution, verticalPanoramicResolution);
        computeAndRenderVisualiser();
      }
    });
    resPanel.add(hozResolutionSelector);
    resPanel.add(vertResolutionSelector);
    configPanel.add(resPanel);

    // Add the top-down resolution UI
    configPanel.add(new JLabel("Topdown image resolution (pixels, WxH):"));
    JPanel tdResPanel = new JPanel(new GridLayout(1,2));
    JTextField tdHozResolutionSelector = new JTextField(horizontalTopDownResolution.toString());
    JTextField tdVertResolutionSelector = new JTextField(verticalTopDownResolution.toString());
    tdHozResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        horizontalTopDownResolution = Math.max(0, Integer.parseInt(tdHozResolutionSelector.getText()));
        sampleTopDownOutput.setNewImage(horizontalPanoramicResolution, verticalPanoramicResolution);
        computeAndRenderVisualiser();
      }
    });
    tdVertResolutionSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        verticalTopDownResolution = Math.max(0, Integer.parseInt(tdVertResolutionSelector.getText()));
        sampleTopDownOutput.setNewImage(horizontalPanoramicResolution, verticalPanoramicResolution);
        computeAndRenderVisualiser();
      }
    });
    tdResPanel.add(tdHozResolutionSelector);
    tdResPanel.add(tdVertResolutionSelector);
    configPanel.add(tdResPanel);

    // Add the file loader UI:
    JButton loadFileBtn = new JButton("Load Sample Image");
    loadFileBtn.setSize(new Dimension(resPanel.getSize().width, loadFileBtn.getSize().height));
    loadFileBtn.addActionListener(new ImageLoader(){
      @Override
      public void doSomethingWithImage(BufferedImage img)
      {
        sampleImageDisplay.imageContainer.setImage(img);
        tabbedPane.setEnabledAt(1, true); // Enable the Masks tab
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

    // Ending segment
    //configPanel.add(new JSeparator());
    //configPanel.add(new JPanel());

    // Return the config panel
    return configPanel;
  }

  /**
   * Calculates sample bounds for the `perPixelSpreadLookupTable` variable.
   * Note: Incomplete. TODO.
   */
  public static void computeSampleBounds()
  {
    // Calculate the angles
    double[] pixelBoundaryAngles = new double[verticalPanoramicResolution+1];// Stores the angles of each vertical pixel boundary
    double scale = (vertAngleEnd - vertAngleStart)/(verticalPanoramicResolution);
    for(int i = 0; i<verticalPanoramicResolution+1; i++)
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
      //double angles[] = new double[verticalPanoramicResolution];// Stores the angle from vertical around the camera for each step // TODO: I have removed this, because I don't think it's needed
      perPixelAngles = new double[verticalPanoramicResolution];// Stores the angle from the vertical around the reflector for use in export.
      double scale = (vertAngleEnd - vertAngleStart)/(verticalPanoramicResolution);
      for(int i = 0; i<verticalPanoramicResolution; i++)
      {
        ReflectionRegressor rr = new ReflectionRegressor(vertAngleStart + scale/2 + i*scale);
        /*angles[i] = */rr.regressAngle();
        perPixelAngles[i] = rr.outputRayAngleFromDown;
        rr.drawLines();
      }

      // Also draw the start and end angles
      ReflectionRegressor rrgui = new ReflectionRegressor(vertAngleStart);
      rrgui.regressAngle();
      rrgui.drawLines(Color.BLACK, false);
      rrgui = new ReflectionRegressor(vertAngleEnd);
      rrgui.regressAngle();
      rrgui.drawLines(Color.BLACK, false);
      
      ////Use the calculated angles to sample the input image and draw to the output image
      // Calculate lookup table
      perPixelLookupTable = new int[horizontalPanoramicResolution][verticalPanoramicResolution][2];// Store a map of coordinates to new coordinates
      for(int x = 0; x<horizontalPanoramicResolution; x++)
      {
        for(int y = 0; y<verticalPanoramicResolution; y++)
        {
          Point coordinate = sampleImageDisplay.getCoordinateAt(x/(double)horizontalPanoramicResolution, perPixelAngles[y]/perPixelAngles[0]);
          perPixelLookupTable[x][y][0] = (int)coordinate.getX();
          perPixelLookupTable[x][y][1] = (int)coordinate.getY();
        }
      }

      // Calculate the top-down lookup table
      topDownLookupTable = new int[horizontalTopDownResolution][verticalTopDownResolution][2];
      double centerX = (horizontalTopDownResolution*topDownMmPerPixel)/2;
      double centerY = (verticalTopDownResolution*topDownMmPerPixel)/2;
      for(int x = 0; x<horizontalTopDownResolution; x++)
      {
        for(int y = 0; y<verticalTopDownResolution; y++)
        {
          ReflectionRegressor rr = new ReflectionRegressor();
          double targetDistance = Math.sqrt(Math.pow(x+0.5-centerX, 2) + Math.pow(y+0.5-centerY, 2));
          rr.setTargetFloorDistance(targetDistance);
          rr.regressFloorAngle();
        }
      }

      // Alter the lookup table to account for the direct-pixel mask
      if(sampleImageDisplay.imageContainer.isDPmaskEnabled())
      {
        // Loop through the lookup table and update the links to the set coordinates
        for(int x = 0; x<horizontalPanoramicResolution; x++)
          for(int y = 0; y<verticalPanoramicResolution; y++)
            if((sampleImageDisplay.imageContainer.DPmaskImage.getRGB(perPixelLookupTable[x][y][0], perPixelLookupTable[x][y][1])&0xff) < 128)
            {
              perPixelLookupTable[x][y][0] = (int)sampleImageDisplay.imageContainer.directLinkReticule.getX();
              perPixelLookupTable[x][y][1] = (int)sampleImageDisplay.imageContainer.directLinkReticule.getY();
            }
      }

      // Alter the lookup table to account for the nearest-neighbour mask
      // TODO: (Also see below) NNmaskImage is public within ImageContainer, and referenced here - this should probably be changed.
      // TODO: I also hate the horrifically nested code below, but this seems like the most efficient way to do it.
      if(sampleImageDisplay.imageContainer.isNNmaskEnabled())
      {
        // Search to find the neareset non-mask pixel at each pixel
        // (The start pixel itself will, by default be the closest non-mask pixel):
        for(int x = 0; x<horizontalPanoramicResolution; x++)
        {
          for(int y = 0; y<verticalPanoramicResolution; y++)
          {
            // Expand the search radius until it completely encloses the
            // circle formed taking the radius from the current closest point
            // (Note that all distances are done without sqrts)
            int[] closestPixel = new int[]{0,0};// By default, the closest pixel is the central pixel
            int searchRadius = 0;
            Integer closestDistance = Integer.MAX_VALUE;
            int dx, dy;
            int[] maskSpacePixel = perPixelLookupTable[x][y];
            while(searchRadius*searchRadius < closestDistance)
            {
              for(int xs = maskSpacePixel[0]-searchRadius; xs<=maskSpacePixel[0]+searchRadius; xs++)
                for(int ys = maskSpacePixel[1]-searchRadius; ys<=maskSpacePixel[1]+searchRadius; ys++)
                {
                  // Skip the direct lookup parts, as we don't want any of that
                  if(sampleImageDisplay.imageContainer.isDPmaskEnabled() && (sampleImageDisplay.imageContainer.DPmaskImage.getRGB(xs,ys)&0xff) < 128)
                    continue;
                  // If we've found a non-mask part:
                  if((sampleImageDisplay.imageContainer.NNmaskImage.getRGB(xs,ys)&0xff) >= 128)
                  {
                    // Calculate it's distance:
                    dx = xs-maskSpacePixel[0];
                    dy = ys-maskSpacePixel[1];
                    int distance = dx*dx+dy*dy;
                    // If it's closer than the closest pixel, update that
                    // Also update the search radius so that it
                    // checks the whole circle possible for closer points
                    if(distance < closestDistance)
                    {
                      //System.out.println("Found a non-mask at ("+dx+","+dy+")");
                      closestPixel[0] = dx;
                      closestPixel[1] = dy;
                      closestDistance = distance;
                    }
                  }
                }
              searchRadius++;// Increase the search radius
            }
            perPixelLookupTable[x][y][0] = maskSpacePixel[0] + closestPixel[0];
            perPixelLookupTable[x][y][1] = maskSpacePixel[1] + closestPixel[1];
          }
        }
      }

      // Use lookup table to draw dewarped image
      // TODO: Probably shouldn't reference the images like this, instead should make getters and setters
      // (and put the subclasses and ImageContainer instances back to being private)
      if (sampleImageDisplay.imageContainer.image != null)
        for(int x = 0; x<horizontalPanoramicResolution; x++)
        {
          for(int y = 0; y<verticalPanoramicResolution; y++)
          {
            //System.out.println("(" + x + ", " + y + ")  -  (" + perPixelLookupTable[x][y][0] + ", " + perPixelLookupTable[x][y][1] + ")");
            samplePanoramicOutput.imageContainer.image.setRGB(x, y, sampleImageDisplay.imageContainer.image.getRGB(perPixelLookupTable[x][y][0], perPixelLookupTable[x][y][1]));
          }
        }
      samplePanoramicOutput.imageContainer.repaint();
    }
  }

  /**
   * Generates the save code for the file.
   */
  public static void generateAndSaveCode(PrintWriter f, String filetype)
  {
    // Assemble reference notes:
    String[] docstring = new String[]{
                         "AUTO-GENERATED FILE.",
                         "File Version: " + VERSION,
                         "Intended Input Image Width: " + sampleImageDisplay.imageContainer.image.getWidth(),
                         "Intended Input Image Height: " + sampleImageDisplay.imageContainer.image.getHeight(),
                         "Output Panoramic Image Width: " + horizontalPanoramicResolution,
                         "Output Panoramic Image Height: " + verticalPanoramicResolution};

    // Export the file:
    if(filetype.equals("py"))
      generateAndSavePythonCode(docstring, f);
    else if(filetype.equals("java"))
      generateAndSaveJavaCode(docstring, f);
    else
    {
      f.println("Error - somehow you've specified a nonexistant save type.");
      System.out.println("Error - somehow you've specified a nonexistant save type.");
    }
  }
  public static void generateAndSavePythonCode(String[] docstring, PrintWriter f)
  {
    for(String s : docstring)
      f.print("#" + s + "\n");
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
    System.out.println("saved!\nSaving lookup vector...");

    // For each pixel in the lookup table, find it's corresponding pixel in the input data as an index from 0 to (inputdata.width*inputdata.height):
    f.print("dewarpData['lookup-vector'] = np.asarray([");
    int[] vectorLookup = new int[perPixelLookupTable.length * perPixelLookupTable[0].length];
    for(int x = 0; x<perPixelLookupTable.length; x++)
    {
      for(int y = 0; y<perPixelLookupTable[x].length; y++)
      {
        int[] data = perPixelLookupTable[x][y];
        //vectorLookup[perPixelLookupTable.length*y + (perPixelLookupTable.length-1-x)] = data[1]*sampleImageDisplay.imageContainer.image.getWidth() + data[0];
        vectorLookup[perPixelLookupTable.length*y + x] = data[1]*sampleImageDisplay.imageContainer.image.getWidth() + data[0];
      }
    }
    for(int i = 0; i<vectorLookup.length; i++)
    {
      f.print(vectorLookup[i] + (i != vectorLookup.length-1? "," : ""));
    }
    f.print("], dtype=np.int32)\n");
    System.out.print("saved!");
  }
  public static void generateAndSaveJavaCode(String[] docstring, PrintWriter f)
  {
    f.println("This is not implemented yet.");
  }
}
