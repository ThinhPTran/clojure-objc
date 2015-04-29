//
//  ReplClient.h
//  mpos
//
//  Created by Gal Dolber on 4/12/14.
//  Copyright (c) 2014 zuldi. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface ReplClient : NSObject

+(void) connect: (NSString*) host;
+(id) callRemote:(id)sel args:(id)args;

@end
