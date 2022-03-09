package dev.felnull.katyouvotifier.event;

import com.vexsoftware.votifier.model.Vote;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

public class VotifierEvent extends Event {
    private final Vote vote;

    public VotifierEvent(Vote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }
}