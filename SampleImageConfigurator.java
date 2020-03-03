import javax.swing.JFrame;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.BorderLayout;
import javax.swing.*;
import blayzeTechUtils.env.TPolygonEntity;
import blayzeTechUtils.math.Point;

// Listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SampleImageConfigurator extends JFrame implements ActionListener {

  private static class ImageContainer extends JPanel{
    private int width, height;
    public TPolygonEntity reticule;

    public BufferedImage image = null;

    public ImageContainer (int w, int h)
    {
      super();
      this.width = w;
      this.height = h;
      this.setPreferredSize(new Dimension(w, h));

      // Draw the reticule
      int reticuleResolution = 10;// Maybe this should be the hoz. resolution of the output?
      Point[] reticulePoints = new Point[reticuleResolution +1];
      for(int i = 0; i<reticuleResolution; i++)
      {
        double angle = i * (Math.PI*2/reticuleResolution);
        reticulePoints[i] = new Point(Math.cos(angle)*0.5, Math.sin(angle)*0.5);
      }
      reticulePoints[reticuleResolution] = new Point(0.25,0);
      reticule = new TPolygonEntity(0,0,reticulePoints);
      reticule.setXscale(100);
      reticule.setYscale(100);
    }
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      ((Graphics2D)g).drawImage(image,0,0,getWidth(),getHeight(), null);
      reticule.draw((Graphics2D)g);
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
  private ImageContainer imageContainer;
  private double reticuleSpeed = 1, reticuleScaleRate = 1;
  
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

    // The control buttons:
    JPanel controlBtns = new JPanel();
    for(ControlButtonType t : ControlButtonType.values())
    {
      JButton btn = new JButton(t.toString());
      btn.addActionListener(this);
      controlBtns.add(btn);
    }
    this.add(controlBtns, BorderLayout.SOUTH);

    // Prepare top-level UI
    pack();
    setResizable(true);
    setVisible(true);
    setLocationRelativeTo(null);
  }
  public void actionPerformed(ActionEvent e)
  {
    switch(ControlButtonType.fromString(e.getActionCommand()))
    {
      case UP:
        imageContainer.reticule.setY(imageContainer.reticule.getY() - reticuleSpeed);
      break;
      case DOWN:
        imageContainer.reticule.setY(imageContainer.reticule.getY() + reticuleSpeed);
      break;
      case LEFT:
        imageContainer.reticule.setX(imageContainer.reticule.getX() - reticuleSpeed);
      break;
      case RIGHT:
        imageContainer.reticule.setX(imageContainer.reticule.getX() + reticuleSpeed);
      break;
      case UPSCALE:
        imageContainer.reticule.setXscale(imageContainer.reticule.getXscale() + reticuleScaleRate);
        imageContainer.reticule.setYscale(imageContainer.reticule.getYscale() + reticuleScaleRate);
      break;
      case DOWNSCALE:
        imageContainer.reticule.setXscale(imageContainer.reticule.getXscale() - reticuleScaleRate);
        imageContainer.reticule.setYscale(imageContainer.reticule.getYscale() - reticuleScaleRate);
      break;
      default:
      break;
    }
    imageContainer.repaint();
  }

  public void setImage(BufferedImage img)
  {
    imageContainer.image = img;
    imageContainer.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
    renderImage();
  }
  private void renderImage()
  {
    // Draw the image
    imageContainer.repaint();
  }
}
