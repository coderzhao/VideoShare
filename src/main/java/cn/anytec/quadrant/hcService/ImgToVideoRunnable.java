package cn.anytec.quadrant.hcService;

import cn.anytec.config.GeneralConfig;
import cn.anytec.config.SpringBootListener;
import cn.anytec.mongo.MongoDB;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.util.RuntimeLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@Component
public class ImgToVideoRunnable implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(ImgToVideoRunnable.class);
    private static final String videoDir = "generateVideo";


    @Autowired
    GeneralConfig config;
    @Autowired
    MongoDBService mongoDB;
    @Autowired
    SpringBootListener listener;

    /**
     * 目录及文件创建，视频处理
     */
    @Override
    public void run() {
        String root = config.getVideoContext();
        File file = new File(root);
        if(!file.exists()){
            if(!file.mkdirs()){
                logger.error("存储临时视频文件的文件夹不存在，创建文件夹失败");
                return;
            }
        }
        boolean hasGenerateVideoDir = false;
        for(String dirName:file.list()){
            if(dirName.equals(videoDir)){
                hasGenerateVideoDir = true;
                break;
            }
        }
        if(!hasGenerateVideoDir){
            if(!new File(file,videoDir).mkdir()){
                logger.error("创建视频文件夹失败");
                return;
            }
        }
        while (true){
            try {
                File[] visitors = file.listFiles();
                if(visitors == null || visitors.length < 2){
                    Thread.sleep(2000);
                    continue;
                }
                d1:for (File visitor : visitors){
                    if(visitor.getName().equals(videoDir))
                        continue;
                    if(!visitor.isDirectory())
                        continue;
                    String[] tmpVideosName = visitor.list();
                    if(tmpVideosName == null||tmpVideosName.length != 2)
                        continue;
                    for(String tmpVideoName:tmpVideosName){
                        if(!tmpVideoName.contains("temp"))
                            continue d1;
                    }
                    //开始处理视频
                    logger.info("========== 视频处理 ==========");
                    //第一步：将临时的temp文件编码为MP4视频文件并删除临时文件
                    RuntimeLocal runtimeLocal = new RuntimeLocal();
                    for(File tmpVideo : visitor.listFiles()){
                        String tmpToMp4;
                        if(tmpVideo.getName().equals("closeView.temp")){
                            tmpToMp4 = "close.mp4";
                        }else {
                            tmpToMp4 = "far.mp4";
                        }
                        StringBuilder makeVideo = new StringBuilder("ffmpeg");
                        makeVideo.append(" -i ").append(visitor.getAbsolutePath())
                                .append(File.separator).append(tmpVideo.getName())
                                .append(" -strict -2 -y ")
                                .append(visitor.getAbsolutePath()).append(File.separator)
                                .append(tmpToMp4);
                        logger.info("开始生成"+tmpToMp4);
                        logger.debug(runtimeLocal.execute(makeVideo.toString()));
                    }
                    Predicate<String> isExist = (n) -> n.endsWith(".mp4");
                    if(!Arrays.stream(visitor.list()).anyMatch(isExist)){
                        logger.error("生成视频失败！检查是否安装了ffmpeg并重启应用");
                        return;
                    }
                    new File(visitor,"closeView.temp").delete();
                    new File(visitor,"farView.temp").delete();
                    //第二步：远景摄像头视频剪切
                    //预处理
                    StringBuilder cutReady = new StringBuilder("ffmpeg");
                    cutReady.append(" -i ").append(visitor.getAbsolutePath())
                            .append(File.separator).append("far.mp4")
                            .append(" -strict -2  -qscale 0 -intra ")
                            .append(visitor.getAbsolutePath()).append(File.separator)
                            .append("cut.mp4");
                    logger.info("视频剪切准备");
                    logger.debug(runtimeLocal.execute(cutReady.toString()));
                    //剪切第一段
                    StringBuilder cutVideo1 = new StringBuilder("ffmpeg");
                    cutVideo1.append(" -ss ").append(config.getStart1()).append(" -t ").append(config.getVideo1Duration())
                            .append(" -i ").append(visitor.getAbsolutePath())
                            .append(File.separator).append("cut.mp4")
                            .append(" -vcodec copy -acodec copy ")
                            .append(visitor.getAbsolutePath()).append(File.separator)
                            .append("far1.mp4");
                    logger.info("开始剪切第一段远景摄像头视频");
                    logger.debug(runtimeLocal.execute(cutVideo1.toString()));
                    //剪切第二段
                    StringBuilder cutVideo2 = new StringBuilder("ffmpeg");
                    cutVideo2.append(" -ss ").append(config.getStart2()).append(" -t ").append(config.getVideo2Duration())
                            .append(" -i ").append(visitor.getAbsolutePath())
                            .append(File.separator).append("cut.mp4")
                            .append(" -vcodec copy -acodec copy ")
                            .append(visitor.getAbsolutePath()).append(File.separator)
                            .append("far2.mp4");
                    logger.info("开始剪切第二段远景摄像头视频");
                    logger.debug(runtimeLocal.execute(cutVideo2.toString()));
                    //第三步：放慢处理近景视频
                    StringBuilder slowVideo = new StringBuilder("ffmpeg");
                    slowVideo.append(" -r ").append(config.getFps()).append(" -i ")
                            .append(visitor.getAbsolutePath())
                            .append(File.separator).append("close.mp4")
                            .append(" -strict -2 -y ")
                            .append(visitor.getAbsolutePath()).append(File.separator)
                            .append("slow.mp4");
                    logger.info("视频降帧处理");
                    logger.debug(runtimeLocal.execute(slowVideo.toString()));
                    //第四步：创建视频合并列表
                    //合并列表
                    StringBuilder concatText1 = new StringBuilder()
                            .append("file '").append(visitor.getAbsolutePath()).append(File.separator).append("far1.mp4").append("'\n")
                            .append("file '").append(visitor.getAbsolutePath()).append(File.separator).append("slow.mp4").append("'\n");
                    StringBuilder concatText2 = new StringBuilder()
                            .append("file '").append(visitor.getAbsolutePath()).append(File.separator).append("merge12.mp4").append("'\n")
                            .append("file '").append(visitor.getAbsolutePath()).append(File.separator).append("far2.mp4").append("'\n");
                    OutputStream outputStream1 = null;
                    OutputStream outputStream2 = null;
                    File concatFile1 = new File(visitor,"concat1.txt");
                    File concatFile2 = new File(visitor,"concat2.txt");
                    try {
                        outputStream1 = new FileOutputStream(concatFile1);
                        outputStream2 = new FileOutputStream(concatFile2);
                        outputStream1.write(concatText1.toString().getBytes());
                        outputStream2.write(concatText2.toString().getBytes());
                        outputStream1.flush();
                        outputStream2.flush();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            if(outputStream1!=null) {
                                outputStream1.close();
                            }
                            if(outputStream2!=null) {
                                outputStream2.close();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    //第五步：合并一二段视频
                    StringBuilder concat1 = new StringBuilder("ffmpeg");
                    concat1.append(" -y -f concat -safe 0 -i ").append(visitor.getAbsolutePath())
                            .append(File.separator).append("concat1.txt")
                            .append(" -c copy -y ").append(visitor.getAbsolutePath())
                            .append(File.separator).append("merge12.mp4");
                    logger.info("合并第一二段视频");
                    logger.debug(runtimeLocal.execute(concat1.toString()));
                    //第六步：合并二三段视频
                    StringBuilder concat = new StringBuilder("ffmpeg");
                    StringBuilder location = new StringBuilder(file.getAbsolutePath())
                            .append(File.separator).append(videoDir)
                            .append(File.separator).append(visitor.getName());
                    File dir = new File(location.toString());
                    if(!dir.exists())
                        dir.mkdirs();
                    String videoName = System.currentTimeMillis()+".mp4";
                    concat.append(" -y -f concat -safe 0 -i ").append(visitor.getAbsolutePath())
                            .append(File.separator).append("concat2.txt")
                            .append(" -c copy -y ").append(location.toString())
                            .append(File.separator).append(videoName);
                    logger.info("合并第三段视频为完整视频");
                    logger.debug(runtimeLocal.execute(concat.toString()));
                    concatFile1.delete();
                    concatFile2.delete();
                    StringBuilder url = new StringBuilder("http://")
                            .append(listener.getHostIP()).append(":").append(listener.getPort())
                            .append("/anytec/videos/").append(visitor.getName())
                            .append(File.separator).append(videoName);
                    List<String> urlList = new ArrayList<>();
                    urlList.add(url.toString());
                    mongoDB.saveVideoUrlList(visitor.getName(),config.getWaterSlide(),urlList);
                    logger.info("=========== 完成 ===========");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }catch (Exception e){
                logger.error("视频处理时出现异常");
                e.printStackTrace();
                return;
            }

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
