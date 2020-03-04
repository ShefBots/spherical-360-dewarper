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

  public static class ImageContainer extends JPanel{
    private int width, height;
    public TPolygonEntity reticule;
    private TPPolygonEntity subReticule;

    public BufferedImage image = null;

    public ImageContainer (int w, int h)
    {
      super();
      this.width = w;
      this.height = h;
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
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      g.setColor(Color.ORANGE);
      ((Graphics2D)g).drawImage(image,0,0,getWidth(),getHeight(), null);
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
  private static Double reticuleScaleRate = 1.0, reticuleRotateRate = 1.0/180 * Math.PI;
  private JTextField moveSpeedSelector, scaleSelector, rotationSelector; // Contain the actual information here (A little sloppy, bit will work)
  
  public SampleImageConfigurator(){
    // Setup
    super();
    this.setLayout(new BorderLayout());
    setTitle("Sample Image Configurator");
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    // Sample Image Display
    imageContainer = new ImageContainer(1280, 720);
    scrollPane = new JScrollPane(imageContainer);
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
      switch(ControlButtonType.fromString(e.getActionCommand()))
      {
        case UP:
          imageContainer.reticule.setY(imageContainer.reticule.getY() - Double.valueOf(moveSpeedSelector.getText()));
        break;
        case DOWN:
          imageContainer.reticule.setY(imageContainer.reticule.getY() + Double.valueOf(moveSpeedSelector.getText()));
        break;
        case LEFT:
          imageContainer.reticule.setX(imageContainer.reticule.getX() - Double.valueOf(moveSpeedSelector.getText()));
        break;
        case RIGHT:
          imageContainer.reticule.setX(imageContainer.reticule.getX() + Double.valueOf(moveSpeedSelector.getText()));
        break;
        case UPSCALE:
          imageContainer.reticule.setScale(imageContainer.reticule.getXscale() + reticuleScaleRate);
          scaleSelector.setText(Double.toString(imageContainer.reticule.getXscale()));
        break;
        case DOWNSCALE:
          imageContainer.reticule.setScale(imageContainer.reticule.getXscale() - reticuleScaleRate);
          scaleSelector.setText(Double.toString(imageContainer.reticule.getXscale()));
        break;
        case COUNTERCLOCKWISE_ROTATE:
          imageContainer.reticule.setRotation(imageContainer.reticule.getRotation() - reticuleRotateRate);
          rotationSelector.setText(Double.toString(imageContainer.reticule.getRotation()/Math.PI*180));
        break;
        case CLOCKWISE_ROTATE:
          imageContainer.reticule.setRotation(imageContainer.reticule.getRotation() + reticuleRotateRate);
          rotationSelector.setText(Double.toString(imageContainer.reticule.getRotation()/Math.PI*180));
        break;
        default:
        break;
      }
    }catch(Exception ex){
      // This happens if we don't parse a regular command, but instead a string from one of the parameter options.
    }

    imageContainer.repaint();
  }

  public void setImage(BufferedImage img)
  {
    imageContainer.image = img;
    imageContainer.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
    imageContainer.reticule.setX(img.getWidth()/2.0);
    imageContainer.reticule.setY(img.getHeight()/2.0);
    renderImage();
  }
  private void renderImage()
  {
    // Draw the image
    imageContainer.repaint();
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
