package modules.bullyElectionAlgorithm;

import util.Logger;
import util.Logger.LogLevel;
import data.AUserNode;
import data.mod.IUserLink;
import data.mod.IUserPacket;

public class BullyElectionAlgorithmNode extends AUserNode {

    /**
     * id of this node, probably unique as a random value out
     * of a quite big pool
     */
    public final double id = Math.random() * Double.MAX_VALUE;

    public Double bully = null;

    public Double pendingBully = null;

    public boolean electionSent = false;

    public boolean gotBully() {
	return bully != null;
    }

    public void execute() {
	if (gotBully()) {
	    // got a bully, we're fine
	} else if (electionSent) {
	    // fine, we waited for some time and no-one claimed being bully, let us be bully :-)
	    bully = getId();
	    // reset electionSent flag
	    electionSent = false;
	    // propagate winning message
	    sendElectionWinPacket();
	} else if (pendingBully != null) {
	    // we were pending for a bully
	    if (gotBully() && pendingBully.equals(bully)) {
		// fine, he is bully, accept
		pendingBully = null;
	    } else {
		// problem, received no bully win packet
		// reset
		bully = null;
		pendingBully = null;
		// send election
		sendElectionPacket();
	    }
	} else {
	    // got no bully, start election
	    sendElectionPacket();
	}
    }

    private void sendElectionPacket() {
	for (IUserLink link : getConnectedLinks())
	    send(new ElectionPacket(getId()), link);
	electionSent = true;
	// wait for some time
	sleep(100);
    }

    private void sendElectionWinPacket() {
	for (IUserLink link : getConnectedLinks())
	    send(new ElectionWinPacket(getId()), link);
    }

    private double getId() {
	return id;
    }

    public void receive(ElectionPacket packet) {
	// someone wants to elect a new bully
	if (getId() < packet.getId()) {
	    // we got smaller id, we're not bully
	    // broadcast election packet
	    for (IUserLink link : getConnectedLinks())
		if (!link.equals(packet.getLinkToSource()))
		    send(new ElectionPacket(packet.getId()), link);
	    // now wait for some time and accept him as bully
	    pendingBully = packet.getId();
	    sleep(200);
	} else {
	    // send election
	    sendElectionPacket();
	}
    }

    public void receive(ElectionWinPacket packet) {
	if (getId() < packet.getId()) {
	    // we got smaller id, accept
	    bully = packet.getId();
	} else {
	    // we got bigger id, broadcast election
	    sendElectionPacket();
	}
    }

    public void receive(IUserPacket packet) {
	if (packet instanceof ElectionPacket) {
	    receive((ElectionPacket) packet);
	} else if (packet instanceof ElectionWinPacket) {
	    receive((ElectionWinPacket) packet);
	} else {
	    Logger
		    .output(LogLevel.ERROR, this,
			    "receive 'unknown' packet from "
				    + packet.getSource());
	}
    }
}
