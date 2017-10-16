package omri.opencvdemo;


public class Pixel {

    private int x;
    private int y;
    private int redChannel;
    private int greenChannel;
    private int blueChannel;

    public Pixel(int x,int y)
    {
        this.x = x;
        this.y=y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
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
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
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
