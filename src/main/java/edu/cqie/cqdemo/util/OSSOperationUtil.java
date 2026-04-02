package edu.cqie.cqdemo.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OSS操作工具类
 */
@Component
@Slf4j
public class OSSOperationUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.prefix}")
    private String prefix;

    private OSS ossClient;

    /**
     * 项目启动时创建客户端
     */
    @PostConstruct
    public void init() {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS 客户端初始化完成，bucket: {}", bucketName);
    }

    /**
     * 项目关闭时销毁客户端
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }

    /**
     * 上传文件到 OSS
     * @param file      前端上传的文件
     * @param directory 存储目录（如 "comments_img/"）
     * @return 图片 URL
     */
    public String upload(MultipartFile file, String directory) {
        try {
            // 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + suffix;

            // 构建存储路径
            String objectName = directory + fileName;

            // 上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            ossClient.putObject(putObjectRequest);

            // 拼接 URL
            String url = prefix + objectName;
            log.info("文件上传成功，URL：{}", url);
            return url;
        } catch (Exception e) {
            log.error("文件上传 OSS 失败", e);
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 上传评论图片
     */
    public String upload(MultipartFile file) {
        return upload(file, "comments_img/");
    }

    /**
     * 上传景点封面图片
     */
    public String uploadScenicsCoverImg(MultipartFile file) {
        return upload(file, "scenics_cover_img/");
    }

    /**
     * 上传公告图片
     */
    public String uploadNoticeImg(MultipartFile file) {
        return upload(file, "announcement_img/");
    }

    /**
     * 获取轮播图图片链接
     */
    public List<String> getSlideShowImageUrls() {
        return getImageUrlsByDirectory("scenics_cover_img/");
    }

    /**
     * 获取指定目录下的图片链接
     * @param directory 目录前缀（如 "banner/"）
     * @return 图片 URL 列表
     */
    public List<String> getImageUrlsByDirectory(String directory) {
        List<String> imageUrls = new ArrayList<>();

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                .withPrefix(directory)
                .withMaxKeys(1000);

        ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);

        for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            String objectName = objectSummary.getKey();
            if (isImageFile(objectName)) {
                String imageUrl = "https://" + bucketName + "." + endpoint + "/" + objectName;
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp");
    }
}
