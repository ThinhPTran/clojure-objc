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

@implementation WeakRef {
    NSValue *val;
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
    return [[WeakRef alloc] initWith:o];
}

- (BOOL)isEqual:(id)f {
    if (f != nil && [f isKindOfClass:[WeakRef class]]) {
        return [[(WeakRef*)f deref] isEqual:[self deref]];
    }
    return NO;
}

@end
