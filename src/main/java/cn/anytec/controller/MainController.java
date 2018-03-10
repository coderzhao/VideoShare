package cn.anytec.controller;

import cn.anytec.config.GeneralConfig;

import cn.anytec.mongo.MongoDB;
import cn.anytec.mongo.MongoDBService;
import cn.anytec.quadrant.hcService.HCSDKHandler;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController{

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    GeneralConfig generalConfig;
    @Autowired
    HCSDKHandler hcsdkHandler;
    @Autowired
    MongoDBService mongoDB;


    //添加游客Id
    @RequestMapping(value = "/anytec/addVisitorId")
    @ResponseBody
    public Map<String,Object> addVisitorId(@RequestParam("visitorId")String visitorId,@RequestParam("place")String place){
        Map<String,Object> resultMap = new HashMap<>();
        String result =hcsdkHandler.addVisitorId(visitorId,place);
        if(result.equals("errorPlace")){
            resultMap.put("result","Fail");
            resultMap.put("msg","传入的地点有误！");
        }
        if(place.equals(generalConfig.getWaterSlide())){
            mongoDB.addVisitorId(visitorId,place);
        }
        resultMap.put("result","Success");
        return resultMap;
    }

    //移除游客Id
    @RequestMapping(value = "/anytec/removeVisitorId")
    @ResponseBody
    public Map<String, Object> removeVisitorId(@RequestParam("visitorId")String visitorId,@RequestParam("place")String place){
        Map<String,Object> resultMap = new HashMap<>();
        if(hcsdkHandler.removeVisitorId(visitorId,place).equals("Success")){
            resultMap.put("result","Success");
        }else {
            resultMap.put("result","Fail");
            resultMap.put("msg","传入的地点有误！");
        }
        return resultMap;
    }

    //清空体验区游客Ids
    @RequestMapping(value = "/anytec/clearVisitorIds")
    @ResponseBody
    public Map<String, Object> clearVisitorIds(@RequestParam("place")String place){
        Map<String,Object> resultMap = new HashMap<>();
        if(hcsdkHandler.clearVisitorIds(place).equals("Success")){
            resultMap.put("result","Success");
        }else {
            resultMap.put("result","Fail");
            resultMap.put("msg","传入的地点有误！");
        }
        return resultMap;
    }

    //根据Id和地点获取视频地址
    @RequestMapping(value = "/anytec/getVideoList")
    @ResponseBody
    public Map<String,Object> getVideoList(@RequestParam("visitorId")String visitorId,@RequestParam("place")String place){
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("place",place);
        List<String> videoUrlList = mongoDB.getVideoUrlList(visitorId,place);
        if(videoUrlList.size()>0){
            resultMap.put("videoUrlList",videoUrlList);
        }else {
            resultMap.put("videoUrlList",null);
        }
        return resultMap;
    }

    //开始录制体验区视频
    @RequestMapping(value = "/anytec/startAreaVideo")
    @ResponseBody
    public Map<String,Object> startAreaVideo(@RequestParam("place")String place){
        Map<String,Object> resultMap = new HashMap<>();
        List<String> visitorIdList =hcsdkHandler.getVisitorIdList(place);
        if(visitorIdList.size()==0){
            resultMap.put("result","fail");
            resultMap.put("msg","没有录入"+place+"游客Id");
            return resultMap;
        }
        if(hcsdkHandler.saveAreaCamera(place)){
            resultMap.put("result","Success");
        }else {
            resultMap.put("result","Fail");
        }
        return resultMap;
    }

    //停止录制体验区视频
    @RequestMapping(value = "/anytec/stopAreaVideo")
    @ResponseBody
    public Map<String,Object> stopAreaVideo(@RequestParam("place")String place){
        Map<String,Object> resultMap = new HashMap<>();
        if(hcsdkHandler.stopAreaCamera()){
            //体验区视频地址入库
            List<String> videoPathList = hcsdkHandler.getVideoPathList(place);
            List<String> visitorIdList =hcsdkHandler.getVisitorIdList(place);
            if(visitorIdList.size()==0){
                resultMap.put("result","fail");
                resultMap.put("msg","没有录入体验区游客Id");
                return resultMap;
            }
            for(String visitorId : visitorIdList){
                mongoDB.saveVideoUrlList(visitorId,place,videoPathList);
            }
            hcsdkHandler.clearVideoPathList(place);
            resultMap.put("result","Success");
        }else {
            resultMap.put("result","Fail");
            resultMap.put("msg","关闭失败，未检测到开启的设备");
        }
        return resultMap;
    }
}
