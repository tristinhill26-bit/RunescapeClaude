using System;
using System.Collections.Generic;
using System.Reflection;
using HarmonyLib;
using UnityEngine;

namespace ItemDrawers;

// A drawer had snap points on its front corners and on its back corners. With the depth (0.5) exactly twice the
// distance from its centre to either face, a drawer placed against another one finds front-to-front, front-to-back
// and back-to-back pairs at the same distance, so it often lands half a depth in front of or behind its neighbour:
// a wall of drawers whose rows stick out by a quarter metre and look skewed. Keeping only the back corners leaves one
// way to snap, so neighbours always end up flush. Drawer.Resize keeps them at the back (it reuses the sign of z).
[HarmonyPatch]
internal static class DrawerPrefabs_BuildDrawer_Patch
{
	private static MethodBase TargetMethod()
	{
		return AccessTools.Method(typeof(DrawerPrefabs), "BuildDrawer");
	}

	private static void Postfix(GameObject __result)
	{
		try
		{
			if (__result == null)
			{
				return;
			}
			List<GameObject> front = new List<GameObject>();
			foreach (Transform child in __result.transform)
			{
				if (child.name == "_snappoint" && child.localPosition.z > 0f)
				{
					front.Add(child.gameObject);
				}
			}
			foreach (GameObject snap in front)
			{
				UnityEngine.Object.DestroyImmediate(snap);
			}
		}
		catch (Exception ex)
		{
			ItemDrawersPlugin.Log.LogError("Removing the drawer's front snap points failed: " + ex);
		}
	}
}
