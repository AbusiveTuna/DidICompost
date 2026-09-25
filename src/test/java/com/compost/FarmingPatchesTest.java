package com.compost;

import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class FarmingPatchesTest
{
	@Test
	public void compostVarbitsAreUnique()
	{
		Set<Integer> seen = new HashSet<>();
		for (FarmingPatches patch : FarmingPatches.values())
		{
			assertTrue(patch + " shares its compost varbit", seen.add(patch.getCompostVarbit()));
		}
	}
}
