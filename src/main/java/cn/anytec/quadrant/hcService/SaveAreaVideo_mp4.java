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

//保存体验区视频回调函数
public class SaveAreaVideo_mp4 implements HCNetSDK.FRealDataCallBack_V30 {

    private static final Logger logger = LoggerFactory.getLogger(SaveAreaVideo_mp4.class);

    private FileOutputStream fileOutputStream;
    private File context;
    private File videoTmp;

    public SaveAreaVideo_mp4(String path, String fileName){
        context = new File(path);
        if(!context.exists()){
            context.mkdirs();
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //删除文件夹下所有文件及当前文件夹
    private boolean clearDir(File dir){
        if(!dir.exists()){
            logger.warn("要删除的文件夹不存在");
            return true;
        }
        if (dir.isDirectory()) {
            String[] childrens = dir.list();
            if(childrens.length == 0){
                logger.warn("要删除的文件夹为空");
                return true;
            }
            for (String children:childrens) {
                File file = new File(dir,children);
                if(!clearDir(file)){
                    return false;
                }
            }
        }
        if(dir.delete())
            return true;
        return false;
    }
}
