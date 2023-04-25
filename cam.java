import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.flir.thermalsdk.image.*;
import com.flir.thermalsdk.image.fusion.Fusion;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.remote.OnReceived;
import com.flir.thermalsdk.live.remote.OnRemoteError;
import com.flir.thermalsdk.live.streaming.Stream;
import com.flir.thermalsdk.live.streaming.ThermalImageStreamListener;
import com.flir.thermalsdk.live.streaming.ThermalStreamer;
import org.jetbrains.annotations.NotNull;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.javasdk.ThermalSdkJava;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.discovery.CameraScanner;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog.LogLevel;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;

public class cam implements DiscoveryEventListener, CameraScanner, ConnectionStatusListener, Renderer {

    java.io.File file=new File("file.txt"); //for camera authentication files, may not be necessary

    public void mySdkInit() { //must call this before other sdk functions in main()
        ThermalSdkJava.init(file, LogLevel.INFO);
        // from here it is safe to call any other SDK APIs
    }
    //DiscoveryEventListener methods
    @Override
    public void onCameraFound(Identity identity) {
        System.out.println("camera found");
    }

    @Override
    public void onDiscoveryError(CommunicationInterface commint, ErrorCode err) {
        // TODO Auto-generated method stub
    }

    //camerascanner methods
    @Override
    public void close() {
        // TODO Auto-generated method stub
        //throw new Exception();
    }

    @Override
    public void poll() {
        // TODO Auto-generated method stub

    }

    @Override
    public void scan(@NotNull DiscoveryEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    //ConnectionStatusListener methods
    @Override
    public void onDisconnected(@Nullable ErrorCode errorCode) {
        hashCode();
    }

    //Renderer methods
    @Override
    public void update() {

    }

    @Override
    public ImageBuffer getImage() {
        return null;
    }

    public static void main(String []args) throws IOException {
        cam cam1=new cam(); //create a new instance of this class. not a Camera method.
        cam1.mySdkInit(); //must call this first

        //https://stackoverflow.com/questions/10087800/convert-a-java-net-inetaddress-to-a-long#10087976
        String ip = "138.67.10.139"; //string must be manipulated into an InetAddress
        InetAddress addr = InetAddress.getByName(ip); //throws ioexception
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);
        buffer.put(new byte[] { 0,0,0,0 });
        buffer.put(addr.getAddress());
        buffer.position(0);

        Camera camera=new Camera();
        ConnectParameters conn=new ConnectParameters(1000); //create new connectparameters obj for camera.connect()
        ConnectionStatusListener list=new ConnectionStatusListener() { //for camera.connect()
            @Override
            public void onDisconnected(@Nullable ErrorCode errorCode) {
                System.out.println(hashCode()); //dummy code
            }
        };
        camera.connect(addr,list,conn); //ip addr, listener object (null not allowed), connectParameters object
        List<Stream> liststreams=camera.getStreams();
        OnReceived rec= o -> {
            //anonymous fcn for onreceived
        };
        OnRemoteError rem=new OnRemoteError() {
            @Override
            public void onRemoteError(ErrorCode errorCode) {
                //default fcn for onremoteerror
            }
        };

        //begin image streaming code
        liststreams.get(0).start(rec,rem); //args: onreceived, onremoteerror
        camera.getImporter(); //is not null
        ThermalStreamer thst=new ThermalStreamer(liststreams.get(0)); //for handling thermal images
        ThermalImageStreamListener imglist= new ThermalImageStreamListener() {
            @Override
            public void onImageReceived() {
            }
        };

        //begin image collecting
        int imgcount=0; //increment names of each image
        while(camera.isConnected()){ //begin collecting images
            thst.setAutoScale(true);
            thst.update(); //renders by polling the image stream. must call before getimage to prevent intermittent null errors
            ImageBuffer buff=thst.getImage(); //note: imagebuffer is a flir class

            //Fusion fus=new Fusion(); //for separating image's thermal data from visual data
            //JavaImageBuffer.Builder hl= ; //trying  out javaimagebuffer to get cleaner image builder functionality
            //hl.build();
            //construct the buffered image. buff gives a BGR image, which means each pixel has 3 bytes of Blue, Green, Red
            BufferedImage newbuff=new BufferedImage(buff.getWidth(),buff.getHeight(),BufferedImage.TYPE_3BYTE_BGR); //setup for manually constructing the image
            //begin saving multiple images https://www.codeproject.com/Questions/1233353/Write-to-multiple-files-with-java
            int count1=0, count2=1, count3=2; //iterate through all three bytes for each bgr
            for (int x = 0; x< buff.getHeight(); x++) { //when x~=480, we have one full image
                for(int y = 0; y< buff.getWidth(); y++) {
                    int rgb = buff.getPixelBuffer()[count1] + buff.getPixelBuffer()[count2] + buff.getPixelBuffer()[count3]; //construct pixels from buffer
                    newbuff.setRGB(y,x,rgb);
                    count1+=3;
                    count2+=3;
                    count3+=3; //move on to next set of values/next pixel
                }
            }
            try { //ImageIO.write must be wrapped in try-catch
                ImageIO.write(newbuff, "jpg", new File("imgs/img" + imgcount + ".jpg")); //save the thermal images
                imgcount++;
                if(imgcount==100) break; //todo: dummy breakout condition
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } //end while
        thst.close(); //close thermal stream
        camera.disconnect(); //camera connect done
        System.exit(0); //program end
    }
}