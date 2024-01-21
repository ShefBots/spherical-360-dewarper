import javax.swing.JFrame;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.BorderLayout;
import javax.swing.*;
import blayzeTechUtils.env.TPolygonEntity;
import blayzeTechUtils.env.TPPolygonEntity;
import blayzeTechUtils.math.Point;

public class SampleOutputDisplay extends JFrame {
  public static class ImageContainer extends JPanel {
    private int width, height;

    public BufferedImage image;
    public ImageContainer (int w, int h)
    {
      super();
      setNewImage(w,h);
    }
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      ((Graphics2D)g).drawImage(image,0,0,getWidth(), getHeight(), null);
    }
    public void setNewImage(int width, int height)
    {
      this.width = width;
      this.height = height;
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      this.setPreferredSize(new Dimension(width, height));
    }
  }

  private JScrollPane scrollPane;
  public ImageContainer imageContainer;

  public SampleOutputDisplay(int width, int height, String type)
  {
    super();
    setTitle("Sample Output: " + type);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    // Sample Output Display
    imageContainer = new ImageContainer(width, height);
    scrollPane = new JScrollPane(imageContainer);
    this.add(scrollPane);

    // Prepare top-level UI
    pack();
    setResizable(true);
    setVisible(true);
    setLocationRelativeTo(null);
  }
  public void setNewImage(int width, int height)
  {
    imageContainer.setNewImage(width, height);
    pack();
  }
}
