package edu.cqie.cqdemo.util;


import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import edu.cqie.cqdemo.common.Result; // 你的Result类

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.error("上传失败：图片大小不能超过10MB，请压缩后重试");
    }
}