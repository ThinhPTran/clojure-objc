//
//  NSSocketImpl.m
//  sample
//
//  Created by Gal Dolber on 2/1/14.
//  Copyright (c) 2014 clojure-objc. All rights reserved.
//

#import "NSSocketImpl.h"
#import "clojure/lang/RT.h"
#import "clojure/lang/Var.h"
#import "clojure/lang/Selector.h"
#import "clojure/lang/ISeq.h"
#import "clojure/lang/ObjC.h"
#import "clojure/lang/Selector.h"
#import "Cst502Socket.h"
#import <UIKit/UIKit.h>

@implementation NSSocketImpl {
    Cst502ClientSocket* cs;
}

- (id) initWithHost:(NSString*)hostName withPort:(NSString*)portNum {
    self = [super init];
    if (self) {
        cs = [[Cst502ClientSocket alloc] initWithHost: hostName
                                           portNumber: portNum];
        [self reconnect];
    }
    return self;
}

- (void) reconnect {
    while(![cs connect]) {
        NSLog(@"Error connecting... will try again in 5 second");
        [NSThread sleepForTimeInterval:5];
    }
}

- (id) read {
    fflush (stdout);
    char *buf = malloc(12);
    NSString *a = [[cs receiveBytes:buf maxBytes:12 beginAt:0] stringByTrimmingCharactersInSet:
                   [NSCharacterSet whitespaceCharacterSet]];
    /*if ([@"" isEqualToString:a]) {
     [self reconnect];
     return [self read];
     }*/
    int size = [a intValue] + 2;
    free(buf);
    buf = malloc(size);
    a = [cs receiveBytes:buf maxBytes:size beginAt:0];
    free(buf);
    return a;
}

- (void) println: (NSString*) s {
    [cs sendString: s];
    fflush (stdout);
}

-(void)dealloc {
    [cs release];
    [super dealloc];
}

-(void)close {
    [cs close];
}

@end
