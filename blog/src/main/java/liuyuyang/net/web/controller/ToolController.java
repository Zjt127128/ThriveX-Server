package liuyuyang.net.web.controller;

import io.swagger.annotations.Api;
import liuyuyang.net.common.annotation.NoTokenRequired;
import liuyuyang.net.common.utils.Result;
import liuyuyang.net.vo.TooLVo;
import liuyuyang.net.web.service.ToolService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "首页")
@RestController
@RequestMapping("/tool")
public class ToolController {
    @Resource
    private ToolService toolService;

    @NoTokenRequired
    @PostMapping("/checkUrl")
    public Result<Map<String, List<Map<String, Object>>>> checkUrl(@RequestBody TooLVo tooLVo) {
        return Result.success(toolService.checkInfo(tooLVo));
    }
}
