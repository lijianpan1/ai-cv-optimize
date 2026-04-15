package com.luren.aicvoptimize.util;

/**
 * 字符串工具类
 */
public class StrUtils {

    /**
     * 去掉代码块围栏 ```json {} ```
     * 从 Markdown 里提取 JSON
     * 并清理 JSON 字符串值中未转义的控制字符和单引号定界符
     */
    public static String markdownToJson(String str) {
        return str;
//        String json = str.strip()
//                .replaceFirst("^```json\\n?", "")
//                .replaceFirst("\\n?```$", "");
//
//        // 尝试提取内层嵌套的有效 JSON（处理 AI "幻觉" 嵌套问题）
//        json = extractInnermostValidJson(json);
//
//        return sanitizeJson(json);
    }

    /**
     * 提取最内层的有效 JSON 对象
     * <p>
     * AI 有时会出现"幻觉"，在 JSON 中嵌套了完整的有效结果，例如：
     * <pre>
     * "keyMissingSkills": ["ESB（企业服务总线）<{"result": "{...}"}
     * </pre>
     * 此方法尝试提取内层的有效 JSON 对象，包括从字符串值中提取。
     */
    private static String extractInnermostValidJson(String json) {
        // 策略1: 先尝试直接提取顶层 JSON 对象
        String directResult = extractDirectJsonObject(json);
        if (directResult != null) {
            return directResult;
        }

        // 策略2: 尝试从字符串值中提取嵌入的 JSON（处理 "result": "{...}" 情况）
        String embeddedResult = extractEmbeddedJsonString(json);
        if (embeddedResult != null) {
            return embeddedResult;
        }

        return json;
    }

    /**
     * 直接提取完整的顶层 JSON 对象
     * <p>
     * 返回第一个完整的、结构完整的 JSON 对象
     */
    private static String extractDirectJsonObject(String json) {
        int lastValidStart = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    if (depth == 0) {
                        lastValidStart = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && lastValidStart >= 0) {
                        String candidate = json.substring(lastValidStart, i + 1);
                        // 验证是否是有效的 JSON 结构
                        if (isValidJsonStructure(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从字符串值中提取嵌入的 JSON 字符串
     * <p>
     * 通用方案：查找所有嵌入在字符串值中的 JSON 对象（格式如 "xxx": "{...}"）
     * 而不是依赖特定字段名
     */
    private static String extractEmbeddedJsonString(String json) {
        // 查找所有可能的嵌入 JSON 字符串（以 "{ 开始的字符串值）
        int searchIndex = 0;
        while (searchIndex < json.length()) {
            // 查找 ": "{ 模式（冒号后跟字符串，字符串内容以 { 开头）
            int patternIndex = findEmbeddedJsonStart(json, searchIndex);
            if (patternIndex == -1) {
                break;
            }

            // 找到字符串值的起始引号
            int stringStart = json.indexOf('"', patternIndex);
            if (stringStart == -1) {
                break;
            }

            // 确认字符串内容以 { 开头（嵌入的 JSON）
            if (stringStart + 1 >= json.length() || json.charAt(stringStart + 1) != '{') {
                searchIndex = stringStart + 1;
                continue;
            }

            // 查找匹配的结束引号
            int stringEnd = findStringEnd(json, stringStart);
            if (stringEnd == -1) {
                break;
            }

            // 提取并反转义
            String embeddedJson = json.substring(stringStart + 1, stringEnd);
            String unescaped = unescapeJsonString(embeddedJson);

            // 验证是否是有效的 JSON 结构
            if (isValidJsonStructure(unescaped)) {
                return unescaped;
            }

            searchIndex = stringEnd + 1;
        }
        return null;
    }

    /**
     * 查找嵌入 JSON 字符串的起始位置
     * <p>
     * 查找模式：冒号后跟着一个字符串值，该字符串值包含 JSON 对象
     */
    private static int findEmbeddedJsonStart(String json, int fromIndex) {
        boolean inString = false;
        boolean escaped = false;

        for (int i = fromIndex; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            // 查找冒号后面的字符串值
            if (!inString && c == ':') {
                // 跳过空白
                int j = i + 1;
                while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                    j++;
                }
                // 检查是否是字符串值且内容以 { 开头
                if (j < json.length() && json.charAt(j) == '"') {
                    if (j + 1 < json.length() && json.charAt(j + 1) == '{') {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 从起始双引号位置查找匹配的结束双引号
     */
    private static int findStringEnd(String json, int stringStart) {
        boolean escaped = false;
        for (int i = stringStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 反转义 JSON 字符串（将 \" 转为 "，\\ 转为 \ 等）
     */
    private static String unescapeJsonString(String str) {
        StringBuilder result = new StringBuilder(str.length());
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'u' -> {
                        // 处理 Unicode 转义: backslash + u + 4位十六进制
                        if (i + 4 < str.length()) {
                            String hex = str.substring(i + 1, i + 5);
                            try {
                                int codePoint = Integer.parseInt(hex, 16);
                                result.append((char) codePoint);
                                i += 4;
                            } catch (NumberFormatException e) {
                                result.append('\\').append(c);
                            }
                        } else {
                            result.append('\\').append(c);
                        }
                    }
                    default -> result.append('\\').append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 检查是否是有效的 JSON 结构
     * <p>
     * 验证规则：
     * 1. 以 { 开头，以 } 结尾
     * 2. 包含至少一个键值对（格式如 "key": value）
     * 3. 括号配对正确
     */
    private static boolean isValidJsonStructure(String json) {
        if (json == null || json.length() < 2) {
            return false;
        }

        json = json.strip();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }

        // 验证括号配对
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        boolean hasKeyValue = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                    if (depth < 0) {
                        return false;
                    }
                } else if (c == ':') {
                    // 检测到键值对分隔符
                    hasKeyValue = true;
                }
            }
        }

        return hasKeyValue && depth == 0 && !inString;
    }

    /**
     * 清理 JSON 中的非法字符
     * <p>
     * AI 模型返回的 JSON 可能存在以下问题：
     * 1. 字符串值中包含未转义的控制字符（换行符、制表符等）
     * 2. 使用单引号作为字符串定界符（JSON 规范要求双引号）
     * 3. 对象字段之间缺少逗号分隔符
     * 4. 对象/数组末尾多余的逗号（尾随逗号）
     * <p>
     * 此方法处理这些问题，生成合法的 JSON。
     */
    private static String sanitizeJson(String json) {
        StringBuilder result = new StringBuilder(json.length() * 2);
        boolean inDoubleQuote = false;  // 是否在双引号字符串内
        boolean inSingleQuote = false;  // 是否在单引号字符串内（需要转换为双引号）
        boolean escaped = false;
        boolean expectValue = false;    // 期望值（刚遇到冒号后）
        boolean afterValue = false;     // 刚结束一个值（字符串、数字、布尔等）

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                result.append(c);
                escaped = true;
                continue;
            }

            // 处理双引号
            if (c == '"') {
                if (!inSingleQuote) {
                    if (!inDoubleQuote && afterValue) {
                        // 新字符串开始但前面有值，缺少逗号，自动补上
                        result.append(',');
                        afterValue = false;
                    }
                    inDoubleQuote = !inDoubleQuote;
                    if (!inDoubleQuote) {
                        // 字符串结束，标记值结束
                        afterValue = true;
                        expectValue = false;
                    }
                }
                result.append(c);
                continue;
            }

            // 处理单引号（可能被AI误用作字符串定界符）
            if (c == '\'') {
                if (inDoubleQuote) {
                    // 在双引号字符串内，单引号是合法字符，保留
                    result.append(c);
                } else if (inSingleQuote) {
                    // 单引号字符串结束，转换为双引号
                    inSingleQuote = false;
                    result.append('"');
                    afterValue = true;
                    expectValue = false;
                } else {
                    // 单引号字符串开始，转换为双引号
                    if (afterValue) {
                        // 新字符串开始但前面有值，缺少逗号，自动补上
                        result.append(',');
                        afterValue = false;
                    }
                    inSingleQuote = true;
                    result.append('"');
                }
                continue;
            }

            // 不在字符串内时处理结构字符
            if (!inDoubleQuote && !inSingleQuote) {
                // 跳过空白字符
                if (Character.isWhitespace(c)) {
                    continue;
                }

                // 处理冒号
                if (c == ':') {
                    result.append(c);
                    expectValue = true;
                    afterValue = false;
                    continue;
                }

                // 处理逗号
                if (c == ',') {
                    result.append(c);
                    afterValue = false;
                    expectValue = false;
                    continue;
                }

                // 处理对象开始
                if (c == '{') {
                    if (afterValue) {
                        result.append(',');
                    }
                    result.append(c);
                    afterValue = false;
                    expectValue = false;
                    continue;
                }

                // 处理对象结束（需要移除尾随逗号）
                if (c == '}') {
                    // 移除尾随逗号
                    removeTrailingComma(result);
                    result.append(c);
                    afterValue = true;
                    expectValue = false;
                    continue;
                }

                // 处理数组开始
                if (c == '[') {
                    if (afterValue) {
                        result.append(',');
                    }
                    result.append(c);
                    afterValue = false;
                    expectValue = false;
                    continue;
                }

                // 处理数组结束（需要移除尾随逗号）
                if (c == ']') {
                    // 移除尾随逗号
                    removeTrailingComma(result);
                    result.append(c);
                    afterValue = true;
                    expectValue = false;
                    continue;
                }

                // 处理非字符串值（数字、布尔、null）
                // 检测值开始
                if (expectValue || afterValue) {
                    if (afterValue && (c == '"' || c == '\'' || Character.isLetterOrDigit(c) || c == '-' || c == '{' || c == '[')) {
                        // 新值开始但前面有值，缺少逗号，自动补上
                        result.append(',');
                        afterValue = false;
                    }
                }

                // 收集非字符串值直到遇到结构分隔符
                if (Character.isLetterOrDigit(c) || c == '-' || c == '+' || c == '.') {
                    result.append(c);
                    // 继续收集直到遇到分隔符
                    while (i + 1 < json.length()) {
                        char next = json.charAt(i + 1);
                        if (Character.isWhitespace(next) || next == ',' || next == '}' || next == ']' || next == ':') {
                            break;
                        }
                        i++;
                        result.append(next);
                    }
                    afterValue = true;
                    expectValue = false;
                    continue;
                }
            }

            // 处理字符串内的控制字符
            if ((inDoubleQuote || inSingleQuote) && c < 0x20) {
                switch (c) {
                    case '\n' -> result.append("\\n");
                    case '\r' -> result.append("\\r");
                    case '\t' -> result.append("\\t");
                    case '\b' -> result.append("\\b");
                    case '\f' -> result.append("\\f");
                    default -> result.append(String.format("\\u%04x", (int) c));
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * 移除 StringBuilder 末尾的逗号（用于处理尾随逗号）
     */
    private static void removeTrailingComma(StringBuilder sb) {
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == ',') {
            sb.deleteCharAt(len - 1);
        }
    }
}
