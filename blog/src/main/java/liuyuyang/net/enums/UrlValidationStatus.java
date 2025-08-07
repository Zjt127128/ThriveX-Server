package liuyuyang.net.enums;

/**
 * URL验证状态枚举
 * 用于统一管理URL验证过程中的各种状态和响应码
 */
public enum UrlValidationStatus {

    // 成功状态
    VALID_AND_ACCESSIBLE("URL有效且可访问", true, 200),
    VALID_WITH_ANTI_SCRAPING("URL有效（反爬机制触发但内容可访问）", true, 200),

    // 未验证状态
    NOT_VERIFIED("未验证", false, -1),

    // 错误状态
    INVALID_OR_SERVER_ERROR("URL无效或服务器错误", false, 400),
    INVALID_WITH_ANTI_SCRAPING("URL无效（反爬机制触发且内容不可访问）", false, 403),
    CONNECTION_ERROR("连接错误", false, -1),
    RATE_LIMITED("请求过多被限制，稍后重试", false, 429),

    // 特殊状态
    UNAUTHORIZED("未授权访问", false, 401),
    FORBIDDEN("禁止访问", false, 403),
    NOT_FOUND("页面不存在", false, 404),
    TIMEOUT("请求超时", false, 408),
    SERVER_ERROR("服务器内部错误", false, 500);

    private final String message;
    private final boolean valid;
    private final int defaultCode;

    UrlValidationStatus(String message, boolean valid, int defaultCode) {
        this.message = message;
        this.valid = valid;
        this.defaultCode = defaultCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isValid() {
        return valid;
    }

    public int getDefaultCode() {
        return defaultCode;
    }

    /**
     * 根据HTTP响应码获取对应的验证状态
     * @param responseCode HTTP响应码
     * @return 对应的验证状态
     */
    public static UrlValidationStatus fromResponseCode(int responseCode) {
        if (responseCode >= 200 && responseCode < 300) {
            return VALID_AND_ACCESSIBLE;
        } else if (responseCode >= 300 && responseCode < 400) {
            return VALID_AND_ACCESSIBLE; // 重定向也认为是有效的
        } else if (responseCode == 401) {
            return UNAUTHORIZED;
        } else if (responseCode == 403) {
            return FORBIDDEN;
        } else if (responseCode == 404) {
            return NOT_FOUND;
        } else if (responseCode == 408) {
            return TIMEOUT;
        } else if (responseCode == 429) {
            return RATE_LIMITED;
        } else if (responseCode >= 500) {
            return SERVER_ERROR;
        } else {
            return INVALID_OR_SERVER_ERROR;
        }
    }

    /**
     * 根据异常类型获取对应的验证状态
     * @param exception 异常对象
     * @return 对应的验证状态
     */
    public static UrlValidationStatus fromException(Exception exception) {
        if (exception instanceof java.net.SocketTimeoutException) {
            return TIMEOUT;
        } else if (exception instanceof java.net.ConnectException) {
            return CONNECTION_ERROR;
        } else if (exception instanceof java.io.IOException) {
            return CONNECTION_ERROR;
        } else {
            return CONNECTION_ERROR;
        }
    }
}

