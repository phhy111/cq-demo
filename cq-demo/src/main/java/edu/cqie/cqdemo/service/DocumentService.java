package edu.cqie.cqdemo.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DocumentService {

    @Value("${rag.document.content-dir:src/main/resources/content}")
    private String contentDir;

    @Value("${rag.document.chunk-size:1000}")
    private int chunkSize;

    @Value("${rag.document.chunk-overlap:200}")
    private int chunkOverlap;

    @Autowired
    private EmbeddingStore<TextSegment> redisEmbeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    public void processAllDocuments() {
        Path contentPath = Paths.get(contentDir);
        
        if (!Files.exists(contentPath)) {
            log.warn("内容目录不存在: {}", contentPath.toAbsolutePath());
            return;
        }

        File[] files = contentPath.toFile().listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".pdf") || lowerName.endsWith(".txt") || lowerName.endsWith(".md");
        });

        if (files == null || files.length == 0) {
            log.warn("未找到可处理的文档文件（支持PDF/TXT/MD格式）");
            return;
        }

        log.info("发现 {} 个文档待处理", files.length);
        AtomicInteger totalSegments = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        for (File file : files) {
            try {
                int segments = processDocument(file);
                totalSegments.addAndGet(segments);
                successCount.incrementAndGet();
                log.info("文档 {} 处理完成，生成 {} 个片段", file.getName(), segments);
            } catch (Exception e) {
                log.error("处理文档 {} 时出错: {}", file.getName(), e.getMessage(), e);
            }
        }

        log.info("所有文档处理完成，成功处理 {} 个文档，共生成 {} 个文本片段，知识库构建成功", 
                successCount.get(), totalSegments.get());
    }

    public int processDocument(File file) {
        log.info("开始处理文档: {}", file.getName());
        
        String text = extractTextFromDocument(file);
        
        if (text == null || text.trim().isEmpty()) {
            log.warn("文档 {} 没有提取到文本", file.getName());
            return 0;
        }

        log.info("文档 {} 原始文本长度: {} 字符", file.getName(), text.length());

        List<TextSegment> segments = splitTextIntoSegments(text, file.getName());
        
        log.info("文档 {} 分割为 {} 个片段", file.getName(), segments.size());

        if (!segments.isEmpty()) {
            batchAddToEmbeddingStore(segments);
        }

        return segments.size();
    }

    private String extractTextFromDocument(File file) {
        String fileName = file.getName().toLowerCase();
        
        try {
            if (fileName.endsWith(".pdf")) {
                return extractTextFromPDF(file);
            } else if (fileName.endsWith(".txt")) {
                return Files.readString(file.toPath());
            } else if (fileName.endsWith(".md")) {
                return Files.readString(file.toPath());
            } else {
                log.warn("不支持的文件格式: {}", fileName);
                return null;
            }
        } catch (Exception e) {
            log.error("提取文档文本时出错: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractTextFromPDF(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                log.warn("PDF文档已加密，无法提取文本: {}", file.getName());
                return null;
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            StringBuilder text = new StringBuilder();
            int totalPages = document.getNumberOfPages();
            
            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                text.append(stripper.getText(document));
                if (i < totalPages) {
                    text.append("\n\n---\n\n");
                }
            }
            
            String result = text.toString();
            result = cleanExtractedText(result);
            
            log.debug("PDF文档 {} 提取完成，共 {} 页，文本长度: {}", 
                    file.getName(), totalPages, result.length());
            
            return result;
        } catch (Exception e) {
            log.error("提取PDF文本时出错: {}", e.getMessage(), e);
            return null;
        }
    }

    private String cleanExtractedText(String text) {
        if (text == null) {
            return null;
        }
        
        return text
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\r", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .replaceAll("\\s{2,}", " ")
            .replaceAll("[\\u0000-\\u001F\\u007F]", "")
            .trim();
    }

    private List<TextSegment> splitTextIntoSegments(String text, String filename) {
        Document document = Document.from(text, Metadata.from(
            "filename", filename,
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "source", "file://" + filename
        ));

        var splitter = DocumentSplitters.recursive(
            chunkSize, 
            chunkOverlap,
            Arrays.asList("\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", "，", ","),
            Arrays.asList("\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", "，", ",")
        );

        return splitter.split(document);
    }

    private void batchAddToEmbeddingStore(List<TextSegment> segments) {
        int batchSize = 10;
        
        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(i, end);
            
            try {
                List<dev.langchain4j.data.embedding.Embedding> embeddings = new ArrayList<>();
                for (TextSegment segment : batch) {
                    embeddings.add(embeddingModel.embed(segment));
                }
                
                redisEmbeddingStore.addAll(embeddings, batch);
                log.debug("批量添加 {} 个片段到向量存储", batch.size());
            } catch (Exception e) {
                log.error("批量添加到向量存储失败: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    public int getDocumentCount() {
        Path contentPath = Paths.get(contentDir);
        if (!Files.exists(contentPath)) {
            return 0;
        }
        
        File[] files = contentPath.toFile().listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".pdf") || lowerName.endsWith(".txt") || lowerName.endsWith(".md");
        });
        
        return files != null ? files.length : 0;
    }

    public void clearEmbeddingStore() {
        try {
            redisEmbeddingStore.removeAll();
            log.info("已清空向量存储");
        } catch (Exception e) {
            log.error("清空向量存储失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
