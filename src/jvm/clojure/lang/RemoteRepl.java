package clojure.lang;

import java.io.IOException;

/*-[
#include "ReplClient.h"
]-*/

public class RemoteRepl {

  public static boolean connected;

  static Var callRemoteSel = RT.var("clojure.remoterepl", "call-remote");
  static Var listen = RT.var("clojure.remoterepl", "listen");
  
  public static Object callRemote(Object o, Object seq) {
    if (RemoteRepl.connected) {
      if (ObjC.objc) {
        return callRemoteSelNative(o, RT.seq(seq));
      } else {
        return callRemoteSel.invoke(o, RT.seq(seq));
      }
    } else {
      //throw new RuntimeException("RemoteRepl not connected");
      return null;
    }
  }

  private native static Object callRemoteSelNative(Object o, ISeq seq) /*-[
      return [ReplClient callRemote:o args:seq];
  ]-*/;

  public static void setConnected(boolean connected) {
    RemoteRepl.connected = connected;
    RemoteRef.reset();
  }
  
  public static void listen(int port) {
    try {
      RT.load("clojure/remoterepl");
      listen.invoke(port);
    } catch (Exception e) {
      throw Util.sneakyThrow(e);
    }
  }
  
  public native static void connect(String host, String port) /*-[
    [ReplClient connect:host port:port];
  ]-*/;

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
