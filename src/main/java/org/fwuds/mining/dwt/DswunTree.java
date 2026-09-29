package org.fwuds.mining.dwt;

import org.fwuds.model.WeightedTransaction;
import org.fwuds.util.FwudsMath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mutable DSWUN-tree plus the transaction-aligned TAILLIST from Algorithms 4 and 5. */
final class DswunTree {
    private final Node root = new Node(0, null);
    private final Deque<TailEntry> tailList = new ArrayDeque<>();
    private final Map<Integer, Integer> rank;
    private double sumTwu;

    DswunTree(Map<Integer, Integer> rank) { this.rank = rank; }

    void insert(WeightedTransaction transaction) {
        Integer[] ordered = java.util.Arrays.stream(transaction.items()).boxed()
                .sorted(Comparator.comparingInt(rank::get)).toArray(Integer[]::new);
        Node current = root;
        for (int item : ordered) {
            Node parent = current;
            current = current.children.computeIfAbsent(item, ignored -> new Node(item, parent));
            current.weight += transaction.twu();
        }
        tailList.addLast(new TailEntry(current, transaction.twu(), transaction.sequence()));
        sumTwu += transaction.twu();
    }

    void evictOldest(int count) {
        if (count < 0 || count > tailList.size()) throw new IllegalArgumentException("Invalid eviction count");
        for (int i = 0; i < count; i++) {
            TailEntry entry = tailList.removeFirst();
            Node node = entry.leaf;
            while (node != root) {
                Node parent = node.parent;
                node.weight -= entry.twu;
                if (Math.abs(node.weight) <= FwudsMath.EPSILON) {
                    node.weight = 0.0;
                    parent.children.remove(node.item, node);
                } else if (node.weight < 0.0) {
                    throw new IllegalStateException("DSWUN node weight became negative while evicting transaction " + entry.sequence);
                }
                node = parent;
            }
            sumTwu -= entry.twu;
        }
        if (Math.abs(sumTwu) <= FwudsMath.EPSILON) sumTwu = 0.0;
    }

    Map<Integer, WunList> generateWunLists() {
        Map<Integer, List<WunCode>> codes = new HashMap<>();
        Counter counter = new Counter();
        visit(root, codes, counter);
        Map<Integer, WunList> result = new HashMap<>();
        codes.forEach((item, value) -> result.put(item, new WunList(value)));
        return result;
    }

    private void visit(Node node, Map<Integer, List<WunCode>> codes, Counter counter) {
        if (node != root) node.pre = counter.pre++;
        List<Node> children = new ArrayList<>(node.children.values());
        children.sort(Comparator.comparingInt(child -> rank.get(child.item)));
        for (Node child : children) visit(child, codes, counter);
        if (node != root) {
            node.post = counter.post++;
            codes.computeIfAbsent(node.item, ignored -> new ArrayList<>())
                    .add(new WunCode(node.pre, node.post, node.weight));
        }
    }

    int transactionCount() { return tailList.size(); }
    double sumTwu() { return sumTwu; }

    private static final class Counter { int pre; int post; }

    static final class Node {
        final int item;
        final Node parent;
        final Map<Integer, Node> children = new HashMap<>();
        double weight;
        int pre;
        int post;

        Node(int item, Node parent) { this.item = item; this.parent = parent; }
    }

    private record TailEntry(Node leaf, double twu, long sequence) { }
}
