package com.shrewdify.statsclient;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Processor;

public class StateListener implements ControllerListener {
	private Integer stateLock = new Integer(0);
    private boolean failed = false;

    public Integer getStateLock() {
        return stateLock;
    }

    public void setFailed() {
        failed = true;
    }

	public void controllerUpdate(ControllerEvent ce) {

		// If there was an error during configure or
		// realize, the processor will be closed
		if (ce instanceof ControllerClosedEvent)
			setFailed();

		// All controller events, send a notification
		// to the waiting thread in waitForState method.
		if (ce instanceof ControllerEvent) {
			synchronized (getStateLock()) {
				getStateLock().notifyAll();
			}
		}
	}

    public synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(this);
        failed = false;

        // Call the required method on the processor
        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }

        // Wait until we get an event that confirms the
        // success of the method, or a failure event.
        // See StateListener inner class
        while (p.getState() < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }

        if (failed)
            return false;
        else
            return true;
    }

}

