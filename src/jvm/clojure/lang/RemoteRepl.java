package clojure.lang;

import java.io.IOException;

public class RemoteRepl {

  public static boolean connected;

  static Var callRemoteSel = RT.var("clojure.remoterepl", "call-remote");
  static Var lister = RT.var("clojure.remoterepl", "listen");
  
  public static Object callRemote(Object o, Object seq) {
    if (RemoteRepl.connected) {
      return callRemoteSel.invoke(o, RT.seq(seq));
    } else {
      //throw new RuntimeException("RemoteRepl not connected");
      return null;
    }
  }

  public static void setConnected(boolean connected) {
    RemoteRepl.connected = connected;
    RemoteRef.reset();
  }
  
  public static void listen(int port) {
    connect(null, port);
  }
  
  public static void connect(String host, int port) {
    try {
      RT.load("clojure/remoterepl");
      lister.invoke(host, port);
    } catch (Exception e) {
      throw Util.sneakyThrow(e);
    }
  }
  
  public static native Object safetry(AFn fn) /*-[
    @try {
      return [fn invoke];
    } 
    @catch (NSException *exception) {
      NSLog(@"%@", [exception callStackSymbols]);
      return nil;
    }
  ]-*/;
}
