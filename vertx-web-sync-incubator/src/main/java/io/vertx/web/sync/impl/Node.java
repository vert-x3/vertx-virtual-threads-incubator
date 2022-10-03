package io.vertx.web.sync.impl;

import io.vertx.web.sync.WebHandler;

import java.util.ArrayList;
import java.util.List;

import static io.vertx.web.sync.impl.Node.Type.*;

public class Node {

  enum Type {
    STATIC,
    ROOT,
    PARAM,
    CATCH_ALL
  }

  private static int countParams(String path) {
    int n = 0;
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) != ':' && path.charAt(i) != '*') {
        continue;
      }
      n++;
    }

    return n;
  }

  private String path;
  private boolean wildChild;
  private Type type;
  private int maxParams;
  private String indices;
  private List<Node> children;
  private WebHandler[] handle;
  private int priority;

  Node() {
    this("", false, STATIC, 0, "", new ArrayList<>(), null, 0);
  }

  private Node(String path, boolean wildChild, Type type, int maxParams, String indices, List<Node> children, WebHandler[] handle, int priority) {
    this.path = path;
    this.wildChild = wildChild;
    this.type = type;
    this.maxParams = maxParams;
    this.indices = indices;
    this.children = children;
    this.handle = handle;
    this.priority = priority;
  }

  private int addPriority(int pos) {
    children.get(pos).priority++;
    final int prio = children.get(pos).priority;

    // Adjust position (move to from)
    int newPos = pos;
    while (newPos > 0 && children.get(newPos - 1).priority < prio) {
      final Node temp = children.get(newPos);
      children.set(newPos, children.get(newPos - 1));
      children.set(newPos - 1, temp);
      newPos--;
    }

    // Build new index char string
    if (newPos != pos) {
      indices =
        indices.substring(0, newPos) +
          indices.charAt(pos) +
          indices.substring(newPos, pos) +
          indices.substring(pos + 1);
    }

    return newPos;
  }

  private void insertChild(int numParams, String path, String fullPath, WebHandler[] handle) {
    Node n = this;
    int offset = 0; // Already handled chars of the path

    // Find prefix until first wildcard
    for (int i = 0, max = path.length(); numParams > 0; i++) {
      final char c = path.charAt(i);
      if (c != ':' && c != '*') {
        continue;
      }

      // Find wildcard end (either '/' or path end)
      int end = i + 1;
      while (end < max && path.charAt(end) != '/') {
        if (path.charAt(end) == ':' || path.charAt(end) == '*') {
          throw new IllegalStateException("only one wildcard per path segment is allowed, has: '" + path.substring(i) + "' in path '" + fullPath + "'");
        } else {
          end++;
        }
      }

      // Check if this Node existing children which would be unreachable
      // if we insert the wildcard here
      if (n.children.size() > 0) {
        throw new IllegalStateException("wildcard route '" + path.substring(i, end) + "' conflicts with existing children in path '" + fullPath + "'");
      }

      // check if the wildcard has a name
      if (end - i < 2) {
        throw new IllegalStateException("wildcards must be named with a non-empty name in path '" + fullPath + "'");
      }

      if (c == ':') {
        // Split path at the beginning of the wildcard
        if (i > 0) {
          n.path = path.substring(offset, i);
          offset = i;
        }

        final Node child = new Node("", false, PARAM, numParams, "", new ArrayList<>(), null, 0);
        n.children = new ArrayList<>();
        n.children.add(child);
        n.wildChild = true;
        n = child;
        n.priority++;
        numParams--;
        if (end < max) {
          n.path = path.substring(offset, end);
          offset = end;

          final Node staticChild = new Node(
            "",
            false,
            STATIC,
            numParams,
            "",
            new ArrayList<>(),
            null,
            1
          );
          n.children = new ArrayList<>();
          n.children.add(staticChild);
          n = staticChild;
        }
      } else {
        if (end != max || numParams > 1) {
          throw new IllegalStateException("catch-all routes are only allowed at the end of the path in path '" + fullPath + "'");
        }

        if (n.path.length() > 0 && n.path.charAt(n.path.length() - 1) == '/') {
          throw new IllegalStateException("catch-all conflicts with existing handle for the path segment root in path '" + fullPath + "'");
        }

        i--;
        if (path.charAt(i) != '/') {
          throw new IllegalStateException("no / before catch-all in path '" + fullPath + "'");
        }

        n.path = path.substring(offset, i);

        // first node: catchAll node with empty path
        final Node catchAllChild = new Node("", true, CATCH_ALL, 1, "", new ArrayList<>(), null, 0);
        n.children = new ArrayList<>();
        n.children.add(catchAllChild);
        n.indices = path.substring(i, i + 1);
        n = catchAllChild;
        n.priority++;

        // second node: node holding the variable
        final Node child = new Node(
          path.substring(i),
          false,
          CATCH_ALL,
          1,
          "",
          new ArrayList<>(),
          handle,
          1
        );
        n.children = new ArrayList<>();
        n.children.add(child);

        return;
      }
    }

    // insert remaining path part and handle to the leaf
    n.path = path.substring(offset);
    n.handle = handle;
  }


  public void addRoute(String path, WebHandler... handle) {
    Node n = this;
    String fullPath = path;

    n.priority++;
    int numParams = countParams(path);

    // Non-empty tree
    if (n.path.length() > 0 || n.children.size() > 0) {
      walk:
      while (true) {
        // Update maxParams of the current node
        if (numParams > n.maxParams) {
          n.maxParams = numParams;
        }

        // Find the longest common prefix
        // This also implies that the common prefix contains no ':' or '*'
        // since the existing key can't contain those chars.
        int i = 0;
        int max = Math.min(path.length(), n.path.length());
        while (i < max && path.charAt(i) == n.path.charAt(i)) {
          i++;
        }

        // Split edge
        if (i < n.path.length()) {
          final Node child = new Node(
            n.path.substring(i),
            n.wildChild,
            STATIC,
            0,
            n.indices,
            n.children,
            n.handle,
            n.priority - 1
          );

          // Update maxParams (max of all children)
          child.children.forEach(grandChild -> {
            if (grandChild.maxParams > child.maxParams) {
              child.maxParams = grandChild.maxParams;
            }
          });

          n.children = new ArrayList<>();
          n.children.add(child);
          n.indices = n.path.substring(i, i + 1);
          n.path = path.substring(0, i);
          n.handle = null;
          n.wildChild = false;
        }

        // Make new node a child of this node
        if (i < path.length()) {
          path = path.substring(i);

          if (n.wildChild) {
            n = n.children.get(0);
            n.priority++;

            // Update maxParams of the child node
            if (numParams > n.maxParams) {
              n.maxParams = numParams;
            }
            numParams--;

            // Check if the wildcard matches
            if (
              path.length() >= n.path.length() &&
                n.path.equals(path.substring(0, n.path.length())) &&
                (n.path.length() >= path.length() || path.charAt(n.path.length()) == '/')
            ) {
              continue walk;
            } else {
              // Wildcard conflict
              String pathSeg = "";
              if (n.type == CATCH_ALL) {
                pathSeg = path;
              } else {
                pathSeg = path.split("/")[0];
              }
              final String prefix =
                fullPath.substring(0, fullPath.indexOf(pathSeg)) + n.path;
              throw new IllegalStateException("'" + pathSeg + "' in new path '" + fullPath + "' conflicts with existing wildcard '" + n.path + "' in existing prefix '" + prefix + "'");
            }
          }

          final char c = path.charAt(0);

          // Slash after param
          if (n.type == PARAM && c == '/' && n.children.size() == 1) {
            n = n.children.get(0);
            n.priority++;
            continue walk;
          }

          // Check if a child with the next path char exists
          for (int j = 0; j < n.indices.length(); j++) {
            if (c == n.indices.charAt(j)) {
              j = n.addPriority(j);
              n = n.children.get(j);
              continue walk;
            }
          }

          // Otherwise insert it
          if (c != ':' && c != '*') {
            n.indices += c;
            final Node child = new Node(
              "",
              false,
              STATIC,
              numParams,
              "",
              new ArrayList<>(),
              null,
              0
            );
            n.children.add(child);
            n.addPriority(n.indices.length() - 1);
            n = child;
          }
          n.insertChild(numParams, path, fullPath, handle);
          return;
        } else if (i == path.length()) {
          // Make node a (in-path leaf)
          if (n.handle != null) {
            throw new IllegalStateException("A handle is already registered for path '" + fullPath + "'");
          }
          n.handle = handle;
        }
        return;
      }

    } else {
      // Empty tree
      n.insertChild(numParams, path, fullPath, handle);
      n.type = ROOT;
    }

  }

  public WebHandler[] search(final RoutingContextInternal ctx) {
    return search(ctx, true);
  }

  public WebHandler[] lookup(final RoutingContextInternal ctx) {
    return search(ctx, false);
  }

  private WebHandler[] search(final RoutingContextInternal ctx, boolean updateParams) {
    String path = ctx.path();
    Node n = this;

    walk:
    while (true) {
      if (path.length() > n.path.length()) {
        if (path.substring(0, n.path.length()).equals(n.path)) {
          path = path.substring(n.path.length());
          // If this node does not have a wildcard child,
          // we can just look up the next child node and continue
          // to walk down the tree
          if (!n.wildChild) {
            final char c = path.charAt(0);
            for (int i = 0; i < n.indices.length(); i++) {
              if (c == n.indices.charAt(i)) {
                n = n.children.get(i);
                continue walk;
              }
            }

            // Nothing found.
            return null;
          }

          // Handle wildcard child
          n = n.children.get(0);
          switch (n.type) {
            case PARAM:
              // Find param end
              int end = 0;
              while (end < path.length() && path.charAt(end) != '/') {
                end++;
              }

              if (updateParams) {
                // Save param value
                ctx.addParam(n.path.substring(1), path.substring(0, end));
              }

              // We need to go deeper!
              if (end < path.length()) {
                if (n.children.size() > 0) {
                  path = path.substring(end);
                  n = n.children.get(0);
                  continue walk;
                }

                // ... but we can't
                return null;
              }

              return n.handle;

            case CATCH_ALL:
              if (updateParams) {
                ctx.addParam(n.path.substring(2), path);
              }
              return n.handle;

            default:
              throw new RuntimeException("invalid node type");
          }
        }
      } else if (path.equals(n.path)) {
        return n.handle;
      }

      return null;
    }
  }

  private void printTree(String prefix) {
    System.out.println(" " + priority + ":" + maxParams + " " + prefix + path + "[" + children.size() + "] " + handle + " " + wildChild + " " + type);

    for (int l = path.length(); l > 0; l--) {
      prefix += " ";
    }

    for (Node child : children) {
      child.printTree(prefix);
    }
  }

  void printTree() {
    printTree("");
  }
}
