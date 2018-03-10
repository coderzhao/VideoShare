package cn.anytec.quadrant.hcService;

import cn.anytec.config.GeneralConfig;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.util.RuntimeLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.function.Predicate;

@Component
public class ImgToAreaVideoRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ImgToVideoRunnable.class);

    @Autowired
    GeneralConfig config;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    MongoDBService mongoDB;

    /**
     * 目录及文件创建，视频处理
     */
    @Override
    public void run() {
        String areaVideoPath = config.getAreaVideoPath();
        File areaFile = new File(areaVideoPath);
        if (!areaFile.exists()) {
            if (!areaFile.mkdirs()) {
                logger.error("存储体验区视频文件的文件夹不存在，创建文件夹失败");
                return;
            }
        }
        while (true) {
            try {
                File[] areaVideoFlies = areaFile.listFiles();
                if (areaVideoFlies == null || areaVideoFlies.length == 0) {
                    Thread.sleep(2000);
                    continue;
                }
                //开始处理视频
                for (File cameraIpFile : areaVideoFlies) {
                    String[] areaVideos = cameraIpFile.list();
                    if(areaVideos.length!=0){
                        for (String areaVideo : areaVideos) {
                            if (!areaVideo.contains("areaVideo.temp")) {
                                continue;
                            }
                            logger.info("========== 视频处理 ==========");
                            RuntimeLocal runtimeLocal = new RuntimeLocal();
                            String areaVideoName = areaVideo.split("\\.")[0] + ".mp4";
                            StringBuilder makeAreaVideo = new StringBuilder("ffmpeg");
                            makeAreaVideo.append(" -i ").append(cameraIpFile.getAbsolutePath())
                                    .append(File.separator).append(areaVideo)
                                    .append(" -strict -2 -y ")
                                    .append(cameraIpFile.getAbsolutePath()).append(File.separator)
                                    .append(areaVideoName);
                            logger.info("开始生成" + areaVideoName);
                            logger.debug(runtimeLocal.execute(makeAreaVideo.toString()));
                            Predicate<String> isExist = (n) -> n.endsWith(".mp4");
                            if(!Arrays.stream(areaVideos).anyMatch(isExist)){
                                logger.error("生成视频失败！检查是否安装了ffmpeg并重启应用");
                                return;
                            }
                            new File(cameraIpFile,areaVideo).delete();

                        }

                    }
                }

            } catch (Exception e) {
                logger.error("视频处理时出现异常");
                e.printStackTrace();
                return;
            }
        }
    }
}