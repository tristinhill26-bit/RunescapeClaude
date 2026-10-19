using System;
using UnityEngine;
namespace BepInEx { public abstract class BaseUnityPlugin : MonoBehaviour { } }
namespace BepInEx.Configuration {
  public abstract class ConfigEntryBase { public object BoxedValue { get { throw null; } set { } } }
  public sealed class ConfigEntry<T> : ConfigEntryBase { public T Value { get { throw null; } set { } } }
  public abstract class AcceptableValueBase { }
  public class AcceptableValueRange<T> : AcceptableValueBase where T : IComparable { public AcceptableValueRange(T minValue, T maxValue) { } }
  public class ConfigDescription { public ConfigDescription(string description, AcceptableValueBase acceptableValues = null, params object[] tags) { } }
  public class ConfigFile {
    public ConfigEntry<T> Bind<T>(string section, string key, T defaultValue, string description) { throw null; }
    public ConfigEntry<T> Bind<T>(string section, string key, T defaultValue, ConfigDescription configDescription = null) { throw null; } } }
namespace BepInEx.Logging { public class ManualLogSource { public void LogInfo(object data) { } public void LogWarning(object data) { } public void LogError(object data) { } } }
namespace BepInEx {
  public class BepInDependency : Attribute { public enum DependencyFlags { HardDependency = 1, SoftDependency = 2 } public BepInDependency(string DependencyGUID, DependencyFlags Flags = DependencyFlags.HardDependency) { } }
  public class BepInPlugin : Attribute { public BepInPlugin(string GUID, string Name, string Version) { } }
  public class BepInProcess : Attribute { public BepInProcess(string ProcessName) { } }
  public class BepInIncompatibility : Attribute { public BepInIncompatibility(string IncompatibilityGUID) { } } }
