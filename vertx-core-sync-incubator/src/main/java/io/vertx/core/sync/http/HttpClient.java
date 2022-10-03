package io.vertx.core.sync.http;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

import static io.vertx.await.Async.await;

public class HttpClient {

  private io.vertx.core.http.HttpClient delegate;

  public HttpClient(io.vertx.core.http.HttpClient delegate) {
    this.delegate = delegate;
  }

  public HttpClientRequest request(int port, String host, String httpMethod, String uri) {
    return new HttpClientRequest(await(delegate.request(new RequestOptions().setPort(port).setHost(host).setMethod(HttpMethod.valueOf(httpMethod)).setURI(uri))));
  }
}

