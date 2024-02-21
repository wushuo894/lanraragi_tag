package lanraragi.tag;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static final Gson gson = new Gson();
    public static String AUTHORIZATION = "";
    public static String host = "http://127.0.0.1:3000";
    public static String password = "";

    public static void main(String[] args) {
        if (ArrayUtil.isEmpty(args)) {
            return;
        }

        for (List<String> strings : CollUtil.split(List.of(args), 2)) {
            String k = strings.get(0);
            String v = strings.get(1);
            if (List.of("-p", "--password").contains(k)) {
                password = v;
            }
            if (List.of("-h", "--host").contains(k)) {
                host = v;
            }
        }

        if (StrUtil.isBlank(password)) {
            Assert.notBlank(password, "password no can`t blank");
        }
        AUTHORIZATION = "Bearer " + Base64.encode(password);


        int length = 100;

        while (true) {
            HttpResponse execute = HttpRequest.get(host + "/search")
                    .form("length", length)
                    .form("start", 0)
                    .form("search[value]", "date_added")
                    .header(Header.AUTHORIZATION, AUTHORIZATION)
                    .execute();
            String body = execute.body();
            JsonObject asJsonObject = gson.fromJson(body, JsonObject.class);
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("data");
            long recordsFiltered = asJsonObject.get("recordsFiltered").getAsLong();
            List<JsonElement> jsonElements = asJsonArray.asList();
            int size = jsonElements.size();
            int index = 0;

            if (size < 1) {
                System.out.println("end");
                break;
            }

            for (JsonElement element : jsonElements) {
                index++;

                System.out.println("page=" + PageUtil.totalPage(recordsFiltered, length) + "\t" + index + "/" + size);

                Info info = gson.fromJson(element, Info.class);
                System.out.println(info);

                String title = info.getTitle();

                List<String> tags = ReUtil.findAll("[\\(\\[]([^()\\[\\]]+)[\\)\\]]", title, 1)
                        .stream()
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());

                System.out.println("title = " + title);
                System.out.println(CollUtil.join(tags, ","));

                HttpResponse metadata = HttpRequest.put(host + "/api/archives/" + info.getArcid() + "/metadata")
                        .form("tags", CollUtil.join(tags, ","))
                        .form("title", title)
                        .header(Header.AUTHORIZATION, AUTHORIZATION)
                        .execute();
                System.out.println(metadata.body());
            }
            for (int i = 0; i < 100; i++) {
                System.out.print("=");
            }
            System.out.println("");
        }
    }
}
