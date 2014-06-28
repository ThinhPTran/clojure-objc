//
//  ReplClient.m
//  mpos
//
//  Created by Gal Dolber on 4/12/14.
//  Copyright (c) 2014 zuldi. All rights reserved.
//

#import "ReplClient.h"
#import "clojure/lang/RemoteRepl.h"
#import "clojure/lang/RT.h"
#import "clojure/lang/AFn.h"
#import "clojure/lang/PersistentHashMap.h"
#import "clojure/lang/Var.h"
#import "clojure/lang/Atom.h"
#import "NSSocketImpl.h"
#import "clojure/core_keyword.h"
#import "clojure/core_pr_str.h"
#import "clojure/core_vector.h"
#import "clojure/core_vec.h"
#import "clojure/core_assoc.h"
#import "clojure/core_dissoc.h"

static NSSocketImpl *nssocket;
static NSSocketImpl *nssocket2;

@implementation ReplClient

+(void) processCall2:(NSSocketImpl*)socket msg:(id)msg {
    @try {
        id i = [ClojureLangRT firstWithId:msg];
        id s = [ClojureLangRT secondWithId:msg];
        id args = [ClojureLangRT thirdWithId:msg];
        id r = [(ClojureLangAFn*)s applyToWithClojureLangISeq:[ClojureLangRT seqWithId:args]];
        [socket println:[Clojurecore_pr_str_get_VAR_() invokeWithId:[Clojurecore_vector_get_VAR_() invokeWithId:nil withId:r]]];
    }
    @catch (NSException *exception) {
        [socket println:[Clojurecore_pr_str_get_VAR_() invokeWithId:[exception callStackSymbols]]];
        NSLog(@"%@ %@", exception, [exception callStackSymbols]);
    }
}

+(void) processCall:(NSSocketImpl*)socket msg:(id)msg {
    if ([NSThread isMainThread]) {
        [ReplClient processCall2:socket msg:msg];
    } else {
        dispatch_sync(dispatch_get_main_queue(), ^{
            [ReplClient processCall2:socket msg:msg];
        });
    }
}

+(void) connect: (NSString*) host {
    nssocket = [[NSSocketImpl alloc] initWithHost:host withPort:@"35813"];
    nssocket2 = [[NSSocketImpl alloc] initWithHost:host withPort:@"35814"];
    [ClojureLangRemoteRepl setConnectedWithBoolean:YES];
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        while (true) {
            [ReplClient processCall:nssocket msg:[ClojureLangRT readStringWithNSString:[nssocket read]]];
        }
    });
}

+(id) callRemote:(id)sel args:(id)args {
    args = [Clojurecore_vec_get_VAR_() invokeWithId:args];
    id i = [[ReplClient randomUUID] description];
    [nssocket2 println:[Clojurecore_pr_str_get_VAR_() invokeWithId:[Clojurecore_vector_get_VAR_() invokeWithId:i withId:sel withId:args]]];
    while (true) {
        id v = [ClojureLangRT readStringWithNSString:[nssocket2 read]];
        if ([ClojureLangRT countWithId:v] == 3) {
            [ReplClient processCall:nssocket2 msg:v];
        } else {
            return [ClojureLangRT secondWithId:v];
        }
    }
}

+ (NSString *)randomUUID {
    CFUUIDRef uuid = CFUUIDCreate(kCFAllocatorDefault);
    NSString *uuidString = (NSString *)CFUUIDCreateString(kCFAllocatorDefault, uuid);
    CFRelease(uuid);
    return uuidString;
}

@end
