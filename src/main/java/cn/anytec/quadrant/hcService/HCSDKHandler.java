package cn.anytec.quadrant.hcService;

import cn.anytec.config.GeneralConfig;
import cn.anytec.config.SpringBootListener;
import cn.anytec.hcsdk.HCNetSDK;
import cn.anytec.quadrant.hcEntity.DeviceInfo;
import com.sun.jna.NativeLong;
import org.bson.BsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HCSDKHandler {

    private static final Logger logger = LoggerFactory.getLogger(HCSDKHandler.class);

    private static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
    private static String waterSildeVistorId;
    private static List<String> bumperCarVisitorIdList = new ArrayList<>();
    private static List<String> bumperCarVideoPathList = new ArrayList<>();
    private static List<String> toyCarVisitorIdList = new ArrayList<>();
    private static List<String> toyCarVideoPathList = new ArrayList<>();
    private static List<String> arAreaVisitorIdList = new ArrayList<>();
    private static List<String> arAreaVideoPathList = new ArrayList<>();
    private volatile boolean flag = true;

    public Map<String, DeviceInfo> deviceInfoMap = new HashMap<>();
    private Map<String, NativeLong> device_lReadPlayHandle = new HashMap<>();

    @Autowired
    GeneralConfig config;
    @Autowired
    SpringBootListener listener;

    //添加游客Id
    public String addVisitorId(String visitorId, String place) {
        if (place.equals(config.getWaterSlide())) {
            HCSDKHandler.waterSildeVistorId = visitorId;
        } else if (place.equals(config.getBumperCar())) {
            bumperCarVisitorIdList.add(visitorId);
        } else if (place.equals(config.getToyCar())) {
            toyCarVisitorIdList.add(visitorId);
        } else if (place.equals(config.getArArea())) {
            arAreaVisitorIdList.add(visitorId);
        } else {
            return "errorPlace";
        }
        return "Success";
    }

    //移除游客Id
    public String removeVisitorId(String visitorId, String place) {
        if (place.equals(config.getBumperCar())) {
            if (bumperCarVisitorIdList.size() > 0) {
                bumperCarVisitorIdList.remove(visitorId);
            }
        } else if (place.equals(config.getToyCar())) {
            if (toyCarVisitorIdList.size() > 0) {
                toyCarVisitorIdList.remove(visitorId);
            }
        } else if (place.equals(config.getArArea())) {
            if (arAreaVisitorIdList.size() > 0) {
                arAreaVisitorIdList.remove(visitorId);
            }
        } else {
            return "errorPlace";
        }
        return "Success";
    }

    //清空游客Ids
    public String clearVisitorIds(String place) {
        if (place.equals(config.getWaterSlide())) {
            HCSDKHandler.waterSildeVistorId = "";
        } else if (place.equals(config.getBumperCar())) {
            bumperCarVisitorIdList.clear();
        } else if (place.equals(config.getToyCar())) {
            toyCarVisitorIdList.clear();
        } else if (place.equals(config.getArArea())) {
            arAreaVisitorIdList.clear();
        } else {
            return "errorPlace";
        }
        return "Success";
    }

    //返回游客IdList
    public List<String> getVisitorIdList(String place) {
        if (place.equals(config.getBumperCar())) {
            return bumperCarVisitorIdList;
        } else if (place.equals(config.getToyCar())) {
            return toyCarVisitorIdList;
        } else if (place.equals(config.getArArea())) {
            return arAreaVisitorIdList;
        } else {
            return null;
        }
    }

    //添加视频路径
    public String addAVideoPath(String path,String place) {
        if (place.equals(config.getBumperCar())) {
            bumperCarVideoPathList.add(path);
        } else if (place.equals(config.getToyCar())) {
            toyCarVideoPathList.add(path);
        } else if (place.equals(config.getArArea())) {
            arAreaVideoPathList.add(path);
        } else {
            return "errorPlace";
        }
        return "Success";
    }

    //返回体验区视频路径List
    public List<String> getVideoPathList(String place) {
       if (place.equals(config.getBumperCar())) {
            return bumperCarVideoPathList;
        } else if (place.equals(config.getToyCar())) {
            return toyCarVideoPathList;
        } else if (place.equals(config.getArArea())) {
            return arAreaVideoPathList;
        } else {
            List<String> resultList = new ArrayList<>();
            resultList.add("placeError");
            return resultList;
        }
    }

    //清空体验区视频路径List
    public void clearVideoPathList(String place) {
        if (place.equals(config.getBumperCar())) {
            bumperCarVideoPathList.clear();
        } else if (place.equals(config.getToyCar())) {
            toyCarVideoPathList.clear();
        } else if (place.equals(config.getArArea())) {
            arAreaVideoPathList.clear();
        }
    }

    /**
     * 视频远近景录制控制
     */
    public void notifyRecord() {
        try {
            DeviceInfo closeView = new DeviceInfo(config.getCloseCameraIp(), config.getCloseCameraUsername(), config.getCloseCameraPassword(), config.getCloseCameraPort(), 0);
            DeviceInfo farView = new DeviceInfo(config.getFarCameraIp(), config.getFarCameraUsername(), config.getFarCameraPassword(), config.getFarCameraPort(), 1);
            if (!loginCamera(farView)) {
                logger.error("远景摄像头注册失败");
                return;
            }
            if (!loginCamera(closeView)) {
                logger.error("近景摄像头注册失败");
                return;
            }
            if (!preView(farView)) {
                logger.error("远景摄像头录制失败");
                return;
            }
            Thread.sleep(config.getDelay());
            logger.info("开启近景摄像头");
            if (!preView(closeView)) {
                logger.error("近景摄像头录制失败");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @PostConstruct
    private void init() {
        if (hCNetSDK.NET_DVR_Init()) {
            logger.info("=====初始化HCNETSDK成功=====");
        } else {
            logger.error("Error：初始化HCNETSDK失败");
        }
        hCNetSDK.NET_DVR_SetConnectTime(2000, 1);
        hCNetSDK.NET_DVR_SetReconnect(10000, true);
        hCNetSDK.NET_DVR_SetLogToFile(true, null, false);
        MyExceptionCallBack myExceptionCallBack = new MyExceptionCallBack();
        hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, 0, myExceptionCallBack, null);
    }

    public boolean loginCamera(DeviceInfo deviceInfo) {
        if (deviceInfoMap.containsKey(deviceInfo.getDeviceIp())) {
            logger.warn(deviceInfo.getDeviceIp() + " 该设备IP已注册");
            return true;
        }
        HCNetSDK.NET_DVR_USER_LOGIN_INFO struLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();
        HCNetSDK.NET_DVR_DEVICEINFO_V40 struDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();
        for (int i = 0; i < deviceInfo.getDeviceIp().length(); i++) {
            struLoginInfo.sDeviceAddress[i] = (byte) deviceInfo.getDeviceIp().charAt(i);
        }
        for (int i = 0; i < deviceInfo.getUserName().length(); i++) {
            struLoginInfo.sUserName[i] = (byte) deviceInfo.getUserName().charAt(i);
        }
        for (int i = 0; i < deviceInfo.getPassword().length(); i++) {
            struLoginInfo.sPassword[i] = (byte) deviceInfo.getPassword().charAt(i);
        }
        struLoginInfo.wPort = deviceInfo.getPort();
        struLoginInfo.bUseAsynLogin = 0;
        struLoginInfo.write();
        NativeLong userId = hCNetSDK.NET_DVR_Login_V40(struLoginInfo.getPointer(), struDeviceInfo.getPointer());
        if (userId.longValue() >= 0 && hCNetSDK.NET_DVR_GetLastError() == 0) {
            logger.info(deviceInfo.getDeviceIp() + " 注册设备成功");
            deviceInfo.setDeviceId(userId);
            deviceInfoMap.put(deviceInfo.getDeviceIp(), deviceInfo);
            return true;
        } else {
            logger.error(deviceInfo.getDeviceIp() + " 注册失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        }
    }

    public boolean loginout(DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId() == null) {
            logger.warn(deviceInfo.getDeviceIp() + " 该设备尚未注册");
            return false;
        }
        if (!hCNetSDK.NET_DVR_Logout(deviceInfo.getDeviceId())) {
            logger.error(deviceInfo.getDeviceIp() + " 注销设备失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        }
        if (deviceInfoMap.remove(deviceInfo.getDeviceIp(), deviceInfo)) {
            logger.info(deviceInfo.getDeviceIp() + " 注销设备成功");
            return true;
        }
        return false;
    }

    //水上滑梯视频预览
    public boolean preView(DeviceInfo deviceInfo) {
        if (device_lReadPlayHandle.containsKey(deviceInfo.getDeviceIp())) {
            logger.warn("该设备已开启预览");
            return false;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long duration;
                StringBuilder path = new StringBuilder(config.getVideoContext());
                path.append(waterSildeVistorId);
                String fileName;
                if (deviceInfo.getStatus() == 0) {
                    duration = config.getDuration0();
                    fileName = "closeView.tmp";
                } else {
                    duration = config.getDuration1();
                    fileName = "farView.tmp";
                }
                SaveVideo_mp4 saveVideo_mp4 = new SaveVideo_mp4(path.toString(), fileName);
                HCNetSDK.NET_DVR_PREVIEWINFO strPreviewInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
                strPreviewInfo.lChannel = new NativeLong(1);//预览通道号
                strPreviewInfo.hPlayWnd = null;//需要SDK解码时句柄设为有效值，仅取流不解码时可设为空
                strPreviewInfo.dwStreamType = 1;//0-主码流，1-子码流，2-码流3，3-码流4，以此类推
                strPreviewInfo.dwLinkMode = 0;//0- TCP方式，1- UDP方式，2- 多播方式，3- RTP方式，4-RTP/RTSP，5-RSTP/HTTP
                NativeLong lRealPlayHandle = hCNetSDK.NET_DVR_RealPlay_V40(deviceInfo.getDeviceId(), strPreviewInfo, saveVideo_mp4, null);
                if (lRealPlayHandle.longValue() >= 0 && hCNetSDK.NET_DVR_GetLastError() == 0) {
                    device_lReadPlayHandle.put(deviceInfo.getDeviceIp(), lRealPlayHandle);
                } else {
                    logger.error("预览失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
                }
                try {
                    Thread.sleep(duration);
                    if (!device_lReadPlayHandle.containsKey(deviceInfo.getDeviceIp())) {
                        logger.warn("关闭失败，设备尚未开启预览或已手动关闭");
                        return;
                    }
                    if (hCNetSDK.NET_DVR_StopRealPlay(device_lReadPlayHandle.get(deviceInfo.getDeviceIp()))) {
                        device_lReadPlayHandle.remove(deviceInfo.getDeviceIp());
                        logger.info("预览结束");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    loginout(deviceInfo);
                    saveVideo_mp4.close();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return true;
    }
//    public boolean stopPreView(DeviceInfo deviceInfo){
//        if(!device_lReadPlayHandle.containsKey(deviceInfo.getDeviceIp())){
//            logger.warn("设备尚未开启预览");
//            return false;
//        }
//        if(hCNetSDK.NET_DVR_StopRealPlay(device_lReadPlayHandle.get(deviceInfo.getDeviceIp()))){
//            device_lReadPlayHandle.remove(deviceInfo.getDeviceIp());
//            logger.info("手动关闭预览成功");
//            return true;
//        }
//        return false;
//    }

    //开启视频录制
    public boolean saveAreaCamera(String place) {
        flag = true;

        List<String> areaCameraIpList = config.getCameraIpList(place);
        for (String ipStr : areaCameraIpList) {
            DeviceInfo deviceInfo = new DeviceInfo(ipStr, "admin", "n-tech123@", (short) 8000, 0);
            //连接摄像机，开启录制
            if (!loginCamera(deviceInfo)) {
                logger.error(place+" 摄像机 " + deviceInfo.getDeviceIp() + " 摄像头注册失败");
                return false;
            }
            if (!areaCameraPreview(deviceInfo,place)) {
                logger.error(place+" 摄像机 " + deviceInfo.getDeviceIp() + " 摄像头录制失败");
                return false;
            }
        }
        return true;
    }

    //体验区视频预览
    public boolean areaCameraPreview(DeviceInfo deviceInfo,String place) {
        if (device_lReadPlayHandle.containsKey(deviceInfo.getDeviceIp())) {
            logger.warn(deviceInfo.getDeviceIp() + " 该设备已开启预览");
            return false;
        }
        String savePath = config.getAreaVideoPath() + deviceInfo.getDeviceIp();
        Thread t = new Thread(new Runnable() {
            Integer videoTime = 0;

            @Override
            public void run() {
                String videoName = System.currentTimeMillis() + "areaVideo.tmp";
                SaveAreaVideo_mp4 saveAreaVideo_mp4 = new SaveAreaVideo_mp4(savePath, videoName);
                videoName = videoName.replace(".tmp", ".mp4");
                StringBuilder url = new StringBuilder("http://")
                        .append(listener.getHostIP()).append(":").append(listener.getPort())
                        .append("/anytec/areaVideos/").append(deviceInfo.getDeviceIp())
                        .append(File.separator).append(videoName);
                addAVideoPath(url.toString(),place);
                HCNetSDK.NET_DVR_PREVIEWINFO strPreviewInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
                strPreviewInfo.hPlayWnd = null;//需要SDK解码时句柄设为有效值，仅取流不解码时可设为空
                NativeLong nativeLong = new NativeLong((long) 1);
                strPreviewInfo.lChannel = nativeLong;
                strPreviewInfo.dwStreamType = 1;//0-主码流，1-子码流，2-码流3，3-码流4，以此类推
                strPreviewInfo.dwLinkMode = 0;//0- TCP方式，1- UDP方式，2- 多播方式，3- RTP方式，4-RTP/RTSP，5-RSTP/HTTP
                NativeLong lRealPlayHandle = hCNetSDK.NET_DVR_RealPlay_V40(deviceInfo.getDeviceId(), strPreviewInfo, saveAreaVideo_mp4, null);
                if (lRealPlayHandle.longValue() >= 0 && hCNetSDK.NET_DVR_GetLastError() == 0) {
                    logger.info("体验区摄像机 " + deviceInfo.getDeviceIp() + " 录制视频线程开启");
                    device_lReadPlayHandle.put(deviceInfo.getDeviceIp(), lRealPlayHandle);
                } else {
                    logger.error("体验区摄像机 " + deviceInfo.getDeviceIp() + " 录制视频失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
                }
                //体验区视频最大录制时长
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (flag) {
                                if (!videoTime.equals(config.getAreaVideoMaxTime())) {
                                    Thread.sleep(1000);
                                    videoTime++;
                                } else {
                                    flag = false;
                                }

                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.setDaemon(true);
                t.start();
                while (flag) {

                }
                if (!device_lReadPlayHandle.containsKey(deviceInfo.getDeviceIp())) {
                    logger.warn(deviceInfo.getDeviceIp() + " 关闭失败，设备尚未开启预览或已手动关闭");
                    return;
                }
                if (hCNetSDK.NET_DVR_StopRealPlay(device_lReadPlayHandle.get(deviceInfo.getDeviceIp()))) {
                    device_lReadPlayHandle.remove(deviceInfo.getDeviceIp());
                    logger.info("摄像机 " + deviceInfo.getDeviceIp() + " 录制视频线程关闭");
                }
                loginout(deviceInfo);
                saveAreaVideo_mp4.close();
            }
        });
        t.setDaemon(true);
        t.start();
        return true;
    }

    //停止体验区视频录制
    public boolean stopAreaCamera() {
        if (device_lReadPlayHandle.size() == 0) {
            logger.warn("关闭失败，设备尚未开启预览或已手动关闭");
            return false;
        }
        this.flag = false;
        return true;
    }


   /* public boolean getDeviceBility(NativeLong userId){
        String string1 = new String();
        String string2 = new String();
        boolean s = hCNetSDK.NET_DVR_GetDeviceAbility(userId,15,string1,0,string2,5000);
        System.out.println("===:"+s);
        System.out.println(hCNetSDK.NET_DVR_GetLastError());
     //logger.info(string2);
        return false;
    }*/

    @PreDestroy
    public void cleanUp() {
        hCNetSDK.NET_DVR_Cleanup();
    }
}
