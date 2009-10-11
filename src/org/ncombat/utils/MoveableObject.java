package org.ncombat.utils;
	
	public abstract class MoveableObject {

		// momentum vector = which way you are moving and how fast - determines position
		// does not update from commands - updates from product of itself and thrust vector
		private Vector momentumVector;
		
		// thrust vector = which way you are pointing (course) and how much engine power you have on
		// updates from commands
		private Vector thrustVector;
		
		// position in universe
		private double posX;
		private double posY;
		
		// remaining time for burn and rotation
		// setter only allows requests, not direct manipulation
		private double burnTime;
		private double rotateTime;
		
		
	/**
		 * @param args
		 */
		public static void main(String[] args) {
			// TODO Auto-generated method stub

		}


	public MoveableObject(
			String guid, 
			Vector initialMomentum, 
			Vector initialThrust,
			double[] initialPos) {

			if (initialMomentum != null) 
				{momentumVector = initialMomentum;}
			else {momentumVector = new Vector(0,0);}
		
		if (initialThrust != null) 
			{thrustVector = initialThrust;}
		else {thrustVector = new Vector(0,0);}
		//p = thePilot;
		//myServer.logEntry("Created new Moveable Object, guid " + guid + " pilot " + p.getPilotName());
	 
		if (initialPos != null) 
		{posX = initialPos[0]; posY=initialPos[1];}
		else {
				posX = NCombatUtils.r.nextInt(NCombatUtils.ARENA_SIZE); 
				posY= posX = NCombatUtils.r.nextInt(NCombatUtils.ARENA_SIZE);
			}
			
	}

	public void setCourse (int degrees){
		// find minimum difference between current heading (from thrust vector) and desired heading = total rotation
		// 15 degrees to 355 degrees is 20 degress negative rotation, not 360 positive
		
		// break down into segments = (total rotation) / max turn rate
		
		
		// find remaining rotation
		// if greater than max turn rate, remove max turn rate degrees of rotation
	}



	private void updateStatus(){
		// apply motion = thrust vector + momentum vector determine new x,y
		
		// update thrust vector with rotation and burn remaining
		if (0==0);
		
	}

	public String getStatus() {
		return null;
	}


	}

