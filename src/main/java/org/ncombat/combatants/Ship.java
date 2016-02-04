package org.ncombat.combatants;

import org.ncombat.MotionComputer;
import org.ncombat.ShipShieldArray;
import org.ncombat.command.AccelerateCommand;
import org.ncombat.command.LaserCommand;
import org.ncombat.command.MissileCommand;
import org.ncombat.command.RepairCommand;
import org.ncombat.command.RotateCommand;
import org.ncombat.command.ShieldCommand;
import org.ncombat.utils.NcombatMath;

public abstract class Ship extends Combatant
{
	// The energy cost of one unit of acceleration for one second.
	private static final double ENERGY_ACCEL_COST = 50.0;
	
	// The energy cost of cooling the engine for each second it
	// operates at a heat level above the danger point (in energy
	// units per second).
	private static final double ENERGY_COOLING_COST = 50.0;
	
	// The damage inflicted to a ship when engine blowout occurs (in %).
	public static final double ENGINE_BLOWOUT_DAMAGE = 10.0;
	
	// Rate at which engine cools down (in degrees per second not running).
	private static final double ENGINE_COOLING_RATE = 20.0;
	
	// Heat level at which it begins to cost extra energy to cool 
	// the engine (in degrees).
	private static final double ENGINE_DANGER_HEAT = 5000.0;
	
	// Rate at which engine heats up (in degrees per unit of acceleration per second).
	private static final double ENGINE_HEATING_RATE = 50.0;
	
	// Heat level at which the engine is destroyed (in degrees).
	protected static final double ENGINE_MAX_HEAT = 8000.0;
	
	protected static final double INITIAL_ENERGY = 30000;
	
	protected static final int INITIAL_NUM_MISSILES = 25;
	
	// The maximum angular deviation within which a laser attack is effective (in degrees).
	protected static final double MAX_LASER_AZIMUTH = 1.0;
	
	// The maximum angular deviation within which a missile attack is effective (in degrees).
	protected static final double MISSILE_MAX_AZIMUTH = 5.0;
	
	// The maximum range from which a missile attack may be launched (in kilometers).
	protected static final double MISSILE_MAX_RANGE = 20000.0;

	// The time required to reload a missile tube after firing a missile (in seconds).
	protected static final double MISSILE_RELOAD_TIME = 60.0;
	
	protected static final int NUM_MISSILE_TUBES = 2;
	
	private double accelRate;
	
	private double accelTime;
	private boolean engineBlown;
	
	protected double engineHeat;
	protected double heading;
	
	protected double laserCoolingTime;
	
	protected final int numMissileTubes = NUM_MISSILE_TUBES;
	protected int numMissiles = INITIAL_NUM_MISSILES;
	protected final double[] missileLoadTime = new double[numMissileTubes];
	
	
	
	private double rotationRate;
	private double rotationTime;
	
	public Ship(String commander) {
		super(commander);
		this.energy = INITIAL_ENERGY;
		this.shields = new ShipShieldArray();
	}
	

	public double azimuth(Combatant combatant) {
		return NcombatMath.degreeAzimuth(this.position, heading, combatant.position);
	}
	
	
	public double course() {
		return -NcombatMath.degreeCourse(velocity, heading);
	}
	
	public double course(Ship ship) {
		return NcombatMath.degreeCourse(this.position, ship.position, ship.velocity);
	}
	
	public double getAccelRate() {
		return accelRate;
	}
	
	public double getAccelTime() {
		return accelTime;
	}
	
	protected String getCombatantStatus() {
		return super.getStatus();
	}
	
	public double getHeading() {
		return heading;
	}

	public double getRotationRate() {
		return rotationRate;
	}
	
	public double getRotationTime() {
		return rotationTime;
	}
	
	public boolean laserReady() {
		return (laserCoolingTime <= 0.0);
	}
	
	public boolean missileReady()
	{
		if (numMissiles < 1) return false;
		
		for (int i = 0 ; i < numMissileTubes ; i++) {
			if (missileLoadTime[i] <= 0.0) {
				return true;
			}
		}
		
		return false;
	}
	
	private void moveTheShip(double intervalLen)
	{
		MotionComputer.Request motionRequest = new MotionComputer.Request();
		
		motionRequest.intervalLength = intervalLen;
		motionRequest.initialPosition = position;
		motionRequest.initialVelocity = velocity;
		motionRequest.initialHeading = heading;
		motionRequest.accelRate = accelRate;
		motionRequest.accelTime = accelTime;
		motionRequest.rotationRate = rotationRate;
		motionRequest.rotationTime = rotationTime;
		
		MotionComputer.Response motionResponse = MotionComputer.compute(motionRequest);
		
		position = motionResponse.finalPosition;
		velocity = motionResponse.finalVelocity;
		heading = motionResponse.finalHeading;
		accelTime = motionResponse.accelTimeLeft;
		if (accelTime == 0.0) accelRate = 0.0;
		rotationTime = motionResponse.rotationTimeLeft;
		if (rotationTime == 0.0) rotationRate = 0.0;
	}

	protected void onEnergyExhaustion() {
		markExhausted();
	}
	
	protected void onEngineBlowout()
	{
		engineBlown = true;
		accelRate = 0.0;
		accelTime = 0.0;
		addDamage(10.0);
		addMessage("*BANG* Ships engine just blew up - Impulse power only.");
		if (!alive) {
			markDestroyedByEngineOverload();
		}
	}

	@Override
	protected AttackResult onLaserHit(Combatant attacker, double power)
	{
		setLastAttacker(attacker);
		
		int shieldHit = shields.coveringShield( azimuth(attacker));
		double shieldPower = shields.getEffectivePower(shieldHit);
		double range = range(attacker);
		
		double coeffDamage = (power * 12.0 * (33.0 - shieldPower)) / range / 2.0;
		double shipDamage = addDamage(coeffDamage);
		shields.addDamage(shieldHit, coeffDamage);
		
		String exclamation = (shipDamage > 20.0 ? "**BLAM**" : ">>PWANG<<");
		String fmt = "%s Ship %d laser hit shield %d caused %d%% damage.";
		addMessage( String.format(fmt, exclamation, attacker.getShipNumber(), 
										shieldHit, (int) shipDamage));
		
		AttackResult results = new AttackResult();
		results.shieldHit = shieldHit;
		results.damage = shipDamage;
		
		return results;
	}

	@Override
	protected AttackResult onMissileHit(Combatant attacker)
	{
		setLastAttacker(attacker);
		
		int shieldHit = shields.coveringShield( azimuth(attacker));
		double shieldPower = shields.getEffectivePower(shieldHit);
		
		double coeffDamage = 102.0 - (shieldPower * 4.0);
		double shipDamage = addDamage(coeffDamage);
		shields.addDamage(shieldHit, coeffDamage);
		
		String exclamation = (shipDamage > 20.0 ? "**BLAM**" : ">>PWANG<<");
		String fmt = "%s Ship %d missile hit shield %d caused %d%% damage.";
		addMessage( String.format(fmt, exclamation, attacker.getShipNumber(),
										shieldHit, (int) shipDamage));
		
		AttackResult results = new AttackResult();
		results.shieldHit = shieldHit;
		results.damage = shipDamage;
		
		return results;
	}

	public void processAccelerateCommand(AccelerateCommand cmd)
	{
		if (engineBlown) {
			addMessage("Engine blown - cannot accelerate.");
			return;
		}
		accelRate = cmd.getRate();
		accelTime = cmd.getTime();
	}
	
	@Override
	public void processKill(Combatant killed)
	{
		super.processKill(killed);
		
		this.energy += killed.energy;
		
		if ((this.numKills % 5) == 0) {
			this.numMissiles = INITIAL_NUM_MISSILES;
		}
	}
	
	public void processLaserCommand(LaserCommand cmd)
	{
		if (!laserReady()) {
			addMessage("Laser too hot to fire.");
			return;
		}
		
		// We limit our firing power to remaining energy.
		double power = Math.min( cmd.getPower(), energy);
		
		energy -= power;
		laserCoolingTime = power/40 + 10;
		
		for (Combatant combatant : gameServer.getCombatants()) {
			if ( Math.abs( azimuth(combatant)) <= MAX_LASER_AZIMUTH) {
				AttackResult result = combatant.onLaserHit(this, power);
				
				String fmt = "Laser hit on shield %d of ship %2d caused %3d%% damage.";
				addMessage( String.format(fmt, result.shieldHit, 
								combatant.getShipNumber(), (int) result.damage));
				
				if (!combatant.isAlive()) {
					processKill(combatant);
					combatant.markDestroyed(this);
				}
			}
		}
		
		if (energy <= 0) {
			markExhausted();
		}
	}
	
	public void processMissileCommand(MissileCommand cmd)
	{
		if (numMissiles < 1) {
			addMessage("No missiles remaining.");
			return;
		}
		
		// Determine the missile tube from which we will fire.
		int tube = -1;
		for (int i = 0 ; i < numMissileTubes ; i++) {
			if (missileLoadTime[i] <= 0.0) {
				tube = i;
				break;
			}
		}
		if (tube == -1) {
			addMessage("No missile tubes available.");
			return;
		}
		
		int shipNum = cmd.getTarget();
		Combatant target = gameServer.getCombatant(shipNum);
		if (target == null) {
			return;
		}
		
		if ( range(target) > MISSILE_MAX_RANGE) return;
		
		if ( Math.abs( azimuth(target)) > MISSILE_MAX_AZIMUTH) return;

		numMissiles--;
		missileLoadTime[tube] = MISSILE_RELOAD_TIME;
		
		AttackResult result = target.onMissileHit(this);
				
		String fmt = "Missile hit on shield %d of ship %2d caused %3d%% damage.";
		addMessage( String.format(fmt, result.shieldHit, 
						target.getShipNumber(), (int) result.damage));
		
		if (!target.isAlive()) {
			processKill(target);
			target.markDestroyed(this);
		}
	}

	public void processRepairCommand(RepairCommand cmd)
	{
		double s1rate = cmd.getShieldRepair1Pct();
		double s2rate = cmd.getShieldRepair2Pct();
		
		repairRate = 1.5 - s1rate - s2rate;
		
		shields.setRepairRate(1, s1rate);
		shields.setRepairRate(2, s2rate);
	}

	public void processRotateCommand(RotateCommand cmd)
	{
		rotationRate = - Math.toRadians(cmd.getRate()) * Math.signum(cmd.getAngle());
		rotationTime = Math.abs( cmd.getAngle()) / cmd.getRate();
	}

	public void processShieldCommand(ShieldCommand cmd) {
		shields.setPower( cmd.getShieldNum(), cmd.getPower());
	}

	protected void repairShip(double intervalLen)
	{
		if (damage > 0.0) {
			double waitTime = Math.min(repairWaitTime, intervalLen);
			double maxRepairTime = intervalLen - waitTime;
			double maxRepair = maxRepairTime * repairRate;
			
			damage = Math.max(damage - maxRepair, 0.0);
			
			repairWaitTime -= waitTime;
		}
	}

	public void setHeading(double heading) {
		this.heading = heading;
	}

	@Override
	public void update(long updateTime)
	{
		double intervalLen = ((double)(updateTime - getLastUpdateTime())) / 1000.0;
		
		// Here we regenerate the things that improve with time.  The way we do this is
		// not STRICTLY correct, as we ought to make sure we don't die in the middle of
		// the interval before regenerating resources for the entire interval.  In the
		// spirit of sportsmanship and readable coding, however, we're giving it up
		// all at once at the beginning of the interval.
		
		repairShip(intervalLen);
		
		shields.repair(intervalLen);
		
		if (laserCoolingTime > 0.0) {
			laserCoolingTime = Math.max(laserCoolingTime - intervalLen, 0.0);
		}
		
		for (int i = 0 ; i < numMissileTubes ; i++) {
			missileLoadTime[i] = Math.max( missileLoadTime[i] - intervalLen, 0.0);
		}
		
		if (accelTime == 0.0) accelRate = 0.0;
		if (accelRate == 0.0) accelTime = 0.0;
		
		/*
		 * To simplify the logic of dealing with engine heat and energy, we will
		 * subdivide our time interval into sub-intervals for which all of the
		 * following are true for each and every sub-interval:
		 * 
		 * 1) We are either accelerating or not for the entire sub-interval.
		 * 2) Our engine is either blown or not for the entire sub-interval.
		 * 3) Engine temperature is either in the danger zone of not for the entire
		 *    sub-interval.
		 */
		for (double timeLeft = intervalLen ; timeLeft > 0.0 ; )
		{
			double subIntervalLen = intervalLen;
			
			double absAccelRate = Math.abs(accelRate);
			
			if (accelTime > 0.0) {
				if (accelTime < subIntervalLen) {
					subIntervalLen = accelTime;
				}
				if (!engineBlown) {
					if (engineHeat < ENGINE_DANGER_HEAT) {
						double tDanger = (ENGINE_DANGER_HEAT - engineHeat) / ENGINE_HEATING_RATE / absAccelRate;
						if (tDanger < subIntervalLen) {
							subIntervalLen = tDanger;
						}
					}
					else {
						double tBlow = (ENGINE_MAX_HEAT - engineHeat) / ENGINE_HEATING_RATE / absAccelRate;
						if (tBlow < subIntervalLen) {
							subIntervalLen = tBlow;
						}
					}
				}
			}
			else {
				if (engineHeat > ENGINE_DANGER_HEAT) {
					double tSafe = (engineHeat - ENGINE_DANGER_HEAT) / ENGINE_COOLING_RATE;
					if (tSafe < subIntervalLen) {
						subIntervalLen = tSafe; 
					}
				}
			}
			
			/*
			 * Now we determine the change in the ship's kinematic state.
			 */
			
			double initAccelTime = accelTime;
			
			moveTheShip(subIntervalLen);
			
			/*
			 * Finally we update energy and engine heat, allowing for the possibilities of
			 * engine overheat and energy exhaustion.
			 */
			
			if (initAccelTime > 0.0) {
				// This was an accelerating sub-interval.
				
				energy -= ENERGY_ACCEL_COST * absAccelRate * subIntervalLen;

				if (engineHeat >= ENGINE_DANGER_HEAT) {
					energy -= ENERGY_COOLING_COST * subIntervalLen;
				}
				
				engineHeat += ENGINE_HEATING_RATE * absAccelRate * subIntervalLen;
				
				if (engineHeat >= ENGINE_MAX_HEAT) {
					onEngineBlowout();
					if (!alive) {
						return;
					}
				}
			}
			else {
				// This was a non-accelerating sub-interval.
				
				if (engineHeat > ENGINE_DANGER_HEAT) {
					energy -= ENERGY_COOLING_COST * subIntervalLen;
				}
				
				engineHeat = Math.max(engineHeat - ENGINE_COOLING_RATE * subIntervalLen, 0.0);
			}
			
			energy -= subIntervalLen * shields.getTotalPower();
			
			if (energy <= 0.0) {
				onEnergyExhaustion();
				if (!alive) {
					return;
				}
			}

			timeLeft -= subIntervalLen;
		}
	}
}
