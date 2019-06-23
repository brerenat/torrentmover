package com.home.torrentmover.routes.file;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.jupiter.api.Test;

class FileMoverProcessorTest {

	@Test
	void test() {
		final FTPClient ftp = new FTPClient();
		final FTPClientConfig config = new FTPClientConfig();
		config.setDefaultDateFormatStr("dd-MM-yyyy hh:mm:ss");

		ftp.configure(config);
		
		File file = new File("C:\\Users\\natha\\Downloads\\test\\chernobyl.s01e01.internal.1080p.web.h264-memento.mkv");
		
		try (final FileInputStream fis = new FileInputStream(file)){
		
			ftp.connect("192.168.1.32");
			
			int replyCode = ftp.getReplyCode();
			System.out.println("Reply :" + replyCode);
			
			boolean check = FTPReply.isPositiveCompletion(replyCode);
			
			if (check) {
			
				ftp.login("admin", "granbarr");
				System.out.println("pwd :" + ftp.printWorkingDirectory());
				ftp.setListHiddenFiles(true);
				
				for (FTPFile ftpFile : ftp.listFiles()) {
					System.out.println(ftpFile.getName());
				}
				
				System.out.println("Moved Up :" + ftp.changeToParentDirectory());
				
				for (FTPFile ftpFile : ftp.listFiles()) {
					System.out.println(ftpFile.getName());
				}
				
				ftp.storeFile("/PorQstuffs/test/chernobyl.s01e01.internal.1080p.web.h264-memento.mkv", fis);
			} else {
				System.out.println("Bad Times");
			}
			
			
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
