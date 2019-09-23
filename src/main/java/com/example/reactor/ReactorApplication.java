package com.example.reactor;

import java.lang.management.ManagementFactory;

import com.example.config.ShutdownApplicationListener;
import com.example.config.StartupApplicationListener;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class ReactorApplication {

    public static void main(String[] args) {
        DisposableServer server =
                HttpServer.create().port(8080)
                          .route(routes ->
                              routes.get("/",        
                                         (request, response) -> response.sendString(Mono.just("Hello World!"))
                              )).bindNow();
        System.out.println(StartupApplicationListener.MARKER);
        System.out.println("Class count: netty=" + ManagementFactory
        .getClassLoadingMXBean().getTotalLoadedClassCount());
        server.onDispose()
              .block();
        System.out.println(ShutdownApplicationListener.MARKER);
    }
}