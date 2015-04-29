/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich 9/5/11 */

package clojure.lang;

import clojure.asm.Opcodes;

public class SourceGenIntrinsics implements Opcodes{
static IPersistentMap ops = RT.map(
 "public static double clojure.lang.Numbers.add(double,double)", "+",
 "public static long clojure.lang.Numbers.and(long,long)", "&",
 "public static long clojure.lang.Numbers.or(long,long)", "|",
 "public static long clojure.lang.Numbers.xor(long,long)", "^",
 "public static double clojure.lang.Numbers.multiply(double,double)", "*",
 "public static double clojure.lang.Numbers.divide(double,double)", "/",
 "public static long clojure.lang.Numbers.remainder(long,long)", "%",
 "public static long clojure.lang.Numbers.shiftLeft(long,long)", "<<",
 "public static long clojure.lang.Numbers.shiftRight(long,long)", ">>",
 "public static long clojure.lang.Numbers.unsignedShiftRight(long,long)", ">>>",
 "public static double clojure.lang.Numbers.minus(double)", "-",
 "public static double clojure.lang.Numbers.minus(double,double)", "-",
 "public static double clojure.lang.Numbers.inc(double)", "1+",
 "public static double clojure.lang.Numbers.dec(double)", "(-1)+",
 "public static long clojure.lang.Numbers.quotient(long,long)", "/",
 "public static int clojure.lang.Numbers.shiftLeftInt(int,int)", "<<",
 "public static int clojure.lang.Numbers.shiftRightInt(int,int)", ">>",
 "public static int clojure.lang.Numbers.unsignedShiftRightInt(int,int)", ">>>",
 "public static int clojure.lang.Numbers.unchecked_int_add(int,int)", "+",
 "public static int clojure.lang.Numbers.unchecked_int_subtract(int,int)", "-",
 "public static int clojure.lang.Numbers.unchecked_int_negate(int)", "-",
 "public static int clojure.lang.Numbers.unchecked_int_inc(int)", "+",
 "public static int clojure.lang.Numbers.unchecked_int_dec(int)", "-",
 "public static int clojure.lang.Numbers.unchecked_int_multiply(int,int)", "*",
 "public static int clojure.lang.Numbers.unchecked_int_divide(int,int)", "/",
 "public static int clojure.lang.Numbers.unchecked_int_remainder(int,int)", "%",
 "public static long clojure.lang.Numbers.unchecked_add(long,long)", "+",
 "public static double clojure.lang.Numbers.unchecked_add(double,double)", "+",
 "public static long clojure.lang.Numbers.unchecked_minus(long)", "-",
 "public static double clojure.lang.Numbers.unchecked_minus(double)", "-",
 "public static double clojure.lang.Numbers.unchecked_minus(double,double)", "-",
 "public static long clojure.lang.Numbers.unchecked_minus(long,long)", "-",
 "public static long clojure.lang.Numbers.unchecked_multiply(long,long)", "*",
 "public static double clojure.lang.Numbers.unchecked_multiply(double,double)", "*",
 "public static double clojure.lang.Numbers.unchecked_inc(double)", "1+",
 "public static long clojure.lang.Numbers.unchecked_inc(long)", "1+",
 "public static double clojure.lang.Numbers.unchecked_dec(double)", "(-1)+",
 "public static long clojure.lang.Numbers.unchecked_dec(long)", "(-1)+",


  "public static short clojure.lang.RT.aget(short[],int)", "aget[]",
  "public static float clojure.lang.RT.aget(float[],int)", "aget[]",
  "public static double clojure.lang.RT.aget(double[],int)", "aget[]",
  "public static int clojure.lang.RT.aget(int[],int)", "aget[]",
  "public static long clojure.lang.RT.aget(long[],int)", "aget[]",
  "public static char clojure.lang.RT.aget(char[],int)", "aget[]",
  "public static byte clojure.lang.RT.aget(byte[],int)", "aget[]",
  "public static boolean clojure.lang.RT.aget(boolean[],int)", "aget[]",
  "public static java.lang.Object clojure.lang.RT.aget(java.lang.Object[],int)", "aget[]",
  "public static int clojure.lang.RT.alength(int[])", "alength[]",
  "public static int clojure.lang.RT.alength(long[])", "alength[]",
  "public static int clojure.lang.RT.alength(char[])", "alength[]",
  "public static int clojure.lang.RT.alength(java.lang.Object[])", "alength[]",
  "public static int clojure.lang.RT.alength(byte[])", "alength[]",
  "public static int clojure.lang.RT.alength(float[])", "alength[]",
  "public static int clojure.lang.RT.alength(short[])", "alength[]",
  "public static int clojure.lang.RT.alength(boolean[])", "alength[]",
  "public static int clojure.lang.RT.alength(double[])", "alength[]",

 "public static double clojure.lang.RT.doubleCast(long)", "(double)",
 "public static double clojure.lang.RT.doubleCast(double)", "",
 "public static double clojure.lang.RT.doubleCast(float)", "(double)",
 "public static double clojure.lang.RT.doubleCast(int)", "(double)",
 "public static double clojure.lang.RT.doubleCast(short)", "(double)",
 "public static double clojure.lang.RT.doubleCast(byte)", "(double)",
 "public static double clojure.lang.RT.uncheckedDoubleCast(double)", "(double)",
 "public static double clojure.lang.RT.uncheckedDoubleCast(float)", "(double)",
 "public static double clojure.lang.RT.uncheckedDoubleCast(long)", "(double)",
 "public static double clojure.lang.RT.uncheckedDoubleCast(int)", "(double)",
 "public static double clojure.lang.RT.uncheckedDoubleCast(short)", "(double)",
 "public static double clojure.lang.RT.uncheckedDoubleCast(byte)", "(double)",
 "public static long clojure.lang.RT.longCast(long)", "",
 "public static long clojure.lang.RT.longCast(short)", "(long)",
 "public static long clojure.lang.RT.longCast(byte)", "(long)",
 "public static long clojure.lang.RT.longCast(int)", "(long)",
  "public static int clojure.lang.RT.uncheckedIntCast(long)", "(int)",
  "public static int clojure.lang.RT.uncheckedIntCast(double)", "(int)",
  "public static int clojure.lang.RT.uncheckedIntCast(byte)", "",
  "public static int clojure.lang.RT.uncheckedIntCast(short)", "",
  "public static int clojure.lang.RT.uncheckedIntCast(char)", "",
  "public static int clojure.lang.RT.uncheckedIntCast(int)", "",
  "public static int clojure.lang.RT.uncheckedIntCast(float)", "(int)",
  "public static long clojure.lang.RT.uncheckedLongCast(short)", "(long)",
  "public static long clojure.lang.RT.uncheckedLongCast(float)", "(long)",
  "public static long clojure.lang.RT.uncheckedLongCast(double)", "(long)",
  "public static long clojure.lang.RT.uncheckedLongCast(byte)", "(long)",
  "public static long clojure.lang.RT.uncheckedLongCast(long)", "",
  "public static long clojure.lang.RT.uncheckedLongCast(int)", "(long)"
);

//map to instructions terminated with comparator for branch to false
static IPersistentMap preds = RT.map(
  "public static boolean clojure.lang.Numbers.lt(double,double)", "<",
  "public static boolean clojure.lang.Numbers.lt(long,long)", "<",
  "public static boolean clojure.lang.Numbers.equiv(double,double)", "==",
  "public static boolean clojure.lang.Numbers.equiv(long,long)", "==",
  "public static boolean clojure.lang.Numbers.lte(double,double)", "<=",
  "public static boolean clojure.lang.Numbers.lte(long,long)", "<=",
  "public static boolean clojure.lang.Numbers.gt(long,long)", ">",
  "public static boolean clojure.lang.Numbers.gt(double,double)", ">",
  "public static boolean clojure.lang.Numbers.gte(long,long)", ">=",
  "public static boolean clojure.lang.Numbers.gte(double,double)", ">=",
  "public static boolean clojure.lang.Util.equiv(long,long)", "==",
  "public static boolean clojure.lang.Util.equiv(boolean,boolean)", "==",
  "public static boolean clojure.lang.Util.equiv(double,double)", "==",

  "public static boolean clojure.lang.Numbers.isZero(double)", "0.0 ==",
  "public static boolean clojure.lang.Numbers.isZero(long)", "0L ==",
  "public static boolean clojure.lang.Numbers.isPos(long)", "0 <",
  "public static boolean clojure.lang.Numbers.isPos(double)", "0 <",
  "public static boolean clojure.lang.Numbers.isNeg(long)", "0 >",
  "public static boolean clojure.lang.Numbers.isNeg(double)", "0 >"
);
}