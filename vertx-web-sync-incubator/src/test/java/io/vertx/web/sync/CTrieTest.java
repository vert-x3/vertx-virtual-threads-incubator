package io.vertx.web.sync;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.web.sync.impl.CTrie;
import io.vertx.web.sync.impl.LList;
import io.vertx.web.sync.impl.RoutingContextInternal;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class CTrieTest {

  static class TestNeedle implements RoutingContextInternal {

    public final String path;

    public final MultiMap params = MultiMap.caseInsensitiveMultiMap();
    public final MultiMap verify = MultiMap.caseInsensitiveMultiMap();

    TestNeedle(String path) {
      this.path = path;
    }

    public TestNeedle test(String name, String value) {
      this.verify.add(name, value);
      return this;
    }

    @Override
    public void addParam(String name, String value) {
      this.params.add(name, value);
    }

    @Override
    public String path() {
      return path;
    }

    @Override
    public HttpMethod method() {
      return null;
    }

    @Override
    public String getHeader(CharSequence key) {
      return null;
    }

    @Override
    public RoutingContext putHeader(CharSequence key, CharSequence value) {
      return null;
    }

    @Override
    public WebHandler.HandlerReturn next() {
      return null;
    }

    @Override
    public WebHandler.HandlerReturn end() {
      return null;
    }

    @Override
    public WebHandler.HandlerReturn end(String chunk) {
      return null;
    }

    @Override
    public WebHandler.HandlerReturn end(Buffer chunk) {
      return null;
    }
  }


  final Handler<?> noOp = (x) -> {
  };
  final Handler<?> filterNoOp = (x) -> {
  };

  @Test
  public void testAddAndGet() {
    final CTrie<Handler<?>> tree = new CTrie<>();

    final String[] routes = {
      "/hi",
      "/contact",
      "/co",
      "/c",
      "/a",
      "/ab",
      "/doc/",
      "/doc/node_faq.html",
      "/doc/node1.html",
      "/α",
      "/β"
    };

    for (String route : routes) {
      tree.add(route, noOp);
    }

    final String[] goodTestData = {
      "/a",
      "/hi",
      "/contact",
      "/co",
      "/ab",
      "/α",
      "/β"
    };

    final String[] badTestData = {
      "/",
      "/con",
      "/cone",
      "/no"
    };

    for (String route : goodTestData) {
      final LList<Handler<?>> needle = tree.search(new TestNeedle(route));
      assertNotNull(needle);
    }

    for (String route : badTestData) {
      final LList<Handler<?>> needle = tree.search(new TestNeedle(route));
      assertNull(needle);
    }
  }

  @Test
  public void testWildcard() {
    final CTrie<Handler<?>> tree = new CTrie<>();
    final String[] routes = {
      "/",
      "/cmd/:tool/:sub",
      "/cmd/:tool/",
      "/src/*filepath",
      "/search/",
      "/search/:query",
      "/user_:name",
      "/user_:name/about",
      "/files/:dir/*filepath",
      "/doc/",
      "/doc/node_faq.html",
      "/doc/node1.html",
      "/info/:user/public",
      "/info/:user/project/:project"
    };

    for (String route : routes) {
      tree.add(route, noOp);
    }

    // tree.printTree();

    final TestNeedle[] foundData = {
      new TestNeedle("/"),
      new TestNeedle("/cmd/test/").test("tool", "test"),
      new TestNeedle("/cmd/test/3").test("tool", "test").test("sub", "3"),
      new TestNeedle("/src/").test("filepath", "/"),
      new TestNeedle("/src/some/file.png").test("filepath", "/some/file.png"),
      new TestNeedle("/search/"),
      new TestNeedle("/search/中文").test("query", "中文"),
      new TestNeedle("/user_noder").test("name", "noder"),
      new TestNeedle("/user_noder/about").test("name", "noder"),
      new TestNeedle("/files/js/inc/framework.js").test("dir", "js").test("filepath", "/inc/framework.js"),
      new TestNeedle("/info/gordon/public").test("user", "gordon"),
      new TestNeedle("/info/gordon/project/node").test("user", "gordon").test("project", "node")
    };

    for (TestNeedle testNeedle : foundData) {
      final LList<Handler<?>> needle = tree.search(testNeedle);
      assertNotNull(needle);
      // TODO: properly compare the multimap
      assertEquals(testNeedle.params.toString(), testNeedle.verify.toString());
    }

    final TestNeedle[] noHandlerData = {
      new TestNeedle("/cmd/test").test("tool", "test"),
      new TestNeedle("/search/中文/").test("query", "中文")
    };

    for (TestNeedle testNeedle : noHandlerData) {
      final LList<Handler<?>> needle = tree.search(testNeedle);
      assertNull(needle);
      // TODO: properly compare the multimap
      assertEquals(testNeedle.params.toString(), testNeedle.verify.toString());
    }
  }

  @Test
  public void testAppendHandler() {
    final CTrie<Handler<?>> tree = new CTrie<>();

    // OK
    tree.add("/", noOp);
    // Should merge into a new list
    tree.add("/", noOp);

    LList<Handler<?>> needle = tree.search(new TestNeedle("/"));
    assertNotNull(needle);
    final AtomicInteger cnt = new AtomicInteger();
    needle.forEach(el -> {
      assertEquals(noOp, el);
      cnt.incrementAndGet();
    });
    assertEquals(2, cnt.get());
  }

  @Test
  public void jsonTest() {
    final CTrie<Handler<?>> tree = new CTrie<>();
    final String[] routes = {
      "/",
      "/cmd/:tool/:sub",
      "/cmd/:tool/",
      "/src/*filepath",
      "/search/",
      "/search/:query",
      "/user_:name",
      "/user_:name/about",
      "/files/:dir/*filepath",
      "/doc/",
      "/doc/node_faq.html",
      "/doc/node1.html",
      "/info/:user/public",
      "/info/:user/project/:project"
    };

    for (String route : routes) {
      tree.add(route, noOp);
    }

    System.out.println(tree.toJson().encodePrettily());
  }
}
