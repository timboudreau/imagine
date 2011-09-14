/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.fx;

import com.jhlabs.image.PointFilter;

/**
 * A BufferedImageOp which removes all information from each pixel which is
 * not color information, by taking the lowest value of r/g/b and subtracting
 * it from each.
 *
 * @author Tim Boudreau
 */
public class RemoveGrayFilter extends PointFilter {

    static int minimize(int val, int other1, int other2, int[] removed) {
        int min = Math.min(val, Math.min(other1, other2));
        removed[0] = min;
        return val - min;
    }

    public int filterRGB(int x, int y, int rgb) {
        int[] removed = new int[1];
        
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        int a = rgb & 0xff000000;

        int rm = 0;
        int ir = minimize(r, g, b, removed);
        rm += removed[0];
        int ig = minimize(g, r, b, removed);
        rm += removed[0];
        int ib = minimize(b, r, g, removed);
        rm += removed[0];
//        a = 255 - (rm / 3);
        return a | (ir << 16) | (ig << 8) | ib;
    }
    
    public String toString() {
        return "Extract Color Information";
    }
    
    /*
    private int[][] lut;
    
    protected int[] filterPixels(int width, int height, int[] inPixels, Rectangle transformedSpace) {
    Histogram histogram = new Histogram(inPixels, width, height, 0, width);
    
    int i, j;
    
    if (histogram.getNumSamples() > 0) {
    float scale = 255.0f / histogram.getNumSamples();
    lut = new int[3][256];
    for (i = 0; i < 3; i++) {
    lut[i][0] = histogram.getFrequency(i, 0);
    for (j = 1; j < 256; j++) {
    lut[i][j] = lut[i][j - 1] + histogram.getFrequency(i, j);
    }
    for (j = 0; j < 256; j++) {
    lut[i][j] = (int) Math.round(lut[i][j] * scale);
    }
    }
    } else {
    lut = null;
    }
    //        int[] mins = new int[4];
    //        Arrays.fill(mins, 255);
    //        int[] maxes = new int[4];
    //        Arrays.fill(maxes, 0);
    //        for (int ii = 0; ii < inPixels.length; ii++) {
    //            minMax(inPixels[ii], mins, maxes);
    //        }
    //        double[] factors = scales(mins, maxes);
    //        System.out.println("Factors are " + db(factors));
    //        System.out.println("Mins are " + ib(mins));
    //        System.out.println("Maxes are " + ib(maxes));
    for (int ii = 0, y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
    inPixels[ii] = filter (inPixels[ii]);
    ii++;
    }
    }
    lut = null;
    
    return inPixels;
    }
    
    double[] scales(int[] mins, int[] maxes) {
    double[] result = new double[4];
    for (int i = 0; i < 4; i++) {
    double min = mins[i];
    double max = maxes[i];
    double range = max - min;
    if (range == 0) {
    result[i] = 1D;
    } else {
    result[i] = 255D / range;
    }
    }
    return result;
    }
    
    String db(double[] d) {
    StringBuilder sb = new StringBuilder();
    for (double db : d) {
    if (sb.length() > 0) {
    sb.append(',');
    }
    sb.append(db);
    }
    return sb.toString();
    }
    
    String ib(int[] d) {
    StringBuilder sb = new StringBuilder();
    for (double db : d) {
    if (sb.length() > 0) {
    sb.append(',');
    }
    sb.append(db);
    }
    return sb.toString();
    }
    
    private static void minMax(int val, int offset, int[] mins, int[] maxes) {
    mins[offset] = Math.min(mins[offset], val);
    maxes[offset] = Math.max(maxes[offset], val);
    }
    
    private void minMax(int rgb, int[] mins, int[] maxes) {
    if (lut != null) {
    int a = rgb & 0xff000000;
    int r = lut[Histogram.RED][(rgb >> 16) & 0xff];
    int g = lut[Histogram.GREEN][(rgb >> 8) & 0xff];
    int b = lut[Histogram.BLUE][rgb & 0xff];
    
    minMax(r, 0, mins, maxes);
    minMax(g, 1, mins, maxes);
    minMax(b, 2, mins, maxes);
    minMax(a, 3, mins, maxes);
    }
    }
    
    private int f (int val, int other1, int other2) {
    int min = Math.min(val, Math.min(other1, other2));
    return val - min;
    }
    
    private int filter(int rgb) {
    int r = lut[Histogram.RED][(rgb >> 16) & 0xff];
    int g = lut[Histogram.GREEN][(rgb >> 8) & 0xff];
    int b = lut[Histogram.BLUE][rgb & 0xff];
    int a = rgb & 0xff000000;
    
    int ir = f (r, g, b);
    int ig = f (g, r, b);
    int ib = f (b, r, g);
    
    return a | (ir << 16) | (ig << 8) | ib;
    }
    
    private int filterRGB(int rgb, double[] scales) {
    if (lut != null) {
    double r = lut[Histogram.RED][(rgb >> 16) & 0xff];
    double g = lut[Histogram.GREEN][(rgb >> 8) & 0xff];
    double b = lut[Histogram.BLUE][rgb & 0xff];
    double a = rgb & 0xff000000;
    
    r *= scales[0];
    g *= scales[1];
    b *= scales[2];
    a *= scales[3];
    
    int ia = (int) a;
    int ir = (int) r;
    int ig = (int) g;
    int ib = (int) b;
    
    return ia | (ir << 16) | (ig << 8) | ib;
    }
    return rgb;
    }
     * 
     */
}
