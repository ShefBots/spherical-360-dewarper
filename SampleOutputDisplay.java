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
      this.width = w;
      this.height = h;
      this.setPreferredSize(new Dimension(w, h));

      image = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
    }
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      ((Graphics2D)g).drawImage(image,0,0,getWidth(), getHeight(), null);
    }
  }

  private JScrollPane scrollPane;
  public ImageContainer imageContainer;

  public SampleOutputDisplay(int width, int height){
    super();
    setTitle("Sample Output");
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
}
