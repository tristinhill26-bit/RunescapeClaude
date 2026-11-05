package com.fairyringlabels;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DestinationStyle
{
	SHORT("Short (Farming Guild)"),
	FULL("Full (North-east of the Farming Guild)");

	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
