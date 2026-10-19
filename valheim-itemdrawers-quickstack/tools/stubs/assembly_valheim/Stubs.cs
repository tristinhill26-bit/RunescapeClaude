// Reference-only stand-ins: only the members the compat code touches, with the game's exact signatures.
using System.Collections.Generic;
using UnityEngine;
public class Character : MonoBehaviour { public bool IsTeleporting() { throw null; } }
public class Humanoid : Character { public Inventory GetInventory() { throw null; } }
public class Player : Humanoid { public static Player m_localPlayer; public long GetPlayerID() { throw null; } }
public class Inventory {
  public List<ItemDrop.ItemData> GetAllItems() { throw null; }
  public bool RemoveItem(ItemDrop.ItemData item, int amount) { throw null; }
  public int GetHeight() { throw null; }
  public int GetWidth() { throw null; } }
public class ItemDrop : MonoBehaviour { public class ItemData { public GameObject m_dropPrefab; public int m_stack; } }
public class Container : MonoBehaviour { }
public class InventoryGui : MonoBehaviour { public static InventoryGui instance { get { throw null; } } public Container m_currentContainer; }
