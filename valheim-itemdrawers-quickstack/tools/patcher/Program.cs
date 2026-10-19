#nullable disable
// Small Cecil tool:
//   publicize <in.dll> <out.dll>                       reference copy with every type/member public (compile only)
//   merge <ItemDrawers.dll> <compat.dll> <out.dll> <searchDir...>
//                                                      copies the compat types into ItemDrawers and adds a soft
//                                                      BepInDependency on Quick Stack Store (load order)
//   verify <merged.dll> <knownGood.dll...> -- <searchDir...>
//                                                      every game/BepInEx member the merged dll uses must already be
//                                                      used by a dll known to work in game; others must resolve
using Mono.Cecil;
using Mono.Cecil.Cil;

static class Program
{
    static int Main(string[] args)
    {
        switch (args[0])
        {
            case "publicize": Publicize(args[1], args[2], args.Skip(3).ToArray()); return 0;
            case "merge": Merge(args[1], args[2], args[3], args.Skip(4).ToArray()); return 0;
            case "verify": return Verify(args);
        }
        return 1;
    }

    static DefaultAssemblyResolver Resolver(IEnumerable<string> dirs)
    {
        var r = new DefaultAssemblyResolver();
        foreach (var d in r.GetSearchDirectories()) r.RemoveSearchDirectory(d);
        foreach (var d in dirs) r.AddSearchDirectory(d);
        return r;
    }

    static void Publicize(string input, string output, string[] dirs)
    {
        var m = ModuleDefinition.ReadModule(input, new ReaderParameters { ReadingMode = ReadingMode.Immediate, InMemory = true, AssemblyResolver = Resolver(dirs) });
        foreach (var t in m.GetTypes())
        {
            if (t.IsNested) t.IsNestedPublic = true; else t.IsPublic = true;
            foreach (var f in t.Fields) f.IsPublic = true;
            foreach (var me in t.Methods) me.IsPublic = true;
        }
        m.Write(output);
    }

    // ---------------------------------------------------------------- merge
    static ModuleDefinition _target, _src;
    static readonly Dictionary<TypeDefinition, TypeDefinition> Types = new();
    static readonly Dictionary<MethodDefinition, MethodDefinition> Methods = new();
    static readonly Dictionary<FieldDefinition, FieldDefinition> Fields = new();

    static void Merge(string targetPath, string srcPath, string output, string[] dirs)
    {
        var rp = new ReaderParameters { ReadingMode = ReadingMode.Immediate, InMemory = true, AssemblyResolver = Resolver(dirs.Append(Path.GetDirectoryName(Path.GetFullPath(targetPath)))) };
        _target = ModuleDefinition.ReadModule(targetPath, rp);
        _src = ModuleDefinition.ReadModule(srcPath, rp);
        var refsBefore = _target.AssemblyReferences.Select(a => a.FullName).ToList();

        var srcTypes = _src.Types.Where(t => t.Name != "<Module>" && t.Namespace != "Microsoft.CodeAnalysis" && t.Namespace != "System.Runtime.CompilerServices").ToList();
        foreach (var t in srcTypes)
        {
            if (t.HasNestedTypes || t.HasGenericParameters || t.HasInterfaces || t.HasEvents)
                throw new Exception("Unsupported shape: " + t.FullName);
            if (_target.GetType(t.FullName) != null) throw new Exception("Already present: " + t.FullName);
            Console.WriteLine("type " + t.FullName);
            var nt = new TypeDefinition(t.Namespace, t.Name, t.Attributes);
            _target.Types.Add(nt);
            Types[t] = nt;
        }
        foreach (var t in srcTypes)
        {
            var nt = Types[t];
            nt.BaseType = T(t.BaseType);
            foreach (var f in t.Fields)
            {
                var nf = new FieldDefinition(f.Name, f.Attributes, T(f.FieldType));
                if (f.HasConstant) nf.Constant = f.Constant;
                nt.Fields.Add(nf);
                Fields[f] = nf;
            }
            foreach (var me in t.Methods)
            {
                if (me.HasGenericParameters || me.HasOverrides) throw new Exception("Unsupported method: " + me.FullName);
                var nm = new MethodDefinition(me.Name, me.Attributes, T(me.ReturnType)) { ImplAttributes = me.ImplAttributes };
                foreach (var p in me.Parameters)
                {
                    var np = new ParameterDefinition(p.Name, p.Attributes, T(p.ParameterType));
                    if (p.HasConstant) np.Constant = p.Constant;
                    nm.Parameters.Add(np);
                }
                nt.Methods.Add(nm);
                Methods[me] = nm;
            }
            foreach (var p in t.Properties)
            {
                var np = new PropertyDefinition(p.Name, p.Attributes, T(p.PropertyType));
                if (p.GetMethod != null) np.GetMethod = Methods[p.GetMethod];
                if (p.SetMethod != null) np.SetMethod = Methods[p.SetMethod];
                nt.Properties.Add(np);
            }
        }
        foreach (var t in srcTypes)
        {
            CopyAttributes(t, Types[t]);
            foreach (var f in t.Fields) CopyAttributes(f, Fields[f]);
            foreach (var me in t.Methods)
            {
                CopyAttributes(me, Methods[me]);
                if (me.HasBody) CopyBody(me, Methods[me]);
            }
        }

        // Load after Quick Stack Store when it is installed, so its types exist when the patches are resolved.
        var plugin = _target.GetType("ItemDrawers.ItemDrawersPlugin");
        var jotunn = plugin.CustomAttributes.First(c => c.AttributeType.FullName == "BepInEx.BepInDependency");
        var guid = System.Text.Encoding.UTF8.GetBytes("goldenrevolver.quick_stack_store");
        var blob = new List<byte> { 1, 0, (byte)guid.Length };
        blob.AddRange(guid);
        blob.AddRange(new byte[] { 2, 0, 0, 0, 0, 0 });
        plugin.CustomAttributes.Add(new CustomAttribute(jotunn.Constructor, blob.ToArray()));

        var added = _target.AssemblyReferences.Select(a => a.FullName).Except(refsBefore).ToList();
        if (added.Count > 0) throw new Exception("New assembly references: " + string.Join("; ", added));
        _target.Write(output);
        Console.WriteLine("written " + output);
    }

    static bool FromItemDrawers(TypeReference t) => t.Scope is AssemblyNameReference a && a.Name == "ItemDrawers";

    static TypeReference T(TypeReference t)
    {
        if (t == null) return null;
        switch (t)
        {
            case GenericInstanceType g:
                var ng = new GenericInstanceType(T(g.ElementType));
                foreach (var a in g.GenericArguments) ng.GenericArguments.Add(T(a));
                return ng;
            case ArrayType at: return new ArrayType(T(at.ElementType), at.Rank);
            case ByReferenceType br: return new ByReferenceType(T(br.ElementType));
            case GenericParameter: throw new Exception("Generic parameter outside a member signature: " + t.FullName);
        }
        if (t is TypeDefinition td && td.Module == _src) return Types[td];
        if (FromItemDrawers(t))
        {
            var local = FindNested(t);
            return local ?? throw new Exception("Not in ItemDrawers: " + t.FullName);
        }
        if (t.Module == _src && t.Scope == _src) return Types[t.Resolve()];
        return _target.ImportReference(t);
    }

    static TypeDefinition FindNested(TypeReference t)
    {
        if (t.DeclaringType == null) return _target.GetType(t.Namespace, t.Name);
        var outer = FindNested(t.DeclaringType);
        return outer?.NestedTypes.FirstOrDefault(n => n.Name == t.Name);
    }

    static MethodReference M(MethodReference m)
    {
        if (m is MethodDefinition md && md.Module == _src) return Methods[md];
        if (m is GenericInstanceMethod gim)
        {
            var ng = new GenericInstanceMethod(M(gim.ElementMethod));
            foreach (var a in gim.GenericArguments) ng.GenericArguments.Add(T(a));
            return ng;
        }
        var decl = m.DeclaringType;
        if (decl is TypeDefinition dd && dd.Module == _src) return Methods[m.Resolve()];
        if (FromItemDrawers(decl))
        {
            var owner = (TypeDefinition)T(decl);
            var hit = owner.Methods.Where(x => x.Name == m.Name && x.Parameters.Count == m.Parameters.Count
                && x.Parameters.Select(p => p.ParameterType.FullName).SequenceEqual(m.Parameters.Select(p => p.ParameterType.FullName))
                && x.ReturnType.FullName == m.ReturnType.FullName).ToList();
            if (hit.Count != 1) throw new Exception("Method not found in ItemDrawers: " + m.FullName);
            return hit[0];
        }
        if (decl is GenericInstanceType git && git.GenericArguments.Any(a => FromItemDrawers(a) || a.Module == _src && a is TypeDefinition))
            throw new Exception("Generic over local type not supported: " + m.FullName);
        return _target.ImportReference(m);
    }

    static FieldReference F(FieldReference f)
    {
        if (f is FieldDefinition fd && fd.Module == _src) return Fields[fd];
        var decl = f.DeclaringType;
        if (decl is TypeDefinition dd && dd.Module == _src) return Fields[f.Resolve()];
        if (FromItemDrawers(decl))
        {
            var owner = (TypeDefinition)T(decl);
            return owner.Fields.Single(x => x.Name == f.Name);
        }
        return _target.ImportReference(f);
    }

    static void CopyAttributes(ICustomAttributeProvider from, ICustomAttributeProvider to)
    {
        foreach (var ca in from.CustomAttributes)
        {
            var name = ca.AttributeType.FullName;
            if (name.StartsWith("System.Runtime.CompilerServices.") || name.StartsWith("Microsoft.CodeAnalysis."))
            {
                Console.WriteLine("  skip attribute " + name + " on " + from);
                continue;
            }
            to.CustomAttributes.Add(new CustomAttribute(M(ca.Constructor), ca.GetBlob()));
        }
    }

    static void CopyBody(MethodDefinition from, MethodDefinition to)
    {
        var ob = from.Body;
        var nb = to.Body;
        nb.InitLocals = ob.InitLocals;
        foreach (var v in ob.Variables) nb.Variables.Add(new VariableDefinition(T(v.VariableType)));
        var map = new Dictionary<Instruction, Instruction>();
        foreach (var ins in ob.Instructions)
        {
            var ni = Instruction.Create(OpCodes.Nop);
            ni.OpCode = ins.OpCode;
            map[ins] = ni;
            nb.Instructions.Add(ni);
        }
        foreach (var ins in ob.Instructions)
        {
            var ni = map[ins];
            ni.Operand = ins.Operand switch
            {
                null => null,
                Instruction target => map[target],
                Instruction[] targets => targets.Select(x => map[x]).ToArray(),
                VariableDefinition v => nb.Variables[v.Index],
                ParameterDefinition p => p.Index < 0 ? nb.ThisParameter : to.Parameters[p.Index],
                MethodReference mr => M(mr),
                FieldReference fr => F(fr),
                TypeReference tr => T(tr),
                CallSite => throw new Exception("calli not supported"),
                var other => other,
            };
        }
        foreach (var h in ob.ExceptionHandlers)
        {
            nb.ExceptionHandlers.Add(new ExceptionHandler(h.HandlerType)
            {
                TryStart = Map(map, h.TryStart), TryEnd = Map(map, h.TryEnd),
                HandlerStart = Map(map, h.HandlerStart), HandlerEnd = Map(map, h.HandlerEnd),
                FilterStart = Map(map, h.FilterStart), CatchType = T(h.CatchType),
            });
        }
    }

    static Instruction Map(Dictionary<Instruction, Instruction> map, Instruction i) => i == null ? null : map[i];

    // ---------------------------------------------------------------- verify
    static readonly string[] Checked = { "assembly_valheim", "BepInEx", "assembly_utils", "assembly_guiutils" };

    static int Verify(string[] args)
    {
        int sep = Array.IndexOf(args, "--");
        var known = args.Skip(2).Take(sep - 2).ToArray();
        var dirs = args.Skip(sep + 1).ToArray();
        var rp = new ReaderParameters { AssemblyResolver = Resolver(dirs) };
        var merged = ModuleDefinition.ReadModule(args[1], rp);
        var good = new HashSet<string>();
        var goodTypes = new HashSet<string>();
        foreach (var k in known)
        {
            var m = ModuleDefinition.ReadModule(k, rp);
            foreach (var r in m.GetMemberReferences()) good.Add(Scope(r.DeclaringType) + "|" + r.FullName);
            foreach (var r in m.GetTypeReferences()) goodTypes.Add(r.Scope.Name + "|" + r.FullName);
        }
        int bad = 0, ok = 0;
        foreach (var r in merged.GetMemberReferences())
        {
            var scope = Scope(r.DeclaringType);
            string why;
            if (good.Contains(scope + "|" + r.FullName)) { ok++; continue; }
            if (Checked.Contains(scope)) why = "not used by any known-good dll";
            else { try { why = r.Resolve() == null ? "does not resolve" : null; } catch (Exception e) { why = e.Message; } }
            if (why != null) { bad++; Console.WriteLine("BAD  [" + scope + "] " + r.FullName + " : " + why); }
            else { ok++; Console.WriteLine("NEW  [" + scope + "] " + r.FullName + " : resolves"); }
        }
        // A stand-in that gets a member's virtual-ness wrong makes the compiler pick call where the game needs
        // callvirt (Mono: "Bad IL format" on an abstract one). A non-virtual call into the game or BepInEx must be one
        // a known-good dll makes too.
        var goodCalls = new HashSet<string>();
        foreach (var k in known)
            foreach (var t in ModuleDefinition.ReadModule(k, rp).GetTypes())
                foreach (var me in t.Methods.Where(x => x.HasBody))
                    foreach (var ins in me.Body.Instructions.Where(i => i.OpCode == OpCodes.Call && i.Operand is MethodReference))
                        goodCalls.Add(((MethodReference)ins.Operand).FullName);
        foreach (var t in merged.GetTypes())
            foreach (var me in t.Methods.Where(x => x.HasBody))
                foreach (var ins in me.Body.Instructions.Where(i => i.OpCode == OpCodes.Call && i.Operand is MethodReference))
                {
                    var mr = (MethodReference)ins.Operand;
                    if (mr.HasThis && Checked.Contains(Scope(mr.DeclaringType)) && !goodCalls.Contains(mr.FullName))
                    {
                        bad++;
                        Console.WriteLine("BAD  non-virtual call in " + me.FullName + " to " + mr.FullName);
                    }
                }
        foreach (var r in merged.GetTypeReferences())
        {
            if (Checked.Contains(r.Scope.Name) && !goodTypes.Contains(r.Scope.Name + "|" + r.FullName)) { bad++; Console.WriteLine("BAD  type " + r.FullName); }
        }
        Console.WriteLine($"verified {ok} member references, {bad} problems");
        foreach (var a in merged.AssemblyReferences) Console.WriteLine("REF " + a.FullName);
        return bad == 0 ? 0 : 2;
    }

    static string Scope(TypeReference t)
    {
        while (t is TypeSpecification ts) t = ts.ElementType;
        while (t.DeclaringType != null) t = t.DeclaringType;
        return t.Scope.Name;
    }
}
