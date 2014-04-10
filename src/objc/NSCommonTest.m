//
//  NSCommonTest.m
//  mpos
//
//  Created by Gal Dolber on 4/10/14.
//  Copyright (c) 2014 zuldi. All rights reserved.
//

#import "NSCommonTest.h"
#import "NSCommon.h"
#import "clojure/lang/RT.h"

@implementation NSCommonTest

+(void) voidm {
}

+(float) floatm: (float)f {
    return f;
}

+(long long) longlongm:(long long) l {
    return l;
}

+(long) longm:(long) l {
    return l;
}

+(char) charm:(char) c {
    return c;
}

+(short) shortm:(short)d {
    return d;
}

+(int) intm:(int)i {
    return i;
}

+(double) doublem:(double)d {
    return d;
}

+(long double) longdoublem:(long double)d {
    return d;
}

+(unsigned long long) ulonglongm:(unsigned long long)l {
    return l;
}

+(unsigned long) ulongm:(unsigned long)l {
    return l;
}

+(unsigned char) ucharm:(unsigned char)l {
    return l;
}

+(unsigned short) ushortm:(unsigned short)l {
    return l;
}

+(unsigned int) uintm:(unsigned int)l {
    return l;
}

+(BOOL) boolm:(BOOL)e {
    return e;
}

+(CGPoint) cgpointm:(CGPoint)c {
    return c;
}

+(NSRange) nsrangem:(NSRange)e {
    return e;
}

+(UIEdgeInsets) uiedgem:(UIEdgeInsets)e {
    return e;
}

+(CGSize) cgsizem:(CGSize)e {
    return e;
}

+(CGAffineTransform) cgam:(CGAffineTransform)e {
    return e;
}

+(CATransform3D) cam:(CATransform3D)e {
    return e;
}

+(UIOffset) uioffsetm:(UIOffset)e {
    return e;
}

+(CGRect) cgrectm:(CGRect)e {
    return e;
}

+(id) idm:(id)e {
    return e;
}

+(void*) pointerm:(void*)b {
    return b;
}

#define check(sel, vv) \
{\
id v = vv; \
id r =[NSCommon invokeSel:[NSCommonTest class] withSelector:sel withArgs:[ClojureLangRT listWithId:v]];\
if (![r isEqual:v]) {\
NSLog(@"%@ FAILED. EXPECTED %@ GOT %@", sel, v, r);\
} else { \
} \
}\

//    NSLog(@"%@ PASSED", sel);\

+ (void) runtests {
    CFTimeInterval tt = CACurrentMediaTime();
    for (int n = 0; n < 1000; n++) {
        check(@"floatm:", [ClojureLangRT boxWithFloat:12.2]);
        check(@"longm:", [ClojureLangRT boxWithLong:1223]);
        check(@"charm:", [ClojureLangRT boxWithChar:'s']);
        check(@"shortm:", [ClojureLangRT boxWithShort:23]);
        check(@"intm:", [ClojureLangRT boxWithInt:12]);
        check(@"doublem:", [ClojureLangRT boxWithDouble:23.4]);
        check(@"longdoublem:", [ClojureLangRT boxWithDouble:23.4]);
        check(@"ulonglongm:", [ClojureLangRT boxWithLong:234]);
        check(@"ulongm:", [ClojureLangRT boxWithLong:234]);
        check(@"ucharm:", [ClojureLangRT boxWithChar:'2']);
        check(@"ushortm:", [ClojureLangRT boxWithShort:23]);
        check(@"uintm:", [ClojureLangRT boxWithInt:23]);
        check(@"boolm:", [ClojureLangRT boxWithBoolean:YES]);
        check(@"cgpointm:", [NSValue valueWithCGPoint:CGPointMake(13, 13)]);
        check(@"nsrangem:", [NSValue valueWithRange:NSRangeFromString(@"{1,23}")]);
        check(@"uiedgem:", [NSValue valueWithUIEdgeInsets:UIEdgeInsetsMake(13, 23, 44, 12)]);
        check(@"cgsizem:", [NSValue valueWithCGSize:CGSizeMake(13, 54)]);
        check(@"cgam:", [NSValue valueWithCGAffineTransform:CGAffineTransformMakeRotation(23)]);
        check(@"cam:", [NSValue valueWithCATransform3D:CATransform3DIdentity]);
        check(@"uioffsetm:", [NSValue valueWithUIOffset:UIOffsetMake(24, 55)]);
        check(@"cgrectm:", [NSValue valueWithCGRect:CGRectMake(24, 55, 33, 56)]);
        check(@"idm:", @23);
        id c = @23;
        check(@"pointerm:", [NSValue valueWithPointer:&c]);
    }
    NSLog(@"%f", CACurrentMediaTime() - tt);
}

@end
