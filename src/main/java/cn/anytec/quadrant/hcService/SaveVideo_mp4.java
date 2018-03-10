package cn.anytec.quadrant.hcService;

import cn.anytec.hcsdk.HCNetSDK;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

//保存水上滑梯视频回调函数
public class SaveVideo_mp4 implements HCNetSDK.FRealDataCallBack_V30 {

    private static final Logger logger = LoggerFactory.getLogger(SaveVideo_mp4.class);

    private FileOutputStream fileOutputStream;
    private File context;
    private File videoTmp;

    public SaveVideo_mp4(String path,String fileName){
        context = new File(path);
        if(!context.exists()){
            context.mkdirs();
        }else if(context.list().length > 1 || context.list().length!=0 && !context.list()[0].equals("farView.tmp")){
            for(File oldFile:context.listFiles()){
                oldFile.delete();
            }
        }
        videoTmp = new File(context,fileName);
        try {
            fileOutputStream = new FileOutputStream(videoTmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void invoke(NativeLong lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {
        switch (dwDataType){
            case HCNetSDK.NET_DVR_SYSHEAD :
                break;
            case HCNetSDK.NET_DVR_STREAMDATA :
                break;
            case HCNetSDK.NET_DVR_AUDIOSTREAMDATA :
                return;
            default:
                return;
        }
        Pointer pointer = pBuffer.getPointer();
        byte[] data = pointer.getByteArray(0,dwBufSize);
        try {
            fileOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            if(fileOutputStream != null)
                fileOutputStream.close();
            if(!videoTmp.renameTo(new File(context,videoTmp.getName().replace("tmp","temp")))){
                logger.error("视频临时文件写入完毕标识失败");
                videoTmp.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
