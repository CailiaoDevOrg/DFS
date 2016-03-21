package com.whut.hadoop.test;

import com.whut.hadoop.loadbalance.LoadBalanceProtocol;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Created by niuyang on 3/16/16.
 */
public class HDFSClient implements LoadBalanceProtocol {


    public static void main(String[] args) throws Exception {
        int i = 10;
        while (i > 0) {
        	new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						start();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
        	}).start();
        	i--;
        }
    }
    
    private static void start() throws Exception {
    	Configuration conf = new Configuration();
    	conf.set("jobId", "job1");
    	FileSystem fs = FileSystem.get(conf);
        Path path = new Path("/test1");
        FSDataInputStream in = fs.open(path);
        byte[] buf = new byte[1024];
        int len = in.read(buf);
        System.out.println(new String(buf, 0, len, "UTF-8"));
        in.close();
        fs.close();
    }


    @Override
    public String doLoadBalance(String jobName) {
        return null;
    }
}
