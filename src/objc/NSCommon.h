//
//  NSCommon.h
//  sample
//
//  Created by Gal Dolber on 2/4/14.
//  Copyright (c) 2014 clojure-objc. All rights reserved.
//

// Supported types
// -------------------
// float
// long long
// long
// char
// short
// int
// double
// long double
// unsigned long long
// unsigned long
// unsigned char
// unsigned short
// unsigned int
// bool
// CGPoint
// NSRange
// UIEdgeInsets
// CGSize
// CGAffineTransform
// CATransform3D
// UIOffset
// CGRect
// id
// void

#import <Foundation/Foundation.h>
#import "clojure/lang/AFn.h"
#import "clojure/lang/Atom.h"

static ClojureLangAtom *dynamicClasses;

#define to_char(c)\
[(JavaLangCharacter*)c charValue]\

// Necessary for inline functions
static NSMutableDictionary *global_functions;

#define register_fn(n)\
[global_functions setObject:[NSValue valueWithPointer:n] forKey:@#n];\

static const char void_type = 'v';
static const char float_type = 'f';
static const char longlong_type = 'q';
static const char long_type = 'l';
static const char char_type = 'c';
static const char short_type = 's';
static const char int_type = 'i';
static const char double_type = 'd';
static const char longdouble_type = 'D';
static const char ulonglong_type = 'Q';
static const char ulong_type = 'L';
static const char uchar_type = 'C';
static const char ushort_type = 'S';
static const char uint_type = 'I';
static const char bool_type = 'b';
static const char cgpoint_type = 'P';
static const char nsrange_type = 'N';
static const char uiedge_type = 'E';
static const char cgsize_type = 'Z';
static const char cgaffinetransform_type = 'A';
static const char catransform3d_type = 'T';
static const char uioffset_type = 'O';
static const char cgrect_type = 'R';
static const char id_type = 'p';
static const char pointer_type = 'Y';

char signatureToType(const char* c);

id signaturesToTypes(NSMethodSignature* sig, BOOL skip);

const char* makeSignature(id types);

void* callWithArgs(void **argsp, id sself, id types, ClojureLangAFn *fn);

void callWithInvocation(NSInvocation *invocation, id sself, id types, ClojureLangAFn *fn);

@interface NSCommon : NSObject

+(BOOL)cgfloatIsDouble;

+(id)ccall:(id)name types:(id)types args:(id)args;

+(id)invokeFun:(NSString*)fun withSelf:(id)object withSelector:(NSString*)selector withArgs:(id<ClojureLangISeq>)arguments;

+(id)invokeSel:(id)object withSelector:(NSString*)selector withArgs:(id<ClojureLangISeq>)arguments;

+(id)invokeSuperSel:(id)object withDispatchClass:(id)clazz withSelector:(NSString*)selector withArgs:(id<ClojureLangISeq>)arguments;

@end
