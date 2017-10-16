package omri.opencvdemo;


import org.opencv.core.Point;

public class Pixel {

    private Point coordinate;
    private int redChannel;
    private int greenChannel;
    private int blueChannel;

public Pixel(Point p)
{
    this.coordinate = new Point(p.x,p.y);
}

  
    public int getRedChannel() {
        return redChannel;
    }

    public int getGreenChannel() {
        return greenChannel;
    }

    public int getBlueChannel() {
        return blueChannel;
    }

    public void setX(int x) {
        this.coordinate.x = x;
    }

    public void setY(int y) {
        this.coordinate.y = y;
    }

    public void setRedChannel(int redChannel) {
        this.redChannel = redChannel;
    }

    public void setGreenChannel(int greenChannel) {
        this.greenChannel = greenChannel;
    }

    public void setBlueChannel(int blueChannel) {
        this.blueChannel = blueChannel;
    }
}
