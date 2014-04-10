/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Mar 29, 2006 10:39:05 AM */

package clojure.lang;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;


public class Keyword implements IFn, Comparable, Named, Serializable, IHashEq {

private static ConcurrentHashMap<Symbol, Reference<Keyword>> table = new ConcurrentHashMap();
static Symbol lineSymbol = Symbol.intern("line");
static Keyword line = new Keyword(lineSymbol);
static Symbol columnSymbol = Symbol.intern("column");
static Keyword column = new Keyword(columnSymbol);
static Symbol addedSymbol = Symbol.intern("added");
static Keyword added = new Keyword(addedSymbol);
static Symbol fileSymbol = Symbol.intern("file");
static Keyword file = new Keyword(fileSymbol);
static Symbol arglistsSymbol = Symbol.intern("arglists");
static Keyword arglists = new Keyword(arglistsSymbol);
static Symbol docSymbol = Symbol.intern("doc");
static Keyword doc = new Keyword(docSymbol);
static Symbol tagSymbol = Symbol.intern("tag");
static Keyword tag = new Keyword(tagSymbol);
static Symbol staticSymbol = Symbol.intern("static");
static Keyword static_ = new Keyword(staticSymbol);
static Symbol privateSymbol = Symbol.intern("private");
static Keyword private_ = new Keyword(privateSymbol);
static Symbol inlineSymbol = Symbol.intern("inline");
static Keyword inline = new Keyword(inlineSymbol);
static Symbol dynamicSymbol = Symbol.intern("dynamic");
static Keyword dynamic = new Keyword(dynamicSymbol);
static Symbol oninterfaceSymbol = Symbol.intern("on-interface");
static Keyword oninterface = new Keyword(oninterfaceSymbol);
static Symbol inlinearitiesSymbol = Symbol.intern("inline-arities");
static Keyword inlinearities = new Keyword(inlinearitiesSymbol);

static final ReferenceQueue rq = new ReferenceQueue();
public final Symbol sym;
final int hasheq;
String _str;

public static Keyword intern(Symbol sym){
  if (lineSymbol.equals(sym)) return line;
  if (fileSymbol.equals(sym)) return file;
  if (columnSymbol.equals(sym)) return column;
  if (addedSymbol.equals(sym)) return added;
  if (docSymbol.equals(sym)) return doc;
  if (tagSymbol.equals(sym)) return tag;
  if (privateSymbol.equals(sym)) return private_;
  if (staticSymbol.equals(sym)) return static_;
  if (oninterfaceSymbol.equals(sym)) return oninterface;
  if (inlineSymbol.equals(sym)) return inline;
  if (dynamicSymbol.equals(sym)) return dynamic;
  if (arglistsSymbol.equals(sym)) return arglists;
  if (inlinearitiesSymbol.equals(sym)) return inlinearities;
	if(sym.meta() != null)
		sym = (Symbol) sym.withMeta(null);
	Util.clearCache(rq, table);
	Keyword k = new Keyword(sym);
	Reference<Keyword> existingRef = table.putIfAbsent(sym, new WeakReference<Keyword>(k,rq));
	if(existingRef == null)
		return k;
	Keyword existingk = existingRef.get();
	if(existingk != null)
		return existingk;
	//entry died in the interim, do over
	table.remove(sym, existingRef);
	return intern(sym);
}

public static Keyword intern(String ns, String name){
	return intern(Symbol.intern(ns, name));
}

public static Keyword intern(String nsname){
	return intern(Symbol.intern(nsname));
}

private Keyword(Symbol sym){
	this.sym = sym;
	hasheq = sym.hasheq() + 0x9e3779b9;
}

public static Keyword find(Symbol sym){
    Reference<Keyword> ref = table.get(sym);
    if (ref != null)
        return ref.get();
    else
        return null;
}

public static Keyword find(String ns, String name){
    return find(Symbol.intern(ns, name));
}

public static Keyword find(String nsname){
    return find(Symbol.intern(nsname));
}

public final int hashCode(){
	return sym.hashCode() + 0x9e3779b9;
}

public int hasheq() {
	return hasheq;
}

public String toString(){
	if(_str == null)
		_str = (":" + sym).intern();
	return _str;
}

public Object throwArity(){
	throw new IllegalArgumentException("Wrong number of args passed to keyword: "
	                                   + toString());
}

public Object call() {
	return throwArity();
}

public void run(){
	throw new UnsupportedOperationException();
}

public Object invoke() {
	return throwArity();
}

public int compareTo(Object o){
	return sym.compareTo(((Keyword) o).sym);
}


public String getNamespace(){
	return sym.getNamespace();
}

public String getName(){
	return sym.getName();
}

private Object readResolve() throws ObjectStreamException{
	return intern(sym);
}

/**
 * Indexer implements IFn for attr access
 *
 * @param obj - must be IPersistentMap
 * @return the value at the key or nil if not found
 * @
 */
final public Object invoke(Object obj) {
	if(obj instanceof ILookup)
		return ((ILookup)obj).valAt(this);
	return RT.get(obj, this);
}

final public Object invoke(Object obj, Object notFound) {
	if(obj instanceof ILookup)
		return ((ILookup)obj).valAt(this,notFound);
	return RT.get(obj, this, notFound);
}

public Object invoke(Object arg1, Object arg2, Object arg3) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7)
		{
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13)
		{
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14)
		{
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15, Object arg16) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15, Object arg16, Object arg17) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15, Object arg16, Object arg17, Object arg18) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15, Object arg16, Object arg17, Object arg18, Object arg19) {
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20)
		{
	return throwArity();
}

public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                     Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
                     Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20,
                     Object... args)
		{
	return throwArity();
}


public Object applyTo(ISeq arglist) {
	return AFn.applyToHelper(this, arglist);
}


}
