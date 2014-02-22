package clojure;

import java.io.IOException;

import clojure.lang.RT;
import clojure.lang.Var;

public class Demo {

  public static void main(String[] args) throws ClassNotFoundException, IOException {
    long c = System.currentTimeMillis();
//    Var v = core_parents.VAR;
    RT.list();
//    System.out.println(core_reduce.VAR.invoke(core__PLUS_.VAR.getRawRoot(),
//        RT.vector(1, 2, 3, 4)));
    System.out.println(System.currentTimeMillis() - c);
  }
}
