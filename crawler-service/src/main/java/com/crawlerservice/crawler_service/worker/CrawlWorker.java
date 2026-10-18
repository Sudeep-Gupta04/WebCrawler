package com.crawlerservice.crawler_service.worker;

import com.crawlerservice.crawler_service.model.CrawlResult;
import com.crawlerservice.crawler_service.model.CrawlTask;
import com.crawlerservice.crawler_service.publisher.CrawlResultPublisher;
import com.crawlerservice.crawler_service.queue.RedisCrawlQueue;
import com.crawlerservice.crawler_service.service.PageCrawlerService;
import com.crawlerservice.crawler_service.service.UrlNormalizationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CrawlWorker {

    private static final Logger log = LoggerFactory.getLogger(CrawlWorker.class);

    private final RedisCrawlQueue redisQueue;
    private final PageCrawlerService pageCrawlerService;
    private final CrawlResultPublisher resultPublisher;
    private final UrlNormalizationService normalizer;
    private final ThreadPoolExecutor crawlerExecutor;
    private final int maxDepth;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread pollerThread;

    public CrawlWorker(
            RedisCrawlQueue redisQueue,
            PageCrawlerService pageCrawlerService,
            CrawlResultPublisher resultPublisher,
            UrlNormalizationService normalizer,
            @Qualifier("crawlerExecutor") ThreadPoolExecutor crawlerExecutor,
            @Value("${crawler.max-depth:2}") int maxDepth) {

        this.redisQueue = redisQueue;
        this.pageCrawlerService = pageCrawlerService;
        this.resultPublisher = resultPublisher;
        this.normalizer = normalizer;
        this.crawlerExecutor = crawlerExecutor;
        this.maxDepth = maxDepth;
    }

    @PostConstruct
    public void start() {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        pollerThread = new Thread(
                this::pollLoop,
                "crawler-redis-poller");

        pollerThread.setDaemon(false);
        pollerThread.start();

        log.info("Crawler worker started; maxDepth={}", maxDepth);
    }

    private void pollLoop() {

        while (running.get()) {
            try {
                CrawlTask task = redisQueue.poll();

                if (task == null) {
                    // Queue is empty — back off briefly rather than spinning at
                    // 100% CPU with rapid leftPop calls against Redis.
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                /*
                 * If the bounded executor is full, CallerRunsPolicy
                 * applies backpressure by running the task here.
                 */
                crawlerExecutor.execute(() -> processTask(task));

            } catch (RuntimeException ex) {
                if (running.get()) {
                    log.error("Error while polling/submitting crawl task", ex);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.info("Crawler poller stopped");
    }

    private void processTask(CrawlTask task) {

        log.info(
                "Crawling URL={}, depth={}",
                task.url(),
                task.depth());

        try {
            CrawlResult result = pageCrawlerService.crawl(task.url());

            log.info(
                    "Crawled URL={}, status={}, links={}",
                    result.url(),
                    result.statusCode(),
                    result.discoveredLinks().size());

            // Publish the current page's result to Kafka.
            resultPublisher.publish(result);

            // Do not discover deeper pages beyond the configured limit.
            if (task.depth() >= maxDepth) {
                return;
            }

            // Only successful HTML pages should normally produce links.
            if (result.statusCode() < 200 || result.statusCode() >= 300) {
                return;
            }

            for (String link : result.discoveredLinks()) {

                String normalizedLink = normalizer.normalize(link);

                if (normalizedLink == null) {
                    continue;
                }

                if (!isSameHost(normalizedLink, task.rootHost())) {
                    continue;
                }

                CrawlTask nextTask = new CrawlTask(
                        normalizedLink,
                        task.depth() + 1,
                        task.rootHost());

                boolean added = redisQueue.enqueueIfNew(nextTask);

                if (added) {
                    log.debug(
                            "Queued URL={}, depth={}",
                            normalizedLink,
                            nextTask.depth());
                }
            }

        } catch (Exception ex) {
            log.error(
                    "Failed to process URL={} at depth={}",
                    task.url(),
                    task.depth(),
                    ex);
        }
    }

    private boolean isSameHost(String url, String rootHost) {

        try {
            String host = URI.create(url).getHost();

            return host != null
                    && rootHost != null
                    && host.equalsIgnoreCase(rootHost);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @PreDestroy
    public void stop() {

        running.set(false);

        if (pollerThread != null) {
            pollerThread.interrupt();

            try {
                pollerThread.join(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        crawlerExecutor.shutdown();

        try {
            if (!crawlerExecutor.awaitTermination(10,
                    java.util.concurrent.TimeUnit.SECONDS)) {
                crawlerExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            crawlerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Crawler worker shutdown completed");
    }
}