package org.ncombat.command;

public class BriefModeCommand implements Command
{
	public BriefModeCommand() {
	}
	
	@Override
	public String toString() {
		return "[BriefModeCommand]";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (this == o) return true;
		if ( getClass().equals(o.getClass())) return true;
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
