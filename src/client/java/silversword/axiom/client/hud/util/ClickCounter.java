package silversword.axiom.client.hud.util;

import java.util.ArrayDeque;
import java.util.Queue;

public final class ClickCounter {
    private static final Queue<Long> leftClicks = new ArrayDeque<>();
    private static final Queue<Long> rightClicks = new ArrayDeque<>();
    private static final long MS_IN_SECOND = 1000;

    private ClickCounter() {}

    public static void onLeftClick() {
        leftClicks.add(System.currentTimeMillis());
        cleanOld(leftClicks);
    }

    public static void onRightClick() {
        rightClicks.add(System.currentTimeMillis());
        cleanOld(rightClicks);
    }

    private static void cleanOld(Queue<Long> queue) {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && now - queue.peek() > MS_IN_SECOND) {
            queue.poll();
        }
    }

    public static int getLeftCps() {
        cleanOld(leftClicks);
        return leftClicks.size();
    }

    public static int getRightCps() {
        cleanOld(rightClicks);
        return rightClicks.size();
    }
}