package omri.opencvdemo;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;

import java.awt.*;

import org.opencv.core.Point;

/**
 * This class used for color calculations
 */
public  class PixelCalc {

    public static double avgSurround(Point p, int[] pRBG, Bitmap bitmap)
    {
        int seedPixel = bitmap.getPixel((int) p.x, (int) p.y);
    }

    private static int[][] fetchNeighborsRGB(Point seed,Bitmap bitmap,int[] pRGB, int[][] allRGB)
    {
        allRGB[5] = pRGB;

        int pixelNorth,pixelSouth,southPixel,eastPixel,LUPixel, RUPixel,LDPixel,RDPixel;



    }

    private static boolean checkBoundaries(Point seed, Bitmap bitmap)
    {

        return true;
    }
}
