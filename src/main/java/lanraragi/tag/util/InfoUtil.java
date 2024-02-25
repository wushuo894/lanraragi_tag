package lanraragi.tag.util;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lanraragi.tag.entity.Info;

import java.util.List;
import java.util.stream.Collectors;

public class InfoUtil {

    /**
     * 获取标签
     *
     * @param info
     * @return
     */
    public static List<String> getTags(Info info) {
        String title = info.getTitle();
        return ReUtil.findAll("[\\(\\[]([^\\[\\]]+)[\\)\\]]", title, 1)
                .stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 获取纯标题
     *
     * @param info
     * @return
     */
    public static String getTitle(Info info) {
        info = ObjUtil.clone(info);

        String title = info.getTitle();
        title = ReUtil.replaceAll(title, "[\\(\\[]([^\\[\\]]+)[\\)\\]]", "");

        if (title.contains("(") && title.contains(")")) {
            title = getTitle(info.setTitle(title));
        }

        if (title.contains("[") && title.contains("]")) {
            title = getTitle(info.setTitle(title));
        }

        return title.trim();
    }

    public static void main(String[] args) {
        String title = "[ふじ()] ドキプリ漫画";
        Info info = new Info().setTitle(title);
        title = getTitle(info);
        System.out.println(title);
        System.out.println(getTags(info));
    }

}
