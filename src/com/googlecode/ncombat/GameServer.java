package com.googlecode.ncombat;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class GameServer
{
	public static final long DEFAULT_CYCLE_PERIOD = 1000; // milliseconds
	
	private static int nextServerNumber = 1;
	
	private Logger log = Logger.getLogger(GameServer.class);
	
	private int serverNumber;
	
	private Timer timer;
	
	private GameServerTimerTask timerTask;
	
	private long cyclePeriod = DEFAULT_CYCLE_PERIOD;
	
	private boolean started;
	
	//------------------------------------------------------------
	// The following variables are protected by the cycle monitor.
	//------------------------------------------------------------
	
	private Object cycleMonitor = new Object();
	
	private List<Combatant> combatants = new ArrayList<Combatant>();
	
	//-------------------------------------------------
	// End of variables protected by the cycle monitor.
	//-------------------------------------------------
	
	public GameServer()
	{
		synchronized (GameServer.class) {
			this.serverNumber = nextServerNumber++;
		}
		
		log.info("Game server #" + serverNumber + " is starting.");

		timer = new Timer("GameServer" + serverNumber, true);
		timerTask = new GameServerTimerTask();
	}
	
	public void addCombatant(Combatant combatant)
	{
		combatant.setGameServer(this);
		synchronized (cycleMonitor) {
			combatants.add(combatant);
		}
	}
	
	public synchronized void start()
	{
		if (started) return;
		timer.schedule(timerTask, cyclePeriod, cyclePeriod);
		started = true;
	}
	
	public synchronized void stop()
	{
		if (!started) return;
		timerTask.cancel();
		started = false;
	}
	
	public long getCyclePeriod() {
		return cyclePeriod;
	}

	public void setCyclePeriod(long cyclePeriod) {
		this.cyclePeriod = cyclePeriod;
	}
	
	private void runGameCycle()
	{
		log.debug("Entering game cycle.");
		log.debug("Exiting game cycle.");
	}
	
	public class GameServerTimerTask extends TimerTask
	{
		@Override
		public void run() {
			runGameCycle();
		}
	}
}
