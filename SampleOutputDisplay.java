public class SampleOutputDisplay extends JFrame {
  private static class ImageContainer extends JPanel {
    public BufferedImage image;
    public ImageContainer (int w, int h)
    {
      image = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
    }
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      ((Graphics2D)g).drawImage(image,0,0,getWidth(), getHeight(), null);
    }
  }
}
