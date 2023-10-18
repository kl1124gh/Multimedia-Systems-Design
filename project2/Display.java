import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Display {
    JFrame frame;
    JLabel lbIm1;
    static BufferedImage inputImg; //input image
    static BufferedImage objImg; //object image
    static int width = 640; // default image width and height
    static int height = 480;

    static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    private static void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;

            File file = new File(imgPath);
            if (file.exists()) {
                System.out.println("File exists: " + imgPath);
            } else {
                System.out.println("File does not exist: " + imgPath);
            }
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];
            raf.read(bytes);
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
            System.out.println("Image read successfully: " + imgPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //在创建hsv的s值直方图时，过滤掉绿色，但是不过滤掉黄色
    public static boolean isGreen_S(Color color) {
        float[] hsv = rgbToHSV(color.getRed(), color.getGreen(), color.getBlue());
        float saturation = hsv[1]; // 获取HSV中的饱和度(S)值

        // 判断颜色是否接近绿色，并且饱和度较高（可以根据需要进行调整）
        return saturation > 0.5f && hsv[0] >= 60 && hsv[0] <= 180;
    }
    //when we create the histograms for the object image, need to remove the green background
    public static boolean isGreen(Color color) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        return red < 15 && green > 230 && blue < 15;
    }
    //create the histogram
    public static int[] createYUV_UHistogram(BufferedImage image) {
        int[] histogram = new int[256];  // Assuming 8-bit U channel for YUV
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                if (isGreen(color)){
                    continue;
                }
                // Convert RGB to YUV
                int yuvU = (int) (-0.147 * r - 0.289 * g + 0.436 * b);

                // Ensure values are within the range [0, 255]
                yuvU = Math.max(0, Math.min(255, yuvU + 128));

                // Update the histogram
                histogram[yuvU]++;
            }
        }
        return histogram;
    }

    public static int[] createYUV_VHistogram(BufferedImage image) {
        int[] histogram = new int[256];  // Assuming 8-bit V channel for YUV
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                if (isGreen(color)){
                    continue;
                }
                // Convert RGB to YUV using the rgbToYUV function
                float[] yuv = rgbToYUV(r, g, b);
                int yuvV = Math.round(yuv[2]); // Use the V component from YUV

                // Ensure values are within the range [0, 255]
                yuvV = Math.max(0, Math.min(255, yuvV));

                // Update the histogram
                histogram[yuvV]++;
            }
        }
        return histogram;
    }
    //method to create a histogram of the object image using hue value, excluding the green color background
// Method to create a histogram of the object image using hue value, excluding the green color background
    public static int[] createHSV_HHistogram(BufferedImage image) {
        int[] histogram = new int[361];  // Assuming 360 bins for H channel in HSV
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                // Check if the pixel is green (using your isGreen method)
                if (isGreen(color)) {
                    continue; // Skip green pixels
                }
                // Convert RGB to HSV
                float[] hsv = rgbToHSV(r, g, b);

                // Update the histogram without rounding
                int hueIndex = (int) (hsv[0]);
                histogram[hueIndex]++;
            }
        }
        return histogram;
    }
    public static int[] createHSV_SHistogram(BufferedImage image) {
        int[] histogram = new int[101];  // Assuming 8-bit S channel in HSV

        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                if(isGreen_S(color)){
                    continue;//skip green pixels
                }
                // Convert RGB to HSV
                float[] hsv = rgbToHSV(r, g, b);

                // Update the histogram
                int saturation = Math.round(hsv[1] * 100);
                if (saturation >= 0 && saturation <= 100) {
                histogram[saturation]++;}else {
                    // Handle the out-of-bounds value or simply skip
                    System.out.println("Out of bounds saturation value: " + saturation);
                }
            }
        }
        return histogram;
    }
    // a method to get the peak value of histogram
    public static int findHistogramPeakValue(int[] histogram) {
        int maxIndex = 0;
        int maxValue = Integer.MIN_VALUE;

        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > maxValue) {
                maxValue = histogram[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }
public static float[] rgbToHSV(int r, int g, int b) {
    float min = Math.min(r, Math.min(g, b));
    float max = Math.max(r, Math.max(g, b));
    float delta = max - min;

    float h, s;
    float v = max;

    if (delta < 0.00001) {
        s = 0;
        h = 0;
        return new float[]{h, s, v};
    }

    if (max > 0.0) {
        s = delta / max;
    } else {
        s = 0;
        h = 0;
        return new float[]{h, s, v};
    }

    if (r >= max) {
        h = (g - b) / delta;
    } else {
        if (g >= max) {
            h = 2.0f + (b - r) / delta;
        } else {
            h = 4.0f + (r - g) / delta;
        }
    }
    h *= 60.0;
    if (h < 0.0) {
        h += 360.0;
    }
    return new float[]{h, s, v};
}
private static int clip(int value) {
    return Math.min(Math.max(value, 0), 255);
}

    private static float[] rgbToYUV(int r, int g, int b) {
        float[] yuv = new float[3];

        // 计算Y（亮度）分量
        yuv[0] = 0.299f * r + 0.587f * g + 0.114f * b;
        yuv[0] = clip((int) yuv[0]);

        // 计算U（蓝色色度）分量
        yuv[1] = -0.14713f * r - 0.288862f * g + 0.436f * b;
        yuv[1] = clip((int) (yuv[1] + 128));

        // 计算V（红色色度）分量
        yuv[2] = 0.615f * r - 0.51499f * g - 0.10001f * b;
        yuv[2] = clip((int) (yuv[2] + 128));

        return yuv;
    }

    //convert rgb image to hsv image
    //a method to get the dominant color of an object image (using its hue histogram's peak value, hue represents color)
    public static String getDominantColor(int[] anyHistogram) {
        // maxCount stores the histogram's peak value's y value
        int maxCount = findHistogramPeakValue(anyHistogram);

        // 根据Hue值确定主导颜色
        String dominantColor;
        if ((maxCount >= 0 && maxCount <= 30) || (maxCount >= 330 && maxCount <= 360)) {
            dominantColor = "red";
        } else if (maxCount >= 30 && maxCount <= 60) {
            dominantColor = "yellow";
        } else if (maxCount >= 210 && maxCount <= 270) {
            dominantColor = "blue";
        } else {
            dominantColor = "unknown"; // 未知颜色
        }
        return dominantColor;
    }
    public static int[][] createBinaryMatrix(BufferedImage inputImage, BufferedImage objectImage) {

        int[] obj_hueHistogram = createHSV_HHistogram(objectImage);
        //get the obj_hueHistogram's peak value
        int obj_hue_peak = findHistogramPeakValue(obj_hueHistogram);
        // 获取对象的主导颜色
        String dominantColor = getDominantColor(obj_hueHistogram);
        System.out.println(dominantColor);

        // 遍历输入图像的每个像素
        int imageWidth = inputImage.getWidth();
        int imageHeight = inputImage.getHeight();
        System.out.println("width:" + imageWidth);
        System.out.println("height:" + imageHeight);
        //binary matrix to label if the current pixel in input image is 0 or 1
        int[][] binaryMatrix = new int[imageWidth][imageHeight];
        //iterating through evert pixel in the input image
        if (dominantColor.equals("yellow")) {
            //这里报错了
            int[] obj_SHistogram = createHSV_SHistogram(objectImage);
            int obj_S_peak = findHistogramPeakValue(obj_SHistogram);
            //System.out.println("程序运行到这里了吗？");
            System.out.println("obj_S_peak 的数值是");
            System.out.println(obj_S_peak);
            for (int y = 0; y < imageHeight; y++) {
                for (int x = 0; x < imageWidth; x++) {
                    Color pixelColor = new Color(inputImage.getRGB(x, y));
                    float[] hsv = rgbToHSV(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue());
                    float h = hsv[0];
                    float s = hsv[1] * 100;
                    //System.out.println("h" + h);
                    if ((h >= obj_hue_peak - 20 && h <= obj_hue_peak + 20) && (s >= obj_S_peak  && s <= obj_S_peak + 15)) {
                        binaryMatrix[x][y] = 1;
                    } else {
                        binaryMatrix[x][y] = 0;
                    }
                }
            }
        } else if (dominantColor.equals("red")) {
            //System.out.println("代码执行到这里，代表我们要用红蓝");
            int[] obj_VHistogram = createYUV_VHistogram(objectImage);
            int obj_V_peak = findHistogramPeakValue(obj_VHistogram);
            int[] obj_SHistogram = createHSV_SHistogram(objectImage);
            int obj_S_peak = findHistogramPeakValue(obj_SHistogram);
            System.out.println("obj_V_peak的数值是");
            System.out.print(obj_V_peak);
            System.out.println("start");
            for (int i = 0; i < obj_VHistogram.length; i++) {
                System.out.println(obj_VHistogram[i]);
            }
            System.out.println("end");
            for (int y = 0; y < imageHeight; y++) {
                for (int x = 0; x < imageWidth; x++) {
                    Color pixelColor = new Color(inputImage.getRGB(x, y));
                    int r = pixelColor.getRed();
                    int g = pixelColor.getGreen();
                    int b = pixelColor.getBlue();
                    if(r > 0 && r < 10 && g > 0 && g < 10 && b > 0 && b < 10){
                        continue;
                    }
                    //yuv[0] -- Y, yuv[1] -- U, yuv[2] -- v
                    float[] yuv = rgbToYUV(r, g, b);
                    float[] hsv = rgbToHSV(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue());
                    float s = hsv[1] * 100;
                    //System.out.print(yuv[2]);
                    int v = (int)yuv[2];
                    int lowerBound = Math.max(0, obj_V_peak - 40);
                    int upperBound = Math.min(255, obj_V_peak + 30);

                    //目前代码无法进入这个if 判断句，因为obj_U_peak 和obj_V_peak 取之不正常
                    if (v >= lowerBound && v <= upperBound) {
                        binaryMatrix[x][y] = 1;
                    } else {
                        binaryMatrix[x][y] = 0;
                    }
                }
            }
        }
        else if (dominantColor.equals("blue")) {
            System.out.println("代码执行到这里，代表我们要用蓝");
            int[] obj_UHistogram = createYUV_UHistogram(objectImage);
            int obj_U_peak = findHistogramPeakValue(obj_UHistogram);
            System.out.println("obj_U_peak的数值是");
            System.out.print(obj_U_peak);

            for (int y = 0; y < imageHeight; y++) {
                for (int x = 0; x < imageWidth; x++) {
                    Color pixelColor = new Color(inputImage.getRGB(x, y));
                    int r = pixelColor.getRed();
                    int g = pixelColor.getGreen();
                    int b = pixelColor.getBlue();
                    //yuv[0] -- Y, yuv[1] -- U, yuv[2] -- v
                    float[] yuv = rgbToYUV(r, g, b);
                    float u = Math.round(yuv[1]);
                    float v = Math.round(yuv[2]);
                    if (u >= obj_U_peak - 20 && u <= obj_U_peak + 20) {
                        //System.out.println("代码执行到这里了嘛");
                        binaryMatrix[x][y] = 1;
                    } else {
                        binaryMatrix[x][y] = 0;
                    }
                }
            }
        }
        return binaryMatrix;
    }

    //这个dfs函数是设计来找一个岛屿里的所有标记为1的坐标。从一个标记为1的点开始，它会递归地遍历所有相邻的、
    //还未访问过的1，并将它们的坐标添加到currentIsland列表中。这样，当dfs函数完成时，currentIsland就会包含该岛屿中所有点的坐标。
private static void dfs(int[][] binaryMatrix, int r, int c, List<Point> currentIsland) {
    int nr = binaryMatrix.length;
    int nc = binaryMatrix[0].length;
    Stack<Point> stack = new Stack<>();
    stack.push(new Point(r, c));
    while (!stack.isEmpty()) {
        Point current = stack.pop();
        int x = current.x;
        int y = current.y;
        if (x < 0 || x >= nr || y < 0 || y >= nc || binaryMatrix[x][y] == 0) {
            continue;
        }
        binaryMatrix[x][y] = 0; // 标记为已访问
        currentIsland.add(new Point(x, y)); // 添加当前坐标到岛屿列表
        stack.push(new Point(x - 1, y));
        stack.push(new Point(x + 1, y));
        stack.push(new Point(x, y - 1));
        stack.push(new Point(x, y + 1));
    }
}
    int[] getBoundingBox(List<Point> cluster) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Point point : cluster) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        return new int[]{minX, minY, maxX, maxY};
    }
    //返回每个小岛的所有像素坐标
    public static List<List<Point>> findAllIslands(int[][] binaryMatrix) {
        if (binaryMatrix == null || binaryMatrix.length == 0) {
            return new ArrayList<>();
        }
        int nr = binaryMatrix.length;
        int nc = binaryMatrix[0].length;
        List<List<Point>> allIslands = new ArrayList<>();

        for (int r = 0; r < nr; ++r) {
            for (int c = 0; c < nc; ++c) {
                if (binaryMatrix[r][c] == 1) {
                    List<Point> currentIsland = new ArrayList<>();
                    dfs(binaryMatrix, r, c, currentIsland);
                    allIslands.add(currentIsland); // 添加当前岛屿到所有岛屿的列表
                }
            }
        }
        return allIslands;
    }
    private static boolean islandsAreClose(List<Point> island1, List<Point> island2) {
        int maxDistance = 20; // 定义两个岛屿之间的最大距离
        for (Point p1 : island1) {
            for (Point p2 : island2) {
                double distance = Math.sqrt(Math.pow((p2.x-p1.x),2)+Math.pow((p2.y-p1.y),2));
                if (distance <= maxDistance) {
                    return true;
                }
            }
        }
        return false;
    }
    private static List<List<Point>> mergeAndFilterIslands(List<List<Point>> allIslands) {
        List<List<Point>> mergedAndFilteredIslands = new ArrayList<>();
        for (List<Point> island : allIslands) {
            boolean merged = false;
            for (List<Point> existingIsland : mergedAndFilteredIslands) {
                if (islandsAreClose(island, existingIsland)) {
                    existingIsland.addAll(island);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                mergedAndFilteredIslands.add(island);
            }
        }
        // 过滤岛屿，仅保留像素数量大于25的岛屿
        List<List<Point>> filteredIslands = new ArrayList<>();
        for (List<Point> island : mergedAndFilteredIslands) {
            if (island.size() > 50) {
                filteredIslands.add(island);
            }
        }
        return filteredIslands;
    }
    //给小岛们上色 看看画的如何
    public void colorIslandsRed(BufferedImage image, List<List<Point>> islands) {
        for (List<Point> island : islands) {
            for (Point point: island) {
                image.setRGB(point.x, point.y, Color.GREEN.getRGB());
            }
        }
    }
    public static void drawBoundingBoxes(BufferedImage inputImg, ArrayList<String> str,List<List<Point>> islands) {
        System.out.println("island size is "+islands.size());
        Graphics2D g2d = inputImg.createGraphics();
        g2d.setColor(Color.RED); // Set the bounding box color to red, you can change it to your desired color

        int i = 0;
        for (List<Point> island : islands) {
            String s = str.get(i);
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Point point : island) {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            }

            g2d.drawRect(minX, minY, maxX - minX, maxY - minY);
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial",Font.ITALIC,15));
            g2d.drawString(s, (int)(minX + 3), (int)(maxY - 5));
            i = i + 1;
        }

        g2d.dispose(); // Release resources
    }

private static Rectangle calculateIslandBounds(List<Point> island) {
    // 计算小岛的包围矩形边界框
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (Point point : island) {
        int x = point.x;
        int y = point.y;

        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
    }

    return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
}

    //method to create sub image corresponding to each island in the input image
    private static BufferedImage extractIslandImage(BufferedImage image, Rectangle islandBounds) {
        // 提取小岛在图像中的部分
        return image.getSubimage(islandBounds.x, islandBounds.y, islandBounds.width, islandBounds.height);
    }
    public static double[] createAndNormalizeYUV_UHistogram(BufferedImage image) {
        int[] histogram = new int[256];  // Assuming 8-bit U channel for YUV
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = 0;
        // Calculate histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                if (isGreen(color)){
                    continue;
                }
                // Convert RGB to YUV
                int yuvU = (int) (-0.147 * r - 0.289 * g + 0.436 * b);

                // Ensure values are within the range [0, 255]
                yuvU = Math.max(0, Math.min(255, yuvU + 128));

                // Update the histogram
                histogram[yuvU]++;
            }
        }
        // Normalize histogram to [0, 1]
        double[] normalizedHistogram = new double[256];
        for(int j = 0; j < histogram.length; j++){
            totalPixels += histogram[j];
        }
        //int totalPixels = width * height;
        for (int i = 0; i < 256; i++) {
            normalizedHistogram[i] = (double) histogram[i] / totalPixels;
        }

        return normalizedHistogram;
    }
    public static double[] createAndNormalizeYUV_VHistogram(BufferedImage image) {
        int[] histogram = new int[256];  // Assuming 8-bit V channel for YUV
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                if (isGreen(color)){
                    continue;
                }
                // Convert RGB to YUV using the rgbToYUV function
                float[] yuv = rgbToYUV(r, g, b);
                int yuvV = Math.round(yuv[2]); // Use the V component from YUV

                // Ensure values are within the range [0, 255]
                yuvV = Math.max(0, Math.min(255, yuvV));

                // Update the histogram
                histogram[yuvV]++;
            }
        }
        // Normalize the histogram
        double[] normalizedHistogram = new double[256];
        int totalPixels = width * height;
        for(int j = 0; j < histogram.length; j++){
            totalPixels += histogram[j];
        }
        for (int i = 0; i < 256; i++) {
            normalizedHistogram[i] = (double) histogram[i] / totalPixels;
        }

        return normalizedHistogram;
    }
    public static List<double[]> getIslandsUHistogram(BufferedImage image, List<List<Point>> mergedIslands) {
        List<double[]> histograms = new ArrayList<>();

        for (List<Point> island : mergedIslands) {
            // 创建包围小岛的矩形边界框
            Rectangle islandBounds = calculateIslandBounds(island);
            // 提取小岛在图像中的部分
            BufferedImage islandImage = extractIslandImage(image, islandBounds);
            // 创建 YUV_U 直方图并归一化
            double[] histogram = createAndNormalizeYUV_UHistogram(islandImage);
            // 将直方图添加到列表中
            histograms.add(histogram);
        }
        return histograms;
    }
    public static List<double[]> getIslandsVHistogram(BufferedImage image, List<List<Point>> mergedIslands) {
        List<double[]> histograms = new ArrayList<>();

        for (List<Point> island : mergedIslands) {
            // 创建包围小岛的矩形边界框
            Rectangle islandBounds = calculateIslandBounds(island);

            // 提取小岛在图像中的部分
            BufferedImage islandImage = extractIslandImage(image, islandBounds);

            // 创建 YUV_U 直方图并归一化
            double[] histogram = createAndNormalizeYUV_VHistogram(islandImage);

            // 将直方图添加到列表中
            histograms.add(histogram);
        }
        return histograms;
    }
    public static double bhattacharyyaDistance(double[] histogram1, double[] histogram2) {
        if (histogram1.length != histogram2.length) {
            throw new IllegalArgumentException("Histograms must have the same length");
        }
        double distance = 0.0;

        for (int i = 0; i < histogram1.length; i++) {
            distance += Math.sqrt(histogram1[i] * histogram2[i]);
        }
        return -Math.log(distance);
    }
    public static List<List<Point>> findClosestBox(List<List<Point>> boxes, BufferedImage objImg) {
        double minAverageDistance = Double.MAX_VALUE;
        List<List<Point>> closestBox = new ArrayList<>();
        double[] objUHistogram = createAndNormalizeYUV_UHistogram(objImg);
        double[] objVHistogram = createAndNormalizeYUV_VHistogram(objImg);
        ArrayList<Integer> d = new ArrayList<>();
        for (int i = 0; i < boxes.size();i++) {
            // 计算小岛的U和V直方图
            BufferedImage islandImage = extractIslandImage(inputImg, calculateIslandBounds(boxes.get(i)));
            double[] uHistogram = createAndNormalizeYUV_UHistogram(islandImage);
            double[] vHistogram = createAndNormalizeYUV_VHistogram(islandImage);

            // 计算巴氏距离
            double uDistance = bhattacharyyaDistance(uHistogram, objUHistogram);
            double vDistance = bhattacharyyaDistance(vHistogram, objVHistogram);
            // 计算平均巴氏距离
            double averageDistance = (uDistance + vDistance) / 2;
            averageDistance = Math.round(averageDistance * 100.0) / 100.0;
            int index = 0;
            System.out.println(averageDistance);
            if (averageDistance < minAverageDistance) {
                minAverageDistance = averageDistance;
            }
        }

        for (int i = 0; i < boxes.size();i++) {
            // 计算小岛的U和V直方图
            BufferedImage islandImage = extractIslandImage(inputImg, calculateIslandBounds(boxes.get(i)));
            double[] uHistogram = createAndNormalizeYUV_UHistogram(islandImage);
            double[] vHistogram = createAndNormalizeYUV_VHistogram(islandImage);

            // 计算巴氏距离
            double uDistance = bhattacharyyaDistance(uHistogram, objUHistogram);
            double vDistance = bhattacharyyaDistance(vHistogram, objVHistogram);

            // 计算平均巴氏距离
            double averageDistance = (uDistance + vDistance) / 2;
            averageDistance = Math.round(averageDistance * 100.0) / 100.0;
            int index = 0;
            System.out.println("min"+minAverageDistance);
            System.out.println(Math.abs(averageDistance - minAverageDistance));
            if (Math.abs(averageDistance - minAverageDistance) <= 0.061) {

                minAverageDistance = averageDistance;
                d.add(i);
                System.out.println("zaizheli");
            }
        }

        for(int i : d){
            closestBox.add(boxes.get(i));
        }
        //System.out.println(closestBox);
        return closestBox;
    }
    public void showIms(String[] args) {

        // 显示带有边界框的输入图像
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);
        lbIm1 = new JLabel(new ImageIcon(inputImg));
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.anchor = GridBagConstraints.CENTER;
        c1.weightx = 0.5;
        c1.gridx = 0;
        c1.gridy = 0;
        frame.getContentPane().add(lbIm1, c1);
        frame.pack();
        frame.setVisible(true);
    }
    public static void main(String[] args) {
        List<List<Point>> res = new ArrayList<>();
        ArrayList<String> resStr = new ArrayList<>();
        for(int i = 1; i <args.length;i++){
            System.out.println(args[i]);
            // 加载输入图像和对象图像
            inputImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            readImageRGB(width, height, args[0], inputImg);
            objImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            readImageRGB(width, height, args[i], objImg);
            // 创建二进制矩阵
            int[][] binaryMatrix = createBinaryMatrix(inputImg, objImg);
            List<List<Point>> myislands = findAllIslands(binaryMatrix);
            List<List<Point>> mergedIslands = mergeAndFilterIslands(myislands);
            //a list of YUV_U normalized histogram for each island
            List<List<Point>> closestBox = findClosestBox(mergedIslands, inputImg);
            res.addAll(closestBox);
            String objPath = args[i];
            String objName = objPath.substring(objPath.lastIndexOf("\\")+1);
            for(int x = 0; x <res.size();x++){
                resStr.add(objName);
            }
        }
        drawBoundingBoxes(inputImg, resStr, res);
        Display display = new Display();
        display.showIms(args);
    }
//end of class
}

