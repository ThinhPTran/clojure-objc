//
//  WeakRef.m
//  sample
//
//  Created by Gal Dolber on 2/14/14.
//  Copyright (c) 2014 clojure-objc. All rights reserved.
//

#import "WeakRef.h"
#import "clojure/lang/RT.h"
#import "clojure/lang/Var.h"
#import "clojure/lang/PersistentHashMap.h"
#import "clojure/lang/Atom.h"

static ClojureLangAtom *map;
static ClojureLangVar *assoc;

@implementation WeakRef {
    NSValue *val;
}

+(void)initialize {
    map = [[ClojureLangAtom alloc] initWithId:[ClojureLangPersistentHashMap EMPTY]];
    assoc = [ClojureLangRT varWithNSString:@"clojure.core" withNSString:@"assoc"];
}

-(id)initWith:(id)o {
    self = [super init];
    if (self) {
        val = [NSValue valueWithNonretainedObject:o];
    }
    return self;
}

-(id)deref {
    return [val nonretainedObjectValue];
}

+(WeakRef*)from:(id)o {
    id e = [[map deref] valAtWithId:o];
    if (e == nil) {
        e = [[WeakRef alloc] initWith:o];
        [map swapWithClojureLangIFn:[assoc getRawRoot] withId:o withId:e];
    }
    return e;
}

- (BOOL)isEqual:(id)f {
    if (f != nil && [f isKindOfClass:[WeakRef class]]) {
        return [[(WeakRef*)f deref] isEqual:[self deref]];
    }
    return NO;
}

@end
