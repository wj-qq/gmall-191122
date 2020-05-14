package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import jodd.io.FileNameUtil;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author Administrator
 * @create 2020-05-14 14:51
 */
@RequestMapping("/admin/product")
@RestController
public class FileUploadController {
    @Value("${fileServer.url}")
    private String fileUrl;

    @RequestMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws Exception {

        //加载配置文件 Io流 绝对路径
        String path = ClassUtils.getDefaultClassLoader().getResource("tracker.conf").getPath();
        ClientGlobal.init(path);

        //连接跟踪器
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getConnection();

        //连接存储节点
        StorageClient1 storageClient = new StorageClient1(trackerServer, null);

        //获取扩展名
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        String fileId = storageClient.upload_file1(file.getBytes(), extension, null);
        String imgUrl = fileUrl + fileId;
//        System.out.println(imgUrl);
        return Result.ok(imgUrl);
    }
}
