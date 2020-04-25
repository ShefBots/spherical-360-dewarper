import javax.swing.JFrame;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.BorderLayout;
import javax.swing.*;
import blayzeTechUtils.env.TPolygonEntity;
import blayzeTechUtils.env.TPPolygonEntity;
import blayzeTechUtils.math.Point;

// Listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SampleImageConfigurator extends JFrame implements ActionListener {

  public static class ImageContainer extends JPanel {
    public TPolygonEntity reticule;
    private TPPolygonEntity subReticule;

    public BufferedImage image = null;

    public BufferedImage NNmaskImage = null;
    private BufferedImage NNmaskDisplay = null;
    private boolean usingNNmask = false;

    public ImageContainer (int w, int h)
    {
      super();
      this.setPreferredSize(new Dimension(w, h));

      // Draw the reticule
      int reticuleResolution = 100;// Maybe this should be the hoz. resolution of the output?
      Point[] reticulePoints = new Point[reticuleResolution +1];
      for(int i = 0; i<reticuleResolution; i++)
      {
        double angle = i * (Math.PI*2/reticuleResolution);
        reticulePoints[i] = new Point(Math.cos(angle)*0.5, Math.sin(angle)*0.5);
      }
      reticulePoints[reticuleResolution] = new Point(0.25,0);
      reticule = new TPolygonEntity(w/2.0,h/2.0,reticulePoints);
      reticule.setXscale(100);
      reticule.setYscale(100);

      // Draw the subreticule
      Point[] subretPoints = new Point[6];
      subretPoints[0] = new Point(0, -0.5);
      subretPoints[1] = new Point(0, 0.5);
      subretPoints[2] = new Point(0, 0);
      subretPoints[3] = new Point(-0.5, 0);
      subretPoints[4] = new Point(0.5, 0);
      subretPoints[5] = new Point(0, 0);
      subReticule = new TPPolygonEntity(0,0,subretPoints, reticule);
    }

    // Checks if the reticule is within the bounds of the image, and moves it inward if not.
    public void bounceReticule()
    {
      // The outer reticule has a radius of 0.5 and a diameter of 1, a.k.a. it's scale is it's total diameter.
      double minImageDimension = Math.min(image.getWidth()-1, image.getHeight()-1);
      reticule.setScale(Math.min(reticule.getXscale(), minImageDimension));
      int radius = (int)Math.ceil(reticule.getXscale()/2.0);// Ceil to avoid half-pixel cliping
      reticule.setX(Math.min(Math.max(reticule.getX(), radius), image.getWidth()-radius-1));
      reticule.setY(Math.min(Math.max(reticule.getY(), radius), image.getHeight()-radius-1));
    }

    public void setImage(BufferedImage img)
    {
      image = img;
      Dimension imgDim = new Dimension(img.getWidth(), img.getHeight());
      setPreferredSize(imgDim);
      setSize(imgDim);
      setMaximumSize(imgDim);
      reticule.setX(imgDim.getWidth()/2.0);
      reticule.setY(imgDim.getHeight()/2.0);
      bounceReticule();
      repaint();
      revalidate();
    }
    public void setNNmaskImage(boolean enableDisable)
    {
      usingNNmask = enableDisable;
      repaint();
    }
    public void setNNmaskImage(BufferedImage img)
    {
      if(img.getWidth() != image.getWidth() || img.getHeight() != image.getHeight())
      {
        System.out.println("ERROR: The provided mask does not possess the same dimensions as the sample image.");
        return;
      }

      // Save the mask image
      usingNNmask = true;
      NNmaskImage = img;

      // Generate the display image
      NNmaskDisplay = generateOutline(img, Color.RED);
      repaint();
    }
    private BufferedImage generateOutline(BufferedImage img, Color c)
    {
      int width = img.getWidth();
      int height = img.getHeight();
      BufferedImage disp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      for(int x = 0; x < width; x++)
        for(int y = 0; y < height; y++)
        {
          boolean blackNear = false;
          if(new Color(img.getRGB(x,y)).getRed() >= 128)
            for(int i = Math.max(0,x-1); i<=Math.min(width-1,x+1); i++)
              for(int o = Math.max(0,y-1); o<=Math.min(height-1, y+1); o++)
              {
                if(i == x && o == y)
                  continue;
                if(new Color(img.getRGB(i,o)).getRed() < 128)
                  blackNear = true;
              }
          if(blackNear)
            disp.setRGB(x,y,c.getRGB());
        }
      return disp;
    }

    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      g.setColor(Color.ORANGE);
      ((Graphics2D)g).drawImage(image,0,0,getWidth(),getHeight(), null);
      if(NNmaskDisplay != null && usingNNmask)
        ((Graphics2D)g).drawImage(NNmaskDisplay,0,0,getWidth(),getHeight(), null);
      reticule.draw((Graphics2D)g);
      subReticule.draw((Graphics2D)g);
    }
  }

  private static enum ControlButtonType {
    UP, DOWN, LEFT, RIGHT, UPSCALE, DOWNSCALE, CLOCKWISE_ROTATE, COUNTERCLOCKWISE_ROTATE;

    private static final String[] stringValues = new String[]{"^","v","<",">","*","/","CW","CCW"};
    @Override
    public String toString()
    {
      switch(this)
      {
        case UP: return(stringValues[0]);
        case DOWN: return(stringValues[1]);
        case LEFT: return(stringValues[2]);
        case RIGHT: return(stringValues[3]);
        case UPSCALE: return(stringValues[4]);
        case DOWNSCALE: return(stringValues[5]);
        case CLOCKWISE_ROTATE: return(stringValues[6]);
        case COUNTERCLOCKWISE_ROTATE: return(stringValues[7]);
        default: break;
      }
      return super.toString();
    }

    public static ControlButtonType fromString(String s)
    {
      for(ControlButtonType bt : ControlButtonType.values())
        if(s.equals(bt.toString()))
          return bt;
      return null;
    }
  }

  private JScrollPane scrollPane;
  public ImageContainer imageContainer;
  private JTextField moveSpeedSelector, scaleSelector, rotationSelector; // Contain the actual information here (A little sloppy, bit will work)
  
  public SampleImageConfigurator(){
    // Setup
    super();
    this.setLayout(new BorderLayout());
    setTitle("Sample Image Configurator");
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    // Sample Image Display
    JPanel containerContainer = new JPanel();// Put the container in a container to center it
    imageContainer = new ImageContainer(1280, 720);
    containerContainer.add(imageContainer);
    scrollPane = new JScrollPane(containerContainer);
    this.add(scrollPane, BorderLayout.CENTER);

    //// The control buttons:
    JPanel controlBtns = new JPanel();
    for(ControlButtonType t : ControlButtonType.values())
    {
      JButton btn = new JButton(t.toString());
      btn.addActionListener(this);
      controlBtns.add(btn);
    }
    // The control text fields:
    controlBtns.add(new JLabel("Move Speed:"));
    moveSpeedSelector = new JTextField("1");
    int newSize = (int)(moveSpeedSelector.getPreferredSize().width*5);
    moveSpeedSelector.setPreferredSize(new Dimension(newSize, moveSpeedSelector.getPreferredSize().height));
    controlBtns.add(moveSpeedSelector);
    controlBtns.add(new JLabel("Radius:"));
    scaleSelector = new JTextField("100");
    scaleSelector.setPreferredSize(new Dimension(newSize, scaleSelector.getPreferredSize().height));
    scaleSelector.addActionListener(this);
    controlBtns.add(scaleSelector);
    controlBtns.add(new JLabel("Rotation (degrees):"));
    rotationSelector = new JTextField("0");
    rotationSelector.setPreferredSize(new Dimension(newSize, rotationSelector.getPreferredSize().height));
    rotationSelector.addActionListener(this);
    controlBtns.add(rotationSelector);

    this.add(controlBtns, BorderLayout.SOUTH);
    
    // The Instructions
    this.add(new JLabel("Align reticule with location of top reflective ray hits"), BorderLayout.NORTH);

    // Prepare top-level UI
    pack();
    setResizable(true);
    setVisible(true);
    setLocationRelativeTo(null);
  }
  private void rerender()
  {
    imageContainer.reticule.setScale(Double.valueOf(scaleSelector.getText()));
    imageContainer.reticule.setRotation(Double.valueOf(rotationSelector.getText())/180 * Math.PI);
  }
  public void actionPerformed(ActionEvent e)
  {
    rerender();
    try{
      double alterSpeed = Double.valueOf(moveSpeedSelector.getText());
      switch(ControlButtonType.fromString(e.getActionCommand()))
      {
        case UP:
          imageContainer.reticule.setY(imageContainer.reticule.getY() - alterSpeed);
        break;
        case DOWN:
          imageContainer.reticule.setY(imageContainer.reticule.getY() + alterSpeed);
        break;
        case LEFT:
          imageContainer.reticule.setX(imageContainer.reticule.getX() - alterSpeed);
        break;
        case RIGHT:
          imageContainer.reticule.setX(imageContainer.reticule.getX() + alterSpeed);
        break;
        case UPSCALE:
          imageContainer.reticule.setScale(imageContainer.reticule.getXscale() + alterSpeed);
          scaleSelector.setText(Double.toString(imageContainer.reticule.getXscale()));
        break;
        case DOWNSCALE:
          imageContainer.reticule.setScale(imageContainer.reticule.getXscale() - alterSpeed);
          scaleSelector.setText(Double.toString(imageContainer.reticule.getXscale()));
        break;
        case COUNTERCLOCKWISE_ROTATE:
          imageContainer.reticule.setRotation(imageContainer.reticule.getRotation() - alterSpeed/180*Math.PI);
          rotationSelector.setText(Double.toString(imageContainer.reticule.getRotation()/Math.PI*180));
        break;
        case CLOCKWISE_ROTATE:
          imageContainer.reticule.setRotation(imageContainer.reticule.getRotation() + alterSpeed/180*Math.PI);
          rotationSelector.setText(Double.toString(imageContainer.reticule.getRotation()/Math.PI*180));
        break;
        default:
        break;
      }
    }catch(Exception ex){
      // This happens if we don't parse a regular command, but instead a string from one of the parameter options.
    }

    imageContainer.bounceReticule();
    imageContainer.repaint();
    SphericalDewarper.computeAndRenderVisualiser();
  }

  /**
   * Returns the projected coordinate `angle` way around the reticule and `distanceFromCenter` from the center.
   *
   * @param	angle	the angle around the reticule this is (0-1)
   * @param	distanceFromCenter	the distance from the center (0-1)
   */
  public Point getCoordinateAt(double angle, double distanceFromCenter)
  {
	double trueDistance = distanceFromCenter*0.5;// Downscale to match the reticule
	double trueAngle = angle*Math.PI*2;// Scale to be in full 360 deg range.
	Point point = new Point(Math.cos(trueAngle)*trueDistance, Math.sin(trueAngle)*trueDistance);
	return(imageContainer.reticule.projectToWorld(point));
  }
}
