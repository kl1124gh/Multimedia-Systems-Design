import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class ImageAnalysis {
    private BufferedImage imgOne;
    private int width = 7680;
    private int height = 4320;

    // Function to calculate the Mean Squared Error (MSE) between two images
    private double calculateMeanSquaredError(BufferedImage original, BufferedImage reconstructed) {
        double sumSquaredError = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgbOriginal = original.getRGB(x, y);
                int rgbReconstructed = reconstructed.getRGB(x, y);

                int redDiff = ((rgbOriginal >> 16) & 0xFF) - ((rgbReconstructed >> 16) & 0xFF);
                int greenDiff = ((rgbOriginal >> 8) & 0xFF) - ((rgbReconstructed >> 8) & 0xFF);
                int blueDiff = (rgbOriginal & 0xFF) - (rgbReconstructed & 0xFF);

                sumSquaredError += redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff;
            }
        }

        return sumSquaredError / (width * height);
    }

    // Function to read an image from a file into a BufferedImage
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            File file = new File(imgPath);
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();

            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Function to reconstruct missing samples in the image
    // Function to reconstruct missing samples in the image
// Function to reconstruct missing samples in the image
    private void reconstructMissingSamples(double missingPercentage) {
        // Create a copy of the original image to store the reconstructed image
        BufferedImage reconstructedImage = deepCopy(imgOne);

        // Calculate the total number of pixels in the image
        int totalPixels = width * height;

        // Calculate the number of missing pixels based on the missing percentage
        int missingPixels = (int) (totalPixels * missingPercentage);

        // Create a list of indices representing all pixels in the image
        List<Integer> pixelIndices = new ArrayList<>(totalPixels);
        for (int i = 0; i < totalPixels; i++) {
            pixelIndices.add(i);
        }

        // Shuffle the list of pixel indices randomly
        Collections.shuffle(pixelIndices);

        // Mark the first 'missingPixels' indices as missing and reconstruct them
        for (int i = 0; i < missingPixels; i++) {
            // Get the index of the pixel to be reconstructed from the shuffled list
            int index = pixelIndices.get(i);

            // Calculate the x and y coordinates of the pixel from its index
            int x = index % width;
            int y = index / width;

            // Sample is missing, reconstruct it using weighted averaging
            int sumRed = 0;
            int sumGreen = 0;
            int sumBlue = 0;
            int totalWeight = 0;

            // Iterate over neighboring pixels for weighted averaging
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    // Calculate the coordinates of the neighboring pixel
                    int nx = x + dx;
                    int ny = y + dy;

                    // Check if the neighboring pixel is within image bounds
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        // Get the color of the neighboring pixel
                        int pixel = imgOne.getRGB(nx, ny);
                        int weight = 1; // Adjust the weight as needed

                        // Accumulate color components with weights
                        sumRed += ((pixel >> 16) & 0xFF) * weight;
                        sumGreen += ((pixel >> 8) & 0xFF) * weight;
                        sumBlue += (pixel & 0xFF) * weight;
                        totalWeight += weight;
                    }
                }
            }

            // Calculate the weighted average color
            int averageRed = totalWeight > 0 ? sumRed / totalWeight : 0;
            int averageGreen = totalWeight > 0 ? sumGreen / totalWeight : 0;
            int averageBlue = totalWeight > 0 ? sumBlue / totalWeight : 0;

            // Create the average color and set it for the missing pixel in the reconstructed image
            int averageColor = (255 << 24) | (averageRed << 16) | (averageGreen << 8) | averageBlue;
            reconstructedImage.setRGB(x, y, averageColor);
        }

        // Save the reconstructed image as a .jpg file
        try {
            // Generate a unique filename based on the missing percentage
            int imageNumber = (int) (missingPercentage * 100);
            String imageName = "reconstructed_images/reconstructed_image" + imageNumber + ".jpg";
            File outputImage = new File(imageName);

            // Create the "reconstructed_images" folder if it doesn't exist
            File folder = new File("reconstructed_images");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Write the reconstructed image to the file in JPEG format
            ImageIO.write(reconstructedImage, "jpg", outputImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Calculate and print the mean squared error between the original and reconstructed images
        double mse = calculateMeanSquaredError(imgOne, reconstructedImage);
        System.out.println("Mean Squared Error for " + missingPercentage + ": " + mse);
    }



    // Function to create a deep copy of a BufferedImage
    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    // Function to analyze an image with a specified missing percentage
    public void analyzeImage(String imagePath, double missingPercentage) {
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, imagePath, imgOne);

        reconstructMissingSamples(missingPercentage);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ImageAnalysis <image_path>");
            return;
        }

        String imagePath = args[0];

        for (int i = 1; i <= 50; i++) {
            double missingPercentage = i / 100.0; // Set missingPercentage from 0.01 to 0.50
            System.out.println("Missing Percentage: " + missingPercentage);

            ImageAnalysis imageAnalysis = new ImageAnalysis();
            imageAnalysis.analyzeImage(imagePath, missingPercentage);
        }
    }
}

