package edu.cqie.cqdemo.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OSSOperationUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

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
            // 筛选常见图片格式
            if (isImageFile(objectName)) {
                //阿里云OSS默认访问地址
                String imageUrl = "https://" + bucketName + "." + endpoint + "/" + objectName;
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
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