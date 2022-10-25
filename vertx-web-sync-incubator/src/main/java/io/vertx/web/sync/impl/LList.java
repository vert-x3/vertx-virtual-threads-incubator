package io.vertx.web.sync.impl;

import java.util.function.Consumer;
import java.util.function.Function;

public class LList<T> {

  Node<T> head;
  Node<T> tail;

  // Linked list Node.
  static class Node<T> {
    T data;
    Node<T> next;

    // Constructor
    Node(T d) {
      data = d;
      next = null;
    }
  }

  LList(T data) {
    push(data);
  }

  LList() {
  }

  public void push(T data) {
    // Create a new node with given data
    Node<T> new_node = new Node<>(data);

    // If the Linked List is empty,
    // then make the new node as head
    if (head == null) {
      head = new_node;
    } else {
      // Insert the new_node at tail node
      tail.next = new_node;
    }
    // update tail
    tail = new_node;
  }

  public void forEach(Function<T, Boolean> consumer) {
    Node<T> currNode = head;
    // Traverse
    while (currNode != null) {
      if (consumer.apply(currNode.data)) {
        // Go to next node
        currNode = currNode.next;
      } else {
        return;
      }
    }
  }

  public void forEach(Consumer<T> consumer) {
    Node<T> currNode = head;
    // Traverse
    while (currNode != null) {
      consumer.accept(currNode.data);
      // Go to next node
      currNode = currNode.next;
    }
  }
}
