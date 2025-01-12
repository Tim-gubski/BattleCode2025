package SuperiorCowPowers_v11.Helpers;

import java.util.*;

public class KDTree {
    public static class KDNode {
        int[] point;  // The point coordinates
        KDNode left, right;

        public KDNode(int[] point) {
            this.point = point;
            this.left = this.right = null;
        }
    }

    private KDNode root;
    private int k; // Number of dimensions

    public KDTree(int k) {
        this.k = k;
    }

    // Insert a point into the KD-tree
    public void insert(int[] point) {
        root = insertRec(root, point, 0);
    }

    private KDNode insertRec(KDNode node, int[] point, int depth) {
        if (node == null) return new KDNode(point);

        int axis = depth % k;
        if (point[axis] < node.point[axis]) {
            node.left = insertRec(node.left, point, depth + 1);
        } else {
            node.right = insertRec(node.right, point, depth + 1);
        }
        return node;
    }

    // Find the nearest neighbor
    public int[] nearestNeighbor(int[] target) {
        return nearestNeighborRec(root, target, 0, null, Integer.MAX_VALUE).point;
    }

    private KDNode nearestNeighborRec(KDNode node, int[] target, int depth, KDNode best, int bestDist) {
        if (node == null) return best;

        int dist = euclideanDistance(node.point, target);
        if (dist < bestDist) {
            bestDist = dist;
            best = node;
        }

        int axis = depth % k;
        KDNode nextBranch = (target[axis] < node.point[axis]) ? node.left : node.right;
        KDNode otherBranch = (target[axis] < node.point[axis]) ? node.right : node.left;

        best = nearestNeighborRec(nextBranch, target, depth + 1, best, bestDist);

        if (Math.abs(target[axis] - node.point[axis]) < bestDist) {
            best = nearestNeighborRec(otherBranch, target, depth + 1, best, bestDist);
        }

        return best;
    }

    private int euclideanDistance(int[] a, int[] b) {
        int sum = 0;
        for (int i = 0; i < k; i++) {
            sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return sum;
    }
}
