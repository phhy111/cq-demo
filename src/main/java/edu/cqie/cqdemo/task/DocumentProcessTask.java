package edu.cqie.cqdemo.task;

import edu.cqie.cqdemo.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocumentProcessTask implements CommandLineRunner {

    @Autowired
    private DocumentService documentService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始处理PDF文档，构建知识库...");
        try {
            documentService.processAllDocuments();
            log.info("文档处理完成，知识库构建成功");
        } catch (Exception e) {
            log.error("文档处理失败，知识库构建失败", e);
            // 不影响应用启动，继续运行
        }
    }
}
