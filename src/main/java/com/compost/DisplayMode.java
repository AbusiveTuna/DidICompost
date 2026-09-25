package com.compost;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisplayMode
{
	ICON("Icon", true, false),
	SOIL("Soil colour", false, true),
	BOTH("Both", true, true);

	private final String name;
	private final boolean icon;
	private final boolean soil;

	@Override
	public String toString()
	{
		return name;
	}
}
