package com.whut.hadoop.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.tools.javac.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

/**
 * 
 * @author niuyang
 *
 */
public class ClientAccessTest {

	private static Executor executor = Executors.newFixedThreadPool(4);

	public void write() throws Exception {
		String localFileUrl = "/home/niuyang/Documents/1";
		String remoteFileUrl = "/test1";
		FileInputStream in = null;
        OutputStream out = null;
        Configuration conf = new Configuration();
        try {
            // 获取读入文件数据
            in = new FileInputStream(new File(localFileUrl));
            // 获取目标文件信息
            FileSystem fs = FileSystem.get(conf);
            out = fs.create(new Path(remoteFileUrl));
            byte[] buffer = new byte[20];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            IOUtils.closeStream(in);
            IOUtils.closeStream(out);
        }       
	}
	
	public void read() throws Exception {
		String remoteFileUrl = "/test1";
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(remoteFileUrl);
        InputStream in = null;
		try {
            in = fs.open(path);
            IOUtils.copyBytes(in, System.out, 1024, false);
        } finally {
            IOUtils.closeStream(in);
        }
    }
	
	public static void main(String[] args) throws Exception {

		int i = 10;
		final long beginTime = System.currentTimeMillis();
		final Vector<Long> v = new Vector<>();
		final ClientAccessTest cat = new ClientAccessTest();
		while (i-- > 0) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						cat.read();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("time = " + (System.currentTimeMillis() - beginTime));
				}
			});
		}	
	}

}
