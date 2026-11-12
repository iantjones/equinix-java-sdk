#!/usr/bin/env python3
"""Generator for the Fabric v4 Beta families (fabric.beta.**).

Reads the Fabric v4 4.34.0 OpenAPI spec and emits:
  * model classes / enums for every schema reachable from the Beta operations,
  * the public client interfaces + implementations and the FabricBeta hub,
  * apiParams_FabricBeta.json,
  * WireMock tests (one per family), a model deserialization test, and verbatim example fixtures.
usage: gen.py <spec.json> <repo>
"""
import json, os, re, sys, keyword
sys.path.insert(0, os.path.dirname(__file__))
from ops_table import FAMILIES, OPS

SPEC, REPO = sys.argv[1], sys.argv[2]
s = json.load(open(SPEC))
SCH = s['components']['schemas']
SPEC_VERSION = s['info']['version']
DATE = '2026-11-11'
BASE = 'com.eqixiac.equinix.fabric.beta'
MAIN = os.path.join(REPO, 'src/main/java', *BASE.split('.'))
TEST = os.path.join(REPO, 'src/test/java', *BASE.split('.'))
FIX = os.path.join(REPO, 'src/test/resources/json/fabric/beta')
HEADER = """/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

"""
GEN_NOTE = "Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit."
BETA_DOC = f"<p><strong>Beta</strong>: Fabric v4 {SPEC_VERSION} (spec read {DATE}).</p>"

RENAME = {'Error': 'ApiError'}
# Properties the spec's own examples carry but its schemas do not declare (Fabric v4 4.34.0).
_EX = ' Present in the spec examples but not declared in the schema.'
EXTRA_PROPS = {
    'Asn': {'type': {'type': 'string', 'description': 'ASN type, PUBLIC_ASN or PRIVATE_ASN.' + _EX}},
    'AsnAsset': {'state': {'type': 'string', 'description': 'Asset state. Returned by the live service '
                           '(observed 2026-11-12 on POST /fabric/v4/asns/search) but not declared in the spec.'}},
    'AppLinkAttachedAppDomain': {'changeLog': {'$ref': '#/components/schemas/Changelog', 'description': 'Change log.' + _EX}},
    'AppLinkAttachedAppService': {'changeLog': {'$ref': '#/components/schemas/Changelog', 'description': 'Change log.' + _EX}},
}
JAVA_RESERVED = set(keyword.kwlist) | {'abstract', 'boolean', 'byte', 'case', 'catch', 'char', 'class', 'const', 'default',
    'do', 'double', 'enum', 'extends', 'final', 'finally', 'float', 'goto', 'implements', 'instanceof', 'int', 'interface',
    'long', 'native', 'new', 'package', 'private', 'protected', 'public', 'short', 'static', 'strictfp', 'super', 'switch',
    'synchronized', 'this', 'throw', 'throws', 'transient', 'volatile', 'record', 'var', 'yield', 'void', 'null', 'true', 'false'}


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(text)


def pascal(x):
    parts = re.split(r'[^A-Za-z0-9]+', x)
    return ''.join(p[:1].upper() + p[1:] for p in parts if p)


def jfield(name):
    n = re.sub(r'[^A-Za-z0-9_]', '_', name)
    if n[:1].isdigit():
        n = '_' + n
    if n[:1].isupper() and not (len(n) > 1 and n[1].isupper()):
        n = n[0].lower() + n[1:]
    if n in JAVA_RESERVED:
        n = n + 'Value'
    return n


def doc(text, limit=None):
    if not text:
        return ''
    t = str(text)
    t = t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('@', '&#64;')
    t = t.replace('*/', '*&#47;').replace('{', '&#123;').replace('}', '&#125;')
    t = re.sub(r'\s+', ' ', t).strip()
    if limit and len(t) > limit:
        t = t[:limit].rsplit(' ', 1)[0] + ' ...'
    return t


def resolve(ref):
    parts = ref.split('/')[1:]
    o = s
    for p in parts:
        o = o[p]
    return o


# ---------------------------------------------------------------------------------------------
# Operations
# ---------------------------------------------------------------------------------------------
OPINFO = {}
for p, po in s['paths'].items():
    for m, o in po.items():
        if m in ('get', 'post', 'put', 'patch', 'delete'):
            OPINFO[o['operationId']] = (m.upper(), p, o)


def schema_name(x):
    if x and '$ref' in x:
        return x['$ref'].split('/')[-1]
    return None


def op_details(opid):
    m, p, o = OPINFO[opid]
    body = None
    if 'requestBody' in o:
        rb = o['requestBody']
        if '$ref' in rb:
            rb = resolve(rb['$ref'])
        ct, c = next(iter(rb['content'].items()))
        body = dict(ct=ct, schema=schema_name(c.get('schema')), raw=c.get('schema'), examples=c.get('examples', {}))
    resp = None
    for code, r in o['responses'].items():
        if code.startswith('2'):
            if '$ref' in r:
                r = resolve(r['$ref'])
            if r.get('content'):
                ct, c = next(iter(r['content'].items()))
                resp = dict(code=int(code), ct=ct, schema=schema_name(c.get('schema')), raw=c.get('schema'),
                            examples=c.get('examples', {}))
            else:
                resp = dict(code=int(code), ct=None, schema=None, raw=None, examples={})
            break
    params = []
    for prm in po_params(p, m) + o.get('parameters', []):
        if '$ref' in prm:
            prm = resolve(prm['$ref'])
        params.append(prm)
    return dict(method=m, path=p, op=o, body=body, resp=resp, params=params)


def po_params(p, m):
    return s['paths'][p].get('parameters', [])


def data_item_schema(env_name):
    env = SCH[env_name]
    d = env.get('properties', {}).get('data')
    return schema_name(d.get('items')) if d else None


# ---------------------------------------------------------------------------------------------
# Model generation
# ---------------------------------------------------------------------------------------------
classes = {}   # java name -> dict(kind='class'|'enum', ...)
schema_java = {}  # schema name -> java type expression
setter_classes = set()


def merged_props(sch, seen=None):
    """Union of properties/required across allOf/oneOf/anyOf (lossless read of every variant)."""
    seen = seen or set()
    props, req = {}, []
    if '$ref' in sch:
        n = schema_name(sch)
        if n in seen:
            return props, req
        seen = seen | {n}
        sch = SCH[n]
    for key in ('allOf', 'oneOf', 'anyOf'):
        for sub in sch.get(key, []):
            p2, r2 = merged_props(sub, seen)
            for k, v in p2.items():
                props.setdefault(k, v)
            req += r2
    for k, v in sch.get('properties', {}).items():
        props[k] = v
    # discriminated subtypes (e.g. AsnOwnershipChallengeDetails -> Rpki/Loa): read every variant's fields
    for ref in (sch.get('discriminator') or {}).get('mapping', {}).values():
        p2, _ = merged_props({'$ref': ref}, seen)
        for k, v in p2.items():
            props.setdefault(k, v)
    req += sch.get('required', [])
    return props, req


def is_object_schema(sch):
    return bool(sch.get('properties') or sch.get('allOf') or sch.get('oneOf') or sch.get('anyOf')) \
        and not sch.get('enum')


def enum_const(v, used):
    raw = str(v)
    c = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', raw)
    c = re.sub(r'[^A-Za-z0-9]+', '_', c).strip('_').upper()
    if not c:
        c = 'EMPTY'
    if c[0].isdigit():
        c = 'V_' + c
    if c in ('UNKNOWN',):
        c = c + '_VALUE'
    base, i = c, 2
    while c in used:
        c = f'{base}_{i}'
        i += 1
    used.add(c)
    return c


def gen_enum(name, sch, owner_doc=None):
    if name in classes:
        return name
    vals = sch['enum']
    if not all(isinstance(v, str) for v in vals):
        return None
    classes[name] = dict(kind='enum', values=list(vals), doc=sch.get('description') or owner_doc, schema=None)
    return name


def jtype(sch, ctx, prop, for_request=False):
    """Java type for a schema node. ctx is the owning java class name; prop the property name."""
    if sch is None:
        return 'JsonNode'
    if '$ref' in sch:
        n = schema_name(sch)
        return type_for_named(n, for_request)
    if sch.get('enum'):
        t = sch.get('type', 'string')
        if t == 'string' and prop == 'path':
            # JSON-pointer paths of change/patch records are open-ended (the spec's own examples use
            # paths outside these enums), so they are read as plain strings.
            return 'String'
        if t == 'string':
            return gen_enum(ctx + pascal(prop), sch) or 'String'
    if sch.get('allOf') and len(sch['allOf']) == 1 and not sch.get('properties'):
        return jtype(sch['allOf'][0], ctx, prop, for_request)
    t = sch.get('type')
    if t == 'array':
        return f"List<{boxed(jtype(sch.get('items'), ctx, prop, for_request))}>"
    if is_object_schema(sch):
        cname = unique_class(ctx + pascal(prop))
        gen_class(cname, sch, None, for_request)
        return cname
    if t == 'string':
        if sch.get('format') == 'binary':
            return 'byte[]'
        return 'String'
    if t == 'integer':
        return 'Long' if sch.get('format') == 'int64' else 'Integer'
    if t == 'number':
        return 'Double'
    if t == 'boolean':
        return 'Boolean'
    if t == 'object':
        ap = sch.get('additionalProperties')
        if isinstance(ap, dict) and ap.get('type') == 'string':
            return 'Map<String, String>'
        return 'JsonNode'
    return 'JsonNode'


def boxed(t):
    return t


def unique_class(name):
    base, i = name, 2
    while name in classes or name in SCH and name not in schema_java:
        name = f'{base}{i}'
        i += 1
    return name


def type_for_named(n, for_request=False):
    sch = SCH[n]
    jn = RENAME.get(n, n)
    if sch.get('enum'):
        if sch.get('type', 'string') == 'string':
            return gen_enum(jn, sch) or 'String'
        return 'Integer'
    if is_object_schema(sch):
        if jn not in classes:
            gen_class(jn, sch, n, for_request)
        elif for_request:
            mark_setters(jn)
        return jn
    if sch.get('type') == 'array':
        return f"List<{jtype(sch.get('items'), jn, 'item', for_request)}>"
    return jtype(sch, jn, '', for_request)


def mark_setters(jn):
    if jn in setter_classes:
        return
    setter_classes.add(jn)
    for f in classes[jn]['fields']:
        for t in re.findall(r'[A-Za-z0-9_]+', f['type']):
            if t in classes and classes[t]['kind'] == 'class':
                mark_setters(t)


def gen_class(jn, sch, schema, for_request):
    classes[jn] = dict(kind='class', fields=[], doc=sch.get('description'), schema=schema, required=[])
    if schema:
        schema_java[schema] = jn
    props, req = merged_props(sch)
    for k, v in EXTRA_PROPS.get(schema or '', {}).items():
        props.setdefault(k, v)
    # spec bug guard: properties named in `required` but not declared (e.g. CreateAsnRequest.type)
    for r in req:
        if r not in props:
            props[r] = {'type': 'string', 'description': 'Declared required by the spec but not listed in its properties.'}
    fields = []
    used = set()
    for pname, psch in props.items():
        fname = jfield(pname)
        while fname in used:
            fname += '_'
        used.add(fname)
        t = jtype(psch, jn, pname, for_request)
        d = psch.get('description')
        if not d and '$ref' in psch:
            d = SCH[schema_name(psch)].get('description')
        fields.append(dict(json=pname, name=fname, type=t, doc=d, required=pname in req,
                           ro=psch.get('readOnly', False)))
    classes[jn]['fields'] = fields
    classes[jn]['required'] = sorted(set(req))
    if for_request:
        mark_setters(jn)


def render_class(jn, c):
    imports = {'com.eqixiac.equinix.fabric.beta.Beta', 'com.fasterxml.jackson.annotation.JsonAutoDetect',
               'com.fasterxml.jackson.annotation.JsonIgnoreProperties', 'com.fasterxml.jackson.annotation.JsonInclude',
               'com.fasterxml.jackson.annotation.JsonProperty', 'lombok.Getter', 'lombok.NoArgsConstructor'}
    setters = jn in setter_classes
    if setters:
        imports |= {'lombok.Setter', 'lombok.experimental.Accessors'}
    body = []
    for f in c['fields']:
        t = f['type']
        if 'List<' in t:
            imports.add('java.util.List')
        if 'Map<' in t:
            imports.add('java.util.Map')
        if 'JsonNode' in t:
            imports.add('com.fasterxml.jackson.databind.JsonNode')
        for tok in re.findall(r'[A-Za-z0-9_]+', t):
            if tok in classes and classes[tok]['kind'] == 'enum':
                imports.add(f'{BASE}.enums.{tok}')
        dd = doc(f['doc'], 600)
        extra = []
        if f['required']:
            extra.append('Required by the spec.')
        if f['ro']:
            extra.append('Read-only (server-populated).')
        jdoc = ' '.join(x for x in [dd] + extra if x) or f"The {{@code {f['json']}}} property."
        body.append(f"    /** {jdoc} */\n    @JsonProperty(\"{f['json']}\")\n    private {t} {f['name']};\n")
    origin = f"spec schema {{@code {c['schema']}}}" if c['schema'] else "an inline spec object"
    cdoc = doc(c['doc'], 800) or f"{jn} model."
    usage = ''
    if setters:
        usage = ("\n * <p>Request-capable: every property has a chained setter ({@code setX(...)} returns this), so an\n"
                 " * instance doubles as its own builder.</p>")
    req = ''
    if c['required']:
        req = "\n * <p>Spec-required properties: " + ', '.join('{@code %s}' % r for r in c['required']) + ".</p>"
    out = HEADER + f"package {BASE}.model;\n\n" + ''.join(f"import {i};\n" for i in sorted(imports)) + f"""
/**
 * {cdoc}
 *
 * <p>Maps {origin}. Unknown properties are ignored on read.</p>{req}{usage}
 * {BETA_DOC}
 * <p>{GEN_NOTE}</p>
 */
@Beta
@Getter
{'@Setter' + chr(10) + '@Accessors(chain = true)' + chr(10) if setters else ''}@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class {jn} {{

""" + '\n'.join(body) + "}\n"
    return out


def render_enum(jn, c):
    used = set()
    consts = []
    for v in c['values']:
        cn = enum_const(v, used)
        consts.append(f"    /** Wire value {{@code {doc(v)}}}. */\n    @JsonProperty(\"{v}\")\n    {cn}(\"{v}\"),\n")
    consts.append("    /** Read-side fallback for any value this SDK version does not know. */\n"
                  "    @JsonEnumDefaultValue\n    UNKNOWN(\"UNKNOWN\");\n")
    cdoc = doc(c['doc'], 600) or f"{jn} values."
    return HEADER + f"""package {BASE}.enums;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {cdoc}
 *
 * <p>Values from the spec; {{@link #UNKNOWN}} is the read-side fallback for values added later.</p>
 * {BETA_DOC}
 * <p>{GEN_NOTE}</p>
 */
@Beta
public enum {jn} {{

""" + '\n'.join(consts) + f"""
    private final String value;

    {jn}(String value) {{
        this.value = value;
    }}

    /**
     * Returns the wire value of this constant.
     *
     * @return the value exactly as the API sends it
     */
    public String getValue() {{
        return value;
    }}
}}
"""


# ---------------------------------------------------------------------------------------------
# Resolve every operation
# ---------------------------------------------------------------------------------------------
FAM = {f[0]: dict(name=f[0], acc=f[1], root=f[2], desc=f[3]) for f in FAMILIES}
resolved = []
for opid, fam, meth, kind, ex in OPS:
    d = op_details(opid)
    d.update(opid=opid, fam=fam, meth=meth, kind=kind, ex=ex)
    d['group'] = ex.get('group', fam)
    d['root'] = ex.get('root', FAM[fam]['root'])
    d['ep'] = opid[0].upper() + opid[1:]
    # path params in order of appearance
    d['pparams'] = re.findall(r'\{([^}]+)\}', d['path'])
    resolved.append(d)

# Request-side models first (setters), then responses
for d in resolved:
    b = d['body']
    if b and d['kind'] in ('create', 'action', 'put', 'envpost'):
        d['bodyType'] = type_for_named(b['schema'], for_request=True)
for d in resolved:
    r = d['resp']
    k = d['kind']
    if k in ('search', 'subsearch', 'list'):
        d['itemSchema'] = data_item_schema(r['schema'])
        d['itemType'] = type_for_named(d['itemSchema'])
    elif k in ('envget', 'envpost'):
        d['envType'] = type_for_named(r['schema'])
        d['itemSchema'] = data_item_schema(r['schema'])
        d['itemType'] = type_for_named(d['itemSchema'])
    elif k == 'bytes':
        pass
    else:
        d['respType'] = type_for_named(r['schema']) if r and r['schema'] else None

# ---------------------------------------------------------------------------------------------
# Search property documentation (filter + sort property names from the request schema)
# ---------------------------------------------------------------------------------------------

def search_props(d):
    filt, sort = set(), set()

    def walk(x, in_sort, seen):
        if isinstance(x, dict):
            if '$ref' in x:
                n = schema_name(x)
                if n in seen:
                    return
                walk(SCH[n], in_sort or 'Sort' in n, seen | {n})
                return
            for k, v in x.items():
                if k == 'property' and isinstance(v, dict):
                    tgt = SCH[schema_name(v)] if '$ref' in v else v
                    vals = [e for e in tgt.get('enum', []) if isinstance(e, str) and e.startswith('/')]
                    vals += re.findall(r'`(/[^`]+)`', tgt.get('description', '') or '')
                    (sort if in_sort else filt).update(vals)
                walk(v, in_sort or k == 'sort', seen)
        elif isinstance(x, list):
            for v in x:
                walk(v, in_sort, seen)
    walk(d['body']['raw'], False, set())
    return sorted(filt), sorted(sort)


# ---------------------------------------------------------------------------------------------
# Emit models
# ---------------------------------------------------------------------------------------------
for jn, c in classes.items():
    if c['kind'] == 'class':
        write(os.path.join(MAIN, 'model', jn + '.java'), render_class(jn, c))
    else:
        write(os.path.join(MAIN, 'enums', jn + '.java'), render_enum(jn, c))

# ---------------------------------------------------------------------------------------------
# apiParams
# ---------------------------------------------------------------------------------------------
groups = {}
for d in resolved:
    g = groups.setdefault(d['group'], dict(rootUri=d['root'], serviceEndpoints={}))
    rel = d['path'][len('/fabric/v4/' + d['root']):].lstrip('/')
    rel = re.sub(r'\{([^}]+)\}', r'{$\1}', rel)
    e = {'httpMethod': d['method']}
    if rel:
        e['requestUri'] = rel
    g['serviceEndpoints'][d['ep']] = e
api = {'functionalAreas': {'FabricBeta': dict(uriFormat='fabric/v{$version}/{$rootUri}/{$requestUri}', defaultVersion=4, **groups)}}
write(os.path.join(REPO, 'src/main/resources/json/apiParams_FabricBeta.json'), json.dumps(api, indent=2) + '\n')

# ---------------------------------------------------------------------------------------------
# Clients
# ---------------------------------------------------------------------------------------------

def pmap(d, names=None):
    names = names or d['pparams']
    if not names:
        return 'Map.of()'
    return 'Map.of(' + ', '.join(f'"{n}", {n}' for n in names) + ')'


def op_doc(d):
    o = d['op']
    summ = doc(o.get('summary'))
    desc = doc(o.get('description'), 500)
    line = f"{summ}: {{@code {d['method']} {d['path']}}} (operation {{@code {d['opid']}}})."
    if desc and desc != summ:
        line += f"\n     * <p>{desc}</p>"
    return line


def jpatch_paths(d):
    b = d['body']
    vals = set()

    def walk(x, seen):
        if isinstance(x, dict):
            if '$ref' in x:
                n = schema_name(x)
                if n not in seen:
                    walk(SCH[n], seen | {n})
                return
            if 'path' in x.get('properties', {}):
                pp = x['properties']['path']
                tgt = SCH[schema_name(pp)] if '$ref' in pp else pp
                vals.update(v for v in tgt.get('enum', []) if isinstance(v, str))
                vals.update(re.findall(r'`(/[^`]+)`', tgt.get('description', '') or ''))
                if tgt.get('example'):
                    vals.add(str(tgt['example']))
            for v in x.values():
                walk(v, seen)
        elif isinstance(x, list):
            for v in x:
                walk(v, seen)
    walk(b['raw'], set())
    return sorted(vals)


def gen_family(fam):
    F = FAM[fam]
    ops = [d for d in resolved if d['fam'] == fam]
    iface, impl = [], []
    types = set()
    needs = set()
    for d in ops:
        k, m = d['kind'], d['meth']
        pp = d['pparams']
        pargs = ', '.join(f'String {p}' for p in pp)
        pdocs = ''.join(f"     * @param {p} the {{@code {p}}} path parameter\n" for p in pp)
        jdoc = op_doc(d)
        def add(sig, body, extra_doc, ret_doc):
            iface.append(f"    /**\n     * {jdoc}\n{extra_doc}     *\n{pdocs_x[0]}     * @return {ret_doc}\n     */\n    {sig};\n")
            impl.append(f"    @Override\n    public {sig} {{\n{body}    }}\n")
        pdocs_x = [pdocs]
        rt = d.get('respType')
        if k in ('get', 'delete'):
            types.add(rt)
            add(f"{rt} {m}({pargs})", f"        return client(\"{d['group']}\", {rt}.class).one(\"{d['ep']}\", {pmap(d)}, null);\n",
                '', f"the {{@code {rt}}} the API returns")
        elif k in ('create', 'action', 'put'):
            bt = d.get('bodyType')
            types.add(rt)
            if bt:
                types.add(bt)
            args = ', '.join(x for x in [pargs, f'{bt} request' if bt else ''] if x)
            pdocs_x[0] = pdocs + (f"     * @param request the {{@code {d['body']['schema']}}} request body\n" if bt else '')
            body_arg = 'request' if bt else 'null'
            add(f"{rt} {m}({args})",
                f"        return client(\"{d['group']}\", {rt}.class).one(\"{d['ep']}\", {pmap(d)}, {body_arg});\n",
                '', f"the {{@code {rt}}} the API returns")
        elif k in ('patch', 'jpatch'):
            types.add(rt)
            needs.add('patch')
            paths = jpatch_paths(d)
            pdocs_x[0] = pdocs + "     * @param operations the patch operations ({@code op}/{@code path}/{@code value})\n"
            ct = 'application/json-patch+json' if k == 'jpatch' else 'application/json'
            extra = f"     * <p>Sent as {{@code {ct}}}, per the spec.{(' Patchable paths named by the spec: ' + ', '.join('{@code %s}' % doc(p) for p in paths) + '.') if paths else ''}</p>\n"
            add(f"{rt} {m}({', '.join(x for x in [pargs, 'List<PatchOperation> operations'] if x)})",
                f"        return client(\"{d['group']}\", {rt}.class).patch(\"{d['ep']}\", {pmap(d)}, operations, {'true' if k == 'jpatch' else 'false'});\n",
                extra, f"the {{@code {rt}}} the API returns")
        elif k in ('search', 'subsearch'):
            it = d['itemType']
            types.add(it)
            needs.add('search')
            fp, sp = search_props(d)
            extra = ''
            if fp:
                extra += "     * <p>Filter properties named by the spec: " + ', '.join('{@code %s}' % doc(x) for x in fp) + ".</p>\n"
            if sp:
                extra += "     * <p>Sort properties named by the spec: " + ', '.join('{@code %s}' % doc(x) for x in sp) + ".</p>\n"
            base_pd = pdocs
            ret = f"a lazily paged list of {{@code {it}}}"
            body = f"        return client(\"{d['group']}\", {it}.class).search(\"{d['ep']}\", {pmap(d)}, filter, sort);\n"
            pdocs_x[0] = base_pd + "     * @param filter the filter ({@code null} for none)\n     * @param sort the sort order ({@code null} for the service default)\n"
            add(f"PaginatedFilteredList<{it}> {m}({', '.join(x for x in [pargs, 'FilterPropertyList filter', 'SortPropertyList sort'] if x)})",
                body, extra, ret)
            pdocs_x[0] = base_pd + "     * @param filter the filter ({@code null} for none)\n"
            add(f"PaginatedFilteredList<{it}> {m}({', '.join(x for x in [pargs, 'FilterPropertyList filter'] if x)})",
                f"        return {m}({', '.join(pp + ['filter', 'null'])});\n", '', ret)
            pdocs_x[0] = base_pd
            add(f"PaginatedFilteredList<{it}> {m}({pargs})",
                f"        return {m}({', '.join(pp + ['null', 'null'])});\n", '', ret)
        elif k == 'list':
            it = d['itemType']
            types.add(it)
            sp = d['ex'].get('statusParam')
            ret = f"a lazily paged list of {{@code {it}}}"
            if sp:
                needs.add('query')
                pdocs_x[0] = pdocs + f"     * @param query optional {{@code {sp}}}/{{@code order}}/{{@code style}} query parameters ({{@code null}} for none)\n"
                add(f"PaginatedList<{it}> {m}({pargs}, AttachmentQuery query)",
                    f"        return client(\"{d['group']}\", {it}.class).list(\"{d['ep']}\", {pmap(d)}, query == null ? null : query.toQueryParams(\"{sp}\"));\n",
                    f"     * <p>Paged with {{@code offset}}/{{@code limit}}; {{@code {sp}}} filters by state.</p>\n", ret)
                pdocs_x[0] = pdocs
                add(f"PaginatedList<{it}> {m}({pargs})", f"        return {m}({', '.join(pp)}, null);\n", '', ret)
            else:
                add(f"PaginatedList<{it}> {m}({pargs})",
                    f"        return client(\"{d['group']}\", {it}.class).list(\"{d['ep']}\", {pmap(d)}, null);\n",
                    "     * <p>Paged with {@code offset}/{@code limit}.</p>\n", ret)
        elif k in ('envget', 'envpost'):
            et, it = d['envType'], d['itemType']
            types |= {et, it}
            needs.add('list')
            bt = d.get('bodyType')
            if bt:
                types.add(bt)
            args = ', '.join(x for x in [pargs, f'{bt} request' if bt else ''] if x)
            pdocs_x[0] = pdocs + (f"     * @param request the {{@code {d['body']['schema']}}} request body\n" if bt else '')
            add(f"List<{it}> {m}({args})",
                f"        {et} envelope = client(\"{d['group']}\", {et}.class).one(\"{d['ep']}\", {pmap(d)}, {'request' if bt else 'null'});\n"
                f"        return envelope == null || envelope.getData() == null ? List.of() : List.copyOf(envelope.getData());\n",
                f"     * <p>Unwraps the {{@code {d['resp']['schema']}}} {{@code data}} envelope (the spec documents no paging for it).</p>\n",
                f"the {{@code {it}}} entries (empty when none)")
        elif k == 'bytes':
            add(f"byte[] {m}({pargs})", f"        return client(\"{d['group']}\", Object.class).bytes(\"{d['ep']}\", {pmap(d)});\n",
                "     * <p>The spec types the response as {@code multipart/mixed}; the raw body bytes are returned.</p>\n",
                "the raw response body")
        elif k == 'multipart':
            types.add(rt)
            pdocs_x[0] = ("     * @param logo the image bytes (spec: required, binary)\n     * @param fileName the file name advertised for the {@code logo} part\n"
                          "     * @param name the logo name (spec: required)\n     * @param type the logo type (spec: required)\n"
                          "     * @param description the logo description (spec: required)\n")
            add(f"{rt} {m}(byte[] logo, String fileName, String name, String type, String description)",
                f"        return client(\"{d['group']}\", {rt}.class).multipart(\"{d['ep']}\", \"logo\", logo, fileName,\n"
                f"                java.util.Map.of(\"name\", name, \"type\", type, \"description\", description));\n",
                "     * <p>Sent as {@code multipart/form-data} with the {@code logo} file part and the\n     * {@code name}/{@code type}/{@code description} text parts.</p>\n",
                f"the {{@code {rt}}} the API returns")
    imports = set()
    for t in types:
        if t and t in classes:
            imports.add(f'{BASE}.model.{t}')
    if 'patch' in needs:
        imports |= {'com.eqixiac.equinix.core.http.request.PatchOperation', 'java.util.List'}
    if 'list' in needs:
        imports.add('java.util.List')
    if 'search' in needs:
        imports |= {'com.eqixiac.equinix.core.http.response.PaginatedFilteredList',
                    'com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList',
                    'com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList'}
    if any(d['kind'] == 'list' for d in ops):
        imports.add('com.eqixiac.equinix.core.http.response.PaginatedList')
    if 'query' in needs:
        imports.add(f'{BASE}.client.AttachmentQuery')
    iimports = sorted(i for i in imports if not i.startswith(f'{BASE}.client.AttachmentQuery') or True)
    iface_src = HEADER + f"package {BASE}.client;\n\nimport {BASE}.Beta;\n" + ''.join(
        f"import {i};\n" for i in iimports if i != f'{BASE}.client.AttachmentQuery') + f"""
/**
 * {F['desc']} ({len(ops)} operations).
 *
 * <p>Obtain it from {{@code fabric.beta().{F['acc']}()}}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * {BETA_DOC}
 * <p>{GEN_NOTE}</p>
 */
@Beta
public interface {fam} {{

""" + '\n'.join(iface) + "}\n"
    write(os.path.join(MAIN, 'client', fam + '.java'), iface_src)
    impl_imports = set(iimports) | {f'{BASE}.Beta', f'{BASE}.client.{fam}', f'{BASE}.client.internal.BetaClientFactory',
                                    f'{BASE}.client.internal.BetaResourceClient', 'java.util.Map'}
    if 'query' in needs:
        impl_imports.add(f'{BASE}.client.AttachmentQuery')
    impl_src = HEADER + f"package {BASE}.client.implementation;\n\n" + ''.join(f"import {i};\n" for i in sorted(impl_imports)) + f"""
/**
 * Implementation of {{@link {fam}}} over the shared {{@code FabricBeta}} apiParams area.
 * {BETA_DOC}
 * <p>{GEN_NOTE}</p>
 */
@Beta
public class {fam}Impl implements {fam} {{

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public {fam}Impl(BetaClientFactory clients) {{
        this.clients = clients;
    }}

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {{
        return clients.client(requestParent, type);
    }}

""" + '\n'.join(impl) + "}\n"
    write(os.path.join(MAIN, 'client/implementation', fam + 'Impl.java'), impl_src)


for fam in FAM:
    gen_family(fam)

# Hub
hub_methods = []
hub_impl = []
for f in FAMILIES:
    name, acc = f[0], f[1]
    n = len([d for d in resolved if d['fam'] == name])
    hub_methods.append(f"""    /**
     * {f[3]} ({n} operations).
     *
     * @return the {{@link {name}}} client
     */
    {name} {acc}();
""")
    hub_impl.append(f"""    @Override
    public {name} {acc}() {{
        {name} c = this.{acc};
        if (c == null) {{
            c = new {name}Impl(clients);
            this.{acc} = c;
        }}
        return c;
    }}
""")
write(os.path.join(MAIN, 'client/FabricBeta.java'), HEADER + f"""package {BASE}.client;

import {BASE}.Beta;

/**
 * Hub for the Fabric v4 operation families the spec marks <strong>Beta</strong>: one typed client per family.
 *
 * <p>Obtain it from {{@code fabric.beta()}}. Endpoints live in their own apiParams functional area
 * ({{@code FabricBeta}}, file {{@code json/apiParams_FabricBeta.json}}) with the same URI format and version as
 * {{@code Fabric}}, so they never shadow or replace a GA Fabric endpoint.</p>
 *
 * <p>Beta endpoints may change shape or be withdrawn by Equinix without notice, and the account must be
 * entitled to them (an un-entitled account typically sees HTTP 403 or 404). None of these operations offers
 * {{@code dryRun}} in the spec. Cross connects, LOAs and optical connects are real physical orders.</p>
 * {BETA_DOC}
 */
@Beta
public interface FabricBeta {{

""" + '\n'.join(hub_methods) + "}\n")
write(os.path.join(MAIN, 'client/implementation/FabricBetaImpl.java'), HEADER + f"""package {BASE}.client.implementation;

import com.eqixiac.equinix.core.client.EquinixClient;
import {BASE}.Beta;
import {BASE}.client.*;
import {BASE}.client.internal.BetaClientFactory;

/**
 * Default {{@link FabricBeta}} hub: lazily creates one client per family over a shared
 * {{@link BetaClientFactory}}.
 * {BETA_DOC}
 */
@Beta
public class FabricBetaImpl implements FabricBeta {{

    private final BetaClientFactory clients;

""" + ''.join(f"    private volatile {f[0]} {f[1]};\n\n" for f in FAMILIES) + f"""    /**
     * Creates the hub over a core client whose catalogue already contains {{@code json/apiParams_FabricBeta.json}}.
     *
     * @param equinixClient the core client
     */
    public FabricBetaImpl(EquinixClient equinixClient) {{
        this.clients = new BetaClientFactory(equinixClient);
    }}

""" + '\n'.join(hub_impl) + "}\n")

# ---------------------------------------------------------------------------------------------
# Fixtures + tests
# ---------------------------------------------------------------------------------------------
fixture_index = []  # (fam, opid, 'request'|'response', exname, relpath, javaType)


def fixture(d, side, exname, value):
    rel = f"{d['fam'].lower()}/{d['opid']}.{side}.{exname}.json"
    write(os.path.join(FIX, rel), json.dumps(value, indent=2) + '\n')
    return '/json/fabric/beta/' + rel


for d in resolved:
    d['reqFix'] = []
    d['respFix'] = []
    if d['body']:
        for exn, ex in d['body']['examples'].items():
            d['reqFix'].append((exn, fixture(d, 'request', exn, ex['value'])))
    if d['resp']:
        for exn, ex in d['resp']['examples'].items():
            d['respFix'].append((exn, fixture(d, 'response', exn, ex['value'])))

# deserialization test cases: every response example into its model; every request example into its body model
deser = []
for d in resolved:
    k = d['kind']
    rtype = None
    if k in ('search', 'subsearch', 'list'):
        rtype = None  # envelope handled by Page; assert items instead
        for exn, path in d['respFix']:
            deser.append((d['opid'], exn, path, d['itemType'], 'data'))
    elif k in ('envget', 'envpost'):
        for exn, path in d['respFix']:
            deser.append((d['opid'], exn, path, d['envType'], ''))
    elif d.get('respType'):
        for exn, path in d['respFix']:
            deser.append((d['opid'], exn, path, d['respType'], ''))
    if d.get('bodyType'):
        for exn, path in d['reqFix']:
            deser.append((d['opid'], exn, path, d['bodyType'], ''))

model_imports = sorted({c[3] for c in deser})
cases = ',\n'.join(f'                Arguments.of("{o}", "{e}", "{p}", {t}.class, "{ptr}")' for o, e, p, t, ptr in deser)
write(os.path.join(TEST, 'FabricBetaModelDeserializationTest.java'), HEADER + f"""package {BASE};

""" + ''.join(f"import {BASE}.model.{t};\n" for t in model_imports) + f"""import com.eqixiac.equinix.core.TestFixtures;
import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Reads every request and response example the Fabric v4 {SPEC_VERSION} spec gives for the Beta operations into
 * its SDK model and asserts nothing is lost: each example value must survive a read and re-serialization
 * (see {{@link BetaTestSupport#assertCovers}}). Search and list envelopes are checked element by element
 * against their {{@code data}} item model, as the SDK pages them.
 */
class FabricBetaModelDeserializationTest {{

    static Stream<Arguments> examples() {{
        return Stream.of(
{cases}
        );
    }}

    @ParameterizedTest(name = "{{0}} / {{1}}")
    @MethodSource("examples")
    void exampleSurvivesReadAndWrite(String operationId, String example, String fixture, Class<?> model, String pointer)
            throws Exception {{
        JsonNode expected = Constants.mapper().readTree(TestFixtures.load(fixture));
        if (!pointer.isEmpty()) {{
            for (JsonNode item : expected.path(pointer)) {{
                Object read = Constants.mapper().treeToValue(item, model);
                BetaTestSupport.assertCovers(item, read, operationId + "/" + example);
            }}
            return;
        }}
        Object read = Constants.mapper().treeToValue(expected, model);
        BetaTestSupport.assertCovers(expected, read, operationId + "/" + example);
    }}
}}
""")

# WireMock tests per family
STUB = {'GET': 'get', 'POST': 'post', 'PUT': 'put', 'PATCH': 'patch', 'DELETE': 'delete'}
VERIFY = {'GET': 'getRequestedFor', 'POST': 'postRequestedFor', 'PUT': 'putRequestedFor', 'PATCH': 'patchRequestedFor',
          'DELETE': 'deleteRequestedFor'}


def sample_ids(d):
    return {p: f"{p}-0001" for p in d['pparams']}


def concrete_path(d):
    ids = sample_ids(d)
    return re.sub(r'\{([^}]+)\}', lambda m: ids[m.group(1)], d['path'])


def gen_test(fam):
    ops = [d for d in resolved if d['fam'] == fam]
    acc = FAM[fam]['acc']
    tests = []
    imports = set()
    for d in ops:
        k, m = d['kind'], d['meth']
        path = concrete_path(d)
        args = [f'"{v}"' for v in sample_ids(d).values()]
        resp = d['respFix'][0] if d['respFix'] else None
        status = d['resp']['code'] if d['resp'] else 200
        rbody = f'loadFixture("{resp[1]}")' if resp else '"{}"'
        resp_ex = f" (response example {{@code {resp[0]}}})" if resp else ''
        verify = f"wireMock.verify(1, {VERIFY[d['method']]}(urlPathEqualTo(\"{path}\"))\n                .withHeader(\"Authorization\", matching(\"Bearer .+\"))"
        stub = (f"        wireMock.stubFor({STUB[d['method']]}(urlPathEqualTo(\"{path}\"))\n"
                f"                .willReturn(aResponse().withStatus({status}).withHeader(\"Content-Type\", \"application/json\")\n"
                f"                        .withBody({rbody})));\n")
        name = d['opid']
        body = ''
        if k in ('get', 'delete'):
            body = stub + f"        Object result = beta.{acc}().{m}({', '.join(args)});\n" + \
                f"        {verify}\n                .withRequestBody(absent()));\n" + \
                (f"        BetaTestSupport.assertCovers(loadFixture(\"{resp[1]}\"), result, \"{name}\");\n" if resp else '')
        elif k in ('create', 'action', 'put', 'envpost'):
            bt = d.get('bodyType')
            if bt:
                req = d['reqFix'][0]
                imports.add(f'{BASE}.model.{bt}')
                body = stub + f"        String requestJson = loadFixture(\"{req[1]}\"); // request example {req[0]}\n" + \
                    f"        {bt} request = BetaTestSupport.read(requestJson, {bt}.class);\n" + \
                    f"        Object result = beta.{acc}().{m}({', '.join(args + ['request'])});\n" + \
                    f"        {verify}\n                .withHeader(\"Content-Type\", matching(\"application/json(;.*)?\"))\n" + \
                    f"                .withRequestBody(equalToJson(BetaTestSupport.withoutNulls(requestJson), false, false)));\n"
            else:
                body = stub + f"        Object result = beta.{acc}().{m}({', '.join(args)});\n" + \
                    f"        {verify}\n                .withRequestBody(absent()));\n"
            if resp:
                if k == 'envpost':
                    body += f"        BetaTestSupport.assertCovers(loadFixture(\"{resp[1]}\"), java.util.Map.of(\"data\", result), \"{name}\");\n"
                else:
                    body += f"        BetaTestSupport.assertCovers(loadFixture(\"{resp[1]}\"), result, \"{name}\");\n"
        elif k in ('patch', 'jpatch'):
            req = d['reqFix'][0]
            ct = 'application/json-patch+json' if k == 'jpatch' else 'application/json'
            ctre = ct.replace('+', '\\\\+')
            imports |= {'com.eqixiac.equinix.core.http.request.PatchOperation', 'java.util.List'}
            body = stub + f"        String requestJson = loadFixture(\"{req[1]}\"); // request example {req[0]}\n" + \
                f"        List<PatchOperation> ops = BetaTestSupport.patchOps(requestJson);\n" + \
                f"        Object result = beta.{acc}().{m}({', '.join(args + ['ops'])});\n" + \
                f"        {verify}\n                .withHeader(\"Content-Type\", matching(\"{ctre}(;.*)?\"))\n" + \
                f"                .withRequestBody(equalToJson(requestJson, false, false)));\n" + \
                (f"        BetaTestSupport.assertCovers(loadFixture(\"{resp[1]}\"), result, \"{name}\");\n" if resp else '')
        elif k in ('search', 'subsearch'):
            imports |= {'com.eqixiac.equinix.fabric.model.implementation.filter.Filter',
                        'com.eqixiac.equinix.fabric.model.implementation.sort.Sort'}
            fp, sp = search_props(d)
            fprop = '/name' if '/name' in fp or not fp else fp[0]
            sprop = '/changeLog/updatedDateTime' if '/changeLog/updatedDateTime' in sp or not sp else sp[0]
            expected = {"filter": {"and": [{"property": fprop, "operator": "=", "values": ["sdk-probe"]}]},
                        "pagination": {"offset": 0, "limit": 100}, "sort": [{"property": sprop, "direction": "DESC"}]}
            exp = json.dumps(json.dumps(expected))
            fexpr = 'Filter.filter().and().equals("%s", "sdk-probe")' % fprop
            sexpr = 'Sort.sort().desc("%s")' % sprop
            sargs = ', '.join(args + [fexpr, sexpr])
            body = stub + f"        var page = beta.{acc}().{m}({sargs});\n" + \
                f"        {verify}\n                .withHeader(\"Content-Type\", matching(\"application/json(;.*)?\"))\n" + \
                f"                .withRequestBody(equalToJson({exp}, false, false)));\n" + \
                f"        BetaTestSupport.assertPageCovers(loadFixture(\"{resp[1]}\"), page.toList(), \"{name}\");\n"
        elif k == 'list':
            sp = d['ex'].get('statusParam')
            if sp:
                qargs = ', '.join(args + ['new AttachmentQuery().states("S1", "S2").order(SortOrder.ASC).style(AttachmentQuery.Style.FULL)'])
                body = stub + f"        var page = beta.{acc}().{m}({qargs});\n" + \
                    f"        {verify}\n                .withQueryParam(\"offset\", equalTo(\"0\")).withQueryParam(\"limit\", matching(\"\\\\d+\"))\n" + \
                    f"                .withQueryParam(\"{sp}\", havingExactly(\"S1\", \"S2\")).withQueryParam(\"order\", equalTo(\"ASC\"))\n" + \
                    f"                .withQueryParam(\"style\", equalTo(\"FULL\")));\n"
                imports |= {f'{BASE}.client.AttachmentQuery', 'com.eqixiac.equinix.core.enums.SortOrder'}
            else:
                body = stub + f"        var page = beta.{acc}().{m}({', '.join(args)});\n" + \
                    f"        {verify}\n                .withQueryParam(\"offset\", equalTo(\"0\")).withQueryParam(\"limit\", matching(\"\\\\d+\")));\n"
            body += f"        BetaTestSupport.assertPageCovers(loadFixture(\"{resp[1]}\"), page.toList(), \"{name}\");\n"
        elif k == 'envget':
            body = stub + f"        var items = beta.{acc}().{m}({', '.join(args)});\n" + \
                f"        {verify}\n                .withRequestBody(absent()));\n" + \
                f"        BetaTestSupport.assertPageCovers(loadFixture(\"{resp[1]}\"), items, \"{name}\");\n"
        elif k == 'bytes':
            body = (f"        wireMock.stubFor(get(urlPathEqualTo(\"{path}\"))\n"
                    f"                .willReturn(aResponse().withStatus(200).withHeader(\"Content-Type\", \"multipart/mixed\")\n"
                    f"                        .withBody(new byte[]{{1, 2, 3}})));\n"
                    f"        byte[] result = beta.{acc}().{m}({', '.join(args)});\n"
                    f"        {verify});\n"
                    f"        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[]{{1, 2, 3}}, result);\n")
        elif k == 'multipart':
            body = (f"        wireMock.stubFor(post(urlPathEqualTo(\"{path}\"))\n"
                    f"                .willReturn(aResponse().withStatus({status}).withHeader(\"Content-Type\", \"application/json\")\n"
                    f"                        .withBody(\"{{\\\"uuid\\\":\\\"logo-1\\\",\\\"name\\\":\\\"acme\\\"}}\")));\n"
                    f"        var result = beta.{acc}().{m}(new byte[]{{(byte) 0x89, 'P', 'N', 'G'}}, \"acme.png\", \"acme\", \"LOGO\", \"Acme logo\");\n"
                    f"        {verify}\n                .withHeader(\"Content-Type\", containing(\"multipart/form-data; boundary=\"))\n"
                    f"                .withRequestBodyPart(aMultipart().withName(\"logo\").build())\n"
                    f"                .withRequestBodyPart(aMultipart().withName(\"name\").withBody(equalTo(\"acme\")).build())\n"
                    f"                .withRequestBodyPart(aMultipart().withName(\"type\").withBody(equalTo(\"LOGO\")).build())\n"
                    f"                .withRequestBodyPart(aMultipart().withName(\"description\").withBody(equalTo(\"Acme logo\")).build()));\n"
                    f"        org.junit.jupiter.api.Assertions.assertEquals(\"logo-1\", result.getUuid());\n")
        tests.append(f"""    /** {{@code {d['method']} {d['path']}}} ({d['opid']}){resp_ex}. */
    @Test
    void {name}() throws Exception {{
{body}    }}
""")
    src = HEADER + f"""package {BASE};

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import {BASE}.client.FabricBeta;
""" + ''.join(f"import {i};\n" for i in sorted(imports)) + f"""import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock contract test for {{@code fabric.beta().{acc}()}}: for every operation it asserts the HTTP method,
 * path, query, headers and body the SDK sends, and that the spec's response example reads back without loss.
 * Request and response fixtures are the spec's own examples, copied verbatim (file names carry the example name).
 */
class FabricBeta{fam}WireMockTest extends WireMockTestBase {{

    static Fabric fabric;
    static FabricBeta beta;

    @BeforeAll
    static void setUp() {{
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
        beta = fabric.beta();
    }}

    @AfterAll
    static void tearDown() throws Exception {{
        if (fabric != null) fabric.close();
    }}

    @BeforeEach
    void reset() {{
        resetStubs();
    }}

""" + '\n'.join(tests) + "}\n"
    write(os.path.join(TEST, f'FabricBeta{fam}WireMockTest.java'), src)


for fam in FAM:
    gen_test(fam)

# summary
json.dump([dict(opid=d['opid'], fam=d['fam'], meth=d['meth'], kind=d['kind'], method=d['method'], path=d['path'])
           for d in resolved], open(os.path.join(os.path.dirname(__file__), 'resolved.json'), 'w'), indent=1)
print('classes', sum(1 for c in classes.values() if c['kind'] == 'class'), 'enums',
      sum(1 for c in classes.values() if c['kind'] == 'enum'), 'ops', len(resolved), 'deser cases', len(deser))
