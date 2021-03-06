package cn.anytec.config;
/**
 * 服务器启动时自动执行
 */


import cn.anytec.quadrant.hcService.HCSDKHandler;
import cn.anytec.quadrant.hcService.ImgToAreaVideoRunnable;
import cn.anytec.quadrant.hcService.ImgToVideoRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

@Configuration
@Order(value = 1)
public class MyApplicationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);
    private static long timeRecord = 0;

    @Autowired
    GeneralConfig config;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    ImgToVideoRunnable imgToVideoRunnable;
    @Autowired
    ImgToAreaVideoRunnable imgToAreaVideoRunnable;

    private Socket socket;


    @Override
    public void run(ApplicationArguments arg) throws Exception {
        logger.info("====== 启动视频处理线程 =======");
        Thread vthread = new Thread(imgToVideoRunnable);
        vthread.setDaemon(true);
        Thread.sleep(2000);
        vthread.start();

        logger.info("====== 启动体验区视频处理线程 =======");
        Thread areaThread = new Thread(imgToAreaVideoRunnable);
        areaThread.setDaemon(true);
        Thread.sleep(2000);
        areaThread.start();

        logger.info("====== 启动时与IO模块进行Socket连接 =======");
        while (true){
            Thread thread = null;
            try {
                socket = buildSocket();
                if(socket == null||!socket.isConnected())
                    throw new Exception("Socket连接失败");
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InputStream inputStream = null;
                        try{
                            inputStream = socket.getInputStream();
                            if(inputStream == null){
                                logger.error("连接失败！");
                                return;
                            }
                            while (true){
                                byte[] b = new byte[10];
                                inputStream.read(b);
                                String info = bytesToHexString(b);
                                logger.info("IO模块："+info);
                                parseIO(info);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            if(inputStream != null){
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                thread.setDaemon(true);
                thread.start();
                while (true){
                    socket.sendUrgentData(0xFF);
                    Thread.sleep(1000);
                }

            }catch (Exception e){
                logger.error(e.getMessage());
                if(socket != null)
                    socket.close();
                if(thread != null && thread.isAlive())
                    thread.interrupt();
                Thread.sleep(1000);
            }
        }

    }

    /**
     * 字节数组转16进制字符串
     * @param bArray
     * @return
     */
    private static  String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 建立socket客户端连接
     * @return
     */
    private Socket buildSocket(){

        Socket socket = null;
        try {
            socket = new Socket(config.getIo_module_ip(),config.getIo_module_port());
            socket.setKeepAlive(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    /**
     * IO模块信号解析
     * @param info
     */
    private void parseIO(String info){
        String DI = info.substring(16);
        long currentTime = System.currentTimeMillis();
        if(DI.equals("0101") && currentTime-timeRecord > config.getDuration1()+3000){
            timeRecord = currentTime;
            logger.info("开始录制视频");
            hcsdkHandler.notifyRecord();
        }
    }
}
