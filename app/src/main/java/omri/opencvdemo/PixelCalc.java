package omri.opencvdemo;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Vector;

import org.opencv.core.Point;

/**
 * This class used for color calculations
 */
public class PixelCalc {

    public static double avgSurround(Point p, int[] pRBG, Bitmap bitmap) {
        int seedPixel = bitmap.getPixel((int) p.x, (int) p.y);
    }

    private static void fetchNeighborsRGB(Point seed, Bitmap bitmap, int[][] allRGB) {
        if (!this.checkbounderies(seed, bitmap))
            return;

        ArrayList<Point> points = new ArrayList<>();
        points.add(new Point(seed.x, seed.y - 1));//north
        points.add(new Point(seed.x, seed.y + 1));//south
        points.add(new Point(seed.x + 1, seed.y));//east
        points.add(new Point(seed.x - 1, seed.y));//west
        points.add(new Point(seed.x - 1, seed.y - 1));//LU
        points.add(new Point(seed.x + 1, seed.y - 1));//RU
        points.add(new Point(seed.x - 1, seed.y + 1));//LD
        points.add(new Point(seed.x + 1, seed.y + 1));//RD
        points.add(seed);

        for (int i = 0; i < points.size(); i++) {

            int pixel = bitmap.getPixel((int)points.get(i).x,(int)points.get(i).y);
            allRGB[0][i] = Color.red(pixel);
            allRGB[1][i] = Color.green(pixel);
            allRGB[2][i] = Color.blue(pixel);
        }


    }


}
