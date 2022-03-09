package dev.felnull.katyouvotifier.handler;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import dev.felnull.katyouvotifier.event.VotifierHooks;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;

public class NuVotifierHandler implements VoteHandler, VotifierPlugin, ForwardedVoteListener {
    private final VotifierScheduler scheduler;
    private final LoggingAdapter loggerAdapter;
    private final Map<String, Key> tokens;
    private final KeyPair keyPair;
    private final boolean debug;

    public NuVotifierHandler(VotifierScheduler scheduler, LoggingAdapter loggerAdapter, Map<String, Key> tokens, KeyPair keyPair, boolean debug) {
        this.scheduler = scheduler;
        this.loggerAdapter = loggerAdapter;
        this.tokens = tokens;
        this.keyPair = keyPair;
        this.debug = debug;
    }

    @Override
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug)
            getPluginLogger().info("Got a " + protocolVersion.humanReadable + " vote record from " + remoteAddress + " -> " + vote);
        VotifierHooks.onVote(vote);
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return loggerAdapter;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onError(Throwable throwable, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                loggerAdapter.error("Vote processed, however an exception occurred with a vote from " + remoteAddress, throwable);
            } else {
                loggerAdapter.error("Unable to process vote from " + remoteAddress, throwable);
            }
        } else if (!alreadyHandledVote) {
            loggerAdapter.error("Unable to process vote from " + remoteAddress);
        }
    }

    @Override
    public void onForward(Vote v) {
        if (debug)
            getPluginLogger().info("Got a forwarded vote -> " + v);
        VotifierHooks.onVote(v);
    }
}