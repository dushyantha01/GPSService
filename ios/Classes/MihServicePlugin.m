#import "MihServicePlugin.h"
#if __has_include(<mih_service/mih_service-Swift.h>)
#import <mih_service/mih_service-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "mih_service-Swift.h"
#endif

@implementation MihServicePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMihServicePlugin registerWithRegistrar:registrar];
}
@end
