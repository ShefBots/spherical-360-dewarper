import javax.swing.JFrame;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.BorderLayout;
import javax.swing.*;
import blayzeTechUtils.env.TPolygonEntity;
import blayzeTechUtils.math.Point;

public class SampleImageConfigurator extends JFrame{

  private static class ImageContainer extends JPanel{
    private int width, height;
    private TPolygonEntity reticule;

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

  private JScrollPane scrollPane;
  private ImageContainer imageContainer;
  
  public SampleImageConfigurator(){
    super();

    this.setLayout(new BorderLayout());
    setTitle("Sample Image Configurator");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

    imageContainer = new ImageContainer(1280, 720);

    scrollPane = new JScrollPane(imageContainer);
    this.add(scrollPane, BorderLayout.CENTER);

    pack();
    setResizable(true);
    setVisible(true);
    setLocationRelativeTo(null);
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
