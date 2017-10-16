package omri.opencvdemo;

import android.graphics.Bitmap;
import android.graphics.Color;

import android.util.Log;


import java.util.ArrayList;

import org.opencv.core.Point;

/**
 * This class used for color calculations
 */
public class PixelCalc {

public static double[]avgSurround(Point p,Bitmap bitmap)
        {
        int[][]allRGB=new int[3][9];// The RGB values of the hole nighbrhood.
        fetchNeighborsRGB(p,bitmap,allRGB);// The function recive the arrays and fill them.
        double[]avg=new double[3];// array for the results. Red=0,Green=1,Blue=2.
        for(int i=0;i<allRGB.length;i++){
        avg[0]+=allRGB[0][i];//Sum the Reds.
        avg[1]+=allRGB[1][i];//Sum the Greens.
        avg[2]+=allRGB[2][i];//Sum the Blues
        }
        avg[0]=avg[0]/allRGB[0].length;
        avg[1]=avg[1]/allRGB[1].length;
        avg[2]=avg[2]/allRGB[2].length;
        return avg;

        }

private static void fetchNeighborsRGB(Point seed,Bitmap bitmap,int[][]allRGB){
        if(!checkBoundaries(seed,bitmap)) {
            return;
        }
        ArrayList<Point> points=new ArrayList<>();
        points.add(new Point(seed.x,seed.y-1));//north
        points.add(new Point(seed.x,seed.y+1));//south
        points.add(new Point(seed.x+1,seed.y));//east
        points.add(new Point(seed.x-1,seed.y));//west
        points.add(new Point(seed.x-1,seed.y-1));//LU
        points.add(new Point(seed.x+1,seed.y-1));//RU
        points.add(new Point(seed.x-1,seed.y+1));//LD
        points.add(new Point(seed.x+1,seed.y+1));//RD
        points.add(seed);

        for(int i=0;i<points.size();i++){

        int pixel=bitmap.getPixel((int)points.get(i).x,(int)points.get(i).y);
        allRGB[0][i]= Color.red(pixel);
        allRGB[1][i]=Color.green(pixel);
        allRGB[2][i]=Color.blue(pixel);
        }
        }


public static boolean checkBoundaries(Point seed,Bitmap bitmap)
        {
        try{
        int test=bitmap.getPixel((int)seed.x,(int)seed.y);//Test me
        test=bitmap.getPixel((int)seed.x-1,(int)seed.y);//Left to seed
        test=bitmap.getPixel((int)seed.x+1,(int)seed.y);//Right to seed
        test=bitmap.getPixel((int)seed.x,(int)seed.y-1);//Above seed
        test=bitmap.getPixel((int)seed.x,(int)seed.y+1);//Below seed
        test=bitmap.getPixel((int)seed.x-1,(int)seed.y-1);//Up left
        test=bitmap.getPixel((int)seed.x+1,(int)seed.y+1);//Down right
        test=bitmap.getPixel((int)seed.x-1,(int)seed.y+1);//Down left
        test=bitmap.getPixel((int)seed.x+1,(int)seed.y-1);//Up right
        test=0;
        }
        catch(Exception e){
        Log.e("","checkBoundaries error");
        return false;
        }
        return true;

        }
        }
