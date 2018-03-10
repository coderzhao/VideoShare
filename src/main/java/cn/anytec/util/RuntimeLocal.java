package cn.anytec.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class RuntimeLocal {
	private static Logger logger  = Logger.getLogger(RuntimeLocal.class);
	private static final Runtime runtime = Runtime.getRuntime();
	private Process process = null;
	public static void main(String[] a) throws IOException {
		RuntimeLocal runtimeLocal = new RuntimeLocal();
		String ffmpeg = "ffmpeg -framerate 15 -pattern_type glob -i /home/anytec-z/Pictures/test/罗工/1515493078334/*.jpeg /home/anytec-z/Pictures/test/罗工/generateVideo/out.mp4";
		logger.info(ffmpeg);
		String s = runtimeLocal.execute(ffmpeg);
		logger.info(s);
	}

	public String execute(String cmd) {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader br = null;
		  try {
			  process = runtime.exec(cmd);
			  String tmp ;
			  br = new BufferedReader(new InputStreamReader(
					  process.getInputStream(), "utf-8"));
			  while ((tmp = br.readLine()) != null) {
				  stringBuilder.append(tmp).append("\n");
			  }
			  br = new BufferedReader(new InputStreamReader(
					  process.getErrorStream(), "utf-8"));
			  while ((tmp = br.readLine()) != null) {
				  stringBuilder.append(tmp).append("\n");
			  }

		  } catch (IOException e) {
		  
		  }finally {
		  	if(br!=null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  return stringBuilder.toString();
	}
	public void closeProcess(){
		while(process.isAlive()){
			process.destroy();
		}
	}
	public boolean isAlive(){
		return process.isAlive();
	}

}
