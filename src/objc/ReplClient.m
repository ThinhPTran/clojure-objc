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
#import "clojure/lang/Keyword.h"
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
static ClojureLangAtom *responses;

@implementation ReplClient

+(void) processCall2:(NSSocketImpl*)socket msg:(id)msg {
    @try {
        id i = [ClojureLangRT firstWithId:msg];
        id s = [ClojureLangRT secondWithId:msg];
        id args = [ClojureLangRT thirdWithId:msg];
        id r = [(ClojureLangAFn*)s applyToWithClojureLangISeq:[ClojureLangRT seqWithId:args]];
        [responses swapWithClojureLangIFn:Clojurecore_assoc_get_VAR_() withId:i withId:r];
        [socket println:[Clojurecore_pr_str_get_VAR_() invokeWithId:[Clojurecore_vector_get_VAR_() invokeWithId:i withId:r]]];
    }
    @catch (NSException *exception) {
        [socket println:[Clojurecore_pr_str_get_VAR_() invokeWithId:[NSString stringWithFormat:@"%@",[exception callStackSymbols]]]];
        NSLog(@"%@", exception);
    }
}

+(void) processCall:(NSSocketImpl*)socket msg:(id)msg {
    if ([ReplClient maybeRetry:socket msg:msg]) {
        return;
    }
    id runInMain = [ClojureLangRT firstWithId:msg];
    msg = [ClojureLangRT nextWithId:msg];
    if ([ClojureLangRT booleanCastWithId:runInMain]) {
        if ([NSThread isMainThread]) {
            [ReplClient processCall2:socket msg:msg];
        } else {
            dispatch_sync(dispatch_get_main_queue(), ^{
                [ReplClient processCall2:socket msg:msg];
            });
        }
    } else {
        dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
            [ReplClient processCall2:socket msg:msg];
        });
    }
}

+(BOOL) maybeRetry:(NSSocketImpl*)socket msg:(id) msg {
    if ([[ClojureLangRT firstWithId:msg] isEqual:[ClojureLangKeyword internWithNSString:@"retry"]]) {
        id i = [ClojureLangRT secondWithId:msg];
        id r = [[responses deref] valAtWithId:i];
        [socket println:[Clojurecore_pr_str_get_VAR_() invokeWithId:[Clojurecore_vector_get_VAR_() invokeWithId:i withId:r]]];
        return YES;
    } else {
        return NO;
    }
}

+(void) connect: (NSString*) host {
    responses = [[ClojureLangAtom alloc] initWithId:ClojureLangPersistentHashMap_get_EMPTY_()];
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
    [nssocket2 println:[Clojurecore_pr_str_get_VAR_()
                            invokeWithId:[Clojurecore_vector_get_VAR_()
                            invokeWithId:[ClojureLangRT boxWithBoolean:[NSThread isMainThread]] withId:i withId:sel withId:args]]];
    while (true) {
        id v = [ClojureLangRT readStringWithNSString:[nssocket2 read]];
        if (![ReplClient maybeRetry:nssocket2 msg:v]) {
            if ([ClojureLangRT countWithId:v] == 4) {
                [ReplClient processCall:nssocket2 msg:v];
            } else {
                return [ClojureLangRT secondWithId:v];
            }
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
