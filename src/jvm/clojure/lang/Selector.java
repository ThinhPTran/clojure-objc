package clojure.lang;

/*-[
#import "NSCommon.h"
]-*/

public class Selector extends RestFn implements Named {

  public static Object invokeSelector(String sel, Object o) {
    return invokeSelector(sel, o, null);
  }
  
  public static Object invokeSelector(String sel, Object o, Object args) {
    if (!ObjC.objc) {
      return RemoteRepl.callRemote(new Selector(sel), RT.cons(o, args));
    } else {
      if (args != null && !sel.endsWith(":")) {
        sel = sel + ":";
      }
      return invokeSel(o, sel, RT.seq(args));
    }
  }
  
  public final String sel;

  public Selector(Symbol sel) {
    this.sel = sel.name;
  }
  
  public Selector(String sel) {
    this.sel = sel;
  }

  @Override
  public String getNamespace() {
    return null;
  }

  @Override
  public String getName() {
    return sel;
  }
  
  @Override
  protected Object doInvoke(Object o, Object args) {
    return invokeSelector(sel, o, args);
  }
  
  public static native Object invokeSel(Object object, String selector,
      ISeq arguments) /*-[
   return [NSCommon invokeSel:object withSelector:selector withArgs:arguments];
  ]-*/;
  
  @Override
  public int getRequiredArity() {
    return 1;
  }
  
  @Override
  public String toString() {
    return sel;
  }
}
