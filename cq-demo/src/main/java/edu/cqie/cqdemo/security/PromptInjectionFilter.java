package edu.cqie.cqdemo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PromptInjectionFilter {

    private static final List<String> INJECTION_PATTERNS = Arrays.asList(
            "ignore previous",
            "ignore above",
            "ignore all",
            "disregard previous",
            "disregard above",
            "forget previous",
            "forget above",
            "new instructions",
            "new prompt",
            "system prompt",
            "you are now",
            "act as",
            "pretend to be",
            "ignore your instructions",
            "bypass",
            "override",
            "忽略之前的",
            "忽略以上",
            "忽略所有",
            "无视之前的",
            "忘记之前的",
            "新指令",
            "系统提示",
            "你现在是",
            "假装是",
            "绕过",
            "覆盖"
    );

    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)\\bsystem\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)\\bassistant\\s*:\\s*"),
            Pattern.compile("(?i)\\buser\\s*:\\s*"),
            Pattern.compile("(?i)\\brole\\s*:\\s*system"),
            Pattern.compile("(?i)\\bact\\s+as\\s+a\\s+"),
            Pattern.compile("(?i)\\bpretend\\s+you\\s+are"),
            Pattern.compile("(?i)\\bignore\\s+(all|previous|above)"),
            Pattern.compile("(?i)\\bdisregard\\s+(all|previous|above)"),
            Pattern.compile("(?i)\\bforget\\s+(all|previous|above)"),
            Pattern.compile("(?i)\\boverride\\s+(instructions|rules)"),
            Pattern.compile("(?i)\\bbypass\\s+(safety|filters|rules)"),
            Pattern.compile("(?i)\\bjailbreak"),
            Pattern.compile("(?i)\\bdan\\s+mode"),
            Pattern.compile("(?i)\\bdo\\s+anything\\s+now"),
            Pattern.compile("(?i)\\bdeveloper\\s+mode")
    );

    private static final int MAX_INPUT_LENGTH = 5000;

    public ValidationResult validate(String input) {
        if (input == null || input.isEmpty()) {
            return ValidationResult.valid();
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("输入内容超过最大长度限制: {} > {}", input.length(), MAX_INPUT_LENGTH);
            return ValidationResult.invalid("输入内容过长，请控制在" + MAX_INPUT_LENGTH + "字符以内");
        }

        String lowerInput = input.toLowerCase();

        for (String pattern : INJECTION_PATTERNS) {
            if (lowerInput.contains(pattern.toLowerCase())) {
                log.warn("检测到潜在的提示注入攻击: {}", pattern);
                return ValidationResult.invalid("输入内容包含不允许的指令，请重新输入");
            }
        }

        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到危险的注入模式: {}", pattern.pattern());
                return ValidationResult.invalid("输入内容包含不允许的指令，请重新输入");
            }
        }

        return ValidationResult.valid();
    }

    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input
                .replaceAll("(?i)<script[^>]*>.*?</script>", "")
                .replaceAll("(?i)<[^>]+>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)on\\w+\\s*=", "");

        sanitized = sanitized
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\t", "\t")
                .replaceAll("\\s+", " ")
                .trim();

        return sanitized;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
