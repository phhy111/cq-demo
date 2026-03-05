package edu.cqie.cqdemo.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    // 从配置文件读取 OSS 信息
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

    private  final OSS ossClient;

    // 构造函数注入OSS客户端
    public OSSOperationUtil(OSS ossClient) {
        this.ossClient = ossClient;
    }

    /**
     * 无参数读取Bucket中所有图片链接
     * 自动筛选常见图片格式（jpg/png/jpeg/gif/webp），返回完整可访问的URL
     * @return 图片链接列表
     */
    public List<String> getSlideShowImageUrls() {
        List<String> imageUrls = new ArrayList<>();

        // 构建列出Bucket中所有文件的请求（可指定前缀筛选目录，比如"images/"）
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                .withPrefix("scenics_cover_img/")
                // 分页读取，默认一次最多返回1000个文件
                .withMaxKeys(1000);

        // 获取Bucket中的文件列表
        ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);

        // 遍历文件，筛选图片并拼接完整URL
        for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            String objectName = objectSummary.getKey();
            if (isImageFile(objectName)) {
                //阿里云OSS默认访问地址
                String imageUrl = "https://" + bucketName + "." + endpoint + "/" + objectName;
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
    }

    /**
     * 上传图片到阿里云 OSS
     * @param file 前端上传的文件
     * @param directory 存储目录
     * @return 阿里云返回的图片 URL
     */
    public String upload(MultipartFile file, String directory) {
        try {
            // 1. 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 2. 生成唯一文件名（避免重复）：uuid + 原文件名后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + suffix;

            // 3. 构建 OSS 存储路径
            String objectName = directory + fileName;

            // 4. 创建 OSS 客户端并上传文件
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            ossClient.putObject(putObjectRequest);

            // 5. 关闭客户端
            ossClient.shutdown();

            // 6. 返回完整的图片 URL（和你现有路径格式一致）
            String url = prefix + objectName;
            log.info("图片上传成功，URL：{}", url);
            return url;
        } catch (Exception e) {
            log.error("图片上传阿里云失败", e);
            throw new RuntimeException("图片上传失败");
        }
    }

    /**
     * 上传评论图片到阿里云 OSS（默认目录）
     * @param file 前端上传的文件
     * @return 阿里云返回的图片 URL
     */
    public String upload(MultipartFile file) {
        return upload(file, "comments_img/");
    }

    /**
     * 上传景点封面图片到阿里云 OSS（默认目录）
     * @param file
     * @return
     */
    public String uploadScenicsCoverImg(MultipartFile file) {
        return upload(file, "scenics_cover_img/");
    }

    /**
     * 上传公告图片到阿里云 OSS
     * @param file
     */
    public String uploadNoticeImg(MultipartFile file) {
        return upload(file, "announcement_img/");
    }

    /**
     * 辅助方法：判断文件是否为图片
     * @param fileName 文件名/对象名
     * @return 是否为图片
     */
    private boolean isImageFile(String fileName) {
        // 转为小写，避免大小写问题
        String lowerFileName = fileName.toLowerCase();
        // 常见图片格式后缀
        return lowerFileName.endsWith(".jpg")
                || lowerFileName.endsWith(".jpeg")
                || lowerFileName.endsWith(".png")
                || lowerFileName.endsWith(".gif")
                || lowerFileName.endsWith(".webp");
    }

    /**
     * 读取指定目录下的所有图片链接（如需按目录筛选，保留该方法）
     * @param directory 图片目录（如"banner/"），不传则读取全部
     * @return 图片链接列表
     */
    public List<String> getImageUrlsByDirectory(String directory) {
        List<String> imageUrls = new ArrayList<>();

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                .withPrefix(directory) // 指定目录前缀
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
     * 销毁OSS客户端
     */
    @Override
    protected void finalize() throws Throwable {
        if (ossClient != null) {
            ossClient.shutdown();
        }
        super.finalize();
    }
}