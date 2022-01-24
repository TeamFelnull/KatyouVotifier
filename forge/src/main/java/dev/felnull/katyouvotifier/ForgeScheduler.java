package dev.felnull.katyouvotifier;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import dev.felnull.katyouvotifier.handler.ServerHandler;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ForgeScheduler implements VotifierScheduler {
    private final MinecraftServer server;

    public ForgeScheduler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ScheduledVotifierTask sync(Runnable runnable) {
        return new ForgeTask(server, runnable, false, 0);
    }

    @Override
    public ScheduledVotifierTask onPool(Runnable runnable) {
        return new ForgeTask(server, runnable, true, 0);
    }

    @Override
    public ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit) {
        return new ForgeTask(server, runnable, false, unit.toMillis(delay));
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new ForgeTask(server, runnable, true, unit.toMillis(delay));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return null;
    }

    public static CompletableFuture<Void> runTask(MinecraftServer server, Runnable runnable, boolean async) {
        if (server != null)
            return async ? server.submitAsync(runnable) : server.submit(runnable);
        return CompletableFuture.completedFuture(null);
    }

    public static class ForgeTask implements ScheduledVotifierTask {
        private final Runnable runnable;
        private final boolean async;
        private final long startTime;
        private final long delay;
        private boolean cancel;
        private CompletableFuture<Void> future;

        private ForgeTask(MinecraftServer server, Runnable runnable, boolean async, long delay) {
            this.runnable = () -> {
                if (!cancel)
                    runnable.run();
            };
            this.async = async;
            this.startTime = System.currentTimeMillis();
            this.delay = delay;

            if (delay == 0) {
                this.future = runTask(server, this.runnable, async);
            } else {
                ServerHandler.tasks.add(this);
            }
        }

        @Override
        public void cancel() {
            cancel = true;
            if (this.future != null)
                this.future.cancel(true);
        }

        public boolean isCancel() {
            return cancel;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDelay() {
            return delay;
        }

        public Runnable getRunnable() {
            return runnable;
        }

        public boolean isAsync() {
            return async;
        }
    }
}
