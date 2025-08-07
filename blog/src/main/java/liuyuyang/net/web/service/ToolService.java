package liuyuyang.net.web.service;

import liuyuyang.net.vo.TooLVo;

import java.util.List;
import java.util.Map;

public interface ToolService {
    Map<String, List<Map<String, Object>>> checkUrl(List<String> urls);

    Map<String, List<Map<String, Object>>> checkInfo(TooLVo tooLVo);
}
