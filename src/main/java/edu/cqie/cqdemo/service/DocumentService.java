package edu.cqie.cqdemo.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DocumentService {

    private static final String CONTENT_DIR = "src/main/resources/content";

    @Autowired
    private EmbeddingStore<TextSegment> redisEmbeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    public void processAllDocuments() {
        Path contentPath = Paths.get(CONTENT_DIR);
        File[] files = contentPath.toFile().listFiles((dir, name) -> name.endsWith(".pdf"));

        if (files == null || files.length == 0) {
            log.warn("未找到PDF文件");
            return;
        }

        log.info("发现 {} 个PDF文档待处理", files.length);

        for (File file : files) {
            try {
                log.info("正在处理文档: {}", file.getName());
                
                String text = extractTextFromPDF(file);
                
                if (text == null || text.trim().isEmpty()) {
                    log.warn("文档 {} 没有提取到文本", file.getName());
                    continue;
                }

                List<TextSegment> segments = splitTextIntoSegments(text, file.getName());
                
                log.info("文档 {} 分割为 {} 个片段", file.getName(), segments.size());

                for (TextSegment segment : segments) {
                    redisEmbeddingStore.add(embeddingModel.embed(segment).content(), segment);
                }
                
                log.info("文档 {} 处理完成", file.getName());
            } catch (Exception e) {
                log.error("处理文档 {} 时出错", file.getName(), e);
            }
        }

        log.info("所有文档处理完成，知识库构建成功");
    }

    private String extractTextFromPDF(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            log.error("提取PDF文本时出错", e);
            return null;
        }
    }

    private List<TextSegment> splitTextIntoSegments(String text, String filename) {
        List<TextSegment> segments = new ArrayList<>();
        int chunkSize = 500;
        int overlap = 100;
        
        String[] words = text.split("\\s+");
        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;
        
        for (String word : words) {
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(word);
            wordCount++;
            
            if (wordCount >= chunkSize / 5) {
                Metadata metadata = Metadata.from("filename", filename);
                segments.add(TextSegment.from(currentChunk.toString(), metadata));
                
                int keepWords = overlap / 5;
                if (keepWords > 0 && wordCount > keepWords) {
                    String[] chunkWords = currentChunk.toString().split("\\s+");
                    currentChunk = new StringBuilder();
                    for (int i = chunkWords.length - keepWords; i < chunkWords.length; i++) {
                        if (currentChunk.length() > 0) {
                            currentChunk.append(" ");
                        }
                        currentChunk.append(chunkWords[i]);
                    }
                    wordCount = keepWords;
                } else {
                    currentChunk = new StringBuilder();
                    wordCount = 0;
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            Metadata metadata = Metadata.from("filename", filename);
            segments.add(TextSegment.from(currentChunk.toString(), metadata));
        }
        
        return segments;
    }
}
