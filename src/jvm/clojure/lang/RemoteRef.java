package clojure.lang;

/*-[
#include "clojure/core_gensym.h"
]-*/

public class RemoteRef extends RestFn {

  private static final String OBJC_REF = "objc-ref-";

  private static final String JVM_REF = "jvm-ref-";

  private static Atom a = new Atom(RT.map());
  private static Atom i = new Atom(RT.map());

  private static Var gensym = RT.var("clojure.core", "gensym");

  public static void reset() {
    a.reset(RT.map());
    i.reset(RT.map());
  }
  
  public static Object register(final Object o) {
    IPersistentMap lookup = (IPersistentMap) i.deref();
    Object curr = lookup.valAt(o);
    if (curr == null) {
      Object invoke = ObjC.objc ? nativeGensym(OBJC_REF) : ((AFn) gensym.getRawRoot())
          .invoke(JVM_REF);
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

  private native static Object nativeGensym(String objcRef) /*-[
    return null;
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
        return this;
      } else {
        return (Object) RT.get(a.deref(), id);
      }
    } else {
      if (id.startsWith(OBJC_REF)) {
        return this;
      } else {
        return (Object) RT.get(a.deref(), id);
      }
    }
  }

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
