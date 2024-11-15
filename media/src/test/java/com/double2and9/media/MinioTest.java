package com.double2and9.media;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
public class MinioTest {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Test
    public void testConnection() throws Exception {
        // 测试连接和bucket是否存在
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            // 创建bucket
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        System.out.println("Bucket '" + bucketName + "' " + (found ? "exists" : "created"));
    }

    @Test
    public void testUpload() throws Exception {
        // 创建一个测试文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        // 上传文件
        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(bucketName)
                .object("test.txt")
                .filename(tempFile.getAbsolutePath())
                .build());

        System.out.println("File uploaded successfully");
    }

    @Test
    public void testBucketOperations() throws Exception {
        // 列出所有bucket
        minioClient.listBuckets().forEach(bucket -> 
            System.out.println(bucket.name() + " - " + bucket.creationDate()));
    }
} 