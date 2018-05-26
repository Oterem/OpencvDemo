package omri.opencvdemo;

/**
 * Created by oterem on 26/05/2018.
 */

public final class Constants {

    private Constants(){
        throw new AssertionError("can't create constants class");
    }
    public static abstract class Strings{
        public static final String UPLOAD_IMAGE = "Uploading image...";
        public static final String DOWNLOAD_IMAGE = "Download diagnose...";
        public static final String NO_CONNECTION = "No internet connection";
    }
}
