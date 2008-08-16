package data;

import sim.exceptions.ObstructInitCallException;
import util.Logger;
import util.Logger.LogLevel;
import data.mod.AUserComponent;
import data.mod.IUserLink;
import data.mod.IUserNode;
import data.mod.IUserPacket;
import data.sim.ISimPacketCon;
import data.var.AVariable;
import data.var.AVariable.COMMON_SCOPES;

/**
 * abstract user packet represents a packet by a user;
 * 
 * module writers should use this class to write their own packet
 * 
 * @author dominik
 * 
 */
public abstract class AUserPacket extends AUserComponent implements IUserPacket {

    protected ISimPacketCon simulatorComponent;

    public void init(ISimPacketCon simulatorComponent)
	    throws ObstructInitCallException {
	if (this.simulatorComponent != null) {
	    Logger
		    .output(LogLevel.ERROR, this, "init(" + simulatorComponent
			    + "), but already registered at "
			    + this.simulatorComponent);
	    throw new ObstructInitCallException();
	}
	this.simulatorComponent = simulatorComponent;
    }

    public final IUserNode getSource() {
	return simulatorComponent.getFrom();
    }

    public IUserLink getLinkToSource() {
	return simulatorComponent.getLink();
    }

    public final void execute() {
	// no action at packets :-)
    }

    public String toString() {
	return simulatorComponent.toString();
    }

    public final void interrupt() {
	try {
	    simulatorComponent.interrupt();
	} catch (NullPointerException e) {
	    // nothing
	}
    }

    public final void sleep(int steps) {
	try {
	    simulatorComponent.sleep(steps);
	} catch (NullPointerException e) {
	    // nothing
	}
    }

    public final AVariable getVariable(String identifier) {
	return simulatorComponent.getScopedVariable(COMMON_SCOPES.USER,
						    identifier);
    }

    public final boolean hasVariable(String identifier) {
	return simulatorComponent.hasScopedVariable(COMMON_SCOPES.USER,
						    identifier);
    }
}
