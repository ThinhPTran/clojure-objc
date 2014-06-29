package clojure.lang;

/*-[
 #include "clojure/core_gensym.h"
 ]-*/

public class RemoteRef extends RestFn {

  private static Object nsPlaceholderString = new Object();

  private static final String OBJC_REF = "objc-ref-";

  private static final String JVM_REF = "jvm-ref-";

  private static Atom a = new Atom(RT.map());
  private static Atom i = new Atom(RT.map());

  private static Var gensym = RT.var("clojure.core", "gensym");

  public static void reset() {
    a.reset(RT.map());
    i.reset(RT.map());
  }

  static {
    register(nsPlaceholderString);
  }

  public static Object register(final Object o) {
    if (ObjC.objc && classDescription(o).equals("NSPlaceholderString")) {
      nativeRelease(o);
      return register(nsPlaceholderString);
    }
    IPersistentMap lookup = (IPersistentMap) i.deref();
    Object curr = lookup.valAt(o);
    if (curr == null) {
      Object invoke = ObjC.objc ? nativeGensym(OBJC_REF) : ((AFn) gensym
          .getRawRoot()).invoke(JVM_REF);
      final String id = ((Symbol) invoke).getName();
      a.swap(new AFn() {
        @Override
        public Object invoke(Object old) {
          return RT.assoc(old, id, o);
        }
      });
      i.swap(new AFn() {
        @Override
        public Object invoke(Object old) {
          return RT.assoc(old, o, id);
        }
      });
      return id;
    } else {
      return curr;
    }
  }

  private static native Object classDescription(Object o) /*-[
                                                          return [[o class] description];
                                                          ]-*/;

  private static native void nativeRelease(Object o) /*-[
                                                     [o release];
                                                     ]-*/;
  
  private static native void nativeRetain(Object o) /*-[
  [o retain];
  ]-*/;

  private native static Object nativeGensym(String objcRef) /*-[
                                                            return [Clojurecore_gensym_get_VAR_() invokeWithId:objcRef];
                                                            ]-*/;

  private String id;

  public RemoteRef(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public Object get() {
    if (ObjC.objc) {
      if (id.startsWith(JVM_REF)) {
        nativeRetain(this);
        return this;
      } else {
        Object o = (Object) RT.get(a.deref(), id);
        if (o.equals(nsPlaceholderString)) {
          return allocNativeString();
        } else {
          return o;
        }
      }
    } else {
      if (id.startsWith(OBJC_REF)) {
        return this;
      } else {
        return (Object) RT.get(a.deref(), id);
      }
    }
  }

  private static native Object allocNativeString() /*-[
                                                   return [NSString alloc]; // leaks in repl
                                                   ]-*/;

  @Override
  protected Object doInvoke(Object args) {
    return RemoteRepl.callRemote(this, args);
  }

  @Override
  public int getRequiredArity() {
    return 0;
  }

  @Override
  public boolean equals(Object f) {
    if (f != null && f instanceof RemoteRef) {
      return ((RemoteRef) f).id.equals(id);
    }
    return false;
  }
}
