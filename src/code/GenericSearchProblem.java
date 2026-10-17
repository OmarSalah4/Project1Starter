
package code;

import java.util.*;

public abstract class GenericSearchProblem<S, A> {

    public static final class Transition<S, A> {
        public final S state;
        public final A action;
        public final int lifeCost;
        public final int energyCost;

        public Transition(S state, A action, int lifeCost, int energyCost) {
            this.state = state;
            this.action = action;
            this.lifeCost = lifeCost;
            this.energyCost = energyCost;
        }
    }

    public static final class SearchNode<S, A> {
        public final S state;
        public final SearchNode<S, A> parent;
        public final A action;
        public final int depth;
        public final int livesUsed;
        public final int energyUsed;

        public SearchNode(
                S state,
                SearchNode<S, A> parent,
                A action,
                int depth,
                int livesUsed,
                int energyUsed) {
            this.state = state;
            this.parent = parent;
            this.action = action;
            this.depth = depth;
            this.livesUsed = livesUsed;
            this.energyUsed = energyUsed;
        }
    }

    public static final class SearchResult<S, A> {
        public final SearchNode<S, A> goal;
        public final int expandedNodes;

        public SearchResult(SearchNode<S, A> goal, int expandedNodes) {
            this.goal = goal;
            this.expandedNodes = expandedNodes;
        }
    }

    protected abstract S initialState();

    protected abstract boolean isGoal(S state);

    protected abstract List<Transition<S, A>> successors(S state);

    protected abstract String stateKey(S state);

    protected int heuristicEnergy(S state) {
        return 0;
    }

    public final SearchResult<S, A> search(String strategy) {
        if (strategy == null) {
            return new SearchResult<>(null, 0);
        }

        switch (strategy.trim().toUpperCase(Locale.ROOT)) {
            case "UC":
                return bestFirst(false);

            case "AS":
                return bestFirst(true);

            case "ID":
                return iterativeDeepening();

            default:
                return new SearchResult<>(null, 0);
        }
    }

    /*
     * Uniform Cost Search and A* Search.
     * Priority:
     * 1. Lives used.
     * 2. Energy used, plus heuristic for A*.
     * 3. Search depth.
     */
    private SearchResult<S, A> bestFirst(boolean useHeuristic) {

        Comparator<SearchNode<S, A>> comparator = (a, b) -> {
            int comparison = Integer.compare(a.livesUsed, b.livesUsed);

            if (comparison != 0) {
                return comparison;
            }

            long costA = (long) a.energyUsed
                    + (useHeuristic ? heuristicEnergy(a.state) : 0);

            long costB = (long) b.energyUsed
                    + (useHeuristic ? heuristicEnergy(b.state) : 0);

            comparison = Long.compare(costA, costB);

            if (comparison != 0) {
                return comparison;
            }

            return Integer.compare(a.depth, b.depth);
        };

        PriorityQueue<SearchNode<S, A>> frontier =
                new PriorityQueue<>(comparator);

        Map<String, int[]> bestCosts = new HashMap<>();

        SearchNode<S, A> root = new SearchNode<>(
                initialState(), null, null, 0, 0, 0);

        frontier.add(root);
        bestCosts.put(stateKey(root.state), new int[]{0, 0});

        int expanded = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S, A> node = frontier.poll();

            String key = stateKey(node.state);
            int[] recorded = bestCosts.get(key);

            if (recorded == null
                    || node.livesUsed != recorded[0]
                    || node.energyUsed != recorded[1]) {
                continue;
            }

            if (isGoal(node.state)) {
                return new SearchResult<>(node, expanded);
            }

            expanded++;

            for (Transition<S, A> transition : successors(node.state)) {
                int newLives = node.livesUsed + transition.lifeCost;
                int newEnergy = node.energyUsed + transition.energyCost;

                String childKey = stateKey(transition.state);
                int[] oldCosts = bestCosts.get(childKey);

                if (oldCosts != null
                        && (oldCosts[0] < newLives
                        || (oldCosts[0] == newLives
                        && oldCosts[1] <= newEnergy))) {
                    continue;
                }

                bestCosts.put(childKey, new int[]{newLives, newEnergy});

                SearchNode<S, A> child = new SearchNode<>(
                        transition.state,
                        node,
                        transition.action,
                        node.depth + 1,
                        newLives,
                        newEnergy);

                frontier.add(child);
            }
        }

        return new SearchResult<>(null, expanded);
    }

    /*
     * Iterative Deepening Search.
     * Repeats depth-limited DFS with increasing limits.
     */
    private SearchResult<S, A> iterativeDeepening() {
        SearchNode<S, A> root = new SearchNode<>(
                initialState(), null, null, 0, 0, 0);

        int totalExpanded = 0;

        for (int limit = 0; limit <= 10000; limit++) {
            Map<String, Integer> shallowestSeen = new HashMap<>();
            Counter counter = new Counter();

            SearchNode<S, A> goal = depthLimited(
                    root, limit, shallowestSeen, counter);

            totalExpanded += counter.value;

            if (goal != null) {
                return new SearchResult<>(goal, totalExpanded);
            }
        }

        return new SearchResult<>(null, totalExpanded);
    }

    private static final class Counter {
        int value = 0;
    }

    private SearchNode<S, A> depthLimited(
            SearchNode<S, A> node,
            int limit,
            Map<String, Integer> shallowestSeen,
            Counter counter) {

        if (isGoal(node.state)) {
            return node;
        }

        if (node.depth >= limit) {
            return null;
        }

        String key = stateKey(node.state);
        Integer previousDepth = shallowestSeen.get(key);

        if (previousDepth != null && previousDepth <= node.depth) {
            return null;
        }

        shallowestSeen.put(key, node.depth);
        counter.value++;

        for (Transition<S, A> transition : successors(node.state)) {
            SearchNode<S, A> child = new SearchNode<>(
                    transition.state,
                    node,
                    transition.action,
                    node.depth + 1,
                    node.livesUsed + transition.lifeCost,
                    node.energyUsed + transition.energyCost);

            SearchNode<S, A> result = depthLimited(
                    child, limit, shallowestSeen, counter);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public static <S, A> List<A> actionsOf(SearchNode<S, A> node) {
        LinkedList<A> actions = new LinkedList<>();

        SearchNode<S, A> current = node;

        while (current != null && current.parent != null) {
            actions.addFirst(current.action);
            current = current.parent;
        }

        return actions;
    }
}
