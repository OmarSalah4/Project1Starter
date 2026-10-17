
package code;

import java.util.*;

public class CaveExplorer
        extends GenericSearchProblem<CaveExplorer.State, String> {

    private final int height;
    private final int width;
    private final int[][] grid;

    private final int initialX;
    private final int initialY;
    private final int initialEnergy;
    private final int initialRope;
    private final int initialLives = 3;

    private final int[] doorX;
    private final int[] doorY;
    private final int[] keyX;
    private final int[] keyY;

    private final int allDoorsMask;
    private final int allKeysMask;

    private static final String[] ACTIONS = {
        "left", "right", "climbup", "climbdown", "jumpdown"
    };

    public static final class State {
        final int x;
        final int y;
        final int energy;
        final int rope;
        final int lives;
        final int doorsMask;
        final int keysMask;
        final int carriedKey;

        State(int x, int y, int energy, int rope, int lives,
              int doorsMask, int keysMask, int carriedKey) {
            this.x = x;
            this.y = y;
            this.energy = energy;
            this.rope = rope;
            this.lives = lives;
            this.doorsMask = doorsMask;
            this.keysMask = keysMask;
            this.carriedKey = carriedKey;
        }
    }

    private CaveExplorer(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty input");
        }

        String[] parts = input.split(";", -1);

        int[] dimensions = parseNumbers(parts[0]);

        if (dimensions.length != 2) {
            throw new IllegalArgumentException("Invalid dimensions");
        }

        height = dimensions[0];
        width = dimensions[1];

        if (height < 3 || height > 10 || width < 3 || width > 10) {
            throw new IllegalArgumentException("Invalid grid dimensions");
        }

        if (parts.length < 3 + height + 2) {
            throw new IllegalArgumentException("Incomplete input");
        }

        int[] position = parseNumbers(parts[1]);
        int[] resources = parseNumbers(parts[2]);

        if (position.length != 2 || resources.length != 2) {
            throw new IllegalArgumentException("Invalid initial state");
        }

        initialX = position[0];
        initialY = position[1];
        initialEnergy = resources[0];
        initialRope = resources[1];

        if (initialEnergy < 0 || initialEnergy > 500
                || initialRope < 0 || initialRope > 25) {
            throw new IllegalArgumentException("Invalid resources");
        }

        grid = new int[height][width];

        for (int y = 0; y < height; y++) {
            int[] row = parseNumbers(parts[3 + y]);

            if (row.length != width) {
                throw new IllegalArgumentException("Invalid grid row");
            }

            for (int x = 0; x < width; x++) {
                if (row[x] < 0 || row[x] > 45) {
                    throw new IllegalArgumentException(
                            "Invalid cell difficulty");
                }

                grid[y][x] = row[x];
            }
        }

        int doorSection = 3 + height;
        int[] doors = parseNumbers(parts[doorSection]);

        if (doors.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid door coordinates");
        }

        doorX = new int[doors.length / 2];
        doorY = new int[doors.length / 2];

        for (int i = 0; i < doorX.length; i++) {
            doorX[i] = doors[2 * i];
            doorY[i] = doors[2 * i + 1];
        }

        int keySection = doorSection + 1;
        int[] keys = parseNumbers(parts[keySection]);

        if (keys.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid key coordinates");
        }

        keyX = new int[keys.length / 2];
        keyY = new int[keys.length / 2];

        for (int i = 0; i < keyX.length; i++) {
            keyX[i] = keys[2 * i];
            keyY[i] = keys[2 * i + 1];
        }

        if (doorX.length < 1 || doorX.length > 5
                || keyX.length < 1 || keyX.length > 8
                || keyX.length < doorX.length) {
            throw new IllegalArgumentException(
                    "Invalid number of doors or keys");
        }

        for (int i = 0; i < doorX.length; i++) {
            if (!inside(doorX[i], doorY[i])
                    || grid[doorY[i]][doorX[i]] == 0) {
                throw new IllegalArgumentException("Invalid door location");
            }
        }

        for (int i = 0; i < keyX.length; i++) {
            if (!inside(keyX[i], keyY[i])
                    || grid[keyY[i]][keyX[i]] == 0) {
                throw new IllegalArgumentException("Invalid key location");
            }
        }

        allDoorsMask = (1 << doorX.length) - 1;
        allKeysMask = (1 << keyX.length) - 1;
    }

    private static int[] parseNumbers(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new int[0];
        }

        String[] tokens = text.split(",");
        int[] result = new int[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            result[i] = Integer.parseInt(tokens[i].trim());
        }

        return result;
    }

    public static String solve(String caveSystem, String strategy) {
        try {
            CaveExplorer problem = new CaveExplorer(caveSystem);

            if (problem.initialX < 0
                    || problem.initialX >= problem.width
                    || problem.initialY < 0
                    || problem.initialY >= problem.height
                    || problem.grid[problem.initialY][problem.initialX] == 0) {
                return "No Solution";
            }

            GenericSearchProblem.SearchResult<State, String> result =
                    problem.search(strategy);

            if (result.goal == null) {
                return "No Solution";
            }

            List<String> plan =
                    GenericSearchProblem.actionsOf(result.goal);

            return String.join(",", plan)
                    + ";" + result.goal.livesUsed
                    + ";" + result.goal.energyUsed
                    + ";" + result.expandedNodes;

        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return "No Solution";
        }
    }

    @Override
    protected State initialState() {
        return new State(
                initialX, initialY,
                initialEnergy, initialRope, initialLives,
                allDoorsMask, allKeysMask, -1);
    }

    @Override
    protected boolean isGoal(State state) {
        return state.doorsMask == 0 && state.lives >= 1;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private int doorAt(int x, int y) {
        for (int i = 0; i < doorX.length; i++) {
            if (x == doorX[i] && y == doorY[i]) {
                return i;
            }
        }

        return -1;
    }

    private int keyAt(int x, int y, int keysMask) {
        for (int i = 0; i < keyX.length; i++) {
            if ((keysMask & (1 << i)) != 0
                    && x == keyX[i] && y == keyY[i]) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected List<Transition<State, String>> successors(State state) {
        List<Transition<State, String>> result = new ArrayList<>();

        int[] dx = {-1, 1, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 1};

        for (int i = 0; i < ACTIONS.length; i++) {
            int nx = state.x + dx[i];
            int ny = state.y + dy[i];

            if (!inside(nx, ny) || grid[ny][nx] == 0) {
                continue;
            }

            boolean jump = ACTIONS[i].equals("jumpdown");
            boolean climb = ACTIONS[i].equals("climbup")
                    || ACTIONS[i].equals("climbdown");

            if (jump && state.lives <= 1) {
                continue;
            }

            if (climb && state.rope <= 0) {
                continue;
            }

            // Current cost interpretation: all movement actions,
            // including jumpdown, pay destination-cell energy.
            int energyCost = grid[ny][nx];

            if (state.energy < energyCost) {
                continue;
            }

            State next = new State(
                    nx, ny,
                    state.energy - energyCost,
                    state.rope - (climb ? 1 : 0),
                    state.lives - (jump ? 1 : 0),
                    state.doorsMask,
                    state.keysMask,
                    state.carriedKey);

            result.add(new Transition<>(
                    next, ACTIONS[i],
                    jump ? 1 : 0,
                    energyCost));
        }

        // Collect a key at the current location.
        if (state.carriedKey == -1) {
            int keyIndex = keyAt(state.x, state.y, state.keysMask);

            if (keyIndex != -1) {
                State next = new State(
                        state.x, state.y,
                        state.energy, state.rope, state.lives,
                        state.doorsMask,
                        state.keysMask & ~(1 << keyIndex),
                        keyIndex);

                result.add(new Transition<>(next, "collect", 0, 0));
            }
        }

        // Unlock a door at the current location.
        if (state.carriedKey != -1) {
            int doorIndex = doorAt(state.x, state.y);

            if (doorIndex != -1
                    && (state.doorsMask & (1 << doorIndex)) != 0) {
                State next = new State(
                        state.x, state.y,
                        state.energy, state.rope, state.lives,
                        state.doorsMask & ~(1 << doorIndex),
                        state.keysMask,
                        -1);

                result.add(new Transition<>(next, "unlock", 0, 0));
            }
        }

        return result;
    }

    @Override
    protected String stateKey(State state) {
        return state.x + "," + state.y
                + "," + state.energy
                + "," + state.rope
                + "," + state.lives
                + "," + state.doorsMask
                + "," + state.keysMask
                + "," + state.carriedKey;
    }

    /*
     * A* heuristic:
     * Estimate the minimum energy to reach the next available key
     * or the next locked door.
     *
     * The estimate is energy-based, not a constant or distance-only
     * heuristic. It considers only the next useful target, so it
     * is a lower bound on the energy needed to complete the whole task.
     */
    @Override
    protected int heuristicEnergy(State state) {
        int best = Integer.MAX_VALUE;

        if (state.carriedKey != -1) {
            for (int i = 0; i < doorX.length; i++) {
                if ((state.doorsMask & (1 << i)) != 0) {
                    best = Math.min(best, shortestEnergy(
                            state.x, state.y, doorX[i], doorY[i]));
                }
            }
        } else {
            for (int i = 0; i < keyX.length; i++) {
                if ((state.keysMask & (1 << i)) != 0) {
                    best = Math.min(best, shortestEnergy(
                            state.x, state.y, keyX[i], keyY[i]));
                }
            }
        }

        return best == Integer.MAX_VALUE ? 0 : best;
    }

    private int shortestEnergy(int sx, int sy, int tx, int ty) {
        if (sx == tx && sy == ty) {
            return 0;
        }

        int[][] dist = new int[height][width];

        for (int[] row : dist) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        PriorityQueue<int[]> pq = new PriorityQueue<>(
                Comparator.comparingInt(a -> a[2]));

        dist[sy][sx] = 0;
        pq.add(new int[]{sx, sy, 0});

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!pq.isEmpty()) {
            int[] current = pq.poll();

            int x = current[0];
            int y = current[1];
            int cost = current[2];

            if (cost != dist[y][x]) {
                continue;
            }

            if (x == tx && y == ty) {
                return cost;
            }

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (!inside(nx, ny) || grid[ny][nx] == 0) {
                    continue;
                }

                // Keep the heuristic consistent with the current
                // movement cost used in successors().
                int stepCost = grid[ny][nx];
                int nextCost = cost + stepCost;

                if (nextCost < dist[ny][nx]) {
                    dist[ny][nx] = nextCost;
                    pq.add(new int[]{nx, ny, nextCost});
                }
            }
        }

        return Integer.MAX_VALUE;
    }
}
