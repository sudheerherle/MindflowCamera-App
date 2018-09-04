package com.felhr.serialportexamplesync;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private Button BtnFetchImage;
    private ImageView im;
    private int index = 0;
    private byte[] rawData = new byte[752*480];
    String SD_CARD_PATH = Environment.getExternalStorageDirectory().toString();

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this);
        BtnFetchImage =  findViewById(R.id.button2);
        im = (ImageView) findViewById(R.id.imageView2);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        BtnFetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                index = 0;
            }
        });
        showImage();
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void showImage(){
        File sdcard = Environment.getExternalStorageDirectory();
        File f = new File(sdcard,"balu.raw");
        int size = (int) f.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.rawData = bytes;
        this.rawToJpeg(this.rawData,752,480);
    }

    public boolean rawToJpeg(byte[] rawBytes, int width, int height){
        int i=0;
        int rawpt = 0;
        int b=0;
        byte [] Bits = new byte[rawBytes.length*4];
        boolean retval = false;
        try{
            for (i = 0; i < width*height; i++ ) {
                if ( (i/width) % 2 == 0 ) {
                    if ( (i % 2) == 0 ) {
                        // B
                        if ( (i > width) && ((i % width) > 0) ) {
                            Bits[b++] = (byte) ((rawBytes[(rawpt-width-1)]+rawBytes[(rawpt-width+1)]+
                                    rawBytes[(rawpt+width-1)]+rawBytes[(rawpt+width+1)])/4);	// R
                            Bits[b++] = (byte) (((rawBytes[rawpt-1]+rawBytes[rawpt+1]+
                                    rawBytes[rawpt+width]+rawBytes[rawpt-width]))/4);	// G
                            Bits[b++]  = rawBytes[rawpt];					// B
                            Bits[b++] = -1;
                        } else {
                            // first line or left column
                            Bits[b++]  = rawBytes[rawpt+width+1];		// R
                            Bits[b++]  = (byte) ((rawBytes[rawpt+1]+rawBytes[rawpt+width])/2);//	G
                            Bits[b++]  = rawBytes[rawpt];				//B
                            Bits[b++] = -1;
                        }
                    } else {
                        // (B)G
                        if ( (i > width) && ((i % width) < (width-1)) ) {
                            Bits[b++]  = (byte) ((rawBytes[rawpt+width]+rawBytes[rawpt-width])/2);	// R
                            Bits[b++]  = rawBytes[rawpt];					//G
                            Bits[b++]  = (byte) ((rawBytes[rawpt-1]+rawBytes[rawpt+1])/2);		// B
                            Bits[b++] = -1;
                        } else {
                            // first line or right column
                            Bits[b++] = rawBytes[rawpt+width];	// R
                            Bits[b++] = rawBytes[rawpt];		// G
                            Bits[b++] = rawBytes[rawpt-1];	// B
                            Bits[b++] = -1;
                        }
                    }
                } else
                {
                    if ( (i % 2) == 0 )
                    {
                        // G(R)
                        if ( (i < (width*(height-1))) && ((i % width) > 0) )
                        {
                            Bits[b++] = (byte) ((rawBytes[rawpt-1]+rawBytes[rawpt+1])/2);//		 R
                            Bits[b++] = rawBytes[rawpt];					// G
                            Bits[b++] = (byte) ((rawBytes[rawpt+width]+rawBytes[rawpt-width])/2);	 //B
                            Bits[b++] = -1;
                        } else {
                            // bottom line or left column
                            Bits[b++] = rawBytes[rawpt+1];		 //R
                            Bits[b++] = rawBytes[rawpt];			 //G
                            Bits[b++] = rawBytes[rawpt-width];		 //B
                            Bits[b++] = -1;
                        }
                    } else {
                        //R
                        if ( i < (width*(height-1)) && ((i % width) < (width-1)) )
                        {
                            Bits[b++] = rawBytes[rawpt];					// R
                            Bits[b++] = (byte) ((rawBytes[rawpt-1]+rawBytes[rawpt+1]+
                                    rawBytes[rawpt-width]+rawBytes[rawpt+width])/4);	// G
                            Bits[b++] = (byte) ((rawBytes[rawpt-width-1]+rawBytes[rawpt-width+1]+
                                    rawBytes[rawpt+width-1]+rawBytes[rawpt+width+1])/4);	// B
                            Bits[b++] = -1;
                        } else
                        {
                            // bottom line or right column
                            Bits[b++]= rawBytes[rawpt];			//	 R
                            Bits[b++] = (byte) ((rawBytes[rawpt-1]+rawBytes[rawpt-width])/2);//	 G
                            Bits[b++] = rawBytes[rawpt-width-1];	//	B
                            Bits[b++] = -1;
                        }
                    }
                }
                rawpt++;
            }
            Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bm.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
            bm.prepareToDraw();
            im.setImageBitmap(bm);
            retval = true;
        }catch(Exception ex){
            ex.printStackTrace();
            retval = false;
        }
        return retval;

    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().display.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;
                    mActivity.get().display.append(buffer);
                    break;
            }
        }
    }
}
