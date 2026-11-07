#!/usr/bin/env bash
# Rebuilds dist/ItemDrawers.dll from an original ItemDrawers.dll and the files in src/,
# without the game installed: the compat code is compiled against small stand-ins for assembly_valheim
# and BepInEx, then copied into the original dll with Mono.Cecil. Needs the .NET 8 SDK and nuget.org.
#
#   tools/build.sh <original ItemDrawers.dll> <QuickStackStore.dll>
set -euo pipefail
ORIGINAL=$(realpath "$1")
QSS=$(realpath "$2")
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(dirname "$HERE")
WORK=$(mktemp -d)
export DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1

fetch() { # id version
  curl -sSL -o "$WORK/$1.nupkg" "https://api.nuget.org/v3-flatcontainer/$1/$2/$1.$2.nupkg"
  mkdir -p "$WORK/$1" && (cd "$WORK/$1" && unzip -q -o "../$1.nupkg")
}
fetch unityengine.modules 2021.3.33
fetch harmonyx 2.9.0
UE=$WORK/unityengine.modules/lib/netstandard2.0/UnityEngine.CoreModule.dll
HX=$WORK/harmonyx/lib/netstandard2.0/0Harmony.dll
NETSTD=$(dirname "$(find / -path /proc -prune -o -name netstandard.dll -path '*NETStandard.Library.Ref*netstandard2.1*' -print -quit 2>/dev/null)")

stub() { # assembly-name version source-dir
  mkdir -p "$WORK/$1" && cp "$3/Stubs.cs" "$WORK/$1/"
  cat > "$WORK/$1/$1.csproj" <<EOF
<Project Sdk="Microsoft.NET.Sdk"><PropertyGroup><TargetFramework>netstandard2.1</TargetFramework><AssemblyName>$1</AssemblyName><Version>$2</Version><Nullable>disable</Nullable></PropertyGroup>
<ItemGroup><Reference Include="UnityEngine.CoreModule"><HintPath>$UE</HintPath></Reference></ItemGroup></Project>
EOF
  dotnet build "$WORK/$1" -c Release -o "$WORK/$1/out" -v q
}
stub assembly_valheim 0.0.0.0 "$HERE/stubs/assembly_valheim"
stub BepInEx 5.4.23.5 "$HERE/stubs/BepInEx"
DIRS="$WORK/assembly_valheim/out $WORK/BepInEx/out $(dirname "$HX") $(dirname "$UE") $NETSTD"

dotnet build "$HERE/patcher" -c Release -o "$WORK/patcher" -v q
dotnet "$WORK/patcher/patcher.dll" publicize "$ORIGINAL" "$WORK/ItemDrawers.dll" $DIRS

mkdir -p "$WORK/compat"
cat > "$WORK/compat/compat.csproj" <<EOF
<Project Sdk="Microsoft.NET.Sdk"><PropertyGroup><TargetFramework>netstandard2.1</TargetFramework><AssemblyName>ItemDrawersQuickStack</AssemblyName><LangVersion>11.0</LangVersion><Nullable>disable</Nullable><ImplicitUsings>disable</ImplicitUsings><DebugType>none</DebugType></PropertyGroup>
<ItemGroup>
<Compile Include="$ROOT/src/*.cs" />
<Reference Include="UnityEngine.CoreModule"><HintPath>$UE</HintPath></Reference>
<Reference Include="0Harmony"><HintPath>$HX</HintPath></Reference>
<Reference Include="assembly_valheim"><HintPath>$WORK/assembly_valheim/out/assembly_valheim.dll</HintPath></Reference>
<Reference Include="BepInEx"><HintPath>$WORK/BepInEx/out/BepInEx.dll</HintPath></Reference>
<Reference Include="ItemDrawers"><HintPath>$WORK/ItemDrawers.dll</HintPath></Reference>
</ItemGroup></Project>
EOF
dotnet build "$WORK/compat" -c Release -o "$WORK/compat/out" -v q

mkdir -p "$ROOT/dist"
rm -f "$ROOT/dist/ItemDrawers.dll"
dotnet "$WORK/patcher/patcher.dll" merge "$ORIGINAL" "$WORK/compat/out/ItemDrawersQuickStack.dll" "$ROOT/dist/ItemDrawers.dll" $DIRS
# The stand-ins prove nothing by themselves: every game and BepInEx member the result uses must already be used
# by the original dll or by Quick Stack Store, both known to work in game.
dotnet "$WORK/patcher/patcher.dll" verify "$ROOT/dist/ItemDrawers.dll" "$ORIGINAL" "$QSS" -- $DIRS | grep -E 'BAD|verified'
rm -rf "$WORK"
