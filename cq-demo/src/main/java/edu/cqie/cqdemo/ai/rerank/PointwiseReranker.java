package edu.cqie.cqdemo.ai.rerank;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PointwiseReranker {

    @Autowired
    @Qualifier("dashscopeChatModel")
    private ChatLanguageModel chatModel;

    private static final String RERANK_PROMPT_TEMPLATE = 
        "请判断以下文档片段与用户问题的相关性，并给出一个0-100的分数。\n\n" +
        "用户问题：\n{question}\n\n" +
        "文档片段：\n{document}\n\n" +
        "请直接输出一个0到100之间的整数分数，不要输出其他任何内容。\n" +
        "评分标准：\n" +
        "100-80：高度相关，文档包含直接回答问题所需的关键信息\n" +
        "79-60：相关，文档包含部分有用信息\n" +
        "59-40：轻微相关，文档包含一些可能有用的背景信息\n" +
        "39-0：不相关，文档与问题无关";

    @Data
    @AllArgsConstructor
    private static class ScoredDocument {
        private Content content;
        private double score;
        private int originalRank;
    }

    public List<Content> rerank(List<Content> documents, String query) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        log.info("开始Pointwise重排序，原始文档数量: {}", documents.size());
        
        List<ScoredDocument> scoredDocuments = documents.stream()
                .map(doc -> new ScoredDocument(doc, scoreDocument(doc, query), 0))
                .collect(Collectors.toList());

        scoredDocuments.sort(Comparator.comparingDouble(ScoredDocument::getScore).reversed());

        List<Content> reranked = scoredDocuments.stream()
                .map(ScoredDocument::getContent)
                .collect(Collectors.toList());

        log.info("重排序完成，最高分数: {}, 最低分数: {}", 
                scoredDocuments.get(0).getScore(),
                scoredDocuments.get(scoredDocuments.size() - 1).getScore());

        return reranked;
    }

    private double scoreDocument(Content document, String query) {
        String documentText = document.text();
        if (documentText == null || documentText.isEmpty()) {
            return 0.0;
        }

        String prompt = RERANK_PROMPT_TEMPLATE
                .replace("{question}", query)
                .replace("{document}", truncateDocument(documentText, 1000));

        try {
            String response = chatModel.generate(prompt);
            double score = parseScore(response);
            log.debug("文档评分: {}", score);
            return score;
        } catch (Exception e) {
            log.warn("文档评分失败，使用默认分数: {}", e.getMessage());
            return 50.0;
        }
    }

    private String truncateDocument(String document, int maxLength) {
        if (document.length() <= maxLength) {
            return document;
        }
        return document.substring(0, maxLength - 3) + "...";
    }

    private double parseScore(String response) {
        if (response == null || response.isEmpty()) {
            return 50.0;
        }

        try {
            String cleanResponse = response.trim().replaceAll("[^0-9]", "");
            if (cleanResponse.isEmpty()) {
                return 50.0;
            }
            int score = Integer.parseInt(cleanResponse);
            return Math.max(0, Math.min(100, score));
        } catch (NumberFormatException e) {
            log.warn("无法解析评分响应: {}", response);
            return 50.0;
        }
    }
}