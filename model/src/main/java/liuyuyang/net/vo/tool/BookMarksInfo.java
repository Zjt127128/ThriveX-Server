package liuyuyang.net.vo.tool;

import lombok.Data;

import java.util.List;

@Data
public class BookMarksInfo {
    private String title;
    private String url;
    private String id;
    private List<BookMarksInfo> children;
}
