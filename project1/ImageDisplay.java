import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne;
    BufferedImage overlayImage; // Stores the overlay image
    //    int width = 1920; // default image width and height for images under the 1920x1080_data_samples folder
//    int height = 1080;
    int width = 7680; // default image width and height for images under the aliasing_test_data_samples folder
    int height = 4320;
    int windowSize = 200; // Default window size for overlay
    private JLabel overlayLabel; // JLabel for displaying the overlay image

    // Method to read an image from a file and populate a BufferedImage
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            File file = new File(imgPath);
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();

            int ind = 0;
// Iterate through each row (y-coordinate) of the image
            for (int y = 0; y < height; y++) {
                // Iterate through each column (x-coordinate) of the image
                for (int x = 0; x < width; x++) {
                    // Initialize alpha byte (transparency) to 0
                    byte a = 0;

                    // Extract red, green, and blue bytes from the bytes array
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    // Convert individual color bytes to a single pixel value and set it in the BufferedImage
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);

                    // Move to the next set of color bytes for the next pixel
                    ind++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to scale an image using nearest-neighbor interpolation
    private void scaleImageNearestNeighbor(double scale) {
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Loop through the new image and map pixels from the original using nearest-neighbor approach
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int originalX = (int) (x / scale);
                int originalY = (int) (y / scale);
                int rgb = imgOne.getRGB(originalX, originalY);
                scaledImage.setRGB(x, y, rgb);
            }
        }

        imgOne = scaledImage;
        width = newWidth;
        height = newHeight;
    }

    // Method to scale an image with anti-aliasing
    private void scaleImageWithAntiAliasing(double scale) {
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Loop through the new image and apply anti-aliasing by averaging neighboring pixels
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double originalX = x / scale;
                double originalY = y / scale;

                // Calculate coordinates and boundaries for averaging
                int x1 = (int) Math.floor(originalX);
                int x2 = x1 + 1;
                int y1 = (int) Math.floor(originalY);
                int y2 = y1 + 1;
                // Ensure x1 is within the image boundaries (minimum: 0, maximum: width - 1)
                x1 = Math.max(0, Math.min(x1, width - 1));
                // Ensure x2 is within the image boundaries
                x2 = Math.max(0, Math.min(x2, width - 1));
                // Ensure y1 is within the image boundaries (minimum: 0, maximum: height - 1)
                y1 = Math.max(0, Math.min(y1, height - 1));
                // Ensure y2 is within the image boundaries (minimum: 0, maximum: height - 1)
                y2 = Math.max(0, Math.min(y2, height - 1));

                // Use a 3x3 grid for averaging nearby pixel colors
                int rgb1 = imgOne.getRGB(x1, y1);
                int rgb2 = imgOne.getRGB(x2, y1);
                int rgb3 = imgOne.getRGB(x1, y2);
                int rgb4 = imgOne.getRGB(x2, y2);

                // Calculate averaged color values
                int r = (int) ((getRed(rgb1) + getRed(rgb2) + getRed(rgb3) + getRed(rgb4)) / 4);
                int g = (int) ((getGreen(rgb1) + getGreen(rgb2) + getGreen(rgb3) + getGreen(rgb4)) / 4);
                int b = (int) ((getBlue(rgb1) + getBlue(rgb2) + getBlue(rgb3) + getBlue(rgb4)) / 4);

                int avgColor = (0xFF << 24) | (r << 16) | (g << 8) | b;
                scaledImage.setRGB(x, y, avgColor);
            }
        }

        imgOne = scaledImage;
        width = newWidth;
        height = newHeight;
    }

    // Helper method to extract the red component from an RGB color
    private int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    // Helper method to extract the green component from an RGB color
    private int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    // Helper method to extract the blue component from an RGB color
    private int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    // Method to create and display an overlay image centered around a specified point
    private void createOverlayImage(int x, int y) {
        // Ensure the overlay coordinates are within bounds
        int x1 = Math.max(0, x - windowSize / 2);
        int y1 = Math.max(0, y - windowSize / 2);

        int x2 = Math.min(width, x + windowSize / 2);
        int y2 = Math.min(height, y + windowSize / 2);

        int overlayWidth = x2 - x1;
        int overlayHeight = y2 - y1;

        overlayImage = new BufferedImage(overlayWidth, overlayHeight, BufferedImage.TYPE_INT_RGB);

        // Copy the corresponding portion of the original image to the overlay image
        for (int oy = 0; oy < overlayHeight; oy++) {
            for (int ox = 0; ox < overlayWidth; ox++) {
                int rgb = imgOne.getRGB(x1 + ox, y1 + oy);
                overlayImage.setRGB(ox, oy, rgb);
            }
        }

        // Create an ImageIcon from the overlay image
        ImageIcon overlayIcon = new ImageIcon(overlayImage);

        // Set the overlayLabel's icon and dimensions
        overlayLabel.setIcon(overlayIcon);
        overlayLabel.setPreferredSize(new Dimension(overlayWidth, overlayHeight));

        // Refresh the overlayLabel to ensure it's displayed
        overlayLabel.revalidate();
        overlayLabel.repaint();
    }


    // Method to display the main application window
    public void showIms(String[] args) {
        String imagePath = args[0];
        double scale = Double.parseDouble(args[1]);
        boolean antiAliasing = Integer.parseInt(args[2]) == 1;
        windowSize = Integer.parseInt(args[3]);

        // Create a BufferedImage and read the image from the specified path
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, imagePath, imgOne);

        // If scaling is requested and anti-aliasing is enabled, apply anti-aliased scaling first
        if (scale < 1.0 && antiAliasing) {
            scaleImageWithAntiAliasing(scale);
        }

        // If scaling is requested and anti-aliasing is disabled or not required, apply nearest-neighbor scaling
        if (scale < 1.0 && !antiAliasing) {
            scaleImageNearestNeighbor(scale);
        }

        // Create and configure the main application frame
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // Create a JLabel to display the main image
        lbIm1 = new JLabel(new ImageIcon(imgOne));

        frame.getContentPane().add(lbIm1, BorderLayout.CENTER);

        // Initialize overlayLabel for displaying the overlay image
        overlayLabel = new JLabel();
        overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Add overlayLabel to the frame
        frame.getContentPane().add(overlayLabel, BorderLayout.WEST);

        // Pack and display the frame
        frame.pack();
        frame.setVisible(true);

        // Add MouseListener and MouseMotionListener for overlay interaction
        lbIm1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // When Control key is pressed, create and display an overlay image
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    createOverlayImage(e.getX(), e.getY());
                }
            }
        });

        lbIm1.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // When Control key is pressed and the mouse is moved, update the overlay image
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    createOverlayImage(e.getX(), e.getY());
                }
            }
        });
    }

    // Main method to create an instance of ImageDisplay and display the image
    public static void main(String[] args) {
        ImageDisplay ren = new ImageDisplay();
        ren.showIms(args);
    }
}