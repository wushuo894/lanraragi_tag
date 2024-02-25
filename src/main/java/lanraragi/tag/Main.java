package lanraragi.tag;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lanraragi.tag.entity.Info;
import lanraragi.tag.util.InfoUtil;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static final Gson gson = new Gson();
    public static String AUTHORIZATION = "";
    public static String HOST = "http://127.0.0.1:3000";
    public static String KEY = "";

    public static ExecutorService EXECUTOR;

    public static void main(String[] args) {
        if (ArrayUtil.isEmpty(args)) {
            return;
        }

        int threadNum = 2;

        for (List<String> strings : CollUtil.split(List.of(args), 2)) {
            String k = strings.get(0);
            String v = strings.get(1);
            if (List.of("-k", "--key").contains(k)) {
                KEY = v;
            }
            if (List.of("-h", "--host").contains(k)) {
                HOST = v;
            }
            if (List.of("-t").contains(k)) {
                threadNum = Integer.parseInt(v);
            }
        }

        EXECUTOR = ExecutorBuilder.create()
                .setCorePoolSize(threadNum)
                .setMaxPoolSize(threadNum)
                .setWorkQueue(new LinkedBlockingQueue<>(100))
                .build();

        if (StrUtil.isBlank(KEY)) {
            Assert.notBlank(KEY, "password no can`t blank");
        }
        AUTHORIZATION = "Bearer " + Base64.encode(KEY);


        AtomicLong recordsFiltered = new AtomicLong(-1L);

        int length = 10;

        AtomicLong index = new AtomicLong(0);

        AtomicBoolean loop = new AtomicBoolean(true);

        ReentrantLock lock = new ReentrantLock();

        ThreadUtil.execute(() -> {
            do {
                long index_ = index.get();
                long recordsFiltered_ = recordsFiltered.get();

                if (recordsFiltered_ < 0) {
                    continue;
                }


                lock.lock();

                loop.set(index_ < recordsFiltered_);

                int progress = Double.valueOf((index_ * 1.0 / (recordsFiltered_ * 1.0)) * 100).intValue();

                System.out.print("\r");
                System.out.print("[");
                for (int i = 0; i < progress; i++) {
                    System.out.print("=");
                }
                for (int i = progress; i < 100; i++) {
                    System.out.print(" ");
                }
                System.out.print("]");
                System.out.print(" " + progress + "%");
                lock.unlock();
                ThreadUtil.sleep(10);
            } while (loop.get());

            System.out.println();
            System.out.println("end");
            System.exit(0);
        });

        while (loop.get()) {
            HttpResponse execute = HttpRequest.get(HOST + "/search")
                    .form("length", length)
                    .form("start", 0)
                    .form("search[value]", "date_added")
                    .header(Header.AUTHORIZATION, AUTHORIZATION)
                    .execute();
            String body = execute.body();
            JsonObject asJsonObject = gson.fromJson(body, JsonObject.class);
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("data");
            if (recordsFiltered.get() < 0) {
                recordsFiltered.set(asJsonObject.get("recordsFiltered").getAsLong());
            }
            List<JsonElement> jsonElements = asJsonArray.asList();
            int size = jsonElements.size();

            if (size < 1) {
                continue;
            }

            CountDownLatch countDownLatch = new CountDownLatch(size);

            for (JsonElement element : jsonElements) {
                EXECUTOR.submit(() -> {
                    Info info = gson.fromJson(element, Info.class);

                    String title = InfoUtil.getTitle(info);
                    List<String> tags = InfoUtil.getTags(info);

                    if (tags.isEmpty()) {
                        // 进度累加
                        index.incrementAndGet();
                        countDownLatch.countDown();
                        return;
                    }

                    HttpResponse response = HttpRequest.put(HOST + "/api/archives/" + info.getArcid() + "/metadata")
                            .form("tags", CollUtil.join(tags, ","))
                            .form("title", title)
                            .header(Header.AUTHORIZATION, AUTHORIZATION)
                            .execute();
                    String error = gson.fromJson(response.body(), JsonObject.class).get("error").getAsString();
                    if (StrUtil.isNotBlank(error)) {
                        lock.lock();
                        System.out.print("\r\n");
                        System.err.print(error);
                        System.exit(1);
                        lock.unlock();
                    }
                    // 进度累加
                    index.incrementAndGet();
                    countDownLatch.countDown();
                });
            }

            // 等待当前页完成
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
