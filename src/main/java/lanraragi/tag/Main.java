package lanraragi.tag;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lanraragi.tag.entity.Info;
import lanraragi.tag.util.InfoUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Main implements Runnable {
    public static final Gson gson = new Gson();
    public static String AUTHORIZATION = "";
    public static String HOST = "http://127.0.0.1:3000";
    public static String KEY = "";
    public static Log log = Log.get(Main.class);

    public static ExecutorService EXECUTOR;

    public static void main(String[] args) {
        HashMap<String, String> map = new HashMap<>();

        Map<String, String> envMap = System.getenv();
        Map<String, String> argsMap = CollUtil.split(List.of(args), 2)
                .stream()
                .collect(Collectors.toMap(it -> it.get(0), it -> it.get(1)));

        map.putAll(argsMap);
        map.putAll(envMap);

        int threadNum = 2;

        boolean run = Boolean.TRUE;

        String cron = "";

        for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
            String k = stringStringEntry.getKey();
            String v = stringStringEntry.getValue();
            if (List.of("-k", "--key", "KEY").contains(k)) {
                KEY = v;
            }
            if (List.of("-h", "--host", "HOST").contains(k)) {
                HOST = v;
            }
            if (List.of("-c", "--cron", "CRON").contains(k)) {
                cron = v;
            }
            if (List.of("-t", "THREAD_NUM").contains(k)) {
                threadNum = Integer.parseInt(v);
            }
            if (List.of("-r", "--run", "RUN").contains(k)) {
                run = Boolean.parseBoolean(v);
            }
            if (List.of("--tz", "TZ").contains(k)) {
                TimeZone.setDefault(TimeZone.getTimeZone(v));
            }
        }

        EXECUTOR = ExecutorBuilder.create()
                .setCorePoolSize(threadNum)
                .setMaxPoolSize(threadNum)
                .setWorkQueue(new LinkedBlockingQueue<>(100))
                .build();

        if (StrUtil.isBlank(KEY)) {
            Assert.notBlank(KEY, "KEY no can`t blank");
        }
        AUTHORIZATION = "Bearer " + Base64.encode(KEY);

        Main main = new Main();
        try {
            if (run) {
                ThreadUtil.execute(main);
            }
        } catch (Exception e) {
            log.error(e);
        }

        if (StrUtil.isNotBlank(cron)) {
            CronUtil.schedule(cron, main);
            CronUtil.start();
        }
    }

    @Override
    public synchronized void run() {
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
            log.info("end");
        });

        while (loop.get()) {
            JsonObject jsonObject = HttpRequest.get(HOST + "/search")
                    .form("length", length)
                    .form("start", 0)
                    .form("search[value]", "date_added")
                    .header(Header.AUTHORIZATION, AUTHORIZATION)
                    .thenFunction(res -> gson.fromJson(res.body(), JsonObject.class));
            JsonArray data = jsonObject.getAsJsonArray("data");
            if (recordsFiltered.get() < 0) {
                recordsFiltered.set(jsonObject.get("recordsFiltered").getAsLong());
            }
            List<JsonElement> jsonElements = data.asList();
            int size = jsonElements.size();

            if (size < 1) {
                continue;
            }

            CountDownLatch countDownLatch = new CountDownLatch(size);

            for (JsonElement element : jsonElements) {
                EXECUTOR.submit(() -> {
                    try {
                        Info info = gson.fromJson(element, Info.class);

                        String title = InfoUtil.getTitle(info);
                        List<String> tags = InfoUtil.getTags(info);

                        String error = HttpRequest.put(HOST + "/api/archives/" + info.getArcid() + "/metadata")
                                .form("tags", CollUtil.join(tags, ","))
                                .form("title", title)
                                .header(Header.AUTHORIZATION, AUTHORIZATION)
                                .thenFunction(res -> gson.fromJson(res.body(), JsonObject.class).get("error").getAsString());
                        if (StrUtil.isNotBlank(error)) {
                            lock.lock();
                            System.out.print("\r\n");
                            System.err.print(error);
                            lock.unlock();
                        }
                    } catch (Exception e) {
                        log.error(e);
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
                log.error(e);
            }
        }
    }
}
