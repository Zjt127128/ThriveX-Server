package liuyuyang.net.web.controller;

import io.swagger.annotations.Api;
import liuyuyang.net.web.service.ToolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Api(tags = "首页")
@RestController
@RequestMapping("/tool")
public class ToolController {
    @Resource
    private ToolService toolService;
    @GetMapping("/checkUrl")
    public List checkUrl(@RequestParam("urls") String urls) {
        String[] urlList = urls.split(",");
        return toolService.checkUrl(Arrays.asList(urlList));
    }
}
