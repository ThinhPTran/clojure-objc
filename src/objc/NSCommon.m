//
//  NSCommon.m
//  sample
//
//  Created by Gal Dolber on 2/4/14.
//  Copyright (c) 2014 clojure-objc. All rights reserved.
//

#import "NSCommon.h"
#import "clojure/lang/AFn.h"
#import "clojure/lang/RT.h"
#import "clojure/lang/Atom.h"
#import "clojure/lang/PersistentVector.h"
#import "clojure/lang/PersistentHashMap.h"
#import "clojure/lang/Selector.h"
#import "clojure/lang/Var.h"
#import "java/lang/Character.h"
#import "java/lang/Boolean.h"
#import "java/lang/Integer.h"
#import "java/lang/Double.h"
#import "java/lang/Float.h"
#import "java/lang/Long.h"
#import "java/lang/Short.h"
#import "ffi.h"
#import "objc/runtime.h"
#import "objc/message.h"
#import <UIKit/UIKit.h>
#import "WeakRef.h"
#import <dlfcn.h>

static id cons;
static id fconj;
static id assoc;

static bool classIsDynamic(Class clazz) {
    return [ClojureLangRT getWithId:[dynamicClasses deref] withId:NSStringFromClass(clazz)] != nil;
}

#if CGFLOAT_IS_DOUBLE
#define CGFloatFFI &ffi_type_double
#else
#define CGFloatFFI &ffi_type_float
#endif

static ffi_type CGPointFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [3]){CGFloatFFI, CGFloatFFI, NULL}};

static ffi_type CGSizeFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [3]){CGFloatFFI, CGFloatFFI, NULL}};

static ffi_type CGRectFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [3]){&CGPointFFI, &CGSizeFFI, NULL}};

static ffi_type NSRangeFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [3]){&ffi_type_uint, &ffi_type_uint, NULL}};

static ffi_type UIEdgeInsetsFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [5]){CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI, NULL}};

static ffi_type UIOffsetFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [3]){CGFloatFFI, CGFloatFFI, NULL}};

static ffi_type CATransform3DFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [17]){
        CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI,
        CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI,
        CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI,
        CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI,
        NULL}};

static ffi_type CGAffineTransformFFI = (ffi_type){
    .size = 0,
    .alignment = 0,
    .type = FFI_TYPE_STRUCT,
    .elements = (ffi_type * [7]){
        CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI, CGFloatFFI, NULL}};

const char* encode_type(char d) {
    switch (d) {
        case void_type: return @encode(void);
        case float_type: return @encode(float);
        case long_type: return @encode(long);
        case longlong_type: return @encode(long long);
        case char_type: return @encode(char);
        case short_type: return @encode(short);
        case int_type: return @encode(int);
        case double_type: return @encode(double);
        case longdouble_type: return @encode(long double);
        case ulong_type: return @encode(unsigned long);
        case ulonglong_type: return @encode(unsigned long long);
        case uchar_type: return @encode(unsigned char);
        case ushort_type: return @encode(unsigned short);
        case uint_type: return @encode(unsigned int);
        case bool_type: return @encode(BOOL);
        case id_type: return @encode(id);
        case cgpoint_type: return @encode(CGPoint);
        case nsrange_type: return @encode(NSRange);
        case uiedge_type: return @encode(UIEdgeInsets);
        case cgsize_type: return @encode(CGSize);
        case cgaffinetransform_type: return @encode(CGAffineTransform);
        case catransform3d_type: return @encode(CATransform3D);
        case uioffset_type: return @encode(UIOffset);
        case cgrect_type: return @encode(CGRect);
        case pointer_type: return @encode(void*);
    }
    return @encode(id);
}

void * ffi_type_for_type(char type) {
    switch (type) {
        case void_type: return &ffi_type_void;
        case float_type: return &ffi_type_float;
        case longlong_type: return &ffi_type_sint64;
        case long_type: return &ffi_type_slong;
        case char_type: return &ffi_type_schar;
        case short_type: return &ffi_type_sshort;
        case int_type: return &ffi_type_sint;
        case double_type: return &ffi_type_double;
        case longdouble_type: return &ffi_type_longdouble;
        case ulonglong_type: return &ffi_type_uint64;
        case ulong_type: return &ffi_type_ulong;
        case uchar_type: return &ffi_type_uchar;
        case ushort_type: return &ffi_type_ushort;
        case uint_type: return &ffi_type_uint;
        case bool_type: return &ffi_type_schar;
        case cgpoint_type: return &CGPointFFI;
        case nsrange_type: return &NSRangeFFI;
        case uiedge_type: return &UIEdgeInsetsFFI;
        case cgsize_type: return &CGSizeFFI;
        case cgaffinetransform_type: return &CGAffineTransformFFI;
        case catransform3d_type: return &CATransform3DFFI;
        case uioffset_type: return &UIOffsetFFI;
        case cgrect_type: return &CGRectFFI;
        default: return &ffi_type_pointer;
    }
}

int sizeof_type(char c) {
    switch (c) {
        case void_type: return sizeof(void);
        case float_type: return sizeof(float);
        case long_type: return sizeof(long);
        case longlong_type: return sizeof(long long);
        case char_type: return sizeof(char);
        case short_type: return sizeof(short);
        case int_type: return sizeof(int);
        case double_type: return sizeof(double);
        case longdouble_type: return sizeof(long double);
        case ulong_type: return sizeof(unsigned long);
        case ulonglong_type: return sizeof(unsigned long long);
        case uchar_type: return sizeof(unsigned char);
        case ushort_type: return sizeof(unsigned short);
        case uint_type: return sizeof(unsigned int);
        case bool_type: return sizeof(BOOL);
        case cgpoint_type: return sizeof(CGPoint);
        case nsrange_type: return sizeof(NSRange);
        case uiedge_type: return sizeof(UIEdgeInsets);
        case cgsize_type: return sizeof(CGSize);
        case cgaffinetransform_type: return sizeof(CGAffineTransform);
        case catransform3d_type: return sizeof(CATransform3D);
        case uioffset_type: return sizeof(UIOffset);
        case cgrect_type: return sizeof(CGRect);
        case pointer_type: return sizeof(void*);
    }
    return sizeof(id);
}

id signaturesToTypes(NSMethodSignature* sig, BOOL skip) {
    id types = [ClojureLangPersistentVector EMPTY];
    types = [fconj invokeWithId:types withId:[[[JavaLangCharacter alloc] initWithChar:signatureToType([sig methodReturnType])] autorelease]];
    for (int n = 0; n < [sig numberOfArguments]; n++) {
        if (!skip || (n != 0 && n != 1)) {
            types = [fconj invokeWithId:types withId:[[[JavaLangCharacter alloc] initWithChar: signatureToType([sig getArgumentTypeAtIndex:n])] autorelease]];
        }
    }
    return types;
}

// https://developer.apple.com/library/mac/documentation/cocoa/conceptual/ObjCRuntimeGuide/Articles/ocrtTypeEncodings.html
char signatureToType(const char* c) {
    switch (*c) {
        case _C_CONST:                        // const
        case 'n':                             // in
        case 'N':                             // inout
        case 'o':                             // out
        case 'O':                             // bycopy
        case 'R':                             // byref
        case 'V':                             // oneway
            c++;
            return signatureToType(c);
        case _C_FLT: return float_type;
        case _C_LNG_LNG: return longlong_type;
        case _C_LNG: return long_type;
        case _C_CHR: return char_type;
        case _C_SHT: return short_type;
        case _C_INT: return int_type;
        case _C_BOOL: return bool_type;
        case _C_DBL: return double_type;
        case _C_ULNG_LNG: return ulonglong_type;
        case _C_ULNG: return ulong_type;
        case _C_UCHR: return uchar_type;
        case _C_USHT: return ushort_type;
        case _C_UINT: return uint_type;
        case _C_VOID: return void_type;
        case 'D': return longdouble_type;
        case _C_CHARPTR:
        case _C_SEL:
        case _C_PTR:
            return pointer_type;
        case _C_CLASS:
        case _C_ID:
        case _C_UNDEF:
            return id_type;
        case _C_STRUCT_B: {
            if (strcmp(c, @encode(CGPoint)) == 0) {
                return cgpoint_type;
            } else if (strcmp(c, @encode(NSRange)) == 0) {
                return nsrange_type;
            } else if (strcmp(c, @encode(UIEdgeInsets)) == 0) {
                return uiedge_type;
            } else if (strcmp(c, @encode(CGSize)) == 0) {
                return  cgsize_type;
            } else if (strcmp(c, @encode(CGAffineTransform)) == 0) {
                return cgaffinetransform_type;
            } else if (strcmp(c, @encode(CATransform3D)) == 0) {
                return catransform3d_type;
            } else if (strcmp(c, @encode(UIOffset)) == 0) {
                return uioffset_type;
            } else if (strcmp(c, @encode(CGRect)) == 0) {
                return cgrect_type;
            }
        }
    }
    @throw [NSException exceptionWithName:@"Type signature not found" reason:[NSString stringWithUTF8String:c] userInfo:nil];
}

void * malloc_ret(char c) {
    if (c == void_type) {
        return malloc(sizeof_type(id_type));
    }
    return malloc(sizeof_type(c));
}

// https://developer.apple.com/library/ios/documentation/Xcode/Conceptual/iPhoneOSABIReference/iPhoneOSABIReference.pdf
BOOL use_stret(char ret) {
    switch (ret) {
        case cgrect_type:
        case cgpoint_type:
        case nsrange_type:
        case uiedge_type:
        case cgsize_type:
        case cgaffinetransform_type:
        case catransform3d_type:
        case uioffset_type: {
            int size = sizeof_type(ret);
#if TARGET_IPHONE_SIMULATOR
            return size > 8;
#else
            return size >= 8;
#endif
            break;
        }
    }
    return NO;
}

#define pval(type)\
(type)*((type*)argsp[j])\

void* callWithArgs(void **argsp, id sself, id types, ClojureLangAFn *fn) {
    id args = [ClojureLangPersistentVector EMPTY];
    args = [fconj invokeWithId:args withId:[WeakRef from:sself]];
    ClojureLangPersistentVector *typesa = [ClojureLangPersistentVector createWithClojureLangISeq:types];
    char retType = to_char([typesa nthWithInt:0]);
    long typesc = [typesa count];
    for (int n = 2; n < typesc; n++) {
        id val = nil;
        int j = n - 2;
        switch (to_char([typesa nthWithInt:n])) {
            case void_type: {
                break;
            }
            case float_type: {
                val = [[[JavaLangFloat alloc] initWithFloat:pval(float)] autorelease];
                break;
            }
            case longlong_type: {
                val = [[[JavaLangLong alloc] initWithLong:pval(long long)] autorelease];
                break;
            }
            case long_type: {
                val = [[[JavaLangLong alloc] initWithLong:pval(long)] autorelease];
                break;
            }
            case char_type: {
                val = [[[JavaLangCharacter alloc] initWithChar:pval(char)] autorelease];
                break;
            }
            case short_type: {
                val = [[[JavaLangShort alloc] initWithShort:pval(short)] autorelease];
                break;
            }
            case int_type: {
                val = [[[JavaLangInteger alloc] initWithInt:pval(int)] autorelease];
                break;
            }
            case longdouble_type: {
                val = [[[JavaLangDouble alloc] initWithDouble:pval(long double)] autorelease];
                break;
            }
            case double_type: {
                val = [[[JavaLangDouble alloc] initWithDouble:pval(double)] autorelease];
                break;
            }
            case ulong_type: {
                val = [[[JavaLangLong alloc] initWithLong:pval(unsigned long)] autorelease];
                break;
            }
            case ulonglong_type: {
                val = [[[JavaLangLong alloc] initWithLong:pval(unsigned long long)] autorelease];
                break;
            }
            case uchar_type: {
                val = [[[JavaLangCharacter alloc] initWithChar:pval(unsigned char)] autorelease];
                break;
            }
            case ushort_type: {
                val = [[[JavaLangShort alloc] initWithShort:pval(unsigned short)] autorelease];
                break;
            }
            case uint_type: {
                val = [[[JavaLangInteger alloc] initWithInt:pval(unsigned int)] autorelease];
                break;
            }
            case bool_type: {
                if (pval(char) == YES) {
                    val = [JavaLangBoolean getTRUE];
                } else {
                    val = [JavaLangBoolean getFALSE];
                }
                break;
            }
            case id_type: {
                val = pval(void*);
                break;
            }
            case cgpoint_type: {
                val = [NSValue valueWithCGPoint:pval(CGPoint)];
                break;
            }
            case nsrange_type: {
                val = [NSValue valueWithRange:pval(NSRange)];
                break;
            }
            case uiedge_type: {
                val = [NSValue valueWithUIEdgeInsets:pval(UIEdgeInsets)];
                break;
            }
            case cgsize_type: {
                val = [NSValue valueWithCGSize:pval(CGSize)];
                break;
            }
            case cgaffinetransform_type: {
                val = [NSValue valueWithCGAffineTransform:pval(CGAffineTransform)];
                break;
            }
            case catransform3d_type: {
                val = [NSValue valueWithCATransform3D:pval(CATransform3D)];
                break;
            }
            case uioffset_type: {
                val = [NSValue valueWithUIOffset:pval(UIOffset)];
                break;
            }
            case cgrect_type: {
                val = [NSValue valueWithCGRect:pval(CGRect)];
                break;
            }
            case pointer_type: {
                val = [NSValue valueWithPointer:pval(void*)];
                break;
            }
            default: @throw [NSException exceptionWithName:@"Error"
                                                    reason:[NSString stringWithFormat:@"%@",
                                                            [typesa nthWithInt:n]] userInfo:nil];
        }
        args = [fconj invokeWithId:args withId:val];
    }
    
    id v = [fn applyToWithClojureLangISeq:[ClojureLangRT seqWithId:args]];
    void * ret;
    
    switch (retType) {
        case void_type: {
            id r = nil;
            ret = &r;
            break;
        }
        case float_type: {
            float o = [ClojureLangRT floatCastWithId:v];
            ret = &o;
            break;
        }
        case longlong_type: {
            long long o = [ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case long_type: {
            long o = (long)[ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case char_type: {
            char o = [ClojureLangRT charCastWithId:v];
            ret = &o;
            break;
        }
        case short_type: {
            short o = [ClojureLangRT shortCastWithId:v];
            ret = &o;
            break;
        }
        case int_type: {
            int o = [ClojureLangRT intCastWithId:v];
            ret = &o;
            break;
        }
        case longdouble_type: {
            long double o = [ClojureLangRT doubleCastWithId:v];
            ret = &o;
            break;
        }
        case double_type: {
            double o = [ClojureLangRT doubleCastWithId:v];
            ret = &o;
            break;
        }
        case ulong_type: {
            unsigned long o = (unsigned long)[ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case ulonglong_type: {
            unsigned long long o = [ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case uchar_type: {
            unsigned char o = [ClojureLangRT charCastWithId:v];
            ret = &o;
            break;
        }
        case ushort_type: {
            unsigned short o = [ClojureLangRT shortCastWithId:v];
            ret = &o;
            break;
        }
        case uint_type: {
            unsigned int o = [ClojureLangRT intCastWithId:v];
            ret = &o;
            break;
        }
        case bool_type: {
            BOOL o = [ClojureLangRT booleanCastWithId:v];
            ret = &o;
            break;
        }
        case id_type: {
            ret = &v;
            break;
        }
        case cgpoint_type: {
            CGPoint o = [((NSValue*) v) CGPointValue];
            ret = &o;
            break;
        }
        case nsrange_type: {
            NSRange o = [((NSValue*) v) rangeValue];
            ret = &o;
            break;
        }
        case uiedge_type: {
            UIEdgeInsets o = [((NSValue*) v) UIEdgeInsetsValue];
            ret = &o;
            break;
        }
        case cgsize_type: {
            CGSize o = [((NSValue*) v) CGSizeValue];
            ret = &o;
            break;
        }
        case cgaffinetransform_type: {
            CGAffineTransform o = [((NSValue*) v) CGAffineTransformValue];
            ret = &o;
            break;
        }
        case catransform3d_type: {
            CATransform3D o = [((NSValue*) v) CATransform3DValue];
            ret = &o;
            break;
        }
        case uioffset_type: {
            UIOffset o = [((NSValue*) v) UIOffsetValue];
            ret = &o;
            break;
        }
        case cgrect_type: {
            CGRect o = [((NSValue*) v) CGRectValue];
            ret = &o;
            break;
        }
        case pointer_type: {
            void* o = [((NSValue*) v) pointerValue];
            ret = &o;
            break;
        }
        default: {
            @throw [NSException exceptionWithName:@"Missing type" reason:[NSString stringWithFormat:@"%c", retType] userInfo:nil];
        }
    }
    
    return ret;
}

void callWithInvocation(NSInvocation *invocation, id sself, id types, ClojureLangAFn *fn) {
    id args = [ClojureLangPersistentVector EMPTY];
    args = [fconj invokeWithId:args withId:[WeakRef from:sself]];
    IOSObjectArray *typesa = [ClojureLangRT toArrayWithId:types];
    char retType = to_char([typesa objectAtIndex:0]);
    long typesc = [typesa count];
    for (int n = 1; n < typesc; n++) {
        id val = nil;
        int j = n + 1;
        switch (to_char([typesa objectAtIndex:n])) {
            case void_type: {
                break;
            }
            case float_type: {
                float v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangFloat alloc] initWithFloat:v] autorelease];
                break;
            }
            case longlong_type: {
                long long v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangLong alloc] initWithLong:v] autorelease];
                break;
            }
            case long_type: {
                long v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangLong alloc] initWithLong:v] autorelease];
                break;
            }
            case char_type: {
                char v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangCharacter alloc] initWithChar:v] autorelease];
                break;
            }
            case short_type: {
                short v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangShort alloc] initWithShort:v] autorelease];
                break;
            }
            case int_type: {
                int v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangInteger alloc] initWithInt:v] autorelease];
                break;
            }
            case longdouble_type: {
                long double v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangDouble alloc] initWithDouble:v] autorelease];
                break;
            }
            case double_type: {
                double v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangDouble alloc] initWithDouble:v] autorelease];
                break;
            }
            case ulong_type: {
                unsigned long v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangLong alloc] initWithLong:v] autorelease];
                break;
            }
            case ulonglong_type: {
                unsigned long long v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangLong alloc] initWithLong:v] autorelease];
                break;
            }
            case uchar_type: {
                unsigned char v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangCharacter alloc] initWithChar:v] autorelease];
                break;
            }
            case ushort_type: {
                unsigned short v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangShort alloc] initWithShort:v] autorelease];
                break;
            }
            case uint_type: {
                unsigned int v;
                [invocation getArgument:&v atIndex: j];
                val = [[[JavaLangInteger alloc] initWithInt:v] autorelease];
                break;
            }
            case bool_type: {
                char v;
                [invocation getArgument:&v atIndex: j];
                if (v == YES) {
                    val = [JavaLangBoolean getTRUE];
                } else {
                    val = [JavaLangBoolean getFALSE];
                }
                break;
            }
            case id_type: {
                void * v;
                [invocation getArgument:&v atIndex:j];
                val = v;
                break;
            }
            case cgpoint_type: {
                CGPoint v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithCGPoint:v];
                break;
            }
            case nsrange_type: {
                NSRange v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithRange:v];
                break;
            }
            case uiedge_type: {
                UIEdgeInsets v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithUIEdgeInsets:v];
                break;
            }
            case cgsize_type: {
                CGSize v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithCGSize:v];
                break;
            }
            case cgaffinetransform_type: {
                CGAffineTransform v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithCGAffineTransform:v];
                break;
            }
            case catransform3d_type: {
                CATransform3D v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithCATransform3D:v];
                break;
            }
            case uioffset_type: {
                UIOffset v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithUIOffset:v];
                break;
            }
            case cgrect_type: {
                CGRect v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithCGRect:v];
                break;
            }
            case pointer_type: {
                void* v;
                [invocation getArgument:&v atIndex: j];
                val = [NSValue valueWithPointer:v];
                break;
            }
            default: @throw [NSException exceptionWithName:@"Error"
                                                    reason:[NSString stringWithFormat:@"%@",
                                                            [typesa objectAtIndex:n]] userInfo:nil];
        }
        args = [fconj invokeWithId:args withId:val];
    }
    id v = [fn applyToWithClojureLangISeq:[ClojureLangRT seqWithId:args]];
    
    void * ret;
    
    switch (retType) {
        case void_type: {
            id r = nil;
            ret = &r;
            break;
        }
        case float_type: {
            float o = [ClojureLangRT floatCastWithId:v];
            ret = &o;
            break;
        }
        case longlong_type: {
            long long o = [ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case long_type: {
            long o = (long)[ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case char_type: {
            char o = [ClojureLangRT charCastWithId:v];
            ret = &o;
            break;
        }
        case short_type: {
            short o = [ClojureLangRT shortCastWithId:v];
            ret = &o;
            break;
        }
        case int_type: {
            int o = [ClojureLangRT intCastWithId:v];
            ret = &o;
            break;
        }
        case double_type: {
            double o = [ClojureLangRT doubleCastWithId:v];
            ret = &o;
            break;
        }
        case longdouble_type: {
            long double o = [ClojureLangRT doubleCastWithId:v];
            ret = &o;
            break;
        }
        case ulong_type: {
            unsigned long o = (unsigned long)[ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case ulonglong_type: {
            unsigned long long o = [ClojureLangRT longCastWithId:v];
            ret = &o;
            break;
        }
        case uchar_type: {
            unsigned char o = [ClojureLangRT charCastWithId:v];
            ret = &o;
            break;
        }
        case ushort_type: {
            unsigned short o = [ClojureLangRT shortCastWithId:v];
            ret = &o;
            break;
        }
        case uint_type: {
            unsigned int o = [ClojureLangRT intCastWithId:v];
            ret = &o;
            break;
        }
        case bool_type: {
            BOOL o = [ClojureLangRT booleanCastWithId:v];
            ret = &o;
            break;
        }
        case id_type: {
            ret = &v;
            break;
        }
        case cgpoint_type: {
            CGPoint o = [((NSValue*) v) CGPointValue];
            ret = &o;
            break;
        }
        case nsrange_type: {
            NSRange o = [((NSValue*) v) rangeValue];
            ret = &o;
            break;
        }
        case uiedge_type: {
            UIEdgeInsets o = [((NSValue*) v) UIEdgeInsetsValue];
            ret = &o;
            break;
        }
        case cgsize_type: {
            CGSize o = [((NSValue*) v) CGSizeValue];
            ret = &o;
            break;
        }
        case cgaffinetransform_type: {
            CGAffineTransform o = [((NSValue*) v) CGAffineTransformValue];
            ret = &o;
            break;
        }
        case catransform3d_type: {
            CATransform3D o = [((NSValue*) v) CATransform3DValue];
            ret = &o;
            break;
        }
        case uioffset_type: {
            UIOffset o = [((NSValue*) v) UIOffsetValue];
            ret = &o;
            break;
        }
        case cgrect_type: {
            CGRect o = [((NSValue*) v) CGRectValue];
            ret = &o;
            break;
        }
        case pointer_type: {
            void* o = [((NSValue*) v) pointerValue];
            ret = &o;
            break;
        }
        default: {
            @throw [NSException exceptionWithName:@"Missing type" reason:[NSString stringWithFormat:@"%c", retType] userInfo:nil];
        }
    }
    if (retType != 'v') {
        [invocation setReturnValue:ret];
    }
}

id boxValue(void* val, char type) {
    id result;
    switch (type) {
        case void_type: {
            return [NSNull null];
        }
        case float_type: {
            float v = *(float*)val;
            return [[[JavaLangFloat alloc] initWithFloat:v] autorelease];
        }
        case long_type: {
            long v = (long)*(long*)val;
            return [[[JavaLangLong alloc] initWithLong:v] autorelease];
        }
        case longlong_type: {
            long long v = *(long long*)val;
            return [[[JavaLangLong alloc] initWithLong:v] autorelease];
        }
        case char_type: {
            if (*(char*)val == YES) {
                return [JavaLangBoolean getTRUE];
            } else if (*(char*)val == NO) {
                return [JavaLangBoolean getFALSE];
            } else {
                return [[[JavaLangCharacter alloc] initWithChar:*(char*)val] autorelease];
            }
        }
        case short_type: {
            short v = *(short*)val;
            return [[[JavaLangShort alloc] initWithShort:v] autorelease];
            break;
        }
        case int_type: {
            int v = *(int*)val;
            return [[[JavaLangInteger alloc] initWithInt:v] autorelease];
        }
        case double_type: {
#if TARGET_CPU_ARM
            double v;
            memcpy(val, &v, sizeof(double));
#else
            double v = *(double*)val;
#endif
            return [[[JavaLangDouble alloc] initWithDouble:v] autorelease];
        }
        case longdouble_type: {
#if TARGET_CPU_ARM
            long double v;
            memcpy(val, &v, sizeof(long double));
#else
            long double v = *(long double*)val;
#endif
            return [[[JavaLangDouble alloc] initWithDouble:v] autorelease];
        }
        case ulong_type: {
            unsigned long v = (unsigned long)*(unsigned long*)val;
            return [[[JavaLangLong alloc] initWithLong:v] autorelease];
        }
        case ulonglong_type: {
            unsigned long long v = (unsigned long long)*(unsigned long long*)val;
            return [[[JavaLangLong alloc] initWithLong:v] autorelease];
        }
        case uchar_type: {
            unsigned char v = *(unsigned char*)val;
            return [[[JavaLangCharacter alloc] initWithChar:v] autorelease];
        }
        case ushort_type: {
            unsigned short v = *(unsigned short*)val;
            return [[[JavaLangShort alloc] initWithShort:v] autorelease];
        }
        case uint_type: {
            unsigned int v = *(unsigned int*)val;
            return [[[JavaLangInteger alloc] initWithInt:v] autorelease];
        }
        case bool_type: {
            return *(char*)val == YES ? [JavaLangBoolean getTRUE] : [JavaLangBoolean getFALSE];
        }
        case cgpoint_type: {
            CGPoint v = *(CGPoint*)val;
            return [NSValue valueWithCGPoint:v];
        }
        case nsrange_type: {
            NSRange v = *(NSRange*)val;
            return [NSValue valueWithRange:v];
        }
        case uiedge_type: {
            UIEdgeInsets v = *(UIEdgeInsets*)val;
            return [NSValue valueWithUIEdgeInsets:v];
        }
        case cgsize_type: {
            CGSize v = *(CGSize*)val;
            return [NSValue valueWithCGSize:v];
        }
        case cgaffinetransform_type: {
            CGAffineTransform v = *(CGAffineTransform*)val;
            return [NSValue valueWithCGAffineTransform:v];
        }
        case catransform3d_type: {
            CATransform3D v = *(CATransform3D*)val;
            return [NSValue valueWithCATransform3D:v];
        }
        case uioffset_type: {
            UIOffset v = *(UIOffset*)val;
            return [NSValue valueWithUIOffset:v];
        }
        case cgrect_type: {
            CGRect v = *(CGRect*)val;
            return [NSValue valueWithCGRect:v];
        }
        case pointer_type: {
            void * v = *(void**)val;
            return [NSValue valueWithPointer:v];
        }
        default: {
            return *(void**)val;
        }
    }
}

#define make_pointer(e,type)\
type o = e;\
type *p = malloc(sizeof(type));\
*p = o;\
argument_values[n] = p;\
break;\

static id ccallWithFn(void* fn, ClojureLangPersistentVector *types, id args) {
    if (![args isKindOfClass:[ClojureLangPersistentVector class]]) {
        args = [ClojureLangPersistentVector createWithClojureLangISeq:args];
    }
    char retType = to_char([types nthWithInt:0]);
    void *result_value = malloc_ret(retType);
    
    long count = [(ClojureLangPersistentVector*)args count];
    ffi_type **argument_types = (ffi_type **) malloc ((count + 1) * sizeof(ffi_type *));
    void **argument_values = (void **) malloc ((count + 1) * sizeof(void *));
    for (int n=0; n < count; n++) {
        char type = to_char([types nthWithInt:n+1]);
        argument_types[n] = ffi_type_for_type(type);
        id v = [args nthWithInt: n];
        switch (type) {
            case void_type: {
                make_pointer([NSNull null], id);
            }
            case float_type: {
                make_pointer([ClojureLangRT floatCastWithId:v], float);
            }
            case longlong_type: {
                make_pointer([ClojureLangRT longCastWithId:v], long long);
            }
            case long_type: {
                make_pointer((long)[ClojureLangRT longCastWithId:v], long);
            }
            case char_type: {
                if ([v isKindOfClass:[JavaLangBoolean class]]) {
                    make_pointer([ClojureLangRT booleanCastWithId:v], BOOL);
                } else {
                    make_pointer([ClojureLangRT charCastWithId:v], char);
                }
            }
            case short_type: {
                make_pointer([ClojureLangRT shortCastWithId:v], short);
            }
            case int_type: {
                make_pointer([ClojureLangRT intCastWithId:v], int);
            }
            case double_type: {
                make_pointer([ClojureLangRT doubleCastWithId:v], double);
            }
            case longdouble_type: {
                make_pointer([ClojureLangRT doubleCastWithId:v], long double);
            }
            case ulong_type: {
                make_pointer((unsigned long)[ClojureLangRT longCastWithId:v], unsigned long);
            }
            case ulonglong_type: {
                make_pointer([ClojureLangRT longCastWithId:v], unsigned long long);
            }
            case uchar_type: {
                make_pointer([ClojureLangRT charCastWithId:v], unsigned char);
            }
            case ushort_type: {
                make_pointer([ClojureLangRT shortCastWithId:v], unsigned short);
            }
            case uint_type: {
                make_pointer([ClojureLangRT intCastWithId:v], unsigned int);
            }
            case bool_type: {
                make_pointer([ClojureLangRT booleanCastWithId:v], BOOL);
            }
            case id_type: {
                make_pointer([v isKindOfClass:[WeakRef class]] ? [(WeakRef*)v deref] : v, id);
            }
            case cgpoint_type: {
                make_pointer([((NSValue*) v) CGPointValue], CGPoint);
            }
            case nsrange_type: {
                make_pointer([((NSValue*) v) rangeValue], NSRange);
            }
            case uiedge_type: {
                make_pointer([((NSValue*) v) UIEdgeInsetsValue], UIEdgeInsets);
            }
            case cgsize_type: {
                make_pointer([((NSValue*) v) CGSizeValue], CGSize);
            }
            case cgaffinetransform_type: {
                make_pointer([((NSValue*) v) CGAffineTransformValue], CGAffineTransform);
                break;
            }
            case catransform3d_type: {
                make_pointer([((NSValue*) v) CATransform3DValue], CATransform3D);
            }
            case uioffset_type: {
                make_pointer([((NSValue*) v) UIOffsetValue], UIOffset);
            }
            case cgrect_type: {
                make_pointer([((NSValue*) v) CGRectValue], CGRect);
            }
            case pointer_type: {
                if ([v isKindOfClass:[ClojureLangSelector class]]) {
                    make_pointer(NSSelectorFromString([(ClojureLangSelector*)v getName]), SEL);
                } else {
                    make_pointer([((NSValue*) v) pointerValue], void*);
                }
            }
            default: {
                @throw [NSException exceptionWithName:@"Type not found" reason:[NSString stringWithFormat:@"%c", type] userInfo:nil];
            }
        }
    }
    
    ffi_cif c;
    ffi_type *result_type = ffi_type_for_type(retType);
    int status = ffi_prep_cif(&c, FFI_DEFAULT_ABI, (unsigned int) count, result_type, argument_types);
    if (status != FFI_OK) {
        NSLog(@"Failed to prepare cif structure");
    }
    ffi_call(&c, fn, result_value, argument_values);
    for (int n=0; n < count; n++) {
        free(argument_values[n]);
    }
    free(argument_types);
    free(argument_values);
    
    id result = boxValue(result_value, retType);
    free(result_value);
    return result;
}

const char* makeSignature(id types) {
    BOOL first = YES;
    NSString *s = @"";
    IOSObjectArray *array = [ClojureLangRT toArrayWithId:types];
    long c = [array count];
    for (int n = 0; n < c; n++) {
        s = [s stringByAppendingString:[NSString stringWithFormat:@"%s", encode_type(to_char([array objectAtIndex:n]))]];
        if (first) {
            s = [s stringByAppendingString:@"@:"];
            first = NO;
        }
    }
    return [s UTF8String];
}


@implementation NSCommon

+(BOOL)cgfloatIsDouble {
#if CGFLOAT_IS_DOUBLE
    return YES;
#else
    return NO;
#endif
}

+(void)initialize {
    assoc = [ClojureLangRT varWithNSString:@"clojure.core" withNSString:@"assoc"];
    cons = [ClojureLangRT varWithNSString:@"clojure.core" withNSString:@"cons"];
    fconj = [ClojureLangRT varWithNSString:@"clojure.core" withNSString:@"conj"];
    global_functions = [NSMutableDictionary new];
    register_fn(objc_msgSend);
#ifndef __arm64__
    register_fn(objc_msgSend_stret);
#endif
    register_fn(CGRectMake);
    register_fn(CGPointMake);
    register_fn(CGSizeMake);
    register_fn(CGVectorMake);
    register_fn(UIEdgeInsetsMake);
    register_fn(UIEdgeInsetsInsetRect);
    register_fn(UIOffsetMake);
    register_fn(UIEdgeInsetsEqualToEdgeInsets);
    register_fn(UIOffsetEqualToOffset);
}

+(id)ccall:(id)name types:(ClojureLangPersistentVector*)types args:(id)args {
    NSValue *val = [global_functions objectForKey:name];
    void *fn;
    if (val != nil) {
        fn = [val pointerValue];
    } else {
        fn = dlsym(RTLD_DEFAULT, [name UTF8String]);
        if (fn == nil) {
            @throw [NSException exceptionWithName:@"Function not found" reason:name userInfo:nil];
        }
        [global_functions setObject:[NSValue valueWithPointer:fn] forKey:name];
    }
    
    return ccallWithFn(fn, types, args);
}

#if TARGET_CPU_X86
#define structmsgf(params, ...) \
case float_type: { \
float r = objc_msgSend_fpret(object, sel, ##__VA_ARGS__); \
return [[[JavaLangFloat alloc] initWithFloat:r] autorelease]; \
} \
case double_type: { \
double r = objc_msgSend_fpret(object, sel, ##__VA_ARGS__); \
return [[[JavaLangDouble alloc] initWithDouble:r] autorelease]; \
} \
case longdouble_type: { \
long double r = objc_msgSend_fpret(object, sel, ##__VA_ARGS__); \
return [[[JavaLangDouble alloc] initWithDouble:r] autorelease]; \
}
#elif TARGET_CPU_X86_64
#define structmsgf(params, ...) \
case longdouble_type: { \
long double r = objc_msgSend_fpret(object, sel, ##__VA_ARGS__); \
return [[[JavaLangDouble alloc] initWithDouble:r] autorelease]; \
}
#else
#define structmsgf(params, ...)
#endif

#define structmsg(params, ...) \
switch (ret) { \
case cgrect_type: { \
return [NSValue valueWithCGRect:((CGRect(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case cgsize_type: {\
return [NSValue valueWithCGSize:((CGSize(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case cgpoint_type: {\
return [NSValue valueWithCGPoint:((CGPoint(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case nsrange_type: {\
return [NSValue valueWithRange:((NSRange(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case uiedge_type: {\
return [NSValue valueWithUIEdgeInsets:((UIEdgeInsets(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case cgaffinetransform_type: {\
return [NSValue valueWithCGAffineTransform:((CGAffineTransform(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case catransform3d_type: {\
return [NSValue valueWithCATransform3D:((CATransform3D(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
case uioffset_type: {\
return [NSValue valueWithUIOffset:((UIOffset(*)params)fun)(object, sel, ##__VA_ARGS__)];\
}\
structmsgf(params, ##__VA_ARGS__) \
default: { \
void *r = objc_msgSend(object, sel, ##__VA_ARGS__); \
return boxValue(&r, ret); \
} \
}\

+ (id) invokeSel:(id)object withSelector:(NSString*)selector withArgs:(id<ClojureLangISeq>)arguments {
    SEL sel = NSSelectorFromString(selector);
    NSMethodSignature *sig = [([object isKindOfClass:[WeakRef class]] ? [(WeakRef*)object deref] : object) methodSignatureForSelector:sel];
    if (sig == nil) {
        @throw([NSException exceptionWithName:@"Error invoking objc method. Selector not found" reason:selector userInfo:nil]);
    }
    char ret = signatureToType([sig methodReturnType]);
#ifndef __arm64__
    bool stret = use_stret(ret);
#else
    bool stret = NO;
#endif
    object = [object isKindOfClass:[WeakRef class]] ? [object deref] : object;
#ifndef __arm64__
    void *fun;
    if (stret) {
        fun = objc_msgSend_stret;
    } else {
#if TARGET_CPU_X86 || TARGET_CPU_X86_64
        if (ret == float_type) {
            fun = objc_msgSend_fpret;
        } else {
#endif
            fun = objc_msgSend;
#if TARGET_CPU_X86 || TARGET_CPU_X86_64
        }
#endif
        
    }
#else
    void *fun;
    if (ret == float_type) {
        fun = objc_msgSend_fpret;
    } else {
        fun = objc_msgSend;
    }
#endif
    // TODO: ulonglong return fails with structmsg
    if (ret != ulonglong_type) {
        switch ([arguments count]) {
            case 0: {
                structmsg((id, SEL));
                break;
            }
                
            case 1: {
                id v = [arguments first];
                switch (signatureToType([sig getArgumentTypeAtIndex:2])) {
                    case id_type: {
                        structmsg((id, SEL, id), [v isKindOfClass:[WeakRef class]] ? [(WeakRef*)v deref] : v);
                    }
                    case int_type: {
                        structmsg((id, SEL, int), [ClojureLangRT intCastWithId:v]);
                    }
                    case uint_type: {
                        structmsg((id, SEL, unsigned int), [ClojureLangRT intCastWithId:v]);
                    }
                    case ulonglong_type: {
                        structmsg((id, SEL, unsigned long long), (unsigned long long)[ClojureLangRT longCastWithId:v]);
                    }
                    case ulong_type: {
                        structmsg((id, SEL, unsigned long), (unsigned long)[ClojureLangRT longCastWithId:v]);
                    }
                    case long_type: {
                        structmsg((id, SEL, long), (long)[ClojureLangRT longCastWithId:v]);
                    }
                    case longlong_type: {
                        structmsg((id, SEL, long long), (long long)[ClojureLangRT longCastWithId:v]);
                    }
                    case float_type: {
                        float f = [ClojureLangRT floatCastWithId:v];
                        structmsg((id, SEL, float), *(int*)&f);
                    }
                    case uchar_type: {
                        structmsg((id, SEL, unsigned char), (unsigned char)[ClojureLangRT charCastWithId:v]);
                    }
                    case char_type: {
                        structmsg((id, SEL, char), v == [JavaLangBoolean getTRUE] ? YES :
                                  (v == [JavaLangBoolean getFALSE] ? NO : [ClojureLangRT charCastWithId:v]));
                    }
                    case ushort_type: {
                        structmsg((id, SEL, unsigned short), (unsigned short)[ClojureLangRT shortCastWithId:v]);
                    }
                    case short_type: {
                        structmsg((id, SEL, short), [ClojureLangRT shortCastWithId:v]);
                    }
                    case double_type: {
                        structmsg((id, SEL, double), [ClojureLangRT doubleCastWithId:v]);
                    }
                    case longdouble_type: {
                        structmsg((id, SEL, long double), (long double)[ClojureLangRT doubleCastWithId:v]);
                    }
                    case bool_type: {
                        structmsg((id, SEL, bool), [ClojureLangRT booleanCastWithId:v]);
                    }
                    case pointer_type: {
                        structmsg((id, SEL, void*), [v isKindOfClass:[ClojureLangSelector class]] ? NSSelectorFromString  ([(ClojureLangSelector*)v getName]) : [(NSValue*)v pointerValue]);
                    }
                    case cgpoint_type: {
                        structmsg((id, SEL, CGPoint), [(NSValue*)v CGPointValue]);
                    }
                    case nsrange_type: {
                        structmsg((id, SEL, NSRange), [(NSValue*)v rangeValue]);
                    }
                    case uiedge_type: {
                        structmsg((id, SEL, UIEdgeInsets), [(NSValue*)v UIEdgeInsetsValue]);
                    }
                    case cgsize_type: {
                        structmsg((id, SEL, CGSize), [(NSValue*)v CGSizeValue]);
                    }
                    case cgaffinetransform_type: {
                        structmsg((id, SEL, CGAffineTransform), [(NSValue*)v CGAffineTransformValue]);
                    }
                    case catransform3d_type: {
                        structmsg((id, SEL, CATransform3D), [(NSValue*)v CATransform3DValue]);
                    }
                    case uioffset_type: {
                        structmsg((id, SEL, UIOffset), [(NSValue*)v UIOffsetValue]);
                    }
                    case cgrect_type: {
                        structmsg((id, SEL, CGRect), [(NSValue*)v CGRectValue]);
                    }
                }
                break;
            }
        }
    }
    
    if ([selector isEqualToString:@"ccall:types:args:"]) {
        return [NSCommon ccall:[ClojureLangRT nthFromWithId:arguments withInt:0] types:[ClojureLangRT nthFromWithId:arguments withInt:1] args:[ClojureLangRT nthFromWithId:arguments withInt:2]];
    }
    return ccallWithFn(fun, signaturesToTypes(sig, NO), [cons invokeWithId:object withId:[cons invokeWithId:[NSValue valueWithPointer:sel] withId:arguments]]);
}

+ (id) invokeSuperSel:(id)object withDispatchClass:(id)clazz withSelector:(NSString*)selector
             withArgs:(id<ClojureLangISeq>)arguments {
    SEL sel = NSSelectorFromString(selector);
    clazz = clazz == nil ? [object superclass] : [NSClassFromString(clazz) superclass];
    if (classIsDynamic(clazz)) {
        objc_setAssociatedObject(object, "__dispatch_class__", clazz, 1);
    }
    struct objc_super superData = {object, clazz};
    
    NSMethodSignature *sig = [object methodSignatureForSelector:sel];
    if (sig == nil) {
        @throw([NSException exceptionWithName:@"Error invoking superclass objc method. Selector not found" reason:selector userInfo:nil]);
    }
    id types = [assoc invokeWithId:signaturesToTypes(sig, NO) withId:[[[JavaLangInteger alloc] initWithInt:1] autorelease] withId:[[[JavaLangCharacter alloc] initWithChar:pointer_type] autorelease]];
    id args = [cons invokeWithId:[NSValue valueWithPointer:sel] withId:arguments];
    args = [cons invokeWithId:[NSValue valueWithPointer:(void*)&superData] withId:args];
#ifndef __arm64__
    void *s;
    if (use_stret(signatureToType([sig methodReturnType]))) {
        s = objc_msgSendSuper_stret;
    } else {
        s = objc_msgSendSuper;
    }
#else
    void *s = FFI_FN(objc_msgSendSuper);
#endif
    return ccallWithFn(s, types, args);
}

+ (id) invokeFun:(NSString*)fun withSelf:(id)object withSelector:(NSString*)selector withArgs:(id<ClojureLangISeq>)arguments {
    SEL sel = NSSelectorFromString(selector);
    NSMethodSignature *sig = [([object isKindOfClass:[WeakRef class]] ? [(WeakRef*)object deref] : object) methodSignatureForSelector:sel];
    if (sig == nil) {
        @throw([NSException exceptionWithName:@"Error invoking objc method. Selector not found" reason:selector userInfo:nil]);
    }
    id args = [cons invokeWithId:[NSValue valueWithPointer:NSSelectorFromString(selector)] withId:arguments];
    args = [cons invokeWithId:object withId:args];
    return [NSCommon ccall:fun types:signaturesToTypes(sig, NO) args:args];
}

@end
