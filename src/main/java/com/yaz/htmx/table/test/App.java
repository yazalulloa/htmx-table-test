package com.yaz.htmx.table.test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

@Slf4j
public class App {

  public static final Random RANDOM = new SecureRandom();
  public static final char[] ALPHANUMERIC = "abcdefghjkmnpqrstuvwxyzABCDEFGHIJKMNPQRSTUVWXYZ0123456789".toCharArray();

  public static void main(String[] args) {
    final var vertx = Vertx.vertx();

    final var router = Router.router(vertx);

    router.route()
        .handler(ResponseTimeHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .handler(LoggerHandler.create(true, LoggerFormat.DEFAULT))
        .handler(FaviconHandler.create(vertx));

    final var templateEngine = ThymeleafTemplateEngine.create(vertx);
    final var templateResolver = new FileTemplateResolver();

    templateResolver.setSuffix(".html");

    ((TemplateEngine) templateEngine.unwrap()).setTemplateResolver(templateResolver);

    final var cacheEnabled = envBool("CACHE_ENABLED");

    Handler<RoutingContext> usersDataHandler = ctx -> {
      final var results = Stream.generate(() -> {
            final var id = randomStr(10);
            return new JsonObject()
                .put("id", id)
                .put("name", "Agent Smith")
                .put("email", "void10@null.org")
                .put("delete_item_url", "/dynamic/users/" + id);
          })
          .limit(30)
          .map(JsonObject::getMap)
          .collect(Collectors.toList());

      ctx.data().put("total_count", randomInt(10, 99));
      ctx.data().put("query_count", randomInt(10, 99));
      ctx.data().put("results", results);

      ctx.next();
    };

    router.get("/dynamic/users").handler(usersDataHandler);
    router.get("/dynamic/card/users").handler(usersDataHandler);

    router.get("/dynamic/users-counters").handler(ctx -> {

      ctx.data().put("total_count", randomInt(10, 99));
      ctx.data().put("query_count", randomInt(10, 99));
      ctx.next();
    });

    router.delete("/dynamic/users/:id").handler(RoutingContext::end);

    final var templateHandler = TemplateHandler.create(templateEngine);
    Handler<RoutingContext> disableCacheHandler = ctx -> {
      if (!cacheEnabled) {
        ctx.addEndHandler(r -> templateEngine.clearCache());
      }

      templateHandler.handle(ctx);
    };

    router.route("/dynamic/*").handler(ctx -> {
      log.info("PATH {}", ctx.request().path());
      ctx.data().put("next_page_url", ctx.request().path());
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        ctx.next();
      } catch (InterruptedException e) {
        ctx.fail(e);
      }
    });

    router.route("/dynamic/*").handler(disableCacheHandler);
    router.route("/*").handler(StaticHandler.create().setCachingEnabled(cacheEnabled));

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080, ar -> {

          if (ar.failed()) {
            log.error("Failed to start server", ar.cause());
            System.exit(-1);
          } else {
            log.info("Server started");
          }
        });
  }


  public static boolean envBool(String key) {
    return Optional.ofNullable(System.getenv(key))
        .map(str -> {
          try {
            return Boolean.parseBoolean(str);
          } catch (Exception e) {
            return false;
          }
        })
        .orElse(false);
  }

  public static String generate(int length, char[] array) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char c = array[RANDOM.nextInt(array.length)];
      sb.append(c);
    }
    return sb.toString();
  }

  public static String randomStr(int length) {
    return generate(length, ALPHANUMERIC);
  }

  public static int randomInt(int min, int max) {
    return RANDOM.nextInt(max - min) + min;
  }
}
